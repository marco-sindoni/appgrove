# 0024 — Esibizione e scarico

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 05 — Conservazione a norma e adempimenti
**Storia**: `0024` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0023`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare a cui è arrivata una richiesta di documenti
> voglio ritrovare le fatture di un periodo e portarle via in un pacchetto completo, con le loro prove
> così da rispondere in un pomeriggio invece che in una settimana, e senza dipendere da nessuno.

**Contesto.** Un archivio da cui non si riesce a tirare fuori nulla non è un archivio: è un costo. L'esibizione è
la funzione che il cliente usa raramente e nel momento peggiore — un controllo, una richiesta del commercialista,
un cambio di consulente — ed è il momento in cui giudica tutto il prodotto. È anche la funzione che il
commercialista usa: nella prima versione lo si serve con un ruolo in sola lettura e con questa esportazione, non
con un portale dedicato (descrizione dell'applicazione §2.4).

## 2. Requisiti funzionali

1. **RF-1** — Si può cercare nell'archivio per periodo, soggetto emittente, controparte, direzione (attivi o
   passivi) e stato di conservazione.
2. **RF-2** — Si può scaricare un **singolo documento conservato** con: artefatto ufficiale, notifiche del ciclo
   di vita, indice dei metadati e **ricevuta di conservazione**.
3. **RF-3** — Si può richiedere un'**esportazione in blocco** per un periodo; l'esportazione viene preparata come
   lavorazione differita e resa disponibile per un tempo limitato, con avviso quando è pronta.
4. **RF-4** — Ogni scarico è **tracciato**: chi, quando, cosa. È una lettura di dati personali e va registrata.
5. **RF-5** — Se un documento risulta `non conservato`, lo scarico avviene comunque ma **dichiara** che manca la
   ricevuta: non si spaccia per conservato ciò che non lo è.
6. **RF-6** — L'esportazione resta accessibile anche con **abbonamento scaduto**, perché è l'esercizio di un
   diritto e perché sono documenti del cliente.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ricerca e scarico filtrano per `tenant_id` preso dal token verificato;
  la lavorazione differita dell'esportazione porta con sé il `tenant_id`. Prova di isolamento **obbligatoria e
  particolarmente attenta**: è la funzione in cui una perdita di isolamento consegnerebbe interi archivi.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `GET /api/einvoicing/v1/archive` (ricerca paginata),
  `GET /api/einvoicing/v1/archive/{id}/download` e
  `POST /api/einvoicing/v1/archive/exports` con `GET .../exports/{id}`. Errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit. Il collegamento allo scarico è a **scadenza breve** e
  monouso.
- **RT-3 — Persistenza (§8).** Migrazione `V20__archive_export.sql`: tabella delle esportazioni con stato,
  perimetro, scadenza e riferimento al file prodotto; `tenant_id`, chiave UUID versione 7, colonne di controllo.
  Il file prodotto ha una **scadenza** e viene rimosso: un'esportazione che resta per sempre è un secondo archivio
  non governato.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Archivio» con ricerca, filtri, scarico singolo e richiesta di
  esportazione con indicazione dello stato. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette di ricerca, stati dell'esportazione e l'avvertenza «documento non
  conservato» dallo spazio-nomi `einvoicing`, presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Lo scarico **non** consuma la metrica `documenti`. ⚠️ **Resta accessibile
  con abbonamento `canceled`**, in deroga al `402`, perché i **diritti dell'interessato e la disponibilità dei
  propri documenti** restano accessibili anche quando l'app è disabilitata
  ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §13). L'esportazione in blocco è protetta da un limite
  di frequenza, per non trasformarsi in un carico continuo.
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati: `search_archive(filtri) → elenco
  minimizzato dei documenti conservati` e `get_archive_receipt(id) → ricevuta di conservazione`, entrambi
  **lettura**, nessuna conferma. ⚠️ **Lo scarico in blocco NON è esposto come strumento**: un'esportazione
  integrale dell'archivio richiesta da un agente è un'estrazione massiva di dati personali, e resta un'azione
  umana con un pulsante. La scelta va scritta, non lasciata implicita. Contratto dentro il servizio; server
  conversazionale non implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun campo nuovo, ma **una lettura massiva di dati personali**: il
  tracciamento degli scarichi è esso stesso un trattamento e va dichiarato nel manifesto (chi ha scaricato cosa e
  quando), in italiano e inglese, con base giuridica «interesse legittimo alla tracciabilità». Il file esportato è
  un contenitore di dati personali con una scadenza: la scadenza va dichiarata.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `documento scaricato`, `esportazione richiesta`,
  `esportazione scaduta e rimossa` sono registrati con `tenant_id`, `app_id`, `user_id`, identificativo di
  correlazione e conteggi — senza denominazioni né contenuti.

## 4. Criteri di accettazione

**CA-1 — Scarico singolo completo**
- **Dato** un documento in stato `conservato`
- **Quando** lo si scarica
- **Allora** il pacchetto contiene artefatto ufficiale, notifiche, indice dei metadati e ricevuta di
  conservazione

**CA-2 — Documento non conservato**
- **Dato** un documento in stato `non conservato`
- **Quando** lo si scarica
- **Allora** il pacchetto arriva ma **dichiara esplicitamente** che la ricevuta manca e cosa questo comporta

**CA-3 — Esportazione in blocco**
- **Dato** una richiesta di esportazione per un trimestre con duecento documenti
- **Quando** la lavorazione differita finisce
- **Allora** l'esportazione è disponibile con un collegamento a scadenza breve, e l'utente è avvisato in app

**CA-4 — L'esportazione scade**
- **Dato** un'esportazione oltre la sua scadenza
- **Quando** si tenta di scaricarla
- **Allora** il collegamento non funziona più e il file non esiste più

**CA-5 — Abbonamento scaduto**
- **Dato** un account con abbonamento `canceled`
- **Quando** richiede l'esportazione dei propri documenti conservati
- **Allora** l'operazione riesce, mentre le rotte di trasmissione rispondono `402`

**CA-6 — Isolamento fra account**
- **Dato** due account con archivi propri
- **Quando** un utente dell'uno richiede un'esportazione forzando l'identificativo dell'altro
- **Allora** ottiene la propria, e mai quella dell'altro

**CA-7 — Ogni scarico è tracciato**
- **Dato** un utente che scarica tre documenti
- **Quando** si guarda la traccia
- **Allora** ci sono tre righe con chi, quando e cosa

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sulla composizione del pacchetto di scarico e sulla scadenza dei collegamenti; di
      **integrazione** sull'esportazione differita;
- [ ] prova di **isolamento fra account** rafforzata su ricerca, scarico ed esportazione;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-EINVOICING]` (storia `0030`) chiuderà con lo scarico
      di un documento conservato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la traccia degli scarichi e con la scadenza del file esportato;
- [ ] controllo automatico di **accessibilità** sulla sezione «Archivio»;
- [ ] **registro delle decisioni** compilato, con la scelta di **non esporre lo scarico in blocco al livello
      conversazionale** e il motivo;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `search_archive` e `get_archive_receipt`.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0023` | Senza ricevuta di conservazione non c'è nulla da esibire con valore probatorio |

## 7. Fuori ambito

- Un **portale dedicato al commercialista** con accesso separato: rimandato. Nella prima versione il
  commercialista è un utente dell'account con ruolo in sola lettura.
- La restituzione integrale dell'archivio alla fine del rapporto: storia `0026`, che è cosa diversa da
  un'esportazione ordinaria.
- La ricerca a testo pieno dentro il contenuto dei documenti: fuori ambito, e con implicazioni sui dati personali
  che andrebbero valutate a parte.

## 8. Punti aperti

- **Per quanto tempo resta disponibile un'esportazione.** Troppo poco è scomodo, troppo è un secondo archivio non
  governato. La proposta è pochi giorni, ma è una scelta di prodotto con effetti di conformità.
- **Se l'accesso del commercialista debba essere tracciato in modo distinto** da quello del personale interno.
  Utile in caso di contestazione, ma introduce un concetto di ruolo che oggi non c'è: da valutare, non da
  anticipare.
