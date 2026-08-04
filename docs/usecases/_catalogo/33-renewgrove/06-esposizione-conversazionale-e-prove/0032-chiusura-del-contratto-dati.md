# 0032 — Chiusura del contratto dati

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0032` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`, `0014` — il rapporto sorvegliato è il soggetto dell'esportazione, la spiegazione del punteggio ne è la parte che conta
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare a cui un ex cliente scrive «che cosa avete su di me, e chi vi ha detto che stavo per andarmene?»
> voglio esportare **tutto**, spiegazione del punteggio compresa, e poi cancellare **tutto** davvero
> così da rispondere per intero a una persona che non ha mai avuto rapporti con appgrove ma su cui appgrove ha
> formulato un giudizio.

**Contesto.** Questa è l'ultima storia dell'app, ed è l'unica che si può scrivere solo quando tutte le tabelle
esistono. Il manifesto e lo scheletro del contratto dati sono nati vuoti con la `0001` e sono cresciuti storia per
storia; «ognuno aggiunge le sue voci» è però precisamente il modo in cui, alla fine, ne manca una. Qui la
**completezza** diventa una proprietà verificata da un programma. E c'è un obbligo specifico di questa app, che
nessuna sorella ha: RenewGrove fa **profilazione**, e la [descrizione](../application-description.md) al §6, punto
5, lo scrive come presidio — *il cliente finale ha diritto a sapere: la spiegazione del punteggio è parte
dell'esportazione, non un segreto industriale*. Un'esportazione che consegnasse il numero senza i fatti da cui
nasce sarebbe formalmente completa e sostanzialmente inutile, proprio sulla domanda che la sentenza C-634/21 rende
esigibile (§2.3).

## 2. Requisiti funzionali

1. **RF-1** — Il contratto `FidelizzazioneDataContract` implementa `appId()`, `exportData(scope)`,
   `purgeData(scope)` e `manifest()`, e comprende **tutte** le tabelle con dati riferiti a persone: `rapporto`,
   `segnale`, `punteggio`, `contributo_punteggio`, `intervento`, `offerta_di_trattenuta`, `esito_del_rapporto`,
   `contestazione` — più quelle nate nelle storie successive alla stesura della descrizione (`correzione_motivo`,
   `bozza_di_strumento`).
2. **RF-2** — Una **verifica eseguibile da un programma** elenca le tabelle e i campi che contengono dati riferiti a
   persone e controlla che ciascuno compaia nel manifesto, in `exportData` **e** in `purgeData`. Aggiungere una
   tabella con dati personali senza dichiararla fa diventare **rossa** la suite: non è una lista da tenere
   aggiornata a mano.
3. **RF-3** — L'esportazione comprende la **spiegazione del punteggio**: per ogni valore della serie storica del
   rapporto, la fascia, la **versione del modello** viva quel giorno, i **contributi con peso e verso**, i **fatti
   datati** che li hanno prodotti (tipo, fonte, momento, intensità) e le **contestazioni** che li riguardano, con
   l'effetto che hanno avuto. Chi è stato profilato ha diritto di sapere **da quali fatti nasceva il giudizio**, non
   solo che un giudizio c'era.
4. **RF-4** — La cancellazione è **fisica** e lascia una riga di prova nel **registro delle purghe**: sostituire i
   nomi con dei codici **non è cancellare**, e la pseudonimizzazione non soddisfa la richiesta.
5. **RF-5** — La cancellazione **non riscrive il rendiconto di efficacia** (`0027`), che conserva solo conteggi
   aggregati già calcolati: il rapporto e i suoi esiti spariscono, i periodi chiusi restano. L'anteprima lo **dice
   prima** di eseguire, insieme a tutto il resto di ciò che verrà cancellato e di ciò che resterà.
6. **RF-6** — Il **manifesto** `docs/compliance/manifests/fidelizzazione.yaml` è completo, in **italiano e inglese**
   su ogni testo, e dichiara anche le **esclusioni motivate**: `fonte`, `modello_di_punteggio`,
   `piano_di_intervento`, `coorte_di_confronto` e `rendiconto_efficacia` non contengono dati riferiti a clienti
   finali, e il perché è scritto.
7. **RF-7** — Esportazione e cancellazione restano accessibili **anche** con app disabilitata o abbonamento cessato
   (sono diritti, non funzioni), e il loro esito è tracciato.

## 3. Requisiti tecnici

- **RT-1 — Dati personali (§10).** Manifesto completato in italiano e inglese per tutte le voci; campi annotati
  `@PersonalData` — un campo annotato e non dichiarato fa già fallire la compilazione, mentre il difetto opposto (un
  campo personale **non** annotato) lo scopre solo la verifica del **RF-2**. Contratto
  `FidelizzazioneDataContract` esteso in `exportData` e `purgeData`. Il registro dei trattamenti si rigenera dal
  manifesto nello stesso commit; parità italiano/inglese e freschezza del registro sono sorvegliate dall'area
  `compliance` di `run-tests.sh`.
- **RT-2 — Isolamento fra account (§1).** Esportazione e cancellazione operano dentro **un solo** account: nessun
  ambito può toccare i dati di un altro, nemmeno per errore, e la prova lo verifica con due account che hanno
  rapporti con la stessa etichetta.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. La cancellazione **fisica** resta l'unica eccezione alla
  cancellazione logica, ed è consentita anche sulle tabelle **in sola aggiunta** (`segnale`, `punteggio`,
  `esito_del_rapporto`): la sola aggiunta è una regola di correttezza della misura, non un ostacolo a un diritto.
- **RT-4 — Diritti esenti dai varchi (§13).** Nessun varco di abilitazione né di abbonamento davanti a esportazione
  e cancellazione; la prova della matrice della storia `0031` lo verifica dall'esterno.
- **RT-5 — Interfaccia di programmazione (§2).** Le rotte dei diritti sono quelle di piattaforma; l'app espone il
  contratto, non una propria via parallela. Errori in `application/problem+json`.
- **RT-6 — Modulo frontend (§3, §5).** L'**anteprima** del **RF-5** — cosa si cancella, cosa resta, perché — è una
  schermata del modulo `fidelizzazione` con i soli token del sistema di design, in tema chiaro e scuro. È la
  schermata che il nostro cliente mostrerà al **suo** cliente quando gli spiegherà che cosa è stato fatto.
- **RT-7 — Cinque lingue (§4).** Anteprima, motivazioni e messaggi in `en, it, fr, es, de`. Il **manifesto** invece
  ne vuole **due**, italiano e inglese: sono elenchi diversi e non vanno confusi.
- **RT-8 — Esposizione conversazionale (§12).** **Nessuno strumento**: esportare o cancellare i dati di una persona
  su richiesta di una chat, anche con conferma, non è un comodo in più — è il modo più veloce di cancellare i dati
  sbagliati. Si fa dall'interfaccia, con l'anteprima davanti. La scelta va scritta nel registro delle decisioni
  perché è una **deroga voluta** al requisito trasversale del catalogo.
- **RT-9 — Registrazione eventi (§14).** `esportazione eseguita (quante sorgenti)`, `purga eseguita (quante righe
  per tabella)`, `anteprima consultata`, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione,
  **senza** etichette di rapporti né contenuti.
- **RT-10 — Prove (§11).** Oltre al **RF-2**: prova che dopo la purga nessuna riga contenga più i dati
  dell'interessato in **nessuna** tabella; prova che i conteggi di un periodo chiuso del rendiconto siano identici
  prima e dopo; prova che l'esportazione contenga la spiegazione completa di ogni punteggio della serie; prova di
  isolamento fra account; tutto su database effimero con migrazioni vere.

## 4. Criteri di accettazione

**CA-1 — Nessuna tabella dimenticata**
- **Dato** l'insieme delle tabelle dell'app con campi riferiti a persone
- **Quando** gira la verifica di completezza
- **Allora** ciascuna compare nel manifesto, in `exportData` e in `purgeData`; aggiungendo una tabella nuova con un
  campo personale senza dichiararla, la suite diventa rossa indicando quale

**CA-2 — La spiegazione è dentro l'esportazione**
- **Dato** un rapporto con nove segnali, tre valori di punteggio in serie storica e una contestazione
- **Quando** si esporta il suo fascicolo
- **Allora** il file contiene, per ciascuno dei tre valori, fascia, versione del modello, contributi con peso e
  verso, i fatti datati che li hanno prodotti e la contestazione con il suo effetto — in forma leggibile da una
  persona

**CA-3 — Cancellazione vera**
- **Dato** lo stesso rapporto, con interventi, un'offerta autorizzata ed esiti valutati
- **Quando** si esegue la purga
- **Allora** non resta alcuna riga con i suoi dati in nessuna delle tabelle dichiarate, il registro delle purghe
  porta la riga di prova, e nessun dato è stato semplicemente sostituito da un codice

**CA-4 — La misura non si riscrive**
- **Dato** un periodo chiuso del rendiconto con 12 trattenuti su 40, comprendente il rapporto cancellato
- **Quando** la purga è eseguita
- **Allora** i conteggi del periodo chiuso restano 12 su 40, e l'anteprima aveva dichiarato **prima** che sarebbe
  successo questo

**CA-5 — Diritti sempre accessibili**
- **Dato** un account con abbonamento `canceled` e l'app disabilitata
- **Quando** chiede l'esportazione e poi la cancellazione
- **Allora** entrambe funzionano, mentre le funzioni di business rispondono `402`

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con un rapporto con la **stessa etichetta**
- **Quando** `A` esegue la purga del proprio
- **Allora** il rapporto di `B`, i suoi segnali, punteggi ed esiti sono intatti

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] verifica automatica di **completezza** tabelle↔manifesto↔contratto, scritta per rompersi quando l'app cresce;
- [ ] prove di **integrazione** su esportazione e purga con database effimero e migrazioni vere, compresa quella che
      confronta i conteggi del rendiconto prima e dopo;
- [ ] prova di **isolamento fra account** su esportazione e purga, con etichette omonime;
- [ ] **prova end-to-end**: *nessun impatto sul percorso* — la superficie dei diritti è di piattaforma e una prova
      che distrugge i propri dati di partenza è fragile; l'accessibilità dell'esportazione a varchi chiusi è già
      coperta dalla matrice della storia `0031`, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) non cambia per questa storia;
- [ ] **traduzioni** dell'anteprima e delle motivazioni in cinque lingue; **manifesto** in due (italiano e inglese);
- [ ] **manifesto dei dati** completo per tutte le tabelle, con le **esclusioni motivate** dichiarate;
- [ ] registro dei trattamenti rigenerato dal manifesto nello stesso commit;
- [ ] **registro delle decisioni** compilato: spiegazione del punteggio dentro l'esportazione e perché, aggregati
      che sopravvivono alla cancellazione, cancellazione fisica anche sulle tabelle in sola aggiunta, nessuno
      strumento conversazionale sui diritti;
- [ ] contratto degli **strumenti conversazionali**: nessuno, con la motivazione scritta;
- [ ] documentazione aggiornata dove la descrizione tratta esportazione e cancellazione (§6).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0009` (il rapporto sorvegliato) | è il soggetto dell'esportazione: senza rapporto non c'è fascicolo da consegnare |
| storia `0014` (spiegazione del punteggio) | l'esportazione **riusa** la spiegazione, non ne costruisce una seconda: due spiegazioni divergenti sarebbero il difetto peggiore possibile qui |
| storie `0024`, `0026`, `0027`, `0029` | sono le tabelle nate dopo la stesura della descrizione, che qui vanno tutte coperte |
| punto aperto n. 4 della [descrizione](../application-description.md) | base giuridica, informativa al cliente finale e valutazione d'impatto: li chiude la revisione legale, l'app li tratta come parametri |

## 7. Fuori ambito

- l'**informativa** che il nostro cliente deve dare ai **suoi** clienti sulla profilazione: è un documento suo, non
  nostro. L'app fornisce la materia (che cosa tratta, con che logica), non il testo;
- la **procedura** di risposta alla richiesta dell'interessato (chi risponde, in quanti giorni, con quale modulo):
  è di piattaforma;
- la cancellazione dei dati **presso le applicazioni sorgenti**: quei fatti stanno anche là, con i loro termini, e
  la richiesta va portata a loro. RenewGrove cancella la **propria** copia e dice che quel trattamento esiste
  altrove;
- la **valutazione d'impatto sulla protezione dei dati**: è dovuta con ogni probabilità (§6 della descrizione) e non
  la scrive un'app;
- la conservazione a termine (24 mesi proposti): è un parametro che dipende dalla revisione legale, punto aperto
  n. 9.

## 8. Punti aperti

- **Se un aggregato su numeri molto piccoli resti riconducibile a una persona.** La scelta del **RF-5** — i periodi
  chiusi sopravvivono perché contengono solo conteggi — è quella proposta al §6 della
  [descrizione](../application-description.md) e vi è scritto «va confermata». In un account con quattro rapporti
  sorvegliati, «1 perso su 4 nel trimestre» può dire più di quanto sembri. Chiude: **sviluppatore** (dati personali)
  con la **revisione legale** ([docs/_REVISIONE-LEGALE.md](../../../../_REVISIONE-LEGALE.md)).
- **Il nome di un piano di intervento come possibile dato personale** (punto aperto n. 8 della descrizione): un
  cliente che battezzasse un piano «piano Mario Rossi» metterebbe un nome in una tabella non esportata. Il presidio
  proposto è un avviso a schermo (`0018`); se basti non lo decide questa storia. Chiude: **sviluppatore** —
  classificazione dei dati personali.
- **Quanto a lungo resta consultabile la spiegazione di un punteggio dopo l'archiviazione del rapporto.** Serve a
  rispondere a un reclamo tardivo, ma allunga la vita di un giudizio su una persona. Va deciso insieme al termine di
  conservazione del punto aperto n. 9. Chiude: **revisione legale**, poi lo sviluppatore.
