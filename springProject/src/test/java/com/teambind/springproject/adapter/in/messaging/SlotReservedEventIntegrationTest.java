package com.teambind.springproject.adapter.in.messaging;

import com.teambind.springproject.adapter.in.messaging.kafka.handler.SlotReservedEventHandler;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.application.port.out.publisher.EventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SlotReservedEvent 통합 테스트.
 * Spring Context 로딩 및 핸들러 동작 검증.
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"room-created", "room-updated", "reservation-reserved", "reservation-confirmed", "reservation-cancelled", "reservation-refund"})
@ActiveProfiles("test")
@DisplayName("SlotReservedEvent 통합 테스트")
class SlotReservedEventIntegrationTest {

	@MockBean
	private EventPublisher eventPublisher;

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
