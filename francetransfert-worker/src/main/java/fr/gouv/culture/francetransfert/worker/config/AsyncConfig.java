/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.worker.config;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.utils.MonitorRunnable;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

	@Autowired
	RedisManager redisManager;

	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncConfig.class);

	@Value("${satisfactionWorkerExecutor.pool.size:3}")
	private int satisfactionWorkerExecutorPoolSize;

	@Value("${sendEmailConfirmationCodeWorker.pool.size:3}")
	private int sendEmailConfirmationCodeWorkerExecutorPoolSize;

	@Value("${tempDataCleanUpWorkerExecutor.pool.size:3}")
	private int tempDataCleanUpWorkerExecutorPoolSize;

	@Value("${zipWorkerExecutor.pool.size:3}")
	private int zipWorkerExecutorPoolSize;

	@Value("${sendEmailDownloadInProgressWorkerExecutor.pool.size:3}")
	private int sendEmailDownloadInProgressWorkerExecutorPoolSize;

	@Value("${uploadDownloadWorkerExecutor.pool.size:3}")
	private int sendEmailNotificationUploadDownloadWorkerExecutorPoolSize;

	@Value("${statWorkerExecutor.pool.size:3}")
	private int statWorkerExecutorPoolSize;

	@Value("${sequestreExecutor.pool.size:2}")
	private int sequestreWorkerExecutorPoolSize;

	@Value("${contactExecutor.pool.size:2}")
	private int formuleContactWorkerExecutorPoolSize;

	@Value("${deleteEnclosureExecutor.pool.size:2}")
	private int deleteEnclosureWorkerExecutorPoolSize;

	@Bean(name = "formuleContactWorkerExecutor")
	public Executor formuleContactWorkerExecutor() {
		return generateThreadPoolTaskExecutor(formuleContactWorkerExecutorPoolSize);
	}

	@Bean(name = "satisfactionWorkerExecutor")
	public Executor satisfactionWorkerExecutor() {
		return generateThreadPoolTaskExecutor(satisfactionWorkerExecutorPoolSize);
	}

	@Bean(name = "sendEmailConfirmationCodeWorkerExecutor")
	public Executor sendEmailConfirmationCodeWorkerExecutor() {
		return generateThreadPoolTaskExecutor(sendEmailConfirmationCodeWorkerExecutorPoolSize);
	}

	@Bean(name = "tempDataCleanUpWorkerExecutor")
	public Executor tempDataCleanUpWorkerExecutor() {
		return generateThreadPoolTaskExecutor(tempDataCleanUpWorkerExecutorPoolSize);
	}

	@Bean(name = "zipWorkerExecutor")
	public Executor zipWorkerExecutor() {
		return generateThreadPoolTaskExecutor(zipWorkerExecutorPoolSize);
	}

	@Bean(name = "sendEmailDownloadInProgressWorkerExecutor")
	public Executor sendEmailDownloadInProgressWorkerExecutor() {
		return generateThreadPoolTaskExecutor(sendEmailDownloadInProgressWorkerExecutorPoolSize);
	}

	@Bean(name = "sendEmailNotificationUploadDownloadWorkerExecutor")
	public Executor sendEmailNotificationUploadDownloadWorkerExecutor() {
		return generateThreadPoolTaskExecutor(sendEmailNotificationUploadDownloadWorkerExecutorPoolSize);
	}

	@Bean(name = "statWorkerExecutor")
	public Executor statWorkerExecutor() {
		return generateThreadPoolTaskExecutor(statWorkerExecutorPoolSize);
	}

	@Bean(name = "sequestreWorkerExecutor")
	public Executor sequestreWorkerExecutor() {
		return generateThreadPoolTaskExecutor(sequestreWorkerExecutorPoolSize);
	}

	@Bean(name = "deleteEnclosureWorkerExecutor")
	public Executor deleteEnclosureWorkerExecutor() {
		return generateThreadPoolTaskExecutor(deleteEnclosureWorkerExecutorPoolSize);
	}

	private Executor generateThreadPoolTaskExecutor(int maxPoolSize) {
		ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
		exec.setMaxPoolSize(maxPoolSize);
		exec.setCorePoolSize(maxPoolSize);
		exec.setKeepAliveSeconds(0);
		exec.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				MonitorRunnable mr = (MonitorRunnable) r;
				LOGGER.info("ThreadPool is full Putting back to queue {} - {}", mr.getQueue(), mr.getData());
				redisManager.publishFT(mr.getQueue(), mr.getData());
				throw new RejectedExecutionException("Queue is full");
			}
		});
		return exec;
	}
}
