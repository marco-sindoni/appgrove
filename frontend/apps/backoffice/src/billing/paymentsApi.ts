import { useQuery } from '@tanstack/react-query'
import { unwrap, type components } from '@appgrove/api-client'
import { useApiClient } from '../api/apiClient'
import { useAuthStore } from '../auth/authStore'

/** Una riga dello storico pagamenti, così come la serve il read-model (UC 0096). */
export type Payment = components['schemas']['PaymentView']

const KEY = ['me', 'payments'] as const

/**
 * Storico pagamenti e ricevute del workspace da `GET /api/platform/v1/me/payments`.
 *
 * <p>Abilitata solo per chi può davvero leggerlo — `owner` e `admin`: il divieto vero è nel backend, ma
 * chiedere una lettura che sappiamo verrà rifiutata riempirebbe la pagina di un errore invece di dire
 * semplicemente che la sezione non è per tutti.
 *
 * <p>Vive in una query separata da quella degli abbonamenti di proposito: un guasto dello storico deve
 * poter restare confinato alla sua sezione, come chiede UC 0096 §5.
 */
export function useMyPayments() {
  const client = useApiClient()
  const canRead = useAuthStore((s) => !!s.claims?.roles?.some((r) => r === 'owner' || r === 'admin'))
  const authenticated = useAuthStore((s) => s.status === 'authenticated')
  return useQuery({
    queryKey: KEY,
    enabled: authenticated && canRead,
    queryFn: () => unwrap(client.GET('/api/platform/v1/me/payments', {})),
  })
}

/** Vero se il ruolo dell'utente in sessione può leggere lo storico (owner o admin). */
export function useCanReadPayments() {
  return useAuthStore((s) => !!s.claims?.roles?.some((r) => r === 'owner' || r === 'admin'))
}

/**
 * Descrizione della riga: fascia e ciclo, come nel riferimento visivo approvato ("Demo Pro — monthly").
 * Quando la fascia non c'è più a catalogo resta il solo ciclo; quando non c'è nemmeno quello, la
 * descrizione è vuota — meglio una cella vuota di una didascalia inventata. Funzione pura.
 */
export function describePayment(payment: Payment, cycleLabel: (cycle: string) => string): string {
  const parts = [payment.planName, payment.billingCycle ? cycleLabel(payment.billingCycle) : null]
  return parts.filter(Boolean).join(' — ')
}
