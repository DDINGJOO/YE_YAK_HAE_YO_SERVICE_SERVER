package com.teambind.springproject.adapter.out.persistence.reservationpricing;

import com.teambind.springproject.adapter.out.persistence.pricingpolicy.PricingPolicyRepositoryAdapter;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.product.vo.PricingType;
import com.teambind.springproject.domain.product.vo.ProductPriceBreakdown;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown;
import com.teambind.springproject.domain.shared.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({ReservationPricingRepositoryAdapter.class, PricingPolicyRepositoryAdapter.class})
@DisplayName("ReservationPricingRepository 통합 테스트")
class ReservationPricingRepositoryAdapterTest {
	
	@Autowired
	private ReservationPricingRepositoryAdapter repository;
	
	@Autowired
	private PricingPolicyRepositoryAdapter pricingPolicyRepository;
	
	private RoomId testRoomId;
	private PlaceId testPlaceId;
	
	@BeforeEach
	void setUp() {
		// 테스트용 PricingPolicy 생성 (Room-Place 매핑을 위해 필요)
		testRoomId = RoomId.of(1001L);
		testPlaceId = PlaceId.of(2001L);
		
		final PricingPolicy pricingPolicy = PricingPolicy.create(
				testRoomId,
				testPlaceId,
				TimeSlot.HOUR,
				Money.of(10000)
		);
		pricingPolicyRepository.save(pricingPolicy);
	}
	
	@Nested
	@DisplayName("CRUD 동작 테스트")
	class CrudTests {
		
		@Test
		@DisplayName("예약 가격 저장 및 조회")
		void saveAndFind() {
			// given
			final Map<LocalDateTime, Money> slotPrices = new HashMap<>();
			slotPrices.put(LocalDateTime.of(2025, 1, 15, 10, 0), Money.of(10000));
			slotPrices.put(LocalDateTime.of(2025, 1, 15, 11, 0), Money.of(12000));
			
			final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
					slotPrices,
					TimeSlot.HOUR
			);
			
			final List<ProductPriceBreakdown> productBreakdowns = List.of(
					new ProductPriceBreakdown(
							ProductId.of(101L),
							"빔 프로젝터",
							1,
							Money.of(5000),
							Money.of(5000),
							PricingType.ONE_TIME
					)
			);
			
			final ReservationPricing pricing = ReservationPricing.calculate(
					ReservationId.of(null), // Auto-generated
					testRoomId,
					timeSlotBreakdown,
					productBreakdowns,
					10L
			);
			
			// when
			final ReservationPricing saved = repository.save(pricing);
			
			// then
			assertThat(saved).isNotNull();
			assertThat(saved.getReservationId()).isNotNull();
			assertThat(saved.getReservationId().getValue()).isNotNull();
			
			final Optional<ReservationPricing> found = repository.findById(saved.getReservationId());
			assertThat(found).isPresent();
			assertThat(found.get().getRoomId()).isEqualTo(testRoomId);
			assertThat(found.get().getStatus()).isEqualTo(ReservationStatus.PENDING);
			assertThat(found.get().getTotalPrice()).isEqualTo(Money.of(27000)); // 22000 + 5000
			assertThat(found.get().getTimeSlotBreakdown().getSlotCount()).isEqualTo(2);
			assertThat(found.get().getProductBreakdowns()).hasSize(1);
		}
		
		@Test
		@DisplayName("예약 가격 저장 시 PricingPolicy가 없으면 예외 발생")
		void saveWithoutPricingPolicy() {
			// given
			final RoomId unknownRoomId = RoomId.of(9999L);
			final Map<LocalDateTime, Money> slotPrices = Map.of(
					LocalDateTime.of(2025, 1, 15, 10, 0), Money.of(10000)
			);
			
			final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
					slotPrices,
					TimeSlot.HOUR
			);
			
			final ReservationPricing pricing = ReservationPricing.calculate(
					ReservationId.of(null),
					unknownRoomId,
					timeSlotBreakdown,
					List.of(),
					10L
			);
			
			// when & then
			assertThatThrownBy(() -> repository.save(pricing))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("PricingPolicy not found for roomId");
		}
		
		@Test
		@DisplayName("상품이 없는 예약 가격 저장")
		void saveWithoutProducts() {
			// given
			final Map<LocalDateTime, Money> slotPrices = Map.of(
					LocalDateTime.of(2025, 1, 15, 10, 0), Money.of(10000)
			);
			
			final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
					slotPrices,
					TimeSlot.HOUR
			);
			
			final ReservationPricing pricing = ReservationPricing.calculate(
					ReservationId.of(null),
					testRoomId,
					timeSlotBreakdown,
					List.of(), // 상품 없음
					10L
			);
			
			// when
			final ReservationPricing saved = repository.save(pricing);
			
			// then
			final Optional<ReservationPricing> found = repository.findById(saved.getReservationId());
			assertThat(found).isPresent();
			assertThat(found.get().getProductBreakdowns()).isEmpty();
			assertThat(found.get().getTotalPrice()).isEqualTo(Money.of(10000));
		}
		
		@Test
		@DisplayName("예약 상태 변경 후 저장")
		void updateStatus() {
			// given
			final Map<LocalDateTime, Money> slotPrices = Map.of(
					LocalDateTime.of(2025, 1, 15, 10, 0), Money.of(10000)
			);
			
			final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
					slotPrices,
					TimeSlot.HOUR
			);
			
			final ReservationPricing pricing = ReservationPricing.calculate(
					ReservationId.of(null),
					testRoomId,
					timeSlotBreakdown,
					List.of(),
					10L
			);
			
			final ReservationPricing saved = repository.save(pricing);
			assertThat(saved.getStatus()).isEqualTo(ReservationStatus.PENDING);
			
			// when
			saved.confirm();
			repository.save(saved);
			
			// then
			final Optional<ReservationPricing> found = repository.findById(saved.getReservationId());
			assertThat(found).isPresent();
			assertThat(found.get().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
		}
		
		@Test
		@DisplayName("예약 가격 삭제")
		void delete() {
			// given
			final Map<LocalDateTime, Money> slotPrices = Map.of(
					LocalDateTime.of(2025, 1, 15, 10, 0), Money.of(10000)
			);
			
			final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
					slotPrices,
					TimeSlot.HOUR
			);
			
			final ReservationPricing pricing = ReservationPricing.calculate(
					ReservationId.of(null),
					testRoomId,
					timeSlotBreakdown,
					List.of(),
					10L
			);
			
			final ReservationPricing saved = repository.save(pricing);
			
			// when
			repository.deleteById(saved.getReservationId());
			
			// then
			final Optional<ReservationPricing> found = repository.findById(saved.getReservationId());
			assertThat(found).isEmpty();
		}
		
		@Test
		@DisplayName("예약 존재 여부 확인")
		void existsById() {
			// given
			final Map<LocalDateTime, Money> slotPrices = Map.of(
					LocalDateTime.of(2025, 1, 15, 10, 0), Money.of(10000)
			);
			
			final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
					slotPrices,
					TimeSlot.HOUR
			);
			
			final ReservationPricing pricing = ReservationPricing.calculate(
					ReservationId.of(null),
					testRoomId,
					timeSlotBreakdown,
					List.of(),
					10L
			);
			
			final ReservationPricing saved = repository.save(pricing);
			
			// when & then
			assertThat(repository.existsById(saved.getReservationId())).isTrue();
			assertThat(repository.existsById(ReservationId.of(99999L))).isFalse();
		}
	}
	
	@Nested
	@DisplayName("쿼리 메서드 테스트")
	class QueryMethodTests {
		
		@Test
		@DisplayName("PlaceId와 시간 범위로 예약 조회")
		void findByPlaceIdAndTimeRange() {
			// given
			// 2025-01-15 10:00-12:00 예약
			final Map<LocalDateTime, Money> slotPrices1 = new HashMap<>();
			slotPrices1.put(LocalDateTime.of(2025, 1, 15, 10, 0), Money.of(10000));
			slotPrices1.put(LocalDateTime.of(2025, 1, 15, 11, 0), Money.of(10000));
			
			final ReservationPricing pricing1 = ReservationPricing.calculate(
					ReservationId.of(null),
					testRoomId,
					new TimeSlotPriceBreakdown(slotPrices1, TimeSlot.HOUR),
					List.of(),
					10L
			);
			repository.save(pricing1);
			
			// 2025-01-15 14:00-15:00 예약
			final Map<LocalDateTime, Money> slotPrices2 = new HashMap<>();
			slotPrices2.put(LocalDateTime.of(2025, 1, 15, 14, 0), Money.of(10000));
			
			final ReservationPricing pricing2 = ReservationPricing.calculate(
					ReservationId.of(null),
					testRoomId,
					new TimeSlotPriceBreakdown(slotPrices2, TimeSlot.HOUR),
					List.of(),
					10L
			);
			repository.save(pricing2);
			
			// when - 2025-01-15 09:00-13:00 범위로 조회
			final List<ReservationPricing> found = repository.findByPlaceIdAndTimeRange(
					testPlaceId,
					LocalDateTime.of(2025, 1, 15, 9, 0),
					LocalDateTime.of(2025, 1, 15, 13, 0),
					List.of(ReservationStatus.PENDING)
			);
			
			// then - 10:00-12:00 예약만 조회됨 (14:00 예약은 범위 밖)
			assertThat(found).hasSize(1);
			assertThat(found.get(0).getTimeSlotBreakdown().getSlotCount()).isEqualTo(2);
		}
		
		@Test
		@DisplayName("RoomId와 시간 범위로 예약 조회")
		void findByRoomIdAndTimeRange() {
			// given
			// Room1의 예약
			final Map<LocalDateTime, Money> slotPrices1 = Map.of(
					LocalDateTime.of(2025, 1, 15, 10, 0), Money.of(10000)
			);
			
			final ReservationPricing pricing1 = ReservationPricing.calculate(
					ReservationId.of(null),
					testRoomId,
					new TimeSlotPriceBreakdown(slotPrices1, TimeSlot.HOUR),
					List.of(),
					10L
			);
			repository.save(pricing1);
			
			// Room2의 예약 (다른 룸)
			final RoomId anotherRoomId = RoomId.of(1002L);
			final PricingPolicy anotherPolicy = PricingPolicy.create(
					anotherRoomId,
					testPlaceId,
					TimeSlot.HOUR,
					Money.of(10000)
			);
			pricingPolicyRepository.save(anotherPolicy);
			
			final Map<LocalDateTime, Money> slotPrices2 = Map.of(
					LocalDateTime.of(2025, 1, 15, 10, 0), Money.of(10000)
			);
			
			final ReservationPricing pricing2 = ReservationPricing.calculate(
					ReservationId.of(null),
					anotherRoomId,
					new TimeSlotPriceBreakdown(slotPrices2, TimeSlot.HOUR),
					List.of(),
					10L
			);
			repository.save(pricing2);
			
			// when
			final List<ReservationPricing> found = repository.findByRoomIdAndTimeRange(
					testRoomId,
					LocalDateTime.of(2025, 1, 15, 9, 0),
					LocalDateTime.of(2025, 1, 15, 12, 0),
					List.of(ReservationStatus.PENDING)
			);
			
			// then - testRoomId의 예약만 조회됨
			assertThat(found).hasSize(1);
			assertThat(found.get(0).getRoomId()).isEqualTo(testRoomId);
		}
		
		@Test
		@DisplayName("상태로 예약 조회")
		void findByStatusIn() {
			// given
			final Map<LocalDateTime, Money> slotPrices = Map.of(
					LocalDateTime.of(2025, 1, 15, 10, 0), Money.of(10000)
			);
			
			final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
					slotPrices,
					TimeSlot.HOUR
			);
			
			// PENDING 예약
			final ReservationPricing pending1 = ReservationPricing.calculate(
					ReservationId.of(null),
					testRoomId,
					timeSlotBreakdown,
					List.of(),
					10L
			);
			repository.save(pending1);
			
			final ReservationPricing pending2 = ReservationPricing.calculate(
					ReservationId.of(null),
					testRoomId,
					timeSlotBreakdown,
					List.of(),
					10L
			);
			repository.save(pending2);
			
			// CONFIRMED 예약
			final ReservationPricing confirmed = ReservationPricing.calculate(
					ReservationId.of(null),
					testRoomId,
					timeSlotBreakdown,
					List.of(),
					10L
			);
			final ReservationPricing savedConfirmed = repository.save(confirmed);
			savedConfirmed.confirm();
			repository.save(savedConfirmed);
			
			// when
			final List<ReservationPricing> pendingList = repository.findByStatusIn(
					List.of(ReservationStatus.PENDING)
			);
			final List<ReservationPricing> confirmedList = repository.findByStatusIn(
					List.of(ReservationStatus.CONFIRMED)
			);
			final List<ReservationPricing> allList = repository.findByStatusIn(
					List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
			);
			
			// then
			assertThat(pendingList).hasSize(2);
			assertThat(confirmedList).hasSize(1);
			assertThat(allList).hasSize(3);
		}
	}
	
	@Nested
	@DisplayName("복잡한 데이터 저장/조회 테스트")
	class ComplexDataTests {
		
		@Test
		@DisplayName("다수의 시간 슬롯과 상품을 포함한 예약 저장")
		void saveWithMultipleSlotsAndProducts() {
			// given
			final Map<LocalDateTime, Money> slotPrices = new HashMap<>();
			slotPrices.put(LocalDateTime.of(2025, 1, 15, 10, 0), Money.of(10000));
			slotPrices.put(LocalDateTime.of(2025, 1, 15, 11, 0), Money.of(12000));
			slotPrices.put(LocalDateTime.of(2025, 1, 15, 12, 0), Money.of(15000));
			
			final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
					slotPrices,
					TimeSlot.HOUR
			);
			
			final List<ProductPriceBreakdown> productBreakdowns = List.of(
					new ProductPriceBreakdown(
							ProductId.of(101L),
							"빔 프로젝터",
							1,
							Money.of(5000),
							Money.of(5000),
							PricingType.ONE_TIME
					),
					new ProductPriceBreakdown(
							ProductId.of(102L),
							"화이트보드",
							2,
							Money.of(2000),
							Money.of(4000),
							PricingType.SIMPLE_STOCK
					),
					new ProductPriceBreakdown(
							ProductId.of(103L),
							"음료",
							5,
							Money.of(1000),
							Money.of(5000),
							PricingType.SIMPLE_STOCK
					)
			);
			
			final ReservationPricing pricing = ReservationPricing.calculate(
					ReservationId.of(null),
					testRoomId,
					timeSlotBreakdown,
					productBreakdowns,
					10L
			);
			
			// when
			final ReservationPricing saved = repository.save(pricing);
			
			// then
			final Optional<ReservationPricing> found = repository.findById(saved.getReservationId());
			assertThat(found).isPresent();
			
			final ReservationPricing foundPricing = found.get();
			assertThat(foundPricing.getTimeSlotBreakdown().getSlotCount()).isEqualTo(3);
			assertThat(foundPricing.getProductBreakdowns()).hasSize(3);
			assertThat(foundPricing.getTotalPrice()).isEqualTo(Money.of(51000)); // 37000 + 14000
		}
		
		@Test
		@DisplayName("restore()를 통한 데이터 복원 검증")
		void restorePreservesOriginalData() {
			// given
			final Map<LocalDateTime, Money> slotPrices = Map.of(
					LocalDateTime.of(2025, 1, 15, 10, 0), Money.of(10000)
			);
			
			final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
					slotPrices,
					TimeSlot.HOUR
			);
			
			final ReservationPricing pricing = ReservationPricing.calculate(
					ReservationId.of(null),
					testRoomId,
					timeSlotBreakdown,
					List.of(),
					10L
			);
			
			final ReservationPricing saved = repository.save(pricing);
			final LocalDateTime originalCalculatedAt = saved.getCalculatedAt();
			
			// when - 상태 변경 후 다시 저장
			saved.confirm();
			final ReservationPricing updated = repository.save(saved);
			
			// then - calculatedAt는 변경되지 않음 (restore()로 복원했기 때문)
			final Optional<ReservationPricing> found = repository.findById(updated.getReservationId());
			assertThat(found).isPresent();
			assertThat(found.get().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
			assertThat(found.get().getCalculatedAt()).isEqualTo(originalCalculatedAt);
		}
	}
}
