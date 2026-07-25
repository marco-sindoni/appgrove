// Landing dell'app #1 "Fatture" (UC 0053) — tedesco.
import type { LandingLocaleContent } from '../types.ts'

export const de: LandingLocaleContent = {
  slug: 'rechnungen',
  meta: {
    title: 'Fatture — Rechnungen ohne den Papierkram, gehostet in der EU',
    description:
      'Erstelle, versende und verfolge Rechnungen mit wenigen Klicks. Eine schlanke Einzelnutzer-App für Rechnungen — deine Daten in der EU gehostet, mit vollen DSGVO-Rechten.',
    ogImage: '/landings/fatture/og.png',
  },
  hero: {
    badge: 'alles in der EU · DSGVO-first',
    title: 'Rechnungen, ohne den Papierkram',
    subtitle:
      'Mit Fatture erstellst, versendest und verfolgst du Rechnungen mit wenigen Klicks — eine schlanke App, die eine Sache gut macht, mit deinen Daten in der EU gehostet.',
    ctaPrimary: 'Kostenlos starten',
    ctaSecondary: 'So funktioniert es',
    screenshot: {
      src: '/landings/fatture/hero.de.png',
      alt: 'Die Rechnungsliste von Fatture mit Nummer, Kunde, Status und Summe',
    },
  },
  problemSolution: {
    title: 'Weniger Zeit für Papierkram',
    problem:
      'Für Freiberufler und kleine Betriebe bedeutet Rechnungsstellung umständliche Vorlagen, manuelle Nummerierung und Tabellen, die nie ganz aufgehen — Zeit, die von der eigentlichen Arbeit abgeht.',
    solution:
      'Fatture macht daraus wenige Klicks: Kunde hinzufügen, Positionen auflisten, und die Summe und die fortlaufende Nummer werden für dich berechnet. Nichts zu installieren, kein Ballast.',
  },
  features: {
    title: 'Alles, was Rechnungen brauchen, nichts Überflüssiges',
    subtitle: 'Ein schlankes Werkzeug, das eine Aufgabe gut erledigt — keine überladene Suite, keine Bindung.',
    items: [
      {
        icon: 'receipt_long',
        title: 'Rechnungen mit wenigen Klicks',
        body: 'Füge einen Kunden und Positionen hinzu — Beschreibung, Menge, Einzelpreis — und Fatture berechnet die Summe für dich.',
      },
      {
        icon: 'tag',
        title: 'Automatische Nummerierung',
        body: 'Eine fortlaufende Rechnungsnummer pro Jahr, für dich vergeben und nie wiederverwendet — ordentlich und konsistent.',
      },
      {
        icon: 'fact_check',
        title: 'Immer klarer Status',
        body: 'Bring jede Rechnung von Entwurf zu ausgestellt, bezahlt oder storniert, damit du immer weißt, wo du stehst.',
      },
      {
        icon: 'lock',
        title: 'In der EU gehostet',
        body: 'Deine Rechnungen liegen in der EU, nach europäischem Recht, mit vollen DSGVO-Rechten und ohne versteckte Tracker.',
      },
    ],
  },
  howItWorks: {
    title: 'In drei Schritten startklar',
    steps: [
      { title: 'Konto erstellen', body: 'Registrierung in Sekunden — ohne Karte, der kostenlose Plan ist sofort einsatzbereit.' },
      { title: 'Erste Rechnung anlegen', body: 'Gib den Kunden und die Positionen ein; die Summe und die Rechnungsnummer werden für dich ausgefüllt.' },
      { title: 'Bis „bezahlt“ verfolgen', body: 'Stelle die Rechnung aus und aktualisiere ihren Status, während sie von Entwurf zu bezahlt wandert.' },
    ],
  },
  pricing: {
    title: 'Ein kostenloser Plan, keine Überraschungen',
    subtitle: 'Fatture ist kostenlos. Erstelle bis zu 10 Rechnungen pro Monat — ohne Karte, ohne ablaufende Testphase.',
    monthlyLabel: 'Monatlich',
    yearlyLabel: 'Jährlich',
    trialNote: 'Nie eine Karte nötig. Der kostenlose Plan bleibt deiner.',
    tiers: [
      {
        name: 'Kostenlos',
        priceMonthly: '0 €',
        priceYearly: '0 €',
        features: [
          'Bis zu 10 Rechnungen pro Monat',
          'Kunden und Rechnungspositionen',
          'Statusverfolgung und automatische Nummerierung',
          'Daten in der EU gehostet, volle DSGVO-Rechte',
        ],
        highlighted: true,
      },
    ],
  },
  privacy: {
    title: 'In der EU gehostet. Volle DSGVO-Rechte.',
    body: 'Deine Rechnungen bleiben in Europa, nach europäischem Recht. Du kannst deine Daten selbst exportieren oder löschen, jederzeit — und sie werden nie verkauft.',
    points: [
      'Alle Daten in der EU gehostet',
      'Exportiere oder lösche deine Daten selbst (DSGVO)',
      'Rechnungen aufbewahrt gemäß den steuerlichen Pflichten',
      'Keine versteckten Tracker, kein Datenverkauf',
    ],
  },
  faq: {
    title: 'Häufige Fragen',
    items: [
      {
        q: 'Was kostet Fatture?',
        a: 'Es ist kostenlos. Der aktuelle Plan erlaubt bis zu 10 Rechnungen pro Monat, ohne Karte und ohne ablaufende Testphase.',
      },
      {
        q: 'Was passiert, wenn ich 10 Rechnungen im Monat erreiche?',
        a: 'Der Monatszähler wird zu Beginn jedes Kalendermonats zurückgesetzt, sodass du ab dem nächsten Monat wieder neue Rechnungen erstellen kannst.',
      },
      {
        q: 'Wo werden meine Daten gespeichert?',
        a: 'Vollständig in der Europäischen Union, nach europäischem Recht, mit vollen DSGVO-Rechten.',
      },
      {
        q: 'Kann ich meine Daten exportieren oder löschen?',
        a: 'Ja. Du kannst deine Daten jederzeit selbst aus deinem Konto exportieren oder endgültig löschen.',
      },
    ],
  },
  finalCta: {
    title: 'Bereit, deine erste Rechnung zu senden?',
    body: 'Erstelle dein Konto und starte kostenlos mit dem Rechnungen — ohne Karte.',
    primary: 'Kostenlos starten',
    secondary: 'Warum appgrove',
  },
}
