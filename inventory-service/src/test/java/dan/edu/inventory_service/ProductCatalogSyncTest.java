package dan.edu.inventory_service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import dan.edu.inventory_service.event.ProductCatalogEvent;
import dan.edu.inventory_service.model.InventoryItem;
import dan.edu.inventory_service.repository.InventoryItemRepository;
import dan.edu.inventory_service.service.InventoryService;
import dan.edu.inventory_service.utils.mapper.InventoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCatalogSyncTest {

	@Mock
	private InventoryItemRepository inventoryItemRepository;

	@Mock
	private InventoryMapper inventoryMapper;

	@InjectMocks
	private InventoryService inventoryService;

	@Test
	void createsInventoryItemWhenProductIsCreated() {
		UUID productId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
		when(inventoryItemRepository.findByProductId(productId)).thenReturn(Optional.empty());
		when(inventoryItemRepository.findBySkuIgnoreCase("TEA-001")).thenReturn(Optional.empty());
		when(inventoryItemRepository.save(any(InventoryItem.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		inventoryService.applyProductCatalog(new ProductCatalogEvent(
				ProductCatalogEvent.CREATED,
				productId,
				"tea-001",
				null,
				new BigDecimal("199.50"),
				true,
				Instant.now()));

		ArgumentCaptor<InventoryItem> captor = ArgumentCaptor.forClass(InventoryItem.class);
		verify(inventoryItemRepository).save(captor.capture());
		assertThat(captor.getValue().getProductId()).isEqualTo(productId);
		assertThat(captor.getValue().getSku()).isEqualTo("TEA-001");
		assertThat(captor.getValue().getPrice()).isEqualByComparingTo("199.50");
		assertThat(captor.getValue().getAvailableQuantity()).isZero();
	}

	@Test
	void linksExistingSkuAndUpdatesPriceWhenProductIsUpdated() {
		UUID productId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
		InventoryItem existing = InventoryItem.fromProduct(null, "TEA-001", BigDecimal.ONE);
		when(inventoryItemRepository.findByProductId(productId)).thenReturn(Optional.empty());
		when(inventoryItemRepository.findBySkuIgnoreCase("TEA-001")).thenReturn(Optional.of(existing));

		inventoryService.applyProductCatalog(new ProductCatalogEvent(
				ProductCatalogEvent.UPDATED,
				productId,
				"TEA-001",
				"TEA-001",
				new BigDecimal("12.00"),
				true,
				Instant.now()));

		assertThat(existing.getProductId()).isEqualTo(productId);
		assertThat(existing.getPrice()).isEqualByComparingTo("12.00");
	}
}
