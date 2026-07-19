# shellcheck shell=bash
# dev service <app_id> — avvio selettivo: stack + auth + core + l'app indicata (UC 0009/0046).
# Alternativa leggera a `dev up`, che accende TUTTI i servizi scoperti: qui si paga solo l'app
# su cui si sta lavorando. L'app si indica con l'app-id (es. `fatture`) o col nome del servizio.

cmd_service() {
  if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<'EOF'
dev service <app_id> — avvia selettivamente lo stack + auth + core + l'app indicata, non tutti
i servizi. L'app-id è quello dichiarato in application.properties (appgrove.app-id); si accetta
anche il nome della directory in services/. Elenco: ./dev.sh services.
Dati: ricordati `./dev.sh seed` (migrazioni + seed) alla prima esecuzione.
Stop: ./app-stop.sh (oppure ./app-stop.sh --apps-only per lasciare su lo stack).
EOF
    return 0
  fi
  local app="${1:-}" svc role
  if [ -z "$app" ]; then
    err "uso: ./dev.sh service <app_id>"
    info "app disponibili: $(services_by_role app | tr '\n' ' ')"
    return 2
  fi
  svc="$(service_by_app_id "$app")"
  if [ -z "$svc" ]; then
    err "app sconosciuta: '$app'"
    info "app disponibili: $(services_by_role app | tr '\n' ' ')  (vedi ./dev.sh services)"
    return 2
  fi
  role="$(service_role "$svc")"

  ensure_env
  ensure_engine
  sync_caddy_routes
  step "avvio stack locale (Postgres, proxy, Mailpit, MinIO, ElasticMQ)"
  compose up -d
  caddy_reload_if_changed

  # auth + core sono il minimo comune: senza JWT e senza le API di piattaforma un'app non gira.
  local base
  while read -r base; do
    [ -n "$base" ] || continue
    svc_start "$base"
  done <<<"$(services_by_role auth; services_by_role core)"

  # …e poi l'app richiesta (se non è già uno dei due di base).
  case "$role" in
    auth|core) : ;;
    *) svc_start "$svc" ;;
  esac
  ok "avvio selettivo completato: auth + core + $svc"
}
