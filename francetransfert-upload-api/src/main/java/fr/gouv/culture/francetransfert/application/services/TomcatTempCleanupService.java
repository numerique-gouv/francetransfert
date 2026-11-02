/*
 * Copyright (c) Direction Interministérielle du Numérique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

package fr.gouv.culture.francetransfert.application.services;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Deletes orphaned Tomcat / Spring multipart temp files on the upload PVC.
 * Each replica has its own volume, so every pod runs the job independently.
 */
@Service
public class TomcatTempCleanupService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TomcatTempCleanupService.class);

	private final boolean enabled;
	private final long minAgeMinutes;
	private final List<Path> directories;
	private final List<String> fileNamePatterns;

	public TomcatTempCleanupService(
			@Value("${tomcat.temp.cleanup.enabled:true}") boolean enabled,
			@Value("${tomcat.temp.cleanup.min-age-minutes:900}") long minAgeMinutes,
			@Value("${tomcat.temp.cleanup.directories:/opt/work,/tmp}") List<String> directories,
			@Value("${tomcat.temp.cleanup.file-name-patterns:tomcat.*,*.tmp,*.upload,upload_*}") List<String> fileNamePatterns) {
		this.enabled = enabled;
		this.minAgeMinutes = Math.max(1, minAgeMinutes);
		this.directories = directories.stream().map(String::trim).filter(s -> !s.isEmpty()).map(Path::of).toList();
		this.fileNamePatterns = fileNamePatterns.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
	}

	@Scheduled(cron = "${tomcat.temp.cleanup.cron:0 0 3 * * *}", zone = "${tomcat.temp.cleanup.zone:Europe/Paris}")
	public CleanupStats clean() {
		if (!enabled) {
			LOGGER.debug("Tomcat temp cleanup is disabled");
			return CleanupStats.EMPTY;
		}

		Instant cutoff = Instant.now().minus(Duration.ofMinutes(minAgeMinutes));
		int deletedFiles = 0;
		long deletedBytes = 0;
		int errors = 0;

		for (Path directory : uniqueExistingDirectories()) {
			try (Stream<Path> files = Files.walk(directory)) {
				List<Path> candidates = files.filter(Files::isRegularFile).filter(this::matchesName)
						.filter(path -> isOlderThan(path, cutoff)).toList();
				for (Path candidate : candidates) {
					try {
						long size = Files.size(candidate);
						if (Files.deleteIfExists(candidate)) {
							deletedFiles++;
							deletedBytes += size;
							LOGGER.debug("Deleted Tomcat temp file {}", candidate);
						}
					} catch (IOException e) {
						errors++;
						LOGGER.warn("Unable to delete Tomcat temp file {}", candidate, e);
					}
				}
			} catch (IOException | UncheckedIOException e) {
				errors++;
				LOGGER.warn("Unable to walk Tomcat temp directory {}", directory, e);
			}
		}

		LOGGER.info("Tomcat temp cleanup finished: deletedFiles={} deletedBytes={} errors={}", deletedFiles,
				deletedBytes, errors);
		return new CleanupStats(deletedFiles, deletedBytes, errors);
	}

	private List<Path> uniqueExistingDirectories() {
		List<Path> unique = new ArrayList<>();
		for (Path directory : directories) {
			if (!Files.isDirectory(directory)) {
				LOGGER.debug("Skip missing Tomcat temp directory {}", directory);
				continue;
			}
			boolean alreadySeen = unique.stream().anyMatch(seen -> isSameDirectory(seen, directory));
			if (!alreadySeen) {
				unique.add(directory);
			}
		}
		return unique;
	}

	private boolean isSameDirectory(Path first, Path second) {
		try {
			return Files.isSameFile(first, second);
		} catch (IOException e) {
			return first.toAbsolutePath().normalize().equals(second.toAbsolutePath().normalize());
		}
	}

	private boolean matchesName(Path path) {
		Path fileName = path.getFileName();
		if (fileName == null) {
			return false;
		}
		String name = fileName.toString();
		return fileNamePatterns.stream().anyMatch(pattern -> FilenameUtils.wildcardMatch(name, pattern));
	}

	private boolean isOlderThan(Path path, Instant cutoff) {
		try {
			return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
		} catch (IOException e) {
			LOGGER.debug("Unable to read last modified time of {}", path, e);
			return false;
		}
	}

	public record CleanupStats(int deletedFiles, long deletedBytes, int errors) {
		static final CleanupStats EMPTY = new CleanupStats(0, 0, 0);
	}
}
