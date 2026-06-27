#!/usr/bin/env bash
# app-start.sh — avvia in locale TUTTE le applicazioni appgrove (auth-local, core, SPA backoffice).
#
# Risolve due fastidi ricorrenti:
#   1. `commons not found`: i servizi Quarkus dipendono dal modulo fratello `services/commons`, che va
#      installato in ~/.m2 PRIMA di lanciare `mvn -pl <servizio> quarkus:dev` (lo facciamo qui).
#   2. avvio/spegnimento manuale di 3 processi: qui partono in background con log e PID in dev/.run/.
#
# Uso:
#   ./app-start.sh              # infra (dev up) + seed + commons + auth-local + core + SPA
#   ./app-start.sh --no-infra   # salta `dev up` e `dev seed` (infra/seed già pronti)
#   ./app-start.sh --no-spa     # non avviare la SPA (solo backend)
#
# Stop:  ./app-stop.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$REPO_ROOT/dev/.run"
mkdir -p "$RUN_DIR"

# carica le porte/credenziali locali (dev/.env) per stampare URL accurati nel riepilogo
ENV_FILE="$REPO_ROOT/dev/.env"
if [ -f "$ENV_FILE" ]; then set -a; . "$ENV_FILE"; set +a; fi

INFRA=1
SPA=1
for arg in "$@"; do
  case "$arg" in
    --no-infra) INFRA=0 ;;
    --no-spa) SPA=0 ;;
    -h|--help) sed -n '2,18p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "opzione sconosciuta: $arg" >&2; exit 2 ;;
  esac
done

log()  { printf '\n\033[1;36m▶ %s\033[0m\n' "$*"; }
ok()   { printf '\033[0;32m✓ %s\033[0m\n' "$*"; }
warn() { printf '\033[0;33m! %s\033[0m\n' "$*"; }

# start_bg <name> <port> <cmd…> — avvia un processo in background se la porta è libera.
start_bg() {
  local name="$1" port="$2"; shift 2
  if lsof -ti "tcp:$port" >/dev/null 2>&1; then
    ok "$name già attivo su :$port (skip)"
    return 0
  fi
  log "Avvio $name (:$port) — log: dev/.run/$name.log"
  nohup "$@" >"$RUN_DIR/$name.log" 2>&1 &
  echo $! >"$RUN_DIR/$name.pid"
}

# wait_port <port> [timeout_s] — attende che qualcuno ascolti sulla porta.
wait_port() {
  local port="$1" timeout="${2:-120}" i=0
  while ! lsof -ti "tcp:$port" >/dev/null 2>&1; do
    i=$((i + 1))
    [ "$i" -ge "$timeout" ] && return 1
    sleep 1
  done
  return 0
}

# 1) infrastruttura + seed (idempotenti)
if [ "$INFRA" -eq 1 ]; then
  log "Infra locale (Postgres, Caddy, Mailpit, …)"
  "$REPO_ROOT/dev/dev" up
  log "Seed dati (migrazioni Flyway + utenti seed)"
  "$REPO_ROOT/dev/dev" seed
else
  warn "infra/seed saltati (--no-infra): assicurati che Postgres sia su e migrato/seedato"
fi

# 2) pom padre + commons → ~/.m2 (fix 'app.appgrove:commons:jar not found' / 'appgrove-services:pom not found')
#    `mvn -pl commons install` installa commons ma NON il pom aggregatore padre, che il pom di commons
#    referenzia: senza, core/auth-local falliscono a risolvere il descriptor di commons. Installiamo prima
#    il padre con `-N` (non ricorsivo), poi commons.
log "Installazione pom padre + commons in ~/.m2"
( cd "$REPO_ROOT/services" \
  && mvn -q -N install -DskipTests \
  && mvn -q -pl commons install -DskipTests )
ok "pom padre + commons installati"

# 3) backend Quarkus (dev mode)
#    auth-local (:9100) è avviato da `dev up` come jar con datasource+porta iniettati (dev/lib/common.sh):
#    qui NON lo rilanciamo. In --no-infra avvisiamo soltanto se non è su.
if [ "$INFRA" -eq 0 ] && ! lsof -ti tcp:9100 >/dev/null 2>&1; then
  warn "auth-local non è su :9100 — lancia ./app-start.sh senza --no-infra (lo avvia 'dev up')"
fi

#    core (:8080): in `%dev` userebbe DevServices (Postgres effimero via Docker) → DB vuoto/separato e
#    dipendenza da Docker. Lo puntiamo invece al Postgres CONDIVISO (stesso pattern di auth-local in
#    dev/lib/common.sh): schema `platform` già migrato + dati seed condivisi con auth-local. Niente
#    DevServices. `-Ddebug=5005` evita la collisione JDWP con altri processi quarkus:dev.
start_bg core 8080 bash -c "cd '$REPO_ROOT/services' && \
  QUARKUS_DATASOURCE_JDBC_URL='jdbc:postgresql://localhost:${POSTGRES_PORT:-5432}/${POSTGRES_DB}' \
  QUARKUS_DATASOURCE_USERNAME='${POSTGRES_USER}' \
  QUARKUS_DATASOURCE_PASSWORD='${POSTGRES_PASSWORD}' \
  QUARKUS_HTTP_PORT=8080 \
  exec mvn -pl core quarkus:dev -Ddebug=5005"

# 4) SPA Vite — backoffice (:5173) + admin (:5174)
if [ "$SPA" -eq 1 ]; then
  start_bg backoffice 5173 bash -c "cd '$REPO_ROOT/frontend' && exec npm run dev -w @appgrove/backoffice"
  start_bg admin 5174 bash -c "cd '$REPO_ROOT/frontend' && exec npm run dev -w @appgrove/admin"
fi

# 5) attesa readiness + riepilogo
log "Attendo l'avvio dei servizi…"
lsof -ti tcp:9100 >/dev/null 2>&1 && ok "auth-local pronto (:9100)" || warn "auth-local non su :9100 (avviato da 'dev up') — vedi dev/.auth-local.log"
wait_port 8080 && ok "core pronto (:8080)" || warn "core non pronto entro il timeout — vedi dev/.run/core.log"
if [ "$SPA" -eq 1 ]; then
  wait_port 5173 && ok "SPA backoffice pronta (:5173)" || warn "SPA backoffice non pronta — vedi dev/.run/backoffice.log"
  wait_port 5174 && ok "SPA admin pronta (:5174)" || warn "SPA admin non pronta — vedi dev/.run/admin.log"
fi

cat <<EOF

$(printf '\033[1;32m')Applicazioni avviate.$(printf '\033[0m')

  Applicazioni:
$( [ "$SPA" -eq 1 ] && echo "    • Backoffice (single-origin) .. https://app.local.appgrove.app    ← cliente (SPA + /api/* via Caddy)" )
$( [ "$SPA" -eq 1 ] && echo "    • Console admin (single-origin) https://admin.local.appgrove.app  ← platform-admin (login admin@appgrove.test)" )
$( [ "$SPA" -eq 1 ] && echo "    • Backoffice SPA (diretto) .... http://localhost:5173             (Vite; /api/* NON cablate qui)" )
$( [ "$SPA" -eq 1 ] && echo "    • Admin SPA (diretto) ......... http://localhost:5174             (Vite; /api/* NON cablate qui)" )
    • core API (diretto) .......... http://localhost:8080/api/platform/v1/
    • auth-local API (diretto) .... http://localhost:9100/api/auth/

  Utility / stack locale (Compose):
    • Mailpit (email) ....... http://localhost:${MAILPIT_UI_PORT:-8025}
    • MinIO console (S3) .... http://localhost:${MINIO_CONSOLE_PORT:-9001}   (user: ${MINIO_ROOT_USER:-appgrove})
    • MinIO API (S3) ........ http://localhost:${MINIO_API_PORT:-9000}
    • ElasticMQ (SQS) ....... http://localhost:${ELASTICMQ_PORT:-9324}
    • Postgres .............. localhost:${POSTGRES_PORT:-5432}  (db ${POSTGRES_DB:-appgrove}, user ${POSTGRES_USER:-appgrove})
    • Caddy proxy (HTTPS) ... app.local (backoffice) · admin.local (admin) · api.local (solo API)

  Utenti seed (password Password1!): owner@acme.test · admin@acme.test · member@acme.test · bob@bob.test
                                     · admin@appgrove.test (platform-admin → console admin)

  Log:  tail -f dev/.run/{core,backoffice,admin}.log  ·  auth-local: dev/.auth-local.log
  Stop: ./app-stop.sh

  Single-origin: ogni SPA e le sue API stanno sullo stesso origin via Caddy (→ SPA :5173/:5174,
  /api/auth/* :9100, /api/platform/* :8080): login/refresh col cookie funzionano.
EOF
