# Implementation Log — Change 0095: semantica dei tre ruoli come contratto verificabile

**Branch**: `change/0095-use-case-0101-semantica-ruoli`
**Use case**: [0101](../../docs/usecases/22-refactor-membership-model/story/0101-semantica-ruoli-viewer-editor-admin.md)
(epica 22, sotto-epica E22.1 Fondamenta) · **Sostituisce** UC 0072 dell'epica 14
**Aree**: `services/commons`, `services/fatture`, `services/crm`, `frontend/packages/*`,
`frontend/apps/backoffice`, `tools/new-application`, `tools/scaffold-parity`, documentazione
**Completata**: 2026-08-22
**Modalità**: **fast** — dichiarata dall'orchestratore `go-fast`. Le risposte alle domande di
approfondimento sono dell'agente e sono tracciate una per una in [decisions.json](decisions.json)
(30 voci). Lo sviluppatore ha rinunciato in anticipo ai tre gate di workflow; le contropartite —
suite completa verde, registro integrale, guida di collaudo **scritta ed eseguita** — sono state onorate.

## File modificati

| File | Azione |
|---|---|
| `services/commons/.../access/AppOperationsContract.java` | Creato — il contratto, con la cascata di classificazione nel javadoc |
| `services/commons/.../access/AppOperation.java` | Creato — la riga del documento: ruolo minimo **o** motivo dell'esenzione |
| `services/commons/src/test/.../access/AppOperationsContractVerifier.java` | Creato — il collaudo riusabile (test-jar), tre direzioni |
| `services/commons/src/test/.../access/AppOperationsContractVerifierTest.java` | Creato — auto-collaudo: 8 difetti che devono diventare rossi |
| `services/commons/src/test/.../access/AppOperationTest.java` | Creato — stato illegale non rappresentabile |
| `services/fatture/.../FattureOperationsContract.java` | Creato — 6 operazioni classificate |
| `services/fatture/.../InvoiceResource.java` | Modificato — `viewer` sulla classe, `editor` sulle tre scritture |
| `services/fatture/.../QuotaResource.java` | Modificato — esenzione dichiarata nel javadoc |
| `services/fatture/.../Roles.java` | Modificato — comprende `member`; javadoc che spiega perché non decide nulla |
| `services/fatture/src/main/resources/META-INF/openapi/openapi.{json,yaml}` | Modificato — istantanea rigenerata dalla compilazione |
| `services/fatture/src/test/.../AppOperationsContractTest.java` | Creato |
| `services/fatture/src/test/.../AppRoleGateTest.java` | Creato — 7 collaudi di integrazione per ruolo |
| `services/fatture/src/test/.../MockAppRoleService.java` | Creato — finta sorgente del ruolo, qualificata rete di sicurezza |
| `services/fatture/src/test/.../{TestProjection,ProjectionResetCallback}.java` | Modificato — copia locale del ruolo |
| `services/fatture/src/test/resources/application.properties` | Modificato — chiavi della copia locale del ruolo |
| `services/crm/.../CrmOperationsContract.java` | Creato — 11 operazioni, tutte e tre le righe della cascata |
| `services/crm/.../SeatResource.java` | Modificato — `viewer` in lettura, `admin` su assegna, `admin`+rilettura su revoca |
| `services/crm/.../QuotaResource.java` | Modificato — esenzione dichiarata |
| `services/crm/src/test/.../AppOperationsContractTest.java` | Creato |
| `services/crm/src/test/.../AppRoleGateTest.java` | Modificato — governo degli accessi, rilettura obbligatoria, esenzione |
| `frontend/packages/api-client/src/app-role.ts` (+ test) | Creato — `appRoleAtLeast`, gemello di `AppRole.atLeast` |
| `frontend/packages/api-client/src/problem.ts` | Modificato — `refusalMessage` **promosso** qui dal modulo del Mini-CRM |
| `frontend/packages/design-system/src/components/DisabledForRole.tsx` (+ test) | Creato — «presente ma disabilitato, con la spiegazione» |
| `frontend/packages/i18n/src/resources/{en,it,fr,es,de}.ts` | Modificato — sezione `roles` nelle cinque lingue |
| `frontend/apps/backoffice/src/modules/crm/api/errors.ts` | **Eliminato** — promosso al pacchetto condiviso |
| `frontend/apps/backoffice/src/modules/crm/screens/*.tsx` | Modificato — importano dal pacchetto |
| `frontend/apps/backoffice/src/modules/fatture/screens/*.tsx` | Modificato — **difetto corretto**: mostrano la frase del server |
| `frontend/apps/backoffice/e2e/roles.spec.ts` | Creato — i due rifiuti letti a parole (livello 2) |
| `tools/new-application/templates/**` | Modificato/Creato — i modelli ereditano tutto (parità verde) |
| `tools/new-application/lib/context.mjs` | Modificato — la lista dei ruoli di piattaforma comprende sempre `member` |
| `tools/scaffold-parity/parity.config.json` | Modificato — `RequiresAppRole` fra le annotazioni portanti |
| `docs/04-services-backend.md` | Modificato — sezione «Semantica dei tre ruoli» |
| `docs/testing/copertura-e2e.yaml` | Modificato — vedi «Copertura end-to-end» |
| `docs/_BACKLOG.md` | Modificato — divergenza osservata nel presidio di parità |
| `docs/usecases/22-*/story/{0107,0111,0114}.md` | Modificato — rimandi differiti |
| `docs/usecases/22-*/epic/E22-01-*.md` | Modificato — 0101 ✅ |

## Cosa è stato fatto

Il varco del ruolo esisteva (UC 0099) ma **non diceva quando usarlo**: il Mini-CRM lo dichiarava a
sentimento, `fatture` non lo dichiarava affatto. Questa change trasforma la semantica dei tre ruoli in un
contratto **verificabile**: una cascata di classificazione scritta una volta sola, un `AppOperationsContract`
per applicazione che dichiara **tutte** le proprie operazioni con il ruolo minimo (o il motivo
dell'esenzione), e un verificatore nel test-jar di `commons` che confronta il documento col codice in tre
direzioni — compresa quella che coglie la rotta aggiunta domani e dimenticata. Le due applicazioni esistenti
sono classificate e collaudate per ruolo; i modelli-sorgente della skill `new-application` ereditano tutto,
così l'applicazione numero tre non nasce antiquata. Lato interfaccia nasce l'involucro condiviso
`DisabledForRole` con le sue traduzioni nelle cinque lingue.

## Decisioni prese

Tutte in [decisions.json](decisions.json), 30 voci, quelle dell'agente marcate `(autopilot)`. Le portanti:

- il documento delle operazioni è un **contratto Java** e non un file YAML, perché il collaudo deve poter
  legare la riga dichiarata al codice reale (classe + metodo, per riflessione);
- ruolo minimo ed esenzione sono **alternativi** e l'esenzione porta un **motivo obbligatorio**: due stati
  illegali diventano non rappresentabili, e sottrarsi al controllo costa una frase scritta;
- il verificatore controlla anche «**reale → dichiarato** per ogni operazione, letture comprese»: è la
  direzione che rende il documento *completo* invece di soltanto *coerente*;
- l'auto-collaudo del verificatore esiste perché un verificatore mai visto fallire non è una prova;
- lo **stato di quota** delle due applicazioni è esente, con il motivo scritto — scelta già assunta in
  UC 0099 e ora verificata: proteggerla rende rossa la suite;
- `refusalMessage` **promosso** a `@appgrove/api-client`, come il suo stesso commento prescriveva.

## Difetti trovati e corretti in questa change

1. **Le schermate di `fatture` buttavano via la frase del server** su ogni rifiuto diverso da un 429,
   mostrando «Si è verificato un errore. Riprova»: lo stesso difetto che il collaudo manuale del 2026-08-21
   trovò nel Mini-CRM. Latente finché `fatture` non aveva il varco del ruolo, **vivo** da questa change su
   un'applicazione attiva. Corretto; provato rosso prima e verde dopo con `e2e/roles.spec.ts`.
2. **La lista dei ruoli di piattaforma di `@RolesAllowed` rendeva il varco del ruolo irraggiungibile su
   `fatture`**: elencava `owner` e `admin`, Quarkus la applica *prima* dei filtri, e ogni collaboratore
   riceveva un 403 **senza corpo**. La classificazione sarebbe stata una dichiarazione senza effetto per
   tutti tranne l'owner. Trovato **eseguendo la guida di collaudo**. Corretto rendendo la lista un «appartieni
   a un account»; presidiato da un collaudo verificato rosso prima della correzione.

## Rimandi tracciati (decisioni differite)

- **UC 0107** — il cablaggio di `DisabledForRole` nelle schermate: serve la lettura del ruolo nella shell,
  che è la fondazione di quella storia. Forma d'uso già scritta là.
- **UC 0111** — il varco vecchio sui ruoli di piattaforma sta ancora davanti a quello del ruolo e su
  `SeatResource` lo copre: un `member` di piattaforma che è `admin` sull'applicazione non arriva a decidere.
  Non è una regressione, ma è un potere dichiarato e non erogabile; si chiude col ritiro dei posti, e i due
  collaudi di governo andranno riportati a un token `member` come prova.
- **UC 0114** — `Roles.java` e il commento che nomina «B2C single-user».
- **docs/_BACKLOG.md** — il presidio di parità non confronta il **corpo** dei file di test: un aiutante nuovo
  in `fatture` non viene richiesto al modello (caso concreto: `ageBySeconds` e i collaudi della scadenza,
  change 0094). Proprietario UC 0046.

## Invarianti appgrove

- **Tenant dal solo JWT verificato**: nessuna operazione nuova, nessun parametro nuovo. Il ruolo continua a
  non stare nel token e a leggersi dal modello.
- **Filtro row-level**: nessuna query nuova.
- **Modulo Terraform `microsaas_app`**: infrastruttura non toccata.
- **Logging strutturato**: il varco già registra `app_id`, ruolo richiesto e ruolo posseduto; nessun log nuovo.

## Note per il revisore

- **Superficie di autorizzazione toccata** su `fatture`: `@RolesAllowed({owner, admin})` →
  `{owner, admin, member}`. Non è un allargamento incondizionato — un collaboratore senza riga di
  `app_access` riceve `403 no-access` — ma è una **concessione deliberata dell'owner** attraverso
  l'endpoint costruito per questo (UC 0098). È la scelta minima che rende esercitabile la classificazione:
  l'alternativa (rimuovere `@RolesAllowed`) è il ritiro del modello vecchio e appartiene a UC 0111/0114.
- La landing di `fatture` è **pubblicata** e questa change ha toccato il servizio e il modulo dell'app: non
  ne cambia però né funzionalità né listino — solo *chi* può usarle e come si legge un rifiuto. Nessuna
  ri-cattura di `finalize-landing` sembra necessaria; la segnalazione è qui per completezza.
- `services/fatture/src/main/resources/META-INF/openapi/*` sono **rigenerati dalla compilazione** (la
  variazione è la comparsa di `member` fra gli scope): non sono scritti a mano.

## Test

| Area | Cosa | Esito |
|---|---|---|
| `services/commons` | `AppOperationTest` (7), `AppOperationsContractVerifierTest` (9: uno verde + otto difetti che diventano rossi) | verde |
| `services/fatture` | `AppOperationsContractTest` (2), `AppRoleGateTest` (7) + suite completa (52) | verde |
| `services/crm` | `AppOperationsContractTest` (2), `AppRoleGateTest` (14) + suite completa (59) | verde |
| `frontend` | `app-role.test.ts` (6), `DisabledForRole.test.tsx` (5, con `jest-axe`), tipi (`tsc --noEmit`), 36 file / 233 prove | verde |
| `frontend` e2e L2 | `e2e/roles.spec.ts` (2) — **provate rosse** prima della correzione delle schermate | verde |
| `tooling` | parità scaffolding, registro di copertura | verde |
| **Suite completa** | `./run-tests.sh` senza parametri — backend, frontend, infra, compliance, tooling, smoke, platform, site | **tutte verdi** |

- **gate privacy (UC 0031)**: `npm run privacy-scan` → «nessun segnale privacy nel diff». Nessun dato
  personale nuovo, nessuna tabella nuova, nessuna finalità nuova → classificazione **nessun impatto**.
- **gate parità scaffold (UC 0046)**: il rilevatore ha segnalato tre percorsi-sorgente (`services/fatture`,
  `services/commons`, `frontend/apps/backoffice/src/modules/fatture`); scelta la **via 1** — modelli
  aggiornati nello stesso commit — e `parity-check.mjs` è verde.
- **nessuna linea di riferimento visiva** (snapshot) toccata.

### Copertura end-to-end

Risposta: **coprire ora**, e non era la risposta prevista dalla storia. La voce `J-APP-ROLE-REFUSALS`
(posseduta da UC 0099) dichiarava per iscritto il proprio grilletto — «si copre … o quando `fatture` (già
attiva) adotterà il varco del ruolo» — e questa change fa esattamente quello. Passa quindi a **coperto**
con `frontend/apps/backoffice/e2e/roles.spec.ts` (livello 2), UC 0101 entra fra gli use case con superficie,
e nasce **`J-ROLES`** (`da-coprire`, proprietario UC 0113) per il percorso dei quattro ruoli, che ha bisogno
di UC 0107 e UC 0111. Rimossa l'esenzione `non-implementato` di 0101. `node tools/e2e-coverage/check.mjs`
verde.

### Guida di collaudo manuale

[how-to-test.md](how-to-test.md) è stata **scritta ed eseguita**: tutti i passi non visivi (§1 comprese le
tre manomissioni deliberate, §2, §3, §5, §6, §7) sono stati eseguiti sullo stack locale il 2026-08-22.
L'esecuzione ha prodotto **quattro correzioni alla guida** e ha scoperto il **difetto n. 2** qui sopra.
Restano allo sviluppatore i passi **visivi** di §4, che sono la ragione per cui la guida esiste.

## Stato criteri di accettazione

- [x] R1 — formato del documento delle operazioni in `services/commons`
- [x] R2 — collaudo riusabile a tre direzioni + auto-collaudo che lo vede fallire
- [x] R3 — `fatture` classificata e collaudata, con la sua infrastruttura di prova
- [x] R4 — `crm` completata con le operazioni di governo (`admin`, rilettura sulla revoca)
- [x] R5 — collaudi di integrazione per ruolo su entrambe le applicazioni
- [x] R6 — `DisabledForRole` + `appRoleAtLeast` + traduzioni nelle cinque lingue
- [x] R7 — modelli-sorgente allineati, `RequiresAppRole` fra le annotazioni portanti
- [x] R8 — la cascata documentata in `docs/04-services-backend.md`
- [x] DoD 1–5 della storia · DoD 6: `./run-tests.sh` **completo** verde (non solo `backend frontend`)
