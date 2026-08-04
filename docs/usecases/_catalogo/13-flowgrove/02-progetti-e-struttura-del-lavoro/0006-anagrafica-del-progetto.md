# 0006 — Anagrafica del progetto

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 02 — Progetti e struttura del lavoro
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di uno studio che ha appena preso un lavoro
> voglio aprire un progetto scrivendo solo il titolo, e completarlo dopo con cliente, date e budget
> così da non dover compilare una scheda di dodici campi prima di poter cominciare a lavorare.

**Contesto.** La lamentela più ricorrente sugli strumenti della categoria è la configurazione obbligatoria
all'avvio: piattaforme «per cui serve un corso»
([application-description.md](../application-description.md) §2.5). Il progetto è la prima entità che l'utente
incontra ed è dove quella lamentela si vince o si perde. La scheda deve essere utilizzabile con un titolo, e
crescere per aggiunte facoltative.

## 2. Requisiti funzionali

1. **RF-1** — Si crea un progetto con il solo **titolo**; tutto il resto è facoltativo e si aggiunge dopo.
2. **RF-2** — Il progetto può avere: codice (assegnato in automatico e modificabile), cliente (riferimento
   all'anagrafica condivisa oppure denominazione libera), referente con nome e recapito, data d'inizio e di fine
   prevista, note.
3. **RF-3** — Il progetto ha uno stato fra `bozza`, `attivo`, `sospeso`, `chiuso`, `archiviato`, con i passaggi
   ammessi: da `bozza` e da `sospeso` si va ad `attivo`, da `attivo` a `sospeso` o `chiuso`, da `chiuso` ad
   `archiviato`. La riapertura di un progetto chiuso è ammessa e resta tracciata.
4. **RF-4** — L'elenco dei progetti si filtra per stato e per cliente, si cerca a testo libero su titolo e codice,
   ed è paginato.
5. **RF-5** — Chiudere un progetto con righe di ore non ancora consegnate alla fatturazione produce un avviso che
   dice quante sono e che cosa succederà, e chiede conferma.
6. **RF-6** — Il progetto si cancella solo se non ha righe di ore; altrimenti si archivia. La cancellazione è
   logica.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `project` filtra per `tenant_id` preso dal
  token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/progetti/v1/projects`,
  `GET|PATCH /api/progetti/v1/projects/{id}`, `POST /api/progetti/v1/projects/{id}/state`; corpo validato in modo
  dichiarativo sugli oggetti di trasferimento; errori in `application/problem+json`; paginazione a pagina e
  dimensione con totale; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V2__progetto.sql` sullo schema `app_progetti`: la tabella `project` di
  0002 riceve i vincoli e gli indici d'uso (per stato, per cliente, ricerca sul titolo); chiave primaria UUID
  versione 7, colonne di controllo, cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Progetti* del modulo `progetti`: elenco, scheda e modulo di
  creazione; dati letti con il client generato; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe, compresi i nomi degli stati e i messaggi di conferma, in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Il progetto **non consuma quota**: la metrica è `seats`, e un progetto in
  più non occupa posti. Ruolo minimo per creare e chiudere: `admin`; `member` può leggere.
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati: `list_projects(stato?, cliente?)` in
  **lettura** (storia 0028); la creazione da chat non è prevista in questa stesura perché il progetto nasce quasi
  sempre da un preventivo (storia 0023).
- **RT-8 — Dati personali (§10).** Il referente (nome e recapito) è un dato personale di un terzo: voci nel
  manifesto in italiano e inglese, campi annotati `@PersonalData`, tabella `project` presente in `exportData` e
  `purgeData`.
- **RT-9 — Registrazione eventi (§14).** «Progetto creato», «stato cambiato», «progetto chiuso» con `tenant_id`,
  `app_id`, `user_id` e correlazione; **mai** il nome del cliente o del referente.

## 4. Criteri di accettazione

**CA-1 — Creazione con il solo titolo**
- **Dato** un utente con ruolo `admin`
- **Quando** crea un progetto scrivendo solo «Ristrutturazione uffici Rossi»
- **Allora** il progetto esiste in stato `bozza`, con un codice assegnato in automatico, e compare in elenco

**CA-2 — Passaggio di stato non ammesso**
- **Dato** un progetto in stato `archiviato`
- **Quando** si tenta di portarlo ad `attivo`
- **Allora** la risposta è `409` con la spiegazione dei passaggi ammessi, e lo stato non cambia

**CA-3 — Chiusura con ore non consegnate**
- **Dato** un progetto `attivo` con 12 ore dichiarate e non ancora consegnate alla fatturazione
- **Quando** si chiede la chiusura
- **Allora** compare un avviso che indica le 12 ore e chiede conferma; senza conferma nulla cambia

**CA-4 — Cancellazione impedita**
- **Dato** un progetto con righe di ore
- **Quando** si tenta di cancellarlo
- **Allora** la risposta è `409` e il messaggio propone l'archiviazione

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri progetti
- **Quando** un utente di `A` chiede l'elenco dei progetti
- **Allora** vede solo i propri, anche forzando l'identificativo dell'altro account nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla macchina a stati del progetto e di **integrazione** sulle rotte;
- [ ] prova di **isolamento fra account** su tutte le rotte introdotte;
- [ ] **prova end-to-end**: coprire ora — la creazione del progetto è il primo passo del percorso `[J-PROGETTI]`
      (storia 0031); voce nel registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per il referente del progetto;
- [ ] **registro delle decisioni** compilato, con annotata la scelta «creabile con il solo titolo» e il perché;
- [ ] contratto degli **strumenti conversazionali**: `list_projects` dichiarato (dettaglio nella storia 0028);
- [ ] controllo automatico di **accessibilità** verde su elenco e scheda;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0002` | Serve la tabella `project` |
| Storia `0003` | Serve il guscio del modulo dove mettere la sezione |

## 7. Fuori ambito

- il budget in ore e in importo: è della storia 0021, perché ha senso solo insieme al consumo;
- la nascita del progetto da un preventivo accettato: storia 0023;
- la struttura delle attività: storia 0007.

## 8. Punti aperti

- **Quanto legare il progetto all'anagrafica clienti condivisa**: qui il cliente può essere un riferimento
  logico oppure una denominazione libera. Se in futuro l'anagrafica condivisa diventasse obbligatoria, questa
  scelta va rivista insieme a 04 LeadGrove e 02 BillGrove — è una decisione di piattaforma, non di questa storia.
