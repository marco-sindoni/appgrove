// Chiamate al provider auth (`/api/auth/*`, servizio auth: provider locale UC 0010/0058 in dev, provider Cognito UC 0015 in cloud).
// Questi endpoint NON sono nello spec OpenAPI del core → fetch raw qui (non passano dall'api-client generato).
// Gli errori del backend sono RFC 9457 problem+json → li mappiamo in `ApiError` con `toApiError`.
// Il refresh token viaggia SOLO come cookie HttpOnly → `credentials: 'include'`.
//
// La console admin usa solo il sottoinsieme sessione (refresh/logout/login + login 2FA): nessun
// signup/verify/reset/accept/enroll (gli admin di piattaforma sono provisionati, non si auto-registrano).

import { ApiError, toApiError } from '@appgrove/api-client'

interface TokenResponse {
  access_token: string
  id_token?: string
  token_type?: string
  expires_in?: number
}

export interface SessionTokens {
  accessToken: string
  idToken: string | null
}

/** Esito di `login`: sessione stabilita, oppure challenge 2FA da superare. */
export type LoginResult =
  | { kind: 'session'; tokens: SessionTokens }
  | { kind: 'mfa'; challengeToken: string }

const toSession = (body: TokenResponse): SessionTokens => ({
  accessToken: body.access_token,
  idToken: body.id_token ?? null,
})

/** fetch JSON con credenziali; solleva {@link ApiError} (problem+json) su risposta non-ok. */
async function authFetch(url: string, init: RequestInit = {}): Promise<Response> {
  let res: Response
  try {
    res = await fetch(url, {
      credentials: 'include',
      ...init,
      headers: { 'content-type': 'application/json', ...(init.headers ?? {}) },
    })
  } catch {
    throw new ApiError(0, null, 'network')
  }
  if (!res.ok) throw await toApiError(res)
  return res
}

const post = (base: string, path: string, body?: unknown) =>
  authFetch(`${base}/api/auth${path}`, {
    method: 'POST',
    body: body === undefined ? undefined : JSON.stringify(body),
  })

// ── Sessione (UC 0010) ──────────────────────────────────────────────────────

/** `POST /api/auth/refresh` (cookie) → nuovi token, o null se la sessione non è ripristinabile. */
export async function refreshSession(authBaseUrl: string): Promise<SessionTokens | null> {
  let res: Response
  try {
    res = await fetch(`${authBaseUrl}/api/auth/refresh`, { method: 'POST', credentials: 'include' })
  } catch {
    return null
  }
  if (!res.ok) return null
  const body = (await res.json()) as TokenResponse
  if (!body.access_token) return null
  return toSession(body)
}

/** `POST /api/auth/logout` → invalida il cookie refresh lato server (best-effort). */
export async function logoutSession(authBaseUrl: string): Promise<void> {
  try {
    await fetch(`${authBaseUrl}/api/auth/logout`, { method: 'POST', credentials: 'include' })
  } catch {
    // lo stato client viene comunque azzerato dal chiamante
  }
}

/** `POST /api/auth/login` → sessione, oppure challenge 2FA se l'utente ha il TOTP attivo. */
export async function login(
  authBaseUrl: string,
  body: { email: string; password: string },
): Promise<LoginResult> {
  const res = await post(authBaseUrl, '/login', body)
  const json = (await res.json()) as TokenResponse & { mfa_required?: boolean; challenge_token?: string }
  if (json.mfa_required && json.challenge_token) {
    return { kind: 'mfa', challengeToken: json.challenge_token }
  }
  return { kind: 'session', tokens: toSession(json) }
}

/** `POST /api/auth/login/2fa` → sessione dopo la challenge TOTP. */
export async function loginTwoFa(
  authBaseUrl: string,
  body: { challengeToken: string; code: string },
): Promise<SessionTokens> {
  const res = await post(authBaseUrl, '/login/2fa', {
    challenge_token: body.challengeToken,
    code: body.code,
  })
  return toSession((await res.json()) as TokenResponse)
}
