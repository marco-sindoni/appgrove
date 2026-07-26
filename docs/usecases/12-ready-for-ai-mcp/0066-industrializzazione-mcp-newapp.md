# UC 0066 — Industrializzazione MCP in new-application/microsaas_app + riconciliazione claim sito

**Area**: 12-ready-for-ai-mcp · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0046 (skill new-application), UC 0004 (modulo Terraform microsaas_app), UC 0037 (sito vetrina / contenuti landing), e gli altri use case dell'epica: UC 0061 (architettura), UC 0062 (consenso), UC 0063 (contratto strumenti), UC 0064 (enforcement), UC 0065 (sicurezza/audit) — devono esistere prima: il generatore di nuove app e il modulo infrastrutturale (per rendere AI-ready di default), i contenuti del sito da riconciliare, e le decisioni delle altre cinque storie che questa industrializza.
**Fonte**: R2 (Tabella dei residui, _INDEX.md) · docs/_BACKLOG.md §"Ready for AI"
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Rendere la capacità MCP **una proprietà di default di ogni app**, non un'aggiunta artigianale app per app: quando la
skill `new-application` (UC 0046) scaffolda un nuovo servizio e il modulo `microsaas_app` (UC 0004) crea l'infrastruttura,
l'app nasce già "AI-ready" (contratto strumenti minimo, endpoint MCP scoperto in locale, gate e audit agganciati). Secondo
obiettivo, strettamente legato: **riconciliare i contenuti del sito vetrina** (UC 0037) con lo stato reale della capacità,
passando — quando MCP atterra davvero — da "progettato per l'AI" a "richiamabile dalla tua AI", con prova. **Fuori scope**:
le decisioni di merito delle storie 0061–0065, che questo use case presuppone chiuse.

## 2. Attori & ruoli
- **Skill new-application** (UC 0046): scaffolda ogni nuova app con la capacità MCP inclusa.
- **Modulo microsaas_app** (UC 0004): istanzia l'infrastruttura AI-ready (endpoint, rotte al bordo) senza infra su misura.
- **Sviluppatore/Platform Engineer**: usa gli strumenti sopra; verifica l'avviabilità locale.
- **Chi cura il sito vetrina** (UC 0037): allinea i contenuti al claim reale prima del go-live.

## 3. Precondizioni
- Le decisioni delle storie 0061–0065 sono chiuse (topologia, consenso, formato contratto, enforcement, audit/privacy):
  senza di esse non si può industrializzare un modello ancora indeciso.
- La skill `new-application` e il modulo `microsaas_app` esistono e sono la via unica per creare app/infrastruttura (invarianti).
- Il sito vetrina (UC 0037) anticipa già il concetto MCP come *principio di design/visione* (deciso nella change 0047),
  **senza** dichiararlo attivo su ogni app.
- Esiste il registro delle deviazioni consapevoli di parità dello scaffolding (docs/_PARITA-SCAFFOLD.md).

## 4. Flusso principale
1. Si aggiornano i modelli-sorgente di `new-application` (derivati dall'app #1 "fatture") perché ogni nuova app generi:
   un **contratto strumenti minimo** (UC 0063), l'**endpoint MCP** derivabile dalle proprietà di scoperta locale (UC 0061),
   e l'aggancio a gate (UC 0064) e audit (UC 0065) senza righe incollate a mano.
2. Si estende il modulo `microsaas_app` (UC 0004) perché l'infrastruttura AI-ready nasca dall'istanziazione del modulo
   (endpoint, rotta al bordo), coerente con l'invariante "nuova app = istanziare il modulo, non infra bespoke".
3. Si verifica che una app appena scaffoldata sia **avviabile in locale** con l'endpoint MCP scoperto automaticamente
   (`./dev.sh services` mostra la mappa), come richiesto dal Definition of Done sull'avvio locale.
4. Si allinea `run-tests.sh` perché la verifica della capacità MCP delle app rientri nella suite (area interessata).
5. Quando la capacità MCP è **realmente attiva** su una o più app, si **riconcilia il sito vetrina** (UC 0037):
   homepage e pagina "Perché appgrove" passano da "progettato per l'AI" a "richiamabile dalla tua AI", con prova concreta
   (es. elenco delle app effettivamente esposte). Finché non è attiva, il claim resta *visione/principio di design*.
6. Si aggiorna il registro di parità dello scaffolding (docs/_PARITA-SCAFFOLD.md) se un modello resta indietro di proposito.

## 5. Flussi alternativi / edge / errori
- **Edge — capacità non ancora attiva**: il sito **non** deve dichiarare MCP come già attivo su ogni app (onestà del claim,
  requisito anche per la revisione di dominio del fornitore di pagamenti Paddle); resta l'inquadramento a "principio di design".
- **Edge — attivazione parziale**: se MCP è attivo solo su alcune app, il claim del sito va calibrato su *quelle* app (prova
  puntuale), non generalizzato.
- **Errore — modelli-sorgente indietro rispetto all'app #1**: se "fatture" evolve la sua capacità MCP e i modelli non
  seguono, ogni app nuova nasce antiquata senza che nulla diventi rosso → va tracciato in docs/_PARITA-SCAFFOLD.md e
  intercettato dal collaudo di parità.
- **Edge — app che non vuole esporre strumenti**: deve poter nascere senza strumenti dichiarati (nessuna esposizione
  forzata), pur restando l'ossatura AI-ready pronta.

## 6. Risorse & runbook
- **Modelli-sorgente new-application** (UC 0046): aggiunta del contratto strumenti minimo e dell'aggancio MCP; la skill
  legge docs/_PARITA-SCAFFOLD.md prima di generare.
- **Modulo microsaas_app** (UC 0004): estensione per l'infrastruttura AI-ready.
- **Sito vetrina** (UC 0037): testi di homepage e "Perché appgrove"; la riconciliazione è un intervento sui contenuti, non
  sul motore MCP, ed è governata dal ciclo di pubblicazione del sito.
- **Runbook riconciliazione claim**: checklist "MCP attivo su app X → aggiorna il claim del sito con prova; MCP non attivo →
  mantieni l'inquadramento a visione". Da eseguire come **gate pre-go-live** sul messaggio del sito.

## 7. Dati toccati
Questo use case è di industrializzazione e di contenuti: non introduce trattamenti di dati personali propri. Eredita la
postura privacy delle storie che industrializza — in particolare la **minimizzazione verso l'assistente** (UC 0065) e la
coerenza col registro dei trattamenti (UC 0030), che ogni nuova app scaffoldata deve rispettare per costruzione. I contenuti
del sito vetrina non trattano dati personali dei tenant.

## 8. Permessi & gate
- Ogni app scaffoldata eredita gli invarianti: **tenant_id solo dal token verificato**, **filtro row-level**
  `WHERE tenant_id = :tid`, logging strutturato con `tenant_id`, `app_id`, `user_id`.
- La capacità MCP di default non introduce scorciatoie ai gate: entitlement/ruolo/quota (UC 0064) e consenso (UC 0062)
  restano attivi per costruzione su ogni app generata.
- **Gate di onestà del claim**: il sito non può dichiarare attiva una capacità che non lo è (presidio pre-go-live).

## 9. Requisiti di test
- **Scaffolding AI-ready**: una app appena generata da `new-application` include contratto strumenti minimo, endpoint MCP
  scoperto in locale, gate e audit agganciati.
- **Avviabilità locale**: la app scaffoldata parte in locale con l'endpoint MCP visibile nella mappa dei servizi.
- **Parità scaffolding**: il collaudo di parità (tools/scaffold-parity) non segnala derive non registrate tra "fatture" e i modelli.
- **run-tests.sh**: la verifica MCP rientra nella suite dell'area toccata ed è verde.
- **Riconciliazione claim**: verifica che il testo del sito rifletta lo stato reale (attivo con prova vs visione).

## 10. Riferimenti & Definition of Done
- **Riferimenti**: UC 0046 (new-application), UC 0004 (modulo microsaas_app), UC 0037 (sito vetrina), change 0047 (anticipo
  marketing MCP), docs/_PARITA-SCAFFOLD.md, run-tests.sh; file fratelli: 0061-architettura-server-mcp.md,
  0062-auth-consenso-delegato-ai.md, 0063-mappatura-operazioni-strumenti-mcp.md, 0064-enforcement-quota-entitlement-ai.md,
  0065-sicurezza-audit-invocazioni-ai.md.
- **DoD**: `new-application` e `microsaas_app` producono app AI-ready di default; app scaffoldata avviabile in locale con
  endpoint MCP scoperto; parità dello scaffolding verde; `run-tests.sh` aggiornato; runbook di riconciliazione del claim del
  sito definito ed eseguibile come gate pre-go-live.

## Punti aperti / decisioni differite
- **L'intera epica 12 "Ready for AI (MCP)" è direzione di prodotto non ancora decisa**: questa industrializzazione **dipende**
  dalla chiusura dei nodi delle storie 0061–0065 (collocazione del server, modello di consenso, formato del contratto
  strumenti). Non va avviata finché quei nodi non sono decisi in sessioni dedicate. Proprietaria: **epica 12**.
- **Riconciliazione del claim del sito**: da eseguire **prima del go-live** e ripetere a ogni attivazione di MCP su nuove
  app; finché la capacità non è attiva, il claim resta inquadrato come visione/principio di design (onestà del claim,
  requisito anche per la revisione di dominio del fornitore di pagamenti).
