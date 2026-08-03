# LeadGrove — descrizione dell'applicazione

**Numero di catalogo**: 04 · **Tipo**: orizzontale · vendite · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 04](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** LeadGrove tiene l'anagrafica delle aziende e delle persone con cui si fanno affari, e ci appoggia
sopra una pipeline di vendita: ogni possibile affare è una **trattativa** che avanza per fasi fino a «vinta» o
«persa», con le attività da fare, le note di ciò che è stato detto e lo storico di come si è arrivati fin lì.
Produce tre cose concrete: una scheda cliente che non vive in una testa sola, una lavagna che dice a colpo
d'occhio dove sono i soldi in ballo, e un rapporto di conversione che dice quante trattative si chiudono, in
quanto tempo e perché si perdono.

**Per chi.** Micro-imprese da 1 a 10 addetti e piccole imprese fino a 50, mercato globale con priorità europea.
Chi compra è il titolare o il responsabile commerciale; chi usa tutti i giorni sono da uno a una decina di
venditori, spesso persone che vendono **e** fanno anche altro. Il profilo tipico non ha un amministratore di
sistema e non ha nessuno il cui mestiere sia «configurare il gestionale».

**Quale problema toglie.** Oggi la vendita di queste aziende sta in tre posti: la rubrica del telefono, la
casella di posta e un foglio di calcolo con i preventivi in corso. Il costo non è il foglio in sé — è che il
foglio è di una persona sola: quando quella persona è in ferie, malata o se ne va, il pezzo di anagrafica che
aveva in testa sparisce, le richieste non richiamate restano non richiamate e nessuno sa dire quanto vale il
lavoro in corso. Le alternative di mercato risolvono il problema, ma con un impianto sovradimensionato: le
statistiche riportate al §2.5 dicono che una implementazione su tre fallisce per complessità e che la maggior
parte dei clienti piccoli usa meno di metà delle funzioni che paga.

**Cosa NON fa.**

- non manda campagne di posta elettronica e non fa automazione di marketing (l'invio massivo è un'altra app del
  catalogo, 16 ReachGrove): LeadGrove registra il **consenso**, non lo usa per spedire;
- non sincronizza casella di posta e calendario con fornitori esterni (Gmail, Microsoft 365): l'agenda si esporta
  in un file di calendario, il collegamento vivo è un punto aperto (§11.3);
- non emette preventivi, ordini, fatture né incassa (sono le app 06, 02, 01, 03): la trattativa vinta **notifica**
  la catena del documento contabile, non la esegue;
- non fa telefonate e non compone numeri: non è un centralino, quindi non tocca gli obblighi di verifica del
  Registro pubblico delle opposizioni (§2.3);
- non assegna un punteggio automatico ai lead e non prende decisioni automatizzate sulle persone (§6);
- non è un gestionale del post-vendita: assistenza e ticket sono l'app 12.

**Rischio di sostituzione da parte dei modelli linguistici.** `rafforzata`, come nel catalogo. Un assistente
generico sa scrivere un messaggio a un cliente ma non sa **quale** cliente va richiamato oggi, a che punto è la
trattativa e cosa è stato promesso tre settimane fa: quel valore sta nei dati proprietari dell'account e nel
flusso di lavoro, non nella generazione di testo. Il livello conversazionale rende l'app più utile, non
superflua: «chi devo richiamare oggi» e «riassumi il rapporto con l'azienda Alfa» sono esattamente le domande
che un elenco non risponde da solo.

---

## 2. Mercato e analisi in rete

> Compilata dopo 9 ricerche e recuperi di pagina ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4).
> Ciò che non è stato trovato è **dichiarato** al §2.7, non colmato a intuito.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| Pipedrive | Estonia/globale, forte in Europa | Pipeline visuale, il riferimento della categoria per le piccole squadre di vendita | Lite 14 $/posto/mese con fatturazione annuale, 24 $ con fatturazione mensile; fino a 79/99 $ sui piani alti; nessun piano gratuito, prova di 14 giorni — **valori da fonte editoriale, non dalla pagina ufficiale** (§2.7) | [Forbes Advisor, Pipedrive Pricing 2026](https://www.forbes.com/advisor/business/software/pipedrive-pricing/) |
| Bigin (Zoho) | India/globale, listino ufficiale pubblico | CRM deliberatamente piccolo, pensato per chi trova Zoho CRM già troppo | Free 0 $ (1 utente, 500 schede, 1 pipeline); Express 9 $/utente/mese mensile, 7 $ annuale; Premier 15/12 $; Bigin 360 21/18 $; prova di 15 giorni — **rilevato su pagina ufficiale** | [bigin.com/pricing](https://www.bigin.com/pricing.html) |
| HubSpot Sales Hub | Stati Uniti/globale | Piano d'ingresso di un ecosistema molto più grande; l'aggancio è il numero di integrazioni | Starter 20 $/posto/mese con fatturazione mensile, 15 $ con impegno annuale — **valori da fonti editoriali** | [blog.hubspot.com — guida ai prezzi di Sales Hub](https://blog.hubspot.com/sales/hubspot-sales-hub-pricing) · [docket.io, ricerca sui prezzi 2026](https://www.docket.io/resources/research/hubspot-sales-hub-pricing) |
| monday CRM | Israele/globale | CRM costruito sopra uno strumento di gestione del lavoro; molto configurabile | Basic 12 $/posto/mese annuale, **con minimo di 3 posti** → 27 $/mese effettivi minimi; Standard 17 $, Pro 28 $; fatturazione mensile +18 % — **valori da fonte editoriale** | [usecarly.com, monday pricing 2026 e il minimo di 3 posti](https://www.usecarly.com/blog/monday-pricing/) |
| Salesflare | Belgio | CRM «che si compila da solo» leggendo posta e calendario; molto citato nel segmento micro europeo | **prezzo non rilevato** in questa analisi (§2.7) | [blog.salesflare.com — migliori strumenti di gestione dei lead 2026](https://blog.salesflare.com/it/il-miglior-software-di-gestione-dei-lead) |

**Lettura.** La categoria è affollata e matura: nessuno vincerà per funzioni. Dove il segmento micro resta scoperto
sono tre punti concreti. Primo, il **prezzo di ingresso reale**: Pipedrive non ha piano gratuito, monday impone un
minimo di tre posti anche a chi è in due, HubSpot ha un piano d'ingresso economico ma tutto il valore sta a
monte. Secondo, la **complessità**: tutti e quattro sono configurabili, e la configurabilità è precisamente ciò che
fa fallire le adozioni piccole (§2.5). Terzo, l'**appartenenza alla suite**: nessuno dei quattro sa che l'azienda
Alfa ha anche tre fatture scadute, perché la fatturazione è di un altro fornitore. È qui che sta l'argomento di
LeadGrove, non nel numero di campi personalizzati.

### 2.2 Prezzi praticati nel dominio

- **Unità di misura**: prevalentemente il **posto** (utente nominale), quasi senza eccezioni. È l'unità che il
  mercato ha insegnato al cliente ed è quella su cui il cliente sa fare il confronto.
- **Fascia d'ingresso**: 7-15 $ per posto al mese con fatturazione annuale (Bigin Express 7 $, monday Basic 12 $,
  Pipedrive Lite 14 $, HubSpot Starter 15 $); la fatturazione mensile costa dal 18 % (monday) al ~70 % (Pipedrive
  Lite: 24 $ contro 14 $) in più.
- **Fascia media**: 12-28 $ per posto al mese. Sopra i 30 $ si esce dal segmento micro.
- **Piano gratuito**: presente in Bigin (1 utente, 500 schede) e in HubSpot; assente in Pipedrive e in monday CRM.
  Dove c'è, è limitato **a giacenza** (utenti, schede), non a consumo.
- **Prova gratuita**: 14 giorni in Pipedrive, 15 in Bigin. È lo standard di categoria e coincide con la
  raccomandazione predefinita della piattaforma ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §7).
- **Sconto annuale**: da 17 % a 35 % dichiarato. La convenzione appgrove — annuale = 10× il mensile, «due mesi in
  regalo», cioè 17 % — sta nella parte bassa dell'intervallo di mercato ed è quindi difendibile.

Distinzione d'onestà: solo i valori di **Bigin** vengono dalla pagina di prezzo ufficiale. Pipedrive, HubSpot e
monday sono presi da fonti editoriali e di comparazione, che il catalogo stesso avverte di trattare come ordini
di grandezza (§8 del catalogo). Vanno riverificati sui siti dei fornitori prima di fissare il posizionamento.

### 2.3 Obblighi normativi del settore

Il dominio non ha una normativa **di settore** (non è la fatturazione, non è la sicurezza sul lavoro): quello che
lo governa è la disciplina generale sui dati personali, che però qui morde più che altrove, perché l'oggetto
stesso dell'app sono nomi di persone raccolti spesso **prima** che esista un contratto.

1. **Informativa al momento della raccolta (art. 13 del regolamento europeo).** Nel momento in cui un modulo web
   raccoglie i dati di una persona, chi li raccoglie deve dire chi è, per quali finalità tratta i dati, su quale
   base giuridica, a chi li comunica e per quanto li conserva. Conseguenza sul prodotto: **il modulo web di
   acquisizione non può esistere senza un posto dove il cliente mette la propria informativa** — è un requisito
   che cambia il modello dati, non una nota legale (storia 0029).
2. **Consenso al marketing: libero, specifico, informato, documentato e revocabile.** L'opt-in non può essere
   pre-spuntato, deve essere separato dalle altre finalità (una casella per «richiedo di essere ricontattato», una
   distinta e facoltativa per «voglio ricevere comunicazioni commerciali») e chi tratta deve poter **dimostrare**
   di averlo ottenuto: servono marca temporale, testo accettato e canale. Conseguenza: una tabella di prova del
   consenso, non una casella booleana (storie 0011 e 0029).
   Fonte: [Cyber Security 360 — Marketing e campagne di lead generation nel rispetto del GDPR](https://www.cybersecurity360.it/legal/privacy-dati-personali/marketing-e-campagne-di-lead-generation-nel-rispetto-del-gdpr-linee-guida/).
3. **Contatti da azienda a azienda non sono esenti.** Un indirizzo `nome.cognome@azienda.it` identifica una persona
   fisica e ricade nella disciplina; la via del «legittimo interesse» esiste ma richiede una valutazione di
   bilanciamento documentata. Conseguenza: LeadGrove non deve suggerire che i contatti aziendali siano liberi, e il
   campo «base giuridica» delle preferenze di contatto deve poter valere sia «consenso» sia «legittimo interesse».
4. **Telefonate promozionali in Italia — Registro pubblico delle opposizioni.** Chi fa campagne telefoniche deve
   confrontare **ogni mese** le proprie liste con il registro prima di chiamare, e la violazione ricade nelle
   sanzioni del regolamento europeo. LeadGrove **non compone numeri e non genera liste di chiamata**, quindi
   l'obbligo resta in capo al cliente; ma la funzione «esporta i contatti» è il punto in cui una lista nasce, e
   l'app deve dirlo all'utente invece di far finta di niente (storia 0027).
   Fonte: [Registro pubblico delle opposizioni — sezione operatore](https://registrodelleopposizioni.it/operatore/).
5. **Diritti dell'interessato.** Cancellazione e portabilità arrivano al cliente dell'app (che è il titolare) e
   scendono su di noi (responsabili): tutte le tabelle con dati di persone devono comparire nell'esportazione e
   nella cancellazione del contratto dati (§6).

Non ho trovato obblighi di conservazione **minima** imposti a un archivio commerciale come tale: la durata di
conservazione è una scelta del titolare, da dichiarare, non un termine di legge (§2.7).

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta, secondo l'analisi di categoria
([Zeeg — le integrazioni di CRM che contano nel 2026](https://zeeg.me/en/blog/post/best-crm-integrations)):

| # | Integrazione | Perché la chiedono | Fornitore esterno che tratterebbe dati? |
|---|---|---|---|
| 1 | **Casella di posta** (Gmail, Microsoft 365) | far comparire da sole le conversazioni sulla scheda del contatto | **sì** — accesso alla corrispondenza: è l'integrazione con l'impatto più alto sulla privacy dell'intero catalogo |
| 2 | **Calendario** (Google Calendar, Microsoft 365) | far comparire gli appuntamenti nella cronologia senza riscriverli | **sì** |
| 3 | **Moduli di acquisizione dal sito** | non ricopiare a mano le richieste che arrivano dal sito | no, se il modulo è nostro (storia 0028) |
| 4 | **Messaggistica** (WhatsApp) | il canale reale di molti mercati; ogni messaggio in ingresso crea o aggiorna una scheda | **sì** — ed è il perimetro dell'app 05 ChatGrove, non di questa |
| 5 | **Contabilità e fatturazione** | vedere sulla scheda cliente se ha fatture aperte | no verso l'esterno: è sinergia **interna** alla suite (§10) |

Decisione di perimetro di questa proposta: le integrazioni 1, 2 e 4 **restano fuori** dalle 37 storie. La 3 è
dentro (è nostra e non introduce fornitori). La 5 è dentro come **evento**, non come chiamata. Il motivo è al
§11.3: collegare la casella di posta significa aggiungere un responsabile esterno del trattamento e un flusso di
consenso delegato, e nessuna delle due cose la decide un agente che scrive documenti.

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Quello che chiedono è poco e sempre lo stesso: sapere chi richiamare oggi, vedere le trattative aperte con il loro
valore, non perdere lo storico. Quello che **rifiutano** è più informativo:

- **la configurazione iniziale**. «Una implementazione di CRM su tre fallisce» e «una piccola impresa su tre
  abbandona il CRM entro il primo anno per complessità o costo» sono le cifre che circolano nella categoria; il
  meccanismo descritto è sempre lo stesso — decine di campi personalizzati e flussi elaborati costruiti *prima*
  che qualcuno abbia registrato una sola trattativa. Fonti:
  [U.S. Small Business Administration — i tre maggiori problemi nell'adozione di un CRM](https://www.sba.gov/blog/3-biggest-problems-implementing-crm-system-what-do-about-them) ·
  [dev.to — perché le piccole imprese abbandonano i CRM da grande impresa](https://dev.to/mainflow07/why-small-businesses-are-abandoning-enterprise-crms-and-what-theyre-using-instead-e23).
- **pagare funzioni che non usano**: la cifra citata è che il 63 % delle piccole imprese usa meno di metà delle
  funzioni del proprio CRM. È una statistica di parte (fonte editoriale, nessuna indagine indipendente), quindi
  la tratto come indizio, non come dato — ma va nella stessa direzione di tutto il resto.
- **l'inserimento manuale**: il 39 % di chi non ha un CRM sta su un misto di fogli di calcolo e posta perché
  «almeno lì non devo registrare niente due volte»
  ([indagine Zoho sulle piccole imprese e il CRM](https://prezohoweb.zoho.com/news/survey-from-zoho-finds-small-businesses-ready-to-invest-in-crm-solutions.html)).

**Requisiti travestiti che ne discendono**, e che ho recepito nella struttura delle epiche:

1. l'app deve essere utile **il primo giorno con zero configurazione**: una pipeline predefinita già pronta, non
   una procedura guidata di impostazione (storia 0012);
2. l'importazione da foglio di calcolo è una funzione di **primo giorno**, non un'aggiunta successiva (storia 0025);
3. i campi personalizzati esistono ma arrivano **dopo** l'anagrafica essenziale e sono deliberatamente pochi
   (storia 0009);
4. la vista che conta è «cosa devo fare oggi», non il cruscotto (storia 0020).

### 2.6 Fonti consultate

1. **Forbes Advisor — Pipedrive Pricing (guida 2026)** — https://www.forbes.com/advisor/business/software/pipedrive-pricing/ — quattro piani da 14 a 79 $/posto/mese con fatturazione annuale (24-99 $ mensile), nessun piano gratuito, prova di 14 giorni su ogni livello. Usata per la fascia di ingresso del concorrente di riferimento.
2. **Bigin by Zoho — pagina di prezzo ufficiale** — https://www.bigin.com/pricing.html — unica pagina ufficiale che sono riuscito a leggere: piano gratuito a 1 utente e 500 schede, Express 9/7 $, Premier 15/12 $, Bigin 360 21/18 $, prova di 15 giorni, sconto annuale dichiarato fino al 35 %. Usata per il piano gratuito e per la struttura dei limiti (schede e pipeline, entrambi a giacenza).
3. **HubSpot — guida ai prezzi di Sales Hub** — https://blog.hubspot.com/sales/hubspot-sales-hub-pricing — struttura dei piani per posto di Sales Hub. Usata per capire come si compone il piano d'ingresso di un ecosistema grande.
4. **Docket — ricerca sui prezzi di HubSpot Sales Hub 2026** — https://www.docket.io/resources/research/hubspot-sales-hub-pricing — Starter a 20 $/posto/mese mensile, 15 $ con impegno annuale (verifica del 6 luglio 2026). Usata come riscontro indipendente del punto 3.
5. **usecarly — monday.com pricing 2026 e il minimo di 3 posti** — https://www.usecarly.com/blog/monday-pricing/ — Basic 12 $/posto/mese annuale ma **minimo tre posti**, scaglioni successivi a blocchi di 5, +18 % con fatturazione mensile. Usata per il §2.1: è l'esempio di come un minimo di posti escluda di fatto chi lavora in due.
6. **Cyber Security 360 — Marketing e campagne di lead generation nel rispetto del GDPR** — https://www.cybersecurity360.it/legal/privacy-dati-personali/marketing-e-campagne-di-lead-generation-nel-rispetto-del-gdpr-linee-guida/ — consenso libero, specifico, informato, non pre-spuntato, separato per finalità, documentabile e revocabile; onere della prova sul titolare. È la fonte da cui discendono le storie 0011 e 0029 e la tabella di prova del consenso.
7. **Registro pubblico delle opposizioni — sezione per l'operatore** — https://registrodelleopposizioni.it/operatore/ — obbligo di confronto **mensile** delle liste prima di ogni campagna telefonica, tariffe di accesso a abbonamento, maggiorazione per chi è già stato sanzionato. Usata per delimitare cosa LeadGrove non fa (niente liste di chiamata) e per l'avviso sull'esportazione.
8. **Zeeg — le migliori integrazioni di CRM nel 2026** — https://zeeg.me/en/blog/post/best-crm-integrations — le tre integrazioni «del primo giorno» sono posta, calendario e moduli di acquisizione; contabilità solo quando il volume lo giustifica. Usata per l'ordine del §2.4 e per decidere cosa resta fuori.
9. **U.S. Small Business Administration — i tre maggiori problemi nell'adozione di un CRM** — https://www.sba.gov/blog/3-biggest-problems-implementing-crm-system-what-do-about-them — una implementazione su tre fallisce, la causa principale è l'eccesso di complessità e la mancanza di un obiettivo chiaro. Usata per il §2.5 e per la scelta di far nascere l'app con una pipeline già pronta.
10. **Indagine Zoho sulle piccole imprese e il CRM** — https://prezohoweb.zoho.com/news/survey-from-zoho-finds-small-businesses-ready-to-invest-in-crm-solutions.html — chi non ha un CRM sta su fogli di calcolo e posta; la domanda è di strumenti più facili da imparare. Fonte **di parte** (è un fornitore che parla del proprio mercato): usata come indizio, non come dato.

### 2.7 Cosa NON sono riuscito a determinare

- **Prezzi ufficiali di Pipedrive, HubSpot e monday CRM in euro.** Le pagine ufficiali di Pipedrive rispondono con
  un rifiuto automatico alla lettura da parte di uno strumento; per HubSpot e monday ho trovato solo pagine
  editoriali. I valori in tabella sono in dollari e da fonti di terzi. *Serve per chiuderlo*: una verifica manuale
  sulle pagine ufficiali con la valuta impostata su euro e su un paese europeo, prima di fissare il listino.
- **Prezzo di Salesflare**, che è il concorrente europeo più citato nel segmento micro. Non l'ho rilevato.
- **Quota di mercato e tassi di abbandono verificati**: le cifre del §2.5 («una su tre», «63 %») circolano su
  fonti editoriali e di fornitori senza indagine primaria indipendente. Le ho riportate come indizi convergenti e
  **non** vanno usate in materiale di vendita.
- **Durata di conservazione attesa di un archivio commerciale.** Non esiste un termine di legge: è una scelta del
  titolare. *Serve per chiuderlo*: una posizione predefinita proposta dallo sviluppatore e validata in sede di
  revisione legale, da scrivere nel manifesto dati.
- **Se il mercato accetti un listino a piano fisso invece che a posto.** È il punto aperto più importante della
  proposta di prezzo (§5 e §11.2).

---

## 3. Varco d'identità — le risposte pronte per `new-application`

> Queste sei righe sono ciò che la skill `new-application` chiede **prima** di generare qualunque cosa.
> L'identificativo dell'app finisce nel nome dello schema del database, nei nomi delle code, nella rotta pubblica
> e nell'istanza del modulo di infrastruttura: cambiarlo dopo **non è una rinomina, è una migrazione di dati**.

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `sales` | Rispetta `^[a-z][a-z0-9_]{0,30}$`. **Attenzione: `crm` è già occupato** nel repository dal Mini-CRM (`services/crm`, schema `app_crm`, porta 8082), l'app costruita come veicolo per validare la skill `new-application` e oggi spenta per tutti gli account. `sales` dice cosa l'app **è** — il sistema che governa la vendita — senza dipendere dal nome commerciale «LeadGrove» e senza collidere. Il rapporto fra le due app è una decisione di prodotto aperta (§11.1): se lo sviluppatore decidesse di ritirare il Mini-CRM e riusare `crm`, va deciso **prima** dello scaffolding, non dopo. |
| **Modello utente** | `multi` | La vendita in due o in dieci è un lavoro di squadra: «chi ha preso in carico questa trattativa», «chi ha chiamato per ultimo», «di chi è questa attività» sono domande che un'app a utente singolo non sa nemmeno porre, e su cui si regge metà dell'epica 03 e tutta la storia 0031. È anche coerente con il mercato, che vende a posti (§2.2). |
| **Porta locale** | `8104` | Convenzione del kit: 8100 + 04. Da confermare con `./dev.sh services` al momento dello scaffolding (le app reali occupano oggi 8081 `fatture`, 8082 `crm`, 9100 autenticazione). |
| **Metrica di quota** | `seats` (posti occupati) | È la **sola** cosa che il piano limita. È l'unità che tutto il mercato usa (§2.2), quella che il cliente sa confrontare, e cresce esattamente con il valore ricevuto: una squadra che vende di più ha più venditori dentro l'app. Le alternative scartate: il numero di contatti (cresce con l'anzianità, non con il valore, e punisce chi importa la propria rubrica il primo giorno — cioè proprio il comportamento che vogliamo incoraggiare, §2.5) e il numero di trattative (spingerebbe a non registrarne, cioè a non usare l'app). |
| **Natura della metrica** | `stock` (a giacenza) | Il tetto vale su quanti posti sono **occupati adesso**, non su quanti se ne assegnano nel mese: «il piano Team ha 5 posti; per far entrare un sesto venditore bisogna liberare un posto o passare di piano». Non c'è nessuna finestra che si azzera — un posto liberato a marzo torna disponibile subito, non il primo di aprile. Conseguenza obbligata: il passaggio a un piano inferiore è **bloccato** finché i posti occupati eccedono il tetto di destinazione ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §13). |
| **Colore-categoria e icona** | `blue` · icona `users` (due sagome di persona) | Il blu è il colore della relazione commerciale nel sistema di design, e distingue LeadGrove dalle app della catena contabile (02 BillGrove e 01 InvoiceGrove, dove il verde segnala il denaro incassato) e da quelle di recupero crediti (dove il rosso segnala il ritardo). Nota: il Mini-CRM spento usa anch'esso `blue`; poiché è `status: inactive` non compare in nessun elenco visibile al cliente, la coincidenza non genera confusione — ma va guardata insieme al §11.1. Lo stesso valore va in `category` nel listino e in `accentToken` nel manifesto del modulo frontend. |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Company` | L'organizzazione con cui si fa affari | denominazione, settore, sito, indirizzo, partita IVA facoltativa, `owner_user_id` | Solo se è una ditta individuale (la denominazione può coincidere con il nome di una persona) |
| `Contact` | La persona dentro l'organizzazione | nome, cognome, ruolo, posta elettronica, telefono, `company_id`, origine, `owner_user_id` | **sì** — nome, contatti, ruolo |
| `ContactPreference` | Come e se questa persona vuole essere contattata | canale, ammesso/negato, base giuridica (`consenso` \| `legittimo interesse`), momento, testo accettato, origine della prova | **sì** — è la prova del consenso |
| `Pipeline` | Un imbuto di vendita | nome, predefinita sì/no, ordine | no |
| `Stage` | Una fase dell'imbuto | nome, posizione, probabilità di chiusura in percentuale, esito terminale (`nessuno` \| `vinta` \| `persa`) | no |
| `Deal` | La trattativa: il possibile affare | titolo, valore, valuta, `company_id`, `contact_id`, `pipeline_id`, `stage_id`, `owner_user_id`, data attesa di chiusura, esito, motivo di perdita | Indirettamente (per riferimento) |
| `DealStageEvent` | Il passaggio di una trattativa da una fase all'altra | trattativa, fase di partenza, fase di arrivo, momento, autore | no (identificativi, non nomi) |
| `Activity` | Una cosa da fare o fatta: chiamata, riunione, compito | tipo, titolo, scadenza, completata, esito, `owner_user_id`, riferimento a contatto/azienda/trattativa | no in sé; il titolo è testo libero |
| `Note` | Annotazione libera su una scheda | testo, autore, riferimento | **potenzialmente** — è testo libero (§6) |
| `WebForm` | Il modulo pubblico di acquisizione | nome, campi, pipeline di destinazione, testo dell'informativa del cliente, testi dei consensi, chiave pubblica, attivo sì/no | no in sé |
| `FormSubmission` | Un invio ricevuto dal modulo pubblico | dati grezzi inviati, momento, esito dell'elaborazione, contatto generato | **sì** |
| `CustomField` / `CustomFieldValue` | Campo aggiuntivo definito dal cliente e suo valore | entità di destinazione, etichetta, tipo, obbligatorio | **potenzialmente** — dipende da cosa ci mette il cliente (§6) |
| `ImportJob` / `ImportRow` | Una importazione da file tabellare e le sue righe | file d'origine, mappatura delle colonne, righe totali/riuscite/scartate, motivo dello scarto | **sì** finché le righe restano in archivio |
| `Seat` | Il posto occupato da un membro dell'account | identificativo interno del membro, momento di assegnazione | Solo l'identificativo interno, non un nome |

**Relazioni.** `Company` 1→N `Contact`; `Company` 1→N `Deal`; `Contact` 1→N `ContactPreference`; `Pipeline` 1→N
`Stage`; `Stage` 1→N `Deal`; `Deal` 1→N `DealStageEvent`; `Activity` e `Note` si agganciano polimorficamente a
contatto, azienda o trattativa (una sola destinazione per riga, non tre); `WebForm` 1→N `FormSubmission`;
`FormSubmission` 0..1→ `Contact` generato; `ImportJob` 1→N `ImportRow`.

**Macchina a stati della trattativa** — è la parte che tutte le storie dell'epica 03 devono rispettare:

```
  aperta ──(cambio di fase)──▶ aperta          il passaggio scrive sempre un DealStageEvent
     │
     ├──(esito «vinta»)───────▶ vinta   ──▶ terminale: la trattativa esce dalla lavagna,
     │                                        emette l'evento «trattativa vinta» (§10)
     └──(esito «persa»)───────▶ persa   ──▶ terminale, con motivo di perdita obbligatorio
```

Riapertura: una trattativa terminale si può **riaprire**, e la riapertura è essa stessa un `DealStageEvent`
(altrimenti i tempi medi di chiusura mentono). Nessuna transizione salta la scrittura dello storico: è la storia
0015 a renderlo un invariante.

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica
(`deleted_at`); schema `app_sales`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8).

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il
> file `services/core/src/main/resources/pricing/sales.yaml`.

**Ragionamento.** Ci sono due numeri che non combaciano e vanno guardati insieme prima di leggere la tabella.

1. Il mercato vende **a posto**: 7-15 $ per posto al mese nella fascia d'ingresso (§2.2), e il catalogo indica per
   LeadGrove 9-19 € per utente al mese.
2. Il listino come codice di appgrove **non sa fatturare a posto**: un livello ha *un* prezzo e *un* tetto sulla
   metrica ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §7, e il file reale `pricing/crm.yaml` lo
   conferma: `team` costa 19 €/mese con `cap: 10` posti, non 19 € a posto).

La proposta qui sotto adotta il modello della piattaforma — **prezzo fisso per livello, tetto sui posti** — e
riporta accanto il prezzo per posto *implicito*, che è il numero con cui il cliente farà il confronto. Il piano
Team a 39 €/mese per 5 posti equivale a 7,80 € per posto: sta nella fascia bassa del mercato senza scendere a un
prezzo che segnali «prodotto minore». Chi è in due paga comunque 39 €, cioè 19,50 € a testa: è il caso peggiore
della proposta ed è il motivo per cui il piano gratuito da un posto conta (Pipedrive non ce l'ha, monday obbliga a
tre posti anche a chi è solo — §2.1).

| Piano | Prezzo mensile | Prezzo annuale | Limite sulla metrica `seats` | Prezzo per posto implicito | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|---|
| `free` | — | — | 1 posto | — | — | Chi vende da solo: abbastanza per portarci dentro la propria rubrica e vedere se serve; il secondo venditore è il momento naturale del passaggio |
| `team` | 39 € | 390 € (= 10× il mensile, «due mesi in regalo») | 5 posti | 7,80 €/posto/mese | 14 giorni | La squadra di vendita tipica della micro-impresa: titolare più due o tre venditori |
| `business` | 99 € | 990 € | 20 posti | 4,95 €/posto/mese | 14 giorni | La piccola impresa strutturata, con più responsabili e più imbuti di vendita |

**Note obbligate.**

- Tre piani, non di più: aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile.
- Un limite lasciato vuoto significa **illimitato**, non zero. Qui tutti e tre i limiti sono espliciti: nessun
  piano è a posti illimitati, perché la metrica è l'unica leva di ricavo dell'app.
- **La prova gratuita sul piano `team` non è ridondante** anche se esiste `free`: i due piani si distinguono per il
  numero di posti, e il valore di squadra — assegnazione delle trattative, agenda condivisa, rendimento per
  responsabile — è invisibile a chi è da solo. La prova serve proprio a farlo vedere. Sul piano `business`, invece,
  la prova è più discutibile: chi arriva lì ha già usato `team`.
- **Costo effettivo dell'incasso**: nessun piano è sotto i 5 € al mese, quindi la parte fissa per transazione non
  divora il margine. Segnalo comunque che l'annuale a 390 € e 990 € rende la parte percentuale la voce dominante.
- I prezzi sono **immutabili una volta vivi**: un cambio si fa creando un prezzo nuovo e archiviando il vecchio,
  gli abbonati restano sul loro (skill `pricing-change`).
- **Conseguenza a giacenza**: passare da `team` a `free` con 3 posti occupati dev'essere **bloccato** finché non se
  ne liberano 2. È il comportamento previsto dalla piattaforma e va provato (storia 0004).

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/sales.yaml`) si compila **insieme** allo sviluppatore: «niente contratto, niente
> produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

**Categorie particolari (articolo 9): NO.** LeadGrove non tratta dati sanitari, biometrici, genetici, né opinioni
politiche, convinzioni religiose, orientamento sessuale o appartenenza sindacale. Nessun campo strutturato li
prevede e nessuna funzione li richiede. Restano **due vie d'ingresso non presidiate**, che vanno dichiarate invece
che minimizzate: i campi a **testo libero** (note, titolo dell'attività, motivo di perdita) e i **campi
personalizzati** definiti dal cliente, dove nulla impedisce materialmente a un utente di scrivere «il titolare è in
malattia fino a settembre». Sono le stesse due vie che ha qualunque archivio commerciale; la posizione proposta è
al paragrafo «Testo libero» qui sotto.

**Ruolo.** Il cliente dell'app (l'account) è **titolare** del trattamento dei propri contatti; appgrove agisce come
**responsabile**, esattamente come già dichiarato per il Mini-CRM (`docs/compliance/manifests/crm.yaml`). La base
giuridica verso i contatti è del cliente, non nostra: l'app deve **registrarla**, non sceglierla al posto suo — è
il motivo per cui `ContactPreference` porta un campo «base giuridica» con due valori ammessi e non una casella
booleana.

**Categorie trattate** *(proposta)*

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `contact.name` | `app_sales.contact` | Contatti del cliente | Identità (nome e cognome) | Riconoscere la persona con cui si tratta | Esecuzione del contratto fra appgrove e il cliente (art. 6.1.b), che tratta per conto del titolare | Finché il cliente lo tiene in archivio; cancellazione fisica alla chiusura dell'account |
| `contact.email` | `app_sales.contact` | Contatti del cliente | Recapito | Contattare la persona | Come sopra | Come sopra |
| `contact.phone` | `app_sales.contact` | Contatti del cliente | Recapito | Come sopra | Come sopra | Come sopra |
| `contact.role` | `app_sales.contact` | Contatti del cliente | Dato professionale | Sapere con chi si sta parlando | Come sopra | Come sopra |
| `contact.source` | `app_sales.contact` | Contatti del cliente | Metadato di provenienza | Dimostrare da dove viene il contatto (è la prima domanda di ogni verifica) | Come sopra | Come sopra |
| `contact_preference.*` | `app_sales.contact_preference` | Contatti del cliente | **Prova del consenso** (marca temporale, testo accettato, canale, base giuridica) | Dimostrare la liceità del contatto commerciale | Obbligo di dimostrabilità in capo al titolare (art. 7.1), che noi custodiamo come responsabili | Deve **sopravvivere** alla revoca: la prova di aver avuto il consenso serve dopo che è stato revocato |
| `company.name` | `app_sales.company` | Aziende clienti, e persone fisiche se è una ditta individuale | Identità/denominazione | Anagrafica | Come sopra | Come sopra |
| `note.body` | `app_sales.note` | Chiunque il cliente nomini | **Testo libero** | Memoria della relazione | Come sopra | Come sopra |
| `activity.title` / `activity.outcome` | `app_sales.activity` | Come sopra | **Testo libero** | Cosa è stato fatto e come è andata | Come sopra | Come sopra |
| `deal.loss_reason` | `app_sales.deal` | Come sopra | **Testo libero** | Capire perché si perde | Come sopra | Come sopra |
| `custom_field_value.value` | `app_sales.custom_field_value` | Come sopra | **Definito dal cliente** | Ciò che il cliente ha deciso di aggiungere | Come sopra | Come sopra |
| `form_submission.payload` | `app_sales.form_submission` | Chi compila il modulo pubblico | Quanto la persona ha scritto nel modulo | Prova di cosa è arrivato e quando | Come sopra | **Da decidere**: proposta di 24 mesi dalla ricezione, poi cancellazione del grezzo mantenendo il contatto generato (§2.7) |
| `import_row.payload` | `app_sales.import_row` | Contatti importati | Riga grezza del file caricato | Poter dire all'utente cosa è stato scartato e perché | Come sopra | **Proposta**: 90 giorni dalla fine dell'importazione, poi cancellazione del grezzo |
| `seat.member_id` | `app_sales.seat` | Membri dell'account | Solo identificativo interno | Contare i posti occupati | Come sopra | Fino alla revoca del posto |

**Esportazione e cancellazione.** Tutte e queste tabelle — `company`, `contact`, `contact_preference`, `deal`,
`deal_stage_event`, `activity`, `note`, `custom_field`, `custom_field_value`, `web_form`, `form_submission`,
`import_job`, `import_row`, `seat` — devono comparire **sia** in `exportData` **sia** in `purgeData` del contratto
`SalesDataContract`. Dimenticarne una è il difetto di conformità più probabile: le tre candidate a essere
dimenticate sono `form_submission`, `import_row` e `deal_stage_event`, perché «sembrano log». Non lo sono:
contengono i dati della persona o vi rimandano. La cancellazione è **fisica** e lascia una riga di prova nel
registro delle purghe: sostituire i nomi con dei codici non è cancellare.

**Testo libero.** L'app ha cinque campi a testo libero (note, titolo ed esito dell'attività, motivo di perdita,
valori dei campi personalizzati). Posizione proposta, da confermare: **nessuna rilevazione automatica di
contenuto** — non ispezioniamo ciò che il cliente scrive, sarebbe un trattamento in più per prevenirne un altro —
ma un **avviso testuale accanto ai campi liberi** («non inserire dati sensibili: salute, appartenenza sindacale,
convinzioni personali»), come già fa il mockup. Se un presidio più forte dovesse servire, è un tema trasversale di
piattaforma, non di questa app.

**Integrazioni esterne.** **Nessuna** nel perimetro delle 37 storie: è una scelta deliberata, non una dimenticanza.
Le tre integrazioni che il mercato chiede (posta, calendario, messaggistica — §2.4) introdurrebbero ognuna un
**responsabile esterno del trattamento** e, nel caso della casella di posta, l'accesso all'intera corrispondenza
del cliente: il trattamento più invasivo dell'intera suite. Vanno decise fuori da qui (§11.3). Il modulo web di
acquisizione (storia 0028) è nostro e **non** aggiunge fornitori. L'esportazione in formato calendario (storia
0024) produce un file che l'utente scarica: nessun dato esce verso terzi per iniziativa nostra.

**Classificazione della change.** Una app nuova introduce finalità nuove e categorie nuove: è un cambiamento
**sostanziale**. Confermato, e con un'aggravante rispetto alla media: LeadGrove raccoglie dati di persone che
**non sono ancora clienti** del cliente — la persona che compila un modulo web non ha nessun rapporto contrattuale
con nessuno. È esattamente il caso in cui l'informativa al momento della raccolta e la prova del consenso non sono
formalità (§2.3). La classificazione descrive la realtà, non è una leva per evitare adempimenti.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.
> I nomi degli strumenti restano in inglese perché sono identificatori tecnici, e ricalcano quelli della scheda
> di catalogo; descrizioni e parametri sono in italiano.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `list_contacts` | `(ricerca?, azienda?, responsabile?, pagina?) → elenco di contatti minimizzato` | Restituisce nome, azienda, ruolo e identificativo: **non** restituisce recapiti se non richiesti espressamente | lettura | no |
| `get_contact` | `(id) → scheda del contatto` | Scheda completa con preferenze di contatto e stato del consenso | lettura | no |
| `get_pipeline` | `(pipeline?) → fasi con trattative, conteggi e valore per fase` | La lavagna in forma di dati | lettura | no |
| `list_deals` | `(stato?, fase?, responsabile?, periodo?) → elenco di trattative` | Le trattative aperte, quelle ferme, quelle chiuse nel periodo | lettura | no |
| `list_activities` | `(responsabile?, entro?) → elenco di attività` | «Cosa devo fare oggi» | lettura | no |
| `summarize_account` | `(azienda) → riassunto della relazione` | Trattative, ultime attività, note recenti, stato del consenso: la risposta a «a che punto siamo con Alfa?» | lettura | no |
| `conversion_report` | `(periodo, pipeline?) → imbuto e tassi di conversione` | I numeri dell'epica 06 | lettura | no |
| `create_lead` | `(nome, azienda?, recapito?, origine) → bozza di contatto e trattativa` | Crea un contatto e, facoltativamente, la prima trattativa nella fase iniziale | scrittura | **sì** — la bozza mostra cosa verrà creato, e se il contatto somiglia a uno esistente lo dice prima di duplicare |
| `log_activity` | `(riferimento, tipo, titolo, scadenza?) → bozza di attività` | Registra una chiamata fatta o programma un richiamo | scrittura | **sì** |
| `update_deal_stage` | `(trattativa, fase) → bozza di passaggio di fase` | Sposta la trattativa e scrive lo storico | scrittura | **sì** |
| `close_deal` | `(trattativa, esito, motivo se persa) → bozza di chiusura` | Porta la trattativa in stato terminale ed **emette un evento verso le altre app della suite** | scrittura con effetto verso l'esterno dell'app | **sì, obbligatoria** |
| `export_contacts` | `(filtro) → bozza di esportazione` | Estrae dati personali in massa fuori dall'app: è il punto in cui nasce una lista | scrittura irreversibile | **sì, obbligatoria**, con l'avviso sull'uso della lista (§2.3 punto 4) |

**Lettura.** Gli strumenti che giustificano da soli il livello conversazionale sono `summarize_account`,
`list_activities` e `create_lead`. I primi due rispondono alle due domande che nessun elenco risponde da sé — «a
che punto siamo con questo cliente» e «chi devo richiamare oggi» — e sono di sola lettura, quindi liberi. Il terzo
è la ragione per cui i dati entrano davvero nell'app: dettare un lead appena usciti da un incontro costa dieci
secondi, aprire il portatile e compilare un modulo costa dieci minuti, e la differenza fra i due è la ragione per
cui i CRM piccoli restano vuoti (§2.5). Sul lato opposto, `export_contacts` e `close_deal` sono gli unici due che
hanno effetti fuori dai confini dell'app e per questo hanno la conferma obbligatoria.

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine di questa epica l'app esiste, è accesa, è vuota e si avvia in locale senza passi manuali: schema del
database, rotte, modulo frontend registrato, catena dei varchi funzionante fino al `429`.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | Istanza dello scaffolding: servizio Quarkus, rotte `/api/sales/v1/*`, infrastruttura dal modulo comune |
| [0002](01-fondamenta/0002-modello-dati-multi-account.md) | Modello dati multi-account | Schema `app_sales`, prime tabelle, `tenant_id` e cancellazione logica ovunque |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Manifesto del modulo, registrazione, sezioni, cinque lingue, colore-categoria |
| [0004](01-fondamenta/0004-posti-abbonamento-e-quota.md) | Posti, abbonamento e quota | Metrica `seats` a giacenza, varco a `429`, passaggio a piano inferiore bloccato |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | `./dev.sh services` mostra l'app; dati inventati che la rendono navigabile subito |

### Epica 02 — Anagrafica di contatti e aziende

Il registro condiviso: la scheda che non vive più nella testa di una persona sola. È anche il pezzo che la suite
riusa (§10).

| # | Storia | In una riga |
|---|---|---|
| [0006](02-anagrafica-di-contatti-e-aziende/0006-scheda-azienda.md) | Scheda azienda | Creare, modificare, archiviare un'organizzazione |
| [0007](02-anagrafica-di-contatti-e-aziende/0007-scheda-contatto.md) | Scheda contatto | La persona dentro l'organizzazione, con il suo ruolo e i suoi recapiti |
| [0008](02-anagrafica-di-contatti-e-aziende/0008-elenco-ricerca-e-filtri.md) | Elenco, ricerca e filtri | Trovare una scheda in meno di cinque secondi, con paginazione |
| [0009](02-anagrafica-di-contatti-e-aziende/0009-campi-personalizzati.md) | Campi personalizzati | Pochi campi aggiuntivi, definiti dal cliente, deliberatamente limitati |
| [0010](02-anagrafica-di-contatti-e-aziende/0010-unione-dei-duplicati.md) | Unione dei duplicati | Riconoscere e fondere due schede della stessa persona senza perdere storia |
| [0011](02-anagrafica-di-contatti-e-aziende/0011-preferenze-di-contatto-e-prova-del-consenso.md) | Preferenze di contatto e prova del consenso | Registrare canale, base giuridica, momento e testo accettato; revoca che non cancella la prova |

### Epica 03 — Pipeline e trattative

Il cuore commerciale: dove sono i soldi in ballo e come si muovono.

| # | Storia | In una riga |
|---|---|---|
| [0012](03-pipeline-e-trattative/0012-pipeline-predefinita-e-fasi.md) | Pipeline predefinita e fasi | Un imbuto già pronto il primo giorno, modificabile ma non obbligatorio |
| [0013](03-pipeline-e-trattative/0013-creazione-della-trattativa.md) | Creazione della trattativa | Titolo, valore, azienda, contatto, fase iniziale |
| [0014](03-pipeline-e-trattative/0014-lavagna-a-colonne.md) | Lavagna a colonne | La pipeline visuale con il trascinamento fra le fasi |
| [0015](03-pipeline-e-trattative/0015-storico-dei-passaggi-di-fase.md) | Storico dei passaggi di fase | Ogni movimento lascia una riga: senza, i tempi medi mentono |
| [0016](03-pipeline-e-trattative/0016-chiusura-vinta-o-persa.md) | Chiusura vinta o persa | Esito terminale, motivo di perdita obbligatorio, riapertura tracciata |
| [0017](03-pipeline-e-trattative/0017-valore-atteso-e-previsione.md) | Valore atteso e previsione | Il valore ponderato per la probabilità della fase, con la data attesa |
| [0018](03-pipeline-e-trattative/0018-assegnazione-e-portafoglio-del-responsabile.md) | Assegnazione e portafoglio del responsabile | Di chi è questa trattativa, e la vista «solo le mie» |

### Epica 04 — Attività e storico della relazione

La parte che fa tornare l'utente ogni giorno: cosa devo fare, e cosa ci siamo detti finora.

| # | Storia | In una riga |
|---|---|---|
| [0019](04-attivita-e-storico-della-relazione/0019-attivita-e-scadenze.md) | Attività e scadenze | Chiamate, riunioni e compiti agganciati a una scheda |
| [0020](04-attivita-e-storico-della-relazione/0020-agenda-del-giorno.md) | Agenda del giorno | «Chi devo richiamare oggi», con arretrato in evidenza |
| [0021](04-attivita-e-storico-della-relazione/0021-note-sulle-schede.md) | Note sulle schede | Annotazioni libere con autore e momento, con l'avviso sui dati sensibili |
| [0022](04-attivita-e-storico-della-relazione/0022-cronologia-unificata.md) | Cronologia unificata | Una sola linea del tempo per attività, note e passaggi di fase |
| [0023](04-attivita-e-storico-della-relazione/0023-avviso-di-trattative-ferme.md) | Avviso di trattative ferme | Le trattative senza movimento da troppo tempo emergono da sole |
| [0024](04-attivita-e-storico-della-relazione/0024-esportazione-dell-agenda-in-formato-calendario.md) | Esportazione dell'agenda in formato calendario | Un file scaricabile, senza fornitori esterni |

### Epica 05 — Acquisizione e scambio dei lead

Come i dati entrano e come escono: è anche il punto di massima esposizione sui dati personali.

| # | Storia | In una riga |
|---|---|---|
| [0025](05-acquisizione-e-scambio-dei-lead/0025-importazione-da-file-tabellare.md) | Importazione da file tabellare | Caricare la rubrica esistente con mappatura delle colonne e anteprima |
| [0026](05-acquisizione-e-scambio-dei-lead/0026-duplicati-in-importazione.md) | Duplicati in importazione | Riconoscere ciò che c'è già, invece di raddoppiarlo |
| [0027](05-acquisizione-e-scambio-dei-lead/0027-esportazione-dei-dati.md) | Esportazione dei dati | Uscita in formato tabellare, tracciata, con l'avviso sull'uso della lista |
| [0028](05-acquisizione-e-scambio-dei-lead/0028-modulo-web-di-acquisizione.md) | Modulo web di acquisizione | Un modulo pubblico che crea contatto e trattativa da solo |
| [0029](05-acquisizione-e-scambio-dei-lead/0029-informativa-e-consensi-del-modulo-web.md) | Informativa e consensi del modulo web | Senza informativa e consensi separati il modulo non si pubblica |

### Epica 06 — Report di conversione

Le tre domande a cui un titolare vuole rispondere: quante ne chiudiamo, in quanto tempo, e perché le perdiamo.

| # | Storia | In una riga |
|---|---|---|
| [0030](06-report-di-conversione/0030-imbuto-di-conversione.md) | Imbuto di conversione | Quante trattative passano da ogni fase alla successiva |
| [0031](06-report-di-conversione/0031-rendimento-per-responsabile.md) | Rendimento per responsabile | Chi chiude cosa, senza trasformarlo in una classifica |
| [0032](06-report-di-conversione/0032-origine-dei-lead-e-resa.md) | Origine dei lead e resa | Da dove arrivano quelli che si chiudono davvero |
| [0033](06-report-di-conversione/0033-esportazione-dei-report.md) | Esportazione dei report | I numeri in un file, senza dati personali dentro |

### Epica 07 — Esposizione conversazionale e prove end-to-end

Il contratto degli strumenti e la prova che l'app funziona davvero, dal principio alla fine.

| # | Storia | In una riga |
|---|---|---|
| [0034](07-esposizione-conversazionale-e-prove/0034-contratto-degli-strumenti-di-lettura.md) | Contratto degli strumenti di lettura | Le sette letture dichiarate, con minimizzazione dei dati restituiti |
| [0035](07-esposizione-conversazionale-e-prove/0035-strumenti-di-scrittura-con-bozza-e-conferma.md) | Strumenti di scrittura con bozza e conferma | Nessuna scrittura senza una persona che approva |
| [0036](07-esposizione-conversazionale-e-prove/0036-riassunto-dell-account.md) | Riassunto dell'account | Lo strumento che rende l'app più utile delle sue concorrenti |
| [0037](07-esposizione-conversazionale-e-prove/0037-percorso-end-to-end-e-registro-di-copertura.md) | Percorso end-to-end e registro di copertura | Il percorso `[J-SALES]` e le voci del registro |

**Totale**: 7 epiche, 37 storie (0001-0037).

---

## 9. Estensioni della console di amministrazione

Servono tre cose oltre lo standard: una **vista sui moduli web pubblici** (sono l'unica superficie dell'app
esposta a Internet senza autenticazione, quindi l'unica che può essere inondata di invii falsi), una **vista sulle
importazioni** con l'esito delle righe scartate, e una **deroga temporanea sui posti** per il cliente che sta
migrando da un altro prodotto. Nessuna di queste dà accesso ai contenuti dell'account: si vedono conteggi, stati e
codici di errore.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

Il catalogo (§6) dice che **l'anagrafica clienti condivisa è il cuore del sistema** e che «la stessa scheda cliente
alimenta CRM (4), fatturazione (2, 1), incasso crediti (3), supporto (12), prenotazioni (7) e tutti i verticali».
LeadGrove è l'app numero 4 di quella frase: è il **sistema di origine** dell'anagrafica, cioè il posto dove una
azienda e una persona vengono create per la prima volta, prima ancora che esista un preventivo o una fattura.

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| 06 — Preventivi | alimenta | Una trattativa vinta è il preventivo che sta per nascere: azienda, contatto, valore atteso. È il primo anello della catena del documento contabile |
| 02 — BillGrove (fatturazione) | alimenta / si arricchisce da | L'anagrafica del cliente da fatturare arriva da qui; in senso inverso, lo stato «ha fatture aperte» è ciò che il venditore vuole vedere sulla scheda prima di chiamare |
| 01 — InvoiceGrove (compliance della fattura) | indiretto | Nessun rapporto diretto: passa da 02, di cui il catalogo (§6) raccomanda che sia un livello e non un'app a sé |
| 03 — Incasso crediti | si arricchisce da | Il ritardo di pagamento di un'azienda è un'informazione commerciale: cambia come e se la si richiama |
| 12 — Supporto | condivide | Stessa anagrafica: chi apre un ticket è una persona che qui ha già una scheda |
| 07 — Prenotazioni | condivide | Stessa anagrafica, direzione opposta: un cliente che prenota è un contatto |
| 05 — ChatGrove (vendita conversazionale) | si sovrappone parzialmente | Un messaggio in ingresso che crea o aggiorna una scheda tocca il nostro perimetro: il confine proposto è che ChatGrove possiede il **canale**, LeadGrove possiede la **scheda** |
| 16 — ReachGrove (campagne) | alimenta | Il consenso al marketing raccolto qui è la condizione di liceità dell'invio là. È una sinergia delicata: la prova del consenso deve viaggiare con il contatto, altrimenti l'invio è illecito |
| Mini-CRM (`services/crm`, app reale già nel repo) | **si sovrappone in modo sostanziale** | Contatti, interazioni, fasi, posti: è la stessa materia. Vedi §11.1 |

**Come si condivide, tecnicamente.** Non con chiamate: una app **non chiama** un'altra app
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §2). L'unica via è **asincrona a eventi**. LeadGrove pubblica
quindi gli eventi «contatto creato/aggiornato», «azienda creata/aggiornata» e «trattativa vinta»; le altre app se
ne fanno una proiezione locale. Il contratto di quegli eventi — chi è la fonte di verità quando due app modificano
la stessa azienda, come si risolve un conflitto, cosa succede a una cancellazione — **non esiste ancora** ed è la
cosa che manca alla suite prima che questa sinergia sia reale (§11.4).

**Riga di lettura.** LeadGrove ha senso anche da sola — la categoria è affollata proprio perché il prodotto
autonomo vende — ma il suo argomento **difendibile** è la suite: nessuno dei concorrenti del §2.1 sa dire al
venditore che l'azienda che sta per richiamare ha tre fatture scadute, perché la fatturazione è di qualcun altro.

**Sovrapposizioni da evitare.** Tre, dette adesso:

1. **il Mini-CRM già nel repository** — è la sovrapposizione vera, non una teorica (§11.1);
2. **l'invio delle campagne**: LeadGrove registra il consenso, ReachGrove (16) invia. Se LeadGrove cominciasse a
   spedire, le due app diventerebbero una;
3. **il preventivo**: LeadGrove chiude la trattativa, l'app 06 emette il documento. La tentazione di aggiungere
   «genera preventivo» qui è forte ed è da respingere: sposterebbe la catena del documento contabile dentro
   un'app di vendita.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **Rapporto con il Mini-CRM esistente** (`services/crm`). Nel repository c'è già un'app che fa contatti, interazioni, fasi e posti: è stata costruita come **veicolo** per validare la skill `new-application` e la meccanica dei posti, ed è oggi `status: inactive`, cioè spenta per tutti gli account. LeadGrove ne è il superinsieme. Le tre vie: (a) **LeadGrove è una app nuova con `app_id` `sales`** e il Mini-CRM resta spento come palestra tecnica — è la proposta di questo documento, perché non richiede migrazioni e non tocca codice che oggi serve da riferimento; (b) LeadGrove **eredita `crm`**, il Mini-CRM viene ritirato con `drop-application` e LeadGrove nasce sul suo schema — più pulito a lungo termine, ma è una migrazione di dati e va deciso **prima** dello scaffolding; (c) LeadGrove **estende** il Mini-CRM invece di nascere da zero — sconsigliato: significherebbe far crescere per accrescimento un'app nata per un altro scopo, e le 37 storie qui sotto suppongono un impianto proprio. | **Sviluppatore** — è una decisione di prodotto e di direzione, non di implementazione. Va presa prima della storia 0001 |
| 2 | **Modello di prezzo: piano fisso contro prezzo per posto.** Tutto il mercato vende a posto (§2.2); il listino come codice sa esprimere solo un prezzo per livello con un tetto sulla metrica (§5). Il rischio non è il margine, è la **comparabilità**: il cliente confronta 39 € con «14 $ a utente» e deve fare un conto per capire chi costa meno. Le vie: accettare il modello a livelli e comunicarlo con chiarezza («5 posti inclusi»), oppure introdurre nella piattaforma il prezzo per unità, che è un cambiamento di piattaforma e non di app | **Sviluppatore** (fermata di escalation prezzi), con eventuale ricaduta sull'area pagamenti |
| 3 | **Sincronizzazione di posta e calendario.** È l'integrazione numero uno richiesta dal mercato (§2.4) ed è fuori dal perimetro di queste 37 storie. Il motivo è che introduce un **responsabile esterno del trattamento** con accesso alla corrispondenza del cliente, un flusso di consenso delegato e una dipendenza da piattaforma di terzi. Senza di essa LeadGrove resta un CRM che si compila a mano, cioè esattamente ciò che il §2.5 dice essere il motivo per cui i CRM piccoli restano vuoti: è il rischio di prodotto più serio della proposta | **Sviluppatore** — decisione di prodotto **e** classificazione di dati personali; da riportare come epica separata se accolta |
| 4 | **Contratto degli eventi dell'anagrafica condivisa.** La sinergia del §10 richiede di sapere chi è la fonte di verità di una azienda modificata da due app, come si risolvono i conflitti e cosa propaga una cancellazione. Oggi non esiste nel repository | **Piattaforma** — è un use case di architettura, non di questa app |
| 5 | **Conservazione dei dati grezzi** (`form_submission.payload`, `import_row.payload`). Ho proposto 24 e 90 giorni, ma non esiste un termine di legge (§2.7) e la scelta ricade sul manifesto dati | **Sviluppatore**, in sede di compilazione del manifesto; da rivedere nella revisione legale |
| 6 | **Punteggio automatico dei lead.** Il mercato lo offre; questa proposta **non lo include**. Se venisse aggiunto, un punteggio che determina chi viene richiamato e chi no può ricadere nella disciplina delle decisioni automatizzate e richiede quantomeno trasparenza sulla logica | **Sviluppatore** — decisione di prodotto, con ricaduta sulla classificazione dei dati |
| 7 | **Base giuridica registrata ma non verificata.** L'app registra che il cliente dichiara «legittimo interesse», ma non può verificare che la valutazione di bilanciamento esista davvero. Il confine di responsabilità (noi responsabili, il cliente titolare) va scritto nel contratto di trattamento, non solo nel manifesto | **Revisione legale** (`docs/_REVISIONE-LEGALE.md`) |

**Rischi noti**

- **Mercato affollato e maturo** — il catalogo lo dice già («WTP media-alta, ma mercato affollato»): il rischio è
  costruire il quinto CRM leggero identico agli altri quattro. *Cosa lo attenua*: non competere sulle funzioni ma
  sull'appartenenza alla suite (§10) e sulla partenza a configurazione zero (§2.5).
- **Archivio vuoto** — un CRM in cui nessuno inserisce niente non produce nessun rapporto di conversione, e il
  cliente disdice dopo tre mesi. *Cosa lo attenua*: importazione al primo giorno (0025), modulo web che riempie da
  solo (0028), inserimento dettato dalla chat (0035). Se anche queste tre non bastano, il vero rimedio è la
  sincronizzazione della posta, cioè il punto aperto 3.
- **Esposizione su dati personali più alta della media della suite** — qui i dati riguardano persone che non hanno
  alcun rapporto con noi né, spesso, con il nostro cliente. *Cosa lo attenua*: prova del consenso strutturata
  (0011, 0029), esportazione e cancellazione complete su **tutte** le quattordici tabelle (§6), nessuna
  integrazione esterna nel perimetro.
- **Il modulo web pubblico è la superficie attaccabile** — è l'unico punto dell'app raggiungibile senza
  autenticazione. *Cosa lo attenua*: limite di frequenza per chiave pubblica, disattivazione dalla console di
  amministrazione, nessuna informazione dell'account restituita nella risposta pubblica (0028).

**Fuori dimensionamento**: non applicabile. 7 epiche (fascia 4-7), da 4 a 7 storie ciascuna (fascia 4-8), 37 storie
in tutto (fascia 20-45).
