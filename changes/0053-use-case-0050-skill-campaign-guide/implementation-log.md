# Implementation Log — Change 0053: skill `campaign-guide`

**Branch**: `change/0053-use-case-0050-skill-campaign-guide`
**Aree**: tooling (`.claude/skills/campaign-guide/`) + documentazione (`docs/`)
**Completata**: 2026-07-25
**Modalità**: autopilot — le risposte alle domande di approfondimento sono dell'agente e sono tracciate in [decisions.json](decisions.json) (9 decisioni su 11 marcate `(autopilot)`)

## File modificati

| File | Azione |
|---|---|
| `.claude/skills/campaign-guide/SKILL.md` | Creato |
| `.claude/skills/campaign-guide/reference/checklist-conformita.md` | Creato |
| `.claude/skills/campaign-guide/reference/convenzioni-utm.md` | Creato |
| `.claude/skills/campaign-guide/reference/copy-on-brand.md` | Creato |
| `changes/0053-use-case-0050-skill-campaign-guide/requirements.md` | Creato (step-02) |
| `changes/0053-use-case-0050-skill-campaign-guide/decisions.json` | Creato/aggiornato |
| `docs/usecases/_INDEX.md` | Modificato (UC 0050 → 🟡 → ✅) |
| `docs/_BACKLOG.md` | Modificato (voce `campaign-guide` → implementata) |
| `docs/usecases/10-skills-tooling/0050-skill-campaign-guide.md` | Modificato (sezione "Punti aperti") |
| `docs/usecases/09-marketing-site/0039-newsletter-consenso-plausible.md` | Modificato (rimando goal↔UTM) |

## Cosa è stato fatto

Creata la skill `campaign-guide`: un co-pilota a intervista che guida la creazione di campagne su Google e Meta
rispettando la postura privacy cookieless difesa (#14 J48). Il `SKILL.md` orchestra il flusso (piattaforma →
obiettivo ammesso → checklist di conformità a ogni step → convenzioni UTM → copy on-brand); tre file in
`reference/` forniscono i contenuti stabili — la checklist di conformità (che **blocca** pixel, API di
conversione server-to-server con dati personali, banner, obiettivi basati su tracciamento), lo schema UTM
raggruppabile da Plausible, e la guida al copy/creatività on-brand (tono F1, tutto generato con l'intelligenza
artificiale). Aggiornata la documentazione di catalogo e i rimandi delle decisioni differite.

## Decisioni prese

Change condotta in **autopilot**: tutte le scelte di scope e struttura sono dell'agente, secondo l'opzione
raccomandata, e sono in [decisions.json](decisions.json). Le principali: ramificazione da `main` per isolare la
change dal branch parallelo 0052/UC 0039 (id 3); struttura leggera `SKILL.md` + `reference/` invece degli step
numerati delle skill procedurali pesanti (id 4); convenzione UTM posseduta dalla skill con coordinamento goal
tracciato in UC 0039 (id 5); output in chat, gestione asset/lancio operativo lasciata a UC 0043 (id 6); nessun
test automatico perché la change è solo Markdown/skill (id 7); nessun dato personale trattato (id 8).

## Invarianti appgrove

Nessuno toccato. La skill è tooling di marketing: non esegue query tenant-scoped, non legge `tenant_id`/`sub`,
non istanzia infrastruttura, non produce log applicativi. Gli invarianti multi-tenant, il modulo `microsaas_app`
e il logging strutturato non sono in gioco.

## Note per il revisore

- **Punto di contatto su file condiviso con la sessione parallela**: la change modifica
  `docs/usecases/09-marketing-site/0039-newsletter-consenso-plausible.md` con **una riga** in coda alla sezione
  "Punti aperti" (rimando goal↔UTM). Lo stesso file è in lavorazione sul branch `change/0052` (UC 0039): al merge
  il conflitto, se ci sarà, è un'append banale a fine lista. È l'**unico** punto di sovrapposizione su file
  condivisi (decisione id 11). Tutto il resto (la skill, le altre modifiche a docs) è esclusivo di questa change.
- **Decisioni differite tracciate**: (a) assistente Playwright non-headless → UC 0050 "Punti aperti" +
  `docs/_BACKLOG.md` + memoria `skills-backlog`; (b) coordinamento goal Plausible ↔ UTM → rimandi gemelli in
  UC 0050 e UC 0039. Nessun'altra decisione differita.
- **Gate parità scaffold (UC 0046)**: nessun percorso-sorgente dei modelli toccato (scanner exit 0).
- **Promemoria landing stale (UC 0057)**: non applicabile — la change non tocca `services/<app>`, moduli
  backoffice di app, né listini pricing; nessuna landing pubblicata coinvolta.
- **Contratti cross-area**: nessuno (nessun confine frontend↔API o servizio↔infra).

## Test

Non applicabile — nessun codice eseguibile modificato (solo skill Markdown + documentazione). Come da regola
`new-change`, i test automatici non si applicano; `run-tests.sh` non cambia (nessun modulo aggiunto, nessun
comando di test modificato). La verifica è di contenuto: la guida ammette solo configurazioni conformi
(obiettivi Traffico/Lead Form, UTM coerenti, niente pixel/API-di-conversione con dati personali) e copy on-brand.

**Gate privacy (UC 0031)**: `npm run privacy-scan` → nessun segnale (la change non introduce campi dati,
migrazioni, dipendenze o manifesti).

## Stato criteri di accettazione

- [x] La skill `.claude/skills/campaign-guide/` esiste con `SKILL.md` + i tre file di `reference/` e la sua `description` la rende invocabile con `/campaign-guide`.
- [x] Il flusso guida passo-passo con checklist di conformità a ogni step e **blocca** le configurazioni non conformi, spiegando il motivo.
- [x] Ammessi solo gli obiettivi Traffico / Lead Form native; documentato lo schema UTM coerente raggruppabile da Plausible, con esempi Google e Meta.
- [x] Il copy/creatività prodotto è on-brand (tono F1) e interamente generato con l'intelligenza artificiale (dec. 35).
- [x] Evoluzione futura (assistente Playwright) e coordinamento goal↔UTM tracciati come rimandi scritti; backlog aggiornato a "implementata".
- [x] `docs/usecases/_INDEX.md` segna UC 0050 come ✅ implementato.
