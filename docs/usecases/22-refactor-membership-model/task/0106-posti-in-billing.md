# Piano di lavoro — UC 0106 · I posti nella sezione «Billing»

**Storia**: [0106](../story/0106-posti-in-billing.md) · **Aree toccate**: `frontend/`, `services/core`
**Dimensione stimata**: piccola-media · **Prerequisiti**: UC 0103, UC 0105

## Passo 1 — Le letture della fatturazione includono i posti

**Modifiche** in `services/core/src/main/java/app/appgrove/core/billing/`:

- `SubscriptionReadModel.java` — l'abbonamento dei posti va incluso, con il suo contrassegno di voce di
  piattaforma, la quantità e i dati di calcolo (posti, fascia, tariffa). Verificare ogni punto che presume
  «un abbonamento appartiene a una applicazione»: è la controparte della scelta strutturale di UC 0103 e va
  cercata con `grep -n "appSlug\|appId" SubscriptionReadModel.java PaymentReadModel.java`.
- `PaymentReadModel.java` — le transazioni dei posti sono distinguibili nello storico.

## Passo 2 — La scheda dei posti

**File nuovo**: `frontend/apps/backoffice/src/billing/SeatsCard.tsx` — in testa alla pagina, sopra la tabella
degli abbonamenti, perché riguarda tutto l'account.

Contenuto: conteggio con composizione, scaglioni, **calcolo in una riga** (`7 × 2,99 + 2 × 1,99 = 24,91 €`),
prossimo rinnovo con data e importo, eventuale riduzione in attesa, eventuale listino futuro programmato,
collegamento a «Members».

**Modifica**: [SubscriptionsPanel.tsx](../../../../frontend/apps/backoffice/src/billing/SubscriptionsPanel.tsx)
— la nuova scheda si inserisce sopra, e la tabella degli abbonamenti **esclude** la voce di piattaforma (che
sarebbe mostrata come se fosse una applicazione).

## Passo 3 — Stati e onestà dei numeri

Regola da rispettare in ogni ramo: se l'importo non è disponibile si mostra un **errore** con possibilità di
riprovare. Nessun valore predefinito, nessuno zero. Il modo più semplice per non sbagliare è che il tipo del
dato non ammetta un valore mancante confuso con lo zero (importo annullabile, non zero implicito).

## Passo 4 — Traduzioni

Cinque lingue, sezione `billing.seats`. Attenzione ai numeri dentro le frasi (plurali) e al formato della
valuta, che segue la lingua come già altrove.

## Passo 5 — Collaudi

- `SeatsCard.test.tsx`: franchigia, a pagamento, riduzione in attesa, listino futuro, errore.
- Non-regressione: `SubscriptionsPanel.test.tsx` — la voce di piattaforma **non** compare fra le applicazioni.
- `frontend/apps/backoffice/e2e/billing.spec.ts`: la scheda dei posti compare con il calcolo, mantenendo
  l'etichetta del percorso nel titolo del test.

## Verifica finale

```bash
cd ../frontend && npm run typecheck && npm test
cd .. && ./run-tests.sh frontend backend
```

## Trappole note

1. **La voce di piattaforma nella tabella delle applicazioni** è l'errore più probabile di questa storia: la
   prova di non-regressione è obbligatoria.
2. **Zero al posto di un errore**: in una sezione di fatturazione è la bugia più costosa possibile.
3. **La conseguenza di un pagamento non riuscito** è un punto aperto della storia: non inventarla in
   implementazione, chiedere.
