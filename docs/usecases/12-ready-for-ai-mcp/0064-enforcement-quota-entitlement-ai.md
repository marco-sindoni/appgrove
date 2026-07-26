# UC 0064 — Enforcement entitlement/quota sulle chiamate AI

**Area**: 12-ready-for-ai-mcp · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0027 (enforcement entitlement/quota), UC 0026 (ciclo di vita dell'abbonamento), UC 0061 (architettura server MCP) — devono esistere prima: la catena di gate già usata dalle chiamate normali, lo stato dell'abbonamento da cui deriva l'entitlement, e la collocazione del server MCP che intercetta l'invocazione.
**Fonte**: R2 (Tabella dei residui, _INDEX.md) · docs/_BACKLOG.md §"Ready for AI"
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Garantire che **ogni invocazione arrivata via assistente AI passi per la stessa catena di gate** delle chiamate normali:
l'app deve essere entitled (l'abbonamento la include), l'utente delegante deve avere il ruolo adeguato, e la chiamata
**consuma quota** esattamente come una chiamata fatta dal backoffice. Nessuna scorciatoia: l'AI è solo un altro canale
verso le stesse operazioni. Scopo aggiuntivo: restituire all'assistente errori di limite (**pagamento richiesto** e
**troppe richieste**) in forma comprensibile, così che l'assistente sappia spiegarli all'utente. **Fuori scope**: consenso
(UC 0062), definizione degli strumenti (UC 0063), audit (UC 0065).

## 2. Attori & ruoli
- **Assistente AI**: invoca uno strumento e riceve il risultato o l'errore di limite.
- **Utente del tenant**: colui il cui abbonamento e la cui quota vengono verificati e consumati.
- **Motore di enforcement** (UC 0027): la catena entitlement → ruolo → quota, riusata senza duplicazione.
- **Ciclo di vita abbonamento** (UC 0026): fornisce lo stato (attivo, in prova, sospeso, scaduto) da cui deriva l'entitlement.

## 3. Precondizioni
- La catena di gate di UC 0027 è disponibile e riusabile come componente condiviso.
- L'invocazione porta un `tenant_id` verificato (dalla credenziale delegata di UC 0062, tramite il bordo di UC 0061).
- Lo stato dell'abbonamento è consultabile (UC 0026) per stabilire l'entitlement corrente.

## 4. Flusso principale
1. L'assistente invoca uno strumento; la chiamata entra nel server MCP con `tenant_id` verificato.
2. **Gate 1 — entitlement**: l'app dietro lo strumento è inclusa nell'abbonamento attivo del tenant? Se no → errore
   "pagamento richiesto" (l'operazione richiede un piano che includa questa app).
3. **Gate 2 — ruolo**: il ruolo dell'utente delegante consente questa operazione (lettura/scrittura)? Se no → negato.
4. **Gate 3 — quota**: la chiamata **consuma quota** come una chiamata normale — quota a consumo (flusso, es. numero di
   operazioni nel periodo) e/o quota a giacenza (stock, es. numero di entità create). Se il limite è superato → errore
   "troppe richieste"/limite raggiunto.
5. Superati i gate, l'operazione è servita con filtro `WHERE tenant_id = :tid` e il consumo di quota è registrato
   **una sola volta** per invocazione (nessun doppio conteggio col canale normale).
6. Il risultato (o l'errore di limite) torna all'assistente con un messaggio comprensibile e, dove utile, l'indicazione
   di *cosa fare* (es. "questo piano non include l'app fatture" oppure "hai esaurito le fatture del mese").

## 5. Flussi alternativi / edge / errori
- **Errore — pagamento richiesto (app non entitled o abbonamento non attivo)**: risposta strutturata che l'assistente
  traduce in linguaggio naturale ("Per farlo serve attivare/aggiornare il piano X"). Nessuna operazione eseguita.
- **Errore — limite di quota raggiunto**: risposta che distingue quota a consumo esaurita (aspetta il rinnovo/aumenta il
  piano) da giacenza piena (libera spazio/aumenta il piano); include, se disponibile, quando la quota si rinnova.
- **Edge — abbonamento in prova o in sospensione**: l'entitlement segue lo stato di UC 0026; in prova valgono i limiti del
  periodo di prova, in sospensione l'app non è entitled.
- **Edge — conteggio equo**: una singola richiesta in linguaggio naturale che l'assistente scompone in più invocazioni
  consuma quota **per invocazione effettiva**, come farebbe il canale normale; va evitato sia il doppio conteggio sia lo sconto.
- **Errore — quota calante durante uno strumento composito**: se un'operazione fa più passi, il superamento a metà va gestito
  in modo prevedibile (fallire presto, prima di effetti parziali irreversibili).

## 6. Risorse & runbook
- **Riuso, non duplicazione**: l'enforcement è lo **stesso** componente di UC 0027, invocato dal percorso MCP; non si
  riscrive una seconda catena di gate per l'AI (sarebbe un punto di divergenza pericoloso).
- **Mappatura errori**: tabella di corrispondenza tra gli esiti dei gate e i codici di errore MCP restituiti all'assistente,
  con messaggi in italiano orientati all'utente finale.
- **Runbook**: quando cambia il listino/quota (skill pricing-change), l'enforcement AI eredita automaticamente i nuovi limiti
  perché condivide il motore; nessun intervento separato sul canale AI.

## 7. Dati toccati
- **Contatori di quota** e **stato di entitlement** del tenant (già di UC 0026/0027): letti e, per la quota a consumo,
  incrementati. Non sono dati personali di per sé, ma sono `tenant`-scoped e vanno filtrati per tenant.
- **Punto chiave di minimizzazione**: gli errori di limite restituiti all'assistente comunicano *il fatto* (limite raggiunto,
  app non inclusa) senza esporre dettagli interni non necessari (es. non serve rivelare all'assistente l'intero stato di
  fatturazione del tenant per dire "quota esaurita").

## 8. Permessi & gate
- **Catena obbligatoria**: entitlement → ruolo → quota, identica alle chiamate normali, senza eccezioni per il canale AI.
- Tenant ID solo dal token verificato; filtro row-level `WHERE tenant_id = :tid` sull'operazione servita.
- Consumo di quota registrato una volta per invocazione effettiva; nessuno strumento aggira i gate.
- Postura fail-closed: in dubbio sull'entitlement o sulla quota, si nega.

## 9. Requisiti di test
- **App non entitled** → errore "pagamento richiesto"; **app entitled** → passa il gate.
- **Ruolo insufficiente** → negato; ruolo adeguato → ammesso.
- **Quota**: chiamata AI consuma quota come la chiamata normale (verifica del contatore); quota esaurita → errore di limite.
- **Nessun doppio conteggio**: una invocazione = un consumo, coerente col canale normale.
- **Parità di enforcement**: stessa decisione dei gate tra canale AI e canale normale a parità di stato.
- **Isolamento cross-tenant** sui contatori e sull'operazione.

## 10. Riferimenti & Definition of Done
- **Riferimenti**: UC 0027 (enforcement entitlement/quota), UC 0026 (ciclo di vita abbonamento), UC 0061 (architettura);
  file fratelli: 0062-auth-consenso-delegato-ai.md, 0063-mappatura-operazioni-strumenti-mcp.md, 0065-sicurezza-audit-invocazioni-ai.md.
- **DoD**: percorso MCP che riusa la catena di gate di UC 0027; consumo quota per invocazione senza doppio conteggio;
  mappatura errori "pagamento richiesto"/"limite raggiunto" verso l'assistente; test di parità di enforcement e isolamento verdi.

## Punti aperti / decisioni differite
- **L'intera epica 12 "Ready for AI (MCP)" è direzione di prodotto non ancora decisa**: collocazione del server (UC 0061),
  modello di consenso (UC 0062), formato strumenti (UC 0063) sono nodi da chiudere in sessioni dedicate. Proprietaria: **epica 12**.
- **Da valutare (non deciso)**: se le chiamate via AI debbano avere un limite dedicato aggiuntivo (per contenere raffiche
  automatiche) oltre alla quota di piano condivisa. È una decisione di prodotto/prezzi: tracciata qui, non forzata.
