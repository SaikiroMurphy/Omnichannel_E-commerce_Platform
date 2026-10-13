package dan.edu.product_service.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

	List<OutboxEvent> findBySentAtIsNullOrderByCreatedAtAsc(Pageable pageable);

	@Modifying
	@Query("delete from OutboxEvent e where e.sentAt is not null and e.sentAt < :before")
	int deleteSentBefore(Instant before);
}
