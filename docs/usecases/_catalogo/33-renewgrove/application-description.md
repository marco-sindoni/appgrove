# RenewGrove — descrizione dell'applicazione

**Numero di catalogo**: 33 · **Tipo**: orizzontale · relazione con il cliente (operazioni di ricavo) · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 33](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 0. 🛑 Da leggere prima di tutto — il confine con l'app 19 SubGrove

Questa applicazione **non si può progettare** senza avere prima risposto a una domanda: l'app **19 SubGrove**
(`abbonati`), già scritta in [19-subgrove/](../19-subgrove/application-description.md), tratta rinnovi, scadenze e
solleciti. La scheda 33 promette *«calendario rinnovi, health score, alert pre-disdetta, sequenze di recupero,
dunning sui pagamenti falliti, analisi dei motivi di cancellazione, forecast MRR»*. Sono in gran parte le stesse
parole. Ho confrontato riga per riga i sette casi d'uso della scheda 33 con le trentacinque storie di SubGrove.
Ecco l'esito, senza addolcirlo.

### 0.1 Che cosa la scheda 33 chiede e che SubGrove **fa già**

| Caso d'uso della scheda 33 | Dove sta già in SubGrove | Giudizio |
|---|---|---|
| Calendario e reminder dei rinnovi | storie `0012` calendario dei rinnovi, `0013` avviso di rinnovo con preavviso | **doppione pieno** |
| Dunning sui pagamenti falliti | storie `0018`-`0022`: registrazione dell'incasso, importazione degli esiti, catena dei solleciti, sospensione automatica | **doppione pieno** |
| Forecast dei ricavi ricorrenti | storie `0027`-`0030`: istantanea mensile, scomposizione, abbandono e durata media, previsione degli incassi | **doppione pieno** |
| Analisi dei motivi di cancellazione | attributo *motivo di cessazione* sull'abbonamento (`0011`) e scomposizione della variazione (`0028`) | **doppione parziale** — SubGrove sa *quanti* e *con che etichetta*, non *perché* nelle parole di chi se n'è andato |

Quattro casi d'uso su sette sono già scritti altrove. Se RenewGrove li ri-implementasse, il marketplace
avrebbe due prodotti che generano due calendari di scadenze sugli stessi contratti e due catene di solleciti sullo
stesso denaro: il modo più rapido per mandare due messaggi diversi allo stesso cliente lo stesso giorno — un
rischio che SubGrove ha già identificato e disinnescato nel proprio confine con **03 CashGrove**.

### 0.2 Che cosa resta, e perché è un prodotto e non un'epica

Restano tre casi d'uso, e non sono briciole: **punteggio di salute del rapporto**, **avviso sui clienti a rischio**,
**sequenze di recupero**. Hanno in comune una cosa che li rende diversi in natura, non in grado, da tutto ciò che
SubGrove fa:

> **SubGrove registra fatti già accaduti e ne trae la conseguenza contrattuale.** La rata non è rientrata → sollecito
> → sospensione. È una macchina a stati: deterministica, verificabile, e giusta per definizione perché il contratto
> dice così.
>
> **RenewGrove formula una previsione su una persona o su un'azienda e propone un'azione per cambiarne l'esito.**
> Questo cliente probabilmente non rinnoverà → ecco perché lo penso → ecco che cosa proporrei di fare → ed ecco, fra
> tre mesi, se è servito. Non è una macchina a stati: è un **giudizio**, e un giudizio va spiegato, va potuto
> contraddire e va misurato.

Sono due mestieri con obblighi diversi. Il secondo è **profilazione** ai sensi dell'articolo 4, punto 4 del
regolamento europeo sulla protezione dei dati, e — se il punteggio determinasse da solo la sorte del rapporto —
ricadrebbe nell'articolo 22 sulle decisioni automatizzate (sentenza della Corte di giustizia dell'Unione europea
del 7 dicembre 2023, causa C-634/21, §2.3). Nessuna storia di SubGrove porta questi presidi, e non deve portarli:
SubGrove non prevede nulla.

C'è una seconda differenza, altrettanto sostanziale: **il perimetro dei clienti sorvegliati**. SubGrove guarda
**gli abbonati**, cioè chi ha un contratto ricorrente formalizzato. RenewGrove guarda **il rapporto commerciale**,
qualunque forma abbia: l'abbonato di SubGrove, ma anche il cliente che rifattura un contratto di assistenza da
**02 BillGrove** ogni anno senza che sia un abbonamento, il cliente che ricompra a intervalli regolari, il cliente
che ha aperto quattro segnalazioni in un mese su **12 DeskGrove**. Un'attività su tre di quelle a cui si rivolge
questa app **non ha abbonamenti affatto**, e ha comunque clienti che perde.

### 0.3 La posizione che prendo — ⚠️ da confermare, è direzione di prodotto

**RenewGrove e SubGrove non sono la stessa applicazione, ma la scheda 33 come è scritta sì.** La scheda descrive un
prodotto che è per due terzi SubGrove. La raccomandazione è di **riscriverla stretta**, ed è ciò che questo
documento fa:

- **fuori** dal perimetro di RenewGrove: generare scadenze, generare avvisi di rinnovo, sollecitare un pagamento,
  sospendere o disdire un contratto, misurare il ricavo ricorrente e prevederne l'incasso. Tutto questo è di
  SubGrove e resta di SubGrove;
- **dentro**: raccogliere i **segnali** che le altre app producono, trasformarli in un **punteggio spiegabile e
  contestabile**, proporre un **intervento** che una persona conferma prima che esca, e **misurare** se
  l'intervento è servito.

Detto in una riga, che è anche il modo di venderla: **SubGrove è il libro mastro del ricorrente; RenewGrove è il
lavoro sulla relazione**. Il primo dice *quanto ti devono*, il secondo dice *chi stai per perdere e che cosa puoi
farci*.

**L'alternativa esiste ed è legittima, va detto.** Se la piattaforma preferisse un catalogo più corto, tutto ciò che
resta di RenewGrove starebbe in un'epica ottava di SubGrove («salute del rapporto»), costerebbe meno da costruire e
si venderebbe da sé a chi ha già l'app. Il prezzo di quella scelta è preciso e va messo in conto: si perde la
copertura di tutte le attività **senza abbonamenti** — che è il grosso del mercato micro — e si mette un motore
predittivo, con i suoi obblighi di spiegazione e contestazione, dentro un'app che oggi è deterministica e che deve
restarlo. **Non è una decisione che spetta a un agente**: è la voce numero 1 dei punti aperti (§11).

**Conseguenza operativa se si tiene RenewGrove separata**: RenewGrove **non ha dati propri**. Ogni segnale arriva
da un'altra applicazione, e il modo in cui arriva è il cuore tecnico del documento (§4.2). Senza almeno una fonte
collegata, l'app è un contenitore vuoto e lo dice.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** RenewGrove sorveglia i rapporti che il cliente ha con i **suoi** clienti e segnala quelli che sta per
perdere. Fa tre cose concrete. Primo, raccoglie **segnali** dalle altre applicazioni appgrove a cui l'account è
abilitato — una rata non rientrata, una segnalazione di assistenza riaperta tre volte, un contratto che scade fra
sessanta giorni, un cliente che non compra da quando compra di solito. Secondo, li compone in un **punteggio di
rischio** che si apre e si legge: quali fatti lo hanno formato, quanto pesa ciascuno, che cosa lo farebbe scendere,
e un pulsante per dire «questo fatto non è pertinente» che lo ricalcola davanti agli occhi di chi guarda. Terzo,
propone un **intervento** — telefonare, scrivere, offrire una proroga — che **una persona conferma** prima che esca
qualcosa verso il cliente finale, e poi **misura**, a distanza di mesi, se quel rapporto è rimasto.

**Per chi.** Micro-impresa (1-10 addetti) e piccola impresa (10-50) che vive di **clienti che tornano**: studi di
consulenza con contratti di assistenza, service informatici, manutentori, scuole e centri, agenzie con clienti a
retainer, fornitori business-to-business con ordini ricorrenti. Compra il titolare, perché è lui che sente la
perdita di un cliente sul conto corrente. Usa tutti i giorni chi tiene la relazione: il titolare stesso nelle
micro-imprese, il responsabile commerciale o l'amministrazione nelle piccole.

**Quale problema toglie.** Nelle attività di questa taglia la perdita di un cliente **non si vede finché non è
avvenuta**. Non c'è un giorno in cui il cliente dice «me ne vado»: c'è un contratto che scade e nessuno lo richiama,
una fattura contestata a cui si risponde in ritardo, un ordine che non arriva a marzo come tutti gli anni. Chi tiene
d'occhio queste cose lo fa a memoria, e la memoria funziona finché i clienti sono venti. A cinquanta smette, senza
avvisare. Il costo è doppio: si perde il cliente, e — questa è la parte che fa più male al titolare — **si scopre di
averlo perso quando è troppo tardi per parlargli**. La finestra utile per intervenire, nelle fonti consultate al
§2.5, è di giorni, non di mesi.

**Cosa NON fa.**

- **Non genera scadenze, avvisi di rinnovo né solleciti di pagamento.** Quelli sono di **19 SubGrove** (§0). Se una
  rata non è rientrata, RenewGrove lo *sa* — perché SubGrove glielo dice — e lo usa come segnale; non manda un
  secondo sollecito.
- **Non decide nulla da solo.** Il punteggio non fa partire un messaggio, non applica uno sconto, non chiude un
  rapporto. Ogni effetto verso il cliente finale passa da una conferma umana esplicita (§0.2, e il presidio della
  storia `0017`).
- **Non è un sistema di posta né di messaggistica.** Nella forma raccomandata (§4.3) RenewGrove **non conserva i
  recapiti** dei clienti finali e **non invia** nulla: prepara l'intervento e lo consegna all'applicazione che ha
  già la relazione, oppure lo mette in una lista di lavoro che una persona esegue.
- **Non è un sistema di gestione della relazione con i clienti.** Le trattative, le offerte, l'anagrafica
  autorevole stanno in **04 LeadGrove** e nel mini-gestionale dei contatti già presente nel repository. RenewGrove
  tiene un **riferimento**, non una copia autorevole.
- **Non fa punteggio di affidabilità creditizia.** «Rischio di perdere questo cliente» non è «rischio che questo
  cliente non paghi». Sono due punteggi diversi con due regimi giuridici diversi, e confonderli sarebbe un errore
  grave: il secondo, in Europa, è materia dell'articolo 22 in senso pieno e di valutazioni che questo documento non
  può scrivere (§2.3, §6).
- **Non misura il ricavo ricorrente né lo prevede.** È di SubGrove (`0027`-`0030`) e, per la lettura trasversale,
  di **20 InsightGrove**.

**Rischio di sostituzione da parte dei modelli linguistici.** `rafforzata`, come nel catalogo, con una motivazione
precisa. Un assistente generico, davanti a un elenco incollato di clienti, sa produrre un punteggio plausibile —
e questo è esattamente il problema: un punteggio plausibile e non tracciabile è peggio di nessun punteggio, perché
non si può contraddire. Il valore di RenewGrove non sta nel *calcolare*: sta nel fatto che il punteggio nasce da
**fatti datati e attribuibili**, arrivati da applicazioni che li hanno registrati per un altro motivo, che ogni fatto
si può marcare come non pertinente lasciando traccia, e che dopo sei mesi si può dire **se le cose fatte hanno
funzionato**. Il livello conversazionale, quando ci sarà, è la superficie naturale per le domande di questa app
(«chi rischio di perdere questo mese?»), ma la risposta vale solo perché sotto c'è uno storico che qualcuno ha
tenuto onestamente.

---

## 2. Mercato e analisi in rete

> Compilata dopo sette ricerche mirate, di cui **quattro letture dirette di pagine ufficiali** dei fornitori
> ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4). Ciò che non è stato trovato è **dichiarato** al §2.7.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| **Baremetrics** (+ moduli *Payment Recovery* e *Cancellation Insights*) | globale (USA) | Analitiche degli abbonamenti; il recupero dei pagamenti falliti e la raccolta dei motivi di disdetta sono **moduli aggiuntivi a pagamento** | Launch **75 $/mese** (49 $ se annuale, fino a 360 mila $ di ricavo annuo); Growth **255 $/mese** (189 $ annuale); Scale **1.152 $/mese** (749 $ annuale). *Payment Recovery* **+129 $/mese**; *Cancellation Insights* **+129 $/mese** | [baremetrics.com/pricing](https://baremetrics.com/pricing) — pagina ufficiale |
| **Churnkey** | globale (USA) | Trattenimento: flusso di disdetta con offerte, recupero dei pagamenti falliti, campagne di riconquista | Starter **250 $/mese** con fatturazione annuale, per un volume di abbandono fino a **5.000 $/mese**; Core e Intelligence su preventivo, da 10.000 $/mese di abbandono. Prova di **14 giorni senza carta**. Nessun piano gratuito | [churnkey.co/pricing](https://churnkey.co/pricing) — pagina ufficiale |
| **ProsperStack** | globale (USA) | Flusso di disdetta configurabile, offerte di trattenuta, campagne di riconquista | Grow **da 200 $/mese** (50-500 sessioni di disdetta al mese); Prosper **da 750 $/mese** (oltre 500 sessioni); Enterprise su preventivo. Nessun piano gratuito | [prosperstack.com/pricing](https://prosperstack.com/pricing/) — pagina ufficiale |
| **Custify** | Europa (Romania) | Piattaforma di *customer success* per software di taglia piccola e media: punteggi di salute, cruscotti, automazioni | **Prezzi non pubblicati**: la pagina ufficiale non riporta né piani né importi. Le fonti di comparazione parlano di «da 200 $/mese», e non le uso come rilevazione | [custify.com/pricing](https://www.custify.com/pricing/) — pagina ufficiale, **senza prezzi** |
| **ChurnZero**, **Gainsight**, **Totango**, **Planhat** | globale | Piattaforme di *customer success* per aziende strutturate | **Prezzi non pubblicati** (vendita su preventivo). Gli ordini di grandezza circolanti — decine di migliaia di dollari all'anno — vengono da comparatori e **non** li riporto come rilevati | vedi §2.7 |

**Lettura.** Il campo si divide in tre famiglie, e nessuna delle tre copre il nostro cliente.

1. **Le piattaforme di *customer success*** (ChurnZero, Gainsight, Totango, Planhat, Custify) fanno esattamente il
   mestiere di RenewGrove — punteggio di salute, avvisi, sequenze di intervento — ma sono costruite per aziende con
   un **reparto** dedicato, si vendono su preventivo e presuppongono che il cliente sia un'azienda di software con
   dati di utilizzo del proprio prodotto. Una carrozzeria con quaranta clienti fissi non ha né il reparto né i dati.
2. **Gli strumenti di trattenimento all'atto della disdetta** (Churnkey, ProsperStack) sono verticali su un momento
   solo: il cliente clicca «disdici» e loro provano a fermarlo. È un momento che nel nostro segmento **quasi non
   esiste**, perché il cliente di una micro-impresa non disdice cliccando: smette di chiamare. Il loro prezzo
   d'ingresso — 200-250 $ al mese — è dieci volte fuori scala per noi.
3. **Le analitiche degli abbonamenti** (Baremetrics) partono da un altro problema e vendono il recupero come
   modulo aggiuntivo.

**Il dato di posizionamento più utile che ho trovato** è nel listino di Baremetrics: il modulo *Payment Recovery*
costa **129 $/mese** su un piano base da **75 $/mese**. Il pezzo «agisci per non perdere il cliente» viene prezzato
**più caro** del pezzo «misura come stai andando». È il contrario dell'intuizione, e sostiene l'ipotesi di catalogo
che la disponibilità a pagare qui sia alta perché il valore è legato direttamente al fatturato. Va però pesato: quel
listino parla ad aziende di software americane, non a un manutentore di Vicenza.

**Dove si può stare, quindi.** In mezzo non c'è nulla per il nostro segmento: uno strumento **in italiano**, che
costa come un abbonamento telefonico, che **non chiede dati di utilizzo di un prodotto software** (che il cliente non
ha) ma usa i segnali che le altre app della suite già producono, e che tratta il punteggio come un **argomento
discutibile** invece che come un verdetto. La terza caratteristica è anche la difendibile: è ciò che nessuno dei
concorrenti fa, ed è ciò che la legge europea chiede (§2.3).

### 2.2 Prezzi praticati nel dominio

**Unità di misura prevalente: tre, tutte diverse dalla nostra.**

1. **Scaglioni di ricavo ricorrente annuo** — è la base di Baremetrics: fino a 360 mila dollari, da 360 mila a 3,6
   milioni, oltre. Rilevata su pagina ufficiale.
2. **Volume di abbandono mensile** — è la base di Churnkey: fino a 5.000 dollari al mese di ricavo perso, oltre i
   10.000. Rilevata su pagina ufficiale. È elegante (si paga in proporzione al problema) e per noi **inutilizzabile**:
   richiede di stare dentro il flusso del denaro per misurarlo.
3. **Numero di sessioni di disdetta al mese** — è la base di ProsperStack: 50-500, oltre 500. Rilevata su pagina
   ufficiale.

**La scheda di catalogo propone scaglioni sul ricavo ricorrente tracciato. Raccomando di non usarli**, per la stessa
ragione per cui SubGrove ha raccomandato di togliere la componente a percentuale dal proprio listino: è un numero
che il cliente **dichiara** e che noi non possiamo verificare, perché il denaro non ci passa davanti. Un prezzo che
si può ridurre dichiarando meno non è un prezzo. Dove finisce allora la dimensione «quanto vale la tua attività»?
Nel **limite**, non nel prezzo: chi ha più clienti da sorvegliare sta su un piano più alto (§3, §5).

**Piano gratuito**: assente in Churnkey e in ProsperStack; assente in Baremetrics. Nel nostro segmento la porta
d'ingresso gratuita ha però un ruolo diverso, perché RenewGrove **non funziona senza fonti collegate** e il piano
gratuito serve a provare che i segnali arrivano davvero (§5).

**Durata della prova**: **14 giorni senza carta** in Churnkey (rilevato su pagina ufficiale); non rilevata per gli
altri.

### 2.3 Obblighi normativi del settore

Il dominio **è normato**, e le norme non toccano il settore del cliente: toccano **il punteggio** e **le azioni verso
il cliente finale**. Sono le due cose che fanno di RenewGrove un prodotto diverso da SubGrove, e sono anche le due
che lo rendono più delicato da costruire.

1. **Un punteggio di rischio è profilazione, e in certe condizioni è una decisione automatizzata.** La Corte di
   giustizia dell'Unione europea, con la sentenza del **7 dicembre 2023, causa C-634/21** (il caso noto come SCHUFA),
   ha stabilito che il calcolo automatizzato di una probabilità sul comportamento di una persona costituisce una
   **decisione automatizzata ai sensi dell'articolo 22** quando quel valore determina «in modo decisivo» la
   conclusione, l'esecuzione o la cessazione di un rapporto contrattuale — **anche quando chi calcola il punteggio e
   chi prende la decisione sono soggetti diversi**. Ne discendono obblighi precisi: informazioni significative sulla
   **logica utilizzata**, comunicazione dell'**importanza e delle conseguenze previste**, **intervento umano** su
   richiesta, possibilità di **esprimere la propria opinione e di contestare** la decisione.
   → è la ragione per cui l'epica 03 esiste nella forma in cui è scritta: il punteggio si apre (`0014`), si
   contraddice (`0015`) e **non fa partire nulla da solo** (`0017`). Non è una raccomandazione di stile: è la
   differenza fra un prodotto vendibile in Europa e uno no.
   Fonti: [Cybersecurity360, sintesi della sentenza C-634/21](https://www.cybersecurity360.it/legal/privacy-dati-personali/decisioni-automatizzate-sullo-scoring-la-corte-ue-amplia-la-tutela-del-gdpr-di-fronte-allalgoritmo/) ·
   [Agenda Digitale, articolo 22 e profilazione](https://www.agendadigitale.eu/sicurezza/privacy/articolo-22-gdpr-come-ci-tutela-da-algoritmi-e-profilazione/).
   **Avvertenza d'onestà**: sono due fonti divulgative specializzate, **non** il testo della sentenza né le linee
   guida del Comitato europeo per la protezione dei dati. La lettura è coerente fra loro e con il testo
   dell'articolo 22, ma il punto merita la revisione legale (punto aperto n. 4).
2. **Ostacolare la disdetta è vietato, e un'offerta di trattenuta può diventare un ostacolo.** La legge tedesca
   impone dal **1° luglio 2022** (§ 312k del codice civile tedesco) un **pulsante di disdetta** chiaramente
   riconoscibile per i contratti a esecuzione continuata conclusi online; la Corte federale, con sentenza del **22
   maggio 2025**, ne ha esteso la portata anche a contratti con pagamento unico se il fornitore continua a erogare
   durante la durata. Chi non lo rispetta si espone a diffide e azioni inibitorie **e il consumatore può recedere in
   qualunque momento senza preavviso**. La fonte consultata **non chiarisce** se sia ammesso frapporre uno sconto o
   un sondaggio prima della conferma: è precisamente la domanda che riguarda il prodotto — vedi §2.7 e punto aperto
   n. 5. → è la ragione per cui la storia `0022` **vieta** che un'offerta di RenewGrove si frapponga al percorso di
   disdetta di SubGrove e la confina al momento *precedente*, quando la disdetta non è stata chiesta.
   Fonte: [Noerr, «Cancellation button in online sales»](https://www.noerr.com/en/insights/cancellation-button-in-online-sales).
3. **Rinnovo tacito e preavviso.** L'obbligo di informare prima del rinnovo e di offrire un canale digitale di
   recesso semplice quanto quello di adesione ricade su **SubGrove** (che genera l'avviso dovuto) e non su
   RenewGrove. L'analisi di dettaglio, con le sue avvertenze, sta nel documento di SubGrove, §2.3: **non la
   riscrivo e non la riverifico**, perché il perimetro di RenewGrove (§0.3) non comprende gli avvisi di rinnovo.
   → conseguenza per noi: un'offerta di trattenuta proposta a ridosso di un rinnovo **cambia le condizioni
   comunicate nell'avviso**, e questo è un punto di coordinamento fra le due app, non un dettaglio (`0022`).
4. **Regolamento europeo sull'intelligenza artificiale.** Non sono riuscito a determinare se un punteggio di rischio
   di abbandono ricada in una categoria regolata — vedi §2.7. Quello che si può dire senza inventare: **non è**
   valutazione dell'affidabilità creditizia di persone fisiche (che è espressamente ad alto rischio), perché non
   decide l'accesso a un credito; ed è, nella forma qui proposta, un modello **a regole dichiarate e leggibili**,
   non un modello addestrato — scelta presa anche per questo motivo (`0012`).

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta prevista. Le prime cinque sono **applicazioni della suite**, non fornitori esterni: è la
conseguenza diretta del fatto che RenewGrove non ha dati propri.

1. **19 SubGrove** — abbonamenti, rate non rientrate, disdette, sospensioni. È la fonte più ricca e la prima da
   costruire.
2. **02 BillGrove** e l'app reale `fatture` — documenti emessi, contratti rifatturati, ritardi di pagamento. È la
   fonte che rende utile RenewGrove a chi **non ha abbonamenti**.
3. **12 DeskGrove** — segnalazioni di assistenza aperte, riaperte, chiuse con insoddisfazione. Nelle fonti del §2.5
   è il segnale più predittivo dopo il pagamento.
4. **04 LeadGrove** e il mini-gestionale dei contatti — l'anagrafica condivisa: il rapporto sorvegliato *è* un
   cliente. Riferimento, non copia.
5. **07 BookGrove** — prenotazioni disdette o non presentate, per chi vende appuntamenti.
6. **Il foglio di calcolo che il cliente ha oggi** — importazione iniziale dell'elenco dei clienti da sorvegliare.
   È sempre la prima cosa che chiedono, e si sottovaluta sempre.
7. **La posta elettronica** per gli avvisi interni a chi lavora — il servizio di invio è già di piattaforma, nessun
   fornitore nuovo.

⚠️ **Nessuna di queste introduce un responsabile esterno del trattamento**, ed è un risultato voluto del disegno del
§4.3: nella forma raccomandata RenewGrove non invia messaggi al cliente finale, quindi non ha bisogno né di un
fornitore di messaggistica né dei recapiti. Se la scelta cambiasse (opzione B del §4.3), cambierebbe anche questa
riga, e andrebbe rifatta la sezione 6.

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Non ho trovato una raccolta di recensioni specifica per il segmento micro europeo (§2.7). Le fonti consultate
sull'anticipazione dell'abbandono convergono su cinque affermazioni, che leggo come **requisiti travestiti**:

- **il segnale non è il valore assoluto, è lo scostamento dalla normalità di quel cliente**. Un cliente che apre
  due segnalazioni al mese da sempre non è in crisi; uno che ne apre due dopo due anni di silenzio sì. → è la
  ragione per cui il punteggio della storia `0012` lavora su **scostamenti da una linea di base per rapporto** e non
  su soglie assolute uguali per tutti;
- **conta la tendenza, non il singolo giorno**: quattro settimane consecutive di calo sono un segnale, un lunedì
  fiacco no. → finestre di osservazione dichiarate, non istantanee;
- **i segnali che pesano di più** sono, in ordine: gli **esiti di pagamento** (una rata fallita è il più forte),
  l'**andamento delle segnalazioni di assistenza**, il **calo di utilizzo rispetto alla propria linea di base**, la
  **completezza dell'avvio del rapporto** nei primi novanta giorni;
- **la soglia non è universale**: lo stesso valore è sano per un cliente maturo e allarmante per uno nuovo. → il
  punteggio si tara per attività (`0016`) e la fascia si legge insieme all'anzianità del rapporto;
- **la finestra utile è breve** e l'attrito precede la decisione di settimane: quando il punteggio scende, spesso è
  già tardi.
  Fonti: [Supportbench, «Building a health score that actually predicts churn»](https://www.supportbench.com/building-health-score-predicts-churn/) ·
  [Fullstory, «Predicting customer churn with behavioral signals»](https://www.fullstory.com/blog/predicting-customer-churn/).
  **Avvertenza d'onestà**: sono fonti di fornitori, con l'interesse commerciale che ne consegue, e parlano di
  aziende di software, non di manutentori. Le leggo come **indicazioni di direzione**, non come misure, e nessun peso
  numerico del punteggio proposto in questo documento è tratto da esse.

Quello che questo segmento **rifiuta**, e che va tenuto fuori: i cruscotti con dodici indicatori; la configurazione
a regole («se lo stato è X e sono passati N giorni allora…»), che SubGrove ha già rilevato come rifiutata; e —
soprattutto — **l'automatismo che scrive ai clienti da solo**. Un titolare di micro-impresa conosce i suoi clienti
per nome: un messaggio partito a sua insaputa è un danno di reputazione, non un risparmio di tempo. È la ragione per
cui la conferma umana in questa app non è un presidio di conformità appiccicato sopra, ma **la forma del prodotto**.

### 2.6 Fonti consultate

1. **Baremetrics — listino ufficiale** — https://baremetrics.com/pricing — piani a scaglioni di ricavo annuo (75 / 255 / 1.152 $ al mese) e i due moduli aggiuntivi a **129 $/mese** ciascuno: mi ha dato la base di calcolo di categoria e il dato di posizionamento più utile (il modulo «agisci» costa più del piano «misura»). Conferma e precisa il riferimento citato nella scheda di catalogo.
2. **Churnkey — listino ufficiale** — https://churnkey.co/pricing — Starter a 250 $/mese annuali per un volume di abbandono fino a 5.000 $/mese, prova di 14 giorni senza carta, nessun piano gratuito: mi ha dato l'unica durata di prova rilevata su fonte ufficiale e la base di calcolo «volume di abbandono», che ho scartato con motivazione (§2.2).
3. **ProsperStack — listino ufficiale** — https://prosperstack.com/pricing/ — 200 $/mese per 50-500 sessioni di disdetta, 750 $/mese oltre: terza base di calcolo del dominio, e la conferma che il prezzo d'ingresso della categoria è un ordine di grandezza sopra il nostro segmento.
4. **Custify — pagina prezzi ufficiale** — https://www.custify.com/pricing/ — **nessun prezzo pubblicato**: è un risultato negativo e lo riporto come tale, invece di ripiegare sui comparatori.
5. **Corte di giustizia UE, causa C-634/21 (SCHUFA), 7 dicembre 2023** — sintesi su https://www.cybersecurity360.it/legal/privacy-dati-personali/decisioni-automatizzate-sullo-scoring-la-corte-ue-amplia-la-tutela-del-gdpr-di-fronte-allalgoritmo/ e inquadramento dell'articolo 22 su https://www.agendadigitale.eu/sicurezza/privacy/articolo-22-gdpr-come-ci-tutela-da-algoritmi-e-profilazione/ — un punteggio che determina in modo decisivo la sorte di un rapporto contrattuale è una decisione automatizzata, con obbligo di spiegare la logica, garantire l'intervento umano e consentire la contestazione: **ha determinato l'intera epica 03** e il divieto di automatismo dell'epica 04.
6. **Noerr — obbligo del pulsante di disdetta (§ 312k del codice civile tedesco)** — https://www.noerr.com/en/insights/cancellation-button-in-online-sales — in vigore dall'1/7/2022, esteso dalla Corte federale il 22/5/2025; violazione = diffide e recesso libero del consumatore: ha determinato il confine della storia `0022`, che vieta di frapporre un'offerta al percorso di disdetta.
7. **Supportbench, «Building a health score that actually predicts churn»** — https://www.supportbench.com/building-health-score-predicts-churn/ — e **Fullstory, «Predicting customer churn with behavioral signals»** — https://www.fullstory.com/blog/predicting-customer-churn/ — scostamento dalla linea di base invece di soglie assolute, tendenza invece di istantanea, ordine di importanza dei segnali: hanno determinato la forma del modello di punteggio (`0012`) e la taratura per attività (`0016`).

### 2.7 Cosa NON sono riuscito a determinare

- **I prezzi delle piattaforme di *customer success***, che sono i concorrenti funzionali più vicini: Custify non
  pubblica il listino, ChurnZero, Gainsight, Totango e Planhat vendono su preventivo. I numeri circolanti vengono da
  comparatori e non li riporto. Conseguenza: la proposta del §5 **non** poggia su di loro, ma sulle tre pagine
  ufficiali rilevate e sull'ancoraggio interno a SubGrove.
- **Se sia ammesso, in Germania e in Italia, frapporre un'offerta di trattenuta o un sondaggio prima della conferma
  di una disdetta.** La fonte sul § 312k non lo affronta. È la domanda più direttamente commerciale del prodotto e
  resta aperta (punto aperto n. 5): la storia `0022` adotta nel frattempo la postura **più prudente possibile** —
  l'offerta vive solo *prima* che la disdetta sia chiesta.
- **Se un punteggio di rischio di abbandono ricada in una categoria regolata dal regolamento europeo
  sull'intelligenza artificiale.** Non ho verificato l'allegato applicabile. Non invento la risposta: punto aperto
  n. 6, per la revisione legale.
- **Quale sia il peso relativo *vero* dei segnali** in imprese non-software. Tutte le fonti sulla predizione
  dell'abbandono che ho trovato parlano di aziende di software con dati di utilizzo del prodotto. Per un manutentore
  o uno studio di consulenza **non esiste il dato di utilizzo**: i segnali disponibili sono pagamento, assistenza e
  ritmo degli acquisti. Conseguenza pratica, e va detta chiaramente: i pesi di partenza della storia `0012` sono
  **una convenzione dichiarata, non una stima** — ed è il motivo per cui devono essere modificabili dal cliente
  (`0016`) e per cui il rendiconto di efficacia (`0027`) è il vero collaudo del modello.
- **Se esista un concorrente italiano** con questa promessa per il segmento micro. La ricerca ha restituito solo
  piattaforme americane e agenzie di consulenza. L'assenza di risultati **non** è prova di assenza.

---

## 3. Varco d'identità — le risposte pronte per `new-application`

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `fidelizzazione` | Rispetta `^[a-z][a-z0-9_]{0,30}$` (14 caratteri, minuscolo, sole lettere) e segue la convenzione del repository, dove l'identificativo tecnico è in italiano (`fatture`) e il nome commerciale sta a parte. **Ho scartato `rinnovi` e `renewal`**, che sarebbero la traduzione diretta del nome commerciale: rimetterebbero in circolo esattamente la confusione che il §0 esiste per spegnere — il rinnovo è di `abbonati` (SubGrove), qui si lavora sul *rapporto*. Ho scartato `churn` (sigla di gergo, vietata dalle regole di lingua) e `retention` (inglese non necessario: esiste la parola italiana). Ho scartato `clienti`, troppo generico e collidente con l'anagrafica condivisa. `fidelizzazione` è lungo — nelle rotte `/api/fidelizzazione/v1/*` e nello schema `app_fidelizzazione` si vede — e lo dico come costo consapevole: è il prezzo di un nome che non si può fraintendere. Cambiarlo dopo non è una rinomina, è una migrazione di dati. |
| **Modello utente** | `multi` | Nella giornata tipo lavorano due figure diverse, e la seconda è la ragione per cui il modello a utente singolo non basta. Chi tiene la relazione (commerciale, amministrazione) **guarda** i punteggi e prepara gli interventi; il titolare **autorizza** ciò che ha un costo — uno sconto, una proroga, una condizione fuori listino. Ma soprattutto: questa app produce **giudizi su persone e aziende** e propone **effetti verso l'esterno**. Le domande «chi ha marcato questo fatto come non pertinente», «chi ha confermato questo intervento» e «chi ha collegato questa fonte» non sono un lusso amministrativo: sono la traccia che rende il punteggio contestabile davvero (§2.3) e che permette di rispondere a un cliente finale che chiede spiegazioni. Un'app a utente singolo non ha il concetto di «chi ha fatto cosa». |
| **Porta locale** | `8133` | Convenzione del kit (8100 + 33) per non far collidere le sessanta proposte. Da confermare con `./dev.sh services` al momento dello scaffolding. |
| **Metrica di quota** | `rapporti_sorvegliati` | La **sola** cosa che il piano limita: quanti rapporti con clienti l'app tiene sotto osservazione in un dato momento. È ciò che cresce esattamente con il valore ricevuto — chi sorveglia trecento clienti riceve trecento volte il servizio di chi ne sorveglia uno — ed è un numero che **l'app conta da sé**, senza chiedere niente al cliente. **Ho scartato gli scaglioni di ricavo ricorrente** proposti dalla scheda di catalogo: è un dato dichiarato dal cliente e non verificabile da noi, perché il denaro non ci passa davanti (§2.2). **Ho scartato «interventi al mese»**, che pure sarebbe un consumo misurabile, per una ragione che vale la pena scrivere: l'intervento è **l'azione che vogliamo che il cliente faccia**. Metterle un contatore sopra significa insegnargli a non farla — e un prodotto che si misura sull'efficacia (epica 05) non può disincentivare la cosa di cui misura l'efficacia. |
| **Che cosa conta nella metrica** | stato `sorvegliato` | Contano i rapporti in stato **sorvegliato**: quelli su cui i segnali entrano, il punteggio si ricalcola ogni giorno e gli avvisi scattano. **Non** contano gli `archiviato` (lo storico resta, il lavoro no) né gli `escluso` per contestazione — escludere un rapporto è un diritto di chi contesta il giudizio (`0015`), e farlo costare quota significherebbe far pagare l'esercizio di quel diritto. Conseguenza voluta: il rimedio alla quota esaurita è **archiviare**, azione reversibile che non perde niente. |
| **Natura della metrica** | `stock` | Tetto su ciò che esiste **ora**: «il piano Cura sorveglia 250 rapporti; per aggiungerne uno bisogna smettere di sorvegliarne un altro, o si passa di piano». Non è un consumo su una finestra che si azzera: un rapporto sorvegliato il primo agosto è ancora lì il primo settembre e continua a costare lavoro all'app ogni giorno — segnali che entrano, punteggio che si ricalcola, avvisi che scattano. Contarlo come consumo lascerebbe accumulare senza limite. Conseguenza voluta e da spiegare bene a schermo: la **riduzione di piano è sbarrata** finché i rapporti sorvegliati eccedono il tetto del piano di destinazione (regola di piattaforma, [docs/09-pagamenti.md](../../../09-pagamenti.md) dec. 23), e il rimedio si dice — «togli N rapporti dalla sorveglianza, poi potrai scendere». Un rapporto si toglie dalla sorveglianza senza cancellarne lo storico: è archiviato, non sparito (`0009`). |
| **Colore-categoria e icona** | `teal` · icona `heart-pulse` (un cuore attraversato da un tracciato di battito) | Deve coincidere fra listino (`category`) e modulo frontend (`accentToken`). Tre colori sono **esclusi per principio**, non per gusto: `green`, `amber` e `red` devono restare liberi di significare «in salute / attenzione / a rischio» **dentro** i punteggi e le fasce di questa app — un'app che colora il rischio non può prendersi il rosso come tinta d'insegna (è lo stesso argomento con cui 20 InsightGrove ha escluso gli stessi tre). Fra i restanti: `blue` è il colore della relazione commerciale (04 LeadGrove) e di 20 InsightGrove, e confonderebbe «chi compra» con «chi resta»; `violet` è il più affollato del catalogo (06, 13, 16). Resta `teal`, proposto finora da 02 BillGrove e 12 DeskGrove, ed è difendibile nel merito: la fidelizzazione è un'area di **servizio al cliente**, sorella dell'assistenza, non delle vendite. **Collisione dichiarata**: sei colori per sessanta app, il conflitto è strutturale e non risolvibile da qui (punto aperto n. 7). |

---

## 4. Modello di dominio

### 4.1 Entità principali

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Fonte` | Un'applicazione appgrove collegata come sorgente di segnali, per questo account | app d'origine, stato (collegata/sospesa/scollegata), chi l'ha collegata e quando, momento dell'ultimo segnale, ritardo atteso, esito del ripopolamento | no |
| `Rapporto` | Il cliente del nostro cliente, sorvegliato | riferimento opaco d'origine, **etichetta leggibile**, data di inizio del rapporto, stato di sorveglianza (sorvegliato/archiviato/escluso), linea di base calcolata, punteggio corrente e fascia | **sì** — l'etichetta è, di norma, un nome (§6) |

⚠️ **Identità del rapporto quando le fonti sono più d'una — problema aperto, con effetto sul conto della quota.**
Il rapporto è identificato dalla coppia `(app d'origine, riferimento opaco)`. Ne discende che lo stesso cliente,
presente sia in SubGrove sia in BillGrove, genera **due rapporti distinti** e consuma **due unità** della metrica.
È il difetto più prevedibile del modello: l'utente vede due volte lo stesso nome e paga per entrambi. Le vie sono
tre — unione manuale suggerita dall'etichetta; riferimento all'anagrafica clienti condivisa della suite come chiave
unica (che però oggi non è autorevole per tutte le app); accettare il doppione e dichiararlo. **Raccomando la
seconda**, ma dipende dallo stato dell'anagrafica condivisa, che non è materia di questa app: punto aperto n. 10.
Finché non è chiuso, la quota va spiegata a schermo per quello che conta davvero — *rapporti sorvegliati*, non
*clienti*.
| `Segnale` | Un fatto datato arrivato da una fonte, riferito a un rapporto | fonte, rapporto, tipo (elenco chiuso), momento del fatto, intensità numerica, unità, chiave di idempotenza, riferimento opaco alla riga d'origine, stato (valido / marcato non pertinente) | **sì**, per riferimento — è un fatto riferito a una persona identificabile |
| `ModelloDiPunteggio` | Le regole con cui i segnali diventano un numero | versione, elenco delle voci (tipo di segnale, peso, verso, finestra di osservazione), soglie delle fasce, stato (bozza/vivo/archiviato) | no |
| `Punteggio` | Il valore calcolato per un rapporto in un istante | rapporto, versione del modello, valore, fascia, momento del calcolo, **elenco dei contributi** (segnale, quanto ha pesato, in che verso) | **sì**, per riferimento — è una previsione su una persona |
| `Contestazione` | Il fatto che qualcuno abbia detto «questo non è pertinente» | segnale o rapporto, chi, quando, motivo, effetto (segnale escluso / rapporto escluso dalla sorveglianza) | no (riguarda un utente nostro) |
| `PianoDiIntervento` | Il modello di azioni consigliate per una fascia di rischio | nome, fascia a cui si applica, passi consigliati con testo di partenza, chi deve autorizzare | no |
| `Intervento` | Un'azione preparata verso un rapporto | rapporto, piano d'origine, stato (bozza / confermato / consegnato / eseguito / annullato), chi l'ha preparato, chi l'ha confermato, canale, contenuto proposto, esito | **sì**, per riferimento |
| `OffertaDiTrattenuta` | Una concessione economica proposta per trattenere | intervento, tipo (sconto/proroga/cambio condizioni), valore, validità, chi l'ha autorizzata, stato | no (importi) |
| `EsitoDelRapporto` | Come è finita, misurata dopo una finestra dichiarata | rapporto, momento della valutazione, esito (trattenuto / perso / ancora aperto), motivo raccolto, gruppo (intervenuto / di confronto) | **sì**, per riferimento |
| `CoorteDiConfronto` | L'intestazione del gruppo di paragone di un periodo | periodo, criteri di formazione dichiarati **prima**, numerosità, fascia di riferimento | no — criteri e conteggi |
| `RendicontoEfficacia` | La sintesi di un periodo chiuso | periodo, conteggi per gruppo ed esito, somma delle concessioni autorizzate, distribuzione dei motivi | no — **soli aggregati**, ed è la ragione per cui sopravvive a una cancellazione (§6) |

### 4.2 🛑 Da dove arrivano i segnali — e perché l'accesso non aggira l'isolamento fra account

È la questione tecnica centrale dell'app, la stessa che **20 InsightGrove** ha già affrontato. Ho letto la sua
soluzione e **la adotto**, perché è corretta e perché due app che risolvono lo stesso problema in due modi diversi
sono un difetto di piattaforma. Ne dichiaro poi l'unica differenza sostanziale, che c'è ed è importante.

**Il meccanismo: eventi asincroni, mai chiamate.** L'app d'origine **pubblica** un segnale sul canale a eventi della
piattaforma (coda dedicata a `fidelizzazione`); il servizio `fidelizzazione` lo consuma e lo scrive nel **proprio**
schema `app_fidelizzazione`. Nessuna lettura fra schemi diversi, nessuna chiamata di rete verso un'altra app.

Le tre scorciatoie e perché sono vietate — le stesse tre di InsightGrove, e vale la pena riscriverle qui perché
chi implementa questa app leggerà questo documento e non quello:

1. **leggere direttamente lo schema altrui** → vietato da [PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8:
   ogni servizio ha un ruolo del database con privilegi **solo sul proprio schema**. Non è una convenzione da
   rispettare, è un permesso che non esiste;
2. **chiamare l'interfaccia dell'altra app in sola lettura** («`fidelizzazione` chiede a SubGrove l'elenco delle rate
   fallite dell'account») → vietato da §2: *un'app non chiama un'altra app; l'unica via fra servizi è asincrona a
   eventi*. La ragione sostanziale è più forte della regola: servirebbe un gettone valido su un'altra applicazione,
   cioè **una credenziale che scavalca l'abilitazione e il ruolo** — la definizione esatta della scorciatoia da
   evitare;
3. **copiare periodicamente le tabelle altrui** → duplica dati personali senza necessità, contro il principio di
   minimizzazione.

**Come si esprime il consenso dell'account.** Due cancelli, entrambi per account, come in InsightGrove:

- **abilitazione di piattaforma** (prerequisito): fra le fonti collegabili compaiono solo le applicazioni **a cui
  l'account è abilitato** e che sanno pubblicare segnali;
- **collegamento esplicito, una fonte per volta**, dato da un utente con ruolo `owner` o `admin`; un `member` vede in
  sola lettura. Prima di collegare, l'interfaccia mostra **l'elenco chiuso dei tipi di segnale** che quella fonte
  dichiara, con il loro significato: nessun collegamento al buio. Il collegamento si **revoca**, e la revoca è
  distruttiva e informata — dice quanti segnali verranno cancellati e quali punteggi smetteranno di essere
  calcolabili (storia `0008`).

**Come è garantito l'isolamento fra account.** Quattro proprietà, tutte da verificare con una prova e non da
promettere:

1. **in lettura `fidelizzazione` non è speciale**: legge il proprio schema con `WHERE tenant_id = :tid`, dove `:tid`
   viene dal gettone verificato, come ogni altra app;
2. **in scrittura l'account non è mai scelto da noi**: il `tenant_id` è quello che l'app d'origine ha messo
   nell'evento, preso a sua volta dal proprio gettone. `fidelizzazione` non lo deduce, non lo cerca, non lo accetta
   da una richiesta: **lo copia**. Un segnale senza `tenant_id`, o con un `tenant_id` sconosciuto alla piattaforma, è
   scartato;
3. **i due percorsi non condividono codice**: la scrittura dei segnali e la lettura via interfaccia web non hanno un
   punto in comune in cui un `tenant_id` di richiesta possa finire in una scrittura o viceversa. È il presidio più
   forte, e la storia `0007` chiede di verificarlo con una prova;
4. **il ripopolamento dello storico non è una lettura del passato altrui**: `fidelizzazione` pubblica una richiesta
   di ripopolamento **per un solo account e una sola fonte**, e la fonte ripubblica i propri fatti storici sullo
   stesso percorso. Non esiste una richiesta «per tutti gli account»: sarebbe esattamente la scorciatoia da evitare.

**Dove sta il ripopolamento, come storia.** InsightGrove gli dedica una storia a sé; qui è un **requisito della
storia `0008`**, quella del collegamento della fonte, e la scelta è voluta: un punteggio che lavora su scostamenti
da una linea di base (§2.5) **non esiste** finché non c'è uno storico da cui ricavarla, quindi una fonte collegata
senza ripopolamento sarebbe una fonte collegata a metà. Finestra proposta: **24 mesi**, la stessa della
conservazione (§6). Se in sede di implementazione la storia si rivelasse troppo grande, si scorpora — ed è il
genere di cosa che si scopre facendo, non scrivendo.

**La differenza sostanziale rispetto a InsightGrove, e va detta forte.** InsightGrove riceve **aggregati**: il
fatto di misura è un numero per un periodo, con dimensioni a identificativi opachi, e il caso peggiore di un difetto
è che trapeli *un numero*. RenewGrove riceve **fatti riferiti a un singolo cliente identificabile**, perché senza
soggetto non si può né formulare un giudizio né telefonare a qualcuno. **Non posso quindi rivendicare il contenimento
del danno per costruzione che InsightGrove rivendica, e non lo rivendico.** Le contropartite, che sostituiscono
quella garanzia e sono requisiti delle storie `0006` e `0007`:

- **elenco chiuso dei tipi di segnale**, dichiarato per fonte: un tipo non dichiarato non entra;
- **nessun testo libero** nel segnale, in nessuna forma. È il divieto che tiene fuori dalla porta le categorie
  particolari dell'articolo 9 (§6);
- **nessun campo anagrafico oltre l'etichetta leggibile**: niente indirizzo, niente recapito, niente identificativo
  fiscale, niente contenuto di documento;
- **l'etichetta viaggia su un evento separato** dal segnale e ha un consumatore distinto, così che il flusso dei
  segnali resti privo di dati personali anche quando l'etichetta esiste — è la soluzione che InsightGrove propone per
  le proprie etichette di dimensione, e qui vale identica;
- **un validatore eseguibile nei collaudi** rifiuta un segnale che violi una di queste regole e dice **quale** regola
  ha violato, con un collaudo di rifiuto per ciascun divieto.

**Lacuna dichiarata, come l'ha dichiarata InsightGrove**: se una fonte pubblicasse un `tenant_id` sbagliato,
`fidelizzazione` scriverebbe sotto l'account sbagliato e non potrebbe accorgersene. È un difetto **della fonte**,
coperto dalle prove di isolamento della fonte. Qui non è risolvibile, e nasconderlo sarebbe peggio che scriverlo.

### 4.3 Chi manda il messaggio al cliente finale — due vie, una raccomandata

Un'app che propone di contattare qualcuno deve dire **chi contatta e con quale recapito**. Le vie sono due e la
differenza fra loro non è tecnica: è di quanti dati personali questa app tratta.

**Via A — raccomandata: RenewGrove non conserva recapiti e non invia.** L'intervento confermato esce come **evento
di richiesta di comunicazione** verso l'applicazione che possiede la relazione (SubGrove per un abbonato, BillGrove
per un cliente fatturato, DeskGrove per una segnalazione): è lei che ha il recapito aggiornato, il modello di
messaggio e — cosa che conta — il rapporto già dichiarato nella propria informativa. L'esito torna indietro come
segnale. Quando **nessuna applicazione può inviare** — ed è il caso più frequente nelle micro-imprese, dove
l'intervento giusto è una telefonata — RenewGrove produce una **lista di lavoro**: chi chiamare, perché, che cosa
dire, e uno spazio per segnare com'è andata. In questa via appgrove **non compie alcun effetto verso l'esterno**, il
manifesto dei dati resta piccolo e non entra alcun fornitore nuovo.

**Via B — RenewGrove conserva un recapito e invia.** Più semplice per l'utente, più autonoma; e porta con sé una
copia dei recapiti che invecchia, un fornitore di messaggistica se si vuole andare oltre la posta elettronica, e una
seconda applicazione che scrive a persone che non sono nostri utenti (oggi lo fa solo SubGrove).

**Raccomando la via A**, e le storie `0020` e `0021` la implementano. ⚠️ La via A ha però un costo che va detto: il
contratto dell'evento di richiesta di comunicazione **oggi non esiste** nel repository, e riguarda tutte le
applicazioni destinatarie, non solo questa. È una decisione di piattaforma (punto aperto n. 2), gemella di quella che
InsightGrove ha sollevato sul contratto del fatto di misura. Se la piattaforma decidesse di non affrontarla adesso,
la ricaduta è precisa e non catastrofica: resta la lista di lavoro (`0021`), che copre da sola il caso d'uso
prevalente del segmento.

### 4.4 Relazioni e vincoli

`Fonte` 1→N `Segnale`; `Rapporto` 1→N `Segnale`; `Rapporto` 1→N `Punteggio` (serie storica, mai riscritta
all'indietro); `Punteggio` 1→N contributi, che puntano ai `Segnale` che lo hanno formato; `Rapporto` 1→N
`Intervento`; `Intervento` 0..1 `OffertaDiTrattenuta`; `Rapporto` 1→N `EsitoDelRapporto` (uno per finestra di
valutazione). `Segnale` è **in sola aggiunta**: un segnale non si modifica: si marca *non pertinente* con una
`Contestazione`, che è una riga nuova.

**Macchina a stati dell'intervento** — la parte che le storie dell'epica 04 devono rispettare:

```
   ┌──────────┐  conferma umana   ┌────────────┐  consegna all'app / lista  ┌───────────┐
   │  bozza   ├──────────────────►│ confermato ├───────────────────────────►│ consegnato│
   └────┬─────┘   (obbligatoria)  └─────┬──────┘                            └─────┬─────┘
        │ annullato                     │ annullato prima della consegna          │ esito registrato
        ▼                               ▼                                         ▼
   ┌──────────┐                    ┌──────────┐                             ┌───────────┐
   │ annullato│                    │ annullato│                             │  eseguito │
   └──────────┘                    └──────────┘                             └───────────┘
```

Regola non negoziabile della macchina: **da `bozza` non si esce senza una persona**. Non esiste un passaggio
automatico, non esiste una configurazione che lo abiliti, e la storia `0019` chiede una prova che lo dimostri.

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica (`deleted_at`);
schema `app_fidelizzazione`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8). Unica eccezione motivata: `Segnale` e `Punteggio` sono in
**sola aggiunta** — la correzione è una riga nuova, non un aggiornamento, perché uno storico che si riscrive
all'indietro non serve a misurare nulla (epica 05) e non regge una contestazione. La regola vale anche per
`EsitoDelRapporto`, **con una sola eccezione motivata**: il *motivo raccolto* arriva per natura **dopo** la
valutazione — si scopre parlando con il cliente, non alla scadenza della finestra — e quindi si scrive dopo. Anche
lì, però, non si sovrascrive in silenzio: una correzione del motivo è una riga di registro con autore e data
(storia `0026`).

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il file
> `services/core/src/main/resources/pricing/fidelizzazione.yaml`.

### 5.1 Gli scaglioni sul giro d'affari ricorrente vanno tolti

La scheda 33 propone «gratis sotto una soglia di ricavo ricorrente → 49 → 149 → 199+ €/mese, a scaglioni di ricavo
ricorrente tracciato». **Raccomando di abbandonare gli scaglioni sul ricavo**, per due ragioni indipendenti.

1. **Il numero non è verificabile da noi.** Il ricavo ricorrente del cliente non ci passa davanti: lo conosceremmo
   solo perché il cliente lo dichiara, o perché lo desumiamo dai segnali di SubGrove — che però non è
   necessariamente collegata, e che comunque copre solo la parte ad abbonamento del suo giro d'affari. Un prezzo che
   si può ridurre dichiarando meno, o non collegando una fonte, non è un prezzo: è un invito. È lo stesso
   ragionamento con cui SubGrove ha eliminato la propria componente a percentuale.
2. **Legherebbe il prezzo dell'app a un dato che l'app non ha il diritto di pretendere.** RenewGrove funziona anche
   con la sola BillGrove collegata, anche solo con l'importazione da foglio di calcolo. Chiedere il giro d'affari per
   stabilire il prezzo trasformerebbe un'informazione facoltativa in un adempimento.

Dove finisce allora la dimensione «quanto sei grande»? Nel **limite**: chi ha più clienti da sorvegliare sta su un
piano più alto. Stesso effetto economico, ottenuto con lo strumento che la piattaforma sa maneggiare, e con un conto
che il cliente può prevedere all'euro.

### 5.2 Ragionamento sui numeri

Tre riferimenti. **In alto**, il mercato rilevato: 200-250 $/mese di prezzo d'ingresso per Churnkey e ProsperStack,
75 $ + 129 $ per Baremetrics con il modulo di recupero (§2.1). È un ordine di grandezza fuori scala per una
micro-impresa europea, e va letto per quello che è: sono prodotti venduti ad aziende di software americane.
**In basso**, l'ancoraggio interno: SubGrove è proposta a 24 e 49 €/mese, InsightGrove a 19 e 39. Un cliente che
compra SubGrove **e** RenewGrove arriva a una cinquantina di euro al mese: è già una decisione vera per un'attività
da cinque persone, e il listino deve tenerne conto. **In mezzo**, il dato di posizionamento più interessante che ho
trovato: Baremetrics prezza il modulo «agisci» (129 $) **più caro** del piano «misura» (75 $). Dice che, quando il
valore è denaro non perso, il cliente paga di più. Lo prendo come argomento per stare **sopra** InsightGrove e
**vicino** a SubGrove, non come licenza per triplicare.

| Piano | Prezzo mensile | Prezzo annuale | Limite su `rapporti_sorvegliati` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | 15 | — | Il professionista con una manciata di clienti fissi. Serve soprattutto a **verificare che i segnali arrivino davvero** dalle app che ha già: senza quello, nessuno crederà al punteggio |
| `cura` | **29 €** | **290 €** (= 10× il mensile, «due mesi in regalo») | 250 | 14 giorni | Lo studio, il service, il manutentore, l'agenzia: il grosso del mercato, quello che oggi tiene i clienti a memoria |
| `portafoglio` | **59 €** | **590 €** | 1.200 | 14 giorni | La piccola impresa strutturata, con una persona che si occupa davvero della relazione con i clienti |

**Note obbligate.**

- **Tre piani, non di più**: aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile. La scheda ne
  proponeva quattro (49 / 149 / 199+): la fascia alta serviva a catturare il ricavo ricorrente grande, dimensione che
  §5.1 elimina.
- **Un limite lasciato vuoto significa illimitato, non zero.** Qui nessuno è vuoto: il tetto di 1.200 sul piano
  `portafoglio` è voluto — un'attività che sorveglia più di milleduecento rapporti non è più il cliente per cui
  questa app è disegnata, e merita una conversazione, non un'attivazione silenziosa.
- **Prova gratuita e piano gratuito insieme: qui non sono ridondanti**, ma per una ragione specifica di questa app.
  Il piano `free` è un posto **dove restare** e serve a rispondere alla domanda «i segnali arrivano?»; la prova di 14
  giorni serve a rispondere alla domanda diversa «il punteggio azzecca?», che richiede di sorvegliare parecchi
  clienti veri. Resta l'obiezione consueta — il piano gratuito attira il segmento che non converte — e la lascio
  scritta perché la decisione non è mia (punto aperto n. 3).
- ⚠️ **Quattordici giorni non bastano a dimostrare il valore di questo prodotto, e va detto.** L'argomento di vendita
  di RenewGrove è l'epica 05: «gli interventi che hai fatto hanno funzionato». Quella misura richiede una finestra di
  osservazione di **mesi**, non di giorni. Al quattordicesimo giorno il cliente vede punteggi e interventi preparati,
  non risultati. Non ho una soluzione da proporre che non sia una decisione commerciale (prova più lunga? un
  rendiconto anticipato basato sui primi esiti? l'accettazione che il primo mese si venda sulla promessa?): la
  registro come **punto aperto n. 3**, ed è il rischio commerciale numero uno del prodotto.
- **Costo effettivo dell'incasso** ([docs/09-pagamenti.md](../../../09-pagamenti.md) dec. 46-49): con una commissione
  dell'ordine del 5% più mezzo dollaro a transazione, su `cura` mensile a 29 € la commissione effettiva sta attorno al
  **6-7%**; sull'annuale a 290 € scende attorno al 5%. Nessun piano proposto sta sotto i 5 €/mese, quindi non scatta
  l'avviso morbido. L'annuale va messo in evidenza.
- **Prezzi immutabili una volta vivi**: un cambio di prezzo si fa creando un prezzo nuovo e archiviando il vecchio,
  mai modificando quello esistente.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/fidelizzazione.yaml`) si compila **insieme** allo sviluppatore: «niente contratto,
> niente produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

> 🛑 **Attenzione — questa app fa profilazione, ed è la sua funzione principale, non un effetto collaterale.**
> Il punteggio di rischio è «trattamento automatizzato di dati personali per valutare aspetti personali di una
> persona fisica, in particolare per prevederne il comportamento»: è la definizione di profilazione dell'articolo 4,
> punto 4 del regolamento europeo. E c'è di più: secondo la sentenza della Corte di giustizia dell'Unione europea del
> **7 dicembre 2023, causa C-634/21**, un punteggio calcolato in automatico diventa una **decisione automatizzata ai
> sensi dell'articolo 22** quando determina «in modo decisivo» la conclusione, l'esecuzione o la cessazione di un
> rapporto contrattuale — **anche se chi calcola e chi decide sono soggetti diversi** (§2.3). Qui chi calcola siamo
> noi e chi decide è il nostro cliente: è esattamente lo schema esaminato dalla Corte.
>
> **Presidi proposti, che valgono come requisiti e non come raccomandazioni** — ed è il motivo per cui l'epica 03 è
> scritta come è scritta:
> 1. **il punteggio non produce da solo alcun effetto**: nessun messaggio, nessuno sconto, nessuna chiusura di
>    rapporto parte senza che una persona lo confermi (storie `0017` e `0019`, con prova che lo dimostri);
> 2. **la logica è leggibile per costruzione**: un modello a **regole dichiarate con pesi visibili**, non un modello
>    addestrato di cui nessuno sappia dire perché ha detto quel numero (`0012`). È una limitazione di prodotto
>    accettata consapevolmente in cambio della spiegabilità;
> 3. **ogni punteggio si apre** e mostra quali fatti lo hanno formato, con quanto peso e in che verso (`0014`);
> 4. **ogni punteggio si contraddice**: chi legge può marcare un fatto come non pertinente, o escludere un rapporto
>    dalla sorveglianza, e resta traccia di chi l'ha fatto e perché (`0015`);
> 5. **il cliente finale ha diritto a sapere**: la spiegazione del punteggio è parte dell'esportazione dei dati
>    dell'interessato (`0032`), non un segreto industriale.
>
> ⚠️ **Quello che questi presidi NON risolvono, e che non spetta a un agente decidere**: la **base giuridica** del
> trattamento e il contenuto dell'informativa che il *nostro cliente* deve dare ai *suoi* clienti. Il legittimo
> interesse è la candidatura naturale e richiede una valutazione di bilanciamento che nessun agente può scrivere.
> Punto aperto n. 4, per la revisione legale.

> 🛑 **Categorie particolari (articolo 9): non ce ne sono, e c'è un presidio preciso che le tiene fuori.**
> Come progettata, RenewGrove non tratta dati sulla salute, biometrici, genetici, né opinioni, convinzioni,
> orientamento o appartenenza sindacale. La porta da cui entrerebbero è una sola ed è nota: **il testo libero**. Per
> questo il contratto del segnale (`0006`) **vieta qualunque campo di testo libero**, in ingresso da qualunque fonte,
> con un validatore che rifiuta e dice quale regola è stata violata. I due punti in cui un testo libero **esiste**
> dentro l'app — il motivo di una contestazione (`0015`) e la nota su un intervento (`0019`) — sono scritti da un
> **nostro utente**, non importati, e portano l'avvertenza esplicita di non inserire dati sulla salute. È lo stesso
> presidio scelto da SubGrove, ed è **contrattuale, non tecnico**: non esiste un rilevamento automatico del contenuto
> e **non lo invento**, sarebbe un presidio finto.
>
> Un terzo campo libero sarebbe stato naturale e **l'ho deliberatamente negato**: la voce «altro» dell'elenco dei
> motivi di abbandono (`0026`) **non** ha una nota libera. È il punto in cui qualcuno scriverebbe «si è ammalato»,
> su una popolazione di interessati che non ha alcun rapporto con noi e a cui nessuno chiederà mai niente. Il costo
> di questa scelta è reale — si perde informazione qualitativa proprio sui casi più interessanti — e va confermato
> (punto aperto n. 12).

**Chi sono gli interessati.** Due popolazioni distinte. Gli **utenti del cliente** (chi guarda i punteggi, chi
conferma gli interventi), già trattati dalla piattaforma. E i **clienti del nostro cliente** — le persone e le
aziende sorvegliate — che non hanno alcun rapporto con appgrove e non sanno che esistiamo. È la seconda che questo
manifesto deve coprire, ed è anche quella su cui si formula il giudizio.

**Categorie trattate**

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `rapporto.etichetta` | `rapporto.etichetta` | cliente del cliente | anagrafico (di norma un nome o una ragione sociale) | rendere leggibile a una persona l'elenco dei rapporti a rischio: senza, l'app mostra codici e nessuno la usa | legittimo interesse del titolare (il nostro cliente) a governare i propri rapporti commerciali — **da confermare in revisione legale** | finché il rapporto è sorvegliato; cancellazione fisica entro 30 giorni dall'archiviazione o dalla revoca della fonte |
| `rapporto.riferimento_origine` | `rapporto.app_origine`, `rapporto.riferimento` | cliente del cliente | identificativo opaco | ricollegare i segnali allo stesso soggetto e aprire la riga d'origine nell'app che la possiede | come sopra | come sopra |
| `segnale.fatto` | `segnale` (tipo, momento, intensità) | cliente del cliente | comportamentale/economico | comporre il punteggio e spiegarlo | come sopra | finestra di osservazione del modello + storico per la misura di efficacia; predefinito proposto **24 mesi** |
| `punteggio.valore_e_contributi` | `punteggio`, `contributo_punteggio` | cliente del cliente | **previsione — profilazione** | dire chi si rischia di perdere e perché | legittimo interesse, **con i presidi dell'avviso qui sopra** | serie storica per 24 mesi, mai riscritta all'indietro |
| `intervento.contenuto_e_stato` | `intervento` | cliente del cliente | comportamentale + prova | sapere che cosa è stato proposto, da chi confermato, con che esito | esecuzione del rapporto commerciale fra il cliente e il suo cliente | 24 mesi, con la prova di chi ha confermato |
| `esito_del_rapporto` | `esito_del_rapporto` | cliente del cliente | comportamentale | misurare se l'intervento è servito | legittimo interesse | 24 mesi |
| `contestazione.autore_e_motivo` | `contestazione` | **utente del cliente** | prova | dimostrare che il punteggio è stato messo in discussione e da chi | esecuzione del contratto con il nostro cliente | come lo storico del rapporto |

**Cosa NON si conserva, e va scritto nel manifesto come esclusione esplicita**: recapiti del cliente finale (posta
elettronica, telefono) nella via A del §4.3 — è il punto che cambierebbe se si scegliesse la via B; indirizzi;
identificativi fiscali; importi delle fatture in chiaro (il segnale porta un'**intensità**, non l'importo del
documento); contenuti di documenti; testo libero importato da qualunque fonte.

**Esportazione e cancellazione.** Devono comparire **tutte** in `exportData` e in `purgeData` del contratto dati
(`FidelizzazioneDataContract`): `rapporto`, `segnale`, `punteggio`, `contributo_punteggio`, `intervento`,
`offerta_di_trattenuta`, `esito_del_rapporto`, `contestazione`, `correzione_motivo` (storia `0026`) e
`bozza_di_strumento` (storia `0029` — conserva il contenuto proposto verso un cliente finale, ed è la tabella che
si dimentica più facilmente perché nasce dentro l'epica conversazionale). **Questo elenco è vivo**: ogni storia che
crea una tabella con dati riferiti a persone la aggiunge qui e nel contratto, e la storia `0032` porta la verifica
eseguibile che nessuna sia rimasta indietro. Fuori restano `fonte`, `modello_di_punteggio`, `piano_di_intervento`,
`coorte_di_confronto` e `rendiconto_efficacia`, che non contengono dati riferiti a clienti finali — **con la stessa
eccezione già notata da
SubGrove**: se un cliente battezzasse un piano d'intervento con il nome di una persona («piano Mario Rossi»), un dato
personale finirebbe in una tabella non esportata. Presidio: avviso a schermo (`0018`); se basti è il punto aperto
n. 8. La cancellazione è **fisica** e lascia una riga di prova nel registro delle purghe: sostituire i nomi con dei
codici non è cancellare.

⚠️ **Una richiesta di cancellazione qui ha un effetto in più** rispetto alle altre app, e va progettato: cancellare
un rapporto rimuove anche i suoi esiti, e quindi **cambia il rendiconto di efficacia dell'epica 05**. La scelta
proposta (`0032`) è che il rendiconto conservi solo **conteggi aggregati già calcolati**, senza righe riconducibili a
persone, così che una cancellazione non riscriva la storia della misura. Va confermata.

**Integrazioni esterne.** Nella via A del §4.3, **nessuna**: tutte le fonti sono applicazioni della suite e nessun
messaggio esce da qui. È una proprietà del disegno, non una coincidenza, e se si scegliesse la via B andrebbe
riscritta questa riga insieme al §2.4.

**Classificazione della change.** Una app nuova che introduce **profilazione** su una popolazione di interessati che
non ha rapporti con noi è un cambiamento **sostanziale**, con aggiornamento dell'informativa, del registro dei
trattamenti e — molto probabilmente — una **valutazione d'impatto sulla protezione dei dati**, che il regolamento
richiede per le valutazioni sistematiche di aspetti personali basate su trattamento automatizzato. Non la classifico
io: la segnalo, ed è parte del punto aperto n. 4. La classificazione descrive la realtà, non è una leva per evitare
adempimenti.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, use case 0061-0066, scritti e non implementati):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `elenca_rapporti_a_rischio` | `(fascia?, fonte?, entro_giorni?) → elenco minimizzato con punteggio e fascia` | Chi si rischia di perdere, in ordine di rischio | lettura | no |
| `spiega_punteggio` | `(rapporto) → valore, fascia, contributi con peso e verso, che cosa lo farebbe scendere` | **La ragione per cui il punteggio è credibile**: apre il giudizio | lettura | no |
| `stato_rapporto` | `(rapporto) → scheda minimizzata: anzianità, segnali recenti, interventi, esito` | Tutto su un rapporto senza aprire l'interfaccia | lettura | no |
| `salute_delle_fonti` | `() → per fonte: stato, ultimo segnale, ritardo` | «Perché non vedo più niente su questo cliente?» | lettura | no |
| `efficacia_degli_interventi` | `(periodo, tipo?) → trattenuti, persi, confronto con il gruppo di riferimento, costo delle concessioni` | Se il lavoro fatto è servito | lettura | no |
| `marca_segnale_non_pertinente` | `(segnale, motivo) → bozza con il punteggio ricalcolato` | Contraddice il giudizio; cambia un numero, non tocca nessuno all'esterno | scrittura | **sì** |
| `escludi_rapporto` | `(rapporto, motivo) → bozza` | Toglie un cliente dalla sorveglianza e libera quota | scrittura | **sì** |
| `prepara_intervento` | `(rapporto, piano?) → bozza dell'intervento, non consegnata` | Prepara e basta: **la bozza non esce** | scrittura | **sì** |
| `conferma_intervento` | `(intervento) → consegna all'app proprietaria o alla lista di lavoro` | **Fa uscire qualcosa verso una persona che non è nostro utente** | scrittura irreversibile | **sì, obbligatoria** |
| `autorizza_offerta` | `(intervento, tipo, valore, validità) → bozza dell'offerta` | Impegna denaro del cliente verso un terzo | scrittura irreversibile | **sì, obbligatoria** |
| `collega_fonte` | `(app) → bozza con l'elenco dei tipi di segnale che entreranno` | Apre un flusso di dati riferiti a persone | scrittura irreversibile | **sì, obbligatoria** |
| `scollega_fonte` | `(app) → bozza che dichiara quanti segnali verranno cancellati` | **Cancella** fisicamente i segnali di quella fonte | scrittura irreversibile | **sì, obbligatoria** |

**Lettura.** Gli strumenti che rendono questa app più utile dalla chat sono i primi due, e il secondo più del primo.
La domanda che il titolare fa davvero non è «chi rischio di perdere» — quella la mostra già un elenco — ma
**«perché dici così?»**. Un punteggio che risponde a quella domanda in linguaggio naturale, citando fatti datati, è
l'unico modo perché qualcuno se ne fidi abbastanza da agire. Sul versante della scrittura, la simmetria è netta e
voluta: **preparare** un intervento è una scrittura ordinaria, **farlo uscire** è irreversibile e non si concede mai
senza una persona. La regola vale a maggior ragione dalla chat, che è la superficie in cui è più facile confondere
«scrivimi una bozza» con «mandagliela».

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine dell'epica l'app esiste, si avvia in locale, compare nella barra laterale di chi ha l'abilitazione, ha il
suo schema vuoto e sa dire «no» quando la quota è finita.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | Istanza di scaffolding, rotte `/api/fidelizzazione/v1/*`, definizione delle interfacce, infrastruttura dal modulo comune |
| [0002](01-fondamenta/0002-modello-dati-multi-account.md) | Modello dati multi-account | Schema `app_fidelizzazione`, prime migrazioni, `tenant_id` e colonne di controllo ovunque |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Manifesto, registrazione, sezioni, cinque lingue, colore-categoria `teal` |
| [0004](01-fondamenta/0004-abbonamento-e-quota.md) | Abbonamento e quota | Metrica `rapporti_sorvegliati` a giacenza, varco a `429`, riduzione di piano sbarrata |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | `./dev.sh services` la vede; dati inventati con segnali, punteggi e interventi in tutti gli stati |

### Epica 02 — Arrivo dei segnali dalle altre app

Alla fine dell'epica i fatti che le altre applicazioni producono entrano qui **senza che nessuno legga i loro dati**,
si aggregano su un rapporto e si vede quando smettono di arrivare.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-arrivo-dei-segnali/0006-contratto-del-segnale.md) | Contratto del segnale di relazione | L'elenco chiuso dei tipi, il divieto di testo libero, il validatore che rifiuta e dice perché |
| [0007](02-arrivo-dei-segnali/0007-ricezione-e-scrittura-dei-segnali.md) | Ricezione e scrittura dei segnali | Il `tenant_id` si copia dall'evento, mai si deduce; percorsi di lettura e scrittura separati |
| [0008](02-arrivo-dei-segnali/0008-collegamento-e-revoca-di-una-fonte.md) | Collegamento e revoca di una fonte | Nessun collegamento al buio; la revoca cancella e lo dice prima |
| [0009](02-arrivo-dei-segnali/0009-il-rapporto-sorvegliato.md) | Il rapporto sorvegliato | I segnali si aggregano su un soggetto; l'etichetta arriva su un evento separato; è qui che si consuma la quota |
| [0010](02-arrivo-dei-segnali/0010-segnali-a-mano-e-da-file.md) | Segnali a mano e da file | Il caso «non ho l'app sorgente»: elenco importato e fatti segnati a mano, con le stesse regole |
| [0011](02-arrivo-dei-segnali/0011-salute-e-ritardo-delle-fonti.md) | Salute e ritardo delle fonti | Un segnale che non arriva è un rischio non visto: il silenzio non è salute |

### Epica 03 — Punteggio di rischio spiegabile e contestabile

Alla fine dell'epica esiste un numero che dice chi si rischia di perdere, **e chi lo legge sa da quali fatti nasce,
può dire che uno di quei fatti non c'entra, e sa che quel numero non farà partire nulla da solo**.

| # | Storia | In una riga |
|---|---|---|
| [0012](03-punteggio-di-rischio/0012-modello-del-punteggio.md) | Modello del punteggio | Regole dichiarate con pesi visibili, non un modello addestrato: la spiegabilità viene prima della precisione |
| [0013](03-punteggio-di-rischio/0013-calcolo-e-storico-del-punteggio.md) | Calcolo e storico del punteggio | Si ricalcola quando arrivano fatti nuovi e si conserva la serie, mai riscritta all'indietro |
| [0014](03-punteggio-di-rischio/0014-spiegazione-del-punteggio.md) | Spiegazione del punteggio | Quali fatti, quanto pesano, in che verso, e che cosa lo farebbe scendere |
| [0015](03-punteggio-di-rischio/0015-contestazione-del-punteggio.md) | Contestazione del punteggio | «Questo fatto non è pertinente»: si marca, il punteggio si rifà, resta traccia di chi e perché |
| [0016](03-punteggio-di-rischio/0016-taratura-per-attivita.md) | Taratura per attività | I pesi di partenza sono una convenzione dichiarata: il cliente li cambia e vede l'effetto prima di applicarli |
| [0017](03-punteggio-di-rischio/0017-il-punteggio-non-decide-da-solo.md) | Il punteggio non decide da solo | I presidi sulla decisione automatizzata, con una prova che nessun effetto parte senza una persona |

### Epica 04 — Interventi con conferma umana

Alla fine dell'epica si può **fare qualcosa** per un cliente a rischio, e niente esce verso di lui senza che una
persona l'abbia guardato e confermato.

| # | Storia | In una riga |
|---|---|---|
| [0018](04-interventi/0018-piani-di-intervento.md) | Piani di intervento | I modelli di azione consigliata per fascia di rischio, scritti dal cliente con le sue parole |
| [0019](04-interventi/0019-intervento-con-conferma-umana.md) | Intervento con conferma umana | Bozza, conferma esplicita, esito: da bozza non si esce senza una persona, e c'è una prova che lo dimostra |
| [0020](04-interventi/0020-consegna-all-app-proprietaria.md) | Consegna all'app proprietaria della relazione | L'intervento confermato esce come richiesta di comunicazione; il recapito resta a chi lo possiede |
| [0021](04-interventi/0021-lista-di-lavoro.md) | Lista di lavoro per la persona | Quando nessuna app può inviare: chi chiamare, perché, che cosa dire, com'è andata |
| [0022](04-interventi/0022-offerte-di-trattenuta-e-loro-limiti.md) | Offerte di trattenuta e loro limiti | Sconto o proroga con autorizzazione e tetto — e il divieto di frapporsi al percorso di disdetta |
| [0023](04-interventi/0023-freni-al-contatto.md) | Freni al contatto | Niente doppio contatto, tetto di frequenza, silenzio per rapporto: i presidi contro il fastidio |

### Epica 05 — Misura dell'efficacia

Alla fine dell'epica il prodotto sa dimostrare — o smentire — il proprio valore, che è il suo unico vero argomento di
vendita.

| # | Storia | In una riga |
|---|---|---|
| [0024](05-misura-dell-efficacia/0024-esito-del-rapporto.md) | Esito del rapporto | Trattenuto, perso o ancora aperto, valutato dopo una finestra dichiarata prima |
| [0025](05-misura-dell-efficacia/0025-gruppo-di-confronto.md) | Gruppo di confronto | Senza un termine di paragone non si misura nulla: come si forma, e i suoi limiti onesti |
| [0026](05-misura-dell-efficacia/0026-motivi-di-abbandono.md) | Motivi di abbandono | Perché se ne sono andati, raccolto in un elenco corto e nelle parole di chi lo registra |
| [0027](05-misura-dell-efficacia/0027-rendiconto-dell-efficacia.md) | Rendiconto dell'efficacia | Quanto ha funzionato, quanto è costato in concessioni, e che cosa il numero **non** dimostra |

### Epica 06 — Esposizione conversazionale e prove end-to-end

Alla fine dell'epica l'app è comandabile da una chat con la regola «lettura libera, scrittura con conferma», e le due
cose che possono andare storte — un effetto che parte da solo, un dato che passa da un account all'altro — sono
sorvegliate da prove vere.

| # | Storia | In una riga |
|---|---|---|
| [0028](06-esposizione-conversazionale-e-prove/0028-strumenti-di-lettura.md) | Strumenti di lettura | I cinque strumenti che rispondono alle domande sul rischio, dati minimizzati, spiegazione compresa |
| [0029](06-esposizione-conversazionale-e-prove/0029-strumenti-di-scrittura-con-conferma.md) | Strumenti di scrittura con conferma | Sette strumenti che scrivono, tutti con bozza; quattro con conferma obbligatoria |
| [0030](06-esposizione-conversazionale-e-prove/0030-percorso-end-to-end.md) | Percorso end-to-end dell'app | Dal segnale all'esito: il percorso `[J-FIDELIZZAZIONE]` sullo stack locale reale |
| [0031](06-esposizione-conversazionale-e-prove/0031-prove-di-non-aggiramento.md) | Prove di non-aggiramento dell'isolamento | Nessuna chiamata verso altre app, nessuna lettura fra schemi, nessun effetto senza persona |
| [0032](06-esposizione-conversazionale-e-prove/0032-chiusura-del-contratto-dati.md) | Chiusura del contratto dati | Esportazione e cancellazione su tutte le tabelle, spiegazione del punteggio compresa |

**Totale**: 6 epiche, 32 storie.

---

## 9. Estensioni della console di amministrazione

Servono estensioni, poche e tutte di **diagnosi**. Due ragioni specifiche di questa app: è alimentata **solo** da
eventi di altre applicazioni, quindi un flusso che si interrompe la rende silenziosamente inutile senza che nulla
diventi rosso; e produce **giudizi su persone**, quindi la piattaforma deve poter rispondere a una richiesta di
spiegazione o a un reclamo sapendo *quale versione del modello* era viva quel giorno — senza mai guardare i
contenuti dell'account.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| **19 SubGrove** (`abbonati`) | **dipende da / confina con** | Il confine è il §0 e va letto prima di ogni altra cosa. SubGrove è la fonte di segnali più ricca (rate fallite, disdette, sospensioni) e la destinataria naturale delle richieste di comunicazione (`0020`). **RenewGrove non genera scadenze, avvisi di rinnovo né solleciti**: mai |
| **02 BillGrove** (fatturazione) e l'app reale `fatture` | **dipende da** | È la fonte che rende RenewGrove utile a chi **non ha abbonamenti**: contratti rifatturati, ritardi, documenti contestati |
| **12 DeskGrove** (assistenza) | **dipende da** | Il secondo segnale per forza predittiva dopo il pagamento: segnalazioni aperte, riaperte, chiuse male |
| **04 LeadGrove** (vendite) e il mini-gestionale dei contatti | **condivide dati con** | L'anagrafica clienti condivisa del §6 del catalogo: il rapporto sorvegliato *è* un cliente. RenewGrove tiene un **riferimento**, non una copia autorevole |
| **20 InsightGrove** (analitiche) | **fratello / alimenta** | Stesso problema tecnico, stessa soluzione (§4.2): sono le due app della suite che vivono di dati altrui. InsightGrove **aggrega**, RenewGrove **individua**: è la differenza che cambia il regime dei dati personali. Gli esiti dell'epica 05 sono materia prima naturale per il cruscotto trasversale |
| **03 CashGrove** (incasso crediti) | **confina con** | Un cliente che non paga è insieme un credito da incassare e un rapporto a rischio. Confine: CashGrove insegue **il denaro**, RenewGrove **la relazione**. Regola operativa nella storia `0023`: due app non contattano lo stesso cliente lo stesso giorno |
| **07 BookGrove** (prenotazioni) | **dipende da** (facoltativo) | Disdette e mancate presentazioni sono segnali forti per chi vende appuntamenti |

**Riga di lettura.** RenewGrove **non ha senso da sola**, e questo va detto chiaramente perché è diverso da tutte le
sorelle: senza almeno una fonte collegata è un contenitore vuoto. La storia `0010` (importazione da file e segnali a
mano) esiste per abbassare quella soglia, non per annullarla. È un'app della suite, e va venduta come tale.

**Sovrapposizioni da evitare.** Tre, e sono già state affrontate sopra:

1. **Con SubGrove** — la più grande e la più costosa se sbagliata. Confine al §0, e il divieto scritto di generare
   scadenze, avvisi di rinnovo e solleciti di pagamento.
2. **Con CashGrove** — due app che contattano lo stesso cliente sullo stesso ritardo. Presidio nella storia `0023`.
3. **Con InsightGrove** — entrambe mostrano numeri sulla clientela. Confine: InsightGrove risponde a *«come sta
   andando l'attività»* con aggregati; RenewGrove risponde a *«chi sto per perdere»* con nomi. Chi vuole il primo non
   deve comprare la seconda.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **RenewGrove resta un prodotto separato o diventa l'epica 08 di SubGrove?** (§0.3) | È direzione di prodotto pura. La raccomandazione è «separata, con il perimetro stretto del §0.3», ma l'alternativa è legittima e costa meno | **sviluppatore** — direzione di prodotto |
| 2 | **Il contratto dell'evento di richiesta di comunicazione** (§4.3, storia `0020`) | Non esiste nel repository e riguarda tutte le applicazioni destinatarie, non solo questa. È la gemella del punto sollevato da InsightGrove sul contratto del fatto di misura | **piattaforma** (sviluppatore), prima della storia `0020` |
| 3 | **Prezzi, e soprattutto la prova gratuita di 14 giorni** (§5.2) | Il valore di questa app si dimostra su mesi, non su giorni: al quattordicesimo giorno il cliente vede lavoro preparato, non risultati. Serve una risposta commerciale che un agente non può dare | **sviluppatore** — prezzi, fermata di escalation |
| 4 | **Base giuridica della profilazione, informativa al cliente finale, valutazione d'impatto** (§6) | Il legittimo interesse è la candidatura naturale ma richiede una valutazione di bilanciamento; e una profilazione sistematica su larga scala richiede probabilmente una valutazione d'impatto | **sviluppatore** + **revisione legale** ([docs/_REVISIONE-LEGALE.md](../../../_REVISIONE-LEGALE.md)) |
| 5 | **È ammesso frapporre un'offerta di trattenuta al percorso di disdetta?** (§2.3, §2.7, storia `0022`) | La fonte sul § 312k tedesco non lo affronta e non ho trovato una risposta per l'Italia. La storia adotta intanto la postura più prudente | **revisione legale** |
| 6 | **Il punteggio ricade nel regolamento europeo sull'intelligenza artificiale?** (§2.3 punto 4) | Non ho verificato l'allegato applicabile e non invento la risposta | **revisione legale** |
| 7 | **Colore-categoria `teal`, già proposto da 02 BillGrove e 12 DeskGrove** (§3) | Sei colori per sessanta app: la collisione è strutturale. Qui è aggravata dal fatto che tre colori sono esclusi per principio | **piattaforma**, quando i colori si assegnano davvero |
| 8 | **Nome di un piano di intervento come possibile dato personale** (§6) | Stessa questione già sollevata da SubGrove sui nomi dei piani: l'avviso a schermo basta? | **sviluppatore** — classificazione dati personali |
| 9 | **La finestra di conservazione di 24 mesi** su segnali, punteggi ed esiti (§6) | È una proposta prudente, non un dato: dipende dalla base giuridica scelta al punto 4 e dalla durata tipica dei rapporti nel settore del cliente | **revisione legale** |
| 10 | **Identità del rapporto quando lo stesso cliente arriva da due fonti** (§4.1) | Lo stesso cliente in SubGrove e in BillGrove genera oggi due rapporti e consuma due unità di quota. La via raccomandata — chiave unica dall'anagrafica clienti condivisa — dipende da quanto quell'anagrafica è autorevole, che è materia di **04 LeadGrove** e del §6 del catalogo, non di questa app | **piattaforma** (sviluppatore), prima della storia `0009` |
| 11 | **La consegna dell'intervento all'app proprietaria genera un secondo contatto?** (storie `0020`, `0023`) | Se SubGrove sta già sollecitando quella stessa scadenza, la richiesta di comunicazione di RenewGrove arriva sopra la sua. Il freno della storia `0023` presuppone che le app dichiarino i propri contatti su un canale comune, che non esiste | **piattaforma**, insieme al punto 2 |

**Rischi noti**

- **Costruire due volte SubGrove** — il rischio numero uno, e la ragione per cui il §0 sta in cima al documento
  invece che in fondo. Se si avvera, il marketplace vende due prodotti che generano due calendari e due catene di
  solleciti sugli stessi contratti. *Attenuazione*: perimetro dichiarato al §0.3, divieto scritto nelle storie
  `0018`-`0023`, e il fatto che l'app non abbia alcuna entità «scadenza».
- **Il punteggio non azzecca, e la fiducia crolla** — è il rischio che la scheda di catalogo stessa segnala
  («*abbandona se l'health score non predice il churn*»). Aggravato dal fatto onestamente dichiarato al §2.7: **non
  esistono pesi validati per imprese non-software**. *Attenuazione*: pesi modificabili (`0016`), spiegazione sempre
  disponibile (`0014`), contestazione (`0015`) e — soprattutto — l'epica 05, che rende il fallimento **visibile e
  discutibile** invece che silenzioso. Un prodotto che dice «i tuoi interventi non hanno cambiato nulla» è più
  difendibile di uno che tace.
- **Un effetto che parte da solo** — il difetto più grave possibile in questa app: un messaggio a un cliente
  finale, mandato da un automatismo, a insaputa del titolare. *Attenuazione*: la macchina a stati del §4.4 non ha un
  passaggio automatico da `bozza`, la storia `0019` chiede una prova che lo dimostri, e la storia `0031` la ripete a
  livello di percorso end-to-end.
- **Il punteggio diventa una decisione automatizzata di fatto** — nessuno la chiama così, ma se il titolare smette
  di leggere e conferma tutto a occhi chiusi, l'intervento umano diventa una formalità. *Attenuazione parziale e
  dichiarata come tale*: la conferma mostra sempre i tre fatti principali che hanno formato il punteggio, così che
  confermare significhi almeno averli visti. **Non è una difesa completa e non fingo che lo sia**: la difesa completa
  è organizzativa, non tecnica.
- **Fonte che smette di pubblicare** — l'app continua a mostrare punteggi vecchi che sembrano attuali, ed è il modo
  più elegante di essere inutili. *Attenuazione*: storia `0011`, dove il ritardo di una fonte è un dato mostrato in
  faccia e un punteggio calcolato su una fonte in silenzio è marcato come tale.
- **RenewGrove venduta a chi non ha altre app** — cliente che paga e non vede nulla. *Attenuazione*: la storia `0010`
  e un piano gratuito la cui funzione dichiarata è verificare che i segnali arrivino (§5.2).

**Fuori dimensionamento**: nessuno. 6 epiche (fascia 4-7), da 4 a 6 storie per epica (fascia 4-8), 32 storie in tutto
(fascia 20-45).

**Nota sui titoli con «e»** ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §3, segnale «congiunzione nel titolo»). Sei
titoli ne contengono una: *Abbonamento e quota*, *Avvio locale e dati di prova*, *Ricezione e scrittura dei segnali*,
*Collegamento e revoca di una fonte*, *Segnali a mano e da file*, *Salute e ritardo delle fonti*. Il segnale della
guida individua le storie che fanno **due lavori diversi** («creazione **e** invio **e** promemoria»); qui la
congiunzione unisce le due facce **inseparabili** di una cosa sola — collegare senza poter revocare non è una
funzione, è una trappola; scrivere senza ricevere non esiste. Le ho lasciate, con la stessa convenzione già adottata
dalle app 19 e 20. La storia che avrebbe potuto davvero spezzarsi in due è `0008`, perché comprende anche il
ripopolamento dello storico (§4.2): se in implementazione risultasse troppo grande, si scorpora lì.
