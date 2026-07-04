package app.appgrove.core.gdpr;

/** Tipo di export job (#13 D22): tutto l'account (piattaforma + ogni app attivata) o una singola app. */
public enum GdprExportKind {
    account,
    app
}
