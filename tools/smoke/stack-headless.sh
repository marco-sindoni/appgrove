#!/usr/bin/env bash
# stack-headless.sh — smoke HEADLESS dello stack backend locale (change 0037).
#
# La fetta pragmatica del "test di app-start.sh in CI" (decisione col Platform Engineer):
# NIENTE browser, NIENTE Caddy/TLS/mkcert//etc/hosts (dove vive quasi tutta la fragilità),
# ma infrastruttura VERA: Postgres + ElasticMQ dal compose dev, migrazioni Flyway + seed
# reali, e i TRE servizi impacchettati avviati in profilo `dev` su porte alternative.
# Copre ciò che il boot-smoke dei profili non vede: migrazioni+seed sullo stesso DB,
# config %dev come la assembla lo stack, wiring cross-servizio, e un login VERO end-to-end.
#
# Convive con lo stack dev acceso: porte alternative (1808x/19100) e DB condiviso
# (migrate no-op, seed idempotente — gli stessi passi di app-start). I container compose
# eventualmente avviati qui restano su (come farebbe app-start; CI è effimera comunque).
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
C_RESET=$'\033[0m'; C_GRN=$'\033[0;32m'; C_RED=$'\033[0;31m'; C_BLU=$'\033[1;36m'
ok()   { printf '%s✓ %s%s\n' "$C_GRN" "$*" "$C_RESET"; }
fail() { printf '%s✗ %s%s\n' "$C_RED" "$*" "$C_RESET"; }
step() { printf '%s▶ %s%s\n' "$C_BLU" "$*" "$C_RESET"; }

BOOT_TIMEOUT="${BOOT_TIMEOUT:-120}"
AUTH_PORT=19100; CORE_PORT=18080; FATTURE_PORT=18081
TMP_DIR="$(mktemp -d /tmp/appgrove-stack-smoke.XXXXXX)"
PIDS=()
cleanup() {
  for p in "${PIDS[@]:-}"; do kill "$p" 2>/dev/null; done
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

# ── prerequisiti: Docker + env dev (stesse variabili di dev/.env di app-start) ─
docker info >/dev/null 2>&1 || { fail "Docker non disponibile: lo smoke headless lo richiede."; exit 1; }
# In CI dev/.env non esiste: lo si crea dall'esempio (stesso comportamento di ensure_env
# in dev/lib/common.sh) PRIMA del compose, che lo legge automaticamente dalla dir dev/.
[ -f "$ROOT/dev/.env" ] || cp "$ROOT/dev/.env.example" "$ROOT/dev/.env"
set -a; . "$ROOT/dev/.env"; set +a
POSTGRES_PORT="${POSTGRES_PORT:-5433}"; POSTGRES_DB="${POSTGRES_DB:-appgrove}"
POSTGRES_USER="${POSTGRES_USER:-appgrove}"; POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-appgrove_local_dev}"

# ── infra reale: Postgres + ElasticMQ dal compose dev (idempotente) ───────────
step "Postgres + ElasticMQ (compose dev)…"
( cd "$ROOT/dev" && docker compose up -d --wait postgres elasticmq ) \
  || { fail "compose up postgres/elasticmq fallito"; exit 1; }

# ── migrazioni + seed: gli STESSI passi di app-start (idempotenti) ────────────
step "Flyway migrate (dev migrate)…"
"$ROOT/dev.sh" migrate || { fail "migrazioni fallite"; exit 1; }
step "seed utenti (dev/seed/seed.sql, idempotente)…"
docker exec -i "$(docker ps --format '{{.Names}}' | grep -m1 postgres)" \
  psql -q -U "$POSTGRES_USER" -d "$POSTGRES_DB" < "$ROOT/dev/seed/seed.sql" \
  || { fail "seed fallito"; exit 1; }

# ── artefatti con PROFILO DI BUILD dev + chiavi JWT locali ────────────────────
# Build dedicata (-Dquarkus.profile=dev): il wiring a build-time deve essere quello
# di sviluppo (es. payment provider = stub, non Paddle) come in `quarkus:dev` di
# app-start — un jar impacchettato di default è "prod" a build-time e in dev
# esploderebbe sul PaddlePaymentProvider non implementato (gated, UC 0022).
# Nota: sovrascrive i jar in target/ — chi serve l'artefatto di spedizione
# (boot-profiles.sh, CI deploy) fa la propria build.
step "build artefatti in profilo dev (mvn package -Dquarkus.profile=dev)…"
( cd "$ROOT/services" && mvn -B -q -pl core,fatture,auth -am package -DskipTests -Dquarkus.profile=dev ) \
  || { fail "build artefatti fallita"; exit 1; }
AUTH_KEYS="$ROOT/dev/auth"
if [ ! -f "$AUTH_KEYS/privateKey.pem" ]; then
  AUTH_KEYS="$TMP_DIR/auth-keys"; mkdir -p "$AUTH_KEYS"
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$AUTH_KEYS/privateKey.pem" 2>/dev/null
  openssl rsa -in "$AUTH_KEYS/privateKey.pem" -pubout -out "$AUTH_KEYS/publicKey.pem" 2>/dev/null
fi

# ── avvio dei 3 servizi in profilo dev (porte alternative) ────────────────────
JDBC="jdbc:postgresql://localhost:${POSTGRES_PORT}/${POSTGRES_DB}"
start_service() { # <nome> <porta> [env extra...]
  local name="$1" port="$2"; shift 2
  local log="$TMP_DIR/$name.log"
  env "$@" \
      QUARKUS_DATASOURCE_JDBC_URL="$JDBC" \
      QUARKUS_DATASOURCE_USERNAME="$POSTGRES_USER" \
      QUARKUS_DATASOURCE_PASSWORD="$POSTGRES_PASSWORD" \
      QUARKUS_HTTP_PORT="$port" \
      java -Dquarkus.profile=dev -jar "$ROOT/services/$name/target/quarkus-app/quarkus-run.jar" \
      > "$log" 2>&1 &
  PIDS+=($!)
}
step "avvio core (:$CORE_PORT), fatture (:$FATTURE_PORT), auth (:$AUTH_PORT) in profilo dev…"
start_service core "$CORE_PORT"
start_service fatture "$FATTURE_PORT"
start_service auth "$AUTH_PORT" \
  AUTH_LOCAL_PRIVATE_KEY="$AUTH_KEYS/privateKey.pem" AUTH_LOCAL_PUBLIC_KEY="$AUTH_KEYS/publicKey.pem"

# ── readiness + asserzioni end-to-end ─────────────────────────────────────────
wait_http() { # <nome> <url> <status atteso>
  local name="$1" url="$2" expected="$3" i=0 code=""
  while [ "$i" -lt "$BOOT_TIMEOUT" ]; do
    code="$(curl -s -o /dev/null -w '%{http_code}' "$url" 2>/dev/null)" || true
    [ "$code" = "$expected" ] && { ok "$name: $url → $code"; return 0; }
    sleep 1; i=$((i + 1))
  done
  fail "$name: $url → atteso $expected, ottenuto '${code:-nessuna risposta}' (timeout ${BOOT_TIMEOUT}s)"
  # diagnostica: causa radice + coda del log
  grep -B2 -A8 "Caused by" "$TMP_DIR/$name.log" 2>/dev/null | head -30
  tail -15 "$TMP_DIR/$name.log" 2>/dev/null
  return 1
}

rc=0
wait_http core    "http://localhost:$CORE_PORT/q/health/ready"    200 || rc=1
wait_http fatture "http://localhost:$FATTURE_PORT/q/health/ready" 200 || rc=1
wait_http auth    "http://localhost:$AUTH_PORT/api/auth/jwks"     200 || rc=1

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
