# Design - Pipeline CI/CD GitHub Actions (EC03 Partie 1)

## Contexte

Le backend UrbanHub est deja deploye sur AWS (EC04 Partie 1 : EC2 + RDS + ECR, provisionnes par Terraform, deploiement applicatif par Ansible via SSM). Le depot a bascule de GitLab vers GitHub (`github.com/Meeria/EC03-EC04`), decision actee avec le formateur, pas a justifier dans la documentation.

Le pipeline `.gitlab-ci.yml` existant (stages build/test/sonar/deploy) est obsolete : il deploie par SSH vers un ancien serveur on-premise de l'equipe (`ALIX-Lubin/UrbanHub`, port 7818), sans lien avec l'infra AWS actuelle, sans build/push d'image Docker, sans analyse de securite.

Objectif : concevoir un pipeline GitHub Actions qui couvre le cycle complet (build, test, qualite, image, deploiement) et boucle reellement avec l'infra AWS existante, sans en faire plus que necessaire.

## Perimetre

- Uniquement le backend Spring Boot (`UrbanHub/`). Le front-end et le simulateur MQTT restent hors perimetre, coherent avec le choix deja fait en EC04-1 (composants simules/decrits, pas deployes).
- Terraform reste en execution manuelle : le pipeline ne provisionne pas l'infra, il deploie l'application dessus.

## Declenchement

| Evenement | Jobs executes |
|---|---|
| `push` ou `pull_request` sur n'importe quelle branche | `build`, `test`, `quality` |
| `push` sur `main` | + `docker` (build + push ECR), `deploy` (Ansible vers l'EC2) |

Le deploiement est automatique sur `main` (choix valide avec l'utilisateur) : pas de validation manuelle intermediaire.

## Jobs

### `build`
`./gradlew assemble` (Java 21, cache Gradle). Valide que le projet compile de maniere reproductible.

### `test`
`./gradlew test jacocoTestReport`. Les tests tournent sur H2 en memoire (`src/test/resources/application.properties`), aucun service externe requis. Le rapport Jacoco (XML + HTML) est publie en artifact GitHub Actions - satisfait le livrable "rapport de tests genere automatiquement".

### `quality`
Remplace la dependance a SonarCloud (compte non accessible) par une instance **SonarQube ephemere**, demarree comme `services:` du job (image publique `sonarqube:community`, la meme que celle deja utilisee en local dans `docker-compose.yml`). Elle ne vit que le temps du job, n'est jamais persistee ni poussee nulle part.

Deroulement :
1. Attendre que le service soit sain (`/api/system/health`).
2. Generer un token d'analyse via l'API SonarQube avec les identifiants par defaut d'une instance fraiche (`admin`/`admin`, `POST /api/user_tokens/generate`) - pas de secret `SONAR_TOKEN` a stocker, le token est genere et jete a chaque run.
3. Lancer `./gradlew sonarqube -Dsonar.host.url=http://localhost:9000 -Dsonar.token=$TOKEN`.
4. Interroger `/api/qualitygates/project_status` sur l'analyse produite, faire echouer le job si le statut n'est pas `OK`, et publier ce statut en artifact JSON.

C'est une vraie analyse a chaque run (pas de simulation), sans dependance a un compte SaaS externe.

### `docker` (uniquement sur push `main`)
Build de l'image applicative depuis le `Dockerfile` existant, tag `${{ github.sha }}` (traçabilite - remplace le tag fixe `latest` actuellement code en dur dans `playbook.yml`), push vers l'ECR existant (`aws-actions/amazon-ecr-login`).

Le scan de vulnerabilites de cette image reste couvert par `scan_on_push = true`, deja configure sur le repository ECR (`ecr.tf`) - pas de duplication dans le pipeline.

### `deploy` (uniquement sur push `main`)
Sur le runner : installer `ansible`, la collection `community.docker`, et le `session-manager-plugin` (requis par la connexion `aws_ssm`). Generer `infra/ansible/inventory.ini` a partir des variables de repository GitHub (non sensibles, stables tant que l'infra n'est pas recreee). Lancer :

```
ansible-playbook infra/ansible/playbook.yml --extra-vars "image_tag=${{ github.sha }}"
```

`playbook.yml` doit accepter `image_tag` en variable plutot qu'en valeur figee `latest`.

## Secrets et variables GitHub

**Secrets** (sensibles) :
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` - cles statiques d'un utilisateur IAM (choix valide ; alternative OIDC notee en amelioration ci-dessous).

**Variables de repository** (non sensibles, deja presentes en clair dans `playbook.yml` aujourd'hui - simplement externalisees proprement) :
- `INSTANCE_ID`, `SSM_TRANSFER_BUCKET`, `ECR_REPOSITORY_URL`, `RDS_ENDPOINT`, `DB_PASSWORD_SECRET_ID`.

Aucun secret SonarQube a stocker (token ephemere genere a chaque run, cf. job `quality`).

## Traçabilite et gestion des erreurs

- Chainage des jobs par `needs:` : un `test` ou un `quality` qui echoue bloque `docker` et `deploy` (fail-fast natif, pas de logique custom).
- Chaque job publie son rapport en artifact GitHub Actions (Jacoco, statut de quality gate).
- L'image deployee est tracee par le SHA du commit (tag Docker = `github.sha`), au lieu du tag `latest` actuel qui ne permet pas de savoir quel commit tourne en production.

## Hors perimetre / ameliorations documentees (pas implementees)

A lister dans `docs/EC04-2-securite.md` ou l'equivalent EC03, comme limites assumees plutot que non-traitees en silence :
- **Analyse de vulnerabilites sur les dependances Java** (risque #8) : pas de job dedie (OWASP Dependency-Check ou equivalent) dans le pipeline, decision explicite de l'utilisateur. A noter comme piste d'amelioration.
- **Authentification AWS par cles statiques** plutot que par OIDC (role assume, sans secret long-lived) : plus simple a mettre en place pour l'epreuve, mais moins "best practice" - a noter comme piste d'amelioration.
- **Terraform hors pipeline** : l'infra reste provisionnee/modifiee manuellement, seul le cycle applicatif est automatise.

## Risque technique a valider en implementation

La generation du token SonarQube via `admin`/`admin` sur un container fraichement demarre suppose que l'API accepte cette authentification par defaut sans changement de mot de passe prealable. A verifier en pratique lors de l'implementation ; si bloquant, repli possible : fixer le mot de passe admin au demarrage du service (`SONAR_WEB_SYSTEMPASSCODE` ou appel a l'API de changement de mot de passe avant la generation du token).

## Fichiers impactes (pour la phase de plan/implementation)

- Nouveau : `.github/workflows/ci-cd.yml`.
- Modifie : `infra/ansible/playbook.yml` (variable `image_tag` au lieu de valeur figee, variables externalisees).
- Modifie : `infra/ansible/inventory.ini.tmpl` ou logique de generation (a adapter pour etre genere par le workflow plutot qu'a la main).
- Supprime (ou conserve a titre d'historique) : `.gitlab-ci.yml`, remplace par le workflow GitHub Actions.
- Documentation : ajout d'une section listant les ameliorations non implementees (ci-dessus).
