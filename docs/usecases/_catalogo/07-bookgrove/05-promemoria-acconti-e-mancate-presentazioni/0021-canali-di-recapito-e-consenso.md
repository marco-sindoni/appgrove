# 0021 — Canali di recapito e consenso

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 05 — Promemoria, acconti e mancate presentazioni
**Storia**: `0021` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio sapere su quali canali posso scrivere a ciascun cliente, e poterlo dimostrare
> così da non mandare messaggi a chi non li vuole e da non trovarmi il canale bloccato dal fornitore.

**Contesto.** Prima del motore dei promemoria serve la regola che dice **a chi si può scrivere e dove**. Non è
burocrazia: il fornitore del canale di messaggistica consente di iniziare una conversazione **solo** dopo un
consenso esplicito della persona (§2.3, punto 4 della descrizione), e un canale usato male viene sospeso. Il
promemoria di un appuntamento che la persona ha preso non è pubblicità, ed è espressamente uno dei casi ammessi;
ma la distinzione va tenuta nel dato, non nelle intenzioni.

## 2. Requisiti funzionali

1. **RF-1** — Ogni cliente porta l'elenco dei canali utilizzabili — posta elettronica, messaggio breve, canale di
   messaggistica — ciascuno con il proprio stato: consentito, non consentito, mai chiesto.
2. **RF-2** — Il consenso raccolto porta con sé **quando** è stato dato, **da dove** (pagina pubblica, banco,
   importazione) e **con quale testo**: senza questi tre elementi non è dimostrabile.
3. **RF-3** — La pagina pubblica raccoglie il consenso al momento della prenotazione, con una frase chiara e
   separata dall'accettazione della politica di disdetta.
4. **RF-4** — Il cliente può revocare il consenso a un canale da ogni messaggio ricevuto, e la revoca ha effetto
   immediato su tutti i messaggi futuri.
5. **RF-5** — Un cliente importato o creato dal banco senza consenso esplicito **non** è contattabile sui canali
   che lo richiedono: resta la posta elettronica per i messaggi legati all'esecuzione dell'appuntamento.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Consensi e revoche sono per `tenant_id`: il consenso dato a
  un'attività non vale per un'altra, nemmeno se il contatto è lo stesso. È un punto in cui l'errore sarebbe grave.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|PUT /api/prenotazioni/v1/clienti/{id}/canali` e la
  revoca pubblica raggiungibile senza autenticazione con un gettone di ambito singolo, sullo stesso modello della
  storia `0018`; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V13__canali_consenso.sql`: tabella `consenso_canale` con `tenant_id`,
  UUID versione 7, colonne di controllo, canale, stato, momento, origine e **testo mostrato** al momento della
  raccolta — il testo si conserva, perché dimostrare un consenso significa poter dire cosa la persona ha letto.
- **RT-4 — Modulo frontend (§3, §5).** I canali si vedono e si modificano dalla scheda cliente, con lo stato di
  ciascuno leggibile a colpo d'occhio; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I testi di consenso e revoca in `en, it, fr, es, de`, e si conserva la versione
  effettivamente mostrata.
- **RT-6 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese: lo stato dei canali e la prova
  del consenso, con finalità «dimostrare la liceità dell'invio»; campi annotati `@PersonalData`; tabella in
  `exportData` e `purgeData`. La base giuridica del **promemoria** resta l'esecuzione del contratto: il consenso
  qui riguarda il **canale**, non la finalità, e la differenza va scritta chiaramente nel manifesto.
- **RT-7 — Registrazione eventi (§14).** `consenso registrato`, `consenso revocato` con `tenant_id`, `app_id`,
  `user_id` quando c'è, canale e correlazione — **mai il contatto**.

## 4. Criteri di accettazione

**CA-1 — Consenso dalla pagina pubblica**
- **Dato** una prenotazione fatta dal pubblico con la casella del canale di messaggistica spuntata
- **Quando** si apre la scheda cliente
- **Allora** il canale risulta consentito, con momento, origine e testo mostrato

**CA-2 — Nessun consenso, nessun invio**
- **Dato** un cliente creato dal banco senza consenso · **Quando** il motore dei promemoria cerca un canale
- **Allora** usa la posta elettronica e non il canale di messaggistica, e lo dice nell'esito

**CA-3 — Revoca immediata**
- **Dato** un cliente che revoca dal messaggio ricevuto · **Quando** è programmato un promemoria su quel canale
- **Allora** il promemoria non parte su quel canale

**CA-4 — Il consenso non attraversa gli account**
- **Dato** lo stesso numero di telefono presente in due account, con consenso solo nel primo
- **Quando** il secondo prova a usare il canale · **Allora** non può

**CA-5 — Prova del consenso**
- **Dato** un consenso registrato · **Quando** lo si esamina · **Allora** si vede quando, da dove e con quale
  testo esatto è stato dato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`);
- [ ] prove di **unità** sulla scelta del canale utilizzabile e di **integrazione** sulla revoca pubblica;
- [ ] prova di **isolamento fra account** sui consensi;
- [ ] **prova end-to-end**: *rimando* — il consenso si vede attraverso la prenotazione pubblica, coperta dalla
      storia `0034`, con il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** dei testi di consenso in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con la distinzione fra base giuridica della
      finalità e consenso al canale;
- [ ] **registro delle decisioni** compilato: conservazione del testo mostrato e perimetro del consenso;
- [ ] avvio locale invariato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0011` | il consenso si appende a un cliente |
| storia `0017` | la pagina pubblica è dove il consenso si raccoglie nel caso più frequente |

## 7. Fuori ambito

- l'invio vero dei messaggi: storie `0022` e `0023`;
- il consenso a comunicazioni promozionali: **fuori dal perimetro dell'app**, che manda solo messaggi legati a un
  appuntamento esistente; le campagne sono dell'applicazione 16.

## 8. Punti aperti

Nessuno.
