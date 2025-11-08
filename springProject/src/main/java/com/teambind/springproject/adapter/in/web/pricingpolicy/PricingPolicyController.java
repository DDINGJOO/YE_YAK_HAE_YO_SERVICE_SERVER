package com.teambind.springproject.adapter.in.web.pricingpolicy;

import com.teambind.springproject.application.dto.request.CopyPricingPolicyRequest;
import com.teambind.springproject.application.dto.request.TimeRangePriceDto;
import com.teambind.springproject.application.dto.request.UpdateDefaultPriceRequest;
import com.teambind.springproject.application.dto.request.UpdateTimeRangePricesRequest;
import com.teambind.springproject.application.dto.response.PricingPolicyResponse;
import com.teambind.springproject.application.port.in.CopyPricingPolicyUseCase;
import com.teambind.springproject.application.port.in.GetPricingPolicyUseCase;
import com.teambind.springproject.application.port.in.UpdatePricingPolicyUseCase;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrice;
import com.teambind.springproject.domain.shared.DayOfWeek;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeRange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가격 정책 관리 REST Controller.
 */
@RestController
@RequestMapping("/api/pricing-policies")
@Validated
public class PricingPolicyController {

  private final GetPricingPolicyUseCase getPricingPolicyUseCase;
  private final UpdatePricingPolicyUseCase updatePricingPolicyUseCase;
  private final CopyPricingPolicyUseCase copyPricingPolicyUseCase;

  public PricingPolicyController(
      final GetPricingPolicyUseCase getPricingPolicyUseCase,
      final UpdatePricingPolicyUseCase updatePricingPolicyUseCase,
      final CopyPricingPolicyUseCase copyPricingPolicyUseCase) {
    this.getPricingPolicyUseCase = getPricingPolicyUseCase;
    this.updatePricingPolicyUseCase = updatePricingPolicyUseCase;
    this.copyPricingPolicyUseCase = copyPricingPolicyUseCase;
  }

  /**
   * 가격 정책 조회.
   *
   * @param roomId 룸 ID
   * @return 가격 정책
   */
  @GetMapping("/{roomId}")
  public ResponseEntity<PricingPolicyResponse> getPricingPolicy(
      @PathVariable @Positive(message = "Room ID must be positive") final Long roomId) {

    final PricingPolicy policy = getPricingPolicyUseCase.getPolicy(RoomId.of(roomId));
    final PricingPolicyResponse response = PricingPolicyResponse.from(policy);

    return ResponseEntity.ok(response);
  }

  /**
   * 기본 가격 업데이트.
   *
   * @param roomId  룸 ID
   * @param request 기본 가격 업데이트 요청
   * @return 업데이트된 가격 정책
   */
  @PutMapping("/{roomId}/default-price")
  public ResponseEntity<PricingPolicyResponse> updateDefaultPrice(
      @PathVariable @Positive(message = "Room ID must be positive") final Long roomId,
      @RequestBody @Valid final UpdateDefaultPriceRequest request) {

    final PricingPolicy policy = updatePricingPolicyUseCase.updateDefaultPrice(
        RoomId.of(roomId),
        Money.of(request.defaultPrice())
    );

    final PricingPolicyResponse response = PricingPolicyResponse.from(policy);

    return ResponseEntity.ok(response);
  }

  /**
   * 시간대별 가격 업데이트.
   *
   * @param roomId  룸 ID
   * @param request 시간대별 가격 업데이트 요청
   * @return 업데이트된 가격 정책
   */
  @PutMapping("/{roomId}/time-range-prices")
  public ResponseEntity<PricingPolicyResponse> updateTimeRangePrices(
      @PathVariable @Positive(message = "Room ID must be positive") final Long roomId,
      @RequestBody @Valid final UpdateTimeRangePricesRequest request) {

    final List<TimeRangePrice> timeRangePrices = convertToTimeRangePriceList(
        request.timeRangePrices());

    final PricingPolicy policy = updatePricingPolicyUseCase.updateTimeRangePrices(
        RoomId.of(roomId),
        timeRangePrices
    );

    final PricingPolicyResponse response = PricingPolicyResponse.from(policy);

    return ResponseEntity.ok(response);
  }

  /**
   * 다른 룸의 가격 정책 복사.
   * 같은 PlaceId를 가진 룸 간에만 복사 가능합니다.
   *
   * @param targetRoomId 복사 대상 룸 ID
   * @param request      복사 요청 (원본 룸 ID)
   * @return 업데이트된 대상 룸의 가격 정책
   */
  @PostMapping("/{targetRoomId}/copy")
  public ResponseEntity<PricingPolicyResponse> copyPricingPolicy(
      @PathVariable @Positive(message = "Target room ID must be positive") final Long targetRoomId,
      @RequestBody @Valid final CopyPricingPolicyRequest request) {

    final PricingPolicy policy = copyPricingPolicyUseCase.copyFromRoom(
        RoomId.of(targetRoomId),
        RoomId.of(request.sourceRoomId())
    );

    final PricingPolicyResponse response = PricingPolicyResponse.from(policy);

    return ResponseEntity.ok(response);
  }

  private List<TimeRangePrice> convertToTimeRangePriceList(
      final Iterable<TimeRangePriceDto> timeRangePriceDtos) {
    final List<TimeRangePrice> result = new ArrayList<>();

    for (final TimeRangePriceDto dto : timeRangePriceDtos) {
      final DayOfWeek dayOfWeek = DayOfWeek.valueOf(dto.dayOfWeek());
      final LocalTime startTime = LocalTime.parse(dto.startTime());
      final LocalTime endTime = LocalTime.parse(dto.endTime());
      final TimeRange timeRange = TimeRange.of(startTime, endTime);
      final Money price = Money.of(dto.price());

      final TimeRangePrice timeRangePrice = new TimeRangePrice(dayOfWeek, timeRange, price);
      result.add(timeRangePrice);
    }

    return result;
  }
}
