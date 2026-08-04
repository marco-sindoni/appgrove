# 0013 — Composizione della nota spese

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 03 — Note spese e approvazione
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come collaboratore che a fine mese deve farsi rimborsare
> voglio raccogliere in un solo fascicolo le spese confermate del periodo e vederne il totale
> così da presentare una cosa sola all'approvazione, invece di quindici richieste separate.

**Contesto.** Le spese confermate esistono ma sono sciolte: nessuno le può approvare in blocco, nessuno sa quando un
periodo è chiuso. La nota spese è il **fascicolo**, ed è l'unità di lavoro dell'amministrazione. Va composta prima
di poter essere inviata (storia `0014`) e approvata (storia `0015`), ed è la storia in cui si stabilisce che una
spesa **sta in una nota sola**: senza questa regola lo stesso pranzo si fa rimborsare due volte per distrazione.

## 2. Requisiti funzionali

1. **RF-1** — Si crea una nota spese scegliendo collaboratore e periodo; nasce in stato `bozza` e vuota.
2. **RF-2** — Si aggiungono e si tolgono spese `confermate` finché la nota è in `bozza`; una spesa già presente in
   un'altra nota **non è aggiungibile**, e l'app dice in quale si trova.
3. **RF-3** — L'app propone le spese confermate del collaboratore nel periodo e non ancora in nessuna nota, così che
   comporre la nota di un mese sia un'azione sola.
4. **RF-4** — La nota mostra sempre il totale, il numero di spese, quante sono senza giustificativo e quante hanno
   un avviso aperto (coerenza, tracciabilità, massimale).
5. **RF-5** — Una nota in `bozza` si può cancellare: le spese tornano libere, nessuna si perde.
6. **RF-6** — La numerazione delle note è progressiva per account e non riusa i numeri delle note cancellate.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `nota_spese` filtra per `tenant_id` preso dal
  token verificato; dentro l'account vale il filtro di visibilità per ruolo della storia `0012`: chi `sostiene` vede
  e compone solo le proprie note.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST /api/notespese/v1/note-spese`,
  `POST /api/notespese/v1/note-spese/{id}/spese` e `DELETE .../spese/{idSpesa}`; errori in
  `application/problem+json`, con `409` distinto quando la spesa è già in un'altra nota; definizione OpenAPI
  aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V10__note_spese.sql`: tabella `nota_spese` con `tenant_id`, numero
  progressivo per account, collaboratore, periodo, stato, totale, colonne di controllo e cancellazione logica; il
  legame con la spesa è una colonna sulla spesa, con **vincolo di unicità**: una spesa, una nota.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Note spese*: elenco, composizione con selezione multipla delle
  spese proposte, riepilogo dei totali e degli avvisi. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe passano dallo spazio-nomi `notespese` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Comporre una nota **non** consuma quota: la quota si è già consumata alla
  conferma di ciascuna spesa (storia `0004`). Comporre due volte lo stesso mese non costa nulla.
- **RT-7 — Esposizione conversazionale (§12).** La storia dichiara
  `crea_nota_spese(collaboratore, periodo, elenco di spese) → bozza di nota spese`, marcato **scrittura**: produce
  una bozza e richiede conferma umana. Dipendenza: UC 0061-0063, non ancora implementati.
- **RT-8 — Dati personali (§10).** La nota lega **una persona** a un elenco di spese: voce nuova nel manifesto in
  italiano e inglese, tabella `nota_spese` aggiunta a `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `nota creata`, `spesa aggiunta alla nota`, `spesa rimossa`,
  `nota cancellata` portano `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e gli
  **identificativi**, mai nomi né importi.

## 4. Criteri di accettazione

**CA-1 — Composizione del mese**
- **Dato** un collaboratore con otto spese confermate a luglio, nessuna in una nota
- **Quando** crea una nota per luglio e accetta le spese proposte
- **Allora** la nota contiene otto spese, il totale è la loro somma e le spese risultano tutte legate a quella nota

**CA-2 — Una spesa, una nota**
- **Dato** una spesa già presente nella nota `NS-12` · **Quando** si tenta di aggiungerla alla nota `NS-13`
- **Allora** l'operazione è respinta con `409`, il messaggio indica la nota in cui si trova, e nulla cambia

**CA-3 — Cancellazione della bozza**
- **Dato** una nota in `bozza` con cinque spese · **Quando** la si cancella
- **Allora** le cinque spese tornano `confermate` e libere, e nessuna risulta persa o duplicata

**CA-4 — Avvisi riepilogati**
- **Dato** una nota con due spese senza giustificativo e una fuori massimale
- **Quando** si apre il riepilogo
- **Allora** i tre avvisi sono contati e visibili **prima** dell'invio, non dopo

**CA-5 — Isolamento fra account e fra ruoli**
- **Dato** due account e, dentro uno di essi, due collaboratori con ruolo `sostiene`
- **Quando** l'uno chiede l'elenco delle note
- **Allora** vede solo le proprie: né quelle dell'altro collaboratore né, a maggior ragione, quelle dell'altro
  account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo dei totali e sul conteggio degli avvisi; di **integrazione** sulla risorsa con
      database effimero e migrazioni vere, compresa la prova concorrente sull'unicità spesa → nota;
- [ ] prova di **isolamento fra account** e di visibilità per ruolo sulle note;
- [ ] **prova end-to-end**: *coprire ora* il passo «compongo la nota del mese» nel percorso `[J-NOTESPESE]`;
      registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, tabella presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la regola «una spesa, una nota» e la numerazione progressiva;
- [ ] contratto dello strumento `crea_nota_spese` dichiarato, marcato scrittura con conferma;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0008` | Servono spese in stato `confermata` da raccogliere |
| `0012` | Servono i collaboratori e il filtro di visibilità per ruolo |

## 7. Fuori ambito

- L'invio all'approvazione: storia `0014`.
- Il documento stampabile della nota: fa parte del pacchetto per il commercialista, storia `0025`.
- Le note che attraversano più periodi contabili: la nota ha un periodo solo; se una spesa cade fuori, si mette
  nella nota del periodo giusto.

## 8. Punti aperti

- **Note collettive** (una nota che raccoglie le spese di più collaboratori, tipica di chi paga con la carta
  aziendale per il gruppo): non previste. Se servissero, cambierebbero il legame nota → collaboratore da uno a molti,
  quindi vanno decise prima e non dopo. Decisione di prodotto.
