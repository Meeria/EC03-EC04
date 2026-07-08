# Infrastructure UrbanHub - EC04 partie 1

Deploiement du backend UrbanHub sur AWS : EC2 (Docker) + RDS PostgreSQL + ECR, sans SSH (acces via SSM Session Manager).

Justification des choix et analyse cout/performance : `docs/EC04-1-architecture.md`.

## Prerequis

- Compte AWS configure (`aws configure`), avec les droits sur VPC, EC2, RDS, ECR, IAM, S3, Secrets Manager.
- Terraform >= 1.5.
- Ansible + collections :
  ```bash
  ansible-galaxy collection install community.docker community.aws amazon.aws
  ```
- Python `boto3`/`botocore` :
  ```bash
  pip3 install boto3 botocore
  ```
  Si Ansible est installe via `pipx`, il faut aussi les injecter dans son environnement isole :
  ```bash
  pipx inject ansible boto3 botocore
  ```
- Plugin `session-manager-plugin` :
  ```bash
  brew install --cask session-manager-plugin
  ```
- Sur macOS, cette variable evite un crash connu d'Ansible avec boto3 :
  ```bash
  export OBJC_DISABLE_INITIALIZE_FORK_SAFETY=YES
  ```
- Docker installe localement (Docker Desktop demarre).

## Deploiement

### 1. Infrastructure

```bash
cd infra/terraform
terraform init
terraform apply
```

Note les 6 outputs affiches : `backend_public_ip`, `backend_instance_id`, `ssm_transfer_bucket`, `rds_endpoint`, `ecr_repository_url`, `db_password_secret_arn`.

### 2. Image du backend

Sur Mac Apple Silicon, l'image doit etre construite pour l'architecture de l'EC2 (x86_64), avec `--platform linux/amd64` :

```bash
cd ..
aws ecr get-login-password --region eu-west-3 | docker login --username AWS --password-stdin <ecr_repository_url>
docker build --platform linux/amd64 -t <ecr_repository_url>:latest .
docker push <ecr_repository_url>:latest
```

### 3. Deploiement Ansible

```bash
cd infra/ansible
cp inventory.ini.tmpl inventory.ini
```

Dans `inventory.ini`, remplacer `<INSTANCE_ID>` par `backend_instance_id` et `<SSM_TRANSFER_BUCKET>` par `ssm_transfer_bucket`. `playbook.yml` contient deja les valeurs (`ecr_repository_url`, `db_password_secret_id`, `rds_endpoint`) pour cet environnement - a mettre a jour uniquement si l'infra est recreee (ces valeurs changeraient).

```bash
ansible-playbook playbook.yml
```

### 4. Verification

```bash
curl -i http://<backend_public_ip>:8080/api/measures
```

Le premier demarrage peut prendre 1-2 minutes (l'image lance `gradlew bootRun`, qui telecharge les dependances Gradle).

## Debug

Connexion a l'instance (pas de SSH, uniquement SSM) :
```bash
aws ssm start-session --target <backend_instance_id>
```

Logs du conteneur, depuis la session :
```bash
sudo docker logs -f urbanhub-backend-1
```

## Redeploiement

Automatise par le pipeline CI/CD a chaque push sur `main` (jobs `docker` + `deploy`, cf. `docs/EC03-1-cicd.md`) : build/push de l'image taguee au SHA du commit, puis deploiement Ansible via SSM.

Procedure manuelle (fallback, ou pour un environnement sans CI) :

```bash
docker build --platform linux/amd64 -t <ecr_repository_url>:latest .
docker push <ecr_repository_url>:latest
cd infra/ansible && ansible-playbook playbook.yml
```

## Destruction

La RDS a une protection contre la suppression accidentelle, a desactiver avant le `destroy` :

```bash
aws rds modify-db-instance --db-instance-identifier urbanhub-exam --no-deletion-protection --apply-immediately
cd infra/terraform
terraform destroy 
```
