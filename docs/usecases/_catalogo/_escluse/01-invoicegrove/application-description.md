# InvoiceGrove — descrizione dell'applicazione

**Numero di catalogo**: 01 · **Tipo**: orizzontale · conformità fiscale · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 1](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** InvoiceGrove prende un documento di vendita già formato — una fattura, una nota di credito — e lo porta
fino in fondo agli adempimenti di legge del paese in cui va emesso: lo traduce nel formato ufficiale, lo valida
contro le regole della giurisdizione **prima** di spedirlo, lo trasmette sul canale giusto (il Sistema di
Interscambio italiano, la rete Peppol, in prospettiva le piattaforme accreditate francesi e il portale polacco),
segue il suo ciclo di vita legale fino all'esito definitivo, e infine lo mette in conservazione a norma per i dieci
anni che la legge pretende. Riceve anche i documenti passivi che arrivano dagli stessi canali.

Non è un'applicazione che *crea* fatture: è lo strato che le rende **giuridicamente valide**. La fattura nasce
altrove — nell'app di fatturazione di appgrove, in BillGrove (catalogo 02), o in un gestionale del cliente che
espone i suoi documenti — e InvoiceGrove la porta a norma.

**Per chi.** Micro-imprese da 1 a 10 addetti e piccole imprese fino a 50, con priorità europea. Chi **compra** è il
titolare o il responsabile amministrativo, spinto da un obbligo con sanzione, non da un desiderio. Chi **usa** tutti
i giorni è la persona che tiene l'amministrazione — spesso una sola, spesso a tempo parziale — più il
commercialista esterno, che ha bisogno di vedere e di scaricare ma non di emettere. Il profilo tipico che rende
l'app necessaria è l'impresa che vende in più di un paese europeo: fintanto che si fattura solo in Italia, il
mercato locale è già servito bene e a poco prezzo (vedi §2.1).

**Quale problema toglie.** «Ho un cliente in Belgio e uno in Polonia e non ho idea di cosa devo mandare, a chi, e
in che formato — e se sbaglio la fattura non esiste.» Oggi il micro-imprenditore lo risolve in tre modi, tutti
costosi: chiede al commercialista (che spesso non conosce le regole degli altri paesi), compra un prodotto per
paese (moltiplicando abbonamenti e anagrafiche), oppure ignora il problema finché non arriva uno scarto e la
fattura resta giuridicamente inesistente. Il costo non è il canone: è il tempo perso a capire perché un documento
è stato rifiutato e la scadenza fiscale che si avvicina.

**Cosa NON fa.**

- **Non crea il documento commerciale.** Niente listini, niente catalogo prodotti, niente preventivi, niente
  solleciti, niente riconciliazione degli incassi: quelli stanno nell'app di fatturazione (repo: `fatture`;
  catalogo: BillGrove 02) e nell'incasso crediti (CashGrove 03).
- **Non è un punto di accesso Peppol né una piattaforma accreditata francese.** Non lo può essere: servono
  l'adesione e la certificazione presso OpenPeppol e, in Francia, l'immatricolazione presso l'amministrazione
  fiscale con certificazione ISO 27001 (§2.3). InvoiceGrove **cavalca** un fornitore certificato tramite un
  adattatore, e questo è un vincolo architetturale, non una scelta rimandabile.
- **Non è un conservatore accreditato.** Il pacchetto di conservazione lo compone InvoiceGrove; il sigillo, la
  marca temporale e la custodia a norma li fornisce un servizio qualificato esterno.
- **Non fa contabilità**, non calcola le imposte dovute, non produce le dichiarazioni. Produce i **riepiloghi**
  che servono a chi le fa.
- **Non copre, nella prima versione, tutte le giurisdizioni del catalogo.** L'ambito iniziale è Italia (modello a
  liberatoria) e rete Peppol a quattro angoli (Belgio e paesi nordici). Polonia, Francia, Germania e India sono
  previste dal contratto dell'adattatore ma non implementate: vedi §11.

**Rischio di sostituzione da parte dei modelli linguistici.** `rafforzata`. Un assistente generico può spiegare
cosa dice la norma, ma non può firmare un documento, non può parlare con il Sistema di Interscambio, non può
garantire che un pacchetto di conservazione sia opponibile fra otto anni, e soprattutto non si assume il rischio
di uno scarto. Il valore sta nell'esecuzione con effetto giuridico e nella responsabilità, non nella conoscenza.
Al contrario, il livello conversazionale rende l'app **più** utile: «perché è stata scartata la fattura 214?» è
esattamente la domanda che oggi costa mezz'ora di ricerca su un forum.

---

## 2. Mercato e analisi in rete

> Compilata dopo 13 ricerche e letture mirate (elenco completo in §2.6).
> Ciò che non ho trovato è dichiarato in §2.7, non colmato a intuito.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| Fatture in Cloud | Italia | Fatturazione completa **più** invio al Sistema di Interscambio e conservazione incluse nel canone | €4/mese (forfettari, 100 doc/anno) · €12 Standard · €21 Premium (400 doc/anno) · €29 Premium Plus (800) · €51 Complete (3.000, multiutente) — IVA esclusa, prova 31 giorni | [fattureincloud.it/costo](https://fattureincloud.it/costo/) |
| Openapi / A-Cube (SdI) | Italia | Interfaccia di programmazione per trasmettere al Sistema di Interscambio, firmare e conservare — è un **fornitore**, non un concorrente diretto | €0,070 per creazione fattura (€0,022 a volume) · €0,090 firma · €0,105 conservazione · €0,125 firma+conservazione (€0,052 a volume) | [console.openapi.com/apis/sdi/pricing](https://console.openapi.com/apis/sdi/pricing) |
| e-invoice.be | Belgio / Peppol | Punto di accesso Peppol come interfaccia di programmazione, pagamento a documento | «da €0,25 per fattura» piano Pro · «da €0,18» piano Enterprise, a volume negoziato; invio **e** ricezione fatturati entrambi | [e-invoice.be/peppol-api](https://e-invoice.be/peppol-api) |
| Storecove | Paesi Bassi / globale | Punto di accesso Peppol certificato, oltre 30 paesi, prezzo a scaglioni decrescenti | preventivo su misura, indicazione «da ~€495/mese» su un comparatore; **nessun listino pubblico** | [g2.com/products/storecove/pricing](https://www.g2.com/products/storecove/pricing) |
| Piattaforme accreditate francesi | Francia | Obbligatorie per legge dal 1° settembre 2026; oltre 100 immatricolate | «da €0 a oltre €500/mese»; per micro-imprese e liberi professionisti «fra €0 e €30/mese» (fonte editoriale, non listino ufficiale) | [independant.io/liste-plateforme-dematerialisation-partenaire](https://independant.io/liste-plateforme-dematerialisation-partenaire/) |

**Lettura.** Il mercato è spaccato in due e InvoiceGrove sta scomodamente in mezzo. Da un lato i **prodotti
completi per paese** (Fatture in Cloud in Italia): fanno la fattura *e* la conformità, costano pochissimo, e chi
fattura solo in Italia non ha alcun motivo di comprare uno strato di conformità separato — a €12 al mese ha già
tutto. Dall'altro lato le **infrastrutture per sviluppatori** (Openapi, Storecove, e-invoice.be): vendono il
trasporto a documento e non hanno interfaccia per un amministrativo. Lo spazio difendibile è la fascia di mezzo:
l'impresa **multi-paese** che oggi deve comprare un prodotto per giurisdizione e non ha nessuno che le tenga
insieme il ciclo di vita. Questa è anche la ragione per cui la nota del catalogo §6 è fondata: vedi §10.

### 2.2 Prezzi praticati nel dominio

Rilevato su **pagine ufficiali**:

- **Prodotto completo italiano** (Fatture in Cloud): €4–51 al mese, unità di misura = **documenti all'anno**
  (100 / 400 / 800 / 3.000), conservazione a norma inclusa in tutti i piani, un solo utente fino al piano da €51.
  Prova gratuita di **31 giorni** ma **con la fatturazione elettronica esclusa** dalla prova — dettaglio
  significativo, perché conferma che nessuno fa provare gratuitamente un'azione con effetto verso l'autorità
  fiscale.
- **Costo variabile del trasporto in Italia** (Openapi): creazione €0,070, firma €0,090, conservazione €0,105;
  il pacchetto firma + conservazione €0,125, che scende a €0,052 a volume. Sommando creazione e pacchetto, il
  costo pieno di un documento italiano trasmesso, firmato e conservato sta fra **€0,074 (a volume)** e **€0,195
  (a listino base)**.
- **Costo variabile del trasporto su Peppol** (e-invoice.be): **€0,18–0,25 per documento**, e si paga sia l'invio
  sia la ricezione.
- **Adesione e certificazione OpenPeppol** per chi volesse diventare punto di accesso: iscrizione €1.025–2.750 e
  quota annuale €1.800–5.100 a seconda della dimensione dell'organizzazione, più la certificazione
  ([peppol.org/join/fees](https://peppol.org/join/fees/)).

Da **fonte editoriale, non da listino ufficiale**: le piattaforme accreditate francesi per micro-imprese
starebbero fra €0 e €30 al mese. Da leggere come ordine di grandezza (il catalogo stesso, §8, avverte che i prezzi
di seconda mano invecchiano male).

**Conseguenza sui margini, ed è la più importante di questa sezione.** La scheda di catalogo avverte: «con
provider a €0,18–0,30/fattura, il tier base non può stare sotto €15–19/mese». La rilevazione **conferma
l'avvertenza per Peppol e la smentisce per l'Italia**: in Italia il costo variabile a volume è di circa 7
centesimi, un ordine di grandezza sotto. Sono due economie diverse e vanno prezzate come tali, non con un'unica
fascia media. È il ragionamento che regge la proposta di listino del §5.

### 2.3 Obblighi normativi del settore

È il dominio più normato dell'intero catalogo: qui la legge non è un contorno, è il prodotto.

**Il calendario dei mandati europei** (fonti in §2.6, voci 1, 2, 12):

| Paese | Da quando | Modello | Nota |
|---|---|---|---|
| Italia | in vigore (B2B **e** B2C) | liberatoria via Sistema di Interscambio, formato FatturaPA | la fattura non esiste giuridicamente finché il Sistema non l'ha accettata |
| Belgio | 1° gennaio 2026, tutte le imprese insieme | quattro angoli su rete Peppol | l'evento rilevante è la **consegna** al destinatario, non l'accettazione di un'autorità |
| Polonia | grandi imprese febbraio 2026, piccole e medie aprile 2026, **micro-imprese 1° gennaio 2027** | liberatoria via portale KSeF, formato FA(3) dal 1° febbraio 2026 | esiste una modalità di emergenza «offline24» con due codici QR e obbligo di invio entro il giorno lavorativo successivo; sanzioni fino al 100% dell'imposta dal 1° gennaio 2027 |
| Francia | 1° settembre 2026 obbligo di **ricezione** per tutti e di emissione per le grandi imprese; **1° settembre 2027** per le micro e piccole imprese | cinque angoli: piattaforma accreditata + comunicazione fiscale parallela | l'uso di una piattaforma accreditata è obbligatorio per ogni soggetto con partita IVA francese |
| Germania | ricezione obbligatoria da gennaio 2025; emissione da gennaio 2027 sopra €800.000 di fatturato, da gennaio 2028 per tutti | rete, formati XRechnung e ZUGFeRD | |
| Unione Europea | 1° luglio 2030 | comunicazione digitale obbligatoria sulle operazioni intracomunitarie (pacchetto «IVA nell'era digitale») | è l'orizzonte che rende l'app strutturale e non congiunturale |

**Conservazione a norma (Italia).** Non è un archivio: è un processo. Le linee guida dell'Agenzia per l'Italia
Digitale e il Codice dell'amministrazione digitale impongono **dieci anni** di conservazione con metadati
obbligatori, firma digitale del pacchetto di versamento, marca temporale, manuale di conservazione e un
responsabile della conservazione nominato. Salvare un file XML su un disco non è conservare
([quicommercialista.it](https://quicommercialista.it/guida/conservazione-fatture-elettroniche),
[fatturapa.gov.it](https://www.fatturapa.gov.it/it/SistemaAccreditamento/)).

**Accreditamento del canale (Italia).** Per trasmettere al Sistema di Interscambio per conto di terzi serve
accreditare un canale (SDICoop su protocollo HTTPS con TLS 1.2, messaggi SOAP con allegati, certificati rilasciati
dall'Agenzia dopo una batteria di prove di interoperabilità). Un codice destinatario di intermediario può servire
fino a 100 posizioni ([istruzioni SDICoop, Agenzia delle
Entrate](https://www.fatturapa.gov.it/export/documenti/ws/trasmissione/v3.x/Istruzioni-per-il-servizio-SDICoop-Trasmissione-versione3.2.pdf)).

**Immatricolazione della piattaforma (Francia).** Per essere piattaforma accreditata bisogna presentare un
dossier all'amministrazione fiscale dimostrando capacità di emissione, trasmissione, ricezione, conversione di
formato e trasferimento sicuro dei dati fiscali, **più una certificazione ISO 27001**; l'immatricolazione dura tre
anni ed è rinnovabile.

**Il divieto sanitario italiano — obbligo che cambia il modello dati.** È **vietato** emettere fattura elettronica
via Sistema di Interscambio per prestazioni sanitarie rese a **persone fisiche**. Fino al 2025 era una proroga
annuale; con il decreto legislativo 81/2025 è diventato **permanente**. Chi trasmette i dati al Sistema Tessera
Sanitaria — e anche chi non vi è tenuto ma documenta prestazioni sanitarie verso persone fisiche — deve emettere
in formato **non elettronico** ([fiscomania.com](https://fiscomania.com/prestazioni-sanitarie/),
[studiomeli.it](https://www.studiomeli.it/prestazioni-sanitarie-e-fattura-elettronica-divieto-permanente-dal-2026/)).
Conseguenza diretta e non negoziabile per InvoiceGrove: la validazione italiana deve **rifiutare** un documento
marcato come prestazione sanitaria verso persona fisica, e il rifiuto va spiegato. È anche il presidio che tiene
le categorie particolari fuori dall'app (§6).

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta attesa, con l'indicazione di chi tratterebbe dati per conto nostro:

1. **La sorgente dei documenti**: l'app di fatturazione di appgrove (`fatture`) e, nel catalogo, BillGrove (02).
   Interna alla piattaforma, per eventi — non è un fornitore esterno.
2. **Il fornitore di trasmissione italiano** (tipo Openapi/A-Cube): riceve la fattura completa, quindi **anche i
   dati personali della controparte**. È un responsabile esterno del trattamento. ⚠️
3. **Il punto di accesso Peppol** (tipo e-invoice.be, Storecove): stesso discorso. ⚠️
4. **Il servizio di conservazione qualificato**: custodisce i documenti per dieci anni. Responsabile esterno, con
   in più il problema della restituzione a fine rapporto. ⚠️
5. **Il commercialista**: non è un'integrazione tecnica ma un ruolo di lettura e scarico. Nella prima versione si
   risolve con un ruolo `member` in sola lettura e con l'esportazione, non con un portale dedicato.
6. **La banca / la riconciliazione degli incassi**: **fuori ambito**, è CashGrove (03).
7. **Il gestionale di terze parti** come sorgente alternativa a BillGrove: richiesta prevedibile, non coperta
   nella prima versione — resta l'importazione da file (storia `0013`).

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Dalle guide di scelta rivolte ai forfettari italiani (fonti 8 e 9 in §2.6), il quadro è netto e utile perché è
per sottrazione: **«la semplicità al minor costo possibile»** è la priorità assoluta dichiarata. Le lamentele
ricorrenti verso i prodotti gratuiti — interfaccia poco intuitiva, nessuna automazione, nessuna integrazione,
reportistica minima — sono requisiti travestiti: chi paga vuole che le cose si facciano da sole e che si capisca
subito cosa non va.

Tradotto in requisiti per InvoiceGrove:

- **Il messaggio d'errore è il prodotto.** Uno scarto del Sistema di Interscambio arriva con un codice tipo
  `00311` e nient'altro. Trasformarlo in «il codice destinatario del cliente non è valido: chiediglielo e
  correggilo qui» è, letteralmente, la funzione per cui il cliente paga. È la storia `0015`.
- **Nessuno vuole imparare la parola "Peppol".** L'interfaccia deve chiedere «a chi mandi la fattura e in che
  paese», non «quale identificativo di rete».
- **Cosa non vogliono**: dashboard di analisi, grafici, moduli configurabili. Vogliono sapere due cose — «è
  andata?» e «se no, perché?» — e vederle nella prima schermata.
- Il tetto di documenti come unità di misura del piano è **già compreso** dal mercato italiano: è esattamente
  come vende Fatture in Cloud. Non serve educare il cliente su una metrica nuova.

### 2.6 Fonti consultate

1. **E-Invoicing Mandates in Europe: The 2026 Business Guide** — https://www.spscommerce.com/community/articles/e-invoicing-mandates-in-europe-the-2026-business-guide — riferimento della scheda di catalogo; quadro dei mandati europei per paese e anno.
2. **E-Invoicing Europe 2025-2027: Complete Mandate Guide** (Novutech) — https://www.novutech.com/news/e-invoicing-in-europe-overview-of-mandates-2025-2027 — riferimento della scheda; conferma le tre famiglie di modelli (liberatoria, quattro angoli, cinque angoli).
3. **E-Invoicing Mandate Matrix** (e-invoice.be) — https://e-invoice.be/e-invoicing-mandate-matrix — matrice paese/anno: chi è già obbligatorio (Belgio, Germania, Italia, Romania, Ungheria, Grecia) e chi arriva nel 2026-2027 (Francia, Spagna, Polonia, Cechia); conferma che l'Italia è l'unica con obbligo anche verso i consumatori.
4. **Prezzi Fatturazione Elettronica SDI — Openapi Console** — https://console.openapi.com/apis/sdi/pricing — **listino ufficiale a documento**: creazione €0,070 (€0,022 a volume), firma €0,090, conservazione €0,105, firma+conservazione €0,125 (€0,052 a volume). È la base del calcolo di margine del §5.
5. **Peppol API — e-invoice.be** — https://e-invoice.be/peppol-api — **listino ufficiale**: da €0,25 a fattura (Pro), da €0,18 (Enterprise, a volume negoziato); invio e ricezione fatturati separatamente; conto, identificativo Peppol e ambiente di prova gratuiti.
6. **Fatture in Cloud — costi** — https://fattureincloud.it/costo/ — **listino ufficiale**: €4/€12/€21/€29/€51 al mese, tetti a 100/100/400/800/3.000 documenti l'anno, conservazione inclusa, prova di 31 giorni **con la fatturazione elettronica esclusa**. È l'ancora di prezzo del mercato italiano.
7. **OpenPeppol — Fees** — https://peppol.org/join/fees/ — quote di adesione e certificazione per un fornitore di servizi punto di accesso: iscrizione €1.025–2.750, quota annuale €1.800–5.100 secondo la dimensione. Prova che diventare punto di accesso non è alla portata di questa app.
8. **Liste des plateformes de dématérialisation partenaires** — https://independant.io/liste-plateforme-dematerialisation-partenaire/ — obbligo di scegliere una piattaforma accreditata dal 1° settembre 2026; requisiti di immatricolazione compresa la **certificazione ISO 27001**; fasce di prezzo €0–30/mese per micro-imprese (fonte editoriale, non listino).
9. **Istruzioni per il servizio SDICoop — Trasmissione v3.2** (Agenzia delle Entrate) — https://www.fatturapa.gov.it/export/documenti/ws/trasmissione/v3.x/Istruzioni-per-il-servizio-SDICoop-Trasmissione-versione3.2.pdf — requisiti tecnici dell'accreditamento del canale: TLS 1.2, SOAP con allegati, certificati rilasciati dopo prove di interoperabilità.
10. **Conservazione delle fatture elettroniche: 10 anni obbligatori** — https://quicommercialista.it/guida/conservazione-fatture-elettroniche — obbligo decennale, metadati, firma del pacchetto di versamento, marca temporale, manuale e responsabile della conservazione.
11. **Prestazioni sanitarie e fattura elettronica: divieto permanente dal 2026** — https://www.studiomeli.it/prestazioni-sanitarie-e-fattura-elettronica-divieto-permanente-dal-2026/ e https://fiscomania.com/prestazioni-sanitarie/ — il divieto di emettere via Sistema di Interscambio verso persone fisiche per prestazioni sanitarie è diventato **permanente** con il decreto legislativo 81/2025. Regola di validazione obbligatoria e presidio contro le categorie particolari.
12. **KSeF dla mikroprzedsiębiorców** (Varico) — https://web.varico.pl/blog/ksef-dla-mikroprzedsiebiorcow-od-kiedy-obowiazkowy-i-jak-sie-przygotowac/ e https://ksef.podatki.gov.pl/jdg-i-msp/ — micro-imprese polacche obbligate dal 1° gennaio 2027, formato FA(3) dal 1° febbraio 2026, modalità «offline24» con doppio codice QR e invio entro il giorno lavorativo successivo, sanzioni fino al 100% dell'imposta.
13. **Miglior software fatturazione elettronica forfettari 2026** — https://www.freeinvoice.it/blog/software-fatturazione-elettronica-forfettari/ e https://www.danea.it/blog/come-scegliere-il-miglior-software-di-fatture-per-un-forfettario/ — aspettative dichiarate del segmento micro italiano: semplicità al minimo costo; lamentele ricorrenti sui prodotti gratuiti (interfaccia, automazioni, integrazioni assenti).

### 2.7 Cosa NON sono riuscito a determinare

- **Il prezzo effettivo a documento su Peppol a volumi da micro-impresa.** Storecove non pubblica listino
  (solo preventivo su misura); e-invoice.be pubblica «da €0,18» ma con la formula «a volume negoziato», che per un
  aggregatore alle prime armi significa realisticamente €0,25. Serve un preventivo vero prima di fissare il prezzo
  del piano multi-paese: è la variabile che decide se quel piano sta in piedi.
- **Il costo della conservazione a norma nel tempo**, cioè quanto costa tenere in custodia un documento per il
  nono e il decimo anno. Openapi pubblica un costo *a documento versato* (€0,105), non un costo di giacenza. Se il
  fornitore fattura anche la giacenza, il conto del §5 cambia in modo sostanziale.
- **Il modello di responsabilità in caso di scarto.** Non ho trovato, sui fornitori esaminati, una dichiarazione
  chiara su chi risponde se un documento viene rifiutato per un difetto di serializzazione del fornitore. È un
  punto contrattuale, non tecnico, e va chiuso prima di vendere una promessa di conformità.
- **Il costo dell'accreditamento del canale italiano** presso l'Agenzia delle Entrate. La documentazione descrive
  la procedura tecnica ma non parla di oneri; presumo sia gratuita, ma **non l'ho verificato** e non lo scrivo
  come fatto.
- **Se esista un concorrente diretto** che vende esattamente «strato di conformità multi-paese per micro-imprese».
  Ho trovato prodotti completi per paese e infrastrutture per sviluppatori; non ho trovato nessuno posizionato nel
  mezzo. Può voler dire che lo spazio è libero, o che non esiste perché non paga: non ho elementi per decidere e
  non li invento.

---

## 3. Varco d'identità — le risposte pronte per `new-application`

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `einvoicing` | Rispetta `^[a-z][a-z0-9_]{0,30}$` (10 caratteri, minuscolo, solo lettere). Dice **cosa l'app è** — l'adempimento di fatturazione elettronica — e non come è commercializzata. Non uso `fatture`, che è **già occupato** dall'app reale numero 1 del repository (`services/fatture`, listino `pricing/fatture.yaml`), né `invoice`, che descriverebbe il documento invece dell'adempimento. Da qui discendono lo schema `app_einvoicing`, la rotta `/api/einvoicing/v1/*` e l'istanza del modulo di infrastruttura: rinominarlo dopo sarebbe una migrazione di dati. |
| **Modello utente** | `multi` | Chi emette non è chi controlla. Nella giornata tipo l'amministrativo prepara e trasmette, il titolare autorizza le trasmissioni fuori dall'ordinario, il commercialista legge e scarica per la dichiarazione. Serve sapere **chi ha trasmesso cosa all'autorità**: è una traccia che ha valore probatorio, e un'app a utente singolo non ha il concetto di «chi ha fatto cosa». Nota: l'app reale `fatture` del repository è `single_user`; qui il modello è diverso perché diverso è il segmento (impresa con amministrazione, non professionista solo). |
| **Porta locale** | `8101` | Convenzione del kit: 8100 + numero di catalogo (01). Da confermare con `./dev.sh services` al momento dello scaffolding, che elenca le porte già prese (oggi `8081` fatture, `8082` mini-CRM, `9100` autenticazione). |
| **Metrica di quota** | `documenti` (documenti trasmessi o ricevuti in un mese) | È la **sola** cosa che il piano limita ed è l'unica grandezza che cresce insieme sia al valore ricevuto dal cliente sia al nostro costo variabile (§2.2: da €0,07 a €0,25 a documento, secondo il canale). Ho scartato «soggetti emittenti» e «giurisdizioni attive» perché limitano l'accesso, non il consumo, e ho scartato «utenti» perché un'app di conformità non si vende a posti. Il mercato italiano usa già questa unità di misura (§2.5): non c'è da educare nessuno. |
| **Natura della metrica** | `flow` | «200 documenti al mese»: ad aprile se ne possono trasmettere altri 200 comunque sia andato marzo. È consumo su una finestra che si azzera, non un tetto su ciò che esiste: le fatture di marzo restano in archivio per dieci anni ma non occupano la quota di aprile. Contarla come giacenza bloccherebbe il cliente per sempre al 200° documento della sua vita. ⚠️ Attenzione, e il punto è nella sezione 11: **la conservazione è invece una giacenza** che si accumula per dieci anni. Metrica e costo divergono, ed è un rischio di margine dichiarato, non un errore di classificazione. |
| **Colore-categoria e icona** | `amber` · icona `file-check` | `amber` è il colore dell'attenzione e della scadenza, ed è ciò che questa app è: adempimenti con date e conseguenze. Le app vicine del catalogo prendono gli altri colori naturali della famiglia finanziaria — l'app reale `fatture` del repository è già `green` e il mini-CRM è `blue` — e `red` va lasciato al pericolo, non alla conformità ordinaria. Deve coincidere fra `category` nel listino e `accentToken` nel manifesto del modulo frontend. |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `LegalEntity` | Il soggetto fiscale che emette: l'impresa del cliente, o una delle sue sedi/partite IVA | denominazione, identificativo IVA, codice fiscale, indirizzo, regime fiscale, giurisdizione di appartenenza, canale di trasmissione configurato | **sì** — se il cliente è una ditta individuale o un libero professionista, denominazione e codice fiscale sono dati di una persona |
| `Counterparty` | La controparte del documento: cliente o fornitore | denominazione, identificativo IVA o codice fiscale, indirizzo, **recapito elettronico** (codice destinatario, indirizzo di posta certificata, identificativo Peppol, identificativo fiscale polacco), paese | **sì** — nome, indirizzo e codice fiscale di una persona fisica quando la controparte è un professionista o un consumatore |
| `Jurisdiction` | Il profilo di conformità di un paese | codice paese, famiglia del modello (`clearance`, `four_corner`, `five_corner`), formato di serializzazione, canale, versione delle regole, date di entrata in vigore | no |
| `CanonicalDocument` | Il documento nel modello interno allineato alla norma europea EN 16931, indipendente dal formato di destinazione | tipo (fattura, nota di credito), numero, data, valuta, totali, riferimenti, `LegalEntity`, `Counterparty`, `Jurisdiction`, stato del ciclo di vita, origine (evento, importazione, inserimento manuale) | **indirettamente** — attraverso emittente e controparte, e attraverso le descrizioni di riga |
| `DocumentLine` | La riga del documento | descrizione, quantità, prezzo, aliquota, natura dell'operazione, esenzione | **potenzialmente** — la descrizione è testo libero: vedi §6 |
| `ValidationRule` | Una regola di conformità, appartenente a una giurisdizione e a una versione | codice, gravità (blocca / avverte), espressione, messaggio in lingua comune nelle cinque lingue, riferimento normativo | no |
| `ValidationOutcome` | L'esito di una validazione su un documento | esito complessivo, elenco delle violazioni con posizione nel documento, versione delle regole usata, istante | no |
| `Transmission` | Un tentativo di consegna del documento su un canale | canale, fornitore, identificativo esterno, stato, tentativi, istanti, carico inviato (riferimento) | no direttamente (il carico sì) |
| `LifecycleEvent` | Un fatto ricevuto dal canale che fa avanzare la macchina a stati | tipo (ricevuta di consegna, mancata consegna, scarto, accettazione, rifiuto commerciale), codice originale, istante dell'autorità, istante di acquisizione | no |
| `ArchiveRecord` | Il documento messo in conservazione a norma | riferimento del pacchetto di versamento, impronta crittografica, marca temporale, indice dei metadati obbligatori, scadenza decennale, fornitore | no direttamente (il contenuto conservato sì) |
| `MandateWatch` | La scadenza di un mandato normativo per paese, con lo stato dell'account rispetto a essa | paese, data, soglia di applicabilità, stato per il soggetto (`non applicabile`, `si avvicina`, `attivo`, `in ritardo`) | no |
| `VatSummary` | Il riepilogo per periodo utile a chi fa la dichiarazione | periodo, giurisdizione, imponibile e imposta per aliquota, conteggi per stato del ciclo di vita | no |

**Relazioni.** Un account (`tenant`) ha uno o più `LegalEntity`; ogni `LegalEntity` appartiene a una
`Jurisdiction` e configura un canale. Un `CanonicalDocument` appartiene a un `LegalEntity`, punta a una
`Counterparty` e a una `Jurisdiction`, contiene molte `DocumentLine`, produce uno o più `ValidationOutcome`,
genera zero o più `Transmission`, riceve molti `LifecycleEvent` e finisce in **un** `ArchiveRecord`.

**Macchina a stati — ed è la parte che le storie devono rispettare.** La nota architetturale del catalogo è
esplicita: «non trattare il problema come "stessi dati, XML diversi"». Il ciclo di vita **cambia famiglia** con
la giurisdizione, e lo stato non è un attributo cosmetico: dice se il documento esiste giuridicamente.

- **Famiglia a liberatoria** (Italia, Polonia) — `bozza` → `validato` → `in_trasmissione` →
  **`accettato_dall_autorita`** (con identificativo del Sistema di Interscambio o di KSeF: **è qui che la fattura
  nasce giuridicamente**) → `consegnato` oppure `mancata_consegna` (messa a disposizione). Ramo di errore:
  `scartato` con codice → si corregge → si ritrasmette come documento **nuovo**, non come modifica.
- **Famiglia a quattro angoli** (Belgio e rete Peppol) — `bozza` → `validato` → `in_trasmissione` →
  **`consegnato_al_destinatario`** (l'evento rilevante è la consegna, non l'accettazione di un'autorità) →
  `accettato` oppure `rifiutato` dal destinatario per motivi commerciali. Non esiste lo stato «accettato
  dall'autorità»: cercarlo è l'errore tipico di chi porta il modello italiano fuori dall'Italia.
- **Famiglia a cinque angoli** (Francia) — quattro angoli **più** un flusso parallelo di comunicazione fiscale con
  stato proprio, **più** gli stati obbligatori del ciclo di vita che l'acquirente deve restituire. Non
  implementata nella prima versione: il contratto dell'adattatore la prevede, la storia no (§11).

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica
(`deleted_at`); schema `app_einvoicing`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8). **Eccezione dichiarata**: la cancellazione logica non
si applica a `ArchiveRecord`, perché un documento in conservazione a norma non si cancella su richiesta — c'è un
obbligo di legge decennale che prevale. Il conflitto fra questo obbligo e il diritto alla cancellazione è
affrontato in §6 e nella storia `0026`.

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il
> file `services/core/src/main/resources/pricing/einvoicing.yaml`.

**Ragionamento.** Tre vincoli, in ordine di durezza.

1. **L'ancora italiana è brutale.** Fatture in Cloud vende fattura *più* conformità *più* conservazione a €12 al
   mese, €4 per i forfettari (§2.1). Uno strato di sola conformità non può costare quanto un prodotto completo:
   deve stare **sotto**, oppure vendere qualcosa che quello non fa — cioè il multi-paese.
2. **Il costo variabile ha due economie diverse.** Italia: €0,074 a documento a volume, €0,195 a listino base.
   Peppol: €0,18–0,25, **sia in invio sia in ricezione**. Un piano unico a fascia media sbaglia entrambe.
3. **La piattaforma vieta l'addebito a consumo.** Il catalogo proponeva «+€0,10–0,30 per e-invoice oltre soglia» e
   «add-on per giurisdizione aggiuntiva»: **nessuna delle due è ammessa** ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md)
   §7 — solo abbonamento ricorrente, al limite si blocca con `429`, e i limiti stanno nel listino, non in
   componenti aggiuntivi). Il modello del catalogo va quindi tradotto: la soglia diventa un **blocco**, la
   giurisdizione aggiuntiva diventa un **piano superiore**.

Conti di margine, con la sola aritmetica visibile (assunzione: circa il 60% del tetto effettivamente consumato,
che è il comportamento normale su un piano a soglia):

| Piano | Ricavo netto stimato al mese | Documenti consumati (60% del tetto) | Costo variabile stimato | Margine lordo |
|---|---|---|---|---|
| `italia` | ~€10,50 su €12 | 30 su 50 | 30 × €0,074 ≈ **€2,2** | ~€8,3 |
| `europa` | ~€26,5 su €29 | 60 su 100, ipotizzando metà su Peppol | 30 × €0,074 + 30 × €0,22 ≈ **€8,8** | ~€17,7 |
| `studio` | ~€64 su €69 | 240 su 400, un terzo su Peppol | 160 × €0,074 + 80 × €0,22 ≈ **€29,4** | ~€34,6 |

| Piano | Prezzo mensile | Prezzo annuale | Limite sulla metrica `documenti` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `italia` | €12 | €120 (= 10× il mensile, «due mesi in regalo») | 50 al mese | 14 giorni, **in modalità prova** (vedi sotto) | Micro-impresa che fattura solo in Italia e ha già un gestionale o BillGrove come sorgente |
| `europa` | €29 | €290 | 100 al mese | 14 giorni, in modalità prova | Impresa che vende in più paesi europei: Italia più rete Peppol, più soggetti emittenti fino a 3 |
| `studio` | €69 | €690 | 400 al mese | 14 giorni, in modalità prova | Piccola impresa con volumi veri o studio che segue più partite IVA: soggetti emittenti senza limite |

**Note obbligate.**

- **Nessun piano gratuito, ed è una scelta motivata, non una dimenticanza.** Ogni documento ha un costo variabile
  verso un fornitore, e soprattutto ogni documento conservato genera **dieci anni di obbligo di custodia** che
  sopravvivono alla disdetta. Un piano gratuito qui non è un assaggio: è una passività decennale regalata. Se lo
  sviluppatore volesse comunque un ingresso gratuito, la forma sicura è «validazione e anteprima gratuite,
  trasmissione a pagamento» — cioè la stessa cosa della modalità prova.
- **Prova gratuita di 14 giorni in modalità prova.** La prova **non trasmette all'autorità**: valida, converte,
  mostra l'anteprima del file ufficiale e simula il ciclo di vita, ma non produce effetti verso l'esterno. Non è
  una limitazione commerciale, è l'applicazione della regola «effetti irreversibili verso l'esterno» a un'azione
  che, una volta fatta, non si disfa. Conforta la scelta il fatto che **Fatture in Cloud esclude proprio la
  fatturazione elettronica dalla sua prova di 31 giorni** (§2.2): nessuno fa provare gratis un atto fiscale.
- Un limite **lasciato vuoto significa illimitato**, non zero. Qui nessun piano ha limite vuoto: il costo
  variabile non lo permette.
- **Costo effettivo dell'incasso.** Il piano `italia` a €12 è sopra la soglia dei ~5 € che rende dolorosa la parte
  fissa per transazione, ma non di molto: sull'annuale a €120 la componente fissa quasi sparisce. Spingere
  l'annuale su questo piano è la mossa naturale.
- I prezzi sono **immutabili una volta vivi**: un cambio di prezzo si fa creando un prezzo nuovo e archiviando il
  vecchio, gli abbonati restano sul loro.
- **La riga più fragile di questa tabella è `europa`.** Regge solo se il costo Peppol negoziato sta intorno a
  €0,18–0,22 e se non tutti i documenti sono transfrontalieri. Con €0,25 e un cliente che manda tutto su Peppol,
  60 documenti costano €15 su €26,5 di ricavo netto: margine dimezzato. Va chiuso con un preventivo vero prima di
  pubblicare (§2.7, punto 1).

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/einvoicing.yaml`) si compila **insieme** allo sviluppatore: «niente contratto,
> niente produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

> 🛑 **Attenzione — rischio di categorie particolari (articolo 9) per via indiretta.** L'app **non chiede e non
> vuole** dati sanitari, biometrici, genetici, politici, religiosi, sindacali o sull'orientamento sessuale, e la
> classificazione che propongo è **senza categorie particolari**. Ma il rischio esiste ed entra da una porta sola:
> la **descrizione di riga** del documento è testo libero, e una riga come «visita specialistica del 3 marzo»
> intestata a una persona fisica è a tutti gli effetti un dato relativo alla salute. Non è un'ipotesi di scuola:
> è esattamente il motivo per cui l'Italia **vieta in modo permanente** (decreto legislativo 81/2025) di emettere
> fattura elettronica via Sistema di Interscambio per prestazioni sanitarie verso persone fisiche
> ([fonte](https://www.studiomeli.it/prestazioni-sanitarie-e-fattura-elettronica-divieto-permanente-dal-2026/)).
>
> **Il presidio è quindi normativo prima che tecnico, e va costruito, non assunto**: la validazione italiana deve
> **rifiutare** un documento marcato come prestazione sanitaria verso persona fisica (storia `0014`), e
> l'interfaccia deve dirlo prima che l'utente scriva la riga. Con quel presidio attivo, l'app resta fuori
> dall'articolo 9. Senza, non ci resta. **La conferma di questa lettura è dello sviluppatore**: se la risposta è
> «lo trattiamo comunque», servono base giuridica rafforzata e valutazione d'impatto, e cambia la natura dell'app.

**Categorie trattate**

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `legal_entity.denominazione` | `legal_entity.denominazione` | titolare dell'account, se ditta individuale o libero professionista | anagrafica | identificare l'emittente sul documento fiscale | obbligo di legge (contenuto obbligatorio della fattura) | 10 anni dall'ultimo documento emesso |
| `legal_entity.codice_fiscale` | `legal_entity.codice_fiscale` | titolare dell'account, persona fisica | identificativo fiscale | identificare l'emittente presso l'autorità | obbligo di legge | 10 anni |
| `counterparty.denominazione` | `counterparty.denominazione` | cliente o fornitore del titolare | anagrafica | identificare la controparte sul documento | obbligo di legge | 10 anni dall'ultimo documento che la cita |
| `counterparty.codice_fiscale` | `counterparty.codice_fiscale` | cliente o fornitore persona fisica | identificativo fiscale | requisito di contenuto della fattura e chiave di recapito | obbligo di legge | 10 anni |
| `counterparty.indirizzo` | `counterparty.via`, `cap`, `comune`, `paese` | cliente o fornitore | contatto | contenuto obbligatorio della fattura | obbligo di legge | 10 anni |
| `counterparty.recapito_elettronico` | `counterparty.codice_destinatario`, `pec`, `peppol_id` | cliente o fornitore | contatto | instradare il documento sul canale corretto | esecuzione del contratto | finché la controparte è attiva, poi 10 anni con l'ultimo documento |
| `document_line.descrizione` | `document_line.descrizione` | riguarda la controparte quando è persona fisica | testo libero, **potenzialmente rivelatore** | descrivere l'operazione, contenuto obbligatorio | obbligo di legge | 10 anni |
| `canonical_document.*` (totali, date, riferimenti) | `canonical_document` | riferibili alla controparte | economico | documento fiscale | obbligo di legge | 10 anni |
| `transmission.payload_ref` | riferimento al carico trasmesso | contiene tutto quanto sopra | copia del documento | prova della trasmissione | obbligo di legge, interesse legittimo alla prova | 10 anni |
| `archive_record.*` | pacchetto in conservazione presso il fornitore qualificato | contiene tutto quanto sopra | documento conservato | conservazione a norma | **obbligo di legge — prevale sulla cancellazione** | 10 anni, non riducibili |
| `audit.attore` | colonne `created_by`, `updated_by`, registro delle trasmissioni | utente dell'account | identificativo interno | sapere chi ha trasmesso cosa all'autorità | interesse legittimo alla tracciabilità, obbligo di legge | quanto il documento |

**Esportazione e cancellazione.** Le tabelle che contengono dati di persone e che devono comparire **sia** in
`exportData` **sia** in `purgeData` del contratto `EinvoicingDataContract`: `legal_entity`, `counterparty`,
`canonical_document`, `document_line`, `transmission`, `lifecycle_event`, `validation_outcome` (perché cita
posizioni e valori del documento), `archive_record`. Dimenticarne una è il difetto di conformità più probabile.

**Il conflitto che va detto ad alta voce.** La cancellazione, sulla piattaforma, è **fisica**: sostituire i nomi
con dei codici non è cancellare. Ma un documento in conservazione a norma **non si può cancellare per dieci anni**:
c'è un obbligo di legge che prevale sul diritto alla cancellazione (ed è un caso previsto dalla norma stessa). La
risposta corretta non è cancellare di nascosto né rifiutare in blocco: è **cancellare tutto ciò che non è coperto
dall'obbligo** e **rispondere all'interessato dichiarando cosa resta, perché, e fino a quando**. È una decisione
di conformità che eccede questa app e che va portata al presidio trasversale; qui la segnalo come punto aperto
(§11, punto 3) e la lego alla storia `0026`.

**Testo libero.** Sì, e in un punto pericoloso: `document_line.descrizione`. È il varco delle categorie
particolari descritto nell'avviso in testa. L'app **non fa rilevazione di contenuto** — non legge le descrizioni
per classificarle — e il presidio resta la regola di validazione sul tipo di operazione più l'avvertenza in
interfaccia. Anche il campo note interne del documento, se lo si introduce, va trattato allo stesso modo.

**Integrazioni esterne.** Tutte e tre le integrazioni del §2.4 ricevono il documento **completo**, quindi dati
personali, e sono quindi **potenziali responsabili esterni del trattamento** da inserire nell'elenco dei fornitori
e nell'informativa: (a) il fornitore di trasmissione al Sistema di Interscambio, (b) il punto di accesso Peppol,
(c) il servizio di conservazione qualificato. Su tutti e tre va verificato che i dati stiano **a riposo in regioni
europee**: per i fornitori italiani ed europei citati è plausibile, ma non l'ho verificato contrattualmente e non
lo do per acquisito. Il fornitore di conservazione ha un problema in più: il rapporto dura dieci anni e sopravvive
alla fine dell'abbonamento del cliente.

**Classificazione della change.** **Sostanziale**, senza esitazione. L'app introduce finalità nuove (adempimento
fiscale, conservazione a norma), categorie di dati nuove, **tre responsabili esterni** e un trattamento con
durata decennale imposta dalla legge. Non c'è lettura onesta che la faccia rientrare fra i cambiamenti minori.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `list_documents` | `(stato?, periodo?, giurisdizione?, controparte?) → elenco minimizzato di documenti` | Restituisce numero, data, totale, stato del ciclo di vita e giurisdizione; **non** le righe né gli indirizzi | lettura | no |
| `get_document_status` | `(id) → stato, storia degli eventi, identificativo dell'autorità` | Risponde a «è andata?» con la cronologia delle notifiche | lettura | no |
| `explain_rejection` | `(id) → codice originale, spiegazione in lingua comune, rimedio proposto` | È lo strumento che giustifica l'app: traduce `00311` in una frase e in un'azione | lettura | no |
| `validate_before_send` | `(id | bozza) → esito, elenco delle violazioni con gravità` | Valida senza trasmettere; **non** ha effetti verso l'esterno | lettura (verifica senza effetti) | no |
| `list_overdue` | `(periodo?) → documenti fermi oltre la soglia, scadenze di mandato in avvicinamento` | Il «cosa richiede attenzione» | lettura | no |
| `get_vat_report` | `(periodo, giurisdizione) → riepilogo per aliquota` | Riepilogo, non dichiarazione | lettura | no |
| `create_document` | `(emittente, controparte, righe, giurisdizione) → bozza di documento` | Crea una **bozza** in stato `bozza`, mai trasmessa | scrittura | **sì** |
| `import_document` | `(file | riferimento) → bozza di documento` | Come sopra, partendo da un file | scrittura | **sì** |
| `fix_and_resubmit` | `(id, correzioni) → nuova bozza collegata allo scarto` | Prepara la correzione dopo uno scarto; **non** ritrasmette | scrittura | **sì** |
| `submit_to_authority` | `(id, conferma_esplicita) → esito della trasmissione` | **Manda il documento all'autorità fiscale o al destinatario. Non si disfa.** | scrittura irreversibile | **sì, obbligatoria, con doppio passaggio** |
| `archive_document` | `(id, conferma_esplicita) → riferimento del pacchetto di conservazione` | Avvia una conservazione decennale presso un fornitore qualificato: non si annulla | scrittura irreversibile | **sì, obbligatoria** |

**Lettura.** Gli strumenti che rendono questa app migliore delle sue concorrenti dalla chat sono i tre di
diagnosi — `get_document_status`, `explain_rejection`, `validate_before_send` — perché rispondono in un secondo
alle sole due domande che il cliente si pone davvero (§2.5). `submit_to_authority` è invece il caso di scuola
citato dal catalogo stesso (§8): la trasmissione a un'autorità fiscale è **l'esempio canonico** di azione che un
agente non deve poter eseguire da solo. Qui la conferma non è un formalismo: dopo l'accettazione del Sistema di
Interscambio la fattura **esiste giuridicamente** e l'unico rimedio è una nota di credito.

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine dell'epica l'app esiste, si avvia in locale, compare nella barra laterale a chi ha l'abbonamento, ha il
suo schema vuoto e la sua quota: non fa ancora nulla di utile, ma è accesa e non ha cablaggi a mano.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | Istanza di scaffolding, rotte `/api/einvoicing/v1/*`, infrastruttura dal modulo condiviso |
| [0002](01-fondamenta/0002-modello-dati-multi-account.md) | Modello dati multi-account | Schema `app_einvoicing`, prime tabelle con `tenant_id`, colonne di controllo e cancellazione logica |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Manifesto, registrazione, sezioni, cinque lingue, tema chiaro e scuro |
| [0004](01-fondamenta/0004-abbonamento-e-quota.md) | Abbonamento e quota | Proiezione dell'abilitazione, metrica `documenti` a consumo mensile, varco a `429`, modalità prova |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | Scoperta automatica del servizio, dati inventati, fornitori simulati in locale |

### Epica 02 — Anagrafiche fiscali e giurisdizioni

Alla fine dell'epica il cliente ha detto chi è, a chi fattura e in quali paesi, e l'app sa dove finirà ogni
documento e quando scattano gli obblighi che lo riguardano.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-anagrafiche-fiscali-e-giurisdizioni/0006-registro-delle-giurisdizioni.md) | Registro delle giurisdizioni | Profili di conformità versionati per paese: famiglia del modello, formato, canale |
| [0007](02-anagrafiche-fiscali-e-giurisdizioni/0007-soggetto-emittente.md) | Soggetto emittente | Anagrafica del soggetto fiscale che emette, con regime e giurisdizione |
| [0008](02-anagrafiche-fiscali-e-giurisdizioni/0008-controparti-e-recapito-elettronico.md) | Controparti e recapito elettronico | Anagrafica delle controparti con il recapito giusto per il loro paese |
| [0009](02-anagrafiche-fiscali-e-giurisdizioni/0009-verifica-del-recapito.md) | Verifica del recapito | Controllo di forma e di esistenza del recapito elettronico prima di emettere |
| [0010](02-anagrafiche-fiscali-e-giurisdizioni/0010-scadenze-dei-mandati.md) | Scadenze dei mandati | Avviso delle date di entrata in vigore che riguardano i soggetti dell'account |

### Epica 03 — Documento canonico e validazione

Alla fine dell'epica un documento entra nell'app da tre porte diverse, viene ricondotto a un unico modello e sa
dire, prima di partire, se è conforme e perché no.

| # | Storia | In una riga |
|---|---|---|
| [0011](03-documento-canonico-e-validazione/0011-documento-canonico.md) | Documento canonico | Modello interno allineato alla norma europea EN 16931, indipendente dal formato |
| [0012](03-documento-canonico-e-validazione/0012-ingresso-dai-documenti-di-billgrove.md) | Ingresso dei documenti dall'app di fatturazione | Ricezione a eventi dei documenti emessi altrove nella piattaforma |
| [0013](03-documento-canonico-e-validazione/0013-importazione-e-inserimento-manuale.md) | Importazione e inserimento manuale | Caricamento da file e modulo minimo per chi non ha una sorgente |
| [0014](03-documento-canonico-e-validazione/0014-motore-delle-regole-di-validazione.md) | Motore delle regole di validazione | Regole per giurisdizione, versionate, con gravità e riferimento normativo |
| [0015](03-documento-canonico-e-validazione/0015-diagnosi-degli-errori.md) | Diagnosi degli errori | Il codice dell'autorità tradotto in una frase comprensibile e in un rimedio |

### Epica 04 — Trasmissione e ciclo di vita legale

Alla fine dell'epica il documento parte davvero, su due famiglie di canale diverse, e l'app sa in ogni momento se
esiste giuridicamente, se è stato consegnato, se è stato rifiutato e cosa si fa dopo.

| # | Storia | In una riga |
|---|---|---|
| [0016](04-trasmissione-e-ciclo-di-vita-legale/0016-adattatore-di-giurisdizione.md) | Adattatore di giurisdizione | Il contratto che incapsula regole, serializzatore, canale e macchina a stati |
| [0017](04-trasmissione-e-ciclo-di-vita-legale/0017-trasmissione-con-clearance-italia.md) | Trasmissione a liberatoria (Italia) | Serializzazione FatturaPA e invio al Sistema di Interscambio tramite fornitore |
| [0018](04-trasmissione-e-ciclo-di-vita-legale/0018-trasmissione-su-rete-a-quattro-angoli.md) | Trasmissione a quattro angoli | Serializzazione UBL e consegna sulla rete Peppol tramite punto di accesso |
| [0019](04-trasmissione-e-ciclo-di-vita-legale/0019-ciclo-di-vita-e-notifiche.md) | Ciclo di vita e notifiche | Acquisizione delle notifiche e avanzamento della macchina a stati, in modo idempotente |
| [0020](04-trasmissione-e-ciclo-di-vita-legale/0020-scarti-e-rinvio-del-documento.md) | Scarti e rinvio del documento | Dallo scarto alla correzione al documento nuovo, con il legame tracciato |
| [0021](04-trasmissione-e-ciclo-di-vita-legale/0021-ricezione-dei-documenti-passivi.md) | Ricezione dei documenti passivi | I documenti che arrivano dai canali, riconosciuti e messi a disposizione |

### Epica 05 — Conservazione a norma e adempimenti

Alla fine dell'epica il documento è custodito per dieci anni in modo opponibile, si può esibire, e chi fa la
dichiarazione ha i numeri che gli servono.

| # | Storia | In una riga |
|---|---|---|
| [0022](05-conservazione-a-norma-e-adempimenti/0022-pacchetto-di-conservazione.md) | Pacchetto di conservazione | Composizione del pacchetto di versamento con i metadati obbligatori |
| [0023](05-conservazione-a-norma-e-adempimenti/0023-sigillo-e-marca-temporale.md) | Sigillo e marca temporale | Consegna al conservatore qualificato e ricevuta opponibile |
| [0024](05-conservazione-a-norma-e-adempimenti/0024-esibizione-e-scarico.md) | Esibizione e scarico | Ritrovare ed esibire un documento conservato, anche in blocco |
| [0025](05-conservazione-a-norma-e-adempimenti/0025-riepilogo-iva.md) | Riepilogo dell'imposta | Riepiloghi per periodo e giurisdizione per chi prepara la dichiarazione |
| [0026](05-conservazione-a-norma-e-adempimenti/0026-uscita-e-restituzione-dell-archivio.md) | Uscita e restituzione dell'archivio | Cosa succede all'archivio quando il cliente se ne va, e chi paga i dieci anni |

### Epica 06 — Esposizione conversazionale e prove end-to-end

Alla fine dell'epica ogni funzione è comandabile da una chat con le protezioni giuste, e il percorso completo
dell'app è coperto da una prova automatica registrata.

| # | Storia | In una riga |
|---|---|---|
| [0027](06-esposizione-conversazionale-e-prove/0027-strumenti-di-lettura.md) | Strumenti di lettura | Contratto degli strumenti che interrogano senza modificare nulla |
| [0028](06-esposizione-conversazionale-e-prove/0028-strumenti-di-scrittura-con-bozza.md) | Strumenti di scrittura con bozza | Creare, importare e correggere producendo sempre una bozza |
| [0029](06-esposizione-conversazionale-e-prove/0029-conferma-umana-per-la-trasmissione.md) | Conferma umana per la trasmissione | Il varco che impedisce a un agente di trasmettere all'autorità da solo |
| [0030](06-esposizione-conversazionale-e-prove/0030-percorso-end-to-end.md) | Percorso end-to-end | Il percorso `[J-EINVOICING]` e le voci del registro di copertura |

**Totale**: 6 epiche, 30 storie (`0001`–`0030`). Dentro la fascia raccomandata: 4-7 epiche, 4-8 storie per epica,
20-45 storie in tutto.

---

## 9. Estensioni della console di amministrazione

Servono estensioni, e per un motivo solo: questa app dipende da **tre fornitori esterni** e da **canali con
ciclo di vita asincrono**, quindi la domanda «perché al cliente non è partita la fattura?» non si risponde con la
scheda dell'account. Serve vedere lo stato delle connessioni per account, l'arretrato delle notifiche in attesa e
i documenti fermi in stato non definitivo — **come metadati e conteggi, mai come contenuti**. Serve inoltre una
forma di deroga di quota per la migrazione iniziale, quando un cliente carica tutto lo storico in un mese.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| **02 — BillGrove** (e, nel repository, l'app reale `fatture`) | **dipende da** | È la sorgente del documento. InvoiceGrove riceve i documenti emessi via eventi (storia `0012`), condivide l'anagrafica clienti e la catena del documento contabile |
| 03 — CashGrove | alimenta | Lo stato del ciclo di vita («consegnata», «scartata») è il presupposto dell'incasso: una fattura scartata non si sollecita |
| 06 — QuoteGrove | a monte, indirettamente | Fa parte della catena preventivo → ordine → fattura → incasso descritta nel catalogo §6; non si tocca direttamente |
| 18 — VaultGrove | si sovrappone in parte | Anche VaultGrove gestisce documenti e conservazione. Qui la conservazione è **fiscale e a norma**, con obblighi specifici; sarebbe un errore duplicare il motore |
| 04 — LeadGrove | condivide dati con | Anagrafica clienti condivisa (catalogo §6): la controparte fiscale e il contatto commerciale sono la stessa persona giuridica vista da due lati |
| 49 — ReconGrove | alimenta | Riconciliazione bancaria: il documento a norma è l'oggetto da riconciliare |

**Lettura — e qui affronto apertamente la nota del catalogo §6.** Il catalogo scrive: *«InvoiceGrove (1) non è un
prodotto autonomo difendibile: il trasporto e la conversione di formato sono commodity acquistabili a ~€0,18 a
fattura. Il valore sta nell'essere il sistema di origine del documento. Va quindi progettato come layer di
compliance di BillGrove (2), non come modulo separato.»*

**La accolgo nella sostanza e la declino diversamente nella forma, per tre ragioni verificabili.**

1. **La sostanza è confermata dalla mia ricerca, e con numeri peggiori di quelli del catalogo.** Il trasporto è
   davvero commodity: €0,074 a documento in Italia, €0,18–0,25 su Peppol (§2.2). E l'ancora di prezzo italiana è
   un prodotto completo a €12 al mese che include già la conformità (§2.1). Un'app che vendesse solo «converto e
   trasmetto» sarebbe morta in partenza. Su questo la nota ha ragione e non c'è da discutere.
2. **Ma «modulo di BillGrove» non è realizzabile su questa piattaforma**, e non per pigrizia: gli invarianti
   dicono che un'app è un servizio, uno schema, un modulo frontend, un listino e un abbonamento
   ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §2, §7, §8), e che **un'app non chiama un'altra app: l'unica
   via è asincrona a eventi**. «Strato di conformità di BillGrove» può quindi significare solo due cose: *(a)* la
   conformità entra dentro il servizio di BillGrove e InvoiceGrove come app **non esiste**; oppure *(b)*
   InvoiceGrove è un'app separata che **riceve i documenti di BillGrove per eventi** e non li crea. Non esiste una
   terza forma tecnica.
3. **Ho scelto (b), e l'ho scritto nella struttura, non solo nelle intenzioni.** L'app **non crea fatture** (§1,
   «Cosa NON fa»); la sua porta d'ingresso principale è una storia di ricezione a eventi (`0012`), non un modulo
   di inserimento; il modulo di inserimento manuale esiste, ma è dichiaratamente **minimo e di ripiego** (`0013`);
   il listino non ha piano gratuito perché l'app non è un punto d'ingresso al prodotto, è un componente aggiuntivo
   di chi già fattura; e l'indice non contiene una sola storia su listini, catalogo prodotti o solleciti.
   **Il motivo per cui non ho scelto (a)** — cioè non ho dissolto l'app dentro BillGrove — è che il ciclo di vita
   legale non è un dettaglio di serializzazione: sono tre macchine a stati diverse, un archivio decennale, tre
   fornitori esterni e un manifesto di dati con obblighi propri. Metterli dentro l'app di fatturazione
   significherebbe imporre a **tutti** i clienti di quella (compreso il forfettario che fattura in Italia e basta)
   il peso di un dominio che riguarda **alcuni**. Tenerli separati costa un abbonamento in più; fonderli costa la
   semplicità dell'app che oggi funziona.

**In una riga: l'app ha senso solo dentro la suite, mai da sola, e la struttura di questo documento lo riflette.**
La nota resta comunque un rischio vivo e non risolto — è la voce 1 della sezione 11 — perché la scelta fra (a) e
(b) è **direzione di prodotto**, e la direzione di prodotto non la decide un agente.

**Sovrapposizioni da evitare.** Con **BillGrove/`fatture`**: creazione documenti, listini, solleciti, incassi —
tutto di là, niente di qua. Con **VaultGrove (18)**: la gestione documentale generica e la conservazione fiscale
a norma sono due cose diverse; se un giorno VaultGrove implementa un motore di conservazione, va usato quello, non
duplicato. Con **CashGrove (03)**: gli stati di pagamento sono suoi; qui si tengono solo gli stati del **ciclo di
vita legale**, che sono un'altra cosa e vanno tenuti distinti anche nel nome.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **App separata o conformità dentro l'app di fatturazione?** (la nota del catalogo §6) | È direzione di prodotto. Ho argomentato la scelta «app separata alimentata a eventi» in §10 con ragioni tecniche verificabili, ma la decisione commerciale — vendere un secondo abbonamento a chi ha già il primo — non è di un agente | sviluppatore |
| 2 | **Prezzi, limiti e durata della prova** (§5), compresa la scelta di non avere un piano gratuito e di trasmettere solo con abbonamento attivo | Fermata di escalation di piattaforma; in più il piano `europa` regge solo con un costo Peppol negoziato che non ho potuto verificare (§2.7) | sviluppatore |
| 3 | **Cancellazione contro conservazione decennale** (§6) | Il diritto alla cancellazione e l'obbligo di custodia per dieci anni confliggono. La risposta ragionevole — cancellare ciò che non è coperto dall'obbligo e dichiarare cosa resta — eccede questa app e tocca il presidio trasversale di conformità | sviluppatore + presidio di conformità della piattaforma; storia `0026` |
| 4 | **Categorie particolari per via indiretta** (§6): il presidio è la regola di validazione sul divieto sanitario italiano | Regge se lo sviluppatore conferma la lettura «l'app non tratta dati dell'articolo 9». Se non la conferma, cambiano base giuridica, valutazione d'impatto e natura dell'app | sviluppatore; storia `0014` |
| 5 | **Tre responsabili esterni del trattamento** (trasmissione, punto di accesso, conservazione) e verifica che i dati stiano a riposo in Europa | Sono contratti, non codice. Nessuno dei tre l'ho verificato contrattualmente | sviluppatore; storie `0017`, `0018`, `0023` |
| 6 | **Quali giurisdizioni nella prima versione.** Ho proposto Italia più rete Peppol; Polonia, Francia, Germania e India restano fuori | Ogni giurisdizione in più è un adattatore completo, non una traduzione. La Francia in particolare **non è aggiungibile**: richiede l'immatricolazione di una piattaforma accreditata con certificazione ISO 27001 (§2.3), che è una decisione aziendale, non una storia | sviluppatore; contratto in `0016` |
| 7 | **Chi è il conservatore e cosa succede alla scadenza del rapporto** | Il rapporto dura dieci anni e sopravvive al cliente che disdice. Nessun fornitore esaminato pubblica un prezzo di giacenza (§2.7) | sviluppatore; storia `0026` |
| 8 | **Responsabilità in caso di scarto per difetto del fornitore** | Non ho trovato dichiarazioni chiare sui fornitori esaminati; è materia contrattuale e riguarda la promessa di vendita | sviluppatore |
| 9 | **Il livello conversazionale non esiste** nel repository (UC 0061-0066 scritti, non implementati) | Le storie `0027`-`0029` dichiarano il contratto degli strumenti dentro il servizio, ma non sono verificabili end-to-end finché la piattaforma non c'è | epica di piattaforma `12-ready-for-ai-mcp` |

**Rischi noti**

- **Il rischio di margine sulla metrica**, ed è il più insidioso perché è strutturale. La quota è un **consumo
  mensile** (`flow`), ma la conservazione è una **giacenza decennale**: un cliente che sta un anno e trasmette
  1.200 documenti ci lascia un obbligo di custodia lungo dieci anni e undici mesi dopo che ha smesso di pagare.
  Attenuazione: capire il costo di giacenza prima di pubblicare, e chiudere il punto 7 della tabella.
- **Rischio di calendario.** Il catalogo avverte (§8) che «le timeline normative sono mobili» e che i mandati di
  fatturazione elettronica hanno già subito rinvii, in Francia in particolare. La mia ricerca lo conferma: la
  Polonia ha spostato le micro-imprese a gennaio 2027 con sanzioni sospese fino ad allora. Attenuazione: non
  legare la vendita a una data singola; l'orizzonte europeo del 2030 rende l'app strutturale comunque.
- **Rischio di concorrenza dal basso.** Se i prodotti completi per paese (Fatture in Cloud e simili) aggiungono il
  multi-paese, lo spazio di mezzo si chiude. Attenuazione: l'integrazione nativa con la suite e il livello
  conversazionale — che è esattamente il differenziatore che il catalogo indica, con una finestra stimata di
  12-18 mesi.
- **Rischio di promessa eccessiva.** Vendere «conformità» significa assumersi una responsabilità che il software
  da solo non copre: la conformità dipende anche da cosa il cliente scrive nel documento. Attenuazione: il
  linguaggio del prodotto dice «valida, trasmette e conserva», non «ti mette in regola».
- **Dipendenza da fornitori terzi su un percorso che ha effetti giuridici.** Se il fornitore di trasmissione è
  fermo, il cliente non emette. Attenuazione: l'adattatore rende sostituibile il fornitore, ma la sostituzione
  richiede comunque un fermo. Va detto al cliente, non nascosto.

**Fuori dimensionamento**: non applicabile. 6 epiche, 4-6 storie ciascuna, 30 storie in tutto: dentro la fascia
raccomandata.
