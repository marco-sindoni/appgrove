import { describe, it, expect, beforeEach } from 'vitest'
import { useAuthStore, getAccessToken } from './authStore'
import { fakeAccessToken, fakeIdToken } from '../test/utils'

describe('authStore', () => {
  beforeEach(() => useAuthStore.getState().clear())

  it('setSession decodifica i claim e marca authenticated; il token resta in memoria', () => {
    useAuthStore.getState().setSession({ accessToken: fakeAccessToken(), idToken: fakeIdToken() })
    const s = useAuthStore.getState()
    expect(s.status).toBe('authenticated')
    expect(s.claims?.tenantId).toBe('tenant-1')
    expect(getAccessToken()).toBe(s.accessToken)
  })

  it('setSession con token invalido → anonymous, nessun token trattenuto', () => {
    useAuthStore.getState().setSession({ accessToken: 'rotto' })
    expect(useAuthStore.getState().status).toBe('anonymous')
    expect(getAccessToken()).toBeNull()
  })

  it('clear azzera la sessione', () => {
    useAuthStore.getState().setSession({ accessToken: fakeAccessToken() })
    useAuthStore.getState().clear()
    expect(useAuthStore.getState().claims).toBeNull()
    expect(useAuthStore.getState().accessToken).toBeNull()
  })
})
