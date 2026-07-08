# EC03 Partie 1 - Pipeline CI/CD

## Plateforme

Le depot a bascule de GitLab vers GitHub. Le pipeline est donc un workflow GitHub Actions (`.github/workflows/ci-cd.yml`), et non un `.gitlab-ci.yml`.

## Declenchement

| Evenement | Jobs |
|---|---|
| push / pull_request, toute branche | build, test, quality, security |
| push sur main | + docker (build/push ECR), deploy (Ansible vers l'EC2) |

## Etapes

- **build** : `./gradlew assemble` - verifie que le projet compile de maniere reproductible.
- **test** : `./gradlew test jacocoTestReport` sur H2 en memoire, rapport Jacoco publie en artifact (`jacoco-report`).
- **quality** : placeholder (`echo "Quality gate ok (mock)"`). Une analyse SonarQube reelle (conteneur `sonarqube:community` ephemere en service du job, generation d'un token, verification du quality gate via l'API) a ete implementee puis remplacee par ce mock. **Limite assumee** : l'instance SonarQube initialement prevue tournait sur les serveurs de l'ecole, qui a ete arretee - plutot que de dependre d'une instance externe non garantie disponible pendant l'epreuve, le choix a ete de simuler le resultat du quality gate.
- **security** : scan de vulnerabilites avec Trivy (`scan-type: fs`, code source + dependances, pas l'image Docker). Deux passages : un rapport complet (CRITICAL+HIGH, non bloquant) publie en SARIF dans l'onglet Security du repo GitHub, puis un scan bloquant (`exit-code: 1`) restreint aux CRITICAL - c'est ce second passage qui fait echouer le job.
- **docker** (main uniquement, `needs: [quality, security]`) : build de l'image depuis le `Dockerfile` existant, tag = SHA du commit (tracabilite), login ECR via `aws-actions/amazon-ecr-login`, push vers l'ECR existant (`ecr.tf`, scan de vulnerabilites deja actif via `scan_on_push`). Une vulnerabilite CRITICAL detectee par `security` bloque ce job, et donc `deploy`.
- **deploy** (main uniquement) : redemarrage du backend sur l'EC2 via Ansible, connexion SSM (pas de SSH). Le job installe le `session-manager-plugin` et Ansible sur le runner, genere `inventory.ini` a partir du template (`inventory.ini.tmpl`) en y injectant les repository variables `INSTANCE_ID`/`SSM_TRANSFER_BUCKET`, puis lance `ansible-playbook playbook.yml --extra-vars "image_tag=<SHA du commit>"` - l'image deployee est donc toujours celle qui vient d'etre poussee sur ECR par le job precedent.

## Prerequis (secrets/variables GitHub)

- Secrets : `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`.
- Variables (Settings > Secrets and variables > Actions > onglet Variables) : `INSTANCE_ID`, `SSM_TRANSFER_BUCKET` (sorties Terraform `backend_instance_id` et `ssm_transfer_bucket`, stables tant que l'infra n'est pas recreee).

## Perimetre : la CI ne gere pas Terraform

Le pipeline automatise build, test, quality, la production de l'image Docker et son deploiement sur l'EC2 existante - il ne provisionne pas l'infrastructure elle-meme (`terraform apply` reste une commande manuelle, cf. `infra/README.md`).

**Choix assume**, pas un oubli :
- Le sujet autorise explicitement un deploiement "automatise **ou semi-automatise**" (EC04-1) et d'automatiser "**tout ou partie** du processus de deploiement" (EC03-1) - le provisioning manuel de l'infra et le deploiement applicatif automatise entrent dans ce cadre.
- Le state Terraform est local (pas de backend S3 distant, cf. `EC04-1-architecture.md`) : sans backend distant, un job CI n'a de toute facon pas acces a l'etat actuel de l'infra pour savoir ce qui devrait changer.
- Meme avec un backend distant, lancer `terraform apply` automatiquement en CI sur une infra de prod (RDS avec `deletion_protection`, changements pouvant impliquer un remplacement de ressource) sans etape de revue humaine serait un risque disproportionne par rapport au besoin, et irait a l'encontre de la "maitrise de la complexite" demandee pour une equipe avec des competences DevOps limitees (EC04-1, point 4).

## Incident rencontre en validant le job `deploy`

Lors du premier passage du job `deploy` sur `main`, le job a echoue avec un timeout Ansible pendant la connexion SSM (`DISABLE ECHO command 'stty -echo' timeout on host`).

**Diagnostic** : le meme `ansible-playbook` execute en local (hors CI, avec le meme inventaire) echouait aussi, avec une erreur differente (`TargetNotConnected` des la tentative de connexion) - ce qui ecartait un probleme specifique a GitHub Actions. `aws ssm describe-instance-information` a confirme la cause : l'agent SSM de l'instance EC2 etait dans l'etat `PingStatus: ConnectionLost`, sans ping depuis plus de 25 minutes, alors que le security group (sortie `0.0.0.0/0`) et l'etat de l'instance (`running`) etaient normaux.

**Cause** : l'agent SSM (version `3.3.4624.0`, pas la derniere disponible) s'est deconnecte du service AWS Systems Manager, independamment du reseau ou de la CI.

**Correction** : redemarrage de l'instance EC2 (`reboot`), l'agent se reenregistre au demarrage. Aucune modification de `ci-cd.yml` n'a ete necessaire : le job `deploy` etait correct, seul l'agent etait bloque.

**Recidive** : au run suivant, le job est alle plus loin mais a echoue avec exactement la meme erreur sur la tache suivante (installation de Docker). Verifications faites avant de reagir : `describe-instance-information` montrait de nouveau `PingStatus: ConnectionLost` ; un Run Command de diagnostic (`AWS-RunShellScript`, hors session interactive Ansible) est reste bloque en `Pending/Delayed`, ce qui confirme que l'instabilite vient de l'agent lui-meme et non d'Ansible ; les metriques CloudWatch `CPUCreditBalance` (~180-197, pas d'epuisement) et `CPUUtilization` (44-54% en moyenne) excluent une saturation CPU du `t3.micro`. Une piste memoire (JVM Gradle + Docker sur seulement 1 Go de RAM) reste plausible mais non verifiee : aucun agent CloudWatch n'est installe pour remonter cette metrique (limite deja notee en `EC04-2-securite.md`).

**Limite assumee** : l'agent SSM de cette instance est instable de maniere recurrente et intermittente, pour une cause non totalement identifiee (hypothese memoire non confirmee). Comme l'instance n'a pas d'acces SSH (SSM uniquement, par choix de securite), la seule recuperation possible est un redemarrage de l'instance depuis l'API/console AWS - aucune supervision automatique de l'etat de l'agent n'est en place pour detecter ou alerter sur ce cas avant qu'un deploiement echoue dessus. Decision assumee de ne pas creuser davantage (upgrade d'instance, agent CloudWatch) pour rester dans le perimetre minimal de l'epreuve : en cas de nouvel echec, la reponse est de reessayer apres un redemarrage plutot que d'investiguer plus en profondeur.

## Interpretation des resultats

- Rapport de tests : artifact `jacoco-report` sur chaque run.
- Rapport qualite : mock actuellement (`quality` renvoie toujours un succes) - pas de rapport reel a interpreter tant que le placeholder est en place. **Limite assumee** : l'instance SonarQube prevue (serveurs de l'ecole) a ete arretee, d'ou ce remplacement par une simulation plutot qu'une dependance a une instance externe non garantie disponible pendant l'epreuve.
- Rapport securite : reel, genere par Trivy sur chaque run (`security`). Le detail (CVE, package concerne, severite) est consultable dans l'onglet **Security > Code scanning** du repo GitHub (format SARIF, categorie `trivy-fs`) - pas seulement un statut succes/echec comme pour `quality`. Une CRITICAL fait echouer le job (et bloque `docker`/`deploy`), une HIGH remonte dans le rapport sans bloquer.
- Tracabilite : chaque image deployee est identifiable par le SHA du commit qui l'a produite (tag ECR, et `image_tag` passe au playbook Ansible).
