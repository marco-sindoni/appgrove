# Implementation Log — Change 0039: Authorizer al bordo (UC 0014)

**Branch**: `change/0039-use-case-0014-authorizer-custom`
**Aree**: `infra/` (+ documentazione)
**Completata**: 2026-07-18

## File modificati

| File | Azione |
|---|---|
| `infra/modules/platform_shared/authorizer.tf` | Creato — authorizer JWT nativo + motivazione della deviazione |
| `infra/modules/platform_shared/tests/plan.tftest.hcl` | Creato — prima suite del modulo (authorizer, rotte pubbliche, proxy DB) |
| `infra/modules/platform_shared/.terraform.lock.hcl` | Creato — versioni provider riproducibili (come gli altri moduli testati) |
| `infra/modules/platform_shared/auth.tf` | Modificato — `local.cognito_issuer` (elimina la duplicazione dell'URL) |
| `infra/modules/platform_shared/outputs.tf` | Modificato — nuovo output `authorizer_id`; issuer/JWKS dal local |
| `infra/modules/platform_shared/rds_proxy.tf` | Modificato — ingress ristretto alle sole Lambda auth (E23-b) |
| `infra/modules/platform_shared/ingress.tf` | Modificato — commenti di testata allineati |
| `infra/modules/microsaas_app/api.tf` | Modificato — route `authorization_type = "JWT"`; nuove route `public` con precondizione |
| `infra/modules/microsaas_app/variables.tf` | Modificato — `authorizer_id` in `shared`; nuova variabile `public_routes` |
| `infra/modules/microsaas_app/tests/plan.tftest.hcl` | Modificato — verifiche su authorizer, default sicuro, eccezione webhook |
| `infra/modules/microsaas_app/tests/fixtures/double/main.tf` | Modificato — fixture allineata |
| `infra/envs/test/main.tf`, `infra/envs/prod/main.tf` | Modificati — `authorizer_id` cablato; `public_routes` sul core |
| `infra/scripts/check` | Modificato — `platform_shared` registrato fra i moduli testati |
| `docs/usecases/04-platform-core/0014-authorizer-custom.md` | Modificato — revisione completa + punti aperti |
| `docs/usecases/05-auth/0015-*.md`, `0016-*.md` | Modificati — punti riassegnati/nuovi |
| `docs/usecases/07-payments/0027-*.md`, `0029-*.md` | Modificati — punti differiti tracciati |
| `docs/02-auth-sicurezza.md`, `04-services-backend.md`, `06-infra-iac.md`, `08-observability.md` | Modificati — allineati alla nuova ripartizione |
| `docs/_BACKLOG.md`, `_EVOLUZIONI-DEVOPS.md`, `_COSTI-AWS.md` | Modificati — E23 aggiornato, item chiusi/riassegnati |
| `docs/usecases/_INDEX.md`, `docs/usecases/README.md` | Modificati — UC 0014 ✅, titolo e "prossimo da implementare" |

## Cosa è stato fatto

Le route `/api/<app_id>/v1/*`, finora **completamente aperte**, sono ora protette al bordo dall'**authorizer JWT nativo**
di API Gateway: firma, emittente, destinatario e scadenza del token verificati prima del VPC Link, con **401** corretto.
L'authorizer nasce nelle risorse condivise e viene agganciato dal modulo `microsaas_app`, quindi **ogni app futura è
protetta per costruzione**. L'unico endpoint pubblico censito — il webhook dei pagamenti — è esposto come **route dedicata
più specifica** senza authorizer, con una precondizione che impedisce a un'app di dichiarare percorsi fuori dal proprio
prefisso. Chiuso anche il residuo **E23-b**: l'ingresso al proxy del database è passato dall'intera rete privata ai soli
gruppi di sicurezza delle due funzioni di autenticazione.

## Decisioni prese

**1. Authorizer nativo invece della funzione Lambda prevista dallo use case** (decisa con l'utente al gate di
chiarimento, motivazione estesa in `requirements.md` e in `authorizer.tf`). Su HTTP API v2 il rifiuto di un authorizer
scritto da noi diventa **sempre 403**, non personalizzabile. Verificato sul codice che ciò romperebbe due meccanismi
funzionanti: il **rinnovo silenzioso della sessione** (costruito sul 401 — un token scaduto darebbe 403 e l'utente
cadrebbe fuori a ogni scadenza) e l'**avviso "abbonamento richiesto"** (costruito sul 402). I controlli app-abilitata ed
entitled restano quindi nel servizio, dove hanno già i codici giusti e un database vero per testarli.

**2. Regola di accesso allineata al codice.** Lo use case descriveva l'entitlement come derivato dal **solo** abbonamento;
il core implementa una regola più larga che include il **piano gratuito** e nega l'accesso agli account in cancellazione.
Applicare la regola stretta avrebbe prodotto rifiuti falsi sulle app gratuite. Lo use case è stato corretto.

**3. Eccezioni come rotte dedicate, non come lista nel codice.** Il gateway sceglie sempre la rotta più specifica: una
deroga espressa come rotta è visibile nel piano e verificabile dai test, mentre una lista dentro una funzione si
disallinea in silenzio quando qualcuno aggiunge un endpoint.

**4. E23-a rimandato di proposito.** Il passaggio del proxy all'autenticazione tramite identità AWS cambierebbe il codice
di connessione di entrambe le funzioni (meccanismi diversi in Python e Java), non è provabile in locale e, se sbagliato,
si manifesta come "nessuna funzione si collega più al database" — proprio durante la prima accensione cloud, mai
avvenuta. È un irrobustimento, non la chiusura di una falla.

## Note sull'implementazione

La suite nuova del modulo delle risorse condivise ha richiesto qualche giro: gli identificativi generati dal provider
finto sono stringhe casuali e diversi attributi (indirizzi di risorsa, documenti di autorizzazione) vengono validati già
in fase di piano. La soluzione adottata è **mirata**: valori finti plausibili per le poche risorse validate, più override
espliciti sulle risorse di cui i test devono confrontare l'identità reale (pool, app client, gateway, gruppi di
sicurezza). Si resta così in modalità "piano", offline e veloce, senza dover simulare l'intero modulo.

## Invarianti appgrove

- **Tenant ID solo dal token verificato** — **rafforzato**: firma, emittente, destinatario e scadenza sono ora verificati
  al bordo, prima che la richiesta raggiunga il servizio. La verifica in profondità nel servizio (tipo di token e client)
  resta e **non va rimossa**: i servizi sono raggiungibili anche dalla rete interna.
- **Filtro per tenant sulle query** — non toccato.
- **Modulo `microsaas_app`** — rispettato e rafforzato: la protezione arriva dal modulo, nessuna infrastruttura su misura.
  La variabile `public_routes` nasce vuota, quindi il default di una nuova app è "interamente protetta".
- **Logging strutturato** — non toccato: nessun codice applicativo nuovo. L'intestazione di correlazione continua a essere
  apposta dall'integrazione e l'authorizer nativo non la altera.

## Note per il revisore

**Impatto cross-area**: infrastruttura ↔ frontend. Il contratto 401/402 su cui poggiano il rinnovo silenzioso e l'avviso
di abbonamento è **preservato** — ed è la ragione stessa della scelta tecnica. Nessuna riga di frontend o di servizio è
stata toccata: che le loro suite restino verdi **senza modifiche** è la verifica che la ripartizione regge.

**Verificabilità**: l'authorizer non è esercitabile in locale (il dev stack passa da un server di sviluppo, non da API
Gateway). Le suite verificano la **configurazione**; il comportamento si vedrà alla prima accensione cloud. Le verifiche
concrete da eseguire allora — comandi inclusi, con il caso "se qui esce 403 fermati" — sono nel **punto 10 della
checklist prima accensione cloud** in [_BACKLOG](../../docs/_BACKLOG.md). Il canarino continuo è il test end-to-end su
ambiente reale, che dipende dall'arrivo del webhook.

**Gate privacy (UC 0031)**: eseguito sul diff finale — **nessun segnale**. Nessun dato personale coinvolto: la change è
solo configurazione di gateway e di un gruppo di sicurezza. Nessun sub-processore nuovo. Classificazione non applicabile.

**Decisioni differite tracciate** (nessuna lasciata in chat):

| Punto | Tracciato in |
|---|---|
| E23-a — autenticazione tramite identità AWS verso il proxy del database, con motivazione del rinvio e prossimo passo | UC 0014 §Punti aperti; `_EVOLUZIONI-DEVOPS` E23; `_BACKLOG` (checklist prima accensione) |
| Blocco al bordo per app disabilitata / tenant non abilitato: rinuncia consapevole e tre strade se servirà | UC 0014 §Punti aperti; `_BACKLOG` #4 |
| Chiamata app→core per gli entitlement: in cloud deve puntare all'indirizzo interno, mai al dominio pubblico | UC 0027 §Punti aperti |
| Costo per-richiesta del salto app→core (preesistente, non risolto da questa change) | UC 0027 §Punti aperti |
| Test end-to-end come canarino dell'eccezione sul webhook | UC 0029 §Punti aperti |
| Eventi di audit dei flussi di autenticazione: **riassegnati** da UC 0014 a UC 0015 e UC 0016 | UC 0015 e UC 0016 §Punti aperti |
| Vincolo sul destinatario dell'authorizer se nascerà un secondo app client | UC 0015 §Punti aperti |
| Drift documentale "Quarkus OIDC" vs implementazione reale nei documenti di area (non corretto qui: sono documenti di decisione dell'utente) | UC 0016 §Punti aperti |
| Override entitlement per-tenant: **riassegnato** da UC 0014/0027 a UC 0027 | `_BACKLOG` #16 |

Punti **chiusi** in questa change e marcati come tali: enforcement disable-app (collocazione rivista), E23-b, esclusione
health/Swagger (vera per costruzione), propagazione dell'identificativo di correlazione (decaduta).

## Test

**Infrastruttura** — `./run-tests.sh infra`: **verde**. Include formattazione, validazione dei quattro stack, le tre suite
di modulo e il controllo di sicurezza automatico (**510 passati, 0 falliti**): la deroga rimossa dal modulo delle app ora
**passa il controllo** invece di essere soppressa.

Test aggiunti/aggiornati:
- `modules/microsaas_app` — la route nasce con authorizer JWT agganciato a quello passato dalle risorse condivise; senza
  `public_routes` **nessuna** rotta scoperta (default sicuro); il webhook ha la sua rotta senza authorizer **senza**
  scoperchiare il proxy generico del core.
- `modules/platform_shared` (**suite nuova**, il modulo non ne aveva) — l'authorizer è nativo, legge il solo header di
  autorizzazione, con emittente = pool e destinatario = app client; le rotte pubbliche per progetto restano scoperte e
  fuori dal pattern coperto dall'authorizer; l'ingresso al proxy del database non accetta più indirizzi di rete, solo i
  due gruppi di sicurezza, su Postgres, con cifratura obbligatoria.

**Suite completa** — `./run-tests.sh`: **verde su tutte le aree** (backend, frontend, infra, compliance, smoke). Frontend
e backend sono passati **senza alcuna modifica**, che è esattamente il requisito di non-regressione dei contratti 401/402.

Nessuno snapshot visivo è stato rigenerato (nessuna modifica al frontend).

## Stato criteri di accettazione

- [x] Route `/api/<app_id>/v1/{proxy+}` protette dall'authorizer JWT; deroga al controllo di sicurezza rimossa e controllo verde
- [x] Rotta `POST /api/platform/v1/webhooks/paddle` senza authorizer, più specifica del proxy generico, con deroga motivata
- [x] Authorizer configurato con emittente e destinatario del pool esistente, sul solo header di autorizzazione
- [x] Ingresso al proxy del database ristretto ai soli gruppi di sicurezza delle due funzioni di autenticazione
- [x] Suite del modulo delle risorse condivise creata e registrata; `./run-tests.sh infra` verde
- [x] Use case 0014 rivisto e punti differiti tracciati nei file di competenza
