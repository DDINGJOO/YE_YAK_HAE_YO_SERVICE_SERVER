package com.teambind.springproject.adapter.in.messaging.handler;

import com.teambind.springproject.adapter.in.messaging.event.RoomUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * RoomUpdatedEvent 핸들러.
 * 룸 업데이트 이벤트를 수신하여 로깅합니다.
 * TODO: 가격 정책 TimeSlot 업데이트 기능은 별도 이슈로 분리하여 구현 예정
 */
@Component
public class RoomUpdatedEventHandler implements EventHandler<RoomUpdatedEvent> {

  private static final Logger logger = LoggerFactory.getLogger(RoomUpdatedEventHandler.class);

  @Override
  public void handle(final RoomUpdatedEvent event) {
    logger.info("Received RoomUpdatedEvent: roomId={}, placeId={}, timeSlot={}",
        event.getRoomId(), event.getPlaceId(), event.getTimeSlot());

    // 현재는 이벤트 수신 및 로깅만 수행
    // 향후 가격 정책 TimeSlot 업데이트 로직 추가 필요
    logger.info("RoomUpdatedEvent logged successfully");
  }

  @Override
  public String getSupportedEventType() {
    return "RoomUpdated";
  }
}