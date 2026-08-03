# 0016 — Trasferimento fra depositi

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 03 — Registro dei movimenti e giacenze
**Storia**: `0016` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che carica il furgone la mattina
> voglio spostare merce dal magazzino al furgone senza far finta di averla venduta
> così da sapere in ogni momento dov'è la mia merce, senza che il totale dell'impresa cambi.

**Contesto.** Un installatore con un furgone e un magazzino ha due depositi, e la merce passa dall'uno all'altro
ogni mattina. Senza il trasferimento l'unico modo di rappresentare lo spostamento sarebbe uno scarico dal magazzino
e un carico sul furgone, registrati separatamente: due movimenti scollegati che nessuno riconosce come lo stesso
fatto, con il rischio che uno dei due manchi e il totale d'impresa diventi falso. Il trasferimento è quindi **una
sola operazione che produce due movimenti opposti**, legati fra loro, in una sola transazione.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il trasferimento: articolo, deposito d'origine, deposito di destinazione, ubicazione di
   destinazione facoltativa, quantità **strettamente positiva**, nota facoltativa. Il motivo è fissato a
   `trasferimento` e non si sceglie.
2. **RF-2** — Il trasferimento produce **due** movimenti nella stessa transazione: uno di tipo
   `trasferimento_uscita` con quantità negativa sul deposito d'origine e uno di tipo `trasferimento_entrata` con
   quantità positiva sulla destinazione, legati da un **identificativo comune** che permette di riconoscerli come lo
   stesso fatto.
3. **RF-3** — Il **totale d'impresa non cambia**: la somma delle giacenze dell'articolo su tutti i depositi prima e
   dopo il trasferimento è identica; cambia solo la collocazione.
4. **RF-4** — Se il deposito d'origine non ha abbastanza merce, il trasferimento **fallisce per intero**: nessuno
   dei due movimenti resta scritto, nessuna delle due giacenze cambia, e la risposta è `409` con la quantità residua
   all'origine.
5. **RF-5** — Origine e destinazione devono essere depositi diversi e attivi dell'account: un trasferimento verso lo
   stesso deposito è respinto con `422`.
6. **RF-6** — Il **costo medio dell'articolo non cambia**: la merce è la stessa, ha solo cambiato posto (storia
   `0014`, dove il costo è per account e non per deposito).
7. **RF-7** — La coppia di movimenti è visibile come una riga sola nello storico dell'articolo, con l'indicazione di
   origine e destinazione, e come due righe negli storici dei rispettivi depositi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `movimento` e `giacenza` filtra per
  `tenant_id` preso dal token verificato; entrambi i depositi devono appartenere allo stesso account, e un deposito
  di un altro account è trattato come inesistente (`404`), non come vietato. Prova di isolamento fra due account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/magazzino/v1/trasferimenti`; corpo validato;
  errori in `application/problem+json`; `409` per merce insufficiente all'origine, `422` per depositi coincidenti;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** **Nessuna migrazione nuova**: si usano le tabelle della storia `0013`, con il campo
  già previsto per legare i due movimenti. Entrambi gli aggiornamenti di giacenza usano l'aggiornamento condizionato
  della storia `0015`, nella **stessa transazione**; le due righe di giacenza si bloccano in un **ordine
  deterministico** (per identificativo del deposito crescente) così che due trasferimenti incrociati fra gli stessi
  depositi non si blocchino a vicenda.
- **RT-4 — Modulo frontend (§3, §5).** Nuova sezione «Trasferimento» del modulo `magazzino`; origine e destinazione
  scelte da elenchi, con la giacenza dell'origine mostrata prima della conferma; solo token del sistema di design;
  funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `magazzino` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** **Nessun consumo di quota**: un trasferimento **non consuma quota e non viene
  mai respinto con `429`**; la metrica `articoli_gestiti` (natura `stock`) riguarda solo la creazione di articoli.
  Con abbonamento `canceled` la rotta risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento `trasferisci(articolo, deposito_origine,
  deposito_destinazione, quantità) → bozza di coppia di movimenti` è dichiarato qui come contratto e implementato
  nella storia `0035`: è di **scrittura** e produce una bozza con conferma umana che mostra entrambi i movimenti. Il
  server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo.** L'autore del trasferimento (`created_by`) è già
  dichiarato nel manifesto dalla storia `0010`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `trasferimento registrato` e `trasferimento respinto per
  giacenza insufficiente` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione,
  con gli identificativi dei depositi e senza note.

## 4. Criteri di accettazione

**CA-1 — Trasferimento riuscito**
- **Dato** un articolo con giacenza `20` nel deposito «Magazzino» e `0` nel deposito «Furgone»
- **Quando** si trasferiscono 5 pezzi dal magazzino al furgone
- **Allora** il magazzino ha `15`, il furgone ha `5`, esistono due movimenti legati dallo stesso identificativo
  (`−5` e `+5`) e la somma delle giacenze dell'articolo resta `20`

**CA-2 — Merce insufficiente all'origine: fallisce per intero**
- **Dato** un articolo con giacenza `3` nel magazzino e `0` nel furgone
- **Quando** si tentano di trasferire 5 pezzi
- **Allora** la risposta è `409` con la quantità residua `3`, nessuno dei due movimenti è scritto, il magazzino ha
  ancora `3` e il furgone `0`

**CA-3 — Origine e destinazione coincidenti**
- **Dato** un utente autenticato di un account abilitato
- **Quando** tenta un trasferimento con lo stesso deposito come origine e destinazione
- **Allora** la risposta è `422` in `application/problem+json` e nulla viene scritto

**CA-4 — Il costo medio non cambia**
- **Dato** un articolo con costo medio 6,00 €
- **Quando** si trasferiscono 10 pezzi da un deposito all'altro
- **Allora** il costo medio dell'articolo resta 6,00 €

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri depositi
- **Quando** un utente di `A` tenta un trasferimento indicando come destinazione un deposito dell'account `B`
- **Allora** la risposta è `404` (il deposito non esiste per lui), nulla viene scritto e le giacenze di `B` restano
  intatte

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione di origine e destinazione e di **integrazione** sulla rotta del
      trasferimento, con verifica che il fallimento non lasci movimenti orfani;
- [ ] prova di **concorrenza** su due trasferimenti incrociati fra gli stessi due depositi, che devono concludersi
      entrambi senza blocco reciproco;
- [ ] prova di **isolamento fra account** sui depositi di origine e destinazione;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-MAGAZZINO]` è di proprietà della storia `0036`, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con la scelta dei due movimenti legati e dell'ordine deterministico di
      blocco delle righe;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `trasferisci` (scrittura, con conferma);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0013` | Registro, giacenza e legame fra movimenti |
| `0015` | L'aggiornamento condizionato e il rifiuto per merce insufficiente sono gli stessi dell'uscita |

## 7. Fuori ambito

- Il trasferimento **in viaggio** (merce partita e non ancora arrivata, con un deposito di transito): oggi il
  trasferimento è istantaneo. Introdurre lo stato «in viaggio» significherebbe un terzo movimento e una tabella di
  spedizione: è materia di MoveGrove (30), non di questa app.
- Lo storno di un trasferimento, che deve stornare la coppia: storia `0017`.
- Il trasferimento avviato da scansione davanti allo scaffale: storia `0031`.

## 8. Punti aperti

- **Chi può trasferire.** La proposta è che il trasferimento sia consentito a chiunque possa registrare movimenti
  (ruolo `member` compreso), perché è l'operazione meno rischiosa: non cambia il totale. Se un cliente volesse
  riservarlo ai responsabili, servirebbe un ruolo intermedio che oggi la piattaforma non ha: lo chiude lo
  sviluppatore.
