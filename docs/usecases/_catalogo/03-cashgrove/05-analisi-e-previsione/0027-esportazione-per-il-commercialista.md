# 0027 — Esportazione per il commercialista

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 05 — Analisi e previsione
**Storia**: `0027` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0023`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare a cui il commercialista chiede «mandami la situazione dei crediti al 31 dicembre»
> voglio produrre quel file in un clic, alla data che mi serve
> così da non passare un pomeriggio a copiare righe in un foglio di calcolo.

**Contesto.** È la richiesta che arriva per prima e più spesso, ed è a costo quasi zero perché i dati ci sono già.
Chiude l'epica di analisi con la funzione meno appariscente e più usata. Vale anche come rete di sicurezza sulla
fiducia: un cliente che può portarsi via i suoi dati quando vuole si affida più volentieri — ed è, in forma diversa, lo
stesso principio dell'esportazione prevista dai diritti dell'interessato (storia `0030`), che però ha finalità e
destinatari del tutto diversi e non va confusa con questa.

## 2. Requisiti funzionali

1. **RF-1** — L'utente esporta la situazione dei crediti a una data scelta, in formato tabellare aperto (valori separati
   e foglio di calcolo).
2. **RF-2** — L'esportazione contiene, riga per riga: debitore, identificativo fiscale, numero e data del documento,
   scadenza, importo originario, incassato, residuo, giorni di ritardo, stato, fascia di anzianità.
3. **RF-3** — È disponibile anche una esportazione **riepilogativa** per debitore e per fascia, che è quella che il
   commercialista guarda per prima.
4. **RF-4** — L'utente può filtrare l'esportazione per stato, debitore e intervallo di scadenza, riusando gli stessi
   filtri dell'elenco.
5. **RF-5** — L'esportazione dichiara nell'intestazione la data di riferimento, la data di generazione e i filtri
   applicati: un file senza queste tre informazioni diventa inservibile dopo una settimana.
6. **RF-6** — Se l'esportazione è grande, viene preparata in secondo piano e l'utente riceve l'avviso quando è pronta,
   senza restare in attesa davanti allo schermo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'esportazione filtra per `tenant_id` preso dal token verificato; il file
  prodotto è scaricabile solo da utenti dello stesso account, con un collegamento a scadenza breve.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/crediti/v1/esportazioni` (avvia) e
  `GET /api/crediti/v1/esportazioni/{id}` (stato e scaricamento); errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per la tabella `esportazione` sullo schema `app_crediti` (istante, tipo,
  filtri, stato, autore, scadenza del file) con `tenant_id`, chiave UUID versione 7, colonne di controllo e
  cancellazione logica. Il **file prodotto** ha vita breve e viene rimosso alla scadenza: contiene dati personali e non
  deve restare in giro.
- **RT-4 — Modulo frontend (§3, §5).** Azione «esporta» nella sezione *Crediti* e nella *Panoramica*, con scelta della
  data e dei filtri, e avviso di pronto; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le stringhe dell'interfaccia passano dallo spazio-nomi `crediti` e sono presenti in
  `en, it, fr, es, de`. **Anche le intestazioni delle colonne del file** seguono la lingua attiva: un file con le
  colonne in inglese mandato al commercialista italiano è un piccolo fallimento evitabile.
- **RT-6 — Varchi e quota (§6, §7).** Non consuma quota. Accessibile anche al ruolo in sola lettura, che è proprio
  quello del commercialista.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: produrre un file di dati personali su richiesta di
  un assistente, senza che nessuno guardi cosa contiene, non è un vantaggio. Le stesse informazioni sono disponibili in
  chat come **risposta** attraverso `riepilogo_anzianita` ed `elenca_crediti_scaduti`. Scelta esplicita, annotata.
- **RT-8 — Dati personali (§10).** Il file contiene dati personali dei debitori: vita breve, collegamento a scadenza,
  nessuna copia in registri o diagnostiche. Voce nel manifesto per la tabella `esportazione` (metadati, non contenuti),
  presente in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «esportazione richiesta», «esportazione pronta», «esportazione
  scaricata» sono registrati con `tenant_id`, `app_id`, `user_id`, tipo e identificativo di correlazione, senza
  contenuti.

## 4. Criteri di accettazione

**CA-1 — Esportazione a data**
- **Dato** un credito incassato il 20 gennaio · **Quando** si esporta la situazione al 31 dicembre precedente ·
  **Allora** quel credito compare come aperto, con il residuo che aveva a quella data

**CA-2 — Intestazione informativa**
- **Dato** un file esportato · **Quando** lo si apre · **Allora** in testa si leggono data di riferimento, data di
  generazione e filtri applicati

**CA-3 — Riepilogo per fascia**
- **Dato** l'esportazione riepilogativa · **Quando** la si apre · **Allora** contiene una riga per debitore con i totali
  per fascia, e i totali coincidono con quelli della *Panoramica*

**CA-4 — Esportazione grande**
- **Dato** un account con molti crediti · **Quando** si avvia l'esportazione · **Allora** la richiesta ritorna subito,
  il file viene preparato in secondo piano e l'utente riceve l'avviso quando è pronto

**CA-5 — Collegamento a scadenza**
- **Dato** un file pronto · **Quando** si tenta di scaricarlo dopo la scadenza del collegamento · **Allora** il
  download è negato e il file non è più disponibile

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** un utente di `B` tenta di scaricare l'esportazione di `A` · **Allora**
  riceve l'errore di risorsa non trovata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sulla ricostruzione della situazione a una data passata e di **integrazione** sulla produzione
      in secondo piano;
- [ ] prova di **isolamento fra account** su richiesta e scaricamento;
- [ ] **prova end-to-end**: *nessun impatto sul percorso principale* — l'esportazione è coperta da prove di
      integrazione; il percorso `[J-CREDITI]` non la attraversa;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, comprese le intestazioni di colonna;
- [ ] **manifesto dei dati** aggiornato con `esportazione`, presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sulla vita breve del file e sull'esclusione dagli strumenti
      conversazionali;
- [ ] contratto degli **strumenti conversazionali**: esclusione deliberata, annotata con il motivo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0023` | La ricostruzione della situazione a una data passata è la stessa dell'anzianità |

## 7. Fuori ambito

- L'invio automatico dell'esportazione al commercialista per posta elettronica: sarebbe una trasmissione di dati
  personali verso un terzo, decisa da un automatismo. Se servirà, sarà una storia con le sue garanzie.
- Formati specifici di programmi di contabilità: nessuna fonte consultata indica un formato di scambio comune nel
  segmento; il formato tabellare aperto è la scelta che funziona ovunque.
- L'esportazione per i **diritti dell'interessato**, che ha finalità, perimetro e destinatario diversi: storia `0030`.

## 8. Punti aperti

Nessuno.
