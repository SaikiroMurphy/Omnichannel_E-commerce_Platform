package dan.edu.payment_service.service;

import java.util.UUID;

import dan.edu.payment_service.common.ServiceTokenProvider;
import dan.edu.payment_service.dto.PaymentOrderEvent;
import dan.edu.payment_service.dto.PaymentResponse;
import dan.edu.payment_service.utils.enums.PaymentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OrderPaymentEventPublisher {

	private final ServiceTokenProvider tokenProvider;
	private final RestClient orders;

	public OrderPaymentEventPublisher(
			ServiceTokenProvider tokenProvider,
			@Value("${payment.services.order-url:http://localhost:8083}") String orderUrl) {
		this.tokenProvider = tokenProvider;
		this.orders = RestClient.builder().baseUrl(orderUrl).build();
	}

	public void publish(PaymentResponse payment, UUID userId) {
		if (payment.status() != PaymentStatus.SUCCEEDED
				&& payment.status() != PaymentStatus.FAILED
				&& payment.status() != PaymentStatus.CANCELLED) {
			return;
		}
		orders.post()
				.uri("/api/orders/internal/payment-events")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.createToken())
				.contentType(MediaType.APPLICATION_JSON)
				.body(new PaymentOrderEvent(
						userId,
						payment.orderId(),
						payment.amount(),
						payment.currency(),
						payment.status().name()))
				.retrieve()
				.toBodilessEntity();
	}
}
