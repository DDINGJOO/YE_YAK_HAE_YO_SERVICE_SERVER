package com.teambind.springproject.adapter.in.web.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.springproject.application.dto.request.SetRoomAllowedProductsRequest;
import com.teambind.springproject.application.dto.response.RoomAllowedProductsResponse;
import com.teambind.springproject.application.port.in.DeleteRoomAllowedProductsUseCase;
import com.teambind.springproject.application.port.in.GetRoomAllowedProductsUseCase;
import com.teambind.springproject.application.port.in.SetRoomAllowedProductsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomAllowedProductController.class)
@DisplayName("RoomAllowedProductController 통합 테스트")
class RoomAllowedProductControllerTest {
	
	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@MockBean
	private SetRoomAllowedProductsUseCase setRoomAllowedProductsUseCase;
	
	@MockBean
	private GetRoomAllowedProductsUseCase getRoomAllowedProductsUseCase;
	
	@MockBean
	private DeleteRoomAllowedProductsUseCase deleteRoomAllowedProductsUseCase;
	
	@Nested
	@DisplayName("POST /api/admin/rooms/{roomId}/allowed-products - 허용 상품 설정")
	class SetAllowedProductsTests {
		
		@Test
		@DisplayName("룸 허용 상품 설정에 성공한다")
		void setAllowedProductsSuccess() throws Exception {
			// given
			final Long roomId = 1L;
			final List<Long> productIds = Arrays.asList(10L, 20L, 30L);
			final SetRoomAllowedProductsRequest request = new SetRoomAllowedProductsRequest(productIds);
			
			final RoomAllowedProductsResponse response = new RoomAllowedProductsResponse(
					roomId,
					productIds
			);
			
			when(setRoomAllowedProductsUseCase.setAllowedProducts(eq(roomId), any(
					SetRoomAllowedProductsRequest.class)))
					.thenReturn(response);
			
			// when & then
			mockMvc.perform(post("/api/v1/admin/rooms/{roomId}/allowed-products", roomId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.roomId").value(roomId))
					.andExpect(jsonPath("$.allowedProductIds").isArray())
					.andExpect(jsonPath("$.allowedProductIds.length()").value(3))
					.andExpect(jsonPath("$.allowedProductIds[0]").value(10))
					.andExpect(jsonPath("$.allowedProductIds[1]").value(20))
					.andExpect(jsonPath("$.allowedProductIds[2]").value(30));
		}
		
		@Test
		@DisplayName("빈 리스트로 설정하면 모든 매핑이 삭제된다")
		void setEmptyListRemovesAllMappings() throws Exception {
			// given
			final Long roomId = 1L;
			final List<Long> emptyList = Collections.emptyList();
			final SetRoomAllowedProductsRequest request = new SetRoomAllowedProductsRequest(emptyList);
			
			final RoomAllowedProductsResponse response = new RoomAllowedProductsResponse(
					roomId,
					emptyList
			);
			
			when(setRoomAllowedProductsUseCase.setAllowedProducts(eq(roomId), any(
					SetRoomAllowedProductsRequest.class)))
					.thenReturn(response);
			
			// when & then
			mockMvc.perform(post("/api/v1/admin/rooms/{roomId}/allowed-products", roomId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.roomId").value(roomId))
					.andExpect(jsonPath("$.allowedProductIds").isArray())
					.andExpect(jsonPath("$.allowedProductIds").isEmpty());
		}
		
		@Test
		@DisplayName("유효하지 않은 roomId로 요청 시 400 에러")
		void invalidRoomIdReturns400() throws Exception {
			// given
			final Long invalidRoomId = -1L;
			final List<Long> productIds = Arrays.asList(10L, 20L);
			final SetRoomAllowedProductsRequest request = new SetRoomAllowedProductsRequest(productIds);
			
			// when & then
			mockMvc.perform(post("/api/v1/admin/rooms/{roomId}/allowed-products", invalidRoomId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest());
		}
	}
	
	@Nested
	@DisplayName("GET /api/admin/rooms/{roomId}/allowed-products - 허용 상품 조회")
	class GetAllowedProductsTests {
		
		@Test
		@DisplayName("룸 허용 상품 조회에 성공한다")
		void getAllowedProductsSuccess() throws Exception {
			// given
			final Long roomId = 1L;
			final List<Long> productIds = Arrays.asList(10L, 20L, 30L);
			final RoomAllowedProductsResponse response = new RoomAllowedProductsResponse(
					roomId,
					productIds
			);
			
			when(getRoomAllowedProductsUseCase.getAllowedProducts(roomId))
					.thenReturn(response);
			
			// when & then
			mockMvc.perform(get("/api/v1/admin/rooms/{roomId}/allowed-products", roomId))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.roomId").value(roomId))
					.andExpect(jsonPath("$.allowedProductIds").isArray())
					.andExpect(jsonPath("$.allowedProductIds.length()").value(3))
					.andExpect(jsonPath("$.allowedProductIds[0]").value(10))
					.andExpect(jsonPath("$.allowedProductIds[1]").value(20))
					.andExpect(jsonPath("$.allowedProductIds[2]").value(30));
		}
		
		@Test
		@DisplayName("매핑이 없으면 빈 리스트를 반환한다")
		void returnsEmptyListWhenNoMappings() throws Exception {
			// given
			final Long roomId = 999L;
			final RoomAllowedProductsResponse response = new RoomAllowedProductsResponse(
					roomId,
					Collections.emptyList()
			);
			
			when(getRoomAllowedProductsUseCase.getAllowedProducts(roomId))
					.thenReturn(response);
			
			// when & then
			mockMvc.perform(get("/api/v1/admin/rooms/{roomId}/allowed-products", roomId))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.roomId").value(roomId))
					.andExpect(jsonPath("$.allowedProductIds").isArray())
					.andExpect(jsonPath("$.allowedProductIds").isEmpty());
		}
	}
	
	@Nested
	@DisplayName("DELETE /api/admin/rooms/{roomId}/allowed-products - 허용 상품 삭제")
	class DeleteAllowedProductsTests {
		
		@Test
		@DisplayName("룸 허용 상품 삭제에 성공한다")
		void deleteAllowedProductsSuccess() throws Exception {
			// given
			final Long roomId = 1L;
			doNothing().when(deleteRoomAllowedProductsUseCase).deleteAllowedProducts(roomId);
			
			// when & then
			mockMvc.perform(delete("/api/v1/admin/rooms/{roomId}/allowed-products", roomId))
					.andExpect(status().isNoContent());
		}
		
		@Test
		@DisplayName("유효하지 않은 roomId로 요청 시 400 에러")
		void invalidRoomIdReturns400() throws Exception {
			// given
			final Long invalidRoomId = 0L;
			
			// when & then
			mockMvc.perform(delete("/api/v1/admin/rooms/{roomId}/allowed-products", invalidRoomId))
					.andExpect(status().isBadRequest());
		}
	}
	
	@Nested
	@DisplayName("비즈니스 시나리오 테스트")
	class BusinessScenarioTests {
		
		@Test
		@DisplayName("어드민이 룸에 특정 상품만 허용하도록 설정")
		void adminAllowsSpecificProductsForRoom() throws Exception {
			// given
			final Long roomId = 5L;
			final List<Long> drinkAndSnackIds = Arrays.asList(100L, 200L);
			final SetRoomAllowedProductsRequest request = new SetRoomAllowedProductsRequest(
					drinkAndSnackIds);
			
			final RoomAllowedProductsResponse response = new RoomAllowedProductsResponse(
					roomId,
					drinkAndSnackIds
			);
			
			when(setRoomAllowedProductsUseCase.setAllowedProducts(eq(roomId), any(
					SetRoomAllowedProductsRequest.class)))
					.thenReturn(response);
			
			// when & then
			mockMvc.perform(post("/api/v1/admin/rooms/{roomId}/allowed-products", roomId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.roomId").value(roomId))
					.andExpect(jsonPath("$.allowedProductIds.length()").value(2));
		}
		
		@Test
		@DisplayName("어드민이 허용 상품 목록을 업데이트")
		void adminUpdatesAllowedProducts() throws Exception {
			// given: 기존 [10, 20] -> 새로운 [10, 30, 40]
			final Long roomId = 1L;
			final List<Long> updatedProductIds = Arrays.asList(10L, 30L, 40L);
			final SetRoomAllowedProductsRequest request = new SetRoomAllowedProductsRequest(
					updatedProductIds);
			
			final RoomAllowedProductsResponse response = new RoomAllowedProductsResponse(
					roomId,
					updatedProductIds
			);
			
			when(setRoomAllowedProductsUseCase.setAllowedProducts(eq(roomId), any(
					SetRoomAllowedProductsRequest.class)))
					.thenReturn(response);
			
			// when & then
			mockMvc.perform(post("/api/v1/admin/rooms/{roomId}/allowed-products", roomId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.allowedProductIds.length()").value(3))
					.andExpect(jsonPath("$.allowedProductIds[0]").value(10))
					.andExpect(jsonPath("$.allowedProductIds[1]").value(30))
					.andExpect(jsonPath("$.allowedProductIds[2]").value(40));
		}
	}
}
