#!/usr/bin/env bash
# boot-profiles.sh — smoke di avvio degli ARTEFATTI impacchettati nei PROFILI DI SPEDIZIONE.
#
# Perché esiste (change 0037): il bug `queue-prefix` (regressione della change 0036) faceva
# fallire l'avvio in dev/prod con `SRCFG00014`, ma NESSUN test avvia l'app fuori dal profilo
# `test` — i bean infrastrutturali sono `@UnlessBuildProfile("test")` e la loro config non
# viene mai validata dalla suite. Questo smoke chiude quella classe di bug: lancia il
# `quarkus-run.jar` di ogni servizio nel profilo con cui viene spedito, con config FINTA ma
# PRESENTE (nessuna infrastruttura: le connessioni reali sono lazy), e pretende che il server
# arrivi a "Listening on" — cioè che TUTTA la validazione config e l'init dei bean passino.
#
#   • core, fatture → profilo `prod` con le stesse env della task definition ECS
#     (infra/modules/microsaas_app/ecs.tf) valorizzate con finti;
#   • auth          → profilo `cloud` con le stesse env della Lambda (auth_lambda.tf).
#
# Il profilo `dev` è coperto dallo smoke gemello `stack-headless.sh` (infra locale REALE).
# FAIL: errori di config (SRCFG*/Failed to load config value), crash all'avvio, o timeout.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
C_RESET=$'\033[0m'; C_GRN=$'\033[0;32m'; C_RED=$'\033[0;31m'; C_BLU=$'\033[1;36m'
ok()   { printf '%s✓ %s%s\n' "$C_GRN" "$*" "$C_RESET"; }
fail() { printf '%s✗ %s%s\n' "$C_RED" "$*" "$C_RESET"; }
step() { printf '%s▶ %s%s\n' "$C_BLU" "$*" "$C_RESET"; }

BOOT_TIMEOUT="${BOOT_TIMEOUT:-90}"   # secondi per arrivare a "Listening on" (primo boot JVM freddo)
TMP_DIR="$(mktemp -d /tmp/appgrove-boot-smoke.XXXXXX)"
trap 'rm -rf "$TMP_DIR"' EXIT

# ── artefatti: build SEMPRE nella variante di spedizione (build-profile default/prod) ──
# Sempre, non "se mancano": lo smoke gemello stack-headless.sh sovrascrive target/
# con jar a build-profile dev (payment provider stub, ecc.) — qui serve la variante
# che va davvero in deploy.
ensure_jars() {
  step "build artefatti di spedizione (mvn package -DskipTests)…"
  ( cd "$ROOT/services" && mvn -B -q -pl core,fatture,auth -am package -DskipTests ) \
    || { fail "build artefatti fallita"; return 1; }
}

# ── env finte ma presenti (specchio di ecs.tf / auth_lambda.tf) ───────────────
# Host inesistenti/irraggiungibili di proposito: se un servizio le CONTATTASSE
# all'avvio (invece che lazy) lo smoke lo farebbe emergere.
ecs_like_env() {
  echo "APPGROVE_ENV=smoke"
  echo "QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://smoke.invalid:5432/appgrove"
  echo "QUARKUS_DATASOURCE_USERNAME=smoke"
  echo "QUARKUS_DATASOURCE_PASSWORD=smoke"
  echo "APPGROVE_SQS_QUEUE_PREFIX=appgrove-smoke-"
  echo "APPGROVE_SQS_REGION=eu-west-1"
  echo "APPGROVE_S3_REGION=eu-west-1"
  echo "APPGROVE_GDPR_EXPORT_BUCKET=smoke-bucket"
}

lambda_like_env() {
  echo "QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://smoke.invalid:5432/appgrove"
  echo "AUTH_DB_SECRET_ARN=arn:aws:secretsmanager:eu-west-1:000000000000:secret:smoke"
  echo "AUTH_COGNITO_REGION=eu-west-1"
  echo "AUTH_COGNITO_USER_POOL_ID=eu-west-1_smoke"
  echo "AUTH_COGNITO_CLIENT_ID=smoke-client"
  echo "AUTH_COGNITO_CLIENT_SECRET_PARAM=/appgrove/smoke/auth/client-secret"
  echo "AUTH_APP_BASE_URL=https://app.smoke.invalid"
  echo "AUTH_MAIL_FROM=noreply@smoke.invalid"
}

# boot_one <servizio> <profilo> <env-provider-fn>
boot_one() {
  local service="$1" profile="$2" env_fn="$3"
  local jar="$ROOT/services/$service/target/quarkus-app/quarkus-run.jar"
  local log="$TMP_DIR/$service-$profile.log"

  step "$service (profilo $profile)…"
  # QUARKUS_HTTP_PORT=0: porta effimera, nessuna collisione con stack dev/altri smoke.
  ( "$env_fn" | sed 's/^/export /'; echo "export QUARKUS_HTTP_PORT=0"
    echo "exec java -Dquarkus.profile=$profile -jar '$jar'" ) | bash > "$log" 2>&1 &
  local pid=$!

  local i=0 outcome=""
  while [ "$i" -lt "$BOOT_TIMEOUT" ]; do
    if grep -qE "SRCFG[0-9]+|Failed to load config value|Failed to start application" "$log"; then
      outcome=config_fail; break
    fi
    if grep -q "Listening on" "$log"; then outcome=ok; break; fi
    if ! kill -0 "$pid" 2>/dev/null; then outcome=died; break; fi
    sleep 1; i=$((i + 1))
  done
  kill "$pid" 2>/dev/null; wait "$pid" 2>/dev/null

  case "$outcome" in
    ok)  ok "$service ($profile): avvio pulito (validazione config superata)"; return 0 ;;
    config_fail)
      fail "$service ($profile): ERRORE DI CONFIG all'avvio — questo è esattamente il bug di classe queue-prefix"
      grep -E "SRCFG[0-9]+|Failed to load config value|required but" "$log" | head -5
      return 1 ;;
    died)
      fail "$service ($profile): processo terminato prima di 'Listening on'"; tail -15 "$log"; return 1 ;;
    *)
      fail "$service ($profile): timeout (${BOOT_TIMEOUT}s) senza 'Listening on'"; tail -15 "$log"; return 1 ;;
  esac
}

ensure_jars || exit 1
rc=0
boot_one core    prod  ecs_like_env    || rc=1
boot_one fatture prod  ecs_like_env    || rc=1
boot_one auth    cloud lambda_like_env || rc=1
exit "$rc"
