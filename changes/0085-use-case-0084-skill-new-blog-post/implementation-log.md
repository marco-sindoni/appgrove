# Implementation Log — Change 0085: Skill `new-blog-post`

**Branch**: `change/0085-use-case-0084-skill-new-blog-post`
**Aree**: `tools/new-blog-post` (nuovo), `.claude/skills/new-blog-post` (nuova), `run-tests.sh`, documentazione
**Completata**: 2026-08-02
**Modalità**: fast — le risposte alle domande di approfondimento sono dell'agente e sono tracciate in
[decisions.json](decisions.json); i tre gate di workflow sono stati rinunciati dallo sviluppatore
all'invocazione, con l'obbligo di suite completa verde prima del commit.

## File modificati

| File | Azione |
|---|---|
| `tools/new-blog-post/package.json` | Creato |
| `tools/new-blog-post/generate.mjs` | Creato — riga di comando: `list`, `check`, `scaffold`, `remove` |
| `tools/new-blog-post/lib/spec.mjs` | Creato — forma della specifica e validazione preventiva |
| `tools/new-blog-post/lib/render.mjs` | Creato — resa dei file TypeScript del post |
| `tools/new-blog-post/lib/registry.mjs` | Creato — lettura e modifiche meccaniche del registro |
| `tools/new-blog-post/lib/apply.mjs` | Creato — scrittura transazionale con ripristino |
| `tools/new-blog-post/lib/scaffold.mjs` | Creato — le operazioni complete |
| `tools/new-blog-post/test/*.mjs` | Creati — 55 test (specifica, resa, registro, generazione, deriva del contratto) |
| `.claude/skills/new-blog-post/SKILL.md` | Creato |
| `.claude/skills/new-blog-post/step-01-mappa.md` | Creato — intervista editoriale |
| `.claude/skills/new-blog-post/step-02-copy.md` | Creato — copy 5 lingue + specifica |
| `.claude/skills/new-blog-post/step-03-genera.md` | Creato — generazione e area `site` |
| `.claude/skills/new-blog-post/step-04-chiudi.md` | Creato — chiusura via `new-change` |
| `run-tests.sh` | Modificato — area `tooling`, punto (8) |
| `docs/usecases/17-skill-e-tooling-contenuto/0084-skill-new-blog-post.md` | Modificato — esito + punti aperti |
| `docs/usecases/09-marketing-site/0042-blog-risorse.md` | Modificato — due rimandi tracciati |
| `docs/usecases/EPICS-WAVE-2.md` | Modificato — riga 15 a ✅ |
| `docs/_BACKLOG.md` | Modificato — voce `new-blog-post` chiusa |
| `docs/testing/copertura-e2e.yaml` | Modificato — motivo dell'esenzione di 0084 esteso |

## Cosa è stato fatto

La skill `new-blog-post` esiste ora nelle sue due metà. La metà meccanica è `tools/new-blog-post`: prende un
file di specifica in formato JSON — il contenuto editoriale nelle 5 lingue, scritto dal co-pilota — lo valida
e lo materializza in `site/src/content/blog/<chiave>/` (cinque file-lingua più il file di identità), appende
l'entry al registro dei contenuti e aggancia i riferimenti reciproci fra il pilastro e il suo articolo cluster,
creando il pilastro nella stessa esecuzione quando non esiste ancora. La metà conversazionale è la skill in
`.claude/skills/new-blog-post/`: conduce l'intervista editoriale, scrive la copy, la fa approvare, chiama il
generatore, verifica con l'area `site` e chiude via `new-change`.

Il generatore rifiuta **prima di scrivere** e ripristina se una scrittura fallisce a metà; `remove` è il suo
inverso esatto, così un build rosso dopo la generazione si annulla invece di lasciare residui. Il tutto è
provato da 55 test nell'area `tooling`, fra cui un allarme di deriva che confronta i campi scritti dal
generatore con i tipi veri del blog.

## Decisioni prese

Condotta in modalità fast: tutte le scelte sono dell'agente e stanno in [decisions.json](decisions.json)
(20 voci, 19 marcate `(autopilot)`). Le portanti:

- **il contratto fra le due metà è un file di specifica JSON** (decisione 3): la copy la scrive il co-pilota,
  i file li scrive lo strumento. L'alternativa — lasciare che il modello scriva direttamente i file TypeScript
  — avrebbe rimesso la parte fragile e ripetitiva nelle mani di un modello linguistico, cioè proprio ciò che il
  generatore esiste per togliere di mezzo;
- **due padroni della validazione, con confini netti** (decisione 4): la validazione autorevole resta in
  `site/src/lib/blog.ts` (UC 0042); il generatore controlla solo ciò che gli serve per non corrompere il
  registro e per non produrre un post che farebbe fallire quella validazione a valle;
- **scrittura transazionale e operazioni simmetriche** (decisioni 5, 13, 14): ogni modifica al registro ha il
  suo inverso esatto, e il ciclo genera→rimuovi riporta i file identici byte a byte;
- **allarme di deriva sul contratto** (decisione 8): era un punto aperto esplicito dello use case 0084 ed è
  ora un test — contratto del blog evoluto senza riallineare il generatore = suite rossa;
- **due fermate obbligatorie nella skill** (decisione 16), valide anche in fast: aprire un nuovo pilastro
  (linea editoriale = direzione di prodotto) e approvare la copy prima di generare (pubblicazione verso
  l'esterno);
- **nessun articolo vero scritto da questa change** (decisione 9): scegliere tema e taglio di un pezzo
  pubblicato è lavoro editoriale del founder, non di piattaforma.

## Invarianti appgrove

Nessuno degli invarianti multi-tenancy è toccato: lo strumento genera contenuti statici del sito vetrina, non
esegue query, non riceve richieste con token e non scrive log applicativi. L'invariante di piattaforma
rispettato è la **fonte unica**: il generatore non duplica le regole di UC 0042 né introduce stili propri —
colori e resa restano del template del blog, che consuma il brand kit condiviso (UC 0086).

## Note per il revisore

- **Rimandi tracciati** (decisione 19). In [UC 0042](../../docs/usecases/09-marketing-site/0042-blog-risorse.md),
  che possiede il motore del blog: la **freschezza dei contenuti pubblicati** — un articolo che cita una
  funzionalità, un prezzo o una scadenza normativa eredita la scadenza di quell'affermazione, e oggi nulla la
  sorveglia; più la rilettura della nota su bozza/pubblicato, che resta non necessaria. In
  [UC 0084](../../docs/usecases/17-skill-e-tooling-contenuto/0084-skill-new-blog-post.md): nessun articolo vero
  scritto qui, e il lavoro manuale che resta quando l'allarme di deriva diventa rosso.
- **Verifica sul repository vero, non solo su cartelle di prova** (decisione 18): è stato generato un articolo
  usa-e-getta agganciato al pilastro reale, eseguita l'area `site` (156 test verdi, 72 pagine costruite,
  controllo post-build verde) e poi rimosso, verificando con git che l'albero tornasse identico.
- **Gate privacy (UC 0031)**: nessun segnale — scanner deterministico eseguito sul diff finale, exit 0.
- **Gate parità scaffold (UC 0046)**: nessun percorso-sorgente dei modelli toccato.
- **Copertura end-to-end (UC 0093/0094)**: nessun impatto — UC 0084 resta classificato `senza-superficie`, con
  il motivo esteso a dire dove sta davvero il presidio. `node tools/e2e-coverage/check.mjs` verde.
- **Contratto cross-area**: il generatore consuma il contratto dei contenuti blog di UC 0042. Se quel contratto
  cambia, il presidio è il test di deriva; la riparazione giusta è riallineare `lib/render.mjs` e
  `lib/spec.mjs`, **non** allargare gli elenchi del test.

## Test

- **`tools/new-blog-post`** (nuovo, area `tooling`): 55 test con `node --test`.
  - *specifica*: ogni motivo di rifiuto ha il suo caso — lingua mancante o di troppo, stringa vuota, slug
    malformato/riservato/duplicato, chiave duplicata, data non ISO, articolo senza pilastro, pilastro con
    pilastro, `clusterKeys` dichiarati a mano, traduzione con una sezione o una voce FAQ in meno della sorgente;
  - *resa*: nome dell'export in forma cammello, scelta delle virgolette con apostrofi, tutti i campi presenti,
    pilastro senza `clusterKeys`;
  - *registro*: aggiunta e rimozione simmetriche, ordine delle importazioni, doppioni rifiutati, lista dei
    cluster che nasce e si estingue, lettura del registro reale;
  - *generazione*: articolo agganciato al pilastro, pilastro e primo cluster nella stessa esecuzione, ciclo
    andata-ritorno identico, cinque casi di rifiuto con confronto byte a byte del repository, fallimento a metà
    che non lascia stato ibrido;
  - *deriva del contratto*: campi di `BlogPost` e `PostLocaleContent`, tipi di post, elenco delle 5 lingue e
    slug riservati letti dalle fonti vere di UC 0042.
- **Suite completa** `./run-tests.sh` (senza parametri), come prescrive la modalità fast: **8 aree su 8 verdi**
  — backend, frontend, infra, compliance, tooling, smoke, platform, site. Exit 0.
- Nessuna base di riferimento visiva aggiornata (nessuna differenza).

## Stato criteri di accettazione

- [x] Data una specifica valida, il generatore crea cartella, cinque file-lingua e file di identità, appende
      l'entry al registro e aggancia i riferimenti reciproci; con pilastro e articolo nella stessa specifica,
      entrambi nascono coerenti in una sola esecuzione.
- [x] Una specifica non valida è rifiutata prima di scrivere, con un messaggio che dice cosa correggere; il
      registro resta identico byte a byte.
- [x] Il ciclo andata-ritorno è simmetrico (verificato sia sulle cartelle di prova sia sul repository vero).
- [x] Esiste la skill `new-blog-post` che conduce l'intervista, redige la copy nelle 5 lingue con il
      collegamento interno alla landing corretta, chiama il generatore e chiude via `new-change`; si ferma e
      chiede prima di aprire un nuovo pilastro e prima di pubblicare la copy.
- [x] `run-tests.sh` esegue i test del nuovo strumento nell'area `tooling`, e la suite completa è verde.
- [x] Se i campi dei tipi del blog cambiano senza che il generatore sia riallineato, la suite diventa rossa.
