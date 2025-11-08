package com.teambind.springproject.domain.pricingpolicy.exception;

/**
 * 가격 정책을 찾을 수 없을 때 발생하는 예외.
 */
public class PricingPolicyNotFoundException extends PricingPolicyException {

  public PricingPolicyNotFoundException() {
    super(PricingPolicyErrorCode.PRICING_POLICY_NOT_FOUND);
  }

  public PricingPolicyNotFoundException(final String message) {
    super(PricingPolicyErrorCode.PRICING_POLICY_NOT_FOUND, message);
  }

  @Override
  public String getExceptionType() {
    return "APPLICATION";
  }
}
