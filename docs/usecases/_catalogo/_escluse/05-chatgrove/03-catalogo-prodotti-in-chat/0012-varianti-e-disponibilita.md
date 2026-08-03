# 0012 — Varianti e disponibilità

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 03 — Catalogo prodotti in chat
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio dire che una maglia esiste in tre taglie e che la rossa è finita
> così da non vendere quello che non ho e da non dover spiegare ogni volta le differenze di prezzo.

**Contesto.** Il prodotto singolo con un prezzo unico regge solo i cataloghi più semplici. Appena si vende
abbigliamento, alimenti a peso o servizi a durata, servono le varianti e serve sapere cosa c'è. Viene subito
dopo l'anagrafica perché il carrello (storia `0015`) deve poter puntare a una variante, non a un prodotto
generico: metterle dopo il carrello significherebbe rifarlo.

## 2. Requisiti funzionali

1. **RF-1** — Un prodotto può avere varianti, ciascuna con nome (per esempio «taglia M»), differenza di prezzo
   rispetto al prodotto e disponibilità propria.
2. **RF-2** — La disponibilità è espressa in tre modi, a scelta del negozio: **non gestita** (sempre
   vendibile), **disponibile / esaurito** (interruttore), **quantità** (numero che cala).
3. **RF-3** — Un prodotto o una variante **esaurita** non si può aggiungere a un carrello: il servizio la
   rifiuta, non solo l'interfaccia la nasconde.
4. **RF-4** — Con la disponibilità a quantità, la conferma di un ordine **scala** la quantità; l'annullamento
   la ripristina.
5. **RF-5** — L'elenco del catalogo può filtrare i soli prodotti disponibili.
6. **RF-6** — La quantità non può diventare negativa: due ordini simultanei sull'ultimo pezzo non possono
   passare entrambi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `product_variant` e della disponibilità
  filtra per `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/chat_commerce/v1/products/{id}/variants`
  e `PUT /api/chat_commerce/v1/products/{id}/availability`; corpo validato (quantità non negativa); errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V7__varianti_disponibilita.sql`: tabella `product_variant` con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica; colonne di
  disponibilità su prodotto e variante. La riduzione della quantità è **atomica** a livello di database: il
  vincolo di non negatività sta sulla colonna, non solo nel codice.
- **RT-4 — Modulo frontend (§3, §4, §5).** La scheda del prodotto mostra e modifica le varianti; l'elenco
  mostra il segno di esaurito. Tutte le stringhe in `en, it, fr, es, de`; solo token del sistema di design.
- **RT-5 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-6 — Registrazione eventi (§14).** `disponibilità aggiornata`, `aggiunta rifiutata per esaurito` con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Varianti con differenza di prezzo**
- **Dato** un prodotto da 20,00 € · **Quando** si aggiunge la variante «grande» con differenza +5,00 €
- **Allora** la variante risulta a 25,00 € nell'elenco e nella scheda

**CA-2 — Prodotto esaurito**
- **Dato** una variante marcata esaurita
- **Quando** si tenta di aggiungerla a un carrello
- **Allora** la richiesta è respinta con `409` e il messaggio dice quale variante è esaurita

**CA-3 — La quantità cala**
- **Dato** una variante con quantità 3 · **Quando** si conferma un ordine che ne contiene 2
- **Allora** la quantità residua è 1

**CA-4 — L'ultimo pezzo non si vende due volte**
- **Dato** una variante con quantità 1
- **Quando** due conferme d'ordine per quella variante arrivano insieme
- **Allora** una sola va a buon fine, l'altra riceve `409`, e la quantità finale è 0 — mai negativa

**CA-5 — Isolamento fra account**
- **Dato** due account con prodotti omonimi
- **Quando** un utente di `A` modifica la disponibilità indicando l'identificativo di una variante di `B`
- **Allora** riceve `404` e nulla cambia in `B`

**CA-6 — Disponibilità non gestita**
- **Dato** un prodotto con disponibilità non gestita · **Quando** lo si aggiunge dieci volte a carrelli diversi
- **Allora** funziona sempre: la modalità «non gestita» è davvero senza limiti

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del prezzo con differenza e di **integrazione** sulla riduzione atomica
      della quantità, compreso il caso di due richieste simultanee;
- [ ] prova di **isolamento fra account** su varianti e disponibilità;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, con le tre modalità di disponibilità e il perché non se ne è
      scelta una sola;
- [ ] contratto degli **strumenti conversazionali**: `cerca_prodotto` restituisce anche la disponibilità, così
      che l'assistente non proponga ciò che non c'è;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0011` | Le varianti appartengono a un prodotto |

## 7. Fuori ambito

- il magazzino vero con carichi, scarichi e inventario (app 14 del catalogo): qui c'è solo un numero che cala;
- la prenotazione temporanea della quantità mentre il carrello è aperto: proposta esclusa per semplicità, la
  quantità cala alla conferma dell'ordine.

## 8. Punti aperti

- **Se la quantità debba calare all'aggiunta al carrello** invece che alla conferma dell'ordine: eviterebbe le
  vendite doppie ma bloccherebbe merce per carrelli mai conclusi. La proposta è alla conferma; la decisione
  cambia l'esperienza del negozio ed è di prodotto.
