package com.teambind.springproject.adapter.in.messaging.handler;

import com.teambind.springproject.adapter.in.messaging.event.RoomCreatedEvent;
import com.teambind.springproject.application.port.in.CreatePricingPolicyUseCase;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * RoomCreatedEvent 핸들러.
 * 룸 생성 이벤트를 수신하여 기본 가격 정책을 생성합니다.
 */
@Component
public class RoomCreatedEventHandler implements EventHandler<RoomCreatedEvent> {
	
	private static final Logger logger = LoggerFactory.getLogger(RoomCreatedEventHandler.class);
	
	private final CreatePricingPolicyUseCase createPricingPolicyUseCase;
	
	public RoomCreatedEventHandler(final CreatePricingPolicyUseCase createPricingPolicyUseCase) {
		this.createPricingPolicyUseCase = createPricingPolicyUseCase;
	}
	
	@Override
	public void handle(final RoomCreatedEvent event) {
		logger.info("Handling RoomCreatedEvent: {}", event);
		
		try {
			final RoomId roomId = RoomId.of(event.getRoomId());
			final PlaceId placeId = PlaceId.of(event.getPlaceId());
			final TimeSlot timeSlot = TimeSlot.valueOf(event.getTimeSlot());
			
			createPricingPolicyUseCase.createDefaultPolicy(roomId, placeId, timeSlot);
			
			logger.info("Successfully handled RoomCreatedEvent for roomId={}", event.getRoomId());
		} catch (final Exception e) {
			logger.error("Failed to handle RoomCreatedEvent: {}", event, e);
			throw new RuntimeException("Failed to handle RoomCreatedEvent", e);
		}
	}
	
	@Override
	public String getSupportedEventType() {
		return "RoomCreated";
	}
}
