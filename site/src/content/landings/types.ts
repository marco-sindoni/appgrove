// Forma unica delle landing per-app del sito vetrina (UC 0038).
//
// Una landing è l'unità RIPETIBILE di acquisizione a livello app (#14 25): copy
// strutturato in 8 sezioni, on-brand (design system UC 0019), nelle 5 lingue.
// Come per i contenuti marketing (UC 0037) i contenuti sono STRUTTURATI (campi
// tipizzati), non prosa: il tipo `Record<Locale, LandingLocaleContent>` forza a
// COMPILE-TIME la parità delle 5 lingue.
//
// Due stati (#14 51/52): `draft` (bozza generata da `new-application`, con
// placeholder al posto degli screenshot) e `published` (finalizzata da
// `finalize-landing` con screenshot reali + immagine Open Graph). Il build
// renderizza SOLO le landing `published` (gate strutturale in lib/landings.ts).

import type { Locale } from '../../lib/i18n.ts'

export type LandingStatus = 'draft' | 'published'

/**
 * Screenshot dell'app. In bozza `src` è `null` → il template mostra un riquadro
 * placeholder on-brand; `finalize-landing` cattura l'immagine reale e riempie `src`
 * (#14 51, punto 1). `alt` è sempre presente (accessibilità).
 */
export interface Screenshot {
  src: string | null
  alt: string
}

/** Meta di pagina: titolo, descrizione, immagine Open Graph (null in bozza). */
export interface LandingMeta {
  /** <title> senza il suffisso "· appgrove", aggiunto dal layout. */
  title: string
  /** meta description + og:description. */
  description: string
  /** og:image; null in bozza (l'immagine OG la genera `finalize-landing`, #14 51). */
  ogImage: string | null
}

/** (1) Hero: headline orientata al job + sotto-beneficio + CTA + screenshot. */
export interface HeroSection {
  badge: string
  title: string
  subtitle: string
  ctaPrimary: string
  ctaSecondary: string
  screenshot: Screenshot
}

/** (2) Problema → soluzione. */
export interface ProblemSolutionSection {
  title: string
  problem: string
  solution: string
}

/** Voce della sezione feature: icona Material Symbols + titolo + testo. */
export interface Feature {
  /** Nome dell'icona Material Symbols (es. "bolt", "lock", "sync"). */
  icon: string
  title: string
  body: string
}

/** (3) Feature chiave (3–6, icone Material Symbols). */
export interface FeaturesSection {
  title: string
  subtitle: string
  items: Feature[]
}

/** Passo della sezione "come funziona". */
export interface Step {
  title: string
  body: string
}

/** (4) Come funziona (2–3 step numerati). */
export interface HowItWorksSection {
  title: string
  steps: Step[]
}

/**
 * Livello di prezzo. I numeri VERI arrivano dal pricing-as-code (#09) tramite le
 * skill; qui sono campi di contenuto (placeholder nella fixture di esempio).
 */
export interface PricingTier {
  name: string
  priceMonthly: string
  priceYearly: string
  features: string[]
  /** Tier evidenziato nel confronto. */
  highlighted?: boolean
}

/** (5) Pricing/tier: mensile/annuale (default annuale) + prova gratuita. */
export interface PricingSection {
  title: string
  subtitle: string
  monthlyLabel: string
  /** Etichetta del ciclo annuale, che è il default (#14 25). */
  yearlyLabel: string
  /** Nota sulla prova gratuita (14 giorni, #09). */
  trialNote: string
  tiers: PricingTier[]
}

/** (6) Badge/sezione privacy EU — firma di fiducia (wedge #14 E7). */
export interface PrivacySection {
  title: string
  body: string
  points: string[]
}

/** Voce FAQ. */
export interface FaqItem {
  q: string
  a: string
}

/** (7) FAQ. */
export interface FaqSection {
  title: string
  items: FaqItem[]
}

/** (8) CTA finale. */
export interface FinalCtaSection {
  title: string
  body: string
  primary: string
  secondary: string
}

/** Contenuto di una landing in UNA lingua: slug localizzato + meta + 8 sezioni. */
export interface LandingLocaleContent {
  /** Slug localizzato (#14 31): usato nell'URL /<lang>/<slug>/. Minuscolo, [a-z0-9-]. */
  slug: string
  meta: LandingMeta
  hero: HeroSection
  problemSolution: ProblemSolutionSection
  features: FeaturesSection
  howItWorks: HowItWorksSection
  pricing: PricingSection
  privacy: PrivacySection
  faq: FaqSection
  finalCta: FinalCtaSection
}

/**
 * Landing di un'app: identità e stato a livello APP (non per-lingua, così non può
 * divergere), più il contenuto nelle 5 lingue. Il tipo Record<Locale, …> garantisce
 * la parità delle lingue a compile-time.
 */
export interface Landing {
  appId: string
  status: LandingStatus
  content: Record<Locale, LandingLocaleContent>
}
