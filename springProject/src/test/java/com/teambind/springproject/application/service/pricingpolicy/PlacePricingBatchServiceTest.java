package com.teambind.springproject.application.service.pricingpolicy;

import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.shared.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * PlacePricingBatchService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class PlacePricingBatchServiceTest {

	@Mock
	private PricingPolicyRepository pricingPolicyRepository;

	@InjectMocks
	private PlacePricingBatchService placePricingBatchService;

	private PlaceId placeId;
	private List<PricingPolicy> mockPolicies;

	@BeforeEach
	void setUp() {
		placeId = PlaceId.of(1L);

		// Mock PricingPolicy 생성
		final PricingPolicy policy1 = PricingPolicy.create(
				RoomId.of(101L),
				placeId,
				TimeSlot.HOUR,
				Money.of(BigDecimal.valueOf(10000))
		);

		final PricingPolicy policy2 = PricingPolicy.create(
				RoomId.of(102L),
				placeId,
				TimeSlot.HALFHOUR,
				Money.of(BigDecimal.valueOf(8000))
		);

		mockPolicies = Arrays.asList(policy1, policy2);
	}

	@Test
	@DisplayName("PlaceId로 가격 정책 리스트를 조회한다")
	void getPricingByPlace_ShouldReturnPoliciesList() {
		// Given
		given(pricingPolicyRepository.findAllByPlaceId(placeId))
				.willReturn(mockPolicies);

		// When
		final List<PricingPolicy> result = placePricingBatchService.getPricingByPlace(placeId);

		// Then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getRoomId().getValue()).isEqualTo(101L);
		assertThat(result.get(1).getRoomId().getValue()).isEqualTo(102L);
		verify(pricingPolicyRepository, times(1)).findAllByPlaceId(placeId);
	}

	@Test
	@DisplayName("PlaceId와 날짜로 가격 정책 리스트를 조회한다")
	void getPricingByPlaceWithDate_ShouldReturnPoliciesList() {
		// Given
		final LocalDate date = LocalDate.of(2025, 12, 7);
		given(pricingPolicyRepository.findAllByPlaceId(placeId))
				.willReturn(mockPolicies);

		// When
		final List<PricingPolicy> result = placePricingBatchService.getPricingByPlace(
				placeId,
				Optional.of(date)
		);

		// Then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getTimeSlot()).isEqualTo(TimeSlot.HOUR);
		assertThat(result.get(1).getTimeSlot()).isEqualTo(TimeSlot.HALFHOUR);
		verify(pricingPolicyRepository, times(1)).findAllByPlaceId(placeId);
	}

	@Test
	@DisplayName("Room이 없는 PlaceId 조회 시 빈 리스트를 반환한다")
	void getPricingByPlace_WhenNoRooms_ShouldReturnEmptyList() {
		// Given
		given(pricingPolicyRepository.findAllByPlaceId(placeId))
				.willReturn(Collections.emptyList());

		// When
		final List<PricingPolicy> result = placePricingBatchService.getPricingByPlace(placeId);

		// Then
		assertThat(result).isEmpty();
		verify(pricingPolicyRepository, times(1)).findAllByPlaceId(placeId);
	}

	@Test
	@DisplayName("null PlaceId 조회 시 IllegalArgumentException을 발생시킨다")
	void getPricingByPlace_WithNullPlaceId_ShouldThrowException() {
		// When & Then
		assertThatThrownBy(() -> placePricingBatchService.getPricingByPlace(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("PlaceId cannot be null");

		verify(pricingPolicyRepository, times(0)).findAllByPlaceId(any());
	}

	@Test
	@DisplayName("날짜 없이 조회와 빈 Optional 날짜로 조회가 동일하게 동작한다")
	void getPricingByPlace_WithEmptyOptionalDate_ShouldWorkSameAsWithoutDate() {
		// Given
		given(pricingPolicyRepository.findAllByPlaceId(placeId))
				.willReturn(mockPolicies);

		// When
		final List<PricingPolicy> resultWithoutDate = placePricingBatchService.getPricingByPlace(placeId);
		final List<PricingPolicy> resultWithEmptyDate = placePricingBatchService.getPricingByPlace(
				placeId,
				Optional.empty()
		);

		// Then
		assertThat(resultWithoutDate).isEqualTo(resultWithEmptyDate);
		verify(pricingPolicyRepository, times(2)).findAllByPlaceId(placeId);
	}
}