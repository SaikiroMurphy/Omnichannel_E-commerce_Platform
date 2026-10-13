package dan.edu.inventory_service.service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import dan.edu.inventory_service.common.ConflictException;
import dan.edu.inventory_service.common.ResourceNotFoundException;
import dan.edu.inventory_service.dto.InventoryItemRequest;
import dan.edu.inventory_service.dto.InventoryItemResponse;
import dan.edu.inventory_service.event.ProductCatalogEvent;
import dan.edu.inventory_service.model.InventoryItem;
import dan.edu.inventory_service.repository.InventoryItemRepository;
import dan.edu.inventory_service.utils.mapper.InventoryMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

	private final InventoryItemRepository inventoryItemRepository;
	private final InventoryMapper inventoryMapper;

	public InventoryService(InventoryItemRepository inventoryItemRepository, InventoryMapper inventoryMapper) {
		this.inventoryItemRepository = inventoryItemRepository;
		this.inventoryMapper = inventoryMapper;
	}

	@Transactional(readOnly = true)
	public Page<InventoryItemResponse> search(String sku, String location, Pageable pageable) {
		return inventoryItemRepository.searchActive(normalize(sku), normalize(location), pageable)
				.map(inventoryMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public InventoryItemResponse get(UUID itemId) {
		return inventoryMapper.toResponse(inventoryItemRepository.findByIdAndActiveTrue(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Inventory item not found")));
	}

	@Transactional(readOnly = true)
	public InventoryItemResponse getBySku(String sku) {
		String normalizedSku = normalizeSku(sku);
		return inventoryMapper.toResponse(inventoryItemRepository.findBySkuIgnoreCase(normalizedSku)
				.orElseThrow(() -> new ResourceNotFoundException("Inventory item not found")));
	}

	@Transactional
	public void applyProductCatalog(ProductCatalogEvent event) {
		if (event == null || event.productId() == null || event.sku() == null || event.sku().isBlank()) {
			return;
		}

		String sku = normalizeSku(event.sku());
		InventoryItem item = findLinkedItem(event, sku).orElse(null);
		String type = event.type() == null ? "" : event.type().trim().toUpperCase(Locale.ROOT);

		switch (type) {
			case ProductCatalogEvent.CREATED, ProductCatalogEvent.UPDATED -> upsertFromCatalog(item, event, sku);
			case ProductCatalogEvent.DEACTIVATED -> {
				if (item != null) {
					item.deactivate();
				}
			}
			case ProductCatalogEvent.ACTIVATED -> {
				if (item != null) {
					item.activate();
				}
			}
			default -> {
			}
		}
	}

	@Transactional
	public InventoryItemResponse create(InventoryItemRequest request) {
		String sku = normalizeSku(request.sku());
		if (inventoryItemRepository.existsBySkuIgnoreCase(sku)) {
			throw new ConflictException("A stock item with this SKU already exists");
		}
		return inventoryMapper.toResponse(inventoryItemRepository.save(new InventoryItem(request)));
	}

	@Transactional
	public InventoryItemResponse update(UUID itemId, InventoryItemRequest request) {
		InventoryItem item = inventoryItemRepository.findById(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
		String sku = normalizeSku(request.sku());
		if (inventoryItemRepository.existsBySkuIgnoreCaseAndIdNot(sku, itemId)) {
			throw new ConflictException("A stock item with this SKU already exists");
		}
		item.update(request);
		return inventoryMapper.toResponse(item);
	}

	@Transactional
	public void deactivate(UUID itemId) {
		InventoryItem item = inventoryItemRepository.findById(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
		item.deactivate();
	}

	@Transactional
	public InventoryItemResponse activate(UUID itemId) {
		InventoryItem item = inventoryItemRepository.findById(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
		item.activate();
		return inventoryMapper.toResponse(item);
	}

	@Transactional
	public InventoryItemResponse addStock(UUID itemId, int quantity) {
		InventoryItem item = inventoryItemRepository.findById(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
		item.addStock(quantity);
		return inventoryMapper.toResponse(item);
	}

	@Transactional
	public InventoryItemResponse removeStock(UUID itemId, int quantity) {
		InventoryItem item = inventoryItemRepository.findById(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
		item.removeStock(quantity);
		return inventoryMapper.toResponse(item);
	}

	@Transactional
	public InventoryItemResponse reserveStock(UUID itemId, int quantity) {
		InventoryItem item = inventoryItemRepository.findById(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
		item.reserveStock(quantity);
		return inventoryMapper.toResponse(item);
	}

	@Transactional
	public InventoryItemResponse releaseStock(UUID itemId, int quantity) {
		InventoryItem item = inventoryItemRepository.findById(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
		item.releaseStock(quantity);
		return inventoryMapper.toResponse(item);
	}

	private static String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private Optional<InventoryItem> findLinkedItem(ProductCatalogEvent event, String sku) {
		Optional<InventoryItem> byProduct = inventoryItemRepository.findByProductId(event.productId());
		if (byProduct.isPresent()) {
			return byProduct;
		}
		Optional<InventoryItem> bySku = inventoryItemRepository.findBySkuIgnoreCase(sku);
		if (bySku.isPresent()) {
			return bySku;
		}
		if (event.previousSku() == null || event.previousSku().isBlank()) {
			return Optional.empty();
		}
		return inventoryItemRepository.findBySkuIgnoreCase(normalizeSku(event.previousSku()));
	}

	private void upsertFromCatalog(InventoryItem item, ProductCatalogEvent event, String sku) {
		if (item == null) {
			inventoryItemRepository.save(InventoryItem.fromProduct(event.productId(), sku, event.price()));
			return;
		}
		if (item.getProductId() != null && !item.getProductId().equals(event.productId())) {
			if (inventoryItemRepository.findByProductId(event.productId()).isEmpty()
					&& !inventoryItemRepository.existsBySkuIgnoreCase(sku)) {
				inventoryItemRepository.save(InventoryItem.fromProduct(event.productId(), sku, event.price()));
			}
			return;
		}
		item.linkProduct(event.productId());
		item.syncCatalog(sku, event.price(), event.active());
	}

	private static String normalizeSku(String sku) {
		return sku.trim().toUpperCase(Locale.ROOT);
	}
}
