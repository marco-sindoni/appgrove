#!/usr/bin/env bash
# app-stop.sh — ferma TUTTO lo stack appgrove avviato da app-start.sh.
#
# Di default spegne sia le applicazioni (tutti i servizi scoperti in services/* + le SPA)
# SIA lo stack Compose
# (Postgres, Caddy, Mailpit, MinIO, ElasticMQ). I dati restano (i volumi NON vengono cancellati).
#
# Uso:
#   ./app-stop.sh             # ferma TUTTO (app + stack Compose; dati preservati)
#   ./app-stop.sh --apps-only # ferma solo le app host (lascia su Postgres/Caddy/…)
#   ./app-stop.sh --wipe      # ferma tutto E cancella i volumi (reset dati: Postgres/MinIO/code)
#
# Stop "per porta" (robusto su macOS): termina qualunque processo ascolti sulle porte note,
# inclusi i figli java spawnati da `mvn quarkus:dev`. L'elenco dei servizi e le loro porte
# sono SCOPERTI da services/* (dev/lib/services.sh): niente nomi scritti a mano qui.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEV_DIR="$REPO_ROOT/dev"
export REPO_ROOT DEV_DIR
RUN_DIR="$DEV_DIR/.run"

# helper condivisi (ok/warn/err, compose, auth_stop, AUTH_PID/AUTH_PORT; carica dev/.env)
# shellcheck source=dev/lib/common.sh
source "$DEV_DIR/lib/common.sh"

APPS_ONLY=0; WIPE=0
for arg in "$@"; do
  case "$arg" in
    --apps-only) APPS_ONLY=1 ;;
    --wipe|-v)   WIPE=1 ;;
    --infra)     : ;; # retrocompat: lo stack ora si ferma di default (no-op)
    -h|--help)   sed -n '2,13p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) err "opzione sconosciuta: $arg"; exit 2 ;;
  esac
done

# stop_port: vive in dev/lib/common.sh come proc_stop (condiviso con dev up / dev service).
stop_port() { proc_stop "$@"; }

step "Stop applicazioni host"
while read -r svc; do
  [ -n "$svc" ] || continue
  svc_stop "$svc"          # auth include il pidfile dedicato (dev/.auth.pid)
done <<<"$(services_startup_order)"
stop_port backoffice 5173
stop_port admin 5174

if [ "$APPS_ONLY" -eq 1 ]; then
  ok "App fermate. Stack Compose lasciato attivo (--apps-only)."
  exit 0
fi

step "Stop stack Compose"
if [ "$WIPE" -eq 1 ]; then
  warn "--wipe: cancello anche i volumi (Postgres/MinIO/code) → reset dati"
  compose down -v || warn "compose down -v fallito"
  ok "stack fermato e volumi cancellati"
else
  compose down || warn "compose down fallito"
  ok "stack fermato (volumi/dati preservati)"
fi
