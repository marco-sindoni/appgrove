# 0016 — Carichi e giacenze

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 04 — Prodotti, consumi e magazzino
**Storia**: `0016` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena ricevuto l'ordine dal fornitore
> voglio caricare quello che è arrivato e vedere subito quanto ne ho
> così da poter contare su un numero invece che sull'occhio, e da capire il giorno in cui il numero non torna
> perché è successo qualcosa e non perché il programma sbaglia.

**Contesto.** Questa storia stabilisce come funziona il magazzino del verticale, e la scelta strutturale è già
presa alla storia `0002`: la giacenza è la **somma dei movimenti**, non un numero che qualcuno aggiorna. È una
scelta che costa un po' di più adesso e risparmia la peggiore delle discussioni dopo — quella in cui la giacenza
dice sette, i movimenti dicono cinque e nessuno sa quale delle due credere.

## 2. Requisiti funzionali

1. **RF-1** — Si registra un **carico**: prodotto, deposito, quantità, costo d'acquisto della fornitura, data,
   riferimento libero al documento del fornitore.
2. **RF-2** — Si registra un **trasferimento** fra i due depositi: la stessa bottiglia che passa dalla rivendita
   alla cabina è un movimento in uscita e uno in entrata, mai una modifica di due numeri.
3. **RF-3** — Si registra una **rettifica** con un **motivo obbligatorio** scelto fra pochi valori (rotto,
   scaduto, errore di conteggio, omaggio, altro con nota): una rettifica senza motivo è un buco nel conto del
   margine.
4. **RF-4** — La giacenza si legge come somma dei movimenti, per prodotto e per deposito, e la scheda del prodotto
   mostra la lista dei movimenti che l'hanno prodotta.
5. **RF-5** — Un movimento **non si modifica e non si cancella**: si registra il movimento contrario, che cita
   quello che corregge.
6. **RF-6** — La giacenza può andare **sotto zero** e quando succede lo dice a voce alta: significa che si è
   consumato qualcosa che nessuno aveva caricato, ed è un'informazione, non un errore da nascondere.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Movimenti e giacenze filtrano per `tenant_id` dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/<app>/v1/movimenti`,
  `GET /api/<app>/v1/giacenze?deposito=`, `GET /api/<app>/v1/prodotti/{id}/movimenti`; corpo validato (quantità
  diversa da zero, motivo obbligatorio per la rettifica); errori in `problem+json`; OpenAPI aggiornata.
  **Nessuna rotta di aggiornamento o cancellazione di un movimento**: non esistono, e la loro assenza è il
  requisito.
- **RT-3 — Persistenza (§8).** Tabella `movimento_magazzino` **immutabile** (storia `0002`): niente `updated_*`
  valorizzabili dopo l'inserimento, niente `deleted_at`. La colonna `giacenza` esiste come comodità di lettura ed
  è ricalcolabile: una prova verifica periodicamente che coincida con la somma.
- **RT-4 — Concorrenza.** Due movimenti simultanei sullo stesso prodotto non si perdono e non si sovrascrivono: la
  somma è corretta anche sotto carico.
- **RT-5 — Modulo frontend (§3, §5).** La scheda del prodotto mostra giacenza per deposito e, sotto, i movimenti
  dal più recente; il modulo di rettifica chiede il motivo prima di consentire il salvataggio. Solo token del
  sistema di design.
- **RT-6 — Cinque lingue (§4).** Tipi di movimento, motivi di rettifica ed errori in `en, it, fr, es, de`.
- **RT-7 — Dati personali (§10).** ⚠️ Il movimento porta **chi l'ha causato**: è un dato che riguarda una persona
  che lavora nel salone. Voce nuova nel manifesto in italiano e inglese
  (`movimento_magazzino.operatore`, finalità «sapere chi ha rettificato una giacenza», base «legittimo interesse
  del salone alla correttezza del magazzino», durata proposta 24 mesi); tabella dichiarata per esportazione e
  cancellazione. **È la voce che si dimentica**, perché un movimento sembra un registro tecnico.
- **RT-8 — Esposizione conversazionale (§12).** `giacenza_prodotti(deposito?, sotto_soglia?) → elenco` in
  lettura; `rettifica_giacenza(prodotto, deposito, quantità, motivo) → bozza` in scrittura, **con conferma**:
  cambia un numero su cui si calcolano i costi.
- **RT-9 — Registrazione eventi (§14).** `movimento registrato` con tipo, `tenant_id`, `app_id`, `user_id` e
  correlazione.

## 4. Criteri di accettazione

**CA-1 — Carico e giacenza**
- **Dato** un prodotto con giacenza zero in cabina
- **Quando** si carica una quantità di dodici
- **Allora** la giacenza di cabina è dodici e nella lista compare un movimento di carico

**CA-2 — Trasferimento**
- **Dato** cinque pezzi in rivendita e zero in cabina
- **Quando** se ne trasferiscono due in cabina
- **Allora** rivendita ha tre, cabina ha due, e ci sono **due** movimenti

**CA-3 — Il movimento non si tocca**
- **Dato** un movimento di carico registrato ieri
- **Quando** si tenta di modificarlo o cancellarlo, anche direttamente dall'interfaccia di programmazione
- **Allora** l'operazione fallisce, e l'unica via offerta è un movimento contrario

**CA-4 — Rettifica senza motivo rifiutata**
- **Dato** il modulo di rettifica
- **Quando** si salva senza motivo
- **Allora** l'errore è chiaro e nulla viene registrato

**CA-5 — Sotto zero si dice**
- **Dato** un prodotto con giacenza zero
- **Quando** un consumo lo porta a meno uno
- **Allora** la giacenza mostra meno uno con un avviso visibile che invita a caricare quello che manca

**CA-6 — La somma torna sotto carico**
- **Dato** venti movimenti registrati insieme sullo stesso prodotto
- **Quando** si legge la giacenza
- **Allora** vale esattamente la somma dei venti

**CA-7 — Isolamento fra account**
- **Dato** due account con lo stesso prodotto
- **Quando** un utente del primo registra un movimento forzando l'identificativo del prodotto dell'altro
- **Allora** l'operazione è rifiutata e nessuna giacenza altrui cambia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sulla somma dei movimenti, di **integrazione** su concorrenza e immutabilità;
- [ ] prova di **isolamento fra account** su movimenti e giacenze;
- [ ] **prova end-to-end**: *rimando* — passo del percorso `[J-SALONGROVE]` della storia `0030`;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `movimento_magazzino.operatore`, campo annotato,
      tabella in esportazione e cancellazione;
- [ ] **registro delle decisioni**: giacenza come somma, immutabilità, motivo obbligatorio, giacenza negativa
      ammessa e segnalata;
- [ ] avvio locale invariato; il salone di prova ha movimenti di tutti i tipi.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0015` | non si carica un prodotto che non esiste |

## 7. Fuori ambito

- il consumo automatico durante il servizio: storia `0017`;
- la lista di riordino: storia `0018`;
- l'inventario fisico periodico con confronto e chiusura: è di un magazzino generale (app 14), e qui la rettifica
  con motivo copre il caso minimo;
- lotti e scadenze: stesso confine.

## 8. Punti aperti

Nessuno.
