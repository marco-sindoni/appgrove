# DeskGrove Support — descrizione dell'applicazione

**Numero di catalogo**: 12 · **Tipo**: orizzontale · assistenza clienti · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 12](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** DeskGrove Support raccoglie in un unico posto tutte le richieste che i clienti di un'azienda
mandano per farsi aiutare — messaggi di posta elettronica, moduli di contatto del sito, messaggi WhatsApp — e le
trasforma in **richieste di assistenza tracciate**: ognuna ha un filo di conversazione, uno stato, un responsabile,
un tempo di risposta atteso e una storia. Attorno alla richiesta l'app mette gli strumenti che fanno risparmiare
minuti ogni giorno: code di lavoro, risposte già scritte, una base di conoscenza consultabile mentre si risponde e
un portale dove il cliente vede a che punto è la sua richiesta senza dover riscrivere.

**Per chi.** Micro-imprese (1-10 addetti) e piccole imprese (10-50) che ricevono richieste di assistenza da clienti
esterni e oggi le gestiscono su una casella di posta condivisa. Chi compra è il titolare o il responsabile del
servizio clienti; chi usa l'app tutti i giorni sono da una a dieci persone che rispondono. Mercato globale con
priorità europea. Il cliente tipo è il negozio online con tre persone che rispondono ai clienti, lo studio di
servizi che riceve richieste dai propri assistiti, l'azienda di software con due persone in assistenza.

**Quale problema toglie.** Oggi la casella `info@` o `assistenza@` è condivisa fra tre persone. Nessuno sa chi ha
già risposto, due persone rispondono la stessa cosa, un messaggio resta senza risposta per quattro giorni e nessuno
se ne accorge finché il cliente non telefona arrabbiato. Non esiste una risposta alla domanda «quanto ci mettiamo a
rispondere?», perché il dato non c'è. Quando la persona che seguiva un cliente è in ferie, la storia della
conversazione è nella sua casella e non è recuperabile. Il rimedio comune — comprare Zendesk o Freshdesk — costa
più di quanto un'azienda di dieci persone voglia spendere e porta con sé un impianto pensato per un centro
assistenza da cinquanta operatori: regole di instradamento, competenze, turni, livelli di supporto. La ricerca in
rete conferma che è la lamentela ricorrente del segmento (§2.5).

**Cosa NON fa.**

- **Non è un centralino telefonico**: niente chiamate in ingresso, niente registrazioni, niente code telefoniche.
- **Non è una chat dal vivo sul sito**: il modulo di contatto genera una richiesta, non apre una finestra di chat
  con l'operatore in attesa. La chat sincrona ha requisiti (presenza, tempi di reazione in secondi) che un'azienda
  di dieci persone non riesce a sostenere e che porterebbero l'app in un mercato diverso.
- **Non è un sistema di gestione dei guasti informatici interni** (richieste dei dipendenti, inventario delle
  postazioni, cambi di configurazione): il richiedente qui è **esterno**, è un cliente dell'azienda.
- **Non è il sistema di assistenza della piattaforma appgrove.** Quello esiste già ed è un'altra cosa: vedi §10.
- **Non fa vendita conversazionale su WhatsApp** (catalogo, cataloghi in chat, ordini, pagamenti): quello è
  ChatGrove, app 5 del catalogo.
- **Non analizza automaticamente il contenuto dei messaggi** con servizi esterni per classificarli o dedurne il
  sentimento: sarebbe un responsabile del trattamento in più su contenuto altrui delicato.

**Rischio di sostituzione da parte dei modelli linguistici.** `rafforzata`. Un assistente generico sa scrivere una
risposta cortese, ma non sa *quale* richiesta sta scoperta da tre giorni, *chi* l'ha presa in carico, *che cosa* si
era detto a quel cliente sei mesi fa e *quale* accordo di servizio sta per essere violato. Il valore sta nello
storico proprietario e nel flusso di lavoro, non nel testo. Anzi: è una delle app dove l'esposizione conversazionale
aumenta di più il valore, perché «riassumi le richieste in ritardo e preparami le risposte» è esattamente il lavoro
che l'operatore fa a mano. Con un vincolo netto: la risposta **esce solo dopo che una persona l'ha approvata** (§7).

---

## 2. Mercato e analisi in rete

> Compilata dopo **otto ricerche mirate** ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4).
> Ciò che non è stato trovato è **dichiarato**, non colmato a intuito.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| Freshdesk (Freshworks) | India/globale, forte in Europa | Ticketing multicanale, base di conoscenza, accordi di servizio, portale clienti | **19 $/operatore/mese** (Growth), 55 $ (Pro), 89 $ (Enterprise), fatturazione annuale; prova 14 giorni | [freshworks.com/freshdesk/pricing](https://www.freshworks.com/freshdesk/pricing/) |
| Zoho Desk | India/globale | Ticketing multicanale, base di conoscenza, indagini di soddisfazione, portale | Piano **gratuito fino a 3 utenti**; Express, Standard, Professional, Enterprise a listino crescente; prova 15 giorni senza carta | [zoho.com/desk/pricing.html](https://www.zoho.com/desk/pricing.html) |
| Help Scout | Stati Uniti | Casella condivisa orientata alla posta elettronica, base di conoscenza, accordi di servizio semplici | Piano **gratuito 5 utenti / 1 casella**; Standard **25 $/utente/mese**, Plus 45 $, Pro 75 $; prova 15 giorni senza carta | [helpscout.com/pricing](https://www.helpscout.com/pricing/) |
| Zammad | Germania (Berlino) | Sistema di assistenza libero e installabile in proprio; versione in cloud gestita | Community gratuita e illimitata se installata in proprio; cloud gestito indicato a **7-27 €/operatore/mese** | [eurotoolkit.eu — alternative europee a Zendesk](https://www.eurotoolkit.eu/blog/european-alternatives-zendesk) |
| LiveAgent | Slovacchia | Ticketing, chat, centralino, base di conoscenza, dati ospitati in Unione Europea | Prezzo non rilevato su pagina ufficiale in questa analisi | [european-saas.eu — alternative europee a Zendesk](https://www.european-saas.eu/blog/zendesk-vs-european-customer-support-alternatives) |

**Lettura.** Il mercato è affollato e maturo, ma è affollato **sopra**: i prodotti forti sono costruiti per squadre
di assistenza vere e il loro prezzo cresce per operatore, con le funzioni che servono davvero (base di conoscenza,
soddisfazione, risposte predefinite) spesso spostate sul secondo o terzo piano. Sotto restano due sole strade: la
casella di posta condivisa (gratuita e senza memoria) e il prodotto libero da installare in proprio (Zammad,
FreeScout), che richiede un server e qualcuno che lo tenga in piedi. Lo spazio difendibile per DeskGrove è
**quello di mezzo**: le cinque cose che servono davvero a chi ha da uno a dieci operatori, a prezzo per account
invece che per operatore, dentro una suite dove l'anagrafica del cliente è già lì, con i dati trattati in Unione
Europea. La differenziazione **non** è la lunghezza dell'elenco di funzioni: è la sottrazione.

### 2.2 Prezzi praticati nel dominio

**Rilevato su pagine ufficiali.**

- **Unità di misura prevalente: per operatore, al mese** (Freshdesk, Help Scout, Zoho Desk). Non per richiesta, non
  per contatto. Chi ha provato a vendere per numero di ticket è l'eccezione, non la regola.
- **Fascia d'ingresso**: 19 $/operatore/mese Freshdesk (annuale), 25 $/utente/mese Help Scout Standard. Zoho Desk è
  più basso ma pubblica il listino in valuta locale sulla pagina che ho raggiunto (vedi §2.7).
- **Piano gratuito**: esiste ed è la norma nel segmento micro — Zoho Desk fino a 3 utenti, Help Scout fino a 5
  utenti con una sola casella. È il modo con cui questi prodotti entrano nelle aziende piccole.
- **Prova gratuita**: 14 giorni Freshdesk, 15 giorni Zoho Desk e Help Scout, questi ultimi **senza carta di
  credito**.
- **Sconto annuale**: la fatturazione mensile costa circa il 20-50% in più di quella annuale a seconda del
  fornitore. Il «due mesi in regalo» della piattaforma appgrove è quindi in linea, anzi conservativo.
- **Supplementi per l'intelligenza artificiale**: sono voci a parte e costose (Freshdesk chiede 29 $/operatore/mese
  per l'assistente e vende a consumo le sessioni dell'agente automatico; Help Scout chiede 0,75 $ per risposta
  risolta automaticamente). È un'informazione di posizionamento importante: in appgrove l'esposizione
  conversazionale è di piattaforma e **non** è un supplemento di questa app.

**Da siti di comparazione, non da pagina ufficiale** (da leggere come ordine di grandezza): Zoho Desk a 7 / 14 / 23 /
40 $ per operatore/mese sui quattro piani a pagamento con fatturazione annuale, e circa il 30-50% in più al mese.

La fascia dichiarata dalla scheda di catalogo — **12-25 €/operatore/mese** — è **confermata** dai rilievi ufficiali.

### 2.3 Obblighi normativi del settore

Il dominio dell'assistenza clienti **non è un settore regolato** come la fatturazione elettronica o la sicurezza sul
lavoro: non esistono albi, licenze o formati obbligatori. L'obbligo pesante è un altro, ed è di protezione dei dati.

- **Ruolo di responsabile del trattamento.** I dati che entrano in DeskGrove sono i dati dei **clienti finali
  dell'azienda cliente**: nomi, indirizzi di posta, numeri di telefono e — soprattutto — il contenuto libero di quello
  che scrivono. Su quei dati il **titolare del trattamento è l'azienda cliente** e appgrove è **responsabile del
  trattamento** che agisce per suo conto. È una differenza che cambia tutto rispetto alle app che trattano dati del
  cliente stesso: servono un contratto di nomina a responsabile, l'elenco dei sotto-responsabili (chi consegna la
  posta, chi consegna i messaggi WhatsApp), l'impegno a cancellare o restituire i dati alla fine del contratto e la
  regola per cui appgrove **non** usa quei dati per finalità proprie. Le fonti consultate (§2.6) sono concordi nel
  descrivere questa doppia veste come il punto in cui la maggior parte dei fornitori di software sbaglia.
- **Conservazione.** Non esiste un termine di legge per «una conversazione di assistenza». Il criterio è la
  minimizzazione: si tiene per il tempo necessario a fornire il servizio e a difendersi da una contestazione, e il
  termine lo fissa il **titolare**, cioè il cliente. Ne discende un requisito di prodotto preciso: la durata di
  conservazione dev'essere **un parametro dell'account**, non una costante scritta nel codice (storia `0036`).
- **Diritti dell'interessato di secondo livello.** Se un cliente finale chiede all'azienda la cancellazione dei
  propri dati, l'azienda deve poterla eseguire **dentro DeskGrove**, da sola, senza aprire una richiesta ad appgrove.
  È il motivo per cui la cancellazione per singolo richiedente è una funzione dell'app e non solo uno strumento di
  piattaforma.
- **WhatsApp**: usare il canale WhatsApp per l'assistenza significa passare dalla piattaforma di Meta e da un
  fornitore intermedio. Sono **sotto-responsabili** aggiuntivi con trasferimento di dati fuori dall'Unione Europea, e
  richiedono una decisione dello sviluppatore (§6, §11).

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta prevedibile per il segmento:

1. **Casella di posta elettronica aziendale** — indispensabile: è da lì che arrivano oggi le richieste. Introduce
   la gestione di un dominio e di un recapito per conto del cliente. *(Fornitore già presente nella piattaforma:
   il servizio di invio posta di appgrove.)*
2. **Modulo di contatto sul sito del cliente** — un frammento da incollare; nessun fornitore esterno nuovo.
3. **Anagrafica clienti condivisa della suite** (LeadGrove, app 4) — «chi è questo che scrive?». Non è
   un'integrazione esterna: è la sinergia interna descritta al §10.
4. **WhatsApp Business** — molto richiesto nel mercato italiano e in quello latino-americano. ⚠️ **Introduce
   sotto-responsabili esterni** (Meta e un fornitore intermedio) e trasferimento fuori dall'Unione Europea.
5. **Fatturazione e ordini** (BillGrove, app 2) — la richiesta «dov'è la mia fattura» ha bisogno del riferimento al
   documento, non di una copia dei dati contabili.
6. **Recensioni e moduli di soddisfazione esterni** — richiesti ma **fuori ambito**: l'indagine di soddisfazione è
   nativa (storia `0027`) proprio per non aggiungere un fornitore.

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Quello che chiedono, in ordine:

- **sapere chi risponde a cosa** — evitare la doppia risposta e il messaggio dimenticato;
- **rispondere in fretta senza riscrivere ogni volta** — risposte predefinite e articoli da inserire nel messaggio;
- **un numero solo che dica se si sta rispondendo in tempo** — non un cruscotto con dodici grafici;
- **che il cliente finale non debba registrarsi** per seguire la propria richiesta.

Quello che **rifiutano** — ed è più istruttivo dell'elenco delle funzioni. Le fonti consultate convergono su un
punto: *uno strumento che funziona benissimo per un reparto di cinquanta persone travolge una squadra di tre*.
Le funzioni che il segmento micro non usa e che gli costano complessità sono l'instradamento per competenza, i
livelli di supporto, i turni, i flussi di automazione a più rami, l'analisi avanzata e le integrazioni d'impresa.
La conclusione di prodotto è **il contrario di una lista della spesa**: DeskGrove ha una sola coda in più
dell'ovvio (le code, storia `0019`), una sola politica di servizio per account (storia `0024`) e nessun motore di
regole. Se il cliente cresce fino ad avere bisogno di quelle cose, ha comprato il prodotto sbagliato — ed è una
frase da scrivere sulla pagina del prodotto, non da nascondere.

**Aspettative sui tempi di risposta** (rilevate, ma da fonti di parte — vedi §2.7): le rilevazioni ricorrenti del
settore indicano che circa il **46% dei clienti si aspetta una risposta alla posta elettronica entro 4 ore** e che il
tempo medio effettivo di prima risposta nel mercato si colloca fra le **7 e le 12 ore**. Il numero preciso non è
affidabile; la forma sì, ed è quella che conta per il prodotto: **l'aspettativa si misura in ore, non in minuti**.
Da qui discendono due scelte di progetto: gli accordi di servizio si esprimono in **ore lavorative** e non in
minuti (storia `0024`), e l'orologio si **ferma** quando la palla è al cliente (storia `0025`) — altrimenti ogni
misura è falsa e nessuno la guarda più.

### 2.6 Fonti consultate

1. **Freshdesk — pagina prezzi ufficiale** — https://www.freshworks.com/freshdesk/pricing/ — tre piani a 19 / 55 /
   89 $ per operatore al mese con fatturazione annuale, prova di 14 giorni con tutte le funzioni, portale clienti e
   base di conoscenza presenti su tutti i piani, più politiche di servizio multiple solo dai piani alti. È il
   riferimento principale della fascia di prezzo.
2. **Zoho Desk — pagina prezzi ufficiale** — https://www.zoho.com/desk/pricing.html — piano gratuito fino a tre
   utenti, prova di 15 giorni senza carta di credito, e soprattutto la **ripartizione delle funzioni per piano**:
   base di conoscenza, valutazioni di soddisfazione e risposte predefinite non sono nel piano d'ingresso. Ne ho
   ricavato che quelle tre funzioni sono percepite come «da piano superiore» e che metterle nel piano base è una
   scelta di posizionamento consapevole.
3. **Help Scout — pagina prezzi ufficiale** — https://www.helpscout.com/pricing/ — piano gratuito con 5 utenti e
   **una sola casella**, poi 25 / 45 / 75 $ per utente al mese. Ne ho ricavato il modello di limitazione del piano
   gratuito (non il numero di richieste ma il numero di caselle e di risposte predefinite) e la conferma che gli
   accordi di servizio sono considerati una funzione di livello base.
4. **Meta — documentazione ufficiale dei prezzi della piattaforma WhatsApp Business** —
   https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing — i messaggi di **servizio**
   avviati dal cliente sono **gratuiti** dentro la finestra di 24 ore dall'ultimo messaggio dell'utente; fuori da
   quella finestra si possono mandare solo modelli approvati, a pagamento. È il vincolo che disegna la storia `0017`:
   l'assistenza sta quasi tutta dentro la finestra gratuita, e il caso «rispondo dopo due giorni» va progettato a
   parte.
5. **Alternative europee a Zendesk — EuroToolKit** — https://www.eurotoolkit.eu/blog/european-alternatives-zendesk —
   Zammad (Berlino) come prodotto libero completo, con cloud gestito a 7-27 €/operatore/mese e giurisdizione
   tedesca. Ne ho ricavato il limite inferiore realistico del prezzo e la conferma che «dati in Unione Europea» è
   già un argomento di vendita presidiato, non un'esclusiva.
6. **Zendesk contro le alternative europee — European SaaS** —
   https://www.european-saas.eu/blog/zendesk-vs-european-customer-support-alternatives — LiveAgent (Slovacchia) come
   alternativa europea completa. Fonte di parte (vende il tema della sovranità dei dati), usata solo per la mappa
   dei concorrenti europei, non per i prezzi.
7. **Il miglior software di assistenza per le piccole imprese — rassegne di settore** —
   https://www.featurebase.app/blog/best-help-desk-software-for-small-business e
   https://supportbee.com/blog/shared-inbox-vs-help-desk-which-does-your-team-need — la lamentela ricorrente del
   segmento: gli strumenti d'impresa «diventano rapidamente complicati e costosi» per le piccole imprese, e uno
   strumento pensato per un reparto di cinquanta persone travolge una squadra di tre. È la fonte del §2.5 e della
   scelta di sottrarre funzioni invece di aggiungerne.
8. **Confronti sui tempi di prima risposta** — https://livechatai.com/blog/customer-support-response-time-statistics
   e https://www.fullview.io/blog/first-response-time — aspettativa entro le 4 ore per circa il 46% dei clienti,
   media di mercato fra 7 e 12 ore. Fonti di parte e non verificabili alla sorgente: usate per la **forma** del
   requisito (ore lavorative), non come dato da citare al cliente.
9. **Conformità al Regolamento europeo per i fornitori di software come servizio** —
   https://secureprivacy.ai/blog/data-processing-agreements-dpas-for-saas e
   https://drata.com/learn/gdpr/for-saas-compliance — la doppia veste titolare/responsabile e l'obbligo di dichiarare
   i sotto-responsabili. Fonti divulgative, non normative: usate per impostare le domande del §6, non come parere
   legale.

### 2.7 Cosa NON sono riuscito a determinare

- **Prezzo di Zoho Desk in euro.** La pagina ufficiale raggiunta pubblica il listino in rupie indiane e la variante
  europea dell'indirizzo restituisce un errore. I valori in dollari che circolano (7 / 14 / 23 / 40 per operatore al
  mese) vengono da siti di comparazione, non dal fornitore. *Cosa servirebbe*: aprire la pagina dei prezzi con
  indirizzo di rete europeo e valuta euro prima di fissare il posizionamento.
- **Prezzo ufficiale di LiveAgent** — non rilevato su pagina del fornitore in questa analisi. È l'unico concorrente
  europeo di rilievo diretto: prima di decidere il listino va verificato.
- **Concorrenti italiani specifici.** Non ho trovato un prodotto italiano di assistenza clienti con listino pubblico
  rivolto alle micro-imprese: la ricerca in lingua italiana restituisce elenchi di comparazione e fornitori
  internazionali. *Cosa servirebbe*: una ricerca sui portali di comparazione italiani con verifica sui siti dei
  fornitori. Non escludo che esistano; **non li ho trovati e non li invento**.
- **Quante richieste al mese riceve davvero un'azienda di dieci persone.** Non ho trovato un dato affidabile. È
  rilevante perché determina se la metrica di quota può restare sui posti operatore o se serve un limite di volume
  (§5, §11): oggi la proposta si basa sul comportamento del mercato (si vende per operatore), non su una misura.
- **Il costo effettivo del canale WhatsApp per un'azienda piccola.** So che i messaggi di servizio dentro le 24 ore
  sono gratuiti (fonte ufficiale), ma il ricarico del fornitore intermedio e il costo dei modelli fuori finestra
  dipendono dal contratto e dal paese. Non è determinabile prima di scegliere il fornitore.

---

## 3. Varco d'identità — le risposte pronte per `new-application`

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `helpdesk` | Rispetta `^[a-z][a-z0-9_]{0,30}$`. **Non** `deskgrove`: l'identificativo finisce nello schema del database (`app_helpdesk`), nella rotta pubblica (`/api/helpdesk/v1/*`), nei nomi delle code e nell'istanza del modulo di infrastruttura — cambiarlo dopo è una migrazione di dati, non una rinomina. «DeskGrove» è il nome commerciale di oggi e può cambiare; «assistenza clienti» è ciò che l'app **è**. Segue la convenzione delle due app reali del repository, che si chiamano `fatture` e `crm` e non con un nome di marca. Nessuna collisione con l'assistenza interna della piattaforma, che vive nel servizio `core` sotto lo schema `platform` e si chiama `support` (§10). |
| **Modello utente** | `multi` | L'app **è** il lavoro di più persone sulla stessa coda: senza «chi ha preso in carico», «chi ha risposto» e «assegnato a», il prodotto non esiste — è di nuovo la casella condivisa. Il numero di persone che rispondono è anche la metrica di quota. Un modello a utente singolo renderebbe impossibile la ragione stessa per cui il cliente compra. |
| **Porta locale** | `8112` | Convenzione del kit: 8100 + numero di catalogo (12). Da confermare con `./dev.sh services` al momento dello scaffolding. |
| **Metrica di quota** | `agents` (posti operatore) | È l'unità con cui **tutto il mercato** vende (§2.2: Freshdesk, Zoho Desk, Help Scout, Zammad, tutti per operatore) ed è ciò che cresce insieme al valore ricevuto: più persone rispondono, più l'azienda si appoggia allo strumento. Un posto operatore è un utente dell'account a cui è stato dato il diritto di lavorare sulle richieste — **non** coincide con il numero di utenti dell'account: chi guarda soltanto i rapporti non consuma un posto. Alternative scartate: *richieste al mese* (punisce il cliente proprio quando ha più bisogno dello strumento — un picco di reclami è il giorno peggiore per bloccarlo, e favorisce il comportamento sbagliato: non tracciare le richieste per non consumare quota); *contatti in anagrafica* (cresce da sola, non è governabile dal cliente). |
| **Natura della metrica** | `stock` | È un tetto su ciò che esiste **ora**: «tre posti operatore» significa che per far entrare una quarta persona nella coda bisogna toglierne una. Non si azzera il primo del mese, perché non è un consumo: l'operatore c'è o non c'è. Conseguenza già prevista dalla piattaforma: il passaggio a un piano inferiore è **bloccato** finché i posti occupati superano il tetto del piano di destinazione. |
| **Colore-categoria e icona** | `teal` · icona `life-buoy` (salvagente) | L'assistenza è un'area di **servizio**, distinta dalle vendite (dove sta LeadGrove) e dai denari (fatturazione e incassi): il verde acqua la separa a colpo d'occhio nella barra laterale quando il cliente ha attive più app della suite. L'icona a salvagente dice «qui si viene aiutati» senza bisogno di leggere. Lo stesso valore va nel listino (`category: teal`) e nel manifesto del modulo frontend (`accentToken`): devono coincidere. |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Ticket` (richiesta) | Una conversazione con un cliente finale, dall'apertura alla chiusura | numero progressivo per account, oggetto, stato, priorità, coda, operatore assegnato, canale d'ingresso, richiedente, data di apertura, data della prima risposta, data di chiusura, scadenze di servizio | sì — indirettamente: è legata al richiedente, e l'oggetto è testo libero |
| `TicketMessage` (messaggio) | Una riga del filo: messaggio del cliente, risposta dell'operatore o **nota interna** | verso (in ingresso / in uscita / interno), autore, corpo, data, identificativo del messaggio di posta d'origine | **sì — il cuore del problema**: testo libero scritto da chiunque |
| `Requester` (richiedente) | La persona che chiede assistenza: un cliente finale dell'azienda, **non** un utente di appgrove | nome, indirizzo di posta, numero di telefono, lingua preferita, riferimento all'anagrafica condivisa quando esiste | **sì** |
| `Channel` (canale) | Un recapito da cui entrano le richieste | tipo (posta, modulo web, WhatsApp), indirizzo o identificativo, stato della connessione, coda di destinazione | no (configurazione), ma custodisce credenziali |
| `Queue` (coda) | Un raggruppamento di lavoro: «Assistenza», «Amministrazione» | nome, colore, operatori che la presidiano, politica di servizio applicata | no |
| `Agent` (operatore) | Un utente dell'account abilitato a lavorare sulle richieste — **occupa un posto** | riferimento all'utente, nome visibile, firma, stato attivo/sospeso | sì — dati del dipendente del cliente |
| `ServicePolicy` (politica di servizio) | I tempi promessi: prima risposta e risoluzione, per priorità | obiettivi in ore lavorative per priorità, calendario di riferimento | no |
| `BusinessCalendar` (orario di servizio) | Quando l'azienda risponde | fasce orarie per giorno, fuso, giorni di chiusura | no |
| `CannedResponse` (risposta predefinita) | Un testo pronto con segnaposto | titolo, corpo, lingua, segnaposto ammessi | no (ma il corpo è testo libero: vedi §6) |
| `KnowledgeArticle` (articolo) | Una risposta pubblicabile alla domanda ricorrente | titolo, corpo, stato bozza/pubblicato, categoria, lingua, conteggio delle visualizzazioni | no |
| `Attachment` (allegato) | Un file arrivato o inviato con un messaggio | nome, tipo, dimensione, riferimento all'archivio, data | **sì, potenzialmente**: un allegato può contenere qualsiasi cosa |
| `SatisfactionSurvey` (soddisfazione) | La valutazione del richiedente dopo la chiusura | voto, commento libero, data, gettone monouso | **sì** — il commento è testo libero |

**Relazioni.** Un `Channel` alimenta una `Queue`; una `Queue` raccoglie `Ticket`; un `Ticket` appartiene a un
`Requester`, è assegnato a un `Agent` e contiene molti `TicketMessage`, ciascuno con zero o più `Attachment`; alla
chiusura genera al più una `SatisfactionSurvey`. Le `ServicePolicy` si applicano alla coda e producono, su ogni
richiesta, due scadenze calcolate sul `BusinessCalendar`.

**Macchina a stati della richiesta** — la parte che tutte le storie devono rispettare:

```
        (apertura da canale)
                 │
                 ▼
    ┌────────► aperta ──────────┐
    │            │              │
    │      (presa in carico)    │ (risposta al cliente)
    │            ▼              ▼
    │      in lavorazione ─► in attesa del cliente
    │            │              │
    │            │        (il cliente replica)
    │            │              │
    │            └──────────────┘
    │            ▼
    │        risolta ──(7 giorni senza repliche)──► chiusa
    │            │                                    │
    └──── (il cliente replica: riapertura) ◄──────────┘
```

Regole che valgono ovunque: l'**orologio della prima risposta** si ferma al primo messaggio in uscita; l'orologio
della risoluzione **si mette in pausa** in `in attesa del cliente` (è la regola che rende onesta la misura, §2.5);
una richiesta `chiusa` non si modifica più — una replica del cliente **riapre** creando il collegamento con la
richiesta precedente, non riscrivendo la vecchia.

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica
(`deleted_at`); schema `app_helpdesk`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8).

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il
> file `services/core/src/main/resources/pricing/helpdesk.yaml`.

**Ragionamento.** Il mercato vende per operatore fra 19 e 25 dollari al mese sui piani d'ingresso (§2.2, rilievi
ufficiali). La piattaforma appgrove vende invece **per account con un tetto di posti**: il confronto onesto si fa
quindi sul prezzo effettivo per operatore a piano pieno. La proposta punta a stare **sotto la metà** del prezzo
d'ingresso dei concorrenti internazionali, che è l'unica ragione per cui un'azienda di dieci persone cambia
strumento, restando **sopra** il prodotto libero installato in proprio, che costa un server e una persona che lo
tenga in piedi. Non ci sono costi variabili per richiesta: la posta elettronica passa dall'infrastruttura già
presente, e i messaggi di assistenza su WhatsApp dentro la finestra di 24 ore sono gratuiti alla fonte (§2.6,
fonte 4) — il canale WhatsApp, quando arriverà, potrà quindi stare nel piano superiore **senza** un addebito a
consumo, coerentemente con il divieto di piattaforma di far pagare lo sforamento.

| Piano | Prezzo mensile | Prezzo annuale | Limite sulla metrica `agents` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | **1 posto operatore** | — | La persona sola che vuole smettere di usare la casella condivisa. Abbastanza per vedere il valore su un canale solo, non abbastanza per farci lavorare una squadra. |
| `team` | **24 €** | **240 €** (= 10× il mensile, «due mesi in regalo») | **3 posti operatore** | 14 giorni | L'azienda micro con due o tre persone che rispondono. Prezzo effettivo a piano pieno: **8 €/operatore/mese**, un terzo di Freshdesk Growth. |
| `business` | **59 €** | **590 €** | **10 posti operatore** | 14 giorni | La piccola impresa con una squadra di assistenza vera. Prezzo effettivo a piano pieno: **5,90 €/operatore/mese**. |

**Note obbligate.**

- **Due piani a pagamento più uno gratuito**: aggiungerne è facile, toglierne quando qualcuno ci sta sopra è
  difficile. Sopra i dieci operatori il cliente è fuori dal perimetro dichiarato al §1 e va accompagnato altrove,
  non trattenuto con un quarto piano.
- **Nessun limite lasciato vuoto**: tutti e tre i piani hanno un tetto esplicito sui posti. Un limite vuoto
  significherebbe *illimitato*, non zero, e su una metrica a giacenza sarebbe un errore costoso.
- **La prova gratuita di 14 giorni convive con il piano gratuito e non è ridondante**, qui: il piano gratuito è
  limitato in *posti* (una persona sola), mentre il valore che il cliente deve poter provare è proprio il lavoro
  **in squadra** sulla stessa coda. Senza prova, non può vederlo. Carta di credito richiesta all'inizio, secondo la
  regola della piattaforma — annotando però che i due concorrenti che offrono la prova senza carta (Zoho Desk, Help
  Scout) lo usano come argomento di vendita.
- **Costo effettivo dell'incasso**: nessun piano proposto sta sotto i 5 €/mese, quindi la parte fissa per
  transazione non è un problema. L'annuale va comunque spinto: dimezza il numero di transazioni.
- **Cosa distingue i piani oltre ai posti**: la proposta è di **non** spostare funzioni fra i piani (base di
  conoscenza, accordi di servizio, soddisfazione, risposte predefinite stanno **in tutti**), al contrario di Zoho
  Desk (§2.6, fonte 2). Motivo: quelle funzioni sono la ragione per cui il cliente lascia la casella condivisa;
  toglierle dal piano d'ingresso lo lascia con un prodotto che non risolve il suo problema. La sola eccezione
  proposta è il **canale WhatsApp**, riservato a `business` perché comporta una configurazione assistita e un
  sotto-responsabile in più. ⚠️ Anche questa è una decisione dello sviluppatore, non dell'agente.
- I prezzi sono **immutabili una volta vivi**: un cambio di prezzo si fa creando un prezzo nuovo, non modificando
  quello esistente.
- **Punto aperto sul volume** (§2.7, §11): la metrica a posti non pone alcun limite al numero di richieste e di
  allegati conservati. Se un cliente da tre posti accumulasse centomila conversazioni con allegati, il costo di
  archiviazione crescerebbe senza contropartita. Il rimedio proposto **non** è una seconda metrica di quota — ne è
  ammessa una sola — ma la **durata di conservazione come parametro del piano** (storia `0036`). Da confermare.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/helpdesk.yaml`) si compila **insieme** allo sviluppatore: «niente contratto, niente
> produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

> 🛑 **Attenzione — via d'ingresso non presidiata per categorie particolari (articolo 9).**
> Questa app **non chiede** dati sanitari, biometrici, genetici, opinioni politiche, convinzioni religiose,
> orientamento sessuale o appartenenza sindacale: nessun campo del modello di dominio li prevede. Ma il corpo di un
> messaggio è **testo libero scritto da una persona che non è nostra utente, che non ha letto la nostra informativa
> e a cui nessuno può impedire di scrivere quello che vuole**. Un cliente finale che scrive «non sono venuto in
> negozio perché sono in chemioterapia», «il vostro prodotto non è compatibile con la mia dieta religiosa» o «vi
> scrivo su indicazione del mio sindacato» ha **appena fatto entrare un dato dell'articolo 9** in una tabella che
> non era stata progettata per riceverlo. Lo stesso vale per gli **allegati** (un certificato medico allegato a un
> reclamo) e per il **commento libero dell'indagine di soddisfazione**.
>
> È una differenza di natura rispetto alle altre app del catalogo: qui il canale è **aperto al pubblico** e la
> quantità di richieste rende impossibile la revisione manuale. Non è un rischio che si possa chiudere con una
> validazione di campo, e **non va ammorbidito** per far sembrare l'app più semplice.
>
> **Cosa si propone** — da validare, punto per punto, con lo sviluppatore:
> 1. **Non chiedere mai** dati particolari: nessun campo strutturato, nessun elenco a tendina che li introduca.
> 2. **Segnalare, non classificare**: riusare il riconoscitore deterministico a radici di parole già scritto per
>    l'assistenza interna della piattaforma (`SpecialCategoryScreening`, §10) per marcare la richiesta come «da
>    guardare con attenzione», **senza registrare quale** categoria sarebbe stata riconosciuta. Un contrassegno
>    booleano non è una classificazione e non crea un nuovo dato particolare.
> 3. **Mai un servizio esterno di analisi del testo** per fare questo lavoro: sarebbe un sotto-responsabile in più
>    proprio sul contenuto più delicato. È già la regola scritta per l'assistenza interna, e qui vale a maggior
>    ragione perché i dati sono di terzi.
> 4. **Conservazione breve e governata dal cliente**: è la sola misura che riduce davvero l'esposizione (storia `0036`).
> 5. **Informativa a monte**: il modulo di contatto e la firma dei messaggi in uscita devono dire al cliente finale
>    chi tratta i suoi dati e per quanto. Il testo lo scrive il **titolare**, cioè l'azienda cliente: l'app deve
>    rendere possibile inserirlo, non scriverlo al posto suo.
>
> **Serve una valutazione d'impatto?** Non lo decide un agente. Gli elementi che la rendono probabile ci sono
> tutti — trattamento su larga scala di dati di terzi, categorie particolari non escludibili, ruolo di responsabile
> del trattamento — e vanno portati alla revisione legale pre-go-live
> ([docs/_REVISIONE-LEGALE.md](../../../_REVISIONE-LEGALE.md)).

**Il punto di partenza di tutto: appgrove qui è responsabile, non titolare.** In `fatture` o in `crm` i dati
trattati sono dell'azienda cliente e dei suoi contatti commerciali, e il rapporto è già inquadrato. Qui il dato è
il **contenuto della conversazione fra l'azienda cliente e i suoi clienti finali**, persone che con appgrove non
hanno alcun rapporto. Titolare è l'azienda cliente; appgrove è responsabile del trattamento e deve: trattare solo
su istruzione, elencare i propri sotto-responsabili, cancellare o restituire i dati alla fine, non usarli per
finalità proprie. **Non è un adempimento formale: è un vincolo di prodotto**, e da esso discendono la durata di
conservazione come parametro dell'account, la cancellazione per singolo richiedente dentro l'app, e il divieto per
la console di amministrazione di appgrove di mostrare il contenuto dei messaggi (vedi
[estensioni-admin.md](estensioni-admin.md)).

**Categorie trattate** (proposta di voci del manifesto, in italiano e inglese nel file reale)

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `requester.name` | `requester.name` | cliente finale dell'azienda cliente | identificativo | rivolgersi alla persona e riconoscere le richieste ripetute | trattamento per conto del titolare (l'azienda cliente), su sua istruzione | durata di conservazione dell'account (proposta predefinita: 24 mesi dalla chiusura) |
| `requester.email` | `requester.email` | cliente finale | contatto | ricevere la richiesta e rispondere | idem | idem |
| `requester.phone` | `requester.phone` | cliente finale | contatto | canale WhatsApp e richiamo | idem | idem |
| `requester.locale` | `requester.locale` | cliente finale | preferenza | rispondere nella lingua giusta | idem | idem |
| `ticket.subject` | `ticket.subject` | cliente finale | **testo libero** | identificare la richiesta | idem | idem |
| `message.body` | `ticket_message.body` | cliente finale e operatore | **testo libero — può contenere qualunque cosa** | erogare l'assistenza | idem | idem |
| `message.from_address` | `ticket_message.from_address` | cliente finale | contatto | ricostruire il filo della posta | idem | idem |
| `attachment.file` | archivio degli allegati + `attachment` | cliente finale e operatore | **contenuto arbitrario** | corredare la richiesta | idem | conservazione più breve dei messaggi (proposta: 12 mesi) — è il dato con il rapporto rischio/utilità peggiore |
| `agent.display_name` | `agent.display_name` | dipendente dell'azienda cliente | identificativo | firmare le risposte e attribuire il lavoro | esecuzione del contratto con l'azienda cliente | finché l'operatore è attivo + 12 mesi |
| `survey.comment` | `satisfaction_survey.comment` | cliente finale | **testo libero** | misurare la qualità del servizio | trattamento per conto del titolare | 24 mesi |
| `webform.ip` | `ticket.source_ip` | cliente finale | dato tecnico di collegamento | difesa dall'abuso del modulo pubblico | legittimo interesse del titolare alla sicurezza | **30 giorni**, poi cancellazione automatica |

**Esportazione e cancellazione.** Tabelle che contengono dati di persone e che devono comparire **sia** in
`exportData` **sia** in `purgeData` del contratto dati dell'app (`HelpdeskDataContract`): `requester`,
`ticket`, `ticket_message`, `attachment` (più i file nell'archivio, che non sono righe di tabella e sono il modo
più facile per dimenticarsene), `agent`, `satisfaction_survey`, `canned_response` (il corpo può contenere un nome
scritto a mano). La cancellazione è **fisica**: sostituire il nome del richiedente con un codice **non** è
cancellare. Serve inoltre una cancellazione **per singolo richiedente** (non solo per account), perché è il modo in
cui l'azienda cliente onora una richiesta che le arriva da un suo cliente: è la storia `0036`.

**Testo libero.** L'app ne ha quattro sorgenti: oggetto della richiesta, corpo dei messaggi, commento
dell'indagine di soddisfazione, corpo delle risposte predefinite. Le prime tre sono scritte da persone che non
controlliamo. È il rischio principale dell'applicazione, trattato nel riquadro in testa a questa sezione.

**Integrazioni esterne che ricevono dati personali** — ognuna è un potenziale sotto-responsabile da elencare nel
contratto di nomina e nell'informativa del cliente:

- **servizio di invio e ricezione della posta elettronica** della piattaforma appgrove: già presente, ma qui cambia
  il ruolo — trasporta messaggi *di terzi*, non nostri;
- **fornitore intermedio per WhatsApp Business e Meta** ⚠️: sotto-responsabili nuovi con trasferimento fuori
  dall'Unione Europea. È il punto su cui la storia `0017` si ferma e chiede allo sviluppatore;
- **archivio degli allegati**: infrastruttura di archiviazione già in uso, con il vincolo che i dati personali
  stanno a riposo **solo in regioni europee**.

Nessun tracciamento dentro l'app, nessun cookie non tecnico, nessun uso secondario dei dati dei clienti — e qui il
divieto è doppio, perché i dati non sono neanche nostri.

**Classificazione della change.** Un'app nuova che introduce finalità nuove, categorie nuove, un **ruolo nuovo**
(responsabile del trattamento) e sotto-responsabili nuovi è senza dubbio un cambiamento **sostanziale**. Non c'è
alcun margine per sostenere il contrario.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `elenca_richieste` | `(stato?, coda?, operatore?, oltre_scadenza?, periodo?) → elenco minimizzato di richieste` | Numero, oggetto, stato, scadenza, operatore. **Non** restituisce il corpo dei messaggi | lettura | no |
| `leggi_richiesta` | `(numero) → richiesta con il filo dei messaggi` | Il contenuto della conversazione, note interne comprese | lettura | no |
| `riassumi_richiesta` | `(numero) → sintesi, punti aperti, prossimo passo` | Il `summarize_ticket` della scheda di catalogo | lettura | no |
| `cerca_articoli` | `(testo, lingua?) → articoli pertinenti` | Il `search_kb` della scheda | lettura | no |
| `stato_del_servizio` | `(periodo) → richieste aperte, tempo medio di prima risposta, scadenze violate` | Il numero che il titolare vuole sapere la mattina | lettura | no |
| `crea_richiesta` | `(canale, richiedente, oggetto, testo) → bozza di richiesta` | Il `create_ticket` della scheda: apre una richiesta a nome di un cliente che ha telefonato | scrittura | **sì** |
| `prepara_risposta` | `(numero, indicazioni | articolo) → bozza di messaggio, non inviata` | Il `draft_reply` della scheda. **La bozza resta dentro l'app**: nessun messaggio esce | scrittura | **sì** |
| `invia_risposta` | `(id della bozza) → esito dell'invio` | **Manda un messaggio a una persona esterna: è irreversibile.** Una risposta sbagliata a un cliente non si richiama | scrittura irreversibile | **sì, obbligatoria e su bozza già scritta** |
| `assegna_richiesta` | `(numero, operatore) → esito` | Cambia il responsabile. Reversibile e interno | scrittura | no |
| `cambia_stato` | `(numero, stato, motivo?) → esito` | Segue la macchina a stati del §4. Reversibile e interno | scrittura | no |
| `inoltra_richiesta` | `(numero, motivo) → esito` | L'`escalate_ticket` della scheda: alza la priorità e avvisa il responsabile. Interno | scrittura | no |

**Lettura.** La ragione per cui il livello conversazionale rende questa app più utile delle concorrenti è la coppia
`riassumi_richiesta` + `prepara_risposta`: è letteralmente il lavoro che l'operatore fa a mano quando riprende in
mano una conversazione lunga. Ma la separazione fra `prepara_risposta` e `invia_risposta` è **il cuore della
sicurezza dell'app** e non è negoziabile: preparare è gratuito e reversibile, inviare è un atto verso una persona
che non è nostra utente e **non si annulla**. Nessun percorso conversazionale può fondere i due passi, nemmeno
«perché l'utente ha detto di fidarsi». La stessa regola vale per la pubblicazione di un articolo della base di
conoscenza, che è visibile al pubblico: nessuno strumento la esegue senza conferma.

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine dell'epica l'app esiste, è accesa, è vuota e utilizzabile: servizio avviabile in locale, schema con le
tabelle portanti, modulo visibile nella barra laterale, quota dei posti operatore che blocca davvero.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | Il servizio `helpdesk` nasce dallo scaffolding, risponde su `/api/helpdesk/v1/*` e ha la sua istanza di infrastruttura |
| [0002](01-fondamenta/0002-modello-dati-multi-account.md) | Modello dati multi-account | Schema `app_helpdesk` con richiesta, messaggio e richiedente: `tenant_id`, colonne di controllo, cancellazione logica |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Modulo `helpdesk` registrato, sezioni vuote, cinque lingue, colore-categoria verde acqua |
| [0004](01-fondamenta/0004-abbonamento-e-quota-dei-posti.md) | Abbonamento e quota dei posti operatore | La metrica `agents` a giacenza: `402` senza abbonamento, `429` a posti esauriti |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | `./dev.sh services` mostra l'app sulla porta 8112 e un comando la riempie di richieste inventate |

### Epica 02 — Casella condivisa e conversazioni

Il cuore del prodotto: una richiesta si apre, si legge, si risponde, cambia stato e non si perde. Alla fine
dell'epica l'app sostituisce già la casella di posta condivisa, anche prima di avere i canali automatici.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-casella-condivisa-e-conversazioni/0006-apertura-manuale-di-una-richiesta.md) | Apertura manuale di una richiesta | L'operatore apre una richiesta a nome di un cliente che ha telefonato o scritto altrove |
| [0007](02-casella-condivisa-e-conversazioni/0007-filo-dei-messaggi-e-risposta.md) | Filo dei messaggi e risposta | Il dettaglio della richiesta con la conversazione in ordine e la casella per rispondere |
| [0008](02-casella-condivisa-e-conversazioni/0008-note-interne.md) | Note interne | Un messaggio che il team vede e il cliente no, distinguibile a colpo d'occhio |
| [0009](02-casella-condivisa-e-conversazioni/0009-ciclo-di-vita-degli-stati.md) | Ciclo di vita degli stati | La macchina a stati del §4, con transizioni ammesse, riapertura e chiusura automatica |
| [0010](02-casella-condivisa-e-conversazioni/0010-elenco-ricerca-e-viste.md) | Elenco, ricerca e viste di lavoro | La coda con ricerca a testo libero, filtri e le tre viste che servono davvero |
| [0011](02-casella-condivisa-e-conversazioni/0011-unione-dei-duplicati.md) | Unione dei duplicati | Due richieste dello stesso cliente sullo stesso problema diventano una sola, senza perdere niente |

### Epica 03 — Canali di ingresso

Le richieste smettono di essere inserite a mano: arrivano da sole dal modulo del sito, dalla casella di posta e —
dopo una decisione dello sviluppatore — da WhatsApp.

| # | Storia | In una riga |
|---|---|---|
| [0012](03-canali-di-ingresso/0012-canali-e-anagrafica-del-richiedente.md) | Canali e anagrafica del richiedente | L'entità canale e il riconoscimento del richiedente già noto, senza duplicati |
| [0013](03-canali-di-ingresso/0013-modulo-web-di-contatto.md) | Modulo web di contatto | Un frammento da incollare nel sito del cliente che apre una richiesta, con difesa dall'abuso |
| [0014](03-canali-di-ingresso/0014-posta-elettronica-in-ingresso.md) | Posta elettronica in ingresso | I messaggi che arrivano alla casella dell'assistenza diventano richieste, con il filo ricostruito |
| [0015](03-canali-di-ingresso/0015-posta-elettronica-in-uscita.md) | Posta elettronica in uscita | La risposta dell'operatore parte come messaggio di posta e la replica torna nello stesso filo |
| [0016](03-canali-di-ingresso/0016-allegati-dei-messaggi.md) | Allegati dei messaggi | File in arrivo e in uscita, con limiti, archiviazione europea e conservazione più breve |
| [0017](03-canali-di-ingresso/0017-canale-whatsapp.md) | Canale WhatsApp | ⚠️ Il canale più richiesto e quello con più conseguenze: si ferma e chiede allo sviluppatore |

### Epica 04 — Organizzazione del lavoro

Da «tutti guardano tutto» a «ognuno sa cosa deve fare»: posti operatore, code, presa in carico, priorità e i testi
già scritti che fanno risparmiare la metà del tempo.

| # | Storia | In una riga |
|---|---|---|
| [0018](04-organizzazione-del-lavoro/0018-operatori-e-posti.md) | Operatori e posti | Chi dell'account lavora sulle richieste: il posto operatore che consuma la quota |
| [0019](04-organizzazione-del-lavoro/0019-code-di-lavoro.md) | Code di lavoro | Due o tre raggruppamenti («Assistenza», «Amministrazione») e il canale che li alimenta |
| [0020](04-organizzazione-del-lavoro/0020-presa-in-carico-e-assegnazione.md) | Presa in carico e assegnazione | «La prendo io» e «la passo a te», con l'avviso a chi la riceve |
| [0021](04-organizzazione-del-lavoro/0021-priorita-ed-etichette.md) | Priorità ed etichette | Quattro priorità che pilotano gli accordi di servizio ed etichette libere per ritrovare le cose |
| [0022](04-organizzazione-del-lavoro/0022-risposte-predefinite.md) | Risposte predefinite | I testi pronti con segnaposto, cercabili mentre si scrive |

### Epica 05 — Tempi di risposta e livello di servizio

La domanda «stiamo rispondendo in tempo?» ottiene una risposta vera: orario di lavoro, obiettivi, orologi che si
fermano quando devono, avvisi prima della scadenza e la soddisfazione del cliente.

| # | Storia | In una riga |
|---|---|---|
| [0023](05-tempi-di-risposta-e-servizio/0023-orario-di-servizio.md) | Orario di servizio | Il calendario dell'azienda: senza di esso ogni misura di tempo è falsa |
| [0024](05-tempi-di-risposta-e-servizio/0024-politiche-di-servizio.md) | Politiche di servizio | Gli obiettivi di prima risposta e risoluzione in ore lavorative, per priorità |
| [0025](05-tempi-di-risposta-e-servizio/0025-orologi-e-violazioni.md) | Orologi e violazioni | Il calcolo delle scadenze, la pausa quando la palla è al cliente, la violazione registrata |
| [0026](05-tempi-di-risposta-e-servizio/0026-avvisi-di-scadenza.md) | Avvisi di scadenza | L'operatore viene avvertito **prima** che la scadenza passi, non dopo |
| [0027](05-tempi-di-risposta-e-servizio/0027-indagine-di-soddisfazione.md) | Indagine di soddisfazione | Alla chiusura il richiedente vota con un collegamento monouso, senza registrarsi |
| [0028](05-tempi-di-risposta-e-servizio/0028-cruscotto-del-servizio.md) | Cruscotto del servizio | Quattro numeri e nessun grafico inutile: carico, tempi, violazioni, soddisfazione |

### Epica 06 — Base di conoscenza e portale del richiedente

Rispondere una volta sola alla stessa domanda, e far vedere al cliente finale a che punto è la sua richiesta senza
che debba registrarsi da nessuna parte.

| # | Storia | In una riga |
|---|---|---|
| [0029](06-base-di-conoscenza-e-portale/0029-articoli-della-base-di-conoscenza.md) | Articoli della base di conoscenza | Scrittura, bozza e pubblicazione degli articoli, per categoria e lingua |
| [0030](06-base-di-conoscenza-e-portale/0030-ricerca-e-inserimento-nella-risposta.md) | Ricerca e inserimento nella risposta | L'operatore trova l'articolo mentre scrive e lo inserisce nel messaggio |
| [0031](06-base-di-conoscenza-e-portale/0031-portale-pubblico-degli-articoli.md) | Portale pubblico degli articoli | Gli articoli pubblicati sono leggibili da chiunque, senza accesso, sotto il nome del cliente |
| [0032](06-base-di-conoscenza-e-portale/0032-collegamento-di-stato-per-il-richiedente.md) | Collegamento di stato per il richiedente | ⚠️ Il cliente finale segue la propria richiesta con un collegamento firmato, senza account |
| [0033](06-base-di-conoscenza-e-portale/0033-suggerimento-di-articoli-nel-modulo.md) | Suggerimento di articoli nel modulo | Il modulo di contatto propone la risposta prima che la domanda venga inviata |

### Epica 07 — Esposizione conversazionale e prove end-to-end

Il contratto degli strumenti per il livello conversazionale, il presidio che impedisce a un assistente di scrivere
a un cliente senza permesso, i diritti delle persone e il percorso end-to-end che tiene insieme tutto.

| # | Storia | In una riga |
|---|---|---|
| [0034](07-esposizione-conversazionale-e-prove/0034-strumenti-di-lettura.md) | Strumenti di lettura | I cinque strumenti che leggono, con la minimizzazione dei dati restituiti |
| [0035](07-esposizione-conversazionale-e-prove/0035-strumenti-di-scrittura-con-conferma.md) | Strumenti di scrittura con conferma | Preparare non è inviare: la bozza, la conferma umana e ciò che non si può automatizzare |
| [0036](07-esposizione-conversazionale-e-prove/0036-esportazione-cancellazione-e-conservazione.md) | Esportazione, cancellazione e conservazione | Il contratto dati dell'app, la cancellazione per singolo richiedente e la durata governata dal cliente |
| [0037](07-esposizione-conversazionale-e-prove/0037-percorso-end-to-end.md) | Percorso end-to-end | Il percorso `[J-HELPDESK]` dal modulo di contatto alla soddisfazione, e il registro di copertura |

**Totale**: 7 epiche, 37 storie.

---

## 9. Estensioni della console di amministrazione

Servono, e per una ragione che nessun'altra app del catalogo ha: qui appgrove è **responsabile del trattamento** di
dati di terzi, quindi il divieto di guardare i contenuti del cliente non è una buona pratica ma un obbligo
contrattuale. Le estensioni chieste sono tre, tutte su metadati: lo **stato dei canali** per account (verifica del
dominio di posta, connessione WhatsApp, ultimo errore), il **consumo di archiviazione degli allegati** e la
**deroga temporanea sui posti operatore** durante una migrazione. Nessuna vista mostra il testo di un messaggio.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| **04 — LeadGrove (CRM)** | condivide dati con | **Anagrafica clienti condivisa** (catalogo §6): il richiedente di una richiesta di assistenza *è* un contatto del CRM. La sinergia è la ragione per cui la suite trattiene il cliente: aprire una richiesta e vedere subito «cliente dal 2024, tre ordini, una richiesta aperta il mese scorso». |
| **02 — BillGrove (fatturazione)** | riceve riferimenti da | Le richieste del genere «dov'è la mia fattura» portano il numero del documento. Si scambia il **riferimento**, non una copia dei dati contabili. |
| **07 — BookGrove (prenotazioni)**, **21 — SalonGrove** e i verticali con clientela | alimentano | Sono le app che generano le richieste: una prenotazione da spostare è una richiesta di assistenza. |
| **05 — ChatGrove (WhatsApp commerce)** | si sovrappone a | Entrambe parlano su WhatsApp, e sarebbe un errore costruire due volte la connessione a Meta. Vedi «Sovrapposizioni» qui sotto. |
| **Assistenza interna della piattaforma (UC 0075, change `0084`)** | **riusa componenti, non dati** | Il confine è la parte più importante di questa sezione: sotto. |

**Come si condivide l'anagrafica clienti, in concreto.** Le regole di piattaforma dicono che **un'app non chiama
un'altra app** e che sono **vietate le interrogazioni fra schemi diversi**: la sola via è asincrona a eventi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §2, §8). DeskGrove tiene quindi la **propria** tabella
`requester` e, quando l'anagrafica condivisa esisterà, la allinea per eventi conservando il riferimento all'identità
condivisa. Finché non esiste, l'app funziona benissimo da sola: è il motivo per cui la storia `0012` non dipende da
nessun'altra app. **Nessuna storia di questa applicazione dipende da un'app del catalogo per poter partire.**

### Il confine con l'assistenza interna della piattaforma — leggere prima di progettare

Nel repository **esiste già** un sistema di richieste di assistenza fatto in casa: use case
[UC 0075](../../15-supporto-e-piattaforma/0075-ticketing-nativo-in-house.md), realizzato dalla change
`0084-use-case-0075-ticketing-nativo-in-house`. Vive nel servizio `core`, schema `platform`, package
`app.appgrove.core.support`, e comprende: entità `support_ticket` con filo di messaggi, stati
(`open`/`in_progress`/`waiting_user`/`resolved`/`closed`), priorità, provenienza (`form`/`event`/`email`),
contrassegno `flagged_for_review` per le categorie particolari, spazzino della conservazione a 24 mesi, notifiche
di posta attraverso il generatore unificato di `services/commons` e i modelli in `shared/email-templates`, pagina
«Supporto» del backoffice, coda «Ticket» **fra tutti gli account** nella console di amministrazione e percorso
end-to-end `J-SUPPORT-TICKETING`.

**Non è la stessa cosa, e non lo diventerà.** Quello serve ad **appgrove per assistere i propri clienti**; DeskGrove
serve **al cliente per assistere i suoi clienti**. Da questa differenza discendono cinque separazioni che non si
possono attraversare:

| # | Perché resta separato | Conseguenza pratica |
|---|---|---|
| 1 | **Veste giuridica opposta.** Sull'assistenza interna appgrove è **titolare** del trattamento verso i propri clienti; su DeskGrove è **responsabile** per conto del cliente, sui dati di persone che non sono nostre utenti | Finalità, basi giuridiche, conservazione e informativa sono diverse voce per voce: due manifesti dei dati distinti, non uno solo |
| 2 | **La coda della console di amministrazione legge fra tutti gli account.** È l'eccezione esplicita e ammessa all'isolamento, riservata al ruolo `platform-admin` | Applicarla a DeskGrove darebbe al personale di appgrove una strada per leggere le conversazioni dei clienti dei clienti. È esattamente ciò che il divieto di impersonificazione vieta |
| 3 | **Varchi opposti.** Una richiesta interna di tipo privacy è **esente** da abbonamento e quota, perché l'esercizio dei diritti non si blocca per motivi commerciali. DeskGrove è un'app a pagamento con `402` e `429` | Non esiste una logica di accesso comune: sarebbe un `if` che decide quando la conformità è opzionale |
| 4 | **Separazione fisica dei dati.** Un'app ha uno schema suo, niente chiavi esterne fra schemi, niente interrogazioni incrociate | Nessuna tabella condivisa è tecnicamente ammessa, nemmeno volendo |
| 5 | **Perimetro funzionale opposto.** L'assistenza interna esclude di proposito allegati, accordi di servizio, base di conoscenza, soddisfazione e portale, perché a un fondatore solo non servono. Sono **il prodotto** di DeskGrove | Fondere i due significherebbe o gonfiare lo strumento interno o mutilare quello venduto |

**Cosa invece si riusa — ed è molto, ed è un dovere riusarlo:**

1. **Il riconoscitore delle categorie particolari** (`SpecialCategoryScreening`, ~90 righe, deterministico, a radici
   di parole italiane e inglesi, che ammette per scelta i falsi positivi): DeskGrove ha lo stesso problema in forma
   **più grave**. Proposta: **spostarlo in `services/commons`** (area `privacy`) e usarlo da entrambi, con l'elenco
   delle radici estendibile per lingua — cambio di casa, non riscrittura. È già scritto nei punti aperti di UC 0075
   che l'elenco andrà esteso se arriveranno richieste in altre lingue: DeskGrove è proprio quel caso.
2. **Il generatore unificato dei messaggi di posta** di `services/commons` (UC 0085) e la sorgente unica
   `shared/email-templates`: DeskGrove aggiunge i **propri** modelli, non ne duplica il meccanismo. Con una
   differenza da progettare: i messaggi di DeskGrove partono **per conto del cliente** e devono portare la sua
   identità, non quella di appgrove (storia `0015`).
3. **La regola di non mettere il contenuto della conversazione dentro la notifica** (decisione 14 della change
   `0084`: l'avviso dice che c'è un aggiornamento e porta alla pagina, dove l'accesso è controllato). Qui vale
   ancora di più, perché il destinatario è esterno.
4. **La forma della macchina a stati**, la semantica di `waiting_user` («la palla è al cliente») e il modo di
   ordinare la coda per scadenza: sono decisioni già prese, discusse e collaudate. Ripensarle da capo sarebbe
   spreco; copiarle **come idea** costa un'ora.
5. **Lo spazzino della conservazione** (`TicketRetentionSweeper`) come modello di lavorazione periodica, e la
   struttura del percorso end-to-end con l'etichetta `[J-…]`.

**In una riga.** *Si riusano le librerie e le decisioni; non si riusano mai i dati, gli endpoint, i ruoli né le
tabelle.* Chi implementerà la storia `0002` deve avere sotto gli occhi
`services/core/src/main/java/app/appgrove/core/support/` — per **copiarne le buone idee e non ricopiarne il
perimetro**.

**Sovrapposizioni da evitare.**

- **WhatsApp con ChatGrove (app 5).** Due app che aprono ciascuna un proprio collegamento a Meta e a un fornitore
  intermedio significano due integrazioni da mantenere, due contratti con sotto-responsabili e due volte lo stesso
  lavoro. La strada giusta, il giorno in cui esistessero entrambe, è **una capacità di piattaforma condivisa** per
  la messaggistica, non una libreria copiata. Oggi ChatGrove è fra le applicazioni escluse dal drill-down, quindi
  chi arriva primo è DeskGrove: la storia `0017` deve essere scritta in modo da **non** cablare l'integrazione
  dentro il dominio dell'assistenza. È un punto aperto (§11).
- **Le note di interazione del CRM (app 4).** «Ho parlato con il cliente e mi ha detto…» può stare in due posti.
  Confine proposto: nel CRM sta l'interazione **commerciale**, in DeskGrove la richiesta di **assistenza**; il CRM
  mostra il rimando, non la copia.
- **La base di conoscenza contro un eventuale sito di documentazione.** Se la suite avesse un giorno un
  raccoglitore di contenuti, la base di conoscenza di DeskGrove non deve diventarne un secondo esemplare: qui gli
  articoli servono a **rispondere**, non a fare marketing.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **Prezzi, tetti dei piani, prova gratuita** (§5) | Fermata di escalation: nessun agente fissa prezzi | Sviluppatore |
| 2 | **Classificazione dei dati personali e valutazione d'impatto** (§6) | Fermata di escalation, aggravata dal fatto che il testo libero è una via d'ingresso non presidiata per l'articolo 9 | Sviluppatore + revisione legale pre-go-live |
| 3 | **Canale WhatsApp: si fa o non si fa?** | Introduce Meta e un fornitore intermedio come sotto-responsabili, con trasferimento fuori dall'Unione Europea. È in tensione diretta con la postura di sovranità dei dati del progetto | Sviluppatore — storia `0017` si ferma qui |
| 4 | **Contratto di nomina a responsabile del trattamento** | Un'app in cui appgrove tratta dati di terzi ha bisogno di un contratto di nomina, dell'elenco dei sotto-responsabili e dell'impegno di restituzione. Oggi la piattaforma non ne ha uno per questa fattispecie | Revisione legale ([docs/_REVISIONE-LEGALE.md](../../../_REVISIONE-LEGALE.md)) |
| 5 | **Portale del richiedente senza account** (storia `0032`) | Il cliente finale non è un utente di appgrove: non ha un token. L'accesso passa da un collegamento **firmato dalla piattaforma**, con scadenza e revoca. Va verificato che questo sia compatibile con l'invariante «`tenant_id` solo dal token verificato»: la proposta è che il gettone firmato **sia** un token verificato, con un solo permesso, e che il servizio ne ricavi il `tenant_id` esattamente come dagli altri | Sviluppatore + storia `0032` |
| 6 | **Volume di richieste e archiviazione senza tetto** (§5) | La metrica a posti non limita né le richieste né gli allegati; una sola metrica è ammessa. Il rimedio proposto è la conservazione come parametro del piano, ma è una scelta di prodotto | Sviluppatore — storia `0036` |
| 7 | **Spostare `SpecialCategoryScreening` in `services/commons`** | È una modifica a codice esistente del servizio `core`, fuori dal perimetro di questa applicazione | Chi implementa la storia `0002`, d'accordo con chi possiede UC 0075 |
| 8 | **Dove vive l'integrazione con WhatsApp** (§10) | Se un giorno esistesse anche ChatGrove, sarebbe una capacità di piattaforma; oggi non c'è nessuno con cui condividerla | Sviluppatore, quando ChatGrove uscisse dalle escluse |
| 9 | **Antivirus sugli allegati** (storia `0016`) | Accettare file da chiunque significa accettare file infetti. Un controllo antivirus è un fornitore esterno in più, oppure un componente da mantenere | Sviluppatore — storia `0016` si ferma alla difesa per tipo e dimensione |
| 10 | **Identità del mittente delle risposte** (storia `0015`) | Rispondere «per conto di» un altro dominio tocca autenticazione dei messaggi e recapitabilità: o si fa verificare il dominio al cliente, o si spedisce da un dominio di appgrove con l'indirizzo di risposta del cliente. Le due strade hanno esiti diversi sulla percezione del cliente finale | Sviluppatore — storia `0015` propone, non decide |
| 11 | **Prezzo di Zoho Desk in euro e listino di LiveAgent** (§2.7) | Dati di mercato non rilevati su pagina ufficiale | Chi valida il listino (punto 1) |
| 12 | **Chat dal vivo** | Dichiarata fuori ambito (§1), ma è la richiesta che tornerà per prima dal mercato. Non va aggiunta di soppiatto: cambierebbe il prodotto e le aspettative sui tempi | Sviluppatore, come decisione di direzione di prodotto |

**Rischi noti**

- **Il testo libero come porta d'ingresso di dati particolari** — un dato dell'articolo 9 finisce in una tabella
  ordinaria e nessuno se ne accorge → contrassegno per la revisione umana, conservazione breve, nessuna analisi
  esterna del contenuto, informativa a monte sul modulo (§6).
- **Il mercato è affollato e maturo** — si compete con prodotti che hanno dieci anni di funzioni → non si vince
  aggiungendo funzioni ma sottraendole e stando dentro una suite dove l'anagrafica è già lì (§2.1, §2.5).
- **Il canale di posta è il prodotto e anche il suo punto fragile** — se i messaggi non arrivano o finiscono nella
  posta indesiderata, l'app non serve a niente → verifica del dominio, sorveglianza dei recapiti falliti, diagnostica
  nella console di amministrazione.
- **Ricezione di posta da chiunque = abuso** — moduli e caselle pubbliche attirano messaggi automatici → limite di
  frequenza, campo-trappola sul modulo, conservazione a 30 giorni dell'indirizzo di rete, mai un elenco di blocco
  gestito a mano.
- **La misura del servizio può diventare un giocattolo** — se gli orologi non si fermano quando la palla è al
  cliente, i numeri sono falsi e gli operatori smettono di guardarli → è il motivo per cui la pausa è un requisito
  della storia `0025` e non una raffinatezza successiva.
- **Costruire due volte il ticketing** — il rischio che questo documento esiste per scongiurare: si riusano
  componenti e decisioni dell'assistenza interna, non i suoi dati (§10).

**Fuori dimensionamento**: nessuno. 7 epiche (limite massimo raccomandato) e 37 storie, dentro la fascia 20-45; le
epiche hanno da 4 a 6 storie ciascuna.
