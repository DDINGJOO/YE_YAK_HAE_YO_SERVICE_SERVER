package com.teambind.springproject.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.teambind.springproject.application.dto.response.ProductAvailabilityResponse;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.application.port.out.RoomAllowedProductRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.pricing.PricingStrategy;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 룸별 상품 허용 관리 E2E 테스트.
 * Scenario 12: 룸별 허용 상품 필터링을 테스트합니다.
 */
@DisplayName("룸별 상품 허용 관리 E2E 테스트")
class RoomAllowedProductE2ETest extends BaseE2ETest {

  @Autowired
  private PricingPolicyRepository pricingPolicyRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private RoomAllowedProductRepository roomAllowedProductRepository;

  private Long testPlaceId;
  private Long testRoomAId;
  private Long testRoomBId;
  private Long product1Id;
  private Long product2Id;
  private Long product3Id;

  @BeforeEach
  void setUpTestData() {
    testPlaceId = 999L;  // Use unique placeId to avoid conflicts with other E2E tests
    testRoomAId = 9001L;  // Use unique roomIds
    testRoomBId = 9002L;

    // 이전 테스트 데이터 정리
    roomAllowedProductRepository.deleteByRoomId(testRoomAId);
    roomAllowedProductRepository.deleteByRoomId(testRoomBId);

    // 가격 정책 생성
    createTestPricingPolicy(testRoomAId);
    createTestPricingPolicy(testRoomBId);

    // PLACE Scope 상품 3개 생성
    product1Id = createPlaceScopedProduct("Product 1", 10);
    product2Id = createPlaceScopedProduct("Product 2", 10);
    product3Id = createPlaceScopedProduct("Product 3", 10);

    // 룸A는 상품 1, 2만 허용
    roomAllowedProductRepository.saveAll(testRoomAId, List.of(
        ProductId.of(product1Id),
        ProductId.of(product2Id)
    ));

    // 룸B는 상품 3만 허용
    roomAllowedProductRepository.saveAll(testRoomBId, List.of(
        ProductId.of(product3Id)
    ));
  }

  @Test
  @DisplayName("Scenario 12-1: 룸A에서 허용된 상품만 조회")
  void scenario12_1_RoomAAvailableProductsFiltering() {
    // Given: 룸A는 상품 1, 2만 허용
    final LocalDateTime slot = LocalDateTime.of(2025, 1, 15, 10, 0);

    // When: 룸A에서 상품 가용성 조회
    final ResponseEntity<ProductAvailabilityResponse> response = restTemplate.getForEntity(
        getBaseUrl() + "/api/v1/products/availability?roomId=" + testRoomAId
            + "&placeId=" + testPlaceId
            + "&timeSlots=" + slot,
        ProductAvailabilityResponse.class
    );

    // Then: PLACE scope 상품 중 1, 2만 반환 (RESERVATION/ROOM scope는 필터링 대상 아님)
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    // PLACE scope 상품만 필터링해서 확인
    final var placeProducts = response.getBody().availableProducts().stream()
        .filter(p -> p.productName().startsWith("Product "))
        .toList();

    assertThat(placeProducts).hasSize(2);
    assertThat(placeProducts)
        .extracting("productId")
        .containsExactlyInAnyOrder(product1Id, product2Id);
  }

  @Test
  @DisplayName("Scenario 12-2: 룸B에서 허용된 상품만 조회")
  void scenario12_2_RoomBAvailableProductsFiltering() {
    // Given: 룸B는 상품 3만 허용
    final LocalDateTime slot = LocalDateTime.of(2025, 1, 15, 14, 0);

    // When: 룸B에서 상품 가용성 조회
    final ResponseEntity<ProductAvailabilityResponse> response = restTemplate.getForEntity(
        getBaseUrl() + "/api/v1/products/availability?roomId=" + testRoomBId
            + "&placeId=" + testPlaceId
            + "&timeSlots=" + slot,
        ProductAvailabilityResponse.class
    );

    // Then: PLACE scope 상품 중 3만 반환 (RESERVATION/ROOM scope는 필터링 대상 아님)
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    // PLACE scope 상품만 필터링해서 확인
    final var placeProducts = response.getBody().availableProducts().stream()
        .filter(p -> p.productName().startsWith("Product "))
        .toList();

    assertThat(placeProducts).hasSize(1);
    assertThat(placeProducts.get(0).productId()).isEqualTo(product3Id);
  }

  private void createTestPricingPolicy(final Long roomId) {
    final PricingPolicy policy = PricingPolicy.create(
        RoomId.of(roomId),
        PlaceId.of(testPlaceId),
        TimeSlot.HOUR,
        Money.of(new BigDecimal("50000"))
    );
    pricingPolicyRepository.save(policy);
  }

  private Long createPlaceScopedProduct(final String name, final int totalQuantity) {
    final Product product = Product.createPlaceScoped(
        ProductId.of(null),
        PlaceId.of(testPlaceId),
        name,
        PricingStrategy.oneTime(Money.of(new BigDecimal("5000"))),
        totalQuantity
    );
    final Product saved = productRepository.save(product);
    return saved.getProductId().getValue();
  }
}