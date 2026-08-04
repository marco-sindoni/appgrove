# 0020 — Lista d'attesa

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 04 — Prenotazione self-service del cliente finale
**Storia**: `0020` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0015`, `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio che quando qualcuno disdice all'ultimo momento il posto venga offerto subito a chi lo stava aspettando
> così da non perdere l'ora e da non dover telefonare a cinque persone sperando che una risponda.

**Contesto.** È il differenziatore vero rispetto a un calendario condiviso. Registrare una disdetta è facile e lo
fa chiunque; **rivendere** l'ora liberata è la funzione che vale i soldi del canone, ed è quella che l'analisi di
mercato indica come richiesta ricorrente (§2.5 della descrizione). È anche una funzione delicata, perché manda
messaggi a persone che non hanno chiesto nulla in quel momento: va fatta con misura, o diventa molestia.

## 2. Requisiti funzionali

1. **RF-1** — Quando non c'è nessuno spazio adatto, la pagina pubblica propone di **mettersi in lista d'attesa**
   indicando servizio e periodo accettabile (per esempio «questa settimana, pomeriggi»).
2. **RF-2** — Il personale può mettere qualcuno in lista d'attesa anche dal banco.
3. **RF-3** — Quando uno spazio si libera — per disdetta, spostamento o apertura straordinaria — le persone in
   lista d'attesa compatibili vengono individuate in ordine di iscrizione.
4. **RF-4** — L'offerta del posto **non è automatica per impostazione predefinita**: l'attività la vede proposta e
   la conferma. Chi vuole l'invio automatico lo attiva, sapendo che manda messaggi senza revisione.
5. **RF-5** — L'offerta ha una **scadenza breve**: chi non risponde entro il tempo previsto lascia il posto al
   successivo, e il posto resta prenotabile normalmente nel frattempo.
6. **RF-6** — Chi accetta l'offerta ottiene una prenotazione confermata; una voce di lista d'attesa soddisfatta o
   scaduta si chiude e non produce altri messaggi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura della `lista_attesa` filtra per `tenant_id`
  preso dal token verificato; sulla superficie pubblica il `tenant_id` arriva dall'identificativo di sede
  (storia `0016`) o dal gettone di capacità (storia `0018`), mai dalla richiesta.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/prenotazioni/v1/lista-attesa`,
  `POST /api/prenotazioni/v1/lista-attesa/{id}/offerta` e la corrispondente accettazione pubblica; errori in
  `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V12__lista_attesa.sql`: tabella `lista_attesa` con `tenant_id`, UUID
  versione 7, colonne di controllo, cancellazione logica, periodo accettabile e stato; tabella delle offerte con
  scadenza.
- **RT-4 — Correttezza sotto concorrenza.** L'accettazione di un'offerta passa dallo stesso vincolo di non
  sovrapposizione della storia `0014`: se nel frattempo qualcun altro ha preso lo spazio, l'accettazione fallisce
  con un messaggio comprensibile e la voce di lista d'attesa **resta viva**.
- **RT-5 — Modulo frontend (§3, §5).** In agenda, quando uno spazio si libera, compare la proposta con le persone
  compatibili e un'azione di conferma esplicita; solo token del sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Interfaccia e testi dell'offerta in `en, it, fr, es, de`; l'offerta al cliente
  segue la **sua** lingua preferita.
- **RT-7 — Varchi e quota (§6, §7).** Nessun consumo di quota: la metrica è a giacenza sulle risorse. Con
  abbonamento non attivo la funzione risponde `402`.
- **RT-8 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese per la lista d'attesa, con
  durata proposta «fino alla scadenza della richiesta, poi tre mesi»; campi annotati `@PersonalData`; tabelle in
  `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** `iscrizione in lista d'attesa`, `offerta inviata`, `offerta accettata`,
  `offerta scaduta` con `tenant_id`, `app_id`, `user_id` quando c'è, e correlazione — mai il contatto.
- **RT-10 — Esposizione conversazionale (§12).** Base dello strumento `offri_posto_da_lista_attesa`, dichiarato
  nella storia `0032`: **scrittura irreversibile**, perché manda messaggi a persone fuori dall'azienda, quindi
  bozza e conferma umana **obbligatorie**.

## 4. Criteri di accettazione

**CA-1 — Iscrizione dalla pagina pubblica**
- **Dato** una settimana senza spazi liberi per un servizio · **Quando** un visitatore chiede di essere avvisato
- **Allora** compare nella lista d'attesa dell'attività con il periodo indicato

**CA-2 — Il posto liberato trova un candidato**
- **Dato** due persone in lista d'attesa compatibili · **Quando** una prenotazione viene disdetta · **Allora**
  l'attività vede la proposta con le due persone in ordine di iscrizione, e nessun messaggio è ancora partito

**CA-3 — Offerta e accettazione**
- **Dato** un'offerta confermata dall'attività · **Quando** la persona accetta entro la scadenza · **Allora**
  ottiene una prenotazione confermata e la voce di lista d'attesa si chiude

**CA-4 — Offerta scaduta**
- **Dato** un'offerta non accettata · **Quando** scade · **Allora** lo spazio resta prenotabile e la persona
  successiva può essere contattata

**CA-5 — Corsa sullo spazio offerto**
- **Dato** un'offerta viva e un altro cliente che prenota lo stesso spazio dalla pagina pubblica
- **Quando** la persona in lista d'attesa accetta dopo · **Allora** riceve un messaggio comprensibile, resta in
  lista, e non si crea nessuna sovrapposizione

**CA-6 — Isolamento fra account**
- **Dato** due account con liste d'attesa · **Quando** uno spazio si libera in uno · **Allora** solo le persone
  di quell'account vengono considerate

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`);
- [ ] prove di **unità** sull'ordinamento dei candidati e di **integrazione** sull'accettazione sotto concorrenza;
- [ ] prova di **isolamento fra account** su lista d'attesa e offerte;
- [ ] **prova end-to-end**: **coperta ora** — passo finale del percorso `[J-BOOKGROVE-PUB]` della storia `0034`,
      con il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la lista d'attesa;
- [ ] **registro delle decisioni** compilato: offerta non automatica per impostazione predefinita, e perché;
- [ ] contratto degli **strumenti conversazionali** predisposto per `offri_posto_da_lista_attesa`;
- [ ] avvio locale invariato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0015` | è la disdetta a liberare lo spazio |
| storia `0017` | l'iscrizione nasce dalla pagina pubblica |
| storia `0022` | serve il motore dei messaggi per recapitare l'offerta |

## 7. Fuori ambito

- l'offerta a più persone contemporaneamente con assegnazione a chi risponde prima: **deliberatamente rimandata**,
  perché promette lo stesso posto a più persone ed è un modo sicuro di far arrabbiare qualcuno. Se emergerà come
  richiesta, va progettata con cura.

## 8. Punti aperti

**Quante offerte prima di essere invadenti.** Una persona in lista d'attesa per un mese potrebbe ricevere
parecchi messaggi. Proposta: un tetto di offerte per iscrizione e la possibilità di uscire dalla lista con un
solo clic dal messaggio. Da confermare, ed è anche un tema di rispetto verso una persona che non è nostra cliente.
