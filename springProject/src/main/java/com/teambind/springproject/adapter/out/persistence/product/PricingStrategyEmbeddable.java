package com.teambind.springproject.adapter.out.persistence.product;

import com.teambind.springproject.domain.product.pricing.PricingStrategy;
import com.teambind.springproject.domain.product.vo.PricingType;
import com.teambind.springproject.domain.shared.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * PricingStrategy를 JPA Embeddable로 매핑하기 위한 클래스.
 */
@Embeddable
public class PricingStrategyEmbeddable {

  @Enumerated(EnumType.STRING)
  @Column(name = "pricing_type", nullable = false, length = 50)
  private PricingType pricingType;

  @Column(name = "initial_price", nullable = false, precision = 19, scale = 2)
  private BigDecimal initialPrice;

  @Column(name = "additional_price", precision = 19, scale = 2)
  private BigDecimal additionalPrice;

  protected PricingStrategyEmbeddable() {
    // JPA용 기본 생성자
  }

  public PricingStrategyEmbeddable(
      final PricingType pricingType,
      final BigDecimal initialPrice,
      final BigDecimal additionalPrice) {
    this.pricingType = pricingType;
    this.initialPrice = initialPrice;
    this.additionalPrice = additionalPrice;
  }

  /**
   * Domain PricingStrategy를 Embeddable로 변환합니다.
   */
  public static PricingStrategyEmbeddable fromDomain(final PricingStrategy strategy) {
    return new PricingStrategyEmbeddable(
        strategy.getPricingType(),
        strategy.getInitialPrice().getAmount(),
        strategy.getAdditionalPrice() != null
            ? strategy.getAdditionalPrice().getAmount()
            : null
    );
  }

  /**
   * Embeddable을 Domain PricingStrategy로 변환합니다.
   */
  public PricingStrategy toDomain() {
    return switch (pricingType) {
      case INITIAL_PLUS_ADDITIONAL -> PricingStrategy.initialPlusAdditional(
          Money.of(initialPrice),
          Money.of(additionalPrice)
      );
      case ONE_TIME -> PricingStrategy.oneTime(Money.of(initialPrice));
      case SIMPLE_STOCK -> PricingStrategy.simpleStock(Money.of(initialPrice));
    };
  }

  public PricingType getPricingType() {
    return pricingType;
  }

  public BigDecimal getInitialPrice() {
    return initialPrice;
  }

  public BigDecimal getAdditionalPrice() {
    return additionalPrice;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final PricingStrategyEmbeddable that = (PricingStrategyEmbeddable) o;
    return pricingType == that.pricingType
        && Objects.equals(initialPrice, that.initialPrice)
        && Objects.equals(additionalPrice, that.additionalPrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pricingType, initialPrice, additionalPrice);
  }
}
