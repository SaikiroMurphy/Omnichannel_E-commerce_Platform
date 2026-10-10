package dan.edu.user_service.dto;

public record AuthResponse(
		String tokenType,
		String accessToken,
		long expiresIn,
		String refreshToken,
		UserResponse user) {
}
