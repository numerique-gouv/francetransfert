/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert;

import fr.gouv.culture.francetransfert.application.resources.model.Download;
import fr.gouv.culture.francetransfert.application.resources.model.DownloadRepresentation;
import fr.gouv.culture.francetransfert.application.services.DownloadServices;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = FranceTransfertDownloadStarter.class)
public class DownloadServiceTest {

    @Autowired
    private DownloadServices downloadServices;


    @Before
    public void setUp() throws Exception {

    }

    @Ignore // TODO; delete @Ignore when implement Redis in environment integration
    @Test
    public void downloadTest() throws Exception {
        //given
        String enclosureId = "8ffd72f0-4432-4e07-b247-362b1eb4edfb";
        String recipientMail = "louay@live.fr";
        String recipientId = "8ffd72f0-4432-4e07-b247-362b1eb4vfrt";
        String password = "";
        //when
        //Download downloadUrl = downloadServices.generateDownloadUrlWithPassword(enclosureId, recipientMail, recipientId, password);
        //then
        //Assert.assertTrue(downloadUrl != null);
    }

    @After
    public void tearDown() throws Exception {

    }

}
