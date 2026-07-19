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

<!-- deviazioni:fine -->

**Una sola deviazione, in due righe, a oggi (2026-07-19)** — ed è di natura tecnica, non di dominio: i
modelli-sorgente nascono in questa stessa change (UC 0046) come gemelli fedeli di `fatture`, quindi non
esiste ancora nessuna scelta di prodotto che li tenga indietro di proposito. Una tabella corta significa
"parità piena e voluta", ed è lo stato normale a cui tornare.

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
