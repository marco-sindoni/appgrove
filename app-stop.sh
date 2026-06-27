#!/usr/bin/env bash
# app-stop.sh — ferma le applicazioni appgrove avviate da app-start.sh (auth-local, core, SPA).
#
# Uso:
#   ./app-stop.sh           # ferma solo le app (auth-local :9100, core :8080, SPA :5173)
#   ./app-stop.sh --infra   # ferma anche lo stack infra (`dev down`: Postgres/Caddy/Mailpit…)
#
# Stop "per porta" (robusto su macOS): termina qualunque processo ascolti su quelle porte,
# inclusi i figli java spawnati da `mvn quarkus:dev`.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$REPO_ROOT/dev/.run"

INFRA=0
for arg in "$@"; do
  case "$arg" in
    --infra) INFRA=1 ;;
    -h|--help) sed -n '2,9p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "opzione sconosciuta: $arg" >&2; exit 2 ;;
  esac
done

ok()   { printf '\033[0;32m✓ %s\033[0m\n' "$*"; }
warn() { printf '\033[0;33m! %s\033[0m\n' "$*"; }

stop_port() {
  local name="$1" port="$2"
  local pids
  pids="$(lsof -ti "tcp:$port" 2>/dev/null || true)"
  if [ -z "$pids" ]; then
    warn "$name: niente in ascolto su :$port"
  else
    # shellcheck disable=SC2086
    kill $pids 2>/dev/null || true
    sleep 2
    pids="$(lsof -ti "tcp:$port" 2>/dev/null || true)"
    # shellcheck disable=SC2086
    [ -n "$pids" ] && kill -9 $pids 2>/dev/null || true
    ok "$name fermato (:$port)"
  fi
  rm -f "$RUN_DIR/$name.pid"
}

stop_port auth-local 9100
stop_port core 8080
stop_port backoffice 5173
stop_port admin 5174

if [ "$INFRA" -eq 1 ]; then
  printf '\n'
  "$REPO_ROOT/dev/dev" down
fi
