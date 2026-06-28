import type {
  CheckoutOpenOptions,
  PaddleClient,
  PaddleSetupOptions,
} from './types'

/** ID transazione plausibile (stile Paddle), deterministicamente unico. */
function fakeTransactionId(): string {
  const uuid =
    typeof globalThis.crypto?.randomUUID === 'function'
      ? globalThis.crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`
  return `txn_${uuid.replace(/-/g, '').slice(0, 24)}`
}

/**
 * Fake Paddle.js (UC 0023): implementa la stessa `PaddleClient` dell'overlay reale, ma invece di
 * aprire l'iframe Paddle emette **sinteticamente** `checkout.completed` (asincrono, come l'originale).
 * Nessuna rete, nessun dato di pagamento: l'attivazione reale resta governata dal webhook (#09 C16),
 * questo evento serve solo alla UX. La schermata di checkout che lo usa è UC 0024.
 */
export function createStubPaddle(options: PaddleSetupOptions = {}): PaddleClient {
  return {
    Checkout: {
      open(open: CheckoutOpenOptions): void {
        const event = {
          name: 'checkout.completed' as const,
          data: {
            transactionId: fakeTransactionId(),
            customData: open.customData,
          },
        }
        // Asincrono come Paddle.js reale (l'overlay non chiama il callback in modo sincrono).
        queueMicrotask(() => options.eventCallback?.(event))
      },
    },
  }
}
