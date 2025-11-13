package com.teambind.springproject.adapter.in.messaging.kafka.handler;

import com.teambind.springproject.adapter.in.messaging.kafka.event.SlotReservedEvent;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.ReservationId;
import com.teambind.springproject.domain.shared.RoomId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SlotReservedEvent 핸들러.
 * 슬롯 예약 이벤트를 수신하여 예약 가격 정보를 Pending 상태로 생성합니다.
 * 이 시점에는 시간대 가격만 계산하며, 추가상품 정보는 예약 확정 시 업데이트됩니다.
 */
@Component
@Transactional
public class SlotReservedEventHandler implements EventHandler<SlotReservedEvent> {
	
	private static final Logger logger = LoggerFactory.getLogger(SlotReservedEventHandler.class);
	
	private final PricingPolicyRepository pricingPolicyRepository;
	private final ReservationPricingRepository reservationPricingRepository;
	private final long pendingTimeoutMinutes;
	
	public SlotReservedEventHandler(
			final PricingPolicyRepository pricingPolicyRepository,
			final ReservationPricingRepository reservationPricingRepository,
			final com.teambind.springproject.common.config.ReservationConfiguration reservationConfiguration) {
		this.pricingPolicyRepository = pricingPolicyRepository;
		this.reservationPricingRepository = reservationPricingRepository;
		this.pendingTimeoutMinutes = reservationConfiguration.getPending().getTimeoutMinutes();
	}
	
	@Override
	public void handle(final SlotReservedEvent event) {
		logger.info("Handling SlotReservedEvent: reservationId={}, roomId={}, slotDate={}, slots={}",
				event.getReservationId(), event.getRoomId(), event.getSlotDate(),
				event.getStartTimes().size());
		
		try {
			final ReservationId reservationId = ReservationId.of(event.getReservationId());
			
			// 1. 멱등성 검사: 이미 처리된 예약인지 확인
			if (reservationPricingRepository.existsById(reservationId)) {
				logger.info("Reservation already exists, skipping: reservationId={}",
						event.getReservationId());
				return;
			}
			
			final RoomId roomId = RoomId.of(event.getRoomId());
			
			// 2. 가격 정책 조회
			final PricingPolicy pricingPolicy = pricingPolicyRepository.findById(roomId)
					.orElseThrow(() -> new IllegalArgumentException(
							"Pricing policy not found for roomId: " + event.getRoomId()));
			
			// 3. 시간 슬롯 목록 생성 (slotDate + startTimes -> List<LocalDateTime>)
			final List<LocalDateTime> timeSlots = event.getStartTimes().stream()
					.map(startTime -> event.getSlotDate().atTime(startTime))
					.sorted()
					.collect(Collectors.toList());
			
			if (timeSlots.isEmpty()) {
				throw new IllegalArgumentException("Time slots cannot be empty");
			}
			
			// 4. 시간대별 가격 계산
			final TimeSlotPriceBreakdown timeSlotBreakdown = calculateTimeSlotBreakdown(
					pricingPolicy, timeSlots);
			
			// 5. 예약 가격 생성 (상품 없이 시간대 가격만 계산)
			final ReservationPricing reservationPricing = ReservationPricing.calculate(
					reservationId,
					roomId,
					timeSlotBreakdown,
					Collections.emptyList(),  // 상품 정보는 예약 확정 시 업데이트
					pendingTimeoutMinutes
			);
			
			// 6. 저장
			reservationPricingRepository.save(reservationPricing);
			
			logger.info("Successfully handled SlotReservedEvent: reservationId={}, totalPrice={}",
					event.getReservationId(), reservationPricing.getTotalPrice().getAmount());
			
		} catch (final Exception e) {
			logger.error("Failed to handle SlotReservedEvent: {}", event, e);
			throw new RuntimeException("Failed to handle SlotReservedEvent", e);
		}
	}
	
	@Override
	public String getSupportedEventType() {
		return "SlotReserved";
	}
	
	/**
	 * 시간대별 가격 내역을 계산합니다.
	 *
	 * @param pricingPolicy 가격 정책
	 * @param timeSlots     시간 슬롯 목록 (정렬된 상태)
	 * @return 시간대별 가격 내역
	 */
	private TimeSlotPriceBreakdown calculateTimeSlotBreakdown(
			final PricingPolicy pricingPolicy,
			final List<LocalDateTime> timeSlots) {
		
		final LocalDateTime start = timeSlots.get(0);
		final LocalDateTime end = timeSlots.get(timeSlots.size() - 1)
				.plusMinutes(pricingPolicy.getTimeSlot().getMinutes());
		
		final PricingPolicy.PriceBreakdown priceBreakdown =
				pricingPolicy.calculatePriceBreakdown(start, end);
		
		// PriceBreakdown을 TimeSlotPriceBreakdown으로 변환
		final Map<LocalDateTime, Money> slotPrices = new HashMap<>();
		for (final PricingPolicy.SlotPrice slotPrice : priceBreakdown.getSlotPrices()) {
			slotPrices.put(slotPrice.slotTime(), slotPrice.price());
		}
		
		return new TimeSlotPriceBreakdown(slotPrices, pricingPolicy.getTimeSlot());
	}
}
