# Step 02 — Gate abbonati (escalation)

Prima di proporre qualunque atto distruttivo, gli **abbonati vengono trattati**. Nessuna distruzione è
lecita finché le subscription attive non sono gestite (#09 H35). Questa decisione è **denaro ed effetto
verso l'esterno**: si **escala allo sviluppatore** — anche in autopilot — non si decide da soli.

## Fotografa le subscription attive

Un'app ha "abbonati vivi" se esistono subscription in uno stato che dà accesso: `active`, `trialing`,
`past_due` (è la stessa regola di `SubscriptionStatus.grantsAccess()` e di
`PricingSyncService.hasActiveSubscription`). Verifica quante e di quali tenant per l'app che si dismette
(read-model subscription del core, `SubscriptionRepository.findByApp(appId)`), e presenta il quadro:
quanti abbonati, su quali piani, con quale fine periodo.

## Proponi il trattamento — poi fermati e chiedi

Presenta, in prosa, le opzioni con la tua raccomandazione, **una alla volta**, e **aspetta la decisione
dello sviluppatore**:

- **Disdetta a fine periodo** — `PaymentProvider.cancelSubscription`: l'abbonato resta sul piano fino a
  `current_period_end`, poi l'accesso cessa. È il trattamento più rispettoso quando l'app chiude e non c'è
  un sostituto. Nessun rimborso implicito: quello è una decisione a parte, dello sviluppatore.
- **Migrazione a un'altra app/piano** — `PaymentProvider.changeSubscriptionTier`: quando esiste un
  sostituto e ha senso spostare gli abbonati. Attenzione al gate di giacenza sul downgrade.
- **Sola comunicazione** — quando non ci sono abbonati vivi (solo storici) o la decisione commerciale è
  già presa altrove: si comunica la dismissione e la tempistica.

La skill **non esegue** queste operazioni: riusa i meccanismi esistenti e li mette nel **runbook**
(step-04). Qui si decide *cosa* fare, non lo si fa.

## Se non ci sono abbonati vivi

Se nessuna subscription dà accesso, dillo esplicitamente: il gate è soddisfatto, restano al più le
comunicazioni agli abbonati storici. Registra comunque la verifica in `decisions.json`.

## Registra la decisione

La scelta di trattamento (e la sua motivazione) va in `changes/NNNN-*/decisions.json` appena presa, come
per ogni decisione della change. In autopilot, se hai dovuto proporre e attendere, registra sia la
raccomandazione sia la risposta dello sviluppatore.

Prosegui con `step-03-data.md`.
