/**
 * api — creazione programmatica di tenant/sessioni via API della suite (UC 0090).
 *
 * `tenant()` e `login()` servono ai journey che NON stanno collaudando la registrazione o
 * l'accesso dal browser (UC 0091/0092) ma hanno bisogno di un tenant fresco o di una
 * sessione come precondizione: passano dalle stesse API pubbliche dell'auth (signup +
 * email di verifica vera da Mailpit + verify), mai da scritture dirette sul database.
 *
 * Gli helper `paddle` (checkout programmatico + webhook sintetici firmati, UC 0023) e `totp`
 * (codici 2FA a tempo) vivono nei moduli omonimi (primi consumatori: UC 0091).
 */
import { waitForEmail, extractLink } from './mailbox'

const AUTH_API = process.env.PLATFORM_AUTH_API ?? 'http://localhost:21100'

/** Password conforme alla policy (#02 dec.19: ≥10 char, maiuscola, minuscola, cifra). */
export const DEFAULT_PASSWORD = 'Password1!'

/**
 * Amministratore di piattaforma del seed (gruppo JWT `platform-admin`). Non ha riga credenziali,
 * quindi entra con la password universale di sviluppo — la stessa che usa il seed dello stack dev.
 * Unica utenza della suite che non nasce da un signup: è parte delle precondizioni (UC 0092 §3).
 */
export const ADMIN_EMAIL = 'admin@appgrove.test'
export const ADMIN_PASSWORD = DEFAULT_PASSWORD

export interface Tokens {
  access_token: string
  id_token: string
  token_type: string
}

export interface Tenant {
  email: string
  password: string
  displayName: string
  /** tenant_id = account id, letto dal claim del JWT emesso (mai da input del client). */
  tenantId: string
  userSub: string
  tokens: Tokens
}

/** Email sintetica unica per run: dominio non recapitabile, mai utenze reali. */
export function uniqueEmail(tag: string): string {
  const runId = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`
  return `e2e+${tag}-${runId}@test.appgrove.local`
}

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${AUTH_API}${path}`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`${path}: HTTP ${res.status} — ${await res.text()}`)
  return (await res.json()) as T
}

/** Payload (non verificato) di un JWT: sufficiente per leggere i claim nei test. */
export function jwtPayload(token: string): Record<string, unknown> {
  return JSON.parse(Buffer.from(token.split('.')[1], 'base64url').toString('utf8'))
}

/**
 * Crea un tenant FRESCO via API: signup → email di verifica REALE (Mailpit) → verify
 * (auto-login del provider locale). Ritorna la sessione e il tenant_id letto dal JWT.
 */
export async function tenant(tag = 'tenant'): Promise<Tenant> {
  const email = uniqueEmail(tag)
  const displayName = `E2E ${tag}`
  await postJson('/api/auth/signup', { email, password: DEFAULT_PASSWORD, displayName, locale: 'en' })
  const message = await waitForEmail(email)
  const token = new URL(extractLink(message, '/verify')).searchParams.get('token')
  if (!token) throw new Error('link di verifica senza parametro token')
  const tokens = await postJson<Tokens>('/api/auth/verify', { token })
  const claims = jwtPayload(tokens.access_token)
  return {
    email,
    password: DEFAULT_PASSWORD,
    displayName,
    tenantId: String(claims.tenant_id),
    userSub: String(claims.sub),
    tokens,
  }
}

/**
 * Accesso via API. Nella suite il bypass dev del 2FA è DISATTIVATO (run.sh): per un utente col
 * 2FA attivo la risposta è la challenge, non i token — quei casi usano loginRaw()/loginMfa().
 */
export async function login(email: string, password: string): Promise<Tokens> {
  return postJson<Tokens>('/api/auth/login', { email, password })
}

/** Risposta di login "grezza": o i token, o la challenge del secondo fattore. */
export type LoginRaw = Tokens | { mfa_required: true; challenge_token: string }

export async function loginRaw(email: string, password: string): Promise<LoginRaw> {
  return postJson<LoginRaw>('/api/auth/login', { email, password })
}

/** Secondo passo del login con 2FA: challenge + codice a tempo → token. */
export async function loginMfa(challengeToken: string, code: string): Promise<Tokens> {
  return postJson<Tokens>('/api/auth/login/2fa', { challenge_token: challengeToken, code })
}

/**
 * Attesa a polling su condizione (mai sleep ciechi nei journey): ritenta `check` ogni 500ms
 * fino a `timeoutMs`, poi fallisce col messaggio parlante fornito dal chiamante.
 */
export async function pollUntil(
  check: () => Promise<boolean> | boolean,
  opts: { timeoutMs?: number; message: string; intervalMs?: number },
): Promise<void> {
  const timeoutMs = opts.timeoutMs ?? 30_000
  const intervalMs = opts.intervalMs ?? 500
  const deadline = Date.now() + timeoutMs
  for (;;) {
    if (await check()) return
    if (Date.now() > deadline) {
      throw new Error(`${opts.message} (attesa scaduta dopo ${Math.round(timeoutMs / 1000)}s)`)
    }
    await new Promise((r) => setTimeout(r, intervalMs))
  }
}

/** Fetch autenticata verso una API della suite (Bearer access token). */
export async function authedFetch(
  base: string,
  path: string,
  tokens: Tokens,
  init: { method?: string; body?: unknown } = {},
): Promise<Response> {
  return fetch(`${base}${path}`, {
    method: init.method ?? (init.body !== undefined ? 'POST' : 'GET'),
    headers: {
      authorization: `Bearer ${tokens.access_token}`,
      ...(init.body !== undefined ? { 'content-type': 'application/json' } : {}),
    },
    body: init.body !== undefined ? JSON.stringify(init.body) : undefined,
  })
}
