// Iscrizione PUBBLICA alla newsletter (UC 0039): endpoint core senza autenticazione.
// Non passa dall'api-client generato (che aggancia il Bearer): al signup l'utente non è ancora
// autenticato. Best-effort — un errore qui non deve mai bloccare la registrazione. Il double opt-in
// (email di conferma) lo gestisce il backend; la SPA non attende la conferma.

/** `POST /api/platform/v1/newsletter/subscriptions` con consenso esplicito e campo esca vuoto. */
export async function subscribeNewsletter(
  coreBaseUrl: string,
  body: { email: string; locale?: string; channel: 'site' | 'signup' },
): Promise<void> {
  await fetch(`${coreBaseUrl}/api/platform/v1/newsletter/subscriptions`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    // consent: la checkbox spuntata È il consenso; website: campo esca ("honeypot") lasciato vuoto.
    body: JSON.stringify({ email: body.email, locale: body.locale, channel: body.channel, consent: true, website: '' }),
  })
}
