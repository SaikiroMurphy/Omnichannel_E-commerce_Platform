package dan.edu.order_service.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import dan.edu.order_service.common.ServiceTokenProvider;
import dan.edu.order_service.dto.InternalPaymentRequest;
import dan.edu.order_service.dto.OrderNotificationRequest;
import dan.edu.order_service.dto.PaymentSnapshot;
import dan.edu.order_service.dto.ProductSnapshot;
import dan.edu.order_service.dto.StockReservationPayload;
import dan.edu.grpc.product.GetProductRequest;
import dan.edu.grpc.product.ProductQueryGrpc;
import dan.edu.grpc.product.ProductReply;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OrderIntegrationClient {

	private final ManagedChannel productChannel;
	private final long productDeadlineMs;
	private final RestClient inventory;
	private final RestClient payments;
	private final RestClient notifications;
	private final ServiceTokenProvider tokenProvider;

	public OrderIntegrationClient(
			ServiceTokenProvider tokenProvider,
			@Value("${order.services.product-grpc-target}") String productGrpcTarget,
			@Value("${order.services.product-grpc-deadline-ms:2000}") long productDeadlineMs,
			@Value("${order.services.inventory-url}") String inventoryUrl,
			@Value("${order.services.payment-url}") String paymentUrl,
			@Value("${order.services.notification-url}") String notificationUrl) {
		this.tokenProvider = tokenProvider;
		this.productChannel = NettyChannelBuilder.forTarget(productGrpcTarget).usePlaintext().build();
		this.productDeadlineMs = productDeadlineMs;
		this.inventory = RestClient.builder().baseUrl(inventoryUrl).build();
		this.payments = RestClient.builder().baseUrl(paymentUrl).build();
		this.notifications = RestClient.builder().baseUrl(notificationUrl).build();
	}

	public ProductSnapshot getProduct(UUID productId) {
		Metadata headers = new Metadata();
		headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER),
				"Bearer " + tokenProvider.createToken());
		try {
			ProductReply reply = ProductQueryGrpc.newBlockingStub(productChannel)
					.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
					.withDeadlineAfter(productDeadlineMs, TimeUnit.MILLISECONDS)
					.getProduct(GetProductRequest.newBuilder().setProductId(productId.toString()).build());
			return new ProductSnapshot(UUID.fromString(reply.getId()), reply.getSku(), reply.getName(),
					new BigDecimal(reply.getPrice()), reply.getCurrency(), reply.getActive());
		} catch (StatusRuntimeException ex) {
			if (ex.getStatus().getCode() == Status.Code.NOT_FOUND) {
				return null;
			}
			throw ex;
		}
	}

	@PreDestroy
	void shutdownChannels() {
		productChannel.shutdown();
	}
	public void reserve(UUID orderId, List<StockReservationPayload.Item> items) {
		inventory.post()
				.uri("/api/inventory/internal/reservations")
				.headers(this::authenticate)
				.contentType(MediaType.APPLICATION_JSON)
				.body(new StockReservationPayload(orderId, items))
				.retrieve()
				.toBodilessEntity();
	}

	public void confirmReservation(UUID orderId) {
		reservationAction(orderId, "confirm");
	}

	public void releaseReservation(UUID orderId) {
		reservationAction(orderId, "release");
	}

	public void cancelPayment(UUID userId, UUID orderId) {
		payments.delete()
				.uri(uriBuilder -> uriBuilder
						.path("/api/payments/internal/orders/{orderId}")
						.queryParam("userId", userId)
						.build(orderId))
				.headers(this::authenticate)
				.retrieve()
				.toBodilessEntity();
	}

	public PaymentSnapshot createPayment(InternalPaymentRequest request) {
		return payments.post()
				.uri("/api/payments/internal")
				.headers(this::authenticate)
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(PaymentSnapshot.class);
	}

	public void enqueueNotification(OrderNotificationRequest request) {
		notifications.post()
				.uri("/api/notifications/internal")
				.headers(this::authenticate)
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.toBodilessEntity();
	}

	private void reservationAction(UUID orderId, String action) {
		inventory.post()
				.uri("/api/inventory/internal/reservations/{orderId}/{action}", orderId, action)
				.headers(this::authenticate)
				.retrieve()
				.toBodilessEntity();
	}

	private void authenticate(HttpHeaders headers) {
		headers.setBearerAuth(tokenProvider.createToken());
	}
}
