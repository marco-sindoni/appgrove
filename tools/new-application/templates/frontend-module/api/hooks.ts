import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { unwrap } from '@appgrove/api-client'
import { use@@APP_CLASS@@Client } from './@@APP_CLASS@@ClientProvider'
import type { CreateItem, UpdateItem } from './client'

const KEY = ['@@APP_ID@@'] as const

/** Lista record paginata (`GET /items`). Tenant-scoped lato server (discriminator). */
export function useItems(page = 0, size = 20) {
  const client = use@@APP_CLASS@@Client()
  return useQuery({
    queryKey: [...KEY, 'items', 'list', page, size],
    queryFn: () =>
      unwrap(client.GET('/api/@@APP_ID@@/v1/items', { params: { query: { page, size } } })),
  })
}

/** Dettaglio di un record (`GET /items/{id}`). */
export function useItemDetail(id: string | undefined) {
  const client = use@@APP_CLASS@@Client()
  return useQuery({
    queryKey: [...KEY, 'items', 'detail', id],
    enabled: !!id,
    queryFn: () =>
      unwrap(
        client.GET('/api/@@APP_ID@@/v1/items/{id}', { params: { path: { id: id as string } } }),
      ),
  })
}

/** Stato quota della metrica `@@METRIC@@` (`GET /quota`): alimenta il banner consumo/limite. */
export function use@@APP_CLASS@@Quota() {
  const client = use@@APP_CLASS@@Client()
  return useQuery({
    queryKey: [...KEY, 'quota'],
    queryFn: () => unwrap(client.GET('/api/@@APP_ID@@/v1/quota')),
  })
}

/** Crea un record (`POST /items`). A quota raggiunta il backend risponde 429 (gestito in UI). */
export function useCreateItem() {
  const client = use@@APP_CLASS@@Client()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateItem) => unwrap(client.POST('/api/@@APP_ID@@/v1/items', { body })),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [...KEY, 'items'] })
      void queryClient.invalidateQueries({ queryKey: [...KEY, 'quota'] })
    },
  })
}

/** Aggiorna contatto/stato di un record (`PATCH /items/{id}`). */
export function useUpdateItem() {
  const client = use@@APP_CLASS@@Client()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string; body: UpdateItem }) =>
      unwrap(
        client.PATCH('/api/@@APP_ID@@/v1/items/{id}', {
          params: { path: { id: vars.id } },
          body: vars.body,
        }),
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [...KEY, 'items'] }),
  })
}

/** Elimina (soft-delete) un record (`DELETE /items/{id}`). */
export function useDeleteItem() {
  const client = use@@APP_CLASS@@Client()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) =>
      unwrap(client.DELETE('/api/@@APP_ID@@/v1/items/{id}', { params: { path: { id } } })),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [...KEY, 'items'] })
      void queryClient.invalidateQueries({ queryKey: [...KEY, 'quota'] })
    },
  })
}
