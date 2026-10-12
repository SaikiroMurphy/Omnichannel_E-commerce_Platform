package dan.edu.inventory_service.service;

import java.util.UUID;

import dan.edu.inventory_service.common.ConflictException;
import dan.edu.inventory_service.common.ResourceNotFoundException;
import dan.edu.inventory_service.dto.InventoryItemRequest;
import dan.edu.inventory_service.dto.InventoryItemResponse;
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

	private static String normalizeSku(String sku) {
		return sku.trim().toUpperCase();
	}
}
