# Log di implementazione — change 0098 (UC 0103, modalità fast)

**Branch**: `change/0098-use-case-0103-acquisto-anticipato-posto` · **Base**: `203900b`
**Aree toccate**: `services/core`, `frontend/`, documentazione (use case, copertura end-to-end, manifesto
dati, revisione legale)

## Che cosa è stato fatto, in ordine

### 1. La voce di catalogo di piattaforma (`V22__platform_seat_subscription.sql`)

Una colonna **`kind`** su `platform.app` (`application` | `platform`, predefinito `application`) e **una
riga**: `platform-seats`, con identificativo deterministico dallo slug. Più `quantity` su
`platform.subscription` (predefinito 1) e `seat_charge_ref` su `platform.invitations`.

Un **attributo** e non un elenco di slug: è la forma che la change 0092 aveva indicato lasciando il
rimando. Con la colonna, la seconda voce di piattaforma di domani è coperta per costruzione.

### 2. Le esclusioni — sei, non cinque

Il piano di lavoro ne elencava cinque. Eseguendo il lavoro sono diventate sei, e due sono di
**correttezza**, non di presentazione:

| Dove | Che cosa accade | Perché |
|---|---|---|
| `EntitlementReadModel` | esclusa | esclusione a monte: da qui discende il menu laterale |
| `CatalogReadModel` | esclusa | e **prima** della condizione sull'abbonamento, che da sola non basterebbe |
| `MeAppAccessResource` | esclusa per costruzione | consuma la lettura di sopra; l'esclusione è provata **qui**, perché è qui che il difetto si vedrebbe |
| `UserResource` | esclusa | altrimenti «l'owner è abilitato ai Posti dell'account» |
| `SubscriptionReadModel` | esclusa (**aggiunta**) | offriva cambio fascia, disdetta e riattivazione sui posti: tre comandi che avrebbero fatto la cosa sbagliata |
| `TenantOffboarding` + `GdprExportService` | escluse (**aggiunte**) | derivano «a quali servizi chiedere i dati»: senza il filtro, export e purga attendevano un servizio `platform-seats` che non esiste |
| `AdminResource` | **visibile, marcata** + esclusa dalla matrice | chi amministra deve constatare che esiste, ma non è qualcosa che un cliente «vede» |

### 3. Il fornitore di pagamento

Due metodi nuovi sul port: `chargeSeats` e `releaseSeatCharge`. `chargeSeats` è l'unico metodo del port
che risponde **subito** sì o no — tutti gli altri delegano l'effetto al webhook — perché l'invito non deve
nascere senza addebito riuscito. Un rifiuto è un **esito**, non un'eccezione: la firma restituisce il
motivo del fornitore. Il simulatore accetta; rifiuta se glielo si chiede da configurazione. Il fornitore
reale resta il segnaposto che fallisce dichiarando la ragione (prerequisito #14).

### 4. Il servizio dei posti

`SeatSubscriptionService` fa due cose: il **riquadro** (tutti i numeri, compreso il giudizio «il prossimo
costa meno del precedente») e l'**acquisto del posto in più** (calcola, addebita, aggiorna l'abbonamento).
`SeatSubscriptionWriter` scrive la riga in SQL nativo, come il writer degli abbonamenti.

`quantity` è un **high-water mark del periodo** e in questa storia sale soltanto. Da questa sola proprietà
derivano due casi dello use case senza una riga di codice per ciascuno: nessun rimborso sul posto liberato,
e nessun secondo addebito per un posto già pagato nel periodo.

`SeatCount` espone anche la **composizione** dei posti, ricavando le attive per differenza invece di
elencare gli stati che valgono: così lo stato «in cessazione» di UC 0104 non farà sparire posti dal conto.

### 5. L'invito che passa dalla cassa

`InvitationResource.create` è una **sequenza numerata**: blocco dell'account → stato dell'account →
riduzione in attesa (commento: il gate nasce con UC 0104) → indirizzo duplicato → addebito → creazione →
compensazione se la creazione fallisce dopo un addebito riuscito.

Il blocco pessimistico sulla riga dell'account chiude il rischio vero: non un tetto di posti sforato (non
esiste) ma **lo stesso salto di fascia addebitato due volte**.

### 6. Il riquadro dei posti nella schermata

`SeatsCard` più la stima nel modulo di invito. Nessuna aritmetica nell'interfaccia. Il pulsante di invito
resta **spento** finché il costo non è noto. Cinque lingue, plurali compresi, e il caso «costa meno» come
frase intera.

Il contratto generato descrive ogni campo come facoltativo, quindi si converte una volta all'ingresso in
una forma stretta (`readSeats`): risposta completa → nessun valore incerto; risposta incompleta → **errore**
del riquadro, non uno zero.

### 7. Un difetto trovato e chiuso strada facendo

Il rifiuto `402` dell'addebito alzava il **banner globale di enforcement**, che avrebbe detto «il tuo
abbonamento è scaduto, riattivalo o esporta i dati» a un account con l'abbonamento attivo — nascondendo la
cosa vera da fare. Ora è escluso, e il rifiuto si legge dove è avvenuto.

### 8. Privacy

Finalità di `membership.identity_id` estesa nelle due lingue (il numero delle persone determina un
importo), registro dei trattamenti rigenerato, classificazione **MINORE** argomentata, voce **L18** nella
revisione legale. `invitations.seat_charge_ref` dichiarato **per prudenza**: la scelta ha avuto un effetto
immediato e utile, perché il collaudo di contratto GDPR è diventato rosso finché il campo non è entrato
nell'**esportazione**.

### 9. Debito ritirato

L'endpoint di collaudo del **conteggio** dei posti è stato ritirato e il suo collaudo agganciato
all'operazione vera. Del probe resta la selezione della versione **per data**, che non ha ancora una
superficie di prodotto (rimando in UC 0106).

## Collaudi

**Backend** (`services/core`, 414 → 414 collaudi verdi; le classi nuove sono quattro):

- `SeatPurchaseApiTest` — franchigia senza abbonamento; quarto posto → quantità 1 e dovuto 299; quinto →
  quantità 2 e dovuto 598; posto liberato non ripagato (scadenza **e** revoca); due inviti simultanei con
  un solo salto di fascia addebitato; account in eliminazione che non invita; confine di fascia in cui il
  prossimo costa meno e il totale sale;
- `SeatChargeDeclinedApiTest` (profilo dedicato) — addebito rifiutato → `402` con il motivo, nessun invito,
  nessun abbonamento; e la franchigia che passa **anche** col simulatore che rifiuta, cioè la prova che la
  franchigia è l'assenza di un addebito e non un addebito da zero euro;
- `SeatChargeCompensationTest` — addebito riuscito + creazione fallita → storno chiesto al fornitore e
  banca dati pulita;
- `PlatformSeatsExclusionTest` — sei prove di esclusione, con l'abbonamento dei posti **davvero** presente.

**Interfaccia**: `MembersPage.test.tsx` (otto prove nuove sul riquadro, la stima, il rifiuto, l'invito
impedito), `seats.test.ts` (la conversione: lo zero legittimo è zero, l'assenza è errore),
`enforcement.test.ts` (il banner che non deve alzarsi).

**Percorso end-to-end**: `J-SEATS`, livello 2, `frontend/apps/backoffice/e2e/seats.spec.ts` — due prove: dal
riquadro al primo posto a pagamento, e l'addebito rifiutato che non crea l'invito. Registro di copertura
aggiornato: 0103 esce dall'esenzione ed entra fra le superfici.

**Suite completa** `./run-tests.sh` (senza parametri): **verde** su tutte le aree — backend, frontend,
infra, compliance, tooling, smoke, platform, site.

**Guida di collaudo**: `how-to-test.md`, con i passi non visivi **eseguiti** contro lo stack locale prima
del commit. Nessun difetto di prodotto; un punto aperto scoperto (gli account preesistenti hanno persone
non pagate) e tracciato in UC 0113.

## Che cosa resta indietro, e dove è scritto

| Rimando | Dove |
|---|---|
| Chiamata al fornitore **dentro** la transazione: deroga da chiudere prima del fornitore vero | UC 0103, punti aperti |
| Rinnovo del periodo dei posti (la quantità non torna giù a fine periodo) | UC 0106, punti aperti |
| Discesa della quantità dopo una riduzione; gate «riduzione in attesa» col suo punto esatto | UC 0104, punti aperti |
| Percorso `J-SEATS` a livello di **piattaforma** (stack vero) | UC 0113, punti aperti |
| Migrazione degli account preesistenti con persone non pagate (decisione **commerciale**) | UC 0113, punti aperti |
| Probe della selezione del listino per data, da ritirare quando esisterà la superficie | UC 0106, punti aperti |
| Tre punti da far confermare a un legale (testo pubblico, prudenza del riferimento all'addebito, assenza di rimborso verso consumatori) | `docs/_REVISIONE-LEGALE.md`, voce L18 |

## Definition of Done

| Voce | Esito |
|---|---|
| Il posto si paga prima che l'invito parta; senza pagamento l'invito non esiste | ✅ provato in tre modi (collaudo, profilo che rifiuta, guida eseguita) |
| L'abbonamento di piattaforma esiste con la sua quantità e riusa l'impianto di pagamento | ✅ |
| La voce di piattaforma è invisibile in tutte le superfici del cliente, provata una per una | ✅ sei prove |
| Il riquadro mostra usati, importo e costo del prossimo, col caso in cui scende | ✅ |
| Manifesto dati e registro dei trattamenti aggiornati | ✅ |
| Registro di copertura end-to-end coerente | ✅ `node tools/e2e-coverage/check.mjs` verde |
| `./run-tests.sh` completo verde | ✅ |
| `how-to-test.md` scritta **ed eseguita** nei passi non visivi | ✅ |
| `run-tests.sh` aggiornato | non necessario: nessun modulo aggiunto, nessun comando di area cambiato (decisione 29) |
| `_INDEX.md` sincronizzato | non applicabile alle storie evo (decisione 3); lo stato è nel drill-down di 0103 |
