package com.teambind.springproject.domain.shared;

import java.util.Objects;

/**
 * 상품 ID를 표현하는 Value Object.
 */
public class ProductId {

  private final Long value;

  private ProductId(final Long value) {
    validateValue(value);
    this.value = value;
  }

  public static ProductId of(final Long value) {
    return new ProductId(value);
  }

  private void validateValue(final Long value) {
    if (value == null) {
      throw new IllegalArgumentException("Product ID cannot be null");
    }
    if (value <= 0) {
      throw new IllegalArgumentException("Product ID must be positive: " + value);
    }
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
    final ProductId productId = (ProductId) o;
    return Objects.equals(value, productId.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "ProductId{" + value + "}";
  }
}
