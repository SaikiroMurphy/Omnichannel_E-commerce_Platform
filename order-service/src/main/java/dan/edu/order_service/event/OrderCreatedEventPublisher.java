package dan.edu.order_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import tools.jackson.databind.ObjectMapper;

@Component
public class OrderCreatedEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventPublisher.class);

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final String topic;

	public OrderCreatedEventPublisher(
			KafkaTemplate<String, String> kafkaTemplate,
			ObjectMapper objectMapper,
			@Value("${order.kafka.order-created-topic}") String topic) {
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
		this.topic = topic;
	}

	public void publishAfterCommit(OrderCreatedEvent event) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					send(event);
				}
			});
			return;
		}
		send(event);
	}

	private void send(OrderCreatedEvent event) {
		try {
			String payload = objectMapper.writeValueAsString(event);
			kafkaTemplate.send(topic, event.orderId().toString(), payload)
					.whenComplete((result, error) -> {
						if (error != null) {
							log.error("Failed to publish OrderCreatedEvent for order {}", event.orderId(), error);
						}
					});
		} catch (Exception error) {
			log.error("Failed to serialize OrderCreatedEvent for order {}", event.orderId(), error);
		}
	}
}
