package fr.gouv.culture.francetransfert.application.services;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.enums.RedisQueueEnum;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;

@Component
public class CustomMetricsService {

    private final Counter uploadedPliCounter;

    @Autowired
    public CustomMetricsService(MeterRegistry meterRegistry) {

        uploadedPliCounter = Counter.builder("ft_uploaded_pli")
                .description("Number of uploaded PLIs")
                .tags("app", "ft-upload-api")
                .register(meterRegistry);
    }

    public void incrementUploadedPliCounter() {
        uploadedPliCounter.increment();
    }
}
