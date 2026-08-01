# UC 0093 — Registro di copertura end-to-end leggibile da programma + check meccanico

**Area**: 20-test-e2e-piattaforma · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0090 (fondamenta suite), UC 0091 (journey utente), UC 0092 (journey admin), UC 0045 (skill `new-usecase`)
**Fonte decisioni**: #10 (testing), CLAUDE.md (tracciamento decisioni differite, registro `decisions.json`)
**Ultimo aggiornamento**: 2026-08-01

## 1. Obiettivo / Scope

Dare alla copertura end-to-end una **memoria strutturata e sorvegliata**: un registro **leggibile da un programma**
che mappa *use case con superficie frontend → journey richiesti → test che li coprono*, più un **check meccanico**
(area `tooling` di `run-tests.sh`) che fallisce quando il registro e la realtà divergono. È il presidio che rende
sostenibile il processo agentico di UC 0094: un obbligo di processo regge solo se un controllo automatico lo fa
rispettare — stessa filosofia di `decisions.json` e della parità di scaffolding (`_PARITA-SCAFFOLD.md`).

**Incluso**: formato e collocazione del registro; il popolamento iniziale (censimento della copertura esistente:
suite di piattaforma + end-to-end L2 + L3); il check meccanico.
**Escluso**: l'aggiornamento del registro a ogni change (è il processo di UC 0094, che questo UC abilita);
la copertura di test unitari/di componente (fuori perimetro: il registro riguarda i percorsi end-to-end).

## 2. Attori & ruoli

- **Agente di sviluppo / sviluppatore**: legge il registro per sapere cosa manca; lo aggiorna a ogni change (da
  UC 0094 in poi).
- **CI / `run-tests.sh tooling`**: esegue il check e blocca le incoerenze.

## 3. Precondizioni

Suite di piattaforma esistente con journey a **ID stabili** (UC 0090–0092). Catalogo use case con l'indicazione
della superficie frontend desumibile (area/contenuto del drill-down).

## 4. Flusso principale

1. **Formato del registro** — `docs/testing/copertura-e2e.yaml` (prosa di accompagnamento in
   `docs/testing/README.md`): un elenco di voci, una per journey, con campi:
   - `id` (stabile, es. `J-BUY`), `titolo` (italiano), `usecases` (gli UC coperti, es. `[0024, 0027, 0077]`),
   - `stato`: `coperto` | `da-coprire` | `escluso`,
   - se `coperto`: `livello` (`platform` | `l2` | `l3`) e `test` (percorso file + riferimento riconoscibile),
   - se `da-coprire`: `motivo` e `possiede` (lo use case che sbloccherà la copertura — stessa logica dei rimandi
     del "Tracciamento delle decisioni differite"),
   - se `escluso`: `motivo` (es. coperto esaustivamente a livello inferiore, costo/beneficio sfavorevole).
2. **Aggancio ai test** — ogni test end-to-end dichiara il journey che implementa con un **tag nel titolo**
   (es. `test('[J-BUY] acquisto e attivazione', …)`): è il legame che il check può verificare senza euristiche.
3. **Popolamento iniziale** — censimento della copertura al momento della change: i journey di piattaforma
   (UC 0090–0092), gli end-to-end L2 esistenti (backoffice + admin) e l'L3 sandbox entrano nel registro; i buchi
   noti nascono come `da-coprire` con motivo e proprietario (es. localizzazione end-to-end → UC 0060).
4. **Check meccanico** — `tools/e2e-coverage/` (Node, senza dipendenze pesanti), eseguito nell'area `tooling`:
   - ogni voce `coperto` punta a un file esistente che contiene il tag del journey (pena: rosso);
   - ogni tag `[J-*]` presente nei test esiste nel registro (pena: rosso — vieta la copertura "fantasma");
   - ogni voce `da-coprire` ha `motivo` e `possiede` valorizzati;
   - ogni use case elencato nel catalogo con superficie frontend è referenziato da almeno una voce (in qualunque
     stato) — l'elenco degli UC "con superficie frontend" è mantenuto **nel registro stesso** (campo dedicato o
     lista di esenzione esplicita), così il check resta deterministico senza interpretare la prosa dei drill-down;
   - exit-code ≠ 0 su qualsiasi violazione, con messaggi che dicono *quale* voce sistemare.

## 5. Flussi alternativi / edge / errori

- **Test spostato/rinominato**: il check fallisce indicando la voce orfana → si aggiorna il registro nello stesso
  commit (è il costo voluto: la mappa non può invecchiare in silenzio).
- **Journey coperto a più livelli** (es. percorso felice in piattaforma + variante d'errore in L2): ammesse più
  voci `test` per la stessa voce di registro, ciascuna col proprio `livello`.
- **Use case senza superficie frontend**: non deve comparire; se il check lo richiede per completezza, va nella
  lista di esenzione con motivo (es. "solo backend/infra").
- **Falso verde**: il check non misura la *qualità* dei test (lo fa la revisione della change); misura che la mappa
  sia vera. Va detto chiaro nel `README.md` del registro.

## 6. Risorse & runbook

- `docs/testing/copertura-e2e.yaml` + `docs/testing/README.md` (formato, esempi, regole di manutenzione).
- `tools/e2e-coverage/` (check + test propri del check su fixture — come `tools/compliance`).
- `run-tests.sh`: il check entra nell'area `tooling` **nello stesso commit** (regola non negoziabile).
- Runbook: come leggere un rosso del check; come aggiungere una voce; come dichiarare un'esenzione.

## 7. Dati toccati

Nessun dato personale: il registro descrive test e file. Nessun manifesto GDPR coinvolto.

## 8. Permessi & gate

Nessuna superficie runtime. Il gate introdotto è di **processo in CI**: `./run-tests.sh tooling` (e quindi
l'esecuzione completa) diventa rosso su registro incoerente. Gli invarianti architetturali non sono toccati; il
registro semmai li rende visibili (i journey che li verificano sono mappati).

## 9. Requisiti di test

- Test del check su fixture: registro valido → verde; voce `coperto` senza test → rosso; tag nel test senza voce →
  rosso; `da-coprire` senza proprietario → rosso; esenzione senza motivo → rosso.
- Popolamento iniziale completo: **zero** use case frontend fuori registro alla chiusura della change (i buchi sono
  ammessi, ma dichiarati come `da-coprire`).
- `./run-tests.sh tooling` verde con il nuovo check integrato.

## 10. Riferimenti & Definition of Done

- **Decisioni**: #10; CLAUDE.md ("Tracciamento delle decisioni differite" — il registro ne è l'applicazione alla
  copertura; "Esecuzione dei test" — aggiornamento di `run-tests.sh` nello stesso commit).
- **DoD**:
  1. registro esistente, popolato con l'intera copertura end-to-end attuale e i buchi dichiarati;
  2. tag `[J-*]` presenti nei test end-to-end esistenti (piattaforma, L2, L3);
  3. check meccanico in area `tooling`, coi suoi test, verde;
  4. documentazione del formato e del processo di manutenzione;
  5. `_INDEX.md` aggiornato dalla change.

## Punti aperti / decisioni differite

- **Estensione oltre il frontend** (journey end-to-end puramente backend/API, es. pipeline webhook): il formato lo
  consentirebbe (`livello` aggiuntivo), ma la decisione appartiene a un eventuale UC futuro — non anticipare.
- **Sincronizzazione con `_INDEX.md`**: un'integrazione più stretta (lo stato di copertura mostrato nell'indice
  degli use case) è possibile ma differita a quando il processo di UC 0094 sarà rodato.
