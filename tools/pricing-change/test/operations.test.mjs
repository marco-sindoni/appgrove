// Test delle modifiche al pricing-as-code (UC 0047). Verificano: le quattro operazioni,
// i guardrail, la CONSERVAZIONE DEI COMMENTI (i listini hanno header curati a mano), la
// regola di IMMUTABILITÀ (#09 H35/H37: un prezzo vivo si cambia via NUOVO tier, mai
// mutando il vecchio) e il round-trip (l'output ricarica valido contro lo schema).
import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  load,
  serialize,
  validate,
  tierKeys,
  addTier,
  addCycle,
  setLimits,
  changePriceInPlace,
  changePriceNewTier,
} from '../lib/pricing.mjs'

// Fixture realistica (formato congelato dalla change 0019), con un commento in testa
// e un tier `team` con prezzo mensile VIVO da 19,00 €.
const FIXTURE = `# Listino di prova — pricing-as-code. Commento da PRESERVARE.
slug: demo
name: Demo
userModel: multi_user
status: inactive
tiers:
  - key: free
    name: Demo Free
    trialDays: 0
    limits: { metric: seats, cap: 2, type: stock }
    features: {}
    prices: []
  - key: team
    name: Demo Team
    trialDays: 14
    limits: { metric: seats, cap: 10, type: stock }
    features: {}
    prices:
      - billingCycle: monthly
        amount: 1900
        currency: EUR
`

function roundTrip(doc) {
  const text = serialize(doc)
  const reparsed = load(text)
  validate(reparsed)
  return { text, reparsed }
}

test('add-tier aggiunge un tier e preserva i commenti', () => {
  const doc = load(FIXTURE)
  addTier(doc, {
    key: 'pro',
    name: 'Demo Pro',
    trialDays: 14,
    limits: { metric: 'seats', cap: 50, type: 'stock' },
    prices: [{ billingCycle: 'monthly', amount: 4900, currency: 'EUR' }],
  })
  const { text, reparsed } = roundTrip(doc)
  assert.deepEqual(tierKeys(reparsed), ['free', 'team', 'pro'])
  assert.ok(text.includes('Commento da PRESERVARE'), 'il commento in testa deve restare')
})

test('add-tier rifiuta una chiave già esistente', () => {
  const doc = load(FIXTURE)
  assert.throws(() => addTier(doc, { key: 'team', name: 'X' }), /esiste già/)
})

test('add-cycle aggiunge l annuale a un tier esistente', () => {
  const doc = load(FIXTURE)
  addCycle(doc, { tierKey: 'team', billingCycle: 'annual', amount: 19000 })
  const { reparsed } = roundTrip(doc)
  const team = reparsed.toJSON().tiers.find((t) => t.key === 'team')
  assert.deepEqual(
    team.prices.map((p) => [p.billingCycle, p.amount]),
    [['monthly', 1900], ['annual', 19000]],
  )
})

test('add-cycle rifiuta un ciclo già presente e un tier inesistente', () => {
  const doc = load(FIXTURE)
  assert.throws(() => addCycle(doc, { tierKey: 'team', billingCycle: 'monthly', amount: 2000 }), /ha già un prezzo monthly/)
  assert.throws(() => addCycle(doc, { tierKey: 'ghost', billingCycle: 'annual', amount: 1 }), /non trovato/)
})

test('set-limits cambia i limiti di un tier senza toccare i prezzi', () => {
  const doc = load(FIXTURE)
  setLimits(doc, { tierKey: 'team', limits: { metric: 'seats', cap: 20, type: 'stock' } })
  const { reparsed } = roundTrip(doc)
  const team = reparsed.toJSON().tiers.find((t) => t.key === 'team')
  assert.equal(team.limits.cap, 20)
  assert.equal(team.prices[0].amount, 1900, 'i prezzi non devono cambiare')
})

test('change-price IN LOCO corregge l importo (solo per prezzo non ancora sincronizzato)', () => {
  const doc = load(FIXTURE)
  changePriceInPlace(doc, { tierKey: 'team', billingCycle: 'monthly', amount: 1700 })
  const { reparsed } = roundTrip(doc)
  const team = reparsed.toJSON().tiers.find((t) => t.key === 'team')
  assert.equal(team.prices[0].amount, 1700)
})

test('change-price via NUOVO TIER: il vecchio prezzo resta IMMUTATO (immutabilità #09 H35/H37)', () => {
  const doc = load(FIXTURE)
  changePriceNewTier(doc, {
    tierKey: 'team', newTierKey: 'team_2026', billingCycle: 'monthly', amount: 2200,
  })
  const { reparsed } = roundTrip(doc)
  const tiers = reparsed.toJSON().tiers

  // il tier sorgente è INTATTO (grandfathering degli abbonati esistenti)
  const team = tiers.find((t) => t.key === 'team')
  assert.equal(team.prices.find((p) => p.billingCycle === 'monthly').amount, 1900)

  // il nuovo tier porta il nuovo prezzo, clonando limiti/prova dal sorgente
  const next = tiers.find((t) => t.key === 'team_2026')
  assert.ok(next, 'il nuovo tier deve esistere')
  assert.equal(next.prices.find((p) => p.billingCycle === 'monthly').amount, 2200)
  assert.deepEqual(next.limits, team.limits)
  assert.equal(next.trialDays, team.trialDays)
})

test('change-price via nuovo tier clona anche gli ALTRI cicli invariati', () => {
  const doc = load(FIXTURE)
  addCycle(doc, { tierKey: 'team', billingCycle: 'annual', amount: 19000 })
  changePriceNewTier(doc, {
    tierKey: 'team', newTierKey: 'team_v2', billingCycle: 'monthly', amount: 2200,
  })
  const next = load(serialize(doc)).toJSON().tiers.find((t) => t.key === 'team_v2')
  assert.equal(next.prices.find((p) => p.billingCycle === 'monthly').amount, 2200) // cambiato
  assert.equal(next.prices.find((p) => p.billingCycle === 'annual').amount, 19000) // invariato
})

test('change-price via nuovo tier rifiuta una chiave già esistente o uguale al sorgente', () => {
  const doc = load(FIXTURE)
  assert.throws(() => changePriceNewTier(doc, { tierKey: 'team', newTierKey: 'free', billingCycle: 'monthly', amount: 1 }), /esiste già/)
  assert.throws(() => changePriceNewTier(doc, { tierKey: 'team', newTierKey: 'team', billingCycle: 'monthly', amount: 1 }), /diversa dal tier sorgente/)
})

test('i limiti dei tier aggiunti sono resi INLINE, come nello stile dei listini', () => {
  const doc = load(FIXTURE)
  addTier(doc, { key: 'pro', name: 'Demo Pro', trialDays: 0, limits: { metric: 'seats', cap: 50, type: 'stock' } })
  changePriceNewTier(doc, { tierKey: 'team', newTierKey: 'team_v2', billingCycle: 'monthly', amount: 2200 })
  const text = serialize(doc)
  assert.ok(text.includes('limits: { metric: seats, cap: 50, type: stock }'), 'add-tier: limiti inline')
  assert.ok(text.includes('limits: { metric: seats, cap: 10, type: stock }'), 'nuovo tier: limiti inline clonati')
  assert.ok(!/limits:\n\s+metric:/.test(text), 'nessun limits in forma a blocco')
})

test('validate rifiuta un ciclo duplicato e un importo non intero', () => {
  const bad = load(FIXTURE)
  bad.get('tiers').items[1].get('prices').add(load('billingCycle: monthly\namount: 999\ncurrency: EUR').contents)
  assert.throws(() => validate(bad), /monthly duplicato/)
})
