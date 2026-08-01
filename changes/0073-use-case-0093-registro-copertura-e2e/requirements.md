# Change 0073: Registro di copertura end-to-end + check meccanico

**Branch**: `change/0073-use-case-0093-registro-copertura-e2e`
**Aree**: `docs/testing` (nuovo registro), `tools/e2e-coverage` (nuovo strumento), `tools/platform-e2e` (etichette nei titoli dei test), `frontend/apps/*/e2e` e `frontend/apps/backoffice/e2e-l3` (etichette nei titoli dei test), `run-tests.sh` (area `tooling`)
**Data**: 2026-08-01
**Autore**: Platform Engineering (modalità fast)
**Use case sorgente**: [`docs/usecases/20-test-e2e-piattaforma/0093-e2e-platform-registro-copertura.md`](../../docs/usecases/20-test-e2e-piattaforma/0093-e2e-platform-registro-copertura.md)
**Tocca dati personali?**: No — il registro descrive test e file sorgente, nessun trattamento. Nessun manifesto dati coinvolto.

## Problema / Obiettivo

Oggi la copertura end-to-end esiste (13 percorsi di piattaforma, 13 file di test a browser simulato, 1 di
collaudo in ambiente di prova) ma **nessuno sa dire, guardando un punto solo, quali funzionalità sono coperte
e quali no**. La conoscenza vive nelle teste, nei log di implementazione e nei "Punti aperti" sparsi fra gli
use case: invecchia in silenzio e non è leggibile da un programma.

Obiettivo: dare alla copertura una **memoria strutturata e sorvegliata** —

1. un **registro leggibile da un programma** che mappa *use case con superficie frontend → percorso richiesto
   → test che lo copre*, con i buchi dichiarati anziché dimenticati;
2. un **controllo automatico** che diventa rosso quando registro e realtà divergono, così che la mappa non
   possa invecchiare senza che nulla se ne accorga.

È il presidio che rende sostenibile il processo di UC 0094: un obbligo di processo regge solo se un controllo
lo fa rispettare — stessa filosofia di `decisions.json` e della parità di scaffolding.

## Scope

### 1. Il registro — `docs/testing/copertura-e2e.yaml`

Un solo file YAML, con tre sezioni:

- **`percorsi`** — un elemento per percorso (journey), con:
  - `id` stabile (es. `J-BUY`), `titolo` in italiano, `usecases` (numeri a 4 cifre degli use case coperti);
  - `stato`: `coperto` | `da-coprire` | `escluso`;
  - se `coperto`: `test`, elenco di `{ livello, file }` con `livello` ∈ `platform` | `l2` | `l3` (più voci
    ammesse: lo stesso percorso può essere coperto a livelli diversi);
  - se `da-coprire`: `motivo` e `possiede` (lo use case che sbloccherà la copertura);
  - se `escluso`: `motivo`.
- **`usecases_con_superficie`** — l'elenco **autorevole** degli use case che hanno una superficie applicativa
  interattiva **già presente in `main`**. Sta nel registro (non desunto dalla prosa dei drill-down) perché il
  controllo deve restare deterministico.
- **`esenzioni`** — un elemento per ogni use case del catalogo **non** presente nell'elenco precedente, con
  `categoria` e `motivo`. Categorie ammesse:
  - `senza-superficie` — nessuna superficie applicativa propria (backend, infrastruttura, strumenti, adempimenti
    di business, librerie di stile);
  - `vetrina-statica` — superficie del sito vetrina (Astro, pagine statiche), coperta dai controlli post-build
    dell'area `site` e non da percorsi a browser;
  - `non-implementato` — superficie che **non esiste ancora** in `main` (storie evolutive): esenzione
    **temporanea**, sorvegliata dal controllo (vedi sotto).

La prosa di accompagnamento (formato, regole di manutenzione, come leggere un rosso, cosa il controllo **non**
misura) sta in `docs/testing/README.md`.

### 2. L'aggancio ai test — etichetta nel titolo

Ogni test end-to-end dichiara il percorso che implementa con un'**etichetta in testa al titolo**, fra parentesi
quadre: `test('[J-BUY] catalogo → tier → …', …)`. È il legame che il controllo verifica senza euristiche.
Le etichette vanno applicate a tutti i test end-to-end esistenti: suite di piattaforma
(`tools/platform-e2e/journeys/`), livello 2 (`frontend/apps/*/e2e/`), livello 3
(`frontend/apps/backoffice/e2e-l3/`).

### 3. Il controllo meccanico — `tools/e2e-coverage/`

Strumento Node senza dipendenze pesanti (stesso stampo di `tools/compliance`), eseguito **nell'area `tooling`**
di `run-tests.sh`. Regole verificate, ciascuna con un messaggio che dice *quale* voce sistemare:

1. registro sintatticamente valido e conforme allo schema (campi noti, stati ammessi, identificativi unici,
   numeri di use case a 4 cifre esistenti nel catalogo);
2. ogni voce `coperto` elenca almeno un test; ogni file elencato **esiste** e **contiene** l'etichetta del
   percorso;
3. ogni etichetta trovata nei file di test end-to-end **esiste** nel registro come voce `coperto`, e il file che
   la contiene **è dichiarato** fra i suoi test (vieta la copertura fantasma e la voce orfana);
4. ogni voce `da-coprire` ha `motivo` e `possiede` valorizzati, con `possiede` che punta a uno use case esistente;
5. ogni voce `escluso` ha `motivo`;
6. **completezza del catalogo**: ogni use case di `docs/usecases/` compare **una e una sola volta** o
   nell'elenco `usecases_con_superficie` o nelle `esenzioni` (con `categoria` e `motivo`);
7. ogni use case in `usecases_con_superficie` è referenziato da **almeno una** voce del registro, in qualunque
   stato;
8. **esenzione `non-implementato` scaduta**: se per quello use case esiste già una cartella
   `changes/*-use-case-NNNN-*`, la storia è stata implementata e l'esenzione non vale più → rosso. È la guardia
   che impedisce all'esenzione temporanea di diventare permanente in silenzio.

Uscita con codice ≠ 0 su qualsiasi violazione. Lo strumento ha **test propri su cartelle di prova**
(`node --test`), come `tools/compliance`.

### 4. Popolamento iniziale

Censimento completo della copertura esistente al momento della change: i 13 percorsi di piattaforma
(UC 0090-0092), i percorsi di livello 2 esistenti (backoffice + console admin), quello di livello 3, più i
buchi noti dichiarati `da-coprire` con motivo e proprietario, raccolti dai "Punti aperti" degli use case
0091, 0092, 0033, 0034 e dal backlog.

### 5. `run-tests.sh`

Il nuovo controllo entra nell'area `tooling` **nello stesso commit**, con l'intestazione dello script aggiornata.

## Fuori scope

- **L'aggiornamento del registro a ogni change**: è il processo di UC 0094 (integrazione nelle skill
  `new-usecase`/`new-change`/`new-application`), che questa change abilita ma non anticipa.
- **Nuovi test end-to-end**: nessun percorso nuovo viene scritto qui. I buchi si dichiarano, non si tappano.
- **Copertura dei test unitari o di componente**: il registro riguarda i percorsi end-to-end.
- **Percorsi puramente di servizio (senza browser)**: il formato li consentirebbe, ma la decisione appartiene a
  un use case futuro (punto aperto già scritto in UC 0093).
- **Mostrare lo stato di copertura in `docs/usecases/_INDEX.md`**: differito a quando il processo di UC 0094
  sarà rodato (punto aperto già scritto in UC 0093).
- **Modifiche al comportamento dei test esistenti**: si tocca solo il *titolo*, mai la logica.

## Criteri di accettazione

- [ ] `docs/testing/copertura-e2e.yaml` esiste, elenca tutti i percorsi end-to-end esistenti come `coperto`,
      dichiara i buchi noti come `da-coprire` con `motivo` e `possiede`, e classifica **tutti** i 97 use case
      del catalogo (superficie o esenzione motivata).
- [ ] Ogni test end-to-end esistente (piattaforma, livello 2, livello 3) porta l'etichetta `[ID]` in testa al
      titolo, e ogni etichetta è dichiarata nel registro.
- [ ] `tools/e2e-coverage/` esegue le otto regole sopra, esce con codice ≠ 0 su violazione con messaggio che
      identifica la voce, e ha test propri su cartelle di prova che coprono almeno: registro valido → verde;
      voce `coperto` senza test → rosso; file dichiarato inesistente → rosso; file senza l'etichetta → rosso;
      etichetta nel test assente dal registro → rosso; `da-coprire` senza proprietario → rosso; esenzione senza
      motivo → rosso; use case del catalogo non classificato → rosso; esenzione `non-implementato` scaduta → rosso.
- [ ] `docs/testing/README.md` documenta formato, regole di manutenzione, come leggere un rosso e il limite
      dichiarato (il controllo misura che la mappa sia vera, **non** la qualità dei test).
- [ ] `./run-tests.sh tooling` verde con il nuovo controllo integrato; `./run-tests.sh` (intera) verde.

## Invarianti appgrove toccati

**Nessuno**: la change non introduce superfici di esecuzione, non tocca query, non tocca l'infrastruttura, non
scrive log applicativi. Il registro *rende visibili* i percorsi che verificano gli invarianti (identificativo
di conto dal token verificato, filtro per riga), ma non li altera.

## Requisiti di test

- Test dello strumento su cartelle di prova (`tools/e2e-coverage/test/`), uno per ciascuna delle nove
  condizioni elencate nei criteri di accettazione.
- Nessun test nuovo end-to-end: i test esistenti devono restare verdi con il solo titolo cambiato — la suite
  completa è la prova di non regressione.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A |
| Version bump | nessuno |
