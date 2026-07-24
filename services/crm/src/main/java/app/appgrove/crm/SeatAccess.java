package app.appgrove.crm;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

/**
 * Varco d'accesso al dominio del mini-CRM basato sul <b>possesso di un posto</b> (UC 0054). Essere
 * membro dell'account non basta: per vedere o modificare contatti e interazioni serve un posto
 * assegnato. Chi non ce l'ha riceve <b>403</b> con una spiegazione, anche se il suo ruolo lo
 * autorizzerebbe (il ruolo dice <i>cosa</i> potrebbe fare, il posto dice <i>se</i> ha accesso).
 *
 * <p>La gestione dei posti ({@link SeatResource}) è volutamente <b>fuori</b> da questo varco: la fa
 * owner/admin in base al ruolo, altrimenti un account a posti esauriti non potrebbe più liberarne
 * nessuno — una trappola senza uscita.
 */
@RequestScoped
public class SeatAccess {

    @Inject
    SeatRepository seats;

    @Inject
    CallerContext caller;

    /** Consente di proseguire solo se il chiamante ha un posto attivo; altrimenti 403. */
    public void requireActiveSeat() {
        if (!seats.hasSeat(caller.subject())) {
            throw new ForbiddenException(
                    "Nessun posto assegnato in Mini-CRM per questo utente: chiedi a un titolare o"
                            + " amministratore dell'account di assegnartene uno.");
        }
    }
}
