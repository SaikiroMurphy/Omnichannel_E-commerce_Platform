package dan.edu.billing_service.repository;

import java.util.Optional;
import java.util.UUID;

import dan.edu.billing_service.model.Invoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

	boolean existsByOrderId(UUID orderId);

	Page<Invoice> findAllByCustomerId(UUID customerId, Pageable pageable);

	Optional<Invoice> findByIdAndCustomerId(UUID id, UUID customerId);

	Optional<Invoice> findByOrderIdAndCustomerId(UUID orderId, UUID customerId);

	Optional<Invoice> findByOrderId(UUID orderId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select invoice from Invoice invoice where invoice.id = :id")
	Optional<Invoice> findByIdForUpdate(@Param("id") UUID id);
}
