package com.teambind.springproject.application.service.pricingpolicy;

import com.teambind.springproject.application.port.in.GetDatePricingUseCase;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.exception.PricingPolicyNotFoundException;
import com.teambind.springproject.domain.shared.DayOfWeek;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.RoomId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 특정 날짜의 시간대별 가격 조회 서비스.
 */
@Service
@Transactional(readOnly = true)
public class GetDatePricingService implements GetDatePricingUseCase {

	private static final Logger logger = LoggerFactory.getLogger(GetDatePricingService.class);
	private static final LocalTime START_TIME = LocalTime.of(0, 0);
	private static final LocalTime END_TIME = LocalTime.of(23, 59);

	private final PricingPolicyRepository pricingPolicyRepository;

	public GetDatePricingService(final PricingPolicyRepository pricingPolicyRepository) {
		this.pricingPolicyRepository = pricingPolicyRepository;
	}

	@Override
	public Map<String, BigDecimal> getPricingByDate(final RoomId roomId, final LocalDate date) {
		logger.info("Fetching pricing for roomId={}, date={}", roomId.getValue(), date);

		final PricingPolicy policy = pricingPolicyRepository.findById(roomId)
				.orElseThrow(() -> new PricingPolicyNotFoundException(
						"Pricing policy not found for roomId: " + roomId.getValue()));

		final DayOfWeek dayOfWeek = DayOfWeek.from(date.getDayOfWeek());
		final int timeSlotMinutes = policy.getTimeSlot().getMinutes();

		return buildTimeSlotPrices(policy, dayOfWeek, timeSlotMinutes);
	}

	private Map<String, BigDecimal> buildTimeSlotPrices(
			final PricingPolicy policy,
			final DayOfWeek dayOfWeek,
			final int timeSlotMinutes) {

		final Map<String, BigDecimal> timeSlotPrices = new LinkedHashMap<>();
		LocalTime currentTime = START_TIME;

		while (currentTime.isBefore(END_TIME) || currentTime.equals(END_TIME)) {
			final Money price = findPriceForTime(policy, dayOfWeek, currentTime);
			timeSlotPrices.put(currentTime.toString(), price.getAmount());

			currentTime = currentTime.plusMinutes(timeSlotMinutes);

			if (currentTime.isAfter(END_TIME)) {
				break;
			}
		}

		return timeSlotPrices;
	}

	private Money findPriceForTime(
			final PricingPolicy policy,
			final DayOfWeek dayOfWeek,
			final LocalTime time) {

		return policy.getTimeRangePrices()
				.findPriceForSlot(dayOfWeek, time)
				.orElse(policy.getDefaultPrice());
	}
}