# Testing strategy — Decisioni

**Stato**: 🟢 deciso
**Ultimo aggiornamento**: 2026-06-20

## Scope
Come garantiamo correttezza e non-regressione su tutti gli strati del monorepo (backend Quarkus, 2 SPA React/TS,
infra Terraform), con enfasi sugli **invarianti** (isolamento tenant, authz). Si integra con #07 (test in PR/CI) e
con #11 (test eseguibili in locale offline). Definisce anche cosa **blocca un merge**. Non copre la sicurezza in
senso compliance/legale (→ #13).

## Filosofia (A)
1. **Piramide "integration-heavy" sul backend**: gli invarianti (isolamento `tenant_id`, authz) sono verificabili solo
   con DB reale e più tenant → la maggior confidenza viene dagli integration test, non dagli unit.
2. **Security/multi-tenancy = categoria di prima classe** (D), non un afterthought.
3. **Frontend**: i **test E2E Playwright headless sono OBBLIGATORI** come garanzia funzionale/anti-regressione; i
   component test sono mirati e di supporto.
4. **Coverage pragmatica**: target **soft 80–85%** su backend core/sicurezza (raggiunto soprattutto via integration),
   **nessun hard limit** che blocca. La copertura è un indicatore, non l'obiettivo; ciò che blocca sono i test rossi.

## Decisioni prese

### B. Backend — unit test
5. **JUnit 5 + AssertJ + Mockito**. Unit **mirati** sulla logica di dominio non banale, sul `commons` e sui **casi
   limite**; non inseguire copertura su codice banale o mapping 1:1. Linea guida: poche dipendenze mockate (se ne servono
   troppe → è un integration test). Tutto ciò che riguarda DB/tenant/endpoint → integration (C/D).

### C. Backend — integration test
6. **`@QuarkusTest` + Testcontainers (Postgres 17 effimero per run) + REST Assured**. **Migrazioni Flyway reali**
   applicate al container → si testa la struttura vera degli schemi (`platform` + `app_<id>`) e le migrazioni stesse.
   Isolamento totale per run (DB usa-e-getta), parità di motore con test/prod (Postgres 17).
7. **Copertura misurata con JaCoCo aggregata (unit + integration)**; il target 80–85% si raggiunge soprattutto qui.

### D. Multi-tenancy & security testing (categoria dedicata, sempre eseguita)
8. **Matrice cross-tenant**: ≥2 tenant seedati; come tenant A, per **ogni** risorsa tenant-scoped, *non* deve poter
   leggere/listare/aggiornare/cancellare dati del tenant B. Da fare a matrice, non a campione.
9. **Fail-closed** (inv. #1): JWT senza `tenant_id`/membership invalida → accesso negato (mai default tenant); token
   assente/forgiato/scaduto → negato.
10. **`tenant_id` solo dal JWT**: un `tenant_id` in body/params deve essere **ignorato** (test esplicito anti-override).
11. **Authz per ruolo (matrice)**: `owner`/`admin`/`member` (+ `platform-admin`) × operazioni (`@RolesAllowed`).
12. **Entitlement**: tenant senza `app_id` attivo → bloccato (authorizer + ri-validazione servizio).
13. **Verifica a livello DB** ("leak detector"): dopo le operazioni, query dirette per assertare che nessuna riga di
    altri tenant sia stata toccata.
14. **Identità nei test**: `@TestSecurity` (+ claim custom) per i test rapidi; **smallrye-jwt** per token firmati reali
    (intero percorso di verifica firma/claim).
15. **Harness riusabile di isolamento**: base parametrica che esegue automaticamente la batteria cross-tenant → ogni
    servizio creato da `new-application` eredita i test di isolamento by-default.
16. **ArchUnit = guardia statica degli invarianti**: fa fallire la build se il codice bypassa il filtro tenant, legge
    `tenant_id` da request invece che dal contesto JWT, o crea repository non tenant-aware. Rete strutturale sopra i
    test comportamentali.

### E. Frontend — unit/component
17. **Vitest + React Testing Library + MSW**. Component test **mirati**: form/validazioni (RHF+Zod), rendering
    condizionale (ruoli/stati), hook, stati loading/empty/error di TanStack Query. Focus particolare sul
    **design-system condiviso** (più riuso → più copertura). Coverage pragmatica (la garanzia forte sta negli E2E).

### F. Frontend E2E (Playwright) — OBBLIGATORI
18. **Playwright headless contro lo stack locale reale** (backend + Postgres + Cognito emulato, #11), su **entrambe le
    SPA** (backoffice + admin), **Chromium-first**. Flussi critici: auth (signup/verifica/login+2FA/refresh/logout/reset/
    invite/onboarding), core-loop delle app demo (incl. B2B multi-utente), azioni admin essenziali.
19. **Anti-flakiness**: web-first assertions + auto-wait (no sleep), **login programmatico** (storageState) tranne i test
    di auth dedicati, **dati seedati deterministici** (I), isolamento per test, **retry limitati** (≈2), **trace/screenshot/
    video on-failure** come artifact. Un flaky va quarantenato e sistemato, non mascherato con retry.
20. **Visual regression SÌ ma non pixel-perfect**, due reti:
    - **aria/DOM snapshot** (`toMatchAriaSnapshot`) = rete **primaria** semantica (diff testuale leggibile, indipendente
      dai pixel; un cambiamento strutturale voluto si vede come nodo aggiunto/rimosso);
    - **pixel snapshot tollerante** (soglie `maxDiffPixelRatio`/`threshold`) su **poche schermate chiave**, **baseline
      generate nell'ambiente CI** (immagine Docker Playwright) per evitare falsi diff da font/OS.
21. **Gestione baseline = a carico dell'agente durante l'implementazione**, con la regola **"mai aggiornare una baseline
    alla cieca per far passare i test"**: si aggiorna solo per cambiamenti **intenzionali e verificati**; un fallimento
    inatteso si **indaga**. Risultato: **zero approvazioni manuali di screenshot**; l'utente fa solo la review della PR.
    (Da encodare nelle skill `new-change`/`new-application`.) Visual-AI SaaS (Applitools/Percy/Chromatic) = evoluzione E17.
22. **Multi-browser** (WebKit/Firefox) = evoluzione E18.

### G. Contract testing / OpenAPI
23. **OpenAPI = unica fonte di verità**: spec generato da Quarkus e **committato**.
24. **Conformità lato BE**: gli integration test **validano le risposte contro lo schema OpenAPI** (es.
    swagger-request-validator) → l'implementazione rispetta il contratto.
25. **Conformità lato FE**: client TS **rigenerato** dallo spec + **`tsc`** → un'incompatibilità rompe la **build** del FE.
26. **Drift detection in CI**: rigenera spec+client e fallisce se divergono dal committato.
27. **`oasdiff` BLOCCANTE** sui breaking change, con **messaggi d'errore chiari** (cosa si rompe, per chi, come dichiararlo
    intenzionale/versionare). Pact/consumer-driven = evoluzione E19 (se FE/BE diventano team/repo separati).

### H. Infra testing (Terraform)
28. **`terraform fmt -check` + `validate`** + **`tflint`** + **`checkov`** (security/misconfig). Le scelte cost-min che i
    tool segnalano (es. subnet pubbliche/no-NAT #06 B) si gestiscono con **soppressioni inline documentate** (motivazione
    + link all'evoluzione, es. E1) → scan verde ma eccezione auditabile.
29. **`terraform test`** sul **modulo `microsaas_app`** (mattone centrale): dato un input, il plan produce le risorse attese.
30. **Infracost sulle PR**: commenta il **delta di costo mensile** dell'infra → guardiano cost-min a build-time
    (complementare all'AWS Budgets alarm di #08, che scatta a costo già speso). Test che applicano infra reale
    (terratest) = evoluzione E20.

### I. Test data, fixtures & seed
31. **Fixture self-contained** per integration/security: create/distrutte nel test (DB effimero), via **test data
    builders** fluenti (`aTenant().withOwner().withApp(...)`) → leggibili e robusti ai cambi di schema.
32. **Seed deterministico, idempotente, versionato** (codice nel repo) con **ID stabili** (UUID fissi) per dev locale +
    E2E: cast fisso **≥2 tenant** (Acme B2B owner/admin/member, Bob B2C single-user) + **platform-admin** + app in vari
    stati (attiva/trial). Lo scaffold `new-application` genera il seed-base per la nuova app.
33. **Dati 100% sintetici** (no PII, email `*.test`) — coerente con #08/#13. **Un unico set condiviso** dev-locale↔E2E (#11).

### J. Test in CI & gate di merge (allineato a #07)
34. **Su PR (bloccante per il merge) — scelta j-a: SUITE COMPLETA, E2E inclusi**, con **path-filtering** (tocchi solo il
    FE → non rilanci infra, ecc.). Backend (unit+integration+security+ArchUnit), FE (component + **E2E**), contract
    (conformità+drift+tsc+**oasdiff**), infra (fmt/validate/tflint/checkov/terraform test/**Infracost**+plan).
35. **Regole non negoziabili**: la **suite security/multi-tenancy (D) gira SEMPRE** (mai esclusa dal path-filter) ed è
    bloccante; **`oasdiff` ed E2E sono bloccanti**.
36. **Coverage = riportata, NON bloccante** (no hard limit, A): la CI commenta la copertura (JaCoCo aggregata + FE) e
    segnala se sotto il target soft 80–85%, ma non blocca. Bloccano i **test rossi**.
37. **Al merge su `main`** (→ deploy test): opzionale **smoke E2E contro l'ambiente test** deployato (problemi
    env-specifici). **Al tag → prod**: gate = immagine native + approvazione (#07 H); si tagga solo un commit verde.

### K. Performance/load & accessibilità
38. **Load testing rimandato** (architettura scale-to-0/1-task in PoC → un load test ora non sarebbe rappresentativo e
    accenderebbe risorse): **k6** come evoluzione **E16** verso il go-live/SLA. **Ora** solo **misura/documentazione del
    cold-start** (Aurora scale-to-0 ~10-15s + risveglio Fargate), coerente con la taratura readiness di #08 G.
39. **Accessibilità: axe-core automatico da subito** — `jest-axe`/axe in Vitest sui componenti del design-system +
    `@axe-core/playwright` sulle schermate chiave; sfrutta Radix (accessibile by-design) e gli aria-snapshot (F).
    Trova le violazioni comuni senza lavoro manuale. **Audit manuale completo** (screen reader, WCAG) = pre-go-live/#13
    (rilevanza European Accessibility Act, in vigore da giugno 2025).

### L. Test del flusso pagamenti (Paddle) — rimando a #09 D20
40. La **strategia di test dei pagamenti** è definita in [09-pagamenti](09-pagamenti.md) dec.20, a **3 livelli**, coerente
    con questa filosofia: **L1** integration esaustivo del processing webhook (payload sintetici firmati, Testcontainers,
    per-PR **bloccante**); **L2** E2E Playwright dei nostri pezzi con **Paddle.js mockato** (per-PR bloccante); **L3** smoke
    reale su **Paddle sandbox** (**pre-release**, override manuale se sandbox down). Principio: **non si guida l'iframe
    Paddle** con Playwright (si mocka il confine). Lo stub Paddle locale (#11) abilita L1/L2 offline.

## Questioni aperte
_Nessuna — #10 chiuso._ Dipendenze: lo scaffold `new-application` deve generare unit/integration/security/E2E/seed di
base e encodare la regola "mai update baseline alla cieca"; lo stack locale offline (#11) è prerequisito degli E2E.

## Alternative valutate / scartate
- **Coverage gate rigido al X% su tutto** — scartato (porta a test inutili); soft 80–85% sul core, non bloccante.
- **Pixel-perfect visual regression** — scartato (fragile, approvazioni continue) → aria-snapshot + pixel tollerante con baseline gestite dall'agente.
- **Pact/consumer-driven contract** ora — rimandato (E19): in monorepo con client generato il giro spec+tsc+oasdiff basta.
- **Terratest (infra reale)** ora — rimandato (E20): `plan`+`terraform test` bastano senza accendere risorse.
- **Load testing** ora — rimandato (E16): non rappresentativo nella config scale-to-0 del PoC.
- **Postgres condiviso con schema dedicato ai test** — scartato a favore di Testcontainers effimeri (isolamento totale).

## Impatti su altre aree
- [04-services-backend](04-services-backend.md), [03-frontend](03-frontend.md), [02-auth-sicurezza](02-auth-sicurezza.md),
  [05-persistenza-dati](05-persistenza-dati.md), [06-infra-iac](06-infra-iac.md), [07-devops-cicd](07-devops-cicd.md),
  [08-observability](08-observability.md), [11-developer-experience](11-developer-experience.md),
  [_EVOLUZIONI-DEVOPS.md](_EVOLUZIONI-DEVOPS.md), [_COSTI-AWS.md](_COSTI-AWS.md), [_BACKLOG.md](_BACKLOG.md)
