// Landing dell'app #1 "Fatture" (UC 0053) — inglese, lingua sorgente della copy.
//
// Copy rifinita sulle funzionalità e sul listino REALI dell'app (fatture precede la
// skill new-application, quindi la landing è stata scritta a mano). Il listino reale ha
// un solo piano gratuito (10 fatture/mese, nessun trial, nessun piano a pagamento): la
// sezione pricing e le FAQ lo rispecchiano, senza promettere prove o tier inesistenti.
// Landing finalizzata da finalize-landing: screenshot reali e immagine Open Graph
// cablati, stato published (vedi index.ts).
import type { LandingLocaleContent } from '../types.ts'

export const en: LandingLocaleContent = {
  slug: 'invoices',
  meta: {
    title: 'Fatture — invoicing without the admin, hosted in the EU',
    description:
      'Create, send and track invoices in a few clicks. A focused, single-user invoicing app — your data hosted in the EU, with full GDPR rights.',
    ogImage: '/landings/fatture/og.png',
  },
  hero: {
    badge: 'all-EU · GDPR-first',
    title: 'Invoicing, without the admin',
    subtitle:
      'Fatture lets you create, send and track invoices in a few clicks — a focused, single-user app that does one job well, with your data hosted in the EU.',
    ctaPrimary: 'Start for free',
    ctaSecondary: 'See how it works',
    screenshot: {
      src: '/landings/fatture/hero.en.png',
      alt: 'The Fatture invoice list, showing invoice number, customer, status and total',
    },
  },
  problemSolution: {
    title: 'Spend less time on paperwork',
    problem:
      'For freelancers and small businesses, invoicing means fiddly templates, manual numbering and spreadsheets that never quite add up — time taken away from the actual work.',
    solution:
      'Fatture turns it into a few clicks: add a customer, list the items, and the total and the progressive number are handled for you. Nothing to install, no clutter.',
  },
  features: {
    title: 'Everything invoicing needs, nothing it does not',
    subtitle: 'A focused tool that does one job well — no bloated suite, no lock-in.',
    items: [
      {
        icon: 'receipt_long',
        title: 'Invoices in a few clicks',
        body: 'Add a customer and line items — description, quantity, unit price — and Fatture works out the total for you.',
      },
      {
        icon: 'tag',
        title: 'Automatic numbering',
        body: 'A progressive invoice number per year, assigned for you and never reused — tidy and consistent.',
      },
      {
        icon: 'fact_check',
        title: 'Clear status tracking',
        body: 'Move each invoice from draft to issued, paid or voided, so you always know where things stand.',
      },
      {
        icon: 'lock',
        title: 'Hosted in the EU',
        body: 'Your invoices live in the EU, under European law, with full GDPR rights and no hidden trackers.',
      },
    ],
  },
  howItWorks: {
    title: 'Up and running in three steps',
    steps: [
      { title: 'Create your account', body: 'Sign up in seconds — no credit card, the free plan is ready to use.' },
      { title: 'Add your first invoice', body: 'Enter the customer and the line items; the total and the invoice number are filled in for you.' },
      { title: 'Track it to paid', body: 'Issue the invoice and update its status as it moves from draft to paid.' },
    ],
  },
  pricing: {
    title: 'One free plan, no surprises',
    subtitle: 'Fatture is free to use. Create up to 10 invoices each month — no credit card, no trial that expires.',
    monthlyLabel: 'Monthly',
    yearlyLabel: 'Yearly',
    trialNote: 'No credit card, ever. The free plan is yours to keep.',
    tiers: [
      {
        name: 'Free',
        priceMonthly: '€0',
        priceYearly: '€0',
        features: [
          'Up to 10 invoices per month',
          'Customers and line items',
          'Status tracking and automatic numbering',
          'Data hosted in the EU, full GDPR rights',
        ],
        highlighted: true,
      },
    ],
  },
  privacy: {
    title: 'Hosted in the EU. Full GDPR rights.',
    body: 'Your invoices stay in Europe, under European law. You can export or delete your data yourself, at any time — and it is never sold.',
    points: [
      'All data hosted in the EU',
      'Export or delete your data yourself (GDPR)',
      'Invoices kept in line with tax-record obligations',
      'No hidden trackers, no data sold',
    ],
  },
  faq: {
    title: 'Frequently asked questions',
    items: [
      {
        q: 'How much does Fatture cost?',
        a: 'It is free. The current plan lets you create up to 10 invoices per month, with no credit card and no expiring trial.',
      },
      {
        q: 'What happens when I reach 10 invoices in a month?',
        a: 'The monthly count resets at the start of each calendar month, so you can create new invoices again from the next month.',
      },
      {
        q: 'Where is my data stored?',
        a: 'Entirely in the European Union, under European law, with full GDPR rights.',
      },
      {
        q: 'Can I export or delete my data?',
        a: 'Yes. You can export or permanently delete your data yourself from your account, at any time.',
      },
    ],
  },
  finalCta: {
    title: 'Ready to send your first invoice?',
    body: 'Create your account and start invoicing for free — no credit card needed.',
    primary: 'Start for free',
    secondary: 'Why appgrove',
  },
}
