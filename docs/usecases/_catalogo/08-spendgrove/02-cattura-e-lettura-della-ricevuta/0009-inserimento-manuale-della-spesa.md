# 0009 — Inserimento manuale della spesa

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 02 — Cattura e lettura della ricevuta
**Storia**: `0009` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come collaboratore che ha perso lo scontrino del parcheggio
> voglio poter inserire la spesa a mano dichiarando che il giustificativo manca
> così da non essere costretto a rinunciare al rimborso, e da far sapere all'amministrazione che quella riga è
> scoperta.

**Contesto.** Una parte delle spese vere non ha una ricevuta fotografabile: il parcheggio a monete, il pedaggio
pagato in contanti, lo scontrino andato in lavatrice. Se l'app accettasse solo spese nate da una foto,
quelle righe tornerebbero nel foglio di calcolo — cioè fuori dall'app — e il pacchetto per il commercialista sarebbe
incompleto. Va fatta subito dopo la revisione perché condivide con essa il modulo di inserimento e la conferma, e
perché l'assenza del giustificativo è un'informazione che deve viaggiare fino all'approvazione.

## 2. Requisiti funzionali

1. **RF-1** — Si può creare una spesa senza ricevuta, compilando data, esercente, totale, categoria e mezzo di
   pagamento; la spesa nasce direttamente in `da_rivedere` con tutti i campi a fiducia «inserito a mano».
2. **RF-2** — La spesa senza giustificativo porta un **contrassegno visibile** («ricevuta mancante») che resta con
   lei fino al pacchetto per il commercialista, e un campo per il motivo dichiarato.
3. **RF-3** — Si può allegare la ricevuta **dopo**, a spesa già creata: allegandola, il contrassegno decade e parte
   la lettura automatica se la spesa non è ancora confermata.
4. **RF-4** — La conferma di una spesa manuale consuma una unità di quota esattamente come quella di una spesa
   letta: conta il documento lavorato, non il modo in cui è nato.
5. **RF-5** — L'elenco e i filtri permettono di isolare in un colpo solo le spese **senza giustificativo**, perché
   è la prima cosa che un'amministrazione vuole vedere prima di chiudere il mese.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Creazione, modifica e ricerca filtrano per `tenant_id` preso dal token
  verificato; un `tenant_id` proveniente dal corpo viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/notespese/v1/spese` con corpo validato (data non
  futura, totale positivo, categoria esistente nell'account); errori in `application/problem+json`; definizione
  OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V6__spesa_senza_giustificativo.sql`: colonne `giustificativo_mancante` e
  `motivo_mancanza` sulla tabella `spesa`, con `tenant_id` già presente e colonne di controllo invariate.
- **RT-4 — Modulo frontend (§3, §5).** Modulo di inserimento nella sezione *Spese*, con i campi obbligatori in alto
  e quelli facoltativi sotto; il contrassegno è visibile nell'elenco e nel dettaglio. Solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, aiuti e testo del contrassegno passano dallo spazio-nomi `notespese` e
  sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Alla conferma il servizio prenota una unità della metrica `receipts` (natura
  `flow`); a quota esaurita risponde `429`. La creazione della bozza non consuma.
- **RT-7 — Esposizione conversazionale (§12).** La storia dichiara
  `crea_spesa(data, esercente, totale, categoria, mezzo di pagamento) → bozza di spesa`, marcato **scrittura**:
  produce una bozza in `da_rivedere` e richiede conferma umana. Dipendenza dichiarata: UC 0061-0063, non ancora
  implementati.
- **RT-8 — Dati personali (§10).** Nessuna categoria nuova rispetto alla storia `0002`; si aggiunge il campo
  **`motivo_mancanza`, che è testo libero** e quindi un ingresso non presidiato: porta l'avviso di non inserire dati
  sensibili e resta fuori dai registri degli eventi. Voce aggiornata nel manifesto in italiano e inglese.
- **RT-9 — Registrazione eventi (§14).** L'evento `spesa creata a mano` porta `tenant_id`, `app_id`, `user_id`,
  identificativo di correlazione e il fatto che il giustificativo manchi — **non** il motivo scritto dall'utente.

## 4. Criteri di accettazione

**CA-1 — Spesa senza ricevuta**
- **Dato** un utente autenticato di un account abilitato
- **Quando** crea una spesa di 3,50 € per un parcheggio, dichiarando che lo scontrino è andato perso, e conferma
- **Allora** la spesa è `confermata`, porta il contrassegno «ricevuta mancante» e il consumo di quota è aumentato
  di uno

**CA-2 — Ricevuta allegata dopo**
- **Dato** una spesa manuale in `da_rivedere` con contrassegno · **Quando** l'utente vi allega la foto ritrovata
- **Allora** il contrassegno decade, parte la lettura automatica e i valori proposti si affiancano a quelli già
  inseriti senza sovrascriverli in silenzio

**CA-3 — Validazione**
- **Dato** il modulo di inserimento · **Quando** l'utente indica una data nel futuro o un totale negativo
- **Allora** la creazione è respinta con `400` in `application/problem+json` e l'errore è mostrato accanto al campo

**CA-4 — Filtro delle spese scoperte**
- **Dato** un account con dieci spese di cui tre senza giustificativo
- **Quando** l'amministrazione filtra per «ricevuta mancante»
- **Allora** vede esattamente quelle tre

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** un utente di `A` crea una spesa forzando l'identificativo dell'account
  `B` nel corpo
- **Allora** la spesa nasce nell'account `A`: il valore forzato viene ignorato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione e di **integrazione** sulla creazione con database effimero e migrazioni
      vere;
- [ ] prova di **isolamento fra account** sulla creazione e sulla ricerca;
- [ ] **prova end-to-end**: *rimando* alla storia `0031` — il percorso `[J-NOTESPESE]` copre il ramo con ricevuta;
      il ramo manuale è un secondo passo dello stesso percorso e viene aggiunto lì, con la voce nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per il campo di testo libero;
- [ ] **registro delle decisioni** compilato, con la scelta di far consumare quota anche alla spesa manuale;
- [ ] contratto dello strumento `crea_spesa` dichiarato, marcato scrittura con conferma;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0002` | Serve la tabella `spesa` e la macchina a stati |
| `0008` | Il modulo di inserimento e la conferma sono gli stessi della revisione: rifarli sarebbe duplicazione |

## 7. Fuori ambito

- L'autodichiarazione firmata che in alcuni casi sostituisce il giustificativo: è un documento con valore fiscale e
  richiede una valutazione che non appartiene a questa storia. Qui si registra solo che manca.
- Il blocco dell'approvazione per le spese scoperte: è una politica, e sta nella storia `0017`.

## 8. Punti aperti

- **Fino a che importo una spesa senza giustificativo è accettabile** è una decisione dell'azienda cliente, non
  nostra: l'app la rende configurabile nella storia `0017` e non impone soglie proprie.
