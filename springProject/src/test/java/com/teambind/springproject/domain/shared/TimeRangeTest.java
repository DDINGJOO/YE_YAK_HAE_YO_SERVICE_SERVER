package com.teambind.springproject.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TimeRange Value Object 테스트")
class TimeRangeTest {

  @Nested
  @DisplayName("생성 테스트")
  class CreationTests {

    @Test
    @DisplayName("유효한 시간 범위로 생성 성공")
    void createValidTimeRange() {
      // given
      final LocalTime startTime = LocalTime.of(9, 0);
      final LocalTime endTime = LocalTime.of(18, 0);

      // when
      final TimeRange timeRange = TimeRange.of(startTime, endTime);

      // then
      assertThat(timeRange.getStartTime()).isEqualTo(startTime);
      assertThat(timeRange.getEndTime()).isEqualTo(endTime);
    }

    @Test
    @DisplayName("시작 시간이 null일 경우 예외 발생")
    void throwExceptionWhenStartTimeIsNull() {
      // given
      final LocalTime endTime = LocalTime.of(18, 0);

      // when & then
      assertThatThrownBy(() -> TimeRange.of(null, endTime))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Start time cannot be null");
    }

    @Test
    @DisplayName("종료 시간이 null일 경우 예외 발생")
    void throwExceptionWhenEndTimeIsNull() {
      // given
      final LocalTime startTime = LocalTime.of(9, 0);

      // when & then
      assertThatThrownBy(() -> TimeRange.of(startTime, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("End time cannot be null");
    }

    @Test
    @DisplayName("시작 시간이 종료 시간보다 늦을 경우 예외 발생")
    void throwExceptionWhenStartTimeIsAfterEndTime() {
      // given
      final LocalTime startTime = LocalTime.of(18, 0);
      final LocalTime endTime = LocalTime.of(9, 0);

      // when & then
      assertThatThrownBy(() -> TimeRange.of(startTime, endTime))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Start time must be before end time");
    }

    @Test
    @DisplayName("시작 시간과 종료 시간이 같을 경우 예외 발생")
    void throwExceptionWhenStartTimeEqualsEndTime() {
      // given
      final LocalTime time = LocalTime.of(9, 0);

      // when & then
      assertThatThrownBy(() -> TimeRange.of(time, time))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Start time must be before end time");
    }
  }

  @Nested
  @DisplayName("슬롯 계산 테스트")
  class CalculateSlotsTests {

    @Test
    @DisplayName("1시간 단위로 9시간 범위의 슬롯 계산")
    void calculateHourSlotsFor9Hours() {
      // given
      final TimeRange timeRange = TimeRange.of(
          LocalTime.of(9, 0),
          LocalTime.of(18, 0)
      );

      // when
      final int slots = timeRange.calculateSlots(TimeSlot.HOUR);

      // then
      assertThat(slots).isEqualTo(9);
    }

    @Test
    @DisplayName("30분 단위로 9시간 범위의 슬롯 계산")
    void calculateHalfHourSlotsFor9Hours() {
      // given
      final TimeRange timeRange = TimeRange.of(
          LocalTime.of(9, 0),
          LocalTime.of(18, 0)
      );

      // when
      final int slots = timeRange.calculateSlots(TimeSlot.HALFHOUR);

      // then
      assertThat(slots).isEqualTo(18);
    }

    @Test
    @DisplayName("1시간 단위로 90분 범위의 슬롯 계산 (소수점 버림)")
    void calculateHourSlotsFor90Minutes() {
      // given
      final TimeRange timeRange = TimeRange.of(
          LocalTime.of(9, 0),
          LocalTime.of(10, 30)
      );

      // when
      final int slots = timeRange.calculateSlots(TimeSlot.HOUR);

      // then
      assertThat(slots).isEqualTo(1);
    }

    @Test
    @DisplayName("30분 단위로 45분 범위의 슬롯 계산 (소수점 버림)")
    void calculateHalfHourSlotsFor45Minutes() {
      // given
      final TimeRange timeRange = TimeRange.of(
          LocalTime.of(9, 0),
          LocalTime.of(9, 45)
      );

      // when
      final int slots = timeRange.calculateSlots(TimeSlot.HALFHOUR);

      // then
      assertThat(slots).isEqualTo(1);
    }

    @Test
    @DisplayName("슬롯 계산 시 null TimeSlot 예외 발생")
    void throwExceptionWhenTimeSlotIsNull() {
      // given
      final TimeRange timeRange = TimeRange.of(
          LocalTime.of(9, 0),
          LocalTime.of(18, 0)
      );

      // when & then
      assertThatThrownBy(() -> timeRange.calculateSlots(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("TimeSlot cannot be null");
    }
  }

  @Nested
  @DisplayName("지속 시간 계산 테스트")
  class DurationTests {

    @Test
    @DisplayName("9시간 범위의 분 단위 지속 시간")
    void calculateDurationFor9Hours() {
      // given
      final TimeRange timeRange = TimeRange.of(
          LocalTime.of(9, 0),
          LocalTime.of(18, 0)
      );

      // when
      final long duration = timeRange.getDurationInMinutes();

      // then
      assertThat(duration).isEqualTo(540);
    }

    @Test
    @DisplayName("30분 범위의 분 단위 지속 시간")
    void calculateDurationFor30Minutes() {
      // given
      final TimeRange timeRange = TimeRange.of(
          LocalTime.of(9, 0),
          LocalTime.of(9, 30)
      );

      // when
      final long duration = timeRange.getDurationInMinutes();

      // then
      assertThat(duration).isEqualTo(30);
    }
  }

  @Nested
  @DisplayName("시간 포함 여부 테스트")
  class ContainsTests {

    @Test
    @DisplayName("범위 내 시간 포함 확인")
    void containsTimeWithinRange() {
      // given
      final TimeRange timeRange = TimeRange.of(
          LocalTime.of(9, 0),
          LocalTime.of(18, 0)
      );

      // when & then
      assertThat(timeRange.contains(LocalTime.of(12, 0))).isTrue();
      assertThat(timeRange.contains(LocalTime.of(9, 0))).isTrue();
    }

    @Test
    @DisplayName("범위 종료 시간은 포함하지 않음")
    void doesNotContainEndTime() {
      // given
      final TimeRange timeRange = TimeRange.of(
          LocalTime.of(9, 0),
          LocalTime.of(18, 0)
      );

      // when & then
      assertThat(timeRange.contains(LocalTime.of(18, 0))).isFalse();
    }

    @Test
    @DisplayName("범위 밖 시간 미포함 확인")
    void doesNotContainTimeOutsideRange() {
      // given
      final TimeRange timeRange = TimeRange.of(
          LocalTime.of(9, 0),
          LocalTime.of(18, 0)
      );

      // when & then
      assertThat(timeRange.contains(LocalTime.of(8, 59))).isFalse();
      assertThat(timeRange.contains(LocalTime.of(18, 1))).isFalse();
    }

    @Test
    @DisplayName("contains에 null 전달 시 예외 발생")
    void throwExceptionWhenContainsNull() {
      // given
      final TimeRange timeRange = TimeRange.of(
          LocalTime.of(9, 0),
          LocalTime.of(18, 0)
      );

      // when & then
      assertThatThrownBy(() -> timeRange.contains(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Time cannot be null");
    }
  }

  @Nested
  @DisplayName("시간 범위 겹침 테스트")
  class OverlapsTests {

    @Test
    @DisplayName("완전히 겹치는 범위")
    void overlapsCompletely() {
      // given
      final TimeRange range1 = TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0));
      final TimeRange range2 = TimeRange.of(LocalTime.of(10, 0), LocalTime.of(16, 0));

      // when & then
      assertThat(range1.overlaps(range2)).isTrue();
      assertThat(range2.overlaps(range1)).isTrue();
    }

    @Test
    @DisplayName("부분적으로 겹치는 범위 (앞부분)")
    void overlapsPartiallyAtStart() {
      // given
      final TimeRange range1 = TimeRange.of(LocalTime.of(9, 0), LocalTime.of(12, 0));
      final TimeRange range2 = TimeRange.of(LocalTime.of(11, 0), LocalTime.of(15, 0));

      // when & then
      assertThat(range1.overlaps(range2)).isTrue();
      assertThat(range2.overlaps(range1)).isTrue();
    }

    @Test
    @DisplayName("부분적으로 겹치는 범위 (뒷부분)")
    void overlapsPartiallyAtEnd() {
      // given
      final TimeRange range1 = TimeRange.of(LocalTime.of(12, 0), LocalTime.of(18, 0));
      final TimeRange range2 = TimeRange.of(LocalTime.of(9, 0), LocalTime.of(13, 0));

      // when & then
      assertThat(range1.overlaps(range2)).isTrue();
      assertThat(range2.overlaps(range1)).isTrue();
    }

    @Test
    @DisplayName("겹치지 않는 범위 (이전)")
    void doesNotOverlapBefore() {
      // given
      final TimeRange range1 = TimeRange.of(LocalTime.of(9, 0), LocalTime.of(12, 0));
      final TimeRange range2 = TimeRange.of(LocalTime.of(13, 0), LocalTime.of(18, 0));

      // when & then
      assertThat(range1.overlaps(range2)).isFalse();
      assertThat(range2.overlaps(range1)).isFalse();
    }

    @Test
    @DisplayName("경계가 맞닿은 범위는 겹치지 않음")
    void doesNotOverlapWhenBoundaryTouches() {
      // given
      final TimeRange range1 = TimeRange.of(LocalTime.of(9, 0), LocalTime.of(12, 0));
      final TimeRange range2 = TimeRange.of(LocalTime.of(12, 0), LocalTime.of(18, 0));

      // when & then
      assertThat(range1.overlaps(range2)).isFalse();
      assertThat(range2.overlaps(range1)).isFalse();
    }

    @Test
    @DisplayName("overlaps에 null 전달 시 예외 발생")
    void throwExceptionWhenOverlapsNull() {
      // given
      final TimeRange timeRange = TimeRange.of(
          LocalTime.of(9, 0),
          LocalTime.of(18, 0)
      );

      // when & then
      assertThatThrownBy(() -> timeRange.overlaps(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Other time range cannot be null");
    }
  }

  @Nested
  @DisplayName("equals 및 hashCode 테스트")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("동일한 시간 범위는 같음")
    void equalTimeRanges() {
      // given
      final TimeRange range1 = TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0));
      final TimeRange range2 = TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0));

      // when & then
      assertThat(range1).isEqualTo(range2);
      assertThat(range1.hashCode()).isEqualTo(range2.hashCode());
    }

    @Test
    @DisplayName("다른 시간 범위는 다름")
    void notEqualTimeRanges() {
      // given
      final TimeRange range1 = TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0));
      final TimeRange range2 = TimeRange.of(LocalTime.of(10, 0), LocalTime.of(18, 0));

      // when & then
      assertThat(range1).isNotEqualTo(range2);
    }

    @Test
    @DisplayName("동일 객체는 같음")
    void equalsSameObject() {
      // given
      final TimeRange timeRange = TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0));

      // when & then
      assertThat(timeRange).isEqualTo(timeRange);
    }

    @Test
    @DisplayName("null과 비교하면 다름")
    void notEqualsNull() {
      // given
      final TimeRange timeRange = TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0));

      // when & then
      assertThat(timeRange).isNotEqualTo(null);
    }

    @Test
    @DisplayName("다른 타입 객체와 비교하면 다름")
    void notEqualsDifferentType() {
      // given
      final TimeRange timeRange = TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0));
      final String other = "09:00-18:00";

      // when & then
      assertThat(timeRange).isNotEqualTo(other);
    }
  }

  @Nested
  @DisplayName("toString 테스트")
  class ToStringTests {

    @Test
    @DisplayName("toString 형식 확인")
    void toStringFormat() {
      // given
      final TimeRange timeRange = TimeRange.of(
          LocalTime.of(9, 0),
          LocalTime.of(18, 0)
      );

      // when
      final String result = timeRange.toString();

      // then
      assertThat(result).isEqualTo("09:00 - 18:00");
    }
  }
}
