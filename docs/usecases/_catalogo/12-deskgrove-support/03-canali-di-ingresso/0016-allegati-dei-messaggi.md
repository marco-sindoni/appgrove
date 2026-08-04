# 0016 — Allegati dei messaggi

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 03 — Canali di ingresso
**Storia**: `0016` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0014`, `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come operatore che riceve un reclamo
> voglio vedere la fotografia del prodotto rotto che il cliente ha mandato, e poter rimandare indietro il modulo da
> firmare
> così da chiudere la richiesta in uno scambio invece che in cinque, senza uscire dall'app per cercare il file
> nella posta.

**Contesto.** Un'assistenza senza allegati costringe l'operatore a tornare nella casella di posta, ed è lì che la
conversazione si spezza di nuovo. Ma è anche la funzione con il **rapporto rischio/utilità peggiore** di tutta
l'app ([application-description.md](../application-description.md) §6): un file arriva da una persona esterna, può
contenere qualunque cosa — un certificato medico allegato a un reclamo è un dato dell'articolo 9 che entra da una
porta che non l'aspettava — e occupa spazio che nessuna metrica di quota limita, perché la metrica dell'app è una
sola ed è `agents`.

Da qui le tre scelte che tengono insieme la storia: **elenco chiuso di tipi ammessi**, **conservazione più breve
dei messaggi** e **collegamento di scarico a scadenza**. E una fermata dichiarata: ⚠️ **in questa storia non c'è
alcun controllo antivirus** — §8.

## 2. Requisiti funzionali

1. **RF-1** — Un operatore allega uno o più file a un messaggio in uscita; i file arrivati con un messaggio in
   ingresso sono estratti e collegati al messaggio del filo, e nel dettaglio della richiesta si vedono nome, tipo,
   dimensione e data.
2. **RF-2** — Sono ammessi solo i tipi di un **elenco chiuso** (immagini comuni, documenti in formato portatile,
   testo semplice, formati d'ufficio più diffusi), verificati **dal contenuto** del file e non dalla sola
   estensione, entro un limite di dimensione per file e per messaggio; ciò che eccede o non è ammesso viene
   rifiutato con un messaggio che dice quale limite è stato superato.
3. **RF-3** — I file vivono nello strato di archiviazione di `services/commons`, **in regione europea**, sotto una
   chiave che contiene il `tenant_id`; nessun file è leggibile senza autorizzazione e nessuno è pubblico.
4. **RF-4** — Lo scarico avviene con un **collegamento firmato a scadenza breve**, generato su richiesta di un
   utente autenticato dell'account proprietario; il collegamento scade anche se l'utente perde il diritto prima
   della scadenza.
5. **RF-5** — Gli allegati hanno una conservazione **più breve** di quella dei messaggi: alla scadenza una
   lavorazione periodica cancella il file dall'archivio e la riga resta con la sola indicazione «allegato rimosso
   per scadenza», così che il filo resti comprensibile.
6. **RF-6** — La cancellazione di un richiedente o dell'intero account cancella anche i **file nell'archivio**, non
   soltanto le righe della tabella, e l'esportazione dei dati li include.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `attachment` filtra per `tenant_id` preso dal
  token verificato; la chiave dell'oggetto nell'archivio contiene il `tenant_id`, e il servizio **ricalcola** la
  chiave dall'account del token invece di fidarsi di quella memorizzata insieme all'identificativo ricevuto. Un
  identificativo di allegato di un altro account riceve la stessa risposta di uno inesistente: nessuna differenza
  osservabile fra «non tuo» e «non c'è».
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/helpdesk/v1/tickets/{id}/attachments` (caricamento
  con verifica del tipo e della dimensione), `GET /api/helpdesk/v1/attachments/{id}/download-link` (che restituisce
  un collegamento firmato a scadenza, non il file) e `DELETE /api/helpdesk/v1/attachments/{id}`. Nessuna rotta di
  allegati sotto `/public/`. Errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__attachments.sql` sullo schema `app_helpdesk`: tabella `attachment`
  con `tenant_id`, `ticket_message_id` (riferimento **logico**, nessuna chiave esterna verso altri schemi), nome
  originale, tipo rilevato, dimensione, chiave nell'archivio, data di scadenza della conservazione e indicatore
  «rimosso per scadenza»; chiave primaria UUID versione 7, colonne di controllo e cancellazione logica. Indice su
  `(tenant_id, retention_expires_at)` per la lavorazione periodica.
- **RT-4 — Modulo frontend (§3, §5).** Nel filo della richiesta l'elenco degli allegati con nome, dimensione e
  comando di scarico, e l'area di caricamento nella casella di risposta; avviso visibile che i file provengono da
  terzi e vanno aperti con prudenza. Dati letti con il client generato; solo token del sistema di design; funziona
  in tema chiaro e scuro; controllo automatico di accessibilità sull'area di caricamento.
- **RT-5 — Cinque lingue (§4).** Nomi dei limiti, motivi di rifiuto, avviso sui file di terzi e l'indicazione
  «allegato rimosso per scadenza» passano dallo spazio-nomi `helpdesk` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Gli allegati **non consumano** la metrica `agents`, che è a giacenza sui
  posti operatore: l'app ha una sola metrica e non se ne aggiunge una seconda. Con abbonamento non attivo il
  caricamento risponde `402` e lo scarico resta possibile per i soli **diritti dell'interessato**, che non si
  bloccano per motivi commerciali. Il vero presidio sul volume è la **conservazione**, che è materia della storia
  `0036` (vedi §8).
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento restituisce il **contenuto** di un allegato:
  `leggi_richiesta` ne elenca al più i metadati (nome, tipo, dimensione). Nessuno strumento carica o scarica file:
  sarebbe un modo per far uscire contenuto di terzi da una chat. Il contratto vive dentro il servizio; il server
  conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Voce nuova nel manifesto `docs/compliance/manifests/helpdesk.yaml` in italiano e
  inglese: `attachment.file` → archivio degli allegati più tabella `attachment`; interessato «cliente finale
  dell'azienda cliente e operatore»; categoria «**contenuto arbitrario**, può contenere qualsiasi cosa»; finalità
  «corredare la richiesta»; base giuridica «trattamento per conto del titolare»; conservazione «più breve dei
  messaggi, proposta 12 mesi, da confermare». Campi annotati `@PersonalData`; `attachment` aggiunta a `exportData`
  **e** a `purgeData` di `HelpdeskDataContract`, **compresi i file nell'archivio**, che non sono righe di tabella e
  sono il modo più facile per dimenticarsene. I dati personali stanno a riposo **solo in regioni europee**. Nessun
  fornitore esterno nuovo: l'archivio è quello già in uso dalla piattaforma. Il contrassegno per la revisione umana
  della storia `0002` lavora sul **testo** e non sul contenuto dei file: un certificato allegato **non** viene
  segnalato, ed è un limite da dire, non da nascondere.
- **RT-9 — Registrazione eventi (§14).** Eventi «allegato caricato», «allegato rifiutato per tipo», «allegato
  rifiutato per dimensione», «collegamento di scarico generato», «allegato rimosso per scadenza», con `tenant_id`,
  `app_id`, `user_id` e identificativo di correlazione. **Mai** il nome del file nei registri — un nome di file
  contiene spessissimo un nome di persona — e mai il contenuto: solo identificativi, tipo e dimensione.

## 4. Criteri di accettazione

**CA-1 — Un file arriva e si scarica**
- **Dato** un messaggio in ingresso con una fotografia allegata
- **Quando** l'operatore apre la richiesta e chiede lo scarico
- **Allora** ottiene un collegamento firmato che restituisce il file, e nel filo si vedono nome, tipo e dimensione

**CA-2 — Tipo non ammesso, riconosciuto dal contenuto**
- **Dato** un file eseguibile rinominato con estensione di documento
- **Quando** si tenta di caricarlo
- **Allora** viene rifiutato perché il **contenuto** non è fra i tipi ammessi, nulla finisce nell'archivio e
  l'operatore legge quale tipo era atteso

**CA-3 — Oltre il limite di dimensione**
- **Dato** un file più grande del limite per singolo file
- **Quando** si tenta di caricarlo
- **Allora** viene rifiutato con l'indicazione del limite superato e nulla viene creato

**CA-4 — Collegamento scaduto**
- **Dato** un collegamento di scarico generato e lasciato scadere
- **Quando** qualcuno lo usa
- **Allora** non ottiene il file e riceve una risposta di collegamento non più valido, senza rivelare l'esistenza
  del file

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri allegati
- **Quando** un utente di `B` chiede il collegamento di scarico per l'identificativo di un allegato di `A`
- **Allora** riceve la stessa risposta di un allegato inesistente, nessun collegamento viene generato e nessun
  accesso all'archivio avviene

**CA-6 — Scadenza della conservazione**
- **Dato** un allegato che ha superato il termine di conservazione
- **Quando** la lavorazione periodica passa
- **Allora** il file non è più nell'archivio, la riga riporta «allegato rimosso per scadenza», il resto del filo è
  intatto e un tentativo di scarico non produce alcun file

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sul riconoscimento del tipo dal contenuto e sul calcolo della scadenza, e di
      **integrazione** su caricamento, scarico e lavorazione periodica con archivio simulato, database effimero e
      migrazioni Flyway vere;
- [ ] prova di **isolamento fra account** su caricamento, generazione del collegamento e scarico, con tentativo di
      usare identificativi e chiavi d'archivio di un altro account;
- [ ] **prova end-to-end**: *rimando* — il percorso principale `[J-HELPDESK]` non passa dagli allegati, che restano
      coperti da prove di integrazione e di frontend; voce `da-coprire` nel **registro di copertura**
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con motivo e storia proprietaria
      (`0037`);
- [ ] **traduzioni** presenti in tutte e cinque le lingue per limiti, rifiuti e avviso sui file di terzi;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `attachment.file`, campi annotati `@PersonalData`,
      tabella **e file dell'archivio** presenti in esportazione e cancellazione, verificato da una prova che
      controlla l'assenza del file dopo la cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotata in modo esplicito la
      **fermata sull'assenza di controllo antivirus** (§8) e le mitigazioni scelte al suo posto;
- [ ] contratto degli **strumenti conversazionali**: nessuno strumento restituisce o carica file, motivo annotato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali, con l'archivio simulato in
      locale;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove (limiti dichiarati nella
      pagina del prodotto e nell'informativa del cliente).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` — filo dei messaggi e risposta | Un allegato appartiene a un messaggio del filo: la struttura deve esistere |
| Storia `0014` — posta elettronica in ingresso | È la porta da cui arrivano i file dei clienti; senza di essa si caricano solo file dell'operatore |
| Storia `0015` — posta elettronica in uscita | Perché un allegato dell'operatore raggiunga davvero il richiedente |
| Strato di archiviazione di `services/commons` | Esiste come `ExportStorage` / `S3ExportStorage`, **orientato alle esportazioni per i diritti dell'interessato**: va generalizzato o affiancato da un archivio di allegati con lo stesso vincolo di regione europea. È una modifica a codice condiviso, da concordare (§8) |
| Storia `0036` — esportazione, cancellazione e conservazione | Il termine di conservazione degli allegati è un parametro dell'account e vive là; qui si costruisce il meccanismo che lo applica |

## 7. Fuori ambito

- **Il caricamento di file dal modulo web pubblico** (storia `0013`): accettare file da chiunque, senza neppure un
  indirizzo verificato, è un rischio diverso e più grande — resta fuori finché la fermata sull'antivirus non è
  sciolta;
- **L'anteprima del contenuto dentro l'app**: i file si scaricano, non si visualizzano in linea — mostrare in linea
  un documento arbitrario è il modo classico per farsi eseguire qualcosa nel browser dell'operatore;
- **Gli allegati nel portale del richiedente senza account**: è la storia `0032`, che ha già i suoi problemi di
  accesso senza token;
- **Il limite di spazio per account e la sua fatturazione**: la metrica di quota è una sola ed è `agents`; il
  volume si governa con la conservazione, storia `0036`;
- **La ricerca dentro il contenuto dei file**: fuori perimetro, e richiederebbe di leggerne il contenuto per
  finalità nuove.

## 8. Punti aperti

> 🛑 **Fermata: in questa storia non c'è alcun controllo antivirus, e va detto forte.** Accettare file da chiunque
> significa accettare file infetti: è il punto 9 dei rischi della descrizione dell'applicazione. L'operatore che
> scarica un allegato lo apre **sul proprio computer**, e noi glielo abbiamo consegnato. Un controllo antivirus è
> un fornitore esterno in più — che riceverebbe il contenuto di terzi, cioè proprio il dato più delicato dell'app —
> oppure un componente da mantenere in casa, con le sue firme da aggiornare e il suo costo di esercizio. Non è una
> scelta che spetta a questa storia. Chiude: **sviluppatore**.
>
> **Cosa fa questa storia nell'attesa**, e sono difese vere ma parziali: elenco chiuso di tipi ammessi verificato
> dal contenuto e non dall'estensione; limite di dimensione; nessuna esecuzione e nessuna visualizzazione in linea
> di contenuto arbitrario; scarico solo tramite collegamento firmato a scadenza breve; avviso esplicito
> nell'interfaccia che i file provengono da terzi. **Nessuna di queste impedisce a un documento infetto ma di tipo
> ammesso di raggiungere l'operatore.** Se la decisione fosse «si va avanti così», il limite va scritto nella
> pagina del prodotto e nel contratto con il cliente, non lasciato implicito.

**Altri punti aperti**

- **Generalizzare lo strato di archiviazione di `services/commons`.** Oggi è pensato per le esportazioni dei
  diritti dell'interessato; gli allegati sono un uso diverso (molti oggetti piccoli, letture frequenti, scadenze
  per riga). È una modifica a codice condiviso, fuori dal perimetro dell'app. Chiude: chi implementa la storia,
  d'accordo con chi possiede `services/commons` — stessa situazione del punto 7 dei rischi
  ([application-description.md](../application-description.md) §11).
- **Durata di conservazione degli allegati** — proposta 12 mesi contro i 24 dei messaggi, da confermare nel
  manifesto insieme al resto della classificazione. Chiude: sviluppatore, con la storia `0036`.
- **Limiti di dimensione e tipi ammessi.** Troppo stretti e il cliente rimanda la fotografia via posta, vanificando
  l'app; troppo larghi e l'archivio cresce senza contropartita, visto che nessuna metrica lo limita. Chiude:
  sviluppatore.
- **Cosa fare dei file arrivati con un messaggio scartato** (doppione, risposta automatica, canale sconosciuto):
  questa storia non li conserva. Se un giorno servisse una diagnostica, conservare contenuto di terzi arrivato «per
  sbaglio» sarebbe una scelta da motivare. Chiude: sviluppatore.
