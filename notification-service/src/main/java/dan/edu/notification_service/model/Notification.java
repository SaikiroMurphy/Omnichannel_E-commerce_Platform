package dan.edu.notification_service.model;

import java.time.Instant;
import java.util.UUID;

import dan.edu.notification_service.common.InvalidNotificationStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "notifications",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_notifications_event_reference",
				columnNames = {"event_type", "reference_id"}))
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@NotNull
	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@NotBlank
	@Size(max = 255)
	@Column(name = "title", nullable = false, length = 255)
	private String title;

	@NotBlank
	@Size(max = 2000)
	@Column(name = "message", nullable = false, length = 2000)
	private String message;

	@Size(max = 255)
	@Column(name = "channel", length = 255)
	private String channel = "EMAIL";

	@Size(max = 255)
	@Column(name = "event_type", length = 255)
	private String eventType;

	@Size(max = 255)
	@Column(name = "reference_id", length = 255)
	private String referenceId;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 50)
	private NotificationStatus status = NotificationStatus.PENDING;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Notification() {
	}

	public Notification(UUID userId, String title, String message, String channel, String eventType, String referenceId) {
		this.userId = userId;
		this.title = title;
		this.message = message;
		this.channel = channel == null || channel.isBlank() ? "EMAIL" : channel;
		this.eventType = eventType;
		this.referenceId = referenceId;
		this.status = NotificationStatus.PENDING;
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	@PrePersist
	void initializeTimestamps() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	@PreUpdate
	void refreshUpdatedAt() {
		updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel == null || channel.isBlank() ? "EMAIL" : channel;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getReferenceId() {
		return referenceId;
	}

	public String getStatus() {
		return status.name();
	}

	public void setStatus(NotificationStatus status) {
		this.status = status;
		this.updatedAt = Instant.now();
	}

	public void markSent() {
		if (this.status != NotificationStatus.PENDING) {
			throw new InvalidNotificationStateException("Only PENDING notifications can be marked as SENT");
		}
		this.status = NotificationStatus.SENT;
		this.updatedAt = Instant.now();
	}

	public void markFailed() {
		if (this.status == NotificationStatus.DELIVERED) {
			throw new InvalidNotificationStateException("DELIVERED notifications cannot be marked as FAILED");
		}
		this.status = NotificationStatus.FAILED;
		this.updatedAt = Instant.now();
	}

	public void markDelivered() {
		if (this.status != NotificationStatus.SENT) {
			throw new InvalidNotificationStateException("Only SENT notifications can be marked as DELIVERED");
		}
		this.status = NotificationStatus.DELIVERED;
		this.updatedAt = Instant.now();
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	@Override
	public String toString() {
		return "Notification{" +
				"id=" + id +
				", userId=" + userId +
				", title='" + title + '\'' +
				", status=" + status +
				'}';
	}
}
