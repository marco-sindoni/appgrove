# shellcheck shell=bash
# dev service <app_id> — avvio selettivo core + app (UC 0009). STUB → finalizzato coi servizi (UC 0046).

cmd_service() {
  if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<'EOF'
dev service <app_id> — avvia selettivamente core + l'app indicata (Quarkus dev + Vite), non tutti gli N servizi.
STUB: nessun servizio ancora presente; l'avvio selettivo e l'auto-wiring arrivano coi servizi (UC 0046).
EOF
    return 0
  fi
  local app="${1:-}"
  if [ -z "$app" ]; then
    err "uso: ./dev.sh service <app_id>"
    return 2
  fi
  warn "dev service '$app': nessun servizio ancora presente → avvio selettivo + auto-wiring in UC 0046."
  return 0
}
