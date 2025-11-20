package com.teambind.springproject.adapter.out.persistence.reservationpricing;

import com.teambind.springproject.adapter.out.persistence.pricingpolicy.PricingPolicyEntity;
import com.teambind.springproject.adapter.out.persistence.pricingpolicy.PricingPolicyJpaRepository;
import com.teambind.springproject.adapter.out.persistence.pricingpolicy.RoomIdEmbeddable;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ReservationId;
import com.teambind.springproject.domain.shared.ReservationStatus;
import com.teambind.springproject.domain.shared.RoomId;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ReservationPricingRepository Port의 JPA Adapter 구현.
 * Hexagonal Architecture의 Adapter 계층입니다.
 */
@Repository
public class ReservationPricingRepositoryAdapter implements ReservationPricingRepository {
	
	private final ReservationPricingJpaRepository jpaRepository;
	private final PricingPolicyJpaRepository pricingPolicyJpaRepository;
	
	public ReservationPricingRepositoryAdapter(
			final ReservationPricingJpaRepository jpaRepository,
			final PricingPolicyJpaRepository pricingPolicyJpaRepository) {
		this.jpaRepository = jpaRepository;
		this.pricingPolicyJpaRepository = pricingPolicyJpaRepository;
	}
	
	@Override
	public Optional<ReservationPricing> findById(final ReservationId reservationId) {
		return jpaRepository.findById(reservationId.getValue())
				.map(ReservationPricingEntity::toDomain);
	}
	
	@Override
	public List<ReservationPricing> findByPlaceIdAndTimeRange(
			final PlaceId placeId,
			final LocalDateTime start,
			final LocalDateTime end,
			final List<ReservationStatus> statuses) {

		// N+1 최적화: 2단계 Fetch Join으로 컬렉션 로딩
		// 1단계: slotPrices fetch join (이미 쿼리에 포함됨)
		final List<ReservationPricingEntity> reservations = jpaRepository.findByPlaceIdAndTimeRange(
				placeId.getValue(),
				start,
				end,
				statuses
		);

		// 2단계: productBreakdowns fetch join (MultipleBagFetchException 방지)
		if (!reservations.isEmpty()) {
			jpaRepository.fetchProductBreakdowns(reservations);
		}

		return reservations.stream()
				.map(ReservationPricingEntity::toDomain)
				.toList();
	}
	
	@Override
	public List<ReservationPricing> findByRoomIdAndTimeRange(
			final RoomId roomId,
			final LocalDateTime start,
			final LocalDateTime end,
			final List<ReservationStatus> statuses) {

		// N+1 최적화: 2단계 Fetch Join으로 컬렉션 로딩
		// 1단계: slotPrices fetch join (이미 쿼리에 포함됨)
		final List<ReservationPricingEntity> reservations = jpaRepository.findByRoomIdAndTimeRange(
				roomId.getValue(),
				start,
				end,
				statuses
		);

		// 2단계: productBreakdowns fetch join (MultipleBagFetchException 방지)
		if (!reservations.isEmpty()) {
			jpaRepository.fetchProductBreakdowns(reservations);
		}

		return reservations.stream()
				.map(ReservationPricingEntity::toDomain)
				.toList();
	}
	
	@Override
	public List<ReservationPricing> findByStatusIn(final List<ReservationStatus> statuses) {
		// N+1 최적화: 2단계 Fetch Join으로 컬렉션 로딩
		// 1단계: slotPrices fetch join
		final List<ReservationPricingEntity> reservations =
				jpaRepository.findByStatusInWithSlots(statuses);

		// 2단계: productBreakdowns fetch join (MultipleBagFetchException 방지)
		if (!reservations.isEmpty()) {
			jpaRepository.fetchProductBreakdowns(reservations);
		}

		return reservations.stream()
				.map(ReservationPricingEntity::toDomain)
				.toList();
	}
	
	@Override
	public ReservationPricing save(final ReservationPricing reservationPricing) {
		// RoomId로 PricingPolicy를 조회하여 PlaceId를 가져옴
		final Long placeId = findPlaceIdByRoomId(reservationPricing.getRoomId());
		
		// Domain을 Entity로 변환 (placeId 포함)
		final ReservationPricingEntity entity = ReservationPricingEntity.fromDomain(
				reservationPricing,
				placeId
		);
		
		// 저장 후 Domain으로 다시 변환
		final ReservationPricingEntity savedEntity = jpaRepository.save(entity);
		return savedEntity.toDomain();
	}
	
	@Override
	public void deleteById(final ReservationId reservationId) {
		jpaRepository.deleteById(reservationId.getValue());
	}
	
	@Override
	public boolean existsById(final ReservationId reservationId) {
		return jpaRepository.existsById(reservationId.getValue());
	}
	
	@Override
	public List<ReservationPricing> findExpiredPendingReservations() {
		// N+1 최적화: 2단계 Fetch Join으로 컬렉션 로딩
		// 1단계: slotPrices fetch join (이미 쿼리에 포함됨)
		final List<ReservationPricingEntity> reservations =
				jpaRepository.findExpiredPendingReservations(LocalDateTime.now());

		// 2단계: productBreakdowns fetch join (MultipleBagFetchException 방지)
		if (!reservations.isEmpty()) {
			jpaRepository.fetchProductBreakdowns(reservations);
		}

		return reservations.stream()
				.map(ReservationPricingEntity::toDomain)
				.toList();
	}
	
	@Override
	public void flush() {
		jpaRepository.flush();
	}

	/**
	 * RoomId로 PlaceId를 조회합니다.
	 * PricingPolicy에서 Room-Place 매핑을 가져옵니다.
	 *
	 * @param roomId 룸 ID
	 * @return PlaceId (Long)
	 * @throws IllegalArgumentException PricingPolicy가 없는 경우
	 */
	private Long findPlaceIdByRoomId(final RoomId roomId) {
		final RoomIdEmbeddable roomIdEmbeddable = new RoomIdEmbeddable(roomId.getValue());

		final Optional<PricingPolicyEntity> pricingPolicy = pricingPolicyJpaRepository.findById(
				roomIdEmbeddable);

		return pricingPolicy
				.map(entity -> entity.getPlaceId().getValue())
				.orElseThrow(() -> new IllegalArgumentException(
						"PricingPolicy not found for roomId: " + roomId.getValue()
								+ ". Room must have a pricing policy before creating reservations."));
	}
}
