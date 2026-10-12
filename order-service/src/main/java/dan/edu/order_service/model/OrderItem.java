package dan.edu.order_service.model;

import java.math.BigDecimal;
import java.util.UUID;

import dan.edu.order_service.dto.OrderItemRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "product_name", nullable = false, length = 255)
	private String productName;

	@Column(nullable = false, length = 64)
	private String sku;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal subtotal;

	protected OrderItem() {
	}

	public OrderItem(Order order, OrderItemRequest request, dan.edu.order_service.dto.ProductSnapshot product) {
		this.order = order;
		this.productId = request.productId();
		this.productName = product.name();
		this.sku = product.sku();
		this.unitPrice = product.price();
		this.quantity = request.quantity();
		this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	public UUID getId() { return id; }
	public Order getOrder() { return order; }
	public UUID getProductId() { return productId; }
	public String getProductName() { return productName; }
	public String getSku() { return sku; }
	public BigDecimal getUnitPrice() { return unitPrice; }
	public int getQuantity() { return quantity; }
	public BigDecimal getSubtotal() { return subtotal; }
}
