package app.appgrove.core.newsletter;

import app.appgrove.commons.persistence.BaseEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Iscritto alla newsletter (UC 0039). <b>Platform-level</b>: estende {@link BaseEntity} (non
 * {@code BaseTenantEntity}), perché l'iscrizione dal sito vetrina arriva senza JWT, quindi senza
 * {@code tenant_id}. Come {@code webhook_event}, non è soggetto al filtro row-level automatico.
 *
 * <p>Una riga per indirizzo (unicità su {@code lower(email)}). Lo stato governa il double opt-in:
 * {@code pending} non riceve marketing; {@code confirmed} sì; {@code unsubscribed} è la revoca.
 * Il token di conferma è persistito solo come hash SHA-256 (single-use, come gli inviti).
 */
@Entity
@Table(schema = "platform", name = "newsletter_subscriber")
public class NewsletterSubscriber extends BaseEntity {

    @PersonalData(
            category = "contatto (indirizzo email dell'iscritto)",
            purpose = "invio della newsletter (marketing diretto) previo consenso",
            legalBasis = "consenso (art. 6.1.a GDPR)",
            retention = "iscritto + 24 mesi dopo la disiscrizione (#13 E)")
    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SubscriberStatus status = SubscriberStatus.pending;

    /** Lingua per l'email di conferma double opt-in (en/it con ripiego, come UC 0018). */
    @Column(nullable = false, length = 8)
    private String locale = "en";

    /** Canale dell'iscrizione iniziale (provenienza della prova di consenso). */
    @Enumerated(EnumType.STRING)
    @Column(name = "origin_channel", nullable = false, length = 16)
    private ConsentChannel originChannel;

    /**
     * Provenienza opzionale: id dell'utente che si è iscritto dal toggle account. Nullo per le
     * iscrizioni anonime (sito/signup). NON è la base del collegamento GDPR — quello avviene per
     * confronto dell'email (vedi {@code PlatformDataContract}).
     */
    @Column(name = "user_id")
    private UUID userId;

    /** Hash SHA-256 (hex) del token di conferma; nullo dopo la conferma (single-use). */
    @Column(name = "confirm_token_hash", length = 64)
    private String confirmTokenHash;

    @Column(name = "confirm_expires_at")
    private Instant confirmExpiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "unsubscribed_at")
    private Instant unsubscribedAt;

    protected NewsletterSubscriber() {
        // richiesto da JPA
    }

    public NewsletterSubscriber(String email, String locale, ConsentChannel originChannel) {
        this.email = email;
        this.locale = locale;
        this.originChannel = originChannel;
    }

    public String getEmail() {
        return email;
    }

    public SubscriberStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriberStatus status) {
        this.status = status;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public ConsentChannel getOriginChannel() {
        return originChannel;
    }

    public void setOriginChannel(ConsentChannel originChannel) {
        this.originChannel = originChannel;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getConfirmTokenHash() {
        return confirmTokenHash;
    }

    public void setConfirmTokenHash(String confirmTokenHash) {
        this.confirmTokenHash = confirmTokenHash;
    }

    public Instant getConfirmExpiresAt() {
        return confirmExpiresAt;
    }

    public void setConfirmExpiresAt(Instant confirmExpiresAt) {
        this.confirmExpiresAt = confirmExpiresAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getUnsubscribedAt() {
        return unsubscribedAt;
    }

    public void setUnsubscribedAt(Instant unsubscribedAt) {
        this.unsubscribedAt = unsubscribedAt;
    }
}
