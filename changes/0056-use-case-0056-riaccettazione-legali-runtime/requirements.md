# Change 0056: Ri-accettazione ToU/Privacy a runtime (derivazione al login + schermata bloccante + log accettazioni)

**Branch**: `change/0056-use-case-0056-riaccettazione-legali-runtime` (git worktree dedicato)
**Aree**: `services/core` (Quarkus: migrazione, entità, endpoint, comando `sync-legal`) · `frontend/apps/backoffice` (gate bloccante + rendering in-app) · `frontend/packages/api-client` (rigenerazione tipi) · `.github/workflows` + `infra/scripts` (popolamento versioni al deploy) · `docs/compliance` (manifesto + RoPA)
**Data**: 2026-07-26
**Autore**: Platform Engineering (modalità autopilot)
**Use case sorgente**: [`docs/usecases/04-platform-core/0056-riaccettazione-legali-runtime.md`](../../docs/usecases/04-platform-core/0056-riaccettazione-legali-runtime.md)
**Tocca dati personali?**: **Sì** — si introduce il **log delle accettazioni legali** (utente + componente + versione + data + commit): dato personale minimo, finalità **accountability/contratto**. Va classificato dal gate privacy/RoPA di step-03 (voce entity-backed nel manifesto `platform.yaml` + RoPA rigenerata + `@PersonalData` sui campi). Classificazione preliminare **MINOR** (nuovo trattamento di accountability strettamente accessorio al contratto, non un nuovo uso dei dati): **da validare in revisione legale** ([docs/_REVISIONE-LEGALE.md](../../docs/_REVISIONE-LEGALE.md)). Base giuridica, retention e categoria: vedi §"Punti per la tua attenzione".

## Problema / Obiettivo

Oggi non esiste alcun meccanismo di **accettazione runtime** dei documenti legali: al signup l'utente non registra l'accettazione dei Termini, e quando un documento legale cambia in modo sostanziale (nuova versione **major**) non c'è modo di richiedere la ri-accettazione. Manca anche la **prova** (log) di chi ha accettato cosa e quando — necessaria per l'accountability (GDPR art. 5.2) e come base contrattuale.

**Obiettivo**: implementare il meccanismo **derivato** di accettazione/ri-accettazione previsto da UC 0056:
- al **login/refresh** lo shell interroga il core, che **confronta** la versione accettata (log) con la major corrente (tabella `legal_version`, popolata dalla CI al deploy dei legali) e restituisce i componenti **da ri-accettare**;
- se ce ne sono, lo shell mostra una **schermata bloccante** prima dell'ingresso; l'utente accetta/prende atto → l'accesso si sblocca;
- l'accettazione iniziale è registrata al **signup**; i **diritti GDPR** (export/erasure/recesso) restano **esenti** dal blocco.

**Osservabile a fine change**: con una major dei Termini più recente dell'ultima accettata, al login l'utente vede la schermata bloccante coi documenti aggiornati (testo reso in-app, token `{{titolare.*}}` risolti) e non entra finché non accetta; l'accettazione è registrata (log tenant/utente-scoped) e alla ri-apertura non ricompare.

## Scope

### A. Backend — `services/core` (schema `platform`)
1. **Migrazione `V11__legal_versions_and_acceptances.sql`**:
   - `platform.legal_version` — dato **non personale**, platform-level: `component`, `major` (intero, prima cifra del semver), `version` (semver pieno), `effective_date`. Fonte di verità delle versioni correnti; **disaccoppia il core da `content/legal/`**.
   - `platform.legal_acceptance` — **tenant/utente-scoped**, append-only: `tenant_id`, `user_id` (= `cognito_sub` locale), `component`, `version`, `major`, `accepted_at`, `commit_hash`, tipo di atto (`accept` per i Termini / `acknowledge` per Privacy·Cookie). Colonne di audit standard (`BaseTenantEntity`).
2. **Entità JPA + repository Panache**: `LegalVersion` (platform-level, `BaseEntity`), `LegalAcceptance` (`BaseTenantEntity`, campi `@PersonalData` dove pertinente). Voce **entity-backed** nel manifesto (obbligatoria per `PersonalDataManifestTest`).
3. **Comando `sync-legal`** in `CoreMain.java` (come `migrate`/`sync-pricing`): legge il **frontmatter** dei componenti legali (lingua IT facente fede) e fa **upsert** in `platform.legal_version` (component/major/version/effective_date). Più uno **startup-sync locale** (analogo a `PricingSyncStartup`, attivabile via config) per l'ambiente locale/test. I `content/legal/*.md` sono **bundlati nella classpath** del core (`maven-resources-plugin`, come `email-templates`).
4. **Endpoint** (resource `@Path("/api/platform/v1/me")`, `@Authenticated`, qualunque ruolo; tenant/user da `CallerContext`):
   - `GET /me/legal-status` → per ogni **componente vincolante** confronta la major accettata (max dal log) con quella corrente (`legal_version`): se inferiore o mancante → componente **pendente** (blocco su major); include i cambi **minor** come **notifica** non bloccante. Ritorna, per componente pendente, versione corrente + tipo di atto richiesto.
   - `POST /me/legal-acceptance` → registra l'accettazione/presa d'atto per uno o più componenti (atomica), idempotente per `(user, component, version)`.
5. **Endpoint rendering testo**: `GET /api/platform/v1/legal/{component}?lang=xx` → ritorna `{ component, version, effectiveDate, markdown }` con i **token `{{titolare.*}}` risolti** da `content/legal/entity.yaml` (stessa logica del sito, replicata lato core; markdown già risolto, il rendering markdown→HTML è del frontend). Aperto agli autenticati.
6. **Componenti vincolanti**: **`terms`** (accettazione esplicita), **`privacy`** e **`cookie`** (presa d'atto). `refund` è **dentro i Termini** (non componente bloccante separato). `subprocessors` **non blocca** (→ notifica/preavviso, differito, vedi Fuori scope). Componenti **per-app**: non applicabili ora (non esistono documenti legali per-app) → differiti.
7. **GDPR**: il log accettazioni entra in **export** (art. 15) ed **erasure** (fisica) in `PlatformDataContract`.

### B. Frontend — `frontend/apps/backoffice`
8. **Gate bloccante** post-login: un `LegalAcceptanceGate` dentro/subito dopo `SessionGate` (prima del router) che, a sessione `authenticated`, chiama `GET /me/legal-status`; se ci sono componenti pendenti rende una **schermata full-screen** (pattern `FullPageMessage`) con l'elenco dei documenti aggiornati, il **testo reso in-app** (token risolti), e l'azione **Accetto / Ho preso atto** → `POST /me/legal-acceptance` → sblocco. Blocca sia login sia refresh (il gate rimonta al bootstrap).
9. **Rendering markdown in-app**: nuova dipendenza frontend minimale (es. `react-markdown`) per rendere il markdown restituito dal core. I **diritti GDPR** (pagina "I miei dati"/privacy) restano raggiungibili anche con accettazione pendente.
10. **Accettazione iniziale al signup**: il wizard di registrazione registra l'accettazione dei **Termini** e la **presa d'atto** di Privacy·Cookie (checkbox esplicita per i Termini) via `POST /me/legal-acceptance` a sessione creata.
11. **Notifica MINOR** (non bloccante): un avviso in-app leggero (banner) quando `legal-status` segnala cambi **minor** dall'ultima accettazione, con link al testo. Nessun blocco.
12. **Hooks + tipi**: rigenerare `@appgrove/api-client` (`npm run gen`) dopo gli endpoint; aggiungere `useLegalStatus()`/`useAcceptLegal()`/`useLegalDoc()`; copy a chiave i18n nelle 5 lingue (il pacchetto i18n è già a 5 lingue, UC 0060).

### C. CI / deploy
13. Aggiungere uno step **`oneshot <env> platform <tag> sync-legal`** in `deploy-test.yml` e `release-prod.yml` **dopo** il `migrate` (popola `legal_version` al deploy dei legali). Nessun deploy del sito è coinvolto.

### D. Compliance
14. Voce di trattamento **"gestione accettazioni legali"** in `docs/compliance/manifests/platform.yaml` (it+en, entity-backed sul log) + **RoPA rigenerata** (`npm run assemble`). Classificazione registrata qui e nel log di implementazione (gate UC 0031/0030).

## Fuori scope (differito, tracciato in UC 0056 "Punti aperti")
- **Canale di notifica esterno + preavviso 30 giorni sub-processor con finestra di opposizione** (#13 C49): la notifica MINOR in-app è inclusa (punto 11), ma il **preavviso email 30gg per nuovi sub-processor** e la finestra di opposizione richiedono il canale email/notifiche e una macchina a stati dedicata → **differito** (dipende dal canale notifiche, territorio newsletter/email UC 0039). Tracciato.
- **Componenti legali per-app** e accettazione all'**attivazione app**: non esistono documenti legali per-app → differito finché non esistono.
- **Deploy del sito vetrina**: fuori scope (buco noto, non introdotto qui).
- **Redazione/versioning dei testi legali** (UC 0002) e **classificazione major/minor di un singolo cambio** (gate `new-change`, UC 0031): già di altri use case.
- **Consent center marketing/cookie** (UC 0033/0039): trattamento distinto.

## Criteri di accettazione
- [ ] Migrazione `V11` applicata: `platform.legal_version` (platform-level) e `platform.legal_acceptance` (tenant/utente-scoped, append-only) esistenti.
- [ ] `sync-legal` (comando + startup-sync locale) popola `legal_version` dai frontmatter dei componenti; major = prima cifra del semver.
- [ ] `GET /me/legal-status` deriva i componenti pendenti (accettata < major corrente → blocco; minor → notifica) leggendo tenant/user **solo dal JWT**; `POST /me/legal-acceptance` registra idempotente e tenant/utente-scoped.
- [ ] `GET /legal/{component}?lang` rende il testo con token `{{titolare.*}}` risolti (nessun token residuo).
- [ ] Al login con una major pendente lo shell mostra la **schermata bloccante** coi documenti aggiornati e non entra finché non si accetta; alla ri-apertura non ricompare. I diritti GDPR restano accessibili col blocco pendente.
- [ ] Signup registra l'accettazione dei Termini + presa d'atto Privacy·Cookie.
- [ ] Log accettazioni in export ed erasure GDPR; voce manifesto entity-backed + RoPA allineata (`mvn test` e `compliance` verdi).
- [ ] Suite verdi: backend (integration Testcontainers + multi-tenancy + contract GDPR + comando), frontend (unit + e2e del gate bloccante), compliance.

## Invarianti appgrove toccati
- **Tenant ID solo dal JWT**: gli endpoint leggono tenant/user da `CallerContext` (claim `tenant_id`/`sub`), mai da body/param; il `POST` di accettazione ignora ogni tenant nel corpo.
- **Filtro row-level**: `legal_acceptance` è `BaseTenantEntity` → `WHERE tenant_id` automatico; test multi-tenancy dedicato (A non vede/accetta per B).
- **Logging strutturato**: i log del core portano `tenant_id`/`user_id` (e `app_id` dove pertinente).
- **Modulo Terraform `microsaas_app`**: non pertinente (nessuna nuova app; solo core).

## Requisiti di test
- **Integration (Testcontainers)**: "accettata < major → pendente/bloccante"; minor → solo notifica; log scritto e **idempotente**; `sync-legal` popola/aggiorna `legal_version` (test in stile `MigrateCommandTest`).
- **Security/multi-tenancy**: A non vede né accetta per conto di B; anti-override `tenant_id` dal body.
- **GDPR contract**: il log accettazioni compare in export ed è cancellato in erasure (`PlatformGdprContractTest`).
- **Rendering token**: nessun `{{...}}` residuo nel testo servito.
- **E2E (Playwright)**: login con major pendente → schermata bloccante → accetta → accesso; i diritti GDPR raggiungibili col blocco pendente.

## Punti per la tua attenzione (decisioni che richiedono la tua validazione — legali)
Queste le ho decise in autopilot con l'opzione raccomandata, ma **hanno peso legale**: valutale in rilettura (e le porto in [docs/_REVISIONE-LEGALE.md](../../docs/_REVISIONE-LEGALE.md)).
1. **Blocco su presa d'atto** (Privacy·Cookie): la specifica prevede la schermata bloccante anche per la *presa d'atto* (non solo per l'accettazione dei Termini). La informativa è un atto unilaterale: bloccare l'accesso su suo aggiornamento è una scelta di prodotto/legale. **Raccomandato**: seguire la specifica (blocco su major dei componenti vincolanti terms/privacy/cookie), distinguendo l'azione ("Accetto" per i Termini, "Ho preso atto" per Privacy·Cookie).
2. **Base giuridica del log accettazioni**: **contratto** (art. 6.1.b) per i Termini + **accountability** (art. 5.2) come obbligo di rendicontazione. **Raccomandato** così; da confermare in revisione legale.
3. **Retention del log accettazioni**: **per la vita dell'account + periodo di prescrizione** applicabile (mantenere la prova dell'accettazione oltre il grace di 14 giorni degli altri dati, perché è la prova del consenso contrattuale). **Da validare** con il legale il periodo esatto.
4. **Classificazione MINOR** del nuovo trattamento (accountability accessorio al contratto, non nuovo uso dei dati) → notifica, non ri-accettazione della Privacy stessa. Da confermare in revisione legale.

## Valutazione di impatto
| Area | Impatto |
|---|---|
| Breaking change | No per l'utente. Nuovi endpoint (additivi) sul core; nuova UI bloccante. Contratto OpenAPI esteso (additivo; `oasdiff` in CI non deve segnalare breaking). |
| Contratto cross-area | Sì — frontend ↔ API core (nuovi endpoint `/me/legal-status`, `/me/legal-acceptance`, `/legal/{component}`); core ↔ CI (comando `sync-legal` invocato al deploy). |
| Version bump | minor (nuova capacità additiva). |
| Dati personali | Sì — nuovo log accettazioni (accountability); manifesto + RoPA aggiornati; classificazione MINOR (da validare legalmente). |
