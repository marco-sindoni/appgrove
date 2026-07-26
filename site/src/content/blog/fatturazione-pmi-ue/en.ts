// Pilastro "Fatturazione per piccole imprese in UE" — inglese (lingua sorgente, UC 0042).
import type { PostLocaleContent } from '../types.ts'

export const en: PostLocaleContent = {
  slug: 'invoicing-small-business-eu',
  title: 'Invoicing for small businesses in the EU',
  description:
    'A plain-language guide to invoicing for small businesses in the EU: what a compliant invoice needs, how VAT and cross-border rules work, and how to keep records the GDPR-friendly way.',
  question: 'How does invoicing work for a small business in the EU?',
  intro: [
    'If you run a small business in Europe, invoicing is where admin quietly piles up: every sale needs a document that is correct, numbered, kept for years, and — increasingly — electronic. Get it wrong and you risk a rejected invoice, a late payment, or a tax question you cannot answer.',
    'This guide is the map. It covers what a compliant invoice must contain, how value-added tax (VAT) and cross-border sales change the picture, and how to keep your records without turning your data into someone else’s asset. The linked articles go deeper on the two questions people ask most.',
  ],
  sections: [
    {
      heading: 'What a compliant invoice must contain',
      paragraphs: [
        'Across the EU, an invoice carries a common core: who is selling and who is buying, a unique sequential number, the date, a clear description of what was sold, the amounts before and after tax, and the VAT rate applied (or the reason none is). National rules add details on top, but that backbone is the same whether you invoice in Milan, Madrid or Munich.',
        'The number matters more than it looks: it must be unique and gap-free within your numbering series, because it is how tax authorities trace the document. A tool that assigns it for you removes the single most common source of manual error.',
      ],
    },
    {
      heading: 'VAT and cross-border sales',
      paragraphs: [
        'Value-added tax is the part that trips people up. Selling to a business in another EU country often means the buyer accounts for the tax (the reverse-charge mechanism), so your invoice shows no VAT but must say why. Selling to consumers across borders can pull you into the One-Stop-Shop scheme once you pass a threshold. The rules are learnable, but they are unforgiving of guesswork.',
        'The safe habit is to decide the VAT treatment before you send, not after. Knowing your customer’s status and country, and recording it on the invoice, is what keeps a cross-border sale clean at audit time.',
      ],
    },
    {
      heading: 'Keeping records — and keeping them yours',
      paragraphs: [
        'Most EU countries require you to keep invoices for several years (often ten), and many now mandate or encourage electronic invoicing in a structured format. That means your archive is not a drawer of paper — it is data, and where that data lives is a real decision.',
        'This is where appgrove takes a clear side: your invoicing data is hosted in the EU, under European law, with full GDPR rights and no hidden trackers. Compliance is not an add-on you bolt on later; it is the default your records sit in from day one.',
      ],
    },
  ],
  faq: {
    title: 'Common questions about EU invoicing',
    items: [
      {
        q: 'Do I have to issue electronic invoices in the EU?',
        a: 'It depends on the country and the customer. Business-to-government invoicing is already electronic across the EU, and several countries are extending structured electronic invoicing to business-to-business sales. Even where paper is still allowed, electronic records are easier to keep and to prove.',
      },
      {
        q: 'How long must I keep my invoices?',
        a: 'Most EU member states require invoices to be retained for several years — often ten. The exact period is set nationally, so check your country’s rule, but plan for long-term, tamper-evident storage rather than ad-hoc files.',
      },
      {
        q: 'What happens if my invoice numbering has a gap?',
        a: 'A gap or a duplicate in your sequential numbering is a red flag at audit, because the sequence is how the tax authority checks that no invoice is missing. Automatic, gap-free numbering is the simplest way to avoid the problem entirely.',
      },
    ],
  },
  ctaText: 'See how appgrove Invoicing handles this for you',
}
