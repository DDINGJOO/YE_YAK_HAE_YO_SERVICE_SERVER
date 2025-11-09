package com.teambind.springproject.application.service.product;

import com.teambind.springproject.application.dto.request.UpdateProductRequest;
import com.teambind.springproject.application.dto.response.ProductResponse;
import com.teambind.springproject.application.port.in.UpdateProductUseCase;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.shared.ProductId;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 수정 Application Service.
 * UpdateProductUseCase를 구현합니다.
 */
@Service
@Transactional
public class UpdateProductService implements UpdateProductUseCase {

  private static final Logger logger = LoggerFactory.getLogger(UpdateProductService.class);

  private final ProductRepository productRepository;

  public UpdateProductService(final ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  @Override
  public ProductResponse update(final ProductId productId, final UpdateProductRequest request) {
    logger.info("Updating product: productId={}", productId.getValue());

    // 기존 상품 조회
    final Product product = productRepository.findById(productId)
        .orElseThrow(() -> new NoSuchElementException(
            "Product not found with id: " + productId.getValue()));

    // 상품 정보 업데이트
    product.updateName(request.name());
    product.updatePricingStrategy(request.pricingStrategy().toDomain());
    product.updateTotalQuantity(request.totalQuantity());

    // 변경사항 저장
    final Product updatedProduct = productRepository.save(product);

    logger.info("Successfully updated product: productId={}", productId.getValue());

    return ProductResponse.from(updatedProduct);
  }
}
