# CashGrove — descrizione dell'applicazione

**Numero di catalogo**: 03 · **Tipo**: orizzontale · finanza d'impresa · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 3](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** CashGrove tiene l'elenco delle fatture emesse e non ancora incassate, sollecita chi non paga secondo una
sequenza decisa dal titolare (posta elettronica e, quando serve, messaggio breve o messaggistica), registra quello che
il debitore risponde — una promessa di pagamento, una contestazione, un pagamento parziale — e mostra quanto denaro è
fermo, da quanto tempo e quando è ragionevole aspettarsi che rientri. Calcola inoltre gli interessi di mora e il
rimborso forfettario di 40 euro previsti per le transazioni commerciali, e prepara la lettera di messa in mora.

**Per chi.** Micro-imprese da 1 a 10 addetti e piccole imprese fino a 50, che emettono fra qualche decina e qualche
centinaio di fatture l'anno verso altre imprese. Chi compra è il titolare, perché il problema è il suo conto corrente;
chi usa l'app tutti i giorni è la persona che tiene l'amministrazione — spesso una sola, spesso a tempo parziale, a
volte lo studio del commercialista. Mercato globale con priorità europea.

**Quale problema toglie.** «Ho fatturato, ma non ho incassato»: il denaro c'è sulla carta e non in banca. Oggi il
titolare se ne accorge a colpo d'occhio guardando l'estratto conto, poi apre un foglio di calcolo copiato a mano dal
programma di fatturazione, cerca chi è in ritardo, scrive una email a uno a uno, si dimentica di chi ha promesso di
pagare «entro venerdì» e ricomincia il mese dopo. Costa tre cose: tempo (il sollecito è un lavoro noioso che si
rimanda), soldi (il credito che invecchia si incassa peggio) e rapporti (il sollecito fatto a memoria arriva o troppo
tardi o al cliente sbagliato). I dati di mercato dicono che il problema è strutturale, non individuale: in Italia i
pagamenti puntuali sono scesi al 44,7% nel primo trimestre 2025 e le microimprese hanno una quota di insoluti doppia
rispetto alle grandi (§2.5).

**Cosa NON fa.**

- **Non emette fatture** e non le trasmette a nessuna autorità: quello è il perimetro delle app 1 e 2 del catalogo.
  CashGrove parte dalla fattura già emessa.
- **Non incassa denaro.** appgrove non tiene in nessun momento il denaro dei clienti dei nostri clienti: non è un
  istituto di pagamento e non vuole diventarlo. Il debitore paga sul conto del creditore, non sul nostro.
- **Non fa recupero crediti per conto terzi**: non c'è nessun operatore di appgrove che telefona ai debitori. L'app è
  uno strumento con cui il creditore sollecita **i propri** debitori (§2.3 — è una distinzione che ha effetti di legge).
- **Non fa azione legale**: prepara la bozza della messa in mora, non deposita ricorsi e non gestisce il decreto
  ingiuntivo.
- **Non fa valutazione del merito creditizio con dati esterni**: il punteggio di rischio nasce dal solo comportamento di
  pagamento osservato dentro l'account, non da banche dati o centrali rischi.
- **Non finanzia il credito** (nessuna cessione, nessuno sconto fattura).

**Rischio di sostituzione da parte dei modelli linguistici.** `rafforzata`. Un assistente generico sa scrivere un
sollecito garbato; non sa quali fatture sono scadute oggi, non ricorda che quel cliente aveva promesso di pagare
venerdì, non sospende la sequenza quando arriva una contestazione e non lascia la prova di che cosa è stato mandato e
quando. Il valore sta nel dato proprietario (lo stato del portafoglio crediti, aggiornato), nel flusso di lavoro
(sequenze, sospensioni, esiti) e nella conformità (interessi calcolati secondo la norma, registro degli invii, regole
di condotta verso il debitore). Il livello conversazionale rende l'app **più** utile, non superflua: «chi mi deve dei
soldi da più di 60 giorni?» è esattamente la domanda che il titolare non fa perché aprire il foglio di calcolo costa
troppo.

---

## 2. Mercato e analisi in rete

> Compilata dopo 12 azioni di ricerca (9 fonti citate qui sotto).
> Ciò che non è stato trovato è **dichiarato**, non colmato a intuito.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| Chaser | Regno Unito, vende in Europa | Solleciti automatici, valutazione del rischio, portale di pagamento, servizio di credit control in appalto | **€239/mese** (piano Compact, fatturato del cliente < €4 mln), €719 (Core), €1.079 (Complete); −10% sull'annuale — **pagina ufficiale** | https://www.chaserhq.com/pricing |
| Paidnice | Nuova Zelanda, vende in Europa | Solleciti, more e interessi, sconti per pagamento anticipato, piani di rientro, portale cliente; innestato su Xero e QuickBooks | **59 €/mese** (Essentials, 150 fatture e 600 email al mese), da 99 a 799 USD per fasce di fatture (Pro), 999 USD (Custom); messaggi brevi a consumo 0,05–0,15 USD — **pagina ufficiale** | https://www.paidnice.com/pricing |
| Kolleno | Regno Unito | Solleciti, incassi, riconciliazione, lavoro di squadra: rivolto a reparti amministrativi strutturati | **£650/utente/mese** riportato da un comparatore, **non verificato su pagina ufficiale** | https://trove.works/chaser-kolleno-pricing-comparison/ |
| Upflow | Francia/Stati Uniti | Solleciti automatici, assegnazione dei compiti, cruscotto, portale di pagamento; innestato su QuickBooks, Xero, NetSuite, Stripe | **Prezzo non pubblicato**: la pagina rimanda a una richiesta di dimostrazione | https://upflow.io/collection-software-small-business |
| Modular Software — «Recupero crediti» | Italia | Gestione dei solleciti in un gestionale italiano | **60 €/semestre** più eventuale attivazione, riportato dalla scheda prodotto raggiunta via ricerca, **non verificato leggendo direttamente la pagina** | https://modularsoftware.it/main.php?cod_prog=SVARC01&pagina=info2 |
| Fatture in Cloud (funzione inclusa) | Italia | Scadenzario e sollecito automatico **dentro** il programma di fatturazione | Prezzo non rilevato per la sola funzione: è compresa nel canone del gestionale | https://www.pmi.it/impresa/subcat/esperto/495134/sollecito-fatture-per-invio-pagamenti-con-fatture-in-cloud.html |

**Lettura.** Il mercato è spaccato in due, e in mezzo c'è un buco che è esattamente il nostro segmento. Da una parte i
prodotti anglosassoni specializzati (Chaser, Kolleno, Upflow) partono da 239 €/mese e sono tarati su un reparto
amministrativo: per una micro-impresa italiana che fattura 300.000 € l'anno sono fuori scala di un ordine di grandezza.
Dall'altra i gestionali italiani includono un sollecitatore rudimentale (manda una email al superamento della scadenza)
che non conosce sequenze, esiti, promesse, contestazioni né interessi di mora. Paidnice è l'unico prezzato dove
staremmo noi (59 €/mese), ma vive appeso a Xero e QuickBooks — praticamente assenti nelle micro-imprese italiane, che
usano Fatture in Cloud, Aruba, TeamSystem. Il posto libero è: **prezzo da micro-impresa, disciplina da prodotto
specializzato, ingresso dei dati indipendente dal gestionale** (importazione da file e inserimento a mano prima ancora
che innesti automatici).

### 2.2 Prezzi praticati nel dominio

- **Fascia alta specializzata** (rilevata su pagina ufficiale): Chaser 239–1.079 €/mese, con l'unità di misura più
  insolita del campione — **la fascia di fatturato del cliente**, non gli utenti né le fatture. Sconto del 10%
  sull'annuale (più prudente del «due mesi in regalo» che usa appgrove).
- **Fascia media** (rilevata su pagina ufficiale): Paidnice 59 €/mese fino a 150 fatture al mese, poi a scaglioni di
  volume fino a 799 USD. Unità di misura: **fatture gestite nel mese** e, in parallelo, un tetto di email. I messaggi
  brevi sono un supplemento **a consumo** (0,05–0,15 USD a messaggio).
- **Fascia enterprise**: Kolleno per utente, prezzo solo da comparatore; Upflow non pubblica nulla.
- **Piano gratuito**: nessuno dei prodotti esaminati ne pubblica uno vero. Paidnice offre «le prime 20 azioni»,
  Chaser una prova (durata non indicata in pagina; un comparatore parla di 10 giorni, non verificato).
- **Unità di misura prevalente**: il **volume di documenti** (fatture o solleciti). L'unità «per utente» compare solo
  nel segmento enterprise — coerente col fatto che nel nostro segmento l'utente è quasi sempre uno solo.
- **Sui prezzi tratti da comparatori vale l'avvertenza del catalogo (§8)**: invecchiano male e vanno riverificati sul
  sito del fornitore prima di fissare il posizionamento.

### 2.3 Obblighi normativi del settore

Il dominio è **normato più di quanto sembri**, e la normativa entra direttamente nel modello dati e nel modo in cui i
solleciti si mandano. Quattro corpi di regole:

1. **Ritardi di pagamento fra imprese (direttiva 2011/7/UE, in Italia decreto legislativo 231/2002).** Termine di
   pagamento massimo di 60 giorni fra imprese e 30 verso la pubblica amministrazione; interessi di mora **automatici**,
   pari al tasso di riferimento della Banca centrale europea maggiorato di **almeno 8 punti percentuali**; **importo
   forfettario minimo di 40 euro per ogni fattura pagata in ritardo**, oltre al rimborso delle spese di recupero
   effettivamente sostenute. Il tasso è pubblicato in Gazzetta Ufficiale ogni semestre: per il primo semestre 2026 è
   **10,15%** (2,15% + 8). Conseguenza per il prodotto: il tasso **non si scrive nel codice**, è un dato che cambia due
   volte l'anno e va tenuto come tabella storicizzata per semestre (storia 0020).
2. **Il regolamento europeo in arrivo.** La Commissione ha proposto (COM 2023/0533) di sostituire la direttiva con un
   **regolamento** direttamente applicabile, con termine unico di 30 giorni e autorità nazionali di vigilanza. **Non
   sono riuscito a determinare lo stato attuale dell'iter** (§2.7). Il catalogo avverte (§8) di non costruire un piano
   commerciale su una singola data normativa: qui l'avvertenza vale doppio, e la conseguenza di prodotto è tenere
   termini e maggiorazione come **parametri**, non come costanti.
3. **Chi può fare recupero crediti.** In Italia l'attività di **recupero stragiudiziale dei crediti per conto terzi**
   è soggetta a licenza del questore (articolo 115 del testo unico delle leggi di pubblica sicurezza). CashGrove è uno
   strumento con cui il creditore sollecita i **propri** debitori, e questo — nella lettura ordinaria della norma, che
   riserva la licenza a chi opera «per conto terzi» — resta fuori dalla riserva. **La fonte consultata non affronta
   però esplicitamente il caso del creditore che agisce in proprio** (§2.7) e, soprattutto, un compenso legato
   all'importo recuperato (§5) sposta il modello verso il terreno riservato: è un punto da legale, non da agente.
4. **Come si sollecita — regole di condotta e dati personali.** Il Garante per la protezione dei dati personali ha un
   vademecum specifico sul recupero crediti. Sono **illecite**: la comunicazione ingiustificata a terzi (familiari,
   conviventi, colleghi, vicini) della condizione di inadempienza; l'affissione di avvisi di mancato pagamento alla
   porta del debitore; le buste con la dicitura «recupero crediti» visibile; le telefonate ripetute o preregistrate e
   ogni contatto che leda la dignità. I dati trattabili sono **solo** quelli identificativi, il codice fiscale o la
   partita IVA, i recapiti, l'importo del debito e le condizioni di pagamento. Conseguenze dirette sul prodotto: il
   messaggio va **al debitore e a nessun altro** (niente copie di cortesia a terzi indirizzi), le finestre di invio
   vanno limitate a orari e giorni ragionevoli con un tetto alla frequenza, l'oggetto del messaggio non deve rivelare
   il contenuto a chi lo vede sullo schermo, e l'app non deve chiedere campi che il Garante non ammette. Sono i
   requisiti delle storie 0011, 0013 e 0014.

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta prevedibile:

1. **Programma di fatturazione / gestionale** — da lì arrivano le fatture e i pagamenti registrati. In Italia: Fatture
   in Cloud (che pubblica interfacce di programmazione e si innesta con QuickBooks, Xero, Zapier), TeamSystem, Aruba;
   fuori: Xero e QuickBooks, su cui vivono tutti i concorrenti esaminati. **Fornitore esterno**: sì, ma il dato viaggia
   *verso* di noi.
2. **Posta elettronica in uscita** — il sollecito deve partire dal dominio del creditore, altrimenti finisce nella
   posta indesiderata. **Fornitore esterno che tratta dati per nostro conto**: sì.
3. **Messaggi brevi e messaggistica istantanea** — il catalogo cita esplicitamente messaggio breve e WhatsApp. La
   piattaforma WhatsApp Business si paga **a messaggio** (categoria «utility», tariffa per Paese, con scaglioni di
   volume) e impone modelli approvati in anticipo: è insieme un costo variabile e un vincolo di prodotto.
   **Fornitore esterno che tratta dati per nostro conto**: sì. Vedi il punto aperto sul costo variabile (§11).
4. **Banca / incassi** — per sapere *davvero* se il debitore ha pagato servirebbe leggere i movimenti del conto
   (accesso ai conti di pagamento, o rendicontazione elettronica). È la sola strada per una riconciliazione automatica
   e per qualunque misurazione del «recuperato». **Fornitore esterno**: sì, con un livello di sensibilità superiore.
   Non è nell'ambito delle 31 storie: vedi §11.
5. **Fogli di calcolo ed esportazione per il commercialista** — non è un'integrazione, è un formato; ma è la richiesta
   che arriva per prima ed è a costo zero (storia 0027).
6. **Calendario** — per i richiami manuali e le promesse in scadenza. Marginale.

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Cosa chiedono, ricavato dalle fonti:

- **Che parta da solo.** L'aspettativa base è la sequenza automatica di solleciti con esiti tracciati e cruscotto:
  è la promessa comune di Upflow, Chaser e Paidnice.
- **Che si innesti sul gestionale che già usano.** Tutti i concorrenti esaminati vivono di questo. È anche il loro
  limite in Italia, dove i gestionali sono altri.
- **Che l'importo dovuto sia indiscutibile.** Interessi, more, pagamenti parziali, note di credito: se il numero nel
  sollecito è sbagliato, il sollecito si ritorce contro chi lo manda.
- **Che il tono sia governabile.** Il cliente moroso spesso è anche il cliente migliore: il titolare vuole decidere lui
  quando si passa dal «promemoria» al «diffido».

Cosa rifiutano:

- **Il prezzo da reparto amministrativo.** È la lamentela ricorrente rilevata sui prodotti di categoria: costosi per
  quello che fanno, con avviamenti lunghi e assistenza lenta.
- **La configurazione lunga.** Se prima di vedere un risultato bisogna collegare il gestionale, mappare i clienti e
  disegnare tre sequenze, il prodotto viene abbandonato. Da qui la scelta di far entrare i dati **anche** da file e a
  mano fin dalla prima epica di dominio (storie 0007 e 0008).
- **L'automatismo cieco.** Nessuno vuole che parta un sollecito verso il cliente che ha appena pagato o che sta
  contestando: la sospensione automatica (storia 0016) non è un dettaglio, è la condizione per fidarsi dell'automatismo.

**Contesto del problema, per dimensionare la promessa.** In Italia i pagamenti puntuali sono scesi al 44,7% nel primo
trimestre 2025, i ritardi oltre 90 giorni sono saliti al 4,9%, e le piccole e medie imprese registrano una quota di
fatture insolute più che doppia rispetto alle grandi (2,6% contro 1,2%). Due terzi degli imprenditori italiani ritengono
che il ritardo del debitore sia **intenzionale** (68%, contro una media europea del 50%): è un dettaglio che dice quale
tono ha il prodotto — non «ti sei dimenticato», ma «tengo il conto».

### 2.6 Fonti consultate

1. **Chaser — pagina prezzi ufficiale** — https://www.chaserhq.com/pricing — fascia alta del mercato specializzato e
   unità di misura insolita (fatturato del cliente): 239–1.079 €/mese, sconto del 10% sull'annuale.
2. **Paidnice — pagina prezzi ufficiale** — https://www.paidnice.com/pricing — l'unico concorrente prezzato nella
   nostra fascia (59 €/mese per 150 fatture al mese); conferma che l'unità di misura naturale è il volume di documenti
   e che i messaggi brevi restano un costo a consumo.
3. **Trove — confronto Chaser/Kolleno** — https://trove.works/chaser-kolleno-pricing-comparison/ — posizionamento di
   Kolleno (per utente, fascia enterprise) e conferma che nel segmento piccolo si compete su prezzo e semplicità.
   Comparatore, non fonte ufficiale.
4. **Upflow — pagina «collection software for small business»** — https://upflow.io/collection-software-small-business
   — elenco delle funzioni date per scontate (solleciti configurabili, assegnazione compiti, cruscotto, portale di
   pagamento) e degli innesti attesi sui gestionali; prezzo non pubblicato.
5. **EUR-Lex — sintesi della direttiva 2011/7/UE sui ritardi di pagamento** —
   https://eur-lex.europa.eu/IT/legal-content/summary/combating-late-payment-in-business-dealings.html — termini di 60
   e 30 giorni, interessi pari al tasso della Banca centrale europea più 8 punti, forfait minimo di 40 euro.
6. **Confartigianato — la proposta di trasformare la direttiva in regolamento** —
   https://www.confartigianato.it/2023/09/europa-ritardi-di-pagamento-lue-trasforma-la-direttiva-in-regolamento-con-termini-fissi-a-30-giorni/
   — termine unico a 30 giorni, interessi automatici, autorità nazionali di vigilanza; iter non concluso al momento
   della ricerca.
7. **Calcolatori fiscali — tasso di mora vigente** — https://www.calcolatorifiscali.it/strumenti/interessi-mora —
   valore corrente del tasso (10,15% per il primo semestre 2026, da comunicato in Gazzetta Ufficiale del 20 gennaio
   2026) e formula di calcolo giorno per giorno; prova che il tasso è un dato semestrale da storicizzare.
8. **Garante per la protezione dei dati personali — vademecum «Privacy e recupero crediti»** —
   https://www.garanteprivacy.it/documents/10160/0/Privacy+e+recupero+crediti+-+Il+Vademecum.pdf — quali dati si
   possono trattare e quali condotte sono illecite: è la fonte che detta i requisiti delle storie 0011, 0013 e 0014.
9. **CreditNews — chi può fare recupero crediti: articolo 115 del testo unico di pubblica sicurezza e articoli 106 e
   114 del testo unico bancario** — https://www.creditnews.it/gestione-e-recupero-crediti-cosa-possono-fare-e-cosa-no-115-tulps-106-tub-e-114-tub/
   — la licenza del questore riguarda chi recupera crediti **per conto terzi**; conferma indiretta che uno strumento
   usato dal creditore in proprio sta fuori dalla riserva, ma senza affrontare il caso esplicitamente.
10. **Intrum — European Payment Report 2025, edizione italiana** —
    https://www.intrum.it/aziende-e-istituzioni-finanziarie/report-e-approfondimenti/report/european-payment-report-25/
    — il 68% degli imprenditori italiani ritiene intenzionale il ritardo del debitore (media europea 50%).
11. **Credit System — dati sui ritardi di pagamento 2025** —
    https://credit-system.it/la-sfida-dei-pagamenti-in-italia-cosa-fare-per-tutelare-la-tua-azienda/ — pagamenti
    puntuali al 44,7% nel primo trimestre 2025, ritardi oltre 90 giorni al 4,9%, insoluti delle piccole imprese al 2,6%
    contro l'1,2% delle grandi. Rielaborazione di dati Assifact, non fonte primaria.
12. **Meta — documentazione sui prezzi della piattaforma WhatsApp Business** —
    https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing — i messaggi di servizio si
    pagano **per messaggio consegnato**, con tariffa per Paese e scaglioni di volume, e richiedono modelli approvati:
    fonte del vincolo economico descritto in §11.

### 2.7 Cosa NON sono riuscito a determinare

- **Stato dell'iter del regolamento europeo sui ritardi di pagamento (COM 2023/0533)** — le fonti trovate descrivono la
  proposta ma nessuna riporta lo stato al 2026. Servirebbe una verifica sull'osservatorio legislativo dell'Unione.
  Effetto: i termini di pagamento e la maggiorazione restano parametri configurabili, non costanti (storia 0020).
- **Prezzo praticato in Italia dai gestionali per la sola funzione di sollecito** — nessuno lo pubblica separatamente,
  perché è compresa nel canone. Effetto: il confronto di prezzo con l'alternativa italiana è **impossibile da fare
  onestamente**; il posizionamento del §5 nasce dal confronto internazionale e dalle fasce del catalogo.
- **Se un compenso legato all'importo recuperato configuri attività riservata** (articolo 115 del testo unico di
  pubblica sicurezza) — la fonte consultata non affronta il caso del creditore che agisce in proprio con uno strumento
  a compenso variabile. È una domanda da legale: vedi §5 e §11.
- **Durata della prova gratuita dei concorrenti** — Chaser non la indica in pagina; un comparatore parla di 10 giorni,
  ma non l'ho verificato.
- **Tariffa per messaggio WhatsApp in Italia** — la documentazione conferma il modello a messaggio e gli scaglioni, ma
  la tabella per Paese non è stata letta. Serve prima di decidere se il canale è compreso nel canone o portato dal
  cliente (§11).

---

## 3. Varco d'identità — le risposte pronte per `new-application`

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `crediti` | Rispetta `^[a-z][a-z0-9_]{0,30}$`. Descrive **cosa l'app è** — la gestione dei crediti commerciali da incassare — e non il nome commerciale «CashGrove», che potrebbe cambiare. Finisce nello schema `app_crediti`, nella rotta `/api/crediti/v1/*` e nell'istanza del modulo di infrastruttura: cambiarlo dopo è una migrazione di dati, non una rinomina. |
| **Modello utente** | `multi` | Il sollecito è un atto che qualcuno compie verso un cliente dell'azienda: sapere **chi** ha mandato cosa e chi ha registrato una promessa non è un lusso, è la condizione per non fare due volte la stessa telefonata e per rispondere quando il debitore protesta. Nella micro-impresa gli utenti sono due o tre — titolare, addetto all'amministrazione, spesso il commercialista esterno — e sono ruoli diversi: il commercialista guarda, non sollecita. Un'app a utente singolo non avrebbe il concetto di «chi ha fatto cosa» e renderebbe impossibile la matrice dei ruoli. |
| **Porta locale** | `8103` | Convenzione del kit: 8100 + 03. Da confermare con `./dev.sh services` al momento dello scaffolding. |
| **Metrica di quota** | `crediti_monitorati` | È la **sola** cosa che il piano limita: quanti crediti aperti l'app tiene sotto sorveglianza in un dato momento. Cresce esattamente con il valore ricevuto — più crediti sorveglio, più solleciti partono da soli, più cruscotto ha senso — ed è comprensibile senza spiegazioni. Le alternative sono peggiori: contare i **solleciti inviati** punirebbe proprio il comportamento che vogliamo incoraggiare e bloccherebbe la funzione principale a fine mese; contare gli **utenti** non c'entra col valore, perché in una micro-impresa gli utenti sono comunque due. |
| **Natura della metrica** | `stock` | Tetto su ciò che esiste **ora**: «150 crediti aperti in monitoraggio; per portarne dentro un altro bisogna che uno si chiuda — incassato, stralciato o archiviato». Non è un consumo che si azzera il primo del mese: un credito monitorato a gennaio è ancora lì a marzo se nessuno l'ha pagato. Trattarlo come consumo lascerebbe accumulare senza limite (basta non aggiungere nulla il mese dopo); trattarlo come giacenza dà anche il comportamento giusto al passaggio a un piano inferiore, che la piattaforma blocca finché lo stato eccede il tetto di destinazione. |
| **Colore-categoria e icona** | `amber` · icona `banconote` (un rettangolo di banconota con una freccia di rientro) | Deve coincidere fra listino (`category`) e modulo frontend (`accentToken`). L'ambra è il colore dell'attenzione dovuta e delle scadenze: dice «c'è qualcosa che aspetta», che è precisamente il messaggio dell'app. Non collide con le app reali già presenti (`fatture` è verde, `crm` è blu) né con il rosso, che va lasciato agli errori veri: un portafoglio crediti tutto rosso smetterebbe di comunicare. |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Debitore` | Il cliente che deve pagare | denominazione, forma (impresa o persona fisica), partita IVA o codice fiscale, recapiti (posta elettronica, telefono), nome del referente amministrativo, lingua preferita, note | **sì** — recapiti e nome del referente; se il debitore è una ditta individuale o un professionista, l'intera anagrafica è dato personale |
| `Credito` | Una fattura emessa e non ancora incassata | numero e data del documento, data di scadenza, importo originario, importo residuo, valuta, stato, riferimento al debitore, origine (a mano / da file), sequenza applicata | indirettamente, attraverso il debitore |
| `Incasso` | Un pagamento ricevuto e imputato a uno o più crediti | data, importo, mezzo dichiarato, riferimento, note | no |
| `SequenzaSolleciti` | Il piano di sollecito: quanti passi, con che tono, a che distanza | nome, attiva sì/no, ambito di applicazione predefinito, finestra oraria consentita | no |
| `PassoSollecito` | Un passo della sequenza | ordine, scarto in giorni rispetto alla scadenza (negativo = prima), canale, modello di messaggio, tono | no |
| `SollecitoInviato` | La **prova** che un messaggio è partito | credito, passo, canale, destinatario, istante, esito (accettato, respinto, non recapitato), identificativo presso il fornitore, corpo effettivamente inviato | **sì** — il destinatario e il corpo |
| `PromessaDiPagamento` | «Pago entro venerdì» | data promessa, importo promesso, chi l'ha registrata, esito (mantenuta, mancata, in attesa) | no (ma è un dato di comportamento riferibile a una persona quando il debitore è persona fisica) |
| `Contestazione` | Il debitore non paga perché contesta | motivo, importo contestato, apertura, chiusura, esito | no |
| `PunteggioDiRischio` | Quanto è probabile che questo debitore paghi in ritardo | valore, fascia, componenti del calcolo, istante del calcolo | è **profilazione** sul comportamento di pagamento |
| `AddebitoDiMora` | Interessi di mora e forfait di 40 euro calcolati su un credito | credito, periodo, tasso applicato, giorni, interessi, forfait, stato (calcolato / incluso in un sollecito / rinunciato) | no |
| `PrevisioneIncassi` | Fotografia della previsione a una certa data | orizzonte, fasce temporali, importi attesi, ipotesi usate | no |

**Relazioni.** `Debitore` **1—N** `Credito`; `Credito` **N—N** `Incasso` (un bonifico può saldare più fatture, una
fattura può essere pagata in più volte); `Credito` **1—N** `SollecitoInviato`, `PromessaDiPagamento`, `Contestazione`,
`AddebitoDiMora`; `SequenzaSolleciti` **1—N** `PassoSollecito`; `Debitore` **1—1** `PunteggioDiRischio` (ricalcolato,
con storico degli istanti).

Macchina a stati del `Credito` — è la parte che tutte le storie devono rispettare:

```
   aperto ──(supera la scadenza)──▶ scaduto ──(sequenza esaurita)──▶ in escalation
     │                                 │  ▲                                │
     │                                 │  └──(contestazione chiusa,        │
     │                                 │      promessa scaduta)            │
     │                                 ▼                                   │
     │                            sospeso ◀──(contestazione aperta         │
     │                                 │       o promessa attiva)          │
     ├──(incasso totale)───────────────┴───────────────────────────────────┴──▶ incassato
     └──(decisione del titolare)──────────────────────────────────────────────▶ stralciato
```

Regole invarianti della macchina a stati: **nessun sollecito parte da `sospeso`, `incassato` o `stralciato`**; un
incasso parziale non cambia stato ma riduce l'importo residuo; solo `incassato` e `stralciato` liberano una unità della
metrica `crediti_monitorati`.

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7, colonne
di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica (`deleted_at`); schema
`app_crediti`; nessuna chiave esterna verso altri schemi ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8).

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il file
> `services/core/src/main/resources/pricing/crediti.yaml`.

**Ragionamento.** La scheda di catalogo indica 19–49 €/mese, e il rilevamento del §2.2 la conferma come collocazione
sensata: sotto il pavimento del mercato specializzato (Chaser parte da 239 €/mese) e appena sotto l'unico concorrente
prezzato nella fascia bassa (Paidnice, 59 €/mese) — che però pretende un gestionale anglosassone e vende in dollari.
I limiti nascono dalla dimensione del cliente: una micro-impresa che emette 30 fatture al mese e viene pagata a 60
giorni ha in giro fra 40 e 90 crediti aperti; una piccola impresa da 50 addetti sta fra 200 e 600. Il piano gratuito
serve a far vedere il valore su una manciata di crediti veri — non a farci vivere dentro.

| Piano | Prezzo mensile | Prezzo annuale | Limite su `crediti_monitorati` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | 15 | — | Chi vuole vedere l'aging e far partire due sequenze prima di decidere. Abbastanza per capire, non abbastanza per viverci. |
| `pro` | 19 € | 190 € (= 10× il mensile, «due mesi in regalo») | 150 | 14 giorni | La micro-impresa: un titolare, un addetto all'amministrazione, qualche decina di fatture al mese. |
| `team` | 49 € | 490 € | 800 | 14 giorni | La piccola impresa con un ufficio amministrativo, più utenti e il commercialista in sola lettura. |

**Note obbligate.**

- Tre piani, non di più: aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile.
- Un limite lasciato vuoto significa **illimitato**, non zero: qui nessun piano è illimitato, di proposito. Un tetto
  esplicito anche in cima evita che un cliente porti dentro dieci anni di storico e trasformi il costo del servizio in
  una sorpresa.
- La prova gratuita di 14 giorni su `pro` e `team` **convive** col piano gratuito ed è utile lo stesso, perché ciò che
  la prova sblocca non è una funzione ma il **volume**: il valore dell'app si vede solo con il portafoglio crediti
  intero dentro, e 15 crediti non bastano a mostrarlo.
- **Costo effettivo dell'incasso**: nessun piano scende sotto i 5 €/mese, quindi la parte fissa per transazione non
  mangia il margine. Il piano `pro` a 19 € regge; l'annuale va comunque spinto.
- **Costo variabile dei canali**: la posta elettronica è a costo trascurabile, i messaggi brevi e la messaggistica no
  (§2.4 e §11). La proposta è che quei canali si usino con **le credenziali del cliente** — è lui che paga il suo
  fornitore — così il canone resta piatto e la piattaforma non finisce ad addebitare a consumo, che le è vietato. Se
  invece si volesse comprenderli, servirebbe un tetto per piano: ma la metrica di quota è **una sola**, quindi
  diventerebbe una funzione del piano e non un limite sorvegliato. **Decisione dello sviluppatore.**
- I prezzi sono **immutabili una volta vivi**: un cambio si fa creando un prezzo nuovo, non modificando l'esistente.

### 5.1 🛑 La componente a percentuale sul recuperato — proposta **respinta con motivazione**, decide lo sviluppatore

La scheda di catalogo propone «flat 19–49 €/mese **più una opzione di successo dello 0,5–1% sull'incassato
recuperato**». È una idea commercialmente attraente — lega il prezzo a un risultato misurabile — e per questo va
esaminata sul serio, non liquidata. **Non la propongo per la prima versione**, per quattro ragioni di peso diverso;
la decisione finale non è mia.

1. **Vincolo di piattaforma, non opinione.** Il listino di appgrove ammette **solo abbonamento ricorrente**: niente
   pagamento una tantum e niente addebito a consumo per lo sforamento ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md)
   §7). Una percentuale sul recuperato è per definizione un addebito variabile a posteriori. Introdurla non è
   configurare un listino: è cambiare il modello di ricavo della piattaforma, per tutte le app.
2. **Misurare «il recuperato» è un problema, non un dettaglio.** Che cosa conta come recuperato? Il pagamento arrivato
   dopo il primo sollecito? Anche quello arrivato senza che nessun sollecito sia partito? E per quanto tempo dopo?
   Senza lettura dei movimenti bancari (§2.4, punto 4) l'unica base è la dichiarazione del cliente, che pagherebbe di
   più dicendo la verità. Con la lettura dei movimenti bancari, si aggiunge un trattamento di dati molto più delicato
   solo per poter fatturare.
3. **Rischio giuridico.** Un compenso proporzionale all'importo recuperato è la forma tipica del contratto delle
   società di recupero crediti, attività riservata a chi ha la licenza del questore quando è svolta **per conto terzi**
   (§2.3, punto 3). Noi venderemmo software, non servizio — ma la forma del corrispettivo è esattamente quella
   dell'attività riservata, ed è un punto su cui non basta il buon senso: **serve un parere legale**.
4. **Il fornitore di pagamento.** Il fornitore che agisce come venditore di riferimento fattura abbonamenti a listino;
   una provvigione calcolata su incassi che avvengono fuori dalla piattaforma, fra due terzi, è un'altra cosa e va
   verificata prima di prometterla.

**Alternativa che ottiene lo stesso effetto senza rompere niente**: far vedere il risultato invece di farselo pagare —
l'app mostra quanto denaro è rientrato dopo l'attivazione, in quanti giorni, e quanto è sceso il tempo medio di incasso
(storia 0024). È l'argomento di vendita, spostato dal listino al prodotto. Se lo sviluppatore vuole comunque la
componente a percentuale, la strada meno rischiosa è un **piano superiore a canone più alto** per chi recupera molto,
non una percentuale.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/crediti.yaml`) si compila **insieme** allo sviluppatore: «niente contratto, niente
> produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

**Categorie particolari (articolo 9): NO.** L'app non tratta dati sulla salute, biometrici, genetici, né opinioni
politiche, convinzioni religiose, orientamento sessuale o appartenenza sindacale. Il dominio non li richiede in nessun
punto e **nessun campo deve permetterne l'ingresso**: l'unica porta aperta è il campo note libero, trattato più sotto.

**Va detto lo stesso, forte, quello che c'è.** Il fatto che una persona non abbia pagato è un dato **non particolare ma
delicato**: la sua diffusione a terzi è illecita, il Garante l'ha sanzionata più volte e il vademecum sul recupero
crediti elenca esattamente le condotte vietate (§2.3, punto 4). Un difetto di prodotto qui — un sollecito mandato
all'indirizzo sbagliato, un oggetto del messaggio che dice «sollecito di pagamento» ed è leggibile da chi passa
davanti allo schermo, una copia di cortesia a un terzo — è una violazione, non un fastidio. Inoltre il **punteggio di
rischio è profilazione**: dev'essere spiegabile, non deve mai produrre da solo effetti verso il debitore, e serve solo
a ordinare il lavoro di una persona.

**Ruoli.** Il cliente di appgrove è **titolare** del trattamento dei dati dei propri debitori; appgrove è
**responsabile** (tratta per suo conto); i fornitori dei canali di invio sono **sub-responsabili**. È la stessa
struttura delle altre app, ma qui conta più del solito perché i dati riguardano persone che **non sono clienti nostri
né suoi utenti**: il debitore non ha mai scelto di stare nel nostro sistema. L'informativa e l'esercizio dei diritti
devono funzionare anche per lui.

**Categorie trattate**

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `debitore.denominazione` | `debitore.denominazione` | debitore (impresa o persona fisica) | anagrafica | identificare chi deve pagare | legittimo interesse del creditore al recupero del proprio credito (per il debitore persona fisica); per il resto non è dato personale | fino a chiusura del credito + termine di prescrizione ordinaria, poi cancellazione |
| `debitore.codice_fiscale` | `debitore.codice_fiscale` | debitore persona fisica | identificativo fiscale | individuare univocamente il debitore, redigere la messa in mora | obbligo di legge e legittimo interesse | come sopra |
| `debitore.referente_nome` | `debitore.referente_nome` | dipendente o collaboratore del debitore | anagrafica | sapere a chi rivolgersi | legittimo interesse | fino a sostituzione del referente o chiusura del rapporto |
| `debitore.email`, `debitore.telefono` | omonime colonne | debitore o suo referente | recapito | recapitare il sollecito | esecuzione del contratto fra creditore e debitore, legittimo interesse al recupero | come sopra |
| `debitore.lingua` | `debitore.lingua` | debitore | preferenza | scrivere il sollecito in una lingua comprensibile | legittimo interesse | come sopra |
| `debitore.note` | `debitore.note` | chiunque il cliente vi scriva | **testo libero, non classificabile a priori** | annotazioni operative | legittimo interesse | come sopra — con l'avvertenza qui sotto |
| `credito.*` (importi, scadenze, stato) | tabella `credito` | debitore | economico | gestire l'incasso | esecuzione del contratto, legittimo interesse | come sopra |
| `sollecito_inviato.destinatario` | `sollecito_inviato.destinatario` | debitore o referente | recapito | prova dell'invio | legittimo interesse, difesa di un diritto in giudizio | 10 anni dall'invio (prova dell'attività di recupero) — **da validare** |
| `sollecito_inviato.corpo` | `sollecito_inviato.corpo` | debitore | contenuto della comunicazione | prova di ciò che è stato effettivamente scritto | come sopra | come sopra |
| `promessa_di_pagamento.*` | tabella omonima | debitore | comportamento | organizzare il lavoro di recupero | legittimo interesse | fino a chiusura del credito |
| `punteggio_di_rischio.*` | tabella omonima | debitore | **profilazione** su comportamento di pagamento | ordinare le priorità di lavoro | legittimo interesse, con trasparenza sul criterio e nessuna decisione automatizzata verso il debitore | ricalcolato; storico 24 mesi — **da validare** |

**Esportazione e cancellazione.** Tutte le tabelle che contengono dati riferibili a una persona devono comparire **sia**
in `exportData` **sia** in `purgeData` del contratto `CreditiDataContract`: `debitore`, `credito`, `incasso`,
`sollecito_inviato`, `promessa_di_pagamento`, `contestazione`, `punteggio_di_rischio`, `addebito_di_mora`. Dimenticare
`sollecito_inviato` è l'errore più probabile, perché sembra un registro tecnico e invece contiene il nome, il recapito
e il testo. La cancellazione è **fisica**: sostituire il nome con un codice non è cancellare. **Attenzione al
conflitto**: la prova dell'attività di recupero può servire in giudizio, quindi la richiesta di cancellazione del
debitore va conciliata con la conservazione per difendere un diritto — è un punto per lo sviluppatore e per la
revisione legale, non per un agente (storia 0030).

**Testo libero.** L'app ha campi nota su debitore, credito e contestazione. Sono un ingresso **non presidiato**: nulla
impedisce a un utente di scriverci «non paga perché è in malattia», che è un dato sulla salute. L'app non fa rilevazione
di contenuto; il presidio, se servirà, è un tema trasversale di piattaforma. Quello che l'app **deve** fare è dirlo
nell'interfaccia, accanto al campo, in tutte e cinque le lingue.

**Integrazioni esterne.** Ognuna di queste, se attivata, è un potenziale nuovo responsabile esterno del trattamento e
va nell'elenco dei fornitori e nell'informativa: fornitore della posta elettronica in uscita; fornitore dei messaggi
brevi; piattaforma WhatsApp Business (Meta — con l'ulteriore questione del trasferimento fuori dall'Unione, che è
**dirimente** vista la regola di residenza europea dei dati); gestionale di fatturazione da cui si importano i crediti;
eventuale accesso ai conti di pagamento per la riconciliazione. Nelle 31 storie **nessuna integrazione automatica è
inclusa**: l'ingresso dei dati è a mano o da file, e i canali si usano con le credenziali del cliente. È una scelta
prudente e volontaria (§11).

**Classificazione della change.** Una app nuova che tratta dati di **persone terze** (i debitori del cliente), con
profilazione e comunicazioni verso l'esterno, è un cambiamento **sostanziale**. Non c'è margine per il contrario, e
merita una valutazione d'impatto anche se le categorie particolari non ci sono: pesano la profilazione, la posizione
degli interessati (non hanno scelto di essere nel sistema) e gli effetti della comunicazione.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata): qui si
> dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `elenca_crediti_scaduti` | `(giorni_minimi?, debitore?, importo_minimo?) → elenco minimizzato di crediti` | Chi deve dei soldi e da quanto | lettura | no |
| `riepilogo_anzianita` | `(alla_data?) → fasce 0-30, 31-60, 61-90, oltre 90 con importi e conteggi` | La fotografia del portafoglio | lettura | no |
| `indicatore_tempo_medio_incasso` | `(periodo) → giorni medi di incasso e andamento` | Come sta andando | lettura | no |
| `previsione_incassi` | `(orizzonte_giorni) → importi attesi per settimana, con le ipotesi usate` | Quanto rientra e quando | lettura | no |
| `punteggio_rischio_debitore` | `(debitore) → punteggio, fascia e componenti del calcolo` | Perché quel cliente è a rischio | lettura | no |
| `storico_solleciti` | `(credito) → cosa è stato mandato, quando, con che esito` | La prova di ciò che si è fatto | lettura | no |
| `registra_incasso` | `(credito, importo, data, mezzo) → bozza di imputazione` | Aggiorna il residuo | scrittura | **sì** |
| `registra_promessa_di_pagamento` | `(credito, data_promessa, importo) → bozza di promessa` | Sospende i solleciti fino alla data | scrittura | **sì** |
| `apri_contestazione` | `(credito, motivo, importo_contestato) → bozza di contestazione` | Sospende i solleciti | scrittura | **sì** |
| `sospendi_solleciti` | `(credito, motivo, fino_a) → bozza di sospensione` | Ferma l'automatismo | scrittura | **sì** |
| `prepara_sollecito` | `(credito, canale, tono) → bozza del messaggio, con destinatario e importo` | Prepara, **non invia** | scrittura | **sì** |
| `invia_sollecito` | `(bozza) → esito della trasmissione` | **Effetto irreversibile verso l'esterno**: il messaggio arriva a una persona che non è nostro utente | scrittura irreversibile | **sì, obbligatoria e non aggirabile** |
| `prepara_messa_in_mora` | `(credito) → bozza della lettera con interessi e forfait calcolati` | Prepara un atto con effetti giuridici | scrittura | **sì** |

**Lettura.** Gli strumenti di **lettura** sono la ragione per cui il livello conversazionale rende questa app più utile
delle concorrenti: la domanda «chi mi deve soldi da più di 60 giorni e quanto?» oggi non viene fatta perché costa dieci
minuti di foglio di calcolo, e in chat costa cinque secondi. Sul lato **scrittura** vale il contrario: `invia_sollecito`
è il caso di scuola del vincolo di sicurezza del catalogo (§8) — il messaggio esce verso una persona terza, non si
richiama indietro, e un assistente che sbaglia destinatario produce una violazione di dati personali. Bozza e conferma
non sono una cortesia: sono il presidio.

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine dell'epica l'app esiste, è accesa, vuota e utilizzabile: servizio avviabile, schema con le sue tabelle,
modulo visibile nella barra laterale, quota che blocca a `429`, avvio locale senza cablaggi a mano.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | Istanza di scaffolding, rotte `/api/crediti/v1/*`, definizione delle interfacce, infrastruttura dal modulo comune |
| [0002](01-fondamenta/0002-modello-dati-multi-account.md) | Modello dati multi-account | Schema `app_crediti` con debitori e crediti, `tenant_id`, colonne di controllo, cancellazione logica |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Manifesto, registrazione, sezioni, cinque lingue, tema chiaro e scuro |
| [0004](01-fondamenta/0004-abbonamento-e-quota.md) | Abbonamento e quota | Metrica `crediti_monitorati` a giacenza, catena dei varchi, blocco a `429` |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | `./dev.sh services` mostra l'app, dati inventati per lavorarci subito |

### Epica 02 — Portafoglio crediti

Alla fine i crediti sono dentro: si inseriscono a mano o da file, si registrano gli incassi, il residuo è affidabile e
lo stato del credito segue la macchina a stati. È la base su cui tutto il resto poggia.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-portafoglio-crediti/0006-anagrafica-dei-debitori.md) | Anagrafica dei debitori | Chi deve pagare, con i recapiti e la lingua, e nient'altro di ciò che la norma non ammette |
| [0007](02-portafoglio-crediti/0007-registrazione-dei-crediti.md) | Registrazione dei crediti | Inserimento a mano di una fattura da incassare, con la sua scadenza |
| [0008](02-portafoglio-crediti/0008-importazione-dei-crediti-da-file.md) | Importazione dei crediti da file | Il portafoglio intero entra in cinque minuti, con anteprima e scarto delle righe errate |
| [0009](02-portafoglio-crediti/0009-registrazione-degli-incassi.md) | Registrazione degli incassi | Pagamenti totali e parziali imputati ai crediti, con residuo ricalcolato |
| [0010](02-portafoglio-crediti/0010-stati-del-credito-e-chiusura.md) | Stati del credito e chiusura | La macchina a stati, lo stralcio e la liberazione della quota |

### Epica 03 — Solleciti automatici

È il cuore dell'app: le sequenze partono da sole, rispettano le regole di condotta, si fermano quando devono e lasciano
la prova di quello che hanno fatto.

| # | Storia | In una riga |
|---|---|---|
| [0011](03-solleciti-automatici/0011-sequenze-di-sollecito.md) | Sequenze di sollecito | Il piano dei passi: quando, con che canale, con che tono |
| [0012](03-solleciti-automatici/0012-modelli-di-messaggio.md) | Modelli di messaggio | Testi con segnaposto verificati, per lingua del debitore, con anteprima |
| [0013](03-solleciti-automatici/0013-pianificazione-e-finestre-di-invio.md) | Pianificazione e finestre di invio | Cosa parte oggi, in quali orari e giorni, con quale frequenza massima |
| [0014](03-solleciti-automatici/0014-invio-per-posta-elettronica.md) | Invio per posta elettronica | Il primo canale reale, con esito, mancato recapito e prova d'invio |
| [0015](03-solleciti-automatici/0015-canali-brevi-e-messaggistica.md) | Canali brevi e messaggistica | Messaggio breve e messaggistica con le credenziali del cliente, come canale aggiuntivo |
| [0016](03-solleciti-automatici/0016-sospensione-dei-solleciti.md) | Sospensione dei solleciti | Niente sollecito a chi ha pagato, a chi ha promesso, a chi contesta |
| [0017](03-solleciti-automatici/0017-registro-dei-solleciti.md) | Registro dei solleciti | Cosa è stato mandato a chi, quando e con che esito: la prova |

### Epica 04 — Esiti e recupero

Quello che succede dopo il sollecito: la risposta del debitore diventa un dato, e quando la via bonaria si esaurisce
l'app prepara — senza mai spedire da sola — gli atti con effetti giuridici.

| # | Storia | In una riga |
|---|---|---|
| [0018](04-esiti-e-recupero/0018-promesse-di-pagamento.md) | Promesse di pagamento | «Pago venerdì»: si registra, sospende, e se salta si riprende |
| [0019](04-esiti-e-recupero/0019-contestazioni-e-pagamenti-parziali.md) | Contestazioni e pagamenti parziali | Il debitore contesta o paga meno: l'importo conteso esce dal sollecito |
| [0020](04-esiti-e-recupero/0020-interessi-di-mora-e-forfait.md) | Interessi di mora e forfait | Calcolo a norma, con tasso storicizzato per semestre e forfait di 40 euro |
| [0021](04-esiti-e-recupero/0021-lettera-di-messa-in-mora.md) | Lettera di messa in mora | La bozza dell'atto formale, mai spedita in automatico |
| [0022](04-esiti-e-recupero/0022-pagina-pubblica-del-credito.md) | Pagina pubblica del credito | Il debitore vede il dettaglio e le coordinate del creditore; appgrove non incassa nulla |

### Epica 05 — Analisi e previsione

I numeri che il titolare guarda: quanto denaro è fermo, da quanto, quanto ci mette a rientrare, chi è a rischio e cosa
aspettarsi nelle prossime settimane.

| # | Storia | In una riga |
|---|---|---|
| [0023](05-analisi-e-previsione/0023-anzianita-dei-crediti.md) | Anzianità dei crediti | Il prospetto per fasce di scaduto, con il denaro fermo a colpo d'occhio |
| [0024](05-analisi-e-previsione/0024-tempo-medio-di-incasso.md) | Tempo medio di incasso | L'indicatore e il suo andamento: la prova che l'app serve a qualcosa |
| [0025](05-analisi-e-previsione/0025-punteggio-di-rischio-del-debitore.md) | Punteggio di rischio del debitore | Chi paga male, con il calcolo spiegato e nessuna decisione automatica |
| [0026](05-analisi-e-previsione/0026-previsione-degli-incassi.md) | Previsione degli incassi | Quanto rientra nelle prossime settimane, con le ipotesi dichiarate |
| [0027](05-analisi-e-previsione/0027-esportazione-per-il-commercialista.md) | Esportazione per il commercialista | Il file che il consulente chiede, generato in un clic |

### Epica 06 — Esposizione conversazionale e prove end-to-end

Il contratto degli strumenti, la regola bozza-e-conferma applicata dove serve davvero, i diritti dell'interessato
completi e il percorso end-to-end che tiene insieme tutto.

| # | Storia | In una riga |
|---|---|---|
| [0028](06-esposizione-conversazionale-e-prove/0028-strumenti-di-lettura.md) | Strumenti di lettura | Contratto dei sei strumenti di sola lettura, con dati minimizzati |
| [0029](06-esposizione-conversazionale-e-prove/0029-strumenti-di-scrittura-con-conferma.md) | Strumenti di scrittura con conferma | Bozza e conferma umana obbligatoria, con `invia_sollecito` come caso limite |
| [0030](06-esposizione-conversazionale-e-prove/0030-esportazione-e-cancellazione.md) | Esportazione e cancellazione | Il contratto dati completo: nessuna tabella dimenticata |
| [0031](06-esposizione-conversazionale-e-prove/0031-percorso-end-to-end.md) | Percorso end-to-end | `[J-CREDITI]` dal credito scaduto all'incasso, e registro di copertura aggiornato |

**Totale**: 6 epiche, 31 storie.

---

## 9. Estensioni della console di amministrazione

Servono estensioni, ma poche e tutte diagnostiche: l'app manda messaggi **verso persone che non sono nostri utenti**
attraverso fornitori esterni, quindi chi amministra la piattaforma deve poter rispondere a «i solleciti di questo
account partono?» senza mai guardare a chi sono indirizzati. Servono inoltre la vista dell'arretrato della lavorazione
programmata e una deroga temporanea al tetto dei crediti monitorati per il caricamento iniziale.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| 02 — BillGrove (fatturazione) | **dipende da** (quando la suite esisterà) | La fattura emessa: è l'oggetto che CashGrove insegue. Oggi il dato entra a mano o da file, proprio per non dipenderne |
| 01 — InvoiceGrove (fatturazione elettronica) | a valle della stessa catena | Il catalogo (§6) avverte che InvoiceGrove non è un prodotto autonomo e va progettato come strato di conformità di BillGrove: CashGrove non deve aspettarsi da lui nulla di diverso da quanto gli dà BillGrove |
| 04 — LeadGrove (gestione clienti) | **condivide dati con** | L'anagrafica clienti condivisa, che il catalogo (§6) indica come cuore della suite: il debitore di CashGrove **è** il cliente di LeadGrove. Il rischio di due anagrafiche divergenti è concreto |
| 06 — QuoteGrove (preventivi) | a monte della stessa catena | Catena preventivo → ordine → fattura → incasso (6 → 2 → 1 → 3): CashGrove è l'ultimo anello, quello che dice se la catena ha prodotto denaro |
| 05 — ChatGrove (commercio su messaggistica) | **si sovrappone a** sul canale | Entrambe vorrebbero mandare messaggi su WhatsApp. Il catalogo (§8) avverte che ChatGrove dipende da Meta e dai fornitori intermedi: è un rischio di piattaforma che, se il canale fosse condiviso, CashGrove erediterebbe |
| 12 — app di assistenza clienti | **alimenta** | Una contestazione aperta in CashGrove è spesso un problema di assistenza: sono due viste dello stesso attrito col cliente |

**Lettura.** CashGrove **ha senso da sola** — è anzi una delle poche del catalogo che vive benissimo isolata, perché il
suo dato di ingresso (una fattura scaduta) si può digitare o importare da qualsiasi gestionale. Dentro la suite diventa
molto più forte: è l'anello che chiude la catena del documento contabile e trasforma «ho fatturato» in «ho incassato»,
che il catalogo (§6) indica come l'argomento di vendita più forte dell'insieme.

**Sovrapposizioni da evitare.**

- **L'anagrafica del debitore contro l'anagrafica cliente** (app 04, e in prospettiva 02). Se le due app tengono due
  anagrafiche scollegate, il cliente si ritrova a correggere l'indirizzo due volte e a dare la colpa a noi. La regola
  di piattaforma vieta le chiavi esterne fra schemi e le chiamate sincrone fra app: la strada è la **proiezione locale
  alimentata a eventi**, e va disegnata quando le due app esisteranno davvero — non ora.
- **Il sollecito dentro il programma di fatturazione.** Molti gestionali mandano già una email al superamento della
  scadenza (§2.1). Se BillGrove facesse lo stesso, si costruirebbe due volte la stessa cosa e il cliente riceverebbe
  due solleciti. Confine proposto: **BillGrove emette e basta, CashGrove sollecita**.
- **Il canale di messaggistica** con l'app 05: un solo strato di invio, se e quando esisterà, non due.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **La componente a percentuale sul recuperato** (§5.1) | Confligge col vincolo «solo abbonamento ricorrente», richiede di misurare il recuperato e ha un profilo giuridico incerto | **sviluppatore**, con parere legale |
| 2 | **Prezzi, limiti dei piani, durata della prova** (§5) | Fermata di escalation di piattaforma: nessun agente li fissa | **sviluppatore** |
| 3 | **Manifesto dei dati e durate di conservazione** (§6) | Le durate proposte (10 anni per la prova d'invio, 24 mesi per lo storico del punteggio) sono ragionevoli ma non fondate su una fonte: vanno decise | **sviluppatore**, con la revisione legale pre-go-live |
| 4 | **Conflitto fra diritto alla cancellazione del debitore e conservazione della prova di recupero** | Due interessi legittimi che si oppongono; la scelta ha effetti sul contratto dati | **sviluppatore** / storia `0030` |
| 5 | **Chi paga i messaggi brevi e la messaggistica** | La piattaforma vieta l'addebito a consumo; il canale costa a messaggio. La proposta (credenziali del cliente) va confermata | **sviluppatore** / storia `0015` |
| 6 | **Trasferimento di dati fuori dall'Unione via WhatsApp Business (Meta)** | La regola di residenza europea dei dati e la preferenza per fornitori europei rendono il canale problematico, anche se il catalogo lo cita | **sviluppatore** |
| 7 | **Se un compenso legato al recuperato configuri attività riservata** (articolo 115 del testo unico di pubblica sicurezza) | La fonte consultata non affronta il caso; non è una domanda da agente | **legale** |
| 8 | **Stato del regolamento europeo sui ritardi di pagamento** | Iter non determinato (§2.7); cambierebbe termini e forse la maggiorazione | **verifica periodica** / storia `0020` |
| 9 | **Innesto sui gestionali italiani di fatturazione** | Nessuna integrazione automatica è nelle 31 storie: è una scelta prudente, ma è anche la prima cosa che il mercato chiederà | **sviluppatore**, come epica successiva |
| 10 | **Riconciliazione bancaria automatica** | Richiede accesso ai conti di pagamento: fornitore nuovo, dati più delicati, e sblocca la misura del «recuperato» del punto 1 | **sviluppatore**, come epica successiva |
| 11 | **Anagrafica condivisa con le app 02 e 04** | Va disegnata a eventi quando quelle app esisteranno; anticiparla ora sarebbe lavoro non richiesto | **epica di piattaforma** |

**Rischi noti**

- **Un sollecito sbagliato fa più danni di un sollecito mancato** — se l'app manda un sollecito a chi ha già pagato, il
  cliente perde la fiducia nell'automatismo e lo spegne, cioè smette di usare il prodotto. Attenuazione: la sospensione
  automatica (storia 0016) è requisito di prima classe, non una funzione accessoria, ed è coperta dal percorso
  end-to-end.
- **Recapito della posta elettronica** — i solleciti hanno un profilo perfetto per finire nella posta indesiderata:
  invio automatico, molti destinatari, parole come «scaduto» e «pagamento». Attenuazione: invio dal dominio del
  creditore con le sue credenziali, esiti tracciati, avviso quando il tasso di mancato recapito sale.
- **Condotta verso il debitore** — il confine fra sollecito legittimo e molestia è tracciato dal Garante (§2.3) e il
  prodotto può farlo superare da solo, se le finestre di invio e i tetti di frequenza non sono presidiati.
  Attenuazione: limiti nel motore, non nella configurazione dell'utente (storia 0013).
- **Dipendenza dall'inserimento manuale dei dati** — senza innesto sul gestionale, il portafoglio crediti invecchia e
  l'app perde credibilità in poche settimane. Attenuazione: importazione da file veloce e ripetibile (storia 0008) come
  ponte, e integrazione come prima epica successiva.
- **Concorrenza dei gestionali** — chi ha già Fatture in Cloud potrebbe accontentarsi del suo sollecito rudimentale.
  Attenuazione: il valore sta in ciò che il gestionale non fa (sequenze, esiti, sospensioni, interessi a norma,
  previsione), e va detto così nella pagina di presentazione.
- **Avvertenza del catalogo sui dati di mercato** (§8) — le fasce di prezzo e le stime vanno riverificate al momento
  della costruzione: due delle fonti di prezzo qui sopra sono comparatori.

**Fuori dimensionamento**: non applicabile. Sei epiche (fascia 4-7), da 4 a 7 storie ciascuna (fascia 4-8), 31 storie in
tutto (fascia 20-45).
