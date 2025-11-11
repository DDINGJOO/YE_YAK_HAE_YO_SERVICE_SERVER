package com.teambind.springproject.domain.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimeSlot Enum 테스트")
class TimeSlotTest {
	
	@Test
	@DisplayName("HOUR는 60분")
	void hourIs60Minutes() {
		// when & then
		assertThat(TimeSlot.HOUR.getMinutes()).isEqualTo(60);
	}
	
	@Test
	@DisplayName("HALFHOUR는 30분")
	void halfHourIs30Minutes() {
		// when & then
		assertThat(TimeSlot.HALFHOUR.getMinutes()).isEqualTo(30);
	}
	
	@Test
	@DisplayName("HOUR는 1시간")
	void hourIs1Hour() {
		// when & then
		assertThat(TimeSlot.HOUR.getHours()).isEqualTo(1);
	}
	
	@Test
	@DisplayName("HALFHOUR는 0시간")
	void halfHourIs0Hours() {
		// when & then
		assertThat(TimeSlot.HALFHOUR.getHours()).isEqualTo(0);
	}
	
	@Test
	@DisplayName("HOUR의 isHour()는 true")
	void hourIsHourReturnsTrue() {
		// when & then
		assertThat(TimeSlot.HOUR.isHour()).isTrue();
	}
	
	@Test
	@DisplayName("HOUR의 isHalfHour()는 false")
	void hourIsHalfHourReturnsFalse() {
		// when & then
		assertThat(TimeSlot.HOUR.isHalfHour()).isFalse();
	}
	
	@Test
	@DisplayName("HALFHOUR의 isHalfHour()는 true")
	void halfHourIsHalfHourReturnsTrue() {
		// when & then
		assertThat(TimeSlot.HALFHOUR.isHalfHour()).isTrue();
	}
	
	@Test
	@DisplayName("HALFHOUR의 isHour()는 false")
	void halfHourIsHourReturnsFalse() {
		// when & then
		assertThat(TimeSlot.HALFHOUR.isHour()).isFalse();
	}
	
	@Test
	@DisplayName("TimeSlot values() 확인")
	void allValues() {
		// when
		final TimeSlot[] values = TimeSlot.values();
		
		// then
		assertThat(values).hasSize(2);
		assertThat(values).containsExactly(TimeSlot.HOUR, TimeSlot.HALFHOUR);
	}
	
	@Test
	@DisplayName("TimeSlot valueOf() 확인")
	void valueOfMethod() {
		// when & then
		assertThat(TimeSlot.valueOf("HOUR")).isEqualTo(TimeSlot.HOUR);
		assertThat(TimeSlot.valueOf("HALFHOUR")).isEqualTo(TimeSlot.HALFHOUR);
	}
}
