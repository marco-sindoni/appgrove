#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# tools/ci/services.sh — la lista dei servizi nel formato che serve
# all'integrazione continua (UC 0046, debito 2 della change 0041).
#
# Prima di questo script i nomi dei servizi erano ripetuti a mano in sette punti
# dei workflow (matrice di build, cicli `for app in platform fatture`, lista dei
# moduli Maven): aggiungere un'app voleva dire ricordarsi di toccare i workflow.
#
# ── Sorgente unica ──────────────────────────────────────────────────────────
# La scoperta dei servizi NON è reimplementata qui: arriva da
# `dev/lib/services.sh`, la libreria che deriva la mappa
# servizio → app_id → porta → schema → ruolo dai file che ogni servizio già
# possiede (`services/<svc>/src/main/resources/application.properties`) ed è la
# stessa usata dallo stack di sviluppo locale. Questo script si limita a
# FILTRARE e FORMATTARE quella mappa per i workflow. Una sola implementazione
# della derivazione, due consumatori (locale e CI).
#
# Dai ruoli della libreria discendono le liste della CI:
#   ruolo `core` + ruolo `app`  → servizi schierabili su ECS (core → app_id
#                                 `platform`, fatture → `fatture`): immagine su
#                                 ECR, servizio ECS, migrazioni Flyway
#   ruolo `auth`                → Lambda BFF (UC 0015): si spedisce come
#                                 function.zip e ha già job dedicati nei
#                                 workflow, quindi resta FUORI da queste liste
#   nessun application.properties → libreria condivisa (commons): mai schierata,
#                                 ma parte della build Maven
#
# Attenzione: la directory Maven e l'app_id DIVERGONO (directory `core` →
# app_id `platform`). Ogni riga porta entrambi: la directory serve a Maven e al
# percorso del Dockerfile, l'app_id ai nomi delle risorse AWS
# (`appgrove-<env>-<app_id>`).
#
# ── Perché non derivare dall'output Terraform `ci_deploy` ───────────────────
# L'output `ci_deploy` (marker `ci-services` in infra/envs/*/outputs.tf, tenuto
# da infra/scripts/service-add) resta la sorgente dei nomi RUNTIME delle risorse
# AWS — il nome del servizio ECS per l'health check — e lì continua a essere
# usato. Non può però essere la sorgente della lista in CI perché:
#   1. verify-pr gira senza credenziali AWS e senza stato Terraform: leggere un
#      output è impossibile per costruzione;
#   2. la build delle immagini e il gate sulle immagini native girano PRIMA di
#      qualunque apply — alla prima attivazione di un ambiente lo stato
#      Terraform non esiste ancora (attivazione per fasi, #12);
#   3. la lista serve anche in locale, senza rete.
# Le due restano coerenti perché nascono dallo stesso gesto: `service-add`
# scrive l'infrastruttura, lo scaffolding del servizio scrive `appgrove.app-id`.
#
# ── Uso (invocabile e verificabile in locale) ───────────────────────────────
#   tools/ci/services.sh list           # "<dir> <app_id>" per riga (default)
#   tools/ci/services.sh app-ids        # app_id, uno per riga (per i cicli `for`)
#   tools/ci/services.sh dirs           # directory Maven dei servizi schierabili
#   tools/ci/services.sh maven-modules  # librerie + servizi schierabili, per `mvn -pl`
#   tools/ci/services.sh matrix         # JSON {"include":[{"service":…,"app_id":…}]}
#                                       # per `fromJSON` in strategy.matrix
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SERVICES_DIR="$REPO_ROOT/services"

die() { echo "services.sh: $*" >&2; exit 1; }

DISCOVERY="$REPO_ROOT/dev/lib/services.sh"
[ -f "$DISCOVERY" ] \
  || die "manca $DISCOVERY (scoperta dei servizi, UC 0046): è la sorgente unica della mappa servizi"
# shellcheck source-path=SCRIPTDIR
# shellcheck source=../../dev/lib/services.sh
source "$DISCOVERY"

# Servizi schierabili su ECS = ruoli `core` e `app` (l'ordine — core, poi le app —
# è quello della libreria: la matrice di build è stabile fra una run e l'altra).
DEPLOYABLE=()   # righe "<dir> <app_id>"
while read -r svc; do
  [ -n "$svc" ] || continue
  app_id="$(service_app_id "$svc")"
  [ -n "$app_id" ] || die "services/$svc: app_id vuoto (atteso appgrove.app-id in application.properties)"
  # Sanity check: un servizio schierabile su ECS ha i Dockerfile di spedizione.
  for flavor in jvm native; do
    [ -f "$SERVICES_DIR/$svc/src/main/docker/Dockerfile.$flavor" ] \
      || die "services/$svc dichiara appgrove.app-id='$app_id' ma manca src/main/docker/Dockerfile.$flavor"
  done
  DEPLOYABLE+=("$svc $app_id")
done < <(services_by_role core; services_by_role app)

[ ${#DEPLOYABLE[@]} -gt 0 ] || die "nessun servizio schierabile trovato in services/ (atteso almeno core)"

# Librerie condivise: directory con pom.xml ma senza application.properties
# (commons). Non sono servizi — la libreria di scoperta le ignora, giustamente —
# ma fanno parte della build Maven, quindi le raccoglie questo script.
libraries() {
  local dir name
  for dir in "$SERVICES_DIR"/*/; do
    name="$(basename "$dir")"
    [ -f "$dir/pom.xml" ] || continue
    [ -f "$dir/src/main/resources/application.properties" ] && continue
    printf '%s\n' "$name"
  done
}

case "${1:-list}" in
  list)
    printf '%s\n' "${DEPLOYABLE[@]}"
    ;;
  app-ids)
    printf '%s\n' "${DEPLOYABLE[@]}" | awk '{print $2}'
    ;;
  dirs)
    printf '%s\n' "${DEPLOYABLE[@]}" | awk '{print $1}'
    ;;
  maven-modules)
    # Librerie condivise + servizi schierabili: è l'insieme di moduli su cui
    # gira la suite di invarianti (sicurezza/multi-tenancy) in verify-pr.
    { libraries; printf '%s\n' "${DEPLOYABLE[@]}" | awk '{print $1}'; } | paste -sd, -
    ;;
  matrix)
    # JSON su una riga, consumabile da `fromJSON(...)` in strategy.matrix.
    sep=''
    printf '{"include":['
    for row in "${DEPLOYABLE[@]}"; do
      printf '%s{"service":"%s","app_id":"%s"}' "$sep" "${row%% *}" "${row##* }"
      sep=','
    done
    printf ']}\n'
    ;;
  -h|--help|help)
    sed -n '/^# ── Uso/,/^# ────*$/p' "${BASH_SOURCE[0]}"
    ;;
  *)
    die "comando sconosciuto: $1 (usare: list | app-ids | dirs | maven-modules | matrix)"
    ;;
esac
