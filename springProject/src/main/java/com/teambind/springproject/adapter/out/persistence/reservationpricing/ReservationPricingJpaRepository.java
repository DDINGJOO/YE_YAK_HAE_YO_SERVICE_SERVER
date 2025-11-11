package com.teambind.springproject.adapter.out.persistence.reservationpricing;

import com.teambind.springproject.domain.shared.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ReservationPricing Entity를 위한 Spring Data JPA Repository.
 */
public interface ReservationPricingJpaRepository extends
		JpaRepository<ReservationPricingEntity, Long> {
	
	/**
	 * PlaceId와 시간 범위, 상태로 예약 가격을 조회합니다.
	 * 시간 범위 체크는 slotPrices의 키(LocalDateTime)를 기준으로 합니다.
	 *
	 * @param placeId  플레이스 ID
	 * @param start    시작 시간 (inclusive)
	 * @param end      종료 시간 (exclusive)
	 * @param statuses 조회할 예약 상태 목록
	 * @return 조건에 맞는 예약 가격 엔티티 목록
	 */
	@Query("SELECT DISTINCT rp FROM ReservationPricingEntity rp "
			+ "JOIN rp.slotPrices sp "
			+ "WHERE rp.placeId = :placeId "
			+ "AND KEY(sp) >= :start AND KEY(sp) < :end "
			+ "AND rp.status IN :statuses")
	List<ReservationPricingEntity> findByPlaceIdAndTimeRange(
			@Param("placeId") Long placeId,
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end,
			@Param("statuses") List<ReservationStatus> statuses);
	
	/**
	 * RoomId와 시간 범위, 상태로 예약 가격을 조회합니다.
	 * 시간 범위 체크는 slotPrices의 키(LocalDateTime)를 기준으로 합니다.
	 *
	 * @param roomId   룸 ID
	 * @param start    시작 시간 (inclusive)
	 * @param end      종료 시간 (exclusive)
	 * @param statuses 조회할 예약 상태 목록
	 * @return 조건에 맞는 예약 가격 엔티티 목록
	 */
	@Query("SELECT DISTINCT rp FROM ReservationPricingEntity rp "
			+ "JOIN rp.slotPrices sp "
			+ "WHERE rp.roomId = :roomId "
			+ "AND KEY(sp) >= :start AND KEY(sp) < :end "
			+ "AND rp.status IN :statuses")
	List<ReservationPricingEntity> findByRoomIdAndTimeRange(
			@Param("roomId") Long roomId,
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end,
			@Param("statuses") List<ReservationStatus> statuses);
	
	/**
	 * 상태로 예약 가격을 조회합니다.
	 *
	 * @param statuses 조회할 예약 상태 목록
	 * @return 조건에 맞는 예약 가격 엔티티 목록
	 */
	List<ReservationPricingEntity> findByStatusIn(List<ReservationStatus> statuses);
	
	/**
	 * 만료된 PENDING 상태의 예약을 조회합니다.
	 *
	 * @param now 현재 시간
	 * @return 만료된 PENDING 예약 엔티티 목록
	 */
	@Query("SELECT rp FROM ReservationPricingEntity rp "
			+ "WHERE rp.status = 'PENDING' "
			+ "AND rp.expiresAt < :now")
	List<ReservationPricingEntity> findExpiredPendingReservations(@Param("now") LocalDateTime now);
}
