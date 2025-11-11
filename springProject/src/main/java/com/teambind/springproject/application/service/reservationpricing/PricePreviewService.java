package com.teambind.springproject.application.service.reservationpricing;

import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.request.ProductRequest;
import com.teambind.springproject.application.dto.response.PricePreviewResponse;
import com.teambind.springproject.application.dto.response.ProductPriceDetail;
import com.teambind.springproject.application.port.in.CalculateReservationPriceUseCase;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.ProductPriceBreakdown;
import com.teambind.springproject.domain.reservationpricing.exception.ReservationPricingNotFoundException;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 가격 미리보기 Application Service.
 */
@Service
@Transactional(readOnly = true)
public class PricePreviewService implements CalculateReservationPriceUseCase {

  private static final Logger logger = LoggerFactory.getLogger(PricePreviewService.class);

  private final PricingPolicyRepository pricingPolicyRepository;
  private final ProductRepository productRepository;

  public PricePreviewService(
      final PricingPolicyRepository pricingPolicyRepository,
      final ProductRepository productRepository) {
    this.pricingPolicyRepository = pricingPolicyRepository;
    this.productRepository = productRepository;
  }

  @Override
  public PricePreviewResponse calculatePrice(final CreateReservationRequest request) {
    logger.info("Calculating price preview: roomId={}, timeSlots={}, products={}",
        request.roomId(), request.timeSlots().size(), request.products().size());

    final RoomId roomId = RoomId.of(request.roomId());

    // 1. 가격 정책 조회
    final PricingPolicy pricingPolicy = pricingPolicyRepository.findById(roomId)
        .orElseThrow(() -> new ReservationPricingNotFoundException(
            "Pricing policy not found for roomId: " + request.roomId()));

    // 2. 시간대 가격 계산
    final BigDecimal timeSlotPrice = calculateTimeSlotPrice(pricingPolicy, request.timeSlots());

    // 3. 상품 목록 조회
    final List<Product> products = fetchProducts(request.products());

    // 4. 상품별 가격 계산
    final List<ProductPriceDetail> productBreakdowns = calculateProductPriceDetails(
        products, request.products());

    // 5. 총 합계 계산
    final BigDecimal productTotalPrice = productBreakdowns.stream()
        .map(ProductPriceDetail::subtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    final BigDecimal totalPrice = timeSlotPrice.add(productTotalPrice);

    logger.info("Price preview calculated: timeSlotPrice={}, productTotal={}, totalPrice={}",
        timeSlotPrice, productTotalPrice, totalPrice);

    return new PricePreviewResponse(
        timeSlotPrice,
        productBreakdowns,
        totalPrice
    );
  }

  /**
   * 시간대 가격을 계산합니다.
   */
  private BigDecimal calculateTimeSlotPrice(
      final PricingPolicy pricingPolicy,
      final List<LocalDateTime> timeSlots) {

    final LocalDateTime start = timeSlots.get(0);
    final LocalDateTime end = timeSlots.get(timeSlots.size() - 1)
        .plusMinutes(pricingPolicy.getTimeSlot().getMinutes());

    final PricingPolicy.PriceBreakdown priceBreakdown =
        pricingPolicy.calculatePriceBreakdown(start, end);

    final Money totalPrice = priceBreakdown.getSlotPrices().stream()
        .map(PricingPolicy.SlotPrice::price)
        .reduce(Money.ZERO, Money::add);

    return totalPrice.getAmount();
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
   * 상품별 가격 상세 정보를 계산합니다.
   */
  private List<ProductPriceDetail> calculateProductPriceDetails(
      final List<Product> products,
      final List<ProductRequest> productRequests) {

    final List<ProductPriceDetail> details = new ArrayList<>();
    for (int i = 0; i < products.size(); i++) {
      final Product product = products.get(i);
      final ProductRequest productRequest = productRequests.get(i);

      final ProductPriceBreakdown breakdown = product.calculatePrice(productRequest.quantity());

      final ProductPriceDetail detail = new ProductPriceDetail(
          product.getProductId().getValue(),
          product.getName(),
          productRequest.quantity(),
          product.getPricingStrategy().getInitialPrice().getAmount(),
          breakdown.totalPrice().getAmount()
      );

      details.add(detail);
    }
    return details;
  }
}
