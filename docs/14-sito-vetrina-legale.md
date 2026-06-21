# Sito vetrina & testi legali (ToU/PP) — Decisioni

**Stato**: 🟡 in corso
**Ultimo aggiornamento**: 2026-06-21

> ⛔ **PREREQUISITO BLOCCANTE PER PADDLE** (richiesto 2026-06-21): Paddle **non attiva l'account** senza un **sito di
> prodotto pubblico** che includa **Terms of Use/Service** e **Privacy Policy** — **vale anche per il SANDBOX** (no sito,
> nessun account nemmeno di test). Senza attivazione → **qualsiasi uso del vero Paddle** (sandbox + production) è bloccato
> → niente pagamenti, niente go-live. Quindi **l'analisi e l'implementazione di quest'area vanno affrontate PER PRIME**
> rispetto a ogni interazione con il vero Paddle. **Unica via non bloccata = lo stub Paddle locale** (#09 I): si
> sviluppa/testa il grosso di #09 senza account Paddle. È un vincolo di **sequenza implementativa**, non di sole decisioni.

## Scope
Sito vetrina (marketing) pubblico + testi legali pubblici (ToU/ToS, Privacy Policy, cookie disclosure). Artefatto
**distinto** dalle 2 SPA (#03 backoffice+admin). Raccoglie e porta a implementazione quanto già deciso/tracciato in:
- [_BACKLOG](_BACKLOG.md) → sezione "Sito vetrina (marketing)" (statico, multilingua EN/IT/FR/ES/DE, contenuti `.md`,
  subscribe newsletter #13 F, Plausible Cloud cookieless EU, build S3+CloudFront, versioning `effective_date`, check CI
  presenza 5 lingue, **nota marketing "all-EU deployed"** come proposta di valore);
- [13-compliance-privacy](13-compliance-privacy.md) G (privacy policy/ToS multilingua, IT facente fede) e F (consenso/newsletter);
- [_REVISIONE-LEGALE](_REVISIONE-LEGALE.md) → **L2** (Privacy Policy), **L3** (ToS, Paddle MoR), **L11** (entità legale
  titolare, richiesta da privacy policy e da Paddle MoR);
- [09-pagamenti](09-pagamenti.md) J (cosa copre Paddle come MoR vs cosa resta a noi).

## Topic dell'area (agenda, da discutere)
- **A. Requisiti Paddle per l'attivazione** ✅ — checklist merchant Paddle (3 doc legali, prodotto/prezzi/feature, SSL, entità legale)
- **B. Architettura del sito & rollout** ✅ — SSG statico, S3+CloudFront, multilingua, `.md` fonte unica, versioning; **sequenza di accensione prod parziale**
- **C. Testi legali** ✅ — T&C, **Refund Policy**, Privacy Policy, cookie disclosure (IT facente fede); coord. #13 + revisione legale (L2/L3)
- **D. Entità legale titolare (L11)** ✅ — **persona fisica / ditta individuale** (no società), indirizzo, contatti (serve a PP e a Paddle MoR); inquadramento fiscale in [_COMMERCIALISTA](_COMMERCIALISTA.md)
- **E. Posizionamento & ICP** ✅ — cosa vendiamo (brand marketplace vs singole app), a chi, value prop, ruolo del wedge "EU-privacy"
- **F. Brand & identità visiva** ✅ — logo, palette, tipografia, tono, stile dei contenuti visuali
- **G. Contenuti & struttura del sito (IA)** ✅ — homepage, pagine app, pricing, blog/risorse, FAQ, supporto, `security.txt`
- **H. SEO** — keyword strategy, SEO tecnico statico, **hreflang 5 lingue**, structured data, performance
- **I. GEO (Generative Engine Optimization)** — essere citati dagli assistenti AI/LLM; contenuti machine-readable
- **J. Paid & social & launch** — Meta/Facebook + altri social, organico vs paid, strategia di lancio

## Decisioni prese

### A. Requisiti Paddle per l'attivazione
1. **Baseline adottata = checklist Domain Review di Paddle**. Il sito deve esporre, **accessibili dalla navigazione**:
   (a) descrizione chiara del prodotto/servizio; (b) **pricing**; (c) **feature/deliverable** inclusi; (d) **tre documenti
   legali** — **Terms & Conditions + Refund Policy + Privacy Policy**; (e) **nome legale** (o brand ditta individuale)
   **dentro i T&C**; (f) **SSL/HTTPS**, sito **live**. Verifica in 3 fasi: Domain Review (5–7 gg se manuale) → Business
   Verification (2–4 gg) → Identity Verification (>25% owner). Fonti in cronologia ricerca 2026-06-21.
2. **Refund Policy = TERZO documento legale richiesto** (oltre T&C + PP): pagina pubblica con contenuto #09 J43 ("vendite
   finali / no refund salvo legge e Buyer Terms Paddle"). I **Buyer Terms Paddle** concedono già il **recesso 14 gg**
   (rinunciabile se uso immediato) → la nostra "no refund" convive con la tutela statutaria gestita da Paddle (L3/L6).
3. **Domini da sottomettere a Paddle**: **sito vetrina** + **dominio backoffice** (`app.appgrove.app`, dove gira il
   Checkout overlay). Le info legali possono stare sul vetrina ma devono essere linkate/raggiungibili.

### B. Architettura del sito & rollout (parziale)
4. **Rollout prod "a fette" — il sito statico è il PRIMO artefatto prod** (richiesto 2026-06-21): per la Domain Review
   serve un sito **live in HTTPS**, quindi si accende **solo** l'infra del sito vetrina (S3+CloudFront+Route53+ACM,
   **~$0–1** free tier), **senza** backend/DB/Fargate. **Backoffice = pagina statica "coming soon"** su `app.appgrove.app`
   (dominio live e sottomettibile a Paddle, app non ancora esistente). **Raffina (non viola)** `phased-env-activation`:
   resta "niente infra costosa early"; il sito statico (≈gratis) precede il backend. Resto di B → da discutere.

### E. Posizionamento & ICP
5. **Modello a due livelli**: acquisizione a livello **APP** (landing dedicate per *job*/keyword, è dove entra il
   traffico — SEO long-tail, ads mirate, intento alto) + **brand appgrove** come **ombrello** (fiducia, **cross-sell**
   "un account, tanti strumenti", **garanzia privacy EU**). L'app porta il cliente; il brand trattiene e moltiplica.
6. **ICP**: micro-business **europei** — freelance, professionisti con P.IVA, ditte individuali, **micro-team (1–10)**.
   Sensibili al prezzo, vogliono strumenti **semplici e focalizzati** (no suite gonfie), poco tempo/non tecnici,
   diffidenti del lock-in e dei dati USA. Ogni app ha la **sua** sotto-nicchia di ICP; il brand le unisce.
7. **Gerarchia del messaggio = (a) JOB-LED + privacy come wedge**: headline sul *lavoro da fare* + semplicità/prezzo;
   **"all-EU/GDPR" come firma di fiducia del brand** (badge, sezione dedicata, riprova nei contenuti, leva PR/GEO), **non**
   come headline principale. Razionale: la privacy da sola converte poco sugli SMB (comprano per fare un lavoro), ma come
   wedge differenzia, dà fiducia ed è una storia "citabile" (ottima per GEO). Scartate: (b) privacy-led (di nicchia,
   converte meno), (c) ibrido per livello (tenuto come opzione, ma evitato per non gestire due toni).
8. **Strati di value prop** (ordine di forza d'acquisto): job/funzionale → semplicità/focus → prezzo → EU-privacy →
   marketplace ("un account, tanti strumenti che crescono con te").
9. **`new-application` genera la landing di prodotto** (richiesto 2026-06-21): la skill crea la **pagina di prodotto
   specifica dell'app** per il sito vetrina, **in tutte le 5 lingue** (IT facente fede), con **contenuti testuali**
   (headline job-led + value prop + feature + pricing/tier + CTA + badge privacy EU, secondo E7) **e visivi**
   (hero/illustrazioni/mockup/icone, coerenti con la brand identity F). Output come **file `.md` multilingua** (fonte
   unica del sito statico, con `effective_date`/versioning + **check CI 5 lingue**). Dettaglio in `skills-backlog` e
   topic **G** (contenuti). Dipendenza: la parte visiva richiede **F (brand & identità visiva)** definito.

10. **Modello linguistico a due assi** (richiesto 2026-06-21, chiarito):
    - **Documenti LEGALI** (ToS, Privacy Policy, Refund Policy): **IT facente fede** — in caso di difformità tra versioni
      prevale l'italiano (certezza giuridica, giurisdizione italiana del titolare; coerente #13 G).
    - **Contenuti MARKETING** (homepage, landing app): **EN lingua sorgente/master** da cui si adattano le altre (mercato
      ampio, SEO/LLM forti in EN); "facente fede" non si applica (non è testo legale).
    - Entrambi resi nelle **5 lingue** EN/IT/FR/ES/DE con **check CI presenza di tutte le lingue**. Le traduzioni mantengono
      il **tono di voce** (F1), non parola-per-parola.

11. **Sequenza go-to-market (uovo-gallina sito↔app, richiesto 2026-06-21)**: il sito **non** è "solo homepage" né serve un
    catalogo pieno — serve **UNA prima app reale** (il "seme" del grove). Ordine: (1) **costruisci la prima app** ad almeno
    **MVP/beta** (`new-application`); (2) genera la sua **landing** (dec. 9) + **homepage + pricing + 3 legali**;
    (3) **pubblica il sito statico** (rollout statico-first B4), backoffice "coming soon"; (4) **sottometti a Paddle** con
    un'app concreta (Paddle può chiedere account di test; pricing screenshot ok se non finale). L'uovo-gallina si scioglie
    ricordando che **Paddle serve solo alla monetizzazione**: prima app (beta) → sito che la mostra → Paddle quando è
    pronta a vendere. In fase free, la landing (anche "coming soon"/waitlist) + newsletter girano **senza Paddle**.
    Homepage con **una sola app forte** = normale e onesto ("altri strumenti in arrivo"); non fingere un catalogo vuoto.
12. **SSG = Astro** (con **islands React** per riusare i componenti del design system #03 dove serve interattività, es.
    selettore lingua, form newsletter). Scelto per md/MDX nativo, **SEO/perf top** (zero JS di default → cruciale per
    ranking e GEO), **i18n integrato**. Le 2 SPA restano Vite/React (#03); il sito è artefatto **separato**. Scartato:
    Vite+React SSG (più lavoro su md/SEO/i18n) — accettato il costo di "un framework in più".
13. **Modello contenuti**: **`.md`/MDX = fonte unica**, multilingua; gli **stessi md** servono **sito + rendering in-app**
    delle policy (#13 G). Frontmatter `version`/`effective_date`/`lang` (versioning git-backed). **Check CI: presenza di
    tutte le 5 lingue** per pagina/componente (build rossa se ne manca una).
14. **i18n / routing**: **subpath per lingua** (`appgrove.app/en|it|fr|es|de/…`) + tag **`hreflang`** (standard SEO, più
    semplice/economico di sottodomini/ccTLD). **Default EN**; root `/` → redirect per `Accept-Language` o fallback EN.
15. **Hosting** (conferma B4): **S3 + CloudFront + Route53 + ACM** (~$0–1 free tier); build in **GitHub Actions** (#07) →
    deploy S3 → invalidazione CloudFront. Domini: **`appgrove.app`** (vetrina) + **`app.appgrove.app`** (backoffice).

### F. Brand & identità visiva
F1. **Brand essence**: *"Strumenti semplici che crescono con te — radicati in Europa."* Metafora del nome (app**grove** =
    boschetto **curato** di strumenti che **crescono**, **radicati in Europa** = privacy/EU). **Personalità**: chiaro,
    pratico, affidabile, umano. **Tono di voce**: linguaggio semplice, seconda persona, benefit-first, privacy rassicurante
    (non allarmista), concretezza > superlativi; coerente nelle 5 lingue (EN sorgente marketing, dec. 10).
F2. **Identità visiva = quella GIÀ definita nei mockup Claude Design** ([docs/frontend-design/](frontend-design/), v1
    backoffice + admin/v1, **token coerenti tra loro**) → adottata come **baseline brand** e **set di design token**
    (supera la proposta "verde-primario", scartata perché meno distintiva):
    - **Logo (combination mark)**: **foglia** (Material Symbols `eco`, riempita) in **quadrato ad angoli morbidi**
      (`border-radius ~11px`) colorato d'accent + **wordmark** "appgrove" (peso 800). La foglia porta la semantica grove/crescita/EU.
    - **Palette**: **accent/brand `#ec5a72`** (rosa/corallo caldo); **bg `#f4f4f1`**, surface `#ffffff`/`#fafaf8`/`#f0efeb`;
      **testo `#262420`** + `#6e6b63`/`#a5a199`; bordi `#e9e7e1`/`#dcd9d1`. **Colori-categoria per-app**: green `#3aae73`,
      amber `#dd9b34`, red `#e3654f`, blue `#5b8def`, violet `#8a76f0`, teal `#1fb6a6`. (Neutri caldi + corallo = distintivo
      e umano, coerente con F1; il verde è colore-categoria, non brand.)
    - **Tipografia**: **Plus Jakarta Sans** (corpo/titoli) + **JetBrains Mono** (numeri/conteggi).
    - **I token (variabili CSS del mockup) sono la base diretta del design system #03** e dei token che `new-application`
      usa per generare le landing on-brand (dec. 9). Artwork finale del logo = task di produzione (anche AI-generabile).
F3. **Stile contenuti visivi & brand kit**:
    - **Gerarchia visiva**: **screenshot/mockup UI reali** (primario, on-brand per costruzione, visual più credibile) +
      **icone Material Symbols** (sistema icone ufficiale, già nei mockup) + **illustrazioni minimali custom AI-assistite**
      (dentro uno stile semplice e coerente, secondarie). **No foto stock** (cliché/fuori-brand/licenze).
    - **Brand kit = fonte unica dei token**, condiviso tra backoffice SPA + admin SPA + sito vetrina + landing generate:
      logo (light/dark), color token (accent corallo + neutri caldi + colori-categoria, **tema chiaro e scuro**), type
      scale (Plus Jakarta Sans/JetBrains Mono), radii/ombre/spacing, sistema icone (Material Symbols), nota stile
      illustrazioni. Vive come **pacchetto token condiviso nel monorepo** (base del design system #03).
    - **Per-app**: ogni app ha **icona (Material Symbol) + colore-categoria**, assegnati/chiesti da `new-application` →
      identità per-app riconoscibile dentro il brand ombrello.

### C. Testi legali
16. **Redazione interna AI-assistita** dei tre documenti (T&C, Refund Policy, Privacy Policy + cookie disclosure come
    sezione, non banner — #13 F). Output **md multilingua** (5 lingue), **IT facente fede** sui legali, **EN default sito +
    sorgente marketing** (dec. 10). **Revisione legale solo opzionale pre-go-live** (L2/L3/L13). Tutti **linkati da
    menu/footer** del sito (requisito Paddle A1) e raggiungibili dal backoffice.
17. **Privacy policy modulare + accettazione scoped** (riafferma #13 G dec. 41 — già corretto lì; era imprecisa solo la
    sintesi a voce): **nucleo piattaforma** + **moduli per-app** (generati dai manifesti, #13 C); l'accettazione segue
    **chi è toccato**: piattaforma al signup, modulo app **alla sua attivazione**. **Pubblicare una nuova app Y NON forza
    ri-accettazione** a chi ha solo X. Cambi **materiali** → schermata bloccante **solo agli utenti vincolati** a quel
    componente; cambi minori → **notifica**. T&C = contratto (accettazione esplicita); Privacy = informativa (presa d'atto).
18. **Anello reso esplicito (richiesto 2026-06-21) — rilevazione cambio materiale → versioning → re-accept**: il **gate
    privacy di `new-change`** (#13 C dec. 16) **classifica** un cambio come **MAJOR** (materiale: finalità/basi/categorie/
    retention) vs **MINOR**, e questa classificazione **pilota il bump di versione** del componente PP/ToS interessato
    (#13 G dec. 41) → **ri-accettazione scoped** (major) o **notifica** (minor). Prima il gate aggiornava manifesto/RoPA ma
    il legame col versioning/re-accept non era scritto. Tracciato anche in #13 G41 + [_BACKLOG](_BACKLOG.md) (gate `new-change`).
19. **Privacy alimentata dai manifesti-dati** (#13 C: snippet pubblico minimizzato per-app, fonte unica con RoPA+export);
    **T&C e Refund** = testi base più stabili (+ entità legale, topic D). Coerenza Paddle MoR (#09 J).
20. **Meccanismo di ri-accettazione (richiesto 2026-06-21)**:
    - **Granularità per-UTENTE** (non solo per-tenant): la PP è informativa verso il **soggetto interessato** → in un
      tenant B2B con più utenti, **ciascun utente** prende atto al proprio accesso. Set interessato da un bump major di
      un'app = **tutti gli utenti dei tenant vincolati a quell'app**. (I T&C/contratto possono avere logica per-owner a
      livello account; per la PP è per-utente.)
    - **Meccanismo DERIVATO, non "marcatura di massa"** (coerente con #09 B12, entitlement derivato): ad ogni login/refresh,
      per ogni componente a cui l'utente è vincolato (piattaforma + app del suo tenant) si **confronta versione-accettata
      (dal log di accettazione #13 G41) vs versione-corrente richiesta (major)**; se `accettata < major` → **schermata
      bloccante**. Nessun flag/job di massa da mantenere in sync; chi adotta l'app **dopo** il bump prende già la versione
      corrente all'attivazione. Stesso effetto del "marcare tutti", ma **calcolato** da un'unica fonte di verità.

### D. Entità legale titolare (L11)
21. **Titolare = persona fisica → ditta individuale**: in **fase free** (sito pubblicato, tratti dati → sei già **titolare
    del trattamento** anche senza P.IVA) il titolare è la **persona fisica** (nome legale + indirizzo + email); **alla
    monetizzazione** diventa **ditta individuale con P.IVA** (stessa persona, si aggiunge la P.IVA) → si aggiornano i documenti.
22. **Dati pubblicati** (legali + footer): **nome legale**, **indirizzo (sede)**, **email** (`privacy@`/`support@`/`security@`,
    #13/#13 J), **P.IVA** quando attiva.
23. **Indirizzo/sede = (b) domiciliazione / virtual office** (scelto): indirizzo commerciale a pagamento (poche centinaia
    di €/anno) per **tenere privato l'indirizzo di casa** ed essere più professionale. Da confermare/impostare col
    commercialista (sede ditta individuale) → note in [_COMMERCIALISTA](_COMMERCIALISTA.md).

### G. Contenuti & architettura informativa (IA)
24. **Sitemap + scope MVP di lancio**: **Homepage** + **≥1 landing per-app** + **"Perché appgrove / Privacy & EU"** (storia
    del wedge, forte per GEO/PR) + **"Prezzi — come funziona la fatturazione"** (mensile/annuale, trial, no-refund; il
    prezzo vero sta sulle landing app) + **legali** (Privacy/Terms/Refund/Cookie) + **Support/Contatti** (+ `security.txt`,
    ticketing #13 I) + **Blog/Risorse** (struttura al lancio). **NIENTE founder story / About personale** (scelta utente):
    eventuali mission/valori confluiscono in "Perché appgrove" **senza narrativa personale** (coerente con privacy del founder).
25. **Template landing per-app** (unità ripetibile generata da `new-application`, job-led + privacy wedge E7): (1) Hero
    (headline job + sub-benefit + CTA + screenshot UI); (2) Problema→soluzione; (3) Feature chiave (3–6, icone Material
    Symbols + mini-screenshot); (4) Come funziona (2–3 step); (5) Pricing/tier (mensile/annuale default annuale, trial);
    (6) Badge/sezione privacy EU; (7) FAQ; (8) CTA finale. Base = brand kit (F); multilingua (dec. 9); soggetto al **gate
    di finalizzazione** (screenshot/copy reali, dubbio in Questioni aperte).
26. **Homepage (brand)**: hero (promessa "strumenti semplici che crescono con te") → vetrina app (anche 1, "altri in
    arrivo", onesta col catalogo piccolo dec. 11) → "un account, tanti strumenti" (cross-sell) → sezione privacy/EU (wedge)
    → newsletter → CTA (registrati/esplora).
27. **Navigazione & footer**: top nav = App · Perché appgrove · Prezzi · Blog · **Login** + CTA "Registrati". Footer =
    legali · Support · `security.txt` · newsletter · **selettore lingua** · social.
28. **Blog/Risorse**: **struttura presente dal lancio** (base di SEO/GEO), **contenuti che crescono** nel tempo (guide,
    confronti, how-to su keyword long-tail). Dettaglio editoriale → topic H/I.

## Questioni aperte
- **DUBBIO da riprendere (richiesto 2026-06-21) — gate di finalizzazione della landing**: `new-application` genera la
  pagina web dell'app (dec. 9), ma **gli screenshot dell'app non esistono al momento di `new-application`** (l'app va
  ancora costruita). Quindi la landing nasce **bozza** (testi + placeholder visivi). Serve definire un **gate di
  finalizzazione** — contenuti + screenshot reali completati con una **mia review interattiva** prima della pubblicazione.
  Da decidere: chi/come scatta il gate (step finale di `new-application`? skill separata `finalize-landing`? parte di
  `new-change`?), quali asset richiede (screenshot reali, copy definitivo, OG image), e il check "landing pubblicabile".
  Collegato a dec. 9 e topic **G**.
- Topic **C (testi legali), D (entità legale), G–J (contenuti, SEO, GEO, paid/social)** da affrontare.

## Impatti su altre aree
- **Sblocca #09 (Pagamenti)**: l'attivazione dell'account Paddle dipende da quest'area.
- [03-frontend](03-frontend.md) (terzo artefatto oltre alle 2 SPA), [13-compliance-privacy](13-compliance-privacy.md),
  [_REVISIONE-LEGALE](_REVISIONE-LEGALE.md), [06-infra-iac](06-infra-iac.md) (S3+CloudFront), [07-devops-cicd](07-devops-cicd.md) (build/deploy statico, check CI lingue).
