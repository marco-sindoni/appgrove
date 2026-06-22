#!/usr/bin/env sh
# Avvia lo stack di sviluppo locale (UC 0008) — wrapper attorno a `docker compose`.
#
# INTERIM: script di comodità versionato in attesa degli script ufficiali `dev/`
# (`dev up`/`dev setup`/`dev doctor`/…) che arriveranno con UC 0009 e lo sostituiranno.
#
# Uso:
#   ./dev-start.sh            # default: up -d (avvia in background)
#   ./dev-start.sh up         # avvia in foreground (log a schermo)
#   ./dev-start.sh ps         # stato container
#   ./dev-start.sh logs -f    # segui i log
#   ./dev-start.sh <qualsiasi sottocomando di docker compose>
set -eu
cd "$(dirname "$0")"

# 1) config locale: crea dev/.env dal template se manca
[ -f dev/.env ] || { cp dev/.env.example dev/.env; echo "→ creato dev/.env da dev/.env.example"; }

# 2) engine: se il daemon non risponde, prova ad avviare Colima
if ! docker info >/dev/null 2>&1; then
  if command -v colima >/dev/null 2>&1; then
    echo "→ Docker daemon non attivo: avvio Colima..."
    colima start
  else
    echo "✗ Docker daemon non raggiungibile e Colima non installato." >&2
    exit 1
  fi
fi

# 3) default = up -d
[ "$#" -eq 0 ] && set -- up -d

exec docker compose -f dev/docker-compose.yml --env-file dev/.env "$@"
