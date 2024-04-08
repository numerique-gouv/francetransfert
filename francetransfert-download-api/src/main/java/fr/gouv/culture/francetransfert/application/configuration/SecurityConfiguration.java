package fr.gouv.culture.francetransfert.application.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfiguration {

	@Value("${agentconnect.enabled:false}")
	private boolean agentConnect;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and().exceptionHandling()
				.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)).and().csrf().disable()
				.headers().frameOptions().disable();
		if (agentConnect) {
			http.authorizeRequests(authz -> authz.anyRequest().permitAll()).oauth2ResourceServer().jwt()
					.jwtAuthenticationConverter(jwtAuthenticationConverter());
		} else {
			http.authorizeRequests(authz -> authz.anyRequest().permitAll());
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

}
