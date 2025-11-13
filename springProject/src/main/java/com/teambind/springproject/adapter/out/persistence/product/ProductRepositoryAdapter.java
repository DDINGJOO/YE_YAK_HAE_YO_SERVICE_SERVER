package com.teambind.springproject.adapter.out.persistence.product;

import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.application.port.out.RoomAllowedProductRepository;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.vo.ProductScope;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ProductRepository Port의 JPA Adapter 구현.
 * Hexagonal Architecture의 Adapter 계층입니다.
 */
@Repository
public class ProductRepositoryAdapter implements ProductRepository {

	private final ProductJpaRepository jpaRepository;
	private final RoomAllowedProductRepository roomAllowedProductRepository;
	private final JdbcTemplate jdbcTemplate;

	public ProductRepositoryAdapter(
			final ProductJpaRepository jpaRepository,
			final RoomAllowedProductRepository roomAllowedProductRepository,
			final JdbcTemplate jdbcTemplate) {
		this.jpaRepository = jpaRepository;
		this.roomAllowedProductRepository = roomAllowedProductRepository;
		this.jdbcTemplate = jdbcTemplate;
	}
	
	@Override
	public Optional<Product> findById(final ProductId productId) {
		return jpaRepository.findById(productId.getValue())
				.map(ProductEntity::toDomain);
	}
	
	@Override
	public List<Product> findAllById(final List<ProductId> productIds) {
		final List<Long> ids = productIds.stream()
				.map(ProductId::getValue)
				.collect(Collectors.toList());
		
		return jpaRepository.findAllById(ids)
				.stream()
				.map(ProductEntity::toDomain)
				.collect(Collectors.toList());
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

	@Override
	public boolean reserveQuantity(final ProductId productId, final int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be positive: " + quantity);
		}

		final String sql = """
				UPDATE products
				SET reserved_quantity = reserved_quantity + ?
				WHERE product_id = ?
				  AND (total_quantity - reserved_quantity) >= ?
				""";

		final int updatedRows = jdbcTemplate.update(
				sql,
				quantity,
				productId.getValue(),
				quantity
		);

		return updatedRows > 0;
	}

	@Override
	public boolean releaseQuantity(final ProductId productId, final int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be positive: " + quantity);
		}

		final String sql = """
				UPDATE products
				SET reserved_quantity = reserved_quantity - ?
				WHERE product_id = ?
				  AND reserved_quantity >= ?
				""";

		final int updatedRows = jdbcTemplate.update(
				sql,
				quantity,
				productId.getValue(),
				quantity
		);

		return updatedRows > 0;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public boolean reserveRoomTimeSlotQuantity(
			final ProductId productId,
			final RoomId roomId,
			final LocalDateTime timeSlot,
			final int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be positive: " + quantity);
		}

		// CRITICAL FIX: INSERT에도 재고 검증 추가
		// ON CONFLICT DO UPDATE의 WHERE는 UPDATE에만 적용되므로,
		// INSERT 시에도 재고 검증이 필요합니다.
		final String sql = """
				INSERT INTO product_time_slot_inventory
				    (product_id, room_id, time_slot, reserved_quantity)
				SELECT ?, ?, ?, ?
				WHERE EXISTS (
				    SELECT 1
				    FROM products p
				    WHERE p.product_id = ?
				      AND p.total_quantity >= ?
				)
				ON CONFLICT (product_id, room_id, time_slot)
				DO UPDATE SET
				    reserved_quantity = product_time_slot_inventory.reserved_quantity + EXCLUDED.reserved_quantity,
				    updated_at = NOW()
				WHERE (
				    SELECT p.total_quantity
				    FROM products p
				    WHERE p.product_id = EXCLUDED.product_id
				) >= (product_time_slot_inventory.reserved_quantity + EXCLUDED.reserved_quantity)
				""";

		final int updatedRows = jdbcTemplate.update(
				sql,
				productId.getValue(),
				roomId.getValue(),
				timeSlot,
				quantity,
				productId.getValue(),  // WHERE EXISTS - product_id
				quantity               // WHERE EXISTS - quantity check
		);

		return updatedRows > 0;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public boolean reservePlaceTimeSlotQuantity(
			final ProductId productId,
			final RoomId roomId,
			final LocalDateTime timeSlot,
			final int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be positive: " + quantity);
		}

		// PLACE Scope: Use product_time_slot_inventory.total_quantity (per time slot)
		// V13 migration added this to enforce time-slot-specific inventory limits
		// NO FALLBACK - if no inventory record exists for this time slot, reservation should fail

		// Step 1: Lock all rows for this product and time slot
		// Note: Cannot use FOR UPDATE with aggregate functions, so we lock rows first then aggregate in code
		final String lockSql = """
				SELECT total_quantity, reserved_quantity
				FROM product_time_slot_inventory
				WHERE product_id = ?
				  AND time_slot = ?
				FOR UPDATE
				""";

		final java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(
				lockSql,
				productId.getValue(),
				timeSlot
		);

		// If no inventory record exists for this time slot, reservation fails
		if (rows.isEmpty()) {
			return false;  // No time slot inventory configured
		}

		// Step 2: Aggregate total_quantity and reserved_quantity across all rooms
		Integer totalQuantity = null;
		int currentReserved = 0;

		for (java.util.Map<String, Object> row : rows) {
			final Integer rowTotalQty = (Integer) row.get("total_quantity");
			final Integer rowReservedQty = (Integer) row.get("reserved_quantity");

			// total_quantity should be same across all rooms (shared inventory)
			if (totalQuantity == null) {
				totalQuantity = rowTotalQty;
			}

			// Sum reserved quantities across all rooms
			if (rowReservedQty != null) {
				currentReserved += rowReservedQty;
			}
		}

		// Validate total_quantity
		if (totalQuantity == null || totalQuantity == 0) {
			return false;  // Invalid inventory configuration
		}

		// Step 3: Check if reservation is possible
		if (totalQuantity < currentReserved + quantity) {
			return false;  // Not enough inventory
		}

		// Step 3: Insert or update the inventory record
		final String upsertSql = """
				INSERT INTO product_time_slot_inventory
				    (product_id, room_id, time_slot, total_quantity, reserved_quantity)
				VALUES (?, ?, ?, ?, ?)
				ON CONFLICT (product_id, room_id, time_slot)
				DO UPDATE SET
				    reserved_quantity = product_time_slot_inventory.reserved_quantity + EXCLUDED.reserved_quantity,
				    updated_at = NOW()
				""";

		final int updatedRows = jdbcTemplate.update(
				upsertSql,
				productId.getValue(),
				roomId.getValue(),
				timeSlot,
				totalQuantity,
				quantity
		);

		return updatedRows > 0;
	}

	@Override
	public boolean releaseTimeSlotQuantity(
			final ProductId productId,
			final RoomId roomId,
			final LocalDateTime timeSlot,
			final int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be positive: " + quantity);
		}

		final String sql = """
				UPDATE product_time_slot_inventory
				SET reserved_quantity = reserved_quantity - ?,
				    updated_at = NOW()
				WHERE product_id = ?
				  AND room_id = ?
				  AND time_slot = ?
				  AND reserved_quantity >= ?
				""";

		final int updatedRows = jdbcTemplate.update(
				sql,
				quantity,
				productId.getValue(),
				roomId.getValue(),
				timeSlot,
				quantity
		);

		return updatedRows > 0;
	}
}
