# BookGrove — descrizione dell'applicazione

**Numero di catalogo**: 07 · **Tipo**: orizzontale · operazioni · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 07](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** BookGrove tiene l'agenda di un'attività che lavora su appuntamento e la apre al pubblico. Dentro
l'applicazione si descrivono i servizi che si vendono (durata, prezzo indicativo, tempo di preparazione fra un
appuntamento e l'altro), le risorse che li erogano (le persone, ma anche una poltrona, una sala, un tavolo, un
macchinario) e gli orari in cui l'attività è disponibile. Da quelle tre cose l'applicazione calcola gli spazi
liberi e li pubblica su una pagina di prenotazione con il marchio del cliente, dove chi vuole un appuntamento
sceglie il servizio, sceglie l'orario e conferma senza doversi registrare. Ogni prenotazione confermata fa
partire i promemoria concordati, e se qualcuno disdice il posto liberato viene offerto a chi era in lista
d'attesa.

**Per chi.** Micro-imprese da 1 a 10 addetti e piccole imprese fino a 50 che vendono tempo su appuntamento:
parrucchieri e centri estetici, studi professionali che ricevono su prenotazione, officine, poliambulatori e
studi odontoiatrici, palestre e studi di personal training, ambulatori veterinari, ristoranti che accettano
prenotazioni al tavolo, noleggi. Compra il titolare; usano tutti i giorni chi sta alla reception e le persone che
erogano il servizio, dal telefono più che dal computer. Mercato globale con priorità europea.

**Quale problema toglie.** Oggi l'agenda di una micro-impresa sta su un quaderno o su un calendario condiviso, e
le prenotazioni arrivano per telefono, per messaggio e di persona. Il costo è di tre tipi. Il primo è il tempo:
ogni prenotazione è una telefonata da interrompere, e le telefonate arrivano mentre si sta lavorando su un
cliente. Il secondo è la doppia prenotazione: due canali che scrivono sullo stesso quaderno prima o poi si
sovrappongono. Il terzo, il più caro, è la **mancata presentazione**: un appuntamento saltato è un'ora di
fatturato persa che non si recupera, e gli studi disponibili misurano riduzioni delle mancate presentazioni
intorno al 25-40 % quando i promemoria automatici sostituiscono la telefonata di cortesia (§2.5). L'alternativa
di mercato più diffusa in Europa — i portali del settore bellezza — risolve il problema ma si fa pagare una
percentuale sul giro d'affari, anche sui clienti che l'attività aveva già: è la lamentela ricorrente del settore
(§2.5) ed è lo spazio che BookGrove occupa.

**Cosa NON fa.**

- **Non è un portale né una vetrina**: non porta clienti nuovi, non mette l'attività in un elenco confrontabile
  con i concorrenti e non prende una percentuale sul giro d'affari. La pagina pubblica è **dell'attività**, non
  di appgrove.
- **Non incassa denaro dei clienti del cliente**: l'acconto si può richiedere e registrare, ma il denaro non
  passa da appgrove (motivo nella sezione 5 e nella storia `0025`).
- **Non emette documenti fiscali**: la ricevuta e la fattura sono delle applicazioni 02 e 01; qui l'appuntamento
  eseguito emette al più un evento.
- **Non tiene la cartella clinica né la scheda tecnica del servizio erogato**: nessun campo per lo stato di
  salute, per la diagnosi, per la terapia o per il trattamento eseguito. È una scelta di perimetro con una
  ragione precisa, ed è spiegata nella sezione 6.
- **Non gestisce turni e presenze del personale**: chi lavora quando è un'informazione che qui serve solo a
  calcolare la disponibilità; la pianificazione dei turni e le timbrature sono dell'applicazione 11.
- **Non fa gestione della trattativa commerciale né campagne**: l'applicazione 04 segue i clienti potenziali,
  l'applicazione 16 fa le campagne. BookGrove manda solo messaggi legati a un appuntamento esistente.

**Rischio di sostituzione da parte dei modelli linguistici.** `rafforzata`. Un assistente generico sa proporre
orari, ma non sa quali sono davvero liberi: la disponibilità nasce dall'incrocio fra regole di apertura,
durata del servizio, tempi di preparazione, ferie, prenotazioni già prese e calendari personali degli operatori.
È un dato proprietario che cambia ogni minuto e che nessun modello può indovinare. In più l'atto che conta —
confermare un appuntamento a una persona e impegnare un'ora di lavoro di qualcun altro — ha bisogno di uno stato
condiviso e di una prova, non di una risposta plausibile. Il livello conversazionale rende l'app **più** utile
(prenotare parlando è il modo naturale di prenotare), ma non la sostituisce.

---

## 2. Mercato e analisi in rete

> Compilata dopo dieci fra ricerche mirate e letture dirette di pagine ufficiali
> ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4). Ciò che non è stato trovato è dichiarato al §2.7.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| SimplyBook.me | Europa (Cipro, Islanda) | motore di prenotazione generico multi-settore, con pagina pubblica, promemoria e moduli aggiuntivi | Free 0 € (50 prenotazioni/mese, 1 operatore); Basic 11,90 €/mese annuale (13,90 mensile) — 100 prenotazioni, 5 operatori; Standard 24,90/29,90 — 500 prenotazioni, 15 operatori; Premium 49,90/59,90 — 2.000 prenotazioni, 30 operatori; prova 14 giorni senza carta | [simplybook.me/en/pricing](https://simplybook.me/en/pricing) — pagina ufficiale |
| Setmore | globale (Stati Uniti) | agenda e prenotazione self-service, molto generoso nel piano gratuito | Free 0 $ — 4 utenti, 200 appuntamenti/mese, **niente promemoria per messaggio breve, niente sincronizzazione del calendario**; Pro 12 $/mese mensile, 5 $/mese se annuale — utenti e appuntamenti illimitati | [setmore.com/pricing](https://www.setmore.com/pricing) — pagina ufficiale |
| Picktime | globale (India) — è la fonte citata dalla scheda di catalogo | prenotazione multi-risorsa (persone, sale, attrezzature) | Free 0 $ — 3 utenti, prenotazioni illimitate, sincronizzazione del calendario **a senso unico**, niente messaggi brevi; Starter 3 $/utente/mese annuale; Pro 2,25 $/utente/mese annuale fino a 10 utenti | [picktime.com/pricing](https://www.picktime.com/pricing) — pagina ufficiale |
| Cal.com | globale, codice aperto | pianificazione appuntamenti, installabile in proprio | Free 0 $ per un utente; Teams 12 $/posto/mese con fatturazione annuale; Organizations 28 $/posto/mese | [schedulingkit.com — guida ai prezzi di Cal.com](https://schedulingkit.com/pricing-guides/cal-com-pricing) — **sito terzo, non pagina ufficiale** |
| Fresha | globale, forte nel settore bellezza europeo | gestionale «gratuito» con incasso integrato | canone dichiarato nullo; trattiene **2,29 % + 0,20-0,25 € per transazione**; secondo una guida italiana di settore, 2.500-4.000 €/anno per un centro medio | [biutify.it — commissioni dei software beauty](https://www.biutify.it/guide/commissioni-prenotazione-beauty-quanto-costano) — **guida di settore, non pagina ufficiale** |
| Booksy | globale, presente in Italia | gestionale più vetrina di quartiere | dal 2024 canone **25-80 €/mese** secondo il numero di operatori, **più 20-30 % sui clienti nuovi** arrivati dalla vetrina; stima 5.760 €/anno per un centro con tre operatori | stessa guida di settore |
| Treatwell | Europa, settore bellezza | portale di prenotazione più gestionale | **20-30 % sui clienti nuovi**, **2-3 % sui clienti già propri** che prenotano dal collegamento; stima 7.000-9.000 €/anno per un centro medio | stessa guida di settore |

**Lettura.** Il mercato è spaccato in due, e le due metà si comportano in modo opposto. I **motori di
prenotazione** (SimplyBook.me, Setmore, Picktime, Cal.com) vendono un canone piatto basso, fra 3 e 30 € al mese,
e limitano o il numero di prenotazioni mensili o il numero di operatori; sono generici e non entrano nel merito
del settore. I **portali del settore bellezza** (Treatwell, Booksy, Fresha) regalano il programma e si fanno
pagare una percentuale sul giro d'affari: per un centro medio italiano il conto annuo va da 2.500 a 9.000 euro,
contro i 600-1.200 euro di un canone fisso. Lo spazio scoperto è quello di un motore di prenotazione **a canone
piatto, senza vetrina e senza percentuali**, che però faccia bene le due cose che i motori generici fanno male: i
promemoria su più canali e il riempimento dei buchi lasciati dalle disdette.

### 2.2 Prezzi praticati nel dominio

- **Unità di misura**: non ce n'è una sola. SimplyBook.me limita **le prenotazioni al mese** (e in parallelo il
  numero di operatori); Picktime e Cal.com vendono **per utente**; Setmore vende un canone piatto per l'intera
  attività; i portali del settore bellezza prendono una **percentuale sul giro d'affari**.
- **Fasce rilevate su pagina ufficiale**: 11,90-49,90 €/mese (SimplyBook.me), 5-12 $/mese piatti (Setmore),
  2,25-3 $/utente/mese (Picktime).
- **Piano gratuito**: presente in tutti e quattro i motori generici esaminati, ed è la norma della categoria. È
  sempre limitato in modo che serva a provare, non a viverci: Setmore toglie i messaggi brevi e la
  sincronizzazione del calendario, Picktime lascia solo la sincronizzazione a senso unico, SimplyBook.me si ferma
  a 50 prenotazioni al mese.
- **Prova gratuita**: 14 giorni in SimplyBook.me, dichiarata senza carta di credito.
- Le fasce indicate dalla scheda di catalogo (**9-25 €/mese piatti per sede**) sono coerenti con quanto rilevato
  sui motori generici europei e molto più basse del costo effettivo dei portali a percentuale.
- **Segnale importante**: i promemoria via messaggio breve sono quasi sempre **fuori dal canone** o riservati ai
  piani alti, perché hanno un costo per messaggio. È il vincolo che pesa di più sulla proposta di listino (§5).

### 2.3 Obblighi normativi del settore

1. **Caparra e penale non sono la stessa cosa, e le conseguenze cambiano.** Nel diritto italiano la *caparra
   confirmatoria* (art. 1385 codice civile) è versata a garanzia: in caso di disdetta chi la riceve la trattiene e
   può, in aggiunta, chiedere il danno maggiore o l'esecuzione del contratto. La *caparra penitenziale* (art.
   1386) è invece il prezzo del diritto di ripensarci: chi disdice la perde, e lì finisce — nessun risarcimento
   ulteriore. **Effetto sul modello dati**: se l'app permette di chiedere una somma alla prenotazione, deve
   registrare **quale delle due è** e con quale testo il cliente finale l'ha accettata, perché è ciò che decide
   cosa succede alla disdetta. Fonti:
   [federalberghicervia.it — caparra e penale, differenze legali e tributarie](https://federalberghicervia.it/news-alberghi-cervia-milano-marittima/notizia/cancellation-policy-caparra-e-penale-differenze-legali-e-tributarie.html) ·
   [euroconsumatori.org — recesso da una prenotazione](https://www.euroconsumatori.org/it/recesso_da_una_prenotazione_alberghiera).
2. **Il ripensamento di quattordici giorni non si applica agli appuntamenti a data fissa.** La direttiva
   2011/83/UE sui contratti a distanza, recepita nel Codice del consumo, esclude il diritto di recesso per i
   servizi «da fornire a una data determinata o in un periodo prestabilito» — alloggio, trasporto, noleggio,
   ristorazione e attività del tempo libero. **Effetto sul prodotto**: un appuntamento prenotato per giovedì alle
   dieci non porta con sé quattordici giorni di ripensamento, ma la **politica di disdetta dell'attività va
   mostrata e accettata prima** della conferma, altrimenti non è opponibile a chi ha prenotato. Fonte:
   [euroconsumatori.org — diritto di recesso nei contratti del tempo libero](https://www.euroconsumatori.org/it/diritto_di_recesso_contratti_del_tempo_libero).
3. **Il servizio prenotato può essere un dato sanitario.** L'art. 9 del regolamento generale sulla protezione dei
   dati vieta in via di principio il trattamento dei dati relativi alla salute, e l'art. 4 punto 15 definisce dato
   relativo alla salute anche quello che riguarda «la prestazione di servizi di assistenza sanitaria» in quanto
   rivela informazioni sullo stato di salute. **Effetto**: il semplice collegamento fra una persona e la
   prestazione «visita dermatologica» è un dato particolare, anche se l'app non conserva nessun referto. È il
   punto più delicato dell'applicazione ed è trattato in modo esteso al §6. Fonti:
   [studiolegalestefanelli.it — art. 9 del regolamento](https://www.studiolegalestefanelli.it/it/art-9-gdpr) ·
   [privacy-regulation.eu — articolo 9](https://www.privacy-regulation.eu/it/9.htm).
4. **I messaggi su canali di messaggistica hanno regole proprie.** La piattaforma di messaggistica di Meta
   consente di iniziare una conversazione **solo** dopo un consenso esplicito della persona e **solo** con un
   modello approvato in anticipo; il promemoria di appuntamento è espressamente uno dei casi ammessi nella
   categoria «di servizio», che deve restare non promozionale. Dal 1° luglio 2025 la tariffazione è **per
   messaggio consegnato** e varia per Paese e per volume mensile. **Effetto sul prodotto**: il consenso al canale
   va raccolto e conservato per ogni cliente finale, i testi vanno gestiti come modelli e non come testo libero, e
   il costo del canale è variabile. Fonte:
   [developers.facebook.com — prezzi della piattaforma di messaggistica](https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing).
5. **Conservazione.** **Non ho trovato** un obbligo generale, europeo o italiano, di conservare le prenotazioni
   per un periodo determinato: l'appuntamento non è un documento fiscale. Diventa rilevante solo quando c'è di
   mezzo una somma versata, e in quel caso il documento che conta è la ricevuta, che questa applicazione non
   emette. Le durate proposte al §6 nascono quindi da un ragionamento di minimizzazione, non da una norma
   rilevata, e sono **da validare**.

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta, ricavato dalle pagine dei concorrenti e dalle differenze fra i loro piani:

1. **Calendario personale (Google, Microsoft, Apple)** — è la funzione che tutti i concorrenti mettono nei piani a
   pagamento e tolgono dal gratuito, segno che è ciò per cui si paga. Serve in due sensi: scrivere gli
   appuntamenti nel calendario dell'operatore e leggere i suoi impegni personali per non offrire orari in cui non
   c'è. **Fornitore esterno che tratterebbe dati per nostro conto**.
2. **Messaggistica e messaggi brevi per i promemoria** — la ragione economica dell'app. **Fornitore esterno**,
   con dati personali di una categoria di interessati nuova (i clienti dei nostri clienti).
3. **Posta elettronica transazionale** — conferme, promemoria, collegamento per gestire la propria prenotazione.
   **Fornitore esterno**, già presente a livello di piattaforma.
4. **Incasso dell'acconto** — atteso, ed è quello che i portali usano come leva. Per appgrove significherebbe
   muovere denaro fra il cliente e il suo cliente: **escluso dal perimetro**, vedi §5 e storia `0025`.
5. **Fatturazione e scontrino** — trasformare l'appuntamento eseguito in un documento. Dentro la suite sono le
   applicazioni 02 e 01, per via di evento; verso l'esterno sarebbe un fornitore terzo.
6. **Anagrafica clienti condivisa** — la stessa scheda cliente usata dall'applicazione 04 e dai verticali (§10).
7. **Videochiamata** per gli appuntamenti a distanza. Bassa priorità, fuori ambito in questa stesura.

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Cosa chiedono:

- **prenotare senza telefonare, a qualsiasi ora**: è il motivo per cui il cliente finale usa la pagina pubblica, e
  di riflesso il motivo per cui l'attività la vuole;
- **promemoria automatici**: gli studi clinici disponibili misurano una riduzione delle mancate presentazioni
  intorno al 38 % con il solo promemoria per messaggio breve, e una rassegna del 2025 indica cali del 25-40 %
  quando i promemoria sono combinati con un contatto digitale prima della visita. Le fonti sono sanitarie e non
  trasferibili tal quali al settore bellezza, ma l'ordine di grandezza è quello;
- **riempire il buco**: quando qualcuno disdice all'ultimo, il valore non sta nel registrarlo, sta nel rimettere
  qualcun altro in quell'ora. È la funzione «lista d'attesa» ed è il vero differenziatore rispetto a un
  calendario condiviso;
- **una pagina con il proprio marchio**, non una scheda dentro un portale.

Cosa rifiutano:

- **la percentuale sui propri clienti**: è la lamentela ricorrente del settore bellezza italiano — «il programma è
  gratis, paghi solo se ti porto clienti» nasconde un costo annuo che per un centro medio supera di tre-cinque
  volte quello di un canone fisso (fonte al §2.1). Chi ha una clientela consolidata al 60 % non vuole pagare a
  percentuale;
- **essere messi in vetrina accanto ai concorrenti**: il portale che porta clienti nuovi porta anche il confronto
  di prezzo;
- **configurazioni lunghe**: se per aprire l'agenda servono due ore di impostazioni, l'attività torna al quaderno.

### 2.6 Fonti consultate

1. **SimplyBook.me — prezzi ufficiali** — [https://simplybook.me/en/pricing](https://simplybook.me/en/pricing) —
   struttura a piani con doppio limite (prenotazioni al mese **e** operatori), prova di 14 giorni senza carta:
   è il concorrente europeo più vicino al perimetro di BookGrove.
2. **Setmore — prezzi ufficiali** — [https://www.setmore.com/pricing](https://www.setmore.com/pricing) — il piano
   gratuito toglie proprio promemoria brevi e sincronizzazione del calendario: conferma che sono quelle due cose
   a fare il valore percepito.
3. **Picktime — prezzi ufficiali** — [https://www.picktime.com/pricing](https://www.picktime.com/pricing) — è la
   fonte citata dalla scheda di catalogo; vende per utente e distingue la sincronizzazione a senso unico da quella
   nei due sensi.
4. **Guida ai prezzi di Cal.com (sito terzo)** —
   [https://schedulingkit.com/pricing-guides/cal-com-pricing](https://schedulingkit.com/pricing-guides/cal-com-pricing) —
   fascia 12-28 $/posto/mese e presenza di una alternativa a codice aperto installabile in proprio: è il rischio
   competitivo da tenere presente per i clienti tecnici.
5. **Biutify — quanto costano davvero le commissioni dei software beauty in Italia** —
   [https://www.biutify.it/guide/commissioni-prenotazione-beauty-quanto-costano](https://www.biutify.it/guide/commissioni-prenotazione-beauty-quanto-costano) —
   percentuali e canoni di Treatwell, Booksy e Fresha, con il confronto contro il canone fisso: è la fonte del
   posizionamento «canone piatto, nessuna percentuale».
6. **Federalberghi Cervia — cancellation policy: caparra e penale** —
   [https://federalberghicervia.it/news-alberghi-cervia-milano-marittima/notizia/cancellation-policy-caparra-e-penale-differenze-legali-e-tributarie.html](https://federalberghicervia.it/news-alberghi-cervia-milano-marittima/notizia/cancellation-policy-caparra-e-penale-differenze-legali-e-tributarie.html) —
   differenza fra caparra confirmatoria e penitenziale: da qui il requisito di registrare **quale** delle due è.
7. **Euroconsumatori — diritto di recesso nei contratti del tempo libero** —
   [https://www.euroconsumatori.org/it/diritto_di_recesso_contratti_del_tempo_libero](https://www.euroconsumatori.org/it/diritto_di_recesso_contratti_del_tempo_libero) —
   esclusione del ripensamento per i servizi a data determinata: la politica di disdetta va accettata prima.
8. **Stefanelli & Stefanelli — art. 9 del regolamento generale sulla protezione dei dati** —
   [https://www.studiolegalestefanelli.it/it/art-9-gdpr](https://www.studiolegalestefanelli.it/it/art-9-gdpr) —
   definizione di dato relativo alla salute comprensiva della prestazione di assistenza sanitaria: è la base
   dell'avviso del §6.
9. **Meta — prezzi della piattaforma di messaggistica per le imprese** —
   [https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing](https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing) —
   tariffazione per messaggio consegnato dal 1° luglio 2025, categorie di conversazione, obbligo di modello
   approvato e di consenso esplicito.
10. **Klara — i promemoria per messaggio riducono le mancate presentazioni del 38 %** —
    [https://www.klara.com/blog/text-message-appointment-reminders-reduce-no-shows-by-38-study-finds](https://www.klara.com/blog/text-message-appointment-reminders-reduce-no-shows-by-38-study-finds)
    e **rassegna sistematica dei promemoria di appuntamento** —
    [https://jhmhp.amegroups.org/article/view/10215/html](https://jhmhp.amegroups.org/article/view/10215/html) —
    ordine di grandezza dell'effetto dei promemoria; entrambe in ambito sanitario.

### 2.7 Cosa NON sono riuscito a determinare

- **Prezzi ufficiali di Treatwell, Booksy e Fresha per l'Italia** — le loro pagine pubbliche non espongono un
  listino: le percentuali e i canoni riportati al §2.1 vengono da una guida di settore, non da una pagina
  ufficiale, e vanno riverificati prima di usarli in un confronto commerciale.
- **Costo effettivo per messaggio in Italia sul canale di messaggistica** — la tabella di Meta è per Paese e per
  fascia di volume e non l'ho letta riga per riga: so che il messaggio «di servizio» costa molto meno di quello
  promozionale e che esiste una finestra gratuita in risposta all'utente, ma **non ho una cifra italiana
  affidabile**. È il dato che manca per dire se i promemoria stiano dentro un canone piatto (§5 e §11, punto 5).
- **Tasso medio di mancata presentazione fuori dalla sanità** — tutti gli studi trovati sono clinici. Per
  parrucchieri, officine e ristoranti ho trovato solo affermazioni di fornitori senza indagine indipendente: non
  le uso.
- **Quanto pesa davvero la sincronizzazione con il calendario nella decisione d'acquisto** — è dedotta dal fatto
  che tutti i concorrenti la tolgono dal piano gratuito, non da una fonte diretta.

---

## 3. Varco d'identità — le risposte pronte per `new-application`

> Queste sei righe sono ciò che la skill `new-application` chiede **prima** di generare qualunque cosa
> ([step-01-identity.md](../../../../.claude/skills/new-application/step-01-identity.md)). L'identificativo
> dell'app finisce nel nome dello schema del database, nei nomi delle code, nella rotta pubblica e nell'istanza
> del modulo di infrastruttura: cambiarlo dopo **non è una rinomina, è una migrazione di dati**.

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `prenotazioni` | Rispetta `^[a-z][a-z0-9_]{0,30}$` (13 caratteri, minuscolo, solo lettere). Segue la convenzione già viva nel repository, dove l'app numero uno è `fatture` e le sorelle di catalogo sono `preventivi` e `crediti`: identificativo tecnico in italiano, riferito a **cosa l'app è**, non al nome commerciale «BookGrove». Schema del database `app_prenotazioni`, rotte `/api/prenotazioni/v1/*`. |
| **Modello utente** | `multi` | L'agenda è per definizione condivisa: in un salone con tre poltrone o in uno studio con due professionisti, «di chi è questo appuntamento», «chi lo ha spostato» e «chi ha segnato che il cliente non si è presentato» sono domande quotidiane, non un lusso. In più il modello `single` non avrebbe modo di rappresentare l'operatore come risorsa distinta dall'utente che usa il programma, che è il cuore del calcolo della disponibilità. Un professionista solo resta rappresentabile: è un account `multi` con un utente e una risorsa. |
| **Porta locale** | `8107` | Convenzione del kit: 8100 + numero di catalogo. Da confermare con `./dev.sh services` al momento dello scaffolding. |
| **Metrica di quota** | `risorse_prenotabili` | È la **sola** cosa che il piano limita: quante risorse (persone, poltrone, sale, macchinari) l'attività tiene aperte alla prenotazione in un dato momento. Cresce esattamente con il valore ricevuto — un'attività con sei operatori vale sei volte una con uno solo — ed è la stessa unità con cui il mercato dei portali si fa pagare (Booksy fascia il canone «secondo il numero di operatori», §2.1), quindi il cliente sa confrontarla. |
| **Natura della metrica** | `stock` | Tetto su ciò che esiste **ora**: «il piano Studio ha tre risorse prenotabili; per aprirne una quarta bisogna chiuderne una o passare di piano». **La scelta è deliberata e va letta insieme all'alternativa scartata.** L'alternativa naturale sarebbe `flow` sulle prenotazioni al mese, ed è quella che il mercato usa (SimplyBook.me, §2.1) e che la scheda di catalogo suggerisce per le attività stagionali. La scarto per una ragione operativa che vale più della coerenza col mercato: quando la quota `flow` si esaurisce, la piattaforma risponde `429` e **blocca la creazione** — e qui la creazione avviene su una pagina pubblica, per mano di un cliente finale che non ha nessun rapporto con appgrove. Un ristorante che a dicembre esaurisce le prenotazioni del piano smetterebbe di prendere prenotazioni nel suo mese migliore, e la persona che si vede rifiutare la prenotazione penserebbe che il locale è pieno. Un tetto **a giacenza** sulle risorse non ha questo effetto: si esaurisce quando l'attività prova ad aprire una risorsa in più, cioè in un momento in cui c'è davanti un utente autenticato che può capire il messaggio e passare di piano. |
| **Colore-categoria e icona** | `green` · icona `calendar-check` (un calendario con un segno di spunta) | Deve essere lo stesso nel listino (`category`) e nel modulo frontend (`accentToken`). Fra le sorelle di catalogo `amber` è già di InvoiceGrove e CashGrove, `teal` di BillGrove e ChatGrove, `blue` di LeadGrove, `violet` di QuoteGrove: `green` è libero ed è il colore giusto per un'app il cui oggetto è lo **spazio libero** — nell'agenda il verde è l'ora disponibile, ed è l'unica app della sestina che non parla di denaro né di documenti. |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Servizio` | ciò che si può prenotare | nome, descrizione, durata, tempo di preparazione prima e dopo, prezzo indicativo, risorse che lo erogano, visibile al pubblico sì/no | no |
| `Risorsa` | chi o cosa eroga il servizio: una persona, una poltrona, una sala, un macchinario | nome, tipo, colore in agenda, utente collegato (facoltativo), attiva sì/no | sì, se è una persona: nome dell'operatore |
| `RegolaDisponibilita` | quando una risorsa è disponibile | risorsa, giorno della settimana, ora di inizio e fine, validità da/a, fuso orario della sede | no |
| `Chiusura` | eccezione che sottrae disponibilità: ferie, festività, malattia, manutenzione | risorsa o intera sede, inizio e fine, motivo (testo breve) | possibile in via indiretta (il motivo è testo libero) |
| `Cliente` | la persona che prenota | nome, contatti (posta elettronica, telefono), lingua preferita, canali di promemoria consentiti, note | **sì**, è l'entità principale di dati personali |
| `Prenotazione` | l'appuntamento | cliente, servizio, risorsa, inizio e fine, stato, origine (pubblica o interna), note del cliente, politica di disdetta applicata, acconto richiesto | sì, per via del cliente e delle note |
| `ListaAttesa` | chi vuole un posto che ora non c'è | cliente, servizio, intervallo di date accettabile, priorità di inserimento, stato | sì |
| `Promemoria` | un messaggio programmato o inviato per una prenotazione | prenotazione, canale, momento previsto, stato, esito, momento di consegna | sì, in via indiretta (il destinatario) |
| `PoliticaDisdetta` | le regole con cui l'attività accetta disdette | finestra libera, penale oltre la finestra, natura della somma richiesta, testo mostrato al cliente | no |
| `Acconto` | la somma dichiarata alla prenotazione e il suo stato | prenotazione, importo, natura (caparra confirmatoria o penitenziale), stato, come è stato incassato fuori dall'app | no |
| `CollegamentoCalendario` | il legame fra una risorsa e un calendario esterno | risorsa, fornitore, identificativo del calendario, gettoni di accesso, stato dell'ultima sincronizzazione | sì, in via indiretta: i titoli degli impegni esterni non si conservano |
| `EventoPrenotazione` | la storia di ciò che è successo a una prenotazione | prenotazione, tipo di evento, momento, chi (utente o cliente finale), indirizzo di rete se dal pubblico | sì: l'indirizzo di rete |
| `PaginaPubblica` | la configurazione della pagina di prenotazione dell'attività | identificativo pubblico, nome mostrato, servizi visibili, testi, lingue, stato | no |

**Le fasce orarie non sono una tabella.** La scheda di catalogo elenca `TimeSlot` fra le entità: qui è una scelta
esplicita **non** memorizzarle. Uno slot libero è il risultato di un calcolo — regole di disponibilità meno
chiusure meno prenotazioni esistenti meno impegni letti dal calendario esterno, filtrato per durata del servizio e
tempi di preparazione — e materializzarlo su tabella significherebbe doverlo ricostruire a ogni modifica di una
qualunque delle quattro sorgenti. Si calcola quando serve, con una finestra limitata, e si memorizza solo ciò che è
un fatto: la prenotazione.

**Relazioni e macchina a stati.** `Servizio` e `Risorsa` stanno in relazione molti-a-molti (una poltrona può fare
taglio e piega; il taglio lo fanno tre operatori su quattro). Una `Prenotazione` lega un `Cliente`, un `Servizio` e
una `Risorsa` in un intervallo di tempo. Gli stati della prenotazione:

```
richiesta ──▶ confermata ──▶ eseguita
    │              │  │
    │              │  └──▶ non_presentato
    │              └──▶ disdetta_dal_cliente | disdetta_dall_attivita
    └──▶ rifiutata (conferma manuale non concessa, o scaduta)
```

`richiesta` esiste solo se l'attività ha scelto la conferma manuale; con la conferma automatica la prenotazione
nasce già `confermata`. `eseguita` e `non_presentato` sono stati finali che si assegnano dopo l'orario, a mano o
per scadenza. Una disdetta libera lo spazio e fa scattare l'offerta a chi è in lista d'attesa. Nessuno stato torna
indietro: uno spostamento non cambia stato, cambia l'orario e lascia una traccia in `EventoPrenotazione`.

**Il fuso orario è parte del modello, non un dettaglio.** Ogni istante si conserva in tempo universale coordinato,
e ogni sede porta il proprio fuso orario: senza questo, il giorno del cambio dell'ora legale l'agenda si sposta di
un'ora e le prenotazioni di quel giorno diventano sbagliate. È un vincolo dichiarato nella storia `0002` e ripreso
da tutte le storie che scrivono orari.

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica
(`deleted_at`); schema `app_prenotazioni`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8).

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il
> file `services/core/src/main/resources/pricing/prenotazioni.yaml`.

**Ragionamento.** Tre numeri guidano la proposta. Primo: la fascia rilevata sui motori di prenotazione europei è
11,90-49,90 €/mese (§2.2), e la scheda di catalogo indica 9-25 €/mese per sede — le due cose combaciano nella
metà bassa. Secondo: i portali a percentuale costano a un centro medio italiano 2.500-9.000 €/anno (§2.1), quindi
un canone annuo di 150-300 € è un argomento di vendita che si spiega in una frase. Terzo: la metrica è a giacenza
sulle risorse prenotabili, quindi i piani si distinguono per **quante persone o postazioni** l'attività tiene
aperte, che è esattamente come il mercato fascia il prezzo.

| Piano | Prezzo mensile | Prezzo annuale | Limite su `risorse_prenotabili` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | 1 risorsa | — | il professionista solo che vuole vedere se la pagina pubblica gli porta prenotazioni: agenda, pagina pubblica e promemoria per posta elettronica, **senza** canale di messaggistica e **senza** sincronizzazione del calendario esterno |
| `studio` | 12 € | 120 € (= 10× il mensile, «due mesi in regalo») | 3 risorse | 14 giorni | lo studio o il piccolo salone con due o tre postazioni: tutto il piano gratuito più promemoria sul canale di messaggistica, sincronizzazione del calendario, lista d'attesa e politica di disdetta |
| `sede` | 25 € | 250 € | 10 risorse | 14 giorni | la sede con più operatori e più sale: come `studio`, più le viste di riempimento e la gestione multi-risorsa completa |

**Note obbligate.**

- **Tre piani, non di più**: aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile.
- Un limite lasciato vuoto significa **illimitato**, non zero: qui nessun limite è vuoto, tutti e tre i piani
  hanno un tetto esplicito.
- **La prova gratuita su un'app che ha già un piano gratuito è in parte ridondante**, ma qui non lo è del tutto:
  il piano gratuito esclude proprio le due funzioni per cui si paga (messaggistica e calendario), e la prova di 14
  giorni serve a farle toccare con mano. Se lo sviluppatore preferisce, si può togliere la prova dal piano
  `studio` e tenerla solo su `sede`.
- **Costo effettivo dell'incasso**: il piano `studio` a 12 €/mese è sopra la soglia dei 5 € sotto la quale la
  parte fissa per transazione pesa in modo insostenibile, ma resta un prezzo basso: spingere l'annuale è il
  rimedio naturale, e il rapporto 10× lo rende conveniente.
- ⚠️ **Il punto debole della proposta è il costo variabile dei promemoria.** La piattaforma vieta l'addebito a
  consumo e impone di **bloccare** al raggiungimento del limite; ma il canale di messaggistica si paga per
  messaggio consegnato (§2.3 punto 4) e la nostra metrica di quota è a giacenza sulle risorse, quindi **non** mette
  un tetto ai messaggi. Un'attività con tre risorse e mille prenotazioni al mese pagherebbe 12 € e ci
  costerebbe un multiplo in messaggi. Tre vie possibili, nessuna delle quali può decidere un agente: (a) i
  promemoria sul canale di messaggistica restano **a carico del cliente**, che collega il proprio contratto con il
  fornitore (è quello che fa l'applicazione 05 del catalogo con la metrica `messaggi_template`); (b) si include
  una dotazione mensile di messaggi che, esaurita, fa **ricadere** i promemoria sulla posta elettronica invece di
  bloccare; (c) si alza il prezzo dei piani che includono la messaggistica. **La raccomandazione è (a)**, perché
  non introduce una seconda metrica di quota e perché è già la scelta di un'altra app della suite — ma è una
  decisione di prodotto e di prezzo, quindi una fermata di escalation (§11, punto 5).
- I prezzi sono **immutabili una volta vivi**: un cambio di prezzo si fa creando un prezzo nuovo, non modificando
  quello esistente.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/prenotazioni.yaml`) si compila **insieme** allo sviluppatore: «niente contratto,
> niente produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

> 🛑 **Attenzione — categorie particolari (articolo 9). Questa applicazione ci passa vicinissimo, e va deciso
> prima di scrivere una riga di codice.** BookGrove non ha, e non deve avere, nessun campo per lo stato di salute:
> niente diagnosi, niente terapia, niente scheda del trattamento. Ma **il collegamento fra una persona e il nome
> del servizio che ha prenotato può essere di per sé un dato relativo alla salute**: il regolamento definisce dato
> sanitario anche quello che riguarda la prestazione di servizi di assistenza sanitaria in quanto rivela
> informazioni sullo stato di salute della persona (§2.3, punto 3). «Mario Rossi, giovedì alle 10, visita
> dermatologica» è un dato particolare tanto quanto un referto, e lo diventa **senza che l'applicazione faccia
> niente di speciale**, per il solo fatto che il nome del servizio lo ha scritto un poliambulatorio invece di un
> parrucchiere.
>
> Lo stesso vale, in misura diversa, per altri due casi: un centro che vende sedute di riabilitazione o di
> psicoterapia, e un'attività il cui catalogo servizi rivela convinzioni religiose o orientamento sessuale.
>
> **Cosa propongo, e cosa non decido.** BookGrove è orizzontale e il suo perimetro dichiarato (§1, «Cosa NON fa»)
> esclude qualunque campo sanitario: questo riduce il rischio ma **non lo elimina**, perché il catalogo servizi è
> testo libero scritto dal cliente. Le tre vie sono: (a) dichiarare che l'uso sanitario è **fuori dalle condizioni
> d'uso** finché non esiste il verticale 23 (CareGrove), che nascerà con base giuridica rafforzata e valutazione
> d'impatto; (b) trattare fin da subito il collegamento persona↔servizio come dato particolare per tutti, con le
> tutele conseguenti su cifratura, accesso e durata; (c) introdurre un interruttore per account «questa attività
> eroga prestazioni sanitarie» che accende le tutele rafforzate solo dove servono. **La raccomandazione è (a) per
> la prima versione e (c) come evoluzione**, perché applicare a un parrucchiere le tutele di un poliambulatorio
> costa senza proteggere nessuno. Ma è una **decisione dello sviluppatore**, non di un agente: comporta una base
> giuridica, una valutazione d'impatto e, con la via (a), una clausola nelle condizioni d'uso. Vedi §11, punto 4.

**Categorie trattate** (proposta)

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `cliente.nome` | `cliente.nome`, `cliente.cognome` | il cliente finale dell'attività | anagrafico | riconoscere chi ha l'appuntamento | esecuzione del contratto fra l'attività e il suo cliente (appgrove è responsabile del trattamento) | 24 mesi dall'ultima prenotazione (proposta, non rilevata da una norma) |
| `cliente.email` | `cliente.email` | cliente finale | contatto | mandare conferma e promemoria, dare accesso alla propria prenotazione | esecuzione del contratto | come sopra |
| `cliente.telefono` | `cliente.telefono` | cliente finale | contatto | promemoria sul canale di messaggistica o messaggio breve | esecuzione del contratto; il **canale** richiede il consenso specifico del fornitore di messaggistica | come sopra |
| `cliente.lingua` | `cliente.lingua` | cliente finale | preferenza | scrivergli nella sua lingua | esecuzione del contratto | come sopra |
| `cliente.note` | `cliente.note` | cliente finale | **testo libero — vedi sotto** | annotazioni dell'attività | legittimo interesse dell'attività | come sopra |
| `prenotazione.servizio` | `prenotazione.servizio_id` → `servizio.nome` | cliente finale | **potenzialmente particolare, vedi l'avviso** | erogare il servizio prenotato | esecuzione del contratto | 24 mesi (proposta) |
| `prenotazione.note_cliente` | `prenotazione.note_cliente` | cliente finale | **testo libero — vedi sotto** | ciò che il cliente scrive prenotando | esecuzione del contratto | 24 mesi |
| `risorsa.nome` | `risorsa.nome` | operatore dell'attività | anagrafico | mostrare chi eroga il servizio | esecuzione del contratto di lavoro, per conto dell'attività | finché la risorsa esiste |
| `evento.indirizzo_rete` | `evento_prenotazione.indirizzo_rete` | chi usa la pagina pubblica | dato di connessione | difesa dalla prenotazione automatica abusiva e prova della disdetta | legittimo interesse, con informativa sulla pagina pubblica | 12 mesi (proposta: l'evento di sicurezza invecchia in fretta) |
| `promemoria.destinatario` | `promemoria.destinatario`, `promemoria.esito` | cliente finale | contatto e metadato di consegna | prova che il promemoria è partito, e perché è fallito | esecuzione del contratto | 12 mesi |
| `lista_attesa.cliente` | `lista_attesa.cliente_id` | cliente finale | anagrafico e contatto | offrire il posto liberato | esecuzione del contratto | fino alla scadenza della richiesta, poi 3 mesi |
| `calendario.identificativo` | `collegamento_calendario.account_esterno` | operatore dell'attività | identificativo di un account esterno | collegare il calendario personale | consenso dell'operatore | finché il collegamento è attivo |

**Esportazione e cancellazione.** Tutte queste tabelle devono comparire **sia** in `exportData` **sia** in
`purgeData` del contratto dati dell'app (`PrenotazioniDataContract`): `cliente`, `prenotazione`, `nota_interna`,
`lista_attesa`, `promemoria`, `evento_prenotazione`, `collegamento_calendario`, `risorsa` (per la parte
anagrafica dell'operatore). Dimenticarne una è il difetto di conformità più probabile in un'app nuova: qui la
candidata a essere dimenticata è `evento_prenotazione`, perché sembra un registro tecnico e invece contiene un
indirizzo di rete. La cancellazione è **fisica**: sostituire il nome del cliente con un codice non è cancellare.
Attenzione a un caso specifico di questa app: cancellare un cliente non deve far sparire l'occupazione del tempo
in agenda, altrimenti la storia dell'attività diventa incoerente — la proposta è che la cancellazione dei dati
del cliente lasci una prenotazione **senza intestatario** negli intervalli già passati e rimuova del tutto quelle
future. Da validare.

**Testo libero.** Ci sono **due** campi nota — quella interna dell'attività sul cliente e quella che il cliente
scrive prenotando — e sono entrambi un ingresso non presidiato per categorie particolari: «allergica al
nichel», «viene per il problema alla schiena», «non vuole l'operatore uomo». L'app non fa rilevazione di
contenuto e il presidio, se servirà, è un tema trasversale; ma il campo che il **cliente finale** compila sulla
pagina pubblica va almeno accompagnato da un avviso esplicito che dica di non scrivere lì informazioni sulla
propria salute. È un requisito della storia `0017`.

**Integrazioni esterne che ricevono dati personali** (dal §2.4), tutte da elencare fra i fornitori che trattano
dati per nostro conto e da citare nell'informativa:

- **fornitore di posta elettronica transazionale** — riceve l'indirizzo del cliente finale: categoria di
  interessati **nuova per la piattaforma**, cioè i clienti dei nostri clienti;
- **fornitore del canale di messaggistica e dei messaggi brevi** — riceve numero di telefono e contenuto del
  promemoria (che contiene data, ora e, se non si sta attenti, il nome del servizio: vedi la minimizzazione
  richiesta dalla storia `0022`);
- **fornitori dei calendari esterni (Google, Microsoft)** — ricevono, se si scrive l'appuntamento sul calendario
  dell'operatore, il fatto che a quell'ora c'è una prenotazione; anche qui il **titolo** dell'evento va
  minimizzato e non deve contenere il nome del servizio per impostazione predefinita (storia `0028`).

**Classificazione della change.** Una app nuova introduce finalità nuove e una categoria di interessati nuova per
la piattaforma: è un cambiamento **sostanziale**, e con l'avviso sull'articolo 9 in testa a questa sezione non c'è
lettura alternativa. La classificazione descrive la realtà, non è una leva per evitare adempimenti.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `verifica_disponibilita` | `(servizio, periodo, risorsa?) → elenco di intervalli liberi` | calcola gli spazi prenotabili senza scrivere nulla | lettura | no |
| `elenca_prenotazioni` | `(periodo, risorsa?, stato?) → elenco minimizzato` | l'agenda del giorno o della settimana, con i contatti oscurati salvo richiesta esplicita | lettura | no |
| `cerca_cliente` | `(testo) → elenco di clienti minimizzato` | ritrova la scheda di chi chiama | lettura | no |
| `riepilogo_mancate_presentazioni` | `(periodo) → conteggi e tasso` | quanto si perde e dove | lettura | no |
| `crea_prenotazione` | `(cliente, servizio, inizio, risorsa?) → bozza di prenotazione` | occupa il tempo di una persona e impegna l'attività verso un cliente | scrittura | **sì** |
| `sposta_prenotazione` | `(id, nuovo_inizio) → bozza dello spostamento` | cambia un impegno già preso con qualcuno | scrittura | **sì** |
| `disdici_prenotazione` | `(id, motivo) → bozza della disdetta` | libera il tempo, può far scattare una penale e un messaggio al cliente | scrittura irreversibile | **sì, obbligatoria** |
| `invia_promemoria` | `(id, canale) → esito dell'invio` | manda un messaggio a una persona fuori da appgrove | scrittura irreversibile | **sì, obbligatoria** |
| `offri_posto_da_lista_attesa` | `(id_posto_liberato) → bozza dell'offerta` | scrive a una o più persone proponendo un orario | scrittura irreversibile | **sì, obbligatoria** |

**Lettura.** `verifica_disponibilita` è la ragione per cui il livello conversazionale rende questa app più utile
delle sue concorrenti: «quando posso mettere la signora Bianchi per un colore, giovedì o venerdì?» è la domanda
che oggi costa una telefonata e trenta secondi di ricerca a mano, ed è una domanda di sola lettura, quindi senza
attriti. Tutto ciò che **impegna il tempo di qualcuno o parla a una persona fuori dall'azienda** passa invece da
una bozza e da un «sì» umano: prenotare per sbaglio è recuperabile, mandare per sbaglio un messaggio a duecento
clienti no.

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine di questa epica l'app esiste, è accesa, vuota e utilizzabile: servizio avviabile in locale, schema del
database con l'isolamento fra account, modulo visibile nella barra laterale, abbonamento e quota funzionanti.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | L'istanza di scaffolding: servizio Quarkus, rotte, infrastruttura dal modulo comune |
| [0002](01-fondamenta/0002-modello-dati-multi-account.md) | Modello dati multi-account | Schema `app_prenotazioni`, prime tabelle, `tenant_id`, tempo in fuso universale |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Manifesto, registrazione, sezioni vuote, cinque lingue, tema chiaro e scuro |
| [0004](01-fondamenta/0004-abbonamento-e-quota.md) | Abbonamento e quota | Catena dei varchi e tetto a giacenza su `risorse_prenotabili` |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | `./dev.sh services` mostra l'app; un salone finto con cui provare tutto |

### Epica 02 — Servizi, risorse e disponibilità

È il motore dell'applicazione: cosa si vende, chi lo eroga, quando, e da lì il calcolo degli spazi liberi.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-servizi-risorse-e-disponibilita/0006-catalogo-dei-servizi.md) | Catalogo dei servizi prenotabili | Durata, tempi di preparazione, prezzo indicativo, visibilità al pubblico |
| [0007](02-servizi-risorse-e-disponibilita/0007-risorse-e-operatori.md) | Risorse e operatori | Persone, poltrone, sale e macchinari; quali servizi eroga ciascuno |
| [0008](02-servizi-risorse-e-disponibilita/0008-orari-e-regole-di-disponibilita.md) | Orari e regole di disponibilità | Orari settimanali per risorsa, con validità nel tempo e fuso della sede |
| [0009](02-servizi-risorse-e-disponibilita/0009-chiusure-ferie-ed-eccezioni.md) | Chiusure, ferie ed eccezioni | Ciò che sottrae disponibilità a una risorsa o all'intera sede |
| [0010](02-servizi-risorse-e-disponibilita/0010-calcolo-degli-spazi-liberi.md) | Calcolo degli spazi liberi | Il motore che incrocia regole, chiusure e prenotazioni e restituisce gli orari |

### Epica 03 — Anagrafica dei clienti e agenda interna

Ciò che l'attività usa tutti i giorni dietro il banco: la scheda del cliente e l'agenda su cui si scrive a mano.

| # | Storia | In una riga |
|---|---|---|
| [0011](03-anagrafica-e-agenda-interna/0011-anagrafica-dei-clienti.md) | Anagrafica dei clienti | La scheda di chi prenota, con contatti, lingua e note |
| [0012](03-anagrafica-e-agenda-interna/0012-manifesto-dati-e-diritti-dell-interessato.md) | Manifesto dati e diritti dell'interessato | Manifesto in due lingue, esportazione e cancellazione fisica |
| [0013](03-anagrafica-e-agenda-interna/0013-agenda-multi-risorsa.md) | Agenda multi-risorsa | La vista giorno e settimana con una colonna per risorsa |
| [0014](03-anagrafica-e-agenda-interna/0014-prenotazione-dal-banco.md) | Prenotazione dal banco | Creare e modificare un appuntamento a mano, senza doppie prenotazioni |
| [0015](03-anagrafica-e-agenda-interna/0015-esiti-della-prenotazione.md) | Esiti della prenotazione | Eseguita, non presentato, disdetta: gli stati finali e la loro traccia |

### Epica 04 — Prenotazione self-service del cliente finale

La superficie pubblica: la pagina su cui prenota chi non è un utente di appgrove. È l'epica che contiene la
deviazione architetturale dell'applicazione (§11, punto 3).

| # | Storia | In una riga |
|---|---|---|
| [0016](04-prenotazione-self-service/0016-pagina-pubblica-e-identificativo-di-sede.md) | Pagina pubblica e identificativo di sede | La pagina di prenotazione dell'attività e da dove arriva il `tenant_id` senza token |
| [0017](04-prenotazione-self-service/0017-prenotazione-dalla-pagina-pubblica.md) | Prenotazione dalla pagina pubblica | Scelta del servizio e dell'orario, dati di contatto, conferma |
| [0018](04-prenotazione-self-service/0018-gettone-di-gestione-della-prenotazione.md) | Gettone di gestione della prenotazione | Il collegamento con cui il cliente sposta o disdice la propria prenotazione |
| [0019](04-prenotazione-self-service/0019-difese-della-superficie-pubblica.md) | Difese della superficie pubblica | Limiti di frequenza, verifica del contatto, prenotazioni fasulle |
| [0020](04-prenotazione-self-service/0020-lista-d-attesa.md) | Lista d'attesa | Chi vuole un posto che ora non c'è, e come glielo si offre quando si libera |

### Epica 05 — Promemoria, acconti e mancate presentazioni

La ragione economica dell'app: far venire le persone all'appuntamento, e sapere quanto costa quando non vengono.

| # | Storia | In una riga |
|---|---|---|
| [0021](05-promemoria-acconti-e-mancate-presentazioni/0021-canali-di-recapito-e-consenso.md) | Canali di recapito e consenso | Quali canali si possono usare per ogni cliente, e la prova del consenso |
| [0022](05-promemoria-acconti-e-mancate-presentazioni/0022-promemoria-automatici.md) | Promemoria automatici | Il motore che programma e manda i promemoria, con la lingua giusta |
| [0023](05-promemoria-acconti-e-mancate-presentazioni/0023-canale-di-messaggistica.md) | Canale di messaggistica | Modelli approvati, consenso specifico e ricaduta sulla posta elettronica |
| [0024](05-promemoria-acconti-e-mancate-presentazioni/0024-politica-di-disdetta.md) | Politica di disdetta | Finestra libera, penale oltre la finestra, testo accettato prima di confermare |
| [0025](05-promemoria-acconti-e-mancate-presentazioni/0025-acconto-richiesto.md) | Acconto richiesto | La somma chiesta alla prenotazione, registrata a mano: appgrove non incassa |
| [0026](05-promemoria-acconti-e-mancate-presentazioni/0026-indicatori-di-riempimento.md) | Indicatori di riempimento | Tasso di mancata presentazione, ore vendute, buchi recuperati |

### Epica 06 — Sincronizzazione con i calendari esterni

Ciò per cui i concorrenti si fanno pagare: non offrire un'ora in cui l'operatore ha già un impegno suo.

| # | Storia | In una riga |
|---|---|---|
| [0027](06-sincronizzazione-calendari/0027-collegamento-del-calendario-esterno.md) | Collegamento del calendario esterno | L'operatore autorizza il proprio calendario, con consenso e revoca |
| [0028](06-sincronizzazione-calendari/0028-scrittura-degli-appuntamenti.md) | Scrittura degli appuntamenti sul calendario | L'appuntamento compare sul calendario dell'operatore, con titolo minimizzato |
| [0029](06-sincronizzazione-calendari/0029-lettura-degli-impegni-esterni.md) | Lettura degli impegni esterni | Gli impegni personali sottraggono disponibilità senza rivelare di cosa si tratta |
| [0030](06-sincronizzazione-calendari/0030-abbonamento-in-sola-lettura.md) | Abbonamento in sola lettura | Il ripiego per chi non vuole autorizzare nulla: un calendario da sottoscrivere |

### Epica 07 — Esposizione conversazionale e prove end-to-end

Il contratto degli strumenti e i due percorsi che dimostrano che l'app funziona davvero.

| # | Storia | In una riga |
|---|---|---|
| [0031](07-esposizione-conversazionale-e-prove/0031-strumenti-di-lettura.md) | Strumenti di lettura | Disponibilità, agenda, clienti e indicatori, minimizzati |
| [0032](07-esposizione-conversazionale-e-prove/0032-strumenti-di-scrittura-con-conferma.md) | Strumenti di scrittura con conferma | Bozza e «sì» umano per tutto ciò che impegna tempo o parla al cliente |
| [0033](07-esposizione-conversazionale-e-prove/0033-percorso-end-to-end-interno.md) | Percorso end-to-end interno | `[J-BOOKGROVE]`: dal servizio all'appuntamento eseguito |
| [0034](07-esposizione-conversazionale-e-prove/0034-percorso-end-to-end-del-cliente-finale.md) | Percorso end-to-end del cliente finale | `[J-BOOKGROVE-PUB]`: la pagina pubblica, dalla prenotazione alla disdetta |

**Totale**: 7 epiche, 34 storie (`0001`-`0034`).

---

## 9. Estensioni della console di amministrazione

Servono tre cose oltre allo standard: una vista sullo **stato dei collegamenti ai calendari esterni** (è la prima
causa di segnalazione prevedibile, e si diagnostica solo con i metadati della sincronizzazione), una vista sulla
**coda dei promemoria** (arretrato ed errori di recapito), e una **deroga temporanea** sul tetto delle risorse
prenotabili per il cliente che sta migrando la propria agenda e per pochi giorni ne ha aperte più di quante ne
preveda il piano. Niente di tutto questo dà accesso ai contenuti dell'account.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| **04 — LeadGrove (CRM e trattative)** | condivide dati con | L'**anagrafica clienti condivisa**, che il catalogo (§6) indica come il cuore della suite: la stessa scheda cliente alimenta le trattative, la fatturazione, l'incasso, il supporto e le prenotazioni. BookGrove **crea** clienti finali — è spesso il primo punto in cui una persona entra nei dati dell'azienda, perché ha prenotato prima di comprare qualsiasi cosa — e li deve poter riconoscere se esistono già. Oggi però la condivisione non ha un meccanismo: le app non si chiamano fra loro e l'unica via è a eventi (§11, punto 7). |
| **02 — BillGrove (fatturazione)** e **01 — InvoiceGrove** | alimenta | L'appuntamento portato a `eseguita` emette un evento che a valle può diventare una ricevuta o una fattura. BookGrove non emette documenti fiscali. |
| **05 — ChatGrove (vendita in chat)** | si sovrappone in parte, e va coordinata | Entrambe parlano al cliente finale su canale di messaggistica e devono raccogliere un consenso. ChatGrove usa la metrica `messaggi_template` e possiede il rapporto col fornitore: la raccomandazione del §5 è che sia **ChatGrove a possedere il canale** e BookGrove a usarlo, invece di costruire due volte la stessa integrazione. Da coordinare, non deciso. |
| **11 — ShiftGrove (turni e timbrature)** | si sovrappone al confine | Entrambe sanno «chi c'è quando». Qui la disponibilità serve a calcolare gli spazi prenotabili; là serve a pianificare e a pagare. Il confine proposto: BookGrove tiene le regole di disponibilità della **risorsa**, ShiftGrove i turni della **persona**; se convivono, i turni diventano una sorgente di disponibilità. |
| **12 — DeskGrove (supporto)**, **17 — RepGrove (recensioni)** | alimenta | Un appuntamento eseguito è il momento naturale per chiedere una recensione. BookGrove emette l'evento, non manda la richiesta. |
| **21 — SalonGrove (beauty)**, **22 — DineGrove (ristorazione)**, **23 — CareGrove (cliniche)**, **27 — FitGrove (palestre)**, **58 — VetGrove (veterinaria)** | **è la base riutilizzabile di tutti** | La scheda di catalogo lo dice esplicitamente: BookGrove «è la base riutilizzabile per i verticali beauty, clinica e fitness». Il motore di disponibilità, la pagina pubblica, i promemoria e la lista d'attesa sono gli stessi in tutti e cinque; cambiano il vocabolario (poltrona, tavolo, sala visite, campo, box), le regole (il tavolo si prenota per numero di coperti, la sala visite per operatore **e** attrezzatura) e — questa è la differenza che conta — **il regime dei dati personali**, perché 23 e 58 entrano nell'articolo 9. |

**Lettura.** BookGrove ha senso da sola: un parrucchiere che non compra nient'altro ci trova già il suo ritorno.
Ma è anche l'app con la **maggiore leva di riuso** dell'intero catalogo, perché cinque verticali su trenta la
contengono. Questo cambia il modo di scriverla: le regole specifiche di un settore (coperti, sale operative,
abbonamenti a lezioni) **non** vanno dentro BookGrove, o i verticali erediteranno un motore pieno di casi
particolari che non li riguardano. Il criterio proposto: BookGrove tiene ciò che è vero per tutti — tempo,
risorsa, servizio, cliente, promemoria — e i verticali aggiungono il proprio vocabolario e le proprie regole.

**Sovrapposizioni da evitare.** Tre, tutte già segnalate sopra: il canale di messaggistica (con 05), la
disponibilità delle persone (con 11) e l'anagrafica clienti (con 04). In tutti e tre i casi il rischio non è
tecnico ma di prodotto: costruire due volte la stessa cosa e poi doverle tenere d'accordo.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **Prezzi, limiti dei piani e durata della prova** (§5) | è una fermata di escalation della piattaforma: nessun agente fissa prezzi | sviluppatore, prima dello scaffolding |
| 2 | **La metrica di quota è `risorse_prenotabili` a giacenza e non le prenotazioni al mese** (§3) | è motivata (una quota a consumo bloccherebbe una prenotazione fatta da un cliente finale sulla pagina pubblica), ma va contro l'uso di mercato e contro il suggerimento della scheda di catalogo per le attività stagionali | sviluppatore, insieme al punto 1 |
| 3 | **La superficie pubblica rompe l'invariante «`tenant_id` solo dal token verificato»** | vedi il riquadro qui sotto: è la decisione architetturale più importante dell'applicazione | sviluppatore, prima della storia `0016` |
| 4 | **Articolo 9: il nome del servizio prenotato può essere un dato sanitario** (§6) | tre vie possibili (escludere l'uso sanitario dalle condizioni d'uso, applicare le tutele a tutti, accenderle per account); comporta base giuridica rafforzata e valutazione d'impatto | sviluppatore, con supporto legale, prima della storia `0012` |
| 5 | **Chi paga i messaggi dei promemoria** (§5) | il canale di messaggistica si paga per messaggio, la piattaforma vieta l'addebito a consumo e la metrica di quota non limita i messaggi. La raccomandazione è che il cliente colleghi il proprio contratto col fornitore, come fa l'app 05, ma è una decisione di prodotto e di prezzo | sviluppatore, prima della storia `0023` |
| 6 | **L'acconto senza movimento di denaro sarà accettato dal mercato?** | i portali usano proprio l'incasso dell'acconto come leva anti-disdetta; qui l'acconto si dichiara e si registra a mano perché appgrove non muove denaro fra il cliente e il suo cliente. È un effetto verso l'esterno e una scelta di perimetro | sviluppatore, prima della storia `0025` |
| 7 | **Anagrafica clienti condivisa con l'app 04** (§10) | il catalogo la indica come il cuore della suite, ma le app non si chiamano fra loro e non esiste ancora un meccanismo di riconciliazione delle schede fra due schemi diversi. Nel frattempo BookGrove tiene la propria anagrafica ed emette eventi | epica di piattaforma, non ancora scritta |
| 8 | **Chi possiede il canale di messaggistica fra BookGrove e ChatGrove** (§10) | costruirlo due volte è lo spreco più prevedibile del catalogo | sviluppatore, in sede di sequenza di costruzione |
| 9 | **Fusi orari e cambio dell'ora legale** (§4) | la scelta proposta — istanti in tempo universale coordinato più fuso della sede — è tecnicamente corretta ma va presidiata su ogni storia che scrive orari, e ha casi limite noti (l'ora che non esiste e l'ora che esiste due volte) | storia `0002`, poi vincolo permanente |
| 10 | **Il livello conversazionale non esiste** (§7) | epica `12-ready-for-ai-mcp` (UC 0061-0066), scritta e non implementata: le storie `0031` e `0032` dichiarano il contratto e si fermano lì | epica di piattaforma |

### Il punto 3 per esteso — la prenotazione senza autenticazione

L'invariante numero uno della piattaforma dice che l'identificativo dell'account arriva **solo** dal token di
accesso verificato. La pagina di prenotazione pubblica non ha un token: chi la apre è una persona che non ha
nessun rapporto con appgrove e spesso nemmeno con l'attività. Serve quindi una deviazione, e va approvata, non
decisa da una storia.

**Cosa ha fatto l'app 06 (QuoteGrove) e dove mi allineo.** QuoteGrove ha lo stesso problema con la pagina su cui
il cliente legge e accetta un preventivo, e lo risolve con un **gettone di capacità firmato dal server**: monouso
nello scopo, a scadenza, revocabile, che dà accesso a un solo documento in sola lettura più l'atto di accettarlo.
Il `tenant_id` è dentro il gettone, firmato, e non arriva mai dalla richiesta. **Mi allineo su questo per la
gestione della prenotazione già esistente** (storia `0018`): il collegamento che il cliente riceve dopo aver
prenotato è esattamente un gettone di capacità di quel tipo, legato a **una** prenotazione, che consente di
vederla, spostarla o disdirla e nient'altro.

**Dove serve un'impostazione diversa, e perché.** La differenza fra le due app è che il destinatario di QuoteGrove
è **noto prima**: l'azienda ha scritto il preventivo per lui e gli ha mandato il collegamento. Il visitatore di
BookGrove è **ignoto**: la pagina di prenotazione deve essere raggiungibile da chiunque, condivisibile, stampabile
su un biglietto da visita e indicizzabile se l'attività lo vuole. Un gettone segreto è la cosa sbagliata: se è
segreto non si può pubblicare, e se si pubblica non è più segreto. Propongo quindi **due meccanismi distinti**,
con proprietà opposte e per questo con difese diverse:

1. **Identificativo pubblico di sede** — non è un segreto e non è una credenziale. È un nome pubblicato
   dall'attività (`/p/salone-da-lucia`) che il server risolve **lato suo** in un `tenant_id` e in una
   configurazione di pagina pubblica. Non arriva mai dal corpo della richiesta: arriva dal percorso, e il server è
   l'unico a sapere a quale account corrisponda. Ciò che concede è **il minimo indispensabile e nient'altro**: i
   servizi che l'attività ha marcato come pubblici, gli spazi liberi in una finestra limitata, e il diritto di
   **proporre** una prenotazione. Non consente di leggere nessuna prenotazione esistente, nessun nome, nessun
   dato di nessun altro. Poiché non è un segreto, le difese non possono essere la segretezza: sono limiti di
   frequenza per indirizzo di rete, verifica del contatto prima che la prenotazione diventi ferma, risposte
   indistinguibili per gli identificativi inesistenti, e nessuna enumerazione (storia `0019`).
2. **Gettone di capacità per la singola prenotazione** — questo sì è un segreto, ed è il meccanismo di QuoteGrove
   senza modifiche: firmato dal server, legato a una sola prenotazione, con scadenza propria, revocabile, mai
   convertito in una sessione, memorizzato come impronta e non in chiaro.

**Perché la scelta della metrica di quota sostiene questa impostazione.** Con una quota a consumo sulle
prenotazioni, un visitatore anonimo potrebbe — per sbaglio o per malizia — **esaurire la quota dell'account** con
prenotazioni fasulle, e a quel punto il cliente vero si vedrebbe rifiutare la prenotazione. Con una quota a
giacenza sulle risorse, la superficie pubblica **non tocca la quota**: il danno massimo di un abuso è sporcare
l'agenda, che si ripulisce, non spegnere il servizio. È una proprietà che non nasce per caso e va tenuta ferma se
qualcuno riaprirà la discussione sulla metrica (punto 2).

**Rischi noti**

- **La superficie pubblica è la parte esposta dell'applicazione** — prenotazioni automatiche in massa, agende
  riempite di appuntamenti falsi, indirizzi altrui usati per prenotare. Effetto: l'attività perde fiducia nello
  strumento più in fretta di quanto la guadagni. Attenuazione: verifica del contatto prima della conferma, limiti
  di frequenza, conferma manuale attivabile, e la separazione fra `richiesta` e `confermata` nella macchina a
  stati (storia `0019`).
- **Il promemoria che non arriva è peggio di nessun promemoria** — se l'attività smette di telefonare perché «ci
  pensa il programma» e il programma sbaglia, la mancata presentazione è colpa nostra. Attenuazione: stato ed
  esito di ogni promemoria visibili in agenda, ricaduta automatica su un secondo canale, e nessuna promessa di
  consegna che non possiamo mantenere (storie `0022` e `0023`).
- **Doppia prenotazione sotto concorrenza** — due persone che scelgono lo stesso spazio nello stesso istante, una
  dal banco e una dalla pagina pubblica. È il difetto più imbarazzante possibile per un'app di prenotazioni.
  Attenuazione: il vincolo di non sovrapposizione sta nel database e non nel programma applicativo, e il conflitto
  si risolve dando torto a chi arriva secondo, con un messaggio chiaro (storie `0014` e `0017`).
- **Il fornitore del calendario esterno cambia le regole** — è successo a tutte le integrazioni di questo tipo.
  Attenuazione: la sincronizzazione è un di più, non il cuore; l'app resta pienamente utilizzabile senza (storia
  `0030` è il ripiego dichiarato).
- **Il verticale sanitario entra dalla finestra** — un poliambulatorio compra BookGrove perché costa meno di
  CareGrove e ci mette dentro dati sanitari. Attenuazione: la decisione del punto 4, qualunque sia, va presa
  **prima** del rilascio e scritta nelle condizioni d'uso; dopo è tardi.

**Fuori dimensionamento**: nessuno. 7 epiche (fascia raccomandata 4-7), da 4 a 6 storie per epica (fascia 4-8),
34 storie in tutto (fascia 20-45).
