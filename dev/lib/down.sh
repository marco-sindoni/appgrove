# shellcheck shell=bash
# dev down — ferma lo stack locale (UC 0009).

cmd_down() {
  if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<'EOF'
dev down [-v] — ferma lo stack locale (docker compose down).
  -v   cancella anche i volumi (RESET dati: Postgres, MinIO, code).
EOF
    return 0
  fi
  if ! engine_up; then
    info "Docker daemon non attivo: niente da fermare."
    return 0
  fi
  step "arresto stack locale"
  compose down "$@"
  ok "stack fermato"
}
