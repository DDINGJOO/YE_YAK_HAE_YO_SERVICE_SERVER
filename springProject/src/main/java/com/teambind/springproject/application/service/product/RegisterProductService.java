package com.teambind.springproject.application.service.product;

import com.teambind.springproject.application.dto.request.RegisterProductRequest;
import com.teambind.springproject.application.dto.response.ProductResponse;
import com.teambind.springproject.application.port.in.RegisterProductUseCase;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.domain.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 등록 Application Service.
 * RegisterProductUseCase를 구현합니다.
 */
@Service
@Transactional
public class RegisterProductService implements RegisterProductUseCase {
	
	private static final Logger logger = LoggerFactory.getLogger(RegisterProductService.class);
	
	private final ProductRepository productRepository;
	
	public RegisterProductService(final ProductRepository productRepository) {
		this.productRepository = productRepository;
	}
	
	@Override
	public ProductResponse register(final RegisterProductRequest request) {
		logger.info("Registering product: scope={}, name={}", request.scope(), request.name());
		
		// DTO를 도메인 객체로 변환
		final Product product = request.toDomain();
		
		// Repository에 저장
		final Product savedProduct = productRepository.save(product);
		
		logger.info("Successfully registered product: productId={}, scope={}",
				savedProduct.getProductId().getValue(), savedProduct.getScope());
		
		// 도메인 객체를 DTO로 변환하여 반환
		return ProductResponse.from(savedProduct);
	}
}
