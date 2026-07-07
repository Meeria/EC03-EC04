variable "region" {
  description = "Région AWS de déploiement"
  type        = string
  default     = "eu-west-3"
}

variable "project_name" {
  description = "Préfixe utilisé pour nommer les ressources AWS"
  type        = string
  default     = "urbanhub"
}

variable "environment" {
  description = "Nom de l'environnement (utilisé dans les noms de ressources et le chemin des secrets)"
  type        = string
  default     = "exam"
}

variable "allow_app_from_cidr" {
  description = "CIDR autorisé à accéder à l'API sur le port applicatif (0.0.0.0/0 = accès public, cohérent avec un service exposé)"
  type        = string
  default     = "0.0.0.0/0"
}

variable "instance_type" {
  description = "Type d'instance EC2 pour le backend"
  type        = string
  default     = "t3.micro"
}

variable "db_instance_class" {
  description = "Classe d'instance RDS"
  type        = string
  default     = "db.t3.micro"
}

variable "db_name" {
  description = "Nom de la base PostgreSQL"
  type        = string
  default     = "urbanhub"
}

variable "db_username" {
  description = "Utilisateur PostgreSQL applicatif"
  type        = string
  default     = "urbanhub"
}

variable "app_http_port" {
  description = "Port HTTP exposé par le backend"
  type        = number
  default     = 8080
}
