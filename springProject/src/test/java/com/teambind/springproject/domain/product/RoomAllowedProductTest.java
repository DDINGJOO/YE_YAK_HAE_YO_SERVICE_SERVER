package com.teambind.springproject.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teambind.springproject.domain.shared.ProductId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RoomAllowedProduct Value Object 테스트")
class RoomAllowedProductTest {

  @Nested
  @DisplayName("생성 테스트")
  class CreationTests {

    @Test
    @DisplayName("정상적인 값으로 생성 성공")
    void createSuccessfully() {
      // given
      final Long roomId = 1L;
      final ProductId productId = ProductId.of(100L);

      // when
      final RoomAllowedProduct roomAllowedProduct = new RoomAllowedProduct(roomId, productId);

      // then
      assertThat(roomAllowedProduct.roomId()).isEqualTo(roomId);
      assertThat(roomAllowedProduct.productId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("null roomId로 생성 시 예외 발생")
    void throwExceptionWhenRoomIdIsNull() {
      // when & then
      assertThatThrownBy(() -> new RoomAllowedProduct(null, ProductId.of(1L)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Room ID cannot be null");
    }

    @Test
    @DisplayName("0인 roomId로 생성 시 예외 발생")
    void throwExceptionWhenRoomIdIsZero() {
      // when & then
      assertThatThrownBy(() -> new RoomAllowedProduct(0L, ProductId.of(1L)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Room ID must be positive: 0");
    }

    @Test
    @DisplayName("음수 roomId로 생성 시 예외 발생")
    void throwExceptionWhenRoomIdIsNegative() {
      // when & then
      assertThatThrownBy(() -> new RoomAllowedProduct(-1L, ProductId.of(1L)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Room ID must be positive: -1");
    }

    @Test
    @DisplayName("null productId로 생성 시 예외 발생")
    void throwExceptionWhenProductIdIsNull() {
      // when & then
      assertThatThrownBy(() -> new RoomAllowedProduct(1L, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Product ID cannot be null");
    }
  }

  @Nested
  @DisplayName("불변성 테스트")
  class ImmutabilityTests {

    @Test
    @DisplayName("Record는 불변이므로 값 변경 불가")
    void recordIsImmutable() {
      // given
      final RoomAllowedProduct roomAllowedProduct = new RoomAllowedProduct(1L, ProductId.of(1L));

      // when & then
      assertThat(roomAllowedProduct.roomId()).isEqualTo(1L);
      assertThat(roomAllowedProduct.productId()).isEqualTo(ProductId.of(1L));
      // Record는 setter가 없으므로 값 변경 시도 자체가 컴파일 에러
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTests {

    @Test
    @DisplayName("모든 필드가 같으면 동일")
    void equalityBasedOnAllFields() {
      // given
      final RoomAllowedProduct product1 = new RoomAllowedProduct(1L, ProductId.of(100L));
      final RoomAllowedProduct product2 = new RoomAllowedProduct(1L, ProductId.of(100L));

      // when & then
      assertThat(product1).isEqualTo(product2);
      assertThat(product1.hashCode()).isEqualTo(product2.hashCode());
    }

    @Test
    @DisplayName("roomId가 다르면 다름")
    void inequalityWhenRoomIdDiffers() {
      // given
      final RoomAllowedProduct product1 = new RoomAllowedProduct(1L, ProductId.of(100L));
      final RoomAllowedProduct product2 = new RoomAllowedProduct(2L, ProductId.of(100L));

      // when & then
      assertThat(product1).isNotEqualTo(product2);
    }

    @Test
    @DisplayName("productId가 다르면 다름")
    void inequalityWhenProductIdDiffers() {
      // given
      final RoomAllowedProduct product1 = new RoomAllowedProduct(1L, ProductId.of(100L));
      final RoomAllowedProduct product2 = new RoomAllowedProduct(1L, ProductId.of(200L));

      // when & then
      assertThat(product1).isNotEqualTo(product2);
    }

    @Test
    @DisplayName("모든 필드가 다르면 다름")
    void inequalityWhenAllFieldsDiffer() {
      // given
      final RoomAllowedProduct product1 = new RoomAllowedProduct(1L, ProductId.of(100L));
      final RoomAllowedProduct product2 = new RoomAllowedProduct(2L, ProductId.of(200L));

      // when & then
      assertThat(product1).isNotEqualTo(product2);
    }
  }

  @Nested
  @DisplayName("비즈니스 시나리오 테스트")
  class BusinessScenarioTests {

    @Test
    @DisplayName("플레이스 어드민이 룸에 PLACE 상품 허용 설정")
    void allowPlaceProductForRoom() {
      // given
      final Long roomId = 1L;
      final ProductId beamProjectorProductId = ProductId.of(10L);

      // when
      final RoomAllowedProduct allowedProduct = new RoomAllowedProduct(
          roomId,
          beamProjectorProductId
      );

      // then
      assertThat(allowedProduct.roomId()).isEqualTo(roomId);
      assertThat(allowedProduct.productId()).isEqualTo(beamProjectorProductId);
    }

    @Test
    @DisplayName("여러 룸에 동일한 상품을 개별적으로 허용 설정")
    void allowSameProductForDifferentRooms() {
      // given
      final ProductId drinkProductId = ProductId.of(50L);

      // when
      final RoomAllowedProduct room1Allowed = new RoomAllowedProduct(1L, drinkProductId);
      final RoomAllowedProduct room2Allowed = new RoomAllowedProduct(2L, drinkProductId);
      final RoomAllowedProduct room3Allowed = new RoomAllowedProduct(3L, drinkProductId);

      // then
      assertThat(room1Allowed.productId()).isEqualTo(drinkProductId);
      assertThat(room2Allowed.productId()).isEqualTo(drinkProductId);
      assertThat(room3Allowed.productId()).isEqualTo(drinkProductId);

      assertThat(room1Allowed).isNotEqualTo(room2Allowed);
      assertThat(room2Allowed).isNotEqualTo(room3Allowed);
    }

    @Test
    @DisplayName("하나의 룸에 여러 상품을 허용 설정")
    void allowMultipleProductsForOneRoom() {
      // given
      final Long roomId = 5L;
      final ProductId product1 = ProductId.of(10L);
      final ProductId product2 = ProductId.of(20L);
      final ProductId product3 = ProductId.of(30L);

      // when
      final RoomAllowedProduct allowed1 = new RoomAllowedProduct(roomId, product1);
      final RoomAllowedProduct allowed2 = new RoomAllowedProduct(roomId, product2);
      final RoomAllowedProduct allowed3 = new RoomAllowedProduct(roomId, product3);

      // then
      assertThat(allowed1.roomId()).isEqualTo(roomId);
      assertThat(allowed2.roomId()).isEqualTo(roomId);
      assertThat(allowed3.roomId()).isEqualTo(roomId);

      assertThat(allowed1).isNotEqualTo(allowed2);
      assertThat(allowed2).isNotEqualTo(allowed3);
    }
  }
}