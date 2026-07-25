# Change 0047: Homepage + navigazione/footer + pagine "Perché appgrove" e "Prezzi"

**Branch**: `change/0047-use-case-0037-homepage-nav-footer`
**Aree**: `site/` (Astro SSG — sito vetrina)
**Data**: 2026-07-25
**Autore**: Platform Engineering (autopilot)
**Use case sorgente**: [docs/usecases/09-marketing-site/0037-homepage-nav-footer.md](../../docs/usecases/09-marketing-site/0037-homepage-nav-footer.md)
**Tocca dati personali?**: No — pagine statiche pubbliche. La sezione privacy/EU *descrive* la postura, non raccoglie dati; la cattura email della newsletter è solo struttura visuale (la raccolta reale, con consenso e Plausible, è UC 0039). Manifesto dati: N/A. Il gate privacy/RoPA (UC 0031) non produce classificazione MAJOR/MINOR perché non c'è alcun trattamento nuovo.

## Problema / Obiettivo

Lo scheletro del sito vetrina (UC 0036, change 0046) porta solo la *shell* di brand: layout, routing i18n a 5 lingue, resa dei documenti legali e una home segnaposto senza contenuti. Manca il **volto pubblico del prodotto**: la homepage che racconta la promessa, la navigazione e il footer veri, e le pagine brand "Perché appgrove" e "Prezzi".

Obiettivo: dare al sito le **pagine brand** con contenuti reali on-brand nelle 5 lingue, secondo il posizionamento deciso (job-led + privacy come cuneo di fiducia, cross-sell "un account, tanti strumenti"), così che il sito sia navigabile, coerente e — insieme ai legali già presenti — pronto a esporre ciò che la revisione di dominio di Paddle richiede (descrizione prodotto, prezzi, feature, legali raggiungibili dalla navigazione).

## Scope

Tutto dentro `site/` (nessun'altra area toccata).

**1. Modello dei contenuti marketing (5 lingue).**
- Nuovo modulo tipizzato in `site/src/content/marketing/`: un tipo condiviso `MarketingContent` (forma di homepage, "Perché appgrove", "Prezzi", etichette di navigazione e footer) e un indice `Record<Locale, MarketingContent>` con le 5 lingue (`en`, `it`, `fr`, `es`, `de`), EN come sorgente. La parità di forma è garantita a tempo di compilazione.
- Contenuti **AI-generati on-brand** (tono lean), soggetti alla **revisione dell'utente** (gate di rilettura requisiti + gate commit).

**2. Homepage** (`site/src/pages/[lang]/index.astro`, riscrive il segnaposto) con la sequenza narrativa decisa (#14 26):
hero (promessa "strumenti semplici che crescono con te") → **vetrina app** onesta col catalogo piccolo (strumento faro "in arrivo" + "altri strumenti in arrivo", nessun link a landing inesistenti) → cross-sell "un account, tanti strumenti" → **sezione privacy/EU** (cuneo, firma di fiducia non headline) → **newsletter** (struttura visuale, senza backend) → **CTA** (registrati/esplora). Ancora `#apps` sulla sezione vetrina.

**3. Top nav** (nel `BaseLayout` condiviso): **App** (ancora a `/<lang>/#apps`) · **Perché appgrove** (`/<lang>/why/`) · **Prezzi** (`/<lang>/pricing/`) · **Login** (`https://app.appgrove.app/`) + CTA **Registrati** (`https://app.appgrove.app/signup`). Selettore lingua interattivo nell'header (già presente). Responsive.

**4. Footer** (nel `BaseLayout`, arricchito): documenti **legali** (già presenti) · **Support** (`mailto:support@appgrove.app`) · **`security.txt`** (`/.well-known/security.txt`) · **newsletter** (struttura visuale) · copyright del titolare. Selettore lingua resta nell'header.

**5. Pagina "Perché appgrove"** (`site/src/pages/[lang]/why.astro`): mission e valori con il cuneo EU/privacy come firma di fiducia, **senza founder story** né narrativa personale (#14 24, G24).

**6. Pagina "Prezzi — come funziona la fatturazione"** (`site/src/pages/[lang]/pricing.astro`): spiega il **modello** di fatturazione (mensile/annuale, default annuale; prova gratuita 14 giorni; nessun rimborso, con rimando alla Refund Policy), **senza prezzi numerici** — il prezzo vero sta sulle landing app (UC 0038).

**7. `security.txt`** statico in `site/public/.well-known/security.txt` (RFC 9116): Contact `security@appgrove.app`, Policy verso la privacy, Preferred-Languages, Expires con data fissa.

**8. Controlli e test:**
- Estendere `site/scripts/postbuild-check.mjs` alla parità 5 lingue anche di `why/` e `pricing/` (finora copriva home + legali).
- Test vitest sul modello marketing: nessuna stringa vuota e stessa forma/liste tra le 5 lingue (la parità di *tipo* è a compile-time; il test copre i *valori* a runtime).
- I controlli esistenti (hreflang completo, noindex, link interni non rotti, nessun token legale residuo) restano verdi.

## Fuori scope

- **Blog/Risorse** (UC 0042): voce nav omessa (link a pagina inesistente romperebbe il controllo link). → tracciato in UC 0042 e nei punti aperti di UC 0037.
- **Backend newsletter**, log di consenso, Plausible (UC 0039): la newsletter è solo struttura visuale. → tracciato in UC 0039.
- **Link social** nel footer (UC 0043): canali decisi (LinkedIn/X) ma account brand non ancora creati. → tracciato in UC 0043 e nei punti aperti di UC 0037.
- **Landing per-app** (UC 0038/0053): le card app non linkano a landing inesistenti. → tracciato in UC 0053.
- **SEO tecnico** — sitemap, robots, Open Graph/meta social, Schema.org, **slug localizzati per lingua** (UC 0040): fuori scope; le pagine restano `noindex` (gate UC 0036). → slug localizzati tracciati in UC 0040.
- **GEO** (`llms.txt`, crawler AI — UC 0041).
- Nessuna modifica a `frontend/`, `services/`, `infra/`, ai documenti legali (`content/legal/`), o all'entità del titolare (`entity.yaml`).

## Criteri di accettazione

- [ ] La homepage `/<lang>/` esiste in tutte e 5 le lingue con la sequenza narrativa completa (hero → app → cross-sell → privacy/EU → newsletter → CTA), contenuti on-brand, catalogo onesto anche con una sola app.
- [ ] Le pagine `/<lang>/why/` e `/<lang>/pricing/` esistono in tutte e 5 le lingue con i contenuti descritti (nessuna founder story su "why"; nessun prezzo numerico su "pricing", rimando alla Refund Policy).
- [ ] Top nav (App · Perché appgrove · Prezzi · Login + Registrati) e footer (legali · Support · security.txt · newsletter · ©) presenti su ogni pagina, coerenti nelle 5 lingue, responsive; i legali sono raggiungibili dalla navigazione.
- [ ] `/.well-known/security.txt` è servito e valido (Contact, Expires).
- [ ] `npm test`, `npm run build` e `npm run check` in `site/` sono verdi: parità 5 lingue (home, why, pricing, legali), hreflang completo, `noindex` attivo, nessun link interno rotto, nessun token legale residuo.
- [ ] Il modello marketing non ha stringhe vuote e ha la stessa forma nelle 5 lingue (test vitest).

## Invarianti appgrove toccati

Nessuno degli invarianti di piattaforma è in gioco: il sito vetrina è un artefatto statico pubblico, senza tenant, senza JWT, senza query tenant-scoped, senza modulo Terraform, senza logging applicativo. Resta valido l'invariante di progetto del sito: **contenuti nelle 5 lingue con parità garantita** (a compile-time dal modello tipizzato, a valle dal controllo post-build) e **gate di indicizzazione** (`noindex` fino al go-live).

## Requisiti di test

- **Parità 5 lingue** delle nuove pagine (`why`, `pricing`) verificata dal controllo post-build sull'HTML generato, oltre alla home già coperta.
- **Modello marketing**: test vitest che, per ogni lingua, verifica assenza di stringhe vuote e coerenza di forma/lunghezza delle liste rispetto alla lingua sorgente (EN).
- **Link non rotti**: il controllo esistente deve restare verde con la nuova nav/footer (ancore `#apps`, mailto, link esterni alla SPA, `/.well-known/security.txt`).
- **noindex e hreflang**: invariati e verdi sulle nuove pagine localizzate.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | No (tutto interno a `site/`; i link esterni alla SPA e i `mailto:` sono URL, non contratti di codice) |
| Version bump | nessuno (sito vetrina, non versionato come le librerie) |
