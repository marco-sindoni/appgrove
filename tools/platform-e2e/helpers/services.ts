/**
 * services — controllo del ciclo di vita dei servizi della suite dai journey (UC 0090 §5, UC 0092).
 *
 * È la leva che permette a F-DEGRADE di produrre un guasto **vero**: il processo del servizio
 * viene fermato davvero, non se ne simula la risposta. Sotto c'è `service-ctl.sh`, che riavvia
 * il servizio con la stessa ricetta con cui `run.sh` l'aveva avviato (descrittore `.run/services.json`).
 *
 * Un journey che usa questa leva agisce su stato **globale** della suite: deve girare da solo,
 * in coda alla batteria (progetti seriali di `playwright.config.ts`), e deve ripristinare ciò che
 * ha fermato **anche quando fallisce** — la rete di sicurezza finale è nel global-teardown.
 */
import { execFileSync } from 'node:child_process'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

// La cartella dello strumento arriva da run.sh (che è anche la cartella di lavoro di Playwright):
// niente percorsi dedotti dal modulo, che dipenderebbero da come il transpilatore lo carica.
const TOOL_DIR = process.env.PLATFORM_TOOL_DIR ?? process.cwd()
const CTL = join(TOOL_DIR, 'service-ctl.sh')

/** Nome del modulo del servizio di piattaforma (quello che serve gli entitlement). */
export const CORE_SERVICE = 'core'

/**
 * Servizi governati dalla suite, dal descrittore generato da `run.sh` a partire dalla scoperta
 * automatica: una nuova app vi compare da sola. Elenco vuoto se la suite non è avviata (chi
 * chiama non deve rompersi: è una domanda sull'ambiente, non un'asserzione).
 */
export function discoveredServices(): string[] {
  const descriptor = join(TOOL_DIR, '.run', 'services.json')
  if (!existsSync(descriptor)) return []
  return Object.keys(JSON.parse(readFileSync(descriptor, 'utf8')) as Record<string, unknown>)
}

function ctl(command: string, service: string): { code: number; output: string } {
  try {
    const output = execFileSync(CTL, [command, service], { encoding: 'utf8', stdio: 'pipe' })
    return { code: 0, output }
  } catch (e) {
    const err = e as { status?: number; stdout?: string; stderr?: string }
    return { code: err.status ?? 1, output: `${err.stdout ?? ''}${err.stderr ?? ''}` }
  }
}

/** Il servizio è in ascolto? (nessuna eccezione: è una domanda, non un'asserzione). */
export function isServiceUp(service: string): boolean {
  return ctl('status', service).code === 0
}

/** Ferma davvero il servizio e attende che la porta sia libera. Fallisce parlando. */
export function stopService(service: string): void {
  const { code, output } = ctl('stop', service)
  if (code !== 0) throw new Error(`stop del servizio '${service}' fallito: ${output.trim()}`)
  if (isServiceUp(service)) throw new Error(`il servizio '${service}' risulta ancora in ascolto dopo lo stop`)
}

/** Riavvia il servizio e attende che risponda pronto. Idempotente: se è su, non fa nulla. */
export function startService(service: string): void {
  const { code, output } = ctl('start', service)
  if (code !== 0) throw new Error(`avvio del servizio '${service}' fallito: ${output.trim()}`)
}

/**
 * Ripristino di fine journey: rimette su il servizio se è giù, senza far fallire il ripristino
 * stesso quando il journey è già rosso per un'altra ragione (l'errore vero non va coperto).
 * Ritorna il messaggio del guasto di ripristino, se c'è stato.
 */
export function ensureServiceUp(service: string): string | null {
  if (isServiceUp(service)) return null
  const { code, output } = ctl('start', service)
  return code === 0 ? null : `ripristino del servizio '${service}' fallito: ${output.trim()}`
}
