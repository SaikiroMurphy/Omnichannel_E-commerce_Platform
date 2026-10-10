package dan.edu.gateway.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(
		prefix = "gateway.rate-limit",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true)
public class GatewayRateLimitFilter implements GlobalFilter, Ordered {

	private static final String RATE_LIMITER_ROUTE_ID = "api-gateway";

	private final RedisRateLimiter rateLimiter;
	private final KeyResolver keyResolver;

	public GatewayRateLimitFilter(RedisRateLimiter rateLimiter, KeyResolver keyResolver) {
		this.rateLimiter = rateLimiter;
		this.keyResolver = keyResolver;
	}

	@Override
	public Mono<Void> filter(
			org.springframework.web.server.ServerWebExchange exchange,
			org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
		return keyResolver.resolve(exchange)
				.flatMap(key -> rateLimiter.isAllowed(RATE_LIMITER_ROUTE_ID, key))
				.flatMap(response -> {
					response.getHeaders().forEach((name, value) ->
							exchange.getResponse().getHeaders().set(name, value));
					if (response.isAllowed()) {
						return chain.filter(exchange);
					}
					exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
					return exchange.getResponse().setComplete();
				});
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
