package com.teambind.springproject.adapter.out.persistence.pricingpolicy;

import com.teambind.springproject.domain.pricingpolicy.TimeRangePrice;
import com.teambind.springproject.domain.shared.DayOfWeek;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.TimeRange;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Objects;

/**
 * TimeRangePrice를 JPA Embeddable로 매핑하기 위한 클래스.
 */
@Embeddable
public class TimeRangePriceEmbeddable {
	
	@Enumerated(EnumType.STRING)
	@Column(name = "day_of_week", nullable = false, length = 20)
	private DayOfWeek dayOfWeek;
	
	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;
	
	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;
	
	@Column(name = "price_per_slot", nullable = false, precision = 19, scale = 2)
	private BigDecimal pricePerSlot;
	
	protected TimeRangePriceEmbeddable() {
		// JPA용 기본 생성자
	}
	
	public TimeRangePriceEmbeddable(
			final DayOfWeek dayOfWeek,
			final LocalTime startTime,
			final LocalTime endTime,
			final BigDecimal pricePerSlot) {
		this.dayOfWeek = dayOfWeek;
		this.startTime = startTime;
		this.endTime = endTime;
		this.pricePerSlot = pricePerSlot;
	}
	
	/**
	 * Domain TimeRangePrice를 Embeddable로 변환합니다.
	 */
	public static TimeRangePriceEmbeddable fromDomain(final TimeRangePrice timeRangePrice) {
		return new TimeRangePriceEmbeddable(
				timeRangePrice.dayOfWeek(),
				timeRangePrice.timeRange().getStartTime(),
				timeRangePrice.timeRange().getEndTime(),
				timeRangePrice.pricePerSlot().getAmount()
		);
	}
	
	/**
	 * Embeddable을 Domain TimeRangePrice로 변환합니다.
	 */
	public TimeRangePrice toDomain() {
		return new TimeRangePrice(
				dayOfWeek,
				TimeRange.of(startTime, endTime),
				Money.of(pricePerSlot)
		);
	}
	
	public DayOfWeek getDayOfWeek() {
		return dayOfWeek;
	}
	
	public LocalTime getStartTime() {
		return startTime;
	}
	
	public LocalTime getEndTime() {
		return endTime;
	}
	
	public BigDecimal getPricePerSlot() {
		return pricePerSlot;
	}
	
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final TimeRangePriceEmbeddable that = (TimeRangePriceEmbeddable) o;
		return dayOfWeek == that.dayOfWeek
				&& Objects.equals(startTime, that.startTime)
				&& Objects.equals(endTime, that.endTime)
				&& Objects.equals(pricePerSlot, that.pricePerSlot);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(dayOfWeek, startTime, endTime, pricePerSlot);
	}
}
