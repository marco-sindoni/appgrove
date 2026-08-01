# Registro di copertura end-to-end

**Che cos'è.** [`copertura-e2e.yaml`](copertura-e2e.yaml) è la mappa, **leggibile da un programma**, fra
*use case con superficie applicativa*, *percorsi (journey) end-to-end richiesti* e *test che li coprono*.
Serve a rispondere in un posto solo alla domanda «questa funzionalità è coperta da un test end-to-end?»
— senza dover leggere tredici file di test e sei log di implementazione.

**Perché esiste un controllo automatico.** Una mappa scritta a mano invecchia in silenzio: un test viene
rinominato, una funzionalità nasce senza copertura, e nessuno se ne accorge. Il controllo
[`tools/e2e-coverage`](../../tools/e2e-coverage) gira nell'area `tooling` di
[`run-tests.sh`](../../run-tests.sh) e diventa **rosso** appena registro e realtà divergono. Stessa
filosofia di `decisions.json` e della [parità di scaffolding](../_PARITA-SCAFFOLD.md): un obbligo di
processo regge solo se un controllo lo fa rispettare.

**Che cosa il controllo NON misura.** La **qualità** dei test: che asseriscano le cose giuste, che non
siano vuoti, che non passino per caso. Quello lo fa la revisione della change. Il controllo misura una
cosa sola, ma la misura sempre: **che la mappa sia vera**.

Origine: [UC 0093](../usecases/20-test-e2e-piattaforma/0093-e2e-platform-registro-copertura.md).
L'aggiornamento del registro a ogni change è invece il processo di
[UC 0094](../usecases/20-test-e2e-piattaforma/0094-e2e-platform-workflow-skill.md).

## I tre livelli di test end-to-end mappati

| Livello nel registro | Dove vivono i test | Backend | Email |
|---|---|---|---|
| `platform` | `tools/platform-e2e/journeys/` | vero (Postgres + code + servizi) | vere, via Mailpit |
| `l2` | `frontend/apps/*/e2e/` | simulato (`page.route`) | no |
| `l3` | `frontend/apps/*/e2e-l3/` | vero, in ambiente di prova cloud | vere |

Test unitari e di componente **non** entrano nel registro: riguarda i percorsi end-to-end.

## Formato

Il file ha tre sezioni.

### 1. `percorsi` — un elemento per percorso

```yaml
- id: J-BUY                      # identificativo stabile, MAIUSCOLO-CON-TRATTINI
  titolo: Acquisto e attivazione — …   # in italiano, leggibile
  usecases: ["0024", "0025"]     # numeri a 4 cifre FRA VIRGOLETTE, esistenti in docs/usecases/
  stato: coperto                 # coperto | da-coprire | escluso
  test:                          # solo se `coperto`; più voci ammesse (stesso percorso, livelli diversi)
    - livello: platform          # platform | l2 | l3, coerente con la cartella del file
      file: tools/platform-e2e/journeys/J-BUY.spec.ts
```

- `stato: da-coprire` → niente `test`, ma **obbligatori** `motivo` (perché non è coperto) e `possiede`
  (lo use case che sbloccherà la copertura). È la stessa logica dei rimandi del
  [tracciamento delle decisioni differite](../../CLAUDE.md).
- `stato: escluso` → niente `test` e niente `possiede`, ma **obbligatorio** `motivo` (es. coperto in modo
  esaustivo a un livello inferiore, costo sproporzionato al rischio).

### 2. `usecases_con_superficie` — l'elenco autorevole

I numeri degli use case la cui **superficie applicativa interattiva esiste oggi in `main`**. L'elenco sta
nel registro, non viene dedotto dalla prosa dei drill-down: il controllo dev'essere deterministico.
Ogni voce qui **deve** essere referenziata da almeno un percorso, in qualunque stato (anche `da-coprire`).

Per "superficie applicativa" si intende una superficie **navigabile con un browser** dentro il prodotto:
backoffice, console admin, moduli delle app. Il sito vetrina è statico e ha i suoi controlli post-build
(area `site` di `run-tests.sh`): non entra qui.

### 3. `esenzioni` — tutto il resto del catalogo

Ogni use case di `docs/usecases/` che non è in `usecases_con_superficie` **deve** comparire qui con
`categoria` e `motivo`:

| Categoria | Significato | Durata |
|---|---|---|
| `senza-superficie` | Nessuna superficie applicativa propria: servizi, infrastruttura, strumenti, adempimenti di business, librerie di stile | permanente |
| `vetrina-statica` | Superficie del sito vetrina (pagine statiche Astro), coperta dai controlli post-build dell'area `site` | permanente |
| `non-implementato` | La superficie **non esiste ancora** in `main` (storie evolutive) | **temporanea** |

L'esenzione `non-implementato` è **sorvegliata**: appena compare una cartella
`changes/*-use-case-NNNN-*` per quello use case, il controllo diventa rosso. È la guardia che impedisce
a un'esenzione temporanea di diventare permanente in silenzio.

## L'aggancio ai test: l'etichetta nel titolo

Ogni test end-to-end dichiara il percorso che implementa con un'**etichetta in testa al titolo**:

```ts
test('[J-BUY] catalogo → tier → fake Paddle → webhook reale → attivata', async ({ page }) => { … })
```

È l'unico legame fra registro e codice, ed è verificabile senza euristiche. Regole:

- **ogni** test nelle cartelle mappate deve portare un'etichetta (un test senza etichetta è rosso);
- l'etichetta deve esistere nel registro come percorso `coperto` (vieta la **copertura fantasma**);
- il file che la contiene deve essere elencato fra i `test` di quel percorso (vieta la **voce orfana**);
- più test nello stesso file possono condividere la stessa etichetta: la convenzione in uso è
  **un identificativo per file** nei livelli 2 e 3, **un identificativo per percorso** nella suite di
  piattaforma.

L'etichetta non disturba l'esecuzione mirata: `tools/platform-e2e/run.sh --journey J-BUY` filtra per
espressione regolare sul titolo e continua a funzionare.

## Manutenzione — chi fa cosa, e quando

| Situazione | Cosa fare |
|---|---|
| Nuovo test end-to-end | Aggiungi l'etichetta nel titolo e, se il percorso è nuovo, la voce nel registro |
| Test spostato o rinominato | Aggiorna il campo `file` della voce, **nello stesso commit** |
| Nuovo use case (`new-usecase`) | Classificalo: `usecases_con_superficie` oppure `esenzioni` con categoria e motivo |
| Nuova app (`new-application`) | Il test end-to-end generato nasce **senza etichetta**: etichettalo (`L2-<APP>`) e aggiungi la voce. Finché non lo fai, l'area `tooling` resta rossa — è voluto |
| Storia evolutiva che viene implementata | Togli l'esenzione `non-implementato` e dichiara il percorso (anche `da-coprire`) |
| Buco che non puoi tappare ora | Voce `da-coprire` con `motivo` e `possiede` — mai lasciarlo implicito |
| Percorso che non vale la pena coprire | Voce `escluso` con `motivo` che regge una revisione |

Esecuzione del controllo:

```bash
./run-tests.sh tooling                    # dentro l'entrypoint canonico
node tools/e2e-coverage/check.mjs         # diretto, veloce
( cd tools/e2e-coverage && npm test )     # i test del controllo stesso, su cartelle di prova
```

## Come leggere un rosso

Il controllo raggruppa le violazioni per **regola** e nomina sempre la voce da sistemare.

| Regola | Che cosa è successo | Rimedio |
|---|---|---|
| `registro` | Il file manca, non è YAML valido, o mancano le sezioni obbligatorie | Correggi il file |
| `percorso` | Una voce è malformata (identificativo, titolo, use case, stato) | Correggi la voce indicata |
| `coperto` | Una voce `coperto` non ha test, punta a un file inesistente, dichiara il livello sbagliato, o il file non contiene l'etichetta | Aggiorna il campo `file`/`livello`, o aggiungi l'etichetta nel test |
| `etichetta` | Un test non ha etichetta, ha un'etichetta sconosciuta, o vive in un file non dichiarato | Etichetta il test, oppure aggiungi/estendi la voce del registro |
| `da-coprire` | Manca `motivo` o `possiede`, o il proprietario non esiste | Dichiara perché è scoperto e chi lo sbloccherà |
| `escluso` | Manca `motivo` | Motiva l'esclusione |
| `catalogo` | Uno use case non è classificato, o è classificato due volte | Aggiungilo alle superfici o alle esenzioni |
| `esenzione` | Categoria o motivo mancanti, oppure esenzione `non-implementato` **scaduta** | Riclassifica lo use case |
| `superficie-scoperta` | Uno use case con superficie in `main` non è referenziato da alcun percorso | Aggiungi una voce, anche solo `da-coprire` |

Un rosso **non si aggira**: il costo di aggiornare il registro nello stesso commit è esattamente il
prezzo che paga la sua veridicità.
