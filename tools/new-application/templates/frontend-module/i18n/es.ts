import type { @@APP_CLASS@@Messages } from './en'

// Stringhe del modulo **@@APP_NAME@@** in spagnolo (UC 0060). Da riscrivere col linguaggio reale dell'app.
export const es: @@APP_CLASS@@Messages = {
  appName: '@@APP_NAME@@',
  sectionItems: '@@APP_NAME@@',
  title: '@@APP_NAME@@',
  subtitle: 'Los registros de tu cuenta.',

  newItem: 'Nuevo registro',
  backToList: '← Volver a la lista',

  // Colonne tabella
  colCode: 'Código',
  colContact: 'Contacto',
  colRecordedOn: 'Fecha',
  colStatus: 'Estado',
  colTotal: 'Total',
  colActions: 'Acciones',

  empty: 'No hay registros: crea uno con «Nuevo registro».',

  // Stati
  status: {
    draft: 'Borrador',
    active: 'Activo',
    done: 'Completado',
    archived: 'Archivado',
  },

  // Editor
  editorTitle: 'Nuevo registro',
  contactSection: 'Contacto',
  fieldContactName: 'Nombre del contacto',
  fieldContactEmail: 'Correo del contacto (opcional)',
  linesTitle: 'Líneas',
  fieldLineDescription: 'Descripción',
  fieldLineQuantity: 'Cantidad',
  fieldLineUnitAmount: 'Importe unitario',
  addLine: 'Añadir línea',
  removeLine: 'Quitar',
  save: 'Crear registro',
  cancel: 'Cancelar',

  // Dettaglio
  detailTitle: 'Detalle del registro',
  changeStatus: 'Cambiar estado',
  delete: 'Eliminar',
  confirmDeleteTitle: '¿Eliminar el registro?',
  confirmDeleteBody: 'Esta acción no se puede deshacer desde la interfaz.',

  // Quota
  quotaLabel: '@@QUOTA_LABEL@@',
  quotaUnit: '@@QUOTA_UNIT@@',
  quotaReached: '@@QUOTA_REACHED@@',
  quotaUpgrade: 'Cambia a un plan superior',

  // Errori
  errorGeneric: 'Se ha producido un error. Inténtalo de nuevo.',
  errorQuota: 'Límite mensual alcanzado: mejora tu plan para crear más registros.',
  required: 'Campo obligatorio',
}
