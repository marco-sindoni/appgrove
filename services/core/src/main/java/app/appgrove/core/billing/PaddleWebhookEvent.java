package app.appgrove.core.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento webhook Paddle (forma sintetica dello stub, UC 0023). Gli eventi {@code subscription.*} e
 * {@code transaction.*} portano uno <b>snapshot</b> dello stato di subscription (catch-all
 * {@code subscription.updated}, #09 D21); gli eventi {@code customer.*} portano il
 * {@code paddle_customer_id} da salvare su {@code accounts}. Il consumer mappa ogni tipo a una
 * mutazione idempotente (vedi {@link SubscriptionWriter}). Il linkage tenant viene dai
 * {@code custom_data={tenant_id, app_id}} impostati server-side (non manomettibili, #09 C14/D21).
 *
 * <p>{@code status} è {@code null} per gli eventi che non sono uno snapshot di subscription (es.
 * {@code customer.*}); {@code paddleCustomerId} è valorizzato solo dagli eventi {@code customer.*};
 * {@code transaction} è valorizzato solo dagli eventi {@code transaction.*} che portano i dati economici
 * (UC 0096) — un evento di transazione senza quei dati resta valido e semplicemente non produce una riga
 * di storico, perché inventare un importo sarebbe peggio che non mostrarlo.
 *
 * <p>Forma JSON (subscription):
 * <pre>{
 *   "event_id": "...", "event_type": "subscription.updated", "occurred_at": "2026-..Z",
 *   "data": {
 *     "paddle_subscription_id": "sub_..", "status": "active",
 *     "current_period_start": "..Z", "current_period_end": "..Z",
 *     "cancel_at": null, "trial_end": null,
 *     "custom_data": { "tenant_id": "..", "app_id": "..", "app_tier_id": ".." }
 *   }
 * }</pre>
 */
public record PaddleWebhookEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String tenantId,
        UUID appId,
        UUID appTierId,
        SubscriptionStatus status,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        Instant cancelAt,
        Instant trialEnd,
        UUID scheduledTierId,
        Instant scheduledChangeAt,
        String paddleSubscriptionId,
        String paddleCustomerId,
        TransactionData transaction) {

    /**
     * Dati economici di una transazione (UC 0096): quanto, in che valuta, con quale ricevuta. Vengono dal
     * fornitore — il backend non li calcola mai da sé, perché il prezzo che l'utente ha davvero pagato lo
     * sa solo chi ha incassato.
     *
     * @param paddleTransactionId riferimento della transazione presso il fornitore (chiave di idempotenza)
     * @param amount importo in unità minori (centesimi)
     * @param billingCycle etichetta del ciclo ({@code monthly}/{@code annual}), da mostrare così com'è
     * @param receiptUrl ricevuta del fornitore; {@code null} quando non è ancora disponibile
     * @param billedAt istante dell'addebito; se assente vale l'{@code occurred_at} dell'evento
     */
    public record TransactionData(
            String paddleTransactionId,
            int amount,
            String currency,
            String billingCycle,
            String receiptUrl,
            Instant billedAt) {}

    /** True per gli eventi {@code customer.*} (mappati su {@code accounts.paddle_customer_id}). */
    public boolean isCustomerEvent() {
        return eventType != null && eventType.startsWith("customer.");
    }

    /**
     * Esito da registrare nello storico per questo evento, o {@code null} se l'evento non è una
     * transazione da mostrare. Sta qui, accanto alla forma del payload, per la stessa ragione per cui
     * {@link WebhookEventMapping} tiene la mappa verso lo stato di subscription: la semantica dei tipi di
     * evento non si sparpaglia.
     */
    public BillingTransactionStatus transactionStatus() {
        if (eventType == null) {
            return null;
        }
        return switch (eventType) {
            case "transaction.completed" -> BillingTransactionStatus.paid;
            case "transaction.payment_failed" -> BillingTransactionStatus.failed;
            case "transaction.disputed" -> BillingTransactionStatus.disputed;
            default -> null;
        };
    }

    public static PaddleWebhookEvent from(ObjectMapper mapper, String body) {
        try {
            JsonNode root = mapper.readTree(body);
            JsonNode data = root.path("data");
            JsonNode custom = data.path("custom_data");
            String statusText = text(data, "status");
            return new PaddleWebhookEvent(
                    text(root, "event_id"),
                    text(root, "event_type"),
                    instant(root, "occurred_at"),
                    text(custom, "tenant_id"),
                    uuid(custom, "app_id"),
                    uuid(custom, "app_tier_id"),
                    statusText == null ? null : SubscriptionStatus.valueOf(statusText),
                    instant(data, "current_period_start"),
                    instant(data, "current_period_end"),
                    instant(data, "cancel_at"),
                    instant(data, "trial_end"),
                    uuid(custom, "scheduled_tier_id"),
                    instant(data, "scheduled_change_at"),
                    text(data, "paddle_subscription_id"),
                    text(data, "paddle_customer_id"),
                    transaction(data, instant(root, "occurred_at")));
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload webhook non valido", e);
        }
    }

    /**
     * Dati economici, se il payload li porta. Il riferimento della transazione è il minimo indispensabile
     * (è la chiave di idempotenza): senza, non si registra nulla — meglio uno storico incompleto di uno
     * storico che duplica la stessa riga a ogni ri-consegna dell'evento.
     */
    private static TransactionData transaction(JsonNode data, Instant occurredAt) {
        String id = text(data, "paddle_transaction_id");
        if (id == null) {
            return null;
        }
        JsonNode amount = data.get("amount");
        Instant billedAt = instant(data, "billed_at");
        return new TransactionData(
                id,
                amount == null || amount.isNull() ? 0 : amount.asInt(),
                text(data, "currency"),
                text(data, "billing_cycle"),
                text(data, "receipt_url"),
                billedAt != null ? billedAt : occurredAt);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static UUID uuid(JsonNode node, String field) {
        String v = text(node, field);
        return v == null ? null : UUID.fromString(v);
    }

    private static Instant instant(JsonNode node, String field) {
        String v = text(node, field);
        return v == null ? null : Instant.parse(v);
    }
}
