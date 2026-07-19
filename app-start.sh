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
#   6. tutti i servizi backend SCOPERTI in services/* (auth, core, app);  [riavvia ciò che è morto]
#   7. SPA backoffice (:5173) + admin (:5174);                   [salta con --no-spa]
#   8. health-check END-TO-END via Caddy/HTTPS (SPA + ogni servizio).
#
# Nessun servizio è scritto a mano: l'elenco (nome, porta, rotta /api/*) è DERIVATO dai
# rispettivi application.properties — vedi dev/lib/services.sh e `./dev.sh services`.
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

# Helper condivisi (ok/warn/step/info/err/die, compose/ensure_engine/ensure_env/certs/auth_start;
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
BASE_URL="https://app.local.appgrove.app"   # origin del backoffice: SPA + /api/* via Caddy

# start_bg/wait_port (avvio background idempotente per porta) vivono in dev/lib/common.sh
# come proc_start/wait_port: li condividono dev up, dev service e app-stop.sh.
start_bg() { proc_start "$@"; }

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

# 3) stack Compose + proxy sano (rotte /api/* rigenerate dalla scoperta prima di avviare Caddy)
sync_caddy_routes
log "Stack infra (Compose): Postgres · Caddy · Mailpit · MinIO · ElasticMQ"
compose up -d || die "docker compose up fallito (engine/arch?) — ./dev.sh doctor"
wait_pg
caddy_reload_if_changed
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

# 6) servizi backend scoperti in services/* → Postgres CONDIVISO (no DevServices).
#    auth parte dal jar con le chiavi JWT; core e le app in `quarkus:dev` con debugger
#    su porte distinte (5005 + offset della porta http). Vedi svc_start in dev/lib/common.sh.
log "Servizi backend: $(services_startup_order | tr '\n' ' ')"
while read -r svc; do
  [ -n "$svc" ] || continue
  svc_start "$svc"
done <<<"$(services_startup_order)"

# 7) SPA Vite
if [ "$SPA" -eq 1 ]; then
  start_bg backoffice 5173 bash -c "cd '$REPO_ROOT/frontend' && exec npm run dev -w @appgrove/backoffice"
  start_bg admin 5174 bash -c "cd '$REPO_ROOT/frontend' && exec npm run dev -w @appgrove/admin"
fi

# 8) readiness porte (boot Quarkus/Vite)
log "Attendo l'avvio dei processi…"
while IFS=$'\t' read -r svc app_id port schema role; do
  if [ "$role" = auth ]; then
    wait_port "$port" 60 && ok "$svc pronto (:$port)" || warn "$svc non su :$port — vedi dev/.auth.log"
  else
    wait_port "$port" && ok "$svc pronto (:$port)" || warn "$svc non pronto entro il timeout — vedi dev/.run/$svc.log"
  fi
done < <(discover_services)
if [ "$SPA" -eq 1 ]; then
  wait_port 5173 && ok "SPA backoffice pronta (:5173)" || warn "SPA backoffice non pronta — vedi dev/.run/backoffice.log"
  wait_port 5174 && ok "SPA admin pronta (:5174)" || warn "SPA admin non pronta — vedi dev/.run/admin.log"
fi

# 9) health-check END-TO-END via Caddy/HTTPS (la verità che conta per il browser)
log "Verifica end-to-end (via Caddy / HTTPS)"
# Percorso di sonda per ruolo: auth espone il JWKS (deve dare 200); core e le app rispondono
# sul prefisso di rotta (basta che siano raggiungibili: 401/404 = servizio vivo dietro Caddy).
while IFS=$'\t' read -r svc app_id port schema role; do
  case "$role" in
    auth) report "$(printf '%-9s /api/auth/jwks' "$svc")" "$BASE_URL/api/auth/jwks" is200 ;;
    core) report "$(printf '%-9s /api/platform/v1/' "$svc")" "$BASE_URL/api/platform/v1/" reachable ;;
    *)    report "$(printf '%-9s /api/%s/v1/' "$svc" "$app_id")" "$BASE_URL/api/$app_id/v1/" reachable ;;
  esac
done < <(discover_services)
if [ "$SPA" -eq 1 ]; then
  report "Backoffice  app.local" "https://app.local.appgrove.app/" is200
  report "Console admin admin.local" "https://admin.local.appgrove.app/" is200
fi

# 10) riepilogo
# L'elenco delle API si calcola PRIMA del riepilogo: dentro un heredoc `$'\t'` non è quoting
# ANSI-C e la sostituzione di comando non si parserebbe.
# `if/elif` e non `case`: bash 3.2 (quello di serie su macOS) sbaglia a chiudere `$( … )` sulla
# parentesi di un pattern `case`, e il blocco finirebbe nel riepilogo come testo grezzo.
API_LINES="$(
  while IFS=$'\t' read -r svc app_id port schema role; do
    if [ "$role" = auth ]; then
      printf '    • %-10s http://localhost:%s/api/auth/\n' "$svc" "$port"
    elif [ "$role" = core ]; then
      printf '    • %-10s http://localhost:%s/api/platform/v1/\n' "$svc" "$port"
    else
      printf '    • %-10s http://localhost:%s/api/%s/v1/\n' "$svc" "$port" "$app_id"
    fi
  done < <(discover_services)
)"

cat <<EOF

$( [ "$FAIL" -eq 0 ] && printf '\033[1;32mTutto su e sano.\033[0m' || printf '\033[1;33mAvvio completato con avvisi (vedi ✗ sopra).\033[0m' )

  Applicazioni:
$( [ "$SPA" -eq 1 ] && echo "    • Backoffice (single-origin) .. https://app.local.appgrove.app    ← cliente (SPA + /api/* via Caddy)" )
$( [ "$SPA" -eq 1 ] && echo "    • Console admin (single-origin) https://admin.local.appgrove.app  ← platform-admin (login admin@appgrove.test)" )
$( [ "$SPA" -eq 1 ] && echo "    • SPA dirette (no /api/*) ..... http://localhost:5173 · http://localhost:5174" )
  API backend (dirette, senza proxy):
$API_LINES

  Utility / stack locale (Compose):
    • Mailpit (email) ....... http://localhost:${MAILPIT_UI_PORT:-8025}
    • MinIO console (S3) .... http://localhost:${MINIO_CONSOLE_PORT:-9001}   (user: ${MINIO_ROOT_USER:-appgrove})
    • ElasticMQ (SQS) ....... http://localhost:${ELASTICMQ_PORT:-9324}
    • Postgres .............. localhost:${POSTGRES_PORT:-5432}  (db ${POSTGRES_DB:-appgrove}, user ${POSTGRES_USER:-appgrove})
    • Caddy proxy (HTTPS) ... app.local (backoffice) · admin.local (admin) · api.local (solo API)

  Utenti seed (password Password1!): owner@acme.test · admin@acme.test · member@acme.test · bob@bob.test
                                     · admin@appgrove.test (platform-admin → console admin)

  Log:  tail -f dev/.run/*.log  ·  auth: dev/.auth.log  ·  mappa servizi: ./dev.sh services
  Stop: ./app-stop.sh
EOF

exit "$FAIL"
