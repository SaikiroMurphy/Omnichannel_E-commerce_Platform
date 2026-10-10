package dan.edu.billing_service;

import java.util.UUID;

import dan.edu.billing_service.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:billing-service-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"billing.jwt.issuer=http://user-service:8080",
		"billing.jwt.audience=quickbite-api",
		"billing.jwt.secret-base64=dXNlci1zZXJ2aWNlLXRlc3Qtc2VjcmV0LWtleS0zMi1ieXRlcyE="
})
@AutoConfigureMockMvc
class BillingServiceApplicationTests {

	private static final UUID CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
	private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");
	private static final UUID ORDER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private InvoiceRepository invoiceRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void clearInvoices() {
		invoiceRepository.deleteAll();
	}

	@Test
	void requiresAuthenticationForInvoiceEndpoints() throws Exception {
		mockMvc.perform(get("/api/billing/invoices"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void adminCreatesInvoiceWithServerCalculatedSubtotalTaxAndTotal() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/billing/invoices")
						.with(adminJwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content(createRequest(ORDER_ID)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.currency").value("VND"))
				.andExpect(jsonPath("$.subtotal").value(251.00))
				.andExpect(jsonPath("$.taxAmount").value(25.10))
				.andExpect(jsonPath("$.total").value(276.10))
				.andExpect(jsonPath("$.lines[0].lineTotal").value(251.00))
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		org.assertj.core.api.Assertions.assertThat(response.path("invoiceNumber").asText()).startsWith("INV-");
	}

	@Test
	void onlyAdminCanCreateIssueOrVoidInvoices() throws Exception {
		mockMvc.perform(post("/api/billing/invoices")
						.with(customerJwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content(createRequest(ORDER_ID)))
				.andExpect(status().isForbidden());

		String invoiceId = createInvoice(ORDER_ID);
		mockMvc.perform(post("/api/billing/invoices/{invoiceId}/issue", invoiceId)
						.with(customerJwt()))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/billing/invoices/{invoiceId}/issue", invoiceId)
						.with(adminJwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ISSUED"));
		mockMvc.perform(post("/api/billing/invoices/{invoiceId}/void", invoiceId)
						.with(adminJwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("VOID"));
	}

	@Test
	void customersCanReadOnlyTheirOwnInvoicesAndFilterByOrder() throws Exception {
		String invoiceId = createInvoice(ORDER_ID);

		mockMvc.perform(get("/api/billing/invoices/{invoiceId}", invoiceId)
						.with(customerJwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()));

		mockMvc.perform(get("/api/billing/invoices/{invoiceId}", invoiceId)
						.with(otherCustomerJwt()))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/billing/invoices/by-order/{orderId}", ORDER_ID)
						.with(customerJwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(invoiceId));
	}

	@Test
	void adminCanListAllInvoicesAndCustomerSeesOnlyTheirOwn() throws Exception {
		createInvoice(ORDER_ID);
		createInvoice(UUID.fromString("20000000-0000-0000-0000-000000000002"), OTHER_CUSTOMER_ID);

		mockMvc.perform(get("/api/billing/invoices").with(customerJwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));
		mockMvc.perform(get("/api/billing/invoices").with(adminJwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void preventsDuplicateInvoicesForOneOrderAndCapsPagination() throws Exception {
		createInvoice(ORDER_ID);

		mockMvc.perform(post("/api/billing/invoices")
						.with(adminJwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content(createRequest(ORDER_ID)))
				.andExpect(status().isConflict());

		mockMvc.perform(get("/api/billing/invoices?size=101").with(customerJwt()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsInvalidInvoiceAndLineInput() throws Exception {
		mockMvc.perform(post("/api/billing/invoices")
						.with(adminJwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "orderId":"%s",
								  "customerId":"%s",
								  "currency":"VND",
								  "lines":[{"sku":"BAD SKU","description":"","quantity":0,"unitPrice":-10}]
								}
								""".formatted(ORDER_ID, CUSTOMER_ID)))
				.andExpect(status().isBadRequest());
	}

	private String createInvoice(UUID orderId) throws Exception {
		return createInvoice(orderId, CUSTOMER_ID);
	}

	private String createInvoice(UUID orderId, UUID customerId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/billing/invoices")
						.with(adminJwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content(createRequest(orderId, customerId)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString())
				.path("id").asText();
	}

	private static String createRequest(UUID orderId) {
		return createRequest(orderId, CUSTOMER_ID);
	}

	private static String createRequest(UUID orderId, UUID customerId) {
		return """
				{
				  "orderId":"%s",
				  "customerId":"%s",
				  "currency":"vnd",
				  "taxRate":10.00,
				  "lines":[
				    {"sku":"TEA-001","description":"Green tea","quantity":2,"unitPrice":125.50}
				  ]
				}
				""".formatted(orderId, customerId);
	}

	private static RequestPostProcessor adminJwt() {
		return jwt().jwt(token -> token.subject(ADMIN_ID.toString())
						.claim("roles", java.util.List.of("ROLE_ADMIN")))
				.authorities(() -> "ROLE_ADMIN");
	}

	private static RequestPostProcessor customerJwt() {
		return jwt().jwt(token -> token.subject(CUSTOMER_ID.toString())
						.claim("roles", java.util.List.of("CUSTOMER")))
				.authorities(() -> "CUSTOMER");
	}

	private static RequestPostProcessor otherCustomerJwt() {
		return jwt().jwt(token -> token.subject(OTHER_CUSTOMER_ID.toString())
						.claim("roles", java.util.List.of("CUSTOMER")))
				.authorities(() -> "CUSTOMER");
	}
}
