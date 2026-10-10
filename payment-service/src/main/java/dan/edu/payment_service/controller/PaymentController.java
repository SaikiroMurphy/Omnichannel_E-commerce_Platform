package dan.edu.payment_service.controller;

import java.util.UUID;

import dan.edu.payment_service.dto.CreatePaymentRequest;
import dan.edu.payment_service.dto.PaymentResponse;
import dan.edu.payment_service.dto.SimulatePaymentRequest;
import dan.edu.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PaymentResponse create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreatePaymentRequest request) {
		return paymentService.create(userId(jwt), request);
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
		return paymentService.simulate(userId(jwt), paymentId, request);
	}

	@PostMapping("/{paymentId}/cancel")
	public PaymentResponse cancel(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID paymentId) {
		return paymentService.cancel(userId(jwt), paymentId);
	}

	@PostMapping("/{paymentId}/refund")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public PaymentResponse refund(@PathVariable UUID paymentId) {
		return paymentService.refund(paymentId);
	}

	private static UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
