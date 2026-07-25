package app.appgrove.core.newsletter;

/**
 * Canale attraverso cui è stato raccolto (o revocato) il consenso — parte della prova ex art. 7.
 * {@code site} = subscribe box del sito vetrina; {@code signup} = checkbox alla registrazione;
 * {@code account} = toggle nelle impostazioni account (utente autenticato); {@code email} = azione
 * partita da un collegamento nell'email (conferma double opt-in o disiscrizione one-click).
 */
public enum ConsentChannel {
    site,
    signup,
    account,
    email
}
