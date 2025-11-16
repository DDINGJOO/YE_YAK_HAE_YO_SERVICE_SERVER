package com.teambind.springproject.adapter.in.web.room;

import com.teambind.springproject.application.dto.request.PricingStrategyDto;
import com.teambind.springproject.application.dto.response.ProductResponse;
import com.teambind.springproject.application.port.in.GetAvailableProductsForRoomUseCase;
import com.teambind.springproject.domain.product.vo.PricingType;
import com.teambind.springproject.domain.product.vo.ProductScope;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomProductController.class)
@DisplayName("RoomProductController 통합 테스트")
class RoomProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private GetAvailableProductsForRoomUseCase getAvailableProductsForRoomUseCase;

	@Nested
	@DisplayName("GET /api/v1/rooms/{roomId}/available-products - 룸별 이용 가능 상품 조회")
	class GetAvailableProductsTests {

		@Test
		@DisplayName("룸에서 이용 가능한 상품 조회에 성공한다")
		void getAvailableProductsSuccess() throws Exception {
			// Given
			final Long roomId = 200L;
			final Long placeId = 100L;

			final List<ProductResponse> responses = List.of(
					// ROOM Scope 상품
					new ProductResponse(
							1L,
							ProductScope.ROOM,
							placeId,
							roomId,
							"룸 전용 화이트보드",
							new PricingStrategyDto(PricingType.ONE_TIME, BigDecimal.valueOf(15000), null),
							3
					),
					// PLACE Scope 상품 (허용된 상품)
					new ProductResponse(
							2L,
							ProductScope.PLACE,
							placeId,
							null,
							"공용 빔 프로젝터",
							new PricingStrategyDto(PricingType.SIMPLE_STOCK, BigDecimal.valueOf(10000), null),
							5
					),
					// RESERVATION Scope 상품
					new ProductResponse(
							3L,
							ProductScope.RESERVATION,
							null,
							null,
							"음료수",
							new PricingStrategyDto(PricingType.SIMPLE_STOCK, BigDecimal.valueOf(2000), null),
							100
					)
			);

			when(getAvailableProductsForRoomUseCase.getAvailableProducts(
					any(RoomId.class),
					any(PlaceId.class)
			)).thenReturn(responses);

			// When & Then
			mockMvc.perform(get("/api/v1/rooms/{roomId}/available-products", roomId)
							.param("placeId", placeId.toString()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(3))
					.andExpect(jsonPath("$[0].productId").value(1))
					.andExpect(jsonPath("$[0].scope").value("ROOM"))
					.andExpect(jsonPath("$[0].roomId").value(roomId))
					.andExpect(jsonPath("$[0].name").value("룸 전용 화이트보드"))
					.andExpect(jsonPath("$[1].productId").value(2))
					.andExpect(jsonPath("$[1].scope").value("PLACE"))
					.andExpect(jsonPath("$[1].placeId").value(placeId))
					.andExpect(jsonPath("$[1].name").value("공용 빔 프로젝터"))
					.andExpect(jsonPath("$[2].productId").value(3))
					.andExpect(jsonPath("$[2].scope").value("RESERVATION"))
					.andExpect(jsonPath("$[2].name").value("음료수"));
		}

		@Test
		@DisplayName("빈 목록 조회에 성공한다")
		void getAvailableProductsEmptyList() throws Exception {
			// Given
			final Long roomId = 200L;
			final Long placeId = 100L;

			when(getAvailableProductsForRoomUseCase.getAvailableProducts(
					any(RoomId.class),
					any(PlaceId.class)
			)).thenReturn(List.of());

			// When & Then
			mockMvc.perform(get("/api/v1/rooms/{roomId}/available-products", roomId)
							.param("placeId", placeId.toString()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(0));
		}

		@Test
		@DisplayName("음수 roomId 요청 시 400 Bad Request를 반환한다")
		void getAvailableProductsWithNegativeRoomId() throws Exception {
			// When & Then
			mockMvc.perform(get("/api/v1/rooms/{roomId}/available-products", -1)
							.param("placeId", "100"))
					.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("음수 placeId 요청 시 400 Bad Request를 반환한다")
		void getAvailableProductsWithNegativePlaceId() throws Exception {
			// When & Then
			mockMvc.perform(get("/api/v1/rooms/{roomId}/available-products", 200)
							.param("placeId", "-1"))
					.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("placeId 누락 시 400 Bad Request를 반환한다")
		void getAvailableProductsWithoutPlaceId() throws Exception {
			// When & Then
			mockMvc.perform(get("/api/v1/rooms/{roomId}/available-products", 200))
					.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("0 값의 roomId 요청 시 400 Bad Request를 반환한다")
		void getAvailableProductsWithZeroRoomId() throws Exception {
			// When & Then
			mockMvc.perform(get("/api/v1/rooms/{roomId}/available-products", 0)
							.param("placeId", "100"))
					.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("0 값의 placeId 요청 시 400 Bad Request를 반환한다")
		void getAvailableProductsWithZeroPlaceId() throws Exception {
			// When & Then
			mockMvc.perform(get("/api/v1/rooms/{roomId}/available-products", 200)
							.param("placeId", "0"))
					.andExpect(status().isBadRequest());
		}
	}
}