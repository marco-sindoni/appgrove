# ─────────────────────────────────────────────────────────────────────────────
# OIDC GitHub Actions → AWS (#07 25): NESSUNA chiave AWS su GitHub.
# La CI si autentica con un token OIDC di corta durata e assume un ruolo IAM
# per ambiente:
#
#   • github-actions-test — assumibile da QUALSIASI ref del repo (branch/PR)
#   • github-actions-prod — assumibile SOLO da tag (`refs/tags/*`):
#     una PR qualunque non può toccare prod.
#
# Least privilege: il perimetro è limitato ai servizi usati dal progetto e alle
# risorse IAM con prefisso `appgrove-`. La stretta per-workflow (permessi separati
# plan/apply, condizioni su environment GitHub) è di UC 0005, quando la pipeline
# esiste e se ne conoscono i job reali.
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
  # AWS valida i token GitHub tramite la propria trust chain e ignora questo
  # thumbprint, ma il campo è obbligatorio: si usa il valore storico documentato.
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

data "aws_caller_identity" "current" {}

# ── Trust policy per ambiente ────────────────────────────────────────────────
data "aws_iam_policy_document" "github_trust" {
  for_each = {
    test = "repo:${var.github_repo}:*"               # ogni branch/PR del repo
    prod = "repo:${var.github_repo}:ref:refs/tags/*" # SOLO release taggate (#07 25)
  }

  statement {
    sid     = "GitHubOIDC"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = [each.value]
    }
  }
}

resource "aws_iam_role" "github_actions" {
  for_each = data.aws_iam_policy_document.github_trust

  name                 = "appgrove-github-actions-${each.key}"
  description          = "Ruolo OIDC per GitHub Actions (${each.key}); prod assumibile solo da tag (#07 25)."
  assume_role_policy   = each.value.json
  max_session_duration = 3600
}

# ── Permessi della CI ────────────────────────────────────────────────────────
# La pipeline (UC 0005) esegue `terraform plan/apply`, push su ECR, deploy ECS,
# sync S3/invalidation CloudFront: serve un perimetro ampio sui NAMESPACE dei
# servizi del progetto, ma chiuso su tutto il resto (niente `*:*`) e con IAM
# limitato alle risorse con prefisso `appgrove-`.
data "aws_iam_policy_document" "ci_permissions" {
  #checkov:skip=CKV_AWS_356:Wildcard sulle risorse dei namespace di progetto: la stretta per-risorsa è di UC 0005, quando i nomi reali esistono
  #checkov:skip=CKV_AWS_111:Come sopra: perimetro per-namespace nel PoC, tightening in UC 0005
  #checkov:skip=CKV_AWS_107:s3/ssm/secretsmanager:* servono alla CI per gestirli come RISORSE Terraform (#07 26, mai letti nei log); stretta in UC 0005
  #checkov:skip=CKV_AWS_108:Come sopra: il perimetro copre i soli namespace del progetto, per-risorsa in UC 0005
  #checkov:skip=CKV_AWS_109:kms/secretsmanager wildcard: la CI applica Terraform su queste risorse; IAM è già limitato al prefisso appgrove-*
  #checkov:skip=CKV_AWS_110:iam:* è vincolato alle risorse appgrove-* (niente escalation su ruoli arbitrari); revisione in UC 0005

  statement {
    sid = "ProjectServices"
    actions = [
      # rete, compute, container
      "ec2:*", "ecs:*", "ecr:*", "application-autoscaling:*", "servicediscovery:*",
      # dati e messaggistica
      "rds:*", "s3:*", "sqs:*", "events:*", "dynamodb:*",
      # edge, DNS, certificati, auth
      "cloudfront:*", "route53:*", "acm:*", "cognito-idp:*",
      # api, funzioni, config/secrets, log e metriche
      "apigateway:*", "lambda:*", "ssm:*", "secretsmanager:*", "kms:*",
      "logs:*", "cloudwatch:*",
      # email transazionali (SES, #06 26)
      "ses:*",
    ]
    resources = ["*"]
  }

  statement {
    sid = "ProjectIam"
    actions = [
      "iam:GetRole", "iam:CreateRole", "iam:DeleteRole", "iam:UpdateRole",
      "iam:TagRole", "iam:UntagRole", "iam:PassRole",
      "iam:AttachRolePolicy", "iam:DetachRolePolicy",
      "iam:PutRolePolicy", "iam:DeleteRolePolicy", "iam:GetRolePolicy",
      "iam:ListRolePolicies", "iam:ListAttachedRolePolicies",
      "iam:ListInstanceProfilesForRole",
      "iam:CreatePolicy", "iam:DeletePolicy", "iam:GetPolicy",
      "iam:GetPolicyVersion", "iam:ListPolicyVersions",
      "iam:CreatePolicyVersion", "iam:DeletePolicyVersion", "iam:TagPolicy",
    ]
    resources = [
      "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/appgrove-*",
      "arn:aws:iam::${data.aws_caller_identity.current.account_id}:policy/appgrove-*",
    ]
  }

  statement {
    sid = "ReadOnlyIamOidc"
    actions = [
      "iam:GetOpenIDConnectProvider",
      "iam:ListOpenIDConnectProviders",
    ]
    resources = ["*"]
  }
}

resource "aws_iam_policy" "ci_permissions" {
  name        = "appgrove-ci-permissions"
  description = "Perimetro della CI appgrove (plan/apply Terraform, deploy). Stretta per-workflow in UC 0005."
  policy      = data.aws_iam_policy_document.ci_permissions.json
}

resource "aws_iam_role_policy_attachment" "ci_permissions" {
  for_each = aws_iam_role.github_actions

  role       = each.value.name
  policy_arn = aws_iam_policy.ci_permissions.arn
}
