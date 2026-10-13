package dan.edu.product_service.grpc;

import java.util.UUID;

import dan.edu.grpc.product.GetProductRequest;
import dan.edu.grpc.product.ProductQueryGrpc;
import dan.edu.grpc.product.ProductReply;
import dan.edu.product_service.common.ResourceNotFoundException;
import dan.edu.product_service.dto.ProductResponse;
import dan.edu.product_service.service.ProductService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

@Component
public class ProductGrpcService extends ProductQueryGrpc.ProductQueryImplBase {

	private final ProductService productService;

	public ProductGrpcService(ProductService productService) {
		this.productService = productService;
	}

	@Override
	public void getProduct(GetProductRequest request, StreamObserver<ProductReply> responseObserver) {
		UUID productId;
		try {
			productId = UUID.fromString(request.getProductId());
		} catch (IllegalArgumentException ex) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid product id").asRuntimeException());
			return;
		}
		try {
			ProductResponse product = productService.get(productId);
			responseObserver.onNext(ProductReply.newBuilder()
					.setId(product.id().toString())
					.setSku(product.sku())
					.setName(product.name())
					.setPrice(product.price().toPlainString())
					.setCurrency(product.currency())
					.setActive(product.active())
					.build());
			responseObserver.onCompleted();
		} catch (ResourceNotFoundException ex) {
			responseObserver.onError(Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException());
		} catch (RuntimeException ex) {
			responseObserver.onError(Status.INTERNAL.withDescription("Product lookup failed").asRuntimeException());
		}
	}
}
