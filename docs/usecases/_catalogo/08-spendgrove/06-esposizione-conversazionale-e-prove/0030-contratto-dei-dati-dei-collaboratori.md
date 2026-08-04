# 0030 — Contratto dei dati dei collaboratori

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0026`, `0029`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come collaboratore che ha lasciato l'azienda
> voglio poter avere copia di quello che l'app sa di me e chiedere che venga cancellato ciò che non è più necessario
> così da non lasciare in giro per anni la mappa di dove sono stato e di quanto ho speso.

**Contesto.** È la storia che rende l'app conforme e insieme la più difficile da scrivere onestamente, perché
contiene un conflitto vero: il collaboratore ha diritto alla cancellazione, e l'azienda ha l'obbligo di conservare
dieci anni i giustificativi contabili. Va fatta **alla fine**, quando tutte le tabelle esistono: il difetto di
conformità più probabile in un'app nuova è dimenticarne una, e non si può verificare di averle tutte finché non ci
sono tutte. La candidata a essere dimenticata, qui, è nota in anticipo: **l'immagine della ricevuta**, che non è
una riga di tabella.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio implementa il contratto dati di piattaforma con `appId()`, `exportData(ambito)`,
   `purgeData(ambito)` e `manifest()`, sotto il nome convenzionale `NoteSpeseDataContract`.
2. **RF-2** — L'esportazione restituisce **tutti** i dati riferibili alla persona indicata, in un formato leggibile,
   comprese **le immagini dei giustificativi** e i contenuti dei pacchetti che la riguardano.
3. **RF-3** — La cancellazione è **fisica** e comprende gli oggetti archiviati: sostituire il nome con un codice non
   è cancellare. Ogni purga lascia una riga di prova nel registro delle purghe.
4. **RF-4** — Ciò che ricade sotto un obbligo di legge di conservazione **non si cancella prima della scadenza**:
   l'esito della purga dice con precisione che cosa è stato cancellato, che cosa è stato trattenuto, per quale
   obbligo e fino a quando.
5. **RF-5** — Esportazione e cancellazione **restano accessibili anche quando l'app è disabilitata o l'abbonamento è
   scaduto**: sono diritti dell'interessato, non funzioni del prodotto.
6. **RF-6** — Il manifesto dei dati è completo e coerente con la realtà del database: ogni tabella che contiene dati
   di persone compare nel manifesto, in `exportData` e in `purgeData`.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni operazione filtra per `tenant_id` preso dal token verificato; una
  richiesta relativa a una persona di un altro account non trova nulla. L'ambito della purga è verificato due volte,
  perché una cancellazione fisica sbagliata non si annulla.
- **RT-2 — Interfaccia di programmazione (§2).** Il contratto è invocato dagli strumenti di piattaforma per i
  diritti dell'interessato; le rotte dell'app non ne espongono una seconda strada. Errori in
  `application/problem+json`; definizione OpenAPI aggiornata dove serve.
- **RT-3 — Persistenza (§8).** Nessuna tabella di dominio nuova. La purga tocca **tutte** quelle esistenti:
  `collaboratore`, `ricevuta` (con l'oggetto archiviato), `esito_lettura`, `correzione_campo`, `spesa`,
  `voce_imposta`, `trasferta`, `percorrenza_veicolo`, `nota_spese`, `rimborso`, `movimento_carta`,
  `sforamento_politica`, `pacchetto_esportazione`, `versamento_conservazione`. La cancellazione fisica **non** usa
  `deleted_at`: quella è la cancellazione logica del dominio, un'altra cosa.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova nel modulo: i diritti dell'interessato si esercitano
  dalla sezione di piattaforma «I miei dati».
- **RT-5 — Cinque lingue (§4).** Nessuna stringa visibile nuova nel modulo. Il **manifesto** vuole invece
  **italiano e inglese** su ogni testo: sono due elenchi diversi e confonderli è un errore classico.
- **RT-6 — Varchi e quota (§6, §7).** Le operazioni non consumano quota e **non** sono soggette al varco
  dell'abbonamento: con `canceled` continuano a funzionare.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: esportare o cancellare i dati di una persona non
  è un'azione da conversazione, nemmeno con conferma.
- **RT-8 — Dati personali (§10).** È la storia che chiude il capitolo. Verifica finale obbligatoria: **ogni** campo
  annotato `@PersonalData` è dichiarato nel manifesto (il contrario fa fallire la compilazione), e **ogni** tabella
  del manifesto compare in esportazione e cancellazione. Va verificato in modo automatico, non a occhio: una prova
  che confronta l'elenco delle tabelle con dati personali e l'elenco toccato dalla purga.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `esportazione prodotta`, `purga eseguita` portano `tenant_id`,
  `app_id`, `user_id`, identificativo di correlazione, ambito e **conteggi per tabella** — mai i dati cancellati.
  La riga di prova nel registro delle purghe è obbligatoria.

## 4. Criteri di accettazione

**CA-1 — Esportazione completa**
- **Dato** un collaboratore con spese, ricevute, trasferte, percorrenze, note e un rimborso
- **Quando** si esporta il suo ambito
- **Allora** il risultato contiene dati da **tutte** le tabelle che lo riguardano **e** le immagini dei suoi
  giustificativi

**CA-2 — Cancellazione fisica**
- **Dato** un collaboratore i cui dati non ricadono sotto obblighi di conservazione ancora vigenti
- **Quando** si esegue la purga
- **Allora** nessuna riga e **nessun file** che lo riguarda resta recuperabile, e nel registro delle purghe compare
  la riga di prova con i conteggi

**CA-3 — Conflitto con l'obbligo di conservazione**
- **Dato** un ex collaboratore con giustificativi dell'esercizio in corso
- **Quando** si esegue la purga
- **Allora** l'esito distingue con precisione ciò che è stato cancellato da ciò che resta, indicando l'obbligo e la
  data di scadenza — e **non** cancella ciò che va conservato

**CA-4 — Diritti accessibili senza abbonamento**
- **Dato** un account con abbonamento `canceled`
- **Quando** si richiede l'esportazione dei dati
- **Allora** l'operazione riesce, mentre le rotte ordinarie del dominio rispondono `402`

**CA-5 — Nessuna tabella dimenticata**
- **Dato** lo schema del database e il manifesto
- **Quando** gira il controllo automatico
- **Allora** ogni tabella con dati personali risulta presente sia nel manifesto sia nelle due operazioni; la prova
  **fallisce** se qualcuno aggiunge una tabella e dimentica di dichiararla

**CA-6 — Isolamento fra account**
- **Dato** due account con collaboratori omonimi · **Quando** si purga l'ambito di uno
- **Allora** i dati dell'altro restano intatti

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sulla risoluzione dell'ambito e sulla regola di trattenimento per obbligo di legge; di
      **integrazione** su esportazione e purga con database effimero, migrazioni vere e archivio simulato,
      **verificando che i file non esistano più**;
- [ ] prova di **isolamento fra account** su esportazione e purga, con collaboratori omonimi;
- [ ] prova **strutturale** che confronta le tabelle con dati personali, il manifesto, `exportData` e `purgeData`:
      è il presidio che impedisce di dimenticarne una in futuro;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, che nel percorso `[J-NOTESPESE]` esercita esportazione e
      cancellazione a valle del ciclo; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato lì;
- [ ] **traduzioni**: nessuna stringa visibile nuova nel modulo;
- [ ] **manifesto dei dati** completo, coerente e in **italiano e inglese** su ogni testo;
- [ ] **registro delle decisioni** compilato, con la regola di trattenimento e la sua motivazione;
- [ ] contratto degli **strumenti conversazionali**: nessuno, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0026` | La ritenzione decennale e i pacchetti di versamento sono l'altra faccia del conflitto con la cancellazione |
| `0029` | Anche le bozze prodotte dall'assistente sono dati personali da esportare e cancellare |
| Contratto dati di piattaforma (`AppDataContract`) | È l'interfaccia che questa storia realizza |

## 7. Fuori ambito

- L'interfaccia con cui l'interessato esercita i suoi diritti: è di piattaforma, sezione «I miei dati».
- La chiusura dell'account e la cancellazione di tutti i suoi dati: è di piattaforma; l'app fornisce il contratto
  che verrà invocato.
- La rettifica dei dati inesatti: si esercita usando l'app, che è già modificabile.

## 8. Punti aperti

- 🛑 **La formulazione precisa della regola di trattenimento** — che cosa esattamente resta e per quale obbligo,
  quando un ex collaboratore chiede la cancellazione — **non la decide questa storia**: è il punto aperto n. 7
  della descrizione dell'applicazione, e va chiuso dallo sviluppatore con verifica legale. La storia implementa il
  meccanismo; il contenuto della regola è configurazione, non codice.
- **Che cosa fare del pacchetto già consegnato al commercialista**: quello è uscito dalla nostra infrastruttura e
  non lo controlliamo. Va detto all'interessato nella risposta, non taciuto.
