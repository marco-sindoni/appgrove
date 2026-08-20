# Piano di lavoro — UC 0111 · Schermata «Gestione utenti» dentro ogni applicazione

**Storia**: [0111](../story/0111-schermata-gestione-utenti-app.md) · **Aree toccate**: `frontend/`, `services/crm`
**Dimensione stimata**: media-grande (comprende il ritiro dei posti del Mini-CRM) · **Prerequisiti**: UC 0098, UC 0107

## Passo 1 — Il componente condiviso

**File nuovo**: `frontend/apps/backoffice/src/shell/AppUsersScreen.tsx` — nella shell e non nel pacchetto dei
componenti, perché ha bisogno del client di rete e del contesto della shell, che il pacchetto grafico non
conosce (scelta raccomandata; l'alternativa è un componente puro nel pacchetto con i dati iniettati, più
riusabile e più laborioso).

Riceve: identificativo dell'applicazione, ruolo di chi guarda, testo di spiegazione dei ruoli per **quella**
applicazione. Rende: intestazione, comando «Aggiungi utente» nei suoi tre stati (attivo · disabilitato con
spiegazione · assente), tabella con l'owner in testa e non modificabile, selettore di ruolo e comando di
rimozione con conferma.

**File nuovo**: `frontend/apps/backoffice/src/api/appAccessHooks.ts` — letture e scritture verso le operazioni
di UC 0098, con invalidazione dopo ogni modifica.

## Passo 2 — Innesto nei due moduli esistenti

**Modifiche**:

- `frontend/apps/backoffice/src/modules/crm/manifest.ts` e
  `frontend/apps/backoffice/src/modules/fatture/manifest.ts` — voce «Utenti» fra le sezioni, con la sua chiave
  di traduzione e la sua icona.
- Le rotte interne dei due moduli montano `AppUsersScreen` con l'identificativo dell'applicazione.
- `frontend/apps/backoffice/src/modules/crm/screens/MembersScreen.tsx` — **eliminato**, sostituito dal
  componente condiviso. Va eliminato, non lasciato accanto: due schermate della stessa cosa nello stesso
  prodotto sono un difetto in attesa.

## Passo 3 — Ritiro dei posti del Mini-CRM

Nell'ordine, in un'unica change:

1. `services/crm/` — rimozione di `Seat.java`, `SeatRepository.java`, `SeatResource.java`,
   `SeatUsagePublisher.java`, `SeatAccess.java` (già sostituito dal varco condiviso in UC 0099);
2. migrazione dello schema del Mini-CRM che dismette `app_crm.seat` — **solo dopo** che UC 0113 ne ha
   travasato il contenuto in `platform.app_access`. Se le due change non sono contigue, questo passo va
   rinviato a UC 0113 e va scritto qui;
3. `CrmQuotaService.java` e il listino `pricing/crm.yaml` — la quota `seats` perde significato. Decidere con
   chi cura i prezzi: togliere il limite o sostituirlo con un limite sui contatti. **Non** lasciare una
   metrica che conta una tabella che non esiste più;
4. `frontend/.../modules/crm/api/hooks.ts` — rimozione delle letture e scritture dei posti; verificare che
   nessun riferimento resti con `grep -rn "useSeats\|assignSeat\|revokeSeat" frontend/`.

## Passo 4 — Il testo dei ruoli per applicazione

Il testo di spiegazione arriva dal documento delle operazioni dell'applicazione (UC 0101), esposto da una
piccola lettura del servizio dell'applicazione oppure — più semplice — scritto nelle traduzioni del modulo
con una chiave per ruolo. La seconda via è preferibile: sono tre frasi per applicazione e non vale una
chiamata di rete.

## Passo 5 — Collaudi

- `AppUsersScreen.test.tsx`: tabella dei quattro ruoli (comando attivo, disabilitato, assente; selettore
  abilitato o no; owner non modificabile).
- Integrazione nel core: già coperta da UC 0098; qui si aggiunge il caso «`admin` di un'altra applicazione».
- Non-regressione del Mini-CRM: la sua suite resta verde senza i posti; nessun riferimento residuo.
- Percorso di piattaforma: è il cuore di `J-ROLES` (UC 0113).

## Verifica finale

```bash
cd services && mvn -B test
cd ../frontend && npm run typecheck && npm test
cd .. && ./run-tests.sh backend frontend
```

## Trappole note

1. **Il doppio conteggio dei posti** è il rischio numero uno: se `app_crm.seat` sopravvive accanto a
   `platform.app_access`, i due numeri divergono e il cliente può pagare due volte lo stesso concetto.
2. **La quota `seats` del listino** non va dimenticata: una metrica che conta una tabella dismessa restituisce
   zero, e un tetto su zero non blocca mai nulla — un varco silenzioso.
3. **L'owner in testa**: non ha righe di accesso, va aggiunto dal codice. Un elenco che non lo mostra fa
   sembrare che il titolare non abbia accesso alla propria applicazione.
