package com.teambind.springproject.application.dto.response;

import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrice;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 가격 정책 응답 DTO.
 */
public record PricingPolicyResponse(
    Long roomId,
    Long placeId,
    String timeSlot,
    BigDecimal defaultPrice,
    List<TimeRangePriceResponse> timeRangePrices
) {

  /**
   * PricingPolicy 도메인 객체로부터 Response DTO를 생성합니다.
   *
   * @param policy 가격 정책 도메인 객체
   * @return PricingPolicyResponse
   */
  public static PricingPolicyResponse from(final PricingPolicy policy) {
    final List<TimeRangePriceResponse> timeRangePriceResponses = policy.getTimeRangePrices()
        .getPrices()
        .stream()
        .map(PricingPolicyResponse::toTimeRangePriceResponse)
        .collect(Collectors.toList());

    return new PricingPolicyResponse(
        policy.getRoomId().getValue(),
        policy.getPlaceId().getValue(),
        policy.getTimeSlot().name(),
        policy.getDefaultPrice().getAmount(),
        timeRangePriceResponses
    );
  }

  private static TimeRangePriceResponse toTimeRangePriceResponse(
      final TimeRangePrice timeRangePrice) {
    return new TimeRangePriceResponse(
        timeRangePrice.dayOfWeek().name(),
        timeRangePrice.timeRange().getStartTime().toString(),
        timeRangePrice.timeRange().getEndTime().toString(),
        timeRangePrice.pricePerSlot().getAmount()
    );
  }
}
