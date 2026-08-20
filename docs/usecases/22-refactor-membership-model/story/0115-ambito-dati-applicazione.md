# UC 0115 — Ambito dei dati di un'applicazione: del gruppo di lavoro o della persona

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.4 Dentro le applicazioni](../epic/E22-04-app-e-industrializzazione.md)
**Dipendenze**: UC 0098 (accesso per applicazione), UC 0099 (autorizzazione), UC 0101 (semantica dei ruoli), UC 0114 (ritiro della categoria che questo campo sostituisce), UC 0032 (esportazione e cancellazione)
**Piano di lavoro**: [task/0115](../task/0115-ambito-dati-applicazione.md)
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Sostituire la categoria ritirata con la distinzione che **conta davvero**: i dati di un'applicazione sono
**del gruppo di lavoro** (chiunque vi abbia accesso li vede tutti) oppure **della persona che li ha
creati** (ognuno vede soltanto i propri)?

Al contrario della categoria B2C/B2B, questa distinzione ha **conseguenze verificabili nel codice**: cambia
il filtro delle interrogazioni, non un'etichetta.

L'ambito è una **caratteristica dichiarata dall'applicazione alla nascita**; il filtro che la fa rispettare
è la sua **conseguenza**, e si costruisce quando esiste la prima applicazione che lo richiede. Questa storia
fa la prima parte e **progetta** la seconda.

**Incluso**: il campo che dichiara l'ambito, dichiarato dal listino dell'applicazione come gli altri
attributi; la **guardia** che impedisce di rilasciare un'applicazione ad ambito `utente` finché il filtro
non esiste; il rapporto fra ambito e ruoli; il rapporto fra ambito e diritti dell'interessato; che cosa
vede l'owner; la sorte dei dati quando una persona esce; la domanda che il copilota della skill deve fare;
il **piano** del meccanismo di persistenza, scritto per intero in [task/0115](../task/0115-ambito-dati-applicazione.md) §2.

**Escluso**: la **costruzione** del meccanismo di filtro — la fa la prima applicazione ad ambito `utente`,
che non nasce in questa epica (vedi §6 e i punti aperti): l'elenco dei percorsi da esentare si conosce
davvero solo col caso concreto davanti. Escluso anche il ritiro della vecchia categoria →
[UC 0114](0114-ritiro-categoria-b2c-b2b.md); il varco dei ruoli → UC 0099.

**Perché il campo si fa comunque adesso, e non insieme al filtro**: ogni applicazione deve nascere
classificata. Aggiungere il campo dopo significherebbe tornare indietro a classificare le applicazioni
esistenti e migrare i loro listini — lavoro fatto due volte. E la domanda sull'ambito si risponde bene
mentre l'applicazione si progetta, non mesi dopo.

## 2. Attori & ruoli

- **Chi definisce l'applicazione**: dichiara l'ambito alla nascita (una domanda del copilota, UC 0112).
- **Persone con accesso all'applicazione**: vedono tutto o solo il proprio, secondo l'ambito.
- **Owner**: vedi §5, ed è il punto più delicato della storia.
- **Sistema**: quando il filtro esisterà, lo applicherà **per costruzione** e non per disciplina di chi
  scrive le interrogazioni (§4.4). Per ora fa una cosa sola, e la fa bene: **impedisce** di rilasciare
  un'applicazione che dichiara `utente` senza filtro.

## 3. Precondizioni

- L'accesso per applicazione esiste (UC 0098) e il ruolo si fa rispettare (UC 0099).
- La vecchia categoria è ritirata (UC 0114): il campo nuovo non le si affianca, la sostituisce.
- Esistono le vie di conformità sull'account (esportazione, cancellazione) — UC 0032, UC 0033.

## 4. Flusso principale

1. Ogni applicazione dichiara il proprio **ambito dei dati**, con due valori possibili:
   - **`account`** (predefinito): i dati appartengono al gruppo di lavoro. Chi ha accesso
     all'applicazione li vede **tutti**. È il comportamento di oggi, e resta quello delle due
     applicazioni esistenti;
   - **`utente`**: i dati appartengono alla persona che li ha creati. Ognuno vede **solo i propri**,
     qualunque sia il suo ruolo.
2. Il valore è dichiarato nel **listino** dell'applicazione, come gli altri attributi, e sincronizzato
   dallo stesso meccanismo: nasce col codice dell'applicazione, non si imposta a mano.
3. **Finché il filtro non è costruito, `utente` è dichiarabile ma non rilasciabile.** Un'applicazione che
   lo dichiara fa **fallire** il controllo dei listini e l'avvio del servizio, con un messaggio che dice
   perché e che rimanda al piano. È la guardia che impedisce a questo campo di diventare la promessa vuota
   che [UC 0114](0114-ritiro-categoria-b2c-b2b.md) ha appena ritirato: qui la promessa sarebbe di
   riservatezza, e una riservatezza creduta e non applicata è peggio di nessuna riservatezza.
4. Quando il filtro verrà costruito (piano in [task/0115](../task/0115-ambito-dati-applicazione.md) §2), il
   comportamento sarà questo: ogni riga porta l'**identità di chi l'ha creata**, valorizzata dal sistema e
   mai dal corpo della richiesta (come per l'account); ogni lettura e ogni scrittura filtra **anche** per
   quella identità, e il filtro è **automatico** — acceso all'inizio della richiesta, non scritto a mano in
   ogni interrogazione. I paragrafi §5, §6 e §8 descrivono quel comportamento: sono il contratto che il
   filtro dovrà rispettare, deciso adesso perché adesso il modello è fresco.
5. **L'ambito precede il ruolo.** Il ruolo dice *che cosa* puoi fare, l'ambito dice *su quali dati*. Un
   `viewer` in un'applicazione ad ambito `utente` legge tutti i **propri** dati; un `editor` modifica i
   **propri**. La frase del contratto dei ruoli — «il `viewer` vede tutti i dati dell'applicazione» — va
   letta come «tutti i dati **che l'ambito gli attribuisce**», e UC 0101 va aggiornato di conseguenza.
6. La **gestione utenti** dell'applicazione (UC 0111) resta identica: l'ambito riguarda i dati, non gli
   accessi. Anche in un'applicazione ad ambito `utente` un `admin` abilita persone e cambia ruoli — non
   per questo legge i loro dati.

## 5. Il nodo della storia: che cosa vede l'owner

Due esigenze legittime si scontrano, e vanno **separate** invece di scegliere una delle due:

- **Visibilità operativa**: nelle applicazioni ad ambito `utente`, l'owner **non** vede i dati degli altri
  dentro l'applicazione. Se li vedesse, la promessa fatta alle persone sarebbe falsa e l'ambito non
  significherebbe nulla.
- **Titolarità**: i dati delle applicazioni appartengono comunque all'**account**, non all'operatore
  (decisione già in vigore, UC 0032). L'owner è il titolare del trattamento verso le persone del suo
  gruppo di lavoro, e ha obblighi che richiedono di poterli **ottenere**: esportazione dell'account,
  risposta a una richiesta di accesso, chiusura dell'account con cancellazione.

La regola è quindi: **l'ambito limita la visibilità nell'interfaccia dell'applicazione, non la titolarità
dei dati.** L'esportazione dell'account (art. 15/20, via di conformità) comprende **tutto**, compresi i
dati ad ambito `utente`; la schermata dell'applicazione no. Le due cose passano da percorsi diversi, e i
percorsi di conformità **disabilitano esplicitamente** il filtro per utente.

Questo va **scritto nell'informativa**: chi lavora in un'applicazione ad ambito `utente` deve sapere che i
suoi dati non sono visibili ai colleghi nell'applicazione, ma **sono** del titolare dell'account, che può
ottenerli per adempiere ai propri obblighi. Dirlo è più onesto che lasciare intendere una riservatezza che
non c'è.

## 6. Flussi alternativi / edge / errori

- **Edge — persona rimossa dall'account** con dati in un'applicazione ad ambito `utente`: i dati **non si
  cancellano** (sono dell'account) e **non si riassegnano** in automatico a nessuno. Restano con
  l'identità originale, raggiungibili dalle vie di conformità dell'owner. La cancellazione avviene con le
  regole di conservazione già stabilite.
- **Edge — un'applicazione cambia ambito** dopo la nascita: da `utente` a `account` è un allargamento di
  visibilità e **non** è un cambio innocuo (persone che avevano dati riservati se li vedono condivisi):
  non è self-service e richiede una decisione consapevole. Da `account` a `utente` richiede di attribuire
  righe esistenti a qualcuno. Fuori scope: punto aperto.
- **Errore — filtro non applicabile** (identità del chiamante non risolvibile): si **nega**, come per
  l'account. Nessuna lettura «senza filtro» per comodità.
- **Edge — lavori periodici e consumatori di coda** (spazzini di conservazione, esportazioni, purghe):
  girano **fuori** da una richiesta utente e non hanno un'identità. Devono lavorare con il filtro
  **disattivato in modo esplicito e dichiarato**, mai per caso: un filtro che si spegne di nascosto è un
  varco.
- **Edge — quote e conteggi**: la quota di un'applicazione conta il consumo **dell'account**, non della
  singola persona, anche ad ambito `utente`. Il limite è dell'abbonamento, che è dell'account. Va detto
  chiaramente perché è controintuitivo.
- **Edge — nessuna applicazione ha ambito `utente`, e nessuna ne avrà in questo rifacimento**: Fatture e
  Mini-CRM sono entrambe `account` e lo restano. È una **decisione dello sviluppatore**: le applicazioni
  vengono dopo il rifacimento dell'appartenenza, non dentro. Da qui la forma di questa storia — si dichiara
  la caratteristica, non si costruisce il filtro che nessuno userebbe — e la guardia di §4.3, che è ciò che
  tiene onesta la dichiarazione. Il **generatore** ([tools/new-application/generate.mjs](../../../../tools/new-application/generate.mjs))
  emette perciò solo entità ad ambito `account`; la **skill** ([.claude/skills/new-application/](../../../../.claude/skills/new-application/))
  fa comunque la domanda, e alla risposta `utente` si ferma.

## 7. Dati toccati

- **Nuovo campo dell'applicazione**: `data_scope` su `platform.app` (`account` | `utente`), con
  predefinito `account`. Dichiarato nel listino come codice, come già avviene per gli altri attributi
  dell'applicazione, e sincronizzato dallo stesso meccanismo. **Prende il posto** del campo ritirato da
  UC 0114 — un campo che significa qualcosa al posto di uno che non significava più nulla.
- **Nuova colonna, quando il filtro arriverà**: `owner_user_id` nelle entità delle applicazioni ad ambito
  `utente` — l'identità di chi ha creato la riga (il `sub` del token, come già si fa per i posti del
  Mini-CRM). **Non** è un dato personale nuovo: è l'identificativo di autenticazione, già trattato e già
  dichiarato. Nessuna tabella la riceve in questa storia, perché nessuna applicazione ha ambito `utente`.
- **Manifesti dati e informativa, quando il filtro arriverà**: un'applicazione ad ambito `utente` va
  **dichiarata** come tale nel proprio manifesto, perché cambia *chi vede che cosa* — materia di
  informativa, non solo di codice (§5). Si scrive col caso concreto, non a vuoto: la voce resta nei punti
  aperti e in [docs/_REVISIONE-LEGALE.md](../../../_REVISIONE-LEGALE.md).

## 8. Permessi & gate

- **Account solo dal token verificato**, e **identità solo dal token verificato**: valgono le stesse
  regole, per la stessa ragione.
- **Filtro acceso per difetto, spento solo dove dichiarato**: i percorsi che vedono tutto sono un elenco
  chiuso e scritto (conformità, lavori periodici), non una scelta di chi scrive l'interrogazione.
- **L'ambito non sostituisce il ruolo**: un `viewer` non diventa `editor` perché i dati sono suoi. I due
  presidi si sommano.
- **L'owner non aggira l'ambito dall'interfaccia dell'applicazione**; lo raggiunge dalle vie di
  conformità, che lasciano traccia. La differenza fra «vedere» e «poter ottenere con traccia» è il cuore
  della storia.

## 9. Requisiti di test

**In questa storia** — il campo e la guardia:

- **Unità**: il campo si legge dal listino, il valore mancante vale `account`, un valore ignoto è un errore.
- **La prova che conta qui — la guardia**: un listino di prova che dichiara `utente` fa **fallire** il
  controllo dei listini e l'avvio del servizio. È la prova che il campo non è una promessa vuota, ed è il
  motivo per cui questa storia può chiudersi senza il filtro.
- **Console di piattaforma**: la colonna mostra l'ambito; le due applicazioni reali dichiarano `account`.
- **Percorsi end-to-end**: nessuno proprio; nel registro come *senza superficie*.

**Con la prima applicazione ad ambito `utente`** — i collaudi del filtro, scritti qui perché sono il
contratto da rispettare, non da inventare allora:

- **Unità**: la funzione che decide se il filtro va acceso (ambito dell'applicazione × tipo di percorso).
- **Integrazione, il collaudo che conta**: due identità diverse, sulla stessa tabella ad ambito `utente`,
  creano una riga ciascuna e **non** vedono quella dell'altra — in lettura, in modifica e in cancellazione.
  Ad ambito `account`, la vedono entrambe.
- **Integrazione — owner**: non vede i dati altrui nell'applicazione; **li ottiene** dall'esportazione
  dell'account. Due prove distinte, ed è la coppia che dimostra la separazione fra visibilità e titolarità.
- **Integrazione — lavori periodici**: con filtro disattivato vedono tutto; senza dichiarazione esplicita
  non partono.
- **Sicurezza**: nessuna interrogazione di un'applicazione ad ambito `utente` restituisce righe di un'altra
  persona; prova per ogni operazione esposta, come si fa per la separazione fra account.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [BaseTenantEntity.java](../../../../services/commons/src/main/java/app/appgrove/commons/persistence/BaseTenantEntity.java)
  e [JwtTenantResolver.java](../../../../services/commons/src/main/java/app/appgrove/commons/tenancy/JwtTenantResolver.java)
  (il filtro per account, che **non** si può riusare: il discriminatore di multitenancy è uno solo);
  [Seat.java](../../../../services/crm/src/main/java/app/appgrove/crm/Seat.java) come precedente di una
  colonna con l'identità del chiamante; UC 0032 per la titolarità dei dati delle applicazioni.
- **Definition of Done** (di questa storia):
  1. l'ambito è dichiarato dal listino dell'applicazione e sincronizzato, con `account` come predefinito;
  2. le due applicazioni esistenti dichiarano `account`, e la console di piattaforma mostra l'ambito;
  3. la **guardia** funziona: un'applicazione che dichiara `utente` non passa il controllo dei listini e non
     si avvia, con un messaggio che rimanda al piano — provato da un collaudo;
  4. il copilota della skill fa la domanda e si ferma se la risposta è `utente` (UC 0112);
  5. il contratto dei ruoli (UC 0101) è aggiornato: l'ambito precede il ruolo;
  6. il contratto che il filtro dovrà rispettare è scritto (§5, §6, §8) e il suo piano è pronto
     ([task/0115](../task/0115-ambito-dati-applicazione.md) §2), con la voce nei punti aperti;
  7. `./run-tests.sh` intero verde.

  **Fuori da questa Definition of Done, per scelta**: la costruzione del filtro, i suoi collaudi di
  isolamento e il testo dell'informativa — vanno con la prima applicazione ad ambito `utente`.

## Punti aperti / decisioni differite

- **Cambio di ambito a posteriori**: fuori scope. Da `utente` ad `account` è un allargamento di visibilità
  con effetti sulle persone e va trattato come decisione di prodotto, non come impostazione.
  Proprietario: questa storia.
- **Un terzo ambito, in due forme diverse che non vanno confuse.** *Orizzontale*: i dati sono personali ma
  la persona può condividerne alcuni coi colleghi — la forma delle applicazioni di appunti e documenti.
  *Verticale* (supervisione): i dati sono personali ma qualcuno **deve** vederli perché è la finalità stessa
  dell'applicazione — l'esempio è la formazione, dove il responsabile vede gli esiti degli esami. Valutata e
  **messa da parte** (2026-08-20): guardata da vicino, la supervisione non è un terzo valore dello stesso
  campo ma un modello più fine — la condivisione riguarda il **singolo dato** (l'esito sì, gli appunti del
  corso no), la supervisione spetta a un **ruolo dell'applicazione** e non all'owner, e deve essere una
  lettura **non modificabile** (un esito d'esame alterabile non vale nulla). Deciderne la forma senza avere
  davanti l'applicazione che la richiede significherebbe scegliere male con buone intenzioni. Costo del
  rimando: nullo, perché il filtro non è ancora costruito e aggiungere un valore a un campo dichiarativo è
  una migrazione banale. Proprietario: questa storia.
- **Formulazione dell'informativa** su ambito e titolarità (§5): va scritta con cura e portata alla
  revisione legale. Annotata in [docs/_REVISIONE-LEGALE.md](../../../_REVISIONE-LEGALE.md).
- **Costruzione del meccanismo di filtro** — la parte più sostanziosa, deliberatamente rimandata alla
  **prima applicazione ad ambito `utente`**. Non è un rimando al buio: il piano è scritto per intero in
  [task/0115](../task/0115-ambito-dati-applicazione.md) §2 (classe base, filtro di sessione parametrico,
  attivazione per difetto, elenco chiuso dei percorsi esenti) e il contratto da rispettare è in §5, §6 e §8
  di questa storia. Insieme al meccanismo vanno: i collaudi di isolamento (§9, secondo blocco), il supporto
  del **generatore** (entità che estendono la classe base e collaudi generati), il manifesto e il testo
  dell'informativa. Fino ad allora la guardia di §4.3 impedisce di credere di avere il filtro.
  Proprietario: questa storia; la guardia è ciò che ne rende impossibile la dimenticanza.
- **Percorsi end-to-end dell'ambito `utente`**: li porterà la prima applicazione reale, che toglierà anche
  l'esenzione *senza superficie* dal registro di copertura. Proprietario: questa storia.
- **Quote per persona** invece che per account nelle applicazioni ad ambito `utente`: non previste (il
  limite è dell'abbonamento). Se servisse, è una metrica nuova, non una variante dell'ambito.
  Proprietario: UC 0027.
