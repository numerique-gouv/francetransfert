package fr.gouv.culture.francetransfert.services.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
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
}
