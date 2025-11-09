package com.teambind.springproject.application.port.out;

import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ReservationStatus;
import com.teambind.springproject.domain.shared.RoomId;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ReservationPricing Aggregate를 영속화하기 위한 Repository Port.
 * Hexagonal Architecture의 출력 포트(Output Port)입니다.
 *
 * <p>이 인터페이스는 예약 가격 스냅샷을 조회하는 메서드를 정의합니다.
 * 특히 ProductAvailabilityService에서 재고 가용성 검증을 위해 사용됩니다.
 */
public interface ReservationPricingRepository {

  /**
   * PlaceId와 시간 범위로 예약 가격 스냅샷을 조회합니다.
   * 특정 플레이스에서 주어진 시간 범위에 겹치는 예약들을 조회합니다.
   *
   * @param placeId 플레이스 ID
   * @param start 시작 시간 (inclusive)
   * @param end 종료 시간 (exclusive)
   * @param statuses 조회할 예약 상태 목록
   * @return 조건에 맞는 예약 가격 스냅샷 목록
   */
  // TODO: ReservationPricing 도메인 모델 구현 후 반환 타입 변경 (Issue #15)
  // List<ReservationPricing> findByPlaceIdAndTimeRange(
  //     PlaceId placeId,
  //     LocalDateTime start,
  //     LocalDateTime end,
  //     List<ReservationStatus> statuses);

  /**
   * RoomId와 시간 범위로 예약 가격 스냅샷을 조회합니다.
   * 특정 룸에서 주어진 시간 범위에 겹치는 예약들을 조회합니다.
   *
   * @param roomId 룸 ID
   * @param start 시작 시간 (inclusive)
   * @param end 종료 시간 (exclusive)
   * @param statuses 조회할 예약 상태 목록
   * @return 조건에 맞는 예약 가격 스냅샷 목록
   */
  // TODO: ReservationPricing 도메인 모델 구현 후 반환 타입 변경 (Issue #15)
  // List<ReservationPricing> findByRoomIdAndTimeRange(
  //     RoomId roomId,
  //     LocalDateTime start,
  //     LocalDateTime end,
  //     List<ReservationStatus> statuses);
}
