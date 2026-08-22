# Implementation Log — Change 0097: listino dei posti a scaglioni progressivi

**Branch**: `change/0097-use-case-0102-listino-posti-fasce`
**Aree**: `services/core` · `.github/workflows` (due passi di pipeline) (+ documentazione: registro di copertura end-to-end, rimandi negli use case)
**Completata**: 2026-08-22
**Modalità**: **fast** — dichiarata all'invocazione dalla skill `go-fast`. Le risposte alle domande di
approfondimento sono dell'agente e sono tracciate in [decisions.json](decisions.json); i tre gate di
workflow sono stati rinunciati in anticipo dallo sviluppatore, con le contropartite obbligatorie: suite
completa verde prima del commit, registro delle decisioni integrale e guida di collaudo
[how-to-test.md](how-to-test.md) **scritta ed eseguita** nei passi non visivi.

## File modificati

| File | Azione |
|---|---|
| `services/core/src/main/resources/db/migration/V21__seat_pricing.sql` | Creato |
| `services/core/src/main/resources/pricing/seats.yaml` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/seats/SeatPricingVersion.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/seats/SeatPricingBand.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/seats/SeatPricing.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/seats/SeatPricingRepository.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/seats/SeatPricingDefinition.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/seats/SeatPricingLoader.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/seats/SeatPricingStartup.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/seats/SeatCount.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/seats/SeatPricingDtos.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/seats/SeatPricingResource.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/seats/IncoherentSeatPricingException.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/seats/NoSeatPricingVersionException.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/CoreMain.java` | Modificato (comando di distribuzione `seed-seat-pricing`) |
| `services/core/src/main/resources/application.properties` | Modificato (`appgrove.seat-pricing.seed-on-startup`: falso, vero in `%dev`) |
| `.github/workflows/deploy-test.yml` · `release-prod.yml` | Modificati (passo `seed-seat-pricing` dopo il `migrate`) |
| `docs/_PARITA-SCAFFOLD.md` | Modificato (due deviazioni consapevoli registrate per i due workflow) |
| `services/core/src/main/resources/META-INF/openapi/openapi.yaml` · `openapi.json` | Modificati (rigenerati dal build) |
| `services/core/src/test/java/app/appgrove/core/billing/seats/SeatPricingTest.java` | Creato |
| `services/core/src/test/java/app/appgrove/core/billing/seats/SeatPricingLoaderTest.java` | Creato |
| `services/core/src/test/java/app/appgrove/core/billing/seats/SeatPricingApiTest.java` | Creato |
| `services/core/src/test/java/app/appgrove/core/billing/seats/SeatCountApiTest.java` | Creato |
| `services/core/src/test/java/app/appgrove/core/billing/seats/SeedSeatPricingCommandTest.java` | Creato |
| `services/core/src/test/java/app/appgrove/core/billing/seats/SeatProbeResource.java` | Creato (solo classpath di test) |
| `services/core/src/test/java/app/appgrove/core/TestData.java` | Modificato (aiutanti per il listino dei posti e per lo stato degli inviti; corretto un difetto in `wasNull()`) |
| `services/core/src/test/resources/application.properties` | Modificato (probe di test escluso dallo spec OpenAPI) |
| `docs/testing/copertura-e2e.yaml` | Modificato (0102 riclassificato `senza-superficie`) |
| `docs/usecases/22-refactor-membership-model/story/0102-listino-posti-a-fasce.md` | Modificato (due rimandi) |
| `docs/usecases/22-refactor-membership-model/story/0103-acquisto-anticipato-posto-invito.md` | Modificato (due rimandi ricevuti) |

## Cosa è stato fatto

Il listino dei posti diventa una cosa che esiste: due tabelle di piattaforma conservano **versioni
immutabili** del listino con la loro decorrenza e le loro fasce; un file di risorse (`pricing/seats.yaml`)
fornisce il valore iniziale e la prima versione nasce da lì all'avvio, una volta sola; il calcolo del dovuto
è una funzione pura a **scaglioni progressivi** che somma, fascia per fascia, i posti che vi cadono per la
tariffa della fascia; una regola sola dice che cosa occupa un posto; e un'operazione di rete aperta a
qualunque autenticato serve il listino vigente.

La franchigia dei primi tre posti **non è una condizione nel programma**: è la prima fascia, a tariffa zero.
È la scelta che tiene semplice il calcolo e che rende un cambio di franchigia una riga di listino invece di
una riga di codice.

## Decisioni prese

Ventotto decisioni, tutte in [decisions.json](decisions.json) e tutte marcate `(autopilot)` tranne la
prima (la modalità). Le portanti:

- **Nessuna escalation sui prezzi** (decisione 3). Fasce, tariffe, franchigia, valuta e ciclo mensile sono
  già fissati per iscritto nel drill-down §4 e nell'epica E22.2, con l'alternativa scartata e il motivo:
  sono decisioni prese, non da prendere. La change non introduce nessuna soglia nuova.
- **La regola del posto è una sola** (decisione 6): occupa un posto chi ha un'appartenenza **viva**, più ogni
  invito in attesa non scaduto. Scritta sull'*esistenza* dell'appartenenza e non sull'elenco dei suoi stati,
  comprende da sé l'owner, le persone sospese e — quando arriverà con UC 0104 — quelle indicate per la
  cessazione, **senza modifiche**.
- **Due residui della specifica corretti con motivazione** (decisione 5): l'espressione «i tre punti in cui
  il dovuto scende» (§9) e la «trappola» n. 1 del piano di lavoro appartengono al modello **scartato**. Col
  modello adottato il dovuto è monotono crescente e a scendere è il costo del posto successivo: si provano
  entrambe le cose.
- **Semina, non sincronizzazione** (decisione 11): risincronizzare il listino a ogni avvio significherebbe
  annullare con un riavvio un cambio di tariffa fatto da console.
- **La semina allo startup è solo di locale e test** (decisione 25, correzione in corsa della 12): in
  produzione è un **passo di distribuzione** (`seed-seat-pricing`, dopo il `migrate`), perché l'artefatto di
  spedizione deve arrivare in ascolto senza toccare la banca dati — è la regola che lo smoke di avvio
  verifica lanciandolo con una banca dati irraggiungibile di proposito. La prima stesura seminava in ogni
  profilo e avrebbe fatto fallire quello smoke.
- **Scritture del caricamento in SQL nativo** (decisione 10): all'avvio, fuori da una richiesta autenticata,
  il risolutore del tenant è fail-closed e nessuna sessione Hibernate può aprirsi. Stesso schema di
  `PricingSyncService`.
- **Deviazione consapevole dal piano di lavoro** (decisione 18): due conteggi separati per account invece di
  una unione in SQL nativo, per non passare a mano il perimetro dell'account.

## Invarianti appgrove

- **Tenant ID solo dal JWT verificato**: l'unica lettura legata a un account è il conteggio dei posti, che
  prende il perimetro dal claim `tenant_id` attraverso il discriminatore delle entità. Nessun identificativo
  di account arriva da parametro o da corpo della richiesta — e la scelta di *non* scrivere l'unione in SQL
  nativo esiste proprio per non doverlo passare a mano.
- **Filtro row-level `WHERE tenant_id`**: aggiunto da Hibernate sulle due entità già separate per account
  (appartenenze e inviti). Le tabelle del listino **non** portano `tenant_id` ed è dichiarato nella
  migrazione: il listino è di piattaforma, come il catalogo delle applicazioni. Un collaudo della guida di
  collaudo (§2) pretende l'assenza della colonna.
- **Modulo Terraform `microsaas_app`**: non toccato, nessuna applicazione nuova.
- **Logging strutturato**: la lettura del listino e la semina registrano una riga (`seat-pricing.read`,
  `seat-pricing.seed`) con il contesto propagato dai filtri esistenti.

## Note per il revisore

- **Contratto di rete additivo**: `GET /api/platform/v1/seat-pricing` è nuovo e nessun frontend lo consuma
  ancora. Lo spec OpenAPI committato di `core` è stato rigenerato dal build e contiene **solo** quella
  operazione in più; l'endpoint di collaudo `SeatProbeResource` è escluso dallo spec, come `MdcProbeResource`.
  Il frontend non genera tipi dallo spec di `core` (solo da `fatture` e `crm`), quindi nulla da rigenerare là.
- **Gate privacy (UC 0031)**: dieci segnali, nessun dato personale — tariffe, numeri d'ordine di posto,
  valuta, decorrenza, una nota, un lettore di file. Nessun manifesto aggiornato, nessuna annotazione
  `@PersonalData`, nessun responsabile esterno nuovo, nessun dato dell'art. 9. Classificazione: **MINORE**
  (nessuna variazione di finalità, base giuridica, categorie o conservazione dichiarate).
- **Gate parità dei modelli di scaffolding**: il rilevatore (strato 2,
  `node tools/scaffold-parity/source-paths-scan.mjs`) segnala **due percorsi-sorgente toccati** — i due
  workflow di consegna, per il passo `seed-seat-pricing`. Scelta presa: **opzione 2, motivazione registrata**
  in [docs/_PARITA-SCAFFOLD.md](../../docs/_PARITA-SCAFFOLD.md), perché il passo è di **piattaforma** e non
  per-servizio (il listino dei posti è uno per tutto il marketplace e lo semina `core`, come `sync-legal`):
  un'app generata non ne ha uno proprio e nessun modello replica quei workflow. Il collaudo di parità
  (strato 1, `parity-check.mjs`) resta **verde**, ed è quello che gira in `run-tests.sh`.
- **Copertura end-to-end**: `node tools/e2e-coverage/check.mjs` verde. 0102 passa da esente
  `non-implementato` a esente `senza-superficie`; nessun percorso Playwright nuovo o esteso, perché nulla di
  questa change è osservabile da un browser.
- **`run-tests.sh` non modificato** (nessun modulo aggiunto o rimosso, nessun comando di area cambiato) e
  **`_INDEX.md` non modificato** (le storie evolutive dell'epica 22 stanno fuori dall'ordine topologico —
  stessa scelta delle change 0091–0096).
- **Decisioni differite tracciate** (quattro, nessuna lasciata in conversazione):
  - in [UC 0103](../../docs/usecases/22-refactor-membership-model/story/0103-acquisto-anticipato-posto-invito.md):
    l'estensione della finalità di `membership.identity_id` nel manifesto dei dati quando il posto si paga
    davvero, e il **ritiro dell'endpoint di collaudo** `SeatProbeResource` quando la superficie vera esisterà;
  - in [UC 0102](../../docs/usecases/22-refactor-membership-model/story/0102-listino-posti-a-fasce.md):
    l'assenza di un'operazione di rete per il listino a una data passata (proprietario UC 0106) e la semina
    all'avvio invece che come passo di distribuzione.
- **Un difetto trovato e corretto negli aiutanti di collaudo**: `TestData.seatPricingBands` interrogava
  `ResultSet.wasNull()` dentro un inizializzatore di array, dove l'ordine di valutazione lo riferiva alla
  colonna sbagliata; il posto finale vuoto risultava `0` invece di «vuoto». Difetto dell'aiutante, non del
  prodotto — ma è il genere di errore che, non trovato, avrebbe reso il collaudo del caricamento cieco
  proprio sulla fascia aperta.

## Test

Area **`services/core`** — 5 file di collaudo nuovi, 42 casi:

| File | Che cosa copre |
|---|---|
| `SeatPricingTest` (30 casi, unità pura) | La tabella dello use case §4 su 17 numeri di posti (dovuto **e** costo del posto successivo); monotonia del dovuto su 0…150; discesa del costo marginale ai tre confini; franchigia come fascia a tariffa zero; fascia aperta su numeri grandi; **sette** forme di listino incoerente rifiutate con messaggio parlante; argomenti negativi. Le fasce arrivano dal file `pricing/seats.yaml`, non da tariffe ricopiate nel collaudo. |
| `SeatPricingLoaderTest` (3 casi, integrazione) | La prima versione nasce all'avvio e le sue fasce combaciano col file; il secondo e il terzo avvio non creano nulla; il file del prodotto è un listino coerente. |
| `SeatPricingApiTest` (4 casi, integrazione) | Il listino si legge con qualunque ruolo e non si legge senza token; una versione con decorrenza **futura** non è quella vigente (e l'operazione di prodotto non se ne accorge); nessuna versione vigente alla data richiesta = rifiuto esplicito. |
| `SeedSeatPricingCommandTest` (1 caso, command-mode) | Il passo di distribuzione `seed-seat-pricing` percorre argomento → caricamento → uscita e, a listino già presente, non riscrive nulla: è l'idempotenza che serve a un passo rieseguibile. |
| `SeatCountApiTest` (3 casi, integrazione) | Che cosa occupa un posto e che cosa no, stato per stato (owner, attiva, sospesa, rimossa; invito in attesa, scaduto, revocato, rifiutato, accettato); separazione fra account; il conteggio richiede un token con account. |

**Esito**: `./run-tests.sh` (suite **completa**, senza parametri) — **verde su tutte e otto le aree**:
backend, frontend, infra, compliance, tooling, smoke, platform, site. L'area `smoke` è quella che conta di
più in questa change: è il presidio che ha fatto emergere l'errore della prima stesura (semina allo startup
in ogni profilo) e la sua luce verde è la prova che la correzione era quella giusta.

Precisazione onesta sull'ordine dei fatti: la suite completa è girata sull'albero **finale del codice**. Dopo
di essa sono cambiati soltanto file di prosa e di registro — questa guida, questo log, `decisions.json` e
`docs/_PARITA-SCAFFOLD.md` — e i due controlli che li leggono sono stati rieseguiti a mano e sono verdi
(`parity-check.mjs`, `tools/e2e-coverage/check.mjs`). Nessuna riga eseguibile è stata toccata dopo il verde.

**Guida di collaudo**: [how-to-test.md](how-to-test.md) scritta **ed eseguita** nei passi non visivi (§1–§8,
§9.2, §9.3) sullo stack locale. Esito: **nessun difetto di prodotto**, **due correzioni alla guida** — un
presupposto non dichiarato sullo stato della banca dati al §7 (l'account Acme aveva più persone del seme
perché la suite di piattaforma era appena girata: il conteggio del prodotto era giusto, il numero atteso
nella guida no) e un sottocomando inesistente nel riavvio del §8. Resta allo sviluppatore il solo passo
visivo §9.1, che è una verifica di assenza. Dettaglio nell'intestazione della guida e nella decisione 26.

## Stato criteri di accettazione

- [x] Versioni immutabili con la loro decorrenza, e nessuna coppia di versioni con la stessa decorrenza
      (indice unico parziale).
- [x] Il calcolo restituisce i valori della tabella §4 per tutti i numeri di posti elencati, e zero per zero.
- [x] Il costo del posto successivo è esposto e scende ai tre confini di fascia.
- [x] Il dovuto è monotono crescente su 0…150.
- [x] La franchigia non è una condizione nel codice: è la prima fascia a tariffa zero.
- [x] La selezione per data ignora una versione futura e nega esplicitamente quando nessuna è vigente.
- [x] La prima versione nasce dal file al primo avvio e non si duplica.
- [x] Il conteggio dei posti comprende owner, attive, sospese e inviti in attesa non scaduti, ed esclude
      rimosse e inviti scaduti, revocati, rifiutati o accettati.
- [x] La lettura del listino risponde a qualunque autenticato e rifiuta l'anonimo.
- [x] `./run-tests.sh` completo — verde su tutte e otto le aree.
