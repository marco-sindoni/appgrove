# 0013 — Avviso di rinnovo con preavviso

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 03 — Rinnovi e scadenze
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un centro con abbonamenti annuali a rinnovo tacito
> voglio che i miei iscritti ricevano per tempo l'avviso che l'anno riparte, con la cifra e la scadenza
> così da non trovarmi contestazioni e richieste di rimborso, e da poterlo dimostrare se qualcuno lo nega.

**Contesto.** Non è una cortesia: è un **obbligo**. Le fonti consultate (§2.3 della descrizione) concordano su
un preavviso di almeno 30 giorni prima della data di rinnovo, con l'indicazione di data, durata, prezzo che si
applicherà e modalità semplici per recedere in tempo utile; e chi non avvisa espone il cliente al recesso libero
e immediato. Nello schema di addebito diretto, poi, esiste anche una **pre-notifica** dovuta al debitore prima
dell'addebito. Questa storia fa due cose insieme: manda l'avviso nei termini e **conserva la prova** di averlo
mandato — perché in una contestazione la prova vale quanto l'avviso.

**Una scelta di progetto da notare**: i giorni di preavviso sono un **dato del piano** (storia `0006`), non una
costante nel codice. È deliberato: il termine esatto è un punto per la revisione legale (punto aperto n. 3 della
descrizione) e cambia per giurisdizione. Se cambia il termine, si cambia un valore, non il programma.

## 2. Requisiti funzionali

1. **RF-1** — Per ogni abbonamento a rinnovo tacito, la lavorazione giornaliera manda l'avviso di rinnovo
   **N giorni prima** della data di rinnovo, dove N è il preavviso del piano.
2. **RF-2** — L'avviso contiene: data del rinnovo, durata del nuovo periodo, **importo che sarà dovuto** (dalla
   versione di prezzo agganciata), ultimo giorno utile per disdire, e un **collegamento diretto alla disdetta**
   (storia `0024`).
3. **RF-3** — Ogni invio genera una riga di **prova**: quando, a quale recapito, con quale contenuto e con quale
   esito di consegna.
4. **RF-4** — L'avviso si manda **una volta sola** per periodo: la lavorazione ripetuta non lo duplica.
5. **RF-5** — Se il recapito manca o l'invio fallisce, l'abbonamento compare in un elenco di **avvisi non
   recapitati** che il cliente deve poter vedere e risolvere: un avviso non arrivato è un rischio suo, e va
   messo davanti agli occhi.
6. **RF-6** — Il cliente può rimandare a mano un avviso, e anche questo lascia una riga di prova.
7. **RF-7** — Quando l'abbonamento ha un'autorizzazione all'addebito attiva, l'avviso vale anche come
   **pre-notifica** dell'addebito e ne riporta importo e data.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Avvisi e prove filtrati per `tenant_id` dal token verificato; la
  lavorazione elabora un account alla volta.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/abbonati/v1/avvisi-rinnovo` (con filtro
  «non recapitati») e `POST /api/abbonati/v1/avvisi-rinnovo/{id}/rinvia`; errori in `problem+json`; OpenAPI
  aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V8__avviso_di_rinnovo.sql`: tabella `avviso_di_rinnovo` con
  `tenant_id`, chiave UUID versione 7, colonne di controllo, e **vincolo di unicità** su abbonamento + periodo
  per garantire l'invio unico.
- **RT-4 — Comunicazioni.** Il messaggio si compone con il **renderer condiviso** della piattaforma (change
  `0079`), non con un modello scritto a mano dentro l'app: è infrastruttura comune e va usata.
- **RT-5 — Modulo frontend (§3, §5).** Nella panoramica, il riquadro «avvisi non recapitati» con l'azione di
  rinvio; nella scheda dell'abbonamento, gli avvisi mandati con il loro esito; solo token del sistema di design.
- **RT-6 — Cinque lingue (§4).** L'interfaccia in `en, it, fr, es, de`. **Attenzione a non confondere due cose**:
  la lingua dell'interfaccia è quella dell'utente del cliente; la lingua dell'**avviso** dev'essere quella
  dell'**abbonato**, che è un'altra persona (vedi punto aperto).
- **RT-7 — Varchi e quota (§6).** Nessun consumo di quota. Con abbonamento di piattaforma non attivo la
  lavorazione si ferma per quell'account.
- **RT-8 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura: mandare avvisi in blocco da chat
  non è ammissibile. La lettura degli avvisi non recapitati entra in `scadenze_non_incassate` (storia `0031`).
- **RT-9 — Dati personali (§10).** L'avviso conserva il **recapito usato** e la prova di consegna: voci nuove nel
  manifesto in italiano e inglese, campi annotati, tabella in `exportData` e `purgeData`. La conservazione della
  prova è il punto aperto n. 8 della descrizione.
- **RT-10 — Registrazione eventi (§14).** `avviso inviato`, `avviso fallito (causa)`, `avviso rinviato a mano`,
  con `tenant_id`, `app_id`, `user_id` e correlazione, **senza** il recapito.

## 4. Criteri di accettazione

**CA-1 — Avviso nei termini**
- **Dato** un abbonamento annuale a rinnovo tacito con preavviso di 30 giorni e rinnovo il 1° ottobre
- **Quando** gira la lavorazione del 1° settembre
- **Allora** parte un avviso che contiene data del rinnovo, durata, importo, ultimo giorno utile per disdire e il
  collegamento alla disdetta

**CA-2 — Invio unico**
- **Dato** l'avviso già mandato per quel periodo · **Quando** la lavorazione gira di nuovo
- **Allora** non parte alcun secondo avviso

**CA-3 — Recapito mancante**
- **Dato** un abbonato senza recapito valido · **Quando** matura il preavviso
- **Allora** l'abbonamento compare fra gli «avvisi non recapitati» con il motivo, e il cliente può intervenire

**CA-4 — Prova conservata**
- **Dato** un avviso mandato · **Quando** si apre la scheda dell'abbonamento
- **Allora** si vede quando è stato mandato, a quale recapito e con quale esito

**CA-5 — Isolamento fra account**
- **Dato** due account · **Quando** uno chiede l'elenco degli avvisi
- **Allora** vede solo i propri

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sul calcolo del giorno di invio e sull'invio unico; **integrazione** sulla lavorazione;
- [ ] prova di **isolamento fra account** su avvisi e prove;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-ABBONATI]` verifica che l'avviso parta e contenga il
      collegamento alla disdetta; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** dell'interfaccia in cinque lingue **e** del testo dell'avviso nelle lingue supportate per gli
      abbonati (vedi punto aperto);
- [ ] **manifesto dei dati** aggiornato con `avviso_di_rinnovo`, recapito compreso, in italiano e inglese;
- [ ] **registro delle decisioni** compilato: preavviso come dato del piano, prova d'invio conservata, avviso che
      vale anche come pre-notifica dell'addebito;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | serve la lavorazione giornaliera e la data di rinnovo |
| storia `0006` | il preavviso è un dato del piano |
| storia `0024` | l'avviso porta il collegamento alla disdetta: se la `0024` non c'è ancora, il collegamento manca e l'avviso è **incompleto rispetto all'obbligo** — le due vanno rilasciate insieme |
| renderer condiviso delle comunicazioni (change `0079`) | il messaggio si compone lì |

## 7. Fuori ambito

- il sollecito **dopo** il mancato incasso: storia `0021` — è un'altra comunicazione, con un'altra finalità;
- la disdetta vera e propria: storia `0024`;
- il canale di messaggistica breve: fuori dal nucleo (introdurrebbe un fornitore esterno).

## 8. Punti aperti

**In che lingua si scrive all'abbonato.** L'interfaccia parla la lingua dell'utente del cliente; l'avviso deve
parlare quella dell'**abbonato**, che è una terza persona di cui non sappiamo la lingua. **Proposta**: un campo
«lingua preferita» sull'abbonato, con predefinito quello dell'account, limitato alle cinque lingue di
piattaforma. Non l'ho messo fra i requisiti perché aggiunge un campo all'anagrafica della storia `0008` e va
deciso insieme a lei. Chiude: lo sviluppatore, con la direzione di prodotto.

**Il termine di 30 giorni.** È il numero che le fonti divulgative riportano, non un articolo di legge che ho
verificato (§2.7 della descrizione). Il progetto lo tratta come parametro proprio per questo. Chiude: revisione
legale.
