package dan.edu.product_service.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

/** Writes the event to the outbox in the caller's transaction; {@link OutboxRelay} delivers it to Kafka. */
@Component
public class ProductCatalogEventPublisher {

	private final OutboxEventRepository outboxRepository;
	private final ObjectMapper objectMapper;
	private final String topic;

	public ProductCatalogEventPublisher(
			OutboxEventRepository outboxRepository,
			ObjectMapper objectMapper,
			@Value("${product.kafka.topic}") String topic) {
		this.outboxRepository = outboxRepository;
		this.objectMapper = objectMapper;
		this.topic = topic;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void publish(ProductCatalogEvent event) {
		outboxRepository.save(new OutboxEvent(topic, event.productId().toString(),
				objectMapper.writeValueAsString(event)));
	}
}
