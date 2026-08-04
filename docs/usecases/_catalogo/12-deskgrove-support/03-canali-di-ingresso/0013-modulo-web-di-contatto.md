# 0013 — Modulo web di contatto

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 03 — Canali di ingresso
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio online
> voglio che chi ha un problema lo scriva da una pagina del mio sito e la richiesta arrivi già tracciata in
> DeskGrove
> così da smettere di raccogliere le domande dei clienti da una casella `info@` che nessuno presidia.

**Contesto.** È l'unico canale che possiamo aprire **senza dipendere da nessuno**: nessun fornitore esterno,
nessuna infrastruttura di ricezione della posta da aspettare, nessun sotto-responsabile in più
([application-description.md](../application-description.md) §2.4). È anche la **prima superficie di DeskGrove
esposta a Internet senza autenticazione**, e quindi la storia dell'epica che porta il peso maggiore di sicurezza:
chi la compila non è un utente della piattaforma, non ha un token, e non gli si può chiedere di registrarsi — è
proprio l'aspettativa rilevata nel segmento (§2.5, «che il cliente finale non debba registrarsi»).

Il punto delicato è l'invariante numero uno. Il `tenant_id` **non può** arrivare da un utente che non c'è: arriva
dall'**identificativo pubblico del canale**, che è la sola cosa che il visitatore possiede. Ne discende una regola
di progetto stretta: sotto il percorso pubblico esiste **una sola operazione possibile**, creare una richiesta su
quel canale. Niente lettura, niente elenco, niente ricerca, niente conferma dell'esistenza di un indirizzo.

## 2. Requisiti funzionali

1. **RF-1** — Per un canale di tipo `modulo web` il servizio genera una **chiave pubblica opaca** e l'interfaccia
   mostra il frammento da incollare nel proprio sito; la chiave si può rigenerare, e da quel momento la vecchia
   smette di funzionare.
2. **RF-2** — Il modulo raccoglie un insieme **fisso** di campi — nome, indirizzo di posta, oggetto, messaggio —
   con indirizzo di posta e messaggio obbligatori; non esistono campi liberi aggiunti dal cliente.
3. **RF-3** — Un invio valido crea una richiesta nello stato `aperta` sul canale, riusando il richiedente
   riconosciuto dalla storia `0012`, e restituisce **solo** una conferma neutra con il numero della richiesta.
4. **RF-4** — L'invio è protetto da un limite di frequenza per indirizzo di rete e da un campo-trappola invisibile
   alle persone; gli invii respinti non creano nulla e vengono contati per motivo.
5. **RF-5** — L'indirizzo di rete di chi invia è conservato **30 giorni** e poi cancellato da una lavorazione
   periodica, senza intervento umano.
6. **RF-6** — Il modulo mostra il testo dell'informativa **scritto dal cliente titolare** (chi tratta i dati, per
   quali finalità, per quanto tempo, come esercitare i diritti): finché quel testo è vuoto la chiave pubblica non
   si genera e il modulo non si pubblica.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le rotte autenticate di configurazione filtrano per `tenant_id` preso
  dal token verificato. La rotta **pubblica non ha token**: l'account si ricava esclusivamente dalla **chiave
  pubblica del canale** — valore casuale a 128 bit, non derivato dal tempo né dall'identificativo del canale,
  quindi non indovinabile né enumerabile — e **mai** da un parametro fornito da chi invia. Un `tenant_id`, una coda
  o un operatore presenti nel corpo dell'invio vengono ignorati.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta pubblica
  `POST /api/helpdesk/public/v1/channels/{publicKey}/tickets`, esplicitamente fuori dalla catena di
  autenticazione; **è l'unica operazione esposta sotto `/public/`** — ogni altro metodo o percorso risponde come
  inesistente. Validazione severa dei campi (lunghezze massime, formato dell'indirizzo); errori in
  `application/problem+json` **senza dettagli utili a chi sonda**; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__web_form_channel.sql` sullo schema `app_helpdesk`: colonne
  `public_key` (unica globalmente) e `privacy_notice` su `channel`, colonna `source_ip` su `ticket`, tabella
  `channel_submission_reject` con `tenant_id` per i conteggi dei rifiuti; tutte con chiave primaria UUID versione
  7, colonne di controllo e cancellazione logica. Indice su `(tenant_id, created_at)` per i conteggi e indice sulla
  data di `source_ip` per la lavorazione periodica dei 30 giorni.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Impostazioni → Canali → Modulo web» del modulo `helpdesk` per la
  configurazione e l'anteprima; la **pagina pubblica del modulo è servita a parte**, minima, senza il guscio del
  backoffice e senza il client autenticato. Solo token del sistema di design; tema chiaro e scuro; controllo
  automatico di accessibilità verde, perché è la pagina più esposta dell'app.
- **RT-5 — Cinque lingue (§4).** L'interfaccia di configurazione passa dallo spazio-nomi `helpdesk` ed è presente
  in `en, it, fr, es, de`. Il **modulo pubblico** invece è nella lingua scelta dal cliente per quel canale e i suoi
  testi sono modificabili da lui: chi lo compila è un visitatore del suo sito, non un utente della piattaforma.
- **RT-6 — Varchi e quota (§6, §7).** La rotta pubblica non attraversa il varco di autenticazione (non c'è un
  `401` possibile), ma **rispetta quello di abilitazione**: con abbonamento non attivo il modulo risponde che non è
  disponibile e non crea nulla, senza spiegare perché. Non consuma la metrica `agents`: una richiesta non è un
  posto operatore. Il limite di frequenza è un presidio di sicurezza, **non** una quota di piano, e il suo
  superamento non è un `429` di quota.
- **RT-7 — Esposizione conversazionale (§12).** La configurazione del modulo **non** è esposta come strumento:
  genera una superficie pubblica, ed è un effetto verso l'esterno. Le richieste arrivate si leggono con
  `elenca_richieste` filtrando per canale. Il contratto vive dentro il servizio; il server conversazionale è di
  piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Voce nuova nel manifesto `docs/compliance/manifests/helpdesk.yaml` in italiano e
  inglese: `webform.ip` → `ticket.source_ip`, interessato «cliente finale dell'azienda cliente», categoria «dato
  tecnico di collegamento», finalità «difesa dall'abuso del modulo pubblico», base giuridica «legittimo interesse
  del titolare alla sicurezza», conservazione **30 giorni**. Campo annotato `@PersonalData`; `ticket` è già in
  `exportData` e `purgeData` e vi resta. Il corpo del messaggio entra qui da una **persona esterna che non ha letto
  alcuna informativa nostra**: si applica il contrassegno per la revisione umana introdotto dalla storia `0002`
  (riconoscitore deterministico a radici di parole), che segnala **senza registrare quale** categoria sarebbe stata
  riconosciuta. Nessun servizio esterno di analisi del testo. Nessun fornitore esterno nuovo: il modulo è nostro.
- **RT-9 — Registrazione eventi (§14).** Eventi «invio ricevuto», «invio respinto per limite di frequenza»,
  «invio respinto come automatico», «invio respinto per canale non disponibile», con `tenant_id`, `app_id`,
  identificativo del canale e identificativo di correlazione. `user_id` è assente per costruzione (non c'è
  utente). **Mai** il contenuto inviato, **mai** l'indirizzo di posta, **mai** l'indirizzo di rete nei registri:
  quest'ultimo vive solo nella colonna dedicata, con la sua scadenza.

## 4. Criteri di accettazione

**CA-1 — L'invio diventa una richiesta**
- **Dato** un canale `modulo web` attivo con informativa compilata e chiave pubblica generata
- **Quando** un visitatore invia indirizzo di posta e messaggio validi
- **Allora** nasce una richiesta `aperta` su quel canale, collegata al richiedente riconosciuto, e la risposta
  contiene solo il numero della richiesta

**CA-2 — Il campo-trappola blocca l'invio automatico**
- **Dato** un modulo pubblicato
- **Quando** arriva un invio con il campo-trappola valorizzato
- **Allora** nulla viene creato, il rifiuto è contato come «automatico» e la risposta è indistinguibile da quella
  di un invio accettato

**CA-3 — Limite di frequenza**
- **Dato** più invii dallo stesso indirizzo di rete oltre la soglia configurata
- **Quando** arriva quello in eccesso
- **Allora** viene respinto, nulla viene creato, il rifiuto è contato e la risposta non rivela la soglia

**CA-4 — Nessuna informazione trapela**
- **Dato** un indirizzo di posta già presente fra i richiedenti dell'account
- **Quando** un visitatore lo usa nel modulo
- **Allora** la risposta pubblica è **identica** a quella di un indirizzo mai visto: nessun nome, nessun numero di
  richiesta precedente, nessun operatore

**CA-5 — Canale non disponibile**
- **Dato** un account con abbonamento `canceled`, oppure un canale in stato `sospeso`
- **Quando** arriva un invio alla chiave pubblica
- **Allora** riceve una risposta di non disponibilità senza motivo esposto e nulla viene creato

**CA-6 — Isolamento fra account**
- **Dato** le chiavi pubbliche di due account `A` e `B`
- **Quando** si invia alla chiave di `A` aggiungendo nel corpo `tenant_id`, coda e operatore di `B`
- **Allora** la richiesta nasce in `A` con la coda predefinita di `A`, e nessuna combinazione di parametri riesce a
  farla nascere in `B` né a leggere alcunché di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla validazione pubblica, sul campo-trappola e sul limite di frequenza, e di
      **integrazione** sul percorso non autenticato, con database effimero e migrazioni Flyway vere;
- [ ] prova di **isolamento fra account** sulla rotta pubblica, con tentativi di forzare l'account dai parametri e
      con chiave pubblica inesistente o revocata;
- [ ] **prova end-to-end**: *coprire ora* — l'invio dal modulo pubblico e la comparsa della richiesta nella coda
      sono il primo passo del percorso `[J-HELPDESK]`; voce corrispondente nel **registro di copertura**
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml), con l'etichetta `[J-HELPDESK]` in
      testa al titolo del test;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per l'interfaccia di configurazione;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la voce `webform.ip`, campo annotato
      `@PersonalData`, tabella presente in esportazione e cancellazione, conservazione di 30 giorni verificata da
      una prova sulla lavorazione periodica;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotate le scelte di sicurezza
      della superficie pubblica e la regola «una sola operazione sotto `/public/`»;
- [ ] contratto degli **strumenti conversazionali**: configurazione del modulo non esposta, motivo annotato;
- [ ] controllo automatico di **accessibilità** verde sulla pagina pubblica;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0012` — canali e anagrafica del richiedente | Il modulo è un canale, e l'invio deve riusare il richiedente riconosciuto invece di crearne uno nuovo a ogni messaggio |
| Storia `0006` — apertura manuale di una richiesta | La creazione della richiesta e le sue regole esistono già: qui cambia solo chi la origina |
| Storia `0002` — modello dati multi-account | Fornisce il contrassegno per la revisione umana sul testo libero, che qui si applica a contenuto scritto da persone esterne |
| Rotta pubblica nel proxy locale | Le rotte `/api/helpdesk/v1/*` sono generate dalla scoperta automatica dei servizi; il percorso `/api/helpdesk/public/v1/*` deve risultare raggiungibile **senza** modifiche a mano agli script: se non lo è, è un difetto della scoperta automatica da correggere lì |

## 7. Fuori ambito

- **Il caricamento di file da parte del visitatore**: lo introduce la storia `0016` (allegati dei messaggi), che ha
  la sua fermata sull'assenza di controllo antivirus — accettare file da chiunque su una superficie pubblica è un
  problema diverso e va deciso lì;
- **Il suggerimento di articoli mentre si scrive la domanda**: è la storia `0033`;
- **La conferma automatica di ricezione via posta al visitatore**: è un invio verso l'esterno e appartiene alla
  storia `0015` (posta elettronica in uscita), che possiede anche la decisione sull'identità del mittente;
- **Il collegamento con cui il visitatore segue la propria richiesta senza account**: è la storia `0032`;
- **Campi personalizzati del modulo**: deliberatamente esclusi, per non trasformare una porta pubblica in un
  raccoglitore di qualunque cosa — e perché ogni campo nuovo è una via d'ingresso in più per dati che non abbiamo
  chiesto.

## 8. Punti aperti

- **L'informativa che blocca la pubblicazione.** RF-6 impedisce di pubblicare il modulo finché il cliente non ha
  scritto la propria informativa. È la scelta corretta dal punto di vista della protezione dei dati — appgrove è
  responsabile e non può raccogliere dati di terzi senza che il titolare abbia informato — ma è **attrito
  commerciale** al primo minuto d'uso. L'alternativa (pubblicare con un testo minimo predefinito) sposta un rischio
  legale sul cliente senza che se ne accorga. Chiude: **sviluppatore**, come decisione di prodotto, con la
  revisione legale pre-go-live ([docs/_REVISIONE-LEGALE.md](../../../../_REVISIONE-LEGALE.md)).
- **Soglia del limite di frequenza.** Quanti invii per indirizzo di rete e in quale finestra: troppo stretta blocca
  un ufficio dietro un solo indirizzo di uscita, troppo larga non difende. Proposta: soglia predefinita conservativa
  e nessun elenco di blocco gestito a mano. Chiude: sviluppatore.
- **Conservazione dell'indirizzo di rete a 30 giorni** — proposta della descrizione dell'applicazione (§6), da
  confermare nel manifesto insieme al resto della classificazione. Chiude: sviluppatore.
- **Se il rifiuto vada mostrato al visitatore.** Rispondere «respinto» insegna a chi abusa come aggirare il
  presidio; rispondere sempre «ricevuto» inganna una persona vera che è finita nel limite per sbaglio. Questa
  storia sceglie la risposta indistinguibile per il campo-trappola e una risposta esplicita di non disponibilità
  per il canale spento; il caso del limite di frequenza resta da confermare. Chiude: sviluppatore.
