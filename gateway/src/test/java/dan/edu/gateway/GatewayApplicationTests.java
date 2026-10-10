package dan.edu.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"gateway.rate-limit.enabled=false",
				"management.health.redis.enabled=false"
		})
class GatewayApplicationTests {

	private static final String TEST_JWT_SECRET = "gateway-test-secret-with-at-least-32-bytes";
	private static HttpServer upstream;

	@Autowired
	private RouteLocator routeLocator;

	@LocalServerPort
	private int gatewayPort;

	@DynamicPropertySource
	static void configureUpstream(DynamicPropertyRegistry registry) throws IOException {
		upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		upstream.createContext("/", exchange -> {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			byte[] response = (exchange.getRequestMethod() + " " + exchange.getRequestURI() + " " + body)
					.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, response.length);
			try (var responseBody = exchange.getResponseBody()) {
				responseBody.write(response);
			}
		});
		upstream.start();
		String upstreamUrl = "http://127.0.0.1:" + upstream.getAddress().getPort();
		registry.add("USER_SERVICE_URL", () -> upstreamUrl);
		registry.add("PRODUCT_SERVICE_URL", () -> upstreamUrl);
		registry.add("ORDER_SERVICE_URL", () -> upstreamUrl);
		registry.add("INVENTORY_SERVICE_URL", () -> upstreamUrl);
		registry.add("PAYMENT_SERVICE_URL", () -> upstreamUrl);
		registry.add("NOTIFICATION_SERVICE_URL", () -> upstreamUrl);
		registry.add("BILLING_SERVICE_URL", () -> upstreamUrl);
		registry.add("JWT_SECRET", () -> TEST_JWT_SECRET);
		registry.add("JWT_SECRET_BASE64", () -> Base64.getEncoder().encodeToString(
				TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
		registry.add("JWT_ISSUER_URI", () -> "http://user-service:8080");
		registry.add("JWT_AUDIENCE", () -> "quickbite-api");
	}

	@Test
	void contextLoads() {
	}

	@Test
	void forwardsRequestWithoutChangingPathQueryOrBody() throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create(
						"http://127.0.0.1:" + gatewayPort + "/api/users/42?include=roles"))
				.header("Content-Type", "text/plain")
				.header("Authorization", "Bearer " + createToken())
				.POST(HttpRequest.BodyPublishers.ofString("payload"))
				.build();

		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).isEqualTo("POST /api/users/42?include=roles payload");
	}

	@Test
	void forwardsAllPublicApiPrefixesToTheirConfiguredUpstreams() throws Exception {
		Map<String, String> paths = Map.of(
				"/api/users", "/api/users",
				"/api/auth/login", "/api/auth/login",
				"/api/products/1", "/api/products/1",
				"/api/orders/1", "/api/orders/1",
				"/api/inventory/items", "/api/inventory/items",
				"/api/payments/1", "/api/payments/1",
				"/api/billing/invoices/1", "/api/billing/invoices/1",
				"/api/notifications/1", "/api/notifications/1");
		HttpClient client = HttpClient.newHttpClient();

		for (Map.Entry<String, String> entry : paths.entrySet()) {
			HttpRequest request = HttpRequest.newBuilder(URI.create(
							"http://127.0.0.1:" + gatewayPort + entry.getKey()))
					.header("Authorization", "Bearer " + createToken())
					.GET()
					.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			assertThat(response.statusCode()).isEqualTo(200);
			assertThat(response.body()).isEqualTo("GET " + entry.getValue() + " ");
		}
	}

	@Test
	void allowsLoginWithoutToken() throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(
						"http://127.0.0.1:" + gatewayPort + "/api/auth/login"))
				.POST(HttpRequest.BodyPublishers.ofString("{}"))
				.build();

		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).isEqualTo("POST /api/auth/login {}");
	}

	@Test
	void allowsPublicProductCatalogWithoutToken() throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(
						"http://127.0.0.1:" + gatewayPort + "/api/products?q=tea"))
				.GET()
				.build();

		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).isEqualTo("GET /api/products?q=tea ");
	}

	@Test
	void rejectsProtectedRequestsWithoutToken() throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(
						"http://127.0.0.1:" + gatewayPort + "/api/users/42"))
				.GET()
				.build();

		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(401);
	}

	@Test
	void rejectsInvalidBearerTokens() throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(
						"http://127.0.0.1:" + gatewayPort + "/api/users/42"))
				.header("Authorization", "Bearer not-a-valid-jwt")
				.GET()
				.build();

		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(401);
	}

	@Test
	void exposesHealthEndpoint() throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(
						"http://127.0.0.1:" + gatewayPort + "/actuator/health"))
				.GET()
				.build();

		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"status\":\"UP\"");
	}

	@Test
	void registersRoutesForAllBackendServices() {
		List<String> routeIds = routeLocator.getRoutes()
				.map(Route::getId)
				.collectList()
				.block();

		assertThat(routeIds).containsExactlyInAnyOrder(
				"user-service",
				"product-service",
				"order-service",
				"inventory-service",
				"payment-service",
				"billing-service",
				"notification-service");
	}

	@AfterAll
	static void stopUpstream() {
		if (upstream != null) {
			upstream.stop(0);
		}
	}

	private static String createToken() throws Exception {
		Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
		String header = encoder.encodeToString(
				"{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
		long now = Instant.now().getEpochSecond();
		String claims = encoder.encodeToString(("{\"iss\":\"http://user-service:8080\","
				+ "\"aud\":[\"quickbite-api\"],\"sub\":\"gateway-test-user\",\"iat\":"
				+ now + ",\"exp\":" + (now + 300) + "}").getBytes(StandardCharsets.UTF_8));
		String unsignedToken = header + "." + claims;
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		String signature = encoder.encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
		return unsignedToken + "." + signature;
	}
}
