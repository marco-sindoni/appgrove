import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { unwrap, type ApiClient } from '@appgrove/api-client'
import { useApiClient } from '../../api/apiClient'

/**
 * API dei diritti GDPR self-service (UC 0033): rettifica profilo, export profilo (download JSON
 * sincrono), export account (job asincrono UC 0032 con polling), eliminazione account con grace
 * 14gg, recesso per-app. Tutti gli endpoint sono esenti dai gate di enforcement (#09 F31).
 */

/** Rettifica (art. 16): aggiorna il proprio nome visualizzato (`PATCH /users/me`). */
export function useRectifyDisplayName() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (displayName: string) =>
      unwrap(client.PATCH('/api/platform/v1/users/me', { body: { displayName } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users', 'me'] }),
  })
}

/** Export del profilo (artt. 15/20): scarica il JSON come file (pattern download da blob). */
export async function downloadProfileExport(client: ApiClient) {
  const { data, response } = await client.GET('/api/platform/v1/users/me/export', {
    parseAs: 'blob',
  })
  if (!data) throw new Error(`export profilo fallito (${response.status})`)
  const url = URL.createObjectURL(data as Blob)
  try {
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = 'appgrove-profilo.json'
    anchor.click()
  } finally {
    URL.revokeObjectURL(url)
  }
}

/** Avvia un export account (`kind=account`) o per-app (`kind=app` + slug). Ritorna il job. */
export function useStartGdprExport() {
  const client = useApiClient()
  return useMutation({
    mutationFn: (vars: { kind: 'account' | 'app'; appId?: string }) =>
      unwrap(client.POST('/api/platform/v1/gdpr/exports', { body: vars })),
  })
}

const JOB_POLL_MS = 1500

/** Stato del job di export, con polling finché è in corso (si spegne da solo a job concluso). */
export function useGdprExportJob(jobId: string | null) {
  const client = useApiClient()
  return useQuery({
    queryKey: ['gdpr', 'exports', jobId],
    enabled: !!jobId,
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'QUEUED' || status === 'RUNNING' ? JOB_POLL_MS : false
    },
    queryFn: () =>
      unwrap(
        client.GET('/api/platform/v1/gdpr/exports/{id}', {
          params: { path: { id: jobId as string } },
        }),
      ),
  })
}

/** Link firmato di download dello ZIP (con scadenza, #13 D22.4). */
export function useGdprExportDownload() {
  const client = useApiClient()
  return useMutation({
    mutationFn: (jobId: string) =>
      unwrap(
        client.GET('/api/platform/v1/gdpr/exports/{id}/download', {
          params: { path: { id: jobId } },
        }),
      ),
  })
}

/** Richiede l'eliminazione dell'account (grace 14gg, #13 E25). OWNER-only lato core. */
export function useRequestAccountDeletion() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => unwrap(client.POST('/api/platform/v1/accounts/me/deletion', {})),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['accounts', 'me'] })
      void queryClient.invalidateQueries({ queryKey: ['me', 'entitlements'] })
    },
  })
}

/** Annulla l'eliminazione entro la grace: l'account torna attivo. */
export function useCancelAccountDeletion() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => unwrap(client.DELETE('/api/platform/v1/accounts/me/deletion', {})),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['accounts', 'me'] })
      void queryClient.invalidateQueries({ queryKey: ['me', 'entitlements'] })
    },
  })
}

/** Recesso per-app (esporta → conferma → cancella): la conferma porta l'export COMPLETED. */
export function useWithdrawFromApp() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { appSlug: string; exportJobId: string }) =>
      unwrap(
        client.POST('/api/platform/v1/gdpr/apps/{appSlug}/withdrawal', {
          params: { path: { appSlug: vars.appSlug } },
          body: { exportJobId: vars.exportJobId },
        }),
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['me', 'subscriptions'] })
      void queryClient.invalidateQueries({ queryKey: ['me', 'entitlements'] })
    },
  })
}
