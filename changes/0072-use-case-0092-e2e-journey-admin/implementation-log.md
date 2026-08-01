# Implementation Log — Change 0072: Batteria journey end-to-end lato amministratore + guasti di piattaforma

**Branch**: `change/0072-use-case-0092-e2e-journey-admin`
**Aree**: `tools/platform-e2e` (suite end-to-end di piattaforma) · `docs/usecases` (indici e rimandi)
**Completata**: 2026-08-01
**Modalità**: fast — autopilot senza fermate di workflow, dichiarata all'invocazione dall'orchestratore
`go-fast`. Le risposte alle domande di approfondimento sono dell'agente e sono tracciate in
[decisions.json](decisions.json) (21 voci, 20 in autopilot).

## File modificati

| File | Azione |
|---|---|
| `tools/platform-e2e/journeys/A-CONSOLE.spec.ts` | Creato |
| `tools/platform-e2e/journeys/A-GDPR.spec.ts` | Creato |
| `tools/platform-e2e/journeys/A-ENTITLE.spec.ts` | Creato |
| `tools/platform-e2e/journeys/F-DEGRADE.spec.ts` | Creato |
| `tools/platform-e2e/service-ctl.sh` | Creato |
| `tools/platform-e2e/helpers/services.ts` | Creato |
| `tools/platform-e2e/run.sh` | Modificato |
| `tools/platform-e2e/playwright.config.ts` | Modificato |
| `tools/platform-e2e/global-setup.ts` | Modificato |
| `tools/platform-e2e/global-teardown.ts` | Modificato |
| `tools/platform-e2e/helpers/api.ts` | Modificato |
| `tools/platform-e2e/helpers/browser.ts` | Modificato |
| `tools/platform-e2e/helpers/db.ts` | Modificato |
| `tools/platform-e2e/README.md` | Modificato |
| `docs/usecases/08-compliance-gdpr/0034-console-diritti-gdpr.md` | Modificato (punti aperti) |
| `docs/usecases/EPICS-WAVE-2.md` | Modificato (0092 → ✅) |

## Cosa è stato fatto

Quattro journey nuovi completano la batteria di piattaforma con la metà che mancava: quello che fa
l'amministratore dalla propria console e quello che l'utente vede quando qualcosa si rompe davvero.
`A-CONSOLE`, `A-GDPR` e `A-ENTITLE` osservano effetti **fra due attori** — due sessioni browser isolate
nello stesso collaudo, l'azione compiuta da una parte e verificata dall'altra — mentre `F-DEGRADE`
produce un guasto **vero**: ferma il processo del servizio dei diritti d'accesso e verifica che l'utente
veda un errore con "riprova", mai un diniego.

Perché F-DEGRADE potesse farlo servivano due cose che la suite non aveva: una leva di controllo del
ciclo di vita dei servizi (`service-ctl.sh` più il descrittore `.run/services.json` generato da `run.sh`
dalla stessa scoperta automatica che accende lo stack) e un ordine di esecuzione che tenesse separati i
journey che muovono stato globale. L'ordine è ora una catena di progetti Playwright —
`chromium → admin-serial → degrade-serial → legal-serial` — e la pulizia finale garantisce che nessun
servizio resti giù.

## Decisioni prese

Tutte in autopilot; il registro strutturato è in [decisions.json](decisions.json). Le principali:

- **Serializzazione in catena** dei journey che toccano stato globale: la dipendenza fra progetti è
  l'unico meccanismo che garantisce sequenzialità stretta anche fra file diversi.
- **Un solo esecutore per il ciclo di vita dei servizi**: la ricetta di avvio viene scritta una volta da
  `run.sh` e riusata per ogni riavvio, invece di essere duplicata dove col tempo divergerebbe.
- **Freschezza osservata col ricaricamento** e non col ritorno sulla scheda del browser: in un browser
  senza finestre quell'evento non è pilotabile in modo deterministico, e ha già copertura fra i test del
  frontend. Un journey che passa "quasi sempre" è un difetto.
- **Limitazione art. 18 applicata all'utente** e non all'account: è l'unico bersaglio che produce un
  effetto osservabile dalla sessione del cliente.
- **`F-DEGRADE` osserva `fatture`**: riavviare il core rilancia la sincronizzazione del listino, che
  riporta il Mini-CRM a disabilitato — l'app sparirebbe per un motivo che col guasto non c'entra.
- **Confronto matrice ↔ menu laterale ristretto alle app con modulo frontend**: in profilo di sviluppo il
  catalogo porta anche app-fixture che nessun modulo serve.
- **Leva d'ambiente per la variante sessione di `F-DEGRADE`**: secondo uso sanzionato di `dbExec`, elencato
  e motivato nella documentazione dell'helper.

## Invarianti appgrove

- **Tenant dal token verificato / filtro per tenant**: nessuna superficie di esecuzione introdotta — i
  journey *verificano* gli invarianti. `A-CONSOLE`, `A-GDPR` e `A-ENTITLE` portano un rilevatore di
  travaso: l'azione dell'amministratore tocca il solo tenant bersaglio e i tenant canarino restano intatti.
- **Modulo Terraform `microsaas_app`**: non pertinente (nessuna infrastruttura).
- **Logging strutturato**: nessun log aggiunto; `A-CONSOLE` verifica che l'azione amministrativa lasci la
  propria traccia persistita con applicazione, operatore e motivazione.
- **Avvio locale di nuove applicazioni**: la leva di controllo dei servizi deriva dalla scoperta automatica,
  quindi una nuova app diventa governabile senza cablaggi manuali.

## Note per il revisore

- **Decisioni differite tracciate** (punti aperti di [UC 0034](../../docs/usecases/08-compliance-gdpr/0034-console-diritti-gdpr.md)):
  1. la limitazione art. 18 applicata all'**account** non ha alcun effetto osservabile per l'utente, perché
     la validità della sessione dipende solo dallo stato della riga utente — da decidere se propagarla o se
     togliere quel bersaglio dalla console;
  2. l'utente limitato riceve «credenziali non valide» al nuovo accesso invece del messaggio dedicato che pure
     esiste nei testi — può essere riservatezza deliberata o svista, da valutare coi flussi di autenticazione
     (UC 0017).
  Entrambe sono direzione di prodotto e non sono state forzate dentro un collaudo.
- **Vincolo d'ordine nuovo**, scritto nel README della suite: dopo `degrade-serial` il Mini-CRM è di nuovo
  disabilitato (il riavvio del core riallinea il catalogo allo YAML). Un journey futuro che ne abbia bisogno
  va collocato prima, o deve riattivarlo da sé.
- **Nessun collaudo di livello 2 rimosso**: i due end-to-end esistenti della console admin sono un percorso
  felice minimo su backend simulato e coprono stati che uno stack vero non sa produrre — il criterio di
  riparto di UC 0091 §1 li ammette.
- **gate privacy** (UC 0031): nessun segnale nel diff. **gate parità scaffold** (UC 0046): nessun
  percorso-sorgente toccato. **Promemoria landing**: nessuna superficie feature/pricing di un'app toccata.
- Nessun contratto fra aree cambia: la change vive dentro l'orchestrazione della suite.

## Test

Suite completa `./run-tests.sh` (nessun parametro) **verde su tutte le aree**: backend, frontend, infra,
compliance, tooling, smoke, platform, site.

Area `platform` — **13 journey su 13 verdi al primo tentativo**, nessuno segnalato instabile (41,5 s per la
batteria). I quattro nuovi:

- `A-CONSOLE` — disabilitazione fra due attori: effetto sul cliente, dati conservati sul database,
  riabilitazione, registro delle transizioni verificato sia sul database sia in console;
- `A-GDPR` — ticket privacy ed export aggregati in console, risposta dell'amministratore visibile al
  cliente, limitazione art. 18 applicata e rimossa con le prove nel registro e l'isolamento provato su un
  tenant canarino;
- `A-ENTITLE` — tre tenant in tre stati, matrice della console coincidente col menu laterale di ciascuno;
- `F-DEGRADE` — servizio fermato davvero, errore con "riprova" e mai diniego, riavvio, rientro senza
  ricaricare la pagina, uscita pulita con sessione invalidata dal lato server.

**Doppia esecuzione consecutiva**: eseguita tre volte di fila. Le prime due hanno fatto emergere due difetti
veri, entrambi corretti alla radice: (1) l'elenco dei servizi per lo spegnimento perdeva l'ultima riga e
lasciava un processo residuo in ascolto; (2) una motivazione fissa si accumulava nel registro fra le corse
rendendo ambigua la riga cercata. Verificato a fine batteria: **nessun processo residuo, nessuna porta della
suite occupata**.

Nessuna istantanea visiva aggiornata (la change non tocca superficie visiva).

## Stato criteri di accettazione

- [x] I quattro journey esistono, girano dentro `./run-tests.sh platform` e sono verdi al primo tentativo.
- [x] `A-CONSOLE` osserva l'effetto nella sessione del cliente, la conservazione dei dati e le due
      transizioni nel registro; lo stato è letto sull'etichetta tradotta.
- [x] `A-GDPR` percorre ticket, export, risposta visibile, limitazione applicata **e rimossa**, con prove e
      isolamento.
- [x] `A-ENTITLE` dimostra la coerenza fra matrice e menu laterale per tre tenant in tre stati.
- [x] `F-DEGRADE` ferma davvero un servizio, verifica errore e non diniego, riavvia e verifica il rientro
      senza ricaricare; a fine esecuzione tutti i servizi sono su.
- [x] Doppia esecuzione consecutiva verde, senza pulizia manuale e senza processi residui.
- [x] `./run-tests.sh` completo verde.
