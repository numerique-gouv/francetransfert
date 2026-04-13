package fr.gouv.culture.francetransfert.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component("custom")
public class CustomHealthIndicator implements HealthIndicator {

	private static final Logger LOGGER = LoggerFactory.getLogger(CustomHealthIndicator.class);
	private static final String JVM_LIVE_THREADS_METRIC = "jvm.threads.live";
	private static final String METRIC_DETAIL = "metric";

	private final MeterRegistry meterRegistry;
	private final int maxLiveThreads;

	public CustomHealthIndicator(
			MeterRegistry meterRegistry,
			@Value("${healthcheck.jvm-threads.max:220}") int maxLiveThreads) {
		this.meterRegistry = meterRegistry;
		this.maxLiveThreads = maxLiveThreads;
	}

	@Override
	public Health health() {

		Gauge liveThreadsGauge = meterRegistry.find(JVM_LIVE_THREADS_METRIC).gauge();
		if (liveThreadsGauge == null) {
			return Health.unknown()
					.withDetail("reason", "Metric not found")
					.withDetail(METRIC_DETAIL, JVM_LIVE_THREADS_METRIC)
					.build();
		}

		int liveThreads = (int) Math.round(liveThreadsGauge.value());
		if (liveThreads >= maxLiveThreads) {
			LOGGER.error("JVM live threads reached max threshold current={} max={}", liveThreads, maxLiveThreads);
			return Health.down()
					.withDetail("error", "JVM live threads reached max threshold")
					.withDetail(METRIC_DETAIL, JVM_LIVE_THREADS_METRIC)
					.withDetail("currentThreads", liveThreads)
					.withDetail("maxThreads", maxLiveThreads)
					.build();
		}

		return Health.up()
				.withDetail(METRIC_DETAIL, JVM_LIVE_THREADS_METRIC)
				.withDetail("currentThreads", liveThreads)
				.withDetail("maxThreads", maxLiveThreads)
				.build();
	}
}
