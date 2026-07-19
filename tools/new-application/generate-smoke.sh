#!/usr/bin/env bash
# generate-smoke.sh — collaudo di LIVELLO 3 del generatore `new-application` (UC 0046).
#
# Genera davvero un'app in una COPIA usa-e-getta del repo e ne esegue l'INTERA suite di test.
# È la dimostrazione letterale della promessa della skill: "l'app generata nasce con suite verde".
#
# Perché livello 3 e non un controllo strutturale più economico: un collaudo che si limita a
# contare i file dice solo che il generatore ha scritto qualcosa. Il modo tipico in cui questi
# modelli si rompono è un'altro — la libreria comune evolve, i modelli restano indietro, e il
# risultato NON COMPILA PIÙ. Solo compilando ed eseguendo i test lo si scopre prima che lo scopra
# l'utente su un ramo pieno di codice appena generato.
#
# Complementare al collaudo di parità (tools/scaffold-parity): quello coglie la divergenza
# silenziosa (i modelli restano indietro pur funzionando), questo coglie quella dura (non compila).
#
# Richiede Docker attivo (Dev Services avvia un Postgres vero) e rete per Maven.
#
# Uso:
#   tools/new-application/generate-smoke.sh                 # app mono-utente
#   tools/new-application/generate-smoke.sh --user-model multi
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
APP_ID="smoke_gen"
USER_MODEL="single"
[ "${1:-}" = "--user-model" ] && USER_MODEL="${2:-single}"

C_RESET=$'\033[0m'; C_GRN=$'\033[0;32m'; C_RED=$'\033[0;31m'; C_YEL=$'\033[0;33m'
ok()   { printf '%s✓ %s%s\n' "$C_GRN" "$*" "$C_RESET"; }
fail() { printf '%s✗ %s%s\n' "$C_RED" "$*" "$C_RESET"; }
info() { printf '%s· %s%s\n' "$C_YEL" "$*" "$C_RESET"; }

WORK="$(mktemp -d "${TMPDIR:-/tmp}/appgrove-genrate-smoke.XXXXXX")"
# La copia va SEMPRE rimossa: è un albero di sorgenti completo, lasciarlo in giro riempie il disco
# e, peggio, un'esecuzione successiva potrebbe leggerlo credendolo il repo vero.
cleanup() { rm -rf "$WORK"; }
trap cleanup EXIT INT TERM

info "copia usa-e-getta del repo in $WORK"
# Esclusioni: artefatti di build e dipendenze installate. Si copia il WORKING TREE (non un clone
# git) di proposito: il collaudo deve provare i modelli COSÌ COME SONO ORA, comprese le modifiche
# non ancora committate — è esattamente durante una change che i modelli invecchiano.
rsync -a \
  --exclude '.git/' \
  --exclude 'node_modules/' \
  --exclude 'target/' \
  --exclude 'dist/' \
  --exclude '.terraform/' \
  --exclude 'test-results/' \
  --exclude 'playwright-report/' \
  "$ROOT/" "$WORK/" || { fail "copia del repo fallita"; exit 1; }

info "generazione dell'app di prova '$APP_ID' (modello utente: $USER_MODEL)"
# --skip-infra: il wiring Terraform è delegato a infra/scripts/service-add, che richiede terraform
# e modificherebbe gli ambienti reali. Qui interessa il codice generato, non l'infrastruttura.
( cd "$WORK" && node tools/new-application/generate.mjs \
    --app-id "$APP_ID" --metric elementi --user-model "$USER_MODEL" --skip-infra ) \
  || { fail "generazione fallita"; exit 1; }
ok "generazione riuscita"

info "verifica che nessun segnaposto sia sopravvissuto"
# Un segnaposto sopravvissuto è il fallimento più insidioso: il file esiste, sembra giusto, e
# contiene un token che nessuno noterà finché non esplode a runtime.
RESIDUI="$(grep -rIl '@@[A-Z0-9_]\+@@' "$WORK/services/$APP_ID" \
            "$WORK/frontend/apps/backoffice/src/modules/$APP_ID" \
            "$WORK/docs/compliance/manifests/$APP_ID.yaml" \
            "$WORK/services/core/src/main/resources/pricing/$APP_ID.yaml" 2>/dev/null)"
if [ -n "$RESIDUI" ]; then
  fail "segnaposto non sostituiti nei file generati:"
  printf '%s\n' "$RESIDUI"
  exit 1
fi
ok "nessun segnaposto residuo"

info "esecuzione dell'INTERA suite dell'app generata (Postgres reale via Dev Services)"
if ( cd "$WORK/services" && mvn -B -pl "$APP_ID" -am test ); then
  ok "suite dell'app generata VERDE — la promessa della skill regge"
  exit 0
fi
fail "la suite dell'app generata è ROSSA: i modelli-sorgente sono invecchiati."
fail "Correggi tools/new-application/templates/ — NON rattoppare l'output generato."
exit 1
