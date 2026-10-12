package dan.edu.payment_service.controller;

import java.util.UUID;

import dan.edu.payment_service.dto.CreatePaymentRequest;
import dan.edu.payment_service.dto.InternalCreatePaymentRequest;
import dan.edu.payment_service.dto.PaymentResponse;
import dan.edu.payment_service.dto.SimulatePaymentRequest;
import dan.edu.payment_service.service.OrderPaymentEventPublisher;
import dan.edu.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

	private final PaymentService paymentService;
	private final OrderPaymentEventPublisher orderPaymentEventPublisher;

	public PaymentController(
			PaymentService paymentService,
			OrderPaymentEventPublisher orderPaymentEventPublisher) {
		this.paymentService = paymentService;
		this.orderPaymentEventPublisher = orderPaymentEventPublisher;
	}

	@PostMapping("/internal")
	@PreAuthorize("hasAuthority('ROLE_SERVICE')")
	@ResponseStatus(HttpStatus.CREATED)
	public PaymentResponse createInternal(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody InternalCreatePaymentRequest request) {
		requireService(jwt, "order-service");
		return paymentService.create(
				request.userId(),
				new CreatePaymentRequest(
						request.orderId(),
						request.amount(),
						request.currency(),
						request.method(),
						request.idempotencyKey()));
	}

	@DeleteMapping("/internal/orders/{orderId}")
	@PreAuthorize("hasAuthority('ROLE_SERVICE')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancelForOrder(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID orderId,
			@RequestParam UUID userId) {
		requireService(jwt, "order-service");
		paymentService.cancelForOrder(userId, orderId);
	}

	@GetMapping
	public Page<PaymentResponse> list(
			@AuthenticationPrincipal Jwt jwt,
			@PageableDefault(size = 20) Pageable pageable) {
		if (pageable.getPageSize() > 100) {
			throw new IllegalArgumentException("Page size must not exceed 100");
		}
		return paymentService.list(userId(jwt), pageable);
	}

	@GetMapping("/{paymentId}")
	public PaymentResponse get(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID paymentId) {
		return paymentService.get(userId(jwt), paymentId);
	}

	@GetMapping("/by-order/{orderId}")
	public PaymentResponse getByOrder(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID orderId) {
		return paymentService.getByOrder(userId(jwt), orderId);
	}

	@PostMapping("/{paymentId}/simulate")
	public PaymentResponse simulate(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID paymentId,
			@Valid @RequestBody SimulatePaymentRequest request) {
		PaymentResponse payment = paymentService.simulate(userId(jwt), paymentId, request);
			orderPaymentEventPublisher.publish(payment, userId(jwt));
		return payment;
	}

	@PostMapping("/{paymentId}/cancel")
	public PaymentResponse cancel(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID paymentId) {
		PaymentResponse payment = paymentService.cancel(userId(jwt), paymentId);
			orderPaymentEventPublisher.publish(payment, userId(jwt));
		return payment;
	}

	@PostMapping("/{paymentId}/refund")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public PaymentResponse refund(@PathVariable UUID paymentId) {
		return paymentService.refund(paymentId);
	}

	private static UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	private static void requireService(Jwt jwt, String expectedSubject) {
		if (!expectedSubject.equals(jwt.getSubject())) {
			throw new AccessDeniedException("This service token cannot access the internal payment operation");
		}
	}

}
