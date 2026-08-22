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
# Con --journey si aggiunge --no-deps: altrimenti la dependency di progetto (legal-serial →
# chromium, vedi playwright.config.ts) rieseguirebbe TUTTI i journey per rilanciarne uno solo.
PW_ARGS=()
while [ $# -gt 0 ]; do
  case "$1" in
    --journey) PW_ARGS+=(--grep "$2" --no-deps); shift 2 ;;
    *) PW_ARGS+=("$1"); shift ;;
  esac
done

# ── porte dedicate alla suite: convivenza con stack dev (+0) e smoke (+10000) ─
PLATFORM_PORT_OFFSET=12000
platform_port() { printf '%s\n' "$(( $1 + PLATFORM_PORT_OFFSET ))"; }
BACKOFFICE_PORT=24173
ADMIN_PORT=24174

RUN_DIR="$TOOL_DIR/.run"
SERVICES_JSON="$RUN_DIR/services.json"
rm -rf "$RUN_DIR"; mkdir -p "$RUN_DIR"
# Pulizia: i server statici delle SPA sono figli diretti (HEADLESS_PIDS), i servizi hanno la
# propria leva — F-DEGRADE ne riavvia qualcuno, quindi l'elenco dei figli non basta più a
# spegnerli tutti: si passa dal descrittore, che li conosce per costruzione (UC 0092).
cleanup() {
  for p in "${HEADLESS_PIDS[@]:-}"; do kill "$p" 2>/dev/null; done
  [ -f "$SERVICES_JSON" ] && "$TOOL_DIR/service-ctl.sh" stop-all >/dev/null 2>&1
  return 0
}
trap cleanup EXIT

# ── 1. infra vera: Postgres + ElasticMQ + Mailpit dal compose dev ─────────────
docker info >/dev/null 2>&1 || { fail "Docker non disponibile: la suite di piattaforma lo richiede."; exit 1; }
headless_env || { fail "env dev non inizializzabile (dev/.env)"; exit 1; }
step "Postgres + ElasticMQ + Mailpit + MinIO (compose dev, idempotente)…"
# MinIO serve all'export GDPR di J-PRIVACY (bucket gdpr-export, creato da minio-init): senza,
# il job di export fallirebbe sempre (UC 0091, decisione 9 della change 0070).
headless_compose_up postgres elasticmq mailpit minio || { fail "compose up fallito"; exit 1; }
# minio-init è one-shot (crea il bucket ed esce): niente --wait, che fallirebbe sul container uscito.
( cd "$ROOT/dev" && docker compose up -d minio-init ) || { fail "minio-init fallito"; exit 1; }
MAILPIT_API="http://localhost:${MAILPIT_UI_PORT}"
headless_wait_http mailpit "$MAILPIT_API/api/v1/info" 200 \
  || { fail "Mailpit non raggiungibile su $MAILPIT_API"; exit 1; }
headless_wait_http minio "http://localhost:${MINIO_API_PORT:-9000}/minio/health/live" 200 \
  || { fail "MinIO non raggiungibile su :${MINIO_API_PORT:-9000}"; exit 1; }

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
# URL del core DELLA SUITE per il rest-client `core-api` delle app (proiezione entitlement,
# UC 0046): in profilo dev punta a :8080 (core dello stack dev) — per un tenant fresco la
# rete di sicurezza "fetch-on-miss" chiamerebbe il core sbagliato e il gate fallirebbe
# chiuso (402 su ogni app per ogni tenant nuovo della suite).
SUITE_CORE_URL="http://localhost:$(platform_port "$(service_port "$(services_by_role core | head -1)")")"

# ── descrittore dei servizi: la RICETTA DI AVVIO, scritta una volta sola ──────
# Da qui passano sia il primo avvio sia i riavvii di F-DEGRADE (UC 0092): la leva
# service-ctl.sh è l'unico esecutore, così non esistono due elenchi di variabili d'ambiente
# che col tempo divergono. Il contenuto resta derivato dalla scoperta servizi: una nuova app
# entra da sola anche nel governo del ciclo di vita.
json_str() { printf '"%s"' "$(printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g')"; }
svc_env() { # <ruolo> → righe CHIAVE=valore
  if [ "$1" = auth ]; then
    # auth: chiavi di firma locali + link email verso il server SPA della suite + Mailpit
    # del compose (auth.app-base-url / mailer sono già proprietà per-ambiente).
    # Il bypass dev del 2FA è DISATTIVATO per la suite (UC 0091, J-PWD esercita la challenge
    # reale del login a due passi); la forma _DEV_ serve perché la proprietà %dev. di
    # application.properties vince sull'ambiente non profilato (stessa ragione del CORS sotto).
    printf 'AUTH_LOCAL_PRIVATE_KEY=%s\n' "$AUTH_KEYS/privateKey.pem"
    printf 'AUTH_LOCAL_PUBLIC_KEY=%s\n' "$AUTH_KEYS/publicKey.pem"
    printf 'AUTH_APP_BASE_URL=%s\n' "http://localhost:$BACKOFFICE_PORT"
    printf 'QUARKUS_MAILER_HOST=%s\n' localhost
    printf 'QUARKUS_MAILER_PORT=%s\n' "$MAILPIT_SMTP_PORT"
    printf 'AUTH_LOCAL_TOTP_BYPASS=false\n_DEV_AUTH_LOCAL_TOTP_BYPASS=false\n'
  else
    # core/app: due override d'ambiente obbligatori per la suite —
    # 1. JWKS: in profilo dev punta all'auth dello stack dev (:9100), qui va rediretto
    #    all'auth DELLA SUITE, o i JWT emessi qui non validerebbero mai (e con lo stack
    #    dev acceso validerebbero contro chiavi sbagliate);
    # 2. CORS: Quarkus con cors=true RIFIUTA (403) ogni richiesta il cui Origin non è in
    #    lista — e il browser manda Origin su tutti i metodi non-GET anche same-origin.
    #    Gli origin dei server SPA della suite vanno quindi ammessi (la forma _DEV_…
    #    sovrascrive la variante %dev. di application.properties, che vince sull'env liscia).
    printf 'MP_JWT_VERIFY_PUBLICKEY_LOCATION=%s\n' "$SUITE_JWKS"
    printf 'QUARKUS_HTTP_CORS_ORIGINS=%s\n' "$SUITE_ORIGINS"
    printf '_DEV_QUARKUS_HTTP_CORS_ORIGINS=%s\n' "$SUITE_ORIGINS"
    printf 'QUARKUS_REST_CLIENT_CORE_API_URL=%s\n' "$SUITE_CORE_URL"
  fi
  # comuni a tutti: datasource del compose dev (gli stessi di headless_start_service)
  printf 'QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:%s/%s\n' "$POSTGRES_PORT" "$POSTGRES_DB"
  printf 'QUARKUS_DATASOURCE_USERNAME=%s\n' "$POSTGRES_USER"
  printf 'QUARKUS_DATASOURCE_PASSWORD=%s\n' "$POSTGRES_PASSWORD"
}
{
  printf '{'
  sep=''
  while IFS=$'\t' read -r svc app_id port schema role; do
    printf '%s%s:{"appId":%s,"role":%s,"port":%s,"log":%s,"env":{' \
      "$sep" "$(json_str "$svc")" "$(json_str "$app_id")" "$(json_str "$role")" \
      "$(platform_port "$port")" "$(json_str "$RUN_DIR/$svc.log")"
    esep=''
    while IFS= read -r kv; do
      [ -n "$kv" ] || continue
      printf '%s%s:%s' "$esep" "$(json_str "${kv%%=*}")" "$(json_str "${kv#*=}")"
      esep=','
    done < <(svc_env "$role")
    printf '}}'
    sep=','
  done < <(discover_services)
  printf '}'
} > "$SERVICES_JSON"

# accensione di tutti i servizi insieme, poi attesa (come prima: il costo è il massimo dei
# tempi di avvio, non la loro somma).
while IFS=$'\t' read -r svc app_id port schema role; do
  "$TOOL_DIR/service-ctl.sh" spawn "$svc" || { fail "$svc: avvio fallito"; exit 1; }
done < <(discover_services)

rc=0
while IFS=$'\t' read -r svc app_id port schema role; do
  if "$TOOL_DIR/service-ctl.sh" wait "$svc"; then
    ok "$svc: pronto su :$(platform_port "$port")"
  else
    fail "$svc: readiness fallita (registro: $RUN_DIR/$svc.log)"
    grep -B2 -A8 "Caused by" "$RUN_DIR/$svc.log" 2>/dev/null | head -30 >&2
    tail -15 "$RUN_DIR/$svc.log" 2>/dev/null >&2
    rc=1
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
    && PLATFORM_TOOL_DIR="$TOOL_DIR" \
       PLATFORM_BACKOFFICE_URL="http://localhost:$BACKOFFICE_PORT" \
       PLATFORM_ADMIN_URL="http://localhost:$ADMIN_PORT" \
       PLATFORM_MAILPIT_API="$MAILPIT_API" \
       PLATFORM_AUTH_API="http://localhost:$(platform_port "$(service_port "$AUTH_SVC")")" \
       PLATFORM_CORE_API="http://localhost:$(platform_port "$(service_port "$CORE_SVC")")" \
       PLATFORM_PG_CONTAINER="$(headless_postgres_container)" \
       POSTGRES_USER="$POSTGRES_USER" POSTGRES_DB="$POSTGRES_DB" \
       npx playwright test ${PW_ARGS[@]+"${PW_ARGS[@]}"} )
rc=$?

# ── 5bis. i percorsi INSTABILI rendono la suite rossa ─────────────────────────
# Playwright esce con codice ZERO anche quando un percorso fallisce al primo tentativo e passa al
# secondo: lo chiama «flaky» nel testo e non nel codice di uscita. Fidarsi del solo codice significa
# perdonare in silenzio esattamente il difetto che si stava cercando — è così che i tre casi di
# instabilità del 2026-08-21 sono vissuti a lungo senza essere visti. Un test che ha fallito ha
# fallito: qui lo si legge dal resoconto in formato dati e si NOMINA (change 0094).
ESITO_JSON="$TOOL_DIR/test-results/esito.json"
if [ "$rc" -eq 0 ] && [ -f "$ESITO_JSON" ]; then
  INSTABILI="$(node -e '
    const fs = require("fs");
    const r = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
    const nomi = [];
    const visita = (s) => {
      for (const spec of s.specs ?? []) {
        for (const t of spec.tests ?? []) {
          // "flaky" = Playwright stesso dichiara che è passato solo grazie a un ritentativo.
          if (t.status === "flaky") nomi.push(spec.title);
        }
      }
      for (const f of s.suites ?? []) visita(f);
    };
    for (const s of r.suites ?? []) visita(s);
    process.stdout.write(nomi.join("\n"));
  ' "$ESITO_JSON" 2>/dev/null)"
  if [ -n "$INSTABILI" ]; then
    fail "suite di piattaforma: ROSSA per instabilità — questi percorsi sono passati solo al RITENTATIVO:"
    printf '%s\n' "$INSTABILI" | sed 's/^/    · /'
    printf '  %s\n' "Un percorso che ha bisogno di un secondo tentativo ha fallito il primo: è un difetto," \
                     "non un incidente. Tracce, schermate e video del tentativo fallito sono in" \
                     "tools/platform-e2e/test-results/ — si parte da là (diagnosi: README.md)."
    rc=1
  fi
elif [ "$rc" -eq 0 ]; then
  # Nessun resoconto in formato dati = non si può escludere l'instabilità. Si avvisa senza far rosso:
  # un file mancante è un guasto dello strumento, non del prodotto, e non deve fermare una corsa sana.
  warn_missing="resoconto in formato dati assente ($ESITO_JSON): instabilità non verificabile"
  printf '%s! %s%s\n' "$C_RED" "$warn_missing" "$C_RESET"
fi

# ── 6. verdetto (teardown dei processi nel trap; i container compose restano su) ──
if [ "$rc" -eq 0 ]; then ok "suite di piattaforma: verde"; else fail "suite di piattaforma: rossa (diagnosi: README.md)"; fi
exit "$rc"
