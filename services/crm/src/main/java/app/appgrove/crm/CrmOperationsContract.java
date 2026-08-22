package app.appgrove.crm;

import app.appgrove.commons.access.AppOperation;
import app.appgrove.commons.access.AppOperationsContract;
import app.appgrove.commons.access.AppRole;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Documento delle operazioni dell'app <b>Mini-CRM</b> (UC 0101): che cosa l'applicazione espone e quanto
 * potere serve per ognuna. La classificazione segue la cascata del contratto di piattaforma
 * ({@link AppOperationsContract}) e la coerenza con il codice è verificata da
 * {@code AppOperationsContractTest}.
 *
 * <p>È l'applicazione che esercita tutte e tre le righe della cascata: letture di dominio
 * ({@code viewer}), scritture di dominio ({@code editor}) e — nel riquadro dei <b>posti</b> — il governo
 * di <i>chi</i> usa l'applicazione ({@code admin}). Il riepilogo dei posti resta {@code viewer} perché
 * la storia §6 vuole le sezioni di governo visibili in sola lettura.
 */
@ApplicationScoped
public class CrmOperationsContract implements AppOperationsContract {

    @Override
    public String appId() {
        return CrmDataContract.APP_ID;
    }

    @Override
    public List<AppOperation> operations() {
        return List.of(
                // ── Dominio: letture (cascata §4.3) ──────────────────────────
                AppOperation.requiring(
                        "contacts.list",
                        "Elenca e cerca i contatti",
                        "List and search the contacts",
                        ContactResource.class,
                        "list",
                        AppRole.viewer),
                AppOperation.requiring(
                        "contacts.get",
                        "Apre il dettaglio di un contatto",
                        "Open the detail of one contact",
                        ContactResource.class,
                        "get",
                        AppRole.viewer),
                AppOperation.requiring(
                        "interactions.list",
                        "Elenca le interazioni di un contatto",
                        "List the interactions of one contact",
                        InteractionResource.class,
                        "list",
                        AppRole.viewer),

                // ── Dominio: operazioni dispositive (cascata §4.1) ───────────
                AppOperation.requiring(
                        "contacts.create",
                        "Crea un contatto (consuma un posto se la persona è nuova)",
                        "Create a contact",
                        ContactResource.class,
                        "create",
                        AppRole.editor),
                AppOperation.requiring(
                        "contacts.update",
                        "Modifica un contatto, incluso il suo stadio",
                        "Update a contact, including its stage",
                        ContactResource.class,
                        "update",
                        AppRole.editor),
                AppOperation.requiring(
                        "contacts.delete",
                        "Cancella un contatto",
                        "Delete a contact",
                        ContactResource.class,
                        "delete",
                        AppRole.editor),
                AppOperation.requiring(
                        "interactions.create",
                        "Registra un'interazione con un contatto",
                        "Record an interaction with a contact",
                        InteractionResource.class,
                        "create",
                        AppRole.editor),

                // ── Governo degli accessi all'applicazione (cascata §4.2) ────
                AppOperation.requiring(
                        "seats.summary",
                        "Legge chi ha un posto sull'applicazione, e quanti ne restano",
                        "Read who holds a seat on the application, and how many are left",
                        SeatResource.class,
                        "summary",
                        AppRole.viewer),
                AppOperation.requiring(
                        "seats.assign",
                        "Assegna un posto sull'applicazione a una persona dell'account",
                        "Grant a seat on the application to a person of the account",
                        SeatResource.class,
                        "assign",
                        AppRole.admin),
                AppOperation.requiring(
                        "seats.revoke",
                        "Revoca il posto di una persona sull'applicazione",
                        "Revoke a person's seat on the application",
                        SeatResource.class,
                        "revoke",
                        AppRole.admin),

                // ── Esente dai ruoli, di proposito ───────────────────────────
                AppOperation.exempt(
                        "quota.status",
                        "Legge uso e tetto della quota",
                        "Read quota usage and cap",
                        QuotaResource.class,
                        "status",
                        "Stato di quota informativo: resta raggiungibile anche da chi non ha ancora un ruolo"
                                + " sull'applicazione, così che il banner del consumo non diventi un rifiuto"
                                + " (scelta già assunta in UC 0099, dove il varco è volutamente opt-in)"));
    }
}
