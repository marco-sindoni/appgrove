#!/usr/bin/env bash
# run-tests.sh — esegue TUTTI i test automatici di TUTTI i moduli del monorepo appgrove.
#
# Aree (vedi CLAUDE.md "Esecuzione dei test"):
#   • backend  — services/* (Quarkus/Maven)  → `mvn test`  [richiede Docker/Colima: Testcontainers/Dev Services]
#   • frontend — frontend/  (npm workspaces)  → `npm run typecheck` + `npm test` + `npm run e2e`
#                [controllo dei tipi `tsc --noEmit` su tutti i pacchetti e le app (change 0075: `vite build`
#                traspila SENZA verificare i tipi, quindi senza questo passo un errore di tipo resta invisibile)
#                + vitest + Playwright L2 (UC 0029), browser chromium auto-installato se assente;
#                la suite L3 sandbox NON è qui: è pre-release]
#   • infra    — infra/     (Terraform)       → infra/scripts/check (fmt + validate per root, + tflint/checkov/actionlint se presenti; actionlint = lint dei workflow CI, UC 0005)
#   • compliance — tools/compliance (Node)    → parità lingue dei manifesti dati + freshness RoPA (UC 0030;
#                dipendenze npm auto-installate se assenti; il check @PersonalData↔manifesto è nei test backend)
#   • tooling  — tools/new-application + tools/scaffold-parity + tools/drop-application + tools/pricing-change + tools/finalize-landing + tools/e2e-coverage (UC 0046/0048/0047/0057/0093) →
#                collaudo delle skill `new-application`, `drop-application`, `pricing-change` e `finalize-landing`,
#                più il controllo del registro di copertura end-to-end:
#                (1) parità dei modelli-sorgente contro l'app #1 `fatture` — coglie la divergenza SILENZIOSA
#                (i modelli restano indietro pur continuando a funzionare); (2) collaudo di LIVELLO 3 — genera
#                davvero un'app in una copia usa-e-getta e ne esegue l'INTERA suite, cogliendo la divergenza
#                DURA (non compila più); (3) de-generatore drop-application — round-trip genera→de-genera che
#                deve riportare il repo identico (simmetria col generatore) + inversi delle modifiche condivise;
#                (4) pricing-change — fee effettiva (avviso soft >10%) + modifiche al pricing-as-code con
#                immutabilità (nuovo prezzo = nuovo tier, mai muta un prezzo vivo) su fixture YAML;
#                (5) e2e-coverage (UC 0093) — il registro docs/testing/copertura-e2e.yaml deve rispecchiare i
#                test end-to-end realmente presenti (etichette [J-*] nei titoli) e classificare OGNI use case
#                del catalogo: coglie la mappa di copertura che invecchia in silenzio.
#                È lenta e volutamente FUORI da `./run-tests.sh backend`, per non appesantire i cicli rapidi;
#                inclusa nell'esecuzione completa. [richiede Docker]
#   • smoke    — tools/smoke/ (change 0037)   → avvio REALE degli artefatti: boot-profiles.sh (jar impacchettati
#                nei profili di spedizione prod/cloud, config finta, validazione config) + stack-headless.sh
#                (Postgres+ElasticMQ veri, migrate+seed, 3 servizi in profilo dev, login end-to-end).
#                Chiude la classe di bug "l'app non parte fuori dal profilo test" (regressione queue-prefix).
#   • platform — tools/platform-e2e/ (UC 0090) → suite end-to-end di PIATTAFORMA: browser vero (Playwright)
#                sui frontend costruiti davvero, contro lo stack backend VERO (Postgres+ElasticMQ+Mailpit,
#                tutti i servizi in profilo dev su porte dedicate +12000) con email reali verificate via
#                Mailpit. Journey indipendenti, un tenant fresco ciascuno. FUORI dal gate rapido per-change
#                (come tooling/smoke), dentro l'esecuzione completa. [richiede Docker]
#   • site     — site/ (Astro SSG, UC 0036)    → vitest (renderer legali/i18n + template landing via Container API,
#                UC 0038) + `astro build` + controllo post-build (parità 5 lingue, nessun token residuo, hreflang,
#                noindex, link interni, + landing per-app: parità 5 lingue + Open Graph, UC 0038).
#
# Esegue TUTTE le aree selezionate (non si ferma al primo errore), raccoglie gli esiti e ritorna
# exit-code != 0 se QUALSIASI suite fallisce. È la SORGENTE DI VERITÀ unica per "lanciare tutti i test".
#
# Uso:
#   ./run-tests.sh                 # tutte le aree
#   ./run-tests.sh backend         # solo una/più aree: backend | frontend | infra | compliance | tooling | smoke | platform | site
#   ./run-tests.sh frontend infra
#   ./run-tests.sh -h
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

C_RESET=$'\033[0m'; C_BLU=$'\033[1;36m'; C_GRN=$'\033[0;32m'; C_RED=$'\033[0;31m'; C_YEL=$'\033[0;33m'
hdr()  { printf '\n%s━━ %s %s\n' "$C_BLU" "$*" "$C_RESET"; }
ok()   { printf '%s✓ %s%s\n' "$C_GRN" "$*" "$C_RESET"; }
fail() { printf '%s✗ %s%s\n' "$C_RED" "$*" "$C_RESET"; }
warn() { printf '%s! %s%s\n' "$C_YEL" "$*" "$C_RESET"; }

usage() { sed -n '2,49p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

# aree richieste (default: tutte)
AREAS=()
for a in "$@"; do
  case "$a" in
    backend|frontend|infra|compliance|tooling|smoke|platform|site) AREAS+=("$a") ;;
    -h|--help) usage; exit 0 ;;
    *) echo "area sconosciuta: $a (usa: backend | frontend | infra | compliance | tooling | smoke | platform | site)" >&2; exit 2 ;;
  esac
done
[ ${#AREAS[@]} -eq 0 ] && AREAS=(backend frontend infra compliance tooling smoke platform site)

declare -a RESULTS=()
record() { RESULTS+=("$1|$2"); }   # area|esito(OK/FAIL/SKIP)

# Assicura che Colima sia avviato (i test backend usano Testcontainers/Dev Services → serve Docker).
# Se Colima NON è in esecuzione, esegue un ciclo stop→start del servizio per ripartire da stato pulito.
ensure_colima() {
  command -v colima >/dev/null 2>&1 || return 0   # Colima non installato: lascio decidere a `docker info`
  if colima status >/dev/null 2>&1; then
    return 0                                       # già in esecuzione
  fi
  warn "Colima fermo: riavvio il servizio (stop → start)…"
  colima stop  >/dev/null 2>&1 || true            # pulisce eventuale stato residuo
  if colima start; then
    ok "Colima avviato."
  else
    fail "Colima: avvio fallito."
  fi
}

run_backend() {
  hdr "BACKEND — services/* (mvn test)"
  ensure_colima
  if ! docker info >/dev/null 2>&1; then
    warn "Docker/Colima non disponibile: i test backend usano Testcontainers/Dev Services e falliranno."
  fi
  if ( cd "$ROOT/services" && mvn -B test ); then
    ok "backend: test verdi"; record backend OK
  else
    fail "backend: test falliti"; record backend FAIL
  fi
}

# Assicura il browser Playwright per gli e2e L2 (UC 0029): `playwright install` è idempotente
# (scarica chromium solo se assente) — stesso spirito di ensure_colima per il backend.
ensure_playwright() {
  ( cd "$ROOT/frontend" && npx playwright install chromium ) \
    || warn "Playwright: install del browser fallita (gli e2e potrebbero fallire)."
}

# I pacchetti-libreria del workspace (frontend/packages/*) sono consumati SOLO dal loro dist/ costruito
# (package.json exports → ./dist/index.js), che è gitignorato e NON viene ricostruito da vitest né dal
# `vite build` dell'app. Costruirli qui, prima dei test, tiene l'entrypoint canonico autoconsistente: una
# modifica ai sorgenti di un pacchetto (es. le traduzioni i18n) non lascia più i consumatori con un dist
# stantìo (change 0057). Scoperta dinamica come per i servizi: ogni pacchetto con uno script `build` viene
# incluso da solo, senza toccare questo script quando se ne aggiunge uno nuovo.
build_frontend_packages() {
  local args=() d
  for d in "$ROOT"/frontend/packages/*/; do
    [ -f "${d}package.json" ] || continue
    node -e "process.exit(require('${d}package.json').scripts?.build?0:1)" 2>/dev/null \
      && args+=(--workspace "packages/$(basename "$d")")
  done
  [ ${#args[@]} -eq 0 ] && return 0
  ( cd "$ROOT/frontend" && npm run build "${args[@]}" )
}

run_frontend() {
  hdr "FRONTEND — frontend/ (tsc --noEmit + npm test + Playwright e2e)"
  if [ ! -d "$ROOT/frontend/node_modules" ]; then
    warn "frontend/node_modules assente: installo le dipendenze (npm ci)…"
    ( cd "$ROOT/frontend" && { npm ci || npm install; } ) || { fail "frontend: install dipendenze fallita"; record frontend FAIL; return; }
  fi
  # I consumatori (vitest + `vite build` per gli e2e) risolvono i pacchetti-libreria dal loro dist/: va
  # costruito prima, altrimenti un dist assente rompe la risoluzione e uno stantìo serve codice vecchio.
  if build_frontend_packages; then
    ok "frontend: pacchetti-libreria costruiti"
  else
    fail "frontend: build dei pacchetti-libreria fallita"; record frontend FAIL; return
  fi
  local rc=0
  # Controllo dei tipi (change 0075): `vite build` TRASPILA senza verificare i tipi, quindi senza questo
  # passo un errore di tipo reale resta invisibile al cancello. Va DOPO la build dei pacchetti (le app
  # risolvono i tipi dei pacchetti condivisi dal loro dist/) e PRIMA delle suite, per fallire in fretta.
  if ( cd "$ROOT/frontend" && npm run typecheck ); then
    ok "frontend: controllo dei tipi verde"
  else
    fail "frontend: controllo dei tipi fallito (tsc --noEmit)"; rc=1
  fi
  if ( cd "$ROOT/frontend" && npm test ); then
    ok "frontend: unit/component verdi"
  else
    fail "frontend: unit/component falliti"; rc=1
  fi
  # L2 (UC 0029): gli E2E Playwright sono parte del gate canonico — "frontend verde" = vitest + e2e.
  ensure_playwright
  if ( cd "$ROOT/frontend" && npm run e2e ); then
    ok "frontend: e2e verdi"
  else
    fail "frontend: e2e falliti"; rc=1
  fi
  if [ "$rc" -eq 0 ]; then record frontend OK; else record frontend FAIL; fi
}

run_infra() {
  hdr "INFRA — infra/ (scripts/check: fmt + validate + terraform test moduli + tflint/checkov/actionlint se presenti)"
  if ! command -v terraform >/dev/null 2>&1; then
    warn "terraform non installato: salto (la validazione completa gira in CI, UC 0005)."; record infra SKIP; return
  fi
  # Delega a infra/scripts/check (UC 0003): fmt -check su tutto, validate su ogni
  # root (init -backend=false: nessuna credenziale AWS; i provider si scaricano
  # una volta sola nella cache condivisa), terraform test sui moduli con suite
  # (microsaas_app, provider mock: offline — UC 0004), più tflint e checkov se
  # installati.
  local infra_ok=1
  if "$ROOT/infra/scripts/check"; then
    ok "infra (terraform): ok"
  else
    fail "infra (terraform): problemi (vedi output di scripts/check)"; infra_ok=0
  fi

  # Test unitari delle Lambda Python (db-bootstrap UC 0004, pre-token-gen UC 0016,
  # custom-message UC 0018, uptime-canary UC 0007): logica pura con DB/Data API mockati, nessun cloud, solo
  # stdlib (unittest). Il custom-message rende i template della sorgente condivisa
  # `shared/email-templates`, quindi copre anche il contenuto che spediremo davvero.
  if command -v python3 >/dev/null 2>&1; then
    if ( cd "$ROOT/infra/modules/platform_shared/lambda" \
          && python3 -m unittest test_db_bootstrap \
          && ( cd pre_token_gen && python3 -m unittest test_handler ) \
          && ( cd custom_message && python3 -m unittest test_handler ) ) \
       && ( cd "$ROOT/infra/modules/uptime_canary/lambda" && python3 -m unittest test_uptime_ping ); then
      ok "infra (lambda python): ok"
    else
      fail "infra (lambda python): test rossi"; infra_ok=0
    fi
  else
    warn "python3 non disponibile: salto i test delle Lambda Python."
  fi

  if [ "$infra_ok" -eq 1 ]; then record infra OK; else record infra FAIL; fi
}

# Check compliance (UC 0030): parità lingue dei manifesti dati + freshness del RoPA generato,
# + check documenti legali pubblici (UC 0002): parità 5 lingue + frontmatter + integrità token entity.yaml.
# Il check @PersonalData ↔ manifesto gira invece nei test backend (JUnit, services/commons).
run_compliance() {
  hdr "COMPLIANCE — tools/compliance (parità lingue manifesti + freshness RoPA + documenti legali 5 lingue)"
  if ! command -v node >/dev/null 2>&1; then
    warn "node non installato: salto il check compliance."; record compliance SKIP; return
  fi
  if [ ! -d "$ROOT/tools/compliance/node_modules" ]; then
    warn "tools/compliance/node_modules assente: installo le dipendenze (npm ci)…"
    ( cd "$ROOT/tools/compliance" && { npm ci || npm install; } ) \
      || { fail "compliance: install dipendenze fallita"; record compliance FAIL; return; }
  fi
  local rc=0
  ( cd "$ROOT/tools/compliance" && npm test ) || rc=1
  ( cd "$ROOT/tools/compliance" && npm run check ) || rc=1
  ( cd "$ROOT/tools/compliance" && npm run legal-check ) || rc=1
  if [ "$rc" -eq 0 ]; then ok "compliance: ok"; record compliance OK; else fail "compliance: check falliti"; record compliance FAIL; fi
}

# Smoke di avvio (change 0037): artefatti reali nei profili reali. Vedi tools/smoke/*.sh.
run_tooling() {
  hdr "TOOLING — skill new-application (parità + livello 3) + de-generatore drop-application (UC 0046/0048)"
  if ! command -v node >/dev/null 2>&1; then
    warn "node non installato: salto il collaudo tooling."; record tooling SKIP; return
  fi
  ensure_colima   # il collaudo livello 3 compila ed esegue i test dell'app generata (Postgres reale)
  if [ ! -d "$ROOT/tools/scaffold-parity/node_modules" ] && [ -f "$ROOT/tools/scaffold-parity/package-lock.json" ]; then
    ( cd "$ROOT/tools/scaffold-parity" && { npm ci || npm install; } ) >/dev/null 2>&1 || true
  fi
  local rc=0
  # (1) test degli strumenti stessi + parità modelli ↔ app #1 (divergenza silenziosa)
  ( cd "$ROOT/tools/scaffold-parity" && npm test )       || rc=1
  ( cd "$ROOT/tools/scaffold-parity" && npm run parity ) || rc=1
  # (2) livello 3: genera un'app vera ed eseguine l'intera suite (divergenza dura)
  "$ROOT/tools/new-application/generate-smoke.sh"        || rc=1
  # (3) de-generatore drop-application: round-trip genera→de-genera + inversi/parità (UC 0048)
  ( cd "$ROOT/tools/drop-application" && npm test )       || rc=1
  # (4) skill pricing-change: fee effettiva + modifiche al pricing-as-code con immutabilità (UC 0047)
  if [ ! -d "$ROOT/tools/pricing-change/node_modules" ] && [ -f "$ROOT/tools/pricing-change/package-lock.json" ]; then
    ( cd "$ROOT/tools/pricing-change" && { npm ci || npm install; } ) >/dev/null 2>&1 || true
  fi
  ( cd "$ROOT/tools/pricing-change" && npm test )        || rc=1
  # (5) skill finalize-landing: immagine Open Graph + preflight + transizione draft→published (UC 0057).
  # La cattura screenshot (Playwright) SALTA con grazia se il browser non è disponibile; qui non si
  # scarica un browser (la cache è globale e condivisa col frontend). Il download del browser di
  # Playwright si salta in install: già presente in cache, oppure lo installa l'area frontend.
  if [ ! -d "$ROOT/tools/finalize-landing/node_modules" ] && [ -f "$ROOT/tools/finalize-landing/package-lock.json" ]; then
    ( cd "$ROOT/tools/finalize-landing" && export PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 && { npm ci || npm install; } ) >/dev/null 2>&1 || true
  fi
  ( cd "$ROOT/tools/finalize-landing" && npm test )      || rc=1
  # (6) registro di copertura end-to-end (UC 0093): test dello strumento su cartelle di prova +
  # controllo del registro VERO contro i test presenti nel repository (docs/testing/README.md).
  if [ ! -d "$ROOT/tools/e2e-coverage/node_modules" ] && [ -f "$ROOT/tools/e2e-coverage/package-lock.json" ]; then
    ( cd "$ROOT/tools/e2e-coverage" && { npm ci || npm install; } ) >/dev/null 2>&1 || true
  fi
  ( cd "$ROOT/tools/e2e-coverage" && npm test )          || rc=1
  ( cd "$ROOT/tools/e2e-coverage" && npm run check )     || rc=1
  if [ "$rc" -eq 0 ]; then ok "tooling: ok"; record tooling OK; else fail "tooling: fallito"; record tooling FAIL; fi
}

run_smoke() {
  hdr "SMOKE — tools/smoke (boot artefatti nei profili di spedizione + stack headless dev)"
  ensure_colima
  local rc=0
  "$ROOT/tools/smoke/boot-profiles.sh"   || rc=1
  "$ROOT/tools/smoke/stack-headless.sh"  || rc=1
  if [ "$rc" -eq 0 ]; then ok "smoke: ok"; record smoke OK; else fail "smoke: fallito"; record smoke FAIL; fi
}

# Suite e2e di piattaforma (UC 0090): browser vero + stack vero + Mailpit. Vedi tools/platform-e2e/README.md.
run_platform() {
  hdr "PLATFORM — tools/platform-e2e (browser vero su stack vero + Mailpit, journey J-*)"
  ensure_colima
  ensure_playwright   # stessa cache browser del frontend: install idempotente
  if "$ROOT/tools/platform-e2e/run.sh"; then
    ok "platform: journey verdi"; record platform OK
  else
    fail "platform: journey rossi (diagnosi: tools/platform-e2e/README.md)"; record platform FAIL
  fi
}

# Vetrina Astro (UC 0036): renderer dei legali/i18n (vitest) + build statica reale
# + controllo post-build (parità 5 lingue, token risolti, hreflang, noindex, link).
run_site() {
  hdr "SITE — site/ (Astro SSG: vitest + astro build + controllo post-build)"
  if ! command -v node >/dev/null 2>&1; then
    warn "node non installato: salto il check site."; record site SKIP; return
  fi
  if [ ! -d "$ROOT/site/node_modules" ]; then
    warn "site/node_modules assente: installo le dipendenze (npm ci)…"
    ( cd "$ROOT/site" && { npm ci || npm install; } ) \
      || { fail "site: install dipendenze fallita"; record site FAIL; return; }
  fi
  local rc=0
  ( cd "$ROOT/site" && npm test )      || rc=1
  ( cd "$ROOT/site" && npm run build ) || rc=1
  ( cd "$ROOT/site" && npm run check ) || rc=1
  if [ "$rc" -eq 0 ]; then ok "site: ok"; record site OK; else fail "site: check falliti"; record site FAIL; fi
}

for area in "${AREAS[@]}"; do "run_$area"; done

# ── riepilogo ────────────────────────────────────────────────────────────────
hdr "RIEPILOGO"
overall=0
for r in "${RESULTS[@]}"; do
  a="${r%%|*}"; s="${r##*|}"
  case "$s" in
    OK)   ok   "$a" ;;
    SKIP) warn "$a (saltata)" ;;
    FAIL) fail "$a"; overall=1 ;;
  esac
done
[ "$overall" -eq 0 ] && ok "TUTTE le suite eseguite sono verdi." || fail "Almeno una suite è ROSSA."
exit "$overall"
