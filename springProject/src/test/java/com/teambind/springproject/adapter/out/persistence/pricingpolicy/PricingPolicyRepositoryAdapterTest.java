package com.teambind.springproject.adapter.out.persistence.pricingpolicy;

import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrice;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrices;
import com.teambind.springproject.domain.shared.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(PricingPolicyRepositoryAdapter.class)
@DisplayName("PricingPolicyRepository 통합 테스트")
class PricingPolicyRepositoryAdapterTest {
	
	@Autowired
	private PricingPolicyRepositoryAdapter repository;
	
	@Nested
	@DisplayName("저장 및 조회 테스트")
	class SaveAndFindTests {
		
		@Test
		@DisplayName("기본 가격만 있는 정책 저장 및 조회")
		void saveAndFindPolicyWithDefaultPriceOnly() {
			// given
			final RoomId roomId = RoomId.of(1L);
			final PricingPolicy policy = PricingPolicy.create(
					roomId,
					PlaceId.of(1L),
					TimeSlot.HOUR,
					Money.of(new BigDecimal("10000"))
			);
			
			// when
			final PricingPolicy saved = repository.save(policy);
			
			// then
			assertThat(saved).isNotNull();
			
			final Optional<PricingPolicy> found = repository.findById(roomId);
			assertThat(found).isPresent();
			assertThat(found.get().getRoomId()).isEqualTo(roomId);
			assertThat(found.get().getPlaceId()).isEqualTo(PlaceId.of(1L));
			assertThat(found.get().getTimeSlot()).isEqualTo(TimeSlot.HOUR);
			assertThat(found.get().getDefaultPrice().getAmount())
					.isEqualByComparingTo(new BigDecimal("10000.00"));
			assertThat(found.get().getTimeRangePrices().isEmpty()).isTrue();
		}
		
		@Test
		@DisplayName("시간대별 가격이 포함된 정책 저장 및 조회")
		void saveAndFindPolicyWithTimeRangePrices() {
			// given
			final RoomId roomId = RoomId.of(2L);
			final TimeRangePrices timeRangePrices = TimeRangePrices.of(List.of(
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(18, 0), LocalTime.of(22, 0)),
							Money.of(new BigDecimal("15000"))
					),
					new TimeRangePrice(
							DayOfWeek.SATURDAY,
							TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
							Money.of(new BigDecimal("20000"))
					)
			));
			
			final PricingPolicy policy = PricingPolicy.createWithTimeRangePrices(
					roomId,
					PlaceId.of(1L),
					TimeSlot.HOUR,
					Money.of(new BigDecimal("10000")),
					timeRangePrices
			);
			
			// when
			repository.save(policy);
			
			// then
			final Optional<PricingPolicy> found = repository.findById(roomId);
			assertThat(found).isPresent();
			assertThat(found.get().getTimeRangePrices().size()).isEqualTo(2);
			
			final List<TimeRangePrice> prices = found.get().getTimeRangePrices().getPrices();
			assertThat(prices).hasSize(2);
			
			// 첫 번째 시간대 가격 확인
			final TimeRangePrice firstPrice = prices.get(0);
			assertThat(firstPrice.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
			assertThat(firstPrice.timeRange().getStartTime()).isEqualTo(LocalTime.of(18, 0));
			assertThat(firstPrice.timeRange().getEndTime()).isEqualTo(LocalTime.of(22, 0));
			assertThat(firstPrice.pricePerSlot().getAmount())
					.isEqualByComparingTo(new BigDecimal("15000.00"));
		}
	}
	
	@Nested
	@DisplayName("수정 테스트")
	class UpdateTests {
		
		@Test
		@DisplayName("기본 가격 변경 후 저장")
		void updateDefaultPrice() {
			// given
			final RoomId roomId = RoomId.of(3L);
			final PricingPolicy policy = PricingPolicy.create(
					roomId,
					PlaceId.of(1L),
					TimeSlot.HOUR,
					Money.of(new BigDecimal("10000"))
			);
			repository.save(policy);
			
			// when
			policy.updateDefaultPrice(Money.of(new BigDecimal("12000")));
			repository.save(policy);
			
			// then
			final Optional<PricingPolicy> found = repository.findById(roomId);
			assertThat(found).isPresent();
			assertThat(found.get().getDefaultPrice().getAmount())
					.isEqualByComparingTo(new BigDecimal("12000.00"));
		}
		
		@Test
		@DisplayName("시간대별 가격 재설정 후 저장")
		void resetTimeRangePrices() {
			// given
			final RoomId roomId = RoomId.of(4L);
			final PricingPolicy policy = PricingPolicy.create(
					roomId,
					PlaceId.of(1L),
					TimeSlot.HOUR,
					Money.of(new BigDecimal("10000"))
			);
			repository.save(policy);
			
			// when
			final TimeRangePrices newPrices = TimeRangePrices.of(List.of(
					new TimeRangePrice(
							DayOfWeek.SUNDAY,
							TimeRange.of(LocalTime.of(10, 0), LocalTime.of(20, 0)),
							Money.of(new BigDecimal("25000"))
					)
			));
			policy.resetPrices(newPrices);
			repository.save(policy);
			
			// then
			final Optional<PricingPolicy> found = repository.findById(roomId);
			assertThat(found).isPresent();
			assertThat(found.get().getTimeRangePrices().size()).isEqualTo(1);
			
			final TimeRangePrice price = found.get().getTimeRangePrices().getPrices().get(0);
			assertThat(price.dayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
			assertThat(price.pricePerSlot().getAmount())
					.isEqualByComparingTo(new BigDecimal("25000.00"));
		}
	}
	
	@Nested
	@DisplayName("삭제 테스트")
	class DeleteTests {
		
		@Test
		@DisplayName("정책 삭제")
		void deletePolicy() {
			// given
			final RoomId roomId = RoomId.of(5L);
			final PricingPolicy policy = PricingPolicy.create(
					roomId,
					PlaceId.of(1L),
					TimeSlot.HOUR,
					Money.of(new BigDecimal("10000"))
			);
			repository.save(policy);
			
			// when
			repository.deleteById(roomId);
			
			// then
			final Optional<PricingPolicy> found = repository.findById(roomId);
			assertThat(found).isEmpty();
		}
		
		@Test
		@DisplayName("시간대별 가격이 포함된 정책 삭제 (CASCADE)")
		void deletePolicyWithTimeRangePrices() {
			// given
			final RoomId roomId = RoomId.of(6L);
			final TimeRangePrices timeRangePrices = TimeRangePrices.of(List.of(
					new TimeRangePrice(
							DayOfWeek.MONDAY,
							TimeRange.of(LocalTime.of(18, 0), LocalTime.of(22, 0)),
							Money.of(new BigDecimal("15000"))
					)
			));
			final PricingPolicy policy = PricingPolicy.createWithTimeRangePrices(
					roomId,
					PlaceId.of(1L),
					TimeSlot.HOUR,
					Money.of(new BigDecimal("10000")),
					timeRangePrices
			);
			repository.save(policy);
			
			// when
			repository.deleteById(roomId);
			
			// then
			final Optional<PricingPolicy> found = repository.findById(roomId);
			assertThat(found).isEmpty();
		}
	}
	
	@Nested
	@DisplayName("존재 여부 확인 테스트")
	class ExistsTests {
		
		@Test
		@DisplayName("존재하는 정책 확인")
		void existsById() {
			// given
			final RoomId roomId = RoomId.of(7L);
			final PricingPolicy policy = PricingPolicy.create(
					roomId,
					PlaceId.of(1L),
					TimeSlot.HOUR,
					Money.of(new BigDecimal("10000"))
			);
			repository.save(policy);
			
			// when
			final boolean exists = repository.existsById(roomId);
			
			// then
			assertThat(exists).isTrue();
		}
		
		@Test
		@DisplayName("존재하지 않는 정책 확인")
		void notExistsById() {
			// given
			final RoomId roomId = RoomId.of(999L);
			
			// when
			final boolean exists = repository.existsById(roomId);
			
			// then
			assertThat(exists).isFalse();
		}
	}
	
	@Nested
	@DisplayName("조회 테스트")
	class FindTests {
		
		@Test
		@DisplayName("존재하지 않는 ID로 조회 시 Optional.empty")
		void findByIdNotFound() {
			// given
			final RoomId roomId = RoomId.of(888L);
			
			// when
			final Optional<PricingPolicy> found = repository.findById(roomId);
			
			// then
			assertThat(found).isEmpty();
		}
	}
}
