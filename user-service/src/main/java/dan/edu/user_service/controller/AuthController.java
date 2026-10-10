package dan.edu.user_service.controller;

import java.util.Map;
import java.util.UUID;

import dan.edu.user_service.dto.AuthResponse;
import dan.edu.user_service.dto.LoginRequest;
import dan.edu.user_service.dto.LogoutRequest;
import dan.edu.user_service.dto.RefreshRequest;
import dan.edu.user_service.dto.RegisterRequest;
import dan.edu.user_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/refresh")
	public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return authService.refresh(request.refreshToken());
	}

	@PostMapping("/logout")
	public Map<String, String> logout(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody LogoutRequest request) {
		authService.logout(UUID.fromString(jwt.getSubject()), request.refreshToken());
		return Map.of("message", "Signed out");
	}
}
