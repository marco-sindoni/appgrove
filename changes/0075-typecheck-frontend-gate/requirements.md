# Change 0075: Il controllo dei tipi TypeScript entra nel cancello di qualità del frontend

**Branch**: `change/0075-typecheck-frontend-gate`
**Aree**: frontend (+ `run-tests.sh` alla radice, `.github/workflows/verify-pr.yml`)
**Data**: 2026-08-01
**Autore**: Platform Engineering
**Use case sorgente**: Nessuno (change ad-hoc, tecnica di qualità)
**Tocca dati personali?**: No

## Problema / Obiettivo

Oggi il controllo dei tipi TypeScript **non fa parte del cancello di qualità** del frontend.
`run-tests.sh`, nella funzione `run_frontend()`, costruisce i pacchetti-libreria e poi esegue `npm test`
(vitest) e `npm run e2e` (Playwright), ma **non esegue mai** `npm run typecheck` — comando che pure esiste
già in `frontend/package.json` (`npm run typecheck --workspaces --if-present`, cioè `tsc --noEmit` in ogni
pacchetto e applicazione).

La costruzione non copre il buco: `vite build` traspila **senza** verificare i tipi, e l'errore viene
soltanto *stampato* dal generatore delle dichiarazioni (vite-plugin-dts) mentre la build esce comunque con
successo. Verificato a mano: build con uscita 0 e artefatto prodotto nonostante l'errore di tipo presente.

Conseguenza osservabile: nel frontend esistono **due errori di tipo reali** che nessuna suite rende rossi.
Eseguendo oggi `npm run typecheck` nella cartella `frontend/` si ottiene uscita 2 con:

1. `packages/design-system/src/components/PageHeader.tsx:5` — `TS2430`: l'interfaccia `PageHeaderProps`
   dichiara `title` come contenuto React (`ReactNode`) ma eredita da `HTMLAttributes<HTMLElement>`, dove
   `title` è l'attributo nativo del suggerimento del browser (`string | undefined`). I due tipi sono
   incompatibili.
2. `apps/backoffice/e2e/privacy.spec.ts:92` — `TS2550`: uso del metodo `.at()` di un array con una
   configurazione del compilatore (`lib`) più vecchia della versione del linguaggio che lo introduce.

Obiettivo: **azzerare i due errori** e poi **agganciare il controllo dei tipi al cancello**, così che da qui
in avanti un errore di tipo reintrodotto faccia diventare rossa la suite invece di restare invisibile.

## Scope

- **Correzione dell'errore su `PageHeader`** (`frontend/packages/design-system`): il tipo del titolo deve
  restare contenuto React (è voluto e ci sono usi con contenuto composto, per esempio nelle storie
  Storybook); va risolto il conflitto con l'attributo nativo omonimo, non declassata la proprietà.
- **Correzione dell'errore su `privacy.spec.ts`** (`frontend/apps/backoffice`): va rimossa la **causa** —
  la configurazione del compilatore ferma a una versione della libreria standard che non conosce quel
  metodo — non il sintomo (riscrivere la riga). La scelta va estesa a tutti i progetti TypeScript del
  frontend solo se ciò non introduce rischio, motivandola.
- **Aggancio del controllo dei tipi all'area `frontend` di `run-tests.sh`**: dopo la costruzione dei
  pacchetti-libreria (le applicazioni risolvono i tipi dei pacchetti dal loro `dist/`) e prima delle altre
  suite. Il fallimento del controllo rende rossa l'area frontend senza interrompere le suite successive,
  coerentemente con la regola "non si ferma al primo errore".
- **Aggiornamento della documentazione interna di `run-tests.sh`** (intestazione con la descrizione delle
  aree) e della riga corrispondente in `CLAUDE.md`, sezione "Esecuzione dei test".
- **Aggancio del controllo dei tipi alla verifica di integrazione continua**
  (`.github/workflows/verify-pr.yml`, lavoro `frontend`): un cancello che esiste solo sulla macchina dello
  sviluppatore non è un cancello.

## Fuori scope

- Qualunque altro difetto incontrato durante il lavoro: si **traccia** come rimando scritto
  (`docs/_BACKLOG.md` o lo use case che lo possiede), non si risolve qui.
- Nessuna modifica al comportamento a video dei componenti: `PageHeader` deve continuare a rendersi
  esattamente come oggi.
- Nessun intervento sul controllo dei tipi delle altre aree del monorepo (sito vetrina `site/`, strumenti
  in `tools/`): questa change riguarda l'area `frontend`.
- Nessuna nuova copertura end-to-end: la change non introduce comportamenti osservabili dall'utente.

## Criteri di accettazione

- [ ] `cd frontend && npm run typecheck` esce con codice 0 (nessun errore in nessuno dei sette progetti:
      i cinque pacchetti condivisi e le due applicazioni).
- [ ] `./run-tests.sh frontend` esegue il controllo dei tipi e lo mostra fra i passi dell'area.
- [ ] Reintroducendo di proposito un errore di tipo in un file qualsiasi del frontend, `./run-tests.sh
      frontend` **fallisce** con codice ≠ 0 e con un messaggio che indica il controllo dei tipi; rimosso
      l'errore, torna verde.
- [ ] Il tipo del titolo di `PageHeader` resta contenuto React e tutti gli usi esistenti (pagine di
      `apps/admin` e `apps/backoffice`, test e storie del pacchetto) continuano a compilare senza modifiche.
- [ ] La verifica di integrazione continua esegue il controllo dei tipi nel lavoro `frontend`.
- [ ] `./run-tests.sh` completa (tutte le aree) è verde.

## Invarianti appgrove toccati

Nessuno degli invarianti architetturali è in gioco: la change non tocca né l'identificativo del cliente
preso dal token verificato, né il filtro per riga sulle interrogazioni, né il modulo Terraform
`microsaas_app`, né il registro strutturato. È interessato invece il presidio non negoziabile
"**Esecuzione dei test**" di `CLAUDE.md`: `run-tests.sh` resta la sorgente di verità unica e va aggiornato
nello stesso commit in cui cambia il comando di test di un'area — è esattamente ciò che questa change fa.

## Requisiti di test

Non esistono test automatici che verifichino `run-tests.sh` stesso, e questa change non ne introduce: la
prova del cancello è la sua stessa esecuzione. La verifica manuale della **rossità** (errore di tipo
introdotto di proposito → suite rossa → errore rimosso → suite verde) è descritta in `how-to-test.md` ed è
il collaudo di accettazione della change. I test già presenti del pacchetto `design-system` per
`PageHeader` devono restare verdi senza modifiche.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No — l'unico effetto per chi sviluppa è che un errore di tipo già esistente diventa visibile |
| Contratto cross-area | N/A |
| Version bump | nessuno |
