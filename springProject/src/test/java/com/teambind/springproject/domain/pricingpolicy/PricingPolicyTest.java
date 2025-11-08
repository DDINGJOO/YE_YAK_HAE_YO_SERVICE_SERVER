package com.teambind.springproject.domain.pricingpolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teambind.springproject.domain.pricingpolicy.PricingPolicy.PriceBreakdown;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy.SlotPrice;
import com.teambind.springproject.domain.shared.DayOfWeek;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeRange;
import com.teambind.springproject.domain.shared.TimeSlot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PricingPolicy Aggregate 테스트")
class PricingPolicyTest {

  @Nested
  @DisplayName("생성 테스트")
  class CreationTests {

    @Test
    @DisplayName("기본 가격만으로 생성 성공")
    void createWithDefaultPriceOnly() {
      // given
      final RoomId roomId = RoomId.of(1L);
      final PlaceId placeId = PlaceId.of(1L);
      final TimeSlot timeSlot = TimeSlot.HOUR;
      final Money defaultPrice = Money.of(new BigDecimal("10000"));

      // when
      final PricingPolicy policy = PricingPolicy.create(roomId, placeId, timeSlot, defaultPrice);

      // then
      assertThat(policy.getRoomId()).isEqualTo(roomId);
      assertThat(policy.getPlaceId()).isEqualTo(placeId);
      assertThat(policy.getTimeSlot()).isEqualTo(timeSlot);
      assertThat(policy.getDefaultPrice()).isEqualTo(defaultPrice);
      assertThat(policy.getTimeRangePrices().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("시간대별 가격과 함께 생성 성공")
    void createWithTimeRangePrices() {
      // given
      final RoomId roomId = RoomId.of(1L);
      final PlaceId placeId = PlaceId.of(1L);
      final TimeSlot timeSlot = TimeSlot.HOUR;
      final Money defaultPrice = Money.of(new BigDecimal("10000"));
      final TimeRangePrices timeRangePrices = TimeRangePrices.of(List.of(
          new TimeRangePrice(
              DayOfWeek.MONDAY,
              TimeRange.of(LocalTime.of(18, 0), LocalTime.of(22, 0)),
              Money.of(new BigDecimal("15000"))
          )
      ));

      // when
      final PricingPolicy policy = PricingPolicy.createWithTimeRangePrices(
          roomId, placeId, timeSlot, defaultPrice, timeRangePrices);

      // then
      assertThat(policy.getTimeRangePrices().isEmpty()).isFalse();
      assertThat(policy.getTimeRangePrices().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("null roomId로 생성 시 예외 발생")
    void throwExceptionWhenRoomIdIsNull() {
      // when & then
      assertThatThrownBy(() -> PricingPolicy.create(
          null,
          PlaceId.of(1L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("10000"))
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Room ID cannot be null");
    }

    @Test
    @DisplayName("null placeId로 생성 시 예외 발생")
    void throwExceptionWhenPlaceIdIsNull() {
      // when & then
      assertThatThrownBy(() -> PricingPolicy.create(
          RoomId.of(1L),
          null,
          TimeSlot.HOUR,
          Money.of(new BigDecimal("10000"))
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Place ID cannot be null");
    }

    @Test
    @DisplayName("null timeSlot로 생성 시 예외 발생")
    void throwExceptionWhenTimeSlotIsNull() {
      // when & then
      assertThatThrownBy(() -> PricingPolicy.create(
          RoomId.of(1L),
          PlaceId.of(1L),
          null,
          Money.of(new BigDecimal("10000"))
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("TimeSlot cannot be null");
    }

    @Test
    @DisplayName("null defaultPrice로 생성 시 예외 발생")
    void throwExceptionWhenDefaultPriceIsNull() {
      // when & then
      assertThatThrownBy(() -> PricingPolicy.create(
          RoomId.of(1L),
          PlaceId.of(1L),
          TimeSlot.HOUR,
          null
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Default price cannot be null");
    }
  }

  @Nested
  @DisplayName("기본 가격 변경 테스트")
  class UpdateDefaultPriceTests {

    @Test
    @DisplayName("기본 가격 변경 성공")
    void updateDefaultPrice() {
      // given
      final PricingPolicy policy = PricingPolicy.create(
          RoomId.of(1L),
          PlaceId.of(1L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("10000"))
      );
      final Money newPrice = Money.of(new BigDecimal("12000"));

      // when
      policy.updateDefaultPrice(newPrice);

      // then
      assertThat(policy.getDefaultPrice()).isEqualTo(newPrice);
    }

    @Test
    @DisplayName("null로 변경 시 예외 발생")
    void throwExceptionWhenUpdatingWithNull() {
      // given
      final PricingPolicy policy = PricingPolicy.create(
          RoomId.of(1L),
          PlaceId.of(1L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("10000"))
      );

      // when & then
      assertThatThrownBy(() -> policy.updateDefaultPrice(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Default price cannot be null");
    }
  }

  @Nested
  @DisplayName("시간대별 가격 재설정 테스트")
  class ResetPricesTests {

    @Test
    @DisplayName("시간대별 가격 재설정 성공")
    void resetPrices() {
      // given
      final PricingPolicy policy = PricingPolicy.create(
          RoomId.of(1L),
          PlaceId.of(1L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("10000"))
      );
      final TimeRangePrices newPrices = TimeRangePrices.of(List.of(
          new TimeRangePrice(
              DayOfWeek.SATURDAY,
              TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
              Money.of(new BigDecimal("20000"))
          )
      ));

      // when
      policy.resetPrices(newPrices);

      // then
      assertThat(policy.getTimeRangePrices()).isEqualTo(newPrices);
      assertThat(policy.getTimeRangePrices().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("빈 가격으로 재설정 가능")
    void resetToEmptyPrices() {
      // given
      final PricingPolicy policy = PricingPolicy.createWithTimeRangePrices(
          RoomId.of(1L),
          PlaceId.of(1L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("10000")),
          TimeRangePrices.of(List.of(
              new TimeRangePrice(
                  DayOfWeek.MONDAY,
                  TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
                  Money.of(new BigDecimal("15000"))
              )
          ))
      );

      // when
      policy.resetPrices(TimeRangePrices.empty());

      // then
      assertThat(policy.getTimeRangePrices().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("null로 재설정 시 예외 발생")
    void throwExceptionWhenResetWithNull() {
      // given
      final PricingPolicy policy = PricingPolicy.create(
          RoomId.of(1L),
          PlaceId.of(1L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("10000"))
      );

      // when & then
      assertThatThrownBy(() -> policy.resetPrices(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Time range prices cannot be null");
    }
  }

  @Nested
  @DisplayName("가격 내역 계산 테스트")
  class CalculatePriceBreakdownTests {

    @Test
    @DisplayName("기본 가격만으로 계산")
    void calculateWithDefaultPriceOnly() {
      // given
      final Money defaultPrice = Money.of(new BigDecimal("10000"));
      final PricingPolicy policy = PricingPolicy.create(
          RoomId.of(1L),
          PlaceId.of(1L),
          TimeSlot.HOUR,
          defaultPrice
      );

      final LocalDateTime start = LocalDateTime.of(2024, 1, 1, 9, 0);  // Monday
      final LocalDateTime end = LocalDateTime.of(2024, 1, 1, 12, 0);   // 3 hours

      // when
      final PriceBreakdown breakdown = policy.calculatePriceBreakdown(start, end);

      // then
      assertThat(breakdown.getSlotCount()).isEqualTo(3);
      assertThat(breakdown.getTotalPrice().getAmount())
          .isEqualByComparingTo(new BigDecimal("30000.00"));
    }

    @Test
    @DisplayName("시간대별 가격 적용하여 계산")
    void calculateWithTimeRangePrices() {
      // given
      final Money defaultPrice = Money.of(new BigDecimal("10000"));
      final Money peakPrice = Money.of(new BigDecimal("15000"));
      final PricingPolicy policy = PricingPolicy.createWithTimeRangePrices(
          RoomId.of(1L),
          PlaceId.of(1L),
          TimeSlot.HOUR,
          defaultPrice,
          TimeRangePrices.of(List.of(
              new TimeRangePrice(
                  DayOfWeek.MONDAY,
                  TimeRange.of(LocalTime.of(18, 0), LocalTime.of(22, 0)),
                  peakPrice
              )
          ))
      );

      // Monday 17:00 ~ 20:00 (17시, 18시, 19시 = 3슬롯)
      // 17시: 기본가격(10000), 18-19시: 피크가격(15000 x 2)
      final LocalDateTime start = LocalDateTime.of(2024, 1, 1, 17, 0);
      final LocalDateTime end = LocalDateTime.of(2024, 1, 1, 20, 0);

      // when
      final PriceBreakdown breakdown = policy.calculatePriceBreakdown(start, end);

      // then
      assertThat(breakdown.getSlotCount()).isEqualTo(3);
      assertThat(breakdown.getTotalPrice().getAmount())
          .isEqualByComparingTo(new BigDecimal("40000.00"));  // 10000 + 15000 + 15000
    }

    @Test
    @DisplayName("30분 단위 TimeSlot으로 계산")
    void calculateWithHalfHourSlots() {
      // given
      final Money defaultPrice = Money.of(new BigDecimal("5000"));
      final PricingPolicy policy = PricingPolicy.create(
          RoomId.of(1L),
          PlaceId.of(1L),
          TimeSlot.HALFHOUR,
          defaultPrice
      );

      final LocalDateTime start = LocalDateTime.of(2024, 1, 1, 9, 0);
      final LocalDateTime end = LocalDateTime.of(2024, 1, 1, 10, 0);  // 1 hour = 2 slots

      // when
      final PriceBreakdown breakdown = policy.calculatePriceBreakdown(start, end);

      // then
      assertThat(breakdown.getSlotCount()).isEqualTo(2);
      assertThat(breakdown.getTotalPrice().getAmount())
          .isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("null startDateTime으로 계산 시 예외 발생")
    void throwExceptionWhenStartDateTimeIsNull() {
      // given
      final PricingPolicy policy = PricingPolicy.create(
          RoomId.of(1L),
          PlaceId.of(1L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("10000"))
      );

      // when & then
      assertThatThrownBy(() -> policy.calculatePriceBreakdown(
          null,
          LocalDateTime.now()
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Start date time cannot be null");
    }

    @Test
    @DisplayName("null endDateTime으로 계산 시 예외 발생")
    void throwExceptionWhenEndDateTimeIsNull() {
      // given
      final PricingPolicy policy = PricingPolicy.create(
          RoomId.of(1L),
          PlaceId.of(1L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("10000"))
      );

      // when & then
      assertThatThrownBy(() -> policy.calculatePriceBreakdown(
          LocalDateTime.now(),
          null
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("End date time cannot be null");
    }

    @Test
    @DisplayName("시작 시간이 종료 시간보다 늦으면 예외 발생")
    void throwExceptionWhenStartIsAfterEnd() {
      // given
      final PricingPolicy policy = PricingPolicy.create(
          RoomId.of(1L),
          PlaceId.of(1L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("10000"))
      );

      final LocalDateTime start = LocalDateTime.of(2024, 1, 1, 12, 0);
      final LocalDateTime end = LocalDateTime.of(2024, 1, 1, 9, 0);

      // when & then
      assertThatThrownBy(() -> policy.calculatePriceBreakdown(start, end))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Start date time must be before end date time");
    }

    @Test
    @DisplayName("시작 시간과 종료 시간이 같으면 예외 발생")
    void throwExceptionWhenStartEqualsEnd() {
      // given
      final PricingPolicy policy = PricingPolicy.create(
          RoomId.of(1L),
          PlaceId.of(1L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("10000"))
      );

      final LocalDateTime time = LocalDateTime.of(2024, 1, 1, 12, 0);

      // when & then
      assertThatThrownBy(() -> policy.calculatePriceBreakdown(time, time))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Start date time must be before end date time");
    }
  }

  @Nested
  @DisplayName("PriceBreakdown Value Object 테스트")
  class PriceBreakdownTests {

    @Test
    @DisplayName("PriceBreakdown 생성 및 총 가격 계산")
    void createAndCalculateTotal() {
      // given
      final List<SlotPrice> slotPrices = List.of(
          new SlotPrice(
              LocalDateTime.of(2024, 1, 1, 9, 0),
              Money.of(new BigDecimal("10000"))
          ),
          new SlotPrice(
              LocalDateTime.of(2024, 1, 1, 10, 0),
              Money.of(new BigDecimal("15000"))
          )
      );

      // when
      final PriceBreakdown breakdown = PriceBreakdown.of(slotPrices);

      // then
      assertThat(breakdown.getSlotCount()).isEqualTo(2);
      assertThat(breakdown.getTotalPrice().getAmount())
          .isEqualByComparingTo(new BigDecimal("25000.00"));
    }

    @Test
    @DisplayName("null slotPrices로 생성 시 예외 발생")
    void throwExceptionWhenSlotPricesIsNull() {
      // when & then
      assertThatThrownBy(() -> PriceBreakdown.of(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Slot prices cannot be null or empty");
    }

    @Test
    @DisplayName("빈 slotPrices로 생성 시 예외 발생")
    void throwExceptionWhenSlotPricesIsEmpty() {
      // when & then
      assertThatThrownBy(() -> PriceBreakdown.of(List.of()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Slot prices cannot be null or empty");
    }
  }

  @Nested
  @DisplayName("SlotPrice Record 테스트")
  class SlotPriceTests {

    @Test
    @DisplayName("SlotPrice 생성 성공")
    void createSlotPrice() {
      // given
      final LocalDateTime slotTime = LocalDateTime.of(2024, 1, 1, 9, 0);
      final Money price = Money.of(new BigDecimal("10000"));

      // when
      final SlotPrice slotPrice = new SlotPrice(slotTime, price);

      // then
      assertThat(slotPrice.slotTime()).isEqualTo(slotTime);
      assertThat(slotPrice.price()).isEqualTo(price);
    }

    @Test
    @DisplayName("null slotTime으로 생성 시 예외 발생")
    void throwExceptionWhenSlotTimeIsNull() {
      // when & then
      assertThatThrownBy(() -> new SlotPrice(null, Money.of(new BigDecimal("10000"))))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Slot time cannot be null");
    }

    @Test
    @DisplayName("null price로 생성 시 예외 발생")
    void throwExceptionWhenPriceIsNull() {
      // when & then
      assertThatThrownBy(() -> new SlotPrice(LocalDateTime.now(), null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Price cannot be null");
    }
  }

  @Nested
  @DisplayName("equals 및 hashCode 테스트")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("같은 RoomId를 가진 정책은 동일")
    void equalWhenSameRoomId() {
      // given
      final RoomId roomId = RoomId.of(1L);
      final PricingPolicy policy1 = PricingPolicy.create(
          roomId,
          PlaceId.of(1L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("10000"))
      );
      final PricingPolicy policy2 = PricingPolicy.create(
          roomId,
          PlaceId.of(2L),  // PlaceId가 달라도
          TimeSlot.HALFHOUR,  // TimeSlot이 달라도
          Money.of(new BigDecimal("15000"))  // 가격이 달라도
      );

      // when & then
      assertThat(policy1).isEqualTo(policy2);
      assertThat(policy1.hashCode()).isEqualTo(policy2.hashCode());
    }

    @Test
    @DisplayName("다른 RoomId를 가진 정책은 다름")
    void notEqualWhenDifferentRoomId() {
      // given
      final PricingPolicy policy1 = PricingPolicy.create(
          RoomId.of(1L),
          PlaceId.of(1L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("10000"))
      );
      final PricingPolicy policy2 = PricingPolicy.create(
          RoomId.of(2L),
          PlaceId.of(1L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("10000"))
      );

      // when & then
      assertThat(policy1).isNotEqualTo(policy2);
    }
  }
}
