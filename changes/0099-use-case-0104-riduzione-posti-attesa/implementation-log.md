# Log di implementazione — change 0099 (UC 0104, modalità fast)

**Branch**: `change/0099-use-case-0104-riduzione-posti-attesa` · **Base**: `133eb01`
**Aree toccate**: `services/core`, `frontend/`, documentazione (use case, copertura end-to-end, manifesto
dati, registro dei trattamenti, revisione legale)

## Che cosa è stato fatto, in ordine

### 1. Il modello: due tabelle, un atto (`V23__seat_downgrade.sql`)

`platform.seat_downgrade` (l'atto: data di esecuzione, stato, chi l'ha chiesta) e
`platform.seat_downgrade_item` (le persone indicate). Non un contrassegno sulla riga della persona, per due
ragioni operative e non estetiche: la data di esecuzione è **una** e con il contrassegno starebbe scritta N
volte — tre persone con tre date sarebbero tre riduzioni, e l'account non ne ha chiesta nessuna delle tre;
e l'annullamento è un atto unico, che con il contrassegno diventerebbe «azzera N colonne», cioè
un'operazione che può riuscire a metà.

**Il vincolo sta in banca dati**, non nel codice: indice unico **parziale** su `tenant_id` per le sole
righe `pending` vive. Il controllo applicativo esiste, ma è il secondo presidio e serve a restituire un
rifiuto comprensibile invece di una violazione di indice — due richieste simultanee dello stesso owner
passerebbero entrambe il controllo, e solo una passa dall'indice.

### 2. Due percorsi di codice, e la divisione è imposta dall'architettura

| Dove | Quando gira | Come parla con la banca dati |
|---|---|---|
| `SeatDowngradeService` | dentro una richiesta autenticata | entità tenant-scoped: il filtro per account lo aggiunge Hibernate (invariante #2 **per costruzione**) |
| `SeatDowngradeExecutor` | **fuori** da una richiesta (spazzino, eventi del fornitore) | SQL nativo con l'account **esplicito** |

La ragione non è di gusto: il risolutore del perimetro di Hibernate è fail-closed in assenza di token, e
senza JWT nessuna sessione si apre. È la stessa postura di `AccountDeletionSweeper`, `TenantOffboarding`,
`SubscriptionWriter` e `PlatformDataContract`. Il prezzo — la franchigia riletta in SQL invece di riusare
`SeatPricing.freeSeats` — è dichiarato nel commento del metodo, e la lettura equivalente sfrutta una
proprietà che il listino ha per vincolo (le fasce sono contigue dal posto 1: la franchigia finisce dove
comincia la prima fascia a pagamento).

### 3. La quantità dell'abbonamento **scende**, e si ricalcola

È il rimando più concreto lasciato dalla change 0098: `quantity` era un high-water mark che saliva
soltanto. Ora scende all'esecuzione — e si **ricalcola** dai posti effettivamente occupati, non si
decrementa del numero di persone indicate. La differenza si vede su un'esecuzione ripetuta: un decremento
applicato due volte lascerebbe l'account a pagare **meno** del dovuto, che è il verso in cui gli errori non
si scoprono perché nessun cliente segnala di aver pagato poco.

### 4. L'idempotenza, passo per passo invece di un lucchetto

Ordine: persone → accessi alle applicazioni → quantità → chiusura, tutto in una transazione. Ogni passo è
ripetibile: `deleted_at is null` nelle condizioni di chiusura (una riga già cancellata non si ricancella, e
la data della prima cancellazione resta quella vera), quantità scritta come valore assoluto, chiusura della
riduzione condizionata allo stato `pending`. Interrompendosi nel mezzo non si applica nulla e la riduzione
resta in attesa con la data passata — cioè nello stato che la misura rende visibile.

### 5. L'ordine col rinnovo: presidiato adesso, esercitato quando servirà

Il piano di lavoro chiedeva di decidere leggendo `PaddleWebhookConsumer`. La lettura dice che gli eventi
arrivano da una coda drenata in modo asincrono e lo spazzino gira ogni ora: l'ordine fra i due **non è
garantito**, e nell'ordine sbagliato il cliente pagherebbe un mese intero alla quantità vecchia.

Scelta: **entrambi i percorsi, un solo punto di esecuzione idempotente**. Lo spazzino è il meccanismo di
oggi; `SubscriptionWriter`, quando applica un evento all'abbonamento dei **posti**, esegue la riduzione
dovuta **prima** di riscrivere il periodo, sulla stessa connessione e nella stessa transazione. Oggi
quell'evento non arriva mai (il prodotto dei posti presso il fornitore non esiste, prerequisito #14): il
presidio va messo ora perché il giorno in cui arriverà nessuno si ricorderà di aggiungerlo.

### 6. La misura che rende visibile il guasto che costa

`appgrove.seats.reduction.overdue` — riduzioni scadute e non eseguite — con avviso di severità alta sopra
zero e un testo che dice la conseguenza («quegli account stanno pagando posti che credevano chiusi»), non
«contatore diverso da zero». Misura di piattaforma, senza dimensioni per account. Nell'enumerazione degli
stati **non** esiste «scaduta», deliberatamente: uno stato dedicato renderebbe normale un guasto.

### 7. Il varco sull'invito, nel punto esatto

La change 0098 aveva lasciato il passo (3) di `InvitationResource.create` come **commento**. Ora è un
rifiuto `409` tipizzato `urn:appgrove:seats:reduction-pending`, e sta lì — prima di ogni calcolo e di ogni
addebito — perché un divieto noto in anticipo non deve costare denaro. Il testo offre **due vie d'uscita**;
lo use case §5 insiste che il messaggio lo dica, e un rifiuto senza uscita è un vicolo cieco.

`SeatCount` **non è stato toccato**: era scritto sull'*esistenza* dell'appartenenza e non sull'elenco dei
suoi stati, quindi le persone in cessazione continuano a occupare — e a costare — il loro posto senza che il
conteggio sappia che quello stato esiste. È il caso che quella scelta doveva coprire, e lo copre.

### 8. La schermata

Selezione multipla (caselle solo sulle righe indicabili), stima **prima** della conferma con la
composizione degli scaglioni, riquadro di avviso con l'elenco degli indicati, comando «**Mantieni**» per
togliere una singola persona, «**Annulla la riduzione**» con conferma esplicita, stato «**in cessazione dal
…**» nell'elenco, invito **spento con spiegazione**. Cinque lingue, plurali compresi, date formattate
secondo la lingua.

Due scelte che vale la pena spiegare:

- lo stato «in cessazione» viaggia come **campo data** (`endingAt`) e non come quarto valore di `status`:
  cessazione programmata e sospensione sono ortogonali (use case §5) e una persona può essere entrambe —
  con un quarto valore di stato una delle due informazioni sarebbe sparita e il comando «riattiva» avrebbe
  smesso di sapere se la persona era sospesa;
- il comando che toglie una persona dall'elenco si chiama «**Mantieni**» e non ha conferma: l'atto
  *aggiunge* una persona all'account, e un'etichetta distruttiva su un'azione che salva qualcuno è la
  peggiore delle ambiguità.

### 9. Privacy

Due campi nuovi, entrambi identificativi online: `seat_downgrade_item.identity_id` e
`seat_downgrade.requested_by`. Nessuna categoria particolare, base giuridica invariata, nessun destinatario
nuovo → classificazione **MINORE**. Manifesto aggiornato nelle due lingue, registro dei trattamenti
rigenerato, esportazione e purga estese, voce **L19** nella revisione legale.

La scelta di **non** avvisare automaticamente la persona indicata è confermata (è una comunicazione del
datore di lavoro, non della piattaforma) e dichiarata nel manifesto; il dubbio residuo di trasparenza verso
l'interessato è nella voce L19.

Il collaudo di contratto GDPR ha fatto il suo lavoro: è diventato rosso finché le due tabelle non sono
entrate nell'**esportazione**. È il presidio che rende impossibile dichiarare un campo personale e
dimenticarsi di esportarlo.

### 10. Un difetto trovato **eseguendo** la guida di collaudo

Il §9 della guida (annullamento) ha mostrato una riga di persona indicata ancora **viva** dopo l'esecuzione
di una riduzione: la scrittura che doveva chiuderle aggiornava soltanto `updated_at`. La regola è stata resa
unica per tutti i modi in cui una riduzione finisce — *una riduzione che non è più in attesa non ha persone
indicate vive* — con un'asserzione nuova nel collaudo dell'esecuzione. Nessuna informazione si perde: la
cancellazione è logica e l'esportazione dei dati personali legge anche le righe cancellate.

**La guida non è stata ammorbidita**: il passo è rimasto quello che era ed è stato rieseguito a correzione
fatta.

## Collaudi

**Backend** (`services/core`, 414 → **431** collaudi verdi; due classi nuove):

- `SeatDowngradeApiTest` (12 prove) — indicazione di due persone con la data del periodo già pagato e
  **nulla di ciò che si paga che cambia**; stima con la composizione degli scaglioni che non programma
  niente; invito durante l'attesa rifiutato **senza costare denaro**; annullamento che riapre gli inviti;
  chiusura automatica a zero indicati; rimozione immediata di una persona indicata; owner non indicabile;
  seconda riduzione rifiutata; nessun posto a pagamento; persona di un altro account; la data di cessazione
  nell'elenco delle persone; la sezione riservata all'owner;
- `SeatDowngradeExecutionTest` (5 prove) — esecuzione alla scadenza (persone fuori, accessi cancellati,
  quantità che scende, nessuna persona che resta indicata); **esecuzione ripetuta** che non rimuove due
  volte e non falsa la quantità; nessuna esecuzione prima della scadenza; la misura delle riduzioni scadute;
  l'**ordine col rinnovo** provato consegnando l'evento del fornitore dalla pipeline vera.

Tre collaudi preesistenti aggiornati perché la realtà è cambiata: `PlatformGdprContractTest` semina una
riduzione, `GdprExportApiTest` conta otto passi di raccolta invece di sette, e nella schermata dei membri le
prove sulle colonne e sull'owner in testa tengono conto della colonna di selezione.

**Interfaccia**: `MembersPage.test.tsx` (quattro prove nuove: avviso con elenco e vie d'uscita,
annullamento che riapre l'invito, indicazione con l'effetto prima della conferma, rifiuto «nulla da
ridurre»), `seats.test.ts` (quattro prove sulla conversione della riduzione: intera o assente, mai a metà),
`roster.test.ts` (tre prove sul quarto stato, sulla convivenza con la sospensione e su chi è indicabile).
Totale frontend: 259 → **266** prove verdi.

**Percorso end-to-end**: `J-SEATS` **estesa** — terzo caso in
`frontend/apps/backoffice/e2e/seats.spec.ts`: l'owner indica una persona vedendo l'effetto, l'avviso
compare con lo stato «in cessazione dal …», l'invito è impedito, l'annullamento riapre l'invito e l'invito
riesce. Registro di copertura aggiornato: 0104 esce dall'esenzione ed entra fra le superfici.

**Suite completa** `./run-tests.sh` (senza parametri): **verde** su tutte le aree.

**Guida di collaudo**: `how-to-test.md`, con i passi non visivi **eseguiti** contro lo stack locale prima
del commit. Un difetto di prodotto trovato e corretto (vedi §10); nessun altro scostamento.

## Che cosa resta indietro, e dove è scritto

| Rimando | Dove |
|---|---|
| Rinnovo del periodo dei posti: senza di esso una **seconda** riduzione nascerebbe con la data già passata | UC 0104, punti aperti → proprietario **UC 0106** |
| Ordine col rinnovo da riesercitare col fornitore **vero** quando esisterà | UC 0104, punti aperti → UC 0106 |
| Rifiuto «nulla da ridurre» da rivedere se la franchigia diventasse configurabile per account | UC 0104, punti aperti → UC 0105 |
| Richiamo dello spazzino dal temporizzatore gestito del cloud | UC 0104, punti aperti → UC 0035 |
| Tre punti da far confermare a un legale (nessun avviso alla persona indicata, conservazione della traccia annullata, assenza di rimborso) | `docs/_REVISIONE-LEGALE.md`, voce **L19** |

Tre rimandi **chiusi** da questa change, e annotati come tali nello use case: il varco sull'invito, il
campo `pendingReduction` che diventa vero, la quantità dell'abbonamento che scende (lasciati da UC 0103);
più il quarto stato dell'elenco unico (lasciato da UC 0100).

## Definition of Done

| Voce | Esito |
|---|---|
| La riduzione è un atto unico su più persone, con data di esecuzione comune | ✅ due tabelle + collaudo + guida eseguita |
| Le persone indicate lavorano fino allo scadere | ✅ appartenenza viva, stato invariato, posti e quantità invariati |
| Nessuna aggiunta durante l'attesa, con presidio nel servizio | ✅ rifiuto tipizzato provato dall'operazione di rete, non dall'interfaccia |
| L'annullamento è completo e senza effetti contabili | ✅ quantità invariata, nessun indicato vivo, invito di nuovo possibile |
| L'esecuzione alla scadenza è automatica, robusta e misurata | ✅ spazzino + idempotenza provata + misura con l'invariante a zero |
| La quantità dell'abbonamento **scende** | ✅ ricalcolata dai posti effettivi, non decrementata |
| Ordine col rinnovo | ✅ stessa transazione dell'evento, con il suo collaudo |
| Dati personali dichiarati, esportati e purgati | ✅ manifesto, registro dei trattamenti, contratto GDPR verde |
| Registro di copertura end-to-end coerente | ✅ `node tools/e2e-coverage/check.mjs` verde |
| `./run-tests.sh` completo verde | ✅ |
| `how-to-test.md` scritta **ed eseguita** nei passi non visivi | ✅ un difetto trovato e corretto |
| `run-tests.sh` aggiornato | non necessario: nessun modulo aggiunto, nessun comando d'area cambiato (decisione 33) |
| `_INDEX.md` sincronizzato | non applicabile alle storie evo (decisione 3); lo stato è nel drill-down di 0104 |
