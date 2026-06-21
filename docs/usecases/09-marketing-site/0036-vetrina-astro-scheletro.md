# UC 0036 — Vetrina Astro skeleton (SSG, i18n subpath+hreflang, content md, S3+CloudFront static-first, test basic-auth+noindex)

**Area**: 09-marketing-site · **Fase**: 3 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0019](../06-frontend/0019-design-system-brand-kit.md) (brand kit), UC [0003](../02-devops-infra/0003-fondamenta-terraform.md) (infra edge)
**Fonte decisioni**: #14 B (architettura/rollout), #06 F (S3/CloudFront), #12 (domini/env)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md), [06-infra-iac](../../06-infra-iac.md), [07-devops-cicd](../../07-devops-cicd.md)

## 1. Obiettivo / Scope
Realizzare lo **skeleton del sito vetrina Astro** (artefatto separato dalle 2 SPA) e il suo **rollout statico-first** — il
**primo artefatto prod** che sblocca Paddle.
**Incluso**: **Astro SSG** (+ islands React per interattività mirata, riusando il design system UC 0019); **i18n subpath**
(`/en|it|fr|es|de/…`) + **hreflang** + `x-default`; **content `.md`/MDX** fonte unica (frontmatter version/effective_date/lang);
hosting **S3+CloudFront+Route53+ACM** (~$0–1); **backoffice "coming soon"** statico su `app.appgrove.app`; **test** protetto da
**basic auth (CloudFront Function)** + **`noindex`**; **prod** pubblico con gate `published` + `noindex` fino al go-live.
**Escluso**: homepage/contenuti (UC 0037), landing per-app (UC 0038), newsletter (UC 0039), SEO/GEO (UC 0040/0041), blog (UC 0042).

## 2. Attori & ruoli
- **Visitatore/Paddle/crawler**: consumano il sito statico pubblico (prod).
- **Founder**: review in **locale** (`astro dev/preview`) — loop primario (#14 54).
- **CI**: build Astro → deploy S3 → invalidation CloudFront (#14 15, #07).

## 3. Precondizioni
- Brand kit (UC 0019); foundation edge (UC 0003: S3/CloudFront/Route53/ACM); domini `appgrove.app` + `app.appgrove.app` (#12 9).

## 4. Flusso principale
1. Scaffold **Astro** con struttura content-driven; islands React per selettore lingua/form (#14 12).
2. **i18n**: subpath per lingua + **hreflang**/`x-default`; root `/` → redirect per `Accept-Language` o fallback EN (#14 14).
3. **Content**: pagine da **md/MDX** (frontmatter version/effective_date/lang); stessi md dei legali (UC 0002) per il rendering policy (#14 13).
4. **Rollout statico-first**: si accende **solo** l'infra del sito (S3+CloudFront), **senza** backend/DB/Fargate; backoffice = pagina **"coming soon"** su `app.appgrove.app` (dominio live sottomettibile a Paddle) (#14 B4).
5. **Ambienti**: locale (preview) → **test** (basic auth + `noindex`) → **prod** (pubblico, gate `published`, `noindex` fino al go-live) (#14 54).

## 5. Flussi alternativi / edge / errori
- **Pagine bozza** (`status: draft`, es. landing non finalizzate) → **non** renderizzate in build; check CI `status: published` (#14 52).
- **`.app` HSTS preload** → HTTPS obbligatorio ovunque (anche locale via mkcert, #12 13).
- **Test indicizzato per errore**: prevenuto da `noindex` + basic auth (#14 54).
- **Lingua mancante**: check CI 5 lingue rompe la build (#14 13).

## 6. Risorse & runbook
**Artefatto**: progetto Astro (`site/` o simile) separato dalle SPA. **Infra**: distribuzione S3+CloudFront dedicata al sito,
CloudFront Function per basic auth (test). **Runbook**: `astro dev` (review locale) → PR → merge: deploy **test** (protetto) →
tag: deploy **prod** (pubblico). **Rollback**: redeploy del commit precedente (artefatto statico immutabile).

## 7. Dati toccati
Solo **contenuti** (md/asset), nessun dato personale a riposo nel sito statico. La newsletter (UC 0039) introdurrà l'unico
trattamento. **Residency**: S3/CloudFront in setup UE (#13 I). Manifest: N/A per lo skeleton (vedi UC 0039 per la newsletter).

## 8. Permessi & gate
- **Invarianti multi-tenancy**: N/A (sito pubblico statico, nessun tenant).
- **Gate di pubblicazione**: build renderizza solo `published`; test dietro basic auth + noindex; prod noindex fino al go-live (Paddle vede, motori no). Coerente con phased-env (statico-first).

## 9. Requisiti di test
- **Check CI**: 5 lingue presenti, link non rotti, solo `published` online (#14 52).
- **Build/perf**: zero JS di default (Astro) per SEO/GEO; HTTPS forzato.
- Smoke: il sito test è raggiungibile solo con basic auth; prod pubblico ma `noindex` pre-go-live.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #14 B4/12/13/14/15/52/54, #06 17, #12 9/10/13.
- **DoD**:
  1. Skeleton Astro SSG con i18n subpath+hreflang e content md (frontmatter versioning).
  2. Hosting S3+CloudFront+Route53+ACM; backoffice "coming soon"; rollout statico-first.
  3. Test con basic auth + noindex; prod gate `published` + noindex fino al go-live.
  4. Check CI 5 lingue + link verdi; build senza backend.
