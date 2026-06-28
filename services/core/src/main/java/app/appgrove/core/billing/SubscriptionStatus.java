package app.appgrove.core.billing;

/**
 * Stati di una subscription (= verità di billing, scritta dai webhook — #09 B11/D21). I nomi
 * coincidono con i valori {@code varchar} in {@code platform.subscription.status} (@Enumerated STRING).
 * Mappa accesso → entitlement (#09 dec.29): accesso se {@code ∈ {trialing, active, past_due}}.
 */
public enum SubscriptionStatus {
    trialing,
    active,
    past_due,
    canceled,
    paused
}
