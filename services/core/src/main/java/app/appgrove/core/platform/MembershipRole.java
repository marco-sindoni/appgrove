package app.appgrove.core.platform;

/**
 * Ruolo di una persona dentro un account (UC 0116: ruolo dell'<b>appartenenza</b>, non dell'utente —
 * la stessa persona può avere ruoli diversi in account diversi). Persistito come stringa.
 * Il ritiro del valore {@code admin} in favore di due soli ruoli è di UC 0098/0113.
 */
public enum MembershipRole {
    owner,
    admin,
    member
}
