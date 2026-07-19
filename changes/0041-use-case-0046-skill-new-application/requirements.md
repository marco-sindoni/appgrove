# Change 0041: skill `new-application` — scaffolding di una nuova app + industrializzazione degli agganci

**Branch**: `change/0041-use-case-0046-skill-new-application`
**Aree**: `.claude/skills/`, `tools/` (generatore), `services/commons`, `services/core`, `services/fatture`, `frontend/`, `infra/`, `.github/workflows/`, `dev/`, `run-tests.sh`, `docs/`
**Data**: 2026-07-19
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/10-skills-tooling/0046-skill-new-application.md](../../docs/usecases/10-skills-tooling/0046-skill-new-application.md)
**Tocca dati personali?**: Sì (indiretto) — la change introduce nuove migrazioni Flyway (tabella di proiezione dei diritti) e nuove entità: il gate privacy/RoPA di step-03 va eseguito. Attesa classificazione **MINOR** (nessuna nuova categoria di dati personali, nessun cambio di finalità/base/retention: la proiezione replica dati di abbonamento già trattati), da confermare col co-pilota. La skill generata è essa stessa lo strumento che **obbliga** a compilare il manifesto dati della nuova app.

## Problema / Obiettivo

Creare una nuova applicazione sul marketplace richiede oggi di replicare a mano il pattern dell'app #1
`fatture`: circa **40 file da creare** (backend Quarkus, modulo frontend, migrazioni, test, manifesto dati,
listino) e **14 file esistenti da modificare** (elenco moduli Maven, registro moduli frontend, tre workflow
di integrazione continua, script di avvio locale, reverse proxy locale, script di verifica d'avvio). È
lento, non ripetibile e non verificabile.

Obiettivo: la skill **`new-application`** codifica il pattern in uno scaffolding **ripetibile e collaudato**,
così chi crea un'app si concentra solo sul business. Contestualmente si chiudono i tre lavori arretrati
intestati a UC 0046, che oggi renderebbero il generatore invasivo e fragile.

## Scope

### 1. Skill `new-application` — struttura ibrida (istruzioni + generatore)

Deciso al gate di chiarimento: **non** una skill di solo testo. Due metà che si incastrano:

- **Generatore deterministico** in `tools/new-application/`: cartella di modelli-sorgente (il gemello di
  `fatture` con segnaposto per identificativo app, porta, schema, modello utente) + script che li istanzia
  e applica le modifiche ai file esistenti. Versionato, quindi **collaudabile**.
- **Skill conversazionale** in `.claude/skills/new-application/` (`SKILL.md` + `step-NN-*.md`, stile e
  convenzioni identici a `new-change`): raccolta identificativo/modello utente/icona-colore, invocazione del
  generatore, **co-pilota prezzi/quota**, **co-pilota dati personali**, bozza landing, consegna nel flusso di
  `new-change` (ramo + richiesta di unione all'utente, **mai** unione automatica).

Copertura del generatore, per artefatto:

| Genera | Modifica | Delega |
|---|---|---|
| `services/<id>/` (modulo Maven, `<App>Main` con command-mode `migrate`, entity su `BaseTenantEntity`, repository, resource `/api/<id>/v1/*` con `@RequiresEntitlement`, `QuotaResource`, `<App>QuotaService`, `<App>DataContract`, `application.properties`, 3 migrazioni incl. `gdpr_purge_audit`, 2 Dockerfile) | `services/pom.xml` | `infra/scripts/service-add <id> --port N` |
| suite test completa (integrazione API, isolamento tenant, gate diritti 402, quota 429, contratto GDPR, manifesto `@PersonalData`, ArchUnit, MDC, health, command `migrate`) + harness | `frontend/apps/backoffice/src/registry/registry.ts` | `tools/compliance && npm run assemble` |
| `frontend/apps/backoffice/src/modules/<id>/` (manifest, modulo lazy, client tipizzato, schermate, `strings.ts`, test) + `e2e/<id>.spec.ts` | `frontend/apps/backoffice/package.json` (`gen:<id>`) | |
| `docs/compliance/manifests/<id>.yaml` (IT+EN) | `services/core/.../pricing/index.yaml` | |
| `services/core/.../pricing/<id>.yaml` | | |
| bozza landing 5 lingue (stato `draft`) | | |

Invarianti **by-default** nello scaffold: `tenant_id` solo dal JWT (ereditato da `commons`), filtro
row-level via discriminator, istanza del modulo `microsaas_app` (mai infrastruttura artigianale), logging
strutturato con `tenant_id`/`app_id`/`user_id`, `installErrorReporter` nel modulo frontend, contratto GDPR
obbligatorio.

Co-piloti (dialogo guidato, una domanda alla volta, conferma esplicita prima di scrivere):
- **prezzi/quota**: livelli, prezzi mensile/annuale con sconto ~17%, gratuito, prova (14 giorni, disattivabile),
  natura della metrica (a consumo / a giacenza), commissione effettiva con avviso non bloccante oltre il 10%;
- **dati personali**: manifesto IT+EN completo, snippet privacy, export/cancellazione, **avviso forte + valutazione
  d'impatto** per categorie particolari, guardrail contro la pseudonimizzazione spacciata per cancellazione.

### 2. Debito 1 — avvio locale e verifica d'avvio per scoperta automatica

Eliminare il nome `fatture` scritto a mano da: `dev/lib/migrate.sh` (oggi solo `core`) → scoperta di tutti i
`services/*/src/main/resources/db/migration` con relativo schema; `dev/lib/service.sh` (oggi guscio vuoto) →
avvio selettivo `core` + app richiesta; hook "processi-app" di `dev/lib/up.sh`; `app-start.sh` / `app-stop.sh`;
`dev/Caddyfile`; `tools/smoke/boot-profiles.sh` e `stack-headless.sh`. Sorgente unica della mappa
app→porta→schema, così **il generatore non incolla righe** in questi file.

### 3. Debito 2 — liste dei servizi nei workflow di integrazione continua

I sette punti che ripetono a mano i nomi dei servizi (matrice di build e cicli in `deploy-test.yml`, tre cicli
in `release-prod.yml`, `-pl` in `verify-pr.yml`) vanno **derivati da una sorgente unica** invece che duplicati.

### 4. Debito 3 — proiezione locale dei diritti (ritiro della chiamata sincrona app→core)

Postura decisa al gate: **cache con rete di sicurezza**.

- `services/core` **pubblica eventi** di ciclo di vita dell'abbonamento sul bus interno (ossatura SQS da UC 0025).
- `services/commons`: consumatore + **tabella di proiezione in sola lettura** nello schema `app_<id>`, e una
  `QuotaLimitSource` che legge la proiezione (sostituisce `EntitlementQuotaLimitSource` sul percorso caldo).
- Risoluzione: proiezione **presente** → si usa, senza soglia di scadenza; proiezione **assente** → **una**
  chiamata sincrona al core come rete di sicurezza, con scrittura del risultato in proiezione; assente **e**
  core irraggiungibile → si blocca (nessuna base per decidere).
- **Strumentazione degli scostamenti** (richiesta esplicita): misure + allarmi su proiezione mancante, proiezione
  molto più vecchia dell'atteso, messaggi finiti nella coda degli scarti, e frequenza di ricorso alla rete di
  sicurezza. Allarmi nel modulo `microsaas_app`, così ogni app li eredita.
- `services/fatture` migrata alla proiezione; il generatore la produce per ogni nuova app.

### 5. Collaudo del generatore — livello (3), sempre

Nuova area **`tooling`** in `run-tests.sh` (invocabile da sola; inclusa nell'esecuzione completa, fuori da
`./run-tests.sh backend` per non rallentare i cicli rapidi): genera un'app di prova in cartella temporanea,
**ne esegue l'intera suite** e verifica il verde, poi ripulisce. È la dimostrazione letterale della promessa
"l'app generata nasce con suite verde" e la guardia contro la deriva dei modelli-sorgente.

## Fuori scope

- **Il business della nuova app**: lo scaffold produce dominio segnaposto, non logica applicativa.
- **Pubblicazione della landing** → skill `finalize-landing` (UC 0057). Qui solo la bozza `draft`.
- **Sincronizzazione prezzi verso il fornitore di pagamento** → pipeline di deploy (UC 0022).
- **La seconda app vera** (B2B multi-utente) → UC 0054, che collauda la skill sul campo.
- **Skill `drop-application`** (UC 0048) e **`pricing-change`**: non toccate.
- Nessun `terraform apply`, nessun deploy, nessun dialogo diretto col fornitore di pagamento.

## Criteri di accettazione

- [ ] `/new-application` produce, su un ramo dedicato, uno scaffold completo (backend + frontend + istanza del
      modulo infrastrutturale + workflow + manifesto dati IT+EN + contratto GDPR + listino + bozza landing +
      seed + test) e **lascia la richiesta di unione all'utente**.
- [ ] I due co-piloti (prezzi/quota e dati personali) chiedono una cosa alla volta e **fanno confermare** prima
      di scrivere; senza manifesto dati compilato lo scaffold non si chiude.
- [ ] `./run-tests.sh tooling` genera un'app di prova, ne esegue l'intera suite e chiude in verde; `./run-tests.sh`
      (completo) la include.
- [ ] Il generatore **non modifica** `dev/lib/*.sh`, `app-start.sh`, `app-stop.sh`, `dev/Caddyfile`,
      `tools/smoke/*.sh` né i tre workflow: quei file si configurano per scoperta automatica o da sorgente unica.
- [ ] L'app `fatture` legge diritti e tetti di consumo dalla **proiezione locale**; la chiamata sincrona al core
      resta solo come rete di sicurezza per il caso "cliente sconosciuto" ed è **strumentata con allarmi**.
- [ ] Tutte le aree di `run-tests.sh` restano verdi.

## Invarianti appgrove toccati

Tutti e quattro, due volte: la change li rispetta **e** deve garantire che lo scaffold generato li rispetti
per costruzione.

- **`tenant_id` solo dal JWT verificato**: ereditato da `JwtTenantResolver` in `commons`; la resource generata
  non legge mai il tenant da corpo o parametri. Il consumatore di eventi e la proiezione girano **fuori** da una
  richiesta con JWT: il tenant arriva dal contenuto dell'evento pubblicato dal core, mai da input esterno non
  autenticato. Test di isolamento cross-tenant nella suite generata.
- **Filtro row-level `WHERE tenant_id = :tid`**: discriminator Hibernate per il dominio; per la proiezione,
  lettura sempre vincolata al tenant risolto dal JWT della richiesta in corso.
- **Modulo Terraform `microsaas_app`**: il generatore **delega** a `infra/scripts/service-add`, non riscrive il
  wiring infrastrutturale. Le nuove code e i nuovi allarmi della proiezione si aggiungono **dentro** il modulo.
- **Logging strutturato**: `MdcRequestFilter` da `commons` nello scaffold; i log del consumatore di eventi e della
  rete di sicurezza portano `tenant_id`, `app_id`, `user_id` quando disponibile.

## Requisiti di test

- **Collaudo del generatore (area `tooling`)**: generazione + suite completa dell'app di prova, verde.
- **Nessun segnaposto residuo**: nessun file generato contiene marcatori di sostituzione non risolti.
- **Scoperta automatica**: con l'app di prova presente, `dev migrate` ne esegue le migrazioni e `dev service <id>`
  la avvia, senza modifiche a mano agli script.
- **Proiezione dei diritti**: proiezione presente → nessuna chiamata al core; proiezione assente → **esattamente
  una** chiamata di rete di sicurezza e scrittura in proiezione; assente + core irraggiungibile → accesso negato;
  evento di disdetta consumato → l'accesso successivo è negato. Idempotenza sui messaggi ripetuti.
- **Strumentazione**: le misure di scostamento vengono emesse nei casi previsti (proiezione mancante, proiezione
  stantia, ricorso alla rete di sicurezza).
- **Guardia dei workflow**: un test verifica che le liste dei servizi siano derivate e non divergano dai servizi
  realmente presenti in `services/`.
- **Regola baseline visive** (#10 F): nessuna baseline di istantanea aggiornata alla cieca.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | **Sì**, interno: la risoluzione di diritti e tetti di consumo passa dalla chiamata sincrona alla proiezione locale. Nessun contratto pubblico rotto (gli endpoint `/api/*` restano identici); l'app `fatture` è migrata nella stessa change. |
| Contratto cross-area | **Sì**: nuovo contratto di evento core → app (bus interno); frontend ↔ API invariato; servizio ↔ infrastruttura (nuove code e allarmi nel modulo `microsaas_app`). |
| Version bump | **minor** |

**Nota sulla dimensione.** La change è volutamente ampia: al gate di chiarimento è stato deciso di includere
anche il debito 3, perché va comunque chiuso **prima** di UC 0054 e spezzarlo introdurrebbe solo un confine
artificiale. Il rischio si concentra lì, non nello scaffolding: la revisione si concentri sulla postura di
risoluzione dei diritti e sulla sua strumentazione.
