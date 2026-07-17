# Configurazione tflint (#10 H). Regole base del linguaggio Terraform + ruleset
# AWS (plugin scaricato da `tflint --init`, eseguito da scripts/check e in CI —
# UC 0005; senza rete il check salta tflint con un warning).
plugin "terraform" {
  enabled = true
  preset  = "recommended"
}

plugin "aws" {
  enabled = true
  version = "0.38.0"
  source  = "github.com/terraform-linters/tflint-ruleset-aws"
}
