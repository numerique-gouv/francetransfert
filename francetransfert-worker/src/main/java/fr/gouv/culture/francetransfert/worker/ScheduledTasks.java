/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.worker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.gson.Gson;

import fr.gouv.culture.francetransfert.core.enums.RedisQueueEnum;
import fr.gouv.culture.francetransfert.core.exception.StorageException;
import fr.gouv.culture.francetransfert.core.model.FormulaireContactData;
import fr.gouv.culture.francetransfert.core.model.NewRecipient;
import fr.gouv.culture.francetransfert.core.model.RateRepresentation;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.security.WorkerException;
import fr.gouv.culture.francetransfert.services.app.sync.AppSyncServices;
import fr.gouv.culture.francetransfert.services.cleanup.CleanUpServices;
import fr.gouv.culture.francetransfert.services.glimps.GlimpsService;
import fr.gouv.culture.francetransfert.services.ignimission.IgnimissionServices;
import fr.gouv.culture.francetransfert.services.mail.MailCheckService;
import fr.gouv.culture.francetransfert.services.mail.SnapService;
import fr.gouv.culture.francetransfert.services.mail.notification.MailAvailbleEnclosureServices;
import fr.gouv.culture.francetransfert.services.mail.notification.MailConfirmationCodeServices;
import fr.gouv.culture.francetransfert.services.mail.notification.MailDownloadServices;
import fr.gouv.culture.francetransfert.services.mail.notification.MailFormulaireContactServices;
import fr.gouv.culture.francetransfert.services.mail.notification.MailRelaunchServices;
import fr.gouv.culture.francetransfert.services.satisfaction.SatisfactionService;
import fr.gouv.culture.francetransfert.services.sequestre.SequestreService;
import fr.gouv.culture.francetransfert.services.stat.StatServices;
import fr.gouv.culture.francetransfert.services.zipworker.ZipWorkerServices;
import fr.gouv.culture.francetransfert.utils.MonitorRunnable;
import fr.gouv.culture.francetransfert.utils.WorkerUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class ScheduledTasks {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTasks.class);

	@Value("${bucket.sequestre}")
	private String sequestreBucket;

	@Autowired
	private MailAvailbleEnclosureServices mailAvailbleEnclosureServices;

	@Autowired
	private MailRelaunchServices mailRelaunchServices;

	@Autowired
	private AppSyncServices appSyncServices;

	@Autowired
	private MailDownloadServices mailDownloadServices;

	@Autowired
	private MailConfirmationCodeServices mailConfirmationCodeServices;

	@Autowired
	private CleanUpServices cleanUpServices;

	@Autowired
	private ZipWorkerServices zipWorkerServices;

	@Autowired
	private StatServices statServices;

	@Autowired
	private SatisfactionService satisfactionService;

	@Autowired
	private IgnimissionServices ignimissionServices;

	@Autowired
	private SnapService snapServices;

	@Autowired
	private SequestreService sequestreService;

	@Autowired
	private MailFormulaireContactServices formulaireContactService;

	@Autowired
	private RedisManager redisManager;

	@Autowired
	private MailCheckService mailCheckService;

	@Autowired
	private GlimpsService glimpsService;

	@Autowired
	@Qualifier("satisfactionWorkerExecutor")
	Executor satisfactionWorkerExecutorFromBean;

	@Autowired
	@Qualifier("sendEmailConfirmationCodeWorkerExecutor")
	Executor sendEmailConfirmationCodeWorkerExecutorFromBean;

	@Autowired
	@Qualifier("tempDataCleanUpWorkerExecutor")
	Executor tempDataCleanUpWorkerExecutorFromBean;

	@Autowired
	@Qualifier("zipWorkerExecutor")
	Executor zipWorkerExecutorFromBean;

	@Autowired
	@Qualifier("sendEmailDownloadInProgressWorkerExecutor")
	Executor sendEmailDownloadInProgressWorkerExecutorFromBean;

	@Autowired
	@Qualifier("sendEmailNotificationUploadDownloadWorkerExecutor")
	Executor sendEmailNotificationUploadDownloadWorkerExecutorFromBean;

	@Autowired
	@Qualifier("statWorkerExecutor")
	Executor statWorkerExecutorFromBean;

	@Autowired
	@Qualifier("sequestreWorkerExecutor")
	Executor sequestreWorkerExecutorFromBean;

	@Autowired
	@Qualifier("formuleContactWorkerExecutor")
	Executor formuleContactWorkerExecutorFromBean;

	@Autowired
	@Qualifier("deleteEnclosureWorkerExecutor")
	Executor deleteEnclosureWorkerExecutorFromBean;

	boolean runningThread = true;

	List<ThreadPoolTaskExecutor> executorList = Collections.synchronizedList(new ArrayList<>());

	@Scheduled(cron = "${scheduled.relaunch.mail}")
	public void relaunchMail() throws WorkerException {
		LOGGER.info("Worker : start relaunch for download Check");
		if (appSyncServices.shouldRelaunch()) {
			LOGGER.info("Worker : start relaunch for download Checked and Started");
			mailRelaunchServices.sendMailsRelaunch();
			mailDownloadServices.sendMailsDownload();
			LOGGER.info("Worker : finished relaunch for download");
		}
	}

	@Scheduled(cron = "${scheduled.clean.up}")
	public void cleanUp() throws WorkerException, StorageException {
		LOGGER.info("Worker : start clean-up expired enclosure Check");
		if (appSyncServices.shouldCleanup()) {
			LOGGER.info("Worker : start clean-up expired enclosure Checked and Started");
			cleanUpServices.cleanUp();
			LOGGER.info("Worker : start clean-up expired buckets");
			cleanUpServices.deleteBucketOutOfTime();
			LOGGER.info("Worker : finished clean-up expired enclosure");
			cleanUpServices.resetPasswordTryCount();
			LOGGER.info("Worker : initiat error counter");
		}
	}

	@Scheduled(cron = "${scheduled.app.sync.cleanup}")
	public void appSyncCleanup() throws WorkerException {
		LOGGER.info("Worker : start Application synchronization cleanup");
		appSyncServices.appSyncCleanup();
		LOGGER.info("Worker : finished pplication synchronization cleanup");
	}

	@Scheduled(cron = "${scheduled.app.sync.relaunch}")
	public void appSyncRelaunch() throws WorkerException {
		LOGGER.info("Worker : start Application synchronization relaunch");
		appSyncServices.appSyncRelaunch();
		LOGGER.info("Worker : finished Application synchronization relaunch");
	}

	@Scheduled(cron = "${scheduled.ignimission.domain}")
	public void ignimissionDomainUpdate() throws WorkerException {
		if (appSyncServices.shouldUpdateIgnimissionDomain()) {
			LOGGER.info("Worker : start Application ignimission/resana/osmose/rizomo domain update");
			ignimissionServices.updateDomains();
			snapServices.updateOsmose();
			snapServices.updateResana();
			snapServices.updateRizomo();
			LOGGER.info("Worker : finished Application ignimission/resana/osmose/rizomo update");
		}
	}

	@Scheduled(cron = "${scheduled.sendcheckmail}")
	public void checkMailSend() throws WorkerException {
		if (appSyncServices.shouldSendCheckMail()) {
			LOGGER.info("Worker : start checkMailSend");
			mailCheckService.sendMail();
			LOGGER.info("Worker : finished checkMailSend");
		}
	}

	@Scheduled(cron = "${scheduled.checkglimps}")
	public void checkGlimps() throws WorkerException {
		if (appSyncServices.shouldCheckGlimps()) {
			LOGGER.info("Worker : start checkGlimps");
			glimpsService.healthCheckGlimps();
			LOGGER.info("Worker : finished checkGlimps");
		}
	}

	@Scheduled(cron = "${scheduled.sync.checkglimps}")
	public void appSyncGlimpsCheck() {
		LOGGER.info("Worker : start Application synchronization appSyncGlimpsCheck");
		appSyncServices.appSyncGlimpsCheck();
		LOGGER.info("Worker : finished Application synchronization appSyncGlimpsCheck");
	}

	@Scheduled(cron = "${scheduled.checkmail}")
	public void checkMailCheck() throws WorkerException {
		if (appSyncServices.shouldCheckMailCheck()) {
			LOGGER.info("Worker : start checkMail");
			mailCheckService.mailCheck();
			LOGGER.info("Worker : finished checkMail");
		}
	}

	@Scheduled(cron = "${scheduled.sync.checkmail}")
	public void appSyncCheckMailCheck() {
		LOGGER.info("Worker : start Application synchronization CheckMailCheck");
		appSyncServices.appSyncCheckMailCheck();
		LOGGER.info("Worker : finished Application synchronization CheckMailCheck");
	}

	@Scheduled(cron = "${scheduled.sync.checkmail}")
	public void appSyncCheckMailSend() {
		LOGGER.info("Worker : start Application synchronization CheckMailSend");
		appSyncServices.appSyncCheckMailSend();
		LOGGER.info("Worker : finished Application synchronization CheckMailSend");
	}

	@Scheduled(cron = "${scheduled.send.stat}")
	public void ignimissionSendStat() throws WorkerException {
		LOGGER.info("Worker : start Application ignimissionSendStat");
		ignimissionServices.sendStats();
		LOGGER.info("Worker : finished Application ignimissionSendStat");
	}

	@Scheduled(cron = "${scheduled.app.sync.ignimission.domain}")
	public void appSyncIgnimissionDomain() {
		LOGGER.info("Worker : start Application synchronization ignimission domain");
		appSyncServices.appSyncIgnimissionDomain();
		LOGGER.info("Worker : finished Application synchronization ignimission domain");
	}

	@PostConstruct
	public void initWorkers() throws WorkerException {
		initZipWorkers();
		initSendEmailNotificationUploadDownloadWorkers();
		initSendEmailConfirmationCodeWorkers();
		initTempDataCleanupWorkers();
		initSatisfactionWorkers();
		initStatWorker();
		initSequestre();
		initFormuleContact();
		initSendToNewEmailNotificationUploadDownloadWorkers();
		initDeleteEnclosureWorkers();
	}

	private void initSequestre() {
		LOGGER.info("initSequestre");
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				ThreadPoolTaskExecutor sequestreWorkerExecutor = (ThreadPoolTaskExecutor) sequestreWorkerExecutorFromBean;
				executorList.add(sequestreWorkerExecutor);
				while (runningThread) {
					try {
						List<String> returnedBLPOPList = redisManager
								.subscribeFT(RedisQueueEnum.SEQUESTRE_QUEUE.getValue());
						if (!CollectionUtils.isEmpty(returnedBLPOPList)) {
							String enclosureId = returnedBLPOPList.get(1);
							SequestreWorkerTask task = new SequestreWorkerTask(enclosureId, sequestreBucket,
									sequestreService);
							sequestreWorkerExecutor.execute(
									new MonitorRunnable(task, RedisQueueEnum.SEQUESTRE_QUEUE.getValue(), enclosureId));
						}
					} catch (Exception e) {
						LOGGER.error("Error initStatWorker : " + e.getMessage(), e);
					}
				}
			}
		});
	}

	private void initStatWorker() {
		LOGGER.info("initStatWorker");
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				ThreadPoolTaskExecutor statWorkerExecutor = (ThreadPoolTaskExecutor) statWorkerExecutorFromBean;
				executorList.add(statWorkerExecutor);
				while (runningThread) {
					try {
						List<String> returnedBLPOPList = redisManager.subscribeFT(RedisQueueEnum.STAT_QUEUE.getValue());
						if (!CollectionUtils.isEmpty(returnedBLPOPList)) {
							String statMessage = returnedBLPOPList.get(1);
							StatTask task = new StatTask(statMessage, redisManager, statServices);
							statWorkerExecutor.execute(
									new MonitorRunnable(task, RedisQueueEnum.STAT_QUEUE.getValue(), statMessage));
						}
					} catch (Exception e) {
						LOGGER.error("Error initStatWorker : " + e.getMessage(), e);
					}
				}
			}
		});
	}

	private void initTempDataCleanupWorkers() {
		LOGGER.info("initTempDataCleanupWorkers");
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				ThreadPoolTaskExecutor TempDataCleanupWorkerExecutor = (ThreadPoolTaskExecutor) tempDataCleanUpWorkerExecutorFromBean;
				executorList.add(TempDataCleanupWorkerExecutor);
				while (runningThread) {
					try {
						List<String> returnedBLPOPList = redisManager
								.subscribeFT(RedisQueueEnum.TEMP_DATA_CLEANUP_QUEUE.getValue());
						if (!CollectionUtils.isEmpty(returnedBLPOPList)) {
							String enclosureId = returnedBLPOPList.get(1);
							TempDataCleanupTask task = new TempDataCleanupTask(enclosureId, cleanUpServices);
							TempDataCleanupWorkerExecutor.execute(new MonitorRunnable(task,
									RedisQueueEnum.TEMP_DATA_CLEANUP_QUEUE.getValue(), enclosureId));
						}
					} catch (Exception e) {
						LOGGER.error("Error initTempDataCleanupWorkers : " + e.getMessage(), e);
					}
				}
			}
		});
	}

	private void initSendEmailConfirmationCodeWorkers() {
		LOGGER.info("initSendEmailConfirmationCodeWorkers");
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				ThreadPoolTaskExecutor SendEmailConfirmationCodeWorkerExecutor = (ThreadPoolTaskExecutor) sendEmailConfirmationCodeWorkerExecutorFromBean;
				executorList.add(SendEmailConfirmationCodeWorkerExecutor);
				while (runningThread) {
					try {
						List<String> returnedBLPOPList = redisManager
								.subscribeFT(RedisQueueEnum.CONFIRMATION_CODE_MAIL_QUEUE.getValue());
						if (!CollectionUtils.isEmpty(returnedBLPOPList)) {
							String mailCode = returnedBLPOPList.get(1);
							SendEmailConfirmationCodeTask task = new SendEmailConfirmationCodeTask(mailCode,
									mailConfirmationCodeServices);
							SendEmailConfirmationCodeWorkerExecutor.execute(new MonitorRunnable(task,
									RedisQueueEnum.CONFIRMATION_CODE_MAIL_QUEUE.getValue(), mailCode));
						}
					} catch (Exception e) {
						LOGGER.error("Error initSendEmailConfirmationCodeWorkers : " + e.getMessage(), e);
					}
				}
			}
		});
	}

	private void initSendEmailNotificationUploadDownloadWorkers() {
		LOGGER.info("initSendEmailNotificationUploadDownloadWorkers");
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				ThreadPoolTaskExecutor SendEmailNotificationUploadDownloadWorkerExecutor = (ThreadPoolTaskExecutor) sendEmailNotificationUploadDownloadWorkerExecutorFromBean;
				while (runningThread) {
					try {
						List<String> returnedBLPOPList = redisManager.subscribeFT(RedisQueueEnum.MAIL_QUEUE.getValue());
						if (!CollectionUtils.isEmpty(returnedBLPOPList)) {
							String enclosureId = returnedBLPOPList.get(1);
							SendEmailNotificationUploadDownloadTask task = new SendEmailNotificationUploadDownloadTask(
									enclosureId, redisManager, mailAvailbleEnclosureServices);
							SendEmailNotificationUploadDownloadWorkerExecutor.execute(
									new MonitorRunnable(task, RedisQueueEnum.MAIL_QUEUE.getValue(), enclosureId));
						}
					} catch (Exception e) {
						LOGGER.error("Error initSendEmailNotificationUploadDownloadWorkers : " + e.getMessage(), e);
					}
				}
			}
		});
	}

	private void initSendToNewEmailNotificationUploadDownloadWorkers() {
		LOGGER.info("initSendEmailNotificationUploadDownloadWorkers");
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				ThreadPoolTaskExecutor SendEmailNotificationUploadDownloadWorkerExecutor = (ThreadPoolTaskExecutor) sendEmailNotificationUploadDownloadWorkerExecutorFromBean;
				executorList.add(SendEmailNotificationUploadDownloadWorkerExecutor);
				while (runningThread) {
					try {
						String email = "";
						List<String> returnedBLPOPList = redisManager
								.subscribeFT(RedisQueueEnum.MAIL_NEW_RECIPIENT_QUEUE.getValue());
						NewRecipient dataRecipient = new Gson().fromJson(returnedBLPOPList.get(1), NewRecipient.class);
						if (!CollectionUtils.isEmpty(returnedBLPOPList)) {
							String enclosureId = dataRecipient.getIdEnclosure();
							SendEmailNotificationUploadDownloadTask task = new SendEmailNotificationUploadDownloadTask(
									enclosureId, dataRecipient, redisManager, mailAvailbleEnclosureServices);
							SendEmailNotificationUploadDownloadWorkerExecutor.execute(new MonitorRunnable(task,
									RedisQueueEnum.MAIL_NEW_RECIPIENT_QUEUE.getValue(), enclosureId));

						}
					} catch (Exception e) {
						LOGGER.error("Error initSendEmailNotificationUploadDownloadWorkers : " + e.getMessage(), e);
					}
				}
			}
		});
	}

	private void initZipWorkers() {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				ThreadPoolTaskExecutor zipWorkerExecutor = (ThreadPoolTaskExecutor) zipWorkerExecutorFromBean;
				executorList.add(zipWorkerExecutor);
				while (runningThread) {
					try {
						List<String> returnedBLPOPList = redisManager.subscribeFT(RedisQueueEnum.ZIP_QUEUE.getValue());
						if (!CollectionUtils.isEmpty(returnedBLPOPList)) {
							String enclosureId = returnedBLPOPList.get(1);
							ZipWorkerTask task = new ZipWorkerTask(enclosureId, zipWorkerServices);
							zipWorkerExecutor.execute(
									new MonitorRunnable(task, RedisQueueEnum.ZIP_QUEUE.getValue(), enclosureId));
						}
					} catch (Exception e) {
						LOGGER.error("Error initZipWorkers : " + e.getMessage(), e);
					}
				}
			}
		});
	}

	private void initFormuleContact() {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				ThreadPoolTaskExecutor formuleContactExecutor = (ThreadPoolTaskExecutor) formuleContactWorkerExecutorFromBean;
				executorList.add(formuleContactExecutor);
				while (runningThread) {
					try {
						List<String> returnedBLPOPList = redisManager
								.subscribeFT(RedisQueueEnum.FORMULE_CONTACT_QUEUE.getValue());
						if (!CollectionUtils.isEmpty(returnedBLPOPList)) {
							FormulaireContactData formulaire = new Gson().fromJson(returnedBLPOPList.get(1),
									FormulaireContactData.class);
							FormulaireContactTask task = new FormulaireContactTask(formulaire,
									formulaireContactService);
							formuleContactExecutor.execute(new MonitorRunnable(task,
									RedisQueueEnum.FORMULE_CONTACT_QUEUE.getValue(), returnedBLPOPList.get(1)));
						}
					} catch (Exception e) {
						LOGGER.error("Error initFormulaireContact : " + e.getMessage(), e);
					}
				}
			}
		});
	}

	private void initSatisfactionWorkers() {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				ThreadPoolTaskExecutor satisfactionWorkerExecutor = (ThreadPoolTaskExecutor) satisfactionWorkerExecutorFromBean;
				executorList.add(satisfactionWorkerExecutor);
				while (runningThread) {
					try {
						List<String> returnedBLPOPList = redisManager
								.subscribeFT(RedisQueueEnum.SATISFACTION_QUEUE.getValue());
						if (!CollectionUtils.isEmpty(returnedBLPOPList)) {
							RateRepresentation rate = new Gson().fromJson(returnedBLPOPList.get(1),
									RateRepresentation.class);
							SatisfactionTask task = new SatisfactionTask(rate, satisfactionService);
							satisfactionWorkerExecutor.execute(new MonitorRunnable(task,
									RedisQueueEnum.SATISFACTION_QUEUE.getValue(), returnedBLPOPList.get(1)));
						}
					} catch (Exception e) {
						LOGGER.error("Error initSatisfactionWorkers : " + e.getMessage(), e);
					}
				}
			}
		});
	}

	private void initDeleteEnclosureWorkers() {
		LOGGER.info("initDeleteEnclosureWorkers");
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				ThreadPoolTaskExecutor DeleteEnclosureWorkerExecutor = (ThreadPoolTaskExecutor) deleteEnclosureWorkerExecutorFromBean;
				executorList.add(DeleteEnclosureWorkerExecutor);
				while (runningThread) {
					try {
						List<String> returnedBLPOPList = redisManager
								.subscribeFT(RedisQueueEnum.DELETE_ENCLOSURE_QUEUE.getValue());
						if (!CollectionUtils.isEmpty(returnedBLPOPList)) {
							String enclosureId = returnedBLPOPList.get(1);
							CleanEnclosureTask task = new CleanEnclosureTask(enclosureId, cleanUpServices);
							DeleteEnclosureWorkerExecutor.execute(new MonitorRunnable(task,
									RedisQueueEnum.DELETE_ENCLOSURE_QUEUE.getValue(), enclosureId));
						}
					} catch (Exception e) {
						LOGGER.error("Error initDeleteEnclosureWorkers : " + e.getMessage(), e);
					}
				}
			}
		});
	}

	@PreDestroy
	public void shutdown() {
		LOGGER.info("ShuttingDown");
		runningThread = false;
		LOGGER.info("Active Task {}", WorkerUtils.activeTasks.size());
		executorList.forEach(executor -> {
			try {
				executor.setQueueCapacity(0);
				executor.shutdown();
			} catch (Exception e) {
				LOGGER.error("Cannot stop executor ", e);
			}
		});
		WorkerUtils.activeTasks.forEach(task -> {
			LOGGER.info("Putting back to queue {} - {}", task.getQueue(), task.getData());
			redisManager.publishFT(task.getQueue(), task.getData());
		});
		LOGGER.info("Finished putting task back to queue");

	}
}
