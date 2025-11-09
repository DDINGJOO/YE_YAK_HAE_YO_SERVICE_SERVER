package com.teambind.springproject.application.port.out;

import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.ProductScope;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import java.util.List;
import java.util.Optional;

/**
 * Product Aggregate를 영속화하기 위한 Repository Port.
 * Hexagonal Architecture의 출력 포트(Output Port)입니다.
 */
public interface ProductRepository {

  /**
   * ProductId로 상품을 조회합니다.
   *
   * @param productId 상품 ID
   * @return 상품 (없으면 Optional.empty())
   */
  Optional<Product> findById(ProductId productId);

  /**
   * PlaceId로 해당 플레이스의 모든 상품을 조회합니다.
   * PLACE 범위 상품과 ROOM 범위 상품을 모두 포함합니다.
   *
   * @param placeId 플레이스 ID
   * @return 상품 목록
   */
  List<Product> findByPlaceId(PlaceId placeId);

  /**
   * RoomId로 해당 룸의 상품을 조회합니다.
   * ROOM 범위 상품만 반환합니다.
   *
   * @param roomId 룸 ID
   * @return 상품 목록
   */
  List<Product> findByRoomId(RoomId roomId);

  /**
   * 특정 룸에서 접근 가능한 모든 상품을 조회합니다.
   * - ROOM scope: roomId가 일치하는 상품
   * - PLACE scope: placeId가 일치하는 상품
   * - RESERVATION scope: 모든 RESERVATION 상품
   *
   * @param placeId 플레이스 ID
   * @param roomId 룸 ID
   * @return 접근 가능한 상품 목록
   */
  List<Product> findAccessibleProducts(PlaceId placeId, RoomId roomId);

  /**
   * ProductScope로 상품을 조회합니다.
   *
   * @param scope 상품 범위
   * @return 상품 목록
   */
  List<Product> findByScope(ProductScope scope);

  /**
   * 상품을 저장합니다.
   * 새로운 상품이면 INSERT, 기존 상품이면 UPDATE합니다.
   *
   * @param product 저장할 상품
   * @return 저장된 상품
   */
  Product save(Product product);

  /**
   * ProductId로 상품을 삭제합니다.
   *
   * @param productId 상품 ID
   */
  void deleteById(ProductId productId);

  /**
   * ProductId에 해당하는 상품이 존재하는지 확인합니다.
   *
   * @param productId 상품 ID
   * @return 존재하면 true, 아니면 false
   */
  boolean existsById(ProductId productId);
}
