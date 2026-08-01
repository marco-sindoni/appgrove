/**
 * paddle — interazione col fornitore di pagamento finto (UC 0091, primo consumatore dell'helper
 * annunciato da UC 0090): acquisto programmatico via checkout reale e **webhook sintetici
 * firmati** inviati all'ingest vero (stessa pipeline del prodotto: ingest → coda → consumer).
 *
 * La firma è quella del contratto Paddle emulato (PaddleSignature, UC 0023):
 *   Paddle-Signature: ts=<unix seconds>;h1=hex(HMAC-SHA256(secret, ts + ":" + body))
 * col secret dev di services/core (application.properties). Nessuna scrittura diretta su DB:
 * la subscription si materializza solo attraverso la pipeline, come in produzione.
 */
import { createHmac, randomBytes } from 'node:crypto'
import type { Tokens } from './api'
import { pollUntil } from './api'

const CORE_API = process.env.PLATFORM_CORE_API ?? 'http://localhost:20080'
/** Secret dev dell'ingest webhook (services/core application.properties, profilo dev). */
const WEBHOOK_SECRET = 'local-dev-paddle-webhook-secret'

/** Header `Paddle-Signature` valido per `body` (schema ts/h1 di PaddleSignature). */
export function paddleSignature(body: string, ts: number = Math.floor(Date.now() / 1000)): string {
  const h1 = createHmac('sha256', WEBHOOK_SECRET).update(`${ts}:${body}`).digest('hex')
  return `ts=${ts};h1=${h1}`
}

export interface SubscriptionEventInput {
  /** es. 'subscription.canceled' | 'subscription.updated' | 'subscription.activated' */
  eventType: string
  /** snapshot dello stato: 'active' | 'canceled' | 'past_due' | 'paused' | 'trialing' */
  status: string
  tenantId: string
  appId: string
  appTierId?: string
  paddleSubscriptionId?: string
  occurredAt?: Date
  currentPeriodStart?: Date
  currentPeriodEnd?: Date
  cancelAt?: Date | null
}

/** Corpo canonico di un webhook subscription.* (stesso formato dello StubScenarioEmitter). */
export function subscriptionEvent(input: SubscriptionEventInput): string {
  const occurred = input.occurredAt ?? new Date()
  return JSON.stringify({
    event_id: `evt_${randomBytes(10).toString('hex')}`,
    event_type: input.eventType,
    occurred_at: occurred.toISOString(),
    data: {
      paddle_subscription_id: input.paddleSubscriptionId ?? `sub_${randomBytes(12).toString('hex')}`,
      status: input.status,
      current_period_start: (input.currentPeriodStart ?? occurred).toISOString(),
      current_period_end: (input.currentPeriodEnd ?? new Date(occurred.getTime() + 30 * 86_400_000)).toISOString(),
      cancel_at: input.cancelAt ? input.cancelAt.toISOString() : null,
      trial_end: null,
      scheduled_change_at: null,
      custom_data: {
        tenant_id: input.tenantId,
        app_id: input.appId,
        ...(input.appTierId ? { app_tier_id: input.appTierId } : {}),
      },
    },
  })
}

/** Invia un webhook firmato all'ingest reale; fallisce con messaggio parlante se non è accettato. */
export async function sendPaddleWebhook(body: string): Promise<void> {
  const res = await fetch(`${CORE_API}/api/platform/v1/webhooks/paddle`, {
    method: 'POST',
    headers: { 'content-type': 'application/json', 'Paddle-Signature': paddleSignature(body) },
    body,
  })
  if (!res.ok) {
    throw new Error(`ingest webhook Paddle: HTTP ${res.status} — ${await res.text()}`)
  }
}

export interface TierInfo {
  tierId: string
  key: string
  name: string
}

export interface AppCatalogInfo {
  appId: string
  slug: string
  name: string
  tiers: TierInfo[]
}

/** Catalogo checkout di un'app (id + tier), via API autenticata del core. */
export async function appCatalog(tokens: Tokens, appSlug: string): Promise<AppCatalogInfo> {
  const res = await fetch(`${CORE_API}/api/platform/v1/checkout/apps/${appSlug}/tiers`, {
    headers: { authorization: `Bearer ${tokens.access_token}` },
  })
  if (!res.ok) throw new Error(`catalogo checkout di '${appSlug}': HTTP ${res.status} — ${await res.text()}`)
  return (await res.json()) as AppCatalogInfo
}

/**
 * Acquisto programmatico: avvia il checkout reale (l'attivazione stub emette i webhook sulla
 * pipeline) e attende a polling che la subscription risulti attiva per il chiamante.
 * È la precondizione "tenant con app attivata" dei journey che non collaudano il checkout stesso.
 */
export async function buyApp(
  tokens: Tokens,
  appSlug: string,
  tierKey: string,
  billingCycle: 'monthly' | 'annual' = 'monthly',
): Promise<void> {
  const res = await fetch(`${CORE_API}/api/platform/v1/checkout/apps/${appSlug}`, {
    method: 'POST',
    headers: { 'content-type': 'application/json', authorization: `Bearer ${tokens.access_token}` },
    body: JSON.stringify({ tierKey, billingCycle }),
  })
  if (!res.ok) throw new Error(`checkout di '${appSlug}' (${tierKey}): HTTP ${res.status} — ${await res.text()}`)
  await pollUntil(
    async () => {
      const sub = await fetch(`${CORE_API}/api/platform/v1/checkout/apps/${appSlug}/subscription`, {
        headers: { authorization: `Bearer ${tokens.access_token}` },
      })
      if (!sub.ok) return false
      return ((await sub.json()) as { active: boolean }).active
    },
    { timeoutMs: 30_000, message: `subscription '${appSlug}' non attiva dopo il checkout (pipeline webhook)` },
  )
}
