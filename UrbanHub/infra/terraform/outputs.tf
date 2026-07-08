output "backend_public_ip" {
  description = "IP publique de l'instance backend (URL de l'API exposée)"
  value       = aws_eip.backend.public_ip
}

output "backend_instance_id" {
  description = "ID de l'instance EC2 — utilisé dans l'inventaire Ansible (connexion SSM) et pour `aws ssm start-session --target`"
  value       = aws_instance.backend.id
}

output "ssm_transfer_bucket" {
  description = "Bucket S3 utilisé par le plugin de connexion Ansible aws_ssm pour le transfert de fichiers"
  value       = aws_s3_bucket.ssm_transfer.bucket
}

output "rds_endpoint" {
  description = "Endpoint RDS (host uniquement, sans le port) — à mettre dans les variables Ansible"
  value       = aws_db_instance.postgres.address
}

output "ecr_repository_url" {
  description = "URL du dépôt ECR — à utiliser pour docker build/push et dans le playbook Ansible"
  value       = aws_ecr_repository.backend.repository_url
}

output "db_password_secret_arn" {
  description = "ARN du secret Secrets Manager contenant le mot de passe RDS — à lire avec `aws secretsmanager get-secret-value`"
  value       = aws_secretsmanager_secret.db_password.arn
}

output "operator_access_key_id" {
  description = "Access key ID de l'utilisateur IAM least-privilege (à utiliser à la place du compte root)"
  value       = aws_iam_access_key.operator.id
}

output "operator_secret_access_key" {
  description = "Secret access key de l'utilisateur IAM least-privilege"
  value       = aws_iam_access_key.operator.secret
  sensitive   = true
}
