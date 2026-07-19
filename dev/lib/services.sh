# shellcheck shell=bash
# Scoperta automatica dei servizi backend (UC 0046).
#
# SORGENTE UNICA della mappa  servizio → app_id → porta → schema → ruolo.
# Nessun registro da tenere allineato a mano: tutto è DERIVATO dai file che ogni servizio
# possiede già, `services/<svc>/src/main/resources/application.properties`:
#
#   appgrove.app-id         → identificativo dell'app nelle rotte (/api/<app_id>/v1/*)
#   quarkus.http.port       → porta del processo host (se assente: 8080, default Quarkus → core)
#   quarkus.flyway.schemas  → schema Postgres di proprietà del servizio (per `dev migrate`)
#
# Il RUOLO non è dichiarato da nessuna parte: si deduce dall'app-id, così una nuova app non
# deve toccare alcun file di registro.
#   core → app-id "platform"      : servizio di piattaforma, rotta /api/platform/*, sempre avviato
#   auth → nessun appgrove.app-id : servizio di piattaforma non-app, rotta /api/auth/*, ciclo di
#                                   vita dedicato (jar + chiavi JWT, vedi auth_start in common.sh)
#   app  → tutti gli altri        : app del marketplace, rotta /api/<app_id>/v1/*
#
# Una directory in services/ SENZA application.properties non è un servizio (es. `commons`,
# libreria condivisa) e viene ignorata.
#
# Questo file NON ha effetti collaterali (niente colori, niente dev/.env, niente docker):
# è sorgiabile da dev/lib/common.sh, dagli script di root e dagli smoke (tools/smoke).
#
# Ispezione a mano:  ./dev.sh services      (oppure: bash dev/lib/services.sh)

: "${REPO_ROOT:=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
SERVICES_DIR="$REPO_ROOT/services"

DEFAULT_HTTP_PORT=8080   # default Quarkus quando `quarkus.http.port` non è dichiarato (core)
DEBUG_PORT_BASE=5005     # debug JVM = 5005 + (porta http − 8080) → core 5005, prima app 5006, …

service_props_file()    { printf '%s\n' "$SERVICES_DIR/$1/src/main/resources/application.properties"; }
service_migrations_dir() { printf '%s\n' "$SERVICES_DIR/$1/src/main/resources/db/migration"; }

# service_prop <svc> <chiave> — valore della proprietà (prima occorrenza; le varianti per-profilo
# `%dev.`/`%prod.` non iniziano con la chiave e vengono quindi ignorate, che è ciò che vogliamo).
service_prop() {
  local file; file="$(service_props_file "$1")"
  [ -f "$file" ] || return 0
  awk -v k="$2=" '
    index($0, k) == 1 { v = substr($0, length(k) + 1); sub(/[ \t\r]+$/, "", v); print v; exit }
  ' "$file"
}

# _scan_services — righe TSV: <svc> <app_id> <porta> <schema> <ruolo>
_scan_services() {
  local dir svc app_id port schema role seen=' '
  for dir in "$SERVICES_DIR"/*/; do
    svc="${dir%/}"; svc="${svc##*/}"
    [ -f "$(service_props_file "$svc")" ] || continue   # non è un servizio (es. commons)
    app_id="$(service_prop "$svc" appgrove.app-id)"
    port="$(service_prop "$svc" quarkus.http.port)"; port="${port:-$DEFAULT_HTTP_PORT}"
    # Nessun campo può restare vuoto: il tab è un separatore "whitespace" per `read`, che
    # collasserebbe due tab consecutivi e sfaserebbe le colonne successive (es. il ruolo).
    # Un servizio senza Flyway è legittimo → segnaposto '-', tradotto in vuoto da service_schema.
    schema="$(service_prop "$svc" quarkus.flyway.schemas)"; schema="${schema:--}"
    case "$app_id" in
      '')       role=auth; app_id="$svc" ;;
      platform) role=core ;;
      *)        role=app ;;
    esac
    case "$seen" in
      *" $port "*) printf '! porte in conflitto: :%s assegnata anche a %s (vedi application.properties)\n' "$port" "$svc" >&2 ;;
    esac
    seen="$seen$port "
    printf '%s\t%s\t%s\t%s\t%s\n' "$svc" "$app_id" "$port" "$schema" "$role"
  done
}

# discover_services — la mappa (memoizzata: la scansione è pura lettura di file).
discover_services() {
  [ -n "${_SERVICES_CACHE:-}" ] || _SERVICES_CACHE="$(_scan_services)"
  [ -n "$_SERVICES_CACHE" ] || return 0
  printf '%s\n' "$_SERVICES_CACHE"
}

# services_by_role <core|auth|app> — nomi dei servizi con quel ruolo.
services_by_role() { discover_services | awk -F'\t' -v r="$1" '$5 == r { print $1 }'; }

# services_startup_order — core, poi le app, poi auth (auth ha un ciclo di vita a parte).
services_startup_order() { services_by_role core; services_by_role app; services_by_role auth; }

# service_row <svc> — la riga TSV del servizio (vuota se sconosciuto).
service_row() { discover_services | awk -F'\t' -v s="$1" '$1 == s { print; exit }'; }

# service_by_app_id <app_id> — nome del servizio che espone quell'app-id (accetta anche il nome
# della directory, così `dev service core` e `dev service platform` funzionano entrambi).
service_by_app_id() {
  discover_services | awk -F'\t' -v a="$1" '$2 == a || $1 == a { print $1; exit }'
}

# accessor per campo (evitano di ricordare l'ordine delle colonne)
service_app_id() { service_row "$1" | cut -f2; }
service_port()   { service_row "$1" | cut -f3; }
# schema Flyway del servizio; vuoto se non ne dichiara uno (segnaposto '-' nella mappa).
service_schema() { local v; v="$(service_row "$1" | cut -f4)"; [ "$v" = '-' ] || printf '%s\n' "$v"; }
service_role()   { service_row "$1" | cut -f5; }

# service_debug_port <porta_http> — porta del debugger JVM associata.
service_debug_port() { printf '%s\n' "$(( DEBUG_PORT_BASE + $1 - DEFAULT_HTTP_PORT ))"; }

# service_api_prefix <ruolo> <app_id> — prefisso di rotta esposto dietro il proxy.
service_api_prefix() {
  case "$1" in
    auth) printf '/api/auth/*\n' ;;
    core) printf '/api/platform/*\n' ;;
    *)    printf '/api/%s/v1/*\n' "$2" ;;
  esac
}

# ── `dev services` (dispatcher dev/dev) ───────────────────────────────────────
cmd_services() {
  if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<'EOF'
dev services — stampa la mappa dei servizi backend scoperta in services/*.
Derivata dai rispettivi application.properties (app-id, porta, schema Flyway): è la sorgente
unica usata da `dev up`, `dev migrate`, `dev service`, app-start.sh/app-stop.sh, dal blocco
rotte del Caddyfile e dagli smoke. Aggiungere un'app non richiede di aggiornare nulla qui.
EOF
    return 0
  fi
  printf '%-10s %-10s %-6s %-6s %-12s %-5s %s\n' SERVIZIO APP_ID PORTA DEBUG SCHEMA RUOLO ROTTA
  local svc app_id port schema role dbg
  while IFS=$'\t' read -r svc app_id port schema role; do
    # auth non gira in `quarkus:dev` (jar + chiavi JWT): nessuna porta di debug convenzionale.
    if [ "$role" = auth ]; then dbg='-'; else dbg="$(service_debug_port "$port")"; fi
    printf '%-10s %-10s %-6s %-6s %-12s %-5s %s\n' \
      "$svc" "$app_id" "$port" "$dbg" "$schema" "$role" \
      "$(service_api_prefix "$role" "$app_id")"
  done < <(discover_services)
}

# Eseguito direttamente (`bash dev/lib/services.sh`): stampa la mappa.
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then cmd_services "$@"; fi
