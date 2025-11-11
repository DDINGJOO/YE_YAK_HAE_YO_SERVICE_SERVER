package com.teambind.springproject.adapter.out.persistence.pricingpolicy;

import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrice;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrices;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * PricingPolicy Aggregate의 JPA Entity.
 */
@Entity
@Table(name = "pricing_policies")
public class PricingPolicyEntity {
	
	@EmbeddedId
	private RoomIdEmbeddable roomId;
	
	@Embedded
	private PlaceIdEmbeddable placeId;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "time_slot", nullable = false, length = 20)
	private TimeSlot timeSlot;
	
	@Column(name = "default_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal defaultPrice;
	
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "time_range_prices",
			joinColumns = @JoinColumn(name = "room_id")
	)
	private List<TimeRangePriceEmbeddable> timeRangePrices = new ArrayList<>();
	
	protected PricingPolicyEntity() {
		// JPA용 기본 생성자
	}
	
	public PricingPolicyEntity(
			final RoomIdEmbeddable roomId,
			final PlaceIdEmbeddable placeId,
			final TimeSlot timeSlot,
			final BigDecimal defaultPrice,
			final List<TimeRangePriceEmbeddable> timeRangePrices) {
		this.roomId = roomId;
		this.placeId = placeId;
		this.timeSlot = timeSlot;
		this.defaultPrice = defaultPrice;
		this.timeRangePrices = timeRangePrices != null ? timeRangePrices : new ArrayList<>();
	}
	
	/**
	 * Domain PricingPolicy를 Entity로 변환합니다.
	 */
	public static PricingPolicyEntity fromDomain(final PricingPolicy policy) {
		final List<TimeRangePriceEmbeddable> timeRangePriceEmbeddables = policy.getTimeRangePrices()
				.getPrices()
				.stream()
				.map(TimeRangePriceEmbeddable::fromDomain)
				.collect(Collectors.toList());
		
		return new PricingPolicyEntity(
				new RoomIdEmbeddable(policy.getRoomId().getValue()),
				new PlaceIdEmbeddable(policy.getPlaceId().getValue()),
				policy.getTimeSlot(),
				policy.getDefaultPrice().getAmount(),
				timeRangePriceEmbeddables
		);
	}
	
	/**
	 * Entity를 Domain PricingPolicy로 변환합니다.
	 */
	public PricingPolicy toDomain() {
		final List<TimeRangePrice> timeRangePriceList = timeRangePrices.stream()
				.map(TimeRangePriceEmbeddable::toDomain)
				.collect(Collectors.toList());
		
		return PricingPolicy.createWithTimeRangePrices(
				RoomId.of(roomId.getValue()),
				PlaceId.of(placeId.getValue()),
				timeSlot,
				Money.of(defaultPrice),
				TimeRangePrices.of(timeRangePriceList)
		);
	}
	
	public RoomIdEmbeddable getRoomId() {
		return roomId;
	}
	
	public PlaceIdEmbeddable getPlaceId() {
		return placeId;
	}
	
	public TimeSlot getTimeSlot() {
		return timeSlot;
	}
	
	public BigDecimal getDefaultPrice() {
		return defaultPrice;
	}
	
	public List<TimeRangePriceEmbeddable> getTimeRangePrices() {
		return timeRangePrices;
	}
	
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final PricingPolicyEntity that = (PricingPolicyEntity) o;
		return Objects.equals(roomId, that.roomId);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(roomId);
	}
}
