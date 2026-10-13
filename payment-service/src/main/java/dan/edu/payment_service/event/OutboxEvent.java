package dan.edu.payment_service.event;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_events", indexes = @Index(name = "idx_outbox_unsent", columnList = "sentAt, createdAt"))
public class OutboxEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String topic;

	@Column(nullable = false)
	private String messageKey;

	@Column(nullable = false, columnDefinition = "text")
	private String payload;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	private Instant sentAt;

	private int attempts;

	protected OutboxEvent() {
	}

	public OutboxEvent(String topic, String messageKey, String payload) {
		this.topic = topic;
		this.messageKey = messageKey;
		this.payload = payload;
	}

	public UUID getId() {
		return id;
	}

	public String getTopic() {
		return topic;
	}

	public String getMessageKey() {
		return messageKey;
	}

	public String getPayload() {
		return payload;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getSentAt() {
		return sentAt;
	}

	public int getAttempts() {
		return attempts;
	}

	public void markSent() {
		this.sentAt = Instant.now();
	}

	public void markFailed() {
		this.attempts++;
	}
}
