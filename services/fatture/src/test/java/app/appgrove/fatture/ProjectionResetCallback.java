package app.appgrove.fatture;

import io.quarkus.arc.Arc;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

/**
 * Azzera le <b>copie locali</b> — diritti d'accesso (UC 0046) e ruolo per applicazione (UC 0099) —
 * prima di ogni test.
 *
 * <p>Serve perché la proiezione è una cache <b>su tabella</b>, non in memoria: sopravvive alla fine
 * di un test e persino al ripristino di {@code MockEntitlementService}. Senza questo azzeramento un
 * test che revoca l'accesso lascia in proiezione un diniego fresco, e il test successivo — che si
 * aspetta accesso concesso — fallisce leggendo il valore del predecessore. È lo stesso motivo per
 * cui non basta ripristinare il finto servizio: il finto servizio non viene nemmeno interpellato,
 * ed è precisamente il comportamento che la proiezione deve avere.
 *
 * <p>La regola vive qui, in un punto solo, invece di essere ripetuta in ogni classe di test che
 * tocca i diritti: dimenticarla in una classe futura produrrebbe un fallimento intermittente e
 * difficile da leggere.
 *
 * <p>Registrato via {@code META-INF/services/io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback}.
 */
public class ProjectionResetCallback implements QuarkusTestBeforeEachCallback {

    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        if (Arc.container() == null) {
            return; // test senza contenitore CDI attivo (es. unit puri)
        }
        var handle = Arc.container().instance(TestProjection.class);
        if (handle.isAvailable()) {
            handle.get().clear();
        }
        // Il finto servizio del ruolo è stato in memoria: ripristinarlo qui evita che un test che
        // abbassa il ruolo o simula il core giù faccia fallire quello successivo per il motivo
        // sbagliato — la stessa trappola della copia su tabella, ma dall'altro lato.
        MockAppRoleService.reset();
    }
}
