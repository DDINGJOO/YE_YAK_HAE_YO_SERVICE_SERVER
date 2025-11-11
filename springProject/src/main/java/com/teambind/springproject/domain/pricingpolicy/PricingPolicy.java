package com.teambind.springproject.domain.pricingpolicy;

import com.teambind.springproject.domain.shared.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 가격 정책 Aggregate Root.
 * 특정 룸에 대한 기본 가격과 시간대별 가격을 관리합니다.
 */
public class PricingPolicy {
	
	private final RoomId roomId;
	private final PlaceId placeId;
	private TimeSlot timeSlot;
	private Money defaultPrice;
	private TimeRangePrices timeRangePrices;
	
	private PricingPolicy(
			final RoomId roomId,
			final PlaceId placeId,
			final TimeSlot timeSlot,
			final Money defaultPrice,
			final TimeRangePrices timeRangePrices) {
		validateRoomId(roomId);
		validatePlaceId(placeId);
		validateTimeSlot(timeSlot);
		validateDefaultPrice(defaultPrice);
		validateTimeRangePrices(timeRangePrices);
		
		this.roomId = roomId;
		this.placeId = placeId;
		this.timeSlot = timeSlot;
		this.defaultPrice = defaultPrice;
		this.timeRangePrices = timeRangePrices;
	}
	
	/**
	 * 기본 가격만 설정된 가격 정책을 생성합니다.
	 *
	 * @param roomId       룸 ID
	 * @param placeId      장소 ID
	 * @param timeSlot     시간 단위
	 * @param defaultPrice 기본 가격
	 * @return PricingPolicy
	 */
	public static PricingPolicy create(
			final RoomId roomId,
			final PlaceId placeId,
			final TimeSlot timeSlot,
			final Money defaultPrice) {
		return new PricingPolicy(roomId, placeId, timeSlot, defaultPrice, TimeRangePrices.empty());
	}
	
	/**
	 * 시간대별 가격이 포함된 가격 정책을 생성합니다.
	 *
	 * @param roomId          룸 ID
	 * @param placeId         장소 ID
	 * @param timeSlot        시간 단위
	 * @param defaultPrice    기본 가격
	 * @param timeRangePrices 시간대별 가격
	 * @return PricingPolicy
	 */
	public static PricingPolicy createWithTimeRangePrices(
			final RoomId roomId,
			final PlaceId placeId,
			final TimeSlot timeSlot,
			final Money defaultPrice,
			final TimeRangePrices timeRangePrices) {
		return new PricingPolicy(roomId, placeId, timeSlot, defaultPrice, timeRangePrices);
	}
	
	private void validateRoomId(final RoomId roomId) {
		if (roomId == null) {
			throw new IllegalArgumentException("Room ID cannot be null");
		}
	}
	
	private void validatePlaceId(final PlaceId placeId) {
		if (placeId == null) {
			throw new IllegalArgumentException("Place ID cannot be null");
		}
	}
	
	private void validateTimeSlot(final TimeSlot timeSlot) {
		if (timeSlot == null) {
			throw new IllegalArgumentException("TimeSlot cannot be null");
		}
	}
	
	private void validateDefaultPrice(final Money defaultPrice) {
		if (defaultPrice == null) {
			throw new IllegalArgumentException("Default price cannot be null");
		}
	}
	
	private void validateTimeRangePrices(final TimeRangePrices timeRangePrices) {
		if (timeRangePrices == null) {
			throw new IllegalArgumentException("Time range prices cannot be null");
		}
	}
	
	/**
	 * 기본 가격을 변경합니다.
	 *
	 * @param newDefaultPrice 새로운 기본 가격
	 */
	public void updateDefaultPrice(final Money newDefaultPrice) {
		validateDefaultPrice(newDefaultPrice);
		this.defaultPrice = newDefaultPrice;
	}
	
	/**
	 * 시간대별 가격을 재설정합니다.
	 *
	 * @param newTimeRangePrices 새로운 시간대별 가격
	 */
	public void resetPrices(final TimeRangePrices newTimeRangePrices) {
		validateTimeRangePrices(newTimeRangePrices);
		this.timeRangePrices = newTimeRangePrices;
	}
	
	/**
	 * TimeSlot을 변경합니다.
	 * Room의 운영 시간 정책 변경 시 사용됩니다.
	 * 기존 예약에는 영향을 주지 않으며, 신규 예약부터 새로운 TimeSlot이 적용됩니다.
	 *
	 * @param newTimeSlot 새로운 TimeSlot
	 */
	public void updateTimeSlot(final TimeSlot newTimeSlot) {
		validateTimeSlot(newTimeSlot);
		
		if (this.timeSlot.equals(newTimeSlot)) {
			return; // 동일하면 변경하지 않음
		}
		
		this.timeSlot = newTimeSlot;
	}
	
	/**
	 * 특정 예약 기간에 대한 가격 내역을 계산합니다.
	 *
	 * @param startDateTime 시작 날짜/시간
	 * @param endDateTime   종료 날짜/시간
	 * @return 가격 내역
	 */
	public PriceBreakdown calculatePriceBreakdown(
			final LocalDateTime startDateTime,
			final LocalDateTime endDateTime) {
		if (startDateTime == null) {
			throw new IllegalArgumentException("Start date time cannot be null");
		}
		if (endDateTime == null) {
			throw new IllegalArgumentException("End date time cannot be null");
		}
		if (startDateTime.isAfter(endDateTime) || startDateTime.equals(endDateTime)) {
			throw new IllegalArgumentException("Start date time must be before end date time");
		}
		
		final List<SlotPrice> slotPrices = new ArrayList<>();
		LocalDateTime currentSlot = startDateTime;
		
		while (currentSlot.isBefore(endDateTime)) {
			final DayOfWeek dayOfWeek = DayOfWeek.from(currentSlot.getDayOfWeek());
			final LocalTime time = currentSlot.toLocalTime();
			final Money priceForSlot = findPriceForSlot(dayOfWeek, time);
			
			slotPrices.add(new SlotPrice(currentSlot, priceForSlot));
			currentSlot = currentSlot.plusMinutes(timeSlot.getMinutes());
		}
		
		return PriceBreakdown.of(slotPrices);
	}
	
	private Money findPriceForSlot(final DayOfWeek dayOfWeek, final LocalTime time) {
		return timeRangePrices.findPriceForSlot(dayOfWeek, time)
				.orElse(defaultPrice);
	}
	
	public RoomId getRoomId() {
		return roomId;
	}
	
	public PlaceId getPlaceId() {
		return placeId;
	}
	
	public TimeSlot getTimeSlot() {
		return timeSlot;
	}
	
	public Money getDefaultPrice() {
		return defaultPrice;
	}
	
	public TimeRangePrices getTimeRangePrices() {
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
		final PricingPolicy that = (PricingPolicy) o;
		return Objects.equals(roomId, that.roomId);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(roomId);
	}
	
	@Override
	public String toString() {
		return "PricingPolicy{"
				+ "roomId=" + roomId
				+ ", placeId=" + placeId
				+ ", timeSlot=" + timeSlot
				+ ", defaultPrice=" + defaultPrice
				+ ", timeRangePrices=" + timeRangePrices
				+ '}';
	}
	
	/**
	 * 특정 시간 슬롯의 가격을 표현하는 Value Object.
	 */
	public record SlotPrice(LocalDateTime slotTime, Money price) {
		public SlotPrice {
			if (slotTime == null) {
				throw new IllegalArgumentException("Slot time cannot be null");
			}
			if (price == null) {
				throw new IllegalArgumentException("Price cannot be null");
			}
		}
	}
	
	/**
	 * 가격 내역을 표현하는 Value Object.
	 */
	public static class PriceBreakdown {
		private final List<SlotPrice> slotPrices;
		private final Money totalPrice;
		
		private PriceBreakdown(final List<SlotPrice> slotPrices) {
			if (slotPrices == null || slotPrices.isEmpty()) {
				throw new IllegalArgumentException("Slot prices cannot be null or empty");
			}
			this.slotPrices = Collections.unmodifiableList(new ArrayList<>(slotPrices));
			this.totalPrice = calculateTotal(slotPrices);
		}
		
		public static PriceBreakdown of(final List<SlotPrice> slotPrices) {
			return new PriceBreakdown(slotPrices);
		}
		
		private Money calculateTotal(final List<SlotPrice> slotPrices) {
			Money total = Money.ZERO;
			for (final SlotPrice slotPrice : slotPrices) {
				total = total.add(slotPrice.price());
			}
			return total;
		}
		
		public List<SlotPrice> getSlotPrices() {
			return slotPrices;
		}
		
		public Money getTotalPrice() {
			return totalPrice;
		}
		
		public int getSlotCount() {
			return slotPrices.size();
		}
		
		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			final PriceBreakdown that = (PriceBreakdown) o;
			return Objects.equals(slotPrices, that.slotPrices)
					&& Objects.equals(totalPrice, that.totalPrice);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(slotPrices, totalPrice);
		}
		
		@Override
		public String toString() {
			return "PriceBreakdown{"
					+ "slotCount=" + slotPrices.size()
					+ ", totalPrice=" + totalPrice
					+ '}';
		}
	}
}
