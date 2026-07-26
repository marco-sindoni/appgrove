import type { FattureMessages } from './en'

// Stringhe del modulo **fatture** in italiano (UC 0060).
export const it: FattureMessages = {
  appName: 'Fatture',
  sectionInvoices: 'Fatture',
  title: 'Fatture',
  subtitle: 'Le fatture del tuo account.',

  newInvoice: 'Nuova fattura',
  backToList: '← Torna alle fatture',

  // Colonne tabella
  colNumber: 'Numero',
  colCustomer: 'Cliente',
  colIssueDate: 'Data',
  colStatus: 'Stato',
  colTotal: 'Totale',
  colActions: 'Azioni',

  empty: 'Nessuna fattura: creane una con “Nuova fattura”.',

  // Stati fattura
  status: {
    draft: 'Bozza',
    issued: 'Emessa',
    paid: 'Pagata',
    voided: 'Annullata',
  },

  // Editor
  editorTitle: 'Nuova fattura',
  customerSection: 'Cliente',
  fieldCustomerName: 'Nome cliente',
  fieldCustomerEmail: 'Email cliente (opzionale)',
  linesTitle: 'Righe',
  fieldLineDescription: 'Descrizione',
  fieldLineQuantity: 'Quantità',
  fieldLineUnitAmount: 'Importo unitario',
  addLine: 'Aggiungi riga',
  removeLine: 'Rimuovi',
  save: 'Crea fattura',
  cancel: 'Annulla',

  // Dettaglio
  detailTitle: 'Dettaglio fattura',
  changeStatus: 'Cambia stato',
  delete: 'Elimina',
  confirmDeleteTitle: 'Eliminare la fattura?',
  confirmDeleteBody: 'L’operazione non è reversibile dalla UI.',

  // Quota
  quotaLabel: 'Fatture questo mese',
  quotaReached: 'Hai raggiunto il limite mensile del tuo piano.',
  quotaUpgrade: 'Passa a un piano superiore',

  // Errori
  errorGeneric: 'Si è verificato un errore. Riprova.',
  errorQuota: 'Limite mensile raggiunto: fai upgrade per creare altre fatture.',
  required: 'Campo obbligatorio',
}
