# 0002 — Modello dati del documento

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio che il documento commerciale esista come tabella, con la sua macchina a stati e le sue righe
> così da poter costruire preventivi, fatture, note di credito e documenti di trasporto sopra un unico modello
> invece di quattro modelli quasi uguali.

**Contesto.** Il catalogo elenca sette entità di dominio, ma il cuore è uno solo: un documento con delle righe, un
tipo, uno stato e un riferimento a un altro documento. Sbagliare questa forma adesso costa più di ogni altro errore
dell'app, perché la normativa vieta di modificare un documento emesso (§2.3 della descrizione): non si potrà
riscrivere lo storico. Va fatta subito dopo l'impianto e prima di qualunque schermata.

## 2. Requisiti funzionali

1. **RF-1** — Esiste la tabella dei documenti con: tipo (preventivo, fattura, nota di credito, documento di
   trasporto, ricevuta), sezionale, numero, data, stato, valuta, totali e riferimento facoltativo a un documento
   d'origine.
2. **RF-2** — Esiste la tabella delle righe, legata al documento, con descrizione, quantità, prezzo unitario,
   sconto, aliquota e totale di riga.
3. **RF-3** — Il documento porta la **copia congelata** dei dati del cliente al momento dell'emissione, distinta
   dall'anagrafica viva.
4. **RF-4** — La macchina a stati è vincolata a livello di dominio: i passaggi ammessi sono solo quelli descritti
   nel §4 della descrizione dell'applicazione; un passaggio non ammesso viene rifiutato.
5. **RF-5** — Un documento in stato diverso da `bozza` non è modificabile né cancellabile logicamente.
6. **RF-6** — Esistono le rotte minime di lettura ed elenco paginato dei documenti dell'account.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `document` e `document_line` filtra per
  `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri
  viene ignorato. Prova di isolamento fra due account su entrambe le risorse.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/billing/v1/documents` (paginato, con totale) e
  `GET /api/billing/v1/documents/{id}`; oggetti di trasferimento al bordo, entità mai esposte; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V2__document_and_lines.sql` sullo schema `app_billing`: tabelle
  `document` e `document_line` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e
  cancellazione logica. Nessuna chiave esterna verso altri schemi. Indice su `(tenant_id, type, status, date)` e
  vincolo di unicità su `(tenant_id, section, year, number)` quando il numero è valorizzato.
- **RT-6 — Varchi e quota (§6).** Le rotte di lettura passano dai varchi comuni; nessun consumo di quota: la quota
  si prenota all'**emissione** (storia `0012`), non alla creazione della bozza.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dichiarato qui: il contratto arriva con l'epica
  06. La forma dell'oggetto di trasferimento è però pensata per essere minimizzabile (numero, data, cliente,
  totale, stato).
- **RT-8 — Dati personali (§10).** La copia congelata dei dati del cliente **è** un dato personale: voce
  `document.dati_cliente_congelati` nel manifesto in italiano e inglese, campo annotato `@PersonalData`, tabelle
  `document` e `document_line` aggiunte a `exportData` e `purgeData` del contratto `BillingDataContract`. La
  cancellazione fisica su un documento **emesso** è però in conflitto con l'obbligo decennale: in questa storia si
  implementa l'esportazione e si lascia la cancellazione al presidio della storia `0026`, dichiarandolo.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `documento creato` e `passaggio di stato rifiutato` sono
  registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati personali: si
  scrivono identificativi, non nomi né importi riferibili.

## 4. Criteri di accettazione

**CA-1 — Creazione di una bozza**
- **Dato** un utente abilitato dell'account `A`
- **Quando** crea un documento di tipo `fattura` con due righe
- **Allora** il documento nasce in stato `bozza`, senza numero, e i totali di riga sono calcolati

**CA-2 — Passaggio di stato non ammesso**
- **Dato** un documento in stato `bozza` · **Quando** si tenta di portarlo direttamente a `pagato`
- **Allora** la risposta è `409` in `problem+json` con l'elenco dei passaggi ammessi, e lo stato non cambia

**CA-3 — Documento emesso non modificabile**
- **Dato** un documento in stato `emesso`
- **Quando** si tenta di modificarne una riga o di cancellarlo
- **Allora** la risposta è `409` e nulla cambia

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri documenti
- **Quando** un utente di `A` chiede l'elenco dei documenti
- **Allora** vede solo i propri, anche se forza l'identificativo dell'altro account nella richiesta, e la lettura
  puntuale di un documento di `B` risponde `404`

**CA-5 — Unicità del numero**
- **Dato** un documento già numerato `2026/A/17`
- **Quando** si tenta di scrivere un secondo documento con lo stesso sezionale, anno e numero
- **Allora** la scrittura fallisce a livello di base dati, non solo a livello applicativo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla macchina a stati e di **integrazione** sulle rotte dei documenti, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su `document` e `document_line`;
- [ ] **prova end-to-end**: *rimando* — non c'è ancora superficie utente; proprietaria è la storia `0031`;
- [ ] **traduzioni**: non applicabile, nessun testo visibile;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la voce della copia congelata, campo annotato,
      tabelle presenti in esportazione;
- [ ] **registro delle decisioni** compilato, con annotata la scelta del modello unico per cinque tipi di documento;
- [ ] contratto degli **strumenti conversazionali**: nessuno qui, dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | Servono lo schema `app_billing`, il contesto del tenant e la mappatura degli errori |

## 7. Fuori ambito

- l'assegnazione del numero: storia `0012` (qui c'è solo il posto dove il numero andrà e il vincolo di unicità);
- il calcolo delle imposte: storia `0013`;
- l'anagrafica cliente viva: storia `0006` (qui c'è solo la copia congelata);
- il blocco decennale della cancellazione: storia `0026`.

## 8. Punti aperti

Il conflitto fra la cancellazione fisica prescritta dalla piattaforma e l'obbligo di conservare dieci anni il
documento emesso è aperto (punto 3 del §11 della descrizione) e lo chiude lo sviluppatore con la revisione legale.
Questa storia lo tocca ma non lo decide: implementa l'esportazione e rimanda il presidio della cancellazione.
