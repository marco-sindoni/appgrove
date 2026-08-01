import type { @@APP_CLASS@@Messages } from './en'

// Stringhe del modulo **@@APP_NAME@@** in tedesco (UC 0060), forma di cortesia "Sie". Da riscrivere col linguaggio reale dell'app.
export const de: @@APP_CLASS@@Messages = {
  appName: '@@APP_NAME@@',
  sectionItems: '@@APP_NAME@@',
  title: '@@APP_NAME@@',
  subtitle: 'Die Datensätze Ihres Kontos.',

  newItem: 'Neuer Datensatz',
  backToList: '← Zurück zur Liste',

  // Colonne tabella
  colCode: 'Code',
  colContact: 'Kontakt',
  colRecordedOn: 'Datum',
  colStatus: 'Status',
  colTotal: 'Summe',
  colActions: 'Aktionen',

  empty: 'Noch keine Datensätze: Erstellen Sie einen mit „Neuer Datensatz“.',

  // Stati
  status: {
    draft: 'Entwurf',
    active: 'Aktiv',
    done: 'Erledigt',
    archived: 'Archiviert',
  },

  // Editor
  editorTitle: 'Neuer Datensatz',
  contactSection: 'Kontakt',
  fieldContactName: 'Kontaktname',
  fieldContactEmail: 'Kontakt-E-Mail (optional)',
  linesTitle: 'Positionen',
  fieldLineDescription: 'Beschreibung',
  fieldLineQuantity: 'Menge',
  fieldLineUnitAmount: 'Einzelbetrag',
  addLine: 'Position hinzufügen',
  removeLine: 'Entfernen',
  save: 'Datensatz erstellen',
  cancel: 'Abbrechen',

  // Dettaglio
  detailTitle: 'Datensatzdetails',
  changeStatus: 'Status ändern',
  delete: 'Löschen',
  confirmDeleteTitle: 'Datensatz löschen?',
  confirmDeleteBody: 'Diese Aktion kann über die Oberfläche nicht rückgängig gemacht werden.',

  // Quota
  quotaLabel: '@@QUOTA_LABEL@@',
  quotaUnit: '@@QUOTA_UNIT@@',
  quotaReached: '@@QUOTA_REACHED@@',
  quotaUpgrade: 'Auf einen höheren Tarif wechseln',

  // Errori
  errorGeneric: 'Ein Fehler ist aufgetreten. Bitte erneut versuchen.',
  errorQuota: 'Monatslimit erreicht: Führen Sie ein Upgrade durch, um weitere Datensätze zu erstellen.',
  required: 'Pflichtfeld',
}
