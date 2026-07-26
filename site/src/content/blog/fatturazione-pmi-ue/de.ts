// Pilastro "Fatturazione per piccole imprese in UE" — tedesco (UC 0042).
import type { PostLocaleContent } from '../types.ts'

export const de: PostLocaleContent = {
  slug: 'rechnungsstellung-kleinunternehmen-eu',
  title: 'Rechnungsstellung für Kleinunternehmen in der EU',
  description:
    'Ein Leitfaden in klarer Sprache zur Rechnungsstellung für Kleinunternehmen in der EU: was eine konforme Rechnung enthalten muss, wie Umsatzsteuer und grenzüberschreitende Verkäufe funktionieren und wie man Belege DSGVO-konform aufbewahrt.',
  question: 'Wie funktioniert die Rechnungsstellung für ein Kleinunternehmen in der EU?',
  intro: [
    'Wer in Europa ein Kleinunternehmen führt, kennt die Rechnungsstellung als den Ort, an dem sich Verwaltung leise auftürmt: Jeder Verkauf braucht ein Dokument, das korrekt, nummeriert, über Jahre aufbewahrt und — zunehmend — elektronisch ist. Ein Fehler bedeutet eine abgelehnte Rechnung, eine verspätete Zahlung oder eine Frage vom Finanzamt, die man nicht beantworten kann.',
    'Dieser Leitfaden ist die Landkarte. Er behandelt, was eine konforme Rechnung enthalten muss, wie die Umsatzsteuer und grenzüberschreitende Verkäufe das Bild verändern und wie man Belege aufbewahrt, ohne die eigenen Daten zum Vermögenswert eines anderen zu machen. Die verlinkten Artikel gehen den beiden häufigsten Fragen auf den Grund.',
  ],
  sections: [
    {
      heading: 'Was eine konforme Rechnung enthalten muss',
      paragraphs: [
        'In der gesamten EU trägt eine Rechnung einen gemeinsamen Kern: wer verkauft und wer kauft, eine eindeutige fortlaufende Nummer, das Datum, eine klare Beschreibung des Verkauften, die Beträge vor und nach Steuer sowie den angewandten Umsatzsteuersatz (oder den Grund für dessen Fehlen). Nationale Regeln fügen Details hinzu, doch dieses Grundgerüst ist dasselbe, ob Sie in Mailand, Madrid oder München fakturieren.',
        'Die Nummer zählt mehr, als es scheint: Sie muss innerhalb Ihres Nummernkreises eindeutig und lückenlos sein, denn so verfolgt das Finanzamt das Dokument. Ein Werkzeug, das sie für Sie vergibt, beseitigt die häufigste Quelle manueller Fehler.',
      ],
    },
    {
      heading: 'Umsatzsteuer und grenzüberschreitende Verkäufe',
      paragraphs: [
        'Die Umsatzsteuer ist der Teil, der zu Fehlern führt. Der Verkauf an ein Unternehmen in einem anderen EU-Land bedeutet oft, dass der Käufer die Steuer schuldet (Reverse-Charge-Verfahren): Ihre Rechnung weist keine Umsatzsteuer aus, muss aber den Grund nennen. Der Verkauf an Verbraucher über Grenzen hinweg kann Sie ab einer Schwelle in das One-Stop-Shop-Verfahren bringen. Die Regeln sind erlernbar, verzeihen aber kein Raten.',
        'Die sichere Gewohnheit ist, die umsatzsteuerliche Behandlung vor dem Versand zu entscheiden, nicht danach. Den Status und das Land Ihres Kunden zu kennen und auf der Rechnung festzuhalten, hält einen grenzüberschreitenden Verkauf bei einer Prüfung sauber.',
      ],
    },
    {
      heading: 'Belege aufbewahren — und sie zu Ihren behalten',
      paragraphs: [
        'Die meisten EU-Länder verlangen, Rechnungen mehrere Jahre aufzubewahren (oft zehn), und viele schreiben die elektronische Rechnung in einem strukturierten Format inzwischen vor oder fördern sie. Ihr Archiv ist also keine Schublade voller Papier: Es sind Daten, und wo diese Daten liegen, ist eine echte Entscheidung.',
        'Hier bezieht appgrove klar Stellung: Ihre Rechnungsdaten werden in der EU gehostet, nach europäischem Recht, mit vollen DSGVO-Rechten und ohne versteckte Tracker. Compliance ist kein Zusatz, den man später anschraubt: Sie ist der Standardrahmen, in dem Ihre Belege vom ersten Tag an liegen.',
      ],
    },
  ],
  faq: {
    title: 'Häufige Fragen zur Rechnungsstellung in der EU',
    items: [
      {
        q: 'Muss ich in der EU elektronische Rechnungen ausstellen?',
        a: 'Das hängt vom Land und vom Kunden ab. Die Rechnungsstellung an die öffentliche Verwaltung ist EU-weit bereits elektronisch, und mehrere Länder weiten die strukturierte elektronische Rechnung auf Verkäufe zwischen Unternehmen aus. Selbst wo Papier noch erlaubt ist, sind elektronische Belege leichter aufzubewahren und nachzuweisen.',
      },
      {
        q: 'Wie lange muss ich meine Rechnungen aufbewahren?',
        a: 'Die meisten EU-Mitgliedstaaten verlangen die Aufbewahrung von Rechnungen über mehrere Jahre — oft zehn. Die genaue Frist wird national festgelegt: Prüfen Sie die Regel Ihres Landes, planen Sie aber eine langfristige, manipulationssichere Aufbewahrung statt loser Dateien ein.',
      },
      {
        q: 'Was passiert, wenn meine Rechnungsnummerierung eine Lücke hat?',
        a: 'Eine Lücke oder ein Duplikat in Ihrer fortlaufenden Nummerierung ist bei einer Prüfung ein Warnsignal, denn über die Reihenfolge prüft das Finanzamt, dass keine Rechnung fehlt. Eine automatische, lückenlose Nummerierung ist der einfachste Weg, das Problem ganz zu vermeiden.',
      },
    ],
  },
  ctaText: 'Sehen Sie, wie appgrove Rechnungen das für Sie übernimmt',
}
