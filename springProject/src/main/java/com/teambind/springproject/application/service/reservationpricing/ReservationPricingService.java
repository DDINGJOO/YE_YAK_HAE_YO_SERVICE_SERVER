package com.teambind.springproject.application.service.reservationpricing;

import com.teambind.springproject.adapter.out.messaging.kafka.event.ReservationPendingPaymentEvent;
import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.request.ProductRequest;
import com.teambind.springproject.application.dto.request.UpdateProductsRequest;
import com.teambind.springproject.application.dto.response.ProductPriceDetail;
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;
import com.teambind.springproject.application.dto.response.ReservationTimePriceDetail;
import com.teambind.springproject.application.port.in.CreateReservationUseCase;
import com.teambind.springproject.application.port.in.UpdateReservationProductsUseCase;
import com.teambind.springproject.application.port.out.InventoryCompensationQueue;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.application.port.out.publisher.EventPublisher;
import com.teambind.springproject.common.config.ReservationConfiguration;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.vo.ProductPriceBreakdown;
import com.teambind.springproject.domain.reservationpricing.InventoryCompensationTask;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown;
import com.teambind.springproject.domain.reservationpricing.exception.ProductNotAvailableException;
import com.teambind.springproject.domain.reservationpricing.exception.ReservationPricingNotFoundException;
import com.teambind.springproject.domain.shared.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 예약 가격 계산 Application Service.
 * CreateReservationUseCase와 UpdateReservationProductsUseCase를 구현합니다.
 */
@Service
@Transactional
public class ReservationPricingService implements CreateReservationUseCase,
		UpdateReservationProductsUseCase {
	
	private static final Logger logger = LoggerFactory.getLogger(ReservationPricingService.class);
	
	private final PricingPolicyRepository pricingPolicyRepository;
	private final ProductRepository productRepository;
	private final ReservationPricingRepository reservationPricingRepository;
	private final InventoryCompensationQueue compensationQueue;
	private final EventPublisher eventPublisher;
	private final long pendingTimeoutMinutes;

	@org.springframework.beans.factory.annotation.Value("${kafka.topics.reservation-pending-payment:reservation-pending-payment}")
	private String reservationPendingPaymentTopic;

	public ReservationPricingService(
			final PricingPolicyRepository pricingPolicyRepository,
			final ProductRepository productRepository,
			final ReservationPricingRepository reservationPricingRepository,
			final InventoryCompensationQueue compensationQueue, EventPublisher eventPublisher,
			final ReservationConfiguration reservationConfiguration) {
		this.pricingPolicyRepository = pricingPolicyRepository;
		this.productRepository = productRepository;
		this.reservationPricingRepository = reservationPricingRepository;
		this.compensationQueue = compensationQueue;
		this.eventPublisher = eventPublisher;
		this.pendingTimeoutMinutes = reservationConfiguration.getPending().getTimeoutMinutes();
	}
	
	@Override
	public ReservationPricingResponse createReservation(final CreateReservationRequest request) {
		logger.info("Creating reservation: roomId={}, timeSlots={}, products={}",
				request.roomId(), request.timeSlots().size(), request.products().size());

		final RoomId roomId = RoomId.of(request.roomId());

		// 1. 가격 정책 조회
		final PricingPolicy pricingPolicy = pricingPolicyRepository.findById(roomId)
				.orElseThrow(() -> new ReservationPricingNotFoundException(
						"Pricing policy not found for roomId: " + request.roomId()));

		// 2. 상품 목록 조회
		final List<Product> products = fetchProducts(request.products());

		// 3. Scope별 재고 예약 (RESERVATION: 원자적 UPDATE, ROOM/PLACE: 시간대별 검증)
		reserveProducts(products, request.products(), roomId, request.timeSlots());

		// 4. 시간대별 가격 계산
		final TimeSlotPriceBreakdown timeSlotBreakdown = calculateTimeSlotBreakdown(
				pricingPolicy, request.timeSlots());

		// 5. 상품별 가격 계산
		final List<ProductPriceBreakdown> productBreakdowns = calculateProductBreakdowns(
				products, request.products());

		// 6. 예약 가격 계산 및 생성
		final ReservationPricing reservationPricing = ReservationPricing.calculate(
				ReservationId.of(null),  // Auto-generated
				roomId,
				timeSlotBreakdown,
				productBreakdowns,
				pendingTimeoutMinutes
		);

		// 7. 저장
		final ReservationPricing savedReservation = reservationPricingRepository.save(
				reservationPricing);

		logger.info("Successfully created reservation: reservationId={}, totalPrice={}",
				savedReservation.getReservationId().getValue(),
				savedReservation.getTotalPrice().getAmount());
		
		// 8. 결제 대기 이벤트 발행
		publishReservationPendingPaymentEvent(savedReservation, pricingPolicy);

		return ReservationPricingResponse.from(savedReservation);
	}
	
	@Override
	public ReservationPricingResponse confirmReservation(final Long reservationId) {
		logger.info("Confirming reservation: reservationId={}", reservationId);
		
		final ReservationPricing reservation = reservationPricingRepository
				.findById(ReservationId.of(reservationId))
				.orElseThrow(() -> new ReservationPricingNotFoundException(reservationId));
		
		reservation.confirm();
		final ReservationPricing savedReservation = reservationPricingRepository.save(reservation);
		
		logger.info("Successfully confirmed reservation: reservationId={}", reservationId);
		
		return ReservationPricingResponse.from(savedReservation);
	}
	
	@Override
	public ReservationPricingResponse cancelReservation(final Long reservationId) {
		logger.info("Cancelling reservation: reservationId={}", reservationId);

		final ReservationPricing reservation = reservationPricingRepository
				.findById(ReservationId.of(reservationId))
				.orElseThrow(() -> new ReservationPricingNotFoundException(reservationId));

		// 1. 예약된 상품 재고 복구
		releaseProducts(reservation);

		// 2. 예약 취소 처리
		reservation.cancel();
		final ReservationPricing savedReservation = reservationPricingRepository.save(reservation);

		logger.info("Successfully cancelled reservation: reservationId={}", reservationId);

		return ReservationPricingResponse.from(savedReservation);
	}

	/**
	 * 예약 환불 처리.
	 * 결제 완료 후 환불하는 경우 CONFIRMED → CANCELLED 상태 전환과 함께 재고를 해제합니다.
	 *
	 * @param reservationId 환불할 예약 ID
	 * @return 환불된 예약 정보
	 * @throws ReservationPricingNotFoundException 예약을 찾을 수 없는 경우
	 */
	public ReservationPricingResponse refundReservation(final Long reservationId) {
		logger.info("Refunding reservation: reservationId={}", reservationId);

		final ReservationPricing reservation = reservationPricingRepository
				.findById(ReservationId.of(reservationId))
				.orElseThrow(() -> new ReservationPricingNotFoundException(reservationId));

		// 1. 예약된 상품 재고 복구
		releaseProducts(reservation);

		// 2. 예약 환불 처리 (CONFIRMED → CANCELLED)
		reservation.refund();
		final ReservationPricing savedReservation = reservationPricingRepository.save(reservation);

		logger.info("Successfully refunded reservation: reservationId={}", reservationId);

		return ReservationPricingResponse.from(savedReservation);
	}

	@Override
	public ReservationPricingResponse updateProducts(
			final Long reservationId,
			final UpdateProductsRequest request) {

		logger.info("Updating products for reservation: reservationId={}, products={}",
				reservationId, request.products().size());

		// 1. 예약 조회
		final ReservationPricing reservation = reservationPricingRepository
				.findById(ReservationId.of(reservationId))
				.orElseThrow(() -> new ReservationPricingNotFoundException(reservationId));

		// 2. 기존 상품 재고 복구
		releaseProducts(reservation);

		// 3. 새 상품 목록 조회
		final List<Product> products = fetchProducts(request.products());

		// 4. 예약의 시간대 추출
		final List<LocalDateTime> timeSlots = extractTimeSlots(reservation);

		// 5. 새 상품 재고 예약 (Scope별 처리)
		reserveProducts(products, request.products(), reservation.getRoomId(), timeSlots);

		// 6. 상품별 가격 계산
		final List<ProductPriceBreakdown> productBreakdowns = calculateProductBreakdowns(
				products, request.products());

		// 7. 예약 상품 업데이트 (도메인 메서드 호출)
		reservation.updateProducts(productBreakdowns);

		// 8. 저장
		final ReservationPricing savedReservation = reservationPricingRepository.save(reservation);

		logger.info("Successfully updated products for reservation: reservationId={}, totalPrice={}",
				reservationId, savedReservation.getTotalPrice().getAmount());

		return ReservationPricingResponse.from(savedReservation);
	}
	
	/**
	 * 상품 목록을 조회합니다.
	 * N+1 쿼리를 방지하기 위해 일괄 조회합니다.
	 */
	private List<Product> fetchProducts(final List<ProductRequest> productRequests) {
		// 1. ProductId 목록 추출
		final List<ProductId> productIds = productRequests.stream()
				.map(req -> ProductId.of(req.productId()))
				.toList();
		
		// 2. 일괄 조회 (1번의 쿼리)
		final List<Product> foundProducts = productRepository.findAllById(productIds);
		
		// 3. 존재하지 않는 상품 검증
		if (foundProducts.size() != productIds.size()) {
			final List<ProductId> foundIds = foundProducts.stream()
					.map(Product::getProductId)
					.toList();
			final List<Long> missingIds = productIds.stream()
					.filter(id -> !foundIds.contains(id))
					.map(ProductId::getValue)
					.toList();
			throw new ReservationPricingNotFoundException(
					"Products not found: " + missingIds);
		}
		
		// 4. 요청 순서대로 정렬 (productRequests 순서 보장)
		final java.util.Map<ProductId, Product> productMap = foundProducts.stream()
				.collect(Collectors.toMap(Product::getProductId, p -> p));
		
		return productIds.stream()
				.map(productMap::get)
				.toList();
	}
	
	/**
	 * 예약 성공한 상품 정보를 저장하는 레코드.
	 * 롤백 시 필요한 최소 정보만 포함합니다.
	 */
	private record ReservedProductInfo(
			Product product,
			int quantity,
			RoomId roomId,
			List<LocalDateTime> timeSlots
	) {}

	/**
	 * 상품 재고를 예약합니다.
	 * Scope에 따라 다른 방식으로 재고를 관리합니다.
	 *
	 * - RESERVATION Scope: 원자적 UPDATE 쿼리로 재고 차감 (시간 무관)
	 * - ROOM Scope: 시간대별 원자적 재고 예약 (room_id 단위)
	 * - PLACE Scope: 시간대별 원자적 재고 예약 (전체 place 범위)
	 *
	 * 중간에 예약 실패 시 이미 예약된 상품들의 재고를 롤백합니다.
	 *
	 * @param products        예약할 상품 목록
	 * @param productRequests 상품 요청 목록 (수량 포함)
	 * @param roomId          예약할 룸 ID (PLACE Scope 상품에서 사용)
	 * @param timeSlots       예약 시간 슬롯 목록 (ROOM/PLACE Scope에서 사용)
	 * @throws ProductNotAvailableException 재고가 부족하여 예약 실패 시
	 */
	private void reserveProducts(
			final List<Product> products,
			final List<ProductRequest> productRequests,
			final RoomId roomId,
			final List<LocalDateTime> timeSlots) {

		final List<ReservedProductInfo> reservedProducts = new ArrayList<>();

		try {
			for (int i = 0; i < products.size(); i++) {
				final Product product = products.get(i);
				final ProductRequest productRequest = productRequests.get(i);

				switch (product.getScope()) {
					case RESERVATION -> reserveReservationProduct(product, productRequest.quantity());
					case ROOM -> reserveRoomProduct(product, productRequest.quantity(), timeSlots);
					case PLACE -> reservePlaceProduct(product, productRequest.quantity(), roomId, timeSlots);
				}

				// 예약 성공 시 목록에 추가
				reservedProducts.add(new ReservedProductInfo(
						product, productRequest.quantity(), roomId, timeSlots));
			}
		} catch (final ProductNotAvailableException e) {
			// 예약 실패 시 지금까지 성공한 예약들을 롤백
			logger.warn("Product reservation failed, rolling back {} previously reserved products",
					reservedProducts.size());
			rollbackReservations(reservedProducts);
			throw e;
		}
	}

	/**
	 * 예약 실패 시 지금까지 예약된 상품들의 재고를 복구합니다.
	 * Best Effort 방식으로 동작하며, 롤백 중 일부 실패하면 보상 트랜잭션 큐에 추가합니다.
	 *
	 * @param reservedProducts 롤백할 예약 목록
	 */
	private void rollbackReservations(final List<ReservedProductInfo> reservedProducts) {
		if (reservedProducts.isEmpty()) {
			return;
		}

		logger.info("Starting rollback for {} reserved products", reservedProducts.size());

		int successCount = 0;
		int failedCount = 0;

		// 역순으로 롤백 (LIFO - Last In First Out)
		for (int i = reservedProducts.size() - 1; i >= 0; i--) {
			final ReservedProductInfo info = reservedProducts.get(i);

			try {
				switch (info.product().getScope()) {
					case RESERVATION -> releaseReservationProduct(info.product(), info.quantity());
					case ROOM -> releaseTimeSlotProduct(
							info.product(), info.quantity(), info.product().getRoomId(), info.timeSlots());
					case PLACE -> releaseTimeSlotProduct(
							info.product(), info.quantity(), info.roomId(), info.timeSlots());
				}

				successCount++;
				logger.debug("Rollback successful: productId={}, scope={}, quantity={}",
						info.product().getProductId().getValue(),
						info.product().getScope(),
						info.quantity());

			} catch (final Exception rollbackException) {
				failedCount++;

				// 롤백 실패 시 보상 트랜잭션 큐에 추가
				logger.error("Rollback failed, adding to compensation queue: productId={}, scope={}, quantity={}, error={}",
						info.product().getProductId().getValue(),
						info.product().getScope(),
						info.quantity(),
						rollbackException.getMessage());

				final InventoryCompensationTask task = new InventoryCompensationTask(
						info.product(),
						info.quantity(),
						info.roomId(),
						info.timeSlots(),
						rollbackException.getMessage()
				);
				compensationQueue.enqueue(task);

				// TODO: 알람 발송 (Slack, PagerDuty, Email 등)
				// alertService.sendWarning("Inventory rollback failed", task);
			}
		}

		logger.info("Rollback completed: {} succeeded, {} failed and queued for compensation out of {} total",
				successCount, failedCount, reservedProducts.size());

		// 롤백 실패가 있더라도 예외를 던지지 않음
		// 실패한 롤백은 보상 트랜잭션 큐에서 재시도됨
	}

	/**
	 * RESERVATION Scope 상품의 재고를 원자적으로 예약합니다.
	 * WHERE 조건에서 재고 검증과 차감을 동시에 수행하여 Race Condition을 방지합니다.
	 *
	 * @param product  예약할 상품
	 * @param quantity 예약할 수량
	 * @throws ProductNotAvailableException 재고가 부족하여 예약 실패 시
	 */
	private void reserveReservationProduct(final Product product, final int quantity) {
		final boolean reserved = productRepository.reserveQuantity(
				product.getProductId(),
				quantity
		);

		if (!reserved) {
			logger.warn("Failed to reserve RESERVATION product: productId={}, quantity={}",
					product.getProductId().getValue(), quantity);
			throw new ProductNotAvailableException(
					product.getProductId().getValue(),
					quantity
			);
		}

		logger.debug("Successfully reserved RESERVATION product: productId={}, quantity={}",
				product.getProductId().getValue(), quantity);
	}

	/**
	 * ROOM Scope 상품의 시간대별 재고를 원자적으로 예약합니다.
	 * product_time_slot_inventory 테이블에 UPSERT + 원자적 UPDATE를 수행합니다.
	 *
	 * @param product   예약할 상품
	 * @param quantity  예약할 수량
	 * @param timeSlots 예약 시간 슬롯 목록
	 * @throws ProductNotAvailableException 재고가 부족하여 예약 실패 시
	 */
	private void reserveRoomProduct(
			final Product product,
			final int quantity,
			final List<LocalDateTime> timeSlots) {

		for (final LocalDateTime timeSlot : timeSlots) {
			final boolean reserved = productRepository.reserveRoomTimeSlotQuantity(
					product.getProductId(),
					product.getRoomId(),
					timeSlot,
					quantity
			);

			if (!reserved) {
				logger.warn("Failed to reserve ROOM product: productId={}, roomId={}, timeSlot={}, quantity={}",
						product.getProductId().getValue(),
						product.getRoomId().getValue(),
						timeSlot,
						quantity);
				throw new ProductNotAvailableException(
						product.getProductId().getValue(),
						quantity
				);
			}
		}

		logger.debug("Successfully reserved ROOM product: productId={}, quantity={}, timeSlots={}",
				product.getProductId().getValue(), quantity, timeSlots.size());
	}

	/**
	 * PLACE Scope 상품의 시간대별 재고를 원자적으로 예약합니다.
	 * 전체 Place 범위의 모든 룸에 걸쳐 재고를 검증하고 예약합니다.
	 *
	 * @param product   예약할 상품
	 * @param quantity  예약할 수량
	 * @param roomId    예약할 룸 ID (product_time_slot_inventory 테이블에 기록)
	 * @param timeSlots 예약 시간 슬롯 목록
	 * @throws ProductNotAvailableException 재고가 부족하여 예약 실패 시
	 */
	private void reservePlaceProduct(
			final Product product,
			final int quantity,
			final RoomId roomId,
			final List<LocalDateTime> timeSlots) {

		for (final LocalDateTime timeSlot : timeSlots) {
			final boolean reserved = productRepository.reservePlaceTimeSlotQuantity(
					product.getProductId(),
					roomId,
					timeSlot,
					quantity
			);

			if (!reserved) {
				logger.warn("Failed to reserve PLACE product: productId={}, placeId={}, roomId={}, timeSlot={}, quantity={}",
						product.getProductId().getValue(),
						product.getPlaceId().getValue(),
						roomId.getValue(),
						timeSlot,
						quantity);
				throw new ProductNotAvailableException(
						product.getProductId().getValue(),
						quantity
				);
			}
		}

		logger.debug("Successfully reserved PLACE product: productId={}, roomId={}, quantity={}, timeSlots={}",
				product.getProductId().getValue(), roomId.getValue(), quantity, timeSlots.size());
	}

	/**
	 * 예약된 상품 재고를 복구합니다.
	 * Scope별로 다른 방식으로 재고를 해제합니다.
	 *
	 * - RESERVATION Scope: 원자적 재고 해제
	 * - ROOM/PLACE Scope: 시간대별 재고 해제
	 *
	 * @param reservation 재고를 복구할 예약
	 */
	private void releaseProducts(final ReservationPricing reservation) {
		final List<ProductPriceBreakdown> productBreakdowns = reservation.getProductBreakdowns();
		final List<LocalDateTime> timeSlots = extractTimeSlots(reservation);

		// N+1 쿼리 방지: 모든 상품을 일괄 조회
		final List<ProductId> productIds = productBreakdowns.stream()
				.map(ProductPriceBreakdown::productId)
				.toList();

		final List<Product> products = productRepository.findAllById(productIds);

		// ProductId -> Product 매핑 생성 (O(1) 조회)
		final java.util.Map<ProductId, Product> productMap = products.stream()
				.collect(java.util.stream.Collectors.toMap(Product::getProductId, p -> p));

		// 상품별 재고 해제
		for (final ProductPriceBreakdown breakdown : productBreakdowns) {
			final Product product = productMap.get(breakdown.productId());

			if (product == null) {
				throw new IllegalStateException(
						"Product not found: productId=" + breakdown.productId().getValue());
			}

			switch (product.getScope()) {
				case RESERVATION -> releaseReservationProduct(product, breakdown.quantity());
				case ROOM -> releaseTimeSlotProduct(product, breakdown.quantity(), product.getRoomId(), timeSlots);
				case PLACE -> releaseTimeSlotProduct(product, breakdown.quantity(), reservation.getRoomId(), timeSlots);
			}
		}
	}

	/**
	 * RESERVATION Scope 상품의 재고를 해제합니다.
	 *
	 * @param product  상품
	 * @param quantity 해제할 수량
	 */
	private void releaseReservationProduct(final Product product, final int quantity) {
		final boolean released = productRepository.releaseQuantity(
				product.getProductId(),
				quantity
		);

		if (!released) {
			logger.error("Failed to release RESERVATION product: productId={}, quantity={}",
					product.getProductId().getValue(), quantity);
			throw new IllegalStateException(
					"Failed to release RESERVATION product: productId=" + product.getProductId().getValue()
			);
		}

		logger.debug("Successfully released RESERVATION product: productId={}, quantity={}",
				product.getProductId().getValue(), quantity);
	}

	/**
	 * ROOM/PLACE Scope 상품의 시간대별 재고를 해제합니다.
	 *
	 * @param product   상품
	 * @param quantity  해제할 수량
	 * @param roomId    룸 ID (예약 시 저장된 roomId)
	 * @param timeSlots 시간 슬롯 목록
	 */
	private void releaseTimeSlotProduct(
			final Product product,
			final int quantity,
			final RoomId roomId,
			final List<LocalDateTime> timeSlots) {

		for (final LocalDateTime timeSlot : timeSlots) {
			final boolean released = productRepository.releaseTimeSlotQuantity(
					product.getProductId(),
					roomId,
					timeSlot,
					quantity
			);

			if (!released) {
				logger.error("Failed to release time-slot product: productId={}, roomId={}, timeSlot={}, quantity={}",
						product.getProductId().getValue(),
						roomId.getValue(),
						timeSlot,
						quantity);
				throw new IllegalStateException(
						"Failed to release time-slot product: productId=" + product.getProductId().getValue()
				);
			}
		}

		logger.debug("Successfully released {} product: productId={}, roomId={}, quantity={}, timeSlots={}",
				product.getScope(), product.getProductId().getValue(), roomId.getValue(), quantity, timeSlots.size());
	}
	
	/**
	 * 시간대별 가격 내역을 계산합니다.
	 */
	private TimeSlotPriceBreakdown calculateTimeSlotBreakdown(
			final PricingPolicy pricingPolicy,
			final List<LocalDateTime> timeSlots) {
		
		final LocalDateTime start = timeSlots.get(0);
		final LocalDateTime end = timeSlots.get(timeSlots.size() - 1)
				.plusMinutes(pricingPolicy.getTimeSlot().getMinutes());
		
		final PricingPolicy.PriceBreakdown priceBreakdown =
				pricingPolicy.calculatePriceBreakdown(start, end);
		
		// PriceBreakdown을 TimeSlotPriceBreakdown으로 변환
		final Map<LocalDateTime, Money> slotPrices = new HashMap<>();
		for (final PricingPolicy.SlotPrice slotPrice : priceBreakdown.getSlotPrices()) {
			slotPrices.put(slotPrice.slotTime(), slotPrice.price());
		}
		
		return new TimeSlotPriceBreakdown(slotPrices, pricingPolicy.getTimeSlot());
	}
	
	/**
	 * 상품별 가격 내역을 계산합니다.
	 */
	private List<ProductPriceBreakdown> calculateProductBreakdowns(
			final List<Product> products,
			final List<ProductRequest> productRequests) {
		
		final List<ProductPriceBreakdown> breakdowns = new ArrayList<>();
		for (int i = 0; i < products.size(); i++) {
			final Product product = products.get(i);
			final ProductRequest productRequest = productRequests.get(i);
			
			final ProductPriceBreakdown breakdown = product.calculatePrice(productRequest.quantity());
			breakdowns.add(breakdown);
		}
		return breakdowns;
	}
	
	/**
	 * 예약의 TimeSlotPriceBreakdown에서 시간대 목록을 추출합니다.
	 */
	private List<LocalDateTime> extractTimeSlots(final ReservationPricing reservation) {
		final TimeSlotPriceBreakdown breakdown = reservation.getTimeSlotBreakdown();
		return new ArrayList<>(breakdown.slotPrices().keySet());
	}
	
	
	private void publishReservationPendingPaymentEvent(
			final ReservationPricing reservation,
			final PricingPolicy pricingPolicy)
	{
		final ReservationPendingPaymentEvent event = buildReservationPendingPaymentEvent(reservation, pricingPolicy);
		eventPublisher.publish(event);
		
		logger.info("Reservation pending payment event published for reservation ID: {}", reservation.getReservationId().getValue());
	}
	
	private ReservationPendingPaymentEvent buildReservationPendingPaymentEvent(
			final ReservationPricing reservation,
			final PricingPolicy pricingPolicy)
	{
		final TimeSlotPriceBreakdown timeSlotBreakdown = reservation.getTimeSlotBreakdown();
		final List<LocalDateTime> timeSlots = extractTimeSlots(reservation);
		final LocalDate reservationDate = timeSlots.get(0).toLocalDate();

		// ReservationTimePriceDetail.from() 사용 - 내부에서 시간을 "HH:mm" 포맷으로 변환
		final ReservationTimePriceDetail timePriceDetail = ReservationTimePriceDetail.from(reservation);

		final List<ProductPriceDetail> productBreakdowns = reservation.getProductBreakdowns().stream()
				.map(breakdown -> new ProductPriceDetail(
					breakdown.productId().getValue(),
					breakdown.productName(),
					breakdown.quantity(),
					breakdown.unitPrice().getAmount(),
					breakdown.totalPrice().getAmount()
				)).toList();

		// 날짜/시간을 String으로 포맷팅
		final String formattedDate = reservationDate.format(DateTimeFormatter.ISO_LOCAL_DATE);  // "yyyy-MM-dd"
		final String formattedOccurredAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);  // "yyyy-MM-dd'T'HH:mm:ss"

		return new ReservationPendingPaymentEvent(
				reservationPendingPaymentTopic,  // yaml에서 주입받은 토픽 사용
				null,  // eventType - 기본값 사용
				reservation.getReservationId().getValue(),
				pricingPolicy.getPlaceId().getValue(),
				reservation.getRoomId().getValue(),
				formattedDate,
				productBreakdowns,
				timePriceDetail,
				reservation.getTotalPrice().getAmount(),
				formattedOccurredAt
		);
	}
	
	
}
