package fr.gouv.culture.francetransfert.services.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

import fr.gouv.culture.francetransfert.core.enums.GlimpsHealthCheckEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.model.HealthCheckRepresentation;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.enums.RedisQueueEnum;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;

@Component
public class CustomMetricsService {

    private final RedisManager redisManager;
    private final Counter finishedPliCounter;
    private final Timer pliTimer;

    @Autowired
    public CustomMetricsService(MeterRegistry meterRegistry, RedisManager redisManager) {

        this.redisManager = redisManager;

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
