# Change 0060: Job retention — inattività 24 mesi + scadenza audit 12 mesi

**Branch**: `change/0060-use-case-0035-job-retention-inattivita-audit`
**Aree**: `services/core`
**Data**: 2026-07-26
**Autore**: Platform Engineering (modalità autopilot)
**Use case sorgente**: [docs/usecases/08-compliance-gdpr/0035-job-conservazione-purga.md](../../docs/usecases/08-compliance-gdpr/0035-job-conservazione-purga.md)
**Tocca dati personali?**: Sì — colonna di ultimo-accesso a livello account e avviso email per inattività. Classificazione **attesa MINOR** (marcatori tecnici di stato + email già trattata + nessuna nuova finalità), da confermare con lo scanner privacy (UC 0031) allo step-03.

## Problema / Obiettivo

Lo use case UC 0035 (job di retention/purge) è per la maggior parte **già in main**, consegnato da change precedenti:
la grace di eliminazione volontaria a 14 giorni (change 0029, `AccountDeletionSweeper` + `TenantOffboarding.offboard`),
l'archivio audit 12 mesi e la retention log per-categoria in Terraform (UC 0006), il ciclo di vita a 7 giorni degli export
ZIP, gli sweeper di ticket (change 0030) e newsletter. **Restano scoperti** tre lavori applicativi che questa change chiude:

1. **Auto-cancellazione degli account inattivi da 24 mesi** (flusso E.2 dello use case, #13 E26): oggi non esiste — e non
   esiste nemmeno il segnale di "ultimo accesso" su cui misurarla.
2. **Scadenza a 12 mesi delle tabelle di prova GDPR nel database** (`gdpr_purge_audit`, `gdpr_restriction_audit` in schema
   `platform`): la retention 12 mesi è dichiarata (#08) ma **nessun job la applica** (punto aperto tracciato da UC 0034).

L'obiettivo è rendere queste due politiche di conservazione **effettivamente applicate** dai job, seguendo il pattern
sweeper già consolidato nel core, testabili in locale, corretti in cloud.

## Scope

Tutto in **`services/core`** (il servizio piattaforma dove vivono migrazioni, sweeper e schema `platform`).

### A. Segnale di ultimo accesso (substrato per l'inattività)

- Nuove colonne su `platform.accounts`: **`last_active_at`** (istante dell'ultima attività autenticata dell'account) e
  **`inactivity_warned_at`** (istante in cui è stato inviato l'avviso di inattività, `NULL` se nessun avviso pendente).
  Migrazione Flyway che le aggiunge e **inizializza `last_active_at = now()`** per gli account esistenti (orologio fresco di
  24 mesi dal rilascio: nessun avviso/cancellazione retroattiva in massa al primo sweep dopo il deploy).
- `last_active_at` è **timbrato dall'attività autenticata**: su ogni richiesta con JWT valido il tenant (dal claim
  `tenant_id`, invariante #1) vede aggiornato `last_active_at`, con **throttling in memoria per-account** (finestra breve,
  default configurabile ~6h) per evitare una scrittura per richiesta. L'aggiornamento è **best-effort**: non deve mai far
  fallire né rallentare in modo osservabile la richiesta. Le richieste non autenticate non timbrano nulla.

### B. Sweeper auto-cancellazione account inattivi (24 mesi + avviso 30 giorni)

Job schedulato (pattern `AccountDeletionSweeper`: `@Scheduled` orario, `sweep(Instant)` con "adesso" iniettabile, accesso
dati fuori-richiesta con tenant esplicito, `concurrentExecution = SKIP`, idempotente). Due fasi:

- **Fase avviso** — account `active`, non ancora avvisato (`inactivity_warned_at IS NULL`), con `last_active_at` più vecchio
  di **24 mesi** → invia **email di avviso ai proprietari** (ruolo OWNER) e timbra `inactivity_warned_at = now`. L'account
  **resta `active` e pienamente usabile** durante l'avviso: accedere è il modo per "rispondere" ed evitare la cancellazione.
- **Fase cancellazione** — account avvisato da almeno **30 giorni**:
  - se nel frattempo è tornato attivo (`last_active_at` successivo all'avviso) → **recuperato**: azzera `inactivity_warned_at`,
    nessuna cancellazione;
  - altrimenti (nessuna attività dall'avviso) → invoca `TenantOffboarding.offboard(tenantId, "account-inactive-24m")` e
    **soft-cancella** l'account (stesso finale dello sweeper di grace: la rimozione fisica la fa la purge di piattaforma via
    coda).
- Email di avviso: via `Mailer` (Mailpit in `%dev`, SES nel cloud), **fail-soft** come `TicketNotifier` (errore loggato e
  inghiottito, mai blocca il job). Testo **bilingue italiano/inglese**.

### C. Sweeper scadenza tabelle di prova GDPR (12 mesi)

Job schedulato (stesso pattern) che **hard-cancella** dalle tabelle in schema `platform` le righe più vecchie di **12 mesi**:
`gdpr_purge_audit` (per timestamp di esecuzione) e `gdpr_restriction_audit` (per il proprio timestamp). Idempotente; logging
strutturato con i conteggi.

## Fuori scope

- **Trigger EventBridge/cron esterno e publisher EventBridge `tenant.offboarded` dal core.** A cost-min i servizi girano su
  ECS con `desired_count = 1`: lo scheduler `@Scheduled` nel container è il meccanismo corretto e non-duplicato anche in cloud.
  Il trigger esterno serve solo con più task (alta disponibilità) e non è verificabile finché il cloud non è attivo (nessun
  `apply`). → tracciato in [UC 0035 §Punti aperti](../../docs/usecases/08-compliance-gdpr/0035-job-conservazione-purga.md) e
  in [docs/_EVOLUZIONI-DEVOPS.md](../../docs/_EVOLUZIONI-DEVOPS.md).
- **Infrastruttura di retention/archivio** (archivio audit 12 mesi Firehose→S3→Glacier, retention log per-categoria, lifecycle
  7gg export): **già in main** (UC 0006). Non toccata.
- **Retention delle copie per-app di `gdpr_purge_audit`** (schemi `app_<id>`) e **conservazione dell'audit dopo la dismissione
  di un'app** (UC 0048): richiedono di iterare gli schemi delle app e si intrecciano con la dismissione. → restano rimandi
  tracciati in UC 0035.
- **Localizzazione per-lingua dell'email di avviso** (oltre al bilingue IT/EN): materia di localizzazione email (UC 0018/0060).
  → tracciato.
- **Scoping temporale delle finestre della console Diritti GDPR** (punto aperto (c) di UC 0034): non pertinente a questi job.

## Criteri di accettazione

- [ ] Ogni richiesta autenticata aggiorna `last_active_at` dell'account (throttlato: una seconda richiesta entro la finestra
      non riscrive); una richiesta non autenticata non lo tocca; l'aggiornamento non altera l'esito della richiesta.
- [ ] Un account inattivo da ≥24 mesi riceve un solo avviso email ai proprietari e resta usabile; a +30 giorni senza attività
      viene offboardato e soft-cancellato; se torna attivo dopo l'avviso, l'avviso è annullato e non viene cancellato. Tutto
      verificato con "adesso" iniettabile e dati retrodatati, senza attese reali; idempotente e tenant-scoped.
- [ ] Le righe di `platform.gdpr_purge_audit` e `platform.gdpr_restriction_audit` più vecchie di 12 mesi vengono eliminate
      dallo sweeper; quelle recenti restano; l'operazione è idempotente.
- [ ] Suite `mvn test` del core verde; gate privacy (UC 0031) eseguito allo step-03 con classificazione confermata.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT**: il filtro di attività legge il tenant dal claim `tenant_id` verificato (mai da body/param); gli
  sweeper girano fuori-richiesta e usano il `tenant_id` persistito nella riga account (stesso razionale degli sweeper esistenti).
- **Filtro row-level**: le scritture/letture per-account sono per `id = tenant_id`; nessun accesso cross-tenant se non
  l'iterazione di sistema degli sweeper sulle proprie righe candidate.
- **Modulo Terraform `microsaas_app`**: non toccato (nessuna infra nuova; nessun nuovo servizio).
- **Logging strutturato**: ogni evento dei job (`account.inactivity.warned`, `account.inactivity.purged`,
  `account.inactivity.recovered`, `gdpr.audit-retention-sweep`) e del filtro di attività porta `tenant_id`/`user_id` dove
  pertinente.

## Requisiti di test

- **Sweeper inattività** (`sweep(Instant)` iniettabile, `quarkus.scheduler.enabled=false` nei test): avviso a 24 mesi con
  email al proprietario (verifica `MockMailbox`); nessun doppio avviso; cancellazione a +30 giorni (verifica offboarding
  invocato / messaggi di purge in coda + soft-delete); **recupero** (attività dopo l'avviso → avviso azzerato, nessuna
  cancellazione); idempotenza; isolamento tenant.
- **Filtro di attività**: una richiesta autenticata timbra `last_active_at`; una seconda entro la finestra non riscrive
  (throttle); una richiesta senza JWT non timbra.
- **Sweeper audit**: righe retrodatate oltre 12 mesi eliminate, righe recenti intatte, idempotente.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | No (nessuna modifica all'API pubblica/OpenAPI: solo job interni + colonne di stato) |
| Version bump | minor (nuove colonne + nuovi job; nessuna rottura) |
