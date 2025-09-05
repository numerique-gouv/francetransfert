/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.zipworker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.model.S3Object;
import com.google.gson.Gson;
import java.time.format.DateTimeFormatter;

import fr.gouv.culture.francetransfert.core.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.GlimpsHealthCheckEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisQueueEnum;
import fr.gouv.culture.francetransfert.core.enums.SourceEnum;
import fr.gouv.culture.francetransfert.core.enums.StatutEnum;
import fr.gouv.culture.francetransfert.core.enums.TypeStat;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.exception.RetryException;
import fr.gouv.culture.francetransfert.core.exception.RetryGlimpsException;
import fr.gouv.culture.francetransfert.core.exception.StorageException;
import fr.gouv.culture.francetransfert.core.services.MimeService;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.services.StorageManager;
import fr.gouv.culture.francetransfert.core.utils.Base64CryptoService;
import fr.gouv.culture.francetransfert.core.utils.DateUtils;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.exception.InvalidSizeTypeException;
import fr.gouv.culture.francetransfert.model.Enclosure;
import fr.gouv.culture.francetransfert.model.ScanInfo;
import fr.gouv.culture.francetransfert.security.WorkerException;
import fr.gouv.culture.francetransfert.services.clamav.ClamAVScannerManager;
import fr.gouv.culture.francetransfert.services.cleanup.CleanUpServices;
import fr.gouv.culture.francetransfert.services.glimps.GlimpsService;
import fr.gouv.culture.francetransfert.services.mail.notification.MailNotificationServices;
import fr.gouv.culture.francetransfert.services.mail.notification.enums.NotificationTemplateEnum;
import fr.gouv.culture.francetransfert.services.metrics.CustomMetricsService;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

@Service
@Slf4j
public class ZipWorkerServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZipWorkerServices.class);

	@Autowired
	StorageManager manager;

	@Autowired
	MimeService mimeService;

	@Autowired
	RedisManager redisManager;

	@Autowired
	StorageManager storageManager;

	@Autowired
	ClamAVScannerManager clamAVScannerManager;

	@Autowired
	CustomMetricsService customMetricsService;

	@Value("${tmp.folder.path}")
	private String tmpFolderPath;

	@Value("${bucket.prefix}")
	private String bucketPrefix;

	@Value("${scan.clamav.maxFileSize}")
	private long scanMaxFileSize;

	@Value("${subject.virus.sender}")
	private String subjectVirusFound;

	@Value("${subject.virus.error.sender}")
	private String subjectVirusError;

	@Value("${subject.virus.senderEn}")
	private String subjectVirusFoundEn;

	@Value("${subject.virus.error.senderEn}")
	private String subjectVirusErrorEn;

	@Value("${upload.limit}")
	private long maxEnclosureSize;

	@Value("${upload.file.limit}")
	private long maxFileSize;

	@Value("${glimps.enabled:false}")
	private boolean glimpsEnabled;

	@Value("${glimps.delay.seconds:30}")
	private int glimpsDelay;

	@Value("${glimps.error.delay.seconds:60}")
	private int glimpsErrorDelay;

	@Value("${glimps.maxTry:10}")
	private Long glipmsMaxTry;

	@Autowired
	MailNotificationServices mailNotificationService;

	@Autowired
	CleanUpServices cleanUpServices;

	@Autowired
	Base64CryptoService base64CryptoService;

	@Autowired
	GlimpsService glimpsService;

	@Autowired
	@Qualifier("downloadExecutor")
	Executor downloadExecutor;

	@Value("${downloadExecutor.enabled:false}")
	private boolean downloadExecutorEnabled;

	List<String> inProgressList = Arrays.asList(StatutEnum.ANA.getCode(), StatutEnum.CHT.getCode());

	public void startZip(String enclosureId) throws MetaloadException, StorageException {

		String bucketName = RedisUtils.getBucketName(redisManager, enclosureId, bucketPrefix);
		Enclosure enclosure = Enclosure.build(enclosureId, redisManager);

		try {

			ArrayList<String> list = manager.getUploadedEnclosureFiles(bucketName, enclosureId);
			LOGGER.debug(" STEP STATE ZIP ");
			LOGGER.debug(" SIZE " + list.size() + " LIST ===> " + list.toString());

			boolean glimpsState = Boolean
					.parseBoolean(redisManager.getString(GlimpsHealthCheckEnum.STATE_REAL.getKey()));

			LOGGER.debug(" glimpsState {} and  glimpsEnabled {}", glimpsState, glimpsEnabled);

			boolean finishedScan = false;
			boolean isClean = false;

			String encStatut = enclosure.getStatut();

			if (glimpsEnabled && !glimpsState) {
				LOGGER.error("Glimps in error fall back to CLAMAV for enclosure {}", enclosureId);
			}

			if (StatutEnum.CHT.getCode().equals(encStatut)) {

				LOGGER.info("[Worker] Start scan process for enclosur N°  {}", enclosureId);

				LOGGER.debug(" start copy files temp to disk and scan for vulnerabilities {} / {} - {} ++ {} ",
						bucketName, list, enclosureId, bucketPrefix);

				downloadFilesToTempFolder(manager, bucketName, list, enclosureId);
				sizeCheck(list);

				if (glimpsEnabled && glimpsState) {
					glimpsService.sendToGlipms(list, enclosureId);
					redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
							EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.ANA.getCode(), -1);
					redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
							EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.ANA.getWord(), -1);
					encStatut = StatutEnum.ANA.getCode();
				} else {
					isClean = performScan(list, enclosureId);
					finishedScan = true;
				}
			}

			if (StatutEnum.ANA.getCode().equals(encStatut)) {
				if (glimpsState) {
					String lastGlimpsCheckStr = redisManager
							.getString(RedisKeysEnum.FT_ENCLOSURE_SCAN_DELAY.getKey(enclosureId));
					if (StringUtils.isBlank(lastGlimpsCheckStr) || LocalDateTime.parse(lastGlimpsCheckStr)
							.plusSeconds(glimpsDelay).isBefore(LocalDateTime.now())) {
						LOGGER.info("Checking glimps for enclosure {}", enclosureId);

						LOGGER.debug("lastGlimpsCheckStr {} and glimpsDelay {}", lastGlimpsCheckStr, glimpsDelay);

						isClean = glimpsService.checkGlipms(enclosureId);
						if (CollectionUtils.isEmpty(redisManager
								.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE_SCAN.getKey(enclosureId)).values())) {
							finishedScan = true;
						}
						if (!isClean) {
							Map<String, String> scanJsonList = redisManager
									.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE_VIRUS.getKey(enclosureId));
							boolean toStop = scanJsonList.values().stream().map(json -> {
								return new Gson().fromJson(json, ScanInfo.class);
							}).anyMatch(x -> x.isError() || x.isVirus());
							Long tryCount = redisManager
									.incrBy(RedisKeysEnum.FT_ENCLOSURE_SCAN_RETRY.getKey(enclosureId), 1);
							if (toStop || (tryCount >= glipmsMaxTry)) {
								isClean = false;
								finishedScan = true;
								LOGGER.error("Too many Glimps error or virus for enclosure {} ", enclosureId);
								redisManager.deleteKey(RedisKeysEnum.FT_ENCLOSURE_SCAN.getKey(enclosureId));
							} else {
								LOGGER.error("Retry error waiting before next call for enclosure {}", enclosureId);
								isClean = true;
								finishedScan = false;
							}
						}
						redisManager.setString(RedisKeysEnum.FT_ENCLOSURE_SCAN_DELAY.getKey(enclosureId),
								LocalDateTime.now().toString());
					} else {
						LOGGER.debug("Waiting before next call for enclosure {}", enclosureId);
						isClean = true;
						finishedScan = false;
					}
				} else {
					LOGGER.error("Glimps in error for check enclosure {} putting enclosure back to CHT", enclosureId);
					isClean = true;
					finishedScan = false;
					redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
							EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.CHT.getCode(), -1);
					redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
							EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.CHT.getWord(), -1);
				}
			}

			if (isClean && finishedScan) {
				LOGGER.info("Finished scan start zipping for enclosure {}", enclosureId);

				if (StatutEnum.ANA.getCode().equals(encStatut)) {
					downloadFilesToTempFolder(manager, bucketName, list, enclosureId);
				}

				String passwordRedis = RedisUtils.getEnclosureValue(redisManager, enclosure.getGuid(),
						EnclosureKeysEnum.PASSWORD.getKey());

				String zipPassword = RedisUtils.getEnclosureValue(redisManager, enclosure.getGuid(),
						EnclosureKeysEnum.PASSWORD_ZIP.getKey());

				String passwordUnHashed = base64CryptoService.aesDecrypt(passwordRedis);

				LOGGER.info("Start zip for enclosure {}", enclosureId);
				zipDownloadedContent(enclosureId, passwordUnHashed, zipPassword);

				LOGGER.info("Start upload zip for enclosure {}", enclosureId);
				uploadZippedEnclosure(bucketName, manager, manager.getZippedEnclosureName(enclosureId),
						getBaseFolderNameWithZipPrefix(enclosureId));

				LOGGER.debug(" add hashZipFile to redis");
				addHashFilesToMetData(enclosureId, getHashFromS3(enclosureId));

				File fileToDelete = new File(getBaseFolderNameWithEnclosurePrefix(enclosureId));
				LOGGER.debug(" start delete zip file in local disk");
				deleteFilesFromTemp(fileToDelete);
				File fileZip = new File(getBaseFolderNameWithZipPrefix(enclosureId));
				if (!fileZip.delete()) {
					throw new WorkerException("error delete zip file");
				}
				LOGGER.debug(" start delete zip file in OSU");
				try {
					deleteFilesFromOSU(manager, bucketName, enclosureId);
				} catch (RetryException rse) {
					LOGGER.error("Cannot delete temporary file for complete enclosure NOT FAILING", rse);
				}

				notifyEmailWorker(enclosureId);
				RedisUtils.updateListOfPliSent(redisManager, enclosure.getSender(), enclosureId);
				if (!CollectionUtils.isEmpty(enclosure.getRecipients())) {
					RedisUtils.updateListOfPliReceived(redisManager,
							enclosure.getRecipients().stream().map(x -> x.getMail()).collect(Collectors.toList()),
							enclosureId);
				}

				String statMessage = TypeStat.UPLOAD + ";" + enclosureId;
				redisManager.publishFT(RedisQueueEnum.STAT_QUEUE.getValue(), statMessage);

				redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
						EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.APT.getCode(), -1);
				redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
						EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.APT.getWord(), -1);

				LocalDateTime depotDate = LocalDateTime.now();
				String depotDateString = depotDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
				LOGGER.debug("enclosure update depotDate : {}", depotDateString);

				LocalDateTime localDateTime = DateUtils.convertStringToLocalDateTime(redisManager.getHgetString(
						RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId), EnclosureKeysEnum.TIMESTAMP.getKey()));

				String uploadDateString = localDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

				if (StringUtils.equalsIgnoreCase(depotDateString, uploadDateString)) {
					redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
							EnclosureKeysEnum.TIMESTAMP.getKey(), depotDate.toString(), -1);
				}

				LOGGER.debug(" STEP STATE ZIP OK");

				String endUploadDate = redisManager.hget(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
						EnclosureKeysEnum.UPLOADED_TIMESTAMP.getKey());

				if (StringUtils.isNotBlank(endUploadDate)) {
					LocalDateTime uploadTime = LocalDateTime
							.parse(redisManager.hget(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
									EnclosureKeysEnum.UPLOADED_TIMESTAMP.getKey()));

					LOGGER.warn("Finish enclosure {} in seconds | {} ", enclosureId,
							ChronoUnit.SECONDS.between(uploadTime, depotDate));

					customMetricsService.incrementFinishedPliCounter();
					customMetricsService.recordPliTime(ChronoUnit.SECONDS.between(uploadTime, depotDate),
							TimeUnit.SECONDS);
				}

			} else if (!isClean && finishedScan) {

				redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
						EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.EAV.getCode(), -1);
				redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
						EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.EAV.getWord(), -1);
				Map<String, String> scanJsonList = redisManager
						.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE_VIRUS.getKey(enclosureId));
				List<ScanInfo> scanList = scanJsonList.values().stream().map(json -> {
					return new Gson().fromJson(json, ScanInfo.class);
				}).collect(Collectors.toList());
				enclosure.getVirusScan().clear();
				enclosure.getVirusScan().addAll(scanList);
				enclosure.getVirusScan().stream().findFirst().ifPresentOrElse(x -> {
					enclosure.setErrorCode(x.getErrorCode());
				}, () -> {
					enclosure.setErrorCode("unknown");
				});
				if (scanList.stream().anyMatch(ScanInfo::isFatalError)) {
					cleanUpEnclosure(bucketName, enclosureId, enclosure,
							NotificationTemplateEnum.MAIL_VIRUS_ERROR_SENDER.getValue(), subjectVirusError);
				} else if (scanList.stream().anyMatch(ScanInfo::isVirus)) {
					LOGGER.warn("msgtype: VIRUS || enclosure: {} || sender: {}", enclosure.getGuid(),
							enclosure.getSender());
					cleanUpEnclosure(bucketName, enclosureId, enclosure,
							NotificationTemplateEnum.MAIL_VIRUS_SENDER.getValue(), subjectVirusFound);
				} else {
					cleanUpEnclosure(bucketName, enclosureId, enclosure,
							NotificationTemplateEnum.MAIL_VIRUS_INDISP_SENDER.getValue(), subjectVirusError);
				}
			} else if (!finishedScan && StringUtils.isNotBlank(encStatut) &&
					inProgressList.contains(encStatut)) {
				LOGGER.debug("Scan in progress for enclosure {}", enclosureId);
				redisManager.publishFT(RedisQueueEnum.ZIP_QUEUE.getValue(), enclosure.getGuid());
			} else {
				LOGGER.error("Enclosure {} is in wrong state {} NOT putting enclosure back to queue", enclosureId,
						encStatut);
				cleanUpEnclosure(bucketName, enclosureId, enclosure,
						NotificationTemplateEnum.MAIL_VIRUS_INDISP_SENDER.getValue(), subjectVirusError);
			}
		} catch (RetryException retryE) {
			Long tryCount = redisManager.incrBy(RedisKeysEnum.FT_ENCLOSURE_SCAN_RETRY.getKey(enclosureId), 1);
			if (tryCount < glipmsMaxTry) {
				LOGGER.error("Retry StorageError putting back enclosure to queue - {}", enclosureId, retryE);
				redisManager.publishFT(RedisQueueEnum.ZIP_QUEUE.getValue(), enclosure.getGuid());
				cleanUpServices.deleteEnclosureTempDirectory(getBaseFolderNameWithEnclosurePrefix(enclosureId));
			} else {
				LOGGER.error("Too many retry StorageError while sending zip to S3 for enclosure {}", enclosureId,
						retryE);
				cleanUpEnclosure(bucketName, enclosureId, enclosure,
						NotificationTemplateEnum.MAIL_ERROR_SENDER.getValue(), subjectVirusError);
			}
		} catch (InvalidSizeTypeException sizeEx) {
			enclosure.setFileError(sizeEx.getFile());
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
					EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.ETF.getCode(), -1);
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
					EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.ETF.getWord(), -1);
			LOGGER.error("Enclosure " + enclosure.getGuid() + " as invalid type or size : ", sizeEx);
			cleanUpEnclosure(bucketName, enclosureId, enclosure,
					NotificationTemplateEnum.MAIL_INVALID_ENCLOSURE_SENDER.getValue(), subjectVirusError);
		} catch (RetryGlimpsException exGlimps) {
			Long tryCount = redisManager.incrBy(RedisKeysEnum.FT_ENCLOSURE_SCAN_RETRY.getKey(enclosureId), 1);
			if (tryCount < glipmsMaxTry) {
				LOGGER.error("Retry GlimpsError putting enclosure back to queue " + enclosureId + " : "
						+ exGlimps.getMessage(), exGlimps);
				cleanUpServices.deleteEnclosureTempDirectory(getBaseFolderNameWithEnclosurePrefix(enclosureId));
				// add 1mn delay if glimps error
				redisManager.setString(RedisKeysEnum.FT_ENCLOSURE_SCAN_DELAY.getKey(enclosureId),
						LocalDateTime.now().plus(glimpsErrorDelay, ChronoUnit.SECONDS).toString());
				redisManager.publishFT(RedisQueueEnum.ZIP_QUEUE.getValue(), enclosure.getGuid());
			} else {
				LOGGER.error("Too many Retry GlimpsError for enclosure " + enclosureId + " : " + exGlimps.getMessage(),
						exGlimps);
				redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
						EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.EAV.getCode(), -1);
				redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
						EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.EAV.getWord(), -1);
				cleanUpEnclosure(bucketName, enclosureId, enclosure,
						NotificationTemplateEnum.MAIL_VIRUS_INDISP_SENDER.getValue(), subjectVirusError);
			}
		} catch (Exception e) {
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
					EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.EAV.getCode(), -1);
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
					EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.EAV.getWord(), -1);
			LOGGER.error("Error in zip process for enclosure " + enclosureId + " : " + e.getMessage(), e);
			Map<String, String> scanJsonList = redisManager
					.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE_VIRUS.getKey(enclosureId));
			List<ScanInfo> scanList = scanJsonList.values().stream().map(json -> {
				return new Gson().fromJson(json, ScanInfo.class);
			}).collect(Collectors.toList());
			enclosure.getVirusScan().clear();
			enclosure.getVirusScan().addAll(scanList);
			enclosure.getVirusScan().stream().findFirst().ifPresentOrElse(x -> {
				enclosure.setErrorCode(x.getErrorCode());
			}, () -> {
				enclosure.setErrorCode("unknown");
			});
			cleanUpEnclosure(bucketName, enclosureId, enclosure, NotificationTemplateEnum.MAIL_ERROR_SENDER.getValue(),
					subjectVirusError);
		} finally {
			File fileToDelete = new File(getBaseFolderNameWithEnclosurePrefix(enclosureId));
			LOGGER.debug("start delete temp folder for enclosure {}", enclosureId);
			if (fileToDelete.exists()) {
				deleteFilesFromTemp(fileToDelete);
			}
		}
	}

	private String getHashFromS3(String enclosureId) throws MetaloadException, StorageException {
		String bucketName = RedisUtils.getBucketName(redisManager, enclosureId, bucketPrefix);
		String fileToDownload = storageManager.getZippedEnclosureName(enclosureId);
		String hashFileFromS3 = storageManager.getEtag(bucketName, fileToDownload);
		return hashFileFromS3;
	}

	/*
	 * private void getContentMd5ForRedis(String prefix) throws IOException { File
	 * fileZip = new File(getBaseFolderNameWithZipPrefix(prefix)); FileInputStream
	 * fis = new FileInputStream(fileZip); byte[] content_bytes =
	 * IOUtils.toByteArray(fis); String md5 = new
	 * String(DigestUtils.md5Hex(content_bytes)); addHashFilesToMetData(prefix,md5);
	 * fis.close(); }
	 */

	public void addHashFilesToMetData(String enclosureId, String hashFile) {
		try {
			Map<String, String> tokenMap = redisManager
					.hmgetAllString(RedisKeysEnum.FT_ADMIN_TOKEN.getKey(enclosureId));
			if (tokenMap != null) {
				Map<String, String> enclosureMap = redisManager
						.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
				enclosureMap.put(EnclosureKeysEnum.HASH_FILE.getKey(), hashFile);
				redisManager.insertHASH(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId), enclosureMap);
			} else {
				throw new WorkerException("tokenMap from Redis is null");
			}
		} catch (Exception e) {
			throw new WorkerException("Unable to add hashFile to redis");
		}
	}

	private void notifyEmailWorker(String prefix) {
		redisManager.publishFT(RedisQueueEnum.MAIL_QUEUE.getValue(), prefix);
	}

	private void deleteFilesFromOSU(StorageManager manager, String bucketName, String prefix) throws RetryException {
		manager.deleteFilesWithPrefix(bucketName, prefix);
	}

	private void deleteFilesFromTemp(File file) {
		if (!FileUtils.deleteQuietly(file)) {
			LOGGER.error("unable to delete file");
		}
	}

	public void uploadZippedEnclosure(String bucketName, StorageManager manager, String fileName, String fileZipPath)
			throws RetryException {
		manager.uploadMultipartForZip(bucketName, fileName, fileZipPath);
	}

	private void zipDownloadedContent(String zippedFileName, String password, String zipPassword) throws IOException {

		if (zipPassword.equalsIgnoreCase("true")) {
			String sourceFile = getBaseFolderNameWithEnclosurePrefix(zippedFileName);
			try (FileOutputStream fos = new FileOutputStream(getBaseFolderNameWithZipPrefix(zippedFileName));
					ZipOutputStream zipOut = new ZipOutputStream(fos, password.toCharArray());) {
				File fileToZip = new File(sourceFile);
				for (File file : fileToZip.listFiles()) {
					zipFile(file, file.getName(), zipOut, true);
				}
				zipOut.flush();
				fos.flush();
			}
		} else {
			zipDownloadedContentWithoutPassword(zippedFileName);
		}

	}

	private void zipDownloadedContentWithoutPassword(String zippedFileName) throws IOException {
		String sourceFile = getBaseFolderNameWithEnclosurePrefix(zippedFileName);
		try (FileOutputStream fos = new FileOutputStream(getBaseFolderNameWithZipPrefix(zippedFileName));
				ZipOutputStream zipOut = new ZipOutputStream(fos);) {
			File fileToZip = new File(sourceFile);
			for (File file : fileToZip.listFiles()) {
				zipFile(file, file.getName(), zipOut, false);
			}
			zipOut.flush();
			fos.flush();
		}
	}

	private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut, boolean crypted)
			throws IOException {
		try {
			ZipParameters parameters = new ZipParameters();
			parameters.setCompressionMethod(CompressionMethod.DEFLATE);
			parameters.setCompressionLevel(CompressionLevel.NORMAL);
			if (crypted) {
				parameters.setEncryptFiles(true);
				parameters.setEncryptionMethod(EncryptionMethod.AES);
				parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
			} else {
				parameters.setEncryptFiles(false);
			}
			parameters.setFileNameInZip(fileName);
			if (fileToZip.isDirectory()) {
				if (fileName.endsWith(File.separator)) {
					zipOut.putNextEntry(parameters);
					zipOut.closeEntry();
				} else {
					parameters.setFileNameInZip(fileName + File.separator);
					zipOut.putNextEntry(parameters);
					zipOut.closeEntry();
				}
				File[] children = fileToZip.listFiles();
				for (File childFile : children) {
					LOGGER.debug(" start zip file {} temp to disk", childFile.getName());
					zipFile(childFile, fileName + File.separator + childFile.getName(), zipOut, crypted);
				}
				return;
			}
			try (FileInputStream fis = new FileInputStream(fileToZip)) {
				zipOut.putNextEntry(parameters);
				byte[] bytes = new byte[1024];
				int length;
				while ((length = fis.read(bytes)) >= 0) {
					zipOut.write(bytes, 0, length);
				}
				zipOut.closeEntry();
			}

		} catch (Exception e) {
			log.error("Error During ZipFile", e);
			throw new WorkerException("Error During ZipFile");
		}
	}

	private void downloadFilesToTempFolder(StorageManager manager, String bucketName, ArrayList<String> list,
			String enclosureId) throws RetryException {
		try {

			if (downloadExecutorEnabled) {
				parallelDownload(manager, bucketName, list, enclosureId);
			} else {
				sequentialDownload(manager, bucketName, list, enclosureId);
			}
		} catch (Exception e) {
			LOGGER.error("Error During File Dowload from OSU to Temp Folder : " + e.getMessage(), e);
			throw new RetryException("Error During File Dowload from OSU to Temp Folder ", e);
		}
	}

	private void parallelDownload(StorageManager manager, String bucketName, ArrayList<String> list,
			String enclosureId) throws RetryException {
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		LOGGER.info("Start download for enclosure {}", enclosureId);
		for (String fileName : list) {
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				S3Object object = null;
				try {
					object = manager.getObjectByName(bucketName, fileName);
					if (!fileName.endsWith(File.separator) && !fileName.endsWith("\\") && !fileName.endsWith("/")) {
						writeFile(object, fileName);
					}
				} catch (Exception e) {
					LOGGER.error("Error during file download {} : {}", fileName, e.getMessage(), e);
					throw new CompletionException(e);
				} finally {
					if (object != null) {
						try {
							object.close();
						} catch (IOException ioe) {
						}
					}
				}
			}, downloadExecutor);
			futures.add(future);
		}

		try {
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		} catch (CompletionException ce) {
			throw new RetryException("Error during parallel file download", ce.getCause());
		}
	}

	private void sequentialDownload(StorageManager manager, String bucketName, ArrayList<String> list,
			String enclosureId) throws RetryException {

		try {
			for (String fileName : list) {
				S3Object object = manager.getObjectByName(bucketName, fileName);
				if (!fileName.endsWith(File.separator) && !fileName.endsWith("\\") &&
						!fileName.endsWith("/")) {
					writeFile(object, fileName);
				}
				object.close();
			}
		} catch (Exception e) {
			LOGGER.error("Error during sequential file download", e);
			throw new RetryException("Error during sequential file download", e);
		}
	}

	/**
	 * @param object
	 * @param fileName
	 * @throws IOException
	 */
	public void writeFile(S3Object object, String fileName) throws IOException {
		LOGGER.info(" start download file : {}  to disk ", fileName);
		String baseFolderName = getBaseFolderName();
		File file = new File(baseFolderName + fileName);
		LOGGER.debug("file exists: {} and file length: {} and object length: {}", file.exists(), file.length(),
				object.getObjectMetadata().getContentLength());
		if (file.exists() && object.getObjectMetadata().getContentLength() == FileUtils.sizeOf(file)) {
			LOGGER.info(" file {} already exists, skipping download", fileName);
			object.getObjectContent().abort();
			return;
		}
		try (InputStream reader = new BufferedInputStream(object.getObjectContent());) {
			file.getParentFile().mkdirs();
			try (OutputStream writer = new BufferedOutputStream(new FileOutputStream(file));) {
				int read = -1;
				while ((read = reader.read()) != -1) {
					writer.write(read);
				}
				writer.flush();
			}
		}
	}

	/**
	 * Writing files into temp directory and scanning for vulnerabilities
	 *
	 * @param list
	 * @return
	 * @throws InvalidSizeTypeException
	 */
	private void sizeCheck(ArrayList<String> list) throws InvalidSizeTypeException {
		String currentFileName = null;
		long enclosureSize = 0;
		try {
			for (String fileName : list) {
				long currentSize = 0;
				currentFileName = fileName;
				if (!fileName.endsWith(File.separator) && !fileName.endsWith("\\") && !fileName.endsWith("/")) {
					String baseFolderName = getBaseFolderName();
					try (FileInputStream fileInputStream = new FileInputStream(baseFolderName + fileName);) {

						currentSize = fileInputStream.getChannel().size();

						enclosureSize += currentSize;

						checkSizeAndMimeType(currentFileName, enclosureSize, currentSize, fileInputStream);
					}
				}
			}
		} catch (InvalidSizeTypeException ex) {
			throw ex;
		} catch (Exception e) {
			LOGGER.error("Error lors du traitement du fichier {} : {}  ", currentFileName, e.getMessage(), e);
			throw new WorkerException("Error During File size check [" + currentFileName + "]");
		}
	}

	private boolean performScan(ArrayList<String> list, String enclosureId) throws InvalidSizeTypeException {
		boolean isClean = true;
		String currentFileName = null;
		LOGGER.info("Checking clamav for enclosure {}", enclosureId);
		try {
			for (String fileName : list) {

				if (!isClean) {
					break;
				}
				currentFileName = fileName;
				if (!fileName.endsWith(File.separator) && !fileName.endsWith("\\") && !fileName.endsWith("/")) {
					String baseFolderName = getBaseFolderName();
					try (FileInputStream fileInputStream = new FileInputStream(baseFolderName + fileName);) {

						FileChannel fileChannel = fileInputStream.getChannel();
						if (fileChannel.size() <= scanMaxFileSize) {
							String status = clamAVScannerManager.performScan(fileChannel);
							LOGGER.debug("Virus status: " + status);
							if (!StringUtils.equalsIgnoreCase("OK", status)) {
								isClean = false;
								ScanInfo glimps = ScanInfo.builder().error(false).fatalError(false).virus(true)
										.filename(currentFileName.replace(enclosureId + "/", "")).uuid(enclosureId)
										.build();
								String jsonInString = new Gson().toJson(glimps);
								redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE_VIRUS.getKey(enclosureId),
										glimps.getUuid(), jsonInString, -1);
							}
						}
						fileChannel.close();
					}
				}
			}
		} catch (Exception e) {
			ScanInfo glimps = ScanInfo.builder().error(true).fatalError(true)
					.filename(currentFileName.replace(enclosureId + "/", "")).uuid(enclosureId).build();
			String jsonInString = new Gson().toJson(glimps);
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE_VIRUS.getKey(enclosureId), glimps.getUuid(),
					jsonInString, -1);
			LOGGER.error("Error lors du traitement du fichier {} : {}  ", currentFileName, e.getMessage(), e);
			throw new WorkerException("Error During File scanning [" + currentFileName + "]");
		}

		return isClean;
	}

	private void checkSizeAndMimeType(String currentFileName, long enclosureSize, long currentSize,
			FileInputStream fileInputStream) throws IOException, InvalidSizeTypeException {
		if (!mimeService.isAuthorisedMimeTypeFromFile(fileInputStream)) {
			String mimetype = mimeService.getMimeTypeFromFile(fileInputStream);
			String file = StringUtils.substringAfterLast(currentFileName, "/");
			throw new InvalidSizeTypeException("File " + currentFileName + " as invalid mimetype : " + mimetype, file);
		}

		if (currentSize > maxFileSize || enclosureSize > maxEnclosureSize) {
			throw new InvalidSizeTypeException("File " + currentFileName + " or enclose is too big");
		}
	}

	/**
	 *
	 * @param bucketName
	 * @param prefix
	 */
	private void cleanUpEnclosure(String bucketName, String enclosureId, Enclosure enclosure, String emailTemplateName,
			String emailSubject) {
		try {
			// Notify sender
			Locale language = Locale.FRANCE;
			try {
				language = LocaleUtils.toLocale(RedisUtils.getEnclosureValue(redisManager, enclosure.getGuid(),
						EnclosureKeysEnum.LANGUAGE.getKey()));
			} catch (Exception eL) {
				LOGGER.error("Error while getting local", eL);
			}
			if (subjectVirusFound.equalsIgnoreCase(emailSubject)) {
				if (Locale.UK.equals(language)) {
					emailSubject = subjectVirusFoundEn;
				}

			} else if (subjectVirusError.equalsIgnoreCase(emailSubject)) {
				if (Locale.UK.equals(language)) {
					emailSubject = subjectVirusErrorEn;
				}
			}

			if (StringUtils.isNotBlank(enclosure.getSubject())) {
				emailSubject = emailSubject.concat(" : ").concat(enclosure.getSubject());
			}

			mailNotificationService.prepareAndSend(enclosure.getSender(), emailSubject, enclosure, emailTemplateName,
					language);
		} catch (Exception e) {
			LOGGER.error("Error while sending mail for Enclosure " + enclosure.getGuid() + " : " + e.getMessage(), e);
		} finally {
			customMetricsService.incrementFinishedPliCounter();
			try {
				/** Clean : OSU, REDIS, UPLOADER FOLDER, and NOTIFY SNDER **/
				LOGGER.info("Processing clean up for enclosure{} - {} / {} - {} ", enclosure.getGuid(), bucketName,
						enclosureId, bucketPrefix);
				LOGGER.debug("clean up OSU");
				deleteFilesFromOSU(manager, bucketName, enclosureId);

				// clean temp data in REDIS for Enclosure
				LOGGER.debug("clean up REDIS temp data");
				cleanUpServices.cleanUpEnclosureTempDataInRedis(enclosureId, true);

				LOGGER.debug("clean up REDIS");
				// Keep enclosure envelope for mail api check if from mail
				String sourceCode = RedisUtils.getEnclosure(redisManager, enclosureId)
						.get(EnclosureKeysEnum.SOURCE.getKey());
				if (SourceEnum.PUBLIC.getValue().equals(sourceCode)) {
					cleanUpServices.cleanUpEnclosurePartiallyCoreInRedis(enclosureId);
				} else {
					cleanUpServices.cleanUpEnclosureCoreInRedis(enclosureId);
				}
				// clean up for Upload directory
				cleanUpServices.deleteEnclosureTempDirectory(getBaseFolderNameWithEnclosurePrefix(enclosureId));
			} catch (Exception e) {
				LOGGER.error("Error while cleaning up Enclosure " + enclosure.getGuid() + " : " + e.getMessage(), e);
			}

		}
	}

	private String getBaseFolderName() {
		String baseString = tmpFolderPath;
		return baseString;
	}

	private String getBaseFolderNameWithEnclosurePrefix(String prefix) {
		String baseString = tmpFolderPath + prefix;
		return baseString;
	}

	private String getBaseFolderNameWithZipPrefix(String zippedFileName) {
		String baseString = tmpFolderPath + zippedFileName + ".zip";
		return baseString;
	}
}
