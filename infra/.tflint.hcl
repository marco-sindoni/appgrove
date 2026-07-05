# Configurazione tflint (#10 H). Regole base del linguaggio Terraform:
# il ruleset AWS (plugin, richiede `tflint --init` con rete) si aggiunge in CI (UC 0005).
plugin "terraform" {
  enabled = true
  preset  = "recommended"
}
