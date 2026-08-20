package app.appgrove.core.platform;

/**
 * Ciclo di vita di un invito. Persistito come stringa.
 *
 * <p>{@code revoked} e {@code rejected} sono due atti <b>diversi</b>, di due soggetti diversi:
 * l'invito revocato l'ha chiuso chi ha invitato, l'invito rifiutato l'ha chiuso la persona invitata
 * (UC 0118 §6). Confonderli renderebbe illeggibile la storia dell'invito — e il posto liberato è lo
 * stesso, ma la ragione per cui si è liberato no.
 */
public enum InvitationStatus {
    pending,
    accepted,
    revoked,
    expired,
    rejected
}
