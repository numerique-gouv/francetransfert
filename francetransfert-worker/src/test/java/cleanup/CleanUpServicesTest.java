/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package cleanup;

import fr.gouv.culture.francetransfert.FranceTransfertWorkerStarter;
import fr.gouv.culture.francetransfert.services.cleanup.CleanUpServices;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FranceTransfertWorkerStarter.class)
public class CleanUpServicesTest {

    @Autowired
    private CleanUpServices cleanUpServices;

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void sendMailsRelaunchTests() throws Exception {
        //given
        //when
        cleanUpServices.cleanUp();
        //then

    }



    @After
    public void tearDown() throws Exception {

    }

}
