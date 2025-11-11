/**
 * Shared Kernel (공유 커널)
 * <p>
 * 여러 Bounded Context에서 공통으로 사용하는 Value Object 및 공통 개념을 정의합니다.
 * <p>
 * 포함 요소:
 * - Money: 금액을 표현하는 Value Object
 * - ReservationId: 예약 ID Value Object
 * - RoomId: 룸 ID Value Object
 * - PlaceId: 플레이스 ID Value Object
 * <p>
 * 설계 원칙:
 * - 모든 Value Object는 불변(Immutable)입니다
 * - equals/hashCode를 적절히 구현합니다
 * - 비즈니스 유효성 검증을 생성자에서 수행합니다
 */
package com.teambind.springproject.domain.shared;
