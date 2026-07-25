// Forma unica dei contenuti marketing del sito vetrina (UC 0037).
//
// I contenuti marketing sono STRUTTURATI (etichette di navigazione, blocchi con
// titolo/testo/punti, sezioni di pagina), diversi dai documenti-prosa legali che
// restano markdown (content/legal/*.md). Qui il tipo condiviso più l'indice
// Record<Locale, MarketingContent> (index.ts) garantiscono la PARITÀ delle 5
// lingue a tempo di COMPILAZIONE: una lingua mancante o una forma diversa non
// compila. Il controllo post-build e il test vitest coprono poi i VALORI.
//
// Pilastri del messaggio (change 0047): (1) ecosistema di app comode/veloci/a
// costi sostenibili — più tempo sul business; (2) un'unica piattaforma, ecosistema
// in crescita; (3) "Ready for AI" (app richiamabili dagli assistenti AI via MCP —
// visione onesta, non disponibilità già attiva); (4) tutto in EU, pieni diritti GDPR.

/** Etichette della navigazione (top nav). Gli URL non sono tradotti (costanti). */
export interface NavContent {
  app: string
  why: string
  pricing: string
  login: string
  signup: string
}

/** Sezione generica titolo + testo (pagine "Perché appgrove" e "Prezzi"). */
export interface Section {
  title: string
  body: string
}

/** Blocco tematico della homepage: titolo, testo, punti brevi, nota opzionale. */
export interface FeatureBlock {
  title: string
  body: string
  points: string[]
  /** Nota di onestà/inquadramento (es. il pilastro AI è visione, non ancora attivo). */
  note?: string
}

export interface HeroContent {
  badge: string
  title: string
  subtitle: string
  /** Etichetta CTA primaria → registrazione (SPA esterna). */
  ctaPrimary: string
  /** Etichetta CTA secondaria → ancora alla vetrina app (#apps). */
  ctaSecondary: string
}

/** Vetrina app in homepage, onesta col catalogo piccolo (#14 11/26). */
export interface AppsShowcase {
  title: string
  subtitle: string
  /** Badge "in arrivo" sullo strumento faro. */
  comingSoon: string
  flagshipName: string
  flagshipCategory: string
  flagshipDescription: string
  moreTitle: string
  moreText: string
}

export interface NewsletterContent {
  title: string
  body: string
  placeholder: string
  cta: string
  /** Testo del consenso accanto alla checkbox (obbligatoria, NON pre-spuntata) — UC 0039. */
  consentLabel: string
  /** Messaggio dopo l'invio accettato: double opt-in, conferma via email. */
  success: string
  /** Messaggio d'errore generico (risposta non 202 o rete non disponibile). */
  error: string
  /** Nota accanto al form (onestà/privacy). */
  note: string
}

export interface FinalCta {
  title: string
  body: string
  primary: string
  secondary: string
}

/** Voce FAQ machine-readable (UC 0041, GEO): usata per il rendering e per FAQPage JSON-LD. */
export interface FaqItem {
  q: string
  a: string
}

/** Sezione FAQ (domanda/risposta): resa in pagina e come dato strutturato FAQPage. */
export interface FaqSection {
  title: string
  items: FaqItem[]
}

export interface WhyPage {
  title: string
  intro: string
  sections: Section[]
  /** FAQ della pagina "perché" (ecosistema, AI, UE) — GEO UC 0041. */
  faq: FaqSection
}

export interface PricingPage {
  title: string
  intro: string
  sections: Section[]
  /** Testo del link alla Refund Policy (documento legale già presente). */
  refundLinkText: string
  /** FAQ della pagina "prezzi" (fatturazione) — GEO UC 0041. */
  faq: FaqSection
}

export interface FooterContent {
  tagline: string
  legalHeading: string
  supportLabel: string
  securityLabel: string
  newsletterTitle: string
  newsletterBody: string
  newsletterPlaceholder: string
  newsletterCta: string
  /** Testo dopo "© <anno>" nel copyright (l'anno è iniettato dal layout). */
  rights: string
}

export interface MarketingContent {
  nav: NavContent
  hero: HeroContent
  apps: AppsShowcase
  crossSell: FeatureBlock
  ai: FeatureBlock
  privacy: FeatureBlock
  /** FAQ generale di marca resa in homepage (+ FAQPage JSON-LD) — GEO UC 0041. */
  faq: FaqSection
  newsletter: NewsletterContent
  finalCta: FinalCta
  why: WhyPage
  pricing: PricingPage
  footer: FooterContent
}
