package dan.edu.notification_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import dan.edu.notification_service.model.Notification;
import dan.edu.notification_service.model.NotificationStatus;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	Page<Notification> findByUserId(UUID userId, Pageable pageable);

	Page<Notification> findByUserIdAndStatus(UUID userId, NotificationStatus status, Pageable pageable);

	java.util.Optional<Notification> findByEventTypeAndReferenceId(String eventType, String referenceId);

	Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);

	Optional<Notification> findByIdAndUserId(UUID id, UUID userId);
}
