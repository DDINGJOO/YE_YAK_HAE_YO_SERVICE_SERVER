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
 * End-to-End tests for basic reservation flow scenarios.
 * Tests the complete reservation lifecycle including creation, confirmation, and cancellation.
 */
@DisplayName("예약 플로우 E2E 테스트")
public class ReservationFlowE2ETest extends BaseE2ETest {
	
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
		// Clean database before each test
		cleanDatabase();

		// Test data setup
		testPlaceId = 1L;
		testRoomId = 100L;

		// Create pricing policy for the room
		createTestPricingPolicy();

		// Create test product
		testProductId = createTestProduct();

		// Initialize inventory for PLACE Scope products
		// All test time slots need inventory records
		initializeInventoryForAllTimeSlots();
	}
	
	@Test
	@DisplayName("Scenario 1: Complete reservation flow (create → confirm → verify)")
	void scenario1_CompleteReservationFlow() {
		// Given: Create a reservation
		final CreateReservationRequest createRequest = new CreateReservationRequest(
				testRoomId,
				List.of(
						LocalDateTime.of(2025, 1, 15, 10, 0),
						LocalDateTime.of(2025, 1, 15, 11, 0)
				),
				List.of(new ProductRequest(testProductId, 2))
		);
		
		// When: POST /api/v1/reservations
		final ResponseEntity<ReservationPricingResponse> createResponse = restTemplate.postForEntity(
				getBaseUrl() + "/api/v1/reservations",
				createRequest,
				ReservationPricingResponse.class
		);
		
		// Then: Reservation is created in PENDING status
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(createResponse.getBody()).isNotNull();
		assertThat(createResponse.getBody().status()).isEqualTo(ReservationStatus.PENDING);
		assertThat(createResponse.getBody().reservationId()).isNotNull();
		assertThat(createResponse.getBody().totalPrice()).isGreaterThan(BigDecimal.ZERO);
		
		final Long reservationId = createResponse.getBody().reservationId();
		
		// When: Confirm the reservation - PUT /api/reservations/{id}/confirm
		final ResponseEntity<ReservationPricingResponse> confirmResponse = restTemplate.exchange(
				getBaseUrl() + "/api/v1/reservations/" + reservationId + "/confirm",
				org.springframework.http.HttpMethod.PUT,
				null,
				ReservationPricingResponse.class
		);
		
		// Then: Reservation is confirmed
		assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(confirmResponse.getBody()).isNotNull();
		assertThat(confirmResponse.getBody().status()).isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(confirmResponse.getBody().reservationId()).isEqualTo(reservationId);
		
		// Verify: Database state is correct
		final Optional<ReservationPricing> savedReservation = reservationPricingRepository
				.findById(ReservationId.of(reservationId));
		assertThat(savedReservation).isPresent();
		assertThat(savedReservation.get().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
	}
	
	@Test
	@DisplayName("Scenario 2: Reservation cancellation flow")
	void scenario2_ReservationCancellationFlow() {
		// Given: Create a reservation in PENDING status
		final CreateReservationRequest createRequest = new CreateReservationRequest(
				testRoomId,
				List.of(LocalDateTime.of(2025, 1, 15, 14, 0)),
				List.of(new ProductRequest(testProductId, 1))
		);
		
		final ResponseEntity<ReservationPricingResponse> createResponse = restTemplate.postForEntity(
				getBaseUrl() + "/api/v1/reservations",
				createRequest,
				ReservationPricingResponse.class
		);
		
		assertThat(createResponse.getBody()).isNotNull();
		final Long reservationId = createResponse.getBody().reservationId();
		
		// When: Cancel the reservation - PUT /api/reservations/{id}/cancel
		final ResponseEntity<ReservationPricingResponse> cancelResponse = restTemplate.exchange(
				getBaseUrl() + "/api/v1/reservations/" + reservationId + "/cancel",
				org.springframework.http.HttpMethod.PUT,
				null,
				ReservationPricingResponse.class
		);
		
		// Then: Reservation is cancelled
		assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(cancelResponse.getBody()).isNotNull();
		assertThat(cancelResponse.getBody().status()).isEqualTo(ReservationStatus.CANCELLED);
		assertThat(cancelResponse.getBody().reservationId()).isEqualTo(reservationId);
		
		// Verify: Database state is correct
		final Optional<ReservationPricing> savedReservation = reservationPricingRepository
				.findById(ReservationId.of(reservationId));
		assertThat(savedReservation).isPresent();
		assertThat(savedReservation.get().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
	}
	
	@Test
	@DisplayName("Scenario 3: PENDING timeout expiration")
	void scenario3_PendingTimeoutExpiration() {
		// Given: Create a reservation with short timeout (configured as 10 minutes in application-e2e.yaml)
		final CreateReservationRequest createRequest = new CreateReservationRequest(
				testRoomId,
				List.of(LocalDateTime.of(2025, 1, 15, 16, 0)),
				List.of(new ProductRequest(testProductId, 1))
		);
		

		final ResponseEntity<ReservationPricingResponse> createResponse = restTemplate.postForEntity(
				getBaseUrl() + "/api/v1/reservations",
				createRequest,
				ReservationPricingResponse.class
		);
		
		assertThat(createResponse.getBody()).isNotNull();
		final Long reservationId = createResponse.getBody().reservationId();
		final LocalDateTime expiresAt = createResponse.getBody().calculatedAt().plusMinutes(10);
		
		// Then: Verify PENDING status and expiresAt is set
		assertThat(createResponse.getBody().status()).isEqualTo(ReservationStatus.PENDING);
		
		final Optional<ReservationPricing> savedReservation = reservationPricingRepository
				.findById(ReservationId.of(reservationId));
		assertThat(savedReservation).isPresent();
		assertThat(savedReservation.get().getExpiresAt()).isNotNull();
		assertThat(savedReservation.get().getExpiresAt()).isAfterOrEqualTo(expiresAt.minusSeconds(1));
		assertThat(savedReservation.get().getExpiresAt()).isBeforeOrEqualTo(expiresAt.plusSeconds(1));
		
		// Note: Actual timeout processing is tested separately in PENDING timeout tests
		// This test only verifies that expiresAt is properly set
	}
	
	/**
	 * Creates a test pricing policy for the test room.
	 */
	private void createTestPricingPolicy() {
		final PricingPolicy policy = PricingPolicy.create(
				RoomId.of(testRoomId),
				PlaceId.of(testPlaceId),
				TimeSlot.HOUR,
				Money.of(new BigDecimal("50000"))
		);
		
		pricingPolicyRepository.save(policy);
	}
	
	/**
	 * Creates a test product and returns its ID.
	 */
	private Long createTestProduct() {
		final Product product = Product.createPlaceScoped(
				ProductId.of(null),
				PlaceId.of(testPlaceId),
				"Test Product",
				PricingStrategy.oneTime(Money.of(new BigDecimal("10000"))),
				100
		);

		final Product savedProduct = productRepository.save(product);
		return savedProduct.getProductId().getValue();
	}

	/**
	 * Initialize inventory for all time slots used in tests.
	 * PLACE Scope products require product_time_slot_inventory records.
	 */
	private void initializeInventoryForAllTimeSlots() {
		// All time slots used across all test scenarios
		final List<LocalDateTime> allTimeSlots = List.of(
				LocalDateTime.of(2025, 1, 15, 10, 0),
				LocalDateTime.of(2025, 1, 15, 11, 0),
				LocalDateTime.of(2025, 1, 15, 14, 0),
				LocalDateTime.of(2025, 1, 15, 15, 0),
				LocalDateTime.of(2025, 1, 15, 16, 0)
		);

		for (LocalDateTime timeSlot : allTimeSlots) {
			jdbcTemplate.update(
					"INSERT INTO product_time_slot_inventory (product_id, room_id, time_slot, total_quantity, reserved_quantity) " +
							"VALUES (?, ?, ?, 100, 0)",
					testProductId, testRoomId, timeSlot
			);
		}
	}
}
