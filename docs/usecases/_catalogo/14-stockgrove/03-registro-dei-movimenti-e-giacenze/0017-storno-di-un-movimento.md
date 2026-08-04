# 0017 — Storno di un movimento

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 03 — Registro dei movimenti e giacenze
**Storia**: `0017` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0014`, `0015`, `0016`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che si è accorto di aver sbagliato a registrare
> voglio annullare un movimento senza cancellarlo
> così da rimettere a posto la giacenza lasciando leggibile cos'era successo davvero.

**Contesto.** Gli errori di digitazione esistono: 12 invece di 1, il deposito sbagliato, il carico registrato due
volte con due chiavi diverse. La tentazione naturale è mettere un pulsante «elimina» sul movimento, ed è
esattamente ciò che questa applicazione non può fare: un movimento cancellato rende la storia **falsa** («non ho
mai caricato niente»), mentre uno storno la lascia **vera e leggibile** («ho caricato 12 per sbaglio, poi ho
stornato»). Il registro è in sola aggiunta (storia `0013`), e lo storno è l'unico modo di correggere: è la
conseguenza pratica di quella scelta, e la storia che la rende utilizzabile invece che solo rigorosa.

## 2. Requisiti funzionali

1. **RF-1** — Un movimento può essere stornato indicando un **motivo scritto obbligatorio**: lo storno senza motivo
   è respinto con `422`.
2. **RF-2** — Lo storno crea **un nuovo movimento** di tipo `storno` con quantità **opposta** a quella del
   movimento stornato — lo storno di uno scarico è un'entrata, quello di un carico è un'uscita — e con il
   riferimento al movimento stornato; la giacenza si aggiorna nella stessa transazione.
3. **RF-3** — Un movimento si storna **una sola volta**: un secondo tentativo sullo stesso movimento è respinto con
   `409` e il rimando allo storno già esistente.
4. **RF-4** — Lo storno di un movimento di trasferimento **storna la coppia**: si generano entrambi i movimenti
   opposti, legati fra loro e ciascuno al proprio originale; non è possibile stornare metà trasferimento.
5. **RF-5** — Uno storno **non si storna**: se serve annullare uno storno si registra il movimento corrispondente
   come operazione ordinaria, così che la catena resti leggibile invece di diventare una serie di annullamenti
   annidati.
6. **RF-6** — Lo storno che porterebbe la giacenza sotto zero — stornare un carico di merce già uscita — è respinto
   con `409` e la quantità residua; l'utente viene indirizzato alla rettifica con motivo (storia `0021`), che è
   l'operazione giusta quando la merce non c'è più.
7. **RF-7** — Nello storico dell'articolo un movimento stornato è mostrato **barrato** con accanto il rimando al suo
   storno, e lo storno riporta il motivo scritto: la lettura della storia non richiede di incrociare due elenchi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il movimento da stornare si cerca filtrando per `tenant_id` preso dal
  token verificato: un movimento di un altro account è inesistente (`404`), non vietato. Prova di isolamento fra due
  account sulla rotta di storno.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/magazzino/v1/movimenti/{id}/storno` con motivo nel
  corpo; errori in `application/problem+json` (`422` senza motivo, `409` per storno già esistente o giacenza
  insufficiente); definizione OpenAPI aggiornata nello stesso commit. **Non esistono** e non esisteranno rotte di
  modifica o cancellazione del movimento (storia `0013`, RF-2).
- **RT-3 — Persistenza (§8).** Migrazione `V10__movimento_storno.sql` sullo schema `app_magazzino`: colonne
  `movimento_stornato_id` e `motivo_storno` sulla tabella `movimento`, con vincolo di unicità su
  `(tenant_id, movimento_stornato_id)` che rende impossibile il doppio storno **anche in concorrenza**. Nessuna
  chiave esterna verso altri schemi; l'aggiornamento della giacenza usa l'aggiornamento condizionato della storia
  `0015`.
- **RT-4 — Modulo frontend (§3, §5).** Nello storico dei movimenti compare l'azione «Storna», con richiesta del
  motivo e conferma; il movimento stornato è mostrato barrato con il rimando al suo storno; solo token del sistema
  di design; funziona in tema chiaro e scuro. **Nessuna casella «quantità» modificabile** compare in questa o in
  altra schermata: sarebbe il difetto d'origine dell'intera applicazione.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — azione, richiesta del motivo, messaggi di rifiuto —
  passano dallo spazio-nomi `magazzino` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** **Nessun consumo di quota**: lo storno **non consuma quota e non viene mai
  respinto con `429`**. Con abbonamento `canceled` la rotta risponde `402`; l'operazione richiede almeno il ruolo
  `member`, come gli altri movimenti.
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento `storna_movimento(id_movimento, motivo) → bozza di
  storno` è dichiarato qui come contratto e implementato nella storia `0035`: è di **scrittura**, produce una bozza
  che mostra il movimento originale e l'effetto sulla giacenza, e richiede conferma umana esplicita. **Cancellare un
  movimento non esiste come strumento**, né dalla chat né dall'interfaccia. Il server conversazionale è di
  piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo.** L'autore dello storno (`created_by`) è già
  dichiarato nel manifesto dalla storia `0010`; il motivo dello storno è testo libero e ricade nella voce già
  dichiarata per le note.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `movimento stornato` e `storno respinto` sono registrati con
  `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e identificativo del movimento originale, senza
  il testo del motivo.

## 4. Criteri di accettazione

**CA-1 — Storno di un carico sbagliato**
- **Dato** un carico di 12 pezzi registrato per errore, con giacenza attuale `12`
- **Quando** un addetto lo storna indicando il motivo «quantità digitata male»
- **Allora** esiste un movimento di tipo `storno` con quantità `−12` e il riferimento al carico, la giacenza torna
  a `0`, il carico originale è ancora presente nel registro e risulta stornato

**CA-2 — Doppio storno impedito**
- **Dato** un movimento già stornato
- **Quando** si tenta di stornarlo una seconda volta
- **Allora** la risposta è `409` in `application/problem+json` con il rimando allo storno esistente, e nessun nuovo
  movimento viene scritto

**CA-3 — Storno di un trasferimento**
- **Dato** un trasferimento di 5 pezzi dal magazzino al furgone
- **Quando** si storna uno dei due movimenti della coppia
- **Allora** vengono creati **entrambi** i movimenti opposti, le due giacenze tornano ai valori precedenti e la
  somma per l'articolo resta invariata

**CA-4 — Storno senza motivo**
- **Dato** un movimento stornabile
- **Quando** si invia lo storno senza motivo o con motivo vuoto
- **Allora** la risposta è `422` e nulla viene scritto

**CA-5 — Storno che porterebbe la giacenza sotto zero**
- **Dato** un carico di 10 pezzi di cui 8 sono già stati scaricati (giacenza `2`)
- **Quando** si tenta di stornare il carico
- **Allora** la risposta è `409` con la quantità residua `2` e l'indicazione di usare la rettifica con motivo,
  e nulla viene scritto

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri movimenti
- **Quando** un utente di `A` tenta di stornare un movimento dell'account `B`
- **Allora** la risposta è `404` e il movimento di `B` resta intatto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sull'inversione del segno per ciascun tipo di movimento e di **integrazione** sulla rotta
      di storno, compreso lo storno della coppia di trasferimento;
- [ ] prova di **concorrenza** sul doppio storno simultaneo dello stesso movimento: uno solo passa, grazie al
      vincolo di unicità;
- [ ] prova di **isolamento fra account** sulla rotta di storno;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-MAGAZZINO]` è di proprietà della storia `0036`, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con la scelta dello storno come unica correzione, il divieto di
      stornare uno storno e il vincolo di unicità che impedisce il doppio storno;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `storna_movimento` (scrittura, con conferma);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0013` | Il registro in sola aggiunta è la ragione per cui lo storno esiste |
| `0014`, `0015` | Servono movimenti da stornare, di entrambi i segni |
| `0016` | Lo storno del trasferimento deve trattare la coppia |

## 7. Fuori ambito

- Il cambio di saldo quando il registro è sbagliato e non c'è un singolo movimento da annullare: è la **rettifica
  con motivo obbligatorio**, storia `0021`. Storno e rettifica rispondono a domande diverse: lo storno dice «questo
  fatto non è avvenuto», la rettifica dice «il conteggio era sbagliato».
- L'effetto dello storno sul costo medio ponderato mobile: il costo non viene ricalcolato all'indietro; il tema è
  della storia `0025`, che dichiara i limiti del valore gestionale.
- Lo storno massivo di un'importazione sbagliata: storia `0018`.

## 8. Punti aperti

- **Il costo medio dopo uno storno.** Stornare un carico che aveva alzato il costo medio non riporta il costo al
  valore precedente: ricalcolarlo all'indietro richiederebbe di rileggere tutta la storia dei carichi. La proposta è
  di **non ricalcolare** e di dichiararlo nell'interfaccia del valore gestionale (storia `0025`). Lo chiude lo
  sviluppatore, perché è una scelta sulla promessa del numero, non sull'algoritmo.
- **Finestra temporale per lo storno.** Oggi si può stornare qualunque movimento, anche di tre anni fa. Se questo
  fosse un problema per la coerenza dei periodi già chiusi con il commercialista, servirebbe una data di blocco:
  è direzione di prodotto e non appartiene a questa storia.
