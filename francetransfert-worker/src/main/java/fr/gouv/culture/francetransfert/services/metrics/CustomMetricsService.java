package fr.gouv.culture.francetransfert.services.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.google.gson.Gson;

import fr.gouv.culture.francetransfert.core.enums.GlimpsHealthCheckEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.model.HealthCheckRepresentation;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.enums.RedisQueueEnum;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;

import org.apache.commons.lang3.StringUtils;

@Component
public class CustomMetricsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomMetricsService.class);

    private final MeterRegistry meterRegistry;
    private final RedisManager redisManager;
    private final Counter finishedPliCounter;
    private final Timer pliTimer;
    private final int maxDistinctMimeTypes;
    private final Set<String> recordedMimeTypes = ConcurrentHashMap.newKeySet();
    private final Object mimeTypeMetricsLock = new Object();

    @Autowired
    public CustomMetricsService(MeterRegistry meterRegistry, RedisManager redisManager,
            @Value("${metrics.file-mimetype.max-distinct:5000}") int maxDistinctMimeTypes) {

        this.meterRegistry = meterRegistry;
        this.redisManager = redisManager;
        this.maxDistinctMimeTypes = maxDistinctMimeTypes;

        Gauge.builder("ft_queue_zip_size", this, CustomMetricsService::getQueueZipSize)
                .description("Size of the ZIP queue")
                .tags("app", "ft-worker")
                .register(meterRegistry);

        finishedPliCounter = Counter.builder("ft_finished_pli")
                .description("Number of finished PLIs")
                .tags("app", "ft-worker")
                .register(meterRegistry);

        pliTimer = Timer.builder("ft_pli_timer")
                .description("Time taken to process a PLI")
                .tags("app", "ft-worker")
                .publishPercentiles(0.3, 0.5, 0.7, 0.95)
                .publishPercentileHistogram()
                .register(meterRegistry);

        Gauge.builder("ft_jcop_state", this, CustomMetricsService::getJcopState)
                .description("State of Jcop")
                .tags("app", "ft-worker")
                .register(meterRegistry);

        Gauge.builder("ft_health_check_state", this, CustomMetricsService::getHealthCheckState)
                .description("State of health check")
                .tags("app", "ft-worker")
                .register(meterRegistry);
    }

    public long getQueueZipSize() {
        return redisManager.llen(RedisQueueEnum.ZIP_QUEUE.getValue());
    }

    public void incrementFinishedPliCounter() {
        finishedPliCounter.increment();
    }

    public void recordPliTime(long duration, TimeUnit timeUnit) {
        pliTimer.record(duration, timeUnit);
    }

    public void incrementMimeType(String mimeType) {
        String normalizedMimeType = StringUtils.isBlank(mimeType) ? "unknown" : mimeType.trim();

        synchronized (mimeTypeMetricsLock) {
            if (!recordedMimeTypes.contains(normalizedMimeType) && recordedMimeTypes.size() >= maxDistinctMimeTypes) {
                resetMimeTypeMetrics();
            }
            recordedMimeTypes.add(normalizedMimeType);
        }

        Counter.builder("ft_file_mimetype")
                .description("Number of files processed by detected mimetype")
                .tags("app", "ft-worker", "mimetype", normalizedMimeType)
                .register(meterRegistry)
                .increment();
    }

    private void resetMimeTypeMetrics() {
        meterRegistry.getMeters().stream()
                .filter(meter -> "ft_file_mimetype".equals(meter.getId().getName()))
                .forEach(meterRegistry::remove);
        recordedMimeTypes.clear();
        LOGGER.warn("Reset ft_file_mimetype metrics after reaching {} distinct mimetypes", maxDistinctMimeTypes);
    }

    public int getJcopState() {
        boolean glimpsState = Boolean
                .parseBoolean(redisManager.getString(GlimpsHealthCheckEnum.STATE_REAL.getKey()));
        return glimpsState ? 1 : 0;
    }

    public int getHealthCheckState() {
        String jsonInString = redisManager.getString(RedisKeysEnum.HEALTHCHECK.getFirstKeyPart());
        HealthCheckRepresentation health = new Gson().fromJson(jsonInString, HealthCheckRepresentation.class);
        return health.isFtError() ? 0 : 1;
    }
}
