package dan.edu.user_service;

import java.util.UUID;

import dan.edu.user_service.model.UserAccount;
import dan.edu.user_service.repository.UserRepository;
import dan.edu.user_service.utils.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:user-service-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"user-service.jwt.issuer=http://user-service:8080",
		"user-service.jwt.audience=quickbite-api",
		"user-service.jwt.secret-base64=dXNlci1zZXJ2aWNlLXRlc3Qtc2VjcmV0LWtleS0zMi1ieXRlcyE=",
		"user-service.bootstrap-admin.email=",
		"user-service.bootstrap-admin.password="
})
@AutoConfigureMockMvc
class UserServiceApplicationTests {

	private static final String PASSWORD = "Correct-Horse-Battery-42!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void mapsShippingAddressAsColumnsOnTheUserRow() {
		var userColumns = jdbcTemplate.queryForList(
				"select column_name from information_schema.columns where table_name = 'USERS'",
				String.class);
		var addressTables = jdbcTemplate.queryForList(
				"select table_name from information_schema.tables "
						+ "where table_schema = 'PUBLIC' and table_name like '%ADDRESS%'",
				String.class);

		assertThat(userColumns).contains("ADDRESS_LINE1", "ADDRESS_CITY", "ADDRESS_COUNTRY");
		assertThat(addressTables).doesNotContain("SHIPPING_ADDRESSES", "USER_ADDRESSES");
	}

	@Test
	void registersAccountIssuesJwtAndAuthenticatesLogin() throws Exception {
		String email = uniqueEmail();

		MvcResult result = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s","firstName":"Ada","lastName":"Lovelace","phone":"+84901234567"}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").isNumber())
				.andExpect(jsonPath("$.user.email").value(email))
				.andExpect(jsonPath("$.user.roles[0]").value("CUSTOMER"))
				.andExpect(jsonPath("$.user.passwordHash").doesNotExist())
				.andReturn();

		JsonNode registration = objectMapper.readTree(result.getResponse().getContentAsString());
		String accessToken = registration.path("accessToken").asText();
		String refreshToken = registration.path("refreshToken").asText();
		assertThat(accessToken.split("\\.")).hasSize(3);
		assertThat(refreshToken).isNotBlank();

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(email.toUpperCase(), PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());

		mockMvc.perform(get("/api/users/me")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.firstName").value("Ada"));
	}

	@Test
	void rotatesRefreshTokensAndRejectsReuseOfThePreviousToken() throws Exception {
		String email = uniqueEmail();
		String originalRefreshToken = register(email);

		MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								java.util.Map.of("refreshToken", originalRefreshToken))))
				.andExpect(status().isOk())
				.andReturn();
		String nextRefreshToken = objectMapper.readTree(refreshed.getResponse().getContentAsString())
				.path("refreshToken").asText();
		assertThat(nextRefreshToken).isNotEqualTo(originalRefreshToken);

		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								java.util.Map.of("refreshToken", originalRefreshToken))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void changingPasswordRevokesRefreshSessionsAndRequiresTheNewPassword() throws Exception {
		String email = uniqueEmail();
		String refreshToken = register(email);
		String accessToken = loginToken(email);

		mockMvc.perform(put("/api/users/me/password")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"currentPassword":"%s","newPassword":"A-New-Strong-Password-53!"}
								""".formatted(PASSWORD)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								java.util.Map.of("refreshToken", refreshToken))))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"A-New-Strong-Password-53!"}
								""".formatted(email)))
				.andExpect(status().isOk());
	}

	@Test
	void storesOneAddressOnEachUserAndBlocksCustomerFromAdminApis() throws Exception {
		String ownerEmail = uniqueEmail();
		String otherEmail = uniqueEmail();
		String ownerToken = loginToken(ownerEmail);
		String otherToken = loginToken(otherEmail);

		mockMvc.perform(put("/api/users/me/address")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(addressJson("Home")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.label").value("Home"))
				.andExpect(jsonPath("$.city").value("Ho Chi Minh City"));

		mockMvc.perform(get("/api/users/me/address")
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.label").value("Home"));
		mockMvc.perform(get("/api/users/me")
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.address.label").value("Home"));

		mockMvc.perform(get("/api/users/me/address")
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound());

		mockMvc.perform(put("/api/users/me/address")
						.header("Authorization", "Bearer " + otherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(addressJson("Office")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/users/me/address")
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.label").value("Home"));

		mockMvc.perform(get("/api/users")
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete("/api/users/me/address")
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/users/me/address")
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isNotFound());

		mockMvc.perform(patch("/api/users/me")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"firstName":"Grace","lastName":"Hopper","phone":"+84901234567"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Grace"));
	}

	@Test
	void adminRoleCanListAndUpdateUserRoles() throws Exception {
		String customerEmail = uniqueEmail();
		loginToken(customerEmail);
		UserAccount admin = new UserAccount(
				uniqueEmail(), passwordEncoder.encode(PASSWORD), "System", "Admin", null);
		admin.getRoles().add(Role.ADMIN);
		admin = userRepository.save(admin);
		String adminToken = loginToken(admin.getEmail());

		mockMvc.perform(get("/api/users")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.totalElements").isNumber());

		UserAccount customer = userRepository.findByEmail(customerEmail).orElseThrow();
		mockMvc.perform(put("/api/users/{userId}/roles", customer.getId())
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"roles":["STAFF","CUSTOMER"]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roles").isArray());
	}

	@Test
	void validatesRegistrationAndDoesNotRevealWhetherAnEmailExistsOnLogin() throws Exception {
		String email = uniqueEmail();
		register(email);

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s","firstName":"A","lastName":"B"}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"wrong-password"}
								""".formatted(email)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Invalid credentials or refresh token"));
	}

	private String register(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s","firstName":"Test","lastName":"User"}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString())
				.path("refreshToken").asText();
	}

	private String loginToken(String email) throws Exception {
		if (!userRepository.existsByEmail(email)) {
			register(email);
		}
		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString())
				.path("accessToken").asText();
	}

	private static String addressJson(String label) {
		return """
				{
				  "label":"%s",
				  "recipientName":"Test User",
				  "phone":"+84901234567",
				  "addressLine1":"1 Test Street",
				  "ward":"Ward 1",
				  "district":"District 1",
				  "city":"Ho Chi Minh City",
				  "country":"Vietnam",
				  "postalCode":"700000"
				}
				""".formatted(label);
	}

	private static String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.test";
	}
}
