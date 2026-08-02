import { useTranslation } from '@appgrove/i18n'
import type { AdminTicketView } from '../api/hooks'

/**
 * Etichette e stato di scadenza dei ticket (UC 0075), condivisi fra la coda e il dettaglio: due
 * schermate che devono chiamare le stesse cose con le stesse parole.
 */

/** Finestra entro cui una scadenza è «in scadenza»: una settimana su un termine di un mese. */
const DUE_SOON_MS = 7 * 24 * 60 * 60 * 1000

/**
 * Stato della scadenza legale di un ticket: `overdue` se il termine è passato, `dueSoon` se manca
 * meno di una settimana, `none` se non c'è scadenza o è ancora lontana. Un ticket già risolto o
 * chiuso non è mai in ritardo: il termine riguarda il riscontro, che è già stato dato.
 */
export function dueState(ticket: AdminTicketView, now: number = Date.now()): 'overdue' | 'dueSoon' | 'none' {
  if (!ticket.dueAt || ticket.status === 'resolved' || ticket.status === 'closed') return 'none'
  const due = new Date(ticket.dueAt).getTime()
  if (Number.isNaN(due)) return 'none'
  if (due <= now) return 'overdue'
  return due - now <= DUE_SOON_MS ? 'dueSoon' : 'none'
}

/** Traduttori del vocabolario del ticketing; il valore grezzo resta il ripiego, mai una stringa vuota. */
export function useTicketLabels() {
  const { t } = useTranslation()
  const lookup = (prefix: string) => (value?: string) =>
    value ? t(`admin.tickets.${prefix}.${value}`, { defaultValue: value }) : '—'
  return {
    type: lookup('type'),
    source: lookup('source'),
    status: lookup('status'),
    priority: lookup('priority'),
    author: lookup('author'),
  }
}
