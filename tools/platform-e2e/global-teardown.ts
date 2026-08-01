/**
 * global-teardown — ripristina lo stato di prodotto del catalogo: `crm` torna `inactive`
 * (com'è nello YAML del listino, decisione della change 0042). Best-effort: se la suite è
 * morta a metà, la sync pricing del prossimo avvio riallinea comunque il DB allo YAML.
 */
import { setAppStatus } from './global-setup'

export default async function globalTeardown(): Promise<void> {
  try {
    await setAppStatus('crm', 'inactive')
  } catch (e) {
    // eslint-disable-next-line no-console
    console.warn(`ripristino stato crm non riuscito (riallineato dalla sync al prossimo avvio): ${String(e)}`)
  }
}
