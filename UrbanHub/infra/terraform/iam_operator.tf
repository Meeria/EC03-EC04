resource "aws_iam_policy" "operator_least_privilege" {
  name        = "${var.project_name}-operator-least-privilege"
  description = "Politique cible pour l'operateur Terraform/Ansible (principe du moindre privilege). Non attachee a un utilisateur : voir docs/EC04-2-securite.md."

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "InfraProvisioningRegionOnly"
        Effect    = "Allow"
        Action    = ["ec2:*", "rds:*", "s3:*", "ecr:*"]
        Resource  = "*"
        Condition = {
          StringEquals = { "aws:RequestedRegion" = var.region }
        }
      },
      {
        Sid      = "SecretsManagerUrbanhubOnly"
        Effect   = "Allow"
        Action   = [
          "secretsmanager:CreateSecret",
          "secretsmanager:DeleteSecret",
          "secretsmanager:PutSecretValue",
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
          "secretsmanager:GetResourcePolicy",
          "secretsmanager:TagResource"
        ]
        Resource = "arn:aws:secretsmanager:${var.region}:*:secret:${var.project_name}/*"
      },
      {
        Sid      = "IamRoleForEc2InstanceOnly"
        Effect   = "Allow"
        Action   = [
          "iam:CreateRole", "iam:DeleteRole", "iam:GetRole",
          "iam:PutRolePolicy", "iam:DeleteRolePolicy", "iam:GetRolePolicy", "iam:ListRolePolicies",
          "iam:AttachRolePolicy", "iam:DetachRolePolicy", "iam:ListAttachedRolePolicies",
          "iam:CreateInstanceProfile", "iam:DeleteInstanceProfile",
          "iam:AddRoleToInstanceProfile", "iam:RemoveRoleFromInstanceProfile",
          "iam:GetInstanceProfile", "iam:TagRole", "iam:PassRole"
        ]
        Resource = "arn:aws:iam::*:role/${var.project_name}-*"
      },
      {
        Sid      = "IamPolicyForOperatorOnly"
        Effect   = "Allow"
        Action   = [
          "iam:CreatePolicy", "iam:DeletePolicy", "iam:GetPolicy",
          "iam:GetPolicyVersion", "iam:ListPolicyVersions",
          "iam:CreatePolicyVersion", "iam:DeletePolicyVersion", "iam:TagPolicy"
        ]
        Resource = "arn:aws:iam::*:policy/${var.project_name}-*"
      },
      {
        Sid      = "IamUserForOperatorOnly"
        Effect   = "Allow"
        Action   = [
          "iam:CreateUser", "iam:DeleteUser", "iam:GetUser", "iam:TagUser",
          "iam:CreateAccessKey", "iam:DeleteAccessKey", "iam:ListAccessKeys", "iam:UpdateAccessKey",
          "iam:AttachUserPolicy", "iam:DetachUserPolicy", "iam:ListAttachedUserPolicies"
        ]
        Resource = "arn:aws:iam::*:user/${var.project_name}-*"
      },
      {
        Sid      = "CloudWatchLogsForBackend"
        Effect   = "Allow"
        Action   = [
          "logs:CreateLogGroup",
          "logs:DeleteLogGroup",
          "logs:PutRetentionPolicy",
          "logs:TagResource",
          "logs:ListTagsForResource"
        ]
        Resource = "arn:aws:logs:${var.region}:*:log-group:/${var.project_name}/*"
      },
      {
        Sid      = "CloudWatchLogsDescribe"
        Effect   = "Allow"
        Action   = ["logs:DescribeLogGroups"]
        Resource = "*"
      },
      {
        Sid      = "SsmSessionForDeployment"
        Effect   = "Allow"
        Action   = [
          "ssm:StartSession", "ssm:TerminateSession",
          "ssm:DescribeInstanceInformation", "ssm:GetConnectionStatus"
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_user" "operator" {
  name = "${var.project_name}-operator"
}

resource "aws_iam_user_policy_attachment" "operator" {
  user       = aws_iam_user.operator.name
  policy_arn = aws_iam_policy.operator_least_privilege.arn
}

resource "aws_iam_access_key" "operator" {
  user = aws_iam_user.operator.name
}
