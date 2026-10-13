package dan.edu.product_service.grpc;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

@Component
public class ServiceJwtServerInterceptor implements ServerInterceptor {

	private static final Metadata.Key<String> AUTHORIZATION =
			Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
	private static final String SERVICE_ROLE = "ROLE_SERVICE";

	private final JwtDecoder jwtDecoder;

	public ServiceJwtServerInterceptor(JwtDecoder productJwtDecoder) {
		this.jwtDecoder = productJwtDecoder;
	}

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
			ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
		String header = headers.get(AUTHORIZATION);
		if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
			return reject(call, Status.UNAUTHENTICATED.withDescription("Missing bearer token"));
		}
		try {
			Jwt jwt = jwtDecoder.decode(header.substring(7).trim());
			List<String> roles = jwt.getClaimAsStringList("roles");
			if (roles == null || !roles.contains(SERVICE_ROLE)) {
				return reject(call, Status.PERMISSION_DENIED.withDescription("Service role required"));
			}
		} catch (RuntimeException ex) {
			return reject(call, Status.UNAUTHENTICATED.withDescription("Invalid token"));
		}
		return Contexts.interceptCall(Context.current(), call, headers, next);
	}

	private static <ReqT, RespT> ServerCall.Listener<ReqT> reject(ServerCall<ReqT, RespT> call, Status status) {
		call.close(status, new Metadata());
		return new ServerCall.Listener<>() {
		};
	}
}
