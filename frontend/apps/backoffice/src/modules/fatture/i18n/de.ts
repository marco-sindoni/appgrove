import type { FattureMessages } from './en'

// Stringhe del modulo **fatture** in tedesco (UC 0060), forma di cortesia "Sie".
export const de: FattureMessages = {
  appName: 'Rechnungen',
  sectionInvoices: 'Rechnungen',
  title: 'Rechnungen',
  subtitle: 'Die Rechnungen Ihres Kontos.',

  newInvoice: 'Neue Rechnung',
  backToList: '← Zurück zu den Rechnungen',

  // Colonne tabella
  colNumber: 'Nummer',
  colCustomer: 'Kunde',
  colIssueDate: 'Datum',
  colStatus: 'Status',
  colTotal: 'Summe',
  colActions: 'Aktionen',

  empty: 'Noch keine Rechnungen: Erstellen Sie eine mit „Neue Rechnung“.',

  // Stati fattura
  status: {
    draft: 'Entwurf',
    issued: 'Ausgestellt',
    paid: 'Bezahlt',
    voided: 'Storniert',
  },

  // Editor
  editorTitle: 'Neue Rechnung',
  customerSection: 'Kunde',
  fieldCustomerName: 'Kundenname',
  fieldCustomerEmail: 'Kunden-E-Mail (optional)',
  linesTitle: 'Positionen',
  fieldLineDescription: 'Beschreibung',
  fieldLineQuantity: 'Menge',
  fieldLineUnitAmount: 'Einzelbetrag',
  addLine: 'Position hinzufügen',
  removeLine: 'Entfernen',
  save: 'Rechnung erstellen',
  cancel: 'Abbrechen',

  // Dettaglio
  detailTitle: 'Rechnungsdetails',
  changeStatus: 'Status ändern',
  delete: 'Löschen',
  confirmDeleteTitle: 'Rechnung löschen?',
  confirmDeleteBody: 'Diese Aktion kann über die Oberfläche nicht rückgängig gemacht werden.',

  // Quota
  quotaLabel: 'Rechnungen diesen Monat',
  quotaReached: 'Sie haben das monatliche Limit Ihres Tarifs erreicht.',
  quotaUpgrade: 'Auf einen höheren Tarif wechseln',

  // Errori
  errorGeneric: 'Ein Fehler ist aufgetreten. Bitte erneut versuchen.',
  errorQuota: 'Monatslimit erreicht: Führen Sie ein Upgrade durch, um weitere Rechnungen zu erstellen.',
  required: 'Pflichtfeld',
}
