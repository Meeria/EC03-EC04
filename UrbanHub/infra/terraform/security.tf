resource "aws_security_group" "app" {
  name        = "${var.project_name}-app"
  description = "Backend UrbanHub : uniquement le port applicatif expose (pas de SSH - acces via SSM Session Manager)"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "API backend"
    from_port   = var.app_http_port
    to_port     = var.app_http_port
    protocol    = "tcp"
    cidr_blocks = [var.allow_app_from_cidr]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-app-sg"
  }
}

resource "aws_security_group" "db" {
  name        = "${var.project_name}-db"
  description = "RDS PostgreSQL : accessible uniquement depuis le SG applicatif"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "PostgreSQL depuis le backend"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-db-sg"
  }
}
