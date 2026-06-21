# UC 0045 — skill `new-usecase` (numerazione/scaffolding/indice)

**Area**: 10-skills-tooling · **Fase**: 0 · **Stato**: 🟢 deciso
**Dipendenze**: — (skill autonoma; opera sul solo catalogo `docs/usecases/`)
**Fonte decisioni**: catalogo use case ([README](../README.md)) + processo decisionale [CLAUDE.md](../../../CLAUDE.md)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [11-developer-experience](../../11-developer-experience.md)

## 1. Obiettivo / Scope
Documentare la skill `new-usecase`, lo strumento che **crea, numera e indicizza** un nuovo use case nel catalogo
`docs/usecases/`. La skill assegna l'ID assoluto `NNNN`, scaffolda il file dal template, registra la riga nell'indice
master e guida la compilazione del drill-down.
**Incluso**: numerazione globale a 4 cifre, scelta area/slug, scaffold da `_TEMPLATE.md`, riga nell'indice
`README.md`, gate di review scaffold, gate clarification (coerenza con #01..14), gate consenso commit.
**Escluso**: l'**implementazione** dello use case (è compito di `new-change`, folder `NNNN-use-case-YYYY-…`), e la
modifica delle decisioni di area (la skill non inventa né rivede decisioni).

## 2. Attori & ruoli
- **Developer** (Platform Engineer): invoca la skill, sceglie area/titolo, risponde ai gate, dà il consenso al commit.
- **Skill `new-usecase`** (sistema/tooling): calcola il numero, scaffolda, indicizza, compila il corpo, si ferma ai gate.
- Nessun attore runtime (B2C/B2B/platform-admin/Paddle/cron): UC **meta/tooling**, opera solo sul repo a build-time.

## 3. Precondizioni
- Repo `appgrove` clonato; cwd = monorepo root `/Users/msindoni/Projects/appgrove`.
- Esistono `docs/usecases/_TEMPLATE.md`, `docs/usecases/README.md` e le sottocartelle di area.
- I documenti di decisione `docs/01..14` e gli invarianti `CLAUDE.md` sono la fonte di verità da rispettare.

## 4. Flusso principale
1. **Determina il numero** `NNNN` (globale, 4 cifre):
   - se lo use case è **già a catalogo** (pre-prenotato nell'indice) → usa il suo numero riservato (non ricalcolare);
   - altrimenti → `max(maggior numero file su tutte le sottocartelle, maggior numero nell'indice) + 1`, padded a 4 cifre
     (`0001` se vuoto), e lo aggiunge al catalogo nella fase corretta.
2. **Scelta area**: una domanda con la lista aree (`01-business-legal … 11-apps`).
3. **Titolo/slug**: una domanda; il titolo → kebab-case (max 40 char) = `slug`.
4. **Scaffold**: copia `_TEMPLATE.md` → `docs/usecases/<area>/NNNN-slug.md`; compila l'header (titolo, Area, Fase,
   Dipendenze, Fonte decisioni, Stato `🟡 in corso`).
5. **Indice**: aggiunge la riga `| NNNN | <area> | <titolo> | <deps> | 🟡 |` nella tabella di fase di `README.md`.
6. **🛑 Gate review scaffold (step-01)**: stampa numero/area/titolo/riga indice e **attende conferma** prima di scrivere il corpo.
7. **🛑 Gate clarification (step-02)**: prima di scrivere il drill-down verifica la coerenza con `docs/01..14` e
   `CLAUDE.md`; se un flusso/dato/permesso è ambiguo o ammette alternative, **pone domande mirate** e le risolve. Non inventa decisioni.
8. **Compila il drill-down**: sezioni 1..10 da `_TEMPLATE.md`, rimuove la nota di scaffold, imposta Stato `🟢` e
   aggiorna lo stato nella riga d'indice (oppure resta `🟡` se la stesura è volutamente differita).
9. **🛑 Gate consenso commit (step-02)**: riepiloga file + riga indice e **attende consenso esplicito**; su consenso esegue
   `git add` dei due file + `git commit -m "docs(usecase NNNN): <titolo>"`.

## 5. Flussi alternativi / edge / errori
- **Edge — use case già prenotato a catalogo**: scrivere il file riempie semplicemente il numero riservato; **mai**
  rinumerare gli use case esistenti (ID stabile).
- **Edge — stesura differita**: solo scaffold per ora → Stato resta `🟡`, dichiarato esplicitamente.
- **Edge — numero collisione**: il calcolo `max(file, indice)+1` evita riuso anche se l'indice anticipa numeri non ancora a file.
- **Errore — ambiguità decisionale**: il gate clarification blocca la stesura finché non risolta; la skill non assume una decisione di area.
- **Errore — nessun consenso al commit**: la skill **non committa**; lascia i file in working tree per la decisione del developer.

## 6. Risorse & runbook
**File della skill** (`.claude/skills/new-usecase/`):
- `SKILL.md` — frontmatter (`name`, `description`, trigger `/new-usecase`, `/usecase`), regole di catalogo,
  due step, gate obbligatori, invarianti appgrove da riflettere in §8.
- `step-01-scaffold.md` — calcolo numero, scelta area, slug, copia template, riga indice, gate review.
- `step-02-detail.md` — gate clarification, compilazione sezioni 1..10, gate consenso commit.

**Artefatti prodotti** (per ogni invocazione):
- `docs/usecases/<area>/NNNN-slug.md` — il file dello use case scaffoldato dal template.
- Riga aggiunta in `docs/usecases/README.md` (tabella della fase corretta).

**Runbook**:
1. `/new-usecase` → rispondere a area + titolo → confermare al gate review (passo 6).
2. Rispondere alle domande del gate clarification (passo 7) → la skill scrive il corpo.
3. Al gate consenso (passo 9) dare l'OK → commit `docs(usecase NNNN): <titolo>`.

**Rollback**: nessuna infra/stato remoto. Annullare = `git restore`/`git checkout` dei due file prima del commit, oppure
`git revert` del commit; eliminare il file appena creato e rimuovere la riga d'indice se si scarta lo scaffold.

## 7. Dati toccati
Nessun dato applicativo, nessun database, nessun dato personale (UC meta/tooling). Gli unici artefatti toccati sono
**file Markdown** versionati in git: il nuovo `NNNN-slug.md` e l'indice `README.md`. Manifest GDPR #13 N/A.

## 8. Permessi & gate
- **Invarianti multi-tenancy** (tenant_id da JWT, filtro row-level `WHERE tenant_id`, modulo Terraform `microsaas_app`,
  logging strutturato): **N/A** — la skill è tooling di build-time sul repo, non gira a runtime e non tocca dati tenant.
  È comunque suo **compito** garantire che ogni use case **prodotto** annoti in §8 come rispetta tali invarianti.
- **Gate procedurali** (questi sì, vincolanti): (a) review scaffold, (b) clarification/coerenza con #01..14, (c) consenso
  commit. Nessun entitlement/ruolo/quota runtime applicabile.

## 9. Requisiti di test
Skill di tooling: nessun test automatizzato di prodotto (unit/integration/E2E/L1-L3 #10 N/A). Verifica funzionale
manuale ("DEVE essere verde" prima di considerare la skill corretta):
- numerazione: numero riservato rispettato se a catalogo, altrimenti `max+1` corretto su tutte le sottocartelle;
- file creato in `docs/usecases/<area>/` con naming `NNNN-slug.md` e header coerente;
- riga d'indice aggiunta nella fase giusta con lo stato corretto;
- i tre gate effettivamente bloccano (scaffold review, clarification, consenso commit) e nessun commit avviene senza OK;
- coerenza del contenuto prodotto con `docs/01..14` e gli invarianti `CLAUDE.md`.

## 10. Riferimenti & Definition of Done
- **Decisioni**: catalogo use case ([docs/usecases/README.md](../README.md) — convenzioni di numerazione/aree/fasi);
  processo decisionale ([CLAUDE.md](../../../CLAUDE.md)); template ([_TEMPLATE.md](../_TEMPLATE.md)).
- **DoD**:
  1. La skill `.claude/skills/new-usecase/` esiste con `SKILL.md` + i due step e i tre gate.
  2. Una invocazione produce file `docs/usecases/<area>/NNNN-slug.md` (numerato e scaffoldato dal template) **e** la riga
     d'indice in `README.md`, con stato coerente.
  3. La numerazione assoluta a 4 cifre è rispettata (riservati prima, altrimenti `max+1`); nessun use case esistente rinumerato.
  4. I gate bloccano come previsto e nessun commit avviene senza consenso esplicito.
  5. Questo spec è 🟢 e la sua riga d'indice è aggiornata.
