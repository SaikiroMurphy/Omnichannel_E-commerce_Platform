package dan.edu.payment_service.utils.mapper;

import dan.edu.payment_service.dto.PaymentResponse;
import dan.edu.payment_service.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentMapper {

	@Mapping(target = "sandbox", constant = "true")
	PaymentResponse toResponse(Payment payment);
}
