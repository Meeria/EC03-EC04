data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_s3_bucket" "ssm_transfer" {
  bucket        = "${var.project_name}-${var.environment}-ssm-transfer"
  force_destroy = true

  tags = {
    Name = "${var.project_name}-ssm-transfer"
  }
}
