package dan.edu.notification_service.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;

import dan.edu.notification_service.common.ConflictException;
import dan.edu.notification_service.common.InvalidNotificationStateException;
import dan.edu.notification_service.common.ResourceNotFoundException;
import dan.edu.notification_service.dto.NotificationPageResponse;
import dan.edu.notification_service.dto.NotificationRequest;
import dan.edu.notification_service.dto.NotificationResponse;
import dan.edu.notification_service.model.Notification;
import dan.edu.notification_service.model.NotificationStatus;
import dan.edu.notification_service.repository.NotificationRepository;
import dan.edu.notification_service.utils.mapper.NotificationMapper;

@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final NotificationMapper notificationMapper;
	private final StringRedisTemplate redis;

	public NotificationService(
			NotificationRepository notificationRepository,
			NotificationMapper notificationMapper,
			StringRedisTemplate redis) {
		this.notificationRepository = notificationRepository;
		this.notificationMapper = notificationMapper;
		this.redis = redis;
	}

	@Transactional(readOnly = true)
	public NotificationPageResponse getNotifications(UUID userId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Notification> result = notificationRepository.findByUserId(userId, pageable);
		return mapPage(result, page, size);
	}

	@Transactional(readOnly = true)
	public NotificationPageResponse getNotifications(UUID userId, NotificationStatus status, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Notification> result = notificationRepository.findByUserIdAndStatus(userId, status, pageable);
		return mapPage(result, page, size);
	}

	@Transactional(readOnly = true)
	public NotificationPageResponse getNotificationsByStatus(NotificationStatus status, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Notification> result = notificationRepository.findByStatus(status, pageable);
		return mapPage(result, page, size);
	}

	@Transactional(readOnly = true)
	public NotificationResponse getNotification(UUID id) {
		Notification notification = notificationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found with id=" + id));
		return notificationMapper.toResponse(notification);
	}

	@Transactional(readOnly = true)
	public NotificationResponse getNotificationForUser(UUID userId, UUID notificationId) {
		Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found for userId=" + userId + " and id=" + notificationId));
		return notificationMapper.toResponse(notification);
	}

	@Transactional
	public NotificationResponse createNotification(NotificationRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Notification request is required");
		}
		if (request.eventType() != null && request.referenceId() != null) {
			var existing = notificationRepository.findByEventTypeAndReferenceId(
					request.eventType(), request.referenceId());
			if (existing.isPresent()) {
				return notificationMapper.toResponse(existing.get());
			}
		}
		Notification entity = notificationMapper.toEntity(request);
		Notification saved = notificationRepository.save(entity);
		redis.opsForList().rightPush("notification:delivery:queue", saved.getId().toString());
		return notificationMapper.toResponse(saved);
	}

	@Transactional
	public NotificationResponse updateNotification(UUID id, NotificationRequest request) {
		Notification notification = notificationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found with id=" + id));
		if (request.userId() != null && !request.userId().equals(notification.getUserId())) {
			throw new ConflictException("Notification userId cannot be changed after creation");
		}
		notificationMapper.updateEntityFromRequest(request, notification);
		notification.setUpdatedAt(java.time.Instant.now());
		Notification saved = notificationRepository.save(notification);
		return notificationMapper.toResponse(saved);
	}

	@Transactional
	public NotificationResponse markAsSent(UUID id) {
		Notification notification = notificationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found with id=" + id));
		notification.markSent();
		return notificationMapper.toResponse(notificationRepository.save(notification));
	}

	@Transactional
	public NotificationResponse markAsDelivered(UUID id) {
		Notification notification = notificationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found with id=" + id));
		notification.markDelivered();
		return notificationMapper.toResponse(notificationRepository.save(notification));
	}

	@Transactional
	public NotificationResponse markAsFailed(UUID id, String reason) {
		Notification notification = notificationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found with id=" + id));
		if (reason != null && !reason.isBlank()) {
			notification.setMessage(notification.getMessage() + " | Failure: " + reason);
		}
		notification.markFailed();
		return notificationMapper.toResponse(notificationRepository.save(notification));
	}

	private NotificationPageResponse mapPage(Page<Notification> page, int requestedPage, int requestedSize) {
		return new NotificationPageResponse(
				notificationMapper.toResponseList(page.getContent()),
				page.getTotalElements(),
				page.getTotalPages(),
				requestedPage,
				requestedSize);
	}
}
