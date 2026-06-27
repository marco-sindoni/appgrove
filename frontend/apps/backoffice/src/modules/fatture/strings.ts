/**
 * Stringhe del modulo **fatture** (per-modulo, #03 dec.6): autocontenute per non accoppiare il modulo
 * all'i18n della shell. In italiano (lingua di prodotto). La standardizzazione EN/IT dei moduli app è
 * una decisione differita (vedi _BACKLOG.md).
 */
export const t = {
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

  empty: 'Nessuna fattura: creane una con "Nuova fattura".',

  // Stati fattura
  statusDraft: 'Bozza',
  statusIssued: 'Emessa',
  statusPaid: 'Pagata',
  statusVoided: 'Annullata',

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
  confirmDeleteBody: 'L\'operazione non è reversibile dalla UI.',

  // Quota
  quotaLabel: 'Fatture questo mese',
  quotaUnlimited: 'Fatture questo mese: {used} (nessun limite)',
  quotaReached: 'Hai raggiunto il limite mensile del tuo piano.',
  quotaUpgrade: 'Passa a un piano superiore',

  // Errori
  errorGeneric: 'Si è verificato un errore. Riprova.',
  errorQuota: 'Limite mensile raggiunto: fai upgrade per creare altre fatture.',
  required: 'Campo obbligatorio',
}

/** Etichetta localizzata per lo stato di una fattura. */
export function statusLabel(status?: string): string {
  switch (status) {
    case 'issued':
      return t.statusIssued
    case 'paid':
      return t.statusPaid
    case 'voided':
      return t.statusVoided
    default:
      return t.statusDraft
  }
}

/** Formatta un importo come valuta (default EUR), con fallback robusto. */
export function formatAmount(amount?: number, currency = 'EUR'): string {
  if (amount == null) return '—'
  try {
    return new Intl.NumberFormat('it-IT', { style: 'currency', currency }).format(amount)
  } catch {
    return `${amount.toFixed(2)} ${currency}`
  }
}
