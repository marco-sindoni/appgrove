import type { SeatSummaryView } from '@appgrove/api-client'

/**
 * Il riquadro dei posti, **in forma stretta** (UC 0103).
 *
 * Perché esiste questo passaggio. Il contratto pubblicato dal servizio descrive ogni campo come
 * *facoltativo* — è come lo genera lo strumento a partire dal codice, non una scelta del contratto — e
 * l'interfaccia si troverebbe quindi a scrivere `?? 0` su ogni importo. Su questi numeri quel `?? 0` è
 * pericoloso in un modo molto concreto: «stai pagando 0,00 €» per un campo assente è indistinguibile da
 * «stai pagando 0,00 €» perché sei entro la franchigia, e sulla base di quello zero qualcuno invita una
 * persona convinto che non costi.
 *
 * Quindi si converte **una volta sola**, all'ingresso: se la risposta è completa si ottiene un oggetto con
 * tutti i campi presenti e il resto del codice non ha più valori incerti da maneggiare; se manca qualcosa
 * si ottiene `null`, e il riquadro mostra lo **stato di errore** — che è la reazione giusta a una risposta
 * che non si capisce, e la stessa che si ha quando la lettura fallisce del tutto.
 */

/** Una fascia del listino: {@link toSeat} nullo = ultima fascia, aperta verso l'alto. */
export interface SeatBand {
  fromSeat: number
  toSeat: number | null
  unitPriceCents: number
}

/** Il posto successivo: la stima che si mostra prima di confermare un invito. */
export interface NextSeat {
  seatNumber: number
  unitPriceCents: number
  dueCentsAfter: number
  chargeCents: number
  cheaperThanPrevious: boolean
}

/** Il riquadro dei posti con tutti i campi presenti. */
export interface Seats {
  usedSeats: number
  composition: { active: number; suspended: number; pendingInvitations: number }
  currency: string
  freeSeats: number
  paidSeats: number
  dueCents: number
  paidQuantity: number
  /** La fascia dell'ultimo posto occupato; assente solo con zero posti (nella pratica mai: c'è l'owner). */
  currentBand: SeatBand | null
  next: NextSeat
  pendingReduction: boolean
  hasSubscription: boolean
}

const num = (v: number | undefined): number | null => (typeof v === 'number' ? v : null)

/**
 * Converte la risposta del servizio nella forma stretta, o restituisce `null` se è incompleta.
 *
 * La fascia corrente è l'unico campo la cui **assenza è legittima** (zero posti occupati), e per questo è
 * l'unico che diventa `null` senza invalidare tutto il resto.
 */
export function readSeats(view: SeatSummaryView | undefined): Seats | null {
  if (!view) return null
  const usedSeats = num(view.usedSeats)
  const freeSeats = num(view.freeSeats)
  const paidSeats = num(view.paidSeats)
  const dueCents = num(view.dueCents)
  const paidQuantity = num(view.paidQuantity)
  const active = num(view.composition?.active)
  const suspended = num(view.composition?.suspended)
  const pendingInvitations = num(view.composition?.pendingInvitations)
  const seatNumber = num(view.next?.seatNumber)
  const unitPriceCents = num(view.next?.unitPriceCents)
  const dueCentsAfter = num(view.next?.dueCentsAfter)
  const chargeCents = num(view.next?.chargeCents)

  if (
    usedSeats == null ||
    freeSeats == null ||
    paidSeats == null ||
    dueCents == null ||
    paidQuantity == null ||
    active == null ||
    suspended == null ||
    pendingInvitations == null ||
    seatNumber == null ||
    unitPriceCents == null ||
    dueCentsAfter == null ||
    chargeCents == null ||
    !view.currency
  ) {
    return null
  }

  const band = view.currentBand
  const bandFrom = num(band?.fromSeat)
  const bandPrice = num(band?.unitPriceCents)

  return {
    usedSeats,
    composition: { active, suspended, pendingInvitations },
    currency: view.currency,
    freeSeats,
    paidSeats,
    dueCents,
    paidQuantity,
    currentBand:
      bandFrom != null && bandPrice != null
        ? { fromSeat: bandFrom, toSeat: num(band?.toSeat), unitPriceCents: bandPrice }
        : null,
    next: {
      seatNumber,
      unitPriceCents,
      dueCentsAfter,
      chargeCents,
      cheaperThanPrevious: view.next?.cheaperThanPrevious === true,
    },
    pendingReduction: view.pendingReduction === true,
    hasSubscription: view.hasSubscription === true,
  }
}
