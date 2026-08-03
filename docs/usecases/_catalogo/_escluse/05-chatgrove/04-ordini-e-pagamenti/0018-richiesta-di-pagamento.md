# 0018 — Richiesta di pagamento

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 04 — Ordini e pagamenti
**Storia**: `0018` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`, `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio mandare al cliente la richiesta di pagamento del suo ordine, con l'importo giusto e il mio
> collegamento di incasso
> così da farmi pagare subito, senza dettare a voce un codice di venti cifre.

**Contesto.** È il passo che chiude la vendita e il punto in cui l'app tocca il denaro — quindi il punto in cui
serve più disciplina. ChatGrove **non incassa**: emette una richiesta che punta allo strumento di incasso che
il negozio già usa (collegamento di pagamento, codice grafico, incasso istantaneo locale). Il confine è
deliberato: incassare ci metterebbe dentro la disciplina dei servizi di pagamento (§2.3 dell'analisi). Va
inoltre osservato che i mezzi di incasso nativi del canale esistono solo in alcuni paesi e le loro condizioni
sono già cambiate (§2.4): l'app tratta il **collegamento** come caso generale.

## 2. Requisiti funzionali

1. **RF-1** — Da un ordine `confermato` si crea una richiesta di pagamento con importo (predefinito: il totale
   dell'ordine), valuta, riferimento o collegamento di incasso e scadenza.
2. **RF-2** — Il negozio configura una volta i propri **mezzi di incasso** (etichetta e collegamento o
   riferimento); la richiesta ne sceglie uno.
3. **RF-3** — L'invio della richiesta al cliente **produce prima una bozza**: si vede il messaggio esatto che
   partirà, e serve una conferma esplicita per inviarlo.
4. **RF-4** — L'invio segue le regole del canale: dentro la finestra di servizio parte come messaggio libero;
   fuori serve un modello approvato e **consuma quota**.
5. **RF-5** — Una richiesta può essere `emessa`, `pagata`, `scaduta` o `annullata`; alla scadenza passa a
   `scaduta` da sola.
6. **RF-6** — Un ordine non può avere due richieste `emesse` contemporaneamente: prima si annulla quella
   esistente.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `payment_request` e dei mezzi di incasso
  filtra per `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/chat_commerce/v1/orders/{id}/payment-requests`
  (crea la bozza), `POST /api/chat_commerce/v1/payment-requests/{id}/send` (invia, con conferma),
  `POST .../cancel`; corpo validato (importo positivo, non superiore al totale dell'ordine); errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V11__richieste_di_pagamento.sql`: tabelle `payment_method` e
  `payment_request` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione
  logica. Importi in centesimi.
- **RT-4 — Varchi e quota (§6, §7).** Se l'invio richiede un modello, il servizio prenota una unità della
  metrica `messaggi_template`; a quota esaurita risponde `429` e **la richiesta resta in bozza, non inviata**.
  Con abbonamento `canceled` risponde `402`.
- **RT-5 — Ruoli (§6).** Solo `owner` e `admin` possono configurare i mezzi di incasso; tutti i ruoli possono
  emettere una richiesta su un ordine. Un `member` che tenta di cambiare un mezzo di incasso riceve `403`.
- **RT-6 — Modulo frontend (§3, §4, §5).** Azione nella scheda dell'ordine, con finestra di conferma che mostra
  il messaggio esatto e l'importo; mezzi di incasso nelle Impostazioni. Tutte le stringhe in `en, it, fr, es, de`.
- **RT-7 — Dati personali (§10).** Voce nuova nel manifesto in italiano e inglese per il riferimento di
  pagamento; tabella `payment_request` aggiunta a `exportData` e `purgeData`. **Nessun dato di carta o di conto
  del cliente finale entra nell'app**: si conserva il collegamento del negozio, non lo strumento del cliente.
- **RT-8 — Registrazione eventi (§14).** `richiesta di pagamento creata`, `inviata`, `annullata`, `scaduta`
  con `tenant_id`, `app_id`, `user_id`, numero dell'ordine, importo e identificativo di correlazione, senza
  dati del cliente.
- **RT-9 — Esposizione conversazionale (§12).** `richiedi_pagamento` è **scrittura irreversibile**: produce una
  bozza e richiede conferma umana esplicita. Il contratto vive dentro il servizio; il server conversazionale è
  di piattaforma e non ancora implementato (UC 0061-0063).

## 4. Criteri di accettazione

**CA-1 — Bozza e conferma**
- **Dato** un ordine `confermato` da 27,00 € e un mezzo di incasso configurato
- **Quando** l'addetto crea la richiesta di pagamento
- **Allora** vede la bozza del messaggio con importo e collegamento, e **nulla è ancora partito**

**CA-2 — Invio**
- **Dato** la bozza di cui sopra, in una conversazione con finestra aperta
- **Quando** l'addetto conferma l'invio
- **Allora** il messaggio parte, la richiesta risulta `emessa` e compare nel filo della conversazione

**CA-3 — Fuori finestra e quota esaurita**
- **Dato** una conversazione con finestra chiusa e un account che ha esaurito `messaggi_template`
- **Quando** si conferma l'invio · **Allora** la risposta è `429`, **nulla parte** e la richiesta resta in bozza

**CA-4 — Una sola richiesta viva**
- **Dato** un ordine con una richiesta `emessa` · **Quando** se ne crea una seconda · **Allora** la richiesta è
  respinta con `409` e il messaggio indica di annullare prima quella esistente

**CA-5 — Importo non valido**
- **Dato** un ordine da 27,00 € · **Quando** si chiede un pagamento di 50,00 € · **Allora** la richiesta è
  respinta con `400`

**CA-6 — Isolamento fra account**
- **Dato** due account · **Quando** un utente di `A` tenta di emettere una richiesta su un ordine di `B`
- **Allora** riceve `404` e nulla parte

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione dell'importo e sulla scadenza, e di **integrazione** sull'invio con
      il canale simulato, compresi quota esaurita e finestra chiusa;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** su richieste e mezzi di incasso;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`, dove l'emissione della richiesta è il penultimo passo
      del percorso `[J-CHAT-COMMERCE]`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compreso il testo della finestra di conferma;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, tabella in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta di non incassare e di emettere sempre una bozza;
- [ ] contratto degli **strumenti conversazionali** dichiarato: `richiedi_pagamento`, scrittura irreversibile
      con conferma obbligatoria;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0009` | Fuori dalla finestra serve un modello approvato |
| `0017` | La richiesta parte da un ordine `confermato` |

## 7. Fuori ambito

- l'incasso vero e la sua conferma automatica: la registrazione è la storia `0019`, e resta manuale;
- il pagamento nativo dentro il canale: disponibile solo in alcuni paesi e con condizioni mutevoli (§2.4 della
  descrizione), va valutato quando si conoscerà il mercato di destinazione;
- la fattura o la ricevuta: appartengono alle app 1 e 2 del catalogo.

## 8. Punti aperti

- **Riscontro automatico dell'avvenuto pagamento.** Collegare lo strumento di incasso del negozio per sapere
  da soli quando il cliente ha pagato sarebbe utile, ma introdurrebbe **un nuovo fornitore esterno che tratta
  dati per nostro conto** e un effetto verso l'esterno: è una decisione dello sviluppatore, non di questa
  storia. Nel frattempo l'incasso si registra a mano (storia `0019`).
