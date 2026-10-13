package dan.edu.product_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import tools.jackson.databind.ObjectMapper;

@Component
public class ProductCatalogEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(ProductCatalogEventPublisher.class);

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final String topic;

	public ProductCatalogEventPublisher(
			KafkaTemplate<String, String> kafkaTemplate,
			ObjectMapper objectMapper,
			@Value("${product.kafka.topic}") String topic) {
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
		this.topic = topic;
	}

	public void publishAfterCommit(ProductCatalogEvent event) {
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

	private void send(ProductCatalogEvent event) {
		try {
			String payload = objectMapper.writeValueAsString(event);
			kafkaTemplate.send(topic, event.productId().toString(), payload)
					.whenComplete((result, error) -> {
						if (error != null) {
							log.error("Failed to publish product catalog event {} for product {}",
									event.type(), event.productId(), error);
						}
					});
		} catch (Exception error) {
			log.error("Failed to serialize product catalog event {} for product {}",
					event.type(), event.productId(), error);
		}
	}
}
