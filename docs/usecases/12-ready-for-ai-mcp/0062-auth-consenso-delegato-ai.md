# UC 0062 — Autenticazione e consenso delegato dall'assistente AI verso il tenant

**Area**: 12-ready-for-ai-mcp · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0015 (Cognito + auth BFF), UC 0016 (Pre-Token-Gen + JWT), UC 0013 (accounts/users), UC 0061 (architettura server MCP) — devono esistere prima: il fornitore di identità e il canale che scambia i token, l'iniezione del `tenant_id` nel token verificato, il modello account/utenti su cui poggia la delega, e la collocazione del server MCP che consuma le credenziali.
**Fonte**: R2 (Tabella dei residui, _INDEX.md) · docs/_BACKLOG.md §"Ready for AI"
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Definire **come un assistente AI ottiene accesso delegato** ai dati e alle operazioni di un tenant, senza mai maneggiare
la password dell'utente: flusso di autorizzazione, ambiti di accesso (scope) per-app, emissione e durata delle credenziali
delegate, e **revoca**. È il punto più delicato dell'epica: mette un software terzo (l'assistente) in condizione di agire
per conto dell'utente. **Fuori scope**: l'implementazione degli strumenti (UC 0063), il conteggio quota (UC 0064).

## 2. Attori & ruoli
- **Utente del tenant** (owner/admin/member): concede o revoca la delega dal backoffice.
- **Assistente AI** (client MCP): chiede la delega e la usa per invocare strumenti.
- **Cognito + auth BFF** (UC 0015): fornitore di identità e mediatore che scambia i token.
- **Pre-Token-Gen** (UC 0016): inietta `tenant_id` e ruoli nel token verificato.
- **Piattaforma appgrove**: registro delle deleghe attive, verifica al bordo, motore di revoca.

## 3. Precondizioni
- L'utente è già autenticato nel backoffice (sessione valida, UC 0015).
- Il suo account/tenant esiste (UC 0013) e ha almeno un'app entitled da esporre.
- Il server MCP esiste e sa consumare le credenziali delegate (UC 0061).

## 4. Flusso principale
1. Dall'assistente l'utente avvia il collegamento all'account appgrove; l'assistente reindirizza l'utente alla
   **schermata di consenso** del backoffice (flusso di autorizzazione delegata basato su reindirizzamento, con scambio
   di un codice a uso singolo — nessuna password lascia mai il backoffice).
2. Il backoffice, con l'utente già autenticato, mostra **cosa** l'assistente potrà fare: quali app, quali categorie di
   operazioni (per-app, dal contratto UC 0063), e con quali limiti.
3. L'utente sceglie gli **ambiti** (scope) da concedere — granularità almeno per-app, preferibilmente per-categoria di
   operazione (es. "solo lettura", "emissione documenti") — e conferma.
4. La piattaforma registra la **delega** (chi ha delegato, quale assistente, quali scope, quando) e restituisce
   all'assistente una credenziale delegata a **durata limitata**, rinnovabile, revocabile.
5. Da quel momento le sessioni MCP dell'assistente presentano la credenziale; al bordo essa viene verificata e da lì
   si ricava il `tenant_id` (invariante: **mai** dai parametri della chiamata). Ogni scope non concesso è negato *fail-closed*.
6. L'utente può in ogni momento vedere e **revocare** le deleghe attive dal backoffice; la revoca invalida subito la credenziale.

## 5. Flussi alternativi / edge / errori
- **Revoca**: l'utente revoca → la credenziale è invalidata al primo controllo al bordo; l'assistente riceve un errore
  chiaro "accesso revocato" e deve ripetere il consenso per riottenere accesso.
- **Scadenza**: credenziale scaduta → l'assistente rinnova entro i limiti della delega; se la delega è stata revocata,
  il rinnovo fallisce *fail-closed*.
- **Cambio ruolo/uscita membro**: se l'utente delegante perde il ruolo o lascia il tenant (UC 0013), le sue deleghe decadono.
- **Scope insufficiente**: l'assistente invoca uno strumento fuori dagli scope concessi → negato, con messaggio che indica
  quale ulteriore consenso servirebbe.
- **Errore — assistente non riconosciuto / redirect non valido**: consenso rifiutato, nessuna credenziale emessa.
- **Edge — più assistenti**: un utente può avere deleghe distinte verso assistenti diversi, ognuna con i propri scope e revoca.

## 6. Risorse & runbook — Schermate & stati
- **Schermata di consenso** (backoffice): titolo "Consenti a <assistente> di operare sul tuo account", elenco app+scope
  con interruttori, nota sulla durata e sulla revocabilità, pulsanti "Consenti" / "Annulla". Copy in italiano; UI multilingua.
- **Stati**: *caricamento* (recupero scope disponibili), *scelta* (interruttori), *conferma* (spinner), *successo*
  (ritorno all'assistente), *errore* (assistente non valido / sessione scaduta → rifai login).
- **Pagina "Assistenti collegati"** (profilo/impostazioni): elenco deleghe attive con data, scope, pulsante "Revoca".
- **Runbook**: revoca di emergenza lato piattaforma (invalidare tutte le deleghe di un tenant o di un assistente) come
  procedura operativa in caso di sospetto abuso.

## 7. Dati toccati
- **Registro deleghe**: `tenant_id`, `user_id` del delegante, identità dell'assistente, scope concessi, timestamp di
  concessione/scadenza/revoca. Sono **dati personali** (collegano una persona a un consenso): finalità = gestire la delega
  di accesso; base giuridica = esecuzione del rapporto contrattuale/consenso dell'utente; retention = finché la delega è
  attiva + periodo di prova a fini di audit, poi cancellazione (allineare a UC 0030 / manifesti dati).
- **Punto chiave di minimizzazione**: la credenziale delegata trasporta solo l'identità del tenant e gli scope; **non**
  incorpora dati di dominio. Nessun dato personale del tenant viaggia verso l'assistente in questa fase.

## 8. Permessi & gate
- **Tenant ID solo dal token verificato** al bordo, ricavato dalla credenziale delegata; mai dai parametri.
- La delega non può concedere più di quanto il ruolo dell'utente delegante possiede (un member non delega poteri da owner).
- Filtro row-level `WHERE tenant_id = :tid` a valle, come per ogni operazione tenant-scoped.
- Postura **fail-closed** ovunque: assenza/scadenza/revoca/scope mancante → accesso negato.

## 9. Requisiti di test
- **Consenso e revoca**: concessione → credenziale valida; revoca → invalidazione immediata al bordo.
- **Scope**: strumento fuori scope negato; strumento in scope ammesso.
- **Isolamento cross-tenant**: credenziale del tenant A non accede mai a dati del tenant B.
- **Delega ≤ ruolo**: un member non ottiene scope da owner.
- **End-to-end** della schermata di consenso (redirect, scelta scope, ritorno all'assistente) via test di interfaccia.

## 10. Riferimenti & Definition of Done
- **Riferimenti**: UC 0015 (Cognito + auth BFF), UC 0016 (Pre-Token-Gen + JWT), UC 0013 (accounts/users);
  file fratelli: 0061-architettura-server-mcp.md, 0063-mappatura-operazioni-strumenti-mcp.md, 0064-enforcement-quota-entitlement-ai.md,
  0065-sicurezza-audit-invocazioni-ai.md.
- **DoD**: flusso di consenso disegnato con scope per-app; registro deleghe e revoca definiti; postura fail-closed; nessun
  dato personale verso l'assistente in fase di consenso; test di consenso/revoca/scope/isolamento verdi.

## Punti aperti / decisioni differite
- **ESCALATION — modello di consenso da decidere a livello di prodotto e sicurezza.** Questo use case è il punto più
  delicato dell'epica: il modello preciso di delega (protocollo di autorizzazione, granularità degli scope, durata,
  rinnovo, comportamento della revoca) **non è deciso** e non va forzato qui. Va chiuso in una **sessione dedicata**
  con la direzione di prodotto e chi presidia la sicurezza. Proprietaria: **epica 12**.
- **L'intera epica 12 è direzione di prodotto non ancora decisa** (collocazione UC 0061, formato strumenti UC 0063).
