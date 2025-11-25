package com.teambind.springproject.adapter.in.messaging.kafka.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 환불 완료 이벤트.
 * 결제 서비스에서 토스페이먼츠 환불이 완료되었을 때 발행되는 이벤트입니다.
 *
 * 이 이벤트를 수신하면 해당 예약의 상태를 CONFIRMED → CANCELLED로 변경하고,
 * 예약으로 선점한 재고를 롤백 처리합니다.
 *
 * 주의: reservationId 필드는 필수이며, 결제 서비스에서 반드시 포함해야 합니다.
 */
public final class RefundCompletedEvent extends Event {

	private static final String EVENT_TYPE_NAME = "RefundCompleted";
	private static final String DEFAULT_TOPIC = "refund-completed";

	private final String refundId;
	private final String paymentId;
	private final Long reservationId;
	private final Long originalAmount;
	private final Long refundAmount;
	private final String reason;
	private final LocalDateTime completedAt;

	@JsonCreator
	public RefundCompletedEvent(
			@JsonProperty("topic") final String topic,
			@JsonProperty("eventType") final String eventType,
			@JsonProperty("refundId") final String refundId,
			@JsonProperty("paymentId") final String paymentId,
			@JsonProperty("reservationId") final Object reservationId,
			@JsonProperty("originalAmount") final Long originalAmount,
			@JsonProperty("refundAmount") final Long refundAmount,
			@JsonProperty("reason") final String reason,
			@JsonProperty("completedAt") final LocalDateTime completedAt) {
		super(topic != null ? topic : DEFAULT_TOPIC, eventType != null ? eventType : EVENT_TYPE_NAME);
		this.refundId = refundId;
		this.paymentId = paymentId;
		this.reservationId = parseLong(reservationId);
		this.originalAmount = originalAmount;
		this.refundAmount = refundAmount;
		this.reason = reason;
		this.completedAt = completedAt;
	}

	@Override
	public String getEventTypeName() {
		return EVENT_TYPE_NAME;
	}

	public String getRefundId() {
		return refundId;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public Long getReservationId() {
		return reservationId;
	}

	public Long getOriginalAmount() {
		return originalAmount;
	}

	public Long getRefundAmount() {
		return refundAmount;
	}

	public String getReason() {
		return reason;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

	@Override
	public String toString() {
		return "RefundCompletedEvent{"
				+ "refundId='" + refundId + '\''
				+ ", paymentId='" + paymentId + '\''
				+ ", reservationId=" + reservationId
				+ ", originalAmount=" + originalAmount
				+ ", refundAmount=" + refundAmount
				+ ", reason='" + reason + '\''
				+ ", completedAt=" + completedAt
				+ ", topic='" + getTopic() + '\''
				+ ", eventType='" + getEventType() + '\''
				+ '}';
	}
}