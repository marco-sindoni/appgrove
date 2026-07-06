package app.appgrove.commons.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.step.StepRegistryConfig;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Bridge Micrometer → CloudWatch <b>Embedded Metric Format</b> (UC 0006, #08 8-9): a ogni step
 * (~60s) ogni meter viene serializzato come una riga JSON EMF scritta <b>direttamente su stdout</b>
 * — con il driver awslogs ogni riga stdout è un evento log e CloudWatch estrae le metriche dal
 * documento EMF radice. NIENTE {@code PutMetricData}, niente logging framework: passando dal
 * logger il documento finirebbe annidato nel campo {@code message} e CloudWatch non lo
 * riconoscerebbe.
 *
 * <p><b>Regola dei due piani (#08 30-31), hard-enforced</b>: solo i tag a bassa cardinalità
 * {@link #ALLOWED_DIMENSIONS} diventano dimensioni; ogni altro tag (in primis
 * {@code tenant_id}/{@code user_id}) viene SCARTATO sia dal meter filter sia in serializzazione.
 * Il tag {@code uri} delle metriche HTTP di Quarkus è rimappato sulla dimensione {@code endpoint}.
 *
 * <p><b>Rappresentazione dei timer</b> (scelta documentata): un {@link Timer} pubblica tre metriche
 * separate {@code <nome>.count} (Count), {@code <nome>.sum} (Millisecondi) e {@code <nome>.max}
 * (Millisecondi) — è la forma EMF più semplice che permette a CloudWatch di ricostruire volumi e
 * latenza media ({@code sum/count}) senza perdere il picco. Stessa forma per le
 * {@link DistributionSummary}.
 */
public class EmfMeterRegistry extends StepMeterRegistry {

    /** Namespace CloudWatch di tutte le metriche di business appgrove. */
    public static final String NAMESPACE = "Appgrove";

    /** Whitelist delle dimensioni ammesse (bassa cardinalità, #08/9): tutto il resto si scarta. */
    public static final Set<String> ALLOWED_DIMENSIONS = Set.of("app_id", "env", "endpoint", "status", "service");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Consumer<String> lineWriter;

    public EmfMeterRegistry(StepRegistryConfig config, Clock clock, Consumer<String> lineWriter) {
        super(config, clock);
        this.lineWriter = lineWriter;
        config().meterFilter(dimensionWhitelist());
    }

    /** Config step-based minimale con lo step indicato (default 60s, configurabile). */
    public static StepRegistryConfig config(Duration step) {
        return new StepRegistryConfig() {
            @Override
            public String prefix() {
                return "appgrove.metrics.emf";
            }

            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public Duration step() {
                return step;
            }
        };
    }

    /**
     * Writer verso lo stdout <b>reale</b> del processo ({@code FileDescriptor.out}): bypassa
     * eventuali redirezioni di {@code System.out} (Quarkus in dev mode) — il documento EMF deve
     * restare l'oggetto radice della riga di log.
     */
    public static Consumer<String> stdoutWriter() {
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        return out::println;
    }

    /**
     * Meter filter della whitelist: tiene solo le dimensioni ammesse, rimappa {@code uri} →
     * {@code endpoint} (metriche {@code http.server.requests} di Quarkus). {@code tenant_id}/
     * {@code user_id} vivono nei log, mai come dimensioni.
     */
    static MeterFilter dimensionWhitelist() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                List<Tag> kept = new ArrayList<>();
                boolean changed = false;
                for (Tag tag : id.getTags()) {
                    String key = "uri".equals(tag.getKey()) ? "endpoint" : tag.getKey();
                    if (ALLOWED_DIMENSIONS.contains(key)) {
                        kept.add(Tag.of(key, tag.getValue()));
                        changed |= !key.equals(tag.getKey());
                    } else {
                        changed = true;
                    }
                }
                return changed ? id.replaceTags(kept) : id;
            }
        };
    }

    // publish() allargato a public: invocabile dai test (flush deterministico, niente attese).
    @Override
    public void publish() {
        long timestamp = clock.wallTime();
        for (Meter meter : getMeters()) {
            // Cost-min: un meter registrato resta per sempre, ma se nello step non ha
            // osservato NULLA non si emette la riga a zero (ingestione CloudWatch pagata
            // per dati nulli, ogni step, per ogni endpoint×status mai visto). I gauge
            // portano stato corrente e si pubblicano sempre.
            if (isIdle(meter)) {
                continue;
            }
            List<MetricValue> values = meter.match(
                    this::of, this::of, this::of, this::of, this::of, this::of, this::of, this::of, this::of);
            String line = toEmfLine(meter.getId(), values, timestamp);
            if (line != null) {
                lineWriter.accept(line);
            }
        }
    }

    /** true se il meter non ha registrato attività nello step corrente (niente righe a zero). */
    private boolean isIdle(Meter meter) {
        return meter.match(
                gauge -> false,
                counter -> counter.count() == 0,
                timer -> timer.count() == 0,
                summary -> summary.count() == 0,
                longTaskTimer -> longTaskTimer.activeTasks() == 0,
                timeGauge -> false,
                functionCounter -> functionCounter.count() == 0,
                functionTimer -> functionTimer.count() == 0,
                other -> false);
    }

    @Override
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    // ── serializzazione per tipo di meter ────────────────────────────────────

    private record MetricValue(String name, String unit, double value) {}

    private List<MetricValue> of(Gauge gauge) {
        return List.of(new MetricValue(gauge.getId().getName(), "None", gauge.value()));
    }

    private List<MetricValue> of(Counter counter) {
        return List.of(new MetricValue(counter.getId().getName(), "Count", counter.count()));
    }

    private List<MetricValue> of(Timer timer) {
        String name = timer.getId().getName();
        return List.of(
                new MetricValue(name + ".count", "Count", timer.count()),
                new MetricValue(name + ".sum", "Milliseconds", timer.totalTime(TimeUnit.MILLISECONDS)),
                new MetricValue(name + ".max", "Milliseconds", timer.max(TimeUnit.MILLISECONDS)));
    }

    private List<MetricValue> of(DistributionSummary summary) {
        String name = summary.getId().getName();
        return List.of(
                new MetricValue(name + ".count", "Count", summary.count()),
                new MetricValue(name + ".sum", "None", summary.totalAmount()),
                new MetricValue(name + ".max", "None", summary.max()));
    }

    private List<MetricValue> of(LongTaskTimer timer) {
        String name = timer.getId().getName();
        return List.of(
                new MetricValue(name + ".active", "Count", timer.activeTasks()),
                new MetricValue(name + ".duration", "Milliseconds", timer.duration(TimeUnit.MILLISECONDS)));
    }

    private List<MetricValue> of(TimeGauge gauge) {
        return List.of(new MetricValue(
                gauge.getId().getName(), "Milliseconds", gauge.value(TimeUnit.MILLISECONDS)));
    }

    private List<MetricValue> of(FunctionCounter counter) {
        return List.of(new MetricValue(counter.getId().getName(), "Count", counter.count()));
    }

    private List<MetricValue> of(FunctionTimer timer) {
        String name = timer.getId().getName();
        return List.of(
                new MetricValue(name + ".count", "Count", timer.count()),
                new MetricValue(name + ".sum", "Milliseconds", timer.totalTime(TimeUnit.MILLISECONDS)));
    }

    private List<MetricValue> of(Meter meter) {
        List<MetricValue> values = new ArrayList<>();
        meter.measure().forEach(m -> values.add(new MetricValue(
                meter.getId().getName() + "." + m.getStatistic().getTagValueRepresentation(), "None", m.getValue())));
        return values;
    }

    /** Riga EMF: {@code _aws} (Timestamp + metadata metriche) + dimensioni e valori come radice. */
    private String toEmfLine(Meter.Id id, List<MetricValue> values, long timestamp) {
        List<MetricValue> finite = values.stream().filter(v -> Double.isFinite(v.value())).toList();
        if (finite.isEmpty()) {
            return null;
        }
        // seconda barriera della whitelist (oltre al meter filter): mai dimensioni fuori lista
        Map<String, String> dimensions = new TreeMap<>();
        for (Tag tag : id.getTags()) {
            if (ALLOWED_DIMENSIONS.contains(tag.getKey())) {
                dimensions.put(tag.getKey(), tag.getValue());
            }
        }
        Map<String, Object> directive = new LinkedHashMap<>();
        directive.put("Namespace", NAMESPACE);
        directive.put("Dimensions", List.of(List.copyOf(dimensions.keySet())));
        directive.put("Metrics", finite.stream()
                .map(v -> Map.of("Name", v.name(), "Unit", v.unit()))
                .toList());

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("_aws", Map.of("Timestamp", timestamp, "CloudWatchMetrics", List.of(directive)));
        root.putAll(dimensions);
        finite.forEach(v -> root.put(v.name(), v.value()));
        try {
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            // mai far cadere il thread di publish per una metrica non serializzabile
            return null;
        }
    }
}
