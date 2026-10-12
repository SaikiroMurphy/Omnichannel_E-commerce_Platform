package dan.edu.order_service.model;

import dan.edu.order_service.utils.enums.OrderStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

	@Override
	public String convertToDatabaseColumn(OrderStatus status) {
		return status == null ? null : status.name();
	}

	@Override
	public OrderStatus convertToEntityAttribute(String value) {
		if (value == null) {
			return null;
		}
		return switch (value) {
			case "PAID" -> OrderStatus.CONFIRMED;
			case "PROCESSING", "SHIPPED" -> OrderStatus.SHIPPING;
			case "DELIVERED" -> OrderStatus.COMPLETED;
			default -> OrderStatus.valueOf(value);
		};
	}
}
