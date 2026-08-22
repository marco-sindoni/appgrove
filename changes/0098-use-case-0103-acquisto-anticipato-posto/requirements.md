# Change 0098: Acquisto anticipato del posto all'invito — abbonamento di piattaforma

**Branch**: `change/0098-use-case-0103-acquisto-anticipato-posto`
**Aree**: `services/core`, `frontend/`
**Data**: 2026-08-22
**Autore**: Platform Engineering (modalità fast, orchestrata da `go-fast`)
**Use case sorgente**: [docs/usecases/22-refactor-membership-model/story/0103-acquisto-anticipato-posto-invito.md](../../docs/usecases/22-refactor-membership-model/story/0103-acquisto-anticipato-posto-invito.md)
(piano di lavoro: [task/0103](../../docs/usecases/22-refactor-membership-model/task/0103-acquisto-anticipato-posto-invito.md);
epica: [E22.2 Posti a pagamento](../../docs/usecases/22-refactor-membership-model/epic/E22-02-posti-a-pagamento.md))
**Tocca dati personali?**: Sì, come **precisazione di finalità**, non come dato nuovo. Nessun campo nuovo
che riguardi una persona, nessuna categoria nuova, nessuna base giuridica nuova: quello che cambia è che il
numero delle appartenenze e degli inviti **determina un importo**, quindi la finalità di
`membership.identity_id` va estesa nel manifesto della piattaforma (rimando lasciato dalla change 0097).
Classificazione MAGGIORE/MINORE argomentata nel §«Privacy» qui sotto.

## Problema / Obiettivo

Dopo la change 0097 il listino dei posti esiste, il calcolo del dovuto è esatto al centesimo e la regola
«che cosa occupa un posto» è scritta una volta sola. Manca però la cosa per cui tutto quello esiste:
**nessuno paga niente**. Invitare una persona è ancora gratuito, l'account non vede che cosa gli costerà la
prossima, e non esiste alcun abbonamento che contenga i posti.

Questa change fa dell'invito **un atto che passa dalla cassa**:

1. l'owner vede, **prima** di confermare, che posto occuperà la persona che sta invitando e quanto costerà;
2. alla conferma il sistema **addebita** e solo a esito positivo crea l'invito: senza addebito riuscito
   l'invito **non nasce**;
3. l'abbonamento che contiene i posti è **di piattaforma**, non di una applicazione, e riusa interi
   l'impianto del pagamento, del ciclo di vita e della fatturazione.

Risultato osservabile a fine change: su un account con tre persone il riquadro dei posti dice «3 posti
usati, stai pagando 0,00 €, il prossimo posto costa 2,99 €»; il quarto invito crea l'abbonamento di
piattaforma con quantità 1; il quinto lo porta a quantità 2 e a un dovuto di 5,98 €; un addebito rifiutato
non lascia dietro di sé nessun invito.

## Scope

### 1. La voce di catalogo di piattaforma, e le sue esclusioni

L'epica E22.2 ha già scelto la **via A**: i posti sono un abbonamento come tutti gli altri, appeso a una
voce di catalogo che **non è una applicazione**. È una decisione già presa per iscritto e questa change la
rende operativa.

- `platform.app` acquisisce una colonna **`kind`** con due valori (`application`, `platform`) e valore
  predefinito `application`. Serve un **attributo**, non un elenco di slug da escludere: un elenco
  invecchia in silenzio (è l'argomento con cui la change 0092 ha rimandato l'esclusione a qui).
- Nasce **una riga** di catalogo: slug `platform-seats`, `kind = platform`. Il suo identificativo è
  **deterministico** dalla chiave stabile, con lo stesso schema di `CatalogIds` usato dalla
  sincronizzazione del listino, così che sia identico in ogni ambiente.
- `platform.subscription` acquisisce la colonna **`quantity`** (intera, predefinita 1): gli abbonamenti
  delle applicazioni sono a quantità uno, quello dei posti porta **il numero di posti a pagamento**.
- `platform.invitations` acquisisce il **riferimento all'addebito** che ha autorizzato quell'invito.

**Le esclusioni, una per una** — è il debito della via A, e ognuna vuole il suo collaudo:

| Dove | Che cosa deve accadere |
|---|---|
| Diritti d'accesso (`EntitlementReadModel`) | la voce di piattaforma non concede accesso ad alcuna applicazione |
| Vetrina del cliente (`CatalogReadModel` → `GET /me/catalog`) | non compare fra le applicazioni acquistabili |
| «Dove posso entrare» (`MeAppAccessResource` → `GET /me/app-access`) | esclusa, quindi assente dal menu laterale |
| Applicazioni per persona (`UserResource`, colonna «applicazioni» dell'elenco delle persone) | esclusa: l'accesso implicito dell'owner non la nomina |
| Console di amministrazione (`AdminResource`) | **compare**, marcata come voce di piattaforma, ed è esclusa dalla matrice dei diritti |

Il menu laterale del frontend non conosce la voce per costruzione (il registro dei moduli è un elenco
chiuso): non c'è nulla da fare, ma la sua assenza si prova.

### 2. Il servizio dei posti e la sua lettura di rete

Nasce `GET /api/platform/v1/me/seats`, riservata all'**owner** come il resto della sezione «Members»,
che risponde con tutto ciò che serve al riquadro e alla stima, **calcolato dal servizio**:
posti usati e loro composizione (persone attive, sospese, inviti in attesa), valuta, dovuto attuale,
numero di posti a pagamento, fascia applicata, **costo del posto successivo**, dovuto dopo il posto
successivo, e il numero d'ordine che quel posto avrà. L'interfaccia **non fa aritmetica**.

La lettura dichiara anche se c'è una **riduzione in attesa**: oggi vale sempre «no», perché lo stato non
esiste ancora (è di UC 0104), e il campo esiste perché il riquadro e il rifiuto dell'invito abbiano un
solo posto da cui leggerlo quando arriverà.

### 3. L'invito che passa dalla cassa

La creazione dell'invito diventa una **sequenza ordinata**, con l'account bloccato in modo pessimistico
per tutta la sequenza (`SELECT … FOR UPDATE` sulla riga dell'account, il modo già in uso nel monorepo per
le quote a giacenza):

1. rifiuto se l'account è **in attesa di eliminazione**;
2. rifiuto se esiste una **riduzione in attesa** (nessuna, oggi: gate predisposto per UC 0104);
3. verifiche già esistenti su indirizzo duplicato (già membro / già invitato);
4. calcolo del nuovo dovuto sul conteggio dei posti **letto sotto blocco**;
5. se il nuovo posto porta oltre quanto **già pagato nel periodo in corso** → **addebito**; se
   l'addebito non riesce si esce **senza creare nulla**, con il motivo che il fornitore restituisce;
6. creazione dell'invito, con il riferimento all'addebito che lo ha autorizzato;
7. se il passo 6 fallisce dopo un addebito riuscito → **annullamento dell'addebito** e avviso operativo
   di severità alta.

**Nessun secondo addebito nello stesso periodo.** L'abbonamento ricorda quanti posti sono **già pagati**
per il periodo in corso (la colonna `quantity`), e questa change lo fa solo **salire**. Da qui derivano da
sé due casi della storia: un invito scaduto o revocato **libera il posto** senza rimborso, e un invito
nuovo entro lo stesso periodo **non genera un secondo addebito** perché il posto bersaglio è già pagato.
La discesa della quantità è di UC 0104.

**La franchigia non è un caso speciale**: se il dovuto non cambia, non si chiama il fornitore e non nasce
alcun abbonamento. L'abbonamento di piattaforma nasce col **quarto** posto.

### 4. Il riquadro dei posti nella sezione «Members»

Nel posto già predisposto dalla change 0096, in testa alla pagina:

- «**Posti usati N**» con la composizione sotto (attive, sospese, inviti in attesa);
- «**Stai pagando X € al mese**» con la fascia applicata e il numero di posti a pagamento;
- «**Il prossimo posto costa Y €**», e quando la tariffa **scende** il testo lo dice per esteso: quello
  che scende è il costo del posto in più, il totale sale sempre — col listino progressivo va detto
  proprio così, altrimenti sembra un errore di conteggio.

Stati del riquadro: **caricamento** (il pulsante di invito resta disabilitato finché il costo non è noto —
mai invitare alla cieca), **errore di lettura** (nessun invito permesso, con possibilità di riprovare),
**riduzione in attesa** (avviso e invito bloccato: predisposto, non raggiungibile oggi).

La finestra di invito mostra la **stima prima della conferma** («questa persona sarà il posto numero 4;
costo 2,99 € al mese; il tuo totale passerà da 0,00 a 2,99 €»). Un addebito rifiutato produce un messaggio
esplicito con il motivo del fornitore e il rimando alla sezione dei pagamenti.

Traduzioni nelle **cinque lingue**, plurali compresi.

### Fuori scope (e dove va)

- il **calcolo** del dovuto → già fatto (UC 0102);
- la **riduzione** dei posti, lo stato «in cessazione», l'esecuzione a scadenza → UC 0104;
- il **governo del listino** dalla console → UC 0105;
- la **riga dei posti in fattura**, lo storico, il prossimo rinnovo, il rinnovo del periodo → UC 0106;
- l'abilitazione della persona sulle applicazioni (gratis, perché il posto è di piattaforma) → UC 0111;
- il percorso end-to-end **di piattaforma** con lo stack vero → UC 0113 (qui si copre il tratto di
  livello 2 con il backend simulato).

## Decisioni tecniche già fissate altrove (si implementano, non si chiedono)

Sono decisioni **dello sviluppatore**, scritte nel drill-down o nell'epica, e questa change le esegue:

- il listino è a **scaglioni progressivi**, la franchigia è di **tre posti compreso l'owner**, le tariffe
  sono 0 / 2,99 / 1,99 / 0,99 / 0,49 € per le cinque fasce (file `pricing/seats.yaml`, change 0097);
- **solo l'owner invita**, perché l'operazione ha effetto economico;
- l'ordine degli atti è verifica → addebito → invito → email, e **senza addebito l'invito non nasce**;
- i posti si pagano **anche durante il periodo di prova** di una applicazione (il posto è di piattaforma);
- **nessun rimborso** su invito scaduto o revocato, coerente con la permanenza minima mensile;
- la via strutturale è la **voce di catalogo di piattaforma** (via A dell'epica), col suo debito di
  esclusioni.

**Nessuna chiamata reale verso il fornitore di pagamento.** Tutto passa dal port `PaymentProvider`, che in
locale e nei collaudi è il **simulatore** già in uso; l'implementazione reale resta il segnaposto che
fallisce in modo esplicito, come per ogni altro metodo del port (bloccata dal prerequisito #14).

## Privacy

**Che cosa cambia.** Nessun campo nuovo che riguardi una persona. L'email dell'invitato è già dichiarata
(UC 0013) e il riferimento all'identità già esistente sull'invito è già dichiarato (UC 0118). La novità è
che il **numero** delle appartenenze e degli inviti in attesa determina ora **un importo addebitato**:
`membership.identity_id` acquisisce una finalità che prima non era vera.

**Classificazione: MINORE.** L'argomento, per esteso — è un caso di confine e va difeso, non dato per
scontato:

- **nessuna categoria nuova** di dati: si contano righe che già esistono, non si raccoglie nulla;
- **nessuna base giuridica nuova**: resta l'esecuzione del contratto con l'account titolare, la stessa che
  già copre la fatturazione degli abbonamenti;
- **nessuna conservazione nuova**: il conteggio non è conservato come tale, e l'abbonamento dei posti
  segue la conservazione già dichiarata per gli abbonamenti;
- **nessun responsabile esterno nuovo**: il fornitore di pagamento è già dichiarato e già riceve l'importo
  e l'identificativo dell'account (non riceve l'elenco delle persone).

Quello che cambia è la **finalità dichiarata** di un dato già dichiarato, all'interno di un trattamento
già dichiarato. È una precisazione di trasparenza, non un trattamento nuovo: un aggiornamento **MINORE**
della privacy policy, senza ri-accettazione dei documenti legali. Adempimenti: estendere `purpose` di
`membership.identity_id` nel manifesto della piattaforma **nelle due lingue** e rigenerare il registro dei
trattamenti.

## Requisiti di test

**Backend (`services/core`)**

- franchigia: il primo, secondo e terzo posto **non** creano abbonamento e non chiamano il fornitore;
- **quarto** invito → abbonamento di piattaforma creato, quantità 1, dovuto 299 centesimi;
  **quinto** → quantità 2, dovuto 598;
- **addebito rifiutato** → nessun invito creato, nessun abbonamento, nessuna riga a metà;
- **creazione dell'invito fallita dopo addebito riuscito** → addebito annullato e nulla di persistito;
- **inviti concorrenti**: due invii simultanei non addebitano due volte lo stesso salto di fascia;
- **invito scaduto o revocato** → il posto si libera; un invito nuovo entro lo stesso periodo **non**
  genera un secondo addebito;
- **cinque prove di esclusione**, una per ogni superficie della tabella del §1;
- la lettura dei posti è **dell'owner** e prende l'account **dal token**, non da un parametro;
- riquadro con listino assente → rifiuto esplicito invece di un dovuto zero.

**Frontend**

- il riquadro nei suoi stati: caricamento (pulsante di invito **disabilitato**), errore (invito impedito,
  possibilità di riprovare), dati presenti;
- la stima prima della conferma, compreso il caso in cui il costo del posto successivo **scende**;
- l'addebito rifiutato mostra il motivo del fornitore;
- controllo dei tipi verde e nessuna chiave di traduzione mancante nelle cinque lingue.

**Copertura end-to-end (registro `docs/testing/copertura-e2e.yaml`)**

Lo use case 0103 esce dall'esenzione `non-implementato` ed entra fra le superfici, con il percorso
**J-SEATS** coperto a **livello 2** (backend simulato) da un nuovo file di collaudo del backoffice. Il
tratto di **piattaforma** dello stesso percorso — stack vero, addebito con il simulatore del fornitore —
resta a UC 0113 e va tracciato come rimando.

## Definition of Done

1. il posto si paga **prima** che l'invito parta, e senza pagamento l'invito non esiste;
2. l'abbonamento di piattaforma esiste con la sua quantità e riusa l'impianto di pagamento;
3. la voce di catalogo di piattaforma è invisibile in tutte le superfici del cliente, provata una per una,
   e visibile-ma-marcata in console;
4. il riquadro dei posti mostra usati, importo e costo del prossimo, con la lettura del caso in cui scende;
5. il manifesto della piattaforma e il registro dei trattamenti sono aggiornati;
6. il registro di copertura end-to-end è coerente (`node tools/e2e-coverage/check.mjs` verde);
7. `./run-tests.sh` **completo** verde;
8. `how-to-test.md` scritta **ed eseguita** nei suoi passi non visivi;
9. i rimandi lasciati indietro sono scritti negli use case che li possiedono.
