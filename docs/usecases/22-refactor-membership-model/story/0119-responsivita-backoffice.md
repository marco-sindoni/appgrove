# UC 0119 — Responsività del backoffice: tabelle a più colonne, barra laterale e comandi su schermo stretto

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.3 Esperienza per ruolo](../epic/E22-03-esperienza-per-ruolo.md)
**Dipendenze**: UC 0100 (elenco unico delle persone, la tabella su cui il problema è emerso), UC 0103 e UC 0104 (riquadro dei posti e colonna di selezione, che quella tabella l'hanno allargata), UC 0020 (shell del backoffice), UC 0019 (design system)
**Requisito che la origina**: [docs/03-frontend.md](../../../03-frontend.md) punto 12 — **preesistente**, questa storia non lo inventa
**Origine**: collaudo manuale del lotto `0095`–`0099` (22 agosto 2026): lo sviluppatore ha giudicato la schermata «Membri» «poco mobile-friendly»
**Piano di lavoro**: nessuno separato — vedi la nota in coda al §10
**Ultimo aggiornamento**: 2026-08-22

## 1. Obiettivo / Scope

Rendere **esigibile** un requisito che esiste già. [docs/03-frontend.md](../../../03-frontend.md) punto 12
prescrive: «**Tutto responsive dal PoC**, backoffice incluso: sidebar collassabile/drawer su mobile,
tabelle responsive, canvas touch-friendly». È scritto dal principio, ma **nessuna storia dell'epica 22 lo
presidia** e **nessun collaudo automatico lo misura**: tutti i percorsi del backoffice girano su finestra
da scrivania. Un requisito che nessuno misura non è un requisito, è un auspicio.

Questa storia non decide *se* il backoffice deve essere responsive — è già deciso. Decide **con quale
criterio verificabile** lo si afferma, e mette quel criterio dentro la suite automatica, così che
smettere di rispettarlo diventi **rosso** invece di essere una scoperta a collaudo.

**Perché adesso e non alla fine dell'epica.** La tabella delle persone ha già **sette colonne**
(selezione, indirizzo di posta, nome, stato, applicazioni, data di ingresso, azioni: la selezione l'ha
aggiunta UC 0104) e le storie che restano le aggiungono ancora roba — comandi di riga, indicatori, e
una gemella dentro ogni applicazione (UC 0111). Scoprire il difetto **dopo** altre undici storie
significa rifarne un pezzo: il costo della correzione cresce con ogni colonna, e il collaudo messo qui
sorveglia anche tutto ciò che arriva dopo.

**Incluso**: il comportamento su schermo stretto delle schermate del backoffice che hanno una tabella o
un elenco di comandi (perimetro al §6); la verifica della barra laterale a cassetto, che **esiste già** e
va provata, non rifatta; la forma dei comandi di riga; il criterio di misura e il collaudo automatico che
lo applica; il minimo dell'area di tocco.

**Escluso**: la **console di piattaforma** (l'applicazione `admin`), che ha le stesse tabelle ma un'altra
platea e un altro ciclo di vita → §Punti aperti; il **sito vetrina**, già responsive e con i suoi
controlli post-costruzione; le **tele interattive touch** («canvas») del punto 12, che non esistono nel
prodotto di oggi; **ogni colonna, comando o schermata nuova**, che appartiene alla storia che la porta;
il tema chiaro/scuro e l'accessibilità in generale, che non sono in discussione qui.

## 2. Attori & ruoli

- **Owner**: è l'attore su cui il problema è emerso, perché le schermate più larghe (persone, posti,
  fatturazione) sono sue.
- **Collaboratore** (`viewer`/`editor`/`admin` di applicazione): vede meno schermate ma le stesse
  tabelle dentro le applicazioni.
- **Nessun attore nuovo, nessun permesso nuovo**: cambia la disposizione, non chi può fare cosa.

## 3. Precondizioni

- La **barra laterale a cassetto esiste già**:
  [ShellLayout.tsx](../../../../frontend/apps/backoffice/src/shell/ShellLayout.tsx) tiene la barra fissa
  sopra i 1024 pixel di larghezza (`lg:block`) e un cassetto sovrapposto sotto, con la sua maschera e il
  comando di chiusura. La storia lo **verifica** (oggi nessun collaudo lo esercita) e non lo riscrive.
- Il design system ha già una **tabella con scorrimento orizzontale contenuto**:
  [Table.tsx](../../../../frontend/packages/design-system/src/components/Table.tsx) avvolge `<table>` in
  un contenitore `overflow-x-auto`. Alcune schermate — fra cui
  [MembersPage.tsx](../../../../frontend/apps/backoffice/src/pages/members/MembersPage.tsx) — **non lo
  usano**: hanno una tabella scritta a mano dentro una scheda. Il lavoro va fatto **in un posto**, non
  schermata per schermata.
- I collaudi di livello 2 del backoffice esistono e girano con backend **simulato**
  (`page.route`), su un solo profilo `chromium` con finestra da scrivania
  ([playwright.config.ts](../../../../frontend/apps/backoffice/playwright.config.ts)).
- **Verifica fatta**: in tutto il repository **non esiste** un solo collaudo che imposti una finestra
  ridotta (nessuna occorrenza di `viewport`/`setViewportSize` fuori dagli strumenti che catturano le
  immagini della vetrina). Non c'è quindi una convenzione da seguire: questa storia la introduce, e la
  introduce nella forma più vicina a ciò che il progetto già fa.

## 4. Flusso principale — che cosa deve accadere su schermo stretto

Sei affermazioni, tutte misurabili (il §9 dice come):

1. **La pagina non deborda.** Su una finestra da telefono nessuna schermata del perimetro produce
   scorrimento orizzontale **della pagina**. Se una tabella è più larga della finestra, è il **suo
   contenitore** a scorrere, non il documento: la barra superiore, la navigazione e i riquadri restano
   dove sono.
2. **La barra laterale non occupa spazio.** Sotto la soglia la barra è assente e il comando che apre il
   cassetto è visibile e raggiungibile; a cassetto aperto una voce di menu naviga **e chiude** il
   cassetto; la maschera lo chiude.
3. **Le tabelle a molte colonne diventano schede.** Sopra le **quattro** colonne, su schermo stretto la
   tabella si trasforma in **una scheda per riga**: in testa l'identità della riga (per le persone,
   l'indirizzo di posta), poi le informazioni come coppie etichetta-valore, in fondo i comandi. Sotto le
   quattro colonne resta una tabella, con lo scorrimento contenuto del punto 1.
   *Perché le schede e non lo scorrimento per tutte*: sette colonne da scorrere fanno perdere di vista
   **di chi** è la riga — si scorre a destra per trovare il comando e non si sa più su chi si sta per
   agire, che su un elenco di persone con azioni distruttive è esattamente l'errore da non rendere
   possibile.
4. **I comandi restano raggiungibili.** Oggi i comandi stanno in fondo alla riga, cioè nel punto più
   lontano dal bordo visibile. Nella scheda stanno **sotto le informazioni della persona a cui si
   riferiscono**, dentro lo stesso contenitore: chi tocca vede il nome e il comando insieme.
5. **Il bersaglio del tocco è abbastanza grande.** Ogni comando ha un'area di almeno **44×44 pixel**,
   la misura minima raccomandata dalle linee guida internazionali di accessibilità dei contenuti web
   (criterio 2.5.5, «dimensione del bersaglio»). È un numero, e un numero si misura.
6. **I riquadri d'intestazione si impilano.** Il riquadro dei posti (UC 0103) e i suoi gemelli passano
   da affiancati a impilati, senza troncare gli importi: un prezzo tagliato a metà è peggio di un prezzo
   su due righe.

**Sopra la soglia non cambia nulla.** Questa storia non è un ridisegno: la resa su schermo largo di oggi
è quella approvata coi prototipi e deve restare identica. Il presidio è che la suite di livello 2
esistente — tutta su finestra da scrivania — resti verde.

## 5. Flussi alternativi / edge / errori

- **Edge — la larghezza di mezzo** (tavoletta, 768–1023 pixel): il cassetto è già lì per costruzione
  (la soglia è 1024). Le tabelle restano tabelle: a quella larghezza sette colonne stanno, con lo
  scorrimento contenuto quando servono.
- **Edge — la selezione multipla** delle persone da cessare (UC 0104): nella forma a schede la casella
  di selezione sta **nella scheda**, e la riga di riepilogo con la stima («indicando queste persone
  pagherai …») deve restare **visibile** e non finire fuori dallo schermo: è l'informazione che rende la
  conferma consapevole.
- **Edge — testi lunghi**: indirizzi di posta e nomi lunghi si troncano con il valore completo
  disponibile a chi usa un lettore di schermo, **mai** allargando il contenitore.
- **Edge — le lingue lunghe**: le etichette in tedesco sono le più larghe delle cinque lingue. Un
  criterio di non-debordamento verificato solo in italiano o in inglese **passa e mente**: almeno una
  schermata del perimetro va provata nella lingua più larga.
- **Edge — tabella vuota o in caricamento**: gli stati di vuoto, errore e caricamento non cambiano
  forma; devono però stare dentro la larghezza, che è l'unica cosa da verificare.
- **Errore — nessuno nuovo.** Non nascono chiamate né esiti nuovi: se qualcosa fallisce, fallisce come
  prima.
- **Trappola da evitare**: la forma a schede **non deve mostrare** comandi che la forma tabellare nega a
  quel ruolo. La disposizione non è un varco: le regole di visibilità restano quelle di UC 0107 e i
  comandi si calcolano una volta, non due.

## 6. Schermate & stati — il perimetro

Le schermate del backoffice del cliente che oggi hanno una tabella o una riga di comandi:

| Schermata | Dove | Perché è nel perimetro |
|---|---|---|
| **Persone («Membri»)** | [MembersPage.tsx](../../../../frontend/apps/backoffice/src/pages/members/MembersPage.tsx) | **È quella su cui il difetto è emerso**: sette colonne, comandi in fondo alla riga, tabella scritta a mano |
| **Shell** (barra laterale, barra superiore, cassetto) | [ShellLayout.tsx](../../../../frontend/apps/backoffice/src/shell/ShellLayout.tsx), [Sidebar.tsx](../../../../frontend/apps/backoffice/src/shell/Sidebar.tsx) | Il cassetto esiste ma **nessun collaudo lo esercita**; il contenuto ha margini fissi (`px-[34px]`) che su 390 pixel si mangiano 68 pixel di larghezza utile |
| **Fatturazione («Billing»)** | [PaymentsPanel.tsx](../../../../frontend/apps/backoffice/src/billing/PaymentsPanel.tsx) | Storico dei pagamenti a tabella; UC 0106 vi aggiunge le righe dei posti |
| **Supporto** | [SupportPage.tsx](../../../../frontend/apps/backoffice/src/pages/support/SupportPage.tsx) | Elenco dei ticket a tabella |
| **Moduli applicativi**: elenco delle fatture, elenco dei contatti del Mini-CRM | [InvoiceListScreen.tsx](../../../../frontend/apps/backoffice/src/modules/fatture/screens/InvoiceListScreen.tsx), [ContactListScreen.tsx](../../../../frontend/apps/backoffice/src/modules/crm/screens/ContactListScreen.tsx) | Sono le tabelle che il cliente guarda ogni giorno, e il modello di ciò che le applicazioni future genereranno |

**Fuori dal perimetro, dichiarato**: la console di piattaforma (applicazione `admin`: ticket, dettaglio
account, diritti dell'interessato) → §Punti aperti; la schermata dei posti locali del Mini-CRM
(`MembersScreen.tsx` del modulo), che UC 0111 **ritira**: metterla in ordine sarebbe lavoro su codice
che sta per essere cancellato.

**Nota sui prototipi.** I cinque [prototipi navigabili](../prototype/README.md) dell'epica sono disegnati
a schermo largo e **non dicono nulla** sullo schermo stretto. La forma a schede del §4 è quindi una
decisione **di questa storia**, non una lettura dei prototipi: va riletta al varco dei requisiti della
change che la implementa.

## 7. Dati toccati

**Nessuno.** Nessuna tabella, nessuna colonna, nessuna chiamata nuova: cambia la disposizione di
informazioni già mostrate alle stesse persone.

**Dati personali**: nessun trattamento nuovo. Indirizzo di posta e nome delle persone sono già
dichiarati in UC 0013 e già visibili in queste schermate; mostrarli impilati invece che affiancati non
cambia né la categoria, né la finalità, né la base giuridica. Un solo dettaglio da rispettare: nella
forma a schede l'indirizzo di posta diventa **il titolo** della scheda, quindi più grande — è la stessa
informazione, alla stessa platea.

## 8. Permessi & gate

- **Nessun cambiamento.** Le regole di visibilità per ruolo sono di UC 0107; le rotte riservate
  all'owner restano quelle di UC 0100.
- **Invarianti di piattaforma**: intatte. Nessuna interrogazione nuova, quindi nessun nuovo punto in cui
  il filtro per account possa essere dimenticato; l'account continua ad arrivare **solo** dal token
  verificato.
- **Regola da rispettare in implementazione**: i comandi ammessi si calcolano **una volta** e le due
  forme (tabella e scheda) leggono lo stesso risultato. Due elenchi di comandi divergono, e divergendo
  uno dei due mostra qualcosa che non deve.

## 9. Requisiti di test — il criterio verificabile

Il cuore della storia. Il giudizio «poco mobile-friendly» non è ripetibile: va tradotto in asserzioni
che una macchina possa rifare a ogni commit.

**La forma scelta: un collaudo di livello 2 a finestra ridotta.** Nuovo file
`frontend/apps/backoffice/e2e/responsive.spec.ts`, nella cartella dove i collaudi di livello 2 del
backoffice già vivono, con la finestra dichiarata dal file stesso
(`test.use({ viewport: { width: 390, height: 844 } })`) invece che da un secondo profilo nella
configurazione. Motivi, in ordine di peso:

1. è un **browser vero**, quindi calcola la disposizione: è l'unico livello che può misurarla;
2. **non tocca la configurazione** esistente né aggiunge un secondo profilo, quindi la suite da
   scrivania resta esattamente com'è e i tempi non raddoppiano;
3. usa il **backend simulato** che quei collaudi già usano: la geometria non ha bisogno di dati veri, e
   pagare lo stack completo per misurare dei rettangoli sarebbe spreco. Per questo **non** è un percorso
   della suite di piattaforma.

**Perché non i collaudi di componente.** I collaudi di componente girano in un finto ambiente di
navigazione (`jsdom`) che **non calcola alcuna disposizione**: ogni rettangolo misurato là vale zero. Su
di essi resta solo ciò che è di contenuto — «la scheda mostra le stesse voci della riga» — non la
geometria. Dirlo qui serve a non far scrivere collaudi che passano sempre.

**Le asserzioni**, per ognuna delle schermate del perimetro:

- **niente debordamento della pagina**: la larghezza scorribile del documento non supera quella
  visibile (con la tolleranza di un pixel per gli arrotondamenti);
- **nessun elemento interattivo ritagliato**: il rettangolo di ogni comando visibile sta dentro la
  finestra;
- **area di tocco**: il rettangolo di ogni comando misura almeno 44×44 pixel;
- **cassetto**: la barra laterale non è visibile; il comando di apertura sì; una voce naviga e chiude il
  cassetto;
- **identità accanto ai comandi**: nella forma a schede, l'indirizzo di posta della persona e i suoi
  comandi appartengono allo **stesso** contenitore (verificato restringendo la ricerca alla scheda, non
  guardando i pixel);
- **scorrimento contenuto**: dove la tabella resta tabella, è il contenitore a scorrere e il documento
  no.

**Le altre reti**:

- **Non regressione su schermo largo**: la suite di livello 2 esistente (finestra da scrivania) resta
  verde. È il presidio contro il «responsive che rompe il largo».
- **Lingua lunga**: almeno la schermata delle persone provata anche in tedesco, con la stessa asserzione
  di non-debordamento.
- **Controllo dei tipi e suite**: `./run-tests.sh frontend` verde (comprende il controllo dei tipi, che
  dalla change `0075` è parte del cancello).

**Il valore duraturo**: da quel momento ogni storia successiva che aggiunge una colonna alla tabella
delle persone o un comando alla riga trova il collaudo **rosso** se lo fa senza pensare allo schermo
stretto. È la ragione per cui questa storia sta prima e non dopo.

### Journey end-to-end di piattaforma

**Journey nuovo**, di livello 2: `L2-RESPONSIVE` — «le schermate del backoffice su finestra da
telefono: niente debordamento, cassetto al posto della barra, comandi accanto alla persona a cui si
riferiscono». Il file è quello del §9, con l'etichetta `[L2-RESPONSIVE]` in testa al titolo dei test.

**Non** è un percorso della suite di piattaforma, e la ragione è dichiarata sopra: la disposizione non
ha bisogno di banca dati, code ed email vere. Aggiungere una voce di piattaforma qui costerebbe minuti
di esecuzione e non asserirebbe nulla in più.

Nel registro [copertura-e2e.yaml](../../../testing/copertura-e2e.yaml) questo use case è oggi esente come
`non-implementato`: la change che lo implementa **toglie l'esenzione**, aggiunge `0119` fra le superfici
e crea la voce `L2-RESPONSIVE` con il suo file.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [docs/03-frontend.md](../../../03-frontend.md) punto 12 (il requisito che preesiste) ·
  [docs/10-testing.md](../../../10-testing.md) (i livelli di collaudo) ·
  [docs/testing/README.md](../../../testing/README.md) (registro e convenzione dell'etichetta) ·
  [UC 0100](0100-sezione-members-elenco-unico.md) e [UC 0104](0104-riduzione-posti-in-attesa.md) (le
  storie che hanno allargato la tabella) · [UC 0111](0111-schermata-gestione-utenti-app.md) (la tabella
  gemella dentro le applicazioni, che nascerà dopo e dovrà nascere già stretta).
- **Definition of Done**:
  1. il criterio del §9 è scritto come collaudo automatico e **verde**, con l'etichetta e la voce nel
     registro di copertura;
  2. le schermate del perimetro non debordano su finestra da telefono, in italiano e nella lingua più
     larga;
  3. il cassetto della barra laterale è **esercitato** da un collaudo, non solo presente nel codice;
  4. ogni comando delle schermate del perimetro ha un'area di tocco di almeno 44×44 pixel;
  5. la resa su schermo largo è invariata: la suite di livello 2 esistente resta verde;
  6. il lavoro sulla forma delle tabelle sta nel **componente condiviso**, non ripetuto schermata per
     schermata;
  7. l'esenzione `non-implementato` di `0119` è rimossa dal registro;
  8. `./run-tests.sh frontend tooling` verde.

**Nota — nessun piano di lavoro separato.** Le ventuno storie nate dall'analisi della change `0087`
hanno un gemello in [task/](../task/); questa e UC 0120 sono state aggiunte dopo, e i loro passi stanno
qui (§4, §6, §9). Non è una dimenticanza: scrivere un piano di lavoro per una storia che tocca due
componenti e un file di collaudo aggiungerebbe un documento e nessuna informazione. Se la change che la
implementa ne sentirà il bisogno, lo scriverà lei.

## Punti aperti / decisioni differite

- **La console di piattaforma (applicazione `admin`) ha lo stesso difetto**, e non è nel perimetro:
  [Tickets.tsx](../../../../frontend/apps/admin/src/pages/Tickets.tsx),
  [AccountDetail.tsx](../../../../frontend/apps/admin/src/pages/AccountDetail.tsx) e
  [GdprRights.tsx](../../../../frontend/apps/admin/src/pages/GdprRights.tsx) hanno tabelle a molte
  colonne con lo stesso scorrimento orizzontale. Resta fuori per una ragione di merito: la sua platea è
  **una persona sola** (l'amministratore di appgrove) che lavora a scrivania, mentre il backoffice lo
  usano i clienti. Proprietario: [UC 0021](../../06-frontend/0021-console-admin-spa.md), dove il rimando
  è annotato.
- **La soglia esatta e la forma per le tabelle diverse da quella delle persone** (quattro colonne come
  confine, schede contro scorrimento) è la proposta del §4: si rifinisce sul concreto, schermata per
  schermata, dentro la change. Se una tabella dimostra di stare meglio in tabella, si tiene tabella —
  purché il criterio del non-debordamento regga.
- **Le tele interattive touch** del punto 12 di [docs/03-frontend.md](../../../03-frontend.md)
  («canvas touch-friendly», pensate per calendari e mappe mentali) non hanno oggi nulla da presidiare:
  nel prodotto non esiste una tela. Il requisito resta scritto e tornerà esigibile con la prima
  applicazione che ne porta una. Proprietario: quella applicazione.
- **Un controllo che vieti in generale i margini fissi in pixel** sui contenitori di pagina sarebbe più
  forte del collaudo per schermata, ma è una regola di stile da far rispettare a un analizzatore
  statico, non a un browser. Rimandata: prima si vede se il collaudo per schermata basta. Proprietario:
  [UC 0086](../../18-brand-e-design-system/0086-brand-kit-token-condiviso.md), che possiede i token e le
  regole del design system.
