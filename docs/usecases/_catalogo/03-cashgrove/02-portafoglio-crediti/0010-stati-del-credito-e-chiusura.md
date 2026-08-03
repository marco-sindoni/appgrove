# 0010 — Stati del credito e chiusura

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 02 — Portafoglio crediti
**Storia**: `0010` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio che i crediti passino da soli a «scaduto» quando la data arriva, e poter dichiarare chiuso un credito che so
> di non incassare più
> così da avere un elenco che dice la verità senza che io lo debba tenere aggiornato a mano.

**Contesto.** Le storie precedenti fanno entrare i crediti e uscire il denaro, ma nessuno fa scattare il tempo: un
credito registrato come `aperto` resterebbe `aperto` per sempre, anche tre mesi dopo la scadenza. Serve inoltre una via
d'uscita per i crediti che non rientreranno — quelli affidati a un legale, quelli di un cliente fallito, quelli che il
titolare decide di lasciar perdere: senza, riempiono il portafoglio, falsano i numeri e consumano quota. Questa storia
chiude l'epica rendendo la macchina a stati completa e verificata.

## 2. Requisiti funzionali

1. **RF-1** — Una lavorazione programmata quotidiana porta a `scaduto` i crediti in stato `aperto` la cui data di
   scadenza è passata.
2. **RF-2** — L'utente può dichiarare un credito `stralciato`, indicando obbligatoriamente un motivo scelto fra un
   elenco (affidato a legale, cliente insolvente, rinuncia, errore di registrazione) più una nota facoltativa.
3. **RF-3** — Lo stralcio libera una unità di quota e ferma ogni sollecito futuro sul credito.
4. **RF-4** — Un credito `stralciato` può essere riportato ad `aperto` o `scaduto` da chi ha ruolo `owner` o `admin`,
   con lo stesso controllo di quota della riapertura per incasso.
5. **RF-5** — La scheda del credito mostra la **cronologia** dei cambi di stato, con istante, stato precedente, stato
   nuovo, causa (automatica o manuale) e autore.
6. **RF-6** — Nessuna transizione fuori dalla macchina a stati è possibile: una richiesta che ne chieda una risponde
   `409` nominando la transizione rifiutata.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lavorazione programmata elabora account per account, applicando il filtro
  `WHERE tenant_id = :tid`; non esiste una interrogazione che attraversi gli account. Le transizioni manuali prendono
  il `tenant_id` dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/crediti/v1/crediti/{id}/stralcio` e
  `POST /api/crediti/v1/crediti/{id}/riapertura`; errori in `application/problem+json`, con `409` per le transizioni
  rifiutate; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per la tabella `cambio_stato_credito` sullo schema `app_crediti`, con
  `tenant_id`, chiave UUID versione 7 e colonne di controllo. È un registro **in sola aggiunta**: non si modifica e non
  si cancella logicamente riga per riga.
- **RT-4 — Modulo frontend (§3, §5).** Cronologia nella scheda del credito, azione di stralcio con finestra di conferma
  che nomina il motivo scelto; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I motivi di stralcio e i testi della cronologia passano dallo spazio-nomi `crediti` e
  sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Lo stralcio libera una unità della metrica `crediti_monitorati`; la riapertura la
  riconsuma e risponde `429` se il tetto è pieno. La lavorazione programmata `aperto` → `scaduto` **non** tocca la
  quota, perché entrambi gli stati sono monitorati.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: lo stralcio è una decisione di
  merito del titolare e resta fuori dagli strumenti anche dopo la storia `0029`. Scelta esplicita, annotata con il
  motivo.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo; la tabella `cambio_stato_credito` è comunque aggiunta a
  `exportData` e `purgeData` perché riferibile al debitore.
- **RT-9 — Registrazione eventi (§14).** Ogni transizione è registrata con `tenant_id`, `app_id`, `user_id` (o
  «sistema» per quelle automatiche), stato precedente, stato nuovo e identificativo di correlazione, senza dati
  personali. La lavorazione programmata registra quanti crediti ha spostato per account.

## 4. Criteri di accettazione

**CA-1 — Scadenza automatica**
- **Dato** un credito `aperto` con scadenza ieri
- **Quando** la lavorazione programmata quotidiana viene eseguita
- **Allora** il credito è `scaduto`, la cronologia riporta la transizione con causa «automatica» e la quota non cambia

**CA-2 — Stralcio**
- **Dato** un credito `scaduto` da 90 giorni
- **Quando** il titolare lo stralcia indicando «affidato a legale»
- **Allora** lo stato è `stralciato`, il consumo di quota scende di uno e nessun sollecito futuro lo riguarda

**CA-3 — Transizione non ammessa**
- **Dato** un credito `incassato` · **Quando** si tenta di portarlo direttamente a `in_escalation` · **Allora** la
  richiesta è respinta con `409` che nomina la transizione rifiutata, e la cronologia non registra nulla

**CA-4 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member` · **Quando** tenta di riaprire un credito stralciato · **Allora** riceve `403`

**CA-5 — Isolamento della lavorazione programmata**
- **Dato** due account `A` e `B` con crediti scaduti · **Quando** la lavorazione viene eseguita · **Allora** i crediti
  di ciascun account cambiano stato correttamente e nessun registro mescola i due account

**CA-6 — Idempotenza**
- **Dato** la lavorazione già eseguita oggi · **Quando** viene eseguita di nuovo · **Allora** nessun credito cambia
  stato una seconda volta e non compaiono righe di cronologia doppie

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend);
- [ ] prove di **unità** sulla macchina a stati (tutte le transizioni ammesse e almeno tre rifiutate) e di
      **integrazione** sulla lavorazione programmata;
- [ ] prova di **isolamento fra account** sulle rotte introdotte e sulla lavorazione programmata;
- [ ] **prova end-to-end**: *rimando* alla storia `0031` — il passaggio a `scaduto` è il presupposto del percorso;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `cambio_stato_credito`, presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sull'elenco chiuso dei motivi di stralcio;
- [ ] contratto degli **strumenti conversazionali**: esclusione deliberata dello stralcio, annotata;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | Serve il credito e il suo stato iniziale |
| storia `0009` | Le transizioni verso `incassato` nascono dagli incassi |

## 7. Fuori ambito

- Gli stati `sospeso` e `in_escalation`: le transizioni verso di essi nascono dalle storie `0016` (sospensione) e
  `0011` (sequenza esaurita); qui si predispone solo la macchina a stati che le accoglierà.
- La deduzione fiscale delle perdite su crediti: è materia del commercialista, non dell'app.

## 8. Punti aperti

Nessuno.
