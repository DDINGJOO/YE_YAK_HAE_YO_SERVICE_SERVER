package com.teambind.springproject.adapter.out.persistence.product;

import com.teambind.springproject.adapter.out.persistence.pricingpolicy.PlaceIdEmbeddable;
import com.teambind.springproject.adapter.out.persistence.pricingpolicy.RoomIdEmbeddable;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.ProductScope;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Product Aggregate의 JPA Entity.
 */
@Entity
@Table(name = "products")
public class ProductEntity {

  @Id
  @GeneratedValue(generator = "snowflake-id")
  @org.hibernate.annotations.GenericGenerator(
      name = "snowflake-id",
      type = com.teambind.springproject.common.util.generator.SnowflakeIdGenerator.class
  )
  @Column(name = "product_id")
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "scope", nullable = false, length = 20)
  private ProductScope scope;

  @Column(name = "place_id")
  private Long placeId;

  @Column(name = "room_id")
  private Long roomId;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Embedded
  private PricingStrategyEmbeddable pricingStrategy;

  @Column(name = "total_quantity", nullable = false)
  private Integer totalQuantity;

  protected ProductEntity() {
    // JPA용 기본 생성자
  }

  public ProductEntity(
      final Long id,
      final ProductScope scope,
      final Long placeId,
      final Long roomId,
      final String name,
      final PricingStrategyEmbeddable pricingStrategy,
      final Integer totalQuantity) {
    this.id = id;
    this.scope = scope;
    this.placeId = placeId;
    this.roomId = roomId;
    this.name = name;
    this.pricingStrategy = pricingStrategy;
    this.totalQuantity = totalQuantity;
  }

  /**
   * Domain Product를 Entity로 변환합니다.
   */
  public static ProductEntity fromDomain(final Product product) {
    return new ProductEntity(
        product.getProductId().getValue(),
        product.getScope(),
        product.getPlaceId() != null ? product.getPlaceId().getValue() : null,
        product.getRoomId() != null ? product.getRoomId().getValue() : null,
        product.getName(),
        PricingStrategyEmbeddable.fromDomain(product.getPricingStrategy()),
        product.getTotalQuantity()
    );
  }

  /**
   * Entity를 Domain Product로 변환합니다.
   */
  public Product toDomain() {
    final ProductId productId = ProductId.of(id);
    final PlaceId domainPlaceId = placeId != null ? PlaceId.of(placeId) : null;
    final RoomId domainRoomId = roomId != null ? RoomId.of(roomId) : null;

    return switch (scope) {
      case PLACE -> Product.createPlaceScoped(
          productId,
          domainPlaceId,
          name,
          pricingStrategy.toDomain(),
          totalQuantity
      );
      case ROOM -> Product.createRoomScoped(
          productId,
          domainPlaceId,
          domainRoomId,
          name,
          pricingStrategy.toDomain(),
          totalQuantity
      );
      case RESERVATION -> Product.createReservationScoped(
          productId,
          name,
          pricingStrategy.toDomain(),
          totalQuantity
      );
    };
  }

  public Long getId() {
    return id;
  }

  public ProductScope getScope() {
    return scope;
  }

  public Long getPlaceId() {
    return placeId;
  }

  public Long getRoomId() {
    return roomId;
  }

  public String getName() {
    return name;
  }

  public PricingStrategyEmbeddable getPricingStrategy() {
    return pricingStrategy;
  }

  public Integer getTotalQuantity() {
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
    final ProductEntity that = (ProductEntity) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
