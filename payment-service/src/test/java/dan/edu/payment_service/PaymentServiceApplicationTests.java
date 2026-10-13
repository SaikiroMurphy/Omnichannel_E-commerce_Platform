package dan.edu.payment_service;

import java.util.UUID;

import dan.edu.payment_service.repository.PaymentRepository;
import dan.edu.payment_service.event.OutboxEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:payment-service-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"payment.jwt.issuer=http://user-service:8080",
		"payment.jwt.audience=quickbite-api",
		"payment.jwt.secret-base64=dXNlci1zZXJ2aWNlLXRlc3Qtc2VjcmV0LWtleS0zMi1ieXRlcyE=",
		"payment.mock.enabled=true",
		"payment.kafka.payment-result-topic=payment.result",
		"payment.outbox.poll-interval-ms=3600000"
})
@AutoConfigureMockMvc
class PaymentServiceApplicationTests {

	private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
	private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");
	private static final UUID ORDER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private OutboxEventRepository outboxRepository;

	@BeforeEach
	void clearPayments() {
		paymentRepository.deleteAll();
		outboxRepository.deleteAll();
	}

	@Test
	void requiresAuthenticationForPaymentEndpoints() throws Exception {
		mockMvc.perform(get("/api/payments"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createsPendingPaymentAndMakesCreateIdempotent() throws Exception {
		String body = createRequest(ORDER_ID, "125000.00", "request-1");

		MvcResult first = mockMvc.perform(post("/api/payments/internal")
						.with(orderServiceJwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.currency").value("VND"))
				.andExpect(jsonPath("$.sandbox").value(true))
				.andReturn();

		String firstId = responseId(first);
		mockMvc.perform(post("/api/payments/internal")
						.with(orderServiceJwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(firstId));

		assertThat(paymentRepository.count()).isEqualTo(1);
	}

	@Test
	void rejectsReusingAnIdempotencyKeyForDifferentPaymentData() throws Exception {
		mockMvc.perform(post("/api/payments/internal")
						.with(orderServiceJwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content(createRequest(ORDER_ID, "125000.00", "request-2")))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/payments/internal")
						.with(orderServiceJwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content(createRequest(ORDER_ID, "125001.00", "request-2")))
				.andExpect(status().isConflict());
	}

	@Test
	void simulatesApprovalAndAllowsOnlyAdminRefund() throws Exception {
		String paymentId = createPayment("request-3");

		mockMvc.perform(post("/api/payments/{paymentId}/simulate", paymentId)
						.with(jwt().jwt(token -> token.subject(USER_ID.toString())))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"outcome\":\"APPROVED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUCCEEDED"));
		assertThat(outboxRepository.findAll()).singleElement().satisfies(event -> {
			assertThat(event.getTopic()).isEqualTo("payment.result");
			assertThat(event.getPayload()).contains("SUCCEEDED");
		});

		mockMvc.perform(post("/api/payments/{paymentId}/refund", paymentId)
						.with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/payments/{paymentId}/refund", paymentId)
						.with(jwt().jwt(token -> token.subject(ADMIN_ID.toString()))
								.authorities(() -> "ROLE_ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REFUNDED"));
	}

	@Test
	void usersCanOnlyReadTheirOwnPayments() throws Exception {
		String paymentId = createPayment("request-4");

		mockMvc.perform(get("/api/payments/{paymentId}", paymentId)
						.with(jwt().jwt(token -> token.subject(OTHER_USER_ID.toString()))))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/payments/by-order/{orderId}", ORDER_ID)
						.with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(paymentId));
	}

	@Test
	void cancellationIsIdempotentAndTerminalPaymentsCannotBeSimulated() throws Exception {
		String paymentId = createPayment("request-5");

		mockMvc.perform(post("/api/payments/{paymentId}/cancel", paymentId)
						.with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
		mockMvc.perform(post("/api/payments/{paymentId}/cancel", paymentId)
						.with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));

		mockMvc.perform(post("/api/payments/{paymentId}/simulate", paymentId)
						.with(jwt().jwt(token -> token.subject(USER_ID.toString())))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"outcome\":\"APPROVED\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void validatesPaymentAmountAndCapsPageSize() throws Exception {
		mockMvc.perform(post("/api/payments/internal")
						.with(orderServiceJwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content(createRequest(ORDER_ID, "0.00", "request-6")))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/payments")
						.with(jwt().jwt(token -> token.subject(USER_ID.toString())))
						.contentType(MediaType.APPLICATION_JSON)
						.content(createRequest(ORDER_ID, "125000.00", "request-7")))
				.andExpect(status().isMethodNotAllowed());

		mockMvc.perform(get("/api/payments?size=101")
						.with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
				.andExpect(status().isBadRequest());
	}

	private String createPayment(String idempotencyKey) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/payments/internal")
						.with(orderServiceJwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content(createRequest(ORDER_ID, "125000.00", idempotencyKey)))
				.andExpect(status().isCreated())
				.andReturn();
		return responseId(result);
	}

	private String responseId(MvcResult result) throws Exception {
		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return response.path("id").asText();
	}

	private static String createRequest(UUID orderId, String amount, String idempotencyKey) {
		return """
				{"userId":"%s","orderId":"%s","amount":%s,"currency":"vnd","method":"CARD","idempotencyKey":"%s"}
				""".formatted(USER_ID, orderId, amount, idempotencyKey);
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor orderServiceJwt() {
		return jwt().jwt(token -> token.subject("order-service"))
				.authorities(() -> "ROLE_SERVICE");
	}
}
