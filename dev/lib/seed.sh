# shellcheck shell=bash
# dev seed — carica il seed deterministico (UC 0011): idempotente, condiviso dev↔E2E.
# Assicura lo stack su, applica le migrazioni (dev migrate), poi carica dev/seed/seed.sql via psql.

SEED_FILE="$REPO_ROOT/dev/seed/seed.sql"
SUBS_FILE="$REPO_ROOT/dev/seed/seed-subscriptions.sql"

cmd_seed() {
  if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<'EOF'
dev seed — carica il seed deterministico (idempotente, condiviso dev↔E2E).
Passi: stack su (Postgres healthy) → dev migrate (Flyway) → sync-pricing (catalogo da YAML) → psql dev/seed/seed.sql.
Ri-eseguibile: ON CONFLICT DO UPDATE → stesso stato. Dati 100% sintetici (*.test).
EOF
    return 0
  fi
  ensure_env
  ensure_engine
  require_cmd psql || die "psql non trovato sull'host (prerequisito): installa il client Postgres."
  require_cmd java || die "java non trovato sull'host (prerequisito): serve per la sync pricing-as-code."
  [ -f "$SEED_FILE" ] || die "seed non trovato: $SEED_FILE"
  [ -f "$SUBS_FILE" ] || die "seed subscription non trovato: $SUBS_FILE"

  step "1/4 Postgres locale su (healthy)"
  compose up -d --wait postgres

  step "2/4 migrazioni (Flyway)"
  # shellcheck source=dev/lib/migrate.sh
  source "$DEV_DIR/lib/migrate.sh"
  cmd_migrate

  # Pricing-as-code (UC 0022, "Strada 1"): il catalogo (app/app_tier/app_price) NON è nel seed.sql ma è
  # prodotto dal LOADER dagli YAML, via l'entrypoint command-mode `sync-pricing` del core. Va PRIMA del
  # seed perché le subscription del seed lo referenziano (FK). È lo stesso comando che invocherà la CI dopo
  # il Flyway migrate (UC 0005). Il jar è SEMPRE ricostruito (mai servire un jar stantio: un jar senza il
  # command-mode girerebbe come server e bloccherebbe il seed); JVM one-shot su porta HTTP 0, con watchdog.
  # Build con profilo DEV: la selezione del provider è build-time (@IfBuildProperty). Senza, `mvn package`
  # builda in prod → provider=paddle (placeholder gated #14) e la sync fallirebbe. In dev → StubPaymentProvider.
  step "3/4 sync pricing-as-code → catalogo (loader)"
  info "build core (profilo dev, per sync-pricing)…"
  ( cd "$REPO_ROOT/services" && mvn -q -pl core -am -DskipTests -Dquarkus.profile=dev package ) \
    || die "build core fallita: impossibile eseguire sync-pricing."
  local core_jar="$REPO_ROOT/services/core/target/quarkus-app/quarkus-run.jar"
  QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://localhost:${POSTGRES_PORT:-5432}/${POSTGRES_DB}" \
  QUARKUS_DATASOURCE_USERNAME="$POSTGRES_USER" \
  QUARKUS_DATASOURCE_PASSWORD="$POSTGRES_PASSWORD" \
    java -Dquarkus.profile=dev \
      -Dquarkus.http.port=0 \
      -Dquarkus.scheduler.enabled=false \
      -Dappgrove.pricing.sync-on-startup=false \
      -jar "$core_jar" sync-pricing &
  local sync_pid=$!
  # watchdog: il command-mode è one-shot e deve uscire; se per qualunque motivo non terminasse (es. boot
  # come server), evita un blocco infinito del seed.
  ( sleep 180; kill -9 "$sync_pid" 2>/dev/null ) & local watchdog_pid=$!
  disown "$watchdog_pid" 2>/dev/null || true   # niente messaggio "Terminated" quando lo spegniamo
  wait "$sync_pid"; local sync_rc=$?
  kill "$watchdog_pid" 2>/dev/null || true
  [ "$sync_rc" -eq 0 ] || die "sync-pricing (pricing-as-code) fallito (rc=$sync_rc). Se il DB ha un catalogo"\
    " legacy (slug già presente con UUID vecchi), esegui './dev.sh reset' una tantum per ripartire pulito."
  ok "catalogo sincronizzato dal pricing-as-code (YAML → DB)."

  # Identità (seed.sql) + subscription (seed-subscriptions.sql, FK sul catalogo già sincronizzato sopra).
  step "4/4 carico il seed"
  PGPASSWORD="$POSTGRES_PASSWORD" psql \
    -h localhost -p "${POSTGRES_PORT:-5432}" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -v ON_ERROR_STOP=1 -q -f "$SEED_FILE" -f "$SUBS_FILE" \
    || die "caricamento seed fallito."
  ok "seed caricato (identità + subscription; catalogo via pricing-as-code)."
}
