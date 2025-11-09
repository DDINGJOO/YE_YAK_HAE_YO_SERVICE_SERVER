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
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;
import com.teambind.springproject.application.port.in.CalculateReservationPriceUseCase;
import com.teambind.springproject.application.port.in.CreateReservationUseCase;
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

  @Nested
  @DisplayName("POST /api/reservations/pricing - 예약 생성")
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
      mockMvc.perform(post("/api/reservations/pricing")
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
      mockMvc.perform(post("/api/reservations/pricing")
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
      mockMvc.perform(post("/api/reservations/pricing")
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
      mockMvc.perform(put("/api/reservations/pricing/{reservationId}/confirm", reservationId))
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
      mockMvc.perform(put("/api/reservations/pricing/{reservationId}/confirm", reservationId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("RESERVATION_001"))
          .andExpect(jsonPath("$.exceptionType").value("ReservationPricingNotFoundException"));
    }
  }

  @Nested
  @DisplayName("PUT /api/reservations/pricing/{reservationId}/cancel - 예약 취소")
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
      mockMvc.perform(put("/api/reservations/pricing/{reservationId}/cancel", reservationId))
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
      mockMvc.perform(put("/api/reservations/pricing/{reservationId}/cancel", reservationId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("RESERVATION_001"))
          .andExpect(jsonPath("$.exceptionType").value("ReservationPricingNotFoundException"));
    }
  }
}
