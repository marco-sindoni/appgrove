# ChatGrove — descrizione dell'applicazione

**Numero di catalogo**: 05 · **Tipo**: orizzontale · vendita conversazionale (commercio via messaggistica) · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 05](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** ChatGrove trasforma le conversazioni di messaggistica di un negozio in un flusso di vendita
ordinato: raccoglie i messaggi in arrivo in una casella condivisa, tiene il catalogo dei prodotti con i prezzi,
costruisce il carrello dentro la conversazione, ne ricava un **ordine** con un numero e uno stato, emette una
**richiesta di pagamento** con il collegamento del fornitore di incassi già usato dal negozio e registra
l'incasso quando arriva. Attorno a questo nucleo stanno la rubrica dei contatti con il loro consenso, gli invii
massivi fatti con i **modelli di messaggio approvati** dal fornitore del canale, il recupero dei carrelli
lasciati a metà e le risposte automatiche fuori orario.

**Per chi.** Micro-imprese di 1-10 addetti che vendono già in chat e non hanno un negozio in rete: sartorie,
pasticcerie, ricambisti, rivenditori di elettronica, servizi a domicilio. Compra il titolare; usano tutti i
giorni una o due persone che rispondono ai messaggi. Il perimetro dichiarato dal catalogo è **mercati emergenti
per primi** (India, Sud-est asiatico, America Latina), dove la messaggistica è il canale di vendita primario;
in Europa il pubblico è più stretto ma esiste, soprattutto nel commercio locale.

**Quale problema toglie.** Oggi il negozio vende dentro una chat personale: l'ordine è una sequenza di messaggi,
il prezzo è ricordato a memoria, il conto si fa a mano, e quando due persone rispondono dallo stesso telefono
non si sa più chi ha promesso cosa. Il costo di questo modo non è il tempo: sono gli **ordini persi** — la
conversazione scorre, il cliente non riceve risposta, l'affare evapora. Le analisi di settore lo dicono in modo
esplicito: i piccoli venditori credono di perdere vendite sul prezzo, mentre le perdono per **attrito e
ritardo** (fonte 8, §2.6). ChatGrove dà una forma a quella conversazione senza spostare il cliente altrove.

**Cosa NON fa.**

- **non è una vetrina in rete** né un carrello sul sito: la vendita resta dentro la chat (il negozio in rete è
  un'altra app del catalogo);
- **non è un fornitore di incassi**: non prende soldi, non custodisce carte, non fa da intermediario di
  pagamento. Emette una richiesta di pagamento che punta allo strumento di incasso che il cliente ha già;
- **non è il canale di messaggistica**: non sostituisce il fornitore del canale, si collega a quello che il
  negozio ha già attivato (vedi la scelta d'architettura al §5);
- **non è un centro assistenza**: la casella condivisa serve a vendere, non a gestire richieste di supporto
  con priorità, tempi di risposta e riassegnazioni (è l'app 12 del catalogo);
- **non è un contabile**: registra l'incasso e lo espone, non produce il documento fiscale (app 1 e 2);
- **non fa spedizioni** né tracciamento del corriere nella prima versione.

**Rischio di sostituzione da parte dei modelli linguistici.** `rafforzata`. Un assistente generico sa scrivere
un messaggio di vendita, ma non conosce il listino del negozio, non sa se il prodotto c'è, non può aprire una
richiesta di pagamento e non ha memoria di che cosa quel cliente ha comprato tre mesi fa. Il valore sta nei dati
proprietari (catalogo, contatti, storico ordini), nel collegamento al canale e nella disciplina degli invii
(consenso, modelli approvati, finestra di risposta). Il livello conversazionale non erode questa app: **è il suo
modo naturale d'uso**, ed è la ragione per cui il catalogo la definisce l'idea più nativamente adatta
all'intelligenza artificiale fra le prime trenta.

---

## 2. Mercato e analisi in rete

> Compilata dopo **dieci** ricerche mirate ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4).
> Ciò che non è stato trovato è **dichiarato** al §2.7, non colmato a intuito.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| Wati | Hong Kong / India, globale | Casella condivisa, invii massivi, costruttore di automazioni, catalogo | **49 $/mese** (3 utenti), **99 $/mese** (5 utenti, +24 $ per utente), **199 $/mese**; prova **7 giorni**; formula a consumo 999 ₹ una tantum | [wati.io/pricing](https://www.wati.io/pricing/) (pagina ufficiale) |
| AiSensy | India | Invii massivi, automazioni, agente conversazionale, utenti attivi illimitati | **2.500 ₹/mese** (costruttore di automazioni), **3.500 ₹/mese** (agente); annuale −10 %; piano gratuito con limiti d'uso equo; messaggi pre-pagati a parte | [aisensy.com/pricing](https://aisensy.com/pricing) (pagina ufficiale) |
| Interakt | India | Commercio in chat con catalogo e ordini, orientato ai piccoli negozi | **Non rilevato**: prezzo non estratto da pagina ufficiale in questa ricerca | [confronto AiSensy](https://aisensy.com/aisensy-vs-interakt-vs-wati) (fonte di parte, non ufficiale) |
| 360dialog | Berlino (Unione Europea) | Accesso al canale con impostazione dei dati in Europa, orientato agli sviluppatori | **Non rilevato** su pagina ufficiale | [rassegna dei fornitori europei](https://engrana.es/en/blog/best-whatsapp-business-solution-providers-europe) |
| Infobip | Croazia (Unione Europea) | Piattaforma di messaggistica completa, con residenza dei dati in Europa | **Non rilevato**: listino su richiesta | [rassegna dei fornitori europei](https://engrana.es/en/blog/best-whatsapp-business-solution-providers-europe) |

**Lettura.** Il segmento è affollato ma stratificato in modo netto. I prodotti indiani (AiSensy, Interakt)
vendono a **25-35 € equivalenti al mese** e parlano ai micro-negozi; i prodotti globali (Wati) partono da
**49 $** e vendono a squadre di assistenza, non a un pasticciere con due addetti. Sopra ci sono le piattaforme
di messaggistica europee (Infobip, Bird, Sinch, 360dialog), che vendono **il canale**, non il flusso di vendita.
Lo spazio scoperto è quello di sempre nel catalogo appgrove: **il micro-negozio che vuole ordini e pagamenti,
non una console di marketing**. La differenza difendibile non è il prezzo, è il perimetro: catalogo → carrello →
ordine → richiesta di pagamento in un'unica catena, comandabile da una chat, dentro una suite che possiede già
l'anagrafica clienti e la fatturazione.

### 2.2 Prezzi praticati nel dominio

**Rilevato su pagine ufficiali.** Wati: 49 / 99 / 199 $ al mese, unità di misura **per posto di lavoro** (3-5
utenti inclusi, utenti aggiuntivi a 24-69 $), prova di **7 giorni**, nessun piano gratuito permanente.
AiSensy: 2.500 / 3.500 ₹ al mese (≈ 26 / 37 € al cambio di agosto 2026), unità di misura **per numero
collegato**, utenti attivi illimitati, sconto **10 %** sull'annuale, esiste un piano gratuito con limiti d'uso
equo, **messaggi pagati a parte in pre-pagato**.

**Il dato che conta più del listino: il costo variabile del canale.** Dal 1° luglio 2025 il fornitore fattura
**per messaggio** e non più per conversazione, con quattro categorie — *marketing*, *utility*,
*authentication*, *service* ([documentazione ufficiale](https://developers.facebook.com/docs/whatsapp/pricing/)).
I messaggi di **servizio** (risposte dentro la finestra di 24 ore aperta dal cliente) sono **gratuiti**; i
modelli *utility* sono gratuiti dentro la stessa finestra; i modelli **marketing si pagano sempre**. Tariffe
base 2026 rilevate (senza il ricarico del rivenditore, che aggiunge 0,003-0,010 $ a messaggio):

| Paese del destinatario | Marketing | Utility |
|---|---|---|
| India | ≈ 0,0103 $ (1,09 ₹, tariffa ufficiale AiSensy da gennaio 2026) | ≈ 0,0014 $ (0,145 ₹) |
| Brasile | ≈ 0,0625 $ | ≈ 0,0080 $ |
| Indonesia | ≈ 0,0271 $ | ≈ 0,0036 $ |
| Stati Uniti | 0,025 $ | — |
| Germania | > 0,11 € | — |

**Conseguenza diretta sul listino.** Mille messaggi promozionali al mese costano **≈ 10 $ in India** e
**≈ 62 $ in Brasile**, e in Germania sarebbero **oltre 110 €**. Un abbonamento piatto da 19 € che comprendesse
gli invii sarebbe in perdita in metà dei mercati di destinazione e in guadagno nell'altra metà, **senza che il
prezzo possa dirlo**. È il motivo per cui tutti i concorrenti esaminati tengono il canale **fuori**
dall'abbonamento e lo fanno pagare a consumo. Vedi la scelta d'architettura proposta al §5.

### 2.3 Obblighi normativi del settore

- **Regole del fornitore del canale, che valgono come una norma di fatto.** Ogni messaggio inviato fuori dalla
  finestra di 24 ore aperta dal cliente deve usare un **modello approvato**; l'approvazione è automatica e può
  richiedere fino a 24 ore; ogni modello appartiene a una delle tre categorie e la categoria determina il
  prezzo; esiste un **punteggio di qualità** che sospende o disabilita i modelli mal ricevuti e un **limite
  d'invio** per numero
  ([linee guida ufficiali](https://developers.facebook.com/docs/whatsapp/message-templates/guidelines/)). Non
  sono dettagli tecnici: sono vincoli che entrano nel modello dati (stato del modello, categoria, lingua) e nel
  comportamento dell'applicazione (cosa si può inviare adesso a questo contatto).
- **Consenso preventivo agli invii promozionali.** Le condizioni del canale richiedono che il contatto abbia
  dato il proprio consenso; in Europa si somma la disciplina delle comunicazioni indesiderate. Serve quindi che
  l'app tenga **traccia di come e quando** il consenso è stato raccolto e renda la disiscrizione immediata.
- **Protezione dei dati personali.** Il titolare del trattamento è l'azienda cliente; il fornitore del canale
  agisce da responsabile per alcuni trattamenti, con contratto stipulato con l'entità irlandese per i clienti
  europei, e usa **sub-responsabili anche fuori dallo Spazio economico europeo, Stati Uniti compresi**, coperti
  da un addendum sui trasferimenti
  ([termini ufficiali sul trattamento](https://www.whatsapp.com/legal/business-data-processing-terms/)). È un
  punto che tocca in pieno la postura di appgrove: vedi §6 e §11.
- **Pagamenti.** L'app **non** incassa. Se lo facesse entrerebbe nella disciplina dei servizi di pagamento —
  un perimetro che questa applicazione non deve nemmeno sfiorare.
- **Tutela del consumatore.** In India il fornitore del canale ha attivato una procedura di reclamo dedicata
  agli acquisti fatti in chat presso le aziende: il negozio deve poter ricostruire un ordine e il suo storico.
  È un requisito di tracciabilità, non di conformità formale.

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta attesa. Le voci marcate ✳ introdurrebbero un **fornitore esterno che tratta dati per
nostro conto** e vanno riportate al §6.

1. ✳ **Canale di messaggistica** (il fornitore del canale, tramite un rivenditore autorizzato) — non è
   un'integrazione facoltativa: è il presupposto dell'app;
2. ✳ **Strumento di incasso** già usato dal negozio (collegamento di pagamento, codice grafico, incasso
   istantaneo locale come UPI in India o Pix in Brasile). Il pagamento nativo dentro la chat è **disponibile
   solo in alcuni paesi** e le sue condizioni sono cambiate anche di recente
   ([stato per paese](https://www.infobip.com/blog/whatsapp-payments)): l'app tratta il collegamento di
   pagamento come il caso generale e il pagamento nativo come una possibile evoluzione;
3. **Anagrafica clienti della suite** (app 4 — CRM) e **catena del documento contabile** (app 2 e 1): un ordine
   confermato dovrebbe poter diventare una fattura senza reinserire nulla. Interno alla suite, quindi **a
   eventi**, non con chiamate dirette fra applicazioni;
4. **Catalogo prodotti e listini condivisi** con le app di vendita (6 preventivi, 14 magazzino, 29 retail);
5. **Foglio di calcolo in ingresso** per caricare il catalogo la prima volta: è la richiesta più banale e la
   più presente nelle recensioni del segmento.

### 2.5 Aspettative funzionali dei clienti micro e piccoli

**Cosa chiedono.** Che il messaggio del cliente non si perda; che il prezzo giusto sia sotto mano mentre si
risponde; che il conto si faccia da solo; che si possa mandare lo stesso avviso a duecento clienti senza
copiarlo duecento volte; che si veda chi ha pagato e chi no.

**Cosa rifiutano.** L'installazione lunga, la configurazione di un'automazione a diagrammi, il vocabolario da
marketing («cammino del cliente», «segmentazione comportamentale»), e soprattutto **un costo che non sanno
prevedere**. Le lamentele ricorrenti rilevate nel segmento sono due, ed entrambe sono requisiti travestiti:
il costo del canale che non si capisce da dove venga — segnalato come reclamo strutturale dei micro-venditori
indonesiani sulle piattaforme digitali (fonte 9) — e la complessità del software, indicata come barriera
principale all'adozione per chi non è pratico di tecnologia (fonte 10). Ne discendono due requisiti che ho
messo nelle storie: **il costo di un invio si mostra prima di inviare** (storia `0023`) e **l'app è usabile il
primo giorno senza configurare nulla oltre alla connessione del canale** (storie `0005` e `0006`).

### 2.6 Fonti consultate

1. **Prezzi ufficiali della piattaforma di messaggistica** — <https://developers.facebook.com/docs/whatsapp/pricing/>
   — fatturazione per messaggio dal 1° luglio 2025, quattro categorie, finestra di servizio di 24 ore gratuita,
   punto d'ingresso gratuito di 72 ore, tariffe per paese del destinatario.
2. **Linee guida ufficiali sui modelli di messaggio** —
   <https://developers.facebook.com/docs/whatsapp/message-templates/guidelines/> — approvazione entro 24 ore,
   tre categorie obbligatorie, punteggio di qualità che sospende i modelli, limiti d'invio, massimo 100 modelli
   creabili in un'ora.
3. **Termini ufficiali sul trattamento dei dati per le aziende** —
   <https://www.whatsapp.com/legal/business-data-processing-terms/> — l'azienda cliente è titolare, il
   fornitore è responsabile, entità contraente irlandese per l'Unione europea, sub-responsabili fuori dallo
   Spazio economico europeo con addendum sui trasferimenti.
4. **Listino ufficiale Wati** — <https://www.wati.io/pricing/> — 49/99/199 $ al mese, utenti inclusi e costo
   degli utenti aggiuntivi, prova di 7 giorni, messaggi fatturati a parte.
5. **Listino ufficiale AiSensy** — <https://aisensy.com/pricing> — 2.500/3.500 ₹ al mese, sconto 10 %
   sull'annuale, tariffe per messaggio in rupie da gennaio 2026 (1,09 ₹ marketing, 0,145 ₹ utility, servizio
   gratuito), pre-pagato.
6. **Tariffe per paese 2026 (rassegne comparative)** — <https://formbeep.com/whatsapp-api-pricing/> ·
   <https://www.uptail.ai/blog/whatsapp-business-api-pricing-2026-what-it-costs-and-how-billing-works> —
   marketing ≈ 0,0103 $ India, 0,0625 $ Brasile, 0,0271 $ Indonesia; ricarico del rivenditore 0,003-0,010 $.
   **Fonti secondarie**: da verificare sul listino ufficiale prima di fissare qualunque prezzo.
7. **Stato dei pagamenti in chat per paese** — <https://www.infobip.com/blog/whatsapp-payments> ·
   <https://aerochat.ai/blog/whatsapp-pay-for-ecommerce-country-by-country-status> — incasso nativo diffuso in
   India (UPI) e Brasile, con condizioni che sono già cambiate (fine dei pagamenti diretti con carta in Brasile
   a gennaio 2026). Fonti secondarie.
8. **Perché i piccoli negozi perdono ordini in chat** —
   <https://blogs.buildwithsamurai.com/why-small-shops-lose-orders-in-whatsapp-chats-and-how-to-fix-it-in-2026/>
   — la perdita nasce da attrito e ritardo, non dal prezzo; la conversazione non strutturata è il difetto.
9. **Reclami dei micro-venditori indonesiani sulle piattaforme digitali** —
   <https://en.antaranews.com/amp/news/424500/indonesia-bolsters-small-business-protection-on-digital-marketplaces>
   — la mancanza di chiarezza sulle componenti di costo è il reclamo strutturale ricorrente.
10. **Barriere all'adozione tecnologica nelle piccole imprese** —
    <https://www.sparkouttech.com/technology-adoption-challenges/> — complessità dell'interfaccia e costo
    imprevedibile come ostacoli principali; il 55 % dichiara di voler accelerare, ma l'esecuzione si blocca lì.
11. **Fornitori del canale con sede europea** —
    <https://engrana.es/en/blog/best-whatsapp-business-solution-providers-europe> — Infobip (Croazia), Bird
    (Paesi Bassi), Sinch (Svezia), 360dialog (Berlino) sono rivenditori autorizzati con opzione di residenza dei
    dati in Europa. Rilevante per la postura di appgrove (§6, §11). Fonte secondaria.

### 2.7 Cosa NON sono riuscito a determinare

- **Prezzo ufficiale di Interakt** e degli altri prodotti indiani diretti — le pagine di listino non sono state
  estratte in questa ricerca e i dati disponibili vengono da confronti pubblicati da un concorrente, quindi di
  parte. Servirebbe una lettura diretta delle pagine ufficiali prima di posizionare il prezzo.
- **Listino di Infobip e 360dialog** — vendono su richiesta di contatto: nessun prezzo pubblico. Serve un
  preventivo reale per stimare il costo di un fornitore del canale con sede europea.
- **Tariffe ufficiali per paese aggiornate ad agosto 2026** — la documentazione ufficiale elenca i paesi ma
  rimanda a un listino scaricabile che non ho potuto leggere. I numeri del §2.2 vengono da fonti secondarie e
  da una pagina ufficiale di rivenditore (AiSensy) per l'India: **vanno riverificati** prima di ogni conto
  economico.
- **Se il rivenditore autorizzato consenta a un fornitore di software di rivendere il canale ai propri clienti**
  senza diventare esso stesso rivenditore autorizzato — è la domanda che decide il modello di costo del §5 e non
  ho trovato una risposta autorevole. Va chiesta direttamente a un rivenditore.
- **Quanti dei clienti europei di appgrove venderebbero davvero in chat** — non ho trovato una stima
  attendibile per il perimetro europeo, che nel catalogo è dichiarato secondario rispetto ai mercati emergenti.

---

## 3. Varco d'identità — le risposte pronte per `new-application`

> Queste sei righe sono ciò che la skill `new-application` chiede **prima** di generare qualunque cosa. L'identificativo
> dell'app finisce nel nome dello schema del database, nei nomi delle code, nella rotta pubblica e nell'istanza
> del modulo di infrastruttura: cambiarlo dopo **non è una rinomina, è una migrazione di dati**.

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `chat_commerce` | Rispetta `^[a-z][a-z0-9_]{0,30}$`. Dice **cosa l'app è** — vendita in chat — e non come è commercializzata («ChatGrove») né su quale canale gira oggi. Mettere il nome del canale nell'identificativo sarebbe un errore strutturale: lo schema del database si chiamerebbe come un fornitore terzo, e il giorno in cui se ne aggiungesse un secondo l'identificativo mentirebbe. Alternativa scartata: `chatgrove` (nome commerciale, si può cambiare); `chat` (troppo generico, si confonde con l'assistenza dell'app 12). |
| **Modello utente** | `multi` | Nel negozio rispondono più persone dallo stesso numero: il titolare la sera, l'addetto al banco di giorno. Senza il concetto di «chi ha preso in carico questa conversazione» e «chi ha confermato quest'ordine» l'app perde metà del suo valore, perché il difetto che risolve è proprio la sovrapposizione fra chi risponde. La presa in carico di una conversazione è un dato per-utente, non per-account. |
| **Porta locale** | `8105` | Convenzione del kit (8100 + numero di catalogo). Da confermare con `./dev.sh services` al momento dello scaffolding. |
| **Metrica di quota** | `messaggi_template` (messaggi inviati con un modello approvato, fuori dalla finestra di servizio) | È la **sola** cosa che il piano limita. È l'unica grandezza che cresce insieme al valore ricevuto (più clienti raggiunti, più vendite) **e** insieme al costo sostenuto (ogni modello inviato ha una tariffa dal fornitore del canale). Le alternative sono peggiori: gli **ordini** misurano il valore ma non il costo, e punirebbero il negozio proprio quando funziona; i **contatti** misurano una giacenza che il negozio non controlla; le **conversazioni** non sono più l'unità di fatturazione del fornitore dal luglio 2025, quindi useremmo un'unità che il mercato ha smesso di usare. Le risposte dentro la finestra di servizio **non consumano quota**, perché non costano nulla e sono il comportamento che vogliamo incoraggiare. |
| **Natura della metrica** | `flow` | Consumo su una finestra mensile che si azzera: «2.000 messaggi con modello al mese» — a marzo se ne possono inviare altri 2.000 comunque sia andato febbraio. Non è una giacenza: un messaggio inviato non si può «togliere» per farne posto a un altro. Contarlo come giacenza bloccherebbe il negozio per sempre al primo mese pieno. |
| **Colore-categoria e icona** | `teal` · icona `message-square` (fumetto di conversazione) | Il verde acqua distingue l'app dalle vicine di catalogo orientate al documento e al denaro (fatturazione, incasso crediti, preventivi, che stanno naturalmente sui toni caldi e sul blu) e la accosta al gruppo «relazione con il cliente». Lo stesso valore va nel listino (`category: teal`) e nel manifesto del modulo frontend (`accentToken`). |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Channel` | La connessione dell'account al canale di messaggistica | identificativo del numero presso il fornitore, riferimento cifrato alle credenziali, stato della connessione, esito dell'ultima verifica | no (dati dell'azienda, non di una persona) |
| `Contact` | Una persona che scrive al negozio | numero di telefono, nome visualizzato, nome dato dal negozio, stato del consenso, origine del consenso, data del consenso, lingua | **sì** — numero, nome, lingua |
| `Conversation` | Il filo di messaggi con un contatto | contatto, stato (aperta, presa in carico, chiusa), utente che l'ha presa in carico, scadenza della finestra di servizio, ultimo messaggio | **sì** — indirettamente, tramite il contatto |
| `Message` | Un singolo messaggio, in entrata o in uscita | direzione, tipo, testo, identificativo presso il fornitore, stato di consegna, modello usato, costo dichiarato | **sì** — il contenuto è scritto da una persona |
| `Product` | Una voce del catalogo del negozio | codice, nome, descrizione, prezzo, valuta, disponibilità, immagine, stato di pubblicazione | no |
| `ProductVariant` | Taglia, colore o formato di un prodotto | prodotto, nome della variante, differenza di prezzo, disponibilità | no |
| `Cart` | Il carrello in costruzione dentro una conversazione | conversazione, righe, totale calcolato, stato (aperto, convertito, abbandonato), ultimo movimento | **sì** — indirettamente |
| `Order` | L'impegno d'acquisto, con un numero e uno stato | numero, contatto, righe congelate con prezzo, totale, stato, note di consegna | **sì** — contatto e, se raccolto, indirizzo di consegna |
| `PaymentRequest` | La richiesta di pagamento inviata al cliente | ordine, importo, valuta, collegamento o riferimento esterno, stato, scadenza | **sì** — indirettamente |
| `Payment` | L'incasso registrato a fronte di una richiesta | richiesta, importo, data, mezzo, riferimento, chi l'ha registrato | **sì** — indirettamente |
| `MessageTemplate` | Un modello approvato dal fornitore del canale | nome, categoria, lingua, corpo con segnaposto, stato di approvazione, punteggio di qualità | no |
| `Segment` | Un insieme di contatti definito da criteri | nome, criteri, conteggio all'ultimo calcolo | no (contiene criteri, non persone) |
| `Campaign` | Un invio massivo a un segmento con un modello | segmento, modello, valori dei segnaposto, stato, stima del costo, conteggi degli esiti | no direttamente |
| `CampaignDelivery` | L'esito di una campagna su un singolo contatto | campagna, contatto, stato di consegna, motivo dello scarto | **sì** — indirettamente |
| `AutoReply` | Una risposta automatica (benvenuto, fuori orario) | tipo, orario di attivazione, testo per lingua, stato | no |

**Relazioni.** `Contact` 1-N `Conversation` 1-N `Message`. Una `Conversation` ha al più un `Cart` aperto.
`Cart` → `Order` (conversione: le righe si **congelano**, il prezzo dell'ordine non segue più il listino).
`Order` 1-N `PaymentRequest` 1-1 `Payment`. `Segment` → `Campaign` 1-N `CampaignDelivery` → `Message`.

Macchine a stati che le storie devono rispettare:

- **Conversazione**: `aperta` → `presa_in_carico` → `chiusa`; una conversazione chiusa si riapre da sola se
  arriva un messaggio nuovo.
- **Finestra di servizio**: `aperta` (entro 24 ore dall'ultimo messaggio del contatto, si può rispondere
  liberamente) → `chiusa` (si può scrivere **solo** con un modello approvato). È un vincolo del fornitore, non
  una scelta nostra, e attraversa tutta l'applicazione.
- **Carrello**: `aperto` → `convertito` | `abbandonato` (nessun movimento entro una soglia configurabile).
- **Ordine**: `bozza` → `confermato` → `pagato` → `consegnato` → `chiuso`; da `bozza` e `confermato` si può
  passare ad `annullato`. Un ordine `pagato` **non torna** a `confermato`: si registra una restituzione, che
  nella prima versione è una nota, non un movimento.
- **Richiesta di pagamento**: `emessa` → `pagata` | `scaduta` | `annullata`.
- **Campagna**: `bozza` → `in_conferma` → `in_invio` → `conclusa` | `interrotta`.

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica
(`deleted_at`); schema `app_chat_commerce`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8).

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il
> file `services/core/src/main/resources/pricing/chat_commerce.yaml`.

### 5.1 La decisione che viene prima del prezzo: chi paga il canale

Il catalogo propone «flat 10-29 €/mese **+ 0,01-0,03 € per conversazione con modello**, oppure 0,5 % sulle
transazioni». **Nessuna delle due varianti è compatibile con le regole della piattaforma appgrove**, che vieta
il pagamento una tantum, l'addebito a consumo per lo sforamento e ogni forma di commissione sul transato: al
raggiungimento del limite si **blocca**, non si addebita
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §7). Va quindi scelto un modello diverso, e la scelta
cambia i margini. Le due vie possibili:

**Via A — «porta il tuo canale» (raccomandata).** Il cliente attiva per conto proprio il numero presso un
rivenditore autorizzato e collega le credenziali a ChatGrove. **Paga i messaggi al suo fornitore**, non a noi.
appgrove vende solo il flusso di lavoro, con un abbonamento piatto e prevedibile.

- *a favore*: nessun costo variabile a nostro carico, quindi margine stabile in ogni paese; nessun rischio di
  vendere sotto costo in Brasile o Germania (§2.2); nessun ruolo di rivenditore da assumere; il cliente resta
  titolare del proprio numero, che è il suo bene più importante;
- *contro*: la messa in funzione è più difficile — il micro-negozio deve fare da solo una cosa che non capisce,
  ed è esattamente la barriera che il §2.5 indica come principale. Va compensata con una guida passo passo
  dentro l'app (storia `0006`).

**Via B — canale rivenduto da appgrove.** Prendiamo noi il rapporto con il rivenditore e includiamo un numero
di messaggi nell'abbonamento.

- *a favore*: attivazione in un clic, che è un vantaggio di vendita reale;
- *contro*: il costo per messaggio varia di **sei volte** fra India e Brasile e di **dieci** verso la Germania:
  un prezzo unico europeo sarebbe una scommessa sul mercato di destinazione dei clienti. Si aggiungono il
  rischio di essere rivenditori senza esserlo (§2.7) e la contabilità di un consumo che non possiamo addebitare.

**Raccomandazione: Via A per il lancio**, con la Via B come evoluzione da valutare quando esisterà un volume
noto. La proposta di listino qui sotto assume la Via A. **La scelta non è mia**: è direzione di prodotto e
prezzo insieme, quindi è una fermata di escalation (punto aperto n. 1 del §11).

### 5.2 Il listino proposto

**Ragionamento.** Con la Via A il prezzo non deve coprire alcun costo per messaggio: deve stare **sotto** ai
prodotti globali (49 $ di Wati) e **accanto** ai prodotti indiani (26-37 € di AiSensy), ricordando che il
segmento di destinazione ha una capacità di spesa bassa e alta sensibilità al prezzo. La quota serve a due
cose: dare una scala di prezzo onesta (chi manda di più paga di più) e proteggere l'infrastruttura da un uso
massivo. Il tetto è espresso in **messaggi con modello al mese**, perché le risposte dentro la finestra di
servizio sono gratuite per il cliente e per noi e non vanno scoraggiate.

| Piano | Prezzo mensile | Prezzo annuale | Limite su `messaggi_template` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | **100 al mese** | — | Il negozio che vuole vedere se serve: bastano per gli avvisi d'ordine di una settimana, non per una campagna |
| `pro` | **19 €** | **190 €** (= 10× il mensile, «due mesi in regalo») | **2.000 al mese** | 14 giorni | Il micro-negozio con uno o due addetti che vende tutti i giorni in chat |
| `business` | **49 €** | **490 €** | **10.000 al mese** | 14 giorni | Il negozio con più addetti, che fa campagne regolari su una rubrica di migliaia di contatti |

**Note obbligate.**

- Tre piani, non di più: aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile.
- Un limite lasciato vuoto significherebbe **illimitato**: qui nessun piano è illimitato, ed è voluto — un
  limite assente su una metrica che genera traffico verso un fornitore terzo è un invito all'abuso.
- **Prova gratuita e piano gratuito insieme**: qui la ridondanza è solo apparente. Il piano gratuito serve a
  provare il flusso su pochi contatti; la prova di 14 giorni del piano `pro` serve a provare **la campagna**,
  che è la funzione che convince a pagare e che 100 messaggi non permettono di vedere. Se lo sviluppatore
  preferisce semplificare, la voce da togliere è la prova, non il piano gratuito.
- **Costo effettivo dell'incasso**: nessun piano sta sotto i 5 €, quindi la parte fissa per transazione non
  erode il prezzo in modo preoccupante. Sull'annuale la proporzione migliora ancora.
- I prezzi sono **immutabili una volta vivi**: un cambio si fa creando un prezzo nuovo e archiviando il
  vecchio; gli abbonati restano sul loro.
- Il passaggio da `business` a `pro` **non** va bloccato: la metrica è a consumo, non a giacenza, e il blocco
  del declassamento riguarda solo le giacenze.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/chat_commerce.yaml`) si compila **insieme** allo sviluppatore: «niente contratto,
> niente produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

> 🛑 **Attenzione — trasferimento verso paesi terzi e fornitore extra-europeo.** Questa applicazione **non può
> esistere senza un canale di messaggistica che appartiene a un fornitore extra-europeo**. Secondo i termini
> ufficiali sul trattamento, il cliente è titolare, il fornitore agisce da responsabile con l'entità irlandese
> per i clienti europei, e impiega **sub-responsabili anche fuori dallo Spazio economico europeo, Stati Uniti
> compresi**, coperti da un addendum sui trasferimenti
> ([fonte](https://www.whatsapp.com/legal/business-data-processing-terms/)). appgrove ha una postura dichiarata
> di **preferenza per fornitori europei** e di dati personali **a riposo solo in regioni europee**
> ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §10). Le due cose sono in tensione, e la tensione **non
> si risolve in un documento d'autore**: è una decisione di conformità e di prodotto dello sviluppatore
> (punto aperto n. 2 del §11). Quello che posso dire è: *cosa* è in gioco (numeri di telefono, nomi e
> **contenuto dei messaggi** dei clienti finali del negozio), *quali* attenuazioni esistono (rivenditore con
> sede e residenza dei dati in Europa — Infobip, Bird, Sinch, 360dialog, fonte 11 del §2.6; conservazione
> minima del contenuto dalla nostra parte; nessun uso secondario) e *cosa non elimina nessuna di esse* (il
> messaggio passa comunque dall'infrastruttura del fornitore del canale). **Non è una fermata che un agente
> può superare.**

**Categorie trattate** (proposta)

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `contact.phone` | `contact.phone_number` | Cliente finale del negozio | Contatto (identificatore diretto) | Identificare la conversazione e recapitare i messaggi | Esecuzione del contratto fra il negozio e il suo cliente / legittimo interesse del negozio | Finché il contatto è attivo + 24 mesi dall'ultima conversazione |
| `contact.display_name` | `contact.display_name`, `contact.given_name` | Cliente finale | Anagrafica | Riconoscere l'interlocutore | Come sopra | Come sopra |
| `contact.consent` | `contact.consent_state`, `consent_source`, `consent_at` | Cliente finale | Prova di consenso | Dimostrare che gli invii promozionali sono leciti | Obbligo del titolare di dimostrare il consenso | 24 mesi dalla revoca (è la prova che serve **dopo**) |
| `message.body` | `message.body` | Cliente finale e addetti del negozio | **Contenuto di comunicazione** | Ricostruire la conversazione e l'ordine | Esecuzione del contratto | 12 mesi, poi conservazione del solo metadato |
| `order.shipping_note` | `order.shipping_note` | Cliente finale | Recapito | Consegnare | Esecuzione del contratto | 24 mesi (tracciabilità dell'ordine) |
| `order.contact_ref` | `order.contact_id` | Cliente finale | Collegamento | Legare ordine e persona | Esecuzione del contratto | Come l'ordine |
| `payment.reference` | `payment.external_reference` | Cliente finale | Economico | Riconciliare l'incasso | Esecuzione del contratto | 24 mesi |
| `campaign_delivery.contact_ref` | `campaign_delivery.contact_id` | Cliente finale | Collegamento + esito | Dimostrare cosa è stato inviato a chi | Legittimo interesse / prova di conformità | 12 mesi |
| `conversation.assignee` | `conversation.assigned_user_id` | Addetto del negozio | Dato di lavoro | Sapere chi ha preso in carico | Esecuzione del contratto di lavoro / legittimo interesse | Vita della conversazione + 12 mesi |

**Il punto più delicato è `message.body`.** Il contenuto di una comunicazione è, di per sé, il dato più
invasivo dell'app: è testo libero scritto da una persona che non è nostro cliente. Tre attenuazioni proposte,
tutte da confermare: conservare il contenuto **12 mesi** e non per sempre; **non** copiarlo nei registri
tecnici (vincolo §14 dei principi); non usarlo per nessuna finalità secondaria — niente addestramento di
modelli, niente statistiche aggregate rivendute.

**Articolo 9 — categorie particolari.** Per **disegno** l'applicazione non chiede né classifica dati sanitari,
biometrici, genetici, opinioni politiche, convinzioni religiose, orientamento sessuale o appartenenza
sindacale: non esiste un campo che li raccolga. Ma il contenuto dei messaggi è **testo libero scritto dal
cliente finale**, e in una farmacia o in un negozio di alimenti per diete particolari quel testo li conterrà
comunque («mi serve il farmaco per…»). La classificazione onesta è quindi: **nessuna categoria particolare
trattata in modo sistematico o intenzionale, ma un ingresso non presidiato che rende probabile la loro
presenza occasionale**. Non ammorbidisco: se lo sviluppatore ritiene che l'ingresso non presidiato basti a
far scattare la valutazione d'impatto, la decisione è sua, non mia (punto aperto n. 3 del §11).

**Esportazione e cancellazione.** Tabelle che contengono dati personali e che **devono** comparire sia in
`exportData` sia in `purgeData` del contratto dati dell'app: `contact`, `conversation`, `message`, `cart`,
`cart_line`, `order`, `order_line`, `payment_request`, `payment`, `campaign_delivery`. Dimenticarne una è il
difetto di conformità più probabile. La cancellazione è **fisica**: sostituire il numero di telefono con un
codice non è cancellare, e in un'app di messaggistica la tentazione è forte perché il numero è la chiave
naturale. La conseguenza è che il modello dati non deve usare il numero come chiave primaria — lo dice la
storia `0002`.

**Testo libero.** Presente in tre punti: corpo dei messaggi, nota interna sul contatto, nota di consegna
sull'ordine. Sono tutti ingressi non presidiati. L'app **non** fa rilevazione di contenuto; l'interfaccia
avvisa nei campi nota di non inserire dati sensibili; il presidio automatico, se servirà, è un tema
trasversale della piattaforma, non di questa app.

**Integrazioni esterne che ricevono dati personali** (dal §2.4):

1. **il fornitore del canale di messaggistica**, tramite il rivenditore autorizzato — riceve numeri e contenuti:
   è il punto dell'avviso in testa a questa sezione;
2. **lo strumento di incasso** del negozio — riceve importo e riferimento dell'ordine; se il collegamento di
   pagamento è generato dal negozio stesso, il rapporto è fra il negozio e il suo fornitore, non nostro: **da
   verificare caso per caso**;
3. **le altre app della suite** (anagrafica clienti, fatturazione) — trattamento interno, a eventi, dentro il
   perimetro europeo.

**Classificazione della change.** Una app nuova che introduce il **contenuto delle comunicazioni** di persone
che non sono nostri clienti, e un trasferimento verso paesi terzi tramite un responsabile extra-europeo, è
senza dubbio un cambiamento **sostanziale**: nuove finalità, nuove categorie, nuovo responsabile del
trattamento, nuovo trasferimento. La classificazione descrive la realtà e non è una leva.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `elenca_conversazioni` | `(stato?, presa_in_carico_da?, pagina?) → elenco minimizzato di conversazioni` | Cosa c'è da rispondere adesso | lettura | no |
| `leggi_conversazione` | `(id, limite?) → ultimi messaggi + stato della finestra di servizio` | Il filo, con l'indicazione se si può ancora rispondere liberamente | lettura | no |
| `cerca_prodotto` | `(testo, solo_disponibili?) → prodotti con prezzo e disponibilità` | Il listino sotto mano mentre si risponde | lettura | no |
| `elenca_ordini` | `(stato?, periodo?, contatto?) → ordini con totale e stato` | Chi ha ordinato e chi non ha pagato | lettura | no |
| `elenca_carrelli_abbandonati` | `(giorni?) → carrelli fermi, con valore e ultimo movimento` | Il recupero possibile | lettura | no |
| `riepiloga_contatto` | `(id) → storico ordini, speso totale, stato del consenso` | Chi è questa persona per il negozio | lettura | no |
| `aggiungi_al_carrello` | `(conversazione, prodotto, quantità) → carrello aggiornato` | Costruisce il carrello | scrittura reversibile | no (si svuota) |
| `crea_ordine` | `(conversazione) → bozza di ordine con righe e totale` | Congela il carrello in un ordine | scrittura | **sì** |
| `invia_messaggio` | `(conversazione, testo) → bozza del messaggio` | Scrive al cliente finale dentro la finestra di servizio | scrittura verso l'esterno | **sì, obbligatoria** |
| `invia_modello` | `(contatto, modello, valori) → bozza del messaggio con modello` | Scrive fuori dalla finestra: costa e consuma quota | scrittura verso l'esterno | **sì, obbligatoria** |
| `richiedi_pagamento` | `(ordine, importo?) → bozza di richiesta di pagamento` | Chiede soldi a una persona | scrittura irreversibile | **sì, obbligatoria** |
| `avvia_campagna` | `(segmento, modello, valori) → bozza con destinatari, esclusi e stima del costo` | Invio massivo verso l'esterno | scrittura irreversibile | **sì, obbligatoria** |

**Lettura.** In quest'app il livello conversazionale non è una comodità: è il modo naturale d'uso. Il titolare
scrive «quanti ordini non pagati ho?» e «manda a Rina il preventivo per due chili» invece di attraversare tre
schermate. Ma proprio per questo la regola della bozza è più severa che altrove: **ogni strumento che fa
partire un messaggio verso una persona reale, o che chiede denaro, produce una bozza e si ferma**. Un
assistente che invia da solo un messaggio promozionale a duemila contatti non è un errore recuperabile: costa
denaro reale, brucia il punteggio di qualità del numero e può farlo sospendere.

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine dell'epica l'app esiste, è accesa, vuota e navigabile: si avvia in locale, mostra le sue sezioni in
cinque lingue, conosce l'abbonamento dell'account e blocca a quota esaurita.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | Istanza di scaffolding, rotte `/api/chat_commerce/v1/*`, infrastruttura dal modulo comune |
| [0002](01-fondamenta/0002-modello-dati-multi-account.md) | Modello dati multi-account | Schema `app_chat_commerce` con contatti, conversazioni e messaggi isolati per account |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Manifesto, sezioni, colore `teal`, cinque lingue, tema chiaro e scuro |
| [0004](01-fondamenta/0004-abbonamento-e-quota.md) | Abbonamento e quota | Proiezione dell'abilitazione, contatore `messaggi_template`, blocco `429` |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | `./dev.sh services` vede l'app; canale simulato e dati inventati per lavorarci subito |

### Epica 02 — Canale di messaggistica e conformità degli invii

Alla fine dell'epica il negozio riceve messaggi veri, risponde dentro le regole del fornitore e sa a chi può
scrivere e a chi no.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-canale-e-conformita-degli-invii/0006-connessione-del-canale.md) | Connessione del canale | Il negozio collega il proprio numero, con credenziali cifrate e verifica dello stato |
| [0007](02-canale-e-conformita-degli-invii/0007-ricezione-dei-messaggi.md) | Ricezione dei messaggi | Il messaggio in arrivo diventa conversazione, una sola volta anche se ripetuto |
| [0008](02-canale-e-conformita-degli-invii/0008-finestra-di-servizio-e-invio-libero.md) | Finestra di servizio e risposta libera | Si risponde liberamente entro 24 ore; fuori, l'app dice che serve un modello |
| [0009](02-canale-e-conformita-degli-invii/0009-registro-dei-modelli-approvati.md) | Registro dei modelli approvati | Modelli, categoria, lingua e stato di approvazione allineati al fornitore |
| [0010](02-canale-e-conformita-degli-invii/0010-consenso-e-disiscrizione.md) | Consenso e disiscrizione | Stato del consenso con la sua prova; la disiscrizione blocca gli invii promozionali |

### Epica 03 — Catalogo prodotti in chat

Alla fine dell'epica il listino del negozio è nell'app e si può mostrare al cliente senza uscire dalla
conversazione.

| # | Storia | In una riga |
|---|---|---|
| [0011](03-catalogo-prodotti-in-chat/0011-anagrafica-dei-prodotti.md) | Anagrafica dei prodotti | Prodotti con prezzo, valuta, descrizione e stato di pubblicazione |
| [0012](03-catalogo-prodotti-in-chat/0012-varianti-e-disponibilita.md) | Varianti e disponibilità | Taglie e formati con differenza di prezzo; il prodotto esaurito non si vende |
| [0013](03-catalogo-prodotti-in-chat/0013-caricamento-del-listino-da-file.md) | Caricamento del listino da file | Il catalogo entra da un foglio di calcolo, con anteprima e scarti spiegati |
| [0014](03-catalogo-prodotti-in-chat/0014-invio-della-scheda-prodotto.md) | Invio della scheda prodotto | Prodotto o selezione inviati in conversazione, dentro la finestra di servizio |

### Epica 04 — Ordini e pagamenti

Alla fine dell'epica la conversazione produce un ordine con un numero, una richiesta di pagamento e un incasso
registrato.

| # | Storia | In una riga |
|---|---|---|
| [0015](04-ordini-e-pagamenti/0015-carrello-della-conversazione.md) | Carrello della conversazione | Righe, quantità e totale calcolato dentro il filo del cliente |
| [0016](04-ordini-e-pagamenti/0016-creazione-dell-ordine.md) | Creazione dell'ordine | Il carrello diventa un ordine numerato con i prezzi congelati |
| [0017](04-ordini-e-pagamenti/0017-ciclo-di-vita-dell-ordine.md) | Ciclo di vita dell'ordine | Stati, annullamento, storico di chi ha cambiato cosa |
| [0018](04-ordini-e-pagamenti/0018-richiesta-di-pagamento.md) | Richiesta di pagamento | Bozza, conferma umana, invio del collegamento di pagamento al cliente |
| [0019](04-ordini-e-pagamenti/0019-registrazione-dell-incasso.md) | Registrazione dell'incasso | L'incasso si registra a mano o dal riscontro, e chiude la richiesta |
| [0020](04-ordini-e-pagamenti/0020-carrelli-abbandonati.md) | Carrelli abbandonati | Elenco dei carrelli fermi e promemoria con modello approvato |

### Epica 05 — Contatti, campagne e recupero

Alla fine dell'epica il negozio ha una rubrica utilizzabile, sa formare gruppi di clienti e può scrivere a
tutti con un modello, vedendo prima quanto costa.

| # | Storia | In una riga |
|---|---|---|
| [0021](05-contatti-campagne-e-recupero/0021-rubrica-dei-contatti.md) | Rubrica dei contatti | Elenco, ricerca, scheda del contatto con lo storico |
| [0022](05-contatti-campagne-e-recupero/0022-segmenti-di-contatti.md) | Segmenti di contatti | Gruppi definiti da criteri semplici, ricalcolati alla lettura |
| [0023](05-contatti-campagne-e-recupero/0023-campagna-di-invio-massivo.md) | Campagna di invio massivo | Bozza con destinatari, esclusi, costo stimato e conferma esplicita |
| [0024](05-contatti-campagne-e-recupero/0024-esiti-di-consegna.md) | Esiti di consegna | Consegnato, letto, fallito: per campagna e per contatto |
| [0025](05-contatti-campagne-e-recupero/0025-risposte-automatiche.md) | Risposte automatiche | Benvenuto e fuori orario, nelle lingue del negozio |

### Epica 06 — Esposizione conversazionale e prove end-to-end

Alla fine dell'epica l'app è comandabile da una chat con la disciplina della bozza, e il suo percorso
end-to-end è verde nel registro di copertura.

| # | Storia | In una riga |
|---|---|---|
| [0026](06-esposizione-conversazionale-e-prove/0026-strumenti-di-lettura.md) | Strumenti di lettura | Contratto degli strumenti che leggono, con risultati minimizzati |
| [0027](06-esposizione-conversazionale-e-prove/0027-strumenti-di-scrittura-con-conferma.md) | Strumenti di scrittura con conferma | Bozza e conferma umana per tutto ciò che esce o chiede denaro |
| [0028](06-esposizione-conversazionale-e-prove/0028-traccia-degli-invii-dell-assistente.md) | Traccia degli invii dell'assistente | Ogni azione dell'assistente resta tracciata e attribuita |
| [0029](06-esposizione-conversazionale-e-prove/0029-percorso-end-to-end.md) | Percorso end-to-end dell'app | Percorso `[J-CHAT-COMMERCE]` e registro di copertura aggiornato |

**Totale**: 6 epiche, 29 storie.

---

## 9. Estensioni della console di amministrazione

Servono estensioni: l'app dipende da una connessione esterna che si rompe (credenziali scadute, numero
sospeso, modelli respinti) e l'assistenza deve poter rispondere a «perché non partono più i miei messaggi?»
senza entrare nell'account del cliente. Serve inoltre una vista sull'arretrato degli invii e una deroga
temporanea sulla quota per la prima migrazione di rubrica.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| 04 — CRM | alimenta / condivide dati con | **Anagrafica clienti condivisa**: il contatto della chat è la stessa persona della scheda cliente. ChatGrove è la porta d'ingresso naturale del CRM nei mercati dove il cliente arriva da un messaggio |
| 02 — Fatturazione · 01 — Fattura elettronica | alimenta | **Catena del documento contabile**: ordine confermato e pagato → fattura. ChatGrove sta a monte della catena preventivo → ordine → fattura → incasso |
| 06 — Preventivi | si sovrappone a | Entrambe partono da un listino e producono un impegno d'acquisto. Confine: il preventivo è un documento formale che si firma, l'ordine in chat è un impegno immediato |
| 14 — Magazzino · 29 — Retail | condivide dati con | **Catalogo prodotti e listini**: la disponibilità mostrata in chat dovrebbe essere quella vera del magazzino |
| 03 — Incasso crediti | alimenta | Un ordine consegnato e non pagato è un credito |
| 12 — Assistenza clienti | si sovrappone a | Entrambe hanno una casella di conversazioni. Confine netto: qui si **vende** (carrello, ordine, incasso), lì si **assiste** (priorità, tempi di risposta, riassegnazione) |
| 16 — Marketing / ReachGrove | si sovrappone a | Le campagne. Confine: qui la campagna esiste solo sul canale di messaggistica e verso contatti che hanno già scritto al negozio |

**Lettura.** ChatGrove **ha senso da sola** — è la sua forza rispetto al resto della suite: un negozio che
vende in chat compra questa e basta. Ma tocca **tre** delle quattro entità condivise indicate dal catalogo (§6:
anagrafica clienti, catalogo prodotti e listini, catena del documento contabile), quindi è anche una delle
porte d'ingresso più efficaci alla suite: chi entra da qui ha già dentro i clienti e i prodotti.

**Sovrapposizioni da evitare.** Tre, tutte reali: la casella condivisa con l'app 12 (assistenza), le campagne
con l'app 16 (marketing), il catalogo prodotti con le app 14 e 29. La regola che propongo è che ChatGrove
**possieda il canale di messaggistica** e non altro: non deve diventare un secondo strumento di marketing né
un secondo magazzino. Se la suite viene costruita, il catalogo prodotti va estratto e condiviso — meglio
saperlo adesso che dopo averlo costruito due volte.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **Chi paga il canale**: «porta il tuo canale» (Via A, raccomandata) oppure canale rivenduto da appgrove (Via B) | È direzione di prodotto **e** prezzo insieme; il modello del catalogo (commissione per conversazione o sul transato) è vietato dalle regole della piattaforma | sviluppatore |
| 2 | **Fornitore extra-europeo del canale** e trasferimento verso paesi terzi, contro la postura di preferenza europea di appgrove | È una decisione di conformità e di prodotto: attenuare (rivenditore con sede e dati in Europa), accettare con motivazione scritta, o rinunciare all'app | sviluppatore, con la revisione legale |
| 3 | **Categorie particolari occasionali** nel contenuto dei messaggi: fa scattare la valutazione d'impatto? | Il contenuto è testo libero scritto da terzi: l'app non le raccoglie ma non può escluderle | sviluppatore |
| 4 | **Prezzi e limiti dei piani** proposti al §5.2 | Fermata di escalation per definizione | sviluppatore |
| 5 | **Conservazione del contenuto dei messaggi** (12 mesi proposti) e se conservarlo affatto | Incide sulla dimensione dei dati, sul rischio e sull'utilità dell'app | sviluppatore |
| 6 | **Quali mercati servire per primi**: il catalogo dice mercati emergenti, ma la piattaforma incassa in euro e ha una postura europea | Direzione di prodotto; cambia le tariffe di riferimento, le lingue e forse le valute | sviluppatore |
| 7 | **Valuta del catalogo prodotti** diversa dall'euro dell'abbonamento | Il negozio indiano vende in rupie e paga in euro: il modello dati regge (valuta sul prodotto), il resto è da decidere | sviluppatore / storia `0011` |
| 8 | **Rivendita del canale**: un fornitore di software può rivendere il canale senza essere rivenditore autorizzato? | Non ho trovato una risposta autorevole (§2.7) | sviluppatore, chiedendo a un rivenditore |

**Rischi noti**

- **Dipendenza da una piattaforma di terzi** (già segnalata dal catalogo, §8) — se il fornitore cambia
  condizioni, prezzi o regole di approvazione, l'app cambia con lui e non ha voce in capitolo. È successo di
  recente: fatturazione passata da conversazione a messaggio nel luglio 2025, fine dei pagamenti con carta in
  Brasile a gennaio 2026. *Attenuazione*: tenere il modello dati **indipendente dal canale** (una tabella
  `channel`, un adattatore, nessun campo con il nome del fornitore nello schema), così che un secondo canale
  sia un lavoro di settimane e non una riscrittura.
- **Sospensione del numero del negozio** per punteggio di qualità basso — il negozio perde il canale e dà la
  colpa a noi. *Attenuazione*: mostrare il punteggio di qualità, avvisare prima di una campagna a rischio,
  rendere la disiscrizione immediata e visibile.
- **Costo imprevedibile per il cliente** — la lamentela numero uno del segmento (§2.5). *Attenuazione*: la
  stima del costo prima dell'invio (storia `0023`) e il contatore dei messaggi con modello sempre visibile.
- **Margine eroso** se si sceglie la Via B senza differenziare per mercato — sei volte di differenza fra India
  e Brasile. *Attenuazione*: Via A, oppure listini per area geografica, che però la piattaforma oggi non
  prevede.
- **Sovrapposizione interna alla suite** con le app 12 e 16 — costruire due volte la stessa casella o le
  stesse campagne. *Attenuazione*: il confine dichiarato al §10, da riverificare quando quelle app verranno
  scritte.

**Fuori dimensionamento**: non applicabile. 6 epiche (fascia 4-7), 4-6 storie per epica (fascia 4-8), 29 storie
in tutto (fascia 20-45).
