package app.appgrove.core.billing;

/**
 * Mappa esplicita <b>event_type → stato {@code subscription}</b> (set sottoscritto #09 D21). Tiene la
 * semantica nel consumer (non si "fida" ciecamente del payload): per gli eventi di pagamento lo stato è
 * forzato, per gli eventi {@code subscription.*} si usa lo stato dello snapshot. Gli eventi non
 * sottoscritti tornano {@code null} → il consumer li registra come no-op (meno rumore/superficie).
 *
 * <p>Confine: qui si decide <b>solo</b> lo stato di {@code subscription}. La <b>semantica</b> del ciclo di
 * vita (dunning/grace, accesso fino a fine periodo, gating downgrade) è <b>UC 0026</b>; l'enforcement
 * dell'entitlement è <b>UC 0027</b>. La precisione del formato eventi reali Paddle è L3 (UC 0029).
 */
final class WebhookEventMapping {

    private WebhookEventMapping() {}

    /**
     * Stato target per l'evento, o {@code null} se non ha effetto su {@code subscription} (ignorato).
     */
    static SubscriptionStatus targetStatus(PaddleWebhookEvent event) {
        String type = event.eventType();
        if (type == null) {
            return null;
        }
        return switch (type) {
            // subscription.*: lo snapshot porta lo stato risultante (created/activated/updated/canceled/
            // paused/resumed). updated è il catch-all (status/tier/period/cancel_at/past_due).
            case "subscription.created",
                    "subscription.activated",
                    "subscription.updated",
                    "subscription.canceled",
                    "subscription.paused",
                    "subscription.resumed" ->
                event.status();
            // transaction.completed: pagamento riuscito (attivazione + rinnovi) → active, periodo avanzato.
            case "transaction.completed" -> SubscriptionStatus.active;
            // dunning + chargeback/dispute (#09 J42): reagiamo via webhook portando la subscription a rischio.
            // La semantica suspend-vs-grace è UC 0026; qui marchiamo past_due.
            case "transaction.payment_failed", "transaction.disputed" -> SubscriptionStatus.past_due;
            default -> null; // evento non sottoscritto → ignorato (no-op registrato)
        };
    }
}
