# Requisiti — App #1 landing: bozza → published + fix seed screenshot (UC 0053)

**Change**: `0054-use-case-0053-app1-landing` · **Modalità**: autopilot
**Use case sorgente**: [docs/usecases/11-apps/0053-app1-landing.md](../../docs/usecases/11-apps/0053-app1-landing.md)
**Fonte decisioni di prodotto**: [docs/14-sito-vetrina-legale.md](../../docs/14-sito-vetrina-legale.md) — dec. G25 (8 sezioni), 9/51 (bozza da template), 52 (gate published), 53 (deploy CI al merge), 35 (review umana della copy), F (brand).
**Aree toccate**: `site/` (contenuto landing + eventuale link homepage) e `tools/finalize-landing/` (fix del seed screenshot + test). Nessun backend, nessuna infra, nessuna SPA applicativa.

## 1. Obiettivo

Portare la landing pubblica della **prima app reale, `Fatture`** (app #1, B2C mono-utente) da **bozza a `published`**,
completando l'intera Definition of Done di UC 0053: otto sezioni on-brand nelle cinque lingue con copy **veritiera**
fondata sulle funzionalità e sul listino reali, **screenshot reali** dell'app, **immagine Open Graph**, **review della
copy approvata dallo sviluppatore** e transizione `draft → published`. Per farlo si **corregge un difetto in codice già
consegnato** (UC 0057): il seed del comando `screenshots` del tool `finalize-landing`.

## 2. Scope

### 2a. Contenuto della landing (`site/`)
1. **Cartella contenuti** `site/src/content/landings/fatture/` con sei file (modellati su `example/` e sul template
   `tools/new-application/templates/landings/@@APP_ID@@/`): `en.ts` (sorgente della copy), `it.ts`, `fr.ts`, `es.ts`,
   `de.ts`, `index.ts` → `export const fattureLanding: Landing = { appId: 'fatture', status, content: {…} }`.
2. **Copy delle 8 sezioni fondata sulla realtà dell'app** (vedi §3), inquadramento **job-first** + **privacy come firma
   di fiducia**; 3–6 feature con icone Material Symbols, 2–3 step (vincoli #14 25).
3. **Pricing veritiero**: il listino reale ([pricing/fatture.yaml](../../services/core/src/main/resources/pricing/fatture.yaml))
   ha **un solo piano gratuito** (10 fatture/mese, nessun trial, nessun piano a pagamento). La sezione pricing e le FAQ
   **non** riportano i placeholder della fixture ("prova 14 giorni", "Starter/Pro"): sarebbe pubblicità ingannevole.
4. **Registrazione** in [site/src/content/landings/index.ts](../../site/src/content/landings/index.ts).
5. **Transizione `draft → published`** eseguita **solo** dal tool `finalize-landing publish` (mai a mano), dopo il gate
   di approvazione della copy. La sentinella «DA RIFINIRE» del badge hero viene rimossa in fase di rifinitura copy.
6. **Test del sito**: aggiornare [landings.test.ts](../../site/src/content/landings/landings.test.ts) dove assume che
   nessuna landing sia pubblicata (`publishedLandings().length === 0`): con `fatture` pubblicata il registro reale ha
   una landing published, quindi il test va adeguato a riflettere la realtà (mantiene però il controllo del gate su una
   fixture di prova). Il controllo post-build valida la landing pubblicata (5 lingue, Open Graph, `SoftwareApplication`+`FAQPage`).
7. **Collegamento card `#apps` della homepage** alla landing di `Fatture`: a landing pubblicata, la homepage mostra
   finalmente un'app reale collegata (DoD UC 0053 #4). Se il collegamento risulta invasivo sul contenuto marketing
   (UC 0037), si limita al minimo o si traccia; la valutazione è in fase di implementazione.

### 2b. Fix del seed screenshot del tool `finalize-landing/` (difetto UC 0057)
Il comando `screenshots` ([lib/screenshot.mjs](../../tools/finalize-landing/lib/screenshot.mjs)) cabla il seed sulla forma
**generica** dello scaffold (rotta `items`, campi `code/contactName`): per un'app che diverge — come `Fatture`
(`/api/fatture/v1/invoices`, campi `number/customerName`) — la lista risulta **vuota**. È codice consegnato ma
incompleto (lo dice il commento del file stesso: il seed reale era un "raffinamento tracciato" mai chiuso).

**Fix (generico, guidato dai dati per-app, con fallback)**:
- si estrae il seed in un **descrittore per-app** minimale — risorsa di lista (`listPath`), `metric`, `freeCap`, record
  d'esempio; il tool costruisce da questo le rotte mock (lista paginata + quota + entitlement);
- il tool carica `tools/finalize-landing/seeds/<appId>.mjs` **se presente**, altrimenti usa il **default generico**
  attuale (rotta `items`) — così le app scaffoldate da `new-application`, che hanno la forma generica, continuano a
  funzionare senza toccare nulla;
- si aggiunge `tools/finalize-landing/seeds/fatture.mjs` allineato alla prova e2e reale
  ([e2e/fatture.spec.ts](../../frontend/apps/backoffice/e2e/fatture.spec.ts)): rotta `invoices`, record fattura
  realistici (numero, cliente, stato, totale), `metric: fatture`, `freeCap: 10`.
- **Nessuna modifica a `new-application`**: un'app appena scaffoldata combacia col default generico (parità scaffold);
  solo un'app che diverge dallo scaffold aggiunge un seed dedicato. Nota tracciata in `_PARITA-SCAFFOLD.md`.

## 3. La realtà dell'app `Fatture` (base della copy e del seed)
- **Cos'è**: gestione fatture per privati/piccole attività, **mono-utente** per account (B2C). Categoria `cat-blue`, icona `receipt_long`.
- **Funzionalità reali**: creazione fattura con **cliente** (nome, email opzionale) e **righe** (descrizione, quantità,
  importo unitario), totale calcolato dal backend; **lista** (numero progressivo, cliente, data, stato, totale);
  **dettaglio** con cambio **stato** (Bozza → Emessa → Pagata → Annullata) ed eliminazione; numero progressivo immutabile
  per anno; valuta EUR.
- **Quota**: piano **gratuito**, **10 fatture/mese** (finestra mensile che si azzera); nessun trial, nessun piano a pagamento.
- **Privacy/dati**: dati ospitati in **UE**, pieni diritti GDPR self-service (export/cancellazione, UC 0033);
  conservazione conforme agli **obblighi fiscali**. Nessun tracciatore, nessun dato venduto.

## 4. Gate umano (non negoziabile, anche in autopilot)
Pubblicare copy di marketing nelle cinque lingue è un atto **rivolto all'esterno** (#14 dec.35): la copy è **redatta
dall'agente** ma la **pubblicazione attende l'approvazione esplicita dello sviluppatore**. In fase di implementazione,
dopo aver scritto/rifinito la copy, l'agente la presenta nelle cinque lingue e **si ferma**: solo dopo l'approvazione
esegue `publish`. Restano inoltre i gate di `new-change`: consenso al commit e consenso al merge.

## 5. Invarianti appgrove
Non applicabili al perimetro applicativo: contenuto statico del sito + tool di build. La landing entra in `dist/` solo
una volta `published` (gate strutturale). Il fix del tool non tocca dati tenant né infra.

## 6. Requisiti di test
- **vitest `site`**: `landings.test.ts` copre `fatture` (parità 5 lingue, nessuna stringa vuota, forma = EN, 3–6
  feature / 2–3 step, slug valido/non riservato). Adeguato il test del gate (ora esiste una landing published).
- **`astro build` + controllo post-build** (`site/scripts/postbuild-check.mjs`): la landing `fatture` pubblicata ha 5
  lingue, `og:title/description/url`, `SoftwareApplication` + `FAQPage`, hreflang completo.
- **`tooling` (`tools/finalize-landing`)**: nuovo test unitario sulla **risoluzione del seed** — `resolveSeed('fatture')`
  ritorna il descrittore `invoices`; un appId sconosciuto ricade sul default `items`. La cattura Playwright resta un test
  che **salta con grazia** senza browser.
- **Screenshot reali**: catturati eseguendo il tool contro l'anteprima del backoffice; verificati visivamente prima del
  cablaggio (uno screenshot vuoto/rotto è peggio del placeholder — step-03 `finalize-landing`).
- **Gate d'area**: `./run-tests.sh site tooling` verde.

## 7. Definition of Done
1. `site/src/content/landings/fatture/` con copy veritiera nelle 5 lingue (pricing = solo Free 10/mese).
2. Fix del seed screenshot del tool (descrittore per-app + fallback generico) + `seeds/fatture.mjs` + test.
3. Screenshot reali (5 lingue) + immagine Open Graph generati e cablati dal tool.
4. **Copy approvata dallo sviluppatore** → `finalize-landing publish` → `status: published`.
5. `./run-tests.sh site tooling` verde; controllo post-build verde sulla landing pubblicata.
6. Homepage collega un'app reale (o rimando tracciato, §8).
7. `decisions.json` completo e coerente col log; UC 0053 → ✅.

## 8. Rimandi / decisioni differite
- **Seed reale con stack + `seed.sql`**: il fix usa il seed mock (rotte in-browser), sufficiente per uno screenshot
  rappresentativo. Il seed contro il backend reale + Postgres resta un raffinamento non necessario ora → tracciato in UC 0057.
- **Parità scaffold del seed screenshot**: nota in `_PARITA-SCAFFOLD.md` — un'app che diverge dalla forma generica dello
  scaffold deve aggiungere un `seeds/<appId>.mjs`.
- **Deploy**: la CI pubblica al merge (UC 0036/0005, #14 dec.53) — questa change non fa deploy.
