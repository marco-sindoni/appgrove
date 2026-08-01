import type { @@APP_CLASS@@Messages } from './en'

// Stringhe del modulo **@@APP_NAME@@** in italiano (UC 0060). Da riscrivere col linguaggio reale dell'app.
export const it: @@APP_CLASS@@Messages = {
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

  empty: 'Nessun record: creane uno con “Nuovo record”.',

  // Stati
  status: {
    draft: 'Bozza',
    active: 'Attivo',
    done: 'Completato',
    archived: 'Archiviato',
  },

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
  quotaLabel: '@@QUOTA_LABEL@@',
  quotaUnit: '@@QUOTA_UNIT@@',
  quotaReached: '@@QUOTA_REACHED@@',
  quotaUpgrade: 'Passa a un piano superiore',

  // Errori
  errorGeneric: 'Si è verificato un errore. Riprova.',
  errorQuota: 'Limite mensile raggiunto: fai upgrade per creare altri record.',
  required: 'Campo obbligatorio',
}
