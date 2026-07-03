#!/usr/bin/env bash
# run-tests.sh — esegue TUTTI i test automatici di TUTTI i moduli del monorepo appgrove.
#
# Aree (vedi CLAUDE.md "Esecuzione dei test"):
#   • backend  — services/* (Quarkus/Maven)  → `mvn test`  [richiede Docker/Colima: Testcontainers/Dev Services]
#   • frontend — frontend/  (npm workspaces)  → `npm test` + `npm run e2e`  [vitest + Playwright L2 (UC 0029),
#                browser chromium auto-installato se assente; la suite L3 sandbox NON è qui: è pre-release]
#   • infra    — infra/     (Terraform)       → `terraform fmt -check` + `validate` (best-effort se inizializzato)
#   • compliance — tools/compliance (Node)    → parità lingue dei manifesti dati + freshness RoPA (UC 0030;
#                dipendenze npm auto-installate se assenti; il check @PersonalData↔manifesto è nei test backend)
#
# Esegue TUTTE le aree selezionate (non si ferma al primo errore), raccoglie gli esiti e ritorna
# exit-code != 0 se QUALSIASI suite fallisce. È la SORGENTE DI VERITÀ unica per "lanciare tutti i test".
#
# Uso:
#   ./run-tests.sh                 # tutte le aree
#   ./run-tests.sh backend         # solo una/più aree: backend | frontend | infra | compliance
#   ./run-tests.sh frontend infra
#   ./run-tests.sh -h
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

C_RESET=$'\033[0m'; C_BLU=$'\033[1;36m'; C_GRN=$'\033[0;32m'; C_RED=$'\033[0;31m'; C_YEL=$'\033[0;33m'
hdr()  { printf '\n%s━━ %s %s\n' "$C_BLU" "$*" "$C_RESET"; }
ok()   { printf '%s✓ %s%s\n' "$C_GRN" "$*" "$C_RESET"; }
fail() { printf '%s✗ %s%s\n' "$C_RED" "$*" "$C_RESET"; }
warn() { printf '%s! %s%s\n' "$C_YEL" "$*" "$C_RESET"; }

usage() { sed -n '2,16p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

# aree richieste (default: tutte)
AREAS=()
for a in "$@"; do
  case "$a" in
    backend|frontend|infra|compliance) AREAS+=("$a") ;;
    -h|--help) usage; exit 0 ;;
    *) echo "area sconosciuta: $a (usa: backend | frontend | infra | compliance)" >&2; exit 2 ;;
  esac
done
[ ${#AREAS[@]} -eq 0 ] && AREAS=(backend frontend infra compliance)

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

run_frontend() {
  hdr "FRONTEND — frontend/ (npm test + Playwright e2e)"
  if [ ! -d "$ROOT/frontend/node_modules" ]; then
    warn "frontend/node_modules assente: installo le dipendenze (npm ci)…"
    ( cd "$ROOT/frontend" && { npm ci || npm install; } ) || { fail "frontend: install dipendenze fallita"; record frontend FAIL; return; }
  fi
  local rc=0
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
  hdr "INFRA — infra/ (terraform fmt/validate)"
  if ! command -v terraform >/dev/null 2>&1; then
    warn "terraform non installato: salto (la validazione completa gira in CI, UC 0005)."; record infra SKIP; return
  fi
  local rc=0
  terraform -chdir="$ROOT/infra" fmt -check -recursive || rc=1
  # validate richiede `init` (provider): lo eseguiamo solo se già inizializzato, per non scaricare in rete.
  if [ -d "$ROOT/infra/.terraform" ]; then
    terraform -chdir="$ROOT/infra" validate || rc=1
  else
    warn "infra non inizializzata (.terraform assente): eseguito solo fmt -check; 'validate' salta (init in CI)."
  fi
  if [ "$rc" -eq 0 ]; then ok "infra: ok"; record infra OK; else fail "infra: problemi (fmt/validate)"; record infra FAIL; fi
}

# Check compliance (UC 0030): parità lingue dei manifesti dati + freshness del RoPA generato.
# Il check @PersonalData ↔ manifesto gira invece nei test backend (JUnit, services/commons).
run_compliance() {
  hdr "COMPLIANCE — tools/compliance (parità lingue manifesti + freshness RoPA)"
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
  if [ "$rc" -eq 0 ]; then ok "compliance: ok"; record compliance OK; else fail "compliance: check falliti"; record compliance FAIL; fi
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
