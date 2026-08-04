# 0030 — Esportazione della spesa attribuita

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 06 — Cruscotti, confronti e rapporti
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0021`, `0022`, `0028`
**Ultimo aggiornamento**: 2026-08-04

## 1. Narrazione

> Come chi tiene i conti dell'azienda
> voglio scaricare la spesa attribuita di un periodo in una tavola che si apre nel foglio di calcolo
> così da poterla accostare alla fattura del fornitore e allegarla alla nota di addebito al cliente, senza ricopiare
> numeri a mano.

**Contesto.** L'esportazione tabellare è chiesta in ogni dominio (§2.4 del documento capofila) e qui più che
altrove: i numeri di TokenGrove finiscono in una riconciliazione contabile e talvolta in un addebito a un cliente
finale. Da qui una conseguenza che questa storia deve portare per intero: **una tavola esportata è un numero che
esce dall'app e su cui qualcuno prenderà una decisione**. Deve quindi portare con sé il proprio contesto — periodo,
asse, copertura di attribuzione, versione del catalogo prezzi, istante di generazione — altrimenti fra due mesi
nessuno saprà più come è stata fatta. È anche il motivo per cui la storia `0017` (ricalcolo dello storico) mostra
un avvertimento aggiuntivo quando il periodo interessato è già stato esportato: quei numeri sono già usciti.

## 2. Requisiti funzionali

1. **RF-1** — Da ogni vista che mostra una tavola (panoramica scomposta per asse, non attribuito, prospetto di
   ribaltamento) si può esportare **esattamente ciò che si sta guardando**: stesso periodo, stesso filtro, stesso
   ordinamento. Nessuna esportazione produce un insieme diverso da quello visibile.
2. **RF-2** — Due granularità, scelte esplicitamente: **aggregata** (una riga per valore dell'asse) e
   **dettagliata** (una riga per misura, con istante, modello, conteggi, costo congelato, versione di catalogo ed
   etichette).
3. **RF-3** — Ogni file esportato contiene un'**intestazione di contesto** con: account, periodo, asse, filtro
   applicato, copertura di attribuzione del periodo, versione del catalogo prezzi, istante di generazione, e la
   riga «i costi sono congelati al momento della misurazione».
4. **RF-4** — I formati sono due: **valori separati da virgola** per il foglio di calcolo (con separatore e formato
   dei numeri coerenti con la lingua scelta) e **tavola leggibile** per chi la allega a un messaggio.
5. **RF-5** — Un'esportazione grande non blocca l'interfaccia: si prepara in secondo piano e si scarica quando è
   pronta, con un collegamento che scade. Sotto una soglia di righe si scarica subito.
6. **RF-6** — Ogni esportazione resta a registro — chi, quando, quale periodo, quale asse, quante righe — e il
   registro è consultabile dall'account. Serve a due cose: rispondere a «chi ha portato fuori questi numeri» e
   avvisare prima di un ricalcolo che tocca un periodo già esportato (storia `0017`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'esportazione contiene solo righe del `tenant_id` del gettone
  verificato; il collegamento allo scarico è legato all'account e all'utente che l'ha richiesto, e non è
  indovinabile.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/spesa_modelli/v1/esportazioni` (richiesta),
  `GET /api/spesa_modelli/v1/esportazioni` (registro), `GET /api/spesa_modelli/v1/esportazioni/{id}/contenuto`
  (scarico); errori in `problem+json` che distinguono «non pronta» da «scaduta» da «oltre lo storico del piano»;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_spesa_modelli`: tabella `esportazione` con
  `tenant_id`, periodo, asse, filtro, granularità, formato, numero di righe, istante, chi l'ha chiesta, stato e
  scadenza; chiave primaria UUID versione 7, colonne di controllo, cancellazione logica. **Il contenuto non si
  conserva oltre la scadenza**: si conserva la traccia di cosa è stato esportato, non una seconda copia dei dati.
- **RT-4 — Varchi, ruoli e quota (§6, §7).** L'esportazione **dettagliata** è riservata a `owner` e `admin` (porta
  fuori le etichette, che possono riguardare persone); l'aggregata è accessibile anche a `member`. L'esportazione
  **non consuma** la metrica `misure_registrate`: non registra misure. Il periodo esportabile è limitato dallo
  **storico del piano**, con lo stesso comportamento dichiarato della panoramica.
- **RT-5 — Modulo frontend (§3, §5).** Pulsante di esportazione su ogni tavola, con scelta di granularità e
  formato, avviso quando la preparazione è in secondo piano, e sezione «Esportazioni» con il registro. Solo token
  del sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Le intestazioni delle colonne e l'intestazione di contesto sono tradotte in
  `en, it, fr, es, de`; i formati di numero e data seguono la lingua scelta al momento dell'esportazione, che è
  dichiarata nell'intestazione (un file letto da un collega tedesco deve essere interpretabile).
- **RT-7 — Esposizione conversazionale (§12).** L'esportazione è **scrittura con conferma** quando è dettagliata,
  perché produce un file che contiene etichette riferibili a persone e che uscirà dall'app: in chat produce una
  bozza con periodo, numero di righe e avvertenza, e richiede conferma umana (storia `0033`). L'esportazione
  aggregata resta comodamente ottenibile come lettura tramite `leggi_spesa` (storia `0032`).
- **RT-8 — Dati personali (§10).** La tabella `esportazione` contiene l'identificativo di chi ha esportato: entra
  in `exportData` e `purgeData` del contratto dati dell'app (storia `0035`) e ha la propria voce nel manifesto
  `docs/compliance/manifests/spesa_modelli.yaml` in italiano e inglese. Il contenuto esportato non è un archivio
  nuovo di dati personali: scade e viene cancellato fisicamente.
- **RT-9 — Registrazione eventi (§14).** Eventi «esportazione richiesta», «esportazione pronta», «esportazione
  scaricata» con `tenant_id`, `app_id`, `user_id`, periodo, asse, granularità, numero di righe e identificativo di
  correlazione — **senza** importi né valori di etichetta.

## 4. Criteri di accettazione

**CA-1 — Esporto quello che vedo**
- **Dato** la panoramica filtrata su un mese e sull'asse «cliente finale», ordinata per importo
- **Quando** si esporta in formato tabellare
- **Allora** il file contiene le stesse righe, nello stesso ordine, con gli stessi importi, più l'intestazione di
  contesto

**CA-2 — Il contesto viaggia col file**
- **Dato** un'esportazione qualunque
- **Quando** si apre il file
- **Allora** in testa ci sono periodo, asse, filtro, copertura, versione del catalogo prezzi, istante di
  generazione e la nota sui costi congelati

**CA-3 — Esportazione grande in secondo piano**
- **Dato** un periodo con un numero di righe oltre la soglia
- **Quando** si chiede l'esportazione dettagliata
- **Allora** l'interfaccia resta utilizzabile, l'esportazione risulta «in preparazione» e diventa scaricabile
  quando è pronta; il collegamento scade e dopo la scadenza lo scarico è rifiutato con la spiegazione

**CA-4 — Ruoli**
- **Dato** un utente con ruolo `member`
- **Quando** chiede l'esportazione dettagliata
- **Allora** riceve `403` con la spiegazione; l'esportazione aggregata gli riesce

**CA-5 — Il registro serve al ricalcolo**
- **Dato** un periodo già esportato
- **Quando** si avvia un ricalcolo dello storico su quel periodo (storia `0017`)
- **Allora** prima della conferma compare l'avvertimento che quei numeri sono già usciti dall'app, con data e
  autore dell'esportazione

**CA-6 — Isolamento fra account**
- **Dato** un'esportazione dell'account `A`
- **Quando** un utente dell'account `B` ne indovina l'identificativo e chiede lo scarico
- **Allora** la richiesta è rifiutata e nulla del contenuto è rivelato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla generazione della tavola (separatori e formati per lingua, intestazione di contesto)
      e di **integrazione** sul ciclo richiesta → preparazione → scarico → scadenza;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulle due granularità;
- [ ] prova che «esporto quello che vedo»: stessa richiesta di lettura e stessa esportazione producono lo stesso
      insieme di righe;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «esporto la spesa
      attribuita del mese e ritrovo nel file gli stessi totali della schermata», e aggiornare il registro di
      copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue, comprese le intestazioni di colonna;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese: `esportazione` in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sulla scadenza del contenuto, sulla riserva della
      granularità dettagliata ai ruoli alti e sull'intestazione di contesto obbligatoria;
- [ ] contratto degli **strumenti conversazionali** dichiarato: esportazione dettagliata = scrittura con conferma;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0021` | La copertura di attribuzione fa parte dell'intestazione di contesto di ogni file |
| Storia `0022` | Il prospetto di ribaltamento è una delle tavole esportabili |
| Storia `0028` | L'esportazione parte dalle stesse viste e dalla stessa sintesi della panoramica |

## 7. Fuori ambito

- l'invio **automatico e periodico** del file a un destinatario: è la storia `0031`;
- l'**esportazione dei dati personali su richiesta dell'interessato**, che è un'altra cosa e ha altre regole: è la
  storia `0035` (contratto dati dell'app), e non si confonde con questa nemmeno nei testi dell'interfaccia;
- il collegamento diretto a un foglio di calcolo in rete o a uno strumento di analisi di terzi: rimandato,
  introdurrebbe un fornitore esterno nuovo (§10 dei principi) e non è stato chiesto;
- l'emissione di documenti commerciali a partire dalla tavola: esclusa per scelta (storia `0022`, §7).

## 8. Punti aperti

- **Per quanto tempo resta scaricabile un'esportazione preparata.** Più è lungo, più comoda è; ma è una seconda
  copia dei dati che vive fuori dalle tabelle e va cancellata. Proposta: poche ore, con la possibilità di rigenerare
  in un clic. La conferma lo sviluppatore, che decide anche dove il contenuto risiede in attesa dello scarico —
  scelta che deve rispettare la residenza europea dei dati (§10 dei principi).
