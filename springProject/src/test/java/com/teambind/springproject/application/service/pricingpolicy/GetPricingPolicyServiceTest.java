package com.teambind.springproject.application.service.pricingpolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.exception.PricingPolicyNotFoundException;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPricingPolicyService 단위 테스트")
class GetPricingPolicyServiceTest {

  @Mock
  private PricingPolicyRepository pricingPolicyRepository;

  @InjectMocks
  private GetPricingPolicyService getPricingPolicyService;

  @Nested
  @DisplayName("getPolicy 테스트")
  class GetPolicyTests {

    @Test
    @DisplayName("가격 정책을 성공적으로 조회한다")
    void getPolicySuccess() {
      // given
      final RoomId roomId = RoomId.of(1L);
      final PricingPolicy expectedPolicy = PricingPolicy.create(
          roomId,
          PlaceId.of(100L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("30000"))
      );

      when(pricingPolicyRepository.findById(roomId)).thenReturn(Optional.of(expectedPolicy));

      // when
      final PricingPolicy result = getPricingPolicyService.getPolicy(roomId);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getRoomId()).isEqualTo(roomId);
      assertThat(result.getDefaultPrice()).isEqualTo(Money.of(new BigDecimal("30000")));
      verify(pricingPolicyRepository).findById(roomId);
    }

    @Test
    @DisplayName("존재하지 않는 정책 조회 시 예외를 던진다")
    void getPolicyNotFound() {
      // given
      final RoomId roomId = RoomId.of(999L);

      when(pricingPolicyRepository.findById(roomId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> getPricingPolicyService.getPolicy(roomId))
          .isInstanceOf(PricingPolicyNotFoundException.class)
          .hasMessageContaining("Pricing policy not found for roomId: 999");
    }
  }
}
