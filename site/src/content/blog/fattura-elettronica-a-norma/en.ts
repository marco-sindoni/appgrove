// Articolo cluster "Fattura elettronica a norma" — inglese (lingua sorgente, UC 0042).
import type { PostLocaleContent } from '../types.ts'

export const en: PostLocaleContent = {
  slug: 'compliant-electronic-invoice',
  title: 'How to create a compliant electronic invoice',
  description:
    'A step-by-step answer: what makes an electronic invoice compliant in the EU, the fields it must carry, and how to send and store it so it holds up at audit.',
  question: 'How do I create a compliant electronic invoice?',
  intro: [
    'An electronic invoice is not just a PDF you email. In the EU sense, it is an invoice issued, transmitted and received in a structured electronic format that a computer can process automatically — and being compliant means it carries the right fields and is stored so it cannot be quietly altered.',
    'Here is the short, practical version: what to put on it, how to send it, and how to keep it. None of it is hard once you do it the same way every time — which is exactly what a tool is for.',
  ],
  sections: [
    {
      heading: 'The fields it must carry',
      paragraphs: [
        'Start from the mandatory core: your details and your customer’s, a unique sequential number, the issue date, a clear line-by-line description, the taxable amount, the VAT rate and amount (or the exemption reason), and the total. For business customers in many countries you also need their tax identification number, and for cross-border business sales the note that the reverse charge applies.',
        'Miss one of these and the invoice can be rejected or challenged. The value of filling them from structured data — customer, product, tax rule — rather than retyping is that the same fields come out right every time.',
      ],
    },
    {
      heading: 'Sending it in the right format',
      paragraphs: [
        'Compliance increasingly means a structured format, not a picture of an invoice. Business-to-government invoicing across the EU already uses structured electronic formats, and several countries route business invoices through a national platform or clearance system before they reach the customer.',
        'The practical takeaway: check whether your country or your customer requires a specific channel, and generate the invoice in a format that channel accepts. A tool that outputs the structured format for you turns a compliance question into a non-event.',
      ],
    },
    {
      heading: 'Storing it so it holds up',
      paragraphs: [
        'A compliant electronic invoice must be kept for the full retention period — often ten years — in a way that preserves its authenticity and integrity. In plain terms: you must be able to prove it has not been changed since it was issued, and produce it on request.',
        'That is a storage decision as much as a formatting one. With appgrove your invoices are archived in the EU, under European law, with full GDPR rights — so the record you rely on years from now is one you still own and control.',
      ],
    },
  ],
  faq: {
    title: 'Questions about compliant e-invoices',
    items: [
      {
        q: 'Is a PDF invoice an electronic invoice?',
        a: 'Not in the strict EU sense. A PDF is a human-readable image; a compliant electronic invoice is issued in a structured format a computer can process automatically. Where a structured format is required, a PDF alone will not satisfy it.',
      },
      {
        q: 'What is the single most common mistake?',
        a: 'A break in the sequential numbering, or a missing mandatory field such as the VAT treatment on a cross-border sale. Both are avoidable when the invoice is built from structured data rather than typed by hand each time.',
      },
      {
        q: 'How do I prove an invoice has not been altered?',
        a: 'By storing it in a system that preserves its integrity for the whole retention period and can produce it unchanged on request. The point is not a single technique but a storage setup you can stand behind at audit.',
      },
    ],
  },
  ctaText: 'Create compliant invoices with appgrove Invoicing',
}
