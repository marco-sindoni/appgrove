# Change 0063: skill `breach-response` + runbook, registro breach e responsible disclosure

**Branch**: `change/0063-use-case-0049-skill-breach-response`
**Aree**: docs/compliance, .claude/skills (nessun codice eseguibile: infra/frontend/services non toccati)
**Data**: 2026-07-26
**Autore**: Platform Engineering (autopilot)
**Use case sorgente**: [docs/usecases/10-skills-tooling/0049-skill-breach-response.md](../../docs/usecases/10-skills-tooling/0049-skill-breach-response.md)
**Tocca dati personali?**: **No** (nessun nuovo trattamento né classificazione MAJOR/MINOR). La finalità "gestione incidenti/sicurezza" è già dichiarata nella RoPA dal trattamento `logs.structured` (Sicurezza/stabilità/diagnostica, legittimo interesse art. 6.1.f) e dalla sezione narrativa "Misure di sicurezza". Il registro breach è metadato **interno** (come la RoPA), non un nuovo canale di raccolta di dati personali. Il blocco `security` del manifesto piattaforma viene esteso con un riferimento al processo di Incident Response e la RoPA ri-assemblata (parità IT/EN mantenuta) — non è una nuova base giuridica né un art. 9, quindi nessuna escalation di classificazione.

## Problema / Obiettivo

appgrove ha deciso (#13 J) la postura sulle violazioni di dati personali — timeline 72h, albero delle soglie, notifiche per ruolo — ma i **deliverable operativi non esistono ancora**: manca il runbook di Incident Response, manca il registro breach obbligatorio (art. 33.5) e manca la skill co-pilota che, durante un incidente reale, guidi la valutazione del rischio e produca la documentazione. Senza questi artefatti, al momento di un incidente si parte da zero mentre il termine delle 72 ore corre già — l'esatto contrario di ciò che il runbook deve garantire ("pronto in anticipo").

Obiettivo: rendere appgrove **pronta a rispondere a una violazione** con materiale predisposto — un runbook eseguibile, un registro pronto ad accogliere le voci, canali di segnalazione responsabile attivi, e una skill che assiste il founder/incident-responder nella valutazione e nella redazione. Coerente con lo use case 0049 e le decisioni #13 J56–J64.

## Scope

**1. Runbook di Incident Response** — `docs/compliance/breach-runbook.md` (interno):
   - le cinque fasi **detect → assess → contain → notify → document** con la **timeline 72h** (che parte da "quando vieni a conoscenza");
   - l'**albero delle soglie** (rischio improbabile → solo registro; rischio → Garante 72h; rischio elevato → Garante + interessati art. 34) con i criteri EDPB;
   - la **leva cifratura** (art. 34.3): dati cifrati/inintelligibili spesso escludono la notifica agli interessati;
   - le **notifiche per ruolo** (titolare → Garante/interessati; responsabile app B2B → notifica il tenant-titolare senza ritardo);
   - una **checklist operativa del founder** (canali, casella `security@`, dove trovare log/audit/manifesto per lo scoping).

**2. Registro breach interno** — `docs/compliance/breach-register.md` (interno, come la RoPA):
   - schema delle voci (art. 33.5): identificativo incidente, date (conoscenza/rilevazione), fatti, natura della violazione (riservatezza/integrità/disponibilità), categorie e numero di interessati, categorie di dati, effetti probabili, misure adottate/proposte, **esito della valutazione del rischio** (improbabile/rischio/elevato) con motivazione, **decisione di notifica** (Garante sì/no + data; interessati sì/no + data), riferimento al ticket;
   - registra **tutte** le violazioni, anche quelle non notificate (con la motivazione del "no-rischio");
   - tabella **vuota** all'inizio (nessun incidente).

**3. Skill `breach-response`** — `.claude/skills/breach-response/`:
   - `SKILL.md`: co-pilota a intervista che, dato l'incidente, percorre detect→assess→contain→notify→document, applica l'albero delle soglie, decide notifica/non-notifica, **redige la voce del registro** e i **draft delle notifiche**, con i gate di escalation al founder/legale nei punti sensibili;
   - `reference/albero-soglie.md`: l'albero decisionale con i criteri EDPB e la leva cifratura;
   - `reference/template-registro.md`: template di una voce del registro;
   - `reference/template-notifiche.md`: bozze notifiche **IT/EN** per Garante (art. 33), interessati (art. 34) e controller B2B (ruolo responsabile), marcate "BOZZA — validazione legale L12".

**4. Responsible disclosure** — `security.txt` + `security@appgrove.app`:
   - `security.txt` **esiste già** sul sito ([site/public/.well-known/security.txt](../../site/public/.well-known/security.txt), UC 0037) ed è conforme RFC 9116: verificato, nessuna modifica. Il provisioning della casella `security@` è azione operativa del founder → inserita come voce di prerequisito nel runbook.

**5. Riferimento nella RoPA** — `docs/compliance/manifests/platform.yaml`:
   - il blocco narrativo `security` (IT+EN) viene esteso con un riferimento al processo di Incident Response (runbook + registro, notifica 72h art. 33/34); la RoPA (`ropa.it.md`/`ropa.en.md`) viene **ri-generata** con `npm run assemble`.

**6. Chiusura tracciamenti** — `docs/usecases/_INDEX.md` (0049 → ✅), `docs/_BACKLOG.md` (sezione "Data breach" e voce skill `breach-response` → fatto).

## Fuori scope

- **Baseline observability/detection** (UC 0006): la skill *usa* allarmi/audit/error tracking per lo scoping, non li costruisce.
- **Console diritti GDPR** (UC 0034) e il **link al registro breach** dalla pagina admin: di proprietà della change 0030/UC 0034, già tracciato nei "Punti aperti" dello use case 0049. Non toccato qui.
- **Validazione legale finale** dei template (Garante/interessati/controller): resta il punto **L12** in [docs/_REVISIONE-LEGALE.md](../../docs/_REVISIONE-LEGALE.md), opzionale pre-go-live. Le bozze sono complete ma non validate.
- **Provisioning tecnico della casella `security@`** (record DNS/mailbox): azione operativa del founder, non infra di questa change.
- **Nuovi trattamenti di dati personali** o modifiche alla classificazione: nessuno (vedi campo "Tocca dati personali?").

## Criteri di accettazione

- [ ] Esiste `docs/compliance/breach-runbook.md` con le cinque fasi detect→assess→contain→notify→document, la timeline 72h, l'albero delle soglie, la leva cifratura, le notifiche per ruolo e la checklist founder.
- [ ] Esiste `docs/compliance/breach-register.md` con lo schema completo art. 33.5 (inclusi esito valutazione rischio + motivazione e decisione di notifica) e la tabella vuota.
- [ ] Esiste la skill `.claude/skills/breach-response/SKILL.md` con i tre file `reference/` (albero soglie, template registro, template notifiche IT/EN); i template notifiche coprono Garante/interessati/controller B2B in IT ed EN e sono marcati "BOZZA — validazione legale L12".
- [ ] Percorrendo l'albero delle soglie sui casi tipo si ottiene la decisione corretta: rischio improbabile → solo registro; rischio → Garante 72h + registro; rischio elevato → Garante + interessati + registro; dati cifrati → leva art. 34.3 applicata.
- [ ] `security.txt` è pubblicato e conforme (verificato, invariato); la mailbox `security@` è tracciata come azione founder nel runbook.
- [ ] Il blocco `security` di `platform.yaml` cita il processo di Incident Response e `npm run check` (tools/compliance) è verde dopo `assemble` (parità IT/EN + freshness RoPA).
- [ ] `docs/usecases/_INDEX.md` porta 0049 a ✅ e `docs/_BACKLOG.md` marca fatti runbook/registro/`security.txt` e la skill; L12 resta aperto.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT** / **filtro row-level**: la skill, nella fase di scoping, indica di usare `tenant_id`/audit per delimitare l'impatto per-tenant — coerente con l'isolamento; nessun accesso ai dati oltre il necessario. Non c'è codice che legge il tenant, quindi nulla da far rispettare a runtime.
- **Modulo Terraform `microsaas_app`**: N/A (nessuna infra).
- **Logging strutturato** (`tenant_id`/`app_id`/`user_id`): il runbook si appoggia ai log strutturati/audit (#08) per lo scoping — li usa, non li modifica.

## Requisiti di test

Nessun test di prodotto: la change tocca solo documentazione, skill e narrativa manifesto. Le verifiche dello use case (§9) sono controlli sui deliverable, coperti dai criteri di accettazione sopra (albero → decisione corretta, registro/draft completi IT/EN, `security.txt` pubblicato). Girano comunque, perché si tocca `docs/compliance`:
- `./run-tests.sh compliance` (parità lingue manifesti + freshness RoPA dopo `assemble`; scanner segnali privacy) deve essere verde;
- il gate `npm run privacy-scan` (UC 0031) non deve segnalare nuovi segnali (change documentale).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A |
| Version bump | nessuno (documentazione/skill; nessuna versione applicativa) |
