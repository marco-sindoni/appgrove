import type { FattureMessages } from './en'

// Stringhe del modulo **fatture** in spagnolo (UC 0060).
export const es: FattureMessages = {
  appName: 'Facturas',
  sectionInvoices: 'Facturas',
  title: 'Facturas',
  subtitle: 'Las facturas de tu cuenta.',

  newInvoice: 'Nueva factura',
  backToList: '← Volver a las facturas',

  // Colonne tabella
  colNumber: 'Número',
  colCustomer: 'Cliente',
  colIssueDate: 'Fecha',
  colStatus: 'Estado',
  colTotal: 'Total',
  colActions: 'Acciones',

  empty: 'No hay facturas: crea una con «Nueva factura».',

  // Stati fattura
  status: {
    draft: 'Borrador',
    issued: 'Emitida',
    paid: 'Pagada',
    voided: 'Anulada',
  },

  // Editor
  editorTitle: 'Nueva factura',
  customerSection: 'Cliente',
  fieldCustomerName: 'Nombre del cliente',
  fieldCustomerEmail: 'Correo del cliente (opcional)',
  linesTitle: 'Líneas',
  fieldLineDescription: 'Descripción',
  fieldLineQuantity: 'Cantidad',
  fieldLineUnitAmount: 'Importe unitario',
  addLine: 'Añadir línea',
  removeLine: 'Quitar',
  save: 'Crear factura',
  cancel: 'Cancelar',

  // Dettaglio
  detailTitle: 'Detalle de la factura',
  changeStatus: 'Cambiar estado',
  delete: 'Eliminar',
  confirmDeleteTitle: '¿Eliminar la factura?',
  confirmDeleteBody: 'Esta acción no se puede deshacer desde la interfaz.',

  // Quota
  quotaLabel: 'Facturas este mes',
  quotaUnit: 'facturas',
  quotaReached: 'Has alcanzado el límite mensual de tu plan.',
  quotaUpgrade: 'Cambia a un plan superior',

  // Errori
  errorGeneric: 'Se ha producido un error. Inténtalo de nuevo.',
  errorQuota: 'Límite mensual alcanzado: mejora tu plan para crear más facturas.',
  required: 'Campo obligatorio',
}
