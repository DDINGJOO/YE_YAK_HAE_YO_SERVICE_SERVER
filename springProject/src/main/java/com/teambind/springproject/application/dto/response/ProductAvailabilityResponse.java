package com.teambind.springproject.application.dto.response;

import java.util.List;

/**
 * 상품 재고 가용성 조회 응답 DTO.
 */
public record ProductAvailabilityResponse(
    Long roomId,
    Long placeId,
    List<AvailableProductDto> availableProducts
) {

}
