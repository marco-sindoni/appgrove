# UC 0040 — SEO technicals (sitemap, Schema.org, meta/OG, hreflang)

**Area**: 09-marketing-site · **Fase**: 3 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0036](0036-vetrina-astro-scheletro.md) (skeleton sito)
**Fonte decisioni**: #14 H (SEO)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md)

## 1. Obiettivo / Scope
Implementare il **SEO tecnico** del sito statico Astro.
**Incluso**: performance/Core Web Vitals nativi; **hreflang** (5 lingue subpath) + **`x-default`**; **sitemap.xml** multilingua
+ **robots.txt**; **canonical** puliti; **Schema.org** (`SoftwareApplication`/`Product`+`Offer`, `Organization`, `BreadcrumbList`,
`FAQPage`, `Article`); **meta** per pagina/lingua + **Open Graph/Twitter card** (OG image); **slug localizzati** per lingua;
keyword strategy a due livelli (app/brand) + nicchia EU/GDPR; misurazione (Plausible + Search Console + Bing); **check CI SEO**.
**Escluso**: GEO/llms.txt (UC 0041), contenuti blog (UC 0042), generazione SEO per-app (parte di `new-application`, UC 0046).

## 2. Attori & ruoli
- **Motori di ricerca/crawler**: indicizzano (post go-live; pre-go-live `noindex`).
- **`new-application`** (UC 0046): genera il SEO per-app (meta/OG/structured data/keyword) come parte della landing.
- **CI**: check SEO (meta/title/description, hreflang, link rotti).

## 3. Precondizioni
- Skeleton sito (UC 0036) con i18n subpath; pagine brand (UC 0037) e template landing (UC 0038).

## 4. Flusso principale
1. **hreflang/x-default** su tutte le pagine localizzate; **canonical** puliti; **slug localizzati** (`/it/fatture` vs `/en/invoicing`) legati da hreflang (#14 29/31).
2. **sitemap.xml** multilingua + **robots.txt** (consenso crawler definito anche in UC 0041) (#14 29).
3. **Schema.org** per tipo di pagina (SoftwareApplication/Product+Offer, Organization, BreadcrumbList, FAQPage, Article) (#14 29).
4. **meta** per pagina/lingua + **OG/Twitter card** con OG image (#14 29).
5. **Keyword strategy** a due livelli + nicchia EU/GDPR, localizzata per mercato (#14 30).
6. **Misurazione**: Plausible (referral, anche AI) + Google Search Console + Bing Webmaster (dati di ricerca tuoi, no PII) (#14 33).

## 5. Flussi alternativi / edge / errori
- **Pre-go-live**: `noindex` su prod (Paddle vede, motori no); al lancio si rimuove (#14 54).
- **Lingua/slug mancante** → check CI SEO + 5 lingue rompe la build (#14 34).
- **OG image mancante** su una landing → segnalata dal check; generata in `finalize-landing` (UC 0057).

## 6. Risorse & runbook
**Artefatti**: integrazioni Astro per sitemap/hreflang/schema/meta; OG image pipeline; robots.txt. **Check CI SEO** (#14 34):
meta/title/description presenti, hreflang corretti, link non rotti. **Runbook**: generato in build; verificato in CI; Search Console/Bing collegati post go-live.

## 7. Dati toccati
Solo metadati di contenuto; nessun dato personale. Search Console/Bing = dati di ricerca/indicizzazione del sito (non tracking
utenti), compatibili con la postura purista (#14 33). Manifest: N/A.

## 8. Permessi & gate
- **Invarianti**: N/A (sito pubblico). Gate: `noindex` finché pre-go-live; check CI SEO bloccante sui difetti definiti.

## 9. Requisiti di test
- **Check CI SEO**: meta/hreflang/canonical/sitemap presenti e coerenti; link non rotti; 5 lingue.
- **Validazione structured data** (schema valido) sulle pagine chiave.
- Perf/Core Web Vitals entro soglia (Astro statico).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #14 29/30/31/32/33/34, 54.
- **DoD**:
  1. hreflang+x-default, sitemap+robots, canonical, Schema.org, meta+OG su tutte le pagine.
  2. Slug localizzati; keyword strategy due livelli + nicchia EU/GDPR.
  3. Misurazione Plausible+Search Console+Bing; `noindex` fino al go-live.
  4. Check CI SEO verde; `new-application` genera il SEO per-app.

## Punti aperti / decisioni differite

- **Slug di pagina localizzati per lingua** _(tracciato dalla change `0047-use-case-0037-…`)_: homepage/why/pricing del
  sito nascono con slug in inglese sotto il prefisso di lingua (`/it/pricing/`, non `/it/prezzi/`). Gli slug localizzati
  (#14 31), legati da hreflang, sono ottimizzazione SEO che appartiene a questo use case. Quando verrà implementato,
  localizzare gli slug delle pagine brand e delle landing, aggiornando gli hreflang e il check link.
  _Nota:_ le **landing per-app** (UC 0038) nascono già con **slug localizzati** (`/en/invoicing/`, `/it/fatture/`); qui resta
  da localizzare **solo** le pagine brand (homepage/why/pricing).

- **Dati strutturati Schema.org + sitemap/robots** _(tracciato dalla change `0048-use-case-0038-…`)_: UC 0038 ha aggiunto al
  `BaseLayout` i meta **Open Graph/Twitter** (titolo, descrizione, URL, immagine) per tutte le pagine, incluse le landing.
  Restano da aggiungere in questo use case i **dati strutturati JSON-LD** (dec. #14 29: `SoftwareApplication`/`Product`+`Offer`
  dai tier di prezzo della landing, **`FAQPage`** dalla sezione FAQ del template, `Organization`, `BreadcrumbList`, `Article`)
  e **`sitemap.xml`/`robots.txt`**. Il contenuto sorgente esiste già strutturato in `site/src/content/landings/types.ts`
  (sezioni `pricing` e `faq`): il JSON-LD si può derivare da lì. **Proprietario**: UC 0040.
