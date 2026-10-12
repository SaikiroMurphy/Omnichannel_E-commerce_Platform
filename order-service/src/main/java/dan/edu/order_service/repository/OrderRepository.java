package dan.edu.order_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dan.edu.order_service.model.Order;
import dan.edu.order_service.utils.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, UUID> {

	Page<Order> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

	Page<Order> findAllByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

	Page<Order> findAllByStatusAndUserIdOrderByCreatedAtDesc(OrderStatus status, UUID userId, Pageable pageable);

	Optional<Order> findByIdAndUserId(UUID id, UUID userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from Order o where o.id = :id and o.userId = :userId")
	Optional<Order> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from Order o where o.id = :id")
	Optional<Order> findByIdForUpdate(@Param("id") UUID id);

	List<Order> findAllByUserId(UUID userId);
}
