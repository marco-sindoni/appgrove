// Fixture di esempio (UC 0038) — tedesco.
import type { LandingLocaleContent } from '../types.ts'

export const de: LandingLocaleContent = {
  slug: 'beispiel',
  meta: {
    title: 'Beispiel — die appgrove-Landing-Vorlage',
    description:
      'Demonstration der wiederverwendbaren Landing-Vorlage pro App: acht markengerechte Abschnitte, fünf Sprachen, Entwurfs- und Veröffentlicht-Status.',
    ogImage: null,
  },
  hero: {
    badge: 'komplett EU · DSGVO zuerst',
    title: 'Erledige die Arbeit in Minuten, nicht in ganzen Nachmittagen',
    subtitle:
      'Beispiel ist die Demo-App der appgrove-Landing-Vorlage. Sie zeigt, wie jede App ihre eigene Geschichte erzählt — die Aufgabe zuerst, Datenschutz als Vertrauenssignatur.',
    ctaPrimary: 'Kostenlose Testphase starten',
    ctaSecondary: 'So funktioniert es',
    screenshot: {
      src: null,
      alt: 'Screenshot des Dashboards der Beispiel-App',
    },
  },
  problemSolution: {
    title: 'Weniger Verwaltung, mehr vom Wesentlichen',
    problem:
      'Kleine Teams verlieren Stunden an mühsame, sich wiederholende Verwaltung — verteilt auf Tools, die nie wirklich miteinander reden.',
    solution:
      'Beispiel macht eine Sache gut: Es nimmt dir diese Verwaltung ab, mit wenigen Klicks, deine Daten sicher in Europa.',
  },
  features: {
    title: 'Alles, was du brauchst, nichts Überflüssiges',
    subtitle: 'Fokussierte Funktionen, die eine Aufgabe gut erledigen — keine aufgeblähte Suite, keine Bindung.',
    items: [
      {
        icon: 'bolt',
        title: 'Schnell von Haus aus',
        body: 'In Minuten eingerichtet und tägliche Aufgaben mit ein paar Klicks erledigt.',
      },
      {
        icon: 'lock',
        title: 'Privat by Design',
        body: 'Deine Daten leben in der EU, nach europäischem Recht, mit vollen DSGVO-Rechten.',
      },
      {
        icon: 'sync',
        title: 'Ein Konto, alle Werkzeuge',
        body: 'Füge weitere appgrove-Apps hinzu, wenn du sie brauchst — gleiche Anmeldung, gleiches vertrautes Zuhause.',
      },
      {
        icon: 'smart_toy',
        title: 'Bereit für KI',
        body: 'Dafür gebaut, von deinem KI-Assistenten erreichbar zu sein, damit die Arbeit im Chat erledigt wird, den du schon nutzt.',
      },
    ],
  },
  howItWorks: {
    title: 'In drei Schritten startklar',
    steps: [
      { title: 'Konto erstellen', body: 'Registrierung in Sekunden — keine Karte, um die Testphase zu starten.' },
      { title: 'Arbeitsbereich einrichten', body: 'Eine geführte Einrichtung macht dich vom ersten Tag an produktiv.' },
      { title: 'Arbeit erledigen', body: 'Erledige deine Arbeit und lass Beispiel die Verwaltung aus dem Weg räumen.' },
    ],
  },
  pricing: {
    title: 'Einfache, faire Preise',
    subtitle: 'Wähle den passenden Tarif. Jährlich kostet weniger; monatlich gibt Flexibilität.',
    monthlyLabel: 'Monatlich',
    yearlyLabel: 'Jährlich',
    trialNote: 'Jeder kostenpflichtige Tarif beginnt mit 14 Tagen kostenloser Testphase — bis zum Ende wird nichts berechnet.',
    tiers: [
      {
        name: 'Starter',
        priceMonthly: '0 €',
        priceYearly: '0 €',
        features: ['Zum Loslegen', 'Kernfunktionen', 'Community-Support'],
      },
      {
        name: 'Pro',
        priceMonthly: '00 € / Monat',
        priceYearly: '000 € / Jahr',
        features: ['Alles aus Starter', 'Alle Funktionen', 'Bevorzugter Support'],
        highlighted: true,
      },
    ],
  },
  privacy: {
    title: 'In der EU gehostet. Volle DSGVO-Rechte.',
    body: 'Datenschutz ist hier kein Zusatz — so ist appgrove gebaut. Deine Daten bleiben in Europa und werden nie verkauft.',
    points: ['Alle Daten in der EU gehostet', 'Volle DSGVO-Rechte, by Design', 'Keine versteckten Tracker, keine Datenverkäufe'],
  },
  faq: {
    title: 'Häufige Fragen',
    items: [
      {
        q: 'Brauche ich eine Karte, um Beispiel zu testen?',
        a: 'Nein. Die 14-tägige kostenlose Testphase startet ohne Karte; du zahlst nur, wenn du dich zum Fortfahren entscheidest.',
      },
      {
        q: 'Wo werden meine Daten gespeichert?',
        a: 'Vollständig in der Europäischen Union, nach europäischem Recht, mit vollen DSGVO-Rechten.',
      },
      {
        q: 'Kann ich jederzeit kündigen?',
        a: 'Ja. Du kannst jederzeit in deinem Konto kündigen; der Tarif läuft bis zum Ende des bezahlten Zeitraums.',
      },
    ],
  },
  finalCta: {
    title: 'Bereit loszulegen?',
    body: 'Erstelle dein Konto und teste Beispiel 14 Tage kostenlos.',
    primary: 'Kostenlose Testphase starten',
    secondary: 'Warum appgrove',
  },
}
