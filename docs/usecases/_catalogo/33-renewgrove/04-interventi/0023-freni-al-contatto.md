# 0023 — Freni al contatto

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 04 — Interventi con conferma umana
**Storia**: `0023` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`, `0020`, `0021`
**Ultimo aggiornamento**: 2026-08-04

## 1. Narrazione

> Come titolare che ha passato vent'anni a costruire la fiducia dei suoi clienti
> voglio che questa app non possa mai farmi chiamare due volte la stessa persona nella stessa settimana, né
> scrivere a chi mi ha detto di non farlo
> così da usare uno strumento che mi fa recuperare clienti invece di uno che me li fa scappare per fastidio.

**Contesto.** Le cinque storie precedenti dell'epica 04 hanno risolto il problema di **chi manda**: una persona, che
guarda e conferma (`0019`), verso l'app che possiede la relazione (`0020`) o attraverso una telefonata (`0021`), con
un tetto su ciò che costa (`0022`). Nessuna di loro ha risposto alla domanda opposta, che è quella di **chi
riceve**: quante volte, ogni quanto, e se ha già chiesto di essere lasciato in pace. Un intervento confermato da una
persona resta un intervento sbagliato se è il terzo in dieci giorni sullo stesso cliente.

È il difetto che questo genere di prodotti fa più spesso, e non per cattiveria: un elenco di clienti a rischio è un
elenco che invita ad agire, e agire due volte sembra sempre meglio che agire una. Il §2.5 della
[descrizione](../application-description.md) dice però la cosa esatta su questo segmento: «*un titolare di
micro-impresa conosce i suoi clienti per nome: un messaggio partito a sua insaputa è un danno di reputazione, non un
risparmio di tempo*». Vale identico per un messaggio partito **a sua saputa e di troppo**. La conferma umana difende
dal messaggio che nessuno ha voluto; i freni di questa storia difendono da quello che qualcuno ha voluto **una volta
di troppo**, ed è un rischio diverso che vuole un presidio diverso.

C'è poi un motivo che riguarda la suite e non solo questa app. Il §10 fissa una **regola operativa che porta il nome
di questa storia**: *due app non contattano lo stesso cliente lo stesso giorno* — il caso è **03 CashGrove** che
insegue il denaro mentre RenewGrove insegue la relazione, sullo stesso ritardo di pagamento e sulla stessa persona.
Il freno completo fra applicazioni presuppone che ciascuna dichiari i propri contatti su un canale comune, **che non
esiste** (punto aperto n. 11): qui si fa la parte che è fattibile oggi e si dice con chiarezza dove finisce.

## 2. Requisiti funzionali

1. **RF-1** — Esiste l'entità `RegoleDiContatto`, una per account: **giorni di silenzio** dopo un contatto sullo
   stesso rapporto, **un solo intervento aperto** per rapporto, **tetto di conferme** su una finestra dichiarata per
   l'intero account. Ogni account nasce con le tre regole **già attive** e con valori predefiniti prudenti — un
   freno che va acceso è un freno che manca proprio nelle prime settimane, quando il cliente sperimenta e sbaglia.
   I valori si cambiano da `owner`; **non esiste alcuna configurazione che disattivi i freni in blocco**.
2. **RF-2** — 🛑 Esiste il **divieto di contatto** su un rapporto: il fatto che quella persona abbia chiesto di non
   essere più contattata. È **assoluto e non scavalcabile da nessun ruolo e da nessuna via** — interfaccia,
   strumento conversazionale, ripiego sulla lista di lavoro. Su un rapporto con divieto **non si crea** un
   intervento: la richiesta è respinta con un messaggio che spiega il motivo per esteso, non con un codice muto.
   Il divieto si pone e si revoca da un nostro utente (`owner`, `admin`), con autore, momento e origine conservati;
   resta valido finché non è revocato e **non scade con il silenzio**.
3. **RF-3** — Il divieto **non è un segnale e non entra nel punteggio**: viaggia su un **evento separato** con un
   consumatore distinto, come già l'etichetta del rapporto (§4.2 della descrizione). Le due ragioni sono entrambe
   sostanziali — un segnale è un *fatto che pesa*, il divieto è una *volontà che vincola*; e far pesare nel
   punteggio il fatto che qualcuno abbia chiesto di essere lasciato in pace sarebbe usare la sua opposizione per
   giudicarlo. Finché il produttore dell'evento non esiste in nessuna applicazione della suite, il divieto si pone
   **a mano** — che è anche il caso reale, perché il cliente lo dice al telefono — e il consumatore resta
   implementato e collaudato con un produttore simulato.
4. **RF-4** — **Silenzio per rapporto**: entro i giorni dichiarati da un contatto sullo stesso rapporto, la conferma
   di un altro intervento è **respinta**, con la data in cui il silenzio finisce. Il momento del contatto è la
   **consegna** per gli interventi consegnati a un'app (`0020`) e l'**esito registrato** per quelli lavorati dalla
   lista (`0021`); l'esito `rimandato` non è un contatto e non fa decorrere nulla. Contano allo stesso modo i
   contatti che **un'altra applicazione dichiara** con un tipo di segnale che significa «ho già contattato questa
   persona» — oggi solo `sollecito_inviato` di SubGrove (`0006` §2.1): sono contatti veri verso la stessa persona, e
   ignorarli sarebbe la scusa più comoda.
5. **RF-5** — **Niente doppio contatto**: se sul rapporto esiste già un intervento aperto — confermato o consegnato
   e non ancora concluso — la conferma di un secondo è **respinta**, indicando chi l'ha confermato e quando. Non
   sostituisce la presa in carico della lista di lavoro (`0021` RF-4), che impedisce a due persone di lavorare la
   **stessa** voce: qui si impedisce che esistano **due voci** sulla stessa persona.
6. **RF-6** — I tre freni di prudenza — silenzio, doppio contatto, tetto di conferme — si **scavalcano**, il divieto
   di RF-2 **no**. Lo scavalco è di `owner` e `admin`, mai di `member`, richiede un **motivo scelto da un elenco
   chiuso** (il cliente ci ha contattati lui · fatto nuovo e grave · errore nell'intervento precedente · richiesta
   esplicita del cliente) e lascia una riga con autore, motivo e momento. Gli scavalchi del periodo sono **contati e
   mostrati accanto alle regole**: un freno scavalcato spesso non è un freno rispettato con giudizio, è un freno
   tarato male, e l'app deve dirlo invece di lasciarlo scoprire ai clienti finali.
7. **RF-7** — I freni si valutano **tre volte, e si mostrano prima di respingere**: quando si prepara un intervento
   l'interfaccia dichiara già quale freno scatterà; alla **conferma** il freno respinge; **immediatamente prima
   della pubblicazione** dell'evento di consegna si rivaluta il **solo divieto** di RF-2, e se è sopravvenuto
   l'intervento passa in `annullato` con il motivo — transizione già ammessa dal §4.4 prima della consegna — e
   **nulla esce**. Non è un automatismo reintrodotto dalla porta di servizio: **l'unico automatismo ammesso in
   questa applicazione è quello che impedisce un effetto, mai quello che ne produce uno.**

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `regole_di_contatto`, `divieto_di_contatto` e
  `scavalco_di_freno` filtra per `tenant_id` preso dal token di accesso verificato; un `tenant_id` che arrivasse dal
  corpo della richiesta o dai parametri viene ignorato. Le regole di un account non sono leggibili né applicabili a
  un altro, e un divieto posto in `A` non frena e non è visibile in `B`. Sull'evento del divieto in ingresso vale la
  regola già stabilita dalla storia `0007`: il `tenant_id` si **copia** dall'evento, e un evento con `tenant_id`
  mancante o sconosciuto viene **scartato**.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET` e `PUT /api/fidelizzazione/v1/regole-di-contatto` (le
  tre regole più il conto degli scavalchi del periodo),
  `POST /api/fidelizzazione/v1/rapporti/{id}/divieto-di-contatto` e
  `POST /api/fidelizzazione/v1/rapporti/{id}/divieto-di-contatto/revoca`,
  `GET /api/fidelizzazione/v1/rapporti/{id}/freni` (quali freni scatterebbero adesso e perché, per poterlo dire
  prima). **Lo scavalco non ha una rotta propria**: è un campo facoltativo — motivo dall'elenco chiuso — della
  conferma già esistente `POST /api/fidelizzazione/v1/interventi/{id}/conferma` (`0019`). Una seconda via di
  conferma «senza freni» sarebbe esattamente il buco che questa storia esiste per chiudere. Corpo validato; errori
  in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V19__freni_al_contatto.sql` sullo schema `app_fidelizzazione`: tabelle
  `regole_di_contatto` (`tenant_id`, giorni di silenzio, tetto di conferme, ampiezza della finestra),
  `divieto_di_contatto` (`tenant_id`, rapporto, origine — utente o evento —, autore, momento, momento e autore della
  revoca) e `scavalco_di_freno` (`tenant_id`, intervento, freno scavalcato, motivo dall'elenco chiuso, autore,
  momento); chiave primaria UUID versione 7, colonne di controllo `created_at`, `updated_at`, `created_by`,
  `updated_by` e cancellazione logica `deleted_at`. Nessuna chiave esterna verso altri schemi. **Il momento
  dell'ultimo contatto non si duplica in una colonna**: si deriva dagli interventi e dai segnali di contatto altrui,
  perché due verità sull'ultima volta che abbiamo disturbato una persona sono peggio di una sola scomoda da
  calcolare. Il vincolo di unicità su `(tenant_id, rapporto)` per il divieto vivo impedisce due divieti
  contraddittori sullo stesso rapporto.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione impostazioni, il riquadro **Freni al contatto** con le tre
  regole, i loro valori e il conto degli scavalchi del periodo. Sulla scheda del rapporto, lo stato di
  contattabilità in chiaro — «contattabile» · «in silenzio fino al *…*» · «🛑 non contattare, dal *…*» — e il gesto
  per porre o revocare il divieto. Nella schermata di conferma (`0019`), la riga del freno **sopra il pulsante**,
  accanto ai tre fatti principali e alla riga che dice dove andrà l'intervento (`0020`): chi conferma vede prima di
  premere i motivi, la destinazione e gli ostacoli. Lo scavalco è un gesto **deliberatamente scomodo**: elenco dei
  motivi, nessuna scelta preselezionata, e la conseguenza scritta accanto. Dati letti e scritti con il client
  generato; solo token del sistema di design; funziona in tema chiaro e scuro; controllo automatico di
  accessibilità.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe — nomi dei tre freni, messaggi di rifiuto con la data di fine
  silenzio, testo del divieto e della sua revoca, le quattro voci dell'elenco dei motivi di scavalco, l'avviso che
  compare in preparazione — passano dallo spazio-nomi `fidelizzazione` e sono presenti in `en, it, fr, es, de`. Il
  messaggio del divieto va scritto bene in tutte e cinque: è il testo che evita a un cliente di sembrare uno che non
  ascolta.
- **RT-6 — Varchi e quota (§6, §7).** Catena dei varchi completa: `401`, `403` ad app spenta, `402` ad account non
  abilitato o abbonamento `canceled`, `403` a ruolo insufficiente — le regole si cambiano da `owner`, il divieto si
  pone e si revoca da `owner` e `admin`, lo scavalco è di `owner` e `admin`. **Nessun consumo di quota nuovo**: la
  metrica `rapporti_sorvegliati` (natura `stock`) conta i rapporti, non i freni. E soprattutto: **un freno non
  risponde mai `429`**. Il `429` significa «hai finito, compra di più»; questi limiti non si comprano e non si
  sbloccano con un piano superiore. Un freno legato al rapporto — divieto, silenzio, doppio contatto — risponde
  `409`, perché è un conflitto con lo stato di quel rapporto; il tetto di conferme dell'account risponde `422`, come
  il tetto delle offerte (`0022`) e per la stessa ragione.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento nuovo**, e la scelta è motivata: la tabella del
  §7 della descrizione non ne prevede, e i freni non sono una funzione da comandare ma una condizione da subire. Il
  contratto degli strumenti già dichiarati però **cambia qui e va dichiarato ora**: `prepara_intervento` (`0019`) e
  `conferma_intervento` (`0020`) applicano **gli stessi freni dell'interfaccia**, e li applicano **prima** di
  produrre la bozza, così che dalla chat non esista una via più permissiva — è la stessa regola che `0022` impone a
  `autorizza_offerta`. Il risultato di `stato_rapporto` (`0028`) porta il campo «contattabile adesso? sì/no, e
  perché no». **Lo scavalco non è esposto come strumento, per scelta**: scavalcare un freno è precisamente il gesto
  che non si delega a un assistente. Il server conversazionale è di piattaforma e **non è ancora implementato**
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Sì.** Voce nuova nel manifesto
  `docs/compliance/manifests/fidelizzazione.yaml`, in **italiano e inglese**: `contatto.divieto_e_scavalchi` — dove
  vive: tabelle `divieto_di_contatto` e `scavalco_di_freno`; di chi è: cliente del nostro cliente (per riferimento,
  per il divieto) e utente del nostro cliente (per gli autori e i motivi); che dato è: **volontà espressa più
  prova**; a cosa serve: non contattare chi ha chiesto di non esserlo, e poter dimostrare che il freno è stato
  rispettato o consapevolmente scavalcato; base giuridica: esecuzione del rapporto commerciale fra il nostro cliente
  e il suo cliente, e — per il divieto — il rispetto della volontà che l'interessato ha manifestato **al nostro
  cliente**; conservazione: quanto il rapporto, con il paradosso dichiarato nella sezione 8. Campi annotati
  `@PersonalData`: riferimento al rapporto, autori, momenti. Le tabelle `divieto_di_contatto` e `scavalco_di_freno`
  entrano in `exportData` e in `purgeData` di `FidelizzazioneDataContract` e **vanno aggiunte all'elenco vivo del §6
  della descrizione**, che oggi non le nomina. `regole_di_contatto` contiene giorni e conteggi e resta fuori, come
  `modello_di_punteggio`. **Il motivo dello scavalco è un elenco chiuso e non un testo libero, per scelta
  deliberata**: sarebbe stato il quarto punto di testo libero dell'app — dopo la contestazione (`0015`), la nota
  dell'intervento (`0019`) e la nota della lista di lavoro (`0021`) — proprio nel momento in cui qualcuno spiega
  perché quella persona va contattata comunque, che è il momento in cui scriverebbe una ragione di salute.
- **RT-9 — Registrazione eventi (§14).** `intervento respinto per divieto di contatto`,
  `conferma respinta per silenzio (giorni residui)`, `conferma respinta per contatto già in corso`,
  `conferma respinta per tetto di frequenza`, `freno scavalcato (quale freno, motivo dall'elenco chiuso, ruolo di
  chi ha scavalcato)`, `consegna fermata da divieto sopravvenuto`, `divieto posto / revocato (origine)`, con
  `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e identificativo dell'intervento o del rapporto;
  **mai l'etichetta del rapporto**, mai il contenuto dell'intervento.

## 4. Criteri di accettazione

**CA-1 — Silenzio per rapporto, e lo scavalco che lo apre**
- **Dato** un rapporto contattato tre giorni fa, in un account con quattordici giorni di silenzio
- **Quando** un utente `admin` tenta di confermare un secondo intervento su quel rapporto
- **Allora** riceve `409` in `problem+json` con il nome del freno e la data in cui il silenzio finisce, l'intervento
  resta in `bozza`; ripetendo la conferma con il motivo «il cliente ci ha contattati lui» la conferma va a buon
  fine, esiste la riga di scavalco con autore, motivo e momento, e il conto degli scavalchi del periodo aumenta di
  uno

**CA-2 — 🛑 Il divieto non si scavalca, da nessuna via**
- **Dato** un rapporto con divieto di contatto vivo
- **Quando** un utente `owner` tenta di creare un intervento su quel rapporto, con e senza motivo di scavalco, e lo
  stesso tenta lo strumento `prepara_intervento`
- **Allora** in tutti i casi la richiesta è respinta con `409` e il messaggio che spiega per esteso il motivo,
  nessun intervento viene creato, e la spiegazione è presente in tutte e cinque le lingue

**CA-3 — Niente due voci sulla stessa persona**
- **Dato** un rapporto con un intervento già confermato e in attesa di esito, e un ufficio di tre utenti
- **Quando** un secondo utente conferma un altro intervento sullo stesso rapporto
- **Allora** riceve `409` con l'indicazione di chi ha confermato il primo e quando; e la stessa situazione prodotta
  da un contatto dichiarato da un'altra applicazione (`sollecito_inviato` di SubGrove, ricevuto ieri) fa scattare il
  silenzio allo stesso modo

**CA-4 — Il tetto di frequenza non è quota**
- **Dato** un account che ha raggiunto il tetto di conferme della finestra dichiarata
- **Quando** un utente tenta di confermare un ulteriore intervento
- **Allora** riceve `422` — **non** `429` — con il tetto, il conteggio corrente e un messaggio che **non** propone
  di passare a un piano superiore, perché non è così che si rimedia; nessun intervento cambia stato

**CA-5 — Divieto sopravvenuto, consegna fermata**
- **Dato** un intervento `confermato` e non ancora consegnato
- **Quando** il divieto di contatto sul suo rapporto arriva prima della pubblicazione dell'evento
- **Allora** l'intervento passa in `annullato` con il motivo, **nessun evento di richiesta di comunicazione viene
  pubblicato**, nessuna voce compare nella lista di lavoro, e il fatto è registrato e leggibile sulla scheda

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con regole diverse, e un divieto posto in `B`
- **Quando** un utente di `A` conferma un intervento forzando nella richiesta gli identificativi di `B`
- **Allora** si applicano le regole di `A`, il divieto di `B` non è né visibile né applicato, e nulla di `B` è
  leggibile o modificato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sul calcolo del momento dell'ultimo contatto (consegna, esito della lista, segnale di
      contatto altrui, esito `rimandato` che non conta), sulla finestra del silenzio e sul conteggio del tetto;
      prove di **integrazione** sulle risorse, con database effimero e migrazioni Flyway vere;
- [ ] **prova propria del divieto**, richiesta esplicitamente da questa storia: nessuna via — interfaccia, strumento
      conversazionale, campo di scavalco, ripiego sulla lista di lavoro — crea o fa uscire un intervento verso un
      rapporto con divieto vivo; la prova fallisce se una via nuova venisse introdotta;
- [ ] **prova che i freni si applicano prima della bozza** anche sul percorso degli strumenti conversazionali, come
      per `autorizza_offerta` (`0022`);
- [ ] prova di **isolamento fra account** sulle risorse `regole_di_contatto`, `divieto_di_contatto` e
      `scavalco_di_freno`, e sullo scarto di un evento di divieto con `tenant_id` sconosciuto;
- [ ] prova sulla **matrice dei ruoli**: regole da `owner`, divieto e scavalco da `owner` e `admin`, `member` mai;
- [ ] **prova end-to-end**: *rimandare* — il percorso `[J-FIDELIZZAZIONE]` nasce nella storia `0030` e dovrà coprire
      il tratto «confermo → riconfermo subito → il freno respinge → scavalco con motivo → passa»; voce `da-coprire`
      con motivo e storia proprietaria `0030` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml). La ripetizione del divieto a
      livello di percorso appartiene alla storia `0031`, insieme agli altri presidi di non-aggiramento;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), messaggio del divieto ed elenco dei
      motivi di scavalco compresi;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `contatto.divieto_e_scavalchi`, campi annotati
      `@PersonalData`, tabelle `divieto_di_contatto` e `scavalco_di_freno` in `exportData` e in `purgeData`, e
      **l'elenco vivo del §6 della descrizione esteso** con le due tabelle nuove;
- [ ] **registro delle decisioni** compilato con: perché i freni nascono accesi e non si spengono, perché il divieto
      non è un segnale e non entra nel punteggio, perché i freni di prudenza si scavalcano e il divieto no, perché
      un freno risponde `409`/`422` e mai `429`, perché il motivo dello scavalco è un elenco chiuso;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo; l'applicazione dei freni **prima della bozza**
      dichiarata su `prepara_intervento` e `conferma_intervento`, il campo di contattabilità su `stato_rapporto`, e
      la scelta scritta di non esporre lo scavalco;
- [ ] documentazione aggiornata: il §4.4 della descrizione riflette che l'annullamento da `confermato` può avere
      origine in un freno, e il §10 rimanda a questa storia per la parte di regola operativa effettivamente
      implementata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` — intervento con conferma umana | i freni si applicano alla creazione e alla conferma, e lo scavalco è un campo della conferma esistente: senza la macchina a stati non c'è niente da frenare |
| storia `0020` — consegna all'app proprietaria | il momento del contatto per gli interventi consegnati è la pubblicazione dell'evento, e il divieto sopravvenuto ferma la consegna prima che l'evento esca |
| storia `0021` — lista di lavoro | il momento del contatto per gli interventi lavorati a mano è l'esito registrato; la presa in carico è un presidio diverso e complementare |
| storia `0009` — il rapporto sorvegliato | il divieto e il silenzio vivono su un rapporto |
| storia `0006` — contratto del segnale | il tipo `sollecito_inviato` di SubGrove è oggi l'unico contatto altrui che RenewGrove sa riconoscere; senza quel tipo nell'elenco chiuso, il freno vede solo i propri contatti |
| **contratto dell'evento di divieto di contatto** — decisione di piattaforma, **non esistente** | nessuna applicazione della suite lo pubblica oggi. Nel frattempo: il divieto si pone a mano dall'interfaccia e il consumatore resta implementato e collaudato con un produttore simulato — stessa postura della storia `0020` |
| **canale comune dei contatti fra applicazioni** — punto aperto n. 11, **non esistente** | senza, il freno fra CashGrove e RenewGrove del §10 resta parziale: si frena su ciò che RenewGrove sa, e lo si dichiara a schermo |
| epica di piattaforma non implementata, UC 0061-0063 | i freni si applicano agli strumenti già dichiarati, che sono dichiarati e non esposti |

## 7. Fuori ambito

- **il freno completo fra applicazioni diverse** — «due app non contattano lo stesso cliente lo stesso giorno»
  (§10): qui si fa la parte fattibile oggi (contano i contatti che RenewGrove conosce, propri e dichiarati da un
  segnale) e si dichiara a schermo che il resto non è visibile. Il canale comune è del punto aperto n. 11, di
  piattaforma;
- **le finestre orarie** («non prima delle nove», «mai di domenica»): rimandate, e con una ragione precisa —
  RenewGrove non invia nulla, e la telefonata la fa una persona che sa che ora è. Tornerebbero utili solo con la
  via B del §4.3, che è esclusa;
- **freni distinti per canale** (due messaggi di posta sì, due telefonate no): rimandati. Il canale è
  dell'intervento (`0019`) e la distinzione è ragionevole, ma triplica le regole prima di sapere se le tre di base
  sono tarate bene;
- **la qualificazione della comunicazione come promozionale** e il regime di consenso che ne discenderebbe: fuori.
  Gli interventi sono comunicazioni dentro un rapporto commerciale esistente; se un cliente ne facesse un canale
  promozionale, la qualificazione è del titolare e non di questa app. Il divieto di RF-2 vale comunque, in entrambe
  le letture;
- **spegnere i freni**: **escluso per scelta, non rimandato**. Non esiste un interruttore generale, perché sarebbe
  la prima cosa che qualcuno cerca il giorno in cui ha fretta — ed è esattamente il giorno in cui il freno serve.
  Si allargano i valori, si scavalca un caso per volta con un motivo, e ogni scavalco si vede;
- **avvisare il cliente finale** che è sotto sorveglianza o che ha un divieto attivo: non è materia di questa
  storia. L'informativa verso i clienti finali è del nostro cliente (punto aperto n. 4) e l'esportazione dei suoi
  dati è la storia `0032`.

## 8. Punti aperti

- **Che fine fa il divieto quando il rapporto viene cancellato.** È il paradosso classico degli elenchi di
  esclusione e qui è reale: la cancellazione in questa app è **fisica** (§6), quindi cancellare un rapporto
  cancella anche il suo divieto — e se la fonte ripubblicasse quel cliente, il rapporto rinascerebbe
  **contattabile**, perdendo proprio la protezione che la persona aveva chiesto. Le due vie sono conservare un
  riferimento minimo che sopravvive alla cancellazione — cioè trattenere un dato di chi ha chiesto di sparire, nel
  suo interesse — oppure accettare la perdita e dichiararla. **Raccomandazione**: accettare la perdita ora,
  dichiararla nel manifesto come limite noto, e non inventare una via di mezzo. Chiude: **revisione legale**,
  insieme al punto aperto n. 4 e in coordinamento con la storia `0032`.
- **I valori predefiniti dei tre freni.** Proposti: **quattordici giorni** di silenzio per rapporto, **un solo**
  intervento aperto per rapporto, **venti conferme al giorno** per account. Il primo è la metà del ciclo mensile
  tipico di questi rapporti; il terzo è deliberatamente alto — in una micro-impresa non si raggiunge mai, e se si
  raggiunge non è più lavoro sulla relazione ma una campagna di massa, che è precisamente ciò che questo prodotto
  non è. Nessuno dei tre poggia su un dato: sono convenzioni dichiarate, come i pesi di partenza del punteggio
  (§2.7). Chiude: **sviluppatore**, con la direzione di prodotto.
- **Se lo scavalco debba esistere.** L'obiezione è la stessa che `0022` fa sul tetto delle offerte — «un freno che
  si può superare non è un freno» — ed è seria. La risposta è che i due casi non sono uguali: il tetto economico
  protegge il **margine del titolare**, che è suo e che nessuna urgenza rende meno suo; il silenzio protegge la
  **pazienza del cliente finale**, e ci sono casi legittimi e frequenti in cui contattare di nuovo è giusto — il
  cliente che ha chiamato lui, il guasto grave del giorno dopo. Un freno rigido in quei casi verrebbe aggirato
  allargando i valori una volta per tutte, e un freno allargato per sempre è peggio di uno scavalcato tre volte e
  contato. **Raccomandazione**: come da RF-6, scavalco tracciato e contato per i tre freni di prudenza, nessuno
  scavalco per il divieto. Chiude: **sviluppatore**, con la direzione di prodotto.
- **Il freno fra applicazioni resta parziale finché il canale comune non esiste** (punto aperto n. 11): oggi
  RenewGrove riconosce i contatti altrui solo se una fonte li pubblica come segnale, e nessuna applicazione è
  obbligata a farlo. La conseguenza va scritta a schermo e non nascosta: «*questo freno vede i contatti di
  RenewGrove e quelli che le tue altre applicazioni dichiarano; non vede gli altri*». Chiude: **piattaforma**,
  insieme al punto aperto n. 2.
