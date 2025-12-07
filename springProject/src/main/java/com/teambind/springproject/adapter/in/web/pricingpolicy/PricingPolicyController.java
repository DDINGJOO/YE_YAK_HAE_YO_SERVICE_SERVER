package com.teambind.springproject.adapter.in.web.pricingpolicy;

import com.teambind.springproject.application.dto.request.CopyPricingPolicyRequest;
import com.teambind.springproject.application.dto.request.TimeRangePriceDto;
import com.teambind.springproject.application.dto.request.UpdateDefaultPriceRequest;
import com.teambind.springproject.application.dto.request.UpdateTimeRangePricesRequest;
import com.teambind.springproject.application.dto.response.DatePricingResponse;
import com.teambind.springproject.application.dto.response.PlacePricingBatchResponse;
import com.teambind.springproject.application.dto.response.PricingPolicyResponse;
import com.teambind.springproject.application.port.in.CopyPricingPolicyUseCase;
import com.teambind.springproject.application.port.in.GetDatePricingUseCase;
import com.teambind.springproject.application.port.in.GetPlacePricingBatchUseCase;
import com.teambind.springproject.application.port.in.GetPricingPolicyUseCase;
import com.teambind.springproject.application.port.in.UpdatePricingPolicyUseCase;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrice;
import com.teambind.springproject.domain.shared.DayOfWeek;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeRange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 가격 정책 관리 REST Controller.
 */
@RestController
@RequestMapping("/api/v1/pricing-policies")
@Validated
public class PricingPolicyController {
	
	private final GetPricingPolicyUseCase getPricingPolicyUseCase;
	private final UpdatePricingPolicyUseCase updatePricingPolicyUseCase;
	private final CopyPricingPolicyUseCase copyPricingPolicyUseCase;
	private final GetDatePricingUseCase getDatePricingUseCase;
	private final GetPlacePricingBatchUseCase getPlacePricingBatchUseCase;

	public PricingPolicyController(
			final GetPricingPolicyUseCase getPricingPolicyUseCase,
			final UpdatePricingPolicyUseCase updatePricingPolicyUseCase,
			final CopyPricingPolicyUseCase copyPricingPolicyUseCase,
			final GetDatePricingUseCase getDatePricingUseCase,
			final GetPlacePricingBatchUseCase getPlacePricingBatchUseCase) {
		this.getPricingPolicyUseCase = getPricingPolicyUseCase;
		this.updatePricingPolicyUseCase = updatePricingPolicyUseCase;
		this.copyPricingPolicyUseCase = copyPricingPolicyUseCase;
		this.getDatePricingUseCase = getDatePricingUseCase;
		this.getPlacePricingBatchUseCase = getPlacePricingBatchUseCase;
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
	 * 특정 날짜의 시간대별 가격 조회.
	 * 시작 시간(예: "11:00")을 키로, 해당 타임슬롯의 가격을 값으로 가지는 Map을 반환합니다.
	 *
	 * @param roomId 룸 ID
	 * @param date   조회할 날짜 (yyyy-MM-dd)
	 * @return 시간대별 가격
	 */
	@GetMapping("/{roomId}/date/{date}")
	public ResponseEntity<DatePricingResponse> getPricingByDate(
			@PathVariable @Positive(message = "Room ID must be positive") final Long roomId,
			@PathVariable final LocalDate date) {

		final Map<String, BigDecimal> timeSlotPrices = getDatePricingUseCase.getPricingByDate(
				RoomId.of(roomId),
				date
		);

		final DatePricingResponse response = DatePricingResponse.of(timeSlotPrices);

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

	/**
	 * PlaceId 기반 배치 조회.
	 * 특정 Place에 속한 모든 Room의 가격 정책을 한 번에 조회합니다.
	 * date 파라미터를 제공하면 해당 날짜의 시간대별 가격도 함께 조회합니다.
	 *
	 * @param placeId Place ID
	 * @param date    조회할 날짜 (선택적, yyyy-MM-dd)
	 * @return PlaceId에 속한 모든 Room의 가격 정보
	 */
	@GetMapping("/places/{placeId}/batch")
	public ResponseEntity<PlacePricingBatchResponse> getPricingByPlace(
			@PathVariable @Positive(message = "Place ID must be positive") final Long placeId,
			@RequestParam(required = false) final LocalDate date) {

		final PlaceId place = PlaceId.of(placeId);
		final Optional<LocalDate> optionalDate = Optional.ofNullable(date);

		// Service 호출하여 정책 리스트 조회
		final List<PricingPolicy> policies = getPlacePricingBatchUseCase.getPricingByPlace(
				place,
				optionalDate
		);

		// 응답 생성
		final PlacePricingBatchResponse response;
		if (policies.isEmpty()) {
			// Room이 없는 경우 빈 응답 반환 (200 OK with empty list)
			response = PlacePricingBatchResponse.empty(place);
		} else {
			// 정책이 있는 경우 날짜 포함 여부에 따라 응답 생성
			response = PlacePricingBatchResponse.from(place, policies, optionalDate);
		}

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
