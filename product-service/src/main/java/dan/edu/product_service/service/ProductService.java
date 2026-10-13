package dan.edu.product_service.service;

import java.util.Locale;
import java.util.UUID;

import dan.edu.product_service.common.ConflictException;
import dan.edu.product_service.common.ResourceNotFoundException;
import dan.edu.product_service.dto.ProductRequest;
import dan.edu.product_service.dto.ProductResponse;
import dan.edu.product_service.event.ProductCatalogEvent;
import dan.edu.product_service.event.ProductCatalogEventPublisher;
import dan.edu.product_service.model.Product;
import dan.edu.product_service.repository.ProductRepository;
import dan.edu.product_service.utils.mapper.ProductMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

	private final ProductRepository productRepository;
	private final ProductMapper productMapper;
	private final ProductCatalogEventPublisher catalogEventPublisher;

	public ProductService(
			ProductRepository productRepository,
			ProductMapper productMapper,
			ProductCatalogEventPublisher catalogEventPublisher) {
		this.productRepository = productRepository;
		this.productMapper = productMapper;
		this.catalogEventPublisher = catalogEventPublisher;
	}

	@Transactional(readOnly = true)
	public Page<ProductResponse> search(String query, String category, Pageable pageable) {
		return productRepository.searchActive(normalize(query), normalize(category), pageable)
				.map(productMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public ProductResponse get(UUID productId) {
		return productMapper.toResponse(productRepository.findByIdAndActiveTrue(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found")));
	}

	@Transactional
	public ProductResponse create(ProductRequest request) {
		String sku = normalizeSku(request.sku());
		if (productRepository.existsBySkuIgnoreCase(sku)) {
			throw new ConflictException("A product with this SKU already exists");
		}
		Product product = productRepository.save(new Product(request));
		catalogEventPublisher.publishAfterCommit(ProductCatalogEvent.created(product));
		return productMapper.toResponse(product);
	}

	@Transactional
	public ProductResponse update(UUID productId, ProductRequest request) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
		String sku = normalizeSku(request.sku());
		if (productRepository.existsBySkuIgnoreCaseAndIdNot(sku, productId)) {
			throw new ConflictException("A product with this SKU already exists");
		}
		String previousSku = product.getSku();
		product.update(request);
		catalogEventPublisher.publishAfterCommit(ProductCatalogEvent.updated(product, previousSku));
		return productMapper.toResponse(product);
	}

	@Transactional
	public void deactivate(UUID productId) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
		product.deactivate();
		catalogEventPublisher.publishAfterCommit(ProductCatalogEvent.deactivated(product));
	}

	@Transactional
	public ProductResponse activate(UUID productId) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
		product.activate();
		catalogEventPublisher.publishAfterCommit(ProductCatalogEvent.activated(product));
		return productMapper.toResponse(product);
	}

	private static String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static String normalizeSku(String sku) {
		return sku.trim().toUpperCase(Locale.ROOT);
	}
}
