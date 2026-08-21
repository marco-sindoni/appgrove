package app.appgrove.core.platform;

import app.appgrove.commons.access.AppRole;

/**
 * Regola <b>unica</b> di chi-può-cosa sugli accessi per applicazione (UC 0098 §8), sul modello di
 * {@code EntitlementAccess}: classe senza stato e <b>senza accesso alla banca dati</b>. I chiamanti
 * raccolgono gli ingredienti — il ruolo di piattaforma del chiamante e il suo eventuale ruolo
 * <i>su quella applicazione</i> — e chiedono qui il verdetto.
 *
 * <p>È il punto in cui la regola resta <b>una</b> invece di essere ripetuta in ogni operazione, e per
 * questo è anche la classe con il collaudo più importante della storia.
 *
 * <p><b>Perché il ruolo di applicazione arriva come parametro e non dal token.</b> Il token porta solo
 * il ruolo di piattaforma (UC 0099): i ruoli per applicazione non ci stanno — un cambio avrebbe effetto
 * solo al rinnovo, e un account con dieci applicazioni gonfierebbe ogni richiesta. Il ruolo si legge
 * quindi dal modello, dentro la stessa transazione dell'operazione.
 */
public final class AppAccessRules {

    private AppAccessRules() {}

    /**
     * Chi può <b>concedere</b>, <b>cambiare ruolo</b> e <b>revocare</b> su una applicazione: l'owner su
     * tutto; l'{@code admin} <b>di quella</b> applicazione (il suo potere è circoscritto: sulle altre
     * applicazioni il suo ruolo è assente e la risposta è no). Un {@code editor} e un {@code viewer} non
     * scrivono; una persona senza accesso nemmeno.
     *
     * @param platformRole ruolo di piattaforma del chiamante ({@code owner} o {@code member})
     * @param callerRoleOnApp ruolo del chiamante <b>su quella</b> applicazione, {@code null} se nessuno
     */
    public static boolean canManage(MembershipRole platformRole, AppRole callerRoleOnApp) {
        if (platformRole == MembershipRole.owner) {
            return true;
        }
        return callerRoleOnApp == AppRole.admin;
    }

    /**
     * Chi può <b>leggere</b> l'elenco di chi ha accesso a una applicazione: l'owner, e chiunque abbia un
     * accesso a quella applicazione — anche {@code viewer} (UC 0098 §2: «leggono chi ha accesso; non
     * scrivono»). Chi non ha accesso all'applicazione non ne conosce nemmeno le persone.
     */
    public static boolean canRead(MembershipRole platformRole, AppRole callerRoleOnApp) {
        return platformRole == MembershipRole.owner || callerRoleOnApp != null;
    }
}
