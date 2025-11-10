package com.teambind.springproject.adapter.out.persistence.product;

import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.application.port.out.RoomAllowedProductRepository;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.ProductScope;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * ProductRepository Port의 JPA Adapter 구현.
 * Hexagonal Architecture의 Adapter 계층입니다.
 */
@Repository
public class ProductRepositoryAdapter implements ProductRepository {

  private final ProductJpaRepository jpaRepository;
  private final RoomAllowedProductRepository roomAllowedProductRepository;

  public ProductRepositoryAdapter(
      final ProductJpaRepository jpaRepository,
      final RoomAllowedProductRepository roomAllowedProductRepository) {
    this.jpaRepository = jpaRepository;
    this.roomAllowedProductRepository = roomAllowedProductRepository;
  }

  @Override
  public Optional<Product> findById(final ProductId productId) {
    return jpaRepository.findById(productId.getValue())
        .map(ProductEntity::toDomain);
  }

  @Override
  public List<Product> findByPlaceId(final PlaceId placeId) {
    return jpaRepository.findByPlaceId(placeId.getValue())
        .stream()
        .map(ProductEntity::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Product> findByRoomId(final RoomId roomId) {
    return jpaRepository.findByRoomId(roomId.getValue())
        .stream()
        .map(ProductEntity::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Product> findAccessibleProducts(final PlaceId placeId, final RoomId roomId) {
    // 1. DB에서 전체 접근 가능한 상품 조회 (PLACE, ROOM, RESERVATION)
    final List<Product> allProducts = jpaRepository.findAccessibleProducts(
            placeId.getValue(),
            roomId.getValue())
        .stream()
        .map(ProductEntity::toDomain)
        .collect(Collectors.toList());

    // 2. 룸별 허용 상품 ID 목록 조회
    final List<ProductId> allowedProductIds = roomAllowedProductRepository
        .findAllowedProductIdsByRoomId(roomId.getValue());

    // 3. PLACE Scope 상품 필터링 (화이트리스트 방식)
    return allProducts.stream()
        .filter(product -> {
          // PLACE 상품이 아니면 그대로 통과 (ROOM, RESERVATION은 필터링 안 함)
          if (product.getScope() != ProductScope.PLACE) {
            return true;
          }

          // PLACE 상품인 경우: 허용 목록에 있는 경우만 통과
          // 매핑이 없으면 (allowedProductIds가 비어있으면) PLACE 상품은 모두 제외
          return allowedProductIds.contains(product.getProductId());
        })
        .collect(Collectors.toList());
  }

  @Override
  public List<Product> findByScope(final ProductScope scope) {
    return jpaRepository.findByScope(scope)
        .stream()
        .map(ProductEntity::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Product save(final Product product) {
    final ProductEntity entity = ProductEntity.fromDomain(product);
    final ProductEntity savedEntity = jpaRepository.save(entity);
    return savedEntity.toDomain();
  }

  @Override
  public void deleteById(final ProductId productId) {
    jpaRepository.deleteById(productId.getValue());
  }

  @Override
  public boolean existsById(final ProductId productId) {
    return jpaRepository.existsById(productId.getValue());
  }
}
