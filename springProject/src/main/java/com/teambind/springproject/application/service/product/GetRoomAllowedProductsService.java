package com.teambind.springproject.application.service.product;

import com.teambind.springproject.application.dto.response.RoomAllowedProductsResponse;
import com.teambind.springproject.application.port.in.GetRoomAllowedProductsUseCase;
import com.teambind.springproject.application.port.out.RoomAllowedProductRepository;
import com.teambind.springproject.domain.shared.ProductId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 룸 허용 상품 조회 Application Service.
 * GetRoomAllowedProductsUseCase를 구현합니다.
 */
@Service
@Transactional(readOnly = true)
public class GetRoomAllowedProductsService implements GetRoomAllowedProductsUseCase {
	
	private static final Logger logger = LoggerFactory.getLogger(
			GetRoomAllowedProductsService.class);
	
	private final RoomAllowedProductRepository roomAllowedProductRepository;
	
	public GetRoomAllowedProductsService(
			final RoomAllowedProductRepository roomAllowedProductRepository) {
		this.roomAllowedProductRepository = roomAllowedProductRepository;
	}
	
	@Override
	public RoomAllowedProductsResponse getAllowedProducts(final Long roomId) {
		logger.info("Getting allowed products for room: roomId={}", roomId);
		
		final List<ProductId> productIds = roomAllowedProductRepository
				.findAllowedProductIdsByRoomId(roomId);
		
		logger.info("Found {} allowed products for room: roomId={}", productIds.size(), roomId);
		
		return RoomAllowedProductsResponse.from(roomId, productIds);
	}
}
