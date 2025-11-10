package com.teambind.springproject.adapter.in.messaging.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * 예약 취소 이벤트.
 * 결제 서비스에서 결제가 실패하거나 취소되어 예약이 취소되었을 때 발행되는 이벤트입니다.
 *
 * 이 이벤트를 수신하면 해당 예약의 상태를 PENDING → CANCELLED로 변경하고,
 * 재고가 자동으로 복구됩니다 (CANCELLED 상태는 재고 계산에서 제외됨).
 */
public final class ReservationCancelledEvent extends Event {

  private static final String EVENT_TYPE_NAME = "ReservationCancelled";
  private static final String DEFAULT_TOPIC = "reservation-cancelled";

  private final Long reservationId;
  private final LocalDateTime occurredAt;

  @JsonCreator
  public ReservationCancelledEvent(
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
    return "ReservationCancelledEvent{"
        + "reservationId=" + reservationId
        + ", occurredAt=" + occurredAt
        + ", topic='" + getTopic() + '\''
        + ", eventType='" + getEventType() + '\''
        + '}';
  }
}