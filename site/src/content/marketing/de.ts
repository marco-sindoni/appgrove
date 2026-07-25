// Contenuti marketing — tedesco (UC 0037). Tono lean, job-led + privacy come firma.
import type { MarketingContent } from './types.ts'

export const de: MarketingContent = {
  nav: {
    app: 'Apps',
    why: 'Warum appgrove',
    pricing: 'Preise',
    login: 'Anmelden',
    signup: 'Registrieren',
  },
  hero: {
    badge: 'komplett in der EU · DSGVO zuerst',
    title: 'Einfache Werkzeuge, die mit Ihrem Unternehmen wachsen',
    subtitle:
      'Ein Ökosystem schneller, bezahlbarer Apps für kleine und mittlere Unternehmen. Weniger Zeit für Verwaltung, mehr Zeit für das, was Sie am besten können.',
    ctaPrimary: 'Loslegen',
    ctaSecondary: 'Apps entdecken',
  },
  apps: {
    title: 'Ein Konto, alle Werkzeuge, die Sie brauchen',
    subtitle: 'Fokussierte Apps, die jede eine Sache gut erledigen — keine überladenen Suiten, keine Bindung.',
    comingSoon: 'Demnächst',
    flagshipName: 'Rechnungen',
    flagshipCategory: 'Rechnungen & Zahlungen',
    flagshipDescription:
      'Erstellen und versenden Sie konforme Rechnungen in Minuten, behalten Sie Zahlungen im Blick und Ihre Buchhaltung im Griff.',
    moreTitle: 'Weitere Werkzeuge sind unterwegs',
    moreText:
      'Der Katalog ist bewusst klein und wächst. Regelmäßig kommen neue Apps ins Ökosystem — und ein einziges Konto schaltet sie alle frei.',
  },
  crossSell: {
    title: 'Ein Konto, viele Werkzeuge',
    body: 'Während das Ökosystem wächst, wächst Ihr Konto mit. Fügen Sie ein neues Werkzeug hinzu, wenn Sie es brauchen — dieselbe Anmeldung, dasselbe vertrauenswürdige Zuhause für Ihre Daten.',
    points: [
      'Eine einzige Anmeldung für alle Apps',
      'Ein einheitliches, einfaches Erlebnis über alle Werkzeuge',
      'Werkzeuge hinzufügen, während Ihr Unternehmen wächst',
    ],
  },
  ai: {
    title: 'Bereit für KI',
    body: 'Jede appgrove-App ist so konzipiert, dass Ihr KI-Assistent sie aufrufen kann. Fragen Sie in natürlicher Sprache in ChatGPT, Claude oder Perplexity — und lassen Sie die Aufgabe in Ihrer App erledigen.',
    points: [
      'Aufgebaut auf MCP (Model Context Protocol), dem offenen Standard, mit dem KI-Assistenten externe Werkzeuge aufrufen',
      'Erledigen Sie die Arbeit direkt aus dem Chat, den Sie ohnehin nutzen',
      'Ihre Werkzeuge, erreichbar dort, wo Sie schon arbeiten',
    ],
    note: 'So gestalten wir das Ökosystem — wir arbeiten heute darauf hin, App für App.',
  },
  privacy: {
    title: 'In der EU gehostet. Volle DSGVO-Rechte.',
    body: 'Ihre Daten leben in Europa, nach europäischem Recht. Datenschutz ist hier kein Zusatz: Er ist die Art, wie appgrove gebaut ist.',
    points: [
      'Alle Daten in der EU gehostet',
      'Volle DSGVO-Rechte, von Grund auf',
      'Keine versteckten Tracker, kein Datenverkauf',
    ],
  },
  newsletter: {
    title: 'Bleiben Sie informiert',
    body: 'Wir melden uns, wenn neue Apps erscheinen. Kein Spam, jederzeit abbestellbar.',
    placeholder: 'sie@email.com',
    cta: 'Benachrichtigen',
    note: 'Wir schreiben Ihnen ausschließlich zu appgrove. Die Anmeldung öffnet bald.',
  },
  finalCta: {
    title: 'Bereit loszulegen?',
    body: 'Erstellen Sie Ihr Konto und erkunden Sie das Ökosystem — kostenlos testen.',
    primary: 'Loslegen',
    secondary: 'Warum appgrove',
  },
  why: {
    title: 'Warum appgrove',
    intro:
      'Wir bauen einfache, fokussierte Werkzeuge für kleine und mittlere Unternehmen in Europa. Vier Versprechen prägen alles, was wir machen.',
    sections: [
      {
        title: 'Werkzeuge, die Ihnen Zeit zurückgeben',
        body: 'Kleine Unternehmen brauchen keine überladenen Suiten. Sie brauchen Werkzeuge, die eine Sache gut erledigen, schnell und zu einem fairen Preis. Jede appgrove-App ist darauf ausgelegt, Ihnen die Verwaltung abzunehmen, damit Sie sich auf Ihr Geschäft konzentrieren können.',
      },
      {
        title: 'Ein wachsendes Ökosystem',
        body: 'Ein Konto, viele Werkzeuge. Wenn Ihre Anforderungen wachsen, wächst das Ökosystem mit Ihnen — fügen Sie eine neue App hinzu, wenn Sie sie brauchen, ohne neue Anmeldungen oder neue Silos.',
      },
      {
        title: 'Bereit für KI',
        body: 'Wir gestalten jede App so, dass Ihr KI-Assistent sie erreichen kann — über MCP, den offenen Standard, mit dem KI-Assistenten externe Werkzeuge aufrufen. Die Vision: Sie fragen in natürlicher Sprache, und die Arbeit wird in Ihrer App erledigt. Wir arbeiten heute darauf hin, App für App.',
      },
      {
        title: 'Europäisch by design, privat als Standard',
        body: 'Ihre Daten werden in der EU gehostet, nach europäischem Recht, mit vollen DSGVO-Rechten. Datenschutz ist das Fundament, keine Funktion — keine versteckten Tracker, und Ihre Daten werden nie verkauft.',
      },
    ],
  },
  pricing: {
    title: 'Preise — wie die Abrechnung funktioniert',
    intro:
      'Jede App legt ihren Preis auf ihrer eigenen Seite fest. So funktioniert die Abrechnung bei appgrove.',
    sections: [
      {
        title: 'Preis pro App',
        body: 'Sie zahlen nur für die Werkzeuge, die Sie nutzen. Jede App zeigt ihre Tarife und Preise auf ihrer eigenen Seite — wählen Sie, was zu Ihrem Unternehmen passt.',
      },
      {
        title: 'Monatlich oder jährlich',
        body: 'Wählen Sie den Abrechnungszeitraum, der Ihnen passt. Jährlich ist die Voreinstellung und kostet übers Jahr weniger; monatlich gibt Ihnen Flexibilität.',
      },
      {
        title: '14 Tage kostenlos testen',
        body: 'Testen Sie, bevor Sie zahlen. Jeder kostenpflichtige Tarif beginnt mit einer 14-tägigen kostenlosen Testphase — bis zu deren Ende wird nichts berechnet.',
      },
      {
        title: 'Keine Rückerstattungen',
        body: 'Da jeder Tarif mit einer kostenlosen Testphase beginnt, sind Abonnements nach der Abrechnung nicht erstattungsfähig. Die Einzelheiten stehen in unserer Refund Policy.',
      },
    ],
    refundLinkText: 'Refund Policy lesen',
  },
  footer: {
    tagline: 'Einfache Werkzeuge, die mit Ihrem Unternehmen wachsen.',
    legalHeading: 'Rechtliches',
    supportLabel: 'Support',
    securityLabel: 'Sicherheit',
    newsletterTitle: 'Newsletter',
    newsletterBody: 'Neue Apps, direkt in Ihr Postfach.',
    newsletterPlaceholder: 'sie@email.com',
    newsletterCta: 'Benachrichtigen',
    rights: 'appgrove — komplett in der EU, DSGVO zuerst.',
  },
}
