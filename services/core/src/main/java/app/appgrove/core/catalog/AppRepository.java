package app.appgrove.core.catalog;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository del catalogo app (platform-level, non tenant-scoped). */
@ApplicationScoped
public class AppRepository implements PanacheRepositoryBase<App, UUID> {

    public Optional<App> findBySlug(String slug) {
        return find("slug", slug).firstResultOptional();
    }

    /**
     * Le <b>applicazioni</b> del catalogo: tutte le righe tranne le voci di piattaforma (UC 0103).
     *
     * <p>È la lettura che ogni superficie del cliente deve usare al posto di «tutte le righe»: i diritti
     * d'accesso, la vetrina, «dove posso entrare», le applicazioni per persona. Sta qui, e non come
     * condizione ripetuta in cinque punti, perché una condizione in cinque copie diverge sempre — e la
     * copia dimenticata si manifesta come una voce che non è una applicazione nel menu laterale di un
     * cliente.
     *
     * <p>Chi ha bisogno anche delle voci di piattaforma — solo la console di amministrazione, che le mostra
     * <b>marcate</b> — continua a usare {@code listAll()}, e lo dichiara nel proprio commento.
     */
    public List<App> listApplications() {
        return list("kind", AppKind.application);
    }
}
