#!/usr/bin/env sh
# Ferma lo stack di sviluppo locale (UC 0008) — wrapper attorno a `docker compose down`.
#
# INTERIM: script di comodità versionato in attesa degli script ufficiali `dev/`
# (`dev down`/…) che arriveranno con UC 0009 e lo sostituiranno.
#
# Uso:
#   ./dev-stop.sh        # ferma i container (mantiene i dati nei volumi)
#   ./dev-stop.sh -v     # ferma + cancella i volumi (RESET dati: Postgres, MinIO, code)
set -eu
cd "$(dirname "$0")"

if ! docker info >/dev/null 2>&1; then
  echo "→ Docker daemon non attivo: niente da fermare."
  exit 0
fi

exec docker compose -f dev/docker-compose.yml --env-file dev/.env down "$@"
