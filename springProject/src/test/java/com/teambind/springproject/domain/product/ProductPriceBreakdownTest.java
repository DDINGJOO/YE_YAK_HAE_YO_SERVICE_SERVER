package com.teambind.springproject.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.ProductId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ProductPriceBreakdown Value Object 테스트")
class ProductPriceBreakdownTest {

  @Nested
  @DisplayName("생성 테스트")
  class CreationTests {

    @Test
    @DisplayName("정상적인 값으로 생성 성공")
    void createSuccessfully() {
      // given
      final ProductId productId = ProductId.of(1L);
      final String productName = "빔 프로젝터";
      final int quantity = 3;
      final Money unitPrice = Money.of(10000);
      final Money totalPrice = Money.of(30000);
      final PricingType pricingType = PricingType.SIMPLE_STOCK;

      // when
      final ProductPriceBreakdown breakdown = new ProductPriceBreakdown(
          productId, productName, quantity, unitPrice, totalPrice, pricingType);

      // then
      assertThat(breakdown.productId()).isEqualTo(productId);
      assertThat(breakdown.productName()).isEqualTo(productName);
      assertThat(breakdown.quantity()).isEqualTo(quantity);
      assertThat(breakdown.unitPrice()).isEqualTo(unitPrice);
      assertThat(breakdown.totalPrice()).isEqualTo(totalPrice);
      assertThat(breakdown.pricingType()).isEqualTo(pricingType);
    }

    @Test
    @DisplayName("null productId로 생성 시 예외 발생")
    void throwExceptionWhenProductIdIsNull() {
      // when & then
      assertThatThrownBy(() -> new ProductPriceBreakdown(
          null,
          "상품",
          1,
          Money.of(1000),
          Money.of(1000),
          PricingType.SIMPLE_STOCK
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Product ID cannot be null");
    }

    @Test
    @DisplayName("null productName으로 생성 시 예외 발생")
    void throwExceptionWhenProductNameIsNull() {
      // when & then
      assertThatThrownBy(() -> new ProductPriceBreakdown(
          ProductId.of(1L),
          null,
          1,
          Money.of(1000),
          Money.of(1000),
          PricingType.SIMPLE_STOCK
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Product name cannot be null or empty");
    }

    @Test
    @DisplayName("빈 productName으로 생성 시 예외 발생")
    void throwExceptionWhenProductNameIsEmpty() {
      // when & then
      assertThatThrownBy(() -> new ProductPriceBreakdown(
          ProductId.of(1L),
          "   ",
          1,
          Money.of(1000),
          Money.of(1000),
          PricingType.SIMPLE_STOCK
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Product name cannot be null or empty");
    }

    @Test
    @DisplayName("quantity가 0이면 예외 발생")
    void throwExceptionWhenQuantityIsZero() {
      // when & then
      assertThatThrownBy(() -> new ProductPriceBreakdown(
          ProductId.of(1L),
          "상품",
          0,
          Money.of(1000),
          Money.of(0),
          PricingType.SIMPLE_STOCK
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Quantity must be positive: 0");
    }

    @Test
    @DisplayName("quantity가 음수면 예외 발생")
    void throwExceptionWhenQuantityIsNegative() {
      // when & then
      assertThatThrownBy(() -> new ProductPriceBreakdown(
          ProductId.of(1L),
          "상품",
          -1,
          Money.of(1000),
          Money.of(1000),
          PricingType.SIMPLE_STOCK
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Quantity must be positive: -1");
    }

    @Test
    @DisplayName("null unitPrice로 생성 시 예외 발생")
    void throwExceptionWhenUnitPriceIsNull() {
      // when & then
      assertThatThrownBy(() -> new ProductPriceBreakdown(
          ProductId.of(1L),
          "상품",
          1,
          null,
          Money.of(1000),
          PricingType.SIMPLE_STOCK
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Unit price cannot be null");
    }

    @Test
    @DisplayName("null totalPrice로 생성 시 예외 발생")
    void throwExceptionWhenTotalPriceIsNull() {
      // when & then
      assertThatThrownBy(() -> new ProductPriceBreakdown(
          ProductId.of(1L),
          "상품",
          1,
          Money.of(1000),
          null,
          PricingType.SIMPLE_STOCK
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Total price cannot be null");
    }

    @Test
    @DisplayName("null pricingType으로 생성 시 예외 발생")
    void throwExceptionWhenPricingTypeIsNull() {
      // when & then
      assertThatThrownBy(() -> new ProductPriceBreakdown(
          ProductId.of(1L),
          "상품",
          1,
          Money.of(1000),
          Money.of(1000),
          null
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Pricing type cannot be null");
    }
  }

  @Nested
  @DisplayName("가격 일관성 검증 테스트")
  class PriceConsistencyTests {

    @Test
    @DisplayName("totalPrice가 unitPrice * quantity와 일치하면 생성 성공")
    void createSuccessfullyWhenTotalPriceIsConsistent() {
      // given
      final Money unitPrice = Money.of(5000);
      final int quantity = 3;
      final Money totalPrice = Money.of(15000);

      // when
      final ProductPriceBreakdown breakdown = new ProductPriceBreakdown(
          ProductId.of(1L),
          "상품",
          quantity,
          unitPrice,
          totalPrice,
          PricingType.SIMPLE_STOCK
      );

      // then
      assertThat(breakdown.totalPrice()).isEqualTo(totalPrice);
    }

    @Test
    @DisplayName("totalPrice가 unitPrice * quantity와 일치하지 않으면 예외 발생")
    void throwExceptionWhenTotalPriceIsInconsistent() {
      // given
      final Money unitPrice = Money.of(5000);
      final int quantity = 3;
      final Money wrongTotalPrice = Money.of(10000); // 잘못된 값

      // when & then
      assertThatThrownBy(() -> new ProductPriceBreakdown(
          ProductId.of(1L),
          "상품",
          quantity,
          unitPrice,
          wrongTotalPrice,
          PricingType.SIMPLE_STOCK
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Total price mismatch");
    }

    @Test
    @DisplayName("INITIAL_PLUS_ADDITIONAL 방식의 가격 일관성 검증")
    void validateInitialPlusAdditionalPriceConsistency() {
      // given
      final Money unitPrice = Money.of(20000); // 초기 10,000 + (3-1) * 5,000
      final int quantity = 3;
      final Money totalPrice = Money.of(60000); // 20,000 * 3

      // when
      final ProductPriceBreakdown breakdown = new ProductPriceBreakdown(
          ProductId.of(1L),
          "빔 프로젝터",
          quantity,
          unitPrice,
          totalPrice,
          PricingType.INITIAL_PLUS_ADDITIONAL
      );

      // then
      assertThat(breakdown.totalPrice()).isEqualTo(totalPrice);
    }

    @Test
    @DisplayName("ONE_TIME 방식의 가격 일관성 검증")
    void validateOneTimePriceConsistency() {
      // given
      final Money unitPrice = Money.of(15000); // 1회 대여료
      final int quantity = 2;
      final Money totalPrice = Money.of(30000); // 15,000 * 2

      // when
      final ProductPriceBreakdown breakdown = new ProductPriceBreakdown(
          ProductId.of(1L),
          "장비",
          quantity,
          unitPrice,
          totalPrice,
          PricingType.ONE_TIME
      );

      // then
      assertThat(breakdown.totalPrice()).isEqualTo(totalPrice);
    }

    @Test
    @DisplayName("SIMPLE_STOCK 방식의 가격 일관성 검증")
    void validateSimpleStockPriceConsistency() {
      // given
      final Money unitPrice = Money.of(2000);
      final int quantity = 5;
      final Money totalPrice = Money.of(10000); // 2,000 * 5

      // when
      final ProductPriceBreakdown breakdown = new ProductPriceBreakdown(
          ProductId.of(1L),
          "음료수",
          quantity,
          unitPrice,
          totalPrice,
          PricingType.SIMPLE_STOCK
      );

      // then
      assertThat(breakdown.totalPrice()).isEqualTo(totalPrice);
    }
  }

  @Nested
  @DisplayName("불변성 테스트")
  class ImmutabilityTests {

    @Test
    @DisplayName("Record는 불변이므로 값 변경 불가")
    void recordIsImmutable() {
      // given
      final ProductPriceBreakdown breakdown = new ProductPriceBreakdown(
          ProductId.of(1L),
          "상품",
          3,
          Money.of(5000),
          Money.of(15000),
          PricingType.SIMPLE_STOCK
      );

      // when & then
      assertThat(breakdown.productId()).isEqualTo(ProductId.of(1L));
      assertThat(breakdown.quantity()).isEqualTo(3);
      // Record는 setter가 없으므로 값 변경 시도 자체가 컴파일 에러
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTests {

    @Test
    @DisplayName("모든 필드가 같으면 동일")
    void equalityBasedOnAllFields() {
      // given
      final ProductPriceBreakdown breakdown1 = new ProductPriceBreakdown(
          ProductId.of(1L),
          "상품",
          3,
          Money.of(5000),
          Money.of(15000),
          PricingType.SIMPLE_STOCK
      );

      final ProductPriceBreakdown breakdown2 = new ProductPriceBreakdown(
          ProductId.of(1L),
          "상품",
          3,
          Money.of(5000),
          Money.of(15000),
          PricingType.SIMPLE_STOCK
      );

      // when & then
      assertThat(breakdown1).isEqualTo(breakdown2);
      assertThat(breakdown1.hashCode()).isEqualTo(breakdown2.hashCode());
    }

    @Test
    @DisplayName("하나라도 다르면 다름")
    void inequalityWhenAnyFieldDiffers() {
      // given
      final ProductPriceBreakdown breakdown1 = new ProductPriceBreakdown(
          ProductId.of(1L),
          "상품",
          3,
          Money.of(5000),
          Money.of(15000),
          PricingType.SIMPLE_STOCK
      );

      final ProductPriceBreakdown breakdown2 = new ProductPriceBreakdown(
          ProductId.of(1L),
          "상품",
          4, // 다른 수량
          Money.of(5000),
          Money.of(20000),
          PricingType.SIMPLE_STOCK
      );

      // when & then
      assertThat(breakdown1).isNotEqualTo(breakdown2);
    }
  }
}
