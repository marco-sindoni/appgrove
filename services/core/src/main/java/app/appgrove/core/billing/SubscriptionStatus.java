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
    paused;

    /**
     * Mappa <b>status → accesso</b> consolidata (#09 E29) — <b>unica fonte di verità</b> del cuore del
     * gate entitlement (gate 3, 402). Accesso se {@code ∈ {trialing, active, past_due}} (incluso il
     * dunning/grace: in {@code past_due} Paddle mantiene l'accesso durante i retry; finita la grace
     * flippa lo status a {@code canceled}/{@code paused}, e qui l'accesso decade da sé). No accesso se
     * {@code ∈ {paused, canceled}}.
     *
     * <p>È lo stato di billing puro: l'eventuale gate "app abilitata" ({@code app.status}, gate 2) e
     * l'enforcement runtime sono di UC 0021/0027 e si <b>compongono</b> con questa mappa, non la
     * duplicano.
     */
    public boolean grantsAccess() {
        return switch (this) {
            case trialing, active, past_due -> true;
            case canceled, paused -> false;
        };
    }
}
