// @appgrove/paddle-stub — Fake Paddle.js per dev/test (UC 0023).
// Stessa interfaccia dell'overlay reale (`PaddleClient`), emette `checkout.completed` sintetico.

export { createStubPaddle } from './stub'
export type {
  PaddleClient,
  PaddleEvent,
  PaddleSetupOptions,
  CheckoutOpenOptions,
  CheckoutCompletedEvent,
} from './types'
