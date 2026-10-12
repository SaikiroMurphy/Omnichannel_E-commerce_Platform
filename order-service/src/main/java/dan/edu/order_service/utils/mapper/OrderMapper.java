package dan.edu.order_service.utils.mapper;

import java.util.List;

import dan.edu.order_service.dto.OrderItemResponse;
import dan.edu.order_service.dto.OrderPageResponse;
import dan.edu.order_service.dto.OrderResponse;
import dan.edu.order_service.model.Order;
import dan.edu.order_service.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderMapper {

	@Mapping(target = "items", source = "items")
	OrderResponse toResponse(Order order);

	OrderItemResponse toResponse(OrderItem item);

	default OrderPageResponse toPageResponse(Page<Order> page) {
		return new OrderPageResponse(
				page.getContent().stream().map(this::toResponse).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
	}

	default List<OrderItemResponse> toResponseList(List<OrderItem> items) {
		return items == null ? List.of() : items.stream().map(this::toResponse).toList();
	}
}
