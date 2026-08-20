package app.appgrove.core.platform;

import app.appgrove.commons.persistence.BaseEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

/**
 * Identità di accesso della persona (UC 0116). Entità di <b>piattaforma</b>: NON estende
 * {@link app.appgrove.commons.persistence.BaseTenantEntity} perché non appartiene a nessun account —
 * come {@code platform.app}. Qui vive l'unicità globale di indirizzo di posta e identificativo di
 * autenticazione, che prima stava (a torto) su una tabella interna all'account.
 *
 * <p>È l'unica entità di piattaforma con dati personali diretti: va interrogata solo attraverso
 * un'appartenenza quando si è dentro un percorso di account (#02, invariante #2). {@code email},
 * {@code displayName}, {@code locale} e {@code cognitoSub} sono dati personali (#13 C) — stessi dati
 * di prima, cambia chi risponde per essi.
 *
 * <p>{@link #status} è la leva del <b>titolare</b> (la persona può accedere alla piattaforma:
 * limitazione del trattamento, art. 18), distinta dalla leva dell'<b>owner</b> sul singolo account,
 * che è {@link Membership#getStatus()}.
 */
@Entity
@Table(schema = "platform", name = "identity")
@SQLRestriction("deleted_at is null")
public class Identity extends BaseEntity {

    @PersonalData(
            category = "identificativo online (subject Cognito)",
            purpose = "collegamento identità di autenticazione ↔ profilo applicativo",
            retention = "identità attiva (ultima appartenenza) + grace 14gg (#13 E25)")
    @Column(name = "cognito_sub", nullable = false, updatable = false)
    private String cognitoSub;

    @PersonalData(
            category = "contatto",
            purpose = "erogazione e gestione account (login, comunicazioni di servizio)",
            retention = "identità attiva (ultima appartenenza) + grace 14gg (#13 E25)")
    @Column(nullable = false)
    private String email;

    @PersonalData(
            category = "identità (nome visualizzato)",
            purpose = "identificazione della persona nella UI e negli account a cui appartiene",
            retention = "identità attiva (ultima appartenenza) + grace 14gg (#13 E25)")
    @Column(name = "display_name")
    private String displayName;

    /**
     * Lingua della persona per le email transazionali (UC 0018): fonte di verità unica, letta dal
     * servizio auth per scegliere il template EN/IT. È della <b>persona</b>, non dell'account — se un
     * giorno un account volesse imporre la lingua ai propri membri servirebbe un valore per
     * appartenenza che vinca su questo (punto aperto di UC 0060). Mai nulla: chi non l'ha espressa
     * è {@code en}.
     */
    @PersonalData(
            category = "preferenza (lingua)",
            purpose = "lingua delle email transazionali di autenticazione",
            retention = "identità attiva (ultima appartenenza) + grace 14gg (#13 E25)")
    @Column(nullable = false, length = 8)
    private String locale = "en";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdentityStatus status = IdentityStatus.active;

    /**
     * Causale della sospensione (UC 0034): {@code gdpr_restriction} = limitazione del trattamento
     * (art. 18, #13 D19), distinta da una sospensione amministrativa. Null se non sospesa.
     */
    @Column(name = "suspended_reason", length = 32)
    private String suspendedReason;

    protected Identity() {
        // richiesto da JPA
    }

    public Identity(String cognitoSub, String email, String displayName) {
        this.cognitoSub = cognitoSub;
        this.email = email;
        this.displayName = displayName;
    }

    public String getCognitoSub() {
        return cognitoSub;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public IdentityStatus getStatus() {
        return status;
    }

    public void setStatus(IdentityStatus status) {
        this.status = status;
    }

    public String getSuspendedReason() {
        return suspendedReason;
    }

    public void setSuspendedReason(String suspendedReason) {
        this.suspendedReason = suspendedReason;
    }
}
