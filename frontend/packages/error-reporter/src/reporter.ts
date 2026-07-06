// Reporter globale errori JS per le SPA appgrove (UC 0006, decisione #08/23).
//
// Cattura SOLO gli errori non gestiti (`window.onerror` + `window.onunhandledrejection`) e li
// invia all'ingest della piattaforma. Nessun tracking comportamentale: base giuridica
// "legittimo interesse" (#13). `user_id`/`tenant_id` sono identificatori opachi di contesto,
// trattati dal backend come NON fidati (solo log, mai usati per autorizzare — invariante #1).
//
// Proprietà chiave:
// - NO-OP senza endpoint (sviluppo locale/e2e: nessun ingest configurato, nessun handler registrato);
// - fire-and-forget (`fetch` con `keepalive`), non lancia MAI dal handler, non logga il payload;
// - anti-flood: dedupe per hash(message+stack) una volta per sessione di pagina + tetto massimo
//   di invii per sessione, oltre il quale si scarta;
// - non "ingoia" gli errori: gli handler ritornano false/undefined, quindi il comportamento di
//   default del browser (log in console) resta intatto.

/** Contesto opzionale allegato al payload quando esiste una sessione. */
export interface ErrorReporterContext {
  /** Identificatore opaco dell'utente (claim `sub`). */
  userId?: string
  /** Identificatore opaco del tenant (claim `tenant_id`). */
  tenantId?: string
}

export interface ErrorReporterOptions {
  /** Identificativo dell'app che invia (es. "backoffice", "admin"). Obbligatorio. */
  appId: string
  /** URL dell'ingest errori. Assente/vuoto → il reporter è un NO-OP (dev locale senza ingest). */
  endpoint?: string | null
  /** SHA di build per de-minificare offline (#08/24). Lo risolve l'app (VITE_BUILD_SHA); default "dev". */
  buildSha?: string
  /** Callback opzionale per allegare gli id opachi di sessione quando disponibili. */
  getContext?: () => ErrorReporterContext
}

/** Payload inviato all'ingest (snake_case, come atteso dal backend di piattaforma). */
export interface ErrorPayload {
  app_id: string
  /** Rotta corrente (solo pathname: niente query string, che può contenere dati personali). */
  route: string
  build_sha: string
  message: string
  /** Stack trace troncato a ~8KB per contenere il costo di ingest/log. */
  stack?: string
  source?: string
  line?: number
  col?: number
  user_id?: string
  tenant_id?: string
  /** Timestamp ISO 8601 dell'errore. */
  ts: string
}

/** Tetto massimo di invii per sessione di pagina (anti-flood, oltre si scarta). */
export const MAX_REPORTS_PER_SESSION = 10
/** Lunghezza massima dello stack trace inviato (~8KB). */
export const MAX_STACK_LENGTH = 8 * 1024
/**
 * Lunghezza massima del message inviato (~4KB): senza tetto, un messaggio enorme
 * porterebbe il body oltre il limite della Lambda di ingest (32KB), che scarta
 * l'intero evento in silenzio — perdendo proprio gli errori più verbosi.
 */
export const MAX_MESSAGE_LENGTH = 4 * 1024

/** Hash non crittografico (djb2) per il dedupe message+stack: serve solo come chiave di sessione. */
function dedupeKey(message: string, stack: string | undefined): string {
  const input = `${message}\n${stack ?? ''}`
  let h = 5381
  for (let i = 0; i < input.length; i++) {
    h = ((h << 5) + h + input.charCodeAt(i)) | 0
  }
  return h.toString(36)
}

/**
 * Registra il reporter globale. Ritorna una funzione di disinstallazione (ripristina gli handler
 * precedenti) — utile nei test; le app non hanno bisogno di chiamarla.
 */
export function installErrorReporter(options: ErrorReporterOptions): () => void {
  const endpoint = options.endpoint?.trim()
  // Senza endpoint il reporter è inerte: nessun handler registrato (dev locale / e2e).
  if (!endpoint) return () => {}

  const appId = options.appId
  const buildSha = options.buildSha ?? 'dev'
  const seen = new Set<string>()
  let sent = 0

  const report = (
    message: string,
    stack: string | undefined,
    source?: string,
    line?: number,
    col?: number,
  ): void => {
    // Il reporter non deve MAI lanciare a sua volta: qualunque problema interno viene scartato.
    try {
      const key = dedupeKey(message, stack)
      if (seen.has(key)) return // già inviato in questa sessione di pagina
      if (sent >= MAX_REPORTS_PER_SESSION) return // tetto anti-flood raggiunto: si scarta
      seen.add(key)
      sent++

      let context: ErrorReporterContext = {}
      try {
        context = options.getContext?.() ?? {}
      } catch {
        // il contesto è best-effort: un errore nel callback non blocca l'invio
      }

      const payload: ErrorPayload = {
        app_id: appId,
        route: window.location.pathname,
        build_sha: buildSha,
        message: message.slice(0, MAX_MESSAGE_LENGTH),
        ...(stack ? { stack: stack.slice(0, MAX_STACK_LENGTH) } : {}),
        ...(source ? { source } : {}),
        ...(line !== undefined ? { line } : {}),
        ...(col !== undefined ? { col } : {}),
        ...(context.userId ? { user_id: context.userId } : {}),
        ...(context.tenantId ? { tenant_id: context.tenantId } : {}),
        ts: new Date().toISOString(),
      }

      // Fire-and-forget: `keepalive` fa sopravvivere la richiesta anche a navigazione/chiusura tab.
      // Il payload non viene mai loggato in console.
      //
      // ATTENZIONE Content-Type: l'ingest vive sul dominio API condiviso (api.<env>.appgrove.app),
      // che è cross-origin rispetto alle SPA, e il CORS sull'API Gateway condiviso NON è ancora
      // configurato (punto aperto differito di UC 0004). Per evitare il preflight si invia una
      // "richiesta semplice" CORS: `text/plain` invece di `application/json` (il body resta la
      // stringa JSON; la Lambda di ingest lo interpreta come JSON a prescindere dal Content-Type).
      // La risposta non va MAI letta: senza header CORS la lettura fallirebbe — atteso e innocuo.
      void fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain;charset=UTF-8' },
        body: JSON.stringify(payload),
        keepalive: true,
      }).catch(() => {
        // esito ignorato (anche i fallimenti CORS): l'osservabilità non deve mai degradare l'app
      })
    } catch {
      // mai propagare errori del reporter
    }
  }

  const prevOnError = window.onerror
  const prevOnUnhandledRejection = window.onunhandledrejection

  window.onerror = function (message, source, line, col, error) {
    const text =
      typeof message === 'string' && message
        ? message
        : (error?.message ?? 'Errore sconosciuto')
    report(
      text,
      error?.stack,
      source ?? undefined,
      typeof line === 'number' ? line : undefined,
      typeof col === 'number' ? col : undefined,
    )
    // Si preserva un eventuale handler preesistente (catena), ignorandone l'esito.
    try {
      prevOnError?.call(window, message, source, line, col, error)
    } catch {
      // un handler precedente difettoso non deve rompere il reporter
    }
    // false → il browser mantiene il comportamento di default (errore in console).
    return false
  }

  window.onunhandledrejection = function (event: PromiseRejectionEvent) {
    const reason: unknown = event.reason
    let message: string
    if (reason instanceof Error) {
      message = reason.message
    } else {
      // String(reason) può lanciare (toString ostile/Proxy): il reporter non deve
      // MAI propagare un proprio errore dall'handler globale.
      let text = '(reason non serializzabile)'
      try {
        text = String(reason)
      } catch {
        // si tiene il segnaposto
      }
      message = `Promise rifiutata senza gestione: ${text}`
    }
    const stack = reason instanceof Error ? reason.stack : undefined
    report(message, stack)
    try {
      prevOnUnhandledRejection?.call(window, event)
    } catch {
      // un handler precedente difettoso non deve rompere il reporter
    }
    // undefined → nessun preventDefault: il browser logga comunque la rejection in console.
  }

  return () => {
    window.onerror = prevOnError
    window.onunhandledrejection = prevOnUnhandledRejection
  }
}
