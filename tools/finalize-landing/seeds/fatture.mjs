// Descrittore di seed screenshot per l'app #1 "Fatture" (UC 0053).
//
// `fatture` precede lo scaffold di new-application e diverge dalla forma generica: la
// sua lista è la risorsa `invoices` (campi number/customerName/issueDate/status/
// totalAmount), non `items`. Questo descrittore allinea gli screenshot di
// finalize-landing alle rotte reali del modulo — la stessa forma della prova e2e
// (frontend/apps/backoffice/e2e/fatture.spec.ts) — così la cattura mostra una lista
// realistica invece dello stato vuoto. Metric/tetto rispecchiano il listino reale
// (services/core/.../pricing/fatture.yaml): 10 fatture/mese.
export const seed = {
  listPath: 'invoices',
  metric: 'fatture',
  freeCap: 10,
  records: [
    { id: 'inv-1', number: '2026-0001', customerName: 'Studio Verdi S.r.l.', issueDate: '2026-01-12', status: 'paid', currency: 'EUR', totalAmount: 1220 },
    { id: 'inv-2', number: '2026-0002', customerName: 'Anna Bianchi', issueDate: '2026-01-20', status: 'issued', currency: 'EUR', totalAmount: 340 },
    { id: 'inv-3', number: '2026-0003', customerName: 'Officine Rossi', issueDate: '2026-02-03', status: 'issued', currency: 'EUR', totalAmount: 890 },
    { id: 'inv-4', number: '2026-0004', customerName: 'Luca Neri', issueDate: '2026-02-11', status: 'draft', currency: 'EUR', totalAmount: 150 },
  ],
}
