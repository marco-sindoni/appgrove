# AuditGrove — descrizione dell'applicazione

**Numero di catalogo**: 31 · **Tipo**: orizzontale · governo delle azioni automatiche · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 31](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 0. La domanda che va risolta prima di tutto — il perimetro

> 🛑 **Fermata di escalation — direzione di prodotto.** Questa sezione contiene **una presa di posizione motivata**,
> non una decisione presa. La decisione spetta allo sviluppatore, e va presa **prima** di scrivere una riga di
> codice: le due risposte producono due prodotti diversi, con due modelli di dati diversi e due mercati diversi.

**La domanda.** AuditGrove registra le azioni che gli agenti automatici compiono **dentro la piattaforma appgrove**,
oppure è un prodotto che il cliente compra per registrare le azioni che i **suoi** agenti compiono nei **suoi**
sistemi?

**Le due risposte, per quello che sono.**

| | A — registro della piattaforma | B — registro dei sistemi del cliente |
|---|---|---|
| Che cos'è | una funzione di appgrove | un'applicazione venduta a chi ha agenti propri |
| Chi scrive nel registro | il livello conversazionale di appgrove | gli agenti del cliente, che dichiarano ciò che fanno |
| Modello dati | l'azione è una chiamata a uno strumento **di un'app appgrove**: si conosce lo schema, il tenant, l'utente, l'app | l'azione è una chiamata a uno strumento **qualsiasi**: nome libero, parametri liberi, sistemi che non controlliamo |
| Disponibilità a pagare | quasi nulla: è ciò che il cliente si aspetta sia già incluso | reale: il rischio da coprire è suo |
| Mercato | i clienti appgrove che useranno il livello conversazionale (oggi: nessuno, l'epica 12 non è implementata) | chiunque abbia agenti in produzione |
| È già dovuta? | **sì**: [UC 0065](../../12-ready-for-ai-mcp/0065-sicurezza-audit-invocazioni-ai.md) impone già alla piattaforma di tracciare ogni invocazione dell'assistente | no |

**La posizione che propongo: B — il registro dei sistemi del cliente**, con la piattaforma appgrove come **prima
sorgente collegata**, non come perimetro.

Le ragioni, in ordine di peso:

1. **La risposta A vende ciò che è già dovuto.** UC 0065 stabilisce che la piattaforma emetta un evento di audit
   per ogni invocazione arrivata dall'assistente, con esito dei varchi e postura di minimizzazione. È un obbligo di
   piattaforma, non un prodotto: farselo pagare a parte è chiedere al cliente un canone per vedere il registro di
   ciò che già gli appartiene. Sarebbe anche un pessimo segnale commerciale.
2. **Il dolore che la scheda di catalogo cita non accade dentro appgrove.** L'incidente che rende questa categoria
   credibile — un agente che cancella una base di dati di produzione durante un blocco delle modifiche — accade nei
   sistemi del cliente, dove gli agenti hanno mano libera. Un registro che copre solo appgrove non lo avrebbe visto.
3. **La disponibilità a pagare sta sul rischio proprio.** Si paga per potersi difendere: davanti a un cliente, a un
   revisore, a un'assicurazione, a un giudice. Il rischio da difendere è quello dei propri sistemi.
4. **Il vantaggio strutturale resta intero, e anzi funziona meglio nella risposta B.** appgrove è nativamente
   conversazionale: ogni sua app espone (esporrà) i propri strumenti. Questo significa che il cliente appgrove ha
   **una sorgente già cablata**, che si accende con un interruttore e riempie il registro dal primo giorno — mentre
   i concorrenti chiedono di mettere un intermediario di rete davanti ai propri agenti prima di vedere una riga.
   È il miglior punto di partenza possibile, ma è un *vantaggio di adozione*, non la definizione del prodotto.
5. **La risposta B è più difficile, e la difficoltà è il fossato.** Registrare azioni di sistemi che non
   controlliamo obbliga ad affrontare i tre problemi veri di questo prodotto: l'inalterabilità (§4.3), il conflitto
   fra prova e cancellazione (§6.2), e i parametri che possono contenere qualunque cosa (§6.3). Chi li risolve ha
   un prodotto; chi li aggira ha un elenco di righe.

**Conseguenza di progetto che discende dalla scelta B, e che va detta subito.** Se le azioni nascono fuori,
AuditGrove **non può intercettarle**: non sta in mezzo al traffico del cliente. Il registro è **cooperativo** —
gli agenti (o l'adattatore che li avvolge) *dichiarano* ciò che stanno per fare e ciò che hanno fatto. Questo ha
un limite che non va nascosto: **AuditGrove prova ciò che è stato dichiarato, non ciò che non è mai stato
dichiarato**. Le contromisure sono tre, e sono tre storie (0011, 0020, 0023): numerazione di sequenza per
sorgente con rilevazione dei buchi, richiesta di nulla osta *prima* dell'azione rischiosa, riconciliazione fra
nulla osta concesso ed esito dichiarato. Il perché non si fa un intermediario di rete sta al §1, «Cosa NON fa».

**Da confermare**: la scelta A/B, la rinuncia all'intercettazione in linea, e il fatto che la sorgente nativa
appgrove **non** sia venduta come prodotto ma inclusa. Chi chiude: lo sviluppatore.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** AuditGrove è il **registro delle azioni degli agenti automatici** di un'azienda, e il posto dove si
decide quali di quelle azioni hanno bisogno del consenso di una persona. Ogni volta che un agente sta per usare
uno strumento — cancellare qualcosa, mandare un pagamento, scrivere in una base di dati, spedire un messaggio a un
cliente — l'agente lo dichiara ad AuditGrove, che risponde con un **nulla osta**: *procedi*, *non procedere*,
oppure *aspetta, questa la deve approvare una persona*. Quello che poi accade viene scritto in un registro **in
sola aggiunta**, dove ogni riga è legata alla precedente da un'impronta crittografica: si può dimostrare che
nessuno — nemmeno noi — ha riscritto la storia dopo il fatto. Il registro risponde a sei domande su ogni azione:
**chi** l'ha chiesta, **quale strumento** è stato usato, **con quali parametri**, **quale effetto** ha prodotto,
**chi l'ha approvata** (o il fatto che nessuno l'abbia fatto), **che cosa ha letto e che cosa ha scritto**.

**Per chi.** Micro-imprese da 1 a 10 addetti e piccole imprese fino a 50, mercato globale con priorità europea.
Chi compra non è il titolare distratto: è **la persona che risponde se un agente combina un guaio** — in
un'azienda di dieci persone è il socio tecnico, il responsabile dei sistemi informativi, a volte il consulente
esterno che ha messo in piedi gli agenti. Chi usa tutti i giorni è la stessa persona più chi approva: il
responsabile amministrativo che riceve la richiesta «l'agente vuole emettere una nota di credito da 4.000 €,
approvi?». Il profilo tipico ha da uno a cinque agenti in produzione, spesso costruiti con strumenti diversi, e
**nessuno il cui mestiere sia sorvegliarli**.

**Quale problema toglie.** Oggi chi ha messo agenti in produzione risponde alla domanda «cosa ha fatto ieri il
tuo agente?» in tre modi, tutti cattivi: guarda i registri tecnici del proprio sistema (che dicono *quale
richiesta è passata*, non *chi l'aveva chiesta e perché*); guarda la cronologia della conversazione
dell'assistente (che è una fonte di parte, cancellabile, e non copre gli agenti che girano senza una chat);
oppure non risponde. Il costo si manifesta in tre momenti precisi:

- **quando qualcosa va storto** — e serve sapere in che ordine sono successe le cose, per capire cosa rimediare;
- **quando qualcuno chiede conto** — un cliente, un revisore, un'assicurazione, un'autorità;
- **quando bisogna decidere di dare più libertà a un agente** — senza uno storico non si sa se se la merita.

C'è un secondo problema, più subdolo, ed è la ragione per cui questa app deve essere fatta bene o non fatta:
**un registro modificabile non è una prova**. Se il gestore del registro (noi) può riscriverlo, il registro vale
quanto la fiducia che si ha in noi — cioè poco, davanti a chi conta. È il motivo per cui l'intera architettura
dell'app è costruita attorno all'inalterabilità dimostrabile (§4.3) e non attorno al cruscotto.

**Cosa NON fa.**

- **non sta in mezzo al traffico.** Non è un intermediario di rete (*gateway*) che intercetta le chiamate degli
  agenti. Tre motivi: (a) un intermediario nel percorso critico del cliente è un punto singolo di guasto —
  «AuditGrove è giù, i miei agenti sono fermi» è un modo sicuro di farsi togliere dal percorso; (b) la piattaforma
  appgrove non ha quella forma: le app stanno dietro il bordo che verifica un token di persona, non davanti al
  traffico di macchine altrui ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §2, §9); (c) l'intercettazione
  dà l'illusione della completezza ma si aggira spostando l'agente su un'altra rotta. AuditGrove sceglie di essere
  **onesto sul proprio limite** e di rilevare i buchi invece di negarli (storia 0011);
- **non esegue e non blocca da solo.** Dice *sì* o *no*; è l'agente che obbedisce. Ne discende che AuditGrove non è
  un presidio di sicurezza contro un agente malevolo: è un presidio di **governo** contro un agente sbagliato,
  distratto o mal istruito — che è il caso reale;
- **non è un sistema di osservazione delle prestazioni.** Non misura la latenza, non conta i gettoni consumati, non
  ottimizza la spesa: quella è l'app 32 TokenGrove. AuditGrove conta *azioni*, non *costi*;
- **non conserva il contenuto dei parametri**, se non quando il cliente lo chiede esplicitamente per uno strumento
  specifico e accettandone le conseguenze (§6.3). Il registro trattiene la **forma** e l'**impronta**, non il
  contenuto: è la scelta che gli impedisce di diventare la più grande raccolta involontaria di dati sensibili del
  sistema;
- **non giudica gli agenti e non assegna punteggi automatici a persone.** Mostra fatti;
- **non fa da archivio a norma di legge** con valore probatorio certificato (conservazione sostitutiva, marca
  temporale qualificata di un prestatore di servizi fiduciari). Produce prove tecnicamente verificabili; il valore
  legale di quelle prove è una domanda per un legale, non per noi (§11, punto 3).

**Rischio di sostituzione da parte dei modelli linguistici.** `rafforzata`. È l'unica app del catalogo il cui
mercato **nasce** dalla diffusione dei modelli linguistici: più agenti girano, più serve. Un assistente generico
non può sostituirla per una ragione strutturale — **non si può chiedere all'agente di essere il custode del
registro delle proprie azioni**. La prova deve stare fuori dal sistema che compie l'azione: è la stessa ragione per
cui non si fa custodire la cassa a chi la usa.

---

## 2. Mercato e analisi in rete

> Compilata dopo **8 ricerche mirate e 4 letture dirette di pagine di prodotto**
> ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4). È un mercato giovane e pieno di annunci: qui sotto i prodotti
> **verificati leggendo la loro pagina**, distinti da ciò che ho letto solo in articoli di terzi. Ciò che non è
> stato trovato è dichiarato al §2.7, non colmato a intuito.

### 2.1 Concorrenti

> Nota sui nomi: quasi tutti questi prodotti contengono la sigla **MCP** nel proprio nome commerciale. Sta per
> *Model Context Protocol*, lo standard aperto con cui un assistente automatico invoca strumenti esterni — lo
> stesso su cui poggia l'epica 12 della piattaforma. Qui la sigla compare solo dentro i nomi propri; nel resto del
> documento si dice «strumento» e «livello conversazionale».

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| MCP Tool Gate | Stati Uniti, in rete | Approva e registra ogni chiamata a strumento: motore di regole per strumento, approvazione umana con avviso su Slack/posta, cronologia esportabile in CSV/JSON, cruscotto d'uso | **Listino pubblico**: gratuito 1.000 chiamate/mese e 7 giorni di registro; Pro **49 $/mese** 50.000 chiamate e 30 giorni; Enterprise su preventivo con 1 anno di registro, autenticazione unificata aziendale; prova di 14 giorni senza carta | [mcptoolgate.com](https://www.mcptoolgate.com/) |
| Natoma | Stati Uniti | Identità e governo degli agenti verso i server di strumenti; oltre 100 connettori pronti | **Parzialmente pubblico**: gratuito 5 server, 5 utenti, 5.000 chiamate/mese; Pro «contattaci» (20.000 chiamate/mese + crediti); Enterprise su misura | [natoma.ai/pricing](https://natoma.ai/pricing) |
| MintMCP | Stati Uniti | Intermediario aziendale per strumenti: distribuzione gestita, autenticazione delegata, cronologia completa | **Nessun prezzo pubblico**: licenza per utente, fasce dichiarate 1-100 / 101-1.000 / 1.001-9.999 / 10.000+ utenti, solo su preventivo | [mintmcp.com/pricing](https://www.mintmcp.com/pricing) |
| Bifrost (Maxim) | Stati Uniti | Nucleo aperto con registrazione delle chiamate e tracciamento; **registri firmati ed esportabili** solo nella versione aziendale | Non pubblico per la parte aziendale; nucleo aperto gratuito | [getmaxim.ai](https://www.getmaxim.ai/bifrost/blog/mcp-audit-logging-requirements-for-enterprise-governance-and-compliance) |
| Lunar MCPX, IBM ContextForge, Kong AI Gateway, TrueFoundry | Stati Uniti / globali | Intermediari di rete per strumenti, rivolti all'impresa grande; instradamento, politiche, registro | Non pubblico (letto in confronti di terzi, non verificato su pagina ufficiale) | [integrate.io](https://www.integrate.io/blog/best-mcp-gateways-and-ai-agent-security-tools/), [composio.dev](https://composio.dev/content/best-mcp-gateway-for-developers) |

**Lettura.** Tre fatti contano più degli altri.

Il primo: **la categoria si sta consolidando verso l'alto**. Natoma, che era il concorrente più vicino a un
listino comprensibile, è stata acquisita da Snowflake nel 2026 (riportato da CIO e Forbes fra i risultati di
ricerca; non ho verificato il comunicato originale). Chi resta indipendente parla per preventivi e per fasce da
1.000 utenti in su. **Il segmento micro e piccolo non è servito da nessuno**: MintMCP parte da fasce che una
azienda di dieci persone non riesce nemmeno a leggere.

Il secondo: **quasi tutti sono intermediari di rete**, cioè chiedono al cliente di far passare il traffico dei
propri agenti attraverso di loro. È la scelta che dà completezza e che, allo stesso tempo, alza la barriera
d'ingresso e crea il timore del punto singolo di guasto. Il posizionamento naturale per AuditGrove è l'opposto:
**dichiarazione invece di intercettazione**, con la sorgente appgrove già collegata (§0, ragione 4).

Il terzo, ed è il più importante: **nessuno di questi prodotti vende l'inalterabilità come promessa centrale**.
Vendono visibilità («vedi cosa fanno i tuoi agenti») e controllo («approva prima che accada»). L'unico riferimento
trovato a registri *firmati* è in Bifrost, e sta nella versione aziendale. Chi promette una **prova verificabile da
un terzo senza doversi fidare di noi** ha uno spazio libero — ed è esattamente ciò che serve nel momento in cui
qualcuno chiede conto (§1).

### 2.2 Prezzi praticati nel dominio

**Rilevato su pagina ufficiale** (le uniche due cifre pubbliche che ho trovato):

- MCP Tool Gate: **0 $** (1.000 chiamate/mese, registro 7 giorni) → **49 $/mese** (50.000 chiamate, registro 30
  giorni, approvazioni via Slack) → su preventivo (illimitato, registro 1 anno). Prova 14 giorni senza carta.
- Natoma: **0 $** (5.000 chiamate/mese, 5 utenti, 5 server) → Pro su richiesta (20.000 chiamate/mese).

**Unità di misura prevalente: la chiamata a strumento al mese** — è il consumo, non l'utente. Su cinque prodotti
esaminati, quattro contano chiamate; solo MintMCP conta utenti. È un dato utile: il mercato ha già educato il
cliente a pensare «quante azioni al mese».

**Osservazione di onestà, importante.** Il listino proposto dalla scheda di catalogo per AuditGrove — gratuito
1.000 chiamate e 7 giorni, Pro 49 €, poi un piano superiore — è **numericamente identico al listino pubblico di
MCP Tool Gate**, che è anche la prima referenza citata dalla scheda. Non è quindi una stima indipendente: è il
prezzo di un concorrente riportato. Ne tengo conto al §5, dove propongo due scostamenti motivati (la durata di
conservazione del piano gratuito e il prezzo del piano alto).

Non ho trovato **alcun prezzo pubblico di un fornitore europeo** per questa categoria.

### 2.3 Obblighi normativi del settore

È la parte che cambia il modello dati, ed è anche la ragione per cui questa categoria ha vento a favore.

- **Regolamento europeo sull'intelligenza artificiale, articolo 12 (conservazione delle registrazioni).** Ho letto
  il testo: «i sistemi di intelligenza artificiale ad alto rischio consentono tecnicamente la registrazione
  automatica degli eventi (log) per tutta la durata del ciclo di vita del sistema». L'obbligo di *rendere possibile*
  la registrazione è del **fornitore** del sistema; il **committente che lo usa** deve sorvegliarne il
  funzionamento e conservare le registrazioni (articolo 26). Fonte primaria:
  [artificialintelligenceact.eu/article/12](https://artificialintelligenceact.eu/article/12/).
- **Durata minima di conservazione**: le fonti secondarie consultate concordano su **almeno sei mesi** salvo
  diversa disposizione (articoli 19 e 26 del medesimo regolamento) e sull'applicazione piena agli usi ad alto
  rischio dal **2 agosto 2026**. **Non ho verificato questi due numeri sul testo primario**: li riporto come
  riportati ([helpnetsecurity.com](https://www.helpnetsecurity.com/2026/04/16/eu-ai-act-logging-requirements/),
  [truescreen.io](https://truescreen.io/insights/ai-act-record-keeping-requirements/)).
- **Conseguenza diretta sul prodotto, e conseguenza forte**: un piano che conserva **7 giorni** non serve a nessun
  obbligo di legge. Se sei mesi è la soglia, un registro da una settimana è un cruscotto, non una prova. È il primo
  motivo per cui al §5 mi scosto dal listino della scheda.
- **Attenzione a non vendere fumo.** La maggior parte degli agenti di una micro-impresa **non** è un sistema ad
  alto rischio ai sensi di quel regolamento, e quindi l'articolo 12 non la riguarda direttamente. AuditGrove non
  deve essere venduto come «ti mette a norma»: va venduto come «se e quando te lo chiedono, hai le carte». La
  differenza è sostanziale, e va scritta nei testi commerciali (§11, punto 6).
- **Protezione dei dati personali**: il registro contiene identificativi di persone (chi ha chiesto, chi ha
  approvato) e, se il cliente attiva la conservazione dei contenuti, potenzialmente dati di terzi. Vale tutto il
  §6, compreso il conflitto fra dovere di prova e diritto alla cancellazione.

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta prevedibile:

1. **appgrove stessa** — il livello conversazionale della piattaforma (epica 12, non implementata) come sorgente
   nativa. Nessun fornitore esterno: sono dati che già stanno da noi. È la storia 0012;
2. **gli assistenti e gli agenti del cliente** attraverso una libreria di dichiarazione o un adattatore che
   avvolge il suo server di strumenti. Non è un fornitore esterno: è il cliente che ci manda i suoi dati;
3. **avviso di approvazione via messaggistica di squadra** (Slack, Teams). ⚠️ **Fornitore esterno che tratterebbe
   dati per nostro conto** — e per giunta riceverebbe il testo della richiesta di approvazione, cioè la parte più
   delicata. Deliberatamente **fuori dal perimetro iniziale** (§7 e storia 0021): l'approvazione vive
   nell'applicazione, con avviso per posta elettronica se la piattaforma lo consente;
4. **esportazione verso i sistemi di sorveglianza della sicurezza del cliente** (formato normalizzato). Non è un
   fornitore: è il cliente che porta via i propri dati. Va però scelto un formato riconoscibile — vedi §2.5;
5. **archiviazione a norma con marca temporale qualificata**. Fornitore esterno, costo per marca, e una promessa
   legale che oggi non sappiamo mantenere: **fuori ambito**, punto aperto §11.

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Dalle guide di settore e dai confronti letti, tre aspettative ricorrono e una assenza è istruttiva.

- **Il minimo indispensabile è un elenco di campi, non una funzione.** La guida più concreta trovata per il
  segmento piccolo elenca i campi che ogni azione deve portare e aggiunge una regola che vale come principio di
  progetto: *se un flusso non riesce a produrre quei campi, quel flusso deve restare in sola proposta*
  ([blog.datavessel.io](https://blog.datavessel.io/ai-agent-audit-trail-smb/)). È esattamente il comportamento
  della storia 0022 (in mancanza di risposta, non si procede).
- **Le azioni di scrittura vogliono un presidio più forte di quelle di lettura**, e i quattro controlli che fanno
  il grosso del lavoro sono: permessi per singola azione, approvazione umana per le operazioni ad alto impatto,
  registro non modificabile, rimozione dei dati sensibili *prima* che arrivino al modello (stessa fonte). Le quattro
  cose stanno rispettivamente nelle storie 0019, 0021, 0002/0008 e 0009/0010.
- **Esiste già una grammatica condivisa per descrivere un'azione di un agente**, e ignorarla sarebbe un errore
  gratuito. Lo standard aperto **AOS (*Agent Observability Standard*, «standard di osservabilità degli agenti»)
  di OWASP** definisce gli eventi `steps/toolCallRequest` (con `toolId`, identificativo di esecuzione, argomenti e
  **motivazione della scelta**) e `steps/toolCallResult` (esito, errore), più una classe di eventi di decisione
  *consenti / nega / modifica* ([aos.owasp.org](https://aos.owasp.org/spec/trace/events/)). Le convenzioni
  OpenTelemetry per l'intelligenza artificiale generativa coprono lo stesso terreno dal lato della misurazione, e
  contengono un dettaglio che conferma la nostra scelta di progetto: **per impostazione predefinita non registrano
  né i testi delle richieste né gli argomenti degli strumenti, perché possono contenere dati sensibili**
  ([opentelemetry.io](https://opentelemetry.io/docs/specs/semconv/registry/attributes/gen-ai/)). Per l'uscita verso
  i sistemi di sicurezza esiste lo schema **OCSF**, che AOS estende con l'attività «uso di strumento da parte di un
  agente» ([owasp.github.io](https://owasp.github.io/www-project-agent-observability-standard/spec/trace/extend_ocsf/)).
  Conseguenza: il contratto dell'azione (storia 0007) **si allinea ad AOS** invece di inventare nomi propri, e
  l'esportazione (storia 0027) offre anche la forma OCSF.
- **L'assenza istruttiva**: in nessuna delle fonti consultate ho trovato clienti che chiedano *analisi del
  comportamento dell'agente*, punteggi di rischio predittivi o rilevamento automatico di intenti malevoli. Chiedono
  di sapere **cosa è successo** e di poter **dire di no prima**. Le funzioni intelligenti sono ciò che i fornitori
  vendono, non ciò che i clienti chiedono: motivo in più per tenerle fuori dal perimetro iniziale.

### 2.6 Fonti consultate

1. **MCP Tool Gate — pagina di prodotto e listino** — https://www.mcptoolgate.com/ — l'unico listino pubblico
   completo della categoria; da qui le cifre del §2.2 e la scoperta che il listino della scheda di catalogo ne è
   una copia.
2. **Natoma — pagina dei prezzi** — https://natoma.ai/pricing — conferma che l'unità di misura è la chiamata a
   strumento e non l'agente; il piano gratuito a 5.000 chiamate fissa l'ordine di grandezza del «per provare».
3. **MintMCP — pagina dei prezzi** — https://www.mintmcp.com/pricing — prova documentale che la fascia alta del
   mercato è a preventivo e per utente, con soglie d'ingresso fuori portata per una micro-impresa.
4. **Regolamento europeo sull'intelligenza artificiale, articolo 12** —
   https://artificialintelligenceact.eu/article/12/ — testo primario dell'obbligo di registrazione automatica e
   ripartizione fra fornitore e committente.
5. **Help Net Security, «What the EU AI Act requires for AI agent logging»** —
   https://www.helpnetsecurity.com/2026/04/16/eu-ai-act-logging-requirements/ — durata minima di conservazione e
   calendario di applicazione; **fonte secondaria**, riportata come tale.
6. **OWASP Agent Observability Standard — eventi** — https://aos.owasp.org/spec/trace/events/ — la grammatica degli
   eventi di un agente, adottata come base del contratto dell'azione (storia 0007).
7. **OpenTelemetry — convenzioni semantiche per l'intelligenza artificiale generativa** —
   https://opentelemetry.io/docs/specs/semconv/registry/attributes/gen-ai/ — conferma che «niente contenuti per
   impostazione predefinita» è la postura standard del settore, non una nostra timidezza.
8. **OWASP AOS — estensione dello schema OCSF** —
   https://owasp.github.io/www-project-agent-observability-standard/spec/trace/extend_ocsf/ — formato di uscita
   verso i sistemi di sorveglianza del cliente.
9. **Maxim/Bifrost, «MCP audit logging requirements»** —
   https://www.getmaxim.ai/bifrost/blog/mcp-audit-logging-requirements-for-enterprise-governance-and-compliance —
   elenco delle funzioni che il mercato considera d'obbligo; i registri firmati sono materia della versione
   aziendale (spazio libero per noi).
10. **Integrate.io, confronto degli intermediari per agenti (2026)** —
    https://www.integrate.io/blog/best-mcp-gateways-and-ai-agent-security-tools/ — panorama e certificazioni;
    **fonte secondaria**, usata solo per la mappa.
11. **Composio, «10 best MCP gateways» (2026)** — https://composio.dev/content/best-mcp-gateway-for-developers —
    come sopra: mappa dei nomi, nessun prezzo verificabile.
12. **DataVessel, guida al registro delle azioni degli agenti per la piccola impresa** —
    https://blog.datavessel.io/ai-agent-audit-trail-smb/ — l'elenco dei campi minimi e la regola «se non produci i
    campi, resta in sola proposta»; **fonte secondaria di parte** (blog di fornitore), usata per le aspettative.
13. **Design Gurus, progettazione di registri a prova di manomissione** —
    https://www.designgurus.io/answers/detail/how-do-you-design-tamperevident-audit-logs-merkle-trees-hashing —
    catena di impronte, albero di Merkle, prova di inclusione, ancoraggio periodico: la tecnica del §4.3.
    **Fonte secondaria di pratica ingegneristica**, non normativa.
14. **Axiom, «il diritto all'oblio contro gli obblighi di registro»** —
    https://axiom.co/blog/the-right-to-be-forgotten-vs-audit-trail-mandates — inquadramento del conflitto del §6.2.
15. **Granit, cancellazione tramite distruzione della chiave** —
    https://granit-fx.dev/blog/crypto-shredding-gdpr-erasure-without-deleting-rows/ — la tecnica proposta al §6.2;
    **fonte secondaria di parte**, e la sua affermazione più importante è proprio quella che non ho potuto
    verificare (§2.7).

### 2.7 Cosa NON sono riuscito a determinare

- **Il prezzo reale dei concorrenti oltre il piano gratuito.** Tre prodotti su cinque mostrano un prezzo solo dopo
  una richiesta di contatto. La proposta del §5 parte quindi da **una sola cifra pubblica** (49 $/mese di MCP Tool
  Gate) e dal ragionamento sul valore. Per chiuderlo servirebbe una richiesta di preventivo a MintMCP e Natoma.
- **Se «almeno sei mesi» sia davvero il minimo di conservazione** del regolamento europeo: l'ho letto in due fonti
  secondarie concordi, non nel testo degli articoli 19 e 26. Va verificato prima di scriverlo in un testo
  commerciale. Chi lo chiude: revisione legale.
- **Se la distruzione della chiave di cifratura valga come cancellazione** ai sensi della normativa sui dati
  personali. Le fonti secondarie citano linee guida del Comitato europeo per la protezione dei dati e delle
  autorità britannica e francese, ma **non ho letto quei documenti**. È il perno del §6.2: finché non è verificato,
  la soluzione resta una proposta.
- **I due incidenti citati dalla scheda di catalogo** (l'agente che cancella una base di dati di produzione durante
  un blocco delle modifiche; il caso analogo presso un'altra azienda). Non ho potuto verificarli su fonte primaria
  — il bilancio di ricerche della sessione si è esaurito prima. Li riporto **come citati dalla scheda**, e vanno
  verificati prima di usarli in un testo commerciale: citare male un incidente altrui è un guaio reputazionale.
- **La disponibilità reale a pagare di un'azienda di dieci persone** per un registro degli agenti. Nessun dato
  pubblico. È il rischio numero uno di questa app (§11).

---

## 3. Varco d'identità — le risposte pronte per `new-application`

> Queste sei righe sono ciò che la skill `new-application` chiede **prima** di generare qualunque cosa.
> L'identificativo dell'app finisce nel nome dello schema del database, nei nomi delle code, nella rotta pubblica
> e nell'istanza del modulo di infrastruttura: cambiarlo dopo **non è una rinomina, è una migrazione di dati**.

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `agentaudit` | Rispetta `^[a-z][a-z0-9_]{0,30}$`: dieci caratteri, minuscolo, solo lettere. Schema `app_agentaudit`, rotte `/api/agentaudit/v1/*`. Scartato `auditgrove`: è il nome commerciale, e i nomi commerciali cambiano. Scartato **`audit` da solo, e per un motivo tecnico non estetico**: la piattaforma ha già un proprio registro di audit delle invocazioni dell'assistente ([UC 0065](../../12-ready-for-ai-mcp/0065-sicurezza-audit-invocazioni-ai.md)) e avrà un proprio registro delle azioni amministrative; un'app che si chiama `audit` renderebbe ambiguo ogni riferimento futuro — «il registro di audit» diventerebbe una domanda invece che un fatto. Scartato `agentlog`: «registro degli eventi» dice *diario*, e questo prodotto vende *prova*. |
| **Modello utente** | `multi` | Non è una preferenza: è **imposto dal prodotto**. L'app esiste per rispondere a «chi ha chiesto» e «chi ha approvato»: un'app a utente singolo non ha nemmeno il concetto di *chi*. Servono inoltre ruoli distinti — chi approva non è necessariamente chi configura le regole, e il **revisore in sola lettura** (storia 0029) è un ruolo che ha senso solo con più persone. Un'app di governo a utente singolo è una contraddizione: sarebbe una persona che approva sé stessa. |
| **Porta locale** | `8131` | Convenzione del kit: 8100 + numero di catalogo (31). Da confermare con `./dev.sh services` al momento dello scaffolding. |
| **Metrica di quota** | `actions` (**azioni registrate**) | È la **sola** cosa che il piano limita, ed è anche l'unità che il mercato ha già insegnato al cliente (§2.2: quattro concorrenti su cinque contano chiamate a strumento). Conta **una** azione registrata ogni riga scritta nella catena, che si tratti di una richiesta di nulla osta, di un esito dichiarato o di un evento di sistema come un sigillo. Il resto — sorgenti collegate, strumenti osservati, regole, approvazioni, esportazioni, verifiche di integrità — è **illimitato in tutti i piani**, deliberatamente: far pagare le regole scoraggerebbe esattamente il comportamento da incoraggiare, e far pagare le verifiche di integrità sarebbe grottesco (si farebbe pagare la prova che la prova è valida). La **durata di conservazione** non è una seconda metrica: è una funzionalità del piano (`features`), come prescritto dal listino come codice ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §7). |
| **Natura della metrica** | `flow` | Consumo su una finestra che si azzera: «50.000 azioni al mese» significa che a ottobre se ne possono registrare altre 50.000 comunque sia andato settembre. Non è una giacenza: un'azione registrata a marzo non «occupa un posto» che vada liberato, e **nessuno deve poter cancellare azioni vecchie per poterne registrare di nuove** — sarebbe la negazione del prodotto. Contarla come giacenza produrrebbe l'incentivo peggiore immaginabile per un registro di prova. |
| **Colore-categoria e icona** | `violet` · icona `shield-check` (scudo con segno di spunta) | Deve coincidere fra listino (`category`) e modulo frontend (`accentToken`). Il ragionamento è vincolato dal dominio: **`red`, `amber` e `green` vanno scartati a priori**, perché in questa app quei tre colori devono restare liberi di significare *negato*, *in attesa di approvazione* e *consentito* **dentro le righe del registro**; un'app il cui accento è rosso non può più usare il rosso per dire «bloccato». Restano `blue`, `violet`, `teal`: `blue` è il colore convenzionale della misura ed è naturale per le app di analisi vicine (20, 32); `teal` è già usato da app operative. Resta `violet`, che è anche il colore che il sistema di design associa alle funzioni trasversali di governo. |

---

## 4. Modello di dominio

### 4.1 Entità principali

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Sorgente` | Un'origine dichiarata di azioni: un agente del cliente, un adattatore davanti al suo server di strumenti, oppure la sorgente nativa appgrove | nome, genere, impronta della chiave d'ingresso, stato, ultima sequenza ricevuta, momento dell'ultimo contatto | no (metadati) |
| `Azione` | **Il cuore dell'app**: una riga del registro, in sola aggiunta. Una chiamata a uno strumento dichiarata da una sorgente | numero di sequenza, momento dichiarato e momento di ricezione, sorgente, identificativo dell'agente, **identificativo di chi ha chiesto**, strumento, forma dei parametri, impronte dei valori, natura (lettura/scrittura), classe di effetto, esito, riferimento al nulla osta, **impronta dell'evento**, **impronta dell'evento precedente** | sì — identificativi di persone (chi ha chiesto, chi ha approvato) |
| `Strumento` | Uno strumento osservato, scoperto dalle azioni o dichiarato: la scheda su cui si appende la regola | nome, sorgente, prima e ultima comparsa, classe di rischio, conteggi | no |
| `Regola` | Che cosa fare quando quello strumento viene chiamato | strumento, decisione (consenti / nega / richiedi approvazione), condizioni, chi l'ha scritta, da quando vale | no |
| `NullaOsta` | La richiesta di autorizzazione prima di un'azione rischiosa, e la sua sorte | stato (in attesa / concesso / negato / scaduto), scadenza, **chi ha deciso**, motivo scritto, riferimento all'azione dichiarata dopo | sì — chi ha deciso |
| `Sigillo` | La fotografia firmata di un tratto di catena, che rende dimostrabile l'inalterabilità | periodo, prima e ultima sequenza, conteggio, impronta di testa, firma, momento e modo di consegna fuori dal perimetro | no |
| `ContenutoAllegato` | Il contenuto vero dei parametri e del risultato, **solo se il cliente l'ha chiesto per quello strumento**: cifrato, con chiave separata | riferimento all'azione, testo cifrato, riferimento alla chiave, scadenza | **sì, e di chiunque** — è la parte pericolosa (§6.3) |
| `ChiaveDiContenuto` | La chiave con cui è cifrato il contenuto allegato, per account e per periodo: distruggerla è cancellare | periodo, stato (viva / distrutta), momento e motivo della distruzione | no |

### 4.2 Relazioni e cicli di vita

Una **sorgente** produce molte **azioni**, numerate in sequenza crescente *per sorgente* — è la numerazione che
rende rilevabili i buchi (storia 0011). Ogni azione appartiene a uno **strumento** (creato alla prima comparsa) e
può avere un **nulla osta** che la precede. Ogni azione porta l'impronta della precedente **nella catena
dell'account**, che è una sola per tutto l'account: una catena per sorgente sarebbe più comoda e molto più debole,
perché permetterebbe di far sparire una sorgente intera senza lasciare traccia.

Il ciclo di vita di un'azione rischiosa, che è il flusso centrale dell'app:

```
   l'agente sta per agire
            │
            ▼
   ① chiede il nulla osta ───► regola dello strumento?
            │                        │
            │                        ├─ consenti ──────────────► ② concesso (subito)
            │                        ├─ nega ──────────────────► ② negato (subito)
            │                        └─ richiedi approvazione ─► ② in attesa
            │                                                        │
            │                                          una persona decide, oppure scade
            │                                                        ▼
            └────────────────────────────► ③ l'agente dichiara l'esito ──► ④ riconciliazione
                                                                            (ha obbedito?)
```

Ogni passaggio numerato **è una riga del registro**: la richiesta, la decisione, l'esito dichiarato, l'eventuale
scostamento rilevato. Nessuno dei quattro si può modificare dopo, nemmeno la decisione: un ripensamento è una
riga nuova, non una correzione.

Il **sigillo** non appartiene a nessuna azione: appartiene a un tratto di catena, e la sua consegna fuori dal
nostro perimetro è ciò che lo rende una prova (§4.3).

### 4.3 Il nodo tecnico dell'app — come si dimostra che il registro non è stato riscritto

È la parte su cui questa applicazione vive o muore, e merita di stare qui e non in una storia.

**Primo strato — la scrittura in sola aggiunta.** La tabella delle azioni non ha percorso di modifica né di
cancellazione nel codice, e il ruolo di database del servizio ha su quella tabella i soli privilegi di inserimento
e lettura ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8: un ruolo per servizio, privilegi solo sul
proprio schema). Nemmeno la cancellazione logica: il `deleted_at` di piattaforma esiste sulla riga ma **non è mai
valorizzato** sulle azioni, e questo va detto esplicitamente nella storia 0002, perché è una deroga consapevole a
una convenzione del repository.

**Secondo strato — la catena delle impronte.** Ogni azione porta l'impronta crittografica del proprio contenuto
canonico **concatenata all'impronta dell'azione precedente dell'account**. Modificare una riga vecchia significa
ricalcolare tutte le successive: la manomissione non è impossibile, è **rilevabile**. La prima riga di ogni account
è un evento di apertura della catena.

**Terzo strato — il sigillo, ed è quello che conta davvero.** Una catena verificabile solo da noi non dimostra
niente: chi ha accesso alla base di dati può riscrivere le righe *e* ricalcolare la catena. Quello che rende la
catena una prova è che **qualcuno fuori dal nostro perimetro tenga una fotografia**. Perciò, a cadenza regolare,
l'app produce un **sigillo**: periodo, numero di azioni, sequenza iniziale e finale, impronta di testa della
catena, firmato. E poi — questo è il punto — **lo consegna**: al cliente per posta elettronica, e nell'esportazione
programmata (storia 0017). Un sigillo che resta solo nella nostra base di dati è un ornamento; un sigillo che il
cliente ha ricevuto due mesi fa nella sua casella è una prova, perché noi non possiamo più raggiungerlo.

**Quarto strato — la verifica da parte di terzi.** Il **pacchetto di prova** (storia 0015) contiene le azioni, i
sigilli, e la descrizione dell'algoritmo, in una forma che chiunque può ricalcolare **senza fidarsi di noi e senza
usare il nostro codice**. Se non è verificabile da un terzo, non è una prova: è una nostra affermazione.

**Il limite, dichiarato.** Restano fuori portata: l'ancoraggio dell'impronta a un registro pubblico di terzi e la
marca temporale qualificata di un prestatore fiduciario. Entrambi renderebbero la prova più forte; entrambi
introducono un fornitore esterno, un costo per operazione e un effetto verso l'esterno. **Punto aperto §11**, non
storia.

### 4.4 Vincoli che discendono dalla piattaforma

Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7, colonne di controllo (`created_at`, `updated_at`,
`created_by`, `updated_by`) e cancellazione logica (`deleted_at`); schema `app_agentaudit`; nessuna chiave esterna
verso altri schemi ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8). Con l'eccezione motivata sopra:
sulle azioni e sui sigilli il `deleted_at` esiste e **non si usa mai**.

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il
> file `services/core/src/main/resources/pricing/agentaudit.yaml`.

**Ragionamento.** Parto da un fatto e da due scostamenti.

Il fatto: **l'unica cifra pubblica del mercato è 49 $/mese per 50.000 azioni e 30 giorni di conservazione** (§2.2).
La scheda di catalogo propone gli stessi numeri, ma — come mostrato al §2.2 — perché li ha copiati da lì.

Primo scostamento: **la conservazione del piano gratuito passa da 7 a 30 giorni.** Sette giorni di registro non
servono a niente di ciò che questa app promette. Chi prova il prodotto per una settimana non ha mai l'occasione di
usarlo per quello che serve (guardare indietro), e il piano gratuito diventa una dimostrazione del cruscotto invece
che del prodotto. Trenta giorni costano poco — sono righe di poche centinaia di byte — e fanno vedere il valore.

Secondo scostamento: **il piano alto scende da 299 € a 149 €.** Duecentonovantanove euro al mese sono un prezzo da
media impresa; il perimetro di appgrove è 1-50 addetti, e il compratore tipo di questa app è la stessa persona che
paga due o tre altre app della suite. A 299 € il piano non verrebbe comprato: verrebbe rimandato.

Cosa giustifica il salto di prezzo fra i piani, dato che la metrica è una sola: **la conservazione**. È l'unico
costo che cresce nel tempo invece che con il consumo, ed è anche l'unica cosa che il cliente compra davvero quando
la posta in gioco sale (§2.3: se la soglia normativa è di sei mesi, un piano da 30 giorni non ci arriva).

| Piano | Prezzo mensile | Prezzo annuale | Limite su `actions` | Conservazione | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|---|
| `free` | — | — | 2.000 azioni/mese | 30 giorni | — | Chi ha un agente solo e vuole capire cosa combina. Abbastanza per vedere il valore, non abbastanza per difendersi |
| `pro` | 39 € | 390 € (= 10× il mensile, «due mesi in regalo») | 50.000 azioni/mese | **13 mesi** | 14 giorni | Il caso normale: da uno a cinque agenti in produzione, una o due persone che approvano. Tredici mesi coprono un anno solare intero più il tempo di accorgersene |
| `team` | 149 € | 1.490 € | 250.000 azioni/mese | **25 mesi** | 14 giorni | Chi ha un obbligo da rispettare o un cliente che chiede conto: ruolo di revisore in sola lettura, consegna programmata del sigillo, esportazione in formato normalizzato |

**Note obbligate.**

- Tre piani, non di più: aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile.
- Un limite lasciato vuoto significa **illimitato**, non zero. Qui nessun limite è vuoto: anche `team` ha un tetto,
  perché una raffica automatica su un registro senza tetto è un problema di costo che si scopre a fine mese.
- **La prova gratuita di 14 giorni su un'app che ha già un piano gratuito non è ridondante, qui**: il piano
  gratuito e i piani a pagamento differiscono soprattutto per *conservazione*, e la conservazione non si prova in
  un piano che ne ha 30 giorni. La prova serve a far vedere il registro lungo, non il numero di azioni.
- **Costo effettivo dell'incasso**: nessun piano sotto i 5 €/mese, quindi la parte fissa per transazione non pesa
  in modo anomalo. Il piano annuale resta la forma da spingere.
- I prezzi sono **immutabili una volta vivi**: un cambio di prezzo si fa creando un prezzo nuovo, non modificando
  quello esistente.
- ⚠️ **Due decisioni di prodotto dentro il listino, che segnalo perché non sono mie.**
  1. **Che cosa succede al raggiungimento del tetto.** La regola di piattaforma è: si blocca con `429`, non si
     addebita a sorpresa. Ma qui bloccare significa **perdere prove**, e un registro con un buco è precisamente
     ciò che il prodotto promette di non essere. La mia proposta (storia 0004) è: avviso all'80 % e al 100 %;
     **banda di cortesia** dichiarata oltre il tetto; e, quando anche quella finisce, rifiuto con `429` **più una
     riga nel registro che conta le azioni rifiutate** — così il buco è a sua volta dimostrabile e misurato.
     Ampiezza della banda e comportamento sono decisione dello sviluppatore.
  2. **Che cosa succede alla conservazione quando si scende di piano.** Passare da `team` a `pro` accorcerebbe la
     conservazione, cioè **distruggerebbe prove già acquisite**. Proposta: il passaggio a un piano inferiore
     avvisa, propone l'esportazione, e la riduzione decorre solo dal momento del cambio in avanti (le azioni già
     registrate mantengono la conservazione del piano sotto cui sono nate). Costa complessità; l'alternativa è
     spiacevole. Decide lo sviluppatore.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/agentaudit.yaml`) si compila **insieme** allo sviluppatore: «niente contratto, niente
> produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

**Categorie particolari (articolo 9): no per costruzione, ma il canale è aperto.** L'app non chiede, non prevede e
non desidera dati su salute, biometria, genetica, opinioni politiche, convinzioni religiose, orientamento sessuale
o appartenenza sindacale. Non ci sono campi che li ospitino. **Ma l'ingresso è un canale su cui il cliente manda
ciò che vuole**: se un suo agente chiama uno strumento con un parametro che contiene una diagnosi, e se il cliente
ha attivato la conservazione dei contenuti per quello strumento, un dato dell'articolo 9 entra. La risposta di
progetto sta al §6.3 ed è la ragione per cui i contenuti **non si conservano per impostazione predefinita**. Va
comunque scritto nero su bianco nel manifesto e nell'informativa: **non trattiamo dati particolari; se ne entrano
attraverso i contenuti allegati, è il cliente titolare ad averli immessi e ad esserne responsabile** — ed è una
frase che va fatta leggere a un legale, non copiata da qui.

### 6.1 Chi è titolare e chi è responsabile — la domanda che viene prima delle altre

Va risolta prima del manifesto, perché cambia tutto il resto.

- Per gli eventi che riguardano **i sistemi del cliente** (perimetro B, §0), le finalità e i mezzi li decide il
  cliente: **appgrove è responsabile del trattamento, il cliente è titolare**. Le richieste degli interessati
  arrivano a lui, e lui le esercita attraverso l'app. Serve un accordo sul trattamento fra noi e lui — che è un
  documento che oggi esiste per la piattaforma ma va verificato per questo caso d'uso.
- Per gli eventi che riguardano **la piattaforma appgrove stessa** (sorgente nativa), il titolare siamo noi:
  è il registro di UC 0065.
- **Le due nature convivono nella stessa tabella.** È il genere di cosa che sembra un dettaglio e poi genera una
  contestazione. La riga di azione deve portare l'indicazione della natura della sorgente, e le due nature devono
  poter avere durate e regole diverse.

⚠️ **Escalation.** La qualificazione dei ruoli è materia di revisione legale, non di un agente che scrive
documenti: va aggiunta al registro dei punti da far rivedere prima del rilascio.

### 6.2 Il conflitto vero — la prova deve restare, la persona ha diritto alla cancellazione

Il problema, senza addolcirlo: il valore di questa app è che **il registro non si tocca**; il diritto alla
cancellazione dei propri dati personali dice che, a certe condizioni, un dato **si toglie**. Le due cose non si
conciliano con un accorgimento tecnico: si conciliano con una decisione di quale prevale, quando, e per quanto
tempo — e quella decisione richiede un legale.

Quello che propongo, e che è una proposta:

1. **Ridurre la superficie prima di tutto.** Meno dati personali ci sono nel registro, meno il conflitto morde.
   Nella catena stanno **identificativi**, non nomi: `user_id`, identificativo dell'agente, identificativo del
   richiedente fornito dal cliente. Chi ha approvato è un identificativo di utente della piattaforma. Nessun nome,
   nessuna posta elettronica, nessun contenuto (§6.3).
2. **Due strati con regole diverse.** La **catena di prova** (impronte, identificativi, esiti, momenti) si
   conserva per la durata dichiarata del piano e **non è cancellabile in quel periodo**: è ciò per cui il cliente
   paga, ed è ciò che gli serve per adempiere ai propri obblighi. Il **contenuto allegato** (§6.3), quando esiste,
   sta in un deposito separato, cifrato con una chiave per account e periodo.
3. **La cancellazione si esercita sul secondo strato, distruggendo la chiave.** Distrutta la chiave, il contenuto è
   rumore per sempre; la catena resta intera e verificabile, perché le impronte non dipendono dalla leggibilità del
   contenuto. ⚠️ **Il presupposto giuridico di questa tecnica — che la distruzione della chiave valga come
   cancellazione — è riportato da fonti secondarie che non ho potuto verificare** (§2.7). Se non regge, la
   soluzione non regge.
4. **La cancellazione è essa stessa un evento del registro.** «Il contenuto delle azioni dal … al … è stato reso
   illeggibile su richiesta, il …»: si dimostra di aver cancellato, senza conservare ciò che si è cancellato.
5. **Ciò che non propongo, e perché.** Non propongo di sostituire gli identificativi con altri codici: sostituire
   nomi con codici **non è cancellare** ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §10), e per giunta
   romperebbe la catena. Non propongo la cancellazione fisica delle righe della catena durante il periodo di
   conservazione: farebbe crollare l'unica cosa che questa app vende.

⚠️ **Escalation, la più importante del documento.** Questo impianto **va rivisto da un legale prima di qualunque
implementazione**: quale base giuridica regge la conservazione della catena (l'obbligo di legge del cliente? il
nostro legittimo interesse? l'esecuzione del contratto?), quali eccezioni al diritto di cancellazione si possono
invocare e per quanto, e se la distruzione della chiave sia sufficiente. Va aggiunto al registro dei punti da far
rivedere prima del rilascio. Fino ad allora, **nessuna riga di codice** su questa parte.

### 6.3 I parametri possono contenere qualunque cosa — come si evita che il registro diventi il problema

Il rischio, detto per intero: i parametri di una chiamata a uno strumento sono testo libero deciso da un agente.
Dentro può esserci il codice fiscale di un cliente, il testo di un messaggio privato, una diagnosi, una chiave di
accesso, la password di un servizio. Un registro che conserva tutto diventa, nel giro di poche settimane, **la
raccolta di dati sensibili più grande e meno presidiata dell'intera azienda del cliente** — e per giunta una
raccolta che, per costruzione, non si può ripulire.

Cinque misure, in ordine di efficacia:

1. **Non conservare i contenuti. È l'impostazione predefinita.** Di ogni parametro il registro trattiene la
   **forma** — nome, tipo, lunghezza, se era vuoto — e l'**impronta** del valore, non il valore. È la stessa
   postura delle convenzioni OpenTelemetry, che per impostazione predefinita non registrano gli argomenti degli
   strumenti proprio perché possono contenere dati sensibili (§2.5). Storia 0009.
2. **L'impronta conserva la capacità di prova senza conservare il dato.** Se domani qualcuno afferma «l'agente ha
   cancellato il cliente numero 4172», si prende quel valore, si ricalcola l'impronta e si confronta: se combacia,
   è dimostrato; se non combacia, è smentito. **Si può verificare un'affermazione senza aver conservato il dato.**
   Il limite, dichiarato: l'impronta di un valore a bassa entropia (un indirizzo di posta, un numero di telefono)
   è indovinabile da chi conosce il sale usato — perciò il sale è per account e sta separato dalle righe. Non è
   perfetto; va valutato da chi presidia la sicurezza (§11).
3. **Redazione all'ingresso, fatta da noi e non dal cliente.** Prima di scrivere, il servizio passa su ciò che
   riceve e sostituisce ciò che ha la forma di un segreto — parametri che si chiamano `password`, `token`,
   `secret`, `api_key`, `authorization`; valori con la forma nota di una chiave di accesso, di una carta di
   pagamento, di un codice fiscale — con un marcatore che dice *che cosa* è stato rimosso. **Non ci si può fidare
   che lo faccia il chiamante**: se il cliente avesse la disciplina di ripulire i propri parametri non avrebbe
   bisogno di noi. Storia 0010.
4. **La redazione diventa una funzione, non solo una difesa.** Quando la redazione trova un segreto, l'app lo dice
   al cliente: «un tuo agente ha passato in chiaro quella che sembra una chiave di accesso allo strumento X». È
   una delle cose più utili che il prodotto può fare, e nasce come effetto collaterale del presidio.
5. **La conservazione del contenuto è possibile, ma per singolo strumento, esplicita, con avviso e con durata più
   corta.** Ci sono casi legittimi (uno strumento che manda comunicazioni ai clienti: senza il testo la prova non
   serve). Si attiva per quello strumento, si vede scritto che cosa comporta, e il contenuto va nel deposito
   cifrato del §6.2. Storia 0031.

### 6.4 Categorie trattate

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `azione.richiedente` | tabella delle azioni, colonna dell'identificativo di chi ha chiesto | persona che lavora presso il cliente (o suo cliente finale, se il cliente lo dichiara così) | identificativo, non nome | attribuire l'azione a chi l'ha chiesta: è il senso del prodotto | esecuzione del contratto col cliente, che agisce per proprio obbligo di rendicontazione | durata di conservazione del piano |
| `azione.agente` | tabella delle azioni | identifica un programma, ma può coincidere con una persona (un assistente personale) | identificativo | distinguere chi ha agito | come sopra | come sopra |
| `nullaosta.decisore` | tabella dei nulla osta | persona che lavora presso il cliente | identificativo dell'utente della piattaforma + momento + motivo scritto | dimostrare che una persona ha approvato | come sopra | come sopra |
| `nullaosta.motivo` | tabella dei nulla osta | scritto da una persona | **testo libero** | spiegare perché si è approvato o negato | come sopra | come sopra |
| `azione.impronte_parametri` | tabella delle azioni | chiunque compaia nei parametri | impronta, non valore | verificare affermazioni senza conservare i dati | minimizzazione: è la misura che *riduce* il trattamento | come sopra |
| `contenuto_allegato.testo` | deposito separato, cifrato | **chiunque** — imprevedibile | contenuto integrale dei parametri e dei risultati | prova del contenuto, quando serve davvero | scelta esplicita del cliente titolare per singolo strumento | durata più corta della catena; cancellabile distruggendo la chiave |
| `sorgente.contatto_di_avviso` | tabella delle sorgenti | persona presso il cliente | indirizzo di posta elettronica | recapitare avvisi e sigilli | esecuzione del contratto | finché la sorgente esiste |

**Esportazione e cancellazione.** Tutte le tabelle che contengono dati di persone devono comparire **sia** in
`exportData` **sia** in `purgeData` del contratto dati dell'app (`AgentauditDataContract`): azioni, nulla osta,
sorgenti, contenuti allegati, chiavi di contenuto. Dimenticarne una è il difetto di conformità più probabile.
⚠️ **Con l'avvertenza che questa app rende speciale**: per le azioni, la cancellazione fisica durante il periodo di
conservazione è in conflitto con la ragion d'essere del prodotto (§6.2). Il contratto dati **deve dichiarare
esplicitamente** cosa fa in quel caso, e la risposta è materia della revisione legale — non del codice.

**Testo libero.** Due ingressi non presidiati: il **motivo scritto** di un'approvazione (breve, scritto da un
utente consapevole: rischio basso) e il **contenuto allegato** (rischio alto, §6.3). L'app non fa rilevazione
automatica di contenuto oltre la redazione dei segreti.

**Integrazioni esterne.** Nel perimetro iniziale **nessun fornitore esterno riceve dati**: la messaggistica di
squadra per le approvazioni (§2.4, punto 3) è deliberatamente esclusa proprio per questo, e l'archiviazione a
norma con marca temporale (punto 5) resta un punto aperto. Se una delle due entrasse, si aprirebbe un nuovo
responsabile esterno del trattamento e andrebbe dichiarata.

**Classificazione della change.** Un'app nuova che introduce una finalità nuova (tracciabilità delle azioni
automatiche), un ruolo nuovo (responsabile del trattamento per conto del cliente) e una categoria di dati
imprevedibile (i contenuti allegati): è un cambiamento **sostanziale**, senza discussione.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `elenca_azioni` | `(periodo?, sorgente?, strumento?, esito?, richiedente?) → elenco minimizzato di azioni` | Cronologia filtrata, senza contenuti | lettura | no |
| `dettaglio_azione` | `(id) → scheda dell'azione` | Forma dei parametri, impronte, nulla osta collegato, posizione nella catena | lettura | no |
| `elenca_approvazioni_in_attesa` | `(sorgente?) → elenco di nulla osta pendenti` | Cosa sta aspettando una persona | lettura | no |
| `verifica_integrita` | `(periodo?) → esito della verifica` | Ricalcola la catena e la confronta coi sigilli; risponde «integra» o indica la prima riga divergente | lettura | no |
| `riepiloga_attivita` | `(periodo) → conteggi per strumento, esito, sorgente` | Il riassunto che serve per la domanda «cosa è successo ieri?» | lettura | no |
| `proponi_regola` | `(strumento, decisione, motivo) → bozza di regola` | Prepara la regola; **non la applica** | scrittura | **sì** |
| `prepara_esportazione` | `(periodo, formato) → bozza di esportazione` | Prepara un'estrazione che può contenere identificativi di persone | scrittura | **sì** |
| `nega_azione` | `(id nulla osta, motivo) → bozza di rifiuto` | Ferma un'azione in attesa | scrittura | **sì, obbligatoria** |
| ~~`approva_azione`~~ | — | **Deliberatamente non esposto** | — | — |

**La riga che conta è l'ultima, ed è una regola di prodotto, non una dimenticanza.** Un assistente non può
approvare. Il senso dell'approvazione umana è che **una persona** si assuma la responsabilità di un'azione
irreversibile: se l'approvazione si può ottenere chiedendola a un assistente, la catena di responsabilità è finita
e il prodotto ha smesso di funzionare. Nel caso peggiore — un assistente dirottato da istruzioni malevole
([UC 0065](../../12-ready-for-ai-mcp/0065-sicurezza-audit-invocazioni-ai.md)) — esporre l'approvazione
significherebbe consegnare la chiave a chi si voleva sorvegliare.

Il **rifiuto**, invece, è esposto: negare va nella direzione sicura, è sempre rimediabile da una persona, e ci
sono casi reali in cui è utile («ferma tutto quello che sta aspettando sullo strumento X»). Resta comunque con
conferma umana.

**Perché il livello conversazionale rende questa app più utile delle concorrenti.** Le domande che si fanno a un
registro sono domande in lingua naturale — «cosa ha fatto l'agente di fatturazione ieri notte?», «chi ha approvato
quella cancellazione?», «ci sono azioni che nessuno ha approvato?». Sono esattamente le domande che un elenco con
i filtri risponde male e una conversazione risponde bene. È anche l'unico caso del catalogo in cui il livello
conversazionale interroga il registro **delle proprie stesse azioni**: ogni domanda posta all'assistente su
AuditGrove è a sua volta un'azione registrata (storia 0036).

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine dell'epica l'app esiste, è accesa, vuota e utilizzabile: servizio, schema, **catena delle impronte**,
modulo frontend, quota, avvio locale.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | Istanza di scaffolding, rotte `/api/agentaudit/v1/*`, infrastruttura dal modulo comune |
| [0002](01-fondamenta/0002-registro-in-sola-aggiunta-e-catena-delle-impronte.md) | Registro in sola aggiunta e catena delle impronte | Il cuore: tabella che non si modifica, catena per account, privilegi di sola scrittura |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Manifesto, registrazione, sezioni, cinque lingue, tema chiaro e scuro |
| [0004](01-fondamenta/0004-abbonamento-e-quota-sulle-azioni.md) | Abbonamento e quota sulle azioni | Metrica `actions`, avvisi, banda di cortesia, rifiuto contato |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | Scoperta automatica, dati inventati, un agente finto che dichiara azioni |

### Epica 02 — Sorgenti e ingresso delle azioni

Come le azioni entrano: chi può scrivere nel registro, che forma ha un'azione, e le tre misure che impediscono al
registro di ingoiare ciò che non deve.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-sorgenti-e-ingresso-delle-azioni/0006-registrazione-di-una-sorgente.md) | Registrazione di una sorgente | Una persona dichiara un agente e ottiene una chiave d'ingresso |
| [0007](02-sorgenti-e-ingresso-delle-azioni/0007-contratto-dell-azione-registrata.md) | Contratto dell'azione registrata | Le sei domande in forma di schema, allineato allo standard OWASP |
| [0008](02-sorgenti-e-ingresso-delle-azioni/0008-rotta-di-ingresso-e-scrittura-nella-catena.md) | Rotta di ingresso e scrittura nella catena | L'azione arriva, viene validata e diventa una riga incatenata |
| [0009](02-sorgenti-e-ingresso-delle-azioni/0009-minimizzazione-dei-parametri.md) | Minimizzazione dei parametri | Si conservano forma e impronta, non i valori |
| [0010](02-sorgenti-e-ingresso-delle-azioni/0010-redazione-dei-segreti-e-campanello.md) | Redazione dei segreti e campanello | Ciò che sembra un segreto non entra, e il cliente viene avvisato |
| [0011](02-sorgenti-e-ingresso-delle-azioni/0011-numerazione-di-sequenza-e-buchi.md) | Numerazione di sequenza e buchi | Il registro dichiara ciò che non ha ricevuto |
| [0012](02-sorgenti-e-ingresso-delle-azioni/0012-sorgente-nativa-appgrove.md) | Sorgente nativa appgrove | Il livello conversazionale della piattaforma si collega con un interruttore |

### Epica 03 — Prova di inalterabilità

Ciò che trasforma un elenco di righe in una prova: sigilli, verifica, pacchetto verificabile da terzi,
conservazione, e la consegna del sigillo fuori dal nostro perimetro.

| # | Storia | In una riga |
|---|---|---|
| [0013](03-prova-di-inalterabilita/0013-sigillo-periodico-della-catena.md) | Sigillo periodico della catena | Fotografia firmata di un tratto di catena, a cadenza regolare |
| [0014](03-prova-di-inalterabilita/0014-verifica-dell-integrita.md) | Verifica dell'integrità | Ricalcolo su richiesta, con indicazione della prima riga divergente |
| [0015](03-prova-di-inalterabilita/0015-pacchetto-di-prova-verificabile-da-terzi.md) | Pacchetto di prova verificabile da terzi | Si verifica senza fidarsi di noi e senza il nostro codice |
| [0016](03-prova-di-inalterabilita/0016-conservazione-e-chiusura-del-periodo.md) | Conservazione e chiusura del periodo | Quanto si tiene, come si chiude, cosa succede scendendo di piano |
| [0017](03-prova-di-inalterabilita/0017-consegna-del-sigillo-fuori-perimetro.md) | Consegna del sigillo fuori perimetro | Il sigillo arriva al cliente: è ciò che lo rende una prova |

### Epica 04 — Regole e approvazione umana

Il flusso centrale: classificare gli strumenti, decidere le regole, chiedere il nulla osta prima di agire, far
decidere una persona, e verificare se l'agente ha obbedito.

| # | Storia | In una riga |
|---|---|---|
| [0018](04-regole-e-approvazione-umana/0018-catalogo-degli-strumenti-osservati.md) | Catalogo degli strumenti osservati | Gli strumenti si scoprono da soli e si classificano per rischio |
| [0019](04-regole-e-approvazione-umana/0019-regola-per-strumento.md) | Regola per strumento | Consenti, nega, oppure fai approvare a una persona |
| [0020](04-regole-e-approvazione-umana/0020-richiesta-di-nulla-osta.md) | Richiesta di nulla osta | L'agente chiede prima di agire e riceve una risposta |
| [0021](04-regole-e-approvazione-umana/0021-decisione-di-una-persona.md) | Decisione di una persona | La coda delle approvazioni, la decisione, il motivo scritto |
| [0022](04-regole-e-approvazione-umana/0022-scadenza-e-mancata-risposta.md) | Scadenza e mancata risposta | Se nessuno risponde non si procede, e lo si dice |
| [0023](04-regole-e-approvazione-umana/0023-riconciliazione-fra-nulla-osta-ed-esito.md) | Riconciliazione fra nulla osta ed esito | L'agente ha fatto quello che gli era stato concesso? |

### Epica 05 — Lettura, ricerca e rendicontazione

Quello che si fa col registro una volta che è pieno: guardarlo, cercarci dentro, essere avvisati, portarselo via,
e farlo vedere a chi chiede conto.

| # | Storia | In una riga |
|---|---|---|
| [0024](05-lettura-ricerca-e-rendicontazione/0024-cronologia-delle-azioni.md) | Cronologia delle azioni | L'elenco con i filtri che servono davvero |
| [0025](05-lettura-ricerca-e-rendicontazione/0025-scheda-di-un-azione.md) | Scheda di un'azione | Tutto ciò che si sa di una riga, compresa la sua posizione nella catena |
| [0026](05-lettura-ricerca-e-rendicontazione/0026-avvisi-su-comportamenti-anomali.md) | Avvisi su comportamenti anomali | Volume fuori scala, strumento mai visto, azione senza nulla osta |
| [0027](05-lettura-ricerca-e-rendicontazione/0027-esportazione-del-registro.md) | Esportazione del registro | CSV, JSON e forma normalizzata per i sistemi di sicurezza |
| [0028](05-lettura-ricerca-e-rendicontazione/0028-rapporto-periodico.md) | Rapporto periodico | Il documento da mostrare a chi chiede conto |
| [0029](05-lettura-ricerca-e-rendicontazione/0029-ruolo-di-revisore-in-sola-lettura.md) | Ruolo di revisore in sola lettura | Chi guarda il registro non deve poter toccare le regole |

### Epica 06 — Dati delle persone e diritti

Il conflitto fra prova e cancellazione, affrontato per intero: due strati, chiavi separate, cancellazione
dimostrabile, contratto dati dell'app.

| # | Storia | In una riga |
|---|---|---|
| [0030](06-dati-delle-persone-e-diritti/0030-due-strati-catena-e-contenuto-cifrato.md) | Due strati: catena e contenuto cifrato | Il contenuto sta fuori dalla catena e sotto chiave |
| [0031](06-dati-delle-persone-e-diritti/0031-conservazione-del-contenuto-per-strumento.md) | Conservazione del contenuto per strumento | Si attiva a mano, per uno strumento alla volta, sapendo cosa comporta |
| [0032](06-dati-delle-persone-e-diritti/0032-cancellazione-tramite-distruzione-della-chiave.md) | Cancellazione tramite distruzione della chiave | Si cancella il contenuto senza rompere la prova, e lo si dimostra |
| [0033](06-dati-delle-persone-e-diritti/0033-esportazione-e-contratto-dati-dell-app.md) | Esportazione e contratto dati dell'app | Manifesto, `exportData`, `purgeData`, e ciò che il contratto deve dichiarare |

### Epica 07 — Esposizione conversazionale e prove end-to-end

Il contratto degli strumenti, il divieto di auto-approvazione, i varchi sulle chiamate dell'assistente e il
percorso end-to-end.

| # | Storia | In una riga |
|---|---|---|
| [0034](07-esposizione-conversazionale-e-prove/0034-strumenti-di-lettura.md) | Strumenti di lettura | Cinque strumenti di sola lettura, con risultati minimizzati |
| [0035](07-esposizione-conversazionale-e-prove/0035-strumenti-di-scrittura-e-divieto-di-auto-approvazione.md) | Strumenti di scrittura e divieto di auto-approvazione | Bozza e conferma; l'approvazione non si espone, mai |
| [0036](07-esposizione-conversazionale-e-prove/0036-ruolo-e-quota-sulle-chiamate-dell-assistente.md) | Ruolo e quota sulle chiamate dell'assistente | Anche l'assistente consuma quota, e le sue chiamate finiscono nel registro |
| [0037](07-esposizione-conversazionale-e-prove/0037-percorso-end-to-end-e-registro-di-copertura.md) | Percorso end-to-end e registro di copertura | Il percorso `[J-AGENTAUDIT]` e le voci del registro di copertura |

**Totale**: 7 epiche, 37 storie.

---

## 9. Estensioni della console di amministrazione

Servono poche cose, ma non zero, e per una ragione precisa: questa è l'app in cui **chi amministra la piattaforma
non deve poter toccare niente**. Servono quindi tre viste di diagnosi su metadati (stato delle sorgenti, arretrato
delle approvazioni, esito dell'ultimo sigillo) e — soprattutto — la dichiarazione esplicita che **nessuna azione
amministrativa può scrivere, correggere o cancellare righe del registro di un cliente**, perché quella capacità,
se esistesse, distruggerebbe il valore del prodotto per tutti i clienti insieme.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| **Piattaforma appgrove** — epica 12 (UC 0061-0066) | **dipende da**, per la sorgente nativa | Il livello conversazionale è la prima sorgente collegata: senza di esso resta la sola dichiarazione da parte degli agenti del cliente (che è comunque sufficiente al prodotto) |
| 32 — TokenGrove | **confina con** | Entrambe osservano le chiamate degli agenti, ma per domande diverse: TokenGrove chiede *quanto costa*, AuditGrove *chi l'ha autorizzato*. Potrebbero condividere il contratto dell'evento in ingresso: sarebbe una sinergia vera, e va valutata prima che entrambe scrivano il proprio |
| Ogni app appgrove con strumenti conversazionali | **alimenta** (in direzione entrante) | Ogni app che espone strumenti diventa una sorgente potenziale: è il vantaggio strutturale del §0 |
| 13 — FlowGrove (automazioni) | **si sovrappone parzialmente** | Un'automazione è un agente senza modello linguistico: le sue esecuzioni sono azioni registrabili. Se FlowGrove tiene un proprio storico di esecuzione, c'è un rischio di doppia costruzione — da chiarire fra le due |

**Lettura.** AuditGrove ha senso **da sola**: il suo mercato non sono i clienti appgrove, sono le aziende che
hanno agenti in produzione (§0). Ma dentro la suite acquista una proprietà che nessun concorrente ha — si accende
già pieno di dati. Nessuna delle entità condivise del catalogo (anagrafica clienti, catalogo prodotti, anagrafica
dipendenti, catena del documento contabile) la riguarda: AuditGrove non tocca il dominio delle altre app, ne
osserva le *azioni*.

**Sovrapposizioni da evitare.** Con **TokenGrove** sul contratto dell'evento in ingresso (vedi sopra); con la
piattaforma stessa sull'audit delle invocazioni dell'assistente ([UC 0065](../../12-ready-for-ai-mcp/0065-sicurezza-audit-invocazioni-ai.md)),
che **resta di piattaforma e non va rifatto qui**: AuditGrove lo *riceve* come sorgente, non lo sostituisce.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **Il perimetro: piattaforma o sistemi del cliente** (§0) | È direzione di prodotto e cambia il modello dati. Ho preso posizione (sistemi del cliente), non ho deciso | sviluppatore, **prima di ogni altra cosa** |
| 2 | **Registro cooperativo invece di intercettazione** (§1) | Discende dal punto 1 ed è la scelta che definisce cosa il prodotto può promettere | sviluppatore |
| 3 | **Valore legale della prova** | Produciamo prove tecnicamente verificabili; se valgano davanti a un giudice, e in quali giurisdizioni, è una domanda per un legale | revisione legale |
| 4 | **Prova contro cancellazione** (§6.2) | Base giuridica della conservazione, eccezioni invocabili, e se la distruzione della chiave valga come cancellazione | revisione legale, **bloccante prima dell'implementazione dell'epica 06** |
| 5 | **Titolare o responsabile del trattamento** (§6.1) | Le due nature convivono nella stessa tabella; cambia l'accordo col cliente | revisione legale |
| 6 | **Come si vende senza promettere conformità** (§2.3) | «Ti mette a norma» sarebbe falso per la maggior parte dei clienti | sviluppatore + revisione legale sui testi |
| 7 | **Come una macchina si autentica** | La piattaforma ricava il `tenant_id` da un token di persona verificato; una sorgente è una macchina. Propongo una chiave d'ingresso per sorgente, verificata dal servizio, da cui si deriva il `tenant_id` — che **non** arriva mai dal corpo della richiesta. Ma introdurre una credenziale non umana eccede una singola storia e tocca il bordo della piattaforma (UC 0014, UC 0016) | sviluppatore + piattaforma; storia 0006 lo assume, non lo decide |
| 8 | **Prezzi e comportamento al tetto** (§5) | Fermata di escalation; in più il blocco per quota su un registro di prova ha un costo che nessun'altra app ha | sviluppatore |
| 9 | **Ancoraggio esterno del sigillo e marca temporale qualificata** (§4.3) | Renderebbero la prova molto più forte, ma introducono un fornitore esterno, un costo per operazione e un effetto verso l'esterno | sviluppatore |
| 10 | **Robustezza delle impronte su valori a bassa entropia** (§6.3) | L'impronta di un indirizzo di posta è indovinabile da chi conosce il sale | chi presidia la sicurezza |
| 11 | **Sovrapposizione con TokenGrove** (§10) | Due app che osservano lo stesso flusso con due contratti diversi sarebbero uno spreco | sviluppatore |
| 12 | **Avviso di approvazione fuori dall'app** (§2.4) | I concorrenti avvisano su Slack, e per un'approvazione urgente conta. Introduce un fornitore esterno che riceverebbe la parte più delicata | sviluppatore |
| 13 | **Le righe della sorgente nativa appgrove devono consumare quota?** | Al §0 la sorgente nativa è inclusa perché «è già dovuta»: farne pagare le righe sarebbe contraddittorio. Ma non contarle apre un consumo non limitato che nessun piano copre. Emerso scrivendo la storia 0012 | sviluppatore (tocca il listino) |
| 14 | **Il codice fiscale è redatto come se fosse un segreto** (storia 0010) | Non è un segreto: è un dato personale. Il marcatore giusto è di un altro genere, e la distinzione cambia il messaggio del «campanello» e la voce del manifesto | sviluppatore + classificazione dati personali |
| 15 | ⚠️ **La chiave di contenuto è per account e periodo, ma la cancellazione si chiede per una persona** | È il difetto potenzialmente fatale dell'impianto del §6.2: distruggere la chiave di un periodo rende illeggibile il contenuto **di tutti**, non di chi ha chiesto la cancellazione. Le vie possibili — una chiave per interessato (che presuppone di sapere chi compare in un parametro che non conserviamo in chiaro), oppure accettare che la cancellazione operi per periodo — hanno conseguenze opposte. Emerso scrivendo le storie 0030-0033 | revisione legale + sviluppatore, **prima dell'epica 06** |
| 16 | **Chi può rivelare un contenuto conservato** | La decisione è condivisa fra tre storie (0025, 0029, 0031) e va presa **una volta sola**, altrimenti si contraddicono in implementazione | sviluppatore |
| 17 | **Chi ha il privilegio di rimuovere gli intervalli scaduti** | La storia 0002 toglie al ruolo del servizio ogni privilegio di cancellazione sul registro; la 0016 ha bisogno di rimuovere gli intervalli scaduti. Propongo un ruolo separato usato **solo** dalla lavorazione di conservazione, così che la superficie capace di cancellare resti minuscola | sviluppatore, prima della storia 0016 |

**Rischi noti**

- **Il rischio numero uno: si registra ma non si approva.** La scheda di catalogo lo dice già («abbandona se i team
  loggano ma non usano le approvazioni»), e l'analisi lo conferma: la sola registrazione ha poca disponibilità a
  pagare. Attenuazione: rendere l'approvazione la prima cosa che si incontra, non la settima — e misurarla come
  segnale di adozione fin dal primo giorno.
- **Un mercato che si consolida verso l'alto** (§2.1): se i concorrenti vengono assorbiti da piattaforme grandi, la
  categoria potrebbe diventare una funzione inclusa altrove invece che un prodotto. Attenuazione: il segmento micro
  e piccolo resta scoperto, ed è il nostro.
- **La promessa di inalterabilità è fragile alla prima smentita.** Un solo caso in cui un cliente dimostra che il
  registro è stato alterato distrugge il prodotto. Attenuazione: sigilli consegnati fuori perimetro (storia 0017),
  verifica di terzi (storia 0015), e nessuna capacità amministrativa di scrittura (§9).
- **Il prodotto dipende da una epica di piattaforma non implementata** per il proprio vantaggio distintivo. Se
  l'epica 12 non arriva, AuditGrove resta un buon prodotto senza il suo fossato. Attenuazione: il perimetro B non
  ne dipende per funzionare (§0).
- **Complessità sopra la media del catalogo.** La scheda la dà «media»: alla luce del §4.3 e del §6.2 è
  **medio-alta**. Non c'è modo di renderla bassa senza togliere ciò che la rende un prodotto.

**Fuori dimensionamento**: nessuno. Sette epiche (fascia 4-7), da 4 a 7 storie ciascuna (fascia 4-8), 37 storie in
tutto (fascia 20-45). L'epica 06 ha 4 storie sole perché la parte difficile è la decisione legale, non il codice.
