package com.teambind.springproject.adapter.in.web.room;

import com.teambind.springproject.application.dto.response.ProductResponse;
import com.teambind.springproject.application.port.in.GetAvailableProductsForRoomUseCase;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 룸별 상품 조회 REST Controller.
 *
 * Room 리소스 중심의 RESTful API를 제공합니다.
 * 일반 사용자가 특정 룸에서 예약 가능한 상품 목록을 조회하는 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/rooms/{roomId}/available-products")
@Validated
public class RoomProductController {

	private final GetAvailableProductsForRoomUseCase getAvailableProductsForRoomUseCase;

	public RoomProductController(
			final GetAvailableProductsForRoomUseCase getAvailableProductsForRoomUseCase) {
		this.getAvailableProductsForRoomUseCase = getAvailableProductsForRoomUseCase;
	}

	/**
	 * 특정 룸에서 이용 가능한 상품 목록을 조회합니다.
	 *
	 * 조회되는 상품:
	 * - ROOM Scope: 해당 roomId를 가진 상품
	 * - PLACE Scope: RoomAllowedProduct에 허용된 상품
	 * - RESERVATION Scope: 모든 룸에서 사용 가능한 상품
	 *
	 * @param roomId 룸 ID
	 * @param placeId 플레이스 ID
	 * @return 이용 가능한 상품 목록
	 */
	@GetMapping
	public ResponseEntity<List<ProductResponse>> getAvailableProducts(
			@PathVariable @Positive(message = "Room ID must be positive") final Long roomId,
			@RequestParam @Positive(message = "Place ID must be positive") final Long placeId) {

		final List<ProductResponse> responses = getAvailableProductsForRoomUseCase
				.getAvailableProducts(RoomId.of(roomId), PlaceId.of(placeId));

		return ResponseEntity.ok(responses);
	}
}