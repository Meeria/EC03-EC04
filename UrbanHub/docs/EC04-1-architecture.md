# EC04 Partie 1 - Architecture de deploiement cloud

## 1. Contexte et perimetre

UrbanHub est aujourd'hui un service qui tourne entierement en local, via un `docker-compose.yml` qui regroupe six conteneurs : la base de donnees (TimescaleDB), le backend Spring Boot, le front-end, un broker MQTT (Mosquitto), un simulateur de capteurs IoT, et SonarQube pour l'analyse de qualite.

L'objectif de cette mission est de deployer ce service dans un environnement cloud reel (AWS), de maniere automatisee et reproductible, tout en gardant un perimetre limite et coherent avec les moyens d'une petite equipe.

Le choix a ete de ne migrer vers le cloud que le coeur du systeme : le backend (l'API) et sa base de donnees. Les autres composants (front-end, Mosquitto, simulateur de capteurs, SonarQube) restent en local. Ce n'est pas un oubli : le sujet autorise explicitement ce recentrage ("les autres composants du systeme peuvent etre simules ou decrits dans la documentation"), et cela evite de multiplier les briques a gerer et a facturer pour un exercice dont le but est de valider une chaine de deploiement, pas de reproduire l'integralite du systeme dans le cloud.

## 2. Architecture retenue

```
Operateur (poste local)
   | aws ssm start-session --target <instance>   (aucun port entrant ouvert)
   v
EC2 (Amazon Linux 2023, t3.micro) - dans un subnet public
 |- Security group : seul le port de l'API (8080) est ouvert vers l'exterieur
 |- Role IAM limite a : SSM (pour la session) + acces a un bucket S3 dedie
 |- Docker + docker compose : un seul conteneur, l'image du backend
        |
        | (security group : uniquement depuis l'instance backend)
        v
RDS PostgreSQL (db.t3.micro) - dans un subnet prive, pas d'IP publique
```

Autour de ca :
- un **VPC** avec 2 zones de disponibilite, chacune avec un subnet public (pour l'EC2) et un subnet prive (pour la RDS),
- un **depot ECR** qui stocke l'image Docker du backend,
- un **secret AWS Secrets Manager** qui contient le mot de passe de la base, genere automatiquement et jamais ecrit en clair nulle part,
- un **bucket S3** qui sert uniquement d'intermediaire technique pour qu'Ansible puisse deposer des fichiers de configuration sur le serveur via une session SSM (puisqu'il n'y a pas de SSH).

Toute cette infrastructure est decrite avec Terraform (fichiers dans `infra/terraform/`), et la configuration du serveur (installation de Docker, deploiement du conteneur) est faite avec Ansible (fichiers dans `infra/ansible/`). La procedure complete est dans `infra/README.md`.

## 3. Justification des choix

### EC2 + Docker Compose

Le sujet insiste sur une equipe avec des "competences limitees en DevOps et en automatisation avancee". EC2 + Docker Compose reproduit exactement le modele deja utilise en local (le meme `docker-compose.yml`, juste simplifie a un seul service) : pas de nouveau concept a apprendre. ECS Fargate ou Elastic Beanstalk auraient ete plus "cloud-natifs", mais ajoutent des notions supplementaires (task definitions, load balancer manage, etc.) qui ne se justifient pas pour un service de cette taille.

### RDS plutot qu'une base en conteneur

Une base de donnees geree par AWS (sauvegardes automatiques, patchs de securite geres par AWS) est plus adaptee a une equipe qui n'a pas le temps ni les competences pour administrer elle-meme un serveur PostgreSQL. Le cout supplementaire par rapport a une base en conteneur est faible (voir section 5) et se justifie par le gain en fiabilite et en exploitabilite.

### RDS PostgreSQL standard plutot que TimescaleDB

En local, la base utilise l'extension TimescaleDB (hypertables) pour optimiser le stockage des mesures dans le temps. Amazon RDS ne propose pas cette extension. Le code du projet (`TimescaleDbInitializer`) est deja concu pour degrader proprement : si l'extension n'est pas disponible, il logue une erreur et continue, les mesures sont alors stockees comme des lignes classiques. Aucune fonctionnalite n'est perdue, seulement une optimisation de performance sur de tres gros volumes, ce qui n'est pas le cas d'usage ici.

### Aucun port SSH, acces via AWS Systems Manager Session Manager

Plutot que d'ouvrir un port SSH (meme restreint a une IP), l'administration de l'instance se fait exclusivement via Session Manager : aucun port entrant d'administration n'est ouvert sur le security group, et chaque connexion est tracee dans CloudTrail. C'est un choix qui repond directement a l'exigence du sujet de "reduire la surface d'exposition" et de "limiter les acces inutiles". La contrepartie technique est qu'Ansible doit passer par un plugin de connexion different (`aws_ssm`), qui a lui-meme besoin d'un bucket S3 pour transferer des fichiers (Session Manager ne propose pas de copie de fichier directe comme SSH).

### Secret RDS dans AWS Secrets Manager

Le mot de passe de la base est genere automatiquement par Terraform et stocke dans Secrets Manager, jamais ecrit en clair dans le code ou la configuration. C'est directement lie a une faille reelle du projet existant, ou ce mot de passe etait ecrit en dur dans `application-production.properties` et dans `docker-compose.yml`. Le mot de passe est recupere uniquement au moment du deploiement, par la machine de l'operateur (avec ses propres identifiants AWS), et transmis au serveur via la session SSM.

### Aucun acces AWS depuis le serveur lui-meme

Le role IAM attache a l'instance EC2 est volontairement reduit au strict minimum : la politique managee `AmazonSSMManagedInstanceCore` (necessaire pour Session Manager) et un acces limite au bucket S3 de transfert. Le serveur n'a acces ni a Secrets Manager, ni a ECR : ces deux acces sont faits depuis la machine de l'operateur, qui transmet ensuite le resultat (mot de passe, jeton de connexion Docker) au serveur. Si le serveur est compromis, l'attaquant ne peut donc rien faire d'autre sur le compte AWS que d'ouvrir une session SSM et lire/ecrire dans ce bucket precis.

### Pas de HTTPS

L'API est exposee en HTTP simple, protegee uniquement par le security group. Ajouter un certificat (via un load balancer applicatif et ACM) aurait demande un nom de domaine reel, non disponible dans le cadre de cet exercice, et une brique d'infrastructure supplementaire (ALB) a gerer. C'est une limite assumee, documentee ci-dessous plutot que masquee par un certificat auto-signe qui n'aurait apporte qu'une securite illusoire (les navigateurs le refusent quand meme).

### State Terraform local

Le state Terraform est garde en local (pas de backend S3 distant). Pour un environnement unique, non partage entre plusieurs personnes, ajouter un bucket S3 et une table de verrouillage pour le state aurait ete une complexite supplementaire sans benefice reel dans ce contexte.

## 4. Securite integree

Cette mission (EC04 partie 1) pose les fondations de securite necessaires a un deploiement cloud correct. L'analyse de risques detaillee, la politique IAM complete et le plan de supervision font l'objet de la mission suivante (EC04 partie 2), pour ne pas dupliquer ce travail entre les deux missions.

Ce qui est deja en place a ce stade :
- secret de base de donnees genere automatiquement et stocke dans Secrets Manager (jamais en clair),
- base de donnees jamais exposee publiquement (subnet prive, security group qui n'autorise que l'instance backend),
- aucun port d'administration ouvert (SSM Session Manager a la place de SSH),
- role IAM de l'instance reduit au strict necessaire (SSM + un seul bucket S3, rien d'autre),
- scan de vulnerabilites automatique sur chaque image poussee dans ECR (fonctionnalite native, gratuite, activee des la creation du depot).

## 5. Analyse performances et cout

### Dimensionnement

- **EC2 `t3.micro`** : 2 vCPU en mode "burstable", 1 Go de RAM. Suffisant pour un backend Spring Boot avec un trafic de demonstration, pas concu pour un trafic de production a fort volume.
- **RDS `db.t3.micro`** : memes caracteristiques cote base de donnees, avec 20 Go de stockage gp3.

### Estimation de cout mensuel (region eu-west-3, hors period free tier)

| Ressource | Estimation |
|---|---|
| EC2 t3.micro | ~8-9 EUR/mois |
| RDS db.t3.micro (single-AZ) + stockage 20 Go | ~15-18 EUR/mois |
| Elastic IP | gratuite tant qu'attachee a une instance active |
| AWS Secrets Manager (1 secret) | ~0,40 USD/mois |
| ECR + bucket S3 (transfert Ansible) | negligeable a ce volume |
| **Total indicatif** | **~25-30 EUR/mois** |

A noter : une partie de ces couts peut etre couverte par l'offre gratuite AWS (free tier) pendant les 12 premiers mois d'un compte, selon les types d'instance eligibles dans la region utilisee.

### Limites de montee en charge

- L'instance EC2 et la base RDS sont chacune sur une seule zone de disponibilite (pas de bascule automatique en cas de panne d'une zone).
- Un seul serveur backend : pas de repartition de charge, pas de tolerance de panne si l'instance tombe.
- `t3.micro` est un type d'instance "burstable" : au-dela d'un certain volume d'utilisation soutenue du CPU, les performances sont reduites (mecanisme de credits CPU).

### Pistes d'evolution si la charge augmente

- Passer `db.t3.micro` a une classe superieure, et activer le Multi-AZ pour la RDS (bascule automatique, au prix d'un cout double sur la base).
- Ajouter un load balancer applicatif devant plusieurs instances EC2, avec un groupe d'auto-scaling.
- Introduire un cache (Redis, par exemple) si la charge de lecture sur la base devient un point de contention.

Ces evolutions ne sont pas mises en oeuvre ici car elles ne se justifient pas pour le volume de trafic actuel (un service de demonstration), et ajouteraient une complexite d'exploitation que l'equipe, avec ses competences DevOps actuelles, ne serait pas en mesure de maintenir facilement.

## 6. Limites et pistes d'amelioration

- **Pas de HTTPS** : a ajouter avec un load balancer applicatif et un certificat ACM, des qu'un nom de domaine reel sera disponible.
- **Pas de haute disponibilite** : instance EC2 et RDS toutes deux single-AZ, a faire evoluer si la continuite de service devient critique.
- **State Terraform local** : a migrer vers un backend S3 distant si plusieurs personnes doivent un jour collaborer sur cette infrastructure.
- **Image Docker de developpement** : l'image backend est construite avec le `Dockerfile` existant du projet, qui lance l'application via `gradlew bootRun` (pas d'optimisation, pas de build multi-etapes). Une image de production plus legere et plus rapide a demarrer est du ressort de la mission suivante (EC03 partie 1, le pipeline CI/CD).
- **Analyse de risques, politique IAM detaillee et supervision (logs, metriques, alertes)** : volontairement laissees a la mission EC04 partie 2
