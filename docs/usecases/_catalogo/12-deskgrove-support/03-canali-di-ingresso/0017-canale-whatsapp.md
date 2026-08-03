# 0017 — Canale WhatsApp

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 03 — Canali di ingresso
**Storia**: `0017` · **Taglia stimata**: una giornata *(la sola parte applicativa; la scelta del fornitore e la sua
messa in opera non sono lavoro di sviluppo)* · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0012`, `0009`
**Ultimo aggiornamento**: 2026-08-03

> 🛑 **Questa storia non entra in implementazione finché lo sviluppatore non ha sciolto la fermata del §8.**
> È l'unica storia dell'applicazione che, per esistere, introduce **due sotto-responsabili del trattamento nuovi
> fuori dall'Unione Europea** su contenuto di terzi. Nessuna delle storie `0012`-`0016` dipende da essa: se la
> risposta fosse «non si fa», DeskGrove resta completo e l'epica 03 si chiude lo stesso.

## 1. Narrazione

> Come titolare di un negozio i cui clienti scrivono su WhatsApp e non per posta
> voglio che quei messaggi entrino nella stessa coda delle altre richieste, con lo stesso stato e la stessa
> scadenza
> così da smettere di rispondere dal telefono personale di una persona sola, che quando è in ferie porta via con sé
> tutte le conversazioni.

**Contesto.** Nel mercato italiano e in quello latino-americano è **il** canale richiesto: chi vende al pubblico
riceve più messaggi lì che via posta. È anche la funzione che la descrizione dell'applicazione propone di riservare
al piano superiore, proprio perché non è gratuita da mantenere ([application-description.md](../application-description.md)
§5). Ma è la storia con più conseguenze fuori dal codice, e le conseguenze vanno dette prima delle funzioni.

**Dato di fatto rilevato su fonte ufficiale.** La documentazione dei prezzi della piattaforma WhatsApp Business di
Meta (https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing) stabilisce che i messaggi
di **servizio** avviati dal cliente sono **gratuiti** dentro la **finestra di 24 ore** dall'ultimo messaggio
dell'utente; **fuori** da quella finestra si possono mandare solo **modelli approvati**, e sono **a pagamento**. Ne
discende la forma del prodotto: l'assistenza vive quasi tutta dentro la finestra gratuita, e il caso «rispondo dopo
due giorni» è un **caso diverso**, da progettare a parte e non da nascondere dietro la stessa casella di testo.
Chi non se ne accorge costruisce un'app che, il lunedì mattina, non riesce a rispondere ai messaggi del venerdì
sera — e non capisce perché.

**Sovrapposizione dichiarata con ChatGrove (app 5 del catalogo).** Anche ChatGrove parla su WhatsApp. Due app che
aprono ciascuna il proprio collegamento a Meta significano due integrazioni da mantenere, due contratti con
sotto-responsabili e due volte lo stesso lavoro; la strada giusta, il giorno in cui esistessero entrambe, è una
**capacità di piattaforma condivisa** per la messaggistica ([application-description.md](../application-description.md)
§10). Oggi ChatGrove è fra le applicazioni escluse dal drill-down, quindi DeskGrove arriva primo: **da qui il
vincolo di progetto RF-5**, l'integrazione non si cabla dentro il dominio dell'assistenza.

## 2. Requisiti funzionali

1. **RF-1** — Il canale di tipo `whatsapp` si configura come gli altri (storia `0012`) e resta in stato
   `da configurare` finché la connessione con il fornitore non è verificata; le credenziali non stanno in chiaro
   nel database e non sono mai restituite dalle interfacce di lettura.
2. **RF-2** — Un messaggio in arrivo sul numero aziendale crea una richiesta sul canale, oppure si aggancia al filo
   della richiesta aperta dello stesso richiedente, riconosciuto per **numero in formato internazionale** (storia
   `0012`).
3. **RF-3** — L'operatore risponde con testo libero **solo** dentro la finestra di 24 ore dall'ultimo messaggio del
   richiedente; l'interfaccia mostra in modo evidente quanto tempo resta.
4. **RF-4** — Fuori dalla finestra la casella di risposta libera è disabilitata e l'unica via è la scelta di un
   **modello approvato** da un elenco; l'app dichiara che quel messaggio è **a pagamento presso il fornitore** e
   non lo invia senza una conferma esplicita dell'operatore.
5. **RF-5** — Il collegamento con il fornitore vive dietro una **porta del servizio** (un'interfaccia con una sola
   realizzazione), affiancata da una realizzazione **simulata** usata in locale e in tutti i collaudi: il dominio
   dell'assistenza non conosce né Meta né il fornitore intermedio, e il giorno in cui l'integrazione diventasse una
   capacità di piattaforma si sostituisce la realizzazione senza toccare le richieste.
6. **RF-6** — Il canale è **spento per impostazione predefinita** e si accende solo per gli account il cui piano lo
   prevede e che hanno preso atto dell'elenco aggiornato dei sotto-responsabili; il fatto che l'informativa sia
   stata presentata al cliente è registrato con data.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il messaggio in arrivo non porta un `tenant_id`: l'account si ricava dal
  **numero aziendale destinatario**, cioè dal canale, e da nient'altro. Ogni lettura e scrittura filtra per
  `tenant_id` così ricavato. Due account che ricevono messaggi dallo stesso numero di cliente hanno due richiedenti
  e due fili distinti, senza alcuna interrogazione che li metta in relazione. La chiamata in ingresso del fornitore
  è autenticata con una firma verificata prima di qualunque scrittura: un messaggio non firmato viene scartato e
  contato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta di ricezione dedicata al fornitore, fuori dalla catena di
  autenticazione a token ma protetta dalla verifica della firma, che **non consente altro che consegnare un
  messaggio**; rotte autenticate `GET|POST|PATCH /api/helpdesk/v1/channels[/{id}]` già esistenti per la
  configurazione e `GET /api/helpdesk/v1/tickets/{id}/whatsapp-window` per il tempo residuo. Errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__whatsapp_channel.sql` sullo schema `app_helpdesk`: su `channel` il
  numero aziendale e il riferimento al segreto (non il segreto); su `ticket` la data dell'ultimo messaggio in
  ingresso del richiedente, che è ciò che apre e chiude la finestra; tabella `whatsapp_template` con i modelli
  approvati e il loro stato. Chiavi primarie UUID versione 7, `tenant_id` ovunque, colonne di controllo e
  cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Nel filo della richiesta il contatore della finestra e, allo scadere, la
  casella disabilitata con la spiegazione e l'elenco dei modelli; sezione «Impostazioni → Canali → WhatsApp» con lo
  stato della connessione e l'informativa sui sotto-responsabili da presentare al cliente. Solo token del sistema di
  design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Contatore, spiegazione della finestra, avviso «questo messaggio è a pagamento» e
  stati della connessione passano dallo spazio-nomi `helpdesk` e sono presenti in `en, it, fr, es, de`. I **modelli
  approvati** seguono invece le lingue approvate presso il fornitore, che sono un elenco diverso e non nostro.
- **RT-6 — Varchi e quota (§6, §7).** Il canale WhatsApp **non consuma** la metrica `agents`, che resta l'unica
  dell'app. La disponibilità del canale dipende dal piano: la storia **non fissa prezzi** e non decide su quale
  piano stia (la descrizione lo **propone** per `business`, §5, ed è una fermata). Con abbonamento non attivo il
  canale non riceve e non invia. Il costo dei messaggi fuori finestra **non** si trasforma in un addebito a
  consumo: la piattaforma vieta di far pagare lo sforamento, quindi o il messaggio si può mandare o si blocca.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. Ma la regola di sicurezza si applica in
  forma **rafforzata**: `invia_risposta` verso un canale WhatsApp è scrittura irreversibile verso una persona
  esterna **e** può avere un costo, quindi la conferma umana è obbligatoria e la bozza deve dire se il messaggio
  cade dentro o fuori la finestra. Il contratto vive dentro il servizio; il server conversazionale è di piattaforma
  e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Voci del manifesto `docs/compliance/manifests/helpdesk.yaml` da aggiornare in
  italiano e inglese: `requester.phone` (che qui diventa il recapito principale, non più un dato accessorio) e
  `message.body` per i messaggi scambiati sul canale. Campi annotati `@PersonalData`; tabelle già in `exportData` e
  `purgeData`. **Sotto-responsabili nuovi**: Meta e il fornitore intermedio, **con trasferimento di dati fuori
  dall'Unione Europea** — vanno dichiarati nel contratto di nomina, nell'elenco pubblico dei sotto-responsabili e
  nell'informativa che il cliente titolare presenta ai propri clienti. È il punto su cui la storia si ferma (§8).
  Il contrassegno per la revisione umana della storia `0002` si applica anche a questi messaggi.
- **RT-9 — Registrazione eventi (§14).** Eventi «messaggio ricevuto», «messaggio scartato per firma non valida»,
  «risposta inviata dentro la finestra», «modello inviato fuori finestra», «fornitore non raggiungibile», con
  `tenant_id`, `app_id`, `user_id` (quando c'è) e identificativo di correlazione. **Mai** il numero di telefono,
  **mai** il corpo del messaggio: il numero è un identificativo diretto di una persona.

## 4. Criteri di accettazione

**CA-1 — Il messaggio del cliente entra in coda**
- **Dato** un canale WhatsApp attivo sul numero aziendale e nessun filo aperto per quel richiedente
- **Quando** arriva un messaggio firmato dal fornitore
- **Allora** nasce una richiesta sul canale, con richiedente riconosciuto per numero in formato internazionale e il
  messaggio come prima riga del filo

**CA-2 — Risposta dentro la finestra**
- **Dato** una richiesta il cui ultimo messaggio del cliente è di due ore fa
- **Quando** l'operatore risponde con testo libero
- **Allora** il messaggio parte come messaggio di servizio, compare nel filo, e il contatore della finestra mostra
  il tempo residuo

**CA-3 — Fuori dalla finestra**
- **Dato** una richiesta il cui ultimo messaggio del cliente è di due giorni fa
- **Quando** l'operatore apre la casella di risposta
- **Allora** la risposta libera è disabilitata con la spiegazione, l'unica via è un modello approvato, e un
  tentativo di inviare testo libero viene rifiutato senza che nulla parta

**CA-4 — Fornitore non raggiungibile**
- **Dato** un canale attivo e il fornitore che non risponde
- **Quando** l'operatore invia una risposta
- **Allora** il canale passa in stato `in errore` con l'ultimo errore leggibile, il testo scritto **non va perso**,
  l'operatore è avvisato e nessun messaggio risulta consegnato

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con due numeri aziendali diversi, e uno stesso cliente finale che scrive a
  entrambi dallo stesso numero
- **Quando** arriva un messaggio al numero di `A`
- **Allora** finisce solo nel filo di `A`, `B` ha un richiedente e un filo separati, e nessun parametro della
  chiamata in ingresso può spostare il messaggio da un account all'altro

## 5. Definizione di fatto

- [ ] **la fermata del §8 è stata sciolta dallo sviluppatore, per iscritto, prima di scrivere codice**;
- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sul calcolo della finestra di 24 ore e sulla verifica della firma in ingresso, e di
      **integrazione** su ricezione e invio con la realizzazione **simulata** del fornitore, database effimero e
      migrazioni Flyway vere;
- [ ] prova di **isolamento fra account** sulla ricezione (numero destinatario → canale → account) e sull'invio;
- [ ] **prova end-to-end**: *rimando* — nessun percorso end-to-end tocca un fornitore esterno reale; quando la
      fermata sarà sciolta il passo entrerà in `[J-HELPDESK]` con il fornitore simulato. Voce `da-coprire` nel
      **registro di copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con
      motivo e storia proprietaria (`0037`);
- [ ] **traduzioni** presenti in tutte e cinque le lingue per contatore, spiegazione della finestra e avviso di
      costo;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con **Meta e il fornitore intermedio dichiarati
      sotto-responsabili** e il trasferimento fuori dall'Unione Europea documentato; registro dei trattamenti
      rigenerato;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotata la risposta data alla
      fermata e le sue motivazioni;
- [ ] contratto degli **strumenti conversazionali**: nessuno strumento nuovo, conferma umana rafforzata su
      `invia_risposta` verso questo canale;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali, con il fornitore **sempre
      simulato** in locale: nessun messaggio vero parte da un portatile;
- [ ] documentazione aggiornata: elenco pubblico dei sotto-responsabili, pagina del prodotto, informativa modello
      per il cliente titolare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| **Decisione dello sviluppatore sul §8** | Senza di essa la storia non si implementa: introduce fornitori nuovi con trasferimento fuori dall'Unione Europea |
| Storia `0012` — canali e anagrafica del richiedente | Il canale e il riconoscimento per numero in formato internazionale sono là |
| Storia `0007` — filo dei messaggi e risposta | Il messaggio WhatsApp è una riga del filo come le altre |
| Storia `0009` — ciclo di vita degli stati | Un messaggio in arrivo su una richiesta chiusa **riapre**, secondo la regola già scritta |
| Contratto di nomina a responsabile del trattamento con l'elenco dei sotto-responsabili | Oggi la piattaforma non ne ha uno per questa fattispecie (punto 4 dei rischi): senza, il canale non si può accendere per un cliente reale |
| Scelta e attivazione del fornitore intermedio presso Meta | Non è lavoro di sviluppo: è un contratto, una verifica dell'azienda e un numero da abilitare |

## 7. Fuori ambito

- **La vendita conversazionale su WhatsApp** — cataloghi, ordini, pagamenti in chat: è **ChatGrove**, app 5 del
  catalogo, e non entra qui nemmeno un pezzo;
- **I messaggi promozionali o di marketing** verso i clienti finali: DeskGrove manda messaggi di **servizio** in
  risposta a chi ha scritto; qualunque uso promozionale cambia base giuridica, costo e reputazione del numero, e
  non è di questa app;
- **Gli allegati su WhatsApp** (fotografie e documenti mandati in chat, che sono la norma su questo canale):
  restano fuori perché aggiungerebbero il rischio della storia `0016` — antivirus compreso — a un canale che ha già
  la sua fermata; vanno affrontati dopo, con la storia `0016` come base;
- **L'approvazione dei modelli presso il fornitore**: è una procedura esterna, con i suoi tempi e i suoi rifiuti;
  l'app li elenca e li usa, non li fa approvare;
- **Gli altri canali di messaggistica** (Telegram, Messenger, messaggi brevi): fuori perimetro, e ciascuno
  porterebbe il proprio fornitore;
- **Una capacità di messaggistica di piattaforma condivisa fra le app**: è la strada giusta il giorno in cui ci
  fosse un secondo consumatore; oggi non c'è, e RF-5 tiene la porta aperta senza costruirla.

## 8. Punti aperti

> 🛑 **Fermata principale: si fa o non si fa?** È il punto 3 dei rischi della descrizione dell'applicazione, e non
> lo scioglie un agente. Chiude: **sviluppatore**, con la revisione legale pre-go-live
> ([docs/_REVISIONE-LEGALE.md](../../../../_REVISIONE-LEGALE.md)).

**Cosa comporta dire di sì**, in chiaro, perché la decisione sia informata:

- **Due sotto-responsabili nuovi** — Meta e il fornitore intermedio — su contenuto di terzi, cioè il dato più
  delicato dell'app: qui appgrove è **responsabile** del trattamento per conto del cliente, non titolare, quindi
  ogni fornitore aggiunto va autorizzato dal titolare, elencato e comunicato.
- **Trasferimento di dati fuori dall'Unione Europea**, in **tensione diretta** con la postura di sovranità dei dati
  del progetto — che è anche un argomento di vendita usato altrove nel catalogo. Sostenere «i tuoi dati restano in
  Europa» e insieme instradare le conversazioni attraverso Meta è una contraddizione che il cliente attento nota.
- **Un adempimento in più a ogni cambio**: l'elenco dei sotto-responsabili va tenuto aggiornato e i clienti vanno
  avvisati quando cambia.
- **Una valutazione del trasferimento** e, probabilmente, un aggiornamento della valutazione d'impatto: sono gli
  elementi già segnalati al punto 2 dei rischi.

**Cosa comporta dire di no**: si perde il canale più richiesto nei due mercati che contano di più per questa app,
e la si vende come prodotto «per posta e sito». È una posizione difendibile e coerente, non una rinuncia mascherata:
va scritta sulla pagina del prodotto, non lasciata capire.

**Fermate secondarie, tutte da sciogliere solo se la principale è «sì»**

- **Quale fornitore intermedio.** Alcuni operano con trattamento in Unione Europea e ridurrebbero — non
  eliminerebbero — l'esposizione, perché Meta resta a valle. **Non ho verificato l'offerta di alcun fornitore
  specifico e non ne propongo nessuno**: servirebbe una ricerca con verifica sulle pagine ufficiali. Chiude:
  sviluppatore.
- **Costo dei messaggi fuori finestra e chi lo paga.** La fonte ufficiale dice che sono a pagamento, non quanto
  costano al netto del ricarico del fornitore, che dipende dal contratto e dal paese
  ([application-description.md](../application-description.md) §2.7). La piattaforma vieta l'addebito a consumo
  dello sforamento, quindi le vie sono: includerne un numero nel piano e poi bloccare, oppure non offrirli affatto
  e chiudere la conversazione fuori finestra su un altro canale. **È una decisione di prezzo: non la prende un
  agente.** Chiude: sviluppatore.
- **Su quale piano sta il canale.** La descrizione lo propone per `business`; è una scelta di listino. Chiude:
  sviluppatore.
- **Dove vive l'integrazione il giorno in cui esistesse ChatGrove** (punto 8 dei rischi). RF-5 tiene l'integrazione
  dietro una porta del servizio proprio per rendere il trasloco possibile, ma la decisione di farne una capacità di
  piattaforma non è di questa storia. Chiude: sviluppatore, quando ChatGrove uscisse dalle applicazioni escluse.
- **Come si comporta l'app quando la finestra scade mentre l'operatore sta scrivendo.** Proposta: la bozza si salva
  come nota interna e l'app propone il modello, invece di perdere il testo. Da confermare, perché è la situazione
  più frequente del lunedì mattina. Chiude: sviluppatore.
