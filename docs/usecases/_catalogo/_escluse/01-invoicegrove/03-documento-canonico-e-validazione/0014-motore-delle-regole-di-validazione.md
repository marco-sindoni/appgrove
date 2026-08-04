# 0014 — Motore delle regole di validazione

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 03 — Documento canonico e validazione
**Storia**: `0014` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0011`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile amministrativo
> voglio sapere **prima** di trasmettere se il documento passerà i controlli del paese di destinazione
> così da correggerlo adesso, che costa un minuto, invece che dopo lo scarto, che costa tre giorni.

**Contesto.** Il controllo prima della trasmissione è la voce «controllo pre-clearance» della scheda di catalogo
ed è la funzione che sposta il valore percepito dell'app dal trasporto (commodity a sette centesimi) alla
conformità. Le regole appartengono alla giurisdizione e alla sua versione (storia `0006`): scriverle come
condizioni nel codice significherebbe dover ricompilare a ogni cambio normativo, in un dominio dove le norme
cambiano ogni anno.

⚠️ **Questa storia porta anche il presidio contro le categorie particolari.** Il divieto italiano di emettere
fattura elettronica via Sistema di Interscambio per prestazioni sanitarie verso persone fisiche è **permanente**
dal decreto legislativo 81/2025 (descrizione dell'applicazione §2.3 e §6). È una regola di validazione bloccante,
ed è il motivo per cui l'app resta fuori dall'articolo 9: senza questa regola, non ci resta.

## 2. Requisiti funzionali

1. **RF-1** — Esiste un motore che applica a un `CanonicalDocument` l'insieme di regole della sua giurisdizione,
   nella versione valida alla data del documento.
2. **RF-2** — Ogni regola ha: codice stabile, **gravità** (`blocca` oppure `avverte`), messaggio in lingua comune
   nelle cinque lingue, riferimento normativo, e la posizione nel documento a cui si riferisce.
3. **RF-3** — L'esito della validazione elenca **tutte** le violazioni, non la prima: chi corregge vuole vedere
   tutto in una volta.
4. **RF-4** — Una violazione `blocca` impedisce la trasmissione; una violazione `avverte` la consente ma resta
   registrata sull'esito.
5. **RF-5** — Fra le regole italiane bloccanti c'è il **divieto sanitario**: un documento marcato come prestazione
   sanitaria verso persona fisica è respinto, con il riferimento normativo e la spiegazione che va emesso in
   formato non elettronico.
6. **RF-6** — L'esito è memorizzato con la **versione delle regole** usata, così che fra tre anni si sappia con
   quali regole un documento fu validato.
7. **RF-7** — La validazione si può richiedere in qualunque momento su un documento in stato `bozza`, senza
   trasmettere nulla.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il documento validato e l'esito filtrano per `tenant_id` preso dal token
  verificato. Prova di isolamento su due account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/einvoicing/v1/documents/{id}/validate` che
  **non** ha effetti verso l'esterno; errori in `application/problem+json`; definizione OpenAPI aggiornata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V11__validation.sql`: tabelle `validation_rule` (comune, senza
  `tenant_id`, come il registro delle giurisdizioni) e `validation_outcome` (con `tenant_id`, chiave UUID versione
  7, colonne di controllo). Le regole si aggiornano con una migrazione, non da interfaccia.
- **RT-4 — Modulo frontend (§3, §5).** Sulla scheda del documento, un riquadro «Controlli» con le violazioni
  raggruppate per gravità e un pulsante «controlla adesso». Solo token del sistema di design; le due gravità usano
  i colori funzionali; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I messaggi delle regole passano dallo spazio-nomi `einvoicing` e sono presenti in
  `en, it, fr, es, de`. Sono testi normativi tradotti: la storia non è conclusa se ne manca uno, e una traduzione
  approssimativa qui produce un errore che il cliente non può correggere.
- **RT-6 — Varchi e quota (§6, §7).** ⚠️ La validazione **non** consuma la metrica `documenti` ed è disponibile
  **anche in modalità prova**: è la funzione che si può mostrare gratuitamente senza effetti verso l'esterno
  (descrizione dell'applicazione §5). Va però protetta da un limite di frequenza per account.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `validate_before_send(id) → esito, violazioni con gravità e rimedio`, marcato **lettura** perché non produce
  effetti; nessuna conferma umana. È uno degli strumenti che rendono l'app utile da una chat. Contratto dentro il
  servizio; server conversazionale non implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** L'esito di validazione **cita posizioni e valori del documento**, quindi può
  contenere dati personali (per esempio «il codice fiscale della controparte non è coerente col paese»): la
  tabella `validation_outcome` va dichiarata nel manifesto in italiano e inglese e inserita in `exportData` e
  `purgeData`. È una delle tabelle che si dimenticano più facilmente perché «è solo un log»: non lo è.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `validazione eseguita`, `violazione bloccante rilevata` sono
  registrati con `tenant_id`, `app_id`, `user_id`, identificativo di correlazione, codice della regola e versione,
  **senza** i valori del documento.

## 4. Criteri di accettazione

**CA-1 — Documento conforme**
- **Dato** un documento italiano completo e coerente
- **Quando** si chiede la validazione
- **Allora** l'esito è positivo, senza violazioni, e riporta la versione delle regole usata

**CA-2 — Più violazioni insieme**
- **Dato** un documento a cui mancano il recapito della controparte e la natura dell'operazione su una riga esente
- **Quando** si chiede la validazione
- **Allora** l'esito elenca **entrambe** le violazioni, ciascuna con gravità, posizione e rimedio

**CA-3 — Divieto sanitario italiano**
- **Dato** un documento italiano verso una persona fisica, marcato come prestazione sanitaria
- **Quando** si chiede la validazione
- **Allora** l'esito contiene una violazione **bloccante** con il riferimento normativo e la spiegazione che il
  documento va emesso in formato non elettronico, e la trasmissione risulta impedita

**CA-4 — Avvertimento non bloccante**
- **Dato** un documento con una violazione di sola gravità `avverte`
- **Quando** si chiede la validazione
- **Allora** l'esito segnala la violazione ma la trasmissione resta possibile

**CA-5 — Versione delle regole**
- **Dato** un documento con data anteriore all'entrata in vigore di una nuova versione delle regole
- **Quando** lo si valida
- **Allora** viene applicata la versione valida a quella data, non l'ultima disponibile

**CA-6 — Isolamento fra account**
- **Dato** due account con documenti propri
- **Quando** un utente dell'uno chiede la validazione di un documento dell'altro
- **Allora** riceve `404`

**CA-7 — Validazione in modalità prova**
- **Dato** un account in stato `trialing`
- **Quando** chiede la validazione di un documento
- **Allora** la validazione riesce e restituisce l'esito completo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** su ciascuna regola introdotta — **compresa quella del divieto sanitario, che va coperta
      con un caso proprio** — e di **integrazione** sulla rotta di validazione;
- [ ] prova di **isolamento fra account** su `validation_outcome`;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-EINVOICING]` (storia `0030`) attraverserà una
      validazione fallita e la successiva correzione;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per **ogni** messaggio di regola;
- [ ] **manifesto dei dati** aggiornato: `validation_outcome` dichiarata, presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta «regole come dati versionati» e con la **regola del
      divieto sanitario dichiarata come presidio contro le categorie particolari**;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `validate_before_send`.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | Le regole appartengono al profilo della giurisdizione e alla sua versione |
| `0011` | Serve il documento canonico da validare |

## 7. Fuori ambito

- La **traduzione degli errori dell'autorità** ricevuti dopo la trasmissione: è la storia `0015`. Qui si
  controllano le regole **prima**; là si spiegano gli esiti **dopo**.
- L'insieme completo delle regole di ogni paese: si parte da Italia e dal profilo europeo comune della rete a
  quattro angoli. Le altre arrivano con le rispettive giurisdizioni.
- La rilevazione automatica di contenuti sensibili nelle descrizioni di riga: **deliberatamente esclusa**. L'app
  non fa rilevazione di contenuto; il presidio è la regola sul tipo di operazione, non l'analisi del testo.

## 8. Punti aperti

- 🛑 **La conferma che l'app non tratta categorie particolari dipende da questa regola.** Se lo sviluppatore
  decidesse di **non** implementare il presidio, o di consentire una deroga, la classificazione dei dati personali
  cambia: servono base giuridica rafforzata e valutazione d'impatto (descrizione dell'applicazione §6). È una
  fermata di escalation, non un dettaglio di implementazione.
- **Come si marca un documento come "prestazione sanitaria"**: serve un campo esplicito sul documento o si deduce
  dalla natura dell'operazione? Dedurlo è fragile; chiederlo è un campo in più che nessun altro paese usa. Va
  deciso con lo sviluppatore.
- **Chi mantiene le regole nel tempo.** Un motore di regole senza qualcuno che le aggiorna diventa un motore di
  regole sbagliate. È un impegno operativo continuo, non un costo di sviluppo iniziale.
