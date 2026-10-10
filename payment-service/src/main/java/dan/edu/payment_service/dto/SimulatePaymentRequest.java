package dan.edu.payment_service.dto;

import dan.edu.payment_service.utils.enums.PaymentOutcome;
import jakarta.validation.constraints.NotNull;

public record SimulatePaymentRequest(@NotNull PaymentOutcome outcome) {
}
