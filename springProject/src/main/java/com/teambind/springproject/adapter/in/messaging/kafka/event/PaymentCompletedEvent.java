package com.teambind.springproject.adapter.in.messaging.kafka.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 결제 완료 이벤트.
 * 결제 서비스에서 토스페이먼츠 결제 승인이 완료되었을 때 발행되는 이벤트입니다.
 *
 * 이 이벤트를 수신하면 해당 예약의 상태를 PENDING → CONFIRMED로 변경합니다.
 */
public final class PaymentCompletedEvent extends Event {

	private static final String EVENT_TYPE_NAME = "PaymentCompleted";
	private static final String DEFAULT_TOPIC = "payment-completed";

	private final String paymentId;
	private final Long reservationId;
	private final String orderId;
	private final String paymentKey;
	private final Long amount;
	private final String method;
	private final LocalDateTime paidAt;

	@JsonCreator
	public PaymentCompletedEvent(
			@JsonProperty("topic") final String topic,
			@JsonProperty("eventType") final String eventType,
			@JsonProperty("paymentId") final String paymentId,
			@JsonProperty("reservationId") final Object reservationId,
			@JsonProperty("orderId") final String orderId,
			@JsonProperty("paymentKey") final String paymentKey,
			@JsonProperty("amount") final Long amount,
			@JsonProperty("method") final String method,
			@JsonProperty("paidAt") final LocalDateTime paidAt) {
		super(topic != null ? topic : DEFAULT_TOPIC, eventType != null ? eventType : EVENT_TYPE_NAME);
		this.paymentId = paymentId;
		this.reservationId = parseLong(reservationId);
		this.orderId = orderId;
		this.paymentKey = paymentKey;
		this.amount = amount;
		this.method = method;
		this.paidAt = paidAt;
	}

	@Override
	public String getEventTypeName() {
		return EVENT_TYPE_NAME;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public Long getReservationId() {
		return reservationId;
	}

	public String getOrderId() {
		return orderId;
	}

	public String getPaymentKey() {
		return paymentKey;
	}

	public Long getAmount() {
		return amount;
	}

	public String getMethod() {
		return method;
	}

	public LocalDateTime getPaidAt() {
		return paidAt;
	}

	@Override
	public String toString() {
		return "PaymentCompletedEvent{"
				+ "paymentId='" + paymentId + '\''
				+ ", reservationId=" + reservationId
				+ ", orderId='" + orderId + '\''
				+ ", paymentKey='" + paymentKey + '\''
				+ ", amount=" + amount
				+ ", method='" + method + '\''
				+ ", paidAt=" + paidAt
				+ ", topic='" + getTopic() + '\''
				+ ", eventType='" + getEventType() + '\''
				+ '}';
	}
}