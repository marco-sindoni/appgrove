# Implementation Log — Change 0068: go-fast — un sub-agente a contesto vergine per ogni story

**Branch**: `change/0068-go-fast-story-subagent`
**Aree**: skill (`.claude/skills/go-fast/`) — solo documentazione operativa
**Completata**: 2026-08-01
**Modalità**: fast — nessun gate di workflow; le risposte alle domande di approfondimento sono
dell'agente e sono tracciate in [decisions.json](decisions.json)

## File modificati

| File | Azione |
|---|---|
| .claude/skills/go-fast/SKILL.md | Modificato |
| changes/0068-go-fast-story-subagent/requirements.md | Creato |
| changes/0068-go-fast-story-subagent/decisions.json | Creato |
| changes/0068-go-fast-story-subagent/implementation-log.md | Creato |

## Cosa è stato fatto

Lo Step 3 di go-fast non implementa più le story nel contesto della sessione principale: per ogni story
l'orchestratore lancia un **sub-agente a contesto vergine** (strumento `Agent`, tipo `general-purpose`,
esecuzione sincrona) che invoca `new-change` in modalità fast, scrive e committa `how-to-test.md` sul branch
della change e termina ritornando un **rapporto strutturato** a campi fissi (`esito`, `change_id`, `branch`,
`esito_suite`, `decisioni_registrate`, `how_to_test`, `rimandi`, `dettaglio`). L'orchestratore resta snello —
lista, clean start, change id, tag di backup, verifica leggera pre-merge, merge, push, rapporto finale — così
il suo contesto cresce di poche righe per story e un lotto lungo non incontra più la compattazione a metà
lavoro. La gestione guasti ora distingue tre esiti: `successo` (merge e story successiva), `guasto` (stop con
istruzioni di ripristino, invariate) ed `escalation` (stop, domanda riportata testualmente allo sviluppatore;
la ripresa è una nuova invocazione di go-fast a punto risolto). Aggiornata anche la `description` in testa
alla skill.

## Decisioni prese

Tutte in autopilot, registro completo in [decisions.json](decisions.json) (8 voci, 6 dell'agente). Le
principali: sub-agente `general-purpose` sincrono per story (dec. 3); ripartizione dei compiti con
`how-to-test.md` a carico del sub-agente, che ha il contesto vivo dell'implementazione (dec. 4); contratto
del rapporto strutturato come unico contenuto che rientra nel contesto dell'orchestratore (dec. 5);
escalation = niente commit, branch lasciato com'è, esito dedicato che ferma il ciclo (dec. 6); verifica
pre-merge leggera senza ri-esecuzione della suite, col tag di backup come rete di salvezza (dec. 7); change
id calcolato dall'orchestratore e passato al sub-agente, discrepanza = guasto (dec. 8).

## Invarianti appgrove

Nessuno toccato (change di sola documentazione operativa). I contrappesi della modalità fast — suite
completa verde, registro decisioni, tag di backup remoto, `how-to-test.md`, fermate di escalation — restano
prescritti integralmente: cambia *dove* gira l'implementazione, non *quali presidi* la governano.

## Note per il revisore

- La skill `new-change` non è stata toccata: la sua modalità fast era già progettata per un chiamante.
- Il commit di `how-to-test.md` passa dall'orchestratore al sub-agente; lo Step 6 del ciclo ora fa solo
  merge, push e pulizia del branch.
- Un rapporto assente o nullo dal sub-agente (agente morto o saltato) è trattato come `guasto`.
- Nessuna decisione differita: l'alternativa scartata (processo `claude` separato per story) non è tracciata
  come rimando perché la soluzione scelta risolve il problema per intero (motivazione in requirements.md).
- Gate privacy: nessun segnale (`npm run privacy-scan` exit 0). Gate parità scaffold: nessun
  percorso-sorgente toccato (exit 0). Nessuna landing pubblicata interessata (la change non tocca
  feature/pricing di alcuna app).

## Test

Non applicabile — nessun codice eseguibile modificato (solo skill Markdown). In modalità fast la suite
completa `./run-tests.sh` senza parametri è stata comunque eseguita prima del commit come prova di
non-regressione: **tutte e 7 le aree verdi** (backend, frontend, infra, compliance, tooling, smoke, site).

Nota: una prima esecuzione era rossa in backend e tooling per **cause ambientali**, non per la change — un
processo Quarkus dev di `fatture` rimasto acceso occupava la porta 8081 (4 test "Failed to start quarkus")
e il demone Docker della macchina virtuale colima era morto durante la corsa. Rimedio: `./app-stop.sh`,
riavvio di colima (dati preservati), ri-esecuzione integrale → verde. Nessuna modifica a codice o test.

## Stato criteri di accettazione

- [x] Lo Step 3 prescrive un sub-agente a contesto vergine per ogni story (strumento `Agent`, tipo
      `general-purpose`, sincrono), con change id calcolato dall'orchestratore nel prompt e contratto del
      rapporto strutturato.
- [x] L'orchestratore non implementa più nulla nel contesto principale; la gestione guasti copre i tre esiti
      e ferma il ciclo nei due negativi, con istruzioni di ripristino invariate.
- [x] La `description` della skill riflette il nuovo funzionamento; i contrappesi fast restano tutti
      prescritti e invariati.
