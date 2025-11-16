package com.teambind.springproject.application.service.product;

import com.teambind.springproject.application.dto.response.ProductResponse;
import com.teambind.springproject.application.port.in.GetAvailableProductsForRoomUseCase;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 룸에서 이용 가능한 상품 조회 Application Service.
 * GetAvailableProductsForRoomUseCase를 구현합니다.
 *
 * 이 서비스는 일반 사용자가 예약 시 선택 가능한 상품 목록을 제공합니다.
 * ProductRepository.findAccessibleProducts()를 활용하여 다음 상품들을 조회합니다:
 * - ROOM Scope: 해당 roomId를 가진 상품
 * - PLACE Scope: placeId를 가지며 RoomAllowedProduct에 등록된 상품
 * - RESERVATION Scope: 모든 룸에서 사용 가능한 상품
 */
@Service
@Transactional(readOnly = true)
public class GetAvailableProductsForRoomService implements GetAvailableProductsForRoomUseCase {

	private static final Logger logger = LoggerFactory.getLogger(GetAvailableProductsForRoomService.class);

	private final ProductRepository productRepository;

	public GetAvailableProductsForRoomService(final ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Override
	public List<ProductResponse> getAvailableProducts(final RoomId roomId, final PlaceId placeId) {
		logger.info("Fetching available products for roomId: {}, placeId: {}",
				roomId.getValue(), placeId.getValue());

		final List<Product> products = productRepository.findAccessibleProducts(placeId, roomId);

		logger.info("Found {} available products for roomId: {}", products.size(), roomId.getValue());

		return products.stream()
				.map(ProductResponse::from)
				.toList();
	}
}