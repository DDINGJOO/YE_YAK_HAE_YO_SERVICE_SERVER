package com.teambind.springproject.application.service.product;

import com.teambind.springproject.application.dto.request.SetRoomAllowedProductsRequest;
import com.teambind.springproject.application.dto.response.RoomAllowedProductsResponse;
import com.teambind.springproject.application.port.in.SetRoomAllowedProductsUseCase;
import com.teambind.springproject.application.port.out.RoomAllowedProductRepository;
import com.teambind.springproject.domain.shared.ProductId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 룸 허용 상품 설정 Application Service.
 * SetRoomAllowedProductsUseCase를 구현합니다.
 */
@Service
@Transactional
public class SetRoomAllowedProductsService implements SetRoomAllowedProductsUseCase {

  private static final Logger logger = LoggerFactory.getLogger(
      SetRoomAllowedProductsService.class);

  private final RoomAllowedProductRepository roomAllowedProductRepository;

  public SetRoomAllowedProductsService(
      final RoomAllowedProductRepository roomAllowedProductRepository) {
    this.roomAllowedProductRepository = roomAllowedProductRepository;
  }

  @Override
  public RoomAllowedProductsResponse setAllowedProducts(
      final Long roomId,
      final SetRoomAllowedProductsRequest request) {
    logger.info("Setting allowed products for room: roomId={}, productCount={}",
        roomId, request.productIds().size());

    // Long List를 ProductId List로 변환
    final List<ProductId> productIds = request.productIds().stream()
        .map(ProductId::of)
        .toList();

    // Repository에 저장 (기존 매핑 삭제 후 저장)
    roomAllowedProductRepository.saveAll(roomId, productIds);

    logger.info("Successfully set allowed products for room: roomId={}, productCount={}",
        roomId, productIds.size());

    // 저장된 데이터 조회 및 반환
    final List<ProductId> savedProductIds = roomAllowedProductRepository
        .findAllowedProductIdsByRoomId(roomId);
    return RoomAllowedProductsResponse.from(roomId, savedProductIds);
  }
}