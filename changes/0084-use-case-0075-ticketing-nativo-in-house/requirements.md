# Change 0084: Ticketing nativo in-house — completamento della storia 0075

**Branch**: `change/0084-use-case-0075-ticketing-nativo-in-house`
**Aree**: `services/core`, `services/commons` (solo consumo), `shared/email-templates`, `frontend/apps/backoffice`, `frontend/apps/admin`, `tools/platform-e2e`, `docs`
**Data**: 2026-08-02
**Autore**: Platform Engineering (modalità fast, agente)
**Use case sorgente**: [`docs/usecases/15-supporto-e-piattaforma/0075-ticketing-nativo-in-house.md`](../../docs/usecases/15-supporto-e-piattaforma/0075-ticketing-nativo-in-house.md)
**Tocca dati personali?**: Sì — classificazione **MINOR**. Non nascono nuove categorie di dati, nuove finalità,
nuove basi giuridiche né nuovi responsabili esterni: il contenuto libero dei ticket (oggetto e messaggi) è già
dichiarato nel manifesto `docs/compliance/manifests/platform.yaml` con finalità «gestione delle richieste di
supporto e di esercizio dei diritti», base contrattuale più obbligo di legge per i ticket privacy e conservazione
di 24 mesi dalla chiusura. Le due colonne nuove (`source`, `flagged_for_review`) sono **metadati operativi non
personali**: da dove è arrivata la richiesta e se un essere umano deve guardarla per primo.

## Problema / Obiettivo

Il ticketing in-house esiste già in forma minima: è nato dentro la change `0030` come strumento della Console
"Diritti GDPR" (UC 0034). Oggi c'è l'entità `support_ticket` col filo di messaggi, la pagina «Supporto» del
backoffice, l'auto-ticket sull'esportazione fallita, la conservazione a 24 mesi e un blocco «Ticket» **innestato
dentro** la pagina Diritti GDPR della console di amministrazione.

Quel che manca è ciò che rende il sistema un vero canale di assistenza, e che la storia 0075 chiede:

1. il ciclo di vita è **monco**: non esiste lo stato «in attesa dell'utente», quindi chi risponde non sa
   distinguere una richiesta che aspetta lui da una che aspetta il cliente;
2. l'assistenza **non ha una sua casa** nella console di amministrazione: i ticket vivono in coda a una pagina
   che parla d'altro, senza filtro per priorità e — soprattutto — **senza ordinamento per scadenza**, che è
   l'unica cosa che impedisce di mancare il termine di legge di un mese sui ticket privacy;
3. non si sa **da dove arriva** una richiesta (modulo dell'applicazione, evento di sistema, in futuro email);
4. quando il contenuto tocca **categorie particolari di dati** (articolo 9: salute, convinzioni, …) nulla lo
   porta in cima: la storia chiede promozione a priorità alta e segnalazione per attenzione umana;
5. le **email** del ticketing sono testo grezzo scritto a mano dentro il notificatore, fuori dal renderer
   unificato di piattaforma (UC 0085) e non tradotte; e chi apre un ticket **non riceve conferma**.

Obiettivo: portare il ticketing dallo stato di strumento interno della console privacy a **canale di assistenza
completo**, con le due superfici che la storia descrive (backoffice «I miei ticket», console «Ticket») e la
disciplina delle scadenze visibile a occhio.

## Scope

**Backend — `services/core`**

- Nuova migrazione Flyway: colonne `source` (`form` | `email` | `event`) e `flagged_for_review` sulla tabella
  `platform.support_ticket`, più l'indice che regge l'ordinamento della coda per scadenza. I ticket già esistenti
  nati dall'esportazione fallita diventano `source = 'event'`, gli altri `source = 'form'`.
- Nuovo stato **`waiting_user`** nel ciclo di vita. Regole: la risposta di chi assiste porta il ticket in
  `waiting_user`; la replica del cliente lo riporta in `open`; da `resolved` una replica del cliente lo riapre
  (comportamento già esistente, conservato); su `closed` non si scrive più.
- **Segnalazione categorie particolari**: all'apertura di un ticket e a ogni messaggio del cliente, un
  riconoscitore deterministico di parole-spia (italiano e inglese) alza la priorità a `high` e accende
  `flagged_for_review`. Non viene registrata **nessuna inferenza sulla persona**: la colonna dice solo «questo
  ticket va guardato da un umano», per minimizzazione.
- **API di amministrazione dedicata** `/api/platform/v1/admin/tickets`: coda cross-account con filtri per tipo,
  stato e priorità, ordinata mettendo per prime le scadenze più vicine; dettaglio con filo; risposta; cambio di
  stato e priorità. Sostituisce gli stessi endpoint oggi annidati sotto `/admin/gdpr/tickets`, che vengono
  rimossi. L'aggregazione dei ticket privacy nella Console "Diritti GDPR" resta e punta alla nuova pagina.
- **Notifiche email attraverso il renderer unificato** di `services/commons` (UC 0085): conferma di apertura a
  chi apre il ticket, avviso di aggiornamento quando la piattaforma risponde o cambia stato, avviso alla casella
  di assistenza quando nasce un ticket o il cliente scrive. Testi nella sorgente unica `shared/email-templates`
  in italiano e inglese, lingua scelta secondo quella dell'utente. L'invio resta **best-effort**: un guasto della
  posta non fa mai fallire l'operazione sul ticket.

**Frontend — console di amministrazione (`frontend/apps/admin`)**

- Nuova sezione «Ticket» con voce di menu propria: coda cross-account con conto, tipo, priorità, stato, scadenza;
  filtri per tipo/stato/priorità; **evidenza visiva della scadenza** dei ticket privacy (in ritardo / in
  scadenza) e del contrassegno «da rivedere».
- Dettaglio del ticket spostato sotto la nuova sezione, con lo stato `waiting_user` fra quelli selezionabili e la
  conferma esplicita richiesta per portare un ticket a `closed`.
- La pagina Diritti GDPR perde il blocco «Ticket» (ora ha una casa propria) e mantiene l'aggregazione delle
  richieste, coi collegamenti aggiornati.

**Frontend — backoffice (`frontend/apps/backoffice`)**

- Stato `waiting_user` mostrato con etichetta propria e con l'indicazione che la palla è al cliente.
- Nota esplicita che in questa versione **non si possono allegare file**, accanto alla casella del messaggio.
- Avviso della scadenza di legge sui ticket privacy nel dettaglio.

**Test**

- Backend: transizioni di stato (compreso `waiting_user`), riconoscitore delle categorie particolari, provenienza
  del ticket, filtri e ordinamento della coda di amministrazione, isolamento fra conti, resa delle email del
  ticketing nelle due lingue.
- Frontend: prove unitarie della nuova pagina «Ticket» e degli stati nel backoffice.
- End-to-end livello 2 (Playwright con risposte finte) su entrambe le applicazioni.
- Percorso di piattaforma **J-SUPPORT-TICKETING** (stack vero, browser vero, Mailpit): il cliente apre una
  richiesta, la piattaforma la trova in coda, risponde, il cliente vede la risposta e replica, la richiesta viene
  chiusa; le email vere vengono verificate in Mailpit.

**Documentazione**

- Registro di copertura end-to-end: 0075 esce dalle esenzioni ed entra fra gli use case con superficie;
  J-SUPPORT-TICKETING passa a coperto.
- Manifesto dati: nessuna voce nuova (le colonne aggiunte non sono dati personali); si annota la precisazione
  sulle categorie particolari nel contenuto libero.

## Fuori scope

- **Ricezione delle email in ingresso** su `privacy@`/`support@` (SES → funzione Lambda → ticket). È lavoro di
  infrastruttura cloud che dipende dall'uscita di SES dalla modalità di prova (UC 0018/0078) e dalla verifica del
  dominio: non è collaudabile in locale e ha effetti verso l'esterno. Il valore `email` della colonna `source`
  resta predisposto. Rimando scritto nei punti aperti di UC 0075.
- **Allegati** ai ticket: esclusi dalla storia stessa; resta il rimando già presente in UC 0075.
- **Assegnatario** e regole di presa in carico: con un solo operatore non servono (rimando già in UC 0075).
- **Rinomina del tipo `support` in `generic`**: la storia parla di «supporto generico», il codice e i dati usano
  già `support`. Rinominare significherebbe migrare dati e rompere il contratto per un sinonimo.
- Job di conservazione e purga (UC 0035) e Console "Diritti GDPR" (UC 0034) restano come sono, salvo i
  collegamenti alla nuova sezione.

## Criteri di accettazione

- [ ] Un ticket aperto dal backoffice nasce con `source = form`; l'auto-ticket dell'esportazione fallita nasce
      con `source = event`; entrambi restano invariati nel resto del comportamento.
- [ ] La risposta della piattaforma porta il ticket in `waiting_user`; la replica del cliente lo riporta in
      `open`; su un ticket `closed` la scrittura è rifiutata con un errore tipizzato.
- [ ] Un messaggio che contiene parole-spia di categorie particolari fa nascere il ticket con priorità `high` e
      `flagged_for_review` acceso; un messaggio ordinario no.
- [ ] La console di amministrazione ha una sezione «Ticket» raggiungibile dal menu, con coda cross-account,
      filtri tipo/stato/priorità e i ticket privacy scaduti o in scadenza evidenziati e ordinati per primi.
- [ ] Chi apre un ticket riceve un'email di conferma resa dal renderer unificato; chi riceve risposta dalla
      piattaforma riceve un'email di aggiornamento; entrambe esistono in italiano e in inglese.
- [ ] Un utente non legge mai i ticket di un altro conto; la coda di amministrazione li vede tutti solo col
      ruolo `platform-admin`.
- [ ] `./run-tests.sh` (suite completa) verde, percorso di piattaforma J-SUPPORT-TICKETING compreso.

## Invarianti appgrove toccati

- **Tenant dal solo token verificato**: le API utente continuano a prendere conto e utente dal `CallerContext`
  (claim del token), mai dal corpo della richiesta. La coda di amministrazione è l'eccezione documentata,
  ammessa solo per `platform-admin` e con accesso registrato nei log.
- **Filtro riga per riga**: le letture utente restano governate dal discriminator Hibernate; le scritture
  cross-tenant di sistema e amministrazione passano da `TicketStore`, che porta il conto in modo esplicito.
- **Modulo Terraform `microsaas_app`**: non toccato (nessuna app nuova).
- **Log strutturato**: ogni operazione sul ticket continua a registrare `ticket_id`, `tenant_id` e l'attore.

## Requisiti di test

- Prova esplicita di **isolamento**: un utente del conto B riceve 404 sul ticket del conto A.
- Prova che la **scadenza** dei ticket privacy sia calcolata alla creazione e usata come chiave di ordinamento
  della coda di amministrazione.
- Prova che il **guasto della posta non perde il ticket**: l'operazione va a buon fine anche se l'invio fallisce.
- Prova di **resa delle email** del ticketing in italiano e in inglese, senza segnaposto non risolti.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | Sì, limitato e interno: gli endpoint di amministrazione dei ticket si spostano da `/admin/gdpr/tickets` a `/admin/tickets` (nessun consumatore fuori dal monorepo) |
| Contratto cross-area | Sì — console di amministrazione ↔ API core (nuovo percorso, nuovi campi `source`, `flaggedForReview`, nuovo stato `waiting_user`) |
| Version bump | minor |
