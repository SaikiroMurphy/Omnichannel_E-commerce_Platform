package dan.edu.billing_service.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import dan.edu.billing_service.dto.InvoiceLineRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class InvoiceLine {

	@Column(name = "item_sku", nullable = false, length = 64)
	private String sku;

	@Column(name = "item_description", nullable = false, length = 200)
	private String description;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "line_total", nullable = false, precision = 19, scale = 2)
	private BigDecimal lineTotal;

	protected InvoiceLine() {
	}

	public InvoiceLine(InvoiceLineRequest request) {
		sku = request.sku().trim().toUpperCase(java.util.Locale.ROOT);
		description = request.description().trim();
		quantity = request.quantity();
		unitPrice = request.unitPrice().setScale(2, RoundingMode.HALF_UP);
		lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
	}

	public String getSku() {
		return sku;
	}

	public String getDescription() {
		return description;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public BigDecimal getLineTotal() {
		return lineTotal;
	}
}
