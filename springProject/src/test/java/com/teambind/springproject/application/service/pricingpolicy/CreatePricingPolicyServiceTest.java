package com.teambind.springproject.application.service.pricingpolicy;

import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePricingPolicyService 단위 테스트")
class CreatePricingPolicyServiceTest {
	
	@Mock
	private PricingPolicyRepository pricingPolicyRepository;
	
	@InjectMocks
	private CreatePricingPolicyService createPricingPolicyService;
	
	private RoomId roomId;
	private PlaceId placeId;
	private TimeSlot timeSlot;
	
	@BeforeEach
	void setUp() {
		roomId = RoomId.of(1L);
		placeId = PlaceId.of(100L);
		timeSlot = TimeSlot.HOUR;
	}
	
	@Nested
	@DisplayName("createDefaultPolicy 테스트")
	class CreateDefaultPolicyTests {
		
		@Test
		@DisplayName("새로운 가격 정책을 생성한다")
		void createNewPolicy() {
			// given
			when(pricingPolicyRepository.existsById(roomId)).thenReturn(false);
			
			final PricingPolicy expectedPolicy = PricingPolicy.create(
					roomId,
					placeId,
					timeSlot,
					Money.of(BigDecimal.ZERO)
			);
			when(pricingPolicyRepository.save(any(PricingPolicy.class))).thenReturn(expectedPolicy);
			
			// when
			final PricingPolicy result = createPricingPolicyService.createDefaultPolicy(
					roomId, placeId, timeSlot);
			
			// then
			assertThat(result).isNotNull();
			assertThat(result.getRoomId()).isEqualTo(roomId);
			assertThat(result.getPlaceId()).isEqualTo(placeId);
			assertThat(result.getTimeSlot()).isEqualTo(timeSlot);
			assertThat(result.getDefaultPrice().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
			
			verify(pricingPolicyRepository).existsById(roomId);
			verify(pricingPolicyRepository).save(any(PricingPolicy.class));
		}
		
		@Test
		@DisplayName("이미 존재하는 정책이 있으면 새로 생성하지 않고 기존 정책을 반환한다")
		void returnExistingPolicy() {
			// given
			final PricingPolicy existingPolicy = PricingPolicy.create(
					roomId,
					placeId,
					timeSlot,
					Money.of(new BigDecimal("10000"))
			);
			
			when(pricingPolicyRepository.existsById(roomId)).thenReturn(true);
			when(pricingPolicyRepository.findById(roomId)).thenReturn(Optional.of(existingPolicy));
			
			// when
			final PricingPolicy result = createPricingPolicyService.createDefaultPolicy(
					roomId, placeId, timeSlot);
			
			// then
			assertThat(result).isNotNull();
			assertThat(result.getRoomId()).isEqualTo(roomId);
			assertThat(result.getDefaultPrice().getAmount())
					.isEqualByComparingTo(new BigDecimal("10000.00"));
			
			verify(pricingPolicyRepository).existsById(roomId);
			verify(pricingPolicyRepository).findById(roomId);
			verify(pricingPolicyRepository, never()).save(any(PricingPolicy.class));
		}
		
		@Test
		@DisplayName("기본 가격은 0원으로 설정된다")
		void defaultPriceIsZero() {
			// given
			when(pricingPolicyRepository.existsById(roomId)).thenReturn(false);
			
			final PricingPolicy expectedPolicy = PricingPolicy.create(
					roomId,
					placeId,
					timeSlot,
					Money.of(BigDecimal.ZERO)
			);
			when(pricingPolicyRepository.save(any(PricingPolicy.class))).thenReturn(expectedPolicy);
			
			// when
			final PricingPolicy result = createPricingPolicyService.createDefaultPolicy(
					roomId, placeId, timeSlot);
			
			// then
			assertThat(result.getDefaultPrice()).isEqualTo(Money.ZERO);
			assertThat(result.getDefaultPrice().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
		}
	}
}
