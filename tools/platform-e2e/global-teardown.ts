/**
 * global-teardown — riporta la suite allo stato di riposo, in due mosse:
 *
 * 1. **servizi tutti su** (UC 0092): F-DEGRADE ne ferma davvero uno, e un journey morto nel punto
 *    sbagliato lo lascerebbe giù — con la corsa successiva rossa per una ragione che non è sua.
 *    Il ripristino dentro il journey copre il caso normale; questo copre il caso patologico.
 * 2. **catalogo al suo posto**: `crm` torna `inactive` (com'è nello YAML del listino, decisione
 *    della change 0042). Best-effort: se la suite è morta a metà, la sync pricing del prossimo
 *    avvio riallinea comunque il database allo YAML.
 */
import { setAppStatus } from './global-setup'
import { discoveredServices, ensureServiceUp } from './helpers/services'

export default async function globalTeardown(): Promise<void> {
  for (const service of discoveredServices()) {
    const problem = ensureServiceUp(service)
    // eslint-disable-next-line no-console
    if (problem) console.warn(problem)
  }
  try {
    await setAppStatus('crm', 'inactive')
  } catch (e) {
    // eslint-disable-next-line no-console
    console.warn(`ripristino stato crm non riuscito (riallineato dalla sync al prossimo avvio): ${String(e)}`)
  }
}
