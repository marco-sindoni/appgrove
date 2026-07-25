package app.appgrove.core.newsletter;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Limite di frequenza in memoria per l'endpoint pubblico di iscrizione (senza JWT). Finestra
 * scorrevole per indirizzo IP: l'IP è usato SOLO transitoriamente per il conteggio e non è mai
 * persistito. Difesa minima insieme al campo esca del form e al double opt-in (nessun marketing
 * verso indirizzi non confermati). Un captcha di terze parti è escluso (postura cookieless/UE) e
 * tracciato come punto aperto.
 *
 * <p>Best-effort: lo stato vive nella singola istanza (nel cloud più task non lo condividono).
 * Con una sola richiesta pubblica a bassa frequenza è sufficiente; un limite distribuito è di là
 * da venire e non serve al volume atteso.
 */
@ApplicationScoped
public class SubscribeRateLimiter {

    @ConfigProperty(name = "appgrove.newsletter.rate-limit.max", defaultValue = "5")
    int max;

    @ConfigProperty(name = "appgrove.newsletter.rate-limit.window", defaultValue = "PT10M")
    Duration window;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** {@code true} se la richiesta da {@code ip} rientra nella finestra; ne registra il colpo. */
    public boolean tryAcquire(String ip, long nowEpochMs) {
        if (ip == null || ip.isBlank()) {
            ip = "unknown";
        }
        long windowStart = nowEpochMs - window.toMillis();
        Deque<Long> deque = hits.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst() < windowStart) {
                deque.pollFirst();
            }
            if (deque.size() >= max) {
                return false;
            }
            deque.addLast(nowEpochMs);
            return true;
        }
    }
}
