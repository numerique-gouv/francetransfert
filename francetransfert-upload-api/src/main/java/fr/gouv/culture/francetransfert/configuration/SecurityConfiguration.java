package fr.gouv.culture.francetransfert.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import tools.jackson.databind.DeserializationFeature;

@Configuration
public class SecurityConfiguration {

	@Value("${agentconnect.enabled:false}")
	private boolean agentConnect;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.csrf(csrf -> csrf.disable())
				.headers(headers -> headers.frameOptions(frame -> frame.disable()));

		if (agentConnect) {
			http
					.authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
					.oauth2ResourceServer(oauth2 -> oauth2
							.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
		} else {
			http.authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
		}

		return http.build();
	}

	@Bean
	@ConditionalOnProperty(prefix = "agentconnect", name = "enabled", havingValue = "true")
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setPrincipalClaimName("email");
		return converter;
	}

	@Bean
	public JsonMapperBuilderCustomizer customizer() {
		return builder -> builder
				.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
	}

}
