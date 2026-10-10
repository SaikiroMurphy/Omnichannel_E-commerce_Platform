package dan.edu.gateway.ratelimit;

import java.net.InetSocketAddress;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayRateLimitConfiguration {

	@Bean
	RedisRateLimiter redisRateLimiter(
			@Value("${gateway.rate-limit.replenish-rate:20}") int replenishRate,
			@Value("${gateway.rate-limit.burst-capacity:40}") int burstCapacity) {
		return new RedisRateLimiter(replenishRate, burstCapacity);
	}

	@Bean
	KeyResolver clientKeyResolver() {
		return exchange -> exchange.getPrincipal()
				.map(Principal::getName)
				.filter(StringUtils::hasText)
				.map(userId -> "user:" + userId)
				.switchIfEmpty(Mono.fromSupplier(() -> {
					InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
					if (remoteAddress == null) {
						return "ip:unknown";
					}
					String address = remoteAddress.getAddress() == null
							? remoteAddress.getHostString()
							: remoteAddress.getAddress().getHostAddress();
					return "ip:" + address;
				}));
	}
}
