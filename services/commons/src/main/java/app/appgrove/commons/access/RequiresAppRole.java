package app.appgrove.commons.access;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Dichiara il <b>ruolo minimo</b> che un'operazione richiede alla persona <i>su questa applicazione</i>
 * (UC 0099). È il varco riusabile che ogni servizio usa <b>invece</b> di scriversi la propria logica di
 * autorizzazione: l'operazione dice quanto potere serve, {@link AppRoleGateFilter} decide.
 *
 * <p>Prima di questa storia ogni applicazione se lo rifaceva a modo suo — il Mini-CRM si era costruito la
 * propria tabella dei posti e il proprio varco — ed è il difetto che l'epica 22 esiste per chiudere: le
 * differenze fra dieci varchi scritti a mano si scoprono a incidente avvenuto. Qui il confronto fra ruoli
 * esiste in <b>un</b> posto ({@link AppRole#atLeast}) e nessuna applicazione lo riscrive; chi lo
 * riscrivesse viene colto dal collaudo di parità (UC 0112).
 *
 * <p><b>Opt-in di proposito</b>, come {@code @RequiresEntitlement}: solo le operazioni annotate passano
 * dal varco. Le operazioni che devono restare raggiungibili anche a chi non ha un ruolo — i diritti sui
 * propri dati personali, lo stato di quota informativo — restano tali <b>per costruzione</b>, senza liste
 * di esclusione da tenere aggiornate.
 *
 * <p>Sull'intera risorsa o sul singolo metodo. Se ci sono entrambe, <b>vince il metodo</b>: è la
 * dichiarazione più vicina all'operazione, e permette a una risorsa di chiedere {@code viewer} per le
 * letture e {@code editor} per le scritture senza spezzarsi in due classi.
 */
@NameBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAppRole {

    /** Ruolo minimo richiesto: la persona passa se il suo ruolo è almeno questo. */
    AppRole value();

    /**
     * Rileggere il ruolo dal core <b>saltando la copia locale</b>. Vale solo per le operazioni
     * <b>irreversibili</b> — cancellazioni di massa, cambi di ruolo, revoche (UC 0099 §5): costa una
     * chiamata di rete su pochissime operazioni e chiude la finestra di pochi secondi in cui una revoca
     * appena decisa non è ancora arrivata.
     *
     * <p><b>Non metterlo «per sicurezza».</b> Se lo si mette su tutto, la copia locale diventa inutile e
     * il core torna sul percorso caldo di ogni richiesta di ogni applicazione — che è esattamente la
     * situazione che la copia locale esiste per evitare. La domanda da farsi è una sola: se questa
     * operazione partisse con un ruolo revocato tre secondi fa, si potrebbe tornare indietro?
     */
    boolean fresh() default false;
}
