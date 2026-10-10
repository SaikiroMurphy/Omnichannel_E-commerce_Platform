package dan.edu.billing_service.dto;

import java.util.List;

public record InvoicePageResponse(
		List<InvoiceResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last) {
}
