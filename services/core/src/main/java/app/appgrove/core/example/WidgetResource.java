package app.appgrove.core.example;

import app.appgrove.core.example.WidgetDtos.CreateWidget;
import app.appgrove.core.example.WidgetDtos.WidgetView;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * HARNESS REST (UC 0012) per esercitare il multitenancy end-to-end (JWT → TenantResolver → discriminator → DB).
 * {@code @Authenticated}: serve un JWT valido. Il tenant è sempre quello del token; il body non lo influenza.
 * Rimovibile con UC 0013.
 */
@Path("/api/_demo/widgets")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WidgetResource {

    @Inject
    WidgetRepository repository;

    @POST
    @Transactional
    public WidgetView create(@Valid CreateWidget body) {
        Widget widget = new Widget(body.name());
        repository.persist(widget);
        repository.flush(); // forza l'INSERT: id e tenant_id (da @TenantId) valorizzati prima della response
        return WidgetView.from(widget);
    }

    @GET
    public List<WidgetView> list() {
        return repository.listAll().stream().map(WidgetView::from).toList();
    }
}
