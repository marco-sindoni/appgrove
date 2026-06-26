import { describe, it, expect, vi } from 'vitest'
import createClient from 'openapi-fetch'
import { authMiddleware, RETRY_HEADER } from './auth-middleware'

// Test del middleware in isolamento (indipendente dallo spec generato): usa un fetch finto.
const makeClient = (cfg: Parameters<typeof authMiddleware>[0]) => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const client: any = createClient<any>({ baseUrl: 'https://api.test', fetch: cfg.fetch })
  client.use(authMiddleware(cfg))
  return client
}

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  })

describe('authMiddleware', () => {
  it('inietta Authorization: Bearer quando un token è presente', async () => {
    const seen: string[] = []
    const fetchMock = vi.fn(async (req: Request) => {
      seen.push(req.headers.get('authorization') ?? '')
      return json(200, { ok: true })
    })
    const client = makeClient({
      getAccessToken: () => 'access-123',
      refresh: async () => true,
      fetch: fetchMock as unknown as typeof fetch,
    })

    await client.GET('/users/me' as never)
    expect(seen[0]).toBe('Bearer access-123')
  })

  it('su 401 chiama refresh e ritenta la richiesta con il nuovo token', async () => {
    let token = 'stale'
    const refresh = vi.fn(async () => {
      token = 'fresh'
      return true
    })
    const calls: { auth: string; retried: string | null }[] = []
    const fetchMock = vi.fn(async (req: Request) => {
      calls.push({
        auth: req.headers.get('authorization') ?? '',
        retried: req.headers.get(RETRY_HEADER),
      })
      // prima chiamata → 401; il retry (header RETRY_HEADER) → 200
      return req.headers.get(RETRY_HEADER) ? json(200, { ok: true }) : json(401, { title: 'expired' })
    })
    const client = makeClient({
      getAccessToken: () => token,
      refresh,
      fetch: fetchMock as unknown as typeof fetch,
    })

    const { response, data } = await client.GET('/users/me' as never)

    expect(refresh).toHaveBeenCalledOnce()
    expect(response.status).toBe(200)
    expect(data).toEqual({ ok: true })
    expect(calls).toHaveLength(2)
    expect(calls[0]).toEqual({ auth: 'Bearer stale', retried: null })
    expect(calls[1]).toEqual({ auth: 'Bearer fresh', retried: '1' })
  })

  it('se il refresh fallisce, invoca onAuthFailure e propaga la 401', async () => {
    const onAuthFailure = vi.fn()
    const fetchMock = vi.fn(async () => json(401, { title: 'expired' }))
    const client = makeClient({
      getAccessToken: () => 'stale',
      refresh: async () => false,
      onAuthFailure,
      fetch: fetchMock as unknown as typeof fetch,
    })

    const { response } = await client.GET('/users/me' as never)

    expect(onAuthFailure).toHaveBeenCalledOnce()
    expect(response.status).toBe(401)
    // nessun retry: una sola chiamata di rete
    expect(fetchMock).toHaveBeenCalledOnce()
  })
})
