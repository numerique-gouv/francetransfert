package fr.gouv.culture.francetransfert.configuration;

import java.security.cert.X509Certificate;
import java.time.Duration;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RestClientConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(RestClientConfig.class);

	@Value("${disableSsl:false}")
	private boolean disableSsl;

	@Value("${enableMtls:true}")
	private boolean mtls;

	@Autowired(required = false)
	@Qualifier("poolingClient")
	HttpClient httpClient;

	@Bean
	public RestTemplate restTemplate() {

		if (disableSsl) {
			try {
				SSLDisableUtils.disable();
				SSLContext sslContext = SSLContextBuilder.create()
						.loadTrustMaterial((X509Certificate[] certificateChain, String authType) -> true).build();

				Registry<ConnectionSocketFactory> socketRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
						.register(URIScheme.HTTPS.getId(),
								new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
						.register(URIScheme.HTTP.getId(), new PlainConnectionSocketFactory()).build();

				HttpClient httpClient = HttpClientBuilder.create()
						.setConnectionManager(new PoolingHttpClientConnectionManager(socketRegistry))
						.setConnectionManagerShared(true).build();

				ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
				RestTemplate restTemplate = new RestTemplate(requestFactory);
				return restTemplate;
			} catch (Exception e) {
				LOGGER.error("error while skipping tls", e);
			}

		} else if (mtls) {
			RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
			return restTemplate;
		}
		RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
		return restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(90)).setReadTimeout(Duration.ofSeconds(90))
				.build();

	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "enableMtls", havingValue = "true", matchIfMissing = true)
	public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
		HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
		clientHttpRequestFactory.setBufferRequestBody(false);
		clientHttpRequestFactory.setHttpClient(httpClient);
		return clientHttpRequestFactory;
	}

}
