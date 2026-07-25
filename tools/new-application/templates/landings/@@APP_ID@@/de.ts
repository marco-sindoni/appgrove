// Bozza landing di @@APP_NAME@@ — tedesco. Copy generico on-brand: `finalize-landing`
// lo rifinisce. Il badge dell'hero porta il sentinella «DA RIFINIRE».
import type { LandingLocaleContent } from '../types.ts'

export const de: LandingLocaleContent = {
  slug: '@@LANDING_SLUG@@',
  meta: {
    title: '@@APP_NAME@@ — Arbeit erledigen, Datenschutz zuerst',
    description:
      '@@APP_NAME@@ ist eine rein-EU-, DSGVO-first-Micro-App von appgrove: sie macht eine Sache gut, deine Daten sicher in Europa.',
    ogImage: null,
  },
  hero: {
    badge: 'DA RIFINIRE — rein EU · DSGVO-first',
    title: 'Erledige die Arbeit in Minuten, nicht in Nachmittagen',
    subtitle:
      '@@APP_NAME@@ nimmt dir eine wiederkehrende Aufgabe ab — in wenigen Klicks, mit deinen Daten in Europa und vollen DSGVO-Rechten.',
    ctaPrimary: 'Kostenlos testen',
    ctaSecondary: 'So funktioniert’s',
    screenshot: {
      src: null,
      alt: 'Screenshot des @@APP_NAME@@-Dashboards',
    },
  },
  problemSolution: {
    title: 'Weniger Verwaltung, mehr vom Wesentlichen',
    problem:
      'Kleine Teams verlieren Stunden an fummeliger, sich wiederholender Verwaltung — verteilt über Tools, die nie richtig miteinander reden.',
    solution:
      '@@APP_NAME@@ macht eine Sache gut: sie nimmt dir diese Verwaltung ab, in wenigen Klicks, deine Daten sicher in Europa.',
  },
  features: {
    title: 'Alles, was du brauchst, nichts, was du nicht brauchst',
    subtitle: 'Fokussierte Funktionen, die eine Sache gut machen — keine überladene Suite, kein Lock-in.',
    items: [
      {
        icon: 'bolt',
        title: 'Schnell von Haus aus',
        body: 'In Minuten eingerichtet; alltägliche Aufgaben in ein paar Klicks erledigt.',
      },
      {
        icon: 'lock',
        title: 'Privat by Design',
        body: 'Deine Daten leben in der EU, nach europäischem Recht, mit vollen DSGVO-Rechten.',
      },
      {
        icon: 'sync',
        title: 'Ein Konto, alle Tools',
        body: 'Füge weitere appgrove-Apps hinzu, wenn du sie brauchst — gleicher Login, gleiches vertrautes Zuhause.',
      },
      {
        icon: 'smart_toy',
        title: 'Bereit für KI',
        body: 'Dafür gebaut, von deinem KI-Assistenten erreicht zu werden, damit die Arbeit aus dem Chat läuft, den du schon nutzt.',
      },
    ],
  },
  howItWorks: {
    title: 'In drei Schritten startklar',
    steps: [
      { title: 'Konto erstellen', body: 'In Sekunden registrieren — keine Karte für den Teststart.' },
      { title: 'Arbeitsbereich einrichten', body: 'Eine geführte Einrichtung macht dich ab Tag eins produktiv.' },
      { title: 'Arbeit erledigen', body: 'Mach, was zu tun ist, und lass @@APP_NAME@@ die Verwaltung aus dem Weg räumen.' },
    ],
  },
  pricing: {
    title: 'Einfache, faire Preise',
    subtitle: 'Wähle den passenden Plan. Jährlich kostet weniger; monatlich gibt Flexibilität.',
    monthlyLabel: 'Monatlich',
    yearlyLabel: 'Jährlich',
    trialNote: 'Jeder kostenpflichtige Plan startet mit 14 Tagen Gratis-Test — bis dahin wird nichts berechnet.',
    tiers: [
      {
        name: 'Starter',
        priceMonthly: '0 €',
        priceYearly: '0 €',
        features: ['Zum Einstieg', 'Grundfunktionen', 'Community-Support'],
      },
      {
        name: 'Pro',
        priceMonthly: '00 € / Monat',
        priceYearly: '000 € / Jahr',
        features: ['Alles aus Starter', 'Alle Funktionen', 'Vorrangiger Support'],
        highlighted: true,
      },
    ],
  },
  privacy: {
    title: 'In der EU gehostet. Volle DSGVO-Rechte.',
    body: 'Datenschutz ist hier kein Zusatz — so ist appgrove gebaut. Deine Daten bleiben in Europa und werden nie verkauft.',
    points: ['Alle Daten in der EU gehostet', 'Volle DSGVO-Rechte, by Design', 'Keine versteckten Tracker, kein Datenverkauf'],
  },
  faq: {
    title: 'Häufige Fragen',
    items: [
      {
        q: 'Brauche ich eine Karte, um @@APP_NAME@@ zu testen?',
        a: 'Nein. Der 14-tägige Gratis-Test startet ohne Karte; du zahlst nur, wenn du fortfahren willst.',
      },
      {
        q: 'Wo werden meine Daten gespeichert?',
        a: 'Vollständig in der Europäischen Union, nach europäischem Recht, mit vollen DSGVO-Rechten.',
      },
      {
        q: 'Kann ich jederzeit kündigen?',
        a: 'Ja. Du kannst jederzeit aus deinem Konto kündigen; der Plan läuft bis zum Ende des bezahlten Zeitraums.',
      },
    ],
  },
  finalCta: {
    title: 'Bereit loszulegen?',
    body: 'Erstelle dein Konto und teste @@APP_NAME@@ 14 Tage gratis.',
    primary: 'Kostenlos testen',
    secondary: 'Warum appgrove',
  },
}
