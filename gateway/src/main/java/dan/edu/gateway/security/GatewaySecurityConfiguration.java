package dan.edu.gateway.security;

import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
public class GatewaySecurityConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(GatewaySecurityConfiguration.class);
	private static final int MINIMUM_HMAC_SECRET_BYTES = 32;

	@Bean
	ReactiveJwtDecoder reactiveJwtDecoder(
			@Value("${gateway.security.jwt.issuer-uri:}") String issuerUri,
			@Value("${gateway.security.jwt.jwk-set-uri:}") String jwkSetUri,
			@Value("${gateway.security.jwt.secret:}") String secret,
			@Value("${gateway.security.jwt.secret-base64:}") String secretBase64,
			@Value("${gateway.security.jwt.audience:}") String audience) {
		if (!secretBase64.isBlank() || !secret.isBlank()) {
			byte[] secretBytes = secretBase64.isBlank()
					? secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)
					: decodeBase64Secret(secretBase64);
			if (secretBytes.length < MINIMUM_HMAC_SECRET_BYTES) {
				throw new IllegalStateException("The HS256 JWT secret must contain at least 32 bytes");
			}
			SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
			NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(secretKey)
					.macAlgorithm(MacAlgorithm.HS256)
					.build();
			configureValidators(decoder, issuerUri, audience);
			return decoder;
		}
		if (!jwkSetUri.isBlank()) {
			NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
			configureValidators(decoder, issuerUri, audience);
			return decoder;
		}
		if (!issuerUri.isBlank()) {
			return ReactiveJwtDecoders.fromIssuerLocation(issuerUri);
		}

		logger.error("JWT verification is not configured; protected API requests will be rejected");
		return token -> Mono.error(new BadJwtException("JWT verification is not configured"));
	}

	private static byte[] decodeBase64Secret(String secretBase64) {
		try {
			return Base64.getDecoder().decode(secretBase64);
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("JWT_SECRET_BASE64 must be valid standard Base64", exception);
		}
	}

	private static void configureValidators(
			NimbusReactiveJwtDecoder decoder,
			String issuerUri,
			String audience) {
		OAuth2TokenValidator<Jwt> defaultValidator = issuerUri.isBlank()
				? JwtValidators.createDefault()
				: JwtValidators.createDefaultWithIssuer(issuerUri);
		if (audience.isBlank()) {
			decoder.setJwtValidator(defaultValidator);
			return;
		}
		OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
				"aud", values -> values != null && values.contains(audience));
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator));
	}

	@Bean
	SecurityWebFilterChain gatewaySecurityWebFilterChain(
			ServerHttpSecurity http,
			ReactiveJwtDecoder jwtDecoder) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchange -> exchange
						.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.pathMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus").permitAll()
						.pathMatchers(HttpMethod.POST,
								"/api/auth/login",
								"/api/auth/register",
								"/api/auth/refresh").permitAll()
						.pathMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
						.anyExchange().authenticated())
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(jwt -> jwt.jwtDecoder(jwtDecoder)))
				.build();
	}
}
