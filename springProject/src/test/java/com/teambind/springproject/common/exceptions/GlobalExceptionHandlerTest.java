package com.teambind.springproject.common.exceptions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teambind.springproject.adapter.in.web.pricingpolicy.PricingPolicyController;
import com.teambind.springproject.application.port.in.CopyPricingPolicyUseCase;
import com.teambind.springproject.application.port.in.GetPricingPolicyUseCase;
import com.teambind.springproject.application.port.in.UpdatePricingPolicyUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PricingPolicyController.class)
@DisplayName("GlobalExceptionHandler 통합 테스트")
class GlobalExceptionHandlerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private GetPricingPolicyUseCase getPricingPolicyUseCase;

  @MockBean
  private UpdatePricingPolicyUseCase updatePricingPolicyUseCase;

  @MockBean
  private CopyPricingPolicyUseCase copyPricingPolicyUseCase;

  @Nested
  @DisplayName("IllegalArgumentException 처리")
  class IllegalArgumentExceptionHandling {

    @Test
    @DisplayName("IllegalArgumentException 발생 시 400 Bad Request 응답")
    void shouldReturn400WhenIllegalArgumentException() throws Exception {
      // given
      when(getPricingPolicyUseCase.getPolicy(any()))
          .thenThrow(new IllegalArgumentException("Room ID cannot be null"));

      // when & then
      mockMvc.perform(get("/api/pricing-policies/1"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
          .andExpect(jsonPath("$.message").value("Room ID cannot be null"))
          .andExpect(jsonPath("$.path").value("/api/pricing-policies/1"))
          .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Domain validation 실패 시 적절한 에러 메시지 반환")
    void shouldReturnProperErrorMessageForDomainValidationFailure() throws Exception {
      // given
      String errorMessage = "Default price cannot be null";
      when(getPricingPolicyUseCase.getPolicy(any()))
          .thenThrow(new IllegalArgumentException(errorMessage));

      // when & then
      mockMvc.perform(get("/api/pricing-policies/123"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
          .andExpect(jsonPath("$.message").value(errorMessage));
    }
  }

  @Nested
  @DisplayName("IllegalStateException 처리")
  class IllegalStateExceptionHandling {

    @Test
    @DisplayName("IllegalStateException 발생 시 409 Conflict 응답")
    void shouldReturn409WhenIllegalStateException() throws Exception {
      // given
      when(getPricingPolicyUseCase.getPolicy(any()))
          .thenThrow(new IllegalStateException("Cannot modify confirmed pricing policy"));

      // when & then
      mockMvc.perform(get("/api/pricing-policies/1"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.status").value(409))
          .andExpect(jsonPath("$.code").value("INVALID_STATE"))
          .andExpect(jsonPath("$.message").value("Cannot modify confirmed pricing policy"))
          .andExpect(jsonPath("$.path").value("/api/pricing-policies/1"))
          .andExpect(jsonPath("$.timestamp").exists());
    }
  }

  @Nested
  @DisplayName("ErrorResponse 구조 검증")
  class ErrorResponseStructure {

    @Test
    @DisplayName("ErrorResponse는 표준 필드를 모두 포함해야 함")
    void errorResponseShouldContainAllStandardFields() throws Exception {
      // given
      when(getPricingPolicyUseCase.getPolicy(any()))
          .thenThrow(new IllegalArgumentException("Test error"));

      // when & then
      mockMvc.perform(get("/api/pricing-policies/999"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.status").exists())
          .andExpect(jsonPath("$.code").exists())
          .andExpect(jsonPath("$.message").exists())
          .andExpect(jsonPath("$.path").exists());
    }

    @Test
    @DisplayName("ErrorResponse는 fieldErrors를 포함하지 않음 (IllegalArgumentException)")
    void errorResponseShouldNotContainFieldErrorsForIllegalArgument() throws Exception {
      // given
      when(getPricingPolicyUseCase.getPolicy(any()))
          .thenThrow(new IllegalArgumentException("Invalid input"));

      // when & then
      mockMvc.perform(get("/api/pricing-policies/1"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }
  }
}