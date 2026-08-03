# 0006 — Contratto del fatto di misura

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 02 — Arrivo dei dati dalle altre app
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che dovrà far pubblicare i propri numeri a sei applicazioni diverse
> voglio un contratto scritto e verificabile che dica esattamente che cosa un fatto di misura può contenere e che
> cosa non può contenere
> così da poter aggiungere una fonte nuova senza riaprire la discussione, e senza che nessuno faccia entrare in
> InsightGrove dati che non ci devono stare.

**Contesto.** È **la storia più importante dell'applicazione**. Tutto il ragionamento del §4.2 della
[descrizione](../application-description.md) — l'isolamento fra account regge perché il fatto è un aggregato, non
una riga — è vero solo se il contratto lo impone e qualcosa lo verifica. Un contratto lasso trasforma questa app
nella copia di tutte le altre, con il doppio della superficie di conformità e il doppio dei posti dove sbagliare.
Va scritto **prima** del consumatore (0007), perché è il consumatore a doverlo far rispettare.

## 2. Requisiti funzionali

1. **RF-1** — Esiste uno schema versionato del fatto di misura, con questi campi e nessun altro:
   identificativo dell'account, applicazione d'origine, chiave della misura, inizio e fine del periodo di
   competenza, dimensioni (coppie chiave-valore), valore numerico, unità, momento dell'evento, chiave di
   idempotenza, riferimento alla riga d'origine (applicazione, tipo di entità, identificativo opaco).
2. **RF-2** — Il contratto **vieta esplicitamente** nel fatto: testo libero di qualunque genere, campi
   anagrafici (nome, indirizzo, contatto, identificativo fiscale), contenuti di documento, e qualunque attributo
   riconducibile a una **categoria particolare** — salute, dati biometrici o genetici, opinioni politiche,
   convinzioni religiose, orientamento sessuale, appartenenza sindacale.
3. **RF-3** — Ogni applicazione d'origine **dichiara** l'elenco chiuso delle chiavi di misura e delle chiavi di
   dimensione che pubblica, con unità e significato in italiano e inglese. Una chiave non dichiarata è una chiave
   che non entra.
4. **RF-4** — Il valore è sempre un numero con un'unità dichiarata (importo in centesimi con valuta, quantità,
   conteggio, durata in secondi, giorni): non esistono valori senza unità, perché sommare due unità diverse è il
   modo più silenzioso di sbagliare un numero.
5. **RF-5** — Il riferimento alla riga d'origine è **opaco**: identifica una riga nell'app che l'ha prodotta e
   non dice niente di per sé. Serve al rimando (storia 0011), non alla lettura.
6. **RF-6** — Esiste un validatore del contratto, eseguibile nei collaudi, che rifiuta un fatto non conforme e
   dice **quale regola** ha violato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'identificativo dell'account nel fatto è quello che l'applicazione
  d'origine ha preso dal proprio gettone verificato: è **l'unica** chiave con cui il fatto verrà scritto. Il
  contratto lo dichiara obbligatorio e non nullo; un fatto senza account è un fatto invalido.
- **RT-2 — Interfaccia di programmazione (§2).** Il contratto vive **dentro il servizio `insights`**, versionato
  con esso, e la sua descrizione è pubblicata in forma leggibile da un programma perché le app sorgenti possano
  generarne il codice invece di riscriverlo.
- **RT-8 — Dati personali (§10).** Il contratto è **il presidio principale sui dati personali di questa app**:
  se tiene, `app_insights` non contiene dati di clienti dell'account. La sola eccezione ammessa è l'etichetta di
  dimensione, che è una decisione aperta (punto aperto 2 della descrizione) e, **se accolta, viaggia su un evento
  separato dal fatto** — così che il fatto resti privo di dati personali anche quando l'etichetta esiste.
- **RT-11 — Prove (§11).** Prove di unità sul validatore: un fatto conforme passa, e per **ognuno** dei divieti
  del RF-2 esiste un caso che viene rifiutato con la regola violata.
- **RT-14 — Registrazione eventi (§14).** Il rifiuto di un fatto si registra con `tenant_id`, `app_id` d'origine
  e la regola violata; **mai il contenuto del fatto rifiutato**, che è precisamente il posto dove starebbe il
  dato che non doveva entrare.

## 4. Criteri di accettazione

**CA-1 — Un fatto conforme è accettato**
- **Dato** un fatto con account, applicazione d'origine, misura `fatturato_emesso`, periodo luglio 2026,
  dimensione `cliente = c-8842`, valore `820000` in centesimi di euro, chiave di idempotenza e riferimento
  d'origine
- **Quando** lo si passa al validatore
- **Allora** è accettato

**CA-2 — Un fatto con testo libero è rifiutato**
- **Dato** un fatto che porta un campo nota con dentro «Cliente da richiamare, ha problemi di liquidità»
- **Quando** lo si passa al validatore
- **Allora** è rifiutato con la regola «nel fatto non è ammesso testo libero», e il contenuto del campo **non
  compare** nel registro applicativo

**CA-3 — Una chiave di dimensione non dichiarata è rifiutata**
- **Dato** una applicazione d'origine che ha dichiarato le dimensioni `cliente`, `categoria` e `sede`
- **Quando** pubblica un fatto con dimensione `diagnosi`
- **Allora** il fatto è rifiutato con la regola «chiave di dimensione non dichiarata»

**CA-4 — Un valore senza unità è rifiutato**
- **Dato** un fatto con valore `4200` e nessuna unità
- **Quando** lo si passa al validatore
- **Allora** è rifiutato con la regola «ogni valore dichiara la propria unità»

**CA-5 — Un fatto senza account è rifiutato**
- **Dato** un fatto in cui l'identificativo dell'account è assente
- **Quando** lo si passa al validatore
- **Allora** è rifiutato, e la regola violata è la prima dell'elenco

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul validatore, con un caso di rifiuto per ciascun divieto;
- [ ] prova di **isolamento fra account**: non applicabile qui (nessuna lettura), ma il contratto è ciò che la
      rende possibile a valle, e va detto esplicitamente nel registro delle decisioni;
- [ ] **prova end-to-end**: *rimando* alla storia 0034; voce `da-coprire` nel registro di copertura;
- [ ] **traduzioni**: le descrizioni delle chiavi di misura e di dimensione sono in italiano e inglese (è il
      requisito del manifesto dati, due lingue, non quello dell'interfaccia, cinque);
- [ ] **manifesto dei dati** aggiornato: il contratto è la prova documentata che il fatto non contiene dati
      personali, e va scritto lì;
- [ ] **registro delle decisioni** compilato, con la decisione (A) o (B) sulle etichette di dimensione e il
      perché;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] documentazione aggiornata: il contratto è materiale che le app sorgenti dovranno leggere.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | la forma della tabella `fatto` e la forma del contratto devono coincidere |
| decisione sulle etichette di dimensione (§6.1 della descrizione) | è una classificazione di dati personali: fermata di escalation dello sviluppatore, da chiudere **prima** di questa storia |
| decisione su chi possiede il contratto degli eventi (punto aperto 11) | se è di piattaforma, questa storia lo consuma invece di definirlo |

## 7. Fuori ambito

- il consumo dei fatti dalla coda: storia 0007;
- l'implementazione della pubblicazione dentro le app sorgenti: è lavoro **loro**, in una loro storia. Qui si
  scrive il contratto che dovranno rispettare;
- il rimando alla riga d'origine come funzione visibile: storia 0011.

## 8. Punti aperti

- 🛑 **Etichette di dimensione: via (A) o via (B)?** Con le etichette leggibili la scomposizione per cliente è
  utile e l'app diventa trattante di dati personali; senza, l'app non tratta dati di clienti ma mostra codici.
  **È una classificazione di dati personali e non la decide un agente** (§6.1 della descrizione).
  Chiude: **sviluppatore**, prima di iniziare questa storia.
- **Il contratto è di piattaforma o di questa app?** Se domani una seconda app volesse consumare gli stessi
  fatti, un contratto nato dentro `insights` sarebbe nel posto sbagliato. Raccomandazione: **nasce qui e si
  promuove a piattaforma** quando servirà a due consumatori, senza anticipare. Chiude: **piattaforma**
  (punto aperto 11 della descrizione).
- **Chi verifica che una fonte rispetti il contratto?** Il validatore rifiuta a valle, ma il difetto è a monte.
  Non esiste oggi un modo di far fallire la compilazione di BillGrove se pubblica un fatto malformato. È una
  lacuna dichiarata, non risolta.
