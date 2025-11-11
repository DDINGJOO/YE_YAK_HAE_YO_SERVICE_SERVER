package com.teambind.springproject.application.dto.request;

import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.vo.ProductScope;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 상품 등록 요청 DTO.
 */
public record RegisterProductRequest(
    @NotNull(message = "Product scope is required")
    ProductScope scope,

    Long placeId,

    Long roomId,

    @NotBlank(message = "Product name is required")
    String name,

    @NotNull(message = "Pricing strategy is required")
    @Valid
    PricingStrategyDto pricingStrategy,

    @NotNull(message = "Total quantity is required")
    @Min(value = 0, message = "Total quantity must be greater than or equal to 0")
    Integer totalQuantity
) {

  /**
   * DTO를 Product 도메인 객체로 변환합니다.
   *
   * @return Product
   * @throws IllegalArgumentException Scope에 따라 placeId/roomId가 필수인데 누락된 경우
   */
  public Product toDomain() {
    validateScopeIds();

    return switch (scope) {
      case PLACE -> Product.createPlaceScoped(
          ProductId.of(null),  // Auto-generated
          PlaceId.of(placeId),
          name,
          pricingStrategy.toDomain(),
          totalQuantity
      );
      case ROOM -> Product.createRoomScoped(
          ProductId.of(null),
          PlaceId.of(placeId),
          RoomId.of(roomId),
          name,
          pricingStrategy.toDomain(),
          totalQuantity
      );
      case RESERVATION -> Product.createReservationScoped(
          ProductId.of(null),
          name,
          pricingStrategy.toDomain(),
          totalQuantity
      );
    };
  }

  /**
   * Scope에 따라 placeId/roomId 필수 여부를 검증합니다.
   *
   * @throws IllegalArgumentException Scope에 맞지 않는 ID가 제공된 경우
   */
  private void validateScopeIds() {
    switch (scope) {
      case PLACE -> {
        if (placeId == null) {
          throw new IllegalArgumentException("PlaceId is required for PLACE scope");
        }
        if (roomId != null) {
          throw new IllegalArgumentException("RoomId must be null for PLACE scope");
        }
      }
      case ROOM -> {
        if (placeId == null) {
          throw new IllegalArgumentException("PlaceId is required for ROOM scope");
        }
        if (roomId == null) {
          throw new IllegalArgumentException("RoomId is required for ROOM scope");
        }
      }
      case RESERVATION -> {
        if (placeId != null) {
          throw new IllegalArgumentException("PlaceId must be null for RESERVATION scope");
        }
        if (roomId != null) {
          throw new IllegalArgumentException("RoomId must be null for RESERVATION scope");
        }
      }
    }
  }
}
