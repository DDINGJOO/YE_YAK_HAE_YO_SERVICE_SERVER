package com.teambind.springproject.adapter.out.persistence.reservationpricing;

import com.teambind.springproject.domain.product.PricingType;
import com.teambind.springproject.domain.product.ProductPriceBreakdown;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.ProductId;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;

/**
 * ProductPriceBreakdown을 JPA Embeddable로 매핑.
 * @ElementCollection과 함께 사용됩니다.
 */
@Embeddable
public class ProductPriceBreakdownEmbeddable {

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "product_name", nullable = false, length = 255)
  private String productName;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalPrice;

  @Enumerated(EnumType.STRING)
  @Column(name = "pricing_type", nullable = false, length = 30)
  private PricingType pricingType;

  protected ProductPriceBreakdownEmbeddable() {
    // JPA용 기본 생성자
  }

  public ProductPriceBreakdownEmbeddable(
      final Long productId,
      final String productName,
      final Integer quantity,
      final BigDecimal unitPrice,
      final BigDecimal totalPrice,
      final PricingType pricingType) {
    this.productId = productId;
    this.productName = productName;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.totalPrice = totalPrice;
    this.pricingType = pricingType;
  }

  /**
   * Domain ProductPriceBreakdown을 Embeddable로 변환합니다.
   */
  public static ProductPriceBreakdownEmbeddable fromDomain(
      final ProductPriceBreakdown breakdown) {
    return new ProductPriceBreakdownEmbeddable(
        breakdown.productId().getValue(),
        breakdown.productName(),
        breakdown.quantity(),
        breakdown.unitPrice().getAmount(),
        breakdown.totalPrice().getAmount(),
        breakdown.pricingType()
    );
  }

  /**
   * Embeddable을 Domain ProductPriceBreakdown으로 변환합니다.
   */
  public ProductPriceBreakdown toDomain() {
    return new ProductPriceBreakdown(
        ProductId.of(productId),
        productName,
        quantity,
        Money.of(unitPrice),
        Money.of(totalPrice),
        pricingType
    );
  }

  // Getters

  public Long getProductId() {
    return productId;
  }

  public String getProductName() {
    return productName;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public BigDecimal getTotalPrice() {
    return totalPrice;
  }

  public PricingType getPricingType() {
    return pricingType;
  }
}
