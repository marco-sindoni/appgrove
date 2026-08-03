# 0025 — Trasmissione a un canale esterno

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 05 — Conformità e apertura verso l'esterno
**Storia**: `0025` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena emesso una fattura
> voglio mandarla al mio cliente, o al canale che la deve inoltrare, con un gesto e sapere se è arrivata
> così da chiudere il lavoro dentro l'app invece di scaricare un file, aprire la posta e allegarlo a mano.

**Contesto.** È il punto in cui BillGrove tocca il mondo esterno, e quindi il punto più delicato dopo la
numerazione. La regola di sicurezza del catalogo (§8) è netta: le azioni di scrittura con **effetti irreversibili**
— trasmettere un documento a un'autorità, mandarlo a un cliente — non si eseguono senza conferma umana esplicita.
Qui si costruisce l'**adattatore**: un contratto stabile verso l'esterno, con esito e stato di ritorno, che la
storia `0019` userà per i solleciti e che InvoiceGrove (1) userà per la trasmissione conforme.

## 2. Requisiti funzionali

1. **RF-1** — Da un documento emesso si può avviare una trasmissione verso un canale configurato: posta elettronica
   al cliente, oppure un canale esterno che riceve la forma canonica.
2. **RF-2** — Prima di trasmettere l'utente vede **che cosa** sta per essere mandato, **a chi** e **su quale
   canale**, e conferma esplicitamente.
3. **RF-3** — Ogni trasmissione lascia una traccia: canale, destinatario, data, esito, eventuale codice di errore.
4. **RF-4** — Una trasmissione fallita si può ripetere; una trasmissione riuscita **non** si ripete senza una
   seconda conferma esplicita, perché mandare due volte lo stesso documento è un problema.
5. **RF-5** — Il canale esterno è **sostituibile**: l'app parla con un contratto, non con un fornitore specifico.
6. **RF-6** — Il documento mostra sempre il proprio stato di trasmissione, distinto dallo stato di pagamento.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Configurazioni del canale, traccia delle trasmissioni e documenti
  filtrano per `tenant_id` preso dal token verificato. La configurazione di un canale di un account non è mai
  leggibile da un altro, credenziali comprese.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/billing/v1/documents/{id}/transmit` con conferma
  esplicita nel corpo; `GET /api/billing/v1/documents/{id}/transmissions`; errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit. La chiamata verso l'esterno è **asincrona**: la rotta accetta
  e restituisce uno stato, non aspetta il fornitore.
- **RT-3 — Persistenza (§8).** Migrazione `V12__transmission.sql` sullo schema `app_billing`: tabelle
  `channel_config` e `transmission` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e
  cancellazione logica. Le credenziali del canale **non** stanno in chiaro nella base dati.
- **RT-4 — Modulo frontend (§3, §5).** Finestra di conferma che mostra destinatario, canale e anteprima; stato di
  trasmissione sulla scheda del documento e negli elenchi. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili, compresi i messaggi di errore del canale tradotti in
  linguaggio comprensibile, passano dallo spazio-nomi `billing` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6).** La trasmissione **non consuma** la metrica `documenti`: il documento ha già
  consumato all'emissione. Con abbonamento non attivo la trasmissione risponde `402`. Richiede ruolo `admin`: manda
  qualcosa fuori dall'azienda.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `invia_documento(id, destinatario) → esito`, marcato **scrittura irreversibile**: conferma umana
  **obbligatoria**, mai eseguibile direttamente dall'assistente. È il caso esemplare della regola del catalogo:
  l'intelligenza artificiale prepara, la persona approva. Dipendenza dichiarata: UC 0061-0063.
- **RT-8 — Dati personali (§10).** **La storia introduce il primo responsabile esterno del trattamento dell'app.**
  Il fornitore del canale (servizio di posta elettronica o canale di trasmissione) riceve dati personali per nostro
  conto: va dichiarato nell'elenco dei fornitori e nell'informativa **prima** di attivarlo, con verifica che i dati
  restino in territorio europeo. Voci nuove nel manifesto in italiano e inglese per
  `transmission.destinatario`; `transmission` va aggiunta a `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `trasmissione avviata`, `trasmissione riuscita`, `trasmissione
  fallita` (con codice di errore) e `ripetizione confermata` sono registrati con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, **senza l'indirizzo del destinatario**.

## 4. Criteri di accettazione

**CA-1 — Trasmissione con conferma**
- **Dato** una fattura emessa e un canale configurato
- **Quando** l'utente avvia la trasmissione e conferma nella finestra che mostra destinatario e canale
- **Allora** la trasmissione parte, il documento mostra lo stato «in corso» e poi l'esito

**CA-2 — Nessuna trasmissione senza conferma**
- **Dato** una richiesta di trasmissione senza il consenso esplicito nel corpo
- **Quando** la si esegue · **Allora** la risposta è `400` e nulla viene mandato

**CA-3 — Fallimento e ripetizione**
- **Dato** una trasmissione fallita con codice di errore
- **Quando** si chiede di ripeterla · **Allora** riparte e la traccia riporta entrambi i tentativi

**CA-4 — Doppio invio protetto**
- **Dato** una trasmissione già riuscita · **Quando** si chiede di rimandare senza seconda conferma
- **Allora** la risposta è `409` con l'indicazione che il documento risulta già trasmesso

**CA-5 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled` · **Quando** tenta una trasmissione
- **Allora** riceve `402`

**CA-6 — Isolamento fra account**
- **Dato** due account con canali configurati
- **Quando** un utente di `A` legge le trasmissioni, anche forzando l'identificativo di `B`
- **Allora** vede solo le proprie, e nessuna credenziale di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla macchina di stato della trasmissione e di **integrazione** con il canale **simulato**
      — mai un fornitore vero nelle prove — con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su configurazioni e tracce;
- [ ] **prova end-to-end**: *coprire ora* — passo «invia la fattura, con conferma» del percorso `[J-BILLING]`,
      contro il canale simulato; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con il destinatario della trasmissione e **con il fornitore esterno
      dichiarato**;
- [ ] **registro delle decisioni** compilato, con annotata la scelta dell'adattatore sostituibile;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `invia_documento`, con conferma obbligatoria;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0024` | Si trasmette la forma canonica, o la stampa; entrambe devono esistere |
| Fornitore esterno da scegliere e dichiarare | Nessuna trasmissione vera parte prima che il fornitore sia stato valutato e inserito nell'elenco dei responsabili esterni |

## 7. Fuori ambito

- la conformità della trasmissione a una specifica giurisdizione, l'accettazione da parte di un'autorità fiscale e
  la gestione degli esiti normativi: sono di InvoiceGrove (1);
- la conservazione a norma: storia `0026`;
- la ricezione dei documenti passivi: fuori ambito, BillGrove emette e non riceve.

## 8. Punti aperti

**Fermata di escalation.** Questa storia produce **effetti verso l'esterno** e introduce il primo responsabile
esterno del trattamento. Due decisioni non spettano a un agente: quale fornitore del canale usare (con verifica che
i dati restino in territorio europeo) e se la conferma per singolo documento basti o serva un consenso preventivo di
account. Le chiude lo sviluppatore, prima che la storia venga implementata.
