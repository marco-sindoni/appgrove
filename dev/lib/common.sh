# shellcheck shell=bash
# Helper condivisi per gli script dev/ (UC 0009). Sorgiato dal dispatcher `dev/dev`.

: "${DEV_DIR:=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
: "${REPO_ROOT:=$(cd "$DEV_DIR/.." && pwd)}"

# --- colori / log ---
if [ -t 1 ] && [ -z "${NO_COLOR:-}" ]; then
  C_RESET=$'\033[0m'; C_RED=$'\033[31m'; C_GRN=$'\033[32m'; C_YEL=$'\033[33m'; C_BLU=$'\033[34m'
else
  C_RESET=''; C_RED=''; C_GRN=''; C_YEL=''; C_BLU=''
fi
info() { printf '%s\n' "$*"; }
step() { printf '%s▶ %s%s\n' "$C_BLU" "$*" "$C_RESET"; }
ok()   { printf '%s✓ %s%s\n' "$C_GRN" "$*" "$C_RESET"; }
warn() { printf '%s! %s%s\n' "$C_YEL" "$*" "$C_RESET" >&2; }
err()  { printf '%s✗ %s%s\n' "$C_RED" "$*" "$C_RESET" >&2; }
die()  { err "$*"; exit 1; }

# --- costanti ambiente locale (stack UC 0008) ---
COMPOSE_FILE="$DEV_DIR/docker-compose.yml"
ENV_FILE="$DEV_DIR/.env"
ENV_EXAMPLE="$DEV_DIR/.env.example"
CERT_DIR="$DEV_DIR/certs"
CERT_CRT="$CERT_DIR/local.appgrove.app.pem"
CERT_KEY="$CERT_DIR/local.appgrove.app-key.pem"
DOMAINS=(local.appgrove.app app.local.appgrove.app admin.local.appgrove.app api.local.appgrove.app)

# Carica le porte da dev/.env (se presente) così doctor riflette la config reale.
if [ -f "$ENV_FILE" ]; then
  set -a; # shellcheck disable=SC1090
  . "$ENV_FILE"; set +a
fi
# porte host pubblicate dallo stack (per il check di doctor)
HOST_PORTS=(
  "${PROXY_HTTP_PORT:-80}" "${PROXY_HTTPS_PORT:-443}" "${POSTGRES_PORT:-5432}"
  "${MAILPIT_SMTP_PORT:-1025}" "${MAILPIT_UI_PORT:-8025}"
  "${MINIO_API_PORT:-9000}" "${MINIO_CONSOLE_PORT:-9001}" "${ELASTICMQ_PORT:-9324}"
)
# prerequisiti per i processi-app (Quarkus dev / Vite), usati più avanti
APP_PREREQS=(node java mvn psql)

require_cmd() { command -v "$1" >/dev/null 2>&1; }

# wrapper compose con file ed env precompilati
compose() { docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" "$@"; }

engine_up() { docker info >/dev/null 2>&1; }

ensure_engine() {
  engine_up && return 0
  if require_cmd colima; then
    step "Docker daemon non attivo: avvio Colima..."
    colima start
  else
    die "Docker daemon non raggiungibile e Colima non installato (vedi: ./dev.sh doctor)."
  fi
}

ensure_env() {
  [ -f "$ENV_FILE" ] && return 0
  cp "$ENV_EXAMPLE" "$ENV_FILE"
  ok "creato dev/.env da dev/.env.example"
}

# Generazione certificati: JAVA_HOME disattivato per evitare lo snag keytool di mkcert
# (la firma è comunque della CA locale; lo store Java si popola con `mkcert -install`).
gen_certs() {
  env -u JAVA_HOME mkcert -cert-file "$CERT_CRT" -key-file "$CERT_KEY" "${DOMAINS[@]}" >/dev/null 2>&1
}

certs_present() { [ -f "$CERT_CRT" ] && [ -f "$CERT_KEY" ]; }

port_in_use() { lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1; }

host_mapped() { grep -qiE "^[[:space:]]*127\.0\.0\.1[[:space:]]+.*[[:space:]]$1([[:space:]]|\$)" /etc/hosts 2>/dev/null; }
