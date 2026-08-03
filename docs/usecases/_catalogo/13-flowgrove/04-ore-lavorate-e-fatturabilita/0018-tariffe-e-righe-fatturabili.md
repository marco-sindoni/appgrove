# 0018 — Tariffe e righe fatturabili

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 04 — Ore lavorate e fatturabilità
**Storia**: `0018` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha concordato 45 € l'ora con questo cliente e 60 con quell'altro
> voglio che le ore dichiarate diventino un importo da sole
> così da non rifare i conti a mano ogni volta che devo fatturare.

**Contesto.** Una riga di ore senza tariffa è una statistica; con la tariffa diventa denaro, ed è il momento in
cui FlowGrove smette di essere una lavagna. La distinzione **fatturabile / non fatturabile** è altrettanto
importante e viene sempre sottovalutata: le riunioni interne, i rifacimenti per errore proprio e i sopralluoghi
di cortesia sono ore vere che **non** si fatturano, ma che devono comunque comparire nel costo della commessa —
altrimenti il margine è finto.

## 2. Requisiti funzionali

1. **RF-1** — Esiste una tariffa oraria predefinita dell'account, e ogni progetto può avere la propria, che
   prevale.
2. **RF-2** — Ogni riga di ore è **fatturabile** oppure **non fatturabile**; il valore predefinito lo eredita
   dall'attività, che a sua volta lo eredita dal progetto; chi dichiara le ore può cambiarlo sulla singola riga.
3. **RF-3** — Alla registrazione la riga **congela** la tariffa applicata: cambiare la tariffa del progetto in
   futuro non riscrive il passato.
4. **RF-4** — Esiste un **costo orario** dell'account, distinto dalla tariffa di vendita, usato per calcolare il
   costo della commessa (storia 0026). È un valore unico d'account, non per persona: l'app non tiene lo stipendio
   di nessuno.
5. **RF-5** — Il riepilogo di un progetto mostra ore fatturabili, ore non fatturabili e importo maturato, tenuti
   distinti.
6. **RF-6** — La modifica della tariffa di un progetto avvisa che vale solo per le ore future e chiede conferma.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `rate` filtra per `tenant_id` dal token
  verificato; la tariffa di progetto deve appartenere allo stesso account del progetto.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|PUT /api/progetti/v1/settings/rates` (predefinite
  d'account) e `GET|PUT /api/progetti/v1/projects/{id}/rate`; il riepilogo su
  `GET /api/progetti/v1/projects/{id}/time-summary`; errori in `application/problem+json`; OpenAPI aggiornata
  nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V11__tariffe.sql`: `rate` con `tenant_id`, ambito, importo orario in
  **centesimi**, valuta, validità, colonne di controllo e cancellazione logica; `time_entry` riceve
  `applied_rate_cents` e `billable`.
- **RT-4 — Modulo frontend (§3, §5).** Impostazioni delle tariffe nella sezione *Ore*; interruttore
  fatturabile/non fatturabile sull'inserimento rapido; riepilogo nella scheda del progetto; solo token del sistema
  di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, avviso sulla non retroattività e messaggi in `en, it, fr, es, de`; gli
  importi si formattano secondo la lingua e la valuta.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Ruolo minimo per vedere e cambiare le tariffe e il
  costo orario: `admin` — un `member` non deve vedere i margini dell'azienda.
- **RT-7 — Esposizione conversazionale (§12).** `log_time` accetta il parametro `fatturabile` (storia 0029);
  `get_time_summary(progetto, periodo)` è **lettura** e restituisce ore e importi solo a chi ha il ruolo per
  vederli (storia 0028).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la tariffa è di progetto e il costo orario è
  d'account. **È una scelta**: un costo orario per persona sarebbe di fatto un dato retributivo, e questa app non
  lo vuole.
- **RT-9 — Registrazione eventi (§14).** «Tariffa cambiata» con `tenant_id`, `app_id`, `user_id`, progetto e
  valore precedente e nuovo; nessun dato personale.

## 4. Criteri di accettazione

**CA-1 — Ereditarietà della tariffa**
- **Dato** un account con tariffa predefinita 50 € e un progetto con tariffa 45 €
- **Quando** una persona dichiara 2 ore fatturabili su quel progetto
- **Allora** la riga congela 45 €/ora e l'importo maturato è 90 €

**CA-2 — Non retroattività**
- **Dato** righe già registrate a 45 €/ora
- **Quando** la tariffa del progetto passa a 55 €/ora
- **Allora** le righe esistenti restano a 45 €/ora e solo le successive usano 55 €

**CA-3 — Ore non fatturabili**
- **Dato** 10 ore fatturabili e 4 non fatturabili sullo stesso progetto
- **Quando** si apre il riepilogo
- **Allora** compaiono separate, con l'importo maturato calcolato solo sulle 10

**CA-4 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member`
- **Quando** chiede le tariffe o il riepilogo economico del progetto
- **Allora** riceve `403`, e continua a poter dichiarare le proprie ore

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con tariffe diverse
- **Quando** un utente di `A` legge le tariffe
- **Allora** vede solo le proprie, anche forzando l'identificativo dell'altro account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** sull'ereditarietà e sul congelamento della tariffa (compresi gli arrotondamenti in
      centesimi) e di **integrazione** sul riepilogo;
- [ ] prova di **isolamento fra account** e prova della matrice dei ruoli su tariffe e riepilogo;
- [ ] **prova end-to-end**: coprire ora — `[J-PROGETTI]` verifica che l'importo maturato sia quello atteso
      (storia 0031); voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, con annotato **perché non esiste un costo orario per persona**;
- [ ] **registro delle decisioni** compilato, con annotato il congelamento della tariffa sulla riga;
- [ ] controllo automatico di **accessibilità** verde sulle impostazioni delle tariffe;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0006` | La tariffa di progetto vive sul progetto |
| Storia `0017` | Serve la riga di ore su cui congelare la tariffa |

## 7. Fuori ambito

- tariffe diverse per tipo di attività o per persona: rimandate; aggiungerebbero configurazione, e nel caso della
  tariffa per persona anche un dato che somiglia troppo a un dato retributivo;
- listini per cliente condivisi con altre app: se l'anagrafica condivisa porterà listini, questa storia andrà
  riletta insieme a 02 BillGrove e 06 QuoteGrove;
- valute diverse dentro lo stesso account: una sola valuta per account in questa stesura.

## 8. Punti aperti

- **Tariffa per persona**: molti concorrenti la offrono ed è una richiesta prevedibile dagli studi professionali,
  dove il socio e il praticante non valgono uguale. Introdurla significa però tenere un valore economico
  associato a una persona, con conseguenze sulla classificazione dei dati: è una decisione dello sviluppatore, da
  prendere consapevolmente e non di sfuggita.
