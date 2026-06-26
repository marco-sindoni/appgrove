# shellcheck shell=bash
# dev migrate — applica le migrazioni Flyway al Postgres locale (UC 0011).
# Core-only: gira le migrazioni di services/core via container flyway/flyway (one-shot).
# Il multi-servizio (scoperta di tutti i services/<app>) è industrializzato in UC 0046.

CORE_MIGRATIONS="$REPO_ROOT/services/core/src/main/resources/db/migration"
FLYWAY_IMAGE="flyway/flyway:11"

cmd_migrate() {
  if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<'EOF'
dev migrate — applica le migrazioni Flyway al Postgres locale (idempotente: no-op sulle versioni già applicate).
Core-only (services/core) via container flyway/flyway. Il multi-servizio arriva con UC 0046.
EOF
    return 0
  fi
  ensure_env
  ensure_engine
  [ -d "$CORE_MIGRATIONS" ] || die "migrazioni core non trovate: $CORE_MIGRATIONS"

  step "Flyway migrate (core) → Postgres locale :${POSTGRES_PORT:-5432}"
  docker run --rm \
    --add-host=host.docker.internal:host-gateway \
    -v "$CORE_MIGRATIONS:/flyway/sql:ro" \
    "$FLYWAY_IMAGE" \
    -url="jdbc:postgresql://host.docker.internal:${POSTGRES_PORT:-5432}/${POSTGRES_DB}" \
    -user="${POSTGRES_USER}" -password="${POSTGRES_PASSWORD}" \
    -schemas=platform -connectRetries=20 -cleanDisabled=true \
    migrate \
    || die "flyway migrate fallito"
  ok "migrazioni applicate."
}
