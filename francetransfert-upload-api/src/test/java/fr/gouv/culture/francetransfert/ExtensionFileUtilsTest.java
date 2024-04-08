/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert;

import fr.gouv.culture.francetransfert.domain.utils.ExtensionFileUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ExtensionFileUtilsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();


    private static List<String> authorisedExtensionFile;
    private static Boolean autorised;

    @BeforeClass
    public static void before()  {
        authorisedExtensionFile = new ArrayList<>();
        authorisedExtensionFile.add("txt");
        autorised = false;
    }

    @Test
    public void testAuthorisedToUpload() {
        //given
        MultipartFile file = new MockMultipartFile("test.txt", "test.txt", "multipart/form-data", new byte[0]);
        //when
        autorised = ExtensionFileUtils.isAuthorisedToUpload(authorisedExtensionFile, file, "test.txt");
        //Then
        assertTrue(autorised);
    }

    @Test
    public void testNOAuthorisedToUpload() { // extension no autorised
        //given
        MultipartFile file = new MockMultipartFile("test.exe", "test.exe", "multipart/form-data", new byte[0]);
        //when
        autorised = ExtensionFileUtils.isAuthorisedToUpload(authorisedExtensionFile, file, "test.exe");
        //Then
        assertFalse(autorised);
    }

    @Test
    public void testNOAuthorisedToUploadOriginalFileNameChanged() {// OriginalFileName diferent MultipartFile name => NO autorised
        //given
        MultipartFile file = new MockMultipartFile("test.txt", "test.exe", "multipart/form-data", new byte[0]);
        //when
        autorised = ExtensionFileUtils.isAuthorisedToUpload(authorisedExtensionFile, file, "test.exe");
        //Then
        assertFalse(autorised);
    }

    @Test
    public void testGetExtension() {
        assertEquals("", ExtensionFileUtils.getExtension("C"));
        assertEquals("ext", ExtensionFileUtils.getExtension("C.ext"));
        assertEquals("ext", ExtensionFileUtils.getExtension("A/B/C.ext"));
        assertEquals("", ExtensionFileUtils.getExtension("A/B/C.ext/"));
        assertEquals("", ExtensionFileUtils.getExtension("A/B/C.ext/.."));
        assertEquals("bin", ExtensionFileUtils.getExtension("A/B/C.bin"));
        assertEquals("hidden", ExtensionFileUtils.getExtension(".hidden"));
        assertEquals("dsstore", ExtensionFileUtils.getExtension("/user/home/.dsstore"));
        assertEquals("", ExtensionFileUtils.getExtension(".strange."));
        assertEquals("3", ExtensionFileUtils.getExtension("1.2.3"));
        assertEquals("exe", ExtensionFileUtils.getExtension("C:\\Program Files (x86)\\java\\bin\\javaw.exe"));
    }

    @Ignore
    @Test
    public void testCreateFolder() throws IOException{
        File file = tempFolder.newFolder("testfolder");
        assertTrue(file.exists());
    }

    @Ignore
    @Test
    public void testDeleteFolder() throws IOException{
        File file = tempFolder.newFile("testfolder");
        file.delete();
        assertFalse(file.exists());
    }

    @AfterClass
    public static void after() throws IOException {
        authorisedExtensionFile = null;
        autorised = false;
    }
}
