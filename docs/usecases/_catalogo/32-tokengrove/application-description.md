# TokenGrove — descrizione dell'applicazione

**Numero di catalogo**: 32 · **Tipo**: orizzontale · controllo dei costi delle chiamate ai modelli linguistici · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 32](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** TokenGrove risponde a tre domande sulla spesa per i modelli linguistici: *quanto ho speso*, *per che
cosa*, *quanto spenderò a fine mese se continuo così*. Raccoglie il consumo delle chiamate ai fornitori di modelli
(OpenAI, Anthropic, Google, Mistral e simili), lo converte in euro con un listino dei prezzi datato, lo imputa a una
squadra, a un progetto, a un cliente finale o a una funzionalità del prodotto, e avvisa **prima** che il budget sia
sfondato, non quando arriva la fattura.

**Per chi.** Imprese da 1 a 50 addetti che hanno messo un modello linguistico **dentro il proprio prodotto o dentro
il proprio lavoro quotidiano** e adesso hanno una voce di costo che cresce senza che nessuno sappia spiegarla. Chi
compra è il titolare o il responsabile tecnico che firma la fattura del fornitore; chi usa tutti i giorni è chi
sviluppa (collega le fonti, mette le etichette) e chi tiene i conti (guarda la spesa per cliente e per squadra).

**Quale problema toglie.** Oggi il cliente tipo apre tre pannelli diversi — quello di OpenAI, quello di Anthropic,
quello di Google — legge tre valute e tre modi di raggruppare, e ricopia i numeri in un foglio di calcolo il primo
del mese. Nessuno dei tre pannelli sa dire «questo cliente mi è costato 43 € di modelli» né «la funzione di riassunto
automatico mi costa il 60% del totale». Il foglio di calcolo costa mezza giornata al mese, è vecchio di trenta giorni
quando è pronto, e non avvisa nessuno: la sorpresa arriva con la fattura. L'indagine Harness 2026 su 700 fra
responsabili tecnici e addetti descrive esattamente questo quadro — la spesa cresce più in fretta della capacità di
vederla, attribuirla e spiegarla (§2.6, fonte 12).

**Cosa NON fa.**

- **non sta in mezzo alle chiamate del cliente**: TokenGrove non è un punto di passaggio, non instrada, non riprova,
  non mette in cache e non può fermare una chiamata (§3.1 e storia [0027](05-budget-avvisi-e-anomalie/0027-semaforo-del-budget-consultabile.md));
- **non vede il contenuto** delle richieste e delle risposte: misura, non conserva (§6);
- **non valuta la qualità** delle risposte del modello (niente confronto fra versioni di istruzioni, niente giudizio
  automatico): è il mestiere di Langfuse e Braintrust, non il nostro;
- **non fa tracciatura profonda degli agenti** (l'albero delle chiamate annidate di un agente che ne chiama altri);
- **non rivende capacità di calcolo** e non fa da intermediario di pagamento verso i fornitori;
- **non fa autogestione presso il cliente** né certificazioni di sicurezza per la grande impresa.

**Rischio di sostituzione da parte dei modelli linguistici.** `rafforzata`. Il mercato di questa app **è** la
diffusione dei modelli: più aziende li usano, più il problema esiste. Un assistente generico non può risolverlo
perché il valore non sta nel ragionamento ma nel **dato accumulato nel tempo e riconciliato con la fattura**: la
serie storica del consumo, il listino datato con cui è stato convertito, le regole di attribuzione dell'azienda. È
però anche l'app più esposta del catalogo alla concorrenza **aperta e gratuita**: vedi §2.1 e il rischio R1 in §11.

---

## 2. Mercato e analisi in rete

> Compilata dopo 10 ricerche e letture mirate ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4).
> Ciò che non è stato trovato è **dichiarato** al §2.7, non colmato a intuito.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| Langfuse | Germania (Berlino), prodotto mondiale | Tracciatura, valutazione e conteggio dei costi delle chiamate ai modelli. Codice aperto, autogestione **gratuita** | Hobby gratuito (50.000 unità/mese, 30 giorni di storico) · Core 29 $/mese (100.000 unità, 90 giorni) · Pro 199 $/mese (3 anni di storico) · consumo oltre soglia 8 $ ogni 100.000 unità | [langfuse.com/pricing](https://langfuse.com/pricing) |
| Helicone | Stati Uniti | Registrazione delle chiamate con punto di passaggio o invio asincrono, cruscotti di costo. Codice aperto | Hobby gratuito (10.000 richieste, 7 giorni, 1 posto) · Pro 79 $/mese (posti illimitati, 1 mese di storico) · Team 799 $/mese (3 mesi) | [helicone.ai/pricing](https://www.helicone.ai/pricing) |
| Portkey | Stati Uniti / India | Punto di passaggio con instradamento, budget e registrazioni | Developer gratuito (10.000 registrazioni/mese, 3 giorni di storico) · Production 49 $/mese (100.000 registrazioni, 30 giorni) + 9 $ ogni 100.000 in più · Enterprise su richiesta | [portkey.ai/pricing](https://portkey.ai/pricing) |
| LiteLLM | Stati Uniti, codice aperto | Punto di passaggio multi-fornitore con chiavi virtuali, budget per chiave e conteggio della spesa. **Autogestione gratuita** | Licenza aperta gratuita; versione a pagamento non rilevata su pagina ufficiale in questa ricerca | [docs.litellm.ai/docs/simple_proxy](https://docs.litellm.ai/docs/simple_proxy) |
| OpenRouter | Stati Uniti | Intermediario verso decine di modelli; restituisce **costo e conteggio dei segni di testo dentro la risposta stessa**, chiavi separate per cliente con tetto di spesa | Nessun abbonamento: margine sul consumo | [openrouter.ai/docs/cookbook/administration/usage-accounting](https://openrouter.ai/docs/cookbook/administration/usage-accounting) |
| CloudZero, Finout, Vantage, Mavvrik | Stati Uniti / Israele | Piattaforme di governo dei costi della nuvola che hanno aggiunto i modelli linguistici: costo per cliente, margine per cliente, ribaltamento interno | Prezzo **non pubblico**: tutte richiedono un contatto commerciale | [finout.io](https://www.finout.io/blog/tracking-ai-costs-per-customer-and-per-feature-in-2026) · [mavvrik.ai](https://www.mavvrik.ai/blog/how-to-track-ai-costs/) |

**Lettura.** Il campo è diviso in due metà e **il buco sta esattamente in mezzo**. La metà tecnica (Langfuse,
Helicone, Portkey, LiteLLM) è fatta per chi sviluppa: tracciatura, valutazione, instradamento; è potente, ha un
piano gratuito generoso e — nei tre casi su quattro con codice aperto — si può far girare in casa senza pagare
nulla. La metà economica (CloudZero, Finout, Vantage) è fatta per il responsabile finanziario della grande impresa:
costo per cliente, margine, ribaltamento; nessuna di queste pubblica un prezzo, il che è di per sé la prova che non
si rivolge a un'azienda di otto persone. Chi ha otto persone, un prodotto con dentro un modello e trenta clienti
paganti non è servito bene da nessuno dei due lati: dal primo perché deve costruirsi l'attribuzione economica a
mano, dal secondo perché non è il suo cliente. **È l'unica ragione onesta per cui questa app potrebbe esistere**, e
il §11 la tratta come rischio, non come certezza.

### 2.2 Prezzi praticati nel dominio

Rilevati **su pagina ufficiale** (non su siti di comparazione), agosto 2026:

- **unità di misura prevalente**: la *richiesta registrata* o l'*unità di osservazione*, per mese, con un tetto
  incluso nel piano. Nessuno dei quattro prodotti esaminati vende a posto/utente: Helicone dichiara esplicitamente
  «posti illimitati» già nel piano intermedio. È un dato importante e va rispettato: un listino a posti sarebbe
  fuori mercato in questo dominio;
- **piano gratuito**: sempre presente, fra 10.000 (Helicone, Portkey) e 50.000 (Langfuse) unità al mese;
- **la leva vera è lo storico**, non il volume: 3 giorni Portkey, 7 giorni Helicone, 30 giorni Langfuse nel piano
  gratuito; 30 giorni-1 mese nel piano intermedio; 90 giorni-3 anni in quello alto. Chi vuole confrontare
  quest'anno con l'anno scorso deve pagare. È il modo con cui questo mercato differenzia i piani;
- **fascia intermedia**: 29-79 $/mese (Langfuse Core 29, Portkey Production 49, Helicone Pro 79);
- **fascia alta**: 199-799 $/mese, con un salto molto ampio (Helicone passa da 79 a 799);
- **consumo oltre il tetto**: Langfuse e Portkey addebitano lo sforamento (8 $ e 9 $ ogni 100.000). **Noi non
  possiamo farlo**: la piattaforma vieta l'addebito a consumo e impone il blocco a `429` ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §7).
  Vedi il punto aperto P2 in §11: la scheda di catalogo prevedeva «espansione tramite sforamento», e non è
  realizzabile così com'è scritta;
- durata della prova gratuita: **non rilevata** su nessuna delle quattro pagine ufficiali (vedi §2.7).

### 2.3 Obblighi normativi del settore

Il dominio **non è normato in sé**: non esiste una legge che imponga di misurare la spesa per i modelli. Gli
obblighi arrivano di rimbalzo, e sono tre:

1. **Protezione dei dati personali (Regolamento generale europeo).** È l'unico obbligo che tocca il modello dati, e
   tocca ciò che *non* si deve raccogliere: se il prodotto vedesse il contenuto delle richieste vedrebbe i dati dei
   clienti dei nostri clienti — e un contenuto qualunque può contenere qualunque cosa, comprese le categorie
   particolari dell'articolo 9. È la ragione tecnica, non solo di gusto, per cui il ricevitore delle misure
   **rifiuta** il contenuto (§6 e storia [0008](02-ingresso-dei-dati-di-consumo/0008-contratto-della-misura-di-consumo.md)).
2. **Conservazione delle scritture contabili.** Se il cliente usa i numeri di TokenGrove per ribaltare il costo su
   un cliente finale (fattura o nota di addebito), quei numeri diventano un supporto di una scrittura contabile e
   vanno conservati coerentemente con essa. Non ci rende un registratore fiscale, ma ci impone che **un conto già
   emesso non cambi mai** quando cambia il listino (§3 dell'epica 03).
3. **Regolamento europeo sull'intelligenza artificiale.** Non si applica a noi: misurare quanto costa una chiamata
   non è né sviluppare né mettere in servizio un sistema di intelligenza artificiale. Va detto perché è la domanda
   che il cliente farà comunque, e la risposta è «no, e neanche a te per il solo fatto di chiamare un modello».

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta prevista:

1. **fonti dei fornitori di modelli in sola lettura** — Anthropic (`/v1/organizations/usage_report/messages` e
   `/v1/organizations/cost_report`), OpenAI (usage e costs), poi Google, Mistral, Azure, OpenRouter. È la prima
   integrazione e quella che dà valore in cinque minuti;
2. **invio delle misure dal prodotto del cliente** — una libreria sottile e un ricevitore compatibile con le
   convenzioni OpenTelemetry per l'intelligenza artificiale generativa (`gen_ai.usage.input_tokens`,
   `gen_ai.usage.output_tokens`), così che chi ha già uno strumento di osservabilità non debba scrivere due volte
   la stessa cosa;
3. **recapito degli avvisi** — posta elettronica (già di piattaforma), poi messaggistica di squadra;
4. **esportazione tabellare e verso il foglio di calcolo** — chiesta sempre, in ogni dominio;
5. **verso le altre app appgrove**: il costo per cliente finale è un numero che serve a InsightGrove (20) e alla
   marginalità per cliente. Nessuna chiamata diretta fra app: solo eventi (§10).

Le voci 1 e 2 **non** introducono un fornitore esterno che tratta dati per nostro conto: nella 1 siamo noi a
leggere dal fornitore del cliente (con una chiave di sola lettura che il cliente ci affida), nella 2 è il cliente a
mandare a noi. Il recapito degli avvisi (3) usa il fornitore di posta già in uso dalla piattaforma.

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Da ciò che i prodotti concorrenti mettono in evidenza e da come la stampa di settore descrive il problema:

- **vogliono il numero in euro, non i segni di testo**. Il conteggio dei segni di testo è il mezzo; la domanda è
  «quanto mi costa»;
- **vogliono sapere *chi*.** L'indagine Harness riassume il vuoto in tre domande senza risposta: chi possiede il
  conto, perché è salito, se sta rendendo. La prima è attribuzione, la seconda è scomposizione dell'impennata;
- **non vogliono un secondo punto di guasto.** È la lamentela ricorrente contro i punti di passaggio: chi mette un
  pezzo in mezzo alle proprie chiamate di produzione sa che quel pezzo può fermarle. Langfuse ne ha scritto per
  spiegare perché il proprio prodotto **non** è un punto di passaggio (fonte 10);
- **non vogliono la valutazione della qualità**: è il bisogno della squadra che costruisce con i modelli tutto il
  giorno, non di chi ne ha messo uno dentro un prodotto e vuole solo che non costi troppo;
- **non vogliono un progetto di adozione.** Se il valore non compare nella prima seduta, la prova finisce lì. È la
  ragione per cui l'epica 02 comincia dalla lettura dei rendiconti (zero righe di codice) e non dall'invio delle
  misure (una riga di codice, ma pur sempre un rilascio del loro prodotto).

### 2.6 Fonti consultate

1. **Anthropic — Usage and Cost API (pagina ufficiale)** — <https://platform.claude.com/docs/en/manage-claude/usage-cost-api>
   — la fonte più utile di tutte. Il consumo si legge a intervalli di 1 minuto, 1 ora o 1 giorno e si può
   raggruppare per modello, spazio di lavoro, chiave e livello di servizio; il **costo in dollari** invece è
   disponibile **solo a giorno**. I dati compaiono «tipicamente entro 5 minuti» dalla chiamata e l'interrogazione
   sostenibile è **una al minuto**. Ne discendono tre requisiti nostri: il ritardo si dichiara all'utente, il
   calendario di importazione rispetta un'interrogazione al minuto per fonte, e i due flussi (consumo e costo) si
   riconciliano invece di sovrapporsi.
2. **OpenAI — ricettario delle interfacce di consumo e costo** — <https://developers.openai.com/cookbook/examples/completions_usage_api>
   — stessa struttura: intervalli 1 minuto/1 ora/1 giorno per il consumo, raggruppabile per modello, progetto,
   chiave e utente; per il **costo solo il giorno**. Conferma che la granularità minima comune fra i due fornitori
   principali è il **giorno per il costo** — quindi il conto per cliente finale non si può ricavare dal rendiconto:
   serve la misura in ingresso.
3. **OpenAI — riferimento dell'interfaccia dei costi** — <https://developers.openai.com/api/reference/resources/admin/subresources/organization/subresources/usage/methods/costs>
   — raggruppamento per progetto, voce di listino e chiave; conferma che l'unità di attribuzione nativa del
   fornitore è **la chiave e il progetto**, non la squadra né il cliente finale. È il motivo per cui esistono le
   regole di mappatura della storia [0020](04-attribuzione-della-spesa/0020-regole-di-mappatura-dalle-chiavi.md).
4. **Helicone — pagina prezzi ufficiale** — <https://www.helicone.ai/pricing> — 10.000 richieste gratuite con 7
   giorni di storico, 79 $/mese con posti illimitati e 1 mese di storico, 799 $/mese con 3 mesi. Da qui: lo storico
   è la leva di differenziazione e i posti non si vendono.
5. **Langfuse — pagina prezzi ufficiale** — <https://langfuse.com/pricing> — 50.000 unità gratuite, 29 $/mese per
   100.000, 199 $/mese per lo storico a 3 anni, **autogestione gratuita**. È il concorrente che fissa il tetto di
   quanto possiamo chiedere: sopra i ~40 €/mese si compete con un prodotto migliore sul suo terreno.
6. **Portkey — pagina prezzi ufficiale** — <https://portkey.ai/pricing> — 49 $/mese per 100.000 registrazioni e 9 $
   ogni 100.000 in più. Conferma la fascia intermedia e l'uso dello sforamento a pagamento, che a noi è vietato.
7. **LiteLLM — sincronizzazione automatica dei modelli** — <https://docs.litellm.ai/docs/proxy/sync_models_github>
   — il listino dei prezzi è un **file di dati scaricato da un archivio pubblico**, sincronizzabile ogni sei ore
   **senza riavviare il servizio**. È la conferma pratica che il catalogo dei prezzi va trattato come dato e non
   come codice: è esattamente il requisito della storia [0013](03-listino-dei-fornitori-e-calcolo-del-costo/0013-catalogo-dei-prezzi-datato.md).
8. **LiteLLM — file dei prezzi e delle finestre di contesto** — <https://github.com/BerriAI/litellm/blob/main/model_prices_and_context_window.json>
   — un unico file con prezzi e limiti di oltre cento modelli, aggiornato in continuazione da contributi esterni.
   Ne ricavo la forma del nostro catalogo (chiave del modello, prezzo per unità in ingresso, in uscita, per la
   parte servita da cache) e la conferma che nessuno riesce a tenerlo aggiornato con i propri rilasci.
9. **LiteLLM — prezzi propri** — <https://docs.litellm.ai/docs/proxy/custom_pricing> — esiste ed è usata la
   necessità di sovrascrivere il listino pubblico con il proprio prezzo negoziato. Da qui la storia
   [0016](03-listino-dei-fornitori-e-calcolo-del-costo/0016-prezzi-negoziati-per-account.md).
10. **Langfuse — «Should you use an LLM Proxy?»** — <https://langfuse.com/blog/2024-09-langfuse-proxy> — il
    ragionamento, scritto da chi vende lo strumento, sul perché stare in mezzo alle chiamate sia una scelta pesante:
    diventa un punto di guasto unico, aggiunge latenza, e vincola il cliente. È la fonte che ha più influito sulla
    decisione del §3.1.
11. **OpenRouter — conteggio del consumo e chiavi di provisioning** —
    <https://openrouter.ai/docs/cookbook/administration/usage-accounting> e
    <https://openrouter.ai/docs/features/provisioning-api-keys> — restituisce costo e conteggi **dentro la risposta
    della chiamata**, e permette una chiave per cliente con tetto di spesa. È il concorrente più scomodo: per chi
    passa da OpenRouter, metà del nostro valore è già inclusa e gratuita. Sta nei rischi (R1).
12. **Harness — «State of AI in FinOps 2026» (comunicato)** — <https://www.prnewswire.com/news-releases/new-harness-report-reveals-enterprise-ai-spend-has-outgrown-the-systems-built-to-track-it-302837776.html>
    — indagine su 700 fra responsabili tecnici e addetti in cinque paesi: la spesa per l'intelligenza artificiale
    cresce più in fretta della capacità di vederla e attribuirla; restano senza risposta «chi possiede il conto,
    perché è salito, se sta rendendo». È un comunicato di un fornitore, quindi di parte, ma le tre domande sono la
    migliore sintesi del bisogno che ho trovato e sono diventate i tre indicatori della schermata di panoramica.
13. **Finout — costi per cliente e per funzionalità** — <https://www.finout.io/blog/tracking-ai-costs-per-customer-and-per-feature-in-2026>
    — descrive il collegamento fra costo dei modelli, costo di servizio e margine per cliente in un prodotto
    multi-cliente. Ne ho ricavato la scelta delle dimensioni di attribuzione (§ epica 04).
14. **Mavvrik — come tracciare i costi dell'intelligenza artificiale** — <https://www.mavvrik.ai/blog/how-to-track-ai-costs/>
    — la regola pratica del passaggio da *mostrare* ad *addebitare*: prima 4-6 settimane di sola visibilità, e si
    ribalta il costo sulle squadre solo quando l'attribuzione copre circa l'80% della spesa. È diventata il criterio
    di accettazione della storia [0022](04-attribuzione-della-spesa/0022-da-mostrare-a-addebitare.md).
15. **OpenTelemetry — osservabilità dell'intelligenza artificiale generativa** — <https://opentelemetry.io/blog/2026/genai-observability/>
    — i nomi standard dei conteggi (`gen_ai.usage.input_tokens`, `gen_ai.usage.output_tokens`). Adottarli ci fa
    ricevere gratis i dati di chi ha già uno strumento di osservabilità.
16. **Stato delle convenzioni OpenTelemetry per l'intelligenza artificiale generativa (luglio 2026)** —
    <https://john-hodge.com/blog/opentelemetry-genai-semantic-conventions/> — avvertenza importante: al 17 luglio
    2026 **nessun** attributo di quella famiglia è dichiarato stabile, e con la versione 1.42.0 (12 giugno 2026)
    l'intera famiglia è stata spostata in un archivio dedicato. Quindi: ci si allinea, ma con una mappatura nostra
    in mezzo, perché lo standard può ancora cambiare. Fonte personale, non ufficiale: da riverificare prima di
    scrivere codice.
17. **TrueFoundry — che cos'è un punto di passaggio per i modelli** — <https://www.truefoundry.com/blog/llm-proxy>
    — rassegna delle contropartite fra punto di passaggio sincrono e osservazione asincrona; usata come
    contraddittorio alla fonte 10.

### 2.7 Cosa NON sono riuscito a determinare

- **Durata della prova gratuita dei concorrenti** — nessuna delle quattro pagine di prezzo ufficiali la dichiara:
  in questo dominio il piano gratuito *è* la prova. Servirebbe l'apertura di un account su ciascuno per saperlo.
  Conseguenza sulla proposta di listino: i 14 giorni di prova sul piano intermedio sono una convenzione della
  piattaforma appgrove, non un dato di mercato.
- **Prezzo della versione a pagamento di LiteLLM** — non rilevato su pagina ufficiale in questa ricerca. Non
  cambia la valutazione: il rischio viene dalla versione **gratuita**, che è quella che il cliente confronterebbe.
- **Notizia di un cambio di proprietà di Helicone all'inizio del 2026** — comparsa in un riassunto di risultati di
  ricerca su siti di comparazione, **non verificata su fonte ufficiale**. Se fosse vera sarebbe un'apertura di
  mercato; finché non è verificata su una pagina dell'azienda **non la si usa** per giustificare nulla.
- **Quanti clienti hanno davvero il problema alla nostra scala.** Non ho trovato un dato sul numero di imprese
  europee sotto i 50 addetti che spendono abbastanza in modelli linguistici da giustificare 39 €/mese per
  misurarlo. È il dato che decide se l'app sta in piedi (rischio R1) e nessuna ricerca in rete lo può sostituire:
  si chiude con venti conversazioni con clienti potenziali, non con un'altra ricerca.
- **Ritardo di pubblicazione dei rendiconti di Google e Mistral** — verificato solo per Anthropic (≈5 minuti) e
  OpenAI (non dichiarato). Va misurato in fase di collegamento della fonte, e infatti la storia
  [0012](02-ingresso-dei-dati-di-consumo/0012-salute-e-ritardo-delle-fonti.md) misura il ritardo osservato invece
  di fidarsi della documentazione.

---

## 3. Varco d'identità — le risposte pronte per `new-application`

> Queste sei righe sono ciò che la skill `new-application` chiede **prima** di generare qualunque cosa.
> L'identificativo dell'app finisce nel nome dello schema del database, nei nomi delle code, nella rotta pubblica e
> nell'istanza del modulo di infrastruttura: cambiarlo dopo **non è una rinomina, è una migrazione di dati**.

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `spesa_modelli` | Rispetta `^[a-z][a-z0-9_]{0,30}$` (13 caratteri). Segue la convenzione viva nel repository, dove l'app numero uno è `fatture`: identificativo tecnico **in italiano** che dice cosa l'app è — la spesa per i modelli linguistici — e non come è commercializzata («TokenGrove» è il nome di listino, e i nomi di listino cambiano). Scartati: `tokengrove` (lega lo schema al marchio), `llm` e `ai` (sigle, vietate dal repository), `token` (gergo tecnico inglese e per giunta ambiguo: nella piattaforma «token» è già il gettone di autenticazione), `costi` (troppo largo: il catalogo ha altre app di costo, per esempio 08 `notespese`). Da qui: schema `app_spesa_modelli`, rotte `/api/spesa_modelli/v1/*`, percorso end-to-end `[J-SPESA-MODELLI]`. |
| **Modello utente** | `multi` | L'app **serve** a dire chi ha speso: senza il concetto di «chi ha fatto cosa» non esiste. Chi compra (titolare o responsabile tecnico) non è chi collega le fonti (chi sviluppa) né chi legge i rapporti (chi tiene i conti): sono tre ruoli distinti nello stesso account, e i permessi contano davvero (chi vede la spesa dei colleghi? chi può definire un budget?). Un'app a utente singolo qui non avrebbe senso. |
| **Porta locale** | `8132` | Convenzione del kit: `8100 + 32`. Da confermare con `./dev.sh services` al momento dello scaffolding (nel repository sono già occupate 8081 `fatture`, 8082 `crm`, 9100 autenticazione). |
| **Metrica di quota** | `misure_registrate` | È la sola cosa che il piano limita: il numero di **misure di consumo** conservate nel periodo, cioè le righe che alimentano tutto il resto. Cresce esattamente con il valore ricevuto (più chiamate misuri, più il prodotto ti serve), è la stessa unità che usa tutto il mercato (§2.2), ed è un numero che il cliente può prevedere. Alternative scartate: *la spesa monitorata in euro* (punirebbe il cliente proprio quando ha più bisogno di noi, ed è la ragione per cui esistiamo: sarebbe una tassa sul problema), *i posti* (in questo dominio nessuno vende a posto: Helicone dichiara «posti illimitati» già nel piano intermedio), *le fonti collegate* (non cresce con il valore: tre fonti collegate sono tre, che tu spenda 20 € o 20.000 €). **Lo storico consultabile non è una seconda metrica**: è una funzionalità del piano (`features`), come fa tutto il mercato. |
| **Natura della metrica** | `flow` | Consumo su una finestra che si azzera: «500.000 misure registrate al mese». Ad aprile se ne possono registrare altre 500.000 comunque sia andato marzo, perché ogni mese è un mese nuovo di chiamate. Non è una giacenza: le misure non «occupano un posto» che si libera cancellandole — e infatti cancellarne una non ne libera il diritto di registrarne un'altra nello stesso mese. Contarla come giacenza sarebbe il difetto più grave possibile qui: il cliente arriverebbe al tetto il primo mese e resterebbe bloccato per sempre. |
| **Colore-categoria e icona** | `teal` · icona `gauge` (strumento di misura) | Coincide fra listino (`category: teal`) e modulo frontend (`accentToken`). Le app vicine di area economica del catalogo hanno preso l'ambra (03 CashGrove, 08 SpendGrove) e il rosso (19 SubGrove), quella di analisi il blu (20 InsightGrove): il verde acqua distingue TokenGrove nella barra laterale da tutte e quattro. C'è anche una ragione interna: dentro questa app l'ambra e il rosso sono i colori degli **avvisi di budget**, e sono i colori che il cliente deve imparare a temere. Se fossero anche il colore dell'app, l'allarme perderebbe forza. |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Fonte` | Un collegamento a un fornitore di modelli in sola lettura, oppure un canale di invio delle misure | tipo (`rendiconto` \| `invio`), fornitore, riferimento cifrato al segreto, stato, ultimo istante importato, ritardo osservato | no (contiene un **segreto**: la chiave di sola lettura del cliente) |
| `Misura` | Una singola chiamata a un modello, o un lotto già aggregato dal fornitore | identificativo esterno, istante, modello, conteggi (ingresso, uscita, ingresso servito da cache, scrittura in cache), costo congelato, versione di listino usata, origine, etichette | **possibile**: l'etichetta `utente_finale` e le etichette libere possono contenere l'identificativo di una persona |
| `Modello` | La riga del catalogo dei prezzi: un modello di un fornitore, valido da una data | fornitore, chiave del modello, prezzi per unità (ingresso, uscita, cache, lotto), valuta, `valido_da`, `valido_fino`, origine della riga | no |
| `VersioneListino` | Una fotografia datata dell'intero catalogo dei prezzi | numero, data di pubblicazione, origine, impronta del contenuto, chi l'ha caricata | no |
| `Dimensione` | Un asse di attribuzione dichiarato dall'account (squadra, progetto, cliente finale, funzionalità, ambiente) | chiave, nome, obbligatoria sì/no, valori ammessi | no |
| `RegolaDiAttribuzione` | Come si assegna una misura a un valore di dimensione quando l'etichetta non c'è | priorità, condizione (chiave del fornitore, progetto, spazio di lavoro, modello), assegnazioni, valida da | no |
| `Budget` | Un tetto di spesa su una dimensione e un periodo | ambito, importo, periodo, soglie di avviso, destinatari, stato | no |
| `Avviso` | L'evento «una soglia è stata superata» e il suo recapito | budget, soglia, istante, valore osservato, previsione, stato del recapito, sospensione | no |
| `Rendiconto` | Ciò che il fornitore dichiara per un giorno: la verità di fatturazione | fonte, giorno, importo dichiarato, valuta, importo nostro corrispondente, scarto | no |

**Relazioni.** `Fonte` **1→N** `Misura` e `Fonte` **1→N** `Rendiconto`. `Misura` **N→1** `Modello` (per chiave del
modello e istante, non per chiave esterna: il listino è versionato nel tempo). `Misura` **N→N** valori di
`Dimensione`, attraverso le etichette. `Budget` **1→N** `Avviso`.

Macchina a stati della `Fonte`: `da_verificare` → `attiva` → (`in_errore` ⇄ `attiva`) → `sospesa` → `scollegata`.
Il passaggio a `in_errore` non cancella nulla: le misure già raccolte restano e la fonte mostra da quando è cieca.

Macchina a stati dell'`Avviso`: `pronto` → `recapitato` → (`rientrato` | `sospeso`). Un avviso non si «riapre»: se
la soglia viene superata di nuovo nasce un avviso nuovo, così il registro racconta la storia vera.

**Il ciclo di vita della `Misura`** è la parte che tutte le storie devono rispettare, e ha una sola regola dura:

> Il costo si calcola **una volta sola**, al momento in cui la misura entra, con la versione di listino valida in
> quell'istante; poi si **congela** sulla riga insieme al numero di versione del listino. Nessuna lettura
> successiva ricalcola. Un ricalcolo esiste, è esplicito, lo chiede una persona e produce **righe nuove**, mai una
> modifica di quelle vecchie (storia [0017](03-listino-dei-fornitori-e-calcolo-del-costo/0017-ricalcolo-tracciato.md)).

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica (`deleted_at`);
schema `app_spesa_modelli`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8).

### 3.1 La decisione che decide il prodotto — come entrano i dati di consumo

> Questa sotto-sezione sta qui, e non in una storia, perché è **la** scelta architetturale dell'app: tutto il resto
> ne discende. Le tre vie possibili sono quelle indicate nel catalogo e nel mandato d'autore.

**Via A — leggere i rendiconti del fornitore.** Il cliente ci dà una chiave amministrativa di **sola lettura**; noi
interroghiamo le interfacce di consumo e costo (fonti 1-3) e ricostruiamo la spesa. *Pro*: zero righe di codice dal
cliente, valore in cinque minuti, nessuna responsabilità sul suo servizio, nessun contenuto visto, riconciliazione
naturale con la fattura. *Contro*: granularità povera — il **costo** è disponibile solo **a giorno** presso entrambi
i fornitori principali, e l'attribuzione nativa arriva al massimo a chiave/progetto/spazio di lavoro. Con la sola
via A non si può dire «questo cliente finale mi è costato 43 €», che è metà del prodotto.

**Via B — mettersi in mezzo alle chiamate (punto di passaggio).** *Pro*: granularità perfetta, attribuzione per
richiesta, ed è l'unica via che permette un **blocco vero**. *Contro*, e sono pesanti:

1. **diventiamo un componente critico del cliente**: se il nostro servizio è lento o giù, il prodotto del cliente è
   lento o giù. Un'app di misurazione che può fermare la produzione di chi la compra è un rischio sproporzionato
   rispetto al valore che dà — e per una piattaforma il cui principio è il costo minimo (nessun bilanciatore, nessuna
   alta disponibilità nelle prime fasi) è una promessa che oggi **non possiamo mantenere**;
2. **vedremmo il contenuto** di ogni richiesta e di ogni risposta: i dati dei clienti dei nostri clienti, di
   qualunque natura, comprese le categorie particolari dell'articolo 9. Diventeremmo responsabili del trattamento
   per una massa di dati di cui non abbiamo bisogno per fare il nostro lavoro;
3. **latenza e traffico**: ogni risposta passerebbe due volte in rete, e le risposte in flusso continuo vanno
   ritrasmesse pezzo per pezzo;
4. è il terreno dove **LiteLLM è gratuito, aperto e migliore di quanto potremmo essere**: competere lì sarebbe
   scegliere di perdere.

**Via C — ricevere le misure da chi già le fa.** Il prodotto del cliente, dopo ogni chiamata, ci manda un piccolo
record: quando, quale modello, quanti segni di testo in ingresso e in uscita, e le etichette di attribuzione. Niente
contenuto. L'invio è **asincrono e a perdere**: se noi non rispondiamo, il cliente non se ne accorge. *Pro*:
granularità per richiesta, attribuzione per cliente finale e per funzionalità, nessuna responsabilità sul percorso
caldo, nessun contenuto. *Contro*: richiede una riga di codice nel prodotto del cliente, e ciò che non è strumentato
non si vede.

**Decisione proposta: C come sorgente primaria, A come corredo obbligatorio, B esplicitamente fuori ambito.**

- si comincia da **A**, perché è il valore in cinque minuti e non chiede niente a nessuno: colleghi una chiave di
  sola lettura e vedi la spesa per modello e per giorno. È l'aggancio della prova gratuita;
- si passa a **C** per avere ciò che A non può dare: il costo per cliente finale, per funzionalità, per squadra. È
  il momento in cui il prodotto smette di essere un cruscotto e comincia a valere un abbonamento;
- **A resta acceso anche dopo**, come controllo: ogni giorno si confronta la somma delle misure con il rendiconto
  del fornitore e si mostra lo scarto (storia [0011](02-ingresso-dei-dati-di-consumo/0011-riconciliazione-fra-misure-e-rendiconto.md)).
  È la funzione che rende il prodotto credibile: senza, siamo un secondo numero che non torna con la fattura;
- **B è fuori ambito e va detto sulla pagina del prodotto**, non nascosto: «TokenGrove non sta in mezzo alle tue
  chiamate; se il nostro servizio si ferma, il tuo prodotto continua a funzionare». È una promessa commerciale,
  non solo una scelta tecnica.

**Conseguenza da guardare in faccia.** Senza la via B **non possiamo fermare le chiamate di nessuno**, e quindi il
«tetto di spesa» del catalogo diventa un tetto **consultivo**: avvisiamo presto e bene, e mettiamo a disposizione un
semaforo che il codice del cliente può interrogare per fermarsi **da solo**. Chi ferma le chiamate è il cliente, mai
noi. Vedi la storia [0027](05-budget-avvisi-e-anomalie/0027-semaforo-del-budget-consultabile.md), che tratta questo
punto per quello che è: un effetto pesante, che si dà in mano a chi ne porta la responsabilità.

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il
> file `services/core/src/main/resources/pricing/spesa_modelli.yaml`.

**Ragionamento.** Tre vincoli stringono da tre lati. (1) Il **tetto di mercato**: Langfuse Core costa 29 $/mese ed
è un prodotto più maturo sul terreno tecnico; sopra i ~40 €/mese si compete male. (2) Il **pavimento della
piattaforma**: sotto i ~5 €/mese la parte fissa per transazione dell'incasso pesa troppo, quindi non esistono piani
da 3 €. (3) Il **divieto di addebito a consumo**: dove i concorrenti fanno pagare lo sforamento, noi **blocchiamo a
`429`**, quindi i tetti devono essere generosi abbastanza da non bloccare un cliente normale — un tetto stretto qui
non produce ricavo aggiuntivo, produce un cliente arrabbiato. Il salto fra i piani lo fa quindi lo **storico
consultabile**, che è ciò che il mercato usa (§2.2) ed è anche ciò che costa davvero a noi (spazio su disco e
interrogazioni su serie lunghe).

| Piano | Prezzo mensile | Prezzo annuale | Limite sulla metrica `misure_registrate` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | 25.000 al mese · storico **30 giorni** · 1 fonte di rendiconto · nessun budget | — | Chi vuole vedere se il problema che ha è quello che risolviamo. Basta per collegare la chiave e guardare un mese di spesa; non basta per governarci un'azienda (niente budget, niente confronto con l'anno scorso) |
| `pro` | € 39 | € 390 (= 10× il mensile, «due mesi in regalo») | 500.000 al mese · storico **13 mesi** · fonti illimitate · budget e avvisi · posti illimitati | 14 giorni | L'impresa che ha messo un modello dentro il prodotto: vuole l'attribuzione per cliente e funzionalità, e i 13 mesi di storico per confrontare con lo stesso mese dell'anno prima |
| `team` | € 149 | € 1.490 | 3.000.000 al mese · storico **25 mesi** · più organizzazioni di fornitore · rapporti programmati e ribaltamento interno | 14 giorni | Chi ha più squadre o più clienti da addebitare e usa i numeri per fatturare |

**Note obbligate.**

- **Tre piani, non di più.** Aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile.
- **Un limite lasciato vuoto significa illimitato**, non zero. Qui nessun limite è vuoto: sono tutti dichiarati.
- **La prova gratuita su un'app che ha già un piano gratuito**: qui **non** è ridondante, perché i due piani non
  danno le stesse cose. Il piano gratuito non ha i budget né lo storico lungo, che sono la ragione per pagare: i 14
  giorni servono a far provare *quelli*. Va però detto che è un caso limite e che la scelta è dello sviluppatore.
- **Costo effettivo dell'incasso**: nessun piano è sotto i 5 €/mese, quindi il segnale non scatta. Il piano annuale
  a 390 € riduce ulteriormente il peso della parte fissa ed è quello da mettere in evidenza.
- **Prezzi immutabili una volta vivi**: un cambio di prezzo si fa creando un prezzo nuovo e archiviando il vecchio;
  gli abbonati restano sul loro (skill `pricing-change`).
- **Attenzione al doppio senso della parola «listino» in questa app**: c'è il listino di appgrove (questo, che è
  codice nel repository e cambia con un rilascio) e il **catalogo dei prezzi dei fornitori di modelli** (dato
  aggiornabile senza rilascio, epica 03). Sono due cose diverse e nei documenti vanno chiamate con nomi diversi.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/spesa_modelli.yaml`) si compila **insieme** allo sviluppatore: «niente contratto,
> niente produzione». Un manifesto inventato è peggio di uno assente, perché sembra conformità ed è finzione.

**Categorie particolari (articolo 9): nessuna, ed è una scelta di progetto, non un caso.** L'unico punto da cui
potrebbero entrare è il **contenuto delle richieste ai modelli** — che può contenere qualunque cosa, compresi dati
sanitari o convinzioni personali. Per questo il contratto della misura (storia
[0008](02-ingresso-dei-dati-di-consumo/0008-contratto-della-misura-di-consumo.md)) **non prevede alcun campo per il
contenuto**, e il ricevitore **scarta la misura** se ne trova uno: non lo tronca, non lo oscura, la respinge con un
errore che spiega perché. È la ragione principale per cui la via B del §3.1 è fuori ambito.

**Il principio, in una riga: si misura senza conservare il contenuto.** Va scritto così anche nella pagina pubblica
del prodotto e nell'informativa, perché è una promessa, non un dettaglio.

**Categorie trattate**

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `misura.etichetta_utente_finale` | `misura`, colonna delle etichette | utente finale del **cliente del nostro cliente** | identificativo indiretto (pseudonimo scelto dal cliente; può però essere un indirizzo di posta se il cliente lo mette) | attribuire la spesa a chi l'ha generata | esecuzione del contratto con il cliente, che agisce da titolare | quanto lo storico del piano (30 giorni / 13 mesi / 25 mesi), poi cancellazione fisica |
| `misura.etichetta_cliente` | `misura`, colonna delle etichette | cliente finale del nostro cliente | può essere la **ragione sociale di una ditta individuale**, quindi dato personale | costo e margine per cliente | esecuzione del contratto | come sopra |
| `misura.autore` | `misura` | addetto dell'account | identificativo dell'utente della piattaforma | sapere chi ha generato la chiamata | esecuzione del contratto | come sopra |
| `fonte.creata_da`, `budget.destinatari` | `fonte`, `budget` | addetti dell'account | identificativo dell'utente, indirizzo di posta per il recapito degli avvisi | sapere chi ha collegato cosa; recapitare l'avviso | esecuzione del contratto | finché la fonte o il budget esistono, poi cancellazione |
| `fonte.segreto` | archivio dei segreti, **non** in tabella | non è un dato personale, è una **credenziale del cliente** | chiave amministrativa di sola lettura verso il fornitore di modelli | leggere il rendiconto | esecuzione del contratto | fino allo scollegamento della fonte; cancellazione immediata allo scollegamento |

**Esportazione e cancellazione.** Tabelle che contengono dati riferibili a persone e che **devono** comparire sia in
`exportData` sia in `purgeData` del contratto dati dell'app (`SpesaModelliDataContract`): `misura` (etichette e
autore), `fonte` (chi l'ha creata), `budget` (destinatari degli avvisi), `avviso` (registro dei recapiti),
`regola_di_attribuzione` (chi l'ha scritta). Dimenticarne una è il difetto di conformità più probabile.
La cancellazione è **fisica**: sostituire un'etichetta con un codice non è cancellare. Attenzione al caso
particolare di questa app: cancellare l'etichetta di una misura **non deve cancellare la misura**, perché il totale
di spesa è un dato contabile del nostro cliente; la via corretta è cancellare l'etichetta e far confluire la misura
nel «non attribuito», dichiarandolo.

**Testo libero.** Le etichette sono l'unico ingresso non presidiato: sono testo scelto dal cliente e possono
contenere qualunque cosa. Presidi proposti: lunghezza massima breve (per esempio 120 caratteri), avviso
nell'interfaccia e nella documentazione che le etichette **non sono un posto per dati personali**, e — questa è una
proposta da valutare, non una decisione — un controllo che segnala (senza bloccare) i valori che hanno la forma di
un indirizzo di posta, suggerendo di sostituirli con un pseudonimo.

**Integrazioni esterne.** Le fonti dei fornitori di modelli **non** sono responsabili del trattamento per nostro
conto: il rapporto è rovesciato, siamo noi a leggere dal fornitore **del cliente**, con una credenziale del cliente,
e nessun dato personale viaggia da noi verso di loro. Va comunque scritto nell'informativa che TokenGrove si collega
a servizi terzi indicati dal cliente. Il recapito degli avvisi usa il fornitore di posta già in uso dalla
piattaforma: nessun fornitore nuovo.

**Ruolo di appgrove: punto da confermare, non deciso.** Per le etichette che descrivono i clienti finali e gli
utenti finali del nostro cliente, appgrove agisce con ogni probabilità come **responsabile del trattamento** per
conto del cliente titolare — mentre per i dati degli addetti dell'account resta la posizione ordinaria della
piattaforma. La distinzione cambia l'accordo sul trattamento dei dati e l'informativa: è una classificazione
**materialmente ambigua** e per regola si ferma qui e la decide lo sviluppatore, eventualmente con la revisione
legale.

**Classificazione della change.** Una app nuova introduce finalità nuove e categorie nuove: è un cambiamento
**sostanziale**. Lo confermo. Non ci sono elementi per sostenere il contrario, e il fatto che i dati siano pochi non
rende il cambiamento non sostanziale: cambia il *chi* (compaiono gli utenti finali dei clienti), non solo il quanto.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `leggi_spesa` | `(periodo, raggruppamento?, filtro?) → tavola di importi con la loro copertura di attribuzione` | Il totale speso, scomposto come chiesto | lettura | no |
| `elenca_maggiori_consumatori` | `(periodo, dimensione, quanti?) → elenco ordinato` | Chi ha speso di più su un asse | lettura | no |
| `confronta_costo_modelli` | `(periodo, modelli[], per_unita?) → tavola comparativa` | Quanto costa la stessa cosa su modelli diversi, ai prezzi datati | lettura | no |
| `stato_budget` | `(budget?) → semaforo, consumato, previsione di fine periodo` | Come sta andando rispetto al tetto | lettura | no |
| `spiega_impennata` | `(periodo) → scomposizione del salto per modello, etichetta e ora` | Perché la spesa è salita | lettura | no |
| `stato_fonti` | `() → elenco delle fonti con ritardo osservato e scarto di riconciliazione` | Se i numeri sono affidabili adesso | lettura | no |
| `definisci_budget` | `(ambito, importo, periodo, soglie) → bozza di budget` | Crea un tetto e i suoi avvisi | scrittura | **sì** |
| `crea_regola_di_attribuzione` | `(condizione, assegnazioni, valida_da) → bozza di regola` | Assegna la spesa non attribuita | scrittura | **sì** |
| `applica_regola_allo_storico` | `(regola, periodo) → bozza con quante misure cambierebbero attribuzione` | **Riscrive l'attribuzione di dati passati** su cui il cliente può aver già fatturato | scrittura irreversibile | **sì, obbligatoria, con l'anteprima del numero di righe toccate** |
| `sospendi_avvisi` | `(budget, fino_a, motivo) → bozza` | Zittisce un avviso: può nascondere uno sfondamento vero | scrittura | **sì** |

**Perché il livello conversazionale rende questa app più utile delle concorrenti.** Perché le domande di questo
dominio sono domande, non cruscotti: «quanto ho speso questa settimana per il cliente Rossi?», «perché martedì è
costato il triplo?», «se continuo così, dove arrivo a fine mese?». Sono esattamente le tre domande dell'indagine
Harness (§2.6, fonte 12), e con un cruscotto si risponde male: bisogna sapere in anticipo quale filtro mettere. In
chat si risponde bene. È anche l'unico modo in cui il titolare — che non aprirà mai un pannello di osservabilità —
usa davvero questo prodotto.

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine l'app è accesa, vuota e utilizzabile: servizio in piedi, schema del database, modulo nella barra laterale,
piano e quota funzionanti, avvio locale senza cablaggi a mano.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | Istanza di scaffolding, rotte `/api/spesa_modelli/v1/*`, definizione delle interfacce, infrastruttura dal modulo comune |
| [0002](01-fondamenta/0002-modello-dati-multi-account.md) | Modello dati multi-account | Schema `app_spesa_modelli`, tabelle di base, `tenant_id` e colonne di controllo, prove di isolamento |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Manifesto, registrazione, sezioni, cinque lingue, tema chiaro e scuro |
| [0004](01-fondamenta/0004-abbonamento-e-quota-sulle-misure.md) | Abbonamento e quota sulle misure | Proiezione dell'abilitazione, prenotazione della metrica `misure_registrate`, `429` con rimedio |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | `./dev.sh services` vede l'app; fornitore di modelli simulato in locale; dati inventati |

### Epica 02 — Ingresso dei dati di consumo

È l'epica che decide il prodotto (§3.1). Si apre con la via che dà valore in cinque minuti — leggere il rendiconto
del fornitore — e prosegue con quella che dà l'attribuzione fine: ricevere le misure da chi le fa.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-ingresso-dei-dati-di-consumo/0006-collegamento-di-una-fonte-in-sola-lettura.md) | Collegamento di una fonte in sola lettura | Il cliente incolla una chiave amministrativa di sola lettura, noi la verifichiamo e la custodiamo cifrata |
| [0007](02-ingresso-dei-dati-di-consumo/0007-importazione-dei-rendiconti.md) | Importazione dei rendiconti del fornitore | Recupero periodico di consumo e costo a intervalli, con cursore, ripresa e rispetto dei limiti di interrogazione |
| [0008](02-ingresso-dei-dati-di-consumo/0008-contratto-della-misura-di-consumo.md) | Contratto della misura di consumo | Il formato del record: cosa entra, cosa è vietato, e il rifiuto esplicito del contenuto |
| [0009](02-ingresso-dei-dati-di-consumo/0009-ricevitore-delle-misure.md) | Ricevitore delle misure e chiave di invio | Ricezione a lotti che non sta sul percorso caldo del cliente e non lo può fermare |
| [0010](02-ingresso-dei-dati-di-consumo/0010-deduplica-e-arrivi-in-ritardo.md) | Deduplica e arrivi in ritardo | Stesso record due volte conta una volta; un record vecchio di due giorni finisce nel giorno giusto |
| [0011](02-ingresso-dei-dati-di-consumo/0011-riconciliazione-fra-misure-e-rendiconto.md) | Riconciliazione fra misure e rendiconto | Ogni giorno si confronta ciò che abbiamo misurato con ciò che il fornitore dichiara, e si mostra lo scarto |
| [0012](02-ingresso-dei-dati-di-consumo/0012-salute-e-ritardo-delle-fonti.md) | Salute e ritardo delle fonti | Semaforo per fonte con ritardo misurato sul campo, non dichiarato dalla documentazione |

### Epica 03 — Listino dei fornitori e calcolo del costo

I prezzi dei modelli cambiano più in fretta di un ciclo di rilascio. Alla fine di questa epica il catalogo dei
prezzi è un **dato datato e aggiornabile senza toccare il codice**, e i conti già fatti non cambiano mai da soli.

| # | Storia | In una riga |
|---|---|---|
| [0013](03-listino-dei-fornitori-e-calcolo-del-costo/0013-catalogo-dei-prezzi-datato.md) | Catalogo dei prezzi datato | Il listino dei fornitori è un dato con validità nel tempo, caricabile senza rilascio |
| [0014](03-listino-dei-fornitori-e-calcolo-del-costo/0014-calcolo-e-congelamento-del-costo.md) | Calcolo e congelamento del costo | Il costo si calcola una volta all'ingresso e resta scolpito sulla riga con la versione di listino usata |
| [0015](03-listino-dei-fornitori-e-calcolo-del-costo/0015-modello-sconosciuto-e-prezzo-mancante.md) | Modello sconosciuto e prezzo mancante | Un prezzo che non c'è non si inventa: si dichiara, si conta a parte e si chiede di colmarlo |
| [0016](03-listino-dei-fornitori-e-calcolo-del-costo/0016-prezzi-negoziati-per-account.md) | Prezzi negoziati per account | Chi ha uno sconto dal fornitore sovrascrive il prezzo pubblico, con la stessa disciplina di date |
| [0017](03-listino-dei-fornitori-e-calcolo-del-costo/0017-ricalcolo-tracciato.md) | Ricalcolo tracciato dello storico | Rifare i conti è un'azione esplicita, tracciata, che produce righe nuove e non tocca quelle vecchie |

### Epica 04 — Attribuzione della spesa

A chi si imputa la spesa, e come si impedisce che «non attribuito» diventi la voce più grande.

| # | Storia | In una riga |
|---|---|---|
| [0018](04-attribuzione-della-spesa/0018-dimensioni-di-attribuzione.md) | Dimensioni di attribuzione | L'account dichiara i propri assi: squadra, progetto, cliente finale, funzionalità, ambiente |
| [0019](04-attribuzione-della-spesa/0019-etichette-sulla-misura.md) | Etichette sulla misura | Le etichette arrivano con la misura, sono validate e non sono un posto per dati personali |
| [0020](04-attribuzione-della-spesa/0020-regole-di-mappatura-dalle-chiavi.md) | Regole di mappatura dalle chiavi | Ciò che arriva dal rendiconto si attribuisce per regola: da chiave, progetto o spazio di lavoro |
| [0021](04-attribuzione-della-spesa/0021-il-non-attribuito.md) | Il non attribuito | Copertura di attribuzione come numero in evidenza, con la via per ridurla in tre clic |
| [0022](04-attribuzione-della-spesa/0022-da-mostrare-a-addebitare.md) | Da mostrare a addebitare | Il ribaltamento interno si accende solo quando la copertura è alta abbastanza da reggerlo |

### Epica 05 — Budget, avvisi e anomalie

Un avviso serve **prima** che il conto arrivi. E poiché non siamo in mezzo alle chiamate, chi ferma le chiamate è
il cliente: noi gli diamo il semaforo e gli diciamo cosa significa fermarsi.

| # | Storia | In una riga |
|---|---|---|
| [0023](05-budget-avvisi-e-anomalie/0023-definizione-di-un-budget.md) | Definizione di un budget | Un tetto su una dimensione e un periodo, con le sue soglie e i suoi destinatari |
| [0024](05-budget-avvisi-e-anomalie/0024-previsione-di-fine-periodo.md) | Previsione di fine periodo | «Di questo passo chiudi a 1.240 €»: il numero che rende l'avviso preventivo invece che postumo |
| [0025](05-budget-avvisi-e-anomalie/0025-recapito-e-sospensione-degli-avvisi.md) | Recapito e sospensione degli avvisi | Un avviso per soglia, mai una tempesta; la sospensione è tracciata perché nasconde uno sfondamento |
| [0026](05-budget-avvisi-e-anomalie/0026-rilevazione-delle-impennate.md) | Rilevazione delle impennate | Il ciclo di ritentativi impazzito si vede in un'ora, non a fine mese |
| [0027](05-budget-avvisi-e-anomalie/0027-semaforo-del-budget-consultabile.md) | Semaforo del budget consultabile | L'unico «tetto» che possiamo dare: un semaforo che il cliente interroga e usa per fermarsi da solo |

### Epica 06 — Cruscotti, confronti e rapporti

Ciò che si guarda tutti i giorni e ciò che si manda a chi non entrerà mai nell'app.

| # | Storia | In una riga |
|---|---|---|
| [0028](06-cruscotti-confronti-e-rapporti/0028-panoramica-della-spesa.md) | Panoramica della spesa | I primi sessanta secondi: quanto, per cosa, dove va a finire |
| [0029](06-cruscotti-confronti-e-rapporti/0029-confronto-del-costo-fra-modelli.md) | Confronto del costo fra modelli | Quanto costerebbe lo stesso lavoro su un altro modello, ai prezzi datati |
| [0030](06-cruscotti-confronti-e-rapporti/0030-esportazione-della-spesa-attribuita.md) | Esportazione della spesa attribuita | La tavola che finisce nel foglio di calcolo e nella nota di addebito |
| [0031](06-cruscotti-confronti-e-rapporti/0031-rapporto-periodico-programmato.md) | Rapporto periodico programmato | Il riepilogo che arriva il primo del mese a chi non apre l'app |

### Epica 07 — Esposizione conversazionale e prove end-to-end

| # | Storia | In una riga |
|---|---|---|
| [0032](07-esposizione-conversazionale-e-prove/0032-contratto-degli-strumenti-di-lettura.md) | Contratto degli strumenti di lettura | Sei strumenti di sola lettura, con schema dei parametri e del risultato |
| [0033](07-esposizione-conversazionale-e-prove/0033-strumenti-di-scrittura-con-conferma.md) | Strumenti di scrittura con conferma | Bozza e conferma umana; l'attribuzione riscritta sullo storico è il caso più delicato |
| [0034](07-esposizione-conversazionale-e-prove/0034-percorso-end-to-end-e-registro-di-copertura.md) | Percorso end-to-end e registro di copertura | Il percorso `[J-SPESA-MODELLI]` dal collegamento della fonte all'avviso, e il registro aggiornato |
| [0035](07-esposizione-conversazionale-e-prove/0035-esportazione-e-cancellazione-dei-dati.md) | Esportazione e cancellazione dei dati personali | Il contratto dati dell'app: tutte le tabelle, esportazione e cancellazione fisica |

**Totale**: 7 epiche, 35 storie.

---

## 9. Estensioni della console di amministrazione

Servono, ed è una conseguenza diretta della §3.1: l'app dipende da servizi di terzi che il cliente ci fa
interrogare, quindi chi amministra deve poter rispondere a «perché al cliente non arrivano più i numeri?» senza
guardare i suoi dati. Servono inoltre una vista sullo stato del catalogo dei prezzi (quanto è vecchio, quanti
modelli sconosciuti sta generando in tutta la piattaforma) e la deroga temporanea sulla quota per chi importa uno
storico lungo il primo giorno.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| 20 — InsightGrove | alimenta | Il costo dei modelli per cliente finale è una misura da pubblicare come fatto verso l'app degli indicatori: entra nel calcolo del margine per cliente. Solo a eventi, mai chiamata diretta |
| 08 — SpendGrove (note spese) | adiacente, nessuna sovrapposizione | Entrambe parlano di spesa, ma SpendGrove tratta i rimborsi agli addetti: nessuna entità in comune |
| 39 — SpendGrove SaaS (abbonamenti software) | **si sovrappone in parte** | Chi governa gli abbonamenti software vorrà vedere anche la spesa per i modelli come voce di costo tecnologico. Confine proposto: 39 tratta gli **abbonamenti** (contratti ricorrenti, rinnovi), 32 tratta il **consumo** (spesa a misura). Se un giorno le due si fondessero, la fusione va decisa prima, non dopo |
| 31 — MCPGrove (sicurezza degli agenti) | complementare | Chi mette in sicurezza le chiamate degli agenti vede passare gli stessi eventi che noi misuriamo. Sinergia possibile: la misura potrebbe arrivarci da lì invece che dal prodotto del cliente. Rimando: non è del perimetro di questa app |
| 02 — BillGrove / catena del documento | nessun rapporto diretto | La spesa per i modelli non è un documento commerciale nostro; il ribaltamento sul cliente finale produce **numeri**, non fatture (§ epica 04, storia 0022) |

**Lettura.** L'app **sta in piedi da sola** — è anzi l'unica del catalogo che un cliente potrebbe comprare senza
comprare nient'altro di appgrove, perché il suo problema non è né la fatturazione né i clienti né il magazzino. È
allo stesso tempo il suo punto debole commerciale: se sta da sola, compete da sola, e compete con prodotti gratuiti
(§2.1). La sinergia con InsightGrove è la sola che sposti l'ago, perché trasforma «quanto spendo in modelli» in
«quanto guadagno su questo cliente».

Nessuna delle entità condivise indicate dal catalogo (anagrafica clienti, catalogo prodotti, anagrafica dipendenti,
catena preventivo → fattura) è toccata da questa app. L'etichetta «cliente finale» è **testo**, non un riferimento
all'anagrafica di un'altra app: legarla sarebbe una chiamata fra app, che è vietata.

**Sovrapposizioni da evitare.** Con l'app 39 (abbonamenti software), come detto sopra. E con la sezione
«Fatturazione» del backoffice appgrove, che riguarda ciò che il cliente paga **a noi**: qui si parla di ciò che il
cliente paga **ai fornitori di modelli**. Sono due cose diverse con parole quasi identiche e nei testi
dell'interfaccia va tenuta la distinzione (la parola «spesa» per la seconda, mai «fattura»).

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| P1 | Prezzi, tetti e durata della prova (§5) | Fermata di escalation: nessun agente fissa i prezzi | sviluppatore |
| P2 | **La scheda di catalogo prevede l'espansione tramite sforamento a pagamento; la piattaforma lo vieta** e impone il blocco a `429` (§2.2, §5) | È un conflitto fra la strategia commerciale scritta nel catalogo e un invariante di piattaforma. O si rinuncia a quel segnale di traction, o si cambia l'invariante — e la seconda è una decisione ben più grande di questa app | sviluppatore |
| P3 | Ruolo di appgrove sulle etichette che descrivono clienti e utenti finali: titolare o responsabile del trattamento (§6) | Classificazione materialmente ambigua; cambia l'accordo sul trattamento dei dati e l'informativa | sviluppatore, eventualmente con la revisione legale |
| P4 | Il controllo che segnala le etichette con forma di indirizzo di posta (§6) | È un presidio utile ma può risultare invadente o dare falsi allarmi; è anche il primo passo verso un rilevamento di contenuto, che l'app dichiara di non fare | sviluppatore |
| P5 | Se e quando aprire la via B — il punto di passaggio (§3.1) | Oggi è fuori ambito con motivazione forte. Se un giorno la piattaforma avrà alta disponibilità e il mercato chiederà il blocco vero, la decisione va ripresa **da capo**, non fatta scivolare dentro | sviluppatore, dopo la prova sul mercato |
| P6 | Da dove arriva il catalogo dei prezzi dei fornitori la prima volta e chi lo mantiene (§ epica 03) | Compilarlo a mano per decine di modelli è lavoro ricorrente; riprendere un file pubblico di terzi (per esempio quello di LiteLLM) va valutato sotto il profilo della licenza e dell'affidabilità | sviluppatore, prima della storia [0013](03-listino-dei-fornitori-e-calcolo-del-costo/0013-catalogo-dei-prezzi-datato.md) |
| P7 | Valuta: i fornitori dichiarano in dollari, il cliente ragiona in euro | Converti quando? Al giorno della chiamata (corretto ma richiede una serie storica dei cambi) o alla fattura? Un cambio congelato è coerente con il congelamento del costo, ma serve una fonte dei cambi — cioè un fornitore esterno nuovo | sviluppatore, prima della storia [0014](03-listino-dei-fornitori-e-calcolo-del-costo/0014-calcolo-e-congelamento-del-costo.md) |
| P8 | Adozione delle convenzioni OpenTelemetry per l'intelligenza artificiale generativa | Al luglio 2026 nessun attributo è dichiarato stabile e la famiglia è stata spostata di archivio (§2.6, fonte 16): allinearsi è giusto, dipenderne è rischioso | storia [0008](02-ingresso-dei-dati-di-consumo/0008-contratto-della-misura-di-consumo.md), con mappatura propria in mezzo |

**Rischi noti**

- **R1 — La concorrenza aperta e gratuita, che è il rischio numero uno e va guardato in faccia.** Langfuse e
  LiteLLM si fanno girare in casa gratis; Helicone regala 10.000 richieste al mese; OpenRouter restituisce il costo
  **dentro la risposta** e permette una chiave con tetto di spesa per cliente. *Effetto se si avvera*: nessuno paga,
  perché il 90% del valore è disponibile a zero. *Cosa lo attenua, in ordine di forza*: (1) il segmento — chi non ha
  una squadra che possa far girare e aggiornare un servizio in casa, per cui «gratis in licenza» non è «gratis in
  lavoro»; (2) l'attribuzione **economica** invece che tecnica: costo per cliente finale, margine, ribaltamento —
  che nella metà tecnica del mercato si costruisce a mano e nella metà economica costa da grande impresa; (3) la
  suite: la spesa per i modelli come voce di costo accanto alle altre, dentro il backoffice dove il titolare guarda
  già i suoi numeri; (4) la residenza europea dei dati e la promessa di non conservare il contenuto.
  **E il contrario, detto per intero: per un cliente che è una squadra di sviluppo con competenze proprie, non ho
  trovato una ragione onesta per pagare.** Quel cliente mette in piedi Langfuse in un pomeriggio e ottiene di più.
  Se il segmento «impresa che usa i modelli senza una squadra che possa gestirsi gli attrezzi» non è abbastanza
  grande in Europa, questa app non sta in piedi — e nessuna funzione in più la salva. È il primo punto da verificare
  con clienti veri, prima di scrivere codice.
- **R2 — La misura vale quanto la strumentazione.** Ciò che il cliente non strumenta non si vede, e quindi il
  «non attribuito» cresce in silenzio. *Attenuazione*: la copertura di attribuzione è un indicatore in prima pagina
  e la riconciliazione col rendiconto (0011) rende visibile ciò che manca — è la ragione per cui quelle due storie
  non sono opzionali.
- **R3 — Il catalogo dei prezzi invecchia.** Se il listino è vecchio, i conti sono sbagliati e il prodotto perde
  la sola cosa che vende: la credibilità del numero. *Attenuazione*: la data del listino è mostrata accanto ai
  totali, non nascosta in un pannello di configurazione; un listino vecchio oltre una soglia è un avviso.
- **R4 — Il rapporto con la fattura del fornitore.** Il nostro numero e la fattura non torneranno mai esatti
  (crediti, sconti, livelli di servizio, arrotondamenti). *Attenuazione*: non promettere mai «uguale alla fattura»,
  mostrare sempre lo **scarto** e da cosa dipende (0011). Un prodotto che dichiara lo scarto è più credibile di uno
  che finge di non averlo.
- **R5 — Dipendenza da interfacce di terzi che cambiano.** Le interfacce di consumo dei fornitori sono giovani e
  cambiano; alcune hanno limiti di interrogazione stretti (Anthropic raccomanda una al minuto). *Attenuazione*:
  un adattatore per fornitore, mai chiamate sparse nel codice, e il ritardo misurato sul campo (0012).

**Fuori dimensionamento**: non applicabile. 7 epiche (fascia 4-7), da 4 a 7 storie per epica (fascia 4-8), 35 storie
in tutto (fascia 20-45).
