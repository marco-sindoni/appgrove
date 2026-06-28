package app.appgrove.core.catalog;

/** Ciclo di fatturazione di un price: {@code monthly} o {@code annual} (#09 B10, (tier × ciclo) = 1 Price). */
public enum BillingCycle {
    monthly,
    annual
}
