# Change 0053: skill `campaign-guide`

**Branch**: `change/0053-use-case-0050-skill-campaign-guide`
**Aree**: tooling (`.claude/skills/campaign-guide/`) + documentazione (`docs/`)
**Data**: 2026-07-25
**Autore**: Platform Engineering (autopilot)
**Use case sorgente**: [docs/usecases/10-skills-tooling/0050-skill-campaign-guide.md](../../docs/usecases/10-skills-tooling/0050-skill-campaign-guide.md)
**Tocca dati personali?**: No — la skill è tooling: produce guida, checklist, convenzioni UTM e copy. Non tratta né archivia dati di visitatori/utenti. Anzi, il suo scopo è **impedire** che le campagne introducano tracciamento (niente pixel/CAPI con dati personali sul sito). Manifesto dati: N/A. Nessun gate privacy/RoPA runtime.

## Problema / Obiettivo

Il founder/marketer deve poter creare campagne pubblicitarie su Google e Meta **senza esperienza di advertising** e, soprattutto, **senza violare la postura privacy** del progetto (cookieless, niente pixel Meta/Google, niente CAPI con dati personali, niente banner cookie, fornitori/impostazione EU-purista — #14 J48). Oggi non esiste alcuno strumento che guidi questa attività: il rischio è che, seguendo i default delle piattaforme (che spingono pixel e conversioni server-side con dati personali), si introduca tracciamento che contraddice il pilastro privacy e obbligherebbe a un banner.

Obiettivo: creare la skill **`campaign-guide`**, un co-pilota che guida passo-passo la creazione di una campagna, con una **checklist di conformità a ogni step** che blocca le configurazioni non ammesse, propone gli **obiettivi ammessi** (Traffico / Lead Form native), impone **convenzioni UTM coerenti** perché Plausible possa attribuire i risultati in modo cookieless, e genera **copy/creatività on-brand** (tono F1, tutto AI-generato — dec. 35).

Esito osservabile: invocando `/campaign-guide`, l'utente ottiene un piano di campagna completo e conforme (piattaforma → obiettivo ammesso → esito checklist → stringhe UTM già pronte → varianti di copy on-brand), e qualsiasi tentativo di configurazione non conforme (pixel, CAPI con dati personali, obiettivo di conversione basato su tracciamento del sito) viene **fermato** dalla checklist con la spiegazione del perché.

## Scope

Creazione della skill `.claude/skills/campaign-guide/`:

- **`SKILL.md`** — il flusso guidato (co-pilota a intervista): (1) tipo di campagna — **Google Search primario, Meta in seconda battuta** (#14 J47); (2) **obiettivo ammesso** — Traffico verso il sito oppure **Lead Form native** della piattaforma (lead raccolti sulla piattaforma, zero tracking sul sito) (#14 J48); (3) **checklist di conformità** applicata a ogni step; (4) **convenzioni UTM**; (5) **copy/creatività on-brand**. Include cosa **non** fare (niente pixel, niente CAPI con dati personali) e il trade-off accettato (attribuzione/ottimizzazione più deboli in cambio di coerenza di brand e privacy — #14 J48).
- **`reference/checklist-conformita.md`** — la checklist di conformità privacy come lista verificabile: niente pixel Meta/Google, niente CAPI con dati personali, niente banner, EU-purista, solo obiettivi ammessi. Ogni voce dice *cosa* verificare e *perché* (quale pilastro tutela).
- **`reference/convenzioni-utm.md`** — lo schema UTM concreto e raggruppabile da Plausible (sorgente/mezzo/campagna/contenuto/termine con valori normalizzati minuscoli e stabili), con esempi pronti per Google Search e Meta. È la convenzione **posseduta dalla skill** per il lato campagna.
- **`reference/copy-on-brand.md`** — la guida al copy e alle creatività on-brand (tono F1 "lean/semplice", tutto AI-generato — dec. 35), con i prompt/istruzioni per generare titoli, testi e call-to-action nelle campagne, coerenti con brand e messaggio.

Documentazione da aggiornare nello stesso commit:

- `docs/usecases/_INDEX.md` — UC 0050 → 🟡 all'apertura, → ✅ alla chiusura (regola `new-change`).
- `docs/_BACKLOG.md` — la voce `campaign-guide` passa da "da creare" a "implementata (change 0053)".
- `docs/usecases/10-skills-tooling/0050-skill-campaign-guide.md` — sezione "Punti aperti / decisioni differite" per i rimandi (assistente Playwright, coordinamento goal↔UTM con UC 0039).

## Fuori scope

- **Il lancio operativo della campagna** (creazione reale sulle piattaforme, budget, bid, gestione asset di campagna) → UC 0043 "Lancio paid/social". La skill guida e prepara, non lancia.
- **La newsletter e l'analytics in sé** (subscribe box, consent log, snippet Plausible, definizione dei goal Plausible) → UC 0039. La skill **assume** Plausible attivo e definisce solo la convenzione UTM lato campagna; il coordinamento tra i goal Plausible e questa convenzione è tracciato come rimando in UC 0039.
- **La generazione dei contenuti del sito** (homepage, landing, blog) → UC 0037/0042.
- **L'assistente Playwright non-headless** che pilota la UI di creazione campagna → evoluzione futura, tracciata in UC 0050 (sezione "Punti aperti").
- Nessuna modifica a `infra/`, `frontend/`, `services/<app>/`, né a `run-tests.sh` (nessun nuovo modulo, nessun comando di test cambiato).

## Criteri di accettazione

- [ ] La skill `.claude/skills/campaign-guide/` esiste con `SKILL.md` + i tre file di `reference/` e la sua `description` la rende scopribile/invocabile con `/campaign-guide`.
- [ ] Il flusso guida passo-passo con una **checklist di conformità a ogni step** e **blocca** le configurazioni non conformi (pixel, CAPI con dati personali, obiettivi basati su tracciamento del sito), spiegando il motivo.
- [ ] Sono ammessi solo gli **obiettivi Traffico / Lead Form native**; è documentato lo schema **UTM coerente** raggruppabile da Plausible, con esempi per Google e Meta.
- [ ] Il copy/creatività prodotto è **on-brand** (tono F1) e interamente AI-generato (dec. 35).
- [ ] L'evoluzione futura (assistente Playwright non-headless) e il coordinamento goal↔UTM con UC 0039 sono **tracciati** come rimandi scritti; il backlog è aggiornato a "implementata".
- [ ] `docs/usecases/_INDEX.md` segna UC 0050 come ✅ implementato.

## Invarianti appgrove toccati

Nessuno. La skill è tooling di marketing: non esegue query tenant-scoped, non legge `tenant_id`/`sub`, non istanzia infrastruttura, non produce log applicativi. Gli invarianti multi-tenant, il modulo `microsaas_app` e il logging strutturato non sono in gioco. L'unico "gate" è funzionale: la checklist di conformità privacy della skill.

## Requisiti di test (opzionale)

Nessun test automatico. La skill è composta da Markdown/istruzioni (nessun codice eseguibile): come da regola `new-change`, i test automatici non si applicano. La verifica è di contenuto — che la guida ammetta solo configurazioni conformi (obiettivi ammessi, UTM coerenti, niente pixel/CAPI con dati personali) e che il copy sia on-brand — e viene svolta a occhio in fase di chiusura, senza suite. `run-tests.sh` non cambia.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A (nessun contratto frontend↔API o servizio↔infra; solo tooling) |
| Version bump | nessuno (tooling/documentazione, nessun artefatto versionato di prodotto) |
