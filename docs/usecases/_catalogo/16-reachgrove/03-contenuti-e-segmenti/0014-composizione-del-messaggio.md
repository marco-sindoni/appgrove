# 0014 — Composizione del messaggio

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 03 — Contenuti e segmenti
**Storia**: `0014` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che scrive la comunicazione del mese
> voglio comporre un messaggio ordinato senza sapere niente di composizione tipografica per la posta elettronica
> così da mandare qualcosa che si legge bene ovunque e che porta sempre con sé chi lo manda e come smettere di
> riceverlo.

**Contesto.** Il cliente tipo non ha nessuno il cui mestiere sia fare marketing
([application-description.md](../application-description.md) §1) e la posta elettronica è il formato più ostile che
esista: quello che si vede in un programma di posta non è quello che si vede in un altro. Servono quindi blocchi
predefiniti invece di un foglio bianco. Ma la ragione per cui questa storia sta **prima** della spedizione è
un'altra: il messaggio deve nascere già conforme. Il piè di pagina con il mittente identificabile e il collegamento
di disiscrizione non è una funzione che si attiva, è parte del messaggio — perché un messaggio senza quelle due
cose non è spedibile né per legge né per i requisiti tecnici dei grandi fornitori di posta
([application-description.md](../application-description.md) §2.3, punti 1 e 5). Un editore che permette di
comporre un messaggio non spedibile è un editore che prepara un rifiuto al controllo pre-volo (storia 0018).

## 2. Requisiti funzionali

1. **RF-1** — Il messaggio si compone con blocchi di tipo definito: titolo, testo, immagine, bottone con
   collegamento, separatore, piè di pagina. I blocchi si aggiungono, si riordinano e si eliminano; non esiste
   modifica del codice sorgente della pagina.
2. **RF-2** — Ogni messaggio ha un oggetto e un mittente visibile (nome e indirizzo), scelti fra i mittenti dei
   domini verificati dell'account (storia 0017). Un mittente non verificato non è selezionabile.
3. **RF-3** — Da ogni messaggio il sistema produce **sempre** anche la versione in solo testo, generata dai blocchi
   e modificabile a mano. Un messaggio senza versione in solo testo non si salva.
4. **RF-4** — Il **piè di pagina obbligatorio** contiene l'identità di chi invia (denominazione e recapito, presi
   dalle impostazioni dell'account) e il collegamento di disiscrizione. Non si rimuove, non si nasconde, non si
   svuota: l'editore lo mostra come blocco fisso e ne consente solo la traduzione e piccole personalizzazioni di
   testo attorno al collegamento. Il meccanismo della disiscrizione — collegamento univoco per destinatario e
   intestazione tecnica di disiscrizione in un clic (norma RFC 8058) — è della storia 0012: qui si garantisce che il
   posto per il collegamento ci sia sempre.
5. **RF-5** — Le immagini si caricano nell'archivio dell'account e vengono servite dalla nostra infrastruttura.
   **Nessuna risorsa esterna** entra nel messaggio: né immagini ospitate altrove, né caratteri scaricati, né
   frammenti di codice di terzi. Un collegamento a una risorsa esterna incollato in un blocco immagine viene
   rifiutato con la spiegazione del motivo.
6. **RF-6** — Il messaggio si salva come bozza in qualunque momento e mostra un controllo di completezza non
   bloccante: oggetto mancante, versione in solo testo vuota, immagine senza testo alternativo, collegamento
   scritto male.
7. **RF-7** — Un'anteprima mostra il messaggio come lo vedrà il destinatario, in due formati: schermo largo e
   schermo di telefono, più la versione in solo testo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Messaggi, blocchi e immagini caricate filtrano per `tenant_id` dal token
  verificato. Le immagini servite hanno un indirizzo non indovinabile e restano legate all'account che le ha
  caricate.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/campaigns/v1/messages`,
  `GET|PUT /api/campaigns/v1/messages/{id}`, `POST /api/campaigns/v1/messages/{id}/preview`,
  `POST /api/campaigns/v1/assets` per le immagini. Corpo validato: l'elenco dei tipi di blocco è chiuso. Errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__message_body.sql` sullo schema `app_campaigns`: il corpo a blocchi
  come documento strutturato sulla riga del messaggio, più la tabella `message_asset` con `tenant_id`, chiave
  primaria UUID versione 7, colonne di controllo e cancellazione logica. La versione in solo testo è una colonna,
  non un calcolo fatto ogni volta: dev'essere modificabile a mano.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Messaggi» del modulo `campaigns`: elenco dei blocchi, riquadro delle
  proprietà, anteprima affiancata. Solo token del sistema di design; tema chiaro e scuro. Attenzione a non
  confondere i due piani: i token vestono **l'editore**, non il messaggio, che deve valere anche fuori dal nostro
  tema.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe dell'editore passano dallo spazio-nomi `campaigns` e sono
  presenti in `en, it, fr, es, de`. Il **contenuto del messaggio** è del cliente e non si traduce mai.
- **RT-6 — Varchi e quota (§6, §7).** Comporre non consuma la metrica `messages_sent` (natura `flow`): si consuma
  quando si spedisce (storia 0019). Valgono i varchi comuni; con abbonamento `canceled` la risorsa risponde `402`.
  Il caricamento di immagini è soggetto al limite di spazio di piattaforma, non a una metrica dell'app.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo qui: la scrittura del testo dalla chat è la
  storia 0036 e produce comunque una bozza che una persona salva. Il contratto vive dentro il servizio; il server
  conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Il corpo del messaggio è **testo libero scritto dal cliente**: è uno dei tre
  ingressi non presidiati citati al §6 della [descrizione dell'applicazione](../application-description.md). Non
  introduce campi che riguardano una persona identificata, ma la tabella dei messaggi entra comunque in
  `exportData` e `purgeData` perché può contenere qualunque cosa il cliente vi scriva. Voce nel manifesto
  `docs/compliance/manifests/campaigns.yaml` in italiano e inglese, con la finalità dichiarata «contenuto della
  comunicazione commerciale».
- **RT-9 — Registrazione eventi (§14).** «Messaggio creato», «messaggio modificato», «risorsa esterna rifiutata»
  con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione. **Mai** il testo del messaggio, mai
  l'oggetto: sono contenuti.

## 4. Criteri di accettazione

**CA-1 — Il piè di pagina non si toglie**
- **Dato** un messaggio in composizione con il piè di pagina obbligatorio
- **Quando** l'utente prova a eliminarlo, a svuotarne il contenuto o a rimuovere il collegamento di disiscrizione
- **Allora** l'operazione è impedita con una spiegazione che dice **perché** (identificazione del mittente e diritto
  di opporsi), e il blocco resta al suo posto

**CA-2 — La versione in solo testo esiste sempre**
- **Dato** un messaggio composto di soli blocchi grafici
- **Quando** l'utente lo salva
- **Allora** la versione in solo testo è stata generata dai blocchi ed è visibile e modificabile; se l'utente la
  svuota a mano, il salvataggio è rifiutato con `400` e il motivo

**CA-3 — Nessuna risorsa esterna**
- **Dato** un blocco immagine
- **Quando** l'utente incolla l'indirizzo di un'immagine ospitata su un altro sito
- **Allora** il messaggio non lo accetta e spiega che le immagini vanno caricate nell'archivio dell'account, perché
  una risorsa esterna farebbe sapere a un terzo chi ha aperto il messaggio

**CA-4 — Mittente non verificato non selezionabile**
- **Dato** un account con un dominio mittente non ancora verificato
- **Quando** l'utente apre l'elenco dei mittenti
- **Allora** quel mittente non compare come selezionabile e un collegamento porta alla verifica del dominio

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri messaggi e le proprie immagini
- **Quando** un utente di `A` chiede un messaggio o un'immagine di `B`
- **Allora** riceve `404`, anche forzando l'identificativo dell'altro account nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla generazione della versione in solo testo e sul rifiuto delle risorse esterne, e di
      **integrazione** sulla risorsa dei messaggi, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su messaggi e immagini;
- [ ] **prova end-to-end**: rimando — la composizione entra nel percorso `[J-CAMPAIGNS]` come passo «scrivi il
      messaggio», ma il percorso nasce con la storia 0037: voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con motivo e storia proprietaria;
- [ ] **traduzioni** dell'editore presenti in tutte e cinque le lingue; contenuti del cliente esclusi;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per il corpo del messaggio, tabelle in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, con annotato perché il piè di pagina è imposto e perché le risorse
      esterne sono vietate;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, con il rimando alla storia 0036;
- [ ] controllo automatico di **accessibilità** verde sull'editore, compreso il testo alternativo delle immagini;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0002` | La tabella dei messaggi e quella delle risorse nascono con il modello dati |
| Storia `0003` | Serve il guscio del modulo per avere una sezione dove comporre |
| Storia `0012` | Il collegamento di disiscrizione univoco e l'intestazione tecnica sono suoi: qui si garantisce solo che il posto ci sia |
| Storia `0017` (successiva) | L'elenco dei mittenti selezionabili viene dai domini verificati: finché non esiste, in locale si lavora con un dominio di prova dichiarato tale |

## 7. Fuori ambito

- i campi variabili e l'anteprima su un destinatario reale: è la storia 0015;
- il salvataggio come modello riusabile e la duplicazione: è la storia 0016;
- la generazione del testo dalla chat: è la storia 0036;
- la prova su due varianti dell'oggetto: è la storia 0031;
- il controllo pre-volo che decide se il messaggio può partire: è la storia 0018. Qui si impedisce di comporre un
  messaggio non conforme, lì si impedisce che parta.

## 8. Punti aperti

- **Quanto lasciare personalizzare il piè di pagina.** Fra «testo fisso» e «testo libero purché contenga il
  collegamento» c'è una zona grigia: il cliente ha bisogno di scriverci la propria denominazione e talvolta un
  riferimento all'informativa. Proposta: struttura fissa con campi compilabili, mai testo libero completo. Chiude
  lo sviluppatore.
- **Formato di scambio del corpo a blocchi.** Se un domani si volesse importare un modello da un altro prodotto,
  servirebbe un formato dichiarato. Non serve adesso e non lo si inventa qui.
