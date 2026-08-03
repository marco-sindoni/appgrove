# 0011 — Documento canonico

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 03 — Documento canonico e validazione
**Storia**: `0011` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0006`, `0007`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che dovrà aggiungere la terza e la quarta giurisdizione
> voglio un unico modello interno del documento, allineato alla norma europea, da cui si derivano tutti i formati
> così da scrivere un serializzatore per paese invece di un'applicazione per paese.

**Contesto.** È la storia che decide se questa app invecchierà bene. La nota architetturale del catalogo lo dice
in modo esplicito: serve «un modello canonico allineato a EN 16931 e un adapter per giurisdizione». Il rischio
opposto — modellare sul formato italiano e poi «adattarlo» — è il difetto più comune del dominio: produce un
modello che ha campi con nomi italiani, obbligatorietà italiane e una nozione di stato che nel resto d'Europa non
esiste. Va fatta adesso, prima che entri il primo documento vero.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modello `CanonicalDocument` con: tipo (fattura, nota di credito), numero, data, valuta,
   riferimenti, soggetto emittente, controparte, giurisdizione, righe, totali per aliquota, totale generale.
2. **RF-2** — I nomi e la semantica dei campi seguono la **norma europea EN 16931**, non il formato di un paese;
   dove un paese richiede un campo suo, questo vive in una parte **estensibile** separata e chiaramente marcata.
3. **RF-3** — I totali sono **ricalcolati e verificati** dal servizio: un documento in cui la somma delle righe
   non corrisponde al totale dichiarato è respinto con l'indicazione della differenza.
4. **RF-4** — Il documento ha uno **stato del ciclo di vita** e nasce sempre in `bozza`; il passaggio di stato è
   consentito solo lungo le transizioni previste dalla famiglia della sua giurisdizione.
5. **RF-5** — Il documento ricorda la propria **origine** (evento dalla piattaforma, importazione da file,
   inserimento manuale) e la **versione delle regole** con cui è stato trattato.
6. **RF-6** — Un documento in stato diverso da `bozza` **non è modificabile**: si correggono i documenti che non
   sono ancora partiti, gli altri si sostituiscono.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `canonical_document` e `document_line`
  filtra per `tenant_id` preso dal token verificato; un `tenant_id` forzato dall'esterno viene ignorato. Prova di
  isolamento su due account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/einvoicing/v1/documents` (elenco paginato con
  filtri per stato, periodo, giurisdizione, controparte) e `GET /api/einvoicing/v1/documents/{id}`; oggetti di
  trasferimento al bordo, entità mai esposte; errori in `application/problem+json`; definizione OpenAPI aggiornata
  nello stesso commit. Le rotte di creazione sono delle storie `0012` e `0013`.
- **RT-3 — Persistenza (§8).** Migrazione `V8__canonical_document.sql` sullo schema `app_einvoicing`: le tabelle
  `canonical_document` e `document_line` si completano, con `tenant_id`, chiave UUID versione 7, colonne di
  controllo, cancellazione logica, e indici su `(tenant_id, stato)` e `(tenant_id, data)`.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Documenti»: elenco con ricerca e filtri, scheda di dettaglio in
  sola lettura con i fatti a sinistra e la cronologia a destra. Solo token del sistema di design; tema chiaro e
  scuro.
- **RT-5 — Cinque lingue (§4).** Etichette dei campi, nomi degli stati e messaggi di errore dallo spazio-nomi
  `einvoicing`, presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La **lettura** non consuma quota; la creazione la consuma, ma la creazione
  appartiene alle storie `0012` e `0013`. Qui si predispone il punto di prenotazione, non lo si usa.
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati: `list_documents(filtri) → elenco
  minimizzato` e `get_document(id) → documento`, entrambi marcati **lettura**, nessuna conferma. Il risultato di
  `list_documents` è **minimizzato**: numero, data, totale, stato, giurisdizione — non le righe né gli indirizzi.
  Contratto dentro il servizio; server conversazionale non implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Sì.** `document_line.descrizione` è testo libero e riguarda la controparte
  quando è persona fisica: voce nuova nel manifesto in italiano e inglese, campo annotato `@PersonalData`, tabelle
  `canonical_document` e `document_line` presenti in `exportData` e `purgeData`. ⚠️ È il campo da cui possono
  entrare **categorie particolari** (descrizione dell'applicazione §6): il presidio è la regola di validazione
  della storia `0014`, e va costruito lì, non assunto qui.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `documento creato`, `stato cambiato`, `totali non coerenti`
  sono registrati con `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e identificativo del
  documento, **senza** numero di documento, denominazioni o descrizioni di riga.

## 4. Criteri di accettazione

**CA-1 — Totali coerenti**
- **Dato** un documento con tre righe e i totali per aliquota
- **Quando** lo si salva
- **Allora** il servizio ricalcola i totali e li accetta perché coincidono

**CA-2 — Totali incoerenti**
- **Dato** un documento in cui il totale generale differisce dalla somma delle righe di un centesimo
- **Quando** lo si salva
- **Allora** riceve `400` con l'indicazione della differenza e del valore atteso, e nulla viene creato

**CA-3 — Stato non modificabile**
- **Dato** un documento in stato diverso da `bozza`
- **Quando** si tenta di modificarne una riga
- **Allora** l'operazione è rifiutata con la spiegazione che un documento già avviato si sostituisce, non si
  corregge

**CA-4 — Transizione non consentita**
- **Dato** un documento italiano in stato `bozza`
- **Quando** si tenta di portarlo direttamente a `consegnato` saltando la validazione e la trasmissione
- **Allora** la transizione è rifiutata

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri documenti
- **Quando** un utente di `A` chiede il documento di `B` per identificativo
- **Allora** riceve `404`, anche forzando l'identificativo dell'account nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sul ricalcolo dei totali e sulle transizioni di stato per famiglia, di **integrazione**
      sull'elenco e sul dettaglio con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulle due tabelle;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-EINVOICING]` (storia `0030`) attraverserà elenco e
      dettaglio;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, campi annotati, tabelle in esportazione e
      cancellazione;
- [ ] controllo automatico di **accessibilità** su elenco e dettaglio;
- [ ] **registro delle decisioni** compilato, con la scelta del modello canonico e il perché **non** si parte dal
      formato italiano;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `list_documents` e `get_document`.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0002` | Servono le tabelle di base |
| `0006` | La famiglia della giurisdizione determina le transizioni di stato ammesse |
| `0007`, `0008` | Il documento punta a un soggetto emittente e a una controparte |

## 7. Fuori ambito

- La **creazione** dei documenti: storie `0012` (dalla piattaforma) e `0013` (importazione e inserimento manuale).
- La **validazione** di conformità: storia `0014`. Qui si controllano solo coerenza interna e transizioni.
- La **serializzazione** nei formati ufficiali: storie `0016`-`0018`.

## 8. Punti aperti

- **Quanto della norma europea recepire subito.** La norma ha molti campi facoltativi; recepirla tutta adesso
  costa e non serve a nessuno. La proposta è: nucleo obbligatorio completo, parte estensibile aperta, campi
  facoltativi aggiunti quando una giurisdizione li richiede. Va confermato, perché è una scelta che si paga dopo.
- **Nota di debito e documenti diversi dalla fattura** (documento di trasporto, ricevuta): non modellati.
  Appartengono alla sorgente del documento, non a questa app, e vanno confermati con lo sviluppatore prima di
  entrare nel modello canonico.
