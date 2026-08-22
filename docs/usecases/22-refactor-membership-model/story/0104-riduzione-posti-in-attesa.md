# UC 0104 — Riduzione dei posti in attesa: scelta delle persone, blocco, annullamento, scadenza

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: ✅ implementato (change `0099-use-case-0104-riduzione-posti-attesa`)
**Epica**: [E22.2 Posti a pagamento](../epic/E22-02-posti-a-pagamento.md)
**Dipendenze**: UC 0102 (calcolo), UC 0103 (acquisto e abbonamento di piattaforma), UC 0026 (ciclo di vita dell'abbonamento), UC 0028 (cambio di piano programmato)
**Piano di lavoro**: [task/0104](../task/0104-riduzione-posti-in-attesa.md)
**Prototipo**: [owner.html](../prototype/owner.html), stato «riduzione in attesa»
**Ultimo aggiornamento**: 2026-08-22

## 1. Obiettivo / Scope

Dare forma allo stato più ricco dell'epica: ridurre i posti **non è immediato**. L'owner indica le persone
da cessare, l'account entra in **riduzione in attesa**, nessun posto nuovo si aggiunge finché quella
attesa non si chiude, e alla scadenza del periodo la riduzione si esegue davvero. L'attesa si può
**annullare** in qualunque momento.

**Incluso**: l'atto di indicare le persone; lo stato di riduzione in attesa a livello di account; il blocco
delle aggiunte; l'annullamento; l'esecuzione alla scadenza; l'aggiornamento dell'abbonamento; ciò che
vedono le persone indicate.

**Escluso**: il calcolo delle tariffe → UC 0102; l'acquisto → UC 0103; la fattura → UC 0106.

## 2. Attori & ruoli

- **Owner**: indica, annulla, e vede lo stato.
- **Persone indicate**: continuano a lavorare fino allo scadere. Non ricevono poteri diversi.
- **Lavoro periodico del sistema**: esegue la riduzione quando il periodo scade.

## 3. Precondizioni

- Esiste l'abbonamento di piattaforma con la sua quantità e la data di fine periodo (UC 0103).
- L'account ha più di un posto (l'owner non è indicabile).

## 4. Flusso principale

1. L'owner apre «Members» e sceglie **una o più persone** da cessare (l'indicazione è un elenco, non una
   azione singola: chi riduce di tre posti lo fa in una volta).
2. Il sistema mostra l'effetto prima della conferma: «cesseranno il 14 settembre; dal 15 pagherai
   17,94 € invece di 24,91 €», con il numero di posti risultante e la composizione degli scaglioni che si
   applicherà (nell'esempio: da dodici posti a nove, cioè da `7 × 2,99 + 2 × 1,99` a `6 × 2,99`).
3. L'owner conferma. L'account entra in **riduzione in attesa**: si registra l'elenco delle persone
   indicate e la **data di esecuzione**, che è la fine del periodo già pagato.
4. Da questo momento:
   - le persone indicate **restano attive**, con lo stesso accesso e gli stessi ruoli;
   - l'elenco le mostra in stato «**in cessazione dal …**»;
   - **nessun nuovo invito** è ammesso e nessuna nuova persona si aggiunge (requisito esplicito);
   - l'owner può **annullare** l'intera attesa, o toglierne una singola persona.
5. Alla **scadenza del periodo** il lavoro periodico esegue: rimuove le persone indicate (rimozione
   logica, come oggi), cancella i loro accessi alle applicazioni, aggiorna la quantità
   dell'abbonamento e ricalcola il dovuto per il periodo nuovo.
6. Lo stato di riduzione in attesa si chiude. Gli inviti tornano possibili.

## 5. Flussi alternativi / edge / errori

- **Annullamento**: ripristina tutto. Le persone tornano in stato normale, il blocco cade, la quantità
  dell'abbonamento non era mai stata cambiata (la riduzione è **programmata**, non applicata) e non c'è
  nulla da rimborsare o riaddebitare.
- **Errore — tentativo di invito durante l'attesa**: rifiuto tipizzato con il motivo e due vie d'uscita
  offerte nel testo: annullare la riduzione, oppure attendere il … . Non è un vicolo cieco, ed è
  importante che il messaggio lo dica.
- **Errore — tentativo di indicare l'owner**: rifiutato.
- **Edge — persona indicata che nel frattempo viene rimossa dall'owner**: la rimozione immediata è una
  operazione diversa e resta possibile; la persona esce dall'elenco degli indicati e il posto resta
  pagato fino a scadenza (nessun rimborso). Se restano zero indicati, l'attesa si chiude da sé.
- **Edge — persona indicata che va sospesa**: ammesso; sono cose ortogonali (una riguarda l'accesso,
  l'altra il posto).
- **Edge — l'owner vuole escludere subito qualcuno**: gli **toglie gli accessi alle applicazioni** (UC
  0111), operazione immediata e gratuita, e in più lo indica per la cessazione. Il testo
  dell'interfaccia deve suggerire questa combinazione, perché è la domanda che l'owner si farà.
- **Edge — periodo che scade mentre il pagamento del rinnovo è in corso**: la riduzione si esegue **prima**
  del calcolo del rinnovo, così il nuovo periodo nasce già con la quantità ridotta. L'ordine conta e va
  provato.
- **Edge — esecuzione fallita** (guasto durante il lavoro periodico): si ritenta; la riduzione resta in
  attesa e la sua data di esecuzione è **passata**, condizione che va misurata e allarmata. Un'attesa
  scaduta e non eseguita significa un cliente che paga posti che credeva chiusi.
- **Edge — account che disdice tutto**: l'attesa decade con l'account.

## 6. Schermate & stati

Nel riquadro dei posti di «Members», quando c'è una riduzione in attesa:

- riquadro di avviso, tono attenzione: «**Riduzione programmata** — 2 persone cesseranno il 14 settembre.
  Fino ad allora non puoi aggiungere persone.»
- elenco delle persone indicate, con la possibilità di **togliere una singola persona** dall'elenco;
- pulsante **«Annulla la riduzione»**, con conferma esplicita;
- il comando di invito è **disabilitato con spiegazione** (non nascosto: la funzione esiste, è
  temporaneamente non disponibile).

Nell'elenco delle persone, quelle indicate hanno lo stato «**in cessazione dal 14 settembre**» con
un'etichetta di tono attenuato — non allarmante, perché la persona sta lavorando normalmente.

Stati: caricamento, attesa presente, attesa scaduta e non ancora eseguita (messaggio onesto: «la
riduzione è in corso di esecuzione»), errore.

## 7. Dati toccati

- **Nuovo stato a livello di account**: la riduzione in attesa. Si può rappresentare come tabella
  `platform.seat_downgrade` con l'account, la data di esecuzione, lo stato (in attesa · eseguita ·
  annullata) e chi l'ha richiesta, più una tabella figlia con le **persone indicate**. Alternativa
  scartata: un contrassegno sulla riga della persona, che non permetterebbe di conoscere la data comune né
  di annullare l'insieme come un atto unico.
- **`platform.subscription`**: la quantità **non** cambia all'atto dell'indicazione; cambia alla scadenza.
  Il campo del cambio programmato che già esiste per i piani delle applicazioni è il precedente da
  seguire, non necessariamente da riusare.
- **Dati personali**: nessuna nuova categoria. Si registra che una persona è stata indicata per la
  cessazione e da chi: è un dato di gestione del rapporto, dentro la finalità già dichiarata. Va
  **conservato** solo per il tempo utile (la traccia di controllo ha già la sua conservazione).

## 8. Permessi & gate

- **Solo l'owner** indica, annulla e vede l'elenco degli indicati.
- **Blocco delle aggiunte**: presidio a livello di servizio, non solo di interfaccia. Vale per l'invito e
  per qualunque via che creerebbe una persona nuova.
- **L'owner non è indicabile.**
- **L'esecuzione alla scadenza è del sistema**, non richiede una persona.

## 9. Requisiti di test

- **Integrazione**: indicazione di due persone → stato di attesa con la data giusta; le persone restano
  attive e mantengono gli accessi.
- **Integrazione**: invito durante l'attesa → rifiuto tipizzato.
- **Integrazione**: annullamento → nessuna traccia residua, invito di nuovo possibile.
- **Integrazione**: esecuzione alla scadenza → persone rimosse, accessi cancellati, quantità
  dell'abbonamento ridotta, dovuto ricalcolato.
- **Ordine degli atti**: riduzione eseguita **prima** del calcolo del rinnovo.
- **Robustezza**: esecuzione interrotta e ritentata non rimuove due volte né lascia stati incoerenti.
- **Misura e allarme**: esiste una misura «riduzioni scadute e non eseguite» e va a zero in condizioni
  normali.
- **Percorso end-to-end di piattaforma**: l'owner indica una persona, prova a invitare (bloccato),
  annulla, invita (riuscito).

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [TierChangePolicy.java](../../../../services/core/src/main/java/app/appgrove/core/billing/TierChangePolicy.java)
  e [V4__subscription_scheduled_change.sql](../../../../services/core/src/main/resources/db/migration/V4__subscription_scheduled_change.sql)
  come precedenti del cambio programmato; [UC 0028](../../07-payments/0028-portale-cliente-self-service.md).
- **Definition of Done**:
  1. la riduzione è un atto unico su più persone, con data di esecuzione comune;
  2. le persone indicate lavorano fino allo scadere;
  3. nessuna aggiunta durante l'attesa, con presidio nel servizio;
  4. l'annullamento è completo e senza effetti contabili;
  5. l'esecuzione alla scadenza è automatica, robusta e misurata;
  6. `run-tests.sh backend frontend` verde più il percorso di piattaforma.

## Punti aperti / decisioni differite

### Lasciato da UC 0103 (change 0098)

- **Il gate «nessun invito con una riduzione in attesa» ha già il suo posto, e non fa ancora niente.** La
  change 0098 ha scritto la sequenza ordinata della creazione dell'invito e ha lasciato il passo (3) come
  **commento** in `InvitationResource.create`, subito dopo il controllo sullo stato dell'account e prima di
  ogni calcolo: è lì che il rifiuto va aggiunto. Non è stato messo un metodo vuoto di proposito — un gate che
  passa sempre è codice morto, un commento nel punto esatto è un'indicazione. Sul lato dell'interfaccia il
  campo esiste già: `GET /api/platform/v1/me/seats` restituisce `pendingReduction`, oggi sempre falso, e il
  riquadro dei posti sa già mostrare l'avviso e spegnere il pulsante di invito quando è vero — quindi questa
  storia deve solo far diventare vero quel campo.
- **La quantità dell'abbonamento dei posti scende solo qui.** La change 0098 la fa **solo salire** (è un
  high-water mark del periodo, ed è la ragione per cui un invito scaduto non produce né rimborso né secondo
  addebito). Il ritorno al numero di posti effettivamente occupati, alla scadenza dell'attesa, è di questa
  storia: il punto di scrittura è `SeatSubscriptionWriter`, che ha già il metodo per riportare la quantità a
  un valore precedente.

### Altri punti


- **Avviso per email alla persona indicata**: se e quando avvertirla. Proposta prudente: **non** avvisarla
  automaticamente (è una comunicazione che spetta al datore di lavoro, non alla piattaforma), ma renderlo
  possibile in futuro. Proprietario: questa storia. **Confermato dalla change 0099**: nessun avviso
  automatico è stato implementato, la scelta è dichiarata nel manifesto dati (voce
  `seat_reduction_people.identity_id`) e il dubbio di trasparenza verso l'interessato (art. 13/14) è
  tracciato come voce **L19** in [docs/_REVISIONE-LEGALE.md](../../../_REVISIONE-LEGALE.md).
- **Riduzione parziale automatica** quando una persona rifiuta o un invito scade: già gestita come
  liberazione di posto in UC 0103, senza passare dall'attesa. Confermare che i due meccanismi non si
  sovrappongano in implementazione. **Verificato dalla change 0099**: non si sovrappongono, e la ragione è
  strutturale — l'attesa agisce sulle **appartenenze**, la liberazione del posto sugli **inviti**. Un invito
  scaduto o revocato non è indicabile per la cessazione (non ha un'appartenenza), e la quantità
  dell'abbonamento non scende per quella via: scende solo all'esecuzione della riduzione, ricalcolata.
- **Durata massima dell'attesa**: coincide col periodo di fatturazione (un mese). Se un giorno esistesse
  il ciclo annuale, un'attesa di undici mesi sarebbe inaccettabile e servirebbe una regola diversa.
  Proprietario: UC 0102.

### Lasciato da UC 0100 (change 0096) — **chiuso dalla change 0099**

L'elenco unico ha ora il **quarto stato**, «in cessazione dal …», con la data e nelle cinque lingue. Una
precisazione rispetto a come il punto era stato scritto: lo stato **non sostituisce** «sospesa» ma le si
affianca — la sospensione e la cessazione programmata sono ortogonali (§5), e mostrarne una sola avrebbe
fatto sparire l'altra dalla schermata. In pratica la riga porta la data in un campo suo (`endingAt`) e
l'interfaccia mostra due etichette quando i due stati convivono.

- **Lo stato «in cessazione» nell'elenco unico delle persone** non esiste ancora, e l'elenco mostra
  **tre** stati invece dei quattro che la storia 0100 §4 elencava: attiva, invito in attesa, sospesa.
  Motivo: l'appartenenza ha due soli stati nel modello (`active`, `suspended`) e la cessazione nasce
  qui, con la logica dei posti. Aggiungere subito a schermo una etichetta che nessun dato può produrre
  avrebbe significato scrivere in cinque lingue una parola morta. Quando questa storia introduce
  l'indicazione per la cessazione, l'elenco unico va esteso con il quarto stato (**con la data di
  esecuzione**, perché «in cessazione» senza il quando non dice nulla) e con la sua azione di riga.
  Proprietario: questa storia.

### Lasciato dalla change 0099 (questa storia, implementata)

- **La riduzione presuppone un abbonamento dei posti, e senza quello si rifiuta.** Un account interamente
  dentro la franchigia riceve un rifiuto tipizzato (`urn:appgrove:seats:reduction-not-needed`) che indirizza
  alla rimozione immediata, gratuita. È la lettura letterale della precondizione §3, e regge finché la
  franchigia è di tre posti su un solo scaglione gratuito. Se un giorno il listino avesse **due** scaglioni
  a pagamento e un account potesse voler scendere di scaglione restando a pagamento, il rifiuto resterebbe
  giusto; se invece la franchigia diventasse configurabile per account, andrebbe rivisto. Proprietario:
  UC 0105 (governo del listino).
- **Il rinnovo del periodo dei posti non esiste ancora**, e questa storia non lo introduce: `execute_at` è
  la fine del periodo scritto sull'abbonamento, e nessuno lo fa avanzare (rimando già di UC 0106). La
  conseguenza pratica, e va detta perché è visibile in collaudo: eseguita una riduzione, la data del periodo
  resta nel passato finché UC 0106 non arriva, quindi una **seconda** riduzione nascerebbe con una data di
  esecuzione già passata e verrebbe eseguita al primo giro dello spazzino — cioè subito. Non è un difetto di
  questa storia (la permanenza minima è già stata pagata e consumata) ma diventa scorretto nel momento in
  cui il periodo si rinnova: la correttezza dipende da UC 0106, non da un presidio in più qui.
  Proprietario: **UC 0106**.
- **L'ordine col rinnovo è presidiato ma non ancora esercitato dal vero.** La riduzione dovuta si esegue
  nella stessa transazione dell'evento del fornitore che riscrive il periodo dell'abbonamento dei posti
  (`SubscriptionWriter`), e c'è un collaudo che lo prova consegnando l'evento a mano. Nella realtà oggi
  quell'evento non arriva mai, perché il prodotto dei posti presso il fornitore non esiste (prerequisito
  #14): quando esisterà, il percorso va riesercitato con il fornitore vero. Proprietario: UC 0106.
- **Lo spazzino gira sullo scheduler applicativo**, come gli altri: il richiamo dal temporizzatore gestito
  del cloud è di UC 0035, e vale per questo spazzino come per `AccountDeletionSweeper`.
