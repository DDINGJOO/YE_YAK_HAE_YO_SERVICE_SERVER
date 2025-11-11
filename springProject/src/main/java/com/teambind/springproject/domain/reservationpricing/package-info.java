/**
 * ReservationPricing Aggregate (예약 가격 애그리거트)
 * <p>
 * 예약 시점의 가격 정보를 스냅샷으로 저장하는 Bounded Context입니다.
 * <p>
 * 책임:
 * - 예약 시점의 가격 정보 스냅샷 저장
 * - 예약 총 가격 계산 (룸 가격 + 추가상품 가격)
 * - 가격 정책 변경 이력 관리 (형상 관리)
 * <p>
 * 도메인 규칙 (Invariants):
 * - 예약 가격은 생성 후 변경 불가 (Immutable Snapshot)
 * - 총 가격은 룸 가격 + 모든 추가상품 가격의 합
 * - 모든 금액은 0원 이상이어야 함
 * <p>
 * 주요 클래스:
 * - ReservationPricing: Aggregate Root
 * - PricingItem: Entity (가격 항목)
 */
package com.teambind.springproject.domain.reservationpricing;
