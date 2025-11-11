package com.teambind.springproject.domain.product.availability;

import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RESERVATION Scope 상품의 재고 가용성 검증 전략.
 *
 * <p>RESERVATION Scope는 시간과 무관하게 총 재고량만 확인합니다.
 * 예: 무제한 수량의 옵션 상품 (음료, 간식 등)
 */
public class ReservationScopedChecker implements ScopedAvailabilityChecker {
	
	@Override
	public boolean isAvailable(
			final Product product,
			final List<LocalDateTime> requestedSlots,
			final int requestedQuantity,
			final List<ReservationPricing> overlappingReservations) {
		
		// RESERVATION Scope는 시간과 무관하게 단순히 총 재고만 확인
		return requestedQuantity <= product.getTotalQuantity();
	}
	
	@Override
	public int calculateAvailableQuantity(
			final Product product,
			final List<LocalDateTime> requestedSlots,
			final List<ReservationPricing> overlappingReservations) {
		
		// RESERVATION Scope는 총 재고량을 그대로 반환
		return product.getTotalQuantity();
	}
}
