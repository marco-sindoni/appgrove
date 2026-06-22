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
  # Hook processi-app (Quarkus dev / Vite): vuoto finché non esistono app — UC 0046/0010.
  ok "stack su"
  compose ps --format 'table {{.Service}}\t{{.Status}}' || true
}
