package com.teambind.springproject.domain.product.vo;

/**
 * 상품의 적용 범위를 나타내는 Enum.
 */
public enum ProductScope {
	
	/**
	 * 플레이스 전체에 적용되는 상품.
	 * 예: 모든 룸에서 사용 가능한 공용 비품
	 */
	PLACE,
	
	/**
	 * 특정 룸에만 적용되는 상품.
	 * 예: 특정 룸 전용 장비
	 */
	ROOM,
	
	/**
	 * 예약에만 적용되는 상품.
	 * 예: 예약 시 추가로 주문 가능한 음료/간식 (시간과 무관)
	 */
	RESERVATION;
	
	/**
	 * 시간 슬롯이 필요한 Scope인지 확인합니다.
	 *
	 * @return PLACE 또는 ROOM이면 true, RESERVATION이면 false
	 */
	public boolean requiresTimeSlots() {
		return this == PLACE || this == ROOM;
	}
}
