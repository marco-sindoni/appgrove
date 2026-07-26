# Implementation Log — Change 0060: Job retention — inattività 24 mesi + scadenza audit 12 mesi

**Branch**: `change/0060-use-case-0035-job-retention-inattivita-audit`
**Aree**: `services/core`
**Completata**: 2026-07-26
**Modalità**: autopilot — le risposte alle domande di approfondimento e le scelte di progetto sono
dell'agente e sono tracciate in [decisions.json](decisions.json) (15 voci, 14 in autopilot).

## File modificati

| File | Azione |
|---|---|
| `services/core/.../db/migration/V12__account_activity_retention.sql` | Creato — colonne `last_active_at` (NOT NULL DEFAULT now()) e `inactivity_warned_at` su `platform.accounts` + indice parziale |
| `services/core/.../platform/Account.java` | Modificato — campi `lastActiveAt`/`inactivityWarnedAt` mappati in sola lettura (`insertable/updatable=false`) + getter |
| `services/core/.../platform/AccountActivityTracker.java` | Creato — timbro throttlato (cache in memoria) di `last_active_at`, best-effort; `shouldStamp` puro come seam di test |
| `services/core/.../platform/ActivityStampFilter.java` | Creato — `ContainerResponseFilter` che timbra l'attività su richiesta autenticata (tenant dal JWT) |
| `services/core/.../gdpr/AccountInactivitySweeper.java` | Creato — sweeper inattività 24 mesi: avviso email → +30gg → offboarding, con recupero |
| `services/core/.../gdpr/GdprAuditRetentionSweeper.java` | Creato — scadenza 12 mesi di `gdpr_purge_audit` e `gdpr_restriction_audit` (schema platform) |
| `services/core/src/main/resources/application.properties` | Modificato — `appgrove.activity.stamp-throttle=PT6H` |
| `services/core/src/test/.../TestData.java` | Modificato — helper: retrodatare attività/avviso, leggere i campi, inserire audit retrodatati |
| Test: `AccountActivityTrackerTest`, `gdpr/AccountInactivitySweeperTest`, `gdpr/GdprAuditRetentionSweeperTest` | Creati — vedi sezione Test |
| `docs/usecases/08-compliance-gdpr/0035-*.md`, `docs/_EVOLUZIONI-DEVOPS.md`, `docs/usecases/_INDEX.md` | Modificati — rimandi tracciati + indice 0035 → ✅ |

## Cosa è stato fatto

Consegnati i lavori applicativi **residui e maturi** di UC 0035, dopo aver verificato che l'infrastruttura
di retention/archivio (archivio audit 12 mesi Firehose→S3→Glacier, retention log, lifecycle 7gg export) è
già in main da UC 0006 e la grace 14gg dalla change 0029. Tutto in `services/core`, sul pattern sweeper
`@Scheduled` già consolidato:

1. **Auto-cancellazione account inattivi (24 mesi)**: nuovo segnale `last_active_at` (timbrato
   dall'attività autenticata, con throttle in memoria da 6h, best-effort) e sweeper a due fasi — avviso
   email ai proprietari a 24 mesi di inattività (l'account resta usabile), offboarding a +30 giorni senza
   attività, **recupero** con azzeramento dell'avviso se l'attività riprende.
2. **Scadenza a 12 mesi** delle tabelle di prova GDPR nel database (`gdpr_purge_audit`,
   `gdpr_restriction_audit` in schema platform), che erano dichiarate ma non applicate da alcun job.

## Decisioni prese

Condotta in **autopilot**: tutte le scelte sono in [decisions.json](decisions.json). Le principali:

- **Scope ristretto al residuo** di UC 0035 (dec. 3): infrastruttura e grace già in main; niente reinfra.
- **Pattern cloud = scheduler nel container** (dec. 4): a cost-min ECS `desired_count=1` lo `@Scheduled`
  è corretto e non-duplicato; trigger EventBridge/cron esterno e publisher `tenant.offboarded` dal core
  **rimandati** (servono solo con alta disponibilità E3, non verificabili senza cloud attivo).
- **Segnale a livello account** `last_active_at` (dec. 5), timbrato da un **filtro di risposta** con
  **throttle in memoria** best-effort (dec. 6, 13); `NOT NULL DEFAULT now()` copre backfill e nuove righe,
  campi entità in sola lettura per non far sovrascrivere all'ORM il DEFAULT (dec. 12).
- **Avviso non disattiva** l'account (dec. 7); email ai proprietari via Mailer fail-soft, bilingue (dec. 8);
  backfill a `now()` per evitare avvisi/cancellazioni retroattive in massa (dec. 9).
- **Sweeper audit limitato allo schema platform** (dec. 10, 14); copie per-app e conservazione post-dismissione
  restano rimandi tracciati.
- **Gate privacy (dec. 11, 15)**: classificazione **MINOR, piattaforma core** — vedi sotto.

## Invarianti appgrove

- **tenant_id solo dal JWT**: il filtro di attività legge il tenant dal claim `tenant_id` verificato; gli
  sweeper girano fuori-richiesta e usano il `tenant_id` persistito nella riga account (come gli sweeper esistenti).
- **Filtro row-level**: scritture/letture per-account per `id = tenant_id`; email dei proprietari filtrate
  `where tenant_id = ?`; nessun accesso cross-tenant oltre l'iterazione di sistema degli sweeper.
- **Modulo `microsaas_app`**: non toccato (nessuna infra nuova).
- **Logging strutturato**: `account.inactivity.warned/purged/recovered`, `gdpr.audit-retention.purge`,
  `activity.stamp` portano `tenant_id`.

## Gate privacy (UC 0031)

Scanner: **4 segnali** (colonne `last_active_at`/`inactivity_warned_at` + i due campi entità). Classificati
col co-pilota: sono **marcatori tecnici di stato** — nessun `@PersonalData`, nessuna voce di manifesto/RoPA —
identici per natura a `status`, `deletion_requested_at` (change 0029) e `suspended_reason` (change 0030), che
in `Account.java` non sono annotati. Nessuna nuova finalità (attua la minimizzazione/retention già decisa,
#13 E26), nessuna nuova base giuridica, **nessun nuovo responsabile esterno** (l'avviso riusa l'email utente
già a manifesto e SES già in lista #13 H45). **Classificazione: MINOR, piattaforma core.** Il check CI
`@PersonalData`↔manifesto (in `mvn test`) resta verde: nessuna annotazione, manifesto invariato.

## Note per il revisore

- **Nessun contratto cross-area toccato**: nessuna modifica all'API pubblica/OpenAPI (solo job interni +
  colonne di stato); OpenAPI e `api-client` non rigenerati (non serve).
- **Decisioni differite tracciate** (regola CLAUDE.md): in [UC 0035 §Punti aperti](../../docs/usecases/08-compliance-gdpr/0035-job-conservazione-purga.md)
  — trigger cloud EventBridge/cron degli sweeper + publisher `tenant.offboarded` dal core (rimando ad alta
  disponibilità **E3**, registrato anche in [docs/_EVOLUZIONI-DEVOPS.md](../../docs/_EVOLUZIONI-DEVOPS.md)),
  retention delle copie per-app di `gdpr_purge_audit`, localizzazione per-lingua dell'email di avviso.
- **Gate parità scaffold (UC 0046)**: nessun percorso-sorgente dei modelli toccato (scan exit 0).
- **Promemoria landing stale**: non applicabile (toccato solo `services/core`, nessuna superficie feature/pricing di un'app).
- `run-tests.sh` invariato: nessun modulo aggiunto/rimosso; i test nuovi girano dentro `mvn test`.

## Test

- **backend** (`./run-tests.sh backend` — **verde**, 5 servizi): i 7 test nuovi + nessuna regressione.
  - `AccountInactivitySweeperTest` (3): avviso singolo con account che resta usabile + account fresco non
    toccato + nessun avviso doppio; cancellazione a +30gg con fan-out purge (piattaforma + app) e
    idempotenza; recupero con azzeramento avviso e nessun offboarding.
  - `AccountActivityTrackerTest` (3): throttle puro (`shouldStamp`); `touch` scrive/ri-scrive rispettando
    la finestra; il filtro timbra solo su richiesta autenticata (anonima 401 → nessun timbro).
  - `GdprAuditRetentionSweeperTest` (1): righe oltre 12 mesi eliminate, recenti conservate, idempotente.
- Il check CI `@PersonalData`↔manifesto (`PersonalDataManifestTest` di ogni servizio) è verde.
- Aree non toccate (frontend/infra/compliance): non eseguite (nessuna modifica).

## Stato criteri di accettazione

- [x] Ogni richiesta autenticata aggiorna `last_active_at` (throttlato); una richiesta non autenticata no;
      l'aggiornamento non altera l'esito della richiesta (best-effort).
- [x] Account inattivo ≥24 mesi: un solo avviso ai proprietari, resta usabile; a +30gg senza attività →
      offboardato e soft-cancellato; tornato attivo dopo l'avviso → annullato. "Adesso" iniettabile,
      idempotente, tenant-scoped.
- [x] Righe di `gdpr_purge_audit`/`gdpr_restriction_audit` oltre 12 mesi eliminate, recenti conservate,
      idempotente.
- [x] `mvn test` del core (e intero backend) verde; gate privacy UC 0031 eseguito, classificazione MINOR.
