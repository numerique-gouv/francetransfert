/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MimeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MimeService.class);

    @Value("${extension.name}")
    private List<String> extensionList;

    @Value("${mimetype.name}")
    private List<String> mimeList;

    private Tika tika = new Tika();

    public Boolean isAuthorisedMimeTypeFromFileName(String filename) {

        Boolean authorised = false;
        if (null == filename) { // test null parameter : file, flowFilename.
            authorised = false;
        } else {
            String ext = FilenameUtils.getExtension(filename);
            if (extensionList.contains(ext)) {
                authorised = false;
            } else {
                authorised = true;
            }
            // WhiteList regex
            // for (String extent : mimeList) {
            //
            // if (mimeType.matches(extent)) {
            // authorised = true;
            // break;
            // }
            // }
        }
        return authorised;
    }

    public Boolean isAuthorisedMimeTypeFromFile(FileInputStream fileInputStream) throws IOException {

        Boolean authorised = false;
        String mimeType = tika.detect(fileInputStream);

        //BlackList
        authorised = !mimeList.stream().anyMatch(mime -> Strings.CI.startsWith(mimeType, mime));

        // WhiteList regex
        // for (String extent : mimeList) {
        //
        // if (mimeType.matches(extent)) {
        // authorised = true;
        // break;
        // }
        // }
        return authorised;
    }

    public String getMimeTypeFromFile(FileInputStream fileInputStream) {
        String mimeType = "";
        try {
            fileInputStream.getChannel().position(0);
            mimeType = tika.detect(fileInputStream);
        } catch (IOException e) {
            LOGGER.error("Error while getting mimetype", e);
        }
        return mimeType;
    }

    public Set<String> authorisedMimeList() {

        Set<String> retList = new HashSet<String>();

        for (String ext : extensionList) {
            String fileName = "ttoto." + ext;
            String mime = tika.detect(fileName);
            retList.add(mime);
        }

        return retList;

    }

}
