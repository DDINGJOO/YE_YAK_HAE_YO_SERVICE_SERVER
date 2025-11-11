package com.teambind.springproject.domain.product;

import com.teambind.springproject.domain.product.pricing.PricingStrategy;
import com.teambind.springproject.domain.product.vo.ProductPriceBreakdown;
import com.teambind.springproject.domain.product.vo.ProductScope;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import java.util.Objects;

/**
 * 추가상품 Aggregate Root.
 * 플레이스, 룸, 예약 범위에서 사용 가능한 추가상품을 관리합니다.
 */
public class Product {

  private final ProductId productId;
  private final ProductScope scope;
  private final PlaceId placeId;
  private final RoomId roomId;
  private String name;
  private PricingStrategy pricingStrategy;
  private int totalQuantity;

  private Product(
      final ProductId productId,
      final ProductScope scope,
      final PlaceId placeId,
      final RoomId roomId,
      final String name,
      final PricingStrategy pricingStrategy,
      final int totalQuantity) {
    validateProductId(productId);
    validateScope(scope);
    validateScopeIds(placeId, roomId, scope);
    validateName(name);
    validatePricingStrategy(pricingStrategy);
    validateTotalQuantity(totalQuantity);

    this.productId = productId;
    this.scope = scope;
    this.placeId = placeId;
    this.roomId = roomId;
    this.name = name;
    this.pricingStrategy = pricingStrategy;
    this.totalQuantity = totalQuantity;
  }

  /**
   * PLACE 범위 상품 생성.
   *
   * @param productId 상품 ID
   * @param placeId 플레이스 ID
   * @param name 상품명
   * @param pricingStrategy 가격 전략
   * @param totalQuantity 총 수량
   * @return Product
   */
  public static Product createPlaceScoped(
      final ProductId productId,
      final PlaceId placeId,
      final String name,
      final PricingStrategy pricingStrategy,
      final int totalQuantity) {
    return new Product(productId, ProductScope.PLACE, placeId, null, name, pricingStrategy,
        totalQuantity);
  }

  /**
   * ROOM 범위 상품 생성.
   *
   * @param productId 상품 ID
   * @param placeId 플레이스 ID
   * @param roomId 룸 ID
   * @param name 상품명
   * @param pricingStrategy 가격 전략
   * @param totalQuantity 총 수량
   * @return Product
   */
  public static Product createRoomScoped(
      final ProductId productId,
      final PlaceId placeId,
      final RoomId roomId,
      final String name,
      final PricingStrategy pricingStrategy,
      final int totalQuantity) {
    return new Product(productId, ProductScope.ROOM, placeId, roomId, name, pricingStrategy,
        totalQuantity);
  }

  /**
   * RESERVATION 범위 상품 생성.
   *
   * @param productId 상품 ID
   * @param name 상품명
   * @param pricingStrategy 가격 전략
   * @param totalQuantity 총 수량
   * @return Product
   */
  public static Product createReservationScoped(
      final ProductId productId,
      final String name,
      final PricingStrategy pricingStrategy,
      final int totalQuantity) {
    return new Product(productId, ProductScope.RESERVATION, null, null, name, pricingStrategy,
        totalQuantity);
  }

  private void validateProductId(final ProductId productId) {
    if (productId == null) {
      throw new IllegalArgumentException("Product ID cannot be null");
    }
  }

  private void validateScope(final ProductScope scope) {
    if (scope == null) {
      throw new IllegalArgumentException("Product scope cannot be null");
    }
  }

  private void validateScopeIds(
      final PlaceId placeId,
      final RoomId roomId,
      final ProductScope scope) {
    switch (scope) {
      case PLACE -> {
        if (placeId == null) {
          throw new IllegalArgumentException("PLACE scope requires placeId");
        }
        if (roomId != null) {
          throw new IllegalArgumentException("PLACE scope must not have roomId");
        }
      }
      case ROOM -> {
        if (placeId == null) {
          throw new IllegalArgumentException("ROOM scope requires placeId");
        }
        if (roomId == null) {
          throw new IllegalArgumentException("ROOM scope requires roomId");
        }
      }
      case RESERVATION -> {
        if (placeId != null) {
          throw new IllegalArgumentException("RESERVATION scope must not have placeId");
        }
        if (roomId != null) {
          throw new IllegalArgumentException("RESERVATION scope must not have roomId");
        }
      }
    }
  }

  private void validateName(final String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Product name cannot be null or empty");
    }
  }

  private void validatePricingStrategy(final PricingStrategy pricingStrategy) {
    if (pricingStrategy == null) {
      throw new IllegalArgumentException("Pricing strategy cannot be null");
    }
  }

  private void validateTotalQuantity(final int totalQuantity) {
    if (totalQuantity < 0) {
      throw new IllegalArgumentException("Total quantity cannot be negative: " + totalQuantity);
    }
  }

  /**
   * 상품명을 변경합니다.
   *
   * @param newName 새로운 상품명
   */
  public void updateName(final String newName) {
    validateName(newName);
    this.name = newName;
  }

  /**
   * 가격 전략을 변경합니다.
   *
   * @param newPricingStrategy 새로운 가격 전략
   */
  public void updatePricingStrategy(final PricingStrategy newPricingStrategy) {
    validatePricingStrategy(newPricingStrategy);
    this.pricingStrategy = newPricingStrategy;
  }

  /**
   * 총 수량을 변경합니다.
   *
   * @param newTotalQuantity 새로운 총 수량
   */
  public void updateTotalQuantity(final int newTotalQuantity) {
    validateTotalQuantity(newTotalQuantity);
    this.totalQuantity = newTotalQuantity;
  }

  /**
   * 상품 가격을 계산하고 가격 내역을 반환합니다.
   *
   * @param quantity 수량
   * @return 상품 가격 내역
   */
  public ProductPriceBreakdown calculatePrice(final int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be positive: " + quantity);
    }

    final Money unitPrice = pricingStrategy.calculate(quantity);
    final Money totalPrice = unitPrice.multiply(quantity);

    return new ProductPriceBreakdown(
        productId,
        name,
        quantity,
        unitPrice,
        totalPrice,
        pricingStrategy.getPricingType()
    );
  }

  public ProductId getProductId() {
    return productId;
  }

  public ProductScope getScope() {
    return scope;
  }

  public PlaceId getPlaceId() {
    return placeId;
  }

  public RoomId getRoomId() {
    return roomId;
  }

  public String getName() {
    return name;
  }

  public PricingStrategy getPricingStrategy() {
    return pricingStrategy;
  }

  public int getTotalQuantity() {
    return totalQuantity;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Product product = (Product) o;
    return Objects.equals(productId, product.productId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productId);
  }

  @Override
  public String toString() {
    return "Product{"
        + "productId=" + productId
        + ", scope=" + scope
        + ", placeId=" + placeId
        + ", roomId=" + roomId
        + ", name='" + name + '\''
        + ", pricingStrategy=" + pricingStrategy
        + ", totalQuantity=" + totalQuantity
        + '}';
  }
}
