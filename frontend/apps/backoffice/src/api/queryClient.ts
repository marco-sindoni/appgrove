import { QueryClient } from '@tanstack/react-query'

/** QueryClient dell'app: errori 4xx (es. 401 dopo refresh fallito, 402 entitlement) non si ritentano. */
export function makeQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: 1, refetchOnWindowFocus: false },
      mutations: { retry: 0 },
    },
  })
}
