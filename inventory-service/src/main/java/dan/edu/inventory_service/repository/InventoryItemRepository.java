package dan.edu.inventory_service.repository;

import java.util.Optional;
import java.util.UUID;

import dan.edu.inventory_service.model.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

	boolean existsBySkuIgnoreCase(String sku);

	boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);

	Optional<InventoryItem> findBySkuIgnoreCase(String sku);

	Optional<InventoryItem> findByProductId(UUID productId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select item from InventoryItem item where lower(item.sku) = lower(:sku)")
	Optional<InventoryItem> findBySkuForUpdate(@Param("sku") String sku);

	Optional<InventoryItem> findByIdAndActiveTrue(UUID id);

	@Query("""
			select item from InventoryItem item
			where item.active = true
			  and (:sku is null or lower(item.sku) like lower(concat('%', :sku, '%')))
			  and (:location is null or lower(coalesce(item.location, '')) like lower(concat('%', :location, '%')))
			""")
	Page<InventoryItem> searchActive(
			@Param("sku") String sku,
			@Param("location") String location,
			Pageable pageable);
}
