package com.teambind.springproject.adapter.in.web.reservationpricing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.request.ProductRequest;
import com.teambind.springproject.application.dto.request.UpdateProductsRequest;
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;
import com.teambind.springproject.application.port.in.CalculateReservationPriceUseCase;
import com.teambind.springproject.application.port.in.CreateReservationUseCase;
import com.teambind.springproject.application.port.in.UpdateReservationProductsUseCase;
import com.teambind.springproject.domain.reservationpricing.exception.InvalidReservationStatusException;
import com.teambind.springproject.domain.reservationpricing.exception.ProductNotAvailableException;
import com.teambind.springproject.domain.reservationpricing.exception.ReservationPricingNotFoundException;
import com.teambind.springproject.domain.shared.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReservationPricingController.class)
@DisplayName("ReservationPricingController 통합 테스트")
class ReservationPricingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private CreateReservationUseCase createReservationUseCase;

  @MockBean
  private CalculateReservationPriceUseCase calculateReservationPriceUseCase;

  @MockBean
  private UpdateReservationProductsUseCase updateReservationProductsUseCase;

  @Nested
  @DisplayName("POST /api/v1/reservations - 예약 생성")
  class CreateReservationTests {

    @Test
    @DisplayName("예약 생성에 성공한다")
    void createReservationSuccess() throws Exception {
      // given
      final LocalDateTime slot1 = LocalDateTime.of(2025, 1, 15, 10, 0);
      final LocalDateTime slot2 = LocalDateTime.of(2025, 1, 15, 11, 0);
      final ProductRequest productRequest = new ProductRequest(1L, 2);

      final CreateReservationRequest request = new CreateReservationRequest(
          1L,
          List.of(slot1, slot2),
          List.of(productRequest)
      );

      final ReservationPricingResponse response = new ReservationPricingResponse(
          1L,
          1L,
          ReservationStatus.PENDING,
          BigDecimal.valueOf(25000),
          LocalDateTime.now()
      );

      when(createReservationUseCase.createReservation(any(CreateReservationRequest.class)))
          .thenReturn(response);

      // when & then
      mockMvc.perform(post("/api/v1/reservations")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.reservationId").value(1))
          .andExpect(jsonPath("$.roomId").value(1))
          .andExpect(jsonPath("$.status").value("PENDING"))
          .andExpect(jsonPath("$.totalPrice").value(25000));
    }

    @Test
    @DisplayName("재고 부족 시 400 Bad Request를 반환한다")
    void createReservationFailsWhenProductNotAvailable() throws Exception {
      // given
      final LocalDateTime slot1 = LocalDateTime.of(2025, 1, 15, 10, 0);
      final ProductRequest productRequest = new ProductRequest(1L, 100);

      final CreateReservationRequest request = new CreateReservationRequest(
          1L,
          List.of(slot1),
          List.of(productRequest)
      );

      when(createReservationUseCase.createReservation(any(CreateReservationRequest.class)))
          .thenThrow(new ProductNotAvailableException(1L, 100));

      // when & then
      mockMvc.perform(post("/api/v1/reservations")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("RESERVATION_002"))
          .andExpect(jsonPath("$.exceptionType").value("ProductNotAvailableException"));
    }

    @Test
    @DisplayName("유효하지 않은 요청 시 400 Bad Request를 반환한다")
    void createReservationFailsWhenInvalidRequest() throws Exception {
      // given - products가 빈 리스트
      final LocalDateTime slot1 = LocalDateTime.of(2025, 1, 15, 10, 0);

      final CreateReservationRequest request = new CreateReservationRequest(
          1L,
          List.of(slot1),
          List.of()  // 빈 리스트 - validation 실패
      );

      // when & then
      mockMvc.perform(post("/api/v1/reservations")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("PUT /api/reservations/pricing/{reservationId}/confirm - 예약 확정")
  class ConfirmReservationTests {

    @Test
    @DisplayName("예약 확정에 성공한다")
    void confirmReservationSuccess() throws Exception {
      // given
      final Long reservationId = 1L;
      final ReservationPricingResponse response = new ReservationPricingResponse(
          reservationId,
          1L,
          ReservationStatus.CONFIRMED,
          BigDecimal.valueOf(25000),
          LocalDateTime.now()
      );

      when(createReservationUseCase.confirmReservation(eq(reservationId)))
          .thenReturn(response);

      // when & then
      mockMvc.perform(put("/api/v1/reservations/{reservationId}/confirm", reservationId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.reservationId").value(1))
          .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("존재하지 않는 예약 확정 시 404 Not Found를 반환한다")
    void confirmReservationFailsWhenNotFound() throws Exception {
      // given
      final Long reservationId = 999L;

      when(createReservationUseCase.confirmReservation(eq(reservationId)))
          .thenThrow(new ReservationPricingNotFoundException(reservationId));

      // when & then
      mockMvc.perform(put("/api/v1/reservations/{reservationId}/confirm", reservationId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("RESERVATION_001"))
          .andExpect(jsonPath("$.exceptionType").value("ReservationPricingNotFoundException"));
    }
  }

  @Nested
  @DisplayName("PUT /api/reservations/{reservationId}/cancel - 예약 취소")
  class CancelReservationTests {

    @Test
    @DisplayName("예약 취소에 성공한다")
    void cancelReservationSuccess() throws Exception {
      // given
      final Long reservationId = 1L;
      final ReservationPricingResponse response = new ReservationPricingResponse(
          reservationId,
          1L,
          ReservationStatus.CANCELLED,
          BigDecimal.valueOf(25000),
          LocalDateTime.now()
      );

      when(createReservationUseCase.cancelReservation(eq(reservationId)))
          .thenReturn(response);

      // when & then
      mockMvc.perform(put("/api/v1/reservations/{reservationId}/cancel", reservationId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.reservationId").value(1))
          .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("존재하지 않는 예약 취소 시 404 Not Found를 반환한다")
    void cancelReservationFailsWhenNotFound() throws Exception {
      // given
      final Long reservationId = 999L;

      when(createReservationUseCase.cancelReservation(eq(reservationId)))
          .thenThrow(new ReservationPricingNotFoundException(reservationId));

      // when & then
      mockMvc.perform(put("/api/v1/reservations/{reservationId}/cancel", reservationId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("RESERVATION_001"))
          .andExpect(jsonPath("$.exceptionType").value("ReservationPricingNotFoundException"));
    }
  }

  @Nested
  @DisplayName("PUT /api/reservations/{reservationId}/products - 예약 상품 업데이트")
  class UpdateProductsTests {

    @Test
    @DisplayName("PENDING 상태에서 상품 업데이트에 성공한다")
    void updateProductsSuccess() throws Exception {
      // given
      final Long reservationId = 1L;
      final ProductRequest productRequest1 = new ProductRequest(1L, 3);
      final ProductRequest productRequest2 = new ProductRequest(2L, 1);

      final UpdateProductsRequest request = new UpdateProductsRequest(
          List.of(productRequest1, productRequest2)
      );

      final ReservationPricingResponse response = new ReservationPricingResponse(
          reservationId,
          1L,
          ReservationStatus.PENDING,
          BigDecimal.valueOf(30000),
          LocalDateTime.now()
      );

      when(updateReservationProductsUseCase.updateProducts(eq(reservationId),
          any(UpdateProductsRequest.class)))
          .thenReturn(response);

      // when & then
      mockMvc.perform(put("/api/v1/reservations/{reservationId}/products", reservationId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.reservationId").value(1))
          .andExpect(jsonPath("$.status").value("PENDING"))
          .andExpect(jsonPath("$.totalPrice").value(30000));
    }

    @Test
    @DisplayName("CONFIRMED 상태에서 상품 업데이트 시 400 Bad Request를 반환한다")
    void updateProductsFailsWhenConfirmed() throws Exception {
      // given
      final Long reservationId = 1L;
      final ProductRequest productRequest = new ProductRequest(1L, 2);

      final UpdateProductsRequest request = new UpdateProductsRequest(
          List.of(productRequest)
      );

      when(updateReservationProductsUseCase.updateProducts(eq(reservationId),
          any(UpdateProductsRequest.class)))
          .thenThrow(new InvalidReservationStatusException(ReservationStatus.CONFIRMED, "updateProducts"));

      // when & then
      mockMvc.perform(put("/api/v1/reservations/{reservationId}/products", reservationId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("RESERVATION_003"))
          .andExpect(jsonPath("$.exceptionType").value("InvalidReservationStatusException"));
    }

    @Test
    @DisplayName("존재하지 않는 예약 업데이트 시 404 Not Found를 반환한다")
    void updateProductsFailsWhenNotFound() throws Exception {
      // given
      final Long reservationId = 999L;
      final ProductRequest productRequest = new ProductRequest(1L, 2);

      final UpdateProductsRequest request = new UpdateProductsRequest(
          List.of(productRequest)
      );

      when(updateReservationProductsUseCase.updateProducts(eq(reservationId),
          any(UpdateProductsRequest.class)))
          .thenThrow(new ReservationPricingNotFoundException(reservationId));

      // when & then
      mockMvc.perform(put("/api/v1/reservations/{reservationId}/products", reservationId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("RESERVATION_001"))
          .andExpect(jsonPath("$.exceptionType").value("ReservationPricingNotFoundException"));
    }

    @Test
    @DisplayName("재고 부족 시 400 Bad Request를 반환한다")
    void updateProductsFailsWhenProductNotAvailable() throws Exception {
      // given
      final Long reservationId = 1L;
      final ProductRequest productRequest = new ProductRequest(1L, 100);

      final UpdateProductsRequest request = new UpdateProductsRequest(
          List.of(productRequest)
      );

      when(updateReservationProductsUseCase.updateProducts(eq(reservationId),
          any(UpdateProductsRequest.class)))
          .thenThrow(new ProductNotAvailableException(1L, 100));

      // when & then
      mockMvc.perform(put("/api/v1/reservations/{reservationId}/products", reservationId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("RESERVATION_002"))
          .andExpect(jsonPath("$.exceptionType").value("ProductNotAvailableException"));
    }

    @Test
    @DisplayName("유효하지 않은 reservationId 시 400 Bad Request를 반환한다")
    void updateProductsFailsWhenInvalidReservationId() throws Exception {
      // given
      final Long invalidReservationId = -1L;
      final ProductRequest productRequest = new ProductRequest(1L, 2);

      final UpdateProductsRequest request = new UpdateProductsRequest(
          List.of(productRequest)
      );

      // when & then
      mockMvc.perform(put("/api/v1/reservations/{reservationId}/products", invalidReservationId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }
}
