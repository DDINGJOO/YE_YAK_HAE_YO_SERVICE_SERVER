package com.teambind.springproject.adapter.out.persistence.product;

import com.teambind.springproject.domain.product.RoomAllowedProduct;
import com.teambind.springproject.domain.shared.ProductId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 룸별 허용 상품 매핑의 JPA Entity.
 */
@Entity
@Table(name = "room_allowed_products")
public class RoomAllowedProductEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "room_id", nullable = false)
  private Long roomId;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  protected RoomAllowedProductEntity() {
    // JPA를 위한 기본 생성자
  }

  private RoomAllowedProductEntity(
      final Long id,
      final Long roomId,
      final Long productId,
      final LocalDateTime createdAt) {
    this.id = id;
    this.roomId = roomId;
    this.productId = productId;
    this.createdAt = createdAt;
  }

  /**
   * Domain Model로부터 Entity를 생성합니다.
   *
   * @param roomAllowedProduct 룸별 허용 상품
   * @return RoomAllowedProductEntity
   */
  public static RoomAllowedProductEntity from(final RoomAllowedProduct roomAllowedProduct) {
    return new RoomAllowedProductEntity(
        null,
        roomAllowedProduct.roomId(),
        roomAllowedProduct.productId().getValue(),
        LocalDateTime.now()
    );
  }

  /**
   * Entity를 Domain Model로 변환합니다.
   *
   * @return RoomAllowedProduct
   */
  public RoomAllowedProduct toDomain() {
    return new RoomAllowedProduct(
        this.roomId,
        ProductId.of(this.productId)
    );
  }

  public Long getId() {
    return id;
  }

  public Long getRoomId() {
    return roomId;
  }

  public Long getProductId() {
    return productId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final RoomAllowedProductEntity that = (RoomAllowedProductEntity) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "RoomAllowedProductEntity{"
        + "id=" + id
        + ", roomId=" + roomId
        + ", productId=" + productId
        + ", createdAt=" + createdAt
        + '}';
  }
}
