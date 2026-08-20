# Log di implementazione — Change 0087 · Epica 22

**Branch**: `change/0087-epica-22-refactor-membership-model` · **Modalità**: autopilot
**Tipo**: analisi documentale + prototipi + un ritocco mirato al rilevatore del catalogo
**Stato**: completata, **in attesa della revisione dello sviluppatore** (nessun commit)
**Data**: 2026-08-19

## Che cosa è stato prodotto

| Cosa | Dove | Quantità |
|---|---|---|
| Epica madre + 4 sotto-epiche | `docs/usecases/22-refactor-membership-model/epic/` | 5 file |
| Storie (use case `0098`–`0118`) | `…/story/` | 21 file |
| Piani di lavoro | `…/task/` | 18 file |
| Prototipi navigabili + documentazione | `…/prototype/` | 5 pagine + README + 4 file di supporto |
| Indice dell'area | `…/README.md` | 1 file |

Totale: **52 file nuovi** nell'area 22, più cinque file modificati fuori da essa (§4).

## Come è stato fatto

**Prima l'esplorazione del codice, poi la scrittura.** I piani di lavoro citano percorsi di file veri
perché sono stati letti: modello utenti/inviti e ruolo di piattaforma (`User.java`, `UserRole.java`,
`InvitationResource.java`), la funzione che costruisce il token (`handler.py` della Lambda e il suo
gemello locale), menu laterale e guardie di rotta del backoffice, i posti che il Mini-CRM si è costruito
da sé (`Seat.java`, `SeatAccess.java`), il motore dei diritti d'accesso e delle quote, la struttura degli
abbonamenti e del listino come codice, la skill `new-application` e il collaudo di parità.

**Documentazione sul modello di riferimento indicato dallo sviluppatore** (GitHub): due ruoli a livello
di organizzazione più ruoli per risorsa, aggiunte immediate e a pagamento, rimozioni valide dal periodo
successivo. Le due divergenze deliberate sono registrate nell'epica madre §3.

**Verifica dei prototipi in un browser vero** (Chromium via Playwright, già presente nel progetto): le
cinque pagine caricano senza errori, dipingono con i token reali, e mostrano per ogni ruolo esattamente
l'insieme di voci e comandi della matrice. Nella console di piattaforma il calcolo è stato verificato
contro la tabella dei requisiti di UC 0102 e la validazione delle fasce contro un buco fra due fasce.
Due difetti trovati così e corretti: il pannello didattico copriva la tabella (ora chiuso di default) e
la spiegazione del ruolo mancante era ripetuta su ogni riga (ora visibile solo sul comando principale).

## Le due storie aggiunte dopo la prima revisione

Lo sviluppatore ha chiesto di riverificare la **categorizzazione B2C/B2B** delle applicazioni, che aveva
introdotto proprio sulla differenza del modello utenti. La verifica nel codice ha dato un esito netto e ha
prodotto due storie in più (18 in tutto):

- **UC 0114 — ritiro della categoria.** Tre ragioni indipendenti: «app privata» non è più una proprietà
  dell'app; il campo non tocca più il prezzo; il suo unico uso funzionale (scegliere i ruoli degli endpoint
  generati) scompare col varco per applicazione. Sarebbe rimasto un campo obbligatorio, dichiarato e mai
  letto. La verifica ha anche portato alla luce che «B2C/B2B» significa **due cose** nel progetto — modello
  utenti dell'app (si ritira) e ruolo giuridico verso i clienti (resta intatto) — e che l'ambiguità era già
  un problema noto: `docs/13` §34-35 aveva dovuto scrivere una nota difensiva, e i collaudi della console
  admin usano valori finti `b2b`/`b2c` che non esistono nell'enumerazione reale.
- **UC 0115 — ambito dei dati**, che prende il posto della categoria ritirata: i dati di un'applicazione
  sono del **gruppo di lavoro** o della **persona** che li ha creati. Lo sviluppatore ha scelto di
  costruirla subito, non di tracciarla. Il nodo — che cosa vede l'owner — è risolto separando **visibilità**
  e **titolarità**: dall'interfaccia dell'applicazione non vede i dati altrui, dalle vie di conformità li
  ottiene, con traccia. In rilettura lo sviluppatore ha posto un limite di scope: **nessuna applicazione ad
  ambito `utente` nasce in questa epica** — dopo il rifacimento dell'appartenenza si passa a lavorare sulle
  applicazioni (voce 41). Da lì la forma finale della storia, che è **sua**: l'ambito è una caratteristica
  che l'applicazione **dichiara** alla nascita, e il filtro è la **conseguenza** che si costruisce quando la
  prima applicazione la richiede (voce 43). Nell'epica entrano quindi il campo dichiarato dal listino, la
  domanda del copilota, la precisazione al contratto dei ruoli e il **piano integrale del filtro**; restano
  fuori la sua costruzione, i collaudi di isolamento, i modelli del generatore, il manifesto e l'informativa.
  Il campo si fa comunque adesso perché così ogni applicazione nasce classificata: aggiungerlo dopo
  significherebbe tornare a classificare le due esistenti e migrare i loro listini. E perché non diventi
  l'etichetta vuota appena ritirata, un'applicazione che dichiara `utente` **non passa** il controllo dei
  listini e **non si avvia** — due punti di arresto, non uno (voce 44): qui la promessa sarebbe di
  riservatezza, e una riservatezza creduta e non applicata è peggio di nessuna riservatezza.

## La sotto-epica emersa in rilettura — identità e appartenenze

Durante la rilettura, esaminando il modello dei dati, è venuto a galla un vincolo che nessuno aveva messo in
discussione: **una persona appartiene a un solo account**, imposto da indici unici globali su una tabella che
sta *dentro* l'account
([V2__core_domain.sql](../../services/core/src/main/resources/db/migration/V2__core_domain.sql), commento
«membership foldata 1 utente→1 tenant»), dichiarato in [docs/02 §14](../../docs/02-auth-sicurezza.md) e
assunto dalla funzione che costruisce il token, che cerca **una** riga per identificativo di autenticazione.

Il difetto si vede al primo cliente vero: chi ha già provato appgrove per conto proprio **non può essere
invitato** da un'azienda, e il rifiuto arriva come violazione di indice invece che come messaggio
comprensibile — l'unico controllo esistente riguarda gli inviti in attesa *dello stesso account*.

Lo sviluppatore ha deciso di affrontarlo **dentro** questa epica, e la ragione è solida: il vincolo vive
esattamente nei due punti che l'epica stava già riscrivendo. Ne sono nate tre storie in una sotto-epica
propria — [E22.5](../../docs/usecases/22-refactor-membership-model/epic/E22-05-identita-e-appartenenze.md),
numerata per ultima e **prima a eseguirsi**, perché 0098 crea la tabella degli accessi con un riferimento
alla persona e cambiarne la forma dopo costringerebbe a rifare due migrazioni.

Le tre decisioni che le tengono in piedi (voci 46-51):

1. **L'unicità si sposta, non si allenta.** L'indirizzo resta unico globalmente, ma sull'identità, che non è
   di nessun account; il vincolo che serve davvero — «non due volte nello stesso account» — diventa
   **esplicito** invece di essere l'effetto collaterale di una regola più larga.
2. **L'account attivo vive in banca dati, non presso il fornitore di identità.** Verificato che il gruppo di
   utenti Cognito non dichiara attributi personalizzati e che aggiungerne uno rischia di **ricreare il
   gruppo**, cioè di perdere gli utenti. E il valore conservato **non è creduto**: l'appartenenza si
   riverifica a ogni creazione di token — la riga che impedisce a una manomissione di diventare un varco fra
   due aziende.
3. **Gli esiti dell'invito non rivelano l'esistenza dell'identità.** Il messaggio utile è anche quello che
   rivela: i messaggi chiari restano per le collisioni che sono informazione dell'account.

I **prototipi** hanno accolto i due elementi d'interfaccia che ne discendono — selettore dell'account e
inviti ricevuti, entrambi nell'intestazione (voce 52) — e con essi si chiudono i tre punti aperti che
rimandavano «ai prototipi». La regola che vale la pena ricordare: il selettore compare in base al **numero
di appartenenze**, non al ruolo. Nei prototipi ce l'hanno `admin` e `viewer` e non `owner` ed `editor`, e la
documentazione lo dice a voce alta, perché legarlo al ruolo in implementazione sarebbe l'errore naturale.
Con una sola appartenenza il selettore **non si rende affatto**: reso disabilitato sarebbe rumore per il
cento per cento degli utenti di oggi. Verificato in un browser reale su tutti e quattro i prototipi.

Da una domanda dello sviluppatore in rilettura — «come ci accertiamo che la barra di commutazione del ruolo
e il riquadro delle differenze non vengano implementati per errore?» — è nato il presidio contro
l'**impalcatura implementata** (voce 53): gli elementi lo dichiarano di sé con l'etichetta «non è prodotto»
visibile in ogni fotografia dello schermo, portano il prefisso di classe `x-proto-scaffold-` che nel design
system non esiste, hanno una sezione §0 dedicata in testa alla loro documentazione, e un **controllo
automatico** nell'area `tooling` fa fallire la suite se quel prefisso compare in `frontend/` — provato nei
due versi. Il presidio automatico non è eccessivo per la ragione che rende insidiosa questa classe di
errori: un comando estraneo nell'interfaccia vera **non rompe nulla**, resta lì.

Sempre in rilettura, tre correzioni di interfaccia dello sviluppatore (voce 54), tutte con la stessa
radice — **mettere ogni informazione dove è vera**: il selettore dell'account scende nella barra laterale
sotto il marchio (l'account è il contesto del lavoro, non un comando accessorio); l'etichetta di ruolo
sparisce dall'intestazione, perché il ruolo è *per applicazione* e una etichetta globale diventa un elenco
appena una persona è abilitata a due applicazioni; gli inviti ricevuti diventano una **sezione del
cruscotto** invece di un pulsante, perché un invito a collaborare con un'altra azienda è un rapporto di
lavoro in sospeso, non una notifica. L'ultima, in particolare, è una cosa da **non** fare, e per questo è
scritta nella tabella di mappatura: senza una riga esplicita, in implementazione l'etichetta ricomparirebbe.

**Il modello di prezzo è cambiato in rilettura** (voci 55-56), e non è un dettaglio di listino: da *tariffa
unica della fascia raggiunta su tutti i posti a pagamento* a **scaglioni progressivi**, dove ogni posto paga
la tariffa della fascia in cui cade. Il motivo è commerciale e stringente: il modello precedente faceva
**scendere il totale** ai confini — undici posti costavano meno di dieci — e un prezzo che cala quando
cresci sembra un errore di conteggio anche quando è a favore del cliente. Con la progressività il totale
sale sempre e a scendere è il **costo del posto successivo**: la stessa convenienza, detta in un modo che si
capisce. Le tariffe nuove sono 2,99 · 1,99 · 0,99 · 0,49, franchigia di tre invariata. Ricadute: la
decisione 23 (il dovuto decresce ai confini) è **superata**, un collaudo può ora pretendere la monotonia, e
l'interfaccia mostra la **somma degli scaglioni** invece di un prodotto unico — che con cinque posti
tornerebbe per caso e con cinquanta sarebbe falso.

Infine, una richiesta di **fedeltà del prototipo** (voce 57): mancavano «Impostazioni» e il menu della
persona («Sicurezza», «Esci»), che nel prodotto stanno nel piede del menu. Aggiunte, con la decisione che le
accompagna — sono preferenze **della persona**, quindi visibili a ogni ruolo. È il tipo di regressione che un
prototipo incompleto non previene ma invita: chi implementa la visibilità per ruolo guarda il prototipo, e
ciò che non vede non lo difende. Nella stessa direzione, e dalla stessa segnalazione (l'ingranaggio assente
su «Impostazioni»), il prototipo ha preso le **icone del menu** con i nomi veri copiati da `Sidebar.tsx` e
dai manifesti dei moduli, in entrambi i prototipi — di piattaforma e di console (voce 58). La regola vale
la pena di essere ricordata: i nomi delle icone **si copiano, non si scelgono**.

## Le decisioni di merito

Sessanta voci in [decisions.json](decisions.json). Le quattro che contano più delle altre:

1. **Il ruolo per applicazione non entra nel token** (voce 19). È la scelta che regge l'epica: altrimenti
   una retrocessione avrebbe effetto solo al rinnovo del token.
2. **Il ruolo di piattaforma scende a due valori** (voce 20), e ogni presidio oggi tarato su «owner
   oppure admin» va stretto a «solo owner». Sono strette da provare una per una.
3. **L'abbonamento dei posti riusa l'impianto di pagamento** attraverso una voce di catalogo di
   piattaforma (voce 21), pagando come debito cinque esclusioni da mantenere con i loro collaudi.
4. **Il listino è versionato e immutabile** (voce 22): la console crea versioni, non modifica prezzi.

Quattro risposte sono dello **sviluppatore**, non dell'agente, perché toccavano denaro o dati personali
(voci 5–8): modello degli scaglioni, owner che occupa un posto, accesso mantenuto durante la riduzione in
attesa, e «I miei dati» in forma ridotta invece di nascosta. La quarta è una **correzione al requisito
iniziale** proposta dall'agente e accettata: il diritto di accedere ai propri dati appartiene a ogni
interessato.

## Fuori dall'area 22: i cinque file modificati

| File | Perché |
|---|---|
| `tools/e2e-coverage/lib.mjs` | `listCatalogUseCases` estesa a un livello di sottocartelle: senza questo le sedici storie in `story/` sarebbero invisibili al presidio che pretende la classificazione di ogni use case |
| `tools/e2e-coverage/test/check.test.mjs` | test nuovo dell'estensione (non classificata → rosso, classificata → verde, numero doppio in `story/` e `task/` → conta una volta) |
| `docs/testing/copertura-e2e.yaml` | le sedici storie classificate come `non-implementato`, con motivo e con la classificazione futura già indicata |
| `docs/usecases/README.md` | l'area 22 nell'indice per area, con la nota che supera l'epica 14 |
| `CLAUDE.md` | conteggio del catalogo da 97 a 113; area 22 fra le epiche evolutive |

## Test

| Area | Comando | Esito |
|---|---|---|
| tooling | `./run-tests.sh tooling` | ✅ verde (55 test, compreso quello nuovo) |
| compliance | `./run-tests.sh compliance` | ✅ verde (gli avvisi sui dati del titolare sono preesistenti e attesi pre-go-live) |
| registro di copertura | `node tools/e2e-coverage/check.mjs` | ✅ coerente |
| prototipi | caricamento in Chromium delle 5 pagine | ✅ nessun errore, token reali applicati, matrice rispettata |

`backend` e `frontend` non sono stati eseguiti: la change non tocca codice di prodotto. L'unico codice
modificato è lo strumento dell'area `tooling`, che è verde.

## Copertura end-to-end

**Nessun impatto** sui percorsi esistenti (la change non tocca superficie applicativa). Le storie nuove
entrano nel registro come esenzioni temporanee; i due percorsi di piattaforma futuri — `J-ROLES` (la
stessa applicazione vista dai quattro ruoli) e `J-SEATS` (il ciclo di vita del posto) — sono progettati
in UC 0113 e saranno scritti quando l'epica si implementa.

## I due punti aperti economici, chiusi dallo sviluppatore

- **Account esistenti oltre la franchigia**: non esistono — la piattaforma è ancora solo in locale. Cade
  l'intera parte economica della migrazione; resta un controllo che la fa **fermare** se il presupposto non
  valesse più.
- **Pagamento dei posti non riuscito**: errore definitivo → l'aggiunta dell'utente **non procede**;
  temporaneo → si **ritenta**; sistematico → **email all'amministratore di appgrove**. In nessun caso le
  persone perdono accesso o l'account si blocca: il guasto impedisce di *aggiungere*, non di *lavorare*.

## Che cosa resta da fare, e non è stato fatto di proposito

Su richiesta esplicita dello sviluppatore, **dopo** la sua approvazione dell'analisi:

1. aggiornare `docs/usecases/EPICS-WAVE-2.md` con le sedici storie nell'ordine di esecuzione;
2. marcare le storie `0072`–`0074` dell'epica 14 come **superate** dall'epica 22 e togliersi dall'onda 2
   (restano come archivio della decisione precedente, non si cancellano).

## I punti aperti rimasti

Elencati nell'indice dell'area, ognuno posseduto da una storia. I due economici sono **chiusi** (§sopra);
restano quattro punti minori, nessuno bloccante per iniziare:

| Punto | Storia |
|---|---|
| Posti durante un periodo di prova gratuito di un'applicazione (proposta: si pagano) | 0103 |
| Comunicazione ai clienti in caso di rincaro del listino (elenco fornito, invio da progettare) | 0105 |
| Sorte della quota `seats` nel listino del Mini-CRM, i cui posti locali vengono ritirati | 0111 |
| Formulazione dell'informativa su ambito dei dati e titolarità, da portare alla revisione legale | 0115 |

## Chiusura: l'analisi approvata entra nell'onda 2

Riletta e approvata dallo sviluppatore, l'analisi è stata integrata in
[EPICS-WAVE-2.md](../../docs/usecases/EPICS-WAVE-2.md) come **fase A2 dedicata** con le ventuno storie in ordine
topologico (voce 59). La collocazione — subito dopo gli abilitanti di piattaforma, **prima della messa in cloud** —
non è una preferenza: l'epica non ha prerequisiti evolutivi pendenti (`0077`, `0085`, `0090`, `0093`, `0095`,
`0096`, `0097` sono tutti già in `main`, verificato riga per riga) ed è quindi eseguibile subito; e `0116` divide in
due la tabella delle persone mentre `0113` migra gli account, operazione oggi **banale** perché nessun account
supera i tre posti gratuiti e siamo in locale. Dopo il go-live sarebbe una migrazione su dati reali, con clienti che
pagano posti. Lo stesso ragionamento vale per il listino: partire col modello a scaglioni già in piedi evita di
cambiare le regole di prezzo a chi le ha già accettate.

Le tre storie dell'epica 14 sono **superate ma conservate** (voce 60): ognuna porta in testa il blocco che dichiara
il superamento e indica da quali storie dell'epica 22 è sostituita, e la loro epica è marcata come archivio nel
catalogo. Cancellarle avrebbe fatto perdere il ragionamento — che è precisamente la parte utile a chi un giorno
riaprisse la questione.
