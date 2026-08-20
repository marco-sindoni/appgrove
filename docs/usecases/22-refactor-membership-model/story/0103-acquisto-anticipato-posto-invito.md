# UC 0103 — Acquisto anticipato del posto all'invito (abbonamento di piattaforma)

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.2 Posti a pagamento](../epic/E22-02-posti-a-pagamento.md)
**Dipendenze**: UC 0102 (listino e calcolo), UC 0100 (elenco unico), UC 0024 (pagamento), UC 0025 (ricezione degli eventi del fornitore), UC 0026 (ciclo di vita dell'abbonamento)
**Piano di lavoro**: [task/0103](../task/0103-acquisto-anticipato-posto-invito.md)
**Prototipo**: [owner.html](../prototype/owner.html), riquadro dei posti e finestra di invito
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Fare dell'invito un atto che **passa dalla cassa**: il posto si paga prima che l'invito parta, e
l'abbonamento che lo contiene è **di piattaforma**, non di una applicazione.

**Incluso**: l'abbonamento di piattaforma per i posti; l'ordine degli atti all'invito; il comportamento se
il pagamento non riesce; l'atomicità sull'ultimo posto; l'esposizione dei posti usati e del costo del
posto successivo; il primo invito oltre la franchigia, che è il caso in cui l'account paga per la prima
volta.

**Escluso**: il calcolo → UC 0102; la riduzione → UC 0104; la fattura e lo storico → UC 0106.

## 2. Attori & ruoli

- **Owner**: l'unico che può invitare, perché l'operazione ha effetto economico. È il motivo per cui
  l'`admin` di una applicazione può solo abilitare persone **già esistenti**.
- **Fornitore di pagamento**: incassa e conferma.
- **Sistema**: verifica, addebita, crea, notifica — in quest'ordine.

## 3. Precondizioni

- Esiste il listino vigente (UC 0102).
- L'account ha un metodo di pagamento valido **se** l'invito porta oltre la franchigia; entro i tre posti
  gratuiti non serve nulla.
- Non è in corso una riduzione in attesa (altrimenti l'invito è bloccato, UC 0104).

## 4. Flusso principale

1. L'owner apre l'invito dalla sezione «Members» e digita l'indirizzo.
2. **Prima di inviare**, l'interfaccia mostra l'effetto: «questa persona sarà il posto numero 4; costo
   2,99 € al mese; il tuo totale passerà da 0,00 a 2,99 €». La cifra arriva dal servizio, che la calcola
   col listino vigente (UC 0102) — l'interfaccia non fa aritmetica.
3. L'owner conferma. Il sistema esegue, **in questo ordine**:
   1. verifica che non ci sia una riduzione in attesa;
   2. verifica che l'indirizzo non sia già presente o già invitato;
   3. calcola il nuovo dovuto;
   4. **addebita** la differenza per il periodo in corso, aggiornando l'abbonamento di piattaforma
      (creandolo alla prima volta);
   5. **solo a esito positivo** crea l'invito e ne invia l'email.
4. La persona compare nell'elenco in stato «invito in attesa» e **occupa già un posto**.
5. Quando accetta, lo stato passa ad «attiva»: il posto non cambia, cambia lo stato. Nessun nuovo
   addebito.
6. L'owner abilita poi la persona sulle applicazioni (UC 0111): **gratis**, perché il posto è di
   piattaforma.

## 5. Flussi alternativi / edge / errori

- **Errore — addebito rifiutato**: l'invito **non nasce**. Messaggio esplicito con il motivo che il
  fornitore restituisce e un rimando alla sezione dei pagamenti. È la regola d'oro di questa storia:
  meglio un invito mancato che un posto attivo non pagato.
- **Errore — la creazione dell'invito fallisce dopo l'addebito riuscito**: si **annulla l'addebito**
  (storno o riduzione della quantità) nella stessa unità di lavoro. Se anche l'annullamento fallisce, si
  registra un avviso operativo con severità alta: è uno dei pochi casi in cui una persona deve
  intervenire, e va reso visibile invece che sepolto in un registro.
- **Edge — due inviti simultanei**: la verifica dei posti e la creazione dell'invito sono **atomiche**
  rispetto al conteggio, come già avviene per le quote a giacenza. Non esiste un tetto massimo di posti,
  quindi il rischio non è di sforare un limite ma di **addebitare due volte lo stesso salto di fascia**:
  è quello che l'atomicità impedisce.
- **Edge — invito che scade senza essere accettato**: il posto si **libera**. Poiché il posto era già
  pagato, non si rimborsa: il periodo pagato resta a disposizione dell'account e il posto torna
  disponibile per un altro invito senza nuovo addebito entro lo stesso periodo. Questa è la lettura
  coerente con la permanenza minima mensile.
- **Edge — invito revocato dall'owner**: identico alla scadenza. Nessun rimborso, posto riutilizzabile
  entro il periodo.
- **Edge — l'account entra nella franchigia**: i primi tre posti non generano né abbonamento né addebito.
  L'abbonamento di piattaforma **nasce** col quarto posto.
- **Edge — periodo di prova gratuito di una applicazione in corso**: irrilevante. Il posto è di
  piattaforma e si paga comunque (punto aperto da confermare).
- **Edge — account in attesa di cancellazione**: nessun invito ammesso.

## 6. Schermate & stati

Nella sezione «Members» (UC 0100), il **riquadro dei posti** in testa alla pagina:

- «**Posti usati N**» con, sotto, la composizione: attive, inviti in attesa, in cessazione.
- «**Stai pagando X € al mese**» con la fascia applicata e il numero di posti a pagamento.
- «**Il prossimo posto costa Y €**», e quando il posto successivo fa **scendere** la tariffa, il testo lo
  dice: «questa persona costa 1,99 € invece di 2,99 €, perché entri nello scaglione successivo; il totale
  passa da 20,93 a 22,92 €». Col listino progressivo il totale **sale sempre**: quello che scende è il
  costo del posto in più, e va detto proprio così.
- Stati: caricamento del riquadro (il pulsante di invito resta disabilitato finché il costo non è noto —
  mai invitare alla cieca), errore di lettura (nessun invito permesso, con possibilità di riprovare),
  riduzione in attesa (riquadro di avviso e invito **bloccato**, UC 0104).

La finestra di invito mostra la stima **prima** della conferma e, dopo la conferma riuscita, l'esito con
il collegamento all'invito, come oggi.

## 7. Dati toccati

- **`platform.subscription`**: l'abbonamento dei posti vive qui, con la scelta strutturale descritta nella
  sotto-epica: una **voce di catalogo di piattaforma**, marcata come non installabile, che permette di
  riusare pagamento, eventi del fornitore, ciclo di vita e riconciliazione. Serve una colonna o una
  proprietà che porti la **quantità** (il numero di posti a pagamento), che oggi non esiste perché gli
  abbonamenti delle applicazioni sono a quantità uno.
- **`platform.app`**: una riga per la voce di piattaforma, con un contrassegno che la esclude da ogni
  vetrina, dal catalogo, dai diritti d'accesso e dal menu laterale.
- **`platform.invitations`**: perde il ruolo (UC 0100), acquisisce il legame con l'addebito che l'ha
  autorizzato.
- **Dati personali**: l'email dell'invitato è **già dichiarata** in UC 0013. Novità: quell'indirizzo
  concorre a determinare un importo, quindi entra indirettamente nel trattamento di fatturazione già
  dichiarato per i pagamenti. Nessuna nuova categoria, nessuna nuova base giuridica: resta l'esecuzione
  del contratto con l'account titolare. Da annotare nel manifesto della piattaforma come precisazione di
  finalità.

## 8. Permessi & gate

- **Solo l'owner invita.** L'interfaccia del core rifiuta ogni altro chiamante. È il presidio economico
  dell'intero modello.
- **Nessun invito con una riduzione in attesa** (UC 0104).
- **Nessun invito senza addebito riuscito**, quando l'invito porta oltre la franchigia.
- **Account solo dal token verificato**; il conteggio dei posti è sempre per account.
- **La voce di catalogo di piattaforma non concede diritti d'accesso** ad alcuna applicazione: va esclusa
  esplicitamente dalla lettura dei diritti, altrimenti comparirebbe nel menu laterale come se fosse una
  applicazione.

## 9. Requisiti di test

- **Integrazione**: primo invito entro la franchigia → nessun abbonamento creato; quarto invito →
  abbonamento creato con quantità 1 e addebito 2,99 €; quinto → quantità 2, addebito 5,98 €.
- **Integrazione**: addebito rifiutato → nessun invito creato, nessuna riga di invito rimasta a metà.
- **Integrazione**: creazione dell'invito fallita dopo addebito riuscito → addebito annullato.
- **Concorrenza**: due inviti simultanei non producono due salti di fascia addebitati.
- **Integrazione**: invito scaduto o revocato → il posto si libera; un nuovo invito entro lo stesso
  periodo non genera un secondo addebito.
- **Esclusione della voce di piattaforma**: non compare nel catalogo, nei diritti d'accesso, nel menu
  laterale, nella pagina delle applicazioni. Una prova per ognuna di queste superfici — è il prezzo della
  scelta strutturale e va pagato in collaudi.
- **Percorso end-to-end di piattaforma**: l'owner invita oltre la franchigia con il simulatore del
  fornitore di pagamento, e vede posti e importo aggiornati.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [Subscription.java](../../../../services/core/src/main/java/app/appgrove/core/billing/Subscription.java),
  [CheckoutResource.java](../../../../services/core/src/main/java/app/appgrove/core/billing/CheckoutResource.java),
  [UC 0024](../../07-payments/0024-checkout.md), [UC 0026](../../07-payments/0026-ciclo-vita-abbonamento.md),
  [EntitlementReadModel.java](../../../../services/core/src/main/java/app/appgrove/core/billing/EntitlementReadModel.java)
  da cui la voce di piattaforma va esclusa.
- **Definition of Done**:
  1. il posto si paga prima che l'invito parta, e senza pagamento l'invito non esiste;
  2. l'abbonamento di piattaforma esiste, con la sua quantità, e riusa l'impianto di pagamento;
  3. la voce di catalogo di piattaforma è invisibile in tutte le superfici, provato una per una;
  4. il riquadro dei posti mostra usati, importo e costo del prossimo, con la lettura del caso in cui
     scende;
  5. `run-tests.sh backend frontend` verde più il percorso di piattaforma.

## Punti aperti / decisioni differite

- **Posti durante un periodo di prova di una applicazione**: proposta «si pagano comunque». Va confermato
  da chi decide i prezzi. Proprietario: questa storia.
- **Rimborsi**: nessuno, coerente con la permanenza mensile minima. Da confermare con chi gestisce la
  fatturazione. Proprietario: UC 0106.
- **Voce di catalogo di piattaforma contro struttura dedicata**: si adotta la voce di catalogo per riusare
  l'impianto; il debito è l'insieme delle esclusioni da mantenere. Se le esclusioni diventassero più di
  una manciata, va rivalutata la struttura dedicata. Proprietario: Epica 22.
- **Chi paga se l'owner cambia**: legato al passaggio di proprietà, fuori scope.
- **Limite al numero di account che una persona può aprire** — *portato qui dalla change `0090`*
  (UC 0118). Il percorso «apri un altro account» esiste ed è senza limiti: oggi non è un problema perché
  aprire un account è gratuito e chi lo apre è già una persona conosciuta. Diventa una domanda vera
  quando aprire un account **costa** qualcosa, e allora è una decisione **commerciale**, non tecnica: un
  tetto, una verifica, o nulla. Da decidere insieme al prezzo dei posti.
- **Il posto non è ancora contato, e l'invito non viene rifiutato per posti esauriti** — *portato qui
  dalla change `0090`* (UC 0118). Quella storia ha scritto la regola nel **testo mostrato al cliente**
  («il posto è di questo account: si paga qui anche se la persona lavora già in un altro account»),
  perché la prima reazione di chi invita qualcuno che ha già un account altrove è «ma la paga già
  l'altra azienda». Applicare la regola — contare i posti e rifiutare quando sono esauriti — è di questa
  storia.
