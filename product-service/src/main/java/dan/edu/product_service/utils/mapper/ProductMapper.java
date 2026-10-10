package dan.edu.product_service.utils.mapper;

import dan.edu.product_service.dto.ProductPageResponse;
import dan.edu.product_service.dto.ProductResponse;
import dan.edu.product_service.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductMapper {

	ProductResponse toResponse(Product product);

	default ProductPageResponse toPageResponse(Page<ProductResponse> products) {
		return new ProductPageResponse(
				products.getContent(),
				products.getNumber(),
				products.getSize(),
				products.getTotalElements(),
				products.getTotalPages(),
				products.isFirst(),
				products.isLast());
	}
}
