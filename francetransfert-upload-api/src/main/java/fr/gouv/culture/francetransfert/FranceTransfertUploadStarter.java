/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * The type Demo application.
 */

@EnableMethodSecurity
@SpringBootApplication
@ComponentScan(basePackages = { "fr.gouv.culture" })
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy
public class FranceTransfertUploadStarter {

	@Value("${agentconnect.enabled:false}")
	private boolean agentConnect;

	/**
	 * The entry point of application.
	 *
	 * @param args the input arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(FranceTransfertUploadStarter.class, args);
	}

}
