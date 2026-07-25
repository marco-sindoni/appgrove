// Contenuti marketing — inglese (lingua sorgente, UC 0037).
// Tono lean/semplice, job-led + privacy come firma di fiducia (#14 E7, F1).
import type { MarketingContent } from './types.ts'

export const en: MarketingContent = {
  nav: {
    app: 'Apps',
    why: 'Why appgrove',
    pricing: 'Pricing',
    login: 'Log in',
    signup: 'Sign up',
  },
  hero: {
    badge: 'all-EU · GDPR-first',
    title: 'Simple tools that grow with your business',
    subtitle:
      'An ecosystem of fast, affordable apps for small and medium businesses. Spend less time on admin and more time on what you do best.',
    ctaPrimary: 'Get started',
    ctaSecondary: 'Explore the apps',
  },
  apps: {
    title: 'One account, every tool you need',
    subtitle: 'Focused apps that each do one job well — no bloated suites, no lock-in.',
    comingSoon: 'Coming soon',
    flagshipName: 'Invoicing',
    flagshipCategory: 'Invoicing & billing',
    flagshipDescription:
      'Create and send compliant invoices in minutes, keep track of payments, and stay on top of your books.',
    moreTitle: 'More tools on the way',
    moreText:
      'The catalogue is small on purpose, and growing. New apps join the ecosystem regularly — and one account unlocks every one of them.',
  },
  crossSell: {
    title: 'One account, many tools',
    body: 'As the ecosystem grows, your account grows with it. Add a new tool when you need it — same login, same trusted home for your data.',
    points: [
      'A single sign-in for every app',
      'A consistent, simple experience across tools',
      'Add tools as your business grows',
    ],
  },
  ai: {
    title: 'Ready for AI',
    body: 'Every appgrove app is designed to be reachable by your AI assistant. Ask in plain language in ChatGPT, Claude or Perplexity — and let it get the job done inside your app.',
    points: [
      'Built on MCP (Model Context Protocol), the open standard AI assistants use to call external tools',
      'Do the work straight from the chat you already use',
      'Your tools, reachable where you already work',
    ],
    note: 'This is how we design the ecosystem — we are building towards it today, app by app.',
  },
  privacy: {
    title: 'Hosted in the EU. Full GDPR rights.',
    body: 'Your data lives in Europe, under European law. Privacy is not an add-on here — it is how appgrove is built.',
    points: [
      'All data hosted in the EU',
      'Full GDPR rights, by design',
      'No hidden trackers, no data sold',
    ],
  },
  newsletter: {
    title: 'Stay in the loop',
    body: 'Get a note when new apps land. No spam, and you can unsubscribe anytime.',
    placeholder: 'you@email.com',
    cta: 'Notify me',
    note: 'We will only ever email you about appgrove. Sign-up opens soon.',
  },
  finalCta: {
    title: 'Ready to get started?',
    body: 'Create your account and explore the ecosystem — free to try.',
    primary: 'Get started',
    secondary: 'Why appgrove',
  },
  why: {
    title: 'Why appgrove',
    intro:
      'We build simple, focused tools for small and medium businesses in Europe. Four promises shape everything we make.',
    sections: [
      {
        title: 'Tools that give you time back',
        body: 'Small businesses do not need bloated suites. They need tools that do one job well, quickly, at a fair price. Every appgrove app is built to take the admin off your plate so you can focus on your business.',
      },
      {
        title: 'One growing ecosystem',
        body: 'One account, many tools. As your needs grow, the ecosystem grows with you — add a new app when you need it, without new logins or new silos.',
      },
      {
        title: 'Ready for AI',
        body: 'We design every app to be reachable by your AI assistant, through MCP — the open standard AI assistants use to call external tools. The vision: you ask in plain language, and the work gets done inside your app. We are building towards this today, app by app.',
      },
      {
        title: 'European by design, private by default',
        body: 'Your data is hosted in the EU, under European law, with full GDPR rights. Privacy is the foundation, not a feature — no hidden trackers, and your data is never sold.',
      },
    ],
  },
  pricing: {
    title: 'Pricing — how billing works',
    intro:
      'Each app sets its own price on its own page. Here is how billing works across appgrove.',
    sections: [
      {
        title: 'Per-app pricing',
        body: 'You pay only for the tools you use. Each app shows its plans and prices on its own page — pick what fits your business.',
      },
      {
        title: 'Monthly or yearly',
        body: 'Choose the billing cycle that suits you. Yearly is the default and costs less over the year; monthly gives you flexibility.',
      },
      {
        title: '14-day free trial',
        body: 'Try before you pay. Every paid plan starts with a 14-day free trial — nothing is charged until it ends.',
      },
      {
        title: 'No refunds',
        body: 'Because every plan starts with a free trial, subscriptions are non-refundable once billed. The details are in our Refund Policy.',
      },
    ],
    refundLinkText: 'Read the Refund Policy',
  },
  footer: {
    tagline: 'Simple tools that grow with your business.',
    legalHeading: 'Legal',
    supportLabel: 'Support',
    securityLabel: 'Security',
    newsletterTitle: 'Newsletter',
    newsletterBody: 'New apps, straight to your inbox.',
    newsletterPlaceholder: 'you@email.com',
    newsletterCta: 'Notify me',
    rights: 'appgrove — all-EU, GDPR-first.',
  },
}
