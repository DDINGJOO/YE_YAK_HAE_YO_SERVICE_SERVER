package com.teambind.springproject.adapter.out.persistence.pricingpolicy;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * RoomId를 JPA Embeddable ID로 매핑하기 위한 클래스.
 */
@Embeddable
public class RoomIdEmbeddable implements Serializable {
	
	@Column(name = "room_id", nullable = false)
	private Long value;
	
	protected RoomIdEmbeddable() {
		// JPA용 기본 생성자
	}
	
	public RoomIdEmbeddable(final Long value) {
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
		final RoomIdEmbeddable that = (RoomIdEmbeddable) o;
		return Objects.equals(value, that.value);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
}
