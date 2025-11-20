package com.teambind.springproject.application.port.out;

import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ReservationId;
import com.teambind.springproject.domain.shared.ReservationStatus;
import com.teambind.springproject.domain.shared.RoomId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ReservationPricing Aggregate를 영속화하기 위한 Repository Port.
 * Hexagonal Architecture의 출력 포트(Output Port)입니다.
 */
public interface ReservationPricingRepository {
	
	/**
	 * ReservationId로 예약 가격을 조회합니다.
	 *
	 * @param reservationId 예약 ID
	 * @return 예약 가격 (없으면 Optional.empty())
	 */
	Optional<ReservationPricing> findById(ReservationId reservationId);
	
	/**
	 * PlaceId와 시간 범위로 예약 가격 스냅샷을 조회합니다.
	 * 특정 플레이스에서 주어진 시간 범위에 겹치는 예약들을 조회합니다.
	 *
	 * @param placeId  플레이스 ID
	 * @param start    시작 시간 (inclusive)
	 * @param end      종료 시간 (exclusive)
	 * @param statuses 조회할 예약 상태 목록
	 * @return 조건에 맞는 예약 가격 스냅샷 목록
	 */
	List<ReservationPricing> findByPlaceIdAndTimeRange(
			PlaceId placeId,
			LocalDateTime start,
			LocalDateTime end,
			List<ReservationStatus> statuses);
	
	/**
	 * RoomId와 시간 범위로 예약 가격 스냅샷을 조회합니다.
	 * 특정 룸에서 주어진 시간 범위에 겹치는 예약들을 조회합니다.
	 *
	 * @param roomId   룸 ID
	 * @param start    시작 시간 (inclusive)
	 * @param end      종료 시간 (exclusive)
	 * @param statuses 조회할 예약 상태 목록
	 * @return 조건에 맞는 예약 가격 스냅샷 목록
	 */
	List<ReservationPricing> findByRoomIdAndTimeRange(
			RoomId roomId,
			LocalDateTime start,
			LocalDateTime end,
			List<ReservationStatus> statuses);
	
	/**
	 * 특정 상태의 예약 가격을 조회합니다.
	 *
	 * @param statuses 조회할 상태 목록
	 * @return 예약 가격 목록
	 */
	List<ReservationPricing> findByStatusIn(List<ReservationStatus> statuses);
	
	/**
	 * 예약 가격을 저장합니다.
	 * 새로운 예약이면 INSERT, 기존 예약이면 UPDATE합니다.
	 *
	 * @param reservationPricing 저장할 예약 가격
	 * @return 저장된 예약 가격
	 */
	ReservationPricing save(ReservationPricing reservationPricing);
	
	/**
	 * ReservationId로 예약 가격을 삭제합니다.
	 *
	 * @param reservationId 예약 ID
	 */
	void deleteById(ReservationId reservationId);
	
	/**
	 * ReservationId에 해당하는 예약 가격이 존재하는지 확인합니다.
	 *
	 * @param reservationId 예약 ID
	 * @return 존재하면 true, 아니면 false
	 */
	boolean existsById(ReservationId reservationId);
	
	/**
	 * 만료된 PENDING 상태의 예약을 조회합니다.
	 *
	 * @return 만료된 PENDING 예약 목록
	 */
	List<ReservationPricing> findExpiredPendingReservations();

	/**
	 * 영속성 컨텍스트의 변경사항을 데이터베이스에 즉시 반영합니다.
	 * Kafka 이벤트 처리 등 트랜잭션 경계가 명확하지 않은 경우 사용합니다.
	 */
	void flush();
}
