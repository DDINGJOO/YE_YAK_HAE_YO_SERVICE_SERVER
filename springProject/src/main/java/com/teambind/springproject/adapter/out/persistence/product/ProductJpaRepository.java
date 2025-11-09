package com.teambind.springproject.adapter.out.persistence.product;

import com.teambind.springproject.domain.product.ProductScope;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Product Entity를 위한 Spring Data JPA Repository.
 */
public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

  /**
   * PlaceId로 해당 플레이스의 모든 상품을 조회합니다.
   * PLACE 범위 상품과 ROOM 범위 상품을 모두 포함합니다.
   *
   * @param placeId 플레이스 ID
   * @return 상품 엔티티 목록
   */
  List<ProductEntity> findByPlaceId(Long placeId);

  /**
   * RoomId로 해당 룸의 상품을 조회합니다.
   * ROOM 범위 상품만 반환합니다.
   *
   * @param roomId 룸 ID
   * @return 상품 엔티티 목록
   */
  List<ProductEntity> findByRoomId(Long roomId);

  /**
   * 특정 룸에서 접근 가능한 모든 상품을 조회합니다.
   * - ROOM scope: roomId가 일치하는 상품
   * - PLACE scope: placeId가 일치하는 상품
   * - RESERVATION scope: 모든 RESERVATION 상품
   *
   * @param placeId 플레이스 ID
   * @param roomId 룸 ID
   * @return 접근 가능한 상품 엔티티 목록
   */
  @Query("SELECT p FROM ProductEntity p WHERE " +
      "(p.scope = 'ROOM' AND p.roomId = :roomId) OR " +
      "(p.scope = 'PLACE' AND p.placeId = :placeId) OR " +
      "(p.scope = 'RESERVATION')")
  List<ProductEntity> findAccessibleProducts(
      @Param("placeId") Long placeId,
      @Param("roomId") Long roomId);

  /**
   * ProductScope로 상품을 조회합니다.
   *
   * @param scope 상품 범위
   * @return 상품 엔티티 목록
   */
  List<ProductEntity> findByScope(ProductScope scope);
}
