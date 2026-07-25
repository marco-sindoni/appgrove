// Bozza landing di @@APP_NAME@@ — inglese, lingua sorgente marketing (UC 0046 → UC 0057).
// Copy generico on-brand: `finalize-landing` lo rifinisce sulle feature reali. Il badge
// dell'hero porta il sentinella «DA RIFINIRE» finché la copy non è rivista.
import type { LandingLocaleContent } from '../types.ts'

export const en: LandingLocaleContent = {
  slug: '@@LANDING_SLUG@@',
  meta: {
    title: '@@APP_NAME@@ — get the job done, privacy-first',
    description:
      '@@APP_NAME@@ is an all-EU, GDPR-first micro-app from appgrove: it does one job well, with your data safe in Europe.',
    ogImage: null,
  },
  hero: {
    badge: 'DA RIFINIRE — all-EU · GDPR-first',
    title: 'Get the job done in minutes, not afternoons',
    subtitle:
      '@@APP_NAME@@ takes one recurring task off your plate — in a few clicks, with your data hosted in Europe under full GDPR rights.',
    ctaPrimary: 'Start free trial',
    ctaSecondary: 'See how it works',
    screenshot: {
      src: null,
      alt: 'Screenshot of the @@APP_NAME@@ dashboard',
    },
  },
  problemSolution: {
    title: 'Less admin, more of what matters',
    problem:
      'Small teams lose hours to fiddly, repetitive admin — spread across tools that never quite talk to each other.',
    solution:
      '@@APP_NAME@@ does one job well: it takes that admin off your plate, in a few clicks, with your data safe in Europe.',
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
      { title: 'Get the job done', body: 'Do the work, and let @@APP_NAME@@ keep the admin out of your way.' },
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
        q: 'Do I need a credit card to try @@APP_NAME@@?',
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
    body: 'Create your account and try @@APP_NAME@@ free for 14 days.',
    primary: 'Start free trial',
    secondary: 'Why appgrove',
  },
}
