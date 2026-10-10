package dan.edu.product_service.service;

import java.util.Locale;
import java.util.UUID;

import dan.edu.product_service.common.ConflictException;
import dan.edu.product_service.common.ResourceNotFoundException;
import dan.edu.product_service.dto.ProductRequest;
import dan.edu.product_service.dto.ProductResponse;
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

	public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
		this.productRepository = productRepository;
		this.productMapper = productMapper;
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
		return productMapper.toResponse(productRepository.save(new Product(request)));
	}

	@Transactional
	public ProductResponse update(UUID productId, ProductRequest request) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
		String sku = normalizeSku(request.sku());
		if (productRepository.existsBySkuIgnoreCaseAndIdNot(sku, productId)) {
			throw new ConflictException("A product with this SKU already exists");
		}
		product.update(request);
		return productMapper.toResponse(product);
	}

	@Transactional
	public void deactivate(UUID productId) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
		product.deactivate();
	}

	@Transactional
	public ProductResponse activate(UUID productId) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
		product.activate();
		return productMapper.toResponse(product);
	}

	private static String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static String normalizeSku(String sku) {
		return sku.trim().toUpperCase(Locale.ROOT);
	}
}
