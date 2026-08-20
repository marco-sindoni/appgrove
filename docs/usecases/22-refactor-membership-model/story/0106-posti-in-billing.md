# UC 0106 — I posti nella sezione «Billing»: righe, storico, prossimo rinnovo

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.2 Posti a pagamento](../epic/E22-02-posti-a-pagamento.md)
**Dipendenze**: UC 0103 (abbonamento di piattaforma), UC 0102 (calcolo), UC 0096 (sezione di fatturazione), UC 0067 (gestione dell'abbonamento self-service)
**Piano di lavoro**: [task/0106](../task/0106-posti-in-billing.md)
**Prototipo**: [owner.html](../prototype/owner.html), sezione «Billing»
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Fare in modo che l'owner **capisca** perché paga quella cifra per le persone: quante sono, quale fascia si
applica, come si arriva al totale, che cosa cambierà al prossimo rinnovo.

**Incluso**: la riga dei posti nella sezione di fatturazione, con il calcolo esposto; l'effetto di una
riduzione in attesa sul prossimo rinnovo; lo storico dei pagamenti che comprende i posti; la ricevuta.

**Escluso**: il calcolo → UC 0102; l'acquisto → UC 0103; la riduzione → UC 0104; il governo delle tariffe →
UC 0105.

## 2. Attori & ruoli

- **Owner**: unico attore (la sezione è sua, UC 0107).
- **Sistema**: espone gli importi calcolati, mai ricalcolati dall'interfaccia.

## 3. Precondizioni

- Esiste l'abbonamento di piattaforma dei posti (UC 0103), oppure l'account è entro la franchigia.
- Esiste la sezione di fatturazione con abbonamenti e storico dei pagamenti (UC 0096).

## 4. Flusso principale

1. L'owner apre «Billing».
2. Accanto agli abbonamenti delle applicazioni compare una voce **distinta**, dichiaratamente non
   un'applicazione: «**Persone del gruppo di lavoro**».
3. La voce mostra, in modo leggibile: **N posti** (di cui 3 gratuiti), la **fascia** applicata, la
   **composizione degli scaglioni**, e il calcolo in una riga: `7 × 2,99 + 2 × 1,99 = 24,91 € al mese`
   (dodici posti). Va mostrata la somma degli scaglioni, non un prodotto unico: è l'unico modo perché il
   cliente ritrovi il proprio conto.
4. Mostra il **prossimo rinnovo**: data e importo previsto. Se c'è una riduzione in attesa, l'importo
   previsto è quello **ridotto**, con la nota «2 persone cesseranno il 14 settembre».
5. Se una nuova versione del listino è già programmata (UC 0105) e cambierà l'importo, la voce lo dice:
   «dal 1° ottobre la tariffa per posto sarà 0,45 €».
6. Lo **storico dei pagamenti** comprende gli addebiti dei posti, distinguibili da quelli delle
   applicazioni, con la loro ricevuta scaricabile come le altre.

## 5. Flussi alternativi / edge / errori

- **Edge — account entro la franchigia**: la voce c'è comunque e dice «3 posti, gratuiti — le prime tre
  persone sono incluse». Mostrarla anche a zero costo serve a far capire che esiste un limite e che oltre
  si paga: è informazione, non pubblicità.
- **Edge — addebito parziale del periodo in corso** (quando si aggiunge una persona a metà mese): la riga
  dello storico deve dire che si riferisce a una frazione di periodo, altrimenti il cliente non ritrova il
  numero. Il modo esatto dipende da come il fornitore di pagamento espone la proporzione: da verificare in
  implementazione.
- **Edge — pagamento dei posti non riuscito**: la regola è **decisa** (risposta dello sviluppatore) e
  distingue tre casi, perché non sono la stessa cosa:
  1. **errore definitivo** (la carta è rifiutata, il metodo di pagamento non è valido): è un **errore
     fatale** e l'aggiunta dell'utente **non procede**. Nessun invito, nessun posto: si torna allo stato
     precedente e il messaggio dice perché;
  2. **errore temporaneo** (il fornitore di pagamento non risponde, un guasto di rete): si **può
     ritentare**, e l'interfaccia lo propone come tale invece di dichiarare un fallimento;
  3. **errore sistematico** (i tentativi continuano a fallire): si invia una **email
     all'amministratore di appgrove**, perché a quel punto il problema è probabilmente nostro o del
     fornitore, non del cliente.
  Le persone già attive **non perdono accesso** per un pagamento non riuscito, e l'account **non si
  blocca**: il guasto impedisce di *aggiungere*, non di *lavorare*. La distinzione fra errore definitivo
  e temporaneo la fornisce il fornitore di pagamento; la soglia oltre cui un temporaneo diventa
  sistematico va fissata in implementazione (proposta: tre tentativi falliti sullo stesso account).
- **Errore — importo non disponibile**: si mostra un errore con possibilità di riprovare, **non** uno zero.
  Uno zero al posto di un errore è la bugia più costosa che una sezione di fatturazione possa raccontare.

## 6. Schermate & stati

Nella pagina «Billing», in testa alla tabella degli abbonamenti (perché riguarda tutto l'account, non una
applicazione): una scheda dedicata con icona di gruppo, distinta dalle applicazioni per tinta e per
etichetta. Dentro: il conteggio, la composizione (attive · inviti in attesa · in cessazione), la fascia,
il calcolo, il prossimo rinnovo, e un collegamento a «Members» per agire.

Stati: caricamento, franchigia (costo zero), a pagamento, riduzione in attesa, listino futuro programmato,
stato irregolare, errore.

## 7. Dati toccati

- **`platform.subscription`** e **`platform.billing_transaction`**: letti. La voce dei posti va **inclusa**
  nelle letture della sezione di fatturazione, che oggi presumono che ogni abbonamento appartenga a una
  applicazione: è la controparte della decisione strutturale di UC 0103 e va gestita con cura in ogni
  lettura.
- **Dati personali**: nessuno nuovo. L'informazione «quante persone» è un conteggio; la sezione non elenca
  chi sono (per quello c'è «Members»).

## 8. Permessi & gate

- **Sezione riservata all'owner** (UC 0107).
- **Importi calcolati dal servizio**; l'interfaccia non fa aritmetica sui prezzi.
- **Account solo dal token verificato**.

## 9. Requisiti di test

- **Componente**: la scheda dei posti mostra il calcolo corretto nei casi di franchigia, a pagamento,
  riduzione in attesa, listino futuro programmato.
- **Integrazione**: le letture della sezione di fatturazione comprendono l'abbonamento dei posti senza
  romperne la presentazione (prova di regressione sulle letture che presumono l'applicazione).
- **Percorso end-to-end di livello 2** su `frontend/apps/backoffice/e2e/billing.spec.ts` (esistente, da
  estendere): la scheda dei posti compare con il suo calcolo.
- **Prova di errore**: importo non disponibile → messaggio di errore, non zero.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [UC 0096](../../21-catalogo-app-backoffice/0096-billing-solo-fatturazione.md),
  [SubscriptionsPanel.tsx](../../../../frontend/apps/backoffice/src/billing/SubscriptionsPanel.tsx),
  [PaymentsResource.java](../../../../services/core/src/main/java/app/appgrove/core/billing/PaymentsResource.java).
- **Definition of Done**:
  1. la voce dei posti c'è sempre, anche a costo zero, e mostra il calcolo;
  2. il prossimo rinnovo riflette riduzioni in attesa e listini futuri;
  3. lo storico comprende gli addebiti dei posti con la loro ricevuta;
  4. un importo non disponibile è un errore, non uno zero;
  5. `run-tests.sh frontend backend` verde.

## Punti aperti / decisioni differite

- **Soglia dell'errore sistematico**: quanti tentativi falliti fanno scattare l'email all'amministratore
  di appgrove (proposta: tre sullo stesso account, entro lo stesso periodo). L'unica cosa rimasta aperta
  del §5: la regola generale è decisa.
- **Rimborsi e proporzioni**: nessun rimborso previsto; la proporzione all'aggiunta dipende dal fornitore
  di pagamento. Da verificare in implementazione. Proprietario: questa storia.
- **Fatturazione unificata** (un'unica riga per tutto invece di una voce per applicazione più i posti):
  fuori scope, e già annotata come accorpamento degli abbonamenti nell'epica 13.
