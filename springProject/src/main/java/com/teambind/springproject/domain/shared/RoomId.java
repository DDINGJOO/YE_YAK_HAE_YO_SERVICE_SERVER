package com.teambind.springproject.domain.shared;

import java.util.Objects;

/**
 * 룸 ID를 표현하는 Value Object.
 */
public class RoomId {

  private final Long value;

  private RoomId(final Long value) {
    validateValue(value);
    this.value = value;
  }

  public static RoomId of(final Long value) {
    return new RoomId(value);
  }

  private void validateValue(final Long value) {
    if (value == null) {
      throw new IllegalArgumentException("Room ID cannot be null");
    }
    if (value <= 0) {
      throw new IllegalArgumentException("Room ID must be positive: " + value);
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
    final RoomId roomId = (RoomId) o;
    return Objects.equals(value, roomId.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "RoomId{" + value + "}";
  }
}
