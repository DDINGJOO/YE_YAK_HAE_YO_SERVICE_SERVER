package com.teambind.springproject.adapter.out.persistence.product;

import com.teambind.springproject.application.port.out.RoomAllowedProductRepository;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.pricing.PricingStrategy;
import com.teambind.springproject.domain.product.vo.PricingType;
import com.teambind.springproject.domain.product.vo.ProductScope;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(ProductRepositoryAdapter.class)
@DisplayName("ProductRepository 통합 테스트")
class ProductRepositoryAdapterTest {
	
	@Autowired
	private ProductRepositoryAdapter repository;
	
	@MockBean
	private RoomAllowedProductRepository roomAllowedProductRepository;
	
	@BeforeEach
	void setUp() {
		// Mock returns empty list by default for all roomIds
		when(roomAllowedProductRepository.findAllowedProductIdsByRoomId(any(Long.class)))
				.thenReturn(Collections.emptyList());
	}
	
	@Nested
	@DisplayName("CRUD 동작 테스트")
	class CrudTests {
		
		@Test
		@DisplayName("PLACE 범위 상품 저장 및 조회")
		void savePlaceScopedProduct() {
			// given
			final Product product = Product.createPlaceScoped(
					ProductId.of(null), // Auto-generated ID
					PlaceId.of(100L),
					"공용 빔 프로젝터",
					PricingStrategy.oneTime(Money.of(10000)),
					5
			);
			
			// when
			final Product saved = repository.save(product);
			
			// then
			assertThat(saved).isNotNull();
			assertThat(saved.getProductId()).isNotNull();
			
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			assertThat(found.get().getScope()).isEqualTo(ProductScope.PLACE);
			assertThat(found.get().getPlaceId()).isEqualTo(PlaceId.of(100L));
			assertThat(found.get().getRoomId()).isNull();
			assertThat(found.get().getName()).isEqualTo("공용 빔 프로젝터");
			assertThat(found.get().getTotalQuantity()).isEqualTo(5);
		}
		
		@Test
		@DisplayName("ROOM 범위 상품 저장 및 조회")
		void saveRoomScopedProduct() {
			// given
			final Product product = Product.createRoomScoped(
					ProductId.of(null),
					PlaceId.of(100L),
					RoomId.of(200L),
					"룸 전용 화이트보드",
					PricingStrategy.simpleStock(Money.of(3000)),
					10
			);
			
			// when
			final Product saved = repository.save(product);
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			assertThat(found.get().getScope()).isEqualTo(ProductScope.ROOM);
			assertThat(found.get().getPlaceId()).isEqualTo(PlaceId.of(100L));
			assertThat(found.get().getRoomId()).isEqualTo(RoomId.of(200L));
			assertThat(found.get().getName()).isEqualTo("룸 전용 화이트보드");
		}
		
		@Test
		@DisplayName("RESERVATION 범위 상품 저장 및 조회")
		void saveReservationScopedProduct() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"음료수",
					PricingStrategy.simpleStock(Money.of(2000)),
					100
			);
			
			// when
			final Product saved = repository.save(product);
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			assertThat(found.get().getScope()).isEqualTo(ProductScope.RESERVATION);
			assertThat(found.get().getPlaceId()).isNull();
			assertThat(found.get().getRoomId()).isNull();
			assertThat(found.get().getName()).isEqualTo("음료수");
		}
		
		@Test
		@DisplayName("상품 수정 후 저장")
		void updateProduct() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"원래 이름",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			final Product saved = repository.save(product);
			
			// when
			saved.updateName("새로운 이름");
			saved.updateTotalQuantity(20);
			repository.save(saved);
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			assertThat(found.get().getName()).isEqualTo("새로운 이름");
			assertThat(found.get().getTotalQuantity()).isEqualTo(20);
		}
		
		@Test
		@DisplayName("상품 삭제")
		void deleteProduct() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"삭제할 상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			final Product saved = repository.save(product);
			
			// when
			repository.deleteById(saved.getProductId());
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isEmpty();
		}
		
		@Test
		@DisplayName("상품 존재 여부 확인")
		void existsById() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			final Product saved = repository.save(product);
			
			// when & then
			assertThat(repository.existsById(saved.getProductId())).isTrue();
			assertThat(repository.existsById(ProductId.of(99999L))).isFalse();
		}
	}
	
	@Nested
	@DisplayName("Scope별 조회 테스트")
	class ScopeQueryTests {
		
		@Test
		@DisplayName("PlaceId로 해당 플레이스의 모든 상품 조회")
		void findByPlaceId() {
			// given
			final PlaceId placeId = PlaceId.of(100L);
			
			// PLACE 범위 상품
			repository.save(Product.createPlaceScoped(
					ProductId.of(null),
					placeId,
					"공용 빔 프로젝터",
					PricingStrategy.oneTime(Money.of(10000)),
					5
			));
			
			// ROOM 범위 상품 (같은 PLACE)
			repository.save(Product.createRoomScoped(
					ProductId.of(null),
					placeId,
					RoomId.of(201L),
					"룸 A 화이트보드",
					PricingStrategy.simpleStock(Money.of(3000)),
					2
			));
			
			repository.save(Product.createRoomScoped(
					ProductId.of(null),
					placeId,
					RoomId.of(202L),
					"룸 B 화이트보드",
					PricingStrategy.simpleStock(Money.of(3000)),
					2
			));
			
			// 다른 PLACE의 상품
			repository.save(Product.createPlaceScoped(
					ProductId.of(null),
					PlaceId.of(200L),
					"다른 플레이스 상품",
					PricingStrategy.oneTime(Money.of(5000)),
					3
			));
			
			// when
			final List<Product> products = repository.findByPlaceId(placeId);
			
			// then - PLACE 범위 1개 + ROOM 범위 2개 = 총 3개
			assertThat(products).hasSize(3);
			assertThat(products).allMatch(p -> p.getPlaceId().equals(placeId));
		}
		
		@Test
		@DisplayName("RoomId로 해당 룸의 상품 조회")
		void findByRoomId() {
			// given
			final RoomId roomId = RoomId.of(201L);
			
			repository.save(Product.createRoomScoped(
					ProductId.of(null),
					PlaceId.of(100L),
					roomId,
					"룸 A 화이트보드",
					PricingStrategy.simpleStock(Money.of(3000)),
					2
			));
			
			repository.save(Product.createRoomScoped(
					ProductId.of(null),
					PlaceId.of(100L),
					RoomId.of(202L),
					"룸 B 화이트보드",
					PricingStrategy.simpleStock(Money.of(3000)),
					2
			));
			
			// when
			final List<Product> products = repository.findByRoomId(roomId);
			
			// then
			assertThat(products).hasSize(1);
			assertThat(products.get(0).getRoomId()).isEqualTo(roomId);
		}
		
		@Test
		@DisplayName("ProductScope로 상품 조회")
		void findByScope() {
			// given
			repository.save(Product.createPlaceScoped(
					ProductId.of(null),
					PlaceId.of(100L),
					"PLACE 상품",
					PricingStrategy.oneTime(Money.of(10000)),
					5
			));
			
			repository.save(Product.createReservationScoped(
					ProductId.of(null),
					"RESERVATION 상품 1",
					PricingStrategy.simpleStock(Money.of(2000)),
					100
			));
			
			repository.save(Product.createReservationScoped(
					ProductId.of(null),
					"RESERVATION 상품 2",
					PricingStrategy.simpleStock(Money.of(3000)),
					50
			));
			
			// when
			final List<Product> reservationProducts = repository.findByScope(ProductScope.RESERVATION);
			final List<Product> placeProducts = repository.findByScope(ProductScope.PLACE);
			
			// then
			assertThat(reservationProducts).hasSize(2);
			assertThat(placeProducts).hasSize(1);
			assertThat(reservationProducts).allMatch(p -> p.getScope() == ProductScope.RESERVATION);
		}
	}
	
	@Nested
	@DisplayName("PricingStrategy 저장/조회 테스트")
	class PricingStrategyTests {
		
		@Test
		@DisplayName("INITIAL_PLUS_ADDITIONAL 타입 저장 및 조회")
		void saveInitialPlusAdditionalType() {
			// given
			final Product product = Product.createPlaceScoped(
					ProductId.of(null),
					PlaceId.of(100L),
					"빔 프로젝터",
					PricingStrategy.initialPlusAdditional(Money.of(10000), Money.of(5000)),
					5
			);
			
			// when
			final Product saved = repository.save(product);
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			
			final PricingStrategy strategy = found.get().getPricingStrategy();
			assertThat(strategy.getPricingType()).isEqualTo(PricingType.INITIAL_PLUS_ADDITIONAL);
			assertThat(strategy.getInitialPrice()).isEqualTo(Money.of(10000));
			assertThat(strategy.getAdditionalPrice()).isEqualTo(Money.of(5000));
		}
		
		@Test
		@DisplayName("ONE_TIME 타입 저장 및 조회")
		void saveOneTimeType() {
			// given
			final Product product = Product.createRoomScoped(
					ProductId.of(null),
					PlaceId.of(100L),
					RoomId.of(200L),
					"1회 대여 장비",
					PricingStrategy.oneTime(Money.of(15000)),
					10
			);
			
			// when
			final Product saved = repository.save(product);
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			
			final PricingStrategy strategy = found.get().getPricingStrategy();
			assertThat(strategy.getPricingType()).isEqualTo(PricingType.ONE_TIME);
			assertThat(strategy.getInitialPrice()).isEqualTo(Money.of(15000));
			assertThat(strategy.getAdditionalPrice()).isNull();
		}
		
		@Test
		@DisplayName("SIMPLE_STOCK 타입 저장 및 조회")
		void saveSimpleStockType() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"음료수",
					PricingStrategy.simpleStock(Money.of(2000)),
					100
			);
			
			// when
			final Product saved = repository.save(product);
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			
			final PricingStrategy strategy = found.get().getPricingStrategy();
			assertThat(strategy.getPricingType()).isEqualTo(PricingType.SIMPLE_STOCK);
			assertThat(strategy.getInitialPrice()).isEqualTo(Money.of(2000));
			assertThat(strategy.getAdditionalPrice()).isNull();
		}
		
		@Test
		@DisplayName("PricingStrategy 변경 후 저장")
		void updatePricingStrategy() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			final Product saved = repository.save(product);
			
			// when
			saved.updatePricingStrategy(PricingStrategy.oneTime(Money.of(5000)));
			repository.save(saved);
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			
			final PricingStrategy strategy = found.get().getPricingStrategy();
			assertThat(strategy.getPricingType()).isEqualTo(PricingType.ONE_TIME);
			assertThat(strategy.getInitialPrice()).isEqualTo(Money.of(5000));
		}
	}
}
