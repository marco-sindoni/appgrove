// Test del reporter globale errori (UC 0006, #08/23). Deterministici: fetch mockato,
// handler invocati direttamente (niente eventi asincroni del browser).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  installErrorReporter,
  MAX_MESSAGE_LENGTH,
  MAX_REPORTS_PER_SESSION,
  MAX_STACK_LENGTH,
  type ErrorPayload,
} from './reporter'

const ENDPOINT = 'https://api.test.appgrove.app/ingest/errors'

let fetchMock: ReturnType<typeof vi.fn>
let uninstall: (() => void) | null = null

beforeEach(() => {
  fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 202 }))
  vi.stubGlobal('fetch', fetchMock)
  window.onerror = null
  window.onunhandledrejection = null
})

afterEach(() => {
  uninstall?.()
  uninstall = null
  vi.unstubAllGlobals()
})

/** Scatena un errore sintetico attraverso l'handler registrato. */
function fireError(message: string, error?: Error, source = 'app.js', line = 10, col = 5): void {
  expect(window.onerror).toBeTypeOf('function')
  window.onerror?.call(window, message, source, line, col, error)
}

/** Scatena una promise rejection sintetica attraverso l'handler registrato. */
function fireRejection(reason: unknown): void {
  expect(window.onunhandledrejection).toBeTypeOf('function')
  window.onunhandledrejection?.call(window, { reason } as PromiseRejectionEvent)
}

/** Estrae il payload JSON dell'n-esima chiamata a fetch. */
function sentPayload(call = 0): ErrorPayload {
  const [, init] = fetchMock.mock.calls[call] as [string, RequestInit]
  return JSON.parse(init.body as string) as ErrorPayload
}

describe('installErrorReporter — NO-OP senza endpoint', () => {
  it('non registra handler né invia nulla con endpoint assente', () => {
    uninstall = installErrorReporter({ appId: 'backoffice' })
    expect(window.onerror).toBeNull()
    expect(window.onunhandledrejection).toBeNull()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('non registra handler con endpoint vuoto o di soli spazi', () => {
    uninstall = installErrorReporter({ appId: 'backoffice', endpoint: '  ' })
    expect(window.onerror).toBeNull()
    expect(window.onunhandledrejection).toBeNull()
  })
})

describe('installErrorReporter — registrazione handler', () => {
  it('registra window.onerror e window.onunhandledrejection', () => {
    uninstall = installErrorReporter({ appId: 'backoffice', endpoint: ENDPOINT })
    expect(window.onerror).toBeTypeOf('function')
    expect(window.onunhandledrejection).toBeTypeOf('function')
  })

  it('la disinstallazione ripristina gli handler precedenti', () => {
    const prev = vi.fn().mockReturnValue(false)
    window.onerror = prev
    const off = installErrorReporter({ appId: 'backoffice', endpoint: ENDPOINT })
    expect(window.onerror).not.toBe(prev)
    off()
    expect(window.onerror).toBe(prev)
  })

  it('incatena un eventuale handler preesistente e ritorna false (default console preservato)', () => {
    const prev = vi.fn()
    window.onerror = prev
    uninstall = installErrorReporter({ appId: 'backoffice', endpoint: ENDPOINT })
    const result = window.onerror?.call(window, 'boom', 'app.js', 1, 1, new Error('boom'))
    expect(result).toBe(false)
    expect(prev).toHaveBeenCalledOnce()
  })
})

describe('installErrorReporter — payload', () => {
  it('invia il payload atteso con fetch keepalive fire-and-forget', () => {
    uninstall = installErrorReporter({
      appId: 'backoffice',
      endpoint: ENDPOINT,
      buildSha: 'abc1234',
    })
    const err = new Error('esplosione')
    fireError('esplosione', err, 'https://cdn/app.js', 42, 7)

    expect(fetchMock).toHaveBeenCalledOnce()
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toBe(ENDPOINT)
    expect(init.method).toBe('POST')
    expect(init.keepalive).toBe(true)
    // "richiesta semplice" CORS (niente preflight): text/plain, il body resta la stringa JSON
    // (CORS sull'API Gateway condiviso non configurato — punto differito di UC 0004)
    expect(init.headers).toEqual({ 'Content-Type': 'text/plain;charset=UTF-8' })

    const payload = sentPayload()
    expect(payload.app_id).toBe('backoffice')
    expect(payload.route).toBe(window.location.pathname)
    expect(payload.build_sha).toBe('abc1234')
    expect(payload.message).toBe('esplosione')
    expect(payload.stack).toBe(err.stack)
    expect(payload.source).toBe('https://cdn/app.js')
    expect(payload.line).toBe(42)
    expect(payload.col).toBe(7)
    expect(payload.ts).toMatch(/^\d{4}-\d{2}-\d{2}T/)
    // senza getContext: niente id utente/tenant nel payload
    expect(payload.user_id).toBeUndefined()
    expect(payload.tenant_id).toBeUndefined()
  })

  it('buildSha assente → "dev"', () => {
    uninstall = installErrorReporter({ appId: 'admin', endpoint: ENDPOINT })
    fireError('x', new Error('x'))
    expect(sentPayload().build_sha).toBe('dev')
  })

  it('allega user_id/tenant_id opachi dal getContext quando la sessione esiste', () => {
    uninstall = installErrorReporter({
      appId: 'admin',
      endpoint: ENDPOINT,
      getContext: () => ({ userId: 'u-123', tenantId: 't-456' }),
    })
    fireError('x', new Error('x'))
    const payload = sentPayload()
    expect(payload.user_id).toBe('u-123')
    expect(payload.tenant_id).toBe('t-456')
  })

  it('un getContext difettoso non impedisce l invio', () => {
    uninstall = installErrorReporter({
      appId: 'admin',
      endpoint: ENDPOINT,
      getContext: () => {
        throw new Error('store non pronto')
      },
    })
    fireError('x', new Error('x'))
    expect(fetchMock).toHaveBeenCalledOnce()
    expect(sentPayload().user_id).toBeUndefined()
  })

  it('tronca lo stack a ~8KB', () => {
    uninstall = installErrorReporter({ appId: 'backoffice', endpoint: ENDPOINT })
    const err = new Error('gigante')
    err.stack = 'x'.repeat(MAX_STACK_LENGTH + 1000)
    fireError('gigante', err)
    expect(sentPayload().stack).toHaveLength(MAX_STACK_LENGTH)
  })

  it('un fetch che lancia in modo sincrono non propaga dal handler', () => {
    fetchMock.mockImplementation(() => {
      throw new Error('rete assente')
    })
    uninstall = installErrorReporter({ appId: 'backoffice', endpoint: ENDPOINT })
    expect(() => fireError('x', new Error('x'))).not.toThrow()
  })

  it('un fetch che fallisce in modo asincrono (es. CORS) viene ingoiato in silenzio', async () => {
    // la risposta non viene mai letta: fire-and-forget, anche il reject resta gestito
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))
    const onRejection = vi.fn()
    process.on('unhandledRejection', onRejection)
    try {
      uninstall = installErrorReporter({ appId: 'backoffice', endpoint: ENDPOINT })
      fireError('x', new Error('x'))
      // lascia girare la microtask queue perché un eventuale reject non gestito emerga
      await new Promise((resolve) => setTimeout(resolve, 0))
      expect(onRejection).not.toHaveBeenCalled()
    } finally {
      process.off('unhandledRejection', onRejection)
    }
  })
})

  it('tronca il message a ~4KB (il body non deve superare il limite della Lambda)', () => {
    uninstall = installErrorReporter({ appId: 'backoffice', endpoint: ENDPOINT })
    fireError('x'.repeat(MAX_MESSAGE_LENGTH * 3))
    expect(sentPayload().message).toHaveLength(MAX_MESSAGE_LENGTH)
  })

describe('installErrorReporter — anti-flood', () => {
  it('dedupe: lo stesso errore (message+stack) parte una sola volta per sessione', () => {
    uninstall = installErrorReporter({ appId: 'backoffice', endpoint: ENDPOINT })
    const err = new Error('ripetuto')
    fireError('ripetuto', err)
    fireError('ripetuto', err)
    fireError('ripetuto', err)
    expect(fetchMock).toHaveBeenCalledOnce()
  })

  it('tetto: oltre il massimo per sessione gli errori distinti vengono scartati', () => {
    uninstall = installErrorReporter({ appId: 'backoffice', endpoint: ENDPOINT })
    for (let i = 0; i < MAX_REPORTS_PER_SESSION + 5; i++) {
      const err = new Error(`errore-${i}`)
      err.stack = `stack-${i}`
      fireError(`errore-${i}`, err)
    }
    expect(fetchMock).toHaveBeenCalledTimes(MAX_REPORTS_PER_SESSION)
  })
})

describe('installErrorReporter — unhandledrejection', () => {
  it('invia message+stack per una rejection con Error', () => {
    uninstall = installErrorReporter({ appId: 'admin', endpoint: ENDPOINT })
    const reason = new Error('promessa fallita')
    fireRejection(reason)
    expect(fetchMock).toHaveBeenCalledOnce()
    const payload = sentPayload()
    expect(payload.message).toBe('promessa fallita')
    expect(payload.stack).toBe(reason.stack)
    expect(payload.app_id).toBe('admin')
  })

  it('una reason con toString ostile non fa lanciare il handler e viene comunque riportata', () => {
    uninstall = installErrorReporter({ appId: 'backoffice', endpoint: ENDPOINT })
    const ostile = {
      toString(): string {
        throw new Error('toString che lancia')
      },
    }
    expect(() => fireRejection(ostile)).not.toThrow()
    expect(sentPayload().message).toContain('non serializzabile')
  })

  it('gestisce una rejection con reason non-Error', () => {
    uninstall = installErrorReporter({ appId: 'admin', endpoint: ENDPOINT })
    fireRejection('stringa qualsiasi')
    expect(sentPayload().message).toContain('stringa qualsiasi')
    expect(sentPayload().stack).toBeUndefined()
  })

  it('incatena un eventuale handler preesistente di rejection', () => {
    const prev = vi.fn()
    window.onunhandledrejection = prev
    uninstall = installErrorReporter({ appId: 'admin', endpoint: ENDPOINT })
    fireRejection(new Error('x'))
    expect(prev).toHaveBeenCalledOnce()
  })
})
