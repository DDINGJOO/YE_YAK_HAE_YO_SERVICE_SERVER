package com.teambind.springproject.domain.pricingpolicy.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 가격 정책 도메인 관련 에러 코드.
 */
@Getter
public enum PricingPolicyErrorCode {
  // Pricing Policy 관련 에러 (PRICING_0XX)
  PRICING_POLICY_NOT_FOUND("PRICING_001", "Pricing policy not found", HttpStatus.NOT_FOUND),
  PRICING_POLICY_ALREADY_EXISTS("PRICING_002", "Pricing policy already exists",
      HttpStatus.CONFLICT),
  CANNOT_COPY_DIFFERENT_PLACE("PRICING_003", "Cannot copy pricing policy between different places",
      HttpStatus.BAD_REQUEST),
  INVALID_TIME_RANGE("PRICING_004", "Invalid time range", HttpStatus.BAD_REQUEST),
  TIME_RANGE_OVERLAP("PRICING_005", "Time ranges overlap", HttpStatus.BAD_REQUEST),
  ;

  private final String errCode;
  private final String message;
  private final HttpStatus status;

  PricingPolicyErrorCode(final String errCode, final String message, final HttpStatus status) {
    this.errCode = errCode;
    this.message = message;
    this.status = status;
  }

  @Override
  public String toString() {
    return "PricingPolicyErrorCode{"
        + " status='"
        + status
        + '\''
        + "errCode='"
        + errCode
        + '\''
        + ", message='"
        + message
        + '\''
        + '}';
  }
}
