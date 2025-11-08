package com.teambind.springproject.domain.shared;

import java.util.Objects;

/**
 * 장소 ID를 표현하는 Value Object.
 */
public class PlaceId {

  private final Long value;

  private PlaceId(final Long value) {
    validateValue(value);
    this.value = value;
  }

  public static PlaceId of(final Long value) {
    return new PlaceId(value);
  }

  private void validateValue(final Long value) {
    if (value == null) {
      throw new IllegalArgumentException("Place ID cannot be null");
    }
    if (value <= 0) {
      throw new IllegalArgumentException("Place ID must be positive: " + value);
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
    final PlaceId placeId = (PlaceId) o;
    return Objects.equals(value, placeId.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "PlaceId{" + value + "}";
  }
}
