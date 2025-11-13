package com.teambind.springproject.adapter.out.messaging.kafka.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.teambind.springproject.application.dto.response.ProductPriceDetail;
import com.teambind.springproject.application.dto.response.ReservationTimePriceDetail;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;


/**
 * 예약 결제 대기 이벤트
 * 전체 예약이 생성 되어 결제 대기 상태로 진입할때 발행되는 이벤트 입니다.
 *
 * 결제 모듈, 예약 정보관리 모듈이 이 이벤트를 구독합니다.
 */

@Getter
@Setter
public class ReservationPendingPaymentEvent extends Event {
	
	private static final String EVENT_TYPE_NAME = "ReservationPendingPayment";
	private static final String DEFAULT_TOPIC = "reservation-pending-payment";
	private Long reservationId;
	private Long placeId;
	private Long roomId;
	private String reservationDate;  // "yyyy-MM-dd" format (e.g., "2025-11-13")

	private List<ProductPriceDetail> productPriceDetails;
	private ReservationTimePriceDetail reservationTimePriceDetail;
	private BigDecimal totalPrice;
	private String occurredAt;  // ISO DateTime format (e.g., "2025-11-13T11:00:00")


	@JsonCreator
	public ReservationPendingPaymentEvent(
			@JsonProperty("topic") final String topic,
			@JsonProperty("eventType") final String eventType,
			@JsonProperty("reservationId") final Long reservationId,
			@JsonProperty("placeId") final Long placeId,
			@JsonProperty("roomId") final Long roomId,
			@JsonProperty("reservationDate") final String reservationDate,
			@JsonProperty("productPriceDetails") final List<ProductPriceDetail> productPriceDetails,
			@JsonProperty("reservationTimePriceDetail") final ReservationTimePriceDetail reservationTimePriceDetail,
			@JsonProperty("totalPrice") final BigDecimal totalPrice,
			@JsonProperty("occurredAt") final String occurredAt
	){
		super(topic != null ? topic : DEFAULT_TOPIC,
				eventType != null ? eventType : EVENT_TYPE_NAME);
		this.reservationId = reservationId;
		this.placeId = placeId;
		this.roomId = roomId;
		this.reservationDate = reservationDate;
		this.productPriceDetails = productPriceDetails;
		this.reservationTimePriceDetail = reservationTimePriceDetail;
		this.totalPrice = totalPrice;
		this.occurredAt = occurredAt;

	}
	
	
	@Override
	public String getEventTypeName() {
		return DEFAULT_TOPIC;
	}
	
	@Override
	public String toString() {
		return "ReservationPendingPaymentEvent{"
				+ "reservationId=" + reservationId
				+ ", placeId=" + placeId
				+ ", roomId=" + roomId
				+ ", reservationDate=" + reservationDate
				+ ", totalPrice=" + totalPrice
				+ ", occurredAt=" + occurredAt
				+ ", topic='" + getTopic() + '\''
				+ ", eventType='" + getEventType() + '\''
				+ '}';
	}
	
	
	
}
