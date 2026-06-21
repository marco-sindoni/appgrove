# Sito vetrina & testi legali (ToU/PP) — Decisioni

**Stato**: 🔴 da definire
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
- **A. Requisiti Paddle per l'attivazione** — checklist esatta di cosa Paddle pretende sul sito (pagine, contenuti, ToU,
  PP, contatti, descrizione prodotto/prezzi, refund policy) → ricerca puntuale sui requisiti merchant Paddle.
- **B. Architettura del sito** — SSG/Vite statico, S3+CloudFront, multilingua EN/IT/FR/ES/DE, contenuti `.md` come fonte
  unica (sito + rendering in-app delle policy), versioning (`effective_date`/commit), check CI 5 lingue.
- **C. Testi legali** — ToU/ToS, Privacy Policy, cookie disclosure (IT facente fede); coordinamento con #13 e revisione
  legale pre-go-live (L2/L3); minimizzazione informativa (#13/_BACKLOG).
- **D. Entità legale titolare (L11)** — prerequisito business: ditta/società, indirizzo, contatti (serve a PP e a Paddle MoR).
- **E. Marketing/posizionamento** — valorizzazione "all-EU deployed / garanzie privacy UE" (#13 I, nota _BACKLOG); newsletter (#13 F).
- **F. Contenuti di prodotto** — descrizione marketplace/app, prezzi, FAQ, supporto, `security.txt` (#13 J).

## Decisioni prese
_Nessuna ancora._

## Questioni aperte
_Da elencare all'avvio dell'argomento._

## Impatti su altre aree
- **Sblocca #09 (Pagamenti)**: l'attivazione dell'account Paddle dipende da quest'area.
- [03-frontend](03-frontend.md) (terzo artefatto oltre alle 2 SPA), [13-compliance-privacy](13-compliance-privacy.md),
  [_REVISIONE-LEGALE](_REVISIONE-LEGALE.md), [06-infra-iac](06-infra-iac.md) (S3+CloudFront), [07-devops-cicd](07-devops-cicd.md) (build/deploy statico, check CI lingue).
