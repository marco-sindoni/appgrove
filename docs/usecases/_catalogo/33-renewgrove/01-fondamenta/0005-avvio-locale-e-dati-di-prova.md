# 0005 — Avvio locale e dati di prova

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che domani mattina deve lavorare sul punteggio di rischio
> voglio avviare lo stack locale con un comando e trovare l'app già piena di casi diversi fra loro
> così da non passare la prima ora a inventarmi clienti finti ogni volta che riparto da zero.

**Contesto.** Le storie `0001`-`0004` hanno prodotto un servizio, uno schema, un modulo e un varco. Manca la
verifica d'insieme: che l'app **si avvii davvero in locale senza passi manuali impliciti**, che è un dovere del
Definition of Done di ogni change che introduce un'applicazione nuova. Qui c'è una particolarità di RenewGrove che
rende i dati di prova più importanti che altrove: quest'app **non ha dati propri**, quindi uno stack locale con lo
schema vuoto non mostra niente e non permette di sviluppare nulla. I dati di prova non sono una comodità: sono la
condizione per lavorare sulle epiche successive. Sono tutti **inventati** — indirizzi nel dominio `*.test`, nomi di
fantasia — perché il divieto di dati veri nelle prove non ammette eccezioni e perché qui i dati veri sarebbero
clienti di qualcun altro.

## 2. Requisiti funzionali

1. **RF-1** — `./dev.sh services` mostra `fidelizzazione` con il suo `app_id`, la porta `8133` e lo schema
   `app_fidelizzazione`, derivati dal **solo**
   `services/fidelizzazione/src/main/resources/application.properties`.
2. **RF-2** — `./app-start.sh` avvia l'app e `./app-stop.sh` la ferma, senza alcuna modifica manuale agli script;
   `dev migrate` applica le migrazioni e `dev service fidelizzazione` avvia il singolo servizio.
3. **RF-3** — La rotta `/api/fidelizzazione/v1/*` è presente nel `dev/Caddyfile`, dentro il blocco rigenerato fra i
   marcatori `api-routes`; il file non si modifica a mano.
4. **RF-4** — Il modulo frontend è abilitato nello stub locale di abilitazione, così che la barra laterale lo mostri
   in locale finché l'abilitazione reale non esiste.
5. **RF-5** — Esiste un insieme di dati di prova, caricabile con un comando e ricaricabile su un database vuoto,
   con almeno due account distinti — serve a rendere visibile a occhio l'isolamento — più fonti in tutti e tre gli
   stati (`collegata`, `sospesa`, `scollegata`), rapporti i cui segnali portano a **ciascuna delle fasce di
   rischio** previste, e segnali distribuiti nel tempo in modo che le finestre di osservazione abbiano senso.
6. **RF-6** — L'insieme di dati di prova è **un solo artefatto che cresce**: le storie che introducono tabelle
   nuove vi aggiungono le proprie righe invece di crearne un secondo. In particolare gli interventi in tutti gli
   stati della macchina del §4.4 della [descrizione](../application-description.md) — `bozza`, `confermato`,
   `consegnato`, `eseguito`, `annullato` — entrano qui non appena la tabella esiste (storia `0019`).
7. **RF-7** — Nessun dato di prova assomiglia a una persona reale: nomi di fantasia, indirizzi `*.test`, importi e
   intensità inventati; nessun identificativo fiscale, nemmeno finto ma formalmente valido.

## 3. Requisiti tecnici

- **RT-1 — Avvio locale automatico (§15).** La scoperta automatica ([dev/lib/services.sh](../../../../../dev/lib/services.sh)
  per lo stack locale, [tools/ci/services.sh](../../../../../tools/ci/services.sh) per l'integrazione continua)
  ricava la mappa servizio → `app_id` → porta → schema dal solo `application.properties`. Il dovere della storia è
  **dichiarare bene quelle proprietà**: se viene voglia di incollare una riga in uno script, è un difetto della
  scoperta automatica, non un passo del lavoro.
- **RT-2 — Isolamento fra account (§1).** I dati di prova comprendono due account con rapporti che si somigliano di
  proposito (stessa etichetta di fantasia, stesso tipo di segnali): è il modo più diretto per accorgersi a occhio di
  una perdita di isolamento durante lo sviluppo, oltre alle prove automatiche di `0002`.
- **RT-3 — Persistenza (§8).** I dati di prova **non** sono una migrazione Flyway: sono un artefatto separato,
  caricato solo in locale e nelle prove, così che non possano finire in produzione.
- **RT-4 — Modulo frontend (§3, §5).** Con i dati caricati, le cinque sezioni del modulo mostrano contenuto invece
  dello stato vuoto; solo token del sistema di design, tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le etichette dei dati di prova sono nomi di fantasia e non passano dalle
  traduzioni; ogni testo dell'interfaccia che la storia tocca resta nello spazio-nomi `fidelizzazione` in
  `en, it, fr, es, de`.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo: i dati di prova sono **inventati** e non riguardano
  persone esistenti. Questo va scritto nel registro delle decisioni, perché è la ragione per cui l'insieme può stare
  nel repository.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento introdotto.
- **RT-8 — Registrazione eventi (§14).** Il caricamento dei dati di prova si registra come operazione di sviluppo,
  con `tenant_id`, `app_id`, `user_id` e correlazione; non gira in produzione.
- **RT-9 — Prove (§11).** L'area `smoke` di `run-tests.sh` avvia realmente l'artefatto e verifica che l'app
  risponda; una prova verifica che il blocco `api-routes` del `dev/Caddyfile` contenga la rotta
  `/api/fidelizzazione/v1/*`; una prova verifica che i dati di prova si carichino su un database vuoto e siano
  ricaricabili senza errori.

## 4. Criteri di accettazione

**CA-1 — L'app è scoperta e si avvia**
- **Dato** un repository appena clonato
- **Quando** si esegue `./dev.sh services` e poi `./app-start.sh`
- **Allora** `fidelizzazione` compare con porta `8133` e schema `app_fidelizzazione`, si avvia e risponde, e
  **nessuno script è stato modificato a mano**

**CA-2 — La rotta locale esiste**
- **Dato** lo stack locale avviato
- **Quando** si chiama `/api/fidelizzazione/v1/` attraverso il proxy locale
- **Allora** la richiesta raggiunge il servizio, e la rotta risulta dentro il blocco rigenerato del `dev/Caddyfile`

**CA-3 — Il modulo compare in locale**
- **Dato** lo stub locale di abilitazione · **Quando** si apre il backoffice in locale
- **Allora** RenewGrove è nella barra laterale e le sue cinque sezioni sono raggiungibili

**CA-4 — I dati di prova coprono i casi**
- **Dato** un database vuoto · **Quando** si caricano i dati di prova
- **Allora** esistono almeno due account, fonti nei tre stati e rapporti i cui segnali portano a ciascuna fascia di
  rischio prevista; ricaricando una seconda volta non si duplica nulla

**CA-5 — Niente che assomigli a una persona vera**
- **Dato** l'insieme dei dati di prova
- **Quando** lo si ispeziona
- **Allora** ogni indirizzo sta nel dominio `*.test`, ogni nome è di fantasia, e non compaiono identificativi
  fiscali

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `smoke`; l'intera suite prima del
      commit);
- [ ] prove di **integrazione** sul caricamento e sul ricaricamento dei dati di prova, con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account**: i due account dei dati di prova sono usati anche dalle prove di
      isolamento della storia `0002`;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-FIDELIZZAZIONE]` nasce con la storia `0030` e userà
      **questi** dati di prova; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con motivo e storia proprietaria;
- [ ] **traduzioni**: nessun testo visibile nuovo;
- [ ] **manifesto dei dati**: nessuna modifica — i dati di prova sono inventati;
- [ ] **registro delle decisioni** compilato: dati di prova come artefatto separato dalle migrazioni, due account
      di proposito, e perché l'insieme è uno solo e cresce;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali — è il criterio centrale della storia;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | servono il servizio e le proprietà da cui la scoperta automatica ricava tutto |
| storia `0002` | servono le tabelle in cui caricare i dati di prova |
| storia `0003` | serve il modulo da abilitare nello stub locale |

## 7. Fuori ambito

- le righe di prova su punteggi, contributi e interventi: le tabelle non esistono ancora (epiche 03 e 04) e le
  aggiungeranno le storie `0013` e `0019` **allo stesso** insieme di dati, senza crearne un secondo;
- l'ambiente di prova remoto e l'ambiente di produzione: qui si parla solo di locale;
- il percorso end-to-end: storia `0030`.

## 8. Punti aperti

**Chi tiene aggiornati i dati di prova quando il modello cambia.** Un insieme di dati che invecchia è peggio di
nessun insieme, perché fallisce in modo confuso. La raccomandazione è che il caricamento faccia parte dell'area
`smoke` di `run-tests.sh`, così che una migrazione incompatibile diventi rossa subito invece di sorprendere il primo
che riparte da zero. Chiude: lo sviluppatore, in fase di implementazione.
