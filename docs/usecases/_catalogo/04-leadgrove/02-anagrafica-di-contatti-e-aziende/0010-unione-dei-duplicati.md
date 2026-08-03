# 0010 — Unione dei duplicati

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 02 — Anagrafica di contatti e aziende
**Storia**: `0010` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile commerciale
> voglio fondere due schede che sono la stessa persona, tenendo tutto lo storico
> così da non avere due mezze verità sullo stesso cliente.

**Contesto.** I doppioni nascono comunque: due venditori inseriscono lo stesso contatto, l'importazione ne
aggiunge una versione, il modulo web una terza. Un archivio con doppioni non è solo brutto: fa dire numeri sbagliati
ai rapporti di conversione (epica 06) e fa telefonare due volte alla stessa persona. La fusione è un'operazione
**distruttiva** e va trattata come tale.

## 2. Requisiti funzionali

1. **RF-1** — L'app segnala i possibili doppioni fra contatti (stessa posta elettronica, stesso telefono, oppure
   nome e cognome uguali nella stessa azienda) e fra aziende (stessa denominazione normalizzata, stessa partita
   IVA).
2. **RF-2** — L'utente sceglie quale scheda **sopravvive** e vede, campo per campo, quale valore verrà tenuto,
   potendolo cambiare prima di confermare.
3. **RF-3** — Alla fusione, trattative, attività, note, preferenze di contatto e valori personalizzati della scheda
   assorbita passano alla sopravvissuta: nulla si perde.
4. **RF-4** — La scheda assorbita viene cancellata **logicamente** e mantiene il riferimento a quella in cui è
   confluita, così che un collegamento vecchio non finisca nel vuoto.
5. **RF-5** — La fusione richiede una conferma esplicita che dice quante trattative, attività e note verranno
   spostate, e non è annullabile.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Si fondono solo schede dello stesso account, ricavato dal token; una
  richiesta che indichi una scheda di un altro account riceve `404`.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/sales/v1/contacts/duplicates`,
  `POST /api/sales/v1/contacts/{id}/merge` e le corrispondenti per le aziende; corpo validato; errori in
  `application/problem+json`; l'operazione è **idempotente** rispetto a un identificativo di richiesta, così che
  un secondo invio non fonda due volte; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** La fusione avviene in **una sola transazione**; la scheda assorbita porta
  `deleted_at` e una colonna `merged_into_id`, aggiunta con migrazione `V<N>__merge_reference.sql`.
- **RT-4 — Modulo frontend (§3, §5).** Schermata di confronto affiancato con la scelta dei valori; la conferma
  passa da una finestra modale che dice cosa verrà spostato; solo token del sistema di design.
- **RT-5 — Cinque lingue (§4).** Tutti i testi, compreso il riepilogo della conferma con i conteggi, presenti in
  `en, it, fr, es, de` con i plurali corretti.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. La fusione richiede ruolo `owner` o `admin`: un
  `member` riceve `403`.
- **RT-7 — Esposizione conversazionale (§12).** La fusione **non** è esposta come strumento conversazionale: è
  distruttiva, non reversibile e richiede un confronto visivo campo per campo che una chat non rende bene. Scelta
  dichiarata, non dimenticanza.
- **RT-8 — Dati personali (§10).** Nessuna voce nuova nel manifesto. Attenzione: la scheda assorbita resta in
  archivio con cancellazione logica, quindi **resta** nell'esportazione e nella cancellazione dei dati fino alla
  purga definitiva.
- **RT-9 — Registrazione eventi (§14).** «Schede fuse» registrato con i due identificativi, i conteggi degli
  elementi spostati e l'autore, senza nomi.

## 4. Criteri di accettazione

**CA-1 — Segnalazione**
- **Dato** due contatti con lo stesso indirizzo di posta elettronica
- **Quando** l'utente apre la sezione dei possibili doppioni
- **Allora** la coppia compare con il motivo della somiglianza

**CA-2 — Fusione che conserva lo storico**
- **Dato** una scheda `X` con 2 trattative e una scheda `Y` con 1 trattativa e 3 note
- **Quando** l'utente fonde `Y` dentro `X` e conferma
- **Allora** `X` ha 3 trattative e 3 note, `Y` risulta archiviata e punta a `X`

**CA-3 — Conferma obbligatoria**
- **Dato** una richiesta di fusione senza conferma esplicita
- **Quando** arriva al servizio
- **Allora** viene rifiutata e nulla viene spostato

**CA-4 — Doppio invio**
- **Dato** la stessa richiesta di fusione inviata due volte con lo stesso identificativo di richiesta
- **Quando** arriva la seconda
- **Allora** l'esito è identico alla prima e nulla viene spostato una seconda volta

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` tenta di fondere una scheda di `A` con una di `B`
- **Allora** riceve `404` e nulla viene modificato

**CA-6 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member`
- **Quando** tenta una fusione
- **Allora** riceve `403`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla rilevazione della somiglianza e di **integrazione** sulla fusione transazionale;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulla fusione;
- [ ] **prova end-to-end**: nessun impatto sul percorso minimo `[J-SALES]`; coperta da prove d'integrazione, con
      il motivo annotato nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, plurali compresi;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificato che le schede assorbite restino in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di non esporre la fusione alla chat;
- [ ] contratto degli **strumenti conversazionali**: esclusione motivata;
- [ ] controllo automatico di **accessibilità** verde sul confronto affiancato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0006`, `0007` | Servono le schede da fondere |
| Storia `0008` | La rilevazione dei doppioni usa la stessa normalizzazione del testo della ricerca |

## 7. Fuori ambito

- la fusione automatica senza intervento umano: esclusa per principio, fondere è distruttivo;
- il riconoscimento dei doppioni **durante** l'importazione: storia 0026, dove il contesto è diverso (si decide
  prima di scrivere, non dopo);
- l'annullamento di una fusione: non previsto; la conferma dice esplicitamente che non si torna indietro.

## 8. Punti aperti

- Nessuno.
