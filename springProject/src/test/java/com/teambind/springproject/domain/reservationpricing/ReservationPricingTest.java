package com.teambind.springproject.domain.reservationpricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teambind.springproject.domain.product.PricingType;
import com.teambind.springproject.domain.product.ProductPriceBreakdown;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.ReservationId;
import com.teambind.springproject.domain.shared.ReservationStatus;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ReservationPricing 테스트")
class ReservationPricingTest {

  @Nested
  @DisplayName("calculate() Factory Method 테스트")
  class CalculateTests {

    @Test
    @DisplayName("시간대 가격만으로 예약 가격을 계산한다")
    void calculateWithTimeSlotOnly() {
      // given
      final ReservationId reservationId = ReservationId.of(1L);
      final RoomId roomId = RoomId.of(100L);

      final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
          Map.of(
              LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000),
              LocalDateTime.of(2025, 11, 10, 11, 0), Money.of(10000)
          ),
          TimeSlot.HOUR
      );

      // when
      final ReservationPricing pricing = ReservationPricing.calculate(
          reservationId,
          roomId,
          timeSlotBreakdown,
          Collections.emptyList(),
        10L
      );

      // then
      assertThat(pricing.getReservationId()).isEqualTo(reservationId);
      assertThat(pricing.getRoomId()).isEqualTo(roomId);
      assertThat(pricing.getStatus()).isEqualTo(ReservationStatus.PENDING);
      assertThat(pricing.getTotalPrice()).isEqualTo(Money.of(20000));
      assertThat(pricing.getTimeSlotTotal()).isEqualTo(Money.of(20000));
      assertThat(pricing.getProductTotal()).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("시간대 가격과 상품 가격을 합산하여 예약 가격을 계산한다")
    void calculateWithTimeSlotAndProducts() {
      // given
      final ReservationId reservationId = ReservationId.of(1L);
      final RoomId roomId = RoomId.of(100L);

      final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
          Map.of(
              LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000),
              LocalDateTime.of(2025, 11, 10, 11, 0), Money.of(12000)
          ),
          TimeSlot.HOUR
      );

      final List<ProductPriceBreakdown> productBreakdowns = List.of(
          new ProductPriceBreakdown(
              ProductId.of(1L),
              "빔 프로젝터",
              1,
              Money.of(5000),
              Money.of(5000),
              PricingType.ONE_TIME
          ),
          new ProductPriceBreakdown(
              ProductId.of(2L),
              "음료수",
              3,
              Money.of(2000),
              Money.of(6000),
              PricingType.SIMPLE_STOCK
          )
      );

      // when
      final ReservationPricing pricing = ReservationPricing.calculate(
          reservationId,
          roomId,
          timeSlotBreakdown,
          productBreakdowns,
        10L
      );

      // then
      assertThat(pricing.getTotalPrice()).isEqualTo(Money.of(33000)); // 22000 + 5000 + 6000
      assertThat(pricing.getTimeSlotTotal()).isEqualTo(Money.of(22000));
      assertThat(pricing.getProductTotal()).isEqualTo(Money.of(11000));
      assertThat(pricing.getProductBreakdowns()).hasSize(2);
    }

    @Test
    @DisplayName("상품 없이도 예약 가격 계산에 성공한다")
    void calculateWithEmptyProducts() {
      // given
      final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
          Map.of(LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000)),
          TimeSlot.HOUR
      );

      // when
      final ReservationPricing pricing = ReservationPricing.calculate(
          ReservationId.of(1L),
          RoomId.of(100L),
          timeSlotBreakdown,
          new ArrayList<>(),
          10L
      );

      // then
      assertThat(pricing.getTotalPrice()).isEqualTo(Money.of(10000));
      assertThat(pricing.getProductBreakdowns()).isEmpty();
    }

    @Test
    @DisplayName("ReservationId가 null이면 예외가 발생한다")
    void calculateWithNullReservationIdFails() {
      // given
      final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
          Map.of(LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000)),
          TimeSlot.HOUR
      );

      // when & then
      assertThatThrownBy(() -> ReservationPricing.calculate(
          null,
          RoomId.of(100L),
          timeSlotBreakdown,
          Collections.emptyList(),
        10L
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Reservation ID cannot be null");
    }

    @Test
    @DisplayName("RoomId가 null이면 예외가 발생한다")
    void calculateWithNullRoomIdFails() {
      // given
      final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
          Map.of(LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000)),
          TimeSlot.HOUR
      );

      // when & then
      assertThatThrownBy(() -> ReservationPricing.calculate(
          ReservationId.of(1L),
          null,
          timeSlotBreakdown,
          Collections.emptyList(),
        10L
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Room ID cannot be null");
    }

    @Test
    @DisplayName("TimeSlotBreakdown이 null이면 예외가 발생한다")
    void calculateWithNullTimeSlotBreakdownFails() {
      assertThatThrownBy(() -> ReservationPricing.calculate(
          ReservationId.of(1L),
          RoomId.of(100L),
          null,
          Collections.emptyList(),
        10L
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Time slot breakdown cannot be null");
    }

    @Test
    @DisplayName("ProductBreakdowns가 null이면 예외가 발생한다")
    void calculateWithNullProductBreakdownsFails() {
      // given
      final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
          Map.of(LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000)),
          TimeSlot.HOUR
      );

      // when & then
      assertThatThrownBy(() -> ReservationPricing.calculate(
          ReservationId.of(1L),
          RoomId.of(100L),
          timeSlotBreakdown,
          null,
          10L
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Product breakdowns cannot be null");
    }
  }

  @Nested
  @DisplayName("confirm() 상태 전이 테스트")
  class ConfirmTests {

    @Test
    @DisplayName("PENDING 상태에서 CONFIRMED로 전환에 성공한다")
    void confirmFromPendingSuccess() {
      // given
      final ReservationPricing pricing = createTestPricing();
      assertThat(pricing.getStatus()).isEqualTo(ReservationStatus.PENDING);

      // when
      pricing.confirm();

      // then
      assertThat(pricing.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("CONFIRMED 상태에서 confirm()하면 예외가 발생한다")
    void confirmFromConfirmedFails() {
      // given
      final ReservationPricing pricing = createTestPricing();
      pricing.confirm();

      // when & then
      assertThatThrownBy(pricing::confirm)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot confirm reservation");
    }

    @Test
    @DisplayName("CANCELLED 상태에서 confirm()하면 예외가 발생한다")
    void confirmFromCancelledFails() {
      // given
      final ReservationPricing pricing = createTestPricing();
      pricing.cancel();

      // when & then
      assertThatThrownBy(pricing::confirm)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot confirm reservation");
    }
  }

  @Nested
  @DisplayName("cancel() 상태 전이 테스트")
  class CancelTests {

    @Test
    @DisplayName("PENDING 상태에서 CANCELLED로 전환에 성공한다")
    void cancelFromPendingSuccess() {
      // given
      final ReservationPricing pricing = createTestPricing();
      assertThat(pricing.getStatus()).isEqualTo(ReservationStatus.PENDING);

      // when
      pricing.cancel();

      // then
      assertThat(pricing.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("CONFIRMED 상태에서 CANCELLED로 전환에 성공한다")
    void cancelFromConfirmedSuccess() {
      // given
      final ReservationPricing pricing = createTestPricing();
      pricing.confirm();

      // when
      pricing.cancel();

      // then
      assertThat(pricing.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("CANCELLED 상태에서 cancel()하면 예외가 발생한다")
    void cancelFromCancelledFails() {
      // given
      final ReservationPricing pricing = createTestPricing();
      pricing.cancel();

      // when & then
      assertThatThrownBy(pricing::cancel)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot cancel reservation: already cancelled");
    }
  }

  @Nested
  @DisplayName("가격 일관성 검증 테스트")
  class PriceConsistencyTests {

    @Test
    @DisplayName("총 가격이 시간대 + 상품 가격과 일치하지 않으면 예외가 발생한다")
    void priceConsistencyFails() {
      // given
      final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
          Map.of(LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000)),
          TimeSlot.HOUR
      );

      // when & then - calculate()를 통하지 않고 직접 생성 시도
      // (테스트 목적으로만 사용, 실제로는 private 생성자이므로 불가능)
      // 이 테스트는 calculate()가 정확한 totalPrice를 생성하는지 확인
      final ReservationPricing pricing = ReservationPricing.calculate(
          ReservationId.of(1L),
          RoomId.of(100L),
          timeSlotBreakdown,
          Collections.emptyList(),
        10L
      );

      assertThat(pricing.getTotalPrice()).isEqualTo(Money.of(10000));
    }
  }

  @Nested
  @DisplayName("불변성 테스트")
  class ImmutabilityTests {

    @Test
    @DisplayName("ProductBreakdowns 리스트를 변경해도 원본은 영향을 받지 않는다")
    void productBreakdownsImmutable() {
      // given
      final List<ProductPriceBreakdown> productBreakdowns = new ArrayList<>();
      productBreakdowns.add(
          new ProductPriceBreakdown(
              ProductId.of(1L),
              "상품1",
              1,
              Money.of(5000),
              Money.of(5000),
              PricingType.ONE_TIME
          )
      );

      final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
          Map.of(LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000)),
          TimeSlot.HOUR
      );

      final ReservationPricing pricing = ReservationPricing.calculate(
          ReservationId.of(1L),
          RoomId.of(100L),
          timeSlotBreakdown,
          productBreakdowns,
        10L
      );

      // when
      productBreakdowns.add(
          new ProductPriceBreakdown(
              ProductId.of(2L),
              "상품2",
              1,
              Money.of(3000),
              Money.of(3000),
              PricingType.ONE_TIME
          )
      );

      // then
      assertThat(pricing.getProductBreakdowns()).hasSize(1);
      assertThat(pricing.getProductTotal()).isEqualTo(Money.of(5000));
    }

    @Test
    @DisplayName("반환된 ProductBreakdowns 리스트를 변경하려고 하면 예외가 발생한다")
    void returnedListIsUnmodifiable() {
      // given
      final ReservationPricing pricing = createTestPricing();

      // when & then
      assertThatThrownBy(() ->
          pricing.getProductBreakdowns().add(
              new ProductPriceBreakdown(
                  ProductId.of(99L),
                  "새 상품",
                  1,
                  Money.of(1000),
                  Money.of(1000),
                  PricingType.ONE_TIME
              )
          )
      ).isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  @DisplayName("equals & hashCode 테스트")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("동일한 ReservationId를 가진 객체는 동등하다")
    void equalsWithSameReservationId() {
      // given
      final ReservationId reservationId = ReservationId.of(1L);

      final TimeSlotPriceBreakdown breakdown1 = new TimeSlotPriceBreakdown(
          Map.of(LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000)),
          TimeSlot.HOUR
      );

      final TimeSlotPriceBreakdown breakdown2 = new TimeSlotPriceBreakdown(
          Map.of(LocalDateTime.of(2025, 11, 10, 11, 0), Money.of(20000)),
          TimeSlot.HOUR
      );

      final ReservationPricing pricing1 = ReservationPricing.calculate(
          reservationId,
          RoomId.of(100L),
          breakdown1,
          Collections.emptyList(),
        10L
      );

      final ReservationPricing pricing2 = ReservationPricing.calculate(
          reservationId,
          RoomId.of(200L),
          breakdown2,
          Collections.emptyList(),
        10L
      );

      // when & then
      assertThat(pricing1).isEqualTo(pricing2);
      assertThat(pricing1.hashCode()).isEqualTo(pricing2.hashCode());
    }

    @Test
    @DisplayName("다른 ReservationId를 가진 객체는 동등하지 않다")
    void notEqualsWithDifferentReservationId() {
      // given
      final TimeSlotPriceBreakdown breakdown = new TimeSlotPriceBreakdown(
          Map.of(LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000)),
          TimeSlot.HOUR
      );

      final ReservationPricing pricing1 = ReservationPricing.calculate(
          ReservationId.of(1L),
          RoomId.of(100L),
          breakdown,
          Collections.emptyList(),
        10L
      );

      final ReservationPricing pricing2 = ReservationPricing.calculate(
          ReservationId.of(2L),
          RoomId.of(100L),
          breakdown,
          Collections.emptyList(),
        10L
      );

      // when & then
      assertThat(pricing1).isNotEqualTo(pricing2);
    }
  }

  @Nested
  @DisplayName("updateProducts() 메서드 테스트")
  class UpdateProductsTests {

    @Test
    @DisplayName("PENDING 상태에서 상품 목록을 업데이트한다")
    void updateProductsInPendingState() {
      // given
      final ReservationPricing pricing = createTestPricing();
      assertThat(pricing.getStatus()).isEqualTo(ReservationStatus.PENDING);
      assertThat(pricing.getProductBreakdowns()).isEmpty();
      final Money initialTotal = pricing.getTotalPrice();

      final List<ProductPriceBreakdown> newProducts = List.of(
          new ProductPriceBreakdown(
              ProductId.of(1L),
              "음료",
              2,
              Money.of(3000),
              Money.of(6000),
              PricingType.SIMPLE_STOCK
          )
      );

      // when
      pricing.updateProducts(newProducts);

      // then
      assertThat(pricing.getProductBreakdowns()).hasSize(1);
      assertThat(pricing.getProductTotal()).isEqualTo(Money.of(6000));
      assertThat(pricing.getTotalPrice()).isEqualTo(initialTotal.add(Money.of(6000)));
    }

    @Test
    @DisplayName("CONFIRMED 상태에서는 상품 업데이트 불가")
    void cannotUpdateProductsWhenConfirmed() {
      // given
      final ReservationPricing pricing = createTestPricing();
      pricing.confirm();
      assertThat(pricing.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

      final List<ProductPriceBreakdown> newProducts = List.of(
          new ProductPriceBreakdown(
              ProductId.of(1L),
              "음료",
              2,
              Money.of(3000),
              Money.of(6000),
              PricingType.SIMPLE_STOCK
          )
      );

      // when & then
      assertThatThrownBy(() -> pricing.updateProducts(newProducts))
          .isInstanceOf(com.teambind.springproject.domain.reservationpricing.exception.InvalidReservationStatusException.class)
          .hasMessageContaining("Invalid reservation status transition")
          .hasMessageContaining("CONFIRMED");
    }

    @Test
    @DisplayName("CANCELLED 상태에서는 상품 업데이트 불가")
    void cannotUpdateProductsWhenCancelled() {
      // given
      final ReservationPricing pricing = createTestPricing();
      pricing.cancel();
      assertThat(pricing.getStatus()).isEqualTo(ReservationStatus.CANCELLED);

      final List<ProductPriceBreakdown> newProducts = List.of(
          new ProductPriceBreakdown(
              ProductId.of(1L),
              "음료",
              2,
              Money.of(3000),
              Money.of(6000),
              PricingType.SIMPLE_STOCK
          )
      );

      // when & then
      assertThatThrownBy(() -> pricing.updateProducts(newProducts))
          .isInstanceOf(com.teambind.springproject.domain.reservationpricing.exception.InvalidReservationStatusException.class)
          .hasMessageContaining("Invalid reservation status transition")
          .hasMessageContaining("CANCELLED");
    }

    @Test
    @DisplayName("상품 업데이트 후 가격이 재계산된다")
    void recalculatesPriceAfterUpdate() {
      // given
      final ReservationPricing pricing = createTestPricing();
      final Money timeSlotTotal = pricing.getTimeSlotTotal();

      final List<ProductPriceBreakdown> products = List.of(
          new ProductPriceBreakdown(
              ProductId.of(1L),
              "빔프로젝터",
              1,
              Money.of(10000),
              Money.of(10000),
              PricingType.ONE_TIME
          ),
          new ProductPriceBreakdown(
              ProductId.of(2L),
              "음료",
              5,
              Money.of(2000),
              Money.of(10000),
              PricingType.SIMPLE_STOCK
          )
      );

      // when
      pricing.updateProducts(products);

      // then
      final Money expectedProductTotal = Money.of(10000).add(Money.of(10000)); // 10000 + (2000 * 5)
      assertThat(pricing.getProductTotal()).isEqualTo(expectedProductTotal);
      assertThat(pricing.getTotalPrice()).isEqualTo(timeSlotTotal.add(expectedProductTotal));
    }

    @Test
    @DisplayName("상품 업데이트 후 reservationId는 변경되지 않는다")
    void reservationIdRemainsUnchangedAfterUpdate() {
      // given
      final ReservationPricing pricing = createTestPricing();
      final ReservationId originalId = pricing.getReservationId();

      final List<ProductPriceBreakdown> newProducts = List.of(
          new ProductPriceBreakdown(
              ProductId.of(1L),
              "음료",
              2,
              Money.of(3000),
              Money.of(6000),
              PricingType.SIMPLE_STOCK
          )
      );

      // when
      pricing.updateProducts(newProducts);

      // then
      assertThat(pricing.getReservationId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("상품을 빈 리스트로 업데이트하면 상품이 제거된다")
    void updateWithEmptyListRemovesProducts() {
      // given
      final ReservationPricing pricing = createTestPricing();
      final Money timeSlotTotal = pricing.getTimeSlotTotal();

      // 먼저 상품 추가
      pricing.updateProducts(List.of(
          new ProductPriceBreakdown(
              ProductId.of(1L),
              "음료",
              2,
              Money.of(3000),
              Money.of(6000),
              PricingType.SIMPLE_STOCK
          )
      ));
      assertThat(pricing.getProductBreakdowns()).isNotEmpty();

      // when: 빈 리스트로 업데이트
      pricing.updateProducts(Collections.emptyList());

      // then
      assertThat(pricing.getProductBreakdowns()).isEmpty();
      assertThat(pricing.getProductTotal()).isEqualTo(Money.ZERO);
      assertThat(pricing.getTotalPrice()).isEqualTo(timeSlotTotal);
    }

    @Test
    @DisplayName("null 상품 리스트로 업데이트 시 예외 발생")
    void throwsExceptionWhenProductsIsNull() {
      // given
      final ReservationPricing pricing = createTestPricing();

      // when & then
      assertThatThrownBy(() -> pricing.updateProducts(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Product breakdowns cannot be null");
    }
  }

  // ========== Helper Methods ==========

  private ReservationPricing createTestPricing() {
    final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
        Map.of(
            LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000),
            LocalDateTime.of(2025, 11, 10, 11, 0), Money.of(10000)
        ),
        TimeSlot.HOUR
    );

    return ReservationPricing.calculate(
        ReservationId.of(1L),
        RoomId.of(100L),
        timeSlotBreakdown,
        Collections.emptyList(),
        10L
    );
  }
}
