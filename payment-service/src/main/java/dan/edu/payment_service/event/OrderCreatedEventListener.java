package dan.edu.payment_service.event;

import dan.edu.payment_service.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
public class OrderCreatedEventListener {

	private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventListener.class);

	private final PaymentService paymentService;
	private final ObjectMapper objectMapper;

	public OrderCreatedEventListener(PaymentService paymentService, ObjectMapper objectMapper) {
		this.paymentService = paymentService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "${payment.kafka.order-created-topic}", groupId = "${spring.kafka.consumer.group-id}")
	public void onOrderCreated(String payload) {
		OrderCreatedEvent event;
		try {
			event = objectMapper.readValue(payload, OrderCreatedEvent.class);
		} catch (Exception error) {
			log.error("Ignoring malformed OrderCreatedEvent: {}", payload, error);
			return;
		}
		paymentService.createFromOrderCreated(event);
	}
}
