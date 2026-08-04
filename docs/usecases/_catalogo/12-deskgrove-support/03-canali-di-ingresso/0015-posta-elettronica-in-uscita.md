# 0015 — Posta elettronica in uscita

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 03 — Canali di ingresso
**Storia**: `0015` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0012`, `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come operatore che risponde dalla coda di DeskGrove
> voglio che quello che scrivo arrivi davvero nella casella del cliente e che la sua replica torni sotto la stessa
> richiesta
> così da non dover uscire dall'app, aprire la posta, incollare, e poi ricopiare a mano la risposta che arriva.

**Contesto.** È la metà mancante del canale di posta: la storia `0014` fa entrare, questa fa uscire. Senza di essa
DeskGrove è un archivio di richieste in sola lettura, e l'operatore continua a rispondere dalla sua casella —
esattamente il problema che l'app esiste per togliere. È anche la storia che chiude il cerchio della ricucitura del
filo: il `Message-Id` che generiamo qui è ciò che la storia `0014` ritrova in `In-Reply-To` quando il cliente
replica.

Due regole ereditate, non negoziabili
([application-description.md](../application-description.md) §10). La prima: si **riusa** il generatore unificato
dei messaggi di `services/commons` (`EmailTemplateRenderer`) e la sorgente unica `shared/email-templates` —
DeskGrove aggiunge i **propri modelli**, non duplica il meccanismo. La seconda, dalla decisione 14 della change
`0084`: una **notifica** non contiene il testo della conversazione, dice che c'è un aggiornamento e porta alla
pagina dove l'accesso è controllato. Qui vale ancora di più, perché il destinatario è esterno.

⚠️ Questa storia **propone e non decide** l'identità del mittente: vedi §8, ed è la fermata che ne governa il
rilascio.

## 2. Requisiti funzionali

1. **RF-1** — Quando un operatore risponde su una richiesta di un canale di posta, il servizio spedisce al
   richiedente un messaggio che contiene il corpo della risposta, la firma dell'operatore e il piè di pagina
   configurato dal cliente; l'esito dell'invio è visibile sul messaggio nel filo.
2. **RF-2** — Ogni messaggio in uscita porta un `Message-Id` generato e registrato sul messaggio del filo, e le
   intestazioni `In-Reply-To` e `References` che lo legano al messaggio precedente della conversazione; il
   `Reply-To` è il recapito del canale, così che la replica rientri dalla porta della storia `0014`.
3. **RF-3** — Il messaggio si compone con il generatore unificato di `services/commons` e con modelli propri
   dell'app aggiunti in `shared/email-templates`; nessun testo di posta è scritto a mano dentro il servizio.
4. **RF-4** — Le **notifiche** verso il richiedente (per esempio «la tua richiesta è stata chiusa») non contengono
   il testo della conversazione: dicono che c'è un aggiornamento e portano alla pagina della richiesta. La
   **risposta dell'operatore** è invece essa stessa il contenuto, ed è per definizione ciò che il cliente deve
   leggere: le due cose sono modelli distinti e non si confondono.
5. **RF-5** — Un invio fallito viene riprovato un numero limitato di volte con attesa crescente; esaurito l'ultimo
   tentativo il messaggio è marcato «recapito fallito» con il motivo leggibile, la richiesta è segnalata
   nell'elenco e il testo scritto dall'operatore **non va perso**.
6. **RF-6** — Il piè di pagina di ogni messaggio in uscita porta l'identità del **cliente titolare** e la sua
   informativa, non testi di appgrove; è configurato per canale insieme all'informativa della storia `0013`.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'invio prende recapito del mittente, firma, piè di pagina e informativa
  **dal canale della richiesta**, che appartiene al `tenant_id` del token verificato; nessun parametro della
  richiesta di invio può indicare un canale o un mittente di un altro account. La registrazione del `Message-Id`
  generato avviene sotto lo stesso `tenant_id`, così che la ricucitura della storia `0014` resti dentro l'account.
- **RT-2 — Interfaccia di programmazione (§2).** L'invio è l'effetto della rotta di risposta già esistente
  (`POST /api/helpdesk/v1/tickets/{id}/messages`, storia `0007`), non una rotta nuova; si aggiungono
  `POST /api/helpdesk/v1/tickets/{id}/messages/{messageId}/retry` per il rinvio manuale e la lettura dell'esito nel
  corpo del messaggio. Errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__outbound_email.sql` sullo schema `app_helpdesk`: su
  `ticket_message` le colonne `outbound_message_id`, `delivery_status`, `delivery_error`, `delivery_attempts` e
  `delivered_at`; su `channel` le colonne del piè di pagina e dell'identità del mittente. Chiavi primarie UUID
  versione 7, colonne di controllo, cancellazione logica; indice su `(tenant_id, delivery_status)` per trovare i
  recapiti falliti.
- **RT-4 — Modulo frontend (§3, §5).** Nel filo della richiesta ogni messaggio in uscita mostra il proprio stato di
  recapito (in coda, consegnato, fallito con motivo) e il comando di rinvio; nell'elenco delle richieste compare un
  contrassegno per «recapito fallito». Sezione «Impostazioni → Canali → Posta» per firma e piè di pagina. Dati
  letti con il client generato; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le stringhe dell'interfaccia passano dallo spazio-nomi `helpdesk` e sono presenti
  in `en, it, fr, es, de`. I **modelli di posta** seguono invece la lingua del **richiedente** (`requester.locale`,
  storia `0012`) con ripiego sulla lingua del canale: chi riceve è un cliente finale, non un utente della
  piattaforma. I modelli vivono in `shared/email-templates`, che oggi porta italiano e inglese: le lingue mancanti
  vanno aggiunte o dichiarate come ripiego esplicito, non lasciate implicite.
- **RT-6 — Varchi e quota (§6, §7).** L'invio **non consuma** la metrica `agents`. Con abbonamento non attivo la
  risposta non parte (`402` sulla rotta di risposta) e l'app lo dice all'operatore prima che scriva, non dopo.
- **RT-7 — Esposizione conversazionale (§12).** È il punto dove la regola di sicurezza del catalogo diventa codice:
  `prepara_risposta(numero, indicazioni) → bozza di messaggio, non inviata` è **scrittura** e resta dentro l'app;
  `invia_risposta(id della bozza) → esito` è **scrittura irreversibile** e richiede una conferma umana esplicita su
  una bozza già scritta. I due passi **non si fondono mai**, nemmeno su richiesta dell'utente: preparare è
  reversibile, inviare no. Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non
  ancora implementato (UC 0061-0063), e l'implementazione degli strumenti è della storia `0035`.
- **RT-8 — Dati personali (§10).** Nessuna categoria di dati **nuova**: si spediscono verso il richiedente dati che
  l'app già tratta. Voci del manifesto `docs/compliance/manifests/helpdesk.yaml` da aggiornare in italiano e
  inglese: `message.body` (aggiungere la destinazione «trasmesso al richiedente per posta elettronica»),
  `agent.display_name` (compare nella firma visibile al cliente finale) e la voce nuova per lo stato di recapito.
  Campi annotati `@PersonalData`, `ticket_message` già presente in `exportData` e `purgeData`. Il vettore di posta
  è **sotto-responsabile** che trasporta contenuto di terzi, e va dichiarato come tale.
- **RT-9 — Registrazione eventi (§14).** Eventi «risposta inviata», «recapito confermato», «recapito fallito» con
  motivo di categoria (non il messaggio del fornitore parola per parola, che può citare l'indirizzo), «rinvio
  richiesto», tutti con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione. **Mai** il corpo del
  messaggio, **mai** l'indirizzo del destinatario.

## 4. Criteri di accettazione

**CA-1 — La risposta esce davvero**
- **Dato** una richiesta arrivata da un canale di posta e un operatore assegnato
- **Quando** l'operatore invia una risposta
- **Allora** il vettore riceve un messaggio con il corpo scritto, la firma dell'operatore, il piè di pagina del
  cliente e il `Reply-To` uguale al recapito del canale, e il messaggio nel filo risulta «in consegna»

**CA-2 — La replica torna nello stesso filo**
- **Dato** una risposta inviata, con il suo `Message-Id` registrato
- **Quando** il richiedente replica e il messaggio rientra dalla porta della storia `0014` con quel valore in
  `In-Reply-To`
- **Allora** la replica compare **nella stessa richiesta**, e non ne nasce una nuova

**CA-3 — La notifica non racconta la conversazione**
- **Dato** una richiesta appena chiusa
- **Quando** parte la notifica al richiedente
- **Allora** il messaggio dice che c'è un aggiornamento e porta alla pagina della richiesta, e **non contiene** il
  testo dei messaggi

**CA-4 — Recapito fallito**
- **Dato** un richiedente con un indirizzo inesistente
- **Quando** i tentativi di invio si esauriscono
- **Allora** il messaggio è marcato «recapito fallito» con motivo leggibile, la richiesta è contrassegnata
  nell'elenco, il testo scritto dall'operatore resta nel filo e il comando di rinvio è disponibile

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con il proprio canale di posta, firma e piè di pagina
- **Quando** un utente di `A` risponde a una richiesta di `A` indicando nel corpo della chiamata il canale di `B`
- **Allora** il messaggio esce con il mittente, la firma e il piè di pagina di `A`, e nessun messaggio può essere
  spedito a nome di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla composizione delle intestazioni del filo e sulla scelta del modello per lingua, e di
      **integrazione** sull'invio con vettore **simulato**, database effimero e migrazioni Flyway vere;
- [ ] prova di **isolamento fra account** sulla scelta di canale, firma e piè di pagina in fase di invio;
- [ ] **prova end-to-end**: *coprire ora* — il passo «l'operatore risponde e il messaggio esce dal vettore
      simulato, con il filo ricucito» entra nel percorso `[J-HELPDESK]`; voce corrispondente nel **registro di
      copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml), con l'etichetta
      `[J-HELPDESK]` in testa al titolo del test;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per l'interfaccia; per i modelli di posta, lingue
      disponibili e ripieghi dichiarati esplicitamente;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per la trasmissione del corpo e per la firma
      dell'operatore, con il vettore di posta dichiarato sotto-responsabile su contenuto di terzi;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotata in modo esplicito la
      **fermata sull'identità del mittente** (§8) e la scelta fatta nell'attesa;
- [ ] contratto degli **strumenti conversazionali** dichiarato: `prepara_risposta` (scrittura, bozza) e
      `invia_risposta` (scrittura irreversibile, conferma umana obbligatoria), con il divieto di fonderli scritto
      nel contratto;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali, con il vettore **sempre
      simulato** in locale: nessun messaggio vero parte da un portatile;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` — filo dei messaggi e risposta | L'invio è l'effetto della risposta: la risposta deve esistere |
| Storia `0012` — canali e anagrafica del richiedente | Mittente, firma e piè di pagina vengono dal canale; il destinatario dal richiedente |
| Storia `0014` — posta elettronica in ingresso | Senza la porta di ingresso la replica non rientra e il filo si spezza a metà: le due storie si completano |
| Generatore unificato dei messaggi di `services/commons` e `shared/email-templates` (UC 0085) | Esiste ed è un dovere riusarlo: DeskGrove aggiunge modelli, non meccanismi |
| **Decisione sull'identità del mittente (§8)** | Senza di essa la storia si implementa con l'identità come parametro del canale, ma il **valore predefinito** e la procedura di attivazione non si possono fissare |
| Infrastruttura di invio della piattaforma fuori dalla modalità di prova (UC 0018 / UC 0078) | Necessaria per spedire a destinatari reali; in locale e nei collaudi il vettore è simulato |

## 7. Fuori ambito

- **Gli allegati nei messaggi in uscita**: li introduce la storia `0016`;
- **La conferma automatica di ricezione al visitatore del modulo web**: dipende dalla stessa decisione sull'identità
  del mittente e si aggiunge come modello quando la fermata è sciolta — resta fuori da qui per non far dipendere il
  modulo web (`0013`) da una decisione aperta;
- **Gli avvisi di scadenza agli operatori**: sono destinatari interni e appartengono alla storia `0026`;
- **L'invito a votare la soddisfazione**: è la storia `0027`, che riusa i modelli introdotti qui;
- **L'implementazione degli strumenti conversazionali** `prepara_risposta` e `invia_risposta`: qui se ne dichiara il
  contratto, li realizza la storia `0035`;
- **La sorveglianza dei recapiti falliti a livello di piattaforma** (reputazione del dominio, rimbalzi ripetuti,
  segnalazioni di posta indesiderata): è materia della console di amministrazione e dell'infrastruttura di invio.

## 8. Punti aperti

> ⚠️ **Fermata: l'identità del mittente non la decide questa storia.** È il punto 10 dei rischi della descrizione
> dell'applicazione. La storia costruisce l'identità come **parametro del canale**, così che nessuna delle due
> strade venga preclusa; ma **quale sia il valore predefinito** e quale procedura di attivazione si offra al
> cliente è una decisione con effetti verso l'esterno e sulla percezione del prodotto. Chiude: **sviluppatore**.

**Strada A — il cliente verifica il proprio dominio.** Al cliente si chiede di inserire alcuni record nella
configurazione del proprio nome a dominio, così che i nostri invii risultino autorizzati a scrivere per suo conto.

- *Conseguenze buone*: il messaggio parte da `assistenza@cliente.it`, il cliente finale vede un mittente che
  riconosce, la recapitabilità è la migliore possibile e la reputazione di ciascun cliente è la propria — chi si
  comporta male non danneggia gli altri.
- *Conseguenze cattive*: il cliente micro spesso **non sa** dove si mettono quei record e non ha chi glielo faccia;
  l'attivazione si blocca al primo passo, e ogni blocco al primo passo è un cliente perso. Serve una procedura
  guidata con verifica automatica, e comunque qualcuno che risponda quando non funziona.

**Strada B — si spedisce da un dominio di appgrove con l'indirizzo di risposta del cliente.** Il messaggio parte da
un recapito nostro (per esempio `richiesta-<codice>@…`), con nome visualizzato del cliente e `Reply-To` verso il
canale del cliente.

- *Conseguenze buone*: attivazione immediata, zero configurazione, il canale funziona il primo giorno; le repliche
  tornano comunque nel filo perché il `Reply-To` è nostro.
- *Conseguenze cattive*: il cliente finale vede un mittente **estraneo** al marchio con cui ha comprato — e nel
  segmento micro il rapporto personale è metà del valore; la reputazione del dominio è **condivisa fra tutti i
  clienti**, quindi un solo cliente che manda messaggi indesiderati peggiora la recapitabilità di tutti; alcuni
  filtri trattano con sospetto un mittente diverso dal dominio citato nel testo.

**Terza via da valutare, non da dare per scelta**: strada B come predefinito per far partire subito il cliente, con
la strada A offerta come miglioramento facoltativo e la promessa di passare automaticamente ad A quando la verifica
del dominio riesce. Ha il pregio di non bloccare nessuno e il difetto di dover mantenere entrambe le vie.

**Altri punti aperti**

- **Lingue dei modelli di posta.** `shared/email-templates` porta oggi italiano e inglese; i richiedenti dei
  clienti europei scrivono anche in francese, spagnolo e tedesco. Aggiungere le tre lingue è lavoro di traduzione
  che appartiene alla piattaforma, non a questa storia. Chiude: sviluppatore, insieme a chi possiede UC 0085.
- **Numero e distanza dei tentativi di rinvio.** Troppo insistenti si diventa un mittente sgradito, troppo pochi si
  perde una risposta per un guasto momentaneo. Chiude: sviluppatore.
- **Se avvisare l'operatore del recapito fallito con una notifica** oltre al contrassegno nell'elenco: è una scelta
  di prodotto sul rumore. Chiude: sviluppatore, insieme alla storia `0026`.
