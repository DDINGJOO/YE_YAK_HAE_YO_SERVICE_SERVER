package com.teambind.springproject.domain.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RoomId Value Object 테스트")
class RoomIdTest {
	
	@Nested
	@DisplayName("생성 테스트")
	class CreationTests {
		
		@Test
		@DisplayName("유효한 ID로 생성 성공")
		void createValidRoomId() {
			// given
			final Long value = 1L;
			
			// when
			final RoomId roomId = RoomId.of(value);
			
			// then
			assertThat(roomId.getValue()).isEqualTo(value);
		}
		
		@Test
		@DisplayName("큰 숫자로 ID 생성 성공")
		void createWithLargeNumber() {
			// given
			final Long value = 999999999L;
			
			// when
			final RoomId roomId = RoomId.of(value);
			
			// then
			assertThat(roomId.getValue()).isEqualTo(value);
		}
	}
	
	@Nested
	@DisplayName("유효성 검증 테스트")
	class ValidationTests {
		
		@Test
		@DisplayName("null 값으로 생성 시 예외 발생")
		void throwExceptionWhenValueIsNull() {
			// when & then
			assertThatThrownBy(() -> RoomId.of(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Room ID cannot be null");
		}
		
		@Test
		@DisplayName("0으로 생성 시 예외 발생")
		void throwExceptionWhenValueIsZero() {
			// given
			final Long zero = 0L;
			
			// when & then
			assertThatThrownBy(() -> RoomId.of(zero))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Room ID must be positive: 0");
		}
		
		@Test
		@DisplayName("음수 값으로 생성 시 예외 발생")
		void throwExceptionWhenValueIsNegative() {
			// given
			final Long negativeValue = -1L;
			
			// when & then
			assertThatThrownBy(() -> RoomId.of(negativeValue))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Room ID must be positive: -1");
		}
	}
	
	@Nested
	@DisplayName("equals 및 hashCode 테스트")
	class EqualsAndHashCodeTests {
		
		@Test
		@DisplayName("같은 값의 RoomId는 동일")
		void equalRoomIdsWithSameValue() {
			// given
			final RoomId id1 = RoomId.of(1L);
			final RoomId id2 = RoomId.of(1L);
			
			// when & then
			assertThat(id1).isEqualTo(id2);
			assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
		}
		
		@Test
		@DisplayName("다른 값의 RoomId는 다름")
		void notEqualRoomIdsWithDifferentValue() {
			// given
			final RoomId id1 = RoomId.of(1L);
			final RoomId id2 = RoomId.of(2L);
			
			// when & then
			assertThat(id1).isNotEqualTo(id2);
		}
		
		@Test
		@DisplayName("동일 객체는 동일")
		void equalsSameObject() {
			// given
			final RoomId id = RoomId.of(1L);
			
			// when & then
			assertThat(id).isEqualTo(id);
		}
		
		@Test
		@DisplayName("null과 비교하면 다름")
		void notEqualsNull() {
			// given
			final RoomId id = RoomId.of(1L);
			
			// when & then
			assertThat(id).isNotEqualTo(null);
		}
		
		@Test
		@DisplayName("다른 타입 객체와 비교하면 다름")
		void notEqualsDifferentType() {
			// given
			final RoomId id = RoomId.of(1L);
			final Long value = 1L;
			
			// when & then
			assertThat(id).isNotEqualTo(value);
		}
		
		@Test
		@DisplayName("다른 ID 타입과 비교하면 다름")
		void notEqualsDifferentIdType() {
			// given
			final RoomId roomId = RoomId.of(1L);
			final PlaceId placeId = PlaceId.of(1L);
			
			// when & then
			assertThat(roomId).isNotEqualTo(placeId);
		}
	}
	
	@Nested
	@DisplayName("toString 테스트")
	class ToStringTests {
		
		@Test
		@DisplayName("toString 형식 확인")
		void toStringFormat() {
			// given
			final RoomId id = RoomId.of(123L);
			
			// when
			final String result = id.toString();
			
			// then
			assertThat(result).isEqualTo("RoomId{123}");
		}
	}
}
