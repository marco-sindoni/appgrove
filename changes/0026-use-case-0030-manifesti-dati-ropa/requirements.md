# Change 0026: Manifesti dati per-app + RoPA automation (UC 0030)

**Branch**: `change/0026-use-case-0030-manifesti-dati-ropa`
**Aree**: services/commons · services/core · services/fatture · tools/compliance (nuovo) · docs/compliance (nuovo) · run-tests.sh · docs
**Data**: 2026-07-03
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/08-compliance-gdpr/0030-manifesti-dati-ropa.md](../../docs/usecases/08-compliance-gdpr/0030-manifesti-dati-ropa.md)
**Tocca dati personali?**: Sì — ma in senso dichiarativo: la change classifica e documenta trattamenti **già esistenti** (nessun nuovo trattamento introdotto). Il checkpoint privacy/RoPA di step-03 è esattamente ciò che questa change implementa.

## Problema / Obiettivo

I dati personali della piattaforma non sono censiti in una fonte unica: `@PersonalData` esiste ma è usata solo in `fatture`, i PII del core (differiti dalla change 0007) non sono annotati, e il RoPA (art. 30, obbligatorio — #13 C13) non esiste. Obiettivo: **manifesto-dati per-app come fonte unica** (bilingue, estensibile a più lingue) + **RoPA assemblato automaticamente** in `docs/compliance/ropa.{it,en}.md` + **check bloccanti** (parità lingue, copertura `@PersonalData`↔manifesto) — decisioni #13 C14/C15/C16/C17/C18, §50.

## Scope

1. **Formato manifesto** (fonte unica, decisa in dialogo — opzione A):
   - file YAML dichiarativi in `docs/compliance/manifests/`: `platform.yaml` (sezione piattaforma) + `<app_id>.yaml` per app (`fatture.yaml`);
   - testi descrittivi **lang-keyed** (`it:`, `en:`, estensibile a `fr/es/de`…), **niente struttura hardcoded a 2 lingue**; le lingue richieste sono configurabili (`_config.yaml`, oggi `[it, en]` per il RoPA);
   - ogni voce: categoria interessati, categoria dati, finalità, base giuridica, retention, ubicazione (`app_<id>` / user pool Cognito / CloudWatch), misure di sicurezza, destinatari/sub-responsabili e trasferimenti extra-UE ove pertinenti (checklist #13 C18);
   - le voci possono essere **entity-backed** (riferimento `entity`/`field` Java) o **non entity-backed** (Cognito, log, …).
2. **Assemblatore + check parità** — script Node in `tools/compliance/` con due modalità:
   - `assemble`: genera `docs/compliance/ropa.<lang>.md` per ogni lingua richiesta (sezione piattaforma + sezioni per-app, zero drift);
   - `check`: fallisce se (a) una voce manca di una lingua richiesta, (b) i file RoPA committati differiscono dalla rigenerazione (freshness — pattern "generato e committato");
   - test unitari dello script (node:test).
3. **Check `@PersonalData` ↔ manifesto** (stile ArchUnit, in `mvn test`): helper riusabile in `services/commons` + test in `core` e `fatture` che via reflection verificano, **bloccando la build**: campo annotato `@PersonalData` senza voce entity-backed nel manifesto → rosso; voce entity-backed che punta a campo inesistente/non annotato → rosso.
4. **Annotazioni core** (chiude il punto differito della change 0007) — classificazioni confermate in dialogo:
   - `users.email`, `users.display_name`, `users.cognito_sub`, `invitations.email` → identità/contatto, finalità erogazione/gestione account (invito: gestione invito), base contratto/precontrattuale 6.1.b, retention account attivo + grace 14gg (#13 E25);
   - `accounts.name` → dato personale (prudente: nei B2C è il nome della persona), base contratto;
   - `accounts.paddle_customer_id` → identificativo online; nota: Paddle = **titolare autonomo** (MoR), non sub-responsabile (#13 H).
5. **Manifesti iniziali completi**: `platform.yaml` (voci entity-backed di cui sopra + voci non entity-backed: credenziali/MFA in Cognito, log strutturati CloudWatch, destinatari AWS/Plausible, garanzie DPF+SCC) e `fatture.yaml` (`customer_name`, `customer_email` già annotati). RoPA it/en generati e committati (interni, non pubblici).
6. **Cablaggio test**: nuova area `compliance` in `run-tests.sh` (token nel `case` + `run_compliance` → script in modalità `check`); aggiornare la sezione "Esecuzione dei test" di `CLAUDE.md`.
7. **Doc**: riga in `docs/_REVISIONE-LEGALE.md` (revisione RoPA pre-go-live, bozza sotto disclaimer); `docs/usecases/_INDEX.md` 0030 → ✅ alla chiusura.

## Fuori scope

- **Snippet privacy pubblico** dai manifesti (UC 0002) e **export/erasure** (UC 0032) — la fonte è predisposta, i consumi no.
- **Gate co-pilota + enforcement nel workflow `new-change`** (UC 0031): qui si costruisce il check tecnico; l'integrazione di processo resta a UC 0031.
- **Workflow GitHub Actions** (UC 0005): il check gira in `run-tests.sh`/`mvn test`, la CI cloud lo erediterà.
- **Riconciliazione `DataManifest` (record Java, monolingua) vs manifesto YAML**: il record resta per la SPI export (UC 0032); coerenza transitiva garantita via annotazioni. Eventuale unificazione → da tracciare come punto differito in UC 0032.
- `auth_local.credentials`: solo dev (#11 B), esclusa dal RoPA. Nessun bump PP/ToS (testi legali non ancora redatti, UC 0002).

## Criteri di accettazione

- [ ] `tools/compliance` `check` verde sui manifesti committati; rosso se si rimuove una lingua richiesta da una voce o si modifica un manifesto senza rigenerare il RoPA.
- [ ] `mvn test` rosso se: campo `@PersonalData` non dichiarato nel manifesto, oppure voce entity-backed orfana; verde sullo stato finale con core annotato.
- [ ] `docs/compliance/ropa.it.md` + `ropa.en.md` generati, identici come set di voci, committati.
- [ ] `./run-tests.sh compliance` funziona e l'area è inclusa nel run completo.

## Invarianti appgrove toccati

Nessuno modificato: le annotazioni sono metadati sui campi (nessun impatto su tenant filter/JWT); nessuna nuova infra; nessun log nuovo. Le regole ArchUnit esistenti restano verdi.

## Requisiti di test

- Da UC 0030 §9: parità lingue (voce senza lingua richiesta → check rosso) e `@PersonalData` non dichiarato → build rossa: entrambi coperti con test negativi (fixture per lo script Node; entità di test per il check JUnit).
- Freshness: manifesto modificato senza rigenerare RoPA → check rosso.
- La coerenza manifesto ↔ export/purge (#13 L74) resta a UC 0032.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A (nessuna API toccata) |
| Version bump | nessuno |
