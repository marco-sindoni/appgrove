# 0016 — Creazione dell'ordine

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 04 — Ordini e pagamenti
**Storia**: `0016` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio che quello che il cliente ha scelto diventi un ordine con un numero
> così da poterlo cercare, ritrovarlo domani e dirgli «il tuo ordine è il 142».

**Contesto.** Il carrello è provvisorio, l'ordine è un impegno. La differenza tecnica che conta è una sola: al
momento della conversione i prezzi si **congelano**. Se il listino cambia la settimana dopo, l'ordine di oggi
deve restare quello di oggi — è la ragione per cui non si può semplicemente rinominare il carrello. È anche il
requisito di tracciabilità che il §2.3 dell'analisi indica come necessario per rispondere a un reclamo.

## 2. Requisiti funzionali

1. **RF-1** — Dal carrello si crea un ordine: le righe vengono copiate con **prezzo, nome e variante del
   momento**, e il carrello passa a `convertito`.
2. **RF-2** — L'ordine riceve un numero progressivo per account, leggibile e non riutilizzabile.
3. **RF-3** — L'ordine nasce in stato `bozza` e riporta contatto, righe, sconto, totale, valuta e la
   conversazione da cui proviene.
4. **RF-4** — Si può indicare una nota di consegna in testo libero, con l'avviso di non inserire dati sensibili.
5. **RF-5** — Alla conferma dell'ordine la disponibilità a quantità viene scalata (storia `0012`); se nel
   frattempo un prodotto è esaurito, la conferma è respinta e l'ordine resta in `bozza`.
6. **RF-6** — L'ordine è consultabile dalla sezione Ordini e dalla conversazione da cui è nato, in entrambe le
   direzioni.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `order` e `order_line` filtra per
  `tenant_id` preso dal token verificato; la numerazione è per account e non deve mai attraversarli.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `POST /api/chat_commerce/v1/conversations/{id}/cart/checkout`, `GET /api/chat_commerce/v1/orders` e
  `GET /api/chat_commerce/v1/orders/{id}`; corpo validato; paginazione con totale; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V9__ordini.sql`: tabelle `order` e `order_line` con `tenant_id`,
  chiave primaria UUID versione 7, colonne di controllo e cancellazione logica. Il numero progressivo è
  assegnato in modo **atomico** per account: due creazioni simultanee non possono ricevere lo stesso numero.
  Nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §4, §5).** Sezione Ordini con elenco, filtro per stato e scheda dell'ordine;
  collegamento reciproco con la conversazione. Tutte le stringhe in `en, it, fr, es, de`; solo token del
  sistema di design.
- **RT-5 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese per `order.contact_ref` e
  `order.shipping_note` (recapito, testo libero); campi annotati `@PersonalData`; tabelle `order` e
  `order_line` aggiunte a `exportData` e `purgeData`.
- **RT-6 — Registrazione eventi (§14).** `ordine creato`, `conferma respinta per disponibilità` con
  `tenant_id`, `app_id`, `user_id`, numero dell'ordine e identificativo di correlazione — **senza** il nome del
  contatto e senza la nota di consegna.

## 4. Criteri di accettazione

**CA-1 — Conversione**
- **Dato** un carrello con tre righe e totale 27,00 €
- **Quando** l'addetto crea l'ordine
- **Allora** nasce un ordine in `bozza` con numero progressivo, le stesse tre righe e totale 27,00 €, e il
  carrello risulta `convertito`

**CA-2 — Prezzi congelati**
- **Dato** un ordine creato con un prodotto a 12,50 €
- **Quando** il listino porta quel prodotto a 15,00 €
- **Allora** l'ordine continua a mostrare 12,50 € e il suo totale non cambia

**CA-3 — Numerazione per account**
- **Dato** due account `A` e `B` che creano ordini nello stesso momento
- **Allora** ciascuno ha la propria sequenza, senza salti condivisi né numeri doppi dentro lo stesso account

**CA-4 — Conferma respinta**
- **Dato** un ordine in `bozza` con un prodotto nel frattempo esaurito
- **Quando** si tenta di confermarlo · **Allora** riceve `409` con l'indicazione della riga, l'ordine resta in
  `bozza` e la disponibilità non cambia

**CA-5 — Isolamento fra account**
- **Dato** due account con i propri ordini · **Quando** un utente di `A` chiede l'ordine di `B` per
  identificativo · **Allora** riceve `404`

**CA-6 — Navigazione reciproca**
- **Dato** un ordine nato da una conversazione · **Quando** si apre l'ordine · **Allora** c'è il collegamento
  alla conversazione, e viceversa

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul congelamento dei prezzi e di **integrazione** sulla numerazione atomica, compresa
      la creazione simultanea;
- [ ] prova di **isolamento fra account** su ordini e righe;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, campi annotati, tabelle in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, con il congelamento dei prezzi e la numerazione per account;
- [ ] contratto degli **strumenti conversazionali**: `crea_ordine` dichiarato come **scrittura con conferma
      umana** e `elenca_ordini` come lettura;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0015` | L'ordine nasce da un carrello |

## 7. Fuori ambito

- gli stati successivi dell'ordine e l'annullamento: storia `0017`;
- la richiesta di pagamento: storia `0018`;
- la fattura: è la catena del documento contabile, che appartiene alle app 1 e 2 del catalogo e passerebbe da
  un evento, non da una chiamata diretta.

## 8. Punti aperti

- **Se l'ordine debba essere confermato dal cliente** (con un messaggio di riepilogo a cui rispondere «sì»)
  oppure solo dal negozio: la proposta è dal negozio, perché nei mercati di destinazione la conferma avviene
  a voce dentro la conversazione. È una scelta di prodotto.
