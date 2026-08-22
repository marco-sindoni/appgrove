# Parità dei modelli-sorgente — deviazioni consapevoli (documento vivo)

Verbale delle **deviazioni volute** fra i modelli-sorgente della skill `new-application`
(`tools/new-application/templates/`) e l'app #1 `fatture`, da cui quei modelli derivano.

**A cosa serve.** La skill `new-application` genera ogni app nuova copiando i modelli-sorgente. Il rischio
non è che si rompano — è che **invecchino in silenzio**: `fatture` evolve, i modelli restano indietro, e le
app nuove nascono già antiquate senza che nulla diventi rosso. Il presidio è a tre strati:

1. **collaudo di parità** (automatico) — `tools/scaffold-parity/parity-check.mjs` confronta strutturalmente
   modelli e app #1: stesso insieme di file, stesse dipendenze Maven, stesse chiavi di
   `application.properties`, stesse annotazioni portanti. Diventa rosso da solo alla prima divergenza;
2. **rilevatore dei percorsi-sorgente** (automatico) — `tools/scaffold-parity/source-paths-scan.mjs` segnala
   quando una change tocca un percorso da cui i modelli derivano; il varco di `new-change` (step-04) obbliga
   allora a scegliere fra aggiornare i modelli e registrare qui la motivazione;
3. **questo file** (umano) — ciò che i primi due strati **non possono esprimere**: perché una divergenza è
   voluta, e va lasciata dov'è.

Questo registro è il **verbale**, non la guardia: non blocca nulla da solo, ma è l'unico posto dove una
divergenza può essere legittimata. Ogni riga della tabella qui sotto **silenzia** la corrispondente
segnalazione del collaudo di parità: è una firma, non un silenziamento distratto.

**Quando si scrive dentro.**

- quando il collaudo di parità segnala una divergenza che **non va sanata**, perché la novità di `fatture`
  è specifica del suo dominio e non va generalizzata a tutte le app;
- quando una change tocca un percorso-sorgente e si decide, motivandolo, di **non** aggiornare i modelli
  nello stesso commit (rimandare è lecito; rimandare in silenzio no);
- quando i modelli restano deliberatamente indietro in attesa di qualcosa (un caso d'uso non ancora
  implementato, una decisione non ancora matura): in quel caso indicare **cosa** sblocca il riallineamento.

**Chi lo legge.**

- la skill **`new-application`**, *prima* di generare: le dice che cosa i modelli consapevolmente non portano;
- chi tocca un **percorso-sorgente** (`tools/scaffold-parity/source-paths.json`) durante una change;
- chi si trova davanti un collaudo di parità rosso e deve capire se è una svista o una scelta già presa.

## Come si registra una deviazione

Aggiungere una riga alla tabella **fra i due marcatori** qui sotto (il collaudo legge solo lì: fuori dai
marcatori questo file è prosa libera e non ha effetto). La chiave va scritta fra apici inversi ed è quella
che il collaudo stampa già pronta da copiare:

| Chiave | Silenzia |
|---|---|
| `file:<percorso nell'app #1>` | un file dell'app #1 che il modello non porta |
| `file-extra:<percorso nel modello>` | un file del modello che l'app #1 non ha |
| `dep:<groupId>:<artifactId>` | una dipendenza Maven presente da un lato solo |
| `prop:<chiave>` | una chiave di `application.properties` presente da un lato solo |
| `ann:<percorso nell'app #1>#<Annotazione>` | un'annotazione portante presente da un lato solo |

Una deviazione **senza data e senza motivazione leggibile non è una deviazione, è una svista firmata**: le
righe vanno tenute vive e rimosse quando il motivo decade.

## Deviazioni attive

<!-- deviazioni:inizio -->

| Chiave | Perché la divergenza è voluta | Cosa la chiuderebbe | Dal |
|---|---|---|---|
| `file:services/fatture/src/main/resources/META-INF/openapi/openapi.json` | Istantanea dello schema delle API **prodotta dalla compilazione**, non scritta a mano: `application.properties` contiene `quarkus.smallrye-openapi.store-schema-directory=src/main/resources/META-INF/openapi`, quindi il file nasce dalla prima compilazione dell'app. È versionato perché serve a generare il client tipizzato del frontend e a confrontare le versioni delle API, ma copiarlo nel modello significherebbe consegnare a ogni app nuova lo schema **delle fatture** — cioè un file sbagliato che sembra giusto. | Nulla: la divergenza è strutturale e va lasciata. Andrebbe rimossa solo se lo schema smettesse di essere generato dalla compilazione. | 2026-07-19 |
| `file:services/fatture/src/main/resources/META-INF/openapi/openapi.yaml` | Come sopra: stessa istantanea, altro formato. | Come sopra. | 2026-07-19 |
| `file:tools/smoke/stack-headless.sh` | Change `0069` (UC 0090): i passi comuni dell'avvio headless sono stati estratti in `dev/lib/headless.sh`, condivisi con la suite e2e di piattaforma (`tools/platform-e2e`). Nessun modello in `tools/new-application/templates/` replica questo file: lo smoke resta **guidato dalla scoperta servizi** (`discover_services`), quindi un'app generata vi entra da sola, senza che il generatore debba produrre alcunché. | Nulla da riallineare nei modelli: la deroga documenta che il rifattorizzamento non cambia il contratto verso le app generate. Da rivedere solo se un modello iniziasse a replicare lo script. | 2026-08-01 |
| `file:.github/workflows/verify-pr.yml` | Change `0069` (UC 0090): aggiunto il job `platform` (suite e2e di piattaforma, path-filtered e non bloccante). I path-filter usano pattern (`services/**`, `frontend/**`) che includono da soli ogni app generata; nessun modello replica il workflow. **Change `0075`**: aggiunto al job `frontend` il passo di controllo dei tipi (`npm run typecheck`). Il comando lavora su tutti gli spazi di lavoro npm (`--workspaces --if-present`), quindi un modulo frontend generato vi entra da solo: nessun frammento per-app da produrre, nessun modello da aggiornare. | Nulla: il pattern del workflow resta neutro rispetto al numero di app. Da rivedere solo se un modello iniziasse a generare frammenti di workflow per-app. | 2026-08-01 |
| `file:tools/platform-e2e/journeys/J-QUOTA.spec.ts` | Change `0077` (UC 0096): il journey asseriva l'intestazione «Get an app» della pagina Billing, che non esiste più — Billing è ora di sola fatturazione e l'asserzione è diventata «Billing». È un adeguamento a un **testo dell'interfaccia di piattaforma**, e il modello del journey è scritto apposta per **non** dipendere dai testi (lo dice a chiare lettere il suo commento: le asserzioni a schermo usano l'identificativo di prova del modulo, il ruolo del banner e il codice del record). Non c'è quindi nulla da portare nel modello: il controllo di contenuto della coppia sono i moduli importati, che non cambiano. | Nulla da riallineare. Da rivedere solo se il modello iniziasse ad asserire testi dell'interfaccia di piattaforma — cosa che dovrebbe comunque evitare. | 2026-08-01 |
| `file:tools/platform-e2e/helpers/browser.ts` | Change `0090` (UC 0118): aggiunti tre passi condivisi — `expectInsideAccount`, `loginIntoAccount`, `switchAccountTo` — che attendono la risposta che *decide* il gate legale e insistono sull'esito. Servono ai percorsi che entrano in **più di un account** (`J-ACCOUNT-SWITCH`, `J-INVITE-EXISTING`): sono quelli che incontrano il gate più volte e pagano la corsa del suo fail-open. **Aggiunta puramente additiva**: nessuna firma esistente cambia, e `browserLogin`/`acceptLegalGateIfPresent` — i soli passi che il modello del journey importa — restano identici. Il journey core-loop di un'app generata entra in **un** account, **una** volta: usare i passi nuovi lì significherebbe portare complessità che quel percorso non ha. | Aggiornare il modello quando (e se) il journey core-loop dovrà entrare in più di un account, oppure quando `browserLogin` verrà reso robusto a sua volta — lavoro tracciato in [docs/_BACKLOG.md](_BACKLOG.md), owner #10 con UC 0091/0092: allora il modello erediterà la correzione senza toccare nulla. | 2026-08-21 |
| `file:.github/workflows/deploy-test.yml` | Change `0097` (UC 0102): aggiunto il passo `oneshot test platform <tag> seed-seat-pricing`, che semina la prima versione del **listino dei posti** dopo il `migrate`. È un passo di **piattaforma**, non per-servizio: il listino dei posti è uno per tutto il marketplace e lo semina `core`, esattamente come `sync-legal`. Un'app generata non ne ha uno proprio e non deve produrre alcun frammento; nessun modello in `tools/new-application/templates/` replica questo workflow. | Nulla da riallineare: il passo resta neutro rispetto al numero di app. Da rivedere solo se un modello iniziasse a generare frammenti di workflow per-app, oppure se un giorno esistesse un listino di posti **per applicazione** — cosa che l'epica 22 esclude. | 2026-08-22 |
| `file:.github/workflows/release-prod.yml` | Come sopra, per la produzione: `oneshot prod platform <tag> seed-seat-pricing` dopo il `migrate`. | Come sopra. | 2026-08-22 |

<!-- deviazioni:fine -->

**Una sola deviazione, in due righe, a oggi (2026-07-19)** — ed è di natura tecnica, non di dominio: i
modelli-sorgente nascono in questa stessa change (UC 0046) come gemelli fedeli di `fatture`, quindi non
esiste ancora nessuna scelta di prodotto che li tenga indietro di proposito. Una tabella corta significa
"parità piena e voluta", ed è lo stato normale a cui tornare.

## Le coppie confrontate

| Coppia | Modello | Gemello nell'app #1 | Controlli |
|---|---|---|---|
| `backend` | `templates/service` | `services/fatture` | insieme dei file, dipendenze Maven, chiavi di `application.properties`, annotazioni portanti |
| `frontend` | `templates/frontend-module` | `frontend/apps/backoffice/src/modules/fatture` | insieme dei file |
| `platform-e2e` | `templates/platform-e2e` | `tools/platform-e2e/journeys/J-QUOTA.spec.ts` | insieme dei file, moduli importati |

La coppia `platform-e2e` (change `0074`, UC 0094) sorveglia il **journey core-loop** che ogni app nuova
eredita. Due particolarità, entrambe volute:

- il confronto è ristretto al solo `J-QUOTA.spec.ts` (`soloFile` in `parity.config.json`): la cartella dei
  journey ne contiene una dozzina, e gli altri non hanno nulla a che vedere con lo scaffolding —
  pretenderli nel modello produrrebbe divergenze inventate;
- il controllo di contenuto sono i **moduli importati**, non il corpo del test. Il corpo *deve* divergere
  (domini diversi, asserzioni diverse); ma se `J-QUOTA` comincia a usare un helper nuovo della suite e il
  modello no, ogni app nuova nascerà con un journey scritto col vocabolario di ieri. È lo stesso ruolo che
  ha il confronto delle dipendenze del `pom.xml` per il servizio.

La corrispondenza di nome `J-FATTURE ↔ J-QUOTA` sta fra i `dominio` della configurazione: il journey
core-loop dell'app #1 porta il nome di ciò che dimostra (la quota), non quello dell'app.

## Come si usano gli strumenti

Tutti i comandi si lanciano dalla radice del monorepo:

```bash
# strato 1 — collaudo di parità modelli ↔ app #1
npm run parity --prefix tools/scaffold-parity

# strato 2 — percorsi-sorgente toccati dalla change corrente (o da un range git, o da percorsi espliciti)
npm run source-paths --prefix tools/scaffold-parity
node tools/scaffold-parity/source-paths-scan.mjs main...HEAD
node tools/scaffold-parity/source-paths-scan.mjs --paths services/fatture/pom.xml

# test degli strumenti stessi
npm test --prefix tools/scaffold-parity
```

Il collaudo di parità e i test degli strumenti girano nell'area **`tooling`** di
[run-tests.sh](../run-tests.sh); il rilevatore dei percorsi-sorgente è invocato dal varco di `new-change`
(step-04) sul diff della change.

## Quando il collaudo dice "modelli-sorgente non ancora presenti"

Esce con codice 2 e non si limita a passare: una parità mai verificata non è una parità. Se compare quel
messaggio, o i modelli non sono ancora stati creati, o sono stati spostati e va aggiornato `templatesRoot`
in `tools/scaffold-parity/parity.config.json`.
