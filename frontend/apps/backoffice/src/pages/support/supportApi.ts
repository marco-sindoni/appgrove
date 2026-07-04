import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { unwrap, type components } from '@appgrove/api-client'
import { useApiClient } from '../../api/apiClient'

/**
 * API ticket lato utente (UC 0034, #13 D21): apri ticket, lista dei propri, thread con risposta.
 * Canale di supporto esente dai gate di enforcement (#09 F31): funziona anche a subscription
 * scaduta (serve anche per esercitare i diritti GDPR).
 */

export type TicketView = components['schemas']['TicketView']
export type TicketDetailView = components['schemas']['TicketDetailView']
export type TicketMessageView = components['schemas']['MessageView']

/** I ticket del proprio tenant, più recenti prima (`GET /tickets`). */
export function useTickets() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['tickets'],
    queryFn: () => unwrap<TicketView[]>(client.GET('/api/platform/v1/tickets')),
  })
}

/** Dettaglio con thread (`GET /tickets/{id}`). */
export function useTicket(id: string | null) {
  const client = useApiClient()
  return useQuery({
    queryKey: ['tickets', id],
    enabled: !!id,
    queryFn: () =>
      unwrap<TicketDetailView>(
        client.GET('/api/platform/v1/tickets/{id}', { params: { path: { id: id as string } } }),
      ),
  })
}

/** Apre un ticket (`POST /tickets`): oggetto + primo messaggio del thread. */
export function useOpenTicket() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { type: 'support' | 'privacy'; subject: string; message: string }) =>
      unwrap<TicketView>(client.POST('/api/platform/v1/tickets', { body: vars })),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['tickets'] }),
  })
}

/** Risponde nel thread (`POST /tickets/{id}/messages`). */
export function useReplyTicket() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string; body: string }) =>
      unwrap<TicketMessageView>(
        client.POST('/api/platform/v1/tickets/{id}/messages', {
          params: { path: { id: vars.id } },
          body: { body: vars.body },
        }),
      ),
    onSuccess: (_data, vars) => {
      void queryClient.invalidateQueries({ queryKey: ['tickets'] })
      void queryClient.invalidateQueries({ queryKey: ['tickets', vars.id] })
    },
  })
}
