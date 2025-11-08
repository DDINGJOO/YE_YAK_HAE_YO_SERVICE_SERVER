package com.teambind.springproject.adapter.in.web.pricingpolicy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.springproject.application.dto.request.CopyPricingPolicyRequest;
import com.teambind.springproject.application.dto.request.TimeRangePriceDto;
import com.teambind.springproject.application.dto.request.UpdateDefaultPriceRequest;
import com.teambind.springproject.application.dto.request.UpdateTimeRangePricesRequest;
import com.teambind.springproject.application.port.in.CopyPricingPolicyUseCase;
import com.teambind.springproject.application.port.in.GetPricingPolicyUseCase;
import com.teambind.springproject.application.port.in.UpdatePricingPolicyUseCase;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrice;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrices;
import com.teambind.springproject.domain.pricingpolicy.exception.CannotCopyDifferentPlaceException;
import com.teambind.springproject.domain.pricingpolicy.exception.PricingPolicyNotFoundException;
import com.teambind.springproject.domain.shared.DayOfWeek;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeRange;
import com.teambind.springproject.domain.shared.TimeSlot;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PricingPolicyController.class)
@DisplayName("PricingPolicyController 통합 테스트")
class PricingPolicyControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private GetPricingPolicyUseCase getPricingPolicyUseCase;

  @MockBean
  private UpdatePricingPolicyUseCase updatePricingPolicyUseCase;

  @MockBean
  private CopyPricingPolicyUseCase copyPricingPolicyUseCase;

  @Nested
  @DisplayName("GET /api/pricing-policies/{roomId}")
  class GetPricingPolicyTests {

    @Test
    @DisplayName("가격 정책 조회에 성공한다")
    void getPricingPolicySuccess() throws Exception {
      // given
      final Long roomId = 1L;
      final PricingPolicy policy = PricingPolicy.create(
          RoomId.of(roomId),
          PlaceId.of(100L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("30000"))
      );

      when(getPricingPolicyUseCase.getPolicy(any(RoomId.class))).thenReturn(policy);

      // when & then
      mockMvc.perform(get("/api/pricing-policies/{roomId}", roomId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.roomId").value(roomId))
          .andExpect(jsonPath("$.placeId").value(100))
          .andExpect(jsonPath("$.timeSlot").value("HOUR"))
          .andExpect(jsonPath("$.defaultPrice").value(30000));
    }

    @Test
    @DisplayName("존재하지 않는 정책 조회 시 404를 반환한다")
    void getPricingPolicyNotFound() throws Exception {
      // given
      final Long roomId = 999L;

      when(getPricingPolicyUseCase.getPolicy(any(RoomId.class)))
          .thenThrow(new PricingPolicyNotFoundException("Pricing policy not found"));

      // when & then
      mockMvc.perform(get("/api/pricing-policies/{roomId}", roomId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("PRICING_001"));
    }

    @Test
    @DisplayName("잘못된 roomId 형식 시 400을 반환한다")
    void getPricingPolicyInvalidRoomId() throws Exception {
      // when & then
      mockMvc.perform(get("/api/pricing-policies/{roomId}", "invalid"))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("PUT /api/pricing-policies/{roomId}/default-price")
  class UpdateDefaultPriceTests {

    @Test
    @DisplayName("기본 가격 업데이트에 성공한다")
    void updateDefaultPriceSuccess() throws Exception {
      // given
      final Long roomId = 1L;
      final UpdateDefaultPriceRequest request = new UpdateDefaultPriceRequest(
          new BigDecimal("50000"));

      final PricingPolicy updatedPolicy = PricingPolicy.create(
          RoomId.of(roomId),
          PlaceId.of(100L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("50000"))
      );

      when(updatePricingPolicyUseCase.updateDefaultPrice(any(RoomId.class), any(Money.class)))
          .thenReturn(updatedPolicy);

      // when & then
      mockMvc.perform(put("/api/pricing-policies/{roomId}/default-price", roomId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.defaultPrice").value(50000));
    }

    @Test
    @DisplayName("음수 가격 입력 시 400을 반환한다")
    void updateDefaultPriceNegative() throws Exception {
      // given
      final Long roomId = 1L;
      final UpdateDefaultPriceRequest request = new UpdateDefaultPriceRequest(
          new BigDecimal("-1000"));

      // when & then
      mockMvc.perform(put("/api/pricing-policies/{roomId}/default-price", roomId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("PUT /api/pricing-policies/{roomId}/time-range-prices")
  class UpdateTimeRangePricesTests {

    @Test
    @DisplayName("시간대별 가격 업데이트에 성공한다")
    void updateTimeRangePricesSuccess() throws Exception {
      // given
      final Long roomId = 1L;
      final List<TimeRangePriceDto> timeRangePriceDtos = List.of(
          new TimeRangePriceDto("MONDAY", "09:00", "18:00", new BigDecimal("50000"))
      );
      final UpdateTimeRangePricesRequest request = new UpdateTimeRangePricesRequest(
          timeRangePriceDtos);

      final List<TimeRangePrice> timeRangePrices = List.of(
          new TimeRangePrice(
              DayOfWeek.MONDAY,
              TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
              Money.of(new BigDecimal("50000"))
          )
      );

      final PricingPolicy updatedPolicy = PricingPolicy.createWithTimeRangePrices(
          RoomId.of(roomId),
          PlaceId.of(100L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("30000")),
          TimeRangePrices.of(timeRangePrices)
      );

      when(updatePricingPolicyUseCase.updateTimeRangePrices(any(RoomId.class), any(List.class)))
          .thenReturn(updatedPolicy);

      // when & then
      mockMvc.perform(put("/api/pricing-policies/{roomId}/time-range-prices", roomId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.timeRangePrices").isArray())
          .andExpect(jsonPath("$.timeRangePrices[0].dayOfWeek").value("MONDAY"))
          .andExpect(jsonPath("$.timeRangePrices[0].price").value(50000));
    }
  }

  @Nested
  @DisplayName("POST /api/pricing-policies/{targetRoomId}/copy")
  class CopyPricingPolicyTests {

    @Test
    @DisplayName("가격 정책 복사에 성공한다")
    void copyPricingPolicySuccess() throws Exception {
      // given
      final Long targetRoomId = 2L;
      final CopyPricingPolicyRequest request = new CopyPricingPolicyRequest(1L);

      final PricingPolicy copiedPolicy = PricingPolicy.create(
          RoomId.of(targetRoomId),
          PlaceId.of(100L),
          TimeSlot.HOUR,
          Money.of(new BigDecimal("30000"))
      );

      when(copyPricingPolicyUseCase.copyFromRoom(any(RoomId.class), any(RoomId.class)))
          .thenReturn(copiedPolicy);

      // when & then
      mockMvc.perform(post("/api/pricing-policies/{targetRoomId}/copy", targetRoomId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.roomId").value(targetRoomId))
          .andExpect(jsonPath("$.defaultPrice").value(30000));
    }

    @Test
    @DisplayName("다른 Place 간 복사 시도 시 400을 반환한다")
    void copyPricingPolicyDifferentPlace() throws Exception {
      // given
      final Long targetRoomId = 2L;
      final CopyPricingPolicyRequest request = new CopyPricingPolicyRequest(1L);

      when(copyPricingPolicyUseCase.copyFromRoom(any(RoomId.class), any(RoomId.class)))
          .thenThrow(new CannotCopyDifferentPlaceException(
              "Cannot copy pricing policy between different places"));

      // when & then
      mockMvc.perform(post("/api/pricing-policies/{targetRoomId}/copy", targetRoomId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("PRICING_003"));
    }

    @Test
    @DisplayName("null sourceRoomId 입력 시 400을 반환한다")
    void copyPricingPolicyNullSourceRoomId() throws Exception {
      // given
      final Long targetRoomId = 2L;
      final String invalidRequest = "{\"sourceRoomId\": null}";

      // when & then
      mockMvc.perform(post("/api/pricing-policies/{targetRoomId}/copy", targetRoomId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(invalidRequest))
          .andExpect(status().isBadRequest());
    }
  }
}
