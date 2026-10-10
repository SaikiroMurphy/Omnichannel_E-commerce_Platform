package dan.edu.product_service.repository;

import java.util.Optional;
import java.util.UUID;

import dan.edu.product_service.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

	boolean existsBySkuIgnoreCase(String sku);

	boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);

	Optional<Product> findByIdAndActiveTrue(UUID id);

	@Query("""
			select product from Product product
			where product.active = true
			  and (:category is null or lower(product.category) = lower(:category))
			  and (:query is null
			       or lower(product.name) like lower(concat('%', :query, '%'))
			       or lower(product.sku) like lower(concat('%', :query, '%'))
			       or lower(coalesce(product.description, '')) like lower(concat('%', :query, '%')))
			""")
	Page<Product> searchActive(
			@Param("query") String query,
			@Param("category") String category,
			Pageable pageable);
}
