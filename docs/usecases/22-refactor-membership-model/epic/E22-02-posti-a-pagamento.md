# E22.2 — Posti a pagamento

**Epica madre**: [Epica 22](E22-00-rifacimento-modello-appartenenza.md) · **Storie**: 0102, 0103, 0104, 0105, 0106
**Stato**: 🟢 analisi scritta · **Ultimo aggiornamento**: 2026-08-19

## Obiettivo

Trasformare la persona in un oggetto che si compra: un **posto**, con un prezzo a fasce, pagato in
anticipo, con permanenza minima di un mese e una riduzione che non è mai immediata. È la parte
dell'epica che tocca il denaro, e quindi quella in cui un difetto non è un fastidio ma un danno.

## La novità architetturale: un abbonamento che non è di un'applicazione

Oggi ogni abbonamento è legato a un'applicazione: la tabella `platform.subscription` ha una colonna
`app_id` **obbligatoria**, con un vincolo di unicità per coppia account-applicazione. I posti sono
invece **di piattaforma**: non appartengono a nessuna applicazione.

Questa è la scelta strutturale più impegnativa della sotto-epica, e le due vie sono entrambe difendibili:

- **Via A — una voce di catalogo «piattaforma».** Si inserisce nel catalogo una voce che non è
  un'applicazione (marcata come tale) e i posti diventano un abbonamento come tutti gli altri. Si
  riusano interi il pagamento, i richiami del fornitore di pagamento, il ciclo di vita, la riconciliazione
  e la sezione «Billing». Prezzo: una voce nel catalogo che non è un'app va **nascosta** in ogni
  vetrina, e ogni lettura che dice «per ogni app dell'account» va rivista.
- **Via B — una struttura dedicata ai posti.** Concettualmente pulita, ma costringe a riscrivere il
  pagamento, la ricezione degli eventi del fornitore, la fatturazione e la riconciliazione: mesi di
  lavoro per elegan­za.

**Si adotta la via A**, con un vincolo esplicito: la voce di piattaforma è marcata come **non
installabile** e ogni superficie che elenca applicazioni la esclude. La storia 0103 rende questa
decisione operativa e ne elenca i punti in cui l'esclusione va garantita da un test.

## Perché in questo ordine

1. **0102 — il listino.** Il calcolo del dovuto è una funzione pura: quanti posti, quale fascia, quanto
   si paga. Va scritta e provata **prima** di qualunque interfaccia, perché è la cosa che nessuno
   perdonerebbe sbagliata.
2. **0103 — l'acquisto anticipato.** L'invito diventa un atto che passa dalla cassa. Include il caso
   più insidioso: che succede se il pagamento non va a buon fine *dopo* che l'invito è partito.
3. **0104 — la riduzione in attesa.** Lo stato più ricco dell'epica: scelta delle persone, blocco delle
   aggiunte, annullamento, e la scadenza che finalmente esegue.
4. **0105 — il governo del listino.** Requisito aggiunto dallo sviluppatore: le tariffe si cambiano da
   console, per tutti, dal ciclo successivo.
5. **0106 — la trasparenza.** Il cliente deve poter capire perché paga quella cifra: righe, storico,
   prossimo rinnovo.

## Le decisioni portanti

**Il listino è a scaglioni progressivi**: ogni posto paga la tariffa della fascia in cui cade *quel
posto*. Con 12 posti si paga `7 × 2,99 + 2 × 1,99 = 24,91 €`, non `9 ×` una tariffa unica.

È il modello scelto dallo sviluppatore **dopo** aver provato l'alternativa (tariffa di fascia su tutti i
posti a pagamento) e averla scartata: quella faceva **scendere il totale** ai confini — undici posti
costavano meno di dieci — e un prezzo che cala quando cresci è indifendibile davanti a un cliente anche
quando è a suo favore, perché sembra un errore di conteggio. Con la progressività il totale è **monotono
crescente** e a scendere è il **costo del posto successivo**: la stessa convenienza, detta in un modo che
si capisce. Costa qualche riga di codice in più e un collaudo che somma gli scaglioni.

**L'owner occupa un posto.** La franchigia è di tre persone in tutto. Così il numero mostrato coincide
con il numero di persone, e non serve spiegare a nessuno perché il conteggio non torna.

**Il posto si paga prima che l'invito parta.** L'ordine degli atti è: verifica dei posti → addebito →
creazione dell'invito → invio dell'email. Se l'addebito non riesce, l'invito **non nasce**: è
preferibile un invito mancato a un posto non pagato.

**La riduzione non tocca l'accesso.** Chi è indicato per la cessazione lavora fino a scadenza. Chi
vuole escludere qualcuno subito gli toglie l'accesso alle applicazioni: due operazioni diverse, con
effetti diversi, e l'interfaccia deve spiegarlo senza ambiguità.

**Nessun posto nuovo durante l'attesa.** Requisito esplicito. La ragione pratica: sommare un'aggiunta e
una riduzione dentro lo stesso periodo renderebbe il conto del periodo indecidibile e la fattura
inspiegabile.

**Le tariffe sono versionate, non modificabili.** La console non modifica un prezzo: **crea una nuova
versione del listino** con la sua data di decorrenza. È lo stesso principio che regge il listino delle
applicazioni («un prezzo nuovo è un livello nuovo, non la mutazione di uno esistente»), e serve a una
cosa concreta: poter rispondere fra un anno alla domanda «quanto pagava questo cliente in marzo?».

## Rischi propri di questa sotto-epica

| Rischio | Mitigazione |
|---|---|
| Un posto attivo non pagato (l'invito nasce, l'addebito no) | 0103: ordine degli atti e ripristino esplicito in caso di fallimento; nessun invito senza addebito riuscito |
| Due inviti simultanei sull'ultimo posto disponibile | 0103: il controllo dei posti e la creazione dell'invito sono atomici, come già avviene per le quote a giacenza |
| Riduzione che scade e non viene eseguita (posti fantasma pagati per sempre) | 0104: esecuzione a scadenza da lavoro periodico, con la sua misura e il suo allarme |
| Cambio di tariffa applicato retroattivamente | 0105: la nuova versione ha decorrenza dal ciclo successivo; i periodi già fatturati sono immutabili |
| Il cliente non capisce la fattura | 0106: la riga dei posti mostra numero, fascia, tariffa e calcolo, non solo il totale |
