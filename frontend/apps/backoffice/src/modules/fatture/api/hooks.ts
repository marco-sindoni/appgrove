import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { unwrap } from '@appgrove/api-client'
import { useFattureClient } from './FattureClientProvider'
import type { CreateInvoice, UpdateInvoice } from './client'

const KEY = ['fatture'] as const

/** Lista fatture paginata (`GET /invoices`). Tenant-scoped lato server (discriminator). */
export function useInvoices(page = 0, size = 20) {
  const client = useFattureClient()
  return useQuery({
    queryKey: [...KEY, 'invoices', 'list', page, size],
    queryFn: () =>
      unwrap(client.GET('/api/fatture/v1/invoices', { params: { query: { page, size } } })),
  })
}

/** Dettaglio di una fattura (`GET /invoices/{id}`). */
export function useInvoiceDetail(id: string | undefined) {
  const client = useFattureClient()
  return useQuery({
    queryKey: [...KEY, 'invoices', 'detail', id],
    enabled: !!id,
    queryFn: () =>
      unwrap(client.GET('/api/fatture/v1/invoices/{id}', { params: { path: { id: id as string } } })),
  })
}

/** Stato quota della metrica `fatture` (`GET /quota`): alimenta il banner consumo/limite. */
export function useFattureQuota() {
  const client = useFattureClient()
  return useQuery({
    queryKey: [...KEY, 'quota'],
    queryFn: () => unwrap(client.GET('/api/fatture/v1/quota')),
  })
}

/** Crea una fattura (`POST /invoices`). A quota raggiunta il backend risponde 429 (gestito in UI). */
export function useCreateInvoice() {
  const client = useFattureClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateInvoice) => unwrap(client.POST('/api/fatture/v1/invoices', { body })),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [...KEY, 'invoices'] })
      void queryClient.invalidateQueries({ queryKey: [...KEY, 'quota'] })
    },
  })
}

/** Aggiorna cliente/stato di una fattura (`PATCH /invoices/{id}`). */
export function useUpdateInvoice() {
  const client = useFattureClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string; body: UpdateInvoice }) =>
      unwrap(
        client.PATCH('/api/fatture/v1/invoices/{id}', {
          params: { path: { id: vars.id } },
          body: vars.body,
        }),
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [...KEY, 'invoices'] }),
  })
}

/** Elimina (soft-delete) una fattura (`DELETE /invoices/{id}`). */
export function useDeleteInvoice() {
  const client = useFattureClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) =>
      unwrap(client.DELETE('/api/fatture/v1/invoices/{id}', { params: { path: { id } } })),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [...KEY, 'invoices'] })
      void queryClient.invalidateQueries({ queryKey: [...KEY, 'quota'] })
    },
  })
}
