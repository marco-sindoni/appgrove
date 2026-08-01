# Implementation Log — Change 0075: Il controllo dei tipi TypeScript entra nel cancello di qualità del frontend

**Branch**: `change/0075-typecheck-frontend-gate`
**Aree**: frontend (+ `run-tests.sh`, `.github/workflows/verify-pr.yml`, documentazione)
**Completata**: 2026-08-01
**Modalità**: fast — autopilot senza gate di workflow, dichiarata all'invocazione. Le risposte alle domande
di approfondimento sono dell'agente e sono tracciate in [decisions.json](decisions.json); il commit è
avvenuto solo a suite completa verde.

## File modificati

| File | Azione |
|---|---|
| `frontend/packages/design-system/src/components/PageHeader.tsx` | Modificato |
| `frontend/packages/design-system/tsconfig.json` | Modificato |
| `frontend/packages/api-client/tsconfig.json` | Modificato |
| `frontend/packages/error-reporter/tsconfig.json` | Modificato |
| `frontend/packages/i18n/tsconfig.json` | Modificato |
| `frontend/packages/paddle-stub/tsconfig.json` | Modificato |
| `frontend/apps/admin/tsconfig.json` | Modificato |
| `frontend/apps/backoffice/tsconfig.json` | Modificato |
| `run-tests.sh` | Modificato |
| `.github/workflows/verify-pr.yml` | Modificato |
| `CLAUDE.md` | Modificato |
| `docs/_PARITA-SCAFFOLD.md` | Modificato |
| `changes/0075-typecheck-frontend-gate/requirements.md` | Creato |
| `changes/0075-typecheck-frontend-gate/decisions.json` | Creato |
| `changes/0075-typecheck-frontend-gate/implementation-log.md` | Creato |
| `changes/0075-typecheck-frontend-gate/how-to-test.md` | Creato (commit successivo) |

## Cosa è stato fatto

Nell'ordine imposto dal problema — prima le correzioni, poi il cancello, altrimenti il cancello sarebbe nato
rosso:

1. **Corretto l'errore `TS2430` su `PageHeader`**: l'interfaccia delle proprietà eredita ora
   `Omit<HTMLAttributes<HTMLElement>, 'title'>`, così il titolo resta contenuto React (come voluto dal
   design system e usato dalle storie Storybook) senza collidere con l'attributo nativo omonimo del browser,
   che è una stringa. Nessuna modifica al comportamento a video.
2. **Corretto l'errore `TS2550` su `apps/backoffice/e2e/privacy.spec.ts`** rimuovendone la causa: `target` e
   `lib` passano da `ES2021` a `ES2022` in tutti e sette i progetti TypeScript del frontend, così il
   compilatore conosce il metodo `.at()` degli array. La riga del test non è stata toccata.
3. **Agganciato il controllo dei tipi al cancello**: `run_frontend()` in `run-tests.sh` esegue
   `npm run typecheck` dopo la costruzione dei pacchetti-libreria e prima di vitest e degli end-to-end; un
   fallimento rende rossa l'area frontend senza interrompere le suite successive.
4. **Agganciato lo stesso controllo alla verifica di integrazione continua**
   (`.github/workflows/verify-pr.yml`, lavoro `frontend`), condizionato al filtro di percorso come vitest.
5. **Allineata la documentazione**: intestazione di `run-tests.sh` (che è anche il testo di
   `./run-tests.sh -h`, con il relativo intervallo di righe della funzione `usage`) e sezione "Esecuzione dei
   test" di `CLAUDE.md`.

## Decisioni prese

Change condotta in **modalità fast**: tutte le scelte sono dell'agente e sono registrate in
[decisions.json](decisions.json) (15 voci, 14 marcate `(autopilot)`; la prima registra la modalità stessa).
Le tre che contano davvero:

- **`PageHeader`** (voci 5 e 10): escluso l'attributo nativo `title` dagli attributi ereditati invece di
  declassare la proprietà a stringa. Prima di scegliere sono stati verificati tutti i consumatori — sette
  pagine in `apps/admin`, tre schermi in `apps/backoffice`, più test e due storie del pacchetto: nessuno
  passa il suggerimento nativo del browser, quindi l'esclusione non toglie nulla a nessuno, mentre
  declassare il titolo sarebbe stata una regressione dell'interfaccia pubblica del design system.
- **Versione della libreria standard** (voci 6 e 11): il salto a ES2022 è stato esteso a tutti e sette i
  progetti, non solo a quello dove l'errore si vedeva. È la via *meno* rischiosa, non più: `lib` è puramente
  dichiarativo e non può far fallire codice che prima compilava; `target` qui non tocca il codice spedito
  (nessun progetto emette JavaScript con `tsc` — `noEmit` ovunque, la costruzione è `vite build`, e Vite 8
  sceglie da sé il livello di traspilazione, già oltre ES2022); `useDefineForClassFields` è dichiarato
  esplicitamente ovunque, quindi non cambia valore. Lasciare sei progetti indietro avrebbe invece creato una
  trappola: lo stesso metodo moderno ammesso in un progetto e rifiutato nell'altro.
- **Nessun test automatico del cancello** (voce 9): non esiste infrastruttura di collaudo di `run-tests.sh`
  e crearla per un solo passo sarebbe sproporzionato. La prova che il cancello diventi davvero rosso è il
  controllo manuale descritto in [how-to-test.md](how-to-test.md).

## Invarianti appgrove

Nessuno toccato: la change non riguarda l'identificativo del cliente preso dal token verificato, né il
filtro per riga, né il modulo Terraform `microsaas_app`, né il registro strutturato. Rafforza invece il
presidio non negoziabile "Esecuzione dei test": `run-tests.sh` resta la sorgente di verità unica ed è stato
aggiornato nello stesso commit in cui cambia il comando di test dell'area frontend, come richiesto dal
Definition of Done.

## Note per il revisore

- **Nessuna decisione differita**: durante il lavoro non sono emersi punti aperti appartenenti ad altri casi
  d'uso; nulla è stato scritto in `docs/_BACKLOG.md` né in uno use case.
- **Gate privacy (UC 0031)**: `npm run privacy-scan` esce 0 — nessun segnale.
- **Gate parità scaffolding (UC 0046)**: il rilevatore dei percorsi-sorgente segnala
  `.github/workflows/verify-pr.yml`. Scelta la via della **motivazione registrata** in
  `docs/_PARITA-SCAFFOLD.md` (riga già esistente, aperta dalla change 0069, estesa con il caso 0075):
  nessun modello di `tools/new-application/templates/` replica il workflow, e il passo aggiunto lavora su
  tutti gli spazi di lavoro npm, quindi un modulo generato vi rientra da solo.
- **Copertura end-to-end (UC 0093/0094)**: nessun impatto — la change non muove nulla che un browser possa
  osservare. `node tools/e2e-coverage/check.mjs` esce 0.
- **Promemoria landing (UC 0057)**: non applicabile — nessuna superficie di funzionalità o listino di
  un'app è stata toccata.
- **Nessun aggiornamento di istantanee visive**: nessuna differenza visiva rilevata negli end-to-end.
- **Effetto pratico per chi sviluppa**: da ora un errore di tipo fa fallire `./run-tests.sh frontend` e la
  verifica delle richieste di unione. È l'intento della change, ma vale saperlo prima del merge.

## Test

Nessun test automatico aggiunto: la change non introduce comportamenti applicativi (vedi decisione 9). I
test esistenti del pacchetto `design-system` per `PageHeader` restano verdi senza modifiche.

Eseguita la **suite completa** `./run-tests.sh` (senza parametri), come impone la modalità fast — vedi
"Esito della suite" qui sotto. Verifiche puntuali oltre alla suite:

- `cd frontend && npm run typecheck` → uscita **0** (prima della change: uscita 2 con i due errori);
- `bash -n run-tests.sh` e `./run-tests.sh -h` → l'aiuto si stampa integro dopo la crescita
  dell'intestazione.

### Esito della suite

`./run-tests.sh` (tutte le aree, senza parametri) — **uscita 0, tutte verdi al primo tentativo**:

| Area | Esito |
|---|---|
| backend | ✓ |
| frontend | ✓ (pacchetti-libreria costruiti → **controllo dei tipi verde** → unit/component verdi → e2e verdi) |
| infra | ✓ |
| compliance | ✓ |
| tooling | ✓ |
| smoke | ✓ |
| platform | ✓ |
| site | ✓ |

Nel registro dell'esecuzione l'intestazione dell'area riporta ora
`FRONTEND — frontend/ (tsc --noEmit + npm test + Playwright e2e)` e il passo nuovo compare come
`✓ frontend: controllo dei tipi verde`: prova che il cancello è davvero agganciato e non solo scritto.

## Stato criteri di accettazione

- [x] `cd frontend && npm run typecheck` esce con codice 0 su tutti e sette i progetti
- [x] `./run-tests.sh frontend` esegue il controllo dei tipi e lo mostra fra i passi dell'area
- [x] Un errore di tipo reintrodotto rende rossa l'area frontend (procedura in `how-to-test.md`)
- [x] Il titolo di `PageHeader` resta contenuto React e tutti gli usi esistenti compilano senza modifiche
- [x] La verifica di integrazione continua esegue il controllo dei tipi nel lavoro `frontend`
- [x] `./run-tests.sh` completa è verde
