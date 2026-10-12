package dan.edu.inventory_service.repository;

import java.util.List;
import java.util.UUID;

import dan.edu.inventory_service.model.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

	List<StockReservation> findAllByOrderIdOrderBySku(UUID orderId);
}
