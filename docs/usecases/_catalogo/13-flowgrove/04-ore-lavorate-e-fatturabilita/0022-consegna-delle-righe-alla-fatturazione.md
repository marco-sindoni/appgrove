# 0022 — Consegna delle righe alla fatturazione

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 04 — Ore lavorate e fatturabilità
**Storia**: `0022` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`, `0020`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha chiuso il mese
> voglio mandare le ore fatturabili all'app di fatturazione con un gesto
> così da smettere di ricopiare a mano righe da un foglio a un altro, sbagliando.

**Contesto.** È il punto in cui FlowGrove diventa **l'anello mancante della catena del valore** della suite: il
catalogo (§6) indica «preventivo → ordine → fattura → incasso» come argomento di vendita più forte, ma fra il
preventivo accettato e la fattura c'è il lavoro, e oggi nessuna app della suite lo tiene. Questa storia consegna
il lavoro alla fatturazione. Vale la regola di piattaforma: **un'app non chiama un'altra app**; la via è
asincrona, a eventi ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §2).

## 2. Requisiti funzionali

1. **RF-1** — Per un progetto e un periodo **chiuso** si compone un **lotto fatturabile**: le righe di ore
   fatturabili, raggruppate per attività (o per persona, a scelta di chi compone), con quantità, tariffa e
   importo, più i costi esterni riaddebitabili (storia 0024).
2. **RF-2** — Prima della consegna si vede l'anteprima del lotto — righe, totale, periodo, cliente — e si
   conferma. La conferma è esplicita e obbligatoria.
3. **RF-3** — La consegna pubblica un evento verso l'app di fatturazione; le righe passano allo stato
   `consegnata` e non si toccano più.
4. **RF-4** — Una riga non può finire in **due** lotti: la consegna è idempotente e un secondo tentativo sullo
   stesso periodo e progetto non produce un secondo lotto.
5. **RF-5** — Lo stato della consegna è visibile: in attesa, consegnata, fallita. Una consegna fallita si può
   ripetere; una consegnata no.
6. **RF-6** — Se l'app di fatturazione non è attiva sull'account, il lotto si può comunque **comporre** ed
   **esportare** in formato tabellare, con un messaggio che spiega la situazione: l'app deve restare utile anche
   fuori dalla suite.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La composizione e la consegna operano solo su righe dell'account del
  token verificato; l'evento pubblicato porta il `tenant_id` e viene consumato solo nel contesto di quell'account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/progetti/v1/billable-batches` (composizione con
  anteprima), `POST /api/progetti/v1/billable-batches/{id}/handoff` (consegna) e
  `GET /api/progetti/v1/billable-batches`; **nessuna chiamata sincrona** all'app di fatturazione: la consegna
  pubblica un evento; errori in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V14__lotti.sql`: `billable_batch` e `billable_batch_line` con
  `tenant_id`, colonne di controllo e cancellazione logica; la pubblicazione dell'evento usa il modello della
  cassetta in uscita, così che la scrittura del lotto e l'invio non possano divergere.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Ore → Da fatturare*: anteprima, conferma, storia delle consegne;
  solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Anteprima, stati della consegna e messaggi in `en, it, fr, es, de`; gli importi
  si formattano secondo la lingua.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Ruolo minimo per comporre e consegnare: `admin`.
  Con abbonamento `canceled` risponde `402`; l'esportazione dei propri dati resta comunque accessibile.
- **RT-7 — Esposizione conversazionale (§12).** Strumento `handoff_billable_lines(progetto, periodo)`, **scrittura
  irreversibile**: produce una bozza con l'anteprima del lotto e richiede **conferma umana obbligatoria**
  (storia 0029). È l'effetto verso l'esterno più forte dell'app: a valle diventa un documento fiscale.
- **RT-8 — Dati personali (§10).** Il lotto aggrega ore di persone: `billable_batch_line` può portare
  l'identificativo dell'utente quando il raggruppamento è per persona. Voce nel manifesto in italiano e inglese,
  campo annotato, tabella in `exportData` e `purgeData`. Attenzione al caso della cancellazione: si veda la storia
  0030.
- **RT-9 — Registrazione eventi (§14).** «Lotto composto», «lotto consegnato», «consegna fallita» con
  `tenant_id`, `app_id`, `user_id`, progetto, periodo, numero di righe e totale; nessun nome di persona.

## 4. Criteri di accettazione

**CA-1 — Composizione e consegna**
- **Dato** un progetto con 120 ore fatturabili in un periodo chiuso
- **Quando** si compone il lotto e si conferma la consegna
- **Allora** l'evento viene pubblicato, le 120 ore risultano `consegnate` e il lotto compare nello storico

**CA-2 — Nessun doppio invio**
- **Dato** un lotto già consegnato per progetto e periodo
- **Quando** si tenta di comporne un altro sugli stessi dati
- **Allora** la risposta è `409` e nessuna riga viene consegnata due volte

**CA-3 — Ore non fatturabili escluse**
- **Dato** un periodo con 120 ore fatturabili e 18 non fatturabili
- **Quando** si compone il lotto
- **Allora** il lotto contiene solo le 120, e le 18 restano nel consuntivo del progetto

**CA-4 — Periodo non chiuso**
- **Dato** un periodo ancora aperto
- **Quando** si tenta la composizione
- **Allora** la risposta è `409` con l'invito a chiudere il periodo prima

**CA-5 — App di fatturazione assente**
- **Dato** un account senza l'app di fatturazione attiva
- **Quando** si compone il lotto
- **Allora** l'anteprima funziona, la consegna è disattivata con una spiegazione e l'esportazione tabellare è
  disponibile

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** `A` consegna un lotto
- **Allora** l'evento è consumabile solo nel contesto di `A`, e nessuna riga di `B` compare nel lotto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** sull'idempotenza della consegna e di **integrazione** sulla pubblicazione dell'evento
      con la cassetta in uscita;
- [ ] prova di **isolamento fra account** sulla composizione e sull'evento;
- [ ] **prova end-to-end**: coprire ora — la consegna è l'**ultimo passo** di `[J-PROGETTI]` (storia 0031) e ne è
      la verifica decisiva; voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato per `billable_batch_line`;
- [ ] **registro delle decisioni** compilato, con annotati il contratto dell'evento e la scelta di funzionare
      anche senza l'app di fatturazione;
- [ ] controllo automatico di **accessibilità** verde sull'anteprima del lotto;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0018` | Servono tariffa congelata e distinzione fatturabile/non fatturabile |
| Storia `0020` | Si consegna solo da un periodo chiuso |
| Storia `0024` | I costi riaddebitabili entrano nel lotto: finché non c'è, il lotto contiene solo ore |
| 02 BillGrove | Deve saper consumare l'evento. Il contratto dell'evento va concordato **con quella app**, non deciso qui da soli |

## 7. Fuori ambito

- l'emissione della fattura: è di 02 BillGrove;
- la fatturazione a stato di avanzamento sui traguardi: rimandata (storia 0009, punti aperti);
- l'annullamento di una consegna già avvenuta: non previsto — a valle esiste già la nota di credito, e disfare
  una consegna sarebbe un modo per creare incoerenze fra due app.

## 8. Punti aperti

- **Il contratto dell'evento verso la fatturazione** non lo decide questa app da sola: forma delle righe, unità di
  misura, trattamento delle imposte e riferimento al cliente vanno concordati con 02 BillGrove. Finché quella app
  non esiste, la storia si può implementare fino all'esportazione tabellare e alla pubblicazione dell'evento su
  un contratto provvisorio, dichiarandolo tale.
