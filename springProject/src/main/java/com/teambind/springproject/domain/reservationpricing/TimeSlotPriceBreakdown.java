package com.teambind.springproject.domain.reservationpricing;

import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.TimeSlot;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 시간대별 가격 내역을 나타내는 Value Object (Record).
 * 예약 가격 스냅샷에 포함되는 불변 객체입니다.
 *
 * @param slotPrices 각 시간 슬롯별 가격 (LocalDateTime -> Money)
 * @param timeSlot   시간 단위 (HOUR 또는 HALFHOUR)
 */
public record TimeSlotPriceBreakdown(
		Map<LocalDateTime, Money> slotPrices,
		TimeSlot timeSlot
) {
	
	/**
	 * Compact Constructor - 불변식 검증.
	 */
	public TimeSlotPriceBreakdown {
		if (slotPrices == null) {
			throw new IllegalArgumentException("Slot prices cannot be null");
		}
		if (slotPrices.isEmpty()) {
			throw new IllegalArgumentException("Slot prices cannot be empty");
		}
		if (timeSlot == null) {
			throw new IllegalArgumentException("Time slot cannot be null");
		}
		
		// null 검증 (Money는 자체적으로 음수를 검증함)
		slotPrices.forEach((slot, price) -> {
			if (slot == null) {
				throw new IllegalArgumentException("Slot time cannot be null");
			}
			if (price == null) {
				throw new IllegalArgumentException("Slot price cannot be null");
			}
		});
		
		// 불변 Map으로 복사
		slotPrices = Collections.unmodifiableMap(Map.copyOf(slotPrices));
	}
	
	/**
	 * 총 가격을 계산합니다.
	 *
	 * @return 모든 슬롯 가격의 합계
	 */
	public Money getTotalPrice() {
		return slotPrices.values().stream()
				.reduce(Money.ZERO, Money::add);
	}
	
	/**
	 * 슬롯 개수를 반환합니다.
	 *
	 * @return 슬롯 개수
	 */
	public int getSlotCount() {
		return slotPrices.size();
	}
	
	/**
	 * 특정 시간의 가격을 조회합니다.
	 *
	 * @param slot 조회할 시간
	 * @return 해당 시간의 가격 (없으면 Money.ZERO)
	 */
	public Money getPriceAt(final LocalDateTime slot) {
		return slotPrices.getOrDefault(Objects.requireNonNull(slot), Money.ZERO);
	}
	
	public List<LocalDateTime> getSlotTimes() {
		return Collections.unmodifiableList(new ArrayList<>(slotPrices.keySet()));
	}
}
