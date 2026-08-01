/**
 * global-setup — stato di catalogo deterministico per l'INTERA esecuzione (UC 0091, decisione 3
 * della change 0070): attiva l'app `crm` con la leva admin sanzionata. Nel catalogo attuale è
 * l'unica combinazione (modulo nel registry frontend × tier a pagamento) e il suo `status:
 * inactive` è una scelta di prodotto (change 0042) che i journey non toccano: l'attivazione è
 * a runtime (PATCH admin), la sync pricing di ogni avvio della suite riparte comunque dallo YAML.
 *
 * Eseguito una volta, PRIMA di ogni journey: così J-REG (sidebar ≡ entitlements, baseline
 * freemium inclusa) vede uno stato stabile e nessun journey osserva il catalogo cambiare in volo.
 */
import { ADMIN_EMAIL, ADMIN_PASSWORD, login } from './helpers/api'

const CORE_API = process.env.PLATFORM_CORE_API ?? 'http://localhost:20080'

async function setAppStatus(slug: string, status: string): Promise<void> {
  const tokens = await login(ADMIN_EMAIL, ADMIN_PASSWORD)
  const headers = { authorization: `Bearer ${tokens.access_token}` }
  const res = await fetch(`${CORE_API}/api/platform/v1/admin/apps`, { headers })
  if (!res.ok) throw new Error(`lista app admin: HTTP ${res.status} — ${await res.text()}`)
  const apps = (await res.json()) as Array<{ id: string; slug: string; status: string }>
  const app = apps.find((a) => a.slug === slug)
  if (!app) throw new Error(`app '${slug}' assente dal catalogo admin`)
  if (app.status === status) return
  const patch = await fetch(`${CORE_API}/api/platform/v1/admin/apps/${app.id}`, {
    method: 'PATCH',
    headers: { ...headers, 'content-type': 'application/json' },
    body: JSON.stringify({ status }),
  })
  if (!patch.ok) throw new Error(`PATCH app '${slug}' → ${status}: HTTP ${patch.status} — ${await patch.text()}`)
}

export default async function globalSetup(): Promise<void> {
  await setAppStatus('crm', 'active')
}

export { setAppStatus }
