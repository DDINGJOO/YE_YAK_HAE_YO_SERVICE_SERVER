package com.teambind.springproject.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ProductAvailabilityService 도메인 서비스 테스트")
class ProductAvailabilityServiceTest {

  private ProductAvailabilityService service;

  @BeforeEach
  void setUp() {
    service = new ProductAvailabilityService();
  }

  @Nested
  @DisplayName("Simple Scope (RESERVATION) 재고 검증 테스트")
  class SimpleStockAvailabilityTests {

    @Test
    @DisplayName("재고가 충분한 경우 true 반환")
    void availableWhenStockIsSufficient() {
      // given
      final Product product = Product.createReservationScoped(
          ProductId.of(1L),
          "음료수",
          PricingStrategy.simpleStock(Money.of(2000)),
          10  // 총 재고 10개
      );
      final int requestedQuantity = 5;  // 5개 요청

      // when
      final boolean available = service.isAvailable(product, null, requestedQuantity, null);

      // then
      assertThat(available).isTrue();
    }

    @Test
    @DisplayName("재고가 부족한 경우 false 반환")
    void notAvailableWhenStockIsInsufficient() {
      // given
      final Product product = Product.createReservationScoped(
          ProductId.of(2L),
          "간식 세트",
          PricingStrategy.simpleStock(Money.of(5000)),
          5  // 총 재고 5개
      );
      final int requestedQuantity = 10;  // 10개 요청 (재고 부족)

      // when
      final boolean available = service.isAvailable(product, null, requestedQuantity, null);

      // then
      assertThat(available).isFalse();
    }

    @Test
    @DisplayName("재고를 정확히 소진하는 경우 true 반환 (경계값)")
    void availableWhenExactlyConsumingAllStock() {
      // given
      final Product product = Product.createReservationScoped(
          ProductId.of(3L),
          "커피",
          PricingStrategy.simpleStock(Money.of(3000)),
          7  // 총 재고 7개
      );
      final int requestedQuantity = 7;  // 정확히 7개 요청

      // when
      final boolean available = service.isAvailable(product, null, requestedQuantity, null);

      // then
      assertThat(available).isTrue();
    }

    @Test
    @DisplayName("재고를 1개 초과하는 경우 false 반환 (경계값)")
    void notAvailableWhenExceedingStockByOne() {
      // given
      final Product product = Product.createReservationScoped(
          ProductId.of(4L),
          "주스",
          PricingStrategy.simpleStock(Money.of(4000)),
          10  // 총 재고 10개
      );
      final int requestedQuantity = 11;  // 11개 요청 (1개 초과)

      // when
      final boolean available = service.isAvailable(product, null, requestedQuantity, null);

      // then
      assertThat(available).isFalse();
    }

    @Test
    @DisplayName("요청 수량이 0인 경우 IllegalArgumentException 발생")
    void throwsExceptionWhenRequestedQuantityIsZero() {
      // given
      final Product product = Product.createReservationScoped(
          ProductId.of(5L),
          "물",
          PricingStrategy.simpleStock(Money.of(1000)),
          10
      );
      final int requestedQuantity = 0;

      // when & then
      assertThatThrownBy(() -> service.isAvailable(product, null, requestedQuantity, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Requested quantity must be positive");
    }

    @Test
    @DisplayName("요청 수량이 음수인 경우 IllegalArgumentException 발생")
    void throwsExceptionWhenRequestedQuantityIsNegative() {
      // given
      final Product product = Product.createReservationScoped(
          ProductId.of(6L),
          "에너지 드링크",
          PricingStrategy.simpleStock(Money.of(2500)),
          10
      );
      final int requestedQuantity = -5;

      // when & then
      assertThatThrownBy(() -> service.isAvailable(product, null, requestedQuantity, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Requested quantity must be positive: -5");
    }

    @Test
    @DisplayName("재고가 0인 상품에 요청 시 false 반환")
    void notAvailableWhenStockIsZero() {
      // given
      final Product product = Product.createReservationScoped(
          ProductId.of(7L),
          "품절 상품",
          PricingStrategy.simpleStock(Money.of(1000)),
          0  // 재고 0
      );
      final int requestedQuantity = 1;

      // when
      final boolean available = service.isAvailable(product, null, requestedQuantity, null);

      // then
      assertThat(available).isFalse();
    }

    @Test
    @DisplayName("대량 재고와 대량 요청 시 정상 동작")
    void worksWithLargeQuantities() {
      // given
      final Product product = Product.createReservationScoped(
          ProductId.of(8L),
          "대량 상품",
          PricingStrategy.simpleStock(Money.of(1000)),
          1000  // 총 재고 1000개
      );
      final int requestedQuantity = 500;  // 500개 요청

      // when
      final boolean available = service.isAvailable(product, null, requestedQuantity, null);

      // then
      assertThat(available).isTrue();
    }
  }

  @Nested
  @DisplayName("Place/Room Scope 미구현 검증 테스트")
  class UnsupportedScopeTests {

    @Test
    @DisplayName("PLACE Scope 호출 시 UnsupportedOperationException 발생")
    void throwsExceptionForPlaceScope() {
      // given
      final Product product = Product.createPlaceScoped(
          ProductId.of(9L),
          com.teambind.springproject.domain.shared.PlaceId.of(100L),
          "플레이스 상품",
          PricingStrategy.oneTime(Money.of(10000)),
          5
      );

      // when & then
      assertThatThrownBy(() -> service.isAvailable(product, null, 1, null))
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("Scope PLACE is not yet implemented")
          .hasMessageContaining("Issue #15");
    }

    @Test
    @DisplayName("ROOM Scope 호출 시 UnsupportedOperationException 발생")
    void throwsExceptionForRoomScope() {
      // given
      final Product product = Product.createRoomScoped(
          ProductId.of(10L),
          com.teambind.springproject.domain.shared.PlaceId.of(100L),
          com.teambind.springproject.domain.shared.RoomId.of(200L),
          "룸 상품",
          PricingStrategy.simpleStock(Money.of(5000)),
          10
      );

      // when & then
      assertThatThrownBy(() -> service.isAvailable(product, null, 1, null))
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("Scope ROOM is not yet implemented")
          .hasMessageContaining("Issue #15");
    }
  }
}
