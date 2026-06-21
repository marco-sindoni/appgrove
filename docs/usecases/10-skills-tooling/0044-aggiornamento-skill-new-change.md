# UC 0044 — `new-change` update (4 cifre + variante use-case + gate test/snapshot + hook privacy/RoPA)

**Area**: 10-skills-tooling · **Fase**: 0 · **Stato**: 🟡 in corso (skill base esistente; hook privacy/RoPA e snapshot da completare in UC 0031/UC 0030)
**Dipendenze**: — (skill di build-time; opera sul repo)
**Fonte decisioni**: #07 (workflow PR→CI), #10 (gate test/E2E/baseline), #13 (gate privacy/RoPA)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [07-devops-cicd](../../07-devops-cicd.md), [10-testing](../../10-testing.md), [13-compliance-privacy](../../13-compliance-privacy.md)

## 1. Obiettivo / Scope
Specificare la skill `new-change`, lo strumento che guida **ogni modifica implementativa** del monorepo (branch + PR),
e portarla alla forma definitiva.
**Incluso**: numerazione a **4 cifre** `NNNN-descrizione-breve`; **variante da use case** `NNNN-use-case-YYYY-descrizione`
(YYYY = numero dello use case sorgente); campo **"Source use case"** nei requirements; i **gate obbligatori**
(requirements review, consenso commit, consenso merge); la **verifica test stack-aware** alla chiusura (per area toccata);
gli **hook futuri** privacy/RoPA (#13 C) e snapshot/baseline E2E (#10 F).
**Escluso**: l'implementazione dei singoli cambi; la creazione degli use case (è `new-usecase`, [0045](0045-skill-new-usecase.md));
l'enforcement CI vero del gate privacy (è UC [0031](../08-compliance-gdpr/0031-gate-privacy-ropa-new-change.md)).

## 2. Attori & ruoli
- **Developer / Platform Engineer**: invoca la skill, scrive/approva i requisiti, dà consenso a commit e merge.
- **Skill `new-change`** (tooling): scaffolda il branch/folder, raccoglie i requisiti, implementa, verifica i test, si ferma ai gate.
- Nessun attore runtime (UC meta/tooling).

## 3. Precondizioni
- Repo `appgrove` clonato; `$DEFAULT_BRANCH` = `main`; working tree pulito.
- Esiste la cartella `changes/`; esistono i documenti decisionali `docs/01..14` e gli invarianti `CLAUDE.md` (fonte di verità).
- Se il cambio nasce da uno use case, esiste già il relativo `docs/usecases/<area>/YYYY-*.md`.

## 4. Flusso principale
1. **Init** (`step-01`): determina il prossimo `NNNN` a **4 cifre** scandendo `changes/` (regex `^[0-9]{3,4}-`, ma **pad a 4**,
   start `0001`); crea il branch `change/NNNN-brief` e il folder `changes/NNNN-brief/`. Se origina da uno use case YYYY,
   usa la forma `NNNN-use-case-YYYY-brief`.
2. **Requirements** (`step-02`): **clarification gate** — se qualcosa è ambiguo o ammette alternative, fa domande mirate;
   poi scrive `requirements.md` (Problem/Goal, Scope, Out of Scope, Acceptance Criteria, **Source use case**, Invarianti
   appgrove toccati, Test richiesti, Impact). Commit `chore(change/NNNN): write requirements`. **🛑 gate review** dei requisiti.
3. **Implement** (`step-03`): implementa nel rispetto degli invarianti; aggiunge/aggiorna i test richiesti (#10).
4. **Close** (`step-04`): esegue la **suite di ogni area toccata** (`mvn test` / `npm test` / `terraform fmt+validate`),
   scrive `implementation-log.md` (file cambiati, decisioni, invarianti, test + esito, acceptance status).
   **🛑 gate consenso commit**, poi **🛑 gate consenso merge** (branch lasciato non mergiato).

## 5. Flussi alternativi / edge / errori
- **Cambio non da use case**: campo "Source use case" = `None (ad-hoc change)`.
- **Cartelle a 3 cifre preesistenti**: tollerate in lettura (regex `{3,4}`), ma i nuovi folder sono sempre a 4 cifre.
- **Cambio senza codice eseguibile** (solo docs/skill/config): nessuna suite obbligatoria → annotato nel log.
- **Suite rossa**: si corregge **prima** del gate commit; mai committare con test rossi.
- **Hook privacy/RoPA (futuro, UC 0031)**: se il diff tocca dati personali (migrazioni, nuovi campi, nuove integrazioni
  esterne) → classificazione assistita + aggiornamento manifesto/RoPA + bump versione PP/ToS (major/minor → #13 C/G).
- **Hook snapshot/baseline (#10 F)**: regola "mai aggiornare una baseline alla cieca"; un fallimento visivo inatteso si **indaga**.

## 6. Risorse & runbook
**File della skill** (`.claude/skills/new-change/`): `SKILL.md`, `step-01-init.md`, `step-02-requirements.md`,
`step-03-implement.md`, `step-04-close.md`.
**Artefatti per invocazione**: branch `change/NNNN-…`, folder `changes/NNNN-…/` con `requirements.md` + `implementation-log.md`.
**Runbook**: `/new-change <descrizione>` → confermare requisiti al gate → implementare → consenso commit → consenso merge/PR.
**Stato attuale vs target**: numerazione 4 cifre + variante use-case + "Source use case" = **fatti**; gate privacy/RoPA e
gestione baseline da **encodare** (rimando a UC 0031 e alle regole #10 F).
**Rollback**: branch eliminabile prima del merge; nessuna infra toccata.

## 7. Dati toccati
Nessun dato applicativo/personale: solo file versionati in git (branch + `changes/NNNN-…`). Manifest GDPR #13 N/A.

## 8. Permessi & gate
- **Invarianti multi-tenancy**: N/A a runtime (tooling). La skill ne è però **garante**: ogni `requirements.md` e
  `implementation-log.md` annota come il cambio mantiene veri tenant_id-da-JWT, filtro row-level, modulo `microsaas_app`,
  logging strutturato.
- **Gate procedurali (vincolanti)**: review requisiti, consenso commit, consenso merge. (Futuri: gate privacy/RoPA bloccante in CI, UC 0031.)

## 9. Requisiti di test
Skill di tooling: verifica funzionale manuale, non test di prodotto. DEVE risultare: numerazione a 4 cifre corretta;
variante use-case ben formata; i gate bloccano davvero (nessun commit/merge senza OK); alla chiusura le suite delle aree
toccate girano e sono verdi. La copertura dei comportamenti enforced dalla skill (es. test cross-tenant #10 D) è
responsabilità dei cambi che la skill produce.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #07 (B/3 PR→CI, H gate prod), #10 (J gate di merge, F baseline), #13 C (gate privacy/RoPA), #13 G41 (versioning PP).
- **DoD**:
  1. `new-change` usa `NNNN` a 4 cifre + variante `NNNN-use-case-YYYY` + campo "Source use case".
  2. I tre gate (requisiti/commit/merge) sono presenti e bloccanti.
  3. La chiusura esegue le suite per area toccata e le richiede verdi.
  4. Gli hook privacy/RoPA e baseline sono **tracciati** come lavoro di UC 0031 / regole #10 (questo UC resta 🟡 finché non wired).
