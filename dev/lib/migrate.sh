# shellcheck shell=bash
# dev migrate — Flyway sul Postgres locale (UC 0009). STUB → finalizzato coi servizi (UC 0046).

cmd_migrate() {
  if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<'EOF'
dev migrate — applica le migrazioni Flyway al Postgres locale (idempotente: no-op sulle versioni già applicate).
STUB: nessun servizio/migrazione ancora presente; l'aggancio Flyway arriva coi servizi (UC 0046).
EOF
    return 0
  fi
  warn "dev migrate: nessuna migrazione Flyway ancora → aggancio coi primi servizi (UC 0046)."
  return 0
}
