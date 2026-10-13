package dan.edu.notification_service.event;

import dan.edu.notification_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
public class OrderCreatedEventListener {

	private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventListener.class);

	private final NotificationService notificationService;
	private final ObjectMapper objectMapper;

	public OrderCreatedEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
		this.notificationService = notificationService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "${notification.kafka.order-created-topic}", groupId = "${spring.kafka.consumer.group-id}")
	public void onOrderCreated(String payload) {
		OrderCreatedEvent event;
		try {
			event = objectMapper.readValue(payload, OrderCreatedEvent.class);
		} catch (Exception error) {
			log.error("Ignoring malformed OrderCreatedEvent: {}", payload, error);
			return;
		}
		notificationService.notifyOrderCreated(event);
	}
}
