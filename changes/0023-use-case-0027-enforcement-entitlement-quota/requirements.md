# Change 0023: Enforcement entitlement + quota (read-model `/me/entitlements`, gate 402, cap reale flow/stock)

**Branch**: `change/0023-use-case-0027-enforcement-entitlement-quota`
**Aree**: `services/core`, `services/commons`, `services/fatture`, `frontend`
**Data**: 2026-06-29
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/07-payments/0027-applicazione-entitlement-quota.md](../../docs/usecases/07-payments/0027-applicazione-entitlement-quota.md)
**Tocca dati personali?**: No — legge `subscription`/catalogo (dati di account/billing, già esistenti, tenant-scoped); nessun nuovo campo personale, nessuna nuova finalità.

## Problema / Obiettivo

Rendere reale l'**enforcement runtime** della catena di gate lato servizio (i gradini fini complementari all'authorizer edge UC 0014, cloud-deferred) e la **risoluzione del tetto di quota dall'entitlement**. Oggi: il gate **ruolo (403)** c'è (`@RolesAllowed`); la SPI quota + 429 esistono ma il tetto è **config-driven** (`ConfigQuotaLimitSource`, cap 10 fisso); **mancano** il gate **entitlement (402)**, la risoluzione del cap da `subscription → app_tier.limits`, la natura **stock**, l'endpoint **`/me/entitlements`** e il **cablaggio reale del registry FE** (oggi `entitled` è hardcoded in locale). Questa change colma tutto ciò che la nota "Punti aperti" di UC 0027 assegna a questo UC.

## Scope

**`services/core`** — read-model entitlement di piattaforma:
- `GET /api/platform/v1/me/entitlements` (autenticato): per il tenant del JWT ritorna le **app entitled** con, per ciascuna, `appSlug`, `tierKey`, `phase` (da `SubscriptionLifecycle`, null per baseline free), `accessUntil`, e `limits` per metrica `{ cap, nature, window }` (da `app_tier.limits`).
- Derivazione (riusa i building block esistenti, non li ri-deriva): `access = app.status==active && (subscription.grantsAccess() ‖ baseline free tier)`. **Free tier come baseline** (decisione A): se non esiste `subscription` per `(tenant, app)`, l'entitlement effettivo è il **tier senza prezzo** (`app_price` assente) → `access=true`, cap dal free tier; se l'app non ha free tier → non entitled. Una subscription a pagamento sovrascrive la baseline.
- **Riconciliazione slug↔UUID**: `subscription.app_id` (UUID) → `app.slug` esposto nella view (il registry FE usa lo slug).

**`services/commons`** — meccanismo di enforcement riusabile (lo erediteranno UC 0046/0054):
- `EntitlementClient` (client REST verso `core /me/entitlements`, **propagazione del JWT** del chiamante, **cache a TTL breve**) + `EntitlementService` (`hasAccess(appSlug)`, `capFor(appSlug, metric)`, `natureOf(appSlug, metric)`).
- Gate **402** come **annotazione opt-in `@RequiresEntitlement`** + filtro/interceptor: endpoint annotato senza accesso → `EntitlementRequiredException` → **nuovo mapper 402** (problem+json, messaggio azionabile).
- Nuova `QuotaLimitSource` in `commons` che delega a `EntitlementService.capFor(...)` (app slug da `quarkus.application.name`); la SPI gestisce semanticamente **flow** (uso-nella-finestra vs cap) e **stock** (livello corrente vs cap).

**`services/fatture`** — adozione del meccanismo:
- **Ritiro** di `ConfigQuotaLimitSource` (sostituito dall'impl `commons` entitlement-driven); il dominio di `fatture` resta invariato (continua a chiamare `quota.checkAndReserve("fatture")`).
- Annotazione `@RequiresEntitlement` sugli endpoint applicativi (gate 402); cablaggio del client/JWT propagation.

**`frontend`** — cablaggio reale del registry:
- Sostituire lo stub `entitled` hardcoded con un fetch a `/me/entitlements`; un'app **appena acquistata compare in sidebar** senza refresh manuale. Il modulo `demo` (senza catalogo/backend) resta abilitato **solo nello stub locale**.

## Fuori scope

- Authorizer edge / gate grossolano (app-abilitata, entitled) — **UC 0014** (cloud); qui solo i gate fini lato servizio (difesa in profondità). _Tracciato: [_INDEX.md](../../docs/usecases/_INDEX.md) Eccezioni #1._
- **Enforcement `stock` live** (consumatore reale = seats) — **UC 0054**: qui si completa solo il *contratto* flow/stock + test SPI, **senza** app/metrica stock fittizia. _Tracciato: Punti aperti UC 0054._
- **UX azionabile FE** dei banner 402/429 + riattivazione/upgrade — **UC 0028**: qui solo il wiring del registry e le risposte problem+json. _Tracciato: Punti aperti UC 0028._
- **Esenzione GDPR** verificata sugli endpoint reali export/erasure — **UC 0032** (gli endpoint non esisteranno qui); l'esenzione è garantita *per costruzione* dal gate opt-in. _Tracciato: Punti aperti UC 0032._
- **Disaccoppiamento event-driven** (rimozione della chiamata sincrona `app→core`) — **UC 0046**. _Tracciato: [_BACKLOG.md](../../docs/_BACKLOG.md) + Punti aperti UC 0046 + [_INDEX.md](../../docs/usecases/_INDEX.md) Eccezioni #5._

## Criteri di accettazione

- [ ] `GET /api/platform/v1/me/entitlements` ritorna le app entitled del tenant del JWT con `appSlug` + `limits{metric:{cap,nature,window}}`; rispetta il filtro row-level (tenant dal JWT) e la regola `access = appActive && (grantsAccess ‖ free baseline)` (verificato con il seed: Acme→teams active entitled; app `legacy` inactive non entitled; tenant senza subscription su un'app con free tier → entitled al free).
- [ ] Gate **402** opt-in: un endpoint annotato `@RequiresEntitlement` risponde **402** quando il tenant non ha accesso e **passa** quando ha accesso; un endpoint **non** annotato resta raggiungibile anche senza accesso (esenzione GDPR per costruzione).
- [ ] Quota `fatture` (flow, cap 10) **risolta dall'entitlement** (free tier baseline) — comportamento attuale preservato: 11ª fattura nel mese → **429**. `ConfigQuotaLimitSource` rimosso.
- [ ] Contratto **flow + stock** completo in `commons` e testato (stock = tetto sul livello, niente reset) anche senza app stock; `EntitlementService.natureOf/capFor` espongono la natura da `app_tier.limits`.
- [ ] Registry FE popolato da `/me/entitlements` (non più hardcoded fuori da locale); `demo` abilitato solo nello stub locale.

## Invarianti appgrove toccati

- **`tenant_id` solo dal JWT**: `/me/entitlements` deriva il tenant dal claim verificato; il client `commons` **propaga il JWT** del chiamante a core (nessun tenant da body/param).
- **Filtro row-level**: la query subscription resta tenant-scoped (discriminator Hibernate); il catalogo (`app`/`app_tier`) è platform-level (non tenant-scoped) by design.
- **Modulo `microsaas_app`**: non toccato (nessuna infra).
- **Logging strutturato**: 402/403/429 loggati con `tenant_id`/`app_id`/`user_id` (#08).

## Requisiti di test

- **core (mvn)**: integration su `/me/entitlements` con il seed multi-stato (active/past_due/trialing/canceled + app inactive + free baseline); isolamento multi-tenant (un tenant non vede l'entitlement di un altro).
- **commons (mvn)**: gate 402 (annotato vs non annotato), mapper 402 problem+json; SPI quota flow **e** stock (cap sul livello, niente reset) a livello unit; `EntitlementService` cap/nature/hasAccess; **regression guard**: endpoint senza annotazione raggiungibile senza accesso (F31 by-construction).
- **fatture (mvn)**: quota flow cap 10 risolta via entitlement (429 alla 11ª); gate 402 sugli endpoint applicativi; client→core con JWT propagato (stub/mock di core nei test).
- **frontend (npm)**: registry popolato da `/me/entitlements` (mock); `demo` solo in locale; intersezione con `MODULES`.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (comportamento quota `fatture` preservato; nuovo endpoint additivo) |
| Contratto cross-area | Sì — nuovo endpoint `core` consumato da `frontend` **e** da `fatture` (via client `commons`); OpenAPI aggiornato |
| Version bump | minor |
