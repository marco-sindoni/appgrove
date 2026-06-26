// Chiamate raw al provider auth (`/api/auth/*`, auth-local UC 0010 / Cognito BFF in cloud).
// Questi endpoint NON sono nello spec OpenAPI del core, quindi non passano dall'api-client generato.
// Il refresh token viaggia SOLO come cookie HttpOnly → `credentials: 'include'`.

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

/** `POST /api/auth/refresh` (cookie) → nuovi token, o null se la sessione non è ripristinabile. */
export async function refreshSession(authBaseUrl: string): Promise<SessionTokens | null> {
  let res: Response
  try {
    res = await fetch(`${authBaseUrl}/api/auth/refresh`, {
      method: 'POST',
      credentials: 'include',
    })
  } catch {
    return null
  }
  if (!res.ok) return null
  const body = (await res.json()) as TokenResponse
  if (!body.access_token) return null
  return { accessToken: body.access_token, idToken: body.id_token ?? null }
}

/** `POST /api/auth/logout` → invalida il cookie refresh lato server. */
export async function logoutSession(authBaseUrl: string): Promise<void> {
  try {
    await fetch(`${authBaseUrl}/api/auth/logout`, { method: 'POST', credentials: 'include' })
  } catch {
    // best-effort: lo stato client viene comunque azzerato dal chiamante
  }
}
