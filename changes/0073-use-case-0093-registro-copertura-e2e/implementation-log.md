# Log di implementazione — Change 0073 (UC 0093)

**Branch**: `change/0073-use-case-0093-registro-copertura-e2e` · **Modalità**: fast (autopilot senza gate di workflow)
**Data**: 2026-08-01

## Cosa è stato fatto

### 1. Il registro — `docs/testing/copertura-e2e.yaml`

Nuovo file, unica sorgente di verità sulla copertura end-to-end. Contiene:

- **34 percorsi**: 26 `coperto` (13 di piattaforma, 12 di livello 2, 1 di livello 3), 7 `da-coprire`, 1 `escluso`;
- **14 use case con superficie** applicativa esistente in `main` (`usecases_con_superficie`);
- **83 esenzioni** che coprono tutto il resto del catalogo: 55 `senza-superficie`, 9 `vetrina-statica`,
  19 `non-implementato`. 14 + 83 = 97 = l'intero catalogo, senza sovrapposizioni.

I sette buchi dichiarati non sono stati inventati: vengono dai "Punti aperti" già scritti negli use case
0091 (ramo del passaggio di fascia sulla quota di `fatture`; invalidazione delle sessioni al reset password;
localizzazione end-to-end), 0092 (sistema di richieste di assistenza nativo), 0033 (conferma del recesso
per-app non osservabile) e 0034 (limitazione art. 18 sul conto senza effetto; messaggio dedicato all'utente
limitato). L'unica esclusione motivata — allarmi e telemetria durante un guasto — era già decisa in UC 0092.

La leva «account in eliminazione» di A-ENTITLE, che lo use case ipotizzava come possibile buco, **non** è
diventata una voce: la decisione 4 della change `0072` aveva già accertato che quello stato si ottiene con la
richiesta reale di cancellazione, senza leve artificiose.

### 2. L'aggancio ai test — etichetta nel titolo

**42 test** in **25 file** hanno ricevuto l'etichetta del percorso in testa al titolo:
`test('[J-BUY] catalogo → tier → …')`. Solo il titolo è cambiato: nessuna logica di test è stata toccata.
Verificato prima di procedere che nessun test usi il titolo per nominare un'istantanea visiva (nessun
`toHaveScreenshot`/`toMatchSnapshot` nel repository) e che `run.sh --journey <id>`, che filtra per espressione
regolare sul titolo, continui a funzionare.

### 3. Il controllo — `tools/e2e-coverage/`

Node puro con la sola dipendenza `yaml`, sullo stampo di `tools/compliance`. `check.mjs` (uscita con codice
≠ 0 e messaggi raggruppati per regola), `lib.mjs` (le regole), `test/check.test.mjs` (**25 test** su
repository finti costruiti in cartelle temporanee).

Le nove regole: forma del registro · voci `coperto` con test esistenti, del livello coerente con la cartella e
contenenti l'etichetta · ogni etichetta nei test esiste nel registro e nel file dichiarato · ogni test ha
un'etichetta · `da-coprire` con `motivo` e `possiede` esistente · `escluso` con `motivo` · completezza del
catalogo (ogni use case classificato una volta sola) · ogni superficie referenziata da almeno un percorso ·
esenzione `non-implementato` **scaduta** quando esiste già `changes/*-use-case-NNNN-*`.

### 4. `run-tests.sh`

Il controllo entra nell'area `tooling` come sesto blocco, in due comandi (`npm test` sulle cartelle di prova,
`npm run check` sul registro vero). Aggiornata l'intestazione dell'area e l'intervallo di righe letto da
`usage()` (era `2,42`, ora `2,46`): senza quella correzione l'aiuto si sarebbe troncato a metà.

### 5. Documentazione e rimandi

- `docs/testing/README.md`: formato, le tre categorie di esenzione, la convenzione delle etichette, la tabella
  di manutenzione (chi fa cosa e quando), la tabella "come leggere un rosso" per regola, e il limite dichiarato —
  il controllo misura che la **mappa sia vera**, non la qualità dei test.
- `docs/10-testing.md` (sezione F) e `tools/platform-e2e/README.md`: rimandi al registro.
- Rimandi differiti scritti dove li possiede chi verrà dopo: quattro nei punti aperti di **UC 0093**, una
  sezione di consegna nei punti aperti di **UC 0094**, una voce nuova in **`docs/_BACKLOG.md`** (l'indice di
  esecuzione non ha righe per le storie evolutive, per cui `new-change` non riesce a marcarle e il controllo
  ha dovuto usare le cartelle `changes/`).

La menzione in `CLAUDE.md` **non** è stata fatta: è il punto 2 della Definition of Done di UC 0094, insieme al
passo di processo che le dà senso.

## Test

`./run-tests.sh` **completa, tutte e otto le aree verdi** (backend, frontend, infra, compliance, tooling,
smoke, platform, site).

Nota sulla prima esecuzione: l'area `backend` è risultata rossa per un guasto transitorio
dell'infrastruttura di test — `PricingCatalogRealOnlyTest` non è riuscito ad avviare Quarkus perché Flyway non
ha stabilito la connessione con il container Postgres appena avviato (`errore impostando la connessione SSL`).
La change non tocca **nessun** file sotto `services/`; il test rieseguito da solo è passato subito e la
riesecuzione completa della suite è stata verde su tutte le aree. Instabilità della stessa famiglia sono già
tracciate in `docs/_BACKLOG.md` ("Instabilità osservate nell'esecuzione COMPLETA di `run-tests.sh`").

## Definition of Done dello use case

1. ✅ registro popolato con l'intera copertura attuale e i buchi dichiarati;
2. ✅ etichette `[J-*]` nei test di piattaforma, livello 2 e livello 3;
3. ✅ controllo meccanico nell'area `tooling`, coi suoi test, verde;
4. ✅ documentazione di formato e manutenzione (`docs/testing/README.md`);
5. ✅ per assenza: `_INDEX.md` non ha righe per le storie evolutive (decisione 3; divario tracciato nel backlog).
