/**
 * PricingPolicy Aggregate (가격 정책 애그리거트)
 * <p>
 * 시간대별 예약 가격을 관리하는 Bounded Context입니다.
 * <p>
 * 책임:
 * - 룸별 시간대별 가격 정책 설정
 * - 요일별, 시간대별 가격 차등 적용
 * - 시간대 중복 검증
 * <p>
 * 도메인 규칙 (Invariants):
 * - 같은 요일 내에서 시간대가 중복될 수 없음
 * - 시작 시간은 종료 시간보다 이전이어야 함
 * - 가격은 0원 이상이어야 함
 * <p>
 * 주요 클래스:
 * - PricingPolicy: Aggregate Root
 * - TimeSlot: Value Object (시간대)
 * - DayOfWeek: Enum (요일)
 */
package com.teambind.springproject.domain.pricingpolicy;
