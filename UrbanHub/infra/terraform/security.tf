resource "aws_security_group" "app" {
  name        = "${var.project_name}-app"
  description = "Backend UrbanHub : uniquement le port applicatif exposé (pas de SSH — accès via SSM Session Manager)"
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
