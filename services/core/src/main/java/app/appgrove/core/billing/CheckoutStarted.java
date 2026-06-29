package app.appgrove.core.billing;

import java.util.UUID;

/**
 * Evento di dominio (CDI) emesso da {@link CheckoutResource} quando un checkout è stato avviato lato
 * server. In <b>prod</b> non ha osservatori: l'attivazione avviene <b>solo</b> via webhook reale di Paddle
 * (invariante #09 C16). In <b>dev/test</b> lo osserva {@link StubCheckoutActivation}, che simula l'invio del
 * webhook da parte di Paddle (stub) così che la UX a polling locale arrivi ad "attivato" senza passi manuali.
 *
 * <p>Disaccoppiare via evento tiene {@code CheckoutResource} fedele alla prod (nessuna auto-attivazione nel
 * codice di checkout): è il bean stub, e solo lui, a reagire.
 */
public record CheckoutStarted(String tenantId, UUID appId, UUID appTierId, String billingCycle) {}
