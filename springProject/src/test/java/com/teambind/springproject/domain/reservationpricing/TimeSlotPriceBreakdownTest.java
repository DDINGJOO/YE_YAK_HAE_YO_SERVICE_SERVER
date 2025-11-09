package com.teambind.springproject.domain.reservationpricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.TimeSlot;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TimeSlotPriceBreakdown 테스트")
class TimeSlotPriceBreakdownTest {

  @Nested
  @DisplayName("생성 테스트")
  class CreateTests {

    @Test
    @DisplayName("유효한 슬롯 가격으로 생성에 성공한다")
    void createSuccess() {
      // given
      final Map<LocalDateTime, Money> slotPrices = new HashMap<>();
      slotPrices.put(LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000));
      slotPrices.put(LocalDateTime.of(2025, 11, 10, 11, 0), Money.of(10000));

      // when
      final TimeSlotPriceBreakdown breakdown = new TimeSlotPriceBreakdown(
          slotPrices,
          TimeSlot.HOUR
      );

      // then
      assertThat(breakdown.slotPrices()).hasSize(2);
      assertThat(breakdown.timeSlot()).isEqualTo(TimeSlot.HOUR);
    }

    @Test
    @DisplayName("슬롯 가격이 null이면 예외가 발생한다")
    void slotPricesNullFails() {
      assertThatThrownBy(() -> new TimeSlotPriceBreakdown(null, TimeSlot.HOUR))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Slot prices cannot be null");
    }

    @Test
    @DisplayName("슬롯 가격이 빈 Map이면 예외가 발생한다")
    void slotPricesEmptyFails() {
      assertThatThrownBy(
          () -> new TimeSlotPriceBreakdown(Collections.emptyMap(), TimeSlot.HOUR))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Slot prices cannot be empty");
    }

    @Test
    @DisplayName("TimeSlot이 null이면 예외가 발생한다")
    void timeSlotNullFails() {
      // given
      final Map<LocalDateTime, Money> slotPrices = Map.of(
          LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000)
      );

      // when & then
      assertThatThrownBy(() -> new TimeSlotPriceBreakdown(slotPrices, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Time slot cannot be null");
    }

    @Test
    @DisplayName("슬롯 시간이 null이면 예외가 발생한다")
    void slotTimeNullFails() {
      // given
      final Map<LocalDateTime, Money> slotPrices = new HashMap<>();
      slotPrices.put(null, Money.of(10000));

      // when & then
      assertThatThrownBy(() -> new TimeSlotPriceBreakdown(slotPrices, TimeSlot.HOUR))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Slot time cannot be null");
    }

    @Test
    @DisplayName("슬롯 가격이 null이면 예외가 발생한다")
    void slotPriceNullFails() {
      // given
      final Map<LocalDateTime, Money> slotPrices = new HashMap<>();
      slotPrices.put(LocalDateTime.of(2025, 11, 10, 10, 0), null);

      // when & then
      assertThatThrownBy(() -> new TimeSlotPriceBreakdown(slotPrices, TimeSlot.HOUR))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Slot price cannot be null");
    }

    @Test
    @DisplayName("음수 가격이면 예외가 발생한다")
    void negativePriceFails() {
      // when & then - Money.of()가 음수를 허용하지 않음
      assertThatThrownBy(() -> Money.of(-1000))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Amount cannot be negative");
    }
  }

  @Nested
  @DisplayName("getTotalPrice 테스트")
  class GetTotalPriceTests {

    @Test
    @DisplayName("모든 슬롯 가격의 합계를 반환한다")
    void getTotalPriceSuccess() {
      // given
      final Map<LocalDateTime, Money> slotPrices = Map.of(
          LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000),
          LocalDateTime.of(2025, 11, 10, 11, 0), Money.of(12000),
          LocalDateTime.of(2025, 11, 10, 12, 0), Money.of(8000)
      );

      final TimeSlotPriceBreakdown breakdown = new TimeSlotPriceBreakdown(
          slotPrices,
          TimeSlot.HOUR
      );

      // when
      final Money totalPrice = breakdown.getTotalPrice();

      // then
      assertThat(totalPrice).isEqualTo(Money.of(30000));
    }

    @Test
    @DisplayName("슬롯이 1개일 때 가격을 반환한다")
    void getSingleSlotPrice() {
      // given
      final Map<LocalDateTime, Money> slotPrices = Map.of(
          LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(15000)
      );

      final TimeSlotPriceBreakdown breakdown = new TimeSlotPriceBreakdown(
          slotPrices,
          TimeSlot.HOUR
      );

      // when
      final Money totalPrice = breakdown.getTotalPrice();

      // then
      assertThat(totalPrice).isEqualTo(Money.of(15000));
    }
  }

  @Nested
  @DisplayName("getSlotCount 테스트")
  class GetSlotCountTests {

    @Test
    @DisplayName("슬롯 개수를 반환한다")
    void getSlotCountSuccess() {
      // given
      final Map<LocalDateTime, Money> slotPrices = Map.of(
          LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000),
          LocalDateTime.of(2025, 11, 10, 11, 0), Money.of(10000),
          LocalDateTime.of(2025, 11, 10, 12, 0), Money.of(10000)
      );

      final TimeSlotPriceBreakdown breakdown = new TimeSlotPriceBreakdown(
          slotPrices,
          TimeSlot.HOUR
      );

      // when & then
      assertThat(breakdown.getSlotCount()).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("getPriceAt 테스트")
  class GetPriceAtTests {

    @Test
    @DisplayName("특정 시간의 가격을 조회한다")
    void getPriceAtSuccess() {
      // given
      final LocalDateTime slot1 = LocalDateTime.of(2025, 11, 10, 10, 0);
      final LocalDateTime slot2 = LocalDateTime.of(2025, 11, 10, 11, 0);

      final Map<LocalDateTime, Money> slotPrices = Map.of(
          slot1, Money.of(10000),
          slot2, Money.of(15000)
      );

      final TimeSlotPriceBreakdown breakdown = new TimeSlotPriceBreakdown(
          slotPrices,
          TimeSlot.HOUR
      );

      // when & then
      assertThat(breakdown.getPriceAt(slot1)).isEqualTo(Money.of(10000));
      assertThat(breakdown.getPriceAt(slot2)).isEqualTo(Money.of(15000));
    }

    @Test
    @DisplayName("존재하지 않는 시간은 ZERO를 반환한다")
    void getPriceAtNotExistsReturnsZero() {
      // given
      final Map<LocalDateTime, Money> slotPrices = Map.of(
          LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000)
      );

      final TimeSlotPriceBreakdown breakdown = new TimeSlotPriceBreakdown(
          slotPrices,
          TimeSlot.HOUR
      );

      // when
      final Money price = breakdown.getPriceAt(LocalDateTime.of(2025, 11, 10, 15, 0));

      // then
      assertThat(price).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("null 시간을 조회하면 예외가 발생한다")
    void getPriceAtNullFails() {
      // given
      final Map<LocalDateTime, Money> slotPrices = Map.of(
          LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000)
      );

      final TimeSlotPriceBreakdown breakdown = new TimeSlotPriceBreakdown(
          slotPrices,
          TimeSlot.HOUR
      );

      // when & then
      assertThatThrownBy(() -> breakdown.getPriceAt(null))
          .isInstanceOf(NullPointerException.class);
    }
  }

  @Nested
  @DisplayName("불변성 테스트")
  class ImmutabilityTests {

    @Test
    @DisplayName("슬롯 가격 Map을 변경해도 원본은 영향을 받지 않는다")
    void slotPricesImmutable() {
      // given
      final Map<LocalDateTime, Money> slotPrices = new HashMap<>();
      slotPrices.put(LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000));

      final TimeSlotPriceBreakdown breakdown = new TimeSlotPriceBreakdown(
          slotPrices,
          TimeSlot.HOUR
      );

      // when
      slotPrices.put(LocalDateTime.of(2025, 11, 10, 11, 0), Money.of(20000));

      // then
      assertThat(breakdown.slotPrices()).hasSize(1);
      assertThat(breakdown.getTotalPrice()).isEqualTo(Money.of(10000));
    }

    @Test
    @DisplayName("반환된 슬롯 가격 Map을 변경하려고 하면 예외가 발생한다")
    void returnedMapIsUnmodifiable() {
      // given
      final Map<LocalDateTime, Money> slotPrices = Map.of(
          LocalDateTime.of(2025, 11, 10, 10, 0), Money.of(10000)
      );

      final TimeSlotPriceBreakdown breakdown = new TimeSlotPriceBreakdown(
          slotPrices,
          TimeSlot.HOUR
      );

      // when & then
      assertThatThrownBy(() ->
          breakdown.slotPrices().put(LocalDateTime.of(2025, 11, 10, 11, 0), Money.of(20000))
      ).isInstanceOf(UnsupportedOperationException.class);
    }
  }
}
