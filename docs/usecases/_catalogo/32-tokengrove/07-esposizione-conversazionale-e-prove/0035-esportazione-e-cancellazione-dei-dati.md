# 0035 — Esportazione e cancellazione dei dati personali

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0035` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0019`, `0030`, `0031`, `0033`, `0034`
**Ultimo aggiornamento**: 2026-08-04

## 1. Narrazione

> Come responsabile dell'account che riceve da un proprio cliente la richiesta di sapere quali dati abbiamo su di
> lui, o di cancellarli
> voglio poter esportare e cancellare **tutto** ciò che TokenGrove conserva su quella persona, senza che nessuna
> tabella resti indietro
> così da rispondere entro i termini, con una prova di quello che ho fatto, e senza perdere i totali di spesa che
> mi servono per la contabilità.

**Contesto.** Ogni app della piattaforma implementa il contratto dati `AppDataContract` — `appId()`,
`exportData(scope)`, `purgeData(scope)`, `manifest()`, per convenzione `SpesaModelliDataContract` — e ogni tabella
che contiene dati riferibili a persone deve comparire in **entrambe** le operazioni: dimenticarne una è il difetto
di conformità più probabile in un'app nuova (§10 dei principi di piattaforma). Le storie precedenti hanno aggiunto
tabelle una alla volta dichiarando ciascuna la propria voce; questa storia **chiude il conto**: verifica che
l'elenco sia completo, che la cancellazione sia fisica e che il manifesto in italiano e inglese descriva la realtà.

**Il caso particolare di quest'app, che è la ragione per cui questa storia non è un adempimento meccanico.**
Cancellare l'etichetta che identifica un cliente finale **non deve cancellare la misura**: il costo di quella
chiamata è un dato contabile del nostro cliente, che ci ha pagato per averlo. La via corretta è cancellare
l'etichetta e far **confluire la misura nel non attribuito**, dichiarandolo (§6 del documento capofila). Farlo al
contrario — cancellare la riga — significherebbe far sparire dei soldi dai conti di un'azienda per rispondere alla
richiesta di una persona diversa.

## 2. Requisiti funzionali

1. **RF-1** — `exportData` restituisce, per l'ambito richiesto, **tutte** le tabelle dell'app che contengono dati
   riferibili a persone: `misura` (etichette e autore), `fonte` (chi l'ha creata), `regola_di_attribuzione` (chi
   l'ha scritta), `budget` (destinatari degli avvisi), `avviso` (registro dei recapiti), `prospetto` (valori
   dell'asse), `esportazione` (chi ha esportato), `rapporto` e `recapito_rapporto` (destinatari e recapiti),
   `bozza_operazione` (chi ha chiesto e chi ha confermato).
2. **RF-2** — `purgeData` cancella **fisicamente** gli stessi dati e lascia una riga di prova nel registro delle
   purghe. Sostituire un valore con un codice **non è cancellare**: la pseudonimizzazione non soddisfa la richiesta.
3. **RF-3** — La cancellazione riferita a una **persona** (l'utente finale o il cliente finale identificato da
   un'etichetta) rimuove l'etichetta e le sue eventuali revisioni, **conserva la misura** e la fa confluire nel non
   attribuito; il totale di spesa del periodo **non cambia**, la copertura di attribuzione **scende** e la
   variazione è visibile e spiegata.
4. **RF-4** — La cancellazione riferita all'**intero account** (chiusura) rimuove ogni dato dell'account,
   comprese le fonti e il segreto custodito nell'archivio dei segreti, che va cancellato anche lì.
5. **RF-5** — La **conservazione** è quella dichiarata dal piano — storico 30 giorni, 13 mesi o 25 mesi — e viene
   applicata da una cancellazione periodica automatica, fisica, tracciata; il declassamento a un piano con storico
   più corto è un evento che **avvisa prima** di cancellare, mai una perdita silenziosa.
6. **RF-6** — I **diritti dell'interessato restano accessibili** anche quando l'app è disabilitata o l'abbonamento
   è scaduto: chi ha smesso di pagare deve comunque poter esportare e cancellare (§13 dei principi).

## 3. Requisiti tecnici

- **RT-1 — Contratto dati dell'app (§10).** Classe `SpesaModelliDataContract` che implementa `AppDataContract` con
  `appId()`, `exportData(scope)`, `purgeData(scope)`, `manifest()`. Una tabella nuova che non compare in entrambe
  le operazioni è un difetto, non un'omissione tollerabile.
- **RT-2 — Manifesto dei dati (§10).** `docs/compliance/manifests/spesa_modelli.yaml` completo, con **italiano e
  inglese obbligatori** su ogni testo, una voce per campo (dove vive, di chi è, che dato è, a cosa serve, perché è
  lecito, per quanto si tiene). I campi Java corrispondenti sono annotati `@PersonalData`: un campo annotato e non
  dichiarato **fa fallire la compilazione**, ed è il presidio su cui questa storia si appoggia.
- **RT-3 — Isolamento fra account (§1).** Esportazione e cancellazione agiscono nel solo `tenant_id` del gettone
  verificato; l'ambito non si allarga mai a partire da un parametro della richiesta.
- **RT-4 — Persistenza (§8).** La cancellazione per i diritti dell'interessato e per la chiusura dell'account è
  **fisica**, non logica: è l'unica eccezione ammessa alla cancellazione con `deleted_at`. La riga di prova nel
  registro delle purghe non contiene i dati cancellati.
- **RT-5 — Coerenza con i costi congelati (§ documento capofila, §4).** Togliere un'etichetta **non** ricalcola il
  costo della misura né tocca la versione di catalogo congelata: si cambia l'attribuzione, non il conto.
- **RT-6 — Modulo frontend (§3, §5).** Nella sezione delle impostazioni dell'app: richiesta di esportazione,
  richiesta di cancellazione con la spiegazione degli effetti (in particolare che la copertura scenderà), e stato
  delle richieste. Solo token del sistema di design; tema chiaro e scuro.
- **RT-7 — Cinque lingue (§4).** L'interfaccia e le spiegazioni degli effetti sono presenti in `en, it, fr, es, de`.
  Il manifesto invece vuole **due** lingue, italiano e inglese: sono due elenchi diversi e non vanno confusi.
- **RT-8 — Esposizione conversazionale (§12).** **Nessuno** strumento conversazionale cancella dati: l'esclusione è
  dichiarata nella storia `0033`, §7. L'esportazione dei dati personali non è l'esportazione della spesa (storia
  `0030`) e i due testi dell'interfaccia non usano la stessa parola senza qualificarla.
- **RT-9 — Registrazione eventi (§14).** Eventi «esportazione dei dati richiesta ed eseguita», «purga eseguita su
  N righe», «conservazione applicata» con `tenant_id`, `app_id`, `user_id`, ambito ed esito — **senza** i dati
  cancellati.

## 4. Criteri di accettazione

**CA-1 — Nessuna tabella resta indietro**
- **Dato** un account con dati in tutte le tabelle elencate in RF-1
- **Quando** si esegue `exportData` sull'intero account
- **Allora** ogni tabella compare nell'esito, e una prova automatica confronta l'elenco delle tabelle con dati
  personali dichiarate nel manifesto con quelle effettivamente esportate: se una manca, la prova è rossa

**CA-2 — La misura sopravvive alla cancellazione dell'etichetta**
- **Dato** un cliente finale identificato da un'etichetta su 300 misure di un mese chiuso
- **Quando** si esegue la cancellazione riferita a quella persona
- **Allora** l'etichetta e le sue revisioni non esistono più, le 300 misure ci sono ancora, il totale del mese è
  invariato e la copertura di attribuzione è scesa di conseguenza, con la variazione spiegata

**CA-3 — La cancellazione è fisica**
- **Dato** una purga eseguita
- **Quando** si interroga il database direttamente
- **Allora** i valori non esistono più in nessuna forma, nemmeno sostituiti da un codice, e il registro delle purghe
  porta la riga di prova senza contenere i dati

**CA-4 — Chiusura dell'account e segreto della fonte**
- **Dato** un account con due fonti collegate e i relativi segreti nell'archivio dei segreti
- **Quando** l'account viene chiuso
- **Allora** dati e segreti sono cancellati anche nell'archivio dei segreti, e la prova lo verifica

**CA-5 — Conservazione secondo il piano**
- **Dato** un account sul piano con storico a 30 giorni e misure di 90 giorni fa
- **Quando** gira la cancellazione periodica
- **Allora** le misure oltre lo storico sono cancellate fisicamente, l'operazione è tracciata, e un declassamento di
  piano avvisa prima di cancellare

**CA-6 — Diritti accessibili anche senza abbonamento**
- **Dato** un account con abbonamento `canceled`
- **Quando** chiede l'esportazione o la cancellazione dei dati
- **Allora** l'operazione riesce, mentre le funzioni ordinarie dell'app restano chiuse con `402`

**CA-7 — Isolamento fra account**
- **Dato** due account con la stessa etichetta di cliente finale
- **Quando** uno dei due esegue la cancellazione riferita a quella persona
- **Allora** i dati dell'altro account sono intatti

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend e `compliance`; l'intera suite prima del commit);
- [ ] `SpesaModelliDataContract` implementato con tutte le tabelle in **esportazione e cancellazione**, e prova
      automatica che confronta l'elenco del manifesto con quello del contratto;
- [ ] **manifesto dei dati** `docs/compliance/manifests/spesa_modelli.yaml` completo, in italiano e inglese, con i
      campi annotati `@PersonalData`; controllo di parità delle lingue verde;
- [ ] prova che la cancellazione è **fisica** e che il totale di spesa non cambia quando si cancella un'etichetta;
- [ ] prova di **isolamento fra account** su esportazione e cancellazione;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` (storia `0034`) con il passo «cancello i
      dati riferiti a un cliente finale: le misure restano, il totale non cambia, la copertura scende», e
      aggiornare il registro di copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** dell'interfaccia in tutte e cinque le lingue, con revisione mirata della spiegazione degli
      effetti;
- [ ] registro dei trattamenti rigenerato dal manifesto e controllo di freschezza verde;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta di conservare la misura cancellando
      l'etichetta e sulla conservazione legata allo storico del piano;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0002` | Le tabelle di base e le colonne di controllo su cui il contratto agisce |
| Storia `0019` | Le etichette e le loro revisioni sono l'oggetto principale della cancellazione |
| Storie `0030`, `0031`, `0033` | Aggiungono le ultime tabelle con dati riferibili a persone: esportazioni, rapporti e recapiti, bozze |
| Storia `0034` | Il percorso end-to-end esiste e va esteso, non creato qui |

## 7. Fuori ambito

- la **risposta all'interessato** (chi scrive, con quali tempi, con quale testo): è di piattaforma e riguarda tutte
  le app, non questa;
- la **valutazione d'impatto** sul trattamento: non serve, perché l'app non tratta categorie particolari e le
  evita per costruzione (§6 del documento capofila) — ma la conclusione la conferma lo sviluppatore, non questa
  storia;
- l'**anonimizzazione statistica** dello storico oltre la conservazione (tenere i totali senza le righe): è
  un'idea sensata per accorciare i tempi di conservazione, ma è una funzionalità di prodotto e va decisa a parte;
- la **cancellazione presso i fornitori di modelli**: i dati stanno da loro per conto del cliente, con le
  credenziali del cliente; noi non possiamo e non dobbiamo cancellare nulla a casa d'altri.

## 8. Punti aperti

- **Il ruolo di appgrove sulle etichette che descrivono clienti e utenti finali** — titolare o responsabile del
  trattamento (punto P3 del documento capofila). Cambia l'accordo sul trattamento dei dati e l'informativa, e
  cambia anche **chi** riceve la richiesta di cancellazione: se siamo responsabili, la richiesta arriva al nostro
  cliente e noi eseguiamo su suo ordine. È una classificazione materialmente ambigua: la chiude lo sviluppatore,
  eventualmente con la revisione legale, **prima** che questa storia venga implementata.
- **Se applicare la conservazione anche ai dati aggregati** (la sintesi giornaliera della storia `0028`). Le sintesi
  non contengono etichette quando l'asse è tecnico, ma le contengono quando l'asse è «cliente finale». Proposta:
  stessa conservazione delle misure, e cancellazione dell'etichetta che si propaga alla sintesi. La conferma lo
  sviluppatore.
