# 0020 — Presa in carico e assegnazione

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 04 — Organizzazione del lavoro
**Storia**: `0020` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`, `0010`, `0018`, `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde ai clienti
> voglio dire «questa la prendo io» e poter passare una richiesta a un collega
> così da non rispondere in due alla stessa persona e da non lasciare a nessuno il messaggio che aspetta da tre giorni.

**Contesto.** È il problema numero uno del segmento, prima ancora dei tempi di risposta: le fonti consultate lo
mettono in cima all'elenco di ciò che i clienti micro chiedono — «sapere chi risponde a cosa», per evitare la
doppia risposta e il messaggio dimenticato ([application-description.md](../application-description.md) §2.5).
Fino a qui la richiesta ha una coda ma non un responsabile: chiunque può rispondere, quindi nessuno è tenuto a
farlo. Questa storia introduce l'assegnatario e lo storico di chi ha avuto la richiesta. **Manuale**, per scelta:
nessuna distribuzione automatica a rotazione, che è la funzione che presuppone turni, disponibilità e carichi
misurati — cose che una squadra di tre persone non ha e non vuole mantenere.

## 2. Requisiti funzionali

1. **RF-1** — Un operatore può prendere in carico una richiesta non assegnata («la prendo io») dal dettaglio e
   dall'elenco: diventa l'assegnatario e la richiesta passa da «aperta» a «in lavorazione» secondo la macchina a
   stati della storia `0009`.
2. **RF-2** — Un operatore può passare una richiesta a un altro operatore, con una nota facoltativa che spiega il
   perché; la nota compare nello storico e non è visibile al richiedente.
3. **RF-3** — L'assegnazione si può togliere: la richiesta torna non assegnata e resta nella sua coda, senza
   cambiare stato.
4. **RF-4** — Chi riceve una richiesta viene **avvisato**: notifica dentro l'applicazione e messaggio di posta
   secondo le proprie preferenze. L'avviso porta il **numero** della richiesta e il collegamento alla pagina; non
   contiene l'oggetto, il testo dei messaggi né alcun dato del richiedente.
5. **RF-5** — Il dettaglio della richiesta mostra lo **storico delle assegnazioni** — chi, quando, da parte di chi,
   con la nota — in ordine cronologico; le voci dello storico non si modificano e non si cancellano.
6. **RF-6** — Solo un operatore **attivo** (posto assegnato, non sospeso — storia `0018`) può essere assegnatario;
   il tentativo di assegnare a un operatore sospeso, o a un utente dell'account senza posto, è rifiutato con `422`
   e la richiesta non cambia.
7. **RF-7** — Prendere in carico una richiesta che qualcun altro ha preso nel frattempo è rifiutato con `409` e un
   messaggio che dice **chi** la ha adesso: due persone non si assegnano la stessa richiesta senza accorgersene.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura dell'assegnazione e dello storico filtra per
  `tenant_id` preso dal token verificato; l'operatore destinatario si risolve **dentro** l'account del token, mai
  da un identificativo che arrivi dal corpo senza verifica di appartenenza.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/helpdesk/v1/tickets/{id}/assignment` (prendere in
  carico o passare), `DELETE /api/helpdesk/v1/tickets/{id}/assignment` (togliere) e
  `GET /api/helpdesk/v1/tickets/{id}/assignments` (storico); corpo validato; errori in
  `application/problem+json`, compreso il `409` di conflitto con l'indicazione dell'assegnatario attuale;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__ticket_assignment.sql` sullo schema `app_helpdesk`: colonna
  `assignee_id` su `ticket` con indice a partire da `tenant_id`, e tabella `ticket_assignment` (richiesta, da chi,
  a chi, nota, momento) con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione
  logica. Il conflitto di RF-7 si risolve con un aggiornamento condizionato sullo stato di assegnazione atteso,
  non con una lettura seguita da una scrittura. **Nessuna chiave esterna verso altri schemi**.
- **RT-4 — Modulo frontend (§3, §5).** Azione «la prendo io» sul dettaglio e sulla riga dell'elenco, selettore
  dell'assegnatario limitato agli operatori attivi della coda, pannello dello storico. Dati letti con il client
  generato; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `helpdesk` e sono presenti in
  `en, it, fr, es, de`, compresi il messaggio di conflitto, quello di rifiuto per operatore sospeso e il testo
  della notifica di assegnazione.
- **RT-6 — Varchi e quota (§6, §7).** Assegnare **non** consuma la metrica `agents`: la quota si consuma quando si
  dà un posto (storia `0018`), non quando si sposta il lavoro. La catena dei varchi resta quella di piattaforma
  (`401 → 403 → 402 → 403 → 429`): con abbonamento `canceled` o `paused` le rotte rispondono `402`, con `past_due`
  restano accessibili.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato: `assegna_richiesta(numero, operatore) →
  esito`, marcato **scrittura reversibile e interna**, quindi **senza** conferma umana obbligatoria — come previsto
  dal §7 della descrizione dell'applicazione. Vincoli che il contratto porta con sé: rispetta RF-6 (operatore
  attivo) e RF-7 (conflitto → errore, mai sovrascrittura silenziosa), e l'avviso a chi riceve parte comunque. Il
  contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** La storia introduce **un** campo che può riguardare una persona: la **nota di
  passaggio** (`ticket_assignment.note`) è testo libero scritto da un operatore e può contenere il nome del
  richiedente o dettagli della sua vicenda. Voce nuova nel manifesto
  `docs/compliance/manifests/helpdesk.yaml` in **italiano e inglese**, campo annotato `@PersonalData`, tabella
  `ticket_assignment` aggiunta **sia** a `exportData` **sia** a `purgeData` del contratto `HelpdeskDataContract`.
  Gli identificativi degli operatori sono già dichiarati dalla storia `0018`. Su DeskGrove appgrove è
  **responsabile del trattamento** per conto dell'azienda cliente, non titolare: finalità, base giuridica e
  conservazione della voce ricalcano quelle del filo dei messaggi.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «richiesta presa in carico», «richiesta passata», «assegnazione
  rimossa», «assegnazione respinta per operatore non attivo» e «conflitto di presa in carico» sono registrati con
  `tenant_id`, `app_id`, `user_id`, identificativo della richiesta e identificativo di correlazione, **senza** la
  nota, l'oggetto o qualunque altro dato personale.

## 4. Criteri di accettazione

**CA-1 — La prendo io**
- **Dato** un operatore attivo e una richiesta «aperta» non assegnata nella sua coda
- **Quando** preme «la prendo io»
- **Allora** ne diventa l'assegnatario, la richiesta passa a «in lavorazione» e lo storico registra la presa in
  carico con il momento

**CA-2 — La passo a te, e chi riceve lo sa**
- **Dato** una richiesta assegnata all'operatore `Anna`, e l'operatore attivo `Bruno`
- **Quando** `Anna` la passa a `Bruno` con la nota «tu conosci questo cliente»
- **Allora** `Bruno` è il nuovo assegnatario, riceve la notifica con **numero e collegamento** e **senza** oggetto
  né testo del messaggio, e la nota compare nello storico ma non è visibile al richiedente

**CA-3 — Conflitto di presa in carico**
- **Dato** due operatori che aprono la stessa richiesta non assegnata
- **Quando** premono entrambi «la prendo io» a distanza di un istante
- **Allora** il primo diventa assegnatario e il secondo riceve `409` con il nome di chi la ha adesso; l'assegnazione
  del primo **non** viene sovrascritta

**CA-4 — Destinatario non ammesso**
- **Dato** un operatore sospeso e un membro dell'account senza posto operatore
- **Quando** si prova ad assegnare loro una richiesta
- **Allora** entrambi i tentativi ricevono `422` con la spiegazione, e la richiesta resta com'era

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri operatori e le proprie richieste
- **Quando** un operatore di `A` prova ad assegnare una propria richiesta a un operatore di `B` forzandone
  l'identificativo nel corpo
- **Allora** la richiesta è rifiutata come se l'operatore non esistesse, e nessuna richiesta di `B` compare mai
  nell'elenco di `A`

**CA-6 — Lo storico sopravvive alla sospensione**
- **Dato** una richiesta passata da `Anna` a `Bruno`, e `Anna` successivamente sospesa
- **Quando** si apre lo storico delle assegnazioni
- **Allora** il passaggio da `Anna` è ancora leggibile con il suo nome visibile: lo storico non si riscrive

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulle regole di ammissibilità dell'assegnatario e di **integrazione** sulla risorsa di
      assegnazione, compresa una prova di **concorrenza** che dimostri il `409` di RF-7, con database effimero e
      migrazioni Flyway vere;
- [ ] prova di **isolamento fra account** su assegnazione e storico;
- [ ] **prova end-to-end**: **rimando** alla storia `0037`, proprietaria del percorso `[J-HELPDESK]`, dove il passo
      «presa in carico» entrerà nel percorso completo; qui la copertura resta alle prove d'integrazione. Voce
      `da-coprire` nel **registro di copertura**
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con motivo e storia proprietaria;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), testo della notifica compreso;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `assignment.note`, campo annotato `@PersonalData`,
      tabella `ticket_assignment` presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotato perché non c'è
      distribuzione automatica e perché la notifica non porta il contenuto;
- [ ] contratto degli **strumenti conversazionali**: `assegna_richiesta` dichiarato, con i suoi vincoli;
- [ ] controllo automatico di **accessibilità** verde sul dettaglio della richiesta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove (macchina a stati della
      storia `0009`: la presa in carico è la transizione «aperta → in lavorazione»).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0009` — ciclo di vita degli stati | La presa in carico è una transizione della macchina a stati e non può contraddirla |
| Storia `0010` — elenco, ricerca e viste | Serve la riga dell'elenco da cui si prende in carico e il filtro «assegnate a me» |
| Storia `0018` — operatori e posti | Solo un operatore attivo può essere assegnatario |
| Storia `0019` — code di lavoro | Il selettore dell'assegnatario si limita agli operatori che presidiano la coda |
| Generatore unificato dei messaggi di posta di `services/commons` (UC 0085) | L'avviso di assegnazione riusa il meccanismo esistente e i modelli in `shared/email-templates`, non ne costruisce un secondo |

## 7. Fuori ambito

- **La distribuzione automatica a rotazione** (assegnazione a giro, per carico o per disponibilità): non si fa. Non
  è un rimando tecnico ma la stessa scelta di sottrazione della storia `0019` — presuppone turni e carichi misurati
  che il segmento non ha. Se emergesse, è una decisione di direzione di prodotto (§8).
- **La coda e il presidio**: li fa la storia `0019`. Qui l'assegnatario è una persona, non un gruppo.
- **Le preferenze di notifica dell'operatore** (quali avvisi ricevere, su quale canale): appartengono alla storia
  `0026`, che possiede gli avvisi; qui si rispettano le preferenze che esistono e, finché non esistono, l'avviso in
  applicazione è sempre attivo e quello di posta segue il valore predefinito.
- **L'inoltro con aumento di priorità** (`inoltra_richiesta`): lo fa la storia `0021`, che possiede la priorità.
- **La riassegnazione automatica quando un operatore viene sospeso**: le sue richieste restano assegnate a lui e
  compaiono nella vista «senza presidio». Ridistribuirle automaticamente è la stessa famiglia di automatismi
  dichiarata fuori ambito qui sopra.

## 8. Punti aperti

- **Che fine fanno le richieste di un operatore sospeso?** La proposta è lasciarle assegnate e renderle visibili in
  una vista dedicata, così che una persona decida. L'alternativa — riportarle automaticamente in coda — cambia la
  responsabilità senza che nessuno se ne accorga. È una scelta di direzione di prodotto: la chiude lo sviluppatore,
  d'accordo con la storia `0010` che possiede le viste.
- **Notifica di posta a ogni assegnazione: aiuta o diventa rumore?** Con dieci passaggi al giorno l'avviso perde
  valore e l'operatore lo filtra. La proposta è renderlo disattivabile fin dall'inizio, ma il valore predefinito è
  una scelta di prodotto — la chiude lo sviluppatore insieme alla storia `0026`.
