package com.teambind.springproject.domain.product;

import com.teambind.springproject.domain.product.pricing.PricingStrategy;
import com.teambind.springproject.domain.product.vo.PricingType;
import com.teambind.springproject.domain.product.vo.ProductPriceBreakdown;
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

@DisplayName("Product Aggregate 테스트")
class ProductTest {
	
	@Nested
	@DisplayName("생성 테스트")
	class CreationTests {
		
		@Test
		@DisplayName("PLACE 범위 상품 생성 성공")
		void createPlaceScopedProduct() {
			// given
			final ProductId productId = ProductId.of(1L);
			final PlaceId placeId = PlaceId.of(100L);
			final String name = "공용 빔 프로젝터";
			final PricingStrategy pricingStrategy = PricingStrategy.oneTime(Money.of(10000));
			final int totalQuantity = 5;
			
			// when
			final Product product = Product.createPlaceScoped(
					productId, placeId, name, pricingStrategy, totalQuantity);
			
			// then
			assertThat(product.getProductId()).isEqualTo(productId);
			assertThat(product.getScope()).isEqualTo(ProductScope.PLACE);
			assertThat(product.getPlaceId()).isEqualTo(placeId);
			assertThat(product.getRoomId()).isNull();
			assertThat(product.getName()).isEqualTo(name);
			assertThat(product.getPricingStrategy()).isEqualTo(pricingStrategy);
			assertThat(product.getTotalQuantity()).isEqualTo(totalQuantity);
		}
		
		@Test
		@DisplayName("ROOM 범위 상품 생성 성공")
		void createRoomScopedProduct() {
			// given
			final ProductId productId = ProductId.of(2L);
			final PlaceId placeId = PlaceId.of(100L);
			final RoomId roomId = RoomId.of(200L);
			final String name = "룸 전용 화이트보드";
			final PricingStrategy pricingStrategy = PricingStrategy.simpleStock(Money.of(3000));
			final int totalQuantity = 10;
			
			// when
			final Product product = Product.createRoomScoped(
					productId, placeId, roomId, name, pricingStrategy, totalQuantity);
			
			// then
			assertThat(product.getProductId()).isEqualTo(productId);
			assertThat(product.getScope()).isEqualTo(ProductScope.ROOM);
			assertThat(product.getPlaceId()).isEqualTo(placeId);
			assertThat(product.getRoomId()).isEqualTo(roomId);
			assertThat(product.getName()).isEqualTo(name);
		}
		
		@Test
		@DisplayName("RESERVATION 범위 상품 생성 성공")
		void createReservationScopedProduct() {
			// given
			final ProductId productId = ProductId.of(3L);
			final String name = "음료수";
			final PricingStrategy pricingStrategy = PricingStrategy.simpleStock(Money.of(2000));
			final int totalQuantity = 100;
			
			// when
			final Product product = Product.createReservationScoped(
					productId, name, pricingStrategy, totalQuantity);
			
			// then
			assertThat(product.getProductId()).isEqualTo(productId);
			assertThat(product.getScope()).isEqualTo(ProductScope.RESERVATION);
			assertThat(product.getPlaceId()).isNull();
			assertThat(product.getRoomId()).isNull();
			assertThat(product.getName()).isEqualTo(name);
			assertThat(product.getTotalQuantity()).isEqualTo(totalQuantity);
		}
		
		@Test
		@DisplayName("null productId로 생성 시 예외 발생")
		void throwExceptionWhenProductIdIsNull() {
			// when & then
			assertThatThrownBy(() -> Product.createPlaceScoped(
					null,
					PlaceId.of(100L),
					"상품",
					PricingStrategy.oneTime(Money.of(10000)),
					5
			))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Product ID cannot be null");
		}
		
		@Test
		@DisplayName("상품명이 null이면 예외 발생")
		void throwExceptionWhenNameIsNull() {
			// when & then
			assertThatThrownBy(() -> Product.createPlaceScoped(
					ProductId.of(1L),
					PlaceId.of(100L),
					null,
					PricingStrategy.oneTime(Money.of(10000)),
					5
			))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Product name cannot be null or empty");
		}
		
		@Test
		@DisplayName("상품명이 빈 문자열이면 예외 발생")
		void throwExceptionWhenNameIsEmpty() {
			// when & then
			assertThatThrownBy(() -> Product.createPlaceScoped(
					ProductId.of(1L),
					PlaceId.of(100L),
					"   ",
					PricingStrategy.oneTime(Money.of(10000)),
					5
			))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Product name cannot be null or empty");
		}
		
		@Test
		@DisplayName("총 수량이 음수면 예외 발생")
		void throwExceptionWhenTotalQuantityIsNegative() {
			// when & then
			assertThatThrownBy(() -> Product.createPlaceScoped(
					ProductId.of(1L),
					PlaceId.of(100L),
					"상품",
					PricingStrategy.oneTime(Money.of(10000)),
					-1
			))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Total quantity cannot be negative: -1");
		}
		
		@Test
		@DisplayName("총 수량이 0인 경우 생성 성공 (품절 상태)")
		void createProductWithZeroQuantity() {
			// given
			final ProductId productId = ProductId.of(1L);
			final PlaceId placeId = PlaceId.of(100L);
			final PricingStrategy pricingStrategy = PricingStrategy.oneTime(Money.of(10000));
			
			// when
			final Product product = Product.createPlaceScoped(
					productId, placeId, "품절 상품", pricingStrategy, 0);
			
			// then
			assertThat(product.getTotalQuantity()).isZero();
		}
	}
	
	@Nested
	@DisplayName("가격 계산 테스트")
	class CalculatePriceTests {
		
		@Test
		@DisplayName("INITIAL_PLUS_ADDITIONAL 방식 가격 계산 성공")
		void calculatePriceWithInitialPlusAdditional() {
			// given
			final Product product = Product.createPlaceScoped(
					ProductId.of(1L),
					PlaceId.of(100L),
					"공용 빔 프로젝터",
					PricingStrategy.initialPlusAdditional(Money.of(10000), Money.of(5000)),
					5
			);
			
			// when
			final ProductPriceBreakdown breakdown = product.calculatePrice(3);
			
			// then
			assertThat(breakdown.productId()).isEqualTo(ProductId.of(1L));
			assertThat(breakdown.productName()).isEqualTo("공용 빔 프로젝터");
			assertThat(breakdown.quantity()).isEqualTo(3);
			assertThat(breakdown.pricingType()).isEqualTo(PricingType.INITIAL_PLUS_ADDITIONAL);
			// 초기 10,000 + (3-1) * 5,000 = 20,000 * 3 = 60,000
			assertThat(breakdown.unitPrice()).isEqualTo(Money.of(20000));
			assertThat(breakdown.totalPrice()).isEqualTo(Money.of(60000));
		}
		
		@Test
		@DisplayName("ONE_TIME 방식 가격 계산 성공")
		void calculatePriceWithOneTime() {
			// given
			final Product product = Product.createRoomScoped(
					ProductId.of(2L),
					PlaceId.of(100L),
					RoomId.of(200L),
					"1회 대여 장비",
					PricingStrategy.oneTime(Money.of(15000)),
					10
			);
			
			// when
			final ProductPriceBreakdown breakdown = product.calculatePrice(2);
			
			// then
			assertThat(breakdown.productId()).isEqualTo(ProductId.of(2L));
			assertThat(breakdown.productName()).isEqualTo("1회 대여 장비");
			assertThat(breakdown.quantity()).isEqualTo(2);
			assertThat(breakdown.pricingType()).isEqualTo(PricingType.ONE_TIME);
			// 1회 대여료 15,000 * 2 = 30,000
			assertThat(breakdown.unitPrice()).isEqualTo(Money.of(15000));
			assertThat(breakdown.totalPrice()).isEqualTo(Money.of(30000));
		}
		
		@Test
		@DisplayName("SIMPLE_STOCK 방식 가격 계산 성공")
		void calculatePriceWithSimpleStock() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(3L),
					"음료수",
					PricingStrategy.simpleStock(Money.of(2000)),
					100
			);
			
			// when
			final ProductPriceBreakdown breakdown = product.calculatePrice(5);
			
			// then
			assertThat(breakdown.productId()).isEqualTo(ProductId.of(3L));
			assertThat(breakdown.productName()).isEqualTo("음료수");
			assertThat(breakdown.quantity()).isEqualTo(5);
			assertThat(breakdown.pricingType()).isEqualTo(PricingType.SIMPLE_STOCK);
			// 단가 2,000 * 5 = 10,000
			assertThat(breakdown.unitPrice()).isEqualTo(Money.of(2000));
			assertThat(breakdown.totalPrice()).isEqualTo(Money.of(10000));
		}
		
		@Test
		@DisplayName("수량이 0 이하면 예외 발생")
		void throwExceptionWhenQuantityIsZeroOrNegative() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(1L),
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			
			// when & then
			assertThatThrownBy(() -> product.calculatePrice(0))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Quantity must be positive: 0");
			
			assertThatThrownBy(() -> product.calculatePrice(-1))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Quantity must be positive: -1");
		}
	}
	
	@Nested
	@DisplayName("수정 테스트")
	class UpdateTests {
		
		@Test
		@DisplayName("상품명 변경 성공")
		void updateName() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(1L),
					"원래 이름",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			
			// when
			product.updateName("새로운 이름");
			
			// then
			assertThat(product.getName()).isEqualTo("새로운 이름");
		}
		
		@Test
		@DisplayName("가격 전략 변경 성공")
		void updatePricingStrategy() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(1L),
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			
			// when
			final PricingStrategy newStrategy = PricingStrategy.oneTime(Money.of(5000));
			product.updatePricingStrategy(newStrategy);
			
			// then
			assertThat(product.getPricingStrategy()).isEqualTo(newStrategy);
		}
		
		@Test
		@DisplayName("총 수량 변경 성공")
		void updateTotalQuantity() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(1L),
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			
			// when
			product.updateTotalQuantity(20);
			
			// then
			assertThat(product.getTotalQuantity()).isEqualTo(20);
		}
		
		@Test
		@DisplayName("null 상품명으로 변경 시 예외 발생")
		void throwExceptionWhenUpdatingWithNullName() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(1L),
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			
			// when & then
			assertThatThrownBy(() -> product.updateName(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Product name cannot be null or empty");
		}
		
		@Test
		@DisplayName("음수 총 수량으로 변경 시 예외 발생")
		void throwExceptionWhenUpdatingWithNegativeQuantity() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(1L),
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			
			// when & then
			assertThatThrownBy(() -> product.updateTotalQuantity(-1))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Total quantity cannot be negative: -1");
		}
	}
	
	@Nested
	@DisplayName("동등성 테스트")
	class EqualityTests {
		
		@Test
		@DisplayName("같은 ProductId를 가진 상품은 동일")
		void equalityBasedOnProductId() {
			// given
			final ProductId productId = ProductId.of(1L);
			final Product product1 = Product.createReservationScoped(
					productId, "상품1", PricingStrategy.simpleStock(Money.of(1000)), 10);
			final Product product2 = Product.createReservationScoped(
					productId, "상품2", PricingStrategy.simpleStock(Money.of(2000)), 20);
			
			// when & then
			assertThat(product1).isEqualTo(product2);
			assertThat(product1.hashCode()).isEqualTo(product2.hashCode());
		}
		
		@Test
		@DisplayName("다른 ProductId를 가진 상품은 다름")
		void inequalityBasedOnDifferentProductId() {
			// given
			final Product product1 = Product.createReservationScoped(
					ProductId.of(1L), "상품1", PricingStrategy.simpleStock(Money.of(1000)), 10);
			final Product product2 = Product.createReservationScoped(
					ProductId.of(2L), "상품1", PricingStrategy.simpleStock(Money.of(1000)), 10);
			
			// when & then
			assertThat(product1).isNotEqualTo(product2);
		}
	}
	
	@Nested
	@DisplayName("엣지 케이스 및 경계값 테스트")
	class EdgeCaseTests {
		
		@Test
		@DisplayName("매우 긴 상품명 (1000자) 생성 성공")
		void createProductWithVeryLongName() {
			// given
			final String longName = "A".repeat(1000);
			final ProductId productId = ProductId.of(1L);
			
			// when
			final Product product = Product.createReservationScoped(
					productId, longName, PricingStrategy.simpleStock(Money.of(1000)), 10);
			
			// then
			assertThat(product.getName()).hasSize(1000);
		}
		
		@Test
		@DisplayName("특수문자가 포함된 상품명 생성 성공")
		void createProductWithSpecialCharactersName() {
			// given
			final String specialName = "상품!@#$%^&*()_+-=[]{}|;:',.<>?/~`";
			final ProductId productId = ProductId.of(1L);
			
			// when
			final Product product = Product.createReservationScoped(
					productId, specialName, PricingStrategy.simpleStock(Money.of(1000)), 10);
			
			// then
			assertThat(product.getName()).isEqualTo(specialName);
		}
		
		@Test
		@DisplayName("매우 큰 수량 (Integer.MAX_VALUE) 생성 성공")
		void createProductWithMaxIntegerQuantity() {
			// given
			final int maxQuantity = Integer.MAX_VALUE;
			final ProductId productId = ProductId.of(1L);
			
			// when
			final Product product = Product.createReservationScoped(
					productId, "대량 재고 상품", PricingStrategy.simpleStock(Money.of(1000)), maxQuantity);
			
			// then
			assertThat(product.getTotalQuantity()).isEqualTo(Integer.MAX_VALUE);
		}
		
		@Test
		@DisplayName("대량 수량 (10000개)으로 가격 계산 성공")
		void calculatePriceWithLargeQuantity() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(1L),
					"대량 상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					100000
			);
			
			// when
			final ProductPriceBreakdown breakdown = product.calculatePrice(10000);
			
			// then
			assertThat(breakdown.quantity()).isEqualTo(10000);
			assertThat(breakdown.totalPrice()).isEqualTo(Money.of(10000000));
		}
		
		@Test
		@DisplayName("가격이 0원인 상품 생성 및 계산 성공")
		void createAndCalculatePriceWithZeroPrice() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(1L),
					"무료 상품",
					PricingStrategy.simpleStock(Money.ZERO),
					10
			);
			
			// when
			final ProductPriceBreakdown breakdown = product.calculatePrice(5);
			
			// then
			assertThat(breakdown.unitPrice()).isEqualTo(Money.ZERO);
			assertThat(breakdown.totalPrice()).isEqualTo(Money.ZERO);
		}
		
		@Test
		@DisplayName("INITIAL_PLUS_ADDITIONAL 방식에서 매우 큰 수량 계산")
		void calculateInitialPlusAdditionalWithLargeQuantity() {
			// given
			final Product product = Product.createPlaceScoped(
					ProductId.of(1L),
					PlaceId.of(100L),
					"시간 대여 상품",
					PricingStrategy.initialPlusAdditional(Money.of(10000), Money.of(5000)),
					10000
			);
			
			// when
			final ProductPriceBreakdown breakdown = product.calculatePrice(100);
			
			// then
			// 10,000 + (100-1) * 5,000 = 505,000 per unit
			// 505,000 * 100 = 50,500,000
			assertThat(breakdown.unitPrice()).isEqualTo(Money.of(505000));
			assertThat(breakdown.totalPrice()).isEqualTo(Money.of(50500000));
		}
		
		@Test
		@DisplayName("단일 문자 상품명 생성 성공")
		void createProductWithSingleCharacterName() {
			// given
			final String singleChar = "A";
			
			// when
			final Product product = Product.createReservationScoped(
					ProductId.of(1L),
					singleChar,
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			
			// then
			assertThat(product.getName()).isEqualTo("A");
		}
		
		@Test
		@DisplayName("공백만 있는 상품명으로 변경 시 예외 발생")
		void throwExceptionWhenUpdatingWithWhitespaceName() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(1L),
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			
			// when & then
			assertThatThrownBy(() -> product.updateName("     "))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Product name cannot be null or empty");
		}
		
		@Test
		@DisplayName("탭과 개행 문자만 있는 상품명으로 생성 시 예외 발생")
		void throwExceptionWhenCreatingWithTabAndNewlineName() {
			// when & then
			assertThatThrownBy(() -> Product.createReservationScoped(
					ProductId.of(1L),
					"\t\n\r",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Product name cannot be null or empty");
		}
	}
}
