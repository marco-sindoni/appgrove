# 0011 — Anagrafica dei prodotti

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 03 — Catalogo prodotti in chat
**Storia**: `0011` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio avere in un posto solo i miei prodotti con il loro prezzo
> così da smettere di cercare il prezzo giusto in tre chat diverse mentre il cliente aspetta.

**Contesto.** Oggi il prezzo sta a memoria, in una foto o in un foglio. Il §2.5 dell'analisi lo dice
chiaramente: la richiesta di avere il prezzo giusto sotto mano mentre si risponde viene prima di qualunque
automazione. È anche il presupposto tecnico del carrello e dell'ordine (epica 04): senza un listino non c'è
totale da calcolare.

## 2. Requisiti funzionali

1. **RF-1** — Il negozio crea, modifica, pubblica, ritira e cancella logicamente un prodotto con codice, nome,
   descrizione, prezzo, valuta e stato di pubblicazione.
2. **RF-2** — Il codice del prodotto è **unico dentro l'account**; se manca, l'app ne propone uno.
3. **RF-3** — L'elenco dei prodotti si cerca per testo (nome, codice, descrizione) e si filtra per stato di
   pubblicazione, con paginazione.
4. **RF-4** — Un prodotto **ritirato** non è proponibile in conversazione, ma resta leggibile negli ordini
   passati.
5. **RF-5** — Il prezzo è conservato in **centesimi** dell'unità monetaria, con la valuta accanto: nessun
   calcolo su numeri con la virgola.
6. **RF-6** — Ogni prodotto può avere una immagine; l'assenza dell'immagine non impedisce nulla.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `product` filtra per `tenant_id` preso
  dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/chat_commerce/v1/products` e
  `GET|PUT|DELETE /api/chat_commerce/v1/products/{id}`; corpo validato (prezzo non negativo, valuta a tre
  lettere, codice non vuoto); paginazione a pagina e dimensione con totale; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V6__prodotti.sql` sullo schema `app_chat_commerce`: tabella
  `product` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
  Unicità del codice su (account, codice) **fra i non cancellati**.
- **RT-4 — Ruoli (§6).** `owner` e `admin` modificano il catalogo; `member` lo legge e lo usa in conversazione.
  Un `member` che tenta la modifica riceve `403`.
- **RT-5 — Modulo frontend (§3, §4, §5).** Sezione Catalogo del modulo `chat_commerce`: elenco con ricerca e
  filtro, scheda del prodotto, modulo di inserimento con validazione. Tutte le stringhe in `en, it, fr, es, de`;
  solo token del sistema di design; tema chiaro e scuro.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo: un prodotto non riguarda una persona. Il campo
  descrizione è testo libero **del negozio**, non di terzi.
- **RT-7 — Registrazione eventi (§14).** `prodotto creato`, `prodotto modificato`, `prodotto ritirato` con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Creazione**
- **Dato** un titolare nella sezione Catalogo
- **Quando** crea un prodotto con nome, prezzo e valuta
- **Allora** il prodotto compare nell'elenco come pubblicato, con il codice proposto se non l'ha indicato

**CA-2 — Codice duplicato**
- **Dato** un prodotto con codice `TORTA-01`
- **Quando** se ne crea un secondo con lo stesso codice
- **Allora** la richiesta è respinta con `409` e il messaggio indica il codice in conflitto

**CA-3 — Ritiro non distruttivo**
- **Dato** un prodotto presente in un ordine passato
- **Quando** lo si ritira
- **Allora** non è più proponibile in conversazione, ma l'ordine passato continua a mostrarlo con il suo prezzo
  di allora

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B` con i propri prodotti
- **Quando** un utente di `A` chiede l'elenco dei prodotti
- **Allora** vede solo i propri, anche indicando l'identificativo di un prodotto di `B`

**CA-5 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member` · **Quando** tenta di modificare un prezzo · **Allora** riceve `403`

**CA-6 — Prezzo non valido**
- **Dato** il modulo di inserimento · **Quando** si indica un prezzo negativo · **Allora** la richiesta è
  respinta con `400` e l'errore è mostrato accanto al campo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla generazione del codice e sulla gestione degli importi in centesimi, e di
      **integrazione** sulle rotte del catalogo;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulla risorsa `product`;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`, dove la creazione di un prodotto è un passo del
      percorso `[J-CHAT-COMMERCE]`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova (nessun dato personale);
- [ ] **registro delle decisioni** compilato, con la scelta degli importi in centesimi e della valuta per
      prodotto;
- [ ] contratto degli **strumenti conversazionali**: `cerca_prodotto` dichiarato come strumento di **lettura**;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0002` | Serve lo schema dell'app |
| `0003` | Serve la sezione Catalogo del modulo |

## 7. Fuori ambito

- varianti e disponibilità: storia `0012`;
- il caricamento da file: storia `0013`;
- l'invio del prodotto in conversazione: storia `0014`;
- la sincronizzazione con un magazzino (app 14 del catalogo): fuori perimetro, sarebbe a eventi.

## 8. Punti aperti

- **Valuta per prodotto o per account.** La proposta è per prodotto, perché un negozio indiano vende in rupie
  mentre paga l'abbonamento in euro (punto 7 del §11 della descrizione). Se lo sviluppatore preferisce una
  valuta unica per account, il modello dati si semplifica ma si perde il caso multi-valuta: è una scelta di
  prodotto.
- **Se il prezzo debba poter essere «su richiesta»** (senza importo) per i negozi che non pubblicano i prezzi:
  proposto no, per non complicare il calcolo del carrello.
