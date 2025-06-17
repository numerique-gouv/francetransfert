/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.domain.utils;

import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class ExtensionFileUtils {
	
	private ExtensionFileUtils() {
		// private Constructor
	}

    public static String getExtension(String fileName) {
        char ch;
        int len;
        if(fileName==null ||
                (len = fileName.length())==0
                || (ch = fileName.charAt(len-1))=='/' //in the case of a directory
                || ch=='\\' || ch=='.' )              //in the case of . or ..
            return ""; // empty extension
        int dotInd = fileName.lastIndexOf('.'),
                sepInd = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if( dotInd<=sepInd )
            return ""; // empty extension
        else
            return fileName.substring(dotInd+1).toLowerCase();
    }

    public static Boolean isAuthorisedToUpload(List<String> authorisedExtensionFile, MultipartFile file, String flowFilename) {
        Boolean authorised = true ;
        if (null == file || null == flowFilename) { // test null parameter : file, flowFilename.
            authorised = false;
        } else {
            String extensionFile = ExtensionFileUtils.getExtension(file.getOriginalFilename());
            if (!flowFilename.equalsIgnoreCase(file.getOriginalFilename()) || !(!CollectionUtils.isEmpty(authorisedExtensionFile) && authorisedExtensionFile.contains(extensionFile))) { // test authorized file to upload.
                authorised = false;
            }
        }
        return authorised;
    }

}
