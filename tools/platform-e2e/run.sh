#!/usr/bin/env bash
# run.sh — orchestratore della suite end-to-end di PIATTAFORMA (UC 0090).
#
# Il quarto livello di test del monorepo: browser vero (Playwright) sui frontend COSTRUITI
# davvero, contro lo stack backend VERO (Postgres + ElasticMQ + Mailpit dal compose dev,
# tutti i servizi scoperti in profilo `dev`), con le email transazionali realmente spedite
# e verificate via Mailpit. Nessuna simulazione del backend, nessun fornitore esterno.
#
# Passi (flusso principale dello use case):
#   1. infra vera dal compose dev (idempotente: convive con lo stack di sviluppo acceso);
#   2. migrazioni Flyway + seed (gli stessi passi di app-start);
#   3. tutti i servizi scoperti (dev/lib/services.sh) in profilo dev su porte DEDICATE
#      alla suite: porta reale + 12000 (lo smoke usa +10000 → i tre stack convivono);
#   4. build delle SPA (backoffice + admin) e pubblicazione via serve-spa.mjs, con inoltro
#      /api/* derivato dalla stessa scoperta servizi;
#   5. journey Playwright (paralleli, un tenant fresco ciascuno);
#   6. verdetto unico + teardown dei processi (i container compose restano su, come smoke).
#
# Uso:
#   tools/platform-e2e/run.sh                 # tutti i journey
#   tools/platform-e2e/run.sh --journey J-REG # un solo journey (grep Playwright)
#
# Diagnosi dopo un rosso: vedi README.md (trace/screenshot/video in test-results/,
# log dei servizi in .run/, casella email su http://localhost:8025).
#
# I passi comuni con lo smoke headless vivono in dev/lib/headless.sh (change 0069).
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TOOL_DIR="$ROOT/tools/platform-e2e"
# shellcheck source=dev/lib/headless.sh
source "$ROOT/dev/lib/headless.sh"
C_RESET=$'\033[0m'; C_GRN=$'\033[0;32m'; C_RED=$'\033[0;31m'; C_BLU=$'\033[1;36m'
ok()   { printf '%s✓ %s%s\n' "$C_GRN" "$*" "$C_RESET"; }
fail() { printf '%s✗ %s%s\n' "$C_RED" "$*" "$C_RESET"; }
step() { printf '%s▶ %s%s\n' "$C_BLU" "$*" "$C_RESET"; }

# ── argomenti: --journey <id> → grep Playwright; il resto passa a Playwright ──
PW_ARGS=()
while [ $# -gt 0 ]; do
  case "$1" in
    --journey) PW_ARGS+=(--grep "$2"); shift 2 ;;
    *) PW_ARGS+=("$1"); shift ;;
  esac
done

# ── porte dedicate alla suite: convivenza con stack dev (+0) e smoke (+10000) ─
PLATFORM_PORT_OFFSET=12000
platform_port() { printf '%s\n' "$(( $1 + PLATFORM_PORT_OFFSET ))"; }
BACKOFFICE_PORT=24173
ADMIN_PORT=24174

RUN_DIR="$TOOL_DIR/.run"
rm -rf "$RUN_DIR"; mkdir -p "$RUN_DIR"
cleanup() { for p in "${HEADLESS_PIDS[@]:-}"; do kill "$p" 2>/dev/null; done; }
trap cleanup EXIT

# ── 1. infra vera: Postgres + ElasticMQ + Mailpit dal compose dev ─────────────
docker info >/dev/null 2>&1 || { fail "Docker non disponibile: la suite di piattaforma lo richiede."; exit 1; }
headless_env || { fail "env dev non inizializzabile (dev/.env)"; exit 1; }
step "Postgres + ElasticMQ + Mailpit (compose dev, idempotente)…"
headless_compose_up postgres elasticmq mailpit || { fail "compose up fallito"; exit 1; }
MAILPIT_API="http://localhost:${MAILPIT_UI_PORT}"
headless_wait_http mailpit "$MAILPIT_API/api/v1/info" 200 \
  || { fail "Mailpit non raggiungibile su $MAILPIT_API"; exit 1; }

# ── 2. migrazioni + seed (gli stessi passi di app-start, idempotenti) ─────────
step "Flyway migrate + seed (idempotenti)…"
headless_migrate_seed || { fail "migrazioni/seed falliti"; exit 1; }

# ── 3. servizi scoperti in profilo dev su porte della suite ───────────────────
step "build artefatti in profilo dev (mvn package -Dquarkus.profile=dev)…"
headless_build_artifacts || { fail "build artefatti fallita"; exit 1; }
AUTH_KEYS="$(headless_auth_keys "$RUN_DIR")" || { fail "chiavi JWT locali non disponibili"; exit 1; }

step "avvio dei servizi scoperti (porte +$PLATFORM_PORT_OFFSET)…"
AUTH_SVC="$(services_by_role auth | head -1)"
SUITE_JWKS="http://localhost:$(platform_port "$(service_port "$AUTH_SVC")")/api/auth/jwks"
SUITE_ORIGINS="http://localhost:$BACKOFFICE_PORT,http://localhost:$ADMIN_PORT"
while IFS=$'\t' read -r svc app_id port schema role; do
  if [ "$role" = auth ]; then
    # auth: chiavi di firma locali + link email verso il server SPA della suite + Mailpit
    # del compose (auth.app-base-url / mailer sono già proprietà per-ambiente).
    headless_start_service "$svc" "$(platform_port "$port")" "$RUN_DIR/$svc.log" \
      AUTH_LOCAL_PRIVATE_KEY="$AUTH_KEYS/privateKey.pem" AUTH_LOCAL_PUBLIC_KEY="$AUTH_KEYS/publicKey.pem" \
      AUTH_APP_BASE_URL="http://localhost:$BACKOFFICE_PORT" \
      QUARKUS_MAILER_HOST=localhost QUARKUS_MAILER_PORT="$MAILPIT_SMTP_PORT"
  else
    # core/app: due override d'ambiente obbligatori per la suite —
    # 1. JWKS: in profilo dev punta all'auth dello stack dev (:9100), qui va rediretto
    #    all'auth DELLA SUITE, o i JWT emessi qui non validerebbero mai (e con lo stack
    #    dev acceso validerebbero contro chiavi sbagliate);
    # 2. CORS: Quarkus con cors=true RIFIUTA (403) ogni richiesta il cui Origin non è in
    #    lista — e il browser manda Origin su tutti i metodi non-GET anche same-origin.
    #    Gli origin dei server SPA della suite vanno quindi ammessi (la forma _DEV_…
    #    sovrascrive la variante %dev. di application.properties, che vince sull'env liscia).
    headless_start_service "$svc" "$(platform_port "$port")" "$RUN_DIR/$svc.log" \
      MP_JWT_VERIFY_PUBLICKEY_LOCATION="$SUITE_JWKS" \
      QUARKUS_HTTP_CORS_ORIGINS="$SUITE_ORIGINS" \
      _DEV_QUARKUS_HTTP_CORS_ORIGINS="$SUITE_ORIGINS"
  fi
done < <(discover_services)

rc=0
while IFS=$'\t' read -r svc app_id port schema role; do
  url="$(headless_service_ready_url "$role" "$(platform_port "$port")")"
  if headless_wait_http "$svc" "$url" 200 "$RUN_DIR/$svc.log"; then
    ok "$svc: pronto su :$(platform_port "$port")"
  else
    fail "$svc: readiness fallita"; rc=1
  fi
done < <(discover_services)
[ "$rc" -eq 0 ] || exit 1

# ── 4. build + pubblicazione SPA (config e rotte derivate dalla scoperta) ─────
if [ ! -d "$ROOT/frontend/node_modules" ]; then
  step "frontend/node_modules assente: npm ci…"
  ( cd "$ROOT/frontend" && { npm ci || npm install; } ) || { fail "install dipendenze frontend fallita"; exit 1; }
fi
step "build delle SPA (packages + apps, npm workspaces)…"
( cd "$ROOT/frontend" && npm run build ) >"$RUN_DIR/frontend-build.log" 2>&1 \
  || { fail "build frontend fallita (log: $RUN_DIR/frontend-build.log)"; tail -30 "$RUN_DIR/frontend-build.log"; exit 1; }

# rotte /api/* → porta di suite del servizio: la STESSA mappa del blocco api-routes del
# Caddyfile, derivata qui in JSON per serve-spa.mjs. Una nuova app entra da sola.
ROUTES_JSON="$RUN_DIR/routes.json"
{
  printf '['
  first=1
  while IFS=$'\t' read -r svc app_id port schema role; do
    prefix="$(service_api_prefix "$role" "$app_id")"; prefix="${prefix%\*}"
    [ "$first" -eq 1 ] || printf ','
    first=0
    printf '{"prefix":"%s","target":"http://localhost:%s"}' "$prefix" "$(platform_port "$port")"
  done < <(discover_services)
  printf ']'
} > "$ROUTES_JSON"

spa_config() { # <origin> — config runtime della SPA: stesso origin per auth e core
  printf '{"env":"platform-e2e","authBaseUrl":"%s","coreBaseUrl":"%s","cognito":{"userPoolId":"","clientId":""}}' "$1" "$1"
}
spa_config "http://localhost:$BACKOFFICE_PORT" > "$RUN_DIR/backoffice-config.json"
spa_config "http://localhost:$ADMIN_PORT"      > "$RUN_DIR/admin-config.json"

step "pubblicazione SPA (serve-spa.mjs: statico + inoltro /api/*)…"
node "$TOOL_DIR/serve-spa.mjs" --port "$BACKOFFICE_PORT" \
  --dist "$ROOT/frontend/apps/backoffice/dist" \
  --config "$RUN_DIR/backoffice-config.json" --routes "$ROUTES_JSON" \
  > "$RUN_DIR/serve-backoffice.log" 2>&1 &
HEADLESS_PIDS+=($!)
node "$TOOL_DIR/serve-spa.mjs" --port "$ADMIN_PORT" \
  --dist "$ROOT/frontend/apps/admin/dist" \
  --config "$RUN_DIR/admin-config.json" --routes "$ROUTES_JSON" \
  > "$RUN_DIR/serve-admin.log" 2>&1 &
HEADLESS_PIDS+=($!)
headless_wait_http backoffice "http://localhost:$BACKOFFICE_PORT/config.json" 200 "$RUN_DIR/serve-backoffice.log" \
  || { fail "server SPA backoffice non pronto"; exit 1; }
headless_wait_http admin "http://localhost:$ADMIN_PORT/config.json" 200 "$RUN_DIR/serve-admin.log" \
  || { fail "server SPA admin non pronto"; exit 1; }
ok "SPA servite: backoffice :$BACKOFFICE_PORT · admin :$ADMIN_PORT"

# ── 5. journey Playwright ─────────────────────────────────────────────────────
if [ ! -d "$TOOL_DIR/node_modules" ]; then
  step "tools/platform-e2e/node_modules assente: npm ci…"
  ( cd "$TOOL_DIR" && { npm ci || npm install; } ) || { fail "install dipendenze platform-e2e fallita"; exit 1; }
fi
# browser chromium: install idempotente (cache Playwright globale, condivisa col frontend)
( cd "$TOOL_DIR" && npx playwright install chromium ) \
  || { fail "playwright install chromium fallita"; exit 1; }

CORE_SVC="$(services_by_role core | head -1)"
step "esecuzione journey (Playwright)…"
( cd "$TOOL_DIR" \
    && PLATFORM_BACKOFFICE_URL="http://localhost:$BACKOFFICE_PORT" \
       PLATFORM_ADMIN_URL="http://localhost:$ADMIN_PORT" \
       PLATFORM_MAILPIT_API="$MAILPIT_API" \
       PLATFORM_AUTH_API="http://localhost:$(platform_port "$(service_port "$AUTH_SVC")")" \
       PLATFORM_CORE_API="http://localhost:$(platform_port "$(service_port "$CORE_SVC")")" \
       PLATFORM_PG_CONTAINER="$(headless_postgres_container)" \
       POSTGRES_USER="$POSTGRES_USER" POSTGRES_DB="$POSTGRES_DB" \
       npx playwright test ${PW_ARGS[@]+"${PW_ARGS[@]}"} )
rc=$?

# ── 6. verdetto (teardown dei processi nel trap; i container compose restano su) ──
if [ "$rc" -eq 0 ]; then ok "suite di piattaforma: verde"; else fail "suite di piattaforma: rossa (diagnosi: README.md)"; fi
exit "$rc"
