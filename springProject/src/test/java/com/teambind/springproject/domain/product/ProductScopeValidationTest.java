package com.teambind.springproject.domain.product;

import com.teambind.springproject.domain.product.pricing.PricingStrategy;
import com.teambind.springproject.domain.product.vo.ProductScope;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Product Scope 검증 테스트")
class ProductScopeValidationTest {
	
	@Nested
	@DisplayName("PLACE 범위 검증")
	class PlaceScopeValidationTests {
		
		@Test
		@DisplayName("placeId가 null이면 예외 발생")
		void throwExceptionWhenPlaceIdIsNull() {
			// when & then
			assertThatThrownBy(() -> Product.createPlaceScoped(
					ProductId.of(1L),
					null,
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("PLACE scope requires placeId");
		}
		
		@Test
		@DisplayName("placeId가 있으면 생성 성공")
		void createSuccessfullyWithPlaceId() {
			// given
			final PlaceId placeId = PlaceId.of(100L);
			
			// when
			final Product product = Product.createPlaceScoped(
					ProductId.of(1L),
					placeId,
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			
			// then
			assertThat(product.getPlaceId()).isEqualTo(placeId);
			assertThat(product.getRoomId()).isNull();
		}
	}
	
	@Nested
	@DisplayName("ROOM 범위 검증")
	class RoomScopeValidationTests {
		
		@Test
		@DisplayName("placeId가 null이면 예외 발생")
		void throwExceptionWhenPlaceIdIsNull() {
			// when & then
			assertThatThrownBy(() -> Product.createRoomScoped(
					ProductId.of(1L),
					null,
					RoomId.of(200L),
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("ROOM scope requires placeId");
		}
		
		@Test
		@DisplayName("roomId가 null이면 예외 발생")
		void throwExceptionWhenRoomIdIsNull() {
			// when & then
			assertThatThrownBy(() -> Product.createRoomScoped(
					ProductId.of(1L),
					PlaceId.of(100L),
					null,
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("ROOM scope requires roomId");
		}
		
		@Test
		@DisplayName("placeId와 roomId가 모두 있으면 생성 성공")
		void createSuccessfullyWithPlaceIdAndRoomId() {
			// given
			final PlaceId placeId = PlaceId.of(100L);
			final RoomId roomId = RoomId.of(200L);
			
			// when
			final Product product = Product.createRoomScoped(
					ProductId.of(1L),
					placeId,
					roomId,
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			
			// then
			assertThat(product.getPlaceId()).isEqualTo(placeId);
			assertThat(product.getRoomId()).isEqualTo(roomId);
		}
		
		@Test
		@DisplayName("ROOM 범위 상품은 PLACE 정보도 저장하여 조회 가능")
		void roomScopedProductStoresPlaceIdForQuery() {
			// given - ROOM 범위 상품 생성 시 PlaceId도 함께 저장
			final PlaceId placeId = PlaceId.of(100L);
			final RoomId roomId = RoomId.of(200L);
			
			// when
			final Product product = Product.createRoomScoped(
					ProductId.of(1L),
					placeId,
					roomId,
					"룸 전용 상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			
			// then - PlaceId로 해당 PLACE 하위 모든 ROOM 상품 조회 가능
			assertThat(product.getPlaceId()).isEqualTo(placeId);
			assertThat(product.getRoomId()).isEqualTo(roomId);
			assertThat(product.getScope()).isEqualTo(ProductScope.ROOM);
		}
	}
	
	@Nested
	@DisplayName("RESERVATION 범위 검증")
	class ReservationScopeValidationTests {
		
		@Test
		@DisplayName("placeId와 roomId가 모두 null이면 생성 성공")
		void createSuccessfullyWithNullIds() {
			// when
			final Product product = Product.createReservationScoped(
					ProductId.of(1L),
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			
			// then
			assertThat(product.getPlaceId()).isNull();
			assertThat(product.getRoomId()).isNull();
		}
	}
	
	@Nested
	@DisplayName("플레이스 하위 상품 조회 시나리오")
	class PlaceProductQueryScenarioTests {
		
		@Test
		@DisplayName("특정 PLACE의 PLACE 범위 상품과 하위 ROOM 상품 모두 조회 가능")
		void queryAllProductsUnderPlace() {
			// given - PlaceId 100L에 속한 상품들
			final PlaceId placeId = PlaceId.of(100L);
			
			// PLACE 범위 상품
			final Product placeScopedProduct = Product.createPlaceScoped(
					ProductId.of(1L),
					placeId,
					"공용 빔 프로젝터",
					PricingStrategy.oneTime(Money.of(10000)),
					5
			);
			
			// ROOM 범위 상품 (같은 PLACE 소속)
			final Product roomScopedProduct1 = Product.createRoomScoped(
					ProductId.of(2L),
					placeId,
					RoomId.of(201L),
					"룸 A 화이트보드",
					PricingStrategy.simpleStock(Money.of(3000)),
					2
			);
			
			final Product roomScopedProduct2 = Product.createRoomScoped(
					ProductId.of(3L),
					placeId,
					RoomId.of(202L),
					"룸 B 화이트보드",
					PricingStrategy.simpleStock(Money.of(3000)),
					2
			);
			
			// when & then - PlaceId로 필터링 가능
			assertThat(placeScopedProduct.getPlaceId()).isEqualTo(placeId);
			assertThat(roomScopedProduct1.getPlaceId()).isEqualTo(placeId);
			assertThat(roomScopedProduct2.getPlaceId()).isEqualTo(placeId);
			
			// Repository에서 다음과 같이 조회 가능:
			// findByPlaceId(placeId) -> PLACE 범위 + 모든 ROOM 범위 상품 반환
		}
		
		@Test
		@DisplayName("특정 ROOM의 상품만 조회 가능")
		void queryProductsForSpecificRoom() {
			// given
			final PlaceId placeId = PlaceId.of(100L);
			final RoomId roomId = RoomId.of(201L);
			
			final Product roomScopedProduct = Product.createRoomScoped(
					ProductId.of(1L),
					placeId,
					roomId,
					"룸 A 화이트보드",
					PricingStrategy.simpleStock(Money.of(3000)),
					2
			);
			
			// when & then - RoomId로 필터링 가능
			assertThat(roomScopedProduct.getRoomId()).isEqualTo(roomId);
			
			// Repository에서 다음과 같이 조회 가능:
			// findByRoomId(roomId) -> 해당 ROOM 범위 상품만 반환
		}
	}
}
