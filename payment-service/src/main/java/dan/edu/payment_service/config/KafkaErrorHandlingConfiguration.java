package dan.edu.payment_service.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import dan.edu.payment_service.event.MalformedOrderCreatedEventException;
import dan.edu.payment_service.event.OrderCreatedEvent;
import dan.edu.payment_service.service.OrderPaymentEventPublisher;
import tools.jackson.databind.ObjectMapper;

/** Retries failed records with backoff, then parks them on "<topic>.DLT" instead of blocking the partition. */
@Configuration
public class KafkaErrorHandlingConfiguration {

	private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlingConfiguration.class);

	@Bean
	public DefaultErrorHandler kafkaErrorHandler(
			KafkaOperations<Object, Object> template,
			ObjectProvider<OrderPaymentEventPublisher> paymentEvents,
			ObjectMapper objectMapper) {
		DeadLetterPublishingRecoverer deadLetters = new DeadLetterPublishingRecoverer(template,
				(record, error) -> new TopicPartition(record.topic() + ".DLT", -1));
		ExponentialBackOff backOff = new ExponentialBackOff(500L, 2.0);
		backOff.setMaxElapsedTime(15_000L);
		DefaultErrorHandler handler = new DefaultErrorHandler((record, error) -> {
			deadLetters.accept(record, error);
			compensate(record, paymentEvents.getObject(), objectMapper);
		}, backOff);
		handler.addNotRetryableExceptions(MalformedOrderCreatedEventException.class);
		return handler;
	}

	// No payment could be created for this order, so tell the order service to cancel it and release its stock.
	private static void compensate(
			ConsumerRecord<?, ?> record, OrderPaymentEventPublisher paymentEvents, ObjectMapper objectMapper) {
		try {
			OrderCreatedEvent event = objectMapper.readValue((String) record.value(), OrderCreatedEvent.class);
			if (event.orderId() != null && event.userId() != null
					&& event.totalAmount() != null && event.currency() != null) {
				paymentEvents.publishCreationFailed(
						event.userId(), event.orderId(), event.totalAmount(), event.currency());
			}
		} catch (Exception error) {
			log.error("Could not publish compensating payment failure for record {}", record, error);
		}
	}
}
