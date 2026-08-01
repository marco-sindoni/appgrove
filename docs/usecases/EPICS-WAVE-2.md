# Onda 2 (evo) — Indice di esecuzione delle epiche 12–21

**Ordine di esecuzione reale** delle **37 storie evolutive** (UC `0061`–`0097`, epiche `12`–`21`), nello stesso spirito di
[_INDEX.md](_INDEX.md): un ordinamento **topologico sulle dipendenze effettive**, da implementare **dall'alto verso il
basso**, così che ogni storia trovi i suoi prerequisiti *sopra* di sé. Catalogo per epica: [README.md](README.md) →
"Epiche evolutive (evo)". Formalizzazione: epiche 12–19 dal backlog (change `0064`); epica 20 (test e2e di
piattaforma) aggiunta con gli UC 0090–0094; epica 21 (catalogo app & UX backoffice) dalla change `0066`.

## Criterio di ordinamento

Differenza sostanziale rispetto a `_INDEX.md`: **tutti i prerequisiti sull'implementazione base (UC `0001`–`0060`) sono
già in `main`** (onda 1 completata). Quindi le dipendenze verso la base **non vincolano** l'ordine di questa onda —
sono soddisfatte per costruzione (colonna "Dip. base ✅", puramente informativa, come lo erano le `☁` in `_INDEX.md`
per lo sviluppo locale). L'**unico vincolo topologico reale** è dato dalle **dipendenze fra storie evo** (colonna
"Dip. evo").

A parità di vincolo (la maggior parte delle storie non ha prerequisiti evo), si adotta la strategia
**"prerequisiti-interni e lavoro deciso prima, decisioni-di-prodotto dopo"**, in cinque fasi:

- **A — Abilitanti di piattaforma** (decise, sbloccano il resto);
- **B — Messa in cloud & go-live** (blocco operativo coeso, ordine interno per dipendenze);
- **C — Self-service, supporto, contenuti** (decise, alto valore);
- **D — Direzione di prodotto da decidere** (implementabili **dopo** la relativa decisione: modello utenti, Ready-for-AI, trial);
- **E — Bassa priorità / deprioritizzate**.

## Legenda
- **Dip. evo**: prerequisiti **fra storie di questa onda** che DEVONO comparire più in alto (vincolo d'ordine). `—` = nessuno.
  `(soft)` = relazione utile ma non bloccante.
- **Dip. base ✅**: prerequisiti sull'implementazione base — **già in `main`**, non vincolano l'ordine (informativi).
- **Decisione**: 🟢 decisa (implementabile) · 🟠 richiede una **decisione di prodotto** prima di implementare.
- **Stato** (implementazione): ⬜ da implementare · 🟡 in corso (change `NNNN-use-case-YYYY-…` aperta, non in main) · ✅ implementato (in main).
  All'apertura di questo indice tutte le storie evo sono **⬜** (onda 1 in main, onda 2 non ancora avviata).

---

| # | UC | Titolo | Epica | Dip. evo | Dip. base ✅ | Decisione | Stato |
|---|------|--------|-------|----------|-------------|-----------|-------|
| **A. Abilitanti di piattaforma** |
| 1 | [0077](15-supporto-e-piattaforma/0077-provider-entitlement-reale.md) | Provider entitlement reale (sostituisce lo stub) | 15 | — | 0013, 0020, 0021, 0027, 0025, 0024 | 🟢 | ✅ |
| 2 | [0085](17-skill-e-tooling-contenuto/0085-unificazione-renderer-email-commons.md) | Unificazione renderer email in `services/commons` | 17 | — | 0018, 0039 | 🟢 | ⬜ |
| 3 | [0086](18-brand-e-design-system/0086-brand-kit-token-condiviso.md) | Brand kit / token condiviso (fonte unica) | 18 | — | 0019, 0036, 0046 | 🟢 | ⬜ |
| 4 | [0087](18-brand-e-design-system/0087-artwork-logo-e-illustrazioni.md) | Artwork logo finale + illustrazioni on-brand | 18 | 0086 | 0019, 0037 | 🟢 | ⬜ |
| **B. Messa in cloud & go-live** |
| 5 | [0080](16-messa-in-cloud-golive/0080-prima-esecuzione-live-pipeline.md) | Prima esecuzione live pipeline + config repo GitHub | 16 | — | 0005, 0003, 0004, 0055 | 🟢 | ⬜ |
| 6 | [0082](16-messa-in-cloud-golive/0082-script-attivazione-ambienti-fasi.md) | Script attivazione ambienti (`test-start`/`test-stop` + cron) | 16 | 0080 | 0004, 0055, 0005, 0006 | 🟢 | ⬜ |
| 7 | [0079](16-messa-in-cloud-golive/0079-gestione-rimbalzi-reclami-ses.md) | Gestione rimbalzi/reclami SES | 16 | — | 0018, 0006 | 🟢 | ⬜ |
| 8 | [0078](16-messa-in-cloud-golive/0078-uscita-ses-sandbox.md) | Uscita di SES dalla sandbox _(bloccante go-live)_ | 16 | 0079 | 0018 | 🟢 | ⬜ |
| 9 | [0081](16-messa-in-cloud-golive/0081-smoke-reali-cloud-test.md) | Smoke reali cloud alla prima accensione `test` | 16 | 0080, 0082 | 0015, 0016, 0014, 0018, 0055, 0005 | 🟢 | ⬜ |
| 10 | [0083](16-messa-in-cloud-golive/0083-drift-regione-e-casella-security.md) | Drift regione `eu-south-1`→`eu-west-1` + casella `security@` | 16 | 0080 (soft) | 0005, 0037, 0049 | 🟢 | ⬜ |
| **C. Self-service, supporto, contenuti** |
| 11 | [0067](13-abbonamenti-self-service/0067-gestione-abbonamento-self-service.md) | Gestione abbonamento self-service (backoffice "Abbonamenti") | 13 | — | 0026, 0028, 0027, 0024, 0020 | 🟢 | ⬜ |
| 12 | [0071](13-abbonamenti-self-service/0071-riconciliazione-netto-revenue.md) | Riconciliazione netto/revenue | 13 | — | 0025, 0006, 0021 | 🟢 | ⬜ |
| 13 | [0075](15-supporto-e-piattaforma/0075-ticketing-nativo-in-house.md) | Ticketing nativo in-house | 15 | 0085 (soft) | 0012, 0013, 0020, 0021, 0018, 0034 | 🟢 | ⬜ |
| 14 | [0076](15-supporto-e-piattaforma/0076-disabilita-applicazione.md) | Disabilita applicazione (feature admin reversibile) | 15 | — | 0021, 0027, 0014, 0035 | 🟢 | ⬜ |
| 15 | [0084](17-skill-e-tooling-contenuto/0084-skill-new-blog-post.md) | Skill `new-blog-post` | 17 | 0086 (soft) | 0042, 0057, 0046, 0040, 0041 | 🟢 | ⬜ |
| **C2. Test end-to-end di piattaforma** |
| 16 | [0090](20-test-e2e-piattaforma/0090-e2e-platform-fondamenta.md) | Fondamenta suite e2e di piattaforma (stack reale + Mailpit + primo journey) | 20 | — | 0058, 0018, 0020, 0023, 0029 | 🟢 | ✅ |
| 17 | [0091](20-test-e2e-piattaforma/0091-e2e-platform-journey-utente.md) | Batteria journey e2e lato utente | 20 | 0090 | 0024, 0027, 0059, 0028, 0033, 0056 | 🟢 | ⬜ |
| 18 | [0092](20-test-e2e-piattaforma/0092-e2e-platform-journey-admin.md) | Batteria journey e2e lato admin + guasti di piattaforma | 20 | 0090, 0076, 0077 | 0021, 0034 | 🟢 | ⬜ |
| 19 | [0093](20-test-e2e-piattaforma/0093-e2e-platform-registro-copertura.md) | Registro di copertura e2e + check meccanico | 20 | 0090, 0091, 0092 | 0045 | 🟢 | ⬜ |
| 20 | [0094](20-test-e2e-piattaforma/0094-e2e-platform-workflow-skill.md) | Integrazione copertura e2e nel workflow delle skill | 20 | 0093 | 0044, 0045, 0046 | 🟢 | ⬜ |
| **C3. Catalogo app & UX backoffice** |
| 21 | [0095](21-catalogo-app-backoffice/0095-pagina-app-catalog.md) | Pagina "App catalog" del backoffice (card, stati, ricerca, paginazione) | 21 | 0077 | 0020, 0022, 0024, 0026 | 🟢 | ⬜ |
| 22 | [0096](21-catalogo-app-backoffice/0096-billing-solo-fatturazione.md) | Billing solo-fatturazione (abbonamenti + storico pagamenti/ricevute) | 21 | 0095, 0076 (soft), 0077 | 0028, 0025, 0026 | 🟢 | ⬜ |
| 23 | [0097](21-catalogo-app-backoffice/0097-dashboard-operativa.md) | Dashboard operativa del workspace (+ Workspace ID in Account) | 21 | 0095, 0077 | 0027, 0028, 0059, 0056 | 🟢 | ⬜ |
| **D. Direzione di prodotto da decidere** |
| 24 | [0072](14-modello-utenti-multiapp/0072-distinzione-b2c-b2b-livello-app.md) | Distinzione B2C/B2B a livello app (`App.user_model`) | 14 | — | 0013, 0059, 0051, 0054 | 🟠 | ⬜ |
| 25 | [0073](14-modello-utenti-multiapp/0073-invito-utenti-per-app-posti-quota.md) | Invito utenti per-app con "posti" come metrica `stock` | 14 | 0072 | 0027, 0046, 0047 | 🟠 | ⬜ |
| 26 | [0074](14-modello-utenti-multiapp/0074-directory-cross-app-ui-membri.md) | Directory cross-app + UI "Membri" per-app | 14 | 0072, 0073 | 0059, 0013 | 🟠 | ⬜ |
| 27 | [0061](12-ready-for-ai-mcp/0061-architettura-server-mcp.md) | Architettura & collocazione del server MCP | 12 | — | 0004, 0055, 0051, 0014 | 🟠 | ⬜ |
| 28 | [0062](12-ready-for-ai-mcp/0062-auth-consenso-delegato-ai.md) | Autenticazione e consenso delegato (AI → tenant) | 12 | 0061 | 0015, 0016, 0013 | 🟠 | ⬜ |
| 29 | [0063](12-ready-for-ai-mcp/0063-mappatura-operazioni-strumenti-mcp.md) | Mappatura operazioni → strumenti MCP | 12 | 0061 | 0051, 0046 | 🟠 | ⬜ |
| 30 | [0064](12-ready-for-ai-mcp/0064-enforcement-quota-entitlement-ai.md) | Enforcement entitlement/quota sulle chiamate AI | 12 | 0061 · 0077 (soft) | 0027, 0026 | 🟠 | ⬜ |
| 31 | [0065](12-ready-for-ai-mcp/0065-sicurezza-audit-invocazioni-ai.md) | Sicurezza & audit invocazioni AI + privacy | 12 | 0061 | 0006, 0030 | 🟠 | ⬜ |
| 32 | [0066](12-ready-for-ai-mcp/0066-industrializzazione-mcp-newapp.md) | Industrializzazione MCP + riconciliazione claim sito | 12 | 0061, 0062, 0063, 0064, 0065 | 0046, 0004, 0037 | 🟠 | ⬜ |
| 33 | [0069](13-abbonamenti-self-service/0069-trial-una-tantum-tenant-app.md) | Trial una-tantum per tenant×app | 13 | — | 0026, 0024, 0027 | 🟠 | ⬜ |
| **E. Bassa priorità / deprioritizzate** |
| 34 | [0068](13-abbonamenti-self-service/0068-pausa-ripresa-subscription.md) | Pausa/ripresa subscription self-service | 13 | — | 0026, 0028, 0020 | 🟢 | ⬜ |
| 35 | [0070](13-abbonamenti-self-service/0070-bundling-abbonamento-multi-app.md) | Bundling: più app in un unico abbonamento | 13 | — | 0022, 0026 | 🟢 | ⬜ |
| 36 | [0088](19-debito-tecnico/0088-search-globale-workspace.md) | Search globale dal workspace del backoffice | 19 | — | 0020, 0013 | 🟢 | ⬜ |
| 37 | [0089](19-debito-tecnico/0089-rimozione-legacy-peer-deps.md) | Rimozione `legacy-peer-deps` nel frontend | 19 | — | 0020, 0019 | 🟢 | ⬜ |

---

## Vincoli di dipendenza evo (le uniche catene che ordinano l'onda)

Tutte soddisfatte dall'ordine sopra (il prerequisito è sempre più in alto):

- **Epica 12 (MCP)**: `0061` (architettura) apre l'epica; `0062`/`0063`/`0064`/`0065` dipendono da `0061`; `0066`
  (industrializzazione) chiude dipendendo da **tutte** `0061`–`0065`.
- **Epica 14 (utenti)**: `0072` (modello) → `0073` (posti) → `0074` (directory + UI).
- **Epica 16 (cloud)**: `0080` (config repo + prima corsa) abilita `0082` (script ambienti) e, con esso, `0081`
  (smoke); `0079` (rimbalzi SES) precede `0078` (uscita sandbox, più solida se presentata dopo la gestione rimbalzi);
  `0083` è a valle di `0080` (soft, config per-ambiente).
- **Epica 18 (brand)**: `0086` (brand kit) → `0087` (logo/illustrazioni).
- **Epica 21 (catalogo/UX)**: `0095` (catalogo) apre e assorbe il punto aperto "vetrina real-catalog" di UC 0024;
  `0096` (Billing) e `0097` (Dashboard) dipendono da `0095`; `0096` chiude il punto aperto Billing di `0076` (soft).
- **Epica 20 (test e2e piattaforma)**: `0090` (fondamenta) apre; `0091`/`0092` (batterie) dipendono da `0090`
  (`0092` anche da `0076`, disabilita applicazione, e da `0077`, già ✅); `0093` (registro) da entrambe le
  batterie; `0094` (workflow skill) chiude dipendendo da `0093`.
- **Dipendenze soft inter-epica**: `0064` (enforcement AI) guadagna da `0077` (entitlement reale, già in fase A);
  `0075` (ticketing) dal renderer email `0085`; `0084` (blog) dal brand kit `0086`. Nessuna è bloccante.

## Note

- **Zero violazioni** dall'alto verso il basso: leggendo la tabella in ordine, ogni "Dip. evo" è già stata implementata.
- Le storie **🟠** (epiche 12 e 14, storia `0069`) sono ordinate *anche* dopo le decise perché sono gated da una
  **decisione di prodotto**, non da codice: la loro implementazione parte quando la direzione è confermata (vedi i
  "Punti aperti / decisioni differite" nelle rispettive storie). Il vincolo topologico interno resta comunque valido.
- Man mano che una storia evo matura e viene implementata, va **promossa** nella tabella di esecuzione principale di
  [_INDEX.md](_INDEX.md) e marcata ✅ dalla `new-change`, come per gli use case base.
- Questo indice è l'**asse esecutivo** dell'onda 2; il **catalogo per epica** (con drill-down) è in [README.md](README.md).
