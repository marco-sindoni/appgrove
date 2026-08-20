# Piano di lavoro — UC 0105 · Governo del listino dalla console di piattaforma

**Storia**: [0105](../story/0105-governo-listino-console-piattaforma.md) · **Aree toccate**: `services/core`, `frontend/apps/admin`
**Dimensione stimata**: media · **Prerequisito**: UC 0102

## Passo 1 — Interfaccia di amministrazione

**File nuovo**: `services/core/src/main/java/app/appgrove/core/platform/AdminSeatPricingResource.java`
(percorso `/api/admin/v1/seat-pricing`), protetto dal solo ruolo di amministratore di piattaforma, come le
altre operazioni della console.

Operazioni: elenco delle versioni (storico), lettura di una versione, **creazione** di una nuova versione,
annullamento di una versione futura non ancora decorsa, e **anteprima dell'effetto**.

## Passo 2 — Validazione e immutabilità

**File nuovo**: `SeatPricingValidator.java` — funzione pura: fasce contigue, nessuna sovrapposizione, nessun
buco, prima fascia che parte da 1, ultima senza limite superiore, tariffe non negative, valuta unica. Gli
errori nominano la fascia colpevole, altrimenti chi sbaglia non capisce cosa correggere.

**Immutabilità**: nessuna via di scrittura raggiunge una versione con decorrenza passata. Il presidio va nel
servizio **e** provato: un collaudo che tenta la modifica e pretende il rifiuto.

**Margine di decorrenza**: rifiuto se la decorrenza è anteriore al margine dichiarato (proposta trenta
giorni), configurabile in `application.properties` — non cablato, perché è una regola commerciale.

## Passo 3 — L'anteprima dell'effetto

**File nuovo**: `SeatPricingImpactService.java`:

- **casi tipici**: dovuto per 3, 4, 8, 12, 55, 120 posti con listino attuale e nuovo, affiancati;
- **portafoglio reale**: quanti account cambiano importo, somma degli importi prima e dopo, rincaro massimo
  su un singolo account, elenco degli account che subiscono un rincaro (identificativi e nomi di account,
  **non** persone).

L'interrogazione aggrega sul conteggio dei posti per account (riuso di `SeatCount` di UC 0102). Su portafogli
grandi va calcolata in una sola interrogazione, non per account.

## Passo 4 — Sincronizzazione col fornitore di pagamento

**Modifica**: il servizio di sincronizzazione dei listini
([PricingSyncService.java](../../../../services/core/src/main/java/app/appgrove/core/catalog/PricingSyncService.java))
o un gemello dedicato ai posti: la nuova versione crea **prezzi nuovi** presso il fornitore, mai modifica di
esistenti (regola già in vigore). Se la sincronizzazione fallisce, la versione resta creata e **non decorre**:
serve una colonna di stato (`draft` · `synced` · `active`), e la selezione della versione vigente considera
solo quelle sincronizzate.

## Passo 5 — La schermata della console

**File nuovo**: `frontend/apps/admin/src/pages/SeatPricing.tsx`, registrato nella navigazione della console.
Tre blocchi come da storia §6: vigente, nuova versione (con validazione mentre si scrive e anteprima),
storico. Doppia conferma con riepilogo dell'effetto.

Il prototipo [platform-admin.html](../prototype/platform-admin.html) è la specifica illustrata di questa
schermata: leggerlo prima di scrivere, e leggere la tabella di mappatura del suo README.

## Passo 6 — Collaudi

- `SeatPricingValidatorTest.java`: ogni forma invalida, con il messaggio giusto.
- `AdminSeatPricingApiTest.java`: creazione, rifiuto della decorrenza troppo vicina, rifiuto della modifica di
  una versione decorsa, annullamento di una futura.
- Selezione della versione vigente con una futura presente e con una non sincronizzata.
- `SeatPricingImpactServiceTest.java`: numeri dell'anteprima su un piccolo portafoglio simulato.
- Prova di sicurezza: un owner di un account non raggiunge queste operazioni.
- `frontend/apps/admin`: componente della schermata, e percorso end-to-end se la console ne ha già (verificare
  `frontend/apps/admin/e2e/`).

## Verifica finale

```bash
cd services && mvn -B -pl core -am test
cd ../frontend && npm run typecheck && npm test
cd .. && ./run-tests.sh backend frontend
```

## Trappole note

1. **La tentazione di modificare la versione vigente** «tanto è un errore di battitura»: è la porta da cui
   entra l'impossibilità di ricostruire quanto pagava un cliente. Correzione = versione nuova.
2. **L'anteprima sul portafoglio reale** va calcolata in una interrogazione: farla per account la rende
   inutilizzabile appena i clienti crescono.
3. **Nessun dato personale nell'anteprima**: mostrare i nomi degli account, non delle persone.
