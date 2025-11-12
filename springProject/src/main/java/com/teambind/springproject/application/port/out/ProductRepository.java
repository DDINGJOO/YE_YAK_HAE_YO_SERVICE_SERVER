package com.teambind.springproject.application.port.out;

import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.vo.ProductScope;
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
	 * 여러 ProductId로 상품들을 일괄 조회합니다.
	 * N+1 쿼리를 방지하기 위해 한 번의 쿼리로 조회합니다.
	 *
	 * @param productIds 상품 ID 목록
	 * @return 상품 목록 (존재하는 상품만 반환)
	 */
	List<Product> findAllById(List<ProductId> productIds);
	
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
	 * @param roomId  룸 ID
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

	/**
	 * 원자적으로 상품 재고를 예약합니다.
	 * UPDATE 쿼리의 WHERE 조건에서 재고 검증과 차감을 동시에 수행합니다.
	 *
	 * 이 메서드는 다음과 같은 원자적 연산을 수행합니다:
	 * - UPDATE products SET reserved_quantity = reserved_quantity + quantity
	 * - WHERE product_id = ? AND (total_quantity - reserved_quantity) >= quantity
	 *
	 * 동시성 제어 메커니즘:
	 * - 데이터베이스의 Row Lock을 활용하여 원자적 연산 보장
	 * - WHERE 조건에서 재고 부족 시 UPDATE 실패 (0 rows affected)
	 * - Race Condition 방지 (Check-Then-Act 안티패턴 해결)
	 *
	 * @param productId 상품 ID
	 * @param quantity  예약할 수량 (양수)
	 * @return 예약 성공 여부 (재고 부족 시 false)
	 * @throws IllegalArgumentException quantity가 0 이하인 경우
	 */
	boolean reserveQuantity(ProductId productId, int quantity);

	/**
	 * 예약 취소 시 상품 재고를 복구합니다.
	 * reserved_quantity를 감소시켜 가용 수량을 증가시킵니다.
	 *
	 * 이 메서드는 다음과 같은 원자적 연산을 수행합니다:
	 * - UPDATE products SET reserved_quantity = reserved_quantity - quantity
	 * - WHERE product_id = ? AND reserved_quantity >= quantity
	 *
	 * 주의사항:
	 * - 예약된 수량보다 많은 수량을 해제할 수 없습니다
	 * - 트랜잭션 내에서 호출하여 일관성을 보장해야 합니다
	 *
	 * @param productId 상품 ID
	 * @param quantity  해제할 수량 (양수)
	 * @return 해제 성공 여부 (예약 수량 부족 시 false)
	 * @throws IllegalArgumentException quantity가 0 이하인 경우
	 */
	boolean releaseQuantity(ProductId productId, int quantity);
}
