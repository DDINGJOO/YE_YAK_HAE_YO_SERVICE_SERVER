package com.teambind.springproject.domain.reservationpricing;

import com.teambind.springproject.domain.product.vo.ProductPriceBreakdown;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.ReservationId;
import com.teambind.springproject.domain.shared.ReservationStatus;
import com.teambind.springproject.domain.shared.RoomId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 예약 가격 Aggregate Root.
 * 예약 시점의 가격 정보를 스냅샷으로 저장하며, 이후 가격 정책 변경에도 불변성을 유지합니다.
 */
public class ReservationPricing {
	
	private final ReservationId reservationId;
	private final RoomId roomId;
	private final TimeSlotPriceBreakdown timeSlotBreakdown;
	private ReservationStatus status;
	private List<ProductPriceBreakdown> productBreakdowns;
	private Money totalPrice;
	private LocalDateTime calculatedAt;
	private LocalDateTime expiresAt;
	
	private ReservationPricing(
			final ReservationId reservationId,
			final RoomId roomId,
			final ReservationStatus status,
			final TimeSlotPriceBreakdown timeSlotBreakdown,
			final List<ProductPriceBreakdown> productBreakdowns,
			final Money totalPrice,
			final LocalDateTime calculatedAt,
			final LocalDateTime expiresAt) {
		validateReservationId(reservationId);
		validateRoomId(roomId);
		validateStatus(status);
		validateTimeSlotBreakdown(timeSlotBreakdown);
		validateProductBreakdowns(productBreakdowns);
		validateTotalPrice(totalPrice);
		validateCalculatedAt(calculatedAt);
		validateExpiresAt(expiresAt, calculatedAt);
		validatePriceConsistency(timeSlotBreakdown, productBreakdowns, totalPrice);
		
		this.reservationId = reservationId;
		this.roomId = roomId;
		this.status = status;
		this.timeSlotBreakdown = timeSlotBreakdown;
		this.productBreakdowns = Collections.unmodifiableList(new ArrayList<>(productBreakdowns));
		this.totalPrice = totalPrice;
		this.calculatedAt = calculatedAt;
		this.expiresAt = expiresAt;
	}
	
	/**
	 * 예약 가격을 계산하여 생성합니다 (Factory Method).
	 *
	 * @param reservationId     예약 ID
	 * @param roomId            룸 ID
	 * @param timeSlotBreakdown 시간대별 가격 내역
	 * @param productBreakdowns 상품별 가격 내역
	 * @param timeoutMinutes    PENDING 상태 타임아웃 시간 (분)
	 * @return ReservationPricing
	 */
	public static ReservationPricing calculate(
			final ReservationId reservationId,
			final RoomId roomId,
			final TimeSlotPriceBreakdown timeSlotBreakdown,
			final List<ProductPriceBreakdown> productBreakdowns,
			final long timeoutMinutes) {
		
		// Null 검증 (생성자보다 먼저 검증)
		if (reservationId == null) {
			throw new IllegalArgumentException("Reservation ID cannot be null");
		}
		if (roomId == null) {
			throw new IllegalArgumentException("Room ID cannot be null");
		}
		if (timeSlotBreakdown == null) {
			throw new IllegalArgumentException("Time slot breakdown cannot be null");
		}
		if (productBreakdowns == null) {
			throw new IllegalArgumentException("Product breakdowns cannot be null");
		}
		if (timeoutMinutes <= 0) {
			throw new IllegalArgumentException("Timeout minutes must be positive");
		}
		
		final Money timeSlotTotal = timeSlotBreakdown.getTotalPrice();
		final Money productTotal = calculateProductTotal(productBreakdowns);
		final Money total = timeSlotTotal.add(productTotal);
		final LocalDateTime now = LocalDateTime.now();
		
		return new ReservationPricing(
				reservationId,
				roomId,
				ReservationStatus.PENDING,
				timeSlotBreakdown,
				productBreakdowns,
				total,
				now,
				now.plusMinutes(timeoutMinutes)
		);
	}
	
	/**
	 * 저장된 예약 가격 데이터를 복원합니다 (Repository용 Factory Method).
	 * 이미 검증된 데이터를 복원하는 용도로만 사용해야 합니다.
	 *
	 * @param reservationId     예약 ID
	 * @param roomId            룸 ID
	 * @param status            예약 상태
	 * @param timeSlotBreakdown 시간대별 가격 내역
	 * @param productBreakdowns 상품별 가격 내역
	 * @param totalPrice        총 가격
	 * @param calculatedAt      계산 시각
	 * @param expiresAt         만료 시각
	 * @return ReservationPricing
	 */
	public static ReservationPricing restore(
			final ReservationId reservationId,
			final RoomId roomId,
			final ReservationStatus status,
			final TimeSlotPriceBreakdown timeSlotBreakdown,
			final List<ProductPriceBreakdown> productBreakdowns,
			final Money totalPrice,
			final LocalDateTime calculatedAt,
			final LocalDateTime expiresAt) {
		
		return new ReservationPricing(
				reservationId,
				roomId,
				status,
				timeSlotBreakdown,
				productBreakdowns,
				totalPrice,
				calculatedAt,
				expiresAt
		);
	}
	
	/**
	 * 상품 총액을 계산합니다.
	 */
	private static Money calculateProductTotal(final List<ProductPriceBreakdown> breakdowns) {
		if (breakdowns == null || breakdowns.isEmpty()) {
			return Money.ZERO;
		}
		return breakdowns.stream()
				.map(ProductPriceBreakdown::totalPrice)
				.reduce(Money.ZERO, Money::add);
	}
	
	/**
	 * 예약을 확정합니다.
	 * PENDING 상태에서만 CONFIRMED로 전환 가능합니다.
	 */
	public void confirm() {
		if (status != ReservationStatus.PENDING) {
			throw new com.teambind.springproject.domain.reservationpricing.exception.InvalidReservationStatusException(
					status, "confirm");
		}
		this.status = ReservationStatus.CONFIRMED;
	}
	
	/**
	 * 예약을 취소합니다.
	 * PENDING 또는 CONFIRMED 상태에서만 CANCELLED로 전환 가능합니다.
	 */
	public void cancel() {
		if (status == ReservationStatus.CANCELLED) {
			throw new com.teambind.springproject.domain.reservationpricing.exception.InvalidReservationStatusException(
					status, "cancel");
		}
		if (status == ReservationStatus.CONFIRMED) {
			throw new com.teambind.springproject.domain.reservationpricing.exception.InvalidReservationStatusException(
					status, "cancel");
		}
		this.status = ReservationStatus.CANCELLED;
	}
	
	/**
	 * 예약의 상품 목록을 업데이트하고 가격을 재계산합니다.
	 * PENDING 상태에서만 상품 업데이트가 가능합니다.
	 *
	 * @param newProductBreakdowns 새로운 상품 가격 내역 목록
	 * @throws com.teambind.springproject.domain.reservationpricing.exception.InvalidReservationStatusException PENDING 상태가 아닌 경우
	 * @throws IllegalArgumentException                                                                         상품 내역이 null인 경우
	 */
	public void updateProducts(final List<ProductPriceBreakdown> newProductBreakdowns) {
		// 상태 검증: PENDING이 아니면 예외
		if (this.status != ReservationStatus.PENDING) {
			throw new com.teambind.springproject.domain.reservationpricing.exception.InvalidReservationStatusException(
					this.status, "updateProducts");
		}
		
		// 입력 검증
		validateProductBreakdowns(newProductBreakdowns);
		
		// 상품 목록 업데이트
		this.productBreakdowns = Collections.unmodifiableList(new ArrayList<>(newProductBreakdowns));
		
		// 가격 재계산
		final Money timeSlotTotal = this.timeSlotBreakdown.getTotalPrice();
		final Money productTotal = calculateProductTotal(newProductBreakdowns);
		this.totalPrice = timeSlotTotal.add(productTotal);
		
		// 계산 시각 업데이트
		this.calculatedAt = LocalDateTime.now();
	}
	
	// ========== Validation Methods ==========
	
	private void validateReservationId(final ReservationId reservationId) {
		if (reservationId == null) {
			throw new IllegalArgumentException("Reservation ID cannot be null");
		}
	}
	
	private void validateRoomId(final RoomId roomId) {
		if (roomId == null) {
			throw new IllegalArgumentException("Room ID cannot be null");
		}
	}
	
	private void validateStatus(final ReservationStatus status) {
		if (status == null) {
			throw new IllegalArgumentException("Reservation status cannot be null");
		}
	}
	
	private void validateTimeSlotBreakdown(final TimeSlotPriceBreakdown breakdown) {
		if (breakdown == null) {
			throw new IllegalArgumentException("Time slot breakdown cannot be null");
		}
	}
	
	private void validateProductBreakdowns(final List<ProductPriceBreakdown> breakdowns) {
		if (breakdowns == null) {
			throw new IllegalArgumentException("Product breakdowns cannot be null");
		}
		// 빈 리스트는 허용 (상품이 없는 예약도 가능)
	}
	
	private void validateTotalPrice(final Money totalPrice) {
		if (totalPrice == null) {
			throw new IllegalArgumentException("Total price cannot be null");
		}
		// Money는 자체적으로 음수를 검증함
	}
	
	private void validateCalculatedAt(final LocalDateTime calculatedAt) {
		if (calculatedAt == null) {
			throw new IllegalArgumentException("Calculated at cannot be null");
		}
	}
	
	private void validateExpiresAt(final LocalDateTime expiresAt, final LocalDateTime calculatedAt) {
		if (expiresAt == null) {
			throw new IllegalArgumentException("Expires at cannot be null");
		}
		if (calculatedAt != null && expiresAt.isBefore(calculatedAt)) {
			throw new IllegalArgumentException("Expires at must be after calculated at");
		}
	}
	
	private void validatePriceConsistency(
			final TimeSlotPriceBreakdown timeSlotBreakdown,
			final List<ProductPriceBreakdown> productBreakdowns,
			final Money totalPrice) {
		
		final Money timeSlotTotal = timeSlotBreakdown.getTotalPrice();
		final Money productTotal = calculateProductTotal(productBreakdowns);
		final Money expectedTotal = timeSlotTotal.add(productTotal);
		
		if (!totalPrice.equals(expectedTotal)) {
			throw new IllegalArgumentException(
					"Total price mismatch: expected " + expectedTotal + " but got " + totalPrice);
		}
	}
	
	// ========== Getters ==========
	
	public ReservationId getReservationId() {
		return reservationId;
	}
	
	public RoomId getRoomId() {
		return roomId;
	}
	
	public ReservationStatus getStatus() {
		return status;
	}
	
	public TimeSlotPriceBreakdown getTimeSlotBreakdown() {
		return timeSlotBreakdown;
	}
	
	public List<ProductPriceBreakdown> getProductBreakdowns() {
		return productBreakdowns;
	}
	
	public Money getTotalPrice() {
		return totalPrice;
	}
	
	public LocalDateTime getCalculatedAt() {
		return calculatedAt;
	}
	
	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}
	
	/**
	 * 예약이 만료되었는지 확인합니다.
	 * PENDING 상태이고 현재 시간이 만료 시각을 초과한 경우 true를 반환합니다.
	 *
	 * @return 만료 여부
	 */
	public boolean isExpired() {
		return status == ReservationStatus.PENDING && LocalDateTime.now().isAfter(expiresAt);
	}
	
	/**
	 * 시간대 가격 총액을 반환합니다.
	 */
	public Money getTimeSlotTotal() {
		return timeSlotBreakdown.getTotalPrice();
	}
	
	/**
	 * 상품 가격 총액을 반환합니다.
	 */
	public Money getProductTotal() {
		return calculateProductTotal(productBreakdowns);
	}
	
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ReservationPricing that = (ReservationPricing) o;
		return Objects.equals(reservationId, that.reservationId);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(reservationId);
	}
	
	@Override
	public String toString() {
		return "ReservationPricing{"
				+ "reservationId=" + reservationId
				+ ", roomId=" + roomId
				+ ", status=" + status
				+ ", totalPrice=" + totalPrice
				+ ", calculatedAt=" + calculatedAt
				+ '}';
	}
}
