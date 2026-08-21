package app.appgrove.core.platform;

import app.appgrove.commons.persistence.BaseTenantEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;

/**
 * Accesso di una persona a una applicazione, con il suo ruolo (UC 0098): la riga che dice «questa
 * persona può usare questa applicazione con questo ruolo». Entità di <b>account</b>, tenant-scoped:
 * il filtro {@code WHERE tenant_id = ?} è automatico (discriminatore, invariante #2) e non va
 * riscritto a mano.
 *
 * <p>Il riferimento è all'{@link Identity identità} della persona e non alla sua
 * {@link Membership appartenenza}: l'appartenenza a un account può chiudersi e riaprirsi, l'identità
 * no. Chi legge questa riga deve comunque verificare che la persona <b>appartenga ancora</b>
 * all'account: la riga di accesso è un permesso, non una prova di appartenenza.
 *
 * <p><b>L'owner non ha righe qui.</b> L'accesso gli è implicito su tutte le applicazioni dell'account
 * (UC 0098 §5): una riga per l'owner andrebbe creata a ogni applicazione installata e potrebbe essere
 * cancellata per errore. Il costo da ricordare è che ogni lettura di «chi ha accesso» deve
 * <b>aggiungere</b> l'owner al risultato — lo fa {@link AppAccessResource}.
 *
 * <p>Un'applicazione disattivata dalla piattaforma <b>non</b> cancella queste righe: è il diritto
 * dell'account a decadere, e riattivandola gli accessi tornano validi senza doverli ricostruire.
 */
@Entity
@Table(schema = "platform", name = "app_access")
@SQLRestriction("deleted_at is null")
public class AppAccess extends BaseTenantEntity {

    @Column(name = "app_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID appId;

    /**
     * Dato personale, come {@code membership.identity_id} e per la stessa ragione: non contiene né
     * nome né indirizzo, ma <b>dice qualcosa di una persona</b> — e qui dice di più, perché dice quale
     * applicazione usa e con quale potere. Il titolare che ne risponde è l'account.
     */
    @PersonalData(
            category = "identificativo online (riferimento all'identità della persona) associato al ruolo su una applicazione",
            purpose = "consentire alle persone dell'account di usare le applicazioni acquistate, con il ruolo assegnato",
            retention = "finché l'accesso è vivo; eliminato con l'appartenenza e con l'account (#13 E25)")
    @Column(name = "identity_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID identityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AppRole role;

    /** Identità di chi ha concesso l'accesso: serve alla traccia di controllo, non all'autorizzazione. */
    @Column(name = "granted_by", updatable = false, columnDefinition = "uuid")
    private UUID grantedBy;

    protected AppAccess() {
        // richiesto da JPA
    }

    public AppAccess(UUID appId, UUID identityId, AppRole role, UUID grantedBy) {
        this.appId = appId;
        this.identityId = identityId;
        this.role = role;
        this.grantedBy = grantedBy;
    }

    public UUID getAppId() {
        return appId;
    }

    public UUID getIdentityId() {
        return identityId;
    }

    public AppRole getRole() {
        return role;
    }

    public void setRole(AppRole role) {
        this.role = role;
    }

    public UUID getGrantedBy() {
        return grantedBy;
    }
}
