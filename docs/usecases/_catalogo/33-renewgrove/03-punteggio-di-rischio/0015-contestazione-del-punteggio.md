# 0015 — Contestazione del punteggio

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 03 — Punteggio di rischio spiegabile e contestabile
**Storia**: `0015` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile commerciale che conosce la storia dei clienti
> voglio poter dire «questo fatto non c'entra» e vedere il punteggio rifarsi davanti a me
> così da non dover scegliere fra credere a un numero che so sbagliato e smettere di usare l'app.

**Contesto.** La spiegazione (`0014`) ha reso il punteggio leggibile; ora deve diventare **discutibile**. La
sentenza della Corte di giustizia dell'Unione europea del 7 dicembre 2023, causa C-634/21, non si ferma alle
informazioni sulla logica: chiede che sia garantito l'**intervento umano** e la possibilità di **esprimere la
propria opinione e di contestare** (§2.3 della [descrizione](../application-description.md)). Questa storia è
l'adempimento della seconda metà.

C'è un vincolo di forma che viene dal modello dati e non è negoziabile: `Segnale` è **in sola aggiunta** (§4.4).
Un fatto non si cancella e non si modifica — sarebbe riscrivere il passato, ed è esattamente ciò che rende uno
storico incapace di reggere una contestazione. Si **marca**: la contestazione è una riga nuova, con chi l'ha
sollevata, quando e perché, e il punteggio si ricalcola da lì in avanti.

Nel segmento a cui l'app si rivolge questa non è una funzione di conformità appiccicata sopra: è la valvola che
impedisce al cliente di spegnere tutto la prima volta che il punteggio sbaglia clamorosamente su un cliente che
lui conosce da dieci anni.

## 2. Requisiti funzionali

1. **RF-1** — Dalla spiegazione, chi legge può marcare **un singolo segnale** come **non pertinente**, indicando
   un **motivo** obbligatorio. Il segnale non viene cancellato né modificato: viene scritta una `Contestazione`
   che lo esclude dai calcoli **da quel momento in avanti**.
2. **RF-2** — Subito dopo la marcatura, il punteggio **si ricalcola davanti a chi ha agito**, e la schermata
   mostra il valore prima e dopo, con la fascia prima e dopo. Il ricalcolo produce una riga nuova nella serie
   storica (`0013`) che dichiara come **origine** la contestazione: la serie continua a non riscriversi
   all'indietro.
3. **RF-3** — Chi legge può **escludere un intero rapporto** dalla sorveglianza, con motivo. Il rapporto passa in
   stato `escluso`: smette di ricevere punteggi, non compare più negli elenchi di rischio, **libera una unità**
   della quota `rapporti_sorvegliati`, e il suo storico resta leggibile. L'esclusione è reversibile e la
   riattivazione è a sua volta tracciata.
4. **RF-4** — Ogni contestazione conserva **chi, quando, perché**, e resta visibile: sulla spiegazione, un segnale
   marcato compare barrato con il motivo e il nome di chi l'ha marcato, non sparisce dalla vista. Un fatto che
   scompare senza lasciare traccia è indistinguibile da un fatto mai arrivato.
5. **RF-5** — Una contestazione si può **revocare** (il segnale torna a contribuire), e anche la revoca è una riga
   nuova con chi e quando: nessuna riga di contestazione viene aggiornata o cancellata.
6. **RF-6** — Il campo **motivo** è testo libero **scritto da un nostro utente** — non importato da alcuna fonte —
   e la casella porta l'avvertenza esplicita, visibile prima di scrivere: **non inserire dati sulla salute** né
   altre informazioni delle categorie particolari dell'articolo 9 del regolamento europeo (convinzioni, opinioni,
   orientamento, appartenenza sindacale, dati biometrici o genetici).
7. **RF-7** — La marcatura e l'esclusione richiedono ruolo `owner`, `admin` o `member`: chi lavora sui rapporti
   deve poter contestare. Non richiedono autorizzazione di un superiore, perché una contestazione **non produce
   alcun effetto verso l'esterno**: cambia un numero, non tocca nessuno.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `contestazione` filtra per `tenant_id`
  preso dal token di accesso verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri
  viene ignorato. Contestare un segnale di un altro account restituisce `404`, non `403`: l'esistenza di quel
  segnale non è un'informazione da concedere.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `POST /api/fidelizzazione/v1/segnali/{id}/contestazione` (corpo: motivo),
  `DELETE /api/fidelizzazione/v1/segnali/{id}/contestazione` (revoca),
  `POST /api/fidelizzazione/v1/rapporti/{id}/esclusione` e la sua revoca; il risultato della marcatura riporta
  **punteggio e fascia prima e dopo**. Corpo validato (motivo obbligatorio, lunghezza massima dichiarata); errori
  in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V12__contestazione.sql` sullo schema `app_fidelizzazione`: tabella
  `contestazione` con `tenant_id`, riferimento al segnale **oppure** al rapporto, tipo (`segnale non pertinente` /
  `rapporto escluso`), motivo, autore, momento, stato (`attiva` / `revocata`), chiave primaria UUID versione 7 e
  colonne di controllo. **Nessun aggiornamento del `segnale`**: lo stato «non pertinente» che il §4.1 della
  descrizione elenca fra gli attributi del segnale è **derivato** dall'esistenza di una contestazione attiva, non
  una colonna scritta — vedi punti aperti. Lo stato `escluso` è invece un campo sul `rapporto`, che non è in sola
  aggiunta. Nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Nel riquadro della spiegazione: pulsante «questo fatto non c'entra» su ogni
  contributo, casella del motivo con l'avvertenza dell'articolo 9 **sopra** il campo e non sotto, e il confronto
  «prima / dopo» mostrato senza ricaricare la pagina; nella scheda del rapporto, l'azione «togli dalla
  sorveglianza» con la conferma che dice che cosa comporta (niente più punteggi, quota liberata, storico
  conservato). Dati letti e scritti con il client generato; solo token del sistema di design; funziona in tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe — pulsanti, avvertenza sull'articolo 9, testi di conferma,
  etichette «prima» e «dopo» — passano dallo spazio-nomi `fidelizzazione` e sono presenti in `en, it, fr, es, de`.
  L'avvertenza sui dati sulla salute non è mai una stringa scritta a mano nel componente.
- **RT-6 — Varchi e quota (§6, §7).** Catena dei varchi completa: `401`, `403` ad app spenta, `402` ad account non
  abilitato o abbonamento `canceled`, `403` a ruolo insufficiente (qui nessun ruolo è escluso: `member` compreso).
  Sulla quota l'effetto è **in restituzione**: escludere un rapporto **libera** una unità della metrica
  `rapporti_sorvegliati` (natura `stock`); riattivarlo la riprenota e, se il tetto è pieno, la riattivazione
  risponde `429` con il rimedio indicato («togli N rapporti dalla sorveglianza, oppure passa di piano»), senza
  cambiare nulla.
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati:
  `marca_segnale_non_pertinente(segnale, motivo) → bozza con il punteggio ricalcolato` e
  `escludi_rapporto(rapporto, motivo) → bozza`, entrambi marcati **scrittura** e **con conferma umana**: cambiano
  un giudizio conservato e la seconda muove la quota. Nessuno dei due tocca qualcuno all'esterno, quindi non sono
  irreversibili nel senso del §7 della descrizione, ma la bozza resta perché il motivo va scritto e riletto. Il
  contratto vive dentro il servizio; il server conversazionale è di piattaforma e **non è ancora implementato**
  (UC 0061-0063); la storia `0029` li assembla.
- **RT-8 — Dati personali (§10).** **Sì.** Voce nuova nel manifesto
  `docs/compliance/manifests/fidelizzazione.yaml`, in **italiano e inglese**:
  `contestazione.autore_e_motivo` — dove vive: tabella `contestazione`; di chi è: **utente del nostro cliente**
  (non il cliente finale); che dato è: prova; a cosa serve: dimostrare che il punteggio è stato messo in
  discussione e da chi; base giuridica: esecuzione del contratto con il nostro cliente; conservazione: come lo
  storico del rapporto. Il campo `motivo` e il campo `autore` sono annotati `@PersonalData`; la tabella
  `contestazione` entra in `exportData` e in `purgeData` di `FidelizzazioneDataContract`. **Attenzione al doppio
  interessato**: una contestazione riferita a un segnale riguarda anche il cliente finale a cui quel segnale si
  riferisce, e per questo la tabella compare anche nell'esportazione richiesta per il rapporto. Il presidio contro
  le categorie particolari dell'articolo 9 è **contrattuale e non tecnico** (avvertenza a schermo): non esiste un
  rilevamento automatico del contenuto e non se ne inventa uno, sarebbe un presidio finto (§6).
- **RT-9 — Registrazione eventi (§14).** `segnale marcato non pertinente`, `contestazione revocata`,
  `rapporto escluso dalla sorveglianza`, `rapporto riattivato`, `riattivazione respinta per quota`, con
  `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e la variazione di punteggio; **mai il testo
  del motivo** e mai l'etichetta del rapporto.

## 4. Criteri di accettazione

**CA-1 — Il punteggio si rifà davanti a chi contesta**
- **Dato** un rapporto in fascia `a rischio` con punteggio 78, il cui contributo maggiore è una segnalazione di
  assistenza che l'utente sa essere stata aperta per errore
- **Quando** l'utente marca quel segnale come non pertinente scrivendo il motivo e conferma
- **Allora** vede immediatamente «prima 78 — a rischio / dopo 41 — attenzione», e nella serie storica compare una
  riga nuova con origine «contestazione»; la riga precedente resta intatta

**CA-2 — Il segnale non sparisce, si marca**
- **Dato** un segnale marcato come non pertinente
- **Quando** l'utente riapre la spiegazione
- **Allora** il segnale è ancora elencato, barrato, con il motivo e il nome di chi l'ha marcato e la data; e il
  fatto originale è ancora presente nella tabella dei segnali, non aggiornato e non cancellato

**CA-3 — Motivo obbligatorio e avvertenza visibile**
- **Dato** l'utente che apre la casella della contestazione
- **Quando** tenta di confermare senza scrivere il motivo
- **Allora** riceve `400` in `problem+json` con l'indicazione del campo mancante, nulla viene scritto, e
  l'avvertenza «non inserire dati sulla salute» è visibile sopra la casella in tutte e cinque le lingue

**CA-4 — Esclusione di un rapporto e quota liberata**
- **Dato** un account al tetto di `rapporti_sorvegliati` del proprio piano
- **Quando** un utente esclude un rapporto dalla sorveglianza indicando il motivo
- **Allora** il rapporto smette di ricevere punteggi e di comparire negli elenchi di rischio, il suo storico resta
  leggibile, e il conteggio della quota scende di una unità

**CA-5 — Riattivazione a quota piena**
- **Dato** un account al tetto della quota e un rapporto in stato `escluso`
- **Quando** l'utente tenta di riattivarlo
- **Allora** riceve `429` con il messaggio che spiega come rimediare, il rapporto resta `escluso` e nessuna riga
  viene scritta

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri segnali
- **Quando** un utente di `A` tenta di contestare un segnale di `B` usandone l'identificativo
- **Allora** riceve `404`, nessuna contestazione viene scritta e nessun punteggio di `B` cambia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sul ricalcolo con contributi esclusi e sulla derivazione dello stato del segnale; prove
      di **integrazione** sulla risorsa, con database effimero e migrazioni vere, che verificano che la riga del
      `segnale` **non** sia stata modificata e che la serie storica abbia una riga in più, non una riscritta;
- [ ] prova di **isolamento fra account** sulla risorsa `contestazione`;
- [ ] prova sulla **quota**: esclusione che libera, riattivazione respinta con `429` a tetto pieno;
- [ ] **prova end-to-end**: *rimandare* — il percorso `[J-FIDELIZZAZIONE]` nasce nella storia `0030` e dovrà
      coprire il tratto «contesto un fatto → il punteggio scende → il fatto resta visibile barrato»; voce
      `da-coprire` con motivo e storia proprietaria `0030` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), avvertenza dell'articolo 9
      compresa;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `contestazione.autore_e_motivo`, campi annotati
      `@PersonalData`, tabella `contestazione` presente in `exportData` e in `purgeData`;
- [ ] **registro delle decisioni** compilato con: perché si marca invece di cancellare, perché la contestazione non
      richiede autorizzazione, perché l'avvertenza sull'articolo 9 è contrattuale e non tecnica, come è derivato lo
      stato del segnale;
- [ ] contratto degli **strumenti conversazionali** `marca_segnale_non_pertinente` e `escludi_rapporto` dichiarato
      come **scrittura con conferma**;
- [ ] documentazione aggiornata: la descrizione §4.1 chiarisce che lo stato del segnale è derivato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0014` — spiegazione del punteggio | si contesta ciò che si vede: senza il riquadro dei contributi non c'è il punto da cui marcare un fatto |
| storia `0013` — calcolo e storico | il ricalcolo produce una riga nuova nella serie, con origine «contestazione» |
| storia `0009` — il rapporto sorvegliato | lo stato `escluso` e la restituzione della quota vivono sul rapporto |
| epica di piattaforma non implementata, UC 0061-0063 | i due strumenti sono dichiarati e non esposti: nel frattempo si contesta dall'interfaccia |

## 7. Fuori ambito

- **cambiare i pesi** perché una contestazione ha rivelato che un tipo di segnale pesa troppo: storia `0016`. Qui
  si corregge un fatto, non la regola;
- **i presidi verificabili sulla decisione automatizzata** e la prova che nessun effetto parte da un punteggio:
  storia `0017`;
- **la cancellazione fisica di un segnale** su richiesta dell'interessato: è del contratto dati (`0032`), non di
  questa storia, ed è un'operazione diversa dal marcare;
- **un flusso di contestazione aperto al cliente finale** (che non è nostro utente e non ha accesso all'app):
  deliberatamente fuori. Il diritto dell'interessato si esercita verso il **nostro cliente**, che è il titolare
  del trattamento; la nostra parte è dargli gli strumenti per rispondere.

## 8. Punti aperti

- **Lo stato del segnale: derivato o materializzato?** Il §4.1 della descrizione elenca `stato (valido / marcato
  non pertinente)` fra gli attributi di `Segnale`, mentre il §4.4 dichiara `Segnale` in sola aggiunta. Le due
  righe non possono valere insieme. **Raccomandazione**: stato **derivato** dall'esistenza di una contestazione
  attiva, come scritto in RT-3, perché è l'unica lettura che rispetta la sola aggiunta; se le prestazioni lo
  richiedessero, una colonna materializzata resta ammissibile solo come proiezione ricostruibile. Chiude:
  **sviluppatore**, aggiornando la descrizione.
- **Se una contestazione debba propagarsi ai punteggi già calcolati.** Oggi no: vale da qui in avanti, perché la
  serie non si riscrive (`0013`). L'obiezione è seria e va scritta: chi guarda l'andamento di un rapporto vede una
  discontinuità il giorno della contestazione, ed è giusto che la veda, ma va spiegata a schermo.
  **Raccomandazione**: mostrare sul grafico dell'andamento un segno nel punto in cui una contestazione ha cambiato
  la base del calcolo. Chiude: **sviluppatore**.
