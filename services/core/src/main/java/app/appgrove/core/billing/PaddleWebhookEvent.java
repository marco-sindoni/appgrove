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
 * {@code customer.*}); {@code paddleCustomerId} è valorizzato solo dagli eventi {@code customer.*}.
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
        String paddleSubscriptionId,
        String paddleCustomerId) {

    /** True per gli eventi {@code customer.*} (mappati su {@code accounts.paddle_customer_id}). */
    public boolean isCustomerEvent() {
        return eventType != null && eventType.startsWith("customer.");
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
                    text(data, "paddle_subscription_id"),
                    text(data, "paddle_customer_id"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload webhook non valido", e);
        }
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
