# Piano di lavoro — UC 0102 · Listino dei posti a fasce

**Storia**: [0102](../story/0102-listino-posti-a-fasce.md) · **Area toccata**: `services/core`
**Dimensione stimata**: piccola-media · **Prerequisito**: UC 0098

## Passo 1 — Migrazione

**File nuovo**: `services/core/src/main/resources/db/migration/V19__seat_pricing.sql` (numero da
riverificare; UC 0098 usa il precedente).

Due tabelle: `platform.seat_pricing_version` (identificativo, decorrenza, valuta, nota, autore, campi di
audit) e `platform.seat_pricing_band` (versione, posto iniziale, posto finale annullabile, tariffa in
centesimi). Nessuna colonna `tenant_id`: il listino è di piattaforma, come il catalogo delle applicazioni.

Indice sulla decorrenza, per la ricerca «versione vigente a questa data».

## Passo 2 — Entità, repository e caricamento iniziale

**File nuovi** in `services/core/src/main/java/app/appgrove/core/billing/seats/`:

- `SeatPricingVersion.java`, `SeatPricingBand.java` — entità che estendono `BaseEntity` (**non**
  `BaseTenantEntity`: non sono separate per account).
- `SeatPricingRepository.java` — con `findVigenteAl(Instant)`, che è la sola via di lettura ammessa: mai
  «prendi l'ultima».
- `SeatPricingLoader.java` — crea la versione iniziale dal file di risorse se la tabella è vuota, sul modello
  di
  [PricingCatalogLoader.java](../../../../services/core/src/main/java/app/appgrove/core/catalog/PricingCatalogLoader.java).
  **Idempotente**: al secondo avvio non deve creare nulla.

**File nuovo di risorse**: `services/core/src/main/resources/pricing/seats.yaml` — le fasce iniziali. Va
**fuori** da `pricing/index.yaml`, che elenca le applicazioni: i posti non sono un'applicazione e mescolarli
là confonderebbe la sincronizzazione dei listini.

## Passo 3 — Il calcolo

**File nuovo**: `SeatPricing.java` — classe senza stato, con due metodi puri:

- `bandFor(int posti, SeatPricingVersion)` → la fascia;
- `dueCents(int posti, SeatPricingVersion)` → il dovuto in centesimi.

La franchigia **non** è un caso speciale nel codice: è la prima fascia a tariffa zero da 1 a 3. Questo è il
punto in cui il codice resta semplice o diventa una selva di condizioni.

## Passo 4 — Che cosa occupa un posto

**File nuovo**: `SeatCount.java` — con `countFor(tenant)` che somma: owner, persone attive, persone sospese,
inviti in attesa non scaduti, persone indicate per la cessazione. Una sola interrogazione con unione, non
cinque. **Una** definizione, usata da tutti: se il conteggio vive in due posti, i due divergeranno.

## Passo 5 — Lettura di rete

**File nuovo**: `SeatPricingResource.java` — `GET /api/platform/v1/seat-pricing` con la versione vigente
(fasce e tariffe), aperta a ogni autenticato. Il calcolo del **proprio** dovuto sta in UC 0103, con il
riquadro dei posti.

## Passo 6 — Collaudi

- `SeatPricingTest.java` — **tabellare**, con tutti i casi della storia §4, compresi i tre confini in cui il
  dovuto scende. Scriverlo come tabella di casi, non come dodici metodi: si leggerà come una specifica.
- `SeatCountTest.java` — che cosa occupa posto e che cosa no, un caso per stato.
- `SeatPricingLoaderTest.java` — creazione al primo avvio, nessuna duplicazione al secondo.
- Selezione per data con una versione futura presente.

## Verifica finale

```bash
cd services && mvn -B -pl core -am test
cd .. && ./run-tests.sh backend
```

## Trappole note

1. **Non presumere che il dovuto cresca al crescere dei posti.** Un collaudo scritto con quella convinzione
   diventerà rosso ai confini, e qualcuno «correggerà» il listino invece del collaudo.
2. **Centesimi interi**, mai virgola mobile sul denaro.
3. **Nessuna fascia scritta nel codice**: le fasce stanno in banca dati e il file serve solo al primo
   popolamento. Cablarle nel codice «per i collaudi» le farebbe divergere il giorno del primo cambio.
