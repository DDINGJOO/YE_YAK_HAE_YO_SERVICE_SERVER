/**
 * Product Aggregate (추가상품 애그리거트)
 *
 * 추가상품 관리 및 재고 관리를 담당하는 Bounded Context입니다.
 *
 * 책임:
 * - 추가상품 등록 및 관리
 * - 적용 범위 관리 (플레이스/룸/예약별)
 * - 재고 관리 (가용 수량, 총 수량)
 *
 * 도메인 규칙 (Invariants):
 * - 사용 수량은 총 수량을 초과할 수 없음
 * - PENDING 또는 CONFIRMED 상태의 예약만 재고 차감
 * - 가격은 0원 이상이어야 함
 *
 * 주요 클래스:
 * - Product: Aggregate Root
 * - ProductScope: Value Object (적용 범위)
 * - ProductAvailabilityService: Domain Service (재고 검증)
 */
package com.teambind.springproject.domain.product;
