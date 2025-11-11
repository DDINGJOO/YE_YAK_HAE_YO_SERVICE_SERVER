package com.teambind.springproject.application.dto.request;

import com.teambind.springproject.domain.product.pricing.PricingStrategy;
import com.teambind.springproject.domain.product.vo.PricingType;
import com.teambind.springproject.domain.shared.Money;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 가격 전략 DTO.
 * PricingStrategy 도메인 객체와 DTO 간 변환을 담당합니다.
 */
public record PricingStrategyDto(
    @NotNull(message = "Pricing type is required")
    PricingType pricingType,

    @NotNull(message = "Initial price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Initial price must be greater than or equal to 0")
    BigDecimal initialPrice,

    @DecimalMin(value = "0.0", inclusive = true, message = "Additional price must be greater than or equal to 0")
    BigDecimal additionalPrice
) {

  /**
   * DTO를 PricingStrategy 도메인 객체로 변환합니다.
   *
   * @return PricingStrategy
   * @throws IllegalArgumentException PricingType에 따라 additionalPrice가 필수이거나 null이어야 할 때
   */
  public PricingStrategy toDomain() {
    return switch (pricingType) {
      case INITIAL_PLUS_ADDITIONAL -> {
        if (additionalPrice == null) {
          throw new IllegalArgumentException(
              "Additional price is required for INITIAL_PLUS_ADDITIONAL pricing type");
        }
        yield PricingStrategy.initialPlusAdditional(
            Money.of(initialPrice),
            Money.of(additionalPrice)
        );
      }
      case ONE_TIME -> {
        if (additionalPrice != null) {
          throw new IllegalArgumentException(
              "Additional price must be null for ONE_TIME pricing type");
        }
        yield PricingStrategy.oneTime(Money.of(initialPrice));
      }
      case SIMPLE_STOCK -> {
        if (additionalPrice != null) {
          throw new IllegalArgumentException(
              "Additional price must be null for SIMPLE_STOCK pricing type");
        }
        yield PricingStrategy.simpleStock(Money.of(initialPrice));
      }
    };
  }

  /**
   * PricingStrategy 도메인 객체로부터 DTO를 생성합니다.
   *
   * @param strategy PricingStrategy 도메인 객체
   * @return PricingStrategyDto
   */
  public static PricingStrategyDto from(final PricingStrategy strategy) {
    final BigDecimal additionalPrice = strategy.getAdditionalPrice() != null
        ? strategy.getAdditionalPrice().getAmount()
        : null;

    return new PricingStrategyDto(
        strategy.getPricingType(),
        strategy.getInitialPrice().getAmount(),
        additionalPrice
    );
  }
}
