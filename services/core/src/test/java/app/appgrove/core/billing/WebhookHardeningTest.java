package app.appgrove.core.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.appgrove.core.TestData;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * L1 esaustivo dell'hardening UC 0025 (#09 D20): dedup su {@code event_id}, out-of-order via
 * {@code occurred_at}, set di eventi completo mappato su {@code subscription}/{@code accounts}, e
 * redrive → DLQ. Payload sintetici firmati attraverso la <b>stessa</b> pipeline ingest → consumer.
 * Deterministico (coda in-memory, drain esplicito, Testcontainers Postgres).
 */
@QuarkusTest
class WebhookHardeningTest {

    @Inject
    PaddleSignature signature;

    @Inject
    WebhookIngestService ingest;

    @Inject
    PaddleWebhookConsumer consumer;

    @Inject
    InMemoryWebhookQueue queue;

    @Inject
    TestData data;

    private final Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(5);

    @BeforeEach
    void reset() {
        queue.clear();
    }

    @Test
    void duplicateEventIsAppliedOnlyOnce() {
        String tenant = tenant();
        UUID app = newApp();
        String eventId = "evt_dup_" + UUID.randomUUID();

        deliver(WebhookFixtures.subscription(
                eventId, "subscription.activated", "active", base, tenant, app, null, year(base)));
        assertEquals("active", data.subscriptionStatus(tenant, app));

        // stesso event_id, body diverso (canceled): Paddle ri-invia → dedup, NON deve cancellare.
        deliver(WebhookFixtures.subscription(
                eventId, "subscription.canceled", "canceled", base.plusSeconds(1), tenant, app, null, year(base)));

        assertEquals("active", data.subscriptionStatus(tenant, app), "il duplicato è no-op");
        assertEquals(1, data.webhookEventCount(eventId), "una sola riga di dedup per event_id");
        assertEquals("processed", data.webhookOutcome(eventId));
    }

    @Test
    void olderEventDoesNotOverwriteNewerState() {
        String tenant = tenant();
        UUID app = newApp();

        deliver(WebhookFixtures.subscription(
                "evt_oo_new_" + UUID.randomUUID(), "subscription.activated", "active",
                base.plusSeconds(100), tenant, app, null, year(base)));
        assertEquals("active", data.subscriptionStatus(tenant, app));

        String stale = "evt_oo_old_" + UUID.randomUUID();
        deliver(WebhookFixtures.subscription(
                stale, "subscription.paused", "paused", base, tenant, app, null, year(base)));

        assertEquals("active", data.subscriptionStatus(tenant, app), "evento più vecchio ignorato");
        assertEquals("skipped_stale", data.webhookOutcome(stale));
    }

    @Test
    void everySubscribedEventMapsToExpectedStatus() {
        // Per gli eventi transaction.* lo status del payload è volutamente "sbagliato" → prova che la
        // mappatura del consumer è autoritativa (non si fida ciecamente del payload).
        record Case(String type, String payloadStatus, String expected) {}
        List<Case> cases = List.of(
                new Case("subscription.created", "trialing", "trialing"),
                new Case("subscription.activated", "active", "active"),
                new Case("subscription.updated", "active", "active"),
                new Case("subscription.paused", "paused", "paused"),
                new Case("subscription.resumed", "active", "active"),
                new Case("subscription.canceled", "canceled", "canceled"),
                new Case("transaction.completed", "trialing", "active"),
                new Case("transaction.payment_failed", "active", "past_due"),
                new Case("transaction.disputed", "active", "past_due"));

        int i = 0;
        for (Case c : cases) {
            String tenant = tenant();
            UUID app = newApp();
            deliver(WebhookFixtures.subscription(
                    "evt_map_" + (i++) + "_" + UUID.randomUUID(), c.type(), c.payloadStatus(),
                    base.plusSeconds(i), tenant, app, null, year(base)));
            assertEquals(c.expected(), data.subscriptionStatus(tenant, app), "tipo " + c.type());
        }
    }

    @Test
    void renewalAdvancesThePeriod() {
        String tenant = tenant();
        UUID app = newApp();
        Instant firstEnd = base.plus(365, ChronoUnit.DAYS);
        Instant renewedEnd = base.plus(730, ChronoUnit.DAYS);

        deliver(WebhookFixtures.subscription(
                "evt_ren_1_" + UUID.randomUUID(), "subscription.activated", "active", base, tenant, app, null, firstEnd));
        deliver(WebhookFixtures.subscription(
                "evt_ren_2_" + UUID.randomUUID(), "transaction.completed", "active",
                base.plusSeconds(1), tenant, app, null, renewedEnd));

        assertEquals("active", data.subscriptionStatus(tenant, app));
        assertEquals(renewedEnd, data.subscriptionPeriodEnd(tenant, app), "il rinnovo avanza il periodo");
    }

    @Test
    void customerEventCapturesPaddleCustomerId() {
        String tenant = tenant();
        data.account(tenant, "Acme");
        String eventId = "evt_cust_" + UUID.randomUUID();

        deliver(WebhookFixtures.customer(eventId, base, tenant, "ctm_12345"));

        assertEquals("ctm_12345", data.accountPaddleCustomerId(tenant));
        assertEquals("processed", data.webhookOutcome(eventId));
    }

    @Test
    void unsubscribedEventIsIgnoredWithoutMutation() {
        String tenant = tenant();
        UUID app = newApp();
        String eventId = "evt_unsub_" + UUID.randomUUID();

        deliver(WebhookFixtures.subscription(
                eventId, "subscription.trial_warning", "active", base, tenant, app, null, year(base)));

        assertNull(data.subscriptionStatus(tenant, app), "evento non sottoscritto → nessuna subscription");
        assertEquals("processed", data.webhookOutcome(eventId), "registrato come no-op (meno rumore)");
    }

    @Test
    void poisonMessageEndsInDlqAndIsNotLost() {
        String tenant = tenant();
        UUID ghostApp = UUID.randomUUID(); // NON nel catalogo → violazione FK su subscription.app_id

        String body = WebhookFixtures.subscription(
                "evt_poison_" + UUID.randomUUID(), "subscription.activated", "active",
                base, tenant, ghostApp, null, year(base));
        ingest.ingest(body, signature.sign(body)); // accodato (firma valida)

        // il consumer fallisce ad ogni tentativo (FK) e NON conferma → redrive fino a DLQ.
        for (int i = 0; i <= InMemoryWebhookQueue.MAX_RECEIVE_COUNT; i++) {
            consumer.drain();
        }

        assertEquals(0, queue.size(), "il messaggio velenoso non resta nella coda principale");
        assertEquals(1, queue.dlqSize(), "finisce in DLQ (non perso, non in loop infinito)");
        assertNull(data.subscriptionStatus(tenant, ghostApp), "nessuno stato scritto");
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private void deliver(String body) {
        ingest.ingest(body, signature.sign(body));
        consumer.drain();
    }

    private UUID newApp() {
        UUID app = UUID.randomUUID();
        data.app(app, "app-" + app.toString().substring(0, 8));
        return app;
    }

    private static String tenant() {
        return UUID.randomUUID().toString();
    }

    private static Instant year(Instant from) {
        return from.plus(365, ChronoUnit.DAYS);
    }
}
