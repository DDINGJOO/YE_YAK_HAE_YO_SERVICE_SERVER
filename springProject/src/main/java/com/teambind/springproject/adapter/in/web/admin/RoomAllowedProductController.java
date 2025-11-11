package com.teambind.springproject.adapter.in.web.admin;

import com.teambind.springproject.application.dto.request.SetRoomAllowedProductsRequest;
import com.teambind.springproject.application.dto.response.RoomAllowedProductsResponse;
import com.teambind.springproject.application.port.in.DeleteRoomAllowedProductsUseCase;
import com.teambind.springproject.application.port.in.GetRoomAllowedProductsUseCase;
import com.teambind.springproject.application.port.in.SetRoomAllowedProductsUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 룸별 허용 상품 관리 Admin REST Controller.
 */
@RestController
@RequestMapping("/api/v1/admin/rooms/{roomId}/allowed-products")
@Validated
public class RoomAllowedProductController {
	
	private final SetRoomAllowedProductsUseCase setRoomAllowedProductsUseCase;
	private final GetRoomAllowedProductsUseCase getRoomAllowedProductsUseCase;
	private final DeleteRoomAllowedProductsUseCase deleteRoomAllowedProductsUseCase;
	
	public RoomAllowedProductController(
			final SetRoomAllowedProductsUseCase setRoomAllowedProductsUseCase,
			final GetRoomAllowedProductsUseCase getRoomAllowedProductsUseCase,
			final DeleteRoomAllowedProductsUseCase deleteRoomAllowedProductsUseCase) {
		this.setRoomAllowedProductsUseCase = setRoomAllowedProductsUseCase;
		this.getRoomAllowedProductsUseCase = getRoomAllowedProductsUseCase;
		this.deleteRoomAllowedProductsUseCase = deleteRoomAllowedProductsUseCase;
	}
	
	/**
	 * 특정 룸의 허용 상품 목록을 설정합니다.
	 * 기존 매핑은 모두 삭제되고 새로운 매핑이 저장됩니다.
	 *
	 * @param roomId  룸 ID
	 * @param request 허용 상품 설정 요청
	 * @return 설정된 허용 상품 정보
	 */
	@PostMapping
	public ResponseEntity<RoomAllowedProductsResponse> setAllowedProducts(
			@PathVariable @Positive final Long roomId,
			@RequestBody @Valid final SetRoomAllowedProductsRequest request) {
		
		final RoomAllowedProductsResponse response = setRoomAllowedProductsUseCase
				.setAllowedProducts(roomId, request);
		
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	/**
	 * 특정 룸의 허용 상품 목록을 조회합니다.
	 *
	 * @param roomId 룸 ID
	 * @return 허용된 상품 정보
	 */
	@GetMapping
	public ResponseEntity<RoomAllowedProductsResponse> getAllowedProducts(
			@PathVariable @Positive final Long roomId) {
		
		final RoomAllowedProductsResponse response = getRoomAllowedProductsUseCase
				.getAllowedProducts(roomId);
		
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	/**
	 * 특정 룸의 모든 허용 상품 매핑을 삭제합니다.
	 *
	 * @param roomId 룸 ID
	 * @return HTTP 204 No Content
	 */
	@DeleteMapping
	public ResponseEntity<Void> deleteAllowedProducts(
			@PathVariable @Positive final Long roomId) {
		
		deleteRoomAllowedProductsUseCase.deleteAllowedProducts(roomId);
		
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
