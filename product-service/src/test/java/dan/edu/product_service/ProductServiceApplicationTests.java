package dan.edu.product_service;

import java.util.UUID;

import dan.edu.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:product-service-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"product.jwt.issuer=http://user-service:8080",
		"product.jwt.audience=quickbite-api",
		"product.jwt.secret-base64=dXNlci1zZXJ2aWNlLXRlc3Qtc2VjcmV0LWtleS0zMi1ieXRlcyE="
})
@AutoConfigureMockMvc
class ProductServiceApplicationTests {

	private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void clearProducts() {
		productRepository.deleteAll();
	}

	@Test
	void allowsPublicCatalogAndProductDetailWithoutAuthentication() throws Exception {
		String productId = createProduct("TEA-001", "Green Tea");

		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("Green Tea"))
				.andExpect(jsonPath("$.content[0].sku").value("TEA-001"));

		mockMvc.perform(get("/api/products/{productId}", productId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(productId));
	}

	@Test
	void requiresAdminForProductMutations() throws Exception {
		mockMvc.perform(post("/api/products")
						.with(jwt().jwt(token -> token.subject(USER_ID.toString()))
								.authorities(() -> "ROLE_CUSTOMER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(productJson("TEA-002", "Tea")))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content(productJson("TEA-002", "Tea")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createsUpdatesAndSoftDeletesProducts() throws Exception {
		String productId = createProductAsAdmin("TEA-003", "Original Tea");

		mockMvc.perform(put("/api/products/{productId}", productId)
						.with(adminJwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content(productJson("TEA-003", "Updated Tea")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Tea"))
				.andExpect(jsonPath("$.price").value(199.50));

		mockMvc.perform(delete("/api/products/{productId}", productId)
						.with(adminJwt()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/products/{productId}", productId))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));

		mockMvc.perform(post("/api/products/{productId}/activate", productId)
						.with(adminJwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void searchesByNameAndCategoryAndPaginates() throws Exception {
		createProduct("COFFEE-001", "Arabica Coffee");
		createProduct("COFFEE-002", "Robusta Coffee");
		createProduct("TEA-004", "Green Tea");

		mockMvc.perform(get("/api/products")
						.param("q", "coffee")
						.param("category", "Beverages")
						.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void rejectsDuplicateSkuAndInvalidProductFields() throws Exception {
		createProductAsAdmin("TEA-005", "Tea");

		mockMvc.perform(post("/api/products")
						.with(adminJwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content(productJson("tea-005", "Duplicate")))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/products")
						.with(adminJwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku":"BAD SKU","name":"","category":"Beverages","price":-1,"currency":"VND"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void limitsPageSizeToOneHundred() throws Exception {
		mockMvc.perform(get("/api/products").param("size", "101"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void exposesHealthEndpoint() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	private String createProduct(String sku, String name) throws Exception {
		return createProduct(sku, name, adminJwt());
	}

	private String createProductAsAdmin(String sku, String name) throws Exception {
		return createProduct(sku, name, adminJwt());
	}

	private String createProduct(String sku, String name,
			org.springframework.test.web.servlet.request.RequestPostProcessor principal) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/products")
						.with(principal)
						.contentType(MediaType.APPLICATION_JSON)
						.content(productJson(sku, name)))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return response.path("id").asText();
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
		return jwt().jwt(token -> token.subject(ADMIN_ID.toString()))
				.authorities(() -> "ROLE_ADMIN");
	}

	private static String productJson(String sku, String name) {
		return """
				{
				  "sku":"%s",
				  "name":"%s",
				  "category":"Beverages",
				  "description":"Quality product",
				  "price":199.50,
				  "currency":"vnd",
				  "imageUrl":"https://example.test/product.png"
				}
				""".formatted(sku, name);
	}
}
