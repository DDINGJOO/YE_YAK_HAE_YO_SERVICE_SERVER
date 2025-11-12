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
	public boolean reserveRoomTimeSlotQuantity(
			final ProductId productId,
			final RoomId roomId,
			final LocalDateTime timeSlot,
			final int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be positive: " + quantity);
		}

		final String sql = """
				INSERT INTO product_time_slot_inventory
				    (product_id, room_id, time_slot, reserved_quantity)
				VALUES (?, ?, ?, ?)
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
				quantity
		);

		return updatedRows > 0;
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public boolean reservePlaceTimeSlotQuantity(
			final ProductId productId,
			final RoomId roomId,
			final LocalDateTime timeSlot,
			final int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be positive: " + quantity);
		}

		// PLACE Scope: Lock product row first, then check total across all rooms
		// CRITICAL: This method MUST run within an existing transaction for FOR UPDATE to work correctly
		// Step 1: Lock the product row to prevent concurrent modifications
		final String lockSql = """
				SELECT total_quantity
				FROM products
				WHERE product_id = ?
				FOR UPDATE
				""";

		final Integer totalQuantity = jdbcTemplate.queryForObject(
				lockSql,
				Integer.class,
				productId.getValue()
		);

		if (totalQuantity == null) {
			return false;  // Product not found
		}

		// Step 2: Calculate current total reserved quantity across all rooms for this time slot
		final String currentTotalSql = """
				SELECT COALESCE(SUM(reserved_quantity), 0)
				FROM product_time_slot_inventory
				WHERE product_id = ?
				  AND time_slot = ?
				""";

		final Integer currentTotal = jdbcTemplate.queryForObject(
				currentTotalSql,
				Integer.class,
				productId.getValue(),
				timeSlot
		);

		// Step 3: Check if reservation is possible
		if (totalQuantity < (currentTotal != null ? currentTotal : 0) + quantity) {
			return false;  // Not enough inventory
		}

		// Step 4: Insert or update the inventory record
		final String upsertSql = """
				INSERT INTO product_time_slot_inventory
				    (product_id, room_id, time_slot, reserved_quantity)
				VALUES (?, ?, ?, ?)
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
