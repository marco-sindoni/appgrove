import { describe, expect, it } from 'vitest'
import { readReduction, readSeats } from './seats'

/**
 * La conversione della risposta dei posti nella forma stretta (UC 0103).
 *
 * Quello che si prova qui è una scelta, non un dettaglio: **una risposta incompleta vale zero, non uno
 * zero**. Se un importo manca, il riquadro deve andare in errore — perché «stai pagando 0,00 €» per un
 * campo assente è indistinguibile da «stai pagando 0,00 €» perché sei entro la franchigia, e su quello zero
 * qualcuno invita una persona convinto che non costi.
 */

const completa = {
  usedSeats: 4,
  composition: { active: 3, suspended: 0, pendingInvitations: 1 },
  currency: 'EUR',
  freeSeats: 3,
  paidSeats: 1,
  dueCents: 299,
  paidQuantity: 1,
  currentBand: { fromSeat: 4, toSeat: 10, unitPriceCents: 299 },
  next: {
    seatNumber: 5,
    unitPriceCents: 299,
    dueCentsAfter: 598,
    chargeCents: 299,
    cheaperThanPrevious: false,
  },
  pendingReduction: false,
  hasSubscription: true,
}

describe('readSeats', () => {
  it('accetta una risposta completa e la restituisce senza valori incerti', () => {
    const seats = readSeats(completa)
    expect(seats).not.toBeNull()
    expect(seats?.usedSeats).toBe(4)
    expect(seats?.dueCents).toBe(299)
    expect(seats?.currentBand).toEqual({ fromSeat: 4, toSeat: 10, unitPriceCents: 299 })
    expect(seats?.next.dueCentsAfter).toBe(598)
    expect(seats?.hasSubscription).toBe(true)
  })

  it('ammette la fascia aperta verso l’alto: il posto finale è nullo, non mancante', () => {
    const seats = readSeats({
      ...completa,
      currentBand: { fromSeat: 101, toSeat: undefined, unitPriceCents: 49 },
    })
    expect(seats?.currentBand).toEqual({ fromSeat: 101, toSeat: null, unitPriceCents: 49 })
  })

  it('ammette l’assenza della fascia (zero posti) senza invalidare il resto', () => {
    const seats = readSeats({ ...completa, currentBand: undefined })
    expect(seats).not.toBeNull()
    expect(seats?.currentBand).toBeNull()
  })

  it('rifiuta la risposta se manca un importo: nessun campo diventa zero per silenzio', () => {
    expect(readSeats({ ...completa, dueCents: undefined })).toBeNull()
    expect(readSeats({ ...completa, next: { ...completa.next, unitPriceCents: undefined } })).toBeNull()
    expect(readSeats({ ...completa, next: { ...completa.next, chargeCents: undefined } })).toBeNull()
    expect(readSeats({ ...completa, freeSeats: undefined })).toBeNull()
    expect(readSeats({ ...completa, currency: undefined })).toBeNull()
    expect(readSeats({ ...completa, composition: undefined })).toBeNull()
    expect(readSeats(undefined)).toBeNull()
  })

  it('legge lo zero legittimo come zero, non come assenza', () => {
    // Entro la franchigia il dovuto È zero: distinguere «zero» da «manca» è tutto il punto.
    const seats = readSeats({
      ...completa,
      usedSeats: 3,
      paidSeats: 0,
      dueCents: 0,
      paidQuantity: 0,
      currentBand: { fromSeat: 1, toSeat: 3, unitPriceCents: 0 },
    })
    expect(seats).not.toBeNull()
    expect(seats?.dueCents).toBe(0)
    expect(seats?.paidSeats).toBe(0)
    expect(seats?.currentBand?.unitPriceCents).toBe(0)
  })

  /**
   * **La riduzione in attesa** (UC 0104): il dettaglio si legge intero, oppure non si legge.
   *
   * La scelta qui è diversa da quella del riquadro, e va detta: una riduzione incompleta si legge come
   * **assente** invece di far andare in errore tutta la sezione. Il fatto che governa il divieto di
   * invitare resta `pendingReduction`, quindi il presidio non si perde; quello che si perde è il
   * dettaglio, e la reazione giusta è non mostrare un avviso a metà — non spegnere il riquadro.
   */
  it('legge la riduzione in attesa con il suo dettaglio', () => {
    const seats = readSeats({
      ...completa,
      pendingReduction: true,
      reduction: {
        id: 'red-1',
        executeAt: '2026-09-14T00:00:00Z',
        overdue: false,
        people: [{ userId: 'u-1', email: 'uno@acme.test', displayName: 'Uno' }],
        seatsAfter: 3,
        dueCentsNow: 299,
        dueCentsAfter: 0,
        currency: 'EUR',
        bandsAfter: [{ fromSeat: 1, toSeat: 3, unitPriceCents: 0, seats: 3, subtotalCents: 0 }],
      },
    })
    expect(seats?.pendingReduction).toBe(true)
    expect(seats?.reduction?.people).toHaveLength(1)
    expect(seats?.reduction?.seatsAfter).toBe(3)
    // Zero legittimo: dopo la riduzione non si paga nulla, e zero è il valore giusto.
    expect(seats?.reduction?.dueCentsAfter).toBe(0)
    expect(seats?.reduction?.bandsAfter[0].seats).toBe(3)
  })

  it('senza riduzione il dettaglio è assente, non un oggetto vuoto', () => {
    expect(readSeats(completa)?.reduction).toBeNull()
    expect(readReduction(undefined)).toBeNull()
  })

  it('scarta una riduzione senza data o senza importo: mai un avviso a metà', () => {
    const base = {
      id: 'red-1',
      executeAt: '2026-09-14T00:00:00Z',
      overdue: false,
      people: [],
      seatsAfter: 3,
      dueCentsNow: 299,
      dueCentsAfter: 0,
      currency: 'EUR',
      bandsAfter: [],
    }
    expect(readReduction({ ...base, executeAt: undefined })).toBeNull()
    expect(readReduction({ ...base, dueCentsAfter: undefined })).toBeNull()
    expect(readReduction({ ...base, currency: undefined })).toBeNull()
    // Il riquadro invece resta leggibile: il divieto di invitare vive su `pendingReduction`.
    const seats = readSeats({ ...completa, pendingReduction: true, reduction: { ...base, executeAt: undefined } })
    expect(seats).not.toBeNull()
    expect(seats?.pendingReduction).toBe(true)
    expect(seats?.reduction).toBeNull()
  })

  it('scarta le righe incomplete della composizione invece di mostrarle a zero', () => {
    const reduction = readReduction({
      id: 'red-1',
      executeAt: '2026-09-14T00:00:00Z',
      overdue: false,
      people: [],
      seatsAfter: 3,
      dueCentsNow: 299,
      dueCentsAfter: 0,
      currency: 'EUR',
      bandsAfter: [
        { fromSeat: 1, toSeat: 3, unitPriceCents: 0, seats: 3, subtotalCents: 0 },
        { fromSeat: 4, toSeat: 10, unitPriceCents: undefined, seats: 1, subtotalCents: 299 },
      ],
    })
    expect(reduction?.bandsAfter).toHaveLength(1)
  })
})
