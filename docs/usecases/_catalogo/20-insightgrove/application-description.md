# InsightGrove — descrizione dell'applicazione

**Numero di catalogo**: 20 · **Tipo**: orizzontale · analytics · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 20](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** InsightGrove risponde a una sola domanda — *come sta andando l'azienda* — usando i numeri che le
**altre applicazioni appgrove** producono già lavorando. Non chiede al titolare di inserire niente: le app della
suite pubblicano i propri numeri (fatturato emesso, incassato, crediti scaduti, valore delle trattative aperte,
valore di magazzino, ore lavorate, ticket aperti) e InsightGrove li raccoglie, li mette in un **catalogo di
indicatori** definiti una volta sola, li mostra su cruscotti, suona quando una soglia viene superata e risponde a
domande scritte in italiano corrente. Ogni numero che mostra porta con sé la propria **scheda**: da quale
indicatore viene, su che periodo, da quali fonti, con quanti fatti, aggiornato a quando — e un rimando che porta
alla riga d'origine dentro l'app che l'ha prodotta.

**Per chi.** Micro-imprese da 1 a 10 addetti e piccole imprese fino a 50, mercato globale con priorità europea.
Chi compra è **il titolare**, ed è anche il lettore principale: guarda i numeri il lunedì mattina e prima di
parlare col commercialista. Chi usa tutti i giorni sono anche gli altri — l'addetto all'amministrazione, il
magazziniere, il commerciale — ma **vedono cose diverse**, ed è precisamente il punto delicato di questa app
(§1 di questo elenco e §6.2 più sotto). Il profilo tipico ha da due a cinque app appgrove attive e non ha
nessuno il cui mestiere sia «guardare i dati».

**Quale problema toglie.** Oggi il titolare di una micro-impresa che vuole sapere come va fa una di tre cose:
apre le app una per una e somma a mente (e ogni app gli dà un numero diverso, perché «fatturato» in una vuol dire
emesso e nell'altra incassato); esporta tre fogli di calcolo e li incolla in un quarto, una volta al trimestre,
quando ha tempo — cioè mai; oppure chiede al commercialista, che risponde con il bilancio dell'anno scorso.
Il costo non è il tempo: è che **le decisioni si prendono a naso**. Il materiale consultato per questa scheda
converge su un punto — a una micro-impresa non servono cento indicatori, ne bastano da cinque a dieci, ma devono
essere **gli stessi ogni mese e definiti in modo univoco** (§2.5, fonti 5 e 6).

C'è un secondo problema, più insidioso: gli strumenti che oggi promettono di rispondere a domande sui dati
aziendali con un modello linguistico **sbagliano in silenzio**. La letteratura del 2026 misura il salto fra
laboratorio e realtà: gli stessi modelli che raggiungono l'85-90 % di risposte corrette sui banchi di prova
accademici scendono al **39,1 %** su un banco di prova costruito su schemi d'impresa veri (§2.5, fonte 4).
Una risposta sbagliata che *sembra* giusta è peggio di nessuna risposta: su quel numero il titolare decide se
assumere. È la ragione per cui l'intera architettura di questa app è costruita attorno alla **tracciabilità del
numero**, non attorno alla conversazione (§4.3).

**Cosa NON fa.**

- **non accede alle basi di dati delle altre app.** Non ha, e non deve avere, alcun modo di leggere lo schema di
  un'altra applicazione: riceve *fatti* pubblicati sul bus di eventi e li scrive nel proprio schema. Il perché sta
  al §4.2: è il cuore tecnico di questa app;
- **non conserva il dettaglio delle altre app.** Non tiene le fatture, non tiene i clienti, non tiene i movimenti
  di magazzino. Tiene **numeri con dimensioni** e un rimando alla riga d'origine. Se si vuole vedere la fattura,
  si va in BillGrove;
- **non è un foglio di calcolo.** Non si scrivono formule libere, non si caricano file, non si costruiscono
  tabelle a piacere. Gli indicatori sono un **catalogo chiuso e versionato**: è la scelta che rende i numeri
  ripetibili e verificabili, ed è anche il suo limite (§11, rischio 2);
- **non collega fonti esterne alla suite.** Niente connettori verso strumenti di terzi (contabilità esterna,
  pubblicità, negozio in rete): quelli sono la materia dell'app 55 SyncGrove. InsightGrove legge appgrove;
- **non prende decisioni al posto di nessuno** e non assegna punteggi automatici a persone: mostra numeri e li
  spiega;
- **non è un sistema di controllo del personale.** Gli indicatori sull'attività delle persone dell'account
  (per esempio ticket chiusi per operatore) sono **fuori ambito** in questa proposta, e il motivo è al §6.3:
  la disciplina del controllo a distanza dell'attività lavorativa è la stessa che ha portato all'esclusione
  dell'app 11 ShiftGrove dal catalogo attivo;
- **non fa budget e scostamento dal budget.** È l'app 54 BudgetGrove: qui si guarda il consuntivo, là si mette a
  confronto con un piano. La sovrapposizione è reale ed è dichiarata al §10.

**Rischio di sostituzione da parte dei modelli linguistici.** `rafforzata`, come nel catalogo, ma per un motivo
diverso da quello che si direbbe. La parte conversazionale, presa da sola, è la più facile da imitare: chiunque
può incollare un foglio in una chat e chiedere «quanto ho fatturato». Ciò che un assistente generico **non può
fare** è garantire che quel numero sia lo stesso della settimana scorsa, che venga da tutte le fonti e non da
metà, e che si possa risalire alla riga che lo ha prodotto. Il valore non è la risposta: è la **catena di custodia
del numero**. Un assistente generico dà una risposta; questa app dà una risposta con la ricevuta.

---

## 2. Mercato e analisi in rete

> Compilata dopo **8 ricerche mirate e 3 recuperi di pagina** ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4).
> Ciò che non è stato trovato è **dichiarato** al §2.7, non colmato a intuito.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| Databox | Stati Uniti | Cruscotti di indicatori senza scrivere codice, ~60-100 connettori verso strumenti di terzi; il riferimento della categoria per le piccole squadre | Gratuito: 3 fonti, 1 utente, 50 crediti di intelligenza artificiale al mese; Analyst 64 $/mese (5 fonti, 1 utente); Pro 159 $/mese (utenti illimitati, 3 fonti, +5,60 $/mese per fonte aggiuntiva); Growth 399 $/mese; prova di 14 giorni; annuale −20 % — **rilevato sulla pagina ufficiale** | [databox.com/pricing](https://databox.com/pricing) |
| Zoho Analytics | India / Unione europea (centro dati selezionabile) | Analisi e cruscotti con assistente «Ask Zia»; parte del pacchetto Zoho, quindi già dentro le altre app della stessa suite — **il concorrente strutturalmente più vicino a InsightGrove** | Piano gratuito 2 utenti e 10.000 righe; a pagamento da ~25 $/mese (2 utenti, 500.000 righe) fino a ~495 $/mese (50 utenti, 50 milioni di righe); prova di **15 giorni senza carta**; annuale −20 %; utenti e «spettatori» aggiuntivi si pagano a parte (minimo 25 spettatori) — **rilevato sulla pagina ufficiale** | [zoho.com/analytics/pricing.html](https://www.zoho.com/analytics/pricing.html) |
| Geckoboard | Regno Unito | Cruscotti da parete, molto semplici, orientati alla squadra che guarda un numero tutto il giorno | Essential 44 $/mese (1 cruscotto, 1 utente); Pro 87 $/mese (5 cruscotti, 10 utenti); Scale 615 $/mese; prova di 14 giorni; annuale −20 %; ogni cruscotto o utente oltre il piano costa 25 $/mese — **valori da fonte editoriale**, non dalla pagina ufficiale (§2.7) | [coefficient.io — Geckoboard pricing 2026](https://coefficient.io/geckoboard-pricing) |
| Klipfolio PowerMetrics | Canada | Prodotto costruito attorno alla **definizione centralizzata delle metriche** — la stessa idea che sta al §4.3 di questa scheda | Piano gratuito per singoli; il piano «Go» a ~125 $/mese è dove il prodotto si apre davvero — **valori da fonte editoriale**, la pagina ufficiale non è stata rilevata (§2.7) | [coefficient.io — Klipfolio pricing 2026](https://coefficient.io/klipfolio-pricing) |
| Microsoft Power BI (con Copilot) | Stati Uniti | Lo strumento che il commercialista e il consulente conoscono; è anche il metro con cui i clienti giudicano le funzioni di esportazione e di invio programmato | **prezzo non rilevato** in questa analisi; entra qui come riferimento funzionale, non di prezzo | [learn.microsoft.com — esportare in PDF](https://learn.microsoft.com/en-us/power-bi/collaborate-share/end-user-pdf) |

**Lettura.** La categoria è matura e cara: il gradino d'ingresso vero sta fra 44 e 159 $/mese, e i due prodotti
con un gratuito serio (Databox, Zoho) lo tengono strettissimo. Ma nessuno di questi concorre davvero con
InsightGrove, perché **tutti risolvono un problema che qui non esiste**: collegare fonti eterogenee. Databox
vende 100 connettori e li fa pagare a uno a uno; Geckoboard fa pagare il cruscotto; Zoho fa pagare le righe.
InsightGrove ha **una sola fonte**, la suite stessa, già intestata all'account, già col significato dei campi
noto. Sparisce l'intero costo del collegamento — che è ciò che nei concorrenti si paga — e resta il valore.
Lo spazio competitivo non è quindi «un Databox più economico»: è **il cruscotto che c'è già acceso il giorno in
cui attivi la seconda app**, senza configurare niente. Il vero concorrente strutturale è Zoho Analytics, per la
stessa ragione: è dentro la sua suite.

### 2.2 Prezzi praticati nel dominio

- **Fascia rilevata**: gradino d'ingresso 25-64 $/mese (Zoho, Databox Analyst), fascia media 87-159 $/mese
  (Geckoboard Pro, Databox Pro), fascia alta 400-615 $/mese. La scheda di catalogo indica 19-49 €/mese, cioè
  **sotto il mercato rilevato**: la differenza si spiega col fatto che i concorrenti vendono i connettori e noi no
  (§2.1).
- **Unità di misura**: quattro modelli in concorrenza, nessuno dominante — **fonti dati collegate** (Databox),
  **righe di dato** (Zoho), **cruscotti e utenti** (Geckoboard), **metriche e squadra** (Klipfolio). Nessuno fa
  pagare le *domande* poste all'assistente come metrica principale del piano.
- **Crediti per l'intelligenza artificiale**: è invece il modello che si sta affermando come **secondo asse**,
  accanto al piano. HubSpot vende crediti a 10 $ per 1.000 (cioè 0,010 $ a credito, ~0,009 $ con impegno annuale)
  e li consuma a tariffe diverse per funzione — 50 crediti per una conversazione risolta, 100 per un contatto
  suggerito. Databox include 50 crediti al mese nel gratuito e 500-4.000 nei piani a pagamento.
  Fonti: [huboexperts.com — HubSpot credits 2026](https://www.huboexperts.com/blog/hubspot-credits-pricing-changes-2026-guide) ·
  [databox.com/pricing](https://databox.com/pricing).
  **Conseguenza per appgrove**: il modello a consumo con addebito allo sforamento è **vietato** dalla piattaforma
  (solo abbonamento ricorrente, al limite si blocca a `429`, non si addebita). La lettura corretta di quel
  segnale di mercato è quindi: *la metrica giusta da limitare è la domanda all'assistente*, ma limitata come
  **tetto mensile che blocca**, non come credito che si ricarica. È ciò che fa la proposta al §5.
- **Piano gratuito**: presente in Databox, Zoho e Klipfolio, sempre molto stretto (1-2 utenti, poche fonti,
  poche righe). Serve a provare, non a vivere.
- **Prova gratuita**: 14 giorni (Databox, Geckoboard), 15 giorni senza carta (Zoho). La convenzione appgrove —
  14 giorni con carta richiesta — è in linea.
- **Sconto annuale**: −20 % dichiarato da tutti e tre i prodotti con pagina ufficiale rilevata. La convenzione
  appgrove (annuale = 10× il mensile, cioè −17 %) è quindi **leggermente meno generosa** del riferimento.

### 2.3 Obblighi normativi del settore

Il dominio in sé è **poco normato**: nessuna legge impone di tenere un cruscotto di indicatori, e nessun
adempimento dipende dai numeri che questa app mostra. Va detto esplicitamente, perché è raro nel catalogo.
Restano però tre vincoli veri, che nascono non dal dominio ma da **cosa l'app fa con i dati**:

1. **Minimizzazione.** Un'app che aggrega dati di tutte le altre è la tentazione perfetta a raccogliere «tutto,
   che poi vediamo». Il principio di minimizzazione dice l'opposto: si riceve **solo ciò che serve a calcolare gli
   indicatori pubblicati**, e un fatto è un numero con dimensioni, non una copia della riga d'origine. È il
   requisito che governa il contratto del fatto (storia 0006).
2. **Uso ulteriore.** I dati dei clienti non si usano per finalità diverse da quelle per cui sono stati raccolti:
   nessuna aggregazione fra account, nessuna statistica di settore costruita sui dati dei clienti, nessun
   addestramento di modelli. Vale già come principio di piattaforma
   ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §10, «nessun uso secondario dei dati dei clienti») e qui
   va ribadito perché questa è **l'unica app del catalogo tecnicamente in grado di farlo**.
3. **Controllo a distanza dell'attività lavorativa.** Un indicatore per persona sull'attività di un dipendente
   ricade in una disciplina propria, con procedure di accordo o autorizzazione preventiva: è la stessa ragione per
   cui l'app 11 ShiftGrove è **esclusa** dal catalogo attivo
   ([_escluse/README.md](../_escluse/README.md)). Conseguenza sul prodotto: gli indicatori **per persona
   dell'account** sono fuori ambito in questa proposta (§1, «Cosa NON fa»), e il catalogo delle dimensioni non
   ammette la dimensione «operatore». È un punto aperto se un giorno lo si vorrà (§11, punto 5).

Un quarto punto, che è di prodotto e non di legge: i numeri di questa app **non sono un bilancio** e non hanno
valore contabile. La schermata deve dirlo, non nascosto in una nota legale: un titolare che porta al
commercialista il fatturato «di InsightGrove» e scopre che non coincide con quello fiscale ha perso fiducia in
tutta la suite. È requisito della storia 0016.

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta prevedibile:

1. **le altre app appgrove** — è l'integrazione, ed è interna: nessun fornitore esterno, nessun trasferimento di
   dati fuori dalla piattaforma;
2. **esportazione in foglio di calcolo** — la richiesta numero uno di tutte le categorie di analisi, e la via con
   cui i numeri arrivano al commercialista (storia 0027). Nessun fornitore esterno: si produce un file;
3. **invio programmato per posta elettronica** — il rapporto del lunedì mattina che arriva senza che nessuno lo
   chieda (storia 0028). Usa il servizio di posta **già in uso dalla piattaforma**, non un fornitore nuovo;
4. **contabilità esterna** (il gestionale del commercialista) — richiesta prevedibile e **non coperta**: è
   materia dell'app 55 SyncGrove. Sarebbe il primo fornitore esterno vero, e per questo sta fuori;
5. **strumenti di pubblicità e negozio in rete** — la richiesta che tutti i concorrenti soddisfano e che qui
   **non si soddisfa**, perché nel catalogo attivo non c'è un'app di commercio elettronico (29 ShopGrove è fra le
   escluse). Va detto in fase di vendita, non scoperto dopo.

Solo il punto 4 introdurrebbe un fornitore esterno che tratta dati per nostro conto, ed è fuori ambito: **questa
proposta non aggiunge alcun responsabile esterno del trattamento**.

### 2.5 Aspettative funzionali dei clienti micro e piccoli

- **Pochi indicatori, sempre gli stessi.** Il materiale italiano sul controllo di gestione per piccole imprese
  converge su un cruscotto da **5-10 numeri**, aggiornato mensilmente o settimanalmente, e mette in guardia
  esplicitamente contro l'eccesso: «un eccesso di dati può risultare controproducente». Gli indicatori ricorrenti
  sono fatturato contro obiettivo, margine di contribuzione percentuale, **giorni medi di incasso**, liquidità
  disponibile oggi, valore di magazzino. Fonti:
  [studioaldegheri.it — cruscotto aziendale PMI](https://studioaldegheri.it/cruscotto-aziendale-pmi/) ·
  [canellacamaiora.it — KPI finanziari essenziali per le PMI](https://canellacamaiora.it/kpi-finanziari-essenziali-pmi-guida-pratica/).
  Conseguenza diretta sul prodotto: il cruscotto **nasce già pieno** con gli indicatori derivabili dalle fonti
  collegate (storia 0018), invece di partire vuoto e chiedere all'utente di costruirlo. È l'errore che rende
  inutili metà degli strumenti della categoria.
- **Il ciclo del contante è l'indicatore che conta davvero.** Le stesse fonti insistono su giorni di incasso,
  giorni di pagamento e giorni di giacenza: un'impresa piccola muore di liquidità, non di margine. Questi tre
  indicatori richiedono però **tre fonti diverse** (fatturazione, incassi, magazzino): è il caso d'uso che
  giustifica l'esistenza di questa app e insieme la conferma della nota del catalogo — «va costruita dopo che
  almeno 3-4 app popolano il dato».
- **Nessuno si fida di un numero che non può controllare.** Il materiale sul 2026 relativo agli assistenti
  analitici è concorde: le risposte sbagliate sono **plausibili**, non assurde, e passano inosservate perché
  nessuno vede la richiesta che le ha prodotte; chi ricalcola dal dato d'origine trova **circa tre volte più**
  errori veri di chi verifica chiedendo a un secondo assistente. Fonti:
  [mitzu.io — trasparenza della richiesta come unico controllo](https://mitzu.io/post/ai-analytics-hallucinations-sql-transparency/) ·
  [typedef.ai — costruire un assistente analitico](https://www.typedef.ai/resources/build-ai-analytics-copilot-databricks) (l'esempio del
  38 % di richieste con nomi di colonna inventati). E il dato quantitativo di riferimento:
  [Text-to-SQL Benchmarks for Enterprise Realities](https://openreview.net/forum?id=gXkIkSN2Ha) —
  **39,1 %** di risposte eseguibilmente corrette su BIRD-Ent e **60,5 %** su Spider-Ent, contro l'85-90 % dei
  banchi di prova accademici classici.
- **La direzione del mercato conferma il rimedio**: gli strumenti che vogliono essere affidabili non fanno
  scrivere richieste libere al modello, ma lo fanno scegliere dentro uno **strato semantico** di metriche
  governate — «definire metriche, dimensioni, giunzioni e permessi una volta sola, così che strumenti di analisi,
  fogli di calcolo e agenti restituiscano tutti lo stesso numero affidabile».
  Fonte: [cube.dev — che cos'è uno strato semantico](https://cube.dev/articles/what-is-a-semantic-layer).
  È esattamente l'architettura del §4.3.
- **Esportazione e invio programmato sono aspettative, non funzioni avanzate.** Il pubblico esterno (il
  commercialista, la banca) si aspetta un documento **statico e impaginato**, non un collegamento a un cruscotto
  vivo, e chi riceve non ha e non vuole un accesso allo strumento. Fonte:
  [rollstack.com — esportazione programmata](https://www.rollstack.com/articles/power-bi-scheduled-export).
  Conseguenza: l'esportazione in foglio di calcolo e il rapporto periodico via posta elettronica non sono
  rifiniture da rimandare, sono l'epica 06.
- **Cosa rifiutano.** Configurare. Nessuno dei clienti di questo segmento costruirà un cruscotto da zero, e la
  lamentela ricorrente sui prodotti della categoria è la curva di apprendimento (rilevata su Klipfolio) e il
  salto di prezzo brutale dal gratuito al primo piano a pagamento (rilevata su Databox: «da 0 a 159 $/mese senza
  niente in mezzo»). Fonti: [coefficient.io — Klipfolio](https://coefficient.io/klipfolio-pricing) ·
  [metricnexus.ai — Databox](https://metricnexus.ai/blog/databox-pricing).

### 2.6 Fonti consultate

1. **Databox — pagina ufficiale dei prezzi** — <https://databox.com/pricing> — piani e prezzi rilevati alla
   fonte: gratuito (3 fonti, 1 utente, 50 crediti/mese), Analyst 64 $, Pro 159 $, Growth 399 $, prova 14 giorni,
   annuale −20 %; conferma che i crediti per l'intelligenza artificiale sono ormai un asse di listino a sé.
2. **Zoho Analytics — pagina ufficiale dei prezzi** — <https://www.zoho.com/analytics/pricing.html> — quattro
   piani per numero di utenti e righe, gratuito a 2 utenti/10.000 righe, prova di 15 giorni senza carta, annuale
   −20 %, «spettatori» a pagamento con minimo 25: è il concorrente strutturalmente identico (analisi dentro una
   suite), e mostra che l'unità di misura naturale della categoria non è la domanda ma il volume di dato.
3. **Coefficient — prezzi di Geckoboard e Klipfolio (2026)** — <https://coefficient.io/geckoboard-pricing> ·
   <https://coefficient.io/klipfolio-pricing> — fascia 44-125 $/mese per la parte bassa del mercato dei cruscotti;
   segnalata la curva di apprendimento come lamentela ricorrente. Fonte editoriale, non ufficiale (§2.7).
4. **Text-to-SQL Benchmarks for Enterprise Realities (OpenReview, 2026)** —
   <https://openreview.net/forum?id=gXkIkSN2Ha> — 39,1 % su BIRD-Ent e 60,5 % su Spider-Ent contro l'85-90 %
   accademico: è il dato che vieta di far scrivere al modello interrogazioni libere sul magazzino dei fatti, ed è
   il fondamento del §4.3. Il documento è stato letto **attraverso i risultati di ricerca**: il recupero diretto
   della pagina è stato bloccato da una verifica antirobot (§2.7).
5. **Studio Aldegheri — cruscotto aziendale per PMI** — <https://studioaldegheri.it/cruscotto-aziendale-pmi/> —
   il cruscotto da 5-10 numeri vitali, con l'avvertenza contro l'eccesso di indicatori; elenco concreto:
   fatturato contro budget, margine di contribuzione, giorni medi di incasso, liquidità, valore di magazzino.
6. **Canella & Camaiora — KPI finanziari essenziali per le PMI** —
   <https://canellacamaiora.it/kpi-finanziari-essenziali-pmi-guida-pratica/> — il ciclo del contante scomposto in
   giorni di giacenza, giorni di incasso e giorni di pagamento, con la soglia d'allarme dei 60-90 giorni di
   incasso: sono gli indicatori derivati della storia 0013.
7. **Mitzu — allucinazioni analitiche e trasparenza della richiesta** —
   <https://mitzu.io/post/ai-analytics-hallucinations-sql-transparency/> — lo schema
   *domanda → richiesta generata → revisione → risposta approvata* e la tesi che gli errori analitici sono
   pericolosi **perché plausibili**. Ne ho ricavato la forma della «scheda del numero» (storia 0016).
8. **Cube — che cos'è uno strato semantico** — <https://cube.dev/articles/what-is-a-semantic-layer> — metriche,
   dimensioni e permessi definiti una volta sola perché ogni consumatore (analisi, foglio, agente) restituisca lo
   stesso numero: è la giustificazione del catalogo chiuso delle metriche (epica 03).
9. **Rollstack / Microsoft Learn — esportazione e invio programmato** —
   <https://www.rollstack.com/articles/power-bi-scheduled-export> ·
   <https://learn.microsoft.com/en-us/power-bi/collaborate-share/end-user-pdf> — il pubblico esterno vuole un
   documento statico e impaginato, non un accesso: fonda l'epica 06 e la scelta di **non** fare collegamenti
   pubblici a cruscotti vivi (§11, punto 4).
10. **HuboExperts — crediti HubSpot 2026** —
    <https://www.huboexperts.com/blog/hubspot-credits-pricing-changes-2026-guide> — 10 $ per 1.000 crediti,
    consumo a tariffa per funzione, tetti di spesa e avvisi al 75/85/90 %: è il riferimento di prezzo unitario
    citato dal catalogo, riletto qui dentro il vincolo appgrove «si blocca, non si addebita».

### 2.7 Cosa NON sono riuscito a determinare

- **Prezzi ufficiali di Geckoboard e Klipfolio** — le pagine ufficiali non sono state rilevate direttamente; i
  valori riportati vengono da una fonte editoriale del 2026 e vanno letti come ordini di grandezza. Servirebbe
  una lettura diretta delle due pagine di listino.
- **Il testo integrale del lavoro sui banchi di prova d'impresa** — il recupero della pagina OpenReview è stato
  bloccato da una verifica antirobot; le cifre 39,1 % / 60,5 % provengono dall'estratto restituito dalla ricerca
  ed è così che vanno citate. Servirebbe scaricare il documento per verificare condizioni sperimentali e modelli.
- **Il costo variabile reale di una domanda al copilota** — non l'ho stimato e non lo invento: dipende dal
  modello scelto, dalla lunghezza del catalogo delle metriche passato in contesto e dal numero di tentativi.
  È il numero che manca per validare il listino del §5, e serve una misura su un prototipo, non una stima.
- **Il prezzo di Power BI e la sua funzione Copilot** — non rilevato; entra in questa scheda solo come
  riferimento funzionale sull'esportazione.
- **Quanto un titolare di micro-impresa sia disposto a pagare per un cruscotto *separato*** dalle app che già
  paga — non ho trovato dati. È la domanda di prodotto più importante e insieme la più incerta (§11, punto 1):
  il catalogo stesso avverte che InsightGrove «non è un entry point».

---

## 3. Varco d'identità — le risposte pronte per `new-application`

> Queste sei righe sono ciò che la skill `new-application` chiede **prima** di generare qualunque cosa.
> L'identificativo dell'app finisce nel nome dello schema del database, nei nomi delle code, nella rotta pubblica
> e nell'istanza del modulo di infrastruttura: cambiarlo dopo **non è una rinomina, è una migrazione di dati**.

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `insights` | Rispetta `^[a-z][a-z0-9_]{0,30}$`: otto caratteri, minuscolo, solo lettere. Descrive **cosa l'app è** — il luogo dove stanno gli indicatori — e non come è commercializzata. Scartato `insightgrove` (è il nome commerciale, e i nomi commerciali cambiano); scartato `analytics` (troppo generico: prima o poi la piattaforma avrà una propria analisi interna e i due nomi collidono); scartato `kpi` (sigla, e il repository vieta le sigle). Lo schema sarà `app_insights`, le rotte `/api/insights/v1/*`. |
| **Modello utente** | `multi` | Non è una scelta di comodo: è **imposta dal problema numero due** di questa app. «Il fatturato non lo vedono tutti» richiede che l'account abbia più persone con ruoli diversi; un'app a utente singolo non ha nemmeno il concetto di «chi sta guardando». Inoltre l'app è il collante della suite, e la suite è multi-persona. La conseguenza pratica sta al §6.2 e alla storia 0014. |
| **Porta locale** | `8120` | Convenzione del kit: 8100 + numero di catalogo (20). Da confermare con `./dev.sh services` al momento dello scaffolding. |
| **Metrica di quota** | `questions` (**domande al copilota**) | È la **sola** cosa che il piano limita. Il resto — cruscotti, riquadri, indicatori, fonti collegate, avvisi — è **illimitato in tutti i piani**, ed è una scelta deliberata: far pagare le fonti collegate (come fa Databox, §2.1) penalizzerebbe esattamente il comportamento che la suite vuole incoraggiare, cioè attivare più app. La domanda al copilota è invece l'unica cosa che (a) cresce col valore percepito — chi la usa la usa tutti i giorni — e (b) ha un **costo variabile vero** per ogni esecuzione. Contano come una domanda anche il riepilogo scritto di un rapporto periodico e la spiegazione di uno scostamento, perché consumano lo stesso motore (storia 0026). |
| **Natura della metrica** | `flow` | Consumo su una finestra che si azzera: «300 domande al mese» significa che a settembre se ne possono fare altre 300 comunque sia andato agosto. Non è una giacenza: una domanda posta non «occupa» niente dopo essere stata risposta, e nessuno deve cancellare vecchie domande per poterne fare di nuove. Contarla come giacenza sarebbe l'errore costoso descritto dal kit: bloccherebbe l'utente per sempre al raggiungimento del tetto. |
| **Colore-categoria e icona** | `blue` · icona `gauge` (quadrante con lancetta) | Deve coincidere fra listino (`category`) e modulo frontend (`accentToken`). Il ragionamento è particolare per questa app: **`amber`, `red` e `green` vanno scartati a priori**, perché in un'app di indicatori quei tre colori devono restare liberi di significare *sotto soglia*, *in allarme* e *in salute* **dentro i numeri**. Un cruscotto il cui accento è ambra non può più usare l'ambra per dire «attenzione». Restano `blue`, `violet` e `teal`: `violet` è già proposto da tre app scritte (06, 13, 16), `teal` da due (02, 12), `blue` da una sola (04 LeadGrove, più il mini-CRM reale che è però `status: inactive`). `blue` è anche il colore convenzionale della misura. La collisione con 04 resta e non è risolvibile — sei colori per sessanta app — ed è un punto aperto di piattaforma (§11, punto 7). |

---

## 4. Modello di dominio

### 4.1 Entità principali

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Fonte` | un'app appgrove collegata **per questo account** come sorgente di numeri | identificativo dell'app d'origine, stato (collegata, sospesa, scollegata), momento del collegamento, chi l'ha collegata, momento dell'ultimo fatto ricevuto, ritardo atteso | indirettamente: chi l'ha collegata |
| `Fatto` | **il mattone**: un numero misurato, già intestato al proprio account dall'app che l'ha prodotto | app d'origine, chiave della misura, periodo di competenza, dimensioni (coppie chiave-valore), valore, unità, momento dell'evento, chiave di idempotenza, riferimento alla riga d'origine | no — **per contratto**: il fatto non trasporta testo libero né campi anagrafici (storia 0006) |
| `EtichettaDimensione` | il nome leggibile di un valore di dimensione («cliente `c-8842`» → «Panificio Aurora») | app d'origine, chiave di dimensione, valore opaco, etichetta, momento dell'ultimo aggiornamento | **possibile sì** — è il punto delicato del §6.1 |
| `DefinizioneMetrica` | che cosa vuol dire un indicatore, scritto una volta sola | chiave stabile, versione, titolo nelle 5 lingue, formula (aggregazione su una chiave di misura, oppure espressione fra metriche), dimensioni ammesse, unità, classe di riservatezza, fonti richieste, stato (bozza, pubblicata, ritirata) | no |
| `ValoreMetrica` | il valore calcolato di una metrica per un periodo e una combinazione di dimensioni | metrica e sua versione, periodo, dimensioni, valore, numero di fatti che vi concorrono, completezza, momento del calcolo | no |
| `Cruscotto` | una pagina di riquadri | titolo, ordine, chi lo vede (classe di riservatezza risultante), predefinito sì/no | no |
| `Riquadro` | un indicatore messo su un cruscotto in una certa forma | metrica, periodo, confronto, forma (numero, andamento, ripartizione), dimensione di scomposizione, posizione | no |
| `Avviso` | una soglia che, se superata, fa suonare qualcosa | metrica, periodo di valutazione, condizione, valore di soglia, canale di recapito, destinatari, stato (attivo, sospeso) | **sì** — i destinatari sono indirizzi di posta elettronica |
| `ScattoAvviso` | il fatto che un avviso è scattato | avviso, momento, valore rilevato, valore di soglia, esito del recapito | no |
| `RapportoProgrammato` | un riepilogo che parte da solo | cruscotto, cadenza, destinatari, formato, prossima esecuzione, stato | **sì** — destinatari |
| `EsecuzioneRapporto` | una spedizione avvenuta | rapporto, momento, periodo coperto, esito, riferimento al file prodotto | no |
| `Domanda` | una domanda posta al copilota | testo posto dall'utente, momento, autore, piano prodotto, esito (risposta, rifiuto, non lo so), riferimento alla traccia | **sì** — il testo è scritto da una persona e può nominarne altre (§6.1) |
| `PianoDiInterrogazione` | la traduzione strutturata di una domanda: metrica, periodo, dimensioni, filtri | metrica e versione, periodo, dimensioni, filtri, ordinamento, limite | no |
| `TracciaDelCalcolo` | **la ricevuta del numero**: come è stato ottenuto | piano eseguito, metriche e versioni usate, fonti che hanno concorso, numero di fatti, intervallo dei momenti degli eventi, completezza, momento dell'esecuzione | no |
| `Previsione` | una proiezione dichiarata come tale | metrica, periodo proiettato, metodo, valore centrale, intervallo, dati su cui è costruita | no |

### 4.2 Come arrivano i dati — e perché l'isolamento fra account regge

Questa è la decisione tecnica che definisce l'applicazione. Il rischio, dichiarato apertamente, è che un'app che
«legge i dati di tutte le altre» diventi **la scorciatoia che aggira l'isolamento fra account**: basta un difetto
in un filtro e un cliente vede i numeri di un altro. La risposta non è «stare attenti»: è togliersi la
possibilità.

**Cosa si è scartato, e perché.**

| Via | Perché è stata scartata |
|---|---|
| **Lettura diretta degli schemi delle altre app** (`app_billgrove`, `app_cashgrove`, …) | È **vietata dalla piattaforma**, e per fortuna: «vietate le chiavi esterne e le interrogazioni fra schemi diversi», e ogni servizio ha **un ruolo del database con privilegi solo sul proprio schema** ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8). Non è una convenzione che si può dimenticare: il ruolo del database di `insights` non ha proprio il permesso. Se questa via fosse aperta, l'app diventerebbe un secondo percorso di lettura su ogni dato della piattaforma, con un secondo filtro per account da scrivere bene — cioè un secondo posto dove sbagliare. |
| **Interfacce di sola lettura per account** (`insights` chiama `GET /api/billgrove/v1/...` con un permesso speciale) | È **vietata dalla piattaforma**: «un'app **non chiama** un'altra app; l'unica via fra servizi è asincrona a eventi» ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §2). Ed è giusto che lo sia: perché funzionasse servirebbe un gettone d'accesso valido su un'altra app, cioè esattamente una **credenziale che scavalca l'abilitazione e il ruolo**. È la definizione della scorciatoia da evitare. In più renderebbe il cruscotto lento e fragile: una fonte spenta spegnerebbe la pagina. |
| **Una copia periodica delle tabelle altrui** (estrazione notturna) | Ha gli stessi problemi della prima via, con in più la duplicazione integrale del dato personale delle altre app dentro `app_insights`: raddoppia la superficie di esportazione e cancellazione, contro il principio di minimizzazione (§2.3). |

**La via scelta: fatti pubblicati sul bus, magazzino proprio.**

```
   BillGrove              CashGrove              StockGrove
  (app_billgrove)        (app_cashgrove)        (app_stockgrove)
        │                       │                      │
        │  pubblica un FATTO    │                      │
        │  {tenant_id, app_id, misura, periodo,        │
        │   dimensioni, valore, unità, rif_origine}    │
        ▼                       ▼                      ▼
  ╔═══════════════════════════════════════════════════════════╗
  ║   bus di eventi della piattaforma  (EventBridge → SQS)     ║
  ╚═══════════════════════════════════════════════════════════╝
                              │  una coda dedicata a `insights`
                              ▼
  ┌───────────────────────────────────────────────────────────┐
  │  servizio `insights` — schema PROPRIO `app_insights`       │
  │  tabella `fatto` con la colonna `tenant_id`,               │
  │  scritta con il tenant_id CHE VIAGGIA NELL'EVENTO          │
  └───────────────────────────────┬───────────────────────────┘
                                  │  lettura: WHERE tenant_id = :tid
                                  │  con :tid PRESO DAL GETTONE VERIFICATO
                                  ▼
                        cruscotti · copilota · rapporti
```

**Perché l'invariante regge — quattro proprietà, non una promessa.**

1. **Sul percorso di lettura, `insights` non è speciale.** Legge **il proprio schema**, con `WHERE tenant_id = :tid`
   e `:tid` preso dal gettone verificato, esattamente come ogni altra app del catalogo. Non esiste una seconda
   forma di lettura da coprire con i collaudi: la suite di isolamento fra due account vale qui parola per parola
   ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §1).
2. **Sul percorso di scrittura, l'account non è mai scelto da `insights`.** Il `tenant_id` con cui il fatto viene
   scritto è **quello che l'app d'origine ha messo nell'evento**, e l'app d'origine lo aveva a sua volta preso dal
   proprio gettone verificato. `insights` non lo deduce, non lo cerca, non lo accetta da una richiesta: lo copia.
   Un fatto senza `tenant_id`, o con un `tenant_id` sconosciuto alla piattaforma, viene **scartato** (storia 0007).
3. **Il fatto è già un aggregato, non una riga.** Non contiene il nome del cliente, non contiene il corpo della
   fattura, non contiene note libere: contiene una misura, un periodo, dimensioni con **identificativi opachi** e
   un riferimento all'origine. Anche nel caso peggiore — un difetto che mescolasse due account — ciò che
   trapelerebbe sarebbe un numero, non un archivio. È **contenimento del danno per costruzione**, ed è il motivo
   per cui il contratto del fatto (storia 0006) è la storia più importante dell'app.
4. **Ogni fonte è attivata dall'account, una per una.** Un fatto che arrivi per un account che non ha collegato
   quella fonte viene scartato con una riga di registro senza contenuto (storia 0008). Il collegamento è un atto
   volontario di chi ha il ruolo per farlo, revocabile, e la revoca cancella i fatti già ricevuti da quella fonte.

**E il dettaglio?** Un aggregato non si può ispezionare: se il titolare vede «crediti scaduti 12.400 €» e vuole
sapere *di chi*, la somma non glielo dice. La soluzione **non** è arricchire il fatto fino a farlo diventare una
copia della riga: è il **rimando** (storia 0011). Ogni fatto porta un riferimento d'origine opaco (app, tipo di
entità, identificativo) e InsightGrove lo trasforma in un collegamento che apre **l'app sorgente sulla sua
schermata**, dove valgono il suo filtro per account, la sua abilitazione e il suo ruolo. Il dettaglio si vede là,
con i controlli di là. InsightGrove non lo recupera, non lo conserva e non lo mostra.

### 4.3 Come si risale dal numero alla sua fonte

Un cruscotto che sbaglia in silenzio è peggio di nessun cruscotto. Il presidio non è un controllo in più: è la
forma stessa del calcolo. Tre regole, che le storie dell'epica 03 e 05 devono rispettare tutte e tre.

**Regola 1 — il numero non lo produce mai il modello linguistico.** Il copilota traduce la domanda in un
**piano di interrogazione strutturato** (quale metrica, quale periodo, quali dimensioni, quali filtri) e il piano
viene **validato contro il catalogo delle metriche pubblicate**: se nomina una metrica che non esiste, una
dimensione non ammessa o un periodo non rappresentabile, il piano viene **rifiutato** e l'utente riceve un «non lo
so», non un numero. Il calcolo lo esegue codice deterministico. Il modello sceglie *cosa chiedere*; non calcola,
non stima, non arrotonda. È il rimedio diretto al 39,1 % del §2.5: il modello non scrive interrogazioni libere,
sceglie dentro un insieme chiuso.

**Regola 2 — ogni numero porta la sua ricevuta.** Ogni valore mostrato — su un cruscotto, in una risposta del
copilota, dentro un rapporto — è accompagnato da una **scheda del numero** che dice:

- **quale metrica** e **quale versione della sua definizione** (perché una definizione cambiata cambia il numero);
- **quale periodo** e con quale calendario;
- **quali fonti** hanno concorso, e **quanti fatti** per ciascuna;
- **a quando è aggiornato**: il momento dell'evento più recente, per fonte;
- **quanto è completo**: se una fonte richiesta dalla metrica è scollegata, silente oltre il proprio ritardo
  atteso, o ha un buco nel periodo, il numero è marcato **incompleto** e la scheda dice quale pezzo manca;
- **il piano eseguito**, in forma leggibile, così che due persone possano confrontarlo;
- **i rimandi**: fino a dieci fatti che vi concorrono, ciascuno con il collegamento alla riga d'origine nell'app
  che l'ha prodotto.

**Regola 3 — l'incompletezza si vede prima di leggere il numero, non dopo.** Un valore incompleto non si mostra
come un valore normale con una nota a fondo pagina: porta un contrassegno accanto alla cifra e il copilota lo dice
nella **prima frase** della risposta. La regola vale anche verso il basso: se il ruolo di chi chiede non gli
consente di vedere una delle metriche che concorrono al calcolo, la risposta **non è un numero più piccolo** — è
un rifiuto motivato. Un aggregato filtrato è un numero sbagliato, non un numero parziale (storia 0025).

### 4.4 Chi può vedere cosa — e cosa la piattaforma oggi non sa fare

Il fatturato non lo vedono tutti. È vero, è un requisito, e va detto subito **che cosa la piattaforma sa fare
oggi e che cosa no**.

**Quello che c'è.** La catena dei varchi prevede un controllo di ruolo con tre valori — `owner`, `admin`,
`member` — validi per l'intero account ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §6). È un modello
**grossolano**: dice *che tipo di persona* è, non *a quali dati può arrivare*.

**Quello che questa app costruisce sopra, senza inventare un modello nuovo.** Ogni `DefinizioneMetrica` porta una
**classe di riservatezza** con due soli valori:

- `operativa` — quantità, tempi, volumi, conteggi: visibile a tutti i ruoli;
- `economica` — importi, margini, crediti, valore: visibile a `owner` e `admin`.

La classe è un attributo **della metrica**, non della persona: si decide una volta, per indicatore, e vale
ovunque quell'indicatore compaia — cruscotto, risposta del copilota, rapporto periodico, esportazione, strumento
conversazionale. Le metriche predefinite nascono già classificate; il titolare può alzare la riservatezza di una
metrica operativa, non abbassare quella di una economica senza una conferma esplicita (storia 0014).

**Quello che manca, e che questa app non inventa.** Un cliente che chieda «Anna vede i numeri della sede di
Torino e Luca quelli di Milano», oppure «il commerciale vede il fatturato ma non i margini», chiede un permesso
**per risorsa e per valore di dimensione**. La piattaforma non ce l'ha, e costruirlo dentro una singola app
sarebbe il peggiore degli esiti: un secondo modello di autorizzazione, diverso da quello di tutte le altre app,
che nessuno terrebbe allineato. **È un punto aperto di piattaforma** (§11, punto 3), non una scelta di questa
scheda. Nel frattempo la risposta onesta al cliente è: due classi, tre ruoli, e la trasparenza su cosa non si può
fare.

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica
(`deleted_at`) — con l'eccezione motivata della tabella `fatto`, che è **in sola aggiunta**: un fatto non si
modifica, si corregge pubblicandone un altro con la stessa chiave di idempotenza e un valore nuovo (storia 0007).
Schema `app_insights`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8).

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata
> della prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di
> scrivere il file `services/core/src/main/resources/pricing/insights.yaml`.

**Ragionamento.** Tre vincoli si incrociano.

1. **Il mercato rilevato è più caro della fascia di catalogo** (§2.2: ingresso 25-64 $, media 87-159 $, contro i
   19-49 € della scheda). Ma i concorrenti vendono **il collegamento delle fonti**, che qui non esiste: il costo
   che loro fanno pagare noi non lo abbiamo. Stare nella fascia bassa del catalogo è quindi coerente, non
   svendita.
2. **Non è un'app d'ingresso.** Il catalogo lo dice: InsightGrove è «il collante e l'upsell premium della suite»,
   e va costruita dopo che 3-4 app popolano il dato. Chi la compra **paga già** altre app appgrove. Un prezzo alto
   su un cliente che ha già tre abbonamenti attivi è la via più rapida per fargli guardare il totale della
   fattura.
3. **L'unica cosa che costa davvero è la domanda al copilota.** Cruscotti, riquadri, avvisi e rapporti sono
   calcoli su dati già in casa: costano quasi zero. La domanda al copilota costa una chiamata a un modello — e
   quanto, **non lo so** (§2.7): è il numero che manca per validare questa proposta.

Da qui la forma: **il cruscotto è generoso, il copilota è la leva**. È anche il rimedio alla lamentela rilevata
sul concorrente più vicino — il salto da 0 a 159 $/mese senza niente in mezzo (§2.5).

| Piano | Prezzo mensile | Prezzo annuale | Limite sulla metrica `questions` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | **20 domande al mese** | — | Chi ha attivato la seconda app appgrove: cruscotti, indicatori, avvisi e rapporti sono **tutti compresi e illimitati**; il copilota si assaggia. Abbastanza per capire che serve, non abbastanza per usarlo tutti i giorni |
| `pro` | 19 € | 190 € (= 10× il mensile, «due mesi in regalo») | **300 domande al mese** | 14 giorni | Il titolare che guarda i numeri ogni mattina e chiede al copilota invece di cercare il riquadro giusto: ~10 domande al giorno |
| `business` | 39 € | 390 € | **1.500 domande al mese** | 14 giorni | L'azienda con più persone che interrogano i dati, e chi usa gli strumenti conversazionali da una chat esterna, dove il consumo è per sua natura più alto |

**Note obbligate.**

- **Tre piani** — il minimo indispensabile per avere un gratuito che tenga il cliente dentro la suite e due
  gradini a pagamento. Aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile.
- Un limite **lasciato vuoto significa illimitato, non zero**: qui nessun limite è vuoto, il tetto c'è sempre.
- **La prova gratuita su un'app che ha già un piano gratuito è ridondante?** Qui **no**, ed è il caso in cui non
  lo è: il piano gratuito non è una versione ridotta del prodotto, è **lo stesso prodotto con il copilota quasi
  spento**. La prova di 14 giorni serve a far provare la cosa che si paga. Coerente con il mercato, che offre
  14-15 giorni ovunque (§2.2).
- **Costo effettivo dell'incasso**: nessun piano proposto sta sotto i ~5 €/mese, quindi la parte fissa per
  transazione non è un problema. Il piano annuale resta la via da spingere.
- **Prezzi immutabili una volta vivi**: un cambio si fa creando un prezzo nuovo e archiviando il vecchio.
- ⚠️ **Il numero che manca.** Se una domanda costasse, poniamo, dieci volte più di quanto ipotizzato, il piano
  `pro` a 19 € con 300 domande andrebbe in perdita. **Prima di pubblicare questo listino va misurato il costo
  medio di una domanda su un prototipo** e vanno rifatti i conti. Non ho fatto quella misura e non ho stimato
  quel costo (§2.7).
- ⚠️ **Una domanda di prodotto, non di prezzo.** Rendere illimitati i cruscotti anche nel piano gratuito è una
  scelta di **direzione di prodotto**: dice che InsightGrove serve alla suite prima che a sé stessa. È difendibile
  (§11, punto 1) ma non spetta a un agente deciderla. Se la risposta fosse l'opposto — InsightGrove deve stare in
  piedi da sola — il listino andrebbe rifatto da capo, probabilmente con la metrica sulle fonti collegate.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/insights.yaml`) si compila **insieme** allo sviluppatore: «niente contratto, niente
> produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

### 6.1 Il punto di partenza è controintuitivo: quasi tutto qui non è un dato personale — ma non tutto

L'istinto dice che l'app che legge i dati di tutte le altre sia la più esposta della suite. È vero il contrario,
**se il contratto del fatto tiene**: un fatto è un numero con un periodo e delle dimensioni opache. Non c'è il
nome del cliente, non c'è la fattura, non c'è la nota libera. Le altre app trattano persone; questa tratta somme.

Restano però **quattro punti in cui il dato personale entra davvero**, e sono tutti facili da non vedere.

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `etichetta_dimensione.etichetta` | `app_insights.etichetta_dimensione`, colonna `etichetta` | clienti, fornitori o contatti **dell'account**, così come nominati dall'app d'origine | anagrafico (una ragione sociale, che per una ditta individuale **è il nome di una persona**) | rendere leggibile la scomposizione: «i primi cinque clienti per fatturato» deve mostrare nomi, non codici | esecuzione del contratto col cliente dell'account (è il servizio richiesto) | finché la fonte resta collegata; alla revoca della fonte, cancellazione fisica entro 30 giorni |
| `domanda.testo` | `app_insights.domanda`, colonna `testo` | **l'utente che scrive**, e chiunque venga nominato dentro la domanda | testo libero scritto da una persona, riferibile al suo autore | ripetere una domanda, riprodurre un calcolo, capire una risposta contestata | legittimo interesse (riproducibilità e assistenza) — da valutare con lo sviluppatore | 12 mesi proposti, poi cancellazione fisica |
| `avviso.destinatari` e `rapporto_programmato.destinatari` | tabelle omonime, colonna `destinatari` | persone dell'account, ed eventualmente il commercialista esterno | contatto (indirizzo di posta elettronica) | recapitare l'avviso e il rapporto | esecuzione del contratto | finché l'avviso o il rapporto esistono; cancellazione fisica alla loro eliminazione |
| `created_by` / `updated_by` su tutte le tabelle | ovunque | utenti dell'account | identificativo interno di un utente | tracciare chi ha fatto cosa | esecuzione del contratto | come il resto della riga |

**La voce numero uno è la decisione da prendere, e non la prendo io.** L'etichetta di dimensione è ciò che
trasforma un elenco di codici in un'informazione utile — «Panificio Aurora, 8.200 €» invece di «`c-8842`,
8.200 €» — e insieme è ciò che porta dentro `app_insights` il nome di clienti che stanno in un'altra app.
Le due vie possibili:

- **(A) con etichetta** — l'app d'origine pubblica anche l'etichetta leggibile. L'app diventa trattante di dati
  personali (in misura limitata: solo il nome, nessun contatto, nessun indirizzo) e li dichiara nel manifesto,
  nell'esportazione e nella cancellazione. **È la via raccomandata**, perché senza etichette la scomposizione per
  cliente — che è metà del valore — diventa illeggibile.
- **(B) senza etichetta** — nel fatto viaggia solo l'identificativo opaco, e il nome lo mostra soltanto l'app
  d'origine quando si segue il rimando. `app_insights` non tratterebbe **alcun** dato personale oltre agli utenti
  dell'account. Più pulito, molto meno utile.

La differenza fra (A) e (B) non è un dettaglio implementativo: cambia la classificazione dell'app. **Va decisa
dallo sviluppatore prima della storia 0006**, ed è tracciata come punto aperto (§11, punto 2).

### 6.2 Categorie particolari (articolo 9): **no**, con una condizione da presidiare

Questa app **non tratta** dati sanitari, biometrici, genetici, opinioni politiche, convinzioni religiose,
orientamento sessuale o appartenenza sindacale, e non ha alcuna ragione per farlo. Non serve una base giuridica
rafforzata né una valutazione d'impatto per questo motivo.

**Ma la condizione va presidiata, perché il dato non lo produce questa app.** Se un giorno una fonte pubblicasse
un'etichetta di dimensione come «servizio = fisioterapia» o «categoria di spesa = contributi sindacali», il dato
particolare entrerebbe da una porta che questa app non controlla. Tre osservazioni oneste:

- il rischio è **oggi basso**, perché le applicazioni del catalogo che tratterebbero naturalmente quei dati sono
  **escluse** dal catalogo attivo — 23 CareGrove (dati sanitari), 58 VetGrove, 27 FitGrove (certificato medico
  sportivo), 09 PeopleGrove e 11 ShiftGrove (rapporto di lavoro) ([_escluse/README.md](../_escluse/README.md));
- il presidio possibile è **contrattuale, non tecnico**: il contratto del fatto (storia 0006) vieta esplicitamente
  di pubblicare come dimensione un attributo riconducibile a una categoria particolare, e la lista delle chiavi di
  dimensione ammesse per fonte è **dichiarata e chiusa**, non libera. Una chiave non dichiarata viene scartata;
- non esiste un rilevamento automatico del contenuto di un'etichetta, e **non lo invento**: sarebbe un presidio
  finto. Se una fonte futura avesse bisogno di dimensioni sensibili, la decisione è sua e va rivalutata allora
  (§11, punto 6).

### 6.3 Esportazione e cancellazione

Le tabelle di `app_insights` che contengono dati riferibili a persone e che devono comparire **sia** in
`exportData` **sia** in `purgeData` del contratto `InsightsDataContract`:

`etichetta_dimensione` (se si sceglie la via A), `domanda`, `piano_di_interrogazione` (contiene il testo della
domanda in forma strutturata), `avviso`, `rapporto_programmato`, e — per i soli `created_by` / `updated_by` —
`fonte`, `definizione_metrica`, `cruscotto`, `riquadro`, `previsione`.
La tabella `fatto` **non** contiene dati personali per contratto, ma va comunque nella cancellazione per account:
è dato dell'account. Il contratto completo — elenco delle tabelle, prova di completezza fra esportazione e
cancellazione, conservazioni — è la storia 0035.

Due precisazioni:

- **la cancellazione è fisica.** Sostituire le etichette con dei codici non è cancellare; una domanda «anonimizzata»
  che conserva il testo resta un dato personale, perché il testo può nominare qualcuno;
- **la revoca di una fonte è una cancellazione parziale che l'app deve saper fare da sola**: scollegare BillGrove
  cancella i fatti e le etichette provenienti da BillGrove, e le metriche che dipendevano solo da quella fonte
  smettono di produrre valori — non producono valori sbagliati (storia 0008).

### 6.4 Testo libero

C'è, ed è uno solo: **il testo della domanda al copilota**. È l'ingresso non presidiato di questa app —
«quanto mi deve ancora Mario Rossi?» è una domanda naturale e contiene un nome. Tre conseguenze:

1. il testo della domanda è dato personale e va nel manifesto (tabella al §6.1);
2. **non deve finire nei registri applicativi**: si registra l'identificativo della domanda, non il suo testo
   ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §14);
3. **non deve essere visibile alla console di amministrazione**: chi amministra la piattaforma non legge le
   domande dei clienti ([estensioni-admin.md](estensioni-admin.md) §6).

L'app non fa rilevazione di contenuto sul testo delle domande; il presidio, se servirà, è un tema trasversale.

### 6.5 Integrazioni esterne

**Nessuna.** Questa proposta non introduce alcun fornitore esterno che tratti dati per nostro conto: le fonti sono
interne alla piattaforma, l'esportazione produce un file che resta all'utente, e l'invio del rapporto periodico
usa il servizio di posta **già in uso dalla piattaforma** — che è già nell'elenco dei fornitori, non uno nuovo.
È un fatto raro nel catalogo e va usato come argomento: l'app che vede più numeri di tutte è anche quella che non
li manda a nessuno.

### 6.6 Classificazione della change

Una app nuova introduce di norma finalità nuove e categorie nuove: è un cambiamento **sostanziale**, e qui si
conferma — non tanto per la quantità di dati personali (poca) quanto per la **finalità nuova**: aggregare
trasversalmente i dati che l'account ha affidato ad applicazioni diverse. Va scritto nell'informativa in modo
comprensibile, perché è precisamente ciò che un cliente vuole sapere prima di attivarla.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

**Nota importante, perché questa app è un caso particolare.** InsightGrove ha un copilota **dentro di sé**
(epica 05) e insieme espone strumenti al livello conversazionale **di piattaforma** (epica 07). Non sono due
motori: sono due clienti dello **stesso** contratto. Il copilota interno è il primo consumatore degli strumenti
di lettura qui sotto; l'assistente esterno è il secondo. Se le due vie divergessero, esisterebbero due modi di
ottenere lo stesso numero — cioè esattamente il difetto che questa app esiste per non avere.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `elenca_metriche` | `(classe?, fonte?) → catalogo delle metriche pubblicate, con versione, unità, dimensioni ammesse e classe di riservatezza` | fa sapere *cosa si può chiedere*: è il primo strumento che un assistente deve chiamare | lettura | no |
| `interroga_metrica` | `(metrica, periodo, dimensioni?, filtri?, confronto?) → valore, unità, completezza, riferimento alla traccia` | esegue **un piano validato** contro il catalogo; se il piano non è validabile restituisce un rifiuto motivato, mai un numero | lettura | no |
| `spiega_numero` | `(riferimento alla traccia) → metrica e versione, periodo, fonti, conteggio dei fatti, momento dell'ultimo dato, elenco dei rimandi all'origine` | è **la ricevuta**: rende verificabile qualunque numero già ottenuto | lettura | no |
| `stato_delle_fonti` | `() → per ogni fonte collegata: stato, momento dell'ultimo fatto, ritardo, metriche che ne dipendono` | risponde a «perché questo numero è fermo a martedì?» | lettura | no |
| `spiega_scostamento` | `(metrica, periodo A, periodo B) → differenza, scomposizione per dimensione ordinata per contributo` | scompone una variazione; **non interpreta**: dice dove la differenza si è formata | lettura | no |
| `crea_avviso` | `(metrica, condizione, soglia, destinatari) → bozza di avviso` | prepara un avviso che poi manderà messaggi a persone: effetto verso l'esterno | scrittura | **sì** |
| `programma_rapporto` | `(cruscotto, cadenza, destinatari, formato) → bozza di rapporto programmato` | prepara un invio ricorrente verso indirizzi di posta elettronica | scrittura | **sì** |
| `pubblica_metrica` | `(definizione) → bozza di definizione di metrica` | cambia **il significato di un numero** per tutti quelli che lo guardano, retroattivamente: è l'azione più pericolosa dell'app | scrittura | **sì, obbligatoria** |
| `collega_fonte` / `scollega_fonte` | `(app di origine) → bozza di collegamento o di revoca` | la revoca **cancella fisicamente** i fatti già ricevuti: è irreversibile | scrittura irreversibile | **sì, obbligatoria** |

**Riga di lettura.** Il valore conversazionale di questa app non sta in `interroga_metrica`: sta nella coppia
`interroga_metrica` + `spiega_numero`. Un assistente che risponde «il fatturato di luglio è 42.300 €» vale poco;
un assistente che risponde «42.300 €, da 118 fatti di BillGrove e CashGrove, aggiornato alle 06:15 di stamattina,
completo, definizione `fatturato_emesso` versione 3 — ecco le prime dieci fatture» è un'altra cosa. E il terzo
strumento che conta è quello che manca dagli elenchi dei concorrenti: `stato_delle_fonti`, cioè la capacità di
dire **perché** un numero non è quello che ci si aspetta.

**Nota di sicurezza specifica.** `pubblica_metrica` è marcata a conferma obbligatoria per una ragione che vale la
pena scrivere: cambiare una definizione **cambia il passato**. Un cruscotto che ieri diceva 42.300 € oggi dice
39.100 € perché qualcuno ha ridefinito «fatturato». È irreversibile nel senso che conta — la fiducia — anche se
tecnicamente la versione precedente resta. Per questo la definizione è **versionata** e la scheda del numero cita
sempre la versione usata (storia 0012).

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine dell'epica l'app esiste, si avvia in locale, si vede nella barra laterale, ha il suo schema vuoto e
sa dire «non ho ancora nessuna fonte collegata».

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio `insights` | Il servizio nasce dallo scaffolding, risponde su `/api/insights/v1/`, ha la sua istanza di infrastruttura |
| [0002](01-fondamenta/0002-magazzino-dei-fatti-e-modello-dati.md) | Magazzino dei fatti e modello dati multi-account | Lo schema `app_insights` con la tabella `fatto` in sola aggiunta e il filtro per account su tutto |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Il modulo `insights` nel registro, sezioni, cinque lingue, tema chiaro e scuro |
| [0004](01-fondamenta/0004-abbonamento-e-quota-sulle-domande.md) | Abbonamento e quota sulle domande | Piani, metrica `questions` di natura `flow`, varco a `429` e messaggio che dice come rimediare |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | `./dev.sh services` la mostra; un generatore produce fatti inventati di due account diversi |

### Epica 02 — Arrivo dei dati dalle altre app

È il cuore tecnico: come i numeri entrano senza che l'isolamento fra account si indebolisca.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-arrivo-dei-dati-dalle-altre-app/0006-contratto-del-fatto-di-misura.md) | Contratto del fatto di misura | Lo schema dell'evento che ogni app sorgente pubblica: che cosa può contenere e che cosa non può |
| [0007](02-arrivo-dei-dati-dalle-altre-app/0007-ricezione-e-scrittura-dei-fatti.md) | Ricezione e scrittura dei fatti | Consumo dalla coda, idempotenza, scarto dei fatti malformati o di account sconosciuti |
| [0008](02-arrivo-dei-dati-dalle-altre-app/0008-collegamento-di-una-fonte.md) | Collegamento e revoca di una fonte | L'account attiva una fonte per volta; la revoca cancella fisicamente ciò che ne è arrivato |
| [0009](02-arrivo-dei-dati-dalle-altre-app/0009-ripopolamento-dello-storico.md) | Ripopolamento dello storico | Al collegamento si chiede alla fonte di ripubblicare il passato, e si sa quando è finito |
| [0010](02-arrivo-dei-dati-dalle-altre-app/0010-salute-e-ritardo-delle-fonti.md) | Salute e ritardo delle fonti | Ogni fonte dichiara ogni quanto parla; se tace oltre l'atteso si vede, prima che il numero menta |
| [0011](02-arrivo-dei-dati-dalle-altre-app/0011-rimando-alla-riga-di-origine.md) | Rimando alla riga d'origine | Dal fatto al collegamento che apre l'app sorgente, senza che InsightGrove legga niente di suo |

### Epica 03 — Catalogo delle metriche e tracciabilità del numero

Definire una volta sola che cosa significa un indicatore, e rendere ogni valore risalibile alla sua fonte.

| # | Storia | In una riga |
|---|---|---|
| [0012](03-catalogo-delle-metriche-e-tracciabilita/0012-catalogo-delle-metriche-pubblicate.md) | Catalogo delle metriche pubblicate | Definizioni versionate, in bozza finché non si pubblicano; nessun calcolo fuori dal catalogo |
| [0013](03-catalogo-delle-metriche-e-tracciabilita/0013-metriche-derivate-da-formula.md) | Metriche derivate da formula | Giorni medi di incasso, margine, ciclo del contante: indicatori costruiti su altri indicatori |
| [0014](03-catalogo-delle-metriche-e-tracciabilita/0014-classe-di-riservatezza-e-ruoli.md) | Classe di riservatezza e ruoli | Metriche `operative` per tutti, `economiche` per chi guida: due classi sopra i tre ruoli che ci sono |
| [0015](03-catalogo-delle-metriche-e-tracciabilita/0015-periodi-e-confronti.md) | Periodi, calendario e confronti | Mese, trimestre, anno, ultimi N giorni; confronto col periodo precedente e con l'anno prima |
| [0016](03-catalogo-delle-metriche-e-tracciabilita/0016-scheda-del-numero.md) | Scheda del numero | La ricevuta di ogni valore: definizione, versione, fonti, conteggio, completezza, rimandi |

### Epica 04 — Cruscotti e avvisi

Ciò che il titolare guarda la mattina, e ciò che lo chiama quando non sta guardando.

| # | Storia | In una riga |
|---|---|---|
| [0017](04-cruscotti-e-avvisi/0017-cruscotto-e-riquadri.md) | Cruscotto e riquadri | Una pagina di indicatori, ognuno con periodo, confronto e forma |
| [0018](04-cruscotti-e-avvisi/0018-cruscotto-iniziale-suggerito.md) | Cruscotto iniziale suggerito | Al primo accesso il cruscotto è già pieno, costruito sulle fonti collegate: non si parte da un foglio bianco |
| [0019](04-cruscotti-e-avvisi/0019-definizione-di-un-avviso.md) | Definizione di un avviso su soglia | «Avvisami se i crediti scaduti superano X»: condizione, soglia, destinatari |
| [0020](04-cruscotti-e-avvisi/0020-valutazione-e-recapito-degli-avvisi.md) | Valutazione e recapito degli avvisi | La valutazione periodica, il recapito, e il rifiuto di suonare su un numero incompleto |
| [0021](04-cruscotti-e-avvisi/0021-registro-e-sospensione-degli-avvisi.md) | Registro e sospensione degli avvisi | Che cosa è scattato, quando, con che valore; e come si fa tacere un avviso senza cancellarlo |

### Epica 05 — Copilota sui dati

La chat dentro l'app: risponde, dice «non lo so», e mostra sempre da dove viene il numero.

| # | Storia | In una riga |
|---|---|---|
| [0022](05-copilota-sui-dati/0022-dalla-domanda-al-piano.md) | Dalla domanda al piano d'interrogazione | Il modello sceglie dentro il catalogo pubblicato; il piano non validabile viene rifiutato |
| [0023](05-copilota-sui-dati/0023-risposta-con-la-scheda-del-numero.md) | Risposta con la scheda del numero | Ogni risposta porta la sua ricevuta e i rimandi alla riga d'origine |
| [0024](05-copilota-sui-dati/0024-il-non-lo-so.md) | Il «non lo so» | Fuori catalogo, dato mancante, periodo incompleto: si dice, e si offre ciò che si può rispondere |
| [0025](05-copilota-sui-dati/0025-riservatezza-nel-copilota.md) | Riservatezza nel copilota | Chi non può vedere una metrica riceve un rifiuto, **non** un numero calcolato senza quel pezzo |
| [0026](05-copilota-sui-dati/0026-registro-delle-domande-e-quota.md) | Registro delle domande e consumo della quota | Ogni domanda è ripetibile e conta una unità di `questions`; a tetto raggiunto `429` |

### Epica 06 — Rapporti, esportazione e previsioni

Come i numeri escono dall'app: verso un file, verso una casella di posta, verso il futuro.

| # | Storia | In una riga |
|---|---|---|
| [0027](06-rapporti-esportazione-e-previsioni/0027-esportazione-di-una-tavola.md) | Esportazione di una tavola | Il foglio di calcolo con i numeri **e** la loro provenienza in testa: un'esportazione senza scheda è un numero orfano |
| [0028](06-rapporti-esportazione-e-previsioni/0028-rapporto-periodico-programmato.md) | Rapporto periodico programmato | Il riepilogo del lunedì mattina che arriva senza che nessuno lo chieda |
| [0029](06-rapporti-esportazione-e-previsioni/0029-spiegazione-dello-scostamento.md) | Spiegazione dello scostamento | «Perché luglio è andato peggio di giugno»: la differenza scomposta per dimensione, senza interpretazioni |
| [0030](06-rapporti-esportazione-e-previsioni/0030-previsione-con-metodo-dichiarato.md) | Previsione con metodo dichiarato | Una proiezione semplice, marcata come tale, con il metodo scritto e l'intervallo mostrato |

### Epica 07 — Esposizione conversazionale e prove end-to-end

Il contratto degli strumenti verso il livello conversazionale di piattaforma, la prova che il percorso regge e il
contratto dati che chiude l'applicazione.

| # | Storia | In una riga |
|---|---|---|
| [0031](07-esposizione-conversazionale-e-prove/0031-contratto-degli-strumenti-di-lettura.md) | Contratto degli strumenti di lettura | I cinque strumenti di lettura, con schema dei parametri e del risultato, versionati col servizio |
| [0032](07-esposizione-conversazionale-e-prove/0032-strumenti-di-scrittura-con-conferma.md) | Strumenti di scrittura con bozza e conferma | Avvisi, rapporti, definizioni e fonti: si preparano, non si eseguono |
| [0033](07-esposizione-conversazionale-e-prove/0033-ruolo-e-quota-sulle-chiamate-dell-assistente.md) | Ruolo e quota sulle chiamate dell'assistente | Gli stessi varchi delle schermate valgono per la chat: nessuna porta di servizio |
| [0034](07-esposizione-conversazionale-e-prove/0034-percorso-end-to-end-e-registro-di-copertura.md) | Percorso end-to-end e registro di copertura | Il percorso `[J-INSIGHTS]` dal collegamento della fonte alla risposta verificabile |
| [0035](07-esposizione-conversazionale-e-prove/0035-esportazione-e-cancellazione-dei-dati-personali.md) | Esportazione e cancellazione dei dati personali | Il contratto dati dell'app: che cosa esce, che cosa sparisce fisicamente, e la prova che nessuna tabella è dimenticata |

**Totale**: 7 epiche, 35 storie.

---

## 9. Estensioni della console di amministrazione

Servono estensioni, ma poche e tutte di diagnosi: chi amministra deve poter rispondere a «perché il cliente dice
che i suoi numeri sono fermi a martedì?» guardando **lo stato delle code e delle fonti**, non i numeri del
cliente. Serve inoltre una deroga temporanea sul tetto delle domande, perché il primo mese di un cliente che
ripopola lo storico e prova il copilota è atipico. Il divieto di impersonificazione qui è più stringente che
altrove: le domande poste al copilota **non** sono visibili alla console.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

**Questa app non ha materia prima propria: senza le altre, è un guscio vuoto.** È la dipendenza più forte del
catalogo e la scheda d'origine lo dice: «va costruita dopo che almeno 3-4 app popolano il dato».

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| 02 — BillGrove | **fonte primaria** | Fatturato emesso, documenti, valore medio; è la fonte senza la quale il cruscotto non ha il numero che il titolare cerca per primo |
| 03 — CashGrove | **fonte primaria** | Incassato, crediti aperti e scaduti, giorni medi di incasso; è la fonte che rende possibile il ciclo del contante (§2.5) |
| 04 — LeadGrove | **fonte** | Valore della pipeline, trattative aperte e chiuse, tasso di conversione |
| 06 — QuoteGrove | **fonte** | Preventivi emessi, accettati, tasso di accettazione; chiude la catena preventivo → fattura → incasso |
| 14 — StockGrove | **fonte** | Valore di magazzino, giorni di giacenza; il terzo pezzo del ciclo del contante |
| 08 — SpendGrove | **fonte** | Spese approvate per categoria: il lato delle uscite, senza il quale il margine non si calcola |
| 07 — BookGrove · 12 — DeskGrove Support · 13 — FlowGrove · 16 — ReachGrove | **fonti secondarie** | Prenotazioni, ticket, ore di progetto, rendimento delle campagne: indicatori operativi, `classe operativa` |
| 19 — SubGrove | **fonte** | Ricavi ricorrenti e abbandono: indicatori economici che hanno senso solo per chi vende abbonamenti |
| 54 — BudgetGrove | **si sovrappone a** | Il confronto consuntivo/piano. Vedi sotto: è la sovrapposizione più seria |
| 31 — AuditGrove | **complementare** | Il catalogo (§6) lo indica come controparte di governance del livello conversazionale: qui sarebbe il luogo dove finisce la traccia di chi ha chiesto cosa |
| 55 — SyncGrove | **complementare** | Le fonti **esterne** alla suite sono materia sua, non nostra (§2.4) |

**Riga di lettura.** InsightGrove **non ha senso da sola**, e non va venduta come primo acquisto: è il pezzo che
trasforma tre abbonamenti separati in una suite. Rispetto alle entità condivise individuate dal catalogo (§6),
tocca tutte e tre le catene — l'anagrafica clienti (come **dimensione**, non come archivio), il catalogo prodotti
(idem) e la catena preventivo → fattura → incasso, che è precisamente la sequenza di fonti da cui derivano gli
indicatori più preziosi. Ma le tocca **di lato**: ne riceve le misure, non ne condivide le tabelle.

**Sovrapposizioni da evitare.**

- **54 BudgetGrove** — sovrapposizione seria e da decidere prima di costruire entrambe. Il confine proposto:
  InsightGrove mostra **quello che è successo**, BudgetGrove mette a confronto con **quello che era previsto**.
  Se BudgetGrove si costruisce, l'obiettivo diventa una sua fonte e il confronto un indicatore derivato qui.
  Se non si costruisce, la tentazione di aggiungere «fatturato contro obiettivo» a InsightGrove sarà forte —
  e la fonte del §2.5 dice che è il primo indicatore che una micro-impresa vuole. **È un punto aperto** (§11,
  punto 8);
- **33 RenewGrove** e **19 SubGrove** hanno indicatori propri sui ricavi ricorrenti: il confine è che quelli
  restano **dentro le loro app** per il lavoro quotidiano, e arrivano qui come misure per il confronto con il
  resto dell'azienda. Non si duplica il calcolo: la fonte pubblica il numero già calcolato secondo la propria
  definizione;
- **31 AuditGrove** — la traccia delle azioni degli assistenti è sua. Qui si conserva la traccia **del calcolo**,
  che è un'altra cosa: la prima dice *chi ha fatto cosa*, la seconda *da dove viene questo numero*.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **InsightGrove deve stare in piedi da sola o servire la suite?** Il listino proposto (§5) risponde «servire la suite»: cruscotti illimitati anche nel gratuito, si paga il copilota. La risposta opposta cambierebbe la metrica di quota e tutto il listino | è una decisione di **direzione di prodotto**, e nessun dato di mercato l'ha risolta (§2.7) | **sviluppatore** |
| 2 | **Etichette di dimensione: via (A) con nomi leggibili o via (B) con soli codici?** Cambia la classificazione dell'app da «tratta dati personali» a «non ne tratta» (§6.1) | è una **classificazione di dati personali** con un contraccolpo forte sull'utilità del prodotto | **sviluppatore**, prima della storia 0006 |
| 3 | **Permessi più fini di `owner`/`admin`/`member`** — «Anna vede Torino, Luca vede Milano», «il commerciale vede il fatturato ma non i margini» | la piattaforma **non ha** un modello di autorizzazione per risorsa o per valore di dimensione, e costruirlo dentro una sola app sarebbe peggio del problema (§4.4) | **piattaforma** — nessuno use case lo copre oggi |
| 4 | **Condivisione verso l'esterno**: un collegamento pubblico a un cruscotto vivo, da dare al commercialista o alla banca | è un **effetto verso l'esterno** su dati economici, senza autenticazione: non lo decido. In questa proposta è **fuori ambito**, e la via sanzionata è l'esportazione (storia 0027) e il rapporto per posta (0028) | **sviluppatore** |
| 5 | **Indicatori per persona dell'account** (ticket per operatore, ore per persona) | ricadono nella disciplina del controllo a distanza dell'attività lavorativa (§2.3, punto 3): stessa materia che ha escluso 11 ShiftGrove dal catalogo | **sviluppatore**, con parere legale |
| 6 | **Dimensioni riconducibili a categorie particolari** pubblicate da una fonte futura | il presidio è contrattuale, non tecnico (§6.2); oggi il rischio è basso perché le app che le tratterebbero sono escluse | **sviluppatore**, se e quando una fonte del genere entra nel catalogo attivo |
| 7 | **Colore-categoria `blue` già proposto da 04 LeadGrove** | sei colori per sessanta app: la collisione è strutturale. Qui è aggravata dal fatto che tre colori sono **inutilizzabili** perché servono a significare stato dentro i numeri (§3) | **piattaforma**, quando i colori si assegnano sul serio |
| 8 | **Confine con 54 BudgetGrove** su «fatturato contro obiettivo» | è l'indicatore numero uno secondo la fonte 5 (§2.5), e sta a cavallo delle due app | **sviluppatore**, quando si decide se costruire 54 |
| 9 | **Costo variabile di una domanda al copilota** | non l'ho stimato e non lo invento (§2.7): senza quel numero il listino del §5 non è validabile | **sviluppatore**, con una misura su prototipo |
| 10 | **Chi definisce le metriche predefinite e chi le può cambiare** — se il titolare ridefinisce «fatturato», il passato cambia | il meccanismo c'è (versioni, storia 0012), ma **se** un cliente debba poter ridefinire gli indicatori di sistema è una scelta di prodotto | **sviluppatore** |
| 11 | **Il contratto del fatto è di piattaforma o di questa app?** Se ogni fonte deve pubblicare fatti, lo schema dell'evento e il dovere di ripubblicare lo storico riguardano **tutte** le app, non solo questa | oggi non esiste un contratto di eventi di dominio nel repository: c'è solo `tenant.offboarded` per la purga | **piattaforma** — va deciso prima della storia 0006, altrimenti il contratto nasce dentro l'app sbagliata |

**Rischi noti**

- **L'app arriva prima delle fonti.** Se si costruisce InsightGrove quando esiste una sola app sorgente, il
  prodotto è un cruscotto con due numeri e nessuno lo compra — e in più le storie dell'epica 02 non sono
  collaudabili sul serio. *Attenuazione*: rispettare la nota del catalogo (3-4 app prima), e nel frattempo
  collaudare con il generatore di fatti inventati della storia 0005.
- **Il catalogo chiuso delle metriche frustra gli utenti evoluti.** Chi sa usare un foglio di calcolo vorrà una
  formula che il catalogo non prevede e si sentirà limitato. *Attenuazione*: è un compromesso consapevole — la
  ripetibilità del numero vale più della flessibilità — ma va detto in fase di vendita, e la storia 0013
  (metriche derivate da formula fra metriche) è la valvola di sfogo.
- **Fiducia persa una volta, persa per sempre.** Un numero sbagliato mostrato senza contrassegno di
  incompletezza è un danno che nessuna funzione successiva ripara. *Attenuazione*: la regola 3 del §4.3 —
  l'incompletezza si vede **prima** del numero — e il rifiuto degli avvisi di suonare su valori incompleti
  (storia 0020).
- **Il copilota diventa il prodotto e il cruscotto un accessorio.** Sarebbe un errore di posizionamento: la parte
  conversazionale è imitabile, la catena di custodia del numero no (§1). *Attenuazione*: tenere il copilota
  ancorato agli stessi strumenti dell'epica 07, senza scorciatoie proprie.
- **La revoca di una fonte è distruttiva e sembra innocua.** «Scollega» cancella fisicamente lo storico ricevuto,
  e chi lo fa può non capirlo. *Attenuazione*: conferma esplicita con il conteggio di ciò che verrà cancellato
  (storia 0008), e la stessa conferma sullo strumento conversazionale (§7).

**Fuori dimensionamento**: non applicabile. 7 epiche (fascia 4-7), da 4 a 6 storie per epica (fascia 4-8),
35 storie in tutto (fascia 20-45).
