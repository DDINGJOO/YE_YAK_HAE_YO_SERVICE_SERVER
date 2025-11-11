package com.teambind.springproject.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.request.ProductRequest;
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.ProductScope;
import com.teambind.springproject.domain.product.PricingStrategy;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.ReservationStatus;
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
import org.springframework.web.client.HttpClientErrorException;

/**
 * 재고 관리 E2E 테스트.
 * Scope별 재고 검증 및 재고 부족 시나리오를 테스트합니다.
 */
@DisplayName("재고 관리 E2E 테스트")
class InventoryManagementE2ETest extends BaseE2ETest {

  @Autowired
  private PricingPolicyRepository pricingPolicyRepository;

  @Autowired
  private ProductRepository productRepository;

  private Long testPlaceId;
  private Long testRoomId;
  private Long placeProductId;
  private Long roomProductId;
  private Long reservationProductId;

  @BeforeEach
  void setUpTestData() {
    testPlaceId = 1L;
    testRoomId = 100L;

    // 가격 정책 생성
    createTestPricingPolicy();

    // Scope별 상품 생성
    placeProductId = createPlaceScopedProduct("Place Product", 10);
    roomProductId = createRoomScopedProduct("Room Product", 5);
    reservationProductId = createReservationScopedProduct("Reservation Product", 100);
  }

  @Test
  @DisplayName("Scenario 4: PLACE scope 상품으로 예약 생성")
  void scenario4_PlaceScopeProductReservation() {
    // Given: PLACE scope 상품
    // When: PLACE scope 상품으로 예약 생성
    final LocalDateTime slot = LocalDateTime.of(2025, 1, 15, 10, 0);
    final CreateReservationRequest request = new CreateReservationRequest(
        testRoomId,
        List.of(slot),
        List.of(new ProductRequest(placeProductId, 3))
    );

    final ResponseEntity<ReservationPricingResponse> response = restTemplate.postForEntity(
        getBaseUrl() + "/api/v1/reservations",
        request,
        ReservationPricingResponse.class
    );

    // Then: 예약 성공
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(ReservationStatus.PENDING);
    assertThat(response.getBody().totalPrice()).isGreaterThan(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("Scenario 5: ROOM scope 상품으로 예약 생성")
  void scenario5_RoomScopeProductReservation() {
    // Given: ROOM scope 상품
    // When: ROOM scope 상품으로 예약 생성
    final LocalDateTime slot = LocalDateTime.of(2025, 1, 15, 14, 0);
    final CreateReservationRequest request = new CreateReservationRequest(
        testRoomId,
        List.of(slot),
        List.of(new ProductRequest(roomProductId, 2))
    );

    final ResponseEntity<ReservationPricingResponse> response = restTemplate.postForEntity(
        getBaseUrl() + "/api/v1/reservations",
        request,
        ReservationPricingResponse.class
    );

    // Then: 예약 성공
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(ReservationStatus.PENDING);
    assertThat(response.getBody().totalPrice()).isGreaterThan(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("Scenario 6: RESERVATION scope는 시간과 무관하게 재고 검증")
  void scenario6_ReservationScopeTimeIndependentValidation() {
    // Given: RESERVATION scope 상품은 시간과 무관하게 총 재고만 확인 (100개)
    // When: 같은 시간대에 50개씩 2번 예약 (시간이 겹쳐도 OK)
    final LocalDateTime slot = LocalDateTime.of(2025, 1, 15, 18, 0);
    final CreateReservationRequest request1 = new CreateReservationRequest(
        testRoomId,
        List.of(slot),
        List.of(new ProductRequest(reservationProductId, 50))
    );

    final ResponseEntity<ReservationPricingResponse> response1 = restTemplate.postForEntity(
        getBaseUrl() + "/api/v1/reservations",
        request1,
        ReservationPricingResponse.class
    );

    // Then: 예약 성공
    assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    // When: 같은 시간대에 추가로 50개 예약 (PLACE/ROOM scope였다면 실패했을 것)
    final CreateReservationRequest request2 = new CreateReservationRequest(
        testRoomId,
        List.of(slot),
        List.of(new ProductRequest(reservationProductId, 50))
    );

    final ResponseEntity<ReservationPricingResponse> response2 = restTemplate.postForEntity(
        getBaseUrl() + "/api/v1/reservations",
        request2,
        ReservationPricingResponse.class
    );

    // Then: 예약 성공 (시간 무관하므로 각 예약마다 독립적으로 재고 확인)
    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  private void createTestPricingPolicy() {
    final PricingPolicy policy = PricingPolicy.create(
        RoomId.of(testRoomId),
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

  private Long createRoomScopedProduct(final String name, final int totalQuantity) {
    final Product product = Product.createRoomScoped(
        ProductId.of(null),
        PlaceId.of(testPlaceId),
        RoomId.of(testRoomId),
        name,
        PricingStrategy.oneTime(Money.of(new BigDecimal("3000"))),
        totalQuantity
    );
    final Product saved = productRepository.save(product);
    return saved.getProductId().getValue();
  }

  private Long createReservationScopedProduct(final String name, final int totalQuantity) {
    final Product product = Product.createReservationScoped(
        ProductId.of(null),
        name,
        PricingStrategy.oneTime(Money.of(new BigDecimal("1000"))),
        totalQuantity
    );
    final Product saved = productRepository.save(product);
    return saved.getProductId().getValue();
  }
}