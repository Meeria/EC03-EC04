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
| 9 | Permissions IAM de l'operateur (celui qui lance Terraform/Ansible) non definies precisement, identifiants personnels potentiellement larges | Acces / identite | Eleve si ces identifiants sont larges (type administrateur) | Moderee | Eleve | A traiter |
| 10 | Absence de logs et de supervision | Detection | Eleve : un incident pourrait passer inapercu | Elevee : rien n'est en place actuellement | Eleve | Objet du plan de supervision (section suivante) |
| 11 | Reference IP residuelle commentee dans `application.properties` (`149.202.77.193:7819`) | Application / hygiene | Faible : fuite d'information mineure sur une infra passee | Faible | Faible | A nettoyer |
| 12 | Pas de rotation automatique du secret RDS | Donnees / secrets | Faible : le secret reste valide indefiniment si compromis | Faible | Faible | Piste d'amelioration, Secrets Manager le permet nativement |

Priorite de traitement dans cette mission : les risques Critique et Eleve (1, 2, 3, 4, 6, 8, 9, 10) sont couverts dans "Configuration securite" et "Plan de supervision" ci-dessous. Les risques Faible (11, 12) sont notes mais pas necessairement traites dans cette iteration.

## Configuration securite

Perimetre retenu pour cette section : IAM, reseau et secrets (infra), en coherence avec le libelle du livrable. Les corrections applicatives (authentification API, restriction CORS) restent hors perimetre de cette mission et sont documentees comme piste d'amelioration en fin de fichier.

### IAM - politique de l'operateur (risque #9)

Aujourd'hui, Terraform et Ansible sont lances avec les identifiants AWS personnels de l'operateur, sans politique dediee. Ce n'est pas un probleme immediat (compte personnel, exercice individuel), mais ce n'est pas non plus le principe du moindre privilege : ces identifiants ont potentiellement bien plus de droits que necessaire.

Voici la politique IAM qui devrait etre attachee a un utilisateur/role de deploiement dedie :

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "InfraProvisioningEuWest3Only",
      "Effect": "Allow",
      "Action": ["ec2:*", "rds:*", "s3:*", "ecr:*"],
      "Resource": "*",
      "Condition": {
        "StringEquals": { "aws:RequestedRegion": "eu-west-3" }
      }
    },
    {
      "Sid": "SecretsManagerUrbanhubOnly",
      "Effect": "Allow",
      "Action": [
        "secretsmanager:CreateSecret",
        "secretsmanager:DeleteSecret",
        "secretsmanager:PutSecretValue",
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret",
        "secretsmanager:TagResource"
      ],
      "Resource": "arn:aws:secretsmanager:eu-west-3:*:secret:urbanhub/*"
    },
    {
      "Sid": "IamRoleForEc2InstanceOnly",
      "Effect": "Allow",
      "Action": [
        "iam:CreateRole", "iam:DeleteRole", "iam:GetRole",
        "iam:PutRolePolicy", "iam:DeleteRolePolicy", "iam:GetRolePolicy",
        "iam:AttachRolePolicy", "iam:DetachRolePolicy",
        "iam:CreateInstanceProfile", "iam:DeleteInstanceProfile",
        "iam:AddRoleToInstanceProfile", "iam:RemoveRoleFromInstanceProfile",
        "iam:GetInstanceProfile", "iam:TagRole", "iam:PassRole"
      ],
      "Resource": "arn:aws:iam::*:role/urbanhub-*"
    },
    {
      "Sid": "SsmSessionForDeployment",
      "Effect": "Allow",
      "Action": [
        "ssm:StartSession", "ssm:TerminateSession",
        "ssm:DescribeInstanceInformation", "ssm:GetConnectionStatus"
      ],
      "Resource": "*"
    }
  ]
}
```

Points a retenir sur cette politique :
- une condition de region (`aws:RequestedRegion`) limite tout ce qui pourrait etre cree en dehors d'`eu-west-3` ;
- l'acces a Secrets Manager est restreint aux secrets sous le prefixe `urbanhub/*`, pas a tous les secrets du compte ;
- les actions IAM sont restreintes aux roles nommes `urbanhub-*` (celui de l'instance EC2), pas a la gestion IAM du compte entier ;
- `ec2:*`, `rds:*`, `s3:*`, `ecr:*` restent larges au niveau des actions, car Terraform a besoin de creer/lire/modifier de nombreuses ressources differentes (VPC, subnets, security groups, instances, bases, depots...) et lister chaque action une par une serait long, fragile (un oubli casse le `terraform apply`), et difficile a verifier sans rejouer le deploiement plusieurs fois. Un resserrement action-par-action plus poussee se ferait normalement avec IAM Access Analyzer, qui genere une politique a partir de l'historique CloudTrail reel d'utilisation - disproportionne pour cet exercice.

Cette politique n'a pas ete appliquee au compte utilise pendant l'epreuve, pour ne pas risquer de bloquer le reste du deploiement en cours de route. Elle est documentee ici comme configuration cible.

### Regles reseau (risques #4, #6)

Deja mises en place en EC04-1, recapitulees ici :

| Security group | Regle entrante | Justification |
|---|---|---|
| `app` (EC2) | port 8080 depuis `0.0.0.0/0` | API a exposer publiquement |
| `app` (EC2) | aucun port SSH | administration via SSM Session Manager uniquement |
| `db` (RDS) | port 5432 depuis le SG `app` uniquement | la base n'est jamais accessible depuis autre chose que le backend |

Le broker MQTT (risque #4) reste hors perimetre cloud (il tourne en local uniquement), donc son absence d'authentification n'est pas corrigee ici : elle n'est pas exposee au-dela du reseau Docker local de developpement. A traiter si Mosquitto devait un jour etre deploye au-dela du poste local.

### Gestion des secrets (risques #3, #12)

Le mot de passe RDS est genere par Terraform et stocke dans AWS Secrets Manager (voir EC04-1) : plus aucune valeur en clair dans le code ou la configuration deployee dans le cloud.

Limite assumee : le code source local (`application.properties`, `application-production.properties`, `docker-compose.yml`) garde des identifiants par defaut en clair (`urbanhub`/`urbanhub`), utilises uniquement en developpement local. Le rotation automatique du secret Secrets Manager n'est pas activee (elle demanderait une fonction Lambda dediee, disproportionne pour un seul secret dans cet exercice).

## Plan de supervision

(logs, metriques, alertes - a completer)
