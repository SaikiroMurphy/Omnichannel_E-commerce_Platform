package dan.edu.order_service.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import dan.edu.order_service.common.ConflictException;
import dan.edu.order_service.common.InvalidOrderStateException;
import dan.edu.order_service.common.ResourceNotFoundException;
import dan.edu.order_service.dto.InternalPaymentRequest;
import dan.edu.order_service.dto.OrderNotificationRequest;
import dan.edu.order_service.dto.OrderPaymentEvent;
import dan.edu.order_service.dto.OrderPaymentRequest;
import dan.edu.order_service.dto.OrderRequest;
import dan.edu.order_service.dto.OrderResponse;
import dan.edu.order_service.dto.OrderItemRequest;
import dan.edu.order_service.dto.PaymentSnapshot;
import dan.edu.order_service.dto.ProductSnapshot;
import dan.edu.order_service.dto.StockReservationPayload;
import dan.edu.order_service.event.OrderCreatedEvent;
import dan.edu.order_service.event.OrderCreatedEventPublisher;
import dan.edu.order_service.model.Order;
import dan.edu.order_service.repository.OrderRepository;
import dan.edu.order_service.utils.enums.OrderStatus;
import dan.edu.order_service.utils.mapper.OrderMapper;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

	private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
	private final OrderRepository orderRepository;
	private final OrderMapper orderMapper;
	private final OrderIntegrationClient integrations;
	private final OrderCreatedEventPublisher orderCreatedEventPublisher;

	public OrderService(
			OrderRepository orderRepository,
			OrderMapper orderMapper,
			OrderIntegrationClient integrations,
			OrderCreatedEventPublisher orderCreatedEventPublisher) {
		this.orderRepository = orderRepository;
		this.orderMapper = orderMapper;
		this.integrations = integrations;
		this.orderCreatedEventPublisher = orderCreatedEventPublisher;
	}

	@Transactional(readOnly = true)
	public Page<OrderResponse> list(UUID userId, Pageable pageable) {
		return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
				.map(orderMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public Page<OrderResponse> listByStatus(UUID userId, OrderStatus status, Pageable pageable) {
		return orderRepository.findAllByStatusAndUserIdOrderByCreatedAtDesc(status, userId, pageable)
				.map(orderMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public Page<OrderResponse> listAllByStatus(OrderStatus status, Pageable pageable) {
		return orderRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable)
				.map(orderMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public OrderResponse get(UUID userId, UUID orderId) {
		return orderMapper.toResponse(findOwned(orderId, userId));
	}

	@Transactional
	public OrderResponse create(UUID userId, OrderRequest request) {
		validateItems(request);
		Map<UUID, ProductSnapshot> products = resolveProducts(request.items());
		validateCurrency(request.currency(), products.values());
		Order order = orderRepository.save(new Order(userId, request, products));
		try {
			integrations.reserve(order.getId(), reservationItems(order));
			orderCreatedEventPublisher.publish(OrderCreatedEvent.from(order));
			return orderMapper.toResponse(order);
		} catch (RuntimeException exception) {
			compensateReservation(order.getId(), exception);
			orderRepository.deleteById(order.getId());
			throw exception;
		}
	}

	public OrderResponse update(UUID userId, UUID orderId, OrderRequest request) {
		Order order = findOwned(orderId, userId);
		if (order.getStatus() != OrderStatus.PENDING) {
			throw new ConflictException("Only pending orders can be updated");
		}
		throw new ConflictException("Orders with reserved stock cannot be edited; cancel and create a new order");
	}

	@Transactional
	public OrderResponse cancel(UUID userId, UUID orderId) {
		Order order = findOwnedForUpdate(orderId, userId);
		order.cancel();
		integrations.cancelPayment(userId, orderId);
		integrations.releaseReservation(orderId);
		Order saved = orderRepository.save(order);
		integrations.enqueueNotification(notification(
				saved, "Order cancelled", "Your order has been cancelled.", "ORDER_CANCELLED"));
		return orderMapper.toResponse(saved);
	}

	@Transactional
	public PaymentSnapshot createPayment(UUID userId, UUID orderId, OrderPaymentRequest request) {
		Order order = findOwnedForUpdate(orderId, userId);
		if (order.getStatus() != OrderStatus.PENDING) {
			throw new ConflictException("Payment can only be started for a pending order");
		}
		return integrations.createPayment(new InternalPaymentRequest(
				userId,
				orderId,
				order.getTotalAmount(),
				order.getCurrency(),
				request.method().trim().toUpperCase(Locale.ROOT),
				request.idempotencyKey()));
	}

	@Transactional
	public OrderResponse applyPaymentEvent(OrderPaymentEvent event) {
		Order order = orderRepository.findByIdForUpdate(event.orderId())
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));
		if (!order.getUserId().equals(event.userId())) {
			throw new ResourceNotFoundException("Order not found");
		}
		if (order.getTotalAmount().compareTo(event.amount()) != 0
				|| !order.getCurrency().equalsIgnoreCase(event.currency())) {
			throw new ConflictException("Payment amount or currency does not match the order");
		}
		if ("SUCCEEDED".equals(event.status())) {
			if (order.getStatus() == OrderStatus.CONFIRMED) {
				integrations.confirmReservation(order.getId());
				integrations.enqueueNotification(notification(
						order, "Order confirmed", "Payment was successful and your order is confirmed.", "ORDER_CONFIRMED"));
				return orderMapper.toResponse(order);
			}
			if (order.getStatus() != OrderStatus.PENDING) {
				throw new InvalidOrderStateException("Payment cannot confirm an order in its current state");
			}
			integrations.confirmReservation(order.getId());
			order.confirm();
			Order saved = orderRepository.save(order);
			integrations.enqueueNotification(notification(
					saved, "Order confirmed", "Payment was successful and your order is confirmed.", "ORDER_CONFIRMED"));
			return orderMapper.toResponse(saved);
		}
		if ("FAILED".equals(event.status())) {
			if (order.getStatus() == OrderStatus.CANCELLED) {
				integrations.releaseReservation(order.getId());
				integrations.enqueueNotification(notification(
						order, "Payment failed", "Payment failed; the reserved stock has been released.", "ORDER_CANCELLED"));
				return orderMapper.toResponse(order);
			}
			order.cancel();
			Order saved = orderRepository.save(order);
			integrations.releaseReservation(order.getId());
			integrations.enqueueNotification(notification(
					saved, "Payment failed", "Payment failed; the reserved stock has been released.", "ORDER_CANCELLED"));
			return orderMapper.toResponse(saved);
		}
		if ("CANCELLED".equals(event.status())) {
			if (order.getStatus() == OrderStatus.CANCELLED) {
				integrations.releaseReservation(order.getId());
				return orderMapper.toResponse(order);
			}
			order.cancel();
			integrations.releaseReservation(order.getId());
			Order saved = orderRepository.save(order);
			integrations.enqueueNotification(notification(
					saved, "Payment cancelled", "The payment was cancelled and reserved stock was released.", "ORDER_CANCELLED"));
			return orderMapper.toResponse(saved);
		}
		throw new ConflictException("Unsupported payment status");
	}

	@Transactional
	public OrderResponse markShipping(UUID orderId) {
		Order order = findById(orderId);
		order.markShipping();
		Order saved = orderRepository.save(order);
		integrations.enqueueNotification(notification(
				saved, "Order shipping", "Your order is on its way.", "ORDER_SHIPPING"));
		return orderMapper.toResponse(saved);
	}

	@Transactional
	public OrderResponse complete(UUID orderId) {
		Order order = findById(orderId);
		order.complete();
		Order saved = orderRepository.save(order);
		integrations.enqueueNotification(notification(
				saved, "Order completed", "Your order has been completed.", "ORDER_COMPLETED"));
		return orderMapper.toResponse(saved);
	}

	private Map<UUID, ProductSnapshot> resolveProducts(List<OrderItemRequest> items) {
		Map<UUID, ProductSnapshot> products = new LinkedHashMap<>();
		for (OrderItemRequest item : items) {
			ProductSnapshot product = integrations.getProduct(item.productId());
			if (product == null || !product.active()) {
				throw new ResourceNotFoundException("Active product not found: " + item.productId());
			}
			products.put(item.productId(), product);
		}
		return products;
	}

	private static void validateItems(OrderRequest request) {
		if (request == null || request.items() == null || request.items().isEmpty()) {
			throw new IllegalArgumentException("Order must include at least one item");
		}
		if (request.items().stream().anyMatch(item -> item.quantity() == null || item.quantity() <= 0)) {
			throw new IllegalArgumentException("Each item quantity must be greater than zero");
		}
	}

	private static void validateCurrency(String requestedCurrency, java.util.Collection<ProductSnapshot> products) {
		String currency = products.iterator().next().currency();
		if (products.stream().anyMatch(product -> !currency.equalsIgnoreCase(product.currency()))) {
			throw new ConflictException("All products in an order must use the same currency");
		}
		if (requestedCurrency != null && !requestedCurrency.isBlank()
				&& !requestedCurrency.equalsIgnoreCase(currency)) {
			throw new ConflictException("Order currency must match the catalog product currency");
		}
	}

	private static List<StockReservationPayload.Item> reservationItems(Order order) {
		return order.getItems().stream()
				.map(item -> new StockReservationPayload.Item(item.getSku(), item.getQuantity()))
				.toList();
	}

	private static OrderNotificationRequest notification(Order order, String title, String message, String eventType) {
		return new OrderNotificationRequest(
				order.getUserId(), title, message, "EMAIL", eventType, order.getId().toString());
	}

	private void compensateReservation(UUID orderId, RuntimeException original) {
		try {
			integrations.releaseReservation(orderId);
		} catch (RuntimeException compensationFailure) {
			original.addSuppressed(compensationFailure);
			logger.error("Failed to release inventory reservation for order {}", orderId, compensationFailure);
		}
	}

	private Order findOwned(UUID orderId, UUID userId) {
		return orderRepository.findByIdAndUserId(orderId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));
	}

	private Order findOwnedForUpdate(UUID orderId, UUID userId) {
		return orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));
	}

	private Order findById(UUID orderId) {
		return orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));
	}
}
