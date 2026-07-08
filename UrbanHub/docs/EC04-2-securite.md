# EC04 Partie 2 - Securisation de l'application

## Risques

Niveau = impact x probabilite, sur une echelle simple : Critique, Eleve, Modere, Faible.

| # | Risque | Categorie | Impact | Probabilite | Niveau | Statut |
|---|---|---|---|---|---|---|
| 1 | Aucune authentification/autorisation sur l'API (pas de Spring Security, tous les endpoints `/api/**` sont publics) | Application | Eleve : n'importe qui peut lire ou modifier toutes les donnees | Elevee : API exposee publiquement en HTTP | **Critique** | A traiter |
| 2 | CORS ouvert a tous les domaines (`allowedOriginPattern("*")`, toutes methodes, tous headers) | Application | Modere : `allowCredentials=false` limite l'impact reel aujourd'hui, mais aucun controle si un jour des cookies/tokens sont introduits | Elevee | Modere | A traiter |
| 3 | Identifiants de base de donnees en dur dans le code (`application.properties`, `application-production.properties`, `docker-compose.yml`, `.env.example`) | Donnees / secrets | Eleve : fuite du mot de passe si le depot est expose | Elevee : deja present dans l'historique Git | **Critique** | Mitige pour le cloud (secret genere dans Secrets Manager, EC04-1) ; le code source garde toujours ces valeurs par defaut |
| 4 | Broker MQTT sans authentification (`allow_anonymous true`, pas de TLS) | Infrastructure / reseau | Eleve : injection de fausses mesures, ecoute du flux | Moderee : depend de l'exposition reseau du broker | Eleve | Hors perimetre EC04-1 (Mosquitto reste en local), pas encore traite |
| 5 | Documentation API (Swagger/OpenAPI) accessible sans authentification | Application | Modere : revele toute la structure de l'API | Elevee | Modere | A traiter (lie au risque 1) |
| 6 | Pas de HTTPS sur l'API exposee dans le cloud | Infrastructure / reseau | Eleve : donnees et futurs identifiants en clair sur le reseau | Moderee | Eleve | Limite assumee, documentee en EC04-1 |
| 7 | Identifiants par defaut SonarQube (`admin`/`admin`), jamais changes | Acces / identite | Modere : acces a l'outil d'analyse, pas aux donnees metier | Elevee si l'outil est expose au-dela du poste local | Modere | A traiter |
| 8 | Aucune analyse de vulnerabilites sur les dependances Java (le scan ECR ne couvre que l'image conteneur, pas les librairies applicatives) | Chaine logicielle | Variable, selon les CVE existantes | Moderee | Modere | A mettre en place |
| 9 | Permissions IAM de l'operateur (celui qui lance Terraform/Ansible) non definies precisement, identifiants personnels potentiellement larges | Acces / identite | Eleve : les identifiants utilises sont ceux du compte root (`arn:aws:iam::410432886251:root`), le niveau de privilege maximal possible sur AWS | Moderee | **Critique** | Policy least-privilege appliquee et attachee a un utilisateur IAM dedie (`urbanhub-operator`), verifiee fonctionnelle (voir Configuration securite) - mais l'usage courant (local + CI) tourne encore sur root, la bascule reste a faire |
| 10 | Absence de logs et de supervision | Detection | Eleve : un incident pourrait passer inapercu | Elevee : rien n'est en place actuellement | Eleve | Objet du plan de supervision (section suivante) |
| 11 | Reference IP residuelle commentee dans `application.properties` (`149.202.77.193:7819`) | Application / hygiene | Faible : fuite d'information mineure sur une infra passee | Faible | Faible | A nettoyer |
| 12 | Pas de rotation automatique du secret RDS | Donnees / secrets | Faible : le secret reste valide indefiniment si compromis | Faible | Faible | Piste d'amelioration, Secrets Manager le permet nativement |

Priorite de traitement dans cette mission : les risques Critique et Eleve (1, 2, 3, 4, 6, 8, 9, 10) sont couverts dans "Configuration securite" et "Plan de supervision" ci-dessous. Les risques Faible (11, 12) sont notes mais pas necessairement traites dans cette iteration.

## Configuration securite

Perimetre retenu pour cette section : IAM, reseau et secrets (infra), en coherence avec le libelle du livrable. Les corrections applicatives (authentification API, restriction CORS) restent hors perimetre de cette mission et sont documentees comme piste d'amelioration en fin de fichier.

### IAM - politique de l'operateur (risque #9)

Jusqu'a present, Terraform et Ansible etaient lances avec les identifiants **root** du compte AWS (`arn:aws:iam::410432886251:root`), verifie via `aws sts get-caller-identity` - pas juste des identifiants personnels larges, le niveau de privilege maximal possible sur AWS. Ce n'est pas un probleme immediat (compte personnel, exercice individuel), mais c'est l'oppose du principe du moindre privilege.

Cette politique est definie comme une vraie ressource Terraform (`infra/terraform/iam_operator.tf`, `aws_iam_policy.operator_least_privilege`) plutot que comme un simple exemple JSON dans ce document, pour rester coherente avec le reste de l'infra-as-code.

Points a retenir sur cette politique :
- une condition de region (`aws:RequestedRegion`) limite tout ce qui pourrait etre cree en dehors de la region du projet (`var.region`) ;
- l'acces a Secrets Manager est restreint aux secrets sous le prefixe `<project_name>/*`, pas a tous les secrets du compte ;
- les actions IAM sont restreintes aux roles/policies/utilisateurs nommes `<project_name>-*`, pas a la gestion IAM du compte entier ;
- des statements `logs:*` scopes au log group `/<project_name>/*` (plus un statement separe pour `logs:DescribeLogGroups`, qui n'accepte pas de scope resource precis cote AWS) couvrent la gestion du log group CloudWatch (`monitoring.tf`) ;
- `ec2:*`, `rds:*`, `s3:*`, `ecr:*` restent larges au niveau des actions, car Terraform a besoin de creer/lire/modifier de nombreuses ressources differentes (VPC, subnets, security groups, instances, bases, depots...) et lister chaque action une par une serait long, fragile (un oubli casse le `terraform apply`), et difficile a verifier sans rejouer le deploiement plusieurs fois. Un resserrement action-par-action plus pousse se ferait normalement avec IAM Access Analyzer, qui genere une politique a partir de l'historique CloudTrail reel d'utilisation - disproportionne pour cet exercice.

**Mise a jour** : cette policy a ete appliquee pour de vrai et attachee a un utilisateur IAM dedie (`aws_iam_user.operator`, `urbanhub-operator`, avec sa propre cle d'acces). Testee en la faisant lire l'integralite de l'etat Terraform existant (`terraform plan` sous ce profil) : plusieurs permissions manquantes ont ete decouvertes et corrigees par iteration (lecture des policies/tags attachees a un role IAM, lecture de la policy et de l'utilisateur eux-memes, `logs:DescribeLogGroups`/`ListTagsForResource`, `secretsmanager:GetResourcePolicy`, et un bug de scope ou `iam:GetInstanceProfile` etait limite aux ARN `role/*` au lieu d'inclure aussi `instance-profile/*`). Une fois ces corrections faites, le profil least-privilege lit l'ensemble de l'infra sans aucune erreur `AccessDenied`.

**Limite assumee, a ce stade** : la policy est prete et validee, mais l'usage courant (poste local, secrets GitHub Actions de la CI) n'a **pas encore ete bascule** dessus - root reste utilise en pratique pour le moment. La bascule (mise a jour des credentials locaux et des secrets `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` du repo, puis desactivation des cles root) est identifiee comme prochaine etape mais volontairement mise en pause pour ne pas risquer de casser le reste du deploiement en cours d'epreuve.

### Regles reseau (risques #4, #6)

Deja mises en place en EC04-1, recapitulees ici :

| Security group | Regle entrante | Justification |
|---|---|---|
| `app` (EC2) | port 8080 depuis `0.0.0.0/0` | API a exposer publiquement |
| `app` (EC2) | aucun port SSH | administration via SSM Session Manager uniquement |
| `db` (RDS) | port 5432 depuis le SG `app` uniquement | la base n'est jamais accessible depuis autre chose que le backend |

Le broker MQTT (risque #4) reste hors perimetre cloud (il tourne en local uniquement), donc son absence d'authentification n'est pas corrigee ici : elle n'est pas exposee au-dela du reseau Docker local de developpement. A traiter si Mosquitto devait un jour etre deploye au-dela du poste local.

La connexion JDBC entre le backend et la RDS utilise `sslmode=require` (`env.j2`, cote Ansible) : les echanges sont chiffres en transit, meme si le SG restreint deja l'acces au seul backend. `require` chiffre sans valider le certificat serveur (pas de `verify-full`) - suffisant contre une ecoute passive du reseau AWS, evite d'avoir a embarquer et maintenir le bundle de certificats CA RDS pour une verification complete, disproportionne ici. En local (docker-compose), la base ne supporte pas SSL par defaut et n'est pas exposee au-dela du reseau Docker de developpement : le JDBC local reste donc sans `sslmode`, ce n'est pas un oubli.

### Gestion des secrets (risques #3, #12)

Le mot de passe RDS est genere par Terraform et stocke dans AWS Secrets Manager (voir EC04-1) : plus aucune valeur en clair dans le code ou la configuration deployee dans le cloud.

Limite assumee : le code source local (`application.properties`, `application-production.properties`, `docker-compose.yml`) garde des identifiants par defaut en clair (`urbanhub`/`urbanhub`), utilises uniquement en developpement local. Le rotation automatique du secret Secrets Manager n'est pas activee (elle demanderait une fonction Lambda dediee, disproportionne pour un seul secret dans cet exercice).

Le stockage RDS est chiffre au repos (`storage_encrypted = true` dans `database.tf`, chiffrement gere par AWS KMS avec la cle par defaut). Ce parametre ne peut pas etre ajoute a une instance existante sans remplacement (Terraform detruit et recree l'instance) : applique ici en acceptant la recreation de la base pendant l'epreuve.

## Plan de supervision

**Logs** : un log group CloudWatch dedie (`aws_cloudwatch_log_group.backend`, retention 7 jours, `monitoring.tf`) est cree, et le role de l'instance EC2 a les droits `logs:CreateLogStream` / `logs:PutLogEvents` / `logs:DescribeLogStreams` scopes a ce log group (`aws_iam_role_policy.cloudwatch_logs`). L'infra est prete a recevoir les logs applicatifs, mais rien ne les y envoie encore : le driver Docker `awslogs` (ou un agent CloudWatch) n'est pas configure dans `docker-compose.prod.yml.j2`. Piste d'amelioration, non traitee dans cette iteration.

**Metriques** : les metriques standard fournies nativement par AWS (CPU, reseau, stockage sur l'EC2 et la RDS, granularite 5 minutes) sont disponibles dans CloudWatch sans configuration supplementaire.

**Alertes** : non mises en place. Le sujet les qualifie d'"eventuelles" (page 6 du sujet) - la variable `alert_email` (`variables.tf`) a ete anticipee pour une future alarme SNS mais n'est utilisee par aucune ressource : choix assume, pas un oubli, pour ne pas ajouter de complexite hors perimetre du minimum demande ("capacite minimale de detection et de suivi").
