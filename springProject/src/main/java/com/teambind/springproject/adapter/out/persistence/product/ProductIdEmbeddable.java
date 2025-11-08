package com.teambind.springproject.adapter.out.persistence.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * ProductId를 JPA Embeddable로 매핑하기 위한 클래스.
 */
@Embeddable
public class ProductIdEmbeddable {

  @Column(name = "product_id", nullable = false)
  private Long value;

  protected ProductIdEmbeddable() {
    // JPA용 기본 생성자
  }

  public ProductIdEmbeddable(final Long value) {
    this.value = value;
  }

  public Long getValue() {
    return value;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ProductIdEmbeddable that = (ProductIdEmbeddable) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
