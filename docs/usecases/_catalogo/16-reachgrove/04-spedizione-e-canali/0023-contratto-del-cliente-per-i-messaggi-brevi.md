# 0023 — Contratto del cliente per i messaggi brevi

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 04 — Spedizione e canali
**Storia**: `0023` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha già un contratto con un fornitore di messaggi brevi
> voglio collegarlo a ReachGrove con le mie credenziali
> così da mandare i miei messaggi pagando il mio fornitore, senza passare da un rivenditore.

**Contesto.** Un messaggio breve verso l'Italia costa circa 0,0927 $ al fornitore, mille volte un messaggio di
posta elettronica ([application-description.md](../application-description.md) §2.6 fonte 8). Rivenderlo dentro un
abbonamento significherebbe o alzare il prezzo per tutti o perdere denaro su chi lo usa; ed è la prima delle tre
ragioni per cui il catalogo ha escluso l'app 05 ChatGrove (§11.1).

La soluzione è semplice e onesta: **il contratto è del cliente**. Le credenziali sono sue, la fattura gli arriva dal
suo fornitore, appgrove non compra e non rivende niente. Noi mettiamo quello che il fornitore non ha: chi si può
contattare e perché, la disiscrizione onorata, il tracciamento.

C'è un punto normativo che questa storia deve rendere impossibile da sbagliare: l'eccezione del cosiddetto «soft
spam» — poter riusare l'indirizzo raccolto durante una vendita per promuovere prodotti analoghi — vale **solo per
la posta elettronica** e il Garante ne ha escluso l'estensione ai messaggi brevi (§2.3 punto 2). Un iscritto la cui
unica base giuridica è quella **non è contattabile** su questo canale, e non è una configurazione: è una regola.

## 2. Requisiti funzionali

1. **RF-1** — L'account collega il proprio contratto inserendo le credenziali del fornitore e il mittente
   registrato; l'app **verifica subito** la connessione con una chiamata di prova e mostra l'esito.
2. **RF-2** — Le credenziali si possono **sostituire** e **revocare**, ma non rileggere: dopo il salvataggio
   l'interfaccia ne mostra solo la forma abbreviata e lo stato.
3. **RF-3** — Gli **errori del fornitore** vengono riportati al cliente **in chiaro**, con il codice e il messaggio
   originale accanto alla spiegazione in lingua: è il suo contratto, e chi deve chiamare l'assistenza del
   fornitore è lui.
4. **RF-4** — Sul canale «messaggi brevi» è inviabile **solo** l'iscritto con un consenso registrato **per questo
   canale**. La base giuridica `soft_spam` **non abilita** questo canale: chi la porta come unica base viene
   escluso, con motivo esplicito.
5. **RF-5** — Ogni messaggio breve porta un modo di **disiscriversi** conforme al canale (una parola chiave di
   risposta o un collegamento breve), e la revoca così raccolta entra nel registro del consenso come tutte le
   altre.
6. **RF-6** — Il consumo del canale si conta sul **tetto di spesa** della storia `0022` e sulla metrica
   `messages_sent`; il costo che il cliente paga al proprio fornitore **non passa** dalla nostra fatturazione e non
   viene stimato in fattura.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La connessione, le credenziali e i consumi filtrano per `tenant_id`
  preso dal token verificato. Un invio parte **sempre** con le credenziali dell'account proprietario della
  campagna: un percorso di consegna che non riesce a risolvere la connessione dell'account deve fallire, mai
  ricadere su un'altra.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `POST /api/campaigns/v1/channels/sms/connection` (collega e verifica),
  `DELETE /api/campaigns/v1/channels/sms/connection` (revoca),
  `POST /api/campaigns/v1/channels/sms/connection/test`. Le credenziali si accettano **solo** in scrittura e non
  compaiono in nessuna risposta. Errori in `application/problem+json` con il codice del fornitore in un campo
  dedicato; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Riga di `channel_connection` con tipo `messaggi_brevi`; il segreto vive in un
  deposito di segreti e in tabella se ne conserva **solo il riferimento**, insieme a stato, mittente registrato,
  esito e momento dell'ultima verifica. Schema `app_campaigns`, chiave primaria UUID versione 7, colonne di
  controllo, cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Scheda del canale nella sezione «Canali»: modulo di collegamento, esito
  della prova, stato, errori del fornitore, revoca con conferma esplicita. Il testo di avvertenza sulla base
  giuridica va **accanto al collegamento**, non in una pagina di aiuto. Solo token del sistema di design; tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, stati, spiegazioni degli errori e l'avvertenza sul soft spam presenti
  in `en, it, fr, es, de`. Il **messaggio originale del fornitore** non si traduce: è una citazione, e tradurla
  renderebbe impossibile cercarlo nella documentazione del fornitore.
- **RT-6 — Varchi e quota (§6, §7).** Ogni messaggio breve prenota una unità di `messages_sent` (natura `flow`) —
  la regola del doppio conteggio spiegata nella storia `0022` — e consuma il tetto di spesa. Il canale è
  disponibile solo sul piano che lo comprende: altrimenti `402`.
- **RT-7 — Esposizione conversazionale (§12).** `stato_iscritto` (lettura, storia `0034`) risponde **canale per
  canale**, e questa storia è il motivo per cui deve farlo: «posso scrivergli?» ha risposte diverse sulla posta
  elettronica e sui messaggi brevi. Il collegamento delle credenziali **non** è esposto alla chat: sono segreti e
  un impegno di spesa. Scelta dichiarata.
- **RT-8 — Dati personali (§10).** Attivando questo canale il **numero di telefono** degli iscritti viene trasmesso
  a un fornitore esterno che lo tratta per conto del cliente: voce nel manifesto
  `docs/compliance/manifests/campaigns.yaml` in italiano e inglese, con la destinazione dichiarata. Il fornitore va
  elencato fra quelli che trattano dati (§6 della descrizione, integrazione 2). Le credenziali non compaiono in
  `exportData`; `channel_connection` entra in `purgeData`.
- **RT-9 — Registrazione eventi (§14).** «Canale messaggi brevi collegato», «prova di connessione con esito»,
  «credenziali revocate», «invio respinto dal fornitore con codice», con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione. **Mai** le credenziali e **mai** i numeri di telefono: si registra
  l'identificativo dell'iscritto.

## 4. Criteri di accettazione

**CA-1 — Collegamento e prova**
- **Dato** un account sul piano che comprende i canali aggiuntivi
- **Quando** inserisce credenziali valide del proprio fornitore
- **Allora** la prova di connessione riesce, il canale risulta attivo e le credenziali **non** sono più leggibili
  da nessuna rotta né dall'interfaccia

**CA-2 — Credenziali sbagliate, errore utile**
- **Dato** credenziali non valide
- **Quando** si tenta il collegamento
- **Allora** il canale **non** si attiva e il cliente vede il codice e il messaggio originale del fornitore accanto
  alla spiegazione in lingua, così da poter aprire una segnalazione con il **suo** fornitore

**CA-3 — Il soft spam non abilita questo canale**
- **Dato** un iscritto la cui unica base giuridica registrata è `soft_spam` e che ha un numero di telefono
- **Quando** viene incluso in una campagna su messaggi brevi
- **Allora** risulta **non inviabile**, con motivo «base giuridica non valida per questo canale»; nessun ruolo e
  nessun parametro produce un esito diverso

**CA-4 — Disiscrizione dal canale**
- **Dato** un destinatario che risponde con la parola chiave di rifiuto
- **Quando** il ritorno viene elaborato
- **Allora** viene registrata una revoca sul canale «messaggi brevi», il recapito entra nella soppressione per quel
  canale e il messaggio successivo non parte

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, entrambi con il canale collegato
- **Quando** una campagna di `A` viene spedita
- **Allora** parte con le credenziali di `A`; nessuna richiesta e nessun errore di configurazione fa usare quelle
  di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla regola «`soft_spam` non abilita i messaggi brevi» e sulla risoluzione della
      connessione per account; prove di **integrazione** con fornitore simulato, comprese credenziali non valide e
      rifiuto del fornitore;
- [ ] prova di **isolamento fra account** su connessione, consumi e invio;
- [ ] prova esplicita che le credenziali **non escono** da nessuna rotta, da nessun registro e da nessuna
      esportazione;
- [ ] **prova end-to-end**: rimando motivato — richiede un contratto esterno; coperta da prove di integrazione con
      fornitore simulato. Voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con motivo e questa storia come
      proprietaria;
- [ ] **traduzioni** in tutte e cinque le lingue, con i messaggi del fornitore esclusi dalla traduzione;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese: trasmissione del numero di telefono al fornitore del
      cliente, fornitore elencato fra quelli che trattano dati;
- [ ] **registro delle decisioni** compilato, con annotato che appgrove non rivende invii e perché il soft spam si
      ferma alla posta elettronica;
- [ ] contratto degli **strumenti conversazionali**: `stato_iscritto` risponde per canale; collegamento **non**
      esposto, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` | Il consenso per canale e l'elenco chiuso delle basi giuridiche sono la struttura su cui poggia la regola del soft spam |
| Storia `0022` | L'astrazione del canale, l'attivazione per account e il tetto di spesa |
| Scelta dei fornitori ammessi | Quali fornitori di messaggi brevi si possono collegare è una decisione dello sviluppatore: ogni fornitore è un'interfaccia da scrivere e mantenere |

## 7. Fuori ambito

- la **messaggistica**: è la storia `0024`, con un problema in più (il trasferimento fuori dall'Unione europea);
- la rivendita di messaggi brevi: esclusa per scelta di prodotto;
- la conversazione in entrata: una risposta diversa dalla parola chiave di rifiuto **non** apre niente qui — è
  l'app 12 DeskGrove ([application-description.md](../application-description.md) §10);
- il confronto con il Registro pubblico delle opposizioni: fuori perimetro, l'app non compone numeri e non fa
  telefonate (§1);
- la riconciliazione della fattura del fornitore del cliente: non passa da noi.

## 8. Punti aperti

- **Quali fornitori supportare al lancio.** Ognuno è un'interfaccia da scrivere e da mantenere; partire con uno
  solo è ragionevole ma vincola i clienti che ne hanno un altro. Chiude lo sviluppatore.
- **Forma della disiscrizione sul canale.** La parola chiave di risposta richiede un numero che possa **ricevere**,
  e non tutti i contratti lo prevedono; il collegamento breve richiede un dominio nostro dentro un messaggio che il
  cliente paga a carattere. Proposta: supportare entrambe e sceglierla in base a ciò che il contratto del cliente
  consente. Da confermare.
- **Chi è responsabile del trattamento verso il fornitore.** Il contratto è del cliente, ma la trasmissione la
  eseguiamo noi: il confine va scritto nel contratto di trattamento, non solo nel manifesto. Chiude la revisione
  legale, insieme al punto della storia `0024`.
