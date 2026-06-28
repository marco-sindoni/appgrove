package app.appgrove.core.billing;

/**
 * Scenari del ciclo di vita subscription simulabili offline dallo stub (#09 I39): coprono il lifecycle
 * E in locale senza account Paddle. Ogni scenario è una sequenza di webhook sintetici firmati che, una
 * volta consumati, portano la {@code subscription} allo stato atteso.
 */
public enum LifecycleScenario {
    /** created(trialing) → activated(active): acquisto andato a buon fine. */
    happy_path,
    /** active → payment_failed: il rinnovo fallisce, si entra in {@code past_due} (dunning, #09 E26). */
    past_due,
    /** active → canceled: disdetta (accesso fino a fine periodo, poi off, #09 E25). */
    canceled,
    /** active(tier) → active(targetTier): upgrade immediato (#09 E22). */
    upgrade,
    /** active(tier) → active(targetTier): downgrade (qui semplificato; scheduling vero → UC 0026). */
    downgrade,
    /** active → paused: sospensione (status {@code paused} = no accesso, #09 E28). */
    paused,
    /** active → paused → resumed(active): ripresa dopo pausa. */
    resumed,
    /** active → transaction.completed: rinnovo riuscito, il periodo avanza (#09 D21). */
    renewal,
    /** active → transaction.disputed: chargeback/dispute → {@code past_due} (reazione MoR, #09 J42). */
    chargeback,
    /** customer.updated: cattura {@code paddle_customer_id} su {@code accounts} (#09 C15/D21). */
    customer
}
