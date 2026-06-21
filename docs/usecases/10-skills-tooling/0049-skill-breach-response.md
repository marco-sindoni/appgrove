# UC 0049 — skill `breach-response` + runbook/registro/`security.txt`

**Area**: 10-skills-tooling · **Fase**: 6 · **Stato**: 🟢 deciso (skill + deliverable da produrre)
**Dipendenze**: — (si appoggia a #08 detection, #13 J)
**Fonte decisioni**: #13 J (data breach), #08 (detection/audit)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [13-compliance-privacy](../../13-compliance-privacy.md), [08-observability](../../08-observability.md), [_REVISIONE-LEGALE](../../_REVISIONE-LEGALE.md)

## 1. Obiettivo / Scope
Creare la skill **`breach-response`** e i relativi deliverable per la gestione delle violazioni di dati (timeline 72h).
**Incluso**: **runbook** di Incident Response (`docs/compliance/breach-runbook.md`: detect→assess→contain→notify→document);
**registro breach interno** (art. 33.5, **tutte** le violazioni anche non notificate); **`security.txt`** + `security@appgrove.app`
(responsible disclosure); la **skill** co-pilota che, dato l'incidente, **guida la valutazione del rischio** (albero soglie),
decide notifica/non-notifica, **redige la voce del registro** e i **draft delle notifiche** (Garante/interessati/controller B2B, IT/EN).
**Escluso**: la baseline observability/detection (UC 0006), la console diritti (UC 0034), i template legali finali (revisione opzionale L12).

## 2. Attori & ruoli
- **Founder/incident responder**: invoca la skill durante un incidente.
- **Skill** (co-pilota): guida l'albero delle soglie, redige registro + draft notifiche.
- **Garante/interessati/controller B2B**: destinatari delle notifiche (per ruolo A).

## 3. Precondizioni
- Detection via #08 (allarmi/audit/error tracking) + manifesto dati (#13 C) per lo scoping; struttura `docs/compliance/`.

## 4. Flusso principale
1. **Detect**: allarme anomalie/audit/error tracking (#08) o scoperta esterna (security.txt) (#13 J59/63).
2. **Assess** (skill): **albero delle soglie** sul rischio per gli interessati — improbabile→solo registro; rischio→Garante 72h; **elevato**→Garante+interessati (art. 34) (#13 J57).
3. **Leva cifratura (art. 34.3)**: dati cifrati/inintelligibili → spesso niente notifica agli interessati (encryption ovunque #06 §20bis riduce l'obbligo) (#13 J58).
4. **Scoping rapido**: log strutturati/audit (#08) + isolamento per-tenant + manifesto dati → chi/cosa colpito (#13 J59).
5. **Notify per ruolo (A)**: titolare→Garante/interessati; responsabile (dati app B2B)→**notifica il tenant-titolare** senza ritardo (#13 J61).
6. **Document**: voce nel **registro breach** (fatti/effetti/azioni); **draft notifiche** IT/EN; tracciato nel ticketing in-house (#13 J60/62).

## 5. Flussi alternativi / edge / errori
- **Rischio improbabile** → niente notifica, **solo registro** + motivazione (#13 J57).
- **72h** partono da "quando vieni a conoscenza" → runbook pronto in anticipo (#13 J56).
- **Responsible disclosure**: segnalazioni via `security@`/`security.txt` alimentano il processo (#13 J63).
- **Template finali**: revisione legale opzionale (L12) (#13 J62).

## 6. Risorse & runbook
**File**: skill `.claude/skills/breach-response/`; `docs/compliance/breach-runbook.md`; registro breach interno (`docs/`);
`security.txt` sul sito (UC 0037 footer). **Runbook**: incidente → `/breach-response` → albero soglie → registro + draft notifiche → invio per ruolo.

## 7. Dati toccati
Tratta metadati dell'incidente + (eventuali) categorie di dati coinvolti; il **registro breach** è **interno** (come il RoPA).
Le notifiche contengono il minimo necessario. Manifest: trattamento "gestione incidenti/sicurezza" (legittimo interesse/obbligo legale).

## 8. Permessi & gate
- **Invarianti**: lo scoping sfrutta `tenant_id`/audit per identificare l'impatto; nessun accesso ai dati oltre il necessario.
- **Gate**: la skill è co-pilota (assiste fino a bozza solida; validazione = legale, L12). Registro obbligatorio (art. 33.5).

## 9. Requisiti di test
Skill/deliverable: verifica che l'albero delle soglie produca la decisione corretta sui casi tipo; registro e draft generati
completi (IT/EN); `security.txt` pubblicato. (Nessun test runtime di prodotto.)

## 10. Riferimenti & Definition of Done
- **Decisioni**: #13 J56–J64, K, #08 (detection/audit), #06 §20bis (cifratura).
- **DoD**:
  1. Runbook IR (detect→assess→contain→notify→document) + registro breach interno.
  2. Skill `breach-response`: albero soglie + draft registro/notifiche (Garante/interessati/controller B2B, IT/EN).
  3. `security.txt` + `security@` per responsible disclosure.
  4. Leva cifratura applicata; notifiche per ruolo (A).
