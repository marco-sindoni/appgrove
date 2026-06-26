import { describe, it, expect } from 'vitest'
import { ApiError, toApiError, unwrap } from './problem'

const problemResponse = () =>
  new Response(JSON.stringify({ title: 'Forbidden', detail: 'manca entitlement', status: 402 }), {
    status: 402,
    headers: { 'content-type': 'application/problem+json' },
  })

describe('problem+json mapping', () => {
  it('toApiError legge il corpo problem+json e lo tipizza', async () => {
    const err = await toApiError(problemResponse())
    expect(err).toBeInstanceOf(ApiError)
    expect(err.status).toBe(402)
    expect(err.problem?.title).toBe('Forbidden')
    expect(err.message).toBe('manca entitlement')
  })

  it('toApiError tollera un corpo non-JSON', async () => {
    const err = await toApiError(new Response('boom', { status: 500 }))
    expect(err.status).toBe(500)
    expect(err.problem).toBeNull()
    expect(err.message).toBe('HTTP 500')
  })

  it('unwrap ritorna data su 2xx e solleva ApiError su errore', async () => {
    const ok = await unwrap(
      Promise.resolve({ data: { id: '1' }, response: new Response('', { status: 200 }) }),
    )
    expect(ok).toEqual({ id: '1' })

    await expect(
      unwrap(Promise.resolve({ error: {}, response: problemResponse() })),
    ).rejects.toBeInstanceOf(ApiError)
  })
})
