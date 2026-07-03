# Implementation Log — Change 0026: Manifesti dati per-app + RoPA automation (UC 0030)

**Branch**: `change/0026-use-case-0030-manifesti-dati-ropa`
**Aree**: services/commons · services/core · services/fatture · tools/compliance (nuovo) · docs/compliance (nuovo) · run-tests.sh · docs
**Completata**: 2026-07-03

## File modificati

| File | Azione |
|---|---|
| `docs/compliance/manifests/_config.yaml` | Creato — lingue richieste configurabili (`[it, en]`) |
| `docs/compliance/manifests/platform.yaml` | Creato — manifesto piattaforma (6 voci entity-backed + Cognito + log, sezioni C18) |
| `docs/compliance/manifests/fatture.yaml` | Creato — manifesto app fatture (`customer_name`, `customer_email`) |
| `docs/compliance/ropa.it.md` / `ropa.en.md` | Creati — RoPA **generati** (interni, committati, freshness-checked) |
| `tools/compliance/{package.json,lib.mjs,ropa.mjs}` | Creati — assemblatore + check parità lingue/freshness (Node, dep `yaml`) |
| `tools/compliance/test/lib.test.mjs` | Creato — 7 test node:test (parità, rendering, casi negativi) |
| `services/commons/src/test/.../PersonalDataManifestVerifier.java` | Creato — check annotazioni↔manifesto (ArchUnit + Jackson YAML), nel test-jar |
| `services/commons/src/test/.../PersonalDataManifestVerifierTest.java` + fixtures | Creati — 3 test (verde/`NON dichiarato`/`stantia`) |
| `services/{core,fatture}/src/test/.../PersonalDataManifestTest.java` | Creati — gate bloccante in `mvn test` per servizio |
| `services/core/src/main/.../{User,Invitation,Account}.java` | Modificati — `@PersonalData` su email/displayName/cognitoSub, email invito, name, paddleCustomerId |
| `services/{pom,commons/pom,core/pom,fatture/pom}.xml` | Modificati — test-jar commons, dipendenze test (archunit/jackson-yaml), pin `maven-jar-plugin` |
| `run-tests.sh` + `CLAUDE.md` | Modificati — nuova area `compliance` (token, `run_compliance`, npm install auto) |
| `docs/_REVISIONE-LEGALE.md` | Modificato — riga L14 (revisione RoPA/classificazioni pre-go-live) |
| `docs/usecases/08-compliance-gdpr/0030-*.md` | Modificato — punto differito PII core chiuso (✅) |
| `docs/usecases/08-compliance-gdpr/0032-*.md` | Modificato — nuovo punto differito (riconciliazione `DataManifest` ↔ YAML) |
| `docs/usecases/_INDEX.md` | Modificato — 0030 → ✅ |

## Cosa è stato fatto

Manifesto dati per-app come **fonte unica** in YAML dichiarativi bilingue (testi lang-keyed, lingue richieste
configurabili in `_config.yaml`), con voci entity-backed (`entity`+`field`) e non (Cognito, log). Da questa fonte
`tools/compliance` **assembla** `docs/compliance/ropa.{it,en}.md` (deterministico, committato) e in modalità `check`
verifica **parità lingue** e **freshness** (drift → rosso). Il check **`@PersonalData` ↔ manifesto** è un verifier
JUnit riusabile nel test-jar di commons, bloccante in `mvn test` di core (manifesto `platform`) e fatture, in
**entrambe le direzioni** (campo annotato non dichiarato / voce stantia). Le entità core sono state annotate chiudendo
il punto differito della change 0007.

## Decisioni prese

- **Escaping YAML**: i testi con ` #…` a metà riga (es. `— #13 H)`) vanno **quotati**, altrimenti YAML li tronca come
  commento (bug trovato e corretto in `platform.yaml`; i blocchi `>-` non sono affetti).
- **Verifier nel test-jar di commons** (non in main): niente dipendenze YAML/ArchUnit nel runtime dei servizi; i
  servizi lo importano con `<type>test-jar</type>`. Aggiunto pin `maven-jar-plugin` 3.4.2 nel parent (warning Maven).
- **Fixture ereditano `BaseEntity`**: il test-jar finisce sul classpath di test dei servizi e le regole ArchUnit
  esistenti scandiscono `app.appgrove` — la fixture `@Entity` deve rispettarle.
- **Path del manifesto risolto risalendo** da `user.dir` fino a `docs/compliance/manifests` (robusto a reactor/IDE).

## Invarianti appgrove

Nessuno alterato: le annotazioni sono metadati (nessun impatto su JWT/row-level filter); le regole ArchUnit esistenti
restano verdi; nessuna infra o log nuovi. Il RoPA documenta gli invarianti (isolamento tenant nelle misure di sicurezza).

## Note per il revisore

- I file `ropa.*.md` sono **generati**: si modificano solo i manifesti YAML e si rigenera (`npm run assemble` in
  `tools/compliance`); il check in `run-tests.sh compliance` fallisce sul drift.
- **Decisioni differite tracciate**: (1) riconciliazione `DataManifest` record ↔ manifesti YAML → UC 0032 ("Punti
  aperti"); (2) gate co-pilota/processo in `new-change` → già di UC 0031; (3) consumo per snippet privacy → UC 0002.
- Nessun contratto cross-area toccato. Nessun bump PP/ToS (testi legali non ancora redatti, UC 0002).
- Classificazioni prudenti (`accounts.name`, `paddle_customer_id`, base 6.1.f per i log) da validare col legale
  (riga L14 di `docs/_REVISIONE-LEGALE.md`).

## Test

- **backend** (`./run-tests.sh backend`): **verde** — commons 12 (3 nuovi del verifier: allineato/non
  dichiarato/stantia), core 30s suite con `PersonalDataManifestTest`, fatture 22 con `PersonalDataManifestTest`,
  auth-local invariato.
- **compliance** (`./run-tests.sh compliance`): **verde** — 7 test node:test + `check` su manifesti reali.
- **Verifiche negative sull'albero reale** (eseguite e ripristinate): drift manifesto→RoPA → check rosso
  ("rigenera con `npm run assemble`"); rimozione traduzione `en` → check rosso con puntatore preciso
  (`entries[1] (invoice.customer_email): purpose: manca la traduzione "en"`).
- frontend/infra non toccati → suite non richieste.

## Stato criteri di accettazione

- [x] `check` verde sui manifesti committati; rosso su lingua mancante o manifesto modificato senza rigenerare.
- [x] `mvn test` rosso su campo `@PersonalData` non dichiarato o voce entity-backed orfana (fixture commons); verde sullo stato finale con core annotato.
- [x] `ropa.it.md` + `ropa.en.md` generati dalla stessa fonte (stesso set di voci per costruzione + test), committati.
- [x] `./run-tests.sh compliance` funziona ed è inclusa nel run completo (default AREAS).
