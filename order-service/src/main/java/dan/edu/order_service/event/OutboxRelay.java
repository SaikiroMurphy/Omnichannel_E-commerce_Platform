package dan.edu.order_service.event;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxRelay {

	private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
	private static final int BATCH_SIZE = 100;

	private final OutboxEventRepository repository;
	private final KafkaTemplate<String, String> kafkaTemplate;

	public OutboxRelay(OutboxEventRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
		this.repository = repository;
		this.kafkaTemplate = kafkaTemplate;
	}

	// Stops at the first failure so events keep their order.
	@Scheduled(fixedDelayString = "${order.outbox.poll-interval-ms:1000}")
	@Transactional
	public void relay() {
		List<OutboxEvent> pending = repository.findBySentAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE));
		for (OutboxEvent event : pending) {
			try {
				kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
						.get(10, TimeUnit.SECONDS);
				event.markSent();
			} catch (InterruptedException error) {
				Thread.currentThread().interrupt();
				return;
			} catch (Exception error) {
				event.markFailed();
				log.warn("Outbox event {} not delivered (attempt {}): {}",
						event.getId(), event.getAttempts(), error.toString());
				return;
			}
		}
	}

	@Scheduled(cron = "${order.outbox.cleanup-cron:0 0 3 * * *}")
	@Transactional
	public void cleanup() {
		int deleted = repository.deleteSentBefore(Instant.now().minus(Duration.ofDays(7)));
		if (deleted > 0) {
			log.info("Removed {} delivered outbox events", deleted);
		}
	}
}
