package app.appgrove.commons.membership;

/**
 * Il <b>ruolo di piattaforma</b> come finisce nel claim del token (UC 0099). Dopo UC 0098 i valori
 * dell'appartenenza sono due — {@code owner} e {@code member} — e questa classe è il punto in cui quella
 * riduzione diventa vera <b>nel token</b>: il claim dei ruoli non porta più {@code admin}.
 *
 * <p><b>La tolleranza, e perché sta qui.</b> Un ambiente i cui dati non sono ancora stati convertiti può
 * avere appartenenze che valgono ancora {@code admin} (la conversione è di UC 0113). Quel valore viene
 * <b>letto come {@code member}</b> nel momento in cui si scrive il claim: convertirlo qui, e non nel
 * modello, significa che una persona di un ambiente non convertito accede con il potere <b>minore</b>
 * invece che con <b>nessun</b> potere — che è il comportamento giusto, perché il valore vecchio significava
 * «membro con poteri in più», non «persona sconosciuta».
 *
 * <p><b>Da togliere con UC 0113</b>, insieme alla conversione dei dati reali e al ritiro della tolleranza
 * dei token già emessi ({@code Roles.ADMIN} in core): a quel punto nessuna riga vale più {@code admin} e
 * questa normalizzazione diventa codice morto. Fino a quel giorno toglierla chiuderebbe fuori qualcuno.
 *
 * <p><b>Parità con la funzione che compone il token in cloud</b>
 * ({@code infra/modules/platform_shared/lambda/pre_token_gen/handler.py}, funzione {@code _roles_for}):
 * quella gira dentro l'infrastruttura e non può chiamare Java, quindi la regola è attuata <b>due volte</b>.
 * È lo stesso debito, e lo stesso presidio, della scelta dell'account attivo ({@link ActiveAccount}): la
 * stessa tabella di casi eseguita dai collaudi di entrambe. Se una cambia, l'altra cambia con essa —
 * altrimenti i collaudi locali dicono una cosa e l'ambiente reale un'altra.
 */
public final class PlatformRoles {

    /** Chi possiede l'account: accesso implicito a tutte le sue applicazioni. */
    public static final String OWNER = "owner";

    /** Tutte le altre persone dell'account: cosa possono fare lo dice il ruolo su ciascuna applicazione. */
    public static final String MEMBER = "member";

    /** Chi amministra la piattaforma: non è un ruolo di appartenenza e non dà accesso ai dati dei clienti. */
    public static final String PLATFORM_ADMIN = "platform-admin";

    /** Valore ritirato da UC 0098: non è più un ruolo di appartenenza. */
    private static final String RETIRED_ADMIN = "admin";

    private PlatformRoles() {}

    /**
     * Il ruolo di piattaforma come va scritto nel claim del token: {@code admin} diventa {@code member},
     * ogni altro valore passa inalterato.
     *
     * <p>Un valore nullo resta nullo: non è compito di questa funzione decidere se un token si possa
     * emettere — quella decisione, a chiusura in caso di dubbio, è di chi compone il token.
     */
    public static String claimRole(String membershipRole) {
        return RETIRED_ADMIN.equals(membershipRole) ? MEMBER : membershipRole;
    }
}
