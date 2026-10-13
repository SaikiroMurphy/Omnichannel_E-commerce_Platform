package dan.edu.inventory_service.event;

import dan.edu.inventory_service.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
public class ProductCatalogEventListener {

	private static final Logger log = LoggerFactory.getLogger(ProductCatalogEventListener.class);

	private final InventoryService inventoryService;
	private final ObjectMapper objectMapper;

	public ProductCatalogEventListener(InventoryService inventoryService, ObjectMapper objectMapper) {
		this.inventoryService = inventoryService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "${inventory.kafka.product-topic}", groupId = "${spring.kafka.consumer.group-id}")
	public void onProductCatalogEvent(String payload) {
		ProductCatalogEvent event;
		try {
			event = objectMapper.readValue(payload, ProductCatalogEvent.class);
		} catch (Exception error) {
			log.error("Malformed product catalog event sent to DLT: {}", payload, error);
			throw new MalformedProductCatalogEventException("Malformed product catalog event", error);
		}
		inventoryService.applyProductCatalog(event);
	}
}
