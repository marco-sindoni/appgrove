import { describe, it, expect, vi } from 'vitest'
import { createStubPaddle } from './stub'
import type { PaddleEvent } from './types'

describe('Fake Paddle.js (stub overlay, UC 0023)', () => {
  it('emette checkout.completed in modo asincrono dopo open()', async () => {
    const events: PaddleEvent[] = []
    const paddle = createStubPaddle({ eventCallback: (e) => events.push(e) })

    paddle.Checkout.open({ transactionToken: 'chk_test' })

    // come Paddle.js reale, il callback non è sincrono
    expect(events).toHaveLength(0)
    await Promise.resolve()
    expect(events).toHaveLength(1)
    expect(events[0].name).toBe('checkout.completed')
    expect(events[0].data.transactionId).toMatch(/^txn_/)
  })

  it('propaga i custom_data (tenant_id/app_id impostati server-side) nel payload', async () => {
    const callback = vi.fn()
    const paddle = createStubPaddle({ eventCallback: callback })
    const customData = { tenant_id: 'a0000000-0000-4000-8000-000000000001', app_id: 'fatture' }

    paddle.Checkout.open({ transactionToken: 'chk_test', customData })
    await Promise.resolve()

    expect(callback).toHaveBeenCalledTimes(1)
    const event = callback.mock.calls[0][0] as PaddleEvent
    expect(event.data.customData).toEqual(customData)
  })

  it('genera transactionId distinti per checkout diversi', async () => {
    const events: PaddleEvent[] = []
    const paddle = createStubPaddle({ eventCallback: (e) => events.push(e) })

    paddle.Checkout.open({ transactionToken: 'chk_1' })
    paddle.Checkout.open({ transactionToken: 'chk_2' })
    await Promise.resolve()

    expect(events).toHaveLength(2)
    expect(events[0].data.transactionId).not.toBe(events[1].data.transactionId)
  })

  it('non chiama nulla se non è fornito un eventCallback', () => {
    const paddle = createStubPaddle()
    expect(() => paddle.Checkout.open({ transactionToken: 'chk_test' })).not.toThrow()
  })
})
