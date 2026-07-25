# Step 03 — Grandfathering degli abbonati (escalation)

Obbligatorio quando hai **cambiato un prezzo** (step-02, via nuovo tier) su un tier che può avere abbonati.
Saltalo solo per un nuovo tier «a freddo», un cambio limiti o un ciclo aggiunto senza cambio prezzo.

Cosa succede agli abbonati esistenti è una decisione di **denaro ed effetto verso l'esterno**: si **escala
allo sviluppatore** — anche in autopilot — non si decide da soli. È la stessa regola del gate abbonati di
`drop-application`.

## Fotografa le subscription attive sul tier toccato

Un tier ha «abbonati vivi» se esistono subscription in uno stato che dà accesso: `active`, `trialing`,
`past_due` (la stessa regola di `SubscriptionStatus.grantsAccess()` e di
`PricingSyncService.hasActiveSubscription`). Verifica quante subscription insistono sul **vecchio tier**
(read-model subscription del core, per app e per tier) e presenta il quadro: quanti abbonati, su quale piano,
con quale fine periodo.

## Il default è già garantito — il grandfathering «gli esistenti restano»

Nel modello appgrove il grandfathering di default **non richiede alcuna azione**: hai lasciato il vecchio tier
**definito** nel listino (step-02), e la sync **non archivia** un tier con subscription attive. Gli abbonati
esistenti restano dove sono, sul vecchio prezzo; i **nuovi** vanno sul nuovo tier. Se questo è il trattamento
voluto — quasi sempre lo è — dillo esplicitamente, registralo, e non serve altro.

## La migrazione esplicita — è di questa skill, ma non la esegue

Se invece si vuole **spostare** gli abbonati esistenti sul nuovo prezzo (migrazione esplicita, oltre il
grandfathering di default), presenta, in prosa, le opzioni con la tua raccomandazione, **una alla volta**, e
**aspetta la decisione dello sviluppatore**:

- **Restano (grandfathering)** — nessuna azione (default). Il trattamento più rispettoso: chi ha comprato a un
  prezzo lo mantiene.
- **Migrano al nuovo prezzo** — riusa il meccanismo esistente `PaymentProvider.changeSubscriptionTier` per ogni
  subscription dal vecchio tier al nuovo (l'implementazione reale è comunque bloccata da #14 finché l'account
  del fornitore non esiste; lo stub funziona in locale). Attenzione al gate di giacenza sul downgrade (se il
  nuovo tier ha un tetto più basso). Un aumento di prezzo su abbonati esistenti è delicato: comunicazione e
  preavviso sono una decisione a parte, dello sviluppatore.

La skill **non esegue** la migrazione: la mette nel **runbook** (step-04), da eseguire dopo il merge, con la
persona presente. Qui si decide *cosa* fare, non lo si fa.

## Registra

La scelta (grandfathering di default vs migrazione, con la motivazione) va in `decisions.json` appena presa. In
autopilot, se hai dovuto proporre e attendere, registra sia la raccomandazione sia la risposta dello
sviluppatore. Prosegui con `step-04-close.md`.
