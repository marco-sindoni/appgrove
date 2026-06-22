# shellcheck shell=bash
# dev reset — wipe volumi + riavvio + reseed (UC 0009). Reseed stub → UC 0011.

cmd_reset() {
  if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<'EOF'
dev reset — riporta l'ambiente a uno stato deterministico:
  1) docker compose down -v  (cancella i volumi: Postgres, MinIO, code)
  2) docker compose up -d     (ricrea lo stack)
  3) reseed                   [stub → UC 0011]
EOF
    return 0
  fi
  ensure_env
  ensure_engine
  warn "RESET: sto per cancellare i volumi locali (Postgres, MinIO, code)."
  step "1/3 wipe volumi"
  compose down -v
  step "2/3 ricreo lo stack"
  compose up -d
  step "3/3 reseed"
  # shellcheck source=dev/lib/seed.sh
  source "$DEV_DIR/lib/seed.sh"
  cmd_seed
  ok "reset completato."
}
