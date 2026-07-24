import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { unwrap } from '@appgrove/api-client'
import { useCrmClient } from './CrmClientProvider'
import type { CreateContact, CreateInteraction, UpdateContact } from './client'

const KEY = ['crm'] as const

/** Elenco contatti paginato, con ricerca testuale e filtro per stato. Tenant-scoped lato server. */
export function useContacts(params: { q?: string; stage?: string; page?: number; size?: number } = {}) {
  const client = useCrmClient()
  const { q, stage, page = 0, size = 20 } = params
  return useQuery({
    queryKey: [...KEY, 'contacts', 'list', q ?? '', stage ?? '', page, size],
    queryFn: () =>
      unwrap(
        client.GET('/api/crm/v1/contacts', {
          params: { query: { q: q || undefined, stage: stage || undefined, page, size } },
        }),
      ),
  })
}

/** Dettaglio di un contatto. */
export function useContact(id: string | undefined) {
  const client = useCrmClient()
  return useQuery({
    queryKey: [...KEY, 'contacts', 'detail', id],
    enabled: !!id,
    queryFn: () =>
      unwrap(client.GET('/api/crm/v1/contacts/{id}', { params: { path: { id: id as string } } })),
  })
}

/** Interazioni di un contatto (dalla più recente). */
export function useInteractions(contactId: string | undefined) {
  const client = useCrmClient()
  return useQuery({
    queryKey: [...KEY, 'contacts', 'interactions', contactId],
    enabled: !!contactId,
    queryFn: () =>
      unwrap(
        client.GET('/api/crm/v1/contacts/{contactId}/interactions', {
          params: { path: { contactId: contactId as string } },
        }),
      ),
  })
}

/** Stato quota dei posti (`GET /quota`): alimenta il banner posti/limite. */
export function useCrmQuota() {
  const client = useCrmClient()
  return useQuery({
    queryKey: [...KEY, 'quota'],
    queryFn: () => unwrap(client.GET('/api/crm/v1/quota')),
  })
}

/** Crea un contatto. */
export function useCreateContact() {
  const client = useCrmClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateContact) => unwrap(client.POST('/api/crm/v1/contacts', { body })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [...KEY, 'contacts'] }),
  })
}

/** Aggiorna un contatto (dati o stato trattativa). */
export function useUpdateContact() {
  const client = useCrmClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string; body: UpdateContact }) =>
      unwrap(
        client.PATCH('/api/crm/v1/contacts/{id}', {
          params: { path: { id: vars.id } },
          body: vars.body,
        }),
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [...KEY, 'contacts'] }),
  })
}

/** Elimina (soft-delete) un contatto. */
export function useDeleteContact() {
  const client = useCrmClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) =>
      unwrap(client.DELETE('/api/crm/v1/contacts/{id}', { params: { path: { id } } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [...KEY, 'contacts'] }),
  })
}

/** Registra un'interazione su un contatto. */
export function useCreateInteraction(contactId: string) {
  const client = useCrmClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateInteraction) =>
      unwrap(
        client.POST('/api/crm/v1/contacts/{contactId}/interactions', {
          params: { path: { contactId } },
          body,
        }),
      ),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: [...KEY, 'contacts', 'interactions', contactId] }),
  })
}

/** Riepilogo posti (occupati/tetto + elenco), per la schermata Membri. */
export function useSeats() {
  const client = useCrmClient()
  return useQuery({
    queryKey: [...KEY, 'seats'],
    queryFn: () => unwrap(client.GET('/api/crm/v1/seats')),
  })
}

/** Assegna un posto a un utente (`POST /seats`). A tetto raggiunto il backend risponde 429. */
export function useAssignSeat() {
  const client = useCrmClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (userId: string) =>
      unwrap(client.POST('/api/crm/v1/seats', { body: { userId } })),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [...KEY, 'seats'] })
      void queryClient.invalidateQueries({ queryKey: [...KEY, 'quota'] })
    },
  })
}

/** Revoca il posto di un utente (`DELETE /seats/{userId}`): libera subito la giacenza. */
export function useRevokeSeat() {
  const client = useCrmClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (userId: string) =>
      unwrap(client.DELETE('/api/crm/v1/seats/{userId}', { params: { path: { userId } } })),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [...KEY, 'seats'] })
      void queryClient.invalidateQueries({ queryKey: [...KEY, 'quota'] })
    },
  })
}
