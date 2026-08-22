# Requisiti — change 0099 · UC 0104 «Riduzione dei posti in attesa»

**Use case sorgente**: [docs/usecases/22-refactor-membership-model/story/0104-riduzione-posti-in-attesa.md](../../docs/usecases/22-refactor-membership-model/story/0104-riduzione-posti-in-attesa.md)
**Piano di lavoro**: [task/0104](../../docs/usecases/22-refactor-membership-model/task/0104-riduzione-posti-in-attesa.md)
**Epica**: E22.2 Posti a pagamento · **Dipende da**: UC 0102 (listino), UC 0103 (acquisto del posto, change 0098)
**Modalità**: fast (dichiarata dall'orchestratore `go-fast`) · **Aree**: `services/core`, `frontend/`, documentazione

## 1. Perché questa change

La change 0098 ha reso l'aggiunta di una persona un atto che passa dalla cassa: si paga il posto e poi
l'invito nasce. La quantità dell'abbonamento dei posti però **sale soltanto**. Manca quindi tutto il
verso opposto: un account che ha assunto e poi ha ridotto il gruppo di lavoro continua a pagare posti
che nessuno occupa, e non esiste alcun modo di dire alla piattaforma «da fine mese siamo in nove».

Questa change introduce quel verso, nella forma che l'epica ha scelto: la riduzione **non è immediata**.
L'owner indica le persone da cessare, l'account entra in **riduzione in attesa**, nessun posto nuovo si
aggiunge finché quell'attesa non si chiude, e alla scadenza del periodo già pagato la riduzione si esegue
davvero. L'attesa si annulla in qualunque momento, senza alcun effetto contabile.

Tre rimandi espliciti della change 0098 si chiudono qui, e sono la fondazione del lavoro:

1. il varco «nessun invito con una riduzione in attesa» ha il suo punto esatto segnato da un commento in
   `InvitationResource.create`, passo (3);
2. il campo `pendingReduction` è già nel contratto di rete di `GET /me/seats` e vale sempre falso: questa
   storia deve solo farlo diventare vero;
3. la quantità dell'abbonamento dei posti scende **solo** qui, e il punto di scrittura esiste già
   (`SeatSubscriptionWriter.restoreQuantity`).

## 2. Perimetro

**Incluso**

- l'atto di **indicare** una o più persone per la cessazione, come atto unico con data di esecuzione comune;
- lo stato di **riduzione in attesa** a livello di account, con la sua unicità garantita in banca dati;
- l'**effetto mostrato prima della conferma**: data di cessazione, posti risultanti, dovuto prima e dopo,
  composizione degli scaglioni che si applicherà;
- il **blocco delle aggiunte** durante l'attesa, con presidio nel servizio e non solo a schermo;
- l'**annullamento** dell'intera attesa e la **rimozione di una singola persona** dall'elenco degli indicati,
  con chiusura automatica quando gli indicati arrivano a zero;
- l'**esecuzione alla scadenza**: rimozione logica delle persone, cancellazione dei loro accessi alle
  applicazioni, invalidazione delle copie locali dei ruoli, discesa della quantità dell'abbonamento;
- la **robustezza** dell'esecuzione (ripetibile senza danni) e la **misura** delle riduzioni scadute e non
  eseguite;
- l'**ordine rispetto al rinnovo**: la riduzione si esegue prima che il periodo nuovo dell'abbonamento dei
  posti venga scritto;
- la **superficie** in «Members»: selezione multipla, effetto prima della conferma, riquadro di avviso,
  stato «in cessazione dal …» nell'elenco unico, comando di invito spento con spiegazione;
- il **percorso end-to-end** (indica → invito bloccato → annulla → invito riuscito) e il registro di
  copertura.

**Escluso** (e dove sta)

- il calcolo delle tariffe → UC 0102, già fatto (change 0097);
- l'acquisto del posto → UC 0103, già fatto (change 0098);
- la fattura e la sezione «Billing» dei posti → UC 0106;
- il **rinnovo del periodo** dell'abbonamento dei posti (chi fa avanzare `current_period_end` quando il mese
  scade) → UC 0106, rimando già scritto dalla change 0098. Questa storia si aggancia al periodo esistente,
  non lo fa avanzare;
- l'**avviso per email alla persona indicata** → punto aperto di questa storia, deliberatamente non fatto
  (§7);
- la schermata «Gestione utenti» dentro le applicazioni, da cui si tolgono gli accessi subito → UC 0111.

## 3. Modello dati

Migrazione `V23__seat_downgrade.sql`, due tabelle nello schema `platform`:

- **`seat_downgrade`** — la riduzione come **atto**: `tenant_id`, `execute_at` (fine del periodo già
  pagato), `status` (`pending` | `executed` | `cancelled`), `requested_by` (identità di chi l'ha chiesta),
  `executed_at`, campi di audit e `deleted_at`.
  **Vincolo non negoziabile**: indice unico **parziale** su `tenant_id` per le sole righe `pending` vive —
  una sola riduzione in attesa per account, garantita dalla banca dati e non da un controllo applicativo;
- **`seat_downgrade_item`** — le **persone indicate**: `downgrade_id`, `identity_id`, con unicità parziale
  sulla coppia per le righe vive (togliere e reindicare deve restare possibile).

Alternativa scartata (come dice lo use case §7): un contrassegno sulla riga della persona. Non permetterebbe
di conoscere la data comune né di annullare l'insieme come atto unico.

`platform.subscription.quantity` **non cambia** all'atto dell'indicazione: la riduzione è *programmata*.
Scende solo all'esecuzione.

## 4. Servizio

Pacchetto `app.appgrove.core.billing.seats`.

- **`SeatDowngradeService`** — il percorso *dentro una richiesta autenticata* (entità tenant-scoped, filtro
  per account automatico):
  - `request(userIds)`: rifiuta l'owner, rifiuta la seconda riduzione, rifiuta un elenco vuoto o una persona
    che non appartiene all'account, rifiuta se **non esiste** l'abbonamento dei posti (v. §5), calcola
    `execute_at` dalla fine del periodo dell'abbonamento, crea le righe. Non tocca la quantità;
  - `preview(userIds)`: l'effetto prima della conferma, con tutti i numeri già calcolati dal servizio;
  - `cancel()`: chiude come annullata, nessun effetto contabile;
  - `removeItem(identityId)`: toglie una persona; a zero indicati la riduzione si chiude da sé (annullata);
  - `pending()`: lo stato corrente, per la lettura del riquadro;
  - `blocksAdditions()`: il predicato che l'invito interroga.
- **`SeatDowngradeExecutor`** — il percorso *fuori da una richiesta autenticata* (lavoro periodico e
  consumatore degli eventi del fornitore): interfaccia di programmazione verso la banca dati **a SQL
  nativo con account esplicito**, come `AccountDeletionSweeper` e `TenantOffboarding`, perché il risolutore
  del perimetro di Hibernate è chiuso in assenza di token. Idempotente per costruzione.
- **`SeatDowngradeSweeper`** — periodico (ogni ora), una transazione per account: un guasto su un account
  non blocca gli altri.
- **`SeatDowngradeMetrics`** — la misura «riduzioni scadute e non eseguite», con avviso di severità alta
  sopra zero. Senza questa misura una riduzione non eseguita è invisibile e il cliente paga.

## 5. Regole di dominio (le decisioni che il codice incarna)

1. **Le persone indicate restano attive.** Stesso accesso, stessi ruoli, fino alla scadenza. Il conteggio
   dei posti non cambia — `SeatCount` è già scritto sull'*esistenza* dell'appartenenza, non sull'elenco dei
   suoi stati, quindi non va toccato.
2. **L'owner non è indicabile.** Rifiuto tipizzato.
3. **Una sola riduzione in attesa per account.** Garantita dall'indice unico parziale.
4. **Nessuna aggiunta durante l'attesa.** Presidio nel servizio dell'invito, con rifiuto `409` tipizzato
   `urn:appgrove:seats:reduction-pending` e un testo che offre **due vie d'uscita** (annullare la riduzione,
   oppure attendere la data): un rifiuto senza uscita è un vicolo cieco.
5. **Senza abbonamento dei posti la riduzione non si programma.** Un account interamente dentro la
   franchigia non paga nulla: programmare una cessazione per fine periodo non gli darebbe alcun risparmio e
   gli negherebbe gli inviti per un mese. Rifiuto tipizzato
   `urn:appgrove:seats:reduction-not-needed`, con il testo che indica l'operazione giusta — la rimozione
   immediata, che è gratuita. È la lettura letterale della precondizione dello use case §3.
6. **L'annullamento non ha effetti contabili.** La quantità non era mai stata cambiata: non c'è nulla da
   rimborsare né da riaddebitare.
7. **La rimozione immediata di una persona indicata resta possibile** ed è un'operazione diversa: la persona
   esce dall'elenco degli indicati, il posto resta pagato fino a scadenza (nessun rimborso). A zero indicati
   l'attesa si chiude da sé.
8. **Sospensione e indicazione sono ortogonali**: una riguarda l'accesso, l'altra il posto.
9. **L'esecuzione è idempotente.** Ogni passo è scritto per essere ripetibile: la rimozione logica non
   ricancella, gli accessi già cancellati restano tali, la quantità si porta al valore calcolato (non si
   decrementa), la riduzione si chiude una volta sola con una scrittura condizionata allo stato `pending`.
10. **L'ordine col rinnovo.** L'esecuzione della riduzione dovuta avviene **prima** che un evento del
    fornitore riscriva il periodo dell'abbonamento dei posti, nella **stessa transazione** di quell'evento:
    così il periodo nuovo nasce già con la quantità ridotta. Oggi l'abbonamento dei posti non riceve eventi
    dal fornitore (il prodotto presso il fornitore non esiste ancora, prerequisito #14), ma il presidio va
    messo adesso — quando quegli eventi arriveranno, nessuno si ricorderà di aggiungerlo.
11. **La quantità scende al numero di posti a pagamento effettivamente occupati** dopo la rimozione, non per
    sottrazione del numero di indicati: il valore giusto si ricalcola, non si deriva da un delta che una
    esecuzione ripetuta falserebbe.

## 6. Contratto di rete

Tutto sotto `GET/POST/DELETE /api/platform/v1/me/seats/reduction`, riservato all'**owner** come il resto
della sezione, con l'account dal claim `tenant_id` del token verificato (invariante #1).

| Operazione | Percorso | Esito |
|---|---|---|
| lettura dello stato | `GET /me/seats/reduction` | la riduzione in attesa, o `204` se non c'è |
| effetto prima della conferma | `GET /me/seats/reduction/preview?userId=…&userId=…` | posti e importi prima/dopo, composizione degli scaglioni |
| indicazione | `POST /me/seats/reduction` | `201` con lo stato creato; `409` tipizzato sui rifiuti |
| annullamento | `DELETE /me/seats/reduction` | `204` |
| rimozione di una persona | `DELETE /me/seats/reduction/people/{identityId}` | `204` |

**Modifica al contratto esistente**: `GET /me/seats` porta `pendingReduction = true` quando l'attesa c'è, e
un campo nuovo `reduction` con il **dettaglio** (data di esecuzione, persone indicate, posti e dovuto dopo).
La schermata non fa una seconda chiamata per disegnare l'avviso.

## 7. Dati personali

Nessuna categoria nuova. Due campi nuovi da dichiarare, entrambi **identificativi online**, con lo stesso
inquadramento di `membership.identity_id` e `app_access.identity_id`:

- `seat_downgrade_item.identity_id` — «questa persona è stata indicata per la cessazione da questo account»;
- `seat_downgrade.requested_by` — chi l'ha chiesta (traccia di controllo).

Titolare: l'account. Base giuridica: contratto (art. 6.1.b), la stessa già dichiarata per l'appartenenza —
gestione del rapporto di lavoro dentro lo strumento e determinazione dell'importo dovuto. Conservazione:
fino alla chiusura della riduzione, eliminata con l'account. Classificazione attesa: **MINORE** (nessuna
categoria nuova, nessuna base giuridica nuova, nessun destinatario nuovo, nulla verso l'esterno).

Vanno aggiornati: annotazioni `@PersonalData` sulle entità, manifesto `docs/compliance/manifests/platform.yaml`
nelle due lingue, registro dei trattamenti rigenerato, **esportazione** e **purga** in `PlatformDataContract`
(un campo dichiarato e non esportato rende rosso il collaudo di contratto, ed è giusto così).

**Deliberatamente non fatto**: nessun avviso automatico per email alla persona indicata. È una comunicazione
che spetta al datore di lavoro, non alla piattaforma. Resta il punto aperto di questa storia.

## 8. Superficie

`MembersPage.tsx` e `SeatsCard.tsx`:

1. **selezione multipla** nell'elenco delle persone (caselle di scelta), disponibile solo sulle righe
   indicabili — non l'owner, non un invito in attesa, non chi è già indicato;
2. comando «indica per la cessazione» che mostra l'**effetto prima della conferma**, con la data, i posti
   risultanti, il dovuto prima e dopo e la composizione degli scaglioni;
3. **riquadro di avviso** (tono attenzione) quando l'attesa c'è: «Riduzione programmata — 2 persone
   cesseranno il 14 settembre. Fino ad allora non puoi aggiungere persone.», con l'elenco degli indicati, la
   rimozione singola e il comando «Annulla la riduzione» con conferma esplicita;
4. stato «**in cessazione dal 14 settembre**» nell'elenco unico, tono attenuato: la persona sta lavorando
   normalmente. È il **quarto stato** che la storia 0100 aveva elencato e che la change 0096 aveva
   deliberatamente lasciato fuori perché nessun dato poteva produrlo — ora può;
5. comando di invito **disabilitato con spiegazione** (non nascosto), e il testo che offre le due vie;
6. quando l'attesa è **scaduta e non ancora eseguita**, un messaggio onesto: «la riduzione è in corso di
   esecuzione»;
7. il testo suggerisce la combinazione utile a chi vuole escludere subito qualcuno: togliere gli accessi
   alle applicazioni (immediato e gratuito) **e** indicarlo per la cessazione.

Tutte le stringhe nelle **cinque lingue**, con le date formattate secondo la lingua. **Nessuna aritmetica
nell'interfaccia**: ogni numero arriva dal servizio.

## 9. Requisiti di test

**Backend** (`services/core`)

- indicazione di due persone → attesa con la data giusta (fine del periodo dell'abbonamento), persone ancora
  attive e con gli accessi intatti, quantità dell'abbonamento **invariata**;
- rifiuto dell'owner; rifiuto della seconda riduzione; rifiuto senza abbonamento dei posti; rifiuto di una
  persona che non appartiene all'account;
- **invito durante l'attesa → `409` tipizzato** (è la prova che lega questa storia a UC 0103), con il varco
  che scatta **prima** di qualunque addebito;
- annullamento → nessuna traccia residua, invito di nuovo possibile;
- rimozione di una singola persona; chiusura automatica a zero indicati;
- esecuzione alla scadenza → persone rimosse, accessi cancellati, quantità ridotta al numero di posti a
  pagamento effettivi, dovuto ricalcolato dalla lettura;
- esecuzione **ripetuta** → nessuna doppia rimozione, nessuno stato incoerente;
- **ordine col rinnovo** → l'evento del fornitore sull'abbonamento dei posti esegue prima la riduzione: il
  periodo nuovo nasce con la quantità ridotta;
- misura «riduzioni scadute e non eseguite» → zero in condizioni normali, uno con un'attesa scaduta;
- esportazione e purga comprendono le due tabelle nuove.

**Interfaccia** (`frontend/`)

- selezione multipla e comando disponibile solo sulle righe indicabili;
- effetto prima della conferma;
- riquadro di avviso con elenco, rimozione singola e annullamento;
- invito spento con spiegazione durante l'attesa;
- stato «in cessazione dal …» nella colonna di stato;
- conversione della risposta in forma stretta (`readSeats`) col dettaglio della riduzione.

**Percorso end-to-end** — estensione di `J-SEATS` (livello 2,
`frontend/apps/backoffice/e2e/seats.spec.ts`): l'owner indica una persona, prova a invitare (bloccato),
annulla, invita (riuscito). Registro `docs/testing/copertura-e2e.yaml`: `0104` esce dall'esenzione
`non-implementato` ed entra fra gli use case con superficie, agganciato a `J-SEATS` come il registro stesso
prescriveva.

**Cancello**: `./run-tests.sh` completo verde prima del commit (modalità fast).

## 10. Definition of Done

| Voce | Come si verifica |
|---|---|
| La riduzione è un atto unico su più persone, con data di esecuzione comune | collaudo di integrazione |
| Le persone indicate lavorano fino allo scadere | collaudo: appartenenza attiva, accessi intatti, posti invariati |
| Nessuna aggiunta durante l'attesa, con presidio nel servizio | collaudo sull'operazione di rete dell'invito |
| L'annullamento è completo e senza effetti contabili | collaudo: nessuna riga viva, quantità intatta |
| L'esecuzione alla scadenza è automatica, robusta e misurata | collaudi dello spazzino + misura |
| La quantità dell'abbonamento **scende** | collaudo: quantità = posti a pagamento effettivi |
| Dati personali dichiarati, esportati e purgati | manifesto, registro dei trattamenti, collaudo di contratto |
| Registro di copertura end-to-end coerente | `node tools/e2e-coverage/check.mjs` verde |
| `./run-tests.sh` completo verde | esecuzione prima del commit |
| `how-to-test.md` scritta **ed eseguita** nei passi non visivi | guida in cartella, con l'esito dichiarato |
| `run-tests.sh` aggiornato | non necessario se nessun modulo si aggiunge e nessun comando d'area cambia |
| `_INDEX.md` sincronizzato | non applicabile: le storie evo non compaiono nell'indice di esecuzione |
