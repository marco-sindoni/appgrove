# shellcheck shell=bash
# dev seed — carica il seed deterministico (UC 0011): idempotente, condiviso dev↔E2E.
# Assicura lo stack su, applica le migrazioni (dev migrate), poi carica dev/seed/seed.sql via psql.

SEED_FILE="$REPO_ROOT/dev/seed/seed.sql"

cmd_seed() {
  if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<'EOF'
dev seed — carica il seed deterministico (idempotente, condiviso dev↔E2E).
Passi: stack su (Postgres healthy) → dev migrate (Flyway) → psql dev/seed/seed.sql.
Ri-eseguibile: ON CONFLICT DO UPDATE → stesso stato. Dati 100% sintetici (*.test).
EOF
    return 0
  fi
  ensure_env
  ensure_engine
  require_cmd psql || die "psql non trovato sull'host (prerequisito): installa il client Postgres."
  [ -f "$SEED_FILE" ] || die "seed non trovato: $SEED_FILE"

  step "1/3 Postgres locale su (healthy)"
  compose up -d --wait postgres

  step "2/3 migrazioni (Flyway)"
  # shellcheck source=dev/lib/migrate.sh
  source "$DEV_DIR/lib/migrate.sh"
  cmd_migrate

  step "3/3 carico il seed"
  PGPASSWORD="$POSTGRES_PASSWORD" psql \
    -h localhost -p "${POSTGRES_PORT:-5432}" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -v ON_ERROR_STOP=1 -q -f "$SEED_FILE" \
    || die "caricamento seed fallito."
  ok "seed caricato (cast multi-tenant + catalogo + subscription)."
}
