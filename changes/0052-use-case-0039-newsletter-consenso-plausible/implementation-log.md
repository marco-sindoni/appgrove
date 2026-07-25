# Log di implementazione — Change 0052 (UC 0039: newsletter + consenso + Plausible)

**Branch**: `change/0052-use-case-0039-newsletter-consenso-plausible` · **Modalità**: autopilot
**Aree**: `services/core`, `frontend/` (SPA backoffice + design-system + i18n + api-client), `site/` (vetrina Astro),
`tools/compliance` (manifesto + RoPA), `shared/email-templates`.

> Nota operativa: durante il lavoro un'altra sessione stava operando in parallelo nella stessa cartella del repo e
> ha spostato `HEAD` su un altro branch (`change/0053`). Per non danneggiare il suo lavoro né il mio, la change è
> stata portata avanti in un **worktree git dedicato** agganciato a questo branch (`.claude/worktrees/0052-newsletter`),
> senza toccare la cartella condivisa. Scelta approvata dallo sviluppatore.

## Cosa è stato fatto

### Backend — `services/core`, nuovo dominio `newsletter/`
- **Store platform-level** (schema `platform`, migrazione **V10**): `newsletter_subscriber` (email `@PersonalData`,
  stato pending/confirmed/unsubscribed, lingua, canale d'origine, hash single-use del token di conferma con scadenza,
  `user_id` di provenienza) e `consent_event` (registro **append-only** della prova ex art. 7: tipo, versione testo,
  canale, marcatempo). Entità su `BaseEntity` (non tenant-scoped): come `webhook_event`, perché l'iscrizione dal sito
  arriva senza JWT.
- **Accesso dati via JDBC diretto** (`NewsletterService`), non Panache: il resolver tenant di Hibernate è fail-closed
  senza JWT e l'endpoint pubblico gira senza autenticazione — stesso pattern di `PlatformWriter`/`PlatformDataContract`.
  Le entità restano mappate per Flyway e per il manifesto `@PersonalData`.
- **API** (`NewsletterResource`, `/api/platform/v1/newsletter`):
  - pubbliche (senza JWT): `POST /subscriptions` (double opt-in, honeypot + limite di frequenza per IP non persistito),
    `GET /confirm` e `GET /unsubscribe` (pagine HTML localizzate it/en, cliccate dall'email);
  - autenticate (tenant dal JWT): `GET`/`PUT /preference` per il toggle in account.
- **Double opt-in per canale**: sito/signup (anonimi) → `pending` + email di conferma; account (autenticato) → confermato
  subito, senza seconda mail. Ogni transizione scrive stato + evento consenso.
- **Email di conferma**: nuovo messaggio `newsletter-confirm` in `shared/email-templates` (en/it, ripiego en), reso da
  un renderer compatto (`NewsletterEmailRenderer`, gemello di `EmailTemplates` di auth) e spedito col `Mailer`
  quarkus-mailer già cablato nel core (Mailpit dev / MockMailbox test / relay SES cloud differito), fail-soft.
- **Token**: conferma = single-use, hash SHA-256 su DB (riuso di `InvitationTokens`); disiscrizione = HMAC-SHA256
  ricalcolabile dal segreto di config sull'id (nessuna colonna).
- **GDPR**: subscriber e consent event collegati al tenant per confronto email → inclusi in export e purge per-tenant
  (`PlatformDataContract`); **sweeper di retention** (`NewsletterRetentionSweeper`) che elimina fisicamente i disiscritti
  da oltre 24 mesi. Config in `application.properties` (base-url, segreto disiscrizione, limite di frequenza).
- **CORS**: abilitato sul core (`quarkus.http.cors`) — l'iscrizione dal sito vetrina è la **prima chiamata cross-origin**
  verso il core (sito e API su domini diversi) e senza CORS fallirebbe nel browser. **Attenzione** (regressione poi
  corretta in locale): Quarkus con `cors=true` rifiuta con 403 ogni `Origin` non elencato, anche le richieste
  **same-origin** non-semplici (POST/PUT/PATCH) — perciò la lista deve includere **tutte** le origini dei front-end
  (backoffice, admin, vetrina), non solo il sito, altrimenti la SPA stessa viene bloccata. Origini per ambiente (dev:
  domini locali; prod: iniettate via `QUARKUS_HTTP_CORS_ORIGINS`). Aggiunto un test di preflight.

### Frontend — SPA backoffice
- Nuovo componente **`Checkbox`** nel design system.
- **Signup**: checkbox consenso **non pre-spuntata** in `AccountStep`; se spuntata, dopo il signup riuscito la SPA chiama
  l'endpoint pubblico del core (canale `signup`), best-effort e slegata dall'auth (`newsletterApi.ts`).
- **Impostazioni account**: card **Newsletter** con toggle (`Switch`) legato ai nuovi hook `useNewsletterPreference`/
  `useSetNewsletterPreference` (client OpenAPI tipizzato, schema rigenerato con `npm run gen`).
- Stringhe i18n en/it (signup + settings).

### Sito vetrina — `site/`
- I due form inerti (homepage e footer) attivati con l'isola React **`NewsletterForm.tsx`** (stati idle/loading/success/
  error, checkbox consenso non pre-spuntata, campo esca nascosto), POST all'endpoint pubblico; evento Plausible
  best-effort `Newsletter: Subscribe` sul successo. Snippet Plausible già cablato (UC 0040): solo variabili env.
- Nuova variabile pubblica **`PUBLIC_CORE_API_URL`** (build robusta se assente: form disabilitato). Contenuti (consenso,
  esito) aggiunti in **tutte e 5 le lingue**.

### Compliance
- Manifesto `platform.yaml`: nuova voce `newsletter_subscribers.email` (marketing diretto, base consenso, retention
  24 mesi). RoPA rigenerata (`npm run assemble`) e allineata (`npm run check` verde). Plausible risultava **già**
  dichiarato sub-responsabile: nessun nuovo responsabile esterno.

## Privacy / RoPA (gate UC 0031)
- Scanner deterministico: **23 segnali**, tutti attesi e gestiti — tabelle V10, campo personale `email` (annotato
  `@PersonalData`, nel manifesto, coperto da export/purge), chiavi di classificazione del manifesto. Il segnale
  "maven-resources-plugin come potenziale sub-responsabile" è un **falso positivo** (plugin di build che copia i
  template, nessun destinatario esterno di dati).
- **Classificazione: MAJOR** — nuova finalità (marketing diretto) e nuova base giuridica (consenso, art. 6.1.a).
  Registrata qui e in `requirements.md` in attesa di `content/legal/` (UC 0002 replicherà nel bump versione PP/ToS).
- Enforcement `@PersonalData` ↔ manifesto: verde in `mvn test`.

## Test
- **Backend** (`services/core`, JUnit + Testcontainers): `NewsletterFlowTest` (pending→confirmed, honeypot, consenso
  obbligatorio, idempotenza neutra, token non valido, limite di frequenza), `NewsletterPreferenceTest`,
  `NewsletterUnsubscribeTest`, `NewsletterRetentionSweeperTest`, `NewsletterGdprTest`. Aggiornati due test esistenti per
  conseguenza attesa del cambio: `GdprExportApiTest` (5 step di export invece di 4), `PlatformGdprContractTest` +
  `TestData` (fixture iscritto). `PersonalDataManifestTest` verde.
- **Frontend** (vitest): tutti i workspace verdi (backoffice 100, design-system 31, i18n 3, api-client 6, admin 11, …);
  nuovi test su checkbox non pre-spuntata, iscrizione al signup, toggle account; asserzione e2e sulla checkbox.
- **Sito** (vitest): 72 verdi (parità 5 lingue + markup dell'isola: checkbox non pre-spuntata, campo esca).
- Gate canonico: `./run-tests.sh backend frontend compliance site` — vedi esito nella sezione finale del commit.

## Decisioni differite tracciate
- **Unificazione dei due renderer Java dei template email** (auth + core) in `services/commons` → `docs/_BACKLOG.md`.
- **Console admin "Diritti GDPR" (UC 0034)**: aggiungere il tipo "cambi consenso" ora che il consent log esiste →
  punto aperto in `docs/usecases/08-compliance-gdpr/0034-*.md`.
- **Lingue email oltre en/it** (fr/es/de) e **captcha UE** per l'endpoint pubblico → punti aperti in UC 0039.

## Avvio locale
- `app-start.sh`/`app-stop.sh` avviano/fermano ora anche il **sito vetrina** (Astro dev su :4321) oltre a infra +
  backend + SPA, con `PUBLIC_CORE_API_URL` iniettato → il flusso newsletter dal sito è collaudabile con un comando solo
  (invariante CLAUDE.md "Avvio locale"). Skippabile con `--no-spa`.

## Note
- `services/auth` NON è stato toccato: il consenso da signup passa dalla SPA all'endpoint pubblico del core, non dalla
  porta identità (meno accoppiamento).
- La suite frontend, nel worktree fresco, richiede la build dei pacchetti workspace (`dist/`) prima dei test delle app.
