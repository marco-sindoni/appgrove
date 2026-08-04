# 0019 — Intervento con conferma umana

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 04 — Interventi con conferma umana
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che risponde di persona di ogni parola che arriva ai suoi clienti
> voglio che ciò che l'app prepara resti fermo finché non lo guardo e non dico di sì
> così da usare uno strumento che mi fa risparmiare tempo senza mai mettermi in bocca parole che non ho detto.

**Contesto.** È il cuore dell'epica 04 e, insieme alla storia `0017`, la ragione per cui questo prodotto è
vendibile in Europa. La [descrizione](../application-description.md) §4.4 fissa la macchina a stati
dell'intervento e vi appende una regola che chiama **non negoziabile**: *da `bozza` non si esce senza una
persona*. Non esiste un passaggio automatico, non esiste una configurazione che lo abiliti, e la storia deve
portare **una prova che lo dimostri** — perché una regola che nessun collaudo sorveglia è una regola che una
modifica innocente cancella senza che nessuno se ne accorga.

La stessa descrizione (§11) elenca come «difetto più grave possibile in questa app» un messaggio a un cliente
finale mandato da un automatismo a insaputa del titolare, e come rischio parallelo il titolare che conferma a
occhi chiusi. Al primo si risponde con la macchina a stati e la prova; al secondo con il riquadro dei tre fatti
principali costruito dalla storia `0017`, che qui viene **incastonato nella conferma**: confermare deve almeno
significare aver avuto davanti i motivi.

## 2. Requisiti funzionali

1. **RF-1** — Esiste l'entità `Intervento` con la macchina a stati del §4.4: `bozza → confermato → consegnato →
   eseguito`, con `annullato` raggiungibile da `bozza` e da `confermato` (prima della consegna). Nessun altro
   passaggio è ammesso, e un passaggio non ammesso è respinto con un errore che nomina lo stato di partenza e
   quello richiesto.
2. **RF-2** — **Da `bozza` si esce solo per intervento di una persona**: la transizione a `confermato` è possibile
   unicamente attraverso una richiesta portata da un token di accesso di un utente. Nessuna lavorazione, nessun
   consumatore di eventi, nessuna funzione pianificata può produrla, e **non esiste alcuna configurazione** che lo
   consenta. Una **prova automatica lo dimostra** ed è parte della suite.
3. **RF-3** — **Chi ha preparato** e **chi ha confermato** sono due campi distinti, entrambi conservati con il
   momento in cui l'hanno fatto. Possono coincidere — nelle micro-imprese di norma coincidono — ma restano due
   campi, perché la domanda «chi ha confermato questo intervento» deve avere una risposta anche fra due anni
   (§3 della descrizione, modello utente `multi`).
4. **RF-4** — La schermata di conferma mostra i **tre fatti principali** che hanno formato il punteggio del
   rapporto (storia `0017`), il testo che uscirà e il canale previsto. Non si conferma un intervento senza avere
   davanti il motivo per cui esiste.
5. **RF-5** — Un intervento si prepara da un rapporto, scegliendo un piano fra quelli attivi per la sua fascia
   (`0018`) oppure senza piano; il testo del piano è un **punto di partenza modificabile**, non un testo bloccato.
6. **RF-6** — L'intervento porta una **nota libera** scritta dal nostro utente, con l'avvertenza esplicita e
   visibile prima di scrivere: **non inserire dati sulla salute** né altre informazioni delle categorie
   particolari dell'articolo 9 del regolamento europeo.
7. **RF-7** — Preparare un intervento è di `owner`, `admin` e `member`; **confermarlo** è di `owner` e `admin`, e
   di `member` solo quando il piano non prevede alcuna concessione economica — le concessioni hanno una regola
   propria e più stretta (`0022`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `intervento` filtra per `tenant_id` preso
  dal token di accesso verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene
  ignorato. Confermare un intervento di un altro account restituisce `404`.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/fidelizzazione/v1/interventi` (crea in stato
  `bozza`), `PUT /api/fidelizzazione/v1/interventi/{id}` (modifica la sola bozza),
  `POST /api/fidelizzazione/v1/interventi/{id}/conferma`,
  `POST /api/fidelizzazione/v1/interventi/{id}/annullamento`,
  `GET /api/fidelizzazione/v1/interventi` (paginata, con filtri per stato e rapporto). Corpo validato; errori in
  `application/problem+json`, con `409` sulle transizioni non ammesse. Definizione OpenAPI aggiornata nello stesso
  commit. **Nessuna rotta interna, nessun percorso di servizio** che confermi: la conferma ha un solo ingresso.
- **RT-3 — Persistenza (§8).** Migrazione `V15__intervento.sql` sullo schema `app_fidelizzazione`: tabella
  `intervento` con `tenant_id`, rapporto, piano d'origine, stato, canale previsto, contenuto proposto, nota,
  **preparato da / preparato il**, **confermato da / confermato il**, esito; chiave primaria UUID versione 7,
  colonne di controllo `created_at`, `updated_at`, `created_by`, `updated_by` e cancellazione logica `deleted_at`.
  Le transizioni di stato sono registrate in `transizione_intervento` (stato precedente, stato nuovo, chi,
  quando): senza, la prova di chi ha confermato è ricostruita e non conservata. Nessuna chiave esterna verso altri
  schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sezione `Interventi` del modulo `fidelizzazione`: elenco per stato, editor
  della bozza, e **schermata di conferma** che incastona il componente dei tre fatti principali (`0017`) sopra il
  testo che uscirà. Il pulsante di conferma dichiara che cosa succederà dopo («l'intervento verrà consegnato
  all'app che possiede la relazione, oppure finirà nella lista di lavoro»). Dati letti e scritti con il client
  generato; solo token del sistema di design; funziona in tema chiaro e scuro; controllo automatico di
  accessibilità.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe — nomi degli stati, messaggi di errore delle transizioni,
  avvertenza dell'articolo 9 sulla nota, testo del pulsante di conferma — passano dallo spazio-nomi
  `fidelizzazione` e sono presenti in `en, it, fr, es, de`. Il **contenuto** dell'intervento resta nella lingua in
  cui il cliente lo ha scritto.
- **RT-6 — Varchi e quota (§6, §7).** Catena dei varchi completa: `401`, `403` ad app spenta, `402` ad account non
  abilitato o abbonamento `canceled`, `403` a ruolo insufficiente secondo RF-7. **Nessun consumo di quota nuovo**:
  la metrica `rapporti_sorvegliati` (natura `stock`) conta i rapporti sorvegliati, non gli interventi — e la
  scelta è motivata nel §3 della descrizione: l'intervento è l'azione che vogliamo che il cliente faccia, mettergli
  sopra un contatore significherebbe insegnargli a non farla.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `prepara_intervento(rapporto, piano?) → bozza dell'intervento, non consegnata`, marcato **scrittura** con
  **conferma umana**. La bozza preparata dalla chat nasce nello stesso stato `bozza` e segue la stessa macchina a
  stati: **non esiste un percorso privilegiato dal livello conversazionale**. Lo strumento che fa uscire qualcosa è
  `conferma_intervento`, dichiarato dalla storia `0020` come **scrittura irreversibile con conferma obbligatoria**.
  Il server conversazionale è di piattaforma e **non è ancora implementato** (UC 0061-0063); la storia `0029`
  assembla gli strumenti di scrittura.
- **RT-8 — Dati personali (§10).** **Sì.** Voce nuova nel manifesto
  `docs/compliance/manifests/fidelizzazione.yaml`, in **italiano e inglese**:
  `intervento.contenuto_e_stato` — dove vive: tabelle `intervento` e `transizione_intervento`; di chi è: cliente
  del nostro cliente (per riferimento) e, per i campi di autore, utente del nostro cliente; che dato è:
  comportamentale più prova; a cosa serve: sapere che cosa è stato proposto, da chi confermato, con che esito;
  base giuridica: esecuzione del rapporto commerciale fra il nostro cliente e il suo cliente; conservazione: 24
  mesi, con la prova di chi ha confermato (punto aperto n. 9). Campi annotati `@PersonalData`: contenuto, nota,
  autori delle transizioni. Le tabelle `intervento` e `transizione_intervento` entrano in `exportData` e in
  `purgeData` di `FidelizzazioneDataContract`. Il presidio contro le categorie particolari dell'articolo 9 sulla
  nota è **contrattuale e non tecnico** (avvertenza a schermo), come già per il motivo della contestazione
  (`0015`): non esiste un rilevamento automatico del contenuto e non se ne inventa uno.
- **RT-9 — Registrazione eventi (§14).** `intervento preparato`, `intervento confermato`, `intervento annullato
  (stato di partenza)`, `transizione respinta (stato di partenza, stato richiesto)`, con `tenant_id`, `app_id`,
  `user_id`, identificativo di correlazione e identificativo dell'intervento; **mai il contenuto**, mai la nota,
  mai l'etichetta del rapporto.

## 4. Criteri di accettazione

**CA-1 — Da bozza non si esce senza una persona**
- **Dato** un intervento in stato `bozza`, un account con qualunque piano e qualunque configurazione
- **Quando** girano tutte le lavorazioni pianificate e tutti i consumatori di eventi del servizio
- **Allora** l'intervento è ancora in stato `bozza`; e la **prova automatica** verifica che nessun percorso
  diverso da una richiesta portata da un token di utente possa produrre la transizione a `confermato`, fallendo se
  un tale percorso venisse introdotto

**CA-2 — La conferma mostra i tre fatti**
- **Dato** un intervento in bozza su un rapporto in fascia `a rischio`
- **Quando** l'utente apre la schermata di conferma
- **Allora** vede i tre contributi di peso maggiore del punteggio con tipo di segnale, data, peso e verso, sopra
  il testo che uscirà, prima di poter premere il pulsante

**CA-3 — Chi ha preparato e chi ha confermato**
- **Dato** un intervento preparato dall'utente `member` Anna e confermato dall'utente `owner` Luca
- **Quando** si apre la scheda dell'intervento
- **Allora** i due nomi e i due momenti compaiono in campi distinti, e la riga corrispondente esiste nello storico
  delle transizioni

**CA-4 — Transizione non ammessa**
- **Dato** un intervento in stato `consegnato`
- **Quando** si tenta di annullarlo
- **Allora** riceve `409` in `problem+json` con lo stato di partenza e quello richiesto, l'intervento resta
  `consegnato` e l'evento del rifiuto è registrato

**CA-5 — Avvertenza sull'articolo 9 e nota obbligatoriamente consapevole**
- **Dato** un utente che compila la nota di un intervento
- **Quando** apre il campo
- **Allora** vede sopra il campo, in tutte e cinque le lingue, l'avvertenza di non inserire dati sulla salute né
  altre categorie particolari

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri interventi
- **Quando** un utente di `A` tenta di confermare un intervento di `B` usandone l'identificativo
- **Allora** riceve `404`, nessuna transizione avviene e nessuna riga di `B` cambia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] **prova propria del divieto di automatismo**, richiesta esplicitamente da questa storia: prova strutturale
      che l'unico ingresso della transizione `bozza → confermato` è una richiesta con token di utente, e prova
      funzionale che l'esecuzione di tutte le lavorazioni pianificate e di tutti i consumatori di eventi lascia le
      bozze intatte. È una prova dell'area `backend`, distinta dal percorso end-to-end;
- [ ] prove di **unità** sulla macchina a stati (tutte le transizioni ammesse e tutte quelle vietate) e di
      **integrazione** sulla risorsa, con database effimero e migrazioni Flyway vere;
- [ ] prova di **isolamento fra account** sulle risorse `intervento` e `transizione_intervento`;
- [ ] prova sulla **matrice dei ruoli** secondo RF-7;
- [ ] **prova end-to-end**: *rimandare* per il percorso — `[J-FIDELIZZAZIONE]` nasce nella storia `0030` e dovrà
      coprire il tratto «preparo → confermo → l'intervento cambia stato»; voce `da-coprire` con motivo e storia
      proprietaria `0030` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml). La ripetizione del divieto a
      livello di percorso è della storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `intervento.contenuto_e_stato`, campi annotati
      `@PersonalData`, tabelle `intervento` e `transizione_intervento` presenti in `exportData` e in `purgeData`;
- [ ] **registro delle decisioni** compilato con: la macchina a stati implementata, perché chi prepara e chi
      conferma sono due campi, perché la conferma mostra i tre fatti, com'è costruita la prova del divieto;
- [ ] contratto dello strumento `prepara_intervento` dichiarato come **scrittura con conferma**, con la nota che
      la bozza dalla chat non ha alcun percorso privilegiato;
- [ ] documentazione aggiornata: la descrizione §4.4 riflette gli stati e le transizioni effettivamente
      implementati.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0018` — piani di intervento | un intervento nasce da un piano (o senza, ma il piano è la via normale) e ne eredita il testo di partenza |
| storia `0017` — il punteggio non decide da solo | fornisce il componente dei tre fatti principali che la conferma incastona, e il vincolo che qui si applica |
| storia `0009` — il rapporto sorvegliato | un intervento è sempre riferito a un rapporto |
| epica di piattaforma non implementata, UC 0061-0063 | `prepara_intervento` è dichiarato e non esposto: finché il livello non esiste, gli interventi si preparano dall'interfaccia |

## 7. Fuori ambito

- **la consegna** dell'intervento confermato all'app che possiede la relazione: storia `0020`. Qui l'intervento
  arriva a `confermato` e si ferma lì;
- **la lista di lavoro** per chi deve telefonare: storia `0021`;
- **le offerte di trattenuta**, con autorizzazione e tetto: storia `0022`;
- **i freni al contatto** (niente doppio contatto, tetto di frequenza, silenzio per rapporto): storia `0023`. Sono
  vincoli che si applicano alla conferma e alla consegna, e vivono lì per non gonfiare questa storia;
- **la pianificazione dei passi di un piano** con esecuzione a tempo: **esclusa per scelta, non rimandata**. Un
  passo che parte da un calendario è un passo che parte senza una persona, ed è il divieto che questa storia
  esiste per garantire.

## 8. Punti aperti

- **Se un `member` possa confermare un intervento senza concessioni economiche.** RF-7 dice di sì, e la
  motivazione è pratica: nelle micro-imprese chi tiene la relazione è spesso l'unico che lavora davvero
  sull'app, e obbligare il titolare a confermare ogni telefonata trasformerebbe il presidio in un ingorgo che
  qualcuno aggirerà condividendo le credenziali. L'obiezione contraria è seria: la conferma è il presidio, e
  allargarla lo diluisce. **Raccomandazione**: come da RF-7, con la possibilità per l'`owner` di stringere la
  regola per il proprio account. Chiude: **sviluppatore**, con la direzione di prodotto.
- **La conservazione a 24 mesi** di interventi e transizioni. È una proposta prudente e non un dato; la prova di
  chi ha confermato ha però un valore difensivo che potrebbe suggerire una durata diversa da quella dei segnali.
  Chiude: **revisione legale** — punto aperto n. 9 della descrizione.
