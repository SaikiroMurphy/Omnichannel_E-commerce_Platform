package dan.edu.user_service.controller;

import java.util.UUID;

import dan.edu.user_service.dto.AddressRequest;
import dan.edu.user_service.dto.AddressResponse;
import dan.edu.user_service.dto.ChangePasswordRequest;
import dan.edu.user_service.dto.UpdateProfileRequest;
import dan.edu.user_service.dto.UpdateRolesRequest;
import dan.edu.user_service.dto.UpdateStatusRequest;
import dan.edu.user_service.dto.UserPageResponse;
import dan.edu.user_service.dto.UserResponse;
import dan.edu.user_service.service.AuthService;
import dan.edu.user_service.service.UserService;
import dan.edu.user_service.utils.mapper.UserMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;
	private final AuthService authService;
	private final UserMapper userMapper;

	public UserController(UserService userService, AuthService authService, UserMapper userMapper) {
		this.userService = userService;
		this.authService = authService;
		this.userMapper = userMapper;
	}

	@GetMapping("/me")
	public UserResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
		return userService.getProfile(UUID.fromString(jwt.getSubject()));
	}

	@PatchMapping("/me")
	public UserResponse updateProfile(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody UpdateProfileRequest request) {
		return userService.updateProfile(UUID.fromString(jwt.getSubject()), request);
	}

	@PutMapping("/me/password")
	public void changePassword(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(UUID.fromString(jwt.getSubject()), request);
	}

	@GetMapping("/me/address")
	public AddressResponse getAddress(@AuthenticationPrincipal Jwt jwt) {
		return userService.getAddress(UUID.fromString(jwt.getSubject()));
	}

	@PutMapping("/me/address")
	public AddressResponse updateAddress(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody AddressRequest request) {
		return userService.updateAddress(UUID.fromString(jwt.getSubject()), request);
	}

	@DeleteMapping("/me/address")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteAddress(@AuthenticationPrincipal Jwt jwt) {
		userService.removeAddress(UUID.fromString(jwt.getSubject()));
	}

	@GetMapping
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public UserPageResponse listUsers(@PageableDefault(size = 20) Pageable pageable) {
		if (pageable.getPageSize() > 100) {
			throw new IllegalArgumentException("Page size must not exceed 100");
		}
		Page<UserResponse> users = userService.listUsers(pageable);
		return userMapper.toPageResponse(users);
	}

	@GetMapping("/{userId}")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public UserResponse getUser(@PathVariable UUID userId) {
		return userService.getUser(userId);
	}

	@PutMapping("/{userId}/roles")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public UserResponse updateRoles(
			@PathVariable UUID userId,
			@Valid @RequestBody UpdateRolesRequest request) {
		return userService.updateRoles(userId, request);
	}

	@PutMapping("/{userId}/status")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public UserResponse updateStatus(
			@PathVariable UUID userId,
			@Valid @RequestBody UpdateStatusRequest request) {
		return userService.updateStatus(userId, request);
	}
}
