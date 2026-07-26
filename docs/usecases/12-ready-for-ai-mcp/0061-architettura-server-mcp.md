# UC 0061 — Architettura & collocazione del server MCP

**Area**: 12-ready-for-ai-mcp · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0004 (modulo Terraform microsaas_app), UC 0055 (risorse condivise per-ambiente), UC 0051 (app #1 "fatture"), UC 0014 (authorizer al bordo / API Gateway) — devono esistere prima: il modulo che scaffolda ogni app, l'infrastruttura condivisa dove agganciare un eventuale gateway, almeno un'app reale da esporre, e il punto di verifica del token al bordo.
**Fonte**: R2 (Tabella dei residui, _INDEX.md) · docs/_BACKLOG.md §"Ready for AI"
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Decidere **dove vive e come è fatto** il server che espone le app del marketplace come strumenti richiamabili
dagli assistenti AI tramite MCP (Model Context Protocol — lo standard aperto con cui un assistente AI, ad esempio
Claude, ChatGPT o Perplexity, invoca strumenti esterni). Lo scope è puramente architetturale: collocazione
(per-app dentro `services/<app>` oppure gateway centrale condiviso), trasporto, multi-tenancy del server, aggancio
al modulo `microsaas_app` e al bordo. **Fuori scope**: il modello di consenso (UC 0062), il contratto degli strumenti
(UC 0063), l'enforcement di quota (UC 0064), audit e privacy (UC 0065), l'industrializzazione (UC 0066).

## 2. Attori & ruoli
- **Assistente AI** (client MCP): software terzo che, per conto dell'utente, apre una sessione MCP e invoca strumenti.
- **Utente del tenant** (owner/admin/member): la persona che dalla chat del proprio assistente chiede un'operazione.
- **Piattaforma appgrove**: il server MCP e la catena di verifica al bordo.
- **Platform Engineer**: decide la topologia infrastrutturale e la istanzia via Terraform.

## 3. Precondizioni
- Esiste almeno un'app reale (UC 0051) con operazioni candidate a diventare strumenti.
- Il modulo `microsaas_app` (UC 0004) è la via unica per creare infrastruttura di un'app (invariante).
- Il bordo verifica già il token e inietta il tenant (UC 0014, UC 0016): il server MCP **non** reinventa l'autenticazione.
- Le risorse condivise per-ambiente (UC 0055) offrono un punto dove agganciare eventuale infrastruttura comune.

## 4. Flusso principale
1. L'assistente AI apre una sessione MCP verso l'endpoint pubblicato dalla piattaforma (trasporto su HTTP con eventi
   in streaming — cioè risposte inviate a pezzi mentre il server elabora; il vecchio trasporto solo-standard-input/output
   resta per l'uso locale di sviluppo, non per la produzione).
2. La sessione presenta le credenziali delegate ottenute nel flusso di consenso (UC 0062); il bordo le verifica e
   ricava il `tenant_id` dal token verificato — **mai** da parametri della chiamata MCP (invariante architetturale).
3. Il server MCP espone la lista degli strumenti disponibili per quel tenant (contratto per-app, UC 0063).
4. L'assistente invoca uno strumento; la chiamata attraversa la catena di gate (UC 0064) e viene servita dal codice
   dell'app corrispondente, con filtro `WHERE tenant_id = :tid` su ogni query.
5. Il risultato torna all'assistente in forma minimizzata (UC 0065); ogni passo è loggato in modo strutturato
   con `tenant_id`, `app_id`, `user_id`.

## 5. Flussi alternativi / edge / errori
- **Decisione di topologia (nodo aperto)**: *per-app* — ogni `services/<app>` espone il proprio endpoint MCP; pro:
  coesione col dominio, deploy indipendente, zero salti di rete extra; contro: N endpoint da scoprire, logica di
  sessione/consenso duplicata. *Gateway centrale* — un solo servizio condiviso instrada verso le app; pro: un unico
  punto di scoperta e di policy, superficie di consenso unica; contro: nuovo componente stateful multi-tenant, punto
  singolo, accoppiamento. La raccomandazione tecnica di partenza è **ibrida**: contratto e implementazione degli
  strumenti *per-app*, un sottile strato di scoperta/instradamento al bordo — ma è **direzione di prodotto non ancora
  decisa** (vedi Punti aperti).
- **Errore — token assente o non verificabile**: sessione rifiutata *fail-closed* (in caso di dubbio si nega),
  risposta di errore MCP comprensibile all'assistente, nessuno strumento elencato.
- **Edge — app non ancora AI-ready**: se un'app non dichiara strumenti, semplicemente non compare nell'elenco; nessun
  errore, nessuna esposizione implicita.
- **Edge — più tenant sullo stesso assistente**: ogni sessione porta un solo `tenant_id`; sessioni diverse restano isolate.

## 6. Risorse & runbook
- **Trasporto**: HTTP con streaming di eventi per la produzione; standard-input/output solo per lo sviluppo locale.
- **Infrastruttura**: qualunque risorsa nuova (endpoint, rotta al bordo) nasce **istanziando il modulo `microsaas_app`**
  o agganciandosi alle risorse condivise (UC 0055), mai come infrastruttura su misura (invariante).
- **Scoperta locale**: l'endpoint MCP di un'app deve derivare dalle stesse proprietà `application.properties` che già
  guidano la scoperta automatica dei servizi, così che l'app resti avviabile in locale senza cablaggi a mano.
- **Runbook**: pubblicazione/aggiornamento dell'endpoint via pipeline standard; rollback = ritiro dell'endpoint dalla
  scoperta, che rimuove l'app dagli strumenti elencati senza toccare l'app stessa.

## 7. Dati toccati
Questo use case non introduce trattamenti nuovi: instrada verso operazioni già esistenti. Il **punto chiave** è che il
server MCP è un *canale*, non un deposito: non deve persistere payload delle chiamate oltre l'audit minimo (UC 0065),
e non deve rendere l'assistente AI capace di leggere più dati di quanti l'operazione richieda. La minimizzazione verso
l'assistente è responsabilità del contratto strumenti (UC 0063) e della postura privacy (UC 0065).

## 8. Permessi & gate
- **Tenant ID solo dal token verificato** al bordo; mai dai parametri della chiamata MCP.
- **Filtro row-level** `WHERE tenant_id = :tid` su ogni query servita dietro uno strumento.
- La catena entitlement → ruolo → quota resta identica a quella delle chiamate normali (delega a UC 0064).
- Postura *fail-closed*: nessuno strumento è esposto senza tenant verificato ed entitlement valido.

## 9. Requisiti di test
- **Isolamento cross-tenant**: una sessione con tenant A non deve mai vedere né toccare dati del tenant B (test di sicurezza).
- **Verifica al bordo**: sessione senza token valido → rifiutata; con token valido → elenco strumenti corretto per il tenant.
- **Trasporto**: apertura sessione, elenco strumenti, invocazione e streaming del risultato in ambiente di test.
- **Scoperta locale**: l'endpoint MCP dell'app di riferimento risulta avviabile in locale via scoperta automatica.

## 10. Riferimenti & Definition of Done
- **Riferimenti**: UC 0004 (modulo microsaas_app), UC 0014 (authorizer al bordo), UC 0055 (risorse condivise), UC 0051 (app #1);
  file fratelli di questa epica: 0062-auth-consenso-delegato-ai.md, 0063-mappatura-operazioni-strumenti-mcp.md,
  0064-enforcement-quota-entitlement-ai.md, 0065-sicurezza-audit-invocazioni-ai.md, 0066-industrializzazione-mcp-newapp.md.
- **DoD**: decisione di topologia formalizzata (o consapevolmente rimandata con opzione di partenza documentata); trasporto
  scelto; aggancio al modulo `microsaas_app` descritto; invarianti multi-tenancy rispettate; test di isolamento verdi.

## Punti aperti / decisioni differite
- **L'intera epica 12 "Ready for AI (MCP)" è direzione di prodotto non ancora decisa.** I nodi progettuali — collocazione
  del server (per-app vs gateway), modello di consenso (UC 0062), formato del contratto strumenti (UC 0063) — vanno chiusi
  in **sessioni dedicate** prima di aprire una change di implementazione. Proprietaria dei nodi: **epica 12**.
- La scelta di topologia qui è espressa come *raccomandazione di partenza* (ibrida), non come decisione presa.
