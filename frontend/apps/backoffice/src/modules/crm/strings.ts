/**
 * Stringhe del modulo **Mini-CRM** (per-modulo, #03 dec.6): autocontenute per non accoppiare il
 * modulo all'i18n della shell. In italiano (lingua di prodotto).
 */
export const t = {
  appName: 'Mini-CRM',
  sectionContacts: 'Contatti',
  sectionMembers: 'Membri',
  title: 'Contatti',
  subtitle: 'Le persone e le trattative del tuo account.',

  newContact: 'Nuovo contatto',
  backToList: '← Torna ai contatti',

  // Ricerca / filtro
  searchPlaceholder: 'Cerca per nome o organizzazione…',
  filterAllStages: 'Tutti gli stati',

  // Colonne tabella contatti
  colName: 'Contatto',
  colOrganization: 'Organizzazione',
  colStage: 'Stato',
  colActions: 'Azioni',

  empty: 'Nessun contatto: creane uno con "Nuovo contatto".',
  emptySearch: 'Nessun contatto corrisponde ai filtri.',

  // Stati trattativa
  stageLead: 'Nuovo',
  stageQualified: 'Qualificato',
  stageNegotiating: 'In trattativa',
  stageWon: 'Vinto',
  stageLost: 'Perso',

  // Editor contatto
  editorTitle: 'Nuovo contatto',
  contactSection: 'Dati del contatto',
  fieldName: 'Nome',
  fieldEmail: 'Email (opzionale)',
  fieldPhone: 'Telefono (opzionale)',
  fieldOrganization: 'Organizzazione (opzionale)',
  fieldNotes: 'Note (opzionale)',
  save: 'Crea contatto',
  cancel: 'Annulla',

  // Dettaglio contatto
  detailTitle: 'Dettaglio contatto',
  changeStage: 'Stato trattativa',
  delete: 'Elimina',
  confirmDeleteTitle: 'Eliminare il contatto?',
  confirmDeleteBody: 'Il contatto e le sue interazioni non saranno più visibili.',

  // Interazioni
  interactionsTitle: 'Interazioni',
  interactionsEmpty: 'Nessuna interazione registrata.',
  fieldInteractionKind: 'Tipo',
  fieldInteractionNote: 'Nota',
  interactionKindCall: 'Telefonata',
  interactionKindEmail: 'Email',
  interactionKindMeeting: 'Incontro',
  interactionKindNote: 'Nota',
  addInteraction: 'Registra',

  // Membri / posti
  membersTitle: 'Membri',
  membersSubtitle: 'Chi, nel tuo account, può usare il Mini-CRM.',
  seatsUsage: 'Posti occupati',
  assignSeat: 'Assegna posto',
  fieldUserId: 'Identificativo utente',
  revokeSeat: 'Revoca',
  confirmRevokeTitle: 'Revocare il posto?',
  confirmRevokeBody: 'L\'utente perderà l\'accesso al Mini-CRM. Potrai riassegnarglielo in seguito.',
  membersEmpty: 'Nessun posto assegnato: assegnane uno per dare accesso a un collega.',

  // Quota (banner posti)
  quotaLabel: 'Posti',
  quotaReached: 'Posti esauriti. Revoca un posto oppure passa a un piano superiore.',
  quotaUpgrade: 'Passa a un piano superiore',

  // Errori
  errorGeneric: 'Si è verificato un errore. Riprova.',
  errorSeatsQuota: 'Posti esauriti: fai upgrade per assegnarne altri.',
  errorNoSeat: 'Non hai un posto assegnato in Mini-CRM: chiedi a un amministratore dell\'account.',
  required: 'Campo obbligatorio',
}

const STAGE_LABELS: Record<string, string> = {
  lead: t.stageLead,
  qualified: t.stageQualified,
  negotiating: t.stageNegotiating,
  won: t.stageWon,
  lost: t.stageLost,
}

/** Etichetta localizzata per lo stato della trattativa. */
export function stageLabel(stage?: string): string {
  return (stage && STAGE_LABELS[stage]) || t.stageLead
}

const KIND_LABELS: Record<string, string> = {
  call: t.interactionKindCall,
  email: t.interactionKindEmail,
  meeting: t.interactionKindMeeting,
  note: t.interactionKindNote,
}

/** Etichetta localizzata per il tipo di interazione. */
export function kindLabel(kind?: string): string {
  return (kind && KIND_LABELS[kind]) || t.interactionKindNote
}
