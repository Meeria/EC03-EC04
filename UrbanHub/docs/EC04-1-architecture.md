# EC04 Partie 1 - Architecture de deploiement cloud

## Contexte et perimetre

UrbanHub tourne aujourd'hui en local via un `docker-compose.yml` a six conteneurs (base TimescaleDB, backend, front-end, MQTT, simulateur de capteurs, SonarQube).

Pour cette mission, seul le coeur du systeme part dans le cloud : le backend et sa base de donnees. Le reste (front-end, MQTT, simulateur) reste en local, comme le sujet l'autorise explicitement. Ca evite de multiplier les briques a gerer pour un exercice dont le but est de valider une chaine de deploiement, pas de migrer tout le systeme.

## Architecture

```
Poste local
   | aws ssm start-session --target <instance>   (aucun port entrant ouvert)
   v
EC2 (Amazon Linux 2023, t3.micro) - subnet public
 |- Security group : seul le port API (8080) est ouvert
 |- Role IAM limite a SSM + acces a un bucket S3 dedie
 |- Docker + docker compose : un seul conteneur, l'image du backend
        |
        v  (security group : uniquement depuis l'EC2)
RDS PostgreSQL (db.t3.micro) - subnet prive, pas d'IP publique
```

Autour de ca :
- **VPC** sur 2 zones de disponibilite (subnet public pour l'EC2, prive pour la RDS),
- **ECR** pour stocker l'image Docker du backend,
- **Secrets Manager** pour le mot de passe de la base, genere automatiquement,
- **bucket S3** utilise uniquement comme intermediaire pour qu'Ansible depose des fichiers de config via SSM (pas de SSH).

Terraform decrit l'infra (`infra/terraform/`), Ansible configure le serveur et deploie l'app (`infra/ansible/`). Procedure complete : `infra/README.md`.

## Pourquoi ces choix

- **EC2 + Docker Compose plutot qu'ECS/Beanstalk** : reprend le meme modele qu'en local, pas de nouveau concept a apprendre pour une equipe avec peu d'experience DevOps.
- **RDS plutot qu'une base en conteneur** : sauvegardes et patchs geres par AWS, l'equipe n'a pas a administrer un serveur PostgreSQL elle-meme.
- **PostgreSQL standard, pas TimescaleDB** : RDS ne propose pas cette extension. Le code (`TimescaleDbInitializer`) degrade deja proprement sans elle (les mesures sont stockees en table classique). Aucune fonctionnalite perdue, juste une optimisation de perf en moins.
- **SSM Session Manager plutot que SSH** : aucun port d'administration ouvert, connexions tracees dans CloudTrail. Ansible passe par le plugin `aws_ssm`, qui a besoin d'un bucket S3 pour transferer des fichiers (pas de copie directe comme en SSH).
- **Mot de passe RDS dans Secrets Manager** : corrige une faille reelle du projet (mot de passe en dur dans `application-production.properties`). Recupere uniquement au moment du deploiement, jamais stocke en clair.
- **Aucun acces AWS depuis le serveur** : le role IAM de l'EC2 se limite a SSM + un seul bucket S3. Secrets Manager et ECR sont contactes depuis la machine de l'operateur, jamais depuis le serveur. Si le serveur est compromis, l'attaquant n'a acces a rien d'autre sur le compte AWS.
- **Pas de HTTPS** : demanderait un nom de domaine reel et un load balancer, hors de portee pour cet exercice. Limite assumee plutot que masquee par un certificat auto-signe inutile.
- **State Terraform local** : pas de backend S3 distant, inutile pour un environnement travaille par une seule personne.

## Securite deja en place

L'analyse de risques detaillee, la politique IAM complete et la supervision sont traitees en EC04 partie 2, pour ne pas dupliquer le travail. Ce qui est deja fait ici :

- mot de passe genere et stocke dans Secrets Manager, jamais en clair,
- RDS jamais exposee publiquement (subnet prive, security group restreint a l'EC2),
- aucun port d'administration ouvert (SSM a la place de SSH),
- role IAM de l'instance reduit au strict necessaire,
- scan de vulnerabilites automatique sur chaque image poussee dans ECR.

## Cout et performance

Dimensionnement : `t3.micro` (2 vCPU burstable, 1 Go RAM) cote EC2 et RDS, 20 Go de stockage gp3.

| Ressource | Estimation mensuelle |
|---|---|
| EC2 t3.micro | ~8-9 EUR |
| RDS db.t3.micro + stockage 20 Go | ~15-18 EUR |
| Elastic IP | gratuite (instance active) |
| Secrets Manager (1 secret) | ~0,40 USD |
| ECR + bucket S3 | negligeable |
| **Total indicatif** | **~25-30 EUR/mois** |

Une partie peut etre couverte par le free tier AWS la premiere annee, selon les types d'instance eligibles dans la region.

Limites de charge : single-AZ des deux cotes (pas de bascule automatique), un seul serveur (pas de repartition de charge), `t3.micro` perd en performance au-dela d'un usage CPU soutenu (credits burstable).

Si la charge augmente : RDS en classe superieure + Multi-AZ, load balancer + plusieurs EC2 en auto-scaling, cache Redis si la base devient le goulot d'etranglement. Pas mis en place ici car pas justifie pour le trafic actuel, et ca ajouterait une complexite que l'equipe ne pourrait pas maintenir facilement.

## Limites connues

- Pas de HTTPS.
- Pas de haute disponibilite (EC2 et RDS single-AZ).
- State Terraform local, pas de backend distant.
- Image Docker de developpement (`gradlew bootRun`, pas de build multi-etapes) : une image de production optimisee est du ressort d'EC03 partie 1 (pipeline CI/CD).
- Analyse de risques, IAM detaille et supervision : renvoyes a EC04 partie 2.
