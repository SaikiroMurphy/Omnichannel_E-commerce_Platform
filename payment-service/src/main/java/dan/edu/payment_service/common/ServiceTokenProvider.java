package dan.edu.payment_service.common;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class ServiceTokenProvider {

	private final JwtEncoder jwtEncoder;
	private final String issuer;
	private final String audience;

	public ServiceTokenProvider(
			JwtEncoder jwtEncoder,
			@Value("${payment.jwt.issuer}") String issuer,
			@Value("${payment.jwt.audience}") String audience) {
		this.jwtEncoder = jwtEncoder;
		this.issuer = issuer;
		this.audience = audience;
	}

	public String createToken() {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.subject("payment-service")
				.audience(List.of(audience))
				.issuedAt(now)
				.expiresAt(now.plusSeconds(30))
				.claim("roles", List.of("ROLE_SERVICE"))
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}
}
