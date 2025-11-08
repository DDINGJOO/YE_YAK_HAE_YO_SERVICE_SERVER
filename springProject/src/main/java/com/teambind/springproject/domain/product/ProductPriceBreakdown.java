package com.teambind.springproject.domain.product;

import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.ProductId;

/**
 * 상품 가격 내역을 나타내는 Value Object (Record).
 * 예약 가격 스냅샷에 포함되는 불변 객체입니다.
 *
 * @param productId 상품 ID
 * @param productName 상품명
 * @param quantity 수량
 * @param unitPrice 단가
 * @param totalPrice 총 가격
 * @param pricingType 가격 책정 방식
 */
public record ProductPriceBreakdown(
    ProductId productId,
    String productName,
    int quantity,
    Money unitPrice,
    Money totalPrice,
    PricingType pricingType
) {

  /**
   * Compact Constructor - 불변식 검증.
   */
  public ProductPriceBreakdown {
    if (productId == null) {
      throw new IllegalArgumentException("Product ID cannot be null");
    }
    if (productName == null || productName.trim().isEmpty()) {
      throw new IllegalArgumentException("Product name cannot be null or empty");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be positive: " + quantity);
    }
    if (unitPrice == null) {
      throw new IllegalArgumentException("Unit price cannot be null");
    }
    if (totalPrice == null) {
      throw new IllegalArgumentException("Total price cannot be null");
    }
    if (pricingType == null) {
      throw new IllegalArgumentException("Pricing type cannot be null");
    }

    // 가격 일관성 검증
    final Money expectedTotalPrice = unitPrice.multiply(quantity);
    if (!totalPrice.equals(expectedTotalPrice)) {
      throw new IllegalArgumentException(
          "Total price mismatch: expected " + expectedTotalPrice + " but got " + totalPrice);
    }
  }
}
