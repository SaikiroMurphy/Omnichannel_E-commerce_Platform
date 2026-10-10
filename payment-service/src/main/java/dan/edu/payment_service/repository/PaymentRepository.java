package dan.edu.payment_service.repository;

import java.util.Optional;
import java.util.UUID;

import dan.edu.payment_service.model.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

	Optional<Payment> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

	Optional<Payment> findByIdAndUserId(UUID id, UUID userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select payment from Payment payment where payment.id = :id and payment.userId = :userId")
	Optional<Payment> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select payment from Payment payment where payment.id = :id")
	Optional<Payment> findByIdForUpdate(@Param("id") UUID id);

	Page<Payment> findAllByUserId(UUID userId, Pageable pageable);

	Optional<Payment> findByOrderIdAndUserId(UUID orderId, UUID userId);
}
