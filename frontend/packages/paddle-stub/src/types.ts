// Interfaccia (port) del client Paddle.js usata dall'app, sottoinsieme dell'overlay reale (#09 C13).
// Il Fake Paddle.js (UC 0023) e il loader Paddle.js reale (UC 0024) implementano entrambi `PaddleClient`,
// così la SPA parla con la stessa forma in dev (stub) e in prod (reale).

/** Evento emesso a checkout completato. Serve SOLO alla UX: l'attivazione avviene via webhook (#09 C16). */
export interface CheckoutCompletedEvent {
  name: 'checkout.completed'
  data: {
    transactionId: string
    customData?: Record<string, unknown>
  }
}

export type PaddleEvent = CheckoutCompletedEvent

/** Opzioni di apertura dell'overlay. Il token è prodotto dal checkout server-initiated (#09 C14). */
export interface CheckoutOpenOptions {
  /** Token di checkout restituito dal backend (server-initiated); il client passa solo questo. */
  transactionToken: string
  /** custom_data impostati server-side (es. { tenant_id, app_id }); non manomettibili dal client. */
  customData?: Record<string, unknown>
}

export interface PaddleClient {
  Checkout: {
    open(options: CheckoutOpenOptions): void
  }
}

export interface PaddleSetupOptions {
  /** Callback degli eventi dell'overlay (la SPA reagisce a `checkout.completed` per la UX). */
  eventCallback?: (event: PaddleEvent) => void
}
