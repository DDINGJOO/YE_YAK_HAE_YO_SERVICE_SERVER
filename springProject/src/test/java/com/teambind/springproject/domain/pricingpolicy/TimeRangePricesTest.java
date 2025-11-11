package com.teambind.springproject.domain.pricingpolicy;

import com.teambind.springproject.domain.shared.DayOfWeek;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.TimeRange;
import com.teambind.springproject.domain.shared.TimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TimeRangePrices Value Object 테스트")
class TimeRangePricesTest {
	
	@Nested
	@DisplayName("생성 테스트")
	class CreationTests {
		
		@Test
		@DisplayName("빈 리스트로 생성 성공")
		void createEmpty() {
			// when
			final TimeRangePrices prices = TimeRangePrices.empty();
			
			// then
			assertThat(prices.isEmpty()).isTrue();
			assertThat(prices.size()).isZero();
		}
		
		@Test
		@DisplayName("유효한 리스트로 생성 성공")
		void createWithValidList() {
			// given
			final List<TimeRangePrice> priceList = List.of(
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
							Money.of(new BigDecimal("10000"))
					),
					new TimeRangePrice(
							DayOfWeek.TUESDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
							Money.of(new BigDecimal("12000"))
					)
			);
			
			// when
			final TimeRangePrices prices = TimeRangePrices.of(priceList);
			
			// then
			assertThat(prices.isEmpty()).isFalse();
			assertThat(prices.size()).isEqualTo(2);
			assertThat(prices.getPrices()).hasSize(2);
		}
		
		@Test
		@DisplayName("null 리스트로 생성 시 예외 발생")
		void throwExceptionWhenListIsNull() {
			// when & then
			assertThatThrownBy(() -> TimeRangePrices.of(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Prices cannot be null");
		}
	}
	
	@Nested
	@DisplayName("시간대 중복 검증 테스트")
	class OverlapValidationTests {
		
		@Test
		@DisplayName("같은 요일에 시간대가 겹치면 예외 발생")
		void throwExceptionWhenOverlappingOnSameDay() {
			// given
			final List<TimeRangePrice> priceList = List.of(
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
							Money.of(new BigDecimal("10000"))
					),
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(16, 0), LocalTime.of(22, 0)),
							Money.of(new BigDecimal("15000"))
					)
			);
			
			// when & then
			assertThatThrownBy(() -> TimeRangePrices.of(priceList))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("Time range prices cannot overlap");
		}
		
		@Test
		@DisplayName("다른 요일은 시간대가 같아도 성공")
		void allowSameTimeRangeOnDifferentDays() {
			// given
			final List<TimeRangePrice> priceList = List.of(
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
							Money.of(new BigDecimal("10000"))
					),
					new TimeRangePrice(
							DayOfWeek.TUESDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
							Money.of(new BigDecimal("12000"))
					)
			);
			
			// when
			final TimeRangePrices prices = TimeRangePrices.of(priceList);
			
			// then
			assertThat(prices.size()).isEqualTo(2);
		}
		
		@Test
		@DisplayName("같은 요일에 경계가 맞닿은 시간대는 허용")
		void allowAdjacentTimeRangesOnSameDay() {
			// given
			final List<TimeRangePrice> priceList = List.of(
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(12, 0)),
							Money.of(new BigDecimal("10000"))
					),
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(12, 0), LocalTime.of(18, 0)),
							Money.of(new BigDecimal("15000"))
					)
			);
			
			// when
			final TimeRangePrices prices = TimeRangePrices.of(priceList);
			
			// then
			assertThat(prices.size()).isEqualTo(2);
		}
	}
	
	@Nested
	@DisplayName("슬롯 가격 찾기 테스트")
	class FindPriceForSlotTests {
		
		@Test
		@DisplayName("해당하는 시간대 가격 찾기 성공")
		void findPriceForMatchingSlot() {
			// given
			final Money expectedPrice = Money.of(new BigDecimal("15000"));
			final TimeRangePrices prices = TimeRangePrices.of(List.of(
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
							expectedPrice
					)
			));
			
			// when
			final var result = prices.findPriceForSlot(DayOfWeek.MONDAY, LocalTime.of(12, 0));
			
			// then
			assertThat(result).isPresent();
			assertThat(result.get()).isEqualTo(expectedPrice);
		}
		
		@Test
		@DisplayName("해당하는 시간대가 없으면 Optional.empty")
		void returnEmptyWhenNoMatchingSlot() {
			// given
			final TimeRangePrices prices = TimeRangePrices.of(List.of(
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
							Money.of(new BigDecimal("15000"))
					)
			));
			
			// when
			final var result = prices.findPriceForSlot(DayOfWeek.MONDAY, LocalTime.of(20, 0));
			
			// then
			assertThat(result).isEmpty();
		}
		
		@Test
		@DisplayName("다른 요일의 시간은 찾지 못함")
		void returnEmptyWhenDifferentDay() {
			// given
			final TimeRangePrices prices = TimeRangePrices.of(List.of(
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
							Money.of(new BigDecimal("15000"))
					)
			));
			
			// when
			final var result = prices.findPriceForSlot(DayOfWeek.TUESDAY, LocalTime.of(12, 0));
			
			// then
			assertThat(result).isEmpty();
		}
		
		@Test
		@DisplayName("null dayOfWeek로 찾기 시 예외 발생")
		void throwExceptionWhenDayOfWeekIsNull() {
			// given
			final TimeRangePrices prices = TimeRangePrices.empty();
			
			// when & then
			assertThatThrownBy(() -> prices.findPriceForSlot(null, LocalTime.of(12, 0)))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Day of week cannot be null");
		}
		
		@Test
		@DisplayName("null time으로 찾기 시 예외 발생")
		void throwExceptionWhenTimeIsNull() {
			// given
			final TimeRangePrices prices = TimeRangePrices.empty();
			
			// when & then
			assertThatThrownBy(() -> prices.findPriceForSlot(DayOfWeek.MONDAY, null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Time cannot be null");
		}
	}
	
	@Nested
	@DisplayName("총 가격 계산 테스트")
	class CalculateTotalTests {
		
		@Test
		@DisplayName("모든 시간대 가격의 총합 계산")
		void calculateTotalForAllTimeRanges() {
			// given
			final TimeRangePrices prices = TimeRangePrices.of(List.of(
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(12, 0)),  // 3시간 = 3슬롯
							Money.of(new BigDecimal("10000"))
					),
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(13, 0), LocalTime.of(18, 0)),  // 5시간 = 5슬롯
							Money.of(new BigDecimal("15000"))
					)
			));
			
			// when
			final Money total = prices.calculateTotal(TimeSlot.HOUR);
			
			// then
			// 3 * 10000 + 5 * 15000 = 30000 + 75000 = 105000
			assertThat(total.getAmount()).isEqualByComparingTo(new BigDecimal("105000.00"));
		}
		
		@Test
		@DisplayName("30분 단위로 총합 계산")
		void calculateTotalForHalfHourSlots() {
			// given
			final TimeRangePrices prices = TimeRangePrices.of(List.of(
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(10, 0)),  // 1시간 = 2슬롯 (30분 단위)
							Money.of(new BigDecimal("5000"))
					)
			));
			
			// when
			final Money total = prices.calculateTotal(TimeSlot.HALFHOUR);
			
			// then
			// 2 * 5000 = 10000
			assertThat(total.getAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
		}
		
		@Test
		@DisplayName("빈 리스트는 0 반환")
		void returnZeroForEmptyList() {
			// given
			final TimeRangePrices prices = TimeRangePrices.empty();
			
			// when
			final Money total = prices.calculateTotal(TimeSlot.HOUR);
			
			// then
			assertThat(total.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
		}
		
		@Test
		@DisplayName("null TimeSlot으로 계산 시 예외 발생")
		void throwExceptionWhenTimeSlotIsNull() {
			// given
			final TimeRangePrices prices = TimeRangePrices.empty();
			
			// when & then
			assertThatThrownBy(() -> prices.calculateTotal(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("TimeSlot cannot be null");
		}
	}
	
	@Nested
	@DisplayName("equals 및 hashCode 테스트")
	class EqualsAndHashCodeTests {
		
		@Test
		@DisplayName("같은 가격 목록은 동일")
		void equalWhenSamePrices() {
			// given
			final List<TimeRangePrice> priceList = List.of(
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
							Money.of(new BigDecimal("10000"))
					)
			);
			final TimeRangePrices prices1 = TimeRangePrices.of(priceList);
			final TimeRangePrices prices2 = TimeRangePrices.of(priceList);
			
			// when & then
			assertThat(prices1).isEqualTo(prices2);
			assertThat(prices1.hashCode()).isEqualTo(prices2.hashCode());
		}
		
		@Test
		@DisplayName("다른 가격 목록은 다름")
		void notEqualWhenDifferentPrices() {
			// given
			final TimeRangePrices prices1 = TimeRangePrices.of(List.of(
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
							Money.of(new BigDecimal("10000"))
					)
			));
			final TimeRangePrices prices2 = TimeRangePrices.of(List.of(
					new TimeRangePrice(
							DayOfWeek.TUESDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
							Money.of(new BigDecimal("10000"))
					)
			));
			
			// when & then
			assertThat(prices1).isNotEqualTo(prices2);
		}
	}
	
	@Nested
	@DisplayName("불변성 테스트")
	class ImmutabilityTests {
		
		@Test
		@DisplayName("getPrices()로 반환된 리스트는 수정 불가")
		void returnedListIsUnmodifiable() {
			// given
			final TimeRangePrices prices = TimeRangePrices.of(List.of(
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
							Money.of(new BigDecimal("10000"))
					)
			));
			
			// when
			final List<TimeRangePrice> priceList = prices.getPrices();
			
			// then
			assertThatThrownBy(() -> priceList.add(
					new TimeRangePrice(
							DayOfWeek.TUESDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
							Money.of(new BigDecimal("12000"))
					)
			)).isInstanceOf(UnsupportedOperationException.class);
		}
		
		@Test
		@DisplayName("생성 후 원본 리스트 변경해도 영향 없음")
		void notAffectedByOriginalListModification() {
			// given
			final List<TimeRangePrice> originalList = new ArrayList<>();
			originalList.add(new TimeRangePrice(
					DayOfWeek.MONDAY,
					TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
					Money.of(new BigDecimal("10000"))
			));
			
			final TimeRangePrices prices = TimeRangePrices.of(originalList);
			final int originalSize = prices.size();
			
			// when
			originalList.add(new TimeRangePrice(
					DayOfWeek.TUESDAY,
					TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
					Money.of(new BigDecimal("12000"))
			));
			
			// then
			assertThat(prices.size()).isEqualTo(originalSize);
		}
	}
}
