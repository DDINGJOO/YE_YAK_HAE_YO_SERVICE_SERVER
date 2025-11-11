package com.teambind.springproject.domain.product.availability;

import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.vo.ProductPriceBreakdown;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.shared.ProductId;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시간 기반 Scope(PLACE, ROOM) 상품의 재고 가용성 검증 전략.
 *
 * <p>PLACE와 ROOM Scope는 동일한 시간 기반 재고 계산 로직을 사용합니다.
 * 요청 시간대별로 사용 중인 최대 수량을 계산하여 가용 여부를 판단합니다.
 */
public class TimeScopedChecker implements ScopedAvailabilityChecker {
	
	@Override
	public boolean isAvailable(
			final Product product,
			final List<LocalDateTime> requestedSlots,
			final int requestedQuantity,
			final List<ReservationPricing> overlappingReservations) {
		
		validateTimeSlots(requestedSlots);
		
		// 각 슬롯별 최대 사용량 계산
		final int maxUsedQuantity = requestedSlots.stream()
				.mapToInt(slot -> calculateUsedAtSlot(
						overlappingReservations,
						product.getProductId(),
						slot))
				.max()
				.orElse(0);
		
		return maxUsedQuantity + requestedQuantity <= product.getTotalQuantity();
	}
	
	@Override
	public int calculateAvailableQuantity(
			final Product product,
			final List<LocalDateTime> requestedSlots,
			final List<ReservationPricing> overlappingReservations) {
		
		validateTimeSlots(requestedSlots);
		
		final int maxUsedQuantity = requestedSlots.stream()
				.mapToInt(slot -> calculateUsedAtSlot(
						overlappingReservations,
						product.getProductId(),
						slot))
				.max()
				.orElse(0);
		
		return Math.max(0, product.getTotalQuantity() - maxUsedQuantity);
	}
	
	/**
	 * 특정 시간 슬롯에서 해당 상품이 사용 중인 수량을 계산합니다.
	 *
	 * @param reservations 예약 목록
	 * @param productId    상품 ID
	 * @param slot         확인할 시간 슬롯
	 * @return 사용 중인 수량
	 */
	private int calculateUsedAtSlot(
			final List<ReservationPricing> reservations,
			final ProductId productId,
			final LocalDateTime slot) {
		
		return reservations.stream()
				.filter(reservation -> reservation.getTimeSlotBreakdown().slotPrices().containsKey(slot))
				.flatMap(reservation -> reservation.getProductBreakdowns().stream())
				.filter(breakdown -> breakdown.productId().equals(productId))
				.mapToInt(ProductPriceBreakdown::quantity)
				.sum();
	}
	
	/**
	 * 시간 슬롯 목록이 유효한지 검증합니다.
	 *
	 * @param requestedSlots 시간 슬롯 목록
	 * @throws IllegalArgumentException 슬롯이 null이거나 비어있는 경우
	 */
	private void validateTimeSlots(final List<LocalDateTime> requestedSlots) {
		if (requestedSlots == null || requestedSlots.isEmpty()) {
			throw new IllegalArgumentException("Requested time slots cannot be null or empty");
		}
	}
}
