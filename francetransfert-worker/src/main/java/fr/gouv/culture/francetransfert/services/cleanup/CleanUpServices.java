/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.cleanup;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.model.Bucket;

import fr.gouv.culture.francetransfert.core.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RecipientKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.StatutEnum;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.exception.RetryException;
import fr.gouv.culture.francetransfert.core.exception.StorageException;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.services.StorageManager;
import fr.gouv.culture.francetransfert.core.utils.Base64CryptoService;
import fr.gouv.culture.francetransfert.core.utils.DateUtils;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.core.utils.StringUploadUtils;
import fr.gouv.culture.francetransfert.model.Enclosure;
import fr.gouv.culture.francetransfert.security.WorkerException;
import fr.gouv.culture.francetransfert.services.mail.notification.MailEnclosureNoLongerAvailbleServices;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

@Service
public class CleanUpServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(CleanUpServices.class);

	private static final DateTimeFormatter DATE_FORMAT_BUCKET = DateTimeFormatter.ofPattern("yyyyMMdd");

	List<String> failState = Arrays.asList(StatutEnum.INI.getCode(), StatutEnum.ECC.getCode(),
			StatutEnum.ECH.getCode(), StatutEnum.CHT.getCode(), StatutEnum.ANA.getCode(), StatutEnum.ETF.getCode(),
			StatutEnum.EAV.getCode(), StatutEnum.APT.getCode(), StatutEnum.EDC.getCode(), StatutEnum.EEC.getCode());

	@Value("${bucket.prefix}")
	private String bucketPrefix;

	@Value("${expire.month:12}")
	private int expireMonth;

	@Value("${deepclean.weekday:7}")
	private int deepCleanWeekDay;

	@Value("${bucket.export}")
	private String bucketExport;

	@Autowired
	MailEnclosureNoLongerAvailbleServices mailEnclosureNoLongerAvailbleServices;

	@Autowired
	StorageManager storageManager;

	@Autowired
	RedisManager redisManager;

	@Autowired
	Base64CryptoService base64CryptoService;

	@Autowired
	StringUploadUtils stringUploadUtils;

	@Value("${worker.expired.limit}")
	private int maxUpdateDate;

	/**
	 * clean all expired data in OSU and REDIS
	 *
	 * @throws WorkerException
	 */
	public void cleanUp() throws WorkerException {

		redisManager.smembersString(RedisKeysEnum.FT_ENCLOSURE_DATES.getKey("")).forEach(date -> {
			redisManager.smembersString(RedisKeysEnum.FT_ENCLOSURE_DATE.getKey(date)).forEach(enclosureId -> {
				try {
					LocalDate enclosureExipireDateRedis = DateUtils.convertStringToLocalDateTime(
							redisManager.getHgetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
									EnclosureKeysEnum.EXPIRED_TIMESTAMP.getKey()))
							.toLocalDate();

					boolean archive = false;

					String archiveDate = redisManager.getHgetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
							EnclosureKeysEnum.EXPIRED_TIMESTAMP_ARCHIVE.getKey());

					LocalDate enclosureExpireArchiveDateRedis = DateUtils.convertStringToLocalDateTime(
							redisManager.getHgetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
									EnclosureKeysEnum.EXPIRED_TIMESTAMP_ARCHIVE.getKey()))
							.toLocalDate();

					if (enclosureExipireDateRedis.plusDays(1).equals(LocalDate.now())
							|| enclosureExipireDateRedis.plusDays(1).isBefore(LocalDate.now())) {
						if (StringUtils.isBlank(archiveDate)) {
							cleanEnclosure(enclosureId, archive);
						} else {
							if (!StringUtils.isBlank(archiveDate)
									&& (enclosureExpireArchiveDateRedis.plusDays(1).equals(LocalDate.now())
											|| enclosureExpireArchiveDateRedis.plusDays(1).isBefore(LocalDate.now()))) {
								archive = true;
								cleanEnclosure(enclosureId, archive);
								cleanUpEnclosureDatesInRedis(date);
							}
						}
						// clean enclosure date : delete list enclosureId and date expired
					}
				} catch (Exception e) {
					LOGGER.error("Cannot clean enclosure {} : " + e.getMessage(), enclosureId, e);
				}
			});
		});
		this.deepClean();
	}

	private void deepClean() {

		Calendar cal = Calendar.getInstance();
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		if (dayOfWeek == deepCleanWeekDay) {
			LOGGER.info("Deep Clean");

			ScanParams scanParams = new ScanParams().count(1000).match("enclosure:*");
			ScanParams scanFilesParams = new ScanParams().count(1000).match("file:*");
			ScanParams scanTokensParams = new ScanParams().count(1000).match("sender:*");
			ScanParams scanSendParams = new ScanParams().count(1000).match("send:*");
			ScanParams scanReceiveParams = new ScanParams().count(1000).match("receive:*");
			String cur = scanParams.SCAN_POINTER_START;
			LocalDate deleteBefore = LocalDate.now().minusMonths(expireMonth);
			LocalDate deleteFailBefore = LocalDate.now().minusDays(2);
			int cpt = 0;
			LOGGER.info("Clean Old Enclosure");
			do {
				ScanResult<String> scanResult = redisManager.sscan(cur, scanParams);

				for (String enclosureKey : scanResult.getResult()) {
					try {
						LOGGER.debug("Key {}", enclosureKey);
						if (StringUtils.countMatches(enclosureKey, ":") == 1) {
							String enclosureId = enclosureKey.split(":")[1];

							LocalDate enclosureExipireDateRedis = DateUtils.convertStringToLocalDateTime(
									redisManager.getHgetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
											EnclosureKeysEnum.EXPIRED_TIMESTAMP.getKey()))
									.toLocalDate();

							LocalDate enclosureUploadDate = DateUtils.convertStringToLocalDateTime(
									redisManager.getHgetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
											EnclosureKeysEnum.TIMESTAMP.getKey()))
									.toLocalDate();

							LOGGER.debug("Enclosure {} at date {} / {}", enclosureId,
									enclosureExipireDateRedis.toString(), enclosureUploadDate.toString());
							if (deleteBefore(enclosureId, deleteBefore, enclosureExipireDateRedis, enclosureUploadDate)
									|| deleteFail(enclosureId, deleteFailBefore, enclosureUploadDate)) {
								LOGGER.info("Deleting {}", enclosureId);
								cleanUpEnclosureTempDataInRedis(enclosureId, true);
								cleanUpEnclosureCoreInRedis(enclosureId);
								cpt++;
							}
						}
					} catch (Exception e) {
						LOGGER.info("Unable to clean {}", enclosureKey, e);
					}
				}
				cur = scanResult.getCursor();
			} while (!cur.equals(scanParams.SCAN_POINTER_START));
			LOGGER.info("Cleaned enclosure {}", cpt);

			LOGGER.info("Clean fail files");
			cpt = 0;
			cur = scanFilesParams.SCAN_POINTER_START;
			do {
				ScanResult<String> scanResult = redisManager.sscan(cur, scanFilesParams);

				for (String file : scanResult.getResult()) {
					try {
						redisManager.deleteKey(file);
						cpt++;
					} catch (Exception e) {
						LOGGER.info("Unable to clean {}", file, e);
					}
				}
				cur = scanResult.getCursor();
			} while (!cur.equals(scanFilesParams.SCAN_POINTER_START));
			LOGGER.info("Cleaned files {}", cpt);

			LOGGER.info("Clean old token");
			cpt = 0;
			cur = scanTokensParams.SCAN_POINTER_START;
			do {
				ScanResult<String> scanResult = redisManager.sscan(cur, scanTokensParams);

				for (String token : scanResult.getResult()) {
					try {
						redisManager.expire(token, 3600);
						cpt++;
					} catch (Exception e) {
						LOGGER.info("Unable to clean {}", token, e);
					}
				}
				cur = scanResult.getCursor();
			} while (!cur.equals(scanTokensParams.SCAN_POINTER_START));
			LOGGER.info("Cleaned token {}", cpt);

			LOGGER.info("Clean receive");
			cur = scanReceiveParams.SCAN_POINTER_START;
			cpt = 0;
			do {
				ScanResult<String> scanResult = redisManager.sscan(cur, scanReceiveParams);
				for (String receive : scanResult.getResult()) {
					if (redisManager.scardString(receive) == 0) {
						redisManager.deleteKey(receive);
						cpt++;
					}
				}
				cur = scanResult.getCursor();
			} while (!cur.equals(scanReceiveParams.SCAN_POINTER_START));
			LOGGER.info("Cleaned receive {}", cpt);

			LOGGER.info("Clean send");
			cur = scanSendParams.SCAN_POINTER_START;
			cpt = 0;
			do {
				ScanResult<String> scanResult = redisManager.sscan(cur, scanSendParams);
				for (String send : scanResult.getResult()) {
					if (redisManager.scardString(send) == 0) {
						redisManager.deleteKey(send);
						cpt++;
					}
				}
				cur = scanResult.getCursor();
			} while (!cur.equals(scanSendParams.SCAN_POINTER_START));
			LOGGER.info("Cleaned send {}", cpt);
		} else {
			LOGGER.info("Next deep clean on {}", dayOfWeek);
		}

	}

	private boolean deleteFail(String enclosureId, LocalDate deleteFailBefore, LocalDate enclosureInDate) {
		try {
			Map<String, String> enclosureRedis = RedisUtils.getEnclosure(redisManager, enclosureId);
			String statut = enclosureRedis.get(EnclosureKeysEnum.STATUS_CODE.getKey());

			if ((StringUtils.isEmpty(statut) || failState.contains(statut))
					&& enclosureInDate.isBefore(deleteFailBefore)) {
				LOGGER.info("Deleting fail enclosure {} with statut {} and enclosureInDate {}", enclosureId, statut,
						enclosureInDate);
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("Cannot get enclosure during deep clean {} : {}", enclosureId, e.getMessage(), e);
			return true;
		}
		return false;
	}

	private boolean deleteBefore(String enclosureId, LocalDate deleteBefore, LocalDate enclosureExipireDateRedis,
			LocalDate enclosureUploadDate) {

		if (enclosureExipireDateRedis.isBefore(deleteBefore) || enclosureUploadDate.isBefore(deleteBefore)) {
			LOGGER.info("Deleting enclosure {} with enclosureExipireDateRedis {} and enclosureInDate {}", enclosureId,
					enclosureExipireDateRedis, enclosureUploadDate);
			return true;
		}

		return false;
	}

	public void cleanEnclosure(String enclosureId, boolean archive) throws MetaloadException {
		// expire date + 1
		Enclosure enc = Enclosure.build(enclosureId, redisManager);
		Integer countDownload = 0;
		if (enc.isPublicLink()) {
			Map<String, String> enclosureMap = redisManager
					.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
			if (enclosureMap != null) {
				countDownload = Integer.parseInt(enclosureMap.get(EnclosureKeysEnum.PUBLIC_DOWNLOAD_COUNT.getKey()));
			}
		} else {
			countDownload = enc.getRecipients().stream().map(recipi -> {
				try {
					return RedisUtils.getNumberOfDownloadsPerRecipient(redisManager, recipi.getId());
				} catch (MetaloadException e) {
					LOGGER.error("Cannot get nbDown recipient", e);
				}
				return 0;
			}).collect(Collectors.summingInt(x -> x));

		}

		if (countDownload == 0) {
			if (stringUploadUtils.isValidEmailIgni(enc.getSender())) {
				LOGGER.warn("msgtype: NOT_DOWNLOADED_AGENT || enclosure: {} || sender: {}", enc.getGuid(),
						enc.getSender());
			} else {
				LOGGER.warn("msgtype: NOT_DOWNLOADED || enclosure: {} || sender: {}", enc.getGuid(), enc.getSender());
			}
		}

		if (!archive) {
			LocalDate enclosureExipireDateRedis = DateUtils.convertStringToLocalDateTime(redisManager.getHgetString(
					RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId), EnclosureKeysEnum.EXPIRED_TIMESTAMP.getKey()))
					.toLocalDate();

			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
					EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.ARC.getCode(), -1);
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
					EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.ARC.getWord(), -1);

			Map<String, String> enclosureMap = redisManager
					.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
			LocalDateTime expiredArchiveDate = enclosureExipireDateRedis.atStartOfDay().plus(Period.ofDays(365));
			enclosureMap.put(EnclosureKeysEnum.EXPIRED_TIMESTAMP_ARCHIVE.getKey(), expiredArchiveDate.toString());
			redisManager.insertHASH(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId), enclosureMap);
			mailEnclosureNoLongerAvailbleServices.sendEnclosureNotAvailble(enc);
		}

		LOGGER.info(" clean up for enclosure N° {}", enclosureId);
		String bucketName = RedisUtils.getBucketName(redisManager, enclosureId, bucketPrefix);

		// clean temp data in REDIS for Enclosure
		cleanUpEnclosureTempDataInRedis(enclosureId, archive);
		LOGGER.info("Clean up REDIS temp data");

		// clean enclosure in OSU : delete enclosure
		LOGGER.info("Clean up OSU");

		try {
			cleanUpOSU(bucketName, enclosureId);
		} catch (Exception ex) {
			LOGGER.error("Cannot delete enclosure " + enclosureId);
		}

		// clean enclosure Core in REDIS : delete files, root-files, root-dirs,
		// recipients, sender and enclosure
		LOGGER.info("Clean up REDIS");

		if (archive) {
			cleanUpEnclosureCoreInRedis(enclosureId);
		}

	}

	/**
	 * clean all data expired in OSU
	 *
	 * @param enclosureId
	 * @throws StorageException
	 */
	private void cleanUpOSU(String bucketName, String enclosureId) throws RetryException {
		storageManager.deleteFilesWithPrefix(bucketName, storageManager.getZippedEnclosureName(enclosureId));
	}

	/**
	 * clean expired data in REDIS: Enclosure core
	 *
	 * @param enclosureId
	 * @throws WorkerException
	 * @throws MetaloadException
	 */
	public void cleanUpEnclosureCoreInRedis(String enclosureId) throws WorkerException, MetaloadException {
		cleanUpEnclosurePartiallyCoreInRedis(enclosureId);
		// delete hash enclosure and sender
		LOGGER.debug("Clean enclosure key {}", enclosureId);
		redisManager.deleteKey(RedisKeysEnum.FT_SENDER.getKey(enclosureId));
		redisManager.deleteKey(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
	}

	public void cleanUpEnclosurePartiallyCoreInRedis(String enclosureId) throws WorkerException, MetaloadException {

		Enclosure enclosure = Enclosure.build(enclosureId, redisManager);
		if (!enclosure.isPublicLink()) {
			// Delete received list
			enclosure.getRecipients().stream().forEach(x -> {
				redisManager.srem(RedisKeysEnum.FT_RECEIVE.getKey(x.getMail()), enclosureId);
				long receiveCount = redisManager.scardString(RedisKeysEnum.FT_RECEIVE.getKey(x.getMail()));
				if (receiveCount == 0) {
					redisManager.deleteKey(RedisKeysEnum.FT_RECEIVE.getKey(x.getMail()));
				}
			});
		}
		// delete list and HASH root-files
		deleteRootFiles(enclosureId);
		LOGGER.debug("clean root-files {}", RedisKeysEnum.FT_ROOT_FILES.getKey(enclosureId));
		// delete list and HASH root-dirs
		deleteRootDirs(enclosureId);
		LOGGER.debug("clean root-dirs {}", RedisKeysEnum.FT_ROOT_DIRS.getKey(enclosureId));
		// delete list and HASH recipients
		deleteListAndHashRecipients(enclosureId);
		LOGGER.debug("clean recipients {}", RedisKeysEnum.FT_RECIPIENTS.getKey(enclosureId));
		// delete hash sender
		redisManager.deleteKey(RedisKeysEnum.FT_ADMIN_TOKEN.getKey(enclosureId));
		redisManager.deleteKey(RedisKeysEnum.FT_ENCLOSURE_SCAN.getKey(enclosureId));
		redisManager.deleteKey(RedisKeysEnum.FT_ENCLOSURE_SCAN_DELAY.getKey(enclosureId));
		redisManager.deleteKey(RedisKeysEnum.FT_ENCLOSURE_VIRUS.getKey(enclosureId));
		redisManager.deleteKey(RedisKeysEnum.FT_ENCLOSURE_SCAN_RETRY.getKey(enclosureId));

		// delete enclosureid from sendlist
		redisManager.srem(RedisKeysEnum.FT_SEND.getKey(enclosure.getSender()), enclosureId);
		long sendCount = redisManager.scardString(RedisKeysEnum.FT_SEND.getKey(enclosure.getSender()));
		if (sendCount == 0) {
			redisManager.deleteKey(RedisKeysEnum.FT_SEND.getKey(enclosure.getSender()));
		}
		LOGGER.debug("clean enclosure HASH {}", RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));

	}

	/**
	 * clean temp data in REDIS for Enclosure
	 *
	 * @param redisManager
	 * @param enclosureId
	 * @throws WorkerException
	 */
	public void cleanUpEnclosureTempDataInRedis(String enclosureId, boolean archive) throws WorkerException {
		// delete part-etags
		deleteListPartEtags(enclosureId);

		// delete id container list
		if (archive) {
			deleteListIdContainer(enclosureId);
		}
		// delete list and HASH files
		deleteFiles(enclosureId);
	}

	/**
	 * clean expired data in REDIS: Enclosure dates
	 *
	 * @param redisManager
	 * @param date
	 * @throws WorkerException
	 */
	private void cleanUpEnclosureDatesInRedis(String date) throws WorkerException {
		// delete list enclosureId of expired date
		redisManager.deleteKey(RedisKeysEnum.FT_ENCLOSURE_DATE.getKey(date));
		LOGGER.info("clean list enclosure per date {}", RedisKeysEnum.FT_ENCLOSURE_DATE.getKey(date));
		// delete date expired from the list of dates
		redisManager.sremString(RedisKeysEnum.FT_ENCLOSURE_DATES.getKey(""), date);
		LOGGER.info("finish clean up list dates {} delete date : {} ", RedisKeysEnum.FT_ENCLOSURE_DATE.getKey(date),
				date);
	}

	/**
	 * @param redisManager
	 * @param enclosureId
	 */
	private void deleteFiles(String enclosureId) {
		String keyFiles = RedisKeysEnum.FT_FILES_IDS.getKey(enclosureId);
		// list files
		List<String> listFileIds = redisManager.lrange(keyFiles, 0, -1);
		// delete Hash files info
		LOGGER.debug("clean up files: {}", RedisKeysEnum.FT_FILES_IDS.getKey(enclosureId));
		for (String fileId : listFileIds) {
			redisManager.deleteKey(RedisKeysEnum.FT_FILE.getKey(fileId));
			LOGGER.debug("clean up file: {}", RedisKeysEnum.FT_FILE.getKey(fileId));
		}
		// delete list of files
		redisManager.deleteKey(keyFiles);
	}

	private void deleteRootFiles(String enclosureId) {
		String keyRootFiles = RedisKeysEnum.FT_ROOT_FILES.getKey(enclosureId);
		// list root-files
		List<String> listRootFileIds = redisManager.lrange(keyRootFiles, 0, -1);
		// delete Hash root-files info
		LOGGER.debug("clean up root-files: {}", RedisKeysEnum.FT_ROOT_FILES.getKey(enclosureId));
		for (String rootFileId : listRootFileIds) {
			redisManager.deleteKey(
					RedisKeysEnum.FT_ROOT_FILE.getKey(RedisUtils.generateHashsha1(enclosureId + ":" + rootFileId)));
			LOGGER.debug("clean up root-file: {}", RedisKeysEnum.FT_ROOT_FILE.getKey(rootFileId));
		}
		// delete list of root-files
		redisManager.deleteKey(keyRootFiles);
	}

	private void deleteRootDirs(String enclosureId) {
		String keyrootDirs = RedisKeysEnum.FT_ROOT_DIRS.getKey(enclosureId);
		// list root-dirs
		List<String> listRootDirIds = redisManager.lrange(keyrootDirs, 0, -1);
		// delete Hash root-dirs info
		LOGGER.debug("clean up root-dirs: {}", RedisKeysEnum.FT_ROOT_DIRS.getKey(enclosureId));
		for (String rootDirId : listRootDirIds) {
			// redisManager.hmgetAllString(RedisKeysEnum.FT_ROOT_DIR.getKey(RedisUtils.generateHashsha1(enclosureId
			// + ":" + rootDirId)))
			redisManager.deleteKey(
					RedisKeysEnum.FT_ROOT_DIR.getKey(RedisUtils.generateHashsha1(enclosureId + ":" + rootDirId)));
			LOGGER.debug("clean up root-dir: {}", RedisKeysEnum.FT_ROOT_DIR.getKey(rootDirId));
		}
		// delete list of root-dirs
		redisManager.deleteKey(keyrootDirs);
	}

	private void deleteListPartEtags(String enclosureId) {
		// list files
		List<String> listFileIds = redisManager.lrange(RedisKeysEnum.FT_FILES_IDS.getKey(enclosureId), 0, -1);
		// delete list part-etags
		for (String fileId : listFileIds) {
			redisManager.deleteKey(RedisKeysEnum.FT_PART_ETAGS.getKey(fileId));
			LOGGER.debug("clean part-etags {}", RedisKeysEnum.FT_PART_ETAGS.getKey(fileId));
			String filekey = RedisKeysEnum.FT_ENCLOSURE_UPLOAD_FILE.getKey(enclosureId) + fileId;
			LOGGER.debug("clean up enclosure {} filekey {}", enclosureId, filekey);
			redisManager.deleteKey(filekey);
		}
	}

	private void deleteListIdContainer(String enclosureId) {
		// list files
		List<String> listFileIds = redisManager.lrange(RedisKeysEnum.FT_FILES_IDS.getKey(enclosureId), 0, -1);
		// delete list id container
		for (String fileId : listFileIds) {
			redisManager.deleteKey(RedisKeysEnum.FT_ID_CONTAINER.getKey(fileId));
			LOGGER.debug("clean id container {}", RedisKeysEnum.FT_ID_CONTAINER.getKey(fileId));
		}
	}

	/**
	 * @param redisManager
	 * @param enclosureId
	 * @throws WorkerException
	 */
	private void deleteListAndHashRecipients(String enclosureId) throws WorkerException {
		try {
			// Map recipients exemple : "charles.domenech@drac-idf.culture.gouv.fr":
			// "93e86440-fc67-4d71-9f74-fe17325e946a",
			Map<String, String> mapRecipients = RedisUtils.getRecipientsEnclosure(redisManager, enclosureId);
			for (String recipientId : mapRecipients.values()) {
				// delete Hash recipient info
				redisManager.deleteKey(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId));
			}
			// delete Hash recipients info
			redisManager.deleteKey(RedisKeysEnum.FT_RECIPIENTS.getKey(enclosureId));
		} catch (Exception e) {
			throw new WorkerException(e);
		}
	}

	/**
	 * Delete directory from uri
	 *
	 * @param path
	 */
	public void deleteEnclosureTempDirectory(String path) {
		LOGGER.info(" clean up Enclosure temp directory {} ", path);
		FileUtils.deleteQuietly(new File(path));
	}

	public void deleteBucketOutOfTime() throws StorageException {

		LocalDateTime now = LocalDateTime.now();
		for (int i = 0; i < 7; i++) {
			try {
				String buckName = bucketPrefix + now.format(DATE_FORMAT_BUCKET);
				storageManager.createBucket(buckName);
			} catch (Exception e) {
				LOGGER.debug("Error while creating bucket : " + e.getMessage(), e);
			}
			now = now.plusDays(1L);
		}

		List<Bucket> listeBucket = storageManager.listBuckets();
		listeBucket.forEach(bucket -> {
			try {
				String bucketDate = bucket.getName().substring(bucketPrefix.length());
				LocalDate date = LocalDate.parse(bucketDate, DATE_FORMAT_BUCKET);
				if (date.plusDays(maxUpdateDate).isBefore(LocalDate.now())
						&& bucket.getName().startsWith(bucketPrefix)) {
					try {
						deleteContentBucket(bucket.getName());
					} catch (StorageException e) {
						LOGGER.error("unable to delete content of bucket {} ", bucket.getName(), e.getMessage(), e);
					}
					try {
						storageManager.deleteBucket(bucket.getName());
					} catch (StorageException e) {
						LOGGER.error("unable to delete bucket {} ", bucket.getName(), e.getMessage(), e);
					}
				}
			} catch (Exception e) {
				LOGGER.error("cannot parse bucket date {} ", bucket.getName(), e.getMessage(), e);
			}
		});

		try {
			LOGGER.info("Clean export bucket");
			storageManager.deleteFilesWithPrefix(bucketExport, "");
		} catch (Exception e) {
			LOGGER.error("cannot delete export bucket content {} ", bucketExport, e);
		}

	}

	public void deleteContentBucket(String bucketName) throws StorageException, RetryException {
		ArrayList<String> objectListing = storageManager.listBucketContent(bucketName);

		objectListing.forEach(file -> {
			try {
				storageManager.deleteFilesWithPrefix(bucketName, file);
			} catch (RetryException e) {
				LOGGER.error("unable to delete file {} ", file, e.getMessage(), e);
			}
		});
	}

	//
	public void resetPasswordTryCount() throws WorkerException {

		redisManager.smembersString(RedisKeysEnum.FT_ENCLOSURE_DATES.getKey("")).forEach(date -> {
			redisManager.smembersString(RedisKeysEnum.FT_ENCLOSURE_DATE.getKey(date)).forEach(enclosureId -> {
				try {

					Map<String, String> mapRecipients = RedisUtils.getRecipientsEnclosure(redisManager, enclosureId);
					for (String recipientId : mapRecipients.values()) {
						redisManager.hsetString(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId),
								RecipientKeysEnum.PASSWORD_TRY_COUNT.getKey(), "0", -1);
					}

				} catch (Exception e) {
					LOGGER.error("Cannot initiat counter {} : " + e.getMessage(), enclosureId, e);
				}
			});
		});
	}

}
