package com.teambind.springproject.application.service.product;

import com.teambind.springproject.application.port.in.DeleteProductUseCase;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.domain.shared.ProductId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * 상품 삭제 Application Service.
 * DeleteProductUseCase를 구현합니다.
 */
@Service
@Transactional
public class DeleteProductService implements DeleteProductUseCase {
	
	private static final Logger logger = LoggerFactory.getLogger(DeleteProductService.class);
	
	private final ProductRepository productRepository;
	
	public DeleteProductService(final ProductRepository productRepository) {
		this.productRepository = productRepository;
	}
	
	@Override
	public void delete(final ProductId productId) {
		logger.info("Deleting product: productId={}", productId.getValue());
		
		// 상품 존재 여부 확인
		if (!productRepository.existsById(productId)) {
			throw new NoSuchElementException(
					"Product not found with id: " + productId.getValue());
		}
		
		// 상품 삭제
		productRepository.deleteById(productId);
		
		logger.info("Successfully deleted product: productId={}", productId.getValue());
	}
}
