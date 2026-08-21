package app.appgrove.commons.access;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

/**
 * Una applicazione in cui la persona che chiama <b>può entrare</b>, con il <b>ruolo</b> che ha su di essa
 * (UC 0099): una voce della risposta di {@code GET /api/platform/v1/me/app-access}.
 *
 * <p>Compaiono solo le applicazioni che hanno <b>insieme</b> il diritto dell'account e l'accesso della
 * persona: la presenza in elenco <b>è</b> il permesso di entrare, come per i diritti d'accesso. Per
 * l'owner dell'account ci sono tutte quelle con diritto, col ruolo massimo.
 *
 * <p>Il contratto vive in {@code commons} perché ha <b>due</b> consumatori che non si vedono fra loro: il
 * core che lo produce e ogni servizio di applicazione che lo legge attraverso il varco condiviso (e domani
 * il menu laterale, UC 0107). Scriverlo due volte significherebbe farlo divergere.
 *
 * @param appId identificativo di catalogo dell'applicazione
 * @param appSlug slug stabile dell'applicazione: è la chiave con cui ogni servizio riconosce sé stesso
 * @param appName nome leggibile, perché il consumatore dichiarato è un menu e deve scrivere un'etichetta
 *     senza fare una seconda chiamata al catalogo
 * @param role ruolo della persona su quella applicazione ({@code viewer}, {@code editor}, {@code admin})
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyAppAccessView(UUID appId, String appSlug, String appName, String role) {}
