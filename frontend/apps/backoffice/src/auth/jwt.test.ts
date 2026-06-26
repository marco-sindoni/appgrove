import { describe, it, expect } from 'vitest'
import { decodeClaims } from './jwt'
import { fakeAccessToken, fakeIdToken } from '../test/utils'

describe('decodeClaims', () => {
  it('estrae tenant_id/sub/roles dall’access token e email/name dall’id token', () => {
    const claims = decodeClaims(fakeAccessToken(), fakeIdToken())
    expect(claims).toEqual({
      userId: 'user-1',
      tenantId: 'tenant-1',
      roles: ['owner'],
      email: 'u@x.io',
      name: 'Utente Uno',
    })
  })

  it('ritorna null se mancano i claim essenziali (tenant_id/sub)', () => {
    expect(decodeClaims(fakeAccessToken({ tenant_id: undefined }))).toBeNull()
    expect(decodeClaims('non-un-jwt')).toBeNull()
  })

  it('normalizza roles assente a array vuoto', () => {
    expect(decodeClaims(fakeAccessToken({ roles: undefined }))?.roles).toEqual([])
  })
})
