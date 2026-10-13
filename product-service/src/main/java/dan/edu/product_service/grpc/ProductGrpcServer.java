package dan.edu.product_service.grpc;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class ProductGrpcServer implements SmartLifecycle {

	private final Server server;
	private volatile boolean running;

	public ProductGrpcServer(
			ProductGrpcService service,
			ServiceJwtServerInterceptor authInterceptor,
			@Value("${product.grpc.port:9090}") int port) {
		this.server = NettyServerBuilder.forPort(port)
				.addService(ServerInterceptors.intercept(service, authInterceptor))
				.build();
	}

	@Override
	public void start() {
		try {
			server.start();
			running = true;
		} catch (IOException ex) {
			throw new IllegalStateException("Cannot start gRPC server", ex);
		}
	}

	@Override
	public void stop() {
		server.shutdown();
		try {
			if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
				server.shutdownNow();
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			server.shutdownNow();
		}
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}
}
