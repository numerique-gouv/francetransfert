package fr.gouv.culture.francetransfert.services.glimps;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

import fr.gouv.culture.francetransfert.core.enums.GlimpsHealthCheckEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.exception.RetryGlimpsException;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.model.GlimpsInitResponse;
import fr.gouv.culture.francetransfert.model.GlimpsResultResponse;
import fr.gouv.culture.francetransfert.model.ScanInfo;
import fr.gouv.culture.francetransfert.security.WorkerException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GlimpsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlimpsService.class);

	@Value("${glimps.scan.url}")
	private String glimpsScanUrl;

	@Value("${glimps.check.url}")
	private String glimpsCheckUrl;

	@Value("${glimps.auth.token.key}")
	private String glimpsTokenKey;

	@Value("${glimps.auth.token.value}")
	private String glimpsTokenValue;

	@Value("${glimps.delay.seconds:120}")
	private int glimpsDelay;

	@Value("${glimps.send.sleep:500}")
	private int sendSleep;

	@Value("${glimps.send.modulo:1}")
	private int sendModulo;

	@Value("${glimps.enabled:false}")
	private boolean glimpsEnabled;

	@Value("${glimps.knownCode}")
	private List<String> knownCode;

	@Value("${glimps.allowCode:4001}")
	private List<String> allowCode;

	@Value("${glimps.maxTry:10}")
	private Long glipmsMaxTry;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	RedisManager redisManager;

	@Value("${tmp.folder.path}")
	private String tmpFolderPath;

	@Value("${check.file.path}")
	private String checkFilePath;

	public boolean glimpsUnitCheck(String enclosureId, String glimpsJson) {

		ScanInfo glimps = new Gson().fromJson(glimpsJson, ScanInfo.class);

		try {

			ResponseEntity<GlimpsResultResponse> templateReturn = glimpsCall(enclosureId, glimps.getUuid());

			GlimpsResultResponse ret = templateReturn.getBody();

			if ((ret.isDone() || StringUtils.isNotBlank(ret.getErrorCode()))
					&& templateReturn.getStatusCode().is2xxSuccessful()) {
				LOGGER.info("Glimps scan done for enclosure {} - glimpsId {} - filename {}", enclosureId,
						glimps.getUuid(), glimps.getFilename());
				if ((StringUtils.isNotBlank(ret.getErrorCode()) && !allowCode.contains(ret.getErrorCode())) || !ret.isStatus()) {
					LOGGER.error("Error while scanning enclosure {} file {} / {}, Body : {}", enclosureId,
							glimps.getUuid(), glimps.getFilename(), templateReturn.getBody());
					boolean fatalError = false;
					fatalError = knownCode.contains(ret.getErrorCode());
					glimps.setError(true);
					glimps.setFatalError(fatalError);
					if (!knownCode.contains(ret.getErrorCode())) {
						glimps.setErrorCode("unknown");
					} else {
						glimps.setErrorCode(ret.getErrorCode());
					}
					String jsonInString = new Gson().toJson(glimps);
					redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE_VIRUS.getKey(enclosureId), glimps.getUuid(),
							jsonInString, -1);
					redisManager.hdel(RedisKeysEnum.FT_ENCLOSURE_SCAN.getKey(enclosureId), glimps.getUuid());
					return false;
				} else if (ret.isMalware()) {
					LOGGER.error("Virus found for enclosure {} in file {} / {}", enclosureId, glimps.getUuid(),
							glimps.getFilename());
					glimps.setVirus(true);
					String jsonInString = new Gson().toJson(glimps);
					redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE_VIRUS.getKey(enclosureId), glimps.getUuid(),
							jsonInString, -1);
					redisManager.hdel(RedisKeysEnum.FT_ENCLOSURE_SCAN.getKey(enclosureId), glimps.getUuid());
					return false;
				} else {
					redisManager.hdel(RedisKeysEnum.FT_ENCLOSURE_SCAN.getKey(enclosureId), glimps.getUuid());
				}
			} else if (!templateReturn.getStatusCode().is2xxSuccessful()) {
				LOGGER.error("Error while fetching glimps for enclosure {} and file {} with statusCode {}", enclosureId,
						glimps.getUuid(), templateReturn.getStatusCode());
			}
		} catch (Exception e) {
			LOGGER.error("Error while fetching glimps for enclosure {} and file {} ", enclosureId, glimps.getUuid(), e);
			return false;
		}

		return true;
	}

	private ResponseEntity<GlimpsResultResponse> glimpsCall(String enclosureId, String uuid) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(glimpsTokenKey, glimpsTokenValue);

		LOGGER.info("Checking Glimps check for enclosure {} and uuid {}", enclosureId, uuid);
		LOGGER.debug("Glimps url : {}", glimpsCheckUrl + uuid);
		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
		ResponseEntity<GlimpsResultResponse> templateReturn = restTemplate.exchange(glimpsCheckUrl + uuid,
				HttpMethod.GET, requestEntity, GlimpsResultResponse.class);
		return templateReturn;
	}

	/**
	 * Check all remaining file from enclosure to glimps
	 * 
	 * @param enclosureId enclosureId
	 * @return
	 */
	public boolean checkGlipms(String enclosureId) {

		boolean isClean = true;
		Map<String, String> scanJsonList = redisManager
				.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE_SCAN.getKey(enclosureId));
		isClean = scanJsonList.values().stream().map(glimpsJson -> glimpsUnitCheck(enclosureId, glimpsJson))
				.allMatch(x -> x);

		return isClean;
	}

	/**
	 * Check file uuid from glimps
	 * 
	 * @param enclosureId enclosureId
	 * @param glimpsJson  Glimps Json string from redis
	 * @return true if file is clean or unchecked, false if error or virus
	 */

	/**
	 * Sort file and send to glimps
	 * 
	 * @param list        list of file to send to glimps
	 * @param enclosureId enclosureId
	 */
	public void sendToGlipms(ArrayList<String> list, String enclosureId) {

		// Create file for length and glimps upload
		int idx = 0;
		list.stream().map(x -> {
			if (!x.endsWith(File.separator) && !x.endsWith("\\") && !x.endsWith("/")) {
				String baseFolderName = getBaseFolderName();
				return new File(baseFolderName + x);
			} else {
				return null;
			}
			// remove null and sort
		}).filter(Objects::nonNull).sorted(Comparator.comparing(File::length, Comparator.reverseOrder()))
				.forEach(file -> uploadGlimps(file, enclosureId));
	}

	/**
	 * Upload file to glimps
	 * 
	 * @param file        file to send
	 * @param enclosureId enclosureId
	 */
	public void uploadGlimps(File file, String enclosureId) {

		HttpHeaders headers = new HttpHeaders();
		headers.set(glimpsTokenKey, glimpsTokenValue);
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		String currentfileName = file.getPath().replace(getBaseFolderNameWithEnclosurePrefix(enclosureId), "");
		String hash = "UNKNOWN";
		try (FileInputStream fs = new FileInputStream(file)) {

			hash = RedisUtils.generateHashSha256(fs);

			if (checkBeforeUpload(enclosureId, hash, currentfileName)) {

				LOGGER.info("Hash already uploded {}",hash);
				ScanInfo rec = ScanInfo.builder().filename(currentfileName).uuid(hash).build();
				String jsonInString = new Gson().toJson(rec);
				redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE_SCAN.getKey(enclosureId), hash, jsonInString, -1);

			} else {
				MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
				body.add("file", new FileSystemResource(file));
				HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
				LOGGER.debug("Start sending file to glimps for enclosure {} - fileName {} - hash {}", enclosureId,
						currentfileName, hash);
				ResponseEntity<GlimpsInitResponse> initResponse = restTemplate.exchange(glimpsScanUrl, HttpMethod.POST,
						requestEntity, GlimpsInitResponse.class);
				String uuid = "";

				if (StringUtils.isNotBlank(initResponse.getBody().getUuid())) {
					uuid = initResponse.getBody().getUuid();
				} else {
					uuid = initResponse.getBody().getId();
				}

				if (StringUtils.isBlank(uuid)) {
					LOGGER.error("Glimps body : {}", initResponse.getBody());
					throw new WorkerException(
							"Not uuid for glimps file scan enclosure : " + enclosureId + ", file : " + currentfileName);
				}

				ScanInfo rec = ScanInfo.builder().filename(currentfileName).uuid(uuid).build();
				String jsonInString = new Gson().toJson(rec);
				LOGGER.info("File sended to glimps for enclosure {} - fileName {} - glimpsId {} - hash {}", enclosureId,
						currentfileName, rec.getUuid(), hash);
				redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE_SCAN.getKey(enclosureId), rec.getUuid(),
						jsonInString, -1);
				Thread.sleep(sendSleep);
			}

		} catch (HttpClientErrorException | HttpServerErrorException re) {
			LOGGER.error("Hash : {}, Glimps body : {}, Status : {}, Enclosure: {}", hash, re.getResponseBodyAsString(),
					re.getStatusText(), enclosureId);
			LOGGER.error("Error while sending to glimps for enclosure " + enclosureId, re);
			ScanInfo glimps = ScanInfo.builder().filename(currentfileName).fatalError(true).error(true)
					.errorCode("unknown").uuid("NONE").build();
			String jsonInString = new Gson().toJson(glimps);
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE_VIRUS.getKey(enclosureId), "NONE", jsonInString, -1);
			throw new RetryGlimpsException("Error while sending to glimps enclosure :" + enclosureId, re);
		} catch (Exception e) {
			LOGGER.error("Error while sending file " + currentfileName + " with hash " + hash
					+ " to glimps for enclosure " + enclosureId, e);
			ScanInfo glimps = ScanInfo.builder().filename(currentfileName).fatalError(true).error(true)
					.errorCode("unknown").uuid("NONE").build();
			String jsonInString = new Gson().toJson(glimps);
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE_VIRUS.getKey(enclosureId), "NONE", jsonInString, -1);
			throw new RetryGlimpsException("Error while sending to glimps enclosure :" + enclosureId, e);
		}
	}

	private boolean checkBeforeUpload(String enclosureId, String hash, String currentfileName) {
		LOGGER.debug("enclosure {} - Checking file {} - before upload {}", enclosureId, currentfileName, hash);
		try {
			ResponseEntity<GlimpsResultResponse> glpRet = glimpsCall(enclosureId, hash);
			if (glpRet.getStatusCode().isError()) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
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

	public void healthCheckGlimps() {
		Boolean glimpsStat = true;
		HttpHeaders headers = new HttpHeaders();
		headers.set(glimpsTokenKey, glimpsTokenValue);
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		File file = new File(checkFilePath);
		try (FileInputStream fs = new FileInputStream(file)) {
			redisManager.setString(GlimpsHealthCheckEnum.SEND_AT.getKey(), DateTime.now().toString());
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("file", new FileSystemResource(file));
			HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
			LOGGER.debug("Start sending check files to glimps");
			ResponseEntity<GlimpsInitResponse> initResponse = restTemplate.exchange(glimpsScanUrl, HttpMethod.POST,
					requestEntity, GlimpsInitResponse.class);
			String uuid = "";

			if (StringUtils.isNotBlank(initResponse.getBody().getUuid())) {
				uuid = initResponse.getBody().getUuid();
			} else {
				uuid = initResponse.getBody().getId();
			}

			if (StringUtils.isBlank(uuid)) {
				LOGGER.error("Fail Glimps dummy check body {}", initResponse.getBody());
				redisManager.setString(GlimpsHealthCheckEnum.STATE.getKey(), Boolean.FALSE.toString());
				throw new Exception("Fail Glimps dummy check body " + initResponse.getBody());
			} else {
				LOGGER.debug("Glimps Check Ok");
			}
		} catch (Exception e) {
			LOGGER.error("Fail Glimps dummy check", e);
			glimpsStat = false;
			redisManager.setString(GlimpsHealthCheckEnum.STATE_REAL.getKey(), glimpsStat.toString());
			if (glimpsEnabled) {
				redisManager.setString(GlimpsHealthCheckEnum.STATE.getKey(), glimpsStat.toString());
			} else {
				redisManager.setString(GlimpsHealthCheckEnum.STATE.getKey(), Boolean.TRUE.toString());
			}
			return;
		}
		redisManager.setString(GlimpsHealthCheckEnum.STATE_REAL.getKey(), glimpsStat.toString());
		if (glimpsEnabled) {
			redisManager.setString(GlimpsHealthCheckEnum.STATE.getKey(), glimpsStat.toString());
		} else {
			redisManager.setString(GlimpsHealthCheckEnum.STATE.getKey(), Boolean.TRUE.toString());
		}
	}

}
