package dan.edu.order_service.controller;

import java.util.UUID;

import dan.edu.order_service.dto.OrderPageResponse;
import dan.edu.order_service.dto.OrderPaymentEvent;
import dan.edu.order_service.dto.OrderPaymentRequest;
import dan.edu.order_service.dto.OrderRequest;
import dan.edu.order_service.dto.OrderResponse;
import dan.edu.order_service.dto.PaymentSnapshot;
import dan.edu.order_service.service.OrderService;
import dan.edu.order_service.utils.enums.OrderStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping
	public OrderPageResponse list(
			@AuthenticationPrincipal Jwt jwt,
			@PageableDefault(size = 20) Pageable pageable,
			@RequestParam(required = false) OrderStatus status) {
		validatePageSize(pageable);
		var page = status == null
				? orderService.list(userId(jwt), pageable)
				: isAdmin(jwt)
						? orderService.listAllByStatus(status, pageable)
						: orderService.listByStatus(userId(jwt), status, pageable);
		return new OrderPageResponse(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
	}

	@GetMapping("/{orderId}")
	public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
		return orderService.get(userId(jwt), orderId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrderResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrderRequest request) {
		return orderService.create(userId(jwt), request);
	}

	@PutMapping("/{orderId}")
	public OrderResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId, @Valid @RequestBody OrderRequest request) {
		return orderService.update(userId(jwt), orderId, request);
	}

	@PostMapping("/{orderId}/cancel")
	public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
		return orderService.cancel(userId(jwt), orderId);
	}

	@PostMapping("/{orderId}/payments")
	@ResponseStatus(HttpStatus.CREATED)
	public PaymentSnapshot createPayment(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID orderId,
			@Valid @RequestBody OrderPaymentRequest request) {
		return orderService.createPayment(userId(jwt), orderId, request);
	}

	@PostMapping("/internal/payment-events")
	@PreAuthorize("hasAuthority('ROLE_SERVICE')")
	public OrderResponse paymentEvent(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody OrderPaymentEvent event) {
		if (!"payment-service".equals(jwt.getSubject())) {
			throw new AccessDeniedException("Only payment-service can publish payment events");
		}
		return orderService.applyPaymentEvent(event);
	}

	@PatchMapping("/{orderId}/status")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public OrderResponse updateStatus(@PathVariable UUID orderId, @RequestParam OrderStatus status) {
		return switch (status) {
			case SHIPPING -> orderService.markShipping(orderId);
			case COMPLETED -> orderService.complete(orderId);
			case PENDING, CONFIRMED, CANCELLED ->
					throw new IllegalArgumentException("This status cannot be set by an administrator");
		};
	}

	private static void validatePageSize(Pageable pageable) {
		if (pageable.getPageSize() > 100) {
			throw new IllegalArgumentException("Page size must not exceed 100");
		}
	}

	private static UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	private static boolean isAdmin(Jwt jwt) {
		return jwt.getClaimAsStringList("roles") != null
				&& jwt.getClaimAsStringList("roles").contains("ROLE_ADMIN");
	}
}
