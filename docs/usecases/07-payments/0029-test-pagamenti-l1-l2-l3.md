# UC 0029 — Test pagamenti L1/L2/L3

**Area**: 07-payments · **Fase**: 5 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0024](0024-checkout.md) (checkout), UC [0025](0025-pipeline-webhook.md) (webhook)
**Fonte decisioni**: #09 D20 (strategia 3 livelli), #10 L (testing), #07 (gate prod)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [09-pagamenti](../../09-pagamenti.md), [10-testing](../../10-testing.md), [07-devops-cicd](../../07-devops-cicd.md)

## 1. Obiettivo / Scope
Implementare la **strategia di test dei pagamenti a 3 livelli**, principio: **non si guida l'iframe Paddle con Playwright** (si
mocka il confine, si testa a fondo il nostro codice).
**Incluso**: **L1** integration esaustivo del processing webhook (payload sintetici firmati, Testcontainers, **per-PR bloccante**);
**L2** E2E Playwright dei nostri pezzi con **Paddle.js mockato** (**per-PR bloccante**); **L3** smoke reale su **Paddle Sandbox**
(**pre-release**, override manuale se sandbox down).
**Escluso**: l'implementazione checkout/webhook/lifecycle (UC 0024-0038), lo stub in sé (UC 0023 — abilita L1/L2 offline).

## 2. Attori & ruoli
- **CI** (UC 0005): esegue L1/L2 per-PR (bloccanti); L3 nel flusso di promozione a prod.
- **Approver prod**: l'esito L3 confluisce nel gate di approvazione manuale (può forzare con motivazione).

## 3. Precondizioni
- Stub Paddle locale (UC 0023) per L1/L2 offline; account **sandbox** attivo per L3 (richiede #14 + account Paddle, UC 0001).

## 4. Flusso principale
1. **L1 (per-PR, bloccante)** 🔑: payload webhook **sintetici firmati** → evoluzione `subscription`; copre firma valida/errata, idempotenza, out-of-order, ogni evento, linkage tenant (custom_data), derivazione entitlement, enforcement quota; Testcontainers Postgres; deterministico (#09 D20 L1).
2. **L2 (per-PR, bloccante)**: E2E Playwright con **Paddle.js mockato** — scelta tier (default annuale+sconto), click acquisto → server-initiated, **UX polling** simulando l'arrivo webhook ("attivazione"→"attivato") (#09 D20 L2).
3. **L3 (pre-release)**: smoke E2E reale contro **Paddle Sandbox** (vero Paddle.js + carte test + vero webhook) → valida il contratto reale; eseguito nel flusso tag→prod, **non** per-PR (esterno/lento/flaky) (#09 D20 L3).
4. L'esito **L3 confluisce nel gate di approvazione manuale** prod (#07 b1); fallimento **ferma la release** salvo override.

## 5. Flussi alternativi / edge / errori
- **Sandbox down** (terzo): **override manuale obbligatorio** — chi approva può forzare con **motivazione registrata** (audit, es. "sandbox Paddle down") (#09 D20 L3).
- **Locale**: solo stub o sandbox opt-in, **mai pagamenti reali** (#09 D20).
- **Non si guida l'iframe Paddle**: il confine è mockato (L2) o reale-ma-smoke (L3) (#10 L).

## 6. Risorse & runbook  _(testing)_
**L1/L2**: nel repo, girano in CI per-PR (UC 0005) e in locale (stub UC 0023). **L3**: job nella pipeline di release (tag→prod),
esito nel gate. **Runbook**: PR → L1+L2 verdi obbligatori; release → L3 smoke → approvazione (override con motivazione se sandbox down).

## 7. Dati toccati
Dati di test sintetici (L1/L2 offline; L3 carte test sandbox). Nessun dato reale/pagamento reale. Manifest: N/A (test). L'audit
dell'override L3 è registrato (#08/#09 D20).

## 8. Permessi & gate
- **Invarianti**: L1 verifica linkage tenant + isolamento; entitlement derivato corretto.
- **Gate**: L1/L2 **bloccanti** per-PR (#10 35); L3 nel gate prod con override motivato.

## 9. Requisiti di test
È il test stesso. DEVE risultare: L1 esaustivo verde (firma/idempotenza/out-of-order/eventi/linkage/entitlement/quota); L2 UX
verde con Paddle.js mockato; L3 smoke verde (o override motivato se sandbox down).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #09 D20 (L1/L2/L3), #10 L, #07 b1/H.
- **DoD**:
  1. L1 integration esaustivo (per-PR, bloccante) sul processing webhook.
  2. L2 E2E con Paddle.js mockato (per-PR, bloccante) sulla UX checkout/polling.
  3. L3 smoke reale su sandbox (pre-release) con override manuale motivato.
  4. Mai pagamenti reali in locale; non si guida l'iframe Paddle.
