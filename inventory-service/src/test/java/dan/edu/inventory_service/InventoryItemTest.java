package dan.edu.inventory_service;

import java.math.BigDecimal;

import dan.edu.inventory_service.common.InvalidInventoryStateException;
import dan.edu.inventory_service.dto.InventoryItemRequest;
import dan.edu.inventory_service.model.InventoryItem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryItemTest {

	@Test
	void updatesPreserveReservationsAndDoNotAllowChangingReservedSku() {
		InventoryItem item = new InventoryItem(request("SKU-1", 10));
		item.reserveStock(4);

		item.update(request("SKU-1", 8));

		assertThat(item.getAvailableQuantity()).isEqualTo(8);
		assertThat(item.getReservedQuantity()).isEqualTo(4);
		assertThatThrownBy(() -> item.update(request("SKU-2", 8)))
				.isInstanceOf(InvalidInventoryStateException.class)
				.hasMessageContaining("SKU cannot be changed");
	}

	private static InventoryItemRequest request(String sku, int availableQuantity) {
		return new InventoryItemRequest(
				sku, BigDecimal.ONE, BigDecimal.ZERO, availableQuantity, null);
	}
}
