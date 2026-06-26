# shellcheck shell=bash
# dev up — avvia lo stack locale (UC 0008/0009).

cmd_up() {
  if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<'EOF'
dev up [args] — avvia lo stack locale in background (docker compose up -d).
Argomenti extra passati a compose (es. --build). Crea dev/.env e avvia l'engine se serve.
EOF
    return 0
  fi
  ensure_env
  ensure_engine
  step "avvio stack locale"
  compose up -d "$@"
  ok "stack su"
  compose ps --format 'table {{.Service}}\t{{.Status}}' || true

  # Processi-app host (modello ibrido #11 §2): auth-local su :9100 (UC 0010).
  # Gli altri servizi/app (selettivi via `dev service`) arrivano con UC 0046.
  step "avvio auth-local (:$AUTH_PORT)"
  auth_local_start
}
