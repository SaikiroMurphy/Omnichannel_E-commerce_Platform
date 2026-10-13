package dan.edu.order_service.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import dan.edu.order_service.common.ConflictException;
import dan.edu.order_service.common.InvalidOrderStateException;
import dan.edu.order_service.common.ResourceNotFoundException;
import dan.edu.order_service.event.MalformedPaymentResultEventException;

/** Retries transient failures with backoff, then parks the record on "<topic>.DLT". */
@Configuration
public class KafkaErrorHandlingConfiguration {

	@Bean
	public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> template) {
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
				(record, error) -> new TopicPartition(record.topic() + ".DLT", -1));
		ExponentialBackOff backOff = new ExponentialBackOff(500L, 2.0);
		backOff.setMaxElapsedTime(15_000L);
		DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
		handler.addNotRetryableExceptions(MalformedPaymentResultEventException.class,
				InvalidOrderStateException.class, ConflictException.class, ResourceNotFoundException.class);
		return handler;
	}
}
