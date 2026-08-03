# 0006 — Anagrafica degli articoli

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 02 — Anagrafiche e catalogo prodotti
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una micro-impresa che tiene merce
> voglio censire le cose di cui tengo il conto, con il codice che uso io e una descrizione che riconosco
> così da avere un elenco unico di riferimento invece di tre fogli di calcolo che non concordano.

**Contesto.** Oggi l'app esiste, si apre e sa contare i posti del piano, ma non ha niente da contare. Questa storia
mette a terra l'entità su cui poggia tutto il resto: l'`Articolo`. È il primo passo obbligato perché la giacenza è
sempre la giacenza **di un articolo in un deposito** e senza anagrafica non c'è nulla da movimentare. Due scelte
vanno fatte adesso e non dopo: il **codice interno lo decide l'impresa** (non lo genera il programma, che al più lo
propone), perché è il codice che l'impresa ha già scritto sugli scaffali e sui documenti dei fornitori; e
l'articolo **non porta la quantità**, mai — la giacenza è un saldo derivato dal registro dei movimenti
(descrizione dell'applicazione, §4) e una colonna `quantita` sull'anagrafica sarebbe il difetto d'origine
dell'intera applicazione.

## 2. Requisiti funzionali

1. **RF-1** — Esiste la tabella `articolo` con `codice_interno`, `descrizione`, `unita_misura`, `categoria`,
   `note`, `stato` (`attivo` | `archiviato`), oltre a `tenant_id`, chiave primaria UUID versione 7, colonne di
   controllo e cancellazione logica. **Nessuna colonna di quantità**: la giacenza non vive qui.
2. **RF-2** — Il `codice_interno` è deciso da chi crea l'articolo ed è **univoco per account** fra gli articoli non
   cancellati; un doppione viene respinto con un errore che dice quale articolo occupa già quel codice. Il
   confronto ignora maiuscole, minuscole e spazi ai bordi.
3. **RF-3** — L'unità di misura è scelta da un elenco chiuso (`pezzo`, `confezione`, `metro`, `metro_quadro`,
   `chilogrammo`, `litro`, `ora`), con l'unità visualizzata tradotta nelle cinque lingue e il valore conservato
   come chiave.
4. **RF-4** — Un articolo si **archivia** e si **riattiva**. L'archiviazione lo toglie dagli elenchi operativi e
   **libera un posto della quota**; non cancella nulla e i movimenti storici restano leggibili e attribuiti a
   quell'articolo. La riattivazione riconsuma un posto e fallisce con `429` se il tetto è pieno.
5. **RF-5** — Un articolo si può archiviare anche con giacenza diversa da zero: l'app **avvisa** indicando i
   depositi in cui restano pezzi, ma non impedisce l'operazione — nascondere merce che esiste è un problema del
   cliente, falsificare il registro sarebbe un problema nostro.
6. **RF-6** — L'elenco degli articoli è paginato, ordinabile per codice e per descrizione, filtrabile per stato,
   categoria e unità di misura, e cercabile per testo su codice e descrizione.
7. **RF-7** — La cancellazione dell'articolo **non esiste** come operazione dell'interfaccia: l'unica uscita è
   l'archiviazione. La cancellazione logica della piattaforma resta disponibile solo per i diritti dell'interessato
   e per la chiusura dell'account (storia `0010`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `articolo` filtra per `tenant_id` preso dal
  token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato. Il
  vincolo di univocità del codice interno è **per account** (indice unico su `tenant_id, lower(codice_interno)`
  con `deleted_at is null`), mai globale. Prova di isolamento fra due account sulla risorsa.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/magazzino/v1/articoli`,
  `GET|PATCH /api/magazzino/v1/articoli/{id}`, `POST /api/magazzino/v1/articoli/{id}/archiviazione` e
  `POST /api/magazzino/v1/articoli/{id}/riattivazione`; oggetti di trasferimento al bordo (le entità non si
  espongono mai); validazione dichiarativa; errori in `application/problem+json`; paginazione a pagina/dimensione
  con totale; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V2__articolo_anagrafica.sql` sullo schema `app_magazzino`: tabella
  `articolo` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo (`created_at`, `updated_at`,
  `created_by`, `updated_by`) e `deleted_at`. Nessuna chiave esterna verso altri schemi. Indice di ricerca su
  `tenant_id, stato` e indice unico sul codice interno.
- **RT-4 — Modulo frontend (§3, §5).** Sezione `articoli` del modulo `magazzino`: elenco con ricerca e filtri,
  scheda di dettaglio, modulo di inserimento e modifica, azioni di archiviazione e riattivazione. Dati letti con il
  client generato dalla definizione OpenAPI; solo token del sistema di design con accento `amber`; funziona in tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — etichette, unità di misura, messaggi di errore,
  avviso di archiviazione con giacenza residua — passano dallo spazio-nomi `magazzino` e sono presenti in
  `en, it, fr, es, de`. Nessun testo scritto a mano nei componenti.
- **RT-6 — Varchi e quota (§6, §7).** La creazione di un articolo e la sua riattivazione **prenotano una unità**
  della metrica `articoli_gestiti` (natura `stock`); a tetto raggiunto rispondono `429` con il numero di articoli
  attivi, il tetto del piano e l'indicazione dei due rimedi (archiviare un articolo o passare di piano). Modifica,
  archiviazione e ricerca **non consumano quota**. Con abbonamento `canceled` il servizio risponde `402`; con
  `past_due` resta accessibile.
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati in questa storia: nessuno. `trova_articolo` ed
  `elenca_articoli` sono di proprietà della storia `0034`, che li costruisce sopra queste rotte; il contratto vive
  dentro il servizio e il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** La storia introduce **un solo** campo che può contenere dati di persone:
  `articolo.descrizione` e `articolo.note` sono testo libero e nessuno può prevedere cosa ci finisca dentro. Voce
  nuova nel manifesto `docs/compliance/manifests/magazzino.yaml` in italiano e inglese, campi annotati
  `@PersonalData`, tabella `articolo` aggiunta a `exportData` e `purgeData` (contratto completo nella storia
  `0010`). L'interfaccia mostra l'avviso «campo a testo libero: non inserire dati sensibili».
- **RT-9 — Registrazione eventi (§14).** Gli eventi `articolo creato`, `articolo archiviato`,
  `articolo riattivato`, `creazione respinta per quota` e `codice interno duplicato` sono registrati con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** descrizione e senza note.

## 4. Criteri di accettazione

**CA-1 — Creazione e ritrovamento di un articolo**
- **Dato** un utente autenticato di un account abilitato, sotto il tetto del piano
- **Quando** crea l'articolo `VT-020` «Vite testa esagonale 8×20» con unità di misura `pezzo`
- **Allora** l'articolo esiste in stato `attivo`, ha un identificativo UUID versione 7, compare nell'elenco paginato
  e si ritrova cercando sia «VT-020» sia «vite»

**CA-2 — Codice interno duplicato**
- **Dato** un account che ha già l'articolo `VT-020` · **Quando** si tenta di crearne un altro con codice `vt-020`
- **Allora** la risposta è `409` in `application/problem+json`, il messaggio nomina l'articolo esistente e nulla
  viene creato

**CA-3 — Archiviazione con giacenza residua**
- **Dato** un articolo attivo con 4 pezzi nel deposito «Magazzino»
- **Quando** l'utente lo archivia
- **Allora** l'articolo passa a `archiviato`, l'interfaccia ha avvisato che restano 4 pezzi in un deposito, il
  conteggio degli articoli attivi cala di uno e i movimenti storici restano leggibili nella scheda

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con un articolo di codice `VT-020`
- **Quando** un utente di `A` chiede l'elenco degli articoli
- **Allora** vede solo il proprio, anche se forza l'identificativo dell'account `B` nel corpo della richiesta o in
  un parametro; e il codice `VT-020` esiste legittimamente in entrambi gli account

**CA-5 — Quota esaurita alla riattivazione**
- **Dato** un account sul piano `free` con 50 articoli attivi e uno archiviato
- **Quando** tenta di riattivare l'articolo archiviato
- **Allora** riceve `429` con il conteggio 50/50 e i due rimedi indicati, e l'articolo resta `archiviato`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sulla normalizzazione del codice interno e sul consumo di quota fra archiviazione e
      riattivazione; prove di **integrazione** sulla risorsa `articoli`, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su `articolo`, compreso il caso dello stesso codice interno in due
      account;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-MAGAZZINO]` è di proprietà della storia `0036`
      (movimenti), che lo apre creando un articolo da questa schermata; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce. Il controllo di
      accessibilità automatico sulla schermata dell'elenco è invece in questa storia;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), unità di misura comprese;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `articolo.descrizione` e `articolo.note`, campi
      annotati, tabella presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta del codice interno deciso dall'impresa e dell'assenza
      di qualunque colonna di quantità sull'anagrafica;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione esposta qui, il contratto è della storia
      `0034`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0002` | Schema `app_magazzino` e cartella delle migrazioni devono esistere |
| `0003` | Il guscio del modulo frontend e le sue sezioni devono esistere per appendere la sezione `articoli` |
| `0004` | La metrica `articoli_gestiti` e la catena dei varchi devono esistere per poter prenotare un posto |

## 7. Fuori ambito

- **Codici a barre e identificativi multipli**: l'articolo qui ha il solo codice interno; i codici GTIN e quelli
  del fornitore sono della storia `0007`.
- **Depositi e ubicazioni**: storia `0008`. Finché non esistono, l'articolo non è collocato da nessuna parte.
- **Giacenza e movimenti**: epica 03. Questa storia non introduce nessuna quantità.
- **Fornitore preferito**: storia `0009`.
- **Importazione da file**: storia `0011`. Qui si inserisce un articolo alla volta.
- **Campo `origine` e catalogo condiviso**: storia `0012`.
- **Prezzo di vendita**: non è di questa app, in nessuna storia (descrizione, §10).

## 8. Punti aperti

- **Categoria come testo libero o come elenco governato**: la proposta è un campo di testo con suggerimento dai
  valori già usati nell'account — è ciò che serve a una micro-impresa e non richiede una tabella. Se il cliente
  chiedesse una gerarchia di categorie, quella è anagrafica avanzata e appartiene a PimGrove (catalogo 43),
  non a questa app. Chiude lo sviluppatore, direzione di prodotto.
- **Elenco chiuso delle unità di misura**: sette valori coprono i casi visti nell'analisi, ma non ho verificato se
  esistano settori del segmento che ne pretendono altri (per esempio la vendita a peso variabile). Aggiungerne è
  una migrazione banale; toglierne no.
