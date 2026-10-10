package dan.edu.billing_service.dto;

import java.math.BigDecimal;

public record InvoiceLineResponse(String sku, String description, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
}
