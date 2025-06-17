/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.message.BasicHeaderElementIterator;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ResourceUtils;

@Configuration
@ConditionalOnProperty(name = "enableMtls", havingValue = "true", matchIfMissing = true)
public class HttpClientConfig {

	@Value("${trustStorePassword:changeit}")
	private String trustStorePassword;
	@Value("${sslTrustStore:}")
	private String trustStore;
	@Value("${sslKeyStorePassword:}")
	private String keyStorePassword;
	@Value("${sslKeyPassword:}")
	private String keyPassword;
	@Value("${sslKeyStore:}")
	private String keyStore;

	@Value("${maxTotalConnection:50}")
	private int MAX_TOTAL_CONNECTIONS;

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientConfig.class);

	// Determines the timeout in milliseconds until a connection is established.
	private static final int CONNECT_TIMEOUT_MIN = 10;

	// The timeout when requesting a connection from the connection manager.
	private static final int REQUEST_TIMEOUT_MIN = 10;

	// The timeout for waiting for data
	private static final int SOCKET_TIMEOUT_MIN = 10;

	private static final int DEFAULT_KEEP_ALIVE_TIME_MIN = 2;

	private static final int VALIDATION_SECS = 30;

	private static final int CLOSE_IDLE_CONNECTION_WAIT_TIME_SECS = 120;

	@Bean
	public SSLContext sslContext() {
		SSLContextBuilder builder = new SSLContextBuilder();
		SSLContext sslcontext = null;
		try {

			// Default cert if not set
			if (StringUtils.isBlank(trustStore)) {
				String javaHome = System.getProperty("java.home");
				trustStore = Paths.get(javaHome, "lib", "security", "cacerts").toString();
			}

			if (StringUtils.isNotBlank(keyStore)) {
				LOGGER.info("init keystore");
				KeyStore keystore = keyStore(keyStore, keyStorePassword.toCharArray());
//				sslcontext = builder.loadTrustMaterial(new File(trustStore), trustStorePassword.toCharArray())
//						.loadKeyMaterial(keystore, keyPassword.toCharArray(), new PrivateKeyStrategy() {
//							@Override
//							public String chooseAlias(Map<String, PrivateKeyDetails> aliases,
//									SSLParameters sslParameters) {
//								// TODO Auto-generated method stub
//								return "FT-test";
//							}
//						}).build();

				sslcontext = builder.loadTrustMaterial(new File(trustStore), trustStorePassword.toCharArray())
						.loadKeyMaterial(keystore, keyPassword.toCharArray()).build();
			} else {
				sslcontext = builder.loadTrustMaterial(new File(trustStore), trustStorePassword.toCharArray()).build();
			}

		} catch (Exception e) {
			LOGGER.error("Pooling Connection Manager Initialisation failure because of " + e.getMessage(), e);
			try {
				sslcontext = builder.loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
			} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
				LOGGER.error("Pooling Connection Manager Initialisation failure because of " + e.getMessage(), e);
			}
		}
		return sslcontext;
	}

	@Bean
	public Registry<ConnectionSocketFactory> socketFactory() {
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext(), new NoopHostnameVerifier());
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("https", sslsf).register("http", new PlainConnectionSocketFactory()).build();
		return socketFactoryRegistry;
	}

	@Bean
	public SocketConfig socketConfig() {
		return SocketConfig.custom().setSoTimeout(Timeout.ofMinutes(REQUEST_TIMEOUT_MIN)).setSoKeepAlive(true).build();
	}

	@Bean
	public PoolingHttpClientConnectionManager poolingConnectionManager() {

		PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager(
				socketFactory());

		poolingConnectionManager.setDefaultSocketConfig(socketConfig());
		poolingConnectionManager.setDefaultConnectionConfig(connectionConfig());
		poolingConnectionManager.setValidateAfterInactivity(TimeValue.ofMinutes(DEFAULT_KEEP_ALIVE_TIME_MIN));
		poolingConnectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
		return poolingConnectionManager;
	}

	@Bean
	public ConnectionConfig connectionConfig() {
		return ConnectionConfig.custom().setConnectTimeout(Timeout.ofMinutes(CONNECT_TIMEOUT_MIN))
				.setSocketTimeout(Timeout.ofMinutes(SOCKET_TIMEOUT_MIN))
				.setTimeToLive(TimeValue.ofMinutes(DEFAULT_KEEP_ALIVE_TIME_MIN))
				.setValidateAfterInactivity(TimeValue.ofSeconds(VALIDATION_SECS)).build();
	}

	@Bean
	public ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
		return new ConnectionKeepAliveStrategy() {
			@Override
			public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
				BasicHeaderElementIterator it = new BasicHeaderElementIterator(
						response.headerIterator(HTTP.CONN_KEEP_ALIVE));
				while (it.hasNext()) {
					HeaderElement he = it.next();
					String param = he.getName();
					String value = he.getValue();

					if (value != null && param.equalsIgnoreCase("timeout")) {
						return TimeValue.ofSeconds(Long.parseLong(value) * 1000);
					}
				}
				return TimeValue.ofMinutes(DEFAULT_KEEP_ALIVE_TIME_MIN);
			}
		};
	}

	@Bean("poolingClient")
	public HttpClient poolHttpClient() {
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(Timeout.ofMinutes(REQUEST_TIMEOUT_MIN))
				.setDefaultKeepAlive(DEFAULT_KEEP_ALIVE_TIME_MIN, TimeUnit.MINUTES)
				.setConnectTimeout(Timeout.ofSeconds(CONNECT_TIMEOUT_MIN)).build();

		return HttpClients.custom().setDefaultRequestConfig(requestConfig)
				.setConnectionManager(poolingConnectionManager()).setKeepAliveStrategy(connectionKeepAliveStrategy())
				.build();
	}

	@Bean("normalClient")
	public HttpClient httpClient() {

		BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(socketFactory());
		connectionManager.setConnectionConfig(connectionConfig());
		return HttpClients.custom().setConnectionManager(connectionManager).build();

	}

	@Bean
	public Runnable idleConnectionMonitor(final PoolingHttpClientConnectionManager connectionManager) {
		return new Runnable() {
			@Override
			@Scheduled(fixedDelay = 10000)
			public void run() {
				try {
					if (connectionManager != null) {
						LOGGER.trace("run IdleConnectionMonitor - Closing expired and idle connections...");
						connectionManager.closeExpired();
						connectionManager.closeIdle(TimeValue.ofSeconds(CLOSE_IDLE_CONNECTION_WAIT_TIME_SECS));
					} else {
						LOGGER.trace("run IdleConnectionMonitor - Http Client Connection manager is not initialised");
					}
				} catch (Exception e) {
					LOGGER.error("run IdleConnectionMonitor - Exception occurred. msg={}, e={}", e.getMessage(), e);
				}
			}
		};
	}

	private KeyStore keyStore(String file, char[] password) throws Exception {
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		File key = ResourceUtils.getFile(file);
		InputStream in = new FileInputStream(key);
		keyStore.load(in, password);
		return keyStore;
	}
}
