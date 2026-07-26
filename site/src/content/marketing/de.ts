// Contenuti marketing — tedesco (UC 0037). Tono lean, job-led + privacy come firma.
import type { MarketingContent } from './types.ts'

export const de: MarketingContent = {
  nav: {
    app: 'Apps',
    why: 'Warum appgrove',
    pricing: 'Preise',
    blog: 'Blog',
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
  faq: {
    title: 'Häufige Fragen',
    items: [
      {
        q: 'Was ist appgrove?',
        a: 'appgrove ist ein komplett europäischer, DSGVO-orientierter Marktplatz fokussierter Micro-SaaS-Apps für kleine und mittlere Unternehmen. Ein einziges Konto schaltet ein wachsendes Ökosystem einfacher, bezahlbarer Werkzeuge frei, von denen jedes eine Sache gut erledigt.',
      },
      {
        q: 'Wo werden meine Daten gespeichert?',
        a: 'Alle Daten von appgrove werden in der Europäischen Union gehostet, nach europäischem Recht. Es gibt keine versteckten Tracker, und Ihre Daten werden nie verkauft.',
      },
      {
        q: 'Habe ich volle DSGVO-Rechte?',
        a: 'Ja. DSGVO-Rechte sind von Grund auf eingebaut — Sie können Ihre Daten einsehen, exportieren und löschen. Datenschutz ist das Fundament, kein Zusatz.',
      },
      {
        q: 'Wie funktioniert die Preisgestaltung?',
        a: 'Jede App legt ihren Preis auf ihrer eigenen Seite fest. Sie zahlen nur für die Werkzeuge, die Sie nutzen, monatlich oder jährlich, und jeder kostenpflichtige Tarif beginnt mit einer 14-tägigen kostenlosen Testphase.',
      },
      {
        q: 'Kann ich appgrove-Apps mit einem KI-Assistenten nutzen?',
        a: 'So gestalten wir das Ökosystem: Jede App ist darauf ausgelegt, von KI-Assistenten über MCP (Model Context Protocol) aufgerufen zu werden. Wir arbeiten heute darauf hin, App für App.',
      },
    ],
  },
  newsletter: {
    title: 'Bleiben Sie informiert',
    body: 'Wir melden uns, wenn neue Apps erscheinen. Kein Spam, jederzeit abbestellbar.',
    placeholder: 'sie@email.com',
    cta: 'Benachrichtigen',
    consentLabel: 'Ich willige ein, den appgrove-Newsletter zu erhalten. Kein Spam, Abmeldung mit einem Klick.',
    success: 'Fast geschafft: Prüfen Sie Ihr E-Mail-Postfach und bestätigen Sie Ihre Anmeldung.',
    error: 'Etwas ist schiefgelaufen. Bitte versuchen Sie es erneut.',
    note: 'Wir schreiben Ihnen ausschließlich zu appgrove. Jederzeit abbestellbar.',
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
    faq: {
      title: 'Mehr über appgrove',
      items: [
        {
          q: 'Was unterscheidet appgrove von einer großen Software-Suite?',
          a: 'appgrove ist ein Ökosystem kleiner, fokussierter Apps statt einer einzigen überladenen Suite. Jedes Werkzeug erledigt eine Sache gut, und ein einziges Konto gibt Ihnen alle — keine Bindung, keine Silos.',
        },
        {
          q: 'Brauche ich ein separates Konto für jede App?',
          a: 'Nein. Ein einziges Konto schaltet jede App des Ökosystems frei, mit einer einzigen Anmeldung und einem vertrauenswürdigen Zuhause für Ihre Daten.',
        },
        {
          q: 'Was bedeutet „Bereit für KI“?',
          a: 'Jede App ist so gestaltet, dass Ihr KI-Assistent sie über MCP aufrufen kann — den offenen Standard, mit dem Assistenten externe Werkzeuge erreichen. Das ist unsere Design-Richtung, App für App gebaut.',
        },
        {
          q: 'Ist appgrove wirklich komplett in der EU?',
          a: 'Ja. Die Daten werden in der EU nach europäischem Recht gehostet, mit vollen DSGVO-Rechten, ohne versteckte Tracker und ohne Datenverkauf — das Vertrauensfundament hinter allem, was wir bauen.',
        },
      ],
    },
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
        title: 'Jederzeit kündbar',
        body: 'Jeder Tarif beginnt mit einer kostenlosen Testphase, sodass Sie ohne Risiko entscheiden, bevor Sie zahlen — deshalb werden bereits berechnete Beträge nicht zurückerstattet. In jedem Fall können Sie Ihr Abonnement jederzeit kündigen: Ab dem nächsten Abrechnungszeitraum wird Ihnen nichts mehr berechnet, und der laufende Zeitraum bleibt bis zu seinem Ende aktiv. Die Einzelheiten stehen in unserer Refund Policy.',
      },
    ],
    refundLinkText: 'Refund Policy lesen',
    faq: {
      title: 'Fragen zur Abrechnung',
      items: [
        {
          q: 'Was kostet appgrove?',
          a: 'Es gibt keinen einheitlichen Preis: Jede App zeigt ihre eigenen Tarife und Preise auf ihrer Seite. Sie zahlen nur für die Werkzeuge, die Sie nutzen.',
        },
        {
          q: 'Monatlich oder jährlich?',
          a: 'Beides. Jährlich ist die Voreinstellung und kostet übers Jahr weniger; monatlich gibt mehr Flexibilität. Sie wählen pro App.',
        },
        {
          q: 'Gibt es eine kostenlose Testphase?',
          a: 'Ja. Jeder kostenpflichtige Tarif beginnt mit einer 14-tägigen kostenlosen Testphase — bis zu deren Ende wird nichts berechnet.',
        },
        {
          q: 'Kann ich eine Rückerstattung erhalten?',
          a: 'Jeder Tarif beginnt mit einer kostenlosen Testphase, sodass Sie ohne Risiko testen, bevor Sie zahlen — deshalb werden bereits berechnete Beträge nicht zurückerstattet. Sie können Ihr Abonnement jedoch jederzeit kündigen: Ab dem nächsten Abrechnungszeitraum wird Ihnen nichts mehr berechnet, und der laufende Zeitraum bleibt bis zu seinem Ende aktiv. Die Einzelheiten stehen in unserer Refund Policy.',
        },
      ],
    },
  },
  footer: {
    tagline: 'Einfache Werkzeuge, die mit Ihrem Unternehmen wachsen.',
    legalHeading: 'Rechtliches',
    socialHeading: 'Folge uns',
    supportLabel: 'Support',
    securityLabel: 'Sicherheit',
    newsletterTitle: 'Newsletter',
    newsletterBody: 'Neue Apps, direkt in Ihr Postfach.',
    newsletterPlaceholder: 'sie@email.com',
    newsletterCta: 'Benachrichtigen',
    rights: 'appgrove — komplett in der EU, DSGVO zuerst.',
  },
}
