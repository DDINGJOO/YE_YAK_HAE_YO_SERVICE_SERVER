package com.teambind.springproject.application.service.product;

import com.teambind.springproject.application.dto.response.ProductResponse;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.pricing.PricingStrategy;
import com.teambind.springproject.domain.product.vo.PricingType;
import com.teambind.springproject.domain.product.vo.ProductScope;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAvailableProductsForRoomService 단위 테스트")
class GetAvailableProductsForRoomServiceTest {

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private GetAvailableProductsForRoomService service;

	private PlaceId placeId;
	private RoomId roomId;

	@BeforeEach
	void setUp() {
		placeId = PlaceId.of(100L);
		roomId = RoomId.of(200L);
	}

	@Nested
	@DisplayName("getAvailableProducts 메서드는")
	class GetAvailableProductsMethod {

		@Test
		@DisplayName("룸에서 이용 가능한 모든 상품을 조회한다")
		void shouldReturnAllAvailableProducts() {
			// Given
			final Product roomProduct = Product.createRoomScoped(
					ProductId.of(1L),
					placeId,
					roomId,
					"룸 전용 화이트보드",
					PricingStrategy.oneTime(Money.of(BigDecimal.valueOf(15000))),
					3
			);

			final Product placeProduct = Product.createPlaceScoped(
					ProductId.of(2L),
					placeId,
					"공용 빔 프로젝터",
					PricingStrategy.simpleStock(Money.of(BigDecimal.valueOf(10000))),
					5
			);

			final Product reservationProduct = Product.createReservationScoped(
					ProductId.of(3L),
					"음료수",
					PricingStrategy.simpleStock(Money.of(BigDecimal.valueOf(2000))),
					100
			);

			final List<Product> products = List.of(roomProduct, placeProduct, reservationProduct);

			when(productRepository.findAccessibleProducts(any(PlaceId.class), any(RoomId.class)))
					.thenReturn(products);

			// When
			final List<ProductResponse> result = service.getAvailableProducts(roomId, placeId);

			// Then
			assertThat(result).hasSize(3);

			// ROOM Scope 상품 검증
			final ProductResponse roomResponse = result.get(0);
			assertThat(roomResponse.productId()).isEqualTo(1L);
			assertThat(roomResponse.scope()).isEqualTo(ProductScope.ROOM);
			assertThat(roomResponse.roomId()).isEqualTo(200L);
			assertThat(roomResponse.placeId()).isEqualTo(100L);
			assertThat(roomResponse.name()).isEqualTo("룸 전용 화이트보드");
			assertThat(roomResponse.pricingStrategy().pricingType()).isEqualTo(PricingType.ONE_TIME);
			assertThat(roomResponse.totalQuantity()).isEqualTo(3);

			// PLACE Scope 상품 검증
			final ProductResponse placeResponse = result.get(1);
			assertThat(placeResponse.productId()).isEqualTo(2L);
			assertThat(placeResponse.scope()).isEqualTo(ProductScope.PLACE);
			assertThat(placeResponse.placeId()).isEqualTo(100L);
			assertThat(placeResponse.roomId()).isNull();
			assertThat(placeResponse.name()).isEqualTo("공용 빔 프로젝터");
			assertThat(placeResponse.pricingStrategy().pricingType()).isEqualTo(PricingType.SIMPLE_STOCK);
			assertThat(placeResponse.totalQuantity()).isEqualTo(5);

			// RESERVATION Scope 상품 검증
			final ProductResponse reservationResponse = result.get(2);
			assertThat(reservationResponse.productId()).isEqualTo(3L);
			assertThat(reservationResponse.scope()).isEqualTo(ProductScope.RESERVATION);
			assertThat(reservationResponse.placeId()).isNull();
			assertThat(reservationResponse.roomId()).isNull();
			assertThat(reservationResponse.name()).isEqualTo("음료수");
			assertThat(reservationResponse.pricingStrategy().pricingType()).isEqualTo(PricingType.SIMPLE_STOCK);
			assertThat(reservationResponse.totalQuantity()).isEqualTo(100);

			verify(productRepository).findAccessibleProducts(placeId, roomId);
		}

		@Test
		@DisplayName("이용 가능한 상품이 없으면 빈 리스트를 반환한다")
		void shouldReturnEmptyListWhenNoProductsAvailable() {
			// Given
			when(productRepository.findAccessibleProducts(any(PlaceId.class), any(RoomId.class)))
					.thenReturn(List.of());

			// When
			final List<ProductResponse> result = service.getAvailableProducts(roomId, placeId);

			// Then
			assertThat(result).isEmpty();
			verify(productRepository).findAccessibleProducts(placeId, roomId);
		}

		@Test
		@DisplayName("ROOM Scope 상품만 존재하는 경우 정상 조회된다")
		void shouldReturnOnlyRoomScopedProducts() {
			// Given
			final Product roomProduct = Product.createRoomScoped(
					ProductId.of(1L),
					placeId,
					roomId,
					"룸 전용 상품",
					PricingStrategy.oneTime(Money.of(BigDecimal.valueOf(10000))),
					5
			);

			when(productRepository.findAccessibleProducts(any(PlaceId.class), any(RoomId.class)))
					.thenReturn(List.of(roomProduct));

			// When
			final List<ProductResponse> result = service.getAvailableProducts(roomId, placeId);

			// Then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).scope()).isEqualTo(ProductScope.ROOM);
			assertThat(result.get(0).roomId()).isEqualTo(200L);
		}

		@Test
		@DisplayName("RESERVATION Scope 상품만 존재하는 경우 정상 조회된다")
		void shouldReturnOnlyReservationScopedProducts() {
			// Given
			final Product reservationProduct = Product.createReservationScoped(
					ProductId.of(1L),
					"예약 전용 상품",
					PricingStrategy.simpleStock(Money.of(BigDecimal.valueOf(5000))),
					50
			);

			when(productRepository.findAccessibleProducts(any(PlaceId.class), any(RoomId.class)))
					.thenReturn(List.of(reservationProduct));

			// When
			final List<ProductResponse> result = service.getAvailableProducts(roomId, placeId);

			// Then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).scope()).isEqualTo(ProductScope.RESERVATION);
			assertThat(result.get(0).placeId()).isNull();
			assertThat(result.get(0).roomId()).isNull();
		}

		@Test
		@DisplayName("Repository 호출 시 올바른 파라미터를 전달한다")
		void shouldPassCorrectParametersToRepository() {
			// Given
			when(productRepository.findAccessibleProducts(placeId, roomId))
					.thenReturn(List.of());

			// When
			service.getAvailableProducts(roomId, placeId);

			// Then
			verify(productRepository).findAccessibleProducts(placeId, roomId);
		}
	}
}