// Chiamate al provider auth (`/api/auth/*`, servizio auth: provider locale UC 0010/0058 in dev, provider Cognito UC 0015 in cloud).
// Questi endpoint NON sono nello spec OpenAPI del core → fetch raw qui (non passano dall'api-client generato).
// Gli errori del backend sono RFC 9457 problem+json → li mappiamo in `ApiError` con `toApiError`.
// Il refresh token viaggia SOLO come cookie HttpOnly → `credentials: 'include'`.

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

/** Un account fra cui scegliere all'accesso (UC 0118). Nessuna etichetta di ruolo. */
export interface AccountOption {
  accountId: string
  accountName: string
}

/**
 * Esito di `login`: sessione stabilita, challenge 2FA da superare, oppure **scelta dell'account**
 * (UC 0118) quando la persona appartiene a più account e nessuno è indicato come attivo.
 *
 * Il terzo esito è additivo esattamente come fu il secondo: nessun token finché non ha scelto.
 */
export type LoginResult =
  | { kind: 'session'; tokens: SessionTokens }
  | { kind: 'mfa'; challengeToken: string }
  | { kind: 'chooseAccount'; choiceToken: string; accounts: AccountOption[] }

export interface EnrollResult {
  secret: string
  otpauthUri: string
}

/**
 * Le due forme del collegamento di verifica/reimpostazione (UC 0018).
 *
 * Col provider locale il collegamento porta un `token` unico. In cloud il messaggio lo compone il
 * Custom Message Lambda dentro Cognito, che quando lo compone **non conosce ancora il codice**
 * (Cognito lo sostituisce dopo): il collegamento porta quindi `email` e `code` separati, e il
 * backend li ricompone. Le pagine accettano entrambe le forme, così un utente non resta bloccato
 * per quale ambiente ha generato il suo collegamento.
 */
export type EmailActionLink = { token: string } | { email: string; code: string }

/** Legge dalla query string la forma presente, o null se non c'è nessuna delle due. */
export function emailActionLinkFrom(params: URLSearchParams): EmailActionLink | null {
  const token = params.get('token')
  if (token) return { token }
  const email = params.get('email')
  const code = params.get('code')
  if (email && code) return { email, code }
  return null
}

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

const post = (base: string, path: string, body?: unknown, accessToken?: string) =>
  authFetch(`${base}/api/auth${path}`, {
    method: 'POST',
    body: body === undefined ? undefined : JSON.stringify(body),
    headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : undefined,
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
  return loginOutcome(await post(authBaseUrl, '/login', body))
}

/**
 * `POST /api/auth/login/2fa` → sessione dopo la challenge TOTP, **oppure** la scelta dell'account: i
 * due passaggi sono indipendenti, e chi ha il secondo fattore attivo può avere anche più account.
 */
export async function loginTwoFa(
  authBaseUrl: string,
  body: { challengeToken: string; code: string },
): Promise<LoginResult> {
  return loginOutcome(
    await post(authBaseUrl, '/login/2fa', {
      challenge_token: body.challengeToken,
      code: body.code,
    }),
  )
}

/** `POST /api/auth/login/account` → sessione con l'account scelto (UC 0118). */
export async function chooseAccount(
  authBaseUrl: string,
  body: { choiceToken: string; accountId: string },
): Promise<SessionTokens> {
  const res = await post(authBaseUrl, '/login/account', {
    choice_token: body.choiceToken,
    account_id: body.accountId,
  })
  return toSession((await res.json()) as TokenResponse)
}

/** I tre esiti dell'accesso letti da una sola risposta: la forma è identica fra gli ambienti. */
async function loginOutcome(res: Response): Promise<LoginResult> {
  const json = (await res.json()) as TokenResponse & {
    mfa_required?: boolean
    challenge_token?: string
    account_selection_required?: boolean
    choice_token?: string
    accounts?: { account_id?: string; account_name?: string }[]
  }
  if (json.mfa_required && json.challenge_token) {
    return { kind: 'mfa', challengeToken: json.challenge_token }
  }
  if (json.account_selection_required && json.choice_token) {
    return {
      kind: 'chooseAccount',
      choiceToken: json.choice_token,
      // Le voci incomplete si scartano qui invece di essere inventate a valle: un account senza
      // identificativo non è selezionabile.
      accounts: (json.accounts ?? []).flatMap((a) =>
        a.account_id ? [{ accountId: a.account_id, accountName: a.account_name ?? a.account_id }] : [],
      ),
    }
  }
  return { kind: 'session', tokens: toSession(json) }
}

// ── Flussi (UC 0058) ────────────────────────────────────────────────────────

/**
 * `POST /api/auth/signup` → 201 (verifica email richiesta). Solleva ApiError 409 se email già
 * registrata. `locale` (UC 0018) è la lingua attiva dell'interfaccia: diventa la lingua delle email
 * dell'utente e viene memorizzata sul suo profilo.
 */
export async function signup(
  authBaseUrl: string,
  body: { email: string; password: string; displayName?: string; locale?: string },
): Promise<void> {
  await post(authBaseUrl, '/signup', body)
}

/**
 * `POST /api/auth/verify` → verifica email. Col provider locale la risposta porta i token
 * (**auto-login**); col provider Cognito (UC 0015) la conferma avviene senza credenziali e la
 * risposta è `{status:"confirmed"}` → si ritorna `null` e la UI rimanda al login.
 */
export async function verifyEmail(
  authBaseUrl: string,
  link: EmailActionLink,
): Promise<SessionTokens | null> {
  const res = await post(authBaseUrl, '/verify', link)
  const body = (await res.json()) as TokenResponse | { status: string }
  if ('access_token' in body) return toSession(body)
  return null
}

/** `POST /api/auth/verify/resend` → 202 (risposta neutra anti-enumeration). */
export async function resendVerification(authBaseUrl: string, email: string): Promise<void> {
  await post(authBaseUrl, '/verify/resend', { email })
}

/** `POST /api/auth/password/forgot` → 202 (neutra). */
export async function forgotPassword(authBaseUrl: string, email: string): Promise<void> {
  await post(authBaseUrl, '/password/forgot', { email })
}

/** `POST /api/auth/password/reset` → 204. Accetta entrambe le forme del collegamento (UC 0018). */
export async function resetPassword(
  authBaseUrl: string,
  body: { link: EmailActionLink; password: string },
): Promise<void> {
  await post(authBaseUrl, '/password/reset', { ...body.link, password: body.password })
}

/** `POST /api/auth/invitations/accept` → **auto-login** come member. */
export async function acceptInvitation(
  authBaseUrl: string,
  body: { token: string; password: string; displayName?: string; locale?: string },
): Promise<SessionTokens> {
  const res = await post(authBaseUrl, '/invitations/accept', body)
  return toSession((await res.json()) as TokenResponse)
}

/**
 * `POST /api/auth/invitations/lookup` — che cosa chiedere a chi apre il collegamento d'invito
 * (UC 0118): `register` se quell'indirizzo non ha ancora un'identità (serve la parola d'accesso),
 * `signin` se ce l'ha già (serve solo che si autentichi).
 *
 * Esiste per non far compilare un modulo destinato al rifiuto: una parola d'accesso nuova su
 * un'identità che esiste già sarebbe una seconda identità mascherata, e il backend la rifiuta.
 */
export async function lookupInvitation(
  authBaseUrl: string,
  token: string,
): Promise<{ mode: 'register' | 'signin'; email: string }> {
  const res = await post(authBaseUrl, '/invitations/lookup', { token })
  const json = (await res.json()) as { mode?: string; email?: string }
  return { mode: json.mode === 'signin' ? 'signin' : 'register', email: json.email ?? '' }
}

/**
 * `POST /api/auth/invitations/send` — invia l'email d'invito dato il token grezzo del core (UC 0059).
 * In locale il servizio auth manda la mail a Mailpit; in cloud l'orchestrazione passa dal BFF (stesso path).
 */
export async function sendInvitation(
  authBaseUrl: string,
  body: { email: string; token: string; role?: string; locale?: string },
): Promise<void> {
  await post(authBaseUrl, '/invitations/send', body)
}

/** `POST /api/auth/2fa/enroll` (Bearer) → secret + otpauth URI per l'app authenticator. */
export async function enroll2fa(authBaseUrl: string, accessToken: string): Promise<EnrollResult> {
  const res = await post(authBaseUrl, '/2fa/enroll', undefined, accessToken)
  const json = (await res.json()) as { secret: string; otpauth_uri: string }
  return { secret: json.secret, otpauthUri: json.otpauth_uri }
}

/** `POST /api/auth/2fa/verify` (Bearer) → 204, attiva il TOTP. */
export async function verify2fa(authBaseUrl: string, accessToken: string, code: string): Promise<void> {
  await post(authBaseUrl, '/2fa/verify', { code }, accessToken)
}

/**
 * `GET /api/auth/2fa/status` (Bearer) → vero se il secondo fattore è **attivo** per l'utente in
 * sessione. È il segnale che permette al prodotto di invitare ad attivarlo **solo** chi non l'ha
 * ancora fatto (UC 0097): prima di questa lettura l'invito era cieco e si limitava a farsi chiudere.
 */
export async function twoFaStatus(authBaseUrl: string, accessToken: string): Promise<boolean> {
  const res = await authFetch(`${authBaseUrl}/api/auth/2fa/status`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  const json = (await res.json()) as { enabled?: boolean }
  return json.enabled === true
}
