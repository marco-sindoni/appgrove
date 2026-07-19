# shellcheck shell=bash
# dev up — avvia lo stack locale (UC 0008/0009) e i processi-app scoperti (UC 0046).

cmd_up() {
  if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<'EOF'
dev up [args] — avvia lo stack locale in background (docker compose up -d) e i servizi backend
scoperti in services/* (auth, core e le app: vedi ./dev.sh services).
Argomenti extra passati a compose (es. --build). Crea dev/.env e avvia l'engine se serve.
Per accendere solo core + una singola app: ./dev.sh service <app_id>.
EOF
    return 0
  fi
  ensure_env
  ensure_engine
  # Rotte /api/* del proxy rigenerate dalla scoperta PRIMA di avviare Caddy (UC 0046).
  sync_caddy_routes
  step "avvio stack locale"
  compose up -d "$@"
  caddy_reload_if_changed
  ok "stack su"
  compose ps --format 'table {{.Service}}\t{{.Status}}' || true

  # Processi-app host (modello ibrido #11 §2): auth (UC 0010), core e le app del marketplace.
  # L'elenco è SCOPERTO da services/* — aggiungere un'app non richiede di toccare questo file.
  local svc
  while read -r svc; do
    [ -n "$svc" ] || continue
    svc_start "$svc"
  done <<<"$(services_startup_order)"
}
