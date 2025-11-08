package com.teambind.springproject.application.service.pricingpolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrice;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrices;
import com.teambind.springproject.domain.pricingpolicy.exception.CannotCopyDifferentPlaceException;
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
@DisplayName("CopyPricingPolicyService 단위 테스트")
class CopyPricingPolicyServiceTest {

  @Mock
  private PricingPolicyRepository pricingPolicyRepository;

  @InjectMocks
  private CopyPricingPolicyService copyPricingPolicyService;

  @Nested
  @DisplayName("copyFromRoom 테스트")
  class CopyFromRoomTests {

    @Test
    @DisplayName("같은 PlaceId를 가진 룸 간 정책 복사에 성공한다")
    void copyFromRoomSuccess() {
      // given
      final PlaceId samePlace = PlaceId.of(100L);
      final RoomId sourceRoomId = RoomId.of(1L);
      final RoomId targetRoomId = RoomId.of(2L);

      final List<TimeRangePrice> sourceTimeRangePrices = List.of(
          new TimeRangePrice(
              DayOfWeek.MONDAY,
              TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
              Money.of(new BigDecimal("50000"))
          )
      );

      final PricingPolicy sourcePolicy = PricingPolicy.createWithTimeRangePrices(
          sourceRoomId,
          samePlace,
          TimeSlot.HOUR,
          Money.of(new BigDecimal("30000")),
          TimeRangePrices.of(sourceTimeRangePrices)
      );

      final PricingPolicy targetPolicy = PricingPolicy.create(
          targetRoomId,
          samePlace,
          TimeSlot.HOUR,
          Money.of(BigDecimal.ZERO)
      );

      when(pricingPolicyRepository.findById(sourceRoomId)).thenReturn(
          Optional.of(sourcePolicy));
      when(pricingPolicyRepository.findById(targetRoomId)).thenReturn(
          Optional.of(targetPolicy));
      when(pricingPolicyRepository.save(any(PricingPolicy.class))).thenReturn(targetPolicy);

      // when
      final PricingPolicy result = copyPricingPolicyService.copyFromRoom(targetRoomId,
          sourceRoomId);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getRoomId()).isEqualTo(targetRoomId);
      assertThat(result.getDefaultPrice()).isEqualTo(sourcePolicy.getDefaultPrice());
      assertThat(result.getTimeRangePrices().getPrices()).hasSize(1);
      verify(pricingPolicyRepository).save(targetPolicy);
    }

    @Test
    @DisplayName("다른 PlaceId 간 복사 시도 시 예외를 던진다")
    void copyFromRoomDifferentPlace() {
      // given
      final RoomId sourceRoomId = RoomId.of(1L);
      final RoomId targetRoomId = RoomId.of(2L);

      final PricingPolicy sourcePolicy = PricingPolicy.create(
          sourceRoomId,
          PlaceId.of(100L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("30000"))
      );

      final PricingPolicy targetPolicy = PricingPolicy.create(
          targetRoomId,
          PlaceId.of(200L),  // 다른 PlaceId
          TimeSlot.HOUR,
          Money.of(BigDecimal.ZERO)
      );

      when(pricingPolicyRepository.findById(sourceRoomId)).thenReturn(
          Optional.of(sourcePolicy));
      when(pricingPolicyRepository.findById(targetRoomId)).thenReturn(
          Optional.of(targetPolicy));

      // when & then
      assertThatThrownBy(() -> copyPricingPolicyService.copyFromRoom(targetRoomId, sourceRoomId))
          .isInstanceOf(CannotCopyDifferentPlaceException.class)
          .hasMessageContaining("Cannot copy pricing policy between different places");
    }

    @Test
    @DisplayName("원본 정책이 존재하지 않으면 예외를 던진다")
    void copyFromRoomSourceNotFound() {
      // given
      final RoomId sourceRoomId = RoomId.of(999L);
      final RoomId targetRoomId = RoomId.of(2L);

      when(pricingPolicyRepository.findById(sourceRoomId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> copyPricingPolicyService.copyFromRoom(targetRoomId, sourceRoomId))
          .isInstanceOf(PricingPolicyNotFoundException.class)
          .hasMessageContaining("Source pricing policy not found for roomId: 999");
    }

    @Test
    @DisplayName("대상 정책이 존재하지 않으면 예외를 던진다")
    void copyFromRoomTargetNotFound() {
      // given
      final RoomId sourceRoomId = RoomId.of(1L);
      final RoomId targetRoomId = RoomId.of(999L);

      final PricingPolicy sourcePolicy = PricingPolicy.create(
          sourceRoomId,
          PlaceId.of(100L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("30000"))
      );

      when(pricingPolicyRepository.findById(sourceRoomId)).thenReturn(
          Optional.of(sourcePolicy));
      when(pricingPolicyRepository.findById(targetRoomId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> copyPricingPolicyService.copyFromRoom(targetRoomId, sourceRoomId))
          .isInstanceOf(PricingPolicyNotFoundException.class)
          .hasMessageContaining("Target pricing policy not found for roomId: 999");
    }
  }
}
