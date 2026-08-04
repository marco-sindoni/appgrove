# Log di implementazione — change 0086

**Modalità:** `go-fast` (autopilot senza gate di workflow).
**Natura:** documentale. Nessuna riga di codice eseguibile, nessuna infrastruttura, nessun dato personale reale.

## Che cosa è stato prodotto

Il drill-down di **17 applicazioni** del catalogo, sotto `docs/usecases/_catalogo/`: **113 epiche**,
**573 storie utente**, 17 artefatti navigabili, 17 documenti di estensione della console di amministrazione,
più il kit d'autore condiviso e l'indice del catalogo.

## Come è stato fatto — e perché così

**Il kit prima delle applicazioni.** Diciassette cartelle scritte da agenti a contesto separato, che non si
vedono fra loro, divergerebbero nel formato entro la terza. Il primo lavoro è stato quindi costruire il
**metro unico** (`_kit/`): guida operativa, digest dei vincoli di piattaforma, tre modelli di documento e il
guscio dell'artefatto navigabile. Ha funzionato: le diciassette cartelle sono indistinguibili nella forma,
per quanto diverse nel contenuto.

**Un'applicazione, un agente, un contesto fresco.** L'orchestratore non ha mai implementato nulla: ha
preparato l'elenco, lanciato gli agenti, verificato i rapporti, fatto i commit. Il suo contesto è cresciuto
di poche righe per applicazione invece che di un'intera stesura.

**Commit per ondata.** Il limite di sessione è stato colpito **quattro volte**, uccidendo agenti in coda al
compito. Nessun lavoro è andato perso perché ogni ondata veniva committata appena chiusa, e i punti di
ripresa erano diagnosticabili da disco (epiche vuote, segnaposto residui, file mancanti) senza dipendere da
ciò che l'agente morto aveva in testa. L'ampiezza dell'ondata è stata ridotta da sei a quattro dopo il primo
esaurimento.

## Le due restrizioni d'ambito, entrambe dello sviluppatore

1. **da 60 a 40** — escluse venti applicazioni per **peso normativo**: quelle la cui conformità richiede un
   albo, una licenza, una certificazione di prodotto o la custodia di dati altrui, e quelle il cui obbligo
   cambia più in fretta di quanto una persona sola possa inseguirlo. Criterio e motivazioni una per una in
   [`_escluse/README.md`](../../docs/usecases/_catalogo/_escluse/README.md). Due applicazioni già scritte
   per intero (01 InvoiceGrove, 05 ChatGrove) sono state **spostate** lì invece di essere cancellate; due
   iniziate e interrotte sono state rimosse, perché un documento a metà costa più di quanto valga;
2. **da 40 a 17** — scritte le quattordici già avviate più le **tre raccomandate dal catalogo**
   (31 AuditGrove, 32 TokenGrove, 33 RenewGrove). Le ventitré restanti sono rinviate, non bocciate: si
   costruisce prima qualcosa, poi si scrive il resto con un metodo messo alla prova.

## Il difetto del guscio, e perché è stato corretto al centro

Quattro agenti indipendenti hanno rattoppato **nel proprio blocco di stili locali** lo stesso difetto: su
schermo stretto la pagina scorreva in orizzontale. Tutti e quattro hanno evitato di toccare il guscio
condiviso, per non far divergere il proprio artefatto dagli altri — comportamento corretto, ma il difetto
sarebbe stato ereditato da ogni applicazione successiva.

La causa non era dove sembrava. Non la barra superiore, ma le **colonne di griglia scritte `1fr` nudo**:
`1fr` vale `minmax(auto,1fr)`, e `auto` non scende sotto la larghezza minima del contenuto — una tabella con
larghezza minima sfondava la griglia invece di far scorrere il proprio contenitore. Correzione applicata al
guscio e ai diciannove artefatti esistenti, regola scritta nella guida con l'obbligo di **verificare per
misura e non a occhio**.

## Verifica finale

Eseguita dall'orchestratore, non delegata:

- **allineamento indice ↔ file** nei due sensi su tutte e diciassette le applicazioni: 573 storie, zero
  disallineamenti. Questo controllo ha trovato un difetto che nessun agente aveva visto — in `33-renewgrove`
  una storia era citata dall'indice **e da due storie sorelle** ma non era mai stata scritta, perché
  l'agente era morto proprio lì. Ogni agente vede solo la propria applicazione; nessuno guardava l'insieme;
- **nessun segnaposto residuo, nessuna epica vuota, tre file obbligatori presenti** in ogni cartella;
- **misura in browser reale** dei diciannove artefatti a 390, 768 e 1280 pixel: eccesso orizzontale 0
  ovunque, zero errori JavaScript, nessuna risorsa dalla rete;
- **nessuna collisione fra identificativi** di applicazione, né con le app reali `fatture` e `crm`;
- `./run-tests.sh` completo verde.

## Che cosa torna allo sviluppatore

Tre decisioni trasversali in [`docs/_BACKLOG.md`](../../docs/_BACKLOG.md), due delle quali da chiudere
**prima** di scaffoldare la prima applicazione: l'accesso alle superfici pubbliche senza autenticazione,
l'autenticazione di una macchina che non è una persona, e se promuovere a invariante il divieto di aggregati
per persona. Più, in ogni cartella, la sezione «Rischi e punti aperti» e le proposte di listino e di
classificazione dei dati personali, tutte marcate **da confermare**: nessun agente le ha decise.

La lista di verifica manuale è in [`how-to-test.md`](how-to-test.md).
