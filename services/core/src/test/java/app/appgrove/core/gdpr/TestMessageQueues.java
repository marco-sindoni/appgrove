package app.appgrove.core.gdpr;

import app.appgrove.commons.messaging.InMemoryMessageQueues;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

/** Code in-memory nei test (sostituiscono {@code SqsMessageQueues}): deterministiche e offline. */
@Mock
@ApplicationScoped
public class TestMessageQueues extends InMemoryMessageQueues {}
