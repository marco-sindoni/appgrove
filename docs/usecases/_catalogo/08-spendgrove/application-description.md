# SpendGrove — descrizione dell'applicazione

**Numero di catalogo**: 08 · **Tipo**: orizzontale · finanza e amministrazione · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 08](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

> ⚠️ **Da non confondere con l'app 39 del catalogo, «SpendGrove SaaS».** Sono due prodotti diversi che condividono
> quasi lo stesso nome commerciale:
> - **08 SpendGrove** (questo documento) = **note spese dei collaboratori**: si fotografa la ricevuta di un pranzo,
>   di un taxi, di un albergo; si compone la nota spese; si approva; si rimborsa; si consegna tutto al
>   commercialista. Le persone di cui si trattano i dati sono **dipendenti e collaboratori dell'azienda cliente**.
> - **39 SpendGrove SaaS** = **spesa in abbonamenti software**: scoperta degli abbonamenti attivi, licenze
>   inutilizzate, scadenze di rinnovo, confronto dei prezzi dei fornitori. Le entità sono `Subscription`, `Vendor`,
>   `License`, `Seat`, `Renewal`.
>
> Poiché l'identificativo tecnico dell'app finisce nello schema del database, nelle rotte pubbliche e nel nome
> dell'istanza di infrastruttura, **non può collidere**: qui si propone `notespese` (§3), lasciando a chi scriverà
> la scheda 39 un identificativo del suo dominio (per esempio `saas_spend`). Se in futuro qualcuno decidesse di
> rinominare commercialmente una delle due, il nome del prodotto si cambia in un file di testo; l'identificativo
> tecnico no — cambiarlo è una migrazione di dati.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** SpendGrove raccoglie le spese anticipate dai collaboratori di una piccola azienda e le porta fino alla
contabilità. Il collaboratore fotografa la ricevuta dal telefono; l'app **legge automaticamente** i dati del
documento (esercente, data, importo totale, imposta sul valore aggiunto, mezzo di pagamento) e propone una spesa
già compilata e già categorizzata; la persona **rivede e conferma**; le spese confermate si raggruppano in una
**nota spese** che passa per l'approvazione di chi di dovere; le note approvate diventano rimborsi registrati e un
**pacchetto di esportazione per il commercialista** con i giustificativi allegati. Sopra tutto questo c'è un
cruscotto di spesa per periodo, categoria, collaboratore e centro di costo.

**Per chi.** Micro-imprese da 1 a 10 addetti e piccole imprese da 10 a 50, dove **non esiste un ufficio spese**:
compra il titolare o chi tiene l'amministrazione (spesso la stessa persona); usano tutti i giorni i collaboratori
che si muovono — tecnici in trasferta, commerciali, chi va a ritirare materiale. Mercato globale con priorità
europea; la prima messa a punto normativa è italiana, perché l'Italia è il mercato di partenza e ha regole di
tracciabilità e di conservazione precise (§2.3).

**Quale problema toglie.** Oggi, senza di noi, il ciclo è questo: il collaboratore accumula scontrini nel
portafoglio, a fine mese li impila sulla scrivania, qualcuno li ricopia in un foglio di calcolo, li incolla su un
foglio A4, li fotocopia e li porta al commercialista. Costa in tre modi: **tempo amministrativo** (ore di ricopiatura
per importi da poche decine di euro), **soldi persi** (scontrini smarriti = costo non dedotto, imposta non
recuperata) e **rischio fiscale** (dal 2025 in Italia un rimborso di vitto, alloggio o trasporto non tracciabile
rende il costo indeducibile per l'azienda e tassabile per il lavoratore — §2.3). Il foglio di calcolo non sa dire se
il pagamento era tracciabile; l'app sì.

**Cosa NON fa.**

- **Non esegue pagamenti.** Non fa bonifici, non emette carte, non si collega ai conti per disporre. Registra che un
  rimborso è stato liquidato, con il riferimento del pagamento fatto altrove. Emettere carte aziendali è il mestiere
  di un istituto di pagamento, non nostro.
- **Non fa contabilità.** Non tiene partita doppia, non produce dichiarazioni, non registra in prima nota: consegna
  un pacchetto ordinato a chi la contabilità la fa (interno o commercialista).
- **Non è un conservatore a norma.** Produce il pacchetto e le impronte dei documenti, ma la conservazione a norma
  dei giustificativi — quella che permette di **buttare la carta** — richiede un conservatore che risponda ai
  requisiti vigilati da AgID (§2.3, storia `0026`).
- **Non fa buste paga.** Il rimborso spese non è retribuzione: se serve farlo transitare dal cedolino, è mestiere di
  PayGrove (catalogo 10).
- **Non è un sistema di controllo dei lavoratori.** Non traccia posizioni, non ricostruisce spostamenti, non misura
  produttività (§6, avvertenza sul rapporto di lavoro).
- **Non gestisce gli abbonamenti software** dell'azienda: quello è l'app 39.

**Rischio di sostituzione da parte dei modelli linguistici.** `rafforzata`. Un modello generico sa già leggere una
foto di scontrino: la lettura, presa da sola, è una funzione di mercato, non un prodotto. Il valore che resta nostro
è il **flusso attorno alla lettura**: la revisione umana tracciata, la catena di approvazione con i ruoli, il legame
fra spesa, mezzo di pagamento tracciabile e deducibilità, l'archivio dei giustificativi con l'impronta, il pacchetto
che il commercialista accetta senza rifare il lavoro. Sono dati proprietari e obblighi, non testo generato. La
lettura automatica è quindi un **componente**, per giunta sostituibile (§6, fornitore esterno), non la ragione
d'essere dell'app.

---

## 2. Mercato e analisi in rete

> Compilata dopo 10 accessi in rete (8 ricerche mirate e 2 letture di pagina), elencati al §2.6.
> Ciò che non è stato trovato è dichiarato al §2.7, non colmato a intuito.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| Rydoo | Belgio, venduto in tutta l'Unione Europea | Note spese complete: lettura della ricevuta da telefono, rimborsi chilometrici, diarie, riconciliazione delle carte, flussi di approvazione a due livelli | **8 €/utente/mese** (piano *Essentials*, fatturazione annuale) o 10 €/utente/mese mensile; **10 €** (*Pro*, annuale) o 12 € mensile; **minimo 5 utenti** su entrambi | [rydoo.com/pricing](https://www.rydoo.com/pricing/) |
| Zoho Expense | India, venduto in Europa | Stesse funzioni, forte sull'applicazione delle politiche di spesa; il limite dichiarato non è solo sugli utenti ma **sulle scansioni di ricevuta** | **piano gratuito**: 3 utenti, **20 scansioni al mese per utente**; *Standard* **3 $/utente/mese** annuale (200 scansioni/utente/mese); *Premium* **5 $/utente/mese** annuale (1.000 scansioni); minimo 5 utenti sui piani a pagamento; prova di **14 giorni** senza carta | [zoho.com/expense/pricing](https://www.zoho.com/expense/pricing/) |
| Expensify | Stati Uniti, molto diffuso anche in Europa | Lettura della ricevuta («SmartScan»), carta propria, riconciliazione | fascia **5–18 $/utente/mese** (rilevata su sito di comparazione, non su pagina ufficiale) | [capterra.com — Rydoo e comparativi](https://www.capterra.com/p/128370/Xpenditure-Expenses/) |
| Soldo | Italia/Regno Unito | **Carte aziendali prepagate** più gestione delle spese: il controllo avviene a monte, sul mezzo di pagamento | **21 €/mese** (piano *Standard*, 3 utenti, fino a 20 carte) e **33 €/mese** (*Plus*), IVA esclusa, **prezzo per azienda e non per utente**; nessun piano gratuito, prova di **30 giorni** | [soldo.com/it-it/prezzo](https://www.soldo.com/it-it/prezzo/) (rilevato tramite scheda di sintesi, vedi §2.7) |
| SAP Concur | Globale | Riferimento delle grandi aziende | **20–35 $/utente/mese** (rilevato su sito di comparazione) | [itqlick.com/rydoo-expense/pricing](https://www.itqlick.com/rydoo-expense/pricing) |

**Lettura.** Il segmento è affollato in alto e vuoto in basso. Tutti i concorrenti internazionali vendono **per
utente al mese con un minimo di 5 utenti**: per un'azienda di tre persone con quindici scontrini al mese
significa pagare 5 posti per usarne 3, cioè 40–60 € l'anno di aria. Soldo, l'unico con listino a canone fisso per
azienda, vende in realtà **le carte**: la gestione delle spese è il contorno di un prodotto finanziario, e infatti
parte da 21 €/mese. Restano scoperte due cose: **il prezzo per una micro-impresa vera** (2–5 persone, poche decine
di ricevute al mese) e la **specificità fiscale italiana** — nessuno dei prodotti internazionali esaminati mette in
prima fila la tracciabilità del pagamento introdotta dal 2025, che è invece la ragione per cui in Italia una
ricevuta rimborsata «male» costa due volte (§2.3).

### 2.2 Prezzi praticati nel dominio

- **Unità di misura prevalente: per utente al mese**, con minimo di posti. Rilevato su pagina ufficiale per Rydoo
  (8–12 €) e Zoho Expense (3–5 $ annuale); su siti di comparazione per Expensify (5–18 $) e Concur (20–35 $).
- **Seconda unità, meno visibile ma reale: le scansioni di ricevuta.** Zoho Expense la mette nero su bianco (20 /
  200 / 1.000 al mese per utente): è l'ammissione che il costo variabile del prodotto non sta nell'utente, sta
  **nel documento letto**. È il dato più utile di tutta questa ricerca per la nostra scelta di metrica (§3).
- **Piano gratuito**: esiste in Zoho Expense (3 utenti, 20 scansioni al mese ciascuno); non esiste in Rydoo né in
  Soldo.
- **Durata della prova**: 14 giorni in Zoho Expense (dichiarata senza carta), 30 giorni in Soldo; per Rydoo la
  pagina dei prezzi non la dichiara.
- **Fascia del catalogo** (4–9 €/utente/mese oppure 19 €/mese piatti per micro-squadra): coerente con quanto
  rilevato, ma la nostra proposta si stacca dall'unità di misura prevalente per il motivo detto sopra (§5).

### 2.3 Obblighi normativi del settore

È la sezione che cambia il modello dati, quindi va letta prima delle storie. Tre blocchi, tutti italiani; le altre
giurisdizioni sono un punto aperto dichiarato (§2.7).

1. **Tracciabilità del pagamento (Legge di Bilancio 2025, L. 207/2024).** Dal 2025 le spese di vitto, alloggio,
   viaggio e trasporto con servizi pubblici non di linea (taxi e noleggio con conducente) sostenute in trasferta
   dai lavoratori **sono deducibili per l'azienda e non tassabili per il lavoratore solo se pagate con strumenti
   tracciabili** — bonifico, carta di credito o di debito, assegno, altri strumenti elettronici (art. 23 D.Lgs.
   241/1997 e art. 1 c. 679 L. 160/2019). Le norme toccate sono l'art. 51 c. 5, l'art. 54 c. 6-*ter*, l'art. 95
   c. 3-*bis* e l'art. 108 c. 2 del Testo unico delle imposte sui redditi. Attenzione a una distinzione che il
   modello dati deve reggere: per **trasporto** l'obbligo vale sia dentro sia fuori il Comune sede di lavoro; per
   **vitto e alloggio** vale solo fuori dal Comune. → **Conseguenza per l'app**: ogni spesa porta il **mezzo di
   pagamento** e la **collocazione rispetto al Comune sede di lavoro**, e l'app segnala prima dell'approvazione
   ciò che rischia l'indeducibilità (storie `0018`, `0020`).
2. **Documento fiscale e recupero dell'imposta sul valore aggiunto.** Il diritto a detrarre l'imposta si esercita
   **solo con una fattura intestata al soggetto passivo** che ha sostenuto la spesa: con un documento commerciale
   (il vecchio scontrino) l'imposta **non si detrae**, il costo resta deducibile se inerente. → **Conseguenza per
   l'app**: la spesa distingue il **tipo di documento** (documento commerciale / fattura / ricevuta fiscale) e
   l'app dice, per ogni riga, se l'imposta è recuperabile o no; non decide da sé le percentuali di indetraibilità
   di ristorazione e alberghi, che restano configurabili (storia `0024`).
3. **Conservazione dei giustificativi.** Le scritture e i documenti si conservano dieci anni; la carta si può
   distruggere solo se il documento digitale è portato in **conservazione a norma** secondo le Linee guida AgID,
   con firma digitale e marca temporale a garanzia di autenticità, integrità e immodificabilità. Il fatto che
   scontrini e ricevute siano documenti analogici **originali non unici** è ciò che rende possibile la
   dematerializzazione senza l'intervento di un pubblico ufficiale. → **Conseguenza per l'app**: si dichiara in
   modo esplicito che SpendGrove **non è un conservatore accreditato**; produce il pacchetto conservabile, le
   impronte e i riferimenti, e l'ultimo miglio è un fornitore terzo o un servizio della piattaforma (storia `0026`
   e punto aperto n. 4).

### 2.4 Integrazioni attese dal cliente

In ordine di quanto vengono chieste, secondo quello che i concorrenti mettono in vetrina:

1. **Uscita verso il commercialista o il gestionale contabile** — è *la* integrazione. In Italia i due poli sono
   TeamSystem (usato dagli studi più strutturati) e Fatture in Cloud (diffuso fra i commercialisti «digitali»);
   la scelta del cliente segue quasi sempre quella del suo studio. **Nel perimetro dell'app resta l'esportazione a
   file** (tabella dei movimenti + allegati); i collegamenti diretti sono un'evoluzione. *Fornitore esterno che
   tratterebbe dati per nostro conto: sì, se e quando si passa al collegamento diretto.*
2. **Movimenti delle carte aziendali e del conto** — per abbinare il movimento alla ricevuta. Due strade: importazione
   di un file di movimenti (nessun fornitore nuovo) oppure collegamento a un aggregatore di conti secondo la
   direttiva europea sui servizi di pagamento. *Fornitore esterno: sì, nella seconda strada.* Nel perimetro resta
   la prima (storia `0022`).
3. **Lettura automatica della ricevuta** — è dentro il prodotto, ma tecnicamente è quasi sempre un servizio di
   terzi (Klippa, Mindee, Veryfi e simili dichiarano tutti conformità al regolamento europeo sui dati personali;
   il trattamento in Unione Europea va però verificato contratto alla mano). *Fornitore esterno che tratta dati per
   nostro conto: **sì, ed è il più delicato** — vedi §6.*
4. **Posta elettronica** — inoltrare a un indirizzo dedicato la ricevuta ricevuta via posta (biglietti del treno,
   alberghi). Molto richiesta, tecnicamente semplice, fuori dal perimetro di questo primo giro (punto aperto n. 5).
5. **Buste paga** — far transitare il rimborso dal cedolino. È PayGrove (catalogo 10), non qui.

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Da quello che emerge dalle rassegne di prodotto e dalle recensioni aggregate (fonti 8 e 9 al §2.6):

- **Vogliono semplicità e prezzo basso**: le piccole imprese cercano «scansione della ricevuta e approvazione
  automatica», non la piattaforma completa. La complessità e l'interfaccia datata sono la critica ricorrente ai
  prodotti pensati per le grandi aziende.
- **Non si fidano della lettura automatica, e hanno ragione**: fra le lamentele ricorrenti c'è la lentezza e
  l'incostanza dell'estrazione. Chi vende promette «oltre il 99% di accuratezza», il che è già un'ammissione: il
  100% non esiste. → Questo è il motivo per cui, in questa app, la **revisione umana non è un passaggio
  facoltativo** ma il centro del flusso, e la **fiducia dichiarata campo per campo** è un requisito, non una
  raffinatezza (storie `0007`, `0008`).
- **Non vogliono** flussi di approvazione a più livelli, centri di costo gerarchici, politiche complesse: in
  un'azienda di sei persone chi approva è uno solo. Un secondo livello di approvazione va offerto ma non imposto.
- **Vogliono buttare la carta.** È l'aspettativa che più spesso resta delusa, perché richiede la conservazione a
  norma (§2.3). Meglio dirlo chiaramente in prodotto che lasciarlo credere.

### 2.6 Fonti consultate

1. **Rydoo — pagina ufficiale dei prezzi** — https://www.rydoo.com/pricing/ — piani *Essentials* 8/10 €, *Pro*
   10/12 € per utente al mese (annuale/mensile), minimo 5 utenti; funzioni divise fra i piani (diarie e
   riconciliazione carte solo dal *Pro*). È il riferimento europeo per l'unità di misura e per la fascia di prezzo.
2. **Zoho Expense — pagina ufficiale dei prezzi** — https://www.zoho.com/expense/pricing/ — piano gratuito con
   3 utenti e **20 scansioni al mese per utente**; *Standard* 3 $ (200 scansioni), *Premium* 5 $ (1.000); prova di
   14 giorni senza carta. La fonte da cui viene l'idea che la metrica giusta sia il **documento letto**.
3. **Legge di Bilancio 2025 e tracciabilità delle spese di trasferta** — https://www.altalex.com/documents/news/2025/03/09/nuovo-trattamento-fiscale-spese-trasferta-relativi-rimborsi-analitici-urgono-chiarimenti
   — quali articoli del Testo unico cambiano e perché un rimborso non tracciabile diventa indeducibile per
   l'azienda e imponibile per il lavoratore.
4. **Tracciabilità: dentro o fuori il Comune** — https://www.informazionefiscale.it/spese-trasferta-dipendenti-tracciabilita-deducibilita
   — la distinzione fra trasporto (sempre) e vitto/alloggio (solo fuori Comune): è un campo del modello dati, non
   una nota a piè di pagina.
5. **Tabelle ACI 2026 per rimborsi chilometrici e valore d'uso dei veicoli** — https://www.informazionefiscale.it/tabelle-ACI-2026-fringe-benefit-pdf-rimborso-auto-aziendale
   — le tariffe chilometriche esenti si calcolano sulle tabelle pubblicate ogni anno in Gazzetta Ufficiale e la
   trasferta dev'essere fuori dal Comune sede di lavoro. Da qui la scelta di **non incorporare le tabelle** nel
   prodotto ma di farle inserire o importare dall'account (storia `0019`).
6. **Conservazione a norma e documenti «originali non unici»** — https://www.teamsystem.com/magazine/identita-digitale/conservazione-sostitutiva-e-digitale-dei-documenti-fiscali-le-regole-per-larchiviazione/
   e https://ecosagile.com/ITA/news/Conservazione-digitale-nota-spese — condizioni per distruggere gli scontrini
   di carta: processo conforme alle Linee guida AgID, firma digitale e marca temporale sul pacchetto.
7. **Documento commerciale, fattura e recupero dell'imposta** — https://www.teamsystem.com/magazine/fatturazione-e-normativa/detraibilita-iva-e-deducibilita-per-alberghi-e-ristoranti/
   — senza fattura intestata l'imposta non si detrae; il costo resta deducibile se inerente. Da qui il campo «tipo
   di documento» e l'avviso in fase di revisione (storia `0024`).
8. **Rassegna dei prodotti di gestione spese per piccole imprese** — https://www.brex.com/spend-trends/expense-management/best-expense-management-software-solution
   — cosa cercano davvero le piccole imprese (semplicità, costo) e quali critiche ricorrono sui prodotti da grande
   azienda (complessità, interfaccia datata).
9. **Confronto prodotti e prezzi (siti di comparazione)** — https://www.itqlick.com/rydoo-expense/pricing e
   https://www.capterra.com/p/128370/Xpenditure-Expenses/ — fasce di Expensify (5–18 $) e Concur (20–35 $).
   Fonti **di comparazione, non ufficiali**: da leggere come ordini di grandezza (il catalogo avverte, §8).
10. **Fornitori di lettura automatica dei documenti** — https://www.klippa.com/en/ocr/financial-documents/receipts/,
    https://www.mindee.com/product/receipt-ocr-api, https://www.veryfi.com/receipt-ocr-api/ — tutti dichiarano
    conformità al regolamento europeo sui dati personali; l'ospitalità dei dati in Unione Europea è dichiarata da
    Klippa, per gli altri va verificata contrattualmente. Base della sezione §6 sul fornitore esterno.
11. **Panorama italiano dei gestionali contabili e degli studi** — https://www.teamsystem.com/commerce/integrazione-gestionale/integrazione-fatture-in-cloud/
    e https://www.fattureincloud.it/pmi/ — TeamSystem e Fatture in Cloud sono i due poli verso cui il cliente
    vorrà esportare; la scelta segue il suo commercialista.
12. **Soldo — piani e canoni in Italia** — https://www.soldo.com/it-it/prezzo/ — canone per azienda (21 €/mese
    *Standard*, 33 €/mese *Plus*, IVA esclusa), nessun piano gratuito, prova di 30 giorni. Unico caso rilevato di
    prezzo **non** per utente, ma è un prodotto di carte di pagamento.

### 2.7 Cosa NON sono riuscito a determinare

- **Prezzi di Soldo letti sulla pagina ufficiale.** La pagina https://www.soldo.com/it-it/prezzi/ risponde «non
  trovata» e i valori riportati (21 € e 33 €) provengono da una scheda di sintesi che cita il listino
  https://www.soldo.com/it-it/prezzo/. Vanno riverificati sulla pagina viva prima di usarli per posizionarsi.
- **Durata della prova gratuita di Rydoo**: la pagina dei prezzi non la dichiara. Non l'ho inventata.
- **Prezzi ufficiali di Expensify e Concur**: rilevati solo su siti di comparazione. Il catalogo stesso avverte che
  questi invecchiano male (§8): da riverificare sulle pagine dei fornitori prima di fissare il posizionamento.
- **Concorrenti italiani puri di nota spese** (per esempio prodotti nati attorno alla dematerializzazione dei
  giustificativi): ne ho visti citati nei redazionali, ma **senza pagine di prezzo pubbliche**. Non ho quindi una
  fascia italiana rilevata: la proposta del §5 nasce dalle fasce internazionali e dal catalogo, non da un confronto
  diretto.
- **Regole di tracciabilità, diarie e rimborsi chilometrici fuori dall'Italia.** Ho verificato solo l'Italia. Per
  Francia, Spagna e Germania non ho fatto ricerca: le storie sono scritte in modo che gli importi e le soglie
  siano **configurabili per account**, non incisi nel codice, ma la validazione per giurisdizione resta aperta
  (punto aperto n. 3).
- **Costo reale per documento dei fornitori di lettura automatica.** Nessuno dei tre esaminati pubblica un listino
  a documento leggibile senza contatto commerciale. È il dato che manca per calcolare il margine del piano base
  (punto aperto n. 2).

---

## 3. Varco d'identità — le risposte pronte per `new-application`

> Queste sei righe sono ciò che la skill `new-application` chiede **prima** di generare qualunque cosa. L'identificativo
> dell'app finisce nel nome dello schema del database, nei nomi delle code, nella rotta pubblica e nell'istanza
> del modulo di infrastruttura: cambiarlo dopo **non è una rinomina, è una migrazione di dati**.

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `notespese` | Rispetta `^[a-z][a-z0-9_]{0,30}$` (9 caratteri, minuscolo, solo lettere). Dice **cosa l'app è** — la nota spese — e non come è commercializzata: se domani il nome commerciale cambiasse, l'identificativo resterebbe giusto. È in italiano come `fatture`, l'app numero 1 già esistente nel repository, quindi in linea con la convenzione di casa. **Risolve alla radice la collisione con l'app 39 «SpendGrove SaaS»**, che tratta gli abbonamenti software e prenderà un identificativo del suo dominio (per esempio `saas_spend`): due schemi diversi, due rotte diverse, nessuna ambiguità. Scartato `spendgrove` proprio perché sarebbe l'identificativo che entrambe le app rivendicherebbero. |
| **Modello utente** | `multi` | Una nota spese ha per definizione almeno due ruoli: **chi la sostiene** (il collaboratore che anticipa i soldi) e **chi la approva** (il titolare o l'amministrazione). Senza più utenti per account non esiste il concetto di «chi ha fatto cosa», che qui non è un dettaglio ma il cuore: un'approvazione senza un approvatore identificato non vale niente. Anche il professionista solo, che approva le proprie spese, resta coerente: è un account `multi` con un membro. |
| **Porta locale** | `8108` | Convenzione del kit: 8100 + numero di catalogo (08). Da confermare con `./dev.sh services` al momento dello scaffolding. |
| **Metrica di quota** | `receipts` — «ricevute elaborate» | La **sola** cosa che il piano limita è il numero di documenti di spesa lavorati nel mese (letti automaticamente **o** inseriti a mano: conta il documento, non il modo). Tre ragioni. **(a)** È ciò che cresce col valore ricevuto: un'azienda che digitalizza duecento ricevute al mese ricava dall'app dieci volte quello che ne ricava chi ne fa venti. **(b)** È dove sta il **costo variabile** vero — la lettura automatica si paga a documento, i posti no. **(c)** È l'unico modo di non punire il comportamento che vogliamo: se limitassimo i **posti**, il titolare escluderebbe il collaboratore occasionale che fa tre scontrini l'anno, e quei tre scontrini tornerebbero nel portafoglio. Zoho Expense conferma indirettamente la scelta: limita le scansioni oltre agli utenti (§2.2). ⚠️ Si discosta dall'unità di misura prevalente del mercato (per utente): è una scelta di posizionamento, quindi rientra nella fermata di escalation del §5. |
| **Natura della metrica** | `flow` | Consumo su una finestra che si azzera: «**200 ricevute elaborate al mese**» — a marzo se ne possono elaborare altre 200 comunque sia andato febbraio, perché la spesa aziendale è un flusso mensile e non un magazzino. Contarla come giacenza sarebbe l'errore costoso descritto dai principi di piattaforma: l'archivio storico delle ricevute cresce per sempre, e un tetto sull'esistente bloccherebbe il cliente al dodicesimo mese per sempre. Il **conteggio scatta alla conferma umana** della spesa, non al caricamento della foto: così una foto illeggibile ricaricata due volte non costa due unità (storia `0004`). |
| **Colore-categoria e icona** | `amber` · icona `receipt` (ricevuta) | Le app della catena attiva del catalogo — preventivo, fattura, incasso (6 → 2 → 1 → 3) — occupano naturalmente i colori «positivi» del denaro che entra. SpendGrove sta sull'altro lato: **il denaro che esce e va verificato**. L'ambra è il colore dell'attenzione e della verifica, coerente con un'app il cui gesto centrale è «controlla prima di approvare». Deve valere sia nel listino (`category: amber`) sia nel modulo frontend (`accentToken`). |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Collaboratore` | La persona che sostiene le spese: dipendente o collaboratore dell'azienda cliente. Può essere legata a un membro dell'account (`user_id`) oppure esistere senza accesso all'app | nome e cognome, riferimento al membro dell'account (facoltativo), ruolo di spesa (`sostiene` / `approva` / `amministra`), centro di costo predefinito, Comune sede di lavoro, stato attivo/cessato | **sì** — anagrafica di un lavoratore. Sezione §6 |
| `Ricevuta` | Il giustificativo: la foto o il file caricato, con i suoi metadati tecnici | riferimento all'oggetto archiviato, impronta del contenuto, tipo di file, dimensione, data di acquisizione, esito della lettura | **sì, in modo indiretto ma pieno** — l'immagine può contenere qualsiasi cosa (§6, avviso) |
| `EsitoLettura` | Che cosa la lettura automatica ha capito della ricevuta, campo per campo, con la **fiducia** dichiarata e il fornitore che l'ha prodotta | valori estratti, fiducia per campo (0–100), fornitore e versione del modello, tempo di risposta, stato (riuscita / parziale / fallita) | sì, riflette il contenuto della ricevuta |
| `Spesa` | La riga di spesa vera e propria, come l'azienda la riconosce | data, esercente, imponibile, imposta, totale, valuta, categoria, mezzo di pagamento, tracciabile sì/no, tipo di documento, dentro o fuori Comune, stato, chi l'ha sostenuta, se e come è stata corretta rispetto alla lettura | sì — è la spesa di una persona identificata |
| `Categoria` | La classificazione della spesa (vitto, alloggio, trasporto, carburante, materiali, rappresentanza…) | nome, codice per la contabilità, regole predefinite sull'imposta, massimale facoltativo | no |
| `VoceImposta` | La qualificazione ai fini dell'imposta sul valore aggiunto di una spesa | aliquota, imposta detraibile, imposta indetraibile, motivo dell'indetraibilità | no |
| `Trasferta` | Il contenitore di più spese legate a una missione fuori sede | destinazione, date di inizio e fine, dentro o fuori Comune sede di lavoro, regime scelto (analitico, forfettario, misto) | sì, in modo indiretto: dice dove è stata una persona in una data |
| `PercorrenzaVeicolo` | Il rimborso chilometrico: un tragitto con la sua tariffa | data, partenza e arrivo dichiarati, chilometri, veicolo e alimentazione, tariffa applicata per chilometro, importo calcolato | sì, e in modo delicato (§6) |
| `NotaSpese` | Il fascicolo periodico che raggruppa le spese di un collaboratore e passa per l'approvazione | periodo, collaboratore, stato, totale, chi ha inviato e quando, chi ha approvato o respinto e perché | sì |
| `Rimborso` | La liquidazione registrata di una nota spese approvata | importo, data, riferimento del pagamento eseguito altrove, chi l'ha registrata | sì |
| `MovimentoCarta` | Un movimento importato da una carta aziendale o da un conto, da abbinare a una spesa | data, importo, descrizione dell'esercente, ultime quattro cifre della carta, stato dell'abbinamento | sì — dice quanto e dove ha speso una persona |
| `PacchettoEsportazione` | Il pacchetto consegnato al commercialista per un periodo: tabella dei movimenti più allegati, congelato | periodo, data di produzione, impronta, elenco dei documenti inclusi, chi l'ha prodotto | sì, nel contenuto |
| `PoliticaDiSpesa` | I massimali e le regole che l'azienda si dà | categoria, massimale per riga o per giorno, obbligo di ricevuta sopra soglia, comportamento allo sforamento (avviso o blocco) | no |

**Relazioni.** `Collaboratore` **1→N** `Spesa`; `Ricevuta` **1→1** `EsitoLettura` e **1→0..1** `Spesa` (una ricevuta
può restare senza spesa se illeggibile o scartata); `Spesa` **N→0..1** `Trasferta`, **N→1** `Categoria`, **1→0..N**
`VoceImposta`; `NotaSpese` **1→N** `Spesa`; `NotaSpese` **1→0..1** `Rimborso`; `MovimentoCarta` **0..1→0..1** `Spesa`
(abbinamento reversibile finché la nota non è approvata); `PacchettoEsportazione` **1→N** `NotaSpese`.

**Macchina a stati della `Spesa`** — è la parte che tutte le storie devono rispettare:

```
caricata → letta → da_rivedere → confermata → in_nota → approvata → rimborsata
                        │                        │           │
                        └→ scartata              └→ respinta ─┘ (torna a confermata)
```

- `caricata`: c'è il file, la lettura non è ancora partita;
- `letta`: la lettura automatica ha prodotto un esito (anche parziale o fallito);
- `da_rivedere`: attende la persona. **Nessuna spesa passa oltre senza un essere umano che confermi** (§6);
- `confermata`: la persona ha validato o corretto i dati; **è qui che si consuma una unità di quota**;
- `in_nota`, `approvata`, `respinta`, `rimborsata`: il ciclo del fascicolo;
- `scartata`: doppione, documento illeggibile, spesa non di lavoro.

**Macchina a stati della `NotaSpese`**: `bozza → inviata → approvata | respinta → rimborsata`. Una nota `approvata`
non si modifica più: si corregge emettendo una nota di rettifica (storia `0015`).

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica (`deleted_at`);
schema `app_notespese`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8).

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il
> file `services/core/src/main/resources/pricing/notespese.yaml`.

**Ragionamento.** Tre numeri fanno da àncora: la fascia rilevata sui concorrenti europei (8–12 €/utente/mese,
minimo 5 utenti, cioè **40–60 € al mese** per la più piccola azienda che possa comprarli); la fascia indicata dal
catalogo (4–9 €/utente/mese oppure 19 €/mese piatti per micro-squadra); e il fatto che il nostro cliente tipo ha
**tre persone e trenta ricevute al mese**, per le quali quaranta euro al mese sono fuori discussione. Vendendo a
**ricevute elaborate** anziché a posti, un'azienda di tre persone paga per quello che usa e può dare accesso a
tutti i collaboratori senza pagare posti fermi — che è esattamente il comportamento che fa entrare le ricevute
nell'app invece di lasciarle nel portafoglio. Il piano gratuito è tarato per essere **abbastanza per capire se
funziona** (un mese di spese di una persona sola) e non abbastanza per viverci.

| Piano | Prezzo mensile | Prezzo annuale | Limite sulla metrica `receipts` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | 20 ricevute al mese | — | Chi vuole provarci sul serio per un mese: una persona, poche spese. Stesso ordine di grandezza del gratuito di Zoho Expense (20 scansioni per utente) |
| `pro` | 9 € | 90 € (= 10× il mensile, «due mesi in regalo») | 150 ricevute al mese | 14 giorni | La micro-impresa: titolare più due o tre collaboratori che si muovono. Utenti non contati |
| `team` | 19 € | 190 € | 600 ricevute al mese | 14 giorni | La piccola impresa con più persone in trasferta, centri di costo e un secondo livello di approvazione |

**Note obbligate.**

- Tre piani, come raccomandato: aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile.
- I limiti sono **espliciti su tutti e tre**: nessun campo lasciato vuoto, perché un limite vuoto significa
  *illimitato* e non *zero* — le due letture distano un refuso.
- **Prova gratuita e piano gratuito insieme**: qui la prova ha senso lo stesso, perché il gratuito (20 ricevute) non
  fa vedere le funzioni che giustificano il prezzo — approvazione, riconciliazione delle carte, pacchetto per il
  commercialista. La prova di 14 giorni sul `pro` serve a mostrarle, non a dare più volume. Se si volesse
  semplificare, la si può togliere dal `team` e lasciarla solo sul `pro`.
- **Costo effettivo dell'incasso**: il piano `pro` a 9 €/mese è sopra la soglia dei ~5 € sotto la quale la parte
  fissa per transazione pesa troppo, quindi il segnale non scatta. Resta però il **costo variabile della lettura
  automatica**, che non sono riuscito a quantificare (§2.7): se il fornitore costasse più di qualche centesimo a
  documento, un piano `pro` da 150 ricevute a 9 € avrebbe un margine da verificare. **È il numero da procurarsi
  prima di confermare questo listino.**
- I prezzi sono **immutabili una volta vivi**: un cambio si fa creando un prezzo nuovo e archiviando il vecchio.
- **Alternativa non scartata**: listino per posti (`seats`, natura `stock`), allineato al mercato e più facile da
  spiegare a chi confronta i prodotti. Costa meno spiegazioni in vendita e più rischio sul margine. La scelta fra
  le due è una decisione di prodotto, non di un agente: punto aperto n. 1.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/notespese.yaml`) si compila **insieme** allo sviluppatore: «niente contratto, niente
> produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

> 🛑 **Prima avvertenza, che cambia tutta la valutazione: qui i dati sono di lavoratori, non di clienti.**
> In tutte le altre app della suite l'azienda cliente tratta dati dei **suoi** clienti — persone con cui ha un
> rapporto paritario e volontario. Qui tratta dati dei **suoi dipendenti e collaboratori**, cioè persone in una
> posizione di squilibrio rispetto a chi decide. Tre conseguenze pratiche, tutte da portare allo sviluppatore:
> 1. **il consenso non è una base giuridica utilizzabile** nel rapporto di lavoro, se non in casi marginali: non è
>    liberamente prestato quando chi lo chiede è il datore di lavoro. La base sta altrove — esecuzione del contratto
>    di lavoro e obblighi fiscali e contabili di legge;
> 2. **i dati di spesa raccontano la vita della persona**: dove è stata, a che ora, con chi ha pranzato, che
>    farmacia ha visitato. La minimizzazione non è un adempimento formale, è la sostanza;
> 3. **c'è un confine con il controllo a distanza dei lavoratori** (in Italia l'art. 4 dello Statuto dei
>    lavoratori, L. 300/1970): uno strumento che serve a lavorare non richiede accordo sindacale o autorizzazione,
>    ma **uno strumento che consente di ricostruire spostamenti e comportamenti sì**. È il motivo per cui l'app non
>    deve leggere né conservare la posizione geografica delle foto e non deve produrre cruscotti individuali di
>    comportamento. Vedi punto aperto n. 6.

> 🛑 **Seconda avvertenza — categorie particolari (articolo 9): non per progetto, ma per ingresso incidentale.**
> SpendGrove **non chiede** e **non ha campi** per dati sanitari, biometrici, genetici, opinioni politiche,
> convinzioni religiose, orientamento sessuale o appartenenza sindacale. Però il gesto centrale dell'app è
> *fotografare un documento qualunque*, e alcuni documenti qualunque rivelano proprio quelle cose: la ricevuta di
> una farmacia o di una visita medica (salute), la quota associativa a un sindacato (appartenenza sindacale), la
> ricevuta di un luogo di culto (convinzioni religiose). Insieme all'immagine entra tutto ciò che l'immagine
> contiene, che nessun campo strutturato dichiara.
> Non si può risolvere fingendo che non accada. Mitigazioni proponibili, tutte da validare:
> **(a)** l'app dichiara nell'informativa che le ricevute vanno caricate solo se sono spese aziendali;
> **(b)** la revisione umana permette di **scartare** una ricevuta e cancellarne subito il file (storia `0011`);
> **(c)** i campi liberi portano l'avviso di non inserire dati sensibili; **(d)** nessuna analisi del contenuto
> dell'immagine oltre l'estrazione dei campi dichiarati; **(e)** cancellazione fisica del file quando la spesa è
> scartata, senza attendere la fine della conservazione.
> **Questa è una valutazione da fare con lo sviluppatore**, e con un legale se la risposta è che il rischio va
> presidiato: potrebbe servire una valutazione d'impatto. **Non la chiudo io.**

**Categorie trattate**

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `collaboratore.nome` | `collaboratore.nome`, `collaboratore.cognome` | dipendente o collaboratore dell'azienda cliente | anagrafica | attribuire la spesa a chi l'ha sostenuta e a chi va rimborsata | esecuzione del contratto di lavoro; obbligo di legge (documentazione contabile) | 10 anni dalla chiusura dell'esercizio a cui la spesa si riferisce (conservazione delle scritture) |
| `collaboratore.sede` | `collaboratore.comune_sede_lavoro` | lavoratore | dato del rapporto di lavoro | stabilire se una spesa è dentro o fuori il Comune sede di lavoro, da cui dipende il regime fiscale | obbligo di legge (art. 51 c. 5 del Testo unico delle imposte sui redditi) | come sopra |
| `spesa.esercente` | `spesa.esercente`, `spesa.data`, `spesa.totale` | il lavoratore (indirettamente: dice dove e quando ha speso) | economico e comportamentale | documentare il costo e il rimborso | esecuzione del contratto; obbligo di legge | 10 anni |
| `ricevuta.file` | archivio degli oggetti (regione europea) | il lavoratore, e chiunque compaia sul documento | immagine di un documento fiscale: contenuto non strutturato | prova del costo verso l'amministrazione finanziaria | obbligo di legge (conservazione dei giustificativi) | 10 anni, oppure **cancellazione immediata** se la spesa viene scartata |
| `percorrenza.tragitto` | `percorrenza.partenza`, `percorrenza.arrivo`, `percorrenza.km` | lavoratore | dato di spostamento **dichiarato dall'interessato** | calcolare il rimborso chilometrico | esecuzione del contratto; obbligo di legge | 10 anni |
| `trasferta.destinazione` | `trasferta.destinazione`, date | lavoratore | dato di spostamento | inquadrare il regime fiscale della trasferta | obbligo di legge | 10 anni |
| `movimento_carta.*` | `movimento_carta` | titolare della carta aziendale (un lavoratore) | economico | abbinare il movimento alla ricevuta | esecuzione del contratto; legittimo interesse dell'azienda al controllo delle proprie carte | 10 anni |
| `nota_spese.approvatore` | `nota_spese.approvato_da`, `respinto_da`, `motivo` | lavoratore che approva | dato di attività lavorativa | tracciare chi ha autorizzato la spesa | esecuzione del contratto; obbligo di legge | 10 anni |
| `rimborso.*` | `rimborso` | lavoratore rimborsato | economico | provare l'avvenuto rimborso | obbligo di legge | 10 anni |

Nessuna voce include **coordinate bancarie** del collaboratore: il pagamento avviene fuori dall'app e l'app registra
solo un riferimento libero. È una scelta di minimizzazione deliberata, e va tenuta (§7 «Fuori ambito» delle storie
`0016`). Se un giorno servisse l'esportazione dei bonifici, sarebbe una categoria nuova e una decisione nuova.

**Esportazione e cancellazione.** Le tabelle che contengono dati di persone e che **devono comparire sia in
`exportData` sia in `purgeData`** del contratto `NoteSpeseDataContract`:
`collaboratore`, `ricevuta` (compreso il file nell'archivio degli oggetti), `esito_lettura`, `spesa`, `trasferta`,
`percorrenza_veicolo`, `nota_spese`, `rimborso`, `movimento_carta`, `pacchetto_esportazione`.
Dimenticarne una è il difetto di conformità più probabile: qui il candidato più facile da dimenticare è
**l'oggetto archiviato della ricevuta**, che non è una riga di tabella e quindi sfugge a chi guarda solo lo schema.
La cancellazione è **fisica**; sostituire il nome del collaboratore con un codice **non è cancellare**.

⚠️ **Conflitto da portare allo sviluppatore**: la richiesta di cancellazione di un ex collaboratore si scontra con
l'obbligo di conservare dieci anni i giustificativi contabili. Sono due doveri veri e non li concilio io: la strada
plausibile è che i dati sotto obbligo di legge restino fino alla scadenza e che si cancelli tutto il resto, ma la
formulazione precisa spetta a chi risponde della conformità (punto aperto n. 7).

**Testo libero.** Sì, l'app ne ha: la descrizione della spesa, il motivo del rifiuto di una nota, le note di
trasferta. Sono un ingresso non presidiato per categorie particolari («visita specialistica del 12/3»). L'app
**non fa rilevazione di contenuto**; il presidio, se servirà, è un tema trasversale di piattaforma. Nel frattempo i
campi portano l'avviso e restano fuori dai registri degli eventi.

**Integrazioni esterne.** Ognuna di queste è un potenziale **responsabile esterno del trattamento**, da mettere
nell'elenco dei fornitori e nell'informativa:

| Integrazione | Riceve dati personali? | Nota |
|---|---|---|
| **Fornitore di lettura automatica della ricevuta** (Klippa, Mindee, Veryfi o equivalente) | **sì, e i più delicati**: riceve l'immagine intera del documento, cioè anche ciò che non abbiamo chiesto | 🛑 È **il** punto della sezione. Serve un contratto di responsabile del trattamento, il **trattamento in Unione Europea** (Klippa lo dichiara; per gli altri va verificato), il divieto contrattuale di riuso per addestramento, la cancellazione dopo l'elaborazione. La storia `0007` impone che il fornitore stia **dietro un'interfaccia interna**, sostituibile senza toccare il resto: è anche la strada per valutare un'elaborazione in casa, che eliminerebbe del tutto il responsabile esterno. Vedi punto aperto n. 2 |
| Aggregatore di conti e carte (direttiva europea sui servizi di pagamento) | sì, movimenti riferibili a persone | Fuori perimetro nel primo giro: si importa un file. Se un giorno entra, è un fornitore nuovo |
| Collegamento diretto al gestionale contabile (TeamSystem, Fatture in Cloud) | sì | Fuori perimetro: si esporta un file. Il file lo consegna il cliente, quindi il destinatario è un suo fornitore, non il nostro |
| Archivio degli oggetti per le immagini | sì | Interno alla piattaforma, **regione europea**, come tutti i dati a riposo |

**Classificazione della change.** Una app nuova che introduce **il trattamento di dati di lavoratori** — categoria di
interessati che la piattaforma finora non trattava — e una **nuova finalità** (documentazione delle spese e dei
rimborsi), con un **responsabile esterno nuovo** per la lettura automatica: è un cambiamento **sostanziale**, senza
sconti. Va aggiornato il registro dei trattamenti e va rivista l'informativa.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `elenca_spese` | `(periodo?, stato?, collaboratore?, categoria?) → elenco di spese minimizzato` | Restituisce codice, data, esercente, totale, stato. Niente immagini, niente note libere | lettura | no |
| `riepilogo_spese` | `(periodo, raggruppamento: categoria \| collaboratore \| centro_di_costo) → totali` | I numeri del cruscotto, in forma di tabella | lettura | no |
| `elenca_da_rivedere` | `(collaboratore?) → elenco delle spese in attesa di revisione, con i campi a bassa fiducia` | «Cosa devo controllare oggi» | lettura | no |
| `verifica_deducibilita` | `(id_spesa \| periodo) → elenco dei rischi rilevati` | Dice quali spese rischiano l'indeducibilità (pagamento non tracciabile, documento sbagliato, ricevuta mancante) | lettura | no |
| `leggi_ricevuta` | `(riferimento del file caricato) → bozza di spesa con la fiducia per campo` | Lancia la lettura automatica e propone i valori. **Non crea nulla di definitivo** | scrittura (produce bozza) | **sì** |
| `crea_spesa` | `(data, esercente, totale, categoria, mezzo di pagamento) → bozza di spesa` | Inserimento manuale, per la spesa senza ricevuta | scrittura | **sì** |
| `categorizza_spesa` | `(id_spesa, categoria) → spesa aggiornata` | Cambia la categoria. Reversibile finché la spesa non è in una nota approvata | scrittura reversibile | **sì**, leggera |
| `crea_nota_spese` | `(collaboratore, periodo, elenco di spese) → bozza di nota spese` | Compone il fascicolo | scrittura | **sì** |
| `invia_nota_spese` | `(id_nota) → esito dell'invio` | Esce dalla sfera del collaboratore e chiama in causa l'approvatore: non si annulla | scrittura irreversibile | **sì, obbligatoria** |
| `esporta_per_contabilita` | `(periodo) → pacchetto congelato` | Chiude il periodo e produce il pacchetto per il commercialista | scrittura irreversibile | **sì, obbligatoria** |

**Due divieti espliciti, che valgono come requisito.**

1. **L'approvazione di una nota spese non è uno strumento di scrittura eseguibile dall'assistente.** L'assistente
   può preparare («ecco le tre note pronte, questa ha una spesa fuori massimale»), ma l'atto di approvare è un
   **atto di una persona verso un'altra persona** e resta un gesto compiuto nell'interfaccia, con l'identità
   dell'approvatore. Un'approvazione automatica non è un'automazione: è la scomparsa del controllo che l'intero
   flusso esiste per garantire.
2. **La conferma dei dati letti dalla ricevuta non è delegabile all'assistente.** `leggi_ricevuta` produce sempre
   una bozza in stato `da_rivedere`: se l'assistente potesse confermarsi da solo la propria estrazione, la revisione
   umana sarebbe una finzione e il numero che finisce in contabilità sarebbe un'ipotesi presentata come fatto.

**Perché il livello conversazionale rende questa app più utile delle concorrenti.** Le domande vere che un titolare
si fa su questo dominio sono domande in lingua naturale, non filtri di tabella: «quanto abbiamo speso in trasferte a
luglio?», «di chi sono le note ancora da approvare?», «quali spese di giugno rischiano di non essere deducibili?».
`riepilogo_spese` e `verifica_deducibilita` rispondono in una frase a ciò che oggi richiede un foglio di calcolo e
una telefonata al commercialista.

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine di questa epica l'app esiste, è accesa, è vuota e si avvia in locale: schema, rotte, modulo nel
backoffice, abbonamento con la sua quota, dati di prova.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio `notespese` | Istanza di scaffolding, rotte `/api/notespese/v1/*`, infrastruttura dal modulo comune |
| [0002](01-fondamenta/0002-modello-dati-delle-spese.md) | Modello dati delle spese, per account | Schema `app_notespese` con le tabelle di base, `tenant_id` e cancellazione logica |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo nel backoffice | Manifesto, registrazione, sezioni, cinque lingue, colore-categoria |
| [0004](01-fondamenta/0004-abbonamento-e-quota-delle-ricevute.md) | Abbonamento e quota delle ricevute | Catena dei varchi e conteggio della metrica `receipts` alla conferma della spesa |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | `./dev.sh services` mostra l'app; un account di prova con spese inventate |

### Epica 02 — Cattura e lettura della ricevuta

Il cuore dell'app: dalla foto al dato verificato. Nessun dato letto automaticamente arriva in contabilità senza che
una persona l'abbia guardato.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-cattura-e-lettura-della-ricevuta/0006-caricamento-della-ricevuta.md) | Caricamento della ricevuta | Foto o file nell'archivio europeo, impronta, limiti di formato e dimensione |
| [0007](02-cattura-e-lettura-della-ricevuta/0007-lettura-automatica-dei-dati.md) | Lettura automatica dei dati | Estrazione dietro un'interfaccia interna, con **fiducia dichiarata campo per campo** |
| [0008](02-cattura-e-lettura-della-ricevuta/0008-revisione-e-correzione-dei-dati-letti.md) | Revisione e correzione dei dati letti | La schermata che affianca immagine e campi; senza conferma umana non si passa |
| [0009](02-cattura-e-lettura-della-ricevuta/0009-inserimento-manuale-della-spesa.md) | Inserimento manuale della spesa | La spesa senza ricevuta, o con ricevuta illeggibile, dichiarata come tale |
| [0010](02-cattura-e-lettura-della-ricevuta/0010-categorizzazione-della-spesa.md) | Categorizzazione della spesa | Categorie dell'account, proposta automatica, regole per esercente ricorrente |
| [0011](02-cattura-e-lettura-della-ricevuta/0011-doppioni-e-ricevute-scartate.md) | Doppioni e ricevute scartate | Riconoscimento del doppione e scarto con cancellazione immediata del file |

### Epica 03 — Note spese e approvazione

Dalla spesa confermata al rimborso registrato, passando per chi deve dire di sì.

| # | Storia | In una riga |
|---|---|---|
| [0012](03-note-spese-e-approvazione/0012-anagrafica-dei-collaboratori.md) | Anagrafica dei collaboratori | Chi sostiene le spese, con o senza accesso all'app; ruoli di spesa |
| [0013](03-note-spese-e-approvazione/0013-composizione-della-nota-spese.md) | Composizione della nota spese | Raggruppare le spese confermate di un periodo in un fascicolo in bozza |
| [0014](03-note-spese-e-approvazione/0014-invio-della-nota-spese.md) | Invio della nota spese | Il passaggio irreversibile dalla bozza alla richiesta di approvazione |
| [0015](03-note-spese-e-approvazione/0015-approvazione-e-rifiuto.md) | Approvazione e rifiuto | L'atto dell'approvatore, con motivo obbligatorio quando respinge |
| [0016](03-note-spese-e-approvazione/0016-registrazione-del-rimborso.md) | Registrazione del rimborso | L'app registra che il rimborso è stato pagato altrove: non paga |
| [0017](03-note-spese-e-approvazione/0017-politiche-di-spesa-e-massimali.md) | Politiche di spesa e massimali | Massimali per categoria, avviso o blocco allo sforamento |

### Epica 04 — Trasferte e rimborsi chilometrici

Le regole che fanno la differenza fra un rimborso deducibile e uno che costa due volte.

| # | Storia | In una riga |
|---|---|---|
| [0018](04-trasferte-e-rimborsi-chilometrici/0018-trasferta-come-contenitore-di-spese.md) | Trasferta come contenitore di spese | Destinazione, date, dentro o fuori Comune sede di lavoro |
| [0019](04-trasferte-e-rimborsi-chilometrici/0019-rimborso-chilometrico.md) | Rimborso chilometrico | Tragitto e chilometri con tariffe **caricate dall'account**, non incise nel prodotto |
| [0020](04-trasferte-e-rimborsi-chilometrici/0020-verifica-della-tracciabilita-del-pagamento.md) | Verifica della tracciabilità del pagamento | Segnala prima dell'approvazione le spese che rischiano l'indeducibilità |
| [0021](04-trasferte-e-rimborsi-chilometrici/0021-diaria-forfettaria-e-regime-della-trasferta.md) | Diaria forfettaria e regime della trasferta | Analitico, forfettario o misto: importi configurabili per account |

### Epica 05 — Riconciliazione e uscita verso la contabilità

Chiudere il cerchio: il movimento della carta, l'imposta, il pacchetto che il commercialista accetta.

| # | Storia | In una riga |
|---|---|---|
| [0022](05-riconciliazione-e-uscita-verso-la-contabilita/0022-importazione-dei-movimenti-di-carta.md) | Importazione dei movimenti di carta | File di movimenti caricato a mano; nessun collegamento bancario |
| [0023](05-riconciliazione-e-uscita-verso-la-contabilita/0023-abbinamento-fra-movimento-e-spesa.md) | Abbinamento fra movimento e spesa | Proposta di abbinamento, conferma umana, movimenti orfani in evidenza |
| [0024](05-riconciliazione-e-uscita-verso-la-contabilita/0024-qualificazione-dell-imposta.md) | Qualificazione dell'imposta | Tipo di documento, imposta detraibile o no, motivo dell'indetraibilità |
| [0025](05-riconciliazione-e-uscita-verso-la-contabilita/0025-pacchetto-per-il-commercialista.md) | Pacchetto per il commercialista | Tabella dei movimenti più allegati, congelato e con impronta |
| [0026](05-riconciliazione-e-uscita-verso-la-contabilita/0026-preparazione-alla-conservazione-a-norma.md) | Preparazione alla conservazione a norma | Che cosa l'app fa e, soprattutto, che cosa **non** fa: non è un conservatore |
| [0027](05-riconciliazione-e-uscita-verso-la-contabilita/0027-cruscotto-della-spesa.md) | Cruscotto della spesa | Totali per periodo, categoria, collaboratore e centro di costo |

### Epica 06 — Esposizione conversazionale e prove end-to-end

L'epica di chiusura: il contratto degli strumenti per la chat, gli obblighi di piattaforma sui dati delle persone e
la prova che il percorso funziona davvero dall'inizio alla fine.

| # | Storia | In una riga |
|---|---|---|
| [0028](06-esposizione-conversazionale-e-prove/0028-contratto-degli-strumenti-di-lettura.md) | Contratto degli strumenti di lettura | I quattro strumenti che leggono, con dati minimizzati |
| [0029](06-esposizione-conversazionale-e-prove/0029-strumenti-di-scrittura-con-bozza-e-conferma.md) | Strumenti di scrittura con bozza e conferma | Sei strumenti che scrivono, tutti con conferma; due divieti espliciti |
| [0030](06-esposizione-conversazionale-e-prove/0030-contratto-dei-dati-dei-collaboratori.md) | Contratto dei dati dei collaboratori | Esportazione e cancellazione fisica, compreso il file della ricevuta |
| [0031](06-esposizione-conversazionale-e-prove/0031-percorso-end-to-end-dell-app.md) | Percorso end-to-end dell'app | `[J-NOTESPESE]` dalla foto al pacchetto, e registro di copertura aggiornato |

**Totale**: 6 epiche, 31 storie (`0001`–`0031`). Dentro la fascia raccomandata: 4-7 epiche, 4-8 storie per epica,
20-45 storie in tutto.

---

## 9. Estensioni della console di amministrazione

Servono tre cose oltre allo standard: una **vista sulla qualità e sullo stato del fornitore di lettura automatica**
(quanti documenti falliscono, quanto si aspetta, per account — solo conteggi, mai contenuti), una **deroga
temporanea sulla quota** per il primo mese di chi migra un arretrato di ricevute, e la possibilità di **ripetere una
lettura fallita** senza consumare quota. Nessuna di queste dà accesso ai contenuti dell'account.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| **39 — SpendGrove SaaS** | **omonimia da disambiguare, nessun rapporto di dominio** | Solo il nome commerciale. Identificativi tecnici distinti (`notespese` qui, altro là): vedi l'avvertenza in testa |
| 09 — PeopleGrove (HR lite) | condivide dati con | **Anagrafica dipendenti**, indicata dal catalogo (§6) come una delle entità centrali della suite. Se PeopleGrove esiste, l'anagrafica dei collaboratori dovrebbe venire da lì per eventi, non essere ridigitata |
| 10 — PayGrove (pagamenti a collaboratori) | alimenta | Il rimborso approvato è un importo da pagare: PayGrove lo paga, SpendGrove lo registra. Confine netto, da tenere |
| 11 — ShiftGrove (timbrature e turni) | condivide dati con | Anagrafica dipendenti; e le giornate di trasferta hanno un'ovvia parentela con le presenze |
| 45 — OnboardGrove | condivide dati con | Anagrafica dipendenti: chi entra ed esce dall'azienda entra ed esce anche da qui |
| 49 — ReconGrove (riconciliazione bancaria) | **si sovrappone a** | La riconciliazione dei movimenti. Qui se ne fa la fetta minima che serve alle note spese (movimento di carta ↔ ricevuta): la riconciliazione bancaria completa è di ReconGrove |
| 18 — VaultGrove (gestione documentale) | **si sovrappone a** | La conservazione dei documenti. Qui si prepara il pacchetto; il servizio di conservazione è di VaultGrove o di un fornitore terzo |
| 61 — ExtractGrove (estrazione dati da documenti) | **si sovrappone a** | La lettura automatica del documento. Se ExtractGrove nascesse, la nostra interfaccia interna di estrazione (storia `0007`) sarebbe il punto di innesto naturale — un'altra ragione per tenerla astratta |
| 54 — BudgetGrove (budget e previsioni) | alimenta | I consuntivi di spesa per centro di costo sono un ingresso naturale del budget |
| 48 — ProcureGrove (acquisti e ordini) | condivide dati con | Confine: qui la spesa **anticipata da una persona**, là l'acquisto **ordinato dall'azienda** a un fornitore |

**Lettura.** SpendGrove ha senso **anche da sola**: è uno dei pochi casi del catalogo in cui il cliente ha un dolore
autonomo (la pila di scontrini) che non richiede nessun'altra app per essere tolto. Dentro la suite guadagna
sull'anagrafica dipendenti condivisa e sul passaggio a valle verso i pagamenti e il budget. Il catalogo (§6) indica
l'anagrafica dipendenti fra le entità centrali della suite: questa app la tocca e non dovrebbe possederla da sola.

**Sovrapposizioni da evitare.** Tre confini da scrivere adesso e non dopo: la riconciliazione bancaria è di
ReconGrove (49), la conservazione a norma è di VaultGrove (18) o di un terzo, l'estrazione documentale generica è di
ExtractGrove (61). Qui dentro ne restano solo le fette indispensabili al ciclo della nota spese — e vanno tenute
piccole apposta.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **Metrica di quota: ricevute o posti?** La proposta (`receipts`, natura `flow`) si discosta dallo standard di mercato (per utente) | È una decisione di posizionamento commerciale, non tecnica: rende il prezzo più giusto per il micro-cliente e più difficile da confrontare con i concorrenti | sviluppatore (fermata di escalation sul listino, §5) |
| 2 | **Fornitore della lettura automatica: quale, a che costo, con quale trattamento dei dati** | Non ho trovato listini a documento pubblici; e la scelta è insieme economica (margine del piano base) e di conformità (responsabile esterno che riceve immagini di documenti) | sviluppatore, con verifica contrattuale; storia `0007` prepara il terreno tenendolo dietro un'interfaccia |
| 3 | **Regole fiscali fuori dall'Italia** (tracciabilità, diarie, tariffe chilometriche) | Ho verificato solo l'Italia. Le storie tengono gli importi configurabili per account, ma nessuno ha validato Francia, Spagna e Germania | sviluppatore + consulente per giurisdizione; incide sulle storie `0019`, `0020`, `0021` |
| 4 | **Conservazione a norma: fino a dove arriva l'app** | Buttare la carta è l'aspettativa numero uno del cliente (§2.5) e richiede un conservatore conforme ai requisiti vigilati da AgID, che non siamo | sviluppatore; storia `0026` dichiara il limite, non lo supera |
| 5 | **Ingresso delle ricevute via posta elettronica** | Molto richiesto e tecnicamente semplice, ma apre una superficie nuova (indirizzo dedicato per account, filtro dei mittenti, spazzatura) che non appartiene a nessuna storia di questo giro | sviluppatore; candidato naturale per il secondo giro |
| 6 | **Confine con il controllo a distanza dei lavoratori** | Un'app che sa dove e quando ha speso ogni collaboratore è a un passo dal diventare uno strumento di controllo. Serve dire in modo netto cosa non si fa (niente posizione geografica, niente cruscotti individuali di comportamento) e verificarlo con chi risponde della conformità | sviluppatore + verifica legale (§6) |
| 7 | **Cancellazione dei dati di un ex collaboratore contro l'obbligo decennale di conservazione** | Due doveri veri in conflitto; la formulazione corretta non spetta a un agente | sviluppatore + verifica legale; storia `0030` implementa ciò che verrà deciso |
| 8 | **Categorie particolari per ingresso incidentale** (ricevute di farmacia, quote sindacali) | È un rischio reale del gesto centrale dell'app, non un'ipotesi di scuola. Le mitigazioni proposte al §6 vanno validate, e potrebbe servire una valutazione d'impatto | sviluppatore + verifica legale |

**Rischi noti**

- **La lettura automatica sbaglia, e sbagliando fa entrare numeri finti in contabilità** — se la revisione umana
  diventasse saltabile «per comodità», l'app produrrebbe errori più velocemente di quanto li produca la ricopiatura
  a mano, con l'aggravante che sembrerebbero verificati. *Attenuazione*: lo stato `da_rivedere` è obbligatorio, la
  fiducia è mostrata campo per campo, e il registro conserva sia il valore letto sia quello corretto (storie `0007`,
  `0008`).
- **Il costo variabile della lettura mangia il margine del piano base** — 150 ricevute a 9 € lasciano pochi
  centesimi a documento. *Attenuazione*: procurarsi il listino del fornitore prima di confermare il prezzo (punto
  aperto n. 2); valutare l'elaborazione in casa; alzare il piano `pro` o abbassarne il tetto.
- **Il cliente si aspetta di buttare la carta e non può** — è la delusione più probabile. *Attenuazione*: dirlo in
  prodotto, nella landing e nella schermata di conservazione, invece di lasciarlo intuire (storia `0026`).
- **La concorrenza è forte e vende a per-utente** — un compratore che confronta i listini fatica a paragonare
  «9 € per 150 ricevute» con «8 € per utente, minimo 5». *Attenuazione*: mostrare il confronto in modo esplicito
  sulla landing («per un'azienda di tre persone, loro 40 € al mese, noi 9»).
- **L'anagrafica dipendenti viene costruita due volte** se PeopleGrove (09) nasce dopo. *Attenuazione*: tenere
  `Collaboratore` sottile e alimentabile per eventi fin da subito (storia `0012`).

**Fuori dimensionamento**: non applicabile. 6 epiche (fascia 4-7), da 4 a 6 storie per epica (fascia 4-8), 31 storie
in tutto (fascia 20-45).
