package com.teambind.springproject.adapter.in.messaging;

import com.teambind.springproject.adapter.in.messaging.handler.SlotReservedEventHandler;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

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
