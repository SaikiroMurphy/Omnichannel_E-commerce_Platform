package dan.edu.order_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import dan.edu.order_service.dto.OrderPaymentEvent;
import dan.edu.order_service.service.OrderService;
import tools.jackson.databind.ObjectMapper;

/** Applies payment results; a FAILED/CANCELLED result cancels the order and releases the reserved stock. */
@Component
public class PaymentResultListener {

	private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

	private final OrderService orderService;
	private final ObjectMapper objectMapper;

	public PaymentResultListener(OrderService orderService, ObjectMapper objectMapper) {
		this.orderService = orderService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "${order.kafka.payment-result-topic}", groupId = "${spring.kafka.consumer.group-id}")
	public void onPaymentResult(String payload) {
		OrderPaymentEvent event;
		try {
			event = objectMapper.readValue(payload, OrderPaymentEvent.class);
		} catch (Exception error) {
			log.error("Malformed payment result sent to DLT: {}", payload, error);
			throw new MalformedPaymentResultEventException("Malformed payment result", error);
		}
		orderService.applyPaymentEvent(event);
	}
}
