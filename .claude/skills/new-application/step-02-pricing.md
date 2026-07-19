# Step 02 — Pricing and Quota Co-pilot

You are writing `services/core/src/main/resources/pricing/<app_id>.yaml` — the **pricing-as-code**
the generator left as a skeleton. Nothing here talks to the payment provider: the sync is the deploy
pipeline's job (UC 0022). You are writing a file, and the file is the truth.

This is a **dialogue**, not a form (SKILL.md "Questioning style"): one question at a time, in prose,
with your reasoning and a recommendation, then **STOP and wait**. The developer is not a pricing
analyst — that is precisely why this step exists.

## The one concept to explain before anything else

Do not use the words "flow" and "stock" without explaining them; they decide how the limit *behaves*
and they are the single most common source of a wrong plan:

- **Consumption over a window** — the counter resets every period. "200 invoices **per month**":
  in March you may issue 200 more regardless of February. Fits things the user *produces*.
- **Standing level** — no reset, it is a ceiling on what exists right now. "10 active projects",
  "5 seats": to add an eleventh you delete one. Fits things the user *keeps*.

Ask which one the metric is, with an example phrased in the app's own terms. Getting this wrong is
not a cosmetic mistake: a standing level counted as consumption lets the user accumulate without
limit, and a consumption metric counted as a level blocks the user forever once the ceiling is hit.

## Questions, in this order

1. **Nature of the metric** (above), and the window if it resets (usually the month).
2. **Tiers** — how many and what separates them. Recommend starting with **two or three**: a free
   baseline plus one or two paid. More tiers are easy to add later (skill `pricing-change`) and hard
   to remove once someone is on them.
3. **Free tier** — is there one, and what is the free ceiling? It exists to let people try the app,
   so it must be enough to see the value and not enough to live on. Propose a number and explain the
   reasoning rather than asking for one out of thin air.
4. **Prices** — monthly, and yearly with the customary ~17% discount (two months free). Yearly is
   worth pushing: it improves cash and, being a single larger transaction, carries **proportionally
   lower fees**.
5. **Trial** — default 14 days, can be disabled. Say plainly that a trial on a plan that already has
   a free tier is often redundant.

## Show the effective fee — and do not turn it into a veto

Once prices exist, compute the **effective fee**: the payment provider's cut plus the fixed
per-transaction part, as a percentage of the actual price. On low monthly prices the fixed part
dominates, and a €3/month plan can lose well over 10% to fees.

Report it plainly, and **warn above 10% without blocking** (#09 K47/48): it is a real signal, not an
error. The natural remedies are a higher price, or pushing the yearly plan — mention both. It stays
the developer's call.

## Write, then verify

Write the file, then show the developer the final shape and get an explicit confirmation before
moving on. Check the numbers actually round-trip:

```bash
( cd services && mvn -B -pl core -am test )   # the catalog tests read the pricing files
```

The tier limits you write here are the **same data** the running app enforces as quota: the cap in
`limits` is what the app's `QuotaLimitSource` resolves through the entitlement projection. A tier
with no limit for the metric means *unlimited*, not *zero* — say so out loud if the developer leaves
one blank, because the two readings are one typo apart.

Proceed to `step-03-personal-data.md`.
