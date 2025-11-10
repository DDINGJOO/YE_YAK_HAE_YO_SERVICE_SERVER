package com.teambind.springproject.application.service.product;

import com.teambind.springproject.application.port.in.DeleteRoomAllowedProductsUseCase;
import com.teambind.springproject.application.port.out.RoomAllowedProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 룸 허용 상품 삭제 Application Service.
 * DeleteRoomAllowedProductsUseCase를 구현합니다.
 */
@Service
@Transactional
public class DeleteRoomAllowedProductsService implements DeleteRoomAllowedProductsUseCase {

  private static final Logger logger = LoggerFactory.getLogger(
      DeleteRoomAllowedProductsService.class);

  private final RoomAllowedProductRepository roomAllowedProductRepository;

  public DeleteRoomAllowedProductsService(
      final RoomAllowedProductRepository roomAllowedProductRepository) {
    this.roomAllowedProductRepository = roomAllowedProductRepository;
  }

  @Override
  public void deleteAllowedProducts(final Long roomId) {
    logger.info("Deleting all allowed products for room: roomId={}", roomId);

    roomAllowedProductRepository.deleteByRoomId(roomId);

    logger.info("Successfully deleted all allowed products for room: roomId={}", roomId);
  }
}