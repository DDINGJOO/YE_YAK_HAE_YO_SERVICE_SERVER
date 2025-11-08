package com.teambind.springproject.domain.pricingpolicy.exception;

/**
 * 다른 Place 간 가격 정책 복사 시도 시 발생하는 예외.
 */
public class CannotCopyDifferentPlaceException extends PricingPolicyException {

  public CannotCopyDifferentPlaceException() {
    super(PricingPolicyErrorCode.CANNOT_COPY_DIFFERENT_PLACE);
  }

  public CannotCopyDifferentPlaceException(final String message) {
    super(PricingPolicyErrorCode.CANNOT_COPY_DIFFERENT_PLACE, message);
  }

  @Override
  public String getExceptionType() {
    return "APPLICATION";
  }
}
