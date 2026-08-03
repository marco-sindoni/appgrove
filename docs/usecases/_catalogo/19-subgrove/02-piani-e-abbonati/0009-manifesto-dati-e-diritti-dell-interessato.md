# 0009 — Manifesto dati e diritti dell'interessato

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 02 — Piani e abbonati
**Storia**: `0009` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare a cui un iscritto ha appena chiesto «cancellatemi tutto»
> voglio che l'app sappia esattamente dove stanno i suoi dati e sappia toglierli davvero
> così da poter rispondere in tempo e senza dimenticare una tabella per strada.

**Contesto.** Il manifesto dei dati è la fonte unica da cui si generano il registro dei trattamenti e gli
strumenti di esportazione e cancellazione: **un campo non dichiarato è un campo che l'esportazione dimentica e la
cancellazione lascia indietro**. In SubGrove il rischio è più alto della media, per una ragione strutturale: i
dati riferiti a persone non stanno in una tabella sola, ma in **sette** — l'abbonato, l'abbonamento, il mandato,
il sollecito, l'avviso di rinnovo, la richiesta dal portale e, indirettamente, la scadenza. Dimenticarne una è il
difetto di conformità più probabile di questa app, ed è per questo che la storia esiste come storia a sé e non
come voce in coda a un'altra. Va implementata **subito dopo** la `0008`: campi annotati e non dichiarati fanno
fallire la compilazione, ed è giusto così.

## 2. Requisiti funzionali

1. **RF-1** — Esiste `docs/compliance/manifests/abbonati.yaml`, con tutte le voci in **italiano e inglese**:
   dove vive il dato, di chi è, che genere di dato è, a cosa serve, perché è lecito trattarlo, per quanto si
   tiene.
2. **RF-2** — Il manifesto dichiara esplicitamente le **esclusioni**: nessun numero di carta, nessuna coordinata
   bancaria completa, nessun documento sanitario, nessuna fotografia, nessun dato di accesso fisico.
3. **RF-3** — Il contratto dati dell'app (`AbbonatiDataContract`) implementa `appId()`, `exportData(scope)`,
   `purgeData(scope)` e `manifest()`, e copre **tutte** le tabelle con dati riferiti a persone.
4. **RF-4** — L'esportazione produce un file leggibile che comprende, per l'interessato richiesto, anagrafica,
   abbonamenti, scadenze, mandati, solleciti ricevuti, avvisi ricevuti e richieste inviate.
5. **RF-5** — La cancellazione è **fisica** e lascia una riga di prova nel registro delle purghe: sostituire i
   nomi con dei codici non è cancellare.
6. **RF-6** — Esportazione e cancellazione restano accessibili **anche** con app disabilitata o abbonamento di
   piattaforma scaduto: sono diritti, non funzioni.

## 3. Requisiti tecnici

- **RT-1 — Dati personali (§10).** Manifesto con **italiano e inglese obbligatori** su ogni testo; ogni campo
  Java annotato `@PersonalData` e dichiarato; ogni tabella con dati di persone presente **sia** in `exportData`
  **sia** in `purgeData`. Il controllo automatico annotazione↔manifesto gira nelle prove del backend e deve
  essere verde.
- **RT-2 — Diritti esenti dai varchi (§13).** Esportazione e cancellazione **non** passano dal varco
  dell'abilitazione né da quello dell'abbonamento: sono capacità di piattaforma che invocano internamente il
  contratto dell'app.
- **RT-3 — Isolamento fra account (§1).** Esportazione e cancellazione operano dentro un solo account: nessun
  percorso può toccare dati di un altro.
- **RT-4 — Persistenza (§8).** La cancellazione fisica è l'**unica** eccezione alla cancellazione logica, e vale
  solo per i diritti dell'interessato e la chiusura dell'account.
- **RT-5 — Conformità (§10).** Il registro dei trattamenti si rigenera dal manifesto; la parità delle due lingue
  e la freschezza del registro sono controllate dall'area `compliance` di `run-tests.sh`.
- **RT-6 — Registrazione eventi (§14).** `esportazione eseguita`, `purga eseguita (quante righe per tabella)`,
  con `tenant_id`, `app_id`, `user_id` e correlazione, **senza** nomi.
- **RT-7 — Prove (§11).** Prova che ogni tabella con dati di persone compare in entrambe le operazioni: è la
  prova che impedisce il difetto tipico. Prova che dopo la purga non resta alcuna riga con quei dati.

## 4. Criteri di accettazione

**CA-1 — Nessuna tabella dimenticata**
- **Dato** l'elenco delle tabelle con campi annotati come dati personali
- **Quando** si esegue il controllo automatico
- **Allora** ognuna compare nel manifesto, in `exportData` e in `purgeData`; se ne manca una la compilazione
  fallisce

**CA-2 — Esportazione completa**
- **Dato** un abbonato con due abbonamenti, sei scadenze, un mandato e tre solleciti ricevuti
- **Quando** si esporta il suo fascicolo
- **Allora** ci sono tutte e quattro le categorie, e nessuna è vuota per errore

**CA-3 — Cancellazione vera**
- **Dato** lo stesso abbonato · **Quando** si esegue la purga
- **Allora** nessuna riga contiene più i suoi dati in nessuna delle sette tabelle, e il registro delle purghe
  porta la riga di prova

**CA-4 — Diritti accessibili con abbonamento scaduto**
- **Dato** un account con abbonamento di piattaforma `canceled`
- **Quando** chiede l'esportazione dei dati
- **Allora** l'esportazione funziona, mentre le funzioni di business rispondono `402`

**CA-5 — Due lingue nel manifesto**
- **Dato** il manifesto · **Quando** gira il controllo di parità delle lingue
- **Allora** ogni testo ha italiano e inglese, e la suite `compliance` è verde

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `compliance`);
- [ ] prove di **unità** sul contratto dati e di **integrazione** su esportazione e purga con database effimero;
- [ ] prova di **isolamento fra account** su esportazione e purga;
- [ ] **prova end-to-end**: *nessun impatto* — la superficie è di piattaforma, non dell'app;
- [ ] **traduzioni**: il manifesto vuole **due** lingue (italiano e inglese), l'interfaccia ne vuole cinque: sono
      elenchi diversi e non vanno confusi;
- [ ] **manifesto dei dati** compilato per intero, con le esclusioni dichiarate;
- [ ] **registro delle decisioni** compilato: sette tabelle coinvolte, base giuridica per ciascuna categoria,
      esclusioni volute;
- [ ] registro dei trattamenti rigenerato nello stesso commit.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0008` | i campi da dichiarare nascono lì |
| decisione dello sviluppatore sulla classificazione (§6 della descrizione) | il manifesto si compila **insieme** a lui: «niente contratto, niente produzione» |

## 7. Fuori ambito

- le voci relative a tabelle che ancora non esistono (mandato, sollecito, avviso, richiesta): **ogni storia che
  le crea aggiunge le proprie voci**, ed è scritto nei loro requisiti tecnici. Questa storia mette in piedi
  l'impianto e copre ciò che esiste al momento;
- l'informativa verso gli abbonati, che è un documento del **cliente** verso i suoi clienti, non nostro.

## 8. Punti aperti

**Per quanto si conservano le prove di invio.** Avvisi di rinnovo e solleciti servono come **prova** in caso di
contestazione, quindi vanno tenuti oltre la fine del rapporto; quanto oltre dipende dalla prescrizione dei diritti
nascenti dal contratto, che non sono in grado di quantificare. **Proposta**: parametro per account con un valore
predefinito prudente, e la domanda portata alla revisione legale (punto aperto n. 8 della descrizione). Chiude:
revisione legale, poi lo sviluppatore.

**Chi è titolare e chi responsabile.** Per i dati degli abbonati il titolare è il **cliente**; appgrove tratta per
suo conto. È la stessa impostazione delle altre app che toccano i clienti del cliente, ma qui va scritta nero su
bianco nel manifesto, perché tocca comunicazioni verso terzi. Chiude: revisione legale.
