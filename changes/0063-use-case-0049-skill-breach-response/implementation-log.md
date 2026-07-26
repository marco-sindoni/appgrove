# Implementation Log — Change 0063: skill `breach-response` + runbook, registro breach e responsible disclosure

**Branch**: `change/0063-use-case-0049-skill-breach-response`
**Aree**: docs/compliance, .claude/skills (nessun codice eseguibile)
**Completata**: 2026-07-26
**Modalità**: autopilot — le risposte alle domande di approfondimento sono dell'agente e sono tracciate in [decisions.json](decisions.json) (11 decisioni, 10 in autopilot).

## File modificati

| File | Azione |
|---|---|
| `docs/compliance/breach-runbook.md` | Creato — runbook IR (detect→assess→contain→notify→document, 72h, albero soglie, leva cifratura, notifiche per ruolo, checklist founder) |
| `docs/compliance/breach-register.md` | Creato — registro breach interno (art. 33.5): schema completo + tabella vuota |
| `.claude/skills/breach-response/SKILL.md` | Creato — skill co-pilota a intervista |
| `.claude/skills/breach-response/reference/albero-soglie.md` | Creato — albero decisionale + criteri EDPB + leva cifratura + casi tipo |
| `.claude/skills/breach-response/reference/template-registro.md` | Creato — template voce del registro |
| `.claude/skills/breach-response/reference/template-notifiche.md` | Creato — bozze notifiche IT/EN (Garante/interessati/controller B2B), marcate L12 |
| `docs/compliance/manifests/platform.yaml` | Modificato — nuovo blocco narrativo `incident_response` (IT+EN) |
| `docs/compliance/ropa.it.md` / `ropa.en.md` | Ri-generati da `npm run assemble` |
| `docs/_BACKLOG.md` | Modificato — sezione "Data breach" e voce skill `breach-response` marcate ✅ fatto |
| `docs/usecases/_INDEX.md` | Modificato — UC 0049 → 🟡 (apertura) → ✅ (chiusura) |
| `changes/0063-*/requirements.md`, `decisions.json` | Creati — artefatti di change |

## Cosa è stato fatto

Prodotti i deliverable operativi della postura data breach decisa in #13 J: un **runbook di Incident
Response** interno (le cinque fasi con la timeline 72h, l'albero delle soglie, la leva cifratura art. 34.3 e le
notifiche per ruolo), il **registro breach** interno obbligatorio (art. 33.5, con lo schema completo e la
tabella vuota) e la skill co-pilota **`breach-response`** che, durante un incidente, guida la valutazione del
rischio e redige la voce di registro + le bozze di notifica IT/EN per i tre destinatari. Il processo è stato
reso **scopribile dagli artefatti di compliance** aggiungendo un blocco narrativo `incident_response` al
manifesto piattaforma e ri-assemblando la RoPA. `security.txt` + `security@appgrove.app` (responsible
disclosure) erano già pubblicati (UC 0037): verificati, invariati.

## Decisioni prese

Change condotta in **autopilot**; registro completo e strutturato in [decisions.json](decisions.json). In sintesi:

- **Struttura** (dec. 4/5): runbook e registro come documenti "sempre-veri" in `docs/compliance/`, skill come
  parte "attivata all'incidente" in `.claude/skills/`; skill puro co-pilota senza tool deterministico (il
  rischio è giudizio umano assistito, non un calcolo).
- **Dati personali** (dec. 6): nessun nuovo trattamento — la finalità sicurezza/incidenti è già nella RoPA
  (`logs.structured`, art. 6.1.f); il registro breach è metadato interno come la RoPA. Classificazione non
  ambigua → decisa in autopilot, senza escalation.
- **RoPA** (dec. 7/11): esteso il manifesto piattaforma con un **blocco narrativo dedicato** `incident_response`
  (più leggibile che gonfiare il blocco `security`); RoPA ri-generata, parità/freshness verde.
- **Template notifiche** (dec. 8): bozze complete IT/EN marcate "BOZZA — validazione legale **L12**"; L12 resta
  aperto in `_REVISIONE-LEGALE`.
- **security.txt / mailbox** (dec. 9): file già pubblicato; provisioning della casella `security@` = azione
  operativa del founder, tracciata nel runbook.

## Invarianti appgrove

Nessun invariante toccato a runtime (change documentale). Il runbook e la skill **rafforzano** gli invarianti
esistenti citandoli come strumenti di scoping: `tenant_id` dal JWT e filtro row-level delimitano per
costruzione il raggio di una violazione per-tenant; i log strutturati (`tenant_id`/`app_id`/`user_id`, #08)
sono la fonte per ricostruire l'impatto. Nessuna infra, quindi modulo `microsaas_app` N/A.

## Note per il revisore

- **Nessuna decisione differita nuova**: il link al registro breach dalla console admin "Diritti GDPR" (UC 0034)
  resta di proprietà della change 0030/UC 0034 ed è **già** annotato nei "Punti aperti" dello use case 0049
  (dec. 10). Non toccato qui.
- **Azione operativa del founder** (non infra di questa change): provisioning/monitoraggio della casella
  `security@appgrove.app` — voce di prerequisito nel runbook (§0).
- **Punto aperto legale**: le bozze di notifica non sono validate legalmente → **L12** in
  [docs/_REVISIONE-LEGALE.md](../../docs/_REVISIONE-LEGALE.md), pre-go-live opzionale.
- Nessun impatto su contratti cross-area (nessun codice frontend/service/infra).
- Nessuna landing pubblicata resa stale (change non tocca feature/pricing di app).

## Test

**Non applicabile — nessun codice eseguibile modificato** (solo documentazione, skill e narrativa manifesto).
Verifiche eseguite comunque perché si tocca `docs/compliance`:

- `./run-tests.sh compliance` → **verde** (30/30 test; parità lingue manifesti + freshness RoPA dopo `assemble`;
  scanner segnali privacy). I warning `entity.yaml DA COMPILARE` sono pre-esistenti e attesi pre-go-live (UC 0001).
- **Gate privacy (UC 0031)**: `npm run privacy-scan` → exit 0, **nessun segnale** (change documentale).
- **Gate parità scaffold (UC 0046)**: `source-paths-scan.mjs` → exit 0, nessun percorso-sorgente toccato.

Le verifiche dello use case (§9) — albero soglie → decisione corretta sui casi tipo, registro/draft completi
IT/EN, `security.txt` pubblicato — sono coperte dai deliverable (casi tipo in `reference/albero-soglie.md`,
template IT/EN, `security.txt` verificato).

## Stato criteri di accettazione

- [x] `docs/compliance/breach-runbook.md` con cinque fasi, timeline 72h, albero soglie, leva cifratura, notifiche per ruolo, checklist founder.
- [x] `docs/compliance/breach-register.md` con schema art. 33.5 completo (esito+motivazione, decisione notifica) e tabella vuota.
- [x] Skill `.claude/skills/breach-response/SKILL.md` + 3 file `reference/`; template notifiche coprono Garante/interessati/controller B2B in IT ed EN, marcati "BOZZA — validazione legale L12".
- [x] Albero delle soglie dà la decisione corretta sui casi tipo (improbabile/rischio/elevato + leva cifratura).
- [x] `security.txt` pubblicato e conforme (verificato, invariato); mailbox `security@` tracciata come azione founder nel runbook.
- [x] Blocco `incident_response` nel manifesto piattaforma e `npm run check` verde dopo `assemble`.
- [x] `_INDEX.md` porta 0049 a ✅; `_BACKLOG.md` marca fatti runbook/registro/`security.txt`/skill; L12 resta aperto.
