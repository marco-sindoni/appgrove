import { describe, it, expect } from 'vitest'
import { requireAuth, requireRole, type GuardContext } from './guards'

const ctx = (over: Partial<GuardContext> = {}): GuardContext => ({
  status: 'authenticated',
  roles: ['platform-admin'],
  ...over,
})

describe('route guards (predicati)', () => {
  it('requireAuth: consente se autenticato, altrimenti → /login', () => {
    expect(requireAuth(ctx())).toBe(true)
    expect(requireAuth(ctx({ status: 'anonymous' }))).toBe('/login')
  })

  it('requireRole(platform-admin): consente col ruolo, → /forbidden senza, → /login se anonimo', () => {
    expect(requireRole('platform-admin')(ctx())).toBe(true)
    expect(requireRole('platform-admin')(ctx({ roles: ['owner'] }))).toBe('/forbidden')
    expect(requireRole('platform-admin')(ctx({ status: 'anonymous' }))).toBe('/login')
  })
})
