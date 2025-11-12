package com.teambind.springproject.integration.e2e;

import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.request.ProductRequest;
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.pricing.PricingStrategy;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.shared.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 가격 불변성 E2E 테스트.
 * 예약 확정 후 가격 정책/상품 가격 변경 시 불변성을 테스트합니다.
 */
@DisplayName("가격 불변성 E2E 테스트")
class PriceImmutabilityE2ETest extends BaseE2ETest {
	
	@Autowired
	private PricingPolicyRepository pricingPolicyRepository;
	
	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private ReservationPricingRepository reservationPricingRepository;
	
	private Long testPlaceId;
	private Long testRoomId;
	private Long testProductId;
	
	@BeforeEach
	void setUpTestData() {
		testPlaceId = 1L;
		testRoomId = 100L;
		
		createTestPricingPolicy();
		testProductId = createTestProduct();
	}
	
	@Test
	@DisplayName("Scenario 7: 가격 정책 변경 후에도 확정된 예약 가격 유지")
	void scenario7_PriceImmutabilityAfterPolicyChange() {
		// Given: 예약 생성
		final LocalDateTime slot = LocalDateTime.of(2025, 1, 15, 10, 0);
		final CreateReservationRequest createRequest = new CreateReservationRequest(
				testRoomId,
				List.of(slot),
				List.of(new ProductRequest(testProductId, 1))
		);
		
		final ResponseEntity<ReservationPricingResponse> createResponse = restTemplate.postForEntity(
				getBaseUrl() + "/api/v1/reservations",
				createRequest,
				ReservationPricingResponse.class
		);
		
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		final Long reservationId = createResponse.getBody().reservationId();
		final BigDecimal originalPrice = createResponse.getBody().totalPrice();
		
		// When: 예약 확정
		restTemplate.exchange(
				getBaseUrl() + "/api/v1/reservations/" + reservationId + "/confirm",
				org.springframework.http.HttpMethod.PUT,
				null,
				ReservationPricingResponse.class
		);
		
		// When: 가격 정책 변경 (50,000 -> 100,000)
		final PricingPolicy policy = pricingPolicyRepository
				.findById(RoomId.of(testRoomId))
				.orElseThrow();
		policy.updateDefaultPrice(Money.of(new BigDecimal("100000")));
		pricingPolicyRepository.save(policy);
		
		// Then: 확정된 예약의 가격은 변경되지 않음
		final Optional<ReservationPricing> savedReservation = reservationPricingRepository
				.findById(ReservationId.of(reservationId));
		assertThat(savedReservation).isPresent();
		assertThat(savedReservation.get().getTotalPrice().getAmount()).isEqualTo(originalPrice);
	}
	
	@Test
	@DisplayName("Scenario 8: 상품 가격 변경 후에도 확정된 예약 가격 유지")
	void scenario8_PriceImmutabilityAfterProductPriceChange() {
		// Given: 예약 생성
		final LocalDateTime slot = LocalDateTime.of(2025, 1, 15, 14, 0);
		final CreateReservationRequest createRequest = new CreateReservationRequest(
				testRoomId,
				List.of(slot),
				List.of(new ProductRequest(testProductId, 2))
		);
		
		final ResponseEntity<ReservationPricingResponse> createResponse = restTemplate.postForEntity(
				getBaseUrl() + "/api/v1/reservations",
				createRequest,
				ReservationPricingResponse.class
		);
		
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		final Long reservationId = createResponse.getBody().reservationId();
		final BigDecimal originalPrice = createResponse.getBody().totalPrice();
		
		// When: 예약 확정
		restTemplate.exchange(
				getBaseUrl() + "/api/v1/reservations/" + reservationId + "/confirm",
				org.springframework.http.HttpMethod.PUT,
				null,
				ReservationPricingResponse.class
		);
		
		// When: 상품 가격 변경 (10,000 -> 50,000)
		final Product product = productRepository
				.findById(ProductId.of(testProductId))
				.orElseThrow();
		product.updatePricingStrategy(
				PricingStrategy.oneTime(Money.of(new BigDecimal("50000")))
		);
		productRepository.save(product);
		
		// Then: 확정된 예약의 가격은 변경되지 않음
		final Optional<ReservationPricing> savedReservation = reservationPricingRepository
				.findById(ReservationId.of(reservationId));
		assertThat(savedReservation).isPresent();
		assertThat(savedReservation.get().getTotalPrice().getAmount()).isEqualTo(originalPrice);
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
	
	private Long createTestProduct() {
		final Product product = Product.createPlaceScoped(
				ProductId.of(null),
				PlaceId.of(testPlaceId),
				"Test Product",
				PricingStrategy.oneTime(Money.of(new BigDecimal("10000"))),
				100
		);
		final Product saved = productRepository.save(product);
		return saved.getProductId().getValue();
	}
}
