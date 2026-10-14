package dan.edu.payment_service.security;

import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import dan.edu.payment_service.common.JwtSecret;
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

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

	@Bean
	SecretKey paymentJwtSecretKey(@Value("${payment.jwt.secret-base64:}") String secretBase64) {
		return new SecretKeySpec(JwtSecret.decode(secretBase64), "HmacSHA256");
	}

	@Bean
	JwtEncoder paymentJwtEncoder(SecretKey paymentJwtSecretKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(paymentJwtSecretKey.getEncoded()));
	}

	@Bean
	JwtDecoder paymentJwtDecoder(
			SecretKey paymentJwtSecretKey,
			@Value("${payment.jwt.issuer}") String issuer,
			@Value("${payment.jwt.audience}") String audience) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(paymentJwtSecretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
		OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
				"aud", values -> values != null && values.contains(audience));
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
		return decoder;
	}

	@Bean
	JwtAuthenticationConverter paymentJwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName("roles");
		authoritiesConverter.setAuthorityPrefix("");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return converter;
	}

	@Bean
	SecurityFilterChain paymentSecurityFilterChain(
			HttpSecurity http,
			JwtDecoder paymentJwtDecoder,
			JwtAuthenticationConverter paymentJwtAuthenticationConverter) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(jwt -> jwt.decoder(paymentJwtDecoder)
								.jwtAuthenticationConverter(paymentJwtAuthenticationConverter)))
				.build();
	}
}
