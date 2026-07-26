/**
 * Stringhe del modulo **@@APP_NAME@@** in inglese — fonte del design del modulo (UC 0060). Le altre
 * lingue (it/fr/es/de) ne sono la traduzione, tipizzate su questa forma (la parità la impone il compilatore).
 *
 * Generate dallo scaffolding sul dominio segnaposto: vanno riscritte col linguaggio reale dell'app
 * (un "record" non si chiama record in nessun prodotto vero).
 */
export const en = {
  appName: '@@APP_NAME@@',
  sectionItems: '@@APP_NAME@@',
  title: '@@APP_NAME@@',
  subtitle: 'Your account records.',

  newItem: 'New record',
  backToList: '← Back to the list',

  // Colonne tabella
  colCode: 'Code',
  colContact: 'Contact',
  colRecordedOn: 'Date',
  colStatus: 'Status',
  colTotal: 'Total',
  colActions: 'Actions',

  empty: 'No records yet: create one with “New record”.',

  // Stati
  status: {
    draft: 'Draft',
    active: 'Active',
    done: 'Done',
    archived: 'Archived',
  },

  // Editor
  editorTitle: 'New record',
  contactSection: 'Contact',
  fieldContactName: 'Contact name',
  fieldContactEmail: 'Contact email (optional)',
  linesTitle: 'Lines',
  fieldLineDescription: 'Description',
  fieldLineQuantity: 'Quantity',
  fieldLineUnitAmount: 'Unit amount',
  addLine: 'Add line',
  removeLine: 'Remove',
  save: 'Create record',
  cancel: 'Cancel',

  // Dettaglio
  detailTitle: 'Record detail',
  changeStatus: 'Change status',
  delete: 'Delete',
  confirmDeleteTitle: 'Delete the record?',
  confirmDeleteBody: 'This action cannot be undone from the UI.',

  // Quota (il testo arriva dal listino: segnaposto sostituiti dal generatore, uguali in ogni lingua finché non riscritti)
  quotaLabel: '@@QUOTA_LABEL@@',
  quotaReached: '@@QUOTA_REACHED@@',
  quotaUpgrade: 'Upgrade your plan',

  // Errori
  errorGeneric: 'Something went wrong. Please try again.',
  errorQuota: 'Monthly limit reached: upgrade to create more records.',
  required: 'This field is required',
}

/** Forma delle stringhe del modulo (chiavi tipizzate): it/fr/es/de la implementano. */
export type @@APP_CLASS@@Messages = typeof en
