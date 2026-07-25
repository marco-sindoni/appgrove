// Test del calcolo della fee effettiva (#09 K46/K47). Riproduce gli esempi della
// decisione: €5/mese mensile → ~15% (avviso), €50/anno annuale → ~6% (niente avviso).
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { computeFee, formatEur, WARN_THRESHOLD_PCT } from '../lib/fee.mjs'

test('€5/mese mensile → fee ~15%, avviso acceso (esempio #09 K46)', () => {
  const r = computeFee({ amountMinor: 500 })
  assert.equal(r.pctMinor, 25) // 5% di €5,00
  assert.equal(r.fixedMinor, 50) // $0,50 ≈ €0,50
  assert.equal(r.feeMinor, 75)
  assert.equal(r.netMinor, 425)
  assert.equal(r.effectivePct, 15)
  assert.equal(r.warn, true)
})

test('€50/anno annuale → fee ~6%, nessun avviso (esempio #09 K46)', () => {
  const r = computeFee({ amountMinor: 5000 })
  assert.equal(r.feeMinor, 300) // €2,50 + €0,50
  assert.equal(r.netMinor, 4700)
  assert.equal(r.effectivePct, 6)
  assert.equal(r.warn, false)
})

test('la soglia dell avviso è esattamente 10% (soft, non blocco)', () => {
  // importo per cui la fee effettiva è ~10%: 5% + €0,50/importo = 10% → importo = €10,00
  const r = computeFee({ amountMinor: 1000 })
  assert.equal(r.effectivePct, 10)
  assert.equal(r.warn, false) // strettamente > soglia → 10% esatto non accende
  assert.equal(WARN_THRESHOLD_PCT, 10)

  const justAbove = computeFee({ amountMinor: 999 })
  assert.ok(justAbove.effectivePct > 10)
  assert.equal(justAbove.warn, true)
})

test('l annuale abbatte la fee rispetto al mensile equivalente (spinta all annuale, K48/K49)', () => {
  const monthly = computeFee({ amountMinor: 1900 }) // €19,00/mese
  const annual = computeFee({ amountMinor: 19000 }) // €190,00/anno (10× il mensile)
  assert.ok(annual.effectivePct < monthly.effectivePct)
})

test('il tasso di cambio è sovrascrivibile e cambia la parte fissa', () => {
  const conservative = computeFee({ amountMinor: 500, fxUsdToEur: 1.0 })
  const cheaper = computeFee({ amountMinor: 500, fxUsdToEur: 0.9 })
  assert.equal(conservative.fixedMinor, 50)
  assert.equal(cheaper.fixedMinor, 45)
  assert.ok(cheaper.feeMinor < conservative.feeMinor)
})

test('importo non valido → errore esplicito', () => {
  assert.throws(() => computeFee({ amountMinor: 0 }), /importo non valido/)
  assert.throws(() => computeFee({ amountMinor: -100 }), /importo non valido/)
  assert.throws(() => computeFee({ amountMinor: 1.5 }), /importo non valido/)
})

test('formatEur formatta i centesimi in euro', () => {
  assert.equal(formatEur(1900), '€19,00')
  assert.equal(formatEur(500), '€5,00')
  assert.equal(formatEur(5), '€0,05')
})
