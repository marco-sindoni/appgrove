#!/usr/bin/env bash
# app-start.sh — avvia e RIPRISTINA in locale TUTTO lo stack appgrove, in modo idempotente e
# auto-curante: rieseguibile in qualunque stato, converge a "tutto su e sano".
#
# Cosa fa (e ripara) ad ogni esecuzione:
#   1. container engine (Colima) attivo;                         [auto-start se giù]
#   2. certificati TLS locali (mkcert) presenti;                 [auto-genera se mancano]
#   3. stack Compose (Postgres, Caddy, Mailpit, MinIO, ElasticMQ) su e SANO, arch nativa
#      → ripara il proxy Caddy se non risponde (es. immagine amd64 emulata che va in panic);
#   4. seed idempotente (migrazioni Flyway + utenti seed);       [salta con --no-seed]
#   5. build dipendenze (pom padre + commons in ~/.m2, npm install frontend); [salta con --no-build]
#   6. auth-local (:9100), core (:8080), fatture (:8081);        [riavvia ciò che è morto]
#   7. SPA backoffice (:5173) + admin (:5174);                   [salta con --no-spa]
#   8. health-check END-TO-END via Caddy/HTTPS (SPA, auth, core, fatture).
#
# Uso:
#   ./app-start.sh            # tira su e ripara TUTTO
#   ./app-start.sh --no-spa   # solo backend (niente SPA Vite)
#   ./app-start.sh --no-seed  # non rieseguire il seed
#   ./app-start.sh --no-build # salta build commons / npm install (assumi già fatti)
#
# Stop:  ./app-stop.sh
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEV_DIR="$REPO_ROOT/dev"
export REPO_ROOT DEV_DIR
RUN_DIR="$DEV_DIR/.run"
mkdir -p "$RUN_DIR"

# Helper condivisi (ok/warn/step/info/err/die, compose/ensure_engine/ensure_env/certs/auth_local_start;
# carica anche dev/.env → POSTGRES_*, porte). Vedi dev/lib/common.sh.
# shellcheck source=dev/lib/common.sh
source "$DEV_DIR/lib/common.sh"
log() { step "$@"; }

SPA=1; SEED=1; BUILD=1
for arg in "$@"; do
  case "$arg" in
    --no-spa)   SPA=0 ;;
    --no-seed)  SEED=0 ;;
    --no-build) BUILD=0 ;;
    -h|--help)  sed -n '2,22p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) err "opzione sconosciuta: $arg"; exit 2 ;;
  esac
done

FAIL=0   # diventa 1 se un health-check end-to-end fallisce

# ── helper di processo ────────────────────────────────────────────────────────
# start_bg <name> <port> <cmd…> — avvia in background se la porta è libera (idempotente).
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

# wait_port <port> [timeout_s] — attende che qualcuno ascolti.
wait_port() {
  local port="$1" timeout="${2:-150}" i=0
  while ! lsof -ti "tcp:$port" >/dev/null 2>&1; do
    i=$((i + 1)); [ "$i" -ge "$timeout" ] && return 1; sleep 1
  done
  return 0
}

# ── arch nativa: evita immagini amd64 emulate (Caddy va in panic su Apple Silicon) ────────────
native_platform() {
  local a; a="$(docker version --format '{{.Server.Arch}}' 2>/dev/null || true)"
  case "$a" in
    arm64|aarch64) echo "linux/arm64"; return ;;
    amd64|x86_64)  echo "linux/amd64"; return ;;
  esac
  case "$(uname -m)" in
    arm64|aarch64) echo "linux/arm64" ;;
    *)             echo "linux/amd64" ;;
  esac
}

# ── salute del proxy Caddy: risponde su :443? (anche un 502 = Caddy vivo, upstream giù) ────────
proxy_alive() { curl -sk -o /dev/null --max-time 4 "https://app.local.appgrove.app/" >/dev/null 2>&1; }

ensure_proxy_healthy() {
  if proxy_alive; then ok "proxy Caddy attivo (:443)"; return 0; fi
  warn "proxy Caddy non risponde su :443 → ripristino con arch nativa ($PLATFORM)"
  docker rm -f appgrove-dev-proxy-1 >/dev/null 2>&1 || true
  docker pull --platform "$PLATFORM" caddy:2 >/dev/null 2>&1 \
    || warn "  pull caddy:2 ($PLATFORM) fallito (offline?) — riprovo comunque la create"
  compose up -d proxy >/dev/null 2>&1 || true
  local i=0; while ! proxy_alive; do i=$((i + 1)); [ "$i" -ge 25 ] && break; sleep 1; done
  if proxy_alive; then ok "proxy Caddy ripristinato"; else err "proxy Caddy ancora KO — vedi: docker logs appgrove-dev-proxy-1"; FAIL=1; fi
}

# ── attesa Postgres healthy ────────────────────────────────────────────────────
wait_pg() {
  local i=0
  until compose exec -T postgres pg_isready -U "${POSTGRES_USER:-appgrove}" -d "${POSTGRES_DB:-appgrove}" >/dev/null 2>&1; do
    i=$((i + 1)); [ "$i" -ge 40 ] && { warn "Postgres non healthy entro il timeout"; return 1; }
    sleep 1
  done
  ok "Postgres healthy"
}

# ── auth-local sempre su (idempotente, ricostruisce il jar se manca) ───────────
ensure_auth_local() {
  if port_in_use "$AUTH_PORT"; then ok "auth-local già attivo (:$AUTH_PORT)"; return 0; fi
  rm -f "$AUTH_PID"   # pid stantio (processo morto): ripulisci e riavvia
  auth_local_start
}

# ── health-check end-to-end via HTTPS (con retry: i servizi bootano lentamente) ─
probe() {  # <url> → stampa l'HTTP code; ritenta finché non è "pronto" (no 000/5xx)
  local url="$1" code i=0
  while :; do
    code="$(curl -sk -o /dev/null -w '%{http_code}' --max-time 5 "$url" 2>/dev/null || echo 000)"
    case "$code" in 000|5??) ;; *) break ;; esac
    i=$((i + 1)); [ "$i" -ge 30 ] && break; sleep 2
  done
  echo "$code"
}
report() {  # <label> <url> <mode: is200|reachable>
  local label="$1" code; code="$(probe "$2")"
  local good=0
  case "${3:-is200}" in
    is200)     [ "$code" = "200" ] && good=1 ;;
    reachable) case "$code" in ""|000|5??) good=0 ;; *) good=1 ;; esac ;;
  esac
  if [ "$good" -eq 1 ]; then ok "$label → HTTP $code"; else err "$label → HTTP ${code:-000}"; FAIL=1; fi
}

# ══════════════════════════════════════════════════════════════════════════════
# 1) container engine
ensure_engine
PLATFORM="$(native_platform)"
export DOCKER_DEFAULT_PLATFORM="$PLATFORM"   # ogni container creato userà l'arch nativa

# 2) ambiente + certificati TLS
ensure_env
if ! certs_present; then
  warn "certificati TLS assenti in dev/certs → li genero (mkcert)"
  gen_certs && ok "certificati generati" || warn "mkcert non disponibile: HTTPS via Caddy non funzionerà → ./dev.sh setup"
fi

# 3) stack Compose + proxy sano
log "Stack infra (Compose): Postgres · Caddy · Mailpit · MinIO · ElasticMQ"
compose up -d || die "docker compose up fallito (engine/arch?) — ./dev.sh doctor"
wait_pg
ensure_proxy_healthy

# 4) seed dati (idempotente)
if [ "$SEED" -eq 1 ]; then
  log "Seed dati (migrazioni Flyway + utenti seed)"
  "$REPO_ROOT/dev/dev" seed || warn "seed fallito (proseguo) — vedi output sopra"
fi

# 5) build dipendenze
if [ "$BUILD" -eq 1 ]; then
  log "Build dipendenze backend (pom padre + commons → ~/.m2)"
  ( cd "$REPO_ROOT/services" && mvn -q -N install -DskipTests && mvn -q -pl commons install -DskipTests ) \
    || die "build pom padre/commons fallita"
  ok "pom padre + commons installati"
  if [ ! -d "$REPO_ROOT/frontend/node_modules" ]; then
    log "Installazione dipendenze frontend (npm install)"
    ( cd "$REPO_ROOT/frontend" && npm install --no-fund --no-audit ) || warn "npm install fallito — le SPA potrebbero non partire"
  fi
fi

# 6) auth-local + backend
log "auth-local (:$AUTH_PORT)"
ensure_auth_local

#    core (:8080) e fatture (:8081) → Postgres CONDIVISO (no DevServices); debug 5005/5006 distinti.
start_bg core 8080 bash -c "cd '$REPO_ROOT/services' && \
  QUARKUS_DATASOURCE_JDBC_URL='jdbc:postgresql://localhost:${POSTGRES_PORT:-5432}/${POSTGRES_DB}' \
  QUARKUS_DATASOURCE_USERNAME='${POSTGRES_USER}' \
  QUARKUS_DATASOURCE_PASSWORD='${POSTGRES_PASSWORD}' \
  QUARKUS_HTTP_PORT=8080 \
  exec mvn -pl core quarkus:dev -Ddebug=5005"

start_bg fatture 8081 bash -c "cd '$REPO_ROOT/services' && \
  QUARKUS_DATASOURCE_JDBC_URL='jdbc:postgresql://localhost:${POSTGRES_PORT:-5432}/${POSTGRES_DB}' \
  QUARKUS_DATASOURCE_USERNAME='${POSTGRES_USER}' \
  QUARKUS_DATASOURCE_PASSWORD='${POSTGRES_PASSWORD}' \
  QUARKUS_HTTP_PORT=8081 \
  exec mvn -pl fatture quarkus:dev -Ddebug=5006"

# 7) SPA Vite
if [ "$SPA" -eq 1 ]; then
  start_bg backoffice 5173 bash -c "cd '$REPO_ROOT/frontend' && exec npm run dev -w @appgrove/backoffice"
  start_bg admin 5174 bash -c "cd '$REPO_ROOT/frontend' && exec npm run dev -w @appgrove/admin"
fi

# 8) readiness porte (boot Quarkus/Vite)
log "Attendo l'avvio dei processi…"
wait_port "$AUTH_PORT" 60 && ok "auth-local pronto (:$AUTH_PORT)" || warn "auth-local non su :$AUTH_PORT — vedi dev/.auth-local.log"
wait_port 8080 && ok "core pronto (:8080)" || warn "core non pronto entro il timeout — vedi dev/.run/core.log"
wait_port 8081 && ok "fatture pronto (:8081)" || warn "fatture non pronto entro il timeout — vedi dev/.run/fatture.log"
if [ "$SPA" -eq 1 ]; then
  wait_port 5173 && ok "SPA backoffice pronta (:5173)" || warn "SPA backoffice non pronta — vedi dev/.run/backoffice.log"
  wait_port 5174 && ok "SPA admin pronta (:5174)" || warn "SPA admin non pronta — vedi dev/.run/admin.log"
fi

# 9) health-check END-TO-END via Caddy/HTTPS (la verità che conta per il browser)
log "Verifica end-to-end (via Caddy / HTTPS)"
report "auth-local  /api/auth/jwks" "https://app.local.appgrove.app/api/auth/jwks" is200
report "core        /api/platform/v1/" "https://app.local.appgrove.app/api/platform/v1/" reachable
report "fatture     /api/fatture/v1/" "https://app.local.appgrove.app/api/fatture/v1/" reachable
if [ "$SPA" -eq 1 ]; then
  report "Backoffice  app.local" "https://app.local.appgrove.app/" is200
  report "Console admin admin.local" "https://admin.local.appgrove.app/" is200
fi

# 10) riepilogo
cat <<EOF

$( [ "$FAIL" -eq 0 ] && printf '\033[1;32mTutto su e sano.\033[0m' || printf '\033[1;33mAvvio completato con avvisi (vedi ✗ sopra).\033[0m' )

  Applicazioni:
$( [ "$SPA" -eq 1 ] && echo "    • Backoffice (single-origin) .. https://app.local.appgrove.app    ← cliente (SPA + /api/* via Caddy)" )
$( [ "$SPA" -eq 1 ] && echo "    • Console admin (single-origin) https://admin.local.appgrove.app  ← platform-admin (login admin@appgrove.test)" )
$( [ "$SPA" -eq 1 ] && echo "    • SPA dirette (no /api/*) ..... http://localhost:5173 · http://localhost:5174" )
    • core API (diretto) .......... http://localhost:8080/api/platform/v1/
    • fatture API (diretto) ....... http://localhost:8081/api/fatture/v1/   (app #1, UC 0051/0052)
    • auth-local API (diretto) .... http://localhost:9100/api/auth/

  Utility / stack locale (Compose):
    • Mailpit (email) ....... http://localhost:${MAILPIT_UI_PORT:-8025}
    • MinIO console (S3) .... http://localhost:${MINIO_CONSOLE_PORT:-9001}   (user: ${MINIO_ROOT_USER:-appgrove})
    • ElasticMQ (SQS) ....... http://localhost:${ELASTICMQ_PORT:-9324}
    • Postgres .............. localhost:${POSTGRES_PORT:-5432}  (db ${POSTGRES_DB:-appgrove}, user ${POSTGRES_USER:-appgrove})
    • Caddy proxy (HTTPS) ... app.local (backoffice) · admin.local (admin) · api.local (solo API)

  Utenti seed (password Password1!): owner@acme.test · admin@acme.test · member@acme.test · bob@bob.test
                                     · admin@appgrove.test (platform-admin → console admin)

  Log:  tail -f dev/.run/{core,fatture,backoffice,admin}.log  ·  auth-local: dev/.auth-local.log
  Stop: ./app-stop.sh
EOF

exit "$FAIL"
