package app.appgrove.core.newsletter;

import app.appgrove.commons.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Voce del registro dei consensi (UC 0039, art. 7): prova immutabile di un consenso, di una sua
 * conferma o di una revoca. <b>Append-only</b>: creata e mai più modificata o cancellata (salvo la
 * purge/erasure che elimina l'intero iscritto). Platform-level come {@link NewsletterSubscriber}.
 *
 * <p>Minimizzazione: registra <i>tipo</i>, <i>versione del testo di consenso</i>, <i>canale</i> e
 * <i>marcatempo</i> — nessun indirizzo IP né user-agent. Il collegamento all'iscritto (e quindi
 * all'email) passa dal solo {@code subscriberId}; l'evento in sé non contiene dati identificativi.
 */
@Entity
@Table(schema = "platform", name = "consent_event")
public class ConsentEvent extends BaseEntity {

    @Column(name = "subscriber_id", nullable = false)
    private UUID subscriberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 16)
    private ConsentEventType eventType;

    @Column(name = "consent_text_version", nullable = false, length = 64)
    private String consentTextVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ConsentChannel channel;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected ConsentEvent() {
        // richiesto da JPA
    }

    public ConsentEvent(
            UUID subscriberId, ConsentEventType eventType, String consentTextVersion,
            ConsentChannel channel, Instant occurredAt) {
        this.subscriberId = subscriberId;
        this.eventType = eventType;
        this.consentTextVersion = consentTextVersion;
        this.channel = channel;
        this.occurredAt = occurredAt;
    }

    public UUID getSubscriberId() {
        return subscriberId;
    }

    public ConsentEventType getEventType() {
        return eventType;
    }

    public String getConsentTextVersion() {
        return consentTextVersion;
    }

    public ConsentChannel getChannel() {
        return channel;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
