package app.appgrove.core.billing.seats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.agroal.api.AgroalDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Crea la <b>prima</b> versione del listino dei posti dal file di risorse, se non esiste (UC 0102 §7).
 *
 * <p>Sul modello di {@code PricingCatalogLoader}/{@code PricingSyncService}, con una differenza sostanziale
 * che vale dichiarare: il listino delle applicazioni si <b>sincronizza</b> dai file a ogni avvio (il file è
 * la verità), questo si <b>semina</b> una volta sola (il file è il valore iniziale, poi la verità è la banca
 * dati). Sincronizzare anche questo vorrebbe dire che al riavvio successivo a un cambio di tariffa da
 * console il listino tornerebbe indietro: cioè un cambio di prezzo annullato da un riavvio.
 *
 * <p><b>Idempotente per costruzione</b>: la condizione «non esiste alcuna versione» sta <b>dentro</b>
 * l'inserzione ({@code where not exists}), quindi due avvii — o due istanze contemporanee — non possono
 * creare due listini, e non serve leggere prima per decidere poi. La condizione è «esiste una versione», non
 * «esiste questa versione», ed è deliberata: dopo il primo cambio da console la versione del file non è più
 * la vigente, e cercarla per riconoscerla la farebbe rinascere.
 *
 * <p><b>Scritture in SQL nativo</b>, non via JPA, e non è una scorciatoia: il caricamento gira all'avvio,
 * fuori da una richiesta autenticata, dove il risolutore del tenant è fail-closed e non esiste sessione
 * Hibernate da aprire. È lo stesso motivo — e lo stesso schema — di {@code PricingSyncService}. Gli
 * identificativi sono <b>deterministici</b> dalla decorrenza, così l'inserzione è ripetibile.
 *
 * <p>Un file <b>incoerente</b> fa fallire l'avvio ({@link IncoherentSeatPricingException}): un servizio che
 * non parte è preferibile a un servizio che fattura su un listino con un buco.
 */
@ApplicationScoped
public class SeatPricingLoader {

    static final String RESOURCE = "pricing/seats.yaml";

    /** Autore delle righe seminate: distingue a vista il listino iniziale da quelli creati da una persona. */
    static final String AUTHOR = "seat-pricing-loader";

    private static final Logger LOG = Logger.getLogger(SeatPricingLoader.class);

    private static final String INSERT_VERSION =
            """
            insert into platform.seat_pricing_version
              (id, effective_from, currency, note, created_at, updated_at, created_by)
            select ?, ?, ?, ?, now(), now(), ?
            where not exists (
              select 1 from platform.seat_pricing_version where deleted_at is null
            )
            """;

    private static final String INSERT_BAND =
            """
            insert into platform.seat_pricing_band
              (id, version_id, from_seat, to_seat, unit_price_cents, created_at, updated_at, created_by)
            values (?, ?, ?, ?, ?, now(), now(), ?)
            on conflict (id) do nothing
            """;

    @Inject
    ObjectMapper json;

    @Inject
    AgroalDataSource ds;

    private ObjectMapper yaml;

    @PostConstruct
    void init() {
        // Riusa l'ObjectMapper configurato da Quarkus (binding dei record per nome parametro, tipi di
        // data/ora), ricreato su YAMLFactory — come PricingCatalogLoader.
        yaml = json.copyWith(new YAMLFactory());
    }

    /**
     * Crea la prima versione dal file, se non esiste già una versione qualunque.
     *
     * @return {@code true} se la versione è stata creata adesso, {@code false} se c'era già (nessuna
     *     scrittura)
     */
    @Transactional
    public boolean ensureInitialVersion() {
        SeatPricingDefinition def = read();
        // La coerenza si controlla PRIMA di scrivere: un listino con un buco non deve nemmeno entrare.
        SeatPricing.requireCoherent(def.asVersion());
        UUID versionId = versionId(def.effectiveFrom().toString());
        try (Connection c = ds.getConnection()) {
            if (insertVersion(c, versionId, def) == 0) {
                return false;
            }
            for (SeatPricingDefinition.BandDef band : def.bands()) {
                insertBand(c, versionId, band, def.effectiveFrom().toString());
            }
        } catch (SQLException e) {
            throw new IllegalStateException("semina del listino dei posti fallita", e);
        }
        LOG.infof(
                "seat-pricing.seed created effective_from=%s currency=%s bands=%d",
                def.effectiveFrom(), def.currency(), def.bands().size());
        return true;
    }

    /** Il listino iniziale come sta scritto nel file. Visibile perché il file stesso ha i suoi collaudi. */
    public SeatPricingDefinition read() {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("risorsa del listino dei posti mancante: " + RESOURCE);
            }
            SeatPricingDefinition def = yaml.readValue(in, SeatPricingDefinition.class);
            if (def == null || def.bands() == null || def.bands().isEmpty()) {
                throw new IncoherentSeatPricingException(RESOURCE + " senza fasce");
            }
            if (def.effectiveFrom() == null || def.currency() == null || def.currency().isBlank()) {
                throw new IncoherentSeatPricingException(
                        RESOURCE + " senza decorrenza o senza valuta: entrambe servono a dire quale listino"
                                + " vigeva quel giorno e in che moneta");
            }
            return def;
        } catch (IOException e) {
            throw new UncheckedIOException("lettura del listino dei posti fallita: " + RESOURCE, e);
        }
    }

    private int insertVersion(Connection c, UUID versionId, SeatPricingDefinition def) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(INSERT_VERSION)) {
            ps.setObject(1, versionId);
            ps.setTimestamp(2, Timestamp.from(def.effectiveFrom()));
            ps.setString(3, def.currency());
            ps.setString(4, def.note());
            ps.setString(5, AUTHOR);
            return ps.executeUpdate();
        }
    }

    private void insertBand(
            Connection c, UUID versionId, SeatPricingDefinition.BandDef band, String versionKey)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(INSERT_BAND)) {
            ps.setObject(1, bandId(versionKey, band.fromSeat()));
            ps.setObject(2, versionId);
            ps.setInt(3, band.fromSeat());
            if (band.toSeat() == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, band.toSeat());
            }
            ps.setInt(5, band.unitPriceCents());
            ps.setString(6, AUTHOR);
            ps.executeUpdate();
        }
    }

    /**
     * Identificativi <b>deterministici</b> dalla decorrenza, con lo stesso algoritmo del catalogo
     * ({@code CatalogIds}): la semina è ripetibile e le fasce si attaccano alla loro versione anche se
     * l'inserzione viene ritentata.
     */
    static UUID versionId(String effectiveFrom) {
        return det("seat-pricing-version:" + effectiveFrom);
    }

    static UUID bandId(String effectiveFrom, int fromSeat) {
        return det("seat-pricing-band:" + effectiveFrom + ":" + fromSeat);
    }

    private static UUID det(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }
}
