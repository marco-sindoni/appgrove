# 0035 — Chiusura del contratto dati

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0035` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`, `0026`, `0032`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare a cui un ex iscritto scrive «voglio che cancelliate tutto quello che avete su di me»
> voglio esportare e cancellare **tutto**, comprese le tabelle nate dopo l'impianto iniziale
> così da rispondere per intero, e non scoprire due anni dopo che il suo recapito era rimasto in una tabella di
> solleciti che nessuno aveva collegato.

**Contesto.** La storia `0009` ha messo in piedi il manifesto e il contratto dati quando esistevano tre tabelle;
poi l'app è cresciuta di **sei**: l'autorizzazione all'addebito (`0017`), il sollecito (`0021`), l'avviso di
rinnovo (`0013`), la richiesta dell'abbonato (`0024`), il gettone della pagina pubblica (`0023`, `0026`) e la
bozza degli strumenti conversazionali (`0032`). Ognuna di quelle storie ha il dovere, scritto nei suoi requisiti
tecnici, di aggiungere le proprie voci — ma «ognuno aggiunge le sue» è precisamente il modo in cui, alla fine, ne
manca una. Questa storia chiude il cerchio: rende la **completezza** una proprietà verificata da un programma
invece che una speranza, e affronta l'unico nodo che il controllo automatico non può sciogliere da solo — il
conflitto fra la cancellazione richiesta dall'interessato e le **prove che la legge impone di conservare**
(l'avviso di rinnovo mandato nei termini, la disdetta ricevuta: §2.3 e §6 della
[descrizione](../application-description.md)).

È l'ultima storia dell'app perché è l'unica che può essere scritta solo quando tutte le tabelle esistono.

## 2. Requisiti funzionali

1. **RF-1** — Una prova automatica **elenca** le tabelle e i campi che contengono dati riferiti a persone e
   verifica che ciascuno compaia nel manifesto, in `exportData` e in `purgeData`. Se ne manca uno, la suite è
   rossa: non è una lista da tenere aggiornata a mano.
2. **RF-2** — L'esportazione del fascicolo di un abbonato comprende **tutte e nove** le sorgenti: anagrafica,
   abbonamenti, scadenze, autorizzazione all'addebito, solleciti ricevuti, avvisi ricevuti, richieste inviate,
   collegamenti emessi verso di lui, bozze che lo riguardano ancora vive.
3. **RF-3** — La cancellazione è **fisica** e lascia una riga di prova nel registro delle purghe; sostituire i nomi
   con dei codici non è cancellare.
4. **RF-4** — Ciò che si conserva **oltre** la richiesta di cancellazione è un **elenco chiuso e motivato**: le
   prove d'invio degli avvisi di rinnovo e le richieste di disdetta, conservate in forma **ridotta al minimo**
   (quando, che cosa, con quale esito) e senza il contenuto del messaggio né il recapito completo. Ogni voce
   dell'elenco porta scritta la ragione per cui resta e per quanto.
5. **RF-5** — Chi esporta o cancella vede **in anticipo** cosa verrà cancellato e cosa resterà, con la ragione: una
   cancellazione che tace ciò che non cancella è peggio di una cancellazione parziale dichiarata.
6. **RF-6** — Esportazione e cancellazione restano accessibili anche con app disabilitata o abbonamento di
   piattaforma scaduto (sono diritti, non funzioni), e il loro esito è tracciato.
7. **RF-7** — La chiusura dell'account cancella **tutto**, elenco chiuso compreso: finito il rapporto con il
   cliente, non c'è più alcun titolare per conto del quale conservare una prova.

## 3. Requisiti tecnici

- **RT-1 — Dati personali (§10).** Manifesto `docs/compliance/manifests/abbonati.yaml` completato in **italiano e
  inglese** per le sei tabelle nate dopo la `0009`; campi annotati `@PersonalData`; contratto `AbbonatiDataContract`
  esteso in `exportData` e `purgeData`. Il controllo annotazione↔manifesto gira nelle prove del backend e deve
  essere verde: un campo annotato e non dichiarato fa già fallire la compilazione, ma il difetto opposto — un campo
  personale **non** annotato — lo scopre solo la prova del **RF-1**.
- **RT-2 — Isolamento fra account (§1).** Esportazione e cancellazione operano dentro un solo account; nessun
  percorso può toccare i dati di un altro, nemmeno per errore di scope.
- **RT-3 — Persistenza (§8).** Migrazione `V23__conservazione_prove.sql`: sulle tabelle `avviso_di_rinnovo` e
  `richiesta_dell_abbonato`, le colonne che consentono la **riduzione al minimo** (marcatura di ridotto, momento
  della riduzione) senza perdere il valore probatorio. La cancellazione fisica resta l'unica eccezione alla
  cancellazione logica.
- **RT-4 — Diritti esenti dai varchi (§13).** Nessun varco di abilitazione né di abbonamento davanti a
  esportazione e cancellazione.
- **RT-5 — Conformità (§10).** Il registro dei trattamenti si rigenera dal manifesto nello stesso commit; la parità
  fra italiano e inglese e la freschezza del registro sono controllate dall'area `compliance` di `run-tests.sh`.
- **RT-6 — Modulo frontend (§3, §5).** L'anteprima del **RF-5** — cosa si cancella, cosa resta, perché — usa i soli
  token del sistema di design e funziona in tema chiaro e scuro; è la schermata che il cliente mostrerà al suo
  iscritto quando gli spiegherà cosa è stato fatto.
- **RT-7 — Cinque lingue (§4).** I testi dell'anteprima e delle motivazioni in `en, it, fr, es, de`. Il
  **manifesto** invece ne vuole due, italiano e inglese: sono elenchi diversi e non vanno confusi.
- **RT-8 — Esposizione conversazionale (§12).** **Nessuno strumento**: esportare o cancellare i dati di una persona
  su richiesta di una chat, anche con conferma, non è un comodo in più — è il modo più veloce di cancellare i dati
  sbagliati. Si fa dall'interfaccia, con l'anteprima davanti.
- **RT-9 — Registrazione eventi (§14).** `esportazione eseguita (quante sorgenti)`, `purga eseguita (quante righe
  per tabella)`, `prove ridotte al minimo (quante)`, con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, **senza** nomi.
- **RT-10 — Prove (§11).** Oltre al **RF-1**: prova che dopo la purga nessuna riga contiene più i dati
  dell'interessato in nessuna delle tabelle, eccetto le voci dell'elenco chiuso, ridotte; prova che la chiusura
  dell'account cancella anche quelle; prova di isolamento fra account.

## 4. Criteri di accettazione

**CA-1 — Nessuna tabella dimenticata**
- **Dato** l'insieme delle tabelle dell'app con campi riferiti a persone
- **Quando** gira la prova di completezza
- **Allora** ciascuna compare nel manifesto, in `exportData` e in `purgeData`; aggiungendo una tabella nuova senza
  dichiararla, la suite diventa rossa

**CA-2 — Esportazione completa**
- **Dato** un abbonato con due abbonamenti, otto scadenze, un mandato, tre solleciti, due avvisi di rinnovo, una
  richiesta di disdetta e un collegamento emesso
- **Quando** si esporta il suo fascicolo
- **Allora** ci sono tutte le sorgenti, nessuna vuota per errore, in un file leggibile da una persona

**CA-3 — Cancellazione vera, con elenco chiuso dichiarato**
- **Dato** lo stesso abbonato · **Quando** si esegue la purga
- **Allora** non resta alcuna riga con i suoi dati, **tranne** la prova ridotta dell'avviso di rinnovo e della
  disdetta; il registro delle purghe porta la riga di prova; l'anteprima aveva detto esattamente questo **prima**
  di eseguire

**CA-4 — La prova conservata è ridotta davvero**
- **Dato** una prova d'invio conservata dopo la purga
- **Quando** la si apre
- **Allora** contiene quando, che cosa e con quale esito, e **non** il recapito completo né il testo del messaggio

**CA-5 — Chiusura dell'account**
- **Dato** un account che chiude il rapporto con appgrove
- **Quando** si esegue la cancellazione dell'account
- **Allora** non resta nulla, elenco chiuso compreso

**CA-6 — Diritti sempre accessibili**
- **Dato** un account con abbonamento di piattaforma `canceled`
- **Quando** chiede l'esportazione · **Allora** funziona, mentre le funzioni di business rispondono `402`

**CA-7 — Isolamento fra account**
- **Dato** due account con abbonati omonimi
- **Quando** uno esegue una purga · **Allora** i dati dell'altro non sono toccati

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`);
- [ ] prova automatica di **completezza** tabelle↔manifesto↔contratto, scritta per rompersi quando l'app cresce;
- [ ] prove di **integrazione** su esportazione e purga con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su esportazione e purga;
- [ ] **prova end-to-end**: *nessun impatto* — la superficie dei diritti è di piattaforma; l'anteprima del **RF-5**
      è coperta dalle prove di frontend, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) non cambia;
- [ ] **traduzioni** dell'anteprima in cinque lingue; manifesto in due (italiano e inglese);
- [ ] **manifesto dei dati** completo per tutte le tabelle dell'app, con le esclusioni dichiarate;
- [ ] registro dei trattamenti rigenerato nello stesso commit;
- [ ] **registro delle decisioni** compilato: elenco chiuso di ciò che sopravvive alla cancellazione e perché,
      forma della riduzione al minimo, nessuno strumento conversazionale sui diritti;
- [ ] documentazione aggiornata dove descrive i diritti dell'interessato per questa app.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0009` | manifesto e contratto dati nascono lì: questa storia li chiude, non li rifà |
| storie `0013`, `0017`, `0021`, `0023`, `0024`, `0026`, `0032` | sono le tabelle nate dopo, che qui vanno tutte coperte |
| **revisione legale** (punto aperto n. 8 della descrizione) | per quanto si conservano le prove d'invio: il termine lo fissa lei, l'app lo tratta come parametro |

## 7. Fuori ambito

- l'informativa verso gli abbonati: è un documento del **cliente** verso i suoi clienti, non nostro;
- la risposta alla richiesta dell'interessato in senso procedurale (chi risponde, in quanti giorni, con quale
  modulo): è di piattaforma, non di questa app;
- la cancellazione dei dati presso il **fornitore di incasso** collegato in sola lettura (storia `0020`): quei dati
  stanno da lui, e la richiesta va portata a lui — l'app dice **che** esiste quel trattamento, non può cancellarlo
  al posto suo;
- la conservazione a fini fiscali dei documenti contabili: sono di **02 BillGrove**, con i propri termini.

## 8. Punti aperti

**Quanto durano le prove che sopravvivono alla cancellazione.** Il conflitto è reale: da una parte il diritto alla
cancellazione, dall'altra l'obbligo del cliente di poter dimostrare di aver avvisato del rinnovo e di aver ricevuto
la disdetta. La via proposta — conservare **il minimo indispensabile**, per un termine dichiarato e legato alla
prescrizione dei diritti nascenti dal contratto — mi sembra difendibile, ma il termine non sono in grado di
fissarlo. Chiude: **revisione legale**
([docs/_REVISIONE-LEGALE.md](../../../../_REVISIONE-LEGALE.md)), poi lo sviluppatore.

**Se la riduzione al minimo basti a chiamarla conservazione lecita.** Una prova ridotta resta un dato riferito a una
persona identificabile attraverso l'abbonamento. **Proposta**: dichiararla come tale nel manifesto — non
pretendere che sia diventata anonima — e motivarne la base giuridica. La pseudonimizzazione non è cancellazione, e
non deve diventare una scorciatoia per raccontarsi che il problema è risolto. Chiude: **sviluppatore** (dati
personali) con la revisione legale.
