package com.teambind.springproject.domain.pricingpolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teambind.springproject.domain.shared.DayOfWeek;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.TimeRange;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TimeRangePrice Record 테스트")
class TimeRangePriceTest {

  @Nested
  @DisplayName("생성 테스트")
  class CreationTests {

    @Test
    @DisplayName("유효한 값으로 생성 성공")
    void createValid() {
      // given
      final DayOfWeek dayOfWeek = DayOfWeek.MONDAY;
      final TimeRange timeRange = TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0));
      final Money price = Money.of(new BigDecimal("10000"));

      // when
      final TimeRangePrice timeRangePrice = new TimeRangePrice(dayOfWeek, timeRange, price);

      // then
      assertThat(timeRangePrice.dayOfWeek()).isEqualTo(dayOfWeek);
      assertThat(timeRangePrice.timeRange()).isEqualTo(timeRange);
      assertThat(timeRangePrice.pricePerSlot()).isEqualTo(price);
    }

    @Test
    @DisplayName("null dayOfWeek로 생성 시 예외 발생")
    void throwExceptionWhenDayOfWeekIsNull() {
      // given
      final TimeRange timeRange = TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0));
      final Money price = Money.of(new BigDecimal("10000"));

      // when & then
      assertThatThrownBy(() -> new TimeRangePrice(null, timeRange, price))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Day of week cannot be null");
    }

    @Test
    @DisplayName("null timeRange로 생성 시 예외 발생")
    void throwExceptionWhenTimeRangeIsNull() {
      // given
      final DayOfWeek dayOfWeek = DayOfWeek.MONDAY;
      final Money price = Money.of(new BigDecimal("10000"));

      // when & then
      assertThatThrownBy(() -> new TimeRangePrice(dayOfWeek, null, price))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Time range cannot be null");
    }

    @Test
    @DisplayName("null pricePerSlot로 생성 시 예외 발생")
    void throwExceptionWhenPriceIsNull() {
      // given
      final DayOfWeek dayOfWeek = DayOfWeek.MONDAY;
      final TimeRange timeRange = TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0));

      // when & then
      assertThatThrownBy(() -> new TimeRangePrice(dayOfWeek, timeRange, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Price per slot cannot be null");
    }
  }

  @Nested
  @DisplayName("겹침 판단 테스트")
  class OverlapsTests {

    @Test
    @DisplayName("같은 요일의 겹치는 시간 범위는 true")
    void overlapsWhenSameDayAndOverlappingTime() {
      // given
      final TimeRangePrice price1 = new TimeRangePrice(
          DayOfWeek.MONDAY,
          TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
          Money.of(new BigDecimal("10000"))
      );
      final TimeRangePrice price2 = new TimeRangePrice(
          DayOfWeek.MONDAY,
          TimeRange.of(LocalTime.of(16, 0), LocalTime.of(22, 0)),
          Money.of(new BigDecimal("15000"))
      );

      // when & then
      assertThat(price1.overlaps(price2)).isTrue();
      assertThat(price2.overlaps(price1)).isTrue();
    }

    @Test
    @DisplayName("같은 요일이지만 겹치지 않는 시간 범위는 false")
    void notOverlapsWhenSameDayButNotOverlappingTime() {
      // given
      final TimeRangePrice price1 = new TimeRangePrice(
          DayOfWeek.MONDAY,
          TimeRange.of(LocalTime.of(9, 0), LocalTime.of(12, 0)),
          Money.of(new BigDecimal("10000"))
      );
      final TimeRangePrice price2 = new TimeRangePrice(
          DayOfWeek.MONDAY,
          TimeRange.of(LocalTime.of(13, 0), LocalTime.of(18, 0)),
          Money.of(new BigDecimal("15000"))
      );

      // when & then
      assertThat(price1.overlaps(price2)).isFalse();
      assertThat(price2.overlaps(price1)).isFalse();
    }

    @Test
    @DisplayName("다른 요일은 시간이 겹쳐도 false")
    void notOverlapsWhenDifferentDayEvenIfOverlappingTime() {
      // given
      final TimeRangePrice price1 = new TimeRangePrice(
          DayOfWeek.MONDAY,
          TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
          Money.of(new BigDecimal("10000"))
      );
      final TimeRangePrice price2 = new TimeRangePrice(
          DayOfWeek.TUESDAY,
          TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
          Money.of(new BigDecimal("15000"))
      );

      // when & then
      assertThat(price1.overlaps(price2)).isFalse();
      assertThat(price2.overlaps(price1)).isFalse();
    }

    @Test
    @DisplayName("null과 비교 시 예외 발생")
    void throwExceptionWhenComparingWithNull() {
      // given
      final TimeRangePrice price = new TimeRangePrice(
          DayOfWeek.MONDAY,
          TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
          Money.of(new BigDecimal("10000"))
      );

      // when & then
      assertThatThrownBy(() -> price.overlaps(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Other TimeRangePrice cannot be null");
    }
  }

  @Nested
  @DisplayName("equals 및 hashCode 테스트")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("모든 필드가 같으면 동일")
    void equalWhenAllFieldsAreSame() {
      // given
      final DayOfWeek dayOfWeek = DayOfWeek.MONDAY;
      final TimeRange timeRange = TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0));
      final Money price = Money.of(new BigDecimal("10000"));

      final TimeRangePrice price1 = new TimeRangePrice(dayOfWeek, timeRange, price);
      final TimeRangePrice price2 = new TimeRangePrice(dayOfWeek, timeRange, price);

      // when & then
      assertThat(price1).isEqualTo(price2);
      assertThat(price1.hashCode()).isEqualTo(price2.hashCode());
    }

    @Test
    @DisplayName("필드가 다르면 다름")
    void notEqualWhenFieldsAreDifferent() {
      // given
      final TimeRangePrice price1 = new TimeRangePrice(
          DayOfWeek.MONDAY,
          TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
          Money.of(new BigDecimal("10000"))
      );
      final TimeRangePrice price2 = new TimeRangePrice(
          DayOfWeek.TUESDAY,
          TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
          Money.of(new BigDecimal("10000"))
      );

      // when & then
      assertThat(price1).isNotEqualTo(price2);
    }
  }
}
