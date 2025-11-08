package com.teambind.springproject.application.service.pricingpolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrice;
import com.teambind.springproject.domain.pricingpolicy.exception.PricingPolicyNotFoundException;
import com.teambind.springproject.domain.shared.DayOfWeek;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeRange;
import com.teambind.springproject.domain.shared.TimeSlot;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdatePricingPolicyService 단위 테스트")
class UpdatePricingPolicyServiceTest {

  @Mock
  private PricingPolicyRepository pricingPolicyRepository;

  @InjectMocks
  private UpdatePricingPolicyService updatePricingPolicyService;

  @Nested
  @DisplayName("updateDefaultPrice 테스트")
  class UpdateDefaultPriceTests {

    @Test
    @DisplayName("기본 가격을 성공적으로 업데이트한다")
    void updateDefaultPriceSuccess() {
      // given
      final RoomId roomId = RoomId.of(1L);
      final Money newDefaultPrice = Money.of(new BigDecimal("50000"));

      final PricingPolicy existingPolicy = PricingPolicy.create(
          roomId,
          PlaceId.of(100L),
          TimeSlot.HOUR,
          Money.of(BigDecimal.ZERO)
      );

      when(pricingPolicyRepository.findById(roomId)).thenReturn(Optional.of(existingPolicy));
      when(pricingPolicyRepository.save(any(PricingPolicy.class))).thenReturn(existingPolicy);

      // when
      final PricingPolicy result = updatePricingPolicyService.updateDefaultPrice(roomId,
          newDefaultPrice);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getDefaultPrice()).isEqualTo(newDefaultPrice);
      verify(pricingPolicyRepository).findById(roomId);
      verify(pricingPolicyRepository).save(existingPolicy);
    }

    @Test
    @DisplayName("존재하지 않는 정책 업데이트 시 예외를 던진다")
    void updateDefaultPriceNotFound() {
      // given
      final RoomId roomId = RoomId.of(999L);
      final Money newDefaultPrice = Money.of(new BigDecimal("50000"));

      when(pricingPolicyRepository.findById(roomId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
          () -> updatePricingPolicyService.updateDefaultPrice(roomId, newDefaultPrice))
          .isInstanceOf(PricingPolicyNotFoundException.class)
          .hasMessageContaining("Pricing policy not found for roomId: 999");
    }
  }

  @Nested
  @DisplayName("updateTimeRangePrices 테스트")
  class UpdateTimeRangePricesTests {

    @Test
    @DisplayName("시간대별 가격을 성공적으로 업데이트한다")
    void updateTimeRangePricesSuccess() {
      // given
      final RoomId roomId = RoomId.of(1L);

      final PricingPolicy existingPolicy = PricingPolicy.create(
          roomId,
          PlaceId.of(100L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("30000"))
      );

      final List<TimeRangePrice> newTimeRangePrices = List.of(
          new TimeRangePrice(
              DayOfWeek.MONDAY,
              TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
              Money.of(new BigDecimal("50000"))
          )
      );

      when(pricingPolicyRepository.findById(roomId)).thenReturn(Optional.of(existingPolicy));
      when(pricingPolicyRepository.save(any(PricingPolicy.class))).thenReturn(existingPolicy);

      // when
      final PricingPolicy result = updatePricingPolicyService.updateTimeRangePrices(roomId,
          newTimeRangePrices);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getTimeRangePrices().getPrices()).hasSize(1);
      verify(pricingPolicyRepository).findById(roomId);
      verify(pricingPolicyRepository).save(existingPolicy);
    }

    @Test
    @DisplayName("존재하지 않는 정책의 시간대별 가격 업데이트 시 예외를 던진다")
    void updateTimeRangePricesNotFound() {
      // given
      final RoomId roomId = RoomId.of(999L);
      final List<TimeRangePrice> newTimeRangePrices = List.of();

      when(pricingPolicyRepository.findById(roomId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
          () -> updatePricingPolicyService.updateTimeRangePrices(roomId, newTimeRangePrices))
          .isInstanceOf(PricingPolicyNotFoundException.class)
          .hasMessageContaining("Pricing policy not found for roomId: 999");
    }
  }
}
