package dan.edu.payment_service.service;

import java.util.UUID;

import dan.edu.payment_service.dto.PaymentOrderEvent;
import dan.edu.payment_service.dto.PaymentResponse;
import dan.edu.payment_service.event.OutboxEvent;
import dan.edu.payment_service.event.OutboxEventRepository;
import dan.edu.payment_service.utils.enums.PaymentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

/**
 * Records payment outcomes in the outbox in the caller's transaction; the order service
 * consumes them to confirm the order or run the compensating stock release.
 */
@Component
public class OrderPaymentEventPublisher {

	private final OutboxEventRepository outboxRepository;
	private final ObjectMapper objectMapper;
	private final String topic;

	public OrderPaymentEventPublisher(
			OutboxEventRepository outboxRepository,
			ObjectMapper objectMapper,
			@Value("${payment.kafka.payment-result-topic}") String topic) {
		this.outboxRepository = outboxRepository;
		this.objectMapper = objectMapper;
		this.topic = topic;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void publish(PaymentResponse payment, UUID userId) {
		if (payment.status() != PaymentStatus.SUCCEEDED
				&& payment.status() != PaymentStatus.FAILED
				&& payment.status() != PaymentStatus.CANCELLED) {
			return;
		}
		write(new PaymentOrderEvent(userId, payment.orderId(), payment.amount(),
				payment.currency(), payment.status().name()));
	}

	/** Used when no payment could be created for an order, so its stock must be released. */
	@Transactional
	public void publishCreationFailed(UUID userId, UUID orderId, java.math.BigDecimal amount, String currency) {
		write(new PaymentOrderEvent(userId, orderId, amount, currency, PaymentStatus.FAILED.name()));
	}

	private void write(PaymentOrderEvent event) {
		outboxRepository.save(new OutboxEvent(topic, event.orderId().toString(),
				objectMapper.writeValueAsString(event)));
	}
}
