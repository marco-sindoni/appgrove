import type { FattureMessages } from './en'

// Stringhe del modulo **fatture** in francese (UC 0060).
export const fr: FattureMessages = {
  appName: 'Factures',
  sectionInvoices: 'Factures',
  title: 'Factures',
  subtitle: 'Les factures de votre compte.',

  newInvoice: 'Nouvelle facture',
  backToList: '← Retour aux factures',

  // Colonne tabella
  colNumber: 'Numéro',
  colCustomer: 'Client',
  colIssueDate: 'Date',
  colStatus: 'Statut',
  colTotal: 'Total',
  colActions: 'Actions',

  empty: 'Aucune facture : créez-en une avec « Nouvelle facture ».',

  // Stati fattura
  status: {
    draft: 'Brouillon',
    issued: 'Émise',
    paid: 'Payée',
    voided: 'Annulée',
  },

  // Editor
  editorTitle: 'Nouvelle facture',
  customerSection: 'Client',
  fieldCustomerName: 'Nom du client',
  fieldCustomerEmail: 'E-mail du client (facultatif)',
  linesTitle: 'Lignes',
  fieldLineDescription: 'Description',
  fieldLineQuantity: 'Quantité',
  fieldLineUnitAmount: 'Montant unitaire',
  addLine: 'Ajouter une ligne',
  removeLine: 'Supprimer',
  save: 'Créer la facture',
  cancel: 'Annuler',

  // Dettaglio
  detailTitle: 'Détail de la facture',
  changeStatus: 'Changer le statut',
  delete: 'Supprimer',
  confirmDeleteTitle: 'Supprimer la facture ?',
  confirmDeleteBody: 'Cette action est irréversible depuis l’interface.',

  // Quota
  quotaLabel: 'Factures ce mois-ci',
  quotaUnit: 'factures',
  quotaReached: 'Vous avez atteint la limite mensuelle de votre forfait.',
  quotaUpgrade: 'Passez à un forfait supérieur',

  // Errori
  errorGeneric: 'Une erreur est survenue. Réessayez.',
  errorQuota: 'Limite mensuelle atteinte : passez à un forfait supérieur pour créer d’autres factures.',
  required: 'Champ obligatoire',
}
