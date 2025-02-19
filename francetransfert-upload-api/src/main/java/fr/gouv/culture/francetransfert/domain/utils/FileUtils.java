/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.domain.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import fr.gouv.culture.francetransfert.application.resources.model.DataRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.DirectoryRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.FileRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.FileRepresentationApi;
import fr.gouv.culture.francetransfert.application.resources.model.FranceTransfertDataRepresentation;
import fr.gouv.culture.francetransfert.domain.redis.entity.FileDomain;

public class FileUtils {

	public static final int MAX_FILENAME_LENGTH = 170;

	private FileUtils() {
		// private Constructor
	}

	// return Map<name-file, size>
	public static Map<String, String> searchRootFiles(FranceTransfertDataRepresentation metadata) {
		Map<String, String> files = new HashMap<>();
		if (!CollectionUtils.isEmpty(metadata.getRootFiles())) {
			files = metadata.getRootFiles().stream()
					.collect(Collectors.toMap(file -> file.getName(), file -> String.valueOf(file.getSize())));
		}
		return files;
	}

	// return Map<name-dir, total-size>
	public static Map<String, String> searchRootDirs(FranceTransfertDataRepresentation metadata) {
		Map<String, String> dirs = new HashMap<>();
		if (!CollectionUtils.isEmpty(metadata.getRootDirs())) {
			dirs = metadata.getRootDirs().stream()
					.collect(Collectors.toMap(dir -> dir.getName(), dir -> String.valueOf(dir.getTotalSize())));
		}
		return dirs;
	}

	// return Map<path/name-file, fileDomain {path, size, fid}>
	public static List<FileDomain> searchFiles(FranceTransfertDataRepresentation metadata, String enclosureId) {
		List<FileDomain> files = metadata.getRootFiles().stream()
				.map(file -> FileDomain.builder().path(enclosureId + "/" + file.getName())
						.size(String.valueOf(file.getSize())).fid(file.getFid()).build())
				.collect(Collectors.toList());

		for (DirectoryRepresentation rootDir : metadata.getRootDirs()) {
			searchFilesInDirectory((DataRepresentation) rootDir, enclosureId, files);
		}
		return files;
	}

	private static void searchFilesInDirectory(DataRepresentation data, String path, List<FileDomain> files) {
		if (data instanceof DirectoryRepresentation) {
			path = path + "/" + data.getName();
			for (FileRepresentation file : ((DirectoryRepresentation) data).getFiles()) {
				files.add(FileDomain.builder().path(path + "/" + file.getName()).size(String.valueOf(file.getSize()))
						.fid(file.getFid()).build());
			}
			for (DataRepresentation temp : ((DirectoryRepresentation) data).getDirs()) {
				searchFilesInDirectory(temp, path, files);
			}
		}
	}

	// ---
	public static long getEnclosureTotalFileSize(List<FileRepresentationApi> rootFiles) {
		long size = 0;
		for (FileRepresentationApi rootFile : rootFiles) {
			size = size + rootFile.getSize();
		}
		return size;
	}

	// ---
	public static long getEnclosureTotalSize(List<FileRepresentation> rootFiles,
			List<DirectoryRepresentation> rootDirs) {
		long size = 0;
		for (DirectoryRepresentation rootDir : rootDirs) {
			size = size + rootDir.getTotalSize();
		}

		for (FileRepresentation rootFile : rootFiles) {
			size = size + rootFile.getSize();
		}
		return size;
	}

	// ---
	public static boolean getSizeFileOverApi(List<FileRepresentationApi> rootFiles, long fileSize) {
		for (FileRepresentationApi rootFile : rootFiles) {
			if (rootFile.getSize() > fileSize) {
				return true;
			}
		}
		return false;
	}

	public static boolean getSizeFileOver(List<FileRepresentation> rootFiles, long fileSize) {
		for (FileRepresentation rootFile : rootFiles) {
			if (rootFile.getSize() > fileSize) {
				return true;
			}
		}
		return false;
	}

	public static boolean getSizeDirFileOver(List<DirectoryRepresentation> rootDirs, long fileSize) {
		for (DirectoryRepresentation rootDir : rootDirs) {
			if (rootDir.getDirs().isEmpty()) {
				if (getSizeFileOver(rootDir.getFiles(), fileSize)) {
					return true;
				}
			} else {
				if (!getSizeFileOver(rootDir.getFiles(), fileSize)) {
					if (getSizeDirFileOver(rootDir.getDirs(), fileSize)) {
						return true;
					}
				} else {
					return true;
				}
			}
		}

		return false;
	}

	// ---
	public static Map<String, String> RootFilesValidation(List<FileRepresentationApi> rootFiles) {
		Map<String, String> files = new HashMap<>();
		if (!CollectionUtils.isEmpty(rootFiles)) {
			files = rootFiles.stream()
					.collect(Collectors.toMap(file -> String.valueOf(file.getFid()), file -> file.getName()));
		}
		return files;
	}

	public static boolean hasFileNameTooLong(List<FileRepresentation> rootFiles,
			List<DirectoryRepresentation> rootDirs) {
		for (FileRepresentation rootFile : rootFiles) {
			if (rootFile.getName().length() > MAX_FILENAME_LENGTH) {
				return true;
			}
		}
		for (DirectoryRepresentation rootDir : rootDirs) {
			if (rootDir.getName().length() > MAX_FILENAME_LENGTH) {
				return true;
			}
			if (hasFileNameTooLong(rootDir.getFiles(), rootDir.getDirs())) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasFileNameTooLong(List<FileRepresentationApi> rootFiles) {
		for (FileRepresentationApi rootFile : rootFiles) {
			if (rootFile.getName().length() > MAX_FILENAME_LENGTH) {
				return true;
			}
		}
		return false;
	}
}
