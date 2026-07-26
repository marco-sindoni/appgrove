// Articolo cluster "Fattura elettronica a norma" — tedesco (UC 0042).
import type { PostLocaleContent } from '../types.ts'

export const de: PostLocaleContent = {
  slug: 'konforme-elektronische-rechnung',
  title: 'Wie man eine konforme elektronische Rechnung erstellt',
  description:
    'Eine Schritt-für-Schritt-Antwort: was eine elektronische Rechnung in der EU konform macht, welche Felder sie tragen muss und wie man sie versendet und aufbewahrt, damit sie einer Prüfung standhält.',
  question: 'Wie erstelle ich eine konforme elektronische Rechnung?',
  intro: [
    'Eine elektronische Rechnung ist nicht einfach ein PDF, das man per E-Mail verschickt. Im Sinne der EU ist sie eine Rechnung, die in einem strukturierten elektronischen Format ausgestellt, übermittelt und empfangen wird, das ein Computer automatisch verarbeiten kann — und konform zu sein bedeutet, dass sie die richtigen Felder trägt und so aufbewahrt wird, dass sie nicht unbemerkt verändert werden kann.',
    'Hier die kurze, praktische Fassung: was hineingehört, wie man sie versendet und wie man sie aufbewahrt. Nichts davon ist schwer, sobald man es immer gleich macht — genau dafür ist ein Werkzeug da.',
  ],
  sections: [
    {
      heading: 'Die Felder, die sie tragen muss',
      paragraphs: [
        'Beginnen Sie beim Pflichtkern: Ihre Angaben und die Ihres Kunden, eine eindeutige fortlaufende Nummer, das Ausstellungsdatum, eine klare zeilenweise Beschreibung, der Nettobetrag, der Umsatzsteuersatz und -betrag (oder der Befreiungsgrund) und die Summe. Für Geschäftskunden ist in vielen Ländern zusätzlich deren Umsatzsteuer-Identifikationsnummer nötig; bei grenzüberschreitenden Verkäufen zwischen Unternehmen der Hinweis, dass das Reverse-Charge-Verfahren gilt.',
        'Fehlt eines davon, kann die Rechnung abgelehnt oder beanstandet werden. Der Vorteil, sie aus strukturierten Daten — Kunde, Produkt, Steuerregel — zu befüllen statt neu zu tippen, ist, dass dieselben Felder jedes Mal korrekt herauskommen.',
      ],
    },
    {
      heading: 'Sie im richtigen Format versenden',
      paragraphs: [
        'Konformität bedeutet zunehmend ein strukturiertes Format, nicht das Bild einer Rechnung. Die Rechnungsstellung an die öffentliche Verwaltung in der EU nutzt bereits strukturierte elektronische Formate, und mehrere Länder leiten Rechnungen zwischen Unternehmen über eine nationale Plattform oder ein Kontrollsystem, bevor sie den Kunden erreichen.',
        'Die praktische Erkenntnis: Prüfen Sie, ob Ihr Land oder Ihr Kunde einen bestimmten Kanal verlangt, und erzeugen Sie die Rechnung in einem Format, das dieser Kanal akzeptiert. Ein Werkzeug, das das strukturierte Format für Sie ausgibt, macht aus einer Compliance-Frage ein Nicht-Ereignis.',
      ],
    },
    {
      heading: 'Sie so aufbewahren, dass sie standhält',
      paragraphs: [
        'Eine konforme elektronische Rechnung muss über die gesamte Aufbewahrungsfrist — oft zehn Jahre — so aufbewahrt werden, dass ihre Echtheit und Unversehrtheit gewahrt bleiben. Im Klartext: Sie müssen nachweisen können, dass sie seit der Ausstellung nicht verändert wurde, und sie auf Anfrage vorlegen.',
        'Das ist ebenso eine Aufbewahrungs- wie eine Formatentscheidung. Mit appgrove werden Ihre Rechnungen in der EU archiviert, nach europäischem Recht, mit vollen DSGVO-Rechten — sodass der Beleg, auf den Sie sich in Jahren stützen, weiterhin Ihnen gehört und unter Ihrer Kontrolle bleibt.',
      ],
    },
  ],
  faq: {
    title: 'Fragen zu konformen elektronischen Rechnungen',
    items: [
      {
        q: 'Ist ein PDF eine elektronische Rechnung?',
        a: 'Nicht im strengen Sinne der EU. Ein PDF ist ein für Menschen lesbares Bild; eine konforme elektronische Rechnung wird in einem strukturierten Format ausgestellt, das ein Computer automatisch verarbeiten kann. Wo das strukturierte Format vorgeschrieben ist, genügt ein bloßes PDF nicht.',
      },
      {
        q: 'Was ist der häufigste Fehler?',
        a: 'Eine Lücke in der fortlaufenden Nummerierung oder ein fehlendes Pflichtfeld wie die umsatzsteuerliche Behandlung bei einem grenzüberschreitenden Verkauf. Beides lässt sich vermeiden, wenn die Rechnung aus strukturierten Daten erstellt statt jedes Mal von Hand getippt wird.',
      },
      {
        q: 'Wie weise ich nach, dass eine Rechnung nicht verändert wurde?',
        a: 'Indem Sie sie in einem System aufbewahren, das ihre Unversehrtheit über die gesamte Aufbewahrungsfrist wahrt und sie auf Anfrage unverändert vorlegen kann. Entscheidend ist nicht eine einzelne Technik, sondern ein Aufbewahrungskonzept, das bei einer Prüfung Bestand hat.',
      },
    ],
  },
  ctaText: 'Erstellen Sie konforme Rechnungen mit appgrove Rechnungen',
}
