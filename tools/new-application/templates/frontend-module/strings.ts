/**
 * Stringhe del modulo **@@APP_NAME@@** (per-modulo, #03 dec.6): autocontenute per non accoppiare il
 * modulo all'i18n della shell. In italiano (lingua di prodotto).
 *
 * Generate dallo scaffolding sul dominio segnaposto: vanno riscritte col linguaggio reale dell'app
 * (un "record" non si chiama record in nessun prodotto vero).
 */
export const t = {
  appName: '@@APP_NAME@@',
  sectionItems: '@@APP_NAME@@',
  title: '@@APP_NAME@@',
  subtitle: 'I record del tuo account.',

  newItem: 'Nuovo record',
  backToList: '← Torna all\'elenco',

  // Colonne tabella
  colCode: 'Codice',
  colContact: 'Contatto',
  colRecordedOn: 'Data',
  colStatus: 'Stato',
  colTotal: 'Totale',
  colActions: 'Azioni',

  empty: 'Nessun record: creane uno con "Nuovo record".',

  // Stati
  statusDraft: 'Bozza',
  statusActive: 'Attivo',
  statusDone: 'Completato',
  statusArchived: 'Archiviato',

  // Editor
  editorTitle: 'Nuovo record',
  contactSection: 'Contatto',
  fieldContactName: 'Nome contatto',
  fieldContactEmail: 'Email contatto (opzionale)',
  linesTitle: 'Righe',
  fieldLineDescription: 'Descrizione',
  fieldLineQuantity: 'Quantità',
  fieldLineUnitAmount: 'Importo unitario',
  addLine: 'Aggiungi riga',
  removeLine: 'Rimuovi',
  save: 'Crea record',
  cancel: 'Annulla',

  // Dettaglio
  detailTitle: 'Dettaglio record',
  changeStatus: 'Cambia stato',
  delete: 'Elimina',
  confirmDeleteTitle: 'Eliminare il record?',
  confirmDeleteBody: 'L\'operazione non è reversibile dalla UI.',

  // Quota
  quotaLabel: 'Consumo questo mese',
  quotaReached: 'Hai raggiunto il limite mensile del tuo piano.',
  quotaUpgrade: 'Passa a un piano superiore',

  // Errori
  errorGeneric: 'Si è verificato un errore. Riprova.',
  errorQuota: 'Limite mensile raggiunto: fai upgrade per creare altri record.',
  required: 'Campo obbligatorio',
}

/** Etichetta localizzata per lo stato di un record. */
export function statusLabel(status?: string): string {
  switch (status) {
    case 'active':
      return t.statusActive
    case 'done':
      return t.statusDone
    case 'archived':
      return t.statusArchived
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
