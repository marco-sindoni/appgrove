package app.appgrove.core.billing;

import java.time.Instant;

/**
 * Derivazione <b>pura</b> della semantica di ciclo di vita di una {@link Subscription} (UC 0026): a
 * partire dallo snapshot di billing ({@code status}, {@code cancel_at}, {@code current_period_end})
 * classifica la <b>fase di accesso</b> e ne deriva {@code access} + {@code accessUntil}. È il
 * read-model che <b>UC 0027</b> (enforcement) e <b>UC 0028</b> (self-service/UX) consumeranno; qui resta
 * logica di dominio testata, <b>senza tabelle nuove</b> (entitlement derivato, invariante #09 B12).
 *
 * <p>Confine: {@code access} viene dalla <b>mappa canonica</b> {@link SubscriptionStatus#grantsAccess()}
 * (fonte unica), così {@code access} e {@link Phase} sono coerenti per costruzione. La temporizzazione
 * dei <b>2 settimane</b> di dunning la fa Paddle (poi flippa lo status); qui non c'è alcun timer locale.
 * La <b>surfacing del cambio tier schedulato</b> (downgrade a fine periodo verso un tier X) richiede
 * persistenza non presente nello schema attuale → è di UC 0028 e non è una fase di accesso (a parità di
 * accesso resta {@link Phase#ACTIVE}).
 *
 * @param phase fase di accesso derivata
 * @param access {@code true} se la subscription concede accesso (= {@code status.grantsAccess()})
 * @param accessUntil fine accesso <b>nota</b>: in {@link Phase#CANCELING} è {@code cancel_at} (cutoff
 *     definitivo, nessun rinnovo); in {@link Phase#TRIAL}/{@link Phase#ACTIVE}/{@link Phase#GRACE} è
 *     {@code current_period_end} (confine del periodo corrente, rinnovabile); {@code null} in
 *     {@link Phase#ENDED}.
 */
public record SubscriptionLifecycle(Phase phase, boolean access, Instant accessUntil) {

    /** Fase di accesso derivata dallo stato di billing. */
    public enum Phase {
        /** {@code trialing}: prova in corso (carta upfront, conversione automatica a pagamento). */
        TRIAL,
        /** {@code active} senza disdetta programmata: accesso pieno, rinnovo automatico. */
        ACTIVE,
        /** {@code active} con {@code cancel_at} valorizzato: accesso fino a {@code cancel_at}, riattivabile. */
        CANCELING,
        /** {@code past_due}: dunning/grace, accesso mantenuto durante i retry Paddle. */
        GRACE,
        /** {@code canceled}/{@code paused}: nessun accesso. */
        ENDED
    }

    /** Deriva la semantica dallo snapshot di {@code subscription}. */
    public static SubscriptionLifecycle of(Subscription sub) {
        return of(sub.getStatus(), sub.getCancelAt(), sub.getCurrentPeriodEnd());
    }

    /** Variante su campi grezzi (testabile senza persistenza); {@code status} non nullo. */
    public static SubscriptionLifecycle of(
            SubscriptionStatus status, Instant cancelAt, Instant currentPeriodEnd) {
        boolean access = status.grantsAccess();
        return switch (status) {
            case canceled, paused -> new SubscriptionLifecycle(Phase.ENDED, access, null);
            case past_due -> new SubscriptionLifecycle(Phase.GRACE, access, currentPeriodEnd);
            case trialing -> new SubscriptionLifecycle(Phase.TRIAL, access, currentPeriodEnd);
            case active ->
                cancelAt != null
                        ? new SubscriptionLifecycle(Phase.CANCELING, access, cancelAt)
                        : new SubscriptionLifecycle(Phase.ACTIVE, access, currentPeriodEnd);
        };
    }
}
