/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.referentiel;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReferentielServices {
	private static final Logger LOGGER = LoggerFactory.getLogger(ReferentielServices.class);

	@Value("${ref.domain.token}")
	private String domainToken;

	@Value("${ref.domain.url}")
	private String domainUrl;

	@Value("${ref.stat.url}")
	private String statUrl;

	@Value("${ref.stat.token}")
	private String statToken;

	@Value("${ref.stat.header}")
	private String statHeader;

	@Autowired
	private RestClientUtils restClientUtils;

	@Autowired
	private RedisManager redisManager;

	private final String CLEANUP_PATTERN = "(?m)^\\s*\\r?\\n|\\r?\\n\\s*(?!.*\\r?\\n)";

	@PreDestroy
	public void onDestroy() throws Exception {
		LOGGER.info("send stat before destroy");
		this.sendStats();
		LOGGER.info("end stat before destroy");
	}

	/**
	 * Update FT email domains from Referentiel ws
	 */
	public void updateDomains() {

		try {

			List<String> referentielDomainResponse = restClientUtils.getDomain(domainToken, domainUrl,
					HttpMethod.GET);

			if (CollectionUtils.isNotEmpty(referentielDomainResponse)) {
				LOGGER.debug("worker LaSuite domains size {} ", referentielDomainResponse.size());

				referentielDomainResponse.forEach(domain -> {
					String ext = (!StringUtils.isEmpty(domain)) ? domain.trim().replaceAll(CLEANUP_PATTERN, "") : null;
					if (Objects.nonNull(ext)) {
						redisManager.saddString(RedisKeysEnum.FT_DOMAINS_MAILS_TMP.getKey(""), ext.toLowerCase());
					}
				});

				// Domains update from TMP list
				redisManager.sInterStore(RedisKeysEnum.FT_DOMAINS_MAILS_MAILS.getKey(""),
						RedisKeysEnum.FT_DOMAINS_MAILS_TMP.getKey(""));
				redisManager.deleteKey(RedisKeysEnum.FT_DOMAINS_MAILS_TMP.getKey(""));
				LOGGER.info("Redis domains mails cache size {} ",
						redisManager.smembersString(RedisKeysEnum.FT_DOMAINS_MAILS_MAILS.getKey("")).size());
			}
		} catch (Exception ex) {
			LOGGER.error("worker LaSuite domain update error {} ", ex.getMessage());
		}
	}

	/**
	 * Send Stat
	 */
	public void sendStats() {

		try {

			Path statPathRoot = Path.of(System.getProperty("java.io.tmpdir"));
			try (Stream<Path> files = Files.list(statPathRoot)) {
				files.filter(x -> x.getFileName().toString().endsWith(".csv")).forEach(x -> {
					try {
						File file = x.toFile();
						// Send file to stat
						restClientUtils.sendStat(statUrl, statToken,
								HttpMethod.POST, file, statHeader);
						// Delete file on success
						file.delete();
					} catch (Exception ex) {
						LOGGER.error("Worker stat error {} ", ex.getMessage(), ex);
					}
					// finally {
					// try {
					// Files.deleteIfExists(x);
					// } catch (Exception d) {
					// LOGGER.error("Cannot delete file error {} ", d.getMessage(), d);
					// }
					// }
				});
			}

		} catch (Exception ex) {
			LOGGER.error("Worker stat update error {} ", ex.getMessage(), ex);
		}
	}

}
