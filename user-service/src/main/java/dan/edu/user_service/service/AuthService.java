package dan.edu.user_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dan.edu.user_service.common.ConflictException;
import dan.edu.user_service.dto.AuthResponse;
import dan.edu.user_service.dto.ChangePasswordRequest;
import dan.edu.user_service.dto.LoginRequest;
import dan.edu.user_service.dto.RegisterRequest;
import dan.edu.user_service.dto.UserResponse;
import dan.edu.user_service.model.RefreshToken;
import dan.edu.user_service.model.UserAccount;
import dan.edu.user_service.repository.UserRepository;
import dan.edu.user_service.repository.RefreshTokenRepository;
import dan.edu.user_service.security.JwtTokenService;
import dan.edu.user_service.security.JwtTokenService.IssuedAccessToken;
import dan.edu.user_service.utils.mapper.UserMapper;

@Service
public class AuthService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final int REFRESH_TOKEN_BYTES = 48;
	private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final UserMapper userMapper;
	private final Duration refreshTokenTtl;

	public AuthService(
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenService jwtTokenService,
			UserMapper userMapper,
			@Value("${user-service.jwt.refresh-token-ttl:30d}") Duration refreshTokenTtl) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.userMapper = userMapper;
		this.refreshTokenTtl = refreshTokenTtl;
		if (refreshTokenTtl.isNegative() || refreshTokenTtl.isZero()) {
			throw new IllegalArgumentException("JWT refresh token TTL must be positive");
		}
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new ConflictException("An account with this email already exists");
		}
		validatePasswordByteLength(request.password());
		UserAccount user = userRepository.save(new UserAccount(
				email,
				passwordEncoder.encode(request.password()),
				request.firstName().trim(),
				request.lastName().trim(),
				normalizeOptional(request.phone())));
		return createSession(user);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		validatePasswordByteLength(request.password());
		UserAccount user = userRepository.findByEmail(normalizeEmail(request.email()))
				.orElseThrow(AuthService::invalidCredentials);
		if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw invalidCredentials();
		}
		return createSession(user);
	}

	@Transactional
	public AuthResponse refresh(String rawRefreshToken) {
		String tokenHash = sha256(rawRefreshToken);
		RefreshToken currentToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
				.orElseThrow(AuthService::invalidCredentials);
		Instant now = Instant.now();
		if (!currentToken.isActiveAt(now)) {
			throw invalidCredentials();
		}

		UserAccount user = currentToken.getUser();
		if (!user.isEnabled()) {
			throw invalidCredentials();
		}
		currentToken.revoke(now);
		return createSession(user);
	}

	@Transactional
	public void logout(UUID userId, String rawRefreshToken) {
		refreshTokenRepository.findByTokenHashForUpdate(sha256(rawRefreshToken))
				.filter(token -> token.getUser().getId().equals(userId))
				.ifPresent(token -> token.revoke(Instant.now()));
	}

	@Transactional
	public void changePassword(UUID userId, ChangePasswordRequest request) {
		validatePasswordByteLength(request.currentPassword());
		validatePasswordByteLength(request.newPassword());
		UserAccount user = userRepository.findByIdForUpdate(userId)
				.orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new BadCredentialsException("Invalid credentials");
		}
		if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
			throw new IllegalArgumentException("New password must be different from the current password");
		}
		user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
		refreshTokenRepository.revokeAllForUser(userId, Instant.now());
	}

	private AuthResponse createSession(UserAccount user) {
		IssuedAccessToken accessToken = jwtTokenService.issueAccessToken(user);
		String refreshToken = newRefreshToken();
		refreshTokenRepository.save(new RefreshToken(
				user,
				sha256(refreshToken),
				Instant.now().plus(refreshTokenTtl)));
		return new AuthResponse(
				"Bearer",
				accessToken.value(),
				accessToken.expiresInSeconds(),
				refreshToken,
				userMapper.toResponse(user));
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static String normalizeOptional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private static void validatePasswordByteLength(String password) {
		if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
			throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes");
		}
	}

	private static String newRefreshToken() {
		byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
		SECURE_RANDOM.nextBytes(randomBytes);
		return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	private static String sha256(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (java.security.NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private static BadCredentialsException invalidCredentials() {
		return new BadCredentialsException("Invalid credentials or refresh token");
	}
}
