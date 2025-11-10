package com.teambind.springproject.adapter.in.messaging.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teambind.springproject.adapter.in.messaging.event.SlotReservedEvent;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.common.config.ReservationConfiguration;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrices;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ReservationId;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("SlotReservedEventHandler 테스트")
@ExtendWith(MockitoExtension.class)
class SlotReservedEventHandlerTest {

  @Mock
  private PricingPolicyRepository pricingPolicyRepository;

  @Mock
  private ReservationPricingRepository reservationPricingRepository;

  @Mock
  private ReservationConfiguration reservationConfiguration;

  private SlotReservedEventHandler handler;

  private PricingPolicy samplePricingPolicy;

  @BeforeEach
  void setUp() {
    final ReservationConfiguration.Pending pending = new ReservationConfiguration.Pending();
    pending.setTimeoutMinutes(10L);
    when(reservationConfiguration.getPending()).thenReturn(pending);

    handler = new SlotReservedEventHandler(
        pricingPolicyRepository,
        reservationPricingRepository,
        reservationConfiguration
    );

    // 샘플 가격 정책 생성 (30분 단위, 기본 가격 10,000원)
    samplePricingPolicy = PricingPolicy.create(
        RoomId.of(1L),
        PlaceId.of(100L),
        TimeSlot.HALFHOUR,
        Money.of(new BigDecimal("10000"))
    );
  }

  @Test
  @DisplayName("이벤트 처리 성공 - 예약 가격 정보 생성")
  void handleEventSuccess() {
    // Given
    final SlotReservedEvent event = new SlotReservedEvent(
        "reservation-reserved",
        "SlotReserved",
        1L,
        LocalDate.of(2025, 11, 15),
        List.of(LocalTime.of(10, 0), LocalTime.of(10, 30)),
        1000L,
        LocalDateTime.now()
    );

    when(reservationPricingRepository.existsById(ReservationId.of(1000L)))
        .thenReturn(false);
    when(pricingPolicyRepository.findById(RoomId.of(1L)))
        .thenReturn(Optional.of(samplePricingPolicy));

    // When
    handler.handle(event);

    // Then
    final ArgumentCaptor<ReservationPricing> captor =
        ArgumentCaptor.forClass(ReservationPricing.class);
    verify(reservationPricingRepository).save(captor.capture());

    final ReservationPricing saved = captor.getValue();
    assertThat(saved.getReservationId().getValue()).isEqualTo(1000L);
    assertThat(saved.getRoomId().getValue()).isEqualTo(1L);
    assertThat(saved.getProductBreakdowns()).isEmpty();
    assertThat(saved.getTotalPrice().getAmount()).isPositive();
  }

  @Test
  @DisplayName("멱등성 검사 - 이미 존재하는 예약은 스킵")
  void handleEventIdempotency() {
    // Given
    final SlotReservedEvent event = new SlotReservedEvent(
        "reservation-reserved",
        "SlotReserved",
        1L,
        LocalDate.of(2025, 11, 15),
        List.of(LocalTime.of(10, 0)),
        1000L,
        LocalDateTime.now()
    );

    when(reservationPricingRepository.existsById(ReservationId.of(1000L)))
        .thenReturn(true);

    // When
    handler.handle(event);

    // Then
    verify(reservationPricingRepository, never()).save(any());
    verify(pricingPolicyRepository, never()).findById(any());
  }

  @Test
  @DisplayName("가격 정책이 없는 경우 예외 발생")
  void handleEventNoPricingPolicy() {
    // Given
    final SlotReservedEvent event = new SlotReservedEvent(
        "reservation-reserved",
        "SlotReserved",
        999L,
        LocalDate.of(2025, 11, 15),
        List.of(LocalTime.of(10, 0)),
        1000L,
        LocalDateTime.now()
    );

    when(reservationPricingRepository.existsById(ReservationId.of(1000L)))
        .thenReturn(false);
    when(pricingPolicyRepository.findById(RoomId.of(999L)))
        .thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> handler.handle(event))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to handle SlotReservedEvent");

    verify(reservationPricingRepository, never()).save(any());
  }

  @Test
  @DisplayName("시간 슬롯이 비어있는 경우 예외 발생")
  void handleEventEmptyTimeSlots() {
    // Given
    final SlotReservedEvent event = new SlotReservedEvent(
        "reservation-reserved",
        "SlotReserved",
        1L,
        LocalDate.of(2025, 11, 15),
        List.of(),  // 빈 리스트
        1000L,
        LocalDateTime.now()
    );

    when(reservationPricingRepository.existsById(ReservationId.of(1000L)))
        .thenReturn(false);
    when(pricingPolicyRepository.findById(RoomId.of(1L)))
        .thenReturn(Optional.of(samplePricingPolicy));

    // When & Then
    assertThatThrownBy(() -> handler.handle(event))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to handle SlotReservedEvent");
  }

  @Test
  @DisplayName("여러 시간 슬롯 처리 성공")
  void handleEventMultipleTimeSlots() {
    // Given
    final SlotReservedEvent event = new SlotReservedEvent(
        "reservation-reserved",
        "SlotReserved",
        1L,
        LocalDate.of(2025, 11, 15),
        List.of(
            LocalTime.of(10, 0),
            LocalTime.of(10, 30),
            LocalTime.of(11, 0),
            LocalTime.of(11, 30)
        ),
        2000L,
        LocalDateTime.now()
    );

    when(reservationPricingRepository.existsById(ReservationId.of(2000L)))
        .thenReturn(false);
    when(pricingPolicyRepository.findById(RoomId.of(1L)))
        .thenReturn(Optional.of(samplePricingPolicy));

    // When
    handler.handle(event);

    // Then
    final ArgumentCaptor<ReservationPricing> captor =
        ArgumentCaptor.forClass(ReservationPricing.class);
    verify(reservationPricingRepository).save(captor.capture());

    final ReservationPricing saved = captor.getValue();
    assertThat(saved.getTimeSlotBreakdown().slotPrices()).hasSize(4);
    assertThat(saved.getTotalPrice().getAmount())
        .isEqualByComparingTo(new BigDecimal("40000"));  // 4 slots * 10,000원
  }

  @Test
  @DisplayName("getSupportedEventType 반환값 확인")
  void getSupportedEventType() {
    // When & Then
    assertThat(handler.getSupportedEventType()).isEqualTo("SlotReserved");
  }

  @Test
  @DisplayName("정렬되지 않은 시간 슬롯도 정상 처리")
  void handleEventUnsortedTimeSlots() {
    // Given
    final SlotReservedEvent event = new SlotReservedEvent(
        "reservation-reserved",
        "SlotReserved",
        1L,
        LocalDate.of(2025, 11, 15),
        List.of(
            LocalTime.of(11, 0),  // 순서가 섞여있음
            LocalTime.of(10, 0),
            LocalTime.of(10, 30)
        ),
        3000L,
        LocalDateTime.now()
    );

    when(reservationPricingRepository.existsById(ReservationId.of(3000L)))
        .thenReturn(false);
    when(pricingPolicyRepository.findById(RoomId.of(1L)))
        .thenReturn(Optional.of(samplePricingPolicy));

    // When
    handler.handle(event);

    // Then
    final ArgumentCaptor<ReservationPricing> captor =
        ArgumentCaptor.forClass(ReservationPricing.class);
    verify(reservationPricingRepository).save(captor.capture());

    final ReservationPricing saved = captor.getValue();
    assertThat(saved.getTimeSlotBreakdown().slotPrices()).hasSize(3);
  }
}
