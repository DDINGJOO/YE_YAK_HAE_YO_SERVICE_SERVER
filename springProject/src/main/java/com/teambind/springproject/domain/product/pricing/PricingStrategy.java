package com.teambind.springproject.domain.product.pricing;

import com.teambind.springproject.domain.product.vo.PricingType;
import com.teambind.springproject.domain.shared.Money;
import java.util.Objects;

/**
 * 상품의 가격 전략을 나타내는 Value Object.
 */
public class PricingStrategy {

  private final PricingType pricingType;
  private final Money initialPrice;
  private final Money additionalPrice;

  private PricingStrategy(
      final PricingType pricingType,
      final Money initialPrice,
      final Money additionalPrice) {
    validatePricingStrategy(pricingType, initialPrice, additionalPrice);
    this.pricingType = pricingType;
    this.initialPrice = initialPrice;
    this.additionalPrice = additionalPrice;
  }

  /**
   * 초기 + 추가 요금 방식의 가격 전략 생성.
   *
   * @param initialPrice     초기 가격
   * @param additionalPrice  추가 가격
   * @return PricingStrategy
   */
  public static PricingStrategy initialPlusAdditional(
      final Money initialPrice,
      final Money additionalPrice) {
    if (initialPrice == null || additionalPrice == null) {
      throw new IllegalArgumentException(
          "Initial price and additional price cannot be null for INITIAL_PLUS_ADDITIONAL type");
    }
    return new PricingStrategy(PricingType.INITIAL_PLUS_ADDITIONAL, initialPrice, additionalPrice);
  }

  /**
   * 1회 대여료 방식의 가격 전략 생성.
   *
   * @param oneTimePrice 1회 대여 가격
   * @return PricingStrategy
   */
  public static PricingStrategy oneTime(final Money oneTimePrice) {
    if (oneTimePrice == null) {
      throw new IllegalArgumentException("One-time price cannot be null for ONE_TIME type");
    }
    return new PricingStrategy(PricingType.ONE_TIME, oneTimePrice, null);
  }

  /**
   * 단순 재고 관리 방식의 가격 전략 생성.
   *
   * @param unitPrice 단가
   * @return PricingStrategy
   */
  public static PricingStrategy simpleStock(final Money unitPrice) {
    if (unitPrice == null) {
      throw new IllegalArgumentException("Unit price cannot be null for SIMPLE_STOCK type");
    }
    return new PricingStrategy(PricingType.SIMPLE_STOCK, unitPrice, null);
  }

  private void validatePricingStrategy(
      final PricingType pricingType,
      final Money initialPrice,
      final Money additionalPrice) {
    if (pricingType == null) {
      throw new IllegalArgumentException("Pricing type cannot be null");
    }
    if (initialPrice == null) {
      throw new IllegalArgumentException("Initial price cannot be null");
    }

    switch (pricingType) {
      case INITIAL_PLUS_ADDITIONAL -> {
        if (additionalPrice == null) {
          throw new IllegalArgumentException(
              "Additional price is required for INITIAL_PLUS_ADDITIONAL type");
        }
      }
      case ONE_TIME, SIMPLE_STOCK -> {
        if (additionalPrice != null) {
          throw new IllegalArgumentException(
              "Additional price must be null for " + pricingType + " type");
        }
      }
    }
  }

  /**
   * 수량에 따른 가격을 계산합니다.
   *
   * @param quantity 수량
   * @return 계산된 가격
   */
  public Money calculate(final int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be positive: " + quantity);
    }

    return switch (pricingType) {
      case INITIAL_PLUS_ADDITIONAL -> {
        // 초기 가격 + (수량 - 1) * 추가 가격
        if (quantity == 1) {
          yield initialPrice;
        }
        final Money additionalCost = additionalPrice.multiply(quantity - 1);
        yield initialPrice.add(additionalCost);
      }
      case ONE_TIME, SIMPLE_STOCK ->
          // 수량과 무관하게 초기 가격 또는 단가 반환
          initialPrice;
    };
  }

  public PricingType getPricingType() {
    return pricingType;
  }

  public Money getInitialPrice() {
    return initialPrice;
  }

  public Money getAdditionalPrice() {
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
    final PricingStrategy that = (PricingStrategy) o;
    return pricingType == that.pricingType
        && Objects.equals(initialPrice, that.initialPrice)
        && Objects.equals(additionalPrice, that.additionalPrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pricingType, initialPrice, additionalPrice);
  }

  @Override
  public String toString() {
    return "PricingStrategy{"
        + "pricingType=" + pricingType
        + ", initialPrice=" + initialPrice
        + ", additionalPrice=" + additionalPrice
        + '}';
  }
}
