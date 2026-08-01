# shellcheck shell=bash
# Avvio HEADLESS dello stack backend locale — libreria condivisa (UC 0090).
#
# Estratta da tools/smoke/stack-headless.sh (change 0037) quando la suite end-to-end di
# piattaforma (tools/platform-e2e, UC 0090) ha avuto bisogno degli stessi passi: env dev,
# compose idempotente, migrazioni+seed, build degli artefatti in profilo dev, chiavi JWT
# locali, avvio dei servizi jar su porte alternative e attesa readiness.
#
# I due consumatori restano indipendenti (offset porte diversi, asserzioni proprie):
#   • tools/smoke/stack-headless.sh   — offset +10000, login end-to-end
#   • tools/platform-e2e/run.sh       — offset +12000, browser vero + Mailpit
#
# Come services.sh, questo file NON ha effetti collaterali al source: nessun docker,
# nessuna stampa. Ogni funzione ritorna ≠ 0 in caso di errore e lascia la diagnostica
# al chiamante. Richiede: REPO_ROOT (o lo deriva), dev/lib/services.sh già sorgibile.

: "${REPO_ROOT:=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
# shellcheck source=dev/lib/services.sh
source "$REPO_ROOT/dev/lib/services.sh"

# Timeout di attesa readiness (secondi), sovrascrivibile dal chiamante/CI.
BOOT_TIMEOUT="${BOOT_TIMEOUT:-120}"

# PID dei processi avviati da headless_start_service: il chiamante li ferma nel suo trap.
HEADLESS_PIDS=()

# headless_env — assicura dev/.env (in CI non esiste: si crea dall'esempio, stesso
# comportamento di ensure_env in dev/lib/common.sh) e ne esporta le variabili con i
# default dello stack (POSTGRES_*, MAILPIT_*, ELASTICMQ_PORT).
headless_env() {
  [ -f "$REPO_ROOT/dev/.env" ] || cp "$REPO_ROOT/dev/.env.example" "$REPO_ROOT/dev/.env" || return 1
  set -a
  # shellcheck disable=SC1091
  . "$REPO_ROOT/dev/.env"
  set +a
  export POSTGRES_PORT="${POSTGRES_PORT:-5433}" POSTGRES_DB="${POSTGRES_DB:-appgrove}"
  export POSTGRES_USER="${POSTGRES_USER:-appgrove}" POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-appgrove_local_dev}"
  export MAILPIT_SMTP_PORT="${MAILPIT_SMTP_PORT:-1025}" MAILPIT_UI_PORT="${MAILPIT_UI_PORT:-8025}"
  export ELASTICMQ_PORT="${ELASTICMQ_PORT:-9324}"
}

# headless_compose_up <servizio…> — container infrastrutturali dal compose dev
# (idempotente: convive con lo stack di sviluppo acceso).
headless_compose_up() {
  ( cd "$REPO_ROOT/dev" && docker compose up -d --wait "$@" )
}

# headless_postgres_container — nome del container Postgres del compose dev.
headless_postgres_container() {
  docker ps --format '{{.Names}}' | grep -m1 postgres
}

# headless_migrate_seed — gli STESSI passi di app-start: Flyway migrate + seed idempotente.
headless_migrate_seed() {
  "$REPO_ROOT/dev.sh" migrate || return 1
  docker exec -i "$(headless_postgres_container)" \
    psql -q -U "$POSTGRES_USER" -d "$POSTGRES_DB" < "$REPO_ROOT/dev/seed/seed.sql"
}

# headless_build_artifacts — impacchetta TUTTI i servizi scoperti con PROFILO DI BUILD dev:
# il wiring a build-time deve essere quello di sviluppo (es. payment provider = stub, non
# Paddle) come in `quarkus:dev` — un jar impacchettato di default è "prod" a build-time e in
# dev esploderebbe sul PaddlePaymentProvider non implementato (gated, UC 0022).
# Nota: sovrascrive i jar in target/ — chi serve l'artefatto di spedizione fa la propria build.
headless_build_artifacts() {
  local modules
  modules="$(discover_services | cut -f1 | paste -sd, -)"
  ( cd "$REPO_ROOT/services" && mvn -B -q -pl "$modules" -am package -DskipTests -Dquarkus.profile=dev )
}

# headless_auth_keys <dir_fallback> — chiavi di firma JWT locali: quelle di dev/auth se
# esistono, altrimenti generate usa-e-getta nella dir indicata. Stampa la dir scelta.
headless_auth_keys() {
  local keys="$REPO_ROOT/dev/auth"
  if [ ! -f "$keys/privateKey.pem" ]; then
    keys="$1/auth-keys"; mkdir -p "$keys" || return 1
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$keys/privateKey.pem" 2>/dev/null || return 1
    openssl rsa -in "$keys/privateKey.pem" -pubout -out "$keys/publicKey.pem" 2>/dev/null || return 1
  fi
  printf '%s\n' "$keys"
}

# headless_start_service <nome> <porta> <logfile> [ENV=val…] — avvia il jar impacchettato in
# profilo dev sul Postgres del compose; il PID finisce in HEADLESS_PIDS (teardown del chiamante).
headless_start_service() {
  local name="$1" port="$2" log="$3"; shift 3
  env "$@" \
      QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://localhost:${POSTGRES_PORT}/${POSTGRES_DB}" \
      QUARKUS_DATASOURCE_USERNAME="$POSTGRES_USER" \
      QUARKUS_DATASOURCE_PASSWORD="$POSTGRES_PASSWORD" \
      QUARKUS_HTTP_PORT="$port" \
      java -Dquarkus.profile=dev -jar "$REPO_ROOT/services/$name/target/quarkus-app/quarkus-run.jar" \
      > "$log" 2>&1 &
  HEADLESS_PIDS+=($!)
}

# headless_wait_http <nome> <url> <status atteso> [logfile] — polling readiness fino a
# BOOT_TIMEOUT secondi; al timeout stampa causa radice + coda del log (se fornito).
headless_wait_http() {
  local name="$1" url="$2" expected="$3" log="${4:-}" i=0 code=""
  while [ "$i" -lt "$BOOT_TIMEOUT" ]; do
    code="$(curl -s -o /dev/null -w '%{http_code}' "$url" 2>/dev/null)" || true
    [ "$code" = "$expected" ] && return 0
    sleep 1; i=$((i + 1))
  done
  printf '✗ %s: %s → atteso %s, ottenuto %s (timeout %ss)\n' \
    "$name" "$url" "$expected" "'${code:-nessuna risposta}'" "$BOOT_TIMEOUT" >&2
  if [ -n "$log" ]; then
    grep -B2 -A8 "Caused by" "$log" 2>/dev/null | head -30 >&2
    tail -15 "$log" 2>/dev/null >&2
  fi
  return 1
}

# headless_service_ready_url <ruolo> <porta> — URL di readiness per ruolo: auth espone il
# JWKS, core e le app la health readiness di Quarkus.
headless_service_ready_url() {
  case "$1" in
    auth) printf 'http://localhost:%s/api/auth/jwks\n' "$2" ;;
    *)    printf 'http://localhost:%s/q/health/ready\n' "$2" ;;
  esac
}
