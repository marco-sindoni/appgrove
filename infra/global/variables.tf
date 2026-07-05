variable "region" {
  description = "Regione AWS delle risorse regionali (#06 6: eu-west-1, cost-min)."
  type        = string
  default     = "eu-west-1"
}

variable "domain" {
  description = "Dominio unico dell'iniziativa, registrato su Route53 (#12 12)."
  type        = string
  default     = "appgrove.app"
}

variable "github_repo" {
  description = "Repository GitHub (owner/nome) autorizzato ad assumere i ruoli OIDC della CI (#07 25)."
  type        = string
  default     = "marco-sindoni/appgrove"
}
