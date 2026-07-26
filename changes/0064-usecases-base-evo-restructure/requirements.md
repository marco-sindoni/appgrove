# Change 0064 — Riorganizzazione catalogo use case: epiche evolutive (evo) in-place

**Tipo**: change normale (non implementa uno use case numerato — riorganizza il catalogo stesso)
**Branch**: `change/0064-usecases-base-evo-restructure`
**Modalità**: autopilot (i tre gate restano dello sviluppatore)
**Aree**: solo documentazione (`docs/usecases/**` + indici). Nessun codice eseguibile → suite di test non applicabili.

## 1. Obiettivo

Formalizzare il **lavoro residuo / evolutivo** oggi sparso nel backlog ([docs/_BACKLOG.md](../../docs/_BACKLOG.md))
e nella *Tabella dei residui* di [docs/usecases/_INDEX.md](../../docs/usecases/_INDEX.md) (R1–R21) in un insieme di
**storie use case numerate e descritte nel dettaglio**, raggruppate per **epica**, così che ognuna sia poi
implementabile con una `new-change` come qualsiasi altro use case.

## 2. Approccio (requisito rivisto dallo sviluppatore)

Rispetto all'invocazione iniziale, lo sviluppatore ha **rivisto il requisito** (vedi `decisions.json` #4/#5):

1. **Niente spostamenti.** NON si crea la cartella `base-implementation`, NON si crea la cartella `evo`.
   Le cartelle-area esistenti `01`–`11` **restano dov'è** in `docs/usecases/`. Conseguenza voluta: i **278
   riferimenti** `usecases/0N-…` sparsi nel repo (molti dentro `changes/*/` storici) **non si rompono** e non
   vanno toccati.
2. **Epiche = nuove cartelle-area numerate.** Le epiche sono nuove cartelle `NN-slug/` create **direttamente
   sotto** `docs/usecases/`, accanto a 01–11, **proseguendo la numerazione delle aree da 12**. Estende la
   convenzione esistente ("cartella-area numerata + use case a 4 cifre globali"), non introduce un albero parallelo.
3. **Storie = numerazione globale continua da 0061.** Ogni storia è un file `NNNN-slug.md`; la numerazione
   assoluta a 4 cifre **continua dal primo libero (0061)** — gli use case esistenti arrivano a 0060. Rispetta
   l'invariante CLAUDE.md "numerazione assoluta, globale/unica" (niente collisioni).
4. **Etichetta "evo".** Le cartelle 12+ sono documentate come **epiche evolutive (evo)**: lavoro post-base,
   non ancora schedulato nell'ordine topologico. La distinzione base ↔ evo vive nella numerazione (01–11 = base,
   12+ = evo), senza bisogno di una cartella contenitore.

## 3. Perimetro del backlog raccolto

**Incluso** — tutto il lavoro **aperto** del backlog: la Tabella dei residui R1–R21 (escluse le voci già fatte),
i tre temi **GRANDE** (Ready-for-AI/MCP, modello utenti B2B/B2C, — l'accoppiamento app↔core è già risolto), e le
leve di business pagamenti ancora aperte (bundling, riconciliazione netto, trial una-tantum).

**Escluso** (già fatto — non diventa storia): R9 runbook/registro/template breach e skill `breach-response`
(change 0063); skill `campaign-guide` (change 0053); accoppiamento sincrono app↔core (risolto change 0041);
Console "Diritti GDPR" (UC 0034) e self-service GDPR (UC 0033). I punti aperti fini della console admin
(UC 0021 #11/#16/#17) restano tracciati nel loro UC e si promuovono quando maturano (non se ne fabbricano storie ora).

## 4. Epiche e storie (mappa proposta)

8 epiche → cartelle `12`–`19`; 29 storie → `0061`–`0089`. Ogni storia sarà scritta con il template
[_TEMPLATE.md](../../docs/usecases/_TEMPLATE.md) (obiettivo, attori, precondizioni, flusso, edge, dati/GDPR,
permessi/gate, test, DoD), con stato **🟢 scritto / ⬜ da implementare** e le dipendenze verso gli UC base.

### Epica 12 — `12-ready-for-ai-mcp` — Ready for AI (MCP)  · fonte: R2 (GRANDE 🤖)
- **0061** — Architettura & collocazione del server MCP (per-app in `services/<app>` vs gateway centrale)
- **0062** — Autenticazione e consenso delegato dall'assistente AI verso il tenant
- **0063** — Mappatura operazioni app → strumenti MCP (contratto per-app)
- **0064** — Enforcement entitlement/quota sulle chiamate AI (riuso gate UC 0027)
- **0065** — Sicurezza & audit delle invocazioni AI + postura privacy (niente dati personali oltre il necessario)
- **0066** — Industrializzazione MCP in `new-application`/`microsaas_app` + riconciliazione claim marketing del sito

### Epica 13 — `13-abbonamenti-self-service` — Abbonamenti self-service & leve billing · fonte: R4, R5, R21, K50, K51
- **0067** — Gestione abbonamento self-service (backoffice "Abbonamenti": stato/tier, upgrade/downgrade con gate flow/stock, disdici/riattiva, uso quota, portale Paddle) — R4
- **0068** — Pausa/ripresa subscription self-service (Paddle pause/resume) — R5
- **0069** — Trial una-tantum per tenant×app (storico "prova consumata" + gate backend + stato in UI) — R21
- **0070** — Bundling: più app in un unico abbonamento — K50
- **0071** — Riconciliazione netto/revenue (osservabilità del netto incassato) — K51

### Epica 14 — `14-modello-utenti-multiapp` — Modello utenti multi-app (B2B/B2C) · fonte: R3 (GRANDE 🏛️)
- **0072** — Distinzione B2C/B2B a livello app (`App.user_model`) + semantica gestione utenti
- **0073** — Invito utenti per-app con "posti" come metrica quota `stock` (pricing posti per-app)
- **0074** — Directory cross-app + UI "Membri" ripensata per-app

### Epica 15 — `15-supporto-e-piattaforma` — Supporto & piattaforma · fonte: R6, R7, R11
- **0075** — Ticketing nativo in-house (`support_ticket` + UI backoffice + console admin + trigger auto + SES)
- **0076** — Disabilita applicazione (feature admin reversibile, non tocca dati/infra)
- **0077** — Provider entitlement reale del backoffice/admin (sostituire `StubEntitlementsProvider`)

### Epica 16 — `16-messa-in-cloud-golive` — Messa in cloud & go-live operativo · fonte: R12–R16, R18, R10
- **0078** — Uscita di SES dalla modalità di prova (sandbox) — richiesta AWS, bloccante go-live
- **0079** — Gestione rimbalzi/reclami SES (notifiche, lista soppressione, allarme sul tasso)
- **0080** — Prima esecuzione live della pipeline + configurazione repo GitHub
- **0081** — Smoke reali cloud alla prima accensione di `test`
- **0082** — Script di attivazione ambienti per fasi (`test-start`/`test-stop` + cron)
- **0083** — Correzione drift regione `eu-south-1`→`eu-west-1` + provisioning casella `security@`

### Epica 17 — `17-skill-e-tooling-contenuto` — Skill & tooling di contenuto/manutenzione · fonte: R1, R17
- **0084** — Skill `new-blog-post` (scaffold 5 file-lingua + registro + agganci pilastro↔cluster + copy on-brand)
- **0085** — Unificazione in `services/commons` dei due renderer Java dei template email

### Epica 18 — `18-brand-e-design-system` — Brand & design system condiviso · fonte: R20
- **0086** — Pacchetto brand kit / token condiviso nel monorepo (fonte unica SPA + vetrina + landing)
- **0087** — Artwork logo finale + stile illustrazioni on-brand

### Epica 19 — `19-debito-tecnico` — Debito tecnico & feature deprioritizzate · fonte: R8, R19, UC 0021 #18
- **0088** — Search globale dal workspace del backoffice
- **0089** — Rimozione `legacy-peer-deps` nel frontend (quando l'ecosistema aggiorna i peer a TypeScript 6)

## 5. Aggiornamento degli indici

- **[docs/usecases/README.md](../../docs/usecases/README.md)** (catalogo per area): aggiungere le sezioni delle
  epiche 12–19 con le rispettive storie; nota che 12+ sono **epiche evolutive (evo)**, distinte dalle aree base 01–11.
- **[docs/usecases/_INDEX.md](../../docs/usecases/_INDEX.md)** (ordine esecutivo): la **tabella di esecuzione**
  topologica (righe 1–60) NON cambia (le storie evo non sono ancora schedulate). La **Tabella dei residui** viene
  aggiornata: ogni residuo promosso riporta il suo **nuovo numero UC** ed è marcato come "formalizzato in epica NN".
  Le storie evo NON entrano ancora nell'ordine topologico (si schedulano quando maturano — regola già scritta in _INDEX.md).
- Nessuna modifica a `_TEMPLATE.md`.

## 6. Requisiti di test

Change **solo-documentazione**: le suite backend/frontend/infra non si applicano. Verifica sostitutiva (step-04):
- validità di `decisions.json` (JSON.parse);
- coerenza dei **link relativi** nei nuovi file e negli indici aggiornati (nessun percorso rotto introdotto);
- parità di numerazione (nessun buco/duplicato tra 0061 e l'ultimo assegnato; cartelle 12–19 contigue).

## 7. Definition of Done

1. Create le cartelle `12`–`19` sotto `docs/usecases/` (nessuno spostamento di 01–11).
2. Scritte in dettaglio le storie `0061`–`0089` (template di casa, dipendenze verso UC base, GDPR dove pertinente).
3. `README.md` e `_INDEX.md` aggiornati (catalogo + residui → promossi); link coerenti.
4. `decisions.json` completo e coerente con l'implementation-log; nessun link `usecases/0N-…` esistente rotto.
5. Lavoro deliberatamente non fatto (es. schedulazione topologica delle evo) tracciato come rimando.
