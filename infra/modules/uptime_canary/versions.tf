# Modulo `uptime_canary` (UC 0007): canary di uptime cross-region. Provider-
# agnostico — l'istanza in envs/prod lo aggancia a un provider aliasato in
# eu-central-1 (Francoforte), separato da eu-west-1, così l'allarme sopravvive a
# un outage regionale di eu-west-1 e può comunque avvisare (#08 G22).
terraform {
  required_version = ">= 1.9"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.7"
    }
  }
}
