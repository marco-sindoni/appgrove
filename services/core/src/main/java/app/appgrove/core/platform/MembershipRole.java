package app.appgrove.core.platform;

/**
 * Ruolo di una persona dentro un account (UC 0116). Dopo UC 0098 i valori sono <b>due</b>:
 *
 * <ul>
 *   <li>{@code owner} — chi possiede l'account: unico, non rimovibile, non retrocedibile, con accesso
 *       <b>implicito</b> a tutte le applicazioni dell'account;</li>
 *   <li>{@code member} — tutte le altre persone dell'account. Che cosa possono <i>fare</i> non lo dice
 *       questo ruolo: lo dice il ruolo su ciascuna applicazione ({@code AppRole} in commons, {@code platform.app_access}).</li>
 * </ul>
 *
 * <p><b>Il valore {@code admin} è stato ritirato da questo livello</b> (UC 0098): era un potere che
 * valeva per tutto — ogni applicazione e anche le schermate di piattaforma — e riappare, molto più
 * circoscritto, come {@code AppRole.admin} su <i>una</i> applicazione. La conversione delle righe
 * esistenti ({@code admin} → {@code member} più accesso {@code admin} su ogni applicazione dell'account)
 * è di UC 0113, con il vincolo di controllo che la sigilla.
 *
 * <p>Persistito come stringa. Il ruolo di piattaforma è anche l'unico ruolo che finisce nel claim del
 * token (UC 0099).
 */
public enum MembershipRole {
    owner,
    member
}
