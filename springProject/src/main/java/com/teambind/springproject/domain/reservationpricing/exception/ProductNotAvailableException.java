package com.teambind.springproject.domain.reservationpricing.exception;

/**
 * 상품 재고가 부족할 때 발생하는 예외.
 */
public class ProductNotAvailableException extends ReservationPricingException {
	
	public ProductNotAvailableException() {
		super(ReservationPricingErrorCode.PRODUCT_NOT_AVAILABLE);
	}
	
	public ProductNotAvailableException(final String message) {
		super(ReservationPricingErrorCode.PRODUCT_NOT_AVAILABLE, message);
	}
	
	public ProductNotAvailableException(final Long productId, final int requestedQuantity) {
		super(ReservationPricingErrorCode.PRODUCT_NOT_AVAILABLE,
				"Product is not available: productId=" + productId + ", requestedQuantity="
						+ requestedQuantity);
	}
	
	@Override
	public String getExceptionType() {
		return "ProductNotAvailableException";
	}
}
