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

/**
 * Una riga della composizione degli scaglioni: «6 posti a 2,99 € = 17,94 €» (UC 0104).
 *
 * `toSeat` nullo = ultima fascia, aperta verso l'alto.
 */
export interface SeatBandUsage {
  fromSeat: number
  toSeat: number | null
  unitPriceCents: number
  seats: number
  subtotalCents: number
}

/** Una persona indicata per la cessazione (UC 0104). */
export interface ReductionPerson {
  userId: string
  email: string
  displayName?: string
}

/**
 * La **riduzione in attesa**, in forma stretta (UC 0104).
 *
 * Stessa ragione della conversione del riquadro: se `dueCentsAfter` fosse incerto, un `?? 0` scriverebbe
 * «dal 15 pagherai 0,00 €» accanto a una data vera — e su quella frase qualcuno programma una cessazione.
 * Se manca un campo, la riduzione si legge come **assente** e il riquadro va in errore col resto: non si
 * mostra un avviso a metà.
 */
export interface SeatReduction {
  id: string
  executeAt: string
  overdue: boolean
  people: ReductionPerson[]
  seatsAfter: number
  dueCentsNow: number
  dueCentsAfter: number
  currency: string
  bandsAfter: SeatBandUsage[]
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
  /** Il dettaglio della riduzione in attesa; `null` quando non ce n'è (il caso normale). */
  reduction: SeatReduction | null
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
    reduction: readReduction(view.reduction),
    hasSubscription: view.hasSubscription === true,
  }
}

/**
 * Converte la riduzione in attesa nella forma stretta, o `null` se è assente o incompleta.
 *
 * **Incompleta = assente, e il riquadro non va in errore per questo.** È una scelta diversa da quella del
 * riquadro, e vale la pena dire perché: `pendingReduction` resta il fatto che governa il divieto di
 * invitare, quindi il presidio non si perde. Quello che si perde è il *dettaglio* — le persone, la data —
 * e la reazione giusta è non mostrare un avviso a metà («2 persone cesseranno il undefined»), non spegnere
 * tutta la sezione.
 */
export function readReduction(
  view: SeatSummaryView['reduction'] | undefined,
): SeatReduction | null {
  if (!view) return null
  const seatsAfter = num(view.seatsAfter)
  const dueCentsNow = num(view.dueCentsNow)
  const dueCentsAfter = num(view.dueCentsAfter)
  if (
    !view.id ||
    !view.executeAt ||
    !view.currency ||
    seatsAfter == null ||
    dueCentsNow == null ||
    dueCentsAfter == null
  ) {
    return null
  }
  return {
    id: view.id,
    executeAt: view.executeAt,
    overdue: view.overdue === true,
    people: (view.people ?? [])
      .filter((p) => !!p.userId)
      .map((p) => ({
        userId: p.userId as string,
        email: p.email ?? '',
        displayName: p.displayName,
      })),
    seatsAfter,
    dueCentsNow,
    dueCentsAfter,
    currency: view.currency,
    bandsAfter: readBands(view.bandsAfter),
  }
}

/**
 * Converte la composizione degli scaglioni. Le righe incomplete si **scartano** invece di comparire con
 * uno zero: una riga «0 posti a 0,00 €» in mezzo a un conto lo rende illeggibile, e la composizione è un
 * aiuto alla verifica — se non si può mostrare intera, meglio non mostrarne una parte falsa.
 */
export function readBands(
  bands: NonNullable<SeatSummaryView['reduction']>['bandsAfter'] | undefined,
): SeatBandUsage[] {
  return (bands ?? [])
    .map((b) => ({
      fromSeat: num(b.fromSeat),
      toSeat: num(b.toSeat),
      unitPriceCents: num(b.unitPriceCents),
      seats: num(b.seats),
      subtotalCents: num(b.subtotalCents),
    }))
    .filter(
      (b): b is SeatBandUsage =>
        b.fromSeat != null &&
        b.unitPriceCents != null &&
        b.seats != null &&
        b.subtotalCents != null,
    )
}
