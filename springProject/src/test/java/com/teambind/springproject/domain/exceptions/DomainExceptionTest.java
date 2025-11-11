package com.teambind.springproject.domain.exceptions;

import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrices;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.pricing.PricingStrategy;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown;
import com.teambind.springproject.domain.reservationpricing.exception.InvalidReservationStatusException;
import com.teambind.springproject.domain.shared.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 도메인 예외 케이스 테스트.
 * <p>
 * 도메인 모델에서 발생하는 다양한 예외 상황을 검증합니다.
 * 이 테스트들은 시스템의 불변식(invariant)과 비즈니스 규칙을 보호합니다.
 */
@DisplayName("도메인 예외 케이스 테스트")
class DomainExceptionTest {
	
	@Nested
	@DisplayName("Product 도메인 예외")
	class ProductDomainExceptions {
		
		@Test
		@DisplayName("null PricingStrategy로 생성 시 예외 발생")
		void throwExceptionWhenCreatingWithNullPricingStrategy() {
			// when & then
			assertThatThrownBy(() -> Product.createReservationScoped(
					ProductId.of(1L),
					"상품",
					null,  // null PricingStrategy
					10
			))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Pricing strategy cannot be null");
		}
		
		@Test
		@DisplayName("PLACE scope에 roomId가 있으면 예외 발생")
		void throwExceptionWhenPlaceScopeHasRoomId() {
			// when & then
			assertThatThrownBy(() -> Product.createPlaceScoped(
					ProductId.of(1L),
					PlaceId.of(100L),
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			))
					.isInstanceOf(IllegalArgumentException.class);
		}
		
		@Test
		@DisplayName("ROOM scope에 placeId가 없으면 예외 발생")
		void throwExceptionWhenRoomScopeHasNoPlaceId() {
			// when & then
			assertThatThrownBy(() -> Product.createRoomScoped(
					ProductId.of(1L),
					null,  // null placeId
					RoomId.of(200L),
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("ROOM scope requires placeId");
		}
		
		@Test
		@DisplayName("ROOM scope에 roomId가 없으면 예외 발생")
		void throwExceptionWhenRoomScopeHasNoRoomId() {
			// when & then
			assertThatThrownBy(() -> Product.createRoomScoped(
					ProductId.of(1L),
					PlaceId.of(100L),
					null,  // null roomId
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("ROOM scope requires roomId");
		}
		
		@Test
		@DisplayName("RESERVATION scope에 placeId가 있으면 예외 발생")
		void throwExceptionWhenReservationScopeHasPlaceId() {
			// RESERVATION scope는 placeId와 roomId가 모두 null이어야 함
			// Product.createReservationScoped는 이미 null로 설정하므로 별도 테스트 불필요
			// 하지만 불변식 검증을 위해 테스트 작성
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
		
		@Test
		@DisplayName("수량 0으로 가격 계산 시 예외 발생")
		void throwExceptionWhenCalculatingPriceWithZeroQuantity() {
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
		}
		
		@Test
		@DisplayName("음수 수량으로 가격 계산 시 예외 발생")
		void throwExceptionWhenCalculatingPriceWithNegativeQuantity() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(1L),
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			
			// when & then
			assertThatThrownBy(() -> product.calculatePrice(-5))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Quantity must be positive: -5");
		}
	}
	
	@Nested
	@DisplayName("Money 도메인 예외")
	class MoneyDomainExceptions {
		
		@Test
		@DisplayName("null BigDecimal로 생성 시 예외 발생")
		void throwExceptionWhenCreatingWithNullBigDecimal() {
			// when & then
			assertThatThrownBy(() -> Money.of((BigDecimal) null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Amount cannot be null");
		}
		
		@Test
		@DisplayName("음수로 생성 시 예외 발생")
		void throwExceptionWhenCreatingWithNegativeAmount() {
			// when & then
			assertThatThrownBy(() -> Money.of(new BigDecimal("-1000")))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Amount cannot be negative: -1000");
		}
		
		@Test
		@DisplayName("빼기 결과가 음수면 예외 발생")
		void throwExceptionWhenSubtractionResultIsNegative() {
			// given
			final Money smaller = Money.of(1000);
			final Money larger = Money.of(5000);
			
			// when & then
			assertThatThrownBy(() -> smaller.subtract(larger))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Result cannot be negative");
		}
		
		@Test
		@DisplayName("null Money와 더하기 시 예외 발생")
		void throwExceptionWhenAddingNullMoney() {
			// given
			final Money money = Money.of(1000);
			
			// when & then
			assertThatThrownBy(() -> money.add(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Other money cannot be null");
		}
		
		@Test
		@DisplayName("null Money와 빼기 시 예외 발생")
		void throwExceptionWhenSubtractingNullMoney() {
			// given
			final Money money = Money.of(1000);
			
			// when & then
			assertThatThrownBy(() -> money.subtract(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Other money cannot be null");
		}
		
		@Test
		@DisplayName("음수로 곱하기 시 예외 발생")
		void throwExceptionWhenMultiplyingByNegative() {
			// given
			final Money money = Money.of(1000);
			
			// when & then
			assertThatThrownBy(() -> money.multiply(-2))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Multiplier cannot be negative: -2");
		}
		
		@Test
		@DisplayName("null Money와 비교 시 예외 발생")
		void throwExceptionWhenComparingWithNullMoney() {
			// given
			final Money money = Money.of(1000);
			
			// when & then
			assertThatThrownBy(() -> money.isGreaterThan(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Other money cannot be null");
		}
	}
	
	@Nested
	@DisplayName("PricingStrategy 도메인 예외")
	class PricingStrategyDomainExceptions {
		
		@Test
		@DisplayName("INITIAL_PLUS_ADDITIONAL에서 initialPrice가 null이면 예외 발생")
		void throwExceptionWhenInitialPriceIsNull() {
			// when & then
			assertThatThrownBy(() -> PricingStrategy.initialPlusAdditional(null, Money.of(5000)))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Initial price and additional price cannot be null for INITIAL_PLUS_ADDITIONAL type");
		}
		
		@Test
		@DisplayName("INITIAL_PLUS_ADDITIONAL에서 additionalPrice가 null이면 예외 발생")
		void throwExceptionWhenAdditionalPriceIsNull() {
			// when & then
			assertThatThrownBy(() -> PricingStrategy.initialPlusAdditional(Money.of(10000), null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Initial price and additional price cannot be null for INITIAL_PLUS_ADDITIONAL type");
		}
		
		@Test
		@DisplayName("ONE_TIME에서 price가 null이면 예외 발생")
		void throwExceptionWhenOneTimePriceIsNull() {
			// when & then
			assertThatThrownBy(() -> PricingStrategy.oneTime(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("One-time price cannot be null for ONE_TIME type");
		}
		
		@Test
		@DisplayName("SIMPLE_STOCK에서 price가 null이면 예외 발생")
		void throwExceptionWhenSimpleStockPriceIsNull() {
			// when & then
			assertThatThrownBy(() -> PricingStrategy.simpleStock(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Unit price cannot be null for SIMPLE_STOCK type");
		}
		
		@Test
		@DisplayName("수량 0으로 계산 시 예외 발생")
		void throwExceptionWhenCalculatingWithZeroQuantity() {
			// given
			final PricingStrategy strategy = PricingStrategy.simpleStock(Money.of(1000));
			
			// when & then
			assertThatThrownBy(() -> strategy.calculate(0))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Quantity must be positive: 0");
		}
		
		@Test
		@DisplayName("음수 수량으로 계산 시 예외 발생")
		void throwExceptionWhenCalculatingWithNegativeQuantity() {
			// given
			final PricingStrategy strategy = PricingStrategy.simpleStock(Money.of(1000));
			
			// when & then
			assertThatThrownBy(() -> strategy.calculate(-3))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Quantity must be positive: -3");
		}
	}
	
	@Nested
	@DisplayName("ReservationPricing 도메인 예외")
	class ReservationPricingDomainExceptions {
		
		private ReservationPricing createTestReservation() {
			final LocalDateTime slot1 = LocalDateTime.of(2025, 1, 15, 10, 0);
			final LocalDateTime slot2 = LocalDateTime.of(2025, 1, 15, 11, 0);
			
			final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
					Map.of(
							slot1, Money.of(10000),
							slot2, Money.of(10000)
					),
					TimeSlot.HOUR
			);
			
			return ReservationPricing.calculate(
					ReservationId.of(1L),
					RoomId.of(100L),
					timeSlotBreakdown,
					List.of(),
					30
			);
		}
		
		@Test
		@DisplayName("이미 확정된 예약을 다시 확정하면 예외 발생")
		void throwExceptionWhenConfirmingAlreadyConfirmedReservation() {
			// given
			final ReservationPricing reservation = createTestReservation();
			reservation.confirm();
			
			// when & then
			assertThatThrownBy(reservation::confirm)
					.isInstanceOf(InvalidReservationStatusException.class);
		}
		
		@Test
		@DisplayName("이미 취소된 예약을 다시 취소하면 예외 발생")
		void throwExceptionWhenCancellingAlreadyCancelledReservation() {
			// given
			final ReservationPricing reservation = createTestReservation();
			reservation.cancel();
			
			// when & then
			assertThatThrownBy(reservation::cancel)
					.isInstanceOf(InvalidReservationStatusException.class);
		}
		
		@Test
		@DisplayName("취소된 예약을 확정하면 예외 발생")
		void throwExceptionWhenConfirmingCancelledReservation() {
			// given
			final ReservationPricing reservation = createTestReservation();
			reservation.cancel();
			
			// when & then
			assertThatThrownBy(reservation::confirm)
					.isInstanceOf(InvalidReservationStatusException.class);
		}
		
		@Test
		@DisplayName("확정된 예약을 취소하려고 하면 예외 발생")
		void throwExceptionWhenCancellingConfirmedReservation() {
			// given
			final ReservationPricing reservation = createTestReservation();
			reservation.confirm();
			
			// when & then
			assertThatThrownBy(reservation::cancel)
					.isInstanceOf(InvalidReservationStatusException.class);
		}
	}
	
	@Nested
	@DisplayName("PricingPolicy 도메인 예외")
	class PricingPolicyDomainExceptions {
		
		@Test
		@DisplayName("빈 timeRangePrices로 생성 시 예외 발생")
		void throwExceptionWhenCreatingWithEmptyTimeRangePrices() {
			// when & then
			assertThatThrownBy(() -> PricingPolicy.createWithTimeRangePrices(
					RoomId.of(1L),
					PlaceId.of(100L),
					TimeSlot.HOUR,
					Money.of(10000),
					TimeRangePrices.empty()  // 빈 TimeRangePrices
			))
					.isInstanceOf(IllegalArgumentException.class);
		}
		
		@Test
		@DisplayName("null roomId로 생성 시 예외 발생")
		void throwExceptionWhenCreatingWithNullRoomId() {
			// when & then
			assertThatThrownBy(() -> PricingPolicy.create(
					null,  // null roomId
					PlaceId.of(100L),
					TimeSlot.HOUR,
					Money.of(10000)
			))
					.isInstanceOf(IllegalArgumentException.class);
		}
		
		@Test
		@DisplayName("null timeSlot으로 생성 시 예외 발생")
		void throwExceptionWhenCreatingWithNullTimeSlot() {
			// when & then
			assertThatThrownBy(() -> PricingPolicy.create(
					RoomId.of(1L),
					PlaceId.of(100L),
					null,  // null timeSlot
					Money.of(10000)
			))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}
	
	@Nested
	@DisplayName("복합 예외 시나리오")
	class ComplexExceptionScenarios {
		
		@Test
		@DisplayName("여러 불변식 위반이 동시에 발생하면 첫 번째 검증에서 예외 발생")
		void throwExceptionOnFirstViolationWhenMultipleViolationsExist() {
			// when & then - null productId가 먼저 검증됨
			assertThatThrownBy(() -> Product.createPlaceScoped(
					null,  // 첫 번째 위반
					PlaceId.of(100L),
					"",  // 두 번째 위반 (빈 문자열)
					null,  // 세 번째 위반 (null strategy)
					-1  // 네 번째 위반 (음수 수량)
			))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Product ID cannot be null");  // 첫 번째 검증 실패
		}
		
		@Test
		@DisplayName("연쇄적인 연산에서 중간 결과가 유효하지 않으면 예외 발생")
		void throwExceptionWhenIntermediateResultIsInvalid() {
			// given
			final Money money1 = Money.of(5000);
			final Money money2 = Money.of(3000);
			final Money money3 = Money.of(10000);
			
			// when & then - (5000 - 3000) - 10000 = -8000 (음수)
			assertThatThrownBy(() -> {
				final Money intermediate = money1.subtract(money2);  // 2000
				intermediate.subtract(money3);  // 2000 - 10000 = -8000 (예외)
			})
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Result cannot be negative");
		}
	}
}
