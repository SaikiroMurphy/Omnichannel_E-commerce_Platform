package dan.edu.user_service.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import dan.edu.user_service.model.UserAccount;

@Service
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final String issuer;
	private final String audience;
	private final Duration accessTokenTtl;

	public JwtTokenService(
			JwtEncoder jwtEncoder,
			@Value("${user-service.jwt.issuer}") String issuer,
			@Value("${user-service.jwt.audience}") String audience,
			@Value("${user-service.jwt.access-token-ttl:15m}") Duration accessTokenTtl) {
		this.jwtEncoder = jwtEncoder;
		this.issuer = issuer;
		this.audience = audience;
		this.accessTokenTtl = accessTokenTtl;
		if (accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
			throw new IllegalArgumentException("JWT access token TTL must be positive");
		}
	}

	public IssuedAccessToken issueAccessToken(UserAccount user) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(accessTokenTtl);
		List<String> roles = user.getRoles().stream()
				.map(role -> "ROLE_" + role.name())
				.sorted()
				.toList();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.subject(user.getId().toString())
				.audience(List.of(audience))
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("email", user.getEmail())
				.claim("roles", roles)
				.build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
		return new IssuedAccessToken(token, accessTokenTtl.toSeconds());
	}

	public record IssuedAccessToken(String value, long expiresInSeconds) {
	}
}
