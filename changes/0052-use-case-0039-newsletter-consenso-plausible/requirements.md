# Change 0052: Newsletter (double opt-in) + registro consensi + Plausible cookieless

**Branch**: `change/0052-use-case-0039-newsletter-consenso-plausible`
**Aree**: `services/core`, `frontend/` (SPA backoffice + design-system + i18n), `site/` (vetrina Astro), `tools/compliance` (manifesto dati + RoPA), `shared/email-templates`
**Data**: 2026-07-25
**Autore**: Platform Engineering (modalità autopilot)
**Use case sorgente**: [docs/usecases/09-marketing-site/0039-newsletter-consenso-plausible.md](../../docs/usecases/09-marketing-site/0039-newsletter-consenso-plausible.md)
**Tocca dati personali?**: **Sì** — nuova finalità *marketing diretto* (email iscritto), base giuridica **consenso** (art. 6.1.a), nuovo responsabile esterno di analytics (**Plausible**, aggregato non identificativo). Classificazione del cambio: **MAJOR** (vedi *Valutazione di impatto*). Le classificazioni sono già stabilite nello UC 0039 (🟢 deciso, sez. 7): qui vengono applicate, non decise. Il gate privacy/RoPA di step-03 aggiorna manifesto e RoPA; il bump versione Privacy Policy/Termini è registrato finché `content/legal/` non esiste.

## Problema / Obiettivo

Oggi il sito vetrina espone i form newsletter (homepage e footer) come **sola struttura visuale**: pulsanti disabilitati, nessun invio, nessun consenso, nessuna misura. La piattaforma non ha modo di raccogliere iscritti alla newsletter né di conservare la **prova del consenso** richiesta dall'art. 7. Le email transazionali (verifica, invito) esistono, ma il *marketing diretto* — che ha base giuridica e ciclo di vita diversi — non è mai stato costruito.

Obiettivo: permettere a un visitatore o a un utente di **iscriversi alla newsletter con doppia conferma** (double opt-in), conservare un **registro immutabile dei consensi e delle revoche**, offrire **disiscrizione facile**, e attivare l'analytics **Plausible** cookieless (UE, senza banner) sul solo sito vetrina. Risultato osservabile: un'email inserita nel subscribe box riceve una mail di conferma; solo dopo il click risulta `confirmed`; ogni consenso/revoca lascia una traccia con versione del testo e canale; il sito misura le visite in forma aggregata senza cookie.

## Scope

Tre punti di raccolta del consenso, un unico store platform-level nel core.

### 1. Backend — `services/core` (nuovo dominio `newsletter/`)
- **Store dati platform-level** (schema `platform`, migrazione **V10**), non legato al tenant:
  - `newsletter_subscriber`: email (univoca su `lower(email)`, annotata `@PersonalData` — finalità marketing diretto, base consenso, retention iscritto + 24 mesi post-disiscrizione), stato `pending|confirmed|unsubscribed`, lingua, marcatempo creazione/conferma/disiscrizione, **hash SHA-256 single-use** del token di conferma con scadenza, riferimento utente opzionale (quando nasce da un account).
  - `consent_event`: registro **append-only** (mai aggiornato/cancellato) — tipo `grant|confirm|revoke`, versione del testo di consenso, canale (`site|signup|account|email-unsubscribe`), marcatempo, riferimento al subscriber. È la prova ex art. 7.
- **Endpoint pubblico** (senza JWT, unico precedente: il webhook Paddle) per l'iscrizione dal sito → crea/riporta a `pending` + invia email di conferma. Difese anti-abuso: campo esca ("honeypot") + limite di frequenza per IP **in memoria** (IP mai persistito). Nessun captcha di terze parti (postura UE/no-tracker).
- **Endpoint di conferma** double opt-in (token single-use) → stato `confirmed` + evento `confirm`. Idempotente sui click ripetuti; token scaduto → errore gestito con possibilità di re-invio.
- **Endpoint di disiscrizione** one-click (link nell'email, token per-subscriber) → stato `unsubscribed` + evento `revoke` canale `email-unsubscribe`.
- **Endpoint autenticati** (user-scoped, `tenant_id` dal JWT) per la preferenza newsletter dell'utente loggato: lettura stato + attiva/disattiva (`grant`/`revoke` canale `account`).
- **Iscrizione da signup**: il core riceve dal provider auth il flag consenso e, se vero, crea il subscriber **già `confirmed`** (email verificata dal flusso di registrazione), evento `grant` canale `signup`, **senza** seconda mail.
- **Invio email di conferma**: nuovo template `newsletter-confirm` in `shared/email-templates` (en/it, ripiego en); porta di invio nel core sul modello `services/auth` (SES cloud / Mailpit locale via `@LookupIfProperty`).
- **GDPR**: i subscriber collegati a un utente entrano in export e purge per-tenant (`PlatformDataContract`); **sweeper di retention** (modello `AccountDeletionSweeper`) che cancella fisicamente i subscriber disiscritti da oltre 24 mesi.

### 2. Provider auth — `services/auth`
- Il body di `POST /api/auth/signup` accetta il flag booleano `newsletterConsent` (default assente = false) e lo propaga al core. Solo passaggio del flag: nessuna logica marketing in auth.

### 3. SPA backoffice — `frontend/`
- **Signup**: checkbox consenso newsletter **non pre-spuntata** (default `false`) in `AccountStep`; nuovo componente `Checkbox` nel design-system (oggi esiste solo `Switch`).
- **Impostazioni account** (`Settings.tsx`): **toggle** newsletter (`Switch`) legato ai nuovi endpoint core della preferenza; riflette lo stato reale, permette attiva/disattiva.
- Nuovi hook TanStack Query verso gli endpoint preferenza. Stringhe nei cataloghi i18n **en/it**.

### 4. Sito vetrina — `site/`
- I due form inerti (homepage `index.astro`, footer `BaseLayout.astro`) diventano un'**isola client React** `NewsletterForm.tsx` con stati idle/loading/success/error e **checkbox consenso non pre-spuntata**; POST verso il core.
- Nuova variabile pubblica **`PUBLIC_CORE_API_URL`** per l'URL del backend (prima chiamata runtime del vetrina).
- Nuove chiavi testo (proposta di valore, placeholder, **testo del consenso**, esito) in `src/content/marketing/types.ts` e in **tutte e 5 le lingue** (en/it/fr/es/de) — la parità è presidiata dal test esistente.
- **Plausible**: snippet già cablato (UC 0040); qui si documentano le variabili `SITE_INDEXABLE`/`PUBLIC_PLAUSIBLE_DOMAIN` come prerequisito go-live e si emette un evento personalizzato `Newsletter: Subscribe` sull'iscrizione riuscita (best-effort).

### 5. Compliance — `tools/compliance` + `docs/compliance`
- Manifesto `docs/compliance/manifests/platform.yaml`: nuove voci `entity`/`field` per `newsletter_subscriber` (e `consent_event` se porta dati personali) — necessarie o `mvn test` fallisce.
- RoPA rigenerata con i trattamenti "newsletter" (marketing diretto, consenso) e "web analytics vetrina" (Plausible, aggregato, legittimo interesse).

## Fuori scope

- **Centro preferenze consensi completo** in account (hub multi-opt-in) → resta a **UC 0033** (già escluso lì). Qui si consegna solo il *singolo* toggle newsletter + unsubscribe, come richiesto dai punti aperti dello UC 0039.
- **Estensione della console admin "Diritti GDPR"** con il tipo "cambi consenso" → è lavoro di **UC 0034**; tracciato come punto aperto (l'estensione presuppone il consent log, che nasce ora).
- **Estrazione in `services/commons`** dell'astrazione di invio email condivisa tra auth e core (deduplica) → tracciato in `docs/_BACKLOG.md`; qui il core replica il pattern.
- **Lingue email oltre en/it** (fr/es/de) → convenzione UC 0018 invariata (ripiego su en); estensione tracciata come punto aperto.
- **Captcha/anti-abuso avanzato** (oltre honeypot + limite di frequenza) → punto aperto se il volume di spam lo richiederà.
- **Gestione automatica del cookie-banner / CMP** → non serve (nessun tracker non essenziale); escluso dallo UC.
- **SEO/GEO** (UC 0040/0041) e qualunque tracciamento dentro l'app loggata (zero tracking, solo Plausible sul vetrina).

## Criteri di accettazione

- [ ] Un'email inserita nel subscribe box del sito crea un subscriber `pending` e fa partire una mail di conferma; nessun contenuto marketing parte prima della conferma.
- [ ] Il click sul link di conferma porta il subscriber a `confirmed` e scrive un `consent_event` di tipo `confirm` con versione del testo, canale e marcatempo; il click ripetuto è idempotente; il token è single-use e scade.
- [ ] Al signup la checkbox consenso è **non pre-spuntata**; se spuntata crea un subscriber `confirmed` con evento `grant` canale `signup` e **nessuna** seconda mail; se non spuntata non crea nulla.
- [ ] Il toggle in impostazioni account riflette lo stato reale e, cambiando, registra `grant`/`revoke` canale `account`; il tutto user-scoped (`tenant_id` dal JWT).
- [ ] La disiscrizione one-click porta a `unsubscribed` e registra un `revoke`; il link funziona senza login.
- [ ] Lo store subscriber/consensi è platform-level (non tenant-scoped) e l'endpoint pubblico è raggiungibile senza JWT ma protetto da honeypot + limite di frequenza per IP (IP non persistito).
- [ ] I subscriber collegati a un utente sono inclusi in export e purge per-tenant; lo sweeper cancella i disiscritti oltre 24 mesi.
- [ ] Il manifesto dati e la RoPA includono i nuovi trattamenti; `mvn test` (`@PersonalData` ↔ manifesto) è verde.
- [ ] Sul sito: nessun cookie e nessun tracker oltre Plausible cookieless, nessun banner; l'evento `Newsletter: Subscribe` è emesso best-effort e non rompe se Plausible è assente.
- [ ] Testi del sito presenti e coerenti in tutte e 5 le lingue (parità verde).
- [ ] Le suite delle aree toccate (`run-tests.sh backend|frontend|compliance` + test sito) sono verdi.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT** — mantenuto: gli endpoint autenticati (preferenza account) leggono `tenant_id` dal JWT via `CallerContext`, mai dal body. L'endpoint pubblico del sito è **deliberatamente senza tenant** (dato platform-level, come `webhook_event`): non inventa un tenant dal body.
- **Filtro row-level `WHERE tenant_id`** — N/A per lo store newsletter (platform-level, entità su `BaseEntity`). Resta valido per la lettura della preferenza dell'utente, filtrata per l'utente autenticato.
- **Modulo Terraform `microsaas_app`** — non toccato (nessuna nuova app).
- **Logging strutturato** — ogni log dei nuovi endpoint porta i campi disponibili (`app_id=platform`, `user_id` quando autenticato; l'iscrizione anonima non ha `tenant_id`/`user_id` — si logga senza inventarli).

## Requisiti di test

- **Backend (`mvn test`, integration con Postgres reale)**: ciclo `pending → confirmed`; token single-use e scaduto; idempotenza conferma; `grant`/`revoke` da signup e da account; disiscrizione; append-only del `consent_event`; endpoint pubblico senza JWT + honeypot/limite di frequenza; inclusione in export e purge per-tenant; regola di retention 24 mesi; coerenza `@PersonalData` ↔ manifesto.
- **Frontend (`npm test` + Playwright e2e)**: checkbox signup non pre-spuntata (default false) e propagazione del flag; toggle account che riflette/aggiorna lo stato; componente `Checkbox` del design-system.
- **Sito (`vitest`)**: parità delle nuove chiavi testo nelle 5 lingue; markup del form con checkbox non pre-spuntata; assenza di cookie/tracker oltre Plausible.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (nuove funzionalità additive; il body signup estende in modo retrocompatibile con flag opzionale) |
| Contratto cross-area | Sì — sito → core (nuovo endpoint pubblico + `PUBLIC_CORE_API_URL`); SPA → auth → core (flag consenso); SPA → core (preferenza) |
| Version bump | **minor** del codice; **MAJOR** ai fini privacy (nuova finalità marketing, nuova base giuridica consenso, nuovo responsabile esterno Plausible) → bump Privacy Policy/Termini registrato qui in attesa di `content/legal/` |
