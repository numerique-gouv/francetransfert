/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * The type Demo application.
 */
@EnableGlobalMethodSecurity(prePostEnabled = true)
@SpringBootApplication
public class FranceTransfertDownloadStarter {

	@Autowired
	private Environment env;

	/**
	 * The entry point of application.
	 *
	 * @param args the input arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(FranceTransfertDownloadStarter.class, args);
	}

	@Bean
	public OpenAPI springShopOpenAPI() {
		return new OpenAPI().info(new Info().title(env.getProperty("tool.swagger.api.title"))
				.description(env.getProperty("tool.swagger.api.description"))
				.version(env.getProperty("tool.swagger.api.version"))
				.license(new License().name(env.getProperty("tool.swagger.api.licence"))
						.url(env.getProperty("tool.swagger.api.licence.url")))
				.termsOfService(env.getProperty("tool.swagger.api.terms-of-services-use")));
	}

}
