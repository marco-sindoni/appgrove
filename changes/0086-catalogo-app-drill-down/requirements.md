# Change 0086 — Drill-down del catalogo applicazioni

**Modalità:** `go-fast` (autopilot senza gate di workflow, dichiarata dallo sviluppatore all'invocazione).
**Origine:** richiesta diretta dello sviluppatore, non uno use case del catalogo `docs/usecases/`.
**Ambito:** solo documentazione sotto `docs/usecases/_catalogo/`. Nessuna riga di codice eseguibile,
nessuna infrastruttura, nessun trattamento di dati personali reali.

## Perché

Il file [`docs/usecases/_catalogo/appgrove-catalogo-applicazioni.md`](../../docs/usecases/_catalogo/appgrove-catalogo-applicazioni.md)
descrive **60 idee di applicazione** per il marketplace, ciascuna con una scheda sintetica (descrizione,
pricing indicativo, casi d'uso, entità di dominio, azioni per il livello conversazionale, note
architetturali, riferimenti). È materiale di valutazione: dice *cosa* potrebbe essere costruito, non
*come si costruisce*.

Fra quella scheda e la skill `new-application` — che scaffolda davvero un'app nel monorepo — manca uno
strato: le epiche, le storie utente, il disegno dell'interfaccia e le risposte che la skill pretende
prima di generare (identificatore dell'app, modello utente, porta, metrica di quota, listino, manifesto
dei dati personali). Oggi quello strato andrebbe improvvisato ogni volta, a mano, da chi apre la
cartella dell'app.

## Cosa deve esistere a fine change

Per **ciascuna** delle 60 applicazioni del catalogo, una sottocartella `docs/usecases/_catalogo/NN-<slug>/`
(dove `NN` è il numero d'ordine dell'app nel catalogo, da `01` a `60`) contenente:

1. **`application-description.md`** — descrizione completa e indice dell'applicazione. È il documento
   che si passa in ingresso alla skill `new-application`: contiene già, esplicite, le risposte al
   *gate di identità* (identificatore, modello utente `single`/`multi`, porta locale proposta, metrica
   di quota e sua natura `flow`/`stock`, icona e colore di categoria), la proposta di **listino** e la
   **classificazione dei dati personali** — proposte, non decise: pricing e dati personali restano
   fermate di escalation della skill, che le fa confermare allo sviluppatore.
2. **Una sottocartella per epica**, numerata (`01-<nome-epica>`, `02-…`), coerente con i casi d'uso
   della scheda di catalogo e con l'analisi online del dominio.
3. **Dentro ogni epica, i file di storia utente** numerati progressivamente **a livello di applicazione**
   (`0001-…`, `0002-…`, senza ripartire da 1 a ogni epica). Ogni storia dichiara requisiti funzionali,
   requisiti tecnici e definizione di fatto (*definition of done*), nel rispetto dei principi
   architetturali e di prodotto di appgrove.
4. **`artefatto-ux.html`** — artefatto navigabile, autoconsistente (nessuna risorsa esterna), che
   mostra l'interfaccia dell'applicazione seguendo struttura, sistema di design e approccio di
   esperienza utente di appgrove.
5. **`estensioni-admin.md`** — le estensioni necessarie alla console di amministrazione per governare
   quella specifica app (parametri, quote, viste operative), **oppure** la dichiarazione esplicita che
   non ne servono oltre a quelle standard di piattaforma.

In più, un **kit d'autore condiviso** in `docs/usecases/_catalogo/_kit/`: guida, modelli dei documenti
e guscio HTML del sistema di design, così che le 60 cartelle nascano coerenti fra loro e restino
rigenerabili con lo stesso metro.

## Vincoli di prodotto e architettura da rispettare in ogni storia

Sono gli invarianti della costituzione del repo, non opinioni della singola app:

- identificativo dell'account **solo dal token verificato**, mai dal corpo della richiesta;
- filtro riga per riga su ogni interrogazione legata all'account;
- nuova app = istanza del modulo Terraform `microsaas_app`, mai infrastruttura su misura;
- registrazione strutturata degli eventi con account, applicazione e utente;
- ogni funzionalità esposta anche come strumento per il livello conversazionale (requisito trasversale
  del catalogo);
- interfaccia in 5 lingue, sistema di design condiviso, prova end-to-end per ogni superficie applicativa;
- dati personali dichiarati nel manifesto prima che l'app possa esistere.

## Fuori ambito (dichiarato)

- **Non** si scaffolda nessuna applicazione: nessuna invocazione di `new-application`, nessun servizio
  Quarkus, nessun modulo frontend, nessuna infrastruttura. Questa change produce **solo** i documenti
  che rendono possibile quell'invocazione.
- **Non** si decide quale app costruire per prima né si tocca l'ordine d'esecuzione degli use case:
  è direzione di prodotto, e la raccomandazione del catalogo (AuditGrove, TokenGrove, RenewGrove)
  resta tale.
- **Non** si registrano voci nel registro di copertura end-to-end: nessuna superficie applicativa viene
  creata da questa change. La copertura nascerà con la change che scaffolda l'app.
- I candidati 61–63 della sezione 4 del catalogo (ExtractGrove, SignalGrove, RadarGrove) e il verdetto
  sul sistema di gestione delle traduzioni **restano fuori**: sono candidati valutati, non voci del
  catalogo delle 60. Aggiungerli è un passo successivo, di costo lineare.

## Definizione di fatto

- 60 cartelle applicazione presenti e complete dei cinque artefatti sopra;
- kit d'autore presente e citato da ogni cartella;
- indice di navigazione del catalogo aggiornato con il collegamento a ogni cartella;
- `./run-tests.sh` completo verde prima del commit (la change non tocca codice eseguibile: il verde è
  la prova che non ha rotto nulla per sbaglio);
- `decisions.json` completo e `how-to-test.md` con la lista di verifica manuale.
