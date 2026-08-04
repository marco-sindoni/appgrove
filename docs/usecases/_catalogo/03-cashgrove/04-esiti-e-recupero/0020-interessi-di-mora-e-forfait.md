# 0020 — Interessi di mora e forfait

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 04 — Esiti e recupero
**Storia**: `0020` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio sapere quanto mi spetta di interessi di mora su una fattura pagata in ritardo, calcolato come dice la legge
> così da poterlo scrivere nel sollecito o nella messa in mora senza chiedere al commercialista e senza sbagliare.

**Contesto.** Nelle transazioni fra imprese gli interessi di mora sono **automaticamente dovuti**: la direttiva europea
sui ritardi di pagamento, recepita in Italia dal decreto legislativo 231 del 2002, prevede un tasso pari al riferimento
della Banca centrale europea maggiorato di almeno otto punti percentuali, più un **importo forfettario minimo di 40 euro
per ogni fattura** pagata in ritardo, oltre alle spese di recupero effettivamente sostenute. Il tasso cambia ogni
semestre ed è pubblicato in Gazzetta Ufficiale: per il primo semestre 2026 è il 10,15% (2,15% + 8)
([documento capofila](../application-description.md) §2.3, punto 1). Quasi nessuna micro-impresa li chiede, perché non
sa calcolarli. Farlo è insieme una funzione utile e un argomento di sollecito: sapere che l'orologio gira cambia la
priorità del debitore.

## 2. Requisiti funzionali

1. **RF-1** — L'app tiene una tabella dei tassi **storicizzata per semestre** (data di inizio, data di fine, tasso di
   riferimento, maggiorazione in punti), aggiornabile senza rilasciare codice.
2. **RF-2** — Su richiesta, l'app calcola per un credito scaduto: giorni di ritardo, interessi maturati giorno per
   giorno attraversando correttamente i cambi di semestre, e importo forfettario.
3. **RF-3** — Il calcolo si appoggia al **residuo** e tiene conto dei pagamenti parziali: gli interessi maturano
   sull'importo effettivamente non pagato in ciascun periodo.
4. **RF-4** — I parametri (termine di pagamento predefinito, maggiorazione, importo forfettario) sono configurabili per
   account, perché la norma cambia per giurisdizione e potrebbe cambiare nel tempo.
5. **RF-5** — L'utente decide se includere gli interessi in un sollecito, se rinunciarvi o se lasciarli solo come
   informazione interna; la scelta è registrata sul credito.
6. **RF-6** — Il dettaglio del calcolo è sempre visibile: quali periodi, quale tasso per periodo, quanti giorni, quale
   base — perché un numero che non si può spiegare non si può mettere in una lettera.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo e gli addebiti filtrano per `tenant_id` preso dal token
  verificato; i parametri sono per account. La tabella dei tassi è invece **comune a tutti**, perché è un dato di
  legge: è di sola lettura per gli account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/crediti/v1/crediti/{id}/mora` (calcolo con dettaglio)
  e `POST /api/crediti/v1/crediti/{id}/mora` (registrazione della decisione); errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazioni per le tabelle `tasso_mora` (comune, senza `tenant_id`, in sola lettura per
  gli account) e `addebito_di_mora` (con `tenant_id`, chiave UUID versione 7, colonne di controllo e cancellazione
  logica). Gli importi sono calcolati con precisione decimale esatta, mai in virgola mobile: un centesimo sbagliato in
  una messa in mora è un errore.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro «interessi di mora» sulla scheda del credito, con il dettaglio del
  calcolo apribile e le tre scelte (includi, rinuncia, solo informazione); parametri nella sezione *Impostazioni*; solo
  token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `crediti` e sono presenti in
  `en, it, fr, es, de`. Gli importi e le percentuali sono formattati secondo la lingua attiva. **Nessuna spiegazione
  giuridica scritta a mano nei componenti**: i testi di aiuto stanno nelle traduzioni come tutto il resto.
- **RT-6 — Varchi e quota (§6, §7).** Il calcolo non consuma quota. Modificare i parametri richiede ruolo `owner` o
  `admin`.
- **RT-7 — Esposizione conversazionale (§12).** Il calcolo è esposto come parte del risultato di
  `elenca_crediti_scaduti` e della bozza di `prepara_messa_in_mora` (storia `0029`); non esiste uno strumento che
  **decida** di addebitare la mora: quella è una scelta commerciale del titolare.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. La tabella `addebito_di_mora` è aggiunta a `exportData`
  e `purgeData` perché riferibile al debitore.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «mora calcolata», «mora inclusa nel sollecito», «rinuncia alla
  mora» sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza importi.

## 4. Criteri di accettazione

**CA-1 — Calcolo su un semestre**
- **Dato** una fattura da 10.000 € scaduta da 90 giorni, tutti dentro un semestre con tasso 10,15%
- **Quando** si chiede il calcolo
- **Allora** gli interessi sono 250,27 € (10.000 × 10,15% × 90 / 36.500) e il forfait è 40 €, per un totale di 290,27 €,
  con il dettaglio del calcolo visibile

**CA-2 — Calcolo a cavallo di due semestri**
- **Dato** un ritardo che attraversa il 30 giugno con due tassi diversi
- **Quando** si chiede il calcolo
- **Allora** il risultato è la somma dei due tronconi, ciascuno col proprio tasso e i propri giorni, e il dettaglio li
  mostra separati

**CA-3 — Pagamento parziale**
- **Dato** un credito da 1.000 € pagato per 600 € dopo 30 giorni e ancora scoperto per 400 € dopo altri 30
- **Quando** si chiede il calcolo
- **Allora** gli interessi dei primi 30 giorni sono su 1.000 € e quelli dei secondi 30 su 400 €

**CA-4 — Tasso mancante**
- **Dato** un periodo per il quale la tabella dei tassi non ha una riga · **Quando** si chiede il calcolo · **Allora**
  l'app risponde con un errore esplicito che dice quale periodo manca — **non** stima e non estrapola

**CA-5 — Rinuncia**
- **Dato** un credito con mora calcolata · **Quando** il titolare sceglie «rinuncia» · **Allora** la mora non compare
  in nessun sollecito né nella messa in mora, e la scelta resta registrata sul credito

**CA-6 — Isolamento fra account**
- **Dato** due account con parametri diversi (maggiorazione 8 e 9 punti) · **Quando** entrambi calcolano sullo stesso
  scenario · **Allora** ottengono risultati diversi, ciascuno secondo i propri parametri

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend);
- [ ] prove di **unità** sul calcolo, con almeno: un semestre, due semestri, anno bisestile, pagamento parziale,
      periodo senza tasso; e di **integrazione** sulla rotta di calcolo;
- [ ] prova di **isolamento fra account** su parametri e addebiti;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, dove la mora compare nella messa in mora;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `addebito_di_mora`, presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sul rifiuto di stimare quando manca un tasso e sulla
      configurabilità dei parametri;
- [ ] contratto degli **strumenti conversazionali**: il calcolo entra nei risultati esistenti, nessuno strumento nuovo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | Serve il credito con la sua scadenza |
| storia `0010` | Serve lo stato `scaduto` e la data da cui decorre il ritardo |

## 7. Fuori ambito

- L'aggiornamento **automatico** della tabella dei tassi da una fonte ufficiale: rimandato, perché richiederebbe una
  integrazione con una fonte che nessuna delle ricerche ha mostrato disponibile in forma leggibile da un programma.
  L'aggiornamento è manuale, due volte l'anno, ed è una attività di piattaforma.
- Il rimborso delle **spese di recupero effettivamente sostenute** oltre il forfait: si può indicare a mano nella messa
  in mora (storia `0021`), non si calcola.
- Le regole di giurisdizioni fuori dall'Unione: i parametri sono configurabili, ma l'app non porta con sé le tabelle di
  altri ordinamenti.

## 8. Punti aperti

**Lo stato del regolamento europeo** che sostituirebbe la direttiva (termine unico a 30 giorni, interessi automatici,
autorità di vigilanza) non è stato determinato: punto aperto n. 8 del documento capofila §11. È la ragione per cui
termini e maggiorazione sono parametri e non costanti — ma se il regolamento entrasse in vigore andrebbero riviste anche
le impostazioni predefinite.
