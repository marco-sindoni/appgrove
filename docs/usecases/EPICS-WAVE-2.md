# Onda 2 (evo) — Indice di esecuzione delle epiche 12–22

**Ordine di esecuzione reale** delle **55 storie evolutive da implementare** (UC `0061`–`0118`, epiche `12`–`22`; sono 58
nel catalogo, meno le **tre superate** dall'epica 22 — vedi in coda), nello stesso spirito di
[_INDEX.md](_INDEX.md): un ordinamento **topologico sulle dipendenze effettive**, da implementare **dall'alto verso il
basso**, così che ogni storia trovi i suoi prerequisiti *sopra* di sé. Catalogo per epica: [README.md](README.md) →
"Epiche evolutive (evo)". Formalizzazione: epiche 12–19 dal backlog (change `0064`); epica 20 (test e2e di
piattaforma) aggiunta con gli UC 0090–0094; epica 21 (catalogo app & UX backoffice) dalla change `0066`; **epica 22 (rifacimento del modello di appartenenza)**
dalla change `0087`, che ne ha prodotto l'analisi completa — epiche, storie, piani di lavoro e prototipi navigabili —
e che **supera l'epica 14**.

## Criterio di ordinamento

Differenza sostanziale rispetto a `_INDEX.md`: **tutti i prerequisiti sull'implementazione base (UC `0001`–`0060`) sono
già in `main`** (onda 1 completata). Quindi le dipendenze verso la base **non vincolano** l'ordine di questa onda —
sono soddisfatte per costruzione (colonna "Dip. base ✅", puramente informativa, come lo erano le `☁` in `_INDEX.md`
per lo sviluppo locale). L'**unico vincolo topologico reale** è dato dalle **dipendenze fra storie evo** (colonna
"Dip. evo").

A parità di vincolo (la maggior parte delle storie non ha prerequisiti evo), si adotta la strategia
**"prerequisiti-interni e lavoro deciso prima, decisioni-di-prodotto dopo"**, in cinque fasi:

- **A — Abilitanti di piattaforma** (decise, sbloccano il resto);
- **A2 — Rifacimento del modello di appartenenza** (epica 22: blocco coeso e **prima del go-live**, vedi la nota
  della sezione);
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
| 2 | [0085](17-skill-e-tooling-contenuto/0085-unificazione-renderer-email-commons.md) | Unificazione renderer email in `services/commons` | 17 | — | 0018, 0039 | 🟢 | ✅ |
| 3 | [0086](18-brand-e-design-system/0086-brand-kit-token-condiviso.md) | Brand kit / token condiviso (fonte unica) | 18 | — | 0019, 0036, 0046 | 🟢 | ✅ |
| 4 | [0087](18-brand-e-design-system/0087-artwork-logo-e-illustrazioni.md) | Artwork logo finale + illustrazioni on-brand | 18 | 0086 | 0019, 0037 | 🟢 | ✅ |
| **A2. Rifacimento del modello di appartenenza (epica 22)** |
| 5 | [0116](22-refactor-membership-model/story/0116-identita-e-appartenenze.md) | Identità della persona e appartenenze agli account | 22 | — | 0013, 0010 | 🟢 | ✅ |
| 6 | [0117](22-refactor-membership-model/story/0117-account-attivo-e-selettore.md) | Account attivo nella sessione, selettore, parità dei fornitori di identità | 22 | 0116 | 0010, 0015, 0016, 0017 | 🟢 | ✅ |
| 7 | [0118](22-refactor-membership-model/story/0118-inviti-e-registrazione-con-identita-esistente.md) | Inviti e registrazione quando l'identità esiste già | 22 | 0116, 0117 | 0013, 0058, 0059 | 🟢 | ✅ |
| 8 | [0098](22-refactor-membership-model/story/0098-modello-dati-accesso-per-applicazione.md) | Modello dati dell'accesso per applicazione + ruolo di piattaforma a due valori | 22 | 0116 | 0013, 0059 | 🟢 | ✅ |
| 9 | [0099](22-refactor-membership-model/story/0099-autorizzazione-per-applicazione.md) | Autorizzazione per applicazione: varco riusabile in `commons`, ruolo fuori dal token | 22 | 0098 | 0016, 0010, 0027 | 🟢 | ✅ |
| 10 | [0101](22-refactor-membership-model/story/0101-semantica-ruoli-viewer-editor-admin.md) | Semantica dei tre ruoli come contratto di piattaforma verificabile | 22 | 0098, 0099 | 0051 | 🟢 | ⬜ |
| 11 | [0100](22-refactor-membership-model/story/0100-sezione-members-elenco-unico.md) | «Members» come elenco unico di persone, senza ruolo | 22 | 0098, 0099 | 0059 | 🟢 | ⬜ |
| 12 | [0102](22-refactor-membership-model/story/0102-listino-posti-a-fasce.md) | Listino dei posti a **scaglioni progressivi**: modello versionato e calcolo | 22 | 0098 | 0022 | 🟢 | ⬜ |
| 13 | [0103](22-refactor-membership-model/story/0103-acquisto-anticipato-posto-invito.md) | Acquisto anticipato del posto all'invito (abbonamento di piattaforma) | 22 | 0100, 0102 | 0024, 0025, 0026 | 🟢 | ⬜ |
| 14 | [0104](22-refactor-membership-model/story/0104-riduzione-posti-in-attesa.md) | Riduzione dei posti in attesa: scelta, blocco, annullamento, scadenza | 22 | 0103 | 0026, 0028 | 🟢 | ⬜ |
| 15 | [0105](22-refactor-membership-model/story/0105-governo-listino-console-piattaforma.md) | Governo del listino dalla console di piattaforma (dal ciclo successivo) | 22 | 0102 | 0021, 0047 | 🟢 | ⬜ |
| 16 | [0106](22-refactor-membership-model/story/0106-posti-in-billing.md) | I posti nella sezione «Billing»: righe, storico, prossimo rinnovo | 22 | 0103, 0105, **0096** | 0067 | 🟢 | ⬜ |
| 17 | [0107](22-refactor-membership-model/story/0107-menu-rotte-visibilita-per-ruolo.md) | Menu, rotte e visibilità per ruolo (intersezione a tre) | 22 | 0099, 0100, **0077** | 0020 | 🟢 | ⬜ |
| 18 | [0108](22-refactor-membership-model/story/0108-cruscotto-collaboratore.md) | Cruscotto del collaboratore, senza azioni dispositive | 22 | 0107, **0097** | — | 🟢 | ⬜ |
| 19 | [0109](22-refactor-membership-model/story/0109-catalogo-sola-lettura-richiesta-owner.md) | Catalogo in sola lettura + richiesta «chiedi all'owner di installare» | 22 | 0107, **0095**, 0085 | 0018 | 🟢 | ⬜ |
| 20 | [0110](22-refactor-membership-model/story/0110-miei-dati-forma-ridotta.md) | «I miei dati» in forma ridotta per il collaboratore | 22 | 0107 | 0033 | 🟢 | ⬜ |
| 21 | [0111](22-refactor-membership-model/story/0111-schermata-gestione-utenti-app.md) | Schermata «Gestione utenti» dentro ogni applicazione (+ ritiro dei posti del Mini-CRM) | 22 | 0098, 0099, 0101, 0107 | 0027 | 🟢 | ⬜ |
| 22 | [0112](22-refactor-membership-model/story/0112-copilota-ruoli-new-application.md) | Copilota dei ruoli nella skill `new-application` + parità dello scaffolding | 22 | 0101, 0099, 0111 | 0046 | 🟢 | ⬜ |
| 23 | [0114](22-refactor-membership-model/story/0114-ritiro-categoria-b2c-b2b.md) | Ritiro della categoria B2C/B2B delle applicazioni (`App.user_model`) | 22 | 0099, 0101, 0112 | 0022 | 🟢 | ⬜ |
| 24 | [0115](22-refactor-membership-model/story/0115-ambito-dati-applicazione.md) | Ambito dei dati di un'applicazione: dichiarazione + guardia (il filtro va con la prima app che lo usa) | 22 | 0114, 0101 | 0032 | 🟢 | ⬜ |
| 25 | [0113](22-refactor-membership-model/story/0113-migrazione-account-e-copertura-e2e.md) | Migrazione degli account esistenti + copertura end-to-end per ruolo | 22 | tutte le precedenti dell'epica, **0090**, **0093** | 0045 | 🟢 | ⬜ |
| **B. Messa in cloud & go-live** |
| 26 | [0080](16-messa-in-cloud-golive/0080-prima-esecuzione-live-pipeline.md) | Prima esecuzione live pipeline + config repo GitHub | 16 | — | 0005, 0003, 0004, 0055 | 🟢 | ⬜ |
| 27 | [0082](16-messa-in-cloud-golive/0082-script-attivazione-ambienti-fasi.md) | Script attivazione ambienti (`test-start`/`test-stop` + cron) | 16 | 0080 | 0004, 0055, 0005, 0006 | 🟢 | ⬜ |
| 28 | [0079](16-messa-in-cloud-golive/0079-gestione-rimbalzi-reclami-ses.md) | Gestione rimbalzi/reclami SES | 16 | — | 0018, 0006 | 🟢 | ⬜ |
| 29 | [0078](16-messa-in-cloud-golive/0078-uscita-ses-sandbox.md) | Uscita di SES dalla sandbox _(bloccante go-live)_ | 16 | 0079 | 0018 | 🟢 | ⬜ |
| 30 | [0081](16-messa-in-cloud-golive/0081-smoke-reali-cloud-test.md) | Smoke reali cloud alla prima accensione `test` | 16 | 0080, 0082 | 0015, 0016, 0014, 0018, 0055, 0005 | 🟢 | ⬜ |
| 31 | [0083](16-messa-in-cloud-golive/0083-drift-regione-e-casella-security.md) | Drift regione `eu-south-1`→`eu-west-1` + casella `security@` | 16 | 0080 (soft) | 0005, 0037, 0049 | 🟢 | ⬜ |
| **C. Self-service, supporto, contenuti** |
| 32 | [0067](13-abbonamenti-self-service/0067-gestione-abbonamento-self-service.md) | Gestione abbonamento self-service (backoffice "Abbonamenti") | 13 | — | 0026, 0028, 0027, 0024, 0020 | 🟢 | ✅ |
| 33 | [0071](13-abbonamenti-self-service/0071-riconciliazione-netto-revenue.md) | Riconciliazione netto/revenue | 13 | — | 0025, 0006, 0021 | 🟢 | ✅ |
| 34 | [0075](15-supporto-e-piattaforma/0075-ticketing-nativo-in-house.md) | Ticketing nativo in-house | 15 | 0085 (soft) | 0012, 0013, 0020, 0021, 0018, 0034 | 🟢 | ✅ |
| 35 | [0076](15-supporto-e-piattaforma/0076-disabilita-applicazione.md) | Disabilita applicazione (feature admin reversibile) | 15 | — | 0021, 0027, 0014, 0035 | 🟢 | ✅ |
| 36 | [0084](17-skill-e-tooling-contenuto/0084-skill-new-blog-post.md) | Skill `new-blog-post` | 17 | 0086 (soft) | 0042, 0057, 0046, 0040, 0041 | 🟢 | ✅ |
| **C2. Test end-to-end di piattaforma** |
| 37 | [0090](20-test-e2e-piattaforma/0090-e2e-platform-fondamenta.md) | Fondamenta suite e2e di piattaforma (stack reale + Mailpit + primo journey) | 20 | — | 0058, 0018, 0020, 0023, 0029 | 🟢 | ✅ |
| 38 | [0091](20-test-e2e-piattaforma/0091-e2e-platform-journey-utente.md) | Batteria journey e2e lato utente | 20 | 0090 | 0024, 0027, 0059, 0028, 0033, 0056 | 🟢 | ✅ |
| 39 | [0092](20-test-e2e-piattaforma/0092-e2e-platform-journey-admin.md) | Batteria journey e2e lato admin + guasti di piattaforma | 20 | 0090, 0076, 0077 | 0021, 0034 | 🟢 | ✅ |
| 40 | [0093](20-test-e2e-piattaforma/0093-e2e-platform-registro-copertura.md) | Registro di copertura e2e + check meccanico | 20 | 0090, 0091, 0092 | 0045 | 🟢 | ✅ |
| 41 | [0094](20-test-e2e-piattaforma/0094-e2e-platform-workflow-skill.md) | Integrazione copertura e2e nel workflow delle skill | 20 | 0093 | 0044, 0045, 0046 | 🟢 | ✅ |
| **C3. Catalogo app & UX backoffice** |
| 42 | [0095](21-catalogo-app-backoffice/0095-pagina-app-catalog.md) | Pagina "App catalog" del backoffice (card, stati, ricerca, paginazione) | 21 | 0077 | 0020, 0022, 0024, 0026 | 🟢 | ✅ |
| 43 | [0096](21-catalogo-app-backoffice/0096-billing-solo-fatturazione.md) | Billing solo-fatturazione (abbonamenti + storico pagamenti/ricevute) | 21 | 0095, 0076 (soft), 0077 | 0028, 0025, 0026 | 🟢 | ✅ |
| 44 | [0097](21-catalogo-app-backoffice/0097-dashboard-operativa.md) | Dashboard operativa del workspace (+ Workspace ID in Account) | 21 | 0095, 0077 | 0027, 0028, 0059, 0056 | 🟢 | ✅ |
| **D. Direzione di prodotto da decidere** |
| 45 | [0061](12-ready-for-ai-mcp/0061-architettura-server-mcp.md) | Architettura & collocazione del server MCP | 12 | — | 0004, 0055, 0051, 0014 | 🟠 | ⬜ |
| 46 | [0062](12-ready-for-ai-mcp/0062-auth-consenso-delegato-ai.md) | Autenticazione e consenso delegato (AI → tenant) | 12 | 0061 | 0015, 0016, 0013 | 🟠 | ⬜ |
| 47 | [0063](12-ready-for-ai-mcp/0063-mappatura-operazioni-strumenti-mcp.md) | Mappatura operazioni → strumenti MCP | 12 | 0061 | 0051, 0046 | 🟠 | ⬜ |
| 48 | [0064](12-ready-for-ai-mcp/0064-enforcement-quota-entitlement-ai.md) | Enforcement entitlement/quota sulle chiamate AI | 12 | 0061 · 0077 (soft) | 0027, 0026 | 🟠 | ⬜ |
| 49 | [0065](12-ready-for-ai-mcp/0065-sicurezza-audit-invocazioni-ai.md) | Sicurezza & audit invocazioni AI + privacy | 12 | 0061 | 0006, 0030 | 🟠 | ⬜ |
| 50 | [0066](12-ready-for-ai-mcp/0066-industrializzazione-mcp-newapp.md) | Industrializzazione MCP + riconciliazione claim sito | 12 | 0061, 0062, 0063, 0064, 0065 | 0046, 0004, 0037 | 🟠 | ⬜ |
| 51 | [0069](13-abbonamenti-self-service/0069-trial-una-tantum-tenant-app.md) | Trial una-tantum per tenant×app | 13 | — | 0026, 0024, 0027 | 🟠 | ⬜ |
| **E. Bassa priorità / deprioritizzate** |
| 52 | [0068](13-abbonamenti-self-service/0068-pausa-ripresa-subscription.md) | Pausa/ripresa subscription self-service | 13 | — | 0026, 0028, 0020 | 🟢 | ⬜ |
| 53 | [0070](13-abbonamenti-self-service/0070-bundling-abbonamento-multi-app.md) | Bundling: più app in un unico abbonamento | 13 | — | 0022, 0026 | 🟢 | ⬜ |
| 54 | [0088](19-debito-tecnico/0088-search-globale-workspace.md) | Search globale dal workspace del backoffice | 19 | — | 0020, 0013 | 🟢 | ⬜ |
| 55 | [0089](19-debito-tecnico/0089-rimozione-legacy-peer-deps.md) | Rimozione `legacy-peer-deps` nel frontend | 19 | — | 0020, 0019 | 🟢 | ⬜ |

---

## Perché l'epica 22 sta qui, e non in coda

**Non ha prerequisiti evolutivi pendenti**: tutti quelli che le servono — `0077` (entitlement reale), `0085`
(renderer email), `0090` e `0093` (fondamenta e registro dei test end-to-end), `0095`, `0096`, `0097` (catalogo,
fatturazione, cruscotto) — sono **già in `main`**. È quindi eseguibile subito, senza attese.

**E conviene farla prima del go-live**, per una ragione che si può ancora scegliere solo adesso: `0116` divide in
due la tabella delle persone e `0113` migra gli account esistenti. Oggi quella migrazione è banale — nessun
account supera i tre posti gratuiti, siamo ancora in locale, e la decisione è registrata nella change `0087`.
Dopo il go-live sarebbe una migrazione su dati reali, con clienti che pagano posti e un'appartenenza da non
perdere. Lo stesso vale per il **listino dei posti** (`0102`–`0106`): partire con il modello a scaglioni
progressivi già in piedi evita di dover cambiare le regole di prezzo a clienti che le hanno già accettate.

La catena interna è lunga ma lineare: **le tre di identità** aprono (senza di loro `0098` nascerebbe con la forma
sbagliata e andrebbe rifatto), poi le **fondamenta** dell'accesso per applicazione, i **posti a pagamento**,
l'**esperienza per ruolo**, e infine il lavoro **dentro le applicazioni** con la migrazione a chiudere.

## Storie superate dall'epica 22 (archivio, non da implementare)

L'epica **14 — modello utenti multi-app** proponeva l'appartenenza **per applicazione**, con i posti come metrica
di quota di ciascuna app e una directory che li ricomponesse. L'epica 22 adotta il modello opposto — appartenenza
**centralizzata**, ruolo per applicazione, posti **di piattaforma** a listino unico — che è esattamente l'opzione
che l'epica 14 registrava come scartata. Le sue tre storie restano nel catalogo come **archivio della decisione
precedente**: dicono cosa si era pensato e perché non si è fatto, che è un'informazione utile e che si perderebbe
cancellandole.

| UC | Titolo | Superata da |
|---|--------|-------------|
| [0072](14-modello-utenti-multiapp/0072-distinzione-b2c-b2b-livello-app.md) | Distinzione B2C/B2B a livello app (`App.user_model`) | [0114](22-refactor-membership-model/story/0114-ritiro-categoria-b2c-b2b.md) la **ritira** (il nuovo modello la rende falsa) e [0115](22-refactor-membership-model/story/0115-ambito-dati-applicazione.md) ne prende il posto con l'**ambito dei dati** |
| [0073](14-modello-utenti-multiapp/0073-invito-utenti-per-app-posti-quota.md) | Invito utenti per-app con «posti» come metrica `stock` | [0098](22-refactor-membership-model/story/0098-modello-dati-accesso-per-applicazione.md) (accesso per applicazione) e [0102](22-refactor-membership-model/story/0102-listino-posti-a-fasce.md)–[0103](22-refactor-membership-model/story/0103-acquisto-anticipato-posto-invito.md) (posti di piattaforma) |
| [0074](14-modello-utenti-multiapp/0074-directory-cross-app-ui-membri.md) | Directory cross-app + interfaccia «Membri» per-app | [0100](22-refactor-membership-model/story/0100-sezione-members-elenco-unico.md) (elenco unico) e [0111](22-refactor-membership-model/story/0111-schermata-gestione-utenti-app.md) (gestione utenti nell'applicazione) |

## Vincoli di dipendenza evo (le uniche catene che ordinano l'onda)

Tutte soddisfatte dall'ordine sopra (il prerequisito è sempre più in alto):

- **Epica 12 (MCP)**: `0061` (architettura) apre l'epica; `0062`/`0063`/`0064`/`0065` dipendono da `0061`; `0066`
  (industrializzazione) chiude dipendendo da **tutte** `0061`–`0065`.
- **Epica 14 (utenti)**: **superata dall'epica 22** — non ordina più nulla (vedi l'archivio sopra).
- **Epica 22 (appartenenza)**: `0116` (identità e appartenenze) apre e precede **tutto**, `0098` compreso, perché
  la tabella degli accessi nasce riferendo l'identità; `0117` (account attivo) dipende da `0116`; `0118` (inviti e
  registrazione) da entrambe. Poi `0098` → `0099` → `0101` → `0100`; i posti `0102` → `0103` → `0104`, con `0105`
  (governo del listino) a valle di `0102` e `0106` (fatturazione) a valle di `0103`, `0105` e **`0096`**;
  l'esperienza per ruolo `0107` (che dipende anche da **`0077`**) → `0108` (anche **`0097`**), `0109` (anche
  **`0095`** e `0085`), `0110`; dentro le applicazioni `0111` → `0112` → `0114` → `0115`; `0113` (migrazione e
  copertura) chiude dipendendo da **tutte** le precedenti dell'epica più **`0090`** e **`0093`**.
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
- **Epica 22**: le sue ventuno storie sono **🟢 decise** — i requisiti sono stati dettati dallo sviluppatore e
  l'analisi (epiche, storie, piani, prototipi navigabili) è stata riletta e approvata nella change `0087`. Non
  attendono nessuna decisione di prodotto: restano aperti solo punti minori, elencati nell'indice dell'area
  [22-refactor-membership-model/README.md](22-refactor-membership-model/README.md).
- Le storie **🟠** (epica 12, storia `0069`) sono ordinate *anche* dopo le decise perché sono gated da una
  **decisione di prodotto**, non da codice: la loro implementazione parte quando la direzione è confermata (vedi i
  "Punti aperti / decisioni differite" nelle rispettive storie). Il vincolo topologico interno resta comunque valido.
  L'epica 14, che era l'altro blocco 🟠, non è più in attesa di una decisione: **la decisione è stata presa in senso
  opposto**, e il suo posto nell'onda è occupato dall'epica 22.
- Man mano che una storia evo matura e viene implementata, va **promossa** nella tabella di esecuzione principale di
  [_INDEX.md](_INDEX.md) e marcata ✅ dalla `new-change`, come per gli use case base.
- Questo indice è l'**asse esecutivo** dell'onda 2; il **catalogo per epica** (con drill-down) è in [README.md](README.md).
