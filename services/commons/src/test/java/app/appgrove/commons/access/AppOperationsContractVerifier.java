package app.appgrove.commons.access;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Il collaudo che rende <b>vero</b> il contratto dei tre ruoli (UC 0101 §8): confronta il documento delle
 * operazioni di una applicazione ({@link AppOperationsContract}) con le operazioni effettivamente esposte
 * dal suo codice. Sta nel <i>test-jar</i> di {@code commons} e viene invocato da un test di una riga in
 * ogni servizio, come {@code PersonalDataManifestVerifier} per il manifesto dati (UC 0030).
 *
 * <p>Verifica <b>tre</b> direzioni, e la seconda è quella che conta:
 *
 * <ol>
 *   <li><b>dichiarato → reale</b>: ogni operazione del documento esiste come metodo di una risorsa
 *       {@code @Path} e porta un verbo HTTP. Coglie il metodo rinominato o rimosso, che lascerebbe il
 *       documento a parlare di qualcosa che non c'è più;</li>
 *   <li><b>reale → dichiarato</b>: ogni operazione esposta dall'applicazione — <b>letture comprese</b> — è
 *       dichiarata nel documento. È la direzione che coglie l'operazione aggiunta domani da qualcuno che si
 *       dimentica il varco: senza di essa il documento resterebbe verde per sempre, perché nulla lo obbliga
 *       a essere completo;</li>
 *   <li><b>coerenza col varco</b>: il ruolo dichiarato coincide con quello effettivo di
 *       {@link RequiresAppRole} (vince il metodo sulla classe, come nel varco); ogni operazione
 *       <b>dispositiva</b> non esente richiede almeno {@link AppRole#editor}; ogni operazione
 *       <b>esente</b> non porta l'annotazione del varco.</li>
 * </ol>
 *
 * <p><b>Il limite, dichiarato invece di nascosto.</b> Un'operazione di scrittura può sottrarsi al ruolo
 * minimo dichiarandosi <i>esente</i>: è inevitabile, perché esistono scritture legittimamente esenti (la
 * cancellazione dei propri dati personali). Quello che il collaudo garantisce è che sottrarsi sia un
 * <b>atto deliberato e scritto</b> — una riga nel documento, con il motivo, visibile in revisione — invece
 * di una annotazione dimenticata, che è il difetto reale e frequente.
 *
 * <p><b>Perché l'auto-collaudo esiste</b> ({@code AppOperationsContractVerifierTest}): un verificatore che
 * non ha mai fallito non è una prova. Là si dimostra che una scrittura non protetta, una scrittura con solo
 * {@code viewer}, un'operazione non dichiarata e un'esenzione protetta diventano rosse davvero.
 */
public final class AppOperationsContractVerifier {

    /** I verbi che cambiano lo stato: sono le operazioni <i>dispositive</i> della cascata (UC 0101 §4.1). */
    private static final Set<String> WRITE_VERBS =
            Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);

    private AppOperationsContractVerifier() {}

    /**
     * Verifica il contratto contro le risorse JAX-RS trovate nei package indicati (le classi di
     * <b>produzione</b>: le risorse-sonda dei test non sono operazioni dell'applicazione).
     */
    public static void verify(AppOperationsContract contract, String... packages) {
        verify(contract, exposedResources(packages));
    }

    /**
     * Variante con le risorse indicate a mano: la usa l'auto-collaudo del verificatore, le cui
     * risorse-campione vivono per forza di cose fra le classi di test.
     */
    public static void verify(AppOperationsContract contract, Collection<Class<?>> resources) {
        List<String> violations = new ArrayList<>();

        if (contract.appId() == null || contract.appId().isBlank()) {
            violations.add("il contratto non dichiara l'app_id");
        }

        // ── Indice del dichiarato, e i suoi doppioni ─────────────────────────
        Map<String, AppOperation> declared = new LinkedHashMap<>();
        Set<String> ids = new HashSet<>();
        for (AppOperation operation : contract.operations()) {
            if (!ids.add(operation.id())) {
                violations.add("identificativo duplicato nel documento: " + operation.id()
                        + " — gli identificativi sono stabili e servono a riferirsi a UNA operazione");
            }
            AppOperation previous = declared.put(key(operation.resource(), operation.javaMethod()), operation);
            if (previous != null) {
                violations.add("lo stesso metodo è dichiarato due volte: "
                        + key(operation.resource(), operation.javaMethod())
                        + " (" + previous.id() + " e " + operation.id() + ")");
            }
        }

        // ── Direzione 1 e 3: dichiarato → reale, e coerenza col varco ────────
        for (AppOperation operation : contract.operations()) {
            Class<?> resource = operation.resource();
            if (!resource.isAnnotationPresent(Path.class)) {
                violations.add(operation.id() + ": " + resource.getSimpleName()
                        + " non è una risorsa @Path, quindi non espone operazioni");
                continue;
            }
            List<Method> candidates = methodsNamed(resource, operation.javaMethod());
            if (candidates.isEmpty()) {
                violations.add(operation.id() + ": il metodo " + resource.getSimpleName() + "#"
                        + operation.javaMethod() + " non esiste (rimosso o rinominato?)");
                continue;
            }
            if (candidates.size() > 1) {
                violations.add(operation.id() + ": " + resource.getSimpleName() + "#" + operation.javaMethod()
                        + " è ambiguo (più metodi con lo stesso nome portano un verbo HTTP)");
                continue;
            }
            Method method = candidates.get(0);
            String verb = httpVerb(method);
            if (verb == null) {
                violations.add(operation.id() + ": " + resource.getSimpleName() + "#" + operation.javaMethod()
                        + " non porta un verbo HTTP, quindi non è un'operazione esposta");
                continue;
            }

            RequiresAppRole gate = effectiveGate(resource, method);
            if (operation.exemptFromRoles()) {
                if (gate != null) {
                    violations.add(operation.id() + ": dichiarata ESENTE dai ruoli («" + operation.exemptionReason()
                            + "») ma protetta dal varco con @RequiresAppRole(" + gate.value()
                            + "). Un'esenzione protetta è un diritto rotto: si toglie l'annotazione, oppure"
                            + " l'operazione non è esente");
                }
            } else if (gate == null) {
                violations.add(operation.id() + ": dichiara ruolo minimo " + operation.minimumRole()
                        + " ma il metodo non è protetto — manca @RequiresAppRole sul metodo o sulla classe");
            } else if (gate.value() != operation.minimumRole()) {
                violations.add(operation.id() + ": il documento dichiara " + operation.minimumRole()
                        + ", il varco applica " + gate.value() + ". Due verità sullo stesso potere");
            }

            if (WRITE_VERBS.contains(verb)
                    && !operation.exemptFromRoles()
                    && !operation.minimumRole().atLeast(AppRole.editor)) {
                violations.add(operation.id() + ": operazione DISPOSITIVA (" + verb + ") con ruolo minimo "
                        + operation.minimumRole() + ". La cascata di UC 0101 §4 non si negozia caso per caso:"
                        + " chi cambia dati è almeno editor");
            }
        }

        // ── Direzione 2: reale → dichiarato ──────────────────────────────────
        for (Class<?> resource : resources) {
            for (Method method : resource.getDeclaredMethods()) {
                String verb = httpVerb(method);
                if (verb == null || !Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                if (!declared.containsKey(key(resource, method.getName()))) {
                    violations.add("operazione ESPOSTA e non dichiarata: " + resource.getSimpleName() + "#"
                            + method.getName() + " (" + verb + "). Ogni operazione va nel documento delle"
                            + " operazioni, letture comprese, col suo ruolo minimo o col motivo"
                            + " dell'esenzione");
                }
            }
        }

        if (!violations.isEmpty()) {
            throw new AssertionError("Documento delle operazioni e codice NON allineati (UC 0101), app_id="
                    + contract.appId() + ":\n  - " + String.join("\n  - ", violations));
        }
    }

    /**
     * Le risorse JAX-RS di produzione dei package indicati. Le interfacce sono escluse di proposito: in
     * {@code commons} i {@code @Path} su interfaccia sono i <b>client</b> REST verso il core
     * ({@code @RegisterRestClient}), che non espongono niente.
     */
    private static Collection<Class<?>> exposedResources(String... packages) {
        Set<Class<?>> out = new LinkedHashSet<>();
        for (JavaClass javaClass : new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(packages)) {
            if (!javaClass.isAnnotatedWith(Path.class)) {
                continue;
            }
            Class<?> reflected = javaClass.reflect();
            if (reflected.isInterface()) {
                continue;
            }
            out.add(reflected);
        }
        return out;
    }

    /** I metodi con quel nome che portano un verbo HTTP (gli helper privati omonimi non contano). */
    private static List<Method> methodsNamed(Class<?> resource, String name) {
        List<Method> out = new ArrayList<>();
        for (Method method : resource.getDeclaredMethods()) {
            if (method.getName().equals(name) && httpVerb(method) != null) {
                out.add(method);
            }
        }
        if (out.isEmpty()) {
            // Nessun verbo: si riporta comunque il metodo, così il messaggio dice «non porta un verbo HTTP»
            // invece del più vago «non esiste».
            for (Method method : resource.getDeclaredMethods()) {
                if (method.getName().equals(name)) {
                    out.add(method);
                }
            }
        }
        return out;
    }

    /**
     * Il verbo HTTP del metodo, letto dalla meta-annotazione {@link HttpMethod} che marca {@code @GET},
     * {@code @POST} e compagnia: così un verbo nuovo o personalizzato è riconosciuto senza toccare questo
     * elenco.
     */
    private static String httpVerb(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            HttpMethod verb = annotation.annotationType().getAnnotation(HttpMethod.class);
            if (verb != null) {
                return verb.value();
            }
        }
        return null;
    }

    /** L'annotazione che vale davvero: <b>vince il metodo</b> sulla classe, come in {@code AppRoleGateFilter}. */
    private static RequiresAppRole effectiveGate(Class<?> resource, Method method) {
        RequiresAppRole onMethod = method.getAnnotation(RequiresAppRole.class);
        return onMethod != null ? onMethod : resource.getAnnotation(RequiresAppRole.class);
    }

    private static String key(Class<?> resource, String method) {
        return resource.getName() + "#" + method;
    }
}
