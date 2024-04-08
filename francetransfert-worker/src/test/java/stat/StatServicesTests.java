/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package stat;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import fr.gouv.culture.francetransfert.FranceTransfertWorkerStarter;
import fr.gouv.culture.francetransfert.services.stat.StatServices;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FranceTransfertWorkerStarter.class)
public class StatServicesTests {

	@Autowired
	private StatServices statServices;

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void shouldSaveDataTest() throws Exception {
		// given
		String enclosureId = "8619c5ac-f8d4-4d6b-930f-e07a27e9e72d";
		// when
		boolean isSaved = statServices.saveDataUpload(enclosureId);
		// then
		Assert.assertTrue(isSaved);
	}

	@After
	public void tearDown() throws Exception {

	}

}
