package dan.edu.user_service.security;

import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import dan.edu.user_service.common.JwtSecret;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

	@Bean
	SecretKey jwtSecretKey(@Value("${user-service.jwt.secret-base64:}") String secretBase64) {
		byte[] secretBytes = JwtSecret.decode(secretBase64);
		return new SecretKeySpec(secretBytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey.getEncoded()));
	}

	@Bean
	JwtDecoder jwtDecoder(
			SecretKey jwtSecretKey,
			@Value("${user-service.jwt.issuer}") String issuer,
			@Value("${user-service.jwt.audience}") String audience) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
		OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
				"aud", values -> values != null && values.contains(audience));
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
		return decoder;
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName("roles");
		authoritiesConverter.setAuthorityPrefix("");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return converter;
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtDecoder jwtDecoder,
			JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus").permitAll()
						.requestMatchers(HttpMethod.POST,
								"/api/auth/register",
								"/api/auth/login",
								"/api/auth/refresh").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(jwt -> jwt.decoder(jwtDecoder)
								.jwtAuthenticationConverter(jwtAuthenticationConverter)))
				.build();
	}
}
