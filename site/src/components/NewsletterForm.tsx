// Island React del form newsletter (UC 0039). Prima chiamata runtime del sito
// vetrina verso il backend: POST all'endpoint PUBBLICO del core (senza
// autenticazione) che avvia il double opt-in. Il sito mostra solo lo stato del
// form; la mail di conferma la manda il backend. Postura privacy: consenso
// esplicito (checkbox NON pre-spuntata), campo esca anti-bot, risposte neutre.
//
// L'URL base del core arriva come prop dal frontmatter Astro (import.meta.env.
// PUBLIC_CORE_API_URL): le variabili PUBLIC_ sono le uniche esposte al browser.
// Se manca (misconfigurazione) il form resta inviabile solo dopo il consenso ma
// l'invio fallisce con messaggio d'errore — il sito compila e si renderizza
// comunque (nessun accesso a env a tempo di build oltre alla prop).

import { useId, useState } from 'react'

export interface NewsletterLabels {
  /** aria-label del <form> (titolo della sezione/colonna). */
  ariaLabel: string
  emailPlaceholder: string
  cta: string
  consentLabel: string
  success: string
  error: string
}

interface Props {
  /** Base URL del core, es. http://localhost:8080; vuota se non configurata. */
  apiUrl: string
  /** Lingua corrente del sito, inviata come `locale`. */
  lang: string
  labels: NewsletterLabels
  /** `footer` = variante compatta (padding ridotti); `section` = homepage. */
  variant?: 'section' | 'footer'
}

type Status = 'idle' | 'loading' | 'success' | 'error'

export default function NewsletterForm({ apiUrl, lang, labels, variant = 'section' }: Props) {
  const [email, setEmail] = useState('')
  const [consent, setConsent] = useState(false)
  // Campo esca ("honeypot"): invisibile agli umani, spesso riempito dai bot.
  const [website, setWebsite] = useState('')
  const [status, setStatus] = useState<Status>('idle')

  const emailId = useId()
  const consentId = useId()
  const websiteId = useId()
  const messageId = useId()

  const compact = variant === 'footer'
  const loading = status === 'loading'
  // Invio possibile solo con consenso dato, endpoint configurato e non in corso.
  const canSubmit = consent && apiUrl.length > 0 && !loading

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit) return
    setStatus('loading')
    try {
      const res = await fetch(`${apiUrl}/api/platform/v1/newsletter/subscriptions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, locale: lang, consent: true, channel: 'site', website }),
      })
      // 202 = accettata (risposta neutra: non rivela se già iscritto). Qualsiasi
      // altro esito (400/429/rete) → messaggio d'errore generico.
      if (res.status === 202) {
        setStatus('success')
        // Evento Plausible best-effort: non deve rompere nulla se assente.
        if (typeof window !== 'undefined' && (window as unknown as { plausible?: (e: string) => void }).plausible) {
          ;(window as unknown as { plausible: (e: string) => void }).plausible('Newsletter: Subscribe')
        }
      } else {
        setStatus('error')
      }
    } catch {
      setStatus('error')
    }
  }

  // Su successo il form si ritira e resta solo il messaggio di conferma.
  if (status === 'success') {
    return (
      <p
        role="status"
        aria-live="polite"
        className={`text-sm text-fg ${compact ? '' : 'mt-6 max-w-md'}`}
      >
        {labels.success}
      </p>
    )
  }

  return (
    <form
      onSubmit={handleSubmit}
      aria-label={labels.ariaLabel}
      className={`flex flex-col gap-3 ${compact ? '' : 'mt-6 max-w-md'}`}
    >
      {/* Campo esca: fuori schermo, fuori dall'albero di accessibilità e dal
          tab-order, autocomplete disattivato. Gli umani non lo vedono né lo
          compilano; i bot che riempiono ogni campo si smascherano da soli. */}
      <div
        aria-hidden="true"
        style={{ position: 'absolute', left: '-9999px', width: '1px', height: '1px', overflow: 'hidden' }}
      >
        <label htmlFor={websiteId}>Website</label>
        <input
          id={websiteId}
          type="text"
          name="website"
          tabIndex={-1}
          autoComplete="off"
          value={website}
          onChange={(e) => setWebsite(e.target.value)}
        />
      </div>

      <div className="flex flex-wrap gap-2">
        <label htmlFor={emailId} className="sr-only">
          {labels.emailPlaceholder}
        </label>
        <input
          id={emailId}
          type="email"
          name="email"
          required
          autoComplete="email"
          placeholder={labels.emailPlaceholder}
          value={email}
          disabled={loading}
          onChange={(e) => setEmail(e.target.value)}
          aria-describedby={status === 'error' ? messageId : undefined}
          className={`min-w-0 flex-1 rounded-md border border-line bg-surface px-3 text-sm ${compact ? 'py-1.5' : 'py-2'}`}
        />
        <button
          type="submit"
          disabled={!canSubmit}
          className={`rounded-md border border-line bg-surface-2 text-sm font-semibold text-fg hover:bg-surface disabled:cursor-not-allowed disabled:opacity-50 ${compact ? 'px-3 py-1.5' : 'px-4 py-2'}`}
        >
          {labels.cta}
        </button>
      </div>

      <label htmlFor={consentId} className="flex items-start gap-2 text-sm text-fg-muted">
        <input
          id={consentId}
          type="checkbox"
          name="consent"
          checked={consent}
          disabled={loading}
          onChange={(e) => setConsent(e.target.checked)}
          className="mt-0.5 h-4 w-4 shrink-0 rounded border-line"
        />
        <span>{labels.consentLabel}</span>
      </label>

      {status === 'error' && (
        <p id={messageId} role="status" aria-live="polite" className="text-sm text-danger">
          {labels.error}
        </p>
      )}
    </form>
  )
}
