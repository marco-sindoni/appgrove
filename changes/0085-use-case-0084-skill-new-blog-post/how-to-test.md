# Come provare a mano la change 0085 — skill `new-blog-post`

Questa è la lista di ciò che devi **vedere con i tuoi occhi**, non una copia dei test automatici. Si parte
dallo strumento (che è la parte che scrive i file) e si finisce sul sito vero, dove l'articolo generato deve
apparire nelle cinque lingue.

Tutti i comandi si lanciano dalla radice del repository, sul branch
`change/0085-use-case-0084-skill-new-blog-post`.

---

## 1. La mappa del blog — il punto di partenza del co-pilota

**Azione**

```bash
node tools/new-blog-post/generate.mjs list
```

**Risultato atteso** — l'albero dei contenuti esistenti, non un elenco piatto: un pilastro
`fatturazione-pmi-ue` con sotto, rientrati, i suoi due articoli cluster, ciascuno con l'app collegata e
l'indirizzo inglese della pagina. È la mappa su cui si decide dove collocare un pezzo nuovo.

**Azione** — la stessa vista, per un programma:

```bash
node tools/new-blog-post/generate.mjs list --json
```

**Risultato atteso** — lo stesso contenuto in JSON, con `pillars[].clusters[]` popolato e `orphans` vuoto.

**Azione** — il messaggio d'uso:

```bash
node tools/new-blog-post/generate.mjs --help
```

**Risultato atteso** — i quattro comandi (`list`, `check`, `scaffold`, `remove`) e, sotto, il formato completo
della specifica: si deve capire cosa scrivere **senza** aprire il codice.

---

## 2. Il rifiuto pulito — la proprietà più importante

Serve una specifica difettosa. Creane una fuori dal repository:

```bash
cat > /tmp/spec-rotta.json <<'JSON'
{
  "key": "prova-rifiuto",
  "kind": "article",
  "datePublished": "2/8/2026",
  "appId": "app-inesistente",
  "pillarKey": "pilastro-che-non-ce",
  "content": {
    "en": { "slug": "index", "title": "T", "description": "D", "question": "Q?", "intro": ["a"],
            "sections": [{ "heading": "H", "paragraphs": ["p"] }],
            "faq": { "title": "F", "items": [{ "q": "q?", "a": "a" }] }, "ctaText": "c" },
    "it": { "slug": "Slug Sbagliato", "title": "   ", "description": "D", "question": "Q?", "intro": ["a"],
            "sections": [{ "heading": "H", "paragraphs": ["p"] }],
            "faq": { "title": "F", "items": [{ "q": "q?", "a": "a" }] }, "ctaText": "c" }
  }
}
JSON
node tools/new-blog-post/generate.mjs check --spec /tmp/spec-rotta.json
```

**Risultato atteso** — un elenco puntato di problemi in **italiano**, leggibile da chi scrive e non da chi ha
scritto lo strumento: lingue mancanti (`fr, es, de`), data non nel formato `AAAA-MM-GG`, slug `index`
riservato, slug italiano malformato, titolo italiano vuoto. In coda, la frase «Niente è stato scritto».
Il comando esce con codice diverso da zero.

**Controllo che conta davvero**

```bash
git status --porcelain site/
```

**Risultato atteso** — **nessun output**. Il rifiuto non deve aver toccato un solo byte del sito. Se qui vedi
righe, la proprietà su cui poggia tutto il resto è rotta.

---

## 3. Generare un articolo vero e vederlo nel sito

Serve una specifica valida. La più rapida: prendi il modello stampato da `generate.mjs --help` e riempilo, con
**la stessa forma in tutte e cinque le lingue** (stesso numero di paragrafi, sezioni e domande). Questa è la
più corta che passa — copiala in `/tmp/spec-buona.json` e completala con le altre tre lingue sullo stesso
stampo:

```json
{
  "key": "prova-a-mano",
  "kind": "article",
  "datePublished": "2026-08-02",
  "appId": "fatture",
  "pillarKey": "fatturazione-pmi-ue",
  "content": {
    "en": { "slug": "manual-check-article", "title": "Manual check",
            "description": "A throwaway article written to verify the generator by hand.",
            "question": "How do I check the blog scaffolding by hand?",
            "intro": ["This article exists only for the manual verification."],
            "sections": [{ "heading": "Why", "paragraphs": ["Because the tests prove the shape, not the prose."] }],
            "faq": { "title": "Questions", "items": [{ "q": "Is it published?", "a": "No, it is removed right after." }] },
            "ctaText": "See how appgrove Invoicing handles this for you" },
    "it": { "slug": "articolo-di-prova-manuale", "title": "Prova manuale",
            "description": "Un articolo usa-e-getta scritto per verificare il generatore a mano.",
            "question": "Come si verifica a mano lo scaffolding del blog?",
            "intro": ["Questo articolo esiste solo per la verifica manuale."],
            "sections": [{ "heading": "Perché", "paragraphs": ["Perché i test provano la forma, non la prosa."] }],
            "faq": { "title": "Domande", "items": [{ "q": "È pubblicato?", "a": "No, viene rimosso subito dopo." }] },
            "ctaText": "Scopri come appgrove Fatture se ne occupa per te" }
  }
}
```

```bash
node tools/new-blog-post/generate.mjs check --spec /tmp/spec-buona.json
node tools/new-blog-post/generate.mjs scaffold --spec /tmp/spec-buona.json
```

Se hai lasciato solo due lingue, `check` te lo dice invece di scrivere: è il punto 2 che si ripresenta.

**Risultato atteso dal comando** — l'elenco degli otto file toccati: i cinque file-lingua, il file di identità
del nuovo post, il registro `site/src/content/blog/index.ts` e l'`index.ts` del **pilastro**.

**Cosa guardare con gli occhi, uno per uno**

1. `site/src/content/blog/<chiave>/it.ts` — deve leggersi come un file scritto a mano: commento di
   intestazione in italiano, virgolette singole, `description` che va a capo, indentazione come negli articoli
   esistenti. Se si riconosce a occhio che è generato, il generatore ha sbagliato stile.
2. `site/src/content/blog/<chiave>/index.ts` — deve avere `kind: 'article'` e `pillarKey` che punta al
   pilastro; **non** deve avere `clusterKeys`.
3. `site/src/content/blog/fatturazione-pmi-ue/index.ts` — la riga `clusterKeys` deve ora contenere anche la
   chiave nuova, in coda alle due esistenti. È l'aggancio reciproco: se manca, i collegamenti interni non si
   chiudono.
4. `site/src/content/blog/index.ts` — l'importazione nuova deve stare **dopo** le altre importazioni di post, e
   la voce in coda all'array. Il commento in testa al file deve essere intatto.

**Azione — il sito deve accettarlo**

```bash
./run-tests.sh site
```

**Risultato atteso** — verde su tutte e tre le parti: vitest, `astro build` (il numero di pagine costruite
sale di cinque rispetto a prima — una per lingua) e il controllo post-build.

**Azione — guardarlo davvero**

```bash
cd site && npm run dev
```

Apri nel browser:

- `http://localhost:4321/en/blog/` — l'indice deve mostrare il nuovo articolo insieme agli altri, con la sua
  descrizione;
- `http://localhost:4321/en/blog/<slug-inglese>/` — la pagina deve mostrare la **domanda-guida** come titolo
  principale, l'introduzione, le sezioni, le domande frequenti, e in fondo il **collegamento all'app**: il
  testo è quello che hai scritto, l'indirizzo è quello della landing di `fatture` **in inglese**;
- `http://localhost:4321/it/blog/<slug-italiano>/` — la stessa pagina in italiano, con lo **slug italiano**
  nell'indirizzo (non quello inglese) e il collegamento che porta alla landing **italiana**;
- ripeti per francese, spagnolo e tedesco: cinque indirizzi diversi, stesso articolo.
- Nella pagina del pilastro (`/it/blog/<slug-del-pilastro>/`) il nuovo articolo deve comparire fra i pezzi
  collegati: è la prova visiva che il riferimento reciproco funziona in entrambe le direzioni.

**Controllo non visivo, sulla stessa pagina** — apri il sorgente della pagina (`view-source:`) e cerca
`FAQPage`: le domande frequenti devono comparire anche nel blocco di dati strutturati, non solo nel testo.

---

## 4. Il ripristino — annullare senza lasciare residui

**Azione**

```bash
node tools/new-blog-post/generate.mjs remove --key <chiave>
git status --porcelain site/
```

**Risultato atteso** — il comando elenca i file toccati e `git status` **non stampa nulla**: la cartella è
sparita, il registro e il pilastro sono tornati esattamente com'erano. È ciò che ti permette di annullare una
generazione sbagliata senza rimettere le mani nei file.

**Verifica incrociata**

```bash
node tools/new-blog-post/generate.mjs list
```

**Risultato atteso** — di nuovo tre post, come al punto 1.

---

## 5. La skill dal vivo — il percorso completo del co-pilota

**Azione** — in una sessione di Claude Code, sul branch della change:

```
/new-blog-post
```

**Risultato atteso, passo per passo**

1. **Modalità** — se non l'hai dichiarata, la prima cosa che vedi è la domanda su classica / autopilot / fast.
2. **Mappa** — la skill esegue `list` e ti mostra i pilastri esistenti **prima** di chiederti qualcosa: non
   deve indovinare lo stato del registro.
3. **Intervista, una domanda alla volta** — di cosa parla il pezzo, sotto quale pilastro sta, che taglio ha,
   qual è la domanda-guida, a quale app rimanda. In prosa, in italiano, senza sigle: se ti trovi davanti a un
   modulo a scelta multipla compatto, la skill sta sbagliando stile.
4. **La fermata sul pilastro nuovo** — questa va provata di proposito. Chiedi un articolo su un tema che i
   pilastri esistenti non coprono (per esempio la gestione dei clienti, non la fatturazione). La skill **deve
   fermarsi** e chiederti se aprire una nuova linea editoriale, invece di deciderlo da sola — **anche in
   modalità fast**. Se prosegue, il presidio non funziona.
5. **La copy** — ti presenta l'inglese sezione per sezione, poi le quattro traduzioni, dicendoti quali scelte
   non sono letterali e **cosa non è riuscita a verificare** (una scadenza, una regola fiscale, una cifra).
   Anche qui **deve fermarsi** ad aspettare la tua approvazione prima di generare.
6. **Generazione e verifica** — chiama `check`, poi `scaffold`, poi `./run-tests.sh site`, e ti dice l'esito.
7. **Chiusura** — apre la change via `new-change` e si ferma ai suoi consensi. Non deve pubblicare nulla e non
   deve fare merge.

**Come si giudica se il risultato è on-brand e pubblicabile** — leggi la copy come se fossi il lettore: il tono
deve essere quello del sito (il lavoro prima, la privacy come firma della fiducia), senza slogan, senza
promesse che il prodotto non mantiene. Le domande frequenti devono essere domande vere, con risposte di due o
tre frasi. Il testo del collegamento all'app deve dire cosa trovi di là, non «clicca qui».

---

## 6. Il cancello automatico

**Azione**

```bash
./run-tests.sh tooling
```

**Risultato atteso** — fra i collaudi delle skill compare anche `new-blog-post`, con i suoi 55 test verdi.

**Azione — provare l'allarme di deriva** (la parte che protegge dal futuro):

```bash
# aggiungi a mano un campo finto ai tipi del blog
printf '\n' >> site/src/content/blog/types.ts
# apri site/src/content/blog/types.ts e aggiungi una riga "  autore: string" dentro BlogPost
( cd tools/new-blog-post && npm test )
```

**Risultato atteso** — due test rossi con il messaggio «il contratto dei post di UC 0042 è cambiato: riallinea
lib/render.mjs e lib/spec.mjs». È il presidio che impedisce a un generatore di continuare a produrre articoli
di forma vecchia senza che nulla diventi rosso.

**Poi annulla la modifica di prova**:

```bash
git checkout site/src/content/blog/types.ts
( cd tools/new-blog-post && npm test )   # di nuovo 55 verdi
```

---

## 7. Documentazione — cosa deve risultare aggiornato

- `docs/usecases/17-skill-e-tooling-contenuto/0084-skill-new-blog-post.md` — c'è la sezione «Stato dopo
  l'implementazione», e fra i punti aperti si legge cosa **non** è stato fatto (nessun articolo vero) e perché.
- `docs/usecases/09-marketing-site/0042-blog-risorse.md` — in coda ci sono i due rimandi tracciati: la
  freschezza dei contenuti pubblicati e la rilettura del gate bozza/pubblicato.
- `docs/usecases/EPICS-WAVE-2.md` — la riga 15 (use case 0084) è a ✅.
- `docs/_BACKLOG.md` — la voce `new-blog-post` fra le skill da creare è marcata **FATTA**, con il testo
  originale conservato per storia.

---

## In sintesi — i cinque controlli che non puoi saltare

1. Una specifica difettosa viene rifiutata e `git status site/` resta **vuoto**.
2. Un articolo generato compare nelle **cinque** lingue con **cinque slug diversi**, e il collegamento all'app
   porta alla landing della lingua giusta.
3. Il pilastro elenca il nuovo articolo, e l'articolo indica il pilastro: il riferimento si chiude nei due sensi.
4. `remove` riporta l'albero **identico**.
5. La skill **si ferma** davanti a un pilastro nuovo e davanti all'approvazione della copy, anche in fast.
