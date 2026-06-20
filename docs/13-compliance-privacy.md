# Compliance & Privacy (GDPR) — Decisioni

**Stato**: 🟢 deciso (A–L)
**Ultimo aggiornamento**: 2026-06-20

> ⚠️ **Disclaimer**: questo documento NON è parere legale. Tutte le decisioni (postura, retention, ruoli, testi) sono
> prese **internamente** in questa fase. È prevista **un'unica revisione legale pre-go-live (consigliata, opzionale)**
> — vedi "Revisione legale pre-go-live" in fondo — limitata ai documenti vincolanti (DPA, privacy policy, ToS) e ai
> casi-limite. Nessun blocco prima del go-live (fase locale/test = nessun utente reale).

## Scope
Trattamento dei dati personali conforme a GDPR (e norme collegate), lato **tecnico** (cosa deve poter fare il sistema)
e **documentale** (policy, registri). Si appoggia a: soft-delete/erasure (#05), purge per-tenant EventBridge (#06 H),
retention log/audit + no-PII nei log (#08), Paddle MoR (#09), accessibilità (#10 K).

## Agenda
- **A. Ruoli & perimetro** (titolare/responsabile) — 🟢 deciso
- **B. Basi giuridiche & finalità** — 🟢 deciso
- **C. Data mapping / Registro trattamenti (Art. 30)** — 🟢 deciso
- **D. Diritti degli interessati** (accesso, rettifica, oblio, portabilità, opposizione) — 🟢 deciso
- **E. Data retention** (politica complessiva) — 🟢 deciso
- **F. Consenso, cookie & tracking** — 🟢 deciso
- **G. Privacy Policy & T&C** (minimizzazione informativa; ripartizione con Paddle) — 🟢 deciso
- **H. Sub-responsabili & DPA** — 🟢 deciso
- **I. Data residency & trasferimenti** — 🟢 deciso
- **J. Data breach** (notifica 72h) — 🟢 deciso
- **K. Privacy by design/default & DPIA** — 🟢 deciso
- **L. Funzionalità GDPR nelle app** (export/erasure per-app) — 🟢 deciso

## Decisioni prese

### A. Ruoli & perimetro — postura uniforme "no classificazione utenti"
1. **Non si classificano gli utenti** (personale vs professionale): è ingestibile e il ruolo GDPR non dipende dalla forma
   dell'app né dal tipo di utente. Il "modello B2B" (multi-utente, pricing a tier) è una **feature di prodotto**, NON un
   ruolo giuridico (es. un capofamiglia che invita 3 familiari su un'app "B2B" → resta uso domestico).
2. **Si distingue per TIPO DI DATO, non per tipo di utente:**
   - **Dati di piattaforma** (account/email/auth/2FA, membership/ruoli, billing, log/audit) → **appgrove = TITOLARE**
     sempre, per tutti. Una sola privacy policy.
   - **Contenuti dentro le app** (ciò che l'utente immette) → postura **uniforme**: *"i dati sono dell'utente; appgrove
     li tratta per suo conto, solo per erogare il servizio, nessun uso secondario, nessuna monetizzazione"*.
3. **DPA incorporato nei termini standard** (allegato auto-applicabile): il cliente **professionale** (titolare) che ha
   bisogno del DPA **ce l'ha già**; per il **consumatore** è inerte. **Nessuna selezione/classificazione** da parte di appgrove.
4. **Costruire allo standard più rigoroso** (export/erasure/rights per OGNI utente, no uso secondario): così l'etichetta
   "titolare vs responsabile" sui contenuti diventa **documentale**, e il comportamento del sistema soddisfa entrambe le
   interpretazioni. È il principio che rende la postura serena senza decidere caso per caso.
5. **Household exemption (art. 2(2)(c))**: esenta la **persona fisica** per uso personale/domestico → quell'utente **non è
   titolare**; ma l'esenzione **NON si estende ad appgrove** (giurisprudenza *Ryneš*) → per i contenuti dei **consumatori**
   appgrove è **titolare** (non responsabile). **Impatto sull'impianto tecnico = nullo**: privacy policy appgrove-facing e
   gestione diritti diretta erano già previste; cambia solo la **dicitura documentale**. Conseguenza vincolante: per i
   consumatori **non** si possono scaricare gli obblighi su un "cliente-titolare" (inesistente) → li gestisce appgrove.
6. **Utenti invitati** (es. familiari/colleghi) = interessati a pieno titolo → informati dalla **stessa** policy, con i
   **loro** diritti (gestiti dalla tooling rights, D/L).
7. **Paddle MoR**: per **pagamento/fiscale** Paddle è titolare/contitolare (gestisce lui tax/fatturazione); appgrove resta
   titolare della **relazione di servizio** (account/uso). Dettaglio in G/H/#09.

### B. Basi giuridiche & finalità
8. **Mappatura basi/finalità** (postura "service-only, no monetizzazione"):

   | Trattamento | Finalità | Base giuridica |
   |---|---|---|
   | Account/auth (email, password/Cognito, 2FA, profilo) | Creare/gestire account, autenticare | **Contratto** (6.1.b) |
   | Membership/ruoli, entitlement | Erogare app attivate, permessi | **Contratto** (6.1.b) |
   | Contenuti nelle app | Far funzionare l'app richiesta | **Contratto** (6.1.b) |
   | Billing/abbonamento (lato appgrove) | Gestire la sottoscrizione | **Contratto** (6.1.b) |
   | Dati pagamento/fiscali | Pagamento, fatturazione, tax | **Obbligo legale**+contratto, **in capo a Paddle (MoR)** |
   | Log tecnici & audit sicurezza | Stabilità, sicurezza, prevenzione abusi, prova | **Legittimo interesse** (6.1.f) |
   | Errori frontend (#08 H) | Diagnostica/stabilità | **Legittimo interesse** (6.1.f) |
   | Email transazionali (verifica/reset/invito/reminder) | Erogare il servizio | **Contratto** (6.1.b) |
   | **Product analytics aggregato** (uso app/funzioni) | Migliorare/espandere l'offerta | **Legittimo interesse** (6.1.f) |
   | **Newsletter** | Marketing diretto | **Consenso** (6.1.a) |
   | **Web analytics sito vetrina** (Plausible Cloud, EU) | Misurare uso del vetrina | cookieless → **legittimo interesse**/nessun consenso |

9. **Limitazione di finalità (art. 5.1.b)**: i dati raccolti per il servizio **non** si riusano per altri fini → il
   "no monetizzazione" diventa vincolo formale documentato.
10. **Newsletter = consenso double opt-in**, **separata** dalle email transazionali (che restano su base contrattuale),
    disiscrizione facile in ogni email, **registrazione della prova del consenso**. (Meccaniche → F.)
11. **Analytics = Plausible Cloud (cookieless privacy-first, EU-hosted Hetzner, €9/mese)** sul **solo sito vetrina**;
    **app loggata = ZERO tracking** (solo cookie tecnici). Niente cookie/PII → **nessun consent banner** (citazione in
    policy per trasparenza, G). **Scelta purista**: EU-hosted → nessun trasferimento USA (vs Cloudflare US, scartato). GA
    scartato (banner + trasferimento USA). Self-host Plausible CE scartato (su AWS ~$30/mese + ClickHouse stateful
    anti-pattern; su VPS UE ~€5 ma +ops → il Cloud €9 vince sul TCO). Setup una-tantum (account + snippet nel vetrina),
    quando si attiva il vetrina.
12. **Product analytics SOLO aggregato/non identificativo** via **metriche EMF** (#08 B, dimensioni `app_id`/`endpoint`/
    `feature`) + dashboard CloudWatch (#08 D) → "quali app/funzioni più usate". **Vietato il data-mining dei log
    per-utente** per questo scopo (limitazione di finalità: i log sono raccolti per sicurezza/stabilità). Disciplina:
    evitare spaccati che **isolino un singolo** tenant (a basso volume = identificabile). **Profilazione per-utente** =
    fuori scope, richiederebbe valutazione/consenso dedicati (eventuale futuro).

### C. Data mapping / Registro dei trattamenti (Art. 30)
13. **Va mantenuto**: l'esenzione <250 dipendenti non si applica (trattamento non occasionale). Due registri: come
    **titolare** (piattaforma + consumatori) e come **responsabile** (per i tenant B2B titolari).
14. **RoPA versionato nel repo, in DUE FILE separati per lingua**: `docs/compliance/ropa.it.md` e `docs/compliance/ropa.en.md`
    (IT per il Garante; EN per crescita/altri DPA UE). Sezione piattaforma stabile + sezione per-app. **Entrambi i file
    sono GENERATI/assemblati dalla fonte unica** — i **manifesti-dati per-app bilingui** + sezione piattaforma bilingue →
    **zero drift** tra le lingue. **Check CI**: le due lingue devono avere lo **stesso set di voci** (traduzione mancante
    → segnala). Il manifesto per-app va quindi compilato in IT+EN (richiesto da `new-application`/`new-change`).
15. **Manifesto "data processing" per-app = fonte unica**: ogni app dichiara categorie di dati personali, finalità,
    base giuridica, retention, ubicazione (`app_<id>`). **Alimenta sia il RoPA sia il tool di export/erasure** (#13 D/L)
    → un'unica dichiarazione, due usi. Reso **obbligatorio dalla skill `new-application`** (RoPA nasce completo).
16. **Privacy/RoPA gate nel workflow `new-change`** (privacy-by-design, art. 25): ogni change che tocca dati personali
    viene **intercettata e applicata al RoPA contestualmente**. Meccanica:
    - **rilevamento segnali**: migrazioni Flyway con nuove colonne/tabelle, nuovi campi entità/DTO/API, nuove
      integrazioni esterne (potenziale sub-responsabile → H), modifiche a retention/finalità;
    - **classificazione assistita (co-pilota, non checklist passiva)**: la skill **ragiona con l'utente** per arrivare
      alla classificazione — elicita lo scopo del campo (*"spiegami cos'è e a cosa serve"*), **deduce e propone**
      natura/finalità/base giuridica/retention **con motivazione**, pone **domande di approfondimento solo se il caso è
      ambiguo** (es. "il telefono serve alla feature o per ricontatto commerciale? cambia la base"), **ragiona insieme
      sulla retention**, poi **propone e fa confermare** ("finalità=X, base=Y, retention=Z, categoria=ordinaria — ti
      torna?") → aggiorna manifesto + RoPA. Resta sotto disclaimer (assiste fino a una bozza solida; validazione = legale);
    - **escalation categorie particolari (art. 9)** (salute, biometrici, ecc.): avviso **forte** + DPIA (#13 K) + base
      rafforzata; non si procede "in sordina";
    - **enforcement CI (bloccante)**: un campo marcato dato personale (es. annotazione `@PersonalData`/tag) **non
      dichiarato nel manifesto** fa fallire la build (stile ArchUnit #10 D). Implementazione fine da definire
      (annotazione+lint/ArchUnit o derivazione dalle migrazioni).
17. **RoPA = documento INTERNO, non pubblico** (art. 30.4): si mette a disposizione **solo dell'autorità (Garante) su
    richiesta**; utenti/clienti non hanno diritto di vederlo. Da non confondere con la **Privacy Policy/informativa**
    (art. 13-14), che è **pubblica** e va data agli interessati (→ G). Pubblici diversi → **dettaglio diverso**: il RoPA
    (solo per il Garante) può essere **tecnicamente dettagliato**; la privacy policy pubblica è **minimizzata** (cosa/
    perché/quanto, niente dump dell'architettura). Quindi manifesto-dati + RoPA restano **dietro le quinte** (repo/uso
    interno); verso l'utente esce solo la privacy policy.
18. **Checklist per voce del registro**: categorie interessati · categorie dati · finalità · base giuridica (B) ·
    destinatari/sub-responsabili (H) · trasferimenti extra-UE + garanzie (I) · retention (E/#08) · misure di sicurezza
    (cifratura, isolamento tenant, least-privilege — #02/#05/#06).

### D. Diritti degli interessati (artt. 15–22)
19. **Self-service first**: i diritti sono costruiti **dentro il prodotto** (UI profilo/impostazioni), alimentati dal
    **manifesto dati per-app** → l'utente li esercita da solo, subito. Mappatura:
    - **Accesso (15)**: "I miei dati" + **download export**. **Rettifica (16)**: modifica in UI.
    - **Cancellazione (17)**: self-service "elimina account" (totale) **o** "recedi da app" (per-app: esporta→conferma→
      cancella). Eccezioni legali → quasi solo fiscale, in capo a **Paddle**.
    - **Portabilità (20)**: export in formato strutturato (JSON/CSV) — stesso motore.
    - **Opposizione (21)**: marketing/newsletter = **unsubscribe** immediato; legittimo interesse (es. analytics
      aggregato) = canale per opporsi, caso per caso.
    - **Limitazione (18)**: **manuale** (flag/sospensione) per ora.
    - **Decisioni automatizzate (22)**: **N/A dichiarato** (nessuna profilazione/decisione automatizzata; se introdotta, si rivede).
20. **Chi gestisce (ruoli A)**: piattaforma + consumatori → appgrove **direttamente**; dati app **B2B** (appgrove
    responsabile) → la richiesta va al **tenant-titolare**, appgrove **assiste** fornendo **tooling admin** (export/
    cancellazione dei dati dei propri utenti).
21. **Fallback manuale = TICKETING NATIVO IN-HOUSE** (scelta purista: niente Jira/Atlassian né help-desk SaaS → zero
    sub-processor, PII in casa, EU). Entità `support_ticket` nel DB piattaforma + **vista nella console admin**. **Esteso
    al supporto generico**, non solo privacy: i **ticket privacy sono un tipo speciale** (SLA legale 1 mese, auto-creati
    da eventi), accanto al supporto best-effort. **MVP minimale, NO allegati per ora**. Modello: tipo/priorità/stato,
    `tenant_id`/`user_id`, oggetto, **thread di messaggi** (utente↔admin), **notifiche email via SES**. **Trigger
    auto-creazione**: form in-app, email a `privacy@`/`support@` via **SES→Lambda**, eventi di sistema (export FAILED
    #13 D, escalation gate art. 9 #13 C). **Verifica identità** implicita (utente autenticato). **Log richieste**
    (accountability #08). È **capability di piattaforma** (#04 core + #03 backoffice/admin) → use case dedicato.
22. **Export asincrono (architettura)** — riusato sia per accesso/portabilità sia per export-prima-di-cancellazione:
    1) richiesta → record **export job** (id, tipo/app, `requested_at`, `status` QUEUED→RUNNING→COMPLETED/FAILED,
       `progress` step X/N);
    2) **worker async (SQS)** esegue gli **step di export dichiarati dal contratto per-app** (#13 C/L) aggiornando il
       progress (app con più tabelle → più step/più tempo); l'orchestratore aggrega;
    3) al completamento → **ZIP su bucket S3 dedicato** (privato, **SSE cifrato**, **lifecycle auto-delete 7gg**) +
       **presigned URL 7gg**; UI mostra **link + data/ora scadenza**;
    4) **link solo in-app** all'utente autenticato (non via email). Refresh UI = polling dello stato/progress.
    Meccanica UX di dettaglio → use-case GDPR (backlog).

### E. Data retention (politica complessiva)
23. **Quadro di retention per categoria** (principio: minimizzazione art. 5.1.e — niente "per sempre"):

    | Categoria | Retention | Meccanismo |
    |---|---|---|
    | Account/profilo (attivo) | finché attivo | base contratto; cancellato su richiesta (D) |
    | Dati app (attivi) | finché l'app è attiva per il tenant | recesso per-app = cancellazione immediata post-export (D) |
    | Backup / PITR | **7 giorni** (#06) | dati cancellati spariscono entro il ciclo backup (dichiarato in policy) |
    | Export ZIP (S3) | **7 giorni** auto-delete | lifecycle S3 + presigned 7gg (#13 D) |
    | Log applicativi/operativi | test **7gg** / prod **30gg** (#08) | poi scadono |
    | Log audit/sicurezza | **12 mesi** | archivio S3/Glacier (#08); deciso internamente (copertura forense; detection incidenti ~200gg) |
    | Ticket privacy (Jira) | **24 mesi** | prova evasione richieste (accountability/difesa), PII minimizzati |
    | Consenso newsletter / prova | iscritto + **24 mesi** post-unsubscribe | prova consenso/revoca |
    | Dati fiscali/pagamento | **in capo a Paddle (MoR)** | non conservati da appgrove |

24. **Retention per-categoria, dichiarata e applicata as-code**: log via Terraform (#08), S3 via lifecycle, DB via purge
    job/EventBridge (#06). Il **manifesto per-app** dichiara la retention dei dati di quell'app (→ RoPA).
25. **(E.1) Grace sulla cancellazione TOTALE account = 14 giorni**: account **disattivato subito** (inaccessibile) +
    **hard-purge dopo 14gg**, **annullabile** entro il periodo (tutela da errore/frode; GDPR-ok perché l'accesso è già
    revocato). Il **recesso per-app resta immediato** (post-export, D).
26. **(E.2) Auto-cancellazione account inattivi = 24 mesi**: dopo 24 mesi di inattività → **email di avviso** → nessuna
    risposta in 30gg → cancellazione (minimizzazione, no accumulo di dati di utenti spariti).

### F. Consenso, cookie & tracking
27. **Inventario cookie**: solo **tecnici essenziali** (refresh token HttpOnly #02, eventuale CSRF/config); **Plausible
    (cookieless/aggregato, EU)** = trattato come tecnico (linee guida Garante/CNIL/ICO); app loggata = **zero tracking**.
    → **Nessun cookie consent banner ora, da nessuna parte.**
28. **Disclosure cookie tecnici** nella privacy/cookie policy (trasparenza, non un banner) → G.
29. **Cookie-consent ≠ newsletter-consent: NON si uniscono** (anti-pattern art. 7 "specifico/non impacchettato"; e nel
    banner l'utente è anonimo, senza email). Newsletter (consenso 6.1.a) si raccoglie dove c'è l'email:
    **subscribe box** sul vetrina + **checkbox non pre-spuntata al signup** + **toggle in impostazioni account**;
    **double opt-in** + **disiscrizione facile**.
30. **Consent log**: ogni consenso raccolto registrato con **prova** (chi/quando/testo-versione/come), art. 7.
31. **Centro preferenze consensi** in impostazioni account = **hub unico** per il loggato (newsletter on/off, futuri
    opt-in) con **revoca facile**. Diverso dal cookie banner (che serve all'anonimo alla prima visita).
32. **Future-proofing con due strumenti distinti**: **consent gate** nel frontend (hook a costo ~0: ogni script non
    essenziale passa da un check di consenso) per **tracker futuri**; **consent log** per newsletter/altri consensi. Se
    in futuro si introducono tracker non essenziali (GA, RUM #08 H/E14, pixel) → **banner + CMP** con opt-in granulare e
    **blocco preventivo** (evoluzione; aggiungere "Cookie policy/CMP" alla revisione legale).
33. **Tutto srotolato negli use case** (`docs/usecases/` GDPR): subscribe box, checkbox signup, toggle/centro preferenze
    in account, disclosure cookie, eventuale CMP → flussi/schermate in fase di dettaglio UX.

### G. Privacy Policy & Terms & Conditions
34. **Set documenti pubblici**: **Privacy Policy** (art. 13-14), **ToS**, **DPA** (incorporato nei ToS, A §3),
    **disclosure cookie** (sezione della privacy policy, F — niente banner/documento separato).
35. **Privacy policy a due livelli** (come il RoPA): sezione **piattaforma** (md a mano) + sezioni **per-app** generate
    dal **manifesto dati**. Il manifesto = **fonte unica per TRE output**: RoPA (interno dettagliato), tool export/erasure,
    **snippet privacy pubblico minimizzato**. `new-application` genera anche lo snippet pubblico → policy sempre allineata.
36. **Minimizzazione nel testo pubblico** (cosa/perché/quanto; diritti) — niente dettagli architetturali (quelli solo nel RoPA).
37. **Contenuti come Markdown = fonte unica** per **sito vetrina statico** + rendering **in-app** (stessi file `.md`,
    versionati in git). Sezioni per-app assemblate in md dai manifesti.
38. **Multilingua**: sito vetrina statico + **privacy policy & ToS in 5 lingue: EN, IT, FR, ES, DE**. **Versione facente
    fede = IT** (clausola "in conflitto prevale l'italiano"); **FR/ES/DE = traduzioni fedeli** (generabili, poi revisione).
    **RoPA interno resta IT+EN** (non pubblico). **Check CI**: ogni componente presente in tutte le 5 lingue.
39. **Contenuto privacy policy (checklist art. 13)**: identità+contatti **titolare**, finalità+basi (B), destinatari/
    sub-responsabili (H), trasferimenti extra-UE+garanzie (I), retention (E), **diritti e come esercitarli** (D, `privacy@`),
    **diritto di reclamo al Garante**, fonte dati per gli **invitati**, **art. 22 = nessuna decisione automatizzata**.
40. **ToS riflette Paddle MoR**: Paddle = Merchant of Record (vende lui, gestisce tax/fatturazione/rimborsi); appgrove
    eroga il servizio; i ToS richiamano i termini Paddle per il pagamento (→ #09).
41. **Accettazione, versioning per-componente, scoping** (privacy-by-design):
    - **versioni indipendenti per componente** (piattaforma + ogni app): l'accettazione utente è un insieme (es.
      "piattaforma v3 + app-A v1 + app-B v2");
    - **accettazione contestuale**: termini **piattaforma** al **signup**; termini di un'**app** alla sua **attivazione**;
    - **aggiungere una nuova app al catalogo NON forza riapprovazione** a chi non la usa (nessuna schermata bloccante
      globale); la sezione dell'app si accetta solo all'attivazione;
    - **schermata bloccante al login solo per cambi SOSTANZIALI** su un componente **a cui l'utente è già vincolato**, e
      **solo per gli utenti interessati**: ToS/contratto o consenso → **ri-accettazione/re-consenso**; aggiornamento
      puramente **informativo** → **notifica non bloccante**;
    - **versioning leggibile**: ogni componente ha **versione + `effective_date`** nel front-matter del md, **git-backed**
      (commit/tag = audit immutabile); il **log di accettazione** registra **componente+versione(+commit hash)** per utente.
      Bump **major** = ri-accettazione; bump **minor** = notifica.
42. **Redazione testi = deliverable, non ora**: redatti internamente allo stato dell'arte (come i template email) →
    **revisione legale pre-go-live** ([_REVISIONE-LEGALE](_REVISIONE-LEGALE.md) L1-L3).
43. **Prerequisito business**: la privacy policy richiede l'**identità del titolare = entità legale** (ditta/società)
    con indirizzo/contatto; serve anche a Paddle (MoR). Non blocca ora; prerequisito pre-go-live (in _REVISIONE-LEGALE).
44. **Tutto srotolato negli use case** (`docs/usecases/` GDPR): schermate di accettazione/versioning/re-consenso, hosting
    md, rendering multilingua → fase di dettaglio UX.

### H. Sub-responsabili (sub-processor) & DPA
45. **Inventario sub-processor** (ridotto dalla scelta purista): **AWS** (hosting/compute/DB/storage/SES, eu-west-1 +
    eu-central-1, UE), **Plausible** (analytics vetrina, **EU-hosted Hetzner**, cookieless). **Atlassian e Cloudflare
    RIMOSSI** (ticketing → in-house; analytics → Plausible EU). **Paddle = ruolo a sé** (Merchant of Record, titolare/
    indipendente per pagamento/fiscale), **non** sub-processor classico. Trasferimenti → I.
46. **Lista sub-processor pubblica e viva** in `content/subprocessors.md` (md, EN/IT min.): nome, finalità, regione,
    categorie dati; **linkata** da privacy policy e DPA.
47. **DPA con ciascun sub-processor**: adesione ai **DPA standard** dei fornitori (AWS, Plausible), una-tantum
    pre-go-live (L9 in [_REVISIONE-LEGALE](_REVISIONE-LEGALE.md)).
48. **DPA verso i clienti** (incorporato nei ToS, A §3): uso di sub-processor, **notifica cambi**, **diritto di
    opposizione**, sicurezza, assistenza breach/diritti, cancellazione/restituzione dati a fine rapporto.
49. **Processo cambio sub-processor**: aggiorni la lista + **notifichi i clienti** con **preavviso 30 giorni** e finestra
    di **opposizione**. Il **gate privacy di `new-change`** (#13 C) rileva le nuove integrazioni esterne → segnala
    "potenziale nuovo sub-processor" → innesca aggiornamento lista + notifica.

### Struttura contenuti: `content/` (pubblico) vs `docs/` (interno)
50. **Separazione netta** (vale per tutti gli md legali/pubblici):
    - **`content/`** (nuova): **md pubblici multilingua** (EN/IT/FR/ES/DE) consumati **sia dal sito statico sia
      dall'app** — `content/legal/` (privacy, terms, cookie), `content/subprocessors.md`, `content/marketing/…`.
      È il pattern "file nel repo = contenuto del sito (e dell'app)".
    - **`docs/`**: **interno, mai pubblicato** (decisioni #NN, `_*`, **RoPA**). Il sito legge **solo `content/`**.
    - **Fonte unica per-app**: il manifesto genera lo **snippet pubblico** in `content/legal/` **e** la voce dettagliata
      nel **RoPA interno** (`docs/`). Stessa sorgente, due destinazioni.
    - **Versioning**: git (storia completa) + `version`/`effective_date` nel front-matter dei legali pubblici (G §41) +
      log di accettazione con commit hash.

### I. Data residency & trasferimenti extra-UE
51. **Residency UE by design**: tutti i dati personali a riposo **solo in regioni UE** — **eu-west-1 (Irlanda)** +
    **eu-central-1 (Francoforte)** monitoring (#06/#08); Cognito/Aurora/S3/SES in regioni UE.
52. **Postura PURISTA scelta**: minimizzare i fornitori extra-UE. Conseguenze già applicate: **analytics → Plausible
    (EU)** invece di Cloudflare (US); **ticketing → in-house** invece di Jira/Atlassian (US). Restano:
    - **AWS**: dati **a riposo in UE**, ma AWS Inc. è USA (CLOUD Act) → garanzie **DPF + SCC** (DPA AWS) + cifratura;
    - **Plausible**: **EU-hosted (Hetzner, Germania)**, società UE → **nessun trasferimento extra-UE**;
    - **Paddle**: ruolo MoR/titolare per pagamento, sue garanzie (DPF/SCC);
    - (eventuale futuro) **API LLM/AI o altri servizi esterni** in una app → **nuovo trasferimento**, intercettato dal
      gate `new-change` (#13 C) → valutazione garanzia prima del merge.
53. **Garanzie**: dove resta un fornitore con casa-madre USA (AWS), ci si appoggia a **EU-US DPF** + **SCC** (nei DPA) +
    **TIA leggero** (Transfer Impact Assessment) come nota di accountability (→ [_REVISIONE-LEGALE](_REVISIONE-LEGALE.md)).
54. **Documentazione**: sezione "trasferimenti" nella privacy policy (art. 13: destinatari, paesi, garanzia).
55. La scelta purista **riduce drasticamente** la superficie di trasferimento: di fatto solo **AWS** (UE-region, parent
    USA) + Paddle (pagamenti). Tutto il resto è UE/in-house.

### J. Data breach (violazione dei dati)
56. **Runbook di Incident Response interno** (`docs/compliance/breach-runbook.md`): **detect → assess → contain →
    notify → document**, con **timeline 72h** e l'albero delle soglie (sotto). Pronto in anticipo (le 72h partono da
    "quando vieni a conoscenza").
57. **Albero delle soglie** (basato sul rischio per gli interessati):
    - **rischio improbabile** → niente notifica; **solo registro** + motivazione del "no-rischio";
    - **rischio (non elevato)** → **Garante entro 72h** (art. 33) + registro;
    - **rischio ELEVATO** → **Garante (72h) + interessati** "senza ingiustificato ritardo" (art. 34) + registro;
    - criteri (EDPB): tipo violazione, **natura/sensibilità/volume** (art. 9 = +rischio), identificabilità, **gravità
      conseguenze**, vulnerabili, numero interessati.
58. **Leva cifratura (art. 34.3)**: dati **cifrati/inintelligibili** → spesso **niente notifica agli interessati** (e può
    rendere "improbabile il rischio"). Le misure di **encryption at rest + in transit ovunque** (#06 §20bis) **riducono
    direttamente** l'obbligo di notifica.
59. **Detection via #08** (allarmi anomalie, audit, error tracking) + scoperta esterna; **scoping rapido** via log
    strutturati/audit (#08) + isolamento per-tenant + **manifesto dati** (#13 C) → chi/cosa colpito.
60. **Registro breach interno** (art. 33.5, **obbligatorio**): registra **TUTTE** le violazioni (anche non notificate)
    con fatti/effetti/azioni correttive. In `docs/` (interno, come il RoPA).
61. **Notifiche per ruolo (A)**: **titolare** (piattaforma/consumatori) → Garante/interessati; **responsabile** (dati app
    B2B) → **notifichi il tenant-titolare senza ritardo** (lui notifica autorità/interessati), tracciato nel ticketing in-house.
62. **Template pronti** (Garante, interessati, controller B2B) IT/EN = **deliverable pre-go-live** → [_REVISIONE-LEGALE](_REVISIONE-LEGALE.md).
63. **Responsible disclosure**: **`security@appgrove.app`** + **`security.txt`** sul sito per le segnalazioni di vulnerabilità → alimenta il processo.
64. **Skill `breach-response`** (da creare): co-pilota che, dato l'incidente, **guida la valutazione del rischio**
    (albero §57), decide notifica/non-notifica, **redige la voce del registro** e i **draft delle notifiche** (Garante/
    interessati/controller, IT/EN). Stesso spirito del gate privacy. → backlog skill.

### K. Privacy by design/default (art. 25) & DPIA (art. 35)
65. **Privacy by DESIGN = già incorporata** (consolidamento, accountability): isolamento per-tenant (#1/#2), **encryption
    ovunque** (#06 §20bis), no-PII nei log (#08), minimizzazione (B/E), **residency UE + fornitori UE/in-house** (I),
    RoPA+manifesto generati (C), **gate privacy in `new-change`** (C), diritti self-service (D), ticketing in-house (I),
    consenso opt-in (F). Non è un'aggiunta: è costruita nel sistema.
66. **Privacy by DEFAULT (principio)**: impostazioni predefinite **più protettive** — newsletter opt-in (checkbox non
    pre-spuntata), **nessun tracking** di default, 2FA opt-in, **minimo dato** raccolto, nessun consenso pre-attivato,
    retention minimizzata. Regola: *ogni nuova feature nasce con la minor raccolta/condivisione possibile*.
67. **DPIA a screening**: baseline attuale (service-only, no profilazione, no categorie particolari, dati minimizzati) →
    **DPIA NON richiesta ora** (motivazione dello screening documentata). **Screening per-change/per-app** innescato dal
    **gate `new-change`** (estende l'escalation art. 9 di C): criteri art. 35 + 9 criteri EDPB (monitoraggio sistematico
    su larga scala, art. 9 su larga scala, profilazione con effetti significativi) → se supera soglia, si **conduce la
    DPIA** (rischi+mitigazioni; eventuale **consultazione preventiva** Garante se rischio residuo alto).
68. **DPO (art. 37) NON obbligatorio ora** (scope non = monitoraggio/categorie particolari su larga scala) → basta un
    **contatto privacy** (`privacy@`). Rivalutare con la crescita.

### L. Funzionalità GDPR nelle app (contratto per-app)
69. **Contratto GDPR per-app (interfaccia obbligatoria)**: ogni app del marketplace implementa **`exportData(scope)`**
    (alimenta l'export job D, dichiarando i propri step per il progress) e **`purgeData(scope)`** (cancellazione completa);
    `scope` = per-tenant o per-utente. Il **manifesto dati** (C) è la dichiarazione collegata. **Rettifica** = normale UI/CRUD
    dell'app (non nel contratto).
70. **Purge completa + audit**: cancellazione totale dei dati dell'app (`app_<id>`, inclusi cache/derivati) con **record
    di audit** (prova). L'**EventBridge purge per-tenant** (#06 H) **è** l'invocazione di `purgeData` in offboarding.
71. **Export strutturato** (JSON/CSV, portabilità D). **Orchestrazione account-level dalla piattaforma**: eliminazione
    account (E) = purge piattaforma **+ invoca `purgeData` di ogni app attivata** dell'utente.
72. **Opzione anonimizzazione (per-app, dichiarata nel manifesto)**: un'app può **anonimizzare** invece di cancellare
    (default = cancellazione). **Guardrail legale nella skill `new-application`** (co-pilota): interroga e definisce
    *cosa* anonimizzare, e **blocca** se la scelta è **pseudonimizzazione** (identificatori sostituiti ma dato
    **ri-collegabile** → resta dato personale → **non** vale come erasure). Solo anonimizzazione **irreversibile** esce dal GDPR.
73. **Retention enforcement per-app**: l'app implementa la **purga programmata** secondo la retention del manifesto (E).
74. **Enforcement "no contratto = no produzione"**: scaffold `new-application` genera stub contratto+manifesto; **categoria
    test di compliance** (#10) verifica che export/purge funzionino e **non lascino dati orfani**; ArchUnit-style: ogni
    entità con dati personali **coperta** da export+purge.
75. **Console "Diritti GDPR" nell'admin (single pane of glass)**: vista di **aggregazione** (read/ops, platform-admin)
    che convoglia tutti gli "oggetti" da esercizio diritti — richieste **export** (stato/progress + **link file S3** +
    scadenza), **recessi per-app**, **eliminazioni account** (stato grace 14gg), cambi **consenso**, ticket privacy —
    ciascuno con stato/timeline e **puntatori all'accessorio**: deep-link a **CloudWatch Logs Insights** (pre-filtrato per
    `correlation_id`/`user_id`), job export/oggetto S3, registro audit/breach. **Scoped alla retention** (E): i record
    spariscono a fine finestra (minimizzazione), la prova di evasione resta nel registro audit per il periodo dovuto.
    Vive nella console admin (#03) come capability core "compliance" (#04); si appoggia a ticketing in-house (I), export
    job (D), audit (#08). **Non è un nuovo store**, è aggregazione. → use case + skill.

## Revisione legale pre-go-live
Consolidata nel documento vivo dedicato → **[_REVISIONE-LEGALE.md](_REVISIONE-LEGALE.md)** (checklist L1–L12:
DPA, privacy policy, ToS, ruoli, art. 9, retention, consenso, sub-processor, accessibilità, entità legale, template breach).
**Consigliata, opzionale**; nessun blocco prima del go-live.

## Questioni aperte
_Nessuna — #13 chiuso (A–L)._ Resta da **redigere i testi** (policy/ToS/DPA/template breach, deliverable) e da
**progettare gli use case** (diritti GDPR, ticketing, console diritti) + le **skill** (`new-application` co-pilota
anonimizzazione, `breach-response`). Validazione finale → [_REVISIONE-LEGALE](_REVISIONE-LEGALE.md).

## Impatti su altre aree
- [05-persistenza-dati](05-persistenza-dati.md) (soft-delete/erasure), [06-infra-iac](06-infra-iac.md) (purge EventBridge),
  [08-observability](08-observability.md) (retention/no-PII/audit), [09-pagamenti](09-pagamenti.md) (Paddle MoR),
  [10-testing](10-testing.md) (a11y), [03-frontend](03-frontend.md) (consenso/policy UI), [_BACKLOG.md](_BACKLOG.md)
