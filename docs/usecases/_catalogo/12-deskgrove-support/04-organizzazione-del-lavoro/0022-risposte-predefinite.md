# 0022 — Risposte predefinite

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 04 — Organizzazione del lavoro
**Storia**: `0022` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0012`, `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde ai clienti
> voglio richiamare con due tasti un testo che ho già scritto, con il nome del cliente e il numero della richiesta
> già dentro
> così da rispondere alla ventesima domanda uguale della settimana in venti secondi invece che in tre minuti.

**Contesto.** È la seconda richiesta del segmento in ordine di importanza, subito dopo «sapere chi risponde a cosa»:
*rispondere in fretta senza riscrivere ogni volta* ([application-description.md](../application-description.md)
§2.5). È anche una funzione che i concorrenti collocano nei piani superiori — Zoho Desk non la mette nel piano
d'ingresso (§2.6, fonte 2) — mentre qui sta in tutti i piani, perché è una delle ragioni per cui il cliente lascia
la casella condivisa. Va fatta ora, alla fine dell'epica dell'organizzazione del lavoro: la casella di risposta
esiste dalla storia `0007`, gli operatori dalla `0018`, e nell'epica successiva la stessa casella riceverà anche
gli articoli della base di conoscenza (storia `0030`) — meglio che il meccanismo di inserimento nasca una volta
sola.

## 2. Requisiti funzionali

1. **RF-1** — Un operatore può creare, modificare e disattivare una risposta predefinita con titolo, corpo e
   lingua. Le risposte sono **dell'account**, non dell'operatore che le ha scritte: tutti gli operatori le vedono e
   le usano.
2. **RF-2** — Il corpo ammette un **elenco chiuso** di segnaposto — nome del richiedente, numero della richiesta,
   nome visibile dell'operatore — e nient'altro. Un segnaposto sconosciuto fa rifiutare il salvataggio con `422` e
   un messaggio che elenca quelli ammessi.
3. **RF-3** — Mentre scrive la risposta, l'operatore apre l'elenco con una scorciatoia, cerca per **titolo e corpo**
   e inserisce il testo nel punto in cui si trova il cursore, con i segnaposto **già sostituiti** dai valori della
   richiesta aperta.
4. **RF-4** — Se un segnaposto non ha valore (un richiedente senza nome, per esempio), l'inserimento usa un ripiego
   dichiarato e leggibile — mai una parentesi graffa lasciata a vista nel messaggio che parte al cliente.
5. **RF-5** — L'elenco delle risposte è filtrabile per lingua e mostra il titolo con l'inizio del corpo. Il numero
   di risposte attive per account è limitato (proposta: **cento**); al superamento la creazione è rifiutata con
   `422` e una spiegazione.
6. **RF-6** — Accanto alla casella del corpo compare l'avviso che una risposta predefinita è **testo riusato per
   molti clienti** e non deve contenere il nome, i recapiti o la vicenda di una singola persona: per quello ci sono
   i segnaposto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura dell'entità `CannedResponse` filtra per
  `tenant_id` preso dal token verificato; la ricerca a testo libero non attraversa mai il confine dell'account, e
  un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/helpdesk/v1/canned-responses`,
  `PATCH /api/helpdesk/v1/canned-responses/{id}` e
  `GET /api/helpdesk/v1/canned-responses?q=…&locale=…&ticketId=…` per la ricerca con i segnaposto risolti sulla
  richiesta indicata; corpo validato, compreso il controllo dei segnaposto ammessi; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__canned_responses.sql` sullo schema `app_helpdesk`: tabella
  `canned_response` (titolo, corpo, lingua, stato) con `tenant_id`, chiave primaria UUID versione 7, colonne di
  controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica (`deleted_at`); indice
  di ricerca su titolo e corpo a partire da `tenant_id`; unicità del titolo per account e lingua fra le righe non
  cancellate. **Nessuna chiave esterna verso altri schemi**.
- **RT-4 — Modulo frontend (§3, §5).** Sezione Impostazioni → Risposte predefinite per la gestione, e pannello di
  ricerca richiamabile dalla casella di risposta del dettaglio (storia `0007`), con inserimento nel punto del
  cursore. Dati letti con il client generato; solo token del sistema di design; funziona in tema chiaro e scuro; il
  pannello si apre, si percorre e si conferma **da tastiera**, perché una funzione che serve a risparmiare secondi
  non può richiedere il passaggio al mouse.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe dell'interfaccia — etichette, avviso di RF-6, messaggi di
  errore, nomi dei segnaposto mostrati all'operatore — passano dallo spazio-nomi `helpdesk` e sono presenti in
  `en, it, fr, es, de`. Il **contenuto** delle risposte predefinite lo scrive il cliente: la `lingua` di RF-1 è una
  proprietà del testo del cliente, non una traduzione dell'interfaccia, e le due cose non vanno confuse.
- **RT-6 — Varchi e quota (§6, §7).** Le risposte predefinite **non** consumano la metrica `agents`: il tetto di
  RF-5 è un limite di prodotto e produce `422`, non `429`. La catena dei varchi resta quella di piattaforma
  (`401 → 403 → 402 → 403 → 429`): con abbonamento `canceled` o `paused` le rotte rispondono `402`, con `past_due`
  restano accessibili.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. Questa storia arricchisce
  `prepara_risposta(numero, indicazioni | articolo) → bozza di messaggio, non inviata` del §7 della descrizione
  dell'applicazione, che può partire da una risposta predefinita: resta **scrittura con conferma umana**, e la
  bozza **resta dentro l'app**. La separazione fra `prepara_risposta` e `invia_risposta` non si tocca: preparare è
  reversibile, inviare è un atto verso una persona esterna e non si annulla. Il contratto vive dentro il servizio;
  il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Il **corpo** della risposta predefinita (`canned_response.body`) è testo libero
  scritto dal cliente e può contenere dati personali scritti a mano nonostante l'avviso di RF-6: è una delle quattro
  sorgenti di testo libero elencate al §6 della descrizione dell'applicazione. Voci nuove nel manifesto
  `docs/compliance/manifests/helpdesk.yaml` in **italiano e inglese** per `canned_response.body` (e per il titolo,
  che è testo libero anch'esso), campi annotati `@PersonalData`, tabella `canned_response` aggiunta **sia** a
  `exportData` **sia** a `purgeData` del contratto `HelpdeskDataContract` — come già previsto dal §6. La
  cancellazione è **fisica**: sostituire un nome con un codice non è cancellare. Su DeskGrove appgrove è
  **responsabile del trattamento** per conto dell'azienda cliente, non titolare.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «risposta predefinita creata», «modificata», «disattivata»,
  «inserita in un messaggio» e «creazione respinta per tetto raggiunto» sono registrati con `tenant_id`, `app_id`,
  `user_id`, identificativo della risposta predefinita e identificativo di correlazione, **senza** il titolo, il
  corpo né il testo risolto.

## 4. Criteri di accettazione

**CA-1 — Inserimento con segnaposto risolti**
- **Dato** una risposta predefinita che comincia con «Buongiorno {nome richiedente}, riguardo alla richiesta
  {numero}…» e una richiesta aperta del signor Bianchi con numero 1042
- **Quando** l'operatore la richiama dalla casella di risposta e la inserisce
- **Allora** nella casella compare «Buongiorno Bianchi, riguardo alla richiesta 1042…» nel punto del cursore, e
  nessuna parentesi graffa resta nel testo

**CA-2 — Segnaposto sconosciuto**
- **Dato** un operatore che scrive nel corpo un segnaposto inventato
- **Quando** salva la risposta predefinita
- **Allora** riceve `422` con l'elenco dei segnaposto ammessi, e nulla viene salvato

**CA-3 — Segnaposto senza valore**
- **Dato** una richiesta arrivata da un indirizzo di posta di cui non si conosce il nome del richiedente
- **Quando** l'operatore inserisce una risposta che usa il nome del richiedente
- **Allora** al posto del nome compare il ripiego dichiarato, il testo resta leggibile e **nessuna** parentesi
  graffa finisce nel messaggio

**CA-4 — Tetto raggiunto**
- **Dato** un account con cento risposte predefinite attive
- **Quando** un operatore ne crea una nuova
- **Allora** riceve `422` con un messaggio che spiega il limite e propone di disattivarne una, e nulla viene creato

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie risposte predefinite
- **Quando** un operatore di `A` cerca una parola contenuta solo in una risposta di `B`, e poi prova a inserirne una
  forzandone l'identificativo nel corpo
- **Allora** la ricerca non restituisce nulla e l'inserimento forzato è rifiutato

**CA-6 — Esportazione e cancellazione**
- **Dato** un account con risposte predefinite che contengono testo scritto a mano
- **Quando** si esercita l'esportazione dei dati e poi la cancellazione dell'account
- **Allora** i corpi compaiono nell'esportazione e dopo la cancellazione le righe di `canned_response` non esistono
  più fisicamente

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sul riconoscimento e sulla sostituzione dei segnaposto, ripiego compreso, e di
      **integrazione** sulla risorsa `canned-responses` e sulla ricerca, con database effimero e migrazioni Flyway
      vere;
- [ ] prova di **isolamento fra account** su elenco, ricerca e inserimento;
- [ ] **prova end-to-end**: **nessun impatto** sul percorso `[J-HELPDESK]` — la risposta predefinita è una
      scorciatoia sulla stessa casella già percorsa dal percorso minimo; la copertura resta alle prove
      d'integrazione e di modulo, e la risposta va scritta nel **registro di copertura**
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), con la distinzione fra lingua
      dell'interfaccia e lingua del contenuto scritta a chiare lettere;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `canned_response.title` e
      `canned_response.body`, campi annotati `@PersonalData`, tabella `canned_response` presente in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotato perché i segnaposto sono
      un elenco chiuso e perché le risposte sono dell'account e non dell'operatore;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, con la nota su come `prepara_risposta` usa
      questa storia;
- [ ] controllo automatico di **accessibilità** verde, con verifica del percorso completo da tastiera;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove (§6 della descrizione
      dell'applicazione, elenco delle sorgenti di testo libero).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` — filo dei messaggi e risposta | Serve la casella di risposta in cui inserire il testo |
| Storia `0018` — operatori e posti | Il segnaposto «nome dell'operatore» prende il valore dal nome visibile dell'operatore |
| Storia `0012` — canali e anagrafica del richiedente | Il segnaposto «nome del richiedente» prende il valore dal richiedente; se manca, vale il ripiego di RF-4 |
| Storia `0030` — ricerca e inserimento nella risposta (epica 06) | **Dipendenza inversa**: sarà `0030` a riusare il pannello di inserimento nato qui, per gli articoli della base di conoscenza |

## 7. Fuori ambito

- **Gli articoli della base di conoscenza inseriti nella risposta**: li fa la storia `0030`. Sono una cosa diversa —
  l'articolo è pubblicabile e vive di vita propria, la risposta predefinita è un frammento privato — e qui si
  costruisce soltanto il meccanismo di inserimento che quella storia riuserà.
- **Le risposte predefinite personali del singolo operatore**: rimandate di proposito. Con da uno a dieci operatori
  la condivisione vale più della personalizzazione, e due elenchi separati raddoppiano il posto in cui cercare.
- **La sostituzione automatica mentre si digita** (scorciatoie testuali del tipo `;saluto`): fuori ambito, è una
  raffinatezza di interfaccia che si aggiunge dopo aver visto se il pannello di ricerca basta.
- **Le statistiche d'uso** («questa risposta è stata inserita 42 volte», l'ordinamento per uso più frequente):
  rimandate al cruscotto della storia `0028`, che possiede i numeri del servizio.
- **La traduzione automatica di una risposta predefinita nelle altre lingue**: esclusa. Farebbe partire al cliente
  finale un testo che nessuno dell'azienda ha riletto.

## 8. Punti aperti

- **Tetto di cento risposte per account** — proposta di prodotto, non vincolo tecnico. Lo chiude lo sviluppatore, e
  va deciso prima: Help Scout usa proprio il numero di risposte predefinite come limite del piano gratuito (§2.6,
  fonte 3), quindi la scelta ha anche un risvolto di listino ed è una **fermata di escalation**.
- **Chi può creare e modificare le risposte predefinite?** La proposta è «ogni operatore», perché sono uno strumento
  di lavoro condiviso. Riservarle a `owner` e `admin` è difendibile — sono testi che partono verso i clienti
  finali a nome dell'azienda — ma è una scelta di direzione di prodotto e la chiude lo sviluppatore.
- **Il ripiego per il nome del richiedente.** «Buongiorno,» senza nome è la proposta, perché una formula generica è
  sempre corretta mentre «Buongiorno cliente» suona male in cinque lingue diverse. Va confermato dallo sviluppatore
  insieme al testo definitivo, perché è un testo che il cliente finale legge.
