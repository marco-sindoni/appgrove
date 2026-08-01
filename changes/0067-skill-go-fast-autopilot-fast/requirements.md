# Change 0067: Skill `go-fast` + modalità `fast` per l'autopilot di `new-change`

**Branch**: `change/0067-skill-go-fast-autopilot-fast`
**Aree**: `.claude/skills/` (nuova skill `go-fast`, revisione `new-change`), `CLAUDE.md` — solo documentazione/skill
**Data**: 2026-08-01
**Autore**: Marco Sindoni (autopilot)
**Use case sorgente**: Nessuno (change ad-hoc, specifica dettata dallo sviluppatore)
**Tocca dati personali?**: No

## Problema / Obiettivo

Implementare più user story in sequenza oggi richiede un giro completo di `new-change` per ciascuna, con i gate
umani a ogni passo. Serve una **corsia veloce controllata**: una skill orchestratrice (`go-fast`) che implementa un
elenco di story (o un'epica intera) una dopo l'altra, appoggiandosi a una nuova modalità **`fast`** di `new-change`
che procede senza fermate, con la sicurezza garantita da: tracciamento completo delle decisioni, suite di test
completa verde, tag di backup remoto prima di ogni story e manuale di test manuali a fine change.

## Scope

### 1. `new-change` — autopilot a tre valori: `off` · `on` · `fast`

- **`off`** (classica) e **`on`** (autopilot): comportamento invariato.
- **`fast`** (nuovo): l'implementazione procede **senza alcuna domanda o fermata di workflow** —
  - niente domanda di modalità (fast si dichiara all'invocazione, tipicamente dal chiamante `go-fast`);
  - `requirements.md` viene comunque scritto e committato, ma **senza gate di revisione**: si passa subito
    all'implementazione;
  - **niente consenso al commit**: a implementazione finita si committa direttamente sul branch della change;
  - **merge e push NON sono compiti di `new-change` fast**: restano al chiamante (`go-fast` nel ciclo; lo
    sviluppatore se fast è usata da sola);
  - ogni decisione è tracciata in `decisions.json` come oggi (prefisso `(autopilot)`; la modalità fast è
    registrata nella decisione 1);
  - **al termine dell'implementazione gira l'intera suite**: `./run-tests.sh` **senza parametri** (tutte le aree,
    non solo quelle toccate) — verifica di non-regressione; il commit avviene **solo a suite verde**; se rossa, la
    skill corregge o si ferma riportando l'esito (mai commit su rosso);
  - una change che tocca superficie frontend **rileva le necessità di test end-to-end Playwright (L2) e li
    implementa** nella stessa change (direzione già scritta in UC 0094, qui anticipata come istruzione operativa
    della skill);
  - **le fermate di escalation restano attive anche in fast** (direzione di prodotto, prezzi/quote,
    classificazioni ambigue su dati personali, effetti irreversibili o verso l'esterno): fast rimuove i gate di
    workflow, non i presidi di sicurezza.

### 2. Nuova skill `go-fast` (`.claude/skills/go-fast/`)

- **Input**: elenco di numeri di user story (es. `0095 0097`) e/o un'epica intera (es. `epica 21`). Se
  l'invocazione non li indica, la skill pone **una domanda preliminare in chat, in testo normale** (niente
  `AskUserQuestion`: l'input è testuale) e attende la risposta.
- **Risoluzione**: dall'epica deriva l'elenco delle story dal catalogo (`docs/usecases/README.md`) e le ordina
  secondo l'**ordine topologico di `EPICS-WAVE-2.md`**; scarta le story già implementate (✅) e **salta quelle
  marcate 🟠** (decisione di prodotto pendente) segnalandole; verifica che le dipendenze delle story richieste
  siano soddisfatte.
- **Ciclo per ogni story**, nell'ordine:
  1. `main` pulito e aggiornato (`git pull`);
  2. calcolo dell'identificativo della change (`NNNN-use-case-YYYY-…`);
  3. **tag di backup remoto PRIMA di avviare la change**: nome `<change-id>-backup`
     (es. `0068-use-case-0095-pagina-app-catalog-backup`), creato sul commit corrente di `main` e **pushato su
     `origin`** — punto di ripristino sicuro della modalità fast;
  4. invocazione di **`new-change` in modalità `fast`** per la story (che chiude con suite completa verde +
     commit sul branch);
  5. scrittura di **`how-to-test.md`** nella cartella della change: elenco dei **test manuali** per verificare la
     story — principalmente visuali (percorsi da navigare, cosa osservare), più i non-visuali necessari —
     committato sul branch **prima** del merge;
  6. **commit + merge su `main` + push**, poi passa **immediatamente** alla story successiva;
- **Fine ciclo**: rapporto riassuntivo (story implementate, change e tag creati, story saltate e perché, rimando
  ai `how-to-test.md`).
- **Guasto a metà ciclo** (suite rossa non recuperabile, errore di implementazione): il ciclo **si ferma** — non
  si passa alla story successiva; branch lasciato in essere per ispezione e rapporto con le istruzioni di
  ripristino dal tag di backup.

### 3. `CLAUDE.md`

Aggiornare la sezione "Modalità autopilot delle skill di change": tre valori (`off`/`on`/`fast`), con la deroga
esplicita — **in `fast` i tre gate umani sono consapevolmente rinunciati dallo sviluppatore all'invocazione**; le
contropartite obbligatorie sono: registro decisioni completo, suite intera verde prima del commit, tag di backup
remoto (nel ciclo `go-fast`), `how-to-test.md` per la verifica manuale a valle. Menzione della skill `go-fast`.

## Fuori scope

- Nessuna modifica al codice eseguibile del monorepo (frontend/servizi/infra).
- Nessuna esecuzione di `go-fast` in questa change (la skill nasce, non viene lanciata).
- Il registro di copertura e2e e l'integrazione piena nel workflow (UC 0093/0094) restano nelle rispettive story.

## Criteri di accettazione

- [ ] `.claude/skills/new-change/` documenta la modalità `fast` con i comportamenti sopra (mode question a tre
      valori, nessun gate di workflow, suite completa, commit senza consenso, merge al chiamante, escalation attive).
- [ ] `.claude/skills/go-fast/SKILL.md` esiste e prescrive: domanda preliminare in chat semplice se manca l'input,
      risoluzione epica→story con ordine dell'onda 2 e salto delle 🟠, tag `<change-id>-backup` remoto prima di
      ogni story, invocazione iterativa di `new-change` fast, `how-to-test.md` per change, commit+merge+push per
      story, arresto al primo guasto con istruzioni di ripristino.
- [ ] `CLAUDE.md` aggiornato coerentemente (tre valori di autopilot + deroga fast + contropartite).
- [ ] `decisions.json` completo e coerente.

## Invarianti appgrove toccati

Nessuno direttamente (solo skill/documentazione). La modalità fast **non indebolisce** gli invarianti
architetturali né il gate privacy/RoPA (UC 0031), che restano parte di ogni change; cambia solo chi preme i
pulsanti dei gate di workflow.

## Requisiti di test (opzionale)

Non applicabile (nessun codice eseguibile). La verifica è la revisione dei documenti prodotti.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A |
| Version bump | nessuno |
