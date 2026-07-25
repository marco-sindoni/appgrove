# Change 0046: Scheletro del sito vetrina (Astro SSG, i18n, contenuti legali, hosting statico-first)

**Branch**: `change/0046-use-case-0036-vetrina-astro-scheletro`
**Aree**: nuovo artefatto `site/` (Astro SSG); `infra/` (distribuzione statica del sito, come codice non applicato); `run-tests.sh` + eventuale check di supporto (nuova area `site`); `docs/` (chiusura punto aperto UC 0036, indice esecuzione).
**Data**: 2026-07-25
**Autore**: Platform Engineering (autopilot)
**Use case sorgente**: [docs/usecases/09-marketing-site/0036-vetrina-astro-scheletro.md](../../docs/usecases/09-marketing-site/0036-vetrina-astro-scheletro.md)
**Tocca dati personali?**: No — sito statico, nessun dato personale a riposo (UC 0036 §7). Il primo trattamento sarà la newsletter, di proprietà di UC 0039. Gate privacy/RoPA non applicabile.

## Problema / Obiettivo

Il marketplace non ha ancora un **sito vetrina**. È il **primo artefatto di produzione** e sblocca l'attivazione
dei pagamenti (Paddle richiede un sito live in HTTPS con i documenti legali — #14 A, memoria *paddle-activation-blocker*).
Questa change realizza lo **scheletro** del sito: la fondazione tecnica su cui poseranno i contenuti veri (homepage,
landing per-app) delle change successive, senza ancora produrli.

Obiettivo osservabile a fine change: un progetto Astro avviabile in locale (`astro dev`/`preview`) che, a partire dai
markdown già presenti nel repo, renderizza i **documenti legali nelle 5 lingue** con i dati del titolare risolti dai token,
espone un'architettura **i18n a subpath** con `hreflang`, applica il **gate di pubblicazione** e `noindex`, ed è coperto da
controlli automatici; più l'**infra di hosting** scritta come codice (non applicata) e l'integrazione nell'entrypoint unico dei test.

## Scope

**Artefatto `site/` (Astro SSG)**
- Progetto Astro in cartella top-level `site/`, separato dalle due SPA, con proprio `package.json`/lockfile e script
  `dev`/`build`/`preview`/`test`.
- **Riuso del brand** (UC 0019) tramite gli export sorgente del design system: preset Tailwind (`@appgrove/design-system/preset`),
  `tokens.css`, `fonts.css`. Nessuna dipendenza dalla build (dist) del design system. Island React minimali stilati col preset.
- **i18n a subpath**: `/en|it|fr|es|de/…`, default **EN**; `hreflang` + `x-default` su ogni pagina; root `/` reindirizza per
  `Accept-Language` con fallback EN.
- **Contenuti = fonte `.md`**: le pagine sono generate dai markdown, non da testi incollati nel sito. Frontmatter
  `version`/`effective_date`/`lang`/`status` rispettato.
- **Pagine renderizzate dallo scheletro**:
  - **Legali** — i 5 componenti (`privacy`, `terms`, `refund`, `cookie`, `subprocessors`) × 5 lingue, letti da
    `content/legal/*.md`, con **sostituzione dei token `{{titolare.<campo>}}`** dai valori di `content/legal/entity.yaml`.
  - **Home placeholder** per lingua — shell di brand, nessun contenuto marketing reale.
  - **Backoffice "coming soon"** — pagina statica destinata a `app.appgrove.app`.
- **Gate di pubblicazione**: la build emette solo contenuti con `status: published`; le bozze restano nel repo, fuori dall'output.
- **`noindex`** su ogni pagina finché un flag di build non lo disattiva (rimozione = atto operativo al go-live).

**Contratto di sostituzione dei token** (punto aperto posseduto da UC 0036)
- Il renderer sostituisce ogni `{{titolare.<campo>}}` col valore da `entity.yaml` **prima** di emettere la pagina; token
  senza chiave = **errore di build**; a fine build si asserisce che **nessun `{{` residuo** compaia nell'HTML dei legali.
- Chiusura del punto aperto in `docs/usecases/09-marketing-site/0036-*.md`.

**Infra (`infra/`, come codice — non applicata)**
- Distribuzione dedicata al sito ricalcando il pattern SPA di `infra/modules/platform_shared/cloudfront.tf`: **bucket S3 privato +
  OAC + CloudFront + Route53 + ACM**; **CloudFront Function di basic auth** per l'ambiente **test**; header/meta **`noindex`**.
- Domini: `appgrove.app` (vetrina) + `app.appgrove.app` (backoffice "coming soon").
- Deve passare `terraform fmt -check` e `validate`. **L'apply resta operativo/differito** (phased-env).

**Integrazione test & tooling**
- Nuova area **`site`** in `run-tests.sh` con: `astro build` verde; **parità 5 lingue** delle rotte del sito; **nessun link
  interno rotto**; **solo `published`** in output; **nessun token `{{` residuo** nell'HTML dei legali; **hreflang completo**.
- `run-tests.sh` aggiornato nello stesso commit (Definition of Done dell'entrypoint unico).

## Fuori scope

Esplicitamente **fuori**, con proprietario tracciato:
- **Contenuti marketing reali** — homepage vera, testi, value prop, sezioni: **UC 0037**.
- **Landing per-app** (generate da `new-application`/`finalize-landing`): **UC 0038** / #14 51.
- **Newsletter** e ogni trattamento di dati personali del sito: **UC 0039**.
- **SEO/GEO avanzati, blog, OG image, structured data**: **UC 0040/0041/0042**.
- **Riuso profondo dei componenti React del design system** negli island: differito ai contenuti interattivi (UC 0037),
  tracciato in UC 0036 "Punti aperti".
- **Apply dell'infra, delega DNS, validazione ACM, provisioning credenziali basic-auth, rimozione `noindex` al go-live,
  job CI di deploy reale verso test/prod**: operativi/differiti (phased-env), tracciati in UC 0036 "Punti aperti".

## Criteri di accettazione

- [ ] `site/` si avvia in locale (`astro dev`) e produce una build statica (`astro build`) senza errori.
- [ ] I 5 documenti legali sono renderizzati nelle 5 lingue ai percorsi `/{lang}/legal/{componente}`; i token
      `{{titolare.<campo>}}` sono **risolti** da `entity.yaml` e **nessun `{{` residuo** compare nell'HTML dei legali.
- [ ] Ogni pagina espone `hreflang` per le 5 lingue + `x-default` e il meta `noindex` (attivo di default); la root `/`
      reindirizza alla lingua secondo `Accept-Language` con fallback EN.
- [ ] La build emette **solo** i contenuti `status: published`; un contenuto `draft` non finisce nell'output.
- [ ] Esistono la home placeholder per lingua e la pagina backoffice "coming soon".
- [ ] Il brand (colori/tipografia) proviene dal design system via preset/token, senza dipendere dalla sua build.
- [ ] L'infra del sito (S3 privato + OAC + CloudFront + Route53 + ACM + CloudFront Function basic-auth + noindex) è scritta
      e passa `terraform fmt -check` e `validate` (non applicata).
- [ ] `run-tests.sh` ha l'area `site` e la esegue verde (build + parità lingue + link + published-only + no-token + hreflang);
      l'insieme delle aree toccate è verde prima del commit.
- [ ] Il punto aperto sui token in `docs/usecases/09-marketing-site/0036-*.md` è chiuso; `_INDEX.md` porta 0036 a ✅.

## Invarianti appgrove toccati

- **Tenant ID dal JWT / filtro row-level**: N/A — sito pubblico statico, nessun tenant, nessuna query.
- **Modulo Terraform `microsaas_app`**: N/A — il sito non è un'app micro-SaaS; è una distribuzione statica dedicata che
  ricalca il pattern CloudFront/S3 esistente delle SPA (non si crea infra bespoke parallela: si riusa lo stesso schema
  di bucket privato + OAC + distribuzione).
- **Logging strutturato**: N/A — nessun servizio backend introdotto.

## Requisiti di test

- Parità 5 lingue delle rotte del sito: la build **fallisce** se per un componente/pagina manca una lingua.
- Integrità dei token: build **rossa** se un `{{titolare.*}}` non risolve; asserzione post-build di **assenza di `{{`** residui.
- Gate di pubblicazione: un contenuto di prova `status: draft` **non** deve comparire nell'output.
- `hreflang`/`x-default` presenti su ogni pagina; root `/` redirige correttamente.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (nuovo artefatto, nessun contratto esistente modificato) |
| Contratto cross-area | N/A — il sito consuma `content/legal/*.md` e `entity.yaml` in sola lettura (contratto già definito in `content/legal/README.md`) |
| Version bump | nessuno (nessun documento legale modificato; solo nuovo renderer) |
