# Implementation Log — Change 0047: Homepage + navigazione/footer + "Perché appgrove" e "Prezzi"

**Branch**: `change/0047-use-case-0037-homepage-nav-footer`
**Aree**: `site/` (Astro SSG — sito vetrina)
**Completata**: 2026-07-25
**Modalità**: autopilot — le risposte alle domande di approfondimento sono dell'agente; i quattro pilastri del messaggio e la formulazione del pilastro "Ready for AI" sono decisioni dello **sviluppatore**. Registro completo in [decisions.json](decisions.json).

## File modificati

| File | Azione |
|---|---|
| `site/src/content/marketing/types.ts` | Creato — tipo condiviso `MarketingContent` |
| `site/src/content/marketing/en.ts` | Creato — contenuti inglese (sorgente) |
| `site/src/content/marketing/it.ts` | Creato — contenuti italiano |
| `site/src/content/marketing/fr.ts` | Creato — contenuti francese |
| `site/src/content/marketing/es.ts` | Creato — contenuti spagnolo |
| `site/src/content/marketing/de.ts` | Creato — contenuti tedesco |
| `site/src/content/marketing/index.ts` | Creato — `Record<Locale, MarketingContent>` |
| `site/src/content/marketing/marketing.test.ts` | Creato — test parità valori 5 lingue |
| `site/src/lib/links.ts` | Creato — URL esterni e contatti |
| `site/src/pages/[lang]/index.astro` | Modificato — homepage vera (era segnaposto) |
| `site/src/pages/[lang]/why.astro` | Creato — pagina "Perché appgrove" |
| `site/src/pages/[lang]/pricing.astro` | Creato — pagina "Prezzi" |
| `site/src/layouts/BaseLayout.astro` | Modificato — top nav + footer arricchito |
| `site/public/.well-known/security.txt` | Creato — file RFC 9116 |
| `site/scripts/postbuild-check.mjs` | Modificato — parità 5 lingue estesa a why/pricing |
| `docs/_BACKLOG.md` | Modificato — tema trasversale "Ready for AI / MCP" |
| `docs/usecases/09-marketing-site/0037-*.md` | Modificato — punti aperti (rimandi) |
| `docs/usecases/09-marketing-site/0039,0040,0042,0043-*.md`, `11-apps/0053-*.md` | Modificati — sezioni punti aperti (rimandi) |
| `docs/usecases/_INDEX.md` | Modificato — UC 0037 🟡 → ✅ |

## Cosa è stato fatto

Le pagine brand del sito vetrina, nelle 5 lingue, alimentate da un modello di contenuti TypeScript tipizzato per lingua (`site/src/content/marketing/`): la **homepage** con la sequenza narrativa completa (hero → vetrina app onesta col catalogo piccolo → cross-sell → **Ready for AI** → privacy/EU → newsletter → CTA), le pagine **"Perché appgrove"** e **"Prezzi — come funziona la fatturazione"**, e la **top nav** + **footer** arricchiti nel `BaseLayout` condiviso (legali, Support, `security.txt`, newsletter). La parità delle 5 lingue è garantita a tempo di compilazione dal tipo `Record<Locale, MarketingContent>` e a runtime dal controllo post-build (esteso a why/pricing) e da un test dedicato.

## Decisioni prese

Condotta in **autopilot**: 21 decisioni dell'agente (prefisso `(autopilot)`), 3 dello sviluppatore (i quattro pilastri del messaggio, il pilastro "Ready for AI" e la conferma della sua formulazione "visione forte ma onesta"). In sintesi:

- **Modello contenuti**: moduli TypeScript tipizzati per lingua invece di markdown (i contenuti sono strutturati; parità di forma garantita dal compilatore).
- **Pilastri del messaggio** (sviluppatore): ecosistema comodo/sostenibile · un'unica piattaforma in crescita · **Ready for AI** (app richiamabili dagli assistenti AI via MCP — inquadramento onesto, visione non ancora attiva) · tutto in EU con pieni diritti GDPR.
- **Onestà dei claim**: catalogo app onesto (strumento faro "in arrivo", nessun link a landing inesistenti); newsletter come struttura visuale con bottone disabilitato (nessun invio finto); "Ready for AI" come principio di design, non disponibilità già attiva.
- **Navigazione/footer**: Blog e link social omessi (non esistono ancora), nav responsive senza menu a scomparsa, URL esterni/contatti centralizzati in `links.ts`.

Il registro strutturato completo, con le motivazioni, è in [decisions.json](decisions.json).

## Invarianti appgrove

Nessuno toccato: il sito vetrina è un artefatto statico pubblico, senza tenant, JWT, query tenant-scoped, modulo Terraform o logging applicativo. Resta valido l'invariante di progetto del sito — **parità 5 lingue** (compile-time + controllo post-build) e **gate di indicizzazione** (`noindex` fino al go-live), entrambi verdi.

## Note per il revisore

- **Contenuti da rivedere**: i testi marketing nelle 5 lingue sono AI-generati on-brand; la rilettura dei requisiti li ha già approvati nell'inquadramento, ma il tono/copy resta materia di revisione dell'utente.
- **Claim "Ready for AI" (importante)**: anticipa una capacità di prodotto (distribuzione MCP) che **non esiste ancora**. Formulato come visione/design (deciso dallo sviluppatore, "visione forte ma onesta"). Tema tracciato in [docs/_BACKLOG.md](../../docs/_BACKLOG.md) come voce trasversale grande, con la **riconciliazione dei contenuti allo stato reale prima del go-live**. Il sito resta comunque `noindex` fino al go-live.
- **Decisioni differite tracciate** (rimandi scritti negli use case che li possiedono): voce "Blog" in nav → UC 0042; wiring backend newsletter → UC 0039; link social nel footer → UC 0043; link card app → landing → UC 0053; slug localizzati per lingua → UC 0040. Più il tema MCP nel backlog.
- **gate privacy**: nessun segnale (pagine statiche, nessun trattamento). **gate parità scaffold**: nessun percorso-sorgente toccato. **typecheck `astro check`**: non nella suite canonica del sito e richiede installazione interattiva — non eseguito; la parità di tipo è comunque garantita dalla build e dal test.
- **Contratti cross-area**: nessuno. I link esterni alla SPA (`app.appgrove.app`) e i `mailto:` sono URL, non contratti di codice.

## Test

Suite canonica `./run-tests.sh site` — **verde**:
- `npm test` (vitest): 20 test verdi, di cui i nuovi in `marketing.test.ts` (10) — parità valori 5 lingue: nessuna stringa vuota, stessa forma/lunghezza liste rispetto alla sorgente EN.
- `npm run build` (astro build): 42 pagine generate, incluse home/why/pricing in 5 lingue.
- `npm run check` (controllo post-build): parità 5 lingue (home + why + pricing + legali), hreflang completo, `noindex` attivo, nessun link interno rotto — tutto verde.

## Stato criteri di accettazione

- [x] Homepage `/<lang>/` in 5 lingue con sequenza narrativa completa e quattro pilastri, catalogo onesto anche con una sola app.
- [x] Pilastro "Ready for AI" presente e inquadrato onestamente (design/visione, MCP spiegato in linguaggio piano), senza affermare disponibilità già attiva.
- [x] Pagine `/<lang>/why/` e `/<lang>/pricing/` in 5 lingue (nessuna founder story; nessun prezzo numerico, rimando alla Refund Policy).
- [x] Top nav e footer presenti su ogni pagina, coerenti nelle 5 lingue, responsive; legali raggiungibili dalla navigazione.
- [x] `/.well-known/security.txt` servito e valido.
- [x] `npm test` + `npm run build` + `npm run check` verdi.
- [x] Modello marketing senza stringhe vuote e con stessa forma nelle 5 lingue (test vitest).
