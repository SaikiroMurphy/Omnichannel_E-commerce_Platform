package dan.edu.billing_service.controller;

import java.util.UUID;

import dan.edu.billing_service.dto.CreateInvoiceRequest;
import dan.edu.billing_service.dto.InvoicePageResponse;
import dan.edu.billing_service.dto.InvoiceResponse;
import dan.edu.billing_service.service.InvoiceService;
import dan.edu.billing_service.utils.mapper.InvoiceMapper;
import jakarta.validation.Valid;
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
@RequestMapping("/api/billing/invoices")
public class InvoiceController {

	private final InvoiceService invoiceService;
	private final InvoiceMapper invoiceMapper;

	public InvoiceController(InvoiceService invoiceService, InvoiceMapper invoiceMapper) {
		this.invoiceService = invoiceService;
		this.invoiceMapper = invoiceMapper;
	}

	@PostMapping
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	public InvoiceResponse create(@Valid @RequestBody CreateInvoiceRequest request) {
		return invoiceService.create(request);
	}

	@GetMapping
	public InvoicePageResponse list(
			@AuthenticationPrincipal Jwt jwt,
			@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
		validatePageSize(pageable);
		if (isAdmin(jwt)) {
			return invoiceMapper.toPageResponse(invoiceService.listAll(pageable));
		}
		return invoiceMapper.toPageResponse(invoiceService.listForCustomer(userId(jwt), pageable));
	}

	@GetMapping("/by-order/{orderId}")
	public InvoiceResponse getByOrder(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID orderId) {
		return invoiceService.getByOrder(orderId, userId(jwt), isAdmin(jwt));
	}

	@GetMapping("/{invoiceId}")
	public InvoiceResponse get(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID invoiceId) {
		return invoiceService.get(invoiceId, userId(jwt), isAdmin(jwt));
	}

	@PostMapping("/{invoiceId}/issue")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public InvoiceResponse issue(@PathVariable UUID invoiceId) {
		return invoiceService.issue(invoiceId);
	}

	@PostMapping("/{invoiceId}/void")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public InvoiceResponse voidInvoice(@PathVariable UUID invoiceId) {
		return invoiceService.voidInvoice(invoiceId);
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
