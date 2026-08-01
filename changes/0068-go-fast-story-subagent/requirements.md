# Change 0068: go-fast — un sub-agente a contesto vergine per ogni story

**Branch**: `change/0068-go-fast-story-subagent`
**Aree**: skill (`.claude/skills/go-fast/`) — solo documentazione operativa, nessun codice eseguibile
**Data**: 2026-08-01
**Autore**: Platform Engineering
**Use case sorgente**: Nessuno (change ad-hoc)
**Tocca dati personali?**: No

## Problema / Obiettivo

Oggi go-fast implementa tutte le story del lotto **nel contesto della sessione principale**: per ogni story
carica lo use case, la skill `new-change`, legge decine di file, scrive codice ed esegue i test. Dopo poche
story il contesto si riempie e scatta la compattazione automatica (il riassunto del pregresso), con degrado
di qualità proprio a metà del lavoro. Obiettivo: **il contesto principale cresce di poche righe per story**,
delegando l'intera implementazione a un sub-agente a contesto vergine, così che un lotto lungo (anche
un'epica intera) resti eseguibile senza perdita di lucidità.

## Scope

Solo `.claude/skills/go-fast/SKILL.md` (Step 3, gestione guasti e sezioni collegate):

- **Orchestratore snello** (sessione principale): risoluzione e ordinamento della lista (Step 1–2, invariati),
  clean start, calcolo del change id, tag di backup remoto, lancio del sub-agente, verifica post-story,
  merge, push, rapporto finale.
- **Sub-agente per story**: per ogni story l'orchestratore lancia — con lo strumento `Agent`, tipo
  `general-purpose`, esecuzione sincrona — un sub-agente che invoca la skill `new-change` in modalità fast
  per lo use case, scrive e committa `how-to-test.md` sul branch della change, e termina ritornando un
  **rapporto strutturato** a campi fissi (esito successo/guasto/escalation, change id, branch, esito suite,
  numero decisioni, percorso di `how-to-test.md`, rimandi tracciati, dettaglio guasto/escalation).
- **Escalation dal sub-agente**: nei casi di escalation il sub-agente non committa, lascia il branch com'è e
  ritorna esito `escalation` con la domanda; l'orchestratore ferma il ciclo e la riporta allo sviluppatore.
- **Verifica leggera pre-merge** dell'orchestratore: branch esistente, i quattro artefatti presenti
  (`requirements.md`, `implementation-log.md`, `decisions.json`, `how-to-test.md`), albero pulito; la suite
  non viene ri-eseguita (già eseguita dal sub-agente; il tag di backup resta la rete di salvezza).
- **Change id calcolato dall'orchestratore** e passato nel prompt del sub-agente (serve prima, per il tag di
  backup); il sub-agente ne verifica la corrispondenza — discrepanza = guasto che ferma il ciclo.
- Aggiornamento della `description` in testa alla skill perché rifletta il nuovo funzionamento.

## Fuori scope

- La skill `new-change` non cambia in alcun modo (la sua modalità fast è già progettata per essere invocata
  da un chiamante).
- Nessuna modifica a codice eseguibile, test, infrastruttura o documentazione di prodotto.
- L'alternativa "processo `claude` headless per story" (sessione separata via riga di comando): scartata in
  analisi a favore del sub-agente; non viene tracciata come rimando perché la soluzione scelta risolve il
  problema per intero.

## Criteri di accettazione

- [ ] Lo Step 3 di `SKILL.md` prescrive un sub-agente a contesto vergine per ogni story (strumento `Agent`,
      tipo `general-purpose`, sincrono), con prompt che include il change id calcolato dall'orchestratore e
      il contratto del rapporto strutturato.
- [ ] L'orchestratore non implementa più nulla nel contesto principale: solo tag di backup, lancio, verifica
      leggera, merge, push; la gestione guasti copre i tre esiti (successo, guasto, escalation) e ferma il
      ciclo nei due negativi, con le istruzioni di ripristino invariate.
- [ ] La `description` della skill riflette il nuovo funzionamento; i contrappesi fast (suite completa,
      `decisions.json`, tag di backup, `how-to-test.md`) restano tutti prescritti e invariati.

## Invarianti appgrove toccati

Nessuno (change di sola documentazione operativa). I contrappesi della modalità fast — suite completa verde,
registro decisioni, tag di backup remoto, `how-to-test.md`, fermate di escalation — restano prescritti
integralmente: la delega al sub-agente sposta *dove* gira l'implementazione, non *quali presidi* la governano.

## Requisiti di test (opzionale)

Non applicabile: la change tocca solo una skill (Markdown). In modalità fast la suite completa
`./run-tests.sh` viene comunque eseguita prima del commit come prova di non-regressione.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A |
| Version bump | nessuno |
