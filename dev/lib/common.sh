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

# ── auth-local (UC 0010): provider auth come PROCESSO host su :9100 (modello ibrido #11 §2) ──
AUTH_DIR="$DEV_DIR/auth"
AUTH_PRIV="$AUTH_DIR/privateKey.pem"
AUTH_PUB="$AUTH_DIR/publicKey.pem"
AUTH_PID="$DEV_DIR/.auth-local.pid"
AUTH_LOG="$DEV_DIR/.auth-local.log"
AUTH_JAR="$REPO_ROOT/services/auth-local/target/quarkus-app/quarkus-run.jar"
AUTH_PORT=9100

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

# ── auth-local lifecycle (UC 0010): host-process su :9100, raggiunto dal proxy via host.docker.internal ──
gen_jwt_keys() {
  [ -f "$AUTH_PRIV" ] && [ -f "$AUTH_PUB" ] && return 0
  require_cmd openssl || { warn "openssl assente: chiavi JWT non generate (auth-local non partirà)."; return 1; }
  mkdir -p "$AUTH_DIR"
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$AUTH_PRIV" 2>/dev/null \
    && openssl rsa -in "$AUTH_PRIV" -pubout -out "$AUTH_PUB" 2>/dev/null
}

auth_local_running() { [ -f "$AUTH_PID" ] && kill -0 "$(cat "$AUTH_PID")" 2>/dev/null; }

auth_local_start() {
  require_cmd java || { warn "java assente: salto auth-local (UC 0010)."; return 0; }
  if auth_local_running; then ok "auth-local già attivo (pid $(cat "$AUTH_PID"))"; return 0; fi
  if [ ! -f "$AUTH_PRIV" ] || [ ! -f "$AUTH_PUB" ]; then
    gen_jwt_keys || { warn "chiavi JWT mancanti: esegui ./dev.sh setup. auth-local non avviato."; return 0; }
  fi
  if [ ! -f "$AUTH_JAR" ]; then
    step "build auth-local (prima esecuzione)"
    ( cd "$REPO_ROOT/services" && mvn -q -pl auth-local -am -DskipTests package ) \
      || { warn "build auth-local fallita: avvio saltato."; return 0; }
  fi
  AUTH_LOCAL_PRIVATE_KEY="$AUTH_PRIV" AUTH_LOCAL_PUBLIC_KEY="$AUTH_PUB" \
  QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://localhost:${POSTGRES_PORT:-5432}/${POSTGRES_DB}" \
  QUARKUS_DATASOURCE_USERNAME="${POSTGRES_USER}" QUARKUS_DATASOURCE_PASSWORD="${POSTGRES_PASSWORD}" \
  QUARKUS_HTTP_PORT="$AUTH_PORT" \
  nohup java -jar "$AUTH_JAR" >"$AUTH_LOG" 2>&1 &
  echo $! > "$AUTH_PID"
  ok "auth-local avviato su :$AUTH_PORT (pid $(cat "$AUTH_PID"), log dev/.auth-local.log)"
}

auth_local_stop() {
  auth_local_running || { rm -f "$AUTH_PID"; return 0; }
  local pid; pid="$(cat "$AUTH_PID")"
  kill "$pid" 2>/dev/null && ok "auth-local fermato (pid $pid)"
  rm -f "$AUTH_PID"
}
