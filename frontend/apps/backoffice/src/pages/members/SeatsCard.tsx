import { Link } from 'react-router-dom'
import { Badge, Button, Card, CardContent, CardHeader } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { formatPrice } from '../../billing/checkoutMachine'
import type { TFn } from '../../auth/schemas'
import type { Seats, SeatBand, SeatBandUsage, SeatReduction } from './seats'

/**
 * Il **riquadro dei posti** in testa alla sezione «Members» (UC 0103 §6).
 *
 * Risponde a tre domande, in quest'ordine, perché è l'ordine in cui vengono in mente:
 *
 * 1. **quanti posti uso?** — con la composizione sotto, perché chi legge conta le righe della tabella e
 *    vuole che gli torni;
 * 2. **quanto sto pagando?** — l'importo mensile e quanti posti sono a pagamento;
 * 3. **quanto mi costa il prossimo?** — che è la domanda vera di chi sta per invitare qualcuno.
 *
 * **Nessun numero è calcolato qui.** Arrivano tutti dal servizio: importi, fascia, costo del posto
 * successivo, nuovo totale, e persino il giudizio «il prossimo costa meno del precedente». L'unica cosa che
 * questo file fa con i numeri è formattarli nella valuta e nella lingua di chi guarda.
 *
 * **Il caso in cui la tariffa scende va detto per esteso.** Col listino progressivo il totale sale
 * *sempre*: quello che scende, ai confini di fascia, è il costo del posto in più. Un cliente che leggesse
 * «costa meno» accanto a un totale più alto senza spiegazione penserebbe a un errore di conteggio — ed è la
 * ragione per cui questa frase esiste come traduzione a sé e non come composizione di due pezzi.
 */

/** Formatta un importo in centesimi nella valuta del listino e nella lingua di chi guarda. */
const money = (cents: number, currency: string, language: string) =>
  formatPrice(cents, currency, language)

/**
 * Etichetta di una fascia: «posti 4–10» oppure «dal posto 101» per l'ultima, che è aperta verso l'alto.
 * La fascia aperta non ha un limite superiore da mostrare, e inventarne uno («101–∞») sarebbe una notazione
 * da manuale, non una frase.
 */
function bandLabel(t: TFn, band: SeatBand | null): string | null {
  if (!band) return null
  return band.toSeat == null
    ? t('seats.bandOpen', { from: band.fromSeat })
    : t('seats.bandRange', { from: band.fromSeat, to: band.toSeat })
}

/**
 * Il **riquadro di avviso della riduzione in attesa** (UC 0104 §6).
 *
 * Tre cose, in quest'ordine, perché è l'ordine delle domande di chi lo legge: *quante persone e quando*,
 * *quanto pagherò da allora* (con la composizione, perché un importo senza il suo conto non si verifica),
 * e *come torno indietro*.
 *
 * Il comando di annullamento e quello per mantenere una singola persona stanno **dentro** l'avviso e non
 * altrove: chi legge «non puoi aggiungere persone» deve avere la via d'uscita sotto gli occhi, non in
 * un'altra schermata.
 */
function ReductionNotice({
  reduction,
  onCancel,
  onKeep,
  busy,
}: {
  reduction: SeatReduction
  onCancel: () => void
  onKeep: (userId: string) => void
  busy: boolean
}) {
  const { t, i18n } = useTranslation()
  const lang = i18n.language
  const date = new Date(reduction.executeAt).toLocaleDateString(lang)

  return (
    <div role="alert" className="space-y-3 rounded-md border border-warning bg-surface-2 p-3">
      <p className="text-sm font-semibold text-warning">{t('seats.reductionTitle')}</p>
      <p className="text-[12.5px] text-fg">
        {t('seats.reductionSummary', { count: reduction.people.length, date })}
      </p>

      {/* La data è passata e l'esecuzione non è ancora avvenuta: si dice, con onestà. Un cliente che
          vede una data passata e nessun cambiamento pensa che il sistema si sia dimenticato di lui. */}
      {reduction.overdue && (
        <p className="text-[12.5px] font-semibold text-warning">{t('seats.reductionOverdue')}</p>
      )}

      <p className="text-[12.5px] text-fg-muted">
        {t('seats.reductionAfter', {
          date,
          from: money(reduction.dueCentsNow, reduction.currency, lang),
          to: money(reduction.dueCentsAfter, reduction.currency, lang),
          count: reduction.seatsAfter,
        })}
      </p>

      {reduction.bandsAfter.length > 0 && (
        <div className="text-[12px] text-fg-muted">
          <p>{t('seats.reductionCompositionAfter')}</p>
          <ul>
            {reduction.bandsAfter.map((b) => (
              <li key={b.fromSeat}>{bandUsageLabel(t, b, reduction.currency, lang)}</li>
            ))}
          </ul>
        </div>
      )}

      {/* Le persone indicate, ognuna con il comando per TOGLIERLA dall'elenco. L'etichetta è
          «Mantieni» e non «Rimuovi»: il comando aggiunge una persona all'account, non la toglie, e
          un'etichetta distruttiva su un'azione che salva qualcuno è la peggiore delle ambiguità. */}
      <ul className="space-y-1.5">
        {reduction.people.map((person) => (
          <li key={person.userId} className="flex flex-wrap items-center gap-2 text-[12.5px]">
            <span className="font-semibold text-fg">{person.email}</span>
            {person.displayName && <span className="text-fg-muted">{person.displayName}</span>}
            <Button
              type="button"
              variant="secondary"
              size="sm"
              disabled={busy}
              aria-label={t('seats.reductionKeepLabel', { email: person.email })}
              onClick={() => onKeep(person.userId)}
            >
              {t('seats.reductionKeep')}
            </Button>
          </li>
        ))}
      </ul>

      <Button type="button" variant="secondary" size="sm" disabled={busy} onClick={onCancel}>
        {t('seats.reductionCancel')}
      </Button>
    </div>
  )
}

/**
 * Una riga della composizione: «6 × 2,99 € = 17,94 €», oppure «3 posti compresi» quando la fascia è a
 * tariffa zero. Le due frasi sono diverse perché dicono due cose diverse: «3 × 0,00 € = 0,00 €» è
 * aritmeticamente giusto e commercialmente sbagliato — «compresi» e «zero euro» non sono la stessa
 * promessa, ed è la stessa distinzione che la vetrina fa fra «gratis» e «€0».
 */
function bandUsageLabel(t: TFn, band: SeatBandUsage, currency: string, language: string): string {
  if (band.unitPriceCents === 0) {
    return t('seats.reductionBandFree', { seats: band.seats })
  }
  return t('seats.reductionBandLine', {
    seats: band.seats,
    price: money(band.unitPriceCents, currency, language),
    subtotal: money(band.subtotalCents, currency, language),
  })
}

export function SeatsCard({
  seats,
  isLoading,
  isError,
  onRetry,
  onCancelReduction,
  onKeepPerson,
  busy = false,
}: {
  seats: Seats | null
  isLoading: boolean
  isError: boolean
  onRetry: () => void
  onCancelReduction?: () => void
  onKeepPerson?: (userId: string) => void
  busy?: boolean
}) {
  const { t, i18n } = useTranslation()
  const lang = i18n.language

  return (
    <Card>
      <CardHeader>
        <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">{t('seats.title')}</h2>
      </CardHeader>
      <CardContent>
        {isLoading && (
          <p role="status" className="text-sm text-fg-muted">
            {t('seats.loading')}
          </p>
        )}

        {/* Errore di lettura: non si mostra «stai pagando 0,00 €», perché sarebbe indistinguibile da un
            account entro la franchigia — e su quello zero qualcuno inviterebbe una persona senza pagarla.
            Il pulsante di invito è spento da chi ci sta sopra: qui si dice perché e si offre di riprovare. */}
        {isError && (
          <div role="alert" className="space-y-3">
            <p className="text-sm text-danger">{t('seats.readError')}</p>
            <Button variant="secondary" size="sm" onClick={onRetry}>
              {t('states.retry')}
            </Button>
          </div>
        )}

        {!isLoading && !isError && seats && (
          <div className="space-y-4">
            {/* 1. Quanti posti uso, e come si compongono. */}
            <div>
              <p className="text-[27px] font-extrabold leading-none tracking-[-0.025em] text-fg">
                {t('seats.used', { count: seats.usedSeats })}
              </p>
              <p className="mt-1.5 text-[12.5px] text-fg-muted">
                {t('seats.composition', {
                  active: seats.composition.active,
                  suspended: seats.composition.suspended,
                  invited: seats.composition.pendingInvitations,
                })}
              </p>
            </div>

            {/* 2. Quanto sto pagando. Con zero posti a pagamento si dice «compresi», non «€0»: non sono
                   la stessa promessa (è la stessa distinzione della vetrina fra «gratis» e «€0»). */}
            <div className="flex flex-wrap items-center gap-2">
              {seats.paidSeats === 0 ? (
                <p className="text-sm text-fg">
                  {t('seats.allIncluded', { count: seats.freeSeats })}
                </p>
              ) : (
                <>
                  <p className="text-sm font-semibold text-fg">
                    {t('seats.paying', { amount: money(seats.dueCents, seats.currency, lang) })}
                  </p>
                  <Badge tone="neutral">
                    {t('seats.paidSeats', { count: seats.paidSeats })}
                  </Badge>
                  {bandLabel(t, seats.currentBand) && (
                    <Badge tone="neutral">
                      {t('seats.currentBand', {
                        band: bandLabel(t, seats.currentBand),
                        price: money(seats.currentBand?.unitPriceCents ?? 0, seats.currency, lang),
                      })}
                    </Badge>
                  )}
                </>
              )}
            </div>

            {/* 3. Quanto costa il prossimo. È la domanda di chi sta per invitare qualcuno, e sta qui perché
                   la si legge PRIMA di aprire il modulo dell'invito. */}
            <p className="text-[12.5px] text-fg-muted">
              {seats.next.unitPriceCents === 0
                ? t('seats.nextFree', { seatNumber: seats.next.seatNumber })
                : seats.next.cheaperThanPrevious
                  ? // Il totale sale comunque: quello che scende è il costo del posto in più. Va detto
                    // così, per esteso, o sembra un errore di conteggio.
                    t('seats.nextCheaper', {
                      price: money(seats.next.unitPriceCents, seats.currency, lang),
                      previous: money(seats.currentBand?.unitPriceCents ?? 0, seats.currency, lang),
                      from: money(seats.dueCents, seats.currency, lang),
                      to: money(seats.next.dueCentsAfter, seats.currency, lang),
                    })
                  : t('seats.next', {
                      price: money(seats.next.unitPriceCents, seats.currency, lang),
                      to: money(seats.next.dueCentsAfter, seats.currency, lang),
                    })}
            </p>

            {/* Riduzione in attesa (UC 0104): nessun posto nuovo finché non si chiude. L'avviso porta
                l'elenco delle persone e le due vie d'uscita, perché chi legge un divieto deve avere il
                modo di uscirne sotto gli occhi. */}
            {seats.reduction && onCancelReduction && onKeepPerson && (
              <ReductionNotice
                reduction={seats.reduction}
                onCancel={onCancelReduction}
                onKeep={onKeepPerson}
                busy={busy}
              />
            )}

            <p className="text-[12px] text-fg-faint">
              {t('seats.billingHint')}{' '}
              <Link to="/billing" className="underline">
                {t('seats.billingLink')}
              </Link>
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
