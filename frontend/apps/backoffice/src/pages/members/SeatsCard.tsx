import { Link } from 'react-router-dom'
import { Badge, Button, Card, CardContent, CardHeader } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { formatPrice } from '../../billing/checkoutMachine'
import type { TFn } from '../../auth/schemas'
import type { Seats, SeatBand } from './seats'

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

export function SeatsCard({
  seats,
  isLoading,
  isError,
  onRetry,
}: {
  seats: Seats | null
  isLoading: boolean
  isError: boolean
  onRetry: () => void
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

            {/* Riduzione in attesa: nessun posto nuovo durante l'attesa (UC 0104). Oggi il servizio
                risponde sempre «no» perché lo stato non esiste ancora; il riquadro sa già dirlo, così
                quando arriverà non ci sarà una schermata da inventare. */}
            {seats.pendingReduction && (
              <p role="alert" className="text-sm text-warning">
                {t('seats.pendingReduction')}
              </p>
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
