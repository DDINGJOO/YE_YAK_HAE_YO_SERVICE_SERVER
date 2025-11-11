package com.teambind.springproject.domain.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DayOfWeek Enum 테스트")
class DayOfWeekTest {
	
	@Nested
	@DisplayName("Enum 값 테스트")
	class EnumValuesTests {
		
		@Test
		@DisplayName("모든 요일 값 확인")
		void allDayOfWeekValues() {
			// when
			final DayOfWeek[] values = DayOfWeek.values();
			
			// then
			assertThat(values).hasSize(7);
			assertThat(values).containsExactly(
					DayOfWeek.MONDAY,
					DayOfWeek.TUESDAY,
					DayOfWeek.WEDNESDAY,
					DayOfWeek.THURSDAY,
					DayOfWeek.FRIDAY,
					DayOfWeek.SATURDAY,
					DayOfWeek.SUNDAY
			);
		}
		
		@Test
		@DisplayName("valueOf 메서드 확인")
		void valueOfMethod() {
			// when & then
			assertThat(DayOfWeek.valueOf("MONDAY")).isEqualTo(DayOfWeek.MONDAY);
			assertThat(DayOfWeek.valueOf("SUNDAY")).isEqualTo(DayOfWeek.SUNDAY);
		}
	}
	
	@Nested
	@DisplayName("한글 이름 테스트")
	class KoreanNameTests {
		
		@Test
		@DisplayName("월요일 한글 이름")
		void mondayKoreanName() {
			// when & then
			assertThat(DayOfWeek.MONDAY.getKoreanName()).isEqualTo("월요일");
		}
		
		@Test
		@DisplayName("화요일 한글 이름")
		void tuesdayKoreanName() {
			// when & then
			assertThat(DayOfWeek.TUESDAY.getKoreanName()).isEqualTo("화요일");
		}
		
		@Test
		@DisplayName("수요일 한글 이름")
		void wednesdayKoreanName() {
			// when & then
			assertThat(DayOfWeek.WEDNESDAY.getKoreanName()).isEqualTo("수요일");
		}
		
		@Test
		@DisplayName("목요일 한글 이름")
		void thursdayKoreanName() {
			// when & then
			assertThat(DayOfWeek.THURSDAY.getKoreanName()).isEqualTo("목요일");
		}
		
		@Test
		@DisplayName("금요일 한글 이름")
		void fridayKoreanName() {
			// when & then
			assertThat(DayOfWeek.FRIDAY.getKoreanName()).isEqualTo("금요일");
		}
		
		@Test
		@DisplayName("토요일 한글 이름")
		void saturdayKoreanName() {
			// when & then
			assertThat(DayOfWeek.SATURDAY.getKoreanName()).isEqualTo("토요일");
		}
		
		@Test
		@DisplayName("일요일 한글 이름")
		void sundayKoreanName() {
			// when & then
			assertThat(DayOfWeek.SUNDAY.getKoreanName()).isEqualTo("일요일");
		}
	}
	
	@Nested
	@DisplayName("숫자 값 테스트")
	class ValueTests {
		
		@Test
		@DisplayName("월요일은 1")
		void mondayValue() {
			// when & then
			assertThat(DayOfWeek.MONDAY.getValue()).isEqualTo(1);
		}
		
		@Test
		@DisplayName("화요일은 2")
		void tuesdayValue() {
			// when & then
			assertThat(DayOfWeek.TUESDAY.getValue()).isEqualTo(2);
		}
		
		@Test
		@DisplayName("수요일은 3")
		void wednesdayValue() {
			// when & then
			assertThat(DayOfWeek.WEDNESDAY.getValue()).isEqualTo(3);
		}
		
		@Test
		@DisplayName("목요일은 4")
		void thursdayValue() {
			// when & then
			assertThat(DayOfWeek.THURSDAY.getValue()).isEqualTo(4);
		}
		
		@Test
		@DisplayName("금요일은 5")
		void fridayValue() {
			// when & then
			assertThat(DayOfWeek.FRIDAY.getValue()).isEqualTo(5);
		}
		
		@Test
		@DisplayName("토요일은 6")
		void saturdayValue() {
			// when & then
			assertThat(DayOfWeek.SATURDAY.getValue()).isEqualTo(6);
		}
		
		@Test
		@DisplayName("일요일은 7")
		void sundayValue() {
			// when & then
			assertThat(DayOfWeek.SUNDAY.getValue()).isEqualTo(7);
		}
	}
	
	@Nested
	@DisplayName("Java DayOfWeek 변환 테스트")
	class JavaDayOfWeekConversionTests {
		
		@Test
		@DisplayName("Java DayOfWeek에서 변환 - MONDAY")
		void fromJavaDayOfWeekMonday() {
			// given
			final java.time.DayOfWeek javaDayOfWeek = java.time.DayOfWeek.MONDAY;
			
			// when
			final DayOfWeek dayOfWeek = DayOfWeek.from(javaDayOfWeek);
			
			// then
			assertThat(dayOfWeek).isEqualTo(DayOfWeek.MONDAY);
		}
		
		@Test
		@DisplayName("Java DayOfWeek에서 변환 - SUNDAY")
		void fromJavaDayOfWeekSunday() {
			// given
			final java.time.DayOfWeek javaDayOfWeek = java.time.DayOfWeek.SUNDAY;
			
			// when
			final DayOfWeek dayOfWeek = DayOfWeek.from(javaDayOfWeek);
			
			// then
			assertThat(dayOfWeek).isEqualTo(DayOfWeek.SUNDAY);
		}
		
		@Test
		@DisplayName("모든 Java DayOfWeek 값 변환 확인")
		void fromAllJavaDayOfWeekValues() {
			// when & then
			assertThat(DayOfWeek.from(java.time.DayOfWeek.MONDAY)).isEqualTo(DayOfWeek.MONDAY);
			assertThat(DayOfWeek.from(java.time.DayOfWeek.TUESDAY)).isEqualTo(DayOfWeek.TUESDAY);
			assertThat(DayOfWeek.from(java.time.DayOfWeek.WEDNESDAY)).isEqualTo(DayOfWeek.WEDNESDAY);
			assertThat(DayOfWeek.from(java.time.DayOfWeek.THURSDAY)).isEqualTo(DayOfWeek.THURSDAY);
			assertThat(DayOfWeek.from(java.time.DayOfWeek.FRIDAY)).isEqualTo(DayOfWeek.FRIDAY);
			assertThat(DayOfWeek.from(java.time.DayOfWeek.SATURDAY)).isEqualTo(DayOfWeek.SATURDAY);
			assertThat(DayOfWeek.from(java.time.DayOfWeek.SUNDAY)).isEqualTo(DayOfWeek.SUNDAY);
		}
		
		@Test
		@DisplayName("null Java DayOfWeek에서 변환 시 예외 발생")
		void throwExceptionWhenFromNullJavaDayOfWeek() {
			// when & then
			assertThatThrownBy(() -> DayOfWeek.from(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Java DayOfWeek cannot be null");
		}
		
		@Test
		@DisplayName("Java DayOfWeek로 변환 - MONDAY")
		void toJavaDayOfWeekMonday() {
			// when
			final java.time.DayOfWeek javaDayOfWeek = DayOfWeek.MONDAY.toJavaDayOfWeek();
			
			// then
			assertThat(javaDayOfWeek).isEqualTo(java.time.DayOfWeek.MONDAY);
		}
		
		@Test
		@DisplayName("Java DayOfWeek로 변환 - SUNDAY")
		void toJavaDayOfWeekSunday() {
			// when
			final java.time.DayOfWeek javaDayOfWeek = DayOfWeek.SUNDAY.toJavaDayOfWeek();
			
			// then
			assertThat(javaDayOfWeek).isEqualTo(java.time.DayOfWeek.SUNDAY);
		}
		
		@Test
		@DisplayName("모든 값의 Java DayOfWeek 변환 확인")
		void toJavaDayOfWeekAllValues() {
			// when & then
			assertThat(DayOfWeek.MONDAY.toJavaDayOfWeek()).isEqualTo(java.time.DayOfWeek.MONDAY);
			assertThat(DayOfWeek.TUESDAY.toJavaDayOfWeek()).isEqualTo(java.time.DayOfWeek.TUESDAY);
			assertThat(DayOfWeek.WEDNESDAY.toJavaDayOfWeek()).isEqualTo(java.time.DayOfWeek.WEDNESDAY);
			assertThat(DayOfWeek.THURSDAY.toJavaDayOfWeek()).isEqualTo(java.time.DayOfWeek.THURSDAY);
			assertThat(DayOfWeek.FRIDAY.toJavaDayOfWeek()).isEqualTo(java.time.DayOfWeek.FRIDAY);
			assertThat(DayOfWeek.SATURDAY.toJavaDayOfWeek()).isEqualTo(java.time.DayOfWeek.SATURDAY);
			assertThat(DayOfWeek.SUNDAY.toJavaDayOfWeek()).isEqualTo(java.time.DayOfWeek.SUNDAY);
		}
		
		@Test
		@DisplayName("양방향 변환 일관성 확인")
		void bidirectionalConversionConsistency() {
			// given
			final java.time.DayOfWeek javaDayOfWeek = java.time.DayOfWeek.WEDNESDAY;
			
			// when
			final DayOfWeek converted = DayOfWeek.from(javaDayOfWeek);
			final java.time.DayOfWeek backToJava = converted.toJavaDayOfWeek();
			
			// then
			assertThat(backToJava).isEqualTo(javaDayOfWeek);
		}
	}
	
	@Nested
	@DisplayName("주말/평일 판별 테스트")
	class WeekendWeekdayTests {
		
		@Test
		@DisplayName("토요일은 주말")
		void saturdayIsWeekend() {
			// when & then
			assertThat(DayOfWeek.SATURDAY.isWeekend()).isTrue();
			assertThat(DayOfWeek.SATURDAY.isWeekday()).isFalse();
		}
		
		@Test
		@DisplayName("일요일은 주말")
		void sundayIsWeekend() {
			// when & then
			assertThat(DayOfWeek.SUNDAY.isWeekend()).isTrue();
			assertThat(DayOfWeek.SUNDAY.isWeekday()).isFalse();
		}
		
		@Test
		@DisplayName("월요일은 평일")
		void mondayIsWeekday() {
			// when & then
			assertThat(DayOfWeek.MONDAY.isWeekday()).isTrue();
			assertThat(DayOfWeek.MONDAY.isWeekend()).isFalse();
		}
		
		@Test
		@DisplayName("화요일은 평일")
		void tuesdayIsWeekday() {
			// when & then
			assertThat(DayOfWeek.TUESDAY.isWeekday()).isTrue();
			assertThat(DayOfWeek.TUESDAY.isWeekend()).isFalse();
		}
		
		@Test
		@DisplayName("수요일은 평일")
		void wednesdayIsWeekday() {
			// when & then
			assertThat(DayOfWeek.WEDNESDAY.isWeekday()).isTrue();
			assertThat(DayOfWeek.WEDNESDAY.isWeekend()).isFalse();
		}
		
		@Test
		@DisplayName("목요일은 평일")
		void thursdayIsWeekday() {
			// when & then
			assertThat(DayOfWeek.THURSDAY.isWeekday()).isTrue();
			assertThat(DayOfWeek.THURSDAY.isWeekend()).isFalse();
		}
		
		@Test
		@DisplayName("금요일은 평일")
		void fridayIsWeekday() {
			// when & then
			assertThat(DayOfWeek.FRIDAY.isWeekday()).isTrue();
			assertThat(DayOfWeek.FRIDAY.isWeekend()).isFalse();
		}
	}
}
