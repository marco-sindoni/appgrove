# Change 0043: skill `drop-application` — dismissione sicura di un'app

**Branch**: `change/0043-use-case-0048-skill-drop-application`
**Aree**: `tools/drop-application` (nuovo, Node — area `tooling`), `.claude/skills/drop-application` (nuova skill), `services/core` (nuova primitiva `offboard-app`), documentazione (`docs/usecases`, `docs/_PARITA-SCAFFOLD.md`, `run-tests.sh`)
**Data**: 2026-07-24
**Autore**: Platform Engineering (modalità autopilot)
**Use case sorgente**: [docs/usecases/10-skills-tooling/0048-skill-drop-application.md](../../docs/usecases/10-skills-tooling/0048-skill-drop-application.md)
**Tocca dati personali?**: Sì (in modo indiretto e strutturale) — la change introduce lo strumento con cui si **cancellano** i dati personali di un'app dismessa (contratto `purgeData` per tutti i tenant, con audit). Non introduce né tratta nuovi dati personali propri, non crea nuovi `@PersonalData` e non aggiunge trattamenti al registro RoPA: quindi non fa scattare un bump di versione di Informativa privacy/Condizioni d'uso. Il gate privacy/RoPA di step-03 viene comunque eseguito; l'esito atteso è "nessun nuovo segnale".

## Problema / Obiettivo

Oggi si sa creare un'app in modo industrializzato (skill `new-application`, UC 0046) ma **non si sa dismetterla**: ritirare un'app dal marketplace significherebbe, a mano, rimuovere decine di file, disfare cinque modifiche puntuali a file condivisi, ricordarsi dell'infrastruttura, dei prezzi, del registro dei trattamenti, degli abbonati e dei dati personali — un'operazione lunga, irripetibile e, soprattutto, **rischiosa** proprio dove sbagliare è più grave (dati personali non cancellati, risorse cloud lasciate accese, abbonati lasciati senza servizio senza preavviso).

Questa change costruisce **`drop-application`**, l'inverso esatto di `new-application`: una skill che dismette un'app in modo **sicuro** (nessun atto irreversibile eseguito dalla skill; tutto reversibile finché non si distrugge deliberatamente), **conforme** (dati cancellati con audit dopo che l'export è stato garantito; registro dei trattamenti aggiornato) e **rispettoso degli abbonati** (nessuna distruzione finché il loro trattamento non è deciso). Il risultato osservabile: un solo comando produce un branch + una richiesta di merge che disfa tutto ciò che `new-application` aveva costruito, più un runbook che elenca — con le relative protezioni — gli unici passi che restano deliberatamente manuali.

## Scope

La change è costruita **a due metà**, come il gemello `new-application` (decisione 3):

### A. De-generatore deterministico — `tools/drop-application/`
Strumento Node collaudato (area `tooling` di `run-tests.sh`), simmetrico a `tools/new-application/generate.mjs`. Con `--dry-run` mostra il piano prima di toccare qualcosa. Disfa **solo la parte meccanica**:

1. **Rimuove i file creati** dal generatore per quell'app: `services/<app_id>/` (intero servizio), `frontend/apps/backoffice/src/modules/<app_id>/`, il test end-to-end `frontend/apps/backoffice/e2e/<app_id>.spec.ts`, il manifesto `docs/compliance/manifests/<app_id>.yaml`, il listino `services/core/src/main/resources/pricing/<app_id>.yaml`.
2. **Disfa le cinque modifiche ai file condivisi** invertendo `tools/new-application/lib/edits.mjs` — e derivando da lì l'elenco, non da una copia parallela (decisione 8): il modulo Maven in `services/pom.xml`, l'import + la voce in `frontend/apps/backoffice/src/registry/registry.ts`, lo script `gen:<app_id>` in `frontend/apps/backoffice/package.json`, la voce in `services/core/.../pricing/index.yaml`, il blocco code in `dev/elasticmq.conf`.
3. **Delega l'infrastruttura** a `infra/scripts/service-remove <app_id>` (già esistente): toglie il blocco `module "app_<app_id>"` dai due ambienti.
4. **Rigenera la RoPA** (`tools/compliance` → `assemble`) dopo aver rimosso il manifesto, così che il registro dei trattamenti non resti in *drift* (il `check` di freschezza andrebbe altrimenti rosso).

Come il generatore: **non sovrascrive nulla alla cieca**, verifica che l'app esista prima di agire, e **non tocca** i file auto-scoperti (`app-start.sh`, `dev/Caddyfile`, `dev/lib/*.sh`, `tools/smoke/*`, workflow): spariscono da soli quando il servizio non c'è più.

### B. Skill co-pilota — `.claude/skills/drop-application/`
Orchestra la dismissione **dentro `new-change`** (branch + test + commit/merge consent) e possiede le decisioni che uno strumento non può prendere:

1. **Gate abbonati (escalation al developer, decisione 7).** Verifica le subscription attive dell'app (`status in ('active','trialing','past_due')`), presenta il quadro e **propone** il trattamento (disdetta a fine periodo / migrazione ad altra app-tier / sola comunicazione), ma **è il developer a decidere**: è denaro ed effetto verso l'esterno. Riusa i meccanismi esistenti (`PaymentProvider.cancelSubscription`, `changeSubscriptionTier`). Nessuna distruzione è proposta finché gli abbonati non sono gestiti (#09 H35).
2. **Archiviazione price (decisione 5).** Mette il listino a `status: inactive` e toglie l'app da `pricing/index.yaml`. L'archiviazione effettiva (soft-delete `deleted_at`) la esegue `PricingSyncService.sync()` al merge/tag, con la guardia di grandfathering esistente. La skill **non contatta il fornitore di pagamento** (placeholder, #14).
3. **Pianificazione della purga dati con audit.** Predispone — **senza eseguirla** — la cancellazione dei dati dell'app per **tutti** i tenant, garantendo prima la possibilità di export (diritti, #13 D) e l'audit (#13 L70). L'esecuzione avviene con la nuova primitiva `offboard-app` (parte C) come passo deliberato del runbook.
4. **Runbook degli atti irreversibili.** Elenca, con le protezioni, gli unici passi che restano manuali dopo il merge: `terraform destroy -target` (safety #06 K), esecuzione di `offboard-app`, pulizia fisica del DB (`DROP SCHEMA app_<id> CASCADE`, `DROP ROLE`, rimozione del segreto in Secrets Manager — già documentata in `service-remove --help`).

### C. Primitiva di purga app-wide — `services/core` (decisione 6)
Nuovo comando one-shot del core `offboard-app <app_id>` (dispatch in `CoreMain`, accanto a `sync-pricing`/`migrate`), servito da una classe `AppOffboarding` **simmetrica a `TenantOffboarding`** ma a livello di app: enumera i tenant con dati nell'app e, per ciascuno, accoda un `TenantPurgeMessage` sulla coda `tenant-purge-<app_id>`, riusando integralmente `TenantPurgeConsumer` (che già cancella + scrive l'audit + purga la proiezione entitlement). È la primitiva senza cui la skill non può soddisfare il proprio DoD; nessun altro use case la possiede.

## Fuori scope

- **Esecuzione** di qualunque atto distruttivo o esterno da parte della skill (destroy infra, lancio della purga, `DROP SCHEMA`, chiamate al Paddle reale) → restano passi manuali/CI post-merge, nel runbook (decisione 4).
- **`new-application`** (UC 0046) e **`pricing-change`** (UC 0047): non si toccano.
- **Il framework purge/erasure in sé** (UC 0032): la change lo **riusa**, non lo riscrive.
- **Rimozione della landing per-app**: la vetrina/landing **non esiste ancora** (UC 0036/0038/0053 non implementati) → oggi non c'è nulla da rimuovere. Tracciato come punto aperto in UC 0048; la skill lo gestirà solo quando la landing esisterà.
- **Conservazione dell'audit di erasure oltre il `DROP SCHEMA`**: l'audit dell'app vive nello schema `app_<id>` che verrà droppato → la sua conservazione a lungo termine appartiene a retention/archivio (UC 0035). Tracciato lì.

## Criteri di accettazione

- [ ] `tools/drop-application`, dato un `app_id` esistente, produce (con `--dry-run` e poi reale) la rimozione di tutti i file creati per quell'app e il disfacimento delle cinque modifiche condivise; delega a `service-remove`; rigenera la RoPA senza *drift*. Rifiuta un `app_id` inesistente o riservato (`platform`) senza toccare nulla.
- [ ] Dopo il de-generatore su un'app generata da zero, lo stato del repository è indistinguibile da "app mai creata" per i file/modifiche coperti: le suite `tooling`, `backend`, `frontend`, `compliance` restano verdi e `./dev.sh services` non elenca più l'app.
- [ ] Il comando `offboard-app <app_id>` enumera i tenant dell'app e accoda una purga per ciascuno sulla coda `tenant-purge-<app_id>`; con un test che verifica l'enumerazione e il fan-out (nessun tenant dell'app dimenticato, nessuna app diversa toccata), riusando l'audit esistente.
- [ ] La skill `.claude/skills/drop-application/` documenta il flusso completo con i gate umani (gestione abbonati come escalation; consenso a commit e merge) e un runbook che isola gli atti irreversibili con le rispettive safety.
- [ ] `service-remove`/destroy mirato non impatta altri servizi (verifica `terraform validate`/plan sugli env dopo la rimozione).
- [ ] `run-tests.sh` e la sua documentazione includono il nuovo strumento; `docs/usecases/_INDEX.md` porta UC 0048 a ✅.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT**: la primitiva `offboard-app` gira **fuori** da una richiesta utente (comando one-shot, nessun JWT) → opera su `tenant_id` **esplicito** letto via JDBC, esattamente come `TenantOffboarding`; il filtro per tenant resta su ogni cancellazione (lo fa `purgeData` di ciascuna app tramite `GdprScope`). Mai da body/params.
- **Filtro row-level** `WHERE tenant_id = :tid`: preservato — la purga è per-tenant anche quando è orchestrata app-wide (fan-out di un messaggio per tenant).
- **Modulo Terraform `microsaas_app`**: la dismissione infra passa **solo** per `service-remove` (che rimuove l'istanza del modulo), mai per infra bespoke.
- **Logging strutturato** (`tenant_id`, `app_id`, `user_id`): `AppOffboarding` logga con `app_id` + i tenant coinvolti, come fa `TenantOffboarding`.

## Requisiti di test

- **`tooling`**: test del de-generatore su un'app generata al volo (genera → de-genera → il repository torna allo stato iniziale per i percorsi coperti); rifiuto di `app_id` inesistente/riservato; idempotenza/sicurezza (non rimuove file non suoi). Parità: il de-generatore resta allineato alle stesse fonti di `new-application`.
- **`backend` (core)**: unit test di `AppOffboarding` — enumerazione dei tenant dell'app e fan-out sulla coda giusta, incluse subscription soft-deleted; nessun messaggio verso app diverse; riuso dell'audit.
- Le suite **`frontend`** e **`compliance`** devono restare verdi dopo il de-generatore (registro frontend coerente, RoPA senza *drift*).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (aggiunge strumenti e un comando; non altera contratti esistenti) |
| Contratto cross-area | N/A per i consumatori runtime; la skill/strumento coordina più aree in un'unica change |
| Version bump | minor (nuova capacità: nuovo strumento + nuovo comando core); nessun bump di Informativa/Condizioni (nessun nuovo trattamento) |
