// Decodifica dei claim JWT per la **sola UX** (display profilo, ruoli per le route guard).
// L'enforcement vero resta nel backend (#09 dec.30): qui non si verifica la firma.
// INVARIANTE appgrove: `tenant_id`/`user_id` provengono SOLO dai claim del token verificato lato
// server (mai da params/body). La shell li legge dal token e li propaga via Context ai moduli.

export interface SessionClaims {
  /** claim `sub`. */
  userId: string
  /** claim `tenant_id` (= account). */
  tenantId: string
  /** claim `roles` (ruoli tenant + eventuale `platform-admin`). */
  roles: string[]
  email?: string
  name?: string
}

function decodePayload(token: string): Record<string, unknown> | null {
  const part = token.split('.')[1]
  if (!part) return null
  try {
    const base64 = part.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join(''),
    )
    return JSON.parse(json) as Record<string, unknown>
  } catch {
    return null
  }
}

const toStringArray = (v: unknown): string[] =>
  Array.isArray(v) ? v.filter((x): x is string => typeof x === 'string') : typeof v === 'string' ? [v] : []

/**
 * Estrae i claim di sessione dall'access token (tenant/user/roles), arricchendo email/name
 * dall'id token quando disponibile. Ritorna null se mancano i claim essenziali.
 */
export function decodeClaims(accessToken: string, idToken?: string | null): SessionClaims | null {
  const access = decodePayload(accessToken)
  if (!access) return null
  const tenantId = typeof access.tenant_id === 'string' ? access.tenant_id : ''
  const userId = typeof access.sub === 'string' ? access.sub : ''
  if (!tenantId || !userId) return null

  const id = idToken ? decodePayload(idToken) : null
  const email =
    (id && typeof id.email === 'string' && id.email) ||
    (typeof access.upn === 'string' ? access.upn : undefined)
  const name = id && typeof id.name === 'string' ? id.name : undefined

  return { userId, tenantId, roles: toStringArray(access.roles), email, name }
}
