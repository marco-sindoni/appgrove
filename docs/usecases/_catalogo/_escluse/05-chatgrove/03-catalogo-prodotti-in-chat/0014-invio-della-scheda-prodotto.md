# 0014 — Invio della scheda prodotto

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 03 — Catalogo prodotti in chat
**Storia**: `0014` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0011`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che sta rispondendo a un cliente
> voglio mandargli la scheda di un prodotto, o una piccola selezione, con due clic
> così da smettere di riscrivere ogni volta nome, prezzo e descrizione.

**Contesto.** È il punto in cui il catalogo smette di essere un archivio e diventa uno strumento di vendita.
Sta dopo la finestra di servizio (storia `0008`) perché ne eredita la regola: si può mandare liberamente solo
dentro le 24 ore. È deliberatamente piccola: fa una cosa sola, ma è quella che l'addetto ripete cento volte al
giorno.

## 2. Requisiti funzionali

1. **RF-1** — Dalla conversazione l'addetto cerca un prodotto per nome o codice e lo invia come messaggio
   formattato con nome, prezzo, variante scelta e, se c'è, immagine.
2. **RF-2** — Si possono selezionare fino a cinque prodotti e inviarli in un unico messaggio.
3. **RF-3** — I prodotti **ritirati o esauriti** non compaiono nella ricerca dell'invio.
4. **RF-4** — Il messaggio inviato resta nel filo come qualunque altro, con il proprio stato di consegna, e
   riporta un riferimento ai prodotti citati.
5. **RF-5** — L'invio segue le regole della finestra di servizio: fuori dalle 24 ore l'azione non è disponibile
   e l'app rimanda al modello approvato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La ricerca dei prodotti e l'invio filtrano per `tenant_id` preso dal
  token verificato: non è possibile inviare in una conversazione di un altro account né citare un prodotto di
  un altro account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta
  `POST /api/chat_commerce/v1/conversations/{id}/messages/products`; corpo validato (da uno a cinque
  identificativi di prodotto o variante); errori in `application/problem+json`; definizione OpenAPI aggiornata
  nello stesso commit.
- **RT-3 — Varchi e quota (§6, §7).** L'invio dentro la finestra di servizio **non** consuma quota. Fuori dalla
  finestra la rotta risponde `409` e rimanda ai modelli (storia `0009`).
- **RT-4 — Modulo frontend (§3, §4, §5).** Selettore del prodotto dentro la conversazione, con anteprima del
  messaggio. Tutte le stringhe in `en, it, fr, es, de`; solo token del sistema di design; l'anteprima è
  navigabile con la tastiera.
- **RT-5 — Dati personali (§10).** Nessun dato personale nuovo: il messaggio prodotto rientra in `message.body`,
  già dichiarato.
- **RT-6 — Registrazione eventi (§14).** `scheda prodotto inviata` con `tenant_id`, `app_id`, `user_id`,
  identificativo di correlazione e numero di prodotti citati, senza il corpo del messaggio.

## 4. Criteri di accettazione

**CA-1 — Invio di un prodotto**
- **Dato** una conversazione con finestra aperta
- **Quando** l'addetto cerca «torta» e invia il primo risultato
- **Allora** nel filo compare un messaggio con nome, prezzo e immagine del prodotto, e il contatore della
  quota non aumenta

**CA-2 — Selezione multipla**
- **Dato** la stessa conversazione · **Quando** si selezionano tre prodotti e si invia
- **Allora** parte **un solo** messaggio che li contiene tutti e tre

**CA-3 — Prodotto esaurito escluso**
- **Dato** un prodotto esaurito · **Quando** l'addetto lo cerca nel selettore d'invio · **Allora** non compare;
  una chiamata diretta con quell'identificativo risponde `409`

**CA-4 — Fuori finestra**
- **Dato** una conversazione con finestra chiusa · **Quando** si tenta l'invio di una scheda · **Allora** la
  risposta è `409` con il rimando ai modelli approvati, e nulla parte

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** un utente di `A` tenta di citare un prodotto di `B`
- **Allora** riceve `404` e nulla viene inviato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla composizione del messaggio e di **integrazione** sull'invio;
- [ ] prova di **isolamento fra account** sull'invio e sulla citazione dei prodotti;
- [ ] **prova end-to-end**: *coprire ora* è possibile ma **rimandato** alla storia `0029`, che scrive il
      percorso `[J-CHAT-COMMERCE]` completo comprendendo questo passo; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con storia proprietaria `0029`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, con il limite di cinque prodotti per messaggio e il perché;
- [ ] contratto degli **strumenti conversazionali**: l'invio della scheda è **scrittura verso l'esterno** e
      quindi passa da bozza e conferma (storia `0027`);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0008` | Serve l'invio in conversazione e la regola della finestra |
| `0011`, `0012` | Serve il catalogo con prezzi e disponibilità |

## 7. Fuori ambito

- il catalogo nativo del canale (la vetrina dentro l'applicazione di messaggistica): dipende dalle
  funzionalità del fornitore e dal paese; qui si invia un messaggio formattato, che funziona ovunque;
- l'aggiunta al carrello: storia `0015`.

## 8. Punti aperti

- **Uso del catalogo nativo del canale.** Se il fornitore lo consente nel mercato del cliente, la scheda
  prodotto nativa è più efficace di un messaggio formattato. Verificarlo richiede una prova sul campo con un
  numero vero: è una decisione di prodotto informata da un fatto che oggi non abbiamo.
