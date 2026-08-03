# 0022 — Pacchetto di conservazione

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 05 — Conservazione a norma e adempimenti
**Storia**: `0022` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0016`, `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una micro-impresa
> voglio che i miei documenti finiscano in conservazione a norma senza che io debba fare nulla
> così da poterli esibire fra otto anni con lo stesso valore dell'originale, se qualcuno me li chiede.

**Contesto.** «Conservare» non è «archiviare»: salvare un file su un disco non è conservazione a norma. Le linee
guida italiane pretendono metadati obbligatori, firma del pacchetto di versamento, marca temporale, un manuale di
conservazione e un responsabile nominato, per **dieci anni** (descrizione dell'applicazione §2.3). È il tratto in
cui l'app smette di essere un convertitore e diventa un adempimento: è anche, non a caso, ciò che i concorrenti
italiani includono in tutti i piani.

Questa storia compone il pacchetto; la storia `0023` lo consegna al conservatore qualificato e ne raccoglie la
ricevuta. Sono separate perché la prima è nostra e la seconda dipende da un fornitore.

## 2. Requisiti funzionali

1. **RF-1** — Quando un documento raggiunge uno stato **definitivo** (accettato, consegnato, oppure passivo
   acquisito), il servizio compone il pacchetto di versamento: artefatto ufficiale, notifiche del ciclo di vita, e
   indice dei metadati obbligatori.
2. **RF-2** — L'indice dei metadati contiene almeno: identificativo del documento, soggetto emittente, controparte,
   data, tipo, importo, impronta crittografica dell'artefatto, riferimento al ciclo di vita.
3. **RF-3** — I metadati obbligatori sono **dichiarati nel profilo della giurisdizione** (storia `0006`), non
   scritti nel codice: cambiano con la norma.
4. **RF-4** — Se un metadato obbligatorio manca, il pacchetto **non** si compone: il documento resta in attesa con
   l'indicazione di cosa manca, e l'utente lo vede.
5. **RF-5** — Il pacchetto è **deterministico**: comporlo due volte dallo stesso documento produce la stessa
   impronta.
6. **RF-6** — La composizione avviene come lavorazione differita, non sul percorso della richiesta dell'utente:
   un ritardo del pacchetto non deve rallentare la trasmissione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il pacchetto è legato al documento e filtra per `tenant_id` preso dal
  token verificato; la lavorazione differita porta con sé il `tenant_id` del documento e non lo ricava altrove.
  Prova di isolamento dedicata.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta di sola lettura
  `GET /api/einvoicing/v1/documents/{id}/archive` che restituisce stato e metadati del pacchetto; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V18__archive_record.sql`: tabella `archive_record` con stato,
  riferimento al pacchetto, impronta, indice dei metadati, scadenza decennale; `tenant_id`, chiave UUID versione
  7, colonne di controllo. ⚠️ **Deroga dichiarata**: `archive_record` **non** ha cancellazione logica e i suoi
  contenuti non si cancellano su richiesta — c'è un obbligo di legge decennale che prevale
  (descrizione dell'applicazione §4 e §6). La deroga va scritta nella migrazione, o al primo che la legge sembrerà
  una dimenticanza.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Archivio»: elenco dei documenti conservati e in attesa, con il
  motivo dell'attesa. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Nomi degli stati di conservazione e messaggi di metadato mancante dallo
  spazio-nomi `einvoicing`, presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La composizione del pacchetto **non** consuma la metrica `documenti`: il
  consumo è già avvenuto alla trasmissione o alla ricezione. Contarla di nuovo sarebbe un doppio addebito.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: la composizione è automatica e
  non si comanda. `archive_document` come strumento esplicito compare nella storia `0023`, dove c'è un effetto
  irreversibile verso un fornitore.
- **RT-8 — Dati personali (§10).** **Sì, ed è la voce più delicata dell'intero manifesto.** Il pacchetto contiene
  una copia integrale del documento, quindi tutti i dati personali, e va conservato **dieci anni non riducibili**.
  Voce nel manifesto in italiano e inglese con base giuridica «obbligo di legge» e ritenzione «10 anni, non
  riducibili»; tabella presente in `exportData`; ⚠️ in `purgeData` la tabella **compare ma con esito parziale
  dichiarato**: si cancella ciò che non è coperto dall'obbligo e si risponde all'interessato dichiarando cosa resta
  e fino a quando (storia `0026`).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `pacchetto composto`, `pacchetto in attesa per metadato
  mancante` sono registrati con `tenant_id`, `app_id`, `user_id`, identificativo di correlazione, identificativo
  del documento e impronta — senza metadati che siano dati personali.

## 4. Criteri di accettazione

**CA-1 — Composizione automatica**
- **Dato** un documento che raggiunge lo stato `accettato_dall_autorita`
- **Quando** la lavorazione differita gira
- **Allora** esiste un `archive_record` in stato «pronto per il versamento» con l'indice dei metadati completo

**CA-2 — Metadato obbligatorio mancante**
- **Dato** un documento a cui manca un metadato dichiarato obbligatorio dal profilo della giurisdizione
- **Quando** si tenta la composizione
- **Allora** il pacchetto non si compone, il documento resta in attesa, e la sezione «Archivio» dice quale
  metadato manca

**CA-3 — Determinismo**
- **Dato** lo stesso documento
- **Quando** si compone il pacchetto due volte
- **Allora** l'impronta è identica

**CA-4 — La trasmissione non aspetta il pacchetto**
- **Dato** la lavorazione differita ferma
- **Quando** si trasmette un documento
- **Allora** la trasmissione riesce nei tempi normali e il pacchetto si compone quando la lavorazione riparte

**CA-5 — Documenti passivi**
- **Dato** un documento passivo acquisito
- **Quando** la lavorazione differita gira
- **Allora** anche per lui esiste un pacchetto: l'obbligo decennale vale in entrambe le direzioni

**CA-6 — Isolamento fra account**
- **Dato** due account con documenti conservati
- **Quando** un utente dell'uno chiede il pacchetto di un documento dell'altro
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sulla composizione, sul determinismo dell'impronta e sui metadati obbligatori per
      giurisdizione; **integrazione** sulla lavorazione differita;
- [ ] prova di **isolamento fra account**, compreso il caso della lavorazione differita;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-EINVOICING]` (storia `0030`) arriverà fino alla
      comparsa del documento in «Archivio»;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `archive_record`, ritenzione decennale non riducibile, e con l'esito
      **parziale dichiarato** in cancellazione;
- [ ] **registro delle decisioni** compilato, con la **deroga alla cancellazione logica** e il suo motivo;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, e il motivo è scritto.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0016` | Serve l'artefatto ufficiale serializzato e la sua impronta |
| `0019` | Il pacchetto si compone quando il ciclo di vita raggiunge uno stato definitivo |

## 7. Fuori ambito

- La **consegna** al conservatore qualificato, il sigillo e la marca temporale: storia `0023`.
- Il **manuale di conservazione** e la nomina del responsabile della conservazione: sono adempimenti
  organizzativi, non software. Vanno prodotti, ma non da una storia di sviluppo: annotati come punto aperto.
- L'esibizione e lo scarico: storia `0024`.

## 8. Punti aperti

- 🛑 **Il manuale di conservazione e il responsabile della conservazione sono obbligatori e non li produce il
  codice.** Chi è il responsabile: appgrove, il cliente, o il conservatore qualificato? La risposta cambia il
  contratto e l'informativa. È una fermata di escalation, e va chiusa prima di vendere la funzione.
- **Se il pacchetto debba contenere anche le diagnosi e gli esiti di validazione** oltre alle notifiche. Più
  contenuto significa più prova ma anche più dati personali conservati dieci anni: nel dubbio, la proposta è il
  minimo che la norma richiede.
