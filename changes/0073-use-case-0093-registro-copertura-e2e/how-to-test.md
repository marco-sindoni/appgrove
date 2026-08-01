# Come verificare a mano la change 0073 (UC 0093)

Storia strumentale: non c'è quasi nulla da guardare in un browser. Quello che conta è che il **registro
rispecchi la realtà** e che il **controllo diventi rosso** quando smette di rispecchiarla — e che il rosso
dica cosa sistemare.

Tutti i comandi si lanciano dalla radice del repository, sul branch
`change/0073-use-case-0093-registro-copertura-e2e`. Nessuno di essi tocca database, servizi o rete:
il controllo legge solo file.

Prima di iniziare, una volta sola:

```bash
( cd tools/e2e-coverage && npm ci )
```

---

## 1. Il registro esiste ed è leggibile da una persona

**Azione**: apri `docs/testing/copertura-e2e.yaml` e scorrilo dall'alto.

**Risultato atteso**: tre sezioni riconoscibili — `usecases_con_superficie` (14 numeri), `percorsi`
(i percorsi, raggruppati da commenti: piattaforma, livello 2, livello 3, buchi dichiarati, esclusioni) e
`esenzioni` (tutto il resto del catalogo). Ogni voce si capisce senza spiegazioni: titolo in italiano, use
case coperti, stato.

## 2. Il registro rispecchia i percorsi che esistono davvero

**Azione**: confronta l'elenco dei file di test con le voci `coperto` del registro:

```bash
ls tools/platform-e2e/journeys/
ls frontend/apps/backoffice/e2e/ frontend/apps/admin/e2e/ frontend/apps/backoffice/e2e-l3/
grep -c "stato: coperto" docs/testing/copertura-e2e.yaml
```

**Risultato atteso**: 12 file di piattaforma (che contengono 13 percorsi: `J-REG.spec.ts` ne ha due, il
secondo è `J-REG-API`), 12 file di livello 2, 1 di livello 3 → **26** voci `coperto`. Nessun file di test
senza voce, nessuna voce senza file.

## 3. I buchi dichiarati sono veri buchi, non segnaposto

**Azione**: leggi le 7 voci `stato: da-coprire` e confrontale con i "Punti aperti" degli use case citati nel
campo `possiede` (0091, 0058, 0033, 0034 ×2, 0060, 0075).

**Risultato atteso**: ogni buco ha un `motivo` che si regge da solo e un proprietario che esiste davvero nel
catalogo. Nessuna voce dice genericamente "da fare".

## 4. Ogni test porta l'etichetta del proprio percorso

**Azione**:

```bash
grep -rn "^\s*test(" tools/platform-e2e/journeys frontend/apps/backoffice/e2e frontend/apps/admin/e2e | sed 's/async.*//'
```

**Risultato atteso**: **ogni** riga comincia col titolo fra parentesi quadre — `test('[J-BUY] …')`,
`test('[L2-SHELL] …')`. Nessun titolo senza etichetta.

## 5. Il controllo, sul repository sano, è verde

**Azione**:

```bash
node tools/e2e-coverage/check.mjs
```

**Risultato atteso**: una riga sola,
`✓ copertura e2e: docs/testing/copertura-e2e.yaml coerente con i test presenti nel repository.`,
e codice di uscita 0 (`echo $?`).

## 6. I test del controllo stesso passano

**Azione**:

```bash
( cd tools/e2e-coverage && npm test )
```

**Risultato atteso**: 25 test verdi, nessuno fallito. I nomi dei test descrivono in italiano la condizione
verificata («voce `coperto` senza test → rosso», «esenzione `non-implementato` scaduta → rosso», …).

---

## 7. Rompi il registro di proposito: il controllo deve diventare rosso col messaggio giusto

Sono cinque prove. Dopo **ciascuna**, annulla la modifica con
`git checkout -- <file>` (oppure `git checkout -- .`) e ricontrolla che il verde torni.

### 7a. Un test spostato o rinominato

**Azione**: in `docs/testing/copertura-e2e.yaml`, nella voce `J-BUY`, cambia il campo `file` in
`tools/platform-e2e/journeys/J-BUY-SPOSTATO.spec.ts`. Poi `node tools/e2e-coverage/check.mjs`.

**Risultato atteso**: rosso, codice di uscita 1, con **due** messaggi che raccontano i due lati dello stesso
strappo — sezione `[coperto]`
(«il file tools/platform-e2e/journeys/J-BUY-SPOSTATO.spec.ts non esiste — test spostato o rinominato: aggiorna
il registro») e sezione `[etichetta]` («il percorso J-BUY non dichiara questo file fra i suoi `test`»), con
file e numero di riga del test rimasto orfano.

**Poi**: `git checkout -- docs/testing/copertura-e2e.yaml` → il controllo torna verde.

### 7b. Copertura fantasma (etichetta nel test che il registro non conosce)

**Azione**: in `tools/platform-e2e/journeys/J-BUY.spec.ts` cambia l'etichetta del titolo da `[J-BUY]` a
`[J-INVENTATO]`. Poi rilancia il controllo.

**Risultato atteso**: rosso con **due** messaggi coerenti fra loro — sezione `[coperto]`
(«il file … non contiene l'etichetta [J-BUY]») e sezione `[etichetta]`
(«etichetta [J-INVENTATO] assente dal registro — copertura fantasma»).

**Poi**: `git checkout -- tools/platform-e2e/journeys/J-BUY.spec.ts` → verde.

### 7c. Un test nuovo senza etichetta

**Azione**: aggiungi in coda a `frontend/apps/backoffice/e2e/shell.spec.ts` la riga
`test('un test nuovo di zecca', async () => {})`. Poi rilancia il controllo.

**Risultato atteso**: rosso, sezione `[etichetta]`, messaggio che nomina **file e numero di riga** e dice
`il test «un test nuovo di zecca» non porta l'etichetta del percorso — attesa la forma test('[ID] …')`.

**Poi**: `git checkout -- frontend/apps/backoffice/e2e/shell.spec.ts` → verde.

### 7d. Uno use case nuovo non classificato

**Azione**: crea un file finto nel catalogo,
`touch docs/usecases/19-debito-tecnico/0099-prova-registro.md`. Poi rilancia il controllo.

**Risultato atteso**: rosso, sezione `[catalogo]`, messaggio
`use case 0099 non classificato: va in \`usecases_con_superficie\` oppure fra le \`esenzioni\` con categoria e motivo`.

**Poi**: `rm docs/usecases/19-debito-tecnico/0099-prova-registro.md` → verde.

### 7e. Un'esenzione temporanea che è scaduta

È la guardia più interessante: dice che una storia evolutiva è stata implementata e nessuno ha aggiornato
il registro.

**Azione**: `mkdir -p changes/9999-use-case-0095-prova` (lo use case 0095 è oggi esentato come
`non-implementato`). Poi rilancia il controllo.

**Risultato atteso**: rosso, sezione `[esenzione]`, messaggio
`esenzione 0095: esentato come \`non-implementato\`, ma esiste già una cartella changes/*-use-case-0095-* — la superficie ora esiste: classificala e dichiara il percorso che la copre`.

**Poi**: `rmdir changes/9999-use-case-0095-prova` → verde.

---

## 8. Il controllo è dentro l'entrypoint canonico

**Azione**:

```bash
./run-tests.sh -h
```

**Risultato atteso**: nella descrizione dell'area `tooling` compare il punto **(5) e2e-coverage (UC 0093)**,
e l'aiuto **non è troncato** (l'ultima riga è `./run-tests.sh -h`).

**Azione**: rompi di nuovo il registro come al punto 7a, poi lancia `./run-tests.sh tooling`.

**Risultato atteso**: l'area `tooling` finisce **rossa** nel riepilogo e `./run-tests.sh tooling` esce con
codice ≠ 0. Ripristinato il registro, torna verde. *(Attenzione: l'area `tooling` è lenta — genera un'app
vera e ne esegue la suite. Se vuoi solo la prova del controllo, usa `node tools/e2e-coverage/check.mjs`.)*

## 9. La documentazione risponde alle domande che ti verranno

**Azione**: apri `docs/testing/README.md`.

**Risultato atteso**: trovi senza cercare (a) le tre categorie di esenzione e la differenza fra permanenti e
temporanea, (b) la tabella "chi fa cosa e quando" per la manutenzione, (c) la tabella "come leggere un rosso"
con una riga per ciascuna delle regole viste al punto 7, e (d) la dichiarazione esplicita che il controllo
**non** misura la qualità dei test.

## 10. Nessuna regressione visibile sui test esistenti

Questa change ha toccato i **titoli** di 42 test. Non c'è interfaccia da guardare, ma vale la pena di
verificare che l'esecuzione mirata di un percorso continui a funzionare:

**Azione** (richiede Docker attivo, alcuni minuti):

```bash
tools/platform-e2e/run.sh --journey J-REG
```

**Risultato atteso**: il filtro trova ed esegue il percorso (nel resoconto compare
`[J-REG] signup → email verificata davvero → …`) e finisce verde. L'etichetta non ha rotto il filtro per
titolo.
