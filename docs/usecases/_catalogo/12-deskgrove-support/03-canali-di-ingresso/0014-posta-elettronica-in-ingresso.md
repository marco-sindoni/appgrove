# 0014 — Posta elettronica in ingresso

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 03 — Canali di ingresso
**Storia**: `0014` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0009`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile del servizio clienti che oggi vive dentro `assistenza@`
> voglio che i messaggi che arrivano a quella casella diventino da soli richieste tracciate, con le risposte del
> cliente attaccate al filo giusto
> così da non dover chiedere ai clienti di cambiare abitudine: continuano a scrivere dove hanno sempre scritto.

**Contesto.** È il canale che il mercato dà per scontato: la casella condivisa è il punto di partenza di ogni
cliente di questo segmento ([application-description.md](../application-description.md) §2.4, integrazione numero
uno). Ed è anche la ragione per cui l'app sostituisce davvero la casella di posta invece di affiancarsi ad essa.

**Dipendenza dichiarata, e non risolvibile qui: la piattaforma appgrove non ha ancora la ricezione della posta.**
Lo dice il punto aperto «Instradamento email in ingresso» di
[UC 0075](../../../15-supporto-e-piattaforma/0075-ticketing-nativo-in-house.md): la ricezione su `privacy@` e
`support@` è stata rimandata dalla change `0084` perché dipende dall'uscita del vettore di posta dalla modalità di
prova e dalla verifica del dominio (UC 0018 / UC 0078), non è collaudabile in locale e ha effetti verso l'esterno.
Quella storia ha lasciato in eredità la cosa giusta: la colonna della provenienza ammette già il valore `email`,
così il giorno in cui la ricezione arriva non serve una migrazione.

**Cosa si fa nel frattempo, ed è la scelta di progetto di questa storia.** Si costruisce **tutto il lato
applicativo** e non si costruisce il trasporto. Il servizio espone una **porta di ingresso interna** che accetta un
messaggio **già normalizzato** (mittente, destinatario, oggetto, corpo, identificativi del filo, intestazioni di
servizio) e ne fa una richiesta o un messaggio nel filo. Il collegamento con il vettore di posta della piattaforma
sta **dietro un interruttore di configurazione, spento**, e si accenderà quando la ricezione esisterà. Così il
comportamento che conta — la ricostruzione del filo, la difesa dai doppioni, le risposte automatiche — è scritto e
collaudato oggi, con carichi sintetici; e il giorno della ricezione resta da scrivere solo la consegna.

## 2. Requisiti funzionali

1. **RF-1** — Un messaggio normalizzato consegnato alla porta di ingresso interna crea una richiesta `aperta` sul
   canale di posta il cui recapito coincide con il destinatario del messaggio, con il richiedente riconosciuto
   dall'indirizzo del mittente (storia `0012`).
2. **RF-2** — Se il messaggio porta `In-Reply-To` o `References` che corrispondono a un `Message-Id` già registrato
   **dello stesso account**, non nasce una richiesta nuova: il messaggio si aggiunge come messaggio in ingresso al
   filo esistente, e la richiesta segue la macchina a stati della storia `0009` (una richiesta chiusa si riapre).
3. **RF-3** — Il `Message-Id` di ogni messaggio in ingresso è registrato; una seconda consegna dello stesso
   `Message-Id` nello stesso account viene scartata e contata, senza creare doppioni.
4. **RF-4** — Le risposte automatiche — riconosciute dalle intestazioni di servizio `Auto-Submitted`, `X-Autoreply`
   e `Precedence` con valore `bulk`, `list` o `auto_reply`, e dal mittente vuoto — **non aprono richieste, non
   aggiungono messaggi al filo e non riaprono richieste chiuse**: vengono scartate e contate con il motivo.
5. **RF-5** — Un messaggio indirizzato a un recapito che non corrisponde ad alcun canale `attivo` dell'account
   viene scartato e contato; non crea nulla e non genera alcuna risposta verso il mittente.
6. **RF-6** — Il collegamento con il vettore di posta della piattaforma è dietro un interruttore di configurazione,
   **spento per impostazione predefinita**; a interruttore spento la porta di ingresso interna resta pienamente
   collaudabile e un comando dello strumentario di sviluppo inietta un messaggio di prova per vedere l'effetto in
   locale.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La porta di ingresso **non riceve** un `tenant_id` da chi consegna: lo
  ricava dal canale che possiede il recapito di destinazione, e da nient'altro. La ricerca del filo per
  `Message-Id` filtra per `tenant_id` così ricavato: un identificativo di messaggio noto in un altro account non
  deve **mai** far agganciare la conversazione a quell'account. La porta è interna al servizio e non è raggiungibile
  dall'esterno del perimetro applicativo.
- **RT-2 — Interfaccia di programmazione (§2).** La consegna è un'operazione **interna** del servizio, non una
  rotta pubblica: nessun percorso sotto `/api/helpdesk/public/v1/*` la espone. Restano pubbliche e autenticate le
  sole rotte di diagnostica `GET /api/helpdesk/v1/channels/{id}/inbound-events` (esiti di ricezione, senza il
  contenuto dei messaggi); errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__inbound_email.sql` sullo schema `app_helpdesk`: colonne
  `message_id`, `in_reply_to` e `from_address` su `ticket_message`, tabella `inbound_event` (esito, motivo dello
  scarto, data) con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
  Indice **unico** su `(tenant_id, message_id)` per le righe non cancellate logicamente — è la difesa dai doppioni
  — e indice su `(tenant_id, in_reply_to)` per la ricucitura del filo.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Impostazioni → Canali → Posta» del modulo `helpdesk` con lo stato
  del canale e gli ultimi esiti di ricezione (quanti accettati, quanti scartati e perché), **senza mostrare il
  contenuto** dei messaggi scartati; dati letti con il client generato; solo token del sistema di design; funziona
  in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I motivi di scarto e le etichette di stato passano dallo spazio-nomi `helpdesk` e
  sono presenti in `en, it, fr, es, de`. La lingua del **richiedente** resta quella del suo profilo e non
  interferisce con la lingua dell'interfaccia.
- **RT-6 — Varchi e quota (§6, §7).** La ricezione **non consuma** la metrica `agents`: gli operatori sono un
  tetto sulle persone, non sui messaggi. Con abbonamento non attivo il canale di posta non accetta ricezioni e le
  scarta contandole, così che il cliente veda cos'ha perso quando riattiva. La diagnostica dalla schermata segue i
  varchi ordinari (`402`, `403`).
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: la ricezione è un fatto del sistema, non
  un comando. Le richieste arrivate si leggono con `elenca_richieste` e `leggi_richiesta` già dichiarati. Il
  contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Voci nuove nel manifesto `docs/compliance/manifests/helpdesk.yaml` in italiano e
  inglese: `message.from_address` → `ticket_message.from_address` (interessato: cliente finale; categoria:
  contatto; finalità: ricostruire il filo della conversazione; base giuridica: trattamento per conto del titolare;
  conservazione: come i messaggi) e `message.message_id` (dato tecnico). Campi annotati `@PersonalData`;
  `ticket_message` e `inbound_event` presenti in `exportData` e `purgeData` di `HelpdeskDataContract`. **Non si
  conserva il messaggio grezzo completo**: si tengono il corpo in testo, il corpo in ipertesto ripulito e le sole
  intestazioni che servono al filo — è minimizzazione, ed è anche ciò che riduce l'esposizione quando il corpo
  contiene per accidente un dato particolare. Al corpo si applica il contrassegno per la revisione umana
  introdotto dalla storia `0002`. Nuovo **sotto-responsabile**: il vettore di posta della piattaforma, che qui
  trasporta messaggi **di terzi** e non nostri — va dichiarato come tale nel contratto di nomina.
- **RT-9 — Registrazione eventi (§14).** Eventi «messaggio accettato», «messaggio agganciato al filo», «scartato
  come doppione», «scartato come risposta automatica», «scartato: canale sconosciuto», con `tenant_id`, `app_id`,
  identificativo del canale e identificativo di correlazione. `user_id` è assente per costruzione. **Mai** il
  corpo, **mai** l'oggetto, **mai** l'indirizzo del mittente nei registri: solo identificativi.

## 4. Criteri di accettazione

**CA-1 — Un messaggio nuovo diventa una richiesta**
- **Dato** un canale di posta `attivo` su `assistenza@cliente.test` e nessun filo aperto per il mittente
- **Quando** la porta di ingresso riceve un messaggio da `luca.verdi@example.test` a quel recapito
- **Allora** nasce una richiesta `aperta` sul canale, con richiedente riconosciuto e primo messaggio in ingresso
  nel filo

**CA-2 — La replica torna nel filo giusto**
- **Dato** una richiesta con un messaggio in uscita il cui `Message-Id` è registrato
- **Quando** arriva un messaggio con quel valore in `In-Reply-To`
- **Allora** si aggiunge al filo di quella richiesta, non ne nasce una nuova, e lo stato evolve secondo la storia
  `0009`

**CA-3 — Doppia consegna**
- **Dato** un messaggio già ricevuto e trasformato in richiesta
- **Quando** lo stesso `Message-Id` viene consegnato una seconda volta
- **Allora** viene scartato come doppione, contato, e non esiste una seconda richiesta né un secondo messaggio

**CA-4 — Risposta automatica di assenza**
- **Dato** una richiesta con un messaggio in uscita appena spedito
- **Quando** arriva una risposta con `Auto-Submitted: auto-replied`
- **Allora** viene scartata e contata: nessuna richiesta nuova, nessun messaggio nel filo, nessuna riapertura

**CA-5 — Recapito senza canale**
- **Dato** un messaggio indirizzato a un recapito che nessun canale attivo possiede
- **Quando** viene consegnato alla porta di ingresso
- **Allora** è scartato e contato con il motivo, nulla viene creato e nulla viene rispedito al mittente

**CA-6 — Isolamento fra account**
- **Dato** l'account `B` che ha registrato il `Message-Id` `<abc@example.test>`, e un canale di posta dell'account
  `A`
- **Quando** arriva al canale di `A` un messaggio con `In-Reply-To: <abc@example.test>`
- **Allora** nasce una richiesta **nuova in `A`**, il filo di `B` resta intatto, e nessun dato di `B` è leggibile
  da `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sul riconoscimento delle risposte automatiche e sulla ricucitura del filo, e di
      **integrazione** sulla porta di ingresso con carichi sintetici (messaggi inventati, indirizzi `*.test`),
      database effimero e migrazioni Flyway vere;
- [ ] prova di **isolamento fra account** sulla ricerca del filo per `Message-Id` e sull'associazione recapito →
      canale;
- [ ] **prova end-to-end**: *rimando* — la ricezione reale non esiste ancora nella piattaforma e non è pilotabile
      dal percorso; il passo entrerà in `[J-HELPDESK]` quando l'instradamento della posta in ingresso esisterà.
      Voce `da-coprire` nel **registro di copertura**
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con motivo e storia proprietaria
      (`0037`);
- [ ] **traduzioni** presenti in tutte e cinque le lingue per gli esiti e i motivi di scarto;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `message.from_address` e `message.message_id`,
      campi annotati `@PersonalData`, tabelle in esportazione e cancellazione, e il vettore di posta dichiarato
      come sotto-responsabile che trasporta dati di terzi;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotate la scelta della porta
      interna dietro interruttore e la scelta di non conservare il messaggio grezzo;
- [ ] contratto degli **strumenti conversazionali**: nessuno strumento nuovo, motivo annotato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali, compreso il comando che
      inietta un messaggio di prova;
- [ ] documentazione aggiornata: il punto aperto di [UC 0075](../../../15-supporto-e-piattaforma/0075-ticketing-nativo-in-house.md)
      va annotato con il fatto che DeskGrove è il secondo consumatore in attesa della ricezione.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0012` — canali e anagrafica del richiedente | Il recapito di destinazione individua il canale, e il mittente individua il richiedente |
| Storia `0007` — filo dei messaggi e risposta | Il messaggio in ingresso è una riga del filo: la struttura deve esistere |
| Storia `0009` — ciclo di vita degli stati | La replica su una richiesta chiusa **riapre**, e la regola è là |
| **Infrastruttura di ricezione della posta della piattaforma — non esiste** ([UC 0075](../../../15-supporto-e-piattaforma/0075-ticketing-nativo-in-house.md), punto aperto «Instradamento email in ingresso»; verifica del dominio UC 0018 / UC 0078) | Manca il trasporto: nessuna casella riceve davvero. Nel frattempo si costruisce la porta di ingresso interna, la si collauda con carichi sintetici e si tiene il collegamento al vettore dietro un interruttore spento |
| Storia `0015` — posta elettronica in uscita | Non è un prerequisito per ricevere, ma senza di essa il `Message-Id` dei nostri messaggi non esiste e CA-2 si può provare solo con un messaggio in uscita simulato |

## 7. Fuori ambito

- **L'invio di messaggi**: è la storia `0015`, che possiede anche la fermata sull'identità del mittente;
- **Gli allegati dei messaggi in ingresso**: la storia `0016` li introduce; qui i messaggi con allegati vengono
  accettati e il corpo viene lavorato, mentre i file sono ignorati e il fatto è annotato sul messaggio, così che
  nessun operatore creda di aver ricevuto tutto;
- **La difesa dai messaggi indesiderati** oltre alle risposte automatiche (punteggio di reputazione, elenchi di
  blocco): rimandata perché senza traffico vero si tarerebbe alla cieca — vedi punti aperti;
- **La verifica del dominio del cliente e la gestione dei recapiti**: appartiene alla piattaforma (UC 0018 /
  UC 0078) e alla decisione della storia `0015`;
- **L'inoltro automatico dalla vecchia casella del cliente**: è una configurazione che fa il cliente sul proprio
  fornitore di posta, e va documentata, non programmata.

## 8. Punti aperti

- **Quando esisterà la ricezione della posta.** È la dipendenza che decide la data di questa storia. Chiude: chi
  possiede [UC 0075](../../../15-supporto-e-piattaforma/0075-ticketing-nativo-in-house.md) insieme a UC 0018 /
  UC 0078, non questa storia.
- **Il vettore di posta come sotto-responsabile su dati di terzi.** Trasportare messaggi dei clienti dei nostri
  clienti non è la stessa cosa che mandare le nostre notifiche: cambia il contratto di nomina e l'elenco dei
  sotto-responsabili da pubblicare. Chiude: sviluppatore con la revisione legale pre-go-live
  ([docs/_REVISIONE-LEGALE.md](../../../../_REVISIONE-LEGALE.md)).
- **Difesa dai messaggi indesiderati su una casella pubblica.** Un recapito di assistenza pubblicato sul sito
  raccoglie messaggi automatici in quantità. La proposta è di **non** costruire un motore di regole né elenchi di
  blocco gestiti a mano (§2.5: è esattamente la complessità che il segmento rifiuta), ma di appoggiarsi al filtro
  del vettore quando esisterà. Chiude: sviluppatore, quando ci sarà traffico vero da misurare.
- **Conservazione degli esiti di ricezione.** Gli eventi di scarto servono a diagnosticare, non a costruire un
  archivio parallelo: proposta di conservarli poche settimane. Chiude: storia `0036`, che governa la conservazione.
- **Messaggi verso un canale sospeso o con abbonamento non attivo.** Questa storia li scarta contandoli. Metterli
  da parte per consegnarli alla riattivazione sarebbe più gentile, ma significherebbe conservare dati di terzi per
  un account che non ha un servizio attivo. Chiude: sviluppatore.
