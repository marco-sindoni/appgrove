# Change 0027: Gate privacy/RoPA in `new-change` (UC 0031)

**Branch**: `change/0027-use-case-0031-gate-privacy-ropa-new-change`
**Aree**: `.claude/skills/new-change` · `tools/compliance` · docs (use case, indice)
**Data**: 2026-07-03
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/08-compliance-gdpr/0031-gate-privacy-ropa-new-change.md](../../docs/usecases/08-compliance-gdpr/0031-gate-privacy-ropa-new-change.md)
**Tocca dati personali?**: No — la change agisce su processo/dichiarazioni (skill + tooling), non su dati utente.

## Problema / Obiettivo

Privacy-by-design (art. 25, #13 C16): ogni change che tocca dati personali va **intercettata, classificata e
applicata a manifesto/RoPA** contestualmente. Oggi `new-change` ha solo un **promemoria non vincolante**
(step-03); l'enforcement CI `@PersonalData`↔manifesto è già bloccante (UC 0030, change 0026). Questa change
consegna il resto di UC 0031: **rilevamento segnali deterministico** sul diff, **co-pilota di classificazione**
nella skill, **escalation art. 9/DPIA**, classificazione **MAJOR/MINOR** che pilota il bump PP/ToS, e
**segnalazione sub-processor**.

## Scope

1. **Scanner deterministico** `tools/compliance/privacy-scan.mjs` (decisione dialogo: opzione B):
   - input: range git (default `main...HEAD`), analizza il diff del repo;
   - segnali rilevati (#13 C16): **(a)** migrazioni Flyway nuove/modificate con `CREATE TABLE` / `ADD COLUMN`;
     **(b)** nuovi campi in entità JPA / DTO Java (righe aggiunte con dichiarazioni di campo in
     `src/main/java`); **(c)** nuove dipendenze in `pom.xml` / `package.json` e nuovi host esterni in config
     (potenziale **sub-processor**); **(d)** modifiche ai manifesti `docs/compliance/manifests/*.yaml` su
     `retention`/`purpose`/`legal_basis`;
   - output: report leggibile + `--json`; exit code 0 = nessun segnale, 1 = segnali (informativo, non build-gate);
   - `npm run privacy-scan` in `tools/compliance`; test `node:test` su diff fixture (auto-inclusi in
     `./run-tests.sh compliance` via `node --test`).
2. **Skill `new-change` — da promemoria a gate**:
   - **step-03**: il "Privacy/RoPA checkpoint" diventa il **gate co-pilota**: esegue lo scanner; se segnali →
     classificazione assistita (elicita lo scopo, **deduce e propone** natura/finalità/base/retention **con
     motivazione**, domande solo se ambiguo, **conferma esplicita**) → aggiorna manifesto YAML + rigenera RoPA;
     **escalation art. 9** (avviso forte + screening DPIA coi criteri art. 35/EDPB, #13 K67) + guardrail
     **pseudonimizzazione ≠ erasure** (#13 L72);
   - **MAJOR/MINOR** (decisione dialogo: opzione A): cambio **materiale** (finalità/basi/categorie/retention) →
     major → re-accept scoped; altrimenti minor → notifica (#13 G41, #14 C18). Finché `content/legal/` non
     esiste (UC 0002 ⬜) la classificazione si **registra negli artefatti della change** (campo dedicato in
     requirements/implementation-log); quando esisterà, il gate bumppa il front-matter del componente;
   - **sub-processor** (decisione dialogo: opzione A): segnale integrazione esterna → "potenziale nuovo
     sub-processor"; aggiorna `content/subprocessors.md` **se esiste** (path canonico dec. 46 — corretto il
     riferimento errato `content/legal/subprocessors.*.md` in step-03) + preavviso 30gg (#13 C49); finché non
     esiste → registrazione negli artefatti;
   - **step-04**: verifica che il gate sia stato eseguito (scan obbligatorio prima del commit-consent, esito
     annotato nell'implementation-log);
   - **step-02**: il campo "Tocca dati personali?" rimanda al gate (classificazione MAJOR/MINOR inclusa).
3. **Tracciamento residui (non si perdono)**:
   - **UC 0002** "Punti aperti": al primo rilascio dei legali, riportare nel front-matter le classificazioni
     major/minor accumulate nelle change + creare `content/subprocessors.md` seminato con dec. 45 (AWS;
     Plausible quando attivo; Paddle escluso = titolare autonomo) + segnalazioni accumulate;
   - **UC 0056** "Punti aperti": canale di **notifica** minor / preavviso 30gg sub-processor (il gate oggi solo
     classifica e registra);
   - `docs/usecases/_INDEX.md`: 0031 → ✅ a chiusura.

## Fuori scope

- Testi legali e sito (UC 0002/0055); re-accept runtime e notifiche (UC 0056); manifesti/RoPA e verifier CI
  `@PersonalData` (già UC 0030). Nessun nuovo check **bloccante** in CI: lo scanner è strumento del gate di
  skill, il blocco build resta il verifier di UC 0030.

## Criteri di accettazione

- [ ] Scanner: su fixture rileva i 4 tipi di segnale (migrazione/campo entità/dipendenza esterna/manifesto) e
      su diff pulito esce 0 senza segnali; output `--json` stabile.
- [ ] `./run-tests.sh compliance` verde, test scanner inclusi automaticamente.
- [ ] Skill aggiornata (step-02/03/04): gate co-pilota con scanner, art. 9/DPIA, MAJOR/MINOR, sub-processor con
      path canonico dec. 46.
- [ ] Punti differiti scritti in UC 0002 e UC 0056 (sezione "Punti aperti / decisioni differite").

## Invarianti appgrove toccati

Nessuno direttamente (change su skill/tooling/docs, nessun codice runtime): tenant/JWT, row-level filter,
modulo Terraform e logging strutturato restano invariati. Il gate **rafforza** l'accountability (art. 5.2).

## Requisiti di test

- Fixture diff (git repo temporaneo o diff sintetici) per: migrazione Flyway con `ADD COLUMN`; nuovo campo in
  `@Entity`; nuova dipendenza in `pom.xml`; edit a `retention` in un manifesto; diff senza segnali.
- Nessun test per la parte skill (solo Markdown): motivazione nell'implementation-log.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A |
| Version bump | nessuno |
