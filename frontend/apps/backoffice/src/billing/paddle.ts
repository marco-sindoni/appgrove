import { createStubPaddle } from '@appgrove/paddle-stub'
import type { PaddleClient, PaddleSetupOptions } from '@appgrove/paddle-stub'

/**
 * Factory del client Paddle.js (UC 0024). In **locale/dev** si usa il **Fake Paddle.js** (UC 0023):
 * stessa `PaddleClient`, nessuna rete, emette `checkout.completed` sintetico (solo UX). Il loader del
 * **vero** Paddle.js (script remoto + client-token) è gated #14 → differito a UC 0029 (smoke L3): vedi i
 * "Punti aperti" di UC 0024. La SPA parla sempre con la stessa forma, così il resto del codice non cambia.
 */
export function createPaddle(options: PaddleSetupOptions): PaddleClient {
  return createStubPaddle(options)
}
