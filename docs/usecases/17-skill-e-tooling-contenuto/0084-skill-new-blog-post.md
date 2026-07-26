# UC 0084 — Skill new-blog-post

**Area**: 17-skill-e-tooling-contenuto · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0042 (blog/risorse del sito vetrina), UC 0057 (skill finalize-landing, per il modello di skill), UC 0046 (skill new-application, per il modello di skill), UC 0040 (ottimizzazione per i motori di ricerca), UC 0041 (ottimizzazione per gli assistenti di intelligenza artificiale)
**Fonte**: R1 (Tabella residui in docs/usecases/_INDEX.md); docs/_BACKLOG.md §"Skill Claude Code da creare" (new-blog-post)
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope

Creare la skill **`new-blog-post`**: un co-pilota che assiste chi scrive nell'**aggiungere un articolo al
blog/risorse** del sito vetrina (UC 0042), gemello di `finalize-landing` (UC 0057) e `new-application` (UC 0046).
Il blog è il motore di crescita nel tempo per i motori di ricerca e per gli assistenti di intelligenza artificiale:
segue il modello editoriale **"pilastro + cluster"** — una pagina **pilastro** ampia per ogni tema, più articoli
**cluster** (guide pratiche, confronti) che vi rimandano con collegamenti interni. Ogni articolo esiste nelle
**5 lingue** (inglese sorgente marketing, poi italiano, francese, spagnolo, tedesco).

La skill è divisa in due parti nette:
1. un **generatore deterministico** (`tools/new-blog-post`) che fa la parte meccanica e ripetibile (creazione dei
   file, aggiornamento del registro, agganci reciproci pilastro↔cluster);
2. un **co-pilota** (la skill vera e propria) che fa le scelte editoriali che uno strumento non può prendere:
   scelta del pilastro, taglio del pezzo, e stesura della copy on-brand "a domanda" nelle 5 lingue.

**Incluso**: file della skill in `.claude/skills/new-blog-post/`; il generatore `tools/new-blog-post`; la stesura
guidata dell'articolo; la chiusura via `new-change` (branch + consenso).
**Escluso**: il motore del blog e la sua validazione (UC 0042, già esistenti — questa skill li **consuma**); la
pubblicazione (la fa l'integrazione continua al merge, UC 0036); le landing per-app (UC 0057); i contenuti
istituzionali del sito (UC 0037).

## 2. Attori & ruoli

- **Autore/Founder**: invoca `/new-blog-post`, risponde all'intervista editoriale, rivede la copy nelle 5 lingue,
  approva requisiti, commit e merge (i tre gate restano suoi).
- **Skill (co-pilota)**: conduce l'intervista, propone pilastro/taglio, redige la copy on-brand, orchestra il
  generatore e la chiusura.
- **Generatore deterministico** (`tools/new-blog-post`): esegue le operazioni meccaniche e ripetibili sul filesystem
  e sul registro dei contenuti.
- **Integrazione continua** (UC 0036): al merge builda e pubblica; non è invocata dalla skill.

## 3. Precondizioni

- Il motore blog di UC 0042 esiste: registro `site/src/content/blog/index.ts`, tipi in `types.ts`
  (`BlogPost`, `PostLocaleContent`, `PostKind` = `pillar` | `article`), validazione in `site/src/lib/blog.ts`.
- Esiste il registro delle landing (`LANDINGS`) da cui la skill risolve il collegamento interno all'app giusta.
- La vetrina Astro (UC 0036) builda in verde prima di partire.
- Ambiente non necessario: la skill è **agnostica rispetto all'ambiente** e non tratta dati personali.

## 4. Flusso principale

1. `/new-blog-post` → la skill dichiara la modalità (classica o autopilot) e apre l'**intervista editoriale**.
2. **Scelta del pilastro**: la skill mostra i pilastri esistenti (dal registro) e propone di collocare l'articolo
   sotto uno di essi come cluster, **oppure** di aprire un nuovo pilastro se il tema non è coperto.
3. **Taglio e tema**: si decide la domanda-guida (`question`, formulata come la si porrebbe a un assistente di
   intelligenza artificiale), il taglio (guida pratica, confronto, spiegazione) e l'app di destinazione (`appId`)
   verso cui puntare il collegamento interno.
4. **Stesura della copy nelle 5 lingue**: la skill genera titolo, descrizione, introduzione, sezioni di corpo e
   voci di domande frequenti (FAQ) on-brand, con tono lean (decisione #14 35), a partire dall'inglese sorgente e poi
   nelle altre quattro lingue. Ogni lingua ha il proprio slug localizzato.
5. **Collegamento interno**: la skill risolve, via registro `LANDINGS`, la landing corretta dell'app e imposta il
   testo del richiamo (`ctaText`) verso di essa, per lingua.
6. **Generazione meccanica**: il generatore `tools/new-blog-post` crea la cartella `site/src/content/blog/<slug>/`
   con i 5 file-lingua `{en,it,fr,es,de}.ts` + `index.ts`, appende la entry nel registro `index.ts`, e — nel modello
   pilastro+cluster — **aggancia i riferimenti reciproci**: aggiunge la chiave del nuovo cluster nella lista
   `clusterKeys` del pilastro e imposta `pillarKey` sull'articolo.
7. **Verifica**: `astro build` + controllo post-build; la validazione di UC 0042 (parità 5 lingue, slug ben formati e
   non riservati, coerenza pilastro↔cluster, `appId` collegato a una landing pubblicata) fa da rete.
8. **Chiusura**: la skill apre la change via `new-change` (branch dedicato) e lascia i tre gate all'autore —
   rilettura requisiti, consenso al commit, consenso al merge.

## 5. Flussi alternativi / edge / errori

- **Nuovo pilastro**: se il tema non ha un pilastro, il generatore crea prima il pilastro (senza cluster) e poi vi
  appende l'articolo, mantenendo coerenti i riferimenti reciproci fin dalla prima esecuzione.
- **Slug in conflitto o riservato**: la validazione di UC 0042 blocca; il generatore rifiuta e propone uno slug
  alternativo prima di scrivere.
- **Lingua mancante**: impossibile per costruzione (il tipo `Record<Locale, PostLocaleContent>` forza la parità delle
  5 lingue a tempo di compilazione); se un file-lingua resta incompleto il build fallisce.
- **`appId` senza landing pubblicata**: la validazione segnala; la skill chiede un'app con landing pubblicata o rinvia
  finché la landing non esiste.
- **Autopilot**: le scelte editoriali seguono l'opzione raccomandata e finiscono in `decisions.json` marcate
  `(autopilot)`; la skill **si ferma e chiede** quando la scelta è di direzione di prodotto (es. aprire un pilastro su
  un tema nuovo che apre una linea editoriale non ancora decisa).
- **Build rosso post-generazione**: la skill non chiude; riporta l'errore e lascia il branch aperto per la correzione.

## 6. Risorse & runbook

- **File skill**: `.claude/skills/new-blog-post/SKILL.md` (co-pilota; conduce intervista e stesura).
- **Generatore deterministico**: `tools/new-blog-post` (parte meccanica): scaffold dei 5 file-lingua + `index.ts`
  della cartella articolo, entry nel registro `site/src/content/blog/index.ts`, aggancio dei riferimenti reciproci
  pilastro↔cluster, aggiornamento della lista `clusterKeys` del pilastro.
- **Comandi**: `/new-blog-post` (avvio); `astro build` + controllo post-build (verifica); `new-change` (chiusura).
- **Runbook**: `/new-blog-post` → intervista (pilastro/taglio/app) → stesura 5 lingue → generatore → `astro build`
  verde → `new-change` → rilettura requisiti → commit → merge → l'integrazione continua pubblica (UC 0036).
- Il registro dei contenuti di UC 0042 è disegnato apposta perché lo scaffolding sia "aggiungi una cartella, appendi
  una riga": la skill si limita a orchestrare quel disegno, non a reinventarlo.

## 7. Dati toccati

Genera **contenuti** del sito vetrina (file TypeScript tipizzati dei post) e aggiorna il registro dei contenuti.
**Nessun dato personale**, nessuna tabella runtime, nessun manifesto dati coinvolto (agnostica rispetto all'ambiente).

## 8. Permessi & gate

- **Invarianti multi-tenancy**: non applicabili (strumento di contenuti, nessuna query tenant-scoped).
- **Gate di processo**: i tre presidi di `new-change` restano dell'autore (rilettura requisiti, consenso al commit,
  consenso al merge). La pubblicazione avviene solo al merge tramite l'integrazione continua (UC 0036), mai dalla skill.
- **Gate di qualità**: parità 5 lingue e coerenza pilastro↔cluster sono bloccanti (validazione UC 0042).

## 9. Requisiti di test

- **Generatore deterministico**: dato un tema/slug, produce cartella con i 5 file-lingua + `index.ts`, entry nel
  registro, e riferimenti reciproci coerenti (nuovo cluster comparso in `clusterKeys` del pilastro, `pillarKey`
  impostato sull'articolo).
- **Validazione a rete** (riuso dei test di UC 0042): parità 5 lingue, slug ben formati/non riservati/unici, coerenza
  pilastro↔cluster, `appId` collegato a una landing pubblicata.
- **Build**: dopo la generazione `astro build` e il controllo post-build restano verdi.
- **Idempotenza/rifiuto**: rieseguire su uno slug esistente non corrompe il registro (rifiuto pulito).
- Prima del merge: le aree toccate di `run-tests.sh` (frontend) verdi.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: UC 0042 (motore blog + registro + validazione), UC 0057 (finalize-landing, modello di skill),
  UC 0046 (new-application, modello di skill), UC 0040 (motori di ricerca), UC 0041 (assistenti di intelligenza
  artificiale, contenuti "a domanda"); decisione #14 35 (tono lean); epica: 0085-unificazione-renderer-email-commons.md.
- **DoD**:
  1. Esiste `tools/new-blog-post` che scaffolda i 5 file-lingua + `index.ts`, appende la entry al registro e aggancia
     i riferimenti reciproci pilastro↔cluster (creando il pilastro se assente).
  2. Esiste la skill `new-blog-post` che conduce l'intervista editoriale e redige la copy on-brand "a domanda" nelle
     5 lingue con collegamento interno alla landing corretta.
  3. La skill chiude con `astro build` verde + controllo post-build e apre la change via `new-change`.
  4. Le scelte sono registrate in `decisions.json` (in autopilot, marcate `(autopilot)`).

## Punti aperti / decisioni differite

- **Contratto consumato da UC 0042**: il registro (`site/src/content/blog/index.ts`), i tipi (`types.ts`:
  `BlogPost`, `PostLocaleContent`, `PostKind`, `clusterKeys`/`pillarKey`) e la validazione (`site/src/lib/blog.ts`)
  sono ciò che questa skill deve consumare; se il contratto evolve, il generatore va riallineato.
- **Set di pilastri come scelta di prodotto**: aprire un nuovo pilastro apre una linea editoriale; in autopilot la
  skill deve fermarsi e chiedere invece di deciderlo da sola. Da annotare in `decisions.json` all'implementazione.
- **Seed dei collegamenti interni**: la mappa articolo→landing dipende da quali app hanno una landing pubblicata; se
  al momento della scrittura l'app non ha ancora landing, il collegamento va rimandato (da tracciare nella change).
