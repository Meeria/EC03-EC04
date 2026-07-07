# Infrastructure UrbanHub - EC04 partie 1

## Presentation

Ce dossier contient l'infrastructure AWS qui heberge le backend UrbanHub (l'API Spring Boot).

Elle est composee de trois éléments principaux :

- une instance EC2 (Amazon Linux 2023, type `t3.micro`) qui fait tourner le backend dans un conteneur Docker,
- une base de donnees RDS PostgreSQL (`db.t3.micro`), placee dans un subnet prive, jamais exposee directement sur Internet,
- un depot ECR qui stocke l'image Docker du backend.

L'administration de l'instance se fait uniquement via AWS Systems Manager Session Manager. Il n'y a pas de SSH et aucun port d'administration n'est ouvert.

Le front-end, le broker MQTT (Mosquitto), le simulateur de capteurs et SonarQube ne sont pas deployes sur AWS. Ils restent decrits comme faisant partie du systeme complet, mais hors du perimetre de cette mission.

## Prerequis

Avant de pouvoir deployer, il faut avoir sur sa machine :

- un compte AWS avec des identifiants configures (`aws configure`), qui a les droits necessaires sur VPC, EC2, RDS, ECR, IAM, S3 et Secrets Manager,
- Terraform version 1.5 ou plus recente,
- Ansible, avec les collections suivantes installees :
  ```bash
  ansible-galaxy collection install community.docker community.aws amazon.aws
  ```
- les paquets Python `boto3` et `botocore` :
  ```bash
  pip3 install boto3 botocore
  ```
- le plugin `session-manager-plugin` de l'AWS CLI,
- Docker installe localement, pour construire et pousser l'image du backend.

## Etapes du deploiement

### 1. Creer l'infrastructure avec Terraform

```bash
cd infra/terraform
terraform init
terraform apply
```

Terraform affiche un plan, il faut taper `yes` pour confirmer. A la fin, il affiche six valeurs (les "outputs") dont on aura besoin juste apres : l'IP publique de l'instance, son identifiant, le nom du bucket S3, l'adresse de la base RDS, l'URL du depot ECR et l'ARN du secret contenant le mot de passe de la base.

### 2. Construire et pousser l'image du backend

```bash
cd ..
aws ecr get-login-password --region eu-west-3 | docker login --username AWS --password-stdin <url_du_depot_ecr>
docker build -t <url_du_depot_ecr>:latest .
docker push <url_du_depot_ecr>:latest
```

Remplacer `<url_du_depot_ecr>` par la valeur de l'output `ecr_repository_url`.

### 3. Deployer avec Ansible

```bash
cd ansible
cp inventory.ini.tmpl inventory.ini
```

Dans `inventory.ini`, remplacer `<INSTANCE_ID>` par l'output `backend_instance_id`, et `<SSM_TRANSFER_BUCKET>` par l'output `ssm_transfer_bucket`.

Dans `playbook.yml`, remplacer les trois `CHANGE_ME` par les outputs `ecr_repository_url`, `db_password_secret_arn` et `rds_endpoint`.

Puis lancer :

```bash
ansible-playbook playbook.yml
```

### 4. Verifier que ca fonctionne

```bash
curl -i http://<ip_publique>:8080/api/measures
```

Une reponse HTTP (200 ou une erreur applicative documentee dans `API.md`) veut dire que le backend a demarre et qu'il arrive a se connecter a la base de donnees.

## Se connecter a l'instance pour du debug

Comme il n'y a pas de SSH, on passe par Session Manager :

```bash
aws ssm start-session --target <backend_instance_id>
```

## Redeployer une nouvelle version

Quand le code a change, il suffit de refaire les etapes 2 et 3 :

```bash
docker build -t <url_du_depot_ecr>:latest .
docker push <url_du_depot_ecr>:latest
cd infra/ansible
ansible-playbook playbook.yml
```

## Detruire l'environnement

```bash
cd infra/terraform
terraform destroy
```

Attention : la base RDS a une protection contre la suppression accidentelle (`deletion_protection`). Il faut d'abord la desactiver avant de pouvoir la detruire, par exemple avec :

```bash
aws rds modify-db-instance --db-instance-identifier urbanhub-exam --no-deletion-protection --apply-immediately
```

## Limites connues

- Pas de HTTPS : l'API est accessible en HTTP simple, la protection se fait uniquement au niveau du security group.
- Pas de haute disponibilite : l'instance EC2 et la base RDS sont chacune sur une seule zone de disponibilite.
- L'image Docker du backend est construite avec le Dockerfile de developpement du projet (qui lance l'application via `gradlew bootRun`). Une image optimisee pour la production sera produite par le pipeline CI/CD (mission EC03 partie 1), pas ici.
