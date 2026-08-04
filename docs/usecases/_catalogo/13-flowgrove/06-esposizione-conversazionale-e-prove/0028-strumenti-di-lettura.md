# 0028 — Strumenti di lettura

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 06 — Esposizione conversazionale e prove
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0013`, `0018`, `0025`, `0026`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che comanda la sua azienda da una chat
> voglio poter chiedere «sul lavoro per Rossi ci abbiamo guadagnato?» e «cosa devo fare oggi?» e ottenere una
> risposta vera
> così da avere le due informazioni che contano senza aprire l'applicazione e senza costruire un rapporto.

**Contesto.** Il catalogo pone a tutte le sessanta app il requisito di essere comandabili da una chat. Il livello
conversazionale della piattaforma **non esiste ancora** (epica `12-ready-for-ai-mcp`, casi d'uso 0061-0066,
scritti e non implementati): il dovere di questa storia non è costruire il server, ma **dichiarare il contratto
degli strumenti di lettura** e tenerlo versionato dentro il servizio dell'app
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §12). Per FlowGrove è il punto in cui l'app diventa utile
a chi non ci entrerebbe mai: la domanda sul margine di commessa
([application-description.md](../application-description.md) §7) oggi richiede di aprire una schermata che il
titolare apre una volta al mese, mentre a voce la farebbe ogni settimana. Gli strumenti di lettura non cambiano
nulla: il rischio da presidiare non è l'effetto irreversibile, è la **sovraesposizione di dati di lavoratori**
(§6 della descrizione).

## 2. Requisiti funzionali

1. **RF-1** — Sono dichiarati i **sei** strumenti di lettura, ciascuno con nome stabile, descrizione in lingua
   naturale, schema dei parametri, schema del risultato, marcatura *lettura* e dichiarazione di idempotenza:
   `list_projects(stato?, cliente?, pagina?)`, `get_project_progress(id_progetto)`,
   `get_my_tasks(periodo?)`, `search_tasks(testo?, progetto?, stato?, scadenza?, pagina?)`,
   `get_time_summary(progetto, periodo)`, `get_project_margin(id_progetto)`.
2. **RF-2** — I risultati sono **minimizzati**: `list_projects` restituisce codice, titolo, cliente, stato,
   avanzamento e budget residuo — non le attività né i commenti; `search_tasks` restituisce titolo, progetto,
   stato, scadenza e assegnatario — non descrizioni, commenti o allegati.
3. **RF-3** — `get_my_tasks` restituisce **solo** le attività di chi sta parlando: non accetta un parametro
   «persona» e non c'è modo di chiedere l'agenda di un collega. È lo stesso vincolo della schermata (storia 0013)
   e discende dai confini di prodotto sui dati dei lavoratori.
4. **RF-4** — `get_time_summary` e `get_project_margin` restituiscono importi **solo** a chi ha ruolo `admin`;
   a un `member` rispondono con le sole ore del progetto e senza valori economici, con una spiegazione. Nessuno
   dei due aggrega **per persona** fuori dal contesto di un progetto e di un periodo (§6 della descrizione).
5. **RF-5** — Gli strumenti riusano le rotte e i calcoli già esistenti (storie 0008, 0013, 0018, 0025, 0026):
   nessuna seconda implementazione del margine o della ricerca, altrimenti la chat e la schermata si mettono a
   dire numeri diversi.
6. **RF-6** — Il contratto è documentato e versionato dentro il servizio: cambiarne uno schema senza cambiare la
   versione dichiarata fa fallire una prova.

## 3. Requisiti tecnici

- **RT-1 — Esposizione conversazionale (§12).** Sei strumenti dichiarati con firma, descrizione, schemi e
  marcatura **lettura**; contratto dentro il servizio `progetti`, versionato con esso. Il server conversazionale è
  di piattaforma e non ancora implementato — **dipendenza dichiarata: casi d'uso 0061-0063**.
- **RT-2 — Isolamento fra account (§1).** Ogni strumento riceve il contesto dell'account dal livello di
  piattaforma e filtra per `tenant_id` preso dal token verificato: **mai** un parametro dell'account nello schema
  dello strumento. Un modello linguistico che inventasse un identificativo di account o di progetto altrui non
  deve poter ottenere nulla.
- **RT-3 — Interfaccia di programmazione (§2).** Gli strumenti si appoggiano alle rotte `/api/progetti/v1/*` già
  esistenti e ai loro servizi applicativi; errori in `application/problem+json`; nessuna rotta nuova.
- **RT-4 — Varchi e quota (§6, §7).** Le invocazioni attraversano gli stessi cinque varchi delle rotte
  (`401`/`403`/`402`/`403`/`429`). La lettura **non** consuma la metrica `seats`, che è a giacenza: leggere non
  occupa un posto. Con abbonamento `canceled` gli strumenti rispondono `402`.
- **RT-5 — Dati personali (§10).** Gli strumenti espongono dati di **lavoratori** (assegnatari, autori delle ore):
  la minimizzazione è un requisito, non un'ottimizzazione. Nessun campo di testo libero — note delle ore, commenti,
  descrizioni — entra nei risultati di elenco. Il manifesto `docs/compliance/manifests/progetti.yaml` dichiara in
  italiano e inglese che quei dati sono esposti **anche per questa via**.
- **RT-6 — Registrazione eventi (§14).** Ogni invocazione è registrata con `tenant_id`, `app_id`, `user_id`,
  identificativo di correlazione e nome dello strumento; **mai** i parametri di ricerca a testo libero, che
  possono contenere nomi di persone.
- **RT-7 — Prove (§11).** Prova di contratto sugli schemi; prova di isolamento fra due account su ognuno dei sei
  strumenti; prova che `get_my_tasks` non restituisca mai attività di un altro utente dello stesso account.

## 4. Criteri di accettazione

**CA-1 — La domanda sul margine**
- **Dato** un progetto con 120 ore fatturabili, una tariffa e due costi esterni, e un utente con ruolo `admin`
- **Quando** si invoca `get_project_margin(id_progetto)`
- **Allora** si ottengono ricavo previsto, costo delle ore, costi esterni e margine, con gli stessi valori della
  schermata di redditività (storia 0026)

**CA-2 — Il margine non si mostra a chi non deve vederlo**
- **Dato** lo stesso progetto e un utente con ruolo `member`
- **Quando** si invoca `get_project_margin(id_progetto)`
- **Allora** la risposta non contiene alcun importo e spiega che serve il ruolo `admin`

**CA-3 — L'agenda è solo la propria**
- **Dato** due collaboratori `Anna` e `Bruno` dello stesso account, ciascuno con attività assegnate
- **Quando** `Anna` invoca `get_my_tasks()` chiedendo nel testo le attività di `Bruno`
- **Allora** ottiene solo le proprie: lo schema non ha un parametro con cui esprimere la richiesta

**CA-4 — Risultati minimizzati**
- **Dato** un progetto con attività che hanno descrizioni lunghe e commenti
- **Quando** si invoca `search_tasks(progetto: …)`
- **Allora** il risultato contiene i soli campi dichiarati, paginati, e nessun testo libero

**CA-5 — Isolamento fra account**
- **Dato** un progetto dell'account `A`
- **Quando** uno strumento invocato nel contesto di `B` ne chiede l'avanzamento passando l'identificativo di `A`
- **Allora** ottiene la risposta che otterrebbe per un progetto inesistente, e l'evento registrato riporta il
  tentativo

**CA-6 — Contratto stabile**
- **Dato** una modifica allo schema di uno strumento senza cambio di versione
- **Quando** si eseguono le prove
- **Allora** la prova di contratto fallisce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (area `backend`);
- [ ] prove di **unità** sugli schemi e di **integrazione** sulle invocazioni, con database effimero e migrazioni
      vere;
- [ ] prova di **isolamento fra account** su tutti e sei gli strumenti, più la prova che `get_my_tasks` non
      attraversi le persone dentro lo stesso account;
- [ ] **prova end-to-end**: nessun impatto sulla superficie utente — il livello conversazionale non esiste ancora
      e non c'è nulla da percorrere; risposta scritta nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) e in `decisions.json`;
- [ ] **traduzioni**: non applicabile — le descrizioni degli strumenti sono in lingua naturale per il modello, non
      testo di interfaccia; nessuna stringa nuova nel modulo frontend;
- [ ] **manifesto dei dati** aggiornato con la nota sull'esposizione conversazionale dei dati dei lavoratori;
- [ ] **registro delle decisioni** compilato: elenco degli strumenti, campi restituiti, criterio di minimizzazione
      e regola di ruolo sugli importi;
- [ ] avvio locale invariato (`./dev.sh services`).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0008` | `search_tasks` riusa il filtro dell'elenco delle attività, non una seconda ricerca |
| storia `0013` | `get_my_tasks` riusa la regola di identità della schermata «Le mie attività» |
| storia `0018` | `get_time_summary` ha bisogno di tariffe e distinzione fatturabile/non fatturabile |
| storia `0025` | `get_project_progress` legge l'avanzamento già calcolato |
| storia `0026` | `get_project_margin` legge il margine già calcolato, con il suo filtro di ruolo |
| casi d'uso di piattaforma 0061-0063 (non implementati) | server conversazionale, autenticazione delegata e mappatura operazioni → strumenti; nel frattempo il contratto resta dichiarato e provato dentro il servizio |

## 7. Fuori ambito

- gli strumenti che **scrivono** — creazione, assegnazione, cambio di stato, ore, chiusura del periodo, consegna
  alla fatturazione: storia `0029`;
- la costruzione del server conversazionale e il consenso delegato: sono di piattaforma (casi d'uso 0061-0062);
- l'esposizione dei commenti e degli allegati in lettura: deliberatamente esclusa — sono contenuti di lavoro con
  testo libero, e il valore di leggerli da una chat non compensa l'esposizione (storie 0014, 0015).

## 8. Punti aperti

- **Quanto in profondità far arrivare `get_project_progress`.** Restituire anche l'elenco dei traguardi mancati
  rende la risposta molto più utile, ma allunga il risultato e avvicina la soglia oltre la quale un modello
  riassume male. La scelta va fatta guardando un risultato vero, e va registrata in `decisions.json`.
