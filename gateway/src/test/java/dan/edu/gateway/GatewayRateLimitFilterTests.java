package dan.edu.gateway;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import dan.edu.gateway.ratelimit.GatewayRateLimitFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayRateLimitFilterTests {

	@Test
	void rejectsRequestsWhenTheTokenBucketIsEmpty() {
		RedisRateLimiter rateLimiter = mock(RedisRateLimiter.class);
		KeyResolver keyResolver = exchange -> Mono.just("ip:127.0.0.1");
		when(rateLimiter.isAllowed("api-gateway", "ip:127.0.0.1"))
				.thenReturn(Mono.just(new RateLimiter.Response(false, Map.of("X-RateLimit-Remaining", "0"))));
		GatewayRateLimitFilter filter = new GatewayRateLimitFilter(rateLimiter, keyResolver);
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/products").build());
		AtomicBoolean forwarded = new AtomicBoolean();
		GatewayFilterChain chain = ignored -> {
			forwarded.set(true);
			return Mono.empty();
		};

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
		assertThat(forwarded).isFalse();
		verify(rateLimiter).isAllowed("api-gateway", "ip:127.0.0.1");
	}

	@Test
	void forwardsRequestsAllowedByTheTokenBucket() {
		RedisRateLimiter rateLimiter = mock(RedisRateLimiter.class);
		KeyResolver keyResolver = exchange -> Mono.just("user:alice");
		when(rateLimiter.isAllowed("api-gateway", "user:alice"))
				.thenReturn(Mono.just(new RateLimiter.Response(true, Map.of())));
		GatewayRateLimitFilter filter = new GatewayRateLimitFilter(rateLimiter, keyResolver);
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/products").build());
		AtomicBoolean forwarded = new AtomicBoolean();

		StepVerifier.create(filter.filter(exchange, ignored -> {
			forwarded.set(true);
			return Mono.empty();
		})).verifyComplete();

		assertThat(exchange.getResponse().getStatusCode()).isNull();
		assertThat(forwarded).isTrue();
		verify(rateLimiter).isAllowed("api-gateway", "user:alice");
	}
}
