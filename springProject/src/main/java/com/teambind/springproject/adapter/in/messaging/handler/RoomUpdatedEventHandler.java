package com.teambind.springproject.adapter.in.messaging.handler;

import com.teambind.springproject.adapter.in.messaging.event.RoomUpdatedEvent;
import com.teambind.springproject.application.port.in.UpdatePricingPolicyUseCase;
import com.teambind.springproject.domain.pricingpolicy.exception.PricingPolicyNotFoundException;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * RoomUpdatedEvent 핸들러.
 * 룸 업데이트 이벤트를 수신하여 가격 정책의 TimeSlot을 업데이트합니다.
 */
@Component
public class RoomUpdatedEventHandler implements EventHandler<RoomUpdatedEvent> {
	
	private static final Logger logger = LoggerFactory.getLogger(RoomUpdatedEventHandler.class);
	
	private final UpdatePricingPolicyUseCase updatePricingPolicyUseCase;
	
	public RoomUpdatedEventHandler(final UpdatePricingPolicyUseCase updatePricingPolicyUseCase) {
		this.updatePricingPolicyUseCase = updatePricingPolicyUseCase;
	}
	
	@Override
	public void handle(final RoomUpdatedEvent event) {
		logger.info("Handling RoomUpdatedEvent: roomId={}, placeId={}, timeSlot={}",
				event.getRoomId(), event.getPlaceId(), event.getTimeSlot());
		
		try {
			final RoomId roomId = RoomId.of(event.getRoomId());
			final TimeSlot newTimeSlot = TimeSlot.valueOf(event.getTimeSlot());
			
			updatePricingPolicyUseCase.updateTimeSlot(roomId, newTimeSlot);
			
			logger.info("Successfully updated TimeSlot for roomId={}", event.getRoomId());
		} catch (final PricingPolicyNotFoundException e) {
			logger.warn("PricingPolicy not found for roomId={}, skipping TimeSlot update",
					event.getRoomId());
		} catch (final Exception e) {
			logger.error("Failed to handle RoomUpdatedEvent: {}", event, e);
			throw new RuntimeException("Failed to handle RoomUpdatedEvent", e);
		}
	}
	
	@Override
	public String getSupportedEventType() {
		return "RoomUpdated";
	}
}
