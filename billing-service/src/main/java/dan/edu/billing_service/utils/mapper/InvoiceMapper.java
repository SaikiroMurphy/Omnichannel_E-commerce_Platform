package dan.edu.billing_service.utils.mapper;

import dan.edu.billing_service.dto.InvoiceLineResponse;
import dan.edu.billing_service.dto.InvoicePageResponse;
import dan.edu.billing_service.dto.InvoiceResponse;
import dan.edu.billing_service.model.Invoice;
import dan.edu.billing_service.model.InvoiceLine;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InvoiceMapper {

	InvoiceResponse toResponse(Invoice invoice);

	InvoiceLineResponse toResponse(InvoiceLine line);

	default InvoicePageResponse toPageResponse(Page<InvoiceResponse> invoices) {
		return new InvoicePageResponse(
				invoices.getContent(),
				invoices.getNumber(),
				invoices.getSize(),
				invoices.getTotalElements(),
				invoices.getTotalPages(),
				invoices.isFirst(),
				invoices.isLast());
	}
}
