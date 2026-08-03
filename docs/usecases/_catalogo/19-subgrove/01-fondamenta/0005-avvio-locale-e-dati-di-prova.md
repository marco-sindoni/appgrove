# 0005 — Avvio locale e dati di prova

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che riprende il lavoro su SubGrove
> voglio avviare l'app in locale e trovarci dentro una situazione realistica, senza costruirla a mano
> così da vedere subito come si comporta l'interfaccia quando qualcosa è in ritardo o sospeso.

**Contesto.** Un'app che si avvia vuota si prova male: gli stati interessanti di SubGrove — il pagamento fallito
al secondo sollecito, l'abbonamento disdetto che vive ancora fino a fine mese, la prova che scade domani — sono
esattamente quelli che nessuno ha voglia di creare a mano ogni volta. Questa storia chiude le fondamenta rendendo
l'app **eseguibile subito dopo l'unione del ramo** e dandole un insieme di dati inventati che copre l'intera
macchina a stati. È anche il posto giusto per verificare che la scoperta automatica dei servizi funzioni davvero:
se viene voglia di modificare a mano uno script di avvio, è un difetto della scoperta, non un passo del lavoro.

## 2. Requisiti funzionali

1. **RF-1** — `./dev.sh services` mostra `abbonati` con la sua porta e il suo schema; `./app-start.sh` avvia
   l'app senza alcuna modifica manuale agli script.
2. **RF-2** — Esiste un insieme di dati di prova, caricabile con un comando, che crea per un account di prova:
   tre piani (mensile, annuale, con prova), una dozzina di abbonati e abbonamenti che coprono **tutti** gli stati
   della macchina.
3. **RF-3** — I dati sono **inventati** e riconoscibili come tali: nomi di fantasia, indirizzi in un dominio di
   prova, importi tondi; nessun dato che possa sembrare di una persona vera.
4. **RF-4** — Il caricamento è **idempotente**: eseguirlo due volte non duplica nulla e non fa fallire nulla.
5. **RF-5** — I dati di prova esistono **solo** in locale e nell'ambiente di collaudo: non c'è alcun percorso che
   li porti in produzione.

## 3. Requisiti tecnici

- **RT-1 — Avvio locale automatico (§15).** La mappa servizio → identificativo → porta → schema si ricava dal
  **solo** `application.properties`; da lì discendono da soli gli script di avvio, le migrazioni, le rotte del
  proxy locale e gli avvii di collaudo.
- **RT-2 — Modulo frontend (§3).** Il modulo è abilitato nello stub locale dell'abilitazione, così che si veda
  subito dopo l'unione del ramo.
- **RT-3 — Isolamento fra account (§1).** I dati di prova nascono sotto un `tenant_id` di prova; un secondo
  account di prova, con dati propri, serve alle prove di isolamento delle storie successive.
- **RT-4 — Dati personali (§10).** Gli abbonati di prova sono **inventati**: la storia non introduce dati
  personali veri, ma i campi che li conterranno vanno già trattati come tali (annotazione e manifesto: storia
  `0009`).
- **RT-5 — Prove (§11).** I dati di prova sono **deterministici**: le prove end-to-end delle storie `0033` e
  `0034` ci si appoggiano e non tollerano casualità né date relative a «oggi» che cambiano il risultato.
- **RT-6 — Registrazione eventi (§14).** Il caricamento registra quante righe ha creato, con account e
  correlazione, senza nomi.

## 4. Criteri di accettazione

**CA-1 — L'app si avvia da sola**
- **Dato** un repository appena clonato
- **Quando** si esegue `./app-start.sh`
- **Allora** `abbonati` si avvia sulla sua porta e risponde, senza che nessuno abbia toccato uno script

**CA-2 — I dati coprono tutti gli stati**
- **Dato** i dati di prova caricati
- **Quando** si apre la sezione Abbonati
- **Allora** ci sono abbonamenti in prova, attivi, in ritardo, disdetti a scadenza, sospesi e cessati

**CA-3 — Caricamento ripetibile**
- **Dato** i dati già caricati · **Quando** si esegue di nuovo il comando · **Allora** non nascono doppioni e il
  comando termina con successo

**CA-4 — Dati riconoscibilmente finti**
- **Dato** l'insieme dei dati · **Quando** lo si ispeziona · **Allora** ogni recapito è in un dominio di prova e
  nessun nome corrisponde a una persona reale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`, `smoke`);
- [ ] prova di **integrazione** sul caricamento ripetuto;
- [ ] prova di **isolamento fra account**: i due account di prova non si vedono a vicenda;
- [ ] **prova end-to-end**: *rimando* — i dati servono ai percorsi delle storie `0033` e `0034`, dichiarati nel
      registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni**: nessun testo visibile nuovo;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato: perché i dati di prova coprono tutti gli stati e perché sono
      deterministici;
- [ ] `run-tests.sh` aggiornato se l'area `smoke` avvia anche questo servizio.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0001`, `0002`, `0003` | servono servizio, schema e modulo per avere qualcosa da avviare e da riempire |

## 7. Fuori ambito

- l'importazione dei dati veri del cliente da foglio di calcolo: è una funzione di prodotto, non di sviluppo, e
  non ha una storia in questo indice (vedi punto aperto);
- la generazione delle scadenze passate: arriva con la storia `0012`, che le sa creare per davvero.

## 8. Punti aperti

**L'importazione iniziale da foglio di calcolo manca dall'indice.** L'analisi in rete la dà come la prima cosa
che i clienti chiedono (§2.4 della descrizione), ma è una storia di prodotto grossa — corrispondenza delle
colonne, anteprima, gestione degli scarti — che non appartiene alle fondamenta e che rischia di sbilanciare
l'epica 02. **Proposta**: tenerla fuori dal primo giro e affrontarla come storia a sé quando l'app avrà una forma
stabile, riusando l'impianto di anteprima e scarti della storia `0019` (importazione degli esiti), che è lo stesso
problema in piccolo. Chiude: lo sviluppatore, quando si decide il perimetro della prima versione utilizzabile.
