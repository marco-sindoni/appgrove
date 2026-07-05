#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# _lib.sh — funzioni comuni degli script wrapper Terraform (#06 25).
# Non si lancia direttamente: viene "sourced" dagli altri script della cartella.
#
# Filosofia (#06 25): nell'uso quotidiano NON si lanciano comandi `terraform`
# grezzi — gli script incapsulano init/backend/guardrail. Chi vuole comunque
# usare terraform a mano trova qui la configurazione del backend.
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

# Radice di infra/ (gli script vivono in infra/scripts/).
INFRA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Costanti dell'iniziativa (#06 4/6): una regione sola, nomi fissi.
REGION="eu-west-1"
LOCK_TABLE="appgrove-tfstate-lock"

# Cache condivisa dei provider: evita di riscaricare il provider AWS (centinaia
# di MB) per ognuna delle 4 root Terraform.
export TF_PLUGIN_CACHE_DIR="${TF_PLUGIN_CACHE_DIR:-$HOME/.terraform.d/plugin-cache}"
mkdir -p "$TF_PLUGIN_CACHE_DIR"

# ── Output leggibile ─────────────────────────────────────────────────────────
C_RESET=$'\033[0m'; C_BLU=$'\033[1;36m'; C_GRN=$'\033[0;32m'; C_RED=$'\033[0;31m'; C_YEL=$'\033[0;33m'
info() { printf '%s▸ %s%s\n' "$C_BLU" "$*" "$C_RESET"; }
ok()   { printf '%s✓ %s%s\n' "$C_GRN" "$*" "$C_RESET"; }
warn() { printf '%s! %s%s\n' "$C_YEL" "$*" "$C_RESET"; }
die()  { printf '%s✗ %s%s\n' "$C_RED" "$*" "$C_RESET" >&2; exit 1; }

# ── Prerequisiti ─────────────────────────────────────────────────────────────
require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "comando mancante: $1 — installalo (es. \`brew install $2\`)"
}
require_terraform() { require_cmd terraform "hashicorp/tap/terraform"; }
require_aws() {
  require_cmd aws awscli
  aws sts get-caller-identity >/dev/null 2>&1 \
    || die "credenziali AWS assenti o scadute: configura AWS CLI (aws configure / SSO) e riprova"
}

# ── Backend dello state (bootstrap crea bucket+tabella; #06 4) ───────────────
aws_account_id() { aws sts get-caller-identity --query Account --output text; }
state_bucket()   { echo "appgrove-tfstate-$(aws_account_id)"; }

# Traduce l'argomento ambiente nella cartella dello stack.
# Ambienti validi: test | prod | global (bootstrap ha il suo script dedicato).
stack_dir() {
  case "$1" in
    global)     echo "$INFRA_DIR/global" ;;
    test|prod)  echo "$INFRA_DIR/envs/$1" ;;
    *)          die "ambiente sconosciuto: '$1' (usa: test | prod | global)" ;;
  esac
}

# `terraform init` con il backend S3 configurato (bucket con account-id nel nome:
# non può stare cablato nei .tf). Idempotente: si può rilanciare sempre.
tf_init() {
  local dir; dir="$(stack_dir "$1")"
  terraform -chdir="$dir" init -input=false -reconfigure \
    -backend-config="bucket=$(state_bucket)" \
    -backend-config="region=$REGION" \
    -backend-config="dynamodb_table=$LOCK_TABLE" \
    -backend-config="encrypt=true" \
    >/dev/null
  ok "init: backend s3://$(state_bucket) (lock: $LOCK_TABLE)"
}

# ── Guardrail prod (#06 25): conferma DIGITATA, mai auto-approve ─────────────
confirm_typed() {
  local expected="$1" prompt="$2" reply
  printf '%s⚠ %s%s\n' "$C_YEL" "$prompt" "$C_RESET"
  printf "  Digita '%s' per continuare: " "$expected"
  read -r reply
  [ "$reply" = "$expected" ] || die "conferma non corrispondente: operazione annullata"
}
