package app.appgrove.core.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Emette i webhook sintetici <b>firmati</b> di uno scenario lifecycle (UC 0023). Ogni evento è uno
 * snapshot completo dello stato di subscription, firmato HMAC e passato per la <b>stessa</b> pipeline
 * di ingest dei webhook reali ({@link WebhookIngestService}: verifica firma → coda → consumer). Il
 * linkage tenant è impostato server-side nei {@code custom_data} (#09 C14/D21).
 *
 * <p>Sa generare anche i <b>casi limite</b> per L1 (firma errata, duplicato, out-of-order); la
 * gestione completa lato consumer (dedup/out-of-order) è di UC 0025.
 */
@ApplicationScoped
public class StubScenarioEmitter {

    @Inject
    WebhookIngestService ingest;

    @Inject
    PaddleSignature signature;

    @Inject
    ObjectMapper mapper;

    /** Evento emesso, per la risposta dell'endpoint dev. */
    public record EmittedEvent(String eventType, String status) {}

    /**
     * Emette la sequenza dello scenario per {@code (tenant, app)}. Per upgrade/downgrade
     * {@code targetTierId} è il tier di destinazione (per gli altri scenari è ignorato).
     */
    public List<EmittedEvent> emit(
            LifecycleScenario scenario, String tenantId, UUID appId, UUID appTierId, UUID targetTierId) {
        String paddleSubId = "sub_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        List<EmittedEvent> out = new ArrayList<>();
        switch (scenario) {
            case happy_path -> {
                out.add(send("subscription.created", SubscriptionStatus.trialing, tenantId, appId, appTierId,
                        paddleSubId, base, base, base.plus(14, ChronoUnit.DAYS), null, base.plus(14, ChronoUnit.DAYS)));
                out.add(send("subscription.activated", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base.plusSeconds(1), base, base.plus(365, ChronoUnit.DAYS), null, null));
            }
            case past_due -> {
                out.add(send("subscription.activated", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base, base, base.plus(365, ChronoUnit.DAYS), null, null));
                out.add(send("transaction.payment_failed", SubscriptionStatus.past_due, tenantId, appId, appTierId,
                        paddleSubId, base.plusSeconds(1), base, base.plus(365, ChronoUnit.DAYS), null, null));
            }
            case canceled -> {
                out.add(send("subscription.activated", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base, base, base.plus(365, ChronoUnit.DAYS), null, null));
                out.add(send("subscription.canceled", SubscriptionStatus.canceled, tenantId, appId, appTierId,
                        paddleSubId, base.plusSeconds(1), base, base.plus(365, ChronoUnit.DAYS),
                        base.plus(365, ChronoUnit.DAYS), null));
            }
            case upgrade -> {
                out.add(send("subscription.activated", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base, base, base.plus(365, ChronoUnit.DAYS), null, null));
                out.add(send("subscription.updated", SubscriptionStatus.active, tenantId, appId,
                        require(targetTierId, "targetTierId richiesto per upgrade"),
                        paddleSubId, base.plusSeconds(1), base, base.plus(365, ChronoUnit.DAYS), null, null));
            }
            case downgrade -> {
                out.add(send("subscription.activated", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base, base, base.plus(365, ChronoUnit.DAYS), null, null));
                out.add(send("subscription.updated", SubscriptionStatus.active, tenantId, appId,
                        require(targetTierId, "targetTierId richiesto per downgrade"),
                        paddleSubId, base.plusSeconds(1), base, base.plus(365, ChronoUnit.DAYS), null, null));
            }
        }
        return out;
    }

    private EmittedEvent send(
            String eventType, SubscriptionStatus status, String tenantId, UUID appId, UUID appTierId,
            String paddleSubId, Instant occurredAt, Instant periodStart, Instant periodEnd,
            Instant cancelAt, Instant trialEnd) {
        String body = build(eventType, status, tenantId, appId, appTierId, paddleSubId, occurredAt,
                periodStart, periodEnd, cancelAt, trialEnd);
        ingest.ingest(body, signature.sign(body));
        return new EmittedEvent(eventType, status.name());
    }

    /** Serializza un evento nella forma attesa da {@link PaddleWebhookEvent}. */
    String build(
            String eventType, SubscriptionStatus status, String tenantId, UUID appId, UUID appTierId,
            String paddleSubId, Instant occurredAt, Instant periodStart, Instant periodEnd,
            Instant cancelAt, Instant trialEnd) {
        ObjectNode root = mapper.createObjectNode();
        root.put("event_id", "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        root.put("event_type", eventType);
        root.put("occurred_at", occurredAt.toString());
        ObjectNode data = root.putObject("data");
        data.put("paddle_subscription_id", paddleSubId);
        data.put("status", status.name());
        putInstant(data, "current_period_start", periodStart);
        putInstant(data, "current_period_end", periodEnd);
        putInstant(data, "cancel_at", cancelAt);
        putInstant(data, "trial_end", trialEnd);
        ObjectNode custom = data.putObject("custom_data");
        custom.put("tenant_id", tenantId);
        custom.put("app_id", appId.toString());
        if (appTierId != null) {
            custom.put("app_tier_id", appTierId.toString());
        }
        try {
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Serializzazione webhook fallita", e);
        }
    }

    private static void putInstant(ObjectNode node, String field, Instant value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value.toString());
        }
    }

    private static UUID require(UUID value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
