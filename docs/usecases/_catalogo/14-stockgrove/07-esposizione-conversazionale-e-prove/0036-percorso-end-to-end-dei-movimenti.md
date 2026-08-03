# 0036 — Percorso end-to-end dei movimenti

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0036` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0004`, `0006`, `0008`, `0013`, `0014`, `0015`, `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio un percorso automatico che parta dall'attivazione dell'app e arrivi a un saldo corretto dopo due scarichi
> simultanei
> così da sapere, a ogni modifica, che il cuore di StockGrove — il registro in sola aggiunta e la giacenza
> derivata — regge davvero e non solo nelle prove di unità.

**Contesto.** La corruzione silenziosa del saldo è il rischio esistenziale di questa applicazione (§11 della
descrizione): il cliente non se ne accorge subito, se ne accorge il giorno in cui promette merce che non ha, e a
quel punto smette di fidarsi per sempre. Le prove di unità e di integrazione coprono i pezzi; questo percorso copre
la catena intera sullo stack locale reale, attivazione dell'abbonamento compresa. Il caso che conta è quello che
non si riproduce a mano: **due persone che scaricano lo stesso articolo nello stesso istante**. È il caso normale
di un magazzino, non il caso limite, e se l'aritmetica finisse in memoria invece che nella base di dati sarebbe
anche il modo in cui due scarichi da 3 su 5 pezzi lasciano 2 pezzi invece di rifiutarne uno.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il file `tools/platform-e2e/journeys/J-MAGAZZINO.spec.ts` e ogni suo test porta l'etichetta
   `[J-MAGAZZINO]` **in testa al titolo**, come vuole la convenzione del registro di copertura.
2. **RF-2** — Il percorso gira sullo **stack locale reale** (servizio `magazzino` sulla sua porta, base di dati
   vera con migrazioni vere, fornitore di pagamento simulato), senza finestra del browser, con accesso
   **programmatico** e **senza attese a tempo**: si attende una condizione, mai un numero di secondi.
3. **RF-3** — I dati del percorso sono **inventati** e deterministici: un account di prova con indirizzo di posta
   su dominio `*.test`, un deposito «Magazzino centrale» e un articolo con codice interno `TEST-VITE-8`; nessun
   dato che assomigli a quello di un cliente vero.
4. **RF-4** — Il percorso esegue in ordine: attivazione dell'app dal catalogo con il fornitore simulato →
   creazione del deposito e dell'articolo → **carico di 5 pezzi** → **due scarichi da 3 lanciati insieme** →
   verifiche → **storno** dello scarico riuscito → verifiche finali.
5. **RF-5** — Sui due scarichi simultanei il percorso verifica che **esattamente uno** riesca e che l'altro riceva
   `409` con la quantità residua indicata nel messaggio; l'esito non dipende da quale dei due arrivi primo, quindi
   l'asserzione è sul conteggio dei successi, non sull'identità del vincitore.
6. **RF-6** — Dopo i due scarichi la giacenza è **2** e il registro contiene **due** movimenti: un carico da +5 e
   uno scarico da −3. Dopo lo storno la giacenza torna **5** e il registro contiene **tre** movimenti, nessuno
   cancellato e nessuno modificato: lo storno è una riga in più, con il rimando al movimento che annulla.
7. **RF-7** — Il registro di copertura [`docs/testing/copertura-e2e.yaml`](../../../../testing/copertura-e2e.yaml)
   riceve le voci di questa applicazione per i percorsi coperti qui, con l'etichetta del percorso e il nome dei
   test che li coprono; il controllo automatico dell'area `tooling` resta verde.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il percorso crea **due** account di prova e verifica che l'elenco dei
  movimenti e la giacenza di ciascuno contengano solo i propri dati, anche quando la richiesta forza
  l'identificativo dell'altro account; il `tenant_id` viene sempre dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Il percorso usa le rotte pubbliche
  `/api/magazzino/v1/articoli`, `/depositi`, `/movimenti` e `/giacenze` così come sono pubblicate, senza scorciatoie
  interne; gli errori attesi sono verificati nella forma `application/problem+json`.
- **RT-3 — Persistenza (§8).** Nessuna migrazione nuova. Il percorso gira su una base di dati con le migrazioni
  Flyway vere applicate dallo stack locale, e ripulisce i propri account alla fine per restare ripetibile.
- **RT-4 — Modulo frontend (§3, §5).** Il percorso attraversa l'interfaccia dove serve a dimostrare la superficie
  utente — attivazione dal catalogo, elenco delle giacenze, storico dei movimenti di un articolo — e usa selettori
  stabili, non testi tradotti, così che restare in cinque lingue non lo renda fragile.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile nuovo. Il percorso gira nella lingua predefinita `en` e non
  fa asserzioni su stringhe tradotte.
- **RT-6 — Varchi e quota (§6, §7).** L'attivazione dell'app passa dal fornitore di pagamento **simulato**: in
  locale non esiste alcun pagamento vero. Il percorso verifica che prima dell'abilitazione la risorsa risponda
  `402` e dopo `200`. Verifica inoltre il punto delicato del listino: con la quota `articoli_gestiti` esaurita la
  creazione di un articolo nuovo riceve `429`, mentre **un movimento su un articolo esistente passa comunque** —
  è la garanzia che nessun tetto di piano corrompa il saldo di un cliente.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: il percorso esercita le rotte pubbliche.
  Gli strumenti dichiarati in `0034` e `0035` restano provati dalle loro prove di integrazione, perché il server
  conversazionale è di piattaforma e non esiste ancora (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. I dati del percorso sono **inventati**, con
  indirizzi di posta su dominio `*.test`; l'autore dei movimenti è l'utente di prova, e il percorso **non** fa
  alcuna asserzione che aggreghi movimenti per persona.
- **RT-9 — Registrazione eventi (§14).** Il percorso verifica che il rifiuto per giacenza insufficiente produca
  una riga di registro `scarico respinto per giacenza insufficiente` con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, e **senza** la descrizione dell'articolo.

## 4. Criteri di accettazione

**CA-1 — Dall'app spenta al primo carico**
- **Dato** un account di prova appena creato, senza abbonamento a `magazzino`
- **Quando** il percorso attiva l'app dal catalogo con il fornitore simulato, crea il deposito «Magazzino
  centrale» e l'articolo `TEST-VITE-8`, e registra un carico di 5 pezzi
- **Allora** prima dell'attivazione la creazione dell'articolo rispondeva `402`, dopo l'attivazione il carico
  riesce e la giacenza dell'articolo nel deposito è **5**

**CA-2 — Due scarichi simultanei, uno solo passa**
- **Dato** 5 pezzi in giacenza · **Quando** due richieste di scarico da 3 partono insieme dallo stesso account
- **Allora** esattamente una riceve `201` e l'altra `409` in `application/problem+json` con la quantità residua
  indicata; la giacenza è **2**, mai −1 e mai 2 per entrambe

**CA-3 — Il registro racconta due fatti, non uno**
- **Dato** il carico e lo scarico riusciti
- **Quando** si legge lo storico dei movimenti dell'articolo
- **Allora** ci sono **due** movimenti — `+5` carico e `−3` scarico — ciascuno con autore e momento, e la somma
  algebrica delle quantità coincide con la giacenza pubblicata

**CA-4 — Lo storno aggiunge, non cancella**
- **Dato** lo scarico riuscito · **Quando** lo si storna con un motivo
- **Allora** la giacenza torna **5**, i movimenti in registro sono **tre**, il movimento stornato è ancora
  presente e invariato, e lo storno porta il rimando al movimento che annulla

**CA-5 — Quota esaurita: si blocca l'articolo, non il movimento**
- **Dato** un account che ha raggiunto il tetto di `articoli_gestiti` del proprio piano
- **Quando** tenta di creare un articolo nuovo e poi registra un carico su un articolo esistente
- **Allora** la creazione riceve `429` con l'indicazione del rimedio e nulla viene creato, mentre il carico riesce
  e la giacenza si aggiorna

**CA-6 — Isolamento fra due account**
- **Dato** due account di prova, ciascuno con il proprio articolo e i propri movimenti
- **Quando** il primo chiede giacenze e movimenti forzando l'identificativo del secondo
- **Allora** vede solo i propri, e il parametro estraneo è ignorato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `platform` e `tooling`; l'intera suite prima del commit);
- [ ] prove di **unità** e di **integrazione** già esistenti dalle storie precedenti: qui non si duplicano, si
      verifica la catena intera;
- [ ] prova di **isolamento fra account** compresa dentro il percorso;
- [ ] **prova end-to-end**: *coperta ora* — percorso `tools/platform-e2e/journeys/J-MAGAZZINO.spec.ts` creato, ogni
      test etichettato `[J-MAGAZZINO]`, e registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato con le voci di questa
      applicazione; il controllo dell'area `tooling` è verde;
- [ ] **traduzioni**: nessun testo visibile nuovo;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificato che i dati di prova siano inventati e su domini
      `*.test`;
- [ ] **registro delle decisioni** compilato, con la scelta di asserire sul conteggio dei successi invece che
      sull'identità del vincitore della corsa;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione nuova esposta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: [docs/testing/README.md](../../../../testing/README.md) elenca il percorso nuovo.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0004` | L'abbonamento e la metrica `articoli_gestiti` servono per attivare l'app e per provare il `429` |
| `0006`, `0008` | Articolo e deposito devono esistere come risorse |
| `0013` | Registro in sola aggiunta, giacenza derivata e chiave di idempotenza sono l'oggetto della prova |
| `0014`, `0015` | Carico e scarico sono le operazioni del percorso, e lo scarico porta la regola dei `409` |
| `0017` | Lo storno è l'ultimo passo, e dimostra che correggere non significa cancellare |

## 7. Fuori ambito

- **L'inventario fisico, le differenze e la ricostruzione della giacenza**: sono il secondo percorso, storia `0037`.
- **Trasferimento fra depositi, movimenti dagli eventi delle altre app, scansione dal telefono e proposta di
  riordino**: restano coperti dalle rispettive prove di integrazione; portarli tutti dentro un percorso end-to-end
  lo renderebbe lento e fragile senza aggiungere garanzia.
- **La finestra del fornitore di pagamento**: non si guida mai con il collaudo del browser; qui il fornitore è
  simulato.
- **La misura del tempo di risposta**: questo percorso verifica la correttezza, non le prestazioni.

## 8. Punti aperti

- **Come si lanciano due richieste davvero simultanee** in un collaudo end-to-end senza attese a tempo: la forma
  concreta (due chiamate parallele dal contesto della prova, con la corsa risolta dal blocco di riga nella base di
  dati) va verificata sullo strumento reale al momento dell'implementazione. Se la simultaneità non fosse
  riproducibile in modo stabile, il rimedio corretto è una prova di integrazione dedicata con transazioni
  concorrenti — **non** rinunciare al caso.
- **La ripetibilità dell'account di prova**: se il percorso debba ricreare l'account a ogni esecuzione o riusarne
  uno stabile dipende da come lo stack locale pulisce i dati, ed è una convenzione di piattaforma.
