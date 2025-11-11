package com.teambind.springproject.domain.product;

import com.teambind.springproject.domain.product.pricing.PricingStrategy;
import com.teambind.springproject.domain.product.vo.PricingType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teambind.springproject.domain.shared.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PricingStrategy Value Object 테스트")
class PricingStrategyTest {

  @Nested
  @DisplayName("Factory Method 테스트")
  class FactoryMethodTests {

    @Test
    @DisplayName("initialPlusAdditional 팩토리 메서드 성공")
    void createInitialPlusAdditional() {
      // given
      final Money initialPrice = Money.of(10000);
      final Money additionalPrice = Money.of(5000);

      // when
      final PricingStrategy strategy = PricingStrategy.initialPlusAdditional(
          initialPrice, additionalPrice);

      // then
      assertThat(strategy.getPricingType()).isEqualTo(PricingType.INITIAL_PLUS_ADDITIONAL);
      assertThat(strategy.getInitialPrice()).isEqualTo(initialPrice);
      assertThat(strategy.getAdditionalPrice()).isEqualTo(additionalPrice);
    }

    @Test
    @DisplayName("initialPlusAdditional에서 initialPrice가 null이면 예외 발생")
    void throwExceptionWhenInitialPriceIsNullForInitialPlusAdditional() {
      // when & then
      assertThatThrownBy(() -> PricingStrategy.initialPlusAdditional(null, Money.of(5000)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Initial price and additional price cannot be null for INITIAL_PLUS_ADDITIONAL type");
    }

    @Test
    @DisplayName("initialPlusAdditional에서 additionalPrice가 null이면 예외 발생")
    void throwExceptionWhenAdditionalPriceIsNullForInitialPlusAdditional() {
      // when & then
      assertThatThrownBy(() -> PricingStrategy.initialPlusAdditional(Money.of(10000), null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Initial price and additional price cannot be null for INITIAL_PLUS_ADDITIONAL type");
    }

    @Test
    @DisplayName("oneTime 팩토리 메서드 성공")
    void createOneTime() {
      // given
      final Money oneTimePrice = Money.of(15000);

      // when
      final PricingStrategy strategy = PricingStrategy.oneTime(oneTimePrice);

      // then
      assertThat(strategy.getPricingType()).isEqualTo(PricingType.ONE_TIME);
      assertThat(strategy.getInitialPrice()).isEqualTo(oneTimePrice);
      assertThat(strategy.getAdditionalPrice()).isNull();
    }

    @Test
    @DisplayName("oneTime에서 price가 null이면 예외 발생")
    void throwExceptionWhenPriceIsNullForOneTime() {
      // when & then
      assertThatThrownBy(() -> PricingStrategy.oneTime(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("One-time price cannot be null for ONE_TIME type");
    }

    @Test
    @DisplayName("simpleStock 팩토리 메서드 성공")
    void createSimpleStock() {
      // given
      final Money unitPrice = Money.of(2000);

      // when
      final PricingStrategy strategy = PricingStrategy.simpleStock(unitPrice);

      // then
      assertThat(strategy.getPricingType()).isEqualTo(PricingType.SIMPLE_STOCK);
      assertThat(strategy.getInitialPrice()).isEqualTo(unitPrice);
      assertThat(strategy.getAdditionalPrice()).isNull();
    }

    @Test
    @DisplayName("simpleStock에서 price가 null이면 예외 발생")
    void throwExceptionWhenPriceIsNullForSimpleStock() {
      // when & then
      assertThatThrownBy(() -> PricingStrategy.simpleStock(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Unit price cannot be null for SIMPLE_STOCK type");
    }
  }

  @Nested
  @DisplayName("가격 계산 테스트 - INITIAL_PLUS_ADDITIONAL")
  class CalculateInitialPlusAdditionalTests {

    @Test
    @DisplayName("수량 1개일 때 초기 가격만 반환")
    void calculateWithQuantityOne() {
      // given
      final PricingStrategy strategy = PricingStrategy.initialPlusAdditional(
          Money.of(10000), Money.of(5000));

      // when
      final Money result = strategy.calculate(1);

      // then
      assertThat(result).isEqualTo(Money.of(10000));
    }

    @Test
    @DisplayName("수량 2개일 때 초기 가격 + 추가 가격")
    void calculateWithQuantityTwo() {
      // given
      final PricingStrategy strategy = PricingStrategy.initialPlusAdditional(
          Money.of(10000), Money.of(5000));

      // when
      final Money result = strategy.calculate(2);

      // then
      // 10,000 + (2-1) * 5,000 = 15,000
      assertThat(result).isEqualTo(Money.of(15000));
    }

    @Test
    @DisplayName("수량 5개일 때 초기 가격 + 추가 가격 * 4")
    void calculateWithQuantityFive() {
      // given
      final PricingStrategy strategy = PricingStrategy.initialPlusAdditional(
          Money.of(10000), Money.of(5000));

      // when
      final Money result = strategy.calculate(5);

      // then
      // 10,000 + (5-1) * 5,000 = 30,000
      assertThat(result).isEqualTo(Money.of(30000));
    }

    @Test
    @DisplayName("실제 시간 대여 시나리오 - 첫 2시간 10,000원 + 추가 시간당 5,000원")
    void calculateRealScenario() {
      // given - 첫 2시간 10,000원 + 추가 시간당 5,000원
      final PricingStrategy strategy = PricingStrategy.initialPlusAdditional(
          Money.of(10000), Money.of(5000));

      // when - 5시간 대여
      final Money result = strategy.calculate(5);

      // then - 10,000 + (5-1) * 5,000 = 30,000
      assertThat(result).isEqualTo(Money.of(30000));
    }
  }

  @Nested
  @DisplayName("가격 계산 테스트 - ONE_TIME")
  class CalculateOneTimeTests {

    @Test
    @DisplayName("수량과 관계없이 1회 대여료만 반환")
    void calculateReturnsFixedPrice() {
      // given
      final PricingStrategy strategy = PricingStrategy.oneTime(Money.of(15000));

      // when & then
      assertThat(strategy.calculate(1)).isEqualTo(Money.of(15000));
      assertThat(strategy.calculate(3)).isEqualTo(Money.of(15000));
      assertThat(strategy.calculate(10)).isEqualTo(Money.of(15000));
    }

    @Test
    @DisplayName("실제 1회 대여 시나리오 - 시간과 무관하게 15,000원")
    void calculateRealScenario() {
      // given
      final PricingStrategy strategy = PricingStrategy.oneTime(Money.of(15000));

      // when - 3시간 대여하든 10시간 대여하든
      final Money result = strategy.calculate(10);

      // then - 1회 대여료 15,000원
      assertThat(result).isEqualTo(Money.of(15000));
    }
  }

  @Nested
  @DisplayName("가격 계산 테스트 - SIMPLE_STOCK")
  class CalculateSimpleStockTests {

    @Test
    @DisplayName("수량과 무관하게 단가만 반환")
    void calculateReturnsUnitPriceRegardlessOfQuantity() {
      // given
      final PricingStrategy strategy = PricingStrategy.simpleStock(Money.of(2000));

      // when & then - 수량과 무관하게 단가 2,000원 반환
      assertThat(strategy.calculate(1)).isEqualTo(Money.of(2000));
      assertThat(strategy.calculate(5)).isEqualTo(Money.of(2000));
      assertThat(strategy.calculate(10)).isEqualTo(Money.of(2000));
    }

    @Test
    @DisplayName("실제 재고 관리 시나리오 - 음료수 개당 2,000원")
    void calculateRealScenario() {
      // given - 음료수 개당 2,000원
      final PricingStrategy strategy = PricingStrategy.simpleStock(Money.of(2000));

      // when - 10개 주문 (calculate는 단가만 반환)
      final Money unitPrice = strategy.calculate(10);

      // then - 단가 2,000원 (총 가격은 Product에서 계산)
      assertThat(unitPrice).isEqualTo(Money.of(2000));
    }
  }

  @Nested
  @DisplayName("유효성 검증 테스트")
  class ValidationTests {

    @Test
    @DisplayName("수량이 0이면 예외 발생")
    void throwExceptionWhenQuantityIsZero() {
      // given
      final PricingStrategy strategy = PricingStrategy.simpleStock(Money.of(1000));

      // when & then
      assertThatThrownBy(() -> strategy.calculate(0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Quantity must be positive: 0");
    }

    @Test
    @DisplayName("수량이 음수면 예외 발생")
    void throwExceptionWhenQuantityIsNegative() {
      // given
      final PricingStrategy strategy = PricingStrategy.simpleStock(Money.of(1000));

      // when & then
      assertThatThrownBy(() -> strategy.calculate(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Quantity must be positive: -1");
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTests {

    @Test
    @DisplayName("같은 내용의 PricingStrategy는 동일")
    void equalityBasedOnContent() {
      // given
      final PricingStrategy strategy1 = PricingStrategy.initialPlusAdditional(
          Money.of(10000), Money.of(5000));
      final PricingStrategy strategy2 = PricingStrategy.initialPlusAdditional(
          Money.of(10000), Money.of(5000));

      // when & then
      assertThat(strategy1).isEqualTo(strategy2);
      assertThat(strategy1.hashCode()).isEqualTo(strategy2.hashCode());
    }

    @Test
    @DisplayName("다른 내용의 PricingStrategy는 다름")
    void inequalityBasedOnDifferentContent() {
      // given
      final PricingStrategy strategy1 = PricingStrategy.initialPlusAdditional(
          Money.of(10000), Money.of(5000));
      final PricingStrategy strategy2 = PricingStrategy.initialPlusAdditional(
          Money.of(10000), Money.of(6000));

      // when & then
      assertThat(strategy1).isNotEqualTo(strategy2);
    }

    @Test
    @DisplayName("다른 PricingType은 다름")
    void inequalityBasedOnDifferentType() {
      // given
      final PricingStrategy strategy1 = PricingStrategy.oneTime(Money.of(10000));
      final PricingStrategy strategy2 = PricingStrategy.simpleStock(Money.of(10000));

      // when & then
      assertThat(strategy1).isNotEqualTo(strategy2);
    }
  }

  @Nested
  @DisplayName("toString 테스트")
  class ToStringTests {

    @Test
    @DisplayName("toString은 모든 필드 정보를 포함")
    void toStringContainsAllFields() {
      // given
      final PricingStrategy strategy = PricingStrategy.initialPlusAdditional(
          Money.of(10000), Money.of(5000));

      // when
      final String result = strategy.toString();

      // then
      assertThat(result).contains("PricingStrategy");
      assertThat(result).contains("INITIAL_PLUS_ADDITIONAL");
      assertThat(result).contains("10000");
      assertThat(result).contains("5000");
    }
  }
}
