// Fixture di esempio (UC 0038) — inglese, lingua sorgente.
//
// NON è la landing di un'app reale: è la landing "example" in stato `draft` che
// collauda il template e i test, senza pubblicare contenuti finti (le bozze non
// vanno online, #14 52) e senza anticipare la landing dell'app #1 (UC 0053).
// Screenshot = null (placeholder in bozza); og:image = null (la genera
// `finalize-landing`, UC 0057). I prezzi sono placeholder: i numeri veri arrivano
// dal pricing-as-code (#09) tramite le skill.
import type { LandingLocaleContent } from '../types.ts'

export const en: LandingLocaleContent = {
  slug: 'example',
  meta: {
    title: 'Example — the appgrove landing template',
    description:
      'A demonstration of the repeatable per-app landing template: eight on-brand sections, five languages, draft and published states.',
    ogImage: null,
  },
  hero: {
    badge: 'all-EU · GDPR-first',
    title: 'Get the job done in minutes, not afternoons',
    subtitle:
      'Example is the demonstration app for the appgrove landing template. It shows how each app tells its own story — job first, privacy as the signature of trust.',
    ctaPrimary: 'Start free trial',
    ctaSecondary: 'See how it works',
    screenshot: {
      src: null,
      alt: 'Screenshot of the Example app dashboard',
    },
  },
  problemSolution: {
    title: 'Less admin, more of what matters',
    problem:
      'Small teams lose hours to fiddly, repetitive admin — spread across tools that never quite talk to each other.',
    solution:
      'Example does one job well: it takes that admin off your plate, in a few clicks, with your data safe in Europe.',
  },
  features: {
    title: 'Everything you need, nothing you do not',
    subtitle: 'Focused features that do one job well — no bloated suite, no lock-in.',
    items: [
      {
        icon: 'bolt',
        title: 'Fast by default',
        body: 'Get set up in minutes and finish everyday tasks in a couple of clicks.',
      },
      {
        icon: 'lock',
        title: 'Private by design',
        body: 'Your data lives in the EU, under European law, with full GDPR rights.',
      },
      {
        icon: 'sync',
        title: 'One account, every tool',
        body: 'Add other appgrove apps when you need them — same login, same trusted home.',
      },
      {
        icon: 'smart_toy',
        title: 'Ready for AI',
        body: 'Designed to be reachable by your AI assistant, so the work gets done from the chat you already use.',
      },
    ],
  },
  howItWorks: {
    title: 'Up and running in three steps',
    steps: [
      { title: 'Create your account', body: 'Sign up in seconds — no credit card to start the trial.' },
      { title: 'Set up your workspace', body: 'A guided setup gets you productive on day one.' },
      { title: 'Get the job done', body: 'Do the work, and let Example keep the admin out of your way.' },
    ],
  },
  pricing: {
    title: 'Simple, fair pricing',
    subtitle: 'Pick the plan that fits. Yearly costs less; monthly gives you flexibility.',
    monthlyLabel: 'Monthly',
    yearlyLabel: 'Yearly',
    trialNote: 'Every paid plan starts with a 14-day free trial — nothing is charged until it ends.',
    tiers: [
      {
        name: 'Starter',
        priceMonthly: '€0',
        priceYearly: '€0',
        features: ['For getting started', 'Core features', 'Community support'],
      },
      {
        name: 'Pro',
        priceMonthly: '€00 / mo',
        priceYearly: '€000 / yr',
        features: ['Everything in Starter', 'All features', 'Priority support'],
        highlighted: true,
      },
    ],
  },
  privacy: {
    title: 'Hosted in the EU. Full GDPR rights.',
    body: 'Privacy is not an add-on here — it is how appgrove is built. Your data stays in Europe, and it is never sold.',
    points: ['All data hosted in the EU', 'Full GDPR rights, by design', 'No hidden trackers, no data sold'],
  },
  faq: {
    title: 'Frequently asked questions',
    items: [
      {
        q: 'Do I need a credit card to try Example?',
        a: 'No. The 14-day free trial starts without a card; you only pay if you decide to continue.',
      },
      {
        q: 'Where is my data stored?',
        a: 'Entirely in the European Union, under European law, with full GDPR rights.',
      },
      {
        q: 'Can I cancel anytime?',
        a: 'Yes. You can cancel from your account at any time; your plan runs to the end of the paid period.',
      },
    ],
  },
  finalCta: {
    title: 'Ready to get started?',
    body: 'Create your account and try Example free for 14 days.',
    primary: 'Start free trial',
    secondary: 'Why appgrove',
  },
}
