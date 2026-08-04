# 0006 — Contratto del segnale di relazione

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 02 — Arrivo dei segnali dalle altre app
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che dovrà far pubblicare i propri fatti a cinque applicazioni diverse
> voglio un contratto scritto e verificabile che dica esattamente che cosa un segnale può contenere e che cosa non
> può contenere
> così da poter aggiungere una fonte nuova senza riaprire la discussione, e senza che nessuno faccia entrare in
> RenewGrove dati che non ci devono stare.

**Contesto.** È **la storia più importante dell'applicazione**, e per una ragione che va detta senza addolcirla.
20 InsightGrove riceve aggregati, e può rivendicare che il caso peggiore di un difetto sia il trapelare di *un
numero*. RenewGrove riceve **fatti riferiti a un singolo cliente identificabile**, perché senza soggetto non si
può né formulare un giudizio né telefonare a qualcuno: quel contenimento del danno per costruzione **qui non
esiste, e la descrizione non lo rivendica** (§4.2). Ciò che lo sostituisce è tutto in questo contratto: un elenco
chiuso di tipi, il divieto assoluto di testo libero, l'assenza di campi anagrafici, e un validatore che rifiuta
dicendo quale regola è stata violata. Il divieto di testo libero, in particolare, è **il presidio che tiene fuori
dalla porta le categorie particolari dell'articolo 9** (§6): non esiste un rilevamento automatico del contenuto e
inventarlo sarebbe un presidio finto. Il contratto va scritto **prima** del consumatore (`0007`), perché è il
consumatore a doverlo far rispettare.

## 2. Requisiti funzionali

1. **RF-1** — Esiste uno schema versionato del segnale di relazione con questi campi **e nessun altro**:
   identificativo dell'account; applicazione d'origine; **riferimento opaco del rapporto**; **tipo di segnale**
   (elenco chiuso dichiarato per fonte); momento del fatto; **intensità numerica con unità dichiarata**; chiave di
   idempotenza; riferimento opaco alla riga d'origine.
2. **RF-2** — Il contratto **vieta esplicitamente** nel segnale: testo libero di qualunque genere; campi anagrafici
   (nome, indirizzo, recapito, identificativo fiscale); importi di documento in chiaro; contenuti di documento; e
   qualunque attributo riconducibile a una **categoria particolare** — salute, dati biometrici o genetici, opinioni
   politiche, convinzioni religiose, orientamento sessuale, appartenenza sindacale.
3. **RF-3** — Ogni applicazione d'origine **dichiara** l'elenco chiuso dei tipi di segnale che pubblica, con
   significato in italiano e inglese, unità dell'intensità e verso atteso. Un tipo non dichiarato è un tipo che non
   entra. L'elenco chiuso iniziale proposto è quello del §2.1 qui sotto.
4. **RF-4** — L'intensità è sempre un numero con un'unità dichiarata (conteggio, giorni, punti percentuali,
   moltiplicatore rispetto alla linea di base): non esistono intensità senza unità, perché confrontare due unità
   diverse è il modo più silenzioso di sbagliare un punteggio.
5. **RF-5** — I due riferimenti — del rapporto e della riga d'origine — sono **opachi**: identificano qualcosa
   nell'app che li ha prodotti e non dicono nulla di per sé. Servono ad aggregare (`0009`) e a rimandare all'origine,
   non a leggere.
6. **RF-6** — L'**etichetta leggibile** del rapporto **non sta nel segnale**: viaggia su un evento separato, con un
   consumatore distinto, così che il flusso dei segnali resti privo di dati anagrafici anche quando l'etichetta
   esiste.
7. **RF-7** — Esiste un **validatore eseguibile nei collaudi** che rifiuta un segnale non conforme e dice **quale
   regola** ha violato, con un collaudo di rifiuto per **ciascuno** dei divieti del RF-2.

### 2.1 Elenco chiuso iniziale dei tipi di segnale, per fonte — proposta

| Fonte | Tipo di segnale | Che cosa significa | Intensità e unità |
|---|---|---|---|
| **19 SubGrove** (`abbonati`) | `rata_non_rientrata` | Una rata attesa non è stata incassata | giorni di ritardo |
| | `sollecito_inviato` | La fonte ha mandato un sollecito su quel rapporto | conteggio dei solleciti nella catena |
| | `abbonamento_sospeso` | L'abbonamento è stato sospeso per mancato incasso | giorni di sospensione |
| | `disdetta_richiesta` | Il cliente finale ha chiesto la disdetta | giorni al termine effettivo |
| | `cambio_di_piano_in_riduzione` | Il cliente è passato a un piano inferiore | punti percentuali di riduzione |
| **02 BillGrove** / app reale `fatture` | `documento_emesso` | È stato emesso un documento a quel cliente | conteggio |
| | `pagamento_in_ritardo` | Un documento è scaduto e non risulta pagato | giorni di ritardo |
| | `documento_contestato` | Il cliente ha contestato un documento | conteggio |
| | `calo_del_ritmo_di_acquisto` | Il cliente compra meno spesso della propria linea di base | moltiplicatore rispetto alla linea di base |
| **12 DeskGrove** | `segnalazione_aperta` | Il cliente ha aperto una segnalazione di assistenza | conteggio |
| | `segnalazione_riaperta` | Una segnalazione chiusa è stata riaperta | conteggio delle riaperture |
| | `tempo_di_risposta_superato` | La risposta è arrivata oltre il tempo previsto | ore di scostamento |
| | `chiusura_con_insoddisfazione` | La segnalazione è stata chiusa con un giudizio negativo | punti sotto la soglia di soddisfazione |
| **07 BookGrove** | `prenotazione_disdetta` | Un appuntamento è stato disdetto dal cliente | giorni di preavviso |
| | `mancata_presentazione` | Il cliente non si è presentato all'appuntamento | conteggio |

Tre cose da notare nella tabella, perché sono decisioni e non dettagli. **`pagamento_in_ritardo` porta i giorni di
ritardo, non l'importo**: l'importo del documento è un dato che questa app non ha bisogno di conoscere e che il
RF-2 vieta. **`calo_del_ritmo_di_acquisto` è già uno scostamento dalla linea di base**, calcolato dalla fonte che
possiede lo storico degli acquisti: è coerente con il §2.5 della descrizione, dove il segnale utile non è il valore
assoluto ma lo scarto dalla normalità di quel cliente. E **`chiusura_con_insoddisfazione` porta una distanza da una
soglia, non il testo del giudizio**: il commento del cliente resta in DeskGrove, dove è nato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'identificativo dell'account nel segnale è quello che l'applicazione
  d'origine ha preso dal **proprio** token verificato: è l'unica chiave con cui il segnale verrà scritto. Il
  contratto lo dichiara obbligatorio e non nullo; un segnale senza account è un segnale invalido.
- **RT-2 — Interfaccia di programmazione (§2).** Il contratto vive **dentro il servizio `fidelizzazione`**,
  versionato con esso, e la sua descrizione è pubblicata in forma leggibile da un programma, così che le app
  sorgenti possano generarne il codice invece di riscriverlo. Nessuna rotta pubblica nuova.
- **RT-3 — Persistenza (§8).** La forma del contratto e la forma della tabella `segnale` creata in `0002` devono
  coincidere campo per campo; il vincolo di unicità su `(tenant_id, app_origine, chiave_idempotenza)` è parte del
  contratto, non un dettaglio di implementazione.
- **RT-4 — Dati personali (§10).** Il contratto è **il presidio principale sui dati personali di questa app**. Non
  introduce campi nuovi, ma **circoscrive** quelli già dichiarati in `0002`: il segnale è un fatto riferito a una
  persona identificabile per riferimento opaco, mai per anagrafica. Le descrizioni dei tipi di segnale entrano nel
  manifesto `docs/compliance/manifests/fidelizzazione.yaml` in **italiano e inglese** — sono due lingue, quelle del
  manifesto, non le cinque dell'interfaccia. Il divieto di testo libero è ciò che tiene fuori le categorie
  particolari dell'articolo 9, e va scritto nel manifesto come esclusione esplicita.
- **RT-5 — Esposizione conversazionale (§12).** Nessuno strumento introdotto: la storia scrive un contratto, non
  una funzione. Gli strumenti che leggeranno i segnali sono della storia `0028`; il server conversazionale è di
  piattaforma e non ancora implementato (UC 0061-0063).
- **RT-6 — Registrazione eventi (§14).** Il rifiuto di un segnale si registra con `tenant_id`, `app_id` d'origine,
  identificativo di correlazione e la **regola violata**; **mai** il contenuto del segnale rifiutato, che è
  precisamente il posto dove starebbe il dato che non doveva entrare.
- **RT-7 — Prove (§11).** Prove di unità sul validatore: un segnale conforme passa, e per **ognuno** dei divieti
  del RF-2 esiste un caso che viene rifiutato con la regola violata nominata.

## 4. Criteri di accettazione

**CA-1 — Un segnale conforme è accettato**
- **Dato** un segnale con account, applicazione d'origine `abbonati`, riferimento opaco del rapporto `r-1042`, tipo
  `rata_non_rientrata`, momento del fatto, intensità `17` con unità `giorni`, chiave di idempotenza e riferimento
  opaco alla riga d'origine
- **Quando** lo si passa al validatore
- **Allora** è accettato

**CA-2 — Un segnale con testo libero è rifiutato**
- **Dato** un segnale che porta un campo nota con dentro «Il titolare è in malattia, richiamare a settembre»
- **Quando** lo si passa al validatore
- **Allora** è rifiutato con la regola «nel segnale non è ammesso testo libero», e il contenuto del campo **non
  compare** nel registro applicativo

**CA-3 — Un campo anagrafico è rifiutato**
- **Dato** un segnale che porta il recapito di posta elettronica del cliente finale
- **Quando** lo si passa al validatore
- **Allora** è rifiutato con la regola «nel segnale non sono ammessi campi anagrafici», e lo stesso vale per
  indirizzo e identificativo fiscale

**CA-4 — Un importo di documento in chiaro è rifiutato**
- **Dato** un segnale di tipo `pagamento_in_ritardo` che porta, oltre ai giorni di ritardo, l'importo della fattura
- **Quando** lo si passa al validatore
- **Allora** è rifiutato con la regola «nel segnale non sono ammessi importi di documento»

**CA-5 — Un tipo non dichiarato è rifiutato**
- **Dato** l'applicazione d'origine `abbonati`, che ha dichiarato i cinque tipi dell'elenco chiuso
- **Quando** pubblica un segnale di tipo `stato_di_salute_del_titolare`
- **Allora** il segnale è rifiutato con la regola «tipo di segnale non dichiarato per questa fonte» — ed è anche il
  modo in cui un attributo dell'articolo 9 verrebbe fermato

**CA-6 — Intensità senza unità e segnale senza account**
- **Dato** un segnale con intensità `4` e nessuna unità, e un secondo segnale in cui l'account è assente
- **Quando** si passano al validatore
- **Allora** il primo è rifiutato con la regola «ogni intensità dichiara la propria unità» e il secondo con la prima
  regola dell'elenco, «il segnale dichiara l'account»

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sul validatore, con un caso di rifiuto per **ciascun** divieto del RF-2;
- [ ] prova di **isolamento fra account**: non applicabile qui — nessuna lettura e nessuna scrittura — ma il
      contratto è ciò che la rende possibile a valle, e va detto esplicitamente nel registro delle decisioni;
- [ ] **prova end-to-end**: *rimando* alla storia `0030`, che dovrà coprire il tragitto completo «una fonte
      pubblica un segnale conforme → il rapporto lo mostra»; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con motivo «percorso
      `[J-FIDELIZZAZIONE]` non ancora creato»;
- [ ] **traduzioni**: le descrizioni dei tipi di segnale sono in italiano e inglese — è il requisito del manifesto
      dei dati, due lingue, non quello dell'interfaccia, cinque;
- [ ] **manifesto dei dati** aggiornato: il contratto è la prova documentata che nel segnale non entrano anagrafica,
      testo libero, importi di documento né categorie particolari, e va scritto lì come esclusione esplicita;
- [ ] **registro delle decisioni** compilato: l'elenco chiuso iniziale per fonte, la scelta dell'unità di ciascun
      tipo, e la decisione di far viaggiare l'etichetta su un evento separato;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] documentazione aggiornata: il contratto è materiale che le app sorgenti dovranno leggere.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | la forma della tabella `segnale` e la forma del contratto devono coincidere |
| classificazione dei dati personali (§6 della descrizione, punto aperto n. 4) | la base giuridica della profilazione e l'informativa al cliente finale sono una fermata di escalation: vanno chiuse **prima** che il primo segnale entri davvero |

## 7. Fuori ambito

- il consumo dei segnali dalla coda: storia `0007`;
- l'implementazione della pubblicazione dentro le app sorgenti: è lavoro **loro**, in una loro storia. Qui si scrive
  il contratto che dovranno rispettare;
- l'aggregazione dei segnali su un rapporto e l'arrivo dell'etichetta leggibile: storia `0009`;
- i segnali registrati a mano da un utente: storia `0010`, che usa lo **stesso** elenco chiuso.

## 8. Punti aperti

- **I pesi che questi tipi avranno nel punteggio non sono decisi qui, e non esistono ancora.** Il §2.7 della
  descrizione dichiara che non esistono pesi validati per imprese non-software: i pesi di partenza saranno una
  convenzione dichiarata (storia `0012`), modificabile dal cliente (`0016`). Il contratto deve quindi restare
  **neutro** rispetto all'importanza dei tipi. Chiude: la storia `0012`.
- **Il contratto è di questa app o di piattaforma?** Se domani una seconda app volesse consumare gli stessi
  segnali, un contratto nato dentro `fidelizzazione` sarebbe nel posto sbagliato. Raccomandazione, la stessa di
  InsightGrove sul contratto del fatto di misura: **nasce qui e si promuove a piattaforma** quando servirà a due
  consumatori, senza anticipare. Chiude: la piattaforma.
- **Chi verifica che una fonte rispetti il contratto?** Il validatore rifiuta a valle, ma il difetto è a monte:
  oggi non esiste un modo di far fallire la compilazione di SubGrove se pubblica un segnale malformato. È una lacuna
  dichiarata, non risolta.
