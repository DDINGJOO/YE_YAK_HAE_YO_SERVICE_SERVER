package com.teambind.springproject.adapter.out.persistence.pricingpolicy;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * PlaceId를 JPA Embeddable로 매핑하기 위한 클래스.
 */
@Embeddable
public class PlaceIdEmbeddable {

  @Column(name = "place_id", nullable = false)
  private Long value;

  protected PlaceIdEmbeddable() {
    // JPA용 기본 생성자
  }

  public PlaceIdEmbeddable(final Long value) {
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
    final PlaceIdEmbeddable that = (PlaceIdEmbeddable) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
