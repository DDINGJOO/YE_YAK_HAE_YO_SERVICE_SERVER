package com.teambind.springproject.adapter.out.persistence.product;

import com.teambind.springproject.application.port.out.ProductRepository;
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

  public ProductRepositoryAdapter(final ProductJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
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
    return jpaRepository.findAccessibleProducts(placeId.getValue(), roomId.getValue())
        .stream()
        .map(ProductEntity::toDomain)
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
