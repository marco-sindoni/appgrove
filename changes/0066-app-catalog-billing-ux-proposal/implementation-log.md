# Implementation Log — Change 0066: Proposta UX "App catalog vs Billing"

**Branch**: `change/0066-app-catalog-billing-ux-proposal`
**Aree**: documentazione (`changes/`, `docs/usecases/`, `CLAUDE.md`) — nessun codice eseguibile
**Completata**: 2026-08-01
**Modalità**: autopilot — risposte alle domande di approfondimento dell'agente, tracciate in
[decisions.json](decisions.json) (13 decisioni, 10 in autopilot); i gate umani rispettati: approvazione requisiti,
**approvazione esplicita dell'artefatto prima delle user story** (gate aggiuntivo richiesto), consenso a commit e
merge allo sviluppatore.

## File prodotti/modificati

| File | Azione |
|---|---|
| `changes/0066-*/proposta-ux.template.html` | Creato — sorgente leggibile del mockup (segnaposto font) |
| `changes/0066-*/proposta-ux.html` | Creato — mockup navigabile autonomo (162 KB, font di brand incorporati), pubblicato come Artifact |
| `docs/usecases/21-catalogo-app-backoffice/0095-pagina-app-catalog.md` | Creato — story catalogo |
| `docs/usecases/21-catalogo-app-backoffice/0096-billing-solo-fatturazione.md` | Creato — story Billing |
| `docs/usecases/21-catalogo-app-backoffice/0097-dashboard-operativa.md` | Creato — story Dashboard/Account |
| `docs/usecases/README.md` | Sezione epica 21 |
| `docs/usecases/EPICS-WAVE-2.md` | Blocco C3 (righe 21–23), rinumerazione, vincoli epica 21, conteggi 37 storie |
| `CLAUDE.md` | Conteggi catalogo: 97 use case, epiche 12–21 |
| `docs/usecases/07-payments/0024-checkout.md` | Punto aperto "vetrina real-catalog" → assorbito da UC 0095 |
| `docs/usecases/15-supporto-e-piattaforma/0076-disabilita-applicazione.md` | Tracciato il bug Billing/app disabilitata (osservato in sessione, fuori scope) |

## Cosa è stato fatto

1. **Analisi dell'esistente**: Dashboard = solo UUID; Billing intitolata "Get an app" con catalogo+abbonamenti
   mischiati; incoerenza dell'app disabilitata (sidebar corretta via `/me/entitlements`, Billing muta via
   `/me/subscriptions`) — bug tracciato in UC 0076, fuori scope.
2. **Artefatto navigabile** fedele al design system reale (token UC 0019, Plus Jakarta Sans/JetBrains Mono
   incorporati, icone SVG inline): quattro viste (Dashboard operativa, App catalog con ricerca/paginazione/6 stati,
   Billing solo-fatturazione con storico pagamenti, Account con Workspace ID), note di proposta in italiano
   attivabili, tema chiaro/scuro e accent funzionanti. **Approvato dallo sviluppatore.**
3. **User story** come use case 0095–0097 nella nuova epica evo 21, con dipendenze, riferimento visivo vincolante
   all'artefatto, requisiti di test (inclusi i journey di piattaforma dell'epica 20) e registrazioni in tutti gli
   indici.

## Test

Non applicabili: la change produce solo documentazione e un mockup HTML statico; nessun codice eseguibile del
monorepo è toccato (nessuna area di `run-tests.sh` coinvolta).

## Rimandi tracciati

- Fix Billing/app disabilitata → UC 0076 (punti aperti); sarà chiuso da UC 0096.
- Vetrina dal catalogo reale (ex punto aperto UC 0024) → UC 0095.
- Fonte delle descrizioni localizzate del catalogo e persistenza dello storico pagamenti → decisioni delle change
  implementative di UC 0095/0096 (dichiarate nei rispettivi drill-down).
