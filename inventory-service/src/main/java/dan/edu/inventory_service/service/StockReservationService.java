package dan.edu.inventory_service.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import dan.edu.inventory_service.common.ConflictException;
import dan.edu.inventory_service.common.ResourceNotFoundException;
import dan.edu.inventory_service.dto.StockReservationRequest;
import dan.edu.inventory_service.dto.StockReservationResponse;
import dan.edu.inventory_service.model.InventoryItem;
import dan.edu.inventory_service.model.ReservationStatus;
import dan.edu.inventory_service.model.StockReservation;
import dan.edu.inventory_service.repository.InventoryItemRepository;
import dan.edu.inventory_service.repository.StockReservationRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class StockReservationService {

	private static final Duration LOCK_TTL = Duration.ofSeconds(30);
	private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
			"if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
			Long.class);

	private final InventoryItemRepository itemRepository;
	private final StockReservationRepository reservationRepository;
	private final StringRedisTemplate redis;
	private final TransactionTemplate transactions;

	public StockReservationService(
			InventoryItemRepository itemRepository,
			StockReservationRepository reservationRepository,
			StringRedisTemplate redis,
			TransactionTemplate transactions) {
		this.itemRepository = itemRepository;
		this.reservationRepository = reservationRepository;
		this.redis = redis;
		this.transactions = transactions;
	}

	public StockReservationResponse reserve(StockReservationRequest request) {
		Map<String, Integer> quantities = aggregate(request.items());
		return withLocks(quantities.keySet().stream().toList(), () -> transactions.execute(status -> {
			List<StockReservation> existing = reservationRepository.findAllByOrderIdOrderBySku(request.orderId());
			if (!existing.isEmpty()) {
				if (existing.size() != quantities.size()
						|| existing.stream().anyMatch(reservation ->
								!Integer.valueOf(reservation.getQuantity()).equals(quantities.get(reservation.getSku())))) {
					throw new ConflictException("The order already has a different stock reservation");
				}
				if (existing.stream().anyMatch(reservation -> reservation.getStatus() == ReservationStatus.RELEASED)) {
					throw new ConflictException("Released stock reservations cannot be reused");
				}
				return response(request.orderId(), existing.get(0).getStatus());
			}

			List<StockReservation> reservations = new ArrayList<>();
			for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
				InventoryItem item = itemRepository.findBySkuForUpdate(entry.getKey())
						.orElseThrow(() -> new ResourceNotFoundException("Inventory item not found for SKU " + entry.getKey()));
				if (!item.isActive()) {
					throw new ConflictException("Inventory item is inactive for SKU " + entry.getKey());
				}
				item.reserveStock(entry.getValue());
				reservations.add(new StockReservation(request.orderId(), entry.getKey(), entry.getValue()));
			}
			reservationRepository.saveAll(reservations);
			return response(request.orderId(), ReservationStatus.RESERVED);
		}));
	}

	public StockReservationResponse confirm(UUID orderId) {
		List<StockReservation> snapshots = reservationRepository.findAllByOrderIdOrderBySku(orderId);
		if (snapshots.isEmpty()) {
			throw new ResourceNotFoundException("Stock reservation not found");
		}
		return withLocks(snapshots.stream().map(StockReservation::getSku).toList(), () ->
				transactions.execute(status -> {
					List<StockReservation> reservations = reservationRepository.findAllByOrderIdOrderBySku(orderId);
					for (StockReservation reservation : reservations) {
						if (reservation.getStatus() == ReservationStatus.RELEASED) {
							throw new ConflictException("Released stock cannot be confirmed");
						}
						if (reservation.getStatus() == ReservationStatus.RESERVED) {
							InventoryItem item = itemRepository.findBySkuForUpdate(reservation.getSku())
									.orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
							item.confirmReservedStock(reservation.getQuantity());
							reservation.confirm();
						}
					}
					return response(orderId, ReservationStatus.CONFIRMED);
				}));
	}

	public StockReservationResponse release(UUID orderId) {
		List<StockReservation> snapshots = reservationRepository.findAllByOrderIdOrderBySku(orderId);
		if (snapshots.isEmpty()) {
			throw new ResourceNotFoundException("Stock reservation not found");
		}
		return withLocks(snapshots.stream().map(StockReservation::getSku).toList(), () ->
				transactions.execute(status -> {
					List<StockReservation> reservations = reservationRepository.findAllByOrderIdOrderBySku(orderId);
					for (StockReservation reservation : reservations) {
						if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
							throw new ConflictException("Confirmed stock cannot be released");
						}
						if (reservation.getStatus() == ReservationStatus.RESERVED) {
							InventoryItem item = itemRepository.findBySkuForUpdate(reservation.getSku())
									.orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
							item.releaseStock(reservation.getQuantity());
							reservation.release();
						}
					}
					return response(orderId, ReservationStatus.RELEASED);
				}));
	}

	private <T> T withLocks(List<String> skus, java.util.function.Supplier<T> action) {
		List<Lock> acquired = new ArrayList<>();
		try {
			for (String sku : skus.stream().distinct().sorted().toList()) {
				String key = "inventory:lock:sku:" + sku;
				String token = UUID.randomUUID().toString();
				Boolean locked = redis.opsForValue().setIfAbsent(key, token, LOCK_TTL);
				if (!Boolean.TRUE.equals(locked)) {
					throw new ConflictException("Inventory is busy; retry the request");
				}
				acquired.add(new Lock(key, token));
			}
			return action.get();
		} finally {
			for (int index = acquired.size() - 1; index >= 0; index--) {
				Lock lock = acquired.get(index);
				redis.execute(RELEASE_LOCK, List.of(lock.key()), lock.token());
			}
		}
	}

	private static Map<String, Integer> aggregate(List<StockReservationRequest.Item> items) {
		Map<String, Integer> quantities = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (StockReservationRequest.Item item : items) {
			String sku = item.sku().trim().toUpperCase(java.util.Locale.ROOT);
			quantities.merge(sku, item.quantity(), Math::addExact);
		}
		return quantities;
	}

	private static StockReservationResponse response(UUID orderId, ReservationStatus status) {
		return new StockReservationResponse(orderId, status.name());
	}

	private record Lock(String key, String token) {
	}
}
