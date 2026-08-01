/**
 * mailbox — lettura delle email REALMENTE spedite dallo stack, via API HTTP di Mailpit (UC 0090).
 *
 * Mailpit è il ≈SES dello stack locale (già nel compose dev): i servizi spediscono su SMTP
 * :1025, questa libreria interroga l'API di lettura (default :8025). Niente attese a tempo
 * fisso: polling su condizione con timeout PARLANTE — un journey non deve mai morire di
 * timeout anonimo del browser per un'email mancata.
 */

const API = process.env.PLATFORM_MAILPIT_API ?? 'http://localhost:8025'

export interface MailpitMessage {
  ID: string
  From: { Address: string; Name: string }
  To: Array<{ Address: string }>
  Subject: string
  /** Corpo testuale (presente solo nel dettaglio, non nel sommario di ricerca). */
  Text: string
  HTML: string
}

async function getJson<T>(path: string): Promise<T> {
  const res = await fetch(`${API}${path}`)
  if (!res.ok) throw new Error(`Mailpit API ${path}: HTTP ${res.status}`)
  return (await res.json()) as T
}

/**
 * Attende (polling, non sleep cieco) l'email più recente per `to` e ne ritorna il DETTAGLIO
 * completo (corpo incluso). Fallisce con messaggio esplicito se non arriva entro il timeout.
 */
export async function waitForEmail(
  to: string,
  opts: { timeoutMs?: number; subjectContains?: string } = {},
): Promise<MailpitMessage> {
  const timeoutMs = opts.timeoutMs ?? 30_000
  const deadline = Date.now() + timeoutMs
  const query = `to:"${to}"` + (opts.subjectContains ? ` subject:"${opts.subjectContains}"` : '')
  for (;;) {
    const { messages } = await getJson<{ messages: Array<{ ID: string }> }>(
      `/api/v1/search?query=${encodeURIComponent(query)}`,
    )
    if (messages.length > 0) return getJson<MailpitMessage>(`/api/v1/message/${messages[0].ID}`)
    if (Date.now() > deadline) {
      throw new Error(
        `email non ricevuta in Mailpit entro ${Math.round(timeoutMs / 1000)}s ` +
          `(destinatario: ${to}${opts.subjectContains ? `, oggetto contenente: "${opts.subjectContains}"` : ''}). ` +
          `Ispeziona la casella: ${API}`,
      )
    }
    await new Promise((r) => setTimeout(r, 500))
  }
}

/**
 * Estrae dal corpo TESTUALE il primo link il cui percorso contiene `pathContains`
 * (es. '/verify'). La versione testuale è — per contratto dei template (UC 0018) —
 * quella che tiene i link leggibili ovunque.
 */
export function extractLink(message: MailpitMessage, pathContains: string): string {
  const links = message.Text.match(/https?:\/\/[^\s"'<>]+/g) ?? []
  const link = links.find((l) => l.includes(pathContains))
  if (!link) {
    throw new Error(
      `nessun link contenente "${pathContains}" nell'email "${message.Subject}" — link trovati: ${links.join(', ') || 'nessuno'}`,
    )
  }
  return link
}
