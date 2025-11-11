package com.teambind.springproject.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.ReservationId;
import com.teambind.springproject.domain.shared.ReservationStatus;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  @DisplayName("PLACE Scope 재고 검증 테스트")
  class PlaceScopeAvailabilityTests {

    @Test
    @DisplayName("예약이 없는 경우 재고 충분")
    void availableWhenNoReservations() {
      // given
      final PlaceId placeId = PlaceId.of(100L);
      final Product product = Product.createPlaceScoped(
          ProductId.of(1L),
          placeId,
          "빔 프로젝터",
          PricingStrategy.oneTime(Money.of(10000)),
          5
      );

      final LocalDateTime slot1 = LocalDateTime.of(2025, 1, 15, 10, 0);
      final LocalDateTime slot2 = LocalDateTime.of(2025, 1, 15, 11, 0);
      final List<LocalDateTime> requestedSlots = List.of(slot1, slot2);

      // when - No existing reservations
      final boolean available = service.isAvailable(product, requestedSlots, 3, Collections.emptyList());

      // then
      assertThat(available).isTrue();
    }

    @Test
    @DisplayName("기존 예약에서 사용 중인 수량이 있어도 재고 충분한 경우")
    void availableWhenSufficientStockAfterExistingReservations() {
      // given
      final PlaceId placeId = PlaceId.of(100L);
      final ProductId productId = ProductId.of(1L);
      final Product product = Product.createPlaceScoped(
          productId,
          placeId,
          "화이트보드",
          PricingStrategy.simpleStock(Money.of(3000)),
          10  // 총 10개
      );

      final LocalDateTime slot1 = LocalDateTime.of(2025, 1, 15, 10, 0);
      final LocalDateTime slot2 = LocalDateTime.of(2025, 1, 15, 11, 0);
      final List<LocalDateTime> requestedSlots = List.of(slot1, slot2);

      // 기존 예약: slot1에 2개 사용 중
      final Map<LocalDateTime, Money> existingSlots = new HashMap<>();
      existingSlots.put(slot1, Money.of(10000));

      final TimeSlotPriceBreakdown breakdown = new TimeSlotPriceBreakdown(
          existingSlots,
          TimeSlot.HOUR
      );

      final ProductPriceBreakdown productBreakdown = new ProductPriceBreakdown(
          productId,
          "화이트보드",
          2,  // 2개 사용 중
          Money.of(3000),
          Money.of(6000),
          PricingType.SIMPLE_STOCK
      );

      final ReservationPricing existingReservation = ReservationPricing.calculate(
          ReservationId.of(null),
          RoomId.of(1L),
          breakdown,
          List.of(productBreakdown),
          10L
      );

      // when - 5개 요청 (2개 사용 중 + 5개 요청 = 7개 < 10개)
      final boolean available = service.isAvailable(product, requestedSlots, 5, List.of(existingReservation));

      // then
      assertThat(available).isTrue();
    }

    @Test
    @DisplayName("기존 예약으로 재고 부족한 경우")
    void notAvailableWhenInsufficientStockDueToExistingReservations() {
      // given
      final PlaceId placeId = PlaceId.of(100L);
      final ProductId productId = ProductId.of(1L);
      final Product product = Product.createPlaceScoped(
          productId,
          placeId,
          "노트북",
          PricingStrategy.oneTime(Money.of(50000)),
          5  // 총 5개
      );

      final LocalDateTime slot1 = LocalDateTime.of(2025, 1, 15, 10, 0);
      final List<LocalDateTime> requestedSlots = List.of(slot1);

      // 기존 예약: slot1에 4개 사용 중
      final Map<LocalDateTime, Money> existingSlots = Map.of(
          slot1, Money.of(10000)
      );

      final TimeSlotPriceBreakdown breakdown = new TimeSlotPriceBreakdown(
          existingSlots,
          TimeSlot.HOUR
      );

      final ProductPriceBreakdown productBreakdown = new ProductPriceBreakdown(
          productId,
          "노트북",
          4,  // 4개 사용 중
          Money.of(50000),
          Money.of(200000),
          PricingType.ONE_TIME
      );

      final ReservationPricing existingReservation = ReservationPricing.calculate(
          ReservationId.of(null),
          RoomId.of(1L),
          breakdown,
          List.of(productBreakdown),
          10L
      );

      // when - 3개 요청 (4개 사용 중 + 3개 요청 = 7개 > 5개)
      final boolean available = service.isAvailable(product, requestedSlots, 3, List.of(existingReservation));

      // then
      assertThat(available).isFalse();
    }

    @Test
    @DisplayName("시간 슬롯이 null인 경우 예외 발생")
    void throwsExceptionWhenTimeSlotsAreNull() {
      // given
      final Product product = Product.createPlaceScoped(
          ProductId.of(1L),
          PlaceId.of(100L),
          "상품",
          PricingStrategy.simpleStock(Money.of(1000)),
          10
      );

      // when & then
      assertThatThrownBy(() -> service.isAvailable(product, null, 1, Collections.emptyList()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Requested time slots cannot be null or empty");
    }

    @Test
    @DisplayName("시간 슬롯이 비어있는 경우 예외 발생")
    void throwsExceptionWhenTimeSlotsAreEmpty() {
      // given
      final Product product = Product.createPlaceScoped(
          ProductId.of(1L),
          PlaceId.of(100L),
          "상품",
          PricingStrategy.simpleStock(Money.of(1000)),
          10
      );

      // when & then
      assertThatThrownBy(() ->
          service.isAvailable(product, Collections.emptyList(), 1, Collections.emptyList()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Requested time slots cannot be null or empty");
    }
  }

  @Nested
  @DisplayName("ROOM Scope 재고 검증 테스트")
  class RoomScopeAvailabilityTests {

    @Test
    @DisplayName("예약이 없는 경우 재고 충분")
    void availableWhenNoReservations() {
      // given
      final RoomId roomId = RoomId.of(200L);
      final Product product = Product.createRoomScoped(
          ProductId.of(1L),
          PlaceId.of(100L),
          roomId,
          "마이크",
          PricingStrategy.simpleStock(Money.of(5000)),
          3
      );

      final LocalDateTime slot1 = LocalDateTime.of(2025, 1, 15, 10, 0);
      final List<LocalDateTime> requestedSlots = List.of(slot1);

      // when - No existing reservations
      final boolean available = service.isAvailable(product, requestedSlots, 2, Collections.emptyList());

      // then
      assertThat(available).isTrue();
    }

    @Test
    @DisplayName("기존 예약으로 재고 정확히 소진되는 경계값 케이스")
    void availableWhenExactlyConsumingAllStock() {
      // given
      final RoomId roomId = RoomId.of(200L);
      final ProductId productId = ProductId.of(1L);
      final Product product = Product.createRoomScoped(
          productId,
          PlaceId.of(100L),
          roomId,
          "스피커",
          PricingStrategy.simpleStock(Money.of(10000)),
          5  // 총 5개
      );

      final LocalDateTime slot1 = LocalDateTime.of(2025, 1, 15, 10, 0);
      final List<LocalDateTime> requestedSlots = List.of(slot1);

      // 기존 예약: slot1에 2개 사용 중
      final Map<LocalDateTime, Money> existingSlots = Map.of(
          slot1, Money.of(10000)
      );

      final TimeSlotPriceBreakdown breakdown = new TimeSlotPriceBreakdown(
          existingSlots,
          TimeSlot.HOUR
      );

      final ProductPriceBreakdown productBreakdown = new ProductPriceBreakdown(
          productId,
          "스피커",
          2,  // 2개 사용 중
          Money.of(10000),
          Money.of(20000),
          PricingType.SIMPLE_STOCK
      );

      final ReservationPricing existingReservation = ReservationPricing.calculate(
          ReservationId.of(null),
          roomId,
          breakdown,
          List.of(productBreakdown),
          10L
      );

      // when - 3개 요청 (2개 사용 중 + 3개 요청 = 5개, 정확히 소진)
      final boolean available = service.isAvailable(product, requestedSlots, 3, List.of(existingReservation));

      // then
      assertThat(available).isTrue();
    }

    @Test
    @DisplayName("시간 슬롯이 null인 경우 예외 발생")
    void throwsExceptionWhenTimeSlotsAreNull() {
      // given
      final Product product = Product.createRoomScoped(
          ProductId.of(1L),
          PlaceId.of(100L),
          RoomId.of(200L),
          "상품",
          PricingStrategy.simpleStock(Money.of(1000)),
          10
      );

      // when & then
      assertThatThrownBy(() -> service.isAvailable(product, null, 1, Collections.emptyList()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Requested time slots cannot be null or empty");
    }
  }
}
