# Compliance & Privacy (GDPR) — Decisioni

**Stato**: 🟡 in corso (A, B, C, D decisi; E–L da definire)
**Ultimo aggiornamento**: 2026-06-20

> ⚠️ **Disclaimer**: questo documento NON è parere legale. Fissa le **scelte tecniche/architetturali** che rendono la
> compliance possibile e una **postura di default ragionevole**, da **validare con un professionista** prima del go-live.
> I punti che richiedono un legale sono segnalati.

## Scope
Trattamento dei dati personali conforme a GDPR (e norme collegate), lato **tecnico** (cosa deve poter fare il sistema)
e **documentale** (policy, registri). Si appoggia a: soft-delete/erasure (#05), purge per-tenant EventBridge (#06 H),
retention log/audit + no-PII nei log (#08), Paddle MoR (#09), accessibilità (#10 K).

## Agenda
- **A. Ruoli & perimetro** (titolare/responsabile) — 🟢 deciso
- **B. Basi giuridiche & finalità** — 🟢 deciso
- **C. Data mapping / Registro trattamenti (Art. 30)** — 🟢 deciso
- **D. Diritti degli interessati** (accesso, rettifica, oblio, portabilità, opposizione) — 🟢 deciso
- **E. Data retention** (politica complessiva) — 🔴
- **F. Consenso, cookie & tracking** — 🔴
- **G. Privacy Policy & T&C** (minimizzazione informativa; ripartizione con Paddle) — 🔴
- **H. Sub-responsabili & DPA** — 🔴
- **I. Data residency & trasferimenti** — 🔴
- **J. Data breach** (notifica 72h) — 🔴
- **K. Privacy by design/default & DPIA** — 🔴
- **L. Funzionalità GDPR nelle app** (export/erasure per-app) — 🔴

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
   | **Web analytics sito vetrina** | Misurare uso del vetrina | cookieless → **legittimo interesse**/nessun consenso |

9. **Limitazione di finalità (art. 5.1.b)**: i dati raccolti per il servizio **non** si riusano per altri fini → il
   "no monetizzazione" diventa vincolo formale documentato.
10. **Newsletter = consenso double opt-in**, **separata** dalle email transazionali (che restano su base contrattuale),
    disiscrizione facile in ogni email, **registrazione della prova del consenso**. (Meccaniche → F.)
11. **Analytics = cookieless privacy-first (Cloudflare Web Analytics, $0)** sul **solo sito vetrina**; **app loggata =
    ZERO tracking** (solo cookie tecnici). Niente cookie/PII → **nessun consent banner** (citazione in policy per
    trasparenza, G). Alternativa EU-hosted = Plausible (~€/mese). GA scartato (banner obbligatorio + trasferimento USA
    sensibile post-Schrems II, pur praticabile via EU-US DPF). Setup Cloudflare = una-tantum (account + snippet beacon
    nel sito vetrina), da fare **quando si attiva il vetrina**.
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
21. **Fallback manuale tracciato su Jira, ticket creato AUTOMATICAMENTE** (no creazione manuale): istanza Jira piano
    **Free** (≤10 utenti, 2GB). Creazione via **REST API Jira** (token in Secrets Manager). **Trigger**: form privacy
    **in-app** (canale strutturato preferito); email a `privacy@appgrove.app` via **SES→Lambda**; eventi di sistema
    (es. export FAILED #13 D, escalation gate art. 9 #13 C). **Contenuto minimizzato**: tipo richiesta, `user_id`/
    `tenant_id` **opachi**, timestamp, **SLA due date 1 mese** impostata auto, **link a vista admin interna** (i dati
    personali veri restano nel sistema, NON in Jira). **Log delle richieste** (accountability, #08). **Verifica identità**
    prima di evadere (nel self-service implicita: utente autenticato). **Atlassian = sub-responsabile** → sub-processor +
    DPA (#13 H). Evoluzione: **Jira Service Management** (free ≤3 agenti) per SLA/automazioni help-desk.
22. **Export asincrono (architettura)** — riusato sia per accesso/portabilità sia per export-prima-di-cancellazione:
    1) richiesta → record **export job** (id, tipo/app, `requested_at`, `status` QUEUED→RUNNING→COMPLETED/FAILED,
       `progress` step X/N);
    2) **worker async (SQS)** esegue gli **step di export dichiarati dal contratto per-app** (#13 C/L) aggiornando il
       progress (app con più tabelle → più step/più tempo); l'orchestratore aggrega;
    3) al completamento → **ZIP su bucket S3 dedicato** (privato, **SSE cifrato**, **lifecycle auto-delete 7gg**) +
       **presigned URL 7gg**; UI mostra **link + data/ora scadenza**;
    4) **link solo in-app** all'utente autenticato (non via email). Refresh UI = polling dello stato/progress.
    Meccanica UX di dettaglio → use-case GDPR (backlog).

## Questioni aperte
E–L da definire. Punti per il **legale**: dicitura esatta titolare/responsabile sui contenuti, testo DPA, validità del
modello "cancellazione immediata post-export", periodi di retention.

## Impatti su altre aree
- [05-persistenza-dati](05-persistenza-dati.md) (soft-delete/erasure), [06-infra-iac](06-infra-iac.md) (purge EventBridge),
  [08-observability](08-observability.md) (retention/no-PII/audit), [09-pagamenti](09-pagamenti.md) (Paddle MoR),
  [10-testing](10-testing.md) (a11y), [03-frontend](03-frontend.md) (consenso/policy UI), [_BACKLOG.md](_BACKLOG.md)
