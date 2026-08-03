# 0015 — Diagnosi degli errori

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 03 — Documento canonico e validazione
**Storia**: `0015` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`, `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile amministrativo senza competenze fiscali
> voglio leggere «il codice destinatario del tuo cliente non è valido: chiediglielo e correggilo qui» invece di
> «errore 00311»
> così da risolvere il problema da sola, senza chiamare il commercialista e senza cercare su un forum.

**Contesto.** Questa è, letteralmente, la funzione per cui il cliente paga. Le autorità e le reti restituiscono
codici: il Sistema di Interscambio numeri di quattro cifre, la rete a quattro angoli codici di regola, i
validatori di formato messaggi tecnici. L'analisi delle aspettative del segmento micro (descrizione
dell'applicazione §2.5) dice che le uniche due domande sono «è andata?» e «se no, perché?»: la prima la risolve lo
stato, la seconda la risolve questa storia. Va fatta prima della trasmissione vera, così che il primo scarto reale
arrivi già tradotto.

## 2. Requisiti funzionali

1. **RF-1** — Esiste un dizionario che mappa i codici di errore delle autorità e delle reti in: **causa** in lingua
   comune, **rimedio** operativo, e **dove** intervenire nel documento o nell'anagrafica.
2. **RF-2** — Il dizionario è **dati versionati**, appartiene alla giurisdizione, e si aggiorna senza ricompilare.
3. **RF-3** — Un codice **non presente** nel dizionario non fa perdere l'informazione: si mostra il codice
   originale con il testo grezzo dell'autorità e un invito a contattare l'assistenza, mai una pagina vuota.
4. **RF-4** — Ogni diagnosi propone, quando è possibile, un'**azione diretta**: «apri la controparte», «correggi la
   riga 3», «riemetti come nota di credito».
5. **RF-5** — La stessa traduzione serve **sia** le violazioni della validazione preventiva (storia `0014`) **sia**
   gli esiti negativi ricevuti dopo la trasmissione: un solo dizionario, due sorgenti.
6. **RF-6** — Le diagnosi non tradotte vengono conteggiate, così da sapere quali codici mancano nel dizionario
   senza che nessuno debba segnalarlo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il dizionario è comune; la diagnosi è legata a un documento e filtra per
  `tenant_id` preso dal token verificato. Prova di isolamento su due account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/einvoicing/v1/documents/{id}/diagnosis` che
  restituisce le diagnosi ordinate per gravità; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V12__diagnosis_dictionary.sql`: tabella `diagnosis_entry` (comune,
  senza `tenant_id`, con codice, giurisdizione, versione, testi) e conteggio dei codici non tradotti. Chiave UUID
  versione 7, colonne di controllo.
- **RT-4 — Modulo frontend (§3, §5).** Sulla scheda del documento, il riquadro «Cosa non va» in testa — non in
  fondo — con causa, rimedio e il pulsante dell'azione diretta. Solo token del sistema di design; tema chiaro e
  scuro.
- **RT-5 — Cinque lingue (§4).** ⚠️ È la storia con il carico di traduzione più alto e più delicato dell'app:
  cause e rimedi passano dallo spazio-nomi `einvoicing` e sono presenti in `en, it, fr, es, de`. Un rimedio
  tradotto male qui è peggio di nessun rimedio, perché manda l'utente a correggere la cosa sbagliata.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: leggere una diagnosi non costa nulla, ed è la cosa
  che il cliente fa quando è già in difficoltà. Disponibile anche in modalità prova.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `explain_rejection(id) → codice originale, causa, rimedio, azione suggerita`, marcato **lettura**, nessuna
  conferma. È lo strumento che giustifica l'app dal lato conversazionale: «perché è stata scartata la fattura
  214?» è la domanda tipica. Contratto dentro il servizio; server conversazionale non implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** La diagnosi **può citare valori del documento** («il codice fiscale indicato non
  è coerente col paese»): se viene memorizzata, va dichiarata nel manifesto e inserita in `exportData` e
  `purgeData` insieme a `validation_outcome`. La proposta è **non memorizzarla**: si calcola al momento dalla
  violazione e dal dizionario, così non nasce una tabella nuova con dati personali dentro.
- **RT-9 — Registrazione eventi (§14).** L'evento `codice non tradotto incontrato` è registrato con `tenant_id`,
  `app_id`, `user_id`, identificativo di correlazione e **il solo codice**: nessun valore del documento.

## 4. Criteri di accettazione

**CA-1 — Codice tradotto**
- **Dato** un documento respinto con un codice presente nel dizionario
- **Quando** si apre la scheda del documento
- **Allora** in testa si legge la causa in lingua comune, il rimedio e il pulsante che porta dove intervenire

**CA-2 — Codice sconosciuto**
- **Dato** un documento respinto con un codice non presente nel dizionario
- **Quando** si apre la scheda
- **Allora** si vedono il codice originale e il testo grezzo dell'autorità, con l'invito a contattare
  l'assistenza — e l'evento è conteggiato

**CA-3 — Una sola traduzione per due sorgenti**
- **Dato** la stessa condizione rilevata una volta dalla validazione preventiva e una volta dall'esito
  dell'autorità
- **Quando** si leggono le due diagnosi
- **Allora** causa e rimedio sono gli stessi testi, non due formulazioni diverse

**CA-4 — Cinque lingue**
- **Dato** l'interfaccia in ciascuna delle cinque lingue
- **Quando** si apre una diagnosi
- **Allora** causa e rimedio sono tradotti, senza chiavi grezze e senza ricadute sull'inglese

**CA-5 — Isolamento fra account**
- **Dato** due account con documenti respinti
- **Quando** un utente dell'uno chiede la diagnosi di un documento dell'altro
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend);
- [ ] prove di **unità** sulla ricerca nel dizionario, sulla ricaduta per codice sconosciuto e sull'unicità dei
      testi fra le due sorgenti; **integrazione** sulla rotta;
- [ ] prova di **isolamento fra account**;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-EINVOICING]` (storia `0030`) attraverserà uno scarto e
      la lettura della sua diagnosi, che è il passo con più valore dell'intero percorso;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per ogni voce del dizionario introdotta;
- [ ] **manifesto dei dati**: nessuna tabella nuova se la diagnosi non viene memorizzata — e la scelta va scritta;
- [ ] controllo automatico di **accessibilità** sul riquadro di diagnosi;
- [ ] **registro delle decisioni** compilato, con la scelta «dizionario come dati, diagnosi non memorizzata» e i
      motivi;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `explain_rejection`.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0011` | Serve il documento a cui appendere la diagnosi |
| `0014` | Le violazioni della validazione preventiva sono una delle due sorgenti da tradurre |

## 7. Fuori ambito

- La **correzione automatica** del documento: qui si dice cosa fare, non si fa. La correzione guidata dopo uno
  scarto è la storia `0020`.
- Il dizionario completo dei codici di ogni autorità: si parte dai codici più frequenti dell'Italia e della rete a
  quattro angoli, e il conteggio dei non tradotti dice quali aggiungere dopo. È un lavoro continuo, non finito.

## 8. Punti aperti

- **Chi scrive i testi di causa e rimedio.** Sono testi di prodotto con effetti pratici: scritti male mandano
  l'utente nella direzione sbagliata. Non è lavoro che un motore possa generare da solo dal codice numerico, e va
  messo in conto come costo redazionale ricorrente.
- **Fin dove spingersi con il rimedio** senza fare consulenza fiscale — stesso punto della storia `0010`. La
  proposta è restare su azioni sul documento e sull'anagrafica, mai su interpretazioni di norma.
