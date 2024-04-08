package fr.gouv.culture.francetransfert.configuration;

import org.apache.hc.client5.http.classic.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

	@Autowired
	@Qualifier("poolingClient")
	HttpClient httpClient;

	@Bean
	public RestTemplate restTemplate() {
//		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
//		requestFactory.setBufferRequestBody(false);
		RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
		// restTemplate.setErrorHandler(errorHandler);
		return restTemplate;
	}

	@Bean
	@ConditionalOnMissingBean
	public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
		HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
		clientHttpRequestFactory.setBufferRequestBody(false);
		clientHttpRequestFactory.setHttpClient(httpClient);
		return clientHttpRequestFactory;
	}

}
