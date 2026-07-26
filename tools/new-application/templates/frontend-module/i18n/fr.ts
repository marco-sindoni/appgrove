import type { @@APP_CLASS@@Messages } from './en'

// Stringhe del modulo **@@APP_NAME@@** in francese (UC 0060). Da riscrivere col linguaggio reale dell'app.
export const fr: @@APP_CLASS@@Messages = {
  appName: '@@APP_NAME@@',
  sectionItems: '@@APP_NAME@@',
  title: '@@APP_NAME@@',
  subtitle: 'Les enregistrements de votre compte.',

  newItem: 'Nouvel enregistrement',
  backToList: '← Retour à la liste',

  // Colonne tabella
  colCode: 'Code',
  colContact: 'Contact',
  colRecordedOn: 'Date',
  colStatus: 'Statut',
  colTotal: 'Total',
  colActions: 'Actions',

  empty: 'Aucun enregistrement : créez-en un avec « Nouvel enregistrement ».',

  // Stati
  status: {
    draft: 'Brouillon',
    active: 'Actif',
    done: 'Terminé',
    archived: 'Archivé',
  },

  // Editor
  editorTitle: 'Nouvel enregistrement',
  contactSection: 'Contact',
  fieldContactName: 'Nom du contact',
  fieldContactEmail: 'E-mail du contact (facultatif)',
  linesTitle: 'Lignes',
  fieldLineDescription: 'Description',
  fieldLineQuantity: 'Quantité',
  fieldLineUnitAmount: 'Montant unitaire',
  addLine: 'Ajouter une ligne',
  removeLine: 'Supprimer',
  save: 'Créer l\'enregistrement',
  cancel: 'Annuler',

  // Dettaglio
  detailTitle: 'Détail de l\'enregistrement',
  changeStatus: 'Changer le statut',
  delete: 'Supprimer',
  confirmDeleteTitle: 'Supprimer l\'enregistrement ?',
  confirmDeleteBody: 'Cette action est irréversible depuis l\'interface.',

  // Quota
  quotaLabel: '@@QUOTA_LABEL@@',
  quotaReached: '@@QUOTA_REACHED@@',
  quotaUpgrade: 'Passez à un forfait supérieur',

  // Errori
  errorGeneric: 'Une erreur est survenue. Réessayez.',
  errorQuota: 'Limite mensuelle atteinte : passez à un forfait supérieur pour créer d\'autres enregistrements.',
  required: 'Champ obligatoire',
}
