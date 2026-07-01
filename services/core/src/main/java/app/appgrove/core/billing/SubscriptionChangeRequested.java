package app.appgrove.core.billing;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento di dominio (CDI) emesso da {@code SubscriptionResource} quando un'azione self-service di gestione
 * abbonamento (cambio tier / disdetta / riattivazione) è stata avviata lato server (UC 0028). Porta lo
 * <b>snapshot risultante</b> già calcolato dal resource, così che l'osservatore possa emettere un
 * {@code subscription.updated} fedele.
 *
 * <p>In <b>prod</b> non ha osservatori: l'effetto arriva <b>solo</b> dal webhook reale di Paddle dopo la
 * chiamata all'API di update (invariante #09 C16). In <b>dev/test</b> lo osserva
 * {@link StubSubscriptionActivation}, che simula l'invio del webhook. Stesso disaccoppiamento del checkout
 * ({@link CheckoutStarted}): il resource resta fedele alla prod (nessuna scrittura diretta della subscription).
 *
 * @param resultTierId tier dopo il cambio: il target per un upgrade immediato, il tier corrente (invariato)
 *     per downgrade schedulato / disdetta / riattivazione
 * @param scheduledTierId tier di destinazione del downgrade schedulato, o {@code null}
 * @param scheduledChangeAt istante di efficacia del downgrade schedulato, o {@code null}
 */
public record SubscriptionChangeRequested(
        String tenantId,
        UUID appId,
        String paddleSubscriptionId,
        UUID resultTierId,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        Instant cancelAt,
        UUID scheduledTierId,
        Instant scheduledChangeAt,
        Instant afterOccurredAt) {}
