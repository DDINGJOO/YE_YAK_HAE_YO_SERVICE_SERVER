package com.teambind.springproject.adapter.in.messaging.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * 예약 확정 이벤트.
 * 결제 서비스에서 결제가 완료되어 예약이 확정되었을 때 발행되는 이벤트입니다.
 *
 * 이 이벤트를 수신하면 해당 예약의 상태를 PENDING → CONFIRMED로 변경합니다.
 */
public final class ReservationConfirmedEvent extends Event {

  private static final String EVENT_TYPE_NAME = "ReservationConfirmed";
  private static final String DEFAULT_TOPIC = "reservation-confirmed";

  private final Long reservationId;
  private final LocalDateTime occurredAt;

  @JsonCreator
  public ReservationConfirmedEvent(
      @JsonProperty("topic") final String topic,
      @JsonProperty("eventType") final String eventType,
      @JsonProperty("reservationId") final Long reservationId,
      @JsonProperty("occurredAt") final LocalDateTime occurredAt) {
    super(topic != null ? topic : DEFAULT_TOPIC, eventType != null ? eventType : EVENT_TYPE_NAME);
    this.reservationId = reservationId;
    this.occurredAt = occurredAt;
  }

  @Override
  public String getEventTypeName() {
    return EVENT_TYPE_NAME;
  }

  public Long getReservationId() {
    return reservationId;
  }

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }

  @Override
  public String toString() {
    return "ReservationConfirmedEvent{"
        + "reservationId=" + reservationId
        + ", occurredAt=" + occurredAt
        + ", topic='" + getTopic() + '\''
        + ", eventType='" + getEventType() + '\''
        + '}';
  }
}