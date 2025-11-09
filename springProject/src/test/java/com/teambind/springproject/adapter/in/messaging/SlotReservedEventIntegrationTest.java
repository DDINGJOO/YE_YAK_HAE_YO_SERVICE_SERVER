package com.teambind.springproject.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.teambind.springproject.adapter.in.messaging.event.SlotReservedEvent;
import com.teambind.springproject.adapter.in.messaging.handler.SlotReservedEventHandler;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ReservationId;
import com.teambind.springproject.domain.shared.ReservationStatus;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/**
 * SlotReservedEvent 통합 테스트.
 * Spring Context 로딩 및 핸들러 동작 검증.
 */
@SpringBootTest
@DisplayName("SlotReservedEvent 통합 테스트")
class SlotReservedEventIntegrationTest {

  @Autowired
  private SlotReservedEventHandler slotReservedEventHandler;

  @Autowired
  private PricingPolicyRepository pricingPolicyRepository;

  @Autowired
  private ReservationPricingRepository reservationPricingRepository;

  @Test
  @DisplayName("Spring Context 로딩 및 Bean Wiring 검증")
  void contextLoads() {
    assertThat(slotReservedEventHandler).isNotNull();
    assertThat(pricingPolicyRepository).isNotNull();
    assertThat(reservationPricingRepository).isNotNull();
  }

  @Test
  @DisplayName("핸들러의 지원 이벤트 타입 확인")
  void checkSupportedEventType() {
    assertThat(slotReservedEventHandler.getSupportedEventType()).isEqualTo("SlotReserved");
  }
}
