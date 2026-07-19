# shellcheck shell=bash
# dev migrate — applica le migrazioni Flyway al Postgres locale (UC 0011, multi-servizio UC 0046).
# Scopre da sé i servizi con migrazioni (services/<svc>/src/main/resources/db/migration) e per
# ciascuno gira il container flyway/flyway one-shot sul suo schema (quarkus.flyway.schemas).
# Nessun nome di servizio scritto a mano: una nuova app viene migrata senza toccare questo file.

FLYWAY_IMAGE="flyway/flyway:11"

# flyway_migrate_one <svc> <schema> <dir> — un container one-shot per servizio.
flyway_migrate_one() {
  local svc="$1" schema="$2" dir="$3"
  step "Flyway migrate: $svc → schema $schema"
  # JAVA_ARGS=-Xint: forza l'interprete (niente JIT). Sotto l'emulazione x86_64 di colima la JVM del
  # container Flyway può andare in SIGSEGV nel compilatore c1; il migrate è minuscolo, l'interprete basta.
  docker run --rm \
    -e JAVA_ARGS="-Xint" \
    --add-host=host.docker.internal:host-gateway \
    -v "$dir:/flyway/sql:ro" \
    "$FLYWAY_IMAGE" \
    -url="jdbc:postgresql://host.docker.internal:${POSTGRES_PORT:-5432}/${POSTGRES_DB}" \
    -user="${POSTGRES_USER}" -password="${POSTGRES_PASSWORD}" \
    -schemas="$schema" -connectRetries=20 -cleanDisabled=true \
    migrate \
    || die "flyway migrate fallito per '$svc' (schema $schema)"
}

cmd_migrate() {
  if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<'EOF'
dev migrate — applica le migrazioni Flyway al Postgres locale (idempotente: no-op sulle versioni
già applicate). Multi-servizio: scopre i servizi con db/migration in services/* e migra ciascuno
sul proprio schema (core→platform, auth→auth_local, ogni app→app_<id>). Vedi ./dev.sh services.
Uso: dev migrate [servizio…]   (senza argomenti: tutti i servizi con migrazioni)
EOF
    return 0
  fi
  ensure_env
  ensure_engine

  # Ordine: core per primo (schema platform, referenziato dagli altri), poi le app, poi auth.
  local targets svc schema dir migrated=0
  if [ "$#" -gt 0 ]; then
    targets="$(printf '%s\n' "$@")"
  else
    targets="$(services_startup_order)"
  fi

  while read -r svc; do
    [ -n "$svc" ] || continue
    [ -n "$(service_row "$svc")" ] || die "servizio sconosciuto: $svc (vedi ./dev.sh services)"
    dir="$(service_migrations_dir "$svc")"
    [ -d "$dir" ] || { info "  $svc: nessuna migrazione (salto)"; continue; }
    schema="$(service_schema "$svc")"
    [ -n "$schema" ] || die "$svc ha migrazioni ma nessun quarkus.flyway.schemas in application.properties"
    flyway_migrate_one "$svc" "$schema" "$dir"
    migrated=$((migrated + 1))
  done <<<"$targets"

  [ "$migrated" -gt 0 ] || die "nessuna migrazione trovata in services/* (scoperta vuota?)"
  ok "migrazioni applicate ($migrated servizi)."
}
