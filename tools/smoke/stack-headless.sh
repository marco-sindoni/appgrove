#!/usr/bin/env bash
# stack-headless.sh — smoke HEADLESS dello stack backend locale (change 0037).
#
# La fetta pragmatica del "test di app-start.sh in CI" (decisione col Platform Engineer):
# NIENTE browser, NIENTE Caddy/TLS/mkcert//etc/hosts (dove vive quasi tutta la fragilità),
# ma infrastruttura VERA: Postgres + ElasticMQ dal compose dev, migrazioni Flyway + seed
# reali, e TUTTI i servizi impacchettati avviati in profilo `dev` su porte alternative.
# L'elenco dei servizi è SCOPERTO da services/* (dev/lib/services.sh): una nuova app entra
# nello smoke senza toccare questo file. Porta di smoke = porta reale + 10000.
# Copre ciò che il boot-smoke dei profili non vede: migrazioni+seed sullo stesso DB,
# config %dev come la assembla lo stack, wiring cross-servizio, e un login VERO end-to-end.
#
# Convive con lo stack dev acceso: porte alternative (1808x/19100) e DB condiviso
# (migrate no-op, seed idempotente — gli stessi passi di app-start). I container compose
# eventualmente avviati qui restano su (come farebbe app-start; CI è effimera comunque).
#
# I passi comuni (env, compose, migrate+seed, build artefatti, chiavi, avvio, readiness)
# vivono in dev/lib/headless.sh, condivisi con la suite e2e di piattaforma (UC 0090).
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# Libreria condivisa dell'avvio headless (sorge anche dev/lib/services.sh). Nessun effetto collaterale.
# shellcheck source=dev/lib/headless.sh
source "$ROOT/dev/lib/headless.sh"
C_RESET=$'\033[0m'; C_GRN=$'\033[0;32m'; C_RED=$'\033[0;31m'; C_BLU=$'\033[1;36m'
ok()   { printf '%s✓ %s%s\n' "$C_GRN" "$*" "$C_RESET"; }
fail() { printf '%s✗ %s%s\n' "$C_RED" "$*" "$C_RESET"; }
step() { printf '%s▶ %s%s\n' "$C_BLU" "$*" "$C_RESET"; }

# Convive con lo stack dev acceso: ogni servizio gira sulla sua porta + 10000
# (core 8080→18080, auth 9100→19100, …), così non collide mai con lo stack reale.
SMOKE_PORT_OFFSET=10000
smoke_port() { printf '%s\n' "$(( $1 + SMOKE_PORT_OFFSET ))"; }
AUTH_SVC="$(services_by_role auth | head -1)"
AUTH_PORT="$(smoke_port "$(service_port "$AUTH_SVC")")"
TMP_DIR="$(mktemp -d /tmp/appgrove-stack-smoke.XXXXXX)"
cleanup() {
  for p in "${HEADLESS_PIDS[@]:-}"; do kill "$p" 2>/dev/null; done
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

# ── prerequisiti: Docker + env dev (stesse variabili di dev/.env di app-start) ─
docker info >/dev/null 2>&1 || { fail "Docker non disponibile: lo smoke headless lo richiede."; exit 1; }
headless_env || { fail "env dev non inizializzabile (dev/.env)"; exit 1; }

# ── infra reale: Postgres + ElasticMQ dal compose dev (idempotente) ───────────
step "Postgres + ElasticMQ (compose dev)…"
headless_compose_up postgres elasticmq || { fail "compose up postgres/elasticmq fallito"; exit 1; }

# ── migrazioni + seed: gli STESSI passi di app-start (idempotenti) ────────────
step "Flyway migrate + seed utenti (idempotenti)…"
headless_migrate_seed || { fail "migrazioni/seed falliti"; exit 1; }

# ── artefatti con PROFILO DI BUILD dev + chiavi JWT locali ────────────────────
step "build artefatti in profilo dev (mvn package -Dquarkus.profile=dev)…"
headless_build_artifacts || { fail "build artefatti fallita"; exit 1; }
AUTH_KEYS="$(headless_auth_keys "$TMP_DIR")" || { fail "chiavi JWT locali non disponibili"; exit 1; }

# ── avvio dei servizi in profilo dev (porte alternative) ──────────────────────
step "avvio dei servizi scoperti in profilo dev (porte +$SMOKE_PORT_OFFSET)…"
while IFS=$'\t' read -r svc app_id port schema role; do
  if [ "$role" = auth ]; then
    # auth ha bisogno delle chiavi di firma locali (le stesse che passa `dev up`).
    headless_start_service "$svc" "$(smoke_port "$port")" "$TMP_DIR/$svc.log" \
      AUTH_LOCAL_PRIVATE_KEY="$AUTH_KEYS/privateKey.pem" AUTH_LOCAL_PUBLIC_KEY="$AUTH_KEYS/publicKey.pem"
  else
    headless_start_service "$svc" "$(smoke_port "$port")" "$TMP_DIR/$svc.log"
  fi
done < <(discover_services)

# ── readiness + asserzioni end-to-end ─────────────────────────────────────────
rc=0
while IFS=$'\t' read -r svc app_id port schema role; do
  url="$(headless_service_ready_url "$role" "$(smoke_port "$port")")"
  if headless_wait_http "$svc" "$url" 200 "$TMP_DIR/$svc.log"; then
    ok "$svc: $url → 200"
  else
    fail "$svc: readiness fallita"; rc=1
  fi
done < <(discover_services)

# Login VERO col seed (migrazioni + seed + provider locale insieme): la garanzia
# che il boot-smoke dei profili non può dare.
if [ "$rc" -eq 0 ]; then
  step "login end-to-end (owner@acme.test, seed)…"
  LOGIN_CODE="$(curl -s -o "$TMP_DIR/login.json" -w '%{http_code}' \
    -X POST "http://localhost:$AUTH_PORT/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d '{"email":"owner@acme.test","password":"Password1!"}')"
  if [ "$LOGIN_CODE" = "200" ] && grep -q '"access_token"' "$TMP_DIR/login.json"; then
    ok "login: 200 con access_token (DB + seed + provider locale coerenti)"
  else
    fail "login: atteso 200 con token, ottenuto $LOGIN_CODE"; cat "$TMP_DIR/login.json" 2>/dev/null; rc=1
  fi
fi

exit "$rc"
