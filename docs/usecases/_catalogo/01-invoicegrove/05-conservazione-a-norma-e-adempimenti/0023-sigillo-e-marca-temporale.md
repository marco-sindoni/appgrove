# 0023 — Sigillo e marca temporale

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 05 — Conservazione a norma e adempimenti
**Storia**: `0023` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio che il mio pacchetto sia sigillato e datato da chi ha il titolo per farlo
> così da avere in mano una prova che vale davanti a un controllo, non una mia dichiarazione.

**Contesto.** Il pacchetto composto dalla storia `0022` non ha ancora valore probatorio: glielo dà il sigillo di
un conservatore qualificato e la marca temporale. Non lo possiamo fare noi — non siamo un conservatore accreditato
e non abbiamo intenzione di diventarlo (descrizione dell'applicazione §1, «Cosa NON fa»). La storia è quindi
un'integrazione con effetti **irreversibili**: una volta versato, il pacchetto è in custodia per dieci anni presso
un terzo, e la custodia sopravvive alla fine dell'abbonamento.

## 2. Requisiti funzionali

1. **RF-1** — Il pacchetto pronto viene versato al conservatore qualificato, che restituisce una **ricevuta**
   con il proprio identificativo, il sigillo e la marca temporale.
2. **RF-2** — La ricevuta è conservata insieme all'`archive_record` ed è ciò che si esibisce: senza ricevuta, il
   documento risulta **non conservato**, per quanto il pacchetto esista.
3. **RF-3** — Il versamento è **idempotente**: un secondo tentativo sullo stesso pacchetto non produce un secondo
   versamento.
4. **RF-4** — Se il conservatore non risponde, il pacchetto resta in stato «in versamento» e viene ritentato con
   ritmo crescente; il documento non risulta conservato finché la ricevuta non arriva.
5. **RF-5** — Ogni versamento e ogni esito sono tracciati con chi, quando e con quale esito: è la catena di prova.
6. **RF-6** — Il versamento **non** si esegue in modalità prova e non si esegue mai in ambiente locale: il
   conservatore è simulato come gli altri fornitori (storia `0005`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Pacchetto e ricevuta filtrano per `tenant_id` preso dal token
  verificato; la lavorazione differita porta con sé il `tenant_id`. Prova di isolamento dedicata.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta
  `POST /api/einvoicing/v1/documents/{id}/archive/deposit` con conferma esplicita, per il versamento richiesto a
  mano; il versamento ordinario è automatico. Errori in `application/problem+json`; definizione OpenAPI aggiornata
  nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V19__archive_receipt.sql`: colonne di ricevuta, sigillo, marca
  temporale, identificativo del conservatore e stato del versamento su `archive_record`; tabella dei tentativi.
  `tenant_id`, chiave UUID versione 7, colonne di controllo. Nessuna cancellazione logica: è la catena di prova.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione «Archivio», lo stato di conservazione con tre valori chiari —
  `conservato`, `in versamento`, `non conservato` — e la ricevuta scaricabile. Solo token del sistema di design;
  tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I tre stati e i messaggi di attesa dallo spazio-nomi `einvoicing`, presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Il versamento **non** consuma la metrica `documenti`. ⚠️ **Ma ha un costo
  variabile reale** (il listino pubblico di un fornitore italiano indica circa 10 centesimi a documento per la
  conservazione, o 5 a volume: descrizione dell'applicazione §2.2) e genera una **giacenza decennale**. È il punto
  in cui la metrica a consumo e il costo a giacenza divergono: il rischio è dichiarato nella descrizione
  dell'applicazione §11 e la storia proprietaria è la `0026`.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `archive_document(id, conferma_esplicita) → riferimento del pacchetto conservato`, marcato **scrittura
  irreversibile** con **conferma umana obbligatoria**. Avviare una conservazione decennale presso un terzo non si
  annulla. Il varco è della storia `0029`. Contratto dentro il servizio; server conversazionale non implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Il versamento consegna il pacchetto — quindi **tutti** i dati personali del
  documento — a un **terzo fornitore esterno**, il conservatore qualificato, che è un responsabile del trattamento
  con un rapporto di dieci anni. Va dichiarato nell'elenco dei fornitori e nell'informativa, con verifica che i
  dati stiano a riposo in regioni europee. Le colonne di ricevuta vanno nel manifesto, in `exportData` e — con
  esito parziale dichiarato — in `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `versamento avviato`, `ricevuta acquisita`,
  `versamento fallito`, con `tenant_id`, `app_id`, `user_id`, identificativo di correlazione, identificativo del
  conservatore e impronta — mai il contenuto del pacchetto.

## 4. Criteri di accettazione

**CA-1 — Versamento riuscito**
- **Dato** un pacchetto pronto
- **Quando** la lavorazione lo versa e il conservatore restituisce la ricevuta
- **Allora** lo stato diventa `conservato`, la ricevuta è scaricabile e riporta sigillo e marca temporale

**CA-2 — Conservatore non disponibile**
- **Dato** il conservatore che non risponde
- **Quando** si tenta il versamento
- **Allora** lo stato resta `in versamento`, il tentativo è registrato, il ritentativo è pianificato, e il
  documento **non** risulta conservato

**CA-3 — Secondo tentativo sullo stesso pacchetto**
- **Dato** un pacchetto già versato
- **Quando** si tenta un nuovo versamento
- **Allora** non parte un secondo versamento e viene restituita la ricevuta esistente

**CA-4 — Modalità prova**
- **Dato** un account in stato `trialing`
- **Quando** si tenta un versamento
- **Allora** l'operazione è negata con il messaggio dedicato, e nulla esce

**CA-5 — Senza ricevuta non è conservato**
- **Dato** un pacchetto composto ma mai versato
- **Quando** si guarda la sezione «Archivio»
- **Allora** lo stato è `non conservato`, con la spiegazione, e non «archiviato» in modo ambiguo

**CA-6 — Isolamento fra account**
- **Dato** due account con pacchetti propri
- **Quando** un utente dell'uno tenta il versamento del pacchetto dell'altro
- **Allora** riceve `404` e nulla esce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance, smoke);
- [ ] prove di **unità** sull'idempotenza del versamento, di **integrazione** con il conservatore **simulato**,
      compresi indisponibilità, lentezza ed esito negativo;
- [ ] prova di **isolamento fra account** sul versamento;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-EINVOICING]` (storia `0030`) arriverà fino allo stato
      `conservato` contro il conservatore simulato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con il **terzo fornitore esterno** dichiarato e la ritenzione decennale;
- [ ] **registro delle decisioni** compilato, con il conservatore scelto, il costo e la natura irreversibile del
      versamento;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `archive_document`, marcato irreversibile.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0022` | Serve il pacchetto composto con i metadati completi |
| Contratto con il conservatore qualificato | Non si diventa conservatore accreditato: il rapporto è contrattuale e dura dieci anni |

## 7. Fuori ambito

- L'**esibizione** e lo scarico del conservato: storia `0024`.
- La sorte dell'archivio alla fine dell'abbonamento: storia `0026`.
- Il manuale di conservazione e la nomina del responsabile: adempimenti organizzativi, punto aperto della storia
  `0022`.

## 8. Punti aperti

- 🛑 **Chi è il conservatore e a quali condizioni.** Nessuno dei fornitori esaminati pubblica un costo di
  **giacenza** — solo un costo a documento versato (descrizione dell'applicazione §2.7): se la giacenza si paga,
  il conto del listino cambia in modo sostanziale. È una fermata di escalation.
- **Cosa succede se il conservatore chiude o cambia condizioni** durante i dieci anni. La portabilità
  dell'archivio verso un altro conservatore va prevista nel contratto, non scoperta dopo: non è una funzione da
  scrivere adesso, ma una clausola da avere.
- **Se il versamento debba essere sempre automatico** o se il cliente debba poter scegliere quali documenti
  conservare. Automatico è più sicuro per lui e più costoso per noi; a scelta è più economico e più rischioso per
  lui. È direzione di prodotto.
