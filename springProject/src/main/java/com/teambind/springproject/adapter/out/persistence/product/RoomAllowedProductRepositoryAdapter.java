package com.teambind.springproject.adapter.out.persistence.product;

import com.teambind.springproject.application.port.out.RoomAllowedProductRepository;
import com.teambind.springproject.domain.product.RoomAllowedProduct;
import com.teambind.springproject.domain.shared.ProductId;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * RoomAllowedProductRepository Port의 JPA Adapter 구현.
 * Hexagonal Architecture의 Adapter 계층입니다.
 */
@Repository
public class RoomAllowedProductRepositoryAdapter implements RoomAllowedProductRepository {

  private final RoomAllowedProductJpaRepository jpaRepository;

  public RoomAllowedProductRepositoryAdapter(
      final RoomAllowedProductJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProductId> findAllowedProductIdsByRoomId(final Long roomId) {
    return jpaRepository.findByRoomId(roomId)
        .stream()
        .map(RoomAllowedProductEntity::toDomain)
        .map(RoomAllowedProduct::productId)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void saveAll(final Long roomId, final List<ProductId> productIds) {
    // 1. 기존 매핑 삭제
    jpaRepository.deleteByRoomId(roomId);

    // 2. 새로운 매핑 저장
    if (productIds != null && !productIds.isEmpty()) {
      final List<RoomAllowedProductEntity> entities = productIds.stream()
          .map(productId -> new RoomAllowedProduct(roomId, productId))
          .map(RoomAllowedProductEntity::from)
          .collect(Collectors.toList());

      jpaRepository.saveAll(entities);
    }
  }

  @Override
  @Transactional
  public void deleteByRoomId(final Long roomId) {
    jpaRepository.deleteByRoomId(roomId);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByRoomId(final Long roomId) {
    return jpaRepository.existsByRoomId(roomId);
  }
}
