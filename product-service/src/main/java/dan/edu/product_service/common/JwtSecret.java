package dan.edu.product_service.common;

import java.util.Base64;

public final class JwtSecret {

	private JwtSecret() {
	}

	public static byte[] decode(String encodedSecret) {
		if (encodedSecret == null || encodedSecret.isBlank()) {
			throw new IllegalStateException("JWT_SECRET_BASE64 must be configured");
		}
		final byte[] secret;
		try {
			secret = Base64.getDecoder().decode(encodedSecret);
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("JWT_SECRET_BASE64 must be valid standard Base64", exception);
		}
		if (secret.length < 32) {
			throw new IllegalStateException("JWT_SECRET_BASE64 must decode to at least 32 bytes for HS256");
		}
		return secret;
	}
}
