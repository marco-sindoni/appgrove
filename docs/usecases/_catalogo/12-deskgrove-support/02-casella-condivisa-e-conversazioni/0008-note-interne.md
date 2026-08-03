# 0008 — Note interne

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 02 — Casella condivisa e conversazioni
**Storia**: `0008` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde ai clienti insieme ad altri colleghi
> voglio poter scrivere dentro la richiesta un appunto che vedono solo i miei colleghi
> così da passare il contesto a chi prenderà in mano la conversazione dopo di me, senza mandare quell'appunto al
> cliente per sbaglio.

**Contesto.** Oggi quell'appunto viaggia su un messaggio istantaneo o a voce, e sparisce: la persona che riprende la
richiesta lunedì non sa che venerdì il cliente aveva già telefonato e che il rimborso era stato promesso. È il
secondo dei bisogni rilevati nel segmento micro (§2.5 della descrizione: «sapere chi risponde a cosa»), e costa
poco costruirlo perché il filo esiste già dalla storia `0007`: la nota è un messaggio con un verso diverso. Costa
invece molto sbagliarlo, e in un modo solo: **una nota interna che finisce al cliente**. Per questo l'esclusione
non è una regola dell'interfaccia ma del livello dati, e per questo la distinzione visiva deve reggere anche a
occhio distratto.

## 2. Requisiti funzionali

1. **RF-1** — Dalla stessa schermata di dettaglio l'operatore sceglie **esplicitamente** fra «Rispondi al cliente» e
   «Nota interna» prima di scrivere; la scelta è visibile mentre si scrive, non solo al momento del salvataggio, e
   non esiste un valore predefinito ambiguo.
2. **RF-2** — La nota si registra come messaggio del filo con verso «interno», autore l'utente autenticato e data
   del momento; è visibile a **tutti** gli operatori dell'account che vedono la richiesta.
3. **RF-3** — La nota è distinguibile **a colpo d'occhio** dai messaggi scambiati col cliente: fondo dedicato,
   etichetta testuale tradotta e allineamento diverso. La distinzione regge anche senza colore — chi non distingue
   le tinte deve leggere l'etichetta e capire.
4. **RF-4** — La nota **non tocca gli orologi né gli stati**: non valorizza la data di prima risposta e non provoca
   alcun cambio di stato della richiesta, né ora né quando la macchina a stati arriverà (storia `0009`).
5. **RF-5** — Le rappresentazioni destinate **all'esterno** escludono i messaggi con verso «interno» **per
   costruzione, nel livello dati**: esiste una sola funzione di lettura «filo visibile al richiedente», che è quella
   che useranno la spedizione (storia `0015`), il portale del richiedente (storia `0032`) e ogni canale futuro.
   Nessuna schermata e nessun canale può interrogare la tabella per conto proprio.
6. **RF-6** — Il filo si può filtrare fra «tutto» e «solo i messaggi scambiati col cliente», e il filtro scelto è
   evidente: chi guarda deve sapere se sta vedendo tutto o una parte.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lettura e la scrittura delle note filtrano per `tenant_id` preso dal
  token verificato; la nota di una richiesta di un altro account non è né leggibile né scrivibile e risponde `404`.
  Prova di isolamento fra due account sulle note, distinta da quella sui messaggi.
- **RT-2 — Interfaccia di programmazione (§2).** La rotta è la stessa della storia `0007`,
  `POST /api/helpdesk/v1/tickets/{id}/messages`, con il verso fra i campi del corpo validati; il verso «interno» non
  è ammesso su rotte destinate a canali esterni. `GET /api/helpdesk/v1/tickets/{id}` accetta il parametro di
  visibilità del filo. Errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova sullo schema `app_helpdesk`: la nota è una riga di
  `ticket_message`. Se il valore `internal` del verso non è già fra quelli ammessi dal vincolo di controllo
  introdotto dalla storia `0002`, lo aggiunge la migrazione `V5__message_direction_internal.sql`. Colonne di
  controllo e cancellazione logica come per ogni altro messaggio; nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Nella schermata di dettaglio del modulo `helpdesk`: selettore fra risposta e
  nota, stile visivo dedicato costruito **solo** con i token del sistema di design (nessun colore scritto a mano),
  funzionante in tema chiaro e scuro, e filtro del filo. Controllo automatico di accessibilità sulla schermata,
  compresa la verifica che la nota resti riconoscibile senza affidarsi al solo colore.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — etichetta «Nota interna», selettore, filtro del filo,
  avviso «questa nota non viene inviata al cliente» — passano dallo spazio-nomi `helpdesk` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Scrivere una nota **non consuma quota**: la metrica unica dell'app è `agents`
  (posti operatore, natura `stock`), consumata dalla storia `0018`. Restano i varchi a monte: `401`, `402` con
  abbonamento non attivo, `403` per ruolo insufficiente; il ruolo `member` può scrivere note. La storia non fissa
  prezzi: consuma il tetto pubblicato dall'abilitazione.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. Si precisa però il contratto di
  `leggi_richiesta` dichiarato dalla storia `0007`: restituisce il filo **note interne comprese** (§7 della
  descrizione), perché l'assistente lavora per l'operatore e non per il richiedente. Al contrario,
  `prepara_risposta` non deve mai copiare il testo di una nota interna dentro la bozza destinata al cliente: è un
  vincolo dichiarato nel contratto dello strumento. Il server conversazionale è di piattaforma e non ancora
  implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun campo nuovo di persone e nessuna tabella nuova: la nota è una riga di
  `ticket_message`, già presente in `exportData` e `purgeData` di `HelpdeskDataContract`. La voce `message.body` del
  manifesto `docs/compliance/manifests/helpdesk.yaml` va però **estesa in italiano e inglese** per dichiarare che
  comprende **annotazioni interne dell'operatore sul richiedente**: sono a tutti gli effetti dati personali di quella
  persona, e il fatto che il richiedente non le veda nell'interfaccia non le sottrae al suo diritto di accesso.
  Anche il testo delle note attraversa il riconoscitore delle categorie particolari con il solo contrassegno
  booleano, senza registrare quale categoria. Accanto alla casella della nota compare l'avviso, tradotto, che la
  nota è comunque un dato del cliente e non un posto dove scrivere giudizi.
- **RT-9 — Registrazione eventi (§14).** L'evento `nota interna registrata` è scritto con `tenant_id`, `app_id`,
  `user_id`, identificativo della richiesta e identificativo di correlazione, **senza** il corpo della nota e senza
  alcun dato personale.

## 4. Criteri di accettazione

**CA-1 — Nota registrata e riconoscibile**
- **Dato** una richiesta con un messaggio in ingresso e un messaggio in uscita
- **Quando** un operatore sceglie «Nota interna» e la registra
- **Allora** la nota compare in coda al filo con verso «interno», etichetta testuale tradotta e stile dedicato, ed è
  visibile a un secondo operatore dello stesso account

**CA-2 — La nota non tocca orologi né stati**
- **Dato** una richiesta senza data di prima risposta
- **Quando** l'operatore registra **solo** una nota interna
- **Allora** la data di prima risposta resta vuota e lo stato della richiesta è invariato

**CA-3 — La nota non esce, e non esce dal livello dati**
- **Dato** una richiesta con due messaggi scambiati col cliente e una nota interna
- **Quando** si chiede il «filo visibile al richiedente» — la stessa funzione che useranno spedizione e portale
- **Allora** vengono restituiti due messaggi e la nota **non** è fra questi, e lo stesso vale per il filtro «solo i
  messaggi scambiati col cliente» nell'interfaccia

**CA-4 — Distinzione senza colore**
- **Dato** la schermata di dettaglio con una nota interna nel filo
- **Quando** la si esamina con il controllo automatico di accessibilità e in tema chiaro e scuro
- **Allora** la nota resta identificabile dall'etichetta testuale, senza dipendere dal solo colore, e i contrasti
  rispettano la soglia richiesta

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie richieste e le proprie note
- **Quando** un utente di `A` tenta di leggere o scrivere una nota su una richiesta di `B`, anche forzando il
  `tenant_id` nel corpo
- **Allora** riceve `404` e nulla viene scritto nel filo di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla funzione «filo visibile al richiedente» e di **integrazione** sulla registrazione
      della nota, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulle note, distinta da quella sui messaggi;
- [ ] **prova end-to-end**: *rimandare* — la prova che il richiedente **non veda** la nota richiede una superficie
      esterna, che oggi non esiste: la spedizione è la storia `0015` e il portale del richiedente la storia `0032`.
      Qui si copre con prove di integrazione sul livello dati e con prove di componente sul filtro del filo, e si
      apre nel registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) una voce
      `da-coprire` con motivo «superficie esterna non ancora esistente» e storia proprietaria `0037`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese: la voce `message.body` estesa alle annotazioni interne
      dell'operatore sul richiedente;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di escludere le note nel **livello dati** e non
      nell'interfaccia;
- [ ] contratto degli **strumenti conversazionali** precisato: `leggi_richiesta` comprende le note,
      `prepara_risposta` non le copia mai nella bozza destinata al cliente;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | Servono il filo, la registrazione di un messaggio e la data di prima risposta: la nota è un messaggio con un verso diverso e la regola «non tocca l'orologio» ha senso solo se l'orologio esiste |
| epica di piattaforma non implementata (UC 0061-0063) | Il livello conversazionale non esiste: qui si precisa solo il contratto degli strumenti già dichiarati |

## 7. Fuori ambito

- la **menzione di un collega** dentro la nota, con avviso a chi è nominato: non è di questa storia e nasce insieme
  all'assegnazione, storia `0020`;
- la **spedizione** dei messaggi in uscita, che consumerà la funzione «filo visibile al richiedente» costruita qui:
  storia `0015`;
- il **portale del richiedente**, secondo consumatore della stessa funzione: storia `0032`;
- il **cambio di stato** provocato dai messaggi: storia `0009`, che dovrà rispettare la regola «la nota non cambia
  lo stato» scritta qui;
- la **presenza delle note nell'esportazione per singolo richiedente**: il meccanismo di esportazione è la storia
  `0036`; qui si dichiara soltanto che le note sono dati personali del richiedente (vedi punti aperti).

## 8. Punti aperti

- **Una nota interna deve comparire nell'esportazione dei dati richiesta dal cliente finale?** La proposta di questa
  storia è **sì**: una annotazione su una persona identificata è un dato personale di quella persona, e il diritto
  di accesso non si ferma davanti a una scelta di interfaccia. Ma è una decisione con conseguenze concrete sul modo
  in cui gli operatori useranno lo strumento, e appartiene alla storia `0036`. Chiudono lo **sviluppatore** e la
  **revisione legale pre-go-live** ([docs/_REVISIONE-LEGALE.md](../../../../_REVISIONE-LEGALE.md)), punto 2 del §11
  della descrizione.
- **Serve un ruolo che veda la richiesta ma non le note?** Oggi no: gli operatori sono da uno a dieci e la
  separazione aggiungerebbe complessità al segmento che l'ha rifiutata (§2.5). Se dovesse servire, il posto è la
  storia `0018` (operatori e posti). Chiude lo **sviluppatore** come decisione di direzione di prodotto.
