# ─────────────────────────────────────────────────────────────────────────────
# Stack `global` — risorse condivise tra gli ambienti (#06 5):
# zona Route53, certificati ACM, ruoli OIDC per la CI (GitHub Actions).
# State: s3://<state-bucket>/global/terraform.tfstate (bucket creato dal bootstrap).
# Il backend è configurato dagli script wrapper via -backend-config (il nome del
# bucket contiene l'account ID, quindi non può essere cablato qui).
# ─────────────────────────────────────────────────────────────────────────────

terraform {
  required_version = ">= 1.9"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  backend "s3" {
    # bucket/region/dynamodb_table arrivano da `infra/scripts/_lib.sh` (-backend-config);
    # la chiave dello state è fissa e distinta per stack.
    key = "global/terraform.tfstate"
  }
}

provider "aws" {
  region = var.region
  default_tags {
    tags = {
      project    = "appgrove"
      stack      = "global"
      managed_by = "terraform"
    }
  }
}

# CloudFront accetta SOLO certificati emessi in us-east-1 (vincolo AWS, #06 17):
# provider con alias dedicato per i certificati edge.
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"
  default_tags {
    tags = {
      project    = "appgrove"
      stack      = "global"
      managed_by = "terraform"
    }
  }
}
