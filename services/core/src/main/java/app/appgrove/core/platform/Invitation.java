package app.appgrove.core.platform;

import app.appgrove.commons.persistence.BaseTenantEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;

/**
 * Invito a entrare in un tenant. Tenant-scoped (discriminator). Il token grezzo NON è persistito:
 * si salva solo {@code tokenHash} (single-use). L'accettazione (→ creazione utente) è UC 0017, e per
 * chi ha già un'identità avviene <b>dalla propria sessione</b> (UC 0118, {@link MeInvitationsResource}).
 */
@Entity
@Table(schema = "platform", name = "invitations")
@SQLRestriction("deleted_at is null")
public class Invitation extends BaseTenantEntity {

    @PersonalData(
            category = "contatto (email dell'invitato)",
            purpose = "recapito e gestione dell'invito",
            legalBasis = "misure precontrattuali/contratto",
            retention = "fino a scadenza/accettazione dell'invito")
    @Column(nullable = false)
    private String email;

    /**
     * <b>Residuo, non scelta</b> (UC 0100). Il ruolo dell'invito è uscito dal contratto — non si chiede
     * più a chi invita e non si restituisce a chi rilegge — ma la colonna resta, perché è
     * {@code NOT NULL} senza valore predefinito (V2) e perché il suo valore è quello con cui nasce
     * l'appartenenza all'accettazione ({@link MeInvitationsResource}, e il servizio di autenticazione
     * per chi si registra dall'invito). Vale quindi <b>sempre</b> {@code member}: non esiste più un modo
     * di scriverlo diverso. Il ritiro della colonna è della conversione di UC 0113.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipRole role = MembershipRole.member;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.pending;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Column(name = "accepted_user_id")
    private UUID acceptedUserId;

    /**
     * L'identità che <b>già esisteva</b> quando l'invito è stato mandato, se esisteva (UC 0118).
     *
     * <p>Valorizzata lato server all'invio; nulla nel caso normale — la maggior parte degli invitati
     * non esiste ancora sulla piattaforma — e un valore nullo significa «all'invio non c'era», non
     * «non controllato».
     *
     * <p><b>Non esce mai verso chi ha invitato</b> (UC 0118 §5): non compare in
     * {@link InvitationDtos.InvitationView} e nessuna interfaccia di account lo mostra. Nell'
     * esportazione dei dati dell'account è <b>ristretto agli inviti accettati</b>: a invito accettato
     * quella persona è un membro dell'account, quindi il riferimento non rivela nulla che l'account
     * non sappia già; su un invito ancora in attesa direbbe invece che quella persona aveva già un
     * rapporto con la piattaforma, che è esattamente l'informazione che non gli appartiene. Stessa
     * forma della restrizione usata per {@code identity.active_membership_id} (UC 0117).
     */
    @PersonalData(
            category = "identificativo online (riferimento all'identità della persona invitata)",
            purpose = "collegare l'invito a una persona che esiste già, per farla entrare senza coniarne "
                    + "una seconda identità",
            legalBasis = "misure precontrattuali/contratto",
            retention = "fino a scadenza/chiusura dell'invito; eliminato con l'account (#13 E25)")
    @Column(name = "identity_id")
    private UUID identityId;

    /**
     * L'addebito che ha <b>autorizzato</b> questo posto (UC 0103): riferimento opaco alla transazione
     * presso il fornitore di pagamento.
     *
     * <p>Nullo quando l'invito non ha richiesto denaro — posto dentro la franchigia, oppure posto già
     * pagato in questo periodo perché rimpiazza un invito scaduto o revocato. Un valore nullo significa
     * «non è stato dovuto nulla», <b>non</b> «non verificato»: senza addebito riuscito l'invito non nasce
     * affatto, quindi una riga esistente è per costruzione una riga autorizzata.
     *
     * <p><b>Dichiarato come dato personale per prudenza</b>, benché descriva una transazione dell'account
     * e non la persona: sta su una riga il cui soggetto <i>è</i> una persona, e collega quella persona a
     * un pagamento. La classificazione conservativa costa una voce di manifesto e nessun obbligo nuovo —
     * la riga di invito è già esportata ed eliminata con l'account — mentre la classificazione permissiva
     * costerebbe, se sbagliata, un campo non dichiarato.
     */
    @PersonalData(
            category = "dato di fatturazione (riferimento alla transazione che ha autorizzato il posto)",
            purpose = "dimostrare che il posto di quella persona era pagato prima che l'invito partisse, "
                    + "e poter annullare l'addebito se l'invito non nasce",
            legalBasis = "contratto",
            retention = "come l'invito che lo porta; eliminato con l'account (#13 E25)")
    @Column(name = "seat_charge_ref", length = 64)
    private String seatChargeRef;

    protected Invitation() {
        // richiesto da JPA
    }

    public Invitation(String email, String tokenHash, Instant expiresAt, UUID invitedBy) {
        this.email = email;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.invitedBy = invitedBy;
    }

    public String getEmail() {
        return email;
    }

    public MembershipRole getRole() {
        return role;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public UUID getAcceptedUserId() {
        return acceptedUserId;
    }

    public void setAcceptedUserId(UUID acceptedUserId) {
        this.acceptedUserId = acceptedUserId;
    }

    public UUID getIdentityId() {
        return identityId;
    }

    public void setIdentityId(UUID identityId) {
        this.identityId = identityId;
    }

    public String getSeatChargeRef() {
        return seatChargeRef;
    }

    public void setSeatChargeRef(String seatChargeRef) {
        this.seatChargeRef = seatChargeRef;
    }
}
