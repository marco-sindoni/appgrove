// Stringhe del modulo **fatture** in inglese — fonte del design del modulo (UC 0060). Le altre lingue
// (it/fr/es/de) ne sono la traduzione e sono tipizzate su questa forma, così la parità è imposta dal compilatore.
export const en = {
  appName: 'Invoices',
  sectionInvoices: 'Invoices',
  title: 'Invoices',
  subtitle: 'The invoices for your account.',

  newInvoice: 'New invoice',
  backToList: '← Back to invoices',

  // Colonne tabella
  colNumber: 'Number',
  colCustomer: 'Customer',
  colIssueDate: 'Date',
  colStatus: 'Status',
  colTotal: 'Total',
  colActions: 'Actions',

  empty: 'No invoices yet: create one with “New invoice”.',

  // Stati fattura
  status: {
    draft: 'Draft',
    issued: 'Issued',
    paid: 'Paid',
    voided: 'Voided',
  },

  // Editor
  editorTitle: 'New invoice',
  customerSection: 'Customer',
  fieldCustomerName: 'Customer name',
  fieldCustomerEmail: 'Customer email (optional)',
  linesTitle: 'Lines',
  fieldLineDescription: 'Description',
  fieldLineQuantity: 'Quantity',
  fieldLineUnitAmount: 'Unit amount',
  addLine: 'Add line',
  removeLine: 'Remove',
  save: 'Create invoice',
  cancel: 'Cancel',

  // Dettaglio
  detailTitle: 'Invoice detail',
  changeStatus: 'Change status',
  delete: 'Delete',
  confirmDeleteTitle: 'Delete the invoice?',
  confirmDeleteBody: 'This action cannot be undone from the UI.',

  // Quota
  quotaLabel: 'Invoices this month',
  quotaReached: 'You have reached your plan’s monthly limit.',
  quotaUpgrade: 'Upgrade your plan',

  // Errori
  errorGeneric: 'Something went wrong. Please try again.',
  errorQuota: 'Monthly limit reached: upgrade to create more invoices.',
  required: 'This field is required',
}

/** Forma delle stringhe del modulo (chiavi tipizzate): it/fr/es/de la implementano. */
export type FattureMessages = typeof en
