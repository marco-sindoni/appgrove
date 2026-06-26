import { describe, it, expect } from 'vitest'
import { requireAuth, requireRole, requireEntitlement, type GuardContext } from './guards'

const ctx = (over: Partial<GuardContext> = {}): GuardContext => ({
  status: 'authenticated',
  roles: ['owner'],
  entitled: ['demo'],
  ...over,
})

describe('route guards (predicati)', () => {
  it('requireAuth: consente se autenticato, altrimenti → /login', () => {
    expect(requireAuth(ctx())).toBe(true)
    expect(requireAuth(ctx({ status: 'anonymous' }))).toBe('/login')
  })

  it('requireRole: consente col ruolo, → /forbidden senza, → /login se anonimo', () => {
    expect(requireRole('owner')(ctx())).toBe(true)
    expect(requireRole('platform-admin')(ctx())).toBe('/forbidden')
    expect(requireRole('owner')(ctx({ status: 'anonymous' }))).toBe('/login')
  })

  it('requireEntitlement: consente se entitled, → /forbidden se non, → /login se anonimo', () => {
    expect(requireEntitlement('demo')(ctx())).toBe(true)
    expect(requireEntitlement('demo')(ctx({ entitled: [] }))).toBe('/forbidden')
    expect(requireEntitlement('demo')(ctx({ status: 'anonymous' }))).toBe('/login')
  })
})
