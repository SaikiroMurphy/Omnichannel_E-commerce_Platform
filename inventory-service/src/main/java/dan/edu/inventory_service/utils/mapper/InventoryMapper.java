package dan.edu.inventory_service.utils.mapper;

import dan.edu.inventory_service.dto.InventoryItemResponse;
import dan.edu.inventory_service.dto.InventoryPageResponse;
import dan.edu.inventory_service.model.InventoryItem;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InventoryMapper {

	InventoryItemResponse toResponse(InventoryItem item);

	default InventoryPageResponse toPageResponse(Page<InventoryItemResponse> items) {
		return new InventoryPageResponse(
				items.getContent(),
				items.getNumber(),
				items.getSize(),
				items.getTotalElements(),
				items.getTotalPages(),
				items.isFirst(),
				items.isLast());
	}
}
