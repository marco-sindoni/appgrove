# StockGrove — descrizione dell'applicazione

**Numero di catalogo**: 14 · **Tipo**: orizzontale · operations · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 14](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** StockGrove tiene il conto di quanta merce c'è, dov'è e come ci è arrivata. Ogni fatto — merce
ricevuta, merce uscita, merce spostata da un deposito all'altro, differenza trovata contando — si scrive come
**movimento** in un registro che si può solo accrescere; la giacenza di un articolo in un deposito non è un numero
che qualcuno modifica, è la **somma dei movimenti**. Sopra a questo registro l'app fa tre cose utili tutti i
giorni: dice cosa sta finendo prima che finisca (soglie di scorta e proposta di riordino), permette di contare
davvero quello che c'è e di registrare la differenza con il motivo per cui c'era (inventario fisico e rettifica), e
si lascia comandare dal telefono inquadrando un codice a barre o un codice QR.

**Per chi.** Micro-imprese da 1 a 10 addetti e piccole imprese fino a 50 che tengono merce fisica: negozi,
laboratori artigiani, installatori con un furgone e un magazzino, piccoli produttori, chi vende online da un
magazzino proprio, chi tiene ricambi e materiale di consumo. Compra il titolare; usa tutti i giorni chi riceve la
merce al banco di carico e chi la preleva per un lavoro o per una spedizione — spesso non sono la stessa persona e
quasi mai stanno davanti a un computer fisso. Mercato globale con priorità europea.

**Quale problema toglie.** Oggi la giacenza di una micro-impresa vive in tre posti che non concordano mai: un
foglio di calcolo aggiornato quando qualcuno se ne ricorda, la testa del magazziniere e gli scaffali veri. Il costo
è doppio e simmetrico. Da una parte la **rottura di scorta**: si promette al cliente un articolo che sulla carta
c'è e nel magazzino no, e si perde l'ordine o si paga un trasporto urgente. Dall'altra il **capitale immobilizzato**:
si ricompra ciò che c'era già perché nessuno riusciva a dirlo con sicurezza, e il denaro resta fermo su uno
scaffale. In mezzo c'è il fatto che nessuno sa **perché** i conti non tornano: senza registro dei movimenti una
differenza è solo un numero sbagliato, con il registro è una domanda a cui si può rispondere.

**Cosa NON fa.**

- **Non fa la contabilità di magazzino a valore ai fini fiscali** e non produce le scritture ausiliarie di
  magazzino previste dall'art. 14, lettera d), del decreto del Presidente della Repubblica 600/1973. Calcola un
  **valore gestionale** delle giacenze — quanto vale, in ordine di grandezza, quello che ho sugli scaffali — con un
  metodo dichiarato, e lo esporta perché il commercialista lo usi come base. La valutazione delle rimanenze ai fini
  del bilancio e della dichiarazione (metodi ammessi dall'art. 92 del testo unico delle imposte sui redditi, ultimo
  costo, media ponderata, primo entrato-primo uscito, svalutazioni) **è materia del commercialista** e resta fuori.
  Il confine è spiegato per esteso al §2.3 e nella storia [`0025`](04-inventario-fisico-e-rettifiche/0025-valore-gestionale-delle-giacenze.md).
- **Non gestisce lotti, date di scadenza e numeri di matricola.** Sono requisiti veri in alimentare, farmaceutico e
  in chi vende beni con garanzia, ma cambiano il modello dei movimenti (un movimento va riferito al lotto, non solo
  all'articolo) e vanno progettati da chi ha quel dominio in mano — i verticali e WarrantyGrove (51). Vedi §11.
- **Non gestisce varianti come entità a sé.** Una taglia, un colore, un formato sono **articoli distinti con codice
  proprio**: è come lavorano davvero le micro-imprese e come funziona il concorrente più vicino per taglia (Sortly).
  L'albero prodotto → variante è un problema di anagrafica avanzata e appartiene a PimGrove (43).
- **Non fa produzione**: niente distinta base, niente ordini di lavorazione, niente semilavorati che si consumano
  per fabbricarne altri.
- **Non manda ordini ai fornitori.** Produce una **proposta di riordino** — cosa comprare, quanto, da chi — e la
  esporta; la trasmissione dell'ordine, la conferma e il ciclo degli acquisti sono di ProcureGrove (48).
- **Non vende**: niente cassa, niente scontrini, niente carrello. La vendita è di ShopGrove (29) e la fatturazione
  di BillGrove (02); StockGrove riceve da loro il fatto «venduto» e ne fa uno scarico.
- **Non spedisce**: etichette di trasporto, corrieri e tracciamento delle consegne sono di MoveGrove (30).
- **Non possiede il prezzo di vendita né i listini commerciali.** Il confine con il catalogo prodotti condiviso è
  al §10 e nella storia [`0012`](02-anagrafiche-e-catalogo-prodotti/0012-confine-con-il-catalogo-prodotti-condiviso.md).

**Rischio di sostituzione da parte dei modelli linguistici.** `neutra`, come dice il catalogo, e il motivo è che
qui non c'è testo da generare: c'è un **saldo da tenere vero**. Un assistente generico non sa quanti pezzi ci sono
in un deposito, e nessun modello linguistico può sostituire il fatto che qualcuno abbia registrato il carico. Il
livello conversazionale però cambia il **modo** di usare l'app in maniera sostanziale: «quante ne ho in furgone?»,
«scaricane due per il lavoro di stamattina», «cosa devo ricomprare?» sono esattamente le domande che si fanno in
piedi, con le mani occupate, e che oggi nessuno fa perché richiedono di aprire un programma. Il valore sta nei dati
proprietari (il registro dei movimenti) e nel flusso di lavoro sul campo, non nella generazione.

---

## 2. Mercato e analisi in rete

> Compilata dopo nove ricerche mirate e quattro letture dirette di pagine ufficiali, con dodici fonti citate
> ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4). Ciò che non è stato trovato è dichiarato al §2.7.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| Zoho Inventory | globale (India) | giacenze multi-sede, ordini di acquisto e di vendita, integrazione con i mercati elettronici | Free (50 ordini/mese, 1 utente, 2 sedi); Standard 27 €/mese; Professional 74 €/mese; Premium 121 €/mese; Enterprise 234 €/mese — tutti a fatturazione annuale, tetto sugli **ordini al mese** (500 / 3.000 / 7.500 / 15.000); sedi aggiuntive 10 $/mese; prova 14 giorni | [zoho.com/inventory/pricing](https://www.zoho.com/inventory/pricing/) — pagina ufficiale |
| Sortly | globale (Stati Uniti) | inventario visuale mobile, scansione con il telefono, avvisi di scorta bassa | Free (100 articoli unici, 1 utente); Advanced 49 $/mese (24 $ il primo anno), **500 articoli**; Ultra 149 $/mese, **2.000 articoli**; Premium 299 $/mese, **5.000 articoli**; prova 14 giorni | [sortly.com/pricing](https://www.sortly.com/pricing/) — pagina ufficiale |
| Fatture in Cloud | Italia | fatturazione con magazzino incluso dal piano medio: carichi, scarichi, giacenze, costo medio d'acquisto | Forfettari 4 €/mese; Standard 12 €; **Premium 21 €** (primo piano con la gestione magazzino); Premium Plus 29 €; Complete 51 €; prova 31 giorni senza carta | [fattureincloud.it/costo](https://www.fattureincloud.it/costo/) · [pagina magazzino](https://www.fattureincloud.it/software-fatturazione/gestione-magazzino/) — pagine ufficiali |
| Odoo Inventory | globale (Belgio) | magazzino completo dentro un sistema gestionale integrato | «una app gratis per sempre» con utenti illimitati se si usa **solo** Inventory; dal secondo modulo ~24,90-31,10 $/utente/mese. **Non rilevato su pagina ufficiale**: dati da fonti terze | [erpresearch.com/pricing/odoo](https://www.erpresearch.com/pricing/odoo) — sito terzo, **non** pagina ufficiale |
| inFlow Inventory | globale (Canada) | magazzino con ordini di acquisto e vendita per piccole e medie imprese | **Non rilevato su pagina ufficiale**: fonti terze indicano Entrepreneur 161-186 $/mese, Small Business 436 $/mese, Mid-Size 874-999 $/mese | [inflowinventory.com — guida ai costi](https://www.inflowinventory.com/blog/inventory-management-software-cost/) — pagina del fornitore ma non listino |

**Lettura.** Il mercato si divide in tre fasce nette. In alto ci sono i prodotti da centinaia di dollari al mese
(inFlow) che il nostro cliente non guarda nemmeno. In mezzo c'è la fascia 27-121 €/mese (Zoho) che vende
**capacità di elaborare ordini**: è un magazzino pensato per chi vende molto, e il micro-cliente paga per un
contatore che non consuma. In basso c'è il magazzino **come funzione accessoria della fatturazione** (Fatture in
Cloud, 21 €/mese) e il gratuito (Odoo con la sua app singola, più una nutrita schiera di prodotti gratuiti nelle
directory italiane). Lo spazio scoperto è quello di Sortly — un'app **mobile-first che conta le cose e basta** —
ma portato in Europa, in cinque lingue, a un prezzo europeo e non americano: Sortly di listino chiede 49 $/mese per
500 articoli, che per un artigiano italiano è fuori scala.

### 2.2 Prezzi praticati nel dominio

- **Unità di misura prevalente**: due, e sono in competizione fra loro. Gli **ordini al mese** (Zoho, inFlow) —
  cioè un consumo su finestra — e gli **articoli unici a catalogo** (Sortly) — cioè una giacenza. La differenza non
  è cosmetica: la prima fa pagare l'attività, la seconda la dimensione dell'inventario.
- **Sedi/depositi**: quasi sempre una leva di prezzo a parte (Zoho: 2 sedi incluse, poi 10 $/mese l'una).
- **Fasce rilevate su pagina ufficiale**: 27-234 €/mese (Zoho), 49-299 $/mese di listino (Sortly), 21-51 €/mese
  per il pacchetto fatturazione+magazzino italiano (Fatture in Cloud).
- **Piano gratuito**: presente e generoso in Zoho (50 ordini/mese), presente ma stretto in Sortly (100 articoli),
  presente e totale in Odoo se si usa una sola app. **Il gratuito in questo dominio è la norma, non l'eccezione.**
- **Prova gratuita**: 14 giorni in Zoho e Sortly; 31 giorni senza carta in Fatture in Cloud.
- La fascia del catalogo (15-39 €/mese piatti oppure per magazzino) è **coerente** con il mercato italiano e
  sensibilmente sotto quello anglosassone: regge.

### 2.3 Obblighi normativi del settore — dove sta il confine con il commercialista

Questo è il paragrafo che decide il perimetro del prodotto, e va letto prima delle funzioni.

1. **Le scritture ausiliarie di magazzino sono obbligatorie solo sopra soglie che il nostro cliente non tocca.**
   L'art. 14, primo comma, lettera d), del decreto del Presidente della Repubblica 600/1973 impone le scritture
   ausiliarie di magazzino a chi supera **contemporaneamente**, per due esercizi consecutivi, **5.164.568,99 € di
   ricavi** e **1.032.913,80 € di rimanenze**; l'obbligo decorre dal secondo periodo d'imposta successivo al secondo
   superamento e cessa con lo stesso meccanismo a scendere. Le quantità da registrare sono entrate e uscite di
   merci, semilavorati classificati distintamente, prodotti finiti, materie prime, imballaggi, più cali, sfridi e
   variazioni inventariali. **Effetto sul prodotto**: la micro-impresa e la piccola impresa a cui parliamo sono
   fuori dall'obbligo, quindi StockGrove **non deve** essere un registro fiscale — ma il registro dei movimenti che
   costruiamo assomiglia molto a quello che la norma descrive, e questo va detto senza promettere conformità.
   Fonti: [circolare SGB Studio sulle scritture ausiliarie](https://www.sgbstudio.it/2015/circolare-n-22-del-06062015-scritture-ausiliarie-di-magazzino/) ·
   [testo dell'art. 14 DPR 600/73](https://trovalegge.it/accertamento-redditi/art-14-dpr-600-73-scritture-contabili-delle-imprese-commerciali-delle-societ%C3%A0-e-degli-enti-equiparati).
2. **La valutazione delle rimanenze è un atto contabile e fiscale, non una funzione di magazzino.** La scelta del
   metodo (ultimo costo, media ponderata, primo entrato-primo uscito), le svalutazioni, il raccordo con il bilancio
   e la dichiarazione stanno in capo a chi tiene la contabilità. **Effetto sul prodotto**: StockGrove calcola un
   valore **gestionale** a costo medio ponderato mobile, lo etichetta come tale in ogni schermata e in ogni
   esportazione, e non usa mai la parola «rimanenze» in senso fiscale. Il numero serve a rispondere a «quanto
   capitale ho fermo?», non a compilare un rigo di dichiarazione.
3. **Il registro dei movimenti tocca il lavoro delle persone.** Ogni movimento porta chi l'ha fatto e quando: è
   indispensabile per capire una differenza, ed è al tempo stesso un dato sull'attività di un lavoratore. In Italia
   l'art. 4 della legge 300/1970 (Statuto dei lavoratori) regola gli strumenti da cui può derivare un controllo a
   distanza dell'attività, con l'eccezione degli strumenti usati per rendere la prestazione — e con l'obbligo, in
   ogni caso, di **informare adeguatamente** il lavoratore. **Effetto sul prodotto e sulla conformità**: il dato si
   tiene perché serve alla tracciabilità della merce, non si costruiscono classifiche di produttività per persona,
   e il punto va scritto nel manifesto dei dati (§6). Non è un dato di categoria particolare, ma non è nemmeno
   neutro: chi lo tratta come neutro sbaglia.
4. **I codici a barre dei prodotti non li generiamo noi.** Il codice EAN/GTIN di un prodotto destinato alla vendita
   nasce da un prefisso aziendale **noleggiato** a GS1 (in Italia, per fatturati fino a 500.000 €: 300 € di
   iscrizione e 95 € l'anno per un pacchetto di 1.000 codici), e non può essere inventato né assegnato a prodotti
   altrui. **Effetto sul modello dati**: l'articolo ha due identificativi distinti e non intercambiabili — il
   **codice interno**, che l'impresa decide e che StockGrove può stampare su un'etichetta, e il **codice GTIN**,
   che si registra se esiste ma non si genera. Fonte: [GS1 Italy — adesione e prefisso aziendale](https://gs1it.org/iscriviti/).
5. **Fuori dall'Italia non ho verificato nulla.** Le soglie del punto 1 e il riferimento del punto 3 sono italiani.
   Francia, Germania e Spagna hanno regole proprie sulle scritture obbligatorie e sul controllo del lavoro che
   **non ho controllato**: è dichiarato al §2.7 e ripreso al §11.

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta, da quanto emerge dalle pagine dei concorrenti e dalle recensioni lette:

1. **Fatturazione e documenti di trasporto** — è l'aspettativa numero uno, e Fatture in Cloud la soddisfa
   generando i movimenti **automaticamente** all'emissione del documento. Dentro la suite è BillGrove (02) e
   l'ingresso avviene per eventi (storia `0019`); verso l'esterno sarebbe un fornitore terzo.
2. **Vendita in negozio e negozio online** — ogni vendita è uno scarico. Dentro la suite è ShopGrove (29).
   I connettori diretti verso i grandi negozi online **non sono nel perimetro iniziale**: vedi §11.
3. **Acquisti** — la proposta di riordino vorrebbe diventare un ordine vero. Dentro la suite è ProcureGrove (48).
4. **Contabilità / commercialista** — non un collegamento tecnico ma un'**esportazione** dell'inventario contato e
   valorizzato, da consegnare a fine anno (storia `0025`).
5. **Lettori di codici a barre esterni** — quelli che si collegano al telefono o al computer e si comportano come
   una tastiera: non sono un'integrazione software, ma il campo di scansione deve accettarli.
6. **Foglio di calcolo** — importare l'anagrafica esistente e i movimenti storici è la condizione per cominciare
   (storie `0011` e `0018`). Nessuno migra ribattendo a mano quattrocento articoli.

Nessuna di queste integrazioni, nella forma proposta qui, introduce un **fornitore esterno nuovo che tratti dati
personali per nostro conto**: sono tutti scambi interni alla suite, per eventi, oppure file che l'utente scarica.
È una delle poche app del catalogo in cui questo paragrafo è corto per davvero.

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Cosa chiedono:

- **sapere quanto ce n'è, subito, dal telefono**, mentre si è davanti allo scaffale;
- **essere avvisati prima** che un articolo finisca, non dopo (le fonti sulle scorte minime insistono su un punto:
  le soglie impostate un anno fa non descrivono più le vendite di oggi, quindi la soglia dev'essere facile da
  cambiare e il sistema deve suggerire quando è sbagliata);
- che il magazzino **si muova da solo** quando si emette un documento di vendita: è la funzione che Fatture in
  Cloud vende come ovvia e che diventa il metro di paragone;
- **contare una volta l'anno senza impazzire**, e poter registrare la differenza con una spiegazione.

Cosa rifiutano:

- la **profondità inutile**: le recensioni del segmento sono esplicite — «la ricchezza di funzioni può essere
  eccessiva per un uso di solo inventario», «se ti serve uno specialista dedicato per l'avviamento, il programma è
  troppo complicato». È il rifiuto più netto emerso e vale come vincolo di progetto: **ogni schermata in più va
  giustificata**;
- **l'avviso che non porta a niente**: un elenco di articoli sotto scorta che non produce almeno una lista della
  spesa viene percepito come lavoro in più, non in meno. È la ragione della storia `0028`;
- il **prezzo per utente**: in magazzino le persone che toccano la merce sono più di quelle che stanno al computer,
  e far pagare i posti significa spingere il cliente a far scansionare tutto a una persona sola — cioè a rompere
  proprio la tracciabilità che vendiamo.
  Fonti: [Ply — inventory management software for small business](https://www.getply.com/blog/inventory-management-software-small-business/) ·
  [Kladana — guida agli avvisi di scorta bassa](https://www.kladana.com/blog/wms/low-stock-alerts-guide/).

### 2.6 Fonti consultate

1. **Zoho Inventory — pagina ufficiale dei prezzi** — https://www.zoho.com/inventory/pricing/ — piani da 27 a
   234 €/mese con tetto sugli **ordini al mese** e sedi vendute a parte: mostra la fascia media europea e il fatto
   che le sedi sono una leva di prezzo.
2. **Sortly — pagina ufficiale dei prezzi** — https://www.sortly.com/pricing/ — tetto sugli **articoli unici**
   (100 / 500 / 2.000 / 5.000): è la conferma diretta che una metrica **a giacenza** esiste e funziona in questo
   dominio, ed è il riferimento della proposta al §5.
3. **Fatture in Cloud — pagina ufficiale dei prezzi** — https://www.fattureincloud.it/costo/ — la gestione
   magazzino compare dal piano Premium a 21 €/mese: è il prezzo che un cliente italiano ha in testa quando pensa
   «software con magazzino», e il tetto psicologico contro cui si misura una proposta.
4. **Fatture in Cloud — pagina della gestione magazzino** — https://www.fattureincloud.it/software-fatturazione/gestione-magazzino/
   — i movimenti si generano **automaticamente** da fattura e documento di trasporto, con giacenze e costo medio
   d'acquisto: definisce l'aspettativa di automatismo (storia `0019`).
5. **Circolare SGB Studio n. 22/2015 — scritture ausiliarie di magazzino** —
   https://www.sgbstudio.it/2015/circolare-n-22-del-06062015-scritture-ausiliarie-di-magazzino/ — le due soglie
   (5.164.568,99 € di ricavi e 1.032.913,80 € di rimanenze, per due esercizi consecutivi) e la decorrenza
   dell'obbligo: è la fonte che autorizza a **non** costruire un registro fiscale.
6. **Testo dell'art. 14 del DPR 600/1973** — https://trovalegge.it/accertamento-redditi/art-14-dpr-600-73-scritture-contabili-delle-imprese-commerciali-delle-societ%C3%A0-e-degli-enti-equiparati
   — la fonte primaria della lettera d) e dell'elenco di ciò che andrebbe registrato.
7. **GS1 Italy — adesione e prefisso aziendale** — https://gs1it.org/iscriviti/ — il prefisso è **noleggiato**, non
   di proprietà, costa 300 € di iscrizione più 95 € l'anno fino a 500.000 € di fatturato e dà 1.000 codici: è la
   ragione per cui l'app registra i codici GTIN ma non li genera.
8. **Descartes Finale — guida alla scelta del lettore di codici a barre** —
   https://www.finaleinventory.com/guides/barcode-scanner-for-inventory/ — la fotocamera del telefono impiega 1-2
   secondi di messa a fuoco per lettura, soffre la scarsa luce e non legge alcuni codici lineari vecchi; sotto una
   cinquantina di letture al giorno basta il telefono, sopra serve un lettore dedicato. È il paragrafo che
   **dimensiona la promessa** sulla scansione (storie `0030`-`0032`).
9. **Kladana — guida agli avvisi di scorta bassa** — https://www.kladana.com/blog/wms/low-stock-alerts-guide/ — le
   soglie invecchiano con le vendite e vanno riviste; un avviso che non genera un ordine viene percepito come
   limitante.
10. **Ply — software di magazzino per la piccola impresa** —
    https://www.getply.com/blog/inventory-management-software-small-business/ — «la profondità di funzioni può
    essere eccessiva per un uso di solo inventario» e «se serve uno specialista dedicato per l'avviamento, il
    programma è troppo complicato»: il vincolo di semplicità di questo progetto.
11. **ERP Research — prezzi Odoo 2026** — https://www.erpresearch.com/pricing/odoo — «una app gratis per sempre»
    con utenti illimitati per il solo Inventory, poi ~24,90-31,10 $/utente/mese: **fonte terza**, non pagina
    ufficiale, ma sufficiente a segnalare che il concorrente gratuito esiste ed è credibile.
12. **inFlow — guida al costo dei programmi di magazzino** —
    https://www.inflowinventory.com/blog/inventory-management-software-cost/ — pagina del fornitore sui costi di
    categoria; i prezzi puntuali di inFlow riportati al §2.1 vengono da siti di comparazione e **divergono fra
    loro** (161 $ contro 186 $ per lo stesso piano).

### 2.7 Cosa NON sono riuscito a determinare

- **Prezzi ufficiali di inFlow e di Odoo**: le rispettive pagine di listino non sono state lette direttamente; i
  numeri riportati vengono da fonti terze e in un caso divergono fra loro. Per chiuderlo serve una lettura diretta.
- **Un concorrente italiano di solo magazzino con listino pubblico**: le ricerche restituiscono directory
  (Capterra Italia) e nomi di prodotti stand-alone — Invoicex, MerciGest, ASA Software — ma **non ho letto un
  listino pubblico** per nessuno di essi. La proposta del §5 si appoggia quindi al riferimento indiretto di Fatture
  in Cloud e a Sortly, non a un concorrente italiano diretto.
- **Quanti micro-imprenditori usino davvero la fotocamera del telefono in modo continuativo** invece di un lettore
  dedicato: nessuna fonte con dati sul segmento. La guida di Finale dà una soglia operativa (~50 letture al giorno)
  ma è un'indicazione del fornitore, non una rilevazione.
- **L'entità reale delle rotture di scorta nel segmento micro europeo**: le cifre in circolazione (4-8 % delle
  vendite perse) sono citate da fornitori senza indagine indipendente. **Non le uso**: gli indicatori dell'app
  misurano il dato del cliente, non lo confrontano con una media inventata.
- **Se i clienti accettino la giacenza negativa** o la considerino un difetto: è una scelta di prodotto rilevante
  (§11, punto 3) e non ho trovato materiale che la illumini.
- **Le regole non italiane** su scritture di magazzino e su controllo dell'attività dei lavoratori (§2.3 punto 5):
  non verificate per nessun altro paese europeo.

---

## 3. Varco d'identità — le risposte pronte per `new-application`

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `magazzino` | Rispetta `^[a-z][a-z0-9_]{0,30}$`. Segue la convenzione già viva nel repository, dove l'app #1 è `fatture`: identificativo tecnico in italiano, nome commerciale in inglese («StockGrove»). Descrive **cosa l'app è** — il posto dove si sa quanta merce c'è — e resta valido se il nome commerciale cambia. Alternative scartate: `stock` (in italiano è ambiguo, si usa anche per «esaurito») e `inventario` (in italiano l'inventario è l'**atto di contare**, che qui è una funzione fra le altre, non l'app intera). |
| **Modello utente** | `multi` | Il magazzino è per definizione un lavoro a più mani: chi riceve la merce al banco di carico non è chi la preleva per un lavoro, e quasi mai è chi decide cosa ricomprare. Soprattutto, **l'intero valore del registro dei movimenti dipende dal sapere chi ha fatto cosa**: senza `created_by` per movimento, una differenza d'inventario è un mistero e la rettifica non ha un responsabile. Con il modello a utente singolo quel «chi» non esiste, e l'app perde la sua ragion d'essere. Attenzione: `multi` riguarda le persone dell'account, **non** la metrica di quota — i posti qui non si pagano (§2.5). |
| **Porta locale** | `8114` | Convenzione del kit: 8100 + 14. Da confermare con `./dev.sh services` al momento dello scaffolding. |
| **Metrica di quota** | `articoli_gestiti` | È la sola cosa che il piano limita: il numero di articoli **attivi** in anagrafica, cioè la dimensione dell'inventario che l'app tiene in ordine. Cresce con il valore ricevuto (più referenze = più difficile tenerle a mente = più serve il programma) ed è la metrica che il concorrente più vicino per taglia usa davvero (Sortly: 100 / 500 / 2.000 / 5.000 articoli unici, §2.2). **Scartate, e il perché conta più della scelta**: (a) i **movimenti registrati** — contare i movimenti significa mettere un prezzo sull'atto di registrare, cioè disincentivare esattamente il comportamento da cui dipende la correttezza della giacenza; un cliente vicino al tetto smetterebbe di registrare gli scarichi e il saldo marcirebbe, con il nostro contatore a fargli da complice. È l'errore peggiore che si possa fare su questa app; (b) i **depositi** — Zoho li vende a parte, ma una micro-impresa ne ha uno o due e la metrica non discriminerebbe nulla; (c) i **posti a sedere** — vedi §2.5. |
| **Natura della metrica** | `stock` | È un tetto su ciò che esiste ora: «500 articoli gestiti» significa che per aggiungerne uno oltre il tetto bisogna archiviarne un altro. Non è un consumo su finestra: un articolo censito l'anno scorso occupa un posto anche oggi, e archiviarlo lo libera davvero. Se lo trattassimo come consumo mensile, un cliente potrebbe accumulare diecimila articoli in un piano da cinquecento senza che nulla lo fermi, e il tetto non significherebbe niente. Conseguenza da tenere presente (§13 dei principi): il **passaggio a un piano inferiore è bloccato** finché gli articoli attivi superano il tetto di destinazione — comportamento corretto, ma va spiegato in interfaccia, non subìto. |
| **Colore-categoria e icona** | `amber` · icona `boxes` | Deve essere lo stesso nel listino (`category`) e nel modulo frontend (`accentToken`). `amber` è il colore delle operazioni e dell'attenzione, ed è coerente con un'app la cui schermata più importante è un elenco di cose che stanno per finire. Resta distinto dal `blue` della fatturazione e dal `violet` proposto per i preventivi (06), che sono le app con cui StockGrove condivide il catalogo prodotti: si vuole che a colpo d'occhio si riconoscano come cose diverse. Nel repository i due colori delle app reali sono `green`/`blue` per `fatture` e `blue`/`teal` per `crm`, con **un disallineamento fra listino e manifesto da verificare al momento dello scaffolding** (§11, punto 8). |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Articolo` | la cosa di cui si tiene il conto | codice interno, descrizione, unità di misura, categoria, codice GTIN facoltativo, origine (locale o dal catalogo condiviso), attivo/archiviato | no |
| `Deposito` | il luogo dove la merce sta | codice, nome, tipo (magazzino, negozio, furgone), indirizzo facoltativo, predefinito sì/no | no — l'indirizzo è di un luogo dell'impresa |
| `Ubicazione` | dove esattamente, dentro il deposito | etichetta libera (scaffale, ripiano, cassetta) | no |
| `Movimento` | **il fatto**: un pezzo di merce entrato, uscito, spostato o corretto | tipo, articolo, deposito, ubicazione, quantità con segno, motivo, riferimento al documento d'origine, chiave di idempotenza, momento, autore | indirettamente: l'autore è una persona dell'account |
| `Giacenza` | **il saldo**: quanti ce ne sono ora, per articolo e deposito | quantità, versione, momento dell'ultimo aggiornamento, ultimo movimento applicato | no |
| `MotivoMovimento` | perché è successo | codice, etichetta, segno ammesso, se richiede una nota obbligatoria | no |
| `Inventario` | una sessione di conteggio fisico | deposito, ambito (tutto / una categoria / una ubicazione), stato, apertura, chiusura, autore | indirettamente: l'autore |
| `RigaInventario` | un articolo contato | quantità attesa congelata all'apertura, quantità contata, differenza, motivo della differenza | no |
| `RegolaScorta` | la soglia sotto cui suonare | articolo, deposito, scorta minima, scorta di sicurezza, quantità di riordino, fornitore preferito | no |
| `PropostaRiordino` | la lista della spesa | articoli, quantità suggerite, fornitore, momento di generazione, stato (aperta, esportata, archiviata) | no |
| `Fornitore` | da chi si compra | ragione sociale, persona di riferimento, posta elettronica, telefono, identificativo fiscale | **sì** |
| `CostoArticolo` | il costo medio ponderato mobile, aggiornato dai carichi | costo medio, valuta, momento dell'ultimo aggiornamento | no |
| `ScansioneInCoda` | una lettura fatta dal telefono e non ancora confermata | codice letto, tipo di movimento previsto, quantità, chiave di idempotenza, stato | indirettamente: l'autore |

**Relazioni.** `Articolo` 1→N `Movimento`; `Deposito` 1→N `Movimento`; `(Articolo, Deposito)` 1→1 `Giacenza`
(riga unica per coppia); `Movimento` N→1 `MotivoMovimento`; `Inventario` 1→N `RigaInventario` e ogni riga chiusa
con differenza genera **un** `Movimento` di rettifica; `(Articolo, Deposito)` 1→0..1 `RegolaScorta`;
`RegolaScorta` N→1 `Fornitore`; `PropostaRiordino` N→1 `Fornitore`; `Articolo` 1→1 `CostoArticolo`;
`ScansioneInCoda` 1→0..1 `Movimento` (quando viene confermata).

**Il vincolo che governa tutto: il registro è in sola aggiunta e la giacenza è derivata.**

```
        ┌──────────────────────────────────────────────────────────┐
        │  movimento  (SOLO INSERT — mai UPDATE, mai DELETE)       │
        │  carico +12 · scarico −3 · trasferimento −5/+5 ·         │
        │  rettifica ±n (con motivo) · storno ∓n (con rimando)     │
        └───────────────────────────┬──────────────────────────────┘
                                    │  stessa transazione
                                    ▼
        ┌──────────────────────────────────────────────────────────┐
        │  giacenza (articolo, deposito) = Σ quantità con segno    │
        │  proiezione materializzata + versione + ultimo_movimento │
        └──────────────────────────────────────────────────────────┘
                                    ▲
                     ricostruibile in qualunque momento
                     rileggendo il registro dall'inizio
```

Le tre regole che ne discendono e che **nessuna storia può violare**:

1. **un movimento non si modifica e non si cancella**: un errore si corregge con un movimento di **storno** che lo
   rimanda, così la storia resta leggibile («ho caricato 12 per sbaglio, poi ho stornato») invece di diventare
   falsa («ho sempre caricato 0»);
2. **nessuno scrive la giacenza a mano**: anche la correzione fatta contando è un movimento di **rettifica**, con
   un motivo obbligatorio. Una casella «quantità» modificabile in una schermata sarebbe il difetto d'origine di
   tutta l'applicazione;
3. **la giacenza deve poter essere ricostruita dal registro** e confrontata con la proiezione: se le due divergono,
   la verità è il registro e la proiezione va rifatta (storia `0024`).

**Perché la proiezione esiste, visto che è ricostruibile.** Perché la domanda che si fa mille volte al giorno è
«quanti ce ne sono?», e sommare ogni volta cinque anni di movimenti sarebbe assurdo. La proiezione è una comodità
di lettura; l'autorità resta al registro. Questa è anche la ragione per cui la proiezione porta una **versione**:
serve a risolvere la concorrenza (sotto) e a riconoscere gli aggiornamenti persi.

**Come si risolve la concorrenza fra due movimenti sullo stesso articolo.** Due persone che scaricano lo stesso
articolo nello stesso istante sono il caso normale, non il caso limite. La regola è: **l'aritmetica si fa nella
base di dati, non in memoria**. In una sola transazione si inserisce il movimento e si esegue un aggiornamento
condizionato della riga di giacenza — `SET quantita = quantita + :delta, versione = versione + 1` con la
condizione `quantita + :delta >= 0` — di modo che le due transazioni si serializzino sul blocco della riga e la
seconda veda il valore già aggiornato dalla prima. Chi perde la corsa non ottiene un saldo sbagliato: ottiene
`409` con «giacenza insufficiente: ne restano 2». Non si legge mai la giacenza in Java per poi riscriverla: quella
sequenza (leggo 5, calcolo 5−3, scrivo 2) è esattamente il modo in cui due scarichi da 3 su 5 pezzi lasciano 2
pezzi invece di rifiutarne uno. In più ogni movimento porta una **chiave di idempotenza** univoca per account, così
che un invio ripetuto — il telefono che ritenta perché la rete è andata via — non conti due volte lo stesso fatto.

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica
(`deleted_at`); schema `app_magazzino`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8). **Un'avvertenza sulla colonna `deleted_at` di
`movimento`**: esiste perché è lo standard di piattaforma, ma l'applicazione **non la valorizza mai** — cancellare
logicamente un movimento significherebbe cambiare il passato. L'unica correzione è lo storno. La colonna resta
disponibile solo per la cancellazione fisica prevista dai diritti dell'interessato e dalla chiusura dell'account.

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il
> file `services/core/src/main/resources/pricing/magazzino.yaml`.

**Ragionamento.** Il riferimento non è Zoho (27-234 €/mese per capacità di elaborare ordini) né inFlow (centinaia
di dollari), ma i due prezzi che il nostro cliente ha davvero in testa: **21 €/mese**, che è quanto costa in Italia
il pacchetto fatturazione con magazzino incluso (Fatture in Cloud, §2.1), e **zero**, che è quanto costa Odoo se si
usa una sola app. Contro il primo si compete offrendo un magazzino che fa più cose di un accessorio della
fatturazione; contro il secondo si compete sulla semplicità e sul telefono, non sul prezzo. La fascia del catalogo
(15-39 €/mese) è coerente e la confermo. I tetti nascono dal confronto con Sortly, che è il concorrente che usa la
stessa metrica: Sortly dà 500 articoli a 49 $/mese di listino e 2.000 a 149 $. La proposta sta molto sotto, perché
il segmento europeo micro non regge quei prezzi e perché StockGrove non offre (ancora) la profondità di Sortly su
foto e campi personalizzati. Non esiste alcun costo variabile per movimento — nessun fornitore si paga a
scansione — quindi il margine è pulito e non c'è nessun motivo tecnico per contare i movimenti.

| Piano | Prezzo mensile | Prezzo annuale | Limite su `articoli_gestiti` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | 50 | — | il laboratorio con poche referenze e chi vuole provare sul lavoro vero: bastano a capire se serve, non a farci l'anno |
| `pro` | 19 € | 190 € (= 10× il mensile, «due mesi in regalo») | 500 | 14 giorni | il negozio, l'artigiano, l'installatore con un magazzino e un furgone: è il piano di riferimento |
| `business` | 39 € | 390 € | 5.000 | 14 giorni | la piccola impresa con più depositi, chi vende anche online, chi tiene ricambi |

**Note obbligate.**

- Tre piani, non di più: aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile.
- Un limite lasciato vuoto significa **illimitato**, non zero. Qui nessun piano è illimitato: il tetto sugli
  articoli è la sola leva che distingue `pro` da `business`, e lasciarlo vuoto per sbaglio regalerebbe il piano
  alto.
- **I depositi non sono la metrica, ma dovrebbero distinguere i piani.** Zoho li vende a parte e ha ragione: un
  cliente con quattro depositi vale più di uno con uno solo. Poiché la piattaforma ammette **una sola metrica di
  quota** (§7 dei principi), la proposta è di tenere `articoli_gestiti` come metrica e di esprimere il numero di
  depositi come **caratteristica del piano** nella mappa `features` — `free`: 1 deposito, `pro`: 3, `business`:
  illimitati. **È una proposta da verificare**: non ho riscontrato nel repository come le `features` vengano
  applicate a runtime, e se non fossero applicabili l'alternativa onesta è dare i depositi illimitati a tutti i
  piani a pagamento, non fingere un limite che nessuno fa rispettare.
- **La prova gratuita su un'app che ha già un piano gratuito è in parte ridondante**, ed è vero anche qui. La
  tengo lo stesso, a 14 giorni, per un motivo specifico di questo dominio: il valore di StockGrove si vede solo
  **dopo** aver caricato l'anagrafica vera, e cinquanta articoli non bastano a caricare l'anagrafica vera. La prova
  serve a far entrare i dati. Se lo sviluppatore preferisce, disattivarla è legittimo — ma allora il piano gratuito
  va allargato, altrimenti nessuno arriva a vedere il prodotto.
- **Costo effettivo dell'incasso**: nessun piano è sotto i 5 €/mese, quindi la parte fissa per transazione non
  mangia il margine. L'annuale resta comunque quello da spingere.
- I prezzi sono **immutabili una volta vivi**: un cambio si fa creando un prezzo nuovo, non modificando l'esistente.
- 🛑 **Punto delicato del listino, da decidere insieme — il blocco al superamento del tetto.** Su una metrica a
  giacenza il blocco a `429` colpisce la creazione di un articolo nuovo. **Non deve mai colpire la registrazione di
  un movimento su un articolo che esiste già**: impedire a un'impresa di registrare uno scarico perché ha finito il
  piano significa corrompere il suo saldo e restituirle un dato falso quando tornerà a pagare. La proposta è
  quindi: quota esaurita = non si aggiungono articoli; i movimenti passano sempre. Questa è una scelta di prodotto,
  la segnalo e non la do per acquisita.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/magazzino.yaml`) si compila **insieme** allo sviluppatore: «niente contratto, niente
> produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

**Questa è una delle poche app del catalogo che tratta pochissimi dati di persone, e lo dico chiaramente invece di
gonfiare la sezione.** L'oggetto dell'applicazione sono le **cose**: un articolo non è una persona, una giacenza
non è una persona, un movimento è un fatto su una cosa. I dati personali entrano da due sole porte, ed entrambe
sono strette.

**Categorie particolari (articolo 9): non previste.** Nessun campo dell'applicazione chiede o suggerisce dati su
salute, biometria, genetica, opinioni politiche, convinzioni religiose, orientamento sessuale o appartenenza
sindacale. Va chiarito un equivoco che in questo dominio si presenta: **la merce di una farmacia o di un
sanitario non è un dato sulla salute**. «Dodici confezioni di un farmaco in giacenza» è un dato su una scatola, non
su una persona; diventerebbe un dato sulla salute solo se fosse legato a un paziente identificato — e StockGrove
non ha il concetto di paziente, di cliente finale né di destinatario della merce. Se un domani si volesse legare
uno scarico alla persona che riceve il bene (uno studio veterinario, una clinica), **quello sarebbe un cambiamento
di natura dell'app** e andrebbe rivalutato da capo, non aggiunto come campo.

> ⚠️ **Il punto che invece va guardato in faccia: il registro dice chi ha fatto cosa.** Ogni movimento porta
> l'identificativo dell'utente che l'ha registrato e il momento. È indispensabile — senza quel dato una differenza
> d'inventario non si spiega e una rettifica non ha un responsabile — ma è anche un dato sull'**attività di un
> lavoratore**, e in Italia gli strumenti da cui può derivare un controllo a distanza dell'attività ricadono
> nell'art. 4 dello Statuto dei lavoratori (legge 300/1970), con l'obbligo in ogni caso di informare adeguatamente
> la persona. Non è un dato di categoria particolare e l'app resta uno «strumento usato per rendere la
> prestazione», ma la conseguenza di prodotto è netta e vincolante per tutte le storie: **niente classifiche di
> produttività per persona, niente indicatori del tipo "movimenti registrati per operatore"**. Il dato serve alla
> tracciabilità della merce, non alla sorveglianza di chi la muove.

**Categorie trattate**

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `fornitore.ragione_sociale` | `fornitore.ragione_sociale` | fornitore dell'account | anagrafico — **personale quando il fornitore è una ditta individuale o un professionista** | sapere da chi si ricompra | esecuzione del contratto / legittimo interesse del titolare a gestire il rapporto | durata del rapporto + 10 anni dall'ultimo movimento collegato |
| `fornitore.persona_riferimento` | `fornitore.persona_riferimento` | persona di contatto presso il fornitore | anagrafico | sapere a chi si telefona per riordinare | legittimo interesse del titolare | come sopra |
| `fornitore.email` | `fornitore.email` | persona di contatto | contatto | recapitare la proposta di riordino esportata | legittimo interesse del titolare | come sopra |
| `fornitore.telefono` | `fornitore.telefono` | persona di contatto | contatto | contatto operativo | legittimo interesse del titolare | come sopra |
| `fornitore.identificativo_fiscale` | `fornitore.partita_iva`, `fornitore.codice_fiscale` | fornitore | identificativo — **personale se persona fisica** | identificare correttamente la controparte | obbligo di legge in capo al titolare quando l'acquisto diventa un documento contabile | come sopra |
| `movimento.autore` | `movimento.created_by` (e `updated_by` delle altre tabelle) | dipendente o collaboratore dell'account | **dato sull'attività lavorativa** | tracciabilità: chi ha caricato, scaricato, rettificato | legittimo interesse del titolare alla tracciabilità della merce, con informativa al lavoratore (§2.3 punto 3) | 10 anni, come il registro |
| `inventario.autore` e `riga_inventario.contato_da` | omonimi | dipendente o collaboratore | come sopra | sapere chi ha contato | come sopra | come sopra |
| `articolo.descrizione`, `movimento.nota`, `riga_inventario.nota` | testo libero | chiunque venga nominato nel testo | **imprevedibile** | descrivere la merce e spiegare una differenza | legittimo interesse del titolare | come il registro |

**Ruoli.** Su questi dati **appgrove è responsabile del trattamento** (tratta per conto del cliente, che è il
titolare). Finalità e basi giuridiche indicate sopra sono quelle del cliente-titolare e nel manifesto vanno scritte
come tali. Le durate proposte derivano per analogia dal termine di prescrizione ordinaria e dalla durata di
conservazione dei documenti contabili: **non sono un dato rilevato**, vanno validate.

**Esportazione e cancellazione.** Devono comparire **tutte** in `exportData` e `purgeData` del contratto
`MagazzinoDataContract`, senza eccezioni: `fornitore`, `movimento` (per l'autore), `inventario` e
`riga_inventario` (per l'autore e le note), `scansione_in_coda` (per l'autore), `regola_scorta` e
`proposta_riordino` (per il riferimento al fornitore), `articolo` (per il testo libero della descrizione). La
cancellazione è **fisica**: sostituire la ragione sociale del fornitore con un codice non è cancellare.
**Il caso difficile va detto adesso**: cancellare l'autore di un movimento significa togliere dal registro proprio
il dato che rende il registro utile, e cancellare un movimento intero è impossibile per costruzione. La proposta è
di distinguere due cose — la richiesta che riguarda un **fornitore** (i suoi dati si cancellano e i movimenti
restano con il riferimento vuoto, perché il fatto «sono entrati 12 pezzi» non è un dato sul fornitore) e la
richiesta che riguarda un **lavoratore dell'account**, dove il conflitto fra il diritto della persona e l'interesse
del titolare alla tracciabilità **non lo risolve questa applicazione**. È un punto aperto (§11, punto 5).

**Testo libero.** Le descrizioni degli articoli, le note dei movimenti e quelle delle righe d'inventario sono
campi liberi: sono l'unico ingresso non presidiato per dati che nessuno ha previsto («scaricato per il signor
Rossi che ha problemi di deambulazione»). L'applicazione non fa rilevazione di contenuto; l'interfaccia avvisa
(«campo a testo libero: non inserire dati sensibili») e il presidio, se servirà, è trasversale.

**Integrazioni esterne.** **Nessuna, nel perimetro proposto.** Gli scambi con le altre app della suite avvengono
per eventi interni alla piattaforma; le esportazioni sono file che l'utente scarica e consegna lui. Non si aggiunge
nessun fornitore esterno che tratti dati per nostro conto. Questo cambierebbe il giorno in cui si aggiungessero i
connettori verso i negozi online o l'invio dell'ordine al fornitore (§11, punti 2 e 6): entrambi vanno fuori
perimetro anche per questo motivo, non solo per taglia.

**Classificazione della change.** Una app nuova introduce comunque finalità nuove e uno schema nuovo, quindi è un
cambiamento **sostanziale** — ma per una ragione diversa dal solito: non per la quantità di dati personali, che qui
è minima, bensì perché introduce il trattamento dei **dati di attività dei lavoratori dell'account**, che la
piattaforma finora non tratta. Lo confermo senza attenuanti, e segnalo che è quel punto, non l'anagrafica
fornitori, a meritare l'attenzione in sede di valutazione.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `leggi_giacenza` | `(codice_o_gtin, deposito?) → quantità per deposito, con totale` | l'azione `get_stock` della scheda di catalogo | lettura | no |
| `trova_articolo` | `(testo_o_codice) → articolo, ubicazione, giacenza per deposito` | l'azione `locate_item`: «dov'è finito?» | lettura | no |
| `elenca_articoli` | `(categoria?, deposito?, solo_sotto_scorta?, pagina?) → elenco minimizzato` | l'elenco con i filtri che si usano a voce | lettura | no |
| `elenca_sotto_scorta` | `(deposito?) → articoli sotto soglia con quantità suggerita` | l'azione `list_reorder` | lettura | no |
| `storico_movimenti` | `(articolo, deposito?, periodo?) → movimenti con motivo e autore` | la risposta a «perché ne mancano tre?» | lettura | no |
| `registra_carico` | `(articolo, deposito, quantità, motivo?, riferimento?) → bozza di movimento` | l'azione `receive_shipment` | scrittura | **sì** |
| `registra_scarico` | `(articolo, deposito, quantità, motivo?, riferimento?) → bozza di movimento` | toglie merce; può essere rifiutata per giacenza insufficiente | scrittura | **sì** |
| `trasferisci` | `(articolo, deposito_origine, deposito_destinazione, quantità) → bozza di coppia di movimenti` | sposta senza cambiare il totale | scrittura | **sì** |
| `rettifica_giacenza` | `(articolo, deposito, quantità_reale, motivo) → bozza di rettifica` | l'azione `adjust_inventory`: **cambia il saldo dichiarando che il registro era sbagliato** | scrittura, effetto non annullabile se non con un altro movimento | **sì, obbligatoria, con il motivo scritto** |
| `storna_movimento` | `(id_movimento, motivo) → bozza di storno` | annulla un movimento aggiungendone uno contrario | scrittura | **sì** |
| `chiudi_inventario` | `(id_inventario) → bozza con l'elenco completo delle differenze` | genera **molte** rettifiche in un colpo solo | scrittura, effetto ampio | **sì, obbligatoria, con l'elenco delle differenze mostrato prima** |

**Quello che non è e non sarà uno strumento.** Tre cose. **Cancellare un movimento**: non esiste come operazione,
né dalla chat né dall'interfaccia — l'unica correzione è lo storno, che è a sua volta un movimento. **Impostare la
giacenza a un numero**: la sola via per cambiare un saldo è `rettifica_giacenza`, che pretende un motivo; uno
strumento «imposta giacenza a N» sarebbe la porta di servizio per aggirare tutto il modello. **Mandare l'ordine al
fornitore**: l'app non manda niente a nessuno fuori dall'azienda (§1), quindi non c'è nessuno strumento con effetti
verso l'esterno — il che rende questa app insolitamente sicura da esporre a un assistente.

**Riga di lettura.** Il paio `trova_articolo` + `leggi_giacenza` è la ragione per cui questa app guadagna dal
livello conversazionale più delle sue concorrenti: «quante ne ho e dove sono?» è una domanda che si fa **in piedi,
con le mani occupate, davanti a uno scaffale**, ed è esattamente la situazione in cui nessuno apre un programma. Il
secondo motivo è `elenca_sotto_scorta`: la lista della spesa chiesta a voce il venerdì sera è il lavoro che nelle
micro-imprese non fa nessuno perché richiede di sedersi.

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine dell'epica l'app esiste, è vuota, si accende dal catalogo, si apre nel backoffice in cinque lingue e
rifiuta con `429` chi supera il tetto di articoli del proprio piano.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | Il servizio `magazzino` nasce dallo scaffolding, risponde su `/api/magazzino/v1`, ha la sua istanza di infrastruttura |
| [0002](01-fondamenta/0002-modello-dati-multi-account.md) | Modello dati multi-account | Schema `app_magazzino`, prima migrazione, tabella degli articoli con `tenant_id` e colonne di controllo |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Modulo registrato, sezioni, tema chiaro e scuro, cinque lingue, elenco vuoto navigabile |
| [0004](01-fondamenta/0004-abbonamento-e-quota.md) | Abbonamento e quota | Catena dei varchi completa e metrica `articoli_gestiti` a giacenza, con blocco a `429` sulla sola creazione |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | `./dev.sh services` vede l'app; un magazzino inventato di una ventina di articoli la rende dimostrabile in un minuto |

### Epica 02 — Anagrafiche e catalogo prodotti

Alla fine dell'epica l'app sa **cosa** conta, **dove** lo tiene e **da chi** lo ricompra, sa rispondere a una
richiesta di esportazione o cancellazione, e ha dichiarato chi possiede l'anagrafica di prodotto.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-anagrafiche-e-catalogo-prodotti/0006-anagrafica-degli-articoli.md) | Anagrafica degli articoli | Codice interno, descrizione, unità di misura, categoria, attivazione e archiviazione |
| [0007](02-anagrafiche-e-catalogo-prodotti/0007-identificativi-e-codici-a-barre.md) | Identificativi e codici a barre | Il codice GTIN si registra e si cerca, non si genera; più codici per lo stesso articolo |
| [0008](02-anagrafiche-e-catalogo-prodotti/0008-depositi-e-ubicazioni.md) | Depositi e ubicazioni | Magazzino, negozio, furgone; l'ubicazione come etichetta libera dentro il deposito |
| [0009](02-anagrafiche-e-catalogo-prodotti/0009-anagrafica-dei-fornitori.md) | Anagrafica dei fornitori | L'unica tabella dell'app fatta di dati di persone, e il fornitore preferito per articolo |
| [0010](02-anagrafiche-e-catalogo-prodotti/0010-manifesto-dati-e-diritti-dell-interessato.md) | Manifesto dei dati e diritti dell'interessato | Manifesto in italiano e inglese e contratto di esportazione e cancellazione dell'app |
| [0011](02-anagrafiche-e-catalogo-prodotti/0011-importazione-dell-anagrafica-da-file.md) | Importazione dell'anagrafica da file | Nessuno migra ribattendo a mano quattrocento articoli: anteprima, errori per riga, nessun caricamento parziale muto |
| [0012](02-anagrafiche-e-catalogo-prodotti/0012-confine-con-il-catalogo-prodotti-condiviso.md) | Confine con il catalogo prodotti condiviso | Chi possiede l'identità del prodotto e chi possiede la sua quantità: proiezione locale alimentata a eventi |

### Epica 03 — Registro dei movimenti e giacenze

È il cuore tecnico dell'applicazione. Alla fine dell'epica ogni fatto che cambia la merce è un movimento
irrevocabile nel registro, la giacenza è la loro somma, e due persone che scaricano lo stesso articolo nello
stesso istante ottengono un risultato corretto.

| # | Storia | In una riga |
|---|---|---|
| [0013](03-registro-dei-movimenti-e-giacenze/0013-registro-dei-movimenti-e-giacenza-derivata.md) | Registro dei movimenti e giacenza derivata | Il modello dati in sola aggiunta, la proiezione con versione, la chiave di idempotenza |
| [0014](03-registro-dei-movimenti-e-giacenze/0014-carico-della-merce.md) | Carico della merce | La merce entra: quantità, deposito, motivo, riferimento al documento, aggiornamento del costo medio |
| [0015](03-registro-dei-movimenti-e-giacenze/0015-scarico-della-merce.md) | Scarico della merce | La merce esce, la giacenza non va sotto zero e due scarichi simultanei non si sovrascrivono |
| [0016](03-registro-dei-movimenti-e-giacenze/0016-trasferimento-fra-depositi.md) | Trasferimento fra depositi | Due movimenti opposti in una sola transazione: il totale non cambia, la collocazione sì |
| [0017](03-registro-dei-movimenti-e-giacenze/0017-storno-di-un-movimento.md) | Storno di un movimento | L'unico modo di correggere un errore senza cancellare il passato |
| [0018](03-registro-dei-movimenti-e-giacenze/0018-importazione-massiva-dei-movimenti.md) | Importazione massiva dei movimenti | Il carico iniziale e i movimenti storici da file, con idempotenza per riga |
| [0019](03-registro-dei-movimenti-e-giacenze/0019-movimenti-dagli-eventi-delle-altre-app.md) | Movimenti dagli eventi delle altre app | Vendita e documento di trasporto diventano scarichi da soli — anche quando il saldo va in negativo |
| [0020](03-registro-dei-movimenti-e-giacenze/0020-evento-giacenza-variata.md) | Evento «giacenza variata» | Ciò che StockGrove racconta alle app a valle, in una forma che invecchia bene |

### Epica 04 — Inventario fisico, rettifiche e valore

Alla fine dell'epica si può contare quello che c'è davvero, registrare la differenza con il motivo per cui c'era,
verificare che la proiezione non abbia mentito, e dire quanto vale il magazzino sapendo cosa quel numero non è.

| # | Storia | In una riga |
|---|---|---|
| [0021](04-inventario-fisico-e-rettifiche/0021-rettifica-con-motivo-obbligatorio.md) | Rettifica con motivo obbligatorio | La sola via per cambiare un saldo, e la ragione per cui non esiste una casella «quantità» modificabile |
| [0022](04-inventario-fisico-e-rettifiche/0022-sessione-di-inventario-fisico.md) | Sessione di inventario fisico | Si apre, si congela l'atteso, si conta in più mani, si può interrompere e riprendere |
| [0023](04-inventario-fisico-e-rettifiche/0023-chiusura-dell-inventario-e-differenze.md) | Chiusura dell'inventario e differenze | L'elenco delle differenze mostrato prima di confermare, poi una rettifica per riga |
| [0024](04-inventario-fisico-e-rettifiche/0024-ricostruzione-della-giacenza-dal-registro.md) | Ricostruzione della giacenza dal registro | Il controllo che dimostra che la proiezione dice la verità, e cosa si fa quando non la dice |
| [0025](04-inventario-fisico-e-rettifiche/0025-valore-gestionale-delle-giacenze.md) | Valore gestionale delle giacenze | Costo medio ponderato mobile, etichettato per quello che è, ed esportabile per il commercialista |

### Epica 05 — Scorte minime e riordino

Alla fine dell'epica l'app smette di limitarsi a registrare il passato e comincia a dire cosa fare: cosa sta per
finire, quanto ricomprarne e da chi.

| # | Storia | In una riga |
|---|---|---|
| [0026](05-scorte-minime-e-riordino/0026-soglie-di-scorta-per-articolo-e-deposito.md) | Soglie di scorta per articolo e deposito | Scorta minima, scorta di sicurezza e quantità di riordino, impostabili in massa |
| [0027](05-scorte-minime-e-riordino/0027-avviso-di-sotto-scorta.md) | Avviso di sotto scorta | L'elenco di ciò che è sceso sotto soglia, con il riepilogo periodico e senza molestie |
| [0028](05-scorte-minime-e-riordino/0028-proposta-di-riordino.md) | Proposta di riordino | La lista della spesa raggruppata per fornitore, modificabile ed esportabile: l'avviso che porta a qualcosa |
| [0029](05-scorte-minime-e-riordino/0029-consumi-e-giorni-di-copertura.md) | Consumi e giorni di copertura | Quanto dura la scorta al ritmo attuale, e la segnalazione delle soglie diventate sbagliate |

### Epica 06 — Scansione e lavoro sul campo

Alla fine dell'epica il magazzino si governa dal telefono, davanti allo scaffale, e una rete che va e viene non
produce mai un doppio conteggio.

| # | Storia | In una riga |
|---|---|---|
| [0030](06-scansione-e-lavoro-sul-campo/0030-scansione-del-codice-con-la-fotocamera.md) | Scansione del codice con la fotocamera | Inquadrare un codice a barre o un codice QR e arrivare all'articolo, con la via manuale sempre disponibile |
| [0031](06-scansione-e-lavoro-sul-campo/0031-movimento-rapido-da-scansione.md) | Movimento rapido da scansione | Dalla lettura al movimento in due tocchi, con il deposito già scelto e la sessione che continua |
| [0032](06-scansione-e-lavoro-sul-campo/0032-coda-delle-scansioni-e-invio-idempotente.md) | Coda delle scansioni e invio idempotente | La rete che manca non ferma il lavoro e non fa contare due volte lo stesso pezzo |
| [0033](06-scansione-e-lavoro-sul-campo/0033-etichette-con-codice-interno.md) | Etichette con codice interno | Per la merce che un codice non ce l'ha: codice QR interno, stampabile in foglio |

### Epica 07 — Esposizione conversazionale e prove end-to-end

Alla fine dell'epica ogni funzione dell'app è comandabile da una chat con la regola «l'assistente prepara, la
persona approva», e due percorsi automatici dimostrano che il registro e il conteggio reggono davvero.

| # | Storia | In una riga |
|---|---|---|
| [0034](07-esposizione-conversazionale-e-prove/0034-strumenti-di-lettura.md) | Contratto degli strumenti di lettura | Cinque strumenti liberi con dati minimizzati: giacenza, ricerca, elenco, sotto scorta, storico |
| [0035](07-esposizione-conversazionale-e-prove/0035-strumenti-di-scrittura-con-conferma.md) | Strumenti di scrittura con bozza e conferma | Carico, scarico, trasferimento, rettifica, storno: bozza sempre, motivo obbligatorio sulla rettifica |
| [0036](07-esposizione-conversazionale-e-prove/0036-percorso-end-to-end-dei-movimenti.md) | Percorso end-to-end dei movimenti | Dall'articolo vuoto al saldo corretto passando per due scarichi simultanei, etichettato `[J-MAGAZZINO]` |
| [0037](07-esposizione-conversazionale-e-prove/0037-percorso-end-to-end-dell-inventario.md) | Percorso end-to-end dell'inventario | Contare, trovare una differenza, rettificarla e ritrovarla nel registro; registro di copertura aggiornato |

**Totale**: 7 epiche, 37 storie — **tutte scritte**, insieme a [estensioni-admin.md](estensioni-admin.md) e al mockup
navigabile [artefatto-ux.html](artefatto-ux.html). Ogni riga di questo indice punta a un file che esiste; le
migrazioni citate dalle storie sono numerate senza salti né doppioni da `V1__articolo.sql` a
`V21__bozze_strumenti.sql`.

---

## 9. Estensioni della console di amministrazione

Servono tre cose oltre lo standard, tutte diagnostiche e nessuna che guardi i dati del cliente: una **deroga
temporanea al tetto di articoli gestiti** per chi importa la sua anagrafica il primo mese, una **vista sullo stato
degli scambi a eventi e delle importazioni da file** (quante in coda, quante fallite, con quale codice di errore),
e soprattutto un **contatore delle divergenze fra registro e proiezione per account** — è il segnale di salute
specifico di questa app: se in un account la somma dei movimenti smette di coincidere con la giacenza pubblicata,
lo si deve sapere prima che lo scopra il cliente.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| **06 — QuoteGrove (preventivi)** e **02 — BillGrove (fatturazione)** | condividono dati con — **confine dichiarato** | **Catalogo prodotti e listini**. Vedi il riquadro qui sotto: è il confine più importante di questa scheda. |
| **29 — ShopGrove (POS e micro-retail)** | alimenta StockGrove | Ogni vendita è uno scarico. ShopGrove emette l'evento della vendita, StockGrove lo trasforma in movimento (storia `0019`). Nessuna chiamata diretta: solo eventi asincroni. |
| **48 — ProcureGrove (acquisti e ordini)** | a valle | La proposta di riordino (`0028`) è l'ingresso naturale di un ordine di acquisto. StockGrove **si ferma alla proposta**; l'ordine, la conferma e il ciclo passivo sono di 48. Quando 48 esisterà, l'ordine ricevuto tornerà indietro come carico. |
| **30 — MoveGrove (logistica leggera)** | complementare | La spedizione consuma merce; StockGrove ne registra lo scarico. Etichette di trasporto, corrieri e tracciamento restano a 30. |
| **43 — PimGrove (PIM leggero)** | complementare, confine da presidiare | Varianti, attributi, schede prodotto ricche e contenuti per canale sono di 43. StockGrove tiene dell'articolo il minimo che serve a contarlo. Se StockGrove cominciasse a fare varianti costruiremmo due volte la stessa cosa, e male. |
| **51 — WarrantyGrove (garanzie e resi)** | complementare | Numeri di matricola e ritorni dal cliente. Il reso è un carico per StockGrove; la garanzia è di 51. |
| **20 — InsightGrove (analytics)** | alimenta | Rotazione, copertura, capitale immobilizzato: StockGrove pubblica i propri indicatori (`0029`), l'analisi trasversale è di 20. |
| **verticali con merce** (21 beauty, 22 ristorazione, 24 artigiani, 25 edilizia, 58 veterinaria) | potenziali consumatori | Chiunque tenga materiale di consumo o ricambi può usare questa app invece di una tabella dentro il proprio verticale. |

> ### Il confine sull'anagrafica di prodotto — chi possiede cosa
>
> Il catalogo (§6, «Sinergie») indica **catalogo prodotti e listini** come una delle quattro entità condivise della
> suite, comune a preventivi (06), fatturazione (02), magazzino (14), retail (29) e verticali. QuoteGrove lo ha
> impostato così: nella sua storia `0008` il catalogo è **anagrafico e di prezzo** e la sua sezione «fuori ambito»
> dice espressamente «giacenze e movimenti di magazzino: sono di StockGrove (catalogo 14)». Questa scheda accetta e
> completa quel confine, e lo dichiara in una riga sola:
>
> **il catalogo condiviso possiede l'identità commerciale del prodotto; StockGrove possiede la sua quantità.**
>
> In concreto:
>
> | Dato | Chi lo possiede | Chi lo legge |
> |---|---|---|
> | codice, descrizione, unità di misura, categoria, tipo (prodotto/servizio) | **catalogo condiviso** (oggi: QuoteGrove `0008`, domani l'anagrafica di suite) | StockGrove, in proiezione locale |
> | prezzo di vendita, listini, sconti, aliquota d'imposta | **catalogo condiviso** — StockGrove non li vede e non li vuole | 06, 02, 29 |
> | codice GTIN, ubicazione, deposito, giacenza, movimenti, costo medio d'acquisto, soglie di scorta | **StockGrove** | chi vuole, tramite l'evento `giacenza.variata` (`0020`) |
> | disponibilità mostrata in un preventivo o su un negozio online | nessuno dei due la «possiede»: è la giacenza di StockGrove **letta** da altri | 06, 29 |
>
> Regole operative del confine, valide finché la suite non esiste e anche dopo:
> 1. **StockGrove non inventa prezzi di vendita**, e non li mostra nemmeno per comodità: se comparissero in una sua
>    schermata, il giorno dopo qualcuno chiederebbe di modificarli da lì;
> 2. **il catalogo condiviso non tiene giacenze**: se le tenesse, esisterebbero due saldi e uno dei due sarebbe
>    sbagliato;
> 3. **finché il catalogo condiviso non esiste**, StockGrove tiene la propria tabella `articolo` (storia `0006`)
>    con il campo `origine = locale`; quando esisterà, gli articoli con `origine = condivisa` diventano una
>    **proiezione locale alimentata a eventi**, in sola lettura per i campi di identità (storia `0012`);
> 4. **un articolo può nascere in StockGrove** — un ricambio che nessuno vende, un materiale di consumo interno — e
>    restare locale per sempre: non tutto ciò che si conta si vende;
> 5. **nessuna chiamata sincrona fra le app**, mai: solo eventi (§2 dei principi di piattaforma).

**Riga di lettura.** StockGrove **ha senso da sola**: un negozio che vuole solo sapere quanta merce ha e cosa sta
finendo compra questa e basta — è la definizione di applicazione piccola e autosufficiente venduta ad abbonamento
che il catalogo persegue, ed è il caso in cui la concorrenza (Sortly) è più forte. Dentro la suite però cambia di
ruolo: diventa **il punto in cui la catena del documento tocca la realtà fisica**. Un preventivo accettato, una
fattura emessa, uno scontrino battuto sono tutte promesse; la giacenza è il fatto. È per questo che l'evento
`giacenza.variata` (`0020`) va scritto bene fin dall'inizio anche se oggi non lo ascolta nessuno.

**Sovrapposizioni da evitare.** Quattro, e tutte già chiuse sopra: le varianti e le schede prodotto ricche sono di
PimGrove (43); l'ordine al fornitore è di ProcureGrove (48); la vendita è di ShopGrove (29); il prezzo di vendita è
del catalogo condiviso. La quinta, meno ovvia e più pericolosa: **la valorizzazione fiscale delle rimanenze non è
di nessuna app di questo catalogo — è del commercialista**. La tentazione di aggiungere «e poi ti calcola anche le
rimanenze per il bilancio» è forte e sbagliata, perché sposta il prodotto in un mestiere regolato in cui un errore
non è un difetto ma un danno (§2.3, §11 punto 1).

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **Prezzi, limiti, prova gratuita e uso della mappa `features` per i depositi** (§5) | è una fermata di escalation: nessun agente fissa i prezzi; in più non ho verificato se le `features` del listino siano applicate a runtime | sviluppatore |
| 2 | **Lotti, date di scadenza e numeri di matricola** | cambiano il modello dei movimenti (il movimento andrebbe riferito al lotto e non solo all'articolo) e servono a interi settori che non sono il nostro cliente tipo; aggiungerli dopo è una migrazione, non una funzione | sviluppatore — direzione di prodotto, prima dell'epica 03 |
| 3 | **La giacenza può andare negativa?** | la proposta è: **no** per i movimenti fatti da una persona (`409` con la quantità residua), **sì** per quelli generati da un evento di vendita già avvenuto, perché rifiutare un fatto accaduto corromperebbe il registro. È una scelta di prodotto con conseguenze visibili in interfaccia e non l'ho trovata discussa in nessuna fonte (§2.7) | sviluppatore — direzione di prodotto |
| 4 | **Nome e promessa del valore di magazzino** (§2.3, storia `0025`) | il confine fra «valore gestionale» e «valutazione delle rimanenze» è chiaro a chi lo scrive e invisibile a chi lo legge; serve decidere come si chiama nell'interfaccia e nelle cinque lingue, perché una traduzione infelice ricrea la promessa che stiamo evitando | sviluppatore, con revisione dei testi |
| 5 | **Cancellazione dei dati dell'autore dei movimenti** (§6) | è al tempo stesso un dato personale di un lavoratore e l'elemento che rende il registro tracciabile; cosa prevale non lo decide questa app | sviluppatore con revisione legale |
| 6 | **Connettori verso i negozi online** (Shopify, WooCommerce, i grandi mercati elettronici) | il catalogo li elenca fra i casi d'uso principali («sincronizzazione e-commerce»), ma ognuno è un fornitore esterno, una superficie di manutenzione e un rischio di vendita oltre la giacenza. Nel perimetro proposto ci sono solo eventi interni e file. **È una riduzione consapevole del catalogo e va approvata** | sviluppatore — direzione di prodotto |
| 7 | **Regole non italiane** su scritture di magazzino e su controllo dell'attività dei lavoratori (§2.3 punto 5, §2.7) | non verificate per nessun altro paese europeo, mentre il prodotto nasce in cinque lingue | sviluppatore, prima di vendere fuori dall'Italia |
| 8 | **Colore-categoria `amber`** | nel repository il listino e il manifesto delle due app reali oggi non concordano fra loro sul colore: prima di scegliere va capito quale delle due fonti comanda | sviluppatore, al momento dello scaffolding |
| 9 | **Anagrafica di prodotto condivisa** (§10) | oggi StockGrove tiene la propria; quando nascerà l'anagrafica di suite servirà una migrazione e un criterio di riconciliazione per i codici duplicati | epica della suite |
| 10 | **Lettura del codice a barre e disegno del codice QR dentro il browser** (storie `0030`, `0033`) | emerso scrivendo l'epica 06: non ho verificato se il pacchetto frontend contenga già una libreria di decodifica e una di generazione. Nulla si può scaricare dalla rete, quindi o esistono già o vanno aggiunte al pacchetto — con il costo di manutenzione che ne segue | sviluppatore, prima dell'epica 06 |
| 11 | **Un solo telefono usato da più addetti** (storia `0032`) | emerso scrivendo la coda delle scansioni: se il dispositivo del magazzino è uno e lo usano in tre, l'autore dei movimenti è quello dell'accesso rimasto aperto e la tracciabilità — che è la ragione d'essere del registro — si annacqua. È un difetto d'uso, non di codice, e la risposta (accesso rapido per persona? scelta dell'operatore a inizio sessione?) è una scelta di prodotto | sviluppatore — direzione di prodotto |

**Rischi noti**

- **La corruzione silenziosa del saldo è il rischio esistenziale di questa app.** Se la giacenza smette di dire il
  vero, il cliente non se ne accorge subito: se ne accorge il giorno in cui promette merce che non ha, e a quel
  punto smette di fidarsi per sempre. Attenuazioni, tutte già dentro il progetto: registro in sola aggiunta,
  aritmetica nella base di dati, chiave di idempotenza su ogni movimento, ricostruzione periodica dal registro
  (`0024`) e contatore delle divergenze in console (§9).
- **Il gratuito è un concorrente vero.** Odoo regala l'app di magazzino con utenti illimitati e le directory
  italiane sono piene di prodotti gratuiti. Attenuazione: non si compete sul prezzo ma sulla semplicità, sul
  telefono e sull'essere dentro una suite che genera i movimenti da sola; e si dice chiaramente che il piano
  gratuito nostro è un assaggio, non un prodotto.
- **La promessa della scansione può essere sovradimensionata.** La fotocamera di un telefono impiega un paio di
  secondi a fuoco per lettura e in un magazzino buio fatica (§2.6, fonte 8). Attenuazione: dichiarare la soglia
  d'uso ragionevole, accettare i lettori esterni che si comportano da tastiera, e non mostrare mai nel materiale di
  vendita una persona che scansiona cento pezzi al minuto con un telefono.
- **Il testo libero è la porta d'ingresso per dati che nessuno ha previsto** (§6) — attenuazione: avviso in
  interfaccia; il presidio vero, se servirà, è trasversale.
- **Lo sconfinamento nella materia del commercialista** — è il rischio di posizionamento più insidioso, perché
  arriva sotto forma di richiesta gentile di un cliente («me lo fai anche per il bilancio?»). Attenuazione: il
  confine è scritto in tre posti diversi di questo documento e nella storia `0025`, e l'etichetta «valore
  gestionale» compare nell'interfaccia, non solo nella documentazione.

**Fuori dimensionamento**: non applicabile. Sette epiche (fascia 4-7), da quattro a otto storie per epica (fascia
4-8), trentasette storie in tutto (fascia 20-45). L'epica 03 è la più densa, con otto storie, e lo è di proposito:
è il cuore tecnico dell'applicazione e spezzarla di più produrrebbe storie che non consegnano niente di
osservabile.
