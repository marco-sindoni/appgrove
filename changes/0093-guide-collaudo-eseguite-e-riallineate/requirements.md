# Change 0093: Le guide di collaudo manuale si eseguono, non si scrivono soltanto

**Branch**: `change/0093-guide-collaudo-eseguite-e-riallineate`
**Aree**: `.claude/skills/` (skill `new-change` e `go-fast`) · `docs/` (backlog)
**Data**: 2026-08-22
**Autore**: Platform Engineering (modalità autopilot)
**Use case sorgente**: Nessuno (change ad-hoc, origine: [docs/_BACKLOG.md](../../docs/_BACKLOG.md) — voce
«le guide di collaudo manuale (`how-to-test.md`) invecchiano in silenzio», sollevata il 2026-08-21)
**Tocca dati personali?**: No — la change modifica solo istruzioni di processo per le skill.

## Problema / Obiettivo

Cinque guide di collaudo manuale collaudate in tre giorni (lotto `go-fast` 0088–0092, epica 22) hanno
richiesto **circa quaranta correzioni**. La diagnosi iniziale — «le guide invecchiano» — spiega solo due delle
sei categorie di difetto raccolte nel backlog. Le altre quattro erano **sbagliate il giorno in cui sono state
scritte**: comandi che non partono, etichette che non esistono a schermo, quattro convenzioni diverse per lo
stesso comando nella stessa guida, prerequisiti non dichiarati.

La causa comune è una sola: **nessuno esegue la guida prima di committarla**. È prosa plausibile, scritta da
chi conosceva il codice ma non ha incollato un comando in un terminale. La prova più eloquente è del
2026-08-21: l'ultima riga del §9 della guida 0091 nascondeva un errore `500` del prodotto, mai visto da
nessuno perché nessuno l'aveva mai eseguita.

Il costo non è la fatica di correggere: è che **una guida scaduta si legge come «i test non ci sono»**, e
fa perdere fiducia nell'automazione proprio quando questa ha funzionato. È accaduto, e ha richiesto
un'indagine per dimostrare il contrario.

**Obiettivo**: che una guida di collaudo, il giorno in cui viene committata, sia **eseguibile per costruzione**
nella sua parte non visiva; e che l'invecchiamento, dentro un lotto di più change consecutive, si manifesti
come **fallimento** invece che come sorpresa per chi collauda.

## Scope

### 1. `new-change` — la guida diventa un artefatto della modalità fast, e si esegue

Oggi `how-to-test.md` è nominata **solo** dalla skill `go-fast`, mentre CLAUDE.md la attribuisce alla
**modalità fast** fra le contropartite non riducibili. È un buco già in essere: `new-change fast` invocata da
sola non produce alcuna guida. Si sana:

- la guida entra fra gli artefatti che la **modalità fast** produce, con la sua struttura obbligatoria;
- **prima del gate del commit**, la change **esegue** i passi della guida il cui esito è osservabile senza
  guardare uno schermo, e la committa solo dopo;
- ogni passo che fallisce va **discriminato**, non aggirato: se la guida è sbagliata si corregge la guida; se
  è sbagliato il prodotto è un difetto, e si corregge o si traccia secondo le regole già in vigore. Questa
  distinzione è il valore dell'intero presidio;
- se lo stack locale non è disponibile, la mancata esecuzione va **dichiarata** nella guida e nel registro
  delle decisioni: la responsabilità passa alla passata di lotto o allo sviluppatore. Una guida non eseguita
  che finge di esserlo è peggio di una dichiaratamente non eseguita.

**Forma obbligatoria della guida** (le tre categorie che nascono sbagliate e si chiudono per costruzione):

- **intestazione** con il commit su cui la guida è stata scritta e la data, così chi la esegue sa di leggere
  una fotografia e non un documento vivo;
- **comandi completi e incollabili**, senza scorciatoie da definire prima (funzioni di shell, alias,
  variabili non valorizzate nel testo). Per la banca dati locale, una **forma canonica dichiarata**;
- **etichette come si leggono a schermo**, nella lingua dell'interfaccia di chi collauda, non nomi tecnici né
  nomi di simboli.

### 2. `go-fast` — passata di fine lotto: le guide del lotto si rieseguono

Alla fine del lotto, con lo stack acceso **una volta sola**, `go-fast` **riesegue** i passi non visivi di
**tutte** le guide del lotto contro lo **stato finale di `main`** — che è lo stato in cui lo sviluppatore le
userà davvero. Non una rilettura: un'esecuzione.

- ogni fallimento è **triagiato**: guida superata da una change successiva del lotto (l'invecchiamento) ·
  guida sbagliata in origine · difetto del prodotto;
- una guida **modificata** vede i suoi passi non visivi **rieseguiti**, se la modifica li ha impattati: una
  correzione mai eseguita è una correzione non verificata;
- le **guardie di perimetro** («se lo vedi adesso, qualcuno ha anticipato lavoro») sono il caso più insidioso:
  dopo il lotto scattano a vuoto e accusano di un difetto il lavoro corretto della change successiva. La
  passata le converte nella verità finale;
- l'esito entra nel resoconto finale del lotto, e le guide corrette si committano.

### 3. `docs/_BACKLOG.md`

La voce di processo si chiude con le decisioni prese, e resta aperto il solo punto escluso di proposito.

## Fuori scope

- **Un controllo meccanico** (uno strumento in `tools/`) che imponga la forma canonica su **tutte** le
  `how-to-test.md` del repository. Applicato a guide di change già chiuse produrrebbe rumore permanente e
  renderebbe rossa la suite per documenti d'archivio che nessuno riscriverà. Tracciato in
  [docs/_BACKLOG.md](../../docs/_BACKLOG.md) con il motivo dell'esclusione.
- **La riscrittura retroattiva** delle guide già committate (0083–0092). Quelle del lotto dell'epica 22 sono
  già state corrette a mano durante il collaudo; le altre restano fotografie storiche, ed è la nuova
  intestazione a dirlo.
- **La modalità classica e autopilot di `new-change`**: non producono la guida (non è una loro contropartita)
  e restano invariate. Se domani si volesse la guida sempre, è una decisione a sé.
- **Le guide di collaudo delle skill diverse da `new-change`/`go-fast`** e qualunque altro artefatto di
  processo.

## Criteri di accettazione

- [ ] `new-change` in modalità fast produce `how-to-test.md` **e** esegue i suoi passi non visivi prima del
      gate del commit; l'esito dell'esecuzione (compresa una mancata esecuzione, con il motivo) è registrato
      in `decisions.json`.
- [ ] La struttura obbligatoria della guida è dichiarata nella skill: intestazione col commit, comandi
      completi e incollabili, forma canonica per la banca dati locale, etichette come si leggono a schermo.
- [ ] `go-fast` esegue, alla fine del lotto, i passi non visivi di **tutte** le guide del lotto contro lo
      stato finale di `main`, triagia ogni fallimento nelle tre categorie e riesegue dopo ogni correzione che
      impatti quei passi; l'esito entra nel resoconto finale.
- [ ] La voce del backlog sulle guide riporta le decisioni prese e il solo punto rimasto aperto.
- [ ] `./run-tests.sh tooling` verde (l'area che sorveglia gli strumenti di processo).

## Invarianti appgrove toccati

Nessuno degli invarianti architetturali (`tenant_id` dal JWT, filtro row-level, modulo Terraform,
logging strutturato): la change non tocca codice eseguibile né infrastruttura.

Restano invece toccate — e vanno mantenute vere — due regole non negoziabili della costituzione:
la **lingua italiana** degli artefatti (le guide sono per lo sviluppatore) e il **tracciamento delle
decisioni differite** (il punto escluso va scritto, non lasciato in conversazione).

## Requisiti di test

Nessun test automatico: la change tocca solo istruzioni di processo in Markdown. Va però verificato
che `./run-tests.sh tooling` resti verde, perché è l'area che sorveglia gli strumenti di processo, e va
dichiarato nel log che i test non sono applicabili al resto, con il motivo.

La verifica sostanziale è di **coerenza**: le due skill non devono contraddirsi né contraddire CLAUDE.md
sulla titolarità della guida e dei tre presidi.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | No |
| Version bump | nessuno |
