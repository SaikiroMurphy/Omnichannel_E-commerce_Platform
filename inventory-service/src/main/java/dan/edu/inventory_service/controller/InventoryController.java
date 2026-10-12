package dan.edu.inventory_service.controller;

import java.util.UUID;

import dan.edu.inventory_service.dto.InventoryItemRequest;
import dan.edu.inventory_service.dto.InventoryItemResponse;
import dan.edu.inventory_service.dto.InventoryPageResponse;
import dan.edu.inventory_service.dto.StockReservationRequest;
import dan.edu.inventory_service.dto.StockReservationResponse;
import dan.edu.inventory_service.service.InventoryService;
import dan.edu.inventory_service.service.StockReservationService;
import dan.edu.inventory_service.utils.mapper.InventoryMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

	private final InventoryService inventoryService;
	private final InventoryMapper inventoryMapper;
	private final StockReservationService stockReservationService;

	public InventoryController(
			InventoryService inventoryService,
			InventoryMapper inventoryMapper,
			StockReservationService stockReservationService) {
		this.inventoryService = inventoryService;
		this.inventoryMapper = inventoryMapper;
		this.stockReservationService = stockReservationService;
	}

	@PostMapping("/internal/reservations")
	@PreAuthorize("hasAuthority('ROLE_SERVICE')")
	@ResponseStatus(HttpStatus.CREATED)
	public StockReservationResponse reserve(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody StockReservationRequest request) {
		requireOrderService(jwt);
		return stockReservationService.reserve(request);
	}

	@PostMapping("/internal/reservations/{orderId}/confirm")
	@PreAuthorize("hasAuthority('ROLE_SERVICE')")
	public StockReservationResponse confirmReservation(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID orderId) {
		requireOrderService(jwt);
		return stockReservationService.confirm(orderId);
	}

	@PostMapping("/internal/reservations/{orderId}/release")
	@PreAuthorize("hasAuthority('ROLE_SERVICE')")
	public StockReservationResponse releaseReservation(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID orderId) {
		requireOrderService(jwt);
		return stockReservationService.release(orderId);
	}

	@GetMapping("/items")
	public InventoryPageResponse search(
			@RequestParam(required = false) String sku,
			@RequestParam(required = false) String location,
			@PageableDefault(size = 20, sort = "sku") Pageable pageable) {
		validatePageSize(pageable);
		return inventoryMapper.toPageResponse(inventoryService.search(sku, location, pageable));
	}

	@GetMapping("/items/{itemId}")
	public InventoryItemResponse get(@PathVariable UUID itemId) {
		return inventoryService.get(itemId);
	}

	@GetMapping("/items/sku/{sku}")
	public InventoryItemResponse getBySku(@PathVariable String sku) {
		return inventoryService.getBySku(sku);
	}

	@PostMapping("/items")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	public InventoryItemResponse create(@Valid @RequestBody InventoryItemRequest request) {
		return inventoryService.create(request);
	}

	@PutMapping("/items/{itemId}")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public InventoryItemResponse update(@PathVariable UUID itemId, @Valid @RequestBody InventoryItemRequest request) {
		return inventoryService.update(itemId, request);
	}

	@DeleteMapping("/items/{itemId}")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivate(@PathVariable UUID itemId) {
		inventoryService.deactivate(itemId);
	}

	@PostMapping("/items/{itemId}/activate")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public InventoryItemResponse activate(@PathVariable UUID itemId) {
		return inventoryService.activate(itemId);
	}

	@PostMapping("/items/{itemId}/stock/add")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public InventoryItemResponse addStock(@PathVariable UUID itemId, @RequestParam @Min(1) int quantity) {
		return inventoryService.addStock(itemId, quantity);
	}

	@PostMapping("/items/{itemId}/stock/remove")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public InventoryItemResponse removeStock(@PathVariable UUID itemId, @RequestParam @Min(1) int quantity) {
		return inventoryService.removeStock(itemId, quantity);
	}

	@PostMapping("/items/{itemId}/stock/reserve")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public InventoryItemResponse reserveStock(@PathVariable UUID itemId, @RequestParam @Min(1) int quantity) {
		return inventoryService.reserveStock(itemId, quantity);
	}

	@PostMapping("/items/{itemId}/stock/release")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public InventoryItemResponse releaseStock(@PathVariable UUID itemId, @RequestParam @Min(1) int quantity) {
		return inventoryService.releaseStock(itemId, quantity);
	}

	private static void validatePageSize(Pageable pageable) {
		if (pageable.getPageSize() > 100) {
			throw new IllegalArgumentException("Page size must not exceed 100");
		}
	}

	private static void requireOrderService(Jwt jwt) {
		if (!"order-service".equals(jwt.getSubject())) {
			throw new AccessDeniedException("Only order-service can manage stock reservations");
		}
	}
}
