package com.teambind.springproject.domain.shared;

import java.util.Objects;

/**
 * 예약 ID를 표현하는 Value Object.
 */
public class ReservationId {

  private final Long value;

  private ReservationId(final Long value) {
    validateValue(value);
    this.value = value;
  }

  public static ReservationId of(final Long value) {
    return new ReservationId(value);
  }

  private void validateValue(final Long value) {
    if (value == null) {
      throw new IllegalArgumentException("Reservation ID cannot be null");
    }
    if (value <= 0) {
      throw new IllegalArgumentException("Reservation ID must be positive: " + value);
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
    final ReservationId that = (ReservationId) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "ReservationId{" + value + "}";
  }
}
