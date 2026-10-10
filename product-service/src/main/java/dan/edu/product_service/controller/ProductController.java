package dan.edu.product_service.controller;

import java.util.UUID;

import dan.edu.product_service.dto.ProductPageResponse;
import dan.edu.product_service.dto.ProductRequest;
import dan.edu.product_service.dto.ProductResponse;
import dan.edu.product_service.service.ProductService;
import dan.edu.product_service.utils.mapper.ProductMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;
	private final ProductMapper productMapper;

	public ProductController(ProductService productService, ProductMapper productMapper) {
		this.productService = productService;
		this.productMapper = productMapper;
	}

	@GetMapping
	public ProductPageResponse search(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String category,
			@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		validatePageSize(pageable);
		return productMapper.toPageResponse(productService.search(q, category, pageable));
	}

	@GetMapping("/{productId}")
	public ProductResponse get(@PathVariable UUID productId) {
		return productService.get(productId);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	public ProductResponse create(@Valid @RequestBody ProductRequest request) {
		return productService.create(request);
	}

	@PutMapping("/{productId}")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public ProductResponse update(
			@PathVariable UUID productId,
			@Valid @RequestBody ProductRequest request) {
		return productService.update(productId, request);
	}

	@DeleteMapping("/{productId}")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivate(@PathVariable UUID productId) {
		productService.deactivate(productId);
	}

	@PostMapping("/{productId}/activate")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public ProductResponse activate(@PathVariable UUID productId) {
		return productService.activate(productId);
	}

	private static void validatePageSize(Pageable pageable) {
		if (pageable.getPageSize() > 100) {
			throw new IllegalArgumentException("Page size must not exceed 100");
		}
	}
}
