package dan.edu.order_service.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

/** Writes the event to the outbox in the caller's transaction; {@link OutboxRelay} delivers it to Kafka. */
@Component
public class OrderCreatedEventPublisher {

	private final OutboxEventRepository outboxRepository;
	private final ObjectMapper objectMapper;
	private final String topic;

	public OrderCreatedEventPublisher(
			OutboxEventRepository outboxRepository,
			ObjectMapper objectMapper,
			@Value("${order.kafka.order-created-topic}") String topic) {
		this.outboxRepository = outboxRepository;
		this.objectMapper = objectMapper;
		this.topic = topic;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void publish(OrderCreatedEvent event) {
		outboxRepository.save(new OutboxEvent(topic, event.orderId().toString(),
				objectMapper.writeValueAsString(event)));
	}
}
