# 0008 — Listini e sconti

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 02 — Anagrafiche e catalogo
**Storia**: `0008` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che pratica prezzi diversi ai rivenditori e ai clienti finali
> voglio associare un listino a un cliente e vedere i suoi prezzi comparire da soli sul documento
> così da smettere di ricordarmi a memoria chi ha il dieci per cento e chi no, e di scoprire l'errore quando il
> cliente protesta.

**Contesto.** Il prezzo differenziato è la funzione che il concorrente principale riserva al piano più alto (il
listino personalizzato è disponibile solo sul piano più costoso, §2.1 della descrizione): metterlo alla portata
della micro-impresa è un elemento di posizionamento, non un dettaglio. Va fatto dopo clienti e catalogo perché li
lega entrambi.

## 2. Requisiti funzionali

1. **RF-1** — Si può creare un listino con nome, valuta e periodo di validità.
2. **RF-2** — Un listino contiene righe che, per una voce di catalogo, fissano un prezzo alternativo **oppure** uno
   sconto percentuale sul prezzo base.
3. **RF-3** — Un cliente può avere un listino associato; se non ce l'ha, vale il prezzo base della voce.
4. **RF-4** — Quando si compone un documento per un cliente con listino, il prezzo proposto sulla riga è quello del
   listino, e l'interfaccia dice **da dove** viene il prezzo.
5. **RF-5** — Il prezzo proposto resta modificabile a mano sulla riga: il listino propone, non impone.
6. **RF-6** — Un listino scaduto non si applica più, e il documento lo segnala invece di applicarlo in silenzio.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `price_list` e delle sue righe filtra per
  `tenant_id` preso dal token verificato; nessuna forzatura dall'esterno.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST /api/billing/v1/price-lists`,
  `GET|PUT|DELETE /api/billing/v1/price-lists/{id}` e la rotta che risolve il prezzo proposto per
  `(cliente, voce, data)`; errori in `application/problem+json`; definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V5__price_list.sql` sullo schema `app_billing`: tabelle `price_list` e
  `price_list_line` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica;
  riferimento del cliente al listino sulla tabella `customer`.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Listini» dentro il Catalogo; sulla riga del documento un
  contrassegno che spiega la provenienza del prezzo. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `billing` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6).** I listini **non** consumano quota. La gestione dei listini richiede ruolo `admin`:
  è una leva sui margini, non un'operazione quotidiana.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento proprio; il prezzo risolto compare nella bozza
  prodotta da `crea_preventivo` e `crea_fattura` (epica 06), che deve dichiarare la provenienza del prezzo perché
  chi conferma sappia che cosa sta confermando.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il listino lega un cliente a dei prezzi, e il
  riferimento al cliente è già dichiarato.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `listino creato`, `listino associato a cliente` e `listino
  scaduto non applicato` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Prezzo del listino applicato**
- **Dato** un cliente con listino «Rivenditori» che fissa 80 € sulla voce `MAN-01` (prezzo base 100 €)
- **Quando** si aggiunge quella voce a un documento per quel cliente
- **Allora** il prezzo proposto è 80 € e l'interfaccia indica che viene dal listino «Rivenditori»

**CA-2 — Sconto percentuale**
- **Dato** un listino che applica il 10% di sconto sulla voce `MAN-01`
- **Quando** si aggiunge quella voce · **Allora** il prezzo proposto è 90 €

**CA-3 — Listino scaduto**
- **Dato** un cliente con listino valido fino a ieri
- **Quando** si compone un documento oggi
- **Allora** viene proposto il prezzo base e l'interfaccia segnala che il listino è scaduto

**CA-4 — Il prezzo resta modificabile**
- **Dato** un prezzo proposto dal listino · **Quando** l'utente lo cambia a mano
- **Allora** il documento usa il prezzo scritto a mano, e resta traccia che non è quello del listino

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con listini diversi
- **Quando** un utente di `A` risolve un prezzo, anche forzando l'identificativo di `B`
- **Allora** viene usato solo il listino di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla risoluzione del prezzo (prezzo fisso, sconto, listino scaduto, nessun listino) e di
      **integrazione** sulla risorsa, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su `price_list`;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-BILLING]` copre il caso senza listino; il caso con listino
      è coperto dalle prove di integrazione. Motivo: tenere il percorso end-to-end corto e leggibile. Proprietaria
      del rimando: storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, dichiarato;
- [ ] **registro delle decisioni** compilato;
- [ ] contratto degli **strumenti conversazionali**: nessuno proprio, dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | Il listino si associa a un cliente |
| storia `0007` | Le righe del listino si riferiscono a voci di catalogo |

## 7. Fuori ambito

- gli sconti di riga estemporanei sul documento: sono un campo della riga, previsto dalla storia `0002`;
- gli sconti a scaglioni di quantità: rimandati, nessuna evidenza di richiesta nel segmento micro;
- i listini in valuta diversa da quella di conto: interagiscono con la storia `0022`, che li tratta.

## 8. Punti aperti

Nessuno.
