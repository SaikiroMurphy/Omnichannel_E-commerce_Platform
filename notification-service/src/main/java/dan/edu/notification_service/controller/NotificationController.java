package dan.edu.notification_service.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dan.edu.notification_service.dto.NotificationPageResponse;
import dan.edu.notification_service.dto.NotificationRequest;
import dan.edu.notification_service.dto.NotificationResponse;
import dan.edu.notification_service.model.NotificationStatus;
import dan.edu.notification_service.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	public NotificationPageResponse list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) UUID userId,
			@RequestParam(required = false) NotificationStatus status,
			@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
		validatePageSize(pageable);
		boolean admin = isAdmin(jwt);
		if (!admin && userId != null && !userId.equals(userId(jwt))) {
			throw new AccessDeniedException("Users can only view their own notifications");
		}
		if (admin && userId == null) {
			return notificationService.getNotificationsByStatus(
					status == null ? NotificationStatus.PENDING : status,
					pageable.getPageNumber(), pageable.getPageSize());
		}
		UUID targetUserId = admin && userId != null ? userId : userId(jwt);
		return status == null
				? notificationService.getNotifications(targetUserId, pageable.getPageNumber(), pageable.getPageSize())
				: notificationService.getNotifications(targetUserId, status, pageable.getPageNumber(), pageable.getPageSize());
	}

	@GetMapping("/{notificationId}")
	public NotificationResponse get(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID notificationId) {
		if (isAdmin(jwt)) {
			return notificationService.getNotification(notificationId);
		}
		return notificationService.getNotificationForUser(userId(jwt), notificationId);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	public NotificationResponse create(@Valid @RequestBody NotificationRequest request) {
		return notificationService.createNotification(request);
	}

	@PostMapping("/internal")
	@PreAuthorize("hasAuthority('ROLE_SERVICE')")
	@ResponseStatus(HttpStatus.CREATED)
	public NotificationResponse createInternal(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody NotificationRequest request) {
		if (!"order-service".equals(jwt.getSubject())) {
			throw new AccessDeniedException("Only order-service can create order notifications");
		}
		return notificationService.createNotification(request);
	}

	@PutMapping("/{notificationId}")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public NotificationResponse update(@PathVariable UUID notificationId, @RequestBody NotificationRequest request) {
		return notificationService.updateNotification(notificationId, request);
	}

	@PostMapping("/{notificationId}/sent")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public NotificationResponse markAsSent(@PathVariable UUID notificationId) {
		return notificationService.markAsSent(notificationId);
	}

	@PostMapping("/{notificationId}/delivered")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public NotificationResponse markAsDelivered(@PathVariable UUID notificationId) {
		return notificationService.markAsDelivered(notificationId);
	}

	@PostMapping("/{notificationId}/failed")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public NotificationResponse markAsFailed(@PathVariable UUID notificationId, @RequestParam(required = false) String reason) {
		return notificationService.markAsFailed(notificationId, reason);
	}

	private static void validatePageSize(Pageable pageable) {
		if (pageable.getPageSize() > 100) {
			throw new IllegalArgumentException("Page size must not exceed 100");
		}
	}

	private static UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	private static boolean isAdmin(Jwt jwt) {
		return jwt.getClaimAsStringList("roles") != null
				&& jwt.getClaimAsStringList("roles").contains("ROLE_ADMIN");
	}
}
